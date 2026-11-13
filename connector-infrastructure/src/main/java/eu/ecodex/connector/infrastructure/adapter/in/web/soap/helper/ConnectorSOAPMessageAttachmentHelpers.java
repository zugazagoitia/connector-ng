/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.soap.helper;

import eu.ecodex.connector.infrastructure.helper.ConnectorTempFilesHelper;
import jakarta.activation.DataHandler;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import javax.xml.XMLConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import org.apache.tika.mime.MimeTypeException;
import org.apache.tika.mime.MimeTypes;
import org.springframework.stereotype.Component;

/**
 * Helper for working with the attachments of SOAP messages exchanged with backend systems.
 *
 * <p>It converts between the attachments carried by a SOAP message (for example the business
 * document, its detached signature and the supporting attachments) and the connector's attachment
 * types, using temporary files (see {@link ConnectorTempFilesHelper}) so that large attachments are
 * not held in memory. XML content is processed with a shared {@link TransformerFactory}.
 */
@Component
public class ConnectorSOAPMessageAttachmentHelpers {
    private static final TransformerFactory transformerFactory = getTransformerFactory();
    private final ConnectorTempFilesHelper tempFilesHelper;

    public ConnectorSOAPMessageAttachmentHelpers(ConnectorTempFilesHelper tempFilesHelper) {
        this.tempFilesHelper = tempFilesHelper;
    }

    private static void discard(Path tempFile, Exception cause) {
        try {
            Files.deleteIfExists(tempFile);
        } catch (IOException suppressed) {
            cause.addSuppressed(suppressed);
        }
    }

    private static byte[] transform(Source source) {
        try {
            var transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");

            var out = new ByteArrayOutputStream();
            transformer.transform(source, new StreamResult(out));

            return out.toByteArray();
        } catch (IllegalArgumentException | TransformerException e) {
            throw new RuntimeException("Exception occurred during transforming xml into byte[]", e);
        }
    }

    private static TransformerFactory getTransformerFactory() {
        TransformerFactory transformerFactory;

        try {
            transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Error setting TransformerFactory attribute", e);
        }

        return transformerFactory;
    }

    private static String contentTypeToExtension(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return ".tmp";
        }

        var baseType = contentType.split(";")[0].trim().toLowerCase();
        var mimeTypes = MimeTypes.getDefaultMimeTypes();

        try {
            var extension = mimeTypes.forName(baseType).getExtension();

            return extension == null ? ".tmp" : extension;
        } catch (MimeTypeException e) {
            return ".tmp";
        }
    }

    /**
     * Best-effort extraction of a file name from the source's system id.
     */
    public static String getSourceFileName(Source source) {
        if (source == null) {
            return null;
        }

        if (source.getSystemId() != null) {
            return fileNameOf(source.getSystemId());
        }

        // SAXSource: delegate to the InputSource
        if (source instanceof SAXSource saxSource) {
            var inputSource = saxSource.getInputSource();
            if (inputSource != null && inputSource.getSystemId() != null) {
                return fileNameOf(inputSource.getSystemId());
            }
        }

        // StreamSource, DOMSource, StAXSource: no file name available beyond the
        // system id (already checked)
        return null;
    }

    private static String fileNameOf(String systemId) {
        try {
            var fileName = Paths.get(new URI(systemId)).getFileName();

            return fileName == null ? systemId : fileName.toString();
        } catch (URISyntaxException | RuntimeException e) {
            // Not a file URI (e.g. http:), return as-is
            return systemId;
        }
    }

    /**
     * Serializes the given XML source to a temporary file and returns its path.
     *
     * <p>The source is first transformed to XML in memory, and the file is only written once this
     * succeeds, so nothing is left on disk if the XML cannot be produced. The file name starts with
     * {@code ws-payload-} and is derived from the source (see {@code getSourceFileName}).
     *
     * <p>The caller owns the returned file and is responsible for deleting it once it is no longer
     * needed.
     *
     * @param source the XML source to serialize, for example the payload of a SOAP message (must
     *               not be null)
     *
     * @return the path of the temporary file containing the serialized XML
     *
     * @throws IOException if the temporary file cannot be created or written
     */
    public Path sourceToTempFile(Source source) throws IOException {
        // Transform first: nothing is written to disk if the XML cannot be produced
        var xml = transform(source);

        return writeToTempFile("ws-payload-", getSourceFileName(source), xml);
    }

    /**
     * Copies the content of the given attachment to a temporary file and returns its path.
     *
     * <p>The content is streamed from the {@link DataHandler} to the file, so the attachment is
     * not
     * held in memory. The file name starts with {@code ws-attachment-} and its extension is derived
     * from the content type of the attachment (see {@code contentTypeToExtension}). If copying the
     * content fails, the partially written file is discarded (see {@code discard}).
     *
     * <p>The caller owns the returned file and is responsible for deleting it once it is no longer
     * needed.
     *
     * @param dh the data handler giving access to the content of the attachment, for example a SOAP
     *           attachment (must not be null)
     *
     * @return the path of the temporary file containing the content of the attachment
     *
     * @throws IOException if the temporary file cannot be created, or if the content cannot be read
     *                     or copied
     */
    public Path dataHandlerToTempFile(DataHandler dh) throws IOException {
        var tempFile = tempFilesHelper.create(
            "ws-attachment-",
            contentTypeToExtension(dh.getContentType())
        );

        try (var inputStream = dh.getInputStream()) {
            Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            discard(tempFile, e);
        }

        return tempFile;
    }

    public Path bytesToTempFile(byte[] data, String filename) throws IOException {
        return writeToTempFile("ws-detached-signature-", filename, data);
    }

    private Path writeToTempFile(String prefix, String filename, byte[] data) throws IOException {
        var tempFile = tempFilesHelper.create(prefix, filename);

        try {
            Files.write(tempFile, data);
        } catch (IOException e) {
            discard(tempFile, e);
            throw e;
        }

        return tempFile;
    }
}
