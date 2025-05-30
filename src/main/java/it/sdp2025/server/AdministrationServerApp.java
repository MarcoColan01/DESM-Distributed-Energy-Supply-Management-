package it.sdp2025.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AdministrationServerApp {
    public static void main(String[] args) {
        SpringApplication.run(AdministrationServerApp.class, args);
        System.out.println("Admin-server up at http://localhost:8080");
    }
}
