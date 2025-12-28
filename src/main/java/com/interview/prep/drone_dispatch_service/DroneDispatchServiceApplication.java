package com.interview.prep.drone_dispatch_service;

import com.interview.prep.drone_dispatch_service.config.DroneConfigProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(DroneConfigProperties.class)
@EnableScheduling
public class DroneDispatchServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(DroneDispatchServiceApplication.class, args);
	}

}
