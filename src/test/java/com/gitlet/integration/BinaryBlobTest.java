package com.gitlet.integration;

import com.gitlet.Repository;
import com.gitlet.storage.BlobStore;
import com.gitlet.support.TestSupport;
import com.gitlet.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class BinaryBlobTest {

    @Test
    void blobStoreRoundTripsRawBytes(@TempDir File dir) {
        File blobsDir = new File(dir, "blobs");
        blobsDir.mkdirs();
        BlobStore store = new BlobStore(blobsDir);
        byte[] payload = new byte[256];
        for (int i = 0; i < 256; i++) {
            payload[i] = (byte) i;
        }
        String hash = store.saveBlob(payload);
        assertArrayEquals(payload, store.getBlobBytes(hash));
    }

    @Test
    void addAndCheckoutPreserveBinaryFile(@TempDir File dir) {
        Repository repo = TestSupport.initRepoIn(dir);
        byte[] payload = new byte[]{
                (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 'I', 'H', 'D', 'R'
        };
        File binaryFile = new File(dir, "image.png");
        FileUtils.writeContents(binaryFile, payload);

        repo.add("image.png");
        repo.commit("add image");

        FileUtils.writeContents(binaryFile, new byte[]{0});
        repo.checkoutFile("image.png");

        byte[] restored = FileUtils.readContents(binaryFile);
        assertArrayEquals(payload, restored);
    }

    @Test
    void blobHashStableForBytes(@TempDir File dir) {
        File blobsDir = new File(dir, "blobs");
        blobsDir.mkdirs();
        BlobStore store = new BlobStore(blobsDir);
        byte[] a = {0x01, (byte) 0xFF, 0x10};
        byte[] b = Arrays.copyOf(a, a.length);
        assertEquals(store.saveBlob(a), store.saveBlob(b));
    }
}
