package org.example.lab9;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Тестирование реализации СМО с Disruptor")
class OnlineStoreDisruptorTest extends OnlineStoreBaseTest {

    @Override
    protected org.example.lab9.queuingsystem.OnlineStoreAPI createStore() {
        return new OnlineStoreDisruptorImpl();
    }

    @Override
    protected void shutdownStore(org.example.lab9.queuingsystem.OnlineStoreAPI store) {
        ((OnlineStoreDisruptorImpl) store).shutdown();
    }
}

