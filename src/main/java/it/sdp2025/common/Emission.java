package it.sdp2025.common;

import it.sdp2025.simulator.Measurement;
import org.jetbrains.annotations.NotNull;

public class Emission implements Comparable<Emission>{
    private String id;
    private String type;
    private double value;
    private long timestamp;

    public Emission(){}
    public Emission(String id, String type, double value, long timestamp) {
        this.value = value;
        this.timestamp = timestamp;
        this.id=id;
        this.type=type;
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getId() {
        return id;
    }

    public void setId(String type) {
        this.id = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }



    public String toString(){
        return value + " " + timestamp;
    }

    @Override
    public int compareTo(@NotNull Emission o) {
        Long thisTimestamp = timestamp;
        Long otherTimestamp = o.getTimestamp();
        return thisTimestamp.compareTo(otherTimestamp);
    }
}


