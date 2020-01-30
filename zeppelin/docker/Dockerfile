FROM eu.gcr.io/prod-bip/ssb/zeppelin:master-9aa41acf

# Incredibly, I could not find a command that solve my tail-on-folder problem.
# Probably a zillion problems with this but it does the job for now.
COPY zeppelin/docker/mtail.c /zeppelin/
COPY zeppelin/docker/shiro.ini /zeppelin/conf/shiro.ini
COPY zeppelin/docker/zeppelin-site.xml /zeppelin/conf/zeppelin-site.xml

RUN gcc /zeppelin/mtail.c -o /bin/mtail
COPY zeppelin/docker/zeppelin-mtail.sh /zeppelin/bin/zeppelin-mtail.sh
RUN chmod +x /zeppelin/bin/zeppelin-mtail.sh

CMD ["bin/zeppelin-mtail.sh"]

COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib