package it.sdp2025.plant;

import com.google.gson.Gson;
import it.sdp2025.common.EnergyRequest;
import org.eclipse.paho.client.mqttv3.*;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public final class MqttEnergySubscriber {
    private final String brokerUrl;
    private final Consumer<EnergyRequest> onRequest;
    private MqttClient client;
    private final Gson gson = new Gson();
    private volatile EnergyRequest last;

    public MqttEnergySubscriber(@NotNull String brokerUrl, @NotNull Consumer<EnergyRequest> onRequest) {
        this.brokerUrl = brokerUrl;
        this.onRequest = onRequest;
    }

    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, "EnergySub-" + System.nanoTime());
        client.setCallback(new MqttCallback() {
            public void connectionLost(Throwable cause) {
                cause.printStackTrace();
            }

            public void messageArrived(String topic, MqttMessage message) {
                String json = new String(message.getPayload(), StandardCharsets.UTF_8);
                EnergyRequest req = gson.fromJson(json, EnergyRequest.class);
                last = req;
                onRequest.accept(req);
            }

            public void deliveryComplete(IMqttDeliveryToken token) {}
        });
        client.connect();
        client.subscribe("desm/energy", 1);
        System.out.println("[MQTT Richieste energia] sottoscritto desm/energy");
    }

    public long lastTimestamp() {
        return last == null ? -1 : last.getTimestamp();
    }
    public int  lastQuantity()  {
        return last == null ?  0 : last.getKwhQty();
    }
}
