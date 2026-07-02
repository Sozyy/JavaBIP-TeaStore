package controller.cache;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

/**
 * Least Frequently Used cache.
 * Tracks how many times each item has been accessed.
 * On eviction, the item with the lowest frequency is removed.
 * Ties are broken by insertion order (oldest inserted first).
 */
public class LFUCache extends AbstractAdapteaStoreCache {

    private final Map<Integer, Integer> freqMap;
    private final Map<Integer, LinkedHashSet<Integer>> freqBuckets;
    private int minFreq;

    public LFUCache(int capacity) {
        super(capacity);
        this.freqMap     = new HashMap<>();
        this.freqBuckets = new HashMap<>();
        this.minFreq     = 0;
    }

    @Override
    public boolean contains(int item) {
        if (!freqMap.containsKey(item)) return false;
        // Increment frequency and move to the next bucket
        int freq = freqMap.get(item);
        freqMap.put(item, freq + 1);
        freqBuckets.get(freq).remove(item);
        if (freqBuckets.get(freq).isEmpty()) {
            freqBuckets.remove(freq);
            if (minFreq == freq) minFreq++;
        }
        freqBuckets.computeIfAbsent(freq + 1, k -> new LinkedHashSet<>()).add(item);
        return true;
    }

    @Override
    public void addItem(int item) {
        while (freqMap.size() >= capacity) {
            evict();
        }
        freqMap.put(item, 1);
        freqBuckets.computeIfAbsent(1, k -> new LinkedHashSet<>()).add(item);
        minFreq = 1;
    }

    @Override
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (freqMap.size() > capacity) {
            evict();
        }
    }

    @Override
    protected int size() {
        return freqMap.size();
    }

    @Override
    protected void evict() {
        LinkedHashSet<Integer> bucket = freqBuckets.get(minFreq);
        if (bucket == null || bucket.isEmpty()) return;
        int lfu = bucket.iterator().next(); // oldest with min frequency
        bucket.remove(lfu);
        if (bucket.isEmpty()) freqBuckets.remove(minFreq);
        freqMap.remove(lfu);
    }

    @Override
    public int[] getItems() {
        return freqMap.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "LFUCache" + freqMap.keySet();
    }
}
