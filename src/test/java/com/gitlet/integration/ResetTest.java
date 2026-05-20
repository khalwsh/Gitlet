package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.model.Branch;
import com.gitlet.support.TestSupport;
import com.gitlet.util.FileUtils;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResetTest {

    @Test
    void resetMovesBranchAndRestoresTree(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        repo.commit("v1");
        String firstHash = currentHead(dir);

        TestSupport.writeWorkingFile(dir, "a.txt", "v2");
        repo.add("a.txt");
        repo.commit("v2");

        repo.reset(firstHash);
        assertEquals("v1", TestSupport.readFile(dir, "a.txt"));
        assertEquals(firstHash, currentHead(dir));
    }

    @Test
    void resetAcceptsShortHash(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        repo.add("a.txt");
        repo.commit("v1");
        String hash = currentHead(dir);

        TestSupport.writeWorkingFile(dir, "a.txt", "v2");
        repo.add("a.txt");
        repo.commit("v2");

        repo.reset(hash.substring(0, 6));
        assertEquals("v1", TestSupport.readFile(dir, "a.txt"));
    }

    @Test
    void resetFailsForMissingHash(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.reset("deadbeef"));
        assertTrue(ex.getMessage().contains("No commit"));
    }

    @Test
    void resetBlocksOnUntrackedConflict(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        TestSupport.writeWorkingFile(dir, "shared.txt", "future-version");
        repo.add("shared.txt");
        repo.commit("future commit");
        String futureHash = currentHead(dir);
        repo.rm("shared.txt");
        repo.commit("drop shared");
        TestSupport.writeWorkingFile(dir, "shared.txt", "manually-recreated");
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> repo.reset(futureHash));
        assertTrue(ex.getMessage().contains("untracked"));
    }

    private String currentHead(File dir) {
        File branchFile = new File(dir, ".gitlet/branches/master");
        Branch b = FileUtils.readObject(branchFile, Branch.class);
        return b.getReferredCommitHash();
    }
}
