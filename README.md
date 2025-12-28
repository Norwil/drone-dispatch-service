# 🚁 Drone Dispatch Service (Microservice B)

A critical decision-engine microservice that determines if a drone can safely fly between two cities based on real-time weather conditions and geospatial constraints.

## 🏗️ Architecture

This service acts as the **Logic Core** in a distributed system, communicating with the [Weather Checker Service](LINK_TO_YOUR_OTHER_REPO).

`Client Request` → `Drone Controller` → `Dispatch Service` → `Weather Service (HTTP)` → `Decision Engine` → `PostgreSQL`

## ✨ Key Features

* **Inter-Service Communication:** Uses `RestClient` to consume external APIs with resilience patterns (Error Handling for 4xx/5xx).
* **Geospatial Logic:** Implements **Haversine Formula** to calculate precise flight distances and enforce battery range limits (Max 25km).
* **Safety Rules Engine:** Automatically rejects flights based on:
    * Wind Speed (> 30km/h)
    * Temperature (< -10°C)
    * Storm Codes (Thunderstorms/Heavy Rain)
* **Audit Logging:** Every request (Approved or Rejected) is persisted to **PostgreSQL** for compliance.
* **Robust Testing:** 80%+ Unit Test coverage using **Mockito** and Integration Testing with **H2**.

## 🚀 Tech Stack

* **Java 21** & **Spring Boot 3.4**
* **PostgreSQL** (Production DB) & **H2** (Test DB)
* **Docker** (Containerization)
* **Mockito** & **JUnit 5**
* **Lombok** & **Validation API**

## 🛠️ How to Run

1.  **Start Infrastructure:**
    ```bash
    docker-compose up -d
    ```
2.  **Run Application:**
    ```bash
    ./mvnw spring-boot:run -Dspring-boot.run.profiles=prod
    ```
3.  **Test Endpoint:**
    ```bash
    curl -X POST http://localhost:8081/api/v1/dispatch \
    -H "Content-Type: application/json" \
    -d '{"droneId": "D-777", "origin": "Berlin", "destination": "Potsdam"}'
    ```