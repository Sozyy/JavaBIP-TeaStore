package tools.spirals.cerberus237.siphonix.strategies.javabip.cache;

public class CacheFactory {

    public static final String[] ALL_STRATEGIES = {"FIFO", "LIFO", "LFU", "LRU", "MRU", "RR"};

    public static ICache create(String strategy, int capacity) {
        switch (strategy.toUpperCase()) {
            case "FIFO": return new FIFOCache(capacity);
            case "LIFO": return new LIFOCache(capacity);
            case "LFU" : return new LFUCache(capacity);
            case "LRU" : return new LRUCache(capacity);
            case "MRU" : return new MRUCache(capacity);
            case "RR"  : return new RRCache(capacity);
            default    : return new LRUCache(capacity);
        }
    }
}
