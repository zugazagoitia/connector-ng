/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

create table connector_business_domains
(
    id          bigint auto_increment
        primary key,
    created_at  datetime(6)                                                       not null,
    updated_at  datetime(6)                                                       not null,
    description varchar(512)                                                      null,
    enabled     bit                                                               null,
    identifier  varchar(255)                                                      not null,
    source      enum ('APPLICATION', 'DATABASE', 'ENVIRONMENT', 'IMPLEMENTATION') not null,
    uuid        varchar(255)                                                      not null,
    constraint UQ_CBD_IDENTIFIER
        unique (identifier),
    constraint UQ_CBD_UUID
        unique (uuid)
);

create table connector_message_transport_steps
(
    id                             bigint auto_increment
        primary key,
    created_at                     datetime(6)                          not null,
    updated_at                     datetime(6)                          not null,
    identifier                     varchar(255)                         not null,
    link_partner_name              varchar(255)                         not null,
    number_of_attempts             int                                  not null,
    remote_system_identifier       varchar(255)                         null,
    status                         enum (
        'DELIVERED', 'PULLED', 'FAILED', 'READY_FOR_PULL', 'SUBMITTED') not null,
    transported_message            mediumtext                           not null,
    transported_message_identifier varchar(255)                         not null,
    constraint UQ_CM_TS_IDENTIFIER
        unique (identifier)
);

create table connector_message_transport_step_statuses
(
    id                bigint auto_increment
        primary key,
    created_at        datetime(6)                                       not null,
    updated_at        datetime(6)                                       not null,
    status            enum (
        'DELIVERED', 'PULLED', 'FAILED', 'READY_FOR_PULL', 'SUBMITTED') not null,
    transport_step_id bigint                                            null,
    constraint FK_CM_TSS_TRANSPORT_STEP
        foreign key (transport_step_id) references connector_message_transport_steps (id)
);

create index IDX_CM_TSS_TRANSPORT_STEP_ID
    on connector_message_transport_step_statuses (transport_step_id);

create index IDX_CM_TS_LINK_PARTNER
    on connector_message_transport_steps (link_partner_name);

create index IDX_CM_TS_REMOTE_SYS_ID
    on connector_message_transport_steps (remote_system_identifier);

create index IDX_CM_TS_STATUS
    on connector_message_transport_steps (status);

create table connector_messages
(
    id                           bigint auto_increment
        primary key,
    created_at                   datetime(6)                       not null,
    updated_at                   datetime(6)                       not null,
    backend_message_identifier   varchar(255)                      null,
    backend_name                 varchar(255)                      null,
    confirmed_at                 datetime(6)                       null,
    deleted_at                   datetime(6)                       null,
    delivered_to_link_partner_at datetime(6)                       null,
    direction                    enum (
        'BACKEND_TO_GATEWAY', 'GATEWAY_TO_BACKEND')                not null,
    gateway_name                 varchar(255)                      null,
    status                       enum (
        'CONFIRMED', 'CREATED', 'DELETED', 'DELIVERED',
        'FAILED', 'PULLED', 'READY_FOR_PULL',
        'RECEIVED_BY_OPPOSITE_CONNECTOR', 'REJECTED', 'SUBMITTED') not null,

    identifier                   varchar(255)                      not null,
    rejected_at                  datetime(6)                       null,
    business_domain_id           bigint                            not null,
    constraint UQ_CM_IDENTIFIER
        unique (identifier),
    constraint FK_CM_BUSINESS_DOMAIN
        foreign key (business_domain_id) references connector_business_domains (id)
);

create table connector_message_attachments
(
    id           bigint auto_increment
        primary key,
    created_at   datetime(6)                                            not null,
    updated_at   datetime(6)                                            not null,
    deleted_at   datetime(6)                                            null,
    content_type varchar(255)                                           not null,
    description  varchar(255)                                           not null,
    identifier   varchar(255)                                           not null,
    name         varchar(255)                                           not null,
    file_size    bigint                                                 not null,
    storage      enum ('S3_BUCKET')                                     not null,
    type         enum (
        'ASICS', 'ATTACHMENT', 'BUSINESS_CONTENT', 'BUSINESS_DOCUMENT',
        'DETACHED_SIGNATURE', 'EVIDENCE_XML', 'PDF_TOKEN', 'XML_TOKEN') null,
    message_id   bigint                                                 null,
    constraint UQ_CM_ATTACHMENTS_IDENTIFIER
        unique (identifier),
    constraint FK_CM_ATTACHMENTS_MESSAGE
        foreign key (message_id) references connector_messages (id)
);

