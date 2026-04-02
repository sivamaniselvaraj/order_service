package org.assignments.kafka;

import org.assignments.dto.OrderEvent;
import org.assignments.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assignments.enums.OrderStatus;
import org.assignments.repository.OrderRepository;
import org.assignments.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.assignments.repository.OutboxRepository;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    @Autowired
    OutboxRepository outboxRepository;

    @Autowired
    OrderService orderService;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.order-topic}")
    private String KAFKA_ORDER_TOPIC;

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Scheduled(fixedDelay = 10000)
    public void publish(){
        List<String> statusList = new ArrayList<String>();
        statusList.add("NEW");
        statusList.add("FAILED");

        List<OutboxEvent> events = outboxRepository.findByStatusIn(statusList);
        log.debug("events to published {}", events.size() );
        if(!events.isEmpty()) {
            for (OutboxEvent event : events) {
                    CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(KAFKA_ORDER_TOPIC, event.getEventId().toString(),event.getPayload());
                    future.whenComplete((result, ex) -> {
                        if (ex == null) {
                            log.info("Sent message=[" + event +
                                    "] with offset=[" + result.getRecordMetadata().offset() + "]");
                            event.setStatus("SENT");
                        } else {
                            log.info("Unable to send message=[" +
                                    event + "] due to : " + ex.getMessage());
                            event.setStatus("FAILED");
                        }
                        outboxRepository.save(event);
                        orderService.updateOrderStatus(event.getAggregateId(), OrderStatus.PENDING.getStatus());
                    });
                log.info("Event published to kafka {}", event.getPayload());
            }
        }
    }
}