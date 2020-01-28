package no.ssb.dapla.notes.zeppelin;


import com.google.common.base.Joiner;
import org.apache.commons.io.FileUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GitBranchRepositoryTest {


    private static final String TEST_NOTE_ID = "2A94M5J1Z";
    private static final String TEST_NOTE_PATH = "process" + File.separator + "step" + File.separator;
    private static final String TEST_NOTE_NAME = "Zeppelin Tutorial";
    public static Path tmpDir;
    private static ZeppelinConfiguration conf;
    private static GitBranchRepository gitHubNotebookRepo;
    private static Git remoteGit;
    private static AuthenticationInfo USER1 = new AuthenticationInfo("user1", null, "user1");
    private static AuthenticationInfo USER2 = new AuthenticationInfo("user2", null, "user2");
    private static String remoteZeppelinDirName = "remoteRepo";
    private static String localNotebookDirName = "notebooks";

    @BeforeAll
    public static void setUp() throws Exception {

        // For some reasons @TmpDir does not work when the tests are
        // executed from maven.
        tmpDir = Files.createTempDirectory(null);

        conf = ZeppelinConfiguration.create();

        // Copy the test notebook directory from the test/resources/2A94M5J1Z folder to the fake remote Git directory
        String remoteTestNoteDir = Joiner.on(File.separator).join(tmpDir, remoteZeppelinDirName, TEST_NOTE_PATH);
        FileUtils.copyFile(
                new File(
                        GitBranchRepositoryTest.class.getResource(
                                Joiner.on(File.separator).join("", TEST_NOTE_ID) + File.separator + "note.json"
                        ).getFile()
                ), new File(remoteTestNoteDir + TEST_NOTE_NAME + ".json")
        );

        // Create the fake remote Git repository
        Repository remoteRepository = new FileRepository(Joiner.on(File.separator).join(remoteTestNoteDir, ".git"));
        remoteRepository.create();

        remoteGit = new Git(remoteRepository);
        remoteGit.add().addFilepattern(".").call();
        RevCommit firstCommitRevision = remoteGit.commit().setMessage("First commit from remote repository").call();

        // Set local notebook storage path
        System.setProperty(GitBranchRepository.Configuration.CONFIG_NOTEBOOK_PATH_NAME, Joiner.on(File.separator).join(tmpDir, localNotebookDirName));

        // Set the Git and Git configurations
        System.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_HOME.getVarName(), remoteTestNoteDir);
        System.setProperty(ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_DIR.getVarName(), remoteTestNoteDir);

        // Set the GitHub configurations
        System.setProperty(
                ZeppelinConfiguration.ConfVars.ZEPPELIN_NOTEBOOK_STORAGE.getVarName(),
                "org.apache.zeppelin.notebook.repo.GitHubNotebookRepo");
        System.setProperty(GitBranchRepository.Configuration.CONFIG_GIT_URL_NAME,
                remoteTestNoteDir + File.separator + ".git");
        System.setProperty(GitBranchRepository.Configuration.CONFIG_GIT_USERNAME_NAME, "token");

        gitHubNotebookRepo = new GitBranchRepository(conf);

    }

    @AfterAll
    static void afterAll() throws IOException {
        Files.walkFileTree(tmpDir, new SimpleFileVisitor<Path>() {
            public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
                Files.delete(path);
                return FileVisitResult.CONTINUE;
            }

            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                if (exc != null) {
                    throw exc;
                } else {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            }
        });
    }

    @Test
    void testEmailNormalization() {
        assertThat(GitBranchRepository.extractName(new AuthenticationInfo("user")))
                .isEqualTo("user@noemail.no");
        assertThat(GitBranchRepository.extractName(new AuthenticationInfo("user@email.no")))
                .isEqualTo("user@email.no");
    }

    @Test
    void testExtractNameAndNamespace() {
        Note note = new Note();
        note.setName("just a name");
        assertThat(GitBranchRepository.extractName(note)).isEqualTo("just a name");
        assertThat(GitBranchRepository.extractNamespace(note)).isEmpty();

        note.setName("/name at root");
        assertThat(GitBranchRepository.extractName(note)).isEqualTo("name at root");
        assertThat(GitBranchRepository.extractNamespace(note)).isEmpty();

        note.setName("/foo/bar/foo bar/name in folders");
        assertThat(GitBranchRepository.extractName(note)).isEqualTo("name in folders");
        assertThat(GitBranchRepository.extractNamespace(note)).containsExactly(
                "foo",
                "bar",
                "foo bar"
        );
    }

    @Test
    void testConnectToGithub() throws GitAPIException, IOException, URISyntaxException {
        ZeppelinConfiguration c = new ZeppelinConfiguration();

        GitBranchRepository repository = new GitBranchRepository(c) {
            Repository getOrCreateBranch(AuthenticationInfo subject) {
                return null;
            }
        };

        repository.hadrienTestGetRepo("hadrien");

    }

    /**
     * List notebooks from remote repo. On first listing, a folder with the logged in user's username is created, remote repo
     * is cloned into it and a branche with the logged in user's username as name is created.
     */
    @Test
    public void testBranchCreation() throws Exception {
        List<NoteInfo> list = gitHubNotebookRepo.list(USER1);
        assertThat(list.size()).isEqualTo(1);
        NoteInfo noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);
    }
}