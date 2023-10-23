package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import jason.asSyntax.Structure;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.op.Operation;
import org.hypermedea.op.ProtocolBindings;
import org.hypermedea.op.Response;
import org.hypermedea.tools.Identifiers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hypermedea artifact, exposing abstract methods to perform operations on the Web.
 * All Hypermedea artifacts share a reference to a singleton Web client.
 * In most application, there probably is a single Hypermedea artifact as well.
 */
public class HypermedeaArtifact extends Artifact {

    public static final String SOURCE_FUNCTOR = "source";

    private final Map<String, Collection<ObsProperty>> representations = new HashMap<>();

    private final Object[] emptyForm = {};

    public void init() {
        // nothing to do
    }

    @Override
    protected void dispose() {
        super.dispose();
    }

    @OPERATION
    public void get(String resourceURI) {
        get(resourceURI, emptyForm);
    }

    @OPERATION
    public void get(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.GET);
        executeOperation(resourceURI, f, Optional.empty());
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
        watch(resourceURI, emptyForm);
    }

    @OPERATION
    public void watch(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.WATCH);
        executeOperation(resourceURI, f, Optional.empty());
    }

    @OPERATION
    public void put(String resourceURI, String representation) {
        put(resourceURI, representation, emptyForm);
    }

    @OPERATION
    public void put(String resourceURI, String representation, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.PUT);
        executeOperation(resourceURI, f, Optional.of(representation));
    }

    @OPERATION
    public void post(String resourceURI, String representationPart) {
        post(resourceURI, representationPart, emptyForm);
    }

    @OPERATION
    public void post(String resourceURI, String representationPart, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.POST);
        executeOperation(resourceURI, f, Optional.of(representationPart));
    }

    @OPERATION
    public void patch(String resourceURI, String representationDiff) {
        patch(resourceURI, representationDiff, emptyForm);
    }

    @OPERATION
    public void patch(String resourceURI, String representationDiff, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.PATCH);
        executeOperation(resourceURI, f, Optional.of(representationDiff));
    }

    @OPERATION
    public void delete(String resourceURI) {
        delete(resourceURI, emptyForm);
    }

    @OPERATION
    public void delete(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.DELETE);
        executeOperation(resourceURI, f, Optional.empty());
    }

    private void executeOperation(String resourceURI, Map<String, Object> formFields, Optional<String> requestPayloadOpt) {
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        try {
            if (requestPayloadOpt.isPresent()) {
                String requestPayload = requestPayloadOpt.get();
                setPayload(op, requestPayload);
            }

            op.sendRequest();
            log(op.toString());

            Response res = op.getResponse();
            log(res.toString());

            if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                // TODO add request/response in error tuples
                failed("The server returned an error: " + res.getStatus());
            } else {
                Set<ObsProperty> props = new HashSet<>();

                for (Literal t : res.getPayload()) {
                    ObsProperty p = defineObsProperty(t.getFunctor(), t.getTerms().toArray());

                    if (t.hasAnnot())
                        for (Term a : t.getAnnots().getAsList()) p.addAnnot(a);
                    
                    p.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, ASSyntax.createString(resourceURI)));

                    props.add(p);
                }

                if (representations.containsKey(resourceURI)) {
                    for (ObsProperty p : representations.get(resourceURI)) {
                        removeObsPropertyByTemplate(p.getName(), p.getValues());
                    }
                }

                commit();
                representations.put(resourceURI, props);
            }
        } catch (IOException e) {
            // TODO clean representations cache if error occurs in the above block
            // TODO add request/response in error tuples
            failed("I/O error occurred: " + e.getMessage());
        }
    }

    private void setPayload(Operation op, String requestPayload) {
        try {
            Literal t = ASSyntax.parseLiteral(requestPayload);
            op.setPayload(t);
        } catch (ParseException e) {
            try {
                // FIXME will never work: payload given as String, not String[]
                List<Term> l = ASSyntax.parseList(requestPayload).getAsList();

                Optional<Term> nonStructureOpt = l.stream().filter(t -> !t.isStructure()).findAny();
                if (nonStructureOpt.isPresent()) {
                    String msg = "The provided request payload include a non-predicate term: " + nonStructureOpt.get();
                    throw new IllegalArgumentException();
                }

                List<Literal> ls = l.stream().map(t -> (Literal) t).collect(Collectors.toList());

                op.setPayload(ls);
            } catch (ParseException e2) {
                String msg = "The provided request payload isn't a proper (list of) Jason predicate(s): " + requestPayload;
                throw new IllegalArgumentException(msg, e2);
            }
        }
    }

    private Map<String, Object> parseFormFields(Object[] l) {
        Map<String, Object> f = new HashMap<>();

        for (Object kv : l) {
            try {
                Structure t = ASSyntax.parseStructure(kv.toString());

                if (t.getArity() == 2) {
                    String k = Identifiers.getLexicalForm(t.getTerm(0));
                    String v = Identifiers.getLexicalForm(t.getTerm(1));

                    f.put(k, v);
                }
            } catch (ParseException e) {
                // TODO log (same if structure isn't valid)
            }
        }

        return f;
    }

}
