package org.hypermedea;

import cartago.OPERATION;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.Pred;
import jason.asSyntax.parser.ParseException;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.client.methods.*;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Triple;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFLanguages;
import org.apache.jena.sparql.graph.GraphFactory;
import org.hypermedea.sparql.TermTriplePatternWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * <p>
 *     Artifact to manage resources with HTTP CR(U)D operations, as e.g. specified in the
 *     <a href="https://www.w3.org/TR/ldp/">Linked Data Platform (LDP)</a> and
 *     <a href="http://www.w3.org/TR/sparql11-http-rdf-update/">SPARQL Graph Store</a> standards.
 * </p>
 * <p>
 *     Resource representations are assumed to be lists of RDF triples (see {@link NavigationArtifact}
 *     and {@link OntologyArtifact} for representations of triples in Jason). The artifact is aware
 *     of LDP specificities such as pagination.
 * </p>
 */
public class ResourceManagementArtifact extends HypermedeaArtifact {

    public static final String DEFAULT_CONTENT_TYPE = "text/turtle";

    private final CloseableHttpClient client = HttpClients.createDefault();

    public void init() {
        // do nothing
    }

    @OPERATION
    public void post(String anchorResourceURI, List<String> representation) {
        Model m = parseRepresentationOrFail(representation);

        HttpPost req = new HttpPost(anchorResourceURI);
        setPayloadOrFail(req, m);

        sendRequestOrFail(req);
    }

    @OPERATION
    public void put(String resourceURI, List<String> representation) {
        Model m = parseRepresentationOrFail(representation);

        HttpPut req = new HttpPut(resourceURI);
        setPayloadOrFail(req, m);

        sendRequestOrFail(req);
    }

    @OPERATION
    public void delete(String resourceURI) {
        sendRequestOrFail(new HttpDelete(resourceURI));
    }


    private Model parseRepresentationOrFail(List<String> representation) {
        Graph g = GraphFactory.createDefaultGraph();

        for (String termString : representation) {
            boolean failed = false;

            try {
                LogicalFormula pred = ASSyntax.parseFormula(termString);
                if (!pred.isPred()) failed = true;

                Triple t = new TermTriplePatternWrapper((Pred) pred).getTriplePattern();
                if (!t.isConcrete()) failed = true;

                g.add(t);
            } catch (ParseException e) {
                failed = true;
            }

            if (failed) {
                failed("The resource's representation must only include ground predicates");
            }
        }

        return ModelFactory.createModelForGraph(g);
    }

    private void setPayloadOrFail(HttpEntityEnclosingRequest request, Model payload) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        payload.write(stream, RDFLanguages.contentTypeToLang(DEFAULT_CONTENT_TYPE).getName());

        ByteArrayEntity body = new ByteArrayEntity(stream.toByteArray());
        body.setContentEncoding("UTF-8");
        body.setContentType(DEFAULT_CONTENT_TYPE);

        request.setEntity(body);
    }

    private void sendRequestOrFail(HttpUriRequest request) {
        try {
            CloseableHttpResponse res = client.execute(request);

            int status = res.getStatusLine().getStatusCode();
            if (status >= 400) failed("Server responded with error code: " + status);

            // TODO return response/links?
        } catch (IOException e) {
            failed("IO error occurred during HTTP operation");
        }
    }

}
