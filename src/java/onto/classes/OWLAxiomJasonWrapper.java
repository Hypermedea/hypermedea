package onto.classes;

import java.util.Objects;

/**
 * A class for axioms used de create in Jason unary/binary predicates
 */
public abstract class OWLAxiomJasonWrapper {

    protected String predicateName;
    protected String predicateFullName;

    /**
     * Constructor
     * @param predicateFullName Full name (namespace + name) of the predicate
     */


    public OWLAxiomJasonWrapper(String predicateFullName) {
        this.predicateFullName = predicateFullName;
    }

    public String getPredicateName() {
        return predicateName;
    }

    public void setPredicateName(String predicateName) {
        this.predicateName = predicateName;
    }

    public String getPredicateFullName() {
        return predicateFullName;
    }

    public void setPredicateFullName(String predicateFullName) {
        this.predicateFullName = predicateFullName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OWLAxiomJasonWrapper)) return false;
        OWLAxiomJasonWrapper that = (OWLAxiomJasonWrapper) o;
        return Objects.equals(predicateName, that.predicateName) && predicateFullName.equals(that.predicateFullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicateName, predicateFullName);
    }

    @Override
    public String toString() {
        return "OWLAxiomJasonWrapper{" +
                "predicateName= " + predicateName + "\n" +
                "predicateFullName= " + predicateFullName + "\n" +
                '}';
    }
}
