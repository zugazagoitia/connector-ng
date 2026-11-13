/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.persistence.adapter;

import eu.ecodex.connector.RepositoryContextConfiguration;
import eu.ecodex.connector.WithUploadTempDirectory;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles(value = {"dss", "security", "evidence", "local"})
@WithUploadTempDirectory
@SpringBootTest(
    classes = RepositoryContextConfiguration.class,
    properties = {
        """
                spring.autoconfigure.exclude=org.apache.cxf.spring.boot.autoconfigure.micrometer.MicrometerMetricsAutoConfiguration
            """
    }
)
public abstract class AbstractRepositoryTest {
    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected EntityManager entityManager;
}
