package it.sdp2025.simulator;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.*;


public final class SensorModule {
    private static final int WINDOW_SIZE = 8;
    private static final int OVERLAPPING_FACTOR = 4;
    private static final Gson gson = new Gson();
    private static PollutionSensor pollutionSensor;
    private static Thread computeThread;
    private static final List<Double> measurements = new ArrayList<>();
    private static final List<Double> averages = new ArrayList<>();
    private static Buffer buffer;
    private static String plantId;
    private static String brokerUrl;

    private SensorModule() {}
    public static void start(@NotNull String id, @NotNull String mqttBroker) {
        plantId = id;
        brokerUrl = mqttBroker;
        buffer = new SensorBuffer();
        pollutionSensor = new PollutionSensor(id, buffer);
        pollutionSensor.start();
        computeThread = new Thread(SensorModule::computeLoop, "SensorCompute-"+id);
        computeThread.setDaemon(true);
        computeThread.start();
    }

    private static void computeLoop() {
        long lastPublish = System.currentTimeMillis();
        while (true) {
            List<Measurement> chunk = buffer.readAllAndClean();
            for (Measurement m : chunk) {
                measurements.add(m.getValue());
                if (measurements.size() >= WINDOW_SIZE) {
                    double avg = measurements.stream()
                            .limit(WINDOW_SIZE)
                            .mapToDouble(Double::doubleValue)
                            .average().orElse(0.0);
                    averages.add(avg);
                    measurements.subList(0, OVERLAPPING_FACTOR).clear();
                }
            }
            long now = System.currentTimeMillis();
            if (now - lastPublish >= 10_000) {
                publishAverages(now);
                lastPublish = now;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {}
        }
    }

    private static void publishAverages(long ts) {
        if (averages.isEmpty()) return;

        try (MqttClient client = new MqttClient(brokerUrl,
                "SensorPub-"+plantId+"-"+ts)) {
            client.connect();
            //System.out.println(averages.size());
            for (Double avg : averages) {
                Map<String,Object> msg = Map.of(
                        "plantId",   plantId,
                        "avgValue",  avg,
                        "timestamp", ts
                );
                byte[] payload = gson.toJson(msg).getBytes(StandardCharsets.UTF_8);
                client.publish("desm/emissions",
                        new MqttMessage(payload));
            }
            client.disconnect();
        } catch (MqttException e) {
            e.printStackTrace();
        }
        averages.clear();
    }
}
