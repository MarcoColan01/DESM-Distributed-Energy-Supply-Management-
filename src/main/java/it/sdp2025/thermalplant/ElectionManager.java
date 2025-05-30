package it.sdp2025.thermalplant;

import java.util.Random;

public class ElectionManager {

    private enum State { NON_PARTICIPANT, PARTICIPANT }

    private final PlantConfig          cfg;
    private final PlantTopologyManager topology;
    private final GrpcClient           client;

    private volatile State  state         = State.NON_PARTICIPANT;
    private volatile String coordinatorId = null;

    private final Random rnd  = new Random();
    private volatile double myPrice;

    /* ----------------------------------------------------------- */
    public ElectionManager(PlantConfig cfg,
                           PlantTopologyManager topology,
                           GrpcClient client) {
        this.cfg      = cfg;
        this.topology = topology;
        this.client   = client;
    }

    /* === trigger da MQTT ======================================= */
    public synchronized void onNewEnergyRequest(int kwh, long ts) {
        myPrice = 0.1 + rnd.nextDouble() * 0.8;
        System.out.printf("[Election] Nuova richiesta %dkWh, prezzo=%.3f – parte elezione%n",
                kwh, myPrice);

        if (state == State.NON_PARTICIPANT) {
            state = State.PARTICIPANT;
            client.sendElection(cfg.getId(), myPrice);
        }
    }

    public synchronized void onElection(String candId, double price) {
        System.out.printf("[Election] ricevo <id=%s, price=%.3f>  (myPrice=%.3f)%n",
                candId, price, myPrice);
        /* 1. se il messaggio torna al candidato → coordinatore */
        if (candId.equals(cfg.getId())) {
            coordinatorId = cfg.getId();
            state = State.NON_PARTICIPANT;
            System.out.println("[Election] Io sono il coordinatore!");
            client.sendElected(coordinatorId);
            return;
        }

        /* 2. verifica se il candidato in ingresso è migliore */
        boolean better =
                (price < myPrice) ||
                        (price == myPrice && candId.compareTo(cfg.getId()) > 0);

        if (better) {
            /* inoltra il candidato migliore */
            client.sendElection(candId, price);
        } else {
            /* candidato peggiore */
            if (state == State.NON_PARTICIPANT) {
                /* propongo me stesso (solo se NON_PARTICIPANT) */
                client.sendElection(cfg.getId(), myPrice);
                state = State.PARTICIPANT;
            } else {
            /* già participant → inoltro comunque il messaggio peggiore
               per non interrompere l’anello                     */
                client.sendElection(candId, price);
            }
        }
        /* se sono arrivato qui, rimango participant */
        state = State.PARTICIPANT;
    }


    public synchronized void onElected(String coordId) {
        coordinatorId = coordId;
        state = State.NON_PARTICIPANT;

        if (!coordId.equals(cfg.getId())) {
            System.out.println("[Election] Coordinatore è " + coordId);
            client.sendElected(coordId);          // inoltra
        } else {
            System.out.println("[Election] Io sono il coordinatore!");
            // qui potresti avviare la produzione
        }
    }

    /* --- getter ------------------------------------------------ */
    public String  getCoordinatorId() { return coordinatorId; }
    public boolean isCoordinator()    { return cfg.getId().equals(coordinatorId); }
    public double  getMyPrice()       { return myPrice; }
}
