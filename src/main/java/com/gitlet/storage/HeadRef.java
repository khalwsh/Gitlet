package com.gitlet.storage;

import com.gitlet.util.FileUtils;

import java.io.File;

public final class HeadRef {

    private final File headFile;

    public HeadRef(File headFile) {
        this.headFile = headFile;
    }

    public void set(String branchName) {
        FileUtils.writeContents(headFile, branchName);
    }

    public String get() {
        return FileUtils.readContentsAsString(headFile);
    }
}
