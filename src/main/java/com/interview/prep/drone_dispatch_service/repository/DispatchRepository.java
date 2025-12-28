package com.interview.prep.drone_dispatch_service.repository;

import com.interview.prep.drone_dispatch_service.entity.DispatchRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DispatchRepository extends JpaRepository<DispatchRecord, Long> {

    List<DispatchRecord> findByDroneIdOrderByTimestampDesc(String droneId);

}
