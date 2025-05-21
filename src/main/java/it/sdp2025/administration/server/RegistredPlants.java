package it.sdp2025.administration.server;

import it.sdp2025.proto.plantInfo;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistredPlants {
    private static final String HOST_NAME = "localhost";
    private final Map<String, plantInfo> plants = new HashMap<>();

    public synchronized plantInfo addPlant(String id, int port){
        if(plants.containsKey(id)) throw new IllegalStateException("L'impianto con id " + id + " è già presente");

        plantInfo info = plantInfo.newBuilder()
                .setId(id)
                .setHost(HOST_NAME)
                .setPort(port).build();
        plants.put(id, info);
        return info;
    }

    public List<plantInfo> getPlants(){
        return List.copyOf(plants.values());
    }

    public static void main(String[] args) {

    }
}
