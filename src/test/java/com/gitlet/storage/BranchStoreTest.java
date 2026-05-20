package com.gitlet.storage;

import com.gitlet.model.Branch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchStoreTest {

    @Test
    void saveGetExistsRoundtrip(@TempDir File dir) {
        BranchStore store = makeStore(dir);
        Branch b = new Branch("master", "abc");
        store.saveBranch(b);
        assertTrue(store.exists("master"));
        Branch read = store.getBranch("master");
        assertNotNull(read);
        assertEquals(b, read);
    }

    @Test
    void getReturnsNullForMissingBranch(@TempDir File dir) {
        BranchStore store = makeStore(dir);
        assertNull(store.getBranch("nope"));
        assertFalse(store.exists("nope"));
    }

    @Test
    void deleteBranchRemovesIt(@TempDir File dir) {
        BranchStore store = makeStore(dir);
        store.saveBranch(new Branch("dev", "x"));
        assertTrue(store.deleteBranch("dev"));
        assertFalse(store.exists("dev"));
    }

    @Test
    void deleteBranchReturnsFalseWhenMissing(@TempDir File dir) {
        BranchStore store = makeStore(dir);
        assertFalse(store.deleteBranch("ghost"));
    }

    @Test
    void getAllBranchNamesReturnsSortedList(@TempDir File dir) {
        BranchStore store = makeStore(dir);
        store.saveBranch(new Branch("zeta", "z"));
        store.saveBranch(new Branch("alpha", "a"));
        store.saveBranch(new Branch("master", "m"));
        List<String> names = store.getAllBranchNames();
        assertEquals(List.of("alpha", "master", "zeta"), names);
    }

    private BranchStore makeStore(File dir) {
        File branchesDir = new File(dir, "branches");
        branchesDir.mkdirs();
        return new BranchStore(branchesDir);
    }
}
