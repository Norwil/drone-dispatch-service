package com.interview.prep.drone_dispatch_service.repository;

import com.interview.prep.drone_dispatch_service.entity.Drone;
import com.interview.prep.drone_dispatch_service.entity.DroneState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DroneRepository extends JpaRepository<Drone, String> {

    Optional<Drone> findByIdAndState(String id, DroneState state);
}
