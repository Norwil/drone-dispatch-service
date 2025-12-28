package com.interview.prep.drone_dispatch_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record WeatherApiResponse(
        @JsonProperty("current_weather") CurrentWeather currentWeather,
        double latitude,
        double longitude
) {
    public record CurrentWeather(
            double temperature,
            double windspeed,
            int weathercode
    ) {}
}
