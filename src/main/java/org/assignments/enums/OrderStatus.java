package org.assignments.enums;

public enum OrderStatus {
    ORDER_CREATED("Order Created"),
    PROCESSING_FAILED("Order Processing"),
    ORDER_CANCELLED("Order Cancelled"),
    ORDER_COMPLETED("Order Completed"),
    ORDER_FAILED("Order Failed");

    private String status;

    // Enum constructor must be private
    private OrderStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return this.status;
    }
}
