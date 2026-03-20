package org.assignments.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
public class OutboxEvent {

    @Id
    @GeneratedValue
    private Long id;

    private Long aggregateId;

    private String eventType;

    @Lob
    private String payload;

    private String status;

    private LocalDateTime createdAt;
}