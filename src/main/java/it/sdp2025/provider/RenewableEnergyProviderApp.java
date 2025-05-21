package it.sdp2025.provider;

import it.sdp2025.administration.server.EnergyRequestPublisher;

public class RenewableEnergyProviderApp {
    public static void main(String[] args) throws Exception{
        new EnergyRequestPublisher().start();
        Thread.currentThread().join();
    }
}
