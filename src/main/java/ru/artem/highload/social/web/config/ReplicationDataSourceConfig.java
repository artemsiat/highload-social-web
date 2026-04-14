package ru.artem.highload.social.web.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;

@Configuration
@Profile("primary-replica-db")
public class ReplicationDataSourceConfig {

    @Bean
    @ConfigurationProperties("app.datasource.primary")
    public HikariDataSource primaryDataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica1")
    public HikariDataSource replica1DataSource() {
        return new HikariDataSource();
    }

    @Bean
    @ConfigurationProperties("app.datasource.replica2")
    public HikariDataSource replica2DataSource() {
        return new HikariDataSource();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            @Qualifier("primaryDataSource") DataSource primary,
            @Qualifier("replica1DataSource") DataSource replica1,
            @Qualifier("replica2DataSource") DataSource replica2) {

        var routing = new ReadOnlyRoutingDataSource(List.of("replica1", "replica2"));
        routing.setTargetDataSources(Map.of(
                "primary", primary,
                "replica1", replica1,
                "replica2", replica2
        ));
        routing.setDefaultTargetDataSource(primary);
        routing.afterPropertiesSet();

        return new LazyConnectionDataSourceProxy(routing);
    }
}
