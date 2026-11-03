package ar.edu.itba.dps.fieldops.adapters.driven.persistence;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.Map;

public class LoadedAggregates {
    private final Object key = new Object();

    @SuppressWarnings("unchecked")
    <T> T get(String id) {
        return (T) current().get(id);
    }

    void put(String id, Object aggregate) {
        current().put(id, aggregate);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> current() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return new HashMap<>();
        }
        final var loaded = (Map<String, Object>) TransactionSynchronizationManager.getResource(key);
        return loaded != null ? loaded : bindNewMap();
    }

    private Map<String, Object> bindNewMap() {
        final var aggregates = new HashMap<String, Object>();
        TransactionSynchronizationManager.bindResource(key, aggregates);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                TransactionSynchronizationManager.unbindResource(key);
            }
        });
        return aggregates;
    }
}
