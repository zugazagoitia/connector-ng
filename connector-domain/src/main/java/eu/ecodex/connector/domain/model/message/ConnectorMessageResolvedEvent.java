/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.domain.model.message;

import eu.ecodex.connector.domain.model.ConnectorPublishable;
import java.io.Serializable;

/**
 * Event published when a connector message has been resolved, that is, when it has reached a final
 * state (confirmed or rejected) and is no longer waiting for a transport or processing outcome.
 *
 * <p>Listeners can use it to react to the end of a message's lifecycle, for example to release
 * the resources that are no longer needed once the message is resolved. The event only carries the
 * message identifier; listeners load the message themselves if they need more.
 *
 * @param messageIdentifier the unique identifier of the resolved message (must not be null)
 */
public record ConnectorMessageResolvedEvent(String messageIdentifier)
    implements Serializable, ConnectorPublishable {
}
