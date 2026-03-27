package org.assignments.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name="OrderItems")
@Data
public class OrderItem {

    @Id
    private Long orderItemId;

    private Long productId;

    private String productName;

    private int quantity;

    private Double unitPrice;

    private Double totalPrice;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name="order_id")
    @JsonBackReference
    private Order order;
}
