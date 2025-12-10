package org.example.lab9;

import org.example.lab9.queuingsystem.StoreListener;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public class PersonalListener implements StoreListener {
    @Override
    public void onOrderCompleted(UUID orderId, Map<Item, Integer> purchasedItems,
                                 BigDecimal totalCost, Map<Item, Integer> pendingItems) {

    }

    @Override
    public void onOrderCancelled(UUID orderId, String reason) {

    }

    @Override
    public void onOrderFulfilled(UUID orderId, Map<Item, Integer> fulfilledItems, BigDecimal additionalCost) {

    }
}
