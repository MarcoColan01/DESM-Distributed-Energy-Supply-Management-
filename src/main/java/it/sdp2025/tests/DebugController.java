package it.sdp2025.tests;

import it.sdp2025.administration.server.Co2Measurements;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;

@RestController
public class DebugController {
    private final Co2Measurements measurements;

    public DebugController(Co2Measurements measurements) {
        this.measurements = measurements;
    }

    @GetMapping("/debug")
    public List<DebugCo2DTO> debugAll() {
        return measurements.dumpAll();
    }
}
