package com.gitlet.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileUtilsTest {

    @Test
    void sha1IsDeterministicForSameInput() {
        String a = FileUtils.sha1("hello", "world");
        String b = FileUtils.sha1("hello", "world");
        assertEquals(a, b);
    }

    @Test
    void sha1DiffersForDifferentInput() {
        assertNotEquals(FileUtils.sha1("a"), FileUtils.sha1("b"));
    }

    @Test
    void sha1HandlesByteArrays() {
        byte[] bytes = {1, 2, 3};
        String h1 = FileUtils.sha1(bytes);
        String h2 = FileUtils.sha1((Object) bytes);
        assertNotNull(h1);
        assertEquals(h1, h2);
    }

    @Test
    void sha1ListVariantMatchesArrayVariant() {
        String fromArray = FileUtils.sha1("a", "b", "c");
        String fromList = FileUtils.sha1(List.of((Object) "a", "b", "c"));
        assertEquals(fromArray, fromList);
    }

    @Test
    void sha1RejectsInvalidType() {
        assertThrows(IllegalArgumentException.class, () -> FileUtils.sha1(42));
    }

    @Test
    void writeAndReadContentsRoundtrip(@TempDir File dir) {
        File file = new File(dir, "hello.txt");
        FileUtils.writeContents(file, "hi");
        assertEquals("hi", FileUtils.readContentsAsString(file));
    }

    @Test
    void writeContentsRejectsDirectory(@TempDir File dir) {
        assertThrows(IllegalArgumentException.class,
                () -> FileUtils.writeContents(dir, "oops"));
    }

    @Test
    void readContentsRejectsMissingFile(@TempDir File dir) {
        File missing = new File(dir, "nope.txt");
        assertThrows(IllegalArgumentException.class,
                () -> FileUtils.readContents(missing));
    }

    @Test
    void joinComposesPaths(@TempDir File dir) {
        File composed = FileUtils.join(dir, "a", "b", "c.txt");
        assertTrue(composed.getPath().endsWith("a" + File.separator + "b" + File.separator + "c.txt"));
    }

    @Test
    void plainFilenamesInListsOnlyPlainFiles(@TempDir File dir) {
        new File(dir, "sub").mkdir();
        FileUtils.writeContents(new File(dir, "a.txt"), "x");
        FileUtils.writeContents(new File(dir, "b.txt"), "y");
        List<String> names = FileUtils.plainFilenamesIn(dir);
        assertEquals(List.of("a.txt", "b.txt"), names);
    }

    @Test
    void plainFilenamesReturnsEmptyForMissingDirectory(@TempDir File dir) {
        File missing = new File(dir, "nope");
        assertTrue(FileUtils.plainFilenamesIn(missing).isEmpty());
    }

    @Test
    void serializeAndDeserializeRoundtrip(@TempDir File dir) {
        Bean bean = new Bean("foo", 42);
        File file = new File(dir, "bean.bin");
        FileUtils.writeObject(file, bean);
        Bean read = FileUtils.readObject(file, Bean.class);
        assertEquals(bean, read);
    }

    @Test
    void exitWithMessageThrowsGitletExitException() {
        GitletExitException ex = assertThrows(GitletExitException.class,
                () -> FileUtils.exitWithMessage("boom"));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void errorBuildsFormattedException() {
        GitletException ex = FileUtils.error("oops %s", "bar");
        assertEquals("oops bar", ex.getMessage());
        assertEquals(GitletException.class, ex.getClass());
    }

    @Test
    void writeContentsHandlesByteArray(@TempDir File dir) {
        File file = new File(dir, "bin.dat");
        byte[] payload = {0x00, 0x01, (byte) 0xFF, 0x7F};
        FileUtils.writeContents(file, payload);
        byte[] read = FileUtils.readContents(file);
        org.junit.jupiter.api.Assertions.assertArrayEquals(payload, read);
    }

    @Test
    void writeContentsRejectsInvalidType(@TempDir File dir) {
        File file = new File(dir, "x.txt");
        assertThrows(IllegalArgumentException.class, () -> FileUtils.writeContents(file, 42));
    }

    @Test
    void sha1AcceptsMixedTypes() {
        byte[] bytes = {0x01, 0x02};
        String mixed = FileUtils.sha1(bytes, "tail");
        String reference = FileUtils.sha1(bytes, "tail");
        assertEquals(mixed, reference);
        assertNotEquals(mixed, FileUtils.sha1("tail", bytes));
    }

    private static final class Bean implements Serializable {
        @Serial
        private static final long serialVersionUID = 1L;
        final String name;
        final int value;

        Bean(String name, int value) {
            this.name = name;
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Bean)) return false;
            Bean other = (Bean) obj;
            return value == other.value && Objects.equals(name, other.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }
}
