package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.OpFeedbackParam;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.ThingDescription.TDFormat;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.InteractionAffordance;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpResponse;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.schemas.DataSchema;
import ch.unisg.ics.interactions.wot.td.security.APIKeySecurityScheme;
import ch.unisg.ics.interactions.wot.td.security.SecurityScheme;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import ch.unisg.ics.interactions.wot.td.vocabularies.WoTSec;
import jason.asSyntax.*;
import jason.asSyntax.parser.ParseException;

import java.io.IOException;
import java.util.Optional;

/**
 * A CArtAgO artifact that can interpret a W3C WoT Thing Description (TD) and exposes the affordances
 * of the described Thing to agents. The artifact uses the hypermedia controls provided in the TD to
 * compose and issue HTTP requests for the exposed affordances.
 *
 * @author Andrei Ciortea, Olivier Boissier, Victor Charpenay
 */
public class ThingArtifact extends Artifact {

    private static final String WEBID_PREFIX = "http://hypermedea.org/#";

    private ThingDescription td;

    private Optional<String> apiKey;

    private boolean dryRun;

    /**
     * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
     * this artifact is retrieved and parsed during initialization.
     *
     * @param url A URL that dereferences to a W3C WoT Thing Description.
     */
    public void init(String url) {
        try {
            td = TDGraphReader.readFromURL(TDFormat.RDF_TURTLE, url);

            for (SecurityScheme scheme : td.getSecuritySchemes()) {
                defineObsProperty("securityScheme", scheme.getSchemaType());
            }
        } catch (IOException e) {
            failed(e.getMessage());
        }

        this.apiKey = Optional.empty();
        this.dryRun = false;
    }

    /**
     * Method called by CArtAgO to initialize the artifact. The W3C WoT Thing Description (TD) used by
     * this artifact is retrieved and parsed during initialization.
     *
     * @param url    A URL that dereferences to a W3C WoT Thing Description.
     * @param dryRun When set to true, the requests are logged, but not executed.
     */
    public void init(String url, boolean dryRun) {
        init(url);
        this.dryRun = dryRun;
    }

    /**
     * CArtAgO operation for reading a property of a Thing using a semantic model of the Thing.
     *
     * @param propertyName The property's name.
     * @param output      The read value. Can be a list of one or more primitives, or a nested list of
     *                    primitives or arbitrary depth.
     */
    @OPERATION
    public void readProperty(String propertyName, OpFeedbackParam<Object> output) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Optional<TDHttpResponse> response = executeRequest(property, TD.readProperty, Optional.empty(),  null);

