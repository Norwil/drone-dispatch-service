package com.interview.prep.drone_dispatch_service.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "drone.rules")
@Data
public class DroneConfigProperties {
    private double maxRangeKm = 100.0;
    private double maxWindSpeed = 30.0;
    private double minTemperature = -10.0;
    private int stormCodeThreshold = 50;
}
