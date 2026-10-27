package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;



@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {

}
