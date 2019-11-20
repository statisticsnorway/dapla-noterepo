package no.ssb.data.catalog.server;

import io.grpc.stub.StreamObserver;
import no.ssb.data.note.api.*;

import javax.inject.Singleton;
import java.util.List;

@Singleton
public class NoteService extends NoteServiceGrpc.NoteServiceImplBase {

    private static Note createNote(String name, List<NamedDataset> input, List<NamedDataset> output) {
        return Note.newBuilder()
                .addAllInput(input)
                .addAllOutput(output)
                .build();
    }

    @Override
    public void list(ListNoteRequest request, StreamObserver<ListNoteResponse> responseObserver) {
        ListNoteResponse response = ListNoteResponse.newBuilder()
                .addNotes(createNote("someName", List.of(), List.of()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
