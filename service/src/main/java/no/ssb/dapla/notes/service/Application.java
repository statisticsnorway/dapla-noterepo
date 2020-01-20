package no.ssb.dapla.notes.service;

import io.helidon.config.Config;
import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import no.ssb.dapla.notes.service.memory.MemoryRepository;
import no.ssb.dapla.notes.service.parsing.ScalaParagraphConverter;
import no.ssb.helidon.application.DefaultHelidonApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class Application extends DefaultHelidonApplication {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        installSlf4jJulBridge();

        new ApplicationBuilder().build()
                .start()
                .toCompletableFuture()
                .orTimeout(10, TimeUnit.SECONDS)
                .thenAccept(app -> log.info("gRPC Server started at: http://localhost:{}", app.get(GrpcServer.class).port()));
    }

    Application(Config config) {
        NoteService noteService = new NoteService(
                List.of(new ScalaParagraphConverter()),
                new MemoryRepository()
        );
        put(NoteService.class, noteService);
        GrpcServer grpcServer = GrpcServer
                .create(GrpcRouting.builder()
                        .register(noteService)
                        .build());
        put(GrpcServer.class, grpcServer);
    }
}