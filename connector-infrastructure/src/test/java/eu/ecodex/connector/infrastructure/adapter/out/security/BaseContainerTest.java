/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.security;

import eu.ecodex.connector.ContainerContextConfiguration;
import eu.ecodex.connector.WithUploadTempDirectory;
import eu.ecodex.connector.infrastructure.config.PropertiesConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@WithUploadTempDirectory
@ActiveProfiles(value = {"dss", "security", "evidence", "local"})
@SpringBootTest(classes = {ContainerContextConfiguration.class, PropertiesConfig.class})
public class BaseContainerTest {
}
