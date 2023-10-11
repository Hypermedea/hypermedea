package org.hypermedea.op.file;

import jason.asSyntax.ASSyntax;
import jason.asSyntax.Literal;
import org.hypermedea.ct.json.JsonHandler;
import org.hypermedea.ct.txt.PlainTextHandler;
import org.hypermedea.op.Response;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class FileOperationTest {

    @Test
    public void testReadWriteDelete() throws IOException {
        String filename = UUID.randomUUID() + ".txt";
        String uri = new File(filename).toURI().toString();

        FileOperation w = new WriteFileOperation(uri, new HashMap<>(), false);
        FileOperation r = new ReadFileOperation(uri, new HashMap<>());
        FileOperation d = new DeleteFileOperation(uri, new HashMap<>());

        String txt = "This file is identified by " + uri;
        Literal cnt = ASSyntax.createStructure(PlainTextHandler.TXT_FUNCTOR, ASSyntax.createString(txt));

        w.setPayload(cnt);
        w.sendRequest();

        assert w.getResponse().getStatus().equals(Response.ResponseStatus.OK);

        r.sendRequest();
        Literal actualCnt = r.getResponse().getPayload().stream().findAny().get();

        assert actualCnt.equals(cnt);

        d.sendRequest();

        assert d.getResponse().getStatus().equals(Response.ResponseStatus.OK);

        new File(filename).delete();
    }

    @Test
    public void testAppend() throws IOException {
        String filename = UUID.randomUUID() + ".txt";
        String uri = new File(filename).toURI().toString();

        FileOperation w = new WriteFileOperation(uri, new HashMap<>(), true);
        FileOperation r1 = new ReadFileOperation(uri, new HashMap<>());
        FileOperation a = new WriteFileOperation(uri, new HashMap<>(), true);
        FileOperation r2 = new ReadFileOperation(uri, new HashMap<>());

        String txt = "This file is identified by ";
        Literal cnt1 = ASSyntax.createStructure(PlainTextHandler.TXT_FUNCTOR, ASSyntax.createString(txt));

        w.setPayload(cnt1);
        w.sendRequest();

        r1.sendRequest();
        Literal actualCnt1 = r1.getResponse().getPayload().stream().findAny().get();

        assert actualCnt1.equals(cnt1);

        Literal cnt2 = ASSyntax.createStructure(PlainTextHandler.TXT_FUNCTOR, ASSyntax.createString(uri));

        a.setPayload(cnt2);
        a.sendRequest();

        r2.sendRequest();
        Literal actualCntAll = r2.getResponse().getPayload().stream().findAny().get();

        String txtAll = "This file is identified by " + uri;
        Literal cntAll = ASSyntax.createStructure(PlainTextHandler.TXT_FUNCTOR, ASSyntax.createString(txtAll));

        assert actualCntAll.equals(cntAll);

        new File(filename).delete();
    }

    @Test
    public void testKnownContentType() throws IOException {
        String filename = UUID.randomUUID() + ".json";
        String uri = new File(filename).toURI().toString();

        FileOperation w = new WriteFileOperation(uri, new HashMap<>(), false);
        FileOperation r = new ReadFileOperation(uri, new HashMap<>());

        Literal cnt = ASSyntax.createStructure(JsonHandler.JSON_FUNCTOR, ASSyntax.createNumber(12));

        w.setPayload(cnt);
        w.sendRequest();

        assert w.getResponse().getStatus().equals(Response.ResponseStatus.OK);
        
        r.sendRequest();
        Literal actualCnt = r.getResponse().getPayload().stream().findAny().get();

        assert actualCnt.equals(cnt);

        new File(filename).delete();
    }

}
