/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

INSERT
INTO connector_message_business_documents
(
    uuid, aes_type, business_content_id, content_attachment_id, created_at, updated_at
)
VALUES
    (
        'e9ccd4ef-9e51-4ad2-b37a-40b74a2dd93e', 'SIGNATURE_BASED', 1, 11, now(), now()
    ),
    (
        '48e22010-fd0f-409f-a67a-2113a08b91cf', 'SIGNATURE_BASED', 2, 12, now(), now()
    ),
    (
        'ddd654ca-afd2-4aef-8115-30dfa97fff60', 'SIGNATURE_BASED', 3, 13, now(), now()
    ),
    (
        '9eb72622-7007-4978-a072-dfa8efb42cbc', 'SIGNATURE_BASED', 4, 14, now(), now()
    );
