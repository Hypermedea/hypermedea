package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import cartago.OpFeedbackParam;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import jason.asSyntax.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.ld.Resource;
import org.hypermedea.owl.NamingStrategyFactory;
import org.hypermedea.owl.OWLAxiomWrapper;
import org.semanticweb.HermiT.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.InferredAxiomGenerator;
import org.semanticweb.owlapi.util.InferredClassAssertionAxiomGenerator;
import org.semanticweb.owlapi.util.InferredPropertyAssertionGenerator;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A CArtAgO artifact for browsing Linked Data.
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataArtifact extends Artifact {

	/**
	 * Manager that listens to changes in the underlying root ontology
	 * and adds/removes corresponding observable properties.
	 *
	 * TODO remove once inference is included in the RDFObsPropertyManager
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
		public void requestCompleted(Resource res) {
			if (visited(res.getURI())) {
				// TODO warn: this shouldn't have happened
				return;
			}

			beginExternalSession();

			if (res.getRepresentation() != null) {
				res.getRepresentation().listStatements().forEachRemaining(st -> {
					RDFNode s = st.getSubject();
					RDFNode p = st.getPredicate();
					RDFNode o = st.getObject();

					Term subject = getRDFNodeTerm(s);
					Term predicate = getRDFNodeTerm(p);
					Term object = getRDFNodeTerm(o);

					ObsProperty prop = defineObsProperty("rdf", subject, predicate, object);

					StringTerm origin = ASSyntax.createString(res.getURI());
					prop.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, origin));

					Atom subjectType = getRDFTypeAtom(s);
					Atom predicateType = getRDFTypeAtom(p);
					Atom objectType = getRDFTypeAtom(o);
					prop.addAnnot(ASSyntax.createStructure(RDF_TYPE_MAP_FUNCTOR, subjectType, predicateType, objectType));
				});

				Ontology o = ontologyManager.createOntology();

				if (!res.getRepresentation().contains(null, RDF.type, OWL.Ontology)) {
					// assuming ABox statements, add signature of closure in ontology definition
					for (OWLEntity e : kbSignature) o.add(dataFactory.getOWLDeclarationAxiom(e));

					o.asGraphModel().add(res.getRepresentation());
				} else {
					// assuming TBox axioms, build signature incrementally
					o.asGraphModel().add(res.getRepresentation());

					kbSignature.addAll(o.getClassesInSignature());
					kbSignature.addAll(o.getObjectPropertiesInSignature());
					kbSignature.addAll(o.getDataPropertiesInSignature());
					kbSignature.addAll(o.getIndividualsInSignature());

					// TODO add predicates for the signature?
				}

				List<OWLOntologyChange> changes = new ArrayList<>();

				IRI iri = IRI.create(res.getURI());
				changes.add(new SetOntologyID(o, iri));
				changes.add(new AddImport(rootOntology, dataFactory.getOWLImportsDeclaration(iri)));

				ontologyManager.applyChanges(changes);

				reasoner.flush();
				updateKbInconsistent(reasoner.isConsistent());

				definePropertiesForAxioms(o.getABoxAxioms(Imports.EXCLUDED));

				if (reasoner.isConsistent()) {
					// TODO remove previously inferred obsProperties
					List<InferredAxiomGenerator<? extends OWLAxiom>> generators = new ArrayList<>();

					generators.add(new InferredClassAssertionAxiomGenerator());
					generators.add(new InferredPropertyAssertionGenerator());

					// TODO are owl:sameAs and owl:differentFrom included?

					Set<ObsProperty> inferredProperties = new HashSet<>();

					for (InferredAxiomGenerator<? extends OWLAxiom> gen : generators) {
						Set<? extends OWLAxiom> axioms = gen.createAxioms(dataFactory, reasoner);
						inferredProperties.addAll(definePropertiesForAxioms((Set<OWLAxiom>) axioms));
					}

					for (ObsProperty p : inferredProperties) {
						Atom annotation = ASSyntax.createAtom("inferred");
						p.addAnnot(annotation);
					}
				}
			}

			removeObsPropertyByTemplate(TO_VISIT_FUNCTOR, res.getURI());
			defineObsProperty(VISITED_FUNCTOR, res.getURI());

			updateCrawlerStatus();

			endExternalSession(true);
		}

		private Term getRDFNodeTerm(RDFNode n) {
			if (n.isURIResource()) return ASSyntax.createString(n.asResource().getURI());
			else if (n.isAnon()) return ASSyntax.createAtom("bnode_" + n.asResource().getId());
			else if (n.asLiteral().getValue() instanceof Number) return ASSyntax.createNumber(n.asLiteral().getDouble());
			else return ASSyntax.createString(n.asLiteral().getString());
		}

		private Atom getRDFTypeAtom(RDFNode n) {
			if (n.isURIResource()) return RDF_TYPE_URI_ATOM;
			else if (n.isAnon()) return RDF_TYPE_BNODE_ATOM;
			else return RDF_TYPE_LITERAL_ATOM;
		}

	}

	private static final String CRAWLER_STATUS_FUNCTOR = "crawler_status";

	private static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

	private static final String SOURCE_FUNCTOR = "crawler_source";

	private static final String RDF_TYPE_MAP_FUNCTOR = "rdf_type_map";

	private static final String VISITED_FUNCTOR = "visited";

	private static final String TO_VISIT_FUNCTOR = "to_visit";

	private static final String KB_INCONSISTENT_FUNCTOR = "kb_inconsistent";

	private static final Atom RDF_TYPE_URI_ATOM = ASSyntax.createAtom("uri");

	private static final Atom RDF_TYPE_BNODE_ATOM = ASSyntax.createAtom("bnode");

	private static final Atom RDF_TYPE_LITERAL_ATOM = ASSyntax.createAtom("literal");

	private LinkedDataCrawler crawler;

	private ObsProperty crawlerStatus;

	private OWLReasoner reasoner;

	private OntologyManager ontologyManager = OntManagers.createManager();

	private OWLDataFactory dataFactory = ontologyManager.getOWLDataFactory();

	private Ontology rootOntology;

	private Set<OWLEntity> kbSignature = new HashSet<>();

	private ShortFormProvider namingStrategy;

	/**
	 * Initialize the artifact without program file (crawl/1 disabled).
	 */
	public void init() {
		init(false);
	}

	/**
	 * Initialize the artifact's LD crawler and ontology manager.
	 * Attach a reasoner to the knowledge base if <code>withInference</code> is set to true.
	 *
	 * @param withInference whether a reasoner should perform inference or not
	 */
	public void init(boolean withInference) {
		initCrawler();
		initOntologyManager(withInference);
	}

	private void initCrawler() {
		crawler = new LinkedDataCrawler();
		crawler.addListener(new RDFObsPropertyManager());

		crawlerStatus = defineObsProperty(CRAWLER_STATUS_FUNCTOR, false);
	}

	private void initOntologyManager(boolean withInference) {
		rootOntology = ontologyManager.createOntology();

		OWLReasonerFactory f = withInference
				// HermiT reasoner (OWL DL)
				? new ReasonerFactory()
				// no reasoner (no implicit axiom inferred from the ontology's structure)
				: new StructuralReasonerFactory();

		reasoner = f.createReasoner(rootOntology);

		namingStrategy = NamingStrategyFactory.createDefaultNamingStrategy(ontologyManager);
	}

	/**
	 * exposes the transformation function from a resource URI to its parent resource URI (without fragment, if any).
	 *
	 * @param resourceURI a resource URI
	 * @param parentResourceURI the URI of the parent resource
	 */
	@OPERATION
	public void getParentURI(String resourceURI, OpFeedbackParam<String> parentResourceURI) {
		try {
			parentResourceURI.set(withoutFragment(resourceURI));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			failed(e.getReason());
		}
	}

	/**
	 * performs a GET request and updates the belief base as the result.
	 */
	@OPERATION
	public void get(String originURI) {
		try {
			// force crawler status to true
			updateCrawlerStatus(true);

			String requestedURI = withoutFragment(originURI);

			if (!visited(requestedURI) && !toVisit(requestedURI)) {
				defineObsProperty(TO_VISIT_FUNCTOR, requestedURI);
				crawler.get(requestedURI);
			} else {
				// TODO necessary?
				updateCrawlerStatus();
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			// TODO improve logging
			failed(e.getMessage());
		}
	}

	private boolean visited(String originURI) {
		return hasObsPropertyByTemplate(VISITED_FUNCTOR, originURI);
	}

	private boolean toVisit(String originURI) {
		return hasObsPropertyByTemplate(TO_VISIT_FUNCTOR, originURI);
	}

	private void updateCrawlerStatus() {
		Boolean isActive = crawler.isActive();

		Boolean hasToVisit;
		try {
			// TODO try/catch shouldn't be here, fix bug upstream in CArtAgO
			hasToVisit = hasObsProperty(TO_VISIT_FUNCTOR);
		} catch (IndexOutOfBoundsException e) {
			hasToVisit = false;
		}

		updateCrawlerStatus(isActive || hasToVisit);
	}

	private void updateCrawlerStatus(Boolean status) {
		if (!crawlerStatus.getValue().equals(status)) crawlerStatus.updateValue(status);
	}

	private void updateKbInconsistent(Boolean consistent) {
		if (consistent && hasObsProperty(KB_INCONSISTENT_FUNCTOR)) removeObsProperty(KB_INCONSISTENT_FUNCTOR);
		else if (!consistent && !hasObsProperty(KB_INCONSISTENT_FUNCTOR)) defineObsProperty(KB_INCONSISTENT_FUNCTOR);
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

	private static String withoutFragment(String resourceURI) throws URISyntaxException {
		URI parsedURI = new URI(resourceURI);
		String fragment = "#" + parsedURI.getFragment();
		return resourceURI.replace(fragment, "");
	}

}
