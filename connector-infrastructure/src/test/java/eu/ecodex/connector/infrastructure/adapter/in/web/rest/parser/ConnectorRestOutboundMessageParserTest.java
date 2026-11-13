/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import eu.ecodex.connector.application.port.api.attachment.ConnectorUploadAttachments;
import eu.ecodex.connector.application.port.api.attachment.FileUploadCommand;
import eu.ecodex.connector.domain.model.businessdomain.ConnectorBusinessDomain;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.exception.ConnectorAttachmentUploadException;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.request.message.outbound.ConnectorOutboundMessageBusinessContent;
import eu.ecodex.connector.infrastructure.config.property.filestorage.ConnectorFileStorageProperties;
import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConnectorRestOutboundMessageParser")
class ConnectorRestOutboundMessageParserTest {
    @TempDir
    Path tempDir;

    @Mock
    ConnectorUploadAttachments uploadAttachmentsService;

    Path uploadDirectory;
    ConnectorRestOutboundMessageParser parser;

    private static MockMultipartFile multipart(String originalFilename, String content) {
        return new MockMultipartFile(
            "file", originalFilename, "application/pdf",
            content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private static String filenameOf(FileUploadCommand command) {
        return (String) ReflectionTestUtils.getField(command, "filename");
    }

    @BeforeEach
    void setUp() throws IOException {
        uploadDirectory = Files.createDirectory(tempDir.resolve("uploads"));
        if (uploadDirectory.getFileSystem().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(
                uploadDirectory, PosixFilePermissions.fromString("rwx------"));
        }

        var tempFilesHelper = new ConnectorTempFilesHelper(
            new ConnectorFileStorageProperties(uploadDirectory, null));
        parser = new ConnectorRestOutboundMessageParser(uploadAttachmentsService, tempFilesHelper);
    }

    private List<Path> remainingTempFiles() throws IOException {
        try (Stream<Path> files = Files.list(uploadDirectory)) {
            return files.toList();
        }
    }

    private Path normalizedUploadDirectory() {
        return uploadDirectory.toAbsolutePath().normalize();
    }

    private Captured stubUploadCapturing(ConnectorMessageAttachment result) {
        var captured = new Captured();
        when(uploadAttachmentsService.execute(any())).thenAnswer(invocation -> {
            List<FileUploadCommand> commands = invocation.getArgument(0);
            captured.commands.addAll(commands);
            captured.file = (Path) ReflectionTestUtils.getField(
                commands.getFirst(),
                "tempFileLocation"
            );
            captured.content = Files.readString(captured.file);
            if (captured.file.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                captured.permissions = Files.getPosixFilePermissions(captured.file);
            }
            return List.of(result);
        });
        return captured;
    }

    /**
     * Snapshot of what the upload service saw at call time (the temp file is deleted afterward).
     */
    private static final class Captured {
        final List<FileUploadCommand> commands = new ArrayList<>();
        Path file;
        String content;
        Set<PosixFilePermission> permissions;

        Object field(String name) {
            return ReflectionTestUtils.getField(commands.getFirst(), name);
        }
    }

    @Nested
    @DisplayName("toAttachment(MultipartFile, String)")
    class ToAttachmentFromMultipartFile {
        private final ConnectorMessageAttachment expected =
            ConnectorMessageAttachment.builder().identifier("att-1").build();

        @Test
        void should_return_the_attachment_produced_by_the_upload_service() throws IOException {
            stubUploadCapturing(expected);

            var result = parser.toAttachment(multipart("doc.pdf", "hello"), "Business document");

            assertThat(result).isSameAs(expected);
        }

        @Test
        void should_pass_file_metadata_to_the_upload_service() throws IOException {
            var captured = stubUploadCapturing(expected);
            var file = multipart("doc.pdf", "hello");

            parser.toAttachment(file, "Business document");

            assertThat(captured.commands).hasSize(1);
            assertThat(captured.field("filename")).isEqualTo("doc.pdf");
            assertThat(captured.field("contentType")).isEqualTo("application/pdf");
            assertThat(captured.field("size")).isEqualTo(file.getSize());
            assertThat(captured.field("description")).isEqualTo("Business document");
        }

        @Test
        void should_normalize_the_filename_passed_to_the_upload_service() throws IOException {
            var captured = stubUploadCapturing(expected);

            parser.toAttachment(multipart("folder/../doc.pdf", "x"), "desc");

            assertThat(captured.field("filename")).isEqualTo("doc.pdf");
        }

        @Test
        void should_write_the_uploaded_content_to_the_temp_file() throws IOException {
            var captured = stubUploadCapturing(expected);

            parser.toAttachment(multipart("doc.pdf", "hello world"), "desc");

            assertThat(captured.content).isEqualTo("hello world");
        }

        @Test
        void should_create_the_temp_file_inside_the_configured_directory() throws IOException {
            var captured = stubUploadCapturing(expected);

            parser.toAttachment(multipart("doc.pdf", "x"), "desc");

            assertThat(captured.file.getParent()).isEqualTo(normalizedUploadDirectory());
            assertThat(captured.file.getFileName().toString()).startsWith("upload-")
                                                              .endsWith(".pdf");
        }

        @Test
        @DisabledOnOs(OS.WINDOWS)
        void should_create_the_temp_file_with_owner_only_permissions() throws IOException {
            var captured = stubUploadCapturing(expected);

            parser.toAttachment(multipart("doc.pdf", "x"), "desc");

            assertThat(captured.permissions)
                .containsExactlyInAnyOrder(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
                );
        }

        @Test
        void should_never_use_path_segments_of_the_client_filename_for_the_temp_file()
            throws IOException {
            var captured = stubUploadCapturing(expected);

            parser.toAttachment(multipart("../../etc/evil.pdf", "x"), "desc");

            assertThat(captured.file.getParent()).isEqualTo(normalizedUploadDirectory());
            assertThat(captured.file.getFileName().toString()).doesNotContain("evil", "..", "/");
        }

        @Test
        void should_delete_the_temp_file_after_a_successful_upload() throws IOException {
            stubUploadCapturing(expected);

            parser.toAttachment(multipart("doc.pdf", "x"), "desc");

            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_wrap_upload_failures_and_still_delete_the_temp_file() throws IOException {
            when(uploadAttachmentsService.execute(any())).thenThrow(new IllegalStateException("boom"));
            var file = multipart("doc.pdf", "x");

            assertThatThrownBy(() -> parser.toAttachment(file, "desc"))
                .isInstanceOf(ConnectorAttachmentUploadException.class)
                .hasMessageContaining("Failed to upload attachment")
                .hasCauseInstanceOf(IllegalStateException.class);
            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_wrap_transfer_failures_and_still_delete_the_temp_file() throws IOException {
            var file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn("doc.pdf");
            when(file.getName()).thenReturn("file");
            doThrow(new IOException("disk full")).when(file).transferTo(any(Path.class));

            assertThatThrownBy(() -> parser.toAttachment(file, "desc"))
                .isInstanceOf(ConnectorAttachmentUploadException.class)
                .hasCauseInstanceOf(IOException.class);
            assertThat(remainingTempFiles()).isEmpty();
            verify(uploadAttachmentsService, never()).execute(any());
        }

        @Test
        void should_fail_and_delete_the_temp_file_when_the_content_type_is_missing()
            throws IOException {
            var file = new MockMultipartFile(
                "file", "doc.pdf", null, "x".getBytes(StandardCharsets.UTF_8));

            assertThatThrownBy(() -> parser.toAttachment(file, "desc"))
                .isInstanceOf(ConnectorAttachmentUploadException.class)
                .hasCauseInstanceOf(NullPointerException.class);
            assertThat(remainingTempFiles()).isEmpty();
            verify(uploadAttachmentsService, never()).execute(any());
        }

        @Test
        void should_fail_without_creating_a_temp_file_when_the_original_filename_is_missing()
            throws IOException {
            var file = mock(MultipartFile.class);
            when(file.getOriginalFilename()).thenReturn(null);

            assertThatThrownBy(() -> parser.toAttachment(file, "desc"))
                .isInstanceOf(NullPointerException.class);
            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_propagate_the_io_exception_when_the_temp_file_cannot_be_created()
            throws IOException {
            Files.delete(uploadDirectory);
            var file = multipart("doc.pdf", "x");

            assertThatThrownBy(() -> parser.toAttachment(file, "desc"))
                .isInstanceOf(IOException.class)
                .isNotInstanceOf(ConnectorAttachmentUploadException.class);
            verify(uploadAttachmentsService, never()).execute(any());
        }
    }

    @Nested
    @DisplayName("toBusinessContent")
    class ToBusinessContent {
        /**
         * Identifies each upload by the client filename, so the result can be traced back.
         */
        private void stubUploadsReturningIdentifierFromFilename() {
            when(uploadAttachmentsService.execute(any())).thenAnswer(invocation -> {
                List<FileUploadCommand> commands = invocation.getArgument(0);

                return List.of(ConnectorMessageAttachment
                                   .builder()
                                   .identifier("id-" + filenameOf(commands.getFirst()))
                                   .build());
            });
        }

        private ConnectorOutboundMessageBusinessContent businessContent(
            MultipartFile document, MultipartFile xml, MultipartFile signature) {
            var content = mock(ConnectorOutboundMessageBusinessContent.class, RETURNS_DEEP_STUBS);
            when(content.businessDocument().document()).thenReturn(document);
            when(content.xmlContent()).thenReturn(xml);
            when(content.businessDocument().detachedSignature().signature()).thenReturn(signature);

            return content;
        }

        private ConnectorOutboundMessageBusinessContent businessContentWithoutSignature(
            MultipartFile document, MultipartFile xml) {
            var content = mock(ConnectorOutboundMessageBusinessContent.class, RETURNS_DEEP_STUBS);
            when(content.businessDocument().document()).thenReturn(document);
            when(content.xmlContent()).thenReturn(xml);
            when(content.businessDocument().detachedSignature()).thenReturn(null);

            return content;
        }

        @Test
        void should_upload_the_document_the_detached_signature_and_the_xml_content()
            throws IOException {
            stubUploadsReturningIdentifierFromFilename();
            var content = businessContent(
                multipart("document.pdf", "doc"),
                multipart("content.xml", "<xml/>"),
                multipart("document.pdf.sig", "sig")
            );

            var result = parser.toBusinessContent(content);

            verify(uploadAttachmentsService, times(3)).execute(any());
            assertThat(result).extracting("xmlContent.identifier").isEqualTo("id-content.xml");
            assertThat(result).extracting("businessDocument.content.identifier")
                              .isEqualTo("id-document.pdf");
            assertThat(result).extracting("businessDocument.detachedSignature.identifier")
                              .isEqualTo("id-document.pdf.sig");
        }

        @Test
        void should_not_set_a_detached_signature_when_the_document_has_none() throws IOException {
            stubUploadsReturningIdentifierFromFilename();
            var content = businessContentWithoutSignature(
                multipart("document.pdf", "doc"),
                multipart("content.xml", "<xml/>")
            );

            var result = parser.toBusinessContent(content);

            verify(uploadAttachmentsService, times(2)).execute(any());
            assertThat(result).extracting("businessDocument.detachedSignature").isNull();
        }

        @Test
        void should_not_set_a_detached_signature_when_its_signature_file_is_null()
            throws IOException {
            stubUploadsReturningIdentifierFromFilename();
            var content = businessContent(
                multipart("document.pdf", "doc"),
                multipart("content.xml", "<xml/>"),
                null
            );

            var result = parser.toBusinessContent(content);

            verify(uploadAttachmentsService, times(2)).execute(any());
            assertThat(result).extracting("businessDocument.detachedSignature").isNull();
        }

        @Test
        void should_delete_all_temp_files_after_the_conversion() throws IOException {
            stubUploadsReturningIdentifierFromFilename();
            var content = businessContent(
                multipart("document.pdf", "doc"),
                multipart("content.xml", "<xml/>"),
                multipart("document.pdf.sig", "sig")
            );

            parser.toBusinessContent(content);

            assertThat(remainingTempFiles()).isEmpty();
        }

        @Test
        void should_fail_and_leave_no_temp_files_when_an_upload_fails() throws IOException {
            when(uploadAttachmentsService.execute(any())).thenThrow(new IllegalStateException("boom"));
            var content = businessContent(
                multipart("document.pdf", "doc"),
                multipart("content.xml", "<xml/>"),
                multipart("document.pdf.sig", "sig")
            );

            assertThatThrownBy(() -> parser.toBusinessContent(content))
                .isInstanceOf(ConnectorAttachmentUploadException.class);
            assertThat(remainingTempFiles()).isEmpty();
        }
    }

    @Nested
    @DisplayName("resolveBusinessDomainIdentifier")
    class ResolveBusinessDomainIdentifier {
        @Test
        void should_return_the_default_identifier_when_the_lane_identifier_is_null() {
            var result = parser.resolveBusinessDomainIdentifier(null);

            assertThat(result).isSameAs(ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID);
        }

        @Test
        void should_build_an_identifier_from_the_given_lane_identifier() {
            var result = parser.resolveBusinessDomainIdentifier("lane-1");

            assertThat(result)
                .isNotNull()
                .isNotSameAs(ConnectorBusinessDomain.DEFAULT_BUSINESS_DOMAIN_ID);
            assertThat(result).extracting("messageLaneIdentifier").isEqualTo("lane-1");
        }
    }

    @Nested
    @DisplayName("toAttachments(List<String>)")
    class ToAttachmentsFromIdentifiers {
        @Test
        void should_return_an_empty_list_when_identifiers_are_null() {
            assertThat(parser.toAttachments(null)).isEmpty();
        }

        @Test
        void should_return_a_mutable_list_when_identifiers_are_null() {
            var result = parser.toAttachments(null);

            assertThatCode(() -> result.add(ConnectorMessageAttachment.builder().build()))
                .doesNotThrowAnyException();
        }

        @Test
        void should_return_an_empty_list_when_identifiers_are_empty() {
            assertThat(parser.toAttachments(List.of())).isEmpty();
        }

        @Test
        void should_map_each_identifier_to_an_attachment_in_order() {
            var result = parser.toAttachments(List.of("a", "b", "c"));

            assertThat(result)
                .extracting("identifier")
                .containsExactly("a", "b", "c");
        }
    }
}
