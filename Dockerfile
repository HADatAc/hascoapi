# The first part of this Dockerfile is inspired by an existing Dockerfile hosted at https://github.com/mozilla/docker-sbt/blob/main/Dockerfile
# The important parts have been copied over to remove a dependency on two public Docker containers
#FROM openjdk:11
FROM sbtscala/scala-sbt:eclipse-temurin-11.0.16_1.7.2_2.12.17 as build-java

RUN sed -i -e 's|http://ports.ubuntu.com/ubuntu-ports|https://ports.ubuntu.com/ubuntu-ports|g' /etc/apt/sources.list && \
	apt-get update && \
	apt-get install -y --no-install-recommends unzip && \
	rm -rf /var/lib/apt/lists/*

# Avoid Maven Central rate-limits by mirroring it via Google Cloud Storage.
RUN mkdir -p /root/.config/coursier && \
	cat > /root/.config/coursier/mirror.properties <<'EOF'
central.from=https://repo1.maven.org/maven2;https://repo.maven.apache.org/maven2
central.to=https://maven-central.storage-download.googleapis.com/maven2
central.type=tree
EOF

ENV COURSIER_MIRRORS=/root/.config/coursier/mirror.properties
ENV JAVA_OPTS="-Xms6048m -Xmx10000m"
WORKDIR /hascoapi

# Copy over the basic configuration files
#COPY ["build.sbt", "/tmp/build/"]
#COPY ["project/plugins.sbt", "project/sbt-ui.sbt", "project/build.properties", "/tmp/build/project/"]

# Sbt sometimes fails because of network problems. Retry 3 times.
#RUN (sbt compile || sbt compile || sbt compile) && \
#    (sbt test:compile || sbt test:compile || sbt test:compile) && \
#    rm -rf /tmp/build

COPY . /hascoapi

RUN sbt playUpdateSecret && sbt dist
RUN cd /hascoapi/target/universal/ && unzip hascoapi-10.0.1-SNAPSHOT.zip

FROM eclipse-temurin:11-jre

WORKDIR /hascoapi

COPY --from=build-java /hascoapi/target/universal/hascoapi-10.0.1-SNAPSHOT /hascoapi

COPY ./conf/hascoapi-docker.conf /hascoapi/conf/hascoapi.conf
COPY ./docker-entrypoint.sh /hascoapi/docker-entrypoint.sh

RUN chmod +x /hascoapi/docker-entrypoint.sh

EXPOSE 9000

ENTRYPOINT [ "/hascoapi/docker-entrypoint.sh" ]
