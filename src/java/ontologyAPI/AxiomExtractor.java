package ontologyAPI;

import classes.Axiom_Jason;
import classes.ComparableArrayList;
import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class AxiomExtractor {

    public static Set<OWLClass> extractClasses(OWLOntology owlOntology){
        return owlOntology.getClassesInSignature();
    }

    public static Set<OWLAxiom> extractAxioms(OWLOntology owlOntology){
        return owlOntology.getAxioms();
    }


    public static HashMap<String,String> extractLabels(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        HashMap<String, String> mapLabel = new HashMap<>();
        for (OWLClass c : owlClassSet){
            for(OWLAnnotationAssertionAxiom a : owlOntology.getAnnotationAssertionAxioms(c.getIRI())) {
                if(a.getProperty().isLabel()) {
                    if(a.getValue() instanceof OWLLiteral) {
                        String label = ((OWLLiteral) a.getValue()).getLiteral().replaceAll(" ","");
                        //System.out.println(c.toString() + " ::: " + label);
                        mapLabel.put(c.toString(), label);
                    }
                }
            }
        }
        return mapLabel;
    }

    public static HashMap<String,String> extractPreferredNamespaces(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        HashMap<String, String> mapLabel = new HashMap<>();
        for (OWLClass c : owlClassSet){
            for(OWLAxiom a : owlOntology.getAxioms(c)) {
                System.out.println(a);
                System.out.println(a.getAxiomType().toString());

                /*
                if(a.getProperty().toString().equals("vann:preferredNamespaceUri") || a.getProperty().toString().equals("<http://purl.org/vocab/vann/preferredNamespaceUri>")) {
                    if(a.getValue() instanceof OWLLiteral) {
                        String label = ((OWLLiteral) a.getValue()).getLiteral().replaceAll(" ","");
                        mapLabel.put(c.toString(), label);
                    }
                }*/
            }
        }
        return mapLabel;
    }

    public static Set<OWLAxiom> extractAnnotationAxioms(OWLOntology owlOntology){
        return null;
    }

    public static HashSet<Axiom_Jason> extractPredicate(Set<OWLAxiom> owlAxiomSet) {
        HashSet<Axiom_Jason> setPredicate = new HashSet<Axiom_Jason>();
        for (OWLAxiom owlAxiom : owlAxiomSet) {
            if (owlAxiom.isOfType(AxiomType.DECLARATION)){
                OWLDeclarationAxiom owlDeclarationAxiom = (OWLDeclarationAxiom) owlAxiom;
                String axiomFullName = owlDeclarationAxiom.getEntity().getEntityType().getName();
                String axiomEntity = owlDeclarationAxiom.getEntity().toString();
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(axiomEntity);
                Axiom_Jason axiom = new Axiom_Jason(content, axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }

            } else if (owlAxiom.isOfType(AxiomType.CLASS_ASSERTION)){
                OWLClassAssertionAxiom owlClassAssertionAxiom = (OWLClassAssertionAxiom) owlAxiom;
                String axiomFullName = owlClassAssertionAxiom.getClassExpression().toString();
                String axiomEntity = owlClassAssertionAxiom.getIndividual().toString();
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(axiomEntity);
                Axiom_Jason axiom = new Axiom_Jason(content,axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            } else if (owlAxiom.isOfType(AxiomType.ANNOTATION_ASSERTION)) {
                OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom = (OWLAnnotationAssertionAxiom) owlAxiom;
                String axiomFullName =owlAnnotationAssertionAxiom.getProperty().toString();
                String subject = owlAnnotationAssertionAxiom.getSubject().toString();
                String object = owlAnnotationAssertionAxiom.getValue().toString();
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(subject);
                content.add(object);
                Axiom_Jason axiom = new Axiom_Jason(content, axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }
        }
        return setPredicate;
    }
}
