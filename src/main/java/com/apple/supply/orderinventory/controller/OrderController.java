package com.apple.supply.orderinventory.controller;

import com.apple.supply.orderinventory.dto.CreateOrderItemRequest;
import com.apple.supply.orderinventory.dto.CreateOrderRequest;
import com.apple.supply.orderinventory.dto.OrderItemResponse;
import com.apple.supply.orderinventory.dto.OrderPageResponse;
import com.apple.supply.orderinventory.dto.OrderResponse;
import com.apple.supply.orderinventory.dto.UpdateOrderItemQuantityRequest;
import com.apple.supply.orderinventory.dto.UpdateOrderStatusRequest;

import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.OrderStatus;

import com.apple.supply.orderinventory.service.OrderService;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // Create a new order in DRAFT status.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse createDraftOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        Order savedOrder = orderService.createDraftOrder(request);

        return orderService.getOrderById(savedOrder.getId());
    }

    // Retrieve a paginated list of order summaries.
    @GetMapping
    public OrderPageResponse getOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long customerId,
            @RequestParam(required = false) OrderStatus status) {

        return orderService.getOrders(page, size, customerId, status);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
            @Parameter(
                    name = "orderId",
                    in = ParameterIn.PATH,
                    required = true,
                    schema = @Schema(type = "integer", format = "int64")
            )
            @PathVariable("orderId") Long orderId) {

        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    @PatchMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request) {

        return ResponseEntity.ok(
                orderService.updateOrderStatus(orderId, request));
    }
    
    @PostMapping("/{orderId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderItemResponse addOrderItem(
            @PathVariable("orderId") Long orderId,
            @Valid @RequestBody CreateOrderItemRequest request) {

        return orderService.addOrderItem(orderId, request);
    }


    @PatchMapping("/{orderId}/items/{itemId}/quantity")
    public ResponseEntity<OrderItemResponse> updateOrderItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemQuantityRequest request) {

        OrderItemResponse response =
                orderService.updateOrderItemQuantity(
                        orderId,
                        itemId,
                        request.getQuantity());

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<Void> deleteOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId) {

        orderService.deleteOrderItem(orderId, orderItemId);

        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{orderId}/items")
    public ResponseEntity<List<OrderItemResponse>> getOrderItems(
            @PathVariable Long orderId) {

        List<OrderItemResponse> items =
                orderService.getOrderItems(orderId);

        return ResponseEntity.ok(items);
    }


    @GetMapping("/{orderId}/items/{orderItemId}")
    public ResponseEntity<OrderItemResponse> getOrderItemById(
            @PathVariable Long orderId,
            @PathVariable Long orderItemId) {

        OrderItemResponse item =
                orderService.getOrderItemById(
                        orderId, orderItemId);

        return ResponseEntity.ok(item);
    }

    @GetMapping("/items/{orderItemId}")
    public ResponseEntity<OrderItemResponse> getOrderItemById(
            @PathVariable Long orderItemId) {

        OrderItemResponse item =
                orderService.getOrderItemById(orderItemId);

        return ResponseEntity.ok(item);
    }

}
