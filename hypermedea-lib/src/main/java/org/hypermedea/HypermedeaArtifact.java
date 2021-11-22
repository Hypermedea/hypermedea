package org.hypermedea;

import cartago.Artifact;
import org.hypermedea.ld.LinkedDataCrawler;
import org.hypermedea.ld.RequestListener;

/**
 * Abstract class for all Hypermedea artifacts, holding a reference to objects with restricted access,
 * e.g. a Linked Data crawler.
 */
public abstract class HypermedeaArtifact extends Artifact {

    /**
     * Singleton Linked Data crawler to which all artifacts can attach a listener.
     */
    protected static LinkedDataCrawler crawler = new LinkedDataCrawler();

    /**
     * Request listener that is automatically attached to the <code>HypermedeaArtifact</code> crawler instance
     * (and detached once disposed of).
     *
     * TODO add CArtAgO session management?
     */
    protected RequestListener crawlerListener = null;

    protected void init() {
        crawler.addListener(crawlerListener);
    }

    @Override
    protected void dispose() {
        if (crawlerListener != null) crawler.removeListener(crawlerListener);
        super.dispose();
    }

}
