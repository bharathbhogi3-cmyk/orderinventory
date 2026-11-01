package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.OrderItem;
import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.Product;
import com.apple.supply.orderinventory.model.Customer;
import com.apple.supply.orderinventory.model.OrderStatus;
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
class OrderItemRepositoryTest {

    @Mock
    private OrderItemRepository orderItemRepository;

    @Test
    void save_shouldReturnSavedOrderItem() {

        // Arrange
        Customer customer = new Customer();
        customer.setId(1L);

        Order order = new Order();
        order.setId(1L);
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");

        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setProduct(product);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("50000.00"));

        when(orderItemRepository.save(orderItem))
                .thenReturn(orderItem);

        // Act
        OrderItem result = orderItemRepository.save(orderItem);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getQuantity());
        assertEquals(new BigDecimal("50000.00"),
                result.getUnitPrice());

        verify(orderItemRepository, times(1))
                .save(orderItem);
    }

    @Test
    void findById_shouldReturnOrderItemWhenExists() {

        // Arrange
        OrderItem orderItem = new OrderItem();
        orderItem.setId(1L);
        orderItem.setQuantity(2);
        orderItem.setUnitPrice(new BigDecimal("50000.00"));

        when(orderItemRepository.findById(1L))
                .thenReturn(Optional.of(orderItem));

        // Act
        Optional<OrderItem> result =
                orderItemRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals(2, result.get().getQuantity());

        verify(orderItemRepository, times(1))
                .findById(1L);
    }

    @Test
    void findById_shouldReturnEmptyWhenNotExists() {

        // Arrange
        when(orderItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<OrderItem> result =
                orderItemRepository.findById(999L);

        // Assert
        assertTrue(result.isEmpty());

        verify(orderItemRepository, times(1))
                .findById(999L);
    }

    @Test
    void findAll_shouldReturnAllOrderItems() {

        // Arrange
        OrderItem item1 = new OrderItem();
        item1.setId(1L);
        item1.setQuantity(2);
        item1.setUnitPrice(new BigDecimal("50000.00"));

        OrderItem item2 = new OrderItem();
        item2.setId(2L);
        item2.setQuantity(1);
        item2.setUnitPrice(new BigDecimal("80000.00"));

        when(orderItemRepository.findAll())
                .thenReturn(List.of(item1, item2));

        // Act
        List<OrderItem> result =
                orderItemRepository.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals(2, result.get(0).getQuantity());
        assertEquals(1, result.get(1).getQuantity());

        verify(orderItemRepository, times(1))
                .findAll();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoItemsExist() {

        // Arrange
        when(orderItemRepository.findAll())
                .thenReturn(List.of());

        // Act
        List<OrderItem> result =
                orderItemRepository.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(orderItemRepository, times(1))
                .findAll();
    }


    @Test
    void deleteById_shouldDeleteOrderItem() {

        // Arrange
        Long orderItemId = 1L;

        // Act
        orderItemRepository.deleteById(orderItemId);

        // Assert
        verify(orderItemRepository, times(1))
                .deleteById(orderItemId);
    }

    @Test
    void existsById_shouldReturnTrueWhenExists() {

        // Arrange
        when(orderItemRepository.existsById(1L))
                .thenReturn(true);

        // Act
        boolean result =
                orderItemRepository.existsById(1L);

        // Assert
        assertTrue(result);

        verify(orderItemRepository, times(1))
                .existsById(1L);
    }

    @Test
    void existsById_shouldReturnFalseWhenNotExists() {

        // Arrange
        when(orderItemRepository.existsById(999L))
                .thenReturn(false);

        // Act
        boolean result =
                orderItemRepository.existsById(999L);

        // Assert
        assertFalse(result);

        verify(orderItemRepository, times(1))
                .existsById(999L);
    }

    @Test
    void count_shouldReturnTotalNumberOfOrderItems() {

        // Arrange
        when(orderItemRepository.count())
                .thenReturn(5L);

        // Act
        long result = orderItemRepository.count();

        // Assert
        assertEquals(5L, result);

        verify(orderItemRepository, times(1))
                .count();
    }

}
