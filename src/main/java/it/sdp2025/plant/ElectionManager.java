package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import it.sdp2025.common.EnergyRequest;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;

public class ElectionManager {
    private final String nodeId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;
    private final RequestBuffer requestBuffer;
    private double myOffer;
    private double bestOffer;
    private String bestOfferId;
    private long currentTimestamp;
    private boolean busy;
    private boolean isCoordinator;
    private boolean isProducing;
    private boolean justFinishedProduction;
    private final Set<Long> processedRequests = new HashSet<>();
    private long lastPrintedTs = -1;

    public ElectionManager(@NotNull String nodeId, @NotNull TopologyManager topology, @NotNull GrpcClient grpcClient) {
        this.nodeId = nodeId;
        this.topology = topology;
        this.grpcClient = grpcClient;
        this.requestBuffer = new RequestBuffer();
        clearElectionState();
        isProducing = false;
        justFinishedProduction = false;
    }

    public synchronized void handleEnergyRequest(@NotNull EnergyRequest request) {
        long timestamp = request.getTimestamp();

        if (processedRequests.contains(timestamp)) {
            return;
        }

        if (busy || isProducing) {
            requestBuffer.addRequest(request);
        } else {
            startElection(request);
        }
        notifyAll();
    }

    private void startElection(@NotNull EnergyRequest request) {
        double offer = 0.1 + 0.8 * Math.random();
        //offer = Math.floor(offer * 10) / 10;
        long timestamp = request.getTimestamp();
        processedRequests.add(timestamp);
        System.out.printf("[%s] OFFERTA %.3f $/kWh per req %d%n", nodeId, offer, timestamp);
        initializeElection(offer, timestamp);
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
            concludeElection();
        }
    }

    public synchronized void checkPendingRequests() {
        if (busy || isProducing || requestBuffer.isEmpty()) {
            return;
        }
        EnergyRequest nextRequest = null;

        while (!requestBuffer.isEmpty()) {
            nextRequest = requestBuffer.nextRequest();
            if (nextRequest == null) {
                break;
            }

            if (processedRequests.contains(nextRequest.getTimestamp())) {
                requestBuffer.getNextRequest();
                System.out.printf("[BUFFER] Richiesta %d già soddisfatta%n",
                        nextRequest.getTimestamp());
                nextRequest = null;
            } else {
                break;
            }
        }

        if (nextRequest != null) {
            requestBuffer.getNextRequest();
            startElection(nextRequest);
        }
    }

    public synchronized boolean getBusy(){
        return busy;
    }

    public synchronized boolean finishedProduction(){
        return justFinishedProduction;
    }

    public synchronized void handleElection(@NotNull PlantNetwork.ElectionMessage message) {
        String initiatorId = message.getInitiatorId();
        double offerMsg = message.getOffer();
        long timestamp = message.getTimestamp();
        String bestIdMsg = message.getBestId();

        if (busy && currentTimestamp != timestamp) {
            if (timestamp != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, bestIdMsg, offerMsg, timestamp);
                lastPrintedTs = timestamp;
            }
            forwardToken(message);
            return;
        }

        if (!busy) {
            if (isProducing || processedRequests.contains(timestamp)) {
                if (timestamp != lastPrintedTs) {
                    System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                            nodeId, bestIdMsg, offerMsg, timestamp);
                    lastPrintedTs = timestamp;
                }
                processedRequests.add(timestamp);
                forwardToken(message);
                return;
            }
            double offer = 0.1 + 0.8 * Math.random();
            //offer = Math.floor(offer * 10) / 10;
            initializeElection(myOffer, timestamp);
            processedRequests.add(timestamp);
            System.out.printf("[%s] OFFERTA %.3f $/kWh per req %d%n",
                    nodeId, myOffer, timestamp);
        }

        if (isBetterOffer(offerMsg, bestIdMsg)) {
            bestOffer = offerMsg;
            bestOfferId = bestIdMsg;
        }

        if (initiatorId.equals(nodeId)) {
            concludeElection();
        } else {
            PlantNetwork.ElectionMessage newMessage = PlantNetwork.ElectionMessage
                    .newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(timestamp)
                    .setBestId(bestOfferId)
                    .setInitiatorId(initiatorId)
                    .build();
            forwardToken(newMessage);
        }
    }

    private boolean isBetterOffer(double offer, String offerId) {
        if (offer < bestOffer) return true;
        if (offer > bestOffer) return false;
        return offerId.compareTo(bestOfferId) > 0;
    }

    private void initializeElection(double offer, long timestamp) {
        busy = true;
        currentTimestamp = timestamp;
        myOffer = offer;
        bestOffer = offer;
        bestOfferId = nodeId;
        isCoordinator = false;
    }

    private void concludeElection() {
        isCoordinator = nodeId.equals(bestOfferId);

        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n",
                    nodeId, bestOffer, currentTimestamp);
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                    nodeId, bestOfferId, bestOffer, currentTimestamp);
        }

        lastPrintedTs = currentTimestamp;
        busy = false;
        notifyAll();
    }

    private void clearElectionState() {
        busy = false;
        isCoordinator = false;
        myOffer = 0.0;
        bestOffer = Double.MAX_VALUE;
        bestOfferId = null;
        currentTimestamp = -1;
    }

    private void forwardToken(PlantNetwork.ElectionMessage message) {
        String next = topology.getSuccessor();
        new Thread(() -> grpcClient.forwardElection(next, message)).start();
    }

    public synchronized boolean isCoordinatorFor(long timestamp) {
        return isCoordinator && currentTimestamp == timestamp;
    }

    public synchronized boolean isProducing() {
        return isProducing;
    }

    public synchronized void setProducing(boolean producing) {
        isProducing = producing;
        notifyAll();
    }

    public synchronized void productionFinished() {
        isProducing = false;
        clearElectionState();
        justFinishedProduction = true;
        notifyAll();
    }

    public synchronized boolean hasWorkToDo(long lastTimestamp) {
        if (justFinishedProduction) {
            return true;
        }

        return (isCoordinatorFor(lastTimestamp) && !isProducing()) ||
                (!busy && !isProducing && !requestBuffer.isEmpty());
    }

    public synchronized void resetJustFinishedFlag() {
        justFinishedProduction = false;
    }
}