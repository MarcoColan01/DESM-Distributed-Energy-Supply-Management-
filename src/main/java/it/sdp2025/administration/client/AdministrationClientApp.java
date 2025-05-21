package it.sdp2025.administration.client;

import it.sdp2025.proto.PlantList;

import java.util.Scanner;

public class AdministrationClientApp {
    public static void main(String[] args) {
        AdministrationClient client = new AdministrationClient();
        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("""
                === Administration CLI ===
                1) List plants
                2) Register new plant
                3) Global CO₂ average
                0) Exit
                """);

            switch (sc.nextLine().trim()) {
                case "1" -> {
                    var list = client.getPlants();
                    list.forEach(p ->
                            System.out.printf("• %s @ %s:%d%n",
                                    p.getId(), p.getHost(), p.getPort()));
                }
                case "2" -> {
                    System.out.print("Id   : ");  String id   = sc.nextLine().trim();
                    System.out.print("Port : "); int port     = Integer.parseInt(sc.nextLine().trim());

                    var peers = client.addPlant(id, port);
                    System.out.println("Registered. Current peers:");
                    peers.forEach(p ->
                            System.out.printf("• %s @ %s:%d%n",
                                    p.getId(), p.getHost(), p.getPort()));
                }
                case "3" -> {
                    System.out.println("""
        Intervallo per media CO₂:
        1) Inserisci manualmente FROM e TO (timestamp ms)
        2) Usa automaticamente gli ultimi 10 secondi
        """);
                    String mode = sc.nextLine().trim();

                    long from, to;

                    if (mode.equals("1")) {
                        System.out.print("from (ms epoch): "); from = Long.parseLong(sc.nextLine().trim());
                        System.out.print("to   (ms epoch): "); to   = Long.parseLong(sc.nextLine().trim());
                    } else {
                        to = System.currentTimeMillis();
                        from = to - 10_000;
                        System.out.printf(">> Calcolando media tra %d e %d (ultimi 10 secondi)%n", from, to);
                    }

                    double v = client.calculateAverage(from, to);
                    System.out.printf("Average CO₂ between [%d, %d] = %.2f g%n", from, to, v);
                }

                case "0" -> System.exit(0);
                default -> System.out.println("Invalid option");
            }
        }
    }
}
