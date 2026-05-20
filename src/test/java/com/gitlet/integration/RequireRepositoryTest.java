package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.support.TestSupport;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RequireRepositoryTest {

    @Test
    void addOutsideRepoFails(@TempDir File dir) {
        Repository repo = TestSupport.repoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        GitletExitException ex = assertThrows(GitletExitException.class, () -> repo.add("a.txt"));
        assertTrue(ex.getMessage().contains("Not in an initialized"));
    }

    @Test
    void commitOutsideRepoFails(@TempDir File dir) {
        Repository repo = TestSupport.repoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class, () -> repo.commit("x"));
        assertTrue(ex.getMessage().contains("Not in an initialized"));
    }

    @Test
    void statusOutsideRepoFails(@TempDir File dir) {
        Repository repo = TestSupport.repoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class, repo::status);
        assertTrue(ex.getMessage().contains("Not in an initialized"));
    }

    @Test
    void constructorRejectsMissingCwd() {
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> new Repository("C:/this-path-cannot-possibly-exist-12345"));
        assertTrue(ex.getMessage().contains("not found"));
    }
}
