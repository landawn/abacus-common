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
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Predicate;
import com.google.common.graph.Traverser;
import com.google.common.io.ByteSink;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.InsecureRecursiveDeleteException;
import com.google.common.io.RecursiveDeleteOption;
import com.landawn.abacus.GeneratedTestUtil;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.guava.Files; // The class to be tested

public class GuavaFiles200Test extends TestBase {

    @TempDir
    Path tempDir;

    private File createTempFile(String prefix, String suffix) throws IOException {
        return java.nio.file.Files.createTempFile(tempDir, prefix, suffix).toFile();
    }

    private Path createTempPath(String prefix, String suffix) throws IOException {
        return java.nio.file.Files.createTempFile(tempDir, prefix, suffix);
    }

    private File createTempFileWithContent(String content, Charset charset) throws IOException {
        File file = createTempFile("test", ".txt");
        Files.asCharSink(file, charset).write(content);
        return file;
    }

    private Path createTempPathWithContent(String content, Charset charset) throws IOException {
        Path path = createTempPath("test", ".txt");
        Files.asCharSink(path, charset).write(content);
        return path;
    }

    private File createTempDir(String prefix) throws IOException {
        return java.nio.file.Files.createTempDirectory(tempDir, prefix).toFile();
    }

    private Path createTempDirPath(String prefix) throws IOException {
        return java.nio.file.Files.createTempDirectory(tempDir, prefix);
    }

