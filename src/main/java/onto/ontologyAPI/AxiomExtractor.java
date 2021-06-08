package onto.ontologyAPI;

import onto.classes.AxiomJason;
import onto.classes.ComparableArrayList;
import onto.classes.Triplet;
import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A class to extract axioms from an ontology
 */
public class AxiomExtractor {

    private static final String rdfType = "http://www.w3.org/1999/02/22-rdf-syntax-ns#type";

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
                        String label = ((OWLLiteral) a.getValue()).getLiteral().replaceAll(" ","_");
                        //System.out.println(c.toString() + " ::: " + label);
                        mapLabel.put(IRITools.removeWrapperUri(c.toString()), label);
                    }
                }
            }
        }
        return mapLabel;
    }

    public static HashMap<String,String> extractPreferredNamespaces(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        HashMap<String, String> mapPreferredNamespace = new HashMap<>();
        for(OWLClass owlClass : owlClassSet) {
            for (OWLAnnotation annotation : owlClass.getAnnotations(owlOntology)) {
                //System.out.println(IRITools.removeWrapperUri(owlClass.toString()) + " ::: "+ ((OWLLiteral) annotation.getValue()).getLiteral());
                if (IRITools.removeWrapperUri(annotation.getProperty().toString()).equals("http://purl.org/vocab/vann/preferredNamespaceUri")
                || IRITools.removeWrapperUri(annotation.getProperty().toString()).equals("http://purl.org/vocab/vann/preferredNamespacePrefix")){
                    mapPreferredNamespace.put(IRITools.removeWrapperUri(owlClass.toString()),((OWLLiteral) annotation.getValue()).getLiteral());
                    //System.out.println(owlClass.toString() + " ::: " + annotation);
                }
            }
        }
        return mapPreferredNamespace;
    }

    public static Set<OWLAxiom> extractAnnotationAxioms(OWLOntology owlOntology){
        return null;
    }

    public static HashSet<AxiomJason> extractPredicate(Set<OWLAxiom> owlAxiomSet) {
        HashSet<AxiomJason> setPredicate = new HashSet<AxiomJason>();
        for (OWLAxiom owlAxiom : owlAxiomSet) {
            if (owlAxiom.isOfType(AxiomType.DECLARATION)){
                OWLDeclarationAxiom owlDeclarationAxiom = (OWLDeclarationAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().getEntityType().getName());
                String axiomEntity = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().toString());
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(axiomEntity);
                AxiomJason axiom = new AxiomJason(content, axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }

            } else if (owlAxiom.isOfType(AxiomType.CLASS_ASSERTION)){
                OWLClassAssertionAxiom owlClassAssertionAxiom = (OWLClassAssertionAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlClassAssertionAxiom.getClassExpression().toString());
                String axiomEntity = IRITools.removeWrapperUri(owlClassAssertionAxiom.getIndividual().toString());
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(axiomEntity);
                AxiomJason axiom = new AxiomJason(content,axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            } else if (owlAxiom.isOfType(AxiomType.ANNOTATION_ASSERTION)) {
                OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom = (OWLAnnotationAssertionAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getProperty().toString());
                String subject = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getSubject().toString());
                String object = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getValue().toString());
                ComparableArrayList<String> content = new ComparableArrayList<String>();
                content.add(subject);
                content.add(object);
                AxiomJason axiom = new AxiomJason(content, axiomFullName);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }
        }
        return setPredicate;
    }

    public static HashSet<AxiomJason> extractAxiomFromTriplet(ArrayList<Triplet> listTriplet, OWLOntology owlOntology, boolean inferred, OWLOntologyManager manager){

        HashSet<AxiomJason> setAxiomJason = new HashSet<>();
        Set<OWLClass> owlClasses = owlOntology.getClassesInSignature();
        Set<OWLObjectProperty> owlObjectProperties = owlOntology.getObjectPropertiesInSignature();
        Set<OWLDataProperty> owlDataProperties = owlOntology.getDataPropertiesInSignature();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        for (Triplet triplet : listTriplet)
        {
            //Check all individual if they are assertion of registered class
            for (OWLClass c : owlClasses) {
                //System.out.println(rdfType + " ::: "+ );
                if (triplet.predicate.equals(rdfType) && IRITools.removeWrapperUri(c.toString()).equals(triplet.object)){
                    //System.out.println(IRITools.removeWrapperUri(c.toString()) + " ::: " + triplet.subject);
                    if (inferred){
                        OWLNamedIndividual mary = dataFactory.getOWLNamedIndividual(IRI.create(triplet.subject));
                        OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(c, mary);
                        manager.addAxiom(owlOntology, classAssertion);
                    } else {
                        String axiomFullName = triplet.object;
                        ComparableArrayList<String> axiomContent = new ComparableArrayList<>();
                        axiomContent.add(triplet.subject);
                        setAxiomJason.add(new AxiomJason(axiomContent, axiomFullName));
                    }
                }
            }

            //Check all properties if they are registered properties (ObjectProp + DataProp
            for (OWLObjectProperty op : owlObjectProperties){
                if (triplet.predicate.equals(IRITools.removeWrapperUri(op.toString()))){
                    //System.out.println(triplet.subject+ " ::: " + IRITools.removeWrapperUri(op.toString()) + " ::: " + triplet.object);
                    String axiomFullName = triplet.predicate;
                    ComparableArrayList<String> axiomContent = new ComparableArrayList<>();
                    axiomContent.add(triplet.subject);
                    axiomContent.add(triplet.object);
                    setAxiomJason.add(new AxiomJason(axiomContent, axiomFullName));
                }
            }

            for (OWLDataProperty dp : owlDataProperties){
                if (triplet.predicate.equals(IRITools.removeWrapperUri(dp.toString()))){
                    //System.out.println(triplet.subject+ " ::: " + IRITools.removeWrapperUri(dp.toString()) + " ::: " + triplet.object);
                    String axiomFullName = triplet.predicate;
                    ComparableArrayList<String> axiomContent = new ComparableArrayList<>();
                    axiomContent.add(triplet.subject);
                    axiomContent.add(triplet.object);
                    setAxiomJason.add(new AxiomJason(axiomContent, axiomFullName));
                }
            }
        }



        if (inferred) {
            InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
            inferredAxiomExtractor.precomputeInferredAxioms();
            setAxiomJason.addAll(inferredAxiomExtractor.getInferredTypes());
        }

        return setAxiomJason;
    }
}
