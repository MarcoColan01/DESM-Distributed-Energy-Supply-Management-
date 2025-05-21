package it.sdp2025.administration.server;

import it.sdp2025.proto.Co2Average;
import it.sdp2025.proto.Co2AverageList;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class Co2MqttListener {
    private static final String BROKER_URL = "tcp://localhost:1883";
    private static final String SUB_TOPIC = "desm/plant/+/co2";
    private final Co2Measurements measurements;
    private MqttClient client;

    public Co2MqttListener(Co2Measurements measurements){
        this.measurements = measurements;
        start();
    }

    private void start(){
        try{
            String clientId = MqttClient.generateClientId();
            client = new MqttClient(BROKER_URL, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable throwable) {
                    System.out.println(clientId + " connection lost: " + throwable.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) throws Exception {
                    System.out.println("Server: received message on topic " + topic + ", size: " + message.getPayload().length + " bytes");
                    try{
                        Co2AverageList avgList = Co2AverageList.parseFrom(message.getPayload());
                        measurements.addMeasurements(avgList.getPlantId(), avgList.getAvgsList());
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    //Qui non serve
                }
            });

            System.out.println(clientId + " connecting to " + BROKER_URL);
            client.connect(connOpts);
            client.subscribe(SUB_TOPIC, 1);
            System.out.println(clientId + " subscribed to " + SUB_TOPIC);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }
}
