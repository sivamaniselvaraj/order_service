package org.assignments.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Value;

@Data
@Value
@AllArgsConstructor
public class OrderResponse {
    Long orderId;
    String orderStatus;
}
