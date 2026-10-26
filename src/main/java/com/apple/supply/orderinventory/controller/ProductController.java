package com.apple.supply.orderinventory.controller;

import com.apple.supply.orderinventory.dto.CreateProductRequest;
import com.apple.supply.orderinventory.dto.ProductResponse;
import com.apple.supply.orderinventory.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import com.apple.supply.orderinventory.dto.UpdateProductRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import com.apple.supply.orderinventory.dto.UpdateProductQuantityRequest;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Create a new product.
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse createProduct(
            @Valid @RequestBody CreateProductRequest request) {

        return productService.createProduct(request);
    }

    // Get all products with optional filters
    @GetMapping
    public Page<ProductResponse> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean active) {

        return productService.getAllProducts(
                page, size, category, active);
    }

    // Get product details by id
    @GetMapping("/{productId}")
    public ProductResponse getProductById(
            @PathVariable Long productId) {
        return productService.getProductById(productId);
    }

    // Update an existing product
    @PutMapping("/{productId}")
    public ProductResponse updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductRequest request) {

        return productService.updateProduct(productId, request);
    }

    @DeleteMapping("/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
    }

    // Update product available quantity
    @PatchMapping("/{productId}/quantity")
    public ProductResponse updateProductQuantity(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateProductQuantityRequest request) {

        return productService.updateProductQuantity(productId, request);
    }
}
