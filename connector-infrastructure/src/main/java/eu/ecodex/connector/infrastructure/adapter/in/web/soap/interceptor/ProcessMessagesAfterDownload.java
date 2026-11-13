/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.in.web.soap.interceptor;

import eu.ecodex.connector.application.port.api.transport.ConnectorSetMessagesTransportStepToPull;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor responsible for processing all connector messages after they have been
 * downloaded.
 */
@Slf4j
public class ProcessMessagesAfterDownload extends AbstractPhaseInterceptor<Message> {
    private final String backendClient;
    private final ConnectorSetMessagesTransportStepToPull setTransportStepsToDownloadService;

    /**
     * Constructs a ProcessMessagesAfterDownload instance to process all connector messages after
     * they have been downloaded. This interceptor is executed in the post-invoke phase to update
     * the transport step status of connector messages for a specific backend client.
     *
     * @param backendClient                      the name of the backend client for which the
     *                                           connector messages are being processed
     * @param setTransportStepsToDownloadService the service responsible for updating the status of
     *                                           pending connector messages
     */
    public ProcessMessagesAfterDownload(
        String backendClient,
        ConnectorSetMessagesTransportStepToPull setTransportStepsToDownloadService) {
        super(Phase.POST_INVOKE);
        this.backendClient = backendClient;
        this.setTransportStepsToDownloadService = setTransportStepsToDownloadService;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        log.info(
            "Updating retrieved messages transport step status for the backend client [{}]",
            this.backendClient
        );
        try {
            setTransportStepsToDownloadService.execute(this.backendClient);
        } catch (Exception e) {
            log.error(
                "Failed to update the transport steps statuses for the backend client [{}]",
                this.backendClient,
                e
            );

            throw new Fault(e);
        }
    }
}
