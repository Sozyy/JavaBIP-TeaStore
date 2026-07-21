package tools.spirals.cerberus237.siphonix.strategies.javabip.cache;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractAdapteaStoreCache implements ICache {

    protected int capacity;

    // History tracking for plotting
    private final List<Integer> capacityHistory     = new ArrayList<>();
    private final List<Float>   responseTimeHistory = new ArrayList<>();
    private final List<Integer> missesHistory       = new ArrayList<>();
    private final List<Integer> hitsHistory         = new ArrayList<>();
    private final List<Integer> requestHistory      = new ArrayList<>();

    public AbstractAdapteaStoreCache(int capacity) {
        this.capacity = capacity;
        capacityHistory.add(capacity);
        responseTimeHistory.add(0.0F);
        missesHistory.add(0);
        hitsHistory.add(0);
        requestHistory.add(0);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    @Override
    public void replaceAllCache(int[] teaStoreCache) {
        while (size() != 0) {
            evict();
        }
        for (int item : teaStoreCache) {
            addItem(item);
        }
    }

    @Override
    public void recordStep(int capacity, float responseTime, int misses, int hits, int request) {
        capacityHistory.add(capacity);
        responseTimeHistory.add(responseTime);
        missesHistory.add(misses);
        hitsHistory.add(hits);
        requestHistory.add(request);
    }

    @Override
    public void exportHistoryCsv(String filePath) throws IOException {
        File file = new File(filePath);
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
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

    protected abstract int size();

    protected abstract void evict();
}
