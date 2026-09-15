package com.example.linkpilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/**
 * A second, independent datasource for ClickHouse (click analytics), deliberately kept
 * separate from the primary SQLite/JPA DataSource - only a JdbcTemplate is exposed as a
 * bean, so there's never a second javax.sql.DataSource bean for Spring's JPA
 * autoconfiguration to get confused about.
 */
@Configuration
public class ClickHouseConfig {

    @Bean
    public JdbcTemplate clickHouseJdbcTemplate(
            @Value("${clickhouse.url}") String url,
            @Value("${clickhouse.username}") String username,
            @Value("${clickhouse.password}") String password
    ) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("com.clickhouse.jdbc.ClickHouseDriver");
        dataSource.setUrl(url);
        dataSource.setUsername(username);
        dataSource.setPassword(password);
        return new JdbcTemplate(dataSource);
    }
}
