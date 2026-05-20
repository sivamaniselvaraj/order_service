package org.assignments.order.enums;

import lombok.Getter;

import java.util.Objects;


public enum OrderStatus {


    PENDING,
    PROCESSING,
    CONFIRMED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    FAILED;


    // Enum constructor must be private
    private OrderStatus() {
    }

}
