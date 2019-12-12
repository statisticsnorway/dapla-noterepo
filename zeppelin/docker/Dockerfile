FROM eu.gcr.io/prod-bip/ssb/lds-zeppelin

COPY zeppelin-plugin/build/libs/zeppelin-plugin-*-all.jar /zeppelin/lib

# Enable multiuser
COPY zeppelin-plugin/docker/zeppelin-site.xml /zeppelin/conf/
COPY zeppelin-plugin/docker/shiro.ini /zeppelin/conf/