/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

create table CONNECTOR_BUSINESS_DOMAINS
(
    ID          NUMBER(19) generated as identity
        primary key,
    CREATED_AT  TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT  TIMESTAMP(9) WITH TIME ZONE not null,
    DESCRIPTION VARCHAR2(512 char),
    ENABLED     BOOLEAN,
    IDENTIFIER  VARCHAR2(255 char)          not null
        constraint UQ_CBD_IDENTIFIER
            unique,
    SOURCE      VARCHAR2(255 char)          not null
        check ((source in ('DATABASE', 'IMPLEMENTATION', 'ENVIRONMENT', 'APPLICATION'))),
    UUID        VARCHAR2(255 char)          not null
        constraint UQ_CBD_UUID
            unique
)
/

create table CONNECTOR_MESSAGE_TRANSPORT_STEPS
(
    ID                             NUMBER(19) generated as identity
        primary key,
    CREATED_AT                     TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT                     TIMESTAMP(9) WITH TIME ZONE not null,
    IDENTIFIER                     VARCHAR2(255 char)          not null
        constraint UQ_CM_TS_IDENTIFIER
            unique,
    LINK_PARTNER_NAME              VARCHAR2(255 char)          not null,
    NUMBER_OF_ATTEMPTS             NUMBER(10)                  not null,
    REMOTE_SYSTEM_IDENTIFIER       VARCHAR2(255 char),
    STATUS                         VARCHAR2(255 char)          not null
        check ((status in ('DELIVERED', 'FAILED', 'SUBMITTED', 'PULLED', 'READY_FOR_PULL'))),
    TRANSPORTED_MESSAGE            CLOB                        not null,
    TRANSPORTED_MESSAGE_IDENTIFIER VARCHAR2(255 char)          not null
)
/

create table CONNECTOR_MESSAGE_TRANSPORT_STEP_STATUSES
(
    ID                NUMBER(19) generated as identity
        primary key,
    CREATED_AT        TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT        TIMESTAMP(9) WITH TIME ZONE not null,
    STATUS            VARCHAR2(255 char)          not null
        check ((status in ('DELIVERED', 'FAILED', 'SUBMITTED', 'PULLED', 'READY_FOR_PULL'))),
    TRANSPORT_STEP_ID NUMBER(19)
        constraint FK_CM_TSS_TRANSPORT_STEP
            references CONNECTOR_MESSAGE_TRANSPORT_STEPS
)
/

create index IDX_CM_TSS_TRANSPORT_STEP_ID
    on CONNECTOR_MESSAGE_TRANSPORT_STEP_STATUSES (TRANSPORT_STEP_ID)
/

create index IDX_CM_TS_REMOTE_SYS_ID
    on CONNECTOR_MESSAGE_TRANSPORT_STEPS (REMOTE_SYSTEM_IDENTIFIER)
/

create index IDX_CM_TS_LINK_PARTNER
    on CONNECTOR_MESSAGE_TRANSPORT_STEPS (LINK_PARTNER_NAME)
/

create index IDX_CM_TS_STATUS
    on CONNECTOR_MESSAGE_TRANSPORT_STEPS (STATUS)
/

create table CONNECTOR_MESSAGES
(
    ID                           NUMBER(19) generated as identity
        primary key,
    CREATED_AT                   TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT                   TIMESTAMP(9) WITH TIME ZONE not null,
    BACKEND_MESSAGE_IDENTIFIER   VARCHAR2(255 char),
    BACKEND_NAME                 VARCHAR2(255 char),
    CONFIRMED_AT                 TIMESTAMP(9) WITH TIME ZONE,
    DELETED_AT                   TIMESTAMP(9) WITH TIME ZONE,
    DELIVERED_TO_LINK_PARTNER_AT TIMESTAMP(9) WITH TIME ZONE,
    DIRECTION                    VARCHAR2(255 char)          not null
        check ((direction in ('BACKEND_TO_GATEWAY', 'GATEWAY_TO_BACKEND'))),
    GATEWAY_NAME                 VARCHAR2(255 char),
    STATUS                       VARCHAR2(255 char)          not null
        check ((status in ('CONFIRMED', 'CREATED', 'DELETED', 'DELIVERED',
                           'FAILED', 'PULLED', 'READY_FOR_PULL',
                           'RECEIVED_BY_OPPOSITE_CONNECTOR', 'REJECTED', 'SUBMITTED'))),
    IDENTIFIER                   VARCHAR2(255 char)          not null
        constraint UQ_CM_IDENTIFIER
            unique,
    REJECTED_AT                  TIMESTAMP(9) WITH TIME ZONE,
    BUSINESS_DOMAIN_ID           NUMBER(19)                  not null
        constraint FK_CM_BUSINESS_DOMAIN
            references CONNECTOR_BUSINESS_DOMAINS
)
/

