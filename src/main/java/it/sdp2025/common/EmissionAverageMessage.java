package it.sdp2025.common;

import org.jetbrains.annotations.NotNull;

public class EmissionAverageMessage {
    private String plantId;
    private double avgValue;
    private long timestamp;

    public EmissionAverageMessage(){};
    public EmissionAverageMessage(@NotNull String plantId, double avgValue, long timestamp){
        this.avgValue = avgValue;
        this.plantId = plantId;
        this.timestamp = timestamp;
    }

    public String getPlantId() {
        return plantId;
    }

    public double getAvgValue() {
        return avgValue;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
