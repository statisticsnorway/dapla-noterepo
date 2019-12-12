FROM eu.gcr.io/prod-bip/ssb/lds-zeppelin


COPY zeppelin/target/dapla-notes-zeppelin-*-shaded.jar /zeppelin/lib

# Enable multiuser
COPY zeppelin/docker/zeppelin-site.xml /zeppelin/conf/
COPY zeppelin/docker/shiro.ini /zeppelin/conf/