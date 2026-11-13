/*
 * Copyright 2026 European Union Agency for the Operational Management of Large-Scale IT Systems
 * in the Area of Freedom, Security and Justice (eu-LISA)
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the
 * European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy at: https://joinup.ec.europa.eu/software/page/eupl
 */

package eu.ecodex.connector.infrastructure.adapter.out.soap;

import eu.ecodex.connector.infrastructure.adapter.out.soap.exception.ConnectorWsPolicyLoaderException;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import lombok.extern.slf4j.Slf4j;
import org.apache.cxf.staxutils.StaxUtils;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.springframework.core.io.Resource;

/**
 * The WsPolicyLoader class is responsible for loading WS-Policy definitions from a specified path
 * and creating corresponding {@link WSPolicyFeature} objects for use in web service clients.
 */
@Slf4j
public class ConnectorWsPolicyLoader {
    private final Resource wsPolicyPath;

    public ConnectorWsPolicyLoader(Resource wsPolicyPath) {
        this.wsPolicyPath = wsPolicyPath;
    }

    /**
     * Loads a WS-Policy definition from the specified path and constructs a {@link WSPolicyFeature}
     * object configured with the resulting policy elements.
     *
     * @return a {@link WSPolicyFeature} object encapsulating the loaded WS-Policy definition, with
     *     its corresponding elements set and enabled.
     *
     * @throws UncheckedIOException             if the policy file cannot be accessed or read.
     * @throws ConnectorWsPolicyLoaderException if parsing the policy file fails, or the parsed
     *                                          document lacks a valid root element.
     */
    public WSPolicyFeature loadPolicyFeature() {
        log.debug("Loading WS policy from path: {}", wsPolicyPath);

        try (var is = wsPolicyPath.getInputStream()) {
            var element = StaxUtils.read(is).getDocumentElement();

            if (element == null) {
                throw new ConnectorWsPolicyLoaderException(
                    "Policy file parsed but produced no document element: " + wsPolicyPath,
                    null
                );
            }

            log.debug("Loaded policy element: {}", element);

            var policyFeature = new WSPolicyFeature();
            policyFeature.setEnabled(true);
            policyFeature.setPolicyElements(List.of(element));
            return policyFeature;
        } catch (IOException e) {
            throw new UncheckedIOException(
                "WS policy '" + wsPolicyPath.getDescription() + "' cannot be read", e
            );
        } catch (XMLStreamException e) {
            throw new ConnectorWsPolicyLoaderException(
                "Cannot parse WS policy '" + wsPolicyPath + "'",
                e
            );
        }
    }
}
