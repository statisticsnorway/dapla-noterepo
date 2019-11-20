package no.ssb.data.catalog.server;

import io.micronaut.test.annotation.MicronautTest;
import no.ssb.data.note.api.ListNoteRequest;
import no.ssb.data.note.api.ListNoteResponse;
import no.ssb.data.note.api.NoteServiceGrpc;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;


@MicronautTest
class NoteServiceTest {

    @Inject
    NoteServiceGrpc.NoteServiceBlockingStub client;

    @Test
    void testHelloWorld() {
        ListNoteResponse response = client.list(ListNoteRequest.newBuilder().build());
        System.out.println(response);
    }

}