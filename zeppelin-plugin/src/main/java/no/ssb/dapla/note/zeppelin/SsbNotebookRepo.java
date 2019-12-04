package no.ssb.dapla.note.zeppelin;


import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.NameBasedGenerator;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import no.ssb.dapla.note.api.*;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.Paragraph;
import org.apache.zeppelin.notebook.repo.NotebookRepo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.joda.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class SsbNotebookRepo implements NotebookRepo {

    private static final NameBasedGenerator UUID_GENERATOR = Generators.nameBasedGenerator(
            UUID.fromString("c0e783e7-db6d-4de4-81bf-27b2f2f02805")
    );

    private static final Logger LOG = LoggerFactory.getLogger(SsbNotebookRepo.class);

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

    private ManagedChannel channel;
    private NoteServiceGrpc.NoteServiceBlockingStub noteClient;

    SsbNotebookRepo() {
        this(new ZeppelinConfiguration());
    }

    SsbNotebookRepo(ManagedChannelBuilder<?> channelBuilder) {
        channel = channelBuilder.build();
        noteClient = NoteServiceGrpc.newBlockingStub(channel);
    }

    public SsbNotebookRepo(ZeppelinConfiguration conf) {
        this(createChannel(conf));
    }

    static ManagedChannelBuilder createChannel(ZeppelinConfiguration conf) {
        String host = conf.getString(CONFIG_HOST_ENV_NAME, CONFIG_HOST_NAME, CONFIG_HOST_DEFAULT);
        int port = conf.getInt(CONFIG_PORT_ENV_NAME, CONFIG_PORT_NAME, CONFIG_PORT_DEFAULT);
        LOG.info("Note backend: {}:{}", host, port);
        ManagedChannelBuilder<?> channelBuilder = ManagedChannelBuilder.forAddress(host, port);
        if (conf.getBoolean(CONFIG_PLAIN_ENV_NAME, CONFIG_PLAIN_NAME, CONFIG_PLAIN_DEFAULT)) {
            LOG.warn("" +
                    "You chose to use a plain (unencrypted) connection.\n" +
                    "Make sure you understand the implications.");
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
    static NameSpace extractNamespace(Note note) {
        List<String> path = Arrays.asList(note.getFolderId().split("/"));
        return NameSpace.newBuilder().addAllNamespace(path).build();
    }

    private static NoteIdentifier extractNoteIdentifier(Note note, String name, NameSpace namespace) {
        return NoteIdentifier.newBuilder()
                .setUuid(UUID_GENERATOR.generate(note.getId()).toString())
                .setName(name)
                .setTimestamp(Instant.now().getMillis())
                .setNamespace(namespace)
                .build();
    }

    public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
        try {
            ListNoteResponse noteResponse = noteClient.list(ListNoteRequest.newBuilder().build());
            List<no.ssb.dapla.note.api.Note> grpcNotes = noteResponse.getNotesList();
            List<NoteInfo> result = new ArrayList<>(noteResponse.getCount());
            for (no.ssb.dapla.note.api.Note grpcNote : grpcNotes) {
                if (grpcNote.containsSerializedNote(ZEPPELIN_NAME)) {
                    Note note = Note.fromJson(grpcNote.getSerializedNoteOrThrow(ZEPPELIN_NAME));
                    if (!note.getId().equals(grpcNote.getIdentifier().getUuid())) {
                        throw new IOException("parsed note id differs from saved note id");
                    }
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
            GetNoteResponse response = noteClient.get(GetNoteRequest.newBuilder().setIdentifier(NoteIdentifier.newBuilder()
                    .setUuid(noteId)).build());
            return Note.fromJson(response.getNote().getSerializedNoteOrThrow(ZEPPELIN_NAME));
        } catch (Exception ex) {
            throw new IOException(ex);
        }
    }

    public void save(Note note, AuthenticationInfo subject) throws IOException {
        try {
            String name = extractName(note);
            NameSpace namespace = extractNamespace(note);
            NoteIdentifier identifier = extractNoteIdentifier(note, name, namespace);

            // Convert paragraphs.
            no.ssb.dapla.note.api.Note.Builder noteBuilder = no.ssb.dapla.note.api.Note.newBuilder()
                    .setIdentifier(identifier);

            for (Paragraph paragraph : note.getParagraphs()) {

                paragraph.getResult().add("test");

                no.ssb.dapla.note.api.Paragraph.Builder grpcParagraphBuilder = no.ssb.dapla.note.api.Paragraph.newBuilder();
                if (paragraph.getText() != null) {
                    grpcParagraphBuilder.setCode(paragraph.getText());
                }
                no.ssb.dapla.note.api.Paragraph grpcParagraph = grpcParagraphBuilder.build();
                noteBuilder.addParagraphs(grpcParagraph);

                // Ask service to parse the paragraphs.
                Iterator<NamedDataset> inputs = noteClient.parseInput(grpcParagraph);
                if (inputs.hasNext() && !noteBuilder.getInputList().isEmpty()) {
                    LOG.warn("the dataset {} has more than one paragraph with inputs (paragraph {})", note.getId(),
                            paragraph.getId());
                }
                while (inputs.hasNext()) {
                    noteBuilder.addInput(inputs.next());
                }

                // Ask service to parse the paragraphs.
                Iterator<NamedDataset> outputs = noteClient.parseOutput(grpcParagraph);
                if (inputs.hasNext() && !noteBuilder.getOutputList().isEmpty()) {
                    LOG.warn("the dataset {} has more than one paragraph with outputs (paragraph {})", note.getId(),
                            paragraph.getId());
                }
                while (outputs.hasNext()) {
                    noteBuilder.addOutput(outputs.next());
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
            noteClient.delete(DeleteNoteRequest.newBuilder().setIdentifier(NoteIdentifier.newBuilder().setUuid(noteId)).build());
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