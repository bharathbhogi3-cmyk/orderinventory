
package com.apple.supply.orderinventory.service;

import com.apple.supply.orderinventory.dto.CreateProductRequest;
import com.apple.supply.orderinventory.dto.ProductResponse;
import com.apple.supply.orderinventory.dto.UpdateProductQuantityRequest;
import com.apple.supply.orderinventory.dto.UpdateProductRequest;
import com.apple.supply.orderinventory.exception.BusinessConflictException;
import com.apple.supply.orderinventory.exception.ProductNotFoundException;
import com.apple.supply.orderinventory.model.OrderItem;
import com.apple.supply.orderinventory.model.Product;
import com.apple.supply.orderinventory.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void createProduct_shouldSaveProductAndReturnResponse() {

        // Arrange: Prepare a valid product request
        CreateProductRequest request = new CreateProductRequest();
        request.setName("iPhone 17");
        request.setDescription("Apple smartphone");
        request.setBrand("Apple");
        request.setColor("Black");
        request.setCategory("Mobile");
        request.setUnitPrice(new BigDecimal("79999.00"));
        request.setQuantityAvailable(10);

        // Simulate saving the product
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(1L);
                    return product;
                });

        // Act: Call the actual service method
        ProductResponse response = productService.createProduct(request);

        // Assert: Check the returned response
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("iPhone 17", response.getName());
        assertEquals("Apple", response.getBrand());
        assertEquals("Black", response.getColor());
        assertEquals("IP17-BLA", response.getModel());
        assertEquals(new BigDecimal("79999.00"),
                response.getUnitPrice());
        assertEquals(10, response.getQuantityAvailable());
        assertTrue(response.isActive());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());

        // Verify that the repository was called once
        verify(productRepository, times(1))
                .save(any(Product.class));
    }


    @Test
    void createProduct_shouldThrowExceptionWhenNameIsBlank() {

        // Arrange: Prepare a request with a blank product name
        CreateProductRequest request = new CreateProductRequest();
        request.setName("   ");
        request.setColor("Black");

        // Act and Assert: Expect an IllegalArgumentException
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(request)
        );

        // Verify the exception message
        assertEquals(
                "Product name cannot be empty.",
                exception.getMessage()
        );

        // Verify that the product was never saved
        verify(productRepository, never()).save(any(Product.class));
    }


    @Test
    void createProduct_shouldThrowExceptionWhenColorIsBlank() {

        // Arrange
        CreateProductRequest request = new CreateProductRequest();
        request.setName("iPhone 17");
        request.setColor("   ");

        // Act and Assert
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> productService.createProduct(request)
        );

        // Check the exception message
        assertEquals(
                "Product color cannot be empty.",
                exception.getMessage()
        );

        // Ensure the repository was never called
        verify(productRepository, never()).save(any(Product.class));
    }


    @Test
    void createProduct_shouldGenerateCorrectModelCode() {

        // Arrange
        CreateProductRequest request = new CreateProductRequest();
        request.setName("MacBook Air");
        request.setColor("Silver");
        request.setUnitPrice(new BigDecimal("999.00"));
        request.setQuantityAvailable(5);

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product product = invocation.getArgument(0);
                    product.setId(2L);
                    return product;
                });

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertEquals("MBA-SIL", response.getModel());
    }


    @Test
    void getProductById_shouldReturnProductWhenFound() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setModel("IP17-BLA");
        product.setCategory("Mobile");
        product.setUnitPrice(new BigDecimal("79999.00"));
        product.setQuantityAvailable(10);
        product.setActive(true);

        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(product));

        // Act
        ProductResponse response = productService.getProductById(1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("iPhone 17", response.getName());
        assertEquals("Apple", response.getBrand());
        assertEquals("IP17-BLA", response.getModel());

        verify(productRepository, times(1)).findById(1L);
    }


    @Test
    void getProductById_shouldThrowExceptionWhenProductNotFound() {

        // Arrange
        when(productRepository.findById(99L))
                .thenReturn(java.util.Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.getProductById(99L)
        );

        // Check the exception message
        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        // Verify the repository was queried once
        verify(productRepository, times(1)).findById(99L);
    }


    @Test
    void updateProduct_shouldUpdateProductAndReturnResponse() {

        // Arrange: Create an existing product
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("iPhone 16");
        existingProduct.setDescription("Old smartphone");
        existingProduct.setBrand("Apple");
        existingProduct.setColor("Black");
        existingProduct.setModel("IP16-BLA");
        existingProduct.setCategory("Mobile");
        existingProduct.setUnitPrice(new BigDecimal("69999.00"));
        existingProduct.setQuantityAvailable(10);
        existingProduct.setActive(true);

        // Prepare the updated values
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("iPhone 17");
        request.setDescription("New smartphone");
        request.setBrand("Apple");
        request.setColor("Blue");
        request.setCategory("Mobile");
        request.setUnitPrice(new BigDecimal("79999.00"));
        request.setActive(false);

        // Mock finding and saving the product
        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act: Call the service method
        ProductResponse response =
                productService.updateProduct(1L, request);

        // Assert: Verify the updated details
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("iPhone 17", response.getName());
        assertEquals("New smartphone", response.getDescription());
        assertEquals("Apple", response.getBrand());
        assertEquals("Blue", response.getColor());
        assertEquals("IP17-BLU", response.getModel());
        assertEquals(new BigDecimal("79999.00"), response.getUnitPrice());
        assertFalse(response.isActive());
        assertNotNull(response.getUpdatedAt());

        // Verify repository interactions
        verify(productRepository, times(1)).findById(1L);
        verify(productRepository, times(1)).save(existingProduct);
    }


    @Test
    void updateProduct_shouldThrowExceptionWhenProductNotFound() {

        // Arrange
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("iPhone 17");
        request.setColor("Blue");
        request.setUnitPrice(new BigDecimal("79999.00"));

        when(productRepository.findById(99L))
                .thenReturn(java.util.Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProduct(99L, request)
        );

        // Check the error message
        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        // Ensure no product is saved
        verify(productRepository, never()).save(any(Product.class));
    }

