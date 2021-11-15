package org.hypermedea;

import cartago.Artifact;
import cartago.OPERATION;
import cartago.ObsProperty;
import cartago.OpFeedbackParam;
import jason.asSyntax.ASSyntax;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RDFTripleWrapper;
import org.hypermedea.ld.RequestListener;
import org.hypermedea.ld.Resource;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * <p>
 *   Artifact for browsing Linked Data.
 * </p>
 *
 * <p>
 *   The main operation of the <code>ThingArtifact</code> is {@link #get(String) get}, to look a Web resource up.
 *   The resource may be identified by an <code>http</code>, <code>https</code> or <code>file</code> URI. If a
 *   relative URI is given as argument, it is assumed to be a <code>file</code> URI relative to the JaCaMo
 *   project's directory.
 * </p>
 *
 * <p>
 *   The <code>LinkedDataArtifact</code> performs lookups in an asynchronous fashion, that is: a
 *   {@link #get(String) get} operation returns before the RDF representation of the resource is fully downloaded.
 *   Several observable properties are exposed by the <code>LinkedDataArtifact</code> to manage pending lookups:
 * </p>
 * <ul>
 *     <li><code>to_visit(URI)</code> is added after a call to {@link #get(String) get(URI)} and removed after the lookup has ended.</li>
 *     <li><code>visited(URI)</code> is added once a lookup has ended.</li>
 *     <li><code>crawler_status(idling|crawling|error)</code> is updated depending on whether lookups are pending or not.</li>
 * </ul>
 *
 * <p>
 *   Once a resource is visited, the RDF statements found under its URI are added as observable properties of the
 *   <code>LinkedDataArtifact</code>. RDF statements have the form
 * </p>
 * <pre>rdf(S, P, O)[ crawler_source(URI) ] .</pre>
 * <p>
 *   where <code>S</code>, <code>P</code> and <code>O</code> are the subject, predicate and object of
 *   the RDF statement. The <code>crawler_source</code> annotation (within square brackets) holds the URI of the
 *   source for that statement, i.e. the URI given as argument of {@link #get(String) get(URI)}. A further annotation
 *   (<code>rdf_type_map</code>) gives the node type of each element of the triple (see {@link RDFTripleWrapper}
 *   for more details).
 * </p>
 *
 * <p>
 *     See <a href="https://github.com/Hypermedea/hypermedea/tree/master/examples/fayol"><code>examples/fayol</code></a>
 *     for an example with Linked Data browsing.
 * </p>
 *
 * @author Victor Charpenay, Noé Saffaf
 */
public class LinkedDataArtifact extends Artifact {

	/**
	 * Manager that listens to incoming resources from the Linked Data crawler
	 * and adds corresponding observable properties.
	 */
	private class RDFObsPropertyManager implements RequestListener {

		@Override
		public void requestCompleted(Resource res) {
			if (visited(res.getURI())) {
				// TODO warn: this shouldn't have happened
				return;
			}

			beginExternalSession();

			if (res.getRepresentation() != null) {
				res.getRepresentation().listStatements().forEachRemaining(st -> {
					RDFTripleWrapper w = new RDFTripleWrapper(st);
					String name = w.getPropertyName();
					Object[] args = w.getPropertyArguments();

					ObsProperty prop = defineObsProperty(name, args[0], args[1], args[2]);

					StringTerm origin = ASSyntax.createString(res.getURI());
					prop.addAnnot(ASSyntax.createStructure(SOURCE_FUNCTOR, origin));

					for (Term t : w.getPropertyAnnotations()) prop.addAnnot(t);
				});
			}

			removeObsPropertyByTemplate(TO_VISIT_FUNCTOR, res.getURI());
			defineObsProperty(VISITED_FUNCTOR, res.getURI());

			updateCrawlerStatus();

			endExternalSession(true);
		}

	}

	public static final String CRAWLER_STATUS_FUNCTOR = "crawler_status";

	public static final String SOURCE_FUNCTOR = "crawler_source";

	public static final String VISITED_FUNCTOR = "visited";

	public static final String TO_VISIT_FUNCTOR = "to_visit";

	private LinkedDataCrawler crawler;

	private ObsProperty crawlerStatus;

	/**
	 * Initialize the Linked Data crawler.
	 */
	public void init() {
		crawler = LinkedDataCrawler.getInstance();
		crawler.addListener(new RDFObsPropertyManager());

		crawlerStatus = defineObsProperty(CRAWLER_STATUS_FUNCTOR, false);
	}

	/**
	 * <p>
	 *   Expose the transformation function from a resource URI to its parent resource URI (without fragment, if any).
	 *   For instance, the parent resource of
	 * </p>
	 * <pre>http://example.org/alice#me</pre>
	 * <p>is</p>
	 * <pre>http://example.org/alice</pre>
	 *
	 * @param resourceURI a resource URI
	 * @param parentResourceURI the URI of the parent resource
	 */
	@OPERATION
	public void getParentURI(String resourceURI, OpFeedbackParam<String> parentResourceURI) {
		try {
			parentResourceURI.set(withoutFragment(resourceURI));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			failed(e.getReason());
		}
	}

	/**
	 * Perform a GET request to retrieve an RDF representation of the provided resource.
	 * Add (asynchronously) the found RDF representation to the belief base.
	 */
	@OPERATION
	public void get(String resourceURI) {
		try {
			// force crawler status to true
			updateCrawlerStatus(true);

			String requestedURI = withoutFragment(resourceURI);

			if (!visited(requestedURI) && !toVisit(requestedURI)) {
				defineObsProperty(TO_VISIT_FUNCTOR, requestedURI);
				crawler.get(requestedURI);
			} else {
				// TODO necessary?
				updateCrawlerStatus();
			}
		} catch (IOException | URISyntaxException e) {
			e.printStackTrace();
			// TODO improve logging
			failed(e.getMessage());
		}
	}

	private boolean visited(String originURI) {
		return hasObsPropertyByTemplate(VISITED_FUNCTOR, originURI);
	}

	private boolean toVisit(String originURI) {
		return hasObsPropertyByTemplate(TO_VISIT_FUNCTOR, originURI);
	}

	private void updateCrawlerStatus() {
		Boolean isActive = crawler.isActive();

		Boolean hasToVisit;
		try {
			// TODO try/catch shouldn't be here, fix bug upstream in CArtAgO
			hasToVisit = hasObsProperty(TO_VISIT_FUNCTOR);
		} catch (IndexOutOfBoundsException e) {
			hasToVisit = false;
		}

		updateCrawlerStatus(isActive || hasToVisit);
	}

	private void updateCrawlerStatus(Boolean status) {
		if (!crawlerStatus.getValue().equals(status)) crawlerStatus.updateValue(status);
	}

	private static String withoutFragment(String resourceURI) throws URISyntaxException {
		URI parsedURI = new URI(resourceURI);
		String fragment = "#" + parsedURI.getFragment();
		return resourceURI.replace(fragment, "");
	}

}
