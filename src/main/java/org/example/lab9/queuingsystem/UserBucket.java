package org.example.lab9.queuingsystem;

import org.example.lab9.Item;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserBucket {
    private UUID ownerId;
    private Map<Item, Integer> userDeferredProducts = new ConcurrentHashMap<>();

    public void addProduct(Item product){
        if (this.userDeferredProducts.containsKey(product)){
            int count = this.userDeferredProducts.get(product);
            this.userDeferredProducts.put(product, ++count);
        }
    }

    public void deleteProduct(Item product){
        this.userDeferredProducts.remove(product);
    }
}
