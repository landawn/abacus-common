package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilDeleteTest extends IOUtilTestSupport {
    @Test
    public void testDeleteQuietly_NullFile() {
        boolean result = IOUtil.deleteQuietly(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteQuietly_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete_quiet", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteQuietly(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteQuietly_NonExistingFile() throws Exception {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");

        boolean result = IOUtil.deleteQuietly(file);

        assertTrue(!result);
    }

    @Test
    public void testDeleteQuietly_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "del-test").toFile();
        assertTrue(dir.exists());
        boolean result = IOUtil.deleteQuietly(dir);
        assertTrue(result);
        assertFalse(dir.exists());
    }

    @Test
    public void testDeleteIfExists_NullFile() {
        boolean result = IOUtil.deleteIfExists(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteIfExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteIfExists(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteIfExists_NonExistingFile() throws Exception {
        File file = new File(tempFolder.toFile(), "nonexistent.txt");

        boolean result = IOUtil.deleteIfExists(file);

        assertTrue(!result);
    }

    @Test
    public void testDeleteIfExists_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_dir").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.deleteIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
    }

    @Test
    public void testDeleteAllIfExists_NullFile() {
        boolean result = IOUtil.deleteRecursivelyIfExists(null);
        assertTrue(!result);
    }

    @Test
    public void testDeleteRecursivelyIfExists_NullFile() {
        boolean result = IOUtil.deleteRecursivelyIfExists(null);
        assertFalse(result);
    }

    @Test
    public void testDeleteRecursivelyIfExists_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent_for_recursive_delete");
        boolean result = IOUtil.deleteRecursivelyIfExists(nonExistent);
        assertFalse(result);
    }

    // ========== Additional tests for deleteRecursivelyIfExists ==========

    @Test
    public void testDeleteRecursivelyIfExists_SingleFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "del-recursive", ".txt").toFile();
        Files.write(file.toPath(), "content".getBytes(UTF_8));
        assertTrue(file.exists());

        boolean result = IOUtil.deleteRecursivelyIfExists(file);
        assertTrue(result);
        assertFalse(file.exists());
    }

    @Test
    public void testDeleteRecursivelyIfExists_DirectoryWithFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "del-recursive-dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "content2".getBytes(UTF_8));
        assertTrue(dir.exists());
        assertTrue(file1.exists());

        boolean result = IOUtil.deleteRecursivelyIfExists(dir);
        assertTrue(result);
        assertFalse(dir.exists());
    }

    @Test
    public void testDeleteRecursivelyIfExists_NestedDirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "del-recursive-nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File subFile = new File(subDir, "nested.txt");
        Files.write(subFile.toPath(), "nested content".getBytes(UTF_8));
        File deepDir = new File(subDir, "deepdir");
        deepDir.mkdir();
        File deepFile = new File(deepDir, "deep.txt");
        Files.write(deepFile.toPath(), "deep content".getBytes(UTF_8));

        boolean result = IOUtil.deleteRecursivelyIfExists(dir);
        assertTrue(result);
        assertFalse(dir.exists());
        assertFalse(subDir.exists());
        assertFalse(deepDir.exists());
    }

    @Test
    public void testDeleteRecursivelyIfExists_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "del-recursive-empty").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.deleteRecursivelyIfExists(dir);
        assertTrue(result);
        assertFalse(dir.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_AllFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_files_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!file1.exists());
        assertTrue(!file2.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_WithFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_filter_dir").toFile();
        File txtFile = new File(dir, "file.txt");
        File logFile = new File(dir, "file.log");
        Files.write(txtFile.toPath(), "Text content".getBytes());
        Files.write(logFile.toPath(), "Log content".getBytes());

        boolean result = IOUtil.deleteFilesFromDirectory(dir, (parent, file) -> file.getName().endsWith(".txt"));

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!txtFile.exists());
        assertTrue(logFile.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_EmptyDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_empty_dir").toFile();

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
    }

    @Test
    public void testDeleteFilesFromDirectory_NonExistingDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "nonexistent_dir");

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(!result);
    }

    @Test
    public void testDeleteFilesFromDirectory_NullDirectory() throws Exception {
        boolean result = IOUtil.deleteFilesFromDirectory(null);
        assertFalse(result);
    }

    @Test
    public void testDeleteFilesFromDirectory_FileNotDirectory() throws Exception {
        assertFalse(IOUtil.deleteFilesFromDirectory(tempFile));
    }

    @Test
    public void testDeleteFilesFromDirectory_ReturnsFalseWhenListingFails() throws Exception {
        File dir = new ListingFailureFile(new File(tempFolder.toFile(), "listing_failure_dir"));

        assertFalse(IOUtil.deleteFilesFromDirectory(dir));
    }

    @Test
    public void testDeleteFilesFromDirectory_WithSubdirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "clean_nested_dir").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteFilesFromDirectory(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(!subDir.exists());
        assertTrue(!file1.exists());
    }

    @Test
    public void testDeleteAllIfExists_SingleFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "delete_all", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.deleteRecursivelyIfExists(file);

        assertTrue(result);
        assertTrue(!file.exists());
    }

    @Test
    public void testDeleteAllIfExists_DirectoryWithFiles() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_all_dir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteRecursivelyIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
        assertTrue(!file1.exists());
        assertTrue(!file2.exists());
    }

    @Test
    public void testDeleteAllIfExists_NestedDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "delete_all_nested").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes());
        Files.write(file2.toPath(), "Content 2".getBytes());

        boolean result = IOUtil.deleteRecursivelyIfExists(dir);

        assertTrue(result);
        assertTrue(!dir.exists());
    }

    @Test
    public void testDeleteFilesFromDirectoryAppliesFilterToDescendants() throws IOException {
        // A directory whose NAME matches the filter must not take its non-matching contents down with it: the
        // filter is consulted at every depth, exactly as copyToDirectory(.., filter) does.
        final File root = new File(tempFolder.toFile(), "filtered-delete");
        final File matchingDir = new File(root, "keep.txt");
        assertTrue(matchingDir.mkdirs());

        final File protectedFile = new File(matchingDir, "precious.dat");
        final File nestedMatch = new File(matchingDir, "inner.txt");
        final File siblingMatch = new File(root, "top.txt");
        final File siblingKept = new File(root, "other.log");

        for (final File f : new File[] { protectedFile, nestedMatch, siblingMatch, siblingKept }) {
            Files.write(f.toPath(), "x".getBytes(UTF_8));
        }

        // FALSE, and deliberately so: the filter accepted the directory "keep.txt", but it could not be removed
        // because a non-matching file survived inside it. An accepted entry that is still on disk is a failure
        // to report - even though refusing to delete it is exactly the protection the filter asked for.
        assertFalse(IOUtil.deleteFilesFromDirectory(root, (parent, file) -> file.getName().endsWith(".txt")));

        assertTrue(protectedFile.exists(), "a non-matching file must survive inside a matching directory");
        assertFalse(nestedMatch.exists(), "a matching file must be deleted at any depth");
        assertFalse(siblingMatch.exists());
        assertTrue(siblingKept.exists());
        assertTrue(matchingDir.exists(), "a matching directory that still holds survivors must not be removed");
    }

    @Test
    public void testDeleteFilesFromDirectoryRemovesEmptiedMatchingDirectory() throws IOException {
        final File root = new File(tempFolder.toFile(), "emptied-delete");
        final File matchingDir = new File(root, "gone.txt");
        assertTrue(matchingDir.mkdirs());
        Files.write(new File(matchingDir, "inner.txt").toPath(), "x".getBytes(UTF_8));

        assertTrue(IOUtil.deleteFilesFromDirectory(root, (parent, file) -> file.getName().endsWith(".txt")));

        assertFalse(matchingDir.exists(), "a matching directory left empty by the filter is removed");
        assertTrue(root.exists());
        assertEquals(0, root.list().length);
    }
}
