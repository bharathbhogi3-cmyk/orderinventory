package com.apple.supply.orderinventory.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
public class OrderItemResponse {

    private Long id;
    private Long productId;
    private String productName;
    private int quantity;
    private BigDecimal unitPrice;
}
