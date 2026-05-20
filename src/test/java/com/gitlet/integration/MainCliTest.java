package com.gitlet.integration;

import com.gitlet.Main;
import com.gitlet.support.TestSupport;
import com.gitlet.util.GitletExitException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainCliTest {

    private String originalUserDir;

    @BeforeEach
    void rememberCwd() {
        originalUserDir = System.getProperty("user.dir");
    }

    @AfterEach
    void restoreCwd() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    void noArgsExitsWithMessage(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> Main.run(new String[0]));
        assertTrue(ex.getMessage().contains("Please enter"));
    }

    @Test
    void unknownCommandExits(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> Main.run(new String[]{"nope-not-a-command"}));
        assertTrue(ex.getMessage().contains("No command with that name"));
    }

    @Test
    void initDispatchCreatesRepo(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        Main.run(new String[]{"init"});
        assertTrue(new File(dir, ".gitlet").isDirectory());
    }

    @Test
    void addCommandThroughCli(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        Main.run(new String[]{"init"});
        TestSupport.writeWorkingFile(dir, "a.txt", "hi");
        Main.run(new String[]{"add", "a.txt"});
        Main.run(new String[]{"commit", "first"});
        assertTrue(new File(dir, ".gitlet/commits").list().length >= 2);
    }

    @Test
    void initRejectsExtraArguments(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> Main.run(new String[]{"init", "extra"}));
        assertTrue(ex.getMessage().contains("Incorrect operands"));
    }

    @Test
    void checkoutDoubleDashFromHead(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        Main.run(new String[]{"init"});
        TestSupport.writeWorkingFile(dir, "a.txt", "v1");
        Main.run(new String[]{"add", "a.txt"});
        Main.run(new String[]{"commit", "v1"});
        TestSupport.writeWorkingFile(dir, "a.txt", "modified");
        Main.run(new String[]{"checkout", "--", "a.txt"});
        assertTrue("v1".equals(TestSupport.readFile(dir, "a.txt")));
    }

    @Test
    void checkoutFallbackForBadShape(@TempDir File dir) {
        System.setProperty("user.dir", dir.getAbsolutePath());
        Main.run(new String[]{"init"});
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> Main.run(new String[]{"checkout", "a", "b", "c", "d"}));
        assertTrue(ex.getMessage().contains("Incorrect operands"));
    }
}
