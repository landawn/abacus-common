package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import com.google.common.base.Predicate;
import com.google.common.io.InsecureRecursiveDeleteException;
import com.google.common.io.RecursiveDeleteOption;
import com.landawn.abacus.util.ImmutableList;

/** Files path and directory contracts. */
@SuppressWarnings("resource")
public class FilesTest extends FilesTestSupport {

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

    // deleteRecursively

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
    public void testDeleteRecursivelyMissingParentThrowsNoSuchFileExceptionWithoutOption(@TempDir Path dir) {
        // The parent directory is opened before the SecureDirectoryStream / ALLOW_INSECURE check, so a
        // missing parent is reported as NoSuchFileException on every file system, with or without the option.
        Path missingParentChild = dir.resolve("no-such-parent").resolve("child");

        assertThrows(NoSuchFileException.class, () -> Files.deleteRecursively(missingParentChild));
        assertThrows(NoSuchFileException.class, () -> Files.deleteRecursively(missingParentChild, RecursiveDeleteOption.ALLOW_INSECURE));
        assertFalse(java.nio.file.Files.exists(dir.resolve("no-such-parent")));
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
    @EnabledOnOs(OS.WINDOWS)
    public void testC364_windowsBackslashIsSeparatorForNameAndExtension() {
        assertEquals("", Files.getFileExtension("a.b\\c"));
        assertEquals("c", Files.getNameWithoutExtension("a.b\\c"));
    }

    // getNameWithoutExtension

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
    public void testC364_nameAndExtensionDropThePathAndTrailingSeparator() {
        assertEquals("document", Files.getNameWithoutExtension("/home/user/document.pdf"));
        assertEquals("b", Files.getNameWithoutExtension("a/b.c/"));
        assertEquals("c", Files.getFileExtension("a/b.c/"));
        assertThrows(NullPointerException.class, () -> Files.getNameWithoutExtension((String) null));
        assertThrows(NullPointerException.class, () -> Files.getFileExtension((String) null));
    }

    @Test
    public void testC368_pathWithoutFileNameGivesEmptyString() {
        final Path root = tempDir.getRoot();
        assertEquals("", Files.getNameWithoutExtension(root));
        assertEquals("", Files.getFileExtension(root));
        assertEquals("", Files.getNameWithoutExtension(Paths.get("")));
        assertEquals("", Files.getFileExtension(Paths.get("")));
        assertEquals("document", Files.getNameWithoutExtension(Paths.get("dir", "document.pdf")));
        assertEquals("pdf", Files.getFileExtension(Paths.get("dir", "document.pdf")));
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
    public void testC364_simplifyPathTreatsDriveAndUncAsOrdinarySegments() {
        assertEquals("b", Files.simplifyPath("C:/a/../../b"));
        assertEquals("b", Files.simplifyPath("C:/../b"));
        assertEquals("/host/x", Files.simplifyPath("//host/share/../x"));
        assertEquals("a\\..\\b", Files.simplifyPath("a\\..\\b"));
        assertEquals("/foo/baz", Files.simplifyPath("/foo//bar/../baz/./"));
        assertEquals(".", Files.simplifyPath(""));
        assertThrows(NullPointerException.class, () -> Files.simplifyPath(null));
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

}
