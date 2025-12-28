package com.interview.prep.drone_dispatch_service.service;

import com.interview.prep.drone_dispatch_service.client.WeatherServiceClient;
import com.interview.prep.drone_dispatch_service.config.DroneConfigProperties;
import com.interview.prep.drone_dispatch_service.dto.*;
import com.interview.prep.drone_dispatch_service.entity.DispatchRecord;
import com.interview.prep.drone_dispatch_service.entity.Drone;
import com.interview.prep.drone_dispatch_service.entity.DroneState;
import com.interview.prep.drone_dispatch_service.entity.Status;
import com.interview.prep.drone_dispatch_service.repository.DispatchRepository;
import com.interview.prep.drone_dispatch_service.repository.DroneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final WeatherServiceClient weatherClient;
    private final DispatchRepository dispatchRepository;
    private final DroneRepository droneRepository;
    private final DroneConfigProperties droneRules;

    @Transactional
    public DispatchResponse dispatchDrone(DispatchRequest request) {
        log.info("Processing dispatch request for drone: {}", request.droneId());

        // 1. Validate Inventory (Helper Method)
        Drone drone = validateDroneAvailability(request.droneId());

        if (!drone.getCurrentLocation().equalsIgnoreCase(request.origin())) {
            return saveAndReturn(request, Status.REJECTED,
                    "Drone is at " + drone.getCurrentLocation() + ", not " + request.origin(),
                    null, null);
        }

        if (drone.getState() != DroneState.IDLE) {
            return saveAndReturn(request, Status.REJECTED,
                    "Drone " + drone.getId() + " is currently " + drone.getState(), null, null);
        }

        // 2. Fetch External Data
        WeatherApiResponse originWeather = weatherClient.getWeather(request.origin());
        WeatherApiResponse destWeather = weatherClient.getWeather(request.destination());

        // 3. Rune Pre-Flight Checks (Helper Method)
        String rejectionReason = runPreFlightChecks(originWeather, destWeather);
        if (rejectionReason != null) {
            return saveAndReturn(request, Status.REJECTED, rejectionReason, originWeather, destWeather);
        }

        // 4. Lock Drone & Approve
        drone.setState(DroneState.IN_FLIGHT);
        drone.setCurrentLocation(request.destination());
        droneRepository.save(drone);

        return saveAndReturn(request, Status.APPROVED,
                "Flight approved. Conditions optimal.", originWeather, destWeather);
    }

    private Drone validateDroneAvailability(String droneId) {
        return droneRepository.findById(droneId)
                .orElseThrow(() -> new IllegalArgumentException("Drone not found: " + droneId));
    }

    private String runPreFlightChecks(WeatherApiResponse origin, WeatherApiResponse dest) {
        // A. Check Distance
        double distance = calculateDistanceKm(
                origin.latitude(), origin.longitude(),
                dest.latitude(), dest.longitude());

        if (distance > droneRules.getMaxRangeKm()) {
            return String.format("Destination too far (%.2f km). Max range is %.0fkm.", distance, droneRules.getMaxRangeKm());
        }

        // B. Check Weather Safety
        if (isUnsafe(origin)) return "Unsafe takeoff conditions in Origin Data.";
        if (isUnsafe(dest)) return "Unsafe takeoff conditions in Destination Data.";

        return null; // Approved - Checks passed!
    }

    public List<DroneResponse> getAvailableDrones() {
        return droneRepository.findAll()
                .stream()
                .map(this::mapToDroneResponse)
                .collect(Collectors.toList());
    }

    public List<DispatchHistoryResponse> getAllHistory() {
        return dispatchRepository.findAll()
                .stream()
                .map(this::mapToDispatchHistoryResponse)
                .collect(Collectors.toList());
    }

    public List<DispatchHistoryResponse> getDroneHistory(String droneId) {
        return dispatchRepository.findByDroneIdOrderByTimestampDesc(droneId)
                .stream()
                .map(this::mapToDispatchHistoryResponse)
                .collect(Collectors.toList());
    }

    private boolean isUnsafe(WeatherApiResponse response) {
        if (response == null) return true; // safety check

        double wind = response.currentWeather().windspeed();
        double temp = response.currentWeather().temperature();
        int code = response.currentWeather().weathercode();

        // Business Rules:
        // 1. Wind > 30 km/h is dangerous
        // 2. Temp < -10C is dangerous for battery
        // 3. Code >= 50 usually implies rain/snow
        return wind >= droneRules.getMaxWindSpeed() || temp <= droneRules.getMinTemperature() ||  code >= droneRules.getStormCodeThreshold();

    }

    private DispatchResponse saveAndReturn(
            DispatchRequest request,
            Status status,
            String reason,
            WeatherApiResponse originWeather,
            WeatherApiResponse destWeather) {

        DispatchRecord.DispatchRecordBuilder builder = DispatchRecord.builder()
                .droneId(request.droneId())
                .origin(request.origin())
                .destination(request.destination())
                .status(status)
                .reason(reason)
                .timestamp(LocalDateTime.now());

        if (originWeather != null) {
            builder.originTemp(originWeather.currentWeather().temperature());
            builder.originWind(originWeather.currentWeather().windspeed());
            builder.originWeatherCode(originWeather.currentWeather().weathercode());
        }

        if (destWeather != null) {
            builder.destTemp(destWeather.currentWeather().temperature());
            builder.destWind(destWeather.currentWeather().windspeed());
            builder.destWeatherCode(destWeather.currentWeather().weathercode());
        }

        dispatchRepository.save(builder.build());
        log.info("Dispatch Decision: {} - Reason: {}", status, reason);

        return new DispatchResponse(request.droneId(), status, reason);
    }

    private double calculateDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    private DispatchHistoryResponse mapToDispatchHistoryResponse(DispatchRecord record) {
        return new DispatchHistoryResponse(
                record.getDroneId(),
                record.getOrigin(),
                record.getDestination(),
                record.getStatus(),
                record.getReason(),
                record.getOriginTemp(),
                record.getDestTemp(),
                record.getTimestamp()
        );
    }

    private DroneResponse mapToDroneResponse(Drone drone) {
        return new DroneResponse(
                drone.getId(),
                drone.getModel().toString(),
                drone.getBatteryCapacity(),
                drone.getState().toString(),
                drone.getCurrentLocation()
        );
    }
}
