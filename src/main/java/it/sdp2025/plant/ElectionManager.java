package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import org.jetbrains.annotations.NotNull;

public class ElectionManager {
    private final String           nodeId;
    private final TopologyManager  topology;
    private final GrpcClient       grpcClient;
    private double  bestOffer;
    private String  bestOfferId;
    private long    currentTimestamp;
    private boolean busy;
    private boolean isCoordinator;
    private boolean isProducing;
    private long lastPrintedTs = -1;

    public ElectionManager(@NotNull String nodeId, @NotNull TopologyManager topology, @NotNull GrpcClient grpcClient) {
        this.nodeId     = nodeId;
        this.topology   = topology;
        this.grpcClient = grpcClient;
        clearBusy();
        isProducing = false;
    }

    public synchronized void startElectionIfFree(double offer, long timestamp) {
        if (busy || isProducing) return;

        busy             = true;
        currentTimestamp = timestamp;
        bestOffer        = offer;
        bestOfferId      = nodeId;
        isCoordinator    = false;
        String next = topology.getSuccessor();

        if (!next.equals(nodeId)) {
            PlantNetwork.ElectionMsg msg = PlantNetwork.ElectionMsg
                    .newBuilder()
                    .setOffer(offer)
                    .setTimestamp(timestamp)
                    .setBestId(nodeId)
                    .setInitiatorId(nodeId)
                    .build();
            forwardToken(msg);
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

    public synchronized void setProducing(boolean flag)   {
        isProducing = flag;
    }

    public synchronized void productionFinished() {
        isProducing = false;
        clearBusy();
    }

    public synchronized void handleElection(@NotNull PlantNetwork.ElectionMsg msg) {
        String starterId      = msg.getInitiatorId();
        double offer          = msg.getOffer();
        long   ts             = msg.getTimestamp();
        String bestIdReceived = msg.getBestId();

        if (busy && currentTimestamp != ts) {
            if (ts != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, msg.getBestId(), msg.getOffer(), ts);
                lastPrintedTs = ts;
            }
            forwardToken(msg);
            return;
        }
        busy             = true;
        currentTimestamp = ts;

        if (bestOfferId == null ||
                offer < bestOffer ||
                (offer == bestOffer && bestIdReceived.compareTo(bestOfferId) > 0)) {

            bestOffer   = offer;
            bestOfferId = bestIdReceived;
        }

        if (starterId.equals(nodeId)) {
            becomeCoordinator();
            if (!isCoordinator) {
                clearBusy();
            }
        } else {
            PlantNetwork.ElectionMsg newMsg = PlantNetwork.ElectionMsg
                    .newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(ts)
                    .setBestId(bestOfferId)
                    .setInitiatorId(starterId)
                    .build();
            forwardToken(newMsg);
            clearBusy();
        }
    }

    private void forwardToken(@NotNull PlantNetwork.ElectionMsg msg) {
        String next = topology.getSuccessor();
        new Thread(() -> grpcClient.forwardElection(next, msg)).start();
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
    }

    private void clearBusy() {
        busy             = false;
        isCoordinator    = false;
        bestOffer        = Double.MAX_VALUE;
        bestOfferId      = null;
        currentTimestamp = -1;
    }

    public void shutdown() {}
}
