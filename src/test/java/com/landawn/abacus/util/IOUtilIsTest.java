package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilIsTest extends IOUtilTestSupport {
    @Test
    public void testIsBufferedReader_BufferedReader() throws Exception {
        Reader reader = new java.io.BufferedReader(new StringReader("test"));
        assertTrue(IOUtil.isBufferedReader(reader));
    }

    @Test
    public void testIsBufferedReader_NonBufferedReader() throws Exception {
        Reader reader = new StringReader("test");
        assertTrue(!IOUtil.isBufferedReader(reader));
    }

    @Test
    public void testIsBufferedReader_FileReader() throws Exception {
        Reader reader = new FileReader(tempFile);
        try {
            assertTrue(!IOUtil.isBufferedReader(reader));
        } finally {
            reader.close();
        }
    }

    @Test
    public void testIsBufferedWriter_BufferedWriter() throws Exception {
        Writer writer = new java.io.BufferedWriter(new java.io.StringWriter());
        assertTrue(IOUtil.isBufferedWriter(writer));
    }

    @Test
    public void testIsBufferedWriter_NonBufferedWriter() throws Exception {
        Writer writer = new java.io.StringWriter();
        assertTrue(!IOUtil.isBufferedWriter(writer));
    }

    @Test
    public void testIsFileNewer_WithDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        java.util.Date pastDate = new java.util.Date(System.currentTimeMillis() - 10000);

        boolean result = IOUtil.isFileNewer(file, pastDate);
        assertTrue(result);
    }

    @Test
    public void testIsFileNewer_WithFutureDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 10000);

        boolean result = IOUtil.isFileNewer(file, futureDate);
        assertTrue(!result);
    }

    @Test
    public void testIsFileNewer_WithReferenceFile() throws Exception {
        File oldFile = Files.createTempFile(tempFolder, "old", ".txt").toFile();
        Thread.sleep(100);
        File newFile = Files.createTempFile(tempFolder, "new", ".txt").toFile();

        boolean result = IOUtil.isFileNewer(newFile, oldFile);
        assertTrue(result);
    }

    @Test
    public void testIsFileOlder_WithDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "older", ".txt").toFile();
        java.util.Date futureDate = new java.util.Date(System.currentTimeMillis() + 10000);

        boolean result = IOUtil.isFileOlder(file, futureDate);
        assertTrue(result);
    }

    @Test
    public void testIsFileOlder_WithPastDate() throws Exception {
        File file = Files.createTempFile(tempFolder, "older", ".txt").toFile();
        java.util.Date pastDate = new java.util.Date(System.currentTimeMillis() - 10000);

        boolean result = IOUtil.isFileOlder(file, pastDate);
        assertTrue(!result);
    }

    @Test
    public void testIsFileOlder_WithReferenceFile() throws Exception {
        File oldFile = Files.createTempFile(tempFolder, "old", ".txt").toFile();
        Thread.sleep(100);
        File newFile = Files.createTempFile(tempFolder, "new", ".txt").toFile();

        boolean result = IOUtil.isFileOlder(oldFile, newFile);
        assertTrue(result);
    }

    @Test
    public void testIsFile_Null() {
        assertFalse(IOUtil.isFile(null));
    }

    @Test
    public void testIsFile_NonExisting() {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");
        assertFalse(IOUtil.isFile(file));
    }

    @Test
    public void testIsFile_ExistingFile() throws Exception {
        assertTrue(IOUtil.isFile(tempFile));
    }

    @Test
    public void testIsFile_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(!IOUtil.isFile(dir));
    }

    @Test
    public void testIsDirectory_Null() {
        assertFalse(IOUtil.isDirectory(null));
    }

    @Test
    public void testIsDirectory_NonExisting() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent_dir_test");
        assertFalse(IOUtil.isDirectory(nonExistent));
    }

    @Test
    public void testIsDirectory_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(IOUtil.isDirectory(dir));
    }

    @Test
    public void testIsDirectory_File() throws Exception {
        assertFalse(IOUtil.isDirectory(tempFile));
    }

    @Test
    public void testIsDirectory_WithLinkOptions() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(IOUtil.isDirectory(dir, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFile_NullFile() {
        assertTrue(!IOUtil.isRegularFile(null, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    // ========== Additional edge case tests ==========

    @Test
    public void testIsRegularFile_NonExisting() {
        File nonExistent = new File(tempFolder.toFile(), "does_not_exist.txt");
        assertFalse(IOUtil.isRegularFile(nonExistent));
    }

    @Test
    public void testIsRegularFile_ExistingFile() throws Exception {
        assertTrue(IOUtil.isRegularFile(tempFile, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFile_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "dir").toFile();
        assertTrue(!IOUtil.isRegularFile(dir, java.nio.file.LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsSymbolicLink_NullFile() {
        assertTrue(!IOUtil.isSymbolicLink(null));
    }

    @Test
    public void testIsSymbolicLink_RegularFile() throws Exception {
        assertTrue(!IOUtil.isSymbolicLink(tempFile));
    }

    @Test
    public void testIsSymbolicLink_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "symlink-test-dir").toFile();
        assertFalse(IOUtil.isSymbolicLink(dir));
    }

    @Test
    public void testIsFileNewerAndOlder_nonExistingFileIsNeither() throws Exception {
        final File missing = new File(tempFolder.toFile(), "definitely-missing.txt");
        final File reference = Files.createTempFile(tempFolder, "reference", ".txt").toFile();

        assertFalse(IOUtil.isFileNewer(missing, new java.util.Date()));
        assertFalse(IOUtil.isFileOlder(missing, new java.util.Date()));
        assertFalse(IOUtil.isFileNewer(missing, reference));
        assertFalse(IOUtil.isFileOlder(missing, reference));

        // Also for a pre-epoch reference, which used to make a missing file compare as "older".
        assertFalse(IOUtil.isFileOlder(missing, new java.util.Date(-1)));
    }

    @Test
    public void testIsFileNewerAndOlder_existingFileIsExactlyOne() throws Exception {
        final File f = Files.createTempFile(tempFolder, "exists", ".txt").toFile();
        final java.util.Date past = new java.util.Date(f.lastModified() - 10_000);
        final java.util.Date future = new java.util.Date(f.lastModified() + 10_000);

        assertTrue(IOUtil.isFileNewer(f, past));
        assertFalse(IOUtil.isFileOlder(f, past));

        assertFalse(IOUtil.isFileNewer(f, future));
        assertTrue(IOUtil.isFileOlder(f, future));
    }

    @Test
    public void nioPredicates_invalidPathReturnsFalse() {
        assumeWindows();
        final File ads = new File(tempFolder.toFile(), "f.txt:stream");
        assertFalse(IOUtil.isDirectory(ads));
        assertFalse(IOUtil.isDirectory(ads, java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isRegularFile(ads, java.nio.file.LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isSymbolicLink(ads));
    }

    private static void assumeWindows() {
        org.junit.jupiter.api.Assumptions.assumeTrue(System.getProperty("os.name", "").toLowerCase().contains("win"));
    }
}
