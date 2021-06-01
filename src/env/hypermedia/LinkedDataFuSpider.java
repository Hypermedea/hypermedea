package hypermedia;

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

import cartago.*;
import classes.Axiom_Jason;
import classes.OntologyIRIHolder;
import classes.SatisfiableResponse;
import classes.TestAnnotation;
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
import jason.asSyntax.*;
import namespaceAPI.BeliefNamingStrategy;
import ontologyAPI.AxiomExtractor;
import ontologyAPI.InferredAxiomExtractor;
import ontologyAPI.OntologyExtractionManager;
import org.apache.jena.rdf.model.Model;
import org.mapdb.Atomic;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.yars.nx.Node;

import static jason.asSyntax.ASSyntax.createStructure;


/**
 * A CArtAgO artifact for browsing Linked Data.
 * <p>
 * Contributors:
 * - Victor Charpenay (author), Mines Saint-Étienne
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
	private HashSet<Axiom_Jason> setAxiomJason;
	private ArrayList<ObsProperty> listObsProperties;
	private String lastMergedOntologyName;
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

	private void initParameters(){
		listObsProperties = new ArrayList<>();
		ontologyIRIHolders = new HashMap<>();
		hasMadeRegister = false;

		beliefNamingStrategy = new BeliefNamingStrategy();
		beliefNamingStrategy.computeMappedKnownNamespaces();
	}

	@OPERATION
	public void addIRIMapping(String iri, boolean local, String key){
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

	@OPERATION
	public void removeIRIMapping(String key){
		if (ontologyIRIHolders.containsKey(key)){
			ontologyIRIHolders.remove(key);
		} else {
			System.out.println("The key corresponds to no IRI is not being mapped");
		}
	}

	@OPERATION
	public void removeAllIRIMapping(){
		ontologyIRIHolders.clear();
	}

	@OPERATION
	public void isPresent(String st1, String st2, String st3, OpFeedbackParam<Boolean> result) {
		result.set(hasObsPropertyByTemplate("rdf", st1, st2, st3));
	}

	@OPERATION
	public void unregisterIRIbyKey(String key){
		if (hasMadeRegister){
			removeIRIMapping(key);
			OWLOntology revisedOwlOntology = OntologyExtractionManager.extractOntologyFromSetIRI(ontologyIRIHolders, lastMergedOntologyName);
			HashSet<Axiom_Jason> setRevisedAxiomJason = AxiomExtractor.extractPredicate(AxiomExtractor.extractAxioms(revisedOwlOntology));
			if (lastInferredBool) {
				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(revisedOwlOntology);
				inferredAxiomExtractor.precomputeInferredAxioms();
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredTypes());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredSuperclasses());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredObjectProperties());
				setRevisedAxiomJason.addAll(inferredAxiomExtractor.getInferredDataProperties());
			}
			HashSet<Axiom_Jason> deltaAxiomSet = this.setAxiomJason;
			deltaAxiomSet.removeAll(setRevisedAxiomJason);


			//System.out.println("delta size : " +  deltaAxiomSet.size());
			for (Axiom_Jason axiomSet :  deltaAxiomSet) {
				if (axiomSet.getPredicateContent().size() == 1) {
					//System.out.println(axiomSet.getPredicateName() + " ::: " + axiomSet.getPredicateContent().get(0));
					removeObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0));

				} else if (axiomSet.getPredicateContent().size() == 2) {
					removeObsPropertyByTemplate(axiomSet.getPredicateName(), axiomSet.getPredicateContent().get(0), axiomSet.getPredicateContent().get(1));
				}
			}

		} else {
			System.out.println("No register has been made");
		}
	}

	@OPERATION
	public void register(String mergedOntologyIRI, boolean inferred) {
		HashSet<Axiom_Jason> setAxiomJason;
		OWLOntology owlOntology = OntologyExtractionManager.extractOntologyFromSetIRI(ontologyIRIHolders, mergedOntologyIRI);

		if (owlOntology == null){
			return;
		}

		Set<OWLAxiom> owlAxiomSet = AxiomExtractor.extractAxioms(owlOntology);
		Set<OWLClass> owlClassSet = AxiomExtractor.extractClasses(owlOntology);

		setAxiomJason = AxiomExtractor.extractPredicate(owlAxiomSet);
		lastInferredBool = inferred;
		lastMergedOntologyName = mergedOntologyIRI;
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

		//TODO : A décommenter quand debug
		//beliefNamingStrategy.computeMappedPreferredNamespaces("get.n3",null);
		for (Axiom_Jason axiom : setAxiomJason){
			beliefNamingStrategy.generateNameBelief(axiom,true,true,true);
		}

		this.setAxiomJason = setAxiomJason;
		System.out.println("Number of predicate/axiom extracted : " + setAxiomJason.size());



		for (Axiom_Jason axiom : setAxiomJason ) {
			if (axiom.getPredicateContent().size() == 1) {
				//System.out.println("checkEmpty : "+ axiom.getPredicateName());
				ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0));
				Structure s = createStructure("namespace", new Atom(axiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			} else if (axiom.getPredicateContent().size() == 2) {
				ObsProperty obsProperty = defineObsProperty(axiom.getPredicateName(), axiom.getPredicateContent().get(0));
				Structure s = createStructure("namespace", new Atom(axiom.getPredicateFullName()));
				obsProperty.addAnnot(s);
			}
		}

		this.owlOntology = owlOntology;
	}

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


	@OPERATION
	public void update(String st1, String st2, String st3, int index, String st4) {
		ObsProperty op = getObsPropertyByTemplate("rdf", st1, st2, st3);
		op.updateValue(index, st4);
	}

	/**
	 * executes the Linked Data program and notifies agent with collected triples.
	 */
	@OPERATION
	public void crawl(String originURI, boolean local) {

		if (local) {
			// TODO case for local, not implemented yet
			return;
		}

		if (program == null) return;

		EvaluateProgramConfig config = new EvaluateProgramConfig();
		//config.setThreadingModel(EvaluateProgramConfig.ThreadingModel.SERIAL);
		EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
		eval.start();

		try {
			eval.getInputOriginConsumer().consume(new RequestOrigin(new URI(originURI), Request.Method.GET));

			eval.awaitIdleAndFinish();
			eval.shutdown();

			for (Binding binding : triples.getCollection()) {
				Node[] st = binding.getNodes().getNodeArray();
				defineObsProperty("rdf", st[0].getLabel(), st[1].getLabel(), st[2].getLabel());
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
			// TODO recover or ignore?
		} catch (URISyntaxException e) {
			// TODO throw it to make operation fail?
			e.printStackTrace();
		}
	}
}
