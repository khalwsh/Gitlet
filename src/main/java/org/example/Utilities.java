package org.example;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class Utilities {
    public static void checkGitletExistense(File Gitlet_Dir) {
        if (!Gitlet_Dir.exists()) {
            Utils.exitWithMessage("initialized Gitlet directory doesn't exist.");
        }
    }

    private static void DFS(Commit node, Set<String> visited, List<Commit> list, String remoteName , CommitStore commitStore , RemoteStore remoteStore) {
        list.add(node);
        visited.add(node.getCommitHash());

        String primaryParent = node.getParentCommitHash();
        String secondaryParent = node.getSecondryParent();

        if (primaryParent != null && !visited.contains(primaryParent)) {
            if (remoteName == null) DFS(commitStore.getCommit(primaryParent), visited, list, null , commitStore , remoteStore);
            else DFS(remoteStore.getRemoteCommit(primaryParent, remoteName), visited, list, remoteName , commitStore , remoteStore);
        }

        if (secondaryParent != null && !visited.contains(secondaryParent)) {
            if (remoteName == null) DFS(commitStore.getCommit(secondaryParent), visited, list, null , commitStore , remoteStore);
            else DFS(remoteStore.getRemoteCommit(secondaryParent, remoteName), visited, list, remoteName , commitStore , remoteStore);
        }
    }

    public static List<Commit> getCommitTree(Commit rootCommit, String remoteName, CommitStore commitStore , RemoteStore remoteStore) {
        List<Commit> result = new ArrayList<>();
        DFS(rootCommit, new HashSet<>(), result, remoteName , commitStore , remoteStore);
        return result;
    }

    public static Commit splitPoint(Branch a, Branch b, String remoteName , CommitStore commitStore , RemoteStore remoteStore) {
        Commit A = commitStore.getCommit(a.getReferredCommitHash());
        Commit B;
        if (remoteName != null) {
            B = remoteStore.getRemoteCommit(b.getReferredCommitHash(), remoteName);
        } else B = commitStore.getCommit(b.getReferredCommitHash());

        Set<String> seta = getCommitTree(A, null , commitStore , remoteStore).stream().map(Commit::getCommitHash).collect(Collectors.toSet());
        Set<String> setb = getCommitTree(B, remoteName , commitStore , remoteStore).stream().map(Commit::getCommitHash).collect(Collectors.toSet());

        Date LcaDate = new Date(0);
        Commit Point = null;
        for (String sa : seta) {
            if (setb.contains(sa)) {
                Commit x = commitStore.getCommit(sa);
                if (!x.GetTime().before(LcaDate)) {
                    LcaDate = x.GetTime();
                    Point = x;
                }
            }
        }
        return Point;
    }

    public static void checkRemoteGitletExistenseAndPathValidity(String remotePath) {
        File remoteDir = new File(remotePath);
        if (!remotePath.endsWith(".gitlet") || !remoteDir.isDirectory())
            Utils.exitWithMessage("Remote directory not found.");
    }

    public static Branch getCurrentBranch(BranchStore branchStore , Head head) {
        return branchStore.getBranch(head.getHead());
    }

    public static  Commit getCurrentCommit(CommitStore commitStore , BranchStore branchStore , Head head) {
        String curCommitHash = getCurrentBranch(branchStore , head).getReferredCommitHash();
        return commitStore.getCommit(curCommitHash);
    }

    public static  void FastForward(String remoteName, Branch remoteBranch, BranchStore branchStore , Head head ,CommitStore commitStore , RemoteStore remoteStore) {
        String curCommitHash = getCurrentCommit(commitStore , branchStore , head).getCommitHash();
        remoteBranch.SetCommit(curCommitHash);
        remoteStore.saveRemoteBranch(remoteName, remoteBranch);
    }
}
