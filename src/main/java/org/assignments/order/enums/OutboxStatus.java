package org.assignments.order.enums;

public enum OutboxStatus {
    PENDING,    // waiting to be dispatched
    PUBLISHED,  // successfully sent
    FAILED      // exhausted retries
}