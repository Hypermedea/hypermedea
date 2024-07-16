package org.hypermedea.op.http;

import org.apache.http.HttpHeaders;
import org.apache.jena.ext.xerces.impl.dv.util.Base64;

import java.nio.charset.StandardCharsets;

public class HTTP {

    public static final String NS = "urn:hypermedea:http:";

    public static final String Accept = NS + "accept";

    public static final String Authorization = NS + "authorization";

    public static final String ContentType = NS + "contentType";

    public static final String Cookie = NS + "cookie";

    public static String getHeader(String term) {
        switch (term) {
            case Accept: return HttpHeaders.ACCEPT;
            case Authorization: return HttpHeaders.AUTHORIZATION;
            case ContentType: return HttpHeaders.CONTENT_TYPE;
            case Cookie: return "Cookie";
            default: return null;
        }
    }

    public static String getBasicAuthField(String username, String password) {
        return "Basic " + Base64.encode(String.format("%s:%s", username, password).getBytes(StandardCharsets.UTF_8));
    }

    private HTTP() {}

}
