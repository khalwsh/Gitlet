package com.gitlet.util;

import com.gitlet.model.Branch;
import com.gitlet.model.Commit;
import com.gitlet.storage.CommitStore;
import com.gitlet.storage.RemoteStore;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

public final class CommitGraph {

    private CommitGraph() {
    }

    public static List<Commit> firstParentChain(Commit head, CommitStore commitStore) {
        List<Commit> chain = new ArrayList<>();
        Commit cur = head;
        while (cur != null) {
            chain.add(cur);
            String parent = cur.getParentCommitHash();
            cur = (parent != null) ? commitStore.getCommit(parent) : null;
        }
        return chain;
    }

    public static List<Commit> firstParentChainRemote(Commit head, String remoteName, RemoteStore remoteStore) {
        List<Commit> chain = new ArrayList<>();
        Commit cur = head;
        while (cur != null) {
            chain.add(cur);
            String parent = cur.getParentCommitHash();
            cur = (parent != null) ? remoteStore.getRemoteCommit(parent, remoteName) : null;
        }
        return chain;
    }

    public static List<Commit> reachable(Commit root, String remoteName,
                                         CommitStore commitStore, RemoteStore remoteStore) {
        List<Commit> result = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        Deque<Commit> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            Commit c = stack.pop();
            if (c == null || !seen.add(c.getCommitHash())) {
                continue;
            }
            result.add(c);
            Commit p1 = resolve(c.getParentCommitHash(), remoteName, commitStore, remoteStore);
            Commit p2 = resolve(c.getSecondParentHash(), remoteName, commitStore, remoteStore);
            if (p1 != null) stack.push(p1);
            if (p2 != null) stack.push(p2);
        }
        return result;
    }

    public static Commit splitPoint(Branch local, Branch other, String remoteName,
                                    CommitStore commitStore, RemoteStore remoteStore) {
        Commit a = commitStore.getCommit(local.getReferredCommitHash());
        Commit b = (remoteName != null)
                ? remoteStore.getRemoteCommit(other.getReferredCommitHash(), remoteName)
                : commitStore.getCommit(other.getReferredCommitHash());
        if (a == null || b == null) {
            return null;
        }

        Set<String> ancestorsOfA = new HashSet<>();
        Deque<Commit> stack = new ArrayDeque<>();
        stack.push(a);
        while (!stack.isEmpty()) {
            Commit c = stack.pop();
            if (c == null || !ancestorsOfA.add(c.getCommitHash())) {
                continue;
            }
            Commit p1 = resolve(c.getParentCommitHash(), null, commitStore, remoteStore);
            Commit p2 = resolve(c.getSecondParentHash(), null, commitStore, remoteStore);
            if (p1 != null) stack.push(p1);
            if (p2 != null) stack.push(p2);
        }

        Queue<Commit> bfs = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        bfs.add(b);
        visited.add(b.getCommitHash());
        while (!bfs.isEmpty()) {
            Commit c = bfs.poll();
            if (ancestorsOfA.contains(c.getCommitHash())) {
                return c;
            }
            Commit p1 = resolve(c.getParentCommitHash(), remoteName, commitStore, remoteStore);
            Commit p2 = resolve(c.getSecondParentHash(), remoteName, commitStore, remoteStore);
            if (p1 != null && visited.add(p1.getCommitHash())) bfs.add(p1);
            if (p2 != null && visited.add(p2.getCommitHash())) bfs.add(p2);
        }
        return null;
    }

    private static Commit resolve(String hash, String remoteName,
                                  CommitStore commitStore, RemoteStore remoteStore) {
        if (hash == null) {
            return null;
        }
        Commit local = commitStore.getCommit(hash);
        if (local != null) {
            return local;
        }
        if (remoteName != null) {
            return remoteStore.getRemoteCommit(hash, remoteName);
        }
        return null;
    }
}
