package it.sdp2025.thermalplant;

import java.util.Random;

public class ElectionManager {
    private enum State { NON_PARTICIPANT, PARTICIPANT }

    private final PlantConfig         cfg;
    private final PlantTopologyManager topology;
    private final GrpcClient     client;

    private volatile State            state         = State.NON_PARTICIPANT;
    private volatile String           coordinatorId = null;

    /* -------------------------------- prezzo simulato ----------- */
    private final Random rnd = new Random();
    private volatile double myPrice;

    public ElectionManager(PlantConfig cfg,
                           PlantTopologyManager topology,
                           GrpcClient client) {
        this.cfg       = cfg;
        this.topology  = topology;
        this.client    = client;
    }

    /* ============================================================ */
    /* === Trigger da MQTT: nuova richiesta energia =============== */
    public synchronized void onNewEnergyRequest(int kwh, long ts) {
        // Genera un prezzo (0.1 – 0.9) – placeholder
        myPrice = 0.1 + rnd.nextDouble() * 0.8;

        System.out.printf("[Election] Nuova richiesta %dkWh, prezzo=%.3f – parte elezione%n",
                kwh, myPrice);

        // Avvia elezione solo se NON_PARTICIPANT
        if (state == State.NON_PARTICIPANT) {
            state = State.PARTICIPANT;
            client.sendElectionToSuccessor(cfg.getId());
        }
    }

    /* ============================================================ */
    /* === Ricezione messaggi ****************************************/

    /** Riceve un messaggio Election(candidatoId) dal successore. */
    public synchronized void onElection(String candidateId) {

        int cmp = candidateId.compareTo(cfg.getId());

        if (cmp == 0) {
            // Sono di nuovo io ⇒ sono il coordinatore
            coordinatorId = cfg.getId();
            state = State.NON_PARTICIPANT;
            System.out.println("[Election] Eletto coordinatore: " + coordinatorId);
            client.sendElectedToSuccessor(coordinatorId);
            return;
        }

        if (cmp > 0) {              // candidatoId > myId
            client.sendElectionToSuccessor(candidateId);   // inoltra
            state = State.PARTICIPANT;
        } else if (cmp < 0 && state == State.NON_PARTICIPANT) {
            client.sendElectionToSuccessor(cfg.getId());   // propongo me
            state = State.PARTICIPANT;
        }
        // else (cmp < 0 && già participant) → scarto
    }

    /** Riceve un messaggio Elected(coordId) dal successore. */
    public synchronized void onElected(String coordId) {
        coordinatorId = coordId;
        state = State.NON_PARTICIPANT;

        if (!coordId.equals(cfg.getId())) {
            System.out.println("[Election] Coordinatore è " + coordId);
            client.sendElectedToSuccessor(coordId);   // inoltra
        } else {
            System.out.println("[Election] Io sono il coordinatore!");
            // TODO: qui l’impianto vincitore può iniziare la produzione
        }
    }

    /* ======= Getter utili ======================================= */
    public String  getCoordinatorId() { return coordinatorId; }
    public boolean isCoordinator()    { return cfg.getId().equals(coordinatorId); }
    public double  getMyPrice()       { return myPrice; }
}

