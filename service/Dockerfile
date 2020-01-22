FROM openjdk:11-jre-slim

WORKDIR /app

COPY service/target/libs /app/lib/
COPY service/target/dapla-notes-service*.jar /app/lib/

CMD ["java", "-p", "/app/lib", "-m", "dapla.notes.service/no.ssb.dapla.notes.service.Application"]

EXPOSE 10138