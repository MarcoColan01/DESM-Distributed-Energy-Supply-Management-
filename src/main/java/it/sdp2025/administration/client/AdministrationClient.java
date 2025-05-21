package it.sdp2025.administration.client;


import it.sdp2025.common.AvgResponse;
import it.sdp2025.common.PlantInfo;
import it.sdp2025.common.PlantRegistration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

public class AdministrationClient {
    private static final RestTemplate HTTP = new RestTemplate();
    private static final String BASE = "http://localhost:8080";

    /* ------------------ API ------------------ */
    public List<PlantInfo> getPlants() {
        ResponseEntity<PlantInfo[]> resp =
                HTTP.getForEntity(BASE + "/plants", PlantInfo[].class);
        return Arrays.asList(resp.getBody());
    }

    public List<PlantInfo> addPlant(String id, int port) {
        PlantRegistration dto = new PlantRegistration();
        dto.setId(id);
        dto.setPort(port);

        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<PlantInfo[]> resp =
                HTTP.postForEntity(BASE + "/plants",
                        new HttpEntity<>(dto, h),
                        PlantInfo[].class);
        return Arrays.asList(resp.getBody());
    }

    public double calculateAverage(long from, long to) {
        String url = BASE + "/stats/avg?from=" + from + "&to=" + to;
        ResponseEntity<AvgResponse> resp =
                HTTP.getForEntity(url, AvgResponse.class);
        return resp.getBody().getValue();
    }
}
