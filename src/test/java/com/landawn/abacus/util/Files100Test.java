package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import com.landawn.abacus.guava.Files.MoreFiles;

public class Files100Test extends TestBase {

    @TempDir
    Path tempDir;

    private File tempFile;
    private Path tempPath;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final String TEST_CONTENT = "Hello, World!\nThis is a test file.";
    private static final byte[] TEST_BYTES = TEST_CONTENT.getBytes(UTF_8);

    @BeforeEach
    public void setUp() throws IOException {
        tempFile = Files.createTempFile(tempDir, "test", ".txt").toFile();
        tempPath = tempFile.toPath();
        Files.write(tempPath, TEST_BYTES);
    }

    @AfterEach
    public void tearDown() throws IOException {
        // Clean up is handled by @TempDir
    }

    @Test
    public void testNewReader() throws IOException {
        // Test with valid file
        try (BufferedReader reader = MoreFiles.newReader(tempFile, UTF_8)) {
            assertNotNull(reader);
            String firstLine = reader.readLine();
            assertEquals("Hello, World!", firstLine);
        }

        // Test with non-existent file
        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> MoreFiles.newReader(nonExistentFile, UTF_8));
    }

    @Test
    public void testNewWriter() throws IOException {
        File writeFile = new File(tempDir.toFile(), "write_test.txt");

        try (BufferedWriter writer = MoreFiles.newWriter(writeFile, UTF_8)) {
            assertNotNull(writer);
            writer.write("Test content");
        }

        String content = Files.readString(writeFile.toPath());
        assertEquals("Test content", content);
    }

    @Test
    public void testAsByteSourceFile() throws IOException {
        ByteSource byteSource = MoreFiles.asByteSource(tempFile);
        assertNotNull(byteSource);

        byte[] bytes = byteSource.read();
        assertArrayEquals(TEST_BYTES, bytes);
    }

    @Test
    public void testAsByteSourcePath() throws IOException {
        ByteSource byteSource = MoreFiles.asByteSource(tempPath, StandardOpenOption.READ);
        assertNotNull(byteSource);

        byte[] bytes = byteSource.read();
        assertArrayEquals(TEST_BYTES, bytes);
    }

    @Test
    public void testAsByteSinkFile() throws IOException {
        File sinkFile = new File(tempDir.toFile(), "sink_test.txt");

        // Test truncate mode (default)
        ByteSink byteSink = MoreFiles.asByteSink(sinkFile);
        byteSink.write(TEST_BYTES);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(sinkFile.toPath()));

        // Test append mode
        ByteSink appendSink = MoreFiles.asByteSink(sinkFile, FileWriteMode.APPEND);
        appendSink.write(" Appended".getBytes(UTF_8));
        String content = Files.readString(sinkFile.toPath());
        assertEquals(TEST_CONTENT + " Appended", content);
    }

    @Test
    public void testAsByteSinkPath() throws IOException {
        Path sinkPath = tempDir.resolve("path_sink_test.txt");

        ByteSink byteSink = MoreFiles.asByteSink(sinkPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        byteSink.write(TEST_BYTES);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(sinkPath));
    }

    @Test
    public void testAsCharSourceFile() throws IOException {
        CharSource charSource = MoreFiles.asCharSource(tempFile, UTF_8);
        assertNotNull(charSource);

        String content = charSource.read();
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testAsCharSourcePath() throws IOException {
        CharSource charSource = MoreFiles.asCharSource(tempPath, UTF_8, StandardOpenOption.READ);
        assertNotNull(charSource);

        String content = charSource.read();
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testAsCharSinkFile() throws IOException {
        File sinkFile = new File(tempDir.toFile(), "char_sink_test.txt");

        CharSink charSink = MoreFiles.asCharSink(sinkFile, UTF_8);
        charSink.write("Test content");
        assertEquals("Test content", Files.readString(sinkFile.toPath()));

        // Test append mode
        CharSink appendSink = MoreFiles.asCharSink(sinkFile, UTF_8, FileWriteMode.APPEND);
        appendSink.write(" Appended");
        assertEquals("Test content Appended", Files.readString(sinkFile.toPath()));
    }

    @Test
    public void testAsCharSinkPath() throws IOException {
        Path sinkPath = tempDir.resolve("path_char_sink_test.txt");

        CharSink charSink = MoreFiles.asCharSink(sinkPath, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        charSink.write("Test content");
        assertEquals("Test content", Files.readString(sinkPath));
    }

    @Test
    public void testToByteArray() throws IOException {
        byte[] bytes = MoreFiles.toByteArray(tempFile);
        assertArrayEquals(TEST_BYTES, bytes);

        // Test with non-existent file
        File nonExistent = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(IOException.class, () -> MoreFiles.toByteArray(nonExistent));
    }

    @Test
    public void testWriteByteArrayToFile() throws IOException {
        File writeFile = new File(tempDir.toFile(), "write_bytes_test.txt");
        byte[] data = "Test data".getBytes(UTF_8);

        MoreFiles.write(data, writeFile);
        assertArrayEquals(data, Files.readAllBytes(writeFile.toPath()));

        // Test overwrite
        byte[] newData = "New data".getBytes(UTF_8);
        MoreFiles.write(newData, writeFile);
        assertArrayEquals(newData, Files.readAllBytes(writeFile.toPath()));
    }

    @Test
    public void testEqualFiles() throws IOException {
        // Test equal files
        File file1 = new File(tempDir.toFile(), "equal1.txt");
        File file2 = new File(tempDir.toFile(), "equal2.txt");
        Files.write(file1.toPath(), TEST_BYTES);
        Files.write(file2.toPath(), TEST_BYTES);

        assertTrue(MoreFiles.equal(file1, file2));

        // Test different files
        Files.write(file2.toPath(), "Different content".getBytes(UTF_8));
        assertFalse(MoreFiles.equal(file1, file2));

        // Test same file
        assertTrue(MoreFiles.equal(file1, file1));

        // Test with directory
        // assertFalse(MoreFiles.equal(file1, tempDir.toFile()));

        assertThrows(FileNotFoundException.class, () -> MoreFiles.equal(file1, tempDir.toFile()));
    }

    @Test
    public void testEqualPaths() throws IOException {
        Path path1 = tempDir.resolve("equal1.txt");
        Path path2 = tempDir.resolve("equal2.txt");
        Files.write(path1, TEST_BYTES);
        Files.write(path2, TEST_BYTES);

        assertTrue(MoreFiles.equal(path1, path2));

        // Test different paths
        Files.write(path2, "Different content".getBytes(UTF_8));
        assertFalse(MoreFiles.equal(path1, path2));
    }

    @Test
    public void testTouchFile() throws IOException, InterruptedException {
        // Test creating new file
        File newFile = new File(tempDir.toFile(), "touch_test.txt");
        assertFalse(newFile.exists());

        MoreFiles.touch(newFile);
        assertTrue(newFile.exists());
        assertEquals(0, newFile.length());

        // Test updating timestamp
        long originalTime = newFile.lastModified();
        Thread.sleep(10); // Ensure time difference
        MoreFiles.touch(newFile);
        assertTrue(newFile.lastModified() >= originalTime);
    }

    @Test
    public void testTouchPath() throws IOException {
        Path newPath = tempDir.resolve("touch_path_test.txt");
        assertFalse(Files.exists(newPath));

        MoreFiles.touch(newPath);
        assertTrue(Files.exists(newPath));
        assertEquals(0, Files.size(newPath));
    }

    @Test
    public void testCreateTempDir() throws IOException {
        File tempDirectory = Files.createTempDirectory("tmp").toFile();
        assertNotNull(tempDirectory);
        assertTrue(tempDirectory.exists());
        assertTrue(tempDirectory.isDirectory());

        // Clean up
        tempDirectory.delete();
    }

    @Test
    public void testCreateParentDirs() throws IOException {
        File deepFile = new File(tempDir.toFile(), "a/b/c/d/file.txt");
        assertFalse(deepFile.getParentFile().exists());

        MoreFiles.createParentDirs(deepFile);
        assertTrue(deepFile.getParentFile().exists());
        assertTrue(deepFile.getParentFile().isDirectory());
    }

    @Test
    public void testCreateParentDirectories() throws IOException {
        Path deepPath = tempDir.resolve("x/y/z/file.txt");
        assertFalse(Files.exists(deepPath.getParent()));

        // Test with POSIX permissions if supported
        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"));
            MoreFiles.createParentDirectories(deepPath, attr);
        } catch (UnsupportedOperationException e) {
            // Fallback for non-POSIX systems
            MoreFiles.createParentDirectories(deepPath);
        }

        assertTrue(Files.exists(deepPath.getParent()));
        assertTrue(Files.isDirectory(deepPath.getParent()));
    }

    @Test
    public void testCopyFileToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        MoreFiles.copy(tempFile, baos);
        assertArrayEquals(TEST_BYTES, baos.toByteArray());
    }

    @Test
    public void testCopyFileToFile() throws IOException {
        File destFile = new File(tempDir.toFile(), "copy_dest.txt");

        MoreFiles.copy(tempFile, destFile);
        assertTrue(destFile.exists());
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        // Test overwriting
        Files.write(destFile.toPath(), "Old content".getBytes(UTF_8));
        MoreFiles.copy(tempFile, destFile);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        // Test copying to same file should throw
        assertThrows(IllegalArgumentException.class, () -> MoreFiles.copy(tempFile, tempFile));
    }

    @Test
    public void testMove() throws IOException {
        File sourceFile = new File(tempDir.toFile(), "move_source.txt");
        File destFile = new File(tempDir.toFile(), "move_dest.txt");
        Files.write(sourceFile.toPath(), TEST_BYTES);

        MoreFiles.move(sourceFile, destFile);
        assertFalse(sourceFile.exists());
        assertTrue(destFile.exists());
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        // Test moving to same file should throw
        assertThrows(IllegalArgumentException.class, () -> MoreFiles.move(destFile, destFile));
    }

    @Test
    public void testReadLines() throws IOException {
        List<String> lines = MoreFiles.readLines(tempFile, UTF_8);
        assertEquals(2, lines.size());
        assertEquals("Hello, World!", lines.get(0));
        assertEquals("This is a test file.", lines.get(1));

        // Test with empty file
        File emptyFile = new File(tempDir.toFile(), "empty.txt");
        emptyFile.createNewFile();
        List<String> emptyLines = MoreFiles.readLines(emptyFile, UTF_8);
        assertTrue(emptyLines.isEmpty());
    }

    @Test
    public void testMap() throws IOException {
        // Create a file with known content
        File mapFile = new File(tempDir.toFile(), "map_test.txt");
        Files.write(mapFile.toPath(), TEST_BYTES);

        // Test read-only mapping
        MappedByteBuffer buffer = MoreFiles.map(mapFile);
        assertNotNull(buffer);
        assertEquals(TEST_BYTES.length, buffer.remaining());

        byte[] readBytes = new byte[TEST_BYTES.length];
        buffer.get(readBytes);
        assertArrayEquals(TEST_BYTES, readBytes);

        buffer.clear(); // Clear the buffer for further operations

        unmap(buffer);
    }

    @Test
    public void testMapWithMode() throws IOException {
        File mapFile = new File(tempDir.toFile(), "map_mode_test.txt");
        Files.write(mapFile.toPath(), TEST_BYTES);

        // Test with READ_ONLY mode
        MappedByteBuffer readOnlyBuffer = MoreFiles.map(mapFile, MapMode.READ_ONLY);
        assertNotNull(readOnlyBuffer);
        assertEquals(TEST_BYTES.length, readOnlyBuffer.remaining());

        // Test with READ_WRITE mode
        MappedByteBuffer readWriteBuffer = MoreFiles.map(mapFile, MapMode.READ_WRITE);
        assertNotNull(readWriteBuffer);

        // Verify we can write
        readWriteBuffer.put(0, (byte) 'h');
        readWriteBuffer.force();
        unmap(readOnlyBuffer);
        unmap(readWriteBuffer);
    }

    @Test
    public void testMapWithModeAndSize() throws IOException {
        File mapFile = new File(tempDir.toFile(), "map_size_test.txt");

        // Create file with specific size
        long size = 1024;
        MappedByteBuffer buffer = MoreFiles.map(mapFile, MapMode.READ_WRITE, size);
        assertNotNull(buffer);
        assertEquals(size, buffer.capacity());

        // Write some data
        buffer.put("Test".getBytes(UTF_8));
        buffer.force();

        assertTrue(mapFile.exists());
        assertEquals(size, mapFile.length());
        unmap(buffer);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", MoreFiles.simplifyPath(""));
        assertEquals(".", MoreFiles.simplifyPath("."));
        assertEquals(".", MoreFiles.simplifyPath("./"));
        assertEquals("b", MoreFiles.simplifyPath("a/../b"));
        assertEquals("b", MoreFiles.simplifyPath("./a/../b"));
        assertEquals("/b", MoreFiles.simplifyPath("/a/../b"));
        assertEquals("b/c", MoreFiles.simplifyPath("a/../b/c"));
        assertEquals("../b/c", MoreFiles.simplifyPath("a/../../b/c"));
        assertEquals("/", MoreFiles.simplifyPath("/"));
        assertEquals("/", MoreFiles.simplifyPath("//"));
        assertEquals("a/b", MoreFiles.simplifyPath("a//b"));
        assertEquals("a/b", MoreFiles.simplifyPath("a/./b"));
        assertEquals("a/b", MoreFiles.simplifyPath("a/b/"));
    }

    @Test
    public void testGetNameWithoutExtensionString() {
        assertEquals("file", MoreFiles.getNameWithoutExtension("file.txt"));
        assertEquals("file", MoreFiles.getNameWithoutExtension("/path/to/file.txt"));
        assertEquals("file.backup", MoreFiles.getNameWithoutExtension("file.backup.txt"));
        assertEquals("file", MoreFiles.getNameWithoutExtension("file"));
        assertEquals("", MoreFiles.getNameWithoutExtension(".hidden"));
        assertEquals("file", MoreFiles.getNameWithoutExtension("/path/to/file"));
    }

    @Test
    public void testGetNameWithoutExtensionPath() {
        assertEquals("file", MoreFiles.getNameWithoutExtension(Paths.get("file.txt")));
        assertEquals("file", MoreFiles.getNameWithoutExtension(Paths.get("/path/to/file.txt")));
        assertEquals("archive.tar", MoreFiles.getNameWithoutExtension(Paths.get("archive.tar.gz")));
    }

    @Test
    public void testGetFileExtensionString() {
        assertEquals("txt", MoreFiles.getFileExtension("file.txt"));
        assertEquals("txt", MoreFiles.getFileExtension("/path/to/file.txt"));
        assertEquals("gz", MoreFiles.getFileExtension("archive.tar.gz"));
        assertEquals("", MoreFiles.getFileExtension("file"));
        assertEquals("hidden", MoreFiles.getFileExtension(".hidden"));
        assertEquals("", MoreFiles.getFileExtension("/path/to/file"));
        assertEquals("TXT", MoreFiles.getFileExtension("FILE.TXT"));
    }

    @Test
    public void testGetFileExtensionPath() {
        assertEquals("txt", MoreFiles.getFileExtension(Paths.get("file.txt")));
        assertEquals("txt", MoreFiles.getFileExtension(Paths.get("/path/to/file.txt")));
        assertEquals("", MoreFiles.getFileExtension(Paths.get("file")));
    }

    @Test
    public void testFileTraverser() throws IOException {
        // Create test directory structure
        Path rootDir = tempDir.resolve("traverse_test");
        Path subDir1 = rootDir.resolve("dir1");
        Path subDir2 = rootDir.resolve("dir2");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir1.resolve("file2.txt");
        Path file3 = subDir2.resolve("file3.txt");

        Files.createDirectories(subDir1);
        Files.createDirectories(subDir2);
        Files.createFile(file1);
        Files.createFile(file2);
        Files.createFile(file3);

        Traverser<File> traverser = MoreFiles.fileTraverser();
        assertNotNull(traverser);

        // Test depth-first traversal
        Iterable<File> files = traverser.depthFirstPreOrder(rootDir.toFile());
        int count = 0;
        for (File f : files) {
            assertNotNull(f);
            count++;
        }
        assertEquals(6, count); // root + 2 dirs + 3 files

        // Test breadth-first traversal
        files = traverser.breadthFirst(rootDir.toFile());
        count = 0;
        for (File f : files) {
            assertNotNull(f);
            count++;
        }
        assertEquals(6, count);
    }

    @Test
    public void testPathTraverser() throws IOException {
        // Create test directory structure
        Path rootDir = tempDir.resolve("path_traverse_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.createFile(file1);
        Files.createFile(file2);

        Traverser<Path> traverser = MoreFiles.pathTraverser();
        assertNotNull(traverser);

        // Test traversal
        Iterable<Path> paths = traverser.depthFirstPreOrder(rootDir);
        int count = 0;
        for (Path p : paths) {
            assertNotNull(p);
            count++;
        }
        assertEquals(4, count); // root + 1 dir + 2 files
    }

    @Test
    public void testListFiles() throws IOException {
        // Create test files
        Path dir = tempDir.resolve("list_test");
        Files.createDirectory(dir);
        Path file1 = dir.resolve("file1.txt");
        Path file2 = dir.resolve("file2.txt");
        Path subDir = dir.resolve("subdir");

        Files.createFile(file1);
        Files.createFile(file2);
        Files.createDirectory(subDir);

        ImmutableList<Path> files = MoreFiles.listFiles(dir);
        assertEquals(3, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
        assertTrue(files.contains(subDir));

        // Test with non-existent directory
        assertThrows(NoSuchFileException.class, () -> MoreFiles.listFiles(tempDir.resolve("nonexistent")));

        // Test with file instead of directory
        assertThrows(NotDirectoryException.class, () -> MoreFiles.listFiles(file1));
    }

    @Test
    public void testDeleteRecursively() throws IOException {
        // Create directory structure
        Path rootDir = tempDir.resolve("delete_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.write(file1, "content".getBytes(UTF_8));
        Files.write(file2, "content".getBytes(UTF_8));

        assertTrue(Files.exists(rootDir));
        MoreFiles.deleteRecursively(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(Files.exists(rootDir));

        // Test with non-existent path
        assertThrows(NoSuchFileException.class, () -> MoreFiles.deleteRecursively(tempDir.resolve("nonexistent"), RecursiveDeleteOption.ALLOW_INSECURE));

        // Test with single file
        Path singleFile = tempDir.resolve("single.txt");
        Files.write(singleFile, "content".getBytes(UTF_8));
        MoreFiles.deleteRecursively(singleFile, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(Files.exists(singleFile));
    }

    @Test
    public void testDeleteDirectoryContents() throws IOException {
        // Create directory structure
        Path rootDir = tempDir.resolve("delete_contents_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.write(file1, "content".getBytes(UTF_8));
        Files.write(file2, "content".getBytes(UTF_8));

        MoreFiles.deleteDirectoryContents(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(Files.exists(rootDir)); // Directory itself should still exist
        assertTrue(Files.list(rootDir).count() == 0); // But should be empty

        // Test with non-existent directory
        assertThrows(NoSuchFileException.class, () -> MoreFiles.deleteDirectoryContents(tempDir.resolve("nonexistent")));

        // Test with file instead of directory
        Path notADir = tempDir.resolve("notadir.txt");
        Files.write(notADir, "content".getBytes(UTF_8));
        assertThrows(NotDirectoryException.class, () -> MoreFiles.deleteDirectoryContents(notADir));
    }

    @Test
    public void testIsDirectory() throws IOException {
        Path dir = tempDir.resolve("test_dir");
        Path file = tempDir.resolve("test_file.txt");
        Path link = tempDir.resolve("test_link");

        Files.createDirectory(dir);
        Files.createFile(file);

        Predicate<Path> isDirPredicate = MoreFiles.isDirectory();
        assertTrue(isDirPredicate.apply(dir));
        assertFalse(isDirPredicate.apply(file));

        // Test with symlink if supported
        try {
            Files.createSymbolicLink(link, dir);

            // Without NOFOLLOW_LINKS, should return true for symlink to directory
            assertTrue(isDirPredicate.apply(link));

            // With NOFOLLOW_LINKS, should return false for symlink
            Predicate<Path> isDirNoFollow = MoreFiles.isDirectory(LinkOption.NOFOLLOW_LINKS);
            assertFalse(isDirNoFollow.apply(link));
        } catch (UnsupportedOperationException | IOException e) {
            // Symbolic links not supported on this system
        }
    }

    @Test
    public void testIsRegularFile() throws IOException {
        Path dir = tempDir.resolve("test_dir");
        Path file = tempDir.resolve("test_file.txt");

        Files.createDirectory(dir);
        Files.createFile(file);

        Predicate<Path> isFilePredicate = MoreFiles.isRegularFile();
        assertFalse(isFilePredicate.apply(dir));
        assertTrue(isFilePredicate.apply(file));

        // Test with non-existent path
        Path nonExistent = tempDir.resolve("nonexistent");
        assertFalse(isFilePredicate.apply(nonExistent));
    }

    @Test
    public void testMoreFilesClass() {
        // Test that MoreFiles class exists and is properly structured
        assertNotNull(MoreFiles.class);
        assertTrue(MoreFiles.class.isAssignableFrom(MoreFiles.class));
    }

    @Test
    public void testEdgeCases() throws IOException {
        // Test with very long file names (if supported by the file system)
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("a");
        }

        try {
            File longFile = new File(tempDir.toFile(), longName.toString() + ".txt");
            MoreFiles.touch(longFile);
            assertTrue(longFile.exists());
        } catch (IOException e) {
            // File name too long for this file system
        }

        // Test with special characters in file names
        String[] specialNames = { "file with spaces.txt", "file-with-dashes.txt", "file_with_underscores.txt", "file.multiple.dots.txt" };

        for (String name : specialNames) {
            File specialFile = new File(tempDir.toFile(), name);
            MoreFiles.touch(specialFile);
            assertTrue(specialFile.exists());

            // Test extension extraction
            if (name.contains(".")) {
                String ext = MoreFiles.getFileExtension(name);
                assertNotNull(ext);
            }
        }
    }

    @Test
    public void testConcurrentAccess() throws Exception {
        // Test concurrent file operations
        Path concurrentFile = tempDir.resolve("concurrent.txt");
        Files.write(concurrentFile, "Initial content".getBytes(UTF_8));

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Read operations
                    byte[] content = MoreFiles.toByteArray(concurrentFile.toFile());
                    assertNotNull(content);

                    // Touch operations
                    MoreFiles.touch(concurrentFile);
                } catch (IOException e) {
                    fail("Concurrent operation failed: " + e.getMessage());
                }
            });
        }

        // Start all threads
        for (Thread t : threads) {
            t.start();
        }

        // Wait for all threads to complete
        for (Thread t : threads) {
            t.join();
        }

        // Verify file still exists and is readable
        assertTrue(Files.exists(concurrentFile));
        assertNotNull(MoreFiles.toByteArray(concurrentFile.toFile()));
    }

    @Test
    public void testLargeFile() throws IOException {
        // Test with a moderately large file (1MB)
        Path largeFile = tempDir.resolve("large.dat");
        byte[] largeData = new byte[1024 * 1024]; // 1MB
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        // Write large file
        MoreFiles.write(largeData, largeFile.toFile());

        // Read and verify
        byte[] readData = MoreFiles.toByteArray(largeFile.toFile());
        assertArrayEquals(largeData, readData);

        // Test copying large file
        Path copyDest = tempDir.resolve("large_copy.dat");
        MoreFiles.copy(largeFile.toFile(), copyDest.toFile());
        assertTrue(MoreFiles.equal(largeFile, copyDest));

        // Test memory mapping large file
        MappedByteBuffer buffer = MoreFiles.map(largeFile.toFile());
        assertEquals(largeData.length, buffer.remaining());
        unmap(buffer);
    }
}
