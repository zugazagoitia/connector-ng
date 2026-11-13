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

import eu.ecodex.connector.application.port.api.transport.ConnectorAckMessageTransportStep;
import eu.ecodex.connector.application.port.api.transport.command.UpdateMessageTransportCommand;
import eu.ecodex.connector.domain.model.message.ConnectorMessage;
import eu.ecodex.connector.domain.model.message.transport.ConnectorMessageTransportStatus;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;

/**
 * CXF interceptor responsible for processing a connector message after it has been downloaded.
 */
@Slf4j
public class ProcessMessageAfterDownload extends AbstractPhaseInterceptor<Message> {
    private final ConnectorMessage connectorMessage;
    private final String linkPartnerName;
    private final ConnectorAckMessageTransportStep ackMessageTransportStep;

    /**
     * Constructs a ProcessMessageAfterDownload instance which processes a message after it has been
     * downloaded. This class is intended to handle the post-invoke phase of message processing.
     *
     * @param connectorMessage        the connector message that contains details of the message to
     *                                be processed
     * @param linkPartnerName         the name of the link partner the message was downloaded from
     *                                or for (must not be null)
     * @param ackMessageTransportStep the service responsible for updating the status of the message
     *                                transport step
     */
    public ProcessMessageAfterDownload(
        ConnectorMessage connectorMessage,
        String linkPartnerName,
        ConnectorAckMessageTransportStep ackMessageTransportStep) {
        super(Phase.POST_INVOKE);

        this.connectorMessage = connectorMessage;
        this.linkPartnerName = linkPartnerName;
        this.ackMessageTransportStep = ackMessageTransportStep;
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        log.info(
            "Updating retrieve message transport step status for the message [{}]",
            this.connectorMessage.identifier()
        );

        try {
            var command = UpdateMessageTransportCommand
                .builder()
                .remoteMessageIdentifier(null)
                .status(ConnectorMessageTransportStatus.PULLED)
                .linkPartnerName(this.linkPartnerName)
                .errors(null)
                .build();
            ackMessageTransportStep.execute(this.connectorMessage.identifier(), command);
        } catch (Exception e) {
            log.error(
                "Failed to update the transport step status for the me message [{}]",
                this.connectorMessage.identifier(), e
            );
            throw new Fault(e);
        }
    }
}
