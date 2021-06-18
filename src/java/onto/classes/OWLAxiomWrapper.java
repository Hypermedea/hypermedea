package onto.classes;


import onto.namespaceAPI.NamingStrategy;
import org.semanticweb.owlapi.model.*;
import tools.IRITools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Wrapper for OWL axioms to be exposed as a CArtAgO observable properties.
 * A CArtAgO property is composed of a name and an ordered list of arguments
 *
 * @author Victor Charpenay, Noe SAFFAF
 */
public class OWLAxiomWrapper {

    protected OWLAxiom axiom;
    protected Set<NamingStrategy> namingStrategySet;

    /*
     * TODO the constructors below can take a TBox or ABox axiom.
     * To create a wrapper for a plain RDF triple, use OWLDataPropertyAssertionAxiomImpl and OWLObjectPropertyAssertionAxiomImpl.
     * If the triple doesn't use any registered OWL term, then don't create a wrapper (agents can still access it via rdf/3).
     */

    public OWLAxiomWrapper(OWLAxiom axiom, Set<NamingStrategy> namingStrategySet) {
        this.axiom = axiom;
        this.namingStrategySet = namingStrategySet;
    }

    /*
     * TODO the two methods below should be used within LinkedDataFuSpider when calling defineObsProperty(name, args).
     * The artifact may keep a map OWLAxiomWrapper -> ObsProperty to quickly retrieve the observable property from an axiom
     */

    /**
     * Returns the property's shortened name based in naming strategies
     * @return Shortened Name of an OWL Axiom
     */
    public String getPropertyName() {
        String name = null;
        for (NamingStrategy namingStrategy : namingStrategySet){
            name = namingStrategy.getNameForIRI(IRI.create(getPropertyFullName()));
            if (name != null){
                return name;
            }
        }
        return null; // TODO retrieve the relevant IRI depending on the type of axiom and use namingStrategy.getNameForIRI()
    }

    /**
     * Returns the property's name of an axiom
     * @return Name of an OWL Axiom
     */
    public String getPropertyFullName(){
        String propertyFullName = null;
        if (axiom.isOfType(AxiomType.DECLARATION)) {
            OWLDeclarationAxiom owlDeclarationAxiom = (OWLDeclarationAxiom) axiom;
            propertyFullName = IRITools.removeWrapperIRI(owlDeclarationAxiom.getEntity().getEntityType().toString());
        }

        else if (axiom.isOfType(AxiomType.CLASS_ASSERTION)) {
            OWLClassAssertionAxiom owlClassAssertionAxiom = (OWLClassAssertionAxiom) axiom;
            propertyFullName = IRITools.removeWrapperIRI(owlClassAssertionAxiom.getClassExpression().toString());
        }

        else if (axiom.isOfType(AxiomType.ANNOTATION_ASSERTION)){
            OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom = (OWLAnnotationAssertionAxiom) axiom;
            propertyFullName = IRITools.removeWrapperIRI(owlAnnotationAssertionAxiom.getProperty().toString());
        }

        else if (axiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
            propertyFullName = IRITools.removeWrapperIRI(owlObjectPropertyAssertionAxiom.getProperty().toString());
        }

        else if (axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
            OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom = (OWLDataPropertyAssertionAxiom) axiom;
            propertyFullName = IRITools.removeWrapperIRI(owlDataPropertyAssertionAxiom.getProperty().toString());
        }

        else {
            propertyFullName = "unhandled_axiom";
        }

        return propertyFullName;
    }

    /**
     * Returns property's arguments
     * @return Property's arguments
     */
    public List<Object> getPropertyArguments() {
        List<Object> propertyArguments = new ArrayList<>();
        if (axiom.isOfType(AxiomType.DECLARATION)) {
            OWLDeclarationAxiom owlDeclarationAxiom = (OWLDeclarationAxiom) axiom;
            propertyArguments.add(IRITools.removeWrapperIRI(owlDeclarationAxiom.getEntity().toString()));
        }


        else if (axiom.isOfType(AxiomType.CLASS_ASSERTION)) {
            OWLClassAssertionAxiom owlClassAssertionAxiom = (OWLClassAssertionAxiom) axiom;
            propertyArguments.add(IRITools.removeWrapperIRI(owlClassAssertionAxiom.getIndividual().toString()));
        }

        else if (axiom.isOfType(AxiomType.ANNOTATION_ASSERTION)){
            OWLAnnotationAssertionAxiom owlAnnotationAssertionAxiom = (OWLAnnotationAssertionAxiom) axiom;
            propertyArguments.add(IRITools.removeWrapperIRI(owlAnnotationAssertionAxiom.getSubject().toString()));
            propertyArguments.add(IRITools.removeWrapperIRI(owlAnnotationAssertionAxiom.getValue().toString()));
        }
        else if (axiom.isOfType(AxiomType.OBJECT_PROPERTY_ASSERTION)) {
            OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = (OWLObjectPropertyAssertionAxiom) axiom;
            propertyArguments.add(IRITools.removeWrapperIRI(owlObjectPropertyAssertionAxiom.getSubject().toString()));
            propertyArguments.add(IRITools.removeWrapperIRI(owlObjectPropertyAssertionAxiom.getObject().toString()));
        }
        else if (axiom.isOfType(AxiomType.DATA_PROPERTY_ASSERTION)) {
            OWLDataPropertyAssertionAxiom owlDataPropertyAssertionAxiom = (OWLDataPropertyAssertionAxiom) axiom;
            propertyArguments.add(IRITools.removeWrapperIRI(owlDataPropertyAssertionAxiom.getSubject().toString()));
            propertyArguments.add(IRITools.removeWrapperIRI(owlDataPropertyAssertionAxiom.getObject().toString()));
        }

        return propertyArguments; // TODO implement depending on the type of axiom
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OWLAxiomWrapper)) return false;
        OWLAxiomWrapper that = (OWLAxiomWrapper) o;
        return axiom.equals(that.axiom);
    }

    @Override
    public int hashCode() {
        return Objects.hash(axiom);
    }
}
