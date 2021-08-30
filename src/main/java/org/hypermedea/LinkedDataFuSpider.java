package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import cartago.OpFeedbackParam;
import edu.kit.aifb.datafu.*;
import edu.kit.aifb.datafu.consumer.impl.BindingConsumerCollection;
import edu.kit.aifb.datafu.engine.EvaluateProgram;
import edu.kit.aifb.datafu.io.origins.FileOrigin;
import edu.kit.aifb.datafu.io.origins.InputOrigin;
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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.owl.NamingStrategyFactory;
import org.hypermedea.owl.OWLAxiomWrapper;
import org.hypermedea.tools.Identifiers;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.yars.nx.Literal;
import org.semanticweb.yars.nx.*;
import org.semanticweb.yars.nx.namespace.XSD;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A CArtAgO artifact for browsing Linked Data.
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataFuSpider extends Artifact {

	/**
	 * Manager that listens to changes in the underlying root ontology
	 * and adds/removes corresponding observable properties.
	 */
	private class OWLObsPropertyManager implements OWLOntologyChangeListener, OWLOntologyChangeVisitor {

		private final Map<OWLOntology, Set<ObsProperty>> propertiesByOntology = new HashMap<>();

		private final Set<ObsProperty> inferredProperties = new HashSet<>();

		@Override
		public void ontologiesChanged(List<? extends OWLOntologyChange> list) {
			for (OWLOntologyChange c : list) c.accept(this);
			updateInferredProperties();
		}

		// FIXME if imported ontologies themselves import other ontologies, do their axioms get defined as ObsProps?

		@Override
		public void visit(AddImport addImport) {
			IRI ontologyIRI = addImport.getImportDeclaration().getIRI();
			OWLOntology o = ontologyManager.getOntology(ontologyIRI);

			Set<ObsProperty> properties = definePropertiesForAxioms(o.getAxioms());
			propertiesByOntology.put(o, properties);
		}

		@Override
		public void visit(RemoveImport removeImport) {
			IRI ontologyIRI = removeImport.getImportDeclaration().getIRI();
			OWLOntology o = ontologyManager.getOntology(ontologyIRI);

			if (propertiesByOntology.containsKey(o)) removeProperties(propertiesByOntology.get(o));
		}

		@Override
		public void visit(AddAxiom change) {
			if (change.getOntology().equals(rootOntology)) {
				OWLAxiom axiom = change.getChangeData().getAxiom();

				Set<OWLAxiom> singleton = new HashSet<>();
				singleton.add(axiom);

				definePropertiesForAxioms(singleton);
			}
		}

		@Override
		public void visit(RemoveAxiom change) {
			if (change.getOntology().equals(rootOntology)) {
				OWLAxiom axiom = change.getChangeData().getAxiom();
				// TODO remove associated property
			}
		}

		/**
		 * TODO perform this method in a worker thread
		 */
		private void updateInferredProperties() {
			if (!reasoner.isConsistent()) {
				log("warning: the set of crawled statements is inconsistent...");
				return;
			}

			removeProperties(inferredProperties);
			inferredProperties.clear();

			List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<>();

			generators.add(new InferredClassAssertionAxiomGenerator());
			generators.add(new InferredPropertyAssertionGenerator());

			// TODO are owl:sameAs and owl:differentFrom included?

			for (InferredAxiomGenerator<? extends OWLAxiom> gen : generators) {
				Set<? extends OWLAxiom> axioms = gen.createAxioms(ontologyManager.getOWLDataFactory(), reasoner);
				inferredProperties.addAll(definePropertiesForAxioms((Set<OWLAxiom>) axioms));
			}

			for (ObsProperty p : inferredProperties) {
				Atom annotation = ASSyntax.createAtom("inferred");
				p.addAnnot(annotation);
			}
		}

	}

	/**
	 * Manager that listens to incoming resources from the Linked Data crawler
	 * and adds corresponding observable properties.
	 */
	private class RDFObsPropertyManager implements RequestListener {

		@Override
		public void requestCompleted(org.hypermedea.ld.Resource res) {
			setStatusProperty(crawler.isActive());

			defineObsProperty("resource", res.getURI());
			// TODO check URI isn't already a resource

			if (res.getRepresentation() != null && !res.isCached()) {
				List<OWLOntologyChange> changes = new ArrayList<>();

				res.getRepresentation().listStatements().forEach(st -> {
					RDFNode s = st.getSubject();
					RDFNode p = st.getPredicate();
					RDFNode o = st.getObject();

					Term subject = getRDFNodeTerm(s);
					Term predicate = getRDFNodeTerm(p);
					Term object = getRDFNodeTerm(o);

					ObsProperty prop = defineObsProperty("rdf", subject, predicate, object);

					StringTerm origin = ASSyntax.createString(res.getURI());
					// FIXME the origin is not the resource's URI
					prop.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, origin));

					Atom subjectType = getRDFTypeAtom(s);
					Atom predicateType = getRDFTypeAtom(p);
					Atom objectType = getRDFTypeAtom(o);
					prop.addAnnot(ASSyntax.createStructure(RDF_TYPE_MAP_FUNCTOR, subjectType, predicateType, objectType));

					OWLAxiom axiom = asOWLAxiom(st);

					if (axiom != null) changes.add(new AddAxiom(rootOntology, axiom));
				});

				ontologyManager.applyChanges(changes); // TODO reasoning scalability if changes by resource?
			}

			commit(); // FIXME commit should be executed by the thread calling the origin operation?
		}

		private Term getRDFNodeTerm(RDFNode n) {
			if (n.isURIResource()) return ASSyntax.createString(n.asResource().getURI());
			else if (n.isAnon()) return ASSyntax.createAtom(n.asResource().getId().toString());
			else if (n.asLiteral().getValue() instanceof Number) return ASSyntax.createNumber(n.asLiteral().getDouble());
			else return ASSyntax.createString(n.asLiteral().getString());
		}

		private Atom getRDFTypeAtom(RDFNode n) {
			if (n.isURIResource()) return RDF_TYPE_URI_ATOM;
			else if (n.isAnon()) return RDF_TYPE_BNODE_ATOM;
			else return RDF_TYPE_LITERAL_ATOM;
		}

	}

	// TODO replace with a select query with quad, to get a triple's origin (same BindingConsumer interface)
	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";

	private static final String RDF_TYPE = OWLRDFVocabulary.RDF_TYPE.toString();

	private static final String CRAWLER_STATUS_FUNCTOR = "crawler_status";

	private static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

	private static final String SOURCE_FUNCTOR = "crawler_source";

	private static final String RDF_TYPE_MAP_FUNCTOR = "rdf_type_map";

	private static final Atom RDF_TYPE_URI_ATOM = ASSyntax.createAtom("uri");

	private static final Atom RDF_TYPE_BNODE_ATOM = ASSyntax.createAtom("bnode");

	private static final Atom RDF_TYPE_LITERAL_ATOM = ASSyntax.createAtom("literal");

	private Pattern tripleTermPattern = Pattern.compile("rdf\\((.*),(.*),(.*)\\)");

	private Program program;

	private EvaluateProgram evaluation; // TODO more explicit name?

	private LinkedDataCrawler crawler;

	private BindingConsumerCollection triples;

	private OWLReasoner reasoner;

	private OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

	private OWLOntology rootOntology;

	private Map<OWLAxiomWrapper, ObsProperty> observablePropertyTripleMap = new HashMap<>();

	private ShortFormProvider namingStrategy;

	private OWLDataFactory dataFactory = new OWLDataFactoryImpl();

	public LinkedDataFuSpider() {
		// set logging level to warning
		Logger log = Logger.getLogger("edu.kit.aifb.datafu");
		log.setLevel(Level.OFF);
		LogManager.getLogManager().addLogger(log);
	}

	/**
	 * Initialize the artifact without program file (crawl/1 disabled).
	 */
	public void init() {
		init(null, false);
	}

	/**
	 * Initialize the artifact by passing a program file name to the ldfu engine.
	 *
	 * @param programFile name of a Linked Data program file
	 */
	public void init(String programFile) {
		init(programFile, false);
	}

	/**
	 * Initialize the artifact by passing a program file name to the ldfu engine.
	 * Attach a reasoner to the knowledge base if <code>withInference</code> is set to true.
	 *
	 * @param programFile name of a Linked Data program file
	 * @param withInference whether a reasoner should perform inference or not
	 */
	public void init(String programFile, boolean withInference) {
		initKeepAliveProgram();
		if (programFile != null) initProgram(programFile);
		initOntology(withInference);

		setStatusProperty(false);
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

			// TODO use the following instead of a SPARQL query?
			// evaluation.getEvaluateInputOrigin().setTripleCallback(new RDFObsPropertyManager());
		} catch (Exception e) {
			e.printStackTrace();
			// TODO report error

			program = null;
		}
	}

	private void initKeepAliveProgram() {
		Origin base = new InternalOrigin("");

//		try {
//			// FIXME duplicate code with initProgram
//			QueryConsumerImpl queryConsumer = new QueryConsumerImpl(base);
//			SparqlParser sparqlParser = new SparqlParser(new StringReader(COLLECT_QUERY));
//			sparqlParser.parse(queryConsumer, base);
//
//			ConstructQuery query = queryConsumer.getConstructQueries().iterator().next();
//
//			Program program = new Program(base); // empty program
//
//			BindingConsumerSink sink = new BindingConsumerSink(new RDFObsPropertyManager());
//			program.registerConstructQuery(query, sink);
//
//			EvaluateProgramConfig config = new EvaluateProgramConfig();
//			evaluation = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
//			evaluation.getEvaluateInputOrigin().setTripleCallback(sink);
//			evaluation.start();
//		} catch (ParseException e) {
//			e.printStackTrace();
//			// TODO log error
//
//			evaluation = null;
//		}

		crawler = new LinkedDataCrawler();
		crawler.addListener(new RDFObsPropertyManager());
	}

	/**
	 * Initialize the artifact's ontology manager.
	 */
	private void initOntology(boolean withInference) {
		try {
			rootOntology = ontologyManager.createOntology();

			OWLOntologyChangeBroadcastStrategy filter = new SpecificOntologyChangeBroadcastStrategy(rootOntology);

			OWLObsPropertyManager m = new OWLObsPropertyManager();
			ontologyManager.addOntologyChangeListener(m, filter);

			OWLReasonerFactory f = withInference
					// HermiT reasoner (OWL DL)
					? new ReasonerFactory()
					// no reasoner (no implicit axiom inferred from the ontology's structure)
					: new StructuralReasonerFactory();

			reasoner = f.createNonBufferingReasoner(rootOntology);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			// TODO log
		}

		namingStrategy = NamingStrategyFactory.createDefaultNamingStrategy(ontologyManager);
	}

	/**
	 * Register an ontology declared in the document given as argument (the IRI of the ontology may differ from the IRI
	 * of the given document, e.g. if the document is a local copy of an online ontology).
	 *
	 * After successful registration, the ontology's vocabulary (class and property names) will then be used to
	 * generate unary and binary predicate in subsequent RDF crawls and the ontology's axioms will be used for
	 * automated reasoning on the crawled RDF.
	 *
	 * @param documentIRI (relative) IRI of an ontology document
	 */
	@OPERATION
	public void register(String documentIRI) {
		IRI iri = IRI.create(documentIRI);

		if (!iri.isAbsolute()) iri = Identifiers.getFileIRI(documentIRI);

		try {
			OWLOntology o = ontologyManager.contains(iri)
					// the ontology has either been manually created beforehand
					? ontologyManager.getOntology(iri)
					// or is assumed to be available online/in the file system
					: ontologyManager.loadOntologyFromOntologyDocument(iri);

			if (o.getOntologyID().isAnonymous()) {
				// set the document's IRI as ontology ID
				OWLOntologyID id = new OWLOntologyID(iri);
				ontologyManager.applyChange(new SetOntologyID(o, id));
			}

			IRI ontologyIRI = o.getOntologyID().getOntologyIRI().get();

			OWLImportsDeclaration decl = dataFactory.getOWLImportsDeclaration(ontologyIRI);
			AddImport change = new AddImport(rootOntology, decl);
			ontologyManager.applyChange(change);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			failed(String.format("Couldn't register ontology <%s>", documentIRI));
			// TODO keep track of stack trace
			return;
		}
	}

	/**
	 * External action to unregister an ontology by key, it recalculate all axioms with the removed ontology and compare
	 * it to the previous ones, and removes from the observable ontology database the delta difference
	 */
	@OPERATION
	public void unregister(String documentIRI) {
		for (OWLImportsDeclaration decl : rootOntology.getImportsDeclarations()) {
			IRI ontologyIRI = decl.getIRI();

			if (documentIRI.equals(ontologyIRI.toString())) {
				RemoveImport change = new RemoveImport(rootOntology, decl);
				ontologyManager.applyChange(change);
			}

			// TODO check document IRI instead of ontology IRI
		}
	}

	/**
	 * External Action to check if the ontology is consistent (no individual instance of owl:Nothing).
	 *
	 * @param b A boolean parameter to unify with the response of the External action
	 */
	@OPERATION
	public void isConsistent(OpFeedbackParam<Boolean> b){
		if (rootOntology == null) b.set(true);
		else b.set(reasoner.isConsistent());
	}

	/**
	 * External action to execute the Linked Data program and notifies agent with collected triples and their unary
	 * binary axioms according to the already registered ontologies. Can accept local file and can have the inferred
	 * axioms (Class assertion only) of the unary/binary beliefs.
	 *
	 * @param originURI The entrypoint for the data graph file to crawl, can be a local path if the option local is activated
	 */
	@OPERATION
	public void crawl(String originURI) {
		if (program == null) return;

		EvaluateProgramConfig config = new EvaluateProgramConfig();
		EvaluateProgram eval = new EvaluateProgramGenerator(program, config).getEvaluateProgram();
		eval.start();

		InputOrigin origin = asOrigin(originURI);

		if (origin == null) return;

		try {
			eval.getInputOriginConsumer().consume(origin);
			eval.awaitIdleAndFinish();
			eval.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		if (hasObsProperty("rdf")) removeObsProperty("rdf"); // FIXME does not remove properties with parameters

		// TODO clear axioms in root ontology?

		definePropertiesForBindings(this.triples.getCollection());

		List<OWLOntologyChange> changes = new ArrayList<>();

		for (Binding binding : this.triples.getCollection()) {
			Node[] st = binding.getNodes().getNodeArray();
			OWLAxiom axiom = asOwlAxiom(st);

			if (axiom != null) {
				AddAxiom addAxiom = new AddAxiom(rootOntology, axiom);
				changes.add(addAxiom);
			}
		}

		ontologyManager.applyChanges(changes);
	}

	/**
	 * performs a GET request and updates the belief base as the result.
	 */
	@OPERATION
	public void get(String originURI) {
		try {
			setStatusProperty(true);
			crawler.get(originURI);
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			// TODO improve logging
			failed(e.getMessage());
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

	private void setStatusProperty(Boolean isActive) {
		if (!hasObsProperty(CRAWLER_STATUS_FUNCTOR)) {
			defineObsProperty(CRAWLER_STATUS_FUNCTOR, isActive);
		}

		ObsProperty p = getObsProperty(CRAWLER_STATUS_FUNCTOR);
		if (!p.getValue().equals(isActive)) p.updateValue(isActive);
	}

	private Set<ObsProperty> definePropertiesForBindings(Collection<Binding> bindings) {
		Set<ObsProperty> properties = new HashSet<>();

		for (Binding binding : bindings) {
			Node[] st = binding.getNodes().getNodeArray();

			String subject = st[0].getLabel();
			String predicate = st[1].getLabel();
			String object = st[2].getLabel();

			ObsProperty p = defineObsProperty("rdf", subject, predicate, object);
			properties.add(p);
		}

		return properties;
	}

	private Set<ObsProperty> definePropertiesForAxioms(Collection<OWLAxiom> axioms) {
		Set<ObsProperty> properties = new HashSet<>();

		for (OWLAxiom axiom : axioms) {
			OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, namingStrategy);

			String name = w.getPropertyName();
			Object[] args = w.getPropertyArguments();

			ObsProperty p = null;

			if (args.length == 1) p = defineObsProperty(name, args[0]);
			else if (args.length == 2) p = defineObsProperty(name, args[0], args[1]);

			if (p != null) {
				String fullName = w.getPropertyIRI();

				if (fullName != null) {
					StringTerm t = ASSyntax.createString(fullName);
					Structure annotation = ASSyntax.createStructure(PREDICATE_IRI_FUNCTOR, t);
					p.addAnnot(annotation);
				}

				properties.add(p);
			}
		}

		return properties;
	}

	private void removeProperties(Set<ObsProperty> properties) {
		for (ObsProperty p : properties) {
			String name = p.getName();
			Object[] args = p.getValues();

			if (args.length == 1) removeObsPropertyByTemplate(name, args[0]);
			else if (args.length == 2) removeObsPropertyByTemplate(name, args[0], args[1]);
		}
	}

	/**
	 * Encapsulate a file or HTTP resource into an upper-level class.
	 *
	 * @param uriOrFilename URI of the HTTP resource or file name
	 * @return an InputOrigin instance
	 */
	private InputOrigin asOrigin(String uriOrFilename) {
		if (uriOrFilename.startsWith("http")) return new RequestOrigin(URI.create(uriOrFilename), Request.Method.GET);
		else return new FileOrigin(new File(uriOrFilename), FileOrigin.Mode.READ, null);
	}

	private OWLAxiom asOWLAxiom(Statement st) {
		// TODO
		return null;
	}

	private OWLAxiom asOwlAxiom(Node[] t) {
		if (t.length != 3) return null;

		if (t[1].getLabel().equals(RDF_TYPE)) {
			OWLClass c = dataFactory.getOWLClass(IRI.create(t[2].getLabel()));

			if (isRegistered(c)) {
				OWLNamedIndividual i = dataFactory.getOWLNamedIndividual(IRI.create(t[0].getLabel()));

				return dataFactory.getOWLClassAssertionAxiom(c, i);
			}
		} else if (t[2] instanceof Literal) {
			OWLDataProperty p = dataFactory.getOWLDataProperty(IRI.create(t[1].getLabel()));

			if (isRegistered(p)) {
				OWLNamedIndividual s = dataFactory.getOWLNamedIndividual(IRI.create(t[0].getLabel()));

				Literal lit = (Literal) t[2];

				OWLDatatype dt = null;
				if (lit.getDatatype() != null) dt = dataFactory.getOWLDatatype(IRI.create(lit.getDatatype().toString()));

				OWLLiteral o = dt != null
						? dataFactory.getOWLLiteral(lit.getLabel(), dt)
						: dataFactory.getOWLLiteral(lit.getLabel());

				return dataFactory.getOWLDataPropertyAssertionAxiom(p, s, o);
			}
		} else {
			OWLObjectProperty p = dataFactory.getOWLObjectProperty(IRI.create(t[1].getLabel()));

			if (isRegistered(p)) {
				OWLNamedIndividual s = dataFactory.getOWLNamedIndividual(IRI.create(t[0].getLabel()));
				OWLNamedIndividual o = dataFactory.getOWLNamedIndividual(IRI.create(t[2].getLabel()));

				return dataFactory.getOWLObjectPropertyAssertionAxiom(p, s, o);
			}
		}

		return null;
	}

	private boolean isRegistered(OWLEntity e) {
		OWLAxiom decl = dataFactory.getOWLDeclarationAxiom(e);
		return !ontologyManager.getOntologies(decl).isEmpty();
	}

	private Object asBuiltIn(Node n) {
		if (!(n instanceof Literal)) return n.getLabel();

		Literal lit = (Literal) n;
		Resource dt = lit.getDatatype();

		if (dt == null) lit.getLabel();

		if (dt.equals(XSD.BOOLEAN)) return Boolean.parseBoolean(lit.getLabel());


		Boolean isDouble = dt.equals(XSD.DECIMAL)
				|| dt.equals(XSD.FLOAT)
				|| dt.equals(XSD.DOUBLE);

		if (isDouble) return Double.parseDouble(lit.getLabel());

		Boolean isInteger = dt.equals(XSD.INTEGER)
				|| dt.equals(XSD.INT)
				|| dt.equals(XSD.BYTE)
				|| dt.equals(XSD.SHORT)
				|| dt.equals(XSD.NEGATIVEINTEGER)
				|| dt.equals(XSD.NONNEGATIVEINTEGER)
				|| dt.equals(XSD.POSITIVEINTEGER)
				|| dt.equals(XSD.NONPOSITIVEINTEGER)
				|| dt.equals(XSD.UNSIGNEDBYTE)
				|| dt.equals(XSD.UNSIGNEDSHORT)
				|| dt.equals(XSD.UNSIGNEDINT);

		if (isInteger) return Integer.parseInt(lit.getLabel());

		return lit.getLabel();
	}

	private Node asNode(String lexicalForm) {
		if (lexicalForm.startsWith("http"))  return new Resource(lexicalForm);

		if (lexicalForm.startsWith("_:")) return new BNode((lexicalForm));

		if (lexicalForm.matches("^\\d+$")) return new Literal(lexicalForm, XSD.INTEGER);

		if (lexicalForm.matches("^\\d+\\.\\d*$")) return new Literal(lexicalForm, XSD.DOUBLE);

		if (lexicalForm.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")) return new Literal(lexicalForm, XSD.DATETIME);

		return new Literal(lexicalForm);
	}

}
