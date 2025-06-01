package it.sdp2025.plant;

import com.google.gson.Gson;
import it.sdp2025.common.EnergyRequest;
import org.eclipse.paho.client.mqttv3.*;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

/**
 *  • Si connette al broker MQTT (QoS 1) e si sottoscrive al topic "desm/energy".
 *  • A ogni EnergyRequest ricevuta:
 *        – memorizza l'ultimo messaggio
 *        – chiama onRequest.accept(req) per innescare ElectionManager
 *  • Nessuna implementazione diretta di MqttCallback: usiamo setCallback(…)
 */
public final class MqttEnergySubscriber {

    private final String brokerUrl;
    private final Consumer<EnergyRequest> onRequest;

    private MqttClient client;
    private final Gson gson = new Gson();

    /* ultimi valori utili al ciclo di produzione */
    private volatile EnergyRequest last;

    public MqttEnergySubscriber(String brokerUrl, Consumer<EnergyRequest> onRequest) {
        this.brokerUrl = brokerUrl;
        this.onRequest = onRequest;
    }

    /** Connessione e sottoscrizione */
    public void connect() throws MqttException {
        client = new MqttClient(brokerUrl, "EnergySub-" + System.nanoTime());
        client.setCallback(new MqttCallback() {
            public void connectionLost(Throwable cause) { cause.printStackTrace(); }

            public void messageArrived(String topic, MqttMessage msg) {
                String json = new String(msg.getPayload(), StandardCharsets.UTF_8);
                EnergyRequest req = gson.fromJson(json, EnergyRequest.class);
                last = req;               // salva per il loop di produzione
                onRequest.accept(req);    // delega all'ElectionManager
            }

            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        client.connect();
        client.subscribe("desm/energy", 1);
        System.out.println("[MQTT] sottoscritto desm/energy");
    }

    /* ----- getter per ThermalPlantMain ----- */
    public long lastTimestamp() { return last == null ? -1 : last.getTimestamp(); }
    public int  lastQuantity()  { return last == null ?  0 : last.getKwhQty(); }
}
