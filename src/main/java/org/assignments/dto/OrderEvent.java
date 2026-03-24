package org.assignments.dto;

import lombok.Data;

@Data
public class OrderEvent {
    private String orderId;
    private String status;
    private String correlationId;
    private String idempotencyKey;
}
