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
import lombok.extern.slf4j.Slf4j;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.apple.supply.orderinventory.dto.OrderPageResponse;
import com.apple.supply.orderinventory.dto.OrderSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.apple.supply.orderinventory.dto.OrderResponse;
import com.apple.supply.orderinventory.dto.OrderItemResponse;
import com.apple.supply.orderinventory.dto.UpdateOrderRequest;
import com.apple.supply.orderinventory.dto.UpdateOrderItemRequest;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final OrderItemRepository orderItemRepository;

    @Transactional
    public Order createDraftOrder(CreateOrderRequest request) {

        log.info("Creating draft order for customer ID: {}",
                request.getCustomerId());

        // 1. Find the customer.
        Customer customer = customerRepository
                .findById(request.getCustomerId())
                .orElseThrow(() ->
                        new CustomerNotFoundException(
                                "Customer not found: "
                                        + request.getCustomerId()));

        // Validate order items.
        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException(
                    "Order must contain at least one item."
            );
        }

        // 2. Create a new draft order.
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.now());
        order.setStatus(OrderStatus.DRAFT);

        // 3. Track order items by product ID to avoid duplicates.
        Map<Long, OrderItem> itemsByProductId = new HashMap<>();

        // 4. Process each requested item.
        for (CreateOrderItemRequest itemRequest : request.getItems()) {

            // Validate quantity.
            if (itemRequest.getQuantity() == null
                    || itemRequest.getQuantity() <= 0) {
                throw new IllegalArgumentException(
                        "Quantity must be greater than zero.");
            }

            Long productId = itemRequest.getProductId();

            if (productId == null) {
                throw new IllegalArgumentException(
                        "Product ID is required.");
            }

            // Find the product.
            Product product = productRepository.findById(productId)
                    .orElseThrow(() ->
                            new ProductNotFoundException(
                                    "Product not found: " + productId));

            // Only active products can be ordered.
            if (!product.isActive()) {
                throw new BusinessConflictException(
                        "Product is inactive: " + product.getId());
            }

            // Capture the product's current price.
            BigDecimal unitPrice = product.getUnitPrice();

            if (unitPrice == null) {
                throw new IllegalArgumentException(
                        "Product has no unit price: " + product.getId());
            }

            // Check whether this product already has an order item.
            OrderItem existingItem = itemsByProductId.get(productId);

            if (existingItem != null) {

                // Same product: add to the existing quantity.
                existingItem.setQuantity(
                        Math.addExact(
                                existingItem.getQuantity(),
                                itemRequest.getQuantity()));

            } else {

                // New product: create one order item.
                OrderItem orderItem = new OrderItem();
                orderItem.setProduct(product);
                orderItem.setQuantity(itemRequest.getQuantity());
                orderItem.setUnitPrice(unitPrice);

                // Maintain both sides of the relationship.
                order.addOrderItem(orderItem);

                // Remember this item for subsequent duplicate products.
                itemsByProductId.put(productId, orderItem);
            }
        }

        // 5. Calculate the order total.
        recalculateOrderTotal(order);

        // 6. Save the order and its items.
        Order savedOrder = orderRepository.save(order);

        log.info(
                "Draft order created successfully. Order ID: {}, Customer ID: {}",
                savedOrder.getId(),
                request.getCustomerId());

        return savedOrder;
    }


    @Transactional(readOnly = true)
    public OrderPageResponse getOrders(
            int page,
            int size,
            Long customerId,
            OrderStatus status) {

        log.info(
                "Fetching orders. Page: {}, Size: {}, Customer ID: {}, Status: {}",
                page, size, customerId, status);

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

        log.info("Successfully retrieved {} orders", orders.getTotalElements());

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

        log.info("Fetching order details for order ID: {}", orderId);

        // 1. Find the order or throw a 404 exception.
        Order order = findOrderById(orderId);

        log.info("Order retrieved successfully. Order ID: {}, Status: {}",
                order.getId(), order.getStatus());

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


    private void applyOrderStatus(
            Order order,
            OrderStatus requestedStatus) {

        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == requestedStatus) {
            return;
        }

        if (currentStatus == OrderStatus.DRAFT
                && requestedStatus == OrderStatus.CONFIRMED) {

            // Step 1: An order must contain at least one item.
            if (order.getOrderItems().isEmpty()) {
                throw new BusinessConflictException(
                        "Cannot confirm an order without items.");
            }

            // Step 2: Aggregate quantities by product ID.
            // TreeMap keeps product IDs sorted to reduce deadlock risk.
            Map<Long, Integer> requiredQuantities = new TreeMap<>();

            for (OrderItem item : order.getOrderItems()) {

                Long productId = item.getProduct().getId();

                requiredQuantities.merge(
                        productId,
                        item.getQuantity(),
                        (existingQuantity, newQuantity) ->
                                Math.addExact(existingQuantity, newQuantity)
                );
            }

            // Step 3: Lock each unique product and validate its stock.
            Map<Long, Product> lockedProducts = new TreeMap<>();

            for (Map.Entry<Long, Integer> entry
                    : requiredQuantities.entrySet()) {

                Long productId = entry.getKey();
                Integer requiredQuantity = entry.getValue();

                Product product = productRepository
                        .findByIdForUpdate(productId)
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product not found: " + productId));

                if (!product.isActive()) {
                    throw new BusinessConflictException(
                            "Product is inactive: " + productId);
                }

                if (product.getQuantityAvailable() < requiredQuantity) {
                    throw new BusinessConflictException(
                            "Insufficient stock for product: " + productId
                                    + ". Required: " + requiredQuantity
                                    + ", Available: "
                                    + product.getQuantityAvailable());
                }

                // Keep the locked entity for the deduction step.
                lockedProducts.put(productId, product);
            }

            // Step 4: Deduct stock only after every product passes validation.
            for (Map.Entry<Long, Integer> entry
                    : requiredQuantities.entrySet()) {

                Long productId = entry.getKey();
                Integer requiredQuantity = entry.getValue();

                Product product = lockedProducts.get(productId);

                product.setQuantityAvailable(
                        product.getQuantityAvailable() - requiredQuantity);

                productRepository.save(product);
            }

            // Step 5: Update the order status.
            order.setStatus(OrderStatus.CONFIRMED);

        } else if (currentStatus == OrderStatus.DRAFT
                && requestedStatus == OrderStatus.CANCELLED) {

            order.setStatus(OrderStatus.CANCELLED);

        } else if (currentStatus == OrderStatus.CONFIRMED
                && requestedStatus == OrderStatus.FULFILLED) {

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

        log.info("Order ID: {} status updated successfully to {}",
                order.getId(), order.getStatus());
    }


    @Transactional
    public void deleteOrderItem(Long orderId, Long orderItemId) {

        log.info("Deleting order item ID: {} from order ID: {}",
                orderItemId, orderId);

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

        log.info("Order item deleted successfully. Item ID: {}, Order ID: {}",
                orderItemId, orderId);
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

        log.info("Fetching order item with ID: {}", orderItemId);

        // 1. Find the order.
        Order order = findOrderById(orderId);

        // 2. Find the item within that specific order.
        OrderItem item = findOrderItemById(order, orderItemId);

        log.info("Order item retrieved successfully. Item ID: {}",
                item.getId());

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

    /*Same product, different quantities: the quantities are added together
     Existing item ID: updates that item's quantity; its product cannot be changed.
     Omitted existing item ID: removes that persisted item from the order.
      New item with id: null: merges into the existing product item if present; otherwise creates one.
       Status update: runs after item changes, so confirmation uses the updated quantities.
        Empty items list: retains our current behavior of skipping item processing.*/
    /*
     * Test cases:
     *
     * 1. {"status": "CONFIRMED"}
     *    -> Status update proceeds through the existing confirmation
     *       and stock-validation logic.
     *
     * 2. {"items": null}
     *    -> Existing order items remain unchanged.
     *
     * 3. {"items": [...]}
     *    -> Existing item-update logic runs.
     *
     * 4. {}
     *    -> No changes are requested.
     *
     * 5. {"items": []}
     *    -> Reject the request with 409 Conflict because an order
     *       must contain at least one item.
     */
    @Transactional
    public OrderResponse updateOrder(
            Long orderId,
            UpdateOrderRequest request) {

        log.info("Processing combined order update for order ID: {}", orderId);

        // 1. Find the existing order.
        Order order = findOrderById(orderId);

        // Reject an explicitly empty item list.
        if (request.getItems() != null
                && request.getItems().isEmpty()) {

            throw new BusinessConflictException(
                    "Order items cannot be empty. "
                            + "An order must contain at least one item.");
        }

        // 2. Reject duplicate existing order item IDs.
        if (request.getItems() != null) {
            Set<Long> itemIds = new HashSet<>();

            for (UpdateOrderItemRequest item : request.getItems()) {
                if (item.getId() != null
                        && !itemIds.add(item.getId())) {
                    throw new IllegalArgumentException(
                            "Duplicate order item ID: " + item.getId());
                }
            }
        }

        // 3. Process item changes before changing the order status.
        if (request.getItems() != null && !request.getItems().isEmpty()) {

            if (order.getStatus() != OrderStatus.DRAFT) {
                throw new BusinessConflictException(
                        "Items can only be modified in draft orders.");
            }

            Set<Long> requestedItemIds = new HashSet<>();
            Map<Long, OrderItem> itemsByProductId = new HashMap<>();

            // 4. Process existing items first.
            for (UpdateOrderItemRequest itemRequest : request.getItems()) {

                if (itemRequest.getQuantity() == null
                        || itemRequest.getQuantity() <= 0) {
                    throw new IllegalArgumentException(
                            "Quantity must be greater than zero.");
                }

                if (itemRequest.getProductId() == null) {
                    throw new IllegalArgumentException(
                            "Product ID is required.");
                }

                if (itemRequest.getId() == null) {
                    continue;
                }

                OrderItem existingItem = findOrderItemById(
                        order, itemRequest.getId());

                if (!existingItem.getProduct().getId()
                        .equals(itemRequest.getProductId())) {
                    throw new IllegalArgumentException(
                            "Product ID cannot be changed for an existing "
                                    + "order item.");
                }

                requestedItemIds.add(itemRequest.getId());

                Long productId = itemRequest.getProductId();
                OrderItem retainedItem = itemsByProductId.get(productId);

                if (retainedItem == null) {

                    // First existing item for this product.
                    existingItem.setQuantity(itemRequest.getQuantity());
                    itemsByProductId.put(productId, existingItem);

                } else {

                    // Another existing item represents the same product.
                    retainedItem.setQuantity(
                            Math.addExact(
                                    retainedItem.getQuantity(),
                                    itemRequest.getQuantity()));

                    order.removeOrderItem(existingItem);
                }
            }

            // 5. Remove persisted items omitted from the request.
            List<OrderItem> itemsToRemove = order.getOrderItems().stream()
                    .filter(item -> item.getId() != null)
                    .filter(item -> !requestedItemIds.contains(item.getId()))
                    .toList();

            for (OrderItem item : itemsToRemove) {
                order.removeOrderItem(item);
            }

            // 6. Process new items and merge duplicate products.
            for (UpdateOrderItemRequest itemRequest : request.getItems()) {

                if (itemRequest.getId() != null) {
                    continue;
                }

                // Validate new-item fields as well.
                if (itemRequest.getQuantity() == null
                        || itemRequest.getQuantity() <= 0) {
                    throw new IllegalArgumentException(
                            "Quantity must be greater than zero.");
                }

                if (itemRequest.getProductId() == null) {
                    throw new IllegalArgumentException(
                            "Product ID is required.");
                }

                Long productId = itemRequest.getProductId();

                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new ProductNotFoundException(
                                "Product not found with ID: " + productId));

                if (!product.isActive()) {
                    throw new BusinessConflictException(
                            "Product is inactive: " + product.getId());
                }

                BigDecimal unitPrice = product.getUnitPrice();

                if (unitPrice == null) {
                    throw new IllegalArgumentException(
                            "Product has no unit price: " + product.getId());
                }

                OrderItem retainedItem = itemsByProductId.get(productId);

                if (retainedItem != null) {

                    // Same product already exists: combine quantities.
                    retainedItem.setQuantity(
                            Math.addExact(
                                    retainedItem.getQuantity(),
                                    itemRequest.getQuantity()));

                } else {

                    // Product not yet in the order: create a new item.
                    OrderItem newItem = new OrderItem();
                    newItem.setProduct(product);
                    newItem.setQuantity(itemRequest.getQuantity());
                    newItem.setUnitPrice(unitPrice);

                    order.addOrderItem(newItem);
                    itemsByProductId.put(productId, newItem);
                }
            }

            // 7. Recalculate the order total.
            recalculateOrderTotal(order);
        }

        // 8. Apply status changes after item changes.
        if (request.getStatus() != null
                && request.getStatus() != order.getStatus()) {
            applyOrderStatus(order, request.getStatus());
        }

        // 9. Save and return the updated order.
        Order savedOrder = orderRepository.save(order);

        var items = savedOrder.getOrderItems().stream()
                .map(this::toOrderItemResponse)
                .toList();

        return new OrderResponse(
                savedOrder.getId(),
                savedOrder.getCustomer().getId(),
                savedOrder.getOrderDateTime(),
                savedOrder.getTotalAmount(),
                savedOrder.getStatus(),
                items);
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
