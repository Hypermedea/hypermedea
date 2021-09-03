package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;

import java.util.Objects;

public class Resource {

    private final String uri;

    private final Model representation;

    public Resource(String uri, Model representation) {
        this.uri = uri;
        this.representation = representation;
    }

    public String getURI() {
        return uri;
    }

    public Model getRepresentation() {
        return representation;
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
