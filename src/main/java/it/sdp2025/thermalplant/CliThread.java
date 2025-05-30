package it.sdp2025.thermalplant;

import java.util.Scanner;

public class CliThread implements Runnable{
    private final ElectionManager election;
    private final PlantTopologyManager topology;

    public CliThread(ElectionManager election, PlantTopologyManager topology) {
        this.election = election;
        this.topology = topology;
    }

    public void start() {
        new Thread(this, "cli-thread").start();
    }

    @Override
    public void run() {
        Scanner in = new Scanner(System.in);
        while (true) {
            System.out.print(">> ");
            String line = in.nextLine().trim();

            switch (line) {
                case "quit", "exit" -> {
                    System.out.println("Terminating process...");
                    System.exit(0);
                }
                case "status" -> {
                    System.out.println("Coordinator: " + election.getCoordinatorId());
                    System.out.println("My price:    " + election.getMyPrice());
                }
                case "election" -> {
                    election.onNewEnergyRequest(10000, System.currentTimeMillis());
                }
                case "show-topo" -> {
                    topology.getPeers().forEach(p ->
                            System.out.printf("- %s @ %s:%d%n", p.getId(), p.getHost(), p.getPort()));
                    System.out.println("My successor: " + (topology.getSuccessor() != null
                            ? topology.getSuccessor().getId()
                            : "(none)"));
                }
                default -> System.out.println("Comandi: status, election, show-topo, quit");
            }
        }
    }
}
