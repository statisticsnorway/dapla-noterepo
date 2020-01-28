package no.ssb.dapla.notes.zeppelin;

import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoWithVersionControl;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevCommit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.filter.PathFilter;

import java.io.*;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * A git based repository that supports per user branches.
 * <p>
 * This class is different from the zeppelin implementation because it
 * saves the notes in folders following the name of the notes as
 * opposed to the id of the notes. The note name is used as file name.
 *
 * Each user gets it's own local copy of the remote repository. The changes
 * are pushed on the remote repository in branches named after the username.
 */
public class GitBranchRepository implements NotebookRepoWithVersionControl {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitBranchRepository.class);
    private static final Logger log = LoggerFactory.getLogger(GitBranchRepository.class);

    // Each user branch is kept checked out in their own folder.
    private final Map<AuthenticationInfo, Git> perUserRepositories = new ConcurrentHashMap<>();
    private final Configuration conf;

    public GitBranchRepository(ZeppelinConfiguration conf) {
        this.conf = new Configuration(conf);
    }

    private Git getOrCreateBranch(AuthenticationInfo subject) {
        Git git = null;
        try {

            git = Git.cloneRepository()
                    .setURI(conf.getGitUrl())
                    .setDirectory(new File(conf.getGitPath() + File.separator + subject.getUser()))
                    .call();
            // check if user branch exists
            if (git.branchList().call().stream()
                    .noneMatch(branch -> branch.getName().equals(subject.getUser()))) {
                git.branchCreate().setName(subject.getUser()).call();
            }

            // TODO check if branch is fully merged. If so, delete and recreate (to get changes from master)
        } catch (GitAPIException ex) {
            ex.printStackTrace();
        }
        return git;
    }

    static NotebookRepoWithVersionControl.Revision toRevision(RevCommit commit) {
        return new Revision(
                ObjectId.toString(commit.getId()),
                commit.getFullMessage(),
                commit.getCommitTime()
        );
    }


    private static Matcher getEmailMatcher(String email) {
        Pattern emailPattern = Pattern.compile("(^.+)@.+\\..+$");
        return emailPattern.matcher(email);
    }

    /**
     * Normalize the subject to email.
     */
    static String extractEmail(AuthenticationInfo subject) {
        if (subject.isAnonymous()) {
            throw new IllegalArgumentException("cannot use anonymous user");
        }
        Matcher matcher = getEmailMatcher(subject.getUser());
        if (matcher.matches()) {
            return subject.getUser();
        } else {
            return subject.getUser() + "@noemail.no";
        }
    }

    /**
     * Normalize the subject name.
     */
    static String extractName(AuthenticationInfo subject) {
        String email = extractEmail(subject);
        Matcher matcher = getEmailMatcher(email);
        if (matcher.matches()) {
            return matcher.group();
        } else {
            throw new IllegalStateException("was not a valid email address");
        }
    }

    /**
     * Extracts the name for the note.
     * <p>
     * The path is included in the name in zeppelin.
     */
    static String extractName(Note note) {
        String name = note.getName();
        if (name.contains("/")) {
            name = name.substring(name.lastIndexOf('/') + 1);
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

    static Path getRootDir(Git userGit) {
        return userGit.getRepository().getWorkTree().toPath();
    }

    private Git getRepository(AuthenticationInfo subject) {
        return perUserRepositories.computeIfAbsent(subject, this::getOrCreateBranch);
    }

    @Override
    public Revision checkpoint(String pattern, String commitMessage, AuthenticationInfo subject) throws IOException {
        Git userGit = getRepository(subject);
        try {
            // TODO: Should use git alternates at some point.
            //
            // TODO(arild): branch -d to delete merged branches.
            //   if success recreate from HEAD
            //   if failure pull from remote.
            userGit.add().addFilepattern(pattern).call();
            RevCommit commit = userGit.commit()
                    .setMessage(commitMessage)
                    .setAuthor(extractName(subject), extractEmail(subject))
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
        Note note = null;
        RevCommit stash = null;

        try {
            Git userGit = getRepository(subject);
            List<DiffEntry> gitDiff = userGit.diff().setPathFilter(PathFilter.create(noteId)).call();
            boolean modified = !gitDiff.isEmpty();
            if (modified) {
                stash = userGit.stashCreate().call();
                Collection<RevCommit> stashes = userGit.stashList().call();
                log.debug("Created stash : {}, stash size : {}", stash, stashes.size());
            }

            ObjectId head = userGit.getRepository().resolve("HEAD");
            userGit.checkout().setStartPoint(revId).addPath(noteId).call();
            note = get(noteId, subject);
            userGit.checkout().setStartPoint(head.getName()).addPath(noteId).call();
            if (modified && stash != null) {
                ObjectId applied = userGit.stashApply().setStashRef(stash.getName()).call();
                ObjectId dropped = userGit.stashDrop().setStashRef(0).call();
                Collection<RevCommit> stashes = userGit.stashList().call();
                log.debug("Stash applied as : {}, and dropped : {}, stash size: {}", applied, dropped, stashes.size());
            }
        } catch (GitAPIException var12) {
            log.error("Failed to return note from revision \"{}\"", revId, var12);
        }

        return note;
    }

    @Override
    public List<Revision> revisionHistory(String noteId, AuthenticationInfo subject) {
        List<Revision> history = Lists.newArrayList();
        log.debug("Listing history for {}:", noteId);

        try {
            Git userGit = getRepository(subject);
            Iterable<RevCommit> logs = userGit.log().addPath(noteId).call();
            for (RevCommit commit : logs) {
                history.add(new Revision(log.getName(), commit.getShortMessage(), commit.getCommitTime()));
                log.debug(" - ({},{},{})", log.getName(), commit.getCommitTime(), commit.getFullMessage());
            }
        } catch (NoHeadException var7) {
            log.warn("No Head found for {}, {}", noteId, var7.getMessage());
        } catch (GitAPIException var8) {
            log.error("Failed to get logs for {}", noteId, var8);
        }

        return history;
    }

    @Override
    public Note setNoteRevision(String noteId, String revId, AuthenticationInfo subject) throws IOException {
        Note revisionNote = this.get(noteId, revId, subject);
        if (revisionNote != null) {
            this.save(revisionNote, subject);
        }
        return revisionNote;
    }

    private Note getNote(Path notePath) throws IOException {
        File file = notePath.normalize().toFile();
        if (file.isDirectory()) {
            throw new IOException(file.getName() + " is a directory");
        } else {
            if (!file.exists()) {
                throw new IOException(file.getName() + " not found");
            } else {
                try (InputStream ins = new FileInputStream(file)) {
                    String json = IOUtils.toString(ins, this.conf.getConfiguration()
                            .getString(ZeppelinConfiguration.ConfVars.ZEPPELIN_ENCODING));
                    return Note.fromJson(json);
                }
            }
        }
    }

    @Override
    public List<NoteInfo> list(AuthenticationInfo subject) throws IOException {
        // Recursively walk the files to build the list.
        Git userGit = getRepository(subject);
        Path rootDir = getRootDir(userGit);
        List<NoteInfo> list = new ArrayList<>();
        Iterator<Path> it = Files.walk(rootDir).filter(Files::isRegularFile).filter(file -> !file.toString().contains(".git")).iterator();
        while (it.hasNext()) {
            Path next = it.next();
            if (!next.toString().contains(".git")) {
                list.add(new NoteInfo(getNote(next)));
            }
        }
        return list;
    }

    @Override
    public Note get(String noteId, AuthenticationInfo subject) throws IOException {
        NoteInfo info = list(subject).stream()
                .filter(noteInfo -> noteId.equals(noteInfo.getId()))
                .findFirst().orElseThrow(() -> new IOException("could not find note with id " + noteId));
        return getNote(Paths.get(info.getName()));
    }

    @Override
    public void save(Note note, AuthenticationInfo subject) throws IOException {
        String relativePath = String.join(File.separator, extractNamespace(note));
        Path absolutePath = conf.getGitPath().resolve(relativePath);
        Files.createDirectories(absolutePath);
        String json = note.toJson();
        try (BufferedWriter out = new BufferedWriter(new FileWriter(absolutePath.toFile()))) {
            out.write(json);
        }
    }

    @Override
    public void remove(String noteId, AuthenticationInfo subject) throws IOException {

    }

    @Override
    public void close() {
        RuntimeException ex = null;
        for (Git value : perUserRepositories.values()) {
            try {
                value.close();
            } catch (RuntimeException e) {
                if (ex != null) {
                    ex.addSuppressed(e);
                } else {
                    ex = e;
                }
            }
        }
        if (ex != null) {
            throw ex;
        }
    }

    @Override
    public List<NotebookRepoSettingsInfo> getSettings(AuthenticationInfo authenticationInfo) {
        return null;
    }

    @Override
    public void updateSettings(Map<String, String> map, AuthenticationInfo authenticationInfo) {

    }

    static class Configuration {

        static final String CONFIG_GIT_URL_NAME = "zeppelin.notebook.ssb.git.url";
        static final String CONFIG_NOTEBOOK_PATH_NAME = "zeppelin.notebook.ssb.git.path";
        static final String CONFIG_GIT_USERNAME_NAME = "zeppelin.notebook.ssb.git.username";
        static final String CONFIG_GIT_PASSWORD_NAME = "zeppelin.notebook.ssb.git.password";
        private final ZeppelinConfiguration configuration;

        public Configuration(ZeppelinConfiguration configuration) {
            this.configuration = Objects.requireNonNull(configuration);
        }

        private static String envName(String name) {
            return name.toUpperCase().replace('.', '_');
        }

        public ZeppelinConfiguration getConfiguration() {
            return this.configuration;
        }

        public String getGitUrl() {
            String name = CONFIG_GIT_URL_NAME;
            return configuration.getString(envName(name), name, null);
        }

        public String getGitUserName() {
            String name = CONFIG_GIT_USERNAME_NAME;
            return configuration.getString(envName(name), name, null);
        }

        public Path getGitPath() {
            String name = CONFIG_NOTEBOOK_PATH_NAME;
            String path = configuration.getString(envName(name), name, null);
            return FileSystems.getDefault().getPath("", path).normalize();
        }

        public String getGitPassword() {
            String name = CONFIG_GIT_PASSWORD_NAME;
            return configuration.getString(envName(name), name, null);
        }

    }
}
