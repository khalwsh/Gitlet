package com.gitlet.storage;

import com.gitlet.model.Commit;
import com.gitlet.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class CommitStore {

    private final File commitsDir;

    public CommitStore(File commitsDir) {
        this.commitsDir = commitsDir;
    }

    public Set<String> getCommitHashes() {
        Set<String> set = new HashSet<>();
        File[] files = commitsDir.listFiles();
        if (files == null) {
            return set;
        }
        for (File file : files) {
            set.add(file.getName());
        }
        return set;
    }

    public void saveCommit(Commit commit) {
        File file = FileUtils.join(commitsDir, commit.getCommitHash());
        FileUtils.writeObject(file, commit);
    }

    public Commit getCommit(String commitHash) {
        if (commitHash == null) {
            return null;
        }
        File file = FileUtils.join(commitsDir, commitHash);
        if (!file.isFile()) {
            return null;
        }
        return FileUtils.readObject(file, Commit.class);
    }

    public Commit findByPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) {
            return null;
        }
        if (prefix.length() == FileUtils.UID_LENGTH) {
            return getCommit(prefix);
        }
        File[] files = commitsDir.listFiles();
        if (files == null) {
            return null;
        }
        Commit match = null;
        for (File file : files) {
            if (file.getName().startsWith(prefix)) {
                if (match != null) {
                    return null;
                }
                match = FileUtils.readObject(file, Commit.class);
            }
        }
        return match;
    }

    public List<Commit> getAllCommits() {
        List<Commit> commits = new ArrayList<>();
        File[] files = commitsDir.listFiles();
        if (files == null) {
            return commits;
        }
        for (File file : files) {
            commits.add(FileUtils.readObject(file, Commit.class));
        }
        return commits;
    }
}
