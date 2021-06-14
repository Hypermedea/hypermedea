package onto.ontologyAPI;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.io.File;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;

public class OntologyExtractionManager {
    public static OWLOntology extractOntology(String uri, boolean local) throws OWLOntologyCreationException {
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

    public static OWLOntology extractOntologyFromRegisteredSet(HashSet<String> registeredOntologySet) {
        HashSet<OWLAxiom> owlAxiomSet = new HashSet<>();
        IRI docIRI;
        for (String originURI : registeredOntologySet ){
            if (originURI.startsWith("http")){
                docIRI = IRI.create(originURI);
            } else {
                docIRI = IRI.create("file:///" + Paths.get(originURI).toAbsolutePath().toString().replaceAll("\\\\","//"));
            }
            try {
                OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
                OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(docIRI);
                owlAxiomSet.addAll(owlOntology.getAxioms());
            } catch (OWLOntologyCreationException e) {
                e.printStackTrace();
            }
        }

        OWLOntology mergedOntology = null;
        try {
            OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
            mergedOntology = owlOntologyManager.createOntology(owlAxiomSet, IRI.create("mergedOntology"));
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
        }
        return mergedOntology;
    }

    public static OWLOntology addOntology(String originURI, OWLOntology rootOntology, HashSet<String> registeredOntologySet) {

        HashSet<OWLAxiom> owlAxiomSet = new HashSet<>();
        owlAxiomSet.addAll(rootOntology.getAxioms());
        IRI docIRI;
        if (originURI.startsWith("http")){
            docIRI = IRI.create(originURI);
        } else {
            docIRI = IRI.create("file:///" + Paths.get(originURI).toAbsolutePath().toString().replaceAll("\\\\","//"));
        }

        try {
            OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
            OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(docIRI);
            owlAxiomSet.addAll(owlOntology.getAxioms());
            OWLOntology incrementedOntology = owlOntologyManager.createOntology(owlAxiomSet, IRI.create("mergedOntology"));
            registeredOntologySet.add(originURI);
            return incrementedOntology;
        } catch (OWLOntologyCreationException e) {
            e.printStackTrace();
            return rootOntology;
        }
    }

    public static OWLOntology copyOntology(OWLOntology owlOntology) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        return manager.createOntology(owlOntology.getAxioms());
    }
}
