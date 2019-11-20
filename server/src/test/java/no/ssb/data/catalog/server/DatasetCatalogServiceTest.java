package no.ssb.data.catalog.server;

import io.micronaut.test.annotation.MicronautTest;
import no.ssb.data.catalog.api.Dataset;
import no.ssb.data.catalog.api.DatasetIdentifier;
import no.ssb.data.catalog.api.ListDatasetRequest;
import no.ssb.data.catalog.api.ListDatasetResponse;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.util.Iterator;

import static no.ssb.data.catalog.api.DatasetCatalogServiceGrpc.DatasetCatalogServiceBlockingStub;


@MicronautTest
class DatasetCatalogServiceTest {

    @Inject
    DatasetCatalogServiceBlockingStub client;

    @Test
    void testHelloWorld() {
        ListDatasetResponse response = client.list(ListDatasetRequest.newBuilder().build());
        System.out.println(response);
    }

}