#!/usr/bin/env bash
# Assembles ./work: keystores, the gateway configuration (connector push plugin + JMS plugin),
# the connector 6.x installation directory. Uses only the published images and the sample
# configuration of the connector-ng repository (docker/gateway).
set -euo pipefail
LAB="$(cd "$(dirname "$0")/.." && pwd)"
REPO="$(cd "$LAB/../.." && pwd)"
WORK="$LAB/work"
REG="${REGISTRY:-scm.ecodex.eu/docker}"
GW_IMAGE="$REG/gateway:${GATEWAY_TAG:-edge}"
C6_IMAGE="$REG/connector:${CONNECTOR6_TAG:-6.1.0}"
C7_IMAGE="$REG/connector-ng:${CONNECTOR7_TAG:-7.1.0}"

for i in "$GW_IMAGE" "$C6_IMAGE" "$C7_IMAGE"; do docker image inspect "$i" >/dev/null 2>&1 || docker pull -q "$i"; done

rm -rf "$WORK"
mkdir -p "$WORK"/{keystores,gateway/attachments,connector6/config/keystores,connector6/data,connector6/logs,connector7/config/keystores,connector7/data,connector7/temp,connector7/transaction-logs,migration,backend}

echo "== keystores"
# connector_blue / alice / bob keystores shipped with the 7.1.0 image
cid=$(docker create "$C7_IMAGE")
docker cp "$cid:/app/config/keystores/." "$WORK/keystores/" >/dev/null
docker cp "$cid:/app/config/log4j2.xml" "$WORK/connector7/config/log4j2.xml" >/dev/null
docker cp "$cid:/app/config/banner.txt" "$WORK/connector7/config/banner.txt" >/dev/null
docker cp "$cid:/app/config/application.properties" "$WORK/connector7/application.properties.template" >/dev/null
docker rm "$cid" >/dev/null
# gw_blue (gateway side of the connector plugin link) from the connector-ng gateway sample
cp "$REPO/docker/gateway/config/conf/domibus/keystores/gwlink-keystore.jks" "$WORK/keystores/"

# new stores get the password 123456 (keytool refuses passwords shorter than 6 characters)
kt() { docker run --rm --user 0 -v "$WORK/keystores:/ks" -w /ks --entrypoint keytool "$C6_IMAGE" "$@"; }
# gateway side: private key gw_blue + certificate connector_blue
kt -importkeystore -noprompt -srckeystore gwlink-keystore.jks -srcstorepass 12345 -srcalias gw_blue -srckeypass 12345 \
   -destkeystore gw-gwlink-keystore.p12 -deststoretype PKCS12 -deststorepass 123456 -destkeypass 123456 >/dev/null 2>&1
kt -exportcert -keystore connector-keystore.jks -storepass 12345 -alias connector_blue -file connector_blue.cer >/dev/null 2>&1
kt -importcert -noprompt -keystore gw-gwlink-keystore.p12 -storetype PKCS12 -storepass 123456 -alias connector_blue -file connector_blue.cer >/dev/null 2>&1
# connector side: private key connector_blue + certificate gw_blue
kt -importkeystore -noprompt -srckeystore connector-keystore.jks -srcstorepass 12345 -srcalias connector_blue -srckeypass 12345 \
   -destkeystore connector-gwlink.p12 -deststoretype PKCS12 -deststorepass 123456 -destkeypass 123456 >/dev/null 2>&1
kt -exportcert -keystore gwlink-keystore.jks -storepass 12345 -alias gw_blue -file gw_blue.cer >/dev/null 2>&1
kt -importcert -noprompt -keystore connector-gwlink.p12 -storetype PKCS12 -storepass 123456 -alias gw_blue -file gw_blue.cer >/dev/null 2>&1
kt -list -keystore gw-gwlink-keystore.p12 -storepass 123456 | grep -E "Entry|entries" || true
kt -list -keystore connector-gwlink.p12 -storepass 123456 | grep -E "Entry|entries" || true

echo "== gateway configuration"
cp -r "$REPO/docker/gateway/config/conf/domibus" "$WORK/gateway/domibus"
# the e-CODEX connector push plugin 4.6.0 that is part of the gateway image (6.x link) ...
cid=$(docker create "$GW_IMAGE")
docker cp "$cid:/app/conf/domibus/plugins/lib/." "$WORK/gateway/plugin-lib-from-image/" >/dev/null
docker rm "$cid" >/dev/null
cp "$WORK"/gateway/plugin-lib-from-image/domibus-connector-push-plugin-*.jar "$WORK/gateway/domibus/plugins/lib/"
rm -rf "$WORK/gateway/plugin-lib-from-image"
# ... next to the default JMS plugin of the connector-ng sample (7.x link)
# lab only: allow the admin REST API with the default password (P-Mode upload, message filter)
printf '\n# lab\ndomibus.passwordPolicy.checkDefaultPassword=false\n' >> "$WORK/gateway/domibus/domibus.properties"
cp "$LAB/gateway/dc-push-plugin.properties" "$WORK/gateway/domibus/plugins/config/"
cp "$WORK/keystores/gw-gwlink-keystore.p12" "$WORK/gateway/domibus/keystores/"
cp "$LAB/gateway/pmode-selfsend.xml" "$WORK/gateway/"
ls "$WORK/gateway/domibus/plugins/lib" "$WORK/gateway/domibus/plugins/config"

echo "== connector 6.x installation"
cp "$LAB/connector6/config/connector.properties" "$WORK/connector6/config/"
cp "$LAB/gateway/pmode-selfsend.xml" "$WORK/connector6/config/"
cp "$WORK"/keystores/{backend-keystore.jks,connector-keystore.jks,connector-truststore.jks,connector-gwlink.p12} "$WORK/connector6/config/keystores/"

echo "== connector 7.1.0 installation (configuration is generated after the migration)"
cp "$WORK"/keystores/{backend-keystore.jks,backend-truststore.jks,connector-keystore.jks,connector-truststore.jks} "$WORK/connector7/config/keystores/"
mkdir -p "$WORK/connector7/config/pmode"
cp "$LAB/gateway/pmode-selfsend.xml" "$WORK/connector7/config/pmode/"

# backend client key store: alice (signing) + connector_blue (encryption)
cp "$WORK/keystores/backend-keystore.jks" "$WORK/backend/"

# containers run as uid 1000
docker run --rm --user 0 -v "$WORK:/w" --entrypoint sh "$C6_IMAGE" -c \
  'chown -R 1000:1000 /w/gateway /w/connector6 /w/connector7 && chmod 700 /w/connector7/temp'
echo "prepared $WORK"
