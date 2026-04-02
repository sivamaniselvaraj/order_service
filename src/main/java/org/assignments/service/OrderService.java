package org.assignments.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.dto.CreateOrderRequest;
import org.assignments.dto.OrderItemDTO;
import org.assignments.dto.OrderResponse;
import org.assignments.entity.Order;
import org.assignments.entity.OrderItem;
import org.assignments.entity.OutboxEvent;
import org.assignments.enums.OrderStatus;
import org.assignments.repository.OrderRepository;
import org.assignments.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

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
        order.setOrderStatus(OrderStatus.CREATED.name());
        order.setTotalAmount(request.getTotalAmount());
        order.setCreatedAt(LocalDateTime.now());
        order.setCurrency(request.getCurrency());
        order.setIdempotencyKey(idempotencyKey);
        List<OrderItem> items = request.getItems().stream()
                .map(itemDTO -> mapItem(itemDTO, order))
                .toList();

        order.setItems(items);

        orderRepository.save(order);

        String correlationId = UUID.randomUUID().toString();

        Map<String, Object> event = Map.of(
                "eventType", "Order",
                "orderId", orderId,
                "correlationId", correlationId,
                "status", OrderStatus.CREATED.name(),
                "idempotencyKey",idempotencyKey
        );

        OutboxEvent outbox = new OutboxEvent();
        outbox.setEventId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        outbox.setAggregateId(orderId);
        outbox.setAggregateType("Order");
        outbox.setEventType(OrderStatus.CREATED.name());
        outbox.setPayload(new ObjectMapper().writeValueAsString(event));
        outbox.setStatus("NEW");
        outbox.setCreatedAt(LocalDateTime.now());

        outboxRepository.save(outbox);

        log.info("Order created {} and outbox event stored", orderId);

        return new OrderResponse(orderId, OrderStatus.CREATED.name());
    }

    public OrderResponse fallbackCreateOrder(CreateOrderRequest request, String idempotencyKey, Throwable t) {

        log.error("Order creation failed", t);
        return new OrderResponse(null, OrderStatus.FAILED.name());
    }



    @Transactional(readOnly = true)
    public Order getOrder(Long id) throws Exception {
        Optional<Order> order = orderRepository.findById(id);
        return order.orElseThrow(() -> {
            return new NoSuchElementException("no data found");
        });
    }

    private static OrderItem mapItem(OrderItemDTO dto, Order order) {
        Long orderItemId = System.currentTimeMillis();
        OrderItem item = new OrderItem();
        item.setOrderItemId(orderItemId);
        item.setProductId(Long.valueOf(dto.getId()));
        item.setProductName(dto.getName());
        item.setUnitPrice(Double.parseDouble(dto.getPrice()));
        item.setTotalPrice(Double.parseDouble(dto.getPrice()) * Double.parseDouble(dto.getQuantity()));
        item.setQuantity(Integer.parseInt(dto.getQuantity()));
        item.setCreatedAt(LocalDateTime.now());
        item.setOrder(order); // 🔥 important for relationship

        return item;
    }

    public List<Order> getAllOrders() {
        List<Order> orderList = orderRepository.findAll();
        return orderList;
    }

    public void updateOrderStatus(Long orderId, String status) {
        Order order = new Order();
        order.setOrderId(orderId);
        order.setOrderStatus(OrderStatus.valueOf(status).name());
        orderRepository.save(order);
    }
}