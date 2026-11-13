/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.soap.backend;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

import eu.ecodex.connector.domain.transition.DomibusConnectorBackendWebService;
import eu.ecodex.connector.domain.transition.EmptyRequestType;
import eu.ecodex.connector.soap.AbstractSoapBackendServiceTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@DisplayName("ConnectorListPendingMessageIDsIT SOAP")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
public class ConnectorListPendingMessageIDsIT extends AbstractSoapBackendServiceTest {
    @LocalServerPort
    private int port;

    private DomibusConnectorBackendWebService soapClient;

    @BeforeEach
    void setUp() {
        soapClient = createClient(port);
    }

    @AfterEach
    void cleanUp() {
        cleanDb();
    }

    @Test
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
        "classpath:sql/message.sql",
        "classpath:sql/message-as4-properties.sql",
        "classpath:sql/message-transport-step.sql",
        "classpath:sql/message-transport-step-statuses.sql",
    })
    void should_retrieve_pending_message_identifiers() {
        var response = soapClient.listPendingMessageIds(new EmptyRequestType());

        assertThat(response).isNotNull();
        assertThat(response.getMessageTransportIds()).isNotNull();
        assertThat(response.getMessageTransportIds().size()).isEqualTo(1);
        assertThat(response.getMessageTransportIds().getFirst())
            .isEqualTo("b0f19c4c-ac3e-438c-9951-8e3a5211fed4@connector.ecodex.eu_backend_alice");
    }
}
