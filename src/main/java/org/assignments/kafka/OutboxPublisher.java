package org.assignments.kafka;

import org.assignments.entity.OutboxEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.assignments.repository.OutboxRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {
    @Autowired
    OutboxRepository outboxRepository;

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.order-topic}")
    private String KAFKA_ORDER_TOPIC;

    @Scheduled(fixedDelay = 5000)
    public void publish(){
        List<OutboxEvent> events = outboxRepository.findByStatus("NEW");

        for(OutboxEvent event : events){
            try {
                kafkaTemplate.send(KAFKA_ORDER_TOPIC, event.getPayload());

                event.setStatus("SENT");
            } catch (Exception e){
                log.info(e.getMessage());
                event.setStatus("FAILED");
            }


            outboxRepository.save(event);

            log.info("Event published to kafka {}",event.getPayload());
        }
    }
}