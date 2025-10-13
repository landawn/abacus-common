package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Predicate;
import com.google.common.graph.Traverser;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.RecursiveDeleteOption;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.Files;

@Tag("2025")
public class Files2025Test extends TestBase {

    @TempDir
    Path tempDir;

    private List<File> filesToCleanup;
    private List<MappedByteBuffer> buffersToUnmap;

    @BeforeEach
    public void setUp() {
        filesToCleanup = new ArrayList<>();
        buffersToUnmap = new ArrayList<>();
    }

    @AfterEach
    public void tearDown() throws IOException {
        for (MappedByteBuffer buffer : buffersToUnmap) {
            if (buffer != null) {
                unmap(buffer);
            }
        }
        buffersToUnmap.clear();

        for (File file : filesToCleanup) {
            if (file != null && file.exists()) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        filesToCleanup.clear();
    }

    private void deleteDirectory(File dir) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }

    @Test
    public void testNewReader_BasicRead() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        filesToCleanup.add(file);
        Files.write("Hello World".getBytes(StandardCharsets.UTF_8), file);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            assertEquals("Hello World", reader.readLine());
        }
    }

    @Test
    public void testNewReader_MultipleLines() throws IOException {
        File file = new File(tempDir.toFile(), "multiline.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            assertEquals("Line 1", reader.readLine());
            assertEquals("Line 2", reader.readLine());
            assertEquals("Line 3", reader.readLine());
            assertEquals(null, reader.readLine());
        }
    }

    @Test
    public void testNewReader_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(StandardCharsets.UTF_8), file);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            assertEquals(null, reader.readLine());
        }
    }

    @Test
    public void testNewReader_FileNotFound() {
        File file = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> {
            Files.newReader(file, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testNewReader_DifferentCharsets() throws IOException {
        File file = new File(tempDir.toFile(), "charset.txt");
        filesToCleanup.add(file);
        String content = "Test with special chars: áéíóú";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.ISO_8859_1)) {
            assertEquals(content, reader.readLine());
        }
    }

    @Test
    public void testNewWriter_BasicWrite() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");
        filesToCleanup.add(file);

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Test output");
        }

        assertEquals("Test output", Files.readString(file));
    }

    @Test
    public void testNewWriter_Overwrite() throws IOException {
        File file = new File(tempDir.toFile(), "overwrite.txt");
        filesToCleanup.add(file);
        Files.write("Original".getBytes(), file);

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("New content");
        }

        assertEquals("New content", Files.readString(file));
    }

    @Test
    public void testNewWriter_MultipleWrites() throws IOException {
        File file = new File(tempDir.toFile(), "multiple.txt");
        filesToCleanup.add(file);

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("First ");
            writer.write("Second ");
            writer.write("Third");
        }

        assertEquals("First Second Third", Files.readString(file));
    }

    @Test
    public void testNewWriter_DifferentCharsets() throws IOException {
        File file = new File(tempDir.toFile(), "charset_write.txt");
        filesToCleanup.add(file);
        String content = "Special chars: áéíóú";

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.ISO_8859_1)) {
            writer.write(content);
        }

        assertEquals(content, Files.readString(file, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testAsByteSource_File_BasicRead() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        filesToCleanup.add(file);
        Files.write("Test content".getBytes(), file);

        ByteSource source = Files.asByteSource(file);
        byte[] bytes = source.read();
        assertEquals("Test content", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsByteSource_File_Size() throws IOException {
        File file = new File(tempDir.toFile(), "sized.txt");
        filesToCleanup.add(file);
        byte[] content = "12345".getBytes();
        Files.write(content, file);

        ByteSource source = Files.asByteSource(file);
        assertEquals(5, source.size());
    }

    @Test
    public void testAsByteSource_File_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        ByteSource source = Files.asByteSource(file);
        assertEquals(0, source.size());
        assertArrayEquals(new byte[0], source.read());
    }

    @Test
    public void testAsByteSource_Path_BasicRead() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Test content".getBytes());

        ByteSource source = Files.asByteSource(path, StandardOpenOption.READ);
        byte[] bytes = source.read();
        assertEquals("Test content", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsByteSource_Path_NoOptions() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Test content".getBytes());

        ByteSource source = Files.asByteSource(path);
        byte[] bytes = source.read();
        assertEquals("Test content", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsByteSink_File_BasicWrite() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");
        filesToCleanup.add(file);

        ByteSink sink = Files.asByteSink(file);
        sink.write("Written content".getBytes(StandardCharsets.UTF_8));

        assertEquals("Written content", Files.readString(file));
    }

    @Test
    public void testAsByteSink_File_Append() throws IOException {
        File file = new File(tempDir.toFile(), "append.txt");
        filesToCleanup.add(file);
        Files.write("Initial".getBytes(), file);

        ByteSink sink = Files.asByteSink(file, FileWriteMode.APPEND);
        sink.write(" Appended".getBytes(StandardCharsets.UTF_8));

        assertEquals("Initial Appended", Files.readString(file));
    }

    @Test
    public void testAsByteSink_File_Overwrite() throws IOException {
        File file = new File(tempDir.toFile(), "overwrite.txt");
        filesToCleanup.add(file);
        Files.write("Original".getBytes(), file);

        ByteSink sink = Files.asByteSink(file);
        sink.write("New".getBytes());

        assertEquals("New", Files.readString(file));
    }

    @Test
    public void testAsByteSink_Path_BasicWrite() throws IOException {
        Path path = tempDir.resolve("output.txt");

        ByteSink sink = Files.asByteSink(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        sink.write("Written content".getBytes(StandardCharsets.UTF_8));

        assertEquals("Written content", new String(java.nio.file.Files.readAllBytes(path)));
    }

    @Test
    public void testAsByteSink_Path_Append() throws IOException {
        Path path = tempDir.resolve("append.txt");
        java.nio.file.Files.write(path, "Initial".getBytes());

        ByteSink sink = Files.asByteSink(path, StandardOpenOption.APPEND);
        sink.write(" Appended".getBytes());

        assertEquals("Initial Appended", new String(java.nio.file.Files.readAllBytes(path)));
    }

    @Test
    public void testAsCharSource_File_BasicRead() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        filesToCleanup.add(file);
        Files.write("Character content".getBytes(StandardCharsets.UTF_8), file);

        CharSource source = Files.asCharSource(file, StandardCharsets.UTF_8);
        String content = source.read();
        assertEquals("Character content", content);
    }

    @Test
    public void testAsCharSource_File_ReadLines() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        CharSource source = Files.asCharSource(file, StandardCharsets.UTF_8);
        ImmutableList<String> lines = ImmutableList.wrap(source.readLines());
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testAsCharSource_File_DifferentCharsets() throws IOException {
        File file = new File(tempDir.toFile(), "charset.txt");
        filesToCleanup.add(file);
        String content = "Special: áéíóú";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        CharSource source = Files.asCharSource(file, StandardCharsets.ISO_8859_1);
        assertEquals(content, source.read());
    }

    @Test
    public void testAsCharSource_Path_BasicRead() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Character content".getBytes(StandardCharsets.UTF_8));

        CharSource source = Files.asCharSource(path, StandardCharsets.UTF_8, StandardOpenOption.READ);
        String content = source.read();
        assertEquals("Character content", content);
    }

    @Test
    public void testAsCharSource_Path_NoOptions() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Character content".getBytes(StandardCharsets.UTF_8));

        CharSource source = Files.asCharSource(path, StandardCharsets.UTF_8);
        String content = source.read();
        assertEquals("Character content", content);
    }

    @Test
    public void testAsCharSink_File_BasicWrite() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");
        filesToCleanup.add(file);

        CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8);
        sink.write("Character output");

        assertEquals("Character output", Files.readString(file));
    }

    @Test
    public void testAsCharSink_File_Append() throws IOException {
        File file = new File(tempDir.toFile(), "append.txt");
        filesToCleanup.add(file);
        Files.write("Initial".getBytes(), file);

        CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8, FileWriteMode.APPEND);
        sink.write(" Appended");

        assertEquals("Initial Appended", Files.readString(file));
    }

    @Test
    public void testAsCharSink_File_WriteLines() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        filesToCleanup.add(file);

        CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8);
        List<String> lines = N.asList("Line 1", "Line 2", "Line 3");
        sink.writeLines(lines);

        String content = Files.readString(file);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testAsCharSink_Path_BasicWrite() throws IOException {
        Path path = tempDir.resolve("output.txt");

        CharSink sink = Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        sink.write("Character output");

        assertEquals("Character output", new String(java.nio.file.Files.readAllBytes(path)));
    }

    @Test
    public void testAsCharSink_Path_Append() throws IOException {
        Path path = tempDir.resolve("append.txt");
        java.nio.file.Files.write(path, "Initial".getBytes());

        CharSink sink = Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        sink.write(" Appended");

        assertEquals("Initial Appended", new String(java.nio.file.Files.readAllBytes(path)));
    }

    @Test
    public void testToByteArray_BasicRead() throws IOException {
        File file = new File(tempDir.toFile(), "bytes.txt");
        filesToCleanup.add(file);
        byte[] originalBytes = "Byte array content".getBytes(StandardCharsets.UTF_8);
        Files.write(originalBytes, file);

        byte[] readBytes = Files.toByteArray(file);
        assertArrayEquals(originalBytes, readBytes);
    }

    @Test
    public void testToByteArray_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write(new byte[0], file);

        byte[] readBytes = Files.toByteArray(file);
        assertEquals(0, readBytes.length);
    }

    @Test
    public void testToByteArray_BinaryData() throws IOException {
        File file = new File(tempDir.toFile(), "binary.bin");
        filesToCleanup.add(file);
        byte[] binaryData = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF, 0x7F };
        Files.write(binaryData, file);

        byte[] readBytes = Files.toByteArray(file);
        assertArrayEquals(binaryData, readBytes);
    }

    @Test
    public void testWrite_BasicWrite() throws IOException {
        File file = new File(tempDir.toFile(), "write.txt");
        filesToCleanup.add(file);
        byte[] bytes = "Written bytes".getBytes(StandardCharsets.UTF_8);

        Files.write(bytes, file);

        assertArrayEquals(bytes, Files.toByteArray(file));
    }

    @Test
    public void testWrite_Overwrite() throws IOException {
        File file = new File(tempDir.toFile(), "overwrite.txt");
        filesToCleanup.add(file);
        Files.write("Original".getBytes(), file);
        byte[] newBytes = "New content".getBytes();

        Files.write(newBytes, file);

        assertArrayEquals(newBytes, Files.toByteArray(file));
    }

    @Test
    public void testWrite_EmptyArray() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);

        Files.write(new byte[0], file);

        assertEquals(0, file.length());
    }

    @Test
    public void testWrite_CreatesParentDirs() throws IOException {
        File file = new File(tempDir.toFile(), "parent/file.txt");
        filesToCleanup.add(file.getParentFile().getParentFile());

        assertThrows(FileNotFoundException.class, () -> Files.write("content".getBytes(), file));
    }

    @Test
    public void testEqual_Files_Identical() throws IOException {
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        filesToCleanup.add(file1);
        filesToCleanup.add(file2);

        Files.write("Same content".getBytes(), file1);
        Files.write("Same content".getBytes(), file2);

        assertTrue(Files.equal(file1, file2));
    }

    @Test
    public void testEqual_Files_Different() throws IOException {
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        filesToCleanup.add(file1);
        filesToCleanup.add(file2);

        Files.write("Content A".getBytes(), file1);
        Files.write("Content B".getBytes(), file2);

        assertFalse(Files.equal(file1, file2));
    }

    @Test
    public void testEqual_Files_SameFile() throws IOException {
        File file = new File(tempDir.toFile(), "file.txt");
        filesToCleanup.add(file);
        Files.write("Content".getBytes(), file);

        assertTrue(Files.equal(file, file));
    }

    @Test
    public void testEqual_Files_EmptyFiles() throws IOException {
        File file1 = new File(tempDir.toFile(), "empty1.txt");
        File file2 = new File(tempDir.toFile(), "empty2.txt");
        filesToCleanup.add(file1);
        filesToCleanup.add(file2);

        Files.write("".getBytes(), file1);
        Files.write("".getBytes(), file2);

        assertTrue(Files.equal(file1, file2));
    }

    @Test
    public void testEqual_Files_DifferentSizes() throws IOException {
        File file1 = new File(tempDir.toFile(), "short.txt");
        File file2 = new File(tempDir.toFile(), "long.txt");
        filesToCleanup.add(file1);
        filesToCleanup.add(file2);

        Files.write("Short".getBytes(), file1);
        Files.write("Much longer content".getBytes(), file2);

        assertFalse(Files.equal(file1, file2));
    }

    @Test
    public void testEqual_Paths_Identical() throws IOException {
        Path path1 = tempDir.resolve("path1.txt");
        Path path2 = tempDir.resolve("path2.txt");

        java.nio.file.Files.write(path1, "Same content".getBytes());
        java.nio.file.Files.write(path2, "Same content".getBytes());

        assertTrue(Files.equal(path1, path2));
    }

    @Test
    public void testEqual_Paths_Different() throws IOException {
        Path path1 = tempDir.resolve("path1.txt");
        Path path2 = tempDir.resolve("path2.txt");

        java.nio.file.Files.write(path1, "Content A".getBytes());
        java.nio.file.Files.write(path2, "Content B".getBytes());

        assertFalse(Files.equal(path1, path2));
    }

    @Test
    public void testTouch_File_CreateNew() throws IOException {
        File file = new File(tempDir.toFile(), "touch.txt");
        filesToCleanup.add(file);
        assertFalse(file.exists());

        Files.touch(file);

        assertTrue(file.exists());
        assertEquals(0, file.length());
    }

    @Test
    public void testTouch_File_UpdateTimestamp() throws IOException, InterruptedException {
        File file = new File(tempDir.toFile(), "touch.txt");
        filesToCleanup.add(file);
        Files.touch(file);

        long firstModified = file.lastModified();
        Thread.sleep(10);
        Files.touch(file);

        assertTrue(file.lastModified() >= firstModified);
    }

    @Test
    public void testTouch_File_ExistingContent() throws IOException {
        File file = new File(tempDir.toFile(), "existing.txt");
        filesToCleanup.add(file);
        Files.write("Content".getBytes(), file);

        Files.touch(file);

        assertEquals("Content", Files.readString(file));
    }

    @Test
    public void testTouch_Path_CreateNew() throws IOException {
        Path path = tempDir.resolve("touch.txt");
        assertFalse(java.nio.file.Files.exists(path));

        Files.touch(path);

        assertTrue(java.nio.file.Files.exists(path));
    }

    @Test
    public void testTouch_Path_UpdateTimestamp() throws IOException, InterruptedException {
        Path path = tempDir.resolve("touch.txt");
        Files.touch(path);

        long firstModified = java.nio.file.Files.getLastModifiedTime(path).toMillis();
        Thread.sleep(10);
        Files.touch(path);

        assertTrue(java.nio.file.Files.getLastModifiedTime(path).toMillis() >= firstModified);
    }

    @Test
    public void testCreateTempDir_Basic() {
        File tempDir = Files.createTempDir();
        filesToCleanup.add(tempDir);

        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
    }

    @Test
    public void testCreateTempDir_Unique() {
        File tempDir1 = Files.createTempDir();
        File tempDir2 = Files.createTempDir();
        filesToCleanup.add(tempDir1);
        filesToCleanup.add(tempDir2);

        assertFalse(tempDir1.equals(tempDir2));
        assertTrue(tempDir1.exists());
        assertTrue(tempDir2.exists());
    }

    @Test
    public void testCreateTempDir_Writable() throws IOException {
        File tempDir = Files.createTempDir();
        filesToCleanup.add(tempDir);

        File testFile = new File(tempDir, "test.txt");
        Files.write("test".getBytes(), testFile);

        assertTrue(testFile.exists());
    }

    @Test
    public void testCreateParentDirs_Basic() throws IOException {
        File deepFile = new File(tempDir.toFile(), "a/b/c/d/file.txt");
        filesToCleanup.add(new File(tempDir.toFile(), "a"));
        assertFalse(deepFile.getParentFile().exists());

        Files.createParentDirs(deepFile);

        assertTrue(deepFile.getParentFile().exists());
        assertTrue(deepFile.getParentFile().isDirectory());
    }

    @Test
    public void testCreateParentDirs_AlreadyExists() throws IOException {
        File dir = new File(tempDir.toFile(), "existing");
        filesToCleanup.add(dir);
        dir.mkdirs();

        File file = new File(dir, "file.txt");

        Files.createParentDirs(file);

        assertTrue(dir.exists());
    }

    @Test
    public void testCreateParentDirs_NoParent() throws IOException {
        File file = new File(tempDir.toFile(), "file.txt");
        filesToCleanup.add(file);

        Files.createParentDirs(file);

        assertTrue(tempDir.toFile().exists());
    }

    @Test
    public void testCreateParentDirs_DeepNesting() throws IOException {
        File deepFile = new File(tempDir.toFile(), "1/2/3/4/5/6/7/8/9/10/file.txt");
        filesToCleanup.add(new File(tempDir.toFile(), "1"));

        Files.createParentDirs(deepFile);

        assertTrue(deepFile.getParentFile().exists());
    }

    @Test
    public void testCreateParentDirectories_Basic() throws IOException {
        Path deepPath = tempDir.resolve("x/y/z/file.txt");
        assertFalse(java.nio.file.Files.exists(deepPath.getParent()));

        Files.createParentDirectories(deepPath);

        assertTrue(java.nio.file.Files.exists(deepPath.getParent()));
        assertTrue(java.nio.file.Files.isDirectory(deepPath.getParent()));
    }

    @Test
    public void testCreateParentDirectories_AlreadyExists() throws IOException {
        Path dir = tempDir.resolve("existing");
        java.nio.file.Files.createDirectory(dir);

        Path file = dir.resolve("file.txt");

        Files.createParentDirectories(file);

        assertTrue(java.nio.file.Files.exists(dir));
    }

    @Test
    public void testCreateParentDirectories_NoParent() throws IOException {
        Path file = tempDir.resolve("file.txt");

        Files.createParentDirectories(file);

        assertTrue(java.nio.file.Files.exists(tempDir));
    }

    @Test
    public void testCopy_FileToOutputStream() throws IOException {
        File file = new File(tempDir.toFile(), "source.txt");
        filesToCleanup.add(file);
        Files.write("Copy this content".getBytes(), file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(file, baos);

        assertEquals("Copy this content", baos.toString());
    }

    @Test
    public void testCopy_FileToOutputStream_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(file, baos);

        assertEquals("", baos.toString());
    }

    @Test
    public void testCopy_FileToOutputStream_BinaryData() throws IOException {
        File file = new File(tempDir.toFile(), "binary.bin");
        filesToCleanup.add(file);
        byte[] binaryData = new byte[] { 0x00, 0x01, 0x02, (byte) 0xFF };
        Files.write(binaryData, file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(file, baos);

        assertArrayEquals(binaryData, baos.toByteArray());
    }

    @Test
    public void testCopy_FileToFile_Basic() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "dest.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(dest);

        Files.write("Content to copy".getBytes(), source);
        Files.copy(source, dest);

        assertTrue(Files.equal(source, dest));
        assertTrue(source.exists());
        assertTrue(dest.exists());
    }

    @Test
    public void testCopy_FileToFile_Overwrite() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "dest.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(dest);

        Files.write("Source content".getBytes(), source);
        Files.write("Original dest".getBytes(), dest);

        Files.copy(source, dest);

        assertEquals("Source content", Files.readString(dest));
    }

    @Test
    public void testCopy_FileToFile_LargeFile() throws IOException {
        File source = new File(tempDir.toFile(), "large.txt");
        File dest = new File(tempDir.toFile(), "large_copy.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(dest);

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("This is line ").append(i).append("\n");
        }
        Files.write(sb.toString().getBytes(), source);

        Files.copy(source, dest);

        assertTrue(Files.equal(source, dest));
    }

    @Test
    public void testCopy_FileToFile_SameFileThrows() throws IOException {
        File file = new File(tempDir.toFile(), "same.txt");
        filesToCleanup.add(file);
        Files.write("Content".getBytes(), file);

        assertThrows(IllegalArgumentException.class, () -> {
            Files.copy(file, file);
        });
    }

    @Test
    public void testMove_Basic() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "dest.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(dest);

        Files.write("Content to move".getBytes(), source);
        Files.move(source, dest);

        assertFalse(source.exists());
        assertTrue(dest.exists());
        assertEquals("Content to move", Files.readString(dest));
    }

    @Test
    public void testMove_ToSubdirectory() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File subdir = new File(tempDir.toFile(), "subdir");
        filesToCleanup.add(source);
        filesToCleanup.add(subdir);
        subdir.mkdirs();

        File dest = new File(subdir, "moved.txt");

        Files.write("Move to subdir".getBytes(), source);
        Files.move(source, dest);

        assertFalse(source.exists());
        assertTrue(dest.exists());
        assertEquals("Move to subdir", Files.readString(dest));
    }

    @Test
    public void testMove_Rename() throws IOException {
        File source = new File(tempDir.toFile(), "old_name.txt");
        File dest = new File(tempDir.toFile(), "new_name.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(dest);

        Files.write("Rename me".getBytes(), source);
        Files.move(source, dest);

        assertFalse(source.exists());
        assertTrue(dest.exists());
    }

    @Test
    public void testMove_SameFileThrows() throws IOException {
        File file = new File(tempDir.toFile(), "same.txt");
        filesToCleanup.add(file);
        Files.write("Content".getBytes(), file);

        assertThrows(IllegalArgumentException.class, () -> {
            Files.move(file, file);
        });
    }

    @Test
    public void testReadLines_Basic() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadLines_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);

        assertEquals(0, lines.size());
    }

    @Test
    public void testReadLines_SingleLine() throws IOException {
        File file = new File(tempDir.toFile(), "single.txt");
        filesToCleanup.add(file);
        Files.write("Only one line".getBytes(), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);

        assertEquals(1, lines.size());
        assertEquals("Only one line", lines.get(0));
    }

    @Test
    public void testReadLines_DifferentCharsets() throws IOException {
        File file = new File(tempDir.toFile(), "charset.txt");
        filesToCleanup.add(file);
        String content = "Línea 1\nLínea 2";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        List<String> lines = Files.readLines(file, StandardCharsets.ISO_8859_1);

        assertEquals(2, lines.size());
        assertEquals("Línea 1", lines.get(0));
        assertEquals("Línea 2", lines.get(1));
    }

    @Test
    public void testReadLines_WindowsLineEndings() throws IOException {
        File file = new File(tempDir.toFile(), "windows.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\r\nLine 2\r\nLine 3".getBytes(), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testMap_ReadOnly() throws IOException {
        File file = new File(tempDir.toFile(), "map.txt");
        filesToCleanup.add(file);
        Files.write("Mapped content".getBytes(), file);

        MappedByteBuffer buffer = Files.map(file);
        buffersToUnmap.add(buffer);

        byte[] bytes = new byte[(int) file.length()];
        buffer.get(bytes);

        assertEquals("Mapped content", new String(bytes));
    }

    @Test
    public void testMap_WithMode_ReadOnly() throws IOException {
        File file = new File(tempDir.toFile(), "map.txt");
        filesToCleanup.add(file);
        Files.write("Test mapping".getBytes(), file);

        MappedByteBuffer buffer = Files.map(file, MapMode.READ_ONLY);
        buffersToUnmap.add(buffer);

        byte[] bytes = new byte[(int) file.length()];
        buffer.get(bytes);

        assertEquals("Test mapping", new String(bytes));
    }

    @Test
    public void testMap_WithMode_ReadWrite() throws IOException {
        File file = new File(tempDir.toFile(), "map_rw.txt");
        filesToCleanup.add(file);
        Files.write("Initial".getBytes(), file);

        MappedByteBuffer buffer = Files.map(file, MapMode.READ_WRITE);
        buffersToUnmap.add(buffer);

        buffer.position(0);
        buffer.put("Changed".getBytes());
        buffer.force();

        unmap(buffer);
        buffersToUnmap.remove(buffer);

        String content = Files.readString(file);
        assertTrue(content.startsWith("Changed"));
    }

    @Test
    public void testMap_WithSize_CreateNew() throws IOException {
        File file = new File(tempDir.toFile(), "map_new.txt");
        filesToCleanup.add(file);

        MappedByteBuffer buffer = Files.map(file, MapMode.READ_WRITE, 1024);
        buffersToUnmap.add(buffer);

        assertEquals(1024, buffer.capacity());
        buffer.put("New file".getBytes());
        buffer.force();

        unmap(buffer);
        buffersToUnmap.remove(buffer);

        assertTrue(file.exists());
        assertTrue(file.length() >= 8);
    }

    @Test
    public void testSimplifyPath_EmptyString() {
        assertEquals(".", Files.simplifyPath(""));
    }

    @Test
    public void testSimplifyPath_CurrentDir() {
        assertEquals(".", Files.simplifyPath("."));
    }

    @Test
    public void testSimplifyPath_RemoveCurrentDir() {
        assertEquals("foo", Files.simplifyPath("./foo"));
        assertEquals("foo/bar", Files.simplifyPath("./foo/./bar"));
    }

    @Test
    public void testSimplifyPath_CollapseSlashes() {
        assertEquals("foo/bar", Files.simplifyPath("foo//bar"));
        assertEquals("/foo/bar", Files.simplifyPath("//foo///bar"));
    }

    @Test
    public void testSimplifyPath_RemoveParentDir() {
        assertEquals("foo/baz", Files.simplifyPath("foo/bar/../baz"));
        assertEquals("/foo/baz", Files.simplifyPath("/foo//bar/../baz/./"));
    }

    @Test
    public void testSimplifyPath_ParentDirAtStart() {
        assertEquals("..", Files.simplifyPath(".."));
        assertEquals("../..", Files.simplifyPath("../.."));
        assertEquals("../../foo", Files.simplifyPath("../../foo"));
    }

    @Test
    public void testSimplifyPath_TrailingSlash() {
        assertEquals("/foo/bar", Files.simplifyPath("/foo/bar/"));
        assertEquals("/", Files.simplifyPath("/"));
        assertEquals("/", Files.simplifyPath("//"));
    }

    @Test
    public void testSimplifyPath_ComplexPath() {
        assertEquals("/a/c/d", Files.simplifyPath("/a/b/../c/./d"));
        assertEquals("a/d", Files.simplifyPath("a/b/../c/../d"));
    }

    @Test
    public void testGetNameWithoutExtension_String_Basic() {
        assertEquals("document", Files.getNameWithoutExtension("document.pdf"));
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("README", Files.getNameWithoutExtension("README"));
    }

    @Test
    public void testGetNameWithoutExtension_String_WithPath() {
        assertEquals("document", Files.getNameWithoutExtension("/path/to/document.pdf"));
        assertEquals("file", Files.getNameWithoutExtension("C:\\Users\\file.txt"));
    }

    @Test
    public void testGetNameWithoutExtension_String_HiddenFile() {
        assertEquals("", Files.getNameWithoutExtension(".hidden"));
        assertEquals(".hidden", Files.getNameWithoutExtension(".hidden.txt"));
    }

    @Test
    public void testGetNameWithoutExtension_String_MultipleExtensions() {
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("file.backup", Files.getNameWithoutExtension("file.backup.bak"));
    }

    @Test
    public void testGetNameWithoutExtension_String_NoExtension() {
        assertEquals("filename", Files.getNameWithoutExtension("filename"));
        assertEquals("file_name", Files.getNameWithoutExtension("file_name"));
    }

    @Test
    public void testGetNameWithoutExtension_Path_Basic() {
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("document.pdf")));
        assertEquals("archive.tar", Files.getNameWithoutExtension(Paths.get("archive.tar.gz")));
        assertEquals("README", Files.getNameWithoutExtension(Paths.get("README")));
    }

    @Test
    public void testGetNameWithoutExtension_Path_WithPath() {
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("/path/to/document.pdf")));
        assertEquals("file", Files.getNameWithoutExtension(Paths.get("subdir/file.txt")));
    }

    @Test
    public void testGetFileExtension_String_Basic() {
        assertEquals("pdf", Files.getFileExtension("document.pdf"));
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("", Files.getFileExtension("README"));
    }

    @Test
    public void testGetFileExtension_String_WithPath() {
        assertEquals("txt", Files.getFileExtension("/path/to/file.txt"));
        assertEquals("java", Files.getFileExtension("C:\\Source\\File.java"));
    }

    @Test
    public void testGetFileExtension_String_HiddenFile() {
        assertEquals("hidden", Files.getFileExtension(".hidden"));
        assertEquals("txt", Files.getFileExtension(".hidden.txt"));
    }

    @Test
    public void testGetFileExtension_String_MultipleExtensions() {
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("bak", Files.getFileExtension("file.backup.bak"));
    }

    @Test
    public void testGetFileExtension_String_NoExtension() {
        assertEquals("", Files.getFileExtension("filename"));
        assertEquals("", Files.getFileExtension("file_name"));
    }

    @Test
    public void testGetFileExtension_Path_Basic() {
        assertEquals("pdf", Files.getFileExtension(Paths.get("document.pdf")));
        assertEquals("gz", Files.getFileExtension(Paths.get("archive.tar.gz")));
        assertEquals("", Files.getFileExtension(Paths.get("README")));
    }

    @Test
    public void testGetFileExtension_Path_WithPath() {
        assertEquals("txt", Files.getFileExtension(Paths.get("/path/to/file.txt")));
        assertEquals("java", Files.getFileExtension(Paths.get("src/File.java")));
    }

    @Test
    public void testFileTraverser_BreadthFirst() throws IOException {
        File root = new File(tempDir.toFile(), "root");
        filesToCleanup.add(root);
        root.mkdir();
        new File(root, "file1.txt").createNewFile();
        File subdir = new File(root, "subdir");
        subdir.mkdir();
        new File(subdir, "file2.txt").createNewFile();

        Traverser<File> traverser = Files.fileTraverser();
        List<String> names = new ArrayList<>();
        for (File file : traverser.breadthFirst(root)) {
            names.add(file.getName());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("subdir"));
        assertTrue(names.contains("file2.txt"));
    }

    @Test
    public void testFileTraverser_DepthFirst() throws IOException {
        File root = new File(tempDir.toFile(), "root");
        filesToCleanup.add(root);
        root.mkdir();
        new File(root, "file1.txt").createNewFile();
        File subdir = new File(root, "subdir");
        subdir.mkdir();
        new File(subdir, "file2.txt").createNewFile();

        Traverser<File> traverser = Files.fileTraverser();
        List<String> names = new ArrayList<>();
        for (File file : traverser.depthFirstPreOrder(root)) {
            names.add(file.getName());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("subdir"));
        assertTrue(names.contains("file2.txt"));
    }

    @Test
    public void testFileTraverser_EmptyDirectory() throws IOException {
        File root = new File(tempDir.toFile(), "empty");
        filesToCleanup.add(root);
        root.mkdir();

        Traverser<File> traverser = Files.fileTraverser();
        List<File> files = new ArrayList<>();
        for (File file : traverser.breadthFirst(root)) {
            files.add(file);
        }

        assertEquals(1, files.size());
        assertEquals("empty", files.get(0).getName());
    }

    @Test
    public void testFileTraverser_NonExistentFile() {
        File nonExistent = new File(tempDir.toFile(), "nonexistent");

        Traverser<File> traverser = Files.fileTraverser();
        List<File> files = new ArrayList<>();
        for (File file : traverser.breadthFirst(nonExistent)) {
            files.add(file);
        }

        assertEquals(1, files.size());
        assertEquals(nonExistent, files.get(0));
    }

    @Test
    public void testPathTraverser_BreadthFirst() throws IOException {
        Path root = tempDir.resolve("root");
        java.nio.file.Files.createDirectory(root);
        java.nio.file.Files.createFile(root.resolve("file.txt"));
        Path subdir = root.resolve("subdir");
        java.nio.file.Files.createDirectory(subdir);
        java.nio.file.Files.createFile(subdir.resolve("nested.txt"));

        Traverser<Path> traverser = Files.pathTraverser();
        Set<String> names = new HashSet<>();
        for (Path path : traverser.breadthFirst(root)) {
            names.add(path.getFileName().toString());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file.txt"));
        assertTrue(names.contains("subdir"));
        assertTrue(names.contains("nested.txt"));
    }

    @Test
    public void testPathTraverser_DepthFirst() throws IOException {
        Path root = tempDir.resolve("root");
        java.nio.file.Files.createDirectory(root);
        java.nio.file.Files.createFile(root.resolve("file.txt"));
        java.nio.file.Files.createDirectory(root.resolve("subdir"));

        Traverser<Path> traverser = Files.pathTraverser();
        Set<String> names = new HashSet<>();
        for (Path path : traverser.depthFirstPreOrder(root)) {
            names.add(path.getFileName().toString());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file.txt"));
        assertTrue(names.contains("subdir"));
    }

    @Test
    public void testPathTraverser_EmptyDirectory() throws IOException {
        Path root = tempDir.resolve("empty");
        java.nio.file.Files.createDirectory(root);

        Traverser<Path> traverser = Files.pathTraverser();
        List<Path> paths = new ArrayList<>();
        for (Path path : traverser.breadthFirst(root)) {
            paths.add(path);
        }

        assertEquals(1, paths.size());
        assertEquals("empty", paths.get(0).getFileName().toString());
    }

    @Test
    public void testListFiles_Basic() throws IOException {
        Path dir = tempDir.resolve("listdir");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(dir.resolve("file1.txt"));
        java.nio.file.Files.createFile(dir.resolve("file2.txt"));
        java.nio.file.Files.createDirectory(dir.resolve("subdir"));

        ImmutableList<Path> files = Files.listFiles(dir);

        assertEquals(3, files.size());
        Set<String> names = new HashSet<>();
        for (Path file : files) {
            names.add(file.getFileName().toString());
        }
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("file2.txt"));
        assertTrue(names.contains("subdir"));
    }

    @Test
    public void testListFiles_EmptyDirectory() throws IOException {
        Path dir = tempDir.resolve("empty");
        java.nio.file.Files.createDirectory(dir);

        ImmutableList<Path> files = Files.listFiles(dir);

        assertEquals(0, files.size());
    }

    @Test
    public void testListFiles_NonExistentDirectory() {
        Path dir = tempDir.resolve("nonexistent");

        assertThrows(NoSuchFileException.class, () -> {
            Files.listFiles(dir);
        });
    }

    @Test
    public void testListFiles_NotDirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        java.nio.file.Files.createFile(file);

        assertThrows(NotDirectoryException.class, () -> {
            Files.listFiles(file);
        });
    }

    @Test
    public void testDeleteRecursively_EmptyDirectory() throws IOException {
        Path dir = tempDir.resolve("empty");
        java.nio.file.Files.createDirectory(dir);

        Files.deleteRecursively(dir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertFalse(java.nio.file.Files.exists(dir));
    }

    @Test
    public void testDeleteRecursively_WithFiles() throws IOException {
        Path root = tempDir.resolve("delete");
        java.nio.file.Files.createDirectory(root);
        java.nio.file.Files.createFile(root.resolve("file1.txt"));
        java.nio.file.Files.createFile(root.resolve("file2.txt"));

        Files.deleteRecursively(root, RecursiveDeleteOption.ALLOW_INSECURE);

        assertFalse(java.nio.file.Files.exists(root));
    }

    @Test
    public void testDeleteRecursively_WithSubdirectories() throws IOException {
        Path root = tempDir.resolve("delete");
        java.nio.file.Files.createDirectory(root);
        Path subdir = root.resolve("subdir");
        java.nio.file.Files.createDirectory(subdir);
        java.nio.file.Files.createFile(subdir.resolve("file.txt"));
        java.nio.file.Files.createFile(root.resolve("root_file.txt"));

        Files.deleteRecursively(root, RecursiveDeleteOption.ALLOW_INSECURE);

        assertFalse(java.nio.file.Files.exists(root));
    }

    @Test
    public void testDeleteRecursively_DeepNesting() throws IOException {
        Path root = tempDir.resolve("deep");
        Path current = root;
        for (int i = 0; i < 5; i++) {
            current = current.resolve("level" + i);
            java.nio.file.Files.createDirectories(current);
            java.nio.file.Files.createFile(current.resolve("file" + i + ".txt"));
        }

        Files.deleteRecursively(root, RecursiveDeleteOption.ALLOW_INSECURE);

        assertFalse(java.nio.file.Files.exists(root));
    }

    @Test
    public void testDeleteRecursively_NonExistentPath() {
        Path nonExistent = tempDir.resolve("nonexistent");

        assertThrows(NoSuchFileException.class, () -> {
            Files.deleteRecursively(nonExistent, RecursiveDeleteOption.ALLOW_INSECURE);
        });
    }

    @Test
    public void testDeleteDirectoryContents_EmptyDirectory() throws IOException {
        Path dir = tempDir.resolve("empty");
        java.nio.file.Files.createDirectory(dir);

        Files.deleteDirectoryContents(dir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertTrue(java.nio.file.Files.exists(dir));
        assertTrue(java.nio.file.Files.isDirectory(dir));
        assertEquals(0, java.nio.file.Files.list(dir).count());
    }

    @Test
    public void testDeleteDirectoryContents_WithFiles() throws IOException {
        Path dir = tempDir.resolve("contents");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(dir.resolve("file1.txt"));
        java.nio.file.Files.createFile(dir.resolve("file2.txt"));

        Files.deleteDirectoryContents(dir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertTrue(java.nio.file.Files.exists(dir));
        assertTrue(java.nio.file.Files.isDirectory(dir));
        assertEquals(0, java.nio.file.Files.list(dir).count());
    }

    @Test
    public void testDeleteDirectoryContents_WithSubdirectories() throws IOException {
        Path dir = tempDir.resolve("contents");
        java.nio.file.Files.createDirectory(dir);
        Path subdir = dir.resolve("subdir");
        java.nio.file.Files.createDirectory(subdir);
        java.nio.file.Files.createFile(subdir.resolve("nested.txt"));
        java.nio.file.Files.createFile(dir.resolve("file.txt"));

        Files.deleteDirectoryContents(dir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertTrue(java.nio.file.Files.exists(dir));
        assertTrue(java.nio.file.Files.isDirectory(dir));
        assertEquals(0, java.nio.file.Files.list(dir).count());
    }

    @Test
    public void testDeleteDirectoryContents_NonExistentDirectory() {
        Path dir = tempDir.resolve("nonexistent");

        assertThrows(NoSuchFileException.class, () -> {
            Files.deleteDirectoryContents(dir, RecursiveDeleteOption.ALLOW_INSECURE);
        });
    }

    @Test
    public void testDeleteDirectoryContents_NotDirectory() throws IOException {
        Path file = tempDir.resolve("file.txt");
        java.nio.file.Files.createFile(file);

        assertThrows(NotDirectoryException.class, () -> {
            Files.deleteDirectoryContents(file, RecursiveDeleteOption.ALLOW_INSECURE);
        });
    }

    @Test
    public void testIsDirectory_Directory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        java.nio.file.Files.createDirectory(dir);

        Predicate<Path> isDirPredicate = Files.isDirectory();
        assertTrue(isDirPredicate.apply(dir));
    }

    @Test
    public void testIsDirectory_File() throws IOException {
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createFile(file);

        Predicate<Path> isDirPredicate = Files.isDirectory();
        assertFalse(isDirPredicate.apply(file));
    }

    @Test
    public void testIsDirectory_NonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent");

        Predicate<Path> isDirPredicate = Files.isDirectory();
        assertFalse(isDirPredicate.apply(nonExistent));
    }

    @Test
    public void testIsDirectory_WithLinkOption() throws IOException {
        Path dir = tempDir.resolve("testdir");
        java.nio.file.Files.createDirectory(dir);

        Predicate<Path> isDirPredicate = Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isDirPredicate.apply(dir));
    }

    @Test
    public void testIsRegularFile_File() throws IOException {
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createFile(file);

        Predicate<Path> isFilePredicate = Files.isRegularFile();
        assertTrue(isFilePredicate.apply(file));
    }

    @Test
    public void testIsRegularFile_Directory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        java.nio.file.Files.createDirectory(dir);

        Predicate<Path> isFilePredicate = Files.isRegularFile();
        assertFalse(isFilePredicate.apply(dir));
    }

    @Test
    public void testIsRegularFile_NonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent");

        Predicate<Path> isFilePredicate = Files.isRegularFile();
        assertFalse(isFilePredicate.apply(nonExistent));
    }

    @Test
    public void testIsRegularFile_WithLinkOption() throws IOException {
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createFile(file);

        Predicate<Path> isFilePredicate = Files.isRegularFile(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isFilePredicate.apply(file));
    }

    @Test
    public void testReadAllBytes_Basic() throws IOException {
        File file = new File(tempDir.toFile(), "bytes.txt");
        filesToCleanup.add(file);
        byte[] content = "Read all bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(content, file);

        byte[] read = Files.readAllBytes(file);

        assertArrayEquals(content, read);
    }

    @Test
    public void testReadAllBytes_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write(new byte[0], file);

        byte[] read = Files.readAllBytes(file);

        assertEquals(0, read.length);
    }

    @Test
    public void testReadAllBytes_BinaryData() throws IOException {
        File file = new File(tempDir.toFile(), "binary.bin");
        filesToCleanup.add(file);
        byte[] binaryData = new byte[] { 0x00, 0x01, 0x7F, (byte) 0x80, (byte) 0xFF };
        Files.write(binaryData, file);

        byte[] read = Files.readAllBytes(file);

        assertArrayEquals(binaryData, read);
    }

    @Test
    public void testReadAllBytes_LargeFile() throws IOException {
        File file = new File(tempDir.toFile(), "large.bin");
        filesToCleanup.add(file);
        byte[] largeData = new byte[100000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        Files.write(largeData, file);

        byte[] read = Files.readAllBytes(file);

        assertArrayEquals(largeData, read);
    }

    @Test
    public void testReadString_Default_UTF8() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        filesToCleanup.add(file);
        Files.write("UTF-8 content".getBytes(StandardCharsets.UTF_8), file);

        String content = Files.readString(file);

        assertEquals("UTF-8 content", content);
    }

    @Test
    public void testReadString_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        filesToCleanup.add(file);
        String content = "ISO-8859-1 content: áéíóú";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        String read = Files.readString(file, StandardCharsets.ISO_8859_1);

        assertEquals(content, read);
    }

    @Test
    public void testReadString_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        String content = Files.readString(file);

        assertEquals("", content);
    }

    @Test
    public void testReadString_MultiLine() throws IOException {
        File file = new File(tempDir.toFile(), "multiline.txt");
        filesToCleanup.add(file);
        String content = "Line 1\nLine 2\nLine 3";
        Files.write(content.getBytes(), file);

        String read = Files.readString(file);

        assertEquals(content, read);
    }

    @Test
    public void testReadString_SpecialCharacters() throws IOException {
        File file = new File(tempDir.toFile(), "special.txt");
        filesToCleanup.add(file);
        String content = "Special chars: !@#$%^&*()_+-=[]{}|;':\",./<>?";
        Files.write(content.getBytes(), file);

        String read = Files.readString(file);

        assertEquals(content, read);
    }

    @Test
    public void testReadAllLines_Default_UTF8() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        List<String> lines = Files.readAllLines(file);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadAllLines_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        filesToCleanup.add(file);
        Files.write("Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);

        assertEquals(2, lines.size());
        assertEquals("Línea 1", lines.get(0));
        assertEquals("Línea 2", lines.get(1));
    }

    @Test
    public void testReadAllLines_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        List<String> lines = Files.readAllLines(file);

        assertEquals(0, lines.size());
    }

    @Test
    public void testReadAllLines_SingleLine() throws IOException {
        File file = new File(tempDir.toFile(), "single.txt");
        filesToCleanup.add(file);
        Files.write("Only one line".getBytes(), file);

        List<String> lines = Files.readAllLines(file);

        assertEquals(1, lines.size());
        assertEquals("Only one line", lines.get(0));
    }

    @Test
    public void testReadAllLines_WindowsLineEndings() throws IOException {
        File file = new File(tempDir.toFile(), "windows.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\r\nLine 2\r\nLine 3".getBytes(), file);

        List<String> lines = Files.readAllLines(file);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadAllLines_EmptyLines() throws IOException {
        File file = new File(tempDir.toFile(), "empty_lines.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\n\nLine 3".getBytes(), file);

        List<String> lines = Files.readAllLines(file);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testMoreFiles_ExtendsFiles() {
        assertTrue(Files.MoreFiles.class.getSuperclass() == Files.class);
    }

    @Test
    public void testMoreFiles_ClassExists() {
        assertNotNull(Files.MoreFiles.class);
    }

    @Test
    public void testNewReader_Directory_ThrowsException() throws IOException {
        File dir = new File(tempDir.toFile(), "directory");
        filesToCleanup.add(dir);
        dir.mkdir();

        assertThrows(FileNotFoundException.class, () -> {
            Files.newReader(dir, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testCopy_FileToFile_CreatesParentDirectories() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "deep/nested/dest.txt");
        filesToCleanup.add(source);
        filesToCleanup.add(new File(tempDir.toFile(), "deep"));

        Files.write("Content".getBytes(), source);
        assertThrows(FileNotFoundException.class, () -> Files.copy(source, dest));
    }

    @Test
    public void testReadLines_MutableList() throws IOException {
        File file = new File(tempDir.toFile(), "mutable.txt");
        filesToCleanup.add(file);
        Files.write("Line 1\nLine 2".getBytes(), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        lines.add("Line 3");

        assertEquals(3, lines.size());
    }

    @Test
    public void testAsCharSource_EmptyFile() throws IOException {
        File file = new File(tempDir.toFile(), "empty.txt");
        filesToCleanup.add(file);
        Files.write("".getBytes(), file);

        CharSource source = Files.asCharSource(file, StandardCharsets.UTF_8);
        String content = source.read();

        assertEquals("", content);
    }

    @Test
    public void testSimplifyPath_RootOnly() {
        assertEquals("/", Files.simplifyPath("/"));
        assertEquals("/", Files.simplifyPath("///"));
    }

    @Test
    public void testFileTraverser_SingleFile() throws IOException {
        File file = new File(tempDir.toFile(), "single.txt");
        filesToCleanup.add(file);
        file.createNewFile();

        Traverser<File> traverser = Files.fileTraverser();
        List<File> files = new ArrayList<>();
        for (File f : traverser.breadthFirst(file)) {
            files.add(f);
        }

        assertEquals(1, files.size());
        assertEquals(file, files.get(0));
    }
}
