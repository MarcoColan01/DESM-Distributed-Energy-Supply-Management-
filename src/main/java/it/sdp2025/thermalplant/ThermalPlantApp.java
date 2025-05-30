package it.sdp2025.thermalplant;

import it.sdp2025.administration.client.AdministrationClient;
import it.sdp2025.common.PlantInfo;

import java.util.List;

public class ThermalPlantApp {
    public static void main(String[] args) throws Exception {
        if (args.length != 5) {
            System.out.println("Inserire TUTTI i parametri richiesti");
            System.exit(1);
        }

        String id = args[0];
        String host = args[1];
        int port = Integer.parseInt(args[2]);
        String adminHost = args[3];
        int adminPort = Integer.parseInt(args[4]);

        PlantConfig config = new PlantConfig(id, host, port, adminHost, adminPort);
//        AdministrationClient adminClient = new AdministrationClient();
//        List<PlantInfo> otherPlants = adminClient.addPlant(id, port);
        ThermalPlant plant = new ThermalPlant(config);
        plant.start();
    }
}
