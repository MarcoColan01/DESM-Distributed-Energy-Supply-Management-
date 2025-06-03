package it.sdp2025.common;

import org.jetbrains.annotations.NotNull;

public class PlantRegistrationRequest {
    private String id;
    private int grpcPort;

    public PlantRegistrationRequest(){};
    public PlantRegistrationRequest(@NotNull String id, int grpcPort){
        this.id = id;
        this.grpcPort = grpcPort;
    }

    public String getId() {
        return id;
    }

    public int getGrpcPort() {
        return grpcPort;
    }
}
