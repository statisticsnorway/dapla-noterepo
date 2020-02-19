FROM eu.gcr.io/prod-bip/ssb/zeppelin:master-90870450

# Incredibly, I could not find a command that solve my tail-on-folder problem.
# Probably a zillion problems with this but it does the job for now.
COPY docker/mtail.c /zeppelin/
COPY docker/shiro.ini /zeppelin/conf/shiro.ini
COPY docker/zeppelin-site.xml /zeppelin/conf/zeppelin-site.xml

USER root
RUN gcc /zeppelin/mtail.c -o /bin/mtail
COPY docker/zeppelin-mtail.sh /zeppelin/bin/zeppelin-mtail.sh
RUN chmod +x /zeppelin/bin/zeppelin-mtail.sh
USER zeppelin

CMD ["bin/zeppelin-mtail.sh"]

COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib
COPY oidc/target/dapla-notes-oidc.jar /zeppelin/lib
COPY oidc/spark-interpreter-0.8.2.jar /zeppelin/interpreter/spark/spark-interpreter-0.8.2.jar