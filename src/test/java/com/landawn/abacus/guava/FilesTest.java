package com.landawn.abacus.guava;

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
import java.nio.charset.MalformedInputException;
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
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Predicate;
import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.InsecureRecursiveDeleteException;
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
        Files.writeString(testFile, "Line 1\nLine 2\nLine 3");
    }

    private File file(String name) {
        return tempDir.resolve(name).toFile();
    }

    private Path path(String name) {
        return tempDir.resolve(name);
    }

    private File writeRawBytes(String name, byte[] bytes) throws IOException {
        File f = file(name);
        java.nio.file.Files.write(f.toPath(), bytes);
        return f;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    @Test
    public void testNewReader() throws IOException {
        try (BufferedReader reader = Files.newReader(testFile, StandardCharsets.UTF_8)) {
            assertEquals("Line 1", reader.readLine());
            assertEquals("Line 2", reader.readLine());
            assertEquals("Line 3", reader.readLine());
            assertEquals(null, reader.readLine());
        }

        File empty = file("empty.txt");
        Files.write(new byte[0], empty);
        try (BufferedReader reader = Files.newReader(empty, StandardCharsets.UTF_8)) {
            assertEquals(null, reader.readLine());
        }

        File latin = writeRawBytes("latin.txt", "café".getBytes(StandardCharsets.ISO_8859_1));
        try (BufferedReader reader = Files.newReader(latin, StandardCharsets.ISO_8859_1)) {
            assertEquals("café", reader.readLine());
        }

        assertThrows(FileNotFoundException.class, () -> Files.newReader(file("missing.txt"), StandardCharsets.UTF_8));
        File dir = path("reader-dir").toFile();
        assertTrue(dir.mkdir());
        assertThrows(FileNotFoundException.class, () -> Files.newReader(dir, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewWriter() throws IOException {
        File file = file("writer.txt");
        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Hello");
        }
        assertEquals("Hello", Files.readString(file));

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Replaced");
        }
        assertEquals("Replaced", Files.readString(file));

        File latin = file("writer-latin.txt");
        try (BufferedWriter writer = Files.newWriter(latin, StandardCharsets.ISO_8859_1)) {
            writer.write("café");
        }
        assertEquals("café", Files.readString(latin, StandardCharsets.ISO_8859_1));

        File dir = path("writer-dir").toFile();
        assertTrue(dir.mkdir());
        assertThrows(FileNotFoundException.class, () -> Files.newWriter(dir, StandardCharsets.UTF_8));
    }

    @Test
    public void testAsByteSource() throws IOException {
        ByteSource fromFile = Files.asByteSource(testFile);
        ByteSource fromPath = Files.asByteSource(testPath, StandardOpenOption.READ);
        ByteSource fromPathNoOptions = Files.asByteSource(testPath);

        byte[] expected = java.nio.file.Files.readAllBytes(testPath);
        assertArrayEquals(expected, fromFile.read());
        assertArrayEquals(expected, fromPath.read());
        assertArrayEquals(expected, fromPathNoOptions.read());
        assertEquals(expected.length, fromFile.size());

        File empty = file("empty.bin");
        Files.write(new byte[0], empty);
        assertEquals(0, Files.asByteSource(empty).size());
        assertArrayEquals(new byte[0], Files.asByteSource(empty).read());
    }

    @Test
    public void testAsByteSink() throws IOException {
        File file = file("bytes.bin");
        Path path = path("bytes-path.bin");
        byte[] data = { 1, 2, 3, 4 };

        Files.asByteSink(file).write(data);
        Files.asByteSink(path).write(data);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(path));

        Files.asByteSink(file, FileWriteMode.APPEND).write(new byte[] { 5 });
        Files.asByteSink(path, StandardOpenOption.APPEND, StandardOpenOption.WRITE).write(new byte[] { 5 });
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, java.nio.file.Files.readAllBytes(file.toPath()));
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, java.nio.file.Files.readAllBytes(path));

        Files.asByteSink(file).write(new byte[] { 9 });
        assertArrayEquals(new byte[] { 9 }, java.nio.file.Files.readAllBytes(file.toPath()));
    }

    @Test
    public void testAsCharSource() throws IOException {
        CharSource fromFile = Files.asCharSource(testFile, StandardCharsets.UTF_8);
        CharSource fromPath = Files.asCharSource(testPath, StandardCharsets.UTF_8, StandardOpenOption.READ);
        CharSource fromPathNoOptions = Files.asCharSource(testPath, StandardCharsets.UTF_8);

        assertEquals("Line 1\nLine 2\nLine 3", fromFile.read());
        assertEquals(fromFile.read(), fromPath.read());
        assertEquals(fromFile.read(), fromPathNoOptions.read());
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), fromFile.readLines());

        File latin = writeRawBytes("chars-latin.txt", "áéíóú".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("áéíóú", Files.asCharSource(latin, StandardCharsets.ISO_8859_1).read());

        File empty = file("empty-chars.txt");
        Files.write(new byte[0], empty);
        assertEquals("", Files.asCharSource(empty, StandardCharsets.UTF_8).read());
    }

    @Test
    public void testAsCharSink() throws IOException {
        File file = file("chars.txt");
        Path path = path("chars-path.txt");

        Files.asCharSink(file, StandardCharsets.UTF_8).write("Hello");
        Files.asCharSink(path, StandardCharsets.UTF_8).write("Hello");
        assertEquals("Hello", Files.readString(file));
        assertEquals("Hello", new String(java.nio.file.Files.readAllBytes(path), StandardCharsets.UTF_8));

        Files.asCharSink(file, StandardCharsets.UTF_8, FileWriteMode.APPEND).write(" World");
        Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.APPEND, StandardOpenOption.WRITE).write(" World");
        assertEquals("Hello World", Files.readString(file));
        assertEquals("Hello World", new String(java.nio.file.Files.readAllBytes(path), StandardCharsets.UTF_8));

        Files.asCharSink(file, StandardCharsets.UTF_8).writeLines(Arrays.asList("A", "B"));
        String lines = Files.readString(file);
        assertTrue(lines.contains("A"));
        assertTrue(lines.contains("B"));
    }

    @Test
    public void testToByteArray() throws IOException {
        byte[] data = "Byte array content".getBytes(StandardCharsets.UTF_8);
        File file = file("bytes.txt");
        Files.write(data, file);
        assertArrayEquals(data, Files.toByteArray(file));

        File empty = file("empty-bytes.txt");
        Files.write(new byte[0], empty);
        assertArrayEquals(new byte[0], Files.toByteArray(empty));
        assertThrows(FileNotFoundException.class, () -> Files.toByteArray(file("missing.bin")));
    }

    @Test
    public void testWrite() throws IOException {
        File file = file("write.bin");
        byte[] data = { 1, 2, 3, 100, -50 };
        Files.write(data, file);
        assertArrayEquals(data, java.nio.file.Files.readAllBytes(file.toPath()));

        Files.write(new byte[] { 4, 5, 6 }, file);
        assertArrayEquals(new byte[] { 4, 5, 6 }, java.nio.file.Files.readAllBytes(file.toPath()));

        Files.write(new byte[0], file);
        assertEquals(0, file.length());

        File nested = new File(tempDir.toFile(), "missing-parent/file.bin");
        assertThrows(FileNotFoundException.class, () -> Files.write("x".getBytes(), nested));
    }

    @Test
    public void testEqual() throws IOException {
        File a = file("eq-a.txt");
        File b = file("eq-b.txt");
        File c = file("eq-c.txt");
        Files.writeString(a, "abc");
        Files.writeString(b, "abc");
        Files.writeString(c, "def");
        File empty1 = file("eq-empty-1.txt");
        File empty2 = file("eq-empty-2.txt");
        Files.write(new byte[0], empty1);
        Files.write(new byte[0], empty2);

        assertTrue(Files.equal(a, b));
        assertFalse(Files.equal(a, c));
        assertTrue(Files.equal(empty1, empty2));
        assertFalse(Files.equal(a, empty1));
        assertTrue(Files.equal(a, a));

        Path pa = path("eq-pa.txt");
        Path pb = path("eq-pb.txt");
        Path pc = path("eq-pc.txt");
        java.nio.file.Files.write(pa, "xyz".getBytes(StandardCharsets.UTF_8));
        java.nio.file.Files.write(pb, "xyz".getBytes(StandardCharsets.UTF_8));
        java.nio.file.Files.write(pc, "123".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.equal(pa, pb));
        assertFalse(Files.equal(pa, pc));

        File dir = path("eq-dir").toFile();
        assertTrue(dir.mkdir());
        assertThrows(FileNotFoundException.class, () -> Files.equal(a, dir));
        assertThrows(NoSuchFileException.class, () -> Files.equal(pa, path("missing-eq.txt")));
    }

    @Test
    public void testTouch() throws IOException, InterruptedException {
        File created = file("touch-new.txt");
        Path createdPath = path("touch-new-path.txt");
        assertFalse(created.exists());
        Files.touch(created);
        Files.touch(createdPath);
        assertTrue(created.exists());
        assertTrue(java.nio.file.Files.exists(createdPath));
        assertEquals(0, created.length());

        Files.writeString(created, "keep");
        long before = created.lastModified();
        Thread.sleep(10);
        Files.touch(created);
        Files.touch(created.toPath());
        assertTrue(created.lastModified() >= before);
        assertEquals("keep", Files.readString(created));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testCreateTempDir() throws IOException {
        File dir = Files.createTempDir();
        assertTrue(dir.isDirectory());
        assertTrue(dir.getAbsolutePath().startsWith(System.getProperty("java.io.tmpdir")));
        File nested = new File(dir, "child.txt");
        Files.touch(nested);
        assertTrue(nested.exists());
        assertTrue(nested.delete());
        assertTrue(dir.delete());
    }

    @Test
    public void testCreateParentDirs() throws IOException {
        File nested = new File(tempDir.toFile(), "a/b/c/file.txt");
        assertFalse(nested.getParentFile().exists());
        Files.createParentDirs(nested);
        assertTrue(nested.getParentFile().isDirectory());
        Files.createParentDirs(nested);

        Path nestedPath = tempDir.resolve("p/q/r/file.txt");
        Files.createParentDirectories(nestedPath);
        assertTrue(java.nio.file.Files.isDirectory(nestedPath.getParent()));

        File noParent = new File("orphan.txt");
        Files.createParentDirs(noParent);
    }

    @Test
    public void testCreateParentDirectories_PosixAttributes() throws IOException {
        Assumptions.assumeFalse(isWindows());
        Path child = tempDir.resolve("posix/file.txt");
        FileAttribute<?> dirPerms = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwxr-x---"));
        Files.createParentDirectories(child, dirPerms);
        Set<PosixFilePermission> perms = java.nio.file.Files.getPosixFilePermissions(child.getParent());
        assertTrue(perms.contains(PosixFilePermission.OWNER_EXECUTE));
        assertFalse(perms.contains(PosixFilePermission.OTHERS_READ));
    }

    @Test
    public void testCopy() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Files.copy(testFile, out);
        assertEquals("Line 1\nLine 2\nLine 3", out.toString(StandardCharsets.UTF_8));

        File dest = file("copy-dest.txt");
        Files.copy(testFile, dest);
        assertTrue(Files.equal(testFile, dest));
        Files.writeString(testFile, "overwritten-source");
        Files.copy(testFile, dest);
        assertEquals("overwritten-source", Files.readString(dest));

        File empty = file("copy-empty.txt");
        Files.write(new byte[0], empty);
        ByteArrayOutputStream emptyOut = new ByteArrayOutputStream();
        Files.copy(empty, emptyOut);
        assertEquals(0, emptyOut.size());

        assertThrows(IllegalArgumentException.class, () -> Files.copy(testFile, testFile));
        File missingParent = new File(tempDir.toFile(), "no-such-dir/dest.txt");
        assertThrows(FileNotFoundException.class, () -> Files.copy(testFile, missingParent));
    }

    @Test
    public void testMove() throws IOException {
        File source = file("move-src.txt");
        File dest = file("move-dest.txt");
        Files.writeString(source, "relocated");
        Files.move(source, dest);
        assertFalse(source.exists());
        assertEquals("relocated", Files.readString(dest));

        File renamed = file("moved-name.txt");
        Files.move(dest, renamed);
        assertEquals("relocated", Files.readString(renamed));
        assertThrows(IllegalArgumentException.class, () -> Files.move(renamed, renamed));
    }

    @Test
    public void testReadLines() throws IOException {
        List<String> lines = Files.readLines(testFile, StandardCharsets.UTF_8);
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), lines);
        lines.add("Line 4");
        assertEquals(4, lines.size());

        File empty = file("read-empty.txt");
        Files.write(new byte[0], empty);
        assertTrue(Files.readLines(empty, StandardCharsets.UTF_8).isEmpty());

        File latin = writeRawBytes("read-latin.txt", "Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals(Arrays.asList("Línea 1", "Línea 2"), Files.readLines(latin, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testMap() throws IOException {
        File file = file("map.dat");
        byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);
        Files.write(data, file);

        MappedByteBuffer readOnly = Files.map(file);
        byte[] read = new byte[data.length];
        readOnly.get(read);
        assertArrayEquals(data, read);
        unmap(readOnly);

        MappedByteBuffer readWrite = Files.map(file, MapMode.READ_WRITE);
        readWrite.put(0, (byte) 'X');
        readWrite.force();
        unmap(readWrite);
        assertEquals((byte) 'X', java.nio.file.Files.readAllBytes(file.toPath())[0]);

        File sized = file("map-sized.dat");
        MappedByteBuffer sizedBuffer = Files.map(sized, MapMode.READ_WRITE, 20);
        assertEquals(20, sized.length());
        assertEquals(20, sizedBuffer.capacity());
        unmap(sizedBuffer);

        assertThrows(FileNotFoundException.class, () -> Files.map(file("missing-map.dat")));
        assertThrows(IllegalArgumentException.class, () -> Files.map(file, MapMode.READ_WRITE, -1));
        assertThrows(IllegalArgumentException.class, () -> Files.map(file, MapMode.READ_ONLY, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(file, null));
    }

    @Test
    public void testMap_MissingFile() throws IOException {
        File missingRw = file("missing-rw.bin");
        MappedByteBuffer rw = Files.map(missingRw, MapMode.READ_WRITE);
        assertEquals(0, rw.capacity());
        assertTrue(missingRw.exists());

        File missingPrivate = file("missing-private.bin");
        MappedByteBuffer priv = Files.map(missingPrivate, MapMode.PRIVATE);
        assertEquals(0, priv.capacity());
        assertTrue(missingPrivate.exists());

        File missingRo = file("missing-ro.bin");
        assertThrows(FileNotFoundException.class, () -> Files.map(missingRo, MapMode.READ_ONLY));
        assertThrows(FileNotFoundException.class, () -> Files.map(missingRo, MapMode.READ_ONLY, 10));
        assertFalse(missingRo.exists());
    }

    @Test
    public void testMap_SizeBeyondLength() throws IOException {
        File file = file("extend.bin");
        Files.write(new byte[] { 1, 2, 3 }, file);
        long len = file.length();
        IOException ex = assertThrows(IOException.class, () -> Files.map(file, MapMode.READ_ONLY, len + 1));
        assertFalse(ex instanceof FileNotFoundException);
        assertEquals(len, file.length());

        MappedByteBuffer buffer = Files.map(file, MapMode.READ_WRITE, 10);
        assertEquals(10, buffer.capacity());
        assertEquals(10L, file.length());
        unmap(buffer);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", Files.simplifyPath(""));
        assertEquals(".", Files.simplifyPath("."));
        assertEquals(".", Files.simplifyPath("./"));
        assertEquals("b", Files.simplifyPath("a/../b"));
        assertEquals("/b", Files.simplifyPath("/a/../b"));
        assertEquals("/", Files.simplifyPath("/"));
        assertEquals("/", Files.simplifyPath("///"));
        assertEquals("a/b", Files.simplifyPath("a//b"));
        assertEquals("a/b", Files.simplifyPath("a/./b/../b"));
        assertEquals("/c", Files.simplifyPath("/a/b/../../c"));
        assertEquals("../b/c", Files.simplifyPath("a/../../b/c"));
    }

    @Test
    public void testGetNameWithoutExtension() {
        assertEquals("file", Files.getNameWithoutExtension("file.txt"));
        assertEquals("file", Files.getNameWithoutExtension("/path/to/file.txt"));
        assertEquals("archive.tar", Files.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("file", Files.getNameWithoutExtension("file"));
        assertEquals("", Files.getNameWithoutExtension(".hidden"));
        assertEquals("file", Files.getNameWithoutExtension(Paths.get("file.txt")));
        assertEquals("file.tar", Files.getNameWithoutExtension(Paths.get("some/file.tar.gz")));
        assertEquals("nodots", Files.getNameWithoutExtension(Paths.get("nodots")));
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("txt", Files.getFileExtension("file.txt"));
        assertEquals("txt", Files.getFileExtension("/path/to/file.txt"));
        assertEquals("gz", Files.getFileExtension("archive.tar.gz"));
        assertEquals("", Files.getFileExtension("file"));
        assertEquals("hidden", Files.getFileExtension(".hidden"));
        assertEquals("", Files.getFileExtension("file."));
        assertEquals("gz", Files.getFileExtension(Paths.get("some/file.tar.gz")));
        assertEquals("jpeg", Files.getFileExtension(Paths.get("image.jpeg")));
        assertEquals("", Files.getFileExtension(Paths.get("nodots")));
    }

    @Test
    public void testFileTraverser() throws IOException {
        File base = path("traverse").toFile();
        assertTrue(base.mkdir());
        File f1 = new File(base, "f1.txt");
        File d1 = new File(base, "d1");
        File f2 = new File(d1, "f2.txt");
        Files.touch(f1);
        assertTrue(d1.mkdir());
        Files.touch(f2);

        Traverser<File> traverser = Files.fileTraverser();
        List<File> breadth = traverser.breadthFirst(base).toList();
        List<File> depth = traverser.depthFirstPreOrder(base).toList();
        assertEquals(4, breadth.size());
        assertEquals(4, depth.size());
        assertTrue(breadth.contains(base));
        assertTrue(breadth.contains(f1));
        assertTrue(breadth.contains(d1));
        assertTrue(breadth.contains(f2));
        assertEquals(List.of(f1), traverser.breadthFirst(f1).toList());
    }

    @Test
    public void testPathTraverser() throws IOException {
        Path base = path("traverse-path");
        java.nio.file.Files.createDirectory(base);
        Path f1 = java.nio.file.Files.createFile(base.resolve("pf1.txt"));
        Path d1 = java.nio.file.Files.createDirectory(base.resolve("pd1"));
        Path f2 = java.nio.file.Files.createFile(d1.resolve("pf2.txt"));

        Traverser<Path> traverser = Files.pathTraverser();
        List<Path> visited = traverser.breadthFirst(base).toList();
        assertEquals(4, visited.size());
        assertTrue(visited.contains(base));
        assertTrue(visited.contains(f1));
        assertTrue(visited.contains(d1));
        assertTrue(visited.contains(f2));
        assertEquals(1, traverser.depthFirstPreOrder(f1).toList().size());
    }

    @Test
    public void testListFiles() throws IOException {
        Path dir = path("list");
        java.nio.file.Files.createDirectory(dir);
        Path f1 = java.nio.file.Files.createFile(dir.resolve("file1.tmp"));
        Path d1 = java.nio.file.Files.createDirectory(dir.resolve("subdir.tmp"));

        ImmutableList<Path> listed = Files.listFiles(dir);
        assertEquals(2, listed.size());
        assertTrue(listed.contains(f1));
        assertTrue(listed.contains(d1));
        assertTrue(Files.listFiles(java.nio.file.Files.createDirectory(path("list-empty"))).isEmpty());
        assertThrows(NotDirectoryException.class, () -> Files.listFiles(f1));
        assertThrows(NoSuchFileException.class, () -> Files.listFiles(dir.resolve("missing")));
    }

    @Test
    public void testDeleteRecursively() throws IOException {
        Path base = path("del-rec");
        java.nio.file.Files.createDirectory(base);
        java.nio.file.Files.createFile(base.resolve("f1.txt"));
        Path d1 = java.nio.file.Files.createDirectory(base.resolve("d1"));
        java.nio.file.Files.createFile(d1.resolve("f2.txt"));

        Files.deleteRecursively(base, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(base));

        Path empty = java.nio.file.Files.createDirectory(path("del-empty"));
        Files.deleteRecursively(empty, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(empty));

        Path single = java.nio.file.Files.createFile(path("del-single.txt"));
        Files.deleteRecursively(single, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(single));

        assertThrows(NoSuchFileException.class, () -> Files.deleteRecursively(path("del-missing"), RecursiveDeleteOption.ALLOW_INSECURE));
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    public void testDeleteRecursively_insecure() throws IOException {
        Path base = path("del-insecure");
        java.nio.file.Files.createDirectory(base);
        java.nio.file.Files.createFile(base.resolve("f1.txt"));
        try {
            Files.deleteRecursively(base);
            assertFalse(java.nio.file.Files.exists(base));
        } catch (InsecureRecursiveDeleteException e) {
            Files.deleteRecursively(base, RecursiveDeleteOption.ALLOW_INSECURE);
            assertFalse(java.nio.file.Files.exists(base));
        }
    }

    @Test
    public void testDeleteRecursively_NoOption_OnWindows() throws IOException {
        Assumptions.assumeTrue(isWindows());
        Path missing = path("does-not-exist");
        assertThrows(InsecureRecursiveDeleteException.class, () -> Files.deleteRecursively(missing));

        Path dir = java.nio.file.Files.createDirectory(path("dir-no-option"));
        java.nio.file.Files.write(dir.resolve("child.txt"), new byte[] { 1 });
        assertThrows(InsecureRecursiveDeleteException.class, () -> Files.deleteRecursively(testPath));
        assertThrows(InsecureRecursiveDeleteException.class, () -> Files.deleteRecursively(dir));
        assertThrows(InsecureRecursiveDeleteException.class, () -> Files.deleteDirectoryContents(dir));
        assertTrue(java.nio.file.Files.exists(testPath));
        assertTrue(java.nio.file.Files.exists(dir.resolve("child.txt")));
    }

    @Test
    public void testDeleteDirectoryContents() throws IOException {
        Path base = java.nio.file.Files.createDirectory(path("del-contents"));
        java.nio.file.Files.createFile(base.resolve("f1.txt"));
        Path d1 = java.nio.file.Files.createDirectory(base.resolve("d1"));
        java.nio.file.Files.createFile(d1.resolve("f2.txt"));

        Files.deleteDirectoryContents(base, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(java.nio.file.Files.exists(base));
        try (var stream = java.nio.file.Files.list(base)) {
            assertEquals(0, stream.count());
        }

        Path empty = java.nio.file.Files.createDirectory(path("del-contents-empty"));
        Files.deleteDirectoryContents(empty, RecursiveDeleteOption.ALLOW_INSECURE);
        assertTrue(java.nio.file.Files.exists(empty));

        assertThrows(NoSuchFileException.class, () -> Files.deleteDirectoryContents(path("del-contents-missing")));
        assertThrows(NoSuchFileException.class, () -> Files.deleteDirectoryContents(path("del-contents-missing-2"), RecursiveDeleteOption.ALLOW_INSECURE));
        assertThrows(NotDirectoryException.class, () -> Files.deleteDirectoryContents(testPath, RecursiveDeleteOption.ALLOW_INSECURE));
        assertTrue(java.nio.file.Files.exists(testPath));
    }

    @Test
    public void testIsDirectory() throws IOException {
        Path dir = java.nio.file.Files.createDirectory(path("is-dir"));
        Predicate<Path> isDir = Files.isDirectory();
        Predicate<Path> noFollow = Files.isDirectory(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isDir.apply(dir));
        assertTrue(noFollow.apply(dir));
        assertFalse(isDir.apply(testPath));
        assertFalse(isDir.apply(path("missing-dir")));
    }

    @Test
    public void testIsRegularFile() throws IOException {
        Path dir = java.nio.file.Files.createDirectory(path("is-file-dir"));
        Predicate<Path> isReg = Files.isRegularFile();
        Predicate<Path> noFollow = Files.isRegularFile(LinkOption.NOFOLLOW_LINKS);
        assertTrue(isReg.apply(testPath));
        assertTrue(noFollow.apply(testPath));
        assertFalse(isReg.apply(dir));
        assertFalse(isReg.apply(path("missing-file.txt")));
    }

    @Test
    public void testReadAllBytes() throws IOException {
        byte[] data = "hello".getBytes(StandardCharsets.UTF_8);
        File file = file("all-bytes.bin");
        Files.write(data, file);
        assertArrayEquals(data, Files.readAllBytes(file));

        File empty = file("all-bytes-empty.bin");
        Files.write(new byte[0], empty);
        assertArrayEquals(new byte[0], Files.readAllBytes(empty));
    }

    @Test
    public void testReadString() throws IOException {
        assertEquals("Line 1\nLine 2\nLine 3", Files.readString(testFile));
        assertEquals("Line 1\nLine 2\nLine 3", Files.readString(testFile, StandardCharsets.UTF_8));

        File empty = file("read-string-empty.txt");
        Files.write(new byte[0], empty);
        assertEquals("", Files.readString(empty));

        File latin = writeRawBytes("read-string-latin.txt", "Línea".getBytes(StandardCharsets.ISO_8859_1));
        assertEquals("Línea", Files.readString(latin, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testReadAllLines() throws IOException {
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), Files.readAllLines(testFile));
        assertEquals(Arrays.asList("Line 1", "Line 2", "Line 3"), Files.readAllLines(testFile, StandardCharsets.UTF_8));

        File empty = file("all-lines-empty.txt");
        Files.write(new byte[0], empty);
        assertTrue(Files.readAllLines(empty).isEmpty());

        File blank = writeRawBytes("blank-lines.txt", "A\n\nC".getBytes(StandardCharsets.UTF_8));
        assertEquals(Arrays.asList("A", "", "C"), Files.readAllLines(blank));
    }

    @Test
    public void testWriteString() throws IOException {
        File file = file("write-string.txt");
        Files.writeString(file, "smile 😀 and 日本 and é");
        assertEquals("smile 😀 and 日本 and é", Files.readString(file));

        Files.writeString(file, "日本", StandardCharsets.ISO_8859_1);
        assertArrayEquals(new byte[] { '?', '?' }, Files.readAllBytes(file));

        assertThrows(NullPointerException.class, () -> Files.writeString(file, null));
        assertThrows(NullPointerException.class, () -> Files.writeString(file, "x", null));
        assertThrows(NullPointerException.class, () -> Files.writeString(null, "x"));
    }

    @Test
    public void testRead_MalformedUtf8() throws IOException {
        File bad = writeRawBytes("bad.txt", new byte[] { 'a', (byte) 0xFF, 'b', '\n', 'c' });
        assertEquals(Arrays.asList("a�b", "c"), Files.readLines(bad, StandardCharsets.UTF_8));
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(bad));
        assertThrows(MalformedInputException.class, () -> Files.readAllLines(bad, StandardCharsets.UTF_8));
        assertThrows(MalformedInputException.class, () -> Files.readString(bad));
        assertThrows(MalformedInputException.class, () -> Files.readString(bad, StandardCharsets.UTF_8));

        File latin = writeRawBytes("latin1.txt", "Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1));
        assertThrows(MalformedInputException.class, () -> Files.readString(latin));
        assertEquals("Línea 1\nLínea 2", Files.readString(latin, StandardCharsets.ISO_8859_1));
    }

    @Test
    public void testReadAllLines_Utf8Bom_IsKeptByBothPaths() throws IOException {
        byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = "x\ny".getBytes(StandardCharsets.UTF_8);
        byte[] all = new byte[bom.length + body.length];
        System.arraycopy(bom, 0, all, 0, bom.length);
        System.arraycopy(body, 0, all, bom.length, body.length);
        File f = writeRawBytes("bom.txt", all);
        assertEquals(Arrays.asList("\uFEFFx", "y"), Files.readAllLines(f));
        assertEquals(Arrays.asList("\uFEFFx", "y"), Files.readLines(f, StandardCharsets.UTF_8));
        assertEquals("\uFEFFx\ny", Files.readString(f));
    }

    @Test
    public void testMoreFiles_ExtendsFiles() {
        assertTrue(Files.class.isAssignableFrom(Files.MoreFiles.class));
    }

    @Test
    public void testNullArguments_AreRejectedWithNullPointerException() {
        assertThrows(NullPointerException.class, () -> Files.readString((File) null));
        assertThrows(NullPointerException.class, () -> Files.readAllLines((File) null));
        assertThrows(NullPointerException.class, () -> Files.readAllBytes(null));
        assertThrows(NullPointerException.class, () -> Files.readLines(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> Files.readLines(testFile, null));
        assertThrows(NullPointerException.class, () -> Files.simplifyPath(null));
        assertThrows(NullPointerException.class, () -> Files.getFileExtension((String) null));
        assertThrows(NullPointerException.class, () -> Files.getNameWithoutExtension((String) null));
        assertThrows(NullPointerException.class, () -> Files.listFiles(null));
        assertThrows(NullPointerException.class, () -> Files.asByteSource((File) null));
        assertThrows(NullPointerException.class, () -> Files.asCharSource((File) null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> Files.touch((File) null));
        assertThrows(NullPointerException.class, () -> Files.touch((Path) null));
        assertThrows(NullPointerException.class, () -> Files.copy((File) null, testFile));
        assertThrows(NullPointerException.class, () -> Files.equal((File) null, testFile));
        assertThrows(NullPointerException.class, () -> Files.deleteRecursively(null, RecursiveDeleteOption.ALLOW_INSECURE));
        assertThrows(NullPointerException.class, () -> Files.newReader(null, StandardCharsets.UTF_8));
        assertThrows(NullPointerException.class, () -> Files.newWriter(null, StandardCharsets.UTF_8));
    }

    @Test
    public void testSpecialCharacterFilenames() throws IOException {
        for (String name : new String[] { "file with spaces.txt", "file-with-dashes.txt", "file_with_underscores.txt", "file.multiple.dots.txt" }) {
            File special = file(name);
            Files.touch(special);
            assertTrue(special.exists());
            assertNotNull(Files.getFileExtension(name));
        }
    }
}
