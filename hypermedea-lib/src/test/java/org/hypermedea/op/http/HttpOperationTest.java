package org.hypermedea.op.http;

import jason.asSyntax.*;
import org.hypermedea.op.Operation;
import org.hypermedea.op.Response;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Optional;

public class HttpOperationTest {

    public static final String SOURCE_RDF_URI = "https://territoire.emse.fr/kg/emse/fayol/4ET";

    public static final String SOURCE_JSON_URI = "https://httpbin.org/get";

    public static final String TARGET_JSON_URI = "https://httpbin.org/put";

    @Test
    public void testGetRDF() throws IOException {
        HashMap<String, Object> f = new HashMap<>();
        f.put(Operation.METHOD_NAME_FIELD, Operation.GET);

        HttpOperation op = new HttpOperation(SOURCE_RDF_URI, f);

        op.sendRequest();
        Response res = op.getResponse();

        assert res.getStatus().equals(Response.ResponseStatus.OK);

        assert res.getPayload().size() == 45;
    }

    @Test
    public void testGetJson() throws IOException {
        HashMap<String, Object> f = new HashMap<>();
        f.put(Operation.METHOD_NAME_FIELD, Operation.GET);

        HttpOperation op = new HttpOperation(SOURCE_JSON_URI, f);

        op.sendRequest();
        Response res = op.getResponse();

        assert res.getStatus().equals(Response.ResponseStatus.OK);

        Optional<Literal> termOpt = res.getPayload().stream().findFirst();

        assert termOpt.isPresent();

        ListTerm pairs = (ListTerm) termOpt.get().getTerm(0);
        Atom k = ASSyntax.createAtom("url");
        Optional<Term> url = pairs.getAsList().stream().filter(kv -> ((Structure) kv).getTerm(0).equals(k)).findAny();

        assert url.isPresent();

        assert ((Structure) url.get()).getTerm(1).equals(ASSyntax.createString(SOURCE_JSON_URI));
    }

    @Test
    public void testPutJson() throws IOException {
        HashMap<String, Object> f = new HashMap<>();
        f.put(Operation.METHOD_NAME_FIELD, Operation.PUT);

        HttpOperation op = new HttpOperation(TARGET_JSON_URI, f);

        op.sendRequest();
        Response res = op.getResponse();

        assert res.getStatus().equals(Response.ResponseStatus.OK);
    }

}
