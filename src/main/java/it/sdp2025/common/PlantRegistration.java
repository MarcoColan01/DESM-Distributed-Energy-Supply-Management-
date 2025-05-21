package it.sdp2025.common;

public class PlantRegistration {
    private String id;
    private int port;

    public PlantRegistration(){}

    public PlantRegistration(String id, int port){
        this.id = id;
        this.port = port;
    }

    public String getId() { return id; }
    public int getPort() { return port; }

    public void setId(String id) { this.id = id; }
    public void setPort(int port) { this.port = port; }
}
