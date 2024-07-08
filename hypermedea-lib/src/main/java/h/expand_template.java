package h;

import jason.asSemantics.DefaultInternalAction;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hypermedea.tools.Identifiers;
import org.hypermedea.tools.KVPairs;
import org.hypermedea.tools.URITemplate;

import java.util.HashMap;
import java.util.Map;

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

        if (!tplTerm.isString() || !mappingTerm.isList() || !uriTerm.isVar()) {
            String signature = "(" + tplTerm + ", " + mappingTerm + ", " + uriTerm + ")";
            String msg = target.class.getName() + " requires (string, list, var) input, got " + signature;
            throw new IllegalArgumentException(msg);
        }

        // TODO if uriTerm not var: check equality

        URITemplate tpl = new URITemplate(Identifiers.getLexicalForm(tplTerm));

        Map<String, Object> mapping = new HashMap<>();
        for (Map.Entry<String,Term> kv : KVPairs.getAsMap((ListTerm) mappingTerm).entrySet()) {
            mapping.put(kv.getKey(), Identifiers.getLexicalForm(kv.getValue()));
        }

        String uri = tpl.createUri(mapping);
        return un.bind((VarTerm) uriTerm, ASSyntax.createString(uri));
    }

}
