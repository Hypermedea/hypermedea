package classes;

import java.util.Objects;

/**
 * A class for triples
 */
public class Triplet {
    public String subject;
    public String predicate;
    public String object;


    public Triplet(String subject, String predicate, String object) {
        this.subject = subject;
        this.predicate = predicate;
        this.object = object;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Triplet)) return false;
        Triplet triplet = (Triplet) o;
        return Objects.equals(subject, triplet.subject) && Objects.equals(predicate, triplet.predicate) && Objects.equals(object, triplet.object);
    }

    @Override
    public int hashCode() {
        return Objects.hash(subject, predicate, object);
    }
}
