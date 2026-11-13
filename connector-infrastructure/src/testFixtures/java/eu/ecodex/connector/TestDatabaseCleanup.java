/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector;

import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Truncates connector tables used by SQL-based integration and repository tests.
 * Synchronized because integration tests share a single Testcontainers MySQL instance.
 */
public final class TestDatabaseCleanup {
    private static final Object LOCK = new Object();

    private TestDatabaseCleanup() {
    }

    /**
     * Truncates all connector tables used by SQL-based integration and repository tests.
     *
     * @param jdbcTemplate JDBC template bound to the test database
     */
    public static void truncateAll(JdbcTemplate jdbcTemplate) {
        synchronized (LOCK) {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_transport_step_statuses");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_transport_steps");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_evidences");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_errors");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_business_documents");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_business_contents");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_as4_properties");
            jdbcTemplate.execute("TRUNCATE TABLE connector_messages");
            jdbcTemplate.execute("TRUNCATE TABLE connector_message_attachments");
            jdbcTemplate.execute("TRUNCATE TABLE connector_parties");
            jdbcTemplate.execute("TRUNCATE TABLE connector_services");
            jdbcTemplate.execute("TRUNCATE TABLE connector_actions");
            jdbcTemplate.execute("TRUNCATE TABLE connector_processing_modes");
            jdbcTemplate.execute("TRUNCATE TABLE connector_processing_mode_truststores");
            jdbcTemplate.execute("TRUNCATE TABLE connector_business_domains");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
        }
    }
}