create table CONNECTOR_MESSAGE_ATTACHMENTS
(
    ID           NUMBER(19) generated as identity
        primary key,
    CREATED_AT   TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT   TIMESTAMP(9) WITH TIME ZONE not null,
    DELETED_AT   TIMESTAMP(9) WITH TIME ZONE null,
    CONTENT_TYPE VARCHAR2(255 char)          not null,
    DESCRIPTION  VARCHAR2(255 char)          not null,
    IDENTIFIER   VARCHAR2(255 char)          not null
        constraint UQ_CM_ATTACHMENTS_IDENTIFIER
            unique,
    NAME         VARCHAR2(255 char)          not null,
    FILE_SIZE    NUMBER(19)                  not null,
    STORAGE      VARCHAR2(255 char)          not null
        check ((storage in ('S3_BUCKET'))),
    TYPE         VARCHAR2(255 char)
        check ((type in
                ('ATTACHMENT', 'ASICS', 'BUSINESS_CONTENT', 'BUSINESS_DOCUMENT',
                 'DETACHED_SIGNATURE', 'EVIDENCE_XML', 'PDF_TOKEN', 'XML_TOKEN'))),
    MESSAGE_ID   NUMBER(19)
        constraint FK_CM_ATTACHMENTS_MESSAGE
            references CONNECTOR_MESSAGES
)
/

create index IDX_CM_ATTACHMENTS_NAME
    on CONNECTOR_MESSAGE_ATTACHMENTS (NAME)
/

create index IDX_CM_ATTACHMENTS_TYPE
    on CONNECTOR_MESSAGE_ATTACHMENTS (TYPE)
/

create index IDX_CM_ATTACHMENTS_MESSAGE_ID
    on CONNECTOR_MESSAGE_ATTACHMENTS (MESSAGE_ID)
/

create table CONNECTOR_MESSAGE_BUSINESS_CONTENTS
(
    ID                        NUMBER(19) generated as identity
        primary key,
    CREATED_AT                TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT                TIMESTAMP(9) WITH TIME ZONE not null,
    UUID                      VARCHAR2(255 char)          not null
        constraint UQ_CM_BC_UUID
            unique,
    MESSAGE_ID                NUMBER(19)                  not null
        constraint UQ_CM_BC_MESSAGE
            unique
        constraint FK_CM_BC_MESSAGE
            references CONNECTOR_MESSAGES,
    XML_CONTENT_ATTACHMENT_ID NUMBER(19)                  not null
        constraint UQ_CM_BC_ATTACHMENT
            unique
        constraint FK_CM_BC_XML_CT_ATTACHMENT
            references CONNECTOR_MESSAGE_ATTACHMENTS
)
/

create table CONNECTOR_MESSAGE_BUSINESS_DOCUMENTS
(
    ID                               NUMBER(19) generated as identity
        primary key,
    CREATED_AT                       TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT                       TIMESTAMP(9) WITH TIME ZONE not null,
    AES_TYPE                         VARCHAR2(255 char)
        check ((aes_type in ('AUTHENTICATION_BASED', 'SIGNATURE_BASED'))),
    UUID                             VARCHAR2(255 char)          not null
        constraint UQ_CM_BD_UUID
            unique,
    CONTENT_ATTACHMENT_ID            NUMBER(19)                  not null
        constraint UQ_CM_BD_ATTACHMENT
            unique
        constraint FK_CM_BD_ATT_CONTENT
            references CONNECTOR_MESSAGE_ATTACHMENTS,
    DETACHED_SIGNATURE_ATTACHMENT_ID NUMBER(19)                  null
        constraint UQ_CM_BD_DS_ATTACHMENT
            unique
        constraint FK_CM_BD_DS_ATTACHMENT
            references CONNECTOR_MESSAGE_ATTACHMENTS,
    BUSINESS_CONTENT_ID              NUMBER(19)                  not null
        constraint UQ_CM_BD_CONTENT
            unique
        constraint FK_CM_BD_CONTENT
            references CONNECTOR_MESSAGE_BUSINESS_CONTENTS
)
/

