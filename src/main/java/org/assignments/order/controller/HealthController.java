package org.assignments.order.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/heartbeat")
    public ResponseEntity<Map<String, String>> heartbeat(){
        return ResponseEntity.ok(Map.of("status", "UP", "service", "order-service"));
    }
}