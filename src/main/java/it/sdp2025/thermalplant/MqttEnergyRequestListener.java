package it.sdp2025.thermalplant;

import org.eclipse.paho.client.mqttv3.*;

public class MqttEnergyRequestListener {
    private final String BROKER = "tcp://localhost:1883";
    private final String TOPIC = "desm/energy/request";
    private final ElectionManager election;
    private MqttClient client;

    public MqttEnergyRequestListener(ElectionManager election){
        this.election = election;
    }

    public void start(){
        try{
            client = new MqttClient(BROKER, MqttClient.generateClientId());
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.err.println("[MQTT] Connessione persa: " + throwable.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    try{
                        int kwh = Integer.parseInt(new String(message.getPayload()).trim());
                        long timestamp = System.currentTimeMillis();
                        System.out.printf("[MQTT] Richiesta %d kWh%n", kwh);
                        election.onNewEnergyRequest(kwh, timestamp);
                    }catch (NumberFormatException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    //
                }
            });
            client.connect(connOpts);
            client.subscribe(TOPIC, 1);
            System.out.println("[MQTT] Sottoscritto a " + TOPIC);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

}
