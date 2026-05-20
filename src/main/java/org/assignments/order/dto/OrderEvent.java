package org.assignments.order.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEvent {

    // ── Tracing ──────────────────────────────────────────────────────────────
    /** Shared across every service that handles this order lifecycle. */
    private String correlationId;

    /** The Kafka topic this event was published to (set by producer). */
    private String eventSource;

    @JsonSerialize(using = LocalDateTimeSerializer.class)
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /** ISO-8601 timestamp of when the event was produced. */
    private LocalDateTime eventTimestamp;


    // ── Order data ────────────────────────────────────────────────────────────
    private String orderId;
    private String customerId;
    private String customerEmail;
    private String status;
    private BigDecimal totalAmount;
    private String currency;
    private List<OrderItemDetail> items;

}
