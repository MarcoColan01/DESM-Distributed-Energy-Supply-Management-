package it.sdp2025.provider;


import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;


import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


@Component
//@RequiredArgsConstructor
@Slf4j
public class MqttPublisher {
    private MqttAsyncClient client;

    @PostConstruct
    void init(){
        try{
            String clientId = "desm-provider-" + UUID.randomUUID();
            client = new MqttAsyncClient(MqttConstants.BROKER_URL, clientId);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            client.connect(connOpts).waitForCompletion();
            System.out.println("Connesso al broker " + MqttConstants.BROKER_URL);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    public void publish(byte[] payload){
        try{
            client.publish(MqttConstants.TOPIC, payload, MqttConstants.QOS, true).waitForCompletion();
            System.out.println("Pubblicato " + payload);
        }catch (MqttException e){
            e.printStackTrace();
        }
    }

    @PreDestroy
    void shutdown(){
        try{
            client.disconnect();
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
