
package it.sdp2025.thermalplant;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import it.sdp2025.proto.PlantRingGrpc;

public class GrpcServer {
    private final PlantConfig config;
    private final Server server;


    public GrpcServer(PlantConfig config, PlantRingGrpc.PlantRingImplBase serviceImpl) {
        this.config = config;
        this.server = ServerBuilder.forPort(config.getPort())
                .addService(serviceImpl).build();
    }

    public void start() throws Exception{
        server.start();
        System.out.println("Server avviato sulla porta " + config.getPort());
    }

    public void stop(){
        if(server != null) server.shutdown();
    }

    public void blockUntilShutdown() throws  InterruptedException{
        if(server != null) server.awaitTermination();
    }
}

