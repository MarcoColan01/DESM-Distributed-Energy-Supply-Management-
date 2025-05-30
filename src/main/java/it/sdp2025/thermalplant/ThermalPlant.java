package it.sdp2025.thermalplant;

import it.sdp2025.administration.client.AdministrationClient;
import it.sdp2025.common.PlantInfo;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Processo di una centrale termoelettrica.
 * <p>
 * Sequenza di bootstrap:
 * <ol>
 *     <li>registrazione presso il server di amministrazione</li>
 *     <li>avvio del server gRPC di questo impianto</li>
 *     <li>popolamento della topologia e chiusura dell'anello</li>
 *     <li>presentazione agli altri peer (HELLO)</li>
 *     <li>sottoscrizione al topic MQTT delle richieste e avvio del simulatore del sensore</li>
 *     <li>schedulazione dell'invio periodico delle medie di CO₂ al server</li>
 * </ol>
 * <p>
 * N.B. – non si usano classi di {@code java.util.concurrent} per rispettare i vincoli
 *       di progetto; la periodizzazione avviene con {@link java.util.Timer}.
 */
public class ThermalPlant {

    /* ==================== componenti =============================== */
    private final PlantConfig              cfg;
    private final GrpcClient               grpcClient;
    private final PlantTopologyManager     topology;
    private final ElectionManager          election;
    private final GrpcServer               grpcServer;
    private final MqttEnergyRequestListener mqttSub;
    private final MqttPollutionPublisher   mqttPub;
    private final PollutionSensorSimulator sensor;
    private final CliThread                cli;
    private final AdministrationClient     admin = new AdministrationClient();

    /* timer per il push periodico delle medie                         */
    private final Timer sensorTimer = new Timer(true);

    /* ---------------------------------------------------------------- */
    public ThermalPlant(PlantConfig cfg) throws Exception {

        this.cfg = cfg;

        /* 1. costruiamo subito il client gRPC – l'ElectionManager        */
        /*    verrà settato più avanti a causa delle dipendenze circolari */
        this.grpcClient = new GrpcClient(cfg, /* election = */ null);

        /* 2. topologia connessa al client (per riconnessioni dinamiche)  */
        this.topology   = new PlantTopologyManager(cfg, grpcClient);

        /* 3. ElectionManager completo delle altre due componenti         */
        this.election   = new ElectionManager(cfg, topology, grpcClient);
        grpcClient.setElectionManager(election);

        /* 4. server gRPC                                                 */
        this.grpcServer = new GrpcServer(
                cfg,
                new PlantRingServiceImpl(election, topology)
        );

        /* 5. MQTT                                                        */
        this.mqttSub = new MqttEnergyRequestListener(election);
        this.mqttPub = new MqttPollutionPublisher();

        /* 6. simulatore sensore + CLI                                    */
        this.sensor  = new PollutionSensorSimulator(cfg.getId());
        this.cli     = new CliThread(election, topology);

        /* 7. hook di terminazione                                        */
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("[Shutdown] chiusura canali gRPC|MQTT …");
            sensorTimer.cancel();
            mqttSub.shutdown();
            grpcClient.shutdown();
        }));
    }

    /* ================================================================= */
    public void start() throws Exception {

        /* 1. registrazione al server di amministrazione ---------------- */
        System.out.printf("[Bootstrap] registrazione impianto %s …%n", cfg.getId());
        List<PlantInfo> peers = admin.addPlant(cfg.getId(), cfg.getPort());

        /* 2. avvio del server gRPC (deve ascoltare prima dei possibili   */
        /*    messaggi HELLO)                                             */
        grpcServer.start();

        /* 3. popolamento topologia & costruzione anello ---------------- */
        /*    NB: aggiungiamo SEMPRE noi stessi prima di eventuali altri  */
        topology.addPeer(cfg.getId(), cfg.getHost(), cfg.getPort());
        for (PlantInfo p : peers) {
            if (!p.getId().equals(cfg.getId()))
                topology.addPeer(p.getId(), p.getHost(), p.getPort());
        }
        topology.buildRing();              // connette già il GrpcClient

        /* 4. presentazione a tutti i peer ------------------------------ */
        grpcClient.helloToPeers();

        /* 5. MQTT subscription e simulatore sensore -------------------- */
        mqttSub.start();
        sensor.start();

        /* 6. schedulazione invio medie CO₂ ----------------------------- */
        sensorTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                var averages = sensor.readAllAndClean();
                if (!averages.isEmpty()) {
                    mqttPub.publishAverages(cfg.getId(), System.currentTimeMillis(), averages);
                }
            }
        }, 10_000L, 10_000L);

        /* 7. CLI ------------------------------------------------------- */
        cli.start();

        /* 8. hold main thread                                           */
        Thread.currentThread().join();
    }
}
