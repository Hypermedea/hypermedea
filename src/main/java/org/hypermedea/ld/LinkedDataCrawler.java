package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Crawler that fetches RDF resource representations in a multi-threaded fashion. Threads:
 * <ul>
 *     <li>main thread dispatching requests to the thread pool</li>
 *     <li>one thread per request in the thread pool</li>
 * </ul>
 */
public class LinkedDataCrawler {

    private class RequestTask implements Runnable {

        private final String resourceURI;

        public RequestTask(String resourceURI) {
            this.resourceURI = resourceURI;
        }

        @Override
        public void run() {
            nbActiveRequests++;

            Model g;
            try {
                // TODO or use RDFParser.source(resourceURI).parse(StreamRDF);
                // TODO check whether HTTP caching is implemented
                g = RDFDataMgr.loadModel(resourceURI);
            } catch (Exception e) {
                // TODO log error
                g = null;
            }

            synchronized (resourceQueue) {
                resourceQueue.add(new Resource(resourceURI, g));
                resourceQueue.notify();
            }
        }

    }

    private class CrawlerRoutine implements Runnable {

        @Override
        public void run() {
            synchronized (resourceQueue) {
                while (true) {
                    try {
                        resourceQueue.wait();

                        while (!resourceQueue.isEmpty()) {
                            nbActiveRequests--;

                            Resource res = resourceQueue.remove();

                            // TODO duplicated with artifact's list of obsProperties
                            // requestedRepresentations.remove(res.getURI());
                            for (RequestListener l : listeners) l.requestCompleted(res);
                        }
                    } catch (InterruptedException e) {
                        // TODO do nothing?
                        e.printStackTrace();
                    }
                }
            }
        }

    }

    public final static int THREAD_POOL_SIZE = 8;

    private final Thread routine = new Thread(new CrawlerRoutine());

    // TODO or newCachedThreadPool? To have variable size pool
    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private final Queue<Resource> resourceQueue = new ConcurrentLinkedQueue<>();

    private final Set<String> requestedRepresentations = new HashSet<>();

    private int nbActiveRequests = 0;

    private Set<RequestListener> listeners = new HashSet<>();

    public LinkedDataCrawler() {
        routine.start();
    }

    public void addListener(RequestListener listener) {
        this.listeners.add(listener);
    }

    public void get(String resourceURI) throws IOException, URISyntaxException {
        String requestedURI = withoutFragment(resourceURI);

        if (!requestedRepresentations.contains(requestedURI)) {
            RequestTask t = new RequestTask(requestedURI);
            pool.submit(t);

            requestedRepresentations.add(requestedURI);
        }
    }

    public boolean isActive() {
        return nbActiveRequests > 0;
    }

    public static String withoutFragment(String resourceURI) throws URISyntaxException {
        URI parsedURI = new URI(resourceURI);
        String fragment = "#" + parsedURI.getFragment();
        return resourceURI.replace(fragment, "");
    }

}
