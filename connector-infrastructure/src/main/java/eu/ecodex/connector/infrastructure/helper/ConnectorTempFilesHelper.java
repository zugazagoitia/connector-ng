/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.helper;

import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Creates temporary files inside the directory configured by
 * {@code connector.file.storage.upload-temp-directory}.
 *
 * <p>The directory is expected to exist already; it is only verified here (not a symlink,
 * not writable by group or others), never created or modified. Files are created with owner-only
 * permissions and random names. Callers may pass a client-supplied filename, but only a sanitized
 * extension of it ever reaches the filesystem.
 */
@Component
public class ConnectorTempFilesHelper {
    private static final Pattern SAFE_EXTENSION = Pattern.compile("[A-Za-z0-9]{1,10}");
    private static final boolean POSIX =
        FileSystems.getDefault().supportedFileAttributeViews().contains("posix");
    private static final FileAttribute<Set<PosixFilePermission>> FILE_PERMISSIONS =
        PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------"));

    private final Path directory;

    public ConnectorTempFilesHelper(ConnectorFileStorageProperties storageProperties) {
        this.directory = verifyDirectory(storageProperties.uploadTempDirectory());
    }

    private static String safeSuffix(String filenameOrExtension) {
        var extension = StringUtils.getFilenameExtension(filenameOrExtension);

        return extension != null && SAFE_EXTENSION.matcher(extension).matches()
            ? "." + extension
            : ".tmp";
    }

    private static Path verifyDirectory(Path configured) {
        var dir = configured.toAbsolutePath().normalize();

        if (Files.isSymbolicLink(dir)) {
            throw new IllegalStateException(
                "Upload directory must not be a symlink: [%s]".formatted(dir)
            );
        }

        if (POSIX) {
            try {
                var permissions = Files.getPosixFilePermissions(dir);
                if (permissions.contains(PosixFilePermission.OTHERS_WRITE)
                    || permissions.contains(PosixFilePermission.GROUP_WRITE)) {
                    throw new IllegalStateException(
                        "Upload directory must not be writable by group or others: [%s]"
                            .formatted(dir)
                    );
                }
            } catch (IOException e) {
                throw new UncheckedIOException(
                    "Cannot read permissions of [%s]".formatted(dir), e
                );
            }
        }

        return dir;
    }

    /**
     * Creates an empty, owner-only temporary file.
     *
     * @param prefix              a fixed prefix chosen by the caller (never user input)
     * @param filenameOrExtension a filename or extension (e.g. {@code "a.pdf"} or {@code ".pdf"})
     *                            used only to derive a safe suffix; may be {@code null}
     *
     * @return the path of the created file
     *
     * @throws IOException if the file cannot be created
     */
    public Path create(String prefix, String filenameOrExtension) throws IOException {
        var suffix = safeSuffix(filenameOrExtension);

        return POSIX
            ? Files.createTempFile(directory, prefix, suffix, FILE_PERMISSIONS)
            : Files.createTempFile(directory, prefix, suffix);
    }
}
