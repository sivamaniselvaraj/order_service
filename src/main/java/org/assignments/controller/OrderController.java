package org.assignments.controller;

import org.assignments.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.assignments.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
public class OrderController {

    @Autowired
    OrderService orderService;

    @PostMapping
    public ResponseEntity<?> createOrder(@RequestBody CreateOrderRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey){

        log.info("Creating order for customer {}", request.getCustomerId());

        return ResponseEntity.ok(orderService.createOrder(request, idempotencyKey));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long id) throws Exception {

        return ResponseEntity.ok(orderService.getOrder(id));
    }

}