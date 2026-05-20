package com.gitlet.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BranchTest {

    @Test
    void getsBackTheConstructorArgs() {
        Branch b = new Branch("master", "abc");
        assertEquals("master", b.getName());
        assertEquals("abc", b.getReferredCommitHash());
    }

    @Test
    void setCommitUpdatesHashOnly() {
        Branch b = new Branch("master", "abc");
        b.setCommit("def");
        assertEquals("master", b.getName());
        assertEquals("def", b.getReferredCommitHash());
    }

    @Test
    void equalsAndHashCode() {
        Branch a = new Branch("master", "abc");
        Branch b = new Branch("master", "abc");
        Branch c = new Branch("dev", "abc");
        Branch d = new Branch("master", "xyz");
        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertNotEquals(a, d);
    }
}
