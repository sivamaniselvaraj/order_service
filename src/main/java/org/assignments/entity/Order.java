package org.assignments.entity;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name="Orders", uniqueConstraints = @UniqueConstraint(columnNames = "idempotency_key"))
@Data
@DynamicUpdate
public class Order {

    @Id
    private Long orderId;

    private Long customerId;

    private String orderStatus;

    private BigDecimal totalAmount;

    private String currency;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @JsonManagedReference
    @OneToMany(mappedBy="order", cascade= CascadeType.ALL)
    private List<OrderItem> items;
}

