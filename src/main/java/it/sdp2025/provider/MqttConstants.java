package it.sdp2025.provider;

public class MqttConstants {
    private MqttConstants() {}   // utility class

    public static final String BROKER_URL = "tcp://localhost:1883";
    public static final String TOPIC      = "desm/energy/request";
    public static final int    QOS        = 1;            // at-least-once
    public static final long   PUBLISH_INTERVAL_MS = 10_000L; // 10 s
}
