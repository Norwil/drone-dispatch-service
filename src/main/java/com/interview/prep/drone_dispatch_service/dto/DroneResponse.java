package com.interview.prep.drone_dispatch_service.dto;

public record DroneResponse(
        String id,
        String model,
        Double batteryCapacity,
        String state,
        String currentLocation
) {}
