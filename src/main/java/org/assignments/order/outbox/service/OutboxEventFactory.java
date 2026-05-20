package org.assignments.order.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.dto.OrderEvent;
import org.assignments.order.dto.OrderItemDetail;
import org.assignments.order.entity.Order;
import org.assignments.order.outbox.entity.OutboxMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the OutboxMessage rows that are written inside the order transaction.
 * Three rows per order:
 *   1. KAFKA → order.processing
 *   2. KAFKA → order.notification
 *   3. REST  → notification-service REST endpoint
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper = new ObjectMapper();;

    @Value("${ordering.kafka.topics.order-create-topic}")
    private String orderCreatedTopic;

    @Value("${ordering.kafka.topics.order-notification-topic}")
    private String notificationTopic;

//    @Value("${notification.service.base-url}")
//    private String notificationBaseUrl;

    public List<OutboxMessage> createOutboxMessages(Order order) {
        return List.of(
                buildKafkaProcessingMessage(order)
                //, buildKafkaNotificationMessage(order),
                //buildRestNotificationMessage(order)
        );
    }

    // ── Kafka: processing ─────────────────────────────────────────────────────

    private OutboxMessage buildKafkaProcessingMessage(Order order) {
        log.info("orderCreatedTopic {}", orderCreatedTopic);
        OrderEvent event = toOrderEvent(order, "KAFKA:" + orderCreatedTopic);
        return OutboxMessage.builder()
                .aggregateId(order.getOrderId())
                .aggregateType("ORDER")
                .correlationId(order.getCorrelationId())
                .eventType("ORDER_CREATED_PROCESSING")
                .destination("KAFKA:" + orderCreatedTopic)
                .payload(serialize(event))
                .build();
    }

    // ── Kafka: notification ───────────────────────────────────────────────────

    private OutboxMessage buildKafkaNotificationMessage(Order order) {
        OrderEvent event = toOrderEvent(order, "KAFKA:" + notificationTopic);
        return OutboxMessage.builder()
                .aggregateId(order.getOrderId())
                .aggregateType("ORDER")
                .correlationId(order.getCorrelationId())
                .eventType("ORDER_CREATED_NOTIFICATION")
                .destination("KAFKA:" + notificationTopic)
                .payload(serialize(event))
                .build();
    }


    // ── Helpers ───────────────────────────────────────────────────────────────

    private OrderEvent toOrderEvent(Order order, String source) {
        List<OrderItemDetail> items = order.getItems().stream()
                .map(i -> OrderItemDetail.builder()
                        .id(i.getProductId())
                        .name(i.getProductName())
                        .quantity(i.getQuantity())
                        .price(i.getUnitPrice())
                        .subtotal(i.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderEvent.builder()
                .correlationId(order.getCorrelationId())
                .eventSource(source)
                .eventTimestamp(LocalDateTime.now())
                .orderId(order.getOrderId())
                .customerId(order.getCustomerId())
                .customerEmail(order.getCustomerEmail())
                .status(order.getStatus().name())
                .totalAmount(order.getTotalAmount())
                .currency(order.getCurrency())
//                .shippingAddress(order.getShippingAddress())
//                .notes(order.getNotes())
                .items(items)
                .build();
    }

    private String serialize(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload", e);
        }
    }
}