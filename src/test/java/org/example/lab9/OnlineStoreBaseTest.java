package org.example.lab9;

import org.example.lab9.queuingsystem.Client;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

abstract class OnlineStoreBaseTest {

    protected abstract org.example.lab9.queuingsystem.OnlineStoreAPI createStore();
    protected abstract void shutdownStore(org.example.lab9.queuingsystem.OnlineStoreAPI store);
    
    protected org.example.lab9.queuingsystem.Client getClient(org.example.lab9.queuingsystem.OnlineStoreAPI store, UUID clientId) {
        if (store instanceof OnlineStoreImpl) {
            return ((OnlineStoreImpl) store).getClient(clientId);
        } else if (store instanceof OnlineStoreQueueImpl) {
            return ((OnlineStoreQueueImpl) store).getClient(clientId);
        } else if (store instanceof OnlineStoreDisruptorImpl) {
            return ((OnlineStoreDisruptorImpl) store).getClient(clientId);
        }
        return null;
    }
    
    protected Map<UUID, Order> getAllOrders(org.example.lab9.queuingsystem.OnlineStoreAPI store) {
        if (store instanceof OnlineStoreImpl) {
            return ((OnlineStoreImpl) store).getAllOrders();
        } else if (store instanceof OnlineStoreQueueImpl) {
            return ((OnlineStoreQueueImpl) store).getAllOrders();
        } else if (store instanceof OnlineStoreDisruptorImpl) {
            return ((OnlineStoreDisruptorImpl) store).getAllOrders();
        }
        return new HashMap<>();
    }

