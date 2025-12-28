package com.interview.prep.drone_dispatch_service.client;

import com.interview.prep.drone_dispatch_service.dto.WeatherApiResponse;
import com.interview.prep.drone_dispatch_service.exception.WeatherServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@Slf4j
public class WeatherServiceClient {

    private final RestClient restClient;
    private final String weatherServiceUrl;

    public WeatherServiceClient(RestClient.Builder builder,
                                @Value("${weather.service.url}") String weatherServiceUrl) {
        this.restClient = builder.build();
        this.weatherServiceUrl = weatherServiceUrl;
    }

    public WeatherApiResponse getWeather(String city) {
        log.info("Calling Weather Service for city: {}", city);

        return restClient.get()
                .uri(weatherServiceUrl + "/weather/" + city)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    log.error("City not found or bad request: {}", city);
                    throw new WeatherServiceException("Weather data not found for city: " + city);
                })
                .onStatus(HttpStatusCode::is5xxServerError, (request, response) -> {
                    log.error("External Weather Service is down!");
                    throw new WeatherServiceException("External Weather Service is currently unavailable");
                })
                .body(WeatherApiResponse.class);
    }
}
