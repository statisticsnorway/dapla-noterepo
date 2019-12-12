package no.ssb.dapla.note.server;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;

import no.ssb.dapla.note.api.*;
import no.ssb.dapla.note.server.parsing.ParagraphConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;


public class NoteService extends NoteServiceGrpc.NoteServiceImplBase {

    private static final Logger log = LoggerFactory.getLogger(NoteService.class);

    private final List<ParagraphConverter> converters;
    private final NoteRepository noteRepo;

    public NoteService(List<ParagraphConverter> converters, NoteRepository noteRepo) {
        this.converters = Objects.requireNonNull(converters);
        this.noteRepo = Objects.requireNonNull(noteRepo);
    }

    @Override
    public void parseOutput(Paragraph request, StreamObserver<Dataset> responseObserver) {
        log.debug("parsing output");
        try {
            for (ParagraphConverter converter : converters) {
                if (converter.canHandle(request)) {
                    Iterator<Dataset> iterator = converter.parseOutput(request);
                    while (iterator.hasNext()) {
                        Dataset output = iterator.next();
                        responseObserver.onNext(output);
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.warn("could not parse output", ex);
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void parseInput(Paragraph request, StreamObserver<Dataset> responseObserver) {
        log.debug("parsing input");
        try {
            for (ParagraphConverter converter : converters) {
                if (converter.canHandle(request)) {
                    Iterator<Dataset> iterator = converter.parseInput(request);
                    while (iterator.hasNext()) {
                        Dataset input = iterator.next();
                        responseObserver.onNext(input);
                    }
                }
            }
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.warn("could not parse input", ex);
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void save(SaveNoteRequest request, StreamObserver<SaveNoteResponse> responseObserver) {
        log.debug("saving dataset");
        try {
            Note note = request.getNote();
            noteRepo.saveNote(note);
            responseObserver.onNext(SaveNoteResponse.newBuilder().build());
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.warn("could not save note", ex);
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void get(GetNoteRequest request, StreamObserver<GetNoteResponse> responseObserver) {
        log.debug("get dataset");
        try {
            if (request.getUuid().isEmpty() && request.getOriginalId().isEmpty()) {
                throw new IllegalArgumentException("uuid or originalId must be set");
            }
            if (!request.getUuid().isEmpty()) {
                Note note = noteRepo.getNote(request.getUuid());
                if (note != null) {
                    responseObserver.onNext(GetNoteResponse.newBuilder().setNote(note).build());
                }
                responseObserver.onCompleted();
            }
            if (!request.getOriginalId().isEmpty()) {
                Note note = noteRepo.getNoteByOriginalId(request.getOriginalId());
                if (note != null) {
                    responseObserver.onNext(GetNoteResponse.newBuilder().setNote(note).build());
                }
                responseObserver.onCompleted();
            }
        } catch (Exception ex) {
            log.warn("could not get note", ex);
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }

    @Override
    public void list(ListNoteRequest request, StreamObserver<ListNoteResponse> responseObserver) {
        log.debug("list dataset");
        try {

            List<String> namespace = request.getNamespaceList();
            List<Note> notes = noteRepo.listNotes(namespace, request.getCount(), request.getOffset());

            ListNoteResponse response = ListNoteResponse.newBuilder()
                    .addAllNotes(notes)
                    .setCount(notes.size())
                    .setOffset(request.getOffset() + notes.size())
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (Exception ex) {
            log.warn("could not list notes", ex);
            responseObserver.onError(new StatusException(Status.fromThrowable(ex)));
        }
    }
}
