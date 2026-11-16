package lab;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import eu.ecodex.connector.domain.transition.DomibusConnectorActionType;
import eu.ecodex.connector.domain.transition.DomibusConnectorConfirmationType;
import eu.ecodex.connector.domain.transition.DomibusConnectorDocumentAESType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageConfirmationType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageContentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDetailsType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageDocumentType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageResponseType;
import eu.ecodex.connector.domain.transition.DomibusConnectorMessageType;
import eu.ecodex.connector.domain.transition.DomibusConnectorPartyType;
import eu.ecodex.connector.domain.transition.DomibusConnectorServiceType;
import eu.ecodex.connector.ws.backend.webservice.DomibusConnectorBackendWSService;
import eu.ecodex.connector.ws.backend.webservice.DomibusConnectorBackendWebService;
import eu.ecodex.connector.ws.backend.webservice.EmptyRequestType;
import eu.ecodex.connector.ws.backend.webservice.GetMessageByIdRequest;
import jakarta.activation.DataHandler;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean;
import org.apache.cxf.ws.policy.WSPolicyFeature;
import org.apache.wss4j.common.ext.WSPasswordCallback;
import org.w3c.dom.Element;

/**
 * A minimal national backend application that sends every message to itself through the connector
 * (connector -> gateway -> same gateway -> connector -> this backend).
 *
 * <p>It uses the connector backend SOAP API in pull mode, signed and encrypted with WS-Security as
 * {@code cn=alice}. The API is the same in connector 6.x and 7.x, so the same binary is used before
 * and after the upgrade. Everything it sends and receives is kept in a JSON ledger, which is what the
 * lab checks at the end: every message sent must come back exactly once with the same bytes, and
 * every message must end with a RETRIEVAL evidence.
 *
 * <p>Commands (environment: CONNECTOR_URL, KEYSTORE, LEDGER, PHASE):
 * <pre>
 *   submit TAG COUNT                 submit COUNT business messages to ourselves
 *   pull                             download and acknowledge everything pending
 *   trigger TYPE TAG                 send an evidence trigger (e.g. RETRIEVAL) for every received message of TAG
 *   await-received TAGS SECONDS      pull until every message of TAGS (comma separated) came back
 *   await-evidence TYPE TAGS SECONDS pull until every sent message of TAGS has an evidence of TYPE
 *   verify TAGS                      check the ledger; exit code 1 on a lost, duplicated or altered message
 *   report                           print the ledger summary
 * </pre>
 */
public final class SelfSendBackend {

    private static final String PARTY_ID = "BL";
    private static final String PARTY_ID_TYPE = "urn:oasis:names:tc:ebcore:partyid-type:ecodex";
    private static final String ROLE = "GW";
    private static final Pattern TOKEN = Pattern.compile("<token>([^<]+)</token>");

    private final ObjectMapper json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    private final Path ledgerFile;
    private final ObjectNode ledger;
    private final String phase;
    private final String url;
    private final String keystore;
    private DomibusConnectorBackendWebService port;

    private SelfSendBackend() throws Exception {
        this.url = env("CONNECTOR_URL", "http://localhost:9081/services/backend");
        this.keystore = env("KEYSTORE", "backend-keystore.jks");
        this.phase = env("PHASE", "6x");
        this.ledgerFile = Path.of(env("LEDGER", "ledger.json"));
        if (Files.exists(ledgerFile)) {
            this.ledger = (ObjectNode) json.readTree(ledgerFile.toFile());
        } else {
            this.ledger = json.createObjectNode();
            ledger.putObject("sent");
            ledger.putObject("received");
            ledger.putArray("evidences");
            ledger.putObject("triggers");
            ledger.putArray("events");
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("usage: submit|pull|trigger|await-received|await-evidence|verify|report ...");
            System.exit(64);
        }
        var app = new SelfSendBackend();
        int rc = 0;
        try {
            switch (args[0]) {
                case "submit" -> app.submit(args[1], Integer.parseInt(args[2]));
                case "pull" -> app.pull();
                case "trigger" -> app.trigger(args[1], args[2]);
                case "await-received" -> rc = app.awaitReceived(tags(args[1]), Integer.parseInt(args[2]));
                case "await-evidence" -> rc = app.awaitEvidence(args[1], tags(args[2]), Integer.parseInt(args[3]));
                case "verify" -> rc = app.verify(tags(args[1]));
                case "report" -> app.report();
                default -> {
                    System.err.println("unknown command " + args[0]);
                    rc = 64;
                }
            }
        } finally {
            app.save();
        }
        System.exit(rc);
    }

