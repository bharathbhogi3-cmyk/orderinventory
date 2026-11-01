package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.Customer;
import com.apple.supply.orderinventory.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderRepositoryTest {

    @Mock
    private OrderRepository orderRepository;

    @Test
    void save_shouldReturnSavedOrder() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("Bharath");
        customer.setEmail("bharath@gmail.com");

        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderDateTime(LocalDateTime.now());
        order.setTotalAmount(new BigDecimal("50000.00"));
        order.setStatus(OrderStatus.DRAFT);

        when(orderRepository.save(order))
                .thenReturn(order);

        // Act
        Order result = orderRepository.save(order);

        // Assert
        assertNotNull(result);
        assertEquals(OrderStatus.DRAFT, result.getStatus());
        assertEquals(new BigDecimal("50000.00"),
                result.getTotalAmount());

        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void findById_shouldReturnOrderWhenOrderExists() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        // Act
        Optional<Order> result =
                orderRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(OrderStatus.CONFIRMED,
                result.get().getStatus());

        verify(orderRepository, times(1))
                .findById(1L);
    }

    @Test
    void findById_shouldReturnEmptyWhenOrderDoesNotExist() {

        // Arrange
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<Order> result =
                orderRepository.findById(999L);

        // Assert
        assertTrue(result.isEmpty());

        verify(orderRepository, times(1))
                .findById(999L);
    }

    @Test
    void findAll_shouldReturnAllOrders() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setCustomer(customer);
        order1.setStatus(OrderStatus.DRAFT);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setCustomer(customer);
        order2.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findAll())
                .thenReturn(List.of(order1, order2));

        // Act
        List<Order> result = orderRepository.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals(OrderStatus.DRAFT,
                result.get(0).getStatus());
        assertEquals(OrderStatus.CONFIRMED,
                result.get(1).getStatus());

        verify(orderRepository, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoOrdersExist() {

        // Arrange
        when(orderRepository.findAll())
                .thenReturn(List.of());

        // Act
        List<Order> result = orderRepository.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(orderRepository, times(1)).findAll();
    }


    @Test
    void deleteById_shouldDeleteOrder() {

        // Arrange
        Long orderId = 1L;

        // Act
        orderRepository.deleteById(orderId);

        // Assert
        verify(orderRepository, times(1))
                .deleteById(orderId);
    }

    @Test
    void existsById_shouldReturnTrueWhenOrderExists() {

        // Arrange
        when(orderRepository.existsById(1L))
                .thenReturn(true);

        // Act
        boolean result = orderRepository.existsById(1L);

        // Assert
        assertTrue(result);

        verify(orderRepository, times(1))
                .existsById(1L);
    }

    @Test
    void existsById_shouldReturnFalseWhenOrderDoesNotExist() {

        // Arrange
        when(orderRepository.existsById(999L))
                .thenReturn(false);

        // Act
        boolean result = orderRepository.existsById(999L);

        // Assert
        assertFalse(result);

        verify(orderRepository, times(1))
                .existsById(999L);
    }

    @Test
    void count_shouldReturnTotalNumberOfOrders() {

        // Arrange
        when(orderRepository.count())
                .thenReturn(5L);

        // Act
        long result = orderRepository.count();

        // Assert
        assertEquals(5L, result);

        verify(orderRepository, times(1))
                .count();
    }

    @Test
    void findOrders_shouldReturnOrdersByCustomerId() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> expectedPage =
                new PageImpl<>(List.of(order));

        when(orderRepository.findOrders(
                1L, null, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        1L, null, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(1L,
                result.getContent().get(0).getCustomer().getId());

        verify(orderRepository, times(1))
                .findOrders(1L, null, pageable);
    }


    @Test
    void findOrders_shouldReturnOrdersByStatus() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.CONFIRMED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> expectedPage =
                new PageImpl<>(List.of(order));

        when(orderRepository.findOrders(
                null, OrderStatus.CONFIRMED, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        null, OrderStatus.CONFIRMED, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(OrderStatus.CONFIRMED,
                result.getContent().get(0).getStatus());

        verify(orderRepository, times(1))
                .findOrders(null, OrderStatus.CONFIRMED, pageable);
    }

    @Test
    void findOrders_shouldReturnOrdersByCustomerIdAndStatus() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.FULFILLED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> expectedPage =
                new PageImpl<>(List.of(order));

        when(orderRepository.findOrders(
                1L, OrderStatus.FULFILLED, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        1L, OrderStatus.FULFILLED, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals(1L,
                result.getContent().get(0).getCustomer().getId());
        assertEquals(OrderStatus.FULFILLED,
                result.getContent().get(0).getStatus());

        verify(orderRepository, times(1))
                .findOrders(1L, OrderStatus.FULFILLED, pageable);
    }

    @Test
    void findOrders_shouldReturnAllOrdersWhenFiltersAreNull() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setCustomer(customer);
        order1.setStatus(OrderStatus.DRAFT);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setCustomer(customer);
        order2.setStatus(OrderStatus.CONFIRMED);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> expectedPage =
                new PageImpl<>(List.of(order1, order2));

        when(orderRepository.findOrders(
                null, null, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        null, null, pageable);

        // Assert
        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        verify(orderRepository, times(1))
                .findOrders(null, null, pageable);
    }

    @Test
    void findOrders_shouldReturnEmptyPageWhenNoOrdersMatch() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Page<Order> expectedPage = Page.empty(pageable);

        when(orderRepository.findOrders(
                999L, OrderStatus.CANCELLED, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        999L, OrderStatus.CANCELLED, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(orderRepository, times(1))
                .findOrders(999L, OrderStatus.CANCELLED, pageable);
    }

    @Test
    void findOrders_shouldSupportPagination() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order1 = new Order();
        order1.setId(1L);
        order1.setCustomer(customer);
        order1.setStatus(OrderStatus.CONFIRMED);

        Order order2 = new Order();
        order2.setId(2L);
        order2.setCustomer(customer);
        order2.setStatus(OrderStatus.CONFIRMED);

        Pageable pageable = PageRequest.of(0, 2);

        Page<Order> expectedPage =
                new PageImpl<>(List.of(order1, order2), pageable, 5);

        when(orderRepository.findOrders(
                null, OrderStatus.CONFIRMED, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Order> result =
                orderRepository.findOrders(
                        null, OrderStatus.CONFIRMED, pageable);

        // Assert
        assertEquals(2, result.getContent().size());
        assertEquals(5, result.getTotalElements());
        assertEquals(3, result.getTotalPages());
        assertEquals(0, result.getNumber());
        assertEquals(2, result.getSize());

        verify(orderRepository, times(1))
                .findOrders(null, OrderStatus.CONFIRMED, pageable);
    }

}
