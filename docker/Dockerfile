FROM openjdk:8-alpine

ENV Z_VERSION="0.8.2"
ENV Z_HOME="/zeppelin"
ENV ZEPPELIN_GROUP_ID=2100
ENV ZEPPELIN_USER_ID=2100

# Install zeppelin
RUN wget -O zeppelin-bin-netinst.tgz https://archive.apache.org/dist/zeppelin/zeppelin-${Z_VERSION}/zeppelin-${Z_VERSION}-bin-netinst.tgz && \
    tar -zxf zeppelin-bin-netinst.tgz && \
    rm -rf zeppelin-bin-netinst.tgz && \
    mv /zeppelin-${Z_VERSION}-bin-netinst ${Z_HOME}/

# Required by the interpreter and launch script.
RUN apk --no-cache add bash

RUN mkdir /zeppelin/logs/ \
          /zeppelin/run

# Required so that we persist the note permissions as well
# TODO: Find a better solution, changing the path/volume/config could break this
RUN ln -sf /zeppelin/notebook/notebook-authorization.json \
           /zeppelin/conf/notebook-authorization.json

# Intall spark.
ENV SPARK_HOME "/usr/lib/spark"
RUN wget https://apache.uib.no/spark/spark-2.4.5/spark-2.4.5-bin-hadoop2.7.tgz && \
    tar -zxpf spark-2.4.5-bin-hadoop2.7.tgz -C /tmp && \
    mv /tmp/spark-2.4.5-bin-hadoop2.7 ${SPARK_HOME} && \
    rm -rf /tmp/spark-2.4.5-bin-hadoop2.7  && \
    rm spark-2.4.5-bin-hadoop2.7.tgz
ENV PATH="$PATH:$SPARK_HOME/bin:$SPARK_HOME/sbin"

# Add sensible interpreter config defaults
COPY docker/interpreter.json /zeppelin/conf/

# Output log to console
COPY docker/log4j.properties /zeppelin/conf/log4j.properties

# Replace interpreter script
COPY docker/interpreter.sh /zeppelin/bin/interpreter.sh
RUN chmod u+x /zeppelin/bin/interpreter.sh

# Copy ssb dependencies
COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib
COPY oidc/target/dapla-notes-oidc-*-shaded.jar /zeppelin/lib


# Zeppelin needs a real user/group
RUN addgroup -g $ZEPPELIN_GROUP_ID -S zeppelin && \
    adduser  -u $ZEPPELIN_USER_ID --shell /bin/bash --home ${Z_HOME} --disabled-password -S zeppelin -G zeppelin && \
    chown -R $ZEPPELIN_USER_ID:$ZEPPELIN_GROUP_ID /zeppelin

USER zeppelin:zeppelin

WORKDIR /zeppelin

CMD ["bin/zeppelin.sh"]


