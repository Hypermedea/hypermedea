package hypermedia;

import cartago.*;
import onto.classes.AxiomJason;
import onto.classes.OntologyIRIHolder;
import onto.classes.SatisfiableResponse;
import onto.classes.Triplet;
import edu.kit.aifb.datafu.*;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.InternalOrigin;
import edu.kit.aifb.datafu.io.origins.RequestOrigin;
import edu.kit.aifb.datafu.io.sinks.BindingConsumerSink;
import edu.kit.aifb.datafu.parser.ProgramConsumerImpl;
import edu.kit.aifb.datafu.parser.QueryConsumerImpl;
import edu.kit.aifb.datafu.parser.notation3.Notation3Parser;
import edu.kit.aifb.datafu.parser.sparql.SparqlParser;
import edu.kit.aifb.datafu.planning.EvaluateProgramConfig;
import edu.kit.aifb.datafu.planning.EvaluateProgramGenerator;
import jason.asSyntax.Atom;
import jason.asSyntax.Structure;
import onto.namespaceAPI.BeliefNamingStrategy;
import onto.ontologyAPI.AxiomExtractor;
import onto.ontologyAPI.InferredAxiomExtractor;
import onto.ontologyAPI.OntologyExtractionManager;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.yars.nx.Node;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import static jason.asSyntax.ASSyntax.createStructure;


