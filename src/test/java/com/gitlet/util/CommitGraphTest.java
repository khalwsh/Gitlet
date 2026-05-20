package com.gitlet.util;

import com.gitlet.model.Branch;
import com.gitlet.model.Commit;
import com.gitlet.storage.CommitStore;
import com.gitlet.storage.RemoteStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitGraphTest {

    private File commitsDir;
    private CommitStore commitStore;
    private RemoteStore remoteStore;

    @BeforeEach
    void setUp(@TempDir File dir) {
        commitsDir = new File(dir, "commits");
        commitsDir.mkdirs();
        commitStore = new CommitStore(commitsDir);
        remoteStore = new RemoteStore(new File(dir, "remotes"));
    }

    @Test
    void firstParentChainWalksOneCommit() {
        Commit initial = saveCommit("first", null, null);
        List<Commit> chain = CommitGraph.firstParentChain(initial, commitStore);
        assertEquals(1, chain.size());
        assertEquals(initial, chain.get(0));
    }

    @Test
    void firstParentChainFollowsParentsOnly() {
        Commit a = saveCommit("a", null, null);
        Commit b = saveCommit("b", a.getCommitHash(), null);
        Commit c = saveCommit("c", b.getCommitHash(), a.getCommitHash());
        List<Commit> chain = CommitGraph.firstParentChain(c, commitStore);
        assertEquals(List.of(c, b, a), chain);
    }

    @Test
    void splitPointOnLinearHistoryReturnsBranchPoint() {
        Commit a = saveCommit("a", null, null);
        Commit b = saveCommit("b", a.getCommitHash(), null);
        Branch left = new Branch("master", b.getCommitHash());
        Branch right = new Branch("dev", a.getCommitHash());
        Commit split = CommitGraph.splitPoint(left, right, null, commitStore, remoteStore);
        assertNotNull(split);
        assertEquals(a, split);
    }

    @Test
    void splitPointOnDivergedBranchesReturnsCommonAncestor() {
        Commit a = saveCommit("a", null, null);
        Commit b = saveCommit("b", a.getCommitHash(), null);
        Commit c = saveCommit("c", a.getCommitHash(), null);
        Branch left = new Branch("master", b.getCommitHash());
        Branch right = new Branch("dev", c.getCommitHash());
        Commit split = CommitGraph.splitPoint(left, right, null, commitStore, remoteStore);
        assertEquals(a, split);
    }

    @Test
    void splitPointWhenOneIsAncestorOfOther() {
        Commit a = saveCommit("a", null, null);
        Commit b = saveCommit("b", a.getCommitHash(), null);
        Commit c = saveCommit("c", b.getCommitHash(), null);
        Branch left = new Branch("master", c.getCommitHash());
        Branch right = new Branch("dev", b.getCommitHash());
        Commit split = CommitGraph.splitPoint(left, right, null, commitStore, remoteStore);
        assertEquals(b, split);
    }

    @Test
    void splitPointWithBrokenHistoryReturnsNullWhenNothingShared() {
        Commit a = new Commit(new Date(0), "a", null, "ghost-hash",
                Map.of("x", "h1"));
        commitStore.saveCommit(a);
        Branch left = new Branch("master", a.getCommitHash());
        Branch right = new Branch("dev", "missing-commit");
        Commit split = CommitGraph.splitPoint(left, right, null, commitStore, remoteStore);
        assertNull(split);
    }

    @Test
    void reachableIncludesAllAncestors() {
        Commit a = saveCommit("a", null, null);
        Commit b = saveCommit("b", a.getCommitHash(), null);
        Commit c = saveCommit("c", b.getCommitHash(), null);
        List<Commit> all = CommitGraph.reachable(c, null, commitStore, remoteStore);
        assertEquals(3, all.size());
        assertTrue(all.contains(a));
        assertTrue(all.contains(b));
        assertTrue(all.contains(c));
    }

    @Test
    void firstParentChainHandlesNullParent() {
        Commit lonely = saveCommit("lonely", null, null);
        List<Commit> chain = CommitGraph.firstParentChain(lonely, commitStore);
        assertEquals(List.of(lonely), chain);
    }

    @Test
    void splitPointWalksMergeCommitSecondParent() {
        Commit base = saveCommit("base", null, null);
        Commit left = saveCommit("left", base.getCommitHash(), null);
        Commit right = saveCommit("right", base.getCommitHash(), null);
        Commit merged = saveCommit("merge", left.getCommitHash(), right.getCommitHash());
        Commit afterRight = saveCommit("afterRight", right.getCommitHash(), null);
        Branch a = new Branch("mainline", merged.getCommitHash());
        Branch b = new Branch("side", afterRight.getCommitHash());
        Commit split = CommitGraph.splitPoint(a, b, null, commitStore, remoteStore);
        assertNotNull(split);
        assertEquals(right, split);
    }

    private Commit saveCommit(String message, String parent, String secondParent) {
        Map<String, String> files = new TreeMap<>();
        files.put(message + ".txt", FileUtils.sha1(message));
        Commit c = new Commit(new Date(0), message, secondParent, parent, files);
        commitStore.saveCommit(c);
        return c;
    }
}
