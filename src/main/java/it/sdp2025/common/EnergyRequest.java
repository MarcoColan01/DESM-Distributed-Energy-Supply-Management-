package it.sdp2025.common;

public final class EnergyRequest {
    private final int kwhQty;
    private final long timestamp;

    public EnergyRequest(int kwhQty, long timestamp) {
        this.kwhQty = kwhQty;
        this.timestamp = timestamp;
    }
}