create index IDX_CM_ATTACHMENTS_MESSAGE_ID
    on connector_message_attachments (message_id);

create index IDX_CM_ATTACHMENTS_NAME
    on connector_message_attachments (name);

create index IDX_CM_ATTACHMENTS_TYPE
    on connector_message_attachments (type);

create table connector_message_business_contents
(
    id                        bigint auto_increment
        primary key,
    created_at                datetime(6)  not null,
    updated_at                datetime(6)  not null,
    uuid                      varchar(255) not null,
    message_id                bigint       not null,
    xml_content_attachment_id bigint       not null,
    constraint UQ_CM_BC_ATTACHMENT
        unique (xml_content_attachment_id),
    constraint UQ_CM_BC_MESSAGE
        unique (message_id),
    constraint UQ_CM_BC_UUID
        unique (uuid),
    constraint FK_CM_BC_XML_CT_ATTACHMENT
        foreign key (xml_content_attachment_id) references connector_message_attachments (id),
    constraint FK_CM_BC_MESSAGE
        foreign key (message_id) references connector_messages (id)
);

create table connector_message_business_documents
(
    id                               bigint auto_increment
        primary key,
    created_at                       datetime(6)                                      not null,
    updated_at                       datetime(6)                                      not null,
    aes_type                         enum ('AUTHENTICATION_BASED', 'SIGNATURE_BASED') null,
    uuid                             varchar(255)                                     not null,
    business_content_id              bigint                                           not null,
    content_attachment_id            bigint                                           not null,
    detached_signature_attachment_id bigint                                           null,
    constraint UQ_CM_BD_ATTACHMENT
        unique (content_attachment_id),
    constraint UQ_CM_BD_CONTENT
        unique (business_content_id),
    constraint UQ_CM_BD_DS_ATTACHMENT
        unique (detached_signature_attachment_id),
    constraint UQ_CM_BD_UUID
        unique (uuid),
    constraint FK_CM_BD_CONTENT
        foreign key (business_content_id) references connector_message_business_contents (id),
    constraint FK_CM_BD_ATT_CONTENT
        foreign key (content_attachment_id) references connector_message_attachments (id),
    constraint FK_CM_BD_DS_ATTACHMENT
        foreign key (detached_signature_attachment_id) references connector_message_attachments (id)
);

create index IDX_CM_BD_AES_TYPE
    on connector_message_business_documents (aes_type);

create table connector_message_errors
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    details    varchar(255) null,
    label      varchar(255) null,
    source     varchar(255) not null,
    message_id bigint       not null,
    constraint FK_CM_ERRORS_MESSAGE
        foreign key (message_id) references connector_messages (id)
);

create index IDX_CM_ERRORS_MESSAGE_ID
    on connector_message_errors (message_id);

create table connector_message_evidences
(
    id                           bigint auto_increment
        primary key,
    created_at                   datetime(6)                          not null,
    updated_at                   datetime(6)                          not null,
    content                      mediumtext                           not null,
    delivered_to_link_partner_at datetime(6)                          null,
    type                         enum (
        'DELIVERY', 'NON_DELIVERY', 'NON_RETRIEVAL',
        'RELAY_REMMD_ACCEPTANCE', 'RELAY_REMMD_FAILURE', 'RELAY_REMMD_REJECTION',
        'RETRIEVAL', 'SUBMISSION_ACCEPTANCE', 'SUBMISSION_REJECTION') not null,
    uuid                         varchar(255)                         not null,
    message_id                   bigint                               null,
    constraint UQ_CM_EVIDENCES_UUID
        unique (uuid),
    constraint FK_CM_EVIDENCES_MESSAGE
        foreign key (message_id) references connector_messages (id)
);

