package org.assignments.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="Orders")
@Data
public class Order {

    @Id
    private Long orderId;

    private Long customerId;

    private String orderStatus;

    private BigDecimal totalAmount;

    private String currency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy="order", cascade= CascadeType.ALL)
    private List<OrderItem> items;
}

