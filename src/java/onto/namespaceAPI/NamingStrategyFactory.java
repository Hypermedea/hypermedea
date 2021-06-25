package onto.namespaceAPI;

import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.AnnotationValueShortFormProvider;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.ShortFormProvider;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;
import org.semanticweb.owlapi.vocab.SKOSVocabulary;
import tools.CSVTools;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NamingStrategyFactory {

    public enum NamingStrategyType {
        /**
         * Take the entity's label if known (skos:prefLabel, rdfs:label), with English as preferred language.
         */
        BY_LABEL,

        /**
         * Take the entity's local name if declared in an ontology that declares a preferred namespace.
         */
        BY_PREFERRED_NAMESPACE,

        /**
         * Take the entity's local name if its IRI starts with a well-known namespace (exported from prefix.cc).
         */
        BY_KNOWN_NAMESPACE,

        /**
         * Take the entity's local name if its IRI is recognized as a hash or slash IRI.
         */
        BY_IRI
    }

    private static final OWLAnnotationProperty SKOS_PREFLABEL;

    private static final OWLAnnotationProperty RDFS_LABEL;

    private static final OWLAnnotationProperty VANN_PREFERRED_NAMESPACE;

    static {
        OWLDataFactory df = new OWLDataFactoryImpl();

        String vannPreferredNamespace = "http://purl.org/vocab/vann/preferredNamespaceUri";

        SKOS_PREFLABEL = df.getOWLAnnotationProperty(SKOSVocabulary.PREFLABEL.getIRI());
        RDFS_LABEL = df.getRDFSLabel();
        VANN_PREFERRED_NAMESPACE = df.getOWLAnnotationProperty(IRI.create(vannPreferredNamespace));
    }

    /**
     * Create a cascade naming strategy with all known strategies (from the most specific to the most generic,
     * in terms of how likely a strategy is to succeed).
     *
     * @return default naming strategy
     */
    public static ShortFormProvider createDefaultNamingStrategy(OWLOntologyManager m) {
        ShortFormProvider byLabel = createNamingStrategy(NamingStrategyType.BY_LABEL, m);
        // TODO byPreferredNamespace
        ShortFormProvider byNamespace = createNamingStrategy(NamingStrategyType.BY_KNOWN_NAMESPACE, m);
        ShortFormProvider byIRI = createNamingStrategy(NamingStrategyType.BY_IRI, m);

        return new CascadeShortFormProvider(byLabel, byNamespace, byIRI);
    }

    /**
     * Create a naming strategy of a certain desired type.
     *
     * @param type the desired strategy type
     * @param m an ontology manager (managing a set of OWL entities)
     * @return a short form provider (i.e. a naming strategy to map an entity's IRI to a string)
     */
    public static ShortFormProvider createNamingStrategy(NamingStrategyType type, OWLOntologyManager m) {
        switch (type) {
            case BY_LABEL:
                List<OWLAnnotationProperty> properties = new ArrayList<>();

                properties.add(SKOS_PREFLABEL);
                properties.add(RDFS_LABEL);

                Map<OWLAnnotationProperty, List<String>> langMap = new HashMap<>();
                List<String> lang = new ArrayList<>();

                lang.add("en"); // TODO others? Make languages parameterizable?
                for (OWLAnnotationProperty p : properties) langMap.put(p, lang);

                return new AnnotationValueShortFormProvider(properties, langMap, m);

            case BY_PREFERRED_NAMESPACE:
                DefaultPrefixManager preferredPrefixManager = new DefaultPrefixManager();
                int inc = 0;

                for (OWLOntology o : m.getOntologies()) {
                    for (OWLAnnotation a : o.getAnnotations()) {
                        if (a.getProperty().equals(VANN_PREFERRED_NAMESPACE)) {
                            String ns = a.getValue().toString();
                            preferredPrefixManager.setPrefix(String.format("ns%d", inc++), ns);
                        }
                    }
                }

                return preferredPrefixManager;

            case BY_KNOWN_NAMESPACE:
                DefaultPrefixManager knownPrefixManager = new DefaultPrefixManager();
                Map<String, String> knownPrefixes = CSVTools.readCSVtoMap("prefixes.csv");

                for (Map.Entry<String, String> kv : knownPrefixes.entrySet()) {
                    knownPrefixManager.setPrefix(kv.getKey(), kv.getValue());
                }

                return knownPrefixManager;

            case BY_IRI:
                return new SimpleShortFormProvider();

            default:
                return null;
        }
    }

}
