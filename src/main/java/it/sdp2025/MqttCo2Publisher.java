package it.sdp2025;

import it.sdp2025.proto.Co2Average;
import it.sdp2025.proto.Co2AverageList;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.*;

public class MqttCo2Publisher {
    public static void main(String[] args) {
        String broker = "tcp://localhost:1883";
        String topic  = "desm/plant/plant1/co2";
        String clientId = MqttClient.generateClientId();

        try {
            long now = System.currentTimeMillis();
            System.out.println("📤 Inviando dati CO₂ a: " + topic);
            System.out.println("🕒 Ora attuale: " + now);

            List<Co2Average> values = new ArrayList<>();
            for (int i = 0; i < 5; i++) {
                long ts = now - (5000 - i * 1000);  // da now - 5000 ms a now
                double val = 50.0 + i * 2.5;
                System.out.printf("   • %.2f g @ %d%n", val, ts);
                values.add(Co2Average.newBuilder()
                        .setAvg(val)
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
            System.out.println("✅ Dati pubblicati.");

            client.disconnect();
            client.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
