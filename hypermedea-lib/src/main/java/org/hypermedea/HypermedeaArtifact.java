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
import org.hypermedea.op.ResponseCallback;
import org.hypermedea.tools.Identifiers;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * <p>
 *   A Hypermedea artifact submits agent requests to servers and processes any incoming response
 *   (synchronous or asynchronous). It also manages a collection of resource representations
 *   available to any agent observing the artifact.
 * </p>
 * <p>
 *   In most applications, a single Hypermedea artifact will be enough. A typical JCM project
 *   file will be as follows:
 * </p>
 * <pre><code>mas example_mas {

 agent example_web_agent {
   join: example_workspace
   focus: example_workspace.h
 }

 workspace example_workspace {
   artifact h: org.hypermedea.HypermedeaArtifact()
 }

}</code></pre>
 * <p>
 *   Agent <code>example_web_agent</code> focuses on artifact <code>h</code>, to be able
 *   to execute its operations and observe the resource representations it manages.
 *   Available operations are
 *   {@link #get(String, Object[]) get},
 *   {@link #watch(String, Object[]) watch},
 *   {@link #forget(String, Object[]) forget},
 *   {@link #put(String, String, Object[]) put},
 *   {@link #post(String, String, Object[]) post},
 *   {@link #patch(String, String, Object[]) patch} and
 *   {@link #delete(String, Object[]) delete}.
 * </p>
 * <p>
 *   All operations have in common that they
 *   accept a <em>form</em> as input, i.e. a set of arbitrary property/value pairs (called
 *   form fields). A form may be used to provide further arguments to the operation,
 *   such as custom headers, or to provide protocol-specific information, such as
 *   an MQTT Quality of Service (QoS) level or a ROS message type. All operations
 *   also have an alternative signature without form, which is equivalent to executing
 *   the operation with an empty form.
 * </p>
*/
public class HypermedeaArtifact extends Artifact {

    private class Subscription implements ResponseCallback {

        public static final Map<String, Subscription> subscriptions = new HashMap<>();

        private final String subscriptionURI;

        private Subscription(String uri) {
            subscriptionURI = uri;
            subscriptions.put(uri, this);

            // TODO if subscription exists for URI, unregister first
        }

        @Override
        public void onResponse(Response res) {
            log(res.toString());

            if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                // TODO do something?
            } else {
                beginExtSession();
                updateRepresentation(subscriptionURI, res.getPayload());
                endExtSession();
            }
        }

        @Override
        public void onError() {
            // TODO annotate latest resource representation as outdated?
            log("Connection with server lost during WATCH operation on: " + subscriptionURI);
        }

        public static Subscription get(String resourceURI) {
            return subscriptions.get(resourceURI);
        }

        public static void remove(String resourceURI) {
            subscriptions.remove(resourceURI);
        }

    }

    public static final String SOURCE_FUNCTOR = "source";

    private final Map<String, Collection<ObsProperty>> representations = new HashMap<>();

    private final Map<String, Operation> activeOperations = new HashMap<>();

    private final Object[] emptyForm = {};

    public void init() {
        // nothing to do
    }

    @Override
    protected void dispose() {
        super.dispose();
    }

    /**
     * Executes {@link #get(String, Object[])} with an empty form.
     */
    @OPERATION
    public void get(String resourceURI) {
        get(resourceURI, emptyForm);
    }

    /**
     * <p>
     *   Asks the server for a representation of {@code resourceURI}. Once the artifact receives
     *   a response from the server, it exposes the response payload as an observable property
     *   (a Jason literal) before returning. The calling agent can thus safely query the
     *   resource's representation after the call returns, as follows:
     * </p>
     * <pre><code>+!retrieve_then_query(URI) &lt;-
    h.target(URI, TargetURI) ;
    get(URI) ;
    for (rdf(URI, P, O)[source(TargetURI)) {
      .print("Found: ", P, O)
    } ;
  .</code></pre>
     * <p>
     *   Any piece of representation found in the server's response will be turned into a
     *   literal with a {@code source} annotation. In some cases, the URI provided by the
     *   agent may not be the exact target used by the artifact to request the server
     *   (URI fragments are e.g. stripped away). This is why Hypermedea also provides
     *   the {@link h.target} internal action to extract the corresponding target from any
     *   URI.
     * </p>
     * <p>
     *   Note that a resource representation may correspond to one or more literals,
     *   depending on the response payload's Content-Type. In RDF, it is indeed more
     *   convenient for programmers to have access to RDF triples individually rather than
     *   as members of a list, whereas in JSON, there is a single literal with a
     *   tree-shaped structure. See {@link org.hypermedea.ct ct} for details on
     *   representation handlers.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void get(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.GET);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        initiateOperation(op, Optional.empty());
    }

    /**
     * Executes {@link #watch(String, Object[])} with an empty form.
     */
    @OPERATION
    public void watch(String resourceURI) {
        watch(resourceURI, emptyForm);
    }

    /**
     * <p>
     *   Subscribes to any change in the representation of {@code resourceURI}, to be sent asynchronously
     *   by the server. Not all protocols may support this operation type. Plain HTTP, for instance, doesn't.
     * </p>
     * <p>
     *   In contrast to a {@link #get(String, Object[]) get} operation, the call may return before having
     *   received any representation from the server. The call returns as soon as the server confirms the
     *   request has been processed (or, if the underlying protocol has no acknowledgement mechanism, it
     *   may return immediately). The caller agent should therefore wait for events corresponding to
     *   server notifications, as follows:
     * </p>
     * <pre><code>+!watch_only(URI) &lt;-
    h.target(URI, TargetURI) ;
    watch(URI) ;
    +watching(TargetURI) ;
  .

+rdf(S, P, O)[source(TargetURI)] : watching(TargetURI) &lt;-
     .print("Received: ", S, P, O) ;
  .</code></pre>
     * <p>
     *   or, if a single notification is enough:
     * </p>
     * <pre><code>+!watch_then_wait(URI) &lt;-
    h.target(URI, TargetURI) ;
    watch(URI) ;
    .wait({ +(json(Val)[source(TargetURI) }) ;
    .print("Received: ", Val) ;
  .</code></pre>
     * <p>
     *   In the above example, the agent must know in advance what Content-Type to expect. Certain
     *   protocol bindings have limitations as to what Content-Type can be exchanged with the server.
     *   In case the server supports it, though, the agent may leverage
     *   <a href="https://en.wikipedia.org/wiki/Content_negotiation">content negotiation</a>
     *   to request the server to return a specific Content-Type.
     * </p>
     * <p>
     *   {@link h.target} is a Hypermedea internal action, {@link jason.stdlib.wait wait} is part of the
     *   <a href="https://jason-lang.github.io/api/jason/stdlib/package-summary.html">Jason standard library</a>.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void watch(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.WATCH);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        // registration must occur before the operation starts
        op.registerResponseCallback(new Subscription(resourceURI));
        activeOperations.put(resourceURI, op);

        initiateOperation(op, Optional.empty());
    }

    /**
     * Executes {@link #forget(String, Object[])} with an empty form.
     */
    @OPERATION
    public void forget(String resourceURI) {
        forget(resourceURI, emptyForm);
    }

    /**
     * Deletes the local representation of {@code resourceURI} and, if the resource is being watched,
     * unsubscribes from server notification. No future change on {@code resourceURI} will be notified
     * to agents.
     *
     * @param resourceURI the URI of a resource
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void forget(String resourceURI, Object[] formFields) {
        updateRepresentation(resourceURI, new HashSet<>());

        if (activeOperations.containsKey(resourceURI)) {
            Operation op = activeOperations.remove(resourceURI);
            // if no further callback, this call should end the operation
            op.unregisterResponseCallback(Subscription.get(resourceURI));
        }
    }

    /**
     * Executes {@link #put(String, String, Object[])} with an empty form.
     */
    @OPERATION
    public void put(String resourceURI, String representation) {
        put(resourceURI, representation, emptyForm);
    }

    /**
     * <p>
     *   Asks the server to replace the current representation(s) it has of {@code resourceURI} with
     *   the provided {@code representation}. This parameter must be provided as a Jason literal, in
     *   order to be serialized in a standard format by the proper representation handler (see
     *   package {@link org.hypermedea.ct ct}).
     * </p>
     * <p>
     *   After the call returns, the caller agent may assume the new representation of {@code resourceURI}
     *   on server side is the provided one. There is however no strict guarantee. The server may also
     *   alter this representation, e.g. to maintain consistency with other resources it manages or to add
     *   metadata (modification date, author, etc.). This is why the Hypermedea artifact makes no assumption
     *   about what the new representation is. If the caller agent wants to cache the new representation, it
     *   should execute a {@link #get(String, Object[]) get} operation right after the {@link #put(String,
     *   String, Object[]) put}, as follows:
     * </p>
     * <pre><code>+!put_then_get(URI) &lt;-
    h.target(URI, TargetURI) ;
    put(URI, json(5)) ;
    get(URI) ;
    // the artifact may then expose json(5)[source(TargetURI)]
    // or, for instance, json([kv("value", 5), kv("modified", 1700304346)])[source(TargetURI)]
  .</code></pre>
     * <p>
     *   By default, the Hypermedea artifact deletes the outdated representation of {@code resourceURI}
     *   when the call returns.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param representation a resource representation to send to the server, in the form of a Jason literal
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void put(String resourceURI, String representation, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.PUT);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        initiateOperation(op, Optional.of(representation));
    }

    /**
     * Executes {@link #post(String, String, Object[])} with an empty form.
     */
    @OPERATION
    public void post(String resourceURI, String representationPart) {
        post(resourceURI, representationPart, emptyForm);
    }

    /**
     * <p>
     *   Asks the server to append the provided {@code representationPart} to the current representation
     *   of {@code resourceURI}. As for {@link #put(String, String, Object[]) put}, it is not guaranteed
     *   that the server does exactly what the agent asked. It may remove other parts of the representation
     *   to maintain consistency or add more information to it.
     * </p>
     * <p>
     *   Notably, a server may also create a new resource as a side effect of the operation. This new
     *   resource should either be linked from the (new) representation of {@code resourceURI}, if its
     *   Content-Type supports hypermedia, or be exposed in a message header, hidden by the protocol binding.
     *   For instance, in response to a POST request, an HTTP server may return a {@code 201 Created}
     *   response that includes a {@code Location} header pointing to the new resource. To expose this
     *   information to the caller agent, the Hypermedea artifact builds an RDF triple from the location
     *   header and adds it to the representation of {@code resourceURI}. The agent may then query that
     *   RDF triple, as follows:
     * </p>
     * <pre><code>+!post_then_follow_link(URI) &lt;-
    h.target(URI, TargetURI) ;
    post(URI, json(5)) ;
    ?(rdf(TargetURI, "related", CreatedResourceURI)) ;
    .print("Created resource: ", CreatedResourceURI) ;
    h.target(CreatedResourceURI, CreatedTargetURI) ;
    get(CreatedResourceURI) ;
    ?(json(Val)[source(CreatedTargetURI)]) ;
    .print(Val) ; // should include "5"
  .</code></pre>
     * <p>
     *   In the above example, the {@code related} predicate is set by default. However, in some cases,
     *   protocol bindings may choose a more precise predicate, if the context permits it (for instance
     *   <a href="http://purl.org/dc/terms/hasPart"><code>dct:hasPart</code></a>).
     * </p>
     * <p>
     *   Note that, as for {@link #put(String, String, Object[]) put}, if the Hypermedea artifact had
     *   a representation of {@code resourceURI} before the operation, this representation is deleted
     *   when the call returns.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param representationPart part of a resource representation to send to the server, in the form of a Jason literal
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void post(String resourceURI, String representationPart, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.POST);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        initiateOperation(op, Optional.of(representationPart));
    }

    /**
     * Executes {@link #patch(String, String, Object[])} with an empty form.
     */
    @OPERATION
    public void patch(String resourceURI, String representationDiff) {
        patch(resourceURI, representationDiff, emptyForm);
    }

    /**
     * <p>
     *   Asks the server to apply a diff on the current representation of {@code resourceURI}, as specified
     *   in {@code representationDiff}. A diff should specify what parts to remove and what parts to add
     *   to the representation. Examples of diff formats include SPARQL Update and Git diff (for text files).
     * </p>
     * <p>
     *   <em>Not fully implemented yet</em>.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param representationDiff a diff to apply to the resource's representation, in the form a Jason literal
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void patch(String resourceURI, String representationDiff, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.PATCH);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        initiateOperation(op, Optional.of(representationDiff));
    }

    /**
     * Executes {@link #delete(String)} with an empty form.
     */
    @OPERATION
    public void delete(String resourceURI) {
        delete(resourceURI, emptyForm);
    }

    /**
     * <p>
     *   Asks the server to delete all known representations of {@code resourceURI}. The Hypermedea
     *   artifact does delete all representations locally, as there is no ambiguity as to what should
     *   happen on the server. However, other resources may have altered representations after the
     *   operation, which the artifact is unaware of. For instance, the server might delete all
     *   references of the resource in the representation of other resources it manages.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    @OPERATION
    public void delete(String resourceURI, Object[] formFields) {
        Map<String, Object> f = parseFormFields(formFields);
        f.put(Operation.METHOD_NAME_FIELD, Operation.DELETE);

        Operation op = ProtocolBindings.bind(resourceURI, f);
        initiateOperation(op, Optional.empty());
    }

    /**
     * Sends the request that will start an operation and waits for an initial response from the server.
     * If the operation is a WATCH operation, the operation remains active after this method returns
     * (until {@link #forget(String)} is called on the target resource). Otherwise, the operation must
     * have ended before the method returns.
     *
     * @param op an operation bound to a protocol binding
     * @param requestPayloadOpt an optional paylaod to add to the request
     * @return the input operation, for further
     */
    private Operation initiateOperation(Operation op, Optional<String> requestPayloadOpt) {
        try {
            if (requestPayloadOpt.isPresent()) {
                String requestPayload = requestPayloadOpt.get();
                setPayload(op, requestPayload);
            }

            op.sendRequest();
            log(op.toString());

            if (!op.isAsync()) {
                Response res = op.getResponse();
                log(res.toString());

                if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                    // TODO add request/response in error tuples
                    failed("The server returned an error: " + res.getStatus());
                } else {
                    updateRepresentation(op.getTargetURI(), res.getPayload());
                    commit();
                }
            }
        } catch (IOException e) {
            // TODO add request/response in error tuples
            failed("I/O error occurred: " + e.getMessage());
        }

        return op;
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

    private void updateRepresentation(String resourceURI, Collection<Literal> newRepresentation) {
        Set<ObsProperty> props = new HashSet<>();

        for (Literal t : newRepresentation) {
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

        if (props.isEmpty()) representations.remove(resourceURI);
        else representations.put(resourceURI, props);
    }

}
