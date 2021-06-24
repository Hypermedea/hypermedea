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
     * A method that returns a valid property name based on the IRI (should be called after init and precompute)
     * @param iri an arbitrary IRI
     * @return a compact name of the IRI
     */
    String getNameForIRI(IRI iri);

}
