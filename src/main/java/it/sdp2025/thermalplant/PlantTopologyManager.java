package it.sdp2025.thermalplant;

import java.util.*;

public class PlantTopologyManager {
    private final String id;
    private final Map<String, PeerInfo> peers = new HashMap<>();
    private volatile PeerInfo successor;

    public PlantTopologyManager(PlantConfig config){
        this.id = config.getId();
        peers.put(id, new PeerInfo(id, config.getHost(), config.getPort()));
    }

    public synchronized void addPeer(String id, String host, int port){
        peers.put(id, new PeerInfo(id, host, port));
        findSuccessor();
    }

    public synchronized void removePeer(String id){
        peers.remove(id);
        findSuccessor();
    }

    public synchronized List<PeerInfo> getPeers(){
        return new ArrayList<>(peers.values());
    }

    public PeerInfo getSuccessor() {
        return successor;
    }

    public synchronized void buildRing(){
        findSuccessor();
    }

    private void findSuccessor(){
        if(peers.size() == 1){
            successor = null;
            return;
        }

        NavigableSet<String> peerIds = new TreeSet<>(peers.keySet());
        String next = peerIds.tailSet(id, false).isEmpty() ? peerIds.first() : peerIds.tailSet(id, false).first();
        successor = peers.get(next);
        System.out.printf("[Topology] Successore di %s → %s (%s:%d)%n",
                id, successor.getId(), successor.getHost(), successor.getPort());

    }
}
