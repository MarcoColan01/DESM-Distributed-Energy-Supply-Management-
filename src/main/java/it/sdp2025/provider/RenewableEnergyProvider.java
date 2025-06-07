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

    public void start() throws Exception {
        client = new MqttClient(BROKER_ADDRESS, "ProviderPublisher");
        client.connect();
        System.out.println("[Renewable Energy Provider] CONNESSO");
        timer = new Timer(true);
        timer.scheduleAtFixedRate(new TimerTask() {
                                      @Override
                                      public void run() {
                                          try {
                                              long timestamp = System.currentTimeMillis();
                                              int kwhQty = 5_000 + rnd.nextInt(10_001);
                                              String payload = gson.toJson(new EnergyRequest(kwhQty, timestamp));
                                              client.publish(PUBLISH_TOPIC, new MqttMessage(payload.getBytes()));
                                              System.out.printf("[Renewable Energy Provider] %,d: Pubblicata richiesta per %d kWh%n",
                                                      timestamp, kwhQty);
                                          } catch (Exception e) {}
                                      }
                                  }, 0L, 10_000L);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            timer.cancel();
            try {
                client.disconnect();
            } catch (Exception ignored) { }
            //System.out.println("[Renewable Energy Provider] stopped");
        }));

        synchronized (this) {
            this.wait();
        }
    }

    public static void main(String[] args) throws Exception {
        new RenewableEnergyProvider().start();
    }
}
