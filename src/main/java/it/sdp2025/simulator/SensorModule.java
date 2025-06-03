package it.sdp2025.simulator;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.*;


public final class SensorModule {

    private static final int   WINDOW_SIZE = 8;
    private static final int   SLIDE       = 4;     // 50 % overlap
    private static final Gson  gson        = new Gson();

    private static PollutionSensor sensorThread;
    private static Thread          computeThread;
    private static final List<Double> window = new ArrayList<>();
    private static final List<Double> averagesToSend = new ArrayList<>();
    private static Buffer sharedBuffer;
    private static String plantId;
    private static String brokerUrl;

    private SensorModule() {}

    public static void start(String id, String mqttBroker) {
        plantId   = id;
        brokerUrl = mqttBroker;
        sharedBuffer = new SensorBuffer();

        sensorThread = new PollutionSensor(id, sharedBuffer);
        sensorThread.start();

        computeThread = new Thread(SensorModule::computeLoop, "SensorCompute-"+id);
        computeThread.setDaemon(true);
        computeThread.start();
    }

    private static void computeLoop() {
        long lastPublish = System.currentTimeMillis();

        while (true) {
            List<Measurement> chunk = sharedBuffer.readAllAndClean();
            for (Measurement m : chunk) {
                window.add(m.getValue());
                if (window.size() >= WINDOW_SIZE) {
                    double avg = window.stream()
                            .limit(WINDOW_SIZE)
                            .mapToDouble(Double::doubleValue)
                            .average().orElse(0.0);
                    averagesToSend.add(avg);
                    window.subList(0, SLIDE).clear();
                }
            }

            long now = System.currentTimeMillis();
            if (now - lastPublish >= 10_000) {
                publishAverages(now);
                lastPublish = now;
            }

            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
        }
    }

    private static void publishAverages(long ts) {
        if (averagesToSend.isEmpty()) return;

        try (MqttClient client = new MqttClient(brokerUrl,
                "SensorPub-"+plantId+"-"+ts)) {
            client.connect();
            System.out.println(averagesToSend.size());
            for (Double avg : averagesToSend) {
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
        averagesToSend.clear();
    }
}
