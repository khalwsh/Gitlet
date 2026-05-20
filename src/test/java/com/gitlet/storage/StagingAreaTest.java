package com.gitlet.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagingAreaTest {

    private StagingArea staging;

    @BeforeEach
    void setUp(@TempDir File dir) {
        File addition = new File(dir, "addition");
        File removal = new File(dir, "removal");
        addition.mkdirs();
        removal.mkdirs();
        staging = new StagingArea(addition, removal);
    }

    @Test
    void freshAreaIsEmpty() {
        assertTrue(staging.isEmpty());
        assertEquals(0, staging.filesStagedForAddition().length);
        assertEquals(0, staging.filesStagedForRemoval().length);
        assertTrue(staging.namesStagedForAddition().isEmpty());
        assertTrue(staging.namesStagedForRemoval().isEmpty());
    }

    @Test
    void stageForAdditionWritesHashAsContent() {
        staging.stageForAddition("foo.txt", "abc123");
        assertTrue(staging.isStagedForAddition("foo.txt"));
        assertEquals("abc123", staging.stagedAdditionHash("foo.txt"));
        assertFalse(staging.isEmpty());
    }

    @Test
    void unstageAdditionRemovesEntry() {
        staging.stageForAddition("foo.txt", "abc");
        staging.unstageAddition("foo.txt");
        assertFalse(staging.isStagedForAddition("foo.txt"));
        assertNull(staging.stagedAdditionHash("foo.txt"));
    }

    @Test
    void stageForRemovalWritesAndUnstages() {
        staging.stageForRemoval("foo.txt", "abc");
        assertTrue(staging.isStagedForRemoval("foo.txt"));
        assertTrue(staging.unstageRemoval("foo.txt"));
        assertFalse(staging.isStagedForRemoval("foo.txt"));
        assertFalse(staging.unstageRemoval("foo.txt"));
    }

    @Test
    void isStagedForRemovalReturnsFalseInitially() {
        assertFalse(staging.isStagedForRemoval("foo.txt"));
    }

    @Test
    void namesAreSortedForAdditionAndRemoval() {
        staging.stageForAddition("c", "1");
        staging.stageForAddition("a", "2");
        staging.stageForAddition("b", "3");
        assertEquals(List.of("a", "b", "c"), staging.namesStagedForAddition());

        staging.stageForRemoval("z", "9");
        staging.stageForRemoval("y", "8");
        assertEquals(List.of("y", "z"), staging.namesStagedForRemoval());
    }

    @Test
    void allStagedNamesUnionsBothDirectories() {
        staging.stageForAddition("add1", "h");
        staging.stageForRemoval("rem1", "h");
        Set<String> all = staging.allStagedNames();
        assertEquals(Set.of("add1", "rem1"), all);
    }

    @Test
    void clearWipesBothDirectories() {
        staging.stageForAddition("a", "1");
        staging.stageForRemoval("b", "2");
        staging.clear();
        assertTrue(staging.isEmpty());
    }
}
