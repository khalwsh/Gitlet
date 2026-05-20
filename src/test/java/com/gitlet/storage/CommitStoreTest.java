package com.gitlet.storage;

import com.gitlet.model.Commit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitStoreTest {

    @Test
    void saveAndRetrieveCommit(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        Commit c = new Commit(new Date(0), "first");
        store.saveCommit(c);
        assertEquals(c, store.getCommit(c.getCommitHash()));
    }

    @Test
    void getCommitReturnsNullForMissing(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        assertNull(store.getCommit("does-not-exist"));
        assertNull(store.getCommit(null));
    }

    @Test
    void getCommitHashesListsAllStoredCommits(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        Commit c1 = new Commit(new Date(0), "a");
        Commit c2 = new Commit(new Date(1), "b");
        store.saveCommit(c1);
        store.saveCommit(c2);
        Set<String> hashes = store.getCommitHashes();
        assertTrue(hashes.contains(c1.getCommitHash()));
        assertTrue(hashes.contains(c2.getCommitHash()));
    }

    @Test
    void getAllCommitsReturnsEveryStoredCommit(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        Commit c1 = new Commit(new Date(0), "a");
        Commit c2 = new Commit(new Date(1), "b");
        store.saveCommit(c1);
        store.saveCommit(c2);
        List<Commit> commits = store.getAllCommits();
        assertEquals(2, commits.size());
        assertTrue(commits.contains(c1));
        assertTrue(commits.contains(c2));
    }

    @Test
    void findByPrefixResolvesUniqueMatch(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        Commit c = new Commit(new Date(0), "unique");
        store.saveCommit(c);
        String hash = c.getCommitHash();
        assertEquals(c, store.findByPrefix(hash.substring(0, 6)));
    }

    @Test
    void findByPrefixReturnsNullForMissing(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        assertNull(store.findByPrefix("ffffff"));
        assertNull(store.findByPrefix(null));
        assertNull(store.findByPrefix(""));
    }

    @Test
    void findByPrefixAcceptsFullHash(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        Commit c = new Commit(new Date(0), "full");
        store.saveCommit(c);
        Commit hit = store.findByPrefix(c.getCommitHash());
        assertNotNull(hit);
        assertEquals(c, hit);
    }

    @Test
    void findByPrefixReturnsNullWhenAmbiguous(@TempDir File dir) {
        CommitStore store = makeStore(dir);
        java.util.Map<String, Commit> seen = new java.util.HashMap<>();
        String sharedPrefix = null;
        for (int i = 0; i < 4000 && sharedPrefix == null; i++) {
            Commit c = new Commit(new Date(i), "msg" + i);
            store.saveCommit(c);
            String prefix = c.getCommitHash().substring(0, 2);
            if (seen.containsKey(prefix)) {
                sharedPrefix = prefix;
            } else {
                seen.put(prefix, c);
            }
        }
        assertNotNull(sharedPrefix);
        assertNull(store.findByPrefix(sharedPrefix));
    }

    private CommitStore makeStore(File dir) {
        File commitsDir = new File(dir, "commits");
        commitsDir.mkdirs();
        return new CommitStore(commitsDir);
    }
}
