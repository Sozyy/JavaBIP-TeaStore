package tools.spirals.cerberus237.siphonix.strategies.javabip.cache;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Random Replacement cache.
 * A random item is evicted when the cache is full.
 */
public class RRCache extends AbstractAdapteaStoreCache {

    private final List<Integer> items;
    private final Set<Integer>  set;
    private final Random        rand;

    public RRCache(int capacity) {
        this(capacity, new Random());
    }

    public RRCache(int capacity, long seed) {
        this(capacity, new Random(seed));
    }

    private RRCache(int capacity, Random rand) {
        super(capacity);
        this.items = new ArrayList<>();
        this.set   = new HashSet<>();
        this.rand  = rand;
    }

    @Override
    public boolean contains(int item) {
        return set.contains(item);
    }

    @Override
    public void addItem(int item) {
        items.add(item);
        set.add(item);
        while (items.size() > capacity) {
            evict();
        }
    }

    @Override
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (items.size() > capacity) {
            evict();
        }
    }

    @Override
    protected int size() {
        return items.size();
    }

    @Override
    protected void evict() {
        if (items.isEmpty()) return;
        int idx = rand.nextInt(items.size());
        Integer removed = items.remove(idx);
        set.remove(removed);
    }

    @Override
    public int[] getItems() {
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "RRCache" + items;
    }
}
