package com.apple.supply.orderinventory.specification;

import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.OrderItem;
import com.apple.supply.orderinventory.model.OrderStatus;
import com.apple.supply.orderinventory.model.Product;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> withFilters(
            Long customerId,
            OrderStatus status,
            String productName,
            String category) {

        return (root, query, criteriaBuilder) -> {

            List<jakarta.persistence.criteria.Predicate> predicates =
                    new ArrayList<>();

            // Filter orders by customer ID.
            if (customerId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("customer").get("id"),
                        customerId
                ));
            }

            // Filter orders by status.
            if (status != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        status
                ));
            }

            // Join Order -> OrderItem -> Product when product filters are used.
            if ((productName != null && !productName.isBlank())
                    || (category != null && !category.isBlank())) {

                Join<Order, OrderItem> orderItemJoin =
                        root.join("orderItems");

                Join<OrderItem, Product> productJoin =
                        orderItemJoin.join("product");

                // Filter by product name.
                if (productName != null && !productName.isBlank()) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    productJoin.get("name")
                            ),
                            "%" + productName.trim().toLowerCase() + "%"
                    ));
                }

                // Filter by product category.
                if (category != null && !category.isBlank()) {
                    predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(
                                    productJoin.get("category")
                            ),
                            "%" + category.trim().toLowerCase() + "%"
                    ));
                }

                // Avoid duplicate orders caused by joining multiple order items.
                query.distinct(true);
            }

            return criteriaBuilder.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }
}