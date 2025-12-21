package org.example.lab9.disruptor;

import org.example.lab9.Item;
import org.example.lab9.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class StoreEvent {
    private EventType type;
    private UUID operationId;
    
    private String name;
    private String userName;
    private UUID clientId;
    private Map<Item, Integer> items;
    private UUID orderId;
    
    private CompletableFuture<UUID> registerClientFuture;
    private CompletableFuture<Order> createOrderFuture;
    private CompletableFuture<Boolean> cancelOrderFuture;
    private CompletableFuture<Map<Item, Integer>> getStatusFuture;
    private CompletableFuture<Void> deliverGoodsFuture;
    
    private UUID resultClientId;
    private Order resultOrder;
    private Boolean resultBoolean;
    private Map<Item, Integer> resultStatus;
    
    public void clear() {
        type = null;
        operationId = null;
        name = null;
        userName = null;
        clientId = null;
        items = null;
        orderId = null;
        registerClientFuture = null;
        createOrderFuture = null;
        cancelOrderFuture = null;
        getStatusFuture = null;
        deliverGoodsFuture = null;
        resultClientId = null;
        resultOrder = null;
        resultBoolean = null;
        resultStatus = null;
    }
    
    public enum EventType {
        REGISTER_CLIENT,
        CREATE_ORDER,
        CANCEL_ORDER,
        GET_STORE_STATUS,
        DELIVER_GOODS
    }
    
    public EventType getType() {
        return type;
    }
    
    public void setType(EventType type) {
        this.type = type;
    }
    
    public UUID getOperationId() {
        return operationId;
    }
    
    public void setOperationId(UUID operationId) {
        this.operationId = operationId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getUserName() {
        return userName;
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public UUID getClientId() {
        return clientId;
    }
    
    public void setClientId(UUID clientId) {
        this.clientId = clientId;
    }
    
    public Map<Item, Integer> getItems() {
        return items;
    }
    
    public void setItems(Map<Item, Integer> items) {
        this.items = items != null ? new HashMap<>(items) : null;
    }
    
    public UUID getOrderId() {
        return orderId;
    }
    
    public void setOrderId(UUID orderId) {
        this.orderId = orderId;
    }
    
    public CompletableFuture<UUID> getRegisterClientFuture() {
        return registerClientFuture;
    }
    
    public void setRegisterClientFuture(CompletableFuture<UUID> registerClientFuture) {
        this.registerClientFuture = registerClientFuture;
    }
    
    public CompletableFuture<Order> getCreateOrderFuture() {
        return createOrderFuture;
    }
    
    public void setCreateOrderFuture(CompletableFuture<Order> createOrderFuture) {
        this.createOrderFuture = createOrderFuture;
    }
    
    public CompletableFuture<Boolean> getCancelOrderFuture() {
        return cancelOrderFuture;
    }
    
    public void setCancelOrderFuture(CompletableFuture<Boolean> cancelOrderFuture) {
        this.cancelOrderFuture = cancelOrderFuture;
    }
    
    public CompletableFuture<Map<Item, Integer>> getGetStatusFuture() {
        return getStatusFuture;
    }
    
    public void setGetStatusFuture(CompletableFuture<Map<Item, Integer>> getStatusFuture) {
        this.getStatusFuture = getStatusFuture;
    }
    
    public CompletableFuture<Void> getDeliverGoodsFuture() {
        return deliverGoodsFuture;
    }
    
    public void setDeliverGoodsFuture(CompletableFuture<Void> deliverGoodsFuture) {
        this.deliverGoodsFuture = deliverGoodsFuture;
    }
    
    public UUID getResultClientId() {
        return resultClientId;
    }
    
    public void setResultClientId(UUID resultClientId) {
        this.resultClientId = resultClientId;
    }
    
    public Order getResultOrder() {
        return resultOrder;
    }
    
    public void setResultOrder(Order resultOrder) {
        this.resultOrder = resultOrder;
    }
    
    public Boolean getResultBoolean() {
        return resultBoolean;
    }
    
    public void setResultBoolean(Boolean resultBoolean) {
        this.resultBoolean = resultBoolean;
    }
    
    public Map<Item, Integer> getResultStatus() {
        return resultStatus;
    }
    
    public void setResultStatus(Map<Item, Integer> resultStatus) {
        this.resultStatus = resultStatus != null ? new HashMap<>(resultStatus) : null;
    }
}

