package it.sdp2025.administration.server;

import org.springframework.stereotype.Service;

@Service
public class Co2MeasurementsService {
    private final Co2Measurements measurements;
    public Co2MeasurementsService(Co2Measurements measurements){
        this.measurements = measurements;
    }
    public double calculateAverage(long from, long to){
        return measurements.average(from, to);
    }
}
