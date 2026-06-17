package adapteastore.cache;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Most Recently Used cache.
 * On a cache hit, the item is promoted to MRU position.
 * On eviction, the most recently accessed item is removed.
 * This is the inverse eviction policy of LRU.
 */
public class MRUCache extends AbstractAdapteaStoreCache {

    private final LinkedHashMap<Integer, Boolean> cache;

    public MRUCache(int capacity) {
        super(capacity);
        this.cache = new LinkedHashMap<>(16, 0.75f, true);
    }

    @Override
    public boolean contains(int item) {
        // get() updates access order (moves item to MRU position)
        return cache.get(item) != null;
    }

    @Override
    public void addItem(int item) {
        cache.put(item, Boolean.TRUE);
        while (cache.size() > capacity) {
            evict();
        }
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
        // The last entry in an access-order LinkedHashMap is the MRU entry
        Integer mruKey = null;
        for (Integer key : cache.keySet()) {
            mruKey = key;
        }
        if (mruKey != null) cache.remove(mruKey);
    }

    @Override
    public int[] getItems() {
        return cache.keySet().stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "MRUCache" + cache.keySet();
    }
}
