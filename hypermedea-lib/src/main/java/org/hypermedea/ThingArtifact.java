package org.hypermedea;

import cartago.ArtifactObsProperty;
import cartago.OPERATION;
import cartago.ObsProperty;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.*;
import ch.unisg.ics.interactions.wot.td.bindings.*;
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
import org.hypermedea.tools.URITemplates;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 *  A CArtAgO artifact that can interpret a
 *  <a href="https://www.w3.org/TR/wot-thing-description/">W3C WoT Thing Description (TD)</a> document
 *  and expose the affordances (i.e. potential actions) of the described Thing to agents. The artifact
 *  uses the hypermedia controls provided in the TD to compose and issue requests for the exposed
 *  affordances.
 * </p>
 *
 * <p>
 *   Operations exposed by a <code>ThingArtifact</code> correspond to the TD operation types:
 * </p>
 * <ul>
 *   <li>
 *       {@link #readProperty(String, OpFeedbackParam) readProperty} and
 *       {@link #writeProperty(String, Object) writeProperty} (for property affordances)
 *   </li>
 *   <li>
 *       {@link #invokeAction(String) invokeAction},
 *       {@link #queryAction(String, Object, OpFeedbackParam) queryAction} and
 *       {@link #queryAction(String, Object, OpFeedbackParam) cancelAction} (for action affordances)
 *   </li>
 *   <li>
 *       {@link #subscribeEvent(String, Object, OpFeedbackParam)} subscribeEvent} and
 *       {@link #unsubscribeEvent(String, String, Object) unsubscribeEvent} (for event affordances)
 *   </li>
 * </ul>
 *
 * <p>
 *     The <code>observeProperty</code> operation that some TDs expose slightly differs from the
 *     operation of the same name defined in CArtAgO. Whenever, an agent observes a WoT property with
 *     name <code>propertyName</code>, the <code>ThingArtifact</code> exposes these values in
 *     observable properties of the form <code>propertyValue(PropertyName, Value)</code>.
 * </p>
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

    /**
     * Name of the configuration file that should be used to declare custom bindings.
     */
    public static final String BINDING_CONFIG_FILENAME = "bindings.txt";

    /**
     * Functor used to advertise that some resource has been created, such as actions or events.
     * For instance, an <code>invokeAction</code> operation may return the result of the action
     * or the URI of a new action resource that can be dereferenced.
     */
    public static final String RESOURCE_FUNCTOR = "resource";

    public static final String PROPERTY_VALUE_FUNCTOR = "propertyValue";

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
     * Initialize the artifact. The W3C WoT Thing Description (TD) used by
     * this artifact is retrieved and parsed during initialization.
     *
     * @param urlOrPath a URL that dereferences to a W3C WoT TD or a path to a local W3C WoT TD, in the Turtle format
     */
    public void init(String urlOrPath) {
        try {
            if (urlOrPath.startsWith("http")) {
                td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, urlOrPath);
            } else {
                td = TDGraphReader.readFromFile(TDFormat.RDF_TURTLE, urlOrPath);
            }
        } catch (IOException e) {
            failed(e.getMessage());
        }

        this.apiKey = Optional.empty();
        this.basicAuth = Optional.empty();
        this.dryRun = false;

        super.init();
    }

    /**
     * See {@link ThingArtifact#init(String)}.
     *
     * @param urlOrPath a URL that dereferences to a W3C WoT TD or a path to a local W3C WoT TD, in the Turtle format
     * @param dryRun when set to true, the requests are logged, but not executed
     */
    public void init(String urlOrPath, boolean dryRun) {
        init(urlOrPath);
        this.dryRun = dryRun;
    }

    /**
     * <p>
     *     Read a property of a Thing by name.
     *     The read value is exposed by the <code>ThingArtifact</code> as an observable property.
     *     Assume the underlying Thing exposes an affordance for the integer-valued property <code>speedLevel</code>,
     *     the read value will be accessible via the observable property <code>speedLevel(12)</code>.
     * </p>
     *
     * See also {@link #readProperty(String, OpFeedbackParam)}.
     *
     * @param propertyName the property's name.
     */
    @OPERATION
    public void readProperty(String propertyName) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Operation op = bindForOperation(property, TD.readProperty, Optional.empty(),  null, property.getUriVariables(), new HashMap<>());

        sendRequest(op);
        Optional<Response> response = waitForOKOrFail(op);

        if (!dryRun) updateValueFromResponse(propertyName, response);
    }

    /**
     * <p>
     *     Read a property of a Thing by name.
     * </p>
     * <p>
     *     The read value is provided in JSON.
     *     For documentation on the mapping between JSON and Jason terms, see {@link org.hypermedea.json}.
     * </p>
     *
     * @param propertyName the property's name.
     * @param output the read value.
     */
    @OPERATION
    public void readProperty(String propertyName, OpFeedbackParam<Object> output) {
        readProperty(propertyName);

        ObsProperty p = getObsPropertyByTemplate(PROPERTY_VALUE_FUNCTOR, propertyName, null);
        output.set(p.getValue(1));
    }

    /**
     * <p>
     *     Write a property of a Thing by name.
     * </p>
     * <p>
     *     The value to write must be provided in JSON.
     *     For documentation on the mapping between JSON and Jason terms, see {@link org.hypermedea.json}.
     * </p>
     *
     * @param propertyName the property's name.
     * @param payload the payload to be issued when writing the property.
     */
    @OPERATION
    public void writeProperty(String propertyName, Object payload) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Optional<DataSchema> schema = Optional.of(property.getDataSchema());

        Operation op = bindForOperation(property, TD.writeProperty, schema, payload, property.getUriVariables(), new HashMap<>());

        sendRequest(op);
        waitForOKOrFail(op);
    }

    /**
     * <p>
     *     Observe a property of a Thing by name (subscribe to change notifications on the property).
     * </p>
     * <p>
     *     <i>
     *         Note: this method has a distinct behavior from
     *         <code>Artifact.observeProperty(String, OpFeedbackParam)</code>.
     *     </i>
     * </p>
     *
     * @param propertyName the property's name (which will also be the name of the observable property created in the Artifact).
     */
    @OPERATION
    public void observeProperty(String propertyName) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Operation op = bindForOperation(property, TD.observeProperty, Optional.empty(),  null, property.getUriVariables(), new HashMap<>());

        op.registerResponseCallback(new ResponseCallback() {
            @Override
            public void onResponse(Response response) {
                beginExternalSession();
                updateValueFromResponse(propertyName, Optional.of(response));
                endExternalSession(true);
            }

            @Override
            public void onError() {
                beginExternalSession();
                log("observeProperty operation has failed because connection to the Thing was lost");
                endExternalSession(false);
            }
        });

        sendRequest(op);
    }

    /**
     * <p>
     *     Invoke an action on a Thing by name. The Thing may either return the output of the action, if executed
     *     synchronously, or a link pointing at the URI of the ongoing action, for asynchronous execution.
     *     Caller agents may differentiate the two cases as follows:
     * </p>
     * <ul>
     *     <li>
     *         if <code>outputOrURI</code> binds to an atom, a string or a <code>json</code> structure,
     *         it is a JSON representation of the action's output as returned by the Thing. For
     *         documentation on the mapping between JSON and Jason terms, see {@link org.hypermedea.json}.
     *     </li>
     *     <li>
     *         if <code>outputOrURI</code> binds to a <code>resource</code> structure,
     *         e.g. <code>resource("http://example.org/actions/123")</code>, the enclosed
     *         URI identifies the ongoing action. It may be used as the <code>targetOrVariableBindings</code> parameter of
     *         {@link #queryAction(String, Object, OpFeedbackParam)} or
     *         {@link #cancelAction(String, Object)}.
     *     </li>
     * </ul>
     * <p>
     *     If the returned value is a URI, that URI
     * </p>
     *
     * @param actionName the action's name.
     * @param input the input payload to be issued when invoking the action as a Jason structure.
     * @param outputOrURI the output of the action, as provided by the Thing, or the URI of the ongoing action.
     *
     */
    @OPERATION
    public void invokeAction(String actionName, Object input, OpFeedbackParam<Object> outputOrURI) {
        ActionAffordance action = getActionOrFail(actionName);

        Optional<DataSchema> inputSchema = action.getInputSchema();

        if (!inputSchema.isPresent() && input != null) {
            log("Input payload ignored. Action " + actionName + " does not take any input.");
        }

        Operation op = bindForOperation(action, TD.invokeAction, inputSchema, input, action.getUriVariables(), new HashMap<>());

        sendRequest(op);
        Optional<Response> resOpt = waitForOKOrFail(op);

        if (resOpt.isPresent() && outputOrURI != null) {
            setOutputOrURI(resOpt.get(), outputOrURI);
        }
    }

    /**
     * Equivalent to <code>invokeAction(actionName, input, null)}</code>.
     * 
     * @param actionName the action's name.
     * @param input the input payload to be issued when invoking the action as a Jason structure.
     */
    @OPERATION
    public void invokeAction(String actionName, Object input) {
        invokeAction(actionName, input, null);
    }

    /**
     * Equivalent to <code>invokeAction(actionName, null, null)}</code>.
     *
     * @param actionName the action's name.
     */
    @OPERATION
    public void invokeAction(String actionName) {
        invokeAction(actionName, null, null);
    }

    /**
     * <p>
     *      Query the status of an ongoing action performed by the Thing. The action may be addressed
     *      either directly (e.g. with the full URI <code>http://example.org/actions/123</code>)
     *      or via variables in a URI template (e.g. <code>actionId</code> binding to <code>123</code>
     *      in the URI template <code>http://example.org/actions/{actionId}</code>).
     *      Variable bindings must be given as a JSON object, with variables as keys
     *      and bindings as values).
     * </p>
     * <p>
     *     For documentation on the mapping between JSON and Jason terms, see {@link org.hypermedea.json}.
     * </p>
     *
     * @param actionName the action's name.
     * @param targetOrVariableBindings URI of the target action or URI template variable bindings.
     * @param status the status of the action.
     */
    @OPERATION
    public void queryAction(String actionName, Object targetOrVariableBindings, OpFeedbackParam<Object> status) {
        ActionAffordance action = getActionOrFail(actionName);
        Map<String, Object> varBindings = getBindings(action, TD.queryAction, targetOrVariableBindings);

        Operation op = bindForOperation(action, TD.queryAction, Optional.empty(), null, action.getUriVariables(), varBindings);

        sendRequest(op);
        Optional<Response> resOpt = waitForOKOrFail(op);

        if (resOpt.isPresent()) {
            JsonTermWrapper w = new JsonTermWrapper(resOpt.get());
            status.set(w.getTerm());
        }
    }

    /**
     * Equivalent to <code>queryAction(actionName, null, status)}</code>.
     *
     * @param actionName the action's name.
     * @param status the status of the action.
     */
    @OPERATION
    public void queryAction(String actionName, OpFeedbackParam<Object> status) {
        queryAction(actionName, null, status);
    }

    /**
     * Cancel an ongoing action. As in the <code>queryAction</code> operation, the action may be addressed
     * directly or via URI template variables.
     *
     * @param actionName the action's name.
     * @param targetOrVariableBindings URI of the target action or URI template variable bindings.
     */
    @OPERATION
    public void cancelAction(String actionName, Object targetOrVariableBindings) {
        ActionAffordance action = getActionOrFail(actionName);
        Map<String, Object> varBindings = getBindings(action, TD.cancelAction, targetOrVariableBindings);

        Operation op = bindForOperation(action, TD.cancelAction, Optional.empty(), null, action.getUriVariables(), varBindings);

        sendRequest(op);
        waitForOKOrFail(op);
    }

    /**
     * Equivalent to <code>cancelAction(actionName, null)}</code>.
     *
     * @param actionName the action's name.
     */
    @OPERATION
    public void cancelAction(String actionName) {
        cancelAction(actionName, null);
    }

    /**
     * <p>
     *     Subscribe to an event observed by the Thing. As a result, the Thing is expected to create
     *     a subscription resource whose URI is returned if the subscription operation was successful.
     *     The URI of the subscription resource is exposed to agents in a <code>resource</code> structure,
     *     e.g. <code>resource("http://example.org/subscriptions/abc")</code>.
     * </p>
     * <p>
     *     Any subsequent notification pushed by the Thing is turned into a signal by the <code>ThingArtifact</code>.
     *     For instance, if an agent subscribes to the <code>tempAboveThreshold</code> event, which the Thing
     *     emits with the latest temperature value it measured, the agent will receive signals such as
     *     <code>tempAboveThreshold(36.8)</code>. If the Thing pushes notification without payloads, signals are
     *     transmitted to agents as plain atoms (e.g. <code>tempAboveThreshold</code>).
     * </p>
     * <p>
     *     Subscribing to an event may require input data to provide to the Thing.
     *     Agents must provide it in JSON via the <code>subscription</code> parameter.
     *     Notification payloads are also exposed to agents in JSON.
     *     For documentation on the mapping between JSON and Jason terms, see {@link org.hypermedea.json}.
     * </p>
     *
     * @param eventName the event's name.
     * @param subscription the payload to be issued to the Thing when subscribing to the event
     * @param subscriptionURI a structure enclosing the URI of the active subscription returned by the Thing.
     */
    @OPERATION
    public void subscribeEvent(String eventName, Object subscription, OpFeedbackParam<Object> subscriptionURI) {
        EventAffordance event = getEventOrFail(eventName);

        Optional<DataSchema> subscriptionSchema = event.getSubscriptionSchema();

        if (!subscriptionSchema.isPresent() && subscription != null) {
            log("subscription payload ignored. Subscription to " + eventName + " does not take any input.");
        }

        Operation op = bindForOperation(event, TD.subscribeEvent, subscriptionSchema, subscription, event.getUriVariables(), new HashMap<>());

        op.registerResponseCallback(new ResponseCallback() {
            @Override
            public void onResponse(Response response) {
                beginExternalSession();
                boolean success = sendSignalFromResponse(eventName, response);
                endExternalSession(success);
            }

            @Override
            public void onError() {
                beginExternalSession();
                log("subscribeEvent operation has failed because connection to the Thing was lost");
                endExternalSession(false);
            }
        });

        sendRequest(op);
        Optional<Response> resOpt = waitForOKOrFail(op);

        if (resOpt.isPresent() && subscriptionURI != null) {
            setOutputOrURI(resOpt.get(), subscriptionURI);
        }
    }

    /**
     * Equivalent to {@link #subscribeEvent(String, Object, OpFeedbackParam) subscribeEvent(eventName, null, null)}.
     *
     * @param eventName
     */
    @OPERATION
    public void subscribeEvent(String eventName) {
        subscribeEvent(eventName, null, null);
    }

    /**
     * Cancel an active subscription to the event. The subscription may be addressed directly or via
     * URI template variables. See {@link #queryAction(String, Object, OpFeedbackParam)} for a similar interface.
     *
     * @param eventName the event's name.
     * @param targetOrVariableBindings URI of the target subscription or URI template variable bindings.
     * @param cancellation cancellation data to provide to the Thing.
     */
    @OPERATION
    public void unsubscribeEvent(String eventName, String targetOrVariableBindings, Object cancellation) {
        EventAffordance event = getEventOrFail(eventName);
        Map<String, Object> varBindings = getBindings(event, TD.unsubscribeEvent, targetOrVariableBindings);

        // TODO take cancellation payload into account

        Operation op = bindForOperation(event, TD.unsubscribeEvent, Optional.empty(), null, event.getUriVariables(), varBindings);

        sendRequest(op);
        waitForOKOrFail(op);
    }

    /**
     * Equivalent to {@link #unsubscribeEvent(String, String, Object) unsubscribeEvent(eventName, targetOrVariableBindings, null)}.
     *
     * @param eventName the event's name.
     * @param targetOrVariableBindings URI of the target subscription or URI template variable bindings.
     */
    @OPERATION
    public void unsubscribeEvent(String eventName, String targetOrVariableBindings) {
        unsubscribeEvent(eventName, targetOrVariableBindings, null);
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

    private PropertyAffordance getPropertyOrFail(String propertyName) {
        Optional<PropertyAffordance> property = td.getPropertyByName(propertyName);

        if (!property.isPresent()) {
            failed("Unknown property: " + propertyName);
        }

        return property.get();
    }

    private ActionAffordance getActionOrFail(String actionName) {
        Optional<ActionAffordance> action = td.getActionByName(actionName);

        if (!action.isPresent()) {
            failed("Unknown action: " + actionName);
        }

        return action.get();
    }

    private EventAffordance getEventOrFail(String eventName) {
        Optional<EventAffordance> event = td.getEventByName(eventName);

        if (!event.isPresent()) {
            failed("Unknown event: " + eventName);
        }

        return event.get();
    }

    /**
     * <p>
     *     Copy the content of an observable property, to expose to agents.
     * </p>
     * <p>
     *     <i>Note: this method duplicates code from <code>ObsProperty.getUserCopy()</code>, which isn't accessible to subclasses.</i>
     * </p>
     *
     * @param p an observable property
     * @return a copy of the observable property exposable to agents
     */
    private ArtifactObsProperty getUserCopy(ObsProperty p) {
        ArtifactObsProperty copy = new ArtifactObsProperty(p.getFullId(), p.getId(), p.getName(), p.getValues().clone());
        return copy.setAnnots(p.cloneAnnots());
    }

    private Map<String, Object> getBindings(InteractionAffordance affordance, String operationType, Object targetOrVariableBindings) {
        Map<String, Object> varBindings = new HashMap<>();

        if (targetOrVariableBindings != null) {
            try {
                Term t = parseCArtAgOObject(targetOrVariableBindings);
                TermJsonWrapper w = new TermJsonWrapper(t);

                if (!w.isJsonObject() && !w.isJsonString()) {
                    failed("URI variables must be a URI or a JSON object (mapping variable names to numbers, booleans or strings)");
                }

                if (w.isJsonObject()) {
                    varBindings = w.getJsonObject();
                } else {
                    String targetURI = w.getJsonString();
                    varBindings = getBindingsForTarget(affordance, operationType, targetURI);
                }
            } catch (ParseException e) {
                failed("Invalid URI variables: " + targetOrVariableBindings);
            }
        }

        return varBindings;
    }

    private Map<String, Object> getBindingsForTarget(InteractionAffordance affordance, String operationType, String targetURI) {
        Stream<Form> stream = affordance.getForms().stream().filter(f -> f.getOperationTypes().contains(operationType));
        List<Form> forms = stream.collect(Collectors.toList());

        Optional<Form> exactMatch = forms.stream().filter(f -> f.getTarget().equals(targetURI)).findAny();

        if (exactMatch.isPresent()) {
            return new HashMap<>();
        } else {
            Stream<Map<String, Object>> bindings = forms.stream().map(f -> URITemplates.bind(f.getTarget(), targetURI));
            Optional<Map<String, Object>> opt = bindings.filter(b -> !b.isEmpty()).findAny();

            return opt.isPresent() ? opt.get() : new HashMap<>();
        }
    }

    private Operation bindForOperation(InteractionAffordance affordance, String operationType,
                                       Optional<DataSchema> schema, Object payload,
                                       Optional<Map<String, DataSchema>> varSchema, Map<String, Object> varValues) {
        Optional<Form> formOpt = affordance.getFirstFormForOperationType(operationType);

        if (!formOpt.isPresent()) {
            // Should not happen (an exception will be raised by the TD library first)
            failed("Invalid TD: the affordance does not have a valid form.");
        }

        if (!varValues.isEmpty() && varSchema.isEmpty()) {
            log("URI variable bindings passed to operation will be ignored (no variable defined in affordance)");
        }

        try {
            Form form = formOpt.get();
            ProtocolBinding binding = ProtocolBindings.getBinding(form);

            Operation op = varValues.isEmpty() || varSchema.isEmpty() ?
                    binding.bind(form, operationType) :
                    binding.bind(form, operationType, varSchema.get(), varValues);

            if (schema.isPresent() && payload != null) {
                Term p = parseCArtAgOObject(payload);

                TermJsonWrapper w = new TermJsonWrapper(p);

                if (w.isJsonBoolean() || w.isJsonNumber() || w.isJsonString()) setPrimitivePayload(op, schema.get(), w);
                else if (w.isJsonArray()) setArrayPayload(op, schema.get(), w);
                else if (w.isJsonObject()) setObjectPayload(op, schema.get(), w);
                else {
                    failed("Could not detect the type of payload (primitive, object, or array).");
                }
            }

            return wrapOperation(op);
        } catch (ParseException e) {
            // Should not happen (original object was a Jason term)
            failed("Invalid payload.");
            return null;
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

    private Operation wrapOperation(Operation op) {
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

        return op;
    }

    private void sendRequest(Operation op) {
        log(op.toString());

        if (!this.dryRun) {
            try {
                op.sendRequest();
            } catch (IOException e) {
                failed(e.getMessage());
            }
        }
    }

    /**
     * Wait for a response, check its status and return the response if not in dry run mode.
     *
     * @param op the operation for which a response is expected
     * @return the response with status OK, if returned by the Thing or {@link Optional#empty()} if in dry run mode
     */
    private Optional<Response> waitForOKOrFail(Operation op) {
        if (!this.dryRun) {
            try {
                Response res = op.getResponse();
                log(res.toString());

                if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
                    failed("Thing responded with status: " + res.getStatus());
                }

                return Optional.of(res);
            } catch (IOException e) {
                failed(e.getMessage());
            }
        }

        return Optional.empty();
    }

    private void setOutputOrURI(Response res, OpFeedbackParam<Object> outputOrURI) {
        // empty rel ~ 201 Location header value
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

    private void updateValueFromResponse(String propertyName, Optional<Response> responseOpt) {
        if (!responseOpt.isPresent()) {
            failed("Something went wrong with the read/observe property request.");
        }

        Response response = responseOpt.get();

        if (!responseOpt.get().getStatus().equals(Response.ResponseStatus.OK)) {
            failed("Status: " + responseOpt.get().getStatus());
        }

        if (!response.getPayload().isPresent()) {
            failed("No payload returned by the Thing");
        }

        Object rawValue = responseOpt.get().getPayload().get();
        Term value = new JsonTermWrapper(rawValue).getTerm();

        if (hasObsPropertyByTemplate(PROPERTY_VALUE_FUNCTOR, propertyName, null)) {
            ObsProperty p = getObsPropertyByTemplate(PROPERTY_VALUE_FUNCTOR, propertyName, null);

            if (!p.getValue(1).equals(value)) {
                p.updateValue(1, value);
            }
        } else {
            defineObsProperty(PROPERTY_VALUE_FUNCTOR, propertyName, value);
        }
    }

    private boolean sendSignalFromResponse(String signalType, Response response) {
        if (!response.getStatus().equals(Response.ResponseStatus.OK)) {
            log("The Thing sent notification for event " + signalType + " with status " + response.getStatus());
            return false;
        }

        if (!response.getPayload().isPresent()) {
            signal(signalType);
        } else {
            Object payload = response.getPayload().get();
            JsonTermWrapper w = new JsonTermWrapper(payload);
            signal(signalType, w.getTerm());
        }

        return true;
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
