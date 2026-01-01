package com.interview.prep.drone_dispatch_service.service;

import com.interview.prep.drone_dispatch_service.client.WeatherServiceClient;
import com.interview.prep.drone_dispatch_service.config.DroneConfigProperties;
import com.interview.prep.drone_dispatch_service.dto.DispatchRequest;
import com.interview.prep.drone_dispatch_service.dto.DispatchResponse;
import com.interview.prep.drone_dispatch_service.dto.WeatherApiResponse;
import com.interview.prep.drone_dispatch_service.entity.*;
import com.interview.prep.drone_dispatch_service.repository.DispatchRepository;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Dispatch Service Unit Tests")
public class DispatchServiceTest {

    @Mock private WeatherServiceClient weatherClient;
    @Mock private DispatchRepository dispatchRepository;
    @Mock private DroneRepository droneRepository;
    @Mock private DroneConfigProperties droneRules;

    @InjectMocks
    private DispatchService dispatchService;

    @Captor private ArgumentCaptor<Drone> droneCaptor;
    @Captor private ArgumentCaptor<DispatchRecord> recordCaptor;

    @BeforeEach
    void setupRules() {
        lenient().when(droneRules.getMaxRangeKm()).thenReturn(20.0);
        lenient().when(droneRules.getMaxWindSpeed()).thenReturn(30.0);
        lenient().when(droneRules.getMinTemperature()).thenReturn(-10.0);
        lenient().when(droneRules.getStormCodeThreshold()).thenReturn(50);
    }

    private DispatchRequest createRequest(String from, String to) {
        return new DispatchRequest("D-001", from, to);
    }

    private Drone createDrone(String location, DroneState state) {
        return Drone.builder()
                .id("D-001")
                .model(DroneModel.LIGHTWEIGHT)
                .batteryCapacity(100.0)
                .currentLocation(location)
                .state(state)
                .build();
    }

    private WeatherApiResponse createWeather(double lat, double lon, double temp, double wind, int code) {
        return new WeatherApiResponse(
                new WeatherApiResponse.CurrentWeather(temp, wind, code),
                lat,
                lon
        );
    }

    @Nested
    @DisplayName("Validation & Inventory Checks")
    class ValidationTests {

        @Test
        @DisplayName("Should throw exception when drone ID does not exist")
        void dispatch_DroneNotFound_ThrowsException() {
            // Arrange
            when(droneRepository.findById("D-UNKNOWN")).thenReturn(Optional.empty());
            DispatchRequest request = new DispatchRequest("D-UNKNOWN", "A", "B");

            // Act & Assert
            assertThrows(IllegalArgumentException.class, () -> dispatchService.dispatchDrone(request));

            // Verify we never bothered the weather service
            verifyNoInteractions(weatherClient);
        }

        @Test
        @DisplayName("Should reject when drone is not at the requested origin")
        void dispatch_LocationMismatch_ReturnsRejected() {
            // Arrange
            Drone droneInLondon = createDrone("London", DroneState.IDLE);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(droneInLondon));

            DispatchRequest request = createRequest("Paris", "Warsaw");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.REJECTED, response.status());
            assertTrue(response.reason().contains("not Paris"));