//    if (request.getActive() != null) {
//        product.setActive(request.getActive());
//    }
    /*If active is true, set the product to active.
      If active is false, set the product to inactive.
      If active is null, preserve the existing status.    These cases followed by service logic */

    @Test //Testing for True remains True or not
    void updateProduct_shouldPreserveActiveStatusWhenNotProvided() {

        // Arrange: Existing product is active
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("iPhone 16");
        existingProduct.setDescription("Old smartphone");
        existingProduct.setBrand("Apple");
        existingProduct.setColor("Black");
        existingProduct.setModel("IP16-BLA");
        existingProduct.setCategory("Mobile");
        existingProduct.setUnitPrice(new BigDecimal("69999.00"));
        existingProduct.setQuantityAvailable(10);
        existingProduct.setActive(true);

        // Prepare an update request without an active value
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("iPhone 17");
        request.setDescription("Updated smartphone");
        request.setBrand("Apple");
        request.setColor("Blue");
        request.setCategory("Mobile");
        request.setUnitPrice(new BigDecimal("79999.00"));
        request.setActive(null);

        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response =
                productService.updateProduct(1L, request);

        // Assert: Existing active status must be preserved
        assertTrue(response.isActive());

        // Verify that the product was saved
        verify(productRepository, times(1)).save(existingProduct);
    }


    @Test //Testing for False remains False or not
    void updateProduct_shouldPreserveInactiveStatusWhenNotProvided() {

        // Arrange: Existing product is inactive
        Product existingProduct = new Product();
        existingProduct.setId(1L);
        existingProduct.setName("iPhone 16");
        existingProduct.setDescription("Old smartphone");
        existingProduct.setBrand("Apple");
        existingProduct.setColor("Black");
        existingProduct.setModel("IP16-BLA");
        existingProduct.setCategory("Mobile");
        existingProduct.setUnitPrice(new BigDecimal("69999.00"));
        existingProduct.setQuantityAvailable(10);
        existingProduct.setActive(false);

        // Prepare an update request without an active value
        UpdateProductRequest request = new UpdateProductRequest();
        request.setName("iPhone 17");
        request.setDescription("Updated smartphone");
        request.setBrand("Apple");
        request.setColor("Blue");
        request.setCategory("Mobile");
        request.setUnitPrice(new BigDecimal("79999.00"));
        request.setActive(null);

        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(existingProduct));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response =
                productService.updateProduct(1L, request);

        // Assert: Product must remain inactive
        assertFalse(response.isActive());

        // Verify that the product was saved
        verify(productRepository, times(1)).save(existingProduct);
    }


    @Test
    void getAllProducts_shouldReturnAllProductsWhenNoFiltersProvided() {

        // Arrange
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setModel("IP17-BLA");
        product.setCategory("Mobile");
        product.setUnitPrice(new BigDecimal("79999.00"));
        product.setQuantityAvailable(10);
        product.setActive(true);

        org.springframework.data.domain.Pageable pageable =
                org.springframework.data.domain.PageRequest.of(0, 10);

        org.springframework.data.domain.Page<Product> productPage =
                new org.springframework.data.domain.PageImpl<>(
                        java.util.List.of(product), pageable, 1);

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        // Act
        org.springframework.data.domain.Page<ProductResponse> response =
                productService.getAllProducts(0, 10, null, null);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getContent().size());
        assertEquals("iPhone 17", response.getContent().get(0).getName());
        assertEquals("IP17-BLA", response.getContent().get(0).getModel());

        verify(productRepository, times(1)).findAll(pageable);
    }


    @Test
    void getAllProducts_shouldFilterByCategory() {

        // Arrange
        Product product = new Product();
        product.setId(2L);
        product.setName("MacBook Air");
        product.setBrand("Apple");
        product.setColor("Silver");
        product.setModel("MBA-SIL");
        product.setCategory("Laptop");
        product.setUnitPrice(new BigDecimal("999.00"));
        product.setQuantityAvailable(5);
        product.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> productPage =
                new PageImpl<>(java.util.List.of(product), pageable, 1);

        when(productRepository.findByCategory("Laptop", pageable))
                .thenReturn(productPage);

        // Act
        Page<ProductResponse> response =
                productService.getAllProducts(0, 10, "Laptop", null);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals("MacBook Air", response.getContent().get(0).getName());
        assertEquals("Laptop", response.getContent().get(0).getCategory());

        verify(productRepository, times(1))
                .findByCategory("Laptop", pageable);

        verify(productRepository, never()).findAll(pageable);
    }


    @Test
    void getAllProducts_shouldFilterByActiveStatus() {

        // Arrange
        Product product = new Product();
        product.setId(3L);
        product.setName("MacBook Pro");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setModel("MPB-BLA");
        product.setCategory("Laptop");
        product.setUnitPrice(new BigDecimal("1499.00"));
        product.setQuantityAvailable(3);
        product.setActive(false);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> productPage =
                new PageImpl<>(java.util.List.of(product), pageable, 1);

        when(productRepository.findByActive(false, pageable))
                .thenReturn(productPage);

        // Act
        Page<ProductResponse> response =
                productService.getAllProducts(0, 10, null, false);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals("MacBook Pro", response.getContent().get(0).getName());
        assertFalse(response.getContent().get(0).isActive());

        verify(productRepository, times(1))
                .findByActive(false, pageable);

        verify(productRepository, never()).findAll(pageable);
    }


    @Test
    void getAllProducts_shouldFilterByCategoryAndActiveStatus() {

        // Arrange
        Product product = new Product();
        product.setId(4L);
        product.setName("MacBook Air");
        product.setBrand("Apple");
        product.setColor("Silver");
        product.setModel("MBA-SIL");
        product.setCategory("Laptop");
        product.setUnitPrice(new BigDecimal("999.00"));
        product.setQuantityAvailable(5);
        product.setActive(true);

        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> productPage =
                new PageImpl<>(java.util.List.of(product), pageable, 1);

        when(productRepository.findByCategoryAndActive(
                "Laptop", true, pageable))
                .thenReturn(productPage);

        // Act
        Page<ProductResponse> response =
                productService.getAllProducts(0, 10, "Laptop", true);

        // Assert
        assertEquals(1, response.getTotalElements());
        assertEquals("Laptop", response.getContent().get(0).getCategory());
        assertTrue(response.getContent().get(0).isActive());

        verify(productRepository, times(1))
                .findByCategoryAndActive("Laptop", true, pageable);

        verify(productRepository, never()).findAll(pageable);
    }


    @Test
    void getAllProducts_shouldIgnoreBlankCategory() {

        // Arrange
        Pageable pageable = PageRequest.of(0, 10);

        Page<Product> emptyPage =
                new PageImpl<>(java.util.List.of(), pageable, 0);

        when(productRepository.findAll(pageable))
                .thenReturn(emptyPage);

        // Act: Category contains only spaces, and active is omitted
        Page<ProductResponse> response =
                productService.getAllProducts(0, 10, "   ", null);

        // Assert
        assertNotNull(response);
        assertEquals(0, response.getTotalElements());

        // Blank category should use the unfiltered repository method
        verify(productRepository, times(1)).findAll(pageable);

        verify(productRepository, never())
                .findByCategory(anyString(), any(Pageable.class));
    }


    @Test
    void getAllProducts_shouldReturnPaginatedResults() {

        // Arrange: Create three products
        Product product1 = new Product();
        product1.setId(1L);
        product1.setName("iPhone 17");
        product1.setBrand("Apple");
        product1.setColor("Black");
        product1.setModel("IP17-BLA");
        product1.setCategory("Mobile");
        product1.setUnitPrice(new BigDecimal("79999.00"));
        product1.setQuantityAvailable(10);
        product1.setActive(true);

        Product product2 = new Product();
        product2.setId(2L);
        product2.setName("MacBook Air");
        product2.setBrand("Apple");
        product2.setColor("Silver");
        product2.setModel("MBA-SIL");
        product2.setCategory("Laptop");
        product2.setUnitPrice(new BigDecimal("999.00"));
        product2.setQuantityAvailable(5);
        product2.setActive(true);

        Pageable pageable = PageRequest.of(0, 2);

        Page<Product> productPage =
                new PageImpl<>(
                        java.util.List.of(product1, product2),
                        pageable,
                        3
                );

        when(productRepository.findAll(pageable))
                .thenReturn(productPage);

        // Act: Request the first page, with two products per page
        Page<ProductResponse> response =
                productService.getAllProducts(0, 2, null, null);

        // Assert
        assertEquals(3, response.getTotalElements());
        assertEquals(2, response.getContent().size());
        assertEquals(2, response.getSize());
        assertEquals(0, response.getNumber());

        verify(productRepository, times(1)).findAll(pageable);
    }


    @Test
    void deleteProduct_shouldThrowExceptionWhenProductHasOrderItems() {

        // Arrange: Create a product associated with order history
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setModel("IP17-BLA");
        product.setUnitPrice(new BigDecimal("79999.00"));
        product.setQuantityAvailable(10);

        // Simulate an existing order item
        OrderItem orderItem = new OrderItem();
        product.getOrderItems().add(orderItem);

        when(productRepository.findById(1L))
                .thenReturn(java.util.Optional.of(product));

        // Act and Assert
        BusinessConflictException exception = assertThrows(
                BusinessConflictException.class,
                () -> productService.deleteProduct(1L)
        );

        assertEquals(
                "Cannot delete this product because it is associated "
                        + "with existing order items. Deactivate it instead.",
                exception.getMessage()
        );

        // Product must not be deleted
        verify(productRepository, never()).delete(any(Product.class));
    }


    @Test
    void deleteProduct_shouldDeleteProductWhenNoOrderItemsExist() {

        // Arrange: Create a product without order history
        Product product = new Product();
        product.setId(2L);
        product.setName("MacBook Air");
        product.setBrand("Apple");
        product.setColor("Silver");
        product.setModel("MBA-SIL");
        product.setUnitPrice(new BigDecimal("999.00"));
        product.setQuantityAvailable(5);

        when(productRepository.findById(2L))
                .thenReturn(java.util.Optional.of(product));

        // Act
        productService.deleteProduct(2L);

        // Assert: Verify the product was deleted
        verify(productRepository, times(1)).delete(product);
    }


    @Test
    void updateProductQuantity_shouldUpdateQuantityAndReturnResponse() {

        // Arrange: Create an existing product
        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone 17");
        product.setDescription("Apple smartphone");
        product.setBrand("Apple");
        product.setColor("Black");
        product.setModel("IP17-BLA");
        product.setCategory("Mobile");
        product.setUnitPrice(new BigDecimal("79999.00"));
        product.setQuantityAvailable(10);
        product.setActive(true);

        // Prepare the requested quantity
        UpdateProductQuantityRequest request =
                new UpdateProductQuantityRequest();
        request.setQuantityAvailable(25);

        when(productRepository.findByIdForUpdate(1L))
                .thenReturn(java.util.Optional.of(product));

        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ProductResponse response =
                productService.updateProductQuantity(1L, request);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(25, response.getQuantityAvailable());
        assertNotNull(response.getUpdatedAt());

        verify(productRepository, times(1)).findByIdForUpdate(1L);
        verify(productRepository, times(1)).save(product);
    }


    @Test
    void updateProductQuantity_shouldThrowExceptionWhenProductNotFound() {

        // Arrange
        UpdateProductQuantityRequest request =
                new UpdateProductQuantityRequest();
        request.setQuantityAvailable(25);

        when(productRepository.findByIdForUpdate(99L))
                .thenReturn(java.util.Optional.empty());

        // Act and Assert
        ProductNotFoundException exception = assertThrows(
                ProductNotFoundException.class,
                () -> productService.updateProductQuantity(99L, request)
        );

        assertEquals(
                "Product not found with id: 99",
                exception.getMessage()
        );

        // Verify that no product was saved
        verify(productRepository, never()).save(any(Product.class));
    }



}
