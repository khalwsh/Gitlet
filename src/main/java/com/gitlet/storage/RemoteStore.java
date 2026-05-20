package com.gitlet.storage;

import com.gitlet.model.Branch;
import com.gitlet.model.Commit;
import com.gitlet.util.FileUtils;

import java.io.File;

public final class RemoteStore {

    private final File remoteDir;

    public RemoteStore(File remoteDir) {
        this.remoteDir = remoteDir;
    }

    public void addRemotePath(String remoteName, String remotePath) {
        File dir = FileUtils.join(remoteDir, remoteName);
        if (dir.exists()) {
            FileUtils.exitWithMessage("A remote with that name already exists.");
        }
        if (!dir.mkdirs()) {
            FileUtils.exitWithMessage("Failed to create the remote directory structure.");
        }
        File pathFile = FileUtils.join(dir, remoteName);
        FileUtils.writeContents(pathFile, remotePath);
    }

    public void removeRemotePath(String remoteName) {
        File dir = FileUtils.join(remoteDir, remoteName);
        if (!dir.exists()) {
            FileUtils.exitWithMessage("A remote with that name does not exist.");
        }
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        dir.delete();
    }

    public String getRemotePath(String remoteName) {
        File pathFile = FileUtils.join(remoteDir, remoteName, remoteName);
        if (!pathFile.exists()) {
            return null;
        }
        return FileUtils.readContentsAsString(pathFile);
    }

    public Branch getRemoteBranch(String remoteName, String remoteBranchName) {
        String remotePath = getRemotePath(remoteName);
        if (remotePath == null) {
            return null;
        }
        File branchFile = FileUtils.join(remotePath, "branches", remoteBranchName);
        if (!branchFile.exists()) {
            return null;
        }
        return FileUtils.readObject(branchFile, Branch.class);
    }

    public Branch getRemoteBranchFromLocal(String remoteName, String remoteBranch) {
        File branchFile = FileUtils.join(remoteDir, remoteName, remoteBranch);
        if (!branchFile.exists()) {
            return null;
        }
        return FileUtils.readObject(branchFile, Branch.class);
    }

    public void saveRemoteBranchAtLocal(String remoteName, Branch remoteBranch) {
        File branchFile = FileUtils.join(remoteDir, remoteName, remoteBranch.getName());
        File parent = branchFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileUtils.writeObject(branchFile, remoteBranch);
    }

    public void saveRemoteBranch(String remoteName, Branch remoteBranch) {
        String remotePath = getRemotePath(remoteName);
        if (remotePath == null) {
            FileUtils.exitWithMessage("Remote does not exist.");
        }
        File branchFile = FileUtils.join(remotePath, "branches", remoteBranch.getName());
        File parent = branchFile.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
        FileUtils.writeObject(branchFile, remoteBranch);
    }

    public Commit getRemoteCommit(String commitHash, String remoteName) {
        if (commitHash == null) {
            return null;
        }
        String remotePath = getRemotePath(remoteName);
        if (remotePath == null) {
            return null;
        }
        File file = FileUtils.join(remotePath, "commits", commitHash);
        if (!file.isFile()) {
            return null;
        }
        return FileUtils.readObject(file, Commit.class);
    }
}
