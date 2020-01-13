FROM openjdk:11-jre-slim

WORKDIR /app

COPY target/libs /app/lib/
COPY target/dapla-notes-service.jar /app/lib/

CMD ["java", "-cp", "/app/lib/*", "no.ssb.dapla.notes.service.Application"]

EXPOSE 1408