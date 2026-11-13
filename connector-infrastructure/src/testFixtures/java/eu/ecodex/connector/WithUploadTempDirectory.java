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

import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.test.context.ContextConfiguration;


/**
 * Test annotation that gives a Spring test context a temporary upload directory.
 *
 * <p>Use it on any test class whose context loads {@link ConnectorFileStorageProperties}, since
 * that property is required and must point to an existing, writable directory.
 *
 * @see UploadTempDirectoryInitializer
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@ContextConfiguration(initializers = UploadTempDirectoryInitializer.class)
public @interface WithUploadTempDirectory {
}
