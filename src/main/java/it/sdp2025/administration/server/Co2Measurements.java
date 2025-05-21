package it.sdp2025.administration.server;

import it.sdp2025.administration.DebugCo2DTO;
import it.sdp2025.proto.Co2Average;
import org.springframework.stereotype.Component;

import java.util.*;


@Component
public class Co2Measurements {
    public class Data{
        private long timestamp;
        private double value;

        public Data(long timestamp, double value){
            this.timestamp = timestamp;
            this.value = value;
        }


        public long getTimestamp() {
            return timestamp;
        }

        public double getValue() {
            return value;
        }
    }
    private final Map<String, List<Data>> measurements = new HashMap<>();

    public synchronized void addMeasurements(String plantId, List<Co2Average> avgs){
        List<Data> data = measurements.computeIfAbsent(plantId, id -> new ArrayList<>());
        for (Co2Average avg: avgs)
            data.add(new Data(avg.getTimestamp(), avg.getAvg()));
    }

    public synchronized List<DebugCo2DTO> dumpAll() {
        List<DebugCo2DTO> result = new ArrayList<>();
        for (Map.Entry<String, List<Data>> entry : measurements.entrySet()) {
            for (Data d : entry.getValue()) {
                result.add(new DebugCo2DTO(entry.getKey(), d.timestamp, d.value));
            }
        }
        return result;
    }


    public synchronized double average(long from, long to){
        double sum = 0;
        long count = 0;

        List<String> ids = new ArrayList<>(measurements.keySet());
        Collections.sort(ids);

        for(String id: ids){
            List<Data> data = measurements.get(id);
            if(data == null) continue;

            for(Data singleData: data){
                if(singleData.timestamp >= from && singleData.timestamp <= to){
                    sum += singleData.value;
                    count++;
                }
            }
        }
        return count == 0 ? 0 : sum/count;
    }

}
