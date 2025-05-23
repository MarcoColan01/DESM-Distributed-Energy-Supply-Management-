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

    public String getId() {
        return id;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getAdminHost() {
        return adminHost;
    }

    public int getAdminPort() {
        return adminPort;
    }

    public static PlantConfig fromArgs(String[] args) {
        String id = null;
        String host = "localhost";
        int port = -1;
        String adminHost = "localhost";
        int adminPort = 8080;

        for (String a : args) {
            if (a.startsWith("--id="))           id        = a.substring(5);
            else if (a.startsWith("--host="))    host      = a.substring(7);
            else if (a.startsWith("--port="))    port      = Integer.parseInt(a.substring(7));
            else if (a.startsWith("--adminHost=")) adminHost = a.substring(12);
            else if (a.startsWith("--adminPort=")) adminPort = Integer.parseInt(a.substring(12));
        }

        if (id == null || port < 0)
            throw new IllegalArgumentException("Usage: --id=<id> --port=<grpcPort> [--host=localhost] [--adminHost=localhost] [--adminPort=8080]");

        return new PlantConfig(id, host, port, adminHost, adminPort);
    }

    @Override
    public String toString() {
        return "PlantConfig[" + id + " @" + host + ":" + port +
                " | admin=" + adminHost + ":" + adminPort + "]";
    }
}

