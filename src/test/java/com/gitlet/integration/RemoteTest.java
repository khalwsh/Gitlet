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

class RemoteTest {

    @Test
    void addRemoteRequiresGitletDirectory(@TempDir File dir, @TempDir File foreignRoot) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.addRemote("origin", foreignRoot.getAbsolutePath()));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void addAndRemoveRemoteRoundtrip(@TempDir File dir, @TempDir File foreignRoot) {
        Repository repo = TestSupport.initRepoIn(dir);
        Repository foreign = TestSupport.initRepoIn(foreignRoot);
        String foreignPath = new File(foreignRoot, ".gitlet").getAbsolutePath();
        repo.addRemote("origin", foreignPath);
        assertTrue(new File(dir, ".gitlet/remotes/origin/origin").isFile());
        repo.removeRemote("origin");
        assertTrue(!new File(dir, ".gitlet/remotes/origin").exists());
    }

    @Test
    void duplicateAddRemoteFails(@TempDir File dir, @TempDir File foreignRoot) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.initRepoIn(foreignRoot);
        String foreignPath = new File(foreignRoot, ".gitlet").getAbsolutePath();
        repo.addRemote("origin", foreignPath);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.addRemote("origin", foreignPath));
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void fetchPullPushIntegratesTwoRepos(@TempDir File alice, @TempDir File bob) {
        Repository repoAlice = TestSupport.initRepoIn(alice);
        TestSupport.writeWorkingFile(alice, "a.txt", "alice");
        repoAlice.add("a.txt");
        repoAlice.commit("alice c1");

        Repository repoBob = TestSupport.initRepoIn(bob);
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        String pullOut = TestSupport.captureStdout(() -> repoBob.pull("alice", "master"));
        assertTrue(pullOut.contains("fast-forwarded"));
        assertEquals("alice", TestSupport.readFile(bob, "a.txt"));

        TestSupport.writeWorkingFile(bob, "b.txt", "bob");
        repoBob.add("b.txt");
        repoBob.commit("bob c1");
        repoBob.push("alice", "master");

        Repository freshAlice = TestSupport.repoIn(alice);
        freshAlice.reset(currentHeadOfRemote(alice, alicePath));
        assertEquals("bob", TestSupport.readFile(alice, "b.txt"));
    }

    @Test
    void pushFailsWhenDivergent(@TempDir File alice, @TempDir File bob) {
        Repository repoAlice = TestSupport.initRepoIn(alice);
        TestSupport.writeWorkingFile(alice, "a.txt", "alice");
        repoAlice.add("a.txt");
        repoAlice.commit("alice c1");

        Repository repoBob = TestSupport.initRepoIn(bob);
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        repoBob.fetch("alice", "master");

        TestSupport.writeWorkingFile(alice, "a.txt", "alice2");
        repoAlice.add("a.txt");
        repoAlice.commit("alice c2");

        TestSupport.writeWorkingFile(bob, "b.txt", "bob");
        repoBob.add("b.txt");
        repoBob.commit("bob c1");

        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repoBob.push("alice", "master"));
        assertTrue(ex.getMessage().toLowerCase().contains("pull"));
    }

    @Test
    void pushCreatesBranchOnFreshRemote(@TempDir File alice, @TempDir File bob) {
        TestSupport.initRepoIn(alice);
        Repository repoBob = TestSupport.initRepoIn(bob);
        TestSupport.writeWorkingFile(bob, "b.txt", "bob");
        repoBob.add("b.txt");
        repoBob.commit("bob c1");
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        repoBob.push("alice", "new-branch");
        assertTrue(new File(alicePath, "branches/new-branch").isFile());
    }

    @Test
    void pushReportsAlreadyUpToDate(@TempDir File alice, @TempDir File bob) {
        Repository repoAlice = TestSupport.initRepoIn(alice);
        TestSupport.writeWorkingFile(alice, "a.txt", "alice");
        repoAlice.add("a.txt");
        repoAlice.commit("alice c1");
        Repository repoBob = TestSupport.initRepoIn(bob);
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        String pullOut = TestSupport.captureStdout(() -> repoBob.pull("alice", "master"));
        assertTrue(pullOut.contains("fast-forwarded"));
        String pushOut = TestSupport.captureStdout(() -> repoBob.push("alice", "master"));
        assertTrue(pushOut.contains("up-to-date"));
    }

    @Test
    void fetchCopiesCommitsAndWritesLocalRef(@TempDir File alice, @TempDir File bob) {
        Repository repoAlice = TestSupport.initRepoIn(alice);
        TestSupport.writeWorkingFile(alice, "a.txt", "alice");
        repoAlice.add("a.txt");
        repoAlice.commit("alice c1");
        Repository repoBob = TestSupport.initRepoIn(bob);
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        repoBob.fetch("alice", "master");
        File aliceRef = new File(bob, ".gitlet/remotes/alice/master");
        assertTrue(aliceRef.isFile());
        File aliceCommits = new File(bob, ".gitlet/commits");
        assertTrue(aliceCommits.list().length >= 2);
    }

    @Test
    void fetchFailsOnMissingRemoteBranch(@TempDir File alice, @TempDir File bob) {
        TestSupport.initRepoIn(alice);
        Repository repoBob = TestSupport.initRepoIn(bob);
        String alicePath = new File(alice, ".gitlet").getAbsolutePath();
        repoBob.addRemote("alice", alicePath);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repoBob.fetch("alice", "nope"));
        assertTrue(ex.getMessage().contains("does not have that branch"));
    }

    private String currentHeadOfRemote(File aliceRoot, String alicePath) {
        File branchFile = new File(alicePath, "branches/master");
        com.gitlet.model.Branch b = com.gitlet.util.FileUtils.readObject(
                branchFile, com.gitlet.model.Branch.class);
        return b.getReferredCommitHash();
    }
}
