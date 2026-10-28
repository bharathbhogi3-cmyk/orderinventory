
package com.apple.supply.orderinventory.service;

import com.apple.supply.orderinventory.dto.CreateProductRequest;
import com.apple.supply.orderinventory.dto.ProductResponse;
import com.apple.supply.orderinventory.dto.UpdateProductRequest;
import com.apple.supply.orderinventory.exception.BusinessConflictException;
import com.apple.supply.orderinventory.exception.ProductNotFoundException;
import com.apple.supply.orderinventory.model.Product;
import com.apple.supply.orderinventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.apple.supply.orderinventory.dto.UpdateProductQuantityRequest;

import java.time.LocalDateTime;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse createProduct(CreateProductRequest request) {

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

        return toResponse(savedProduct);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getAllProducts(
            int page,
            int size,
            String category,
            Boolean active) {

        Pageable pageable = PageRequest.of(page, size);

        Page<Product> products;

        boolean hasCategory =
                category != null && !category.isBlank();

        if (hasCategory && active != null) {
            products = productRepository.findByCategoryAndActive(
                    category, active, pageable);
        } else if (hasCategory) {
            products = productRepository.findByCategory(
                    category, pageable);
        } else if (active != null) {
            products = productRepository.findByActive(
                    active, pageable);
        } else {
            products = productRepository.findAll(pageable);
        }

        return products.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long productId) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        return toResponse(product);
    }


    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            UpdateProductRequest request) {

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
    public void deleteProduct(Long productId) {

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
    }

    // Updates the Quantity of the product(Replaces the current Quantity)
    @Transactional
    public ProductResponse updateProductQuantity(
            Long productId,
            UpdateProductQuantityRequest request) {

        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new ProductNotFoundException(
                        "Product not found with id: " + productId
                ));

        product.setQuantityAvailable(request.getQuantityAvailable());
        product.setUpdatedAt(LocalDateTime.now());

        Product updatedProduct = productRepository.save(product);

        return toResponse(updatedProduct);
    }
}
