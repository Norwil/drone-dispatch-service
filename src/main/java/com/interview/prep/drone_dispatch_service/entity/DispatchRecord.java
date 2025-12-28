package com.interview.prep.drone_dispatch_service.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DispatchRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String droneId;
    private String origin;
    private String destination;

    @Enumerated(EnumType.STRING)
    private Status status;  // Approved - Rejected
    private String reason;

    // Origin Weather
    private Double originTemp;
    private Double originWind;
    private Integer originWeatherCode;

    // Destination Weather
    private Double destTemp;
    private Double destWind;
    private Integer destWeatherCode;

    private LocalDateTime timestamp;
}
