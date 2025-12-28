package com.interview.prep.drone_dispatch_service;

import com.interview.prep.drone_dispatch_service.client.WeatherServiceClient;
import com.interview.prep.drone_dispatch_service.config.DroneConfigProperties;
import com.interview.prep.drone_dispatch_service.dto.DispatchRequest;
import com.interview.prep.drone_dispatch_service.dto.DispatchResponse;
import com.interview.prep.drone_dispatch_service.dto.WeatherApiResponse;
import com.interview.prep.drone_dispatch_service.entity.*;
import com.interview.prep.drone_dispatch_service.repository.DispatchRepository;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import com.interview.prep.drone_dispatch_service.service.DispatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DispatchServiceTest {

    @Mock private WeatherServiceClient weatherClient;
    @Mock private DispatchRepository dispatchRepository;
    @Mock private DroneRepository droneRepository;
    @Mock private DroneConfigProperties droneRules;

    @InjectMocks
    private DispatchService dispatchService;

    @BeforeEach
    void setupRules() {
        lenient().when(droneRules.getMaxRangeKm()).thenReturn(25.0);
        lenient().when(droneRules.getMaxWindSpeed()).thenReturn(30.0);
        lenient().when(droneRules.getMinTemperature()).thenReturn(-10.0);
        lenient().when(droneRules.getStormCodeThreshold()).thenReturn(50);
    }

    private WeatherApiResponse createWeather(double lat, double lon, double temp, double wind, int code) {
        return new WeatherApiResponse(
                new WeatherApiResponse.CurrentWeather(temp, wind, code),
                lat,
                lon
        );
    }

    @Test
    void shouldApproveFlight_WhenDroneIsIdle_AndConditionsOptimal() {
        // Arrange
        DispatchRequest request = new DispatchRequest("D-001", "Berlin", "Grunewald");

        // 1. Mock Drone Inventory (It must exist and be IDLE)
        Drone idleDrone = new Drone("D-001", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IDLE, "Berlin");
        when(droneRepository.findById("D-001")).thenReturn(Optional.of(idleDrone));

        WeatherApiResponse berlinWeather = createWeather(52.52, 13.40, 20., 5.0, 0);
        WeatherApiResponse grunewaldWeather = createWeather(52.50, 13.15, 21.0, 6.0, 0);

        when(weatherClient.getWeather("Berlin")).thenReturn(berlinWeather);
        when(weatherClient.getWeather("Grunewald")).thenReturn(grunewaldWeather);

        when(dispatchRepository.save(any(DispatchRecord.class))).thenAnswer(i -> i.getArguments()[0]);

        // Act
        DispatchResponse response = dispatchService.dispatchDrone(request);

        // Assert
        assertEquals(Status.APPROVED, response.status());
        assertTrue(response.reason().contains("Flight approved"));

        // Verify saved to DB
        verify(dispatchRepository, times(1)).save(any(DispatchRecord.class));

        // Verify Drone State updated to IN_FLIGHT
        assertEquals(DroneState.IN_FLIGHT, idleDrone.getState());
        verify(droneRepository).save(idleDrone);
    }

    @Test
    void shouldRejectFlight_WhenDistanceIsTooFar() {
        // Arrange
        DispatchRequest request = new DispatchRequest("D-001", "Warsaw", "Paris");

        Drone idleDrone = new Drone("D-001", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IDLE, "Warsaw");
        when(droneRepository.findById("D-001")).thenReturn(Optional.of(idleDrone));

        WeatherApiResponse warsawWeather = createWeather(52.23, 21.01, 20.0, 5.0, 0);
        WeatherApiResponse parisWeather = createWeather(48.85, 2.35, 22.0, 10.0, 0);

        when(weatherClient.getWeather("Warsaw")).thenReturn(warsawWeather);
        when(weatherClient.getWeather("Paris")).thenReturn(parisWeather);

        // Act
        DispatchResponse response = dispatchService.dispatchDrone(request);

        // Assert
        assertEquals(Status.REJECTED, response.status());
        assertTrue(response.reason().contains("Destination too far"));
    }

    @Test
    void shouldRejectFlight_WhenWeatherIsStormy() {
        DispatchRequest request = new DispatchRequest("D-001", "Berlin", "Grunewald");

        Drone idleDrone = new Drone("D-001", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IDLE, "Berlin");
        when(droneRepository.findById("D-001")).thenReturn(Optional.of(idleDrone));

        WeatherApiResponse berlinWeather = createWeather(52.52, 13.40, -5.0, 55.0, 95);
        WeatherApiResponse grunewaldWeather = createWeather(52.50, 13.15, 21.0, 6.0, 0);

        when(weatherClient.getWeather("Berlin")).thenReturn(berlinWeather);
        when(weatherClient.getWeather("Grunewald")).thenReturn(grunewaldWeather);

        // Act
        DispatchResponse response = dispatchService.dispatchDrone(request);

        // Assert
        assertEquals(Status.REJECTED, response.status());
        assertTrue(response.reason().contains("Unsafe takeoff"));
    }

    @Test
    void shouldRejectFlight_WhenDroneIsBusy() {
        // Arrange
        DispatchRequest request = new DispatchRequest("D-001", "Warsaw", "Zabki");

        Drone busyDrone = new Drone("D-001", DroneModel.LIGHTWEIGHT, 100.0, DroneState.IN_FLIGHT, "Warsaw");
        when(droneRepository.findById("D-001")).thenReturn(Optional.of(busyDrone));

        // Act
        DispatchResponse response = dispatchService.dispatchDrone(request);

        // Assert
        assertEquals(Status.REJECTED, response.status());
        assertTrue(response.reason().contains("currently IN_FLIGHT"));

        // Verify
        verifyNoInteractions(weatherClient);
    }

}
