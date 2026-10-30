package com.apple.supply.orderinventory.service;

import com.apple.supply.orderinventory.dto.*;
import com.apple.supply.orderinventory.exception.*;
import com.apple.supply.orderinventory.model.*;
import com.apple.supply.orderinventory.repository.CustomerRepository;
import com.apple.supply.orderinventory.repository.OrderItemRepository;
import com.apple.supply.orderinventory.repository.OrderRepository;
import com.apple.supply.orderinventory.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.time.LocalDateTime;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createDraftOrder_shouldCreateOrderSuccessfully() {

        // Arrange
        // 1. Create a customer
        Customer customer = new Customer();
        customer.setId(1L);

        // 2. Create a product
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);
        product.setQuantityAvailable(10);

        // 3. Create an order item request
        CreateOrderItemRequest itemRequest =
                new CreateOrderItemRequest();

        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        // 4. Create an order request
        CreateOrderRequest request = new CreateOrderRequest();

        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        // 5. Configure repository responses
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        assertEquals(1L, result.getCustomer().getId());
        assertEquals(1, result.getOrderItems().size());
        assertEquals(2, result.getOrderItems().get(0).getQuantity());
        assertEquals(
                new BigDecimal("79900.00"),
                result.getOrderItems().get(0).getUnitPrice());
        assertEquals(
                new BigDecimal("159800.00"),
                result.getTotalAmount());

        // Verify repository interactions
        verify(customerRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }


    @Test
    void createDraftOrder_shouldMergeDuplicateProductsSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);
        product.setQuantityAvailable(10);

        // First request for the same product
        CreateOrderItemRequest itemRequest1 = new CreateOrderItemRequest();
        itemRequest1.setProductId(1L);
        itemRequest1.setQuantity(2);

        // Second request for the same product
        CreateOrderItemRequest itemRequest2 = new CreateOrderItemRequest();
        itemRequest2.setProductId(1L);
        itemRequest2.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest1, itemRequest2));

        // Configure repository responses
        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.DRAFT, result.getStatus());

        // Duplicate product requests should create only one order item
        assertEquals(1, result.getOrderItems().size());

        // Quantities should be combined: 2 + 3 = 5
        assertEquals(5, result.getOrderItems().get(0).getQuantity());

        // Total: 5 × 79,900 = 3,99,500
        assertEquals(
                new BigDecimal("399500.00"),
                result.getTotalAmount()
        );

        // Verify repository interactions
        verify(customerRepository, times(1)).findById(1L);

        // Product is looked up for each request
        verify(productRepository, times(2)).findById(1L);

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenCustomerNotFound() {

        // Arrange
        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(999L);
        CreateOrderItemRequest itemRequest =
                new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                CustomerNotFoundException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository, times(1)).findById(999L);
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenProductNotFound() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(999L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).findById(999L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenProductIsInactive() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(false);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verify(productRepository).findById(1L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenQuantityIsZero() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(0);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenQuantityIsNegative() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(-2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenProductIdIsNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(null);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verifyNoInteractions(productRepository, orderRepository);
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenProductPriceIsNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(null);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verify(productRepository).findById(1L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createDraftOrder_shouldMergeDuplicateProductIds() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);

        CreateOrderItemRequest firstItem = new CreateOrderItemRequest();
        firstItem.setProductId(1L);
        firstItem.setQuantity(2);

        CreateOrderItemRequest secondItem = new CreateOrderItemRequest();
        secondItem.setProductId(1L);
        secondItem.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(firstItem, secondItem));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertEquals(1, result.getOrderItems().size());
        assertEquals(5, result.getOrderItems().get(0).getQuantity());
        assertEquals(new BigDecimal("399500.00"), result.getTotalAmount());

        // Verify
        verify(productRepository, times(2)).findById(1L);
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenItemsAreEmpty() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of());

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);

        verifyNoInteractions(
                productRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenItemsAreNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(null);

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);

        verifyNoInteractions(
                productRepository,
                orderRepository,
                orderItemRepository
        );
    }

    @Test
    void createDraftOrder_shouldThrowExceptionWhenDuplicateQuantityOverflows() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);

        CreateOrderItemRequest firstItem = new CreateOrderItemRequest();
        firstItem.setProductId(1L);
        firstItem.setQuantity(Integer.MAX_VALUE);

        CreateOrderItemRequest secondItem = new CreateOrderItemRequest();
        secondItem.setProductId(1L);
        secondItem.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(firstItem, secondItem));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                ArithmeticException.class,
                () -> orderService.createDraftOrder(request)
        );

        // Verify
        verify(customerRepository).findById(1L);
        verify(productRepository, times(2)).findById(1L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void createDraftOrder_shouldCreateOrderWithMultipleDifferentProducts() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("iPhone 17");
        product1.setUnitPrice(new BigDecimal("79900.00"));
        product1.setActive(true);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("MacBook Air");
        product2.setUnitPrice(new BigDecimal("99900.00"));
        product2.setActive(true);

        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(item1, item2));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product1));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertEquals(2, result.getOrderItems().size());
        assertEquals(2, result.getOrderItems().get(0).getQuantity());
        assertEquals(1, result.getOrderItems().get(1).getQuantity());
        assertEquals(
                new BigDecimal("259700.00"),
                result.getTotalAmount()
        );

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository).findById(2L);
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createDraftOrder_shouldNotDeductProductStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);
        product.setQuantityAvailable(10);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(3);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertEquals(10, product.getQuantityAvailable());
        assertEquals(3, result.getOrderItems().get(0).getQuantity());
        assertEquals(OrderStatus.DRAFT, result.getStatus());

        // Verify
        verify(productRepository).findById(1L);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createDraftOrder_shouldCaptureCurrentProductPrice() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setActive(true);

        CreateOrderItemRequest itemRequest = new CreateOrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(2);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(itemRequest));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        OrderItem savedItem = result.getOrderItems().get(0);
        assertEquals(
                new BigDecimal("79900.00"),
                savedItem.getUnitPrice());
        assertEquals(
                new BigDecimal("159800.00"),
                result.getTotalAmount());

        // Verify
        verify(orderRepository).save(any(Order.class));
    }

    @Test
    void createDraftOrder_shouldSaveOrderOnlyOnce() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("iPhone 17");
        product1.setUnitPrice(new BigDecimal("79900.00"));
        product1.setActive(true);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("MacBook Air");
        product2.setUnitPrice(new BigDecimal("99900.00"));
        product2.setActive(true);

        CreateOrderItemRequest item1 = new CreateOrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(1);

        CreateOrderItemRequest item2 = new CreateOrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setCustomerId(1L);
        request.setItems(List.of(item1, item2));

        when(customerRepository.findById(1L))
                .thenReturn(Optional.of(customer));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product1));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order result = orderService.createDraftOrder(request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getOrderItems().size());

        // Verify
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void getOrders_shouldReturnAllOrdersSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.of(2026, 10, 30, 10, 30));
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setStatus(OrderStatus.DRAFT);

        Page<Order> orderPage = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 10),
                1
        );

        when(orderRepository.findOrders(
                isNull(),
                isNull(),
                any(Pageable.class)
        )).thenReturn(orderPage);

        // Act
        OrderPageResponse result =
                orderService.getOrders(0, 10, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(101L, result.getContent().get(0).getId());
        assertEquals(1L, result.getContent().get(0).getCustomerId());
        assertEquals(
                new BigDecimal("159800.00"),
                result.getContent().get(0).getTotalAmount());
        assertEquals(OrderStatus.DRAFT,
                result.getContent().get(0).getStatus());

        // Pagination assertions
        assertEquals(0, result.getPage());
        assertEquals(10, result.getSize());
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getTotalPages());
        assertTrue(result.isFirst());
        assertTrue(result.isLast());

        // Verify
        verify(orderRepository).findOrders(
                isNull(),
                isNull(),
                any(Pageable.class)
        );

        verifyNoInteractions(customerRepository);
    }

    @Test
    void getOrders_shouldFilterOrdersByCustomerId() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.of(2026, 10, 30, 10, 30));
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setStatus(OrderStatus.DRAFT);

        Page<Order> orderPage = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 10),
                1
        );

        when(customerRepository.existsById(1L))
                .thenReturn(true);

        when(orderRepository.findOrders(
                eq(1L),
                isNull(),
                any(Pageable.class)
        )).thenReturn(orderPage);

        // Act
        OrderPageResponse result =
                orderService.getOrders(0, 10, 1L, null);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(101L, result.getContent().get(0).getId());
        assertEquals(1L, result.getContent().get(0).getCustomerId());

        // Verify
        verify(customerRepository).existsById(1L);

        verify(orderRepository).findOrders(
                eq(1L),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    void getOrders_shouldFilterOrdersByStatus() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.of(2026, 10, 30, 10, 30));
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setStatus(OrderStatus.CONFIRMED);

        Page<Order> orderPage = new PageImpl<>(
                List.of(order),
                PageRequest.of(0, 10),
                1
        );

        when(orderRepository.findOrders(
                isNull(),
                eq(OrderStatus.CONFIRMED),
                any(Pageable.class)
        )).thenReturn(orderPage);

        // Act
        OrderPageResponse result =
                orderService.getOrders(0, 10, null, OrderStatus.CONFIRMED);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals(101L, result.getContent().get(0).getId());
        assertEquals(OrderStatus.CONFIRMED, result.getContent().get(0).getStatus());

        // Verify
        verify(orderRepository).findOrders(
                isNull(),
                eq(OrderStatus.CONFIRMED),
                any(Pageable.class));
        verifyNoInteractions(customerRepository);
    }

    @Test
    void getOrders_shouldThrowExceptionWhenCustomerNotFound() {

        // Arrange
        when(customerRepository.existsById(999L)).thenReturn(false);

        // Act & Assert
        assertThrows(CustomerNotFoundException.class, () -> orderService.getOrders(0, 10, 999L, null));

        // Verify
        verify(customerRepository).existsById(999L);
        verifyNoInteractions(orderRepository);
    }

    @Test
    void getOrders_shouldThrowExceptionWhenPageIsNegative() {

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrders(-1, 10, null, null));

        // Verify
        verifyNoInteractions(orderRepository, customerRepository);
    }

    @Test
    void getOrders_shouldThrowExceptionWhenPageSizeExceeds100() {

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.getOrders(0, 101, null, null));

        // Verify
        verifyNoInteractions(orderRepository, customerRepository);
    }

    @Test
    void getOrderById_shouldReturnOrderSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.of(2026, 10, 30, 10, 30));
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setStatus(OrderStatus.DRAFT);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act
        OrderResponse result = orderService.getOrderById(101L);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(1L, result.getCustomerId());
        assertEquals(OrderStatus.DRAFT, result.getStatus());

        // Verify
        verify(orderRepository).findById(101L);
    }

    @Test
    void getOrderById_shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderById(999L));

        // Verify
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrderItems_shouldReturnAllItemsSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(10L);
        product1.setName("iPhone 17");

        Product product2 = new Product();
        product2.setId(20L);
        product2.setName("MacBook Air");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("99900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act
        List<OrderItemResponse> result =
                orderService.getOrderItems(101L);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(10L, result.get(0).getProductId());
        assertEquals(2, result.get(0).getQuantity());

        assertEquals(2L, result.get(1).getId());
        assertEquals(20L, result.get(1).getProductId());
        assertEquals(1, result.get(1).getQuantity());

        // Verify
        verify(orderRepository).findById(101L);
    }

    @Test
    void getOrderItems_shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.getOrderItems(999L)
        );

        // Verify
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrderItemById_shouldReturnItemSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act
        OrderItemResponse result =
                orderService.getOrderItemById(101L, 1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getProductId());
        assertEquals(2, result.getQuantity());

        // Verify
        verify(orderRepository).findById(101L);
    }

    @Test
    void getOrderItemById_shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.getOrderItemById(999L, 1L));

        // Verify
        verify(orderRepository).findById(999L);
    }

    @Test
    void getOrderItemById_shouldThrowExceptionWhenOrderItemNotFound() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setOrderItems(new ArrayList<>());

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(OrderItemNotFoundException.class, () -> orderService.getOrderItemById(101L, 999L));

        // Verify
        verify(orderRepository).findById(101L);
    }

    @Test
    void getOrderItemById_shouldReturnItemSuccessfullyWhenSearchedByItemId() {

        // Arrange
        Order order = new Order();
        order.setId(101L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        when(orderItemRepository.findById(1L))
                .thenReturn(Optional.of(item));

        // Act
        OrderItemResponse result = orderService.getOrderItemById(1L);

        // Assert
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(10L, result.getProductId());
        assertEquals(2, result.getQuantity());

        // Verify
        verify(orderItemRepository).findById(1L);
    }

    @Test
    void getOrderItemById_shouldThrowExceptionWhenItemNotFound() {

        // Arrange
        when(orderItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderItemNotFoundException.class, () -> orderService.getOrderItemById(999L));

        // Verify
        verify(orderItemRepository).findById(999L);
    }

    @Test
    void updateOrder_shouldUpdateExistingItemQuantitySuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalAmount(new BigDecimal("159800.00"));

        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(orderItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(5);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(OrderStatus.DRAFT, result.getStatus());

        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().getFirst().getQuantity());

        assertEquals(new BigDecimal("399500.00"), result.getTotalAmount());

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        UpdateOrderRequest request = new UpdateOrderRequest();

        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                OrderNotFoundException.class,
                () -> orderService.updateOrder(999L, request)
        );

        // Verify
        verify(orderRepository).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenItemsAreEmpty() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.DRAFT);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of());

        when(orderRepository.findById(101L)).thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenDuplicateItemIdsAreProvided() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.DRAFT);

        UpdateOrderItemRequest item1 = new UpdateOrderItemRequest();
        item1.setId(1L);
        item1.setProductId(10L);
        item1.setQuantity(2);

        UpdateOrderItemRequest item2 = new UpdateOrderItemRequest();
        item2.setId(1L);
        item2.setProductId(10L);
        item2.setQuantity(3);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(item1, item2));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductIdIsChanged() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product existingProduct = new Product();
        existingProduct.setId(10L);
        existingProduct.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(existingProduct);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(20L);
        itemRequest.setQuantity(3);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenQuantityIsZero() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(0);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenQuantityIsNegative() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(-5);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductIdIsNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(null);
        itemRequest.setProductId(null);
        itemRequest.setQuantity(2);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductNotFound() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setProductId(999L);
        itemRequest.setQuantity(2);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ProductNotFoundException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(productRepository).findById(999L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductIsInactive() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(false);

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () -> orderService.updateOrder(101L, request));

        // Verify
        verify(orderRepository).findById(101L);
        verify(productRepository).findById(10L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductUnitPriceIsNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(null);

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(productRepository).findById(10L);
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenOrderIsNotDraft() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.CONFIRMED);

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(5);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldRemoveOmittedExistingItemsSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(10L);
        product1.setName("iPhone 17");

        Product product2 = new Product();
        product2.setId(20L);
        product2.setName("MacBook Air");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("99900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        // Retain item 1 and omit item 2.
        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(2);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals(10L, result.getItems().get(0).getProductId());

        assertEquals(
                new BigDecimal("159800.00"),
                result.getTotalAmount()
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_shouldAddNewItemSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product existingProduct = new Product();
        existingProduct.setId(10L);
        existingProduct.setName("iPhone 17");

        Product newProduct = new Product();
        newProduct.setId(20L);
        newProduct.setName("MacBook Air");
        newProduct.setActive(true);
        newProduct.setUnitPrice(new BigDecimal("99900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(existingProduct);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        // Existing item request
        UpdateOrderItemRequest existingItemRequest =
                new UpdateOrderItemRequest();

        existingItemRequest.setId(1L);
        existingItemRequest.setProductId(10L);
        existingItemRequest.setQuantity(2);

        // New item request
        UpdateOrderItemRequest newItemRequest =
                new UpdateOrderItemRequest();

        newItemRequest.setId(null);
        newItemRequest.setProductId(20L);
        newItemRequest.setQuantity(1);

        UpdateOrderRequest request = new UpdateOrderRequest();

        request.setItems(List.of(existingItemRequest, newItemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(20L))
                .thenReturn(Optional.of(newProduct));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result =
                orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getItems().size());

        assertEquals(new BigDecimal("259700.00"), result.getTotalAmount());

        // Verify existing item
        assertEquals(1L, result.getItems().get(0).getId());
        assertEquals(10L, result.getItems().get(0).getProductId());

        // Verify new item
        assertNull(result.getItems().get(1).getId());
        assertEquals(20L, result.getItems().get(1).getProductId());
        assertEquals(1, result.getItems().get(1).getQuantity());

        // Verify
        verify(orderRepository).findById(101L);
        verify(productRepository).findById(20L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_shouldMergeNewItemWithExistingProduct() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest existingItemRequest =
                new UpdateOrderItemRequest();
        existingItemRequest.setId(1L);
        existingItemRequest.setProductId(10L);
        existingItemRequest.setQuantity(2);

        UpdateOrderItemRequest newItemRequest =
                new UpdateOrderItemRequest();
        newItemRequest.setId(null);
        newItemRequest.setProductId(10L);
        newItemRequest.setQuantity(3);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(existingItemRequest, newItemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());
        assertEquals(5, result.getItems().get(0).getQuantity());
        assertEquals(10L, result.getItems().get(0).getProductId());

        assertEquals(
                new BigDecimal("399500.00"),
                result.getTotalAmount()
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(productRepository).findById(10L);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_shouldUpdateStatusWithoutModifyingItems() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setOrderItems(new ArrayList<>());

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);
        request.setItems(null);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(OrderStatus.CANCELLED, result.getStatus());
        assertTrue(result.getItems().isEmpty());

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, times(2)).save(order);
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldSaveOrderWithoutChangesWhenRequestIsEmpty() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setOrderItems(new ArrayList<>());

        UpdateOrderRequest request = new UpdateOrderRequest();

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(101L, result.getId());
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        assertEquals(
                new BigDecimal("159800.00"),
                result.getTotalAmount()
        );
        assertTrue(result.getItems().isEmpty());

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, times(1)).save(order);

        verifyNoInteractions(
                productRepository,
                orderItemRepository
        );
    }

    @Test
    void updateOrder_shouldUpdateQuantityAndConfirmOrderSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setQuantityAvailable(10);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(5);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(5, result.getItems().get(0).getQuantity());
        assertEquals(new BigDecimal("399500.00"), result.getTotalAmount());

        // Stock should be reduced from 10 to 5.
        assertEquals(5, product.getQuantityAvailable());

        // Verify
        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository).save(product);
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    void updateOrder_shouldNotConfirmWhenUpdatedQuantityExceedsStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setQuantityAvailable(3);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(2);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        UpdateOrderItemRequest itemRequest = new UpdateOrderItemRequest();
        itemRequest.setId(1L);
        itemRequest.setProductId(10L);
        itemRequest.setQuantity(5);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(itemRequest));
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () -> orderService.updateOrder(101L, request));

        // Verify order remains DRAFT
        assertEquals(OrderStatus.DRAFT, order.getStatus());

        // Verify stock remains unchanged
        assertEquals(3, product.getQuantityAvailable());

        // Verify
        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldNotConfirmWhenProductIsInactive() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(false);
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setQuantityAvailable(10);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify order remains DRAFT
        assertEquals(OrderStatus.DRAFT, order.getStatus());

        // Verify stock remains unchanged
        assertEquals(10, product.getQuantityAvailable());

        // Verify
        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldFulfillConfirmedOrderSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setTotalAmount(new BigDecimal("159800.00"));
        order.setOrderItems(new ArrayList<>());

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.FULFILLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.FULFILLED, result.getStatus());

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository, times(2)).save(order);

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldCancelConfirmedOrderAndRestoreStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setQuantityAvailable(7);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(3);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());

        // Verify stock restoration: 7 + 3 = 10
        assertEquals(10, product.getQuantityAvailable());

        // Verify
        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository).save(product);
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenCancellingFulfilledOrder() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.FULFILLED);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        assertEquals(OrderStatus.FULFILLED, order.getStatus());

        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenConfirmingCancelledOrder() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.CANCELLED);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        assertEquals(OrderStatus.CANCELLED, order.getStatus());

        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenCancellingAlreadyCancelledOrder() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CANCELLED);
        order.setOrderItems(new ArrayList<>());

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CANCELLED, result.getStatus());

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository).save(order);

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldConfirmOrderWithoutModifyingItems() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));
        product.setQuantityAvailable(10);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);
        request.setItems(null);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.CONFIRMED, result.getStatus());
        assertEquals(1, result.getItems().size());
        assertEquals(2, result.getItems().get(0).getQuantity());

        // Stock should be deducted: 10 - 2 = 8
        assertEquals(8, product.getQuantityAvailable());

        // Verify
        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository).save(product);
        verify(orderRepository, times(2)).save(order);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenConfirmingOrderWithoutItems() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        assertEquals(OrderStatus.DRAFT, order.getStatus());

        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));

        verifyNoInteractions(productRepository);
    }

    @Test
    void deleteOrderItem_shouldDeleteItemSuccessfully() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(10L);
        product1.setName("iPhone 17");

        Product product2 = new Product();
        product2.setId(20L);
        product2.setName("MacBook Air");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("99900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act
        orderService.deleteOrderItem(101L, 1L);

        // Assert
        assertEquals(1, order.getOrderItems().size());
        assertEquals(2L, order.getOrderItems().get(0).getId());

        assertEquals(
                new BigDecimal("99900.00"),
                order.getTotalAmount()
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderItemRepository).delete(item1);
    }

    @Test
    void deleteOrderItem_shouldThrowExceptionWhenOrderNotFound() {

        // Arrange
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(OrderNotFoundException.class, () -> orderService.deleteOrderItem(999L, 1L));

        // Verify
        verify(orderRepository).findById(999L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItem_shouldThrowExceptionWhenOrderItemNotFound() {

        // Arrange
        Order order = new Order();
        order.setId(101L);
        order.setStatus(OrderStatus.DRAFT);
        order.setOrderItems(new ArrayList<>());

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(OrderItemNotFoundException.class, () -> orderService.deleteOrderItem(101L, 999L));

        // Verify
        verify(orderRepository).findById(101L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItem_shouldThrowExceptionWhenDeletingLastItem() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () -> orderService.deleteOrderItem(101L, 1L));

        // Verify
        assertEquals(1, order.getOrderItems().size());

        verify(orderRepository).findById(101L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItem_shouldThrowExceptionWhenOrderIsNotDraft() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(BusinessConflictException.class, () -> orderService.deleteOrderItem(101L, 1L));

        // Verify
        assertEquals(1, order.getOrderItems().size());

        verify(orderRepository).findById(101L);
        verifyNoInteractions(orderItemRepository);
    }

    @Test
    void deleteOrderItem_shouldNotChangeProductStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(10L);
        product1.setName("iPhone 17");
        product1.setQuantityAvailable(10);

        Product product2 = new Product();
        product2.setId(20L);
        product2.setName("MacBook Air");
        product2.setQuantityAvailable(5);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("99900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act
        orderService.deleteOrderItem(101L, 1L);

        // Assert
        assertEquals(10, product1.getQuantityAvailable());
        assertEquals(5, product2.getQuantityAvailable());

        assertEquals(1, order.getOrderItems().size());

        assertEquals(
                new BigDecimal("99900.00"),
                order.getTotalAmount()
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderItemRepository).delete(item1);

        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldNotDeductAnyStockWhenOneProductHasInsufficientStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product1 = new Product();
        product1.setId(10L);
        product1.setName("iPhone 17");
        product1.setActive(true);
        product1.setQuantityAvailable(10);
        product1.setUnitPrice(new BigDecimal("79900.00"));

        Product product2 = new Product();
        product2.setId(20L);
        product2.setName("MacBook Air");
        product2.setActive(true);
        product2.setQuantityAvailable(1);
        product2.setUnitPrice(new BigDecimal("99900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product1);
        item1.setQuantity(2);
        item1.setUnitPrice(product1.getUnitPrice());

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product2);
        item2.setQuantity(2);
        item2.setUnitPrice(product2.getUnitPrice());

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(product1));

        when(productRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(product2));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify stock remains unchanged
        assertEquals(10, product1.getQuantityAvailable());
        assertEquals(1, product2.getQuantityAvailable());

        assertEquals(OrderStatus.DRAFT, order.getStatus());

        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductNotFoundDuringConfirmation() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setQuantityAvailable(10);
        product.setUnitPrice(new BigDecimal("79900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getUnitPrice());

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        assertEquals(10, product.getQuantityAvailable());
        assertEquals(OrderStatus.DRAFT, order.getStatus());

        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenProductNotFoundDuringCancellation() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setQuantityAvailable(8);

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(
                ProductNotFoundException.class,
                () -> orderService.updateOrder(101L, request)
        );

        // Verify
        assertEquals(8, product.getQuantityAvailable());
        assertEquals(OrderStatus.CONFIRMED, order.getStatus());

        verify(productRepository).findByIdForUpdate(10L);
        verify(productRepository, never()).save(any(Product.class));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void deleteOrderItem_shouldThrowExceptionWhenOrderIsFulfilled() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.FULFILLED);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                BusinessConflictException.class,
                () -> orderService.deleteOrderItem(101L, 1L)
        );

        // Verify
        assertEquals(2, order.getOrderItems().size());

        verify(orderItemRepository, never()).delete(any(OrderItem.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldCancelDraftOrderWithoutChangingStock() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setQuantityAvailable(10);
        product.setUnitPrice(new BigDecimal("79900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item = new OrderItem();
        item.setId(1L);
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(2);
        item.setUnitPrice(product.getUnitPrice());

        order.setOrderItems(new ArrayList<>(List.of(item)));

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setStatus(OrderStatus.CANCELLED);

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenReturn(order);

        // Act
        orderService.updateOrder(101L, request);

        // Assert
        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertEquals(10, product.getQuantityAvailable());

        // Verify
        verify(orderRepository, times(2)).save(order);
        verifyNoInteractions(productRepository);
    }


    @Test
    void updateOrder_shouldMergeDuplicateExistingItemsForSameProduct() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product);
        item2.setQuantity(3);
        item2.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        UpdateOrderItemRequest request1 = new UpdateOrderItemRequest();
        request1.setId(1L);
        request1.setProductId(10L);
        request1.setQuantity(2);

        UpdateOrderItemRequest request2 = new UpdateOrderItemRequest();
        request2.setId(2L);
        request2.setProductId(10L);
        request2.setQuantity(3);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(request1, request2));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResponse result = orderService.updateOrder(101L, request);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getItems().size());

        // First existing item should be retained
        assertEquals(1L, result.getItems().get(0).getId());

        // Quantities should be combined
        assertEquals(5, result.getItems().get(0).getQuantity());

        assertEquals(
                new BigDecimal("399500.00"),
                result.getTotalAmount()
        );

        // Verify
        verify(orderRepository).findById(101L);
        verify(orderRepository).save(order);
        verifyNoInteractions(productRepository);
    }

    @Test
    void getOrders_shouldFilterOrdersByCustomerIdAndStatus() {
        Long customerId = 1L;
        OrderStatus status = OrderStatus.DRAFT;

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> orderPage = new PageImpl<>(List.of());

        when(customerRepository.existsById(customerId))
                .thenReturn(true);

        when(orderRepository.findOrders(customerId, status, pageable))
                .thenReturn(orderPage);

        // Act
        OrderPageResponse result =
                orderService.getOrders(0, 10, customerId, status);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());

        // Verify
        verify(customerRepository).existsById(customerId);
        verify(orderRepository).findOrders(customerId, status, pageable);
    }

    @Test
    void getOrders_shouldReturnEmptyPageWhenNoOrdersFound() {
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> emptyPage = new PageImpl<>(List.of());

        when(orderRepository.findOrders(null, null, pageable))
                .thenReturn(emptyPage);

        // Act
        OrderPageResponse result =
                orderService.getOrders(0, 10, null, null);

        // Assert
        assertNotNull(result);
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());

        // Verify
        verify(orderRepository).findOrders(null, null, pageable);
        verifyNoInteractions(customerRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenExistingItemQuantitiesOverflow() {
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setOrder(order);
        item1.setProduct(product);
        item1.setQuantity(Integer.MAX_VALUE);
        item1.setUnitPrice(new BigDecimal("79900.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setOrder(order);
        item2.setProduct(product);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(item1, item2)));

        UpdateOrderItemRequest request1 = new UpdateOrderItemRequest();
        request1.setId(1L);
        request1.setProductId(10L);
        request1.setQuantity(Integer.MAX_VALUE);

        UpdateOrderItemRequest request2 = new UpdateOrderItemRequest();
        request2.setId(2L);
        request2.setProductId(10L);
        request2.setQuantity(1);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(request1, request2));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        // Act & Assert
        assertThrows(
                ArithmeticException.class,
                () -> orderService.updateOrder(101L, request)
        );

        verify(orderRepository).findById(101L);
        verify(orderRepository, never()).save(any(Order.class));
        verifyNoInteractions(productRepository);
    }

    @Test
    void updateOrder_shouldThrowExceptionWhenNewItemQuantityOverflows() {
        Customer customer = new Customer();
        customer.setId(1L);

        Product product = new Product();
        product.setId(10L);
        product.setName("iPhone 17");
        product.setActive(true);
        product.setUnitPrice(new BigDecimal("79900.00"));

        Order order = new Order();
        order.setId(101L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        OrderItem existingItem = new OrderItem();
        existingItem.setId(1L);
        existingItem.setOrder(order);
        existingItem.setProduct(product);
        existingItem.setQuantity(Integer.MAX_VALUE);
        existingItem.setUnitPrice(new BigDecimal("79900.00"));

        order.setOrderItems(new ArrayList<>(List.of(existingItem)));

        // Existing item request
        UpdateOrderItemRequest existingItemRequest =
                new UpdateOrderItemRequest();

        existingItemRequest.setId(1L);
        existingItemRequest.setProductId(10L);
        existingItemRequest.setQuantity(Integer.MAX_VALUE);

        // New item request for the same product
        UpdateOrderItemRequest newItemRequest =
                new UpdateOrderItemRequest();

        newItemRequest.setProductId(10L);
        newItemRequest.setQuantity(1);

        UpdateOrderRequest request = new UpdateOrderRequest();
        request.setItems(List.of(existingItemRequest, newItemRequest));

        when(orderRepository.findById(101L))
                .thenReturn(Optional.of(order));

        when(productRepository.findById(10L))
                .thenReturn(Optional.of(product));

        // Act & Assert
        assertThrows(
                ArithmeticException.class,
                () -> orderService.updateOrder(101L, request)
        );

        verify(orderRepository).findById(101L);
        verify(productRepository).findById(10L);
        verify(orderRepository, never()).save(any(Order.class));
    }
}