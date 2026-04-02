package org.assignments.enums;

import lombok.Getter;

import java.util.Objects;


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

    public String getStatus() {
        return this.status;
    }

    public static OrderStatus fromValue(String value)
            throws IllegalArgumentException {
        try {
            for(OrderStatus e : OrderStatus.values()){
                if(Objects.equals(value, e.status)) return e;
            }

        } catch(ArrayIndexOutOfBoundsException e) {
            throw new IllegalArgumentException("Unknown enum value :"+ value);
        }
        return null;
    }
}
