package org.assignments.order.repository;

import org.assignments.order.entity.Order;
import jakarta.transaction.Transactional;
import org.assignments.order.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByIdempotencyKey(String idempotencyKey);

    Optional<Order> findByOrderId(String orderId);

    Optional<Order> findByCorrelationId(String correlationId);

    Page<Order> findByCustomerId(String customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    boolean existsByOrderId(String orderId);
}
