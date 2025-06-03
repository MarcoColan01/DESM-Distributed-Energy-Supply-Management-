package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;

/**
 * Gestisce l’elezione ad anello (Chang & Roberts) fra le centrali
 * e lo stato locale della centrale stessa.
 *
 *   • busy          – true se la centrale sta elaborando un’ELEZIONE
 *   • isCoordinator – true solo per la centrale vincitrice
 *   • isProducing   – true mentre la centrale sta fisicamente producendo
 *
 * Zero dipendenze da java.util.concurrent: i token gRPC sono inoltrati
 * con un semplice new Thread().
 */
public class ElectionManager {

    /* ------------------------- campi -------------------------------- */

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

    /* ---------------------- costruttore ----------------------------- */

    public ElectionManager(String nodeId,
                           TopologyManager topology,
                           GrpcClient grpcClient) {

        this.nodeId     = nodeId;
        this.topology   = topology;
        this.grpcClient = grpcClient;
        clearBusy();            // inizializza tutti i flag
        isProducing = false;
    }

    /* ================================================================ */
    /* ===========  API INVOCATE DAL THREAD PRINCIPALE  =============== */
    /* ================================================================ */

    /**
     * Avvia un’elezione se la centrale è libera.
     * @return true se l’elezione parte, false se la centrale era impegnata.
     */
    public synchronized boolean startElectionIfFree(double offer, long timestamp) {

        if (busy || isProducing) return false;   // già occupata

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
            forwardToken(msg);                   // parte l’anello
        } else {
            // anello di un solo nodo: eleggiti subito
            becomeCoordinator();
        }
        return true;
    }

    /** @return true se *questa* centrale è coordinatore per quel timestamp. */
    public synchronized boolean isCoordinatorFor(long timestamp) {
        return isCoordinator && currentTimestamp == timestamp;
    }

    public synchronized boolean isProducing()               { return isProducing; }
    public synchronized void    setProducing(boolean flag)   { isProducing = flag; }

    /** Chiamare quando la centrale ha finito di produrre. */
    public synchronized void productionFinished() {
        isProducing = false;
        clearBusy();                 // pronta per altre elezioni
    }

    /* ================================================================ */
    /* ==============  HANDLER CHIAMATO DAL SERVER gRPC  ============== */
    /* ================================================================ */

    /** Token d’elezione ricevuto da un altro nodo dell’anello. */
    public synchronized void handleElection(PlantNetwork.ElectionMsg msg) {

        String starterId      = msg.getInitiatorId();
        double offer          = msg.getOffer();
        long   ts             = msg.getTimestamp();
        String bestIdReceived = msg.getBestId();

        /* 1) Se sto già gestendo un’elezione *diversa*, passo il token. */
        if (busy && currentTimestamp != ts) {

    /* Stampa "coordinatore eletto" una sola volta per questo timestamp,
       anche se la centrale è impegnata a produrre.                    */
            if (ts != lastPrintedTs) {
                System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                        nodeId, msg.getBestId(), msg.getOffer(), ts);
                lastPrintedTs = ts;
            }

            forwardToken(msg);   // passa comunque il token
            return;
        }


        /* 2) Aggiorna lo stato per *questa* elezione. */
        busy             = true;
        currentTimestamp = ts;

        if (bestOfferId == null ||
                offer < bestOffer ||
                (offer == bestOffer && bestIdReceived.compareTo(bestOfferId) > 0)) {

            bestOffer   = offer;
            bestOfferId = bestIdReceived;
        }

        /* 3) Se il token ha completato il giro… */
        if (starterId.equals(nodeId)) {

            becomeCoordinator();   // stampa e setta isCoordinator

            if (!isCoordinator) {  // lo starter NON vincitore si libera
                clearBusy();
            }
            /* Il coordinatore resta busy finché produce.  */
        } else {
            /* 4) Nodo intermedio: inoltra e libera subito lo stato. */
            PlantNetwork.ElectionMsg newMsg = PlantNetwork.ElectionMsg
                    .newBuilder()
                    .setOffer(bestOffer)
                    .setTimestamp(ts)
                    .setBestId(bestOfferId)
                    .setInitiatorId(starterId)
                    .build();
            forwardToken(newMsg);
            clearBusy();          // <--  IMPORTANTE: non resta “bloccato”
        }
    }

    /* ================================================================ */
    /* ==================  METODI DI UTILITÀ PRIVATI  ================= */
    /* ================================================================ */

    /** Inoltra il token al successore in un nuovo thread. */
    private void forwardToken(PlantNetwork.ElectionMsg msg) {
        String next = topology.getSuccessor();
        new Thread(() -> grpcClient.forwardElection(next, msg)).start();
    }

    private void becomeCoordinator() {
        isCoordinator = nodeId.equals(bestOfferId);

        if (isCoordinator) {
            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n",
                    nodeId, bestOffer, currentTimestamp);
        } else {
            System.out.printf("[%s] Coordinatore eletto: %s (%.3f) per richiesta %d%n",
                    nodeId, bestOfferId, bestOffer, currentTimestamp);
        }
        lastPrintedTs = currentTimestamp;   // evita di ristampare
    }


    /** Riporta la centrale allo stato “idle” (nessuna elezione in corso). */
    private void clearBusy() {
        busy             = false;
        isCoordinator    = false;
        bestOffer        = Double.MAX_VALUE;
        bestOfferId      = null;
        currentTimestamp = -1;
    }

    /* Nessuna risorsa long-living da chiudere. */
    public void shutdown() {}
}
