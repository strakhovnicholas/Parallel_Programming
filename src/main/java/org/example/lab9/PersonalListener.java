package org.example.lab9;

import lombok.Getter;
import org.example.lab9.queuingsystem.StoreListener;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class PersonalListener implements StoreListener {
    private final List<OperationRecord> operations = new ArrayList<>();
    private final Lock operationsLock = new ReentrantLock();
    
    @Getter
    private BigDecimal initialBalance = BigDecimal.ZERO;
    
    private BigDecimal currentBalance = BigDecimal.ZERO;

    public void setInitialBalance(BigDecimal balance) {
        this.initialBalance = balance;
        this.currentBalance = balance;
    }

    @Override
    public void onOrderCompleted(UUID orderId, Map<Item, Integer> purchasedItems,
                                 BigDecimal totalCost, Map<Item, Integer> pendingItems) {
        operationsLock.lock();
        try {
            if (totalCost.compareTo(BigDecimal.ZERO) > 0) {
                currentBalance = currentBalance.subtract(totalCost);
                operations.add(new OperationRecord(OperationType.PURCHASE, orderId, totalCost, purchasedItems));
            }
            
            BigDecimal blockedAmount = BigDecimal.ZERO;
            for (Map.Entry<Item, Integer> entry : pendingItems.entrySet()) {
                blockedAmount = blockedAmount.add(
                    entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue()))
                );
            }
            if (blockedAmount.compareTo(BigDecimal.ZERO) > 0) {
                currentBalance = currentBalance.subtract(blockedAmount);
                operations.add(new OperationRecord(OperationType.BLOCK, orderId, blockedAmount, pendingItems));
            }
        } finally {
            operationsLock.unlock();
        }
    }

    @Override
    public void onOrderCancelled(UUID orderId, String reason) {
        operationsLock.lock();
        try {
            operations.add(new OperationRecord(OperationType.UNBLOCK, orderId, BigDecimal.ZERO, null));
        } finally {
            operationsLock.unlock();
        }
    }

    @Override
    public void onOrderFulfilled(UUID orderId, Map<Item, Integer> fulfilledItems, BigDecimal additionalCost) {
        operationsLock.lock();
        try {
            if (additionalCost.compareTo(BigDecimal.ZERO) > 0) {
                currentBalance = currentBalance.subtract(additionalCost);
                operations.add(new OperationRecord(OperationType.PURCHASE, orderId, additionalCost, fulfilledItems));
            }
        } finally {
            operationsLock.unlock();
        }
    }

    public BigDecimal getCurrentBalance() {
        operationsLock.lock();
        try {
            return currentBalance;
        } finally {
            operationsLock.unlock();
        }
    }

    public List<OperationRecord> getOperations() {
        operationsLock.lock();
        try {
            return new ArrayList<>(operations);
        } finally {
            operationsLock.unlock();
        }
    }

    public record OperationRecord(
            OperationType type,
            UUID orderId,
            BigDecimal amount,
            Map<Item, Integer> items) { }

    public enum OperationType {
        PURCHASE,
        BLOCK,
        UNBLOCK
    }
}
