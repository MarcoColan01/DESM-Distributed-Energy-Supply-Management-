package it.sdp2025.plant;
import it.sdp2025.common.PlantInfo;
import it.sdp2025.common.PlantRegistrationRequest;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public final class PlantRegistration {

    private final RestTemplate http = new RestTemplate();
    private final String base;

    public PlantRegistration(String host, int port) {
        this.base = "http://" + host + ":" + port;
    }

    /** ritorna la lista dei peer già presenti */
    public List<PlantInfo> register(String id, int grpcPort) {
        PlantRegistrationRequest req = new PlantRegistrationRequest(id, grpcPort);
        ResponseEntity<PlantInfo[]> resp = http.postForEntity(
                base + "/plants/register", req, PlantInfo[].class);

        if (resp.getStatusCode().is2xxSuccessful()) {
            return Arrays.asList(resp.getBody());
        } else {
            throw new IllegalStateException("Registrazione fallita: " + resp.getStatusCode());
        }
    }
}
