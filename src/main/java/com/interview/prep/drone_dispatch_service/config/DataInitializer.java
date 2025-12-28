package com.interview.prep.drone_dispatch_service.config;

import com.interview.prep.drone_dispatch_service.entity.Drone;
import com.interview.prep.drone_dispatch_service.entity.DroneModel;
import com.interview.prep.drone_dispatch_service.entity.DroneState;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final DroneRepository droneRepository;

    @Override
    public void run(String... args) throws Exception {
        if (droneRepository.count() == 0) {
            System.out.println("🌱 Seeding Drone Fleet...");

            List<Drone> initialFleet = List.of(
                    // BERLIN SQUAD
                    new Drone("D-001", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IDLE, "Berlin"),
                    new Drone("D-002", DroneModel.MIDDLEWEIGHT, 85.0, DroneState.IDLE, "Berlin"),
                    new Drone("D-003", DroneModel.HEAVYWEIGHT, 20.0, DroneState.MAINTENANCE, "Berlin"), // Low Battery start

                    // WARSAW SQUAD
                    new Drone("D-004", DroneModel.CRUISERWEIGHT, 100.0, DroneState.IDLE, "Warsaw"),
                    new Drone("D-005", DroneModel.LIGHTWEIGHT, 90.0, DroneState.IDLE, "Warsaw"),
                    new Drone("D-006", DroneModel.MIDDLEWEIGHT, 45.0, DroneState.IDLE, "Warsaw"),

                    // LONDON SQUAD
                    new Drone("D-007", DroneModel.HEAVYWEIGHT, 100.0, DroneState.IDLE, "London"),
                    new Drone("D-008", DroneModel.CRUISERWEIGHT, 10.0, DroneState.MAINTENANCE, "London"),

                    // ISTANBUL SQUAD
                    new Drone("D-009", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IDLE, "Istanbul"),
                    new Drone("D-010", DroneModel.MIDDLEWEIGHT, 75.0, DroneState.IDLE, "Istanbul")
            );

            droneRepository.saveAll(initialFleet);
            System.out.println("💫 Drone Inventory Initialized with " + initialFleet.size() + " drones!");
        }
    }
}