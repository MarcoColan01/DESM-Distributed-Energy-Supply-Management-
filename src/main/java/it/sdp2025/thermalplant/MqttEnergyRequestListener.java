package it.sdp2025.thermalplant;

import it.sdp2025.proto.EnergyRequest;
import org.eclipse.paho.client.mqttv3.*;

public class MqttEnergyRequestListener {

    private static final String BROKER = "tcp://localhost:1883";
    private static final String TOPIC  = "desm/energy/request";
    private final ElectionManager election;
    private final MqttClient client;

    public MqttEnergyRequestListener(ElectionManager election) throws MqttException {
        this.election = election;
        this.client   = new MqttClient(BROKER, MqttClient.generateClientId());
    }

    public void start() throws MqttException {
        client.connect();
        client.subscribe(TOPIC, 1, (topic, message) -> {
            try {
                /* 1. decodifica payload protobuf */
                EnergyRequest req = EnergyRequest.parseFrom(message.getPayload());

                int  kwh = req.getKwh();
                long ts  = req.getTimestamp();
                System.out.printf("[MQTT] Richiesta %s: %d kWh%n",
                        req.getRequestId(), kwh);

                /* 2. innesca l’elezione */
                election.onNewEnergyRequest(kwh, ts);

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        System.out.println("[MQTT] Sottoscritto a " + TOPIC);
    }
}
