package it.sdp2025.thermalplant;

public class PlantConfig {
    private final String id;
    private final String host;
    private final int port;
    private final String adminHost;
    private final int adminPort;

    public PlantConfig(String id, String host, int port, String adminHost, int adminPort) {
        this.id = id;
        this.host = host;
        this.port = port;
        this.adminHost = adminHost;
        this.adminPort = adminPort;
    }

    public String getId() { return id; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public String getAdminHost() { return adminHost; }
    public int getAdminPort() { return adminPort; }
}
