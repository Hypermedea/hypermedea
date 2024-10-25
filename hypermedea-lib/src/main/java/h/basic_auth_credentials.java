package h;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hypermedea.op.http.HTTP;
import org.hypermedea.tools.Identifiers;

/**
 * <p>
 *   <code>basic_auth_credentials(Username, Password, Header)</code> is true if <code>Header</code> is the proper
 *   <code>Authorization</code> header value for <code>Username</code> / <code>Password</code>, as per
 *   <a href="https://en.wikipedia.org/wiki/Basic_access_authentication">HTTP basic authentication</a>.
 * </p>
 */
public class basic_auth_credentials extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length != 3) {
            String msg = basic_auth_credentials.class.getName() + " expects 2 arguments, " + args.length + " received";
            throw new IllegalArgumentException(msg);
        }

        Term usernameTerm = args[0];
        Term passwordTerm = args[1];
        Term headerTerm = args[2];

        if (!usernameTerm.isString()) {
            String msg = basic_auth_credentials.class.getName() + "'s first argument (username) must be a string";
            throw new IllegalArgumentException(msg);
        }

        if (!passwordTerm.isString()) {
            String msg = basic_auth_credentials.class.getName() + "'s second argument (password) must be a string";
            throw new IllegalArgumentException(msg);
        }

        if (!isStringOrVar(headerTerm)) {
            String msg = basic_auth_credentials.class.getName() + "'s third argument (header) must be a string or var";
            throw new IllegalArgumentException(msg);
        }

        String username = Identifiers.getLexicalForm(usernameTerm);
        String password = Identifiers.getLexicalForm(passwordTerm);
        String h = HTTP.getBasicAuthField(username, password);

        if (headerTerm.isVar()) {
            return un.bind((VarTerm) headerTerm, ASSyntax.createString(h));
        } else {
            return h.equals(Identifiers.getLexicalForm(headerTerm));
        }
    }

    private boolean isStringOrVar(Term uri) {
        return uri.isVar() || uri.isString();
    }

}
