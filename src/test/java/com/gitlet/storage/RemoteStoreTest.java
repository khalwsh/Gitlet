package com.gitlet.storage;

import com.gitlet.model.Branch;
import com.gitlet.util.FileUtils;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemoteStoreTest {

    private RemoteStore store;
    private File remotesDir;

    @BeforeEach
    void setUp(@TempDir File dir) {
        remotesDir = new File(dir, "remotes");
        remotesDir.mkdirs();
        store = new RemoteStore(remotesDir);
    }

    @Test
    void addRemotePathStoresPath() {
        store.addRemotePath("origin", "/some/path/.gitlet");
        assertEquals("/some/path/.gitlet", store.getRemotePath("origin"));
    }

    @Test
    void addingDuplicateRemoteFails() {
        store.addRemotePath("origin", "/p/.gitlet");
        assertThrows(GitletExitException.class,
                () -> store.addRemotePath("origin", "/other/.gitlet"));
    }

    @Test
    void removeRemoteCleansEntireDirectory() {
        store.addRemotePath("origin", "/p/.gitlet");
        store.removeRemotePath("origin");
        assertNull(store.getRemotePath("origin"));
        assertFalse(new File(remotesDir, "origin").exists());
    }

    @Test
    void removingMissingRemoteFails() {
        assertThrows(GitletExitException.class, () -> store.removeRemotePath("ghost"));
    }

    @Test
    void localRemoteBranchRoundtrip() {
        store.addRemotePath("origin", "/p/.gitlet");
        Branch b = new Branch("master", "abc");
        store.saveRemoteBranchAtLocal("origin", b);
        Branch read = store.getRemoteBranchFromLocal("origin", "master");
        assertEquals(b, read);
    }

    @Test
    void getRemoteCommitReturnsNullWhenRemoteMissing() {
        assertNull(store.getRemoteCommit("anyhash", "unknown"));
        assertNull(store.getRemoteCommit(null, "unknown"));
    }

    @Test
    void getRemoteBranchReturnsNullWhenBranchAbsent(@TempDir File foreignRoot) {
        File foreignGitlet = new File(foreignRoot, ".gitlet");
        new File(foreignGitlet, "branches").mkdirs();
        new File(foreignGitlet, "commits").mkdirs();
        store.addRemotePath("origin", foreignGitlet.getAbsolutePath());
        assertNull(store.getRemoteBranch("origin", "nope"));
    }

    @Test
    void getRemoteCommitReadsFromRemoteDirectory(@TempDir File foreignRoot) {
        File foreignGitlet = new File(foreignRoot, ".gitlet");
        File commitsDir = new File(foreignGitlet, "commits");
        commitsDir.mkdirs();
        com.gitlet.model.Commit c = new com.gitlet.model.Commit(new java.util.Date(0), "x");
        File commitFile = new File(commitsDir, c.getCommitHash());
        FileUtils.writeObject(commitFile, c);

        store.addRemotePath("origin", foreignGitlet.getAbsolutePath());
        com.gitlet.model.Commit read = store.getRemoteCommit(c.getCommitHash(), "origin");
        assertEquals(c, read);
    }

}
