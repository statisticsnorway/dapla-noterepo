FROM alpine AS build-env

ENV Z_VERSION="0.8.2"
ENV Z_HOME="/zeppelin"

RUN wget -O zeppelin-bin-netinst.tgz https://archive.apache.org/dist/zeppelin/zeppelin-${Z_VERSION}/zeppelin-${Z_VERSION}-bin-netinst.tgz && \
    tar -zxvf zeppelin-bin-netinst.tgz && \
    rm -rf zeppelin-bin-netinst.tgz && \
    mv /zeppelin-${Z_VERSION}-bin-netinst ${Z_HOME}/

FROM openjdk:8-alpine

# Copy zeppelin
COPY --from=build-env /zeppelin /zeppelin

# Required by the interpreter and launch script.
RUN apk --no-cache add bash

RUN mkdir /zeppelin/logs/ \
          /zeppelin/run

# Output log to console
COPY /docker/log4j.properties /zeppelin/conf/log4j.properties

# Copy ssb dependencies
COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib
COPY oidc/target/dapla-notes-oidc-*-shaded.jar /zeppelin/lib
COPY oidc/spark-interpreter-0.8.2.jar /zeppelin/interpreter/spark/spark-interpreter-0.8.2.jar

# Required so that we persist the note permissions as well
# TODO: Find a better solution, changing the path/volume/config could break this
RUN ln -sf /zeppelin/notebook/notebook-authorization.json \
           /zeppelin/conf/notebook-authorization.json

# Install hadoop
ENV HADOOP_HOME="/opt/hadoop-2.7.7"
RUN wget https://archive.apache.org/dist/hadoop/common/hadoop-2.7.7/hadoop-2.7.7.tar.gz && \
    tar -zxpf hadoop-2.7.7.tar.gz -C /tmp && \
    mv /tmp/hadoop-2.7.7 ${HADOOP_HOME} && \
    rm -rf hadoop-2.7.7.tar.gz

# Intall spark.
ENV SPARK_HOME "/usr/lib/spark"
RUN wget https://apache.uib.no/spark/spark-2.4.5/spark-2.4.5-bin-without-hadoop-scala-2.12.tgz && \
    tar -zxpf spark-2.4.5-bin-without-hadoop-scala-2.12.tgz -C /tmp && \
    mv /tmp/spark-2.4.5-bin-without-hadoop-scala-2.12 ${SPARK_HOME} && \
    rm -rf spark-2.4.5-bin-without-hadoop-scala-2.12.tgz

RUN chown -R 2100:2100 /zeppelin
USER 2100:2100

WORKDIR /zeppelin

CMD ["bin/zeppelin.sh"]


