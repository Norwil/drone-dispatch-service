package com.interview.prep.drone_dispatch_service.dto;

import com.interview.prep.drone_dispatch_service.entity.Status;

import java.time.LocalDateTime;

public record DispatchHistoryResponse(
        String droneId,
        String origin,
        String destination,
        Status status,
        String reason,
        Double originTemp,
        Double destTemp,
        LocalDateTime timestamp
) {
}
