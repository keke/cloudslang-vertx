FROM openjdk:alpine
MAINTAINER keke <iamkeke@gmail.com>

ENV REGISTRY_PORT 9999
ARG VERSION

RUN mkdir /work
EXPOSE 9999
VOLUME /config
VOLUME /data

COPY cloudslang-vertx-$VERSION-fat.jar /work/cloudslangvertx.jar

WORKDIR work

ENTRYPOINT ["java","-jar", "cloudslangvertx.jar"]