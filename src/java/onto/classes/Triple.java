package onto.classes;

import java.util.Objects;

/**
 * A class for triples
 */
public class Triple {
    public String subject;
    public String predicate;
    public String object;


    public Triple(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triple)) return false;
        Triple triple = (Triple) o;
        return Objects.equals(subject, triple.subject) && Objects.equals(predicate, triple.predicate) && Objects.equals(object, triple.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }

    @Override
    public String toString() {
        return "Triple {subject= " + subject + ", predicate= " + predicate + ", object= " + object +"}";
    }
}
