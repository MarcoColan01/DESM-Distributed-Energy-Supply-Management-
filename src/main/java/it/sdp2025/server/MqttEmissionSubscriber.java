package it.sdp2025.server;

import com.google.gson.Gson;
import it.sdp2025.common.EmissionAverageMessage;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;

@Component
public class MqttEmissionSubscriber {
    private static final String BROKER = "tcp://localhost:1883";
    private static final String TOPIC = "desm/emissions";

    @Autowired
    private EmissionStoreService emissionStore;
    private final Gson gson = new Gson();
    private MqttClient client;

    @PostConstruct
    public void init() throws MqttException {
        this.client = new MqttClient(BROKER, "AdminServerSubscriber");
        client.connect();
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void messageArrived(String s, MqttMessage mqttMessage) throws Exception {
                String json = new String(mqttMessage.getPayload(), StandardCharsets.UTF_8);
                EmissionAverageMessage message = gson.fromJson(json, EmissionAverageMessage.class);
                emissionStore.addEmission(message);
                System.out.printf("[MQTT] avg %.2f g da %s%n", message.getAvgValue(), message.getPlantId());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                //
            }
        });

        client.subscribe(TOPIC, 1);
        System.out.println("[MQTT] Subscribed to " + TOPIC);
    }

}
