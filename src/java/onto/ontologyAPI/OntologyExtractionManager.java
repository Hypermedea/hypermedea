package onto.ontologyAPI;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;

import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * A set of static utilities methods for ontologies
 * @author Noé SAFFAF
 */
public class OntologyExtractionManager {
    public static OWLOntology extractOntologyFromRegisteredSet(Set<String> registeredOntologySet) {
        Set<OWLAxiom> owlAxiomSet = new HashSet<>();
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

    public static OWLOntology addOntology(String originURI, OWLOntology rootOntology, Set<String> registeredOntologySet) {
        Set<OWLAxiom> owlAxiomSet = new HashSet<>();
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

    public static OWLOntology addAxiomToOntology(Set<OWLAxiom> owlAxiomSet, OWLOntology ontology) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        Set<OWLAxiom> newOwlAxiom = new HashSet<>();
        newOwlAxiom.addAll(ontology.getAxioms());
        newOwlAxiom.addAll(owlAxiomSet);
        return manager.createOntology(newOwlAxiom);
    }

    public static OWLOntology copyOntology(OWLOntology owlOntology) throws OWLOntologyCreationException {
        OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
        return manager.createOntology(owlOntology.getAxioms());
    }
}
