package it.sdp2025.plant;

import java.util.Scanner;

/** Thread bloccante che interroga lo stdin,
 *  poi consegna i 5 parametri a chi lo aspetta. */
public final class CliThread extends Thread {

    public static final class Params {
        public final String id;
        public final int grpcPort;
        public final String adminHost;
        public final int adminPort;
        public final String mqttBroker;
        private Params(String id, int grpcPort, String adminHost, int adminPort, String mqttBroker){
            this.id = id;
            this.grpcPort = grpcPort;
            this.adminPort = adminPort;
            this.mqttBroker = mqttBroker;
            this.adminHost = adminHost;
        }
    }

    private Params params;

    public void run() {
        Scanner in = new Scanner(System.in);

        System.out.print("Inserisci ID centrale              > ");
        String id = in.nextLine().trim();

        System.out.print("Inserisci porta gRPC                > ");
        int grpcPort = Integer.parseInt(in.nextLine().trim());

        params = new Params(id,
                grpcPort,
                "localhost",
                8080,
                "tcp://localhost:1883");
    }

    /** bloccante finché l’utente non ha finito */
    public Params waitParams() throws InterruptedException {
        join();
        return params;
    }
}
