package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.op.Operation;
import org.hypermedea.op.ProtocolBindings;
import org.hypermedea.op.Response;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Hypermedea artifact, exposing abstract methods to perform operations on the Web.
 * All Hypermedea artifacts share a reference to a singleton Web client.
 * In most application, there probably is a single Hypermedea artifact as well.
 */
public abstract class HypermedeaArtifact extends Artifact {

    public static final String SOURCE_FUNCTOR = "crawler_source";

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
        if (crawlerListener != null) crawler.addListener(crawlerListener);
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

    /**
     * A+REST extension (for asynchronous communication).
     *
     * See <a href="https://doi.org/10.1109/ICSE.2004.1317465">
     *     Extending the Representational State Transfer (REST)
     *     Architectural Style for Decentralized Systems
     * </a>.
     *
     * @param resourceURI
     */
    @OPERATION
    public void watch(String resourceURI) {
        watch(resourceURI, new HashMap<>());
    }

    @OPERATION
    public void watch(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.WATCH);
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
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        try {
            if (requestPayloadOpt.isPresent()) {
                Object requestPayload = requestPayloadOpt.get();
                setPayload(op, requestPayload);
            }

            op.sendRequest();
            Response res = op.getResponse();

            if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                // TODO add request/response in error tuples
                failed("The server returned an error: " + res.getStatus());
            } else {
                for (Structure t : res.getPayload()) addPredicate(t, resourceURI);
                // TODO delete previous representation of the resource?
            }
        } catch (IOException e) {
            // TODO add request/response in error tuples
            failed(e.getMessage());
        }
    }

    private void setPayload(Operation op, Object requestPayload) {
        try {
            Structure t = ASSyntax.parseStructure(requestPayload.toString());
            op.setPayload(t);
        } catch (ParseException e) {
            try {
                List<Term> l = ASSyntax.parseList(requestPayload.toString()).getAsList();

                Optional<Term> nonStructureOpt = l.stream().filter(t -> !t.isStructure()).findAny();
                if (nonStructureOpt.isPresent()) {
                    String msg = "The provided request payload include a non-predicate term: " + nonStructureOpt.get();
                    throw new IllegalArgumentException();
                }

                List<Structure> ls = l.stream().map(t -> (Structure) t).collect(Collectors.toList());

                op.setPayload(ls);
            } catch (ParseException e2) {
                String msg = "The provided request payload isn't a proper (list of) Jason predicate(s): " + requestPayload;
                throw new IllegalArgumentException(msg, e2);
            }
        }
    }

    private void addPredicate(Structure t, String src) {
        ObsProperty p = defineObsProperty(t.getFunctor(), t.getTerms().toArray());
        p.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, ASSyntax.createString(src)));
    }

}
