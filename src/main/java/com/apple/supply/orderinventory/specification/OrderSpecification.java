package com.apple.supply.orderinventory.specification;

import com.apple.supply.orderinventory.model.Order;
import com.apple.supply.orderinventory.model.OrderStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class OrderSpecification {

    public static Specification<Order> withFilters(
            Long customerId,
            OrderStatus status) {

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

            return criteriaBuilder.and(
                    predicates.toArray(
                            new jakarta.persistence.criteria.Predicate[0]
                    )
            );
        };
    }
}