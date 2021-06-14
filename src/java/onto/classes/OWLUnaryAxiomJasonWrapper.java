package onto.classes;

import java.util.Objects;

public class OWLUnaryAxiomJasonWrapper extends OWLAxiomJasonWrapper {
    private String predicateTerm;

    public OWLUnaryAxiomJasonWrapper(String predicateFullName, String predicateTerm) {
        super(predicateFullName);
        this.predicateTerm = predicateTerm;
    }

    public String getPredicateTerm() {
        return predicateTerm;
    }

    public void setPredicateTerm(String predicateTerm) {
        this.predicateTerm = predicateTerm;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OWLUnaryAxiomJasonWrapper)) return false;
        if (!super.equals(o)) return false;
        OWLUnaryAxiomJasonWrapper that = (OWLUnaryAxiomJasonWrapper) o;
        return Objects.equals(predicateTerm, that.predicateTerm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), predicateTerm);
    }

    @Override
    public String toString() {
        return "OWLUnaryAxiomJasonWrapper {" +
                "predicateName= " + predicateName + "\n" +
                "predicateFullName= " + predicateFullName + "\n" +
                "predicateContent1= " + predicateTerm + "\n" +
                '}';
    }
}