    private org.example.lab9.queuingsystem.OnlineStoreAPI store;
    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        store = createStore();
        item1 = createItem("Товар 1", new BigDecimal("100.00"));
        item2 = createItem("Товар 2", new BigDecimal("200.00"));
    }

    @Test
    @DisplayName("Тест 1: Успешная покупка всех товаров")
    void testSuccessfulPurchase() {
        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, 5);
        delivery.put(item2, 3);
        store.deliverGoods(delivery);

        UUID clientId = store.registerClient("Иван", "ivan_user");
        Client client = getClient(store, clientId);
        PersonalListener listener = (PersonalListener) client.getClientListener();
        listener.setInitialBalance(new BigDecimal("1000.00"));

        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 2);
        orderItems.put(item2, 1);

        Order order = store.createOrder(clientId, orderItems);

        assertNotNull(order, "Заказ не должен быть null");
        assertEquals(Order.OrderStatus.COMPLETED, order.getStatus(), "Заказ должен быть выполнен");
        assertEquals(2, order.getFulfilledItems().size(), "Должно быть 2 выполненных товара");
        assertTrue(order.getPendingItems().isEmpty(), "Не должно быть ожидающих товаров");
        assertEquals(0, order.getTotalPrice().compareTo(new BigDecimal("400.00")), 
                "Общая стоимость должна быть 400.00");

        Map<Item, Integer> status = store.getStoreStatus();
        assertEquals(3, status.get(item1), "Должно остаться 3 единицы товара 1");
        assertEquals(2, status.get(item2), "Должно остаться 2 единицы товара 2");
    }

    @Test
    @DisplayName("Тест 2: Частичная покупка с ожиданием поставки")
    void testPartialPurchaseWithDelivery() {
        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, 2);
        store.deliverGoods(delivery);

        UUID clientId = store.registerClient("Петр", "petr_user");
        Client client = getClient(store, clientId);
        PersonalListener listener = (PersonalListener) client.getClientListener();
        listener.setInitialBalance(new BigDecimal("1000.00"));

        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 1);
        orderItems.put(item2, 2);

        Order order = store.createOrder(clientId, orderItems);

        assertNotNull(order, "Заказ не должен быть null");
        assertEquals(Order.OrderStatus.PARTIAL, order.getStatus(), "Заказ должен быть частично выполнен");
        assertTrue(order.getFulfilledItems().containsKey(item1), "Товар 1 должен быть выполнен");
        assertTrue(order.getPendingItems().containsKey(item2), "Товар 2 должен быть в ожидании");
        assertEquals(0, order.getTotalPrice().compareTo(new BigDecimal("100.00")), 
                "Стоимость выполненных товаров должна быть 100.00");
        assertEquals(0, order.getBlockedAmount().compareTo(new BigDecimal("400.00")), 
                "Заблокированная сумма должна быть 400.00");
    }

    @Test
    @DisplayName("Тест 3: Поставка товаров и выполнение ожидающих заказов")
    void testDeliveryFulfillsPendingOrders() {
        UUID clientId = store.registerClient("Мария", "maria_user");
        Client client = getClient(store, clientId);
        PersonalListener listener = (PersonalListener) client.getClientListener();
        listener.setInitialBalance(new BigDecimal("1000.00"));

        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 3);
        Order order = store.createOrder(clientId, orderItems);

        assertEquals(Order.OrderStatus.PENDING, order.getStatus(), "Заказ должен быть в ожидании");

        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, 5);
        store.deliverGoods(delivery);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        Order updatedOrder = getAllOrders(store).get(order.getOrderId());
        assertEquals(Order.OrderStatus.COMPLETED, updatedOrder.getStatus(), "Заказ должен быть выполнен");
        assertEquals(3, updatedOrder.getFulfilledItems().get(item1), "Должно быть выполнено 3 единицы");
        assertTrue(updatedOrder.getPendingItems().isEmpty(), "Не должно быть ожидающих товаров");
    }

    @Test
    @DisplayName("Тест 4: Отмена заказа")
    void testOrderCancellation() {
        UUID clientId = store.registerClient("Анна", "anna_user");
        Client client = getClient(store, clientId);
        PersonalListener listener = (PersonalListener) client.getClientListener();
        listener.setInitialBalance(new BigDecimal("1000.00"));

        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 2);
        Order order = store.createOrder(clientId, orderItems);

        assertEquals(Order.OrderStatus.PENDING, order.getStatus(), "Заказ должен быть в ожидании");

        boolean cancelled = store.cancelOrder(clientId, order.getOrderId());
        assertTrue(cancelled, "Заказ должен быть отменен");

        Order cancelledOrder = getAllOrders(store).get(order.getOrderId());
        assertEquals(Order.OrderStatus.CANCELLED, cancelledOrder.getStatus(), 
                "Статус заказа должен быть CANCELLED");
    }

    @Test
    @DisplayName("Тест 5: Заказ от несуществующего клиента")
    void testOrderFromNonExistentClient() {
        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 1);

        UUID fakeClientId = UUID.randomUUID();
        Order order = store.createOrder(fakeClientId, orderItems);

        assertNull(order, "Заказ от несуществующего клиента должен быть null");
    }

    @Test
    @DisplayName("Тест 6: Пустой заказ")
    void testEmptyOrder() {
        UUID clientId = store.registerClient("Тест", "test_user");
        Order order = store.createOrder(clientId, new HashMap<>());

        assertNull(order, "Пустой заказ должен быть null");
    }

    @Test
    @DisplayName("Тест 7: Заказ с нулевым количеством товаров")
    void testOrderWithZeroQuantity() {
        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 0);

        UUID clientId = store.registerClient("Тест", "test_user");
        assertDoesNotThrow(() -> store.createOrder(clientId, orderItems),
                "Система должна обработать запрос без ошибок");
    }

    @Test
    @DisplayName("Тест 8: Отмена уже выполненного заказа")
    void testCancelCompletedOrder() {
        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, 5);
        store.deliverGoods(delivery);

        UUID clientId = store.registerClient("Тест", "test_user");
        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 2);
        Order order = store.createOrder(clientId, orderItems);

        assertEquals(Order.OrderStatus.COMPLETED, order.getStatus(), "Заказ должен быть выполнен");

        boolean cancelled = store.cancelOrder(clientId, order.getOrderId());
        assertFalse(cancelled, "Выполненный заказ не должен быть отменен");
    }

    @Test
    @DisplayName("Тест 9: Заказ большего количества, чем есть в наличии")
    void testOrderMoreThanAvailable() {
        Map<Item, Integer> delivery = new HashMap<>();
        delivery.put(item1, 2);
        store.deliverGoods(delivery);

        UUID clientId = store.registerClient("Тест", "test_user");
        Client client = getClient(store, clientId);
        PersonalListener listener = (PersonalListener) client.getClientListener();
        listener.setInitialBalance(new BigDecimal("1000.00"));

        Map<Item, Integer> orderItems = new HashMap<>();
        orderItems.put(item1, 5);

        Order order = store.createOrder(clientId, orderItems);

        assertNotNull(order, "Заказ не должен быть null");
        assertEquals(Order.OrderStatus.PARTIAL, order.getStatus(), "Заказ должен быть частично выполнен");
        assertEquals(2, order.getFulfilledItems().get(item1), "Должно быть выполнено 2 единицы");
        assertEquals(3, order.getPendingItems().get(item1), "Должно быть в ожидании 3 единицы");
    }

    protected Item createItem(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        item.setItemId(UUID.randomUUID());
        return item;
    }
}

