package com.apple.supply.orderinventory.service;

import com.apple.supply.orderinventory.dto.CreateOrderItemRequest;
import com.apple.supply.orderinventory.dto.CreateOrderRequest;
import com.apple.supply.orderinventory.exception.*;
import com.apple.supply.orderinventory.model.Customer;
import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.OrderItem;
import com.apple.supply.orderinventory.model.OrderStatus;
import com.apple.supply.orderinventory.model.Product;
import com.apple.supply.orderinventory.repository.CustomerRepository;
import com.apple.supply.orderinventory.repository.OrderItemRepository;
import com.apple.supply.orderinventory.repository.OrderRepository;
import com.apple.supply.orderinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.apple.supply.orderinventory.dto.OrderPageResponse;
import com.apple.supply.orderinventory.dto.OrderSummaryResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.apple.supply.orderinventory.dto.OrderResponse;
import com.apple.supply.orderinventory.dto.OrderItemResponse;
import com.apple.supply.orderinventory.dto.UpdateOrderStatusRequest;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createDraftOrder(CreateOrderRequest request) {

        // 1. Find the customer in the database.
        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer not found: "
                                        + request.getCustomerId()));

        // 2. Create a new order.
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.now());
        order.setStatus(OrderStatus.DRAFT);

        // 4. Process each requested order item.
        for (CreateOrderItemRequest itemRequest
                : request.getItems()) {

            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() ->
                            new ProductNotFoundException(
                                    "Product not found: "
                                            + itemRequest.getProductId()));

            // Only active products can be ordered.
            if (!product.isActive()) {
                throw new IllegalArgumentException(
                        "Product is inactive: " + product.getId());
            }

            // Quantity must be positive.
            if (itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero.");
            }

            // Capture the product's current price.
            BigDecimal unitPrice = product.getUnitPrice();

            if (unitPrice == null) {
                throw new IllegalArgumentException(
                        "Product has no unit price: " + product.getId());
            }

            // Create the order item.
            OrderItem orderItem = new OrderItem();
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());
            orderItem.setUnitPrice(unitPrice);

            // Maintain both sides of the relationship.
            order.addOrderItem(orderItem);
        }

        // 5. Store the calculated total.
        recalculateOrderTotal(order);

        // 6. Save the order and its items.
        // The configured PERSIST cascade saves the new items.
        return orderRepository.save(order);
    }


    @Transactional(readOnly = true)
    public OrderPageResponse getOrders(
            int page,
            int size,
            Long customerId,
            OrderStatus status) {

        // Validate pagination inputs.
        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100.");
        }

        // Validate the customer before filtering orders.
        if (customerId != null
                && !customerRepository.existsById(customerId)) {

            throw new CustomerNotFoundException(
                    "Customer not found with ID: " + customerId);
        }

        // Tell Spring Data JPA which page to retrieve.
        Pageable pageable = PageRequest.of(page, size);

        // Retrieve the requested orders from the database.
        Page<Order> orders = orderRepository.findOrders(
                customerId, status, pageable);

        // Convert each Order entity into a summary DTO.
        Page<OrderSummaryResponse> summaries =
                orders.map(order -> new OrderSummaryResponse(
                        order.getId(),
                        order.getCustomer().getId(),
                        order.getOrderDateTime(),
                        order.getTotalAmount(),
                        order.getStatus()
                ));

        // Return the results and pagination information.
        return new OrderPageResponse(
                summaries.getContent(),
                summaries.getNumber(),
                summaries.getSize(),
                summaries.getTotalElements(),
                summaries.getTotalPages(),
                summaries.isFirst(),
                summaries.isLast()
        );
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long orderId) {

        // 1. Find the order or throw a 404 exception.
        Order order = findOrderById(orderId);

        // 2. Convert each order item into its response DTO.
        var items = order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        // 3. Return the complete order response.
        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getOrderDateTime(),
                order.getTotalAmount(),
                order.getStatus(),
                items
        );
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            UpdateOrderStatusRequest request) {

        Order order = findOrderById(orderId);

        OrderStatus currentStatus = order.getStatus();
        OrderStatus requestedStatus = request.getStatus();

        if (requestedStatus == null) {
            throw new IllegalArgumentException(
                    "Status is required.");
        }

        if (currentStatus == requestedStatus) {
            throw new BusinessConflictException(
                    "Order is already in status " + currentStatus);
        }

        if (currentStatus == OrderStatus.DRAFT
                && requestedStatus == OrderStatus.CONFIRMED) {

            // Lock products in a consistent order to reduce deadlock risk.
            var items = order.getOrderItems().stream()
                    .sorted((a, b) -> Long.compare(
                            a.getProduct().getId(),
                            b.getProduct().getId()))
                    .toList();

            // First validate all items before changing any stock.
            for (OrderItem item : items) {
                Product product = productRepository
                        .findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product not found: "
                                        + item.getProduct().getId()));

                if (!product.isActive()) {
                    throw new BusinessConflictException(
                            "Product is inactive: " + product.getId());
                }

                if (product.getQuantityAvailable() < item.getQuantity()) {
                    throw new BusinessConflictException(
                            "Insufficient stock for product: "
                                    + product.getId());
                }
            }

            // All items passed validation. Deduct stock.
            for (OrderItem item : items) {
                Product product = productRepository
                        .findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product not found: "
                                        + item.getProduct().getId()));

                product.setQuantityAvailable(
                        product.getQuantityAvailable() - item.getQuantity());

                productRepository.save(product);
            }

            order.setStatus(OrderStatus.CONFIRMED);

        } else if (currentStatus == OrderStatus.DRAFT
                && requestedStatus == OrderStatus.CANCELLED) {

            order.setStatus(OrderStatus.CANCELLED);

        } else if (currentStatus == OrderStatus.CONFIRMED
                && requestedStatus == OrderStatus.FULFILLED) {

            // Stock was already deducted during confirmation.
            order.setStatus(OrderStatus.FULFILLED);

        } else if (currentStatus == OrderStatus.CONFIRMED
                && requestedStatus == OrderStatus.CANCELLED) {

            var items = order.getOrderItems().stream()
                    .sorted((a, b) -> Long.compare(
                            a.getProduct().getId(),
                            b.getProduct().getId()))
                    .toList();

            for (OrderItem item : items) {
                Product product = productRepository
                        .findByIdForUpdate(item.getProduct().getId())
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product not found: "
                                        + item.getProduct().getId()));

                product.setQuantityAvailable(
                        product.getQuantityAvailable() + item.getQuantity());

                productRepository.save(product);
            }

            order.setStatus(OrderStatus.CANCELLED);

        } else {
            throw new BusinessConflictException(
                    "Cannot change order status from "
                            + currentStatus + " to " + requestedStatus);
        }

        orderRepository.save(order);

        // Build the response inside the transaction.
        var items = order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCustomer().getId(),
                order.getOrderDateTime(),
                order.getTotalAmount(),
                order.getStatus(),
                items);
    }



    @Transactional
    public OrderItemResponse addOrderItem(
            Long orderId,
            CreateOrderItemRequest request) {

        // 1. Find the existing order.
        Order order = findOrderById(orderId);

        // 2. Only draft orders can be modified.
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessConflictException(
                    "Items can only be added to draft orders.");
        }

        // 3. Validate the requested quantity.
        if (request.getQuantity() == null
                || request.getQuantity() <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        // 4. Find the requested product.
        Product product = productRepository
                .findById(request.getProductId())
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with ID: "
                                + request.getProductId()));

        // 5. Only active products can be ordered.
        if (!product.isActive()) {
            throw new BusinessConflictException(
                    "Product is inactive: " + product.getId());
        }

        // 6. Check whether this product already exists
        //    in the current order.
        OrderItem item = order.getOrderItems().stream()
                .filter(existingItem ->
                        existingItem.getProduct().getId()
                                .equals(product.getId()))
                .findFirst()
                .orElse(null);

        if (item != null) {

            // Product already exists: update its quantity.
            // Preserve its original unit price.
            item.setQuantity(request.getQuantity());

        } else {

            // Product does not exist in this order:
            // create a new order item.
            BigDecimal unitPrice = product.getUnitPrice();

            if (unitPrice == null) {
                throw new IllegalArgumentException(
                        "Product has no unit price: "
                                + product.getId());
            }

            item = new OrderItem();
            item.setProduct(product);
            item.setQuantity(request.getQuantity());
            item.setUnitPrice(unitPrice);

            // Maintain both sides of the relationship.
            order.addOrderItem(item);

            // Explicitly saving the new item and retaining the saved entity.
            item = orderItemRepository.saveAndFlush(item);
        }

        // 7. Recalculate the order total.
        recalculateOrderTotal(order);

        // 8. Save the order and its changes.
        orderRepository.save(order);

        // 9. Return the added or updated order item.
        return toOrderItemResponse(item);
    }


    @Transactional
    public OrderItemResponse updateOrderItemQuantity(
            Long orderId,
            Long itemId,
            Integer quantity) {

        // 1. Validate the requested quantity.
        if (quantity == null || quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        // 2. Find the order.
        Order order = findOrderById(orderId);

        // 3. Only draft orders can be modified.
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessConflictException(
                    "Items can only be modified in draft orders.");
        }

        // 4. Find the item within this order.
        OrderItem item = findOrderItemById(order, itemId);

        // 5. Update only the quantity.
        // Preserve the original unit price.
        item.setQuantity(quantity);

        // 6. Recalculate the order total.
        recalculateOrderTotal(order);

        // 7. Save the order and its updated item.
        orderRepository.save(order);

        // 8. Return the updated item.
        return toOrderItemResponse(item);
    }



    @Transactional
    public void deleteOrderItem(Long orderId, Long orderItemId) {

        // 1. Find the order.
        Order order = findOrderById(orderId);

        // 2. Only DRAFT orders can be modified.
        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessConflictException(
                    "Items can only be deleted from draft orders.");
        }

        // 3. Find the item within the specified order.
        OrderItem item = findOrderItemById(order, orderItemId);

        // 4. Prevent deletion of the last remaining item.
        if (order.getOrderItems().size() <= 1) {
            throw new BusinessConflictException(
                    "Cannot delete the last remaining item from an order.");
        }

        // 5. Remove the item from the order.
        order.getOrderItems().remove(item);

        // 6. Delete the item from the database.
        orderItemRepository.delete(item);

        // 7. Recalculate the order total.
        recalculateOrderTotal(order);
    }



    @Transactional(readOnly = true)
    public List<OrderItemResponse> getOrderItems(Long orderId) {

        // 1. Find the order.
        Order order = findOrderById(orderId);

        // 2. Convert each item into its response DTO.
        return order.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();
    }


    @Transactional(readOnly = true)
    public OrderItemResponse getOrderItemById(
            Long orderId,
            Long orderItemId) {

        // 1. Find the order.
        Order order = findOrderById(orderId);

        // 2. Find the item within that specific order.
        OrderItem item = findOrderItemById(order, orderItemId);

        // 3. Return the item details.
        return toOrderItemResponse(item);
    }


    @Transactional(readOnly = true)
    public OrderItemResponse getOrderItemById(Long orderItemId) {

        // Find the order item.
        OrderItem item = orderItemRepository.findById(orderItemId)
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Order item not found with ID: "
                                + orderItemId));

        // Return the response DTO.
        return toOrderItemResponse(item);
    }


    // Created Helper method to calculate the order Total
    private void recalculateOrderTotal(Order order) {

        BigDecimal totalAmount = order.getOrderItems().stream()
                .map(item -> item.getUnitPrice().multiply(
                        BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalAmount(totalAmount);
    }


    // Helper method for Order item response
    private OrderItemResponse toOrderItemResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProduct().getId(),
                item.getProduct().getName(),
                item.getQuantity(),
                item.getUnitPrice()
        );
    }


    // Helper method for find Order by ID
    private Order findOrderById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                        "Order not found with ID: " + orderId));
    }


    // Helper method for find OrderItem by ID
    private OrderItem findOrderItemById(Order order, Long orderItemId) {
        return order.getOrderItems().stream()
                .filter(item -> item.getId().equals(orderItemId))
                .findFirst()
                .orElseThrow(() -> new OrderItemNotFoundException(
                        "Order item " + orderItemId
                                + " not found in order " + order.getId()));
    }
}
