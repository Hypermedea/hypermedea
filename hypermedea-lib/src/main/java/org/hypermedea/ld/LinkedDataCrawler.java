package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Crawler that fetches RDF resource representations in a multi-threaded fashion. Threads:
 * <ul>
 *     <li>main thread dispatching requests to the thread pool</li>
 *     <li>one thread per request in the thread pool</li>
 * </ul>
 *
 * The <code>LinkedDataCrawler</code> class is a singleton.
 * Its unique instance is accessible via <code>getInstance()</code>.
 *
 * @author Victor Charpenay
 */
public class LinkedDataCrawler {

    private class RequestTask implements Runnable {

        private final String resourceURI;

        public RequestTask(String resourceURI) {
            this.resourceURI = resourceURI;
        }

        @Override
        public void run() {
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
                            Resource res = resourceQueue.remove();
                            resourceURIQueue.remove(res.getURI());
                            // TODO execute callbacks in separate threads
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

    /**
     * TODO check env variable at instantiation time
     */
    public final static int THREAD_POOL_SIZE = 8;

    private static LinkedDataCrawler instance;

    private final Thread routine = new Thread(new CrawlerRoutine());

    // TODO or newCachedThreadPool? To have variable size pool
    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private final Queue<Resource> resourceQueue = new ConcurrentLinkedQueue<>();

    private final Queue<String> resourceURIQueue = new ConcurrentLinkedQueue<>();

    private Set<RequestListener> listeners = new HashSet<>();

    public static LinkedDataCrawler getInstance() {
        if (instance == null) instance = new LinkedDataCrawler();
        return instance;
    }

    private LinkedDataCrawler() {
        routine.start();
    }

    public void addListener(RequestListener listener) {
        this.listeners.add(listener);
    }

    public void get(String resourceURI) throws IOException, URISyntaxException {
        if (!resourceURIQueue.contains(resourceURI)) {
            RequestTask t = new RequestTask(resourceURI);
            pool.submit(t);

            resourceURIQueue.add(resourceURI);
        }
    }

    public boolean isActive() {
        return resourceURIQueue.size() > 0;
    }

}
