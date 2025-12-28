package com.interview.prep.drone_dispatch_service.entity;

import lombok.Getter;

@Getter
public enum DroneModel {
    LIGHTWEIGHT("Light Cargo", 5.0),
    MIDDLEWEIGHT("Medium Cargo", 10.0),
    CRUISERWEIGHT("Heavy Cargo", 20.0),
    HEAVYWEIGHT("Industrial", 50.0);

    private final String description;
    private final Double maxPayloadKg;

    DroneModel(String description, Double maxPayloadKg) {
        this.description = description;
        this.maxPayloadKg = maxPayloadKg;
    }
}