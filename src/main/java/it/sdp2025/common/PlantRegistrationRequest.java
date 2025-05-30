package it.sdp2025.common;

public class PlantRegistrationRequest {
    private String id;
    private int grpcPort;

    public PlantRegistrationRequest(){};
    public PlantRegistrationRequest(String id, int grpcPort){
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
