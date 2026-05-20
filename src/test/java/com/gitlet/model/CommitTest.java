package com.gitlet.model;

import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommitTest {

    @Test
    void initialCommitHashIsDeterministic() {
        Commit one = new Commit(new Date(0), "initial commit");
        Commit two = new Commit(new Date(0), "initial commit");
        assertEquals(one.getCommitHash(), two.getCommitHash());
    }

    @Test
    void differentMessagesProduceDifferentHashes() {
        Commit one = new Commit(new Date(0), "first");
        Commit two = new Commit(new Date(0), "second");
        assertNotEquals(one.getCommitHash(), two.getCommitHash());
    }

    @Test
    void differentTimestampsProduceDifferentHashes() {
        Commit one = new Commit(new Date(0), "msg");
        Commit two = new Commit(new Date(1), "msg");
        assertNotEquals(one.getCommitHash(), two.getCommitHash());
    }

    @Test
    void trackedFilesAffectHash() {
        Map<String, String> filesA = Map.of("a.txt", "hash1");
        Map<String, String> filesB = Map.of("a.txt", "hash2");
        Commit a = new Commit(new Date(0), "m", null, null, filesA);
        Commit b = new Commit(new Date(0), "m", null, null, filesB);
        assertNotEquals(a.getCommitHash(), b.getCommitHash());
    }

    @Test
    void trackedFilesAreCopiedDefensively() {
        Map<String, String> source = new TreeMap<>();
        source.put("a.txt", "h1");
        Commit c = new Commit(new Date(0), "m", null, null, source);
        source.put("b.txt", "h2");
        assertFalse(c.trackedFiles().containsKey("b.txt"));
    }

    @Test
    void parentsAreExposedCorrectly() {
        Commit c = new Commit(new Date(0), "m", "other-parent", "main-parent",
                new TreeMap<>());
        assertEquals("main-parent", c.getParentCommitHash());
        assertEquals("other-parent", c.getSecondParentHash());
    }

    @Test
    void equalsIsBasedOnHash() {
        Commit one = new Commit(new Date(0), "x");
        Commit two = new Commit(new Date(0), "x");
        Commit three = new Commit(new Date(0), "y");
        assertEquals(one, two);
        assertEquals(one.hashCode(), two.hashCode());
        assertNotEquals(one, three);
    }

    @Test
    void toStringContainsHashAndMessage() {
        Commit c = new Commit(new Date(0), "hello");
        String s = c.toString();
        assertTrue(s.contains(c.getCommitHash()));
        assertTrue(s.contains("hello"));
        assertTrue(s.contains("Date:"));
    }

    @Test
    void toStringShowsMergeLineWhenBothParentsPresent() {
        Commit c = new Commit(new Date(0), "merge", "abcdef0123", "1234567890",
                new TreeMap<>());
        String s = c.toString();
        assertTrue(s.contains("Merge:"));
    }

    @Test
    void singleArgConstructorHasEmptyTrackedFiles() {
        Commit c = new Commit(new Date(0), "m");
        assertNotNull(c.trackedFiles());
        assertTrue(c.trackedFiles().isEmpty());
    }

    @Test
    void equalsHandlesNullAndForeignTypes() {
        Commit c = new Commit(new Date(0), "m");
        assertNotEquals(null, c);
        assertNotEquals("not a commit", c);
    }

    @Test
    void trackedFilesViewIsUnmodifiable() {
        Commit c = new Commit(new Date(0), "m", null, null, Map.of("x", "h1"));
        Map<String, String> view = c.trackedFiles();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> view.put("y", "h2"));
    }

    @Test
    void shortHashHandlesUnderSevenCharParents() {
        Commit c = new Commit(new Date(0), "m", "ab", "cd", new java.util.TreeMap<>());
        String s = c.toString();
        assertTrue(s.contains("Merge: cd ab"));
    }
}
