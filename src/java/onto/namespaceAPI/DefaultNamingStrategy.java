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
    public String getNameForIRI(IRI iri) {
        if (iri == null || iri.toString().equals("")) return null;

        return IRITools.getSuffixIri(iri.toString(),true);
    }

}
