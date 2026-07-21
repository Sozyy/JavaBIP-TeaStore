package tools.spirals.cerberus237.siphonix.strategies.javabip.cache;

import java.io.IOException;

public interface ICache {
    /**
     * Checks if the cache contains the item.
     * 
     * @param item the item to check
     * 
     * @return true if the item is the cache, false otherwise
     */
    boolean contains(int item);

    /**
     * Adds an item to the cache.
     * If the cache is full, an item will be evicted based on the cache strategy.
     * 
     * @param item the item to add
     */
    void addItem(int item);

    /**
     * Returns the capacity of the cache.
     * 
     * @return the capacity of the cache
     */
    int getCapacity();

    /**
     * Sets the cache a new capacity. 
     * If the new capacity is smaller than it was, items will be evicted based on the strategy
     */
    void setCapacity(int newCapacity);

    /**
     * Erases all of the cache and adds in the tea store cache, read via a collector
     * 
     * @param newCache the new cache
     */
    void replaceAllCache(int[] newCache);

    /**
     * Records the step of the cache as it is on call. 
     * This is used for plotting to help analysis.
     */
    void recordStep(int capacity, float responseTime, int misses, int hits, int request);

    /**
     * Returns all items currently in the cache as an array.
     * Order is strategy-dependent (access order for LRU/MRU, insertion order for FIFO/LIFO, etc.).
     */
    int[] getItems();

    /**
     * Exports the history of the cache to a CSV file.
     *
     * @param filePath the path to the CSV file
     * @throws IOException if an I/O error occurs
     */
    void exportHistoryCsv(String filePath) throws IOException;
}