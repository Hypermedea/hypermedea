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
 *       {@link #invokeAction(String) invokeAction} (for action affordances)
 *   </li>
 *   <li>
 *       TODO <code>subscribeEvent</code> and <code>unsubscribeEvent</code> (for event affordances)
 *   </li>
 * </ul>
 *
 * <p>
 *     The <code>observeProperty</code> operation that some TDs expose has a special role in CArtAgO:
 *     agents explicitly observe a property to receive notifications only when property values change.
 *     The <code>ThingArtifact</code> keeps this behavior, only acting as a proxy that caches values
 *     received from the Thing.
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

            for (PropertyAffordance property : td.getProperties()) {
                defineObsProperty(property.getName(), Double.NaN);
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
     * Read a property of a Thing by name.
     * The read value is exposed by the <code>ThingArtifact</code> as an observable property.
     * Assume the underlying Thing exposes an affordance for the integer-valued property <code>prop</code>,
     * the read value will be accessible via the observable property <code>prop(12)</code>.
     *
     * See also {@link #readProperty(String, OpFeedbackParam)}.
     *
     * @param propertyName the property's name.
     */
    public void readProperty(String propertyName) {
        ObsProperty p = getObsProperty(propertyName);
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Operation op = bindForOperation(property, TD.readProperty, Optional.empty(),  null, property.getUriVariables(), new HashMap<>());
        Optional<Response> response = waitForResponse(op);

        if (!dryRun) updateValueFromResponse(p, response);
    }

    /**
     * Read a property of a Thing by name.
     *
     * @param propertyName the property's name.
     * @param output the read value. Can be a list of one or more primitives, or a nested list of
     *               primitives of arbitrary depth.
     */
    @OPERATION
    public void readProperty(String propertyName, OpFeedbackParam<Object> output) {
        readProperty(propertyName);

        ObsProperty p = getObsProperty(propertyName);
        output.set(p.getValue());
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

        Operation op = bindForOperation(property, TD.writeProperty, schema, payload, property.getUriVariables(), new HashMap<>());
        Optional<Response> response = waitForResponse(op);

        // TODO no need for the optional response; check dryRun and fail if IOException
        // TODO check status inside waitForResponse?

        if (response.isPresent() && !response.get().getStatus().equals(Response.ResponseStatus.OK)) {
            failed("Status: " + response.get().getStatus());
        }
    }

    /**
     * Observe a property of a Thing by name (subscribe to change notifications on the property).
     *
     * Note: the <code>ThingArtifact</code> classes overrides the behavior of
     * <code>Artifact.observeProperty(String, OpFeedbackParam)</code>.
     *
     * @param propertyName the property's name (which will also be the name of the observable property created in the Artifact).
     * @param propParam the observable property as exposed to agents
     */
    @OPERATION
    public void observeProperty(String propertyName, OpFeedbackParam<ArtifactObsProperty> propParam) {
        ObsProperty p = getObsProperty(propertyName);
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Operation op = bindForOperation(property, TD.observeProperty, Optional.empty(),  null, property.getUriVariables(), new HashMap<>());

        op.registerResponseCallback(new ResponseCallback() {
            @Override
            public void onResponse(Response response) {
                beginExternalSession();
                updateValueFromResponse(p, Optional.of(response));
                endExternalSession(true);
            }

            @Override
            public void onError() {
                beginExternalSession();

                log("observeProperty operation has failed because connection to the Thing was lost");

                endExternalSession(false);
            }
        });

        waitForResponse(op); // FIXME shouldn't wait for an answer

        propParam.set(getUserCopy(p));
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
        ActionAffordance action = getActionOrFail(actionName);

        Optional<DataSchema> inputSchema = action.getInputSchema();

        if (!inputSchema.isPresent() && input != null) {
            log("Input payload ignored. Action " + actionName + " does not take any input.");
        }

        Operation op = bindForOperation(action, TD.invokeAction, inputSchema, input, action.getUriVariables(), new HashMap<>());

        Optional<Response> resOpt = waitForResponse(op);
        Response res = getResponseOrFail(resOpt);

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
    public void queryAction(String actionName, Object targetOrVariableBindings, OpFeedbackParam<Object> output) {
        ActionAffordance action = getActionOrFail(actionName);
        Map<String, Object> varBindings = getBindings(action, TD.queryAction, targetOrVariableBindings);

        Operation op = bindForOperation(action, TD.queryAction, Optional.empty(), null, action.getUriVariables(), varBindings);

        Optional<Response> resOpt = waitForResponse(op);
        Response res = getResponseOrFail(resOpt);

        JsonTermWrapper w = new JsonTermWrapper(res);
        output.set(w.getTerm());
    }

    @OPERATION
    public void queryAction(String actionName, OpFeedbackParam<Object> output) {
        queryAction(actionName, null, output);
    }

    @OPERATION
    public void cancelAction(String actionName) {
        cancelAction(actionName, null);
    }

    @OPERATION
    public void cancelAction(String actionName, Object targetOrVariableBindings) {
        ActionAffordance action = getActionOrFail(actionName);
        Map<String, Object> varBindings = getBindings(action, TD.cancelAction, targetOrVariableBindings);

        Operation op = bindForOperation(action, TD.cancelAction, Optional.empty(), null, action.getUriVariables(), varBindings);

        Optional<Response> resOpt = waitForResponse(op);
        getResponseOrFail(resOpt);
    }

    @OPERATION
    public void subscribeEvent(String eventName) {
        log("subscribeEvent not implemented");

        // TODO call signal(predicate) in the notification callback
    }

    @OPERATION
    public void unsubscribeEvent(String eventName) {
        log("unsubscribeEvent not implemented");
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

    /**
     * <p>
     *     Copy the content of an observable property, to expose to agents.
     * </p>
     *
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

    private Optional<Response> waitForResponse(Operation op) {
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

    private Response getResponseOrFail(Optional<Response> opt) {
        if (opt.isEmpty()) {
            failed("No response returned by the Thing");
        }

        Response res = opt.get();

        if (!res.getStatus().equals(Response.ResponseStatus.OK)) {
            failed("The Thing responded with error status: " + res.getStatus());
        }

        return res;
    }

    private void updateValueFromResponse(ObsProperty p, Optional<Response> responseOpt) {
        if (!responseOpt.isPresent()) {
            failed("Something went wrong with the read property request.");
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

        if (!p.getValue().equals(value)) {
            p.updateValue(value);
        }
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
