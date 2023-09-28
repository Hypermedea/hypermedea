package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import jason.asSyntax.Term;
import org.hypermedea.ct.RepresentationWrapper;
import org.hypermedea.ct.RepresentationWrappers;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.op.Operation;
import org.hypermedea.op.ProtocolBinding;
import org.hypermedea.op.ProtocolBindings;
import org.hypermedea.op.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Hypermedea artifact, exposing abstract methods to perform operations on the Web.
 * All Hypermedea artifacts share a reference to a singleton Web client.
 * In most application, there probably is a single Hypermedea artifact as well.
 */
public abstract class HypermedeaArtifact extends Artifact {

    /**
     * Singleton Linked Data crawler to which all artifacts can attach a listener.
     */
    protected static LinkedDataCrawler crawler = new LinkedDataCrawler();

    /**
     * Request listener that is automatically attached to the <code>HypermedeaArtifact</code> crawler instance
     * (and detached once disposed of).
     *
     * TODO add CArtAgO session management?
     */
    protected RequestListener crawlerListener = null;

    protected void init() {
        crawler.addListener(crawlerListener);
    }

    @Override
    protected void dispose() {
        if (crawlerListener != null) crawler.removeListener(crawlerListener);
        super.dispose();
    }

    @OPERATION
    public void get(String resourceURI) {
        get(resourceURI, new HashMap<>());
    }

    @OPERATION
    public void get(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.GET);
        executeOperation(resourceURI, formFields, Optional.empty());
    }

    @OPERATION
    public void put(String resourceURI, Object representation) {
        put(resourceURI, representation, new HashMap<>());
    }

    @OPERATION
    public void put(String resourceURI, Object representation, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PUT);
        executeOperation(resourceURI, formFields, Optional.of(representation));
    }

    @OPERATION
    public void post(String resourceURI, Object representationPart) {
        post(resourceURI, representationPart, new HashMap<>());
    }

    @OPERATION
    public void post(String resourceURI, Object representationPart, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.POST);
        executeOperation(resourceURI, formFields, Optional.of(representationPart));
    }

    @OPERATION
    public void patch(String resourceURI, Object representationDiff) {
        patch(resourceURI, representationDiff, new HashMap<>());
    }

    @OPERATION
    public void patch(String resourceURI, Object representationDiff, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PATCH);
        executeOperation(resourceURI, formFields, Optional.of(representationDiff));
    }

    @OPERATION
    public void delete(String resourceURI) {
        delete(resourceURI, new HashMap<>());
    }

    @OPERATION
    public void delete(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.DELETE);
        executeOperation(resourceURI, formFields, Optional.empty());
    }

    private void executeOperation(String resourceURI, Map<String, Object> formFields, Optional<Object> requestPayloadOpt) {
        ProtocolBinding b = ProtocolBindings.getBinding(resourceURI);
        Operation op = b.bind(resourceURI, formFields);
        try {
            if (requestPayloadOpt.isPresent()) op.setPayload(requestPayloadOpt.get());

            op.sendRequest();
            Response res = op.getResponse();

            if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                // TODO add request/response in error tuples
                failed("The server returned an error: " + res.getStatus());
            } else {
                Optional<Object> responsePayloadOpt = res.getPayload();

                if (responsePayloadOpt.isPresent()) {
                    Object responsePayload = responsePayloadOpt.get();
                    // TODO instead, let the binding call RepresentationWrappers (content-type is protocol-specific)
                    RepresentationWrapper w = RepresentationWrappers.wrap(responsePayload, "");
                    Term t = w.getTerm();
                    // TODO expose t as obs property
                    // TODO resource(URI, Rep) or resource(URI) ; hasRepresentation(URI, Rep)
                    // TODO or all in RDF: rdf(URI, a, rdfs:Resource) ; rdf(URI, rdf:value, ""^^Content-Type)

                    // TODO RDF store: each resource is a named graph, rdf(S,P,O)[resource(R)]
                    // TODO the entire graph is exposed to agents
                    // TODO use https://www.w3.org/TR/Content-in-RDF10/ and dct:isFormat?
                    // TODO or dct:hasFormat [ rdf:value ; dct:format "Content-Type" ] ?
                }
            }
        } catch (IOException e) {
            // TODO add request/response in error tuples
            failed(e.getMessage());
        }
    }

}
