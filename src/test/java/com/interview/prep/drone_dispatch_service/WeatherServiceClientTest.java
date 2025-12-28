package com.interview.prep.drone_dispatch_service;

import com.interview.prep.drone_dispatch_service.client.WeatherServiceClient;
import com.interview.prep.drone_dispatch_service.exception.WeatherServiceException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

@RestClientTest(WeatherServiceClient.class)
@TestPropertySource(properties = "weather.service.url=http://localhost:8080")
class WeatherServiceClientTest {

    @Autowired
    private MockRestServiceServer mockServer;

    @Autowired
    private WeatherServiceClient client;

    @Test
    void shouldThrowException_WhenCityNotFound() {
        // Arrange
        String city = "Atlantis";

        mockServer.expect(requestTo("http://localhost:8080/weather/" + city))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withBadRequest()); // Simulate 400/404 Error

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.getWeather(city);
        });

        assertTrue(exception.getMessage().contains("Weather data not found"));
    }

    @Test
    void shouldThrowException_WhenServerIsDown() {
        // Arrange
        String city = "Berlin";

        mockServer.expect(requestTo("http://localhost:8080/weather/" + city))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withServerError()); // Simulate 500 Error

        // Act & Assert
        WeatherServiceException exception = assertThrows(WeatherServiceException.class, () -> {
            client.getWeather(city);
        });

        assertTrue(exception.getMessage().contains("currently unavailable"));
    }
}