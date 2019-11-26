package no.ssb.data.note.server;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import no.ssb.data.note.api.*;
import no.ssb.data.note.server.parsing.ParagraphConverter;

import javax.inject.Singleton;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

//@Slf4j
@Singleton
public class NoteService extends NoteServiceGrpc.NoteServiceImplBase {

    private static Map<String, Note> noteRepo = new ConcurrentHashMap<>();

    private final List<ParagraphConverter> converters;

    public NoteService(List<ParagraphConverter> converters) {
        this.converters = converters;
    }

    @Override
    public void parseOutput(Paragraph request, StreamObserver<NamedDataset> responseObserver) {
//        log.debug("parsing output");
        try {
            for (ParagraphConverter converter : converters) {
                if (converter.canHandle(request)) {
                    Iterator<NamedDataset> iterator = converter.parseOutput(request);
                    while (iterator.hasNext()) {
                        NamedDataset output = iterator.next();
                        responseObserver.onNext(output);
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void parseInput(Paragraph request, StreamObserver<NamedDataset> responseObserver) {
//        log.debug("parsing input");
        try {
            for (ParagraphConverter converter : converters) {
                if (converter.canHandle(request)) {
                    Iterator<NamedDataset> iterator = converter.parseInput(request);
                    while (iterator.hasNext()) {
                        NamedDataset input = iterator.next();
                        responseObserver.onNext(input);
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void save(SaveNoteRequest request, StreamObserver<SaveNoteResponse> responseObserver) {
//        log.debug("saving dataset");
        try {
            Note note = request.getNote();
            noteRepo.put(note.getIdentifier().getUuid(), note);
            responseObserver.onNext(SaveNoteResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void get(GetNoteRequest request, StreamObserver<GetNoteResponse> responseObserver) {
//        log.debug("get dataset");
        try {
            Note note = noteRepo.get(request.getIdentifier().getUuid());
            if (note != null) {
                responseObserver.onNext(GetNoteResponse.newBuilder().setNote(note).build());
            }
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void list(ListNoteRequest request, StreamObserver<ListNoteResponse> responseObserver) {
//        log.debug("list dataset");
        try {
            ListNoteResponse response = ListNoteResponse.newBuilder()
                    .addAllNotes(noteRepo.values())
                    .setCount(noteRepo.size())
                    .setOffset(0)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }
}
