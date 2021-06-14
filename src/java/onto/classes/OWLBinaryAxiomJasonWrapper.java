package onto.classes;

import java.util.Objects;

public class OWLBinaryAxiomJasonWrapper extends OWLAxiomJasonWrapper {
    private String predicateTermSubject;
    private String predicateTermObject;

    public OWLBinaryAxiomJasonWrapper(String predicateFullName, String predicateTermSubject, String predicateObject) {
        super(predicateFullName);
        this.predicateTermSubject = predicateTermSubject;
        this.predicateTermObject = predicateObject;
    }

    public String getPredicateTermSubject() {
        return predicateTermSubject;
    }

    public void setPredicateTermSubject(String predicateTermSubject) {
        this.predicateTermSubject = predicateTermSubject;
    }

    public String getPredicateTermObject() {
        return predicateTermObject;
    }

    public void setPredicateTermObject(String predicateTermObject) {
        this.predicateTermObject = predicateTermObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OWLBinaryAxiomJasonWrapper)) return false;
        if (!super.equals(o)) return false;
        OWLBinaryAxiomJasonWrapper that = (OWLBinaryAxiomJasonWrapper) o;
        return predicateTermSubject.equals(that.predicateTermSubject) && predicateTermObject.equals(that.predicateTermObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), predicateTermSubject, predicateTermObject);
    }

    @Override
    public String toString() {
        return "OWLBinaryAxiomJasonWrapper {" +
                "predicateName= " + predicateName + "\n" +
                "predicateFullName= " + predicateFullName + "\n" +
                "predicateSubject= " + predicateTermSubject + "\n" +
                "predicateObject= " + predicateTermObject + "\n" +
                '}';
    }
}
