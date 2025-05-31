package it.sdp2025.simulator;

import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;

import java.util.*;
import java.util.concurrent.*;

public final class SensorModule {

    private static final int WINDOW_SIZE = 8;
    private static final int SLIDE = 4; // 50% overlap

    private static final List<Double> buffer = new ArrayList<>();
    private static final Gson gson = new Gson();
    private static PollutionSensor sensor;
    private static ScheduledExecutorService scheduler;

    public static void start(String plantId) {
        Buffer sharedBuffer = new SensorBuffer(); // tua implementazione concreta di Buffer

        sensor = new PollutionSensor(plantId, sharedBuffer);
        sensor.start();

        scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> publishAverages(plantId, sharedBuffer),
                10, 10, TimeUnit.SECONDS);
    }

    private static void publishAverages(String id, Buffer sharedBuffer) {
        List<Measurement> data = sharedBuffer.readAllAndClean();
        List<Double> values = new ArrayList<>();
        for (Measurement m : data) values.add(m.getValue());

        List<Double> averages = new ArrayList<>();
        for (int i = 0; i + WINDOW_SIZE <= values.size(); i += SLIDE) {
            List<Double> window = values.subList(i, i + WINDOW_SIZE);
            double avg = window.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            averages.add(avg);
        }

        long ts = System.currentTimeMillis();

        try {
            MqttClient client = new MqttClient("tcp://localhost:1883", "PlantSensor-" + id);
            client.connect();

            for (double avg : averages) {
                Map<String, Object> msg = new HashMap<>();
                msg.put("plantId", id);
                msg.put("avgValue", avg);
                msg.put("timestamp", ts);

                String payload = gson.toJson(msg);
                client.publish("desm/emissions", new MqttMessage(payload.getBytes()));
            }

            client.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (sensor != null) sensor.stopMeGently();
        if (scheduler != null) scheduler.shutdownNow();
    }
}
