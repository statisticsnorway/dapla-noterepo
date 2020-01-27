package no.ssb.dapla.notes.zeppelin;

import org.apache.zeppelin.conf.ZeppelinConfiguration;
import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class GitBranchRepositoryHadrienTest {


    @TempDir
    Path tempDir;

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
            @Override
            Repository getOrCreateBranch(AuthenticationInfo subject) {
                return null;
            }
        };

        repository.hadrienTestGetRepo("hadrien");

    }
}