package com.gitlet.storage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HeadRefTest {

    @Test
    void setAndGetRoundtrip(@TempDir File dir) throws IOException {
        File headFile = new File(dir, "HEAD");
        headFile.createNewFile();
        HeadRef head = new HeadRef(headFile);
        head.set("master");
        assertEquals("master", head.get());
        head.set("dev");
        assertEquals("dev", head.get());
    }
}
