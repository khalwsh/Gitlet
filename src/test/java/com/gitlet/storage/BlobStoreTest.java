package com.gitlet.storage;

import com.gitlet.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlobStoreTest {

    @Test
    void saveBlobReturnsContentHashAndStoresContent(@TempDir File dir) {
        File blobsDir = new File(dir, "blobs");
        blobsDir.mkdirs();
        BlobStore store = new BlobStore(blobsDir);

        File source = new File(dir, "src.txt");
        FileUtils.writeContents(source, "hello world");

        String hash = store.saveBlob(source);
        assertEquals(FileUtils.sha1("hello world"), hash);
        assertEquals("hello world", store.getBlobContent(hash));
    }

    @Test
    void identicalContentDeduplicates(@TempDir File dir) {
        File blobsDir = new File(dir, "blobs");
        blobsDir.mkdirs();
        BlobStore store = new BlobStore(blobsDir);

        String h1 = store.saveBlobContent("payload");
        String h2 = store.saveBlobContent("payload");
        assertEquals(h1, h2);
        assertEquals(1, blobsDir.list().length);
    }

    @Test
    void getBlobContentReturnsEmptyForMissingHash(@TempDir File dir) {
        File blobsDir = new File(dir, "blobs");
        blobsDir.mkdirs();
        BlobStore store = new BlobStore(blobsDir);
        assertEquals("", store.getBlobContent("nope"));
        assertEquals("", store.getBlobContent(null));
    }
}
