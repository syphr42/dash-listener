FROM multiarch/debian-debootstrap:amd64-jessie

# Set download urls
ENV \
    JAVA_URL="https://www.azul.com/downloads/zulu/zdk-8-ga-linux_x64.tar.gz" \
    DASH_LISTENER_URL="https://github.com/syphr42/dash-listener/archive/master.zip"

# Set variables and locales
ENV \
    APPDIR="/dash" \
    EXTRA_JAVA_OPTS="" \
    LC_ALL=en_US.UTF-8 \
    LANG=en_US.UTF-8 \
    LANGUAGE=en_US.UTF-8
    DASH_CONFIG="conf/config.properties"

# Set arguments on build
ARG BUILD_DATE
ARG VCS_REF
ARG VERSION

# Basic build-time metadata as defined at http://label-schema.org
LABEL org.label-schema.build-date=$BUILD_DATE \
    org.label-schema.docker.dockerfile="/Dockerfile" \
    org.label-schema.license="Apache 2.0" \
    org.label-schema.name="Dash Listener" \
    org.label-schema.vendor="Syphr" \
    org.label-schema.version=$VERSION \
    org.label-schema.description="Amazon Dash Button listener" \
    org.label-schema.url="https://github.com/syphr42/dash-listener" \
    org.label-schema.vcs-ref=$VCS_REF \
    org.label-schema.vcs-type="Git" \
    org.label-schema.vcs-url="https://github.com/syphr42/dash-listener.git" \
    maintainer="Gregory Moyer <moyerg@syphr.com>"

# Install basepackages
RUN apt-get update && \
    DEBIAN_FRONTEND=noninteractive apt-get install --no-install-recommends -y \
    maven \
    ca-certificates \
    fontconfig \
    locales \
    locales-all \
    libpcap-dev \
    netbase \
    unzip \
    wget \
    zip && \
    rm -rf /var/lib/apt/lists/* && \
ln -s -f /bin/true /usr/bin/chfn

# Install java
#ENV JAVA_HOME='/usr/lib/java-8'
#RUN wget -nv -O /tmp/java.tar.gz ${JAVA_URL} && \
#    mkdir ${JAVA_HOME} && \
#    tar -xvf /tmp/java.tar.gz --strip-components=1 -C ${JAVA_HOME} && \
#    rm /tmp/java.tar.gz && \
#    update-alternatives --install /usr/bin/java java ${JAVA_HOME}/bin/java 50 && \
#    update-alternatives --install /usr/bin/javac javac ${JAVA_HOME}/bin/javac 50
#RUN cd /tmp \
#    && wget https://cdn.azul.com/zcek/bin/ZuluJCEPolicies.zip \
#    && unzip -jo -d ${JAVA_HOME}/jre/lib/security /tmp/ZuluJCEPolicies.zip \
#    && rm /tmp/ZuluJCEPolicies.zip

# Install gosu
#ENV GOSU_VERSION 1.10
#RUN set -x \
#    && dpkgArch="$(dpkg --print-architecture | awk -F- '{ print $NF }')" \
#    && wget -O /usr/local/bin/gosu "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch" \
#    && wget -O /usr/local/bin/gosu.asc "https://github.com/tianon/gosu/releases/download/$GOSU_VERSION/gosu-$dpkgArch.asc" \
#    && export GNUPGHOME \
#    && GNUPGHOME="$(mktemp -d)" \
#    && gpg --keyserver ha.pool.sks-keyservers.net --recv-keys B42F6819007F00F88E364FD4036A9C25BF357DD4 \
#    && gpg --batch --verify /usr/local/bin/gosu.asc /usr/local/bin/gosu \
#    && rm -r "$GNUPGHOME" /usr/local/bin/gosu.asc \
#    && chmod +x /usr/local/bin/gosu

# Install dash-listener
RUN wget -nv -O /tmp/dash-listener.zip ${DASH_LISTENER_URL} && \
    unzip -q /tmp/dash-listener.zip -d ${APPDIR}/src && \
    rm /tmp/dash-listener.zip && \
    cd ${APPDIR}/src && \
    mvn clean package -P executable && \
    cp ${APPDIR}/target/dash-listener-*.jar ${APPDIR}/dash-listener.jar && \
    rm -rf ${APPDIR}/src && \
    mkdir -p ${APPDIR}/conf

# Expose volume with configuration
VOLUME ${APPDIR}/conf

# Set working directory, expose and entrypoint
WORKDIR ${APPDIR}

# Execute command
CMD ["java", "-jar", "dash-listener.jar", "$DASH_CONFIG"]
