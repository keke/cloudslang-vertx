FROM openjdk:alpine
MAINTAINER keke <iamkeke@gmail.com>

ENV REGISTRY_PORT 9999
ARG VERSION=0.0.2

RUN mkdir /work
EXPOSE 9999
VOLUME /config
VOLUME /data
WORKDIR work
RUN wget -O cloudslangvertx.jar http://dl.bintray.com/keke/keke-maven/io/kk/cloudslang-vertx/$VERSION/cloudslang-vertx-$VERSION-fat.jar
ENTRYPOINT ["java","-jar", "cloudslangvertx.jar"]