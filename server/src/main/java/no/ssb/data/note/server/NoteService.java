package no.ssb.data.note.server;

import io.grpc.Status;
import io.grpc.StatusException;
import io.grpc.stub.StreamObserver;
import no.ssb.data.note.api.*;

import javax.inject.Singleton;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

//@Slf4j
@Singleton
public class NoteService extends NoteServiceGrpc.NoteServiceImplBase {

    private static Map<String, Note> noteRepo = new ConcurrentHashMap<>();

    private static Note createNote(String name, List<NamedDataset> input, List<NamedDataset> output) {
        return Note.newBuilder()
                .addAllInput(input)
                .addAllOutput(output)
                .build();
    }

    @Override
    public void parseOutput(Paragraph request, StreamObserver<NamedDataset> responseObserver) {
//        log.debug("parsing output");
        try {
            Pattern outputPattern = Pattern.compile("" +
                    "(?<outputName>\\w\\w*)" +
                    "\\s*" +
                    "\\.spark" +
                    "\\s*" +
                    "\\.write" +
                    "\\s*" +
                    "(:?\\.mode\\(\"\\w*\"\\))?" +
                    "\\s*" +
                    "\\.format\\(\"(gsim|no\\.ssb\\.gsim\\.spark)\"\\)" +
                    "\\s*" +
                    "\\.save\\(\"(?<namespace>.+?)\"\\)"
            );
            Matcher outputMatcher = outputPattern.matcher(request.getCode());
            while (outputMatcher.find()) {
                String inputName = outputMatcher.group("outputName");
                String namespace = outputMatcher.group("namespace");
                NamedDataset input = NamedDataset.newBuilder()
                        .setName(inputName)
                        .setUri(namespace)
                        .build();
                responseObserver.onNext(input);
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
            Pattern inputPattern = Pattern.compile("" +
                    "val" +
                    "\\s+" +
                    "(?<inputName>\\w\\w*)" +
                    "\\s+" +
                    "=" +
                    "\\s+" +
                    "spark" +
                    "\\s*" +
                    "\\.read" +
                    "\\s*" +
                    "\\.format\\(\"(gsim|no\\.ssb\\.gsim\\.spark)\"\\)" +
                    "\\s*" +
                    "\\.load\\(\"(?<namespace>.+?)\"\\)"
            );
            Matcher inputMatcher = inputPattern.matcher(request.getCode());
            while (inputMatcher.find()) {
                String inputName = inputMatcher.group("inputName");
                String namespace = inputMatcher.group("namespace");
                NamedDataset input = NamedDataset.newBuilder()
                        .setName(inputName)
                        .setUri(namespace)
                        .build();
                responseObserver.onNext(input);
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
