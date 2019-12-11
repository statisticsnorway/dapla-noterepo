package no.ssb.dapla.note.server;

import no.ssb.dapla.note.api.ListNoteRequest;
import no.ssb.dapla.note.api.ListNoteResponse;
import no.ssb.dapla.note.api.NoteServiceGrpc;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

class NoteServiceTest {


    NoteServiceGrpc.NoteServiceBlockingStub client;

    void testHelloWorld() {
        ListNoteResponse response = client.list(ListNoteRequest.newBuilder().build());
        System.out.println(response);
    }

}