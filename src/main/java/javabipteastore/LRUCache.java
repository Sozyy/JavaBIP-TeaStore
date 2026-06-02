package TeaStoreWithConnector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import java.util.LinkedHashMap;

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

    public boolean contains(int item) {
        return cache.containsKey(item);
    }

    public void addItem(int item) {
        cache.put(item, Boolean.TRUE);
    }

    public int getCapacity() { return capacity; }

    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (cache.size() > capacity) {
            cache.remove(cache.keySet().iterator().next()); // deletes oldest
        }
    }

    public void recordStep(int capacity, float responseTime, int misses, int hits, int request) {
        capacityHistory.add(capacity);
        responseTimeHistory.add(responseTime);
        missesHistory.add(misses);
        hitsHistory.add(hits);
        requestHistory.add(request);
    }

    public List<Integer> getCapacityHistory() {
        return Collections.unmodifiableList(capacityHistory);
    }

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
