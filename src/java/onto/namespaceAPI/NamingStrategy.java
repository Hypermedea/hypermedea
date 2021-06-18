package onto.namespaceAPI;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;

/**
 * Interface providing basic methods for retrieving a name from an IRI.
 *
 * @author Victor Charpenay, Noe SAFFAF
 *
 */
public interface NamingStrategy {
    /**
     * An init method that should be run once at the very start to setup ontology-independent strategies
     */
    void init();

    /**
     * A method to set up ontology-dependent strategies (ie : maps for labels)
     * @param ontology
     */
    void precompute(OWLOntology ontology);

    /**
     * A method that return a compact name based on the IRI (should be called after init and precompute)
     * @param iri an arbitrary IRI
     * @return a compact name of the IRI
     */
    String getNameForIRI(IRI iri);
}
