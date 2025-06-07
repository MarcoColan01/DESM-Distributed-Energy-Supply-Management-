package it.sdp2025.simulator;

import java.util.ArrayList;
import java.util.List;

public final class SensorBuffer implements Buffer {
    private final List<Measurement> buffer = new ArrayList<>();

    @Override
    public synchronized void addMeasurement(Measurement m) {
        buffer.add(m);
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> bufferCopy = new ArrayList<>(buffer);
        buffer.clear();
        return bufferCopy;
    }
}
