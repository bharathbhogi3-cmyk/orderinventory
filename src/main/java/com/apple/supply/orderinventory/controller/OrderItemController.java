package com.apple.supply.orderinventory.controller;

import com.apple.supply.orderinventory.dto.OrderItemResponse;
import com.apple.supply.orderinventory.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order-items")
public class OrderItemController {

    private final OrderService orderService;

    public OrderItemController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<Page<OrderItemResponse>> getOrderItems(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long orderId) {

        return ResponseEntity.ok(
                orderService.getOrderItems(page, size, orderId)
        );
    }

    @GetMapping("/{orderItemId}")
    public ResponseEntity<OrderItemResponse> getOrderItemById(
            @PathVariable Long orderItemId) {

        return ResponseEntity.ok(
                orderService.getOrderItemById(orderItemId)
        );
    }
}