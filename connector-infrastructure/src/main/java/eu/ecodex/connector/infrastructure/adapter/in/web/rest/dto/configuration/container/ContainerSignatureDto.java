/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.dto.configuration.container;

import eu.ecodex.connector.infrastructure.config.property.container.ContainerSignatureProperties;
import eu.europa.esig.dss.enumerations.DigestAlgorithm;
import eu.europa.esig.dss.enumerations.EncryptionAlgorithm;
import lombok.Builder;

@Builder
@SuppressWarnings({"checkstyle:MissingJavadocMethod", "checkstyle:MissingJavadocType"})
public record ContainerSignatureDto(
    EncryptionAlgorithm encryptionAlgorithm,
    DigestAlgorithm digestAlgorithm,
    KeystoreDto keystore,
    PrivateKeyDto privateKey,
    SignatureValidationDto validation
) {
    public static ContainerSignatureDto from(ContainerSignatureProperties p) {
        return new ContainerSignatureDto(
            p.getEncryptionAlgorithm(),
            p.getDigestAlgorithm(),
            KeystoreDto.from(p.getKeystore()),
            PrivateKeyDto.from(p.getPrivateKey()),
            SignatureValidationDto.from(p.getValidation())
        );
    }
}
