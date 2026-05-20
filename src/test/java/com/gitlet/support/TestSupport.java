package com.gitlet.support;

import com.gitlet.Repository;
import com.gitlet.util.FileUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class TestSupport {

    private TestSupport() {
    }

    public static Repository initRepoIn(File dir) {
        Repository repo = new Repository(dir.getAbsolutePath());
        repo.init();
        return repo;
    }

    public static Repository repoIn(File dir) {
        return new Repository(dir.getAbsolutePath());
    }

    public static File writeWorkingFile(File dir, String name, String content) {
        File file = new File(dir, name);
        FileUtils.writeContents(file, content);
        return file;
    }

    public static String readFile(File dir, String name) {
        File file = new File(dir, name);
        if (!file.exists()) {
            return null;
        }
        return FileUtils.readContentsAsString(file);
    }

    public static String captureStdout(Runnable action) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream original = System.out;
        System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
        try {
            action.run();
        } finally {
            System.setOut(original);
        }
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
