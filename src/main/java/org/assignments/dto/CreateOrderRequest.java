package org.assignments.dto;

import lombok.Data;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Data
@Value
public class CreateOrderRequest {
    Long customerId;
    BigDecimal totalAmount;
    String currency;
    List<OrderItemDTO> items;
}
