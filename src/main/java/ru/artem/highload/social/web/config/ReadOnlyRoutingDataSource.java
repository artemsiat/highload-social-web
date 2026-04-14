package ru.artem.highload.social.web.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReadOnlyRoutingDataSource extends AbstractRoutingDataSource {

    private static final String PRIMARY_KEY = "primary";

    private final List<String> replicaKeys;
    private final AtomicInteger counter = new AtomicInteger(0);

    public ReadOnlyRoutingDataSource(List<String> replicaKeys) {
        this.replicaKeys = replicaKeys;
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        if (readOnly && !replicaKeys.isEmpty()) {
            int idx = Math.abs(counter.getAndIncrement() % replicaKeys.size());
            String key = replicaKeys.get(idx);
            log.debug("Routing read-only query to: {}", key);
            return key;
        }
        log.debug("Routing query to: {}", PRIMARY_KEY);
        return PRIMARY_KEY;
    }
}
