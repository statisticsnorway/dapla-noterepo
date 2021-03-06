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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GitBranchRepositoryBranchLogicTest {

    private static final String TEST_NOTE_ID = "2A94M5J1Z";
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

    @BeforeEach
    public void setUp() throws Exception {

        // For some reasons @TmpDir does not work when the tests are
        // executed from maven.
        tmpDir = Files.createTempDirectory(null);

        conf = ZeppelinConfiguration.create();

        // Copy the test notebook directory from the test/resources/2A94M5J1Z folder to the fake remote Git directory
        remoteTestNoteDir = Joiner.on(File.separator).join(tmpDir, remoteZeppelinDirName) + File.separator;
        FileUtils.copyFile(
                new File(
                        GitBranchRepositoryTest.class.getResource(
                                Joiner.on(File.separator).join("", TEST_NOTE_ID) + File.separator + "note.json"
                        ).getFile()
                ), new File(remoteTestNoteDir + TEST_NOTE_NAME)
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
        System.setProperty(GitBranchRepository.Configuration.CONFIG_GIT_PASSWORD_NAME, "password");

        gitHubNotebookRepo = new GitBranchRepository(conf);

    }

    @AfterEach
    void afterAll() throws IOException {
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
     * Assert that an anonymous user gets an empty list
     */
    @Test
    public void testAnonymousLogin() throws IOException {
        List<NoteInfo> list = gitHubNotebookRepo.list(new AuthenticationInfo("anonymous"));
        assertThat(list.size()).isEqualTo(0);
    }

    /**
     * List notebooks from remote repo. On first listing, a folder with the logged in user's username is created,
     * remote repo is cloned into it and a branch with the logged in user's username as branch name is created.
     */
    @Test
    public void testBranchCreation() throws Exception {

        Repository user1Repo = gitHubNotebookRepo.getRepository(USER1).getRepository();
        Repository user2Repo = gitHubNotebookRepo.getRepository(USER2).getRepository();

        // Assert that a branch is created for the user and that it contains one note
        List<NoteInfo> list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());
        assertThat(list.size()).isEqualTo(1);
        NoteInfo noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);

        // Assert that a committed note is present in local branch
        String noteName = "Note2";
        Note note = createNote(noteName, USER1.getUser());
        gitHubNotebookRepo.save(note, USER1);
        gitHubNotebookRepo.checkpoint(note.getId(), "Commit Note2", USER1);
        list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());
        assertThat(list.size()).isEqualTo(2);
        Optional<NoteInfo> newNoteOptional = list.stream().filter(n -> n.getId().equals(note.getId())).findFirst();
        assertThat(newNoteOptional.isPresent()).isTrue();
        NoteInfo newNote = newNoteOptional.get();
        assertThat(newNote.getId()).isEqualTo(note.getId());
        assertThat(newNote.getName()).isEqualTo(noteName);

        // Assert that a new user gets a new branch from master
        list = gitHubNotebookRepo.list(USER2);
        assertThat(user2Repo.getBranch()).isEqualTo(USER2.getUser());
        assertThat(list.size()).isEqualTo(1);
        noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);
    }

    @Test
    public void testRebaseAfterMerge() throws Exception{
        Repository user1Repo = gitHubNotebookRepo.getRepository(USER1).getRepository();
        Repository user2Repo = gitHubNotebookRepo.getRepository(USER2).getRepository();

        // Log in as user 1
        List<NoteInfo> list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());
        assertThat(list.size()).isEqualTo(1);
        NoteInfo noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);

        // do one commit with new note
        String noteNameUser1 = "Note2_User1";
        final Note note = createNote(noteNameUser1, USER1.getUser());
        gitHubNotebookRepo.save(note, USER1);
        gitHubNotebookRepo.checkpoint(note.getId(), "Commit Note2 User 1", USER1);
        list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());

        // Assert that branch contains two notes
        assertThat(list.size()).isEqualTo(2);
        Optional<NoteInfo> newNoteOptional = list.stream().filter(n -> n.getId().equals(note.getId())).findFirst();
        assertThat(newNoteOptional.isPresent()).isTrue();
        NoteInfo newNote = newNoteOptional.get();
        assertThat(newNote.getId()).isEqualTo(note.getId());
        assertThat(newNote.getName()).isEqualTo(noteNameUser1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());

        // Merge branch to master
        gitHubNotebookRepo.mergeToBranch("master", USER1);
        assertThat(user1Repo.getBranch()).isEqualTo("master"); // branch should be master after merge

        // Log in as user 2
        list = gitHubNotebookRepo.list(USER2);
        assertThat(user2Repo.getBranch()).isEqualTo(USER2.getUser());

        // Assert that branch contains two notes
        assertThat(list.size()).isEqualTo(2);
        newNoteOptional = list.stream().filter(n -> n.getId().equals(note.getId())).findFirst();
        assertThat(newNoteOptional.isPresent()).isTrue();
        newNote = newNoteOptional.get();
        assertThat(newNote.getId()).isEqualTo(note.getId());
        assertThat(newNote.getName()).isEqualTo(noteNameUser1);
        assertThat(user2Repo.getBranch()).isEqualTo(USER2.getUser());

        // do one commit with new note
        String noteNameUser2 = "Note2_User2";
        Note note1User2 = createNote(noteNameUser2, USER2.getUser());
        gitHubNotebookRepo.save(note1User2, USER2);
        gitHubNotebookRepo.checkpoint(note1User2.getId(), "Commit Note2 User 2", USER2);
        list = gitHubNotebookRepo.list(USER2);
        assertThat(user2Repo.getBranch()).isEqualTo(USER2.getUser());

        // Assert that branch contains three notes
        assertThat(list.size()).isEqualTo(3);

        // Merge branch to master
        gitHubNotebookRepo.mergeToBranch("master", USER2);
        assertThat(user1Repo.getBranch()).isEqualTo("master"); // branch should be master after merge

        // Log in as user 1
        list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());

        // Assert that branch contains three notes
        assertThat(list.size()).isEqualTo(3);
    }

    /**
     * Assert that when a note is deleted, the note is removed from its original location
     * and placed in the "~Trash" folder and that both the Trash folder and the note inside
     * are not tracked.
     */
    @Test
    public void testDeleteNote() throws IOException, GitAPIException {

        Git user1Git = gitHubNotebookRepo.getRepository(USER1);
        Repository user1Repo = user1Git.getRepository();

        // Log in as user 1
        List<NoteInfo> list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());
        assertThat(list.size()).isEqualTo(1);
        NoteInfo noteInfo = list.get(0);
        assertThat(noteInfo).isNotNull();
        assertThat(noteInfo.getId()).isEqualTo(TEST_NOTE_ID);
        assertThat(noteInfo.getName()).isEqualTo(TEST_NOTE_NAME);

        // do one commit with new note
        String noteNameUser1 = "Note2_User1";
        Note note = createNote(noteNameUser1, USER1.getUser());
        gitHubNotebookRepo.save(note, USER1);
        gitHubNotebookRepo.checkpoint(note.getId(), "Commit Note2 User 1", USER1);
        list = gitHubNotebookRepo.list(USER1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());

        // Assert that branch contains two notes
        assertThat(list.size()).isEqualTo(2);
        Optional<NoteInfo> newNoteOptional = list.stream().filter(n -> n.getId().equals(note.getId())).findFirst();
        assertThat(newNoteOptional.isPresent()).isTrue();
        NoteInfo newNote = newNoteOptional.get();
        assertThat(newNote.getId()).isEqualTo(note.getId());
        assertThat(newNote.getName()).isEqualTo(noteNameUser1);
        assertThat(user1Repo.getBranch()).isEqualTo(USER1.getUser());

        // Delete note
        String deletedName = "~Trash/" + noteNameUser1;
        note.setName(deletedName);
        gitHubNotebookRepo.save(note, USER1);
        list = gitHubNotebookRepo.list((USER1));
        assertThat(list.size()).isEqualTo(2);
        assertThat(list.stream().anyMatch(n -> n.getName().equals(deletedName))).isTrue();

        // Assert that the Trash folder is untracked
        Set<String> untrackedFolders = user1Git.status().call().getUntrackedFolders();
        assertThat(untrackedFolders.size()).isEqualTo(1);
        assertThat(untrackedFolders.stream().anyMatch(folder -> folder.equals("~Trash"))).isTrue();

        // Assert that the deleted note is untracked
        Set<String> untracked = user1Git.status().call().getUntracked();
        assertThat(untracked.size()).isEqualTo(1);
        assertThat(untracked.stream().anyMatch(file -> file.equals(deletedName))).isTrue();
    }

    private Note createNote(String noteName, String username) {
        Note note = new Note().getUserNote(username);
        note.setName(noteName);
        return note;
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
                .toString().toUpperCase();
    }
}