package org.assignments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

@Data
@Value
@AllArgsConstructor
@Schema(description = "Order response payload")
public class OrderResponse {
    Long orderId;
    String orderStatus;
}
