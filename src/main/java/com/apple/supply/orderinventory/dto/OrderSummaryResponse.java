package com.apple.supply.orderinventory.dto;

import com.apple.supply.orderinventory.model.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderSummaryResponse {

    private Long id;
    private Long customerId;
    private LocalDateTime orderDateTime;
    private BigDecimal totalAmount;
    private OrderStatus status;
}
