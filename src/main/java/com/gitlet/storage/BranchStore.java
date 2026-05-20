package com.gitlet.storage;

import com.gitlet.model.Branch;
import com.gitlet.util.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class BranchStore {

    private final File branchesDir;

    public BranchStore(File branchesDir) {
        this.branchesDir = branchesDir;
    }

    public void saveBranch(Branch branch) {
        File file = FileUtils.join(branchesDir, branch.getName());
        FileUtils.writeObject(file, branch);
    }

    public boolean deleteBranch(String branchName) {
        File file = FileUtils.join(branchesDir, branchName);
        return file.exists() && file.delete();
    }

    public Branch getBranch(String branchName) {
        File file = FileUtils.join(branchesDir, branchName);
        if (!file.exists()) {
            return null;
        }
        return FileUtils.readObject(file, Branch.class);
    }

    public boolean exists(String branchName) {
        return FileUtils.join(branchesDir, branchName).exists();
    }

    public List<String> getAllBranchNames() {
        String[] names = branchesDir.list();
        if (names == null) {
            return Collections.emptyList();
        }
        List<String> sorted = new ArrayList<>(Arrays.asList(names));
        Collections.sort(sorted);
        return sorted;
    }
}
