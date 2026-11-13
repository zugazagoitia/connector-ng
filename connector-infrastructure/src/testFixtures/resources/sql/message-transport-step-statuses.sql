/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

insert into connector_message_transport_step_statuses (
    status,
    created_at,
    updated_at,
    transport_step_id
)
values (
           'SUBMITTED',
           now(),
           now(),
           1
       ),
       (
           'READY_FOR_PULL',
           now(),
           now(),
           2
       ),
       (
           'READY_FOR_PULL',
           now(),
           now(),
           3
       ),
       (
           'PULLED',
           now(),
           now(),
           3
       ),
       (
           'READY_FOR_PULL',
           now(),
           now(),
           4
       );
