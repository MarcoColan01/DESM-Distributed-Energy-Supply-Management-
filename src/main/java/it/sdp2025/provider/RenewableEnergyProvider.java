package it.sdp2025.provider;

import com.google.gson.Gson;
import it.sdp2025.common.EnergyRequest;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;

public class RenewableEnergyProvider {
    private static final String BROKER = "tcp://localhost:1883";
    private static final String TOPIC = "desm/energy";
    private final Random rnd = new Random();
    private final Gson gson = new Gson();

    public void start() throws Exception{
        MqttClient client = new MqttClient(BROKER, "ProviderPublisher");
        client.connect();
        System.out.println("[Provider] connected");
        while(true){
            long timestamp = System.currentTimeMillis();
            int kwhQty = 5000 + rnd.nextInt(10001);
            String message = gson.toJson(new EnergyRequest(kwhQty, timestamp));
            client.publish(TOPIC, new MqttMessage(message.getBytes()));
            System.out.printf("[Provider] request %d kWh%n", kwhQty);
            Thread.sleep(5000);
        }
    }

    public static void main(String[] args) throws Exception{
        new RenewableEnergyProvider().start();
    }
}
