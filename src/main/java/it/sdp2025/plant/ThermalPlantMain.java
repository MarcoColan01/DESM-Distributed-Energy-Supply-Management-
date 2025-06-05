package it.sdp2025.plant;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import it.sdp2025.common.PlantInfo;
import it.sdp2025.simulator.SensorModule;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public final class ThermalPlantMain {

    public static void main(String[] args) throws Exception {
        final Random rnd = new Random();
        CliThread cli = new CliThread();
        cli.start();
        CliThread.Params p = cli.waitParams();

        PlantRegistration admin = new PlantRegistration(p.adminHost, p.adminPort);
        List<PlantInfo> peers = admin.register(p.id, p.grpcPort);
        System.out.println("[ADMIN] registrato – peer ricevuti: " + peers.size());

        TopologyManager topo = new TopologyManager(p.id);
        topo.initRing(
                peers.stream().map(PlantInfo::getId).collect(Collectors.toList())
        );
        System.out.printf("[%s] RING → %s%n", p.id, topo.getPlants());

        GrpcClient grpcClient = new GrpcClient();
        grpcClient.announceJoinAll(new PlantInfo(p.id, "localhost", p.grpcPort), peers);

        ElectionManager elect = new ElectionManager(p.id, topo, grpcClient);

        Server server = ServerBuilder.forPort(p.grpcPort)
                .addService(new GrpcServer(elect, topo, grpcClient))
                .build().start();
        System.out.printf("[gRPC] %s listening on %d%n", p.id, p.grpcPort);

        MqttEnergySubscriber sub = new MqttEnergySubscriber(
                p.mqttBroker,
                req -> {
                    double price = 0.1 + 0.8 * rnd.nextDouble();
                    System.out.printf("[%s] OFFERTA %.3f $/kWh per req %d%n",
                            p.id, price, req.getTimestamp());
                    elect.startElectionIfFree(price, req.getTimestamp());

                    /* risveglia il main thread in caso la condizione diventi vera */
                    synchronized (elect) { elect.notifyAll(); }
                });

        sub.connect();
        SensorModule.start(p.id, p.mqttBroker);

        /* ------------------------------------------------------------------
         *  Loop principale per gestire la produzione di energia
         * ------------------------------------------------------------------ */
        for (;;) {
            int qty;
            long timestamp;

            /* ATTENDO la condizione ------------------------ */
            synchronized (elect) {
                // Continua ad attendere finché non sono coordinatore E libero di produrre
                while (true) {
                    timestamp = sub.lastTimestamp();
                    if (timestamp != -1 &&
                            elect.isCoordinatorFor(timestamp) &&
                            !elect.isProducing()) {
                        break; // Condizione soddisfatta, posso produrre
                    }
                    elect.wait(); // Attendo che la condizione diventi vera
                }

                /* Qui la condizione è vera: sono coordinatore, libero di produrre */
                qty = sub.lastQuantity();
                System.out.printf("[%s] RICHIESTA %,d: PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.setProducing(true); // Imposto il flag di produzione
            }

            /* Produzione (fuori da synchronized per non bloccare altri thread) */
            try {
                Thread.sleep(qty); // 1 ms × kWh simulati
            } catch (InterruptedException e) {
                System.err.printf("[%s] Produzione interrotta per richiesta %d%n", p.id, timestamp);
                synchronized (elect) {
                    elect.productionFinished();
                }
                continue;
            }

            /* Fine produzione ------------------------------ */
            synchronized (elect) {
                System.out.printf("[%s] RICHIESTA %,d: FINITO PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.productionFinished(); // Reset flags e notifica
            }
        }
    }
}