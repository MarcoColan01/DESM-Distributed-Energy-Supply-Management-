package it.sdp2025.plant;

import java.util.Scanner;

/** Thread bloccante che interroga lo stdin,
 *  poi consegna i 5 parametri a chi lo aspetta. */
public final class CliThread extends Thread {

    public static final class Params {
        public final String id;
        public final int    grpcPort;
        public final String adminHost;
        public final int    adminPort;
        public final String mqttBroker;
        private Params(String i,int gp,String ah,int ap,String mb){
            id=i;grpcPort=gp;adminHost=ah;adminPort=ap;mqttBroker=mb;
        }
    }

    private Params params;

    public void run() {
        Scanner in = new Scanner(System.in);

        System.out.print("Inserisci ID centrale              > ");
        String id = in.nextLine().trim();

        System.out.print("Inserisci porta gRPC                > ");
        int grpcPort = Integer.parseInt(in.nextLine().trim());

        System.out.print("Host Administration Server [localhost] > ");
        String aHost = in.nextLine().trim();
        if (aHost.isEmpty()) aHost = "localhost";

        System.out.print("Porta Administration Server [8080]  > ");
        String aPortStr = in.nextLine().trim();
        int aPort = aPortStr.isEmpty() ? 8080 : Integer.parseInt(aPortStr);

        System.out.print("Broker MQTT [tcp://localhost:1883]  > ");
        String broker = in.nextLine().trim();
        if (broker.isEmpty()) broker = "tcp://localhost:1883";

        params = new Params(id, grpcPort, aHost, aPort, broker);
    }

    /** bloccante finché l’utente non ha finito */
    public Params waitParams() throws InterruptedException {
        join();
        return params;
    }
}
