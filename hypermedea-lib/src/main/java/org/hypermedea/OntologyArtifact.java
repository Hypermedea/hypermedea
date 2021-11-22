package org.hypermedea;

import cartago.ObsProperty;
import com.github.owlcs.ontapi.OntManagers;
import com.github.owlcs.ontapi.Ontology;
import com.github.owlcs.ontapi.OntologyManager;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Atom;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Structure;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
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

import java.util.*;

/**
 * <p>
 *     Artifact to manage OWL vocabularies (class and property names) and to perform reasoning over OWL axioms.
 * </p>
 *
 * <p>
 *   Moreover, the <code>OntologyArtifact</code> keeps track of OWL ontologies that are being crawled.
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
 *   stating that every person is also a living being. The <code>OntologyArtifact</code> can be
 *   initialized with an OWL reasoner that would assert derived predicates with an annotation, as follows:
 * </p>
 * <pre>livingBeing("x")[ inferred ] .</pre>
 *
 * <p>
 *   If the <code>OntologyArtifact</code> uses an OWL reasoner, it may happen that the set of crawled statements
 *   is logically inconsistent (meaning that no derivation is possible). In this case, the observable property
 *   <code>kb_inconsistent</code> is added.
 * </p>
 *
 * <p>
 *     See <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/fayol"><code>examples/fayol</code></a>
 *     for an example with OWL reasoning.
 * </p>
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class OntologyArtifact extends HypermedeaArtifact {

    /**
     * Manager that listens to incoming resources from the Linked Data crawler
     * and adds corresponding observable properties.
     */
    private class OWLObsPropertyManager implements RequestListener {

        @Override
        public void requestCompleted(Resource res) {
            beginExternalSession();
            updateReasonerStatus(true);
            endExternalSession(true);

            beginExternalSession();

            if (res.getRepresentation() != null) {
                IRI iri = IRI.create(res.getURI());
                List<OWLOntologyChange> changes = new ArrayList<>();

                if (ontologyManager.contains(iri)) {
                    // ontology previously crawled
                    ontologyManager.removeOntology(ontologyManager.getOntology(iri));

                    changes.add(new RemoveImport(rootOntology, dataFactory.getOWLImportsDeclaration(iri)));
                    ontologyManager.applyChanges(changes);
                    changes.clear();
                }

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

                changes.add(new SetOntologyID(o, iri));
                changes.add(new AddImport(rootOntology, dataFactory.getOWLImportsDeclaration(iri)));

                ontologyManager.applyChanges(changes);

                reasoner.flush();
                updateKbInconsistent(reasoner.isConsistent());

                definePropertiesForAxioms(o.getABoxAxioms(Imports.EXCLUDED));

                if (reasoner.isConsistent()) {
                    // FIXME this code block significantly slows down crawling
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

            endExternalSession(true);

            beginExternalSession();
            updateReasonerStatus(false);
            endExternalSession(true);
        }

    }

    public static final String REASONER_STATUS_FUNCTOR = "reasoner_status";

    public static final String KB_INCONSISTENT_FUNCTOR = "kb_inconsistent";

    public static final String PREDICATE_IRI_FUNCTOR = "predicate_uri";

    public static final String INFERRED_FUNCTOR = "inferred";

    private OWLReasoner reasoner;

    /**
     * Note: the ontology manager seems to be a singleton with its own lifecycle: a new instance of
     * <code>OntologyArtifact</code> shares the same pre-loaded ontologies with all other instances.
     */
    private OntologyManager ontologyManager = OntManagers.createManager();

    private OWLDataFactory dataFactory = ontologyManager.getOWLDataFactory();

    private Ontology rootOntology;

    private Set<OWLEntity> kbSignature = new HashSet<>();

    private ShortFormProvider namingStrategy;

    private ObsProperty reasonerStatus;

    /**
     * Initialize the artifact without inference, i.e. call {@link #init(boolean) init(false)}).
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
        rootOntology = ontologyManager.createOntology();

        OWLReasonerFactory f = withInference
                // HermiT reasoner (OWL DL)
                ? new ReasonerFactory()
                // no reasoner (no implicit axiom inferred from the ontology's structure)
                : new StructuralReasonerFactory();

        reasoner = f.createReasoner(rootOntology);

        namingStrategy = NamingStrategyFactory.createDefaultNamingStrategy(ontologyManager);

        reasonerStatus = defineObsProperty(REASONER_STATUS_FUNCTOR, false);

        crawlerListener = new OWLObsPropertyManager();

        super.init();
    }

    private void updateReasonerStatus(Boolean status) {
        if (!reasonerStatus.getValue().equals(status)) reasonerStatus.updateValue(status);
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

            ObsProperty p = defineObsPropertyIfAbsent(name, args);

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

    private ObsProperty defineObsPropertyIfAbsent(String name, Object[] args) {
        if (args.length == 1 && !hasObsPropertyByTemplate(name, args[0]))
            return defineObsProperty(name, args[0]);
        else if (args.length == 2 && !hasObsPropertyByTemplate(name, args[0], args[1]))
            return defineObsProperty(name, args[0], args[1]);
        else
            return null;
    }

}
