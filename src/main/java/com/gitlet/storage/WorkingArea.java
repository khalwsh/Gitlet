package com.gitlet.storage;

import com.gitlet.util.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.List;

public final class WorkingArea {

    private final File workingDir;

    public WorkingArea(File workingDir) {
        this.workingDir = workingDir;
    }

    public File getWorkingDir() {
        return workingDir;
    }

    public List<String> plainFileNames() {
        List<String> names = FileUtils.plainFilenamesIn(workingDir);
        return names == null ? Collections.emptyList() : names;
    }

    public File[] plainFiles() {
        File[] files = workingDir.listFiles(File::isFile);
        return files == null ? new File[0] : files;
    }

    public File getFile(String fileName) {
        File file = FileUtils.join(workingDir, fileName);
        return file.exists() ? file : null;
    }

    public File getPlainFile(String fileName) {
        File file = FileUtils.join(workingDir, fileName);
        return file.isFile() ? file : null;
    }

    public File saveFile(String content, String fileName) {
        File file = FileUtils.join(workingDir, fileName);
        FileUtils.writeContents(file, content);
        return file;
    }

    public File saveBytes(byte[] content, String fileName) {
        File file = FileUtils.join(workingDir, fileName);
        FileUtils.writeContents(file, content);
        return file;
    }

    public boolean deleteFile(String fileName) {
        File file = FileUtils.join(workingDir, fileName);
        return file.exists() && file.delete();
    }

    public void clear() {
        File[] files = workingDir.listFiles(File::isFile);
        if (files == null) {
            return;
        }
        for (File file : files) {
            file.delete();
        }
    }
}
