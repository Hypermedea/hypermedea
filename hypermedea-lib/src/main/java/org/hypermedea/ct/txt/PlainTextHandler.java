package org.hypermedea.ct.txt;

import jason.asSyntax.*;
import org.hypermedea.ct.BaseRepresentationHandler;
import org.hypermedea.ct.UnsupportedRepresentationException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

public class PlainTextHandler extends BaseRepresentationHandler {

    public static final String TXT_FUNCTOR = "text";

    public static final String TXT_CT = "text/plain";

    public PlainTextHandler() {
        super(TXT_FUNCTOR, TXT_CT);
    }

    @Override
    public void serialize(Collection<Literal> terms, OutputStream out, String resourceURI) throws UnsupportedRepresentationException, IOException {
        for (Literal t : terms) {
            if (t.getFunctor().equals(TXT_FUNCTOR) && t.getArity() == 1) {
                Term txt = t.getTerm(0);

                if (txt.isString()) out.write(((StringTerm) txt).getString().getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    @Override
    public Collection<Literal> deserialize(InputStream representation, String resourceURI, String contentType) throws UnsupportedRepresentationException {
        try {
            // TODO get charset from Content-Type
            String txt = new String(new BufferedInputStream(representation).readAllBytes(), StandardCharsets.UTF_8);

            Literal t = ASSyntax.createLiteral(functor, ASSyntax.createString(txt));
            return Arrays.asList(t);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
