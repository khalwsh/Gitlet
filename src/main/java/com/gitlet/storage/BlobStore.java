package com.gitlet.storage;

import com.gitlet.util.FileUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;

public final class BlobStore {

    private static final byte[] EMPTY = new byte[0];

    private final File blobsDir;

    public BlobStore(File blobsDir) {
        this.blobsDir = blobsDir;
    }

    public String saveBlob(byte[] content) {
        String hash = FileUtils.sha1(content);
        File blobFile = FileUtils.join(blobsDir, hash);
        FileUtils.writeContents(blobFile, content);
        return hash;
    }

    public String saveBlob(File source) {
        return saveBlob(FileUtils.readContents(source));
    }

    public String saveBlobContent(String content) {
        return saveBlob(content.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] getBlobBytes(String hash) {
        if (hash == null) {
            return EMPTY;
        }
        File file = FileUtils.join(blobsDir, hash);
        if (!file.isFile()) {
            return EMPTY;
        }
        return FileUtils.readContents(file);
    }

    public String getBlobContent(String hash) {
        return new String(getBlobBytes(hash), StandardCharsets.UTF_8);
    }
}
