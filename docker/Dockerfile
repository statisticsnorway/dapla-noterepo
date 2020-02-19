FROM eu.gcr.io/prod-bip/ssb/zeppelin:master-90870450

USER root
# Incredibly, I could not find a command that solve my tail-on-folder problem.
# Probably a zillion problems with this but it does the job for now.
COPY docker/mtail.c /zeppelin/
RUN mv /zeppelin/bin/zeppelin.sh /zeppelin/bin/zeppelin-orig.sh
RUN gcc /zeppelin/mtail.c -o /bin/mtail
COPY docker/zeppelin-mtail.sh /zeppelin/bin/zeppelin.sh
RUN chmod +x /zeppelin/bin/zeppelin.sh
USER zeppelin

COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib
COPY oidc/target/dapla-notes-oidc.jar /zeppelin/lib
COPY oidc/spark-interpreter-0.8.2.jar /zeppelin/interpreter/spark/spark-interpreter-0.8.2.jar