    @Test
    public void testNewReader() throws IOException {
        String content = "Hello World!";
        File file = createTempFileWithContent(content, StandardCharsets.UTF_8);

        try (BufferedReader reader = Files.newReader(file, StandardCharsets.UTF_8)) {
            assertEquals(content, reader.readLine());
        }

        File nonExistentFile = new File(tempDir.toFile(), "nonExistent.txt");
        assertThrows(FileNotFoundException.class, () -> Files.newReader(nonExistentFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewWriter() throws IOException {
        File file = createTempFile("writerTest", ".txt");
        String content = "Writing with BufferedWriter.";

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write(content);
        }
        assertEquals(content, IOUtil.readAllToString(file, StandardCharsets.UTF_8));

        // Test on a path that is a directory (should fail to open as a file for writing)
        File dir = createTempDir("writerDirTest");
        assertThrows(FileNotFoundException.class, () -> Files.newWriter(dir, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsByteSourceFile() throws IOException {
        File file = createTempFile("byteSource", ".bin");
        byte[] data = { 1, 2, 3, 4, 5 };
        java.nio.file.Files.write(file.toPath(), data);

        ByteSource byteSource = Files.asByteSource(file);
        assertArrayEquals(data, byteSource.read());
    }

    @Test
    public void testAsByteSourcePath() throws IOException {
        Path path = createTempPath("byteSourcePath", ".bin");
        byte[] data = { 10, 20, 30 };
        java.nio.file.Files.write(path, data);

        ByteSource byteSource = Files.asByteSource(path, StandardOpenOption.READ);
        assertArrayEquals(data, byteSource.read());

        ByteSource byteSourceNoOptions = Files.asByteSource(path); // Default is READ
        assertArrayEquals(data, byteSourceNoOptions.read());
    }

    @Test
    public void testAsByteSinkFile() throws IOException {
        File file = createTempFile("byteSink", ".bin");
        byte[] data = { 5, 6, 7, 8 };

        ByteSink byteSink = Files.asByteSink(file); // Default mode: TRUNCATE
        byteSink.write(data);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));

        byte[] appendData = { 9, 10 };
        ByteSink byteSinkAppend = Files.asByteSink(file, FileWriteMode.APPEND);
        byteSinkAppend.write(appendData);
        byte[] combinedData = { 5, 6, 7, 8, 9, 10 };
        assertArrayEquals(combinedData, java.nio.file.Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testAsByteSinkPath() throws IOException {
        Path path = createTempPath("byteSinkPath", ".bin");
        byte[] data = { 15, 16, 17 };

        // Default options: CREATE, TRUNCATE_EXISTING, WRITE
        ByteSink byteSink = Files.asByteSink(path);
        byteSink.write(data);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(path));

        byte[] appendData = { 18, 19 };
        // Explicit append
        ByteSink byteSinkAppend = Files.asByteSink(path, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        byteSinkAppend.write(appendData);
        byte[] combinedData = { 15, 16, 17, 18, 19 };
        assertArrayEquals(combinedData, java.nio.file.Files.readAllBytes(path));
    }

    @Test
    public void testAsCharSourceFile() throws IOException {
        String content = "CharSource Test Content";
        File file = createTempFileWithContent(content, StandardCharsets.UTF_16);

        CharSource charSource = Files.asCharSource(file, StandardCharsets.UTF_16);
        assertEquals(content, charSource.read());
    }

    @Test
    public void testAsCharSourcePath() throws IOException {
        String content = "CharSource Path Test";
        Path path = createTempPathWithContent(content, StandardCharsets.ISO_8859_1);

        CharSource charSource = Files.asCharSource(path, StandardCharsets.ISO_8859_1, StandardOpenOption.READ);
        assertEquals(content, charSource.read());

        CharSource charSourceNoOptions = Files.asCharSource(path, StandardCharsets.ISO_8859_1); // Default is READ
        assertEquals(content, charSourceNoOptions.read());
    }

    @Test
    public void testAsCharSinkFile() throws IOException {
        File file = createTempFile("charSink", ".txt");
        String content = "Writing CharSink content.";

        CharSink charSink = Files.asCharSink(file, StandardCharsets.UTF_8); // Default mode: TRUNCATE
        charSink.write(content);
        assertEquals(content, IOUtil.readAllToString(file, StandardCharsets.UTF_8));

        String appendContent = " Appending more.";
        CharSink charSinkAppend = Files.asCharSink(file, StandardCharsets.UTF_8, FileWriteMode.APPEND);
        charSinkAppend.write(appendContent);
        assertEquals(content + appendContent, IOUtil.readAllToString(file, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsCharSinkPath() throws IOException {
        Path path = createTempPath("charSinkPath", ".txt");
        String content = "CharSink Path Content.";

        // Default options: CREATE, TRUNCATE_EXISTING, WRITE
        CharSink charSink = Files.asCharSink(path, StandardCharsets.UTF_8);
        charSink.write(content);
        assertEquals(content, IOUtil.readAllToString(path.toFile(), StandardCharsets.UTF_8));

        String appendContent = " Appending path.";
        CharSink charSinkAppend = Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        charSinkAppend.write(appendContent);
        assertEquals(content + appendContent, IOUtil.readAllToString(path.toFile(), StandardCharsets.UTF_8));
    }

    @Test
    public void testToByteArray() throws IOException {
        File file = createTempFile("toByteArray", ".bin");
        byte[] data = "Test Data".getBytes(StandardCharsets.UTF_8);
        java.nio.file.Files.write(file.toPath(), data);
        assertArrayEquals(data, Files.toByteArray(file));

        File emptyFile = createTempFile("emptyByteArray", ".bin");
        assertArrayEquals(new byte[0], Files.toByteArray(emptyFile));

        File nonExistent = new File(tempDir.toFile(), "noExistByteArray.bin");
        assertThrows(FileNotFoundException.class, () -> Files.toByteArray(nonExistent));
    }

    @Test
    public void testWriteByteArrayToFile() throws IOException {
        File file = createTempFile("writeBytes", ".bin");
        byte[] data = { 1, 2, 3, 100, -50 };
        Files.write(data, file);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));

        // Test overwrite
        byte[] newData = { 4, 5, 6 };
        Files.write(newData, file);
        assertArrayEquals(newData, java.nio.file.Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testEqualFile() throws IOException {
        File file1 = createTempFileWithContent("abc", StandardCharsets.UTF_8);
        File file2 = createTempFileWithContent("abc", StandardCharsets.UTF_8);
        File file3 = createTempFileWithContent("def", StandardCharsets.UTF_8);
        File empty1 = createTempFileWithContent("", StandardCharsets.UTF_8);
        File empty2 = createTempFileWithContent("", StandardCharsets.UTF_8);
        File dir1 = createTempDir("dirEq1");

        assertTrue(Files.equal(file1, file2));
        assertFalse(Files.equal(file1, file3));
        assertTrue(Files.equal(empty1, empty2));
        assertFalse(Files.equal(file1, empty1));

        // File vs Dir
        assertThrows(FileNotFoundException.class, () -> Files.equal(file1, dir1)); // Guava throws FNFE if one is not a regular file

        File nonExistent1 = new File(tempDir.toFile(), "nonExistentEq1.txt");
        File nonExistent2 = new File(tempDir.toFile(), "nonExistentEq2.txt");
        // Guava Files.equal throws FNFE if files don't exist
        assertThrows(FileNotFoundException.class, () -> Files.equal(nonExistent1, nonExistent2));
        assertThrows(FileNotFoundException.class, () -> Files.equal(file1, nonExistent1));
    }

    @Test
    public void testEqualPath() throws IOException {
        Path path1 = createTempPathWithContent("xyz", StandardCharsets.UTF_8);
        Path path2 = createTempPathWithContent("xyz", StandardCharsets.UTF_8);
        Path path3 = createTempPathWithContent("123", StandardCharsets.UTF_8);
        Path dirPath = createTempDirPath("dirEqPath");

        assertTrue(Files.equal(path1, path2));
        assertFalse(Files.equal(path1, path3));

        // Path vs Dir
        assertThrows(IOException.class, () -> Files.equal(path1, dirPath)); // MoreFiles.equal checks isRegularFile

        Path nonExistentPath = tempDir.resolve("nonExistentEqPath.txt");
        // MoreFiles.equal throws NoSuchFileException if files don't exist
        assertThrows(NoSuchFileException.class, () -> Files.equal(path1, nonExistentPath));
    }

    @Test
    public void testTouchFile() throws IOException, InterruptedException {
        File file = createTempFile("touch", ".tmp");
        long time1 = file.lastModified();
        assertTrue(time1 > 0);

        Thread.sleep(10); // Ensure time advances for lastModified to change
        Files.touch(file);
        long time2 = file.lastModified();
        assertTrue(time2 >= time1); // Should be strictly > on most systems if sleep worked

        File newFile = new File(tempDir.toFile(), "newTouch.tmp");
        assertFalse(newFile.exists());
        Files.touch(newFile);
        assertTrue(newFile.exists());
        assertEquals(0, newFile.length());
    }

    @Test
    public void testTouchPath() throws IOException, InterruptedException {
        Path path = createTempPath("touchPath", ".tmp");
        long time1 = java.nio.file.Files.getLastModifiedTime(path).toMillis();

        Thread.sleep(10);
        Files.touch(path);
        long time2 = java.nio.file.Files.getLastModifiedTime(path).toMillis();
        assertTrue(time2 >= time1);

        Path newPath = tempDir.resolve("newTouchPath.tmp");
        assertFalse(java.nio.file.Files.exists(newPath));
        Files.touch(newPath);
        assertTrue(java.nio.file.Files.exists(newPath));
        assertEquals(0, java.nio.file.Files.size(newPath));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCreateTempDir() {
        File tempDir = Files.createTempDir();
        assertNotNull(tempDir);
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
        assertTrue(tempDir.getAbsolutePath().startsWith(System.getProperty("java.io.tmpdir")));
        // Attempt to clean up
        assertTrue(tempDir.delete());
    }

    @Test
    public void testCreateParentDirsFile() throws IOException {
        File parent = createTempDir("parentBase");
        File child = new File(parent, "subdir1/subdir2/file.txt");
        assertFalse(child.getParentFile().exists());

        Files.createParentDirs(child);
        assertTrue(child.getParentFile().exists());
        assertTrue(child.getParentFile().isDirectory());

        // Call again, should not fail
        Files.createParentDirs(child);
    }

    @Test
    public void testCreateParentDirectoriesPath() throws IOException {
        Path parent = createTempDirPath("parentBasePath");
        Path childPath = parent.resolve("pathSub1/pathSub2/file.txt");
        assertFalse(java.nio.file.Files.exists(childPath.getParent()));

        Files.createParentDirectories(childPath);
        assertTrue(java.nio.file.Files.exists(childPath.getParent()));
        assertTrue(java.nio.file.Files.isDirectory(childPath.getParent()));

        // With attributes (hard to verify specific attributes without POSIX OS)
        Path childPathAttrs = parent.resolve("pathSubAttrs/file.txt");
        if (System.getProperty("os.name").toLowerCase().contains("win")) {
            Files.createParentDirectories(childPathAttrs); // Windows doesn't use PosixFilePermissions easily for default provider
        } else {
            FileAttribute<?> dirPerms = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));
            Files.createParentDirectories(childPathAttrs, dirPerms);
            assertTrue(java.nio.file.Files.exists(childPathAttrs.getParent()));
            Set<PosixFilePermission> perms = java.nio.file.Files.getPosixFilePermissions(childPathAttrs.getParent());
            assertTrue(perms.contains(PosixFilePermission.OWNER_EXECUTE));
            assertFalse(perms.contains(PosixFilePermission.OTHERS_READ));
        }
    }

    @Test
    public void testCopyFileToOutputStream() throws IOException {
        File fromFile = createTempFile("copyToOS", ".txt");
        String content = "Data to copy to OutputStream.";
        java.nio.file.Files.write(fromFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Files.copy(fromFile, baos);
        assertEquals(content, baos.toString(StandardCharsets.UTF_8.name()));
    }

    @Test
    public void testCopyFileToFile() throws IOException {
        File fromFile = createTempFile("copyFrom", ".txt");
        String content = "Data for file to file copy.";
        java.nio.file.Files.write(fromFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        File toFile = createTempFile("copyTo", ".txt");
        Files.copy(fromFile, toFile);
        assertEquals(content, IOUtil.readAllToString(toFile, StandardCharsets.UTF_8));

        // Test overwrite
        String newContent = "New data overriding.";
        java.nio.file.Files.write(fromFile.toPath(), newContent.getBytes(StandardCharsets.UTF_8));
        Files.copy(fromFile, toFile); // Should overwrite
        assertEquals(newContent, IOUtil.readAllToString(toFile, StandardCharsets.UTF_8));

        // Test copy to self - Guava Files.copy disallows this
        assertThrows(IllegalArgumentException.class, () -> Files.copy(fromFile, fromFile));
    }

    @Test
    public void testMoveFile() throws IOException {
        File fromFile = createTempFile("moveFrom", ".txt");
        String content = "Data to move.";
        java.nio.file.Files.write(fromFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        File toFile = new File(tempDir.toFile(), "moveTo.txt"); // Destination should not exist for simple move
        Files.move(fromFile, toFile);

        assertFalse(fromFile.exists());
        assertTrue(toFile.exists());
        assertEquals(content, IOUtil.readAllToString(toFile, StandardCharsets.UTF_8));

        // Test move to self - Guava Files.move disallows this
        File selfMoveFile = createTempFileWithContent("selfmove", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> Files.move(selfMoveFile, selfMoveFile));
    }

    @Test
    public void testReadLines() throws IOException {
        String line1 = "First line of file.";
        String line2 = "Second line, with Ümlauts.";
        String line3 = "Third line €.";
        String content = line1 + "\n" + line2 + "\r\n" + line3;
        File file = createTempFileWithContent(content, StandardCharsets.UTF_8);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals(line1, lines.get(0));
        assertEquals(line2, lines.get(1));
        assertEquals(line3, lines.get(2));

        File emptyFile = createTempFileWithContent("", StandardCharsets.UTF_8);
        assertTrue(Files.readLines(emptyFile, StandardCharsets.UTF_8).isEmpty());
    }

    @Test
    public void testMapOperations() throws IOException {
        File file = createTempFile("mapTest", ".dat");
        byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);
        java.nio.file.Files.write(file.toPath(), data);

        // Read-only map
        MappedByteBuffer mbbRO = Files.map(file);
        assertEquals(data.length, mbbRO.remaining());
        byte[] readData = new byte[data.length];
        mbbRO.get(readData);
        assertArrayEquals(data, readData);
        GeneratedTestUtil.unmap(mbbRO); // Unmap to release resources)

        // Read-write map
        MappedByteBuffer mbbRW = Files.map(file, MapMode.READ_WRITE);
        assertEquals(data.length, mbbRW.remaining());
        mbbRW.put(0, (byte) 'X');
        mbbRW.force(); // For visibility if we re-read via filesystem
        GeneratedTestUtil.unmap(mbbRW); // Unmap to release resources)

        byte[] changedData = java.nio.file.Files.readAllBytes(file.toPath());
        assertEquals((byte) 'X', changedData[0]);

        // Map with specific size (can extend or truncate for RW, create for RW)
        File sizedMapFile = new File(tempDir.toFile(), "sizedMap.dat");
        long mapSize = 20;
        MappedByteBuffer mbbSized = Files.map(sizedMapFile, MapMode.READ_WRITE, mapSize);
        assertTrue(sizedMapFile.exists());
        assertEquals(mapSize, sizedMapFile.length());
        assertEquals(mapSize, mbbSized.capacity());
        mbbSized.put((byte) 'A').put((int) (mapSize - 1), (byte) 'Z'); // MappedByteBuffer positions might be tricky

        File nonExistentMapRO = new File(tempDir.toFile(), "nonExistentMapRO.dat");
        assertThrows(FileNotFoundException.class, () -> Files.map(nonExistentMapRO));
        assertThrows(FileNotFoundException.class, () -> Files.map(nonExistentMapRO, MapMode.READ_ONLY));

        GeneratedTestUtil.unmap(mbbSized); // Unmap to release resources)

        Files.deleteRecursively(tempDir, RecursiveDeleteOption.ALLOW_INSECURE); // Cleanup temp dir after tests);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", Files.simplifyPath(""));
        assertEquals("a/b", Files.simplifyPath("a/./b/../b"));
        assertEquals("/c", Files.simplifyPath("/a/b/../../c"));
    }

    @Test
    public void testGetNameWithoutExtensionString() {
        assertEquals("file", Files.getNameWithoutExtension("file.txt"));
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("nodot", Files.getNameWithoutExtension("nodot"));
        assertEquals("file", Files.getNameWithoutExtension("path/to/file.ext"));
    }

    @Test
    public void testGetNameWithoutExtensionPath() throws IOException {
        Path p1 = Paths.get("some/file.tar.gz");
        assertEquals("file.tar", Files.getNameWithoutExtension(p1));
        Path p2 = Paths.get("image.jpeg");
        assertEquals("image", Files.getNameWithoutExtension(p2));
        Path p3 = Paths.get("nodots");
        assertEquals("nodots", Files.getNameWithoutExtension(p3));
    }

    @Test
    public void testGetFileExtensionString() {
        assertEquals("txt", Files.getFileExtension("file.txt"));
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("", Files.getFileExtension("nodots"));
        assertEquals("", Files.getFileExtension("file.")); // Guava behavior
    }

    @Test
    public void testGetFileExtensionPath() throws IOException {
        Path p1 = Paths.get("some/file.tar.gz");
        assertEquals("gz", Files.getFileExtension(p1));
        Path p2 = Paths.get("image.jpeg");
        assertEquals("jpeg", Files.getFileExtension(p2));
        Path p3 = Paths.get("nodots");
        assertEquals("", Files.getFileExtension(p3));
    }

    @Test
    public void testFileTraverser() throws IOException {
        File base = createTempDir("traverseBase");
        File f1 = new File(base, "f1.txt");
        Files.touch(f1);
        File d1 = new File(base, "d1");
        d1.mkdir();
        File f2 = new File(d1, "f2.txt");
        Files.touch(f2);

        Traverser<File> traverser = Files.fileTraverser();
        List<String> visitedNames = new ArrayList<>();
        // Sort for predictable order in assertion
        List<File> traversedFiles = StreamSupport.stream(traverser.depthFirstPreOrder(base).spliterator(), false)
                .sorted(Comparator.comparing(File::getAbsolutePath))
                .collect(Collectors.toList());

        for (File f : traversedFiles) {
            visitedNames.add(f.getName());
        }

        List<String> expectedNames = Arrays.asList(base.getName(), "d1", "f1.txt", "f2.txt").stream().sorted().collect(Collectors.toList());
        // The order from traverser might not be lexicographical always, adjust assertion
        // For depthFirstPreOrder: base, d1, f2, f1 (if d1 processed before f1 alphabetically)
        // The exact order depends on listFiles() order which is platform dependent.
        // So, checking for presence and count is more robust.

        assertTrue(N.isEqualCollection(expectedNames, visitedNames));
        assertEquals(4, visitedNames.size());
        assertTrue(visitedNames.contains(base.getName()));
        assertTrue(visitedNames.contains("d1"));
        assertTrue(visitedNames.contains("f1.txt"));
        assertTrue(visitedNames.contains("f2.txt"));

        // Test traversing a single file
        Iterable<File> singleFileIterable = traverser.depthFirstPreOrder(f1);
        Iterator<File> iterator = singleFileIterable.iterator();
        assertEquals(f1, iterator.next());
        assertFalse(iterator.hasNext()); // Should only contain itself
    }

    @Test
    public void testPathTraverser() throws IOException {
        Path basePath = createTempDirPath("traversePathBase");
        Path pf1 = java.nio.file.Files.createFile(basePath.resolve("pf1.txt"));
        Path pd1 = java.nio.file.Files.createDirectory(basePath.resolve("pd1"));
        Path pf2 = java.nio.file.Files.createFile(pd1.resolve("pf2.txt"));

        Traverser<Path> traverser = Files.pathTraverser();
        List<Path> traversedPaths = StreamSupport.stream(traverser.depthFirstPreOrder(basePath).spliterator(), false).sorted().collect(Collectors.toList());

        assertEquals(4, traversedPaths.size());
        assertTrue(traversedPaths.contains(basePath));
        assertTrue(traversedPaths.contains(pd1));
        assertTrue(traversedPaths.contains(pf1));
        assertTrue(traversedPaths.contains(pf2));
    }

    @Test
    public void testListFilesPath() throws IOException {
        Path dir = createTempDirPath("listPathDir");
        Path f1 = java.nio.file.Files.createFile(dir.resolve("file1.tmp"));
        Path d1 = java.nio.file.Files.createDirectory(dir.resolve("subdir.tmp"));

        ImmutableList<Path> listed = Files.listFiles(dir);
        assertEquals(2, listed.size());
        assertTrue(listed.contains(f1));
        assertTrue(listed.contains(d1));

        // Test empty dir
        Path emptyDir = createTempDirPath("emptyListPathDir");
        assertTrue(Files.listFiles(emptyDir).isEmpty());

        // Test on a file
        assertThrows(NotDirectoryException.class, () -> Files.listFiles(f1));

        // Test on non-existent path
        Path nonExistent = dir.resolve("noSuchDir");
        assertThrows(NoSuchFileException.class, () -> Files.listFiles(nonExistent));
    }

    @Test
    public void testDeleteRecursivelyPath() throws IOException {
        Path base = createTempDirPath("delRecBase");
        java.nio.file.Files.createFile(base.resolve("f1.txt"));
        Path d1 = java.nio.file.Files.createDirectory(base.resolve("d1"));
        java.nio.file.Files.createFile(d1.resolve("f2.txt"));

        Files.deleteRecursively(base, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(base));

        // Test on non-existent path
        Path nonExistent = tempDir.resolve("delRecNonExistent");
        assertThrows(NoSuchFileException.class, () -> Files.deleteRecursively(nonExistent, RecursiveDeleteOption.ALLOW_INSECURE));

        // Test deleting a single file
        Path singleFile = createTempPath("delRecSingle", ".txt");
        Files.deleteRecursively(singleFile, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(singleFile));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS) // SecureDirectoryStream and InsecureRecursiveDeleteException are more relevant on POSIX
    public void testDeleteRecursively_insecure() throws IOException {
        // This test is hard to make truly insecure in a unit test.
        // Guava's MoreFiles.deleteRecursively throws InsecureRecursiveDeleteException
        // if it cannot use SecureDirectoryStream (e.g. older Java or specific FS).
        // We assume default options (no ALLOW_INSECURE).
        // If the test environment *is* secure, it won't throw InsecureRecursiveDeleteException.
        // This test is more about the ALLOW_INSECURE option if the condition arises.

        Path base = createTempDirPath("delRecInsecureBase");
        java.nio.file.Files.createFile(base.resolve("f1.txt"));

        // If the underlying com.google.common.io.MoreFiles.deleteRecursively
        // determines the environment is insecure and no ALLOW_INSECURE is passed,
        // it would throw InsecureRecursiveDeleteException.
        // Otherwise, it will just delete.
        try {
            Files.deleteRecursively(base); // No options
            assertFalse(java.nio.file.Files.exists(base));
        } catch (InsecureRecursiveDeleteException e) {
            // This is the path we'd ideally hit if we could simulate insecurity.
            // Since we can't easily, we re-create and delete with ALLOW_INSECURE
            java.nio.file.Files.createDirectories(base); // Recreate
            java.nio.file.Files.createFile(base.resolve("f1.txt"));
            Files.deleteRecursively(base, RecursiveDeleteOption.ALLOW_INSECURE);
            assertFalse(java.nio.file.Files.exists(base));
        }
    }

    @Test
    public void testDeleteDirectoryContentsPath() throws IOException {
        Path base = createTempDirPath("delContBase");
        java.nio.file.Files.createFile(base.resolve("f1.txt"));
        Path d1 = java.nio.file.Files.createDirectory(base.resolve("d1"));
        java.nio.file.Files.createFile(d1.resolve("f2.txt"));

        Files.deleteDirectoryContents(base, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(java.nio.file.Files.exists(base)); // Base directory itself remains
        assertEquals(0, java.nio.file.Files.list(base).count()); // Should be empty

        // Test on empty directory
        Path emptyDir = createTempDirPath("delContEmpty");
        Files.deleteDirectoryContents(emptyDir, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(java.nio.file.Files.exists(emptyDir));

        // Test on non-existent path
        Path nonExistent = tempDir.resolve("delContNonExistent");
        assertThrows(NoSuchFileException.class, () -> Files.deleteDirectoryContents(nonExistent));

        // Test on a file
        Path fileAsDir = createTempPath("delContFileAsDir", ".txt");
        assertThrows(NotDirectoryException.class, () -> Files.deleteDirectoryContents(fileAsDir));
    }

    @Test
    public void testIsDirectoryPredicate() throws IOException {
        Path dir = createTempDirPath("isDirPred");
        Path file = createTempPath("isFilePred", ".txt");

        Predicate<Path> isDir = Files.isDirectory();
        assertTrue(isDir.apply(dir));
        assertFalse(isDir.apply(file));

        // Test with NO_FOLLOW_LINKS (assuming no symlinks for basic test)
        Predicate<Path> isDirNoFollow = Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isDirNoFollow.apply(dir));

        // Test on non-existent path
        Path nonExistent = dir.resolve("no_such_thing");
        assertFalse(isDir.apply(nonExistent)); // Files.isDirectory(nonExistent) is false
    }

    @Test
    public void testIsRegularFilePredicate() throws IOException {
        Path dir = createTempDirPath("isRegFilePredDir");
        Path file = createTempPath("isRegFilePredFile", ".txt");

        Predicate<Path> isReg = Files.isRegularFile();
        assertTrue(isReg.apply(file));
        assertFalse(isReg.apply(dir));

        Predicate<Path> isRegNoFollow = Files.isRegularFile(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isRegNoFollow.apply(file));

        Path nonExistent = dir.resolve("no_such_file.txt");
        assertFalse(isReg.apply(nonExistent)); // Files.isRegularFile(nonExistent) is false
    }

}
