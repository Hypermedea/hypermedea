package hypermedia;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import cartago.*;
import onto.classes.*;
import edu.kit.aifb.datafu.*;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.input.request.EvaluateRequestOrigin;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.origins.RequestOrigin;
import edu.kit.aifb.datafu.io.output.request.EvaluateUnsafeRequestOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import jason.asSyntax.*;
import onto.namespaceAPI.BeliefNamingStrategy;
import onto.ontologyAPI.AxiomExtractor;
import onto.ontologyAPI.InferredAxiomExtractor;
import onto.ontologyAPI.OntologyExtractionManager;
import org.jwat.common.Uri;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.Literal;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jason.asSyntax.ASSyntax.createStructure;

/**
 * A CArtAgO artifact for browsing Linked Data.
 * <p>
 * Contributors:
 * - Victor Charpenay (author), Mines Saint-Étienne
 * - Noé SAFFAF, Mines Saint-Étienne
 */
public class LinkedDataFuSpider extends Artifact {

	private Pattern tripleTermPattern = Pattern.compile("rdf\\(\"(.*)\",\"(.*)\",\"(.*)\"\\)");
	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";
	private Program program;
	private BindingConsumerCollection triples;
	private Timer timer;
	private HashSet<String> registeredURIset;
	private OWLOntology owlOntology;
	private boolean inferred;
	//private

	//Register Save
	private HashSet<OWLAxiomJasonWrapper> setOWLAxiomJasonWrapper;
	private boolean hasMadeRegister;

	//NamingStrategy
	BeliefNamingStrategy beliefNamingStrategy;


	public LinkedDataFuSpider() {
		// set logging level to warning

		Logger log = Logger.getLogger("edu.kit.aifb.datafu");
		//log.setLevel(Level.WARNING);
		log.setLevel(Level.OFF);
		LogManager.getLogManager().addLogger(log);
	}

	/**
	 * Initiate the artifact by passing a programFile for the ldfu engine
	 * @param programFile
	 */
	public void init(String programFile, boolean inferred) {
		try {
			InputStream is = new FileInputStream(programFile);
			Origin base = new FileOrigin(new File(programFile), FileOrigin.Mode.READ, null);
			Notation3Parser n3Parser = new Notation3Parser(is);
			ProgramConsumerImpl programConsumer = new ProgramConsumerImpl(base);

			n3Parser.parse(programConsumer, base);
			is.close();

			program = programConsumer.getProgram(base);

			QueryConsumerImpl queryConsumer = new QueryConsumerImpl(base);
			SparqlParser sparqlParser = new SparqlParser(new StringReader(COLLECT_QUERY));
			sparqlParser.parse(queryConsumer, new InternalOrigin(""));

			ConstructQuery query = queryConsumer.getConstructQueries().iterator().next();

			triples = new BindingConsumerCollection();
			program.registerConstructQuery(query, new BindingConsumerSink(triples));

			initParameters(inferred);

			// TODO recurrent polling with computation of +/- delta?
			//timer = new Timer();
			//timer.schedule(new TimerTask() { @Override public void run() { crawl(); } }, 0, 10000);
		} catch (Exception e) {
			e.printStackTrace();
			// TODO report error

			program = null;
		}
	}

	/**
	 * Initiate parameters and prepare the belief naming strategy by computing all known namespaces
	 */
	private void initParameters(boolean inferred) {
		registeredURIset = new HashSet<>();
		hasMadeRegister = false;
		this.inferred = inferred;

		try {
			owlOntology = OWLManager.createOWLOntologyManager().createOntology();
		} catch (OWLOntologyCreationException e){
			e.printStackTrace();
		}


		beliefNamingStrategy = new BeliefNamingStrategy();
		beliefNamingStrategy.computeMappedKnownNamespaces();
	}

	/**
	 * Deprecated, better use a verification from the agent space
	 */
	@OPERATION
	public void isPresent(String st1, String st2, String st3, OpFeedbackParam<Boolean> result) {
		result.set(hasObsPropertyByTemplate("rdf", st1, st2, st3));
	}

