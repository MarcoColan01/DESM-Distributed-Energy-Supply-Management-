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
        System.out.println("[SERVER] Centrale registrata – peer ricevuti: " + peers.size());

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
        System.out.printf("[gRPC] %s in ascolto su %d%n", p.id, p.grpcPort);

        MqttEnergySubscriber sub = new MqttEnergySubscriber(
                p.mqttBroker,
                elect::handleEnergyRequest);

        sub.connect();
        SensorModule.start(p.id, p.mqttBroker);

        int bufferCheckCounter = 0;

        for (;;) {
            int qty;
            long timestamp;

            synchronized (elect) {
                while (!elect.hasWorkToDo(sub.lastTimestamp())) {
                    elect.wait();
                }
                if (elect.finishedProduction()) {
                    elect.resetJustFinishedFlag();
                    bufferCheckCounter = 3;
                }
                if (bufferCheckCounter > 0) {
                    elect.checkPendingRequests();
                    bufferCheckCounter--;
                    if (elect.getBusy()) {
                        continue;
                    }
                }

                if (elect.isCoordinatorFor(sub.lastTimestamp()) && !elect.isProducing()) {
                    qty = sub.lastQuantity();
                    timestamp = sub.lastTimestamp();
                } else {
                    elect.checkPendingRequests();
                    continue;
                }

                System.out.printf("[%s] RICHIESTA %,d: PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.setProducing(true);
            }

            Thread.sleep(qty);

            synchronized (elect) {
                System.out.printf("[%s] RICHIESTA %,d: FINITO PRODUZIONE di %d kWh%n",
                        p.id, timestamp, qty);
                elect.productionFinished();
            }
        }
    }
}