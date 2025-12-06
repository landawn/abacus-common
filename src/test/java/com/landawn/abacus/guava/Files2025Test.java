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
import com.landawn.abacus.util.ImmutableList;

/**
 * Comprehensive unit tests for the Files utility class.
 * Tests all public static methods including file I/O operations, path operations,
 * and various utility functions.
 */
@Tag("2025")
public class Files2025Test extends TestBase {

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
    @DisplayName("Test asByteSource with Path and options")
    public void testAsByteSourcePathOptions() throws IOException {
        ByteSource source = Files.asByteSource(testPath, StandardOpenOption.READ);
        assertNotNull(source);

        assertTrue(source.size() > 0);
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
    @DisplayName("Test asByteSink with Path and options")
    public void testAsByteSinkPathOptions() throws IOException {
        Path outputPath = tempDir.resolve("sink2.txt");
        ByteSink sink = Files.asByteSink(outputPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertNotNull(sink);

        sink.write("Path sink test".getBytes());
        assertTrue(java.nio.file.Files.exists(outputPath));
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
    @DisplayName("Test asCharSink with Path and options")
    public void testAsCharSinkPathOptions() throws IOException {
        Path outputPath = tempDir.resolve("charsink2.txt");
        CharSink sink = Files.asCharSink(outputPath, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        assertNotNull(sink);

        sink.writeLines(Arrays.asList("Line A", "Line B"));

        List<String> lines = java.nio.file.Files.readAllLines(outputPath);
        assertEquals(2, lines.size());
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
    @DisplayName("Test write byte array to file")
    public void testWriteByteArray() throws IOException {
        File outputFile = tempDir.resolve("bytewrite.txt").toFile();
        byte[] data = "Byte array data".getBytes(StandardCharsets.UTF_8);

        Files.createParentDirs(outputFile);
        Files.write(data, outputFile);

        byte[] readData = java.nio.file.Files.readAllBytes(outputFile.toPath());
        assertArrayEquals(data, readData);
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
            Thread.sleep(10);   // Small delay to ensure time difference
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        Files.touch(newFile);
        assertTrue(newFile.lastModified() >= initialTime);
    }

    @Test
    @DisplayName("Test touch with Path")
    public void testTouchPath() throws IOException {
        Path newPath = tempDir.resolve("touched2.txt");
        assertFalse(java.nio.file.Files.exists(newPath));

        Files.touch(newPath);
        assertTrue(java.nio.file.Files.exists(newPath));
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
    @DisplayName("Test copy with same source and destination throws exception")
    public void testCopySameFile() {
        assertThrows(IllegalArgumentException.class, () -> Files.copy(testFile, testFile));
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
    @DisplayName("Test getNameWithoutExtension Path")
    public void testGetNameWithoutExtensionPath() {
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("document.pdf")));
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("/home/user/document.pdf")));
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
    @DisplayName("Test getFileExtension Path")
    public void testGetFileExtensionPath() {
        assertEquals("pdf", Files.getFileExtension(Paths.get("document.pdf")));
        assertEquals("", Files.getFileExtension(Paths.get("README")));
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

        assertTrue(files.size() >= 4);   // dir1, dir2, file1, file2
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

        assertTrue(java.nio.file.Files.exists(rootDir));   // Directory itself remains
        assertFalse(java.nio.file.Files.exists(file1));
        assertFalse(java.nio.file.Files.exists(file2));
        assertFalse(java.nio.file.Files.exists(subDir));
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
    @DisplayName("Test readString with specific charset")
    public void testReadStringWithCharset() throws IOException {
        String content = Files.readString(testFile, StandardCharsets.UTF_8);
        assertNotNull(content);
        assertTrue(content.contains("Line 1"));
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
    @DisplayName("Test readAllLines with specific charset")
    public void testReadAllLinesWithCharset() throws IOException {
        List<String> lines = Files.readAllLines(testFile, StandardCharsets.UTF_8);
        assertNotNull(lines);
        assertEquals(3, lines.size());
    }

    // Test for MoreFiles inner class

    @Test
    @DisplayName("Test MoreFiles class exists")
    public void testMoreFilesClass() {
        // MoreFiles is a final class extending Files
        assertNotNull(Files.MoreFiles.class);
        assertTrue(Files.class.isAssignableFrom(Files.MoreFiles.class));
    }

    // Edge cases and error conditions

    @Test
    @DisplayName("Test operations with null parameters")
    public void testNullParameters() {
        assertThrows(NullPointerException.class, () -> Files.newReader(null, StandardCharsets.UTF_8));

        assertThrows(NullPointerException.class, () -> Files.toByteArray(null));
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
}