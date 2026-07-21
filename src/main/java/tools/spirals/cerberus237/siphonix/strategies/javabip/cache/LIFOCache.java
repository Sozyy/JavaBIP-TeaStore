package tools.spirals.cerberus237.siphonix.strategies.javabip.cache;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * Last In First Out cache.
 * The most recently inserted item is evicted first
 */
public class LIFOCache extends AbstractAdapteaStoreCache {

    private final Deque<Integer> stack;
    private final Set<Integer> set;

    public LIFOCache(int capacity) {
        super(capacity);
        this.stack = new ArrayDeque<>();
        this.set   = new HashSet<>();
    }

    @Override
    public boolean contains(int item) {
        return set.contains(item);
    }

    @Override
    public void addItem(int item) {
        stack.push(item);
        set.add(item);
        while (stack.size() > capacity) {
            evict();
        }
    }

    @Override
    public void setCapacity(int newCapacity) {
        this.capacity = newCapacity;
        while (stack.size() > capacity) {
            evict();
        }
    }

    @Override
    protected int size() {
        return stack.size();
    }

    @Override
    protected void evict() {
        Integer removed = stack.pop();
        if (removed != null) set.remove(removed);
    }

    @Override
    public int[] getItems() {
        return set.stream().mapToInt(Integer::intValue).toArray();
    }

    @Override
    public String toString() {
        return "LIFOCache" + stack;
    }
}

