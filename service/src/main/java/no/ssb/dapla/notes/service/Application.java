package no.ssb.dapla.notes.service;

import io.helidon.grpc.server.GrpcRouting;
import io.helidon.grpc.server.GrpcServer;
import no.ssb.dapla.notes.service.memory.MemoryRepository;
import no.ssb.dapla.notes.service.parsing.ScalaParagraphConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogManager;

public class Application {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static int getVersion() {
        String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            version = version.substring(2, 3);
        } else {
            int dot = version.indexOf(".");
            if (dot != -1) {
                version = version.substring(0, dot);
            }
        }
        return Integer.parseInt(version);
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException, TimeoutException {

        setupLogging();

        NoteService noteService = new NoteService(
                List.of(new ScalaParagraphConverter()),
                new MemoryRepository()
        );

        GrpcServer grpcServer = GrpcServer
                .create(GrpcRouting.builder()
                        .register(noteService)
                        .build())
                .start()
                .toCompletableFuture()
                .get(10, TimeUnit.SECONDS);
        log.info("Java version {}", getVersion());
        log.info("gRPC Server started at: http://localhost:{}", grpcServer.port());
    }

    /**
     * Disable the JUL hendler and instal the SLF4J bridge.
     */
    private static void setupLogging() {
        LogManager.getLogManager().reset();
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();
    }

}