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

import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentStorage;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorAttachmentType;
import eu.ecodex.connector.domain.model.message.attachment.ConnectorMessageAttachment;
import java.time.Instant;

@SuppressWarnings({"MissingJavadocType", "MissingJavadocMethod"})
public class MessageAttachmentTestFixtures {
    public static ConnectorMessageAttachment createAttachment() {
        var name = "test_attachment";
        var identifier = String.format("%s_%s", "c12f879b-3c9a-4d26-b36c-b6d67a84f0ed", name);
        return ConnectorMessageAttachment
                .builder()
                .identifier(identifier)
                .name(name + ".txt")
                .size(100L)
                .description("test attachment description")
                .contentType("text/plain")
                .storage(ConnectorAttachmentStorage.S3_BUCKET)
                .type(ConnectorAttachmentType.ATTACHMENT)
                .build();
    }

    public static ConnectorMessageAttachment createdAttachment() {
        return createAttachment().toBuilder()
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    public static ConnectorMessageAttachment createBusinessContentAttachment() {
        return createAttachment()
                .toBuilder()
                .identifier("104ebc70-abd5-45da-8c74-940d687501b3_messageContent")
                .type(ConnectorAttachmentType.BUSINESS_CONTENT)
                .contentType("application/xml")
                .name("businessContent.xml")
                .build();
    }

    public static ConnectorMessageAttachment createBusinessDocumentAttachment() {
        return createAttachment()
            .toBuilder()
            .identifier("f79623a9-3792-4c6e-a96b-819bd4b69879_messageContent")
            .type(ConnectorAttachmentType.BUSINESS_DOCUMENT)
            .contentType("application/pdf")
            .name("Form_A.pdf")
            .build();
    }

    public static ConnectorMessageAttachment createPDFTrustOKTokenAttachment() {
        return createAttachment()
            .toBuilder()
            .identifier("5ba2556d-a77f-4b30-a6b4-6e7920e6109d_pdf_trust_ok_token")
            .type(ConnectorAttachmentType.PDF_TOKEN)
            .contentType("application/pdf")
            .name("PDFTrustOKToken.pdf")
            .build();
    }

    public static ConnectorMessageAttachment createXMLTrustOKTokenAttachment() {
        return createAttachment()
            .toBuilder()
            .identifier("16e8bfe9-3763-4cb3-9587-10c813ec0938_xml_trust_ok_token")
            .type(ConnectorAttachmentType.XML_TOKEN)
            .contentType("application/xml")
            .name("XMLTrustOKToken.xml")
            .build();
    }

    public static ConnectorMessageAttachment createPKCS7DetachedSignatureAttachment() {
        return createAttachment()
            .toBuilder()
            .identifier("1e74780d-9a92-4df2-a368-119d2f70eb07_xml_detached_signature")
            .description("Detached signature")
            .type(ConnectorAttachmentType.DETACHED_SIGNATURE)
            .contentType("application/pkcs7-signature")
            .name("detachedSignature.p7b")
            .build();
    }
}
