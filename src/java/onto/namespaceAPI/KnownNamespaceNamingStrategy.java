package onto.namespaceAPI;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLOntology;
import tools.CSVTools;
import tools.IRITools;
;
import java.util.Map;
/**
 * Strategy
 * @author Noe SAFFAF
 */
public class KnownNamespaceNamingStrategy implements NamingStrategy {

    private Map<String,String> mappedKnownNamespaces = CSVTools.readCSVtoMap("prefixes.csv");

    @Override
    public String getNameForIRI(IRI iri) {
        if (iri == null || iri.toString() == ""){
            return null;
        } else if (mappedKnownNamespaces.containsKey(iri.toString())) {
            if (iri.toString().startsWith(mappedKnownNamespaces.get(iri.toString()))) {
                String suffix = IRITools.getNameByMatchingPrefix(iri.toString(),mappedKnownNamespaces.get(iri.toString()));
                return IRITools.firstCharToLowerCase(suffix);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
