package org.hypermedea;

import cartago.OPERATION;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.LogicalFormula;
import jason.asSyntax.parser.ParseException;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.ResultSet;
import org.apache.jena.sparql.engine.http.QueryEngineHTTP;
import org.hypermedea.sparql.TermSPARQLQueryWrapper;

/**
 * <p>
 *     Artifact to interact with SPARQL query endpoints.
 * </p>
 *
 * <p>
 *     The <code>QueryArtifact</code> uses the naming strategy managed by the {@link OntologyArtifact}
 *     to turn predicate names used in Jason into full URIs.
 * </p>
 *
 * <p>
 *     TODO SPARUL interface?
 * </p>
 *
 * @author Victor Charpenay
 */
public class QueryArtifact extends HypermedeaArtifact {

    private String endpointURI;

    public void init(String endpointURI) {
        this.endpointURI = endpointURI;
    }

    @OPERATION
    public void submitQuery(String query) {
        try {
            LogicalFormula queryTerm = ASSyntax.parseFormula(query);
            Query selectQuery = new TermSPARQLQueryWrapper(queryTerm).getSPARQLQuery();

            QueryEngineHTTP exec = QueryExecutionFactory.createServiceRequest(endpointURI, selectQuery);
            ResultSet mappings = exec.execSelect();

            // TODO unify query variables with results
        } catch (ParseException e) {
            e.printStackTrace();
            failed("Invalid query");
        }
    }

}
