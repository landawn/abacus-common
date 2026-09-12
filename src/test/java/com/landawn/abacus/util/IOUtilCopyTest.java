package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

public class IOUtilCopyTest extends IOUtilTestSupport {
    @Test
    public void testCopyToDirectory_File() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(tempFile, destDir);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(copiedFile));
    }

    @Test
    public void testCopyToDirectory_FileWithPreserveFileDate() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyToDirectory(tempFile, destDir, true);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals(originalTime, copiedFile.lastModified());
    }

    @Test
    public void testCopyToDirectory_FileWithoutPreserveFileDate() throws Exception {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyToDirectory(tempFile, destDir, false);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
    }

    @Test
    public void testCopyToDirectory_Directory() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File subFile1 = new File(srcDir, "file1.txt");
        File subFile2 = new File(srcDir, "file2.txt");
        Files.write(subFile1.toPath(), "Content 1".getBytes());
        Files.write(subFile2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(srcDir, destDir);

        File copiedDir = new File(destDir, srcDir.getName());
        assertTrue(copiedDir.exists());
        assertTrue(copiedDir.isDirectory());
        assertTrue(new File(copiedDir, "file1.txt").exists());
        assertTrue(new File(copiedDir, "file2.txt").exists());
    }

    @Test
    public void testCopyToDirectory_WithFilter() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File txtFile = new File(srcDir, "file.txt");
        File logFile = new File(srcDir, "file.log");
        Files.write(txtFile.toPath(), "Text content".getBytes());
        Files.write(logFile.toPath(), "Log content".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(srcDir, destDir, true, (parent, file) -> file.getName().endsWith(".txt"));

        File copiedDir = new File(destDir, srcDir.getName());
        assertTrue(new File(copiedDir, "file.txt").exists());
        assertTrue(!new File(copiedDir, "file.log").exists());
    }

    @Test
    public void testCopyToDirectory_SameDirectory() throws Exception {
        File parentDir = tempFile.getParentFile();
        IOUtil.copyToDirectory(tempFile, parentDir);

        File copiedFile = new File(parentDir, "Copy of " + tempFile.getName());
        assertTrue(copiedFile.exists());
    }

    // ===== getRelativePath via copyToDirectory with subdirs =====

    @Test
    public void testCopyToDirectory_PreservesRelativePaths() throws Exception {
        File subDir = new File(tempFolder.toFile(), "srcDir");
        subDir.mkdirs();
        File srcFile = new File(subDir, "hello.txt");
        Files.write(srcFile.toPath(), "Hello".getBytes(UTF_8));

        File destDir = new File(tempFolder.toFile(), "destDir");
        destDir.mkdirs();

        IOUtil.copyToDirectory(srcFile, destDir);
        File copiedFile = new File(destDir, "hello.txt");
        assertTrue(copiedFile.exists());
    }

    @Test
    public void testCopyDirectory_Basic() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(srcDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyDirectory(srcDir, destDir);

        assertTrue(new File(destDir, file1.getName()).exists());
        assertTrue(new File(destDir, file2.getName()).exists());
    }

    @Test
    public void testCopyDirectory_Nested() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File subDir = new File(srcDir, "subdir");
        subDir.mkdir();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyDirectory(srcDir, destDir);

        assertTrue(new File(destDir, "file1.txt").exists());
        File copiedSubDir = new File(destDir, "subdir");
        assertTrue(copiedSubDir.exists());
        assertTrue(new File(copiedSubDir, "file2.txt").exists());
    }

    // ===== copyDirectory =====

    @Test
    public void testCopyDirectory_basicFiles() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "copy-src").toFile();
        File destDir = Files.createTempDirectory(tempFolder, "copy-dest").toFile();
        // Create files in srcDir
        File f1 = new File(srcDir, "file1.txt");
        Files.write(f1.toPath(), "content1".getBytes(UTF_8));
        File f2 = new File(srcDir, "file2.txt");
        Files.write(f2.toPath(), "content2".getBytes(UTF_8));

        IOUtil.copyDirectory(srcDir, destDir);

        File copied1 = new File(destDir, "file1.txt");
        File copied2 = new File(destDir, "file2.txt");
        assertTrue(copied1.exists());
        assertTrue(copied2.exists());
        assertEquals("content1", new String(IOUtil.readAllBytes(copied1), UTF_8));
        assertEquals("content2", new String(IOUtil.readAllBytes(copied2), UTF_8));
    }

    @Test
    public void testCopyDirectory_emptyDirectory() throws Exception {
        File srcDir = Files.createTempDirectory(tempFolder, "copy-empty-src").toFile();
        File destDir = Files.createTempDirectory(tempFolder, "copy-empty-dest").toFile();

        IOUtil.copyDirectory(srcDir, destDir);

        assertEquals(0, destDir.listFiles().length);
    }

    @Test
    public void testCopyFile_Basic() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyFile(tempFile, destFile);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyFile_WithPreserveFileDate() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        long originalTime = tempFile.lastModified();
        Thread.sleep(100);

        IOUtil.copyFile(tempFile, destFile, true);

        assertTrue(destFile.exists());
        assertEquals(originalTime, destFile.lastModified());
    }

    @Test
    public void testCopyFile_WithoutPreserveFileDate() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyFile(tempFile, destFile, false);

        assertTrue(destFile.exists());
    }

    @Test
    public void testCopyFile_WithCopyOptions() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();

        IOUtil.copyFile(tempFile, destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyFile_WithPreserveDateAndCopyOptions() throws Exception {
        File destFile = Files.createTempFile(tempFolder, "dest", ".txt").toFile();

        IOUtil.copyFile(tempFile, destFile, true, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(destFile.exists());
    }

    @Test
    public void testCopyFile_ToOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        long bytesWritten = IOUtil.copyFile(tempFile, baos);

        assertTrue(bytesWritten > 0);
        assertEquals(TEST_CONTENT, new String(baos.toByteArray(), UTF_8));
    }

    @Test
    public void testCopyURLToFile_Basic() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "URL content".getBytes());

        java.net.URL url = sourceFile.toURI().toURL();
        File destFile = Files.createTempFile(tempFolder, "url_dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyURLToFile(url, destFile);

        assertTrue(destFile.exists());
        assertEquals("URL content", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopyURLToFile_WithTimeout() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "URL content with timeout".getBytes());

        java.net.URL url = sourceFile.toURI().toURL();
        File destFile = Files.createTempFile(tempFolder, "url_dest", ".txt").toFile();
        destFile.delete();

        IOUtil.copyURLToFile(url, destFile, 5000, 5000);

        assertTrue(destFile.exists());
        assertEquals("URL content with timeout", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testCopy_PathToPath() throws Exception {
        Path source = tempFile.toPath();
        Path target = Files.createTempFile(tempFolder, "path_dest", ".txt");
        Files.delete(target);

        Path result = IOUtil.copy(source, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertNotNull(result);
        assertTrue(Files.exists(target));
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(target.toFile()));
    }

    @Test
    public void testCopy_InputStreamToPath() throws Exception {
        InputStream is = new ByteArrayInputStream("InputStream content".getBytes());
        Path target = Files.createTempFile(tempFolder, "is_dest", ".txt");
        Files.delete(target);

        long bytesWritten = IOUtil.copy(is, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertTrue(bytesWritten > 0);
        assertTrue(Files.exists(target));
        assertEquals("InputStream content", IOUtil.readAllToString(target.toFile()));
    }

    @Test
    public void testCopy_PathToOutputStream() throws Exception {
        Path source = tempFile.toPath();
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        long bytesWritten = IOUtil.copy(source, baos);

        assertTrue(bytesWritten > 0);
        assertEquals(TEST_CONTENT, new String(baos.toByteArray(), UTF_8));
    }

    @Test
    public void testCopy_missingPathSourceIsFileNotFoundException() {
        final Path missing = tempFolder.resolve("no-such-path-copy-source.txt");
        final Path target = tempFolder.resolve("copy-target.txt");
        final FileNotFoundException thrown = assertThrows(FileNotFoundException.class, () -> IOUtil.copy(missing, target));
        assertTrue(thrown.getCause() instanceof NoSuchFileException);
        assertThrows(FileNotFoundException.class, () -> IOUtil.move(missing, target));
        assertThrows(FileNotFoundException.class, () -> IOUtil.copy(missing, new java.io.ByteArrayOutputStream()));
    }

    @Test
    public void testCopy_pathDirectoryDoesNotCopyEntries() throws Exception {
        final Path srcDir = Files.createTempDirectory(tempFolder, "copy-dir-src");
        Files.writeString(srcDir.resolve("child.txt"), "x");
        final Path destDir = tempFolder.resolve("copy-dir-dest");
        IOUtil.copy(srcDir, destDir);
        assertTrue(Files.isDirectory(destDir));
        assertFalse(Files.exists(destDir.resolve("child.txt")));
    }

    @Test
    public void testCopyDirectory_doesNotFollowSymlinkCycle() throws Exception {
        File src = Files.createTempDirectory(tempFolder, "copy-symlink-src").toFile();
        File dest = Files.createTempDirectory(tempFolder, "copy-symlink-dest").toFile();
        // Make dest non-existent so copyDirectory creates it inside.
        assertTrue(IOUtil.deleteRecursivelyIfExists(dest));
        Files.write(new File(src, "a.txt").toPath(), "A".getBytes(UTF_8));

        // src/loop -> src
        Path loop = src.toPath().resolve("loop");
        assumeTrue(trySymlink(loop, src.toPath()), SYMLINK_UNSUPPORTED);

        // Must terminate; previously this could spin forever.
        IOUtil.copyDirectory(src, dest);

        // a.txt was copied; we don't recurse through the symlink, so we shouldn't see
        // src/loop/loop/loop/... materialized as nested directories at the destination.
        File copiedA = new File(dest, src.getName() + "/a.txt");
        assertTrue(copiedA.exists() || new File(dest, "a.txt").exists());
    }

    @Test
    public void testCopyEmptyDirectoryPreservesFileDate() throws Exception {
        // regression: the empty-directory early return skipped the preserveFileDate step
        final java.io.File srcDir = tempFolder.resolve("regression_empty_src_dir").toFile();
        assertTrue(srcDir.mkdirs());
        final long past = (System.currentTimeMillis() - 200_000_000L) / 1000 * 1000;
        assertTrue(srcDir.setLastModified(past));

        final java.io.File destParent = tempFolder.resolve("regression_copy_dest").toFile();
        assertTrue(destParent.mkdirs());
        IOUtil.copyToDirectory(srcDir, destParent, true);

        final java.io.File copied = new java.io.File(destParent, srcDir.getName());
        assertTrue(copied.exists());
        assertEquals(srcDir.lastModified(), copied.lastModified());
    }

    @Test
    public void testCopyToDirectory_NestedDirectorySymlinkCopiedAsLink() throws Exception {
        File src = Files.createTempDirectory(tempFolder, "copy-link-src").toFile();
        File outside = Files.createTempDirectory(tempFolder, "copy-link-outside").toFile();
        Files.write(new File(outside, "secret.txt").toPath(), "secret".getBytes(UTF_8));
        Path nestedLink = src.toPath().resolve("linkdir");
        assumeTrue(trySymlink(nestedLink, outside.toPath()), SYMLINK_UNSUPPORTED);

        File destParent = Files.createTempDirectory(tempFolder, "copy-link-dest").toFile();
        IOUtil.copyToDirectory(src, destParent, true);

        File copiedLink = new File(new File(destParent, src.getName()), "linkdir");
        assertTrue(Files.isSymbolicLink(copiedLink.toPath()));
        assertTrue(Files.isSameFile(copiedLink.toPath(), outside.toPath()));
    }

    @Test
    public void testCopyDirectory_ChildDirectorySymlinkCopiedAsLink() throws Exception {
        File src = Files.createTempDirectory(tempFolder, "copy-dir-src").toFile();
        File dest = Files.createTempDirectory(tempFolder, "copy-dir-dest").toFile();
        Files.write(new File(src, "a.txt").toPath(), "A".getBytes(UTF_8));
        File outside = Files.createTempDirectory(tempFolder, "copy-dir-outside").toFile();
        Files.write(new File(outside, "secret.txt").toPath(), "secret".getBytes(UTF_8));
        Path childLink = src.toPath().resolve("linkdir");
        assumeTrue(trySymlink(childLink, outside.toPath()), SYMLINK_UNSUPPORTED);

        IOUtil.copyDirectory(src, dest);

        File copiedLink = new File(dest, "linkdir");
        assertTrue(Files.isSymbolicLink(copiedLink.toPath()));
        assertTrue(Files.isSameFile(copiedLink.toPath(), outside.toPath()));
        assertTrue(new File(dest, "a.txt").exists());
    }

    @Test
    public void testCopyToDirectory_NestedFileSymlinkCopiedAsLink() throws Exception {
        File src = Files.createTempDirectory(tempFolder, "copy-file-link-src").toFile();
        File real = new File(src, "real.txt");
        Files.write(real.toPath(), "payload".getBytes(UTF_8));
        Path nestedLink = src.toPath().resolve("link.txt");
        assumeTrue(trySymlink(nestedLink, real.toPath()), SYMLINK_UNSUPPORTED);

        File destParent = Files.createTempDirectory(tempFolder, "copy-file-link-dest").toFile();
        IOUtil.copyToDirectory(src, destParent, true);

        File copiedLink = new File(new File(destParent, src.getName()), "link.txt");
        assertTrue(Files.isSymbolicLink(copiedLink.toPath()));
        assertTrue(Files.isSameFile(copiedLink.toPath(), real.toPath()));
    }

    @Test
    public void testCopyToDirectory_returnsTheFileActuallyWritten() throws Exception {
        final File src = new File(tempFolder.toFile(), "src-doc.txt");
        Files.write(src.toPath(), "body".getBytes(UTF_8));
        final File destDir = new File(tempFolder.toFile(), "copy-dest");

        final File written = IOUtil.copyToDirectory(src, destDir);

        assertNotNull(written);
        assertTrue(written.isFile());
        assertEquals("src-doc.txt", written.getName());
        assertEquals("body", new String(Files.readAllBytes(written.toPath()), UTF_8));
    }

    @Test
    public void testCopyToDirectory_returnsCopyOfNameWhenCopiedIntoOwnParent() throws Exception {
        final File dir = Files.createTempDirectory(tempFolder, "self-copy").toFile();
        final File src = new File(dir, "doc.txt");
        Files.write(src.toPath(), "body".getBytes(UTF_8));

        final File written = IOUtil.copyToDirectory(src, dir);

        assertEquals("Copy of doc.txt", written.getName());
        assertTrue(written.isFile());
        assertEquals("body", new String(Files.readAllBytes(written.toPath()), UTF_8));
    }

    @Test
    public void testCopyToDirectory_returnsTheCreatedDirectoryForADirectorySource() throws Exception {
        final File srcDir = Files.createTempDirectory(tempFolder, "tree-src").toFile();
        Files.write(new File(srcDir, "inner.txt").toPath(), "inner".getBytes(UTF_8));
        final File destDir = new File(tempFolder.toFile(), "tree-dest");

        final File written = IOUtil.copyToDirectory(srcDir, destDir);

        assertTrue(written.isDirectory());
        assertEquals(srcDir.getName(), written.getName());
        assertTrue(new File(written, "inner.txt").isFile());
    }

    @Test
    public void testCopyToDirectory_rejectedCallLeavesNoNewDirectoryBehind() throws Exception {
        final File srcDir = Files.createTempDirectory(tempFolder, "no-side-effect-src").toFile();
        Files.write(new File(srcDir, "a.txt").toPath(), "a".getBytes(UTF_8));
        final File insideSrc = new File(srcDir, "nested-dest");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(srcDir, insideSrc));

        // The destination is inside the source, so the call is rejected - and must not have created it first.
        assertFalse(insideSrc.exists(), "a rejected copyToDirectory must not create the destination directory");
    }

    @Test
    public void testCopyToDirectory_nullFilterLeavesNoNewDirectoryBehind() throws Exception {
        final File src = new File(tempFolder.toFile(), "filter-src.txt");
        Files.write(src.toPath(), "x".getBytes(UTF_8));
        final File destDir = new File(tempFolder.toFile(), "filter-dest");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(src, destDir, true, null));

        assertFalse(destDir.exists(), "a rejected copyToDirectory must not create the destination directory");
    }
}
