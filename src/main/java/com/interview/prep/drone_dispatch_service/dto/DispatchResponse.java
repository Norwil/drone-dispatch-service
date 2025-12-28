package com.interview.prep.drone_dispatch_service.dto;

import com.interview.prep.drone_dispatch_service.entity.Status;

public record DispatchResponse(
        String droneId,
        Status status,
        String reason
) {
}
