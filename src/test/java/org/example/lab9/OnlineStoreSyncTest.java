package org.example.lab9;

import org.junit.jupiter.api.DisplayName;

@DisplayName("Тестирование синхронной реализации СМО")
class OnlineStoreSyncTest extends OnlineStoreBaseTest {

    @Override
    protected org.example.lab9.queuingsystem.OnlineStoreAPI createStore() {
        return new OnlineStoreImpl();
    }

    @Override
    protected void shutdownStore(org.example.lab9.queuingsystem.OnlineStoreAPI store) {
        ((OnlineStoreImpl) store).shutdown();
    }
}

