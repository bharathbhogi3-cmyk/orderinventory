package com.apple.supply.orderinventory.specification;

import com.apple.supply.orderinventory.model.Product;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductSpecification {

    public static Specification<Product> withFilters(
            String name,
            String category,
            String brand,
            String color,
            Boolean active,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, criteriaBuilder) -> {

            List<Predicate> predicates = new ArrayList<>();

            // Filter by product name (partial match)
            if (name != null && !name.isBlank()) {
                predicates.add(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.<String>get("name")),
                                "%" + name.trim().toLowerCase(Locale.ROOT) + "%"
                        )
                );
            }

            // Filter by category
            if (category != null && !category.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("category"), category.trim()
                        )
                );
            }

            // Filter by brand
            if (brand != null && !brand.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("brand"), brand.trim()
                        )
                );
            }

            // Filter by color
            if (color != null && !color.isBlank()) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("color"), color.trim()
                        )
                );
            }

            // Filter by active status
            if (active != null) {
                predicates.add(
                        criteriaBuilder.equal(
                                root.get("active"), active
                        )
                );
            }

            // Filter by minimum price
            if (minPrice != null) {
                predicates.add(
                        criteriaBuilder.greaterThanOrEqualTo(
                                root.<BigDecimal>get("unitPrice"), minPrice
                        )
                );
            }

            // Filter by maximum price
            if (maxPrice != null) {
                predicates.add(
                        criteriaBuilder.lessThanOrEqualTo(
                                root.<BigDecimal>get("unitPrice"), maxPrice
                        )
                );
            }

            return criteriaBuilder.and(
                    predicates.toArray(new Predicate[0])
            );
        };
    }
}
