package org.hypermedea.owl;

import org.junit.Test;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ShortFormProvider;

import java.net.URISyntaxException;
import java.net.URL;

public class OWLAxiomWrapperTest {

    private final OWLOntologyManager ontologyManager = OWLManager.createOWLOntologyManager();

    private final OWLOntology ontology;

    public OWLAxiomWrapperTest() throws OWLOntologyCreationException, URISyntaxException {
        URL url = getClass().getClassLoader().getResource("test-ontology.ttl");
        ontology = ontologyManager.loadOntologyFromOntologyDocument(IRI.create(url));
    }

    @Test
    public void testGetUnaryPropertyNameByIRI() {
        OWLClassAssertionAxiom axiom = getClassAssertion();
        ShortFormProvider strategy = getNamingStrategy(NamingStrategyFactory.NamingStrategyType.BY_IRI);

        OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, strategy);

        assert w.getPropertyName().equals("c");
    }

    @Test
    public void testGetBinaryPropertyNameByIRI() {
        OWLObjectPropertyAssertionAxiom axiom = getObjectPropertyAssertion();
        ShortFormProvider strategy = getNamingStrategy(NamingStrategyFactory.NamingStrategyType.BY_IRI);

        OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, strategy);

        assert w.getPropertyName().equals("p");
    }

    @Test
    public void testGetPropertyNameByTaggedLabel() {
        OWLClassAssertionAxiom axiom = getClassAssertion();
        ShortFormProvider strategy = getNamingStrategy(NamingStrategyFactory.NamingStrategyType.BY_LABEL);

        OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, strategy);

        assert w.getPropertyName().equals("class");
    }

    @Test
    public void testGetPropertyNameByLabel() {
        OWLObjectPropertyAssertionAxiom axiom = getObjectPropertyAssertion();
        ShortFormProvider strategy = getNamingStrategy(NamingStrategyFactory.NamingStrategyType.BY_LABEL);

        OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, strategy);

        assert w.getPropertyName().equals("objectProperty");
    }

    private OWLClassAssertionAxiom getClassAssertion() {
        for (OWLAxiom axiom : ontology.getAxioms()) {
            if (axiom.isOfType(AxiomType.CLASS_ASSERTION)) return (OWLClassAssertionAxiom) axiom;
        }

        return null;
    }

    private OWLObjectPropertyAssertionAxiom getObjectPropertyAssertion() {
        for (OWLAxiom axiom : ontology.getAxioms()) {
            if (axiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) return (OWLObjectPropertyAssertionAxiom) axiom;
        }

        return null;
    }

    private ShortFormProvider getNamingStrategy(NamingStrategyFactory.NamingStrategyType type) {
        return NamingStrategyFactory.createNamingStrategy(type, ontologyManager);
    }

}
