package onto.ontologyAPI;

import onto.classes.*;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import tools.IRITools;

import java.util.HashSet;

public class InferredAxiomExtractor {
    private static final String subclassStringURI = "http://www.w3.org/2000/01/rdf-schema#SubClassOf";
    private static final String subPropertyStringURI = "http://www.w3.org/2000/01/rdf-schema#subPropertyOf";

    OWLOntology owlOntology;
    OWLReasoner reasoner;
    ReasonerFactory reasonerFactory;

    public InferredAxiomExtractor(OWLOntology owlOntology){
        this.reasonerFactory = new ReasonerFactory();
        this.reasoner = reasonerFactory.createReasoner(owlOntology);
        this.owlOntology = owlOntology;
    }

    public void precomputeInferredAxioms(){
        reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);
        reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
        reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_HIERARCHY);
        reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_ASSERTIONS);
    }

    public boolean checkConsistency(){
        return reasoner.isConsistent();
    }

    public SatisfiableResponse checkSatisfiability(boolean retrieveUnsatisfiableClass){
        if (reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size() > 0) {

            if (retrieveUnsatisfiableClass){
                return new SatisfiableResponse(false, reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom());
            } else {
                return new SatisfiableResponse(false, null);
            }
            /*
            // get root of unstaisfiableClasses, and get their explanations
            BlackBoxExplanation bb = new BlackBoxExplanation(owlOntology, reasonerFactory, reasoner);
            HSTExplanationGenerator multExplanator = new HSTExplanationGenerator(bb);
            CompleteRootDerivedReasoner rdr = new CompleteRootDerivedReasoner(tempManager, reasoner, reasonerFactory);
            */

        } else {
            return new SatisfiableResponse(true,null);
        }
    }

    public HashSet<OWLAxiomJasonWrapper> getInferredSuperclasses(){
        HashSet<OWLAxiomJasonWrapper> axiomSet = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLClass c : owlOntology.getClassesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(c,false);
            for (OWLClass superClass : superClasses.getFlattened()) {
                //System.out.println(c.toString() +" :: " +superClass.toString());
                if (!superClass.toString().contains("owl:Thing") && !superClass.toString().contains("owl#Thing")){
                    String axiomTermSubject = IRITools.removeWrapperUri(IRITools.removeWrapperUri(c.toString()));
                    String axiomTermObject = IRITools.removeWrapperUri(IRITools.removeWrapperUri(superClass.toString()));
                    OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(subclassStringURI, axiomTermSubject, axiomTermObject);
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<OWLAxiomJasonWrapper> getInferredTypes(){
        HashSet<OWLAxiomJasonWrapper> axiomSet = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLNamedIndividual n : owlOntology.getIndividualsInSignature()) {
            NodeSet<OWLClass> classTypeSet = reasoner.getTypes(n, false);
            // the boolean argument specifies direct subclasses
            for (OWLClass owlClass : classTypeSet.getFlattened()) {
                if (!owlClass.toString().contains("owl:Thing") && !owlClass.toString().contains("owl#Thing")) {
                    String axiomTerm = IRITools.removeWrapperUri(n.toString());
                    String axiomFullName = IRITools.removeWrapperUri(owlClass.toString());
                    OWLAxiomJasonWrapper axiom = new OWLUnaryAxiomJasonWrapper(axiomFullName, axiomTerm);
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<OWLAxiomJasonWrapper> getInferredObjectProperties() {
        HashSet<OWLAxiomJasonWrapper> axiomSet = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLObjectProperty o : owlOntology.getObjectPropertiesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLObjectPropertyExpression> superObjectProperties = reasoner.getSuperObjectProperties(o, false);
            for (OWLObjectPropertyExpression superObjectProperty : superObjectProperties.getFlattened()) {
                if (!superObjectProperty.toString().contains("owl:topObjectProperty") && !superObjectProperty.toString().contains("owl#topObjectProperty")) {
                    String axiomTermSubject = IRITools.removeWrapperUri(o.toString());
                    String axiomTermObject = IRITools.removeWrapperUri(superObjectProperty.toString());
                    OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(subPropertyStringURI,axiomTermSubject,axiomTermObject);
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<OWLAxiomJasonWrapper> getInferredDataProperties() {
        HashSet<OWLAxiomJasonWrapper> axiomSet = new HashSet<OWLAxiomJasonWrapper>();
        for (OWLDataProperty d : owlOntology.getDataPropertiesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLDataProperty> superDataProperties = reasoner.getSuperDataProperties(d,false);
            for (OWLDataProperty superDataProperty : superDataProperties.getFlattened()) {
                if (!superDataProperty.toString().contains("owl:topDataProperty") && !superDataProperty.toString().contains("owl#topDataProperty")) {
                    String axiomTermSubject = IRITools.removeWrapperUri(d.toString());
                    String axiomTermObject = IRITools.removeWrapperUri(superDataProperty.toString());
                    OWLAxiomJasonWrapper axiom = new OWLBinaryAxiomJasonWrapper(subPropertyStringURI, axiomTermSubject, axiomTermObject);
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }
}
