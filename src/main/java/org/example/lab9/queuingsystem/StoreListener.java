package org.example.lab9.queuingsystem;

import org.example.lab9.Item;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface StoreListener {
    void onOrderCompleted(UUID orderId,
                          Map<Item, Integer> purchasedItems,
                          BigDecimal totalCost,
                          Map<Item, Integer> pendingItems);

    void onOrderCancelled(UUID orderId, String reason);

    void onOrderFulfilled(UUID orderId,
                          Map<Item, Integer> fulfilledItems,
                          BigDecimal additionalCost);
}
