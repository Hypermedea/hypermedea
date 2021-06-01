package ontologyAPI;

import classes.OntologyIRIHolder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class OntologyExtractionManager {
    public static OWLOntology extractOntology(String uri, boolean local) throws OWLOntologyCreationException {
        // Get hold of an ontology manager
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();

        OWLOntology owlOntology = null;
        if (local){
            try {
                owlOntology = manager.loadOntologyFromOntologyDocument(new File(uri));
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }

        } else {
            try {
                // Load an ontology from the Web.  We load the ontology from a document IRI
                IRI docIRI = IRI.create(uri);
                owlOntology = manager.loadOntologyFromOntologyDocument(docIRI);
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }

        return owlOntology;
    }

    public static OWLOntology extractOntologyFromSetIRI(HashMap<String, OntologyIRIHolder> listIRIOntology, String mergedOntologyIRI) {
        OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLOntology mergedOntology = null;
        HashSet<OWLAxiom> owlAxiomSet = new HashSet<>();

        for (OntologyIRIHolder iriOntology : listIRIOntology.values()){
            if (iriOntology.getOrigin().equals(OntologyIRIHolder.distant)){
                IRI docIRI = IRI.create(iriOntology.getIRI());
                try {
                    OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(docIRI);
                    owlAxiomSet.addAll(owlOntology.getAxioms());
                    owlOntologyManager = OWLManager.createOWLOntologyManager();
                } catch (OWLOntologyCreationException e) {
                    e.printStackTrace();
                }
            } else if (iriOntology.getOrigin().equals(OntologyIRIHolder.local)){
                try {
                    OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(new File(iriOntology.getIRI()));
                    owlAxiomSet.addAll(owlOntology.getAxioms());
                    owlOntologyManager = OWLManager.createOWLOntologyManager();
                } catch (OWLOntologyCreationException e) {
                    e.printStackTrace();
                }
            }
        }

        try {
            mergedOntology = owlOntologyManager.createOntology(owlAxiomSet, IRI.create(mergedOntologyIRI));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return mergedOntology;
    }
}