/**
 * A CArtAgO artifact for browsing Linked Data.
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataFuSpider extends Artifact {

	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";
	private Program program;
	private BindingConsumerCollection triples;
	private Timer timer;
	private HashMap<String,OntologyIRIHolder> ontologyIRIHolders;
	private OWLOntology owlOntology;
	//private

	//Register Save
	private HashSet<AxiomJason> setAxiomJason;
	private ArrayList<ObsProperty> listObsProperties;
	private boolean lastInferredBool;
	private boolean hasMadeRegister;

	//NamingStrategy
	BeliefNamingStrategy beliefNamingStrategy;


	public LinkedDataFuSpider() {
		// set logging level to warning
		Logger log = Logger.getLogger("edu.kit.aifb.datafu");
		log.setLevel(Level.WARNING);
		LogManager.getLogManager().addLogger(log);
	}

	/**
	 * Initiate the artifact by passing a programFile for the ldfu engine
	 * @param programFile
	 */
	public void init(String programFile) {
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

			initParameters();

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
	private void initParameters(){
		listObsProperties = new ArrayList<>();
		ontologyIRIHolders = new HashMap<>();
		hasMadeRegister = false;

		beliefNamingStrategy = new BeliefNamingStrategy();
		beliefNamingStrategy.computeMappedKnownNamespaces();
	}

	/**
	 * External action to add an ontology file to a pending list for the register action
	 * @param iri The iri of the ontology resource file (a relative path if the option local is activated)
	 * @param local a boolean option to declare whether the ontology file is to be looked locally or not, (true for local, false instead)
	 * @param key A user-defined key to keep in track of a register ontology
	 */
	@OPERATION
	public void addIRIPendingRegister(String iri, boolean local, String key){
		if (ontologyIRIHolders.containsKey(key)){
			System.out.println("Key has already been affected");
		} else {
			if (local){
				ontologyIRIHolders.put(key,new OntologyIRIHolder(OntologyIRIHolder.local, iri, key));
			} else {
				ontologyIRIHolders.put(key,new OntologyIRIHolder(OntologyIRIHolder.distant, iri, key));
			}
		}
	}

	/**
	 * External action to remove an ontology file by key
	 * @param key The ontology's key to be removed from the pending list
	 */
	@OPERATION
	public void removeIRIPendingRegister(String key){
		if (ontologyIRIHolders.containsKey(key)){
			ontologyIRIHolders.remove(key);
		} else {
			System.out.println("The key corresponds to no IRI is not being mapped");
		}
	}

	/**
	 * External action to remove all ontologies from the pending list
	 */
	@OPERATION
	public void removeAllIRIPendingRegister(){
		ontologyIRIHolders.clear();
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
	 * @param inferred Option to have inferred axioms of the merged ontology
	 */
	@OPERATION
	public void register(boolean inferred) {
		HashSet<AxiomJason> setAxiomJason;
		OWLOntology owlOntology = OntologyExtractionManager.extractOntologyFromSetIRI(ontologyIRIHolders);

		if (owlOntology == null){
			return;
		}

		Set<OWLAxiom> owlAxiomSet = AxiomExtractor.extractAxioms(owlOntology);
		Set<OWLClass> owlClassSet = AxiomExtractor.extractClasses(owlOntology);

		setAxiomJason = AxiomExtractor.extractPredicate(owlAxiomSet);
		lastInferredBool = inferred;
		hasMadeRegister = true;

		//Case We want to have inferred axioms
		if (inferred) {
			InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
			inferredAxiomExtractor.precomputeInferredAxioms();
			setAxiomJason.addAll(inferredAxiomExtractor.getInferredTypes());
			setAxiomJason.addAll(inferredAxiomExtractor.getInferredSuperclasses());
			setAxiomJason.addAll(inferredAxiomExtractor.getInferredObjectProperties());
			setAxiomJason.addAll(inferredAxiomExtractor.getInferredDataProperties());
		}

		//We check ontology for data used for a strategy to name beliefs
		beliefNamingStrategy.computeMappedLabels(owlClassSet, owlOntology);
		beliefNamingStrategy.computeMappedPreferredNamespaces(owlClassSet, owlOntology);

		for (AxiomJason axiom : setAxiomJason){
			beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
		}

		this.setAxiomJason = setAxiomJason;
		System.out.println("Number of predicate/axiom extracted : " + setAxiomJason.size());



		for (AxiomJason axiom : setAxiomJason ) {
			if (axiom.getPredicateContent().size() == 1) {
				//System.out.println("checkEmpty : "+ axiom.getPredicateName());
				ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0));
				Structure s = createStructure("predicate_uri", new Atom(axiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			} else if (axiom.getPredicateContent().size() == 2) {
				ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0), axiom.getPredicateContent().get(1));
				Structure s = createStructure("predicate_uri", new Atom(axiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			}
		}

		this.owlOntology = owlOntology;
	}

	/**
	 * External action to unregister an ontology by key, it recalculate all axioms with the removed ontology and compare it to the previous ones, and removes
	 * from the observable ontology database the delta difference
	 * @param key The ontology's key to be removed from the merged ontology with is related observable properties
	 */
	@OPERATION
	public void unregisterIRIbyKey(String key){
		if (hasMadeRegister){
			removeIRIPendingRegister(key);
			OWLOntology revisedOwlOntology = OntologyExtractionManager.extractOntologyFromSetIRI(ontologyIRIHolders);
			HashSet<AxiomJason> setRevisedAxiomJason = AxiomExtractor.extractPredicate(AxiomExtractor.extractAxioms(revisedOwlOntology));
			if (lastInferredBool) {
				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(revisedOwlOntology);
				inferredAxiomExtractor.precomputeInferredAxioms();
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredTypes());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredSuperclasses());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredObjectProperties());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredDataProperties());
			}
			HashSet<AxiomJason> deltaAxiomSet = this.setAxiomJason;
			deltaAxiomSet.removeAll(setRevisedAxiomJason);


			//System.out.println("delta size : " +  deltaAxiomSet.size());
			int numberDeleted = 0;
			for (AxiomJason axiomSet :  deltaAxiomSet) {
				if (axiomSet.getPredicateContent().size() == 1) {
					// TODO : There is a weird bug here, we try to remove wrong predicate like "definition", to check after
					//System.out.println(axiomSet.getPredicateName() + " ::: " + axiomSet.getPredicateContent().get(0));

					if (hasObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0)))
					{
						removeObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0));
						numberDeleted++;
					}


				} else if (axiomSet.getPredicateContent().size() == 2) {
					// TODO : Same
					//System.out.println(axiomSet.getPredicateName() + " ::: " + axiomSet.getPredicateContent().get(0) + " ::: " + axiomSet.getPredicateContent().get(1));
					if (hasObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0), axiomSet.getPredicateContent().get(1))){
						removeObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0), axiomSet.getPredicateContent().get(1));
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
	 * Eternal Action to check if the ontology is Consistent (has NamedIndividual instance of an unsatisfiable class)
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
	 * @param local A boolean option to declare whether the data graph file is to be looked locally or not, (true for local, false instead)
	 * @param inferred A boolean option to chose if inferred axioms should also be considered
	 */
	@OPERATION
	public void crawl(String originURI, boolean local, boolean inferred) {

		ArrayList<Triplet> triplets = new ArrayList<Triplet>();

		if (program == null) return;

		EvaluateProgramConfig config = new EvaluateProgramConfig();
		//config.setThreadingModel(EvaluateProgramConfig.ThreadingModel.SERIAL);
		EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
		eval.start();

		try {
			if (local){
				eval.getInputOriginConsumer().consume(new FileOrigin(new File(originURI), FileOrigin.Mode.READ, null));
			} else {
				eval.getInputOriginConsumer().consume(new RequestOrigin(new URI(originURI), Request.Method.GET));
			}

			eval.awaitIdleAndFinish();
			eval.shutdown();

			String subject;
			String predicate;
			String object;
			for (Binding binding : triples.getCollection()) {
				Node[] st = binding.getNodes().getNodeArray();
				subject = st[0].getLabel();
				predicate = st[1].getLabel();
				object = st[2].getLabel();
				defineObsProperty("rdf", subject, predicate, object);
				triplets.add(new Triplet(subject, predicate, object));
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}

		if (!hasMadeRegister) {
			return;
		}

		try {
			OntologyExtractionManager.copyOntology(owlOntology);
			HashSet<AxiomJason> setAxiomJasonCrawl = AxiomExtractor.extractAxiomFromTriplet(triplets, owlOntology, inferred, OWLManager.createOWLOntologyManager());
			//System.out.println(setAxiomJasonCrawl.size());
			for (AxiomJason axiom : setAxiomJasonCrawl){
				beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
				if (axiom.getPredicateContent().size() == 1) {
					//System.out.println("checkEmpty : "+ axiom.getPredicateName());
					ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0));
					Structure s = createStructure("predicate_uri", new Atom(axiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				} else if (axiom.getPredicateContent().size() == 2) {
					ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0), axiom.getPredicateContent().get(1));
					Structure s = createStructure("predicate_uri", new Atom(axiom.getPredicateFullName()));
					obsProperty.addAnnot(s);
				}
			}


			//Add the crawl axiom to the general set, but this may not be desired, might remove it
			this.setAxiomJason.addAll(setAxiomJasonCrawl);
		} catch (OWLOntologyCreationException e){
			e.printStackTrace();
		}

	}
}
