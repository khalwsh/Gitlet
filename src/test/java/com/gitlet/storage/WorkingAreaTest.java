package com.gitlet.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkingAreaTest {

    private File workingDir;
    private WorkingArea area;

    @BeforeEach
    void setUp(@TempDir File dir) {
        workingDir = dir;
        area = new WorkingArea(dir);
    }

    @Test
    void saveAndReadFile() {
        File file = area.saveFile("hi", "a.txt");
        assertTrue(file.isFile());
        assertNotNull(area.getFile("a.txt"));
        assertEquals("a.txt", area.getFile("a.txt").getName());
    }

    @Test
    void getFileReturnsNullForMissing() {
        assertNull(area.getFile("nope.txt"));
    }

    @Test
    void deleteFileRemovesFile() {
        area.saveFile("x", "a.txt");
        assertTrue(area.deleteFile("a.txt"));
        assertNull(area.getFile("a.txt"));
        assertFalse(area.deleteFile("a.txt"));
    }

    @Test
    void plainFileNamesExcludesDirectories() {
        area.saveFile("x", "a.txt");
        new File(workingDir, "subdir").mkdir();
        List<String> names = area.plainFileNames();
        assertTrue(names.contains("a.txt"));
        assertFalse(names.contains("subdir"));
    }

    @Test
    void plainFilesExcludesDirectories() {
        area.saveFile("x", "a.txt");
        new File(workingDir, "subdir").mkdir();
        File[] files = area.plainFiles();
        assertEquals(1, files.length);
        assertEquals("a.txt", files[0].getName());
    }

    @Test
    void clearOnlyDeletesPlainFiles() {
        area.saveFile("x", "a.txt");
        area.saveFile("y", "b.txt");
        new File(workingDir, "subdir").mkdir();
        area.clear();
        assertEquals(0, area.plainFiles().length);
        assertTrue(new File(workingDir, "subdir").exists());
    }
}
