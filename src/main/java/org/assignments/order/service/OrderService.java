package org.assignments.order.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.dto.CreateOrderRequest;
import org.assignments.order.dto.OrderResponse;
import org.assignments.order.entity.Order;
import org.assignments.order.entity.OrderItem;
import org.assignments.order.exception.OrderNotFoundException;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.assignments.order.enums.OrderStatus;
import org.assignments.order.outbox.service.OutboxEventFactory;
import org.assignments.order.repository.OrderRepository;
import org.assignments.order.outbox.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OutboxEventFactory outboxEventFactory;

    // ── Create Order ──────────────────────────────────────────────────────────

    /**
     * Single atomic transaction:
     *   1. Persist the Order
     *   2. Write 3 OutboxMessage rows (Kafka×2 + REST×1) to the same DB transaction
     *
     * No Kafka or HTTP call happens here.
     * The OutboxPoller picks up the rows asynchronously and dispatches them.
     * If the transaction rolls back, the outbox rows roll back with it — no phantom events.
     */

    @Transactional(rollbackFor = Exception.class)
    @CircuitBreaker(name = "orderService", fallbackMethod = "fallbackCreateOrder")

    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {

        String orderId       =  UUID.randomUUID().toString().toUpperCase();
        String correlationId = "COR-" + UUID.randomUUID().toString().toUpperCase();

        log.info("[ORDER-SERVICE] Creating order orderId={} correlationId={} customerId={} idempotencyKey={}",
                orderId, correlationId, request.getCustomerId(), idempotencyKey);

        // 🔍 Step 1: Check existing
        Optional<Order> orderExisting = orderRepository.findByIdempotencyKey(idempotencyKey);

        if (orderExisting.isPresent()) {
            return new OrderResponse(orderExisting.get().getOrderId(), "Order Already Exists");
        }

        // Step 2: Build line items
        List<OrderItem> items = request.getItems().stream()
                .map(i -> {
                    BigDecimal subtotal = i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity()));
                    return OrderItem.builder()
                            .productId(i.getId())
                            .productName(i.getName())
                            .quantity(i.getQuantity())
                            .unitPrice(i.getPrice())
                            .subtotal(subtotal)
                            .build();
                })
                .collect(Collectors.toList());

        BigDecimal total = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);


        // Step 3: Persist order
        Order order = Order.builder()
                .orderId(orderId)
                .correlationId(correlationId)
                .customerId(request.getCustomerId())
                .customerEmail(request.getCustomerEmail())
                .status(OrderStatus.PENDING)
                .totalAmount(total)
                .currency(request.getCurrency())
                .items(items)
                .idempotencyKey(idempotencyKey)
                //.shippingAddress(request.getShippingAddress())
                .build();

        Order savedOrder = orderRepository.save(order);

        log.info("[ORDER-SERVICE] Persisted order id={} orderId={}", savedOrder.getId(), savedOrder.getOrderId());

        // Step 4: Write outbox messages — same transaction, atomically with the order row
        List<OutboxMessage> outboxMessages = outboxEventFactory.createOutboxMessages(savedOrder);
        outboxRepository.saveAll(outboxMessages);

        log.info("[ORDER-SERVICE] Written {} outbox message(s) for orderId={} correlationId={}",
                outboxMessages.size(), orderId, correlationId);
        return new OrderResponse(savedOrder.getOrderId(), savedOrder.getStatus().toString());
        //return OrderResponse.from(savedOrder);
    }

    public OrderResponse fallbackCreateOrder(CreateOrderRequest request, String idempotencyKey, Throwable t) {

        log.error("Order creation failed", t);
        return new OrderResponse(null, OrderStatus.FAILED.name());
    }



    // ── Read Operations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderId(String orderId) {
        return OrderResponse.from(
                orderRepository.findByOrderId(orderId)
                        .orElseThrow(() -> new OrderNotFoundException("Order not found: " + orderId)));
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByCorrelationId(String correlationId) {
        return OrderResponse.from(
                orderRepository.findByCorrelationId(correlationId)
                        .orElseThrow(() -> new OrderNotFoundException("No order found for correlationId: " + correlationId)));
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAll(pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByCustomer(String customerId, Pageable pageable) {
        return orderRepository.findByCustomerId(customerId, pageable).map(OrderResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> getOrdersByStatus(String status, Pageable pageable) {
        OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
        return orderRepository.findByStatus(orderStatus, pageable).map(OrderResponse::from);
    }
}