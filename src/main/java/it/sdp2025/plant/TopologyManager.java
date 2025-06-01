package it.sdp2025.plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TopologyManager {
    private final List<String> ring = new ArrayList<>(); // ID ordinati
    private final String myId;

    public TopologyManager(String myId) { this.myId = myId; }

    public synchronized void initRing(List<String> ids) {
        ring.clear();
        ring.addAll(ids);
        Collections.sort(ring);        // ordinamento logico
        if (!ring.contains(myId)) ring.add(myId);
    }

    public synchronized void addPlant(String id) {
        if (!ring.contains(id)) {
            ring.add(id); Collections.sort(ring);
        }
    }

    public synchronized String successor() {
        int idx = ring.indexOf(myId);
        return ring.get((idx + 1) % ring.size());
    }

    /* versione thread-safe della lista per debug / CLI */
    public synchronized List<String> getPlants() { return new ArrayList<>(ring); }
}

