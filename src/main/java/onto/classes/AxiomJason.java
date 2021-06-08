package onto.classes;

import java.util.Objects;

/**
 * A class for axioms used de create in Jason unary/binary predicates
 */
public class AxiomJason {

    private String predicateName;
    private ComparableArrayList<String> predicateContent;
    private String predicateFullName;

    /**
     * Constructor
     * @param predicateName Name of the predicate
     * @param predicateContent Content of the predicate (normally size 1 or 2)
     * @param predicateFullName Full name (namespace + name) of the predicate
     */
    public AxiomJason(String predicateName, ComparableArrayList<String> predicateContent, String predicateFullName) {
        this.predicateName = predicateName;
        this.predicateContent = predicateContent;
        this.predicateFullName = predicateFullName;
    }


    //This one should be used
    public AxiomJason(ComparableArrayList<String> predicateContent, String predicateFullName) {
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
        if (predicateContent.size() == 1) {
            return "Axiom_Jason {" +
                    "predicateName= " + predicateName + "\n" +
                    "predicateFullName= " + predicateFullName + "\n" +
                    "predicateContent1= " + predicateContent.get(0) + "\n" +
                    '}';
        } else if (predicateContent.size() == 2){
            return "Axiom_Jason {" +
                    "predicateName= " + predicateName + "\n" +
                    "predicateFullName= " + predicateFullName + "\n" +
                    "predicateContent1= " + predicateContent.get(0) + "\n" +
                    "predicateContent2= " + predicateContent.get(1) + "\n" +
                    '}';
        } else {
            return "Axiom_Jason {" +
                    "predicateName= " + predicateName + "\n" +
                    "predicateFullName= " + predicateFullName + "\n" +
                    '}';
        }



    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AxiomJason)) return false;
        AxiomJason that = (AxiomJason) o;
        return predicateContent.equals(that.predicateContent) && Objects.equals(predicateFullName, that.predicateFullName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(predicateContent, predicateFullName);
    }
}