        if (!dryRun) {
            if (!response.isPresent()) {
                failed("Something went wrong with the read property request.");
            }

            if (requestSucceeded(response.get().getStatusCode())) {
                readPayloadWithSchema(response.get(), property.getDataSchema(), output);
            } else {
                failed("Status code: " + response.get().getStatusCode());
            }
        }
    }

    /**
     * CArtAgO operation for subscribing to a property of a Thing.
     *
     * TODO implement WebSub instead of long polling?
     *
     * @param propertyName The property's name (which will also be the name of the observable property created in the Artifact).
     */
    @OPERATION
    public void observeProperty(String propertyName, int timer) {
        Thread t = new Thread(() -> {
            OpFeedbackParam<Object> output = new OpFeedbackParam<>();
            while (true) {
                beginExternalSession();
                readProperty(propertyName, output);
                if (hasObsProperty(propertyName)) {
                    try {
                        if (!getObsProperty(propertyName).equals(output.get())) {
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
     * CArtAgO operation for writing a property of a Thing using a semantic model of the Thing.
     *
     * @param propertyName The property's name.
     * @param payload      The payload to be issued when writing the property.
     */
    @OPERATION
    public void writeProperty(String propertyName, Object payload) {
        PropertyAffordance property = getPropertyOrFail(propertyName);

        Optional<DataSchema> schema = Optional.of(property.getDataSchema());

        Optional<TDHttpResponse> response = executeRequest(property, TD.writeProperty, schema, payload);

        if (response.isPresent() && !requestSucceeded(response.get().getStatusCode())) {
            failed("Status code: " + response.get().getStatusCode());
        }
    }

    /**
     * CArtAgO operation for invoking an action on a Thing using a semantic model of the Thing.
     *
     * TODO return action's output
     *
     * @param actionName   Either an IRI that identifies the action type, or the action's name.
     * @param payload     The payload to be issued when invoking the action.
     *
     */
    @OPERATION
    public void invokeAction(String actionName, Object payload) {
        Optional<ActionAffordance> action = td.getActionByName(actionName);

        if (action.isPresent()) {
            Optional<DataSchema> inputSchema = action.get().getInputSchema();

            if (!inputSchema.isPresent() && payload != null) {
                log("Input payload ignored. Action " + actionName + " does not take any input.");
            }

            Optional<TDHttpResponse> response = executeRequest(action.get(), TD.invokeAction, inputSchema, payload);

            if (response.isPresent() && !requestSucceeded(response.get().getStatusCode())) {
                failed("Status code: " + response.get().getStatusCode());
            }
        } else {
            failed("Unknown action: " + actionName);
        }
    }

    @OPERATION
    public void invokeAction(String actionName) {
        invokeAction(actionName, null);
    }

    /**
     * CArtAgO operation that sets an authentication token (used with APIKeySecurityScheme).
     *
     * @param token The authentication token.
     */
    @OPERATION
    public void setAPIKey(String token) {
        if (token != null && !token.isEmpty()) {
            this.apiKey = Optional.of(token);
        }
    }

    /**
     * Matches the entire 2XX class
     */
    private boolean requestSucceeded(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    /* Tries to retrieve a property first by semantic tag, then by name. Fails if none works. */
    private PropertyAffordance getPropertyOrFail(String propertyName) {
        Optional<PropertyAffordance> property = td.getPropertyByName(propertyName);

        if (!property.isPresent()) {
            failed("Unknown property: " + propertyName);
        }

        return property.get();
    }

    @SuppressWarnings("unchecked")
    private void readPayloadWithSchema(TDHttpResponse response, DataSchema schema, OpFeedbackParam<Object> output) {
        switch (schema.getDatatype()) {
            case DataSchema.BOOLEAN:
                output.set(response.getPayloadAsBoolean());
                break;
            case DataSchema.STRING:
                output.set(response.getPayloadAsString());
                break;
            case DataSchema.INTEGER:
                output.set(response.getPayloadAsInteger());
                break;
            case DataSchema.NUMBER:
                output.set(response.getPayloadAsDouble());
                break;
            case DataSchema.OBJECT:
            case DataSchema.ARRAY:
                throw new RuntimeException("Not implemented"); // TODO
            default:
                break;
        }
    }

    private Optional<TDHttpResponse> executeRequest(InteractionAffordance affordance, String operationType, Optional<DataSchema> schema, Object payload) {
        Optional<Form> form = affordance.getFirstFormForOperationType(operationType);

        if (!form.isPresent()) {
            // Should not happen (an exception will be raised by the TD library first)
            failed("Invalid TD: the affordance does not have a valid form.");
        }

        try {
            TDHttpRequest request = new TDHttpRequest(form.get(), operationType);

            if (schema.isPresent() && payload != null) {
                Term p = ASSyntax.parseTerm(payload.toString());

                if (p.isStructure()) {
                    setObjectPayload(request, schema.get(), (Structure) p);
                } else if (p.isList()) {
                    setArrayPayload(request, schema.get(), (ListTerm) p);
                } else if (p.isString() || p.isAtom() || p.isNumeric()) {
                    setPrimitivePayload(request, schema.get(), p);
                } else {
                    failed("Could not detect the type of payload (primitive, object, or array).");
                    return Optional.empty();
                }
            }

            return issueRequest(request);
        } catch (ParseException e) {
            // Should not happen (original object was a Jason term)
            failed("Invalid payload.");
            return Optional.empty();
        }
    }

    /**
     * Set a primitive payload.
     */
    TDHttpRequest setPrimitivePayload(TDHttpRequest request, DataSchema schema, Term payload) {
        try {
            // TODO better handling of booleans?
            if (payload.isAtom() && payload.equals(Literal.LTrue)) {
                // Matches to TD BooleanSchema
                request.setPrimitivePayload(schema, true);
            } else if (payload.isAtom() && payload.equals(Literal.LFalse)) {
                // Matches to TD BooleanSchema
                request.setPrimitivePayload(schema, false);
                // TODO differentiqte between integers and other numbers
//            } else if (payload.isNumeric()) {
//                // Matches to TD IntegerSchema
//                request.setPrimitivePayload(schema, Long.valueOf(String.valueOf(payload)));
            } else if (payload.isNumeric()) {
                // Matches to TD NumberSchema
                request.setPrimitivePayload(schema, Double.valueOf(String.valueOf(payload)));
            } else if (payload.isString()) {
                // Matches to TD StringSchema
                // TODO allow for atoms as well?
                request.setPrimitivePayload(schema, payload.toString());
            } else {
                failed("Unable to detect the primitive datatype of payload: "
                        + payload.getClass().getCanonicalName());
            }
        } catch (IllegalArgumentException e) {
            failed(e.getMessage());
        }

        return request;
    }

    /**
     * Set a TD ObjectSchema payload
     */
    TDHttpRequest setObjectPayload(TDHttpRequest request, DataSchema schema, Structure payload) {
        if (schema.getDatatype() != DataSchema.OBJECT) {
            failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
                    + schema.getDatatype());
        }

        throw new RuntimeException("Not implemented"); // TODO
    }

    /**
     * Set a TD ArraySchema payload
     */
    TDHttpRequest setArrayPayload(TDHttpRequest request, DataSchema schema, ListTerm payload) {
        if (schema.getDatatype() != DataSchema.ARRAY) {
            failed("TD mismatch: illegal arguments, this affordance uses a data schema of type "
                    + schema.getDatatype());
        }

        throw new RuntimeException("Not implemented"); // TODO
    }

    private Optional<TDHttpResponse> issueRequest(TDHttpRequest request) {
        Optional<SecurityScheme> scheme = td.getFirstSecuritySchemeByType(WoTSec.APIKeySecurityScheme);

        if (scheme.isPresent() && apiKey.isPresent()) {
            request.setAPIKey((APIKeySecurityScheme) scheme.get(), apiKey.get());
        }

        // Set a header with the id of the operating agent
        request.addHeader("X-Agent-WebID", WEBID_PREFIX + getCurrentOpAgentId().getAgentName());
        log("operating agent: " + getCurrentOpAgentId().getAgentName());

        if (this.dryRun) {
            log(request.toString());
            return Optional.empty();
        } else {
            log(request.toString());
            try {
                return Optional.of(request.execute());
            } catch (IOException e) {
                failed(e.getMessage());
            }
        }

        return Optional.empty();
    }

}
