package org.assignments.entity;

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

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;

    private LocalDateTime createdAt;

    @ManyToOne
    @JoinColumn(name="order_id")
    private Order order;
}
