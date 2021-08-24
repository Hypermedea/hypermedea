package org.hypermedea.ld;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.junit.Test;

import java.io.IOException;
import java.util.Collection;

public class LinkedDataCrawlerTest {

    private static final long SLEEP_DURATION= 5000;

    private static final String SOURCE_URI = "http://ontology.irstea.fr/weather/data/resource/platform/VP2lesPalaquins01";

    private static final String SOSA_HOSTS = "http://www.w3.org/ns/sosa/hosts";

    private class ResourceCollector implements RequestListener {

        private Collection<Resource> resources = new ConcurrentHashSet<>();

        public Collection<Resource> getResources() {
            return resources;
        }

        @Override
        public void requestComplete(Resource res) {
            resources.add(res);
        }

    }


    @Test
    public void testSingleRequest() throws IOException, InterruptedException {
        LinkedDataCrawler c = new LinkedDataCrawler();
        ResourceCollector collector = new ResourceCollector();

        c.addListener(collector);

        c.get(SOURCE_URI);

        Thread.sleep(SLEEP_DURATION);

        assert collector.getResources().size() == 1;

        Resource res = collector.getResources().iterator().next();

        assert res.getRepresentation().size() == 17;
    }

    @Test
    public void testRequestChain() throws IOException, InterruptedException {
        LinkedDataCrawler c = new LinkedDataCrawler();
        ResourceCollector collector = new ResourceCollector();

        c.addListener(collector);
        c.addListener(res -> {
            if (res.getURI().equals(SOURCE_URI)) {
                Model m = res.getRepresentation();
                Property p = m.createProperty(SOSA_HOSTS);

                m.listObjectsOfProperty(p).forEach(o -> {
                    try {
                        c.get(o.asResource().getURI());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            }
        });

        c.get(SOURCE_URI);

        Thread.sleep(SLEEP_DURATION);

        assert collector.getResources().size() == 8;
    }

}
