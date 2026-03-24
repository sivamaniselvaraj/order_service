package org.assignments.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.dto.CreateOrderRequest;
import org.assignments.dto.OrderResponse;
import org.assignments.entity.Order;
import org.assignments.entity.OutboxEvent;
import org.assignments.repository.OrderRepository;
import org.assignments.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    @Autowired
    OrderRepository orderRepository;
    @Autowired
    OutboxRepository outboxRepository;

    @Transactional(rollbackFor = Exception.class)
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")

    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {

        // 🔍 Step 1: Check existing
        Optional<Order> orderExisting = orderRepository.findByIdempotencyKey(idempotencyKey);

        if (orderExisting.isPresent()) {
            return new OrderResponse(orderExisting.get().getOrderId(), "Order Already Exists");
        }

        Long orderId = System.currentTimeMillis();

        Order order = new Order();
        order.setOrderId(orderId);
        order.setCustomerId(request.getCustomerId());
        order.setOrderStatus("CREATED");
        order.setTotalAmount(request.getTotalAmount());
        order.setCreatedAt(LocalDateTime.now());
        order.setCurrency(request.getCurrency());

        orderRepository.save(order);

        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "Order",
                "orderId", orderId,
                "correlationId", correlationId,
                "status", "orderCreated",
                "idempotencyKey",idempotencyKey
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        outbox.setAggregateId(orderId);
        outbox.setAggregateType("Ordering");
        outbox.setEventType("orderCreated");
        outbox.setPayload(new ObjectMapper().writeValueAsString(event));
        outbox.setStatus("NEW");
        outbox.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(outbox);

        log.info("Order created {} and outbox event stored", orderId);

        return new OrderResponse(orderId, "CREATED");
    }

    public OrderResponse fallbackCreateOrder(CreateOrderRequest request, Throwable t) {

        log.error("Order creation failed", t);
        return new OrderResponse(null, "FAILED");
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) throws Exception {
        Optional<Order> order = orderRepository.findById(id);
        return order.orElseThrow(() -> {
            return new NoSuchElementException("no data found");
        });
    }
}