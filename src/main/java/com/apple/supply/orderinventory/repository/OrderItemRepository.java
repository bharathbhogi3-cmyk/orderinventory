package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.OrderItem;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Pageable;


@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem,Long> {


    Page<OrderItem> findByOrder_Id(Long orderId, Pageable pageable);


}
