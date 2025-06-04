package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import org.jetbrains.annotations.NotNull;

public class ElectionManager {
    private final String nodeId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;
    private double bestOffer;
    private String bestOfferId;
    private long currentTimestamp;
    private boolean busy;
    private boolean isCoordinator;
    private boolean isProducing;
    private long lastPrintedTs = -1;

    public ElectionManager(@NotNull String nodeId, @NotNull TopologyManager topology, @NotNull GrpcClient grpcClient) {
        this.nodeId = nodeId;
        this.topology = topology;
        this.grpcClient = grpcClient;
        clearBusy();
        isProducing = false;
    }

    public synchronized void startElectionIfFree(double offer, long timestamp) {
        if (busy || isProducing) return;

        busy = true;
        currentTimestamp = timestamp;
        bestOffer = offer;
        bestOfferId = nodeId;
        isCoordinator = false;
        String next = topology.getSuccessor();

        if (!next.equals(nodeId)) {
            PlantNetwork.ElectionMessage message = PlantNetwork.ElectionMessage
                    .newBuilder()
                    .setOffer(offer)
                    .setTimestamp(timestamp)
                    .setBestId(nodeId)
                    .setInitiatorId(nodeId)
                    .build();
            forwardToken(message);
        } else {
            becomeCoordinator();
        }
    }

    public synchronized boolean isCoordinatorFor(long timestamp) {
        return isCoordinator && currentTimestamp == timestamp;
    }

    public synchronized boolean isProducing(){
        return isProducing;
    }

    public synchronized void setProducing(boolean producing)   {
        isProducing = producing;
        notifyAll();
    }

    public synchronized void productionFinished() {
        isProducing = false;
        clearBusy();
        notifyAll();
    }

    public synchronized void handleElection(@NotNull PlantNetwork.ElectionMessage message) {
        String initiatorId = message.getInitiatorId();
        double offer = message.getOffer();
        long timestamp = message.getTimestamp();
        String bestIdMsg = message.getBestId();

        if (busy && currentTimestamp != timestamp) {
            if (timestamp != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, message.getBestId(), message.getOffer(), timestamp);
                lastPrintedTs = timestamp;
            }
            forwardToken(message);
            return;
        }
        busy = true;
        currentTimestamp = timestamp;

        if (bestOfferId == null || offer < bestOffer || (offer == bestOffer && bestIdMsg.compareTo(bestOfferId) > 0)) {
            bestOffer   = offer;
            bestOfferId = bestIdMsg;
        }

        if (initiatorId.equals(nodeId)) {
            becomeCoordinator();
            if (!isCoordinator) {
                clearBusy();
            }
        } else {
            PlantNetwork.ElectionMessage newMessage = PlantNetwork.ElectionMessage
                    .newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(timestamp)
                    .setBestId(bestOfferId)
                    .setInitiatorId(initiatorId)
                    .build();
            forwardToken(newMessage);
            clearBusy();
        }
    }

    private void forwardToken(@NotNull PlantNetwork.ElectionMessage message) {
        String next = topology.getSuccessor();
        new Thread(() -> grpcClient.forwardElection(next, message)).start();
    }

    private void becomeCoordinator() {
        isCoordinator = nodeId.equals(bestOfferId);

        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n", nodeId, bestOffer, currentTimestamp);
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                    nodeId, bestOfferId, bestOffer, currentTimestamp);
        }
        lastPrintedTs = currentTimestamp;
        notifyAll();
    }

    private void clearBusy() {
        busy             = false;
        isCoordinator    = false;
        bestOffer        = Double.MAX_VALUE;
        bestOfferId      = null;
        currentTimestamp = -1;
    }

    //public void shutdown() {}
}
