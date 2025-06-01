package it.sdp2025.simulator;
import org.eclipse.paho.client.mqttv3.*;
import com.google.gson.Gson;

import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 *  ▸ Avvia un PollutionSensor (thread proprietario)
 *  ▸ Gestisce finestra scorrevole di 8 misurazioni, overlap 50 %
 *  ▸ Ogni 10 s pubblica su topic   desm/emissions   la lista delle medie calcolate
 */
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

    private SensorModule() {} // no-instance

    public static void start(String id, String mqttBroker) {
        plantId   = id;
        brokerUrl = mqttBroker;
        sharedBuffer = new SensorBuffer();

        /* ➊ – avvia thread simulatore */
        sensorThread = new PollutionSensor(id, sharedBuffer);
        sensorThread.start();

        /* ➋ – thread di calcolo e invio */
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
                    /* rimuovi le prime 4 (overlap 50 %) */
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

    /* facoltativo – per chiudere il modulo */
    public static void stop() {
        if (sensorThread != null) sensorThread.stopMeGently();
        if (computeThread != null) computeThread.interrupt();
    }
}
