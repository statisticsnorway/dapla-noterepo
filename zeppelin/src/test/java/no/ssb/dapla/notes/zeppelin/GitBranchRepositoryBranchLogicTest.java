package no.ssb.dapla.notes.zeppelin;

import com.google.common.base.Joiner;
import com.google.gson.JsonObject;
import org.apache.commons.io.FileUtils;
import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.NoteInfo;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.internal.storage.file.FileRepository;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.*;

class GitBranchRepositoryBranchLogicTest {

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
    private static String remoteTestNoteDir;

    @BeforeAll
    public static void setUp() throws Exception {

        // For some reasons @TmpDir does not work when the tests are
        // executed from maven.
        tmpDir = Files.createTempDirectory(null);

        conf = ZeppelinConfiguration.create();

        // Copy the test notebook directory from the test/resources/2A94M5J1Z folder to the fake remote Git directory
        remoteTestNoteDir = Joiner.on(File.separator).join(tmpDir, remoteZeppelinDirName, TEST_NOTE_PATH);
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

    /**
     * List notebooks from remote repo. On first listing, a folder with the logged in user's username is created,
     * remote repo is cloned into it and a branch with the logged in user's username as branch name is created.
     */
    @Test
    public void testBranchCreation() throws Exception {

        // Assert that a branch is created for the user and that it contains one note
        List<NoteInfo> list = gitHubNotebookRepo.list(USER1);
        assertThat(list.size()).isEqualTo(1);
        NoteInfo noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);

        // Assert that a committed note is present in local branch
        String noteName = "Note2";
        createNoteInLocalRepo(noteName);
        gitHubNotebookRepo.checkpoint("*", "Commit Note2", USER1);
        list = gitHubNotebookRepo.list(USER1);
        assertThat(list.size()).isEqualTo(2);
        noteInfo = list.get(1);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isNotEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(noteName);

        // Assert that a new user gets a new branch from master
        list = gitHubNotebookRepo.list(USER2);
        assertThat(list.size()).isEqualTo(1);
        noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);
    }

    private void createNoteInLocalRepo(String noteName) throws IOException {
        JsonObject note = new JsonObject();
        note.addProperty("name", noteName);
        note.addProperty("id", createRandomID());
        try (FileWriter file =
                     new FileWriter(
                             Joiner.on(File.separator).join(tmpDir, localNotebookDirName, USER1.getUser())
                                     + File.separator + noteName + ".json")) {
            file.write(note.toString());
        }
    }

    private String createRandomID() {
        int leftLimit = 48; // numeral '0'
        int rightLimit = 122; // letter 'z'
        int targetStringLength = 9;
        Random random = new Random();

        return random.ints(leftLimit, rightLimit + 1)
                .filter(i -> (i <= 57 || i >= 65) && (i <= 90 || i >= 97))
                .limit(targetStringLength)
                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
                .toString();
    }
}