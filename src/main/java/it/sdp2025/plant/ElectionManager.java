package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import org.jetbrains.annotations.NotNull;

public class ElectionManager {
    private final String nodeId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;

    // Stato per l'elezione corrente
    private double myOffer;
    private long electionTimestamp;
    private boolean participating;
    private boolean isCoordinator;
    private boolean isProducing;

    // Per evitare stampe duplicate
    private long lastPrintedTs = -1;

    public ElectionManager(@NotNull String nodeId, @NotNull TopologyManager topology, @NotNull GrpcClient grpcClient) {
        this.nodeId = nodeId;
        this.topology = topology;
        this.grpcClient = grpcClient;
        reset();
    }

    public synchronized void startElectionIfFree(double offer, long timestamp) {
        // Non partecipo se sto già partecipando a un'elezione o sto producendo
        if (participating || isProducing) {
            return;
        }

        // Inizio una nuova elezione
        participating = true;
        myOffer = offer;
        electionTimestamp = timestamp;
        isCoordinator = false;

        String next = topology.getSuccessor();

        // Se sono solo nell'anello, divento subito coordinatore
        if (next.equals(nodeId)) {
            becomeCoordinator(nodeId, offer, timestamp);
            return;
        }

        // Altrimenti invio il messaggio di elezione
        PlantNetwork.ElectionMessage message = PlantNetwork.ElectionMessage
                .newBuilder()
                .setOffer(offer)
                .setTimestamp(timestamp)
                .setBestId(nodeId)
                .setInitiatorId(nodeId)
                .build();
        forwardToken(message);
    }

    public synchronized boolean isCoordinatorFor(long timestamp) {
        return isCoordinator && electionTimestamp == timestamp;
    }

    public synchronized boolean isProducing() {
        return isProducing;
    }

    public synchronized void setProducing(boolean producing) {
        isProducing = producing;
        if (!producing) {
            // Quando finisco di produrre, resetto lo stato
            reset();
        }
        notifyAll();
    }

    public synchronized void productionFinished() {
        setProducing(false);
    }

    public synchronized void handleElection(@NotNull PlantNetwork.ElectionMessage message) {
        String initiatorId = message.getInitiatorId();
        double bestOffer = message.getOffer();
        long timestamp = message.getTimestamp();
        String bestId = message.getBestId();

        // Se sto producendo, inoltra sempre il messaggio senza modifiche
        if (isProducing) {
            forwardToken(message);
            return;
        }

        // Se il messaggio è tornato all'iniziatore
        if (initiatorId.equals(nodeId)) {
            // L'elezione è terminata
            becomeCoordinator(bestId, bestOffer, timestamp);
            return;
        }

        // Se sto già partecipando a un'elezione per questo timestamp
        if (participating && electionTimestamp == timestamp) {
            // Confronto la mia offerta con quella nel messaggio
            if (myOffer < bestOffer || (myOffer == bestOffer && nodeId.compareTo(bestId) > 0)) {
                // La mia offerta è migliore, aggiorno il messaggio
                bestOffer = myOffer;
                bestId = nodeId;
            }
        } else if (!participating) {
            // Non sto partecipando, ma devo comunque inoltrare
            // Stampo il risultato se non l'ho già fatto
            if (timestamp != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, bestId, bestOffer, timestamp);
                lastPrintedTs = timestamp;
            }
        }

        // Inoltro il messaggio con l'offerta migliore
        PlantNetwork.ElectionMessage newMessage = PlantNetwork.ElectionMessage
                .newBuilder()
                .setOffer(bestOffer)
                .setTimestamp(timestamp)
                .setBestId(bestId)
                .setInitiatorId(initiatorId)
                .build();
        forwardToken(newMessage);
    }

    private void forwardToken(@NotNull PlantNetwork.ElectionMessage message) {
        String next = topology.getSuccessor();
        new Thread(() -> grpcClient.forwardElection(next, message)).start();
    }

    private void becomeCoordinator(String winnerId, double winnerOffer, long timestamp) {
        isCoordinator = winnerId.equals(nodeId);

        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n", nodeId, winnerOffer, timestamp);
            // Rimango participating = true finché non finisco la produzione
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                    nodeId, winnerId, winnerOffer, timestamp);
            // Non sono coordinatore, posso resettare
            reset();
        }

        lastPrintedTs = timestamp;
        notifyAll();
    }

    private void reset() {
        participating = false;
        isCoordinator = false;
        isProducing = false;
        myOffer = Double.MAX_VALUE;
        electionTimestamp = -1;
    }
}