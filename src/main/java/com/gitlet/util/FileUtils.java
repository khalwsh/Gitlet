package com.gitlet.util;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public final class FileUtils {

    public static final int UID_LENGTH = 40;
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    private static final FilenameFilter PLAIN_FILES = (dir, name) -> new File(dir, name).isFile();

    private FileUtils() {
    }

    public static String sha1(Object... vals) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            for (Object val : vals) {
                if (val instanceof byte[]) {
                    md.update((byte[]) val);
                } else if (val instanceof String) {
                    md.update(((String) val).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to sha1");
                }
            }
            return toHex(md.digest());
        } catch (NoSuchAlgorithmException excp) {
            throw new IllegalArgumentException("System does not support SHA-1");
        }
    }

    public static String sha1(List<Object> vals) {
        return sha1(vals.toArray(new Object[0]));
    }

    private static String toHex(byte[] bytes) {
        char[] out = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            out[i * 2] = HEX_DIGITS[b >>> 4];
            out[i * 2 + 1] = HEX_DIGITS[b & 0x0F];
        }
        return new String(out);
    }

    public static byte[] readContents(File file) {
        if (!file.isFile()) {
            throw new IllegalArgumentException("must be a normal file");
        }
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static String readContentsAsString(File file) {
        return new String(readContents(file), StandardCharsets.UTF_8);
    }

    public static void writeContents(File file, Object... contents) {
        if (file.isDirectory()) {
            throw new IllegalArgumentException("cannot overwrite directory");
        }
        try (OutputStream out = new BufferedOutputStream(Files.newOutputStream(file.toPath()))) {
            for (Object obj : contents) {
                if (obj instanceof byte[]) {
                    out.write((byte[]) obj);
                } else if (obj instanceof String) {
                    out.write(((String) obj).getBytes(StandardCharsets.UTF_8));
                } else {
                    throw new IllegalArgumentException("improper type to writeContents");
                }
            }
        } catch (IOException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static <T extends Serializable> T readObject(File file, Class<T> expectedClass) {
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            return expectedClass.cast(in.readObject());
        } catch (IOException | ClassCastException | ClassNotFoundException excp) {
            throw new IllegalArgumentException(excp.getMessage());
        }
    }

    public static void writeObject(File file, Serializable obj) {
        writeContents(file, serialize(obj));
    }

    public static List<String> plainFilenamesIn(File dir) {
        String[] files = dir.list(PLAIN_FILES);
        if (files == null) {
            return List.of();
        }
        Arrays.sort(files);
        return Arrays.asList(files);
    }

    public static List<String> plainFilenamesIn(String dir) {
        return plainFilenamesIn(new File(dir));
    }

    public static File join(String first, String... others) {
        return Paths.get(first, others).toFile();
    }

    public static File join(File first, String... others) {
        return Paths.get(first.getPath(), others).toFile();
    }

    public static byte[] serialize(Serializable obj) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(buffer)) {
            out.writeObject(obj);
        } catch (IOException excp) {
            throw new GitletException("Internal error serializing object.");
        }
        return buffer.toByteArray();
    }

    public static GitletException error(String msg, Object... args) {
        return new GitletException(String.format(msg, args));
    }

    public static void exitWithMessage(String msg) {
        throw new GitletExitException(msg);
    }
}
