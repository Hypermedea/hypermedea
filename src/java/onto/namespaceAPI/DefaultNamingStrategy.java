package onto.namespaceAPI;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import tools.IRITools;

/**
 * Strategy based slicing a full IRI by taking the suffix detected before a :,/,# character
 * @author Noe SAFFAF
 */
public class DefaultNamingStrategy implements NamingStrategy {
    @Override
    public void init() {
        return;
    }

    @Override
    public void precompute(OWLOntology ontology) {
        return;
    }

    @Override
    public String getNameForIRI(IRI iri) {
        if (iri == null || iri.toString() == ""){
            return null;
        }
        return IRITools.getSuffixIri(iri.toString(),true);
    }
}
