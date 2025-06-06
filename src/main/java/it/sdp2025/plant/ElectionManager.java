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

    // Stato dell'elezione corrente
    private double myOffer;
    private double bestOffer;
    private String bestOfferId;
    private long currentTimestamp;

    // Stato del nodo
    private boolean busy;           // Partecipa ad un'elezione
    private boolean isCoordinator;  // È il coordinatore per l'elezione corrente
    private boolean isProducing;    // Sta producendo energia
    private boolean justFinishedProduction; // Flag per indicare che ha appena finito di produrre

    // Tracciamento delle richieste già processate
    private final Set<Long> processedRequests = new HashSet<>();

    // Per evitare stampe duplicate
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

    /**
     * Gestisce una nuova richiesta di energia.
     * Se il nodo è libero, avvia un'elezione. Altrimenti, accoda la richiesta.
     */
    public synchronized void handleEnergyRequest(@NotNull EnergyRequest request) {
        long timestamp = request.getTimestamp();

        // Evita di processare richieste duplicate
        if (processedRequests.contains(timestamp)) {
            return;
        }

        if (busy || isProducing) {
            // Nodo occupato - accoda la richiesta
            requestBuffer.addRequest(request);
        } else {
            // Nodo libero - avvia elezione
            startElection(request);
        }
        notifyAll(); // Risveglia il thread principale
    }

    /**
     * Avvia un'elezione per una richiesta specifica
     */
    private void startElection(@NotNull EnergyRequest request) {
        double offer = 0.1 + 0.8 * Math.random();
        long timestamp = request.getTimestamp();

        // Marca la richiesta come processata
        processedRequests.add(timestamp);

        System.out.printf("[%s] OFFERTA %.3f $/kWh per req %d%n", nodeId, offer, timestamp);

        // Inizia una nuova elezione
        initializeElection(offer, timestamp);

        String next = topology.getSuccessor();
        if (!next.equals(nodeId)) {
            // Invia il token al successore
            PlantNetwork.ElectionMessage message = PlantNetwork.ElectionMessage
                    .newBuilder()
                    .setOffer(offer)
                    .setTimestamp(timestamp)
                    .setBestId(nodeId)
                    .setInitiatorId(nodeId)
                    .build();
            forwardToken(message);
        } else {
            // Sono l'unico nodo, divento coordinatore automaticamente
            concludeElection();
        }
    }

    /**
     * Verifica se ci sono richieste in buffer da processare e avvia un'elezione se necessario
     */
    public synchronized void checkPendingRequests() {
        // IMPORTANTE: Verifica che non ci sia un'elezione in corso
        if (busy || isProducing || requestBuffer.isEmpty()) {
            return;
        }

        // Filtra le richieste già processate
        EnergyRequest nextRequest = null;

        while (!requestBuffer.isEmpty()) {
            nextRequest = requestBuffer.peekNext();
            if (nextRequest == null) {
                break;
            }

            if (processedRequests.contains(nextRequest.getTimestamp())) {
                // Richiesta già processata, rimuovila dal buffer
                requestBuffer.getNextRequest();
                System.out.printf("[BUFFER] Richiesta %d già processata, rimossa dal buffer%n",
                        nextRequest.getTimestamp());
                nextRequest = null;
            } else {
                // Trovata una richiesta valida
                break;
            }
        }

        if (nextRequest != null) {
            // Rimuovi dal buffer e avvia elezione
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

    /**
     * Gestisce la ricezione di un messaggio di elezione
     */
    public synchronized void handleElection(@NotNull PlantNetwork.ElectionMessage message) {
        String initiatorId = message.getInitiatorId();
        double offerMsg = message.getOffer();
        long timestamp = message.getTimestamp();
        String bestIdMsg = message.getBestId();

        // Caso 1: Sono già impegnato in un'elezione per un timestamp diverso
        if (busy && currentTimestamp != timestamp) {
            // Stampa il risultato solo se non già fatto
            if (timestamp != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, bestIdMsg, offerMsg, timestamp);
                lastPrintedTs = timestamp;
            }
            // Inoltra il messaggio senza modifiche
            forwardToken(message);
            return;
        }

        // Caso 2: Non sono impegnato in alcuna elezione
        if (!busy) {
            // Se sto producendo o ho già processato questa richiesta, non posso partecipare
            if (isProducing || processedRequests.contains(timestamp)) {
                // Stampa il risultato e inoltra
                if (timestamp != lastPrintedTs) {
                    System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                            nodeId, bestIdMsg, offerMsg, timestamp);
                    lastPrintedTs = timestamp;
                }
                // Marca come processata per evitare future partecipazioni
                processedRequests.add(timestamp);
                forwardToken(message);
                return;
            }

            // Partecipo all'elezione generando la mia offerta
            double myOffer = 0.1 + 0.8 * Math.random();
            initializeElection(myOffer, timestamp);
            processedRequests.add(timestamp); // IMPORTANTE: marca subito come processata
            System.out.printf("[%s] OFFERTA %.3f $/kWh per req %d%n",
                    nodeId, myOffer, timestamp);
        }

        // Caso 3: Partecipo all'elezione - confronto le offerte
        if (isBetterOffer(offerMsg, bestIdMsg)) {
            bestOffer = offerMsg;
            bestOfferId = bestIdMsg;
        }

        // Verifico se il token è tornato all'iniziatore
        if (initiatorId.equals(nodeId)) {
            // Sono l'iniziatore, concludo l'elezione
            concludeElection();
        } else {
            // Inoltra il token aggiornato
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

    /**
     * Verifica se un'offerta è migliore di quella attuale
     */
    private boolean isBetterOffer(double offer, String offerId) {
        // Prezzo più basso vince
        if (offer < bestOffer) return true;
        if (offer > bestOffer) return false;

        // A parità di prezzo, ID più alto vince
        return offerId.compareTo(bestOfferId) > 0;
    }

    /**
     * Inizializza lo stato per una nuova elezione
     */
    private void initializeElection(double offer, long timestamp) {
        busy = true;
        currentTimestamp = timestamp;
        myOffer = offer;
        bestOffer = offer;
        bestOfferId = nodeId;
        isCoordinator = false;
    }

    /**
     * Conclude l'elezione determinando il coordinatore
     */
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
        busy = false; // Elezione conclusa
        notifyAll();  // Risveglia il thread principale
    }

    /**
     * Pulisce lo stato dell'elezione
     */
    private void clearElectionState() {
        busy = false;
        isCoordinator = false;
        myOffer = 0.0;
        bestOffer = Double.MAX_VALUE;
        bestOfferId = null;
        currentTimestamp = -1;
    }

    /**
     * Inoltra il token al successore nell'anello
     */
    private void forwardToken(PlantNetwork.ElectionMessage message) {
        String next = topology.getSuccessor();
        // Usa un thread separato per evitare blocchi
        new Thread(() -> grpcClient.forwardElection(next, message)).start();
    }

    // Metodi di stato pubblici
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
        clearElectionState(); // Reset completo dello stato
        justFinishedProduction = true; // Segnala che ha appena finito la produzione
        notifyAll();
    }

    /**
     * Verifica se ci sono richieste da processare (buffer o coordinatore)
     */
    public synchronized boolean hasWorkToDo(long lastTimestamp) {
        // Se ho appena finito la produzione, segnala che c'è lavoro da fare
        // per permettere al main di controllare il buffer
        if (justFinishedProduction) {
            return true;
        }

        return (isCoordinatorFor(lastTimestamp) && !isProducing()) ||
                (!busy && !isProducing && !requestBuffer.isEmpty());
    }

    /**
     * Resetta il flag di produzione appena terminata
     */
    public synchronized void resetJustFinishedFlag() {
        justFinishedProduction = false;
    }

    /**
     * Pulisce le richieste processate vecchie per evitare memory leak
     */
    public synchronized void cleanupOldRequests(long currentTime) {
        // Rimuovi richieste più vecchie di 5 minuti
        processedRequests.removeIf(timestamp -> currentTime - timestamp > 300000);
    }
}