package it.sdp2025.plant;

import it.sdp2025.PlantNetwork;
import java.util.Random;

public class ElectionManager {

    private enum State { NON_PARTICIPANT, PARTICIPANT, BUSY }

    private volatile State state = State.NON_PARTICIPANT;

    private final String myId;
    private final TopologyManager topology;
    private final GrpcClient grpcClient;

    private double  offer;             // mia offerta corrente
    private long    timestamp = -1L;   // ts della richiesta gestita
    private String  coordinatorId = null;

    private final Random rnd = new Random();

    /* -------------------------------------------------------------------------------- */

    public ElectionManager(String myId, TopologyManager topology, GrpcClient grpcClient) {
        this.myId       = myId;
        this.topology   = topology;
        this.grpcClient = grpcClient;
    }

    /* ========================= 1. avvio locale dell’elezione ======================== */

    /** chiamato dal subscriber MQTT quando arriva una NUOVA richiesta energia */
    public synchronized void startElectionIfFree(double myOffer, long reqTs) {

        if (state == State.BUSY)      return;   // sono già coordinatore di qualcos’altro
        if (timestamp == reqTs)       return;   // stesso timestamp già in gestione

        offer     = myOffer;
        timestamp = reqTs;
        state     = State.PARTICIPANT;

        PlantNetwork.ElectionMsg msg = PlantNetwork.ElectionMsg.newBuilder()
                .setType     (PlantNetwork.ElecType.ELECTION)
                .setCandidate(myId)
                .setPrice    (offer)
                .setReqTs(timestamp)
                .build();

        pass(msg);        // → al successore
        System.out.printf("[%s] OFFERTA %.3f $ per req %d%n", myId, offer, timestamp);
    }

    /* ========================= 2. RPC ricevuto dal ring ============================ */

    /** chiamato da GrpcServer.forwardElection() */
    public synchronized void onElection(PlantNetwork.ElectionMsg msg) {

        switch (msg.getType()) {
            case ELECTION  -> handleElection(msg);
            case ELECTED   -> handleElected (msg);
        }
    }

    /* -------------------------------------------------------------------------------- */

    private void handleElection(PlantNetwork.ElectionMsg m) {

        /* ── primo incontro con questo timestamp → genero la mia offerta ── */
        if (timestamp != m.getReqTs()) {
            timestamp = m.getReqTs();
            offer     = 0.1 + 0.8 * rnd.nextDouble();
            state     = State.NON_PARTICIPANT;
            System.out.printf("[%s] OFFERTA %.3f $ per req %d%n", myId, offer, timestamp);
        }

        /* ── decide se sostituire il candidato corrente ── */
        boolean betterPrice      = offer <  m.getPrice();
        boolean samePriceHigher  = Math.abs(offer - m.getPrice()) < 1e-9
                && myId.compareTo(m.getCandidate()) > 0;

        boolean iReplace = (betterPrice || samePriceHigher)
                && state == State.NON_PARTICIPANT;

        PlantNetwork.ElectionMsg fwd = m.toBuilder()
                .setCandidate(iReplace ? myId   : m.getCandidate())
                .setPrice    (iReplace ? offer  : m.getPrice())
                .build();

        if (fwd.getCandidate().equals(myId)) {          // il messaggio è tornato a me → vinco
            state          = State.BUSY;
            coordinatorId  = myId;

            System.out.printf("[%s] *** COORDINATORE (%.3f) req %d ***%n",
                    myId, offer, timestamp);

            PlantNetwork.ElectionMsg elected = PlantNetwork.ElectionMsg.newBuilder()
                    .setType     (PlantNetwork.ElecType.ELECTED)
                    .setCandidate(myId)
                    .setReqTs(timestamp)
                    .build();

            pass(elected);    // notifica a tutto il ring
        } else {
            pass(fwd);        // continua il giro
        }
    }

    private void handleElected(PlantNetwork.ElectionMsg m) {
        coordinatorId = m.getCandidate();
        state         = coordinatorId.equals(myId) ? State.BUSY
                : State.NON_PARTICIPANT;
        if (!coordinatorId.equals(myId)) pass(m);   // propaga
    }

    /* -------------------------------------------------------------------------------- */

    private void pass(PlantNetwork.ElectionMsg msg) {
        grpcClient.forwardElection(topology.getSuccessor(), msg);
    }

    /* ========================= API di utilità ======================================= */

    public boolean isCoordinatorFor(long reqTs) {
        return state == State.BUSY && timestamp == reqTs && myId.equals(coordinatorId);
    }

    public synchronized void clearBusy() {
        state = State.NON_PARTICIPANT;
        timestamp = -1L;
        coordinatorId = null;
    }
}
