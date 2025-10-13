package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
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

@Tag("new-test")
public class Files101Test extends TestBase {

    @TempDir
    Path tempDir;

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

    @Test
    public void testNewWriter() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");

        try (BufferedWriter writer = Files.newWriter(file, StandardCharsets.UTF_8)) {
            writer.write("Test output");
        }

        assertEquals("Test output", Files.readString(file));
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
    public void testAsByteSource_Path() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Test content".getBytes());

        ByteSource source = Files.asByteSource(path, StandardOpenOption.READ);
        byte[] bytes = source.read();
        assertEquals("Test content", new String(bytes, StandardCharsets.UTF_8));
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
    public void testAsByteSink_Path() throws IOException {
        Path path = tempDir.resolve("output.txt");

        ByteSink sink = Files.asByteSink(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        sink.write("Written content".getBytes(StandardCharsets.UTF_8));

        assertEquals("Written content", new String(java.nio.file.Files.readAllBytes(path)));
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
    public void testAsCharSource_Path() throws IOException {
        Path path = tempDir.resolve("test.txt");
        java.nio.file.Files.write(path, "Character content".getBytes(StandardCharsets.UTF_8));

        CharSource source = Files.asCharSource(path, StandardCharsets.UTF_8, StandardOpenOption.READ);
        String content = source.read();
        assertEquals("Character content", content);
    }

    @Test
    public void testAsCharSink_File() throws IOException {
        File file = new File(tempDir.toFile(), "output.txt");

        CharSink sink = Files.asCharSink(file, StandardCharsets.UTF_8);
        sink.write("Character output");

        assertEquals("Character output", Files.readString(file));
    }

    @Test
    public void testAsCharSink_Path() throws IOException {
        Path path = tempDir.resolve("output.txt");

        CharSink sink = Files.asCharSink(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        sink.write("Character output");

        assertEquals("Character output", new String(java.nio.file.Files.readAllBytes(path)));
    }

    @Test
    public void testToByteArray() throws IOException {
        File file = new File(tempDir.toFile(), "bytes.txt");
        byte[] originalBytes = "Byte array content".getBytes(StandardCharsets.UTF_8);
        Files.write(originalBytes, file);

        byte[] readBytes = Files.toByteArray(file);
        assertArrayEquals(originalBytes, readBytes);
    }

    @Test
    public void testWrite() throws IOException {
        File file = new File(tempDir.toFile(), "write.txt");
        byte[] bytes = "Written bytes".getBytes(StandardCharsets.UTF_8);

        Files.write(bytes, file);

        assertArrayEquals(bytes, Files.toByteArray(file));
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
    public void testEqual_Paths() throws IOException {
        Path path1 = tempDir.resolve("path1.txt");
        Path path2 = tempDir.resolve("path2.txt");

        java.nio.file.Files.write(path1, "Same content".getBytes());
        java.nio.file.Files.write(path2, "Same content".getBytes());

        assertTrue(Files.equal(path1, path2));
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
    public void testTouch_Path() throws IOException {
        Path path = tempDir.resolve("touch.txt");
        assertFalse(java.nio.file.Files.exists(path));

        Files.touch(path);
        assertTrue(java.nio.file.Files.exists(path));
    }

    @Test
    public void testCreateTempDir() {
        File tempDir = Files.createTempDir();
        assertTrue(tempDir.exists());
        assertTrue(tempDir.isDirectory());
        tempDir.delete();
    }

    @Test
    public void testCreateParentDirs() throws IOException {
        File deepFile = new File(tempDir.toFile(), "a/b/c/d/file.txt");
        assertFalse(deepFile.getParentFile().exists());

        Files.createParentDirs(deepFile);
        assertTrue(deepFile.getParentFile().exists());
    }

    @Test
    public void testCreateParentDirectories() throws IOException {
        Path deepPath = tempDir.resolve("x/y/z/file.txt");
        assertFalse(java.nio.file.Files.exists(deepPath.getParent()));

        Files.createParentDirectories(deepPath);
        assertTrue(java.nio.file.Files.exists(deepPath.getParent()));
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
    public void testReadLines() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        Files.write("Line 1\nLine 2\nLine 3".getBytes(StandardCharsets.UTF_8), file);

        List<String> lines = Files.readLines(file, StandardCharsets.UTF_8);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", Files.simplifyPath(""));
        assertEquals(".", Files.simplifyPath("."));
        assertEquals("foo", Files.simplifyPath("./foo"));
        assertEquals("foo/bar", Files.simplifyPath("foo//bar"));
        assertEquals("foo/baz", Files.simplifyPath("foo/bar/../baz"));
        assertEquals("/foo/baz", Files.simplifyPath("/foo//bar/../baz/./"));
        assertEquals("..", Files.simplifyPath(".."));
        assertEquals("../..", Files.simplifyPath("../.."));
        assertEquals("/", Files.simplifyPath("//"));
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
    public void testGetNameWithoutExtension_Path() {
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("document.pdf")));
        assertEquals("archive.tar", Files.getNameWithoutExtension(Paths.get("archive.tar.gz")));
        assertEquals("README", Files.getNameWithoutExtension(Paths.get("README")));
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
    public void testGetFileExtension_Path() {
        assertEquals("pdf", Files.getFileExtension(Paths.get("document.pdf")));
        assertEquals("gz", Files.getFileExtension(Paths.get("archive.tar.gz")));
        assertEquals("", Files.getFileExtension(Paths.get("README")));
    }

    @Test
    public void testFileTraverser() throws IOException {
        File root = new File(tempDir.toFile(), "root");
        root.mkdir();
        new File(root, "file1.txt").createNewFile();
        new File(root, "subdir").mkdir();

        Traverser<File> traverser = Files.fileTraverser();
        List<String> names = new ArrayList<>();
        for (File file : traverser.breadthFirst(root)) {
            names.add(file.getName());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("subdir"));
    }

    @Test
    public void testPathTraverser() throws IOException {
        Path root = tempDir.resolve("root");
        java.nio.file.Files.createDirectory(root);
        java.nio.file.Files.createFile(root.resolve("file.txt"));
        java.nio.file.Files.createDirectory(root.resolve("subdir"));

        Traverser<Path> traverser = Files.pathTraverser();
        Set<String> names = new HashSet<>();
        for (Path path : traverser.breadthFirst(root)) {
            names.add(path.getFileName().toString());
        }

        assertTrue(names.contains("root"));
        assertTrue(names.contains("file.txt"));
        assertTrue(names.contains("subdir"));
    }

    @Test
    public void testListFiles() throws IOException {
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
    public void testDeleteRecursively() throws IOException {
        Path root = tempDir.resolve("delete");
        java.nio.file.Files.createDirectory(root);
        Path subdir = root.resolve("subdir");
        java.nio.file.Files.createDirectory(subdir);
        java.nio.file.Files.createFile(subdir.resolve("file.txt"));

        Files.deleteRecursively(root, RecursiveDeleteOption.ALLOW_INSECURE);
        assertFalse(java.nio.file.Files.exists(root));
    }

    @Test
    public void testDeleteDirectoryContents() throws IOException {
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
    public void testIsRegularFile() throws IOException {
        Path dir = tempDir.resolve("testdir");
        Path file = tempDir.resolve("testfile.txt");
        java.nio.file.Files.createDirectory(dir);
        java.nio.file.Files.createFile(file);

        Predicate<Path> isFilePredicate = Files.isRegularFile();
        assertFalse(isFilePredicate.apply(dir));
        assertTrue(isFilePredicate.apply(file));
    }

    @Test
    public void testReadAllBytes() throws IOException {
        File file = new File(tempDir.toFile(), "bytes.txt");
        byte[] content = "Read all bytes".getBytes(StandardCharsets.UTF_8);
        Files.write(content, file);

        byte[] read = Files.readAllBytes(file);
        assertArrayEquals(content, read);
    }

    @Test
    public void testReadString_Default() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        Files.write("UTF-8 content".getBytes(StandardCharsets.UTF_8), file);

        String content = Files.readString(file);
        assertEquals("UTF-8 content", content);
    }

    @Test
    public void testReadString_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "string.txt");
        String content = "ISO-8859-1 content: áéíóú";
        Files.write(content.getBytes(StandardCharsets.ISO_8859_1), file);

        String read = Files.readString(file, StandardCharsets.ISO_8859_1);
        assertEquals(content, read);
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
    public void testReadAllLines_WithCharset() throws IOException {
        File file = new File(tempDir.toFile(), "lines.txt");
        Files.write("Línea 1\nLínea 2".getBytes(StandardCharsets.ISO_8859_1), file);

        List<String> lines = Files.readAllLines(file, StandardCharsets.ISO_8859_1);
        assertEquals(2, lines.size());
        assertEquals("Línea 1", lines.get(0));
        assertEquals("Línea 2", lines.get(1));
    }

    @Test
    public void testMoreFiles() {
        assertTrue(Files.MoreFiles.class.getSuperclass() == Files.class);
    }
}
