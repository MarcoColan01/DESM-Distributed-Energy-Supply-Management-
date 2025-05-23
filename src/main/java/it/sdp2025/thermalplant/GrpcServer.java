
package it.sdp2025.thermalplant;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class GrpcServer {
    private final PlantConfig config;
    private Server server;

    public GrpcServer(PlantConfig config, PlantRingServiceImpl serviceImpl){
        this.config = config;
        this.server = ServerBuilder.forPort(config.getPort()).addService(serviceImpl).build();
    }

    public void start() throws Exception {
        server.start();
        System.out.println("[gRPC] Server avviato sulla porta " + config.getPort());
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
    }

    public void stop(){
        if(server != null) server.shutdown();
    }
}

