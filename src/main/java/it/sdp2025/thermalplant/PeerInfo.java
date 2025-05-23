package it.sdp2025.thermalplant;

public final class PeerInfo {
    private final String id;
    private final String host;
    private final int port;

    public PeerInfo(String id, String host, int port) {
        this.id = id;
        this.host = host;
        this.port = port;
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
}
