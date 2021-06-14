package onto.ontologyAPI;

import onto.classes.*;
import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.*;

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



    public static Map<String,String> extractLabels(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        Map<String, String> mapLabel = new HashMap<>();
        for (OWLClass c : owlClassSet){
            for(OWLAnnotationAssertionAxiom a : owlOntology.getAnnotationAssertionAxioms(c.getIRI())) {
                if(a.getProperty().isLabel()) {
                    if(a.getValue() instanceof OWLLiteral) {
                        String label = ((OWLLiteral) a.getValue()).getLiteral().replaceAll(" ","_");
                        mapLabel.put(IRITools.removeWrapperUri(c.toString()), label);
                    }
                }
            }
        }
        return mapLabel;
    }

    public static Map<String,String> extractPreferredNamespaces(Set<OWLClass> owlClassSet, OWLOntology owlOntology){
        Map<String, String> mapPreferredNamespace = new HashMap<>();
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

    public static HashSet<OWLAxiomJasonWrapper> extractPredicate(OWLOntology owlOntology, Set<OWLClass> owlClassSet) {
        HashSet<OWLAxiomJasonWrapper> setPredicate = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLClass c : owlClassSet) {
            for (OWLDeclarationAxiom owlDeclarationAxiom : owlOntology.getDeclarationAxioms(c)) {
                String axiomFullName = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().getEntityType().getName());
                String axiomTerm = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().toString());
                OWLAxiomJasonWrapper axiom = new OWLUnaryAxiomJasonWrapper(axiomFullName, axiomTerm);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }

            for (OWLClassAssertionAxiom owlClassAssertionAxiom : owlOntology.getClassAssertionAxioms(c)) {
                String axiomFullName = IRITools.removeWrapperUri(owlClassAssertionAxiom.getClassExpression().toString());
                String axiomTerm = IRITools.removeWrapperUri(owlClassAssertionAxiom.getIndividual().toString());
                OWLAxiomJasonWrapper axiom = new OWLUnaryAxiomJasonWrapper(axiomFullName, axiomTerm);
                if (!setPredicate.contains(axiom)) {
                    setPredicate.add(axiom);
                }
            }

            for (OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom: owlOntology.getAnnotationAssertionAxioms(c.getIRI())) {
                String axiomFullName = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getProperty().toString());
                String axiomTermSubject = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getSubject().toString());
                String axiomTermObject = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getValue().toString());
                OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(axiomFullName,axiomTermSubject,axiomTermObject);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }

            for (OWLAnnotation owlAnnotation : c.getAnnotations(owlOntology)){
                String axiomFullName = IRITools.removeWrapperUri(owlAnnotation.getProperty().toString());
                String axiomTermSubject = IRITools.removeWrapperUri(c.toString());
                String axiomTermObject = IRITools.removeWrapperUri(owlAnnotation.getValue().toString());
                OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(axiomFullName,axiomTermSubject,axiomTermObject);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }
        }

        return setPredicate;
    }

    public static HashSet<OWLAxiomJasonWrapper> extractAxiomFromTriplet(List<Triple> listTriple, OWLOntology owlOntology, boolean inferred, OWLOntologyManager manager){

        HashSet<OWLAxiomJasonWrapper> setOWLAxiomJasonWrapper = new HashSet<>();
        Set<OWLClass> owlClasses = owlOntology.getClassesInSignature();
        Set<OWLObjectProperty> owlObjectProperties = owlOntology.getObjectPropertiesInSignature();
        Set<OWLDataProperty> owlDataProperties = owlOntology.getDataPropertiesInSignature();
        OWLDataFactory dataFactory = manager.getOWLDataFactory();

        for (Triple triple : listTriple)
        {
            //Check all individual if they are assertion of registered class
            for (OWLClass c : owlClasses) {
                //System.out.println(rdfType + " ::: "+ );
                if (triple.predicate.equals(rdfType) && IRITools.removeWrapperUri(c.toString()).equals(triple.object)){
                    //System.out.println(IRITools.removeWrapperUri(c.toString()) + " ::: " + triplet.subject);
                    if (inferred){
                        OWLNamedIndividual namedIndividual = dataFactory.getOWLNamedIndividual(IRI.create(triple.subject));
                        OWLClassAssertionAxiom classAssertion = dataFactory.getOWLClassAssertionAxiom(c, namedIndividual);
                        manager.addAxiom(owlOntology, classAssertion);
                    } else {
                        String axiomFullName = triple.object;
                        String axiomTerm = triple.subject;
                        setOWLAxiomJasonWrapper.add(new OWLUnaryAxiomJasonWrapper(axiomFullName, axiomTerm));
                    }
                }
            }

            //Check all properties if they are registered properties (ObjectProp + DataProp)
            for (OWLObjectProperty op : owlObjectProperties){
                if (triple.predicate.equals(IRITools.removeWrapperUri(op.toString()))){
                    //System.out.println(triplet.subject+ " ::: " + IRITools.removeWrapperUri(op.toString()) + " ::: " + triplet.object);
                    String axiomFullName = triple.predicate;
                    String axiomTermSubject = triple.subject;
                    String axiomTermObject = triple.object;
                    setOWLAxiomJasonWrapper.add(new OWLBinaryAxiomJasonWrapper(axiomFullName, axiomTermSubject, axiomTermObject));
                }
            }

            for (OWLDataProperty dp : owlDataProperties){
                if (triple.predicate.equals(IRITools.removeWrapperUri(dp.toString()))){
                    //System.out.println(triplet.subject+ " ::: " + IRITools.removeWrapperUri(dp.toString()) + " ::: " + triplet.object);
                    String axiomFullName = triple.predicate;
                    String axiomTermSubject = triple.subject;
                    String axiomTermObject = triple.object;
                    setOWLAxiomJasonWrapper.add(new OWLBinaryAxiomJasonWrapper(axiomFullName, axiomTermSubject, axiomTermObject));
                }
            }
        }

        if (inferred) {
            InferredAxiomExtractor inferredAxiomExtractor = new InferredAxiomExtractor(owlOntology);
            inferredAxiomExtractor.precomputeInferredAxioms();
            setOWLAxiomJasonWrapper.addAll(inferredAxiomExtractor.getInferredTypes());
        }
        return setOWLAxiomJasonWrapper;
    }

    /*
    public static HashSet<OWLAxiomJasonWrapper> extractPredicate(OWLOntology owlOntology, OWLClass c) {
        HashSet<OWLAxiomJasonWrapper> setPredicate = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLDeclarationAxiom owlDeclarationAxiom : owlOntology.getDeclarationAxioms(c)) {
            if (owlAxiom.isOfType(AxiomType.DECLARATION)){
                OWLDeclarationAxiom owlDeclarationAxiom = (OWLDeclarationAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().getEntityType().getName());
                String axiomTerm = IRITools.removeWrapperUri(owlDeclarationAxiom.getEntity().toString());
                OWLAxiomJasonWrapper axiom = new OWLUnaryAxiomJasonWrapper(axiomFullName, axiomTerm);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }

            } else if (owlAxiom.isOfType(AxiomType.CLASS_ASSERTION)){
                OWLClassAssertionAxiom owlClassAssertionAxiom = (OWLClassAssertionAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlClassAssertionAxiom.getClassExpression().toString());
                String axiomTerm = IRITools.removeWrapperUri(owlClassAssertionAxiom.getIndividual().toString());
                OWLAxiomJasonWrapper axiom = new OWLUnaryAxiomJasonWrapper(axiomFullName,axiomTerm);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            } else if (owlAxiom.isOfType(AxiomType.ANNOTATION_ASSERTION)) {
                OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom = (OWLAnnotationAssertionAxiom) owlAxiom;
                String axiomFullName = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getProperty().toString());
                String axiomTermSubject = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getSubject().toString());
                String axiomTermObject = IRITools.removeWrapperUri(owlAnnotationAssertionAxiom.getValue().toString());
                OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(axiomFullName,axiomTermSubject,axiomTermObject);
                if (!setPredicate.contains(axiom)){
                    setPredicate.add(axiom);
                }
            }
        }
        return setPredicate;
    }
    */
}
