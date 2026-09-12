package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.math.BigInteger;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilSizeTest extends IOUtilTestSupport {

    @Test
    public void testSizeOf() throws Exception {
        assertEquals(TEST_CONTENT.getBytes(UTF_8).length, IOUtil.sizeOf(tempFile));
        assertEquals(TEST_CONTENT.length(), IOUtil.sizeOf(tempFile, false));
        assertEquals(TEST_CONTENT.length(), IOUtil.sizeOf(tempFile, true));
    }

    @Test
    public void testSizeOf_Empty() throws Exception {
        assertEquals(0, IOUtil.sizeOf(emptyFile));
    }

    @Test
    public void testSizeOf_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_dir").toFile();
        Files.write(new File(dir, "file1.txt").toPath(), "12345".getBytes());
        Files.write(new File(dir, "file2.txt").toPath(), "67890".getBytes());
        assertEquals(10, IOUtil.sizeOf(dir));
        assertTrue(IOUtil.sizeOf(dir, true) >= 10);
    }

    @Test
    public void testSizeOf_Null() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOf(null));
        assertEquals(0L, IOUtil.sizeOf(null, true));
    }

    @Test
    public void testSizeOf_Missing() {
        File missing = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOf(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOf(missing, false));
        assertEquals(0, IOUtil.sizeOf(missing, true));
    }

    @Test
    public void testSizeOfDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_dir").toFile();
        Files.write(new File(dir, "file1.txt").toPath(), "123".getBytes());
        Files.write(new File(dir, "file2.txt").toPath(), "4567".getBytes());
        assertEquals(7, IOUtil.sizeOfDirectory(dir));
    }

    @Test
    public void testSizeOfDirectory_Nested() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        Files.write(new File(dir, "file1.txt").toPath(), "12".getBytes());
        Files.write(new File(subDir, "file2.txt").toPath(), "345".getBytes());
        assertEquals(5, IOUtil.sizeOfDirectory(dir));
    }

    @Test
    public void testSizeOfDirectory_Empty() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "size_empty").toFile();
        assertEquals(0, IOUtil.sizeOfDirectory(dir));
    }

    @Test
    public void testSizeOfDirectory_Missing() {
        File missing = new File(tempFolder.toFile(), "nonexistent_dir");
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfDirectory(missing));
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfDirectory(missing, false));
        assertEquals(0, IOUtil.sizeOfDirectory(missing, true));
        assertEquals(0L, IOUtil.sizeOfDirectory(null, true));
    }

    @Test
    public void testSizeOfAsBigInteger() throws Exception {
        assertEquals(BigInteger.valueOf(TEST_CONTENT.getBytes(UTF_8).length), IOUtil.sizeOfAsBigInteger(tempFile));
    }

    @Test
    public void testSizeOfAsBigInteger_Empty() throws Exception {
        assertEquals(BigInteger.ZERO, IOUtil.sizeOfAsBigInteger(emptyFile));
    }

    @Test
    public void testSizeOfAsBigInteger_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_dir").toFile();
        Files.write(new File(dir, "file1.txt").toPath(), "12345".getBytes());
        Files.write(new File(dir, "file2.txt").toPath(), "67890".getBytes());
        assertEquals(BigInteger.valueOf(10), IOUtil.sizeOfAsBigInteger(dir));
    }

    @Test
    public void testSizeOfAsBigInteger_Missing() {
        File missing = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfAsBigInteger(missing));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfAsBigInteger(null));
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_dir").toFile();
        Files.write(new File(dir, "file1.txt").toPath(), "123".getBytes());
        Files.write(new File(dir, "file2.txt").toPath(), "4567".getBytes());
        assertEquals(BigInteger.valueOf(7), IOUtil.sizeOfDirectoryAsBigInteger(dir));
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Nested() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        Files.write(new File(dir, "file1.txt").toPath(), "12".getBytes());
        Files.write(new File(subDir, "file2.txt").toPath(), "345".getBytes());
        assertEquals(BigInteger.valueOf(5), IOUtil.sizeOfDirectoryAsBigInteger(dir));
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Empty() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "bigint_empty").toFile();
        assertEquals(BigInteger.ZERO, IOUtil.sizeOfDirectoryAsBigInteger(dir));
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger_Invalid() {
        File missing = tempFolder.resolve("missing_big_integer_directory").toFile();
        assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOfDirectoryAsBigInteger(missing));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfDirectoryAsBigInteger(tempFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfDirectory(tempFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfDirectoryAsBigInteger(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfDirectory(null));
    }

    @Test
    public void testSizeOf_MissingWrapsFileNotFoundInAnUncheckedIOException() {
        final File missing = new File(tempFolder.toFile(), "missing_file.tmp");

        final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> IOUtil.sizeOf(missing, false));
        assertInstanceOf(FileNotFoundException.class, e.getCause());
        assertEquals(0, IOUtil.sizeOf(missing, true));
    }
}
