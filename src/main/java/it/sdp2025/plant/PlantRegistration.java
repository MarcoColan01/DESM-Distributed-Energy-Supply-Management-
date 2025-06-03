package it.sdp2025.plant;
import it.sdp2025.common.PlantInfo;
import it.sdp2025.common.PlantRegistrationRequest;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public final class PlantRegistration {
    private final RestTemplate http = new RestTemplate();
    private final String base;

    public PlantRegistration(@NotNull String host, int port) {
        this.base = "http://" + host + ":" + port;
    }

    public List<PlantInfo> register(@NotNull String id, int grpcPort) {
        PlantRegistrationRequest req = new PlantRegistrationRequest(id, grpcPort);
        ResponseEntity<PlantInfo[]> resp = http.postForEntity(
                base + "/plants/register", req, PlantInfo[].class);

        if (resp.getStatusCode().is2xxSuccessful()) {
            return Arrays.asList(Objects.requireNonNull(resp.getBody()));
        } else {
            throw new IllegalStateException("Registrazione fallita: " + resp.getStatusCode());
        }
    }
}
