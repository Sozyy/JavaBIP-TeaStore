package tools.spirals.cerberus237.siphonix.strategies.javabip.controller;

import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.core.RestMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheHitMissMetrics;

/**
 * Collects the real, measured mean hit/miss response times of the AdaptableTeaStore
 * image service cache, via the official endpoint GET /rest/metrics/cache-performance.
 *
 * Confirmed endpoint + model (CacheHitMissMetrics: meanHitTime, meanMissTime) by
 * inspecting Arléon's CacheSizeAdaptationObservation, which wires this endpoint for
 * AdaptiFlow but does not expose it to JavaBIP. This is the metric the PID controller
 * actually needs as its process variable: it was previously approximated with fixed,
 * arbitrary constants (hitWeight=0.2, missWeight=2.0) in DataProvider, instead of the
 * real values measured by the TeaStore itself.
 */
public class CacheHitMissCollector implements IMetricsCollector<CacheHitMissMetrics> {

    private static final String CACHE_PERFORMANCE_ENDPOINT = "/rest/metrics/cache-performance";

    private final String label;
    private final RestMetricsCollector<CacheHitMissMetrics> delegate;

    /**
     * @param label   Label printed in logs
     * @param baseUrl base URL of the image service
     *                (ex "http://localhost:8080/tools.descartes.teastore.image")
     */
    public CacheHitMissCollector(String label, String baseUrl) {
        this.label = label;
        this.delegate = new RestMetricsCollector<>(
                baseUrl, CACHE_PERFORMANCE_ENDPOINT, "GET", CacheHitMissMetrics.class);
    }

    /**
     * @return the real mean hit/miss response times, or null on error.
     */
    @Override
    public CacheHitMissMetrics get() {
        try {
            return delegate.get();
        } catch (Exception e) {
            System.err.printf("[%sCollector] error calling cache-performance endpoint: %s%n", label, e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}