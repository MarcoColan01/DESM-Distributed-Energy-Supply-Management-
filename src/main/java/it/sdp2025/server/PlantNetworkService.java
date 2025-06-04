package it.sdp2025.server;

import it.sdp2025.common.PlantInfo;
import org.jetbrains.annotations.NotNull;
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

    public synchronized List<PlantInfo> registerPlant(@NotNull PlantInfo plant) throws IllegalStateException{
        if(plants.containsKey(plant.getId())) throw new IllegalStateException("Impianto con ID " + plant.getId()
                + " già presente in rete");
        plants.put(plant.getId(), plant);
        List<PlantInfo> registredPlants = new ArrayList<>(plants.values());
        registredPlants.remove(plant);
        return registredPlants;
    }
}
