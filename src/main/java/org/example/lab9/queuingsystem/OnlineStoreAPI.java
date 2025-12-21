package org.example.lab9.queuingsystem;

import org.example.lab9.Item;
import org.example.lab9.Order;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public interface OnlineStoreAPI {

    UUID registerClient(String name, String userName);

    Order createOrder(UUID clientId, Map<Item, Integer> items);

    boolean cancelOrder(UUID clientId, UUID orderId);

    Map<Item, Integer> getStoreStatus();

    void deliverGoods(Map<Item, Integer> items);
}