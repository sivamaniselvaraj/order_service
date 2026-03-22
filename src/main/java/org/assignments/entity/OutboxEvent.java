package org.assignments.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name="OutboxEvents")
@Data
public class OutboxEvent {

    @Id
    private Long eventId;

    private String aggregateType;
    private Long aggregateId;
    private String eventType;

    @Column(columnDefinition="json")
    private String payload;

    private String status;

    private LocalDateTime createdAt;

}