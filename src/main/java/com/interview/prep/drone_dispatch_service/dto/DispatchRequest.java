package com.interview.prep.drone_dispatch_service.dto;

import jakarta.validation.constraints.NotBlank;

public record DispatchRequest(
        @NotBlank String droneId,
        @NotBlank String origin,
        @NotBlank String destination
) {
}
