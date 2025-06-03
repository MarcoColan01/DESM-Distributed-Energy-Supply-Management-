package it.sdp2025.common;

import org.jetbrains.annotations.NotNull;

public class PlantInfo {
    private String id;
    private String host;
    private int grpcPort;

    public PlantInfo(){}
    public PlantInfo(@NotNull String id, @NotNull String host, int grpcPort){
        this.id = id;
        this.host = host;
        this.grpcPort = grpcPort;
    }

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getGrpcPort() {
        return grpcPort;
    }
}
