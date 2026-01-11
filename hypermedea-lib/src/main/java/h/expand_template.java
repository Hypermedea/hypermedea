package h;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.MapTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hypermedea.tools.Identifiers;
import org.hypermedea.tools.URITemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * <code>expand_template(Tpl, {Var1 -> Val1, Var2 -> Val2, ...}, URI)</code> is true if <code>URI</code> is the
 * result of expanding <code>Tpl</code>, a <a href="https://datatracker.ietf.org/doc/html/rfc6570">URI template</a>,
 * with the mapping provided in the second argument. This mapping should be a list of key-value pairs, each key and
 * value being interpreted as a string. For instance, after executing the following action:
 * <pre><code>expand_template("http://example.org/{id}{?ts,user}", {id -> res123, user -> alice}, URI)</code></pre>
 * <code>URI</code> unifies with <code>"http://example.org/res123?user=alice"</code> (and the missing variable mapping
 * for <code>ts</code> is ignored).
 */
public class expand_template extends DefaultInternalAction {

    @Override
    public Object execute(TransitionSystem ts, Unifier un, Term[] args) throws Exception {
        if (args.length != 3) {
            String msg = expand_template.class.getName() + " expects 3 arguments, " + args.length + " received";
            throw new IllegalArgumentException(msg);
        }

        Term tplTerm = args[0];
        Term mappingTerm = args[1];
        Term uriTerm = args[2];

        if (!tplTerm.isString() || !mappingTerm.isMap() || !uriTerm.isVar()) {
            String signature = "(" + tplTerm + ", " + mappingTerm + ", " + uriTerm + ")";
            String msg = expand_template.class.getName() + " requires (string, map, var) input, got " + signature;
            throw new IllegalArgumentException(msg);
        }

        // TODO if uriTerm not var: check equality
        // TODO manage multiple variable assignments (to same var)

        URITemplate tpl = new URITemplate(Identifiers.getLexicalForm(tplTerm));

        Map<String, Object> mapping = new HashMap<>();

        for (Term k : ((MapTerm) mappingTerm).keys()) {
            String key = Identifiers.getLexicalForm(k);
            String value = Identifiers.getLexicalForm(((MapTerm) mappingTerm).get(k));
            mapping.put(key, value);
        }

        String uri = tpl.createUri(mapping);
        return un.bind((VarTerm) uriTerm, ASSyntax.createString(uri));
    }

}
