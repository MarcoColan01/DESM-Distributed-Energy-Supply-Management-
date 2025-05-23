package it.sdp2025.thermalplant;

public class ThermalPlantApp {
    public static void main(String[] args) throws Exception{
        PlantConfig config = PlantConfig.fromArgs(args);
        ThermalPlant plant = new ThermalPlant(config);
        plant.start(args);
    }
}
