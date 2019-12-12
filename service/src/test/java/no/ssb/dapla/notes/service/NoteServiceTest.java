package no.ssb.dapla.notes.service;

import no.ssb.dapla.notes.protobuf.ListNoteRequest;
import no.ssb.dapla.notes.protobuf.ListNoteResponse;
import no.ssb.dapla.notes.protobuf.NoteServiceGrpc;

class NoteServiceTest {


    NoteServiceGrpc.NoteServiceBlockingStub client;

    void testHelloWorld() {
        ListNoteResponse response = client.list(ListNoteRequest.newBuilder().build());
        System.out.println(response);
    }

}