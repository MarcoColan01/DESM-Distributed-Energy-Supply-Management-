package it.sdp2025.administration.server;

import it.sdp2025.common.PlantInfo;
import it.sdp2025.common.PlantRegistration;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/plants")
public class PlantController {

    private final RegistredPlantsService service;

    public PlantController(RegistredPlantsService service) {
        this.service = service;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PlantInfo> addPlant(@RequestBody PlantRegistration req) {
        var added = service.addPlant(req.getId(), req.getPort());

        return service.getPlants().stream()
                .filter(p -> !p.getId().equals(added.getId()))
                .map(p -> new PlantInfo(p.getId(), p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }

    @GetMapping(produces = "application/json")
    public List<PlantInfo> getPlants() {
        return service.getPlants().stream()
                .map(p -> new PlantInfo(p.getId(), p.getHost(), p.getPort()))
                .collect(Collectors.toList());
    }
}
