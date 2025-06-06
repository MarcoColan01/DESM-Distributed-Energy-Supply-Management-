package it.sdp2025.plant;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import it.sdp2025.common.PlantInfo;
import it.sdp2025.simulator.SensorModule;

import java.util.List;
import java.util.stream.Collectors;

public final class ThermalPlantMain {

    public static void main(String[] args) throws Exception {
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
                    // Gestisce la richiesta tramite l'ElectionManager (con buffer)
                    elect.handleEnergyRequest(req);
                });

        sub.connect();
        SensorModule.start(p.id, p.mqttBroker);

        // Contatore per dare priorità alle richieste buffer dopo la produzione
        int bufferCheckCounter = 0;

        // Loop principale modificato per gestire sia richieste correnti che buffer
        for (;;) {
            int qty;
            long timestamp;

            synchronized (elect) {
                // Aspetta finché non ho lavoro da fare
                while (!elect.hasWorkToDo(sub.lastTimestamp())) {
                    elect.wait();
                }

                // Se ho appena finito la produzione, controlla prima il buffer
                if (elect.finishedProduction()) {
                    elect.resetJustFinishedFlag();
                    bufferCheckCounter = 3; // Dai priorità al buffer per i prossimi 3 cicli
                }

                // Se devo dare priorità al buffer, controlla prima quello
                if (bufferCheckCounter > 0) {
                    elect.checkPendingRequests();
                    bufferCheckCounter--;

                    // Se ho iniziato un'elezione dal buffer, continua
                    if (elect.getBusy()) {
                        continue;
                    }
                }

                // Verifico se sono coordinatore per la richiesta corrente
                if (elect.isCoordinatorFor(sub.lastTimestamp()) && !elect.isProducing()) {
                    // Processo la richiesta corrente
                    qty = sub.lastQuantity();
                    timestamp = sub.lastTimestamp();
                } else {
                    // Controllo se ci sono richieste nel buffer da processare
                    elect.checkPendingRequests();
                    continue; // Torna al wait per aspettare di diventare coordinatore
                }

                System.out.printf("[%s] RICHIESTA %,d: PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.setProducing(true);
            }

            // Produzione (fuori da synchronized)
            Thread.sleep(qty);

            // Fine produzione
            synchronized (elect) {
                System.out.printf("[%s] RICHIESTA %,d: FINITO PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.productionFinished();
            }

            // Cleanup periodico delle richieste vecchie (ogni 100 iterazioni)
            if (timestamp % 100 == 0) {
                elect.cleanupOldRequests(System.currentTimeMillis());
            }
        }
    }
}