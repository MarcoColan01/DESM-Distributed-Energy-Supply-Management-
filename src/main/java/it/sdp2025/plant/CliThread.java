package it.sdp2025.plant;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Scanner;

public final class CliThread extends Thread {

    public static final class Params {
        public final String id;
        public final int grpcPort;
        public final String adminHost;
        public final int adminPort;
        public final String mqttBroker;

        private Params(String id, int grpcPort, @NotNull String adminHost, int adminPort, @NotNull String mqttBroker){
            Objects.requireNonNull(id, "L'id della centrale non può essere null");
            if(id.isEmpty()) throw new IllegalArgumentException("L'id della centrale non può essere vuoto");
            if(grpcPort <= 0) throw new IllegalArgumentException("Il numero porta gRPC del client non può essere negativo o 0");
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

       try{
           System.out.print("Inserisci ID centrale: ");
           String id = in.nextLine().trim();

           System.out.print("Inserisci porta gRPC: ");
           int grpcPort = Integer.parseInt(in.nextLine().trim());

           params = new Params(id,
                   grpcPort,
                   "localhost",
                   8080,
                   "tcp://localhost:1883");
       }catch (Exception e){
           e.printStackTrace();
       }
    }

    public Params waitParams() throws InterruptedException {
        join();
        return params;
    }
}
