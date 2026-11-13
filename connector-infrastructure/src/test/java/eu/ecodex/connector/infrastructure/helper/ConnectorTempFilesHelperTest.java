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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ConnectorTempFilesHelper")
public class ConnectorTempFilesHelperTest {
    @TempDir
    Path tempDir;

    Path uploadDirectory;

    private static boolean isPosix(Path path) {
        return path.getFileSystem().supportedFileAttributeViews().contains("posix");
    }

    private static void setPermissions(Path path, String permissions) throws IOException {
        if (isPosix(path)) {
            Files.setPosixFilePermissions(path, PosixFilePermissions.fromString(permissions));
        }
    }

    private static ConnectorTempFilesHelper newHelper(Path directory) {
        return new ConnectorTempFilesHelper(new ConnectorFileStorageProperties(directory, null));
    }

    private ConnectorTempFilesHelper newHelper() {
        return newHelper(uploadDirectory);
    }

    private Path normalizedUploadDirectory() {
        return uploadDirectory.toAbsolutePath().normalize();
    }

    @BeforeEach
    void setUp() throws IOException {
        uploadDirectory = Files.createDirectory(tempDir.resolve("uploads"));
        setPermissions(uploadDirectory, "rwx------");
    }

    @Nested
    @DisplayName("directory verification")
    class DirectoryVerification {
        @Test
        void should_accept_an_owner_only_directory() {
            assertThatCode(ConnectorTempFilesHelperTest.this::newHelper).doesNotThrowAnyException();
        }

        @Test
        void should_not_create_or_modify_anything_in_the_directory() throws IOException {
            newHelper();

            try (Stream<Path> files = Files.list(uploadDirectory)) {
                assertThat(files).isEmpty();
            }
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_not_change_the_permissions_of_the_directory() throws IOException {
            setPermissions(uploadDirectory, "rwxr-x---");

            newHelper();

            assertThat(Files.getPosixFilePermissions(uploadDirectory))
                .isEqualTo(PosixFilePermissions.fromString("rwxr-x---"));
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_accept_a_group_readable_but_not_group_writable_directory() throws IOException {
            setPermissions(uploadDirectory, "rwxr-x---");

            assertThatCode(ConnectorTempFilesHelperTest.this::newHelper).doesNotThrowAnyException();
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_reject_a_symlinked_directory() throws IOException {
            var link = Files.createSymbolicLink(tempDir.resolve("link"), uploadDirectory);

            assertThatThrownBy(() -> newHelper(link))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be a symlink");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
            "rwxrwxrwx",  // world-writable
            "rwx---rwx",  // writable only by others
            "rwxrwx---",  // writable only by group
            "rwx-w----",  // group write bit alone
            "rwx----w-"   // others write bit alone
        })
        @DisabledOnOs(OS.WINDOWS)
        void should_reject_a_directory_writable_by_group_or_others(String permissions)
            throws IOException {
            setPermissions(uploadDirectory, permissions);

            assertThatThrownBy(ConnectorTempFilesHelperTest.this::newHelper)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must not be writable by group or others");
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_fail_when_the_directory_does_not_exist() {
            var missing = tempDir.resolve("missing");

            assertThatThrownBy(() -> newHelper(missing))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("Cannot read permissions of")
                .hasCauseInstanceOf(NoSuchFileException.class);
        }
    }

    @Nested
    @DisplayName("create")
    class Create {
        private static Path createQuietly(ConnectorTempFilesHelper helper) {
            try {
                return helper.create("upload-", "same-name.pdf");
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Test
        void should_create_an_empty_regular_file() throws IOException {
            var file = newHelper().create("upload-", "doc.pdf");

            assertThat(file).isRegularFile();
            assertThat(Files.size(file)).isZero();
        }

        @Test
        void should_create_the_file_inside_the_configured_directory() throws IOException {
            var file = newHelper().create("upload-", "doc.pdf");

            assertThat(file.getParent()).isEqualTo(normalizedUploadDirectory());
        }

        @Test
        void should_use_the_given_prefix() throws IOException {
            var file = newHelper().create("ws-payload-", "doc.xml");

            assertThat(file.getFileName().toString()).startsWith("ws-payload-");
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_create_the_file_with_owner_only_permissions() throws IOException {
            var file = newHelper().create("upload-", "doc.pdf");

            assertThat(Files.getPosixFilePermissions(file))
                .containsExactlyInAnyOrder(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                );
        }

        @Test
        void should_generate_a_different_file_for_each_call() {
            var helper = newHelper();

            var names = IntStream.range(0, 50)
                                 .mapToObj(i -> createQuietly(helper))
                                 .map(path -> path.getFileName().toString())
                                 .collect(Collectors.toSet());

            assertThat(names).hasSize(50);
        }

        @Test
        void should_fail_when_the_directory_was_removed_after_startup() throws IOException {
            var helper = newHelper();
            Files.delete(uploadDirectory);

            assertThatThrownBy(() -> helper.create("upload-", "doc.pdf"))
                .isInstanceOf(IOException.class);
        }
    }

    @Nested
    @DisplayName("suffix handling")
    class SuffixHandling {
        @ParameterizedTest
        @CsvSource(
            nullValues = "NULL",
            value = {
                // safe extensions are kept
                "doc.pdf,                     .pdf",
                "doc.PDF,                     .PDF",
                "archive.tar.gz,              .gz",
                ".pdf,                        .pdf",
                "a.b1c2,                      .b1c2",
                "a.abcdefghij,                .abcdefghij",
                // missing or unsafe extensions fall back to .tmp
                "README,                      .tmp",
                "name.,                       .tmp",
                "file.p$f,                    .tmp",
                "file.p df,                   .tmp",
                "file.p%00df,                 .tmp",
                "archive.waytoolongextension, .tmp",
                "a.abcdefghijk,               .tmp",
                "'',                          .tmp",
                "NULL,                        .tmp"
            })
        void should_derive_a_safe_suffix_from_the_given_filename(
            String input,
            String expectedSuffix)
            throws IOException {
            var file = newHelper().create("upload-", input);

            assertThat(file.getFileName().toString()).endsWith(expectedSuffix);
        }

        @Test
        void should_fall_back_to_tmp_when_the_filename_is_null() throws IOException {
            var file = newHelper().create("upload-", null);

            assertThat(file.getFileName().toString()).endsWith(".tmp");
        }

        @ParameterizedTest
        @CsvSource({
            "../../etc/passwd",
            "../../../evil.pdf",
            "a/b/c.pdf",
            "..\\..\\evil.pdf",
            "file.p/df"
        })
        void should_never_let_path_segments_of_the_input_reach_the_filesystem(String input)
            throws IOException {
            var file = newHelper().create("upload-", input);

            assertThat(file.getParent()).isEqualTo(normalizedUploadDirectory());
            assertThat(file.getFileName().toString())
                .startsWith("upload-")
                .doesNotContain("..", "/", "\\", "etc", "passwd", "evil");
        }
    }
}
