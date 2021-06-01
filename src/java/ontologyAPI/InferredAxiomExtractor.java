package ontologyAPI;

import classes.Axiom_Jason;
import classes.ComparableArrayList;
import classes.SatisfiableResponse;
import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import tools.BeliefsSyntaxTools;
import tools.IRITools;

import java.util.HashSet;

public class InferredAxiomExtractor {
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

    public HashSet<Axiom_Jason> getInferredSuperclasses(){
        HashSet<Axiom_Jason> axiomSet = new HashSet<Axiom_Jason>();
        for (OWLClass c : owlOntology.getClassesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLClass> superClasses = reasoner.getSuperClasses(c,false);
            for (OWLClass superClass : superClasses.getFlattened()) {
                //System.out.println(c.toString() +" :: " +superClass.toString());
                if (!superClass.toString().contains("owl:Thing") && !superClass.toString().contains("owl#Thing")){
                    ComparableArrayList<String> content = new ComparableArrayList<String>();
                    content.add(c.toString());
                    content.add(superClass.toString());
                    Axiom_Jason axiom = new Axiom_Jason(content, "<http://www.w3.org/2000/01/rdf-schema#SubClassOf>");
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<Axiom_Jason> getInferredTypes(){
        HashSet<Axiom_Jason> axiomSet = new HashSet<Axiom_Jason>();
        for (OWLNamedIndividual n : owlOntology.getIndividualsInSignature()) {
            NodeSet<OWLClass> classTypeSet = reasoner.getTypes(n, false);
            // the boolean argument specifies direct subclasses
            for (OWLClass owlClass : classTypeSet.getFlattened()) {
                if (!owlClass.toString().contains("owl:Thing") && !owlClass.toString().contains("owl#Thing")) {
                    ComparableArrayList<String> content = new ComparableArrayList<String>();
                    content.add(n.toString());
                    String axiomFullName = owlClass.toString();
                    Axiom_Jason axiom = new Axiom_Jason( content, axiomFullName);
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<Axiom_Jason> getInferredObjectProperties() {
        HashSet<Axiom_Jason> axiomSet = new HashSet<Axiom_Jason>();
        for (OWLObjectProperty o : owlOntology.getObjectPropertiesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLObjectPropertyExpression> superObjectProperties = reasoner.getSuperObjectProperties(o, false);
            for (OWLObjectPropertyExpression superObjectProperty : superObjectProperties.getFlattened()) {
                if (!superObjectProperty.toString().contains("owl:topObjectProperty") && !superObjectProperty.toString().contains("owl#topObjectProperty")) {
                    ComparableArrayList<String> content = new ComparableArrayList<String>();
                    content.add(o.toString());
                    content.add(superObjectProperty.toString());
                    Axiom_Jason axiom = new Axiom_Jason(content,"<http://www.w3.org/2000/01/rdf-schema#subPropertyOf>");
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }

    public HashSet<Axiom_Jason> getInferredDataProperties() {
        HashSet<Axiom_Jason> axiomSet = new HashSet<Axiom_Jason>();
        for (OWLDataProperty d : owlOntology.getDataPropertiesInSignature()) {
            // the boolean argument specifies direct subclasses
            NodeSet<OWLDataProperty> superDataProperties = reasoner.getSuperDataProperties(d,false);
            for (OWLDataProperty superDataProperty : superDataProperties.getFlattened()) {
                if (!superDataProperty.toString().contains("owl:topDataProperty") && !superDataProperty.toString().contains("owl#topDataProperty")) {
                    ComparableArrayList<String> content = new ComparableArrayList<String>();
                    content.add(d.toString());
                    content.add(superDataProperty.toString());
                    Axiom_Jason axiom = new Axiom_Jason(content,"<http://www.w3.org/2000/01/rdf-schema#subPropertyOf>");
                    if (!axiomSet.contains(axiom)){
                        axiomSet.add(axiom);
                    }
                }
            }
        }
        return axiomSet;
    }
}
