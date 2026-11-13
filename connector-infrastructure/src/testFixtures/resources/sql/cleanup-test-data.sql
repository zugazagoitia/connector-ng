/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE connector_message_transport_step_statuses;
TRUNCATE TABLE connector_message_transport_steps;
TRUNCATE TABLE connector_message_evidences;
TRUNCATE TABLE connector_message_errors;
TRUNCATE TABLE connector_message_business_documents;
TRUNCATE TABLE connector_message_business_contents;
TRUNCATE TABLE connector_message_as4_properties;
TRUNCATE TABLE connector_messages;
TRUNCATE TABLE connector_message_attachments;
TRUNCATE TABLE connector_parties;
TRUNCATE TABLE connector_services;
TRUNCATE TABLE connector_actions;
TRUNCATE TABLE connector_processing_modes;
TRUNCATE TABLE connector.connector_processing_mode_truststores;
TRUNCATE TABLE connector_business_domains;

SET FOREIGN_KEY_CHECKS = 1;
