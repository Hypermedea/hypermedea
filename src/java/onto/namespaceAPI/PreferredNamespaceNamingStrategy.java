package onto.namespaceAPI;

import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategy based on the label of class, ie: if a class that has a defined preferredNamespace/prefix, it will use the regex at the right as the shortened name
 * @author Noé SAFFAF
 */
public class PreferredNamespaceNamingStrategy implements NamingStrategy{

    public Map<String,String> mappedPreferredNamespaces;

    public PreferredNamespaceNamingStrategy() {
    }

    @Override
    public void init() {
        mappedPreferredNamespaces = new HashMap<>();
        return;
    }

    @Override
    public void precompute(OWLOntology owlOntology) {
        mappedPreferredNamespaces.clear();
        for(OWLClass owlClass : owlOntology.getClassesInSignature()) {
            for (OWLAnnotation annotation : owlClass.getAnnotations(owlOntology)) {
                //System.out.println(IRITools.removeWrapperUri(owlClass.toString()) + " ::: "+ ((OWLLiteral) annotation.getValue()).getLiteral());
                if (IRITools.removeWrapperIRI(annotation.getProperty().toString()).equals("http://purl.org/vocab/vann/preferredNamespaceUri")
                        || IRITools.removeWrapperIRI(annotation.getProperty().toString()).equals("http://purl.org/vocab/vann/preferredNamespacePrefix")){
                    mappedPreferredNamespaces.put(IRITools.removeWrapperIRI(owlClass.toString()),((OWLLiteral) annotation.getValue()).getLiteral());
                }
            }
        }
    }

    @Override
    public String getNameForIRI(IRI iri) {
        if (iri == null || iri.toString() == ""){
            return null;
        } else if (mappedPreferredNamespaces.containsKey(iri.toString())){
            //System.out.println(axiom.getPredicateFullName() + " : " + mappedPreferredNamespaces.get(axiom.getPredicateFullName()));
            if (iri.toString().startsWith(mappedPreferredNamespaces.get(iri.toString()))) {
                String suffix = IRITools.getNameByMatchingPrefix(iri.toString(),mappedPreferredNamespaces.get(iri.toString()));
                return IRITools.firstCharToLowerCase(suffix);
            } else {
                return null;
            }
        } else {
            return null;
        }
    }
}
