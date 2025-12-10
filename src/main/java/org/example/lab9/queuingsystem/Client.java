package org.example.lab9.queuingsystem;

import lombok.Data;
import org.example.lab9.PersonalListener;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.UUID;

@Data
public class Client implements PropertyChangeListener {
    private UUID userId;
    private String name;
    private String userName;
    private UserBucket userBucket;
    private StoreListener clientListener;

    public Client(String name, String userName){
        this.userId = UUID.randomUUID();
        this.userBucket = new UserBucket();
        this.name = name;
        this.userName = userName;
        this.clientListener = new PersonalListener();
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {

    }
}
