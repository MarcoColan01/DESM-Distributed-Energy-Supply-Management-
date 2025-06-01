package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import it.sdp2025.common.PlantInfo;

public class ElectionManager {
    private final String nodeId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;

    private double bestOffer = Double.MAX_VALUE;
    private String bestOfferId = null;
    private long currentTimestamp = -1;
    private boolean isCoordinator = false;
    private boolean busy = false;

    public ElectionManager(String nodeId, TopologyManager topology, GrpcClient grpcClient) {
        this.nodeId = nodeId;
        this.topology = topology;
        this.grpcClient = grpcClient;
    }

    /**
     * Avvia una nuova elezione, solo se libera, per una certa richiesta.
     * Viene chiamato da ThermalPlantMain con l'offerta generata localmente.
     */
    public synchronized void startElectionIfFree(double offer, long timestamp) {
        if (busy && currentTimestamp == timestamp)
            return;

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
                    .setInitiatorId(nodeId)      // AGGIUNTO
                    .build();
            grpcClient.forwardElection(nextId, msg);
        } else {
            // Sono l'unico nodo, mi proclamo subito
            becomeCoordinator();
        }
    }

    /**
     * Gestisce un ElectionMsg ricevuto da un altro nodo via gRPC.
     */
    public synchronized void handleElection(PlantNetwork.ElectionMsg msg) {
        System.out.printf("[%s] RING attuale: %s%n", nodeId, topology.getPlants());
        System.out.printf("[%s] Ricevuto token, offerta: %.3f, starter: %s, bestId: %s, bestOffer: %.3f%n",
                nodeId, bestOffer, msg.getInitiatorId(), msg.getBestId(), msg.getOffer());


        double offer = msg.getOffer();
        long timestamp = msg.getTimestamp();
        String bestIdReceived = msg.getBestId();
        String starterId = msg.getInitiatorId();

        // IGNORA SOLO se sto partecipando a una richiesta DIVERSA
        if (currentTimestamp != -1 && currentTimestamp != timestamp) {
            System.out.printf("[%s] Ignora election per timestamp %d: sto già gestendo %d%n", nodeId, timestamp, currentTimestamp);
            return;
        }

        busy = true;
        currentTimestamp = timestamp;

        // Valuta offerta migliore
        if (offer < bestOffer || bestOfferId == null) {
            bestOffer = offer;
            bestOfferId = bestIdReceived;
        }

        if (starterId.equals(nodeId)) {
            becomeCoordinator();
            clearBusy(); // RESETTA per accettare nuove richieste!
        } else {
            String nextId = topology.getSuccessor();
            PlantNetwork.ElectionMsg newMsg = PlantNetwork.ElectionMsg.newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(timestamp)
                    .setBestId(bestOfferId)
                    .setInitiatorId(starterId)
                    .build();
            grpcClient.forwardElection(nextId, newMsg);
        }
    }


    private void becomeCoordinator() {
        isCoordinator = bestOfferId.equals(nodeId);
        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n", nodeId, bestOffer, currentTimestamp);
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n", nodeId, bestOfferId, bestOffer, currentTimestamp);
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
}
