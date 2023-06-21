package org.hypermedea;

import org.junit.Test;

public class QueryArtifactTest {

    public static final String ENDPOINT_URL = "https://query.wikidata.org/sparql";

    public static final String QUERY_TERM = "rdf(S, P, O) & rdf(S, Q, O)";

    @Test
    public void testSubmitQuery() {
        QueryArtifact art = new QueryArtifact();
        art.init(ENDPOINT_URL);

        art.submitQuery(QUERY_TERM);
    }

}
