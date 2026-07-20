package tools.spirals.cerberus237.siphonix.strategies.javabip.controller;

import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.core.RestMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheMetrics;

/**
 * Collects the real occupation of the AdaptableTeaStore image service cache,
 * via the official endpoint GET /rest/metrics/cache-metrics.
 *
 * Confirmed endpoint + model (CacheMetrics: maxCacheSize, currentCacheSize, freeSpace,
 * all expressed in bytes) by inspecting Arléon's CacheSizeAdaptationObservation, which
 * wires the same endpoint/model pair for AdaptiFlow but does not expose it to JavaBIP.
 *
 * Used at startup to size the PIDController's min/max cache bounds from the real
 * cache capacity instead of hardcoded guesses, and can also be polled at runtime
 * to log/monitor real occupation alongside the PID's own (item-based) estimate.
 */
public class CacheMetricsCollector implements IMetricsCollector<CacheMetrics> {

    private static final String CACHE_METRICS_ENDPOINT = "/rest/metrics/cache-metrics";

    private final String label;
    private final RestMetricsCollector<CacheMetrics> delegate;

    /**
     * @param label   Label printed in logs
     * @param baseUrl base URL of the image service
     *                (ex "http://localhost:8080/tools.descartes.teastore.image")
     */
    public CacheMetricsCollector(String label, String baseUrl) {
        this.label = label;
        this.delegate = new RestMetricsCollector<>(
                baseUrl, CACHE_METRICS_ENDPOINT, "GET", CacheMetrics.class);
    }

    /**
     * @return the real cache occupation (max/current/free, in bytes), or null on error.
     */
    @Override
    public CacheMetrics get() {
        try {
            return delegate.get();
        } catch (Exception e) {
            System.err.printf("[%sCollector] error calling cache-metrics endpoint: %s%n", label, e.getMessage());
            return null;
        }
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}