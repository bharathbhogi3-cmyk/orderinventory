package com.apple.supply.orderinventory.dto;

import com.apple.supply.orderinventory.model.OrderStatus;
import jakarta.validation.Valid;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UpdateOrderRequest {

    private OrderStatus status;

    @Valid
    private List<UpdateOrderItemRequest> items;
}
