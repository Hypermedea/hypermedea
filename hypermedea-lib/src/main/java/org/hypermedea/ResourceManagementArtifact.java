package org.hypermedea;

import cartago.OPERATION;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;

import java.util.List;

/**
 * <p>
 *     Artifact to manage resources with HTTP CR(U)D operations, as e.g. specified in the
 *     <a href="https://www.w3.org/TR/ldp/">Linked Data Platform (LDP)</a> and
 *     <a>SPARQL </a>.
 * </p>
 * <p>
 *     Resource representations are assumed to be lists of RDF triples (see {@link NavigationArtifact}
 *     and {@link OntologyArtifact} for representations of triples in Jason). The artifact is aware
 *     of LDP specificities such as pagination.
 * </p>
 */
public class ResourceManagementArtifact extends HypermedeaArtifact {

    public void init() {
        // do nothing
    }

    @OPERATION
    public void createResource(List<String> representation) {
        Model m = parseRepresentationOrFail(representation);


        // TODO
    }

    @OPERATION
    public void replaceResourceRepresentation(String resourceURI, List<String> representation) {
        // TODO
    }

    @OPERATION
    public void deleteResource(String resourceURI) {
        // TODO
    }

    private Model parseRepresentationOrFail(List<String> representation) {
        Model m = ModelFactory.createDefaultModel();

        for (String termString : representation) {
            // TODO parse term into an RDF triple
        }

        return m;
    }

}
