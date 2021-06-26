package onto;

import org.junit.Test;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.util.ShortFormProvider;
import uk.ac.manchester.cs.owl.owlapi.OWLDataFactoryImpl;
import uk.ac.manchester.cs.owl.owlapi.OWLOntologyManagerImpl;

public class OWLAxiomWrapperTest {

    private static final String TEST_CLASS_IRI = "http://example.org/test#C";

    private static final String TEST_PROPERTY_IRI = "http://example.org/test#P";

    private static final String TEST_INDIVIDUAL_A_IRI = "http://example.org/test#A";

    private static final String TEST_INDIVIDUAL_B_IRI = "http://example.org/test#B";

    private final OWLDataFactory dataFactory = new OWLDataFactoryImpl();

    private final OWLOntologyManager ontologyManager = new OWLOntologyManagerImpl(dataFactory);

    @Test
    public void testGetPropertyNameByIRI() {
        OWLClassAssertionAxiom axiom = getClassAssertion(TEST_CLASS_IRI, TEST_INDIVIDUAL_A_IRI);
        ShortFormProvider strategy = getNamingStrategy(NamingStrategyFactory.NamingStrategyType.BY_IRI);

        OWLAxiomWrapper w = new OWLAxiomWrapper(axiom, strategy);

        assert w.getPropertyName().equals("C");
    }

    private OWLClassAssertionAxiom getClassAssertion(String classIRI, String individualIRI) {
        OWLClass c = dataFactory.getOWLClass(IRI.create(classIRI));
        OWLNamedIndividual i = dataFactory.getOWLNamedIndividual(IRI.create(individualIRI));

        return dataFactory.getOWLClassAssertionAxiom(c, i);
    }

    private ShortFormProvider getNamingStrategy(NamingStrategyFactory.NamingStrategyType type) {
        return NamingStrategyFactory.createNamingStrategy(type, ontologyManager);
    }

}
