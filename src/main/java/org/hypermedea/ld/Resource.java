package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;

import java.util.Objects;

public class Resource {

    private final String uri;

    private final Model representation;

    private final Boolean isCached;

    public Resource(String uri, Model representation) {
        this(uri, representation, false);
    }

    public Resource(String uri, Model representation, Boolean isCached) {
        this.uri = uri;
        this.representation = representation;
        this.isCached = isCached;
    }

    public String getURI() {
        return uri;
    }

    public Model getRepresentation() {
        return representation;
    }

    public Boolean isCached() {
        return isCached;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Resource resource = (Resource) o;
        return uri.equals(resource.uri);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri);
    }

}