create index IDX_CM_BD_AES_TYPE
    on CONNECTOR_MESSAGE_BUSINESS_DOCUMENTS (AES_TYPE)
/

create table CONNECTOR_MESSAGE_ERRORS
(
    ID         NUMBER(19) generated as identity
        primary key,
    CREATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    DETAILS    VARCHAR2(255 char),
    LABEL      VARCHAR2(255 char),
    SOURCE     VARCHAR2(255 char)          not null,
    MESSAGE_ID NUMBER(19)                  not null
        constraint FK_CM_ERRORS_MESSAGE
            references CONNECTOR_MESSAGES
)
/

create index IDX_CM_ERRORS_MESSAGE_ID
    on CONNECTOR_MESSAGE_ERRORS (MESSAGE_ID)
/

create table CONNECTOR_MESSAGE_EVIDENCES
(
    ID                           NUMBER(19) generated as identity
        primary key,
    CREATED_AT                   TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT                   TIMESTAMP(9) WITH TIME ZONE not null,
    CONTENT                      CLOB                        not null,
    DELIVERED_TO_LINK_PARTNER_AT TIMESTAMP(9) WITH TIME ZONE,
    TYPE                         VARCHAR2(255 char)          not null
        check ((type in
                ('SUBMISSION_ACCEPTANCE', 'SUBMISSION_REJECTION', 'RELAY_REMMD_ACCEPTANCE',
                 'RELAY_REMMD_REJECTION', 'RELAY_REMMD_FAILURE', 'DELIVERY', 'NON_DELIVERY',
                 'RETRIEVAL', 'NON_RETRIEVAL'))),
    UUID                         VARCHAR2(255 char)          not null
        constraint UQ_CM_EVIDENCES_UUID
            unique,
    MESSAGE_ID                   NUMBER(19)
        constraint FK_CM_EVIDENCES_MESSAGE
            references CONNECTOR_MESSAGES
)
/

create index IDX_CM_EVIDENCES_MESSAGE_ID
    on CONNECTOR_MESSAGE_EVIDENCES (MESSAGE_ID)
/

create index IDX_CM_BACKEND_NAME
    on CONNECTOR_MESSAGES (BACKEND_NAME)
/

create index IDX_CM_BACKEND_MSG_ID
    on CONNECTOR_MESSAGES (BACKEND_MESSAGE_IDENTIFIER)
/

create index IDX_CM_GATEWAY_NAME
    on CONNECTOR_MESSAGES (GATEWAY_NAME)
/

create index IDX_CM_DIRECTION
    on CONNECTOR_MESSAGES (DIRECTION)
/

create index IDX_CM_DELIVERED_AT
    on CONNECTOR_MESSAGES (SYS_EXTRACT_UTC("DELIVERED_TO_LINK_PARTNER_AT"))
/

create index IDX_CM_CREATED_AT
    on CONNECTOR_MESSAGES (SYS_EXTRACT_UTC("CREATED_AT"))
/

create index IDX_CM_BUSINESS_DOMAIN_ID
    on CONNECTOR_MESSAGES (BUSINESS_DOMAIN_ID)
/

create table CONNECTOR_PROCESSING_MODES
(
    ID                 NUMBER(19) generated as identity
        primary key,
    CREATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    CONTENT            CLOB                        not null,
    DESCRIPTION        VARCHAR2(512 char)          not null,
    FILENAME           VARCHAR2(255 char)          not null,
    UUID               VARCHAR2(255 char)          not null
        constraint UQ_CPM_UUID
            unique,
    BUSINESS_DOMAIN_ID NUMBER(19)                  not null
        constraint FK_CPM_BUSINESS_DOMAIN
            references CONNECTOR_BUSINESS_DOMAINS
)
/

create table CONNECTOR_PROCESSING_MODE_ACTIONS
(
    ID                 NUMBER(19) generated as identity
        primary key,
    CREATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    NAME               VARCHAR2(255 char)          not null,
    UUID               VARCHAR2(255 char)          not null
        constraint UQ_CPM_ACTIONS_UUID
            unique,
    PROCESSING_MODE_ID NUMBER(19)                  not null
        constraint FK_CPM_ACTIONS_PM
            references CONNECTOR_PROCESSING_MODES
)
/

