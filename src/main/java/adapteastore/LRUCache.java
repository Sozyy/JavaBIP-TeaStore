package adapteastore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashMap;

/**
 * Least Recently Used Cache
 *
 * Loads a given number of images, if the image was already loaded, a hit is added, otherwise a miss.
 * Hits and misses have values and adding all of them up gives the responseTime
 */
public class LRUCache {
    private int capacity;
    private final LinkedHashMap<Integer, Boolean> cache;

    // cache history for plotting
    private final List<Integer> capacityHistory     = new ArrayList<>();
    private final List<Float>   responseTimeHistory = new ArrayList<>();
    private final List<Integer> missesHistory       = new ArrayList<>();
    private final List<Integer> hitsHistory         = new ArrayList<>();
    private final List<Integer> requestHistory      = new ArrayList<>();

    public LRUCache(int capacity) {
        this.capacity = capacity;
        this.cache = new LinkedHashMap<Integer, Boolean>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> e) {
                return size() > LRUCache.this.capacity;
            }
        };
        capacityHistory.add(capacity);
        responseTimeHistory.add(0.0F);
        missesHistory.add(0);
        hitsHistory.add(0);
        requestHistory.add(0);
    }

    /**
     * Returns true if item is present in cache, false otherwise
     */
    public boolean contains(int item) {
        return cache.containsKey(item);
    }

    /**
     * Adds item to the cache, if the cache is full, the least recently used item will be removed
     */
    public void addItem(int item) {
        cache.put(item, Boolean.TRUE);
    }

    public int getCapacity() { return capacity; }

    /**
     * Set a new cache capacity, if the new capacity is smaller than the current number of items in the cache,
     * the least recently used items will be removed until the cache size is equal to the new capacity
     */
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (cache.size() > capacity) {
            cache.remove(cache.keySet().iterator().next()); // deletes oldest
        }
    }

    /**
     * Records the current capacity, responseTime, misses, hits and requestSize for each iteration
     */
    public void recordStep(int capacity, float responseTime, int misses, int hits, int request) {
        capacityHistory.add(capacity);
        responseTimeHistory.add(responseTime);
        missesHistory.add(misses);
        hitsHistory.add(hits);
        requestHistory.add(request);
    }

    /**
     * Exports the capacity, responseTime, misses, hits and requestSize for each step in a .csv file
     *
     * @param filePath the path to the .csv file to write to
     */
    public void exportHistoryCsv(String filePath) throws IOException {
        java.io.File file = new java.io.File(filePath);
        java.io.File parent = file.getParentFile();

        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(file))) {
            writer.write("step,capacity,responseTime,misses,hits,request\n");
            for (int i = 0; i < capacityHistory.size(); i++) {
                writer.write(i + "," +
                        capacityHistory.get(i) + "," +
                        responseTimeHistory.get(i) + "," +
                        missesHistory.get(i) + "," +
                        hitsHistory.get(i) + "," +
                        requestHistory.get(i) + "\n");
            }
        }

        System.out.println("CSV written to: " + file.getAbsolutePath());
    }

    @Override
    public String toString() {
        return "Cache content: " + cache.keySet();
    }
}
