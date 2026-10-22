package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.OrderStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.apple.supply.orderinventory.model.OrderItem;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("""
        SELECT o FROM Order o
        WHERE (:customerId IS NULL OR o.customer.id = :customerId)
          AND (:status IS NULL OR o.status = :status)
        """)
    Page<Order> findOrders(
            @Param("customerId") Long customerId,
            @Param("status") OrderStatus status,
            Pageable pageable
    );
}
