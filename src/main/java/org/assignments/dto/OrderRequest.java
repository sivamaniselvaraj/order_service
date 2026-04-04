package org.assignments.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;

@Data
@Value
@Schema(description = "Order request payload")
public class OrderRequest {
    Long customerId;
    BigDecimal totalAmount;
    String currency;
    List<OrderItemDTO> items;
}
