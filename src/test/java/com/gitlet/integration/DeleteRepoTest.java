package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.support.TestSupport;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeleteRepoTest {

    @Test
    void deleteRepoRemovesGitletDirectory(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("v1");
        String out = TestSupport.captureStdout(repo::deleteRepo);
        assertFalse(new File(dir, ".gitlet").exists());
        assertTrue(out.contains("deleted"));
        assertTrue(new File(dir, "a.txt").exists());
    }

    @Test
    void deleteRepoFailsWhenNotInRepository(@TempDir File dir) {
        Repository repo = TestSupport.repoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class, repo::deleteRepo);
        assertTrue(ex.getMessage().contains("Not in an initialized"));
    }
}
