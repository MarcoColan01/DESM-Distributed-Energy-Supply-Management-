package it.sdp2025.provider;

import com.google.gson.Gson;
import it.sdp2025.common.EnergyRequest;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class RenewableEnergyProvider {
    private static final String BROKER_ADDRESS = "tcp://localhost:1883";
    private static final String PUBLISH_TOPIC = "desm/energy";
    private final Random rnd = new Random();
    private final Gson gson = new Gson();
    private MqttClient client;
    private Timer timer;

    public void start() throws Exception{
        MqttClient client = new MqttClient(BROKER_ADDRESS, "ProviderPublisher");
        client.connect();
        System.out.println("[Provider] connected");
        while (!Thread.currentThread().isInterrupted()) {
            try {
                long timestamp = System.currentTimeMillis();
                int kwhQty = 5000 + rnd.nextInt(10001);
                String message = gson.toJson(new EnergyRequest(kwhQty, timestamp));
                client.publish(PUBLISH_TOPIC, new MqttMessage(message.getBytes()));
                System.out.printf("[Provider] request %d kWh%n", kwhQty);
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("[Provider] Publishing interrupted, stopping...");
                break;
            } catch (Exception e) {
                System.err.println("[Provider] Error publishing message: " + e.getMessage());
            }
        }
        System.out.println("[Provider] Publishing stopped");
    }

    public static void main(String[] args) throws Exception{
        new RenewableEnergyProvider().start();
    }
}
