package h;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hypermedea.tools.Identifiers;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>
 *   <code>target(URI, TargetURI)</code> is true if <code>TargetURI</code> has no fragment
 *   and is equal to <code>URI</code> (except for the fragment).
 * </p>
 * <p>
 *   The <code>target</code> internal action is useful to anticipate what URI is set as target of an HTTP request.
 * </p>
 */
public class target extends DefaultInternalAction {

    /**
     * TODO in case of error, error message shown in agent console is empty. Improve?
     *
     * @param ts
     * @param un
     * @param args
     * @return
     * @throws Exception
     */
    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length != 2) {
            String msg = target.class.getName() + " expects 2 arguments, " + args.length + " received";
            throw new IllegalArgumentException(msg);
        }

        Term uriTerm = args[0];
        Term targetTerm = args[1];

        if (bothVarUnbound(uriTerm, targetTerm)) {
            String msg = target.class.getName() + " must have at least one argument bound";
            throw new IllegalArgumentException(msg);
        }

        if (!isStringOrVar(uriTerm) || !isStringOrVar(targetTerm)) {
            String signature = "(" + uriTerm + ", " + targetTerm + ")";
            String msg = target.class.getName() + " requires (string, string) input, got " + signature;
            throw new IllegalArgumentException(msg);
        }

        if (uriTerm.isVar()) {
            try {
                URI target = new URI(Identifiers.getLexicalForm(targetTerm));

                if (!target.getFragment().isEmpty()) return false;

                un.bind((VarTerm) uriTerm, ASSyntax.createString(target));
                return true;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        } else if (targetTerm.isVar()) {
            try {
                URI uri = new URI(Identifiers.getLexicalForm(uriTerm));
                URI target = getTarget(uri);

                un.bind((VarTerm) targetTerm, ASSyntax.createString(target));
                return true;
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        } else {
            try {
                URI uri = new URI(Identifiers.getLexicalForm(uriTerm));
                URI target = getTarget(uri);
                return target.equals(getTarget(uri));
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    private boolean bothVarUnbound(Term uri, Term target) {
        return uri.isVar() && target.isVar();
    }

    private boolean isStringOrVar(Term uri) {
        return uri.isVar() || uri.isString();
    }

    /**
     * Remove the fragment of the input URI.
     *
     * @param uri any URI
     * @return the same URI, without fragment
     * @throws URISyntaxException
     */
    private URI getTarget(URI uri) throws URISyntaxException {
        return new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), null);
    }

}
