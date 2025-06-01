package it.sdp2025.plant;
import it.sdp2025.PlantNetwork;

import java.util.*;

public final class ElectionManager {

    /* ---------- stato locale ---------- */
    private enum LocalState { NON_PARTICIPANT, PARTICIPANT, BUSY }

    private volatile LocalState state = LocalState.NON_PARTICIPANT;
    private final String   myId;
    private final TopologyManager topo;
    private final GrpcClient grpc;

    /* offerta propria per la richiesta corrente */
    private double myOffer;
    private long   currentReqTs = -1L;   // timestamp della energy-request in corso
    private String coordinatorId = null; // risultato finale

    public ElectionManager(String myId, TopologyManager topo, GrpcClient grpc) {
        this.myId = myId;
        this.topo = topo;
        this.grpc = grpc;
    }

    /* ====================================================
       == 1.  TRIGGER ELEZIONE QUANDO ARRIVA L’ENERGY REQUEST
       ==================================================== */
    public synchronized void startElectionIfFree(double offer, long reqTs) {

        if (state == LocalState.BUSY) return;                 // occupato → non partecipo
        if (currentReqTs == reqTs)   return;                 // elezione già partita

        /* inizializzo contesto locale */
        myOffer       = offer;
        currentReqTs  = reqTs;
        state         = LocalState.PARTICIPANT;

        /* preparo e invio il primo messaggio */
        PlantNetwork.ElectionMsg msg = PlantNetwork.ElectionMsg.newBuilder()
                .setType(PlantNetwork.ElecType.ELECTION)
                .setCandidate(myId)
                .setPrice(offer)
                .setReqTs(reqTs)
                .build();

        grpc.forwardElection(topo.successor(), msg);
    }

    /* ====================================================
       == 2.  HANDLER INVOCAto DAL SERVIZIO gRPC
       ==================================================== */
    public void onElectionMsg(PlantNetwork.ElectionMsg msg) {
        synchronized (this) {

            /* Se il messaggio è di una richiesta già gestita, inoltra e basta */
            if (msg.getReqTs() != currentReqTs) {
                pass(msg);   // nessun cambio di stato
                return;
            }

            switch (msg.getType()) {
                case ELECTION -> handleElection(msg);
                case ELECTED  -> handleElected(msg);
            }
        }
    }

    /* ---------- gestione messaggi <ELECTION,…> ---------- */
    private void handleElection(PlantNetwork.ElectionMsg m) {

        /* Se sono il candidato trasmesso e il messaggio ha fatto il giro,
           significa che resto il migliore → divento coordinatore */
        if (m.getCandidate().equals(myId)) {
            state = LocalState.BUSY;
            coordinatorId = myId;

            PlantNetwork.ElectionMsg elected = PlantNetwork.ElectionMsg.newBuilder()
                    .setType(PlantNetwork.ElecType.ELECTED)
                    .setCandidate(myId)
                    .setPrice(m.getPrice())
                    .setReqTs(m.getReqTs())
                    .build();
            pass(elected);
            return;
        }

        /* Se sono BUSY non posso candidarmi: giro il messaggio */
        if (state == LocalState.BUSY) { pass(m); return; }

        /* Valuto se la mia offerta è migliore dell’attuale candidato */
        boolean betterPrice = myOffer < m.getPrice();
        boolean equalPriceHigherId = (Math.abs(myOffer - m.getPrice()) < 1e-9)
                && myId.compareTo(m.getCandidate()) > 0;

        if ((betterPrice || equalPriceHigherId) && state == LocalState.NON_PARTICIPANT) {
            // divento nuovo candidato
            state = LocalState.PARTICIPANT;
            PlantNetwork.ElectionMsg forward = m.toBuilder()
                    .setCandidate(myId)
                    .setPrice(myOffer)
                    .build();
            pass(forward);
        } else {
            // rimane il candidato corrente
            pass(m);
        }
    }

    /* ---------- gestione messaggi <ELECTED,…> ---------- */
    private void handleElected(PlantNetwork.ElectionMsg m) {
        coordinatorId = m.getCandidate();

        /* Tutti tornano NON_PARTICIPANT tranne il coordinatore che resta BUSY */
        if (!coordinatorId.equals(myId)) {
            state = LocalState.NON_PARTICIPANT;
        }
        pass(m);   // propaga finché torna al coordinatore (che lo scarterà)
    }

    /* ---------- util ---------- */
    private void pass(PlantNetwork.ElectionMsg msg) {
        grpc.forwardElection(topo.successor(), msg);
    }

    /* ====================================================
       == 3.  INFORMAZIONI PER IL CICLO DI PRODUZIONE
       ==================================================== */
    public boolean isCoordinatorFor(long reqTs) {
        return state == LocalState.BUSY && currentReqTs == reqTs && myId.equals(coordinatorId);
    }

    public synchronized void clearBusy() {
        state = LocalState.NON_PARTICIPANT;
        currentReqTs = -1L;
        coordinatorId = null;
    }
}
