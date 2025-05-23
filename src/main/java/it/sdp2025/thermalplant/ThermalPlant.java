package it.sdp2025.thermalplant;

import java.util.Arrays;

public class ThermalPlant {

    private final PlantConfig config;

    private final PlantTopologyManager      topology;
    private final ElectionManager           election;
    private final GrpcServer server;
    private final GrpcClient client;
    private final MqttEnergyRequestListener mqtt;
    private final TemporarySimulator           sensor;
    private final CliThread                 cli;

    public ThermalPlant(PlantConfig config) {
        this.config       = config;
        this.topology  = new PlantTopologyManager(config);
        this.client = new GrpcClient(config, topology);
        this.election  = new ElectionManager(config, topology, client);
        this.server = new GrpcServer(config, new PlantRingServiceImpl(election, topology));
        this.mqtt      = new MqttEnergyRequestListener(election);
        this.sensor    = new TemporarySimulator(config.getId());
        this.cli       = new CliThread(election, topology);
    }

    public void start(String[] peerArgs) throws Exception {
        server.start();

        Arrays.stream(peerArgs)
                .filter(p -> p.startsWith("--peer="))
                .map(p -> p.substring(7))
                .forEach(this::addPeerFromArg);

        topology.buildRing();

        client.connectToSuccessor();
        client.helloToPeers();

        mqtt.start();
        sensor.start();
        cli.start();

        Thread.currentThread().join();
    }

    private void addPeerFromArg(String s) {
        String[] parts = s.split(":");
        if (parts.length != 3) return;
        topology.addPeer(parts[0], parts[1], Integer.parseInt(parts[2]));
    }
}
