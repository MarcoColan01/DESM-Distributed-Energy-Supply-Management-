package it.sdp2025.tests;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class Test {
    public static void main(String[] args) throws Exception {
        String broker = "tcp://localhost:1883";
        String topic = "desm/energy/request";

        MqttClient client = new MqttClient(broker, MqttClient.generateClientId());
        client.connect();

        client.setCallback(new MqttCallback() {
            public void connectionLost(Throwable cause) {
                System.out.println("Lost connection");
            }

            public void messageArrived(String topic, MqttMessage message) {
                System.out.println("Received message on " + topic + ": " + message.getPayload().toString() + " bytes");
            }

            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        client.subscribe(topic, 1);
        System.out.println("Subscribed to: " + topic);
    }
}
