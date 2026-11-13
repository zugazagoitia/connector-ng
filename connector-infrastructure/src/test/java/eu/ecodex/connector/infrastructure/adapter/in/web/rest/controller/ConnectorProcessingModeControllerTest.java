/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import eu.ecodex.connector.ActionTestFixtures;
import eu.ecodex.connector.PartyTestFixtures;
import eu.ecodex.connector.ServiceTestFixtures;
import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeActions;
import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeParties;
import eu.ecodex.connector.application.port.api.pmode.ConnectorListProcessingModeServices;
import eu.ecodex.connector.domain.ConnectorDefaults;
import eu.ecodex.connector.infrastructure.adapter.in.web.rest.controller.pmode.ConnectorProcessingModeController;
import eu.ecodex.connector.infrastructure.config.property.c2ctest.Connector2ConnectorTestMessageProperties;
import eu.ecodex.connector.infrastructure.config.property.c2ctest.TestServiceProperties;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@DisplayName("ConnectorProcessingModeController")
@WebMvcTest(ConnectorProcessingModeController.class)
public class ConnectorProcessingModeControllerTest extends AbstractWebMvcTest {
    private static final String SERVICE_URL = "/api/v1/processing-modes/%s/services";
    private static final String ACTION_URL = "/api/v1/processing-modes/%s/actions";
    private static final String PARTY_URL = "/api/v1/processing-modes/%s/parties";
    private static final String BUSINESS_DOMAIN = "default_business_domain";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ConnectorListProcessingModeServices listProcessingModeServices;
    @MockitoBean
    private ConnectorListProcessingModeActions listProcessingModeActions;
    @MockitoBean
    private ConnectorListProcessingModeParties listProcessingModeParties;
    @MockitoBean
    Connector2ConnectorTestMessageProperties testMessageProperties;

    @Test
    void should_return_200_with_the_services() throws Exception {
        when(listProcessingModeServices.execute(any()))
            .thenReturn(List.of(ServiceTestFixtures.createService()));
        when(testMessageProperties.service()).thenReturn(new TestServiceProperties(
            ConnectorDefaults.DEFAULT_TEST_SERVICE_NAME,
            ConnectorDefaults.DEFAULT_TEST_SERVICE_TYPE
        ));

        mockMvc.perform(get(SERVICE_URL.formatted(BUSINESS_DOMAIN))
                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$[0].name").value("Connector-TEST"))
               .andExpect(jsonPath("$[0].type").value("urn:e-codex:services:"));
    }

    @Test
    void should_return_200_with_the_actions() throws Exception {
        when(listProcessingModeActions.execute(any()))
            .thenReturn(List.of(ActionTestFixtures.createAction()));
        when(testMessageProperties.action()).thenReturn(ConnectorDefaults.DEFAULT_TEST_ACTION_NAME);

        mockMvc.perform(get(ACTION_URL.formatted(BUSINESS_DOMAIN))
                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$[0].name").value("ConTest_Form"));
    }

    @Test
    void should_return_200_with_the_parties() throws Exception {
        when(listProcessingModeParties.execute(any()))
            .thenReturn(List.of(PartyTestFixtures.createToParty()));

        mockMvc.perform(get(PARTY_URL.formatted(BUSINESS_DOMAIN))
                            .contentType(MediaType.APPLICATION_JSON))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$[0].name").value("service_red_ecodex"))
               .andExpect(jsonPath("$[0].identifier").value("RE"))
               .andExpect(jsonPath("$[0].identifierType")
                              .value("urn:oasis:names:tc:ebcore:partyid-type:ecodex"))
               .andExpect(jsonPath("$[0].role").value("GW"))
               .andExpect(jsonPath("$[0].roleType").value("RESPONDER"));
    }
}
