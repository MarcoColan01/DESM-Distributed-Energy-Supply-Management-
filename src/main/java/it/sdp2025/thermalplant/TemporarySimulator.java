package it.sdp2025.thermalplant;

import java.util.Random;

/**
 * Simulatore CO₂ provvisorio – genera un valore random ogni 2 s.
 * Puoi sostituirlo con il vero simulatore quando sarà disponibile.
 */
public class TemporarySimulator implements Runnable {

    private final String plantId;
    private final Random rnd = new Random();

    public TemporarySimulator(String plantId) {
        this.plantId = plantId;
    }

    public void start() {
        new Thread(this, "sensor-" + plantId).start();
    }

    @Override
    public void run() {
        while (true) {
            double value = 50 + rnd.nextDouble() * 30;  // 50–80 g
            long ts = System.currentTimeMillis();
            System.out.printf("[Sensor-%s] %.2f g @ %d%n", plantId, value, ts);

            // TODO: inviare a Co2Measurements o buffer
            try { Thread.sleep(2000); } catch (InterruptedException ignored) { }
        }
    }
}
