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
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.util.FileSystemUtils;

/**
 * Test initializer that provides a temporary upload directory to the Spring test context.
 *
 * <p>The property {@code connector.file.storage.upload-temp-directory} is required, and
 * {@link ConnectorFileStorageProperties} checks at startup that it is an existing, writable
 * directory. This initializer creates a fresh directory named {@code connector-uploads-test-*}
 * under the system temporary directory, sets the property to it before the context is refreshed,
 * and deletes the directory recursively when the context is closed. Each context therefore gets its
 * own directory, and nothing is left on disk after the tests.
 */

public class UploadTempDirectoryInitializer
    implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    /**
     * Creates the temporary upload directory, registers it as the value of
     * {@code connector.file.storage.upload-temp-directory} in the given context, and schedules its
     * deletion when the context is closed.
     *
     * <p>The deletion only happens for the context that created the directory, not for a parent
     * or child context that publishes its own close event.
     *
     * @param context the application context being initialized
     *
     * @throws UncheckedIOException if the temporary directory cannot be created
     */

    @Override
    public void initialize(@NonNull ConfigurableApplicationContext context) {
        try {
            var dir = Files.createTempDirectory("connector-uploads-test-");
            TestPropertyValues
                .of("connector.file.storage.upload-temp-directory=" + dir)
                .applyTo(context);

            context.addApplicationListener((ApplicationListener<ContextClosedEvent>) event -> {
                if (event.getApplicationContext() == context) {
                    FileSystemUtils.deleteRecursively(dir.toFile());
                }
            });
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
