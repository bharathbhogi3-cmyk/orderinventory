package com.apple.supply.orderinventory.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
@Entity
@Table(name="order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;
    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;
    @Column(nullable = false)
    private int quantity;
    private BigDecimal unitPrice;
}
