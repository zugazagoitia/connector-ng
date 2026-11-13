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
    updated_at,
    deleted_at
)
values
    (
        1,
        'd98a621a-4d14-4cfb-be00-0feae9f9b277_fake_file',
        'application/pdf',
        'Persisting file to S3 bucket',
        'fake_file.pdf',
        157286400,
        'S3_BUCKET',
        'ATTACHMENT',
        null,
        now(),
        now(),
        null
    ),
    (
        2,
        '6aeef356-d580-4b94-a569-250435ac3ec5_fake_file',
        'application/pdf',
        'Persisting file to S3 bucket',
        'fake_file.pdf',
        200000000,
        'S3_BUCKET',
        'ATTACHMENT',
        1,
        now(),
        now(),
        null
    ),
    (
        3,
        'c12f879b-3c9a-4d26-b36c-b6d67a84f0ed_test_attachment',
        'application/pdf',
        'Persisting file to S3 bucket',
        'fake_file.pdf',
        300000000,
        'S3_BUCKET',
        'ATTACHMENT',
        1,
        now(),
        now(),
        null
    ),
    (
        4,
        'd9368fda-92f2-498f-95bf-1ca6f4985b85_ASIC-S',
        'application/vnd.etsi.asic-s+zip',
        'File from gateway',
        'ASIC-S.zip',
        400000000,
        'S3_BUCKET',
        'ASICS',
        2,
        now(),
        now(),
        null
    ),
    (
        5,
        '0f942a85-1e4d-4e36-9432-1625a582b20c_tokenXML',
        'application/xml',
        'File from gateway',
        'tokenXML.xml',
        5282,
        'S3_BUCKET',
        'XML_TOKEN',
        2,
        now(),
        now(),
        null
    ),
    (
        6,
        'c3e18064-e0da-4170-9733-1e7e2768e0bb_SUBMISSION_ACCEPTANCE',
        'application/xml',
        'File from gateway',
        'SUBMISSION_ACCEPTANCE.xml',
        6613,
        'S3_BUCKET',
        'EVIDENCE_XML',
        2,
        now(),
        now(),
        null
    ),
    -- Business content
    (
        7,
        '104ebc70-abd5-45da-8c74-940d687501b3_messageContent',
        'application/xml',
        'File from gateway',
        'messageContent.xml',
        6613,
        'S3_BUCKET',
        'BUSINESS_CONTENT',
        2,
        now(),
        now(),
        null
    ),
    (
        8,
        '1875f1b7-5ea2-494d-8932-29966189f02c_messageContent',
        'application/xml',
        'Outgoing message business content',
        'messageContent.xml',
        6613,
        'S3_BUCKET',
        'BUSINESS_CONTENT',
        1,
        now(),
        now(),
        null
    ),
    (
        9,
        'b9bd63c7-5a5a-4701-a23c-e5e714436585_messageContent',
        'application/xml',
        'File from gateway',
        'messageContent.xml',
        6613,
        'S3_BUCKET',
        'BUSINESS_CONTENT',
        3,
        now(),
        now(),
        null
    ),
    (
        10,
        'c8bb2032-22cc-411a-969d-68f1c526f0ad_messageContent',
        'application/xml',
        'File from gateway',
        'messageContent.xml',
        6613,
        'S3_BUCKET',
        'BUSINESS_CONTENT',
        4,
        now(),
        now(),
        null
    ),
    -- Business document
    (
        11,
        'dd5a7178-c1aa-4723-bc7b-d3f007ccd7ac_messageContent',
        'application/pdf',
        'Business document',
        'Form_A.pdf',
        6613,
        'S3_BUCKET',
        'BUSINESS_DOCUMENT',
        1,
        now(),
        now(),
        null
    ),
    (
        12,
        'd0b094c0-3058-4800-af91-96f89c60c9c0_messageContent',
        'application/pdf',
        'Business document',
        'Form_A.pdf',
        6613,
        'S3_BUCKET',
        'BUSINESS_DOCUMENT',
        2,
        now(),
        now(),
        null
    ),
    (
        13,
        '99969b4f-ed87-489b-aa77-9525ad9d1b42_messageContent',
        'application/pdf',
        'Business document',
        'Form_A.pdf',
        6613,
        'S3_BUCKET',
        'BUSINESS_DOCUMENT',
        3,
        now(),
        now(),
        null
    ),
    (
        14,
        '890a990c-1ce9-4b96-8c07-5836f8f0da33_messageContent',
        'application/pdf',
        'Business document',
        'Form_A.pdf',
        6613,
        'S3_BUCKET',
        'BUSINESS_DOCUMENT',
        4,
        now(),
        now(),
        null
    ),
    (
        15,
        'f07d1bfd-685a-4fee-b70d-a582cd0711c7_test_attachment',
        'application/pdf',
        'Persisting file to S3 bucket',
        'fake_file.pdf',
        300000000,
        'S3_BUCKET',
        'ATTACHMENT',
        null,
        now(),
        now(),
        now()
    );
