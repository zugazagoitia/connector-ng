/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

insert into connector_message_attachments (
    id,
    identifier,
    content_type,
    description,
    name,
    file_size,
    storage,
    type,
    message_id,
    created_at,
    updated_at
)
values
    -- pending attachments
    (
        1,
        '104ebc70-abd5-45da-8c74-940d687501b3_messageContent',
        'application/xml',
        'Pending business content',
        'messageContent.xml',
        6613,
        'S3_BUCKET',
        'BUSINESS_CONTENT',
        NULL,
        now(),
        now()
    ),
    (
        2,
        'f79623a9-3792-4c6e-a96b-819bd4b69879_messageContent',
        'application/pdf',
        'Pending business document',
        'Form_A.pdf',
        6613,
        'S3_BUCKET',
        'BUSINESS_DOCUMENT',
        NULL,
        now(),
        now()
    ),
    (
        3,
        '1e74780d-9a92-4df2-a368-119d2f70eb07_xml_detached_signature',
        'application/pkcs7-signature',
        'Detached signature',
        'detachedSignature.p7b',
        30000000,
        'S3_BUCKET',
        'DETACHED_SIGNATURE',
        NULL,
        now(),
        now()
    );
