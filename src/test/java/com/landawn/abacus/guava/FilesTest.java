package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
import com.landawn.abacus.util.ImmutableList;

@SuppressWarnings("resource")
public class FilesTest extends TestBase {

    @TempDir
    Path tempDir;

    private File testFile;
    private Path testPath;

    @BeforeEach
    public void setUp() throws IOException {
        testFile = tempDir.resolve("test.txt").toFile();
        testPath = testFile.toPath();

        // Create test file with sample content
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(testFile))) {
            writer.write("Line 1\n");
            writer.write("Line 2\n");
            writer.write("Line 3");
        }
    }

    @AfterEach
    public void tearDown() {
        // Cleanup is handled by JUnit's @TempDir
    }

    // Tests for memory-mapped files

    @Test
    @DisplayName("Test map File read-only")
    public void testMapFileReadOnly() throws IOException {
        MappedByteBuffer buffer = Files.map(testFile);
        assertNotNull(buffer);
        assertTrue(buffer.capacity() > 0);

        // Read first byte
        byte firstByte = buffer.get(0);
        assertEquals('L', (char) firstByte);

        unmap(buffer);
    }

    // Tests for newReader methods

    @Test
    @DisplayName("Test newReader with File and Charset")
    public void testNewReaderFileCharset() throws IOException {
        try (BufferedReader reader = Files.newReader(testFile, StandardCharsets.UTF_8)) {
            assertNotNull(reader);
            assertEquals("Line 1", reader.readLine());
            assertEquals("Line 2", reader.readLine());
            assertEquals("Line 3", reader.readLine());
            assertNull(reader.readLine());
        }
    }

    @Test
    @DisplayName("Test newReader with non-existent file")
    public void testNewReaderFileNotFound() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> Files.newReader(nonExistentFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewReader() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        Files.write("Hello World".getBytes(StandardCharsets.UTF_8), file);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            assertEquals("Hello World", reader.readLine());
        }
    }

    @Test
    public void testNewReader_FileNotFound() {
        File file = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> {
            Files.newReader(file, StandardCharsets.UTF_8);
        });
    }

    // Edge cases and error conditions

    @Test
    @DisplayName("Test operations with null parameters")
    public void testNullParameters() {
        assertThrows(NullPointerException.class, () -> Files.newReader(null, StandardCharsets.UTF_8));

        assertThrows(NullPointerException.class, () -> Files.toByteArray(null));
    }

    // Tests for newWriter methods

    @Test
    @DisplayName("Test newWriter with File and Charset")
    public void testNewWriterFileCharset() throws IOException {
        File outputFile = tempDir.resolve("output.txt").toFile();

        try (BufferedWriter writer = Files.newWriter(outputFile, StandardCharsets.UTF_8)) {
            assertNotNull(writer);
            writer.write("Test content");
            writer.newLine();
            writer.write("Second line");
        }

        // Verify content was written
        List<String> lines = java.nio.file.Files.readAllLines(outputFile.toPath());
        assertEquals(2, lines.size());
        assertEquals("Test content", lines.get(0));
        assertEquals("Second line", lines.get(1));
    }

    @Test
    public void testNewWriter() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Test output");
        }

        assertEquals("Test output", Files.readString(file));
    }

    // Tests for ByteSource methods

    @Test
    @DisplayName("Test asByteSource with File")
    public void testAsByteSourceFile() throws IOException {
        ByteSource source = Files.asByteSource(testFile);
        assertNotNull(source);

        byte[] bytes = source.read();
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("Line 1"));
    }

    @Test
    public void testAsByteSource_File() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        Files.write("Test content".getBytes(), file);

        ByteSource source = Files.asByteSource(file);
        byte[] bytes = source.read();
        assertEquals("Test content", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("Test asByteSource with Path and options")
    public void testAsByteSourcePathOptions() throws IOException {
        ByteSource source = Files.asByteSource(testPath, StandardOpenOption.READ);
        assertNotNull(source);

        assertTrue(source.size() > 0);
    }

    // Additional tests for missing coverage

    @Test
    public void testAsByteSourcePath_NoOptions() throws IOException {
        ByteSource source = Files.asByteSource(testPath);
        assertNotNull(source);
        byte[] bytes = source.read();
        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("Line 1"));
    }

    // Tests for ByteSink methods

    @Test
    @DisplayName("Test asByteSink with File")
    public void testAsByteSinkFile() throws IOException {
        File outputFile = tempDir.resolve("sink.txt").toFile();
        ByteSink sink = Files.asByteSink(outputFile);
        assertNotNull(sink);

        byte[] data = "Test data".getBytes(StandardCharsets.UTF_8);
        sink.write(data);

        // Verify content
        byte[] readData = java.nio.file.Files.readAllBytes(outputFile.toPath());
        assertArrayEquals(data, readData);
    }

    @Test
    @DisplayName("Test asByteSink with File and APPEND mode")
    public void testAsByteSinkFileAppend() throws IOException {
        ByteSink sink = Files.asByteSink(testFile, FileWriteMode.APPEND);
        assertNotNull(sink);

        sink.write("\nAppended".getBytes(StandardCharsets.UTF_8));

        List<String> lines = java.nio.file.Files.readAllLines(testPath);
        assertEquals(4, lines.size());
        assertEquals("Appended", lines.get(3));
    }

    @Test
    public void testAsByteSink_File() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");

        ByteSink sink = Files.asByteSink(file);
        sink.write("Written content".getBytes(StandardCharsets.UTF_8));

        assertEquals("Written content", Files.readString(file));
    }

    @Test
    public void testAsByteSink_FileAppend() throws IOException {
        File file = new File(tempDir.toFile(), "append.txt");
        Files.write("Initial".getBytes(), file);

        ByteSink sink = Files.asByteSink(file, FileWriteMode.APPEND);
        sink.write(" Appended".getBytes(StandardCharsets.UTF_8));

        assertEquals("Initial Appended", Files.readString(file));
    }

    @Test
    @DisplayName("Test asByteSink with Path and options")
    public void testAsByteSinkPathOptions() throws IOException {
        Path outputPath = tempDir.resolve("sink2.txt");
        ByteSink sink = Files.asByteSink(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertNotNull(sink);

        sink.write("Path sink test".getBytes());
        assertTrue(java.nio.file.Files.exists(outputPath));
    }

    @Test
    public void testAsByteSinkPathWithCreateAndAppend() throws IOException {
        Path outPath = tempDir.resolve("sink_path_append.txt");
        ByteSink createSink = Files.asByteSink(outPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        createSink.write("First".getBytes());

        ByteSink appendSink = Files.asByteSink(outPath, StandardOpenOption.APPEND);
        appendSink.write(" Second".getBytes());

        String content = new String(java.nio.file.Files.readAllBytes(outPath));
        assertEquals("First Second", content);
    }

    // Tests for CharSource methods

    @Test
    @DisplayName("Test asCharSource with File")
    public void testAsCharSourceFile() throws IOException {
        CharSource source = Files.asCharSource(testFile, StandardCharsets.UTF_8);
        assertNotNull(source);

        String content = source.read();
        assertTrue(content.contains("Line 1"));

        List<String> lines = source.readLines();
        assertEquals(3, lines.size());
    }

    @Test
    public void testAsCharSource_File() throws IOException {
        File file = new File(tempDir.toFile(), "test.txt");
        Files.write("Character content".getBytes(StandardCharsets.UTF_8), file);

        CharSource source = Files.asCharSource(file, StandardCharsets.UTF_8);
        String content = source.read();
        assertEquals("Character content", content);
    }

    @Test
    @DisplayName("Test asCharSource with Path and options")
    public void testAsCharSourcePathOptions() throws IOException {
        CharSource source = Files.asCharSource(testPath, StandardCharsets.UTF_8, StandardOpenOption.READ);
        assertNotNull(source);

        try (BufferedReader reader = source.openBufferedStream()) {
            assertNotNull(reader.readLine());
        }
    }

    // Tests for CharSink methods

    @Test
    @DisplayName("Test asCharSink with File")
    public void testAsCharSinkFile() throws IOException {
        File outputFile = tempDir.resolve("charsink.txt").toFile();
        CharSink sink = Files.asCharSink(outputFile, StandardCharsets.UTF_8);
        assertNotNull(sink);

        sink.write("Character sink test");

        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()));
        assertEquals("Character sink test", content);
    }

    @Test
    public void testAsCharSink_File() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");

        CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8);
        sink.write("Character output");

        assertEquals("Character output", Files.readString(file));
    }

    @Test
    @DisplayName("Test asCharSink with File and APPEND mode")
    public void testAsCharSinkFileAppend() throws IOException {
        File outputFile = tempDir.resolve("charsinkappend.txt").toFile();
        // Write initial content
        CharSink initialSink = Files.asCharSink(outputFile, StandardCharsets.UTF_8);
        initialSink.write("Initial");

        // Append more content
        CharSink appendSink = Files.asCharSink(outputFile, StandardCharsets.UTF_8, FileWriteMode.APPEND);
        appendSink.write(" Appended");

        String content = new String(java.nio.file.Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("Initial Appended", content);
    }

    @Test
    @DisplayName("Test asCharSink with Path and options")
    public void testAsCharSinkPathOptions() throws IOException {
        Path outputPath = tempDir.resolve("charsink2.txt");
        CharSink sink = Files.asCharSink(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertNotNull(sink);

        sink.writeLines(Arrays.asList("Line A", "Line B"));

        List<String> lines = java.nio.file.Files.readAllLines(outputPath);
        assertEquals(2, lines.size());
    }

    @Test
    public void testAsCharSinkFile_NoModes() throws IOException {
        File outputFile = tempDir.resolve("charsink_nomodes.txt").toFile();
        CharSink sink = Files.asCharSink(outputFile, StandardCharsets.UTF_8);
        sink.write("No mode test");
        assertEquals("No mode test", Files.readString(outputFile));
    }

    // Tests for byte array operations

    @Test
    @DisplayName("Test toByteArray")
    public void testToByteArray() throws IOException {
        byte[] bytes = Files.toByteArray(testFile);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("Line 1"));
    }

    @Test
    @DisplayName("Test operations with empty files")
    public void testEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty.txt").toFile();
        emptyFile.createNewFile();

        byte[] bytes = Files.toByteArray(emptyFile);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);

        List<String> lines = Files.readLines(emptyFile, StandardCharsets.UTF_8);
        assertNotNull(lines);
        assertEquals(0, lines.size());
    }

    @Test
    public void testWrite() throws IOException {
        File file = new File(tempDir.toFile(), "write.txt");
        byte[] bytes = "Written bytes".getBytes(StandardCharsets.UTF_8);

        Files.write(bytes, file);

        assertArrayEquals(bytes, Files.toByteArray(file));
    }

    @Test
    @DisplayName("Test write byte array to file")
    public void testWriteByteArray() throws IOException {
        File outputFile = tempDir.resolve("bytewrite.txt").toFile();
        byte[] data = "Byte array data".getBytes(StandardCharsets.UTF_8);

        Files.createParentDirs(outputFile);
        Files.write(data, outputFile);

        byte[] readData = java.nio.file.Files.readAllBytes(outputFile.toPath());
        assertArrayEquals(data, readData);
    }

    @Test
    @DisplayName("Test file operations with special characters in name")
    public void testSpecialCharacterFilenames() throws IOException {
        File specialFile = tempDir.resolve("file with spaces.txt").toFile();
        byte[] data = "Special file content".getBytes();

        Files.write(data, specialFile);
        assertTrue(specialFile.exists());

        byte[] readData = Files.toByteArray(specialFile);
        assertArrayEquals(data, readData);
    }

    @Test
    @DisplayName("Test large file handling")
    public void testLargeFileHandling() throws IOException {
        File largeFile = tempDir.resolve("large.txt").toFile();

        // Create a file with multiple lines
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(largeFile))) {
            for (int i = 0; i < 1000; i++) {
                writer.write("Line " + i + "\n");
            }
        }

        List<String> lines = Files.readLines(largeFile, StandardCharsets.UTF_8);
        assertEquals(1000, lines.size());
        assertEquals("Line 0", lines.get(0));
        assertEquals("Line 999", lines.get(999));
    }

    @Test
    public void testWriteAndReadLargeByteArray() throws IOException {
        File largeFile = tempDir.resolve("large_bytes.bin").toFile();
        byte[] data = new byte[10000];
        Arrays.fill(data, (byte) 42);

        Files.write(data, largeFile);
        byte[] readBack = Files.toByteArray(largeFile);
        assertArrayEquals(data, readBack);
    }

    // Tests for file comparison

    @Test
    @DisplayName("Test equal with identical files")
    public void testEqualFilesIdentical() throws IOException {
        File file1 = tempDir.resolve("file1.txt").toFile();
        File file2 = tempDir.resolve("file2.txt").toFile();

        byte[] data = "Same content".getBytes();
        java.nio.file.Files.write(file1.toPath(), data);
        java.nio.file.Files.write(file2.toPath(), data);

        assertTrue(Files.equal(file1, file2));
    }

    @Test
    @DisplayName("Test equal with different files")
    public void testEqualFilesDifferent() throws IOException {
        File file1 = tempDir.resolve("file1.txt").toFile();
        File file2 = tempDir.resolve("file2.txt").toFile();

        java.nio.file.Files.write(file1.toPath(), "Content 1".getBytes());
        java.nio.file.Files.write(file2.toPath(), "Content 2".getBytes());

        assertFalse(Files.equal(file1, file2));
    }

    @Test
    @DisplayName("Test equal with Paths")
    public void testEqualPaths() throws IOException {
        Path path1 = tempDir.resolve("path1.txt");
        Path path2 = tempDir.resolve("path2.txt");

        byte[] data = "Path content".getBytes();
        java.nio.file.Files.write(path1, data);
        java.nio.file.Files.write(path2, data);

        assertTrue(Files.equal(path1, path2));
    }

    @Test
    public void testEqual_Files() throws IOException {
        File file1 = new File(tempDir.toFile(), "file1.txt");
        File file2 = new File(tempDir.toFile(), "file2.txt");
        File file3 = new File(tempDir.toFile(), "file3.txt");

        Files.write("Same content".getBytes(), file1);
        Files.write("Same content".getBytes(), file2);
        Files.write("Different content".getBytes(), file3);

        assertTrue(Files.equal(file1, file2));
        assertFalse(Files.equal(file1, file3));
        assertTrue(Files.equal(file1, file1));
    }

    @Test
    @DisplayName("Test equal with Paths having different content")
    public void testEqualPaths_DifferentContent() throws IOException {
        Path path1 = tempDir.resolve("pathA.txt");
        Path path2 = tempDir.resolve("pathB.txt");

        java.nio.file.Files.write(path1, "Content A".getBytes());
        java.nio.file.Files.write(path2, "Content B".getBytes());

        assertFalse(Files.equal(path1, path2));
    }

    @Test
    public void testEqualSameFile() throws IOException {
        assertTrue(Files.equal(testFile, testFile));
    }

    @Test
    public void testEqualPathsSame() throws IOException {
        assertTrue(Files.equal(testPath, testPath));
    }

    // Tests for touch operations

    @Test
    @DisplayName("Test touch with File")
    public void testTouchFile() throws IOException {
        File newFile = tempDir.resolve("touched.txt").toFile();
        assertFalse(newFile.exists());

        Files.touch(newFile);
        assertTrue(newFile.exists());
        assertEquals(0, newFile.length());

        // Touch existing file updates timestamp
        long initialTime = newFile.lastModified();
        try {
            Thread.sleep(10); // Small delay to ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Files.touch(newFile);
        assertTrue(newFile.lastModified() >= initialTime);
    }

    @Test
    public void testTouch_File() throws IOException, InterruptedException {
        File file = new File(tempDir.toFile(), "touch.txt");
        assertFalse(file.exists());

        Files.touch(file);
        assertTrue(file.exists());
        assertEquals(0, file.length());

        long firstModified = file.lastModified();
        Thread.sleep(10);
        Files.touch(file);
        assertTrue(file.lastModified() >= firstModified);
    }

    @Test
    @DisplayName("Test touch with Path")
    public void testTouchPath() throws IOException {
        Path newPath = tempDir.resolve("touched2.txt");
        assertFalse(java.nio.file.Files.exists(newPath));

        Files.touch(newPath);
        assertTrue(java.nio.file.Files.exists(newPath));
    }

    @Test
    public void testTouchExistingFile() throws IOException {
        Path existingFile = tempDir.resolve("existing_touch.txt");
        java.nio.file.Files.write(existingFile, "content".getBytes());
        long sizeBefore = java.nio.file.Files.size(existingFile);

        Files.touch(existingFile);
        long sizeAfter = java.nio.file.Files.size(existingFile);
        assertEquals(sizeBefore, sizeAfter);
    }

    // Tests for temporary directory creation

    @Test
    @DisplayName("Test createTempDir")
    @SuppressWarnings("deprecation")
    public void testCreateTempDir() {
        File tempDir = Files.createTempDir();
        assertNotNull(tempDir);
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());

        // Clean up
        tempDir.delete();
    }

    // Tests for parent directory creation

    @Test
    @DisplayName("Test createParentDirs")
    public void testCreateParentDirs() throws IOException {
        File deepFile = new File(tempDir.toFile(), "a/b/c/d/file.txt");
        assertFalse(deepFile.getParentFile().exists());

        Files.createParentDirs(deepFile);
        assertTrue(deepFile.getParentFile().exists());
    }

    @Test
    public void testCreateParentDirsWithExistingParent() throws IOException {
        File fileInExistingDir = new File(tempDir.toFile(), "existing_parent.txt");
        Files.createParentDirs(fileInExistingDir);
        assertTrue(fileInExistingDir.getParentFile().exists());
    }

    @Test
    @DisplayName("Test createParentDirectories with Path")
    public void testCreateParentDirectoriesPath() throws IOException {
        Path deepPath = tempDir.resolve("x/y/z/file.txt");
        assertFalse(java.nio.file.Files.exists(deepPath.getParent()));

        Files.createParentDirectories(deepPath);
        assertTrue(java.nio.file.Files.exists(deepPath.getParent()));
    }

    // Tests for copy operations

    @Test
    @DisplayName("Test copy File to OutputStream")
    public void testCopyFileToOutputStream() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(testFile, out);

        String content = out.toString(StandardCharsets.UTF_8.name());
        assertTrue(content.contains("Line 1"));
    }

    @Test
    @DisplayName("Test copy File to File")
    public void testCopyFileToFile() throws IOException {
        File destFile = tempDir.resolve("copy.txt").toFile();
        Files.copy(testFile, destFile);

        assertTrue(destFile.exists());
        assertTrue(Files.equal(testFile, destFile));
    }

    @Test
    public void testCopy_FileToOutputStream() throws IOException {
        File file = new File(tempDir.toFile(), "source.txt");
        Files.write("Copy this content".getBytes(), file);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(file, baos);

        assertEquals("Copy this content", baos.toString());
    }

    @Test
    public void testCopy_FileToFile() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "dest.txt");

        Files.write("Content to copy".getBytes(), source);
        Files.copy(source, dest);

        assertTrue(Files.equal(source, dest));
    }

    @Test
    @DisplayName("Test copy with same source and destination throws exception")
    public void testCopySameFile() {
        assertThrows(IllegalArgumentException.class, () -> Files.copy(testFile, testFile));
    }

    @Test
    public void testCopyFileToCharSink() throws IOException {
        File outputFile = tempDir.resolve("copy_charsink.txt").toFile();
        CharSink sink = Files.asCharSink(outputFile, StandardCharsets.UTF_8);
        CharSource source = Files.asCharSource(testFile, StandardCharsets.UTF_8);

        sink.write(source.read());
        assertTrue(outputFile.exists());
        assertTrue(Files.readString(outputFile).contains("Line 1"));
    }

    // Tests for move operation

    @Test
    @DisplayName("Test move File")
    public void testMoveFile() throws IOException {
        File sourceFile = tempDir.resolve("source.txt").toFile();
        File destFile = tempDir.resolve("dest.txt").toFile();

        java.nio.file.Files.write(sourceFile.toPath(), "Move test".getBytes());

        Files.move(sourceFile, destFile);

        assertFalse(sourceFile.exists());
        assertTrue(destFile.exists());
        assertEquals("Move test", new String(java.nio.file.Files.readAllBytes(destFile.toPath())));
    }

    @Test
    public void testMove() throws IOException {
        File source = new File(tempDir.toFile(), "source.txt");
        File dest = new File(tempDir.toFile(), "dest.txt");

        Files.write("Content to move".getBytes(), source);
        Files.move(source, dest);

        assertFalse(source.exists());
        assertTrue(dest.exists());
        assertEquals("Content to move", Files.readString(dest));
    }

    @Test
    public void testMoveWithSameFile() {
        assertThrows(IllegalArgumentException.class, () -> Files.move(testFile, testFile));
    }

    // Tests for readLines

    @Test
    @DisplayName("Test readLines")
    public void testReadLines() throws IOException {
        List<String> lines = Files.readLines(testFile, StandardCharsets.UTF_8);

        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadLinesEmptyFile() throws IOException {
        File emptyFile = tempDir.resolve("empty_lines.txt").toFile();
        emptyFile.createNewFile();
        List<String> lines = Files.readLines(emptyFile, StandardCharsets.UTF_8);
        assertEquals(0, lines.size());
    }

    @Test
    @DisplayName("Test map File with MapMode")
    public void testMapFileWithMode() throws IOException {
        File mapFile = tempDir.resolve("map.txt").toFile();
        java.nio.file.Files.write(mapFile.toPath(), "Mapped".getBytes());

        MappedByteBuffer buffer = Files.map(mapFile, FileChannel.MapMode.READ_ONLY);
        assertNotNull(buffer);
        assertEquals(6, buffer.capacity());

        unmap(buffer);
    }

    @Test
    @DisplayName("Test map with non-existent file")
    public void testMapFileNonExistent() {
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent_map.txt");
        assertThrows(Exception.class, () -> Files.map(nonExistentFile));
    }

    @Test
    @DisplayName("Test map File with size")
    public void testMapFileWithSize() throws IOException {
        File mapFile = tempDir.resolve("mapsize.txt").toFile();
        mapFile.createNewFile();

        MappedByteBuffer buffer = Files.map(mapFile, FileChannel.MapMode.READ_WRITE, 1024);
        assertNotNull(buffer);
        assertEquals(1024, buffer.capacity());

        // Write and read data
        buffer.put(0, (byte) 'X');
        assertEquals('X', (char) buffer.get(0));

        unmap(buffer);
    }

    @Test
    public void testMapFileWithModeAndSize() throws IOException {
        File mapFile = tempDir.resolve("map_mode_size.txt").toFile();
        mapFile.createNewFile();

        MappedByteBuffer buffer = Files.map(mapFile, FileChannel.MapMode.READ_WRITE, 512);
        assertNotNull(buffer);
        assertEquals(512, buffer.capacity());

        buffer.put(0, (byte) 'A');
        buffer.put(1, (byte) 'B');
        assertEquals('A', (char) buffer.get(0));
        assertEquals('B', (char) buffer.get(1));

        unmap(buffer);
    }

    // Tests for path operations

    @Test
    @DisplayName("Test simplifyPath")
    public void testSimplifyPath() {
        assertEquals(".", Files.simplifyPath(""));
        assertEquals(".", Files.simplifyPath("."));
        assertEquals("foo/baz", Files.simplifyPath("foo//bar/../baz/./"));
        assertEquals("/foo/baz", Files.simplifyPath("/foo//bar/../baz/./"));
        assertEquals("../foo", Files.simplifyPath("../foo"));
        assertEquals("/", Files.simplifyPath("/"));
        assertEquals("/", Files.simplifyPath("//"));
        assertEquals("foo", Files.simplifyPath("foo/"));
    }

    @Test
    public void testSimplifyPathEdgeCases() {
        assertEquals("a/b", Files.simplifyPath("a/b"));
        assertEquals("/a", Files.simplifyPath("/a/b/.."));
        assertEquals("/", Files.simplifyPath("/.."));
        assertEquals(".", Files.simplifyPath(""));
    }

    @Test
    @DisplayName("Test getNameWithoutExtension String")
    public void testGetNameWithoutExtensionString() {
        assertEquals("document", Files.getNameWithoutExtension("document.pdf"));
        assertEquals("document", Files.getNameWithoutExtension("/home/user/document.pdf"));
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("README", Files.getNameWithoutExtension("README"));
        assertEquals("", Files.getNameWithoutExtension(".hidden"));
        assertEquals("", Files.getNameWithoutExtension(".txt"));
    }

    @Test
    public void testGetNameWithoutExtension_String() {
        assertEquals("document", Files.getNameWithoutExtension("document.pdf"));
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("README", Files.getNameWithoutExtension("README"));
        assertEquals("document", Files.getNameWithoutExtension("/path/to/document.pdf"));
        assertEquals("", Files.getNameWithoutExtension(".hidden"));
        assertEquals(".hidden", Files.getNameWithoutExtension(".hidden.txt"));
    }

    @Test
    @DisplayName("Test getNameWithoutExtension Path")
    public void testGetNameWithoutExtensionPath() {
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("document.pdf")));
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("/home/user/document.pdf")));
    }

    @Test
    public void testGetNameWithoutExtensionNoExtension() {
        assertEquals("Makefile", Files.getNameWithoutExtension("Makefile"));
        assertEquals("Makefile", Files.getNameWithoutExtension(Paths.get("Makefile")));
    }

    @Test
    @DisplayName("Test getFileExtension String")
    public void testGetFileExtensionString() {
        assertEquals("pdf", Files.getFileExtension("document.pdf"));
        assertEquals("pdf", Files.getFileExtension("/home/user/document.pdf"));
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("", Files.getFileExtension("README"));
        assertEquals("hidden", Files.getFileExtension(".hidden"));
        assertEquals("txt", Files.getFileExtension(".txt"));
    }

    @Test
    public void testGetFileExtension_String() {
        assertEquals("pdf", Files.getFileExtension("document.pdf"));
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("", Files.getFileExtension("README"));
        assertEquals("txt", Files.getFileExtension("/path/to/file.txt"));
        assertEquals("hidden", Files.getFileExtension(".hidden"));
        assertEquals("txt", Files.getFileExtension(".hidden.txt"));
    }

    @Test
    @DisplayName("Test getFileExtension Path")
    public void testGetFileExtensionPath() {
        assertEquals("pdf", Files.getFileExtension(Paths.get("document.pdf")));
        assertEquals("", Files.getFileExtension(Paths.get("README")));
    }

    @Test
    public void testGetFileExtensionNoExtension() {
        assertEquals("", Files.getFileExtension("Makefile"));
        assertEquals("", Files.getFileExtension(Paths.get("Makefile")));
    }

    // Tests for traversers

    @Test
    @DisplayName("Test fileTraverser")
    public void testFileTraverser() throws IOException {
        // Create directory structure
        File dir1 = new File(tempDir.toFile(), "dir1");
        File dir2 = new File(dir1, "dir2");
        File file1 = new File(dir1, "file1.txt");
        File file2 = new File(dir2, "file2.txt");

        dir1.mkdir();
        dir2.mkdir();
        file1.createNewFile();
        file2.createNewFile();

        Traverser<File> traverser = Files.fileTraverser();
        assertNotNull(traverser);

        // Test breadth-first traversal
        List<File> files = new ArrayList<>();
        for (File f : traverser.breadthFirst(dir1)) {
            files.add(f);
        }

        assertTrue(files.size() >= 4); // dir1, dir2, file1, file2
        assertTrue(files.contains(dir1));
        assertTrue(files.contains(file1));
    }

    @Test
    @DisplayName("Test pathTraverser")
    public void testPathTraverser() throws IOException {
        // Create directory structure
        Path dir1 = tempDir.resolve("pdir1");
        Path dir2 = dir1.resolve("pdir2");
        Path file1 = dir1.resolve("pfile1.txt");

        java.nio.file.Files.createDirectory(dir1);
        java.nio.file.Files.createDirectory(dir2);
        java.nio.file.Files.createFile(file1);

        Traverser<Path> traverser = Files.pathTraverser();
        assertNotNull(traverser);

        List<Path> paths = new ArrayList<>();
        for (Path p : traverser.depthFirstPreOrder(dir1)) {
            paths.add(p);
        }

        assertTrue(paths.contains(dir1));
        assertTrue(paths.contains(file1));
    }

    // Tests for listFiles

    @Test
    @DisplayName("Test listFiles")
    public void testListFiles() throws IOException {
        // Create some files
        Path file1 = tempDir.resolve("list1.txt");
        Path file2 = tempDir.resolve("list2.txt");
        Path subDir = tempDir.resolve("subdir");

        java.nio.file.Files.createFile(file1);
        java.nio.file.Files.createFile(file2);
        java.nio.file.Files.createDirectory(subDir);

        ImmutableList<Path> files = Files.listFiles(tempDir);
        assertNotNull(files);
        assertTrue(files.size() >= 2);

        // Should contain files but not necessarily in any order
        List<String> fileNames = files.stream().map(p -> p.getFileName().toString()).collect(Collectors.toList());

        assertTrue(fileNames.contains("list1.txt"));
        assertTrue(fileNames.contains("list2.txt"));
    }

    @Test
    @DisplayName("Test listFiles with non-existent directory")
    public void testListFilesNonExistent() {
        Path nonExistent = tempDir.resolve("nonexistent");
        assertThrows(NoSuchFileException.class, () -> Files.listFiles(nonExistent));
    }

    @Test
    @DisplayName("Test listFiles with regular file instead of directory")
    public void testListFilesWithRegularFile() throws IOException {
        Path regularFile = tempDir.resolve("regularfile.txt");
        java.nio.file.Files.createFile(regularFile);

        assertThrows(Exception.class, () -> Files.listFiles(regularFile));
    }

    // Tests for delete operations

    @Test
    @DisplayName("Test deleteRecursively")
    public void testDeleteRecursively() throws IOException {
        // Create directory structure
        Path rootDir = tempDir.resolve("delete_root");
        Path subDir = rootDir.resolve("sub");
        Path file = subDir.resolve("file.txt");

        java.nio.file.Files.createDirectory(rootDir);
        java.nio.file.Files.createDirectory(subDir);
        java.nio.file.Files.write(file, "content".getBytes());

        assertTrue(java.nio.file.Files.exists(rootDir));

        Files.deleteRecursively(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertFalse(java.nio.file.Files.exists(rootDir));
        assertFalse(java.nio.file.Files.exists(subDir));
        assertFalse(java.nio.file.Files.exists(file));
    }

    @Test
    public void testDeleteRecursivelyEmptyDir() throws IOException {
        Path emptyDir = tempDir.resolve("empty_dir_delete");
        java.nio.file.Files.createDirectory(emptyDir);
        assertTrue(java.nio.file.Files.exists(emptyDir));

        Files.deleteRecursively(emptyDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(emptyDir));
    }

    @Test
    @DisplayName("Test deleteDirectoryContents")
    public void testDeleteDirectoryContents() throws IOException {
        // Create directory structure
        Path rootDir = tempDir.resolve("content_delete");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = rootDir.resolve("file2.txt");
        Path subDir = rootDir.resolve("sub");

        java.nio.file.Files.createDirectory(rootDir);
        java.nio.file.Files.write(file1, "content1".getBytes());
        java.nio.file.Files.write(file2, "content2".getBytes());
        java.nio.file.Files.createDirectory(subDir);

        Files.deleteDirectoryContents(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);

        assertTrue(java.nio.file.Files.exists(rootDir)); // Directory itself remains
        assertFalse(java.nio.file.Files.exists(file1));
        assertFalse(java.nio.file.Files.exists(file2));
        assertFalse(java.nio.file.Files.exists(subDir));
    }

    @Test
    public void testDeleteDirectoryContentsEmpty() throws IOException {
        Path emptyDir = tempDir.resolve("empty_dir_contents");
        java.nio.file.Files.createDirectory(emptyDir);

        Files.deleteDirectoryContents(emptyDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(java.nio.file.Files.exists(emptyDir));
    }

    // Tests for predicates

    @Test
    @DisplayName("Test isDirectory predicate")
    public void testIsDirectoryPredicate() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Path file = tempDir.resolve("testfile.txt");

        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(file);

        Predicate<Path> isDir = Files.isDirectory();

        assertTrue(isDir.apply(dir));
        assertFalse(isDir.apply(file));

        // Test with NOFOLLOW_LINKS
        Predicate<Path> isDirNoFollow = Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isDirNoFollow.apply(dir));
    }

    @Test
    public void testIsDirectory() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(file);

        Predicate<Path> isDirPredicate = Files.isDirectory();
        assertTrue(isDirPredicate.apply(dir));
        assertFalse(isDirPredicate.apply(file));
    }

    @Test
    @DisplayName("Test isRegularFile predicate")
    public void testIsRegularFilePredicate() throws IOException {
        Path dir = tempDir.resolve("testdir2");
        Path file = tempDir.resolve("testfile2.txt");

        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(file);

        Predicate<Path> isFile = Files.isRegularFile();

        assertFalse(isFile.apply(dir));
        assertTrue(isFile.apply(file));

        // Test with NOFOLLOW_LINKS
        Predicate<Path> isFileNoFollow = Files.isRegularFile(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isFileNoFollow.apply(file));
    }

    @Test
    public void testIsRegularFile() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(file);

        Predicate<Path> isFilePredicate = Files.isRegularFile();
        assertFalse(isFilePredicate.apply(dir));
        assertTrue(isFilePredicate.apply(file));
    }

    // Tests for readAllBytes

    @Test
    @DisplayName("Test readAllBytes")
    public void testReadAllBytes() throws IOException {
        byte[] bytes = Files.readAllBytes(testFile);
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        String content = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testReadAllBytesPath() throws IOException {
        byte[] bytes = Files.readAllBytes(testFile);
        byte[] pathBytes = java.nio.file.Files.readAllBytes(testPath);
        assertArrayEquals(pathBytes, bytes);
    }

    // Tests for readString

    @Test
    @DisplayName("Test readString with default charset")
    public void testReadStringDefault() throws IOException {
        String content = Files.readString(testFile);
        assertNotNull(content);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testReadString_Default() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        Files.write("UTF-8 content".getBytes(StandardCharsets.UTF_8), file);

        String content = Files.readString(file);
        assertEquals("UTF-8 content", content);
    }

    @Test
    @DisplayName("Test readString with specific charset")
    public void testReadStringWithCharset() throws IOException {
        String content = Files.readString(testFile, StandardCharsets.UTF_8);
        assertNotNull(content);
        assertTrue(content.contains("Line 1"));
    }

    @Test
    public void testReadString_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        String content = "ISO-8859-1 content: áéíóú";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        String read = Files.readString(file, StandardCharsets.ISO_8859_1);
        assertEquals(content, read);
    }

    // Tests for readAllLines

    @Test
    @DisplayName("Test readAllLines with default charset")
    public void testReadAllLinesDefault() throws IOException {
        List<String> lines = Files.readAllLines(testFile);
        assertNotNull(lines);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadAllLines_Default() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        List<String> lines = Files.readAllLines(file);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    @DisplayName("Test readAllLines with specific charset")
    public void testReadAllLinesWithCharset() throws IOException {
        List<String> lines = Files.readAllLines(testFile, StandardCharsets.UTF_8);
        assertNotNull(lines);
        assertEquals(3, lines.size());
    }

    @Test
    public void testReadAllLines_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        Files.write("Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        assertEquals(2, lines.size());
        assertEquals("Línea 1", lines.get(0));
        assertEquals("Línea 2", lines.get(1));
    }

    // Test for MoreFiles inner class

    @Test
    @DisplayName("Test MoreFiles class exists")
    public void testMoreFilesClass() {
        // MoreFiles is a final class extending Files
        assertNotNull(Files.MoreFiles.class);
        assertTrue(Files.class.isAssignableFrom(Files.MoreFiles.class));
    }

    @Test
    public void testMoreFilesClassIsSubclassOfFiles() {
        assertTrue(Files.class.isAssignableFrom(Files.MoreFiles.class));
    }
}
