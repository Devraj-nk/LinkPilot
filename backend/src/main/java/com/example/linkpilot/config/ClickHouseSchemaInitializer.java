package com.example.linkpilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Runs clickHouse.sql + aggregators.sql against ClickHouse on startup (both are written
 * with IF NOT EXISTS, so this is safe to re-run every boot). ClickHouse is optional
 * infrastructure for the analytics feature, not a hard dependency for the core product,
 * so a connection failure here is logged and swallowed rather than failing app startup.
 */
@Component
public class ClickHouseSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ClickHouseSchemaInitializer.class);

    private final JdbcTemplate clickHouseJdbcTemplate;
    private final ResourceLoader resourceLoader;

    public ClickHouseSchemaInitializer(JdbcTemplate clickHouseJdbcTemplate, ResourceLoader resourceLoader) {
        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            runScript("classpath:clickHouse.sql");
            runScript("classpath:aggregators.sql");
            log.info("ClickHouse analytics schema is ready");
        } catch (Exception e) {
            log.warn("Could not initialize ClickHouse schema - analytics will be unavailable until it's reachable ({})", e.getMessage());
        }
    }

    private void runScript(String location) throws Exception {
        Resource resource = resourceLoader.getResource(location);
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        for (String statement : content.split(";")) {
            String trimmed = statement.strip();
            boolean isRunnable = !trimmed.isEmpty()
                    && !trimmed.lines().allMatch(line -> line.isBlank() || line.strip().startsWith("--"));
            if (isRunnable) {
                clickHouseJdbcTemplate.execute(trimmed);
            }
        }
    }
}
