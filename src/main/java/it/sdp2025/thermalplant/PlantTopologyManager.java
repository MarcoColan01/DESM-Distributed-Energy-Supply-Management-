package it.sdp2025.thermalplant;


import it.sdp2025.common.PlantInfo;

import java.util.*;

public class PlantTopologyManager {

    private final PlantConfig cfg;
    private final Map<String, PlantInfo> peers = new HashMap<>();
    private final SortedSet<String> ids       = new TreeSet<>();
    private final GrpcClient client;

    private PlantInfo successor = null;

    public PlantTopologyManager(PlantConfig cfg, GrpcClient client) {
        this.cfg    = cfg;
        this.client = client;
        ids.add(cfg.getId());
    }

    // aggiungi in PlantTopologyManager.java

    /* ---------------------------------------------------- */
    /* 1. aggiunge (o aggiorna) un singolo peer             */
    public synchronized void addPeer(String id, String host, int port) {
        if (id == null || id.isBlank()) return;
        PlantInfo info = new PlantInfo(id, host, port);
        peers.put(id, info);
        ids.add(id);
    }

    /* 2. registra in blocco un elenco iniziale             */
    public synchronized void addPeers(Collection<PlantInfo> list) {
        list.forEach(p -> addPeer(p.getId(), p.getHost(), p.getPort()));
    }


    public synchronized void buildRing() {
        String me = cfg.getId();

        /* eleggi successore */
        String next = ids.tailSet(me).stream()
                .filter(id -> id.compareTo(me) > 0)
                .findFirst()
                .orElseGet(() -> ids.isEmpty() ? null : ids.first());

        successor = next == null ? null : peers.get(next);

        if (successor != null) {
            System.out.printf("[Topology] Successore di %s → %s (%s:%d)%n",
                    me, successor.getId(), successor.getHost(), successor.getPort());
        }

        /* chiediamo al client di (ri)connettersi SOLO se il successore
           è cambiato; dentro `connect()` viene scartato il caso null.   */
        client.connect(successor);
    }

    public synchronized PlantInfo getSuccessor() { return successor; }
    public synchronized Collection<PlantInfo> getPeers() {
        return new ArrayList<>(peers.values());
    }
}
