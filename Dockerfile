ARG BUILD_BASE_IMAGE=docker.io/library/maven:3-eclipse-temurin-11
ARG RUNTIME_BASE_IMAGE=docker.io/library/eclipse-temurin:11-jre-jammy

# Stage 1: Build
FROM ${BUILD_BASE_IMAGE} AS build

WORKDIR /build

COPY pom.xml .

RUN mvn --batch-mode dependency:go-offline

COPY src src

RUN mvn --batch-mode --activate-profiles executable verify \
 && mv target/dash-listener-*.jar target/dash-listener.jar

# Stage 2: Runtime
FROM ${RUNTIME_BASE_IMAGE}

# Install dependencies
RUN apt-get update \
 && apt-get install --no-install-recommends -y \
        ca-certificates \
        fontconfig \
        locales \
        locales-all \
        libpcap-dev \
        netbase \
 && rm -rf /var/lib/apt/lists/* \
 && ln -s -f /bin/true /usr/bin/chfn

# Set variables and locales
ENV APPDIR="/dash" \
    EXTRA_JAVA_OPTS="" \
    LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8 \
    DASH_CONFIG="conf/config.properties"

# Set working directory
WORKDIR ${APPDIR}

# Install dash-listener
COPY --from=build /build/target/dash-listener.jar ${APPDIR}/dash-listener.jar

# Expose volume with configuration
VOLUME ${APPDIR}/conf

# Setup entrypoint to prepare container
COPY entrypoint.sh /entrypoint.sh
ENTRYPOINT [ "/entrypoint.sh" ]

# Execute command
CMD java -jar dash-listener.jar ${DASH_CONFIG}

# Set arguments on build
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Basic build-time metadata as defined at https://github.com/opencontainers/image-spec/blob/main/annotations.md
LABEL org.opencontainers.artifact.created="${BUILD_DATE}" \
      org.opencontainers.artifact.description="Amazon Dash Button listener" \
      org.opencontainers.image.created="${BUILD_DATE}" \
      org.opencontainers.image.authors="Gregory Moyer <greg@commitpush.run>" \
      org.opencontainers.image.url="https://github.com/syphr42/dash-listener" \
      org.opencontainers.image.documentation="https://github.com/syphr42/dash-listener" \
      org.opencontainers.image.source="https://github.com/syphr42/dash-listener.git" \
      org.opencontainers.image.version="${VERSION}" \
      org.opencontainers.image.revision="${VCS_REF}" \
      org.opencontainers.image.vendor="Syphr" \
      org.opencontainers.image.licenses="Apache-2.0" \
      org.opencontainers.image.title="Dash Listener" \
      org.opencontainers.image.description="Amazon Dash Button listener" \
      org.opencontainers.image.base.name="${RUNTIME_BASE_IMAGE}"
