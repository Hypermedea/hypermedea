package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;

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

}