create index IDX_CM_EVIDENCES_MESSAGE_ID
    on connector_message_evidences (message_id);

create index IDX_CM_BACKEND_MSG_ID
    on connector_messages (backend_message_identifier);

create index IDX_CM_BACKEND_NAME
    on connector_messages (backend_name);

create index IDX_CM_BUSINESS_DOMAIN_ID
    on connector_messages (business_domain_id);

create index IDX_CM_CREATED_AT
    on connector_messages (created_at);

create index IDX_CM_DELIVERED_AT
    on connector_messages (delivered_to_link_partner_at);

create index IDX_CM_DIRECTION
    on connector_messages (direction);

create index IDX_CM_GATEWAY_NAME
    on connector_messages (gateway_name);

create table connector_processing_modes
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    content            mediumtext   not null,
    description        varchar(512) not null,
    filename           varchar(255) not null,
    uuid               varchar(255) not null,
    business_domain_id bigint       not null,
    constraint UQ_CPM_UUID
        unique (uuid),
    constraint FK_CPM_BUSINESS_DOMAIN
        foreign key (business_domain_id) references connector_business_domains (id)
);

create table connector_processing_mode_actions
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    name               varchar(255) not null,
    uuid               varchar(255) not null,
    processing_mode_id bigint       not null,
    constraint UQ_CPM_ACTIONS_UUID
        unique (uuid),
    constraint FK_CPM_ACTIONS_PM
        foreign key (processing_mode_id) references connector_processing_modes (id)
);

create index IDX_CPM_ACTIONS_NAME
    on connector_processing_mode_actions (name);

create index IDX_CPM_ACTIONS_PM_ID
    on connector_processing_mode_actions (processing_mode_id);

create table connector_processing_mode_parties
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)                     not null,
    updated_at         datetime(6)                     not null,
    identifier         varchar(255)                    not null,
    identifier_type    varchar(255)                    not null,
    is_home            bit                             null,
    name               varchar(255)                    not null,
    role               varchar(255)                    not null,
    role_type          enum ('INITIATOR', 'RESPONDER') not null,
    uuid               varchar(255)                    not null,
    processing_mode_id bigint                          not null,
    constraint UQ_CPM_PARTIES_UUID
        unique (uuid),
    constraint FK_CPM_PARTIES_PM
        foreign key (processing_mode_id) references connector_processing_modes (id)
);

create index IDX_CPM_PARTIES_IDENTIFIER
    on connector_processing_mode_parties (identifier);

create index IDX_CPM_PARTIES_IDENT_TYPE
    on connector_processing_mode_parties (identifier_type);

create index IDX_CPM_PARTIES_PM_ID
    on connector_processing_mode_parties (processing_mode_id);

create index IDX_CPM_PARTIES_ROLE
    on connector_processing_mode_parties (role);

create index IDX_CPM_PARTIES_ROLE_TYPE
    on connector_processing_mode_parties (role_type);

create table connector_processing_mode_services
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    name               varchar(255) not null,
    type               varchar(255) not null,
    uuid               varchar(255) not null,
    processing_mode_id bigint       not null,
    constraint UQ_CPM_SERVICES_UUID
        unique (uuid),
    constraint FK_CPM_SERVICES_PM
        foreign key (processing_mode_id) references connector_processing_modes (id)
);

create table connector_message_as4_properties
(
    id                      bigint auto_increment
        primary key,
    created_at              datetime(6)  not null,
    updated_at              datetime(6)  not null,
    conversation_identifier varchar(255) null,
    ebms_message_identifier varchar(255) null,
    final_recipient         varchar(255) not null,
    original_sender         varchar(255) not null,
    reference_to_identifier varchar(255) null,
    action_id               bigint       not null,
    from_party_id           bigint       not null,
    message_id              bigint       not null,
    service_id              bigint       not null,
    to_party_id             bigint       not null,
    constraint UQ_CM_AS4_MESSAGE
        unique (message_id),
    constraint FK_CM_AS4_ACTION
        foreign key (action_id) references connector_processing_mode_actions (id),
    constraint FK_CM_AS4_FROM_PARTY
        foreign key (from_party_id) references connector_processing_mode_parties (id),
    constraint FK_CM_AS4_MESSAGE
        foreign key (message_id) references connector_messages (id),
    constraint FK_CM_AS4_SERVICE
        foreign key (service_id) references connector_processing_mode_services (id),
    constraint FK_CM_AS4_TO_PARTY
        foreign key (to_party_id) references connector_processing_mode_parties (id)
);

