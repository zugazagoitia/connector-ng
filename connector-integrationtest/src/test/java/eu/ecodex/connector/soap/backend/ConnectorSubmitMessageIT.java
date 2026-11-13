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

import eu.ecodex.connector.SoapMessageSubmitTestFixtures;
import eu.ecodex.connector.domain.transition.DomibusConnectorBackendWebService;
import eu.ecodex.connector.soap.AbstractSoapBackendServiceTest;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.jdbc.Sql;

@DisplayName("ConnectorSubmitMessageIT SOAP")
@Sql(
    statements = "DELETE FROM connector_business_domains WHERE id > 0",
    executionPhase = Sql.ExecutionPhase.BEFORE_TEST_CLASS
)
public class ConnectorSubmitMessageIT extends AbstractSoapBackendServiceTest {
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
    @WithReferenceData
    void should_submit_outbound_message_via_soap() {
        var message = SoapMessageSubmitTestFixtures.createBackendToConnectorMessage();

        var ack = soapClient.submitMessage(message);

        assertThat(ack).isNotNull();
        assertThat(ack.isResult()).isTrue();
        assertThat(ack.getMessageId()).isNotBlank();
    }

    @Test
    @WithReferenceData
    void should_submit_soap_outbound_message_without_attachment() {
        var message =
            SoapMessageSubmitTestFixtures.createBackendToConnectorMessageWithoutAttachment();

        var ack = soapClient.submitMessage(message);

        assertThat(ack).isNotNull();
        assertThat(ack.isResult()).isTrue();
        assertThat(ack.getMessageId()).isNotBlank();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @Sql({
        "classpath:sql/business-domain.sql",
        "classpath:sql/processing-mode.sql",
        "classpath:sql/processing-mode-truststores.sql",
        "classpath:sql/processing-mode-party.sql",
        "classpath:sql/processing-mode-service.sql",
        "classpath:sql/processing-mode-action.sql",
    })
    private @interface WithReferenceData {
    }
}
