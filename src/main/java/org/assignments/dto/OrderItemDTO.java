package org.assignments.dto;

import lombok.Value;

@Value
public class OrderItemDTO {
    private String id;
    private String name;
    private String price;
    private String quantity;
}
