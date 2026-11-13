FROM gradle:9-jdk21-alpine AS build
WORKDIR /app
COPY . .
RUN gradle clean build -x test -x :connector-integrationtest:integrationTest -x integrationTest

FROM eclipse-temurin:21-jre-jammy

LABEL maintainer="e-codex@eulisa.europa.eu"
LABEL description="e-CODEX connector"

ARG USERNAME=connector
ARG USER_UID=1000
ARG USER_GID=${USER_UID}
ARG BUILD_OUTPUT_FOLDER=/app/connector-distribution/build/connector-distribution/standalone

ARG APP_FOLDER=/app

WORKDIR ${APP_FOLDER}

RUN groupadd -g ${USER_GID} ${USERNAME} \
    && useradd -u ${USER_UID} -g ${USER_GID} -m ${USERNAME} \
    && mkdir -p data temp transaction-logs \
    && chown -R ${USERNAME}:${USERNAME} ${APP_FOLDER} \
    && chmod 700 ${APP_FOLDER}/temp

COPY --from=build --chown=${USERNAME}:${USERNAME} ${BUILD_OUTPUT_FOLDER}/bin/ ${APP_FOLDER}/bin/
COPY --from=build --chown=${USERNAME}:${USERNAME} ${BUILD_OUTPUT_FOLDER}/config/ ${APP_FOLDER}/config/
COPY --from=build --chown=${USERNAME}:${USERNAME} ${BUILD_OUTPUT_FOLDER}/start.sh ${APP_FOLDER}/

RUN chmod +x /app/start.sh

ENV TEMP_DIR_LOCATION=${APP_FOLDER}/temp

USER $USERNAME

EXPOSE 30000

ENTRYPOINT ["/app/start.sh"]
