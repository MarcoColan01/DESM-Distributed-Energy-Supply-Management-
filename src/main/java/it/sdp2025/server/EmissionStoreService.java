package it.sdp2025.server;

import it.sdp2025.common.EmissionAverageMessage;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class EmissionStoreService {
    private final Map<String, Object> locks = new HashMap<>();
    private final Map<String, NavigableMap<Long, Double>> store = new HashMap<>();

    public void addEmission(EmissionAverageMessage message){
        synchronized (getLock(message.getPlantId())){
            store.computeIfAbsent(message.getPlantId(), k -> new TreeMap<>())
                    .put(message.getTimestamp(), message.getAvgValue());
        }
    }

    public OptionalDouble calculateAverage(long t1, long t2){
        double sum = 0;
        int n = 0;
        for(String id: store.keySet()){
            synchronized (getLock(id)){
                NavigableMap<Long, Double> app = store.get(id);
                if(app == null) continue;
                for(Double value: app.subMap(t1, true, t2, true).values()){
                    sum += value;
                    n++;
                }
            }
        }
        return n == 0 ? OptionalDouble.empty() : OptionalDouble.of(sum / n);
    }
    private Object getLock(String id){
        synchronized (locks){ return locks.computeIfAbsent(id, k -> new Object());}
    }
}
