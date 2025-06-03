package it.sdp2025.plant;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TopologyManager {
    private final List<String> ring = new ArrayList<>();
    private final String myId;

    public TopologyManager(@NotNull String myId) {
        this.myId = myId;
    }

    public synchronized void initRing(@NotNull List<String> ids) {
        ring.clear();
        ring.addAll(ids);
        Collections.sort(ring);
        if (!ring.contains(myId)) ring.add(myId);
    }

    public synchronized void addPlant(@NotNull String id) {
        if (!ring.contains(id)) {
            ring.add(id); Collections.sort(ring);
            System.out.printf("[%s] RING → %s%n", myId, ring);
        }
    }

    public synchronized String getSuccessor() {
        int idx = ring.indexOf(myId);
        return ring.get((idx + 1) % ring.size());
    }

    public synchronized String getMyId(){
        return myId;
    }

    public synchronized List<String> getPlants() {
        return new ArrayList<>(ring);
    }
}
