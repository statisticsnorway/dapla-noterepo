package no.ssb.dapla.notes.zeppelin;


import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import no.ssb.dapla.notes.protobuf.*;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.interpreter.InterpreterResult;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class SsbNotebookRepo implements NotebookRepo {

    private static final NameBasedGenerator UUID_GENERATOR = Generators.nameBasedGenerator(
            UUID.fromString("c0e783e7-db6d-4de4-81bf-27b2f2f02805")
    );

    private static final Logger log = LoggerFactory.getLogger(SsbNotebookRepo.class);

    private static final Boolean CONFIG_PLAIN_DEFAULT = false;
    private static final String CONFIG_PLAIN_NAME = "zeppelin.notebook.ssb.plain";
    private static final String CONFIG_PLAIN_ENV_NAME = CONFIG_PLAIN_NAME.toUpperCase().replace('.', '_');

    private static final String CONFIG_HOST_DEFAULT = "localhost";
    private static final String CONFIG_HOST_NAME = "zeppelin.notebook.ssb.host";
    private static final String CONFIG_HOST_ENV_NAME = CONFIG_HOST_NAME.toUpperCase().replace('.', '_');

    private static final int CONFIG_PORT_DEFAULT = 50051;
    private static final String CONFIG_PORT_NAME = "zeppelin.notebook.ssb.port";
    private static final String CONFIG_PORT_ENV_NAME = CONFIG_PORT_NAME.toUpperCase().replace('.', '_');
    private static final String ZEPPELIN_NAME = "zeppelin";

    private static final String CONFIG_AUTH_HEADER_NAME_NAME = "zeppelin.notebook.ssb.auth.header.name";
    private static final String CONFIG_AUTH_HEADER_NAME_ENV_NAME = CONFIG_AUTH_HEADER_NAME_NAME.toUpperCase().replace('.', '_');
    private static final String CONFIG_AUTH_HEADER_VALUE_NAME = "zeppelin.notebook.ssb.auth.header.value";
    private static final String CONFIG_AUTH_HEADER_VALUE_ENV_NAME = CONFIG_AUTH_HEADER_VALUE_NAME.toUpperCase().replace('.', '_');

    private ManagedChannel channel;
    private NoteServiceGrpc.NoteServiceBlockingStub noteClient;

    SsbNotebookRepo() {
        this(new ZeppelinConfiguration());
    }

    SsbNotebookRepo(ManagedChannelBuilder<?> channelBuilder, ZeppelinConfiguration conf) {
        channel = channelBuilder.build();
        noteClient = NoteServiceGrpc.newBlockingStub(channel);

        // TODO: Check https://github.com/avast/grpc-java-jwt
        String authKey = conf.getString(CONFIG_AUTH_HEADER_NAME_ENV_NAME, CONFIG_AUTH_HEADER_NAME_NAME, "");
        String authValue = conf.getString(CONFIG_AUTH_HEADER_VALUE_ENV_NAME, CONFIG_AUTH_HEADER_VALUE_NAME, "");
        if (!"".equals(authKey)) {
            log.warn("Using static auth metadata {}", authKey);
            noteClient = noteClient.withCallCredentials(new StaticCallCredentials(authKey, authValue));
        }
    }

    public SsbNotebookRepo(ZeppelinConfiguration conf) {
        this(createChannel(conf), conf);
    }

    static ManagedChannelBuilder<?> createChannel(ZeppelinConfiguration conf) {
        String host = conf.getString(CONFIG_HOST_ENV_NAME, CONFIG_HOST_NAME, CONFIG_HOST_DEFAULT);
        int port = conf.getInt(CONFIG_PORT_ENV_NAME, CONFIG_PORT_NAME, CONFIG_PORT_DEFAULT);
        log.info("Note backend: {}:{}", host, port);
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        if (conf.getBoolean(CONFIG_PLAIN_ENV_NAME, CONFIG_PLAIN_NAME, CONFIG_PLAIN_DEFAULT)) {
            log.warn("\n\n" +
                    "\t\tYou chose to use a plain (unencrypted) connection.\n" +
                    "\t\tMake sure you understand the implications." +
                    "\n\n");
            channelBuilder.usePlaintext();
        }
        return channelBuilder;
    }

    /**
     * Extracts the name for the note.
     * <p>
     * The path is included in the name in zeppelin.
     */
    static String extractName(Note note) {
        String name = note.getName();
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/'));
        }
        return name;
    }

    /**
     * Extracts the namespace (folder) of a note
     * <p>
     * Path is relative and '/' separated.
     */
    static List<String> extractNamespace(Note note) {
        return Arrays.asList(note.getFolderId().split("/"));
    }

    private static UUID extractUUID(Note note) {
        return UUID_GENERATOR.generate(note.getId());
    }

    public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
        try {
            ListNoteResponse noteResponse = noteClient.list(ListNoteRequest.newBuilder().build());
            List<no.ssb.dapla.notes.protobuf.Note> grpcNotes = noteResponse.getNotesList();
            List<NoteInfo> result = new ArrayList<>(noteResponse.getCount());
            for (no.ssb.dapla.notes.protobuf.Note grpcNote : grpcNotes) {
                if (grpcNote.containsSerializedNote(ZEPPELIN_NAME)) {
                    Note note = Note.fromJson(grpcNote.getSerializedNoteOrThrow(ZEPPELIN_NAME));
                    result.add(new NoteInfo(note));
                }
            }
            return result;
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public Note get(String noteId, AuthenticationInfo subject) throws IOException {
        try {
            // Note that we use the original note id.
            GetNoteResponse response = noteClient.get(GetNoteRequest.newBuilder().setOriginalId(noteId).build());
            return Note.fromJson(response.getNote().getSerializedNoteOrThrow(ZEPPELIN_NAME));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void save(Note note, AuthenticationInfo subject) throws IOException {
        try {

            // Convert paragraphs.
            no.ssb.dapla.notes.protobuf.Note.Builder noteBuilder = no.ssb.dapla.notes.protobuf.Note.newBuilder()

                    .setUuid(extractUUID(note).toString())
                    .addAliasIdentifiers(note.getId())

                    .setName(extractName(note))
                    .addAllNamespace(extractNamespace(note));


            for (Paragraph paragraph : note.getParagraphs()) {

                try {
                    no.ssb.dapla.notes.protobuf.Paragraph.Builder grpcParagraphBuilder = no.ssb.dapla.notes.protobuf.Paragraph.newBuilder();
                    if (paragraph.getText() != null) {
                        grpcParagraphBuilder.setCode(paragraph.getText());
                    }
                    no.ssb.dapla.notes.protobuf.Paragraph grpcParagraph = grpcParagraphBuilder.build();
                    noteBuilder.addParagraphs(grpcParagraph);

                    // Ask service to parse the paragraphs.
                    Iterator<Dataset> inputs = noteClient.parseInput(grpcParagraph);
                    if (inputs.hasNext() && !noteBuilder.getInputsList().isEmpty()) {
                        log.warn("the dataset {} has more than one paragraph with inputs (paragraph {})", note.getId(),
                                paragraph.getId());
                    }
                    while (inputs.hasNext()) {
                        noteBuilder.addInputs(inputs.next());
                    }

                    // Ask service to parse the paragraphs.
                    Iterator<Dataset> outputs = noteClient.parseOutput(grpcParagraph);
                    if (inputs.hasNext() && !noteBuilder.getOutputsList().isEmpty()) {
                        log.warn("the dataset {} has more than one paragraph with outputs (paragraph {})", note.getId(),
                                paragraph.getId());
                    }

                    while (outputs.hasNext()) {
                        Dataset next = outputs.next();
                        noteBuilder.addOutputs(next);
                    }

                    if (paragraph.getResult() != null && paragraph.getResult().code() != InterpreterResult.Code.ERROR) {
                        String foundInputText = noteBuilder.getInputsList().stream()
                                .map(dataset -> "Name: " + dataset.getName() + ", " + dataset.getUri())
                                .collect(Collectors.joining("\n", "Found output:", "\n"));
                        String foundOutputText = noteBuilder.getOutputsList().stream()
                                .map(dataset -> "Name: " + dataset.getName() + ", " + dataset.getUri())
                                .collect(Collectors.joining("\n", "Found inputs", "\n"));

                        paragraph.setReturn(new InterpreterResult(InterpreterResult.Code.SUCCESS, foundInputText + foundOutputText), null);
                    }
                } catch (Exception ex) {
                    paragraph.setReturn(new InterpreterResult(InterpreterResult.Code.ERROR), ex);
                }

            }

            noteBuilder.putSerializedNote(ZEPPELIN_NAME, note.toJson());

            noteClient.save(SaveNoteRequest.newBuilder().setNote(noteBuilder).build());
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void remove(String noteId, AuthenticationInfo subject) throws IOException {
        try {
            noteClient.delete(DeleteNoteRequest.newBuilder().setUuid(noteId).build());
        } catch (Exception ex) {
            throw new IOException(ex);
        }

    }

    public synchronized void close() {
        if (channel != null) {
            channel.shutdown();
            channel = null;
        }
    }

    public List<NotebookRepoSettingsInfo> getSettings(AuthenticationInfo authenticationInfo) {
        return Collections.emptyList();
    }

    public void updateSettings(Map<String, String> map, AuthenticationInfo authenticationInfo) {
        // Not implemented yet.
    }
}