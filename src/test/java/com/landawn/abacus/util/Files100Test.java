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
import com.landawn.abacus.guava.Files.MoreFiles;

@Tag("new-test")
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
    }

    @Test
    public void testNewReader() throws IOException {
        try (BufferedReader reader = com.landawn.abacus.guava.Files.newReader(tempFile, UTF_8)) {
            assertNotNull(reader);
            String firstLine = reader.readLine();
            assertEquals("Hello, World!", firstLine);
        }

        File nonExistentFile = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> com.landawn.abacus.guava.Files.newReader(nonExistentFile, UTF_8));
    }

    @Test
    public void testNewWriter() throws IOException {
        File writeFile = new File(tempDir.toFile(), "write_test.txt");

        try (BufferedWriter writer = com.landawn.abacus.guava.Files.newWriter(writeFile, UTF_8)) {
            assertNotNull(writer);
            writer.write("Test content");
        }

        String content = Files.readString(writeFile.toPath());
        assertEquals("Test content", content);
    }

    @Test
    public void testAsByteSourceFile() throws IOException {
        ByteSource byteSource = com.landawn.abacus.guava.Files.asByteSource(tempFile);
        assertNotNull(byteSource);

        byte[] bytes = byteSource.read();
        assertArrayEquals(TEST_BYTES, bytes);
    }

    @Test
    public void testAsByteSourcePath() throws IOException {
        ByteSource byteSource = com.landawn.abacus.guava.Files.asByteSource(tempPath, StandardOpenOption.READ);
        assertNotNull(byteSource);

        byte[] bytes = byteSource.read();
        assertArrayEquals(TEST_BYTES, bytes);
    }

    @Test
    public void testAsByteSinkFile() throws IOException {
        File sinkFile = new File(tempDir.toFile(), "sink_test.txt");

        ByteSink byteSink = com.landawn.abacus.guava.Files.asByteSink(sinkFile);
        byteSink.write(TEST_BYTES);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(sinkFile.toPath()));

        ByteSink appendSink = com.landawn.abacus.guava.Files.asByteSink(sinkFile, FileWriteMode.APPEND);
        appendSink.write(" Appended".getBytes(UTF_8));
        String content = Files.readString(sinkFile.toPath());
        assertEquals(TEST_CONTENT + " Appended", content);
    }

    @Test
    public void testAsByteSinkPath() throws IOException {
        Path sinkPath = tempDir.resolve("path_sink_test.txt");

        ByteSink byteSink = com.landawn.abacus.guava.Files.asByteSink(sinkPath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        byteSink.write(TEST_BYTES);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(sinkPath));
    }

    @Test
    public void testAsCharSourceFile() throws IOException {
        CharSource charSource = com.landawn.abacus.guava.Files.asCharSource(tempFile, UTF_8);
        assertNotNull(charSource);

        String content = charSource.read();
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testAsCharSourcePath() throws IOException {
        CharSource charSource = com.landawn.abacus.guava.Files.asCharSource(tempPath, UTF_8, StandardOpenOption.READ);
        assertNotNull(charSource);

        String content = charSource.read();
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testAsCharSinkFile() throws IOException {
        File sinkFile = new File(tempDir.toFile(), "char_sink_test.txt");

        CharSink charSink = com.landawn.abacus.guava.Files.asCharSink(sinkFile, UTF_8);
        charSink.write("Test content");
        assertEquals("Test content", Files.readString(sinkFile.toPath()));

        CharSink appendSink = com.landawn.abacus.guava.Files.asCharSink(sinkFile, UTF_8, FileWriteMode.APPEND);
        appendSink.write(" Appended");
        assertEquals("Test content Appended", Files.readString(sinkFile.toPath()));
    }

    @Test
    public void testAsCharSinkPath() throws IOException {
        Path sinkPath = tempDir.resolve("path_char_sink_test.txt");

        CharSink charSink = com.landawn.abacus.guava.Files.asCharSink(sinkPath, UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        charSink.write("Test content");
        assertEquals("Test content", Files.readString(sinkPath));
    }

    @Test
    public void testToByteArray() throws IOException {
        byte[] bytes = com.landawn.abacus.guava.Files.toByteArray(tempFile);
        assertArrayEquals(TEST_BYTES, bytes);

        File nonExistent = new File(tempDir.toFile(), "nonexistent.txt");
        assertThrows(IOException.class, () -> com.landawn.abacus.guava.Files.toByteArray(nonExistent));
    }

    @Test
    public void testWriteByteArrayToFile() throws IOException {
        File writeFile = new File(tempDir.toFile(), "write_bytes_test.txt");
        byte[] data = "Test data".getBytes(UTF_8);

        com.landawn.abacus.guava.Files.write(data, writeFile);
        assertArrayEquals(data, Files.readAllBytes(writeFile.toPath()));

        byte[] newData = "New data".getBytes(UTF_8);
        com.landawn.abacus.guava.Files.write(newData, writeFile);
        assertArrayEquals(newData, Files.readAllBytes(writeFile.toPath()));
    }

    @Test
    public void testEqualFiles() throws IOException {
        File file1 = new File(tempDir.toFile(), "equal1.txt");
        File file2 = new File(tempDir.toFile(), "equal2.txt");
        Files.write(file1.toPath(), TEST_BYTES);
        Files.write(file2.toPath(), TEST_BYTES);

        assertTrue(com.landawn.abacus.guava.Files.equal(file1, file2));

        Files.write(file2.toPath(), "Different content".getBytes(UTF_8));
        assertFalse(com.landawn.abacus.guava.Files.equal(file1, file2));

        assertTrue(com.landawn.abacus.guava.Files.equal(file1, file1));

        assertThrows(FileNotFoundException.class, () -> com.landawn.abacus.guava.Files.equal(file1, tempDir.toFile()));
    }

    @Test
    public void testEqualPaths() throws IOException {
        Path path1 = tempDir.resolve("equal1.txt");
        Path path2 = tempDir.resolve("equal2.txt");
        Files.write(path1, TEST_BYTES);
        Files.write(path2, TEST_BYTES);

        assertTrue(com.landawn.abacus.guava.Files.equal(path1, path2));

        Files.write(path2, "Different content".getBytes(UTF_8));
        assertFalse(com.landawn.abacus.guava.Files.equal(path1, path2));
    }

    @Test
    public void testTouchFile() throws IOException, InterruptedException {
        File newFile = new File(tempDir.toFile(), "touch_test.txt");
        assertFalse(newFile.exists());

        com.landawn.abacus.guava.Files.touch(newFile);
        assertTrue(newFile.exists());
        assertEquals(0, newFile.length());

        long originalTime = newFile.lastModified();
        Thread.sleep(10);
        com.landawn.abacus.guava.Files.touch(newFile);
        assertTrue(newFile.lastModified() >= originalTime);
    }

    @Test
    public void testTouchPath() throws IOException {
        Path newPath = tempDir.resolve("touch_path_test.txt");
        assertFalse(Files.exists(newPath));

        com.landawn.abacus.guava.Files.touch(newPath);
        assertTrue(Files.exists(newPath));
        assertEquals(0, Files.size(newPath));
    }

    @Test
    public void testCreateTempDir() throws IOException {
        File tempDirectory = Files.createTempDirectory("tmp").toFile();
        assertNotNull(tempDirectory);
        assertTrue(tempDirectory.exists());
        assertTrue(tempDirectory.isDirectory());

        tempDirectory.delete();
    }

    @Test
    public void testCreateParentDirs() throws IOException {
        File deepFile = new File(tempDir.toFile(), "a/b/c/d/file.txt");
        assertFalse(deepFile.getParentFile().exists());

        com.landawn.abacus.guava.Files.createParentDirs(deepFile);
        assertTrue(deepFile.getParentFile().exists());
        assertTrue(deepFile.getParentFile().isDirectory());
    }

    @Test
    public void testCreateParentDirectories() throws IOException {
        Path deepPath = tempDir.resolve("x/y/z/file.txt");
        assertFalse(Files.exists(deepPath.getParent()));

        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-xr-x"));
            com.landawn.abacus.guava.Files.createParentDirectories(deepPath, attr);
        } catch (UnsupportedOperationException e) {
            com.landawn.abacus.guava.Files.createParentDirectories(deepPath);
        }

        assertTrue(Files.exists(deepPath.getParent()));
        assertTrue(Files.isDirectory(deepPath.getParent()));
    }

    @Test
    public void testCopyFileToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        com.landawn.abacus.guava.Files.copy(tempFile, baos);
        assertArrayEquals(TEST_BYTES, baos.toByteArray());
    }

    @Test
    public void testCopyFileToFile() throws IOException {
        File destFile = new File(tempDir.toFile(), "copy_dest.txt");

        com.landawn.abacus.guava.Files.copy(tempFile, destFile);
        assertTrue(destFile.exists());
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        Files.write(destFile.toPath(), "Old content".getBytes(UTF_8));
        com.landawn.abacus.guava.Files.copy(tempFile, destFile);
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        assertThrows(IllegalArgumentException.class, () -> com.landawn.abacus.guava.Files.copy(tempFile, tempFile));
    }

    @Test
    public void testMove() throws IOException {
        File sourceFile = new File(tempDir.toFile(), "move_source.txt");
        File destFile = new File(tempDir.toFile(), "move_dest.txt");
        Files.write(sourceFile.toPath(), TEST_BYTES);

        com.landawn.abacus.guava.Files.move(sourceFile, destFile);
        assertFalse(sourceFile.exists());
        assertTrue(destFile.exists());
        assertArrayEquals(TEST_BYTES, Files.readAllBytes(destFile.toPath()));

        assertThrows(IllegalArgumentException.class, () -> com.landawn.abacus.guava.Files.move(destFile, destFile));
    }

    @Test
    public void testReadLines() throws IOException {
        List<String> lines = com.landawn.abacus.guava.Files.readLines(tempFile, UTF_8);
        assertEquals(2, lines.size());
        assertEquals("Hello, World!", lines.get(0));
        assertEquals("This is a test file.", lines.get(1));

        File emptyFile = new File(tempDir.toFile(), "empty.txt");
        emptyFile.createNewFile();
        List<String> emptyLines = com.landawn.abacus.guava.Files.readLines(emptyFile, UTF_8);
        assertTrue(emptyLines.isEmpty());
    }

    @Test
    public void testMap() throws IOException {
        File mapFile = new File(tempDir.toFile(), "map_test.txt");
        Files.write(mapFile.toPath(), TEST_BYTES);

        MappedByteBuffer buffer = com.landawn.abacus.guava.Files.map(mapFile);
        assertNotNull(buffer);
        assertEquals(TEST_BYTES.length, buffer.remaining());

        byte[] readBytes = new byte[TEST_BYTES.length];
        buffer.get(readBytes);
        assertArrayEquals(TEST_BYTES, readBytes);

        buffer.clear();

        unmap(buffer);
    }

    @Test
    public void testMapWithMode() throws IOException {
        File mapFile = new File(tempDir.toFile(), "map_mode_test.txt");
        Files.write(mapFile.toPath(), TEST_BYTES);

        MappedByteBuffer readOnlyBuffer = com.landawn.abacus.guava.Files.map(mapFile, MapMode.READ_ONLY);
        assertNotNull(readOnlyBuffer);
        assertEquals(TEST_BYTES.length, readOnlyBuffer.remaining());

        MappedByteBuffer readWriteBuffer = com.landawn.abacus.guava.Files.map(mapFile, MapMode.READ_WRITE);
        assertNotNull(readWriteBuffer);

        readWriteBuffer.put(0, (byte) 'h');
        readWriteBuffer.force();
        unmap(readOnlyBuffer);
        unmap(readWriteBuffer);
    }

    @Test
    public void testMapWithModeAndSize() throws IOException {
        File mapFile = new File(tempDir.toFile(), "map_size_test.txt");

        long size = 1024;
        MappedByteBuffer buffer = com.landawn.abacus.guava.Files.map(mapFile, MapMode.READ_WRITE, size);
        assertNotNull(buffer);
        assertEquals(size, buffer.capacity());

        buffer.put("Test".getBytes(UTF_8));
        buffer.force();

        assertTrue(mapFile.exists());
        assertEquals(size, mapFile.length());
        unmap(buffer);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", com.landawn.abacus.guava.Files.simplifyPath(""));
        assertEquals(".", com.landawn.abacus.guava.Files.simplifyPath("."));
        assertEquals(".", com.landawn.abacus.guava.Files.simplifyPath("./"));
        assertEquals("b", com.landawn.abacus.guava.Files.simplifyPath("a/../b"));
        assertEquals("b", com.landawn.abacus.guava.Files.simplifyPath("./a/../b"));
        assertEquals("/b", com.landawn.abacus.guava.Files.simplifyPath("/a/../b"));
        assertEquals("b/c", com.landawn.abacus.guava.Files.simplifyPath("a/../b/c"));
        assertEquals("../b/c", com.landawn.abacus.guava.Files.simplifyPath("a/../../b/c"));
        assertEquals("/", com.landawn.abacus.guava.Files.simplifyPath("/"));
        assertEquals("/", com.landawn.abacus.guava.Files.simplifyPath("//"));
        assertEquals("a/b", com.landawn.abacus.guava.Files.simplifyPath("a//b"));
        assertEquals("a/b", com.landawn.abacus.guava.Files.simplifyPath("a/./b"));
        assertEquals("a/b", com.landawn.abacus.guava.Files.simplifyPath("a/b/"));
    }

    @Test
    public void testGetNameWithoutExtensionString() {
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension("file.txt"));
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension("/path/to/file.txt"));
        assertEquals("file.backup", com.landawn.abacus.guava.Files.getNameWithoutExtension("file.backup.txt"));
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension("file"));
        assertEquals("", com.landawn.abacus.guava.Files.getNameWithoutExtension(".hidden"));
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension("/path/to/file"));
    }

    @Test
    public void testGetNameWithoutExtensionPath() {
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension(Paths.get("file.txt")));
        assertEquals("file", com.landawn.abacus.guava.Files.getNameWithoutExtension(Paths.get("/path/to/file.txt")));
        assertEquals("archive.tar", com.landawn.abacus.guava.Files.getNameWithoutExtension(Paths.get("archive.tar.gz")));
    }

    @Test
    public void testGetFileExtensionString() {
        assertEquals("txt", com.landawn.abacus.guava.Files.getFileExtension("file.txt"));
        assertEquals("txt", com.landawn.abacus.guava.Files.getFileExtension("/path/to/file.txt"));
        assertEquals("gz", com.landawn.abacus.guava.Files.getFileExtension("archive.tar.gz"));
        assertEquals("", com.landawn.abacus.guava.Files.getFileExtension("file"));
        assertEquals("hidden", com.landawn.abacus.guava.Files.getFileExtension(".hidden"));
        assertEquals("", com.landawn.abacus.guava.Files.getFileExtension("/path/to/file"));
        assertEquals("TXT", com.landawn.abacus.guava.Files.getFileExtension("FILE.TXT"));
    }

    @Test
    public void testGetFileExtensionPath() {
        assertEquals("txt", com.landawn.abacus.guava.Files.getFileExtension(Paths.get("file.txt")));
        assertEquals("txt", com.landawn.abacus.guava.Files.getFileExtension(Paths.get("/path/to/file.txt")));
        assertEquals("", com.landawn.abacus.guava.Files.getFileExtension(Paths.get("file")));
    }

    @Test
    public void testFileTraverser() throws IOException {
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

        Traverser<File> traverser = com.landawn.abacus.guava.Files.fileTraverser();
        assertNotNull(traverser);

        Iterable<File> files = traverser.depthFirstPreOrder(rootDir.toFile());
        int count = 0;
        for (File f : files) {
            assertNotNull(f);
            count++;
        }
        assertEquals(6, count);

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
        Path rootDir = tempDir.resolve("path_traverse_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.createFile(file1);
        Files.createFile(file2);

        Traverser<Path> traverser = com.landawn.abacus.guava.Files.pathTraverser();
        assertNotNull(traverser);

        Iterable<Path> paths = traverser.depthFirstPreOrder(rootDir);
        int count = 0;
        for (Path p : paths) {
            assertNotNull(p);
            count++;
        }
        assertEquals(4, count);
    }

    @Test
    public void testListFiles() throws IOException {
        Path dir = tempDir.resolve("list_test");
        Files.createDirectory(dir);
        Path file1 = dir.resolve("file1.txt");
        Path file2 = dir.resolve("file2.txt");
        Path subDir = dir.resolve("subdir");

        Files.createFile(file1);
        Files.createFile(file2);
        Files.createDirectory(subDir);

        ImmutableList<Path> files = com.landawn.abacus.guava.Files.listFiles(dir);
        assertEquals(3, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
        assertTrue(files.contains(subDir));

        assertThrows(NoSuchFileException.class, () -> com.landawn.abacus.guava.Files.listFiles(tempDir.resolve("nonexistent")));

        assertThrows(NotDirectoryException.class, () -> com.landawn.abacus.guava.Files.listFiles(file1));
    }

    @Test
    public void testDeleteRecursively() throws IOException {
        Path rootDir = tempDir.resolve("delete_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.write(file1, "content".getBytes(UTF_8));
        Files.write(file2, "content".getBytes(UTF_8));

        assertTrue(Files.exists(rootDir));
        com.landawn.abacus.guava.Files.deleteRecursively(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(Files.exists(rootDir));

        assertThrows(NoSuchFileException.class,
                () -> com.landawn.abacus.guava.Files.deleteRecursively(tempDir.resolve("nonexistent"), RecursiveDeleteOption.ALLOW_INSECURE));

        Path singleFile = tempDir.resolve("single.txt");
        Files.write(singleFile, "content".getBytes(UTF_8));
        com.landawn.abacus.guava.Files.deleteRecursively(singleFile, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(Files.exists(singleFile));
    }

    @Test
    public void testDeleteDirectoryContents() throws IOException {
        Path rootDir = tempDir.resolve("delete_contents_test");
        Path subDir = rootDir.resolve("subdir");
        Path file1 = rootDir.resolve("file1.txt");
        Path file2 = subDir.resolve("file2.txt");

        Files.createDirectories(subDir);
        Files.write(file1, "content".getBytes(UTF_8));
        Files.write(file2, "content".getBytes(UTF_8));

        com.landawn.abacus.guava.Files.deleteDirectoryContents(rootDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(Files.exists(rootDir));
        assertTrue(Files.list(rootDir).count() == 0);

        assertThrows(NoSuchFileException.class, () -> com.landawn.abacus.guava.Files.deleteDirectoryContents(tempDir.resolve("nonexistent")));

        Path notADir = tempDir.resolve("notadir.txt");
        Files.write(notADir, "content".getBytes(UTF_8));
        assertThrows(NotDirectoryException.class, () -> com.landawn.abacus.guava.Files.deleteDirectoryContents(notADir));
    }

    @Test
    public void testIsDirectory() throws IOException {
        Path dir = tempDir.resolve("test_dir");
        Path file = tempDir.resolve("test_file.txt");
        Path link = tempDir.resolve("test_link");

        Files.createDirectory(dir);
        Files.createFile(file);

        Predicate<Path> isDirPredicate = com.landawn.abacus.guava.Files.isDirectory();
        assertTrue(isDirPredicate.apply(dir));
        assertFalse(isDirPredicate.apply(file));

        try {
            Files.createSymbolicLink(link, dir);

            assertTrue(isDirPredicate.apply(link));

            Predicate<Path> isDirNoFollow = com.landawn.abacus.guava.Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
            assertFalse(isDirNoFollow.apply(link));
        } catch (UnsupportedOperationException | IOException e) {
        }
    }

    @Test
    public void testIsRegularFile() throws IOException {
        Path dir = tempDir.resolve("test_dir");
        Path file = tempDir.resolve("test_file.txt");

        Files.createDirectory(dir);
        Files.createFile(file);

        Predicate<Path> isFilePredicate = com.landawn.abacus.guava.Files.isRegularFile();
        assertFalse(isFilePredicate.apply(dir));
        assertTrue(isFilePredicate.apply(file));

        Path nonExistent = tempDir.resolve("nonexistent");
        assertFalse(isFilePredicate.apply(nonExistent));
    }

    @Test
    public void testMoreFilesClass() {
        assertNotNull(MoreFiles.class);
        assertTrue(MoreFiles.class.isAssignableFrom(MoreFiles.class));
    }

    @Test
    public void testEdgeCases() throws IOException {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("a");
        }

        try {
            File longFile = new File(tempDir.toFile(), longName.toString() + ".txt");
            com.landawn.abacus.guava.Files.touch(longFile);
            assertTrue(longFile.exists());
        } catch (IOException e) {
        }

        String[] specialNames = { "file with spaces.txt", "file-with-dashes.txt", "file_with_underscores.txt", "file.multiple.dots.txt" };

        for (String name : specialNames) {
            File specialFile = new File(tempDir.toFile(), name);
            com.landawn.abacus.guava.Files.touch(specialFile);
            assertTrue(specialFile.exists());

            if (name.contains(".")) {
                String ext = com.landawn.abacus.guava.Files.getFileExtension(name);
                assertNotNull(ext);
            }
        }
    }

    @Test
    public void testConcurrentAccess() throws Exception {
        Path concurrentFile = tempDir.resolve("concurrent.txt");
        Files.write(concurrentFile, "Initial content".getBytes(UTF_8));

        int threadCount = 10;
        Thread[] threads = new Thread[threadCount];

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    byte[] content = com.landawn.abacus.guava.Files.toByteArray(concurrentFile.toFile());
                    assertNotNull(content);

                    com.landawn.abacus.guava.Files.touch(concurrentFile);
                } catch (IOException e) {
                    fail("Concurrent operation failed: " + e.getMessage());
                }
            });
        }

        for (Thread t : threads) {
            t.start();
        }

        for (Thread t : threads) {
            t.join();
        }

        assertTrue(Files.exists(concurrentFile));
        assertNotNull(com.landawn.abacus.guava.Files.toByteArray(concurrentFile.toFile()));
    }

    @Test
    public void testLargeFile() throws IOException {
        Path largeFile = tempDir.resolve("large.dat");
        byte[] largeData = new byte[1024 * 1024];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }

        com.landawn.abacus.guava.Files.write(largeData, largeFile.toFile());

        byte[] readData = com.landawn.abacus.guava.Files.toByteArray(largeFile.toFile());
        assertArrayEquals(largeData, readData);

        Path copyDest = tempDir.resolve("large_copy.dat");
        com.landawn.abacus.guava.Files.copy(largeFile.toFile(), copyDest.toFile());
        assertTrue(com.landawn.abacus.guava.Files.equal(largeFile, copyDest));

        MappedByteBuffer buffer = com.landawn.abacus.guava.Files.map(largeFile.toFile());
        assertEquals(largeData.length, buffer.remaining());
        unmap(buffer);
    }
}
