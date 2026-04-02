package org.assignments.enums;

import lombok.Getter;

@Getter
public enum OrderStatus {
    CREATED("Order Created"),
    PENDING("Order Pending"),
    CANCELLED("Order Cancelled"),
    COMPLETED("Order Completed"),
    FAILED("Order Failed");

    private final String status;

    // Enum constructor must be private
    private OrderStatus(String status) {
        this.status = status;
    }

}
