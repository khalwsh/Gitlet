package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.support.TestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusTest {

    @Test
    void statusOnFreshRepo(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        String out = TestSupport.captureStdout(repo::status);
        assertTrue(out.contains("=== Branches ==="));
        assertTrue(out.contains("*master"));
        assertTrue(out.contains("=== Staged Files ==="));
        assertTrue(out.contains("=== Removed Files ==="));
        assertTrue(out.contains("=== Modifications Not Staged For Commit ==="));
        assertTrue(out.contains("=== Untracked Files ==="));
    }

    @Test
    void statusShowsStagedAdditionsAndRemovals(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "tracked.txt", "x");
        repo.add("tracked.txt");
        repo.commit("v1");

        TestSupport.writeWorkingFile(dir, "added.txt", "y");
        repo.add("added.txt");
        repo.rm("tracked.txt");

        String out = TestSupport.captureStdout(repo::status);
        assertTrue(out.contains("added.txt"));
        assertTrue(out.contains("tracked.txt"));
    }

    @Test
    void statusReportsModifiedNotStaged(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("v1");
        TestSupport.writeWorkingFile(dir, "a.txt", "modified");
        String out = TestSupport.captureStdout(repo::status);
        assertTrue(out.contains("a.txt (modified)"));
    }

    @Test
    void statusReportsDeletedTrackedFile(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "x");
        repo.add("a.txt");
        repo.commit("v1");
        new File(dir, "a.txt").delete();
        String out = TestSupport.captureStdout(repo::status);
        assertTrue(out.contains("a.txt (deleted)"));
    }

    @Test
    void statusReportsUntrackedFiles(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "untracked.txt", "x");
        String out = TestSupport.captureStdout(repo::status);
        int idx = out.indexOf("=== Untracked Files ===");
        assertTrue(idx >= 0);
        assertTrue(out.substring(idx).contains("untracked.txt"));
    }

    @Test
    void statusReportsStagedButLocallyModified(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        TestSupport.writeWorkingFile(dir, "a.txt", "v2");
        String out = TestSupport.captureStdout(repo::status);
        int idx = out.indexOf("=== Modifications Not Staged");
        assertTrue(idx >= 0);
        assertTrue(out.substring(idx).contains("a.txt (modified)"));
    }

    @Test
    void statusReportsStagedButLocallyDeleted(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        new File(dir, "a.txt").delete();
        String out = TestSupport.captureStdout(repo::status);
        int idx = out.indexOf("=== Modifications Not Staged");
        assertTrue(idx >= 0);
        assertTrue(out.substring(idx).contains("a.txt (deleted)"));
    }

    @Test
    void statusOmitsStagedFromUntracked(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "staged.txt", "x");
        repo.add("staged.txt");
        String out = TestSupport.captureStdout(repo::status);
        int idx = out.indexOf("=== Untracked Files ===");
        assertFalse(out.substring(idx).contains("staged.txt"));
    }
}
