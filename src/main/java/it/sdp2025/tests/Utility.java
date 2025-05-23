package it.sdp2025.tests;

import it.sdp2025.proto.Co2Average;
import it.sdp2025.proto.Co2AverageList;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;

public class Utility {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883";
        String topic  = "desm/plant/plant1/co2";
        String clientId = MqttClient.generateClientId();

        try {
            List<Co2Average> values = new ArrayList<>();
            long now = System.currentTimeMillis();
            System.out.println("Sending values at timestamps: ");
            for (int i = 0; i < 10; i++) {
                long ts = now - (10_000 - i * 1000); // Spaziati 1 sec
                System.out.printf("  • %.2f g at %d%n", 50 + i * 1.5, ts);

                values.add(Co2Average.newBuilder()
                        .setAvg(50 + i * 1.5)
                        .setTimestamp(ts)
                        .build());
            }

            Co2AverageList list = Co2AverageList.newBuilder()
                    .setPlantId("plant1")
                    .setCreationTimestamp(now)
                    .addAllAvgs(values)
                    .build();

            MqttClient client = new MqttClient(broker, clientId);
            MqttConnectOptions opts = new MqttConnectOptions();
            opts.setCleanSession(true);
            client.connect(opts);

            MqttMessage msg = new MqttMessage(list.toByteArray());
            msg.setQos(1);
            client.publish(topic, msg);
            System.out.println("✅ Published 10 CO₂ averages to " + topic);

            client.disconnect();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
