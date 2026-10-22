
package com.apple.supply.orderinventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateProductRequest {

    @NotBlank(message = "Product name is required.")
    private String name;

    private String description;

    private String brand;

    @NotBlank(message = "Product color is required.")
    private String color;

    private String category;

    @NotNull(message = "Unit price is required.")
    @DecimalMin(
            value = "0.01",
            message = "Unit price must be greater than zero."
    )
    private BigDecimal unitPrice;

    @NotNull(message = "Available quantity is required.")
    @PositiveOrZero(
            message = "Available quantity cannot be negative."
    )
    private Integer quantityAvailable;
}
