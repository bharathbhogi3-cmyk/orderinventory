
package com.apple.supply.orderinventory.model;

import com.apple.supply.orderinventory.model.Customer;
import com.apple.supply.orderinventory.model.OrderItem;
import com.apple.supply.orderinventory.model.OrderStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    private LocalDateTime orderDateTime;

    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    private OrderStatus status;

    // Adding OneToMany relationship to manage multiple order items under one order.
    // PERSIST and MERGE are used to save new items and update existing items along with the order.
    // orphanRemoval is used to delete an item from the database when it is removed from the order.
    @OneToMany(
            mappedBy = "order",
            cascade = {
                    CascadeType.PERSIST,
                    CascadeType.MERGE
            },
            orphanRemoval = true
    )
    private List<OrderItem> orderItems = new ArrayList<>();

    // Method to add an order item and set its parent order.
    public void addOrderItem(OrderItem item) {
        orderItems.add(item);
        item.setOrder(this);
    }

    // Method to remove an order item and clear its parent order reference.
    public void removeOrderItem(OrderItem item) {
        orderItems.remove(item);
        item.setOrder(null);
    }
}