    // ------------------------------------------------------------------ commands

    private void submit(String tag, int count) throws Exception {
        var random = new Random();
        for (int i = 1; i <= count; i++) {
            var token = tag + "-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
            var backendId = "backend-" + token;
            var xml = ("<?xml version=\"1.0\" encoding=\"UTF-8\"?><selfSendForm><token>%s</token>"
                + "<tag>%s</tag><phase>%s</phase><created>%s</created></selfSendForm>")
                .formatted(token, tag, phase, Instant.now());
            var pdf = new byte[2048 + random.nextInt(4096)];
            random.nextBytes(pdf);
            var header = ("%PDF-1.4 lab document " + token + "\n").getBytes(StandardCharsets.US_ASCII);
            System.arraycopy(header, 0, pdf, 0, header.length);

            var msg = new DomibusConnectorMessageType();
            msg.setMessageDetails(details(backendId, null));
            var content = new DomibusConnectorMessageContentType();
            content.setXmlContent(new StreamSource(new java.io.ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))));
            var doc = new DomibusConnectorMessageDocumentType();
            doc.setDocument(new DataHandler(new BytesDataSource(pdf, "application/octet-stream")));
            doc.setDocumentName("document-" + token + ".pdf");
            doc.setAesType(DomibusConnectorDocumentAESType.AUTHENTICATION_BASED);
            content.setDocument(doc);
            msg.setMessageContent(content);

