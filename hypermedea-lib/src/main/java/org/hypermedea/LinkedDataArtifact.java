package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import cartago.OpFeedbackParam;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import jason.asSyntax.*;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RDFTripleWrapper;
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
 * <p>
 *   Artifact for browsing Linked Data.
 * </p>
 *
 * <h4>Browsing Linked Data</h4>
 *
 * <p>
 *   The main operation of the <code>ThingArtifact</code> is {@link #get(String) get}, to look a Web resource up.
 *   The resource may be identified by an <code>http</code>, <code>https</code> or <code>file</code> URI. If a
 *   relative URI is given as argument, it is assumed to be a <code>file</code> URI relative to the JaCaMo
 *   project's directory.
 * </p>
 *
 * <p>
 *   The <code>LinkedDataArtifact</code> performs lookups in an asynchronous fashion, that is: a
 *   {@link #get(String) get} operation returns before the RDF representation of the resource is fully downloaded.
 *   Several observable properties are exposed by the <code>LinkedDataArtifact</code> to manage pending lookups:
 * </p>
 * <ul>
 *     <li><code>to_visit(URI)</code> is added after a call to {@link #get(String) get(URI)} and removed after the lookup has ended.</li>
 *     <li><code>visited(URI)</code> is added once a lookup has ended.</li>
 *     <li><code>crawler_status(idling|crawling|error)</code> is updated depending on whether lookups are pending or not.</li>
 * </ul>
 *
 * <p>
 *   Once a resource is visited, the RDF statements found under its URI are added as observable properties of the
 *   <code>LinkedDataArtifact</code>. RDF statements have the form
 * </p>
 * <pre>rdf(S, P, O)[ crawler_source(URI) ] .</pre>
 * <p>
 *   where <code>S</code>, <code>P</code> and <code>O</code> are the subject, predicate and object of
 *   the RDF statement. The <code>crawler_source</code> annotation (within square brackets) holds the URI of the
 *   source for that statement, i.e. the URI given as argument of {@link #get(String) get(URI)}. A further annotation
 *   (<code>rdf_type_map</code>) gives the node type of each element of the triple (see {@link RDFTripleWrapper}
 *   for more details).
 * </p>
 *
 * <h4>Processing OWL and Reasoning</h4>
 *
 * <p>
 *   Moreover, the <code>LinkedDataArtifact</code> keeps track of OWL ontologies that are being crawled.
 *   Whenever an OWL ontology is crawled, subsequent lookups may yield more predicates. For instance, if an OWL
 *   definition for the class <code>ex:Person</code> is found, any RDF statement
 * </p>
 * <pre>&lt;x&gt; a ex:Person .</pre>
 * <p>
 *   will yield the observable property
 * </p>
 * <pre>person("x") .</pre>
 * <p>
 *   based on the OWL definition of <code>ex:Person</code>. The naming strategy exist to map OWL class/property
 *   definitions to predicate names. See {@link NamingStrategyFactory} for more details.
 * </p>
 *
 * <p>
 *   <em>Note: <code>person("X")</code> won't be defined as an observable property if the RDF statement is crawled
 *   <u>before</u> the OWL definition of <code>ex:Person</code>, however.</em>
 * </p>
 *
 * <p>
 *   OWL ontologies may also include axioms from which new predicates can be derived. For instance,
 * </p>
 * <pre>ex:Person rdfs:subClassOf ex:LivingBeing .</pre>
 * <p>
 *   stating that every person is also a living being. The <code>LinkedDataArtifact</code> can be
 *   initialized with an OWL reasoner that would assert derived predicates with an annotation, as follows:
 * </p>
 * <pre>livingBeing("x")[ inferred ] .</pre>
 *
 * <p>
 *   If the <code>LinkedDataArtifact</code> uses an OWL reasoner, it may happen that the set of crawled statements
 *   is logically inconsistent (meaning that no derivation is possible). In this case, the observable property
 *   <code>kb_inconsistent</code> is added.
 * </p>
 *
 * <p>
 *     See <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/fayol"><code>examples/fayol</code></a>
 *     for an example with Linked Data browsing.
 *     TODO update example
 * </p>
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataArtifact extends Artifact {

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
					RDFTripleWrapper w = new RDFTripleWrapper(st);
					String name = w.getPropertyName();
					Object[] args = w.getPropertyArguments();

					ObsProperty prop = defineObsProperty(name, args[0], args[1], args[2]);

					StringTerm origin = ASSyntax.createString(res.getURI());
					prop.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, origin));

					for (Term t : w.getPropertyAnnotations()) prop.addAnnot(t);
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
						Atom annotation = ASSyntax.createAtom(INFERRED_FUNCTOR);
						p.addAnnot(annotation);
					}
				}
			}

			removeObsPropertyByTemplate(TO_VISIT_FUNCTOR, res.getURI());
			defineObsProperty(VISITED_FUNCTOR, res.getURI());

			updateCrawlerStatus();

			endExternalSession(true);
		}

	}

	public static final String CRAWLER_STATUS_FUNCTOR = "crawler_status";

	public static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

	public static final String SOURCE_FUNCTOR = "crawler_source";

	public static final String VISITED_FUNCTOR = "visited";

	public static final String TO_VISIT_FUNCTOR = "to_visit";

	public static final String INFERRED_FUNCTOR = "inferred";

	public static final String KB_INCONSISTENT_FUNCTOR = "kb_inconsistent";

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
	 * <p>
	 *   Expose the transformation function from a resource URI to its parent resource URI (without fragment, if any).
	 *   For instance, the parent resource of
	 * </p>
	 * <pre>http://example.org/alice#me</pre>
	 * <p>is</p>
	 * <pre>http://example.org/alice</pre>
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
	 * Perform a GET request to retrieve an RDF representation of the provided resource.
	 * Add (asynchronously) the found RDF representation to the belief base.
	 */
	@OPERATION
	public void get(String resourceURI) {
		try {
			// force crawler status to true
			updateCrawlerStatus(true);

			String requestedURI = withoutFragment(resourceURI);

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