create index IDX_CPM_ACTIONS_NAME
    on CONNECTOR_PROCESSING_MODE_ACTIONS (NAME)
/

create index IDX_CPM_ACTIONS_PM_ID
    on CONNECTOR_PROCESSING_MODE_ACTIONS (PROCESSING_MODE_ID)
/

create table CONNECTOR_PROCESSING_MODE_PARTIES
(
    ID                 NUMBER(19) generated as identity
        primary key,
    CREATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    IDENTIFIER         VARCHAR2(255 char)          not null,
    IDENTIFIER_TYPE    VARCHAR2(255 char)          not null,
    IS_HOME            BOOLEAN,
    NAME               VARCHAR2(255 char)          not null,
    ROLE               VARCHAR2(255 char)          not null,
    ROLE_TYPE          VARCHAR2(255 char)          not null
        check ((role_type in ('INITIATOR', 'RESPONDER'))),
    UUID               VARCHAR2(255 char)          not null
        constraint UQ_CPM_PARTIES_UUID
            unique,
    PROCESSING_MODE_ID NUMBER(19)                  not null
        constraint FK_CPM_PARTIES_PM
            references CONNECTOR_PROCESSING_MODES
)
/

create index IDX_CPM_PARTIES_ROLE
    on CONNECTOR_PROCESSING_MODE_PARTIES (ROLE)
/

create index IDX_CPM_PARTIES_IDENTIFIER
    on CONNECTOR_PROCESSING_MODE_PARTIES (IDENTIFIER)
/

create index IDX_CPM_PARTIES_IDENT_TYPE
    on CONNECTOR_PROCESSING_MODE_PARTIES (IDENTIFIER_TYPE)
/

create index IDX_CPM_PARTIES_ROLE_TYPE
    on CONNECTOR_PROCESSING_MODE_PARTIES (ROLE_TYPE)
/

create index IDX_CPM_PARTIES_PM_ID
    on CONNECTOR_PROCESSING_MODE_PARTIES (PROCESSING_MODE_ID)
/

create table CONNECTOR_PROCESSING_MODE_SERVICES
(
    ID                 NUMBER(19) generated as identity
        primary key,
    CREATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    NAME               VARCHAR2(255 char)          not null,
    TYPE               VARCHAR2(255 char)          not null,
    UUID               VARCHAR2(255 char)          not null
        constraint UQ_CPM_SERVICES_UUID
            unique,
    PROCESSING_MODE_ID NUMBER(19)                  not null
        constraint FK_CPM_SERVICES_PM
            references CONNECTOR_PROCESSING_MODES
)
/

create table CONNECTOR_MESSAGE_AS4_PROPERTIES
(
    ID                      NUMBER(19) generated as identity
        primary key,
    CREATED_AT              TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT              TIMESTAMP(9) WITH TIME ZONE not null,
    CONVERSATION_IDENTIFIER VARCHAR2(255 char),
    EBMS_MESSAGE_IDENTIFIER VARCHAR2(255 char),
    FINAL_RECIPIENT         VARCHAR2(255 char)          not null,
    ORIGINAL_SENDER         VARCHAR2(255 char)          not null,
    REFERENCE_TO_IDENTIFIER VARCHAR2(255 char),
    ACTION_ID               NUMBER(19)                  not null
        constraint FK_CM_AS4_ACTION
            references CONNECTOR_PROCESSING_MODE_ACTIONS,
    FROM_PARTY_ID           NUMBER(19)                  not null
        constraint FK_CM_AS4_FROM_PARTY
            references CONNECTOR_PROCESSING_MODE_PARTIES,
    MESSAGE_ID              NUMBER(19)                  not null
        constraint UQ_CM_AS4_MESSAGE
            unique
        constraint FK_CM_AS4_MESSAGE
            references CONNECTOR_MESSAGES,
    SERVICE_ID              NUMBER(19)                  not null
        constraint FK_CM_AS4_SERVICE
            references CONNECTOR_PROCESSING_MODE_SERVICES,
    TO_PARTY_ID             NUMBER(19)                  not null
        constraint FK_CM_AS4_TO_PARTY
            references CONNECTOR_PROCESSING_MODE_PARTIES
)
/

create index IDX_CM_AS4_EBMS_MSG_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (EBMS_MESSAGE_IDENTIFIER)
/

create index IDX_CM_AS4_REF_TO_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (REFERENCE_TO_IDENTIFIER)
/

