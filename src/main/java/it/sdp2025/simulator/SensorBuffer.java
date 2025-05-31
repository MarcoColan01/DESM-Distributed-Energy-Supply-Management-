package it.sdp2025.simulator;

import java.util.ArrayList;
import java.util.List;

/** Buffer thread-safe minimalista. */
public final class SensorBuffer implements Buffer {

    private final List<Measurement> data = new ArrayList<>();

    @Override
    public synchronized void addMeasurement(Measurement m) {
        data.add(m);
    }

    @Override
    public synchronized List<Measurement> readAllAndClean() {
        List<Measurement> copy = new ArrayList<>(data);
        data.clear();
        return copy;
    }
}

