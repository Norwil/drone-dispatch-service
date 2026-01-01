package com.interview.prep.drone_dispatch_service.controller;

import com.interview.prep.drone_dispatch_service.dto.DispatchHistoryResponse;
import com.interview.prep.drone_dispatch_service.dto.DispatchRequest;
import com.interview.prep.drone_dispatch_service.dto.DispatchResponse;
import com.interview.prep.drone_dispatch_service.dto.DroneResponse;
import com.interview.prep.drone_dispatch_service.service.DispatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/dispatch")
@RequiredArgsConstructor
@Slf4j
public class DispatchController {

    private final DispatchService dispatchService;

    @PostMapping
    public ResponseEntity<DispatchResponse> dispatch(@Valid @RequestBody DispatchRequest request) {
        log.info("New dispatch request received for drone: {}", request.droneId());

        DispatchResponse response = dispatchService.dispatchDrone(request);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/history")
    public ResponseEntity<List<DispatchHistoryResponse>> getAllHistory() {
        return ResponseEntity.ok(dispatchService.getAllHistory());
    }

    @GetMapping("/history/{droneId}")
    public ResponseEntity<List<DispatchHistoryResponse>> getDroneHistory(
            @PathVariable String droneId
    ) {
        return ResponseEntity.ok(dispatchService.getDroneHistory(droneId));
    }

    @GetMapping("/fleet")
    public ResponseEntity<List<DroneResponse>> getFleetStatus() {
        return ResponseEntity.ok(dispatchService.getAvailableDrones());
    }

}