            var ack = port().submitMessage(msg);
            var sent = ((ObjectNode) ledger.get("sent")).putObject(backendId);
            sent.put("tag", tag);
            sent.put("token", token);
            sent.put("phase", phase);
            sent.put("submittedAt", Instant.now().toString());
            sent.put("xmlFingerprint", fingerprint(xml));
            sent.put("documentSha256", sha256(pdf));
            sent.put("documentSize", pdf.length);
            sent.put("accepted", ack.isResult());
            sent.put("connectorMessageId", ack.getMessageId());
            sent.put("resultMessage", ack.getResultMessage());
            log("submitted %s -> result=%s connectorId=%s %s", backendId, ack.isResult(), ack.getMessageId(),
                ack.getResultMessage() == null ? "" : ack.getResultMessage());
            if (!ack.isResult()) {
                throw new IllegalStateException("submission refused: " + ack.getResultMessage());
            }
        }
    }

    /** Downloads and acknowledges everything that is pending. Returns the number of messages fetched. */
    private int pull() throws Exception {
        var ids = port().listPendingMessageIds(new EmptyRequestType()).getMessageTransportIds();
        int n = 0;
        for (var transportId : ids) {
            // A message whose acknowledgement failed is offered again: record it once, but try to acknowledge it again.
            var handled = (ObjectNode) ledger.with("handledTransports");
            try {
                var req = new GetMessageByIdRequest();
                req.setMessageTransportId(transportId);
                var msg = port().getMessageById(req);
                if (msg == null) {
                    log("pending transport %s returned no message", transportId);
                    continue;
                }
                String assigned = handled.has(transportId) ? handled.get(transportId).asText()
                    : handleIncoming(transportId, msg);
                handled.put(transportId, assigned);
                var ack = new DomibusConnectorMessageResponseType();
                ack.setResponseForMessageId(transportId);
                ack.setResult(true);
                ack.setResultMessage("processed by lab backend (" + phase + ")");
                ack.setAssignedMessageId(assigned);
                port().acknowledgeMessage(ack);
                n++;
            } catch (jakarta.xml.ws.WebServiceException e) {
                // a real backend would retry later; the lab records the failure and continues with the next message
                var failed = (ObjectNode) ledger.with("failedAcks");
                if (!failed.has(transportId)) {
                    log("FAILED to download/acknowledge transport %s: %s", transportId, e.getMessage());
                }
                failed.put(transportId, phase + ": " + e.getMessage());
            }
        }
        if (n > 0) {
            save();
        }
        return n;
    }

    private String handleIncoming(String transportId, DomibusConnectorMessageType msg) throws Exception {
        var d = msg.getMessageDetails();
        var content = msg.getMessageContent();
        if (content != null && content.getXmlContent() != null) {
            var xml = readSource(content.getXmlContent());
            var m = TOKEN.matcher(new String(xml, StandardCharsets.UTF_8));
            var token = m.find() ? m.group(1) : "unknown-" + transportId;
            byte[] document = null;
            if (content.getDocument() != null && content.getDocument().getDocument() != null) {
                try (InputStream in = content.getDocument().getDocument().getInputStream()) {
                    document = in.readAllBytes();
                }
            }
            var list = (ArrayNode) ledger.get("received").withArray(token);
            var rec = list.addObject();
            rec.put("phase", phase);
            rec.put("transportId", transportId);
            rec.put("receivedAt", Instant.now().toString());
            rec.put("ebmsMessageId", d.getEbmsMessageId());
            rec.put("backendMessageIdInDetails", d.getBackendMessageId());
            rec.put("conversationId", d.getConversationId());
            rec.put("xmlFingerprint", fingerprint(new String(xml, StandardCharsets.UTF_8)));
            rec.put("documentSha256", document == null ? null : sha256(document));
            rec.put("documentName", content.getDocument() == null ? null : content.getDocument().getDocumentName());
            rec.put("confirmations", String.join(",", confirmationTypes(msg)));
            var assigned = "inbound-" + token;
            rec.put("assignedBackendId", assigned);
            log("received business message token=%s ebms=%s transport=%s (copy #%d)", token, d.getEbmsMessageId(),
                transportId, list.size());
            return assigned;
        }
        // an evidence (confirmation) message for one of our messages
        for (var c : msg.getMessageConfirmations()) {
            var ev = ((ArrayNode) ledger.get("evidences")).addObject();
            ev.put("phase", phase);
            ev.put("type", c.getConfirmationType().value());
            ev.put("transportId", transportId);
            ev.put("receivedAt", Instant.now().toString());
            ev.put("refToMessageId", d.getRefToMessageId());
            ev.put("ebmsMessageId", d.getEbmsMessageId());
            ev.put("backendMessageId", d.getBackendMessageId());
            ev.put("hasEvidenceXml", c.getConfirmation() != null);
            var ref = d.getRefToMessageId();
            if (ref != null && ref.startsWith("inbound-")) {
                // evidence the connector generated for the copy we received (its id is the one we assigned at ack)
                ev.put("forReceivedCopy", ref.substring("inbound-".length()));
                log("received evidence %s for our received copy %s", c.getConfirmationType().value(), ref);
            } else {
                var owner = ownerOf(ref);
                ev.put("forSentMessage", owner);
                log("received evidence %s ref=%s -> %s", c.getConfirmationType().value(), ref, owner);
            }
        }
        return "evidence-" + transportId;
    }

    /** Sends an evidence trigger for every received business message of TAG that has none of this type yet. */
    private void trigger(String type, String tag) throws Exception {
        var triggers = (ObjectNode) ledger.get("triggers");
        var received = ledger.get("received");
        for (var it = received.fieldNames(); it.hasNext(); ) {
            var token = it.next();
            if (!token.startsWith(tag + "-")) {
                continue;
            }
            var key = token + "|" + type;
            if (triggers.has(key) && triggers.get(key).get("result").asBoolean()) {
                continue;
            }
            var copy = received.get(token).get(0);
            var ebmsId = copy.get("ebmsMessageId").asText();
            var msg = new DomibusConnectorMessageType();
            var details = details(null, ebmsId);
            details.getAction().setAction(switch (type) {
                case "DELIVERY", "NON_DELIVERY" -> "DeliveryNonDeliveryToRecipient";
                default -> "RetrievalNonRetrievalToRecipient";
            });
            msg.setMessageDetails(details);
            var conf = new DomibusConnectorMessageConfirmationType();
            conf.setConfirmationType(DomibusConnectorConfirmationType.fromValue(type));
            msg.getMessageConfirmations().add(conf);
            var ack = port().submitMessage(msg);
            var t = triggers.putObject(key);
            t.put("phase", phase);
            t.put("refToMessageId", ebmsId);
            t.put("result", ack.isResult());
            t.put("resultMessage", ack.getResultMessage());
            t.put("at", Instant.now().toString());
            log("trigger %s for %s (ebms %s): result=%s %s", type, token, ebmsId, ack.isResult(),
                ack.getResultMessage() == null ? "" : ack.getResultMessage());
        }
    }

    private int awaitReceived(Set<String> tags, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (true) {
            pull();
            var missing = sentOf(tags).stream().filter(s -> !ledger.get("received").has(s.get("token").asText())).toList();
            if (missing.isEmpty()) {
                log("all messages of %s came back", tags);
                return 0;
            }
            if (System.currentTimeMillis() > end) {
                log("TIMEOUT: %d messages of %s did not come back: %s", missing.size(), tags,
                    missing.stream().map(s -> s.get("token").asText()).toList());
                return 2;
            }
            Thread.sleep(5000);
        }
    }

    private int awaitEvidence(String type, Set<String> tags, int seconds) throws Exception {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (true) {
            pull();
            var missing = new ArrayList<String>();
            for (var e : sentEntries(tags).entrySet()) {
                if (!hasEvidence(e.getKey(), type)) {
                    missing.add(e.getKey());
                }
            }
            if (missing.isEmpty()) {
                log("every message of %s has a %s evidence", tags, type);
                return 0;
            }
            if (System.currentTimeMillis() > end) {
                log("TIMEOUT: %d messages of %s have no %s evidence: %s", missing.size(), tags, type, missing);
                return 2;
            }
            Thread.sleep(5000);
        }
    }

    private int verify(Set<String> tags) {
        int problems = 0;
        var lines = new ArrayList<String>();
        lines.add(String.format("%-40s %-6s %-6s %-9s %-8s %s", "sent message", "sent", "copies", "bytes", "phase-in",
                                "evidences received by the backend"));
        for (var e : sentEntries(tags).entrySet()) {
            var s = e.getValue();
            var token = s.get("token").asText();
            var copies = ledger.get("received").has(token) ? ledger.get("received").get(token).size() : 0;
            String bytes = "-";
            String phaseIn = "-";
            if (copies > 0) {
                var c = ledger.get("received").get(token).get(0);
                phaseIn = c.get("phase").asText();
                boolean sameXml = s.get("xmlFingerprint").asText().equals(c.get("xmlFingerprint").asText());
                boolean noDoc = c.path("documentSha256").isNull() || c.path("documentSha256").isMissingNode();
                boolean sameDoc = s.get("documentSha256").asText().equals(c.path("documentSha256").asText());
                bytes = sameXml && sameDoc ? "identical" : !sameXml ? "XML-DIFF" : noDoc ? "NO-DOC" : "DOC-DIFF";
                if (!(sameXml && sameDoc)) {
                    problems++;
                }
            }
            if (copies != 1) {
                problems++;
            }
            var evs = evidenceTypes(e.getKey());
            if (!evs.contains("RETRIEVAL")) {
                problems++;
            }
            lines.add(String.format("%-40s %-6s %-6s %-9s %-8s %s", e.getKey(), s.get("phase").asText(), copies,
                                    bytes, phaseIn, evs));
        }
        lines.forEach(System.out::println);
        System.out.printf("%n%d sent messages checked, %d problems (a problem is: not exactly one copy received, "
                              + "different bytes, or no RETRIEVAL evidence)%n", sentEntries(tags).size(), problems);
        return problems == 0 ? 0 : 1;
    }

    private void report() throws Exception {
        System.out.println(json.writeValueAsString(ledger));
    }

    // ------------------------------------------------------------------ helpers

    private DomibusConnectorMessageDetailsType details(String backendId, String refToMessageId) {
        var d = new DomibusConnectorMessageDetailsType();
        d.setBackendMessageId(backendId);
        d.setRefToMessageId(refToMessageId);
        d.setOriginalSender("lab-backend-alice");
        d.setFinalRecipient("lab-backend-alice");
        var service = new DomibusConnectorServiceType();
        service.setService("EPO");
        service.setServiceType("urn:e-codex:services:");
        d.setService(service);
        var action = new DomibusConnectorActionType();
        action.setAction(refToMessageId == null ? "Form_A" : "RetrievalNonRetrievalToRecipient");
        d.setAction(action);
        d.setFromParty(party());
        d.setToParty(party());
        return d;
    }

    private static DomibusConnectorPartyType party() {
        var p = new DomibusConnectorPartyType();
        p.setPartyId(PARTY_ID);
        p.setPartyIdType(PARTY_ID_TYPE);
        p.setRole(ROLE);
        return p;
    }

    /** Which sent message (backend id) an evidence refers to: by backend id, or by the ebMS id of its copy. */
    private String ownerOf(String ref) {
        if (ref == null) {
            return null;
        }
        if (ledger.get("sent").has(ref)) {
            return ref;
        }
        var stripped = ref.replaceFirst("^backend-", "");
        for (var it = ledger.get("sent").fields(); it.hasNext(); ) {
            var e = it.next();
            var token = e.getValue().get("token").asText();
            if (stripped.equals(token) || ref.equals(e.getValue().path("connectorMessageId").asText(null))) {
                return e.getKey();
            }
            var copies = ledger.get("received").get(token);
            if (copies != null) {
                for (var c : copies) {
                    if (ref.equals(c.path("ebmsMessageId").asText(null))) {
                        return e.getKey();
                    }
                }
            }
        }
        return "unknown";
    }

    private boolean hasEvidence(String backendId, String type) {
        return evidenceTypes(backendId).contains(type);
    }

    private List<String> evidenceTypes(String backendId) {
        var out = new ArrayList<String>();
        for (var ev : ledger.get("evidences")) {
            if (ev.has("forReceivedCopy")) {
                continue;
            }
            var owner = ev.path("forSentMessage").asText(null);
            if (owner == null || "unknown".equals(owner)) {
                owner = ownerOf(ev.path("refToMessageId").asText(null));
            }
            if (backendId.equals(owner)) {
                out.add(ev.get("type").asText() + "@" + ev.get("phase").asText());
            }
        }
        return out.stream().map(s -> s.substring(0, s.indexOf('@'))).toList();
    }

    private List<com.fasterxml.jackson.databind.JsonNode> sentOf(Set<String> tags) {
        return new ArrayList<>(sentEntries(tags).values());
    }

    private Map<String, com.fasterxml.jackson.databind.JsonNode> sentEntries(Set<String> tags) {
        var out = new LinkedHashMap<String, com.fasterxml.jackson.databind.JsonNode>();
        for (var it = ledger.get("sent").fields(); it.hasNext(); ) {
            var e = it.next();
            if (tags.contains("*") || tags.contains(e.getValue().get("tag").asText())) {
                out.put(e.getKey(), e.getValue());
            }
        }
        return out;
    }

    private static List<String> confirmationTypes(DomibusConnectorMessageType msg) {
        return msg.getMessageConfirmations().stream().map(c -> c.getConfirmationType().value()).toList();
    }

    private void log(String fmt, Object... args) {
        var line = "[backend " + phase + "] " + fmt.formatted(args);
        System.out.println(line);
        ((ArrayNode) ledger.get("events")).add(Instant.now() + " " + line);
    }

    private void save() throws Exception {
        json.writeValue(ledgerFile.toFile(), ledger);
    }

    private static Set<String> tags(String csv) {
        return Set.of(csv.split(","));
    }

    private static String env(String name, String def) {
        var v = System.getenv(name);
        return v == null || v.isBlank() ? def : v;
    }

    /** The XML is re-serialised by the connector, so it is compared by the values of its elements. */
    private static String fingerprint(String xml) {
        var m = Pattern.compile("<(token|tag|phase|created)>([^<]*)</\\1>").matcher(xml);
        var sb = new StringBuilder();
        while (m.find()) {
            sb.append(m.group(1)).append('=').append(m.group(2)).append(';');
        }
        return sb.toString();
    }

    private static String sha256(byte[] data) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static byte[] readSource(Source source) throws Exception {
        if (source instanceof StreamSource ss && ss.getInputStream() != null) {
            return ss.getInputStream().readAllBytes();
        }
        var out = new ByteArrayOutputStream();
        var w = new StringWriter();
        TransformerFactory.newInstance().newTransformer().transform(source, new StreamResult(w));
        out.write(w.toString().getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private record BytesDataSource(byte[] data, String type) implements jakarta.activation.DataSource {
        @Override public InputStream getInputStream() { return new java.io.ByteArrayInputStream(data); }
        @Override public java.io.OutputStream getOutputStream() { throw new UnsupportedOperationException(); }
        @Override public String getContentType() { return type; }
        @Override public String getName() { return "document"; }
    }

    // ------------------------------------------------------------------ SOAP client with WS-Security

    private DomibusConnectorBackendWebService port() throws Exception {
        if (port != null) {
            return port;
        }
        var factory = new JaxWsProxyFactoryBean();
        factory.setServiceClass(DomibusConnectorBackendWebService.class);
        factory.setAddress(url);
        factory.setWsdlURL(SelfSendBackend.class.getResource("/wsdl/DomibusConnectorBackendWebService.wsdl").toString());
        factory.setServiceName(DomibusConnectorBackendWSService.SERVICE);
        factory.setEndpointName(DomibusConnectorBackendWSService.DomibusConnectorBackendWebService);

        var dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        Element policy;
        try (var in = SelfSendBackend.class.getResourceAsStream("/wsdl/backend.policy.xml")) {
            policy = dbf.newDocumentBuilder().parse(in).getDocumentElement();
        }
        var policyFeature = new WSPolicyFeature();
        policyFeature.setPolicyElements(List.of(policy));
        factory.getFeatures().add(policyFeature);

        var crypto = new Properties();
        crypto.put("org.apache.wss4j.crypto.provider", "org.apache.wss4j.common.crypto.Merlin");
        crypto.put("org.apache.wss4j.crypto.merlin.keystore.type", "JKS");
        crypto.put("org.apache.wss4j.crypto.merlin.keystore.file", keystore);
        crypto.put("org.apache.wss4j.crypto.merlin.keystore.password", "12345");
        crypto.put("org.apache.wss4j.crypto.merlin.keystore.alias", "alice");
        crypto.put("org.apache.wss4j.crypto.merlin.truststore.type", "JKS");
        crypto.put("org.apache.wss4j.crypto.merlin.truststore.file", keystore);
        crypto.put("org.apache.wss4j.crypto.merlin.truststore.password", "12345");

        Map<String, Object> props = new java.util.HashMap<>();
        props.put("mtom-enabled", true);
        props.put("security.signature.properties", crypto);
        props.put("security.signature.username", "alice");
        props.put("security.encryption.properties", crypto);
        props.put("security.encryption.username", "connector_blue");
        props.put("security.callback-handler", (CallbackHandler) callbacks -> {
            for (Callback cb : callbacks) {
                if (cb instanceof WSPasswordCallback pc) {
                    pc.setPassword("12345");
                }
            }
        });
        factory.setProperties(props);
        port = (DomibusConnectorBackendWebService) factory.create();
        var client = org.apache.cxf.frontend.ClientProxy.getClient(port);
        var http = (org.apache.cxf.transport.http.HTTPConduit) client.getConduit();
        var policyHttp = new org.apache.cxf.transports.http.configuration.HTTPClientPolicy();
        policyHttp.setReceiveTimeout(120_000);
        policyHttp.setConnectionTimeout(10_000);
        http.setClient(policyHttp);
        return port;
    }
}
