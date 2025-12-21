package org.example.lab9;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Тестирование реализации СМО с очередями")
class OnlineStoreQueueTest extends OnlineStoreBaseTest {

    @Override
    protected org.example.lab9.queuingsystem.OnlineStoreAPI createStore() {
        return new OnlineStoreQueueImpl();
    }

    @Override
    protected void shutdownStore(org.example.lab9.queuingsystem.OnlineStoreAPI store) {
        ((OnlineStoreQueueImpl) store).shutdown();
    }
}

