package it.sdp2025.administration.server;

import it.sdp2025.proto.EnergyRequest;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;
import java.util.UUID;

public class EnergyRequestPublisher {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";
    private static final String PUB_TOPIC = "desm/energy/request";
    private final MqttClient client;
    private final Random rnd = new Random();

    public EnergyRequestPublisher() throws MqttException{
        this.client = new MqttClient(BROKER_ADDRESS, MqttClient.generateClientId());
    }

    public void start() throws MqttException{
        client.connect();
        Thread t = new Thread(this::loop, "energy-publisher-"+client.getClientId());
        t.setDaemon(false);
        t.start();
    }

    private void loop(){
        while(true){
            try{
                EnergyRequest request = EnergyRequest.newBuilder()
                        .setRequestId(UUID.randomUUID().toString())
                        .setKwh(5000+ rnd.nextInt(10000+1))
                        .setTimestamp(System.currentTimeMillis()).build();
                MqttMessage message = new MqttMessage(request.toByteArray());
                message.setQos(1);
                client.publish(PUB_TOPIC, message);
                System.out.printf("Published %s (%d kWh)%n", request.getRequestId(), request.getKwh());
                Thread.sleep(10001);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }
}
