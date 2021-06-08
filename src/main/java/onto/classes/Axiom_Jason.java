package onto.classes;

import java.util.Objects;

public class Axiom_Jason {

    private String predicateName;
    private ComparableArrayList<String> predicateContent;
    private String predicateFullName;

    public Axiom_Jason(String predicateName, ComparableArrayList<String> predicateContent, String predicateFullName) {
        this.predicateName = predicateName;
        this.predicateContent = predicateContent;
        this.predicateFullName = predicateFullName;
    }


    //This one should be used
    public Axiom_Jason(ComparableArrayList<String> predicateContent, String predicateFullName) {
        this.predicateContent = predicateContent;
        this.predicateFullName = predicateFullName;
    }

    public String getPredicateName() {
        return predicateName;
    }

    public void setPredicateName(String predicateName) {
        this.predicateName = predicateName;
    }

    public ComparableArrayList<String> getPredicateContent() {
        return predicateContent;
    }

    public void setPredicateContent(ComparableArrayList<String> predicateContent) { this.predicateContent = predicateContent; }

    public String getPredicateFullName() {
        return predicateFullName;
    }

    public void setPredicateFullName(String predicateFullName) {
        this.predicateFullName = predicateFullName;
    }

    @Override
    public String toString() {
        return "Axiom_Jason{" +
                "predicateName='" + predicateName + '\'' +
                ", predicateFullName='" + predicateFullName + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Axiom_Jason)) return false;
        Axiom_Jason that = (Axiom_Jason) o;
        return predicateContent.equals(that.predicateContent) && Objects.equals(predicateFullName, that.predicateFullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicateContent, predicateFullName);
    }
}
