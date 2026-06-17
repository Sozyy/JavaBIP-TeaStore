package adapteastore.cache;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * First In First Out cache.
 * The oldest inserted item is evicted first, regardless of access pattern.
 */
public class FIFOCache extends AbstractAdapteaStoreCache {
    
    private final Queue<Integer> queue;
    private final Set<Integer>   set;

    public FIFOCache(int capacity) {
        super(capacity);
        this.queue = new LinkedList<>();
        this.set   = new HashSet<>();
    }

    @Override
    public boolean contains(int item) {
        return set.contains(item);
    }

    @Override
    public void addItem(int item) {
        queue.add(item);
        set.add(item);
        while (queue.size() > capacity) {
            evict();
        }
    }

    @Override
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (queue.size() > capacity) {
            evict();
        }
    }

    @Override
    protected int size() {
        return queue.size();
    }

    @Override
    protected void evict() {
        Integer removed = queue.poll();
        if (removed != null) set.remove(removed);
    }

    @Override
    public int[] getItems() {
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "FIFOCache" + queue;
    }
}
