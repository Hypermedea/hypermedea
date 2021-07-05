package hypermedia;

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
import jason.asSyntax.ASSyntax;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Structure;
import onto.InferredAxiomExtractor;
import onto.NamingStrategyFactory;
import onto.OWLAxiomWrapper;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.util.OWLOntologyChangeVisitorAdapter;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.vocab.OWLRDFVocabulary;
import org.semanticweb.yars.nx.*;
import org.semanticweb.yars.nx.namespace.XSD;
import tools.IRITools;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

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
	private class ObsPropertyManager extends OWLOntologyChangeVisitorAdapter implements OWLOntologyChangeListener {

		private final Map<OWLOntology, Set<ObsProperty>> propertiesByOntology = new HashMap<>();

		@Override
		public void ontologiesChanged(List<? extends OWLOntologyChange> list) {
			for (OWLOntologyChange c : list) {
				if (c.getOntology().equals(rootOntology)) c.accept(this);
			}
		}

		@Override
		public void visit(AddImport addImport) {
			IRI ontologyIRI = addImport.getImportDeclaration().getIRI();
			OWLOntology o = ontologyManager.getOntology(ontologyIRI);

			definePropertiesForOntology(o);
		}

		@Override
		public void visit(RemoveImport removeImport) {
			IRI ontologyIRI = removeImport.getImportDeclaration().getIRI();
			OWLOntology o = ontologyManager.getOntology(ontologyIRI);

			removePropertiesForOntology(o);
		}

		// TODO manage inferred statements

		private void definePropertiesForOntology(OWLOntology o) {
			if (!propertiesByOntology.containsKey(o)) {
				Set<ObsProperty> properties = new HashSet<>();

				for (OWLAxiom axiom : o.getAxioms()) {
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

				propertiesByOntology.put(o, properties);
			}
		}

		private void removePropertiesForOntology(OWLOntology o) {
			if (propertiesByOntology.containsKey(o)) {
				for (ObsProperty p : propertiesByOntology.get(o)) {
					String name = p.getName();
					Object[] args = p.getValues();

					if (args.length == 1) removeObsPropertyByTemplate(name, args[0]);
					else if (args.length == 2) removeObsPropertyByTemplate(name, args[0], args[1]);
				}
			}
		}

	}

	private static final String COLLECT_QUERY = "construct { ?s ?p ?o . } where { ?s ?p ?o . }";

	private static final String RDF_TYPE = OWLRDFVocabulary.RDF_TYPE.toString();

	private static final String CRAWLED_ASSERTIONS_IRI = String.format("urn:uuid:%s", UUID.randomUUID());

	private static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

	private Pattern tripleTermPattern = Pattern.compile("rdf\\((.*),(.*),(.*)\\)");

	private Program program;

	private BindingConsumerCollection triples;

	private Timer timer;

	private OWLReasoner owlReasoner = null;

	// TODO or a StructuralReasonerFactory if no inference?
	private ReasonerFactory reasonerFactory = new ReasonerFactory();

	private OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

	private OWLOntology rootOntology;

	private OWLOntology crawledAssertions;

	private Map<OWLAxiomWrapper, ObsProperty> observablePropertyTripleMap = new HashMap<>();

	private ShortFormProvider namingStrategy;

	private OWLDataFactory dataFactory = new OWLDataFactoryImpl();

	public LinkedDataFuSpider() {
		// set logging level to warning
		Logger log = Logger.getLogger("edu.kit.aifb.datafu");
		//log.setLevel(Level.WARNING);
		log.setLevel(Level.OFF);
		LogManager.getLogManager().addLogger(log);
	}

	/**
	 * Initialize the artifact by passing a program file name to the ldfu engine.
	 *
	 * @param programFile name of a Linked Data program file
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
	 * Initialize the artifact's ontology manager.
	 */
	private void initOntology(boolean inferred) {
		try {
			rootOntology = ontologyManager.createOntology();

			OWLOntologyChangeBroadcastStrategy filter = new SpecificOntologyChangeBroadcastStrategy(rootOntology);

			ObsPropertyManager m = new ObsPropertyManager();
			ontologyManager.addOntologyChangeListener(m, filter);

			if (inferred) owlReasoner = reasonerFactory.createNonBufferingReasoner(rootOntology);
		} catch (OWLOntologyCreationException e){
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

		if (!iri.isAbsolute()) iri = IRITools.getFileIRI(documentIRI);

		try {
			OWLOntology o = ontologyManager.loadOntologyFromOntologyDocument(iri);
			IRI ontologyIRI = o.getOntologyID().getOntologyIRI();

			OWLImportsDeclaration decl = dataFactory.getOWLImportsDeclaration(ontologyIRI);
			AddImport change = new AddImport(rootOntology, decl);
			ontologyManager.applyChange(change);
		} catch (OWLOntologyCreationException e) {
			failed(String.format("Couldn't register ontology <%s>: %s", documentIRI, e.getMessage()));
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
		if (rootOntology == null) {
			b.set(true);
			return;
		}

		InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(rootOntology, reasonerFactory);
		b.set(inferredAxiomExtractor.checkConsistency());
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

		if (hasObsProperty("rdf")) removeObsProperty("rdf"); // TODO only if crawl succeeded

		try {
			unregister(CRAWLED_ASSERTIONS_IRI);

			crawledAssertions = ontologyManager.createOntology(IRI.create(CRAWLED_ASSERTIONS_IRI));
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
			failed("Could not create ABox statements.");
		}

		for (Binding binding : this.triples.getCollection()) {
			Node[] st = binding.getNodes().getNodeArray();

			// create basic rdf/3 property

			String subject = st[0].getLabel();
			String predicate = st[1].getLabel();
			String object = st[2].getLabel();

			defineObsProperty("rdf", subject, predicate, object);

			// add axiom to ABox (will create idiomatic property with name derived from registered ontologies)

			OWLAxiom axiom = asOwlAxiom(st);

			if (axiom != null) crawledAssertions.getAxioms().add(axiom);
		}

		register(CRAWLED_ASSERTIONS_IRI);
	}

//	/**
//	 * performs a GET request and updates the belief base as the result.
//	 */
//	@OPERATION
//	public void get(String originURI) {
//		//long startTime = System.currentTimeMillis();
//
//		/*
//		long endTime = System.currentTimeMillis();
//		op_time.set(new Double(endTime-startTime));
//		totalTime += endTime - startTime;
//		 */
//
//		RequestOrigin req;
//
//		boolean uriCreated = false;
//		URI uri;
//
//		if (!uriCreated && originURI.startsWith("http")) {
//			uri = URI.create(originURI);
//		} else {
//			uri = URI.create("file:///" + Paths.get(originURI).toAbsolutePath().toString().replaceAll("\\\\", "//"));
//		}
//
//		req = new RequestOrigin(uri, Request.Method.GET);
//
//		BindingConsumerCollection triples = new BindingConsumerCollection();
//
//		EvaluateRequestOrigin eval = new EvaluateRequestOrigin();
//		eval.setTripleCallback(new BindingConsumerSink(triples));
//		try {
//			eval.consume(req);
//			eval.shutdown();
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//			return;
//		}
//
//		// authoritative subject
//		// TODO graph name available?
//		if (hasObsPropertyByTemplate("rdf", originURI, null, null)) {
//			removeObsPropertyByTemplate("rdf", originURI, null, null);
//		}
//
//		String subject;
//		String predicate;
//		String object;
//
//		Set<OWLClass> owlClassSet = rootOntology.getClassesInSignature();
//		Set<OWLObjectProperty> owlObjectPropertySet = rootOntology.getObjectPropertiesInSignature();
//		Set<OWLDataProperty> owlDataPropertySet = rootOntology.getDataPropertiesInSignature();
//
//		// TODO duplicated code
//
//		Set<OWLAxiom> owlCrawledAxiomSet = new HashSet<>();
//		for (Binding binding : this.triples.getCollection()) {
//			Node[] st = binding.getNodes().getNodeArray();
//			subject = st[0].getLabel();
//			predicate = st[1].getLabel();
//			object = st[2].getLabel();
//			defineObsProperty("rdf", subject, predicate, object);
//			owlCrawledAxiomSet.add(asOwlAxiom(subject, predicate, object, owlClassSet, owlObjectPropertySet, owlDataPropertySet, dataFactory));
//		}
//
//		try {
//			OWLOntology copiedOntology = OntologyExtractionManager.copyOntology(rootOntology);
//			copiedOntology = OntologyExtractionManager.addAxiomToOntology(owlCrawledAxiomSet, copiedOntology);
//			if (inferred) {
//				InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(copiedOntology, reasonerFactory);
//				inferredAxiomExtractor.precomputeInferredAxioms();
//				copiedOntology = inferredAxiomExtractor.getInferredOntology();
//			}
//			Set<OWLAxiomWrapper> owlCrawledAxiomWrapperSet = getOwlAxiomWrapperSet(copiedOntology);
//
//			for (OWLAxiomWrapper axiom : owlCrawledAxiomWrapperSet) {
//				String propFullName = axiom.getPropertyFullName();
//				String propName = axiom.getPropertyName();
//				if (propName == null || propName.isBlank()){
//					continue;
//				}
//				List<Object> argumentsList = axiom.getPropertyArguments();
//				if (!observablePropertyTripleMap.containsKey(axiom)) {
//					if (argumentsList.size() == 1 && !hasObsPropertyByTemplate(propName,argumentsList.get(0))) {
//						ObsProperty obsProperty = defineObsProperty(propName, argumentsList.get(0));
//						Structure s = createStructure("predicate_uri", new Atom(propFullName));
//						obsProperty.addAnnot(s);
//						observablePropertyTripleMap.put(axiom, obsProperty);
//					} else if (argumentsList.size() == 2 && !hasObsPropertyByTemplate(propName, argumentsList.get(0), argumentsList.get(1))) {
//						ObsProperty obsProperty = defineObsProperty(propName, argumentsList.get(0), argumentsList.get(1));
//						Structure s = createStructure("predicate_uri", new Atom(propFullName));
//						obsProperty.addAnnot(s);
//						observablePropertyTripleMap.put(axiom, obsProperty);
//					}
//				}
//			}
//		} catch (OWLOntologyCreationException e) {
//			e.printStackTrace();
//			return;
//		}
//	}

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

	/**
	 * Encapsulate a file or HTTP resource into a upper-level class.
	 *
	 * @param uriOrFilename URI of the HTTP resource or file name
	 * @return an InputOrigin instance
	 */
	private InputOrigin asOrigin(String uriOrFilename) {
		if (uriOrFilename.startsWith("http")) return new RequestOrigin(URI.create(uriOrFilename), Request.Method.GET);
		else return new FileOrigin(new File(uriOrFilename), FileOrigin.Mode.READ, null);
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
