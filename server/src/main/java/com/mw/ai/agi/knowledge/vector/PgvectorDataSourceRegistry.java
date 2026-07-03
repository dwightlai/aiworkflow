package com.mw.ai.agi.knowledge.vector;

import com.mw.ai.agi.knowledge.domain.VectorStoreConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PgvectorDataSourceRegistry {
    private final Map<String, Entry> sources = new ConcurrentHashMap<>();

    public DataSource get(VectorStoreConfig config) {
        String signature = PgvectorVectorStoreProvider.jdbcUrl(config)
                + "|" + config.username() + "|" + config.password();
        Entry current = sources.get(config.id());
        if (current != null && current.signature().equals(signature)) {
            return current.dataSource();
        }
        close(config.id());
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(PgvectorVectorStoreProvider.jdbcUrl(config));
        hikari.setUsername(config.username());
        hikari.setPassword(config.password());
        hikari.setConnectionTimeout(config.connectTimeoutMs());
        hikari.setValidationTimeout(Math.min(config.connectTimeoutMs(), 5000));
        hikari.setMaximumPoolSize(5);
        hikari.setMinimumIdle(0);
        hikari.setPoolName("pgvector-" + config.id());
        HikariDataSource dataSource = new HikariDataSource(hikari);
        sources.put(config.id(), new Entry(signature, dataSource));
        return dataSource;
    }

    public void close(String configId) {
        Entry entry = sources.remove(configId);
        if (entry != null) {
            entry.dataSource().close();
        }
    }

    private record Entry(String signature, HikariDataSource dataSource) {
    }
}
