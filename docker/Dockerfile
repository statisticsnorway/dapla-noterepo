FROM alpine AS build-env

ENV Z_VERSION="0.8.2"
ENV Z_HOME="/zeppelin"

RUN wget -O zeppelin-bin-netinst.tgz http://archive.apache.org/dist/zeppelin/zeppelin-${Z_VERSION}/zeppelin-${Z_VERSION}-bin-netinst.tgz && \
    tar -zxvf zeppelin-bin-netinst.tgz && \
    rm -rf zeppelin-bin-netinst.tgz && \
    mv /zeppelin-${Z_VERSION}-bin-netinst ${Z_HOME}/

FROM openjdk:8-alpine

# Copy zeppelin
COPY --from=build-env /zeppelin /zeppelin

# Required by the interpreter and launch script.
RUN apk --no-cache add bash

RUN mkdir /zeppelin/logs/

# Copy ssb dependencies
COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib
COPY oidc/target/dapla-notes-oidc.jar /zeppelin/lib
COPY oidc/spark-interpreter-0.8.2.jar /zeppelin/interpreter/spark/spark-interpreter-0.8.2.jar

RUN chown -R 2001:2002 /zeppelin
USER 2001:2002

WORKDIR /zeppelin

CMD ["bin/zeppelin.sh"]


