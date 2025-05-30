package it.sdp2025.client;

import it.sdp2025.common.PlantInfo;
import org.apache.coyote.Response;
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
                    == Administration Client Menu ==
                    1) Lista degli impianti
                    2) Media CO2 intervallata
                    0) Exit
                    """);
            switch (in.nextLine()) {
                case "1" -> listOfPlants();
                case "2" -> calculateAverage();
                case "0" -> System.exit(0);
            }
        }
    }

    private void listOfPlants(){
        ResponseEntity<PlantInfo[]> response = http.getForEntity(SERVER_ADDRESS + "/plants", PlantInfo[].class);
        Arrays.stream(response.getBody()).forEach(plant ->
                System.out.printf("• %s @ %s:%d%n", plant.getId(), plant.getHost(), plant.getGrpcPort()));
    }

    private void calculateAverage(){
        System.out.print("t1 (ms): "); long t1 = Long.parseLong(in.nextLine());
        System.out.print("t2 (ms): "); long t2 = Long.parseLong(in.nextLine());
        String url = SERVER_ADDRESS + "/emissions/average?t1=" + t1 + "&t2=" + t2;
        ResponseEntity<Double> r = http.getForEntity(url, Double.class);
        if (r.getStatusCode().is2xxSuccessful())
            System.out.printf("Media CO₂ = %.2f g%n", r.getBody());
        else System.out.printf("Nessun dato nell'intervallo %.2f - %.2f", t1, t2);
    }

    public static void main(String[] args) {
        new AdministrationClient().loop();
    }
}
