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

class InitCommitTest {

    @Test
    void initCreatesGitletStructure(@TempDir File dir) {
        TestSupport.initRepoIn(dir);
        File gitlet = new File(dir, ".gitlet");
        assertTrue(gitlet.isDirectory());
        assertTrue(new File(gitlet, "commits").isDirectory());
        assertTrue(new File(gitlet, "blobs").isDirectory());
        assertTrue(new File(gitlet, "branches").isDirectory());
        assertTrue(new File(gitlet, "branches/master").isFile());
        assertTrue(new File(gitlet, "staged/addition").isDirectory());
        assertTrue(new File(gitlet, "staged/removal").isDirectory());
        assertTrue(new File(gitlet, "remotes").isDirectory());
        assertTrue(new File(gitlet, "HEAD").isFile());
    }

    @Test
    void initialCommitHashIsStableAcrossInits(@TempDir File a, @TempDir File b) {
        TestSupport.initRepoIn(a);
        TestSupport.initRepoIn(b);
        String hashA = new File(a, ".gitlet/commits").list()[0];
        String hashB = new File(b, ".gitlet/commits").list()[0];
        assertEquals(hashA, hashB);
    }

    @Test
    void doubleInitFails(@TempDir File dir) {
        TestSupport.initRepoIn(dir);
        Repository repo = TestSupport.repoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class, repo::init);
        assertTrue(ex.getMessage().contains("already exists"));
    }

    @Test
    void addRequiresExistingFile(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.add("ghost.txt"));
        assertTrue(ex.getMessage().contains("does not exist"));
    }

    @Test
    void commitWithoutStagingFails(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.commit("nothing"));
        assertTrue(ex.getMessage().contains("No changes"));
    }

    @Test
    void commitWithEmptyMessageFails(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.commit(""));
        assertTrue(ex.getMessage().toLowerCase().contains("message"));
    }

    @Test
    void addThenCommitTracksFile(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "alpha");
        repo.add("a.txt");
        repo.commit("add a");
        String[] commits = new File(dir, ".gitlet/commits").list();
        assertEquals(2, commits.length);
    }

    @Test
    void addingUnchangedFileRemovesStagedEntry(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("v1");
        repo.add("a.txt");
        assertEquals(0, new File(dir, ".gitlet/staged/addition").list().length);
    }

    @Test
    void rmStagesRemovalAndDeletesFromWorkingDirectory(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("add");
        repo.rm("a.txt");
        assertTrue(new File(dir, ".gitlet/staged/removal/a.txt").isFile());
        assertEquals(null, TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void rmOfUntrackedFileFails(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.rm("ghost.txt"));
        assertTrue(ex.getMessage().contains("No reason"));
    }

    @Test
    void logShowsCommitsBackToInitial(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "1");
        repo.add("a.txt");
        repo.commit("first");
        String out = TestSupport.captureStdout(repo::log);
        assertTrue(out.contains("first"));
        assertTrue(out.contains("initial commit"));
    }

    @Test
    void globalLogIncludesEveryCommit(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "1");
        repo.add("a.txt");
        repo.commit("first");
        String out = TestSupport.captureStdout(repo::globalLog);
        long markers = out.lines().filter("==="::equals).count();
        assertEquals(2, markers);
    }

    @Test
    void findLocatesByExactMessage(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "1");
        repo.add("a.txt");
        repo.commit("special");
        String out = TestSupport.captureStdout(() -> repo.find("special"));
        assertEquals(1, out.lines().filter(s -> s.length() == 40).count());
    }

    @Test
    void findReportsWhenNothingMatches(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.find("missing"));
        assertTrue(ex.getMessage().contains("Found no commit"));
    }
}
