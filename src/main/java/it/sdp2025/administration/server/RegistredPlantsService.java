package it.sdp2025.administration.server;

import it.sdp2025.proto.plantInfo;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RegistredPlantsService {
    private final RegistredPlants registredPlants;
    public RegistredPlantsService(RegistredPlants registredPlants) {
        this.registredPlants = registredPlants;
    }
    public plantInfo addPlant(String id, int port) {
        return registredPlants.addPlant(id, port);
    }
    public List<plantInfo> getPlants()              {
        return registredPlants.getPlants();
    }
}