create index IDX_CM_AS4_ACTION_ID
    on connector_message_as4_properties (action_id);

create index IDX_CM_AS4_CONVERSATION_ID
    on connector_message_as4_properties (conversation_identifier);

create index IDX_CM_AS4_EBMS_MSG_ID
    on connector_message_as4_properties (ebms_message_identifier);

create index IDX_CM_AS4_FINAL_RECIPIENT
    on connector_message_as4_properties (final_recipient);

create index IDX_CM_AS4_FROM_PARTY_ID
    on connector_message_as4_properties (from_party_id);

create index IDX_CM_AS4_ORIG_SENDER
    on connector_message_as4_properties (original_sender);

create index IDX_CM_AS4_REF_TO_ID
    on connector_message_as4_properties (reference_to_identifier);

create index IDX_CM_AS4_SERVICE_ID
    on connector_message_as4_properties (service_id);

create index IDX_CM_AS4_TO_PARTY_ID
    on connector_message_as4_properties (to_party_id);

create index IDX_CPM_SERVICES_NAME
    on connector_processing_mode_services (name);

create index IDX_CPM_SERVICES_PM_ID
    on connector_processing_mode_services (processing_mode_id);

create index IDX_CPM_SERVICES_TYPE
    on connector_processing_mode_services (type);

create table connector_processing_mode_truststores
(
    id                 bigint auto_increment
        primary key,
    created_at         datetime(6)            not null,
    updated_at         datetime(6)            not null,
    content            mediumblob             not null,
    filename           varchar(255)           not null,
    password           varchar(255)           not null,
    type               enum ('JKS', 'PKCS12') not null,
    processing_mode_id bigint                 not null,
    constraint UQ_CPM_TRUSTSTORES_PM
        unique (processing_mode_id),
    constraint FK_CPM_TRUSTSTORES_PM
        foreign key (processing_mode_id) references connector_processing_modes (id)
);

create index IDX_CPM_BUSINESS_DOMAIN_ID
    on connector_processing_modes (business_domain_id);

create table connector_roles
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    name       varchar(255) not null,
    uuid       varchar(255) not null,
    constraint UQ_CR_NAME
        unique (name),
    constraint UQ_CR_UUID
        unique (uuid)
);

create table connector_users
(
    id                   bigint auto_increment
        primary key,
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    email                varchar(255) null,
    enabled              bit          null,
    must_change_password bit          null,
    password             varchar(255) not null,
    username             varchar(255) not null,
    uuid                 varchar(255) not null,
    constraint UQ_CU_USERNAME
        unique (username),
    constraint UQ_CU_UUID
        unique (uuid)
);

create table connector_refresh_tokens
(
    id         bigint auto_increment
        primary key,
    created_at datetime(6)  not null,
    updated_at datetime(6)  not null,
    expires_at datetime(6)  not null,
    revoked    bit          not null,
    revoked_at datetime(6)  null,
    token      varchar(255) not null,
    user_id    bigint       not null,
    constraint UQ_CRT_TOKEN
        unique (token),
    constraint FK_CRT_USER
        foreign key (user_id) references connector_users (id)
);

create index IDX_CRT_USER_ID
    on connector_refresh_tokens (user_id);

create table connector_users_roles
(
    user_id bigint not null,
    role_id bigint not null,
    constraint UQ_CUR_USER_ROLE
        unique (user_id, role_id),
    constraint FK_CUR_ROLE
        foreign key (role_id) references connector_roles (id),
    constraint FK_CUR_USER
        foreign key (user_id) references connector_users (id)
);
