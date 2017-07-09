package main.java.com.djrapitops.plan.data.cache;

import main.java.com.djrapitops.plan.Plan;
import main.java.com.djrapitops.plan.data.AnalysisData;
import main.java.com.djrapitops.plan.utilities.analysis.Analysis;

/**
 * This class is used to store the most recent AnalysisData object and to run
 * Analysis.
 *
 * @author Rsl1122
 * @since 2.0.0
 */
public class AnalysisCacheHandler {

    private final Plan plugin;
    private AnalysisData cache;
    private final Analysis analysis;
    private boolean analysisEnabled;

    /**
     * Class Constructor.
     *
     * Initializes Analysis
     *
     * @param plugin Current instance of Plan
     */
    public AnalysisCacheHandler(Plan plugin) {
        this.plugin = plugin;
        analysis = new Analysis(plugin);
        analysisEnabled = true;
    }

    /**
     * Runs analysis, cache method is called after analysis is complete.
     */
    public void updateCache() {
        analysis.runAnalysis(this);
    }

    /**
     * Saves the new analysis data to cache.
     *
     * @param data AnalysisData generated by Analysis.analyze
     */
    public void cache(AnalysisData data) {
        cache = data;
    }

    /**
     * Returns the cached AnalysisData.
     *
     * @return null if not cached
     */
    public AnalysisData getData() {
        return cache;
    }

    /**
     * Check if the AnalysisData has been cached.
     *
     * @return true if there is data in the cache.
     */
    public boolean isCached() {
        return (cache != null);
    }

    /**
     *
     * @return
     */
    public boolean isAnalysisBeingRun() {
        return analysis.isAnalysisBeingRun();
    }

    public boolean isAnalysisEnabled() {
        return analysisEnabled;
    }

    public void disableAnalysisTemporarily() {
        analysisEnabled = false;
        analysis.setTaskId(-2);
    }

    public void enableAnalysis() {
        analysis.setTaskId(-1);
        analysisEnabled = true;
    }
}
