package no.ssb.dapla.notes.zeppelin;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoSettingsInfo;
import org.apache.zeppelin.notebook.repo.NotebookRepoWithVersionControl;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A git based repository that supports per user branches.
 * <p>
 * This class is different from the zeppelin implementation because it
 * saves the notes in folders following the name of the notes as
 * opposed to the id of the notes. The note name is used as file name.
 * <p>
 * Each user gets it's own local copy of the remote repository. The changes
 * are pushed on the remote repository in branches named after the username.
 */
public class GitBranchRepository implements NotebookRepoWithVersionControl {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitBranchRepository.class);
    private static final Logger log = LoggerFactory.getLogger(GitBranchRepository.class);

    // Each user branch is kept checked out in their own folder.
    private final Map<String, Git> perUserRepositories = new ConcurrentHashMap<>();
    private final Configuration conf;

    public GitBranchRepository(ZeppelinConfiguration conf) {
        this.conf = new Configuration(conf);
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
        //if (subject.isAnonymous()) {
        //    throw new IllegalArgumentException("cannot use anonymous user");
        //}
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
     * Extracts the folder/path for the note
     * <p/>
     * The path is included in the name in zeppelin.
     */
    static String extractFolder(Note note) {
        String name = note.getName();
        if (name.contains("/")) {
            return name.substring(0, name.lastIndexOf('/'));
        }
        return "";
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

    Path getUserFolder(AuthenticationInfo subject) {
        return getUserFolder(extractName(subject));
    }

    Path getUserFolder(String user) {
        return conf.getGitPath().resolve(user);
    }

    private Git getOrCreateBranch(String user) {
        try {
            Git git;
            File userFolder = getUserFolder(user).toFile();
            if (!userFolder.exists() || !Arrays.asList(Objects.requireNonNull(userFolder.list())).contains(".git")) {
                CloneCommand cloneCommand = Git.cloneRepository()
                        .setURI(conf.getGitUrl())
                        .setDirectory(new File(conf.getGitPath() + File.separator + user));

                if (conf.getGitUserName() != null && conf.getGitPassword() != null) {
                    cloneCommand.setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            conf.getGitUserName(), conf.getGitPassword()
                    ));
                }

                git = cloneCommand.call();
            } else {
                git = new Git(new FileRepository(userFolder + "/.git"));
            }

            // check if user branch exists
            if (git.branchList().call().stream()
                    .noneMatch(branch -> branch.getName().endsWith(user))) {
                // Create branch and push it
                Ref userBranch = git.branchCreate().setName(user).call();
                git.push()
                        .add(userBranch)
                        .setRefSpecs( new RefSpec( user+":"+user ))
                        .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                                conf.getGitUserName(), conf.getGitPassword()))
                        .call();

                // Add config for tracking remote user branch
                StoredConfig config = git.getRepository().getConfig();
                config.setString( "branch", user, "remote", "origin" );
                config.setString( "branch", user, "merge", "refs/heads/" + user );
                config.save();

                // Checkout newly created branch
                git.checkout()
                        .setName(user)
                        .setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.TRACK)
                        .setStartPoint("origin/" + user)
                        .call();
            }

            return git;

        } catch (GitAPIException | IOException ex) {
            LOGGER.warn("failed to create the repository for {}", user, ex);
            // This method is used in a Map.computeIfAbsent. Null values will be ignored.
            return null;
        }
    }

    Git getRepository(AuthenticationInfo subject) throws IOException {
        String username = subject.getUser();
        Git userGit = perUserRepositories.computeIfAbsent(username, this::getOrCreateBranch);
        UsernamePasswordCredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(
                conf.getGitUserName(), conf.getGitPassword());
        log.info("GIT USER NAME: {}", conf.getGitUserName());
        log.info("GIT PASSWORD: {}", conf.getGitPassword());

        try {
            userGit.pull().setCredentialsProvider(credentialsProvider).call();

            // we need to checkout master, pull, and then checkout user branch again
            // to make sure we have remote master HEAD to compare with
            userGit.checkout().setName("master").call();
            userGit.pull().setCredentialsProvider(credentialsProvider).call();
            userGit.checkout().setName(username).call();

        } catch (GitAPIException e) {
            throw new IOException("Failed to fetch from remote: " + e.getMessage(), e);
        }

        // Check if branch is behind master and fully merged, if so, delete and recreate from master
        Repository repo = userGit.getRepository();

        try (RevWalk revWalk = new RevWalk(repo)) {
            RevCommit masterHead = revWalk.parseCommit(repo.resolve("refs/heads/master"));
            RevCommit userBranchHead = revWalk.parseCommit(repo.findRef(username).getObjectId());

            // Check if master head is unequal to user branch head and user branch is fully merged
            if (!masterHead.equals(userBranchHead) && revWalk.isMergedInto(userBranchHead, masterHead)) {
                // Check out user branch and merge from master
                userGit.checkout().setName(username).call();

                userGit.pull().setRebase(true).setRemote("origin").setRemoteBranchName("master").call();
                userGit.push()
                        .add(repo.findRef(username))
                        .setCredentialsProvider(credentialsProvider)
                        .call();
            }
        } catch (GitAPIException e) {
            log.error("Failed rebasing from master", e);
        }

        return userGit;
    }

    MergeResult mergeToBranch(String branchToMergeTo, AuthenticationInfo subject) throws IOException, GitAPIException {
        Git userGit = getRepository(subject);
        userGit.checkout().setName(branchToMergeTo).setCreateBranch(false).call();
        MergeResult mergeResult = userGit.merge().include(userGit.getRepository().findRef(subject.getUser())).call();
        userGit.push()
                .add(userGit.getRepository().findRef(branchToMergeTo))
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                        conf.getGitUserName(), conf.getGitPassword()))
                .call();
        return mergeResult;
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
            Note noteToCommit = get(pattern, subject);
            userGit.add().addFilepattern(noteToCommit.getName()).call();
            RevCommit commit = userGit.commit()
                    .setMessage(commitMessage)
                    .setAuthor(extractName(subject), extractEmail(subject))
                    .setCommitter("zeppelin",
                            "zeppelin@" + InetAddress.getLocalHost().getHostName())
                    .call();

            // Push local branch to remote
            userGit.push()
                    .add(userGit.getRepository().findRef(subject.getUser()))
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(
                            conf.getGitUserName(), conf.getGitPassword()))
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
        } catch (IOException e) {
            log.error("Failed to get repository for user {}", subject.getUser());
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
        List<NoteInfo> list = new ArrayList<>();

        // TODO list is cached by Zeppelin, which means that when you refresh the list for one user,
        //  all logged in users get the same list. This needs to be handled.

        // Return empty list if user is anonymous
        if(subject.getUser().equals("anonymous")) return list;

        // Recursively walk the files to build the list.
        Path rootDir = getRepository(subject).getRepository().getWorkTree().toPath();
        Iterator<Path> it = Files.walk(rootDir).filter(Files::isRegularFile).filter(file -> !file.toString().contains(".git")).iterator();
        while (it.hasNext()) {
            Path next = it.next();
            if (!next.toString().contains(".git")) {
                try {
                    list.add(new NoteInfo(getNote(next)));
                } catch (Exception ex) {
                    LOGGER.warn("failed to load {}. Maybe it was not a note?", next, ex);
                }
            }
        }
        return list;
    }

    @Override
    public Note get(String noteId, AuthenticationInfo subject) throws IOException {
        NoteInfo info = list(subject).stream()
                .filter(noteInfo -> noteId.equals(noteInfo.getId()))
                .findFirst().orElseThrow(() -> new IOException("could not find note with id " + noteId));
        return getNote(Paths.get(getRepository(subject).getRepository().getWorkTree().toPath().toString(), info.getName()));
    }

    @Override
    public void save(Note note, AuthenticationInfo subject) throws IOException {
        // Strange thing; when creating the note this method is called twice. First
        // without the name.
        if (note.getName().equals(note.getId())) {
            LOGGER.debug("ignoring the note {}", note);
        } else {

            Path userRepository = getRepository(subject).getRepository().getWorkTree().toPath();
            Path fileName = Paths.get(note.getName());
            Path noteFolder = Paths.get(extractFolder(note));

            LOGGER.info("Saving note {} to {}/{}", note.getId(), userRepository, fileName);
            LOGGER.info("Note:{}", note.toJson());
            Files.createDirectories(Paths.get(Joiner.on(File.separator).join(userRepository, noteFolder)));
            String json = note.toJson();
            try (BufferedWriter out = new BufferedWriter(new FileWriter(userRepository.resolve(fileName).toFile()))) {
                out.write(json);
            }
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
