package org.assignments.order.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.assignments.order.dto.CreateOrderRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.dto.OrderResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.assignments.order.service.OrderService;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin
@Tag(name = "Order API", description = "Operations related to Orders")
public class OrderController {

    @Autowired
    OrderService orderService;

    // ── POST /api/v1/orders ───────────────────────────────────────────────────
    /**
     * Create a new order.
     * Publishes events to Kafka (processing + notification topics)
     * and calls the Notification Service REST API — all with a shared correlationId.
     */
    @PostMapping
    @Operation(summary = "Create a new order")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request, @RequestHeader("Idempotency-Key") String idempotencyKey){

        log.info("[CONTROLLER] POST Received create order request: {}", request);
        OrderResponse createOrder = orderService.createOrder(request, idempotencyKey);
        log.info("Order Status {}", createOrder.getOrderStatus());

        return ResponseEntity.status(HttpStatus.CREATED).body(createOrder);
    }

    // ── GET /api/v1/orders/{orderId} ──────────────────────────────────────────
    @Operation(summary = "Get order by ID", description = "Fetch order details using order ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "Order not found")
    })
    @GetMapping("/{orderId}")
    public ResponseEntity<?> getOrder(@PathVariable String orderId) throws Exception {
        long startTime = System.currentTimeMillis();
        log.info("[CONTROLLER] GET /api/v1/orders/{}", orderId);
        try {
            OrderResponse order = orderService.getOrderByOrderId(orderId);

            if (order == null) {
                log.warn("GET /api/v1/orders/{} - Order not found", orderId);
                return ResponseEntity.notFound().build();
            }

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("GET /api/v1/orders/{} - Success | TimeTaken={}ms | Status=FOUND",
                    orderId, timeTaken);

            return ResponseEntity.ok(order);

        } catch (Exception ex) {

            long timeTaken = System.currentTimeMillis() - startTime;

            log.error("GET /orders/{} - Failed | TimeTaken={}ms | Error={}",
                    orderId, timeTaken, ex.getMessage(), ex);
            throw ex;
        }
    }


    // ── GET /api/v1/orders ────────────────────────────────────────────────────
    /**
     * Paginated list of all orders.
     * Supports ?page=0&size=20&sort=createdAt,desc
     */
    @Operation(summary = "Get all orders", description = "Fetch all order details")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Order(s) found"),
            @ApiResponse(responseCode = "404", description = "No Order found")
    })
    @GetMapping("/")
    public ResponseEntity<Page<OrderResponse>> getAllOrders(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction
    ) throws Exception {

        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        long startTime = System.currentTimeMillis();

        try {
            Page<OrderResponse> order = orderService.getAllOrders(pageable);

            if (order == null) {
                log.warn("[CONTROLLER] GET /api/v1/orders/ - Order(s) not found");
                return ResponseEntity.notFound().build();
            }

            long timeTaken = System.currentTimeMillis() - startTime;

            log.info("[CONTROLLER] GET /api/v1/orders/ - Success | TimeTaken={}ms | Status=FOUND | size: {}"
                    ,timeTaken, order.getTotalPages());

            return ResponseEntity.ok(order);

        } catch (Exception ex) {

            long timeTaken = System.currentTimeMillis() - startTime;

            log.error("[CONTROLLER] GET /api/v1/orders/ - Failed | TimeTaken={}ms | Error={}", timeTaken, ex.getMessage(), ex);
            throw ex;
        }
    }

}