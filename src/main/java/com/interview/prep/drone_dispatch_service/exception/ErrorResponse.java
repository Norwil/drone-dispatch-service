package com.interview.prep.drone_dispatch_service.exception;

import java.time.LocalDateTime;

public record ErrorResponse(
        String message,
        int statusCode,
        LocalDateTime timestamp
) {
}
