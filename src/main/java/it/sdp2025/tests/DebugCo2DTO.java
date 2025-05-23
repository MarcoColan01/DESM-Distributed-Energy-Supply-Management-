package it.sdp2025.tests;

public class DebugCo2DTO {
    private String plantId;
    private long timestamp;
    private double value;

    public DebugCo2DTO(String plantId, long timestamp, double value) {
        this.plantId = plantId;
        this.timestamp = timestamp;
        this.value = value;
    }

    public String getPlantId() {
        return plantId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public double getValue() {
        return value;
    }
}