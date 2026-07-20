package tools.spirals.cerberus237.siphonix.strategies.javabip.controller.cache;

import java.io.IOException;

/**
 * A single cache whose eviction strategy can be changed at runtime.
 *
 * When switchStrategy() is called, the current items are extracted and
 * loaded into a fresh cache of the new strategy type, preserving content
 * while discarding internal metadata (access order, frequencies).
 */
public class SwitchableCache implements ICache {

    private ICache   delegate;
    private String   strategyName;

    public SwitchableCache(String strategy, int capacity) {
        this.strategyName = strategy.toUpperCase();
        this.delegate     = CacheFactory.create(strategyName, capacity);
    }

    /**
     * Switches to a new eviction strategy.
     * All items currently in the cache are transferred to the new strategy instance.
     * Internal ordering/frequency metadata cannot be migrated, so items are inserted
     * in the order returned by getItems() of the old strategy.
     */
    public void switchStrategy(String newStrategy) {
        String key = newStrategy.toUpperCase();
        if (key.equals(strategyName)) return;

        int[] items    = delegate.getItems();
        int   capacity = delegate.getCapacity();

        delegate     = CacheFactory.create(key, capacity);
        strategyName = key;

        delegate.replaceAllCache(items);

        System.out.printf("[SwitchableCache] Switched to %s (%d items transferred)%n", strategyName, items.length);
    }

    public String getActiveStrategy() {
        return strategyName;
    }

    // ── ICache delegation ────────────────────────────────────────────────────

    @Override public boolean contains(int item)   { return delegate.contains(item); }
    @Override public void    addItem(int item)     { delegate.addItem(item); }
    @Override public int     getCapacity()         { return delegate.getCapacity(); }
    @Override public void    setCapacity(int cap)  { delegate.setCapacity(cap); }
    @Override public int[]   getItems()            { return delegate.getItems(); }

    @Override
    public void replaceAllCache(int[] newCache) {
        delegate.replaceAllCache(newCache);
    }

    @Override
    public void recordStep(int capacity, float responseTime, int misses, int hits, int request) {
        delegate.recordStep(capacity, responseTime, misses, hits, request);
    }

    @Override
    public void exportHistoryCsv(String filePath) throws IOException {
        delegate.exportHistoryCsv(filePath);
    }

    @Override
    public String toString() {
        return "SwitchableCache[" + strategyName + ": " + delegate + "]";
    }
}
