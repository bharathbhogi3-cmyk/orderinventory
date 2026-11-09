
package com.apple.supply.orderinventory.service;

import com.apple.supply.orderinventory.dto.CreateProductRequest;
import com.apple.supply.orderinventory.dto.ProductResponse;
import com.apple.supply.orderinventory.dto.UpdateProductRequest;
import com.apple.supply.orderinventory.exception.BusinessConflictException;
import com.apple.supply.orderinventory.exception.ProductNotFoundException;
import com.apple.supply.orderinventory.model.Product;
import com.apple.supply.orderinventory.repository.ProductRepository;
import com.apple.supply.orderinventory.specification.ProductSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.apple.supply.orderinventory.dto.UpdateProductQuantityRequest;
import org.springframework.data.domain.Sort;
import java.util.Set;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    @CacheEvict(
            cacheNames = {"products", "productsByCategory"},
            allEntries = true
    )
    public ProductResponse createProduct(CreateProductRequest request) {

        log.info("Creating product with name: {}", request.getName());

        LocalDateTime now = LocalDateTime.now();

        Product product = new Product();

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setModel(
                generateModelCode(request.getName(), request.getColor())
        );
        product.setCategory(request.getCategory());
        product.setUnitPrice(request.getUnitPrice());
        product.setQuantityAvailable(request.getQuantityAvailable());

        // New products are active by default.
        product.setActive(true);

        product.setCreatedAt(now);
        product.setUpdatedAt(now);

        Product savedProduct = productRepository.save(product);

        log.info("Product created successfully. Product ID: {}, Model: {}",
                savedProduct.getId(),
                savedProduct.getModel());

        return toResponse(savedProduct);
    }


    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = "products",
            key = "#p0 + '_' + #p1 + '_' + #p2 + '_' + #p3 + '_' + #p4 + '_' + #p5 + '_' + #p6 + '_' + #p7 + '_' + #p8 + '_' + #p9 + '_' + #p10"
    )
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String name,
            String category,
            String brand,
            String color,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            String sortBy,
            String direction) {

        log.info(
                "Fetching products. Page: {}, Size: {}, Name: {}, Category: {}, Brand: {}, Color: {}, Active: {}, MinPrice: {}, MaxPrice: {}, SortBy: {}, Direction: {}",
                page, size, name, category, brand, color, active,
                minPrice, maxPrice, sortBy, direction
        );

        // Allowed fields for sorting.
        Set<String> allowedSortFields = Set.of(
                "id",
                "name",
                "category",
                "unitPrice",
                "createdAt"
        );

        // Apply default sorting values.
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        if (direction == null || direction.isBlank()) {
            direction = "desc";
        }

        // Validate sorting field.
        if (!allowedSortFields.contains(sortBy)) {
            throw new IllegalArgumentException(
                    "Invalid sort field: " + sortBy);
        }

        // Validate sorting direction.
        Sort.Direction sortDirection;

        try {
            sortDirection = Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "Invalid sort direction. Use 'asc' or 'desc'.");
        }

        if (page < 0) {
            throw new IllegalArgumentException(
                    "Page number cannot be negative.");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException(
                    "Page size must be between 1 and 100.");
        }

        // Create pagination with sorting.
        Pageable pageable = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortBy)
        );

        Specification<Product> specification =
                ProductSpecification.withFilters(
                        name,
                        category,
                        brand,
                        color,
                        active,
                        minPrice,
                        maxPrice
                );

        Page<Product> products =
                productRepository.findAll(specification, pageable);

        log.info(
                "Successfully retrieved {} products",
                products.getTotalElements()
        );

        return products.map(this::toResponse);
    }


    @Transactional(readOnly = true)
    @Cacheable(value = "productsById", key = "#p0")
    public ProductResponse getProductById(Long productId) {

        log.info("Fetching product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        log.info("Product retrieved successfully. Product ID: {}, Model: {}",
                product.getId(), product.getModel());
        return toResponse(product);
    }


    @Transactional
    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "productsById",
                    key = "#p0"
            ),
            @CacheEvict(
                    cacheNames = {"products", "productsByCategory"},
                    allEntries = true
            )
    })
    public ProductResponse updateProduct(
            Long productId,
            UpdateProductRequest request) {

        log.info("Updating product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setColor(request.getColor());
        product.setCategory(request.getCategory());
        product.setUnitPrice(request.getUnitPrice());

        // Regenerate model from the updated name and color.
        product.setModel(
                generateModelCode(request.getName(), request.getColor())
        );

        // Update active status only when it is provided.
        if (request.getActive() != null) {
            product.setActive(request.getActive());
        }

        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);

        log.info("Product updated successfully. Product ID: {}, Model: {}",
                updatedProduct.getId(),
                updatedProduct.getModel());

        return toResponse(updatedProduct);
    }

    private ProductResponse toResponse(Product product) {

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getBrand(),
                product.getModel(),
                product.getColor(),
                product.getCategory(),
                product.getUnitPrice(),
                product.isActive(),
                product.getQuantityAvailable(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private String generateModelCode(String productName, String color) {

        if (productName == null || productName.isBlank()) {
            throw new IllegalArgumentException(
                    "Product name cannot be empty."
            );
        }

        if (color == null || color.isBlank()) {
            throw new IllegalArgumentException(
                    "Product color cannot be empty."
            );
        }

        // Split camel-case names: iPhone becomes i Phone.
        String formattedName = productName
                .replaceAll("([a-z])([A-Z])", "$1 $2");

        StringBuilder productCode = new StringBuilder();

        for (String word : formattedName.split("[^a-zA-Z0-9]+")) {

            if (!word.isEmpty()) {

                char firstChar = word.charAt(0);

                if (Character.isLetter(firstChar)) {
                    productCode.append(
                            Character.toUpperCase(firstChar)
                    );
                }

                // Preserve digits, such as 17 in iPhone 17.
                String digits = word.replaceAll("[^0-9]", "");
                productCode.append(digits);
            }
        }

        String colorCode = color
                .replaceAll("[^a-zA-Z]", "")
                .toUpperCase(Locale.ROOT);

        if (productCode.isEmpty() || colorCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "Unable to generate model code from product name and color."
            );
        }

        if (colorCode.length() >= 3) {
            colorCode = colorCode.substring(0, 3);
        }

        return productCode + "-" + colorCode;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "productsById",
                    key = "#p0"
            ),
            @CacheEvict(
                    cacheNames = {"products", "productsByCategory"},
                    allEntries = true
            )
    })
    public void deleteProduct(Long productId) {

        log.info("Deleting product with ID: {}", productId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        // Preserve products that are referenced by order history.
        if (!product.getOrderItems().isEmpty()) {
            throw new BusinessConflictException(
                    "Cannot delete this product because it is associated "
                            + "with existing order items. Deactivate it instead."
            );
        }

        productRepository.delete(product);

        log.info("Product deleted successfully. Product ID: {}, Model: {}",
                product.getId(), product.getModel());
    }

    // Updates the Quantity of the product(Replaces the current Quantity)
    @Transactional
    @Caching(evict = {
            @CacheEvict(
                    cacheNames = "productsById",
                    key = "#p0"
            ),
            @CacheEvict(
                    cacheNames = {"products", "productsByCategory"},
                    allEntries = true
            )
    })
    public ProductResponse updateProductQuantity(
            Long productId,
            UpdateProductQuantityRequest request) {

        log.info("Updating quantity for product ID: {}", productId);

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        product.setQuantityAvailable(request.getQuantityAvailable());
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);

        log.info(
                "Product quantity updated successfully. Product ID: {}, New quantity: {}",
                updatedProduct.getId(),
                updatedProduct.getQuantityAvailable());

        return toResponse(updatedProduct);
    }
}
