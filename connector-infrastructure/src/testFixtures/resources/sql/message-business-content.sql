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
INTO connector_message_business_contents
(
    id,
    uuid,
    xml_content_attachment_id,
    message_id,
    created_at,
    updated_at
)
VALUES
    (
        1,
        'fb3ae502-2d92-4ff4-8cd8-b17c9cfc1e07',
        8,
        1,
        now(),
        now()
    ),
    (
        2,
        'c0a44547-b00e-4dd3-9a3f-6d6db95353c8',
        7,
        2,
        now(),
        now()
    ),
    (
        3,
        '77a31ea2-eec1-4e10-be4d-40481563cf9a',
        9,
        3,
        now(),
        now()
    ),
    (
        4,
        'c2becfdb-daa8-42a9-8b6e-4b980a25c6c0',
        10,
        4,
        now(),
        now()
    );
