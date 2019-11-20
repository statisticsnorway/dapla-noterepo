package no.ssb.data.catalog.server;

import io.grpc.ManagedChannel;
import io.micronaut.context.annotation.Bean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.grpc.annotation.GrpcChannel;
import io.micronaut.grpc.server.GrpcServerChannel;

import static no.ssb.data.catalog.api.DatasetCatalogServiceGrpc.DatasetCatalogServiceBlockingStub;
import static no.ssb.data.catalog.api.DatasetCatalogServiceGrpc.newBlockingStub;

@Factory
public class TestRpcClient {

    @Bean
    public DatasetCatalogServiceBlockingStub blockingStub(
            @GrpcChannel(GrpcServerChannel.NAME) ManagedChannel channel
    ) {
        return newBlockingStub(channel);
    }
}
