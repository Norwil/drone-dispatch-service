package com.interview.prep.drone_dispatch_service.service;

import com.interview.prep.drone_dispatch_service.entity.Drone;
import com.interview.prep.drone_dispatch_service.entity.DroneModel;
import com.interview.prep.drone_dispatch_service.entity.DroneState;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Drone Scheduler Logic Tests")
public class DroneSchedulerTest {

    @Mock
    private DroneRepository droneRepository;

    @InjectMocks
    private DroneScheduler droneScheduler;

    @Captor
    private ArgumentCaptor<Drone> droneCaptor;

    private Drone createDrone(String id, DroneState state, double battery) {
        return Drone.builder()
                .id(id)
                .model(DroneModel.LIGHTWEIGHT)
                .state(state)
                .batteryCapacity(battery)
                .currentLocation("Base")
                .build();
    }

    @Nested
    @DisplayName("Scenario 1: Drone Arrival")
    class ArrivalTests {

        @Test
        @DisplayName("Should drain battery and switch to IDLE when drone is IN_FLIGHT")
        void simulate_WhenDroneIsFlying_ShouldLandAndDrainBattery() {

            // Arrange
            Drone flyingDrone = createDrone("D-001", DroneState.IN_FLIGHT, 100.0);

            when(droneRepository.findAll()).thenReturn(List.of(flyingDrone));

            // Act
            droneScheduler.simulateDroneActivity();

            // Assert
            verify(droneRepository).save(droneCaptor.capture());

            Drone savedDrone = droneCaptor.getValue();

            assertAll("Arrival Validations",
                    () -> assertEquals(DroneState.IDLE, savedDrone.getState(), "Drone should land (IDLE)"),
                    () -> assertEquals(80.0, savedDrone.getBatteryCapacity(), "Battery should drain by 20%")
            );
        }
    }

    @Nested
    @DisplayName("Scenario 2: Low Battery Check")
    class LowBatteryTests {

        @Test
        @DisplayName("Should switch to MAINTENANCE and start charging when battery < 25%")
        void simulate_WhenIdleAndBatteryCritical_ShouldSwitchToMaintenanceAndCharge() {
            // Arrange
            Drone lowBatteryDrone = createDrone("D-001", DroneState.IDLE, 0.0);
            when(droneRepository.findAll()).thenReturn(List.of(lowBatteryDrone));

            // Act
            droneScheduler.simulateDroneActivity();

            // Assert
            verify(droneRepository, times(2)).save(droneCaptor.capture());

            Drone finalState = droneCaptor.getValue(); // getValue() gives the latest capture

            assertAll("Low Battery Transition",
                    () -> assertEquals(DroneState.MAINTENANCE, finalState.getState(), "State should end up in MAINTENANCE"),
                    () -> assertEquals(25.0, finalState.getBatteryCapacity(), "Battery should have started charging (0 -> 25)")
            );
        }
    }

    @Nested
    @DisplayName("Scenario 3: Recharging Logic")
    class RechargingTests {

        @Test
        @DisplayName("Should continue charging if in MAINTENANCE and not full")
        void simulate_WhenMaintenanceAndNotFull_ShouldCharge() {
            // Arrange
            Drone chargingDrone = createDrone("D-001", DroneState.MAINTENANCE, 50.0);
            when(droneRepository.findAll()).thenReturn(List.of(chargingDrone));

            // Act
            droneScheduler.simulateDroneActivity();

            // Assert
            verify(droneRepository, times(1)).save(droneCaptor.capture());

            Drone savedDrone = droneCaptor.getValue();
            assertAll("Charging Progress",
                    () -> assertEquals(DroneState.MAINTENANCE, savedDrone.getState(), "Should stay in MAINTENANCE"),
                    () -> assertEquals(75.0, savedDrone.getBatteryCapacity(), "Should add 25% charge")
            );
        }

        @Test
        @DisplayName("Should finish charging and switch to IDLE when battery reaches threshold")
        void simulate_WhenChargingFinishes_ShouldSwitchToIdle() {
            // Arrange
            Drone almostFullDrone = createDrone("D-001", DroneState.MAINTENANCE, 90.0);
            when(droneRepository.findAll()).thenReturn(List.of(almostFullDrone));

            // Act
            droneScheduler.simulateDroneActivity();

            // Assert
            verify(droneRepository, times(1)).save(droneCaptor.capture());

            Drone savedDrone = droneCaptor.getValue();
            assertAll("Charging Completion",
                    () -> assertEquals(DroneState.IDLE, savedDrone.getState(), "Should return to service (IDLE)"),
                    () -> assertEquals(100.0, savedDrone.getBatteryCapacity(), "Battery should be capped at 100%")
            );
        }
    }


}
