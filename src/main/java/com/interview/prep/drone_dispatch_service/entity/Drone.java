package com.interview.prep.drone_dispatch_service.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Drone {

    @Id
    private String id; // Manual ID like "D-001"

    @Enumerated(EnumType.STRING)
    private DroneModel model;

    private Double batteryCapacity; // e.g., 100%

    @Enumerated(EnumType.STRING)
    private DroneState state;

    private String currentLocation;
}