create index IDX_CM_AS4_CONVERSATION_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (CONVERSATION_IDENTIFIER)
/

create index IDX_CM_AS4_ORIG_SENDER
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (ORIGINAL_SENDER)
/

create index IDX_CM_AS4_FINAL_RECIPIENT
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (FINAL_RECIPIENT)
/

create index IDX_CM_AS4_SERVICE_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (SERVICE_ID)
/

create index IDX_CM_AS4_ACTION_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (ACTION_ID)
/

create index IDX_CM_AS4_FROM_PARTY_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (FROM_PARTY_ID)
/

create index IDX_CM_AS4_TO_PARTY_ID
    on CONNECTOR_MESSAGE_AS4_PROPERTIES (TO_PARTY_ID)
/

create index IDX_CPM_SERVICES_NAME
    on CONNECTOR_PROCESSING_MODE_SERVICES (NAME)
/

create index IDX_CPM_SERVICES_TYPE
    on CONNECTOR_PROCESSING_MODE_SERVICES (TYPE)
/

create index IDX_CPM_SERVICES_PM_ID
    on CONNECTOR_PROCESSING_MODE_SERVICES (PROCESSING_MODE_ID)
/

create table CONNECTOR_PROCESSING_MODE_TRUSTSTORES
(
    ID                 NUMBER(19) generated as identity
        primary key,
    CREATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT         TIMESTAMP(9) WITH TIME ZONE not null,
    CONTENT            BLOB                        not null,
    FILENAME           VARCHAR2(255 char)          not null,
    PASSWORD           VARCHAR2(255 char)          not null,
    TYPE               VARCHAR2(255 char)          not null
        check ((type in ('JKS', 'PKCS12'))),
    PROCESSING_MODE_ID NUMBER(19)                  not null
        constraint UQ_CPM_TRUSTSTORES_PM
            unique
        constraint FK_CPM_TRUSTSTORES_PM
            references CONNECTOR_PROCESSING_MODES
)
/

create index IDX_CPM_BUSINESS_DOMAIN_ID
    on CONNECTOR_PROCESSING_MODES (BUSINESS_DOMAIN_ID)
/

create table CONNECTOR_ROLES
(
    ID         NUMBER(19) generated as identity
        primary key,
    CREATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    NAME       VARCHAR2(255 char)          not null
        constraint UQ_CR_NAME
            unique,
    UUID       VARCHAR2(255 char)          not null
        constraint UQ_CR_UUID
            unique
)
/

create table CONNECTOR_USERS
(
    ID                   NUMBER(19) generated as identity
        primary key,
    CREATED_AT           TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT           TIMESTAMP(9) WITH TIME ZONE not null,
    EMAIL                VARCHAR2(255 char),
    ENABLED              BOOLEAN,
    MUST_CHANGE_PASSWORD BOOLEAN,
    PASSWORD             VARCHAR2(255 char)          not null,
    USERNAME             VARCHAR2(255 char)          not null
        constraint UQ_CU_USERNAME
            unique,
    UUID                 VARCHAR2(255 char)          not null
        constraint UQ_CU_UUID
            unique
)
/

create table CONNECTOR_REFRESH_TOKENS
(
    ID         NUMBER(19) generated as identity
        primary key,
    CREATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    UPDATED_AT TIMESTAMP(9) WITH TIME ZONE not null,
    EXPIRES_AT TIMESTAMP(9) WITH TIME ZONE not null,
    REVOKED    BOOLEAN                     not null,
    REVOKED_AT TIMESTAMP(9) WITH TIME ZONE,
    TOKEN      VARCHAR2(255 char)          not null
        constraint UQ_CRT_TOKEN
            unique,
    USER_ID    NUMBER(19)                  not null
        constraint FK_CRT_USER
            references CONNECTOR_USERS
)
/

create index IDX_CRT_USER_ID
    on CONNECTOR_REFRESH_TOKENS (USER_ID)
/

create table CONNECTOR_USERS_ROLES
(
    USER_ID NUMBER(19) not null
        constraint FK_CUR_USER
            references CONNECTOR_USERS,
    ROLE_ID NUMBER(19) not null
        constraint FK_CUR_ROLE
            references CONNECTOR_ROLES,
    constraint UQ_CUR_USER_ROLE
        unique (USER_ID, ROLE_ID)
)
/
