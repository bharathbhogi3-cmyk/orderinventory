package com.apple.supply.orderinventory.model;
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
@Table(name="products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private String brand;
    @Column(name = "model", nullable = false, unique = true)
    private String model;
    private String color;
    private String category;
    private BigDecimal unitPrice;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable;

    @OneToMany(mappedBy = "product")
    private List<OrderItem> orderItems = new ArrayList<>();



}

