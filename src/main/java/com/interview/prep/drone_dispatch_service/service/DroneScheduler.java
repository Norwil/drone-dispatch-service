package com.interview.prep.drone_dispatch_service.service;

import com.interview.prep.drone_dispatch_service.entity.Drone;
import com.interview.prep.drone_dispatch_service.entity.DroneState;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DroneScheduler {

    private final DroneRepository droneRepository;

    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void simulateDroneActivity() {
        List<Drone> allDrones = droneRepository.findAll();

        for (Drone drone : allDrones) {
            // SCENARIO 1: ARRIVAL (Flight Complete)
            if (drone.getState() == DroneState.IN_FLIGHT) {
                // 1. Drain Battery (Simulate consumption)
                double newBattery = Math.max(0, drone.getBatteryCapacity() - 20.0);
                drone.setBatteryCapacity(newBattery);

                // 2. Land
                drone.setState(DroneState.IDLE);

                log.info("🛬 Drone {} arrived at {}. Battery: {}%", drone.getId(), drone.getCurrentLocation(), newBattery);
                droneRepository.save(drone);
            }

            // SCENARIO 2: LOW BATTERY CHECK
            if (drone.getState() == DroneState.IDLE && drone.getBatteryCapacity() < 25.0) {
                log.warn("🪫 Drone {} battery critical ({}%). Sending to RECHARGING.", drone.getId(), drone.getBatteryCapacity());
                drone.setState(DroneState.MAINTENANCE); // We use MAINTENANCE as "Recharging"
                droneRepository.save(drone);
            }

            // SCENARIO 3: RECHARGING
            if (drone.getState() == DroneState.MAINTENANCE) {
                double currentBat = drone.getBatteryCapacity();
                if (currentBat < 100.0) {
                    double chargedBat = Math.min(100.0, currentBat + 25.0); // Charge 25% per tick
                    drone.setBatteryCapacity(chargedBat);
                    log.info("⚡ Drone {} recharging... {}%", drone.getId(), chargedBat);

                    // If fully charged, release to IDLE
                    if (chargedBat >= 95.0) {
                        drone.setState(DroneState.IDLE);
                        log.info("✅ Drone {} fully charged. Returning to service.", drone.getId());
                    }
                    droneRepository.save(drone);
                }
            }
        }
    }
}