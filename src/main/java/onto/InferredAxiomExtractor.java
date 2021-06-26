package onto;

import org.semanticweb.HermiT.Reasoner.ReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.reasoner.InferenceType;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.OWLReasonerFactory;
import org.semanticweb.owlapi.util.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * A class to get Inferred Ontology, as well as other properties of an ontology based on a reasoner
 * @author Noé SAFFAF
 */
public class InferredAxiomExtractor {
    OWLOntology owlOntology;
    OWLReasoner reasoner;

    public InferredAxiomExtractor(OWLOntology owlOntology, OWLReasonerFactory reasonerFactory){
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

    public boolean checkSatisfiability(){
        return (reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom().size() == 0);
    }

    public Set<OWLClass> getUnsatisfiableClasses(){
        return reasoner.getUnsatisfiableClasses().getEntitiesMinusBottom();
    }


    public OWLOntology getInferredOntology() {
        ReasonerFactory reasonerFactory = new ReasonerFactory();
        OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        try {
            OWLOntology newOwlOntology = owlOntologyManager.createOntology(owlOntology.getAxioms());
            OWLReasoner reasoner = reasonerFactory.createReasoner(newOwlOntology);

            reasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
            reasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
            reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);
            reasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
            reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_HIERARCHY);
            reasoner.precomputeInferences(InferenceType.DATA_PROPERTY_ASSERTIONS);

            List<InferredAxiomGenerator<? extends OWLAxiom>> inferredAxiomGeneratorList = new ArrayList<InferredAxiomGenerator<? extends OWLAxiom>>();
            inferredAxiomGeneratorList.add(new InferredSubClassAxiomGenerator());
            inferredAxiomGeneratorList.add(new InferredClassAssertionAxiomGenerator());
            inferredAxiomGeneratorList.add(new InferredSubDataPropertyAxiomGenerator());
            inferredAxiomGeneratorList.add(new InferredSubObjectPropertyAxiomGenerator());
            InferredOntologyGenerator inferredGenerator = new InferredOntologyGenerator(reasoner, inferredAxiomGeneratorList);
            inferredGenerator.fillOntology(owlOntologyManager, newOwlOntology);

            return newOwlOntology;
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return null;
        }
    }
}
