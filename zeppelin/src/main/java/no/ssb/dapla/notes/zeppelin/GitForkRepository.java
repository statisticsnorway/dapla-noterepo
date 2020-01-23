package no.ssb.dapla.notes.zeppelin;

import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoWithVersionControl;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

import java.io.IOException;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A git based repository that supports per user branches.
 */
public abstract class GitForkRepository implements NotebookRepoWithVersionControl {

    private final Map<AuthenticationInfo, Repository> perUserRepositories = new ConcurrentHashMap<>();
    private final Repository masterRepository;

    public GitForkRepository(Repository gitRepository) {
        this.masterRepository = gitRepository;
    }

    abstract Repository getOrCreateBranch(AuthenticationInfo subject);

    private Repository getRepository(AuthenticationInfo subject) {
        return perUserRepositories.computeIfAbsent(subject, this::getOrCreateBranch);
    }

    static Revision toRevision(RevCommit commit) {
        return new Revision(
                ObjectId.toString(commit.getId()),
                commit.getFullMessage(),
                commit.getCommitTime()
        );
    }

    @Override
    public Revision checkpoint(String pattern, String commitMessage, AuthenticationInfo subject) throws IOException {
        Git userGit = new Git(getRepository(subject));
        try {
            // TODO(arild): branch -d to delete merged branches.
            //   if success recreate from HEAD
            //   if failure pull from remote.
            userGit.add().addFilepattern(pattern).call();
            RevCommit commit = userGit.commit()
                    .setMessage(commitMessage)
                    .setAuthor(subject.getUser(), subject.getUser())
                    .setCommitter("zeppelin",
                            "zeppelin@" + InetAddress.getLocalHost().getHostName())
                    .call();
            return toRevision(commit);

        } catch (GitAPIException e) {
            throw new IOException("Git error: " + e.getMessage(), e);
        }
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
        for (Repository value : perUserRepositories.values()) {
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
