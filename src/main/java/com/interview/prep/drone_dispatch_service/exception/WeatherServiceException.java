package com.interview.prep.drone_dispatch_service.exception;

public class WeatherServiceException extends RuntimeException {
    public WeatherServiceException(String message) {
        super(message);
    }
}