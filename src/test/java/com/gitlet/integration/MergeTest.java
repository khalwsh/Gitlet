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

class MergeTest {

    @Test
    void mergeRejectsMissingBranch(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.merge("ghost", null));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void mergeRejectsSelf(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.merge("master", null));
        assertTrue(ex.getMessage().contains("with itself"));
    }

    @Test
    void mergeRejectsWhenStagingDirty(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.merge("dev", null));
        assertTrue(ex.getMessage().contains("uncommitted"));
    }

    @Test
    void mergeOfAncestorReports(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("first");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "y");
        repo.add("a.txt");
        repo.commit("master-only");
        String out = TestSupport.captureStdout(() -> repo.merge("dev", null));
        assertTrue(out.contains("ancestor"));
    }

    @Test
    void mergeFastForwards(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("first");
        repo.branch("dev");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "b.txt", "dev");
        repo.add("b.txt");
        repo.commit("dev b");
        repo.checkoutBranch("master");
        String out = TestSupport.captureStdout(() -> repo.merge("dev", null));
        assertTrue(out.contains("fast-forwarded"));
        assertEquals("dev", TestSupport.readFile(dir, "b.txt"));
    }

    @Test
    void mergeBringsInChangesFromOther(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "base");
        repo.add("a.txt");
        repo.commit("base");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "m.txt", "master");
        repo.add("m.txt");
        repo.commit("master m");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "d.txt", "dev");
        repo.add("d.txt");
        repo.commit("dev d");
        repo.checkoutBranch("master");
        repo.merge("dev", null);
        assertEquals("master", TestSupport.readFile(dir, "m.txt"));
        assertEquals("dev", TestSupport.readFile(dir, "d.txt"));
        assertEquals("base", TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void mergeWithConflictWritesMarkers(@TempDir File dir) {
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
        repo.checkoutBranch("master");
        String out = TestSupport.captureStdout(() -> repo.merge("dev", null));
        assertTrue(out.contains("Encountered a merge conflict"));
        String content = TestSupport.readFile(dir, "a.txt");
        assertTrue(content.contains("<<<<<<< HEAD"));
        assertTrue(content.contains("master-version"));
        assertTrue(content.contains("======="));
        assertTrue(content.contains("dev-version"));
        assertTrue(content.contains(">>>>>>>"));
    }

    @Test
    void mergeModifyVsDeleteIsConflict(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "base");
        repo.add("a.txt");
        repo.commit("base");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "master-edit");
        repo.add("a.txt");
        repo.commit("master edit");
        repo.checkoutBranch("dev");
        repo.rm("a.txt");
        repo.commit("dev delete");
        repo.checkoutBranch("master");
        String out = TestSupport.captureStdout(() -> repo.merge("dev", null));
        assertTrue(out.contains("Encountered a merge conflict"));
        String content = TestSupport.readFile(dir, "a.txt");
        assertTrue(content.contains("<<<<<<< HEAD"));
        assertTrue(content.contains("master-edit"));
    }

    @Test
    void mergeBothAddDifferentIsConflict(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "seed.txt", "seed");
        repo.add("seed.txt");
        repo.commit("base");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "fresh.txt", "master-fresh");
        repo.add("fresh.txt");
        repo.commit("master adds fresh");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "fresh.txt", "dev-fresh");
        repo.add("fresh.txt");
        repo.commit("dev adds fresh");
        repo.checkoutBranch("master");
        String out = TestSupport.captureStdout(() -> repo.merge("dev", null));
        assertTrue(out.contains("Encountered a merge conflict"));
        String content = TestSupport.readFile(dir, "fresh.txt");
        assertTrue(content.contains("master-fresh"));
        assertTrue(content.contains("dev-fresh"));
    }

    @Test
    void mergeWithDeletionInOtherStagesRemoval(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("base");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "master.txt", "m");
        repo.add("master.txt");
        repo.commit("master only");
        repo.checkoutBranch("dev");
        repo.rm("a.txt");
        repo.commit("delete a");
        repo.checkoutBranch("master");
        repo.merge("dev", null);
        assertEquals(null, TestSupport.readFile(dir, "a.txt"));
        assertEquals("m", TestSupport.readFile(dir, "master.txt"));
    }
}
