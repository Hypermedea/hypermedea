package classes;

import java.util.Objects;

public class OntologyIRIHolder {
    public static final String distant = "Distant";
    public static final String local = "Local";

    private String origin;
    private String IRI;
    private String key;

    public OntologyIRIHolder(String origin, String IRI, String key) {
        this.origin = origin;
        this.IRI = IRI;
        this.key = key;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public String getIRI() {
        return IRI;
    }

    public void setIRI(String IRI) {
        this.IRI = IRI;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OntologyIRIHolder)) return false;
        OntologyIRIHolder that = (OntologyIRIHolder) o;
        return Objects.equals(origin, that.origin) && Objects.equals(IRI, that.IRI) && Objects.equals(key, that.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(origin, IRI, key);
    }
}
