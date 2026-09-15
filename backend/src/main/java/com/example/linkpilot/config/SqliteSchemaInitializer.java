package com.example.linkpilot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;

/**
 * Adds the indexes (including the unique ones) in performIndex.sql once Hibernate's
 * ddl-auto=update has created the tables. Hibernate's own generated schema is left
 * alone here rather than replaced: SQLite's Hibernate dialect binds UUID/OffsetDateTime
 * columns in its own internal formats (not the plain TEXT a hand-written schema.sql
 * would assume), so hand-authored DDL can't safely stand in for Hibernate's own
 * table/column generation - it can only safely *add* what ddl-auto can't express, which
 * is exactly what CREATE [UNIQUE] INDEX IF NOT EXISTS statements are for. schema.sql
 * itself stays as documentation of the intended shape, not something the app runs.
 */
@Component
public class SqliteSchemaInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SqliteSchemaInitializer.class);

    private final JdbcTemplate jdbcTemplate;
    private final ResourceLoader resourceLoader;

    public SqliteSchemaInitializer(DataSource dataSource, ResourceLoader resourceLoader) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Resource resource = resourceLoader.getResource("classpath:performIndex.sql");
        String content = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);

        for (String statement : content.split(";")) {
            String trimmed = statement.strip();
            boolean isRunnable = !trimmed.isEmpty()
                    && !trimmed.lines().allMatch(line -> line.isBlank() || line.strip().startsWith("--"));
            if (isRunnable) {
                jdbcTemplate.execute(trimmed);
            }
        }
        log.info("SQLite indexes are ready");
    }
}
