package com.gitlet.storage;

import com.gitlet.util.FileUtils;

import java.io.File;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public final class StagingArea {

    private final File additionDir;
    private final File removalDir;

    public StagingArea(File additionDir, File removalDir) {
        this.additionDir = additionDir;
        this.removalDir = removalDir;
    }

    public File getAdditionDir() {
        return additionDir;
    }

    public File getRemovalDir() {
        return removalDir;
    }

    public File[] filesStagedForAddition() {
        File[] files = additionDir.listFiles(File::isFile);
        return files == null ? new File[0] : files;
    }

    public File[] filesStagedForRemoval() {
        File[] files = removalDir.listFiles(File::isFile);
        return files == null ? new File[0] : files;
    }

    public List<String> namesStagedForAddition() {
        String[] names = additionDir.list();
        if (names == null) {
            return Collections.emptyList();
        }
        return new java.util.ArrayList<>(new TreeSet<>(java.util.Arrays.asList(names)));
    }

    public List<String> namesStagedForRemoval() {
        String[] names = removalDir.list();
        if (names == null) {
            return Collections.emptyList();
        }
        return new java.util.ArrayList<>(new TreeSet<>(java.util.Arrays.asList(names)));
    }

    public Set<String> allStagedNames() {
        Set<String> all = new LinkedHashSet<>();
        all.addAll(namesStagedForAddition());
        all.addAll(namesStagedForRemoval());
        return all;
    }

    public boolean isEmpty() {
        return filesStagedForAddition().length == 0 && filesStagedForRemoval().length == 0;
    }

    public void stageForAddition(String fileName, String hash) {
        File file = FileUtils.join(additionDir, fileName);
        FileUtils.writeContents(file, hash);
    }

    public void unstageAddition(String fileName) {
        File file = FileUtils.join(additionDir, fileName);
        if (file.exists()) {
            file.delete();
        }
    }

    public void stageForRemoval(String fileName, String hash) {
        File file = FileUtils.join(removalDir, fileName);
        FileUtils.writeContents(file, hash);
    }

    public boolean unstageRemoval(String fileName) {
        File file = FileUtils.join(removalDir, fileName);
        return file.exists() && file.delete();
    }

    public boolean isStagedForAddition(String fileName) {
        return FileUtils.join(additionDir, fileName).exists();
    }

    public boolean isStagedForRemoval(String fileName) {
        return FileUtils.join(removalDir, fileName).exists();
    }

    public String stagedAdditionHash(String fileName) {
        File file = FileUtils.join(additionDir, fileName);
        if (!file.exists()) {
            return null;
        }
        return FileUtils.readContentsAsString(file);
    }

    public void clear() {
        for (File file : filesStagedForAddition()) {
            file.delete();
        }
        for (File file : filesStagedForRemoval()) {
            file.delete();
        }
    }
}
