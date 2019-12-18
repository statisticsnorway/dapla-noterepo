package no.ssb.dapla.notes.zeppelin;

import io.grpc.CallCredentials;
import io.grpc.Metadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executor;

public class StaticCallCredentials extends CallCredentials {

    private static final Logger log = LoggerFactory.getLogger(StaticCallCredentials.class);

    private final Metadata metadata = new Metadata();

    public StaticCallCredentials(String key, String value) {
        metadata.put(
                Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER),
                value
        );
    }

    @Override
    public void applyRequestMetadata(RequestInfo requestInfo, Executor executor, MetadataApplier metadataApplier) {
        log.debug("adding metadata {} to {}", metadata, requestInfo);
        executor.execute(() -> metadataApplier.apply(this.metadata));
    }

    @Override
    public void thisUsesUnstableApi() {
    }
}
