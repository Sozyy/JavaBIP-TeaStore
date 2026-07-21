package tools.spirals.cerberus237.siphonix.strategies.javabip.http;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

import tools.spirals.cerberus237.metricscollectorbase.IMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.core.RestMetricsCollector;
import tools.spirals.cerberus237.metricscollectorbase.models.CacheEntryMetrics;

/**
 * Collects the real cache entries (image IDs) currently held by the AdaptableTeaStore
 * image service, via the official endpoint GET /rest/metrics/cache-entries.
 *
 * Replaces the former LoadedImagesCollector, which targeted an invented endpoint
 * (/rest/metrics/loadedImages) with a guessed JSON field name. This one uses the
 * real model class (CacheEntryMetrics) confirmed in metricscollectorbase, via
 * RestMetricsCollector — no more field-name guessing needed.
 *
 * Type note: CacheEntryMetrics.id is a `long` in the REST model, but ICache /
 * DataProvider / PIDController in this project work with `int` item IDs and
 * `int` capacities (item-count based cache, not byte-based). AdaptableTeaStore
 * image IDs are expected to stay well within int range, so we narrow long -> int
 * here, with a guard that skips (and logs) any id that wouldn't fit. If that
 * assumption ever breaks (huge datasets, non-sequential/large IDs), ICache and
 * the rest of the pipeline will need to be migrated to long.
 */
public class CacheEntriesCollector implements IMetricsCollector<int[]> {

    private static final String CACHE_ENTRIES_ENDPOINT = "/rest/metrics/cache-entries";

    private final String label;
    private final RestMetricsCollector<List<CacheEntryMetrics>> delegate;
    private final ObjectMapper mapper = new ObjectMapper();

    // Average real byte size of the images seen on the last get() call. Lets callers convert
    // the item-count based PID/shadow-cache size into a byte target meaningful to TeaStore's
    // real, byte-based cache (see CacheManagementStrategy). 0.0 until the first non-empty poll.
    private volatile double lastAverageByteSize = 0.0;

    /**
     * @param label   Label printed in logs
     * @param baseUrl base URL of the image service
     *                (ex "http://localhost:8083/tools.descartes.teastore.image")
     */
    @SuppressWarnings("unchecked")
    public CacheEntriesCollector(String label, String baseUrl) {
        this.label = label;
        this.delegate = new RestMetricsCollector<List<CacheEntryMetrics>>(
                baseUrl, CACHE_ENTRIES_ENDPOINT, "GET",
                (Class<List<CacheEntryMetrics>>) (Class<?>) List.class);
    }

    /**
     * @return the IDs of the images currently in the real cache, or an empty array on error.
     *         IDs that don't fit in an int are skipped (see class javadoc).
     */
    @Override
    public int[] get() {
        // delegate.get() deserializes via Jackson's Class<T> overload, so due to generic type
        // erasure the elements come back as raw LinkedHashMap, not CacheEntryMetrics, despite
        // the declared List<CacheEntryMetrics> type. Converting each element explicitly avoids
        // the ClassCastException that would otherwise happen on the first non-empty response.
        List<?> entries;
        try {
            entries = delegate.get();
        } catch (Exception e) {
            System.err.printf("[%sCollector] error calling cache-entries endpoint: %s%n", label, e.getMessage());
            lastAverageByteSize = 0.0;
            return new int[0];
        }

        if (entries == null || entries.isEmpty()) {
            lastAverageByteSize = 0.0;
            return new int[0];
        }

        int[] ids = new int[entries.size()];
        int count = 0;
        long totalBytes = 0;
        for (Object raw : entries) {
            CacheEntryMetrics entry = mapper.convertValue(raw, CacheEntryMetrics.class);
            totalBytes += entry.getByteSize();
            long id = entry.getId();
            if (id < Integer.MIN_VALUE || id > Integer.MAX_VALUE) {
                System.err.printf("[%sCollector] image id %d out of int range, skipping%n", label, id);
                continue;
            }
            ids[count++] = (int) id;
        }
        lastAverageByteSize = (double) totalBytes / entries.size();

        return count == ids.length ? ids : java.util.Arrays.copyOf(ids, count);
    }

    /**
     * @return the average byte size of the images seen on the last get() call,
     *         or 0.0 if unknown (no entries yet, or the last call failed).
     */
    public double getLastAverageByteSize() {
        return lastAverageByteSize;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
