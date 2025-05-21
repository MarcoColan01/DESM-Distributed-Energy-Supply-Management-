package it.sdp2025.common;

public class PlantInfo {
    private String id;
    private String host;
    private int port;

    public PlantInfo() {}
    public PlantInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }

    public void setId(String id) { this.id = id; }
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }
}