	/**
	 * External action to register all the ontologies in the pending list, merge them into one merged ontology and create unary/binary beliefs (observable properties)
	 * of class declarations object property declarations, data property declarations, owl annotations, and class insertions (instances of indivual) from the ontologies
	 * files. It is possible to have inferred axioms.
	 */
	@OPERATION
	public void register(String originURI) {
		owlOntology = OntologyExtractionManager.addOntology(originURI, owlOntology, registeredURIset);

		Set<OWLAxiom> owlAxiomSet = AxiomExtractor.extractAxioms(owlOntology);
		Set<OWLClass> owlClassSet = AxiomExtractor.extractClasses(owlOntology);

		setOWLAxiomJasonWrapper = AxiomExtractor.extractPredicate(owlOntology, owlClassSet);

		hasMadeRegister = true;
		/*
		if (!setOWLAxiomJasonWrapper.isEmpty()){
			hasMadeRegister = true;
		} else {
			hasMadeRegister = false;
		}*/

		//Case We want to have inferred axioms
		if (inferred) {
			InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
			inferredAxiomExtractor.precomputeInferredAxioms();
			setOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredTypes());
			setOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredSuperclasses());
			setOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredObjectProperties());
			setOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredDataProperties());
		}

		//We check ontology for data used for a strategy to name beliefs
		beliefNamingStrategy.computeMappedLabels(owlClassSet, owlOntology);
		beliefNamingStrategy.computeMappedPreferredNamespaces(owlClassSet, owlOntology);

		for (OWLAxiomJasonWrapper axiom : setOWLAxiomJasonWrapper){
			beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
			if (axiom instanceof OWLUnaryAxiomJasonWrapper) {
				//System.out.println("checkEmpty : "+ axiom.getPredicateName());
				OWLUnaryAxiomJasonWrapper unaryAxiom = (OWLUnaryAxiomJasonWrapper) axiom;
				ObsProperty obsProperty = defineObsProperty(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm());
				Structure s = createStructure("predicate_uri", new Atom(unaryAxiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			} else if (axiom instanceof OWLBinaryAxiomJasonWrapper) {
				OWLBinaryAxiomJasonWrapper binaryAxiom = (OWLBinaryAxiomJasonWrapper) axiom;
				ObsProperty obsProperty = defineObsProperty(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject());
				Structure s = createStructure("predicate_uri", new Atom(binaryAxiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			}

		}
		System.out.println("Number of predicate/axiom extracted : " + setOWLAxiomJasonWrapper.size());
	}

	/**
	 * External action to unregister an ontology by key, it recalculate all axioms with the removed ontology and compare it to the previous ones, and removes
	 * from the observable ontology database the delta difference
	 */
	@OPERATION
	public void unregister(String originURI){
		if (hasMadeRegister && registeredURIset.contains(originURI)){
			registeredURIset.remove(originURI);

			OWLOntology revisedOwlOntology = OntologyExtractionManager.extractOntologyFromRegisteredSet(registeredURIset);
			Set<OWLClass> revisedOwlClasses = AxiomExtractor.extractClasses(revisedOwlOntology);
			HashSet<OWLAxiomJasonWrapper> setRevisedOWLAxiomJasonWrapper = AxiomExtractor.extractPredicate(revisedOwlOntology,revisedOwlClasses);

			if (inferred) {
				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(revisedOwlOntology);
				inferredAxiomExtractor.precomputeInferredAxioms();
				setRevisedOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredTypes());
				setRevisedOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredSuperclasses());
				setRevisedOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredObjectProperties());
				setRevisedOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredDataProperties());
			}
			HashSet<OWLAxiomJasonWrapper> deltaAxiomSet = this.setOWLAxiomJasonWrapper;
			deltaAxiomSet.removeAll(setRevisedOWLAxiomJasonWrapper);

			this.owlOntology = revisedOwlOntology;

			//System.out.println("delta size : " +  deltaAxiomSet.size());
			int numberDeleted = 0;
			for (OWLAxiomJasonWrapper axiom :  deltaAxiomSet) {

				if (axiom instanceof OWLUnaryAxiomJasonWrapper) {
					OWLUnaryAxiomJasonWrapper unaryAxiom = (OWLUnaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm());
					Structure s = createStructure("predicate_uri", new Atom(unaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				} else if (axiom instanceof OWLBinaryAxiomJasonWrapper) {
					OWLBinaryAxiomJasonWrapper binaryAxiom = (OWLBinaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject());
					Structure s = createStructure("predicate_uri", new Atom(binaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				}


				if (axiom instanceof OWLUnaryAxiomJasonWrapper) {
					OWLUnaryAxiomJasonWrapper unaryAxiom = (OWLUnaryAxiomJasonWrapper) axiom;
					if (hasObsPropertyByTemplate(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm()))
					{
						removeObsPropertyByTemplate(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm());
						numberDeleted++;
					}


				} else if (axiom instanceof OWLBinaryAxiomJasonWrapper) {
					OWLBinaryAxiomJasonWrapper binaryAxiom = (OWLBinaryAxiomJasonWrapper) axiom;
					if (hasObsPropertyByTemplate(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject())){
						removeObsPropertyByTemplate(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject());
						numberDeleted++;
					}
				}
			}
			System.out.println("Number of ObsProperties deleted : "+numberDeleted);
		} else {
			System.out.println("No register has been made");
		}
	}

	/**
	 * External Action to check if the ontology is Consistent (has NamedIndividual instance of an unsatisfiable class)
	 * @param b A boolean parameter to unify with the response of the External action
	 * @param report A string parameter to unify with the message report of the External action
	 */
	@OPERATION
	public void isConsistent(OpFeedbackParam<Boolean> b, OpFeedbackParam<String> report){
		if (owlOntology == null){
			b.set(true);
			report.set("No ontology is saved");
		}

		InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
		if (inferredAxiomExtractor.checkConsistency()){
			b.set(true);
			report.set("The ontology is consistent");
		} else {
			b.set(false);
			report.set("Warning : The ontology is not consistent");
		}
	}

	/**
	 * Eternal Action to check if the ontology is Satisfiable
	 * @param displayClasses An option to feed the report message with all the unsatisfiable onto.classes
	 * @param b A boolean parameter to unify with the response of the External action
	 * @param report A string parameter to unify with the message report of the External action
	 */
	@OPERATION
	public void isSatisfiable(boolean displayClasses, OpFeedbackParam<Boolean> b, OpFeedbackParam<String> report){
		if (owlOntology == null){
			b.set(true);
			report.set("No ontology is saved");
		}

		InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
		SatisfiableResponse s = inferredAxiomExtractor.checkSatisfiability(displayClasses);
		if (s.isConsistent()) {
			b.set(true);
			report.set("The ontology is satisfiable");
		} else {
			b.set(false);
			if (displayClasses){
				report.set("Warning : The ontology is not satisfiable.\n"+s);
			} else {
				report.set("Warning : The ontology is not satisfiable.");
			}
		}
	}


	/**
	 * Deprecated
	 * @param st1
	 * @param st2
	 * @param st3
	 * @param index
	 * @param st4
	 */
	@OPERATION
	public void update(String st1, String st2, String st3, int index, String st4) {
		ObsProperty op = getObsPropertyByTemplate("rdf", st1, st2, st3);
		op.updateValue(index, st4);
	}

	/**
	 * External action to execute the Linked Data program and notifies agent with collected triples and their unary
	 * binary axioms according to the already registered ontologies. Can accept local file and can have the inferred
	 * axioms (Class assertion only) of the unary/binary beliefs
	 * @param originURI The entrypoint for the data graph file to crawl, can be a local path if the option local is activated
	 */
	@OPERATION
	public void crawl(String originURI){

		ArrayList<Triple> triples = new ArrayList<Triple>();

		if (program == null) return;

		EvaluateProgramConfig config = new EvaluateProgramConfig();
		//config.setThreadingModel(EvaluateProgramConfig.ThreadingModel.SERIAL);
		EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
		eval.start();


		boolean uriCreated = false;
		URI uri;

		try {
			if (!uriCreated && originURI.startsWith("http")) {
				uri = URI.create(originURI);
				eval.getInputOriginConsumer().consume(new RequestOrigin(uri, Request.Method.GET));
				uriCreated = true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


		try {
			if (!uriCreated) {
				eval.getInputOriginConsumer().consume(new FileOrigin(new File(originURI), FileOrigin.Mode.READ, null));
				uriCreated = true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		try {
			if (!uriCreated) {
				uri = URI.create("file:///" + Paths.get(originURI).toAbsolutePath().toString().replaceAll("\\\\","//"));
				System.out.println();
				eval.getInputOriginConsumer().consume(new RequestOrigin(uri, Request.Method.GET));
				uriCreated = true;
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			System.out.println("URI/FilePath is invalid");
			return;
		}


		try {
			eval.awaitIdleAndFinish();
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		String subject;
		String predicate;
		String object;
		for (Binding binding : this.triples.getCollection()) {
			Node[] st = binding.getNodes().getNodeArray();
			subject = st[0].getLabel();
			predicate = st[1].getLabel();
			object = st[2].getLabel();
			defineObsProperty("rdf", subject, predicate, object);
			triples.add(new Triple(subject, predicate, object));
		}

		/*
		catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}*/


		if (!hasMadeRegister) {
			return;
		}

		try {
			OWLOntology copiedOntology = OntologyExtractionManager.copyOntology(owlOntology);
			HashSet<OWLAxiomJasonWrapper> setOWLAxiomJasonWrapperCrawl = AxiomExtractor.extractAxiomFromTriplet(triples, copiedOntology, inferred, OWLManager.createOWLOntologyManager());
			//System.out.println(setAxiomJasonCrawl.size());
			for (OWLAxiomJasonWrapper axiom : setOWLAxiomJasonWrapperCrawl){
				beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
				if (axiom instanceof OWLUnaryAxiomJasonWrapper) {
					//System.out.println("checkEmpty : "+ axiom.getPredicateName());
					OWLUnaryAxiomJasonWrapper unaryAxiom = (OWLUnaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm());
					Structure s = createStructure("predicate_uri", new Atom(unaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				} else if (axiom instanceof OWLBinaryAxiomJasonWrapper) {
					OWLBinaryAxiomJasonWrapper binaryAxiom = (OWLBinaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject());
					Structure s = createStructure("predicate_uri", new Atom(binaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				}
			}
			//Add the crawl axiom to the general set, but this may not be desired, might remove it
			this.setOWLAxiomJasonWrapper.addAll(setOWLAxiomJasonWrapperCrawl);
		} catch (OWLOntologyCreationException e){
			e.printStackTrace();
		}
	}

	/**
	 * performs a GET request and updates the belief base as the result.
	 */
	@OPERATION
	public void get(String originURI, boolean local, boolean inferred) {
		try {
			List<Triple> tripleList = new ArrayList<>();
			RequestOrigin req;

			if (local){
				return;
				//Origin origin = new FileOrigin(new File(originURI), FileOrigin.Mode.READ, null);
				//req = new RequestOrigin(new URI("randomUri"), Request.Method.GET, origin);
			} else {
				req = new RequestOrigin(new URI(originURI), Request.Method.GET);
			}

			BindingConsumerCollection triples = new BindingConsumerCollection();

			EvaluateRequestOrigin eval = new EvaluateRequestOrigin();
			eval.setTripleCallback(new BindingConsumerSink(triples));
			eval.consume(req);
			eval.shutdown();

			// authoritative subject
			// TODO graph name available?
			if (hasObsPropertyByTemplate("rdf", originURI, null, null)) {
				removeObsPropertyByTemplate("rdf", originURI, null, null);
			}

			String subject;
			String predicate;
			String object;
			for (Binding binding : this.triples.getCollection()) {
				Node[] st = binding.getNodes().getNodeArray();
				subject = st[0].getLabel();
				predicate = st[1].getLabel();
				object = st[2].getLabel();
				defineObsProperty("rdf", subject, predicate, object);
				tripleList.add(new Triple(subject, predicate, object));
			}

			if (!hasMadeRegister) {
				return;
			}

			OntologyExtractionManager.copyOntology(owlOntology);
			HashSet<OWLAxiomJasonWrapper> setOWLAxiomJasonWrapperCrawl = AxiomExtractor.extractAxiomFromTriplet(tripleList, owlOntology, inferred, OWLManager.createOWLOntologyManager());
			//System.out.println(setAxiomJasonCrawl.size());
			for (OWLAxiomJasonWrapper axiom : setOWLAxiomJasonWrapperCrawl){
				beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
				if (axiom instanceof OWLUnaryAxiomJasonWrapper) {
					//System.out.println("checkEmpty : "+ axiom.getPredicateName());
					OWLUnaryAxiomJasonWrapper unaryAxiom = (OWLUnaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(unaryAxiom.getPredicateName(), unaryAxiom.getPredicateTerm());
					Structure s = createStructure("predicate_uri", new Atom(unaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				} else if (axiom instanceof OWLBinaryAxiomJasonWrapper) {
					OWLBinaryAxiomJasonWrapper binaryAxiom = (OWLBinaryAxiomJasonWrapper) axiom;
					ObsProperty obsProperty = defineObsProperty(binaryAxiom.getPredicateName(), binaryAxiom.getPredicateTermSubject(), binaryAxiom.getPredicateTermObject());
					Structure s = createStructure("predicate_uri", new Atom(binaryAxiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				}
			}

			//Add the crawl axiom to the general set, but this may not be desired, might remove it
			this.setOWLAxiomJasonWrapper.addAll(setOWLAxiomJasonWrapperCrawl);

		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}

	/**
	 * performs a PUT request with the given input triples, of the form [rdf(S, P, O), rdf(S, P, O), ...].
	 */
	@OPERATION
	public void put(String originURI, Object[] payload) {
		try {
			RequestOrigin req = new RequestOrigin(new URI(originURI), Request.Method.PUT);

			Set<Nodes> triples = new HashSet<>();
			for (Object term : payload) {
				// terms are exposed as strings to CArtAgO artifacts

				//Case 1, object is of type rdf(S, P, O)
				Matcher m = tripleTermPattern.matcher((String) term);
				if (m.matches()) {
					triples.add(new Nodes(asNode(m.group(1)), asNode(m.group(2)), asNode((m.group(3)))));
				}

				//Case 2,
			}
			req.setTriplesPayload(triples);

			EvaluateUnsafeRequestOrigin eval = new EvaluateUnsafeRequestOrigin();
			eval.consume(req);
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}
	}

	@OPERATION
	public void post(String originURI, Object[] payload) {
		try {
			RequestOrigin req = new RequestOrigin(new URI(originURI), Request.Method.POST);

			Set<Nodes> triples = new HashSet<>();
			for (Object term : payload) {
				// terms are exposed as strings to CArtAgO artifacts
				Matcher m = tripleTermPattern.matcher((String) term);
				if (m.matches()) {
					triples.add(new Nodes(asNode(m.group(1)), asNode(m.group(2)), asNode((m.group(3)))));
				}
			}
			req.setTriplesPayload(triples);

			EvaluateUnsafeRequestOrigin eval = new EvaluateUnsafeRequestOrigin();
			eval.consume(req);
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}
	}

	@OPERATION
	public void delete(String originURI, Object[] payload) {
		try {
			RequestOrigin req = new RequestOrigin(new URI(originURI), Request.Method.DELETE);

			Set<Nodes> triples = new HashSet<>();
			for (Object term : payload) {
				// terms are exposed as strings to CArtAgO artifacts
				Matcher m = tripleTermPattern.matcher((String) term);
				if (m.matches()) {
					triples.add(new Nodes(asNode(m.group(1)), asNode(m.group(2)), asNode((m.group(3)))));
				}
			}
			req.setTriplesPayload(triples);

			EvaluateUnsafeRequestOrigin eval = new EvaluateUnsafeRequestOrigin();
			eval.consume(req);
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}
	}

	private Node asNode(String lexicalForm) {
		if (lexicalForm.startsWith("http")) {
			return new Resource(lexicalForm);
		} else if (lexicalForm.startsWith("_:")) {
			return new BNode((lexicalForm));
		} else if (lexicalForm.matches("^\\d+$")) {
			return new Literal(lexicalForm, new Resource("http://www.w3.org/2001/XMLSchema#integer"));
		} else if (lexicalForm.matches("^\\d+\\.\\d*$")) {
			return new Literal(lexicalForm, new Resource("http://www.w3.org/2001/XMLSchema#decimal"));
		} else if (lexicalForm.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) {
			return new Literal(lexicalForm, new Resource("http://www.w3.org/2001/XMLSchema#dateTime"));
		} else {
			return new Literal(lexicalForm);
		}
	}
}
