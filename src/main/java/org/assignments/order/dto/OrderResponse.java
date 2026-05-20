package org.assignments.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.assignments.order.entity.Order;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Order response payload")
public class OrderResponse {
    String orderId;
    String orderStatus;

    public static OrderResponse from(Order order) {
        return OrderResponse.builder()
//                .id(order.getId())
                .orderId(order.getOrderId())
//                .correlationId(order.getCorrelationId())
//                .customerId(order.getCustomerId())
//                .customerEmail(order.getCustomerEmail())
                .orderStatus(order.getStatus().name())
//                .totalAmount(order.getTotalAmount())
//                .currency(order.getCurrency())
//                .items(order.getItems().stream()
//                        .map(OrderItemResponse::from)
//                        .collect(Collectors.toList()))
//                .shippingAddress(order.getShippingAddress())
//                .notes(order.getNotes())
//                .createdAt(order.getCreatedAt())
//                .updatedAt(order.getUpdatedAt())
                .build();
    }
}
