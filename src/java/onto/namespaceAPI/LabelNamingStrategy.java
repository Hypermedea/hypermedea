package onto.namespaceAPI;

import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy based on the label of class, ie: if a class that has a defined label, it will be used as the shortened name
 * @author Noé SAFFAF
 */
public class LabelNamingStrategy implements NamingStrategy {

    private Map<String,String> mappedLabels = new HashMap<>();

    public LabelNamingStrategy(OWLOntology owlOntology) {
        mappedLabels.clear();
        for (OWLClass c : owlOntology.getClassesInSignature()){
            for(OWLAnnotationAssertionAxiom a : owlOntology.getAnnotationAssertionAxioms(c.getIRI())) {
                if(a.getProperty().isLabel()) {
                    if(a.getValue() instanceof OWLLiteral) {
                        String label = ((OWLLiteral) a.getValue()).getLiteral().replaceAll(" ","_");
                        mappedLabels.put(IRITools.removeWrapperIRI(c.toString()), label);
                    }
                }
            }
        }
    }

    @Override
    public String getNameForIRI(IRI iri) {
        if (iri == null || iri.toString() == ""){
            return null;
        } else if (mappedLabels.containsKey(iri.toString())){
            if (iri.toString().contains(mappedLabels.get(iri.toString()))) {
                return IRITools.firstCharToLowerCase(mappedLabels.get(iri.toString()));
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
