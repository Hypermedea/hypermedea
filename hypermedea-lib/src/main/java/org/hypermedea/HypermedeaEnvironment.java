package org.hypermedea;

import jason.asSyntax.*;
import jason.environment.Environment;
import org.hypermedea.ct.RepresentationHandler;
import org.hypermedea.op.Operation;
import org.hypermedea.op.ProtocolBindings;
import org.hypermedea.op.Response;
import org.hypermedea.op.ResponseCallback;
import org.hypermedea.tools.Identifiers;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * <p>
 *   The Hypermedea environment turns agent actions into requests to servers and processes any
 *   incoming response (synchronous or asynchronous). It also manages a collection of resource
 *   representations available to all agents as percepts.
 * </p>
 * <p>
 *   A typical MAS project file (.mas2j) will be as follows:
 * </p>
 * <pre><code>MA example_mas {

   environment: org.hypermedea.HypermedeaEnvironment

   agents: example_web_agent;

   aslSourcePath: "src";

 }</code></pre>
 * <p>
 *   Operations available to <code>example_web_agent</code> are
 *   {@link #get(String, Map) get},
 *   {@link #watch(String, Map) watch},
 *   {@link #forget(String, Map) forget},
 *   {@link #put(String, Collection, Map) put},
 *   {@link #post(String, Collection, Map) post},
 *   {@link #patch(String, Collection, Map) patch} and
 *   {@link #delete(String, Map) delete}.
 * </p>
 * <p>
 *   Safe operations (<code>get</code>, <code>watch</code>, <code>forget</code>)
 *   and <code>delete</code> only require a URI as input. Other operations must also
 *   have a resource representation as input, represented in AgentSpeak as a literal
 *   or a list of literals (see {@link RepresentationHandler} for more details).
 * </p>
 * <p>
 *   All operations have in common that they also accept a <em>form</em> as input, i.e.
 *   a set of arbitrary property/value pairs (called form fields). In AgentSpeak,
 *   a form is represented as a {@link MapTerm}. It may be used to provide further
 *   arguments to the operation, such as custom headers, or to provide
 *   protocol-specific information, such as an MQTT Quality of Service (QoS) level
 *   or a ROS message type. All operations also have an alternative signature
 *   without form, which is equivalent to executing the operation with an empty form.
 * </p>
 */
public class HypermedeaEnvironment extends Environment {

    private class Subscription implements ResponseCallback {

        private static final Map<String, Subscription> activeSubscriptions = new HashMap<>();

        private final Logger logger = HypermedeaEnvironment.this.getLogger();

        private final Operation operation;

        public Subscription(Operation op) {
            this.operation = op;
            activeSubscriptions.put(op.getTargetURI(), this);
        }

        @Override
        public void onResponse(Response response) {
            logger.info(response.toString());

            if (response.getStatus().equals(Response.ResponseStatus.OK)) {
                // TODO removal and addition should be done in a single transaction
                updateRepresentation(operation.getTargetURI(), response.getPayload());
            }

            informAgsEnvironmentChanged();
        }

        @Override
        public void onError() {
            logger.warning("Connection with server lost during WATCH operation on: " + operation.getTargetURI());
        }

        public static void deactivate(String resourceURI) {
            Subscription sub = activeSubscriptions.remove(resourceURI);
            if (sub != null) sub.operation.unregisterResponseCallback(sub);
        }

        public static void deactivateAll() {
            for (Subscription sub : activeSubscriptions.values()) sub.operation.unregisterResponseCallback(sub);
            activeSubscriptions.clear();
        }

    }

    /**
     * note: <code>source</code> is a reserved annotation in Jason; using <code>source_uri</code> instead.
     */
    public static final String SOURCE_FUNCTOR = "source_uri";

    @Override
    public boolean executeAction(String agName, Structure act) {
        String actionName = act.getFunctor();
        List<Term> args = act.getTerms();

        checkSignature(actionName, args);

        Term firstArg = act.getTerm(0);
        String resourceURI = Identifiers.getLexicalForm(firstArg);

        Term lastArg = act.getTerm(act.getArity() - 1);
        Map<String, Object> form = new HashMap<>();

        if (lastArg.isMap()) { // assumed to be a form
            MapTerm formArg = (MapTerm) lastArg;
            for (Term k : formArg.keys()) {
                Term v = formArg.get(k);
                form.put(Identifiers.getLexicalForm(k), Identifiers.getLexicalForm(v));
            }
        }

        Optional<Term> payloadOpt = args.stream().filter(Term::isStructure).findFirst();
        Collection<Literal> payload = new HashSet<>();

        if (payloadOpt.isPresent()) {
            Term t = payloadOpt.get();
            if (t.isList()) {
                for (Term member : ((ListTerm) t)) payload.add((Literal) member);
            } else {
                payload.add((Literal) t);
            }
        }

        // TODO environment computes a delta at each reasoning cycle. Alter behavior?
        // addPercept = (current environmental state) + percept
        // add events -> (current environmental state) \ (previous environmental state)
        // remove events -> (previous environmental state) \ (current environmental state)
        // [if Set is used for state, fast computation?]

        switch (actionName) {
            case "get":
                return get(resourceURI, form);

            case "watch":
                return watch(resourceURI, form);

            case "forget":
                return forget(resourceURI, form);

            case "put":
                return put(resourceURI, payload, form);

            case "post":
                return post(resourceURI, payload, form);

            case "patch":
                return patch(resourceURI, payload, form);

            case "delete":
                return delete(resourceURI, form);

            default:
                return false;
        }
    }

    @Override
    public void stop() {
        super.stop();
        Subscription.deactivateAll();
    }

    private void checkSignature(String actionName, List<Term> args) {
        switch (actionName) {
            case "get":
            case "watch":
            case "forget":
            case "delete":
                checkFirstArg(actionName, args);
                checkActionWithoutPayload(actionName, args);
                break;

            case "put":
            case "post":
            case "patch":
                checkFirstArg(actionName, args);
                checkActionWithPayload(actionName, args);
                break;

            default:
                throw new RuntimeException("Unknown action: " + actionName);
        }
    }

    private void checkFirstArg(String actionName, List<Term> args) {
        if (args.isEmpty())
            throw new RuntimeException("Action must have at least one argument: " + actionName);

        if (!(args.getFirst() instanceof StringTerm))
            throw new RuntimeException("Action's first argument must be a (URI) string: " + actionName);
    }

    private void checkActionWithoutPayload(String actionName, List<Term> args) {
        if (args.size() > 2) {
            String msg = String.format("Action expects 1 or 2 arguments but %d were provided: %s", args.size(), actionName);
            throw new RuntimeException(msg);
        }

        if (args.size() == 2 && !args.get(1).isMap()) {
            String argType = args.get(1).getClass().getSimpleName();
            String msg = String.format("Action's 2nd argument must be a MapTerm but got a %s: %s", argType, actionName);
            throw new RuntimeException(msg);
        }
    }

    private void checkActionWithPayload(String actionName, List<Term> args) {
        if (args.size() < 2 || args.size() > 3) {
            String msg = String.format("Action expects 2 or 3 arguments but %d were provided: %s", args.size(), actionName);
            throw new RuntimeException(msg);
        }

        if (!args.get(1).isStructure()) {
            String argType = args.get(1).getClass().getSimpleName();
            String msg = String.format("Action's 2nd argument must be a Structure but got a %s: %s", argType, actionName);
            throw new RuntimeException(msg);

            // TODO if arg is list, check that all elements are literals
        }

        if (args.size() == 3 && !args.get(2).isMap()) {
            String argType = args.get(2).getClass().getSimpleName();
            String msg = String.format("Action's 3nd argument must be a MapTerm but got a %s: %s", argType, actionName);
            throw new RuntimeException(msg);
        }
    }

    /**
     * <p>
     *   Asks the server for a representation of {@code resourceURI}. Once a response is
     *   received from the server, its payload is exposed as a (list of) AgentSpeak literal(s)
     *   before returning. The calling agent can thus safely query the resource's
     *   representation after the call returns, as follows:
     * </p>
     * <pre><code>+!retrieve_then_query(URI) &lt;-
  h.target(URI, TargetURI) ;
  get(URI) ;
  for (rdf(URI, P, O)[source_uri(TargetURI)]) {
    .print("Found: ", P, O)
  } .</code></pre>
     * <p>
     *   Any piece of representation found in the server's response will be turned into a
     *   literal with a {@code source_uri} annotation. In some cases, the URI provided by the
     *   agent may not be the exact target used to request the server (URI fragments are
     *   stripped away). This is why Hypermedea also provides the {@link h.target} internal
     *   action to extract the corresponding target from any URI.
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
    private boolean get(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.GET);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        return runOperation(op);
    }

    /**
     * <p>
     *   Subscribes to any change in the representation of {@code resourceURI}, to be sent asynchronously
     *   by the server. Not all protocols may support this operation type. Plain HTTP, for instance, doesn't.
     * </p>
     * <p>
     *   In contrast to a {@link #get(String, Map) get} operation, the call may return before having
     *   received any representation from the server. The call returns as soon as the server confirms the
     *   request has been processed (or, if the underlying protocol has no acknowledgement mechanism, it
     *   returns immediately). The caller agent should therefore wait for events corresponding to
     *   server notifications, as follows:
     * </p>
     * <pre><code>+!watch_only(URI) &lt;-
  h.target(URI, TargetURI) ;
  watch(URI) ;
  +watching(TargetURI) .

+rdf(S, P, O)[source_uri(TargetURI)] : watching(TargetURI) &lt;-
  .print("Received: ", S, P, O) .</code></pre>
     * <p>
     *   or, if a single notification is enough:
     * </p>
     * <pre><code>+!watch_then_wait(URI) &lt;-
  h.target(URI, TargetURI) ;
  watch(URI) ;
  .wait({ +(json(Val)[source_uri(TargetURI)] }) ;
  .print("Received: ", Val) .</code></pre>
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
    private boolean watch(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.WATCH);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        Subscription sub = new Subscription(op);
        op.registerResponseCallback(sub);

        return runOperation(op);
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
    private boolean forget(String resourceURI, Map<String, Object> formFields) {
        updateRepresentation(resourceURI, new HashSet<>());
        Subscription.deactivate(resourceURI);

        return true;
    }

    /**
     * <p>
     *   Asks the server to replace the current representation(s) it has of {@code resourceURI} with
     *   the provided {@code representation}. This parameter must be provided as a (list of) Jason
     *   literal(s), in order to be serialized in a standard format by the proper representation
     *   handler (see package {@link org.hypermedea.ct ct}).
     * </p>
     * <p>
     *   After the call returns, the caller agent may assume the new representation of {@code resourceURI}
     *   on server side is the provided one. There is however no strict guarantee. The server may also
     *   alter this representation, e.g. to maintain consistency with other resources it manages or to add
     *   metadata (modification date, author, etc.). This is why Hypermedea makes no assumption about what
     *   the new representation is. If the caller agent wants to cache the new representation, it
     *   should execute a {@link #get(String, Map) get} operation right after {@code put}, as follows:
     * </p>
     * <pre><code>+!put_then_get(URI) &lt;-
  h.target(URI, TargetURI) ;
  put(URI, [json(5)]) ;
  get(URI) ;
  // the artifact then exposes json(5)[source_uri(TargetURI)]
  // or, for instance, json({ value -> 5, modified -> 1700304346 })[source_uri(TargetURI)]
  .</code></pre>
     * <p>
     *   By default, the Hypermedea artifact deletes the outdated representation of {@code resourceURI}
     *   when the call returns.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param representation a resource representation to send to the server, in the form of Jason literals
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    private boolean put(String resourceURI, Collection<Literal> representation, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PUT);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representation);

        return runOperation(op);
    }

    /**
     * <p>
     *   Asks the server to append the provided {@code representationPart} to the current representation
     *   of {@code resourceURI}. As for {@link #put(String, Collection, Map) put}, it is not guaranteed
     *   that the server does exactly what the agent asked. It may remove other parts of the representation
     *   to maintain consistency or add more information to it.
     * </p>
     * <p>
     *   Notably, a server may also create a new resource as a side effect of the operation. This new
     *   resource should either be linked from the (new) representation of {@code resourceURI}, if its
     *   Content-Type supports hypermedia, or be exposed in a message header, hidden by the protocol binding.
     *   For instance, in response to a POST request, an HTTP server may return a {@code 201 Created}
     *   response that includes a Location header pointing to the new resource. To expose this
     *   information to the caller agent, Hypermedea builds an RDF triple from the location header and
     *   adds it to the representation of {@code resourceURI}. The agent may then query that RDF triple,
     *   as follows:
     * </p>
     * <pre><code>+!post_then_follow_link(URI) &lt;-
  h.target(URI, TargetURI) ;
  post(URI, [json(5)]) ;
  ?(rdf(TargetURI, "related", CreatedResourceURI)) ;
  .print("Created resource: ", CreatedResourceURI) ;
  h.target(CreatedResourceURI, CreatedTargetURI) ;
  get(CreatedResourceURI) ;
  ?(json(Val)[source(CreatedTargetURI)]) ;
  .print(Val) . // should include "5"</code></pre>
     * <p>
     *   In the above example, the {@code related} predicate is set by default. However, in some cases,
     *   protocol bindings may choose a more precise predicate, if the context permits it (for instance
     *   <a href="http://purl.org/dc/terms/hasPart"><code>dct:hasPart</code></a>).
     * </p>
     * <p>
     *   Note that, as for {@link #put(String, Collection, Map) put}, if Hypermedea had
     *   a representation of {@code resourceURI} before the operation, this representation is deleted
     *   when the call returns.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param representationPart part of a resource representation to send to the server, in the form of Jason literals
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    private boolean post(String resourceURI, Collection<Literal> representationPart, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.POST);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representationPart);

        return runOperation(op);
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
    private boolean patch(String resourceURI, Collection<Literal> representationDiff, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PATCH);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representationDiff);

        return runOperation(op);
    }

    /**
     * <p>
     *   Asks the server to delete all known representations of {@code resourceURI}. Hypermedea
     *   also deletes all representations locally (if the operation succeeds), as there is no
     *   ambiguity as to what should happen on the server. However, other resources may have
     *   altered representations after the operation, which agents are unaware of. For instance,
     *   the server might delete all references of the resource in the representation of other
     *   resources it manages.
     * </p>
     *
     * @param resourceURI the URI of a resource
     * @param formFields a collection of form fields (key/value pairs), to parameterize the operation, the
     *                   protocol binding or the payload binding
     */
    private boolean delete(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.DELETE);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        return runOperation(op);
    }

    /**
     * Sends the request that will start an operation and waits for an initial response from the server.
     * If the operation is a WATCH operation, the operation remains active after this method returns
     * (until {@link #forget(String, Map) forget} is called on the target resource).
     * Otherwise, the operation must have ended before the method returns.
     *
     * @param op an operation bound to a protocol binding
     * @return the input operation, for call chaining
     */
    private boolean runOperation(Operation op) {
        try {
            op.sendRequest();
            getLogger().info(op.toString());

            if (!op.isAsync()) {
                Response res = op.getResponse();
                getLogger().info(res.toString());

                if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                    // TODO more details
                    getLogger().warning("The server returned an error: " + res.getStatus());
                    return false;
                } else {
                    updateRepresentation(op.getTargetURI(), res.getPayload());
                }
            }

            return true;
        } catch (IOException e) {
            getLogger().warning(e.getLocalizedMessage());
            return false;
        }
    }

    private void updateRepresentation(String resourceURI, Collection<Literal> newRepresentation) {
        Structure src = ASSyntax.createStructure(SOURCE_FUNCTOR, ASSyntax.createString(resourceURI));

        // TODO do once and keep functors only
        ServiceLoader<RepresentationHandler> loader = ServiceLoader.load(RepresentationHandler.class);

        loader.forEach(h -> {
            VarTerm[] vars = new VarTerm[h.getArity()];
            for (int i = 0; i < vars.length; i++) vars[i] = ASSyntax.createVar();

            Literal l = ASSyntax.createLiteral(h.getFunctor(), vars);
            l.addAnnot(src);

            removePerceptsByUnif(l);
        });

        newRepresentation.forEach(l -> l.addAnnot(src));
        newRepresentation.forEach(this::addPercept);
    }

}
