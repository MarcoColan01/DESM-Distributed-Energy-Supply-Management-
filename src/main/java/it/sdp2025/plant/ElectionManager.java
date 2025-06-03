package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ElectionManager {
    private final String nodeId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;

    private double bestOffer;
    private String bestOfferId;
    private long currentTimestamp;
    private boolean isCoordinator = false;
    private boolean busy = false;
    private boolean isProducing = false;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public ElectionManager(String nodeId, TopologyManager topology, GrpcClient grpcClient) {
        this.nodeId = nodeId;
        this.topology = topology;
        this.grpcClient = grpcClient;
        clearBusy();  // inizializza le variabili correttamente
    }

    public synchronized boolean startElectionIfFree(double offer, long timestamp) {
        if (busy || isProducing)
            return false;

        busy = true;
        currentTimestamp = timestamp;
        bestOffer = offer;
        bestOfferId = nodeId;
        isCoordinator = false;

        String nextId = topology.getSuccessor();
        if (!nextId.equals(nodeId)) {
            PlantNetwork.ElectionMsg msg = PlantNetwork.ElectionMsg.newBuilder()
                    .setOffer(offer)
                    .setTimestamp(timestamp)
                    .setBestId(nodeId)
                    .setInitiatorId(nodeId)
                    .build();
            executor.submit(() -> grpcClient.forwardElection(nextId, msg));
        } else {
            becomeCoordinator();
        }
        return true;
    }

    public synchronized void handleElection(PlantNetwork.ElectionMsg msg) {
        String starterId = msg.getInitiatorId();
        double offer = msg.getOffer();
        long timestamp = msg.getTimestamp();
        String bestIdReceived = msg.getBestId();

        System.out.printf("[%s] RING attuale: %s%n", nodeId, topology.getPlants());
        System.out.printf("[%s] Ricevuto token, offerta: %.3f, starter: %s, bestId: %s%n",
                nodeId, offer, starterId, bestIdReceived);

        if (busy && currentTimestamp != timestamp) {
            System.out.printf("[%s] Passo token per timestamp %d (sto già gestendo %d)%n",
                    nodeId, timestamp, currentTimestamp);
            forwardToken(msg);
            return;
        }

        busy = true;
        currentTimestamp = timestamp;

        if (bestOfferId == null ||
                offer < bestOffer ||
                (offer == bestOffer && bestIdReceived.compareTo(bestOfferId) > 0)) {
            bestOffer = offer;
            bestOfferId = bestIdReceived;
        }

        if (starterId.equals(nodeId)) {
            becomeCoordinator();
            clearBusy(); // Libera lo stato post-elezione
        } else {
            PlantNetwork.ElectionMsg newMsg = PlantNetwork.ElectionMsg.newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(timestamp)
                    .setBestId(bestOfferId)
                    .setInitiatorId(starterId)
                    .build();
            forwardToken(newMsg);
        }
    }

    private void forwardToken(PlantNetwork.ElectionMsg msg) {
        String nextId = topology.getSuccessor();
        executor.submit(() -> grpcClient.forwardElection(nextId, msg));
    }

    private void becomeCoordinator() {
        isCoordinator = bestOfferId.equals(nodeId);
        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n", nodeId, bestOffer, currentTimestamp);
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                    nodeId, bestOfferId, bestOffer, currentTimestamp);
        }
    }

    public synchronized boolean isCoordinatorFor(long timestamp) {
        return isCoordinator && currentTimestamp == timestamp;
    }

    public synchronized void clearBusy() {
        busy = false;
        isCoordinator = false;
        bestOffer = Double.MAX_VALUE;
        bestOfferId = null;
        currentTimestamp = -1;
    }

    public synchronized boolean isProducing() {
        return isProducing;
    }

    public synchronized void setProducing(boolean producing) {
        isProducing = producing;
    }

    public void shutdown() {
        executor.shutdown();
    }
}
