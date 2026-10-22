
package com.apple.supply.orderinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ProductResponse {

    private Long id;
    private String name;
    private String description;
    private String brand;
    private String model;
    private String color;
    private String category;
    private BigDecimal unitPrice;
    private boolean active;
    private int quantityAvailable;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
