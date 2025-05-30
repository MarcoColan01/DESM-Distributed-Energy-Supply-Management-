package it.sdp2025.server;

import it.sdp2025.common.PlantInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PlantNetworkService {
    private final Map<String, PlantInfo> plants = new HashMap<>();

    public synchronized List<PlantInfo> listOfPlants(){
        return new ArrayList<>(plants.values());
    }

    public synchronized List<PlantInfo> registerPlant(PlantInfo plant) throws IllegalStateException{
        if(plants.containsKey(plant.getId())) throw new IllegalStateException("Duplicated id " + plant.getId());
        plants.put(plant.getId(), plant);
        List<PlantInfo> others = new ArrayList<>(plants.values());
        others.remove(plant);
        return others;
    }
}
