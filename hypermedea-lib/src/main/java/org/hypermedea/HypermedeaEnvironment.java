package org.hypermedea;

import jason.asSyntax.*;
import jason.environment.Environment;
import org.hypermedea.ct.RepresentationHandler;
import org.hypermedea.op.Operation;
import org.hypermedea.op.ProtocolBindings;
import org.hypermedea.op.Response;
import org.hypermedea.tools.Identifiers;

import java.io.IOException;
import java.util.*;

/**
 * <i>Not yet implemented.</i>
 */
public class HypermedeaEnvironment extends Environment {

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
            for (Term k : formArg.keys()) form.put(Identifiers.getLexicalForm(k), formArg.get(k));
        }

        Optional<Term> payloadOpt = args.stream().filter(t -> t.isStructure()).findFirst();
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
        // TODO end all on-going operations
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

        if (!(args.get(0) instanceof StringTerm))
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

        if (!args.get(2).isStructure()) {
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

    private boolean get(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.GET);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        return runOperation(op);
    }

    private boolean watch(String resourceURI, Map<String, Object> formFields) {
        return false;
    }

    private boolean forget(String resourceURI, Map<String, Object> formFields) {
        Structure src = ASSyntax.createStructure("source", ASSyntax.createString(resourceURI));

        // TODO do once and keep functors only
        ServiceLoader<RepresentationHandler> loader = ServiceLoader.load(RepresentationHandler.class);

        loader.forEach(h -> {
            VarTerm[] vars = new VarTerm[h.getArity()];
            for (int i = 0; i < vars.length; i++) vars[i] = ASSyntax.createVar();

            Literal l = ASSyntax.createLiteral(h.getFunctor(), vars);
            l.addAnnot(src);

            removePerceptsByUnif(l);
        });

        return true;
    }

    private boolean put(String resourceURI, Collection<Literal> representation, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PUT);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representation);

        return runOperation(op);
    }

    private boolean post(String resourceURI, Collection<Literal> representationPart, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.POST);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representationPart);

        return runOperation(op);
    }

    private boolean patch(String resourceURI, Collection<Literal> representationDiff, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.PATCH);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        op.setPayload(representationDiff);

        return runOperation(op);
    }

    private boolean delete(String resourceURI, Map<String, Object> formFields) {
        formFields.put(Operation.METHOD_NAME_FIELD, Operation.DELETE);
        Operation op = ProtocolBindings.bind(resourceURI, formFields);

        return runOperation(op);
    }

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
                    forget(op.getTargetURI(), new HashMap<>());
                    res.getPayload().forEach(this::addPercept);
                }
            }

            return true;
        } catch (IOException e) {
            getLogger().warning(e.getLocalizedMessage());
            return false;
        }
    }

}
