package tools.spirals.cerberus237.siphonix.strategies.javabip.controller.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Least Recently Used cache.
 * On a cache hit, the item is promoted to MRU position.
 * On eviction, the least recently accessed item is removed.
 */
public class LRUCache extends AbstractAdapteaStoreCache {

    private final LinkedHashMap<Integer, Boolean> cache;

    public LRUCache(int capacity) {
        super(capacity);
        this.cache = new LinkedHashMap<Integer, Boolean>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Integer, Boolean> e) {
                return size() > LRUCache.this.capacity;
            }
        };
    }

    @Override
    public boolean contains(int item) {
        // get() updates access order (moves item to MRU position)
        return cache.get(item) != null;
    }

    @Override
    public void addItem(int item) {
        cache.put(item, Boolean.TRUE);
    }

    @Override
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (cache.size() > capacity) {
            evict();
        }
    }

    @Override
    protected int size() {
        return cache.size();
    }

    @Override
    protected void evict() {
        // remove the LRU entry (head of access-order linked list)
        cache.remove(cache.keySet().iterator().next());
    }

    @Override
    public int[] getItems() {
        return cache.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "LRUCache" + cache.keySet();
    }
}
