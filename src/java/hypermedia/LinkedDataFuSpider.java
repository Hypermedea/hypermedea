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
import onto.namespaceAPI.NamingStrategy;
import onto.namespaceAPI.NamingStrategyFactory;
import onto.ontologyAPI.InferredAxiomExtractor;
import onto.ontologyAPI.OntologyExtractionManager;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.yars.nx.BNode;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Nodes;
import org.semanticweb.yars.nx.Resource;
import org.semanticweb.yars.nx.Literal;
import tools.IRITools;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jason.asSyntax.ASSyntax.createStructure;

/**
 * A CArtAgO artifact for browsing Linked Data.
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataFuSpider extends Artifact {

	/**
	 * Manager that listens to changes in the underlying root ontology and adds/removes corresponding observable
	 * properties
	 */
	private class ObsPropertyManager implements OWLOntologyChangeListener, OWLOntologyChangeVisitor {

		private final Map<OWLOntology, Set<ObsProperty>> propertiesByOntology = new HashMap<>();

		@Override
		public void ontologiesChanged(List<? extends OWLOntologyChange> list) throws OWLException {
			for (OWLOntologyChange c : list) {
				if (c.getOntology().equals(rootOntology)) c.accept(this);
			}
		}

		@Override
		public void visit(AddImport addImport) {
			IRI ontologyIRI = addImport.getImportDeclaration().getIRI();
			OWLOntology o = owlOntologyManager.getOntology(ontologyIRI);

			if (!propertiesByOntology.containsKey(o)) {
				Set<ObsProperty> properties = new HashSet<>();

				for (OWLAxiom axiom : o.getAxioms()) {
					OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, namingStrategySet);

					String name = w.getPropertyName();
					List<Object> args = w.getPropertyArguments();

					ObsProperty p = null;

					if (args.size() == 1) p = defineObsProperty(name, args.get(0));
					else if (args.size() == 2) p = defineObsProperty(name, args.get(0), args.get(1));

					if (p != null) properties.add(p);
				}

				propertiesByOntology.put(o, properties);
			}
		}

		@Override
		public void visit(RemoveImport removeImport) {
			IRI ontologyIRI = removeImport.getImportDeclaration().getIRI();
			OWLOntology o = owlOntologyManager.getOntology(ontologyIRI);

			if (propertiesByOntology.containsKey(o)) {
				for (ObsProperty p : propertiesByOntology.get(o)) {
					String name = p.getName();
					Object[] args = p.getValues();

					if (args.length == 1) removeObsPropertyByTemplate(name, args[0]);
					else if (args.length == 2) removeObsPropertyByTemplate(name, args[0], args[1]);
				}
			}
		}
		
		// TODO manage inferred statements

		@Override
		public void visit(AddAxiom addAxiom) { /* ignore */ }

		@Override
		public void visit(RemoveAxiom removeAxiom) { /* ignore */ }

		@Override
		public void visit(SetOntologyID setOntologyID) { /* ignore */ }

		@Override
		public void visit(AddOntologyAnnotation addOntologyAnnotation) { /* ignore */ }

		@Override
		public void visit(RemoveOntologyAnnotation removeOntologyAnnotation) { /* ignore */ }

	}

	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";

	private static final String RDF_TYPE = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

	private static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

	private Pattern tripleTermPattern = Pattern.compile("rdf\\((.*),(.*),(.*)\\)");

	private Program program;

	private BindingConsumerCollection triples;

	private Timer timer;

	private OWLReasoner owlReasoner = null;

	// TODO or a StructuralReasonerFactory if no inference
	private ReasonerFactory reasonerFactory = new ReasonerFactory();

	private OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();

	private OWLOntology rootOntology;

	// TODO to remove
	private Set<String> registeredURIset = new HashSet<>();

	private Map<OWLAxiomWrapper, ObsProperty> observablePropertyOntologyMap = new HashMap<>();

	private Map<OWLAxiomWrapper, ObsProperty> observablePropertyTripleMap = new HashMap<>();

	private Set<OWLAxiomWrapper> owlAxiomWrapperSet = new HashSet<>();

	// TODO replace set of naming strategies with "combo" naming strategy (-> ordered for determinism)

	//NamingStrategy
	Set<NamingStrategy> namingStrategySet;

	//DataFactory
	OWLDataFactory dataFactory = new OWLDataFactoryImpl();

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
		initProgram(programFile);
		initOntology(inferred);
	}

	private void initProgram(String programFile) {
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
	private void initOntology(boolean inferred) {
		try {
			rootOntology = owlOntologyManager.createOntology();

			owlOntologyManager.addOntologyChangeListener(new ObsPropertyManager());

			if (inferred) owlReasoner = reasonerFactory.createNonBufferingReasoner(rootOntology);
		} catch (OWLOntologyCreationException e){
			e.printStackTrace();

			// TODO log
		}

		namingStrategySet = NamingStrategyFactory.createAllNamingStrategySet();

		// TODO replace naming strategies with instances of ShortFormProvider
	}

	/**
	 * External action to register all the ontologies in the pending list, merge them into one merged ontology and
	 * create unary/binary beliefs (observable properties) of class declarations object property declarations, data
	 * property declarations, owl annotations, and assertions from imported ontologies. Some axioms may be inferred.
	 *
	 * TODO rewrite description
	 */
	@OPERATION
	public void register(String ontologyIRI) {
		OWLImportsDeclaration decl = dataFactory.getOWLImportsDeclaration(IRI.create(ontologyIRI));
		rootOntology.getImportsDeclarations().add(decl);
	}

	/**
	 * External action to unregister an ontology by key, it recalculate all axioms with the removed ontology and compare
	 * it to the previous ones, and removes from the observable ontology database the delta difference
	 */
	@OPERATION
	public void unregister(String ontologyIRI) {
		OWLImportsDeclaration decl = dataFactory.getOWLImportsDeclaration(IRI.create(ontologyIRI));
		rootOntology.getImportsDeclarations().remove(decl);

		// TODO is OWLImportsDeclaration.equals() based on IRIs?
	}

	/**
	 * External Action to check if the ontology is Consistent (has NamedIndividual instance of an unsatisfiable class)
	 * @param b A boolean parameter to unify with the response of the External action
	 * @param report A string parameter to unify with the message report of the External action
	 */
	@OPERATION
	public void isConsistent(OpFeedbackParam<Boolean> b, OpFeedbackParam<String> report){
		if (rootOntology == null) {
			b.set(true);
			report.set("No ontology is saved");
		}

		InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(rootOntology, reasonerFactory);
		if (inferredAxiomExtractor.checkConsistency()) {
			b.set(true);
			report.set("The ontology is consistent");
		} else {
			b.set(false);
			report.set("Warning : The ontology is not consistent");
		}
	}

	// TODO remove one of the two operations (above/below)

	/**
	 * External Action to check if the ontology is Satisfiable
	 * @param displayClasses An option to feed the report message with all the unsatisfiable onto.classes
	 * @param b A boolean parameter to unify with the response of the External action
	 * @param report A string parameter to unify with the message report of the External action
	 */
	@OPERATION
	public void isSatisfiable(boolean displayClasses, OpFeedbackParam<Boolean> b, OpFeedbackParam<String> report){
		if (rootOntology == null){
			b.set(true);
			report.set("No ontology is saved");
		}
		InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(rootOntology, reasonerFactory);
		if (inferredAxiomExtractor.checkSatisfiability()) {
			b.set(true);
			report.set("The ontology is satisfiable");
		} else {
			b.set(false);
			if (displayClasses){
				String s = "Unsatisfiable Axioms : ";
				for (OWLClass c : inferredAxiomExtractor.getUnsatisfiableClasses()){
					s += "\n" + c.toString();
				}
				report.set("Warning : The ontology is not satisfiable.\n"+s);
			} else {
				report.set("Warning : The ontology is not satisfiable.");
			}
		}
	}

	/**
	 * External action to execute the Linked Data program and notifies agent with collected triples and their unary
	 * binary axioms according to the already registered ontologies. Can accept local file and can have the inferred
	 * axioms (Class assertion only) of the unary/binary beliefs
	 * @param originURI The entrypoint for the data graph file to crawl, can be a local path if the option local is activated
	 */
	@OPERATION
	public void crawl(String originURI){
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

		Set<OWLClass> owlClassSet = rootOntology.getClassesInSignature();
		Set<OWLObjectProperty> owlObjectPropertySet = rootOntology.getObjectPropertiesInSignature();
		Set<OWLDataProperty> owlDataPropertySet = rootOntology.getDataPropertiesInSignature();

		// TODO set axioms in a separate ontology and import it in the master ontology

		Set<OWLAxiom> owlCrawledAxiomSet = new HashSet<>();
		for (Binding binding : this.triples.getCollection()) {
			Node[] st = binding.getNodes().getNodeArray();
			subject = st[0].getLabel();
			predicate = st[1].getLabel();
			object = st[2].getLabel();
			defineObsProperty("rdf", subject, predicate, object);
			owlCrawledAxiomSet.add(getOwlAxiomFromTriple(subject, predicate, object, owlClassSet, owlObjectPropertySet, owlDataPropertySet, dataFactory));
		}


		try {
			OWLOntology copiedOntology = OntologyExtractionManager.copyOntology(rootOntology);
			copiedOntology = OntologyExtractionManager.addAxiomToOntology(owlCrawledAxiomSet, copiedOntology);
			if (inferred){
				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(copiedOntology, reasonerFactory);
				inferredAxiomExtractor.precomputeInferredAxioms();
				copiedOntology = inferredAxiomExtractor.getInferredOntology();
			}

			Set<OWLAxiomWrapper> owlCrawledAxiomWrapperSet = getOwlAxiomWrapperSet(copiedOntology);

			for (OWLAxiomWrapper axiom : owlCrawledAxiomWrapperSet){
				String propFullName = axiom.getPropertyFullName();
				String propName = axiom.getPropertyName();
				if (propName == null || propName.isBlank()){
					continue;
				}
				List<Object> argumentsList = axiom.getPropertyArguments();
				if (!observablePropertyTripleMap.containsKey(axiom)){
					if (argumentsList.size() == 1 && !hasObsPropertyByTemplate(propName,argumentsList.get(0))){
						ObsProperty obsProperty = defineObsProperty(propName,argumentsList.get(0));
						Structure s = createStructure("predicate_uri", new Atom(propFullName));
						obsProperty.addAnnot(s);
						observablePropertyTripleMap.put(axiom, obsProperty);
					} else if (argumentsList.size() == 2 && !hasObsPropertyByTemplate(propName, argumentsList.get(0), argumentsList.get(1))) {
						ObsProperty obsProperty = defineObsProperty(propName,argumentsList.get(0), argumentsList.get(1));
						Structure s = createStructure("predicate_uri", new Atom(propFullName));
						obsProperty.addAnnot(s);
						observablePropertyTripleMap.put(axiom, obsProperty);
					}
				}
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			return;
		}
	}

	/**
	 * performs a GET request and updates the belief base as the result.
	 */
	@OPERATION
	public void get(String originURI) {
		//long startTime = System.currentTimeMillis();

		/*
		long endTime = System.currentTimeMillis();
		op_time.set(new Double(endTime-startTime));
		totalTime += endTime - startTime;
		 */

		RequestOrigin req;

		boolean uriCreated = false;
		URI uri;

		if (!uriCreated && originURI.startsWith("http")) {
			uri = URI.create(originURI);
		} else {
			uri = URI.create("file:///" + Paths.get(originURI).toAbsolutePath().toString().replaceAll("\\\\", "//"));
		}

		req = new RequestOrigin(uri, Request.Method.GET);

		BindingConsumerCollection triples = new BindingConsumerCollection();

		EvaluateRequestOrigin eval = new EvaluateRequestOrigin();
		eval.setTripleCallback(new BindingConsumerSink(triples));
		try {
			eval.consume(req);
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}

		// authoritative subject
		// TODO graph name available?
		if (hasObsPropertyByTemplate("rdf", originURI, null, null)) {
			removeObsPropertyByTemplate("rdf", originURI, null, null);
		}

		String subject;
		String predicate;
		String object;

		Set<OWLClass> owlClassSet = rootOntology.getClassesInSignature();
		Set<OWLObjectProperty> owlObjectPropertySet = rootOntology.getObjectPropertiesInSignature();
		Set<OWLDataProperty> owlDataPropertySet = rootOntology.getDataPropertiesInSignature();

		// TODO duplicated code

		Set<OWLAxiom> owlCrawledAxiomSet = new HashSet<>();
		for (Binding binding : this.triples.getCollection()) {
			Node[] st = binding.getNodes().getNodeArray();
			subject = st[0].getLabel();
			predicate = st[1].getLabel();
			object = st[2].getLabel();
			defineObsProperty("rdf", subject, predicate, object);
			owlCrawledAxiomSet.add(getOwlAxiomFromTriple(subject, predicate, object, owlClassSet, owlObjectPropertySet, owlDataPropertySet, dataFactory));
		}

		try {
			OWLOntology copiedOntology = OntologyExtractionManager.copyOntology(rootOntology);
			copiedOntology = OntologyExtractionManager.addAxiomToOntology(owlCrawledAxiomSet, copiedOntology);
			if (inferred) {
				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(copiedOntology, reasonerFactory);
				inferredAxiomExtractor.precomputeInferredAxioms();
				copiedOntology = inferredAxiomExtractor.getInferredOntology();
			}
			Set<OWLAxiomWrapper> owlCrawledAxiomWrapperSet = getOwlAxiomWrapperSet(copiedOntology);

			for (OWLAxiomWrapper axiom : owlCrawledAxiomWrapperSet) {
				String propFullName = axiom.getPropertyFullName();
				String propName = axiom.getPropertyName();
				if (propName == null || propName.isBlank()){
					continue;
				}
				List<Object> argumentsList = axiom.getPropertyArguments();
				if (!observablePropertyTripleMap.containsKey(axiom)) {
					if (argumentsList.size() == 1 && !hasObsPropertyByTemplate(propName,argumentsList.get(0))) {
						ObsProperty obsProperty = defineObsProperty(propName, argumentsList.get(0));
						Structure s = createStructure("predicate_uri", new Atom(propFullName));
						obsProperty.addAnnot(s);
						observablePropertyTripleMap.put(axiom, obsProperty);
					} else if (argumentsList.size() == 2 && !hasObsPropertyByTemplate(propName, argumentsList.get(0), argumentsList.get(1))) {
						ObsProperty obsProperty = defineObsProperty(propName, argumentsList.get(0), argumentsList.get(1));
						Structure s = createStructure("predicate_uri", new Atom(propFullName));
						obsProperty.addAnnot(s);
						observablePropertyTripleMap.put(axiom, obsProperty);
					}
				}
			}
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			return;
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
				System.out.println((String) term);
				//Case 1, object is of type rdf(S, P, O)
				Matcher m = tripleTermPattern.matcher((String) term);
				if (m.matches()) {
					Nodes n = new Nodes(asNode(m.group(1)), asNode(m.group(2)), asNode((m.group(3))));
					System.out.println(n);
					triples.add(n);
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

	private OWLAxiom getOwlAxiomFromTriple(String subject, String predicate, Object object, Set<OWLClass> owlClassSet, Set<OWLObjectProperty> owlObjectProperties, Set<OWLDataProperty> owlDataProperties, OWLDataFactory dataFactory)
	{
		if (predicate.equals(RDF_TYPE)){
			for (OWLClass c : owlClassSet) {
				if (IRITools.removeWrapperIRI(c.toString()).equals(object)){
					OWLNamedIndividual namedIndividual = dataFactory.getOWLNamedIndividual(IRI.create(subject));
					OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(c, namedIndividual);
					return classAssertion;
				}
			}

			//Case we didn't find the type class
			OWLNamedIndividual namedIndividual = dataFactory.getOWLNamedIndividual(IRI.create(subject));
			OWLClassExpression classExpression = dataFactory.getOWLClass(IRI.create(object.toString()));
			OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(classExpression, namedIndividual);
			return classAssertion;

		} else {
			//We check all properties
			for (OWLObjectProperty op : owlObjectProperties){
				if (predicate.equals(IRITools.removeWrapperIRI(op.toString()))){
					OWLObjectPropertyAssertionAxiom propertyAssertionAxiom =
							dataFactory.getOWLObjectPropertyAssertionAxiom(
								op,
								dataFactory.getOWLNamedIndividual(IRI.create(subject)),
								dataFactory.getOWLNamedIndividual(IRI.create(object.toString()))
						);
					return propertyAssertionAxiom;
				}
			}

			for (OWLDataProperty dp : owlDataProperties){
				if (predicate.equals(IRITools.removeWrapperIRI(dp.toString()))){
					OWLLiteral literal = null;
					if (object instanceof String){
						literal = dataFactory.getOWLLiteral((String) object);
					} else if (object instanceof Boolean){
						literal = dataFactory.getOWLLiteral((Boolean) object);
					} else if (object instanceof Integer){
						literal = dataFactory.getOWLLiteral((Integer) object);
					} else if (object instanceof Double){
						literal = dataFactory.getOWLLiteral((Double) object);
					}

					OWLDataPropertyAssertionAxiom propertyAssertionAxiom =
							dataFactory.getOWLDataPropertyAssertionAxiom(
									dp,
									dataFactory.getOWLNamedIndividual(IRI.create(subject)),
									literal);
					return propertyAssertionAxiom;
				}
			}
		}
		OWLObjectPropertyAssertionAxiom propertyAssertionAxiom =
				dataFactory.getOWLObjectPropertyAssertionAxiom(
							dataFactory.getOWLObjectProperty(IRI.create(predicate)),
							dataFactory.getOWLNamedIndividual(IRI.create(subject)),
							dataFactory.getOWLNamedIndividual(IRI.create(object.toString())
							));
		return propertyAssertionAxiom;
	}

	private Set<OWLAxiomWrapper> getOwlAxiomWrapperSet(OWLOntology o){
		Set<OWLAxiomWrapper> owlAxiomWrapperSet = new HashSet<>();
		for (OWLAxiom axiom : o.getAxioms()){
			owlAxiomWrapperSet.add(new OWLAxiomWrapper(axiom, namingStrategySet));
		}
		return owlAxiomWrapperSet;
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
