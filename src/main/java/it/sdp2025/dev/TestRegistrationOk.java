package it.sdp2025.dev;

import it.sdp2025.common.PlantRegistrationRequest;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class TestRegistrationOk {
    public static void main(String[] args) {
        RestTemplate rt = new RestTemplate();

        // REGISTRA P1 (OK)
        var req = new PlantRegistrationRequest("P1", 5001);
        rt.postForEntity("http://localhost:8080/plants/register", req, Void.class);
        System.out.println("► Registrazione P1 inviata (atteso 200 OK)");

        // DUPLICATO P1 (CONFLICT)
        var req2 = new PlantRegistrationRequest("P1", 9999);
        try {
            rt.postForEntity("http://localhost:8080/plants/register", req2, Void.class);
            System.out.println("⚠ ERRORE: duplicato non rilevato!");
        } catch (HttpClientErrorException.Conflict e) {
            System.out.println("► Registrazione P1 FALLITA (atteso 409 CONFLICT)");
        }

        // REGISTRA P2 (OK)
        var req3 = new PlantRegistrationRequest("P2", 5002);
        rt.postForEntity("http://localhost:8080/plants/register", req3, Void.class);
        System.out.println("► Registrazione P2 inviata (atteso 200 OK)");
    }
}
