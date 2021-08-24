package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;

import java.io.IOException;
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
 */
public class LinkedDataCrawler {

    private class RequestTask implements Runnable {

        private final String resourceURI;

        public RequestTask(String resourceURI) {
            this.resourceURI = resourceURI;
        }

        @Override
        public void run() {
            // TODO or use RDFParser.source(resourceURI).parse(StreamRDF);
            Model g = RDFDataMgr.loadModel(resourceURI);

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
                            for (RequestListener l : listeners) l.requestComplete(res);
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

    private final Thread main = new Thread(new CrawlerRoutine());

    // TODO or newCachedThreadPool? To have variable size pool
    private final ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    private final Queue<Resource> resourceQueue = new ConcurrentLinkedQueue<>();

    private Set<RequestListener> listeners = new HashSet<>();

    public LinkedDataCrawler() {
        main.start();
    }

    public void addListener(RequestListener listener) {
        this.listeners.add(listener);
    }

    public void get(String resourceURI) throws IOException {
        pool.submit(new RequestTask(resourceURI));
    }

}
