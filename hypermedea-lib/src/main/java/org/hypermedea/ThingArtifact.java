package org.hypermedea;

import cartago.ArtifactConfig;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import cartago.OperationException;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.*;
import ch.unisg.ics.interactions.wot.td.bindings.BindingNotRegisteredException;
import ch.unisg.ics.interactions.wot.td.bindings.Operation;
import ch.unisg.ics.interactions.wot.td.bindings.ProtocolBindings;
import ch.unisg.ics.interactions.wot.td.bindings.Response;
import ch.unisg.ics.interactions.wot.td.bindings.http.TDHttpOperation;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.parser.ParseException;
import org.hypermedea.json.JsonTermWrapper;
import org.hypermedea.json.TermJsonWrapper;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.ld.Resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * <p>
 *  A CArtAgO artifact that can interpret a
 *  <a href="https://www.w3.org/TR/wot-thing-description/">W3C WoT Thing Description (TD)</a> document
 *  and expose the affordances (i.e. potential actions) of the described Thing to agents.
 *  The artifact uses the hypermedia controls provided in the TD to compose and issue HTTP
 *  requests for the exposed affordances.
 * </p>
 *
 * <p>
 *   Operations exposed by a <code>ThingArtifact</code> correspond to the TD operation types:
 * </p>
 * <ul>
 *   <li>
 *       {@link #readProperty(String, OpFeedbackParam) readProperty},
 *       {@link #writeProperty(String, Object) writeProperty} and
 *       {@link #observeProperty(String, String, int) observeProperty}
 *       (for property affordances)
 *   </li>
 *   <li>
 *       {@link #invokeAction(String) invokeAction} (for action affordances)
 *   </li>
 *   <li>
 *       TODO <code>subscribeEvent</code> (for event affordances)
 *   </li>
 * </ul>
 *
 * <p>
 *   Additional operations for authentication are also available.
 * </p>
 *
 * <p>
 *     To register custom WoT protocol bindings (e.g. for MQTT, OPC UA, ROS, etc.) to all
 *     <code>ThingArtifact</code>s, create the file {@value BINDING_CONFIG_FILENAME} in the root folder
 *     of your application and declare binding class names as follows (one Java class name per line):
 * </p>
 *
 * <pre>
 * org.hypermedea.opcua.OpcUaBinding
 * org.hypermedea.ros.ROSBinding
 * ...
 * </pre>
 *
 * <p>
 *     See
 *     <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/thing"><code>examples/thing</code></a>,
 *     <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/itm-factory"><code>examples/itm-factory</code></a> and
 *     <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/leubot"><code>examples/leubot</code></a>
 *     for examples with WoT Things.
 * </p>
 *
 * @author Andrei Ciortea, Olivier Boissier, Victor Charpenay
 */
public class ThingArtifact extends HypermedeaArtifact {

    private class TDListener implements RequestListener {

        @Override
        public void requestCompleted(Resource res) {
            // TODO test whether a TD is defined in the resource.
            if (false) {
                try {
                    // TODO get name from TD instead, to prevent duplicates? Or better if several TDs for the same Thing?
                    String name = res.getURI();
                    // TODO serialize TD in string buffer and add to params
                    ArtifactConfig params = new ArtifactConfig();
                    // FIXME no calling agent, not possible to create artifacts this way?
                    makeArtifact(name, ThingArtifact.class.getName(), params);
                    // TODO expose artifactID?
                } catch (OperationException e) {
                    // TODO log error
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * Name of the configuration file that should be used to declare custom bindings.
     */
    public static final String BINDING_CONFIG_FILENAME = "bindings.txt";

    /**
     * Functor used to advertise that a new resource has been created.
     * TODO as RDF triple instead? Would require anchor information
     * TODO or as Link object?
     */
    public static final String RESOURCE_FUNCTOR = "resource";

    private static final String WEBID_PREFIX = "http://hypermedea.org/#";

    private ThingDescription td;

    private Optional<String> apiKey;

    private Optional<String> basicAuth;

    private boolean dryRun;

    static {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(BINDING_CONFIG_FILENAME));

            reader.lines().forEach((String bindingClass) -> {
                try {
                    ProtocolBindings.registerBinding(bindingClass);
                } catch (BindingNotRegisteredException e) {
                    e.printStackTrace();
                    // TODO log error
                }
            });
        } catch (FileNotFoundException e) {
            // do nothing
            // TODO log (debug) that no binding file was found
        }
    }

    /**
     * Call {@link #init(String, boolean) init(String, false)}.
     *
     * @param url a URL that dereferences to a W3C WoT Thing Description.
     */
    public void init(String url) {
        try {
            td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, url);

            for (SecurityScheme scheme : td.getSecuritySchemes()) {
                defineObsProperty("securityScheme", scheme.getSchemeType());
            }
        } catch (IOException e) {
            failed(e.getMessage());
        }

        this.apiKey = Optional.empty();
        this.basicAuth = Optional.empty();
        this.dryRun = false;

        crawlerListener = new TDListener();

        super.init();
    }

    /**
     * Initialize the artifact. The W3C WoT Thing Description (TD) used by
     * this artifact is retrieved and parsed during initialization.
     *
     * @param url a URL that dereferences to a W3C WoT Thing Description.
     * @param dryRun when set to true, the requests are logged, but not executed.
     */
    public void init(String url, boolean dryRun) {
        init(url);
        this.dryRun = dryRun;
    }

    /**
     * Read a property of a Thing by name.
     *
     * @param propertyName the property's name.
     * @param output the read value. Can be a list of one or more primitives, or a nested list of
     *               primitives or arbitrary depth.
     */
    @OPERATION
    public void readProperty(String propertyName, OpFeedbackParam<Object> output) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Optional<Response> response = executeRequest(property, TD.readProperty, Optional.empty(),  null);

        if (!dryRun) {
            if (!response.isPresent()) {
                failed("Something went wrong with the read property request.");
            }

            if (response.get().getStatus().equals(Response.ResponseStatus.OK) && response.get().getPayload().isPresent()) {
                Object value = response.get().getPayload().get();
                output.set(new JsonTermWrapper(value).getTerm());
            } else if (!response.get().getStatus().equals(Response.ResponseStatus.OK)) {
                failed("Status: " + response.get().getStatus());
            } else {
                failed("No payload returned by the Thing");
            }
        }
    }

    /**
     * Observe a property of a Thing by name (subscribe to change notifications on the property).
     *
     * TODO cartago.Artifact already includes an observeProperty operation. Add a 3rd parameter to distinguish the two
     * TODO replace stubLabel with an outputParam with a ref to the property and override the parent operation
     *
     * TODO implement WebSub instead of long polling?
     *
     * @param propertyName the property's name (which will also be the name of the observable property created in the Artifact).
     * @param timer a time interval in ms between each property read
     */
    @OPERATION
    public void observeProperty(String propertyName, String stubLabel, int timer) {
        Thread t = new Thread(() -> {
            OpFeedbackParam<Object> output = new OpFeedbackParam<>();
            while (true) {
                beginExternalSession();
                readProperty(propertyName, output);
                if (hasObsProperty(propertyName)) {
                    try {
                        if (!getObsProperty(propertyName).getValue().equals(output.get())) {
                            getObsProperty(propertyName).updateValues(output.get());
                        }
                    } catch (IllegalArgumentException e) {
                    }
                } else {
                    defineObsProperty(propertyName, output.get());
                }
                endExternalSession(true);
                try {
                    Thread.sleep(timer);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed("Property polling thread was interrupted");
                }
            }
        });
        t.start();
    }

    /**
     * Write a property of a Thing by name.
     *
     * @param propertyName the property's name.
     * @param payload the payload to be issued when writing the property.
     */
    @OPERATION
    public void writeProperty(String propertyName, Object payload) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Optional<DataSchema> schema = Optional.of(property.getDataSchema());

        Optional<Response> response = executeRequest(property, TD.writeProperty, schema, payload);

        if (response.isPresent() && !response.get().getStatus().equals(Response.ResponseStatus.OK)) {
            failed("Status: " + response.get().getStatus());
        }
    }

    /**
     * Invoke an action on a Thing by name.
     *
     * TODO return action's output
     *
     * @param actionName the action's name.
     * @param input the input payload to be issued when invoking the action as a Jason structure.
     * @param outputOrURI the output of the action, as provided by the Thing or the URI of the ongoing action.
     *
     */
    @OPERATION
    public void invokeAction(String actionName, Object input, OpFeedbackParam<Object> outputOrURI) {
        Optional<ActionAffordance> action = td.getActionByName(actionName);

        if (action.isPresent()) {
            Optional<DataSchema> inputSchema = action.get().getInputSchema();

            if (!inputSchema.isPresent() && input != null) {
                log("Input payload ignored. Action " + actionName + " does not take any input.");
            }

            Optional<Response> response = executeRequest(action.get(), TD.invokeAction, inputSchema, input);

            if (response.isPresent() && !response.get().getStatus().equals(Response.ResponseStatus.OK)) {
                failed("Status: " + response.get().getStatus());
            } else if (outputOrURI != null) {
                Response res = response.get();

                // TODO improve detection of "201 Location" header value (currently, empty rel)
                Optional<Link> linkToNewResource = res.getLinks().stream().filter(l -> l.getRelationType().isEmpty()).findFirst();

                if (linkToNewResource.isPresent()) {
                    String uri = linkToNewResource.get().getTarget();
                    StringTerm uriTerm = ASSyntax.createString(uri);

                    Term t = ASSyntax.createLiteral(RESOURCE_FUNCTOR, uriTerm);
                    outputOrURI.set(t);
                } else if (res.getPayload().isPresent()) {
                    Object payload = res.getPayload().get();

                    JsonTermWrapper w = new JsonTermWrapper(payload);
                    outputOrURI.set(w.getTerm());
                }
            }
        } else {
            failed("Unknown action: " + actionName);
        }
    }

    @OPERATION
    public void invokeAction(String actionName, Object input) {
        invokeAction(actionName, input, null);
    }

    @OPERATION
    public void invokeAction(String actionName) {
        invokeAction(actionName, null, null);
    }

    @OPERATION
    public void invokeAction(String actionName, OpFeedbackParam<Object> outputOrURI) {
        invokeAction(actionName, null, outputOrURI);
    }

    @OPERATION
    public void queryAction(String actionName, Object uriVariables, OpFeedbackParam<Object> output) {
        Optional<ActionAffordance> action = td.getActionByName(actionName);

        if (action.isPresent()) {
            try {
                Term t = parseCArtAgOObject(uriVariables);

                TermJsonWrapper w = new TermJsonWrapper(t);
                // TODO get kv for uri variables

                executeRequest(action.get(), TD.invokeAction, Optional.empty(), null);
            } catch (ParseException e) {
                failed("Invalid URI variables: " + uriVariables);
            }
        } else {
            failed("Unknown action: " + actionName);
        }
    }

    /**
     * Define credentials to include in a <code>Authorization</code> header (for HTTP bindings).
     *
     * @param username a username
     * @param password a password
     */
    @OPERATION
    public void setAuthCredentials(String username, String password) {
        if (username != null && password != null) {
            String creds = String.format("%s:%s", username, password);
            String val = Base64.getEncoder().encodeToString(creds.getBytes(StandardCharsets.UTF_8));
            this.basicAuth = Optional.of(val);
        }
    }

    /**
     * Set an authentication token (used with <code>APIKeySecurityScheme</code>).
     *
     * @param token The authentication token.
     */
    @OPERATION
    public void setAPIKey(String token) {
        if (token != null && !token.isEmpty()) {
            this.apiKey = Optional.of(token);
        }
    }

    /* Tries to retrieve a property first by semantic tag, then by name. Fails if none works. */
    private PropertyAffordance getPropertyOrFail(String propertyName) {
        Optional<PropertyAffordance> property = td.getPropertyByName(propertyName);

        if (!property.isPresent()) {
            failed("Unknown property: " + propertyName);
        }

        return property.get();
    }

    private Optional<Response> executeRequest(InteractionAffordance affordance, String operationType, Optional<DataSchema> schema, Object payload) {
        Optional<Form> form = affordance.getFirstFormForOperationType(operationType);

        if (!form.isPresent()) {
            // Should not happen (an exception will be raised by the TD library first)
            failed("Invalid TD: the affordance does not have a valid form.");
        }

        try {
            Operation op = ProtocolBindings.getBinding(form.get()).bind(form.get(), operationType);
            // TODO or .bind() with uri variables

            if (schema.isPresent() && payload != null) {
                Term p = parseCArtAgOObject(payload);

                TermJsonWrapper w = new TermJsonWrapper(p);

                if (w.isJsonBoolean() || w.isJsonNumber() || w.isJsonString()) setPrimitivePayload(op, schema.get(), w);
                else if (w.isJsonArray()) setArrayPayload(op, schema.get(), w);
                else if (w.isJsonObject()) setObjectPayload(op, schema.get(), w);
                else {
                    failed("Could not detect the type of payload (primitive, object, or array).");
                    return Optional.empty();
                }
            }

            return issueRequest(op);
        } catch (ParseException e) {
            // Should not happen (original object was a Jason term)
            failed("Invalid payload.");
            return Optional.empty();
        }
    }

    Operation setPrimitivePayload(Operation op, DataSchema schema, TermJsonWrapper w) {
        try {
            if (w.isJsonBoolean()) {
                op.setPayload(schema, w.getJsonBoolean());
            } else if (w.isJsonNumber()) {
                op.setPayload(schema, w.getJsonNumber());
            } else if (w.isJsonString()) {
                op.setPayload(schema, w.getJsonString());
            } else {
                failed("Unable to detect the primitive datatype of payload: " + w.getJsonValue());
            }
        } catch (IllegalArgumentException e) {
            failed(e.getMessage());
        }

        return op;
    }

    Operation setObjectPayload(Operation op, DataSchema schema, TermJsonWrapper w) {
        if (schema.getDatatype() != DataSchema.OBJECT) {
            failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
                    + schema.getDatatype());
        }

        op.setPayload(schema, w.getJsonObject());
        return op;
    }

    Operation setArrayPayload(Operation op, DataSchema schema, TermJsonWrapper w) {
        if (schema.getDatatype() != DataSchema.ARRAY) {
            failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
                    + schema.getDatatype());
        }

        op.setPayload(schema, w.getJsonArray());
        return op;
    }

    private Optional<Response> issueRequest(Operation op) {
        if (apiKey.isPresent() && op instanceof TDHttpOperation) {
            TDHttpOperation httpOp = (TDHttpOperation) op;
            Optional<SecurityScheme> scheme = td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme);
            if (scheme.isPresent()) httpOp.setAPIKey((APIKeySecurityScheme) scheme.get(), apiKey.get());
        } else if (apiKey.isPresent()) {
            log("Warning: API key auth is only supported for HTTP bindings. Key given to artifact was ignored.");
        }

        if (basicAuth.isPresent() && op instanceof TDHttpOperation) {
            TDHttpOperation httpOp = (TDHttpOperation) op;
            // TODO if future version of wot-td-java includes the whole vocab, replace string with constant
            Optional<SecurityScheme> scheme = td.getFirstSecuritySchemeByType("BasicSecurityScheme");
            //if (scheme.isPresent())
                httpOp.addHeader("Authorization", "Basic " + basicAuth.get());
        } else if (basicAuth.isPresent()) {
            log("Warning: basic auth is only supported for HTTP bindings. Credentials given to artifact were ignored.");
        }

        log("operating agent: " + getCurrentOpAgentId().getAgentName());

        if (op instanceof TDHttpOperation) {
            // Set a header with the id of the operating agent
            TDHttpOperation httpOp = (TDHttpOperation) op;
            httpOp.addHeader("X-Agent-WebID", WEBID_PREFIX + getCurrentOpAgentId().getAgentName());
        }

        if (this.dryRun) {
            log(op.toString());
            return Optional.empty();
        } else {
            log(op.toString());
            try {
                op.sendRequest();
                Response res = op.getResponse();
                log(String.format("[%s] Status: %s", res.getClass().getTypeName(), res.getStatus())); // TODO override res.toString()
                return Optional.of(res);
            } catch (IOException e) {
                failed(e.getMessage());
            }
        }

        return Optional.empty();
    }

    /**
     * Retrieve a Jason term from a Java object processed by CArtAgO
     *
     * @param obj an object holding a Jason term mapped to Java by CArtAgo
     * @return the original Jason term (if known)
     */
    private Term parseCArtAgOObject(Object obj) throws ParseException {
        if (obj.getClass().isArray()) {
            ListTerm l = ASSyntax.createList();

            for (Object m : (Object[]) obj) l.add(parseCArtAgOObject(m));

            return l;
        } else {
            // TODO take arbitrary objects (cobj_XXX) into account
            return ASSyntax.parseTerm(obj.toString());
        }
    }

}
