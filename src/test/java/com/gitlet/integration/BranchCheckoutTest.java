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

class BranchCheckoutTest {

    @Test
    void branchCreatesNewRef(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        repo.branch("dev");
        assertTrue(new File(dir, ".gitlet/branches/dev").isFile());
    }

    @Test
    void branchRejectsDuplicateName(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        repo.branch("dev");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.branch("dev"));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void branchRejectsExistingMaster(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.branch("master"));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void checkoutBranchSwitchesHeadAndWorkingTree(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "master");
        repo.add("a.txt");
        repo.commit("master a");
        repo.branch("dev");
        TestSupport.writeWorkingFile(dir, "a.txt", "master-v2");
        repo.add("a.txt");
        repo.commit("master a v2");
        repo.checkoutBranch("dev");
        assertEquals("master", TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void checkoutBranchRejectsCurrent(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutBranch("master"));
        assertTrue(ex.getMessage().contains("current branch"));
    }

    @Test
    void checkoutBranchRejectsMissing(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutBranch("ghost"));
        assertTrue(ex.getMessage().contains("No such branch"));
    }

    @Test
    void checkoutFileRestoresFromHead(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        repo.commit("v1");
        TestSupport.writeWorkingFile(dir, "a.txt", "modified");
        repo.checkoutFile("a.txt");
        assertEquals("v1", TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void checkoutFileFailsForMissingTrackedFile(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutFile("ghost.txt"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void checkoutFileByPrefixWorks(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        repo.commit("v1");
        String hash = new File(dir, ".gitlet/branches/master").exists()
                ? findLatestCommitHash(dir)
                : "";
        TestSupport.writeWorkingFile(dir, "a.txt", "v2");
        repo.add("a.txt");
        repo.commit("v2");
        repo.checkoutFileByCommit(hash.substring(0, 6), "a.txt");
        assertEquals("v1", TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void checkoutBlocksOnUntrackedConflict(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("v1");
        repo.branch("dev");
        repo.checkoutBranch("dev");
        TestSupport.writeWorkingFile(dir, "b.txt", "dev-only");
        repo.add("b.txt");
        repo.commit("dev b");
        repo.checkoutBranch("master");
        TestSupport.writeWorkingFile(dir, "b.txt", "untracked-conflict");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutBranch("dev"));
        assertTrue(ex.getMessage().contains("untracked"));
    }

    @Test
    void rmBranchRemovesPointer(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        repo.branch("dev");
        repo.rmBranch("dev");
        assertTrue(!new File(dir, ".gitlet/branches/dev").exists());
    }

    @Test
    void rmBranchRejectsCurrent(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rmBranch("master"));
        assertTrue(ex.getMessage().contains("Cannot remove"));
    }

    @Test
    void rmBranchRejectsMissing(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rmBranch("ghost"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void checkoutFileByCommitFailsWhenFileMissing(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        repo.commit("v1");
        String hash = findLatestCommitHash(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutFileByCommit(hash, "ghost.txt"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void checkoutFileByCommitFailsWhenCommitMissing(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.checkoutFileByCommit("deadbeef", "ghost.txt"));
        assertTrue(ex.getMessage().contains("No commit"));
    }

    private String findLatestCommitHash(File dir) {
        File branchFile = new File(dir, ".gitlet/branches/master");
        com.gitlet.model.Branch branch =
                com.gitlet.util.FileUtils.readObject(branchFile, com.gitlet.model.Branch.class);
        return branch.getReferredCommitHash();
    }
}
