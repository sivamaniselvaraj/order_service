package org.assignments.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.assignments.dto.OrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.dto.OrderResponse;
import org.assignments.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.assignments.service.OrderService;

import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
@Tag(name = "Order API", description = "Operations related to Orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<?> createOrder(@RequestBody OrderRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey){

        log.info("Received create order request: {}", request);
        OrderResponse createOrder = orderService.createOrder(request, idempotencyKey);
        log.info("Order created successfully");

        return ResponseEntity.ok(createOrder);
    }

    @Operation(summary = "Get order by ID", description = "Fetch order details using order ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> getOrder(@PathVariable Long orderId) throws Exception {
        long startTime = System.currentTimeMillis();

        try {
            Order order = orderService.getOrderById(orderId);

            if (order == null) {
                log.warn("GET /orders/{} - Order not found", orderId);
                return ResponseEntity.notFound().build();
            }

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("GET /orders/{} - Success | TimeTaken={}ms | Status=FOUND",
                    orderId, timeTaken);

            return ResponseEntity.ok(order);

        } catch (Exception ex) {

            long timeTaken = System.currentTimeMillis() - startTime;

            log.error("GET /orders/{} - Failed | TimeTaken={}ms | Error={}",
                    orderId, timeTaken, ex.getMessage(), ex);
            throw ex;
        }
    }
    @Operation(summary = "Get all orders", description = "Fetch all order details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order(s) found"),
            @ApiResponse(responseCode = "404", description = "No Order found")
    })
    @GetMapping("/")
    public ResponseEntity<?> getAllOrders() throws Exception {


        long startTime = System.currentTimeMillis();

        try {
            List<Order> order = orderService.getAllOrders();

            if (order == null) {
                log.warn("GET /orders/ - Order(s) not found");
                return ResponseEntity.notFound().build();
            }

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("GET /orders - Success | TimeTaken={}ms | Status=FOUND | size: {}"
                    ,timeTaken, order.size());

            return ResponseEntity.ok(order);

        } catch (Exception ex) {

            long timeTaken = System.currentTimeMillis() - startTime;

            log.error("GET /orders - Failed | TimeTaken={}ms | Error={}", timeTaken, ex.getMessage(), ex);
            throw ex;
        }
    }

}