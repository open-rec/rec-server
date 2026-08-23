# syntax=docker/dockerfile:1.7
ARG MAVEN_IMAGE=maven:3.9.9-eclipse-temurin-8
ARG JAVA_IMAGE=eclipse-temurin:8-jre-jammy

FROM ${MAVEN_IMAGE} AS build
WORKDIR /src

COPY . .
# Keep downloaded Maven artifacts outside the invalidated source layer. Source
# changes still trigger compilation, but dependencies are only downloaded once.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B package -DskipTests

FROM ${JAVA_IMAGE}
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
WORKDIR /opt/openrec
COPY --from=build /src/server/target/rec-server-1.0-SNAPSHOT.jar ./rec-server.jar
COPY --from=build /src/contrib/target/rec-contrib-1.0-SNAPSHOT.jar ./plugins/rec-contrib.jar

EXPOSE 13579
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=12 \
  CMD curl -fsS http://127.0.0.1:13579/health >/dev/null

ENTRYPOINT ["java", "-Dopenrec.operation.plugin=/opt/openrec/plugins/rec-contrib.jar", "-jar", "/opt/openrec/rec-server.jar"]
