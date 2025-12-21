package org.example.lab9;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.example.lab9.disruptor.StoreEvent;
import org.example.lab9.disruptor.StoreEventHandler;
import org.example.lab9.queuingsystem.Client;
import org.example.lab9.queuingsystem.OnlineStoreAPI;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class OnlineStoreDisruptorImpl implements OnlineStoreAPI {
    private static final int BUFFER_SIZE = 1024 * 1024;
    
    private final Disruptor<StoreEvent> disruptor;
    private final RingBuffer<StoreEvent> ringBuffer;
    private final StoreEventHandler eventHandler;

    public OnlineStoreDisruptorImpl() {
        this.eventHandler = new StoreEventHandler();
        this.disruptor = new Disruptor<>(
            StoreEvent::new,
            BUFFER_SIZE,
            r -> {
                Thread t = new Thread(r, "StoreDisruptorProcessor");
                t.setDaemon(true);
                return t;
            }
        );
        
        disruptor.handleEventsWith(eventHandler);
        this.ringBuffer = disruptor.getRingBuffer();
        disruptor.start();
    }

    @Override
    public UUID registerClient(String name, String userName) {
        CompletableFuture<UUID> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            StoreEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setType(StoreEvent.EventType.REGISTER_CLIENT);
            event.setOperationId(UUID.randomUUID());
            event.setName(name);
            event.setUserName(userName);
            event.setRegisterClientFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to register client", e);
        }
    }

    @Override
    public Order createOrder(UUID clientId, Map<Item, Integer> items) {
        if (items == null || items.isEmpty()) {
            return null;
        }
        
        CompletableFuture<Order> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            StoreEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setType(StoreEvent.EventType.CREATE_ORDER);
            event.setOperationId(UUID.randomUUID());
            event.setClientId(clientId);
            event.setItems(items);
            event.setCreateOrderFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create order", e);
        }
    }

    @Override
    public boolean cancelOrder(UUID clientId, UUID orderId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            StoreEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setType(StoreEvent.EventType.CANCEL_ORDER);
            event.setOperationId(UUID.randomUUID());
            event.setClientId(clientId);
            event.setOrderId(orderId);
            event.setCancelOrderFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cancel order", e);
        }
    }

    @Override
    public Map<Item, Integer> getStoreStatus() {
        CompletableFuture<Map<Item, Integer>> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            StoreEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setType(StoreEvent.EventType.GET_STORE_STATUS);
            event.setOperationId(UUID.randomUUID());
            event.setGetStatusFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            return future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get store status", e);
        }
    }

    @Override
    public void deliverGoods(Map<Item, Integer> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        
        CompletableFuture<Void> future = new CompletableFuture<>();
        long sequence = ringBuffer.next();
        try {
            StoreEvent event = ringBuffer.get(sequence);
            event.clear();
            event.setType(StoreEvent.EventType.DELIVER_GOODS);
            event.setOperationId(UUID.randomUUID());
            event.setItems(items);
            event.setDeliverGoodsFuture(future);
        } finally {
            ringBuffer.publish(sequence);
        }
        
        try {
            future.get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deliver goods", e);
        }
    }

    public void shutdown() {
        eventHandler.shutdownStore();
        disruptor.shutdown();
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public BigDecimal getStoreMoney() {
        return eventHandler.getStoreMoney();
    }

    public Map<UUID, Order> getAllOrders() {
        return eventHandler.getAllOrders();
    }

    public Client getClient(UUID clientId) {
        return eventHandler.getClient(clientId);
    }
}