            // Verify efficiency
            verifyNoInteractions(weatherClient);
        }

        @Test
        @DisplayName("Should reject drone is currently BUSY (not IDLE)")
        void dispatch_DroneBusy_ReturnsRejected() {
            // Arrange
            Drone busyDrone = createDrone("Paris", DroneState.IN_FLIGHT);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(busyDrone));

            DispatchRequest request = createRequest("Paris", "Berlin");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.REJECTED, response.status());
            assertTrue(response.reason().contains("IN_FLIGHT"));

            verifyNoInteractions(weatherClient);
        }
    }

    @Nested
    @DisplayName("Business Rule Checks")
    class BusinessRuleTests {

        @Test
        @DisplayName("Should reject when destination is beyond max range (20km)")
        void dispatch_DistanceTooFar_ReturnsRejected() {
            // Arrange
            Drone drone = createDrone("Berlin", DroneState.IDLE);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(drone));

            WeatherApiResponse berlinWeather = createWeather(52.52, 13.40, 20.0, 5.0, 0);
            WeatherApiResponse parisWeather = createWeather(48.85, 2.35, 20.0, 5.0, 0);

            when(weatherClient.getWeather("Berlin")).thenReturn(berlinWeather);
            when(weatherClient.getWeather("Paris")).thenReturn(parisWeather);

            DispatchRequest request = createRequest("Berlin", "Paris");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.REJECTED, response.status());
            assertTrue(response.reason().contains("Destination too far"),
                    "Reason should mention distance but was: " + response.reason());
        }

        @Test
        @DisplayName("Should reject when ORIGIN weather is unsafe (High Wind)")
        void dispatch_OriginUnsafe_ReturnsRejected() {
            // Arrange
            Drone drone = createDrone("Berlin", DroneState.IDLE);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(drone));

            WeatherApiResponse badOrigin = createWeather(52.52, 13.40, 20.0, 50.0, 0);
            WeatherApiResponse goodDest = createWeather(52.50, 13.41, 20.0, 5.0, 0);

            when(weatherClient.getWeather("Berlin")).thenReturn(badOrigin);
            when(weatherClient.getWeather("Potsdam")).thenReturn(goodDest);

            DispatchRequest request = createRequest("Berlin", "Potsdam");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.REJECTED, response.status());
            assertTrue(response.reason().contains("Unsafe takeoff"), "Should fail due to unsafe origin");
        }

        @Test
        @DisplayName("Should reject when DESTINATION weather is unsafe (Freezing Temp)")
        void dispatch_DestUnsafe_ReturnsRejected() {
            // Arrange
            Drone drone = createDrone("Berlin", DroneState.IDLE);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(drone));

            WeatherApiResponse goodOrigin = createWeather(52.52, 13.40, 20.0, 5.0, 0);
            WeatherApiResponse badDest = createWeather(52.52, 13.40, -15.0, 5.0, 0);

            when(weatherClient.getWeather("Berlin")).thenReturn(goodOrigin);
            when(weatherClient.getWeather("Potsdam")).thenReturn(badDest);

            DispatchRequest request =  createRequest("Berlin", "Potsdam");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.REJECTED, response.status());
            assertTrue(response.reason().contains("Unsafe takeoff"),"Should fail due to unsafe destination");
        }
    }

    @Nested
    @DisplayName("Successful Dispatch Scenarios")
    class SuccessfulDispatchTests {

        @Test
        @DisplayName("Should approve flight and update drone state when all conditions met")
        void dispatch_Success_UpdatesStateAndSaveRecord() {
            // Arrange
            Drone drone = createDrone("Berlin", DroneState.IDLE);
            when(droneRepository.findById("D-001")).thenReturn(Optional.of(drone));

            WeatherApiResponse originWeather = createWeather(52.520, 13.400, 20.0, 5.0, 0);
            WeatherApiResponse destWeather   = createWeather(52.525, 13.395, 21.0, 6.0, 0);

            when(weatherClient.getWeather("Berlin")).thenReturn(originWeather);
            when(weatherClient.getWeather("Berlin Zoo")).thenReturn((destWeather));

            DispatchRequest request = createRequest("Berlin", "Berlin Zoo");

            // Act
            DispatchResponse response = dispatchService.dispatchDrone(request);

            // Assert
            assertEquals(Status.APPROVED, response.status());
            assertEquals("Flight approved. Conditions optimal.", response.reason());

            verify(droneRepository).save(droneCaptor.capture());

            Drone savedDrone = droneCaptor.getValue();
            assertAll("Drone State Updates",
                    () -> assertEquals(DroneState.IN_FLIGHT, savedDrone.getState(), "Drone should be flying"),
                    () -> assertEquals("Berlin Zoo", savedDrone.getCurrentLocation(), "Location should update to dest")
                    );

            verify(dispatchRepository).save(recordCaptor.capture());

            DispatchRecord savedRecord = recordCaptor.getValue();
            assertAll("Dispatch Record Details",
                    () -> assertEquals("D-001", savedRecord.getDroneId()),
                    () -> assertEquals(Status.APPROVED, savedRecord.getStatus()),
                    () -> assertEquals(20.0, savedRecord.getOriginTemp(), "Should record origin temp")
            );
        }
    }
}
