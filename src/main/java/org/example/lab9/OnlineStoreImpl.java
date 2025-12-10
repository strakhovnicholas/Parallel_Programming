package org.example.lab9;

import org.example.lab9.queuingsystem.Client;
import org.example.lab9.queuingsystem.OnlineStoreAPI;
import org.example.lab9.queuingsystem.StoreListener;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OnlineStoreImpl implements OnlineStoreAPI {
    private final Map<UUID, Map<Item, Integer>> inventory = new HashMap<>();
    private final Map<UUID, Order> orders = new HashMap<>();
    private final Map<UUID, Client> clients = new HashMap<>();

    private Lock inventoryLock = new ReentrantLock();
    private Lock ordersLock = new ReentrantLock();
    private Lock clientsLock = new ReentrantLock();

    @Override
    public UUID registerClient(String name, String userName) {
        this.clientsLock.lock();
        try {
            Client client = new Client(name, userName);
            this.clients.put(client.getUserId(), client);
            return client.getUserId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            this.clientsLock.unlock();
        }
    }

    @Override
    public Order createOrder(UUID clientId, Map<Item, Integer> selectedItemsForOrdering) {
        Map<Item, Integer> itemsInStock = new HashMap<>();
        Map<Item, Integer> itemsNotInStock = new HashMap<>();

        for (var entry: selectedItemsForOrdering.entrySet()){
            Item selectedItem = entry.getKey();
            int requiredItemCount = entry.getValue();
//            if (this.inventory.get(item.getItemId()).values().su)
        }
        return null;
    }

    @Override
    public boolean cancelOrder(UUID clientId, UUID orderId) {
        return false;
    }

    @Override
    public Integer getStoreStatus() {
        return 1;
    }

    @Override
    public void deliverGoods(Map<Item, Integer> items) {

    }
}
