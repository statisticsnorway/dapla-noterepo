package no.ssb.dapla.notes.zeppelin;


import org.apache.zeppelin.notebook.Note;
import org.apache.zeppelin.user.AuthenticationInfo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;


public class GitBranchRepositoryTest {

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

}