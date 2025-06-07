package it.sdp2025.client;

import it.sdp2025.common.Emission;
import it.sdp2025.common.PlantInfo;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import java.util.Arrays;
import java.util.Scanner;

public class AdministrationClient {
    private static final String SERVER_ADDRESS = "http://localhost:8080";
    private final RestTemplate http = new RestTemplate();
    private final Scanner in = new Scanner(System.in);

    private void loop() {
        while (true) {
            System.out.println("""
                    == Administration Client - Scegliere un comando ==
                    1) Lista degli impianti in rete
                    2) Media CO2 intervallata
                    3) Tutte le misurazioni ottenute
                    0) Exit
                    """);
            switch (in.nextLine()) {
                case "1" -> listOfPlants();
                case "2" -> calculateAverage();
                case "3" -> listOfMeasurements();
                case "0" -> System.exit(0);
                default -> System.out.println("Comando non riconosciuto");
            }
        }
    }

    private void listOfPlants(){
        ResponseEntity<PlantInfo[]> response = http.getForEntity(SERVER_ADDRESS + "/plants", PlantInfo[].class);
        Arrays.stream(response.getBody()).forEach(plant ->
                System.out.printf("• %s @ %s:%d%n", plant.getId(), plant.getHost(), plant.getGrpcPort()));
    }

    private void listOfMeasurements(){
        ResponseEntity<Emission[]> response = http.getForEntity(SERVER_ADDRESS + "/emissions/getAll", Emission[].class);
        Arrays.stream(response.getBody()).forEach(measurement ->
                System.out.printf("At %,d: @%s %.2f%n", measurement.getTimestamp(), measurement.getId(), measurement.getValue()));
    }

    private void calculateAverage(){
        System.out.print("Inserisci timestamp di inizio: ");
        long t1 = Long.parseLong(in.nextLine());
        System.out.print("Inserisci timestamp di fine: ");
        long t2 = Long.parseLong(in.nextLine());
        if(t1 >= t2){
            System.out.println("L'inizio dell'intervallo non può essere successivo alla fine");
            return;
        }
        String url = SERVER_ADDRESS + "/emissions/average?t1=" + t1 + "&t2=" + t2;
        ResponseEntity<Double> r = http.getForEntity(url, Double.class);
        if (r.getStatusCode().is2xxSuccessful())
            System.out.printf("Media CO2 = %.2f g%n", r.getBody());
        else System.out.printf("Nessun dato nell'intervallo %.2f - %.2f", t1, t2);
    }

    public static void main(String[] args) {
        new AdministrationClient().loop();
    }
}
