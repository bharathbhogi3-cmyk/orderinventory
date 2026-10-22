
package com.apple.supply.orderinventory.model;

public enum OrderStatus {

    // Order has been created but inventory availability
    // has not yet been checked and confirmed.
    DRAFT,

    // Sufficient inventory is available for all order items,
    // and the order is ready to proceed with fulfillment.
    CONFIRMED,

    // All items in the order have been fulfilled completely.
    FULFILLED,

    // The order has been canceled because of a cancellation
    // request or a business rule that prevents it from proceeding.
    CANCELLED
}
