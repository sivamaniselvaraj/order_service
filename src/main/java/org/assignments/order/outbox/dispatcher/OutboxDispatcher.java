package org.assignments.order.outbox.dispatcher;

import org.assignments.order.outbox.entity.OutboxMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.assignments.order.outbox.repository.OutboxRepository;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxDispatcher {


    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OrderService orderService;

    private static final String KAFKA_PREFIX = "KAFKA:";

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Dispatch one outbox message. Throws on failure.
     */
    public void dispatch(OutboxMessage message) {
        String destination = message.getDestination();

        if (destination.startsWith(KAFKA_PREFIX)) {
            String topic = destination.substring(KAFKA_PREFIX.length());
            dispatchKafka(message, topic);
        } else {
            throw new IllegalArgumentException("Unknown destination prefix: " + destination);
        }
    }

    // ── Kafka ─────────────────────────────────────────────────────────────────

    private void dispatchKafka(OutboxMessage outbox, String topic) {
//        OrderEvent event;
//        try {
//            event = objectMapper.readValue(outbox.getPayload(), OrderEvent.class);
//            log.info("event string {}", event.toString());
//        } catch (Exception e) {
//            throw new RuntimeException("Failed to deserialise Kafka payload id=" + outbox.getId(), e);
//        }

        Message<String> kafkaMsg = MessageBuilder
                .withPayload(outbox.getPayload())
                .setHeader(KafkaHeaders.TOPIC, topic)
                .setHeader(KafkaHeaders.KEY, outbox.getAggregateId())
                .setHeader("X-Correlation-Id", outbox.getCorrelationId())
                .setHeader("X-Outbox-Id", String.valueOf(outbox.getId()))
                .setHeader("X-Event-Type", outbox.getEventType())
                .build();

        try {
            var result = kafkaTemplate.send(kafkaMsg).get();   // block to detect send errors
            log.info("[OUTBOX-DISPATCHER] Kafka OK id={} topic={} partition={} offset={} correlationId={}",
                    outbox.getId(), topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset(),
                    outbox.getCorrelationId());
        } catch (Exception e) {
            throw new RuntimeException("Kafka send failed for outbox id=" + outbox.getId(), e);
        }
    }
}