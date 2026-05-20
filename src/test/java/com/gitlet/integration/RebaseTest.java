package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.support.TestSupport;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RebaseTest {

    @Test
    void rebaseRejectsMissingBranch(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rebase("ghost"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void rebaseRejectsSelf(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rebase("master"));
        assertTrue(ex.getMessage().contains("itself"));
    }

    @Test
    void rebaseReplaysCommitsOntoTarget(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "a");
        repo.add("a.txt");
        repo.commit("a");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "m.txt", "master");
        repo.add("m.txt");
        repo.commit("master m");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "d.txt", "dev");
        repo.add("d.txt");
        repo.commit("dev d");
        repo.rebase("master");
        assertEquals("a", TestSupport.readFile(dir, "a.txt"));
        assertEquals("master", TestSupport.readFile(dir, "m.txt"));
        assertEquals("dev", TestSupport.readFile(dir, "d.txt"));
    }

    @Test
    void rebaseFastForwardsWhenCurrentIsAncestor(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "a");
        repo.add("a.txt");
        repo.commit("a");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "b.txt", "b");
        repo.add("b.txt");
        repo.commit("master b");
        repo.checkoutBranch("dev");
        repo.rebase("master");
        assertEquals("b", TestSupport.readFile(dir, "b.txt"));
    }

    @Test
    void rebaseReportsAlreadyUpToDate(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "a");
        repo.add("a.txt");
        repo.commit("a");
        repo.branch("dev");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "b.txt", "b");
        repo.add("b.txt");
        repo.commit("dev b");
        String out = TestSupport.captureStdout(() -> repo.rebase("master"));
        assertTrue(out.contains("up-to-date"));
    }

    @Test
    void rebaseReplayWithConflict(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "base");
        repo.add("a.txt");
        repo.commit("base");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "master-version");
        repo.add("a.txt");
        repo.commit("master change");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "dev-version");
        repo.add("a.txt");
        repo.commit("dev change");
        repo.rebase("master");
        String content = TestSupport.readFile(dir, "a.txt");
        assertTrue(content.contains("<<<<<<< HEAD"));
        assertTrue(content.contains("dev-version"));
        assertTrue(content.contains("master-version"));
    }

    @Test
    void rebaseRejectsDirtyStaging(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "a");
        repo.add("a.txt");
        repo.commit("a");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "b.txt", "b");
        repo.add("b.txt");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rebase("dev"));
        assertTrue(ex.getMessage().contains("uncommitted"));
    }
}
