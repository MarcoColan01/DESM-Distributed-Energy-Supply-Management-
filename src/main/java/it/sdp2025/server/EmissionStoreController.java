package it.sdp2025.server;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.OptionalDouble;

@RestController
@RequestMapping("/emissions")
public class EmissionStoreController {
    @Autowired EmissionStoreService storeService;

    @GetMapping("/average")
    public ResponseEntity<Double> getAverage(@RequestParam long t1, @RequestParam long t2) {
        OptionalDouble avg = storeService.calculateAverage(t1, t2);
        if (avg.isPresent()) {
            return ResponseEntity.ok(avg.getAsDouble());
        } else {
            return ResponseEntity.noContent().build();
        }
    }

}
