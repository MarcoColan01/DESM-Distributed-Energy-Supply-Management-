package it.sdp2025.administration.server;

import it.sdp2025.common.AvgResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/stats")
public class Co2Controller {
    private final Co2MeasurementsService measurements;
    public Co2Controller(Co2MeasurementsService measurements){
        this.measurements = measurements;
    }

    @GetMapping(value = "/avg", produces = "application/json")
    public AvgResponse average(@RequestParam long from, @RequestParam long to) {
        double v = measurements.calculateAverage(from, to);
        return new AvgResponse(v);
    }
}
