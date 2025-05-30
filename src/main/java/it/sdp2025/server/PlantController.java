package it.sdp2025.server;

import it.sdp2025.common.PlantInfo;
import it.sdp2025.common.PlantRegistrationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/plants")
public class PlantController {
    @Autowired PlantNetworkService plantService;

    @GetMapping
    public ResponseEntity<List<PlantInfo>> getPlants(){
        return ResponseEntity.ok(plantService.listOfPlants());
    }

    @PostMapping("/register")
    public ResponseEntity<List<PlantInfo>> registerPlant(@RequestBody PlantRegistrationRequest request){
        PlantInfo newPlant = new PlantInfo(request.getId(), "localhost", request.getGrpcPort());
        try{
            List<PlantInfo> others = plantService.registerPlant(newPlant);
            return ResponseEntity.ok(others);
        }catch (IllegalStateException e){
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }
}
