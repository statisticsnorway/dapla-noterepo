package no.ssb.dapla.notes.zeppelin;

import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoWithVersionControl;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.lib.Repository;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A git based repository that supports remotes
 * <p>
 * This repository supports remote git servers.
 * It relies on a jgit client to do so.
 * <p>
 * Each user accessing the repo will create a fork of the master repository. When a
 * user (from the AuthenticationInfo) requests something, his/her fork is returned
 * it exists or a new fork is created.
 */
public abstract class GitForkRepository implements NotebookRepoWithVersionControl {

    // Keep a hot cache of clients.
    private final Map<AuthenticationInfo, Repository> cachedClient = new ConcurrentHashMap<>();
    private final Repository masterRepository;

    public GitForkRepository(Repository gitRepository) {
        this.masterRepository = gitRepository;
    }

    abstract Repository getOrCreateFork(AuthenticationInfo subject);

    private Repository getRepository(AuthenticationInfo subject) {
        return cachedClient.computeIfAbsent(subject, this::getOrCreateFork);
    }

    @Override
    public Revision checkpoint(String pattern, String commitMessage, AuthenticationInfo subject) throws IOException {
        return null;
    }

    @Override
    public Note get(String noteId, String revId, AuthenticationInfo subject) throws IOException {
        return null;
    }

    @Override
    public List<Revision> revisionHistory(String noteId, AuthenticationInfo subject) {
        return null;
    }

    @Override
    public Note setNoteRevision(String noteId, String revId, AuthenticationInfo subject) throws IOException {
        return null;
    }

    @Override
    public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
        return null;
    }

    @Override
    public Note get(String noteId, AuthenticationInfo subject) throws IOException {
        return null;
    }

    @Override
    public void save(Note note, AuthenticationInfo subject) throws IOException {

    }

    @Override
    public void remove(String noteId, AuthenticationInfo subject) throws IOException {

    }

    @Override
    public void close() {
        masterRepository.close();
        for (Repository value : cachedClient.values()) {
            value.close();
        }
    }

    @Override
    public List<NotebookRepoSettingsInfo> getSettings(AuthenticationInfo authenticationInfo) {
        return null;
    }

    @Override
    public void updateSettings(Map<String, String> map, AuthenticationInfo authenticationInfo) {

    }
}
