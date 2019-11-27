package no.ssb.dapla.note.server;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.grpc.server.GrpcServerChannel;
import no.ssb.data.note.api.NoteServiceGrpc;

import static no.ssb.data.note.api.NoteServiceGrpc.newBlockingStub;

@Factory
public class TestRpcClient {

    @Bean
    public NoteServiceGrpc.NoteServiceBlockingStub blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) ManagedChannel channel
    ) {
        return newBlockingStub(channel);
    }
}
