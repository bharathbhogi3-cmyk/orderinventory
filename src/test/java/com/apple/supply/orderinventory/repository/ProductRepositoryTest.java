package com.apple.supply.orderinventory.repository;

import com.apple.supply.orderinventory.model.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRepositoryTest {

    @Mock
    private ProductRepository productRepository;

    @Test
    void findById_shouldReturnProductWhenProductExists() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setCategory("Smartphone");
        product.setQuantityAvailable(10);

        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(product));

        // Act
        java.util.Optional<Product> result =
                productRepository.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("iPhone 17", result.get().getName());

        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void findById_shouldReturnEmptyWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.findById(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<Product> result =
                productRepository.findById(999L);

        // Assert
        assertTrue(result.isEmpty());

        verify(productRepository, times(1))
                .findById(999L);
    }

    @Test
    void save_shouldReturnSavedProduct() {

        // Arrange
        Product product = new Product();
        product.setName("MacBook Air");
        product.setModel("MBA-SIL");
        product.setCategory("Laptop");
        product.setQuantityAvailable(5);

        when(productRepository.save(product))
                .thenReturn(product);

        // Act
        Product result = productRepository.save(product);

        // Assert
        assertNotNull(result);
        assertEquals("MacBook Air", result.getName());
        assertEquals("MBA-SIL", result.getModel());

        verify(productRepository, times(1)).save(product);
    }

    @Test
    void deleteById_shouldDeleteProduct() {

        // Arrange
        Long productId = 1L;

        // Act
        productRepository.deleteById(productId);

        // Assert
        verify(productRepository, times(1))
                .deleteById(productId);
    }

    @Test
    void existsById_shouldReturnTrueWhenProductExists() {

        // Arrange
        when(productRepository.existsById(1L))
                .thenReturn(true);

        // Act
        boolean result = productRepository.existsById(1L);

        // Assert
        assertTrue(result);

        verify(productRepository, times(1))
                .existsById(1L);
    }

    @Test
    void existsById_shouldReturnFalseWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.existsById(999L))
                .thenReturn(false);

        // Act
        boolean result = productRepository.existsById(999L);

        // Assert
        assertFalse(result);

        verify(productRepository, times(1))
                .existsById(999L);
    }

    @Test
    void count_shouldReturnTotalNumberOfProducts() {

        // Arrange
        when(productRepository.count()).thenReturn(5L);

        // Act
        long result = productRepository.count();

        // Assert
        assertEquals(5L, result);

        verify(productRepository, times(1)).count();
    }

    @Test
    void findAll_shouldReturnAllProducts() {

        // Arrange
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("iPhone 17");
        product1.setModel("IP17-BLA");

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("MacBook Air");
        product2.setModel("MBA-SIL");

        when(productRepository.findAll())
                .thenReturn(java.util.List.of(product1, product2));

        // Act
        java.util.List<Product> result = productRepository.findAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals("iPhone 17", result.get(0).getName());
        assertEquals("MacBook Air", result.get(1).getName());

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoProductsExist() {

        // Arrange
        when(productRepository.findAll())
                .thenReturn(java.util.Collections.emptyList());

        // Act
        java.util.List<Product> result = productRepository.findAll();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(productRepository, times(1)).findAll();
    }

    @Test
    void findByCategory_shouldReturnProductsForCategory() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("MacBook Air");
        product.setModel("MBA-SIL");
        product.setCategory("Laptop");

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> expectedPage =
                new PageImpl<>(java.util.List.of(product));

        when(productRepository.findByCategory("Laptop", pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result =
                productRepository.findByCategory("Laptop", pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Laptop", result.getContent().get(0).getCategory());

        verify(productRepository, times(1))
                .findByCategory("Laptop", pageable);
    }

    @Test
    void findByActive_shouldReturnActiveProducts() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> expectedPage =
                new PageImpl<>(java.util.List.of(product));

        when(productRepository.findByActive(true, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result =
                productRepository.findByActive(true, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertTrue(result.getContent().get(0).isActive());

        verify(productRepository, times(1))
                .findByActive(true, pageable);
    }

    @Test
    void findByActive_shouldReturnInactiveProducts() {

        // Arrange
        Product product = new Product();
        product.setId(2L);
        product.setName("MacBook Air");
        product.setModel("MBA-SIL");
        product.setActive(false);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> expectedPage =
                new PageImpl<>(java.util.List.of(product));

        when(productRepository.findByActive(false, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result =
                productRepository.findByActive(false, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isActive());

        verify(productRepository, times(1))
                .findByActive(false, pageable);
    }

    @Test
    void findByCategoryAndActive_shouldReturnMatchingProducts() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setCategory("Smartphone");
        product.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> expectedPage =
                new PageImpl<>(java.util.List.of(product));

        when(productRepository.findByCategoryAndActive(
                "Smartphone", true, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result =
                productRepository.findByCategoryAndActive(
                        "Smartphone", true, pageable);

        // Assert
        assertEquals(1, result.getTotalElements());
        assertEquals("Smartphone",
                result.getContent().get(0).getCategory());
        assertTrue(result.getContent().get(0).isActive());

        verify(productRepository, times(1))
                .findByCategoryAndActive(
                        "Smartphone", true, pageable);
    }

    @Test
    void findByCategoryAndActive_shouldReturnEmptyPageWhenNoMatch() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> expectedPage = Page.empty(pageable);

        when(productRepository.findByCategoryAndActive(
                "Laptop", true, pageable))
                .thenReturn(expectedPage);

        // Act
        Page<Product> result =
                productRepository.findByCategoryAndActive(
                        "Laptop", true, pageable);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        assertEquals(0, result.getTotalElements());

        verify(productRepository, times(1))
                .findByCategoryAndActive(
                        "Laptop", true, pageable);
    }

    @Test
    void findByIdForUpdate_shouldReturnProductWhenFound() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setModel("IP17-BLA");
        product.setQuantityAvailable(10);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(product));

        // Act
        Optional<Product> result =
                productRepository.findByIdForUpdate(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals("iPhone 17", result.get().getName());
        assertEquals(10, result.get().getQuantityAvailable());

        verify(productRepository, times(1))
                .findByIdForUpdate(1L);
    }

    @Test
    void findByIdForUpdate_shouldReturnEmptyWhenProductDoesNotExist() {

        // Arrange
        when(productRepository.findByIdForUpdate(999L))
                .thenReturn(Optional.empty());

        // Act
        Optional<Product> result =
                productRepository.findByIdForUpdate(999L);

        // Assert
        assertTrue(result.isEmpty());

        verify(productRepository, times(1))
                .findByIdForUpdate(999L);
    }
}