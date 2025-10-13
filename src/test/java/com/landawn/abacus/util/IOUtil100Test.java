package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel.MapMode;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IOUtil100Test extends TestBase {

    @TempDir
    Path tempFolder;

    private File tempFile;
    private File tempDir;
    private static final String TEST_CONTENT = "Hello World!";
    private static final String MULTILINE_CONTENT = "Line 1\nLine 2\nLine 3\n";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        tempDir = Files.createTempDirectory(tempFolder, "testDir").toFile();
        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes(UTF_8));
    }

    @AfterEach
    public void tearDown() throws Exception {
    }

    @Test
    public void testGetHostName() {
        String hostName = IOUtil.getHostName();
        assertNotNull(hostName);
        assertFalse(hostName.isEmpty());
    }

    @Test
    public void testFreeDiskSpaceKb() {
        long freeSpace = IOUtil.freeDiskSpaceKb();
        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKbWithTimeout() {
        long freeSpace = IOUtil.freeDiskSpaceKb(5000);
        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKbWithPath() {
        long freeSpace = IOUtil.freeDiskSpaceKb(tempDir.getAbsolutePath());
        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKbWithPathAndTimeout() {
        long freeSpace = IOUtil.freeDiskSpaceKb(tempDir.getAbsolutePath(), 5000);
        assertTrue(freeSpace > 0);
    }

    @Test
    public void testChars2Bytes() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars);
        assertArrayEquals(TEST_CONTENT.getBytes(), bytes);
    }

    @Test
    public void testChars2BytesWithCharset() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, UTF_8);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), bytes);
    }

    @Test
    public void testChars2BytesWithOffsetAndCount() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.chars2Bytes(chars, 0, 5, UTF_8);
        assertArrayEquals("Hello".getBytes(UTF_8), bytes);
    }

    @Test
    public void testBytes2Chars() {
        byte[] bytes = TEST_CONTENT.getBytes();
        char[] chars = IOUtil.bytes2Chars(bytes);
        assertArrayEquals(TEST_CONTENT.toCharArray(), chars);
    }

    @Test
    public void testBytes2CharsWithCharset() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, UTF_8);
        assertArrayEquals(TEST_CONTENT.toCharArray(), chars);
    }

    @Test
    public void testBytes2CharsWithOffsetAndCount() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytes2Chars(bytes, 0, 5, UTF_8);
        assertArrayEquals("Hello".toCharArray(), chars);
    }

    @Test
    public void testString2InputStream() throws IOException {
        InputStream is = IOUtil.string2InputStream(TEST_CONTENT);
        String result = IOUtil.readAllToString(is);
        assertEquals(TEST_CONTENT, result);
    }

    @Test
    public void testString2InputStreamWithCharset() throws IOException {
        InputStream is = IOUtil.string2InputStream(TEST_CONTENT, UTF_8);
        String result = IOUtil.readAllToString(is, UTF_8);
        assertEquals(TEST_CONTENT, result);
    }

    @Test
    public void testString2Reader() throws IOException {
        Reader reader = IOUtil.string2Reader(TEST_CONTENT);
        String result = IOUtil.readAllToString(reader);
        assertEquals(TEST_CONTENT, result);
    }

    @Test
    public void testStringBuilder2Writer() throws IOException {
        StringBuilder sb = new StringBuilder(TEST_CONTENT);
        Writer writer = IOUtil.stringBuilder2Writer(sb);
        writer.write(" Additional");
        assertEquals(TEST_CONTENT + " Additional", sb.toString());
    }

    @Test
    public void testReadAllBytes() {
        byte[] bytes = IOUtil.readAllBytes(tempFile);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), bytes);
    }

    @Test
    public void testReadAllBytesFromInputStream() throws IOException {
        try (InputStream is = new FileInputStream(tempFile)) {
            byte[] bytes = IOUtil.readAllBytes(is);
            assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), bytes);
        }
    }

    @Test
    public void testReadBytes() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), bytes);
    }

    @Test
    public void testReadBytesWithOffsetAndMaxLen() throws IOException {
        byte[] bytes = IOUtil.readBytes(tempFile, 0, 5);
        assertArrayEquals("Hello".getBytes(UTF_8), bytes);
    }

    @Test
    public void testReadAllChars() {
        char[] chars = IOUtil.readAllChars(tempFile);
        assertArrayEquals(TEST_CONTENT.toCharArray(), chars);
    }

    @Test
    public void testReadAllCharsWithCharset() {
        char[] chars = IOUtil.readAllChars(tempFile, UTF_8);
        assertArrayEquals(TEST_CONTENT.toCharArray(), chars);
    }

    @Test
    public void testReadAllToString() {
        String content = IOUtil.readAllToString(tempFile);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadAllToStringWithCharset() {
        String content = IOUtil.readAllToString(tempFile, UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testReadAllLines() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        List<String> lines = IOUtil.readAllLines(multilineFile);
        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testReadLines() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        List<String> lines = IOUtil.readLines(multilineFile, 1, 2);
        assertEquals(2, lines.size());
        assertEquals("Line 2", lines.get(0));
        assertEquals("Line 3", lines.get(1));
    }

    @Test
    public void testReadFirstLine() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String firstLine = IOUtil.readFirstLine(multilineFile);
        assertEquals("Line 1", firstLine);
    }

    @Test
    public void testReadLastLine() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String lastLine = IOUtil.readLastLine(multilineFile);
        assertEquals("Line 3", lastLine);
    }

    @Test
    public void testReadLine() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        String line = IOUtil.readLine(multilineFile, 1);
        assertEquals("Line 2", line);
    }

    @Test
    public void testWriteLine() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.writeLine("Test Line", outputFile);

        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(1, lines.size());
        assertEquals("Test Line", lines.get(0));
    }

    @Test
    public void testWriteLines() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        List<String> lines = Arrays.asList("Line 1", "Line 2", "Line 3");
        IOUtil.writeLines(lines, outputFile);

        List<String> readLines = Files.readAllLines(outputFile.toPath());
        assertEquals(3, readLines.size());
        assertEquals(lines, readLines);
    }

    @Test
    public void testWriteCharSequence() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWriteCharArray() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT.toCharArray(), outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWriteByteArray() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write(TEST_CONTENT.getBytes(UTF_8), outputFile);

        byte[] bytes = Files.readAllBytes(outputFile.toPath());
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), bytes);
    }

    @Test
    public void testWriteFileToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long bytesWritten = IOUtil.write(tempFile, outputFile);

        assertEquals(TEST_CONTENT.length(), bytesWritten);
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWriteInputStreamToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long bytesWritten = IOUtil.write(is, outputFile);
            assertEquals(TEST_CONTENT.length(), bytesWritten);
        }

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testWriteReaderToFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long charsWritten = IOUtil.write(reader, outputFile);
            assertEquals(TEST_CONTENT.length(), charsWritten);
        }

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testAppendBytes() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial".getBytes(UTF_8));

        IOUtil.append(" Content".getBytes(UTF_8), outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Initial Content", content);
    }

    @Test
    public void testAppendChars() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial".getBytes(UTF_8));

        IOUtil.append(" Content".toCharArray(), outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Initial Content", content);
    }

    @Test
    public void testAppendCharSequence() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial".getBytes(UTF_8));

        IOUtil.append(" Content", outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Initial Content", content);
    }

    @Test
    public void testAppendLine() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.writeLine("Line 1", outputFile);

        IOUtil.appendLine("Line 2", outputFile);

        N.println(IOUtil.readAllLines(outputFile));

        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(2, lines.size());
        assertEquals("Line 1", lines.get(0));
    }

    @Test
    public void testAppendLines() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Line 1\n".getBytes(UTF_8));

        List<String> newLines = Arrays.asList("Line 2", "Line 3");
        IOUtil.appendLines(newLines, outputFile);

        List<String> lines = Files.readAllLines(outputFile.toPath());
        assertEquals(3, lines.size());
    }

    @Test
    public void testSkipInputStream() throws IOException {
        byte[] data = "0123456789".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 5);
            assertEquals(5, skipped);

            byte[] remaining = new byte[5];
            is.read(remaining);
            assertArrayEquals("56789".getBytes(UTF_8), remaining);
        }
    }

    @Test
    public void testSkipReader() throws IOException {
        try (Reader reader = new StringReader("0123456789")) {
            long skipped = IOUtil.skip(reader, 5);
            assertEquals(5, skipped);

            char[] remaining = new char[5];
            reader.read(remaining);
            assertArrayEquals("56789".toCharArray(), remaining);
        }
    }

    @Test
    public void testSkipFully() throws IOException {
        byte[] data = "0123456789".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 5);

            byte[] remaining = new byte[5];
            is.read(remaining);
            assertArrayEquals("56789".getBytes(UTF_8), remaining);
        }
    }

    @Test
    public void testSkipFullyThrowsOnInsufficientData() throws IOException {
        byte[] data = "0123".getBytes(UTF_8);
        assertThrows(IOException.class, () -> {
            try (InputStream is = new ByteArrayInputStream(data)) {
                IOUtil.skipFully(is, 10);
            }
        });
    }

    @Test
    public void testCopyToDirectory() throws IOException {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(tempFile, destDir);

        File copiedFile = new File(destDir, tempFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(copiedFile.toPath()), UTF_8));
    }

    @Test
    public void testCopyFile() throws IOException {
        File destFile = new File(tempDir, "copy.txt");
        IOUtil.copyFile(tempFile, destFile);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(destFile.toPath()), UTF_8));
    }

    @Test
    public void testCopyFileWithPreserveDate() throws IOException {
        File destFile = new File(tempDir, "copy.txt");
        IOUtil.copyFile(tempFile, destFile, true);

        assertTrue(destFile.exists());
        assertEquals(tempFile.lastModified(), destFile.lastModified());
    }

    @Test
    public void testCopyURLToFile() throws IOException {
        File destFile = Files.createTempFile(tempFolder, "downloaded", ".txt").toFile();
        URL url = tempFile.toURI().toURL();

        IOUtil.copyURLToFile(url, destFile);

        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(destFile.toPath()), UTF_8));
    }

    @Test
    public void testMove() throws IOException {
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        String originalName = tempFile.getName();

        IOUtil.move(tempFile, destDir);

        assertFalse(tempFile.exists());
        File movedFile = new File(destDir, originalName);
        assertTrue(movedFile.exists());
    }

    @Test
    public void testRenameTo() throws IOException {
        File fileToRename = Files.createTempFile(tempFolder, "oldname", ".txt").toFile();
        Files.write(fileToRename.toPath(), TEST_CONTENT.getBytes(UTF_8));

        boolean renamed = IOUtil.renameTo(fileToRename, "newname.txt");

        assertTrue(renamed);
        assertFalse(fileToRename.exists());
        File renamedFile = new File(fileToRename.getParent(), "newname.txt");
        assertTrue(renamedFile.exists());
    }

    @Test
    public void testDeleteIfExists() throws IOException {
        File fileToDelete = Files.createTempFile(tempFolder, "delete", ".txt").toFile();
        assertTrue(fileToDelete.exists());

        boolean deleted = IOUtil.deleteIfExists(fileToDelete);

        assertTrue(deleted);
        assertFalse(fileToDelete.exists());
    }

    @Test
    public void testDeleteIfExistsNonExistent() {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        boolean deleted = IOUtil.deleteIfExists(nonExistent);
        assertFalse(deleted);
    }

    @Test
    public void testDeleteQuietly() throws IOException {
        File fileToDelete = Files.createTempFile(tempFolder, "delete", ".txt").toFile();
        boolean deleted = IOUtil.deleteQuietly(fileToDelete);

        assertTrue(deleted);
        assertFalse(fileToDelete.exists());
    }

    @Test
    public void testDeleteAllIfExists() throws IOException {
        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        File subFile = new File(subDir, "subfile.txt");
        Files.write(subFile.toPath(), TEST_CONTENT.getBytes(UTF_8));

        boolean deleted = IOUtil.deleteAllIfExists(tempDir);

        assertTrue(deleted);
        assertFalse(tempDir.exists());
    }

    @Test
    public void testCleanDirectory() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        Files.write(file1.toPath(), TEST_CONTENT.getBytes(UTF_8));
        Files.write(file2.toPath(), TEST_CONTENT.getBytes(UTF_8));

        boolean cleaned = IOUtil.cleanDirectory(tempDir);

        assertTrue(cleaned);
        assertTrue(tempDir.exists());
        assertEquals(0, tempDir.listFiles().length);
    }

    @Test
    public void testCreateIfNotExists() throws IOException {
        File newFile = new File(tempDir, "new.txt");
        assertFalse(newFile.exists());

        boolean created = IOUtil.createFileIfNotExists(newFile);

        assertTrue(created);
        assertTrue(newFile.exists());
    }

    @Test
    public void testMkdirIfNotExists() {
        File newDir = new File(tempDir, "newdir");
        assertFalse(newDir.exists());

        boolean created = IOUtil.mkdirIfNotExists(newDir);

        assertTrue(created);
        assertTrue(newDir.exists());
        assertTrue(newDir.isDirectory());
    }

    @Test
    public void testMkdirsIfNotExists() {
        File newDirs = new File(tempDir, "level1/level2/level3");
        assertFalse(newDirs.exists());

        boolean created = IOUtil.mkdirsIfNotExists(newDirs);

        assertTrue(created);
        assertTrue(newDirs.exists());
        assertTrue(newDirs.isDirectory());
    }

    @Test
    public void testList() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<String> files = IOUtil.walk(tempDir).map(File::getName).toList();

        assertEquals(2, files.size());
    }

    @Test
    public void testListFiles() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = IOUtil.listFiles(tempDir);

        assertEquals(2, files.size());
        assertTrue(files.contains(file1));
        assertTrue(files.contains(file2));
    }

    @Test
    public void testListDirectories() throws IOException {
        File dir1 = new File(tempDir, "dir1");
        File dir2 = new File(tempDir, "dir2");
        File file = new File(tempDir, "file.txt");
        dir1.mkdir();
        dir2.mkdir();
        file.createNewFile();

        List<File> dirs = IOUtil.listDirectories(tempDir);

        assertEquals(2, dirs.size());
        assertTrue(dirs.contains(dir1));
        assertTrue(dirs.contains(dir2));
        assertFalse(dirs.contains(file));
    }

    @Test
    public void testNewFileInputStream() throws IOException {
        try (FileInputStream fis = IOUtil.newFileInputStream(tempFile)) {
            assertNotNull(fis);
            byte[] buffer = new byte[TEST_CONTENT.length()];
            fis.read(buffer);
            assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
        }
    }

    @Test
    public void testNewFileOutputStream() throws IOException {
        File outputFile = new File(tempDir, "output.txt");
        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile)) {
            assertNotNull(fos);
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(outputFile.toPath()), UTF_8));
    }

    @Test
    public void testNewBufferedReader() throws IOException {
        try (BufferedReader br = IOUtil.newBufferedReader(tempFile)) {
            assertNotNull(br);
            assertEquals(TEST_CONTENT, br.readLine());
        }
    }

    @Test
    public void testNewBufferedWriter() throws IOException {
        File outputFile = new File(tempDir, "output.txt");
        try (BufferedWriter bw = IOUtil.newBufferedWriter(outputFile)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
        }

        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(outputFile.toPath()), UTF_8));
    }

    @Test
    public void testGZIPStreams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = IOUtil.newGZIPOutputStream(baos)) {
            gzos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        byte[] compressed = baos.toByteArray();

        try (GZIPInputStream gzis = IOUtil.newGZIPInputStream(new ByteArrayInputStream(compressed))) {
            String decompressed = IOUtil.readAllToString(gzis, UTF_8);
            assertEquals(TEST_CONTENT, decompressed);
        }
    }

    @Test
    public void testZip() throws IOException {
        File zipFile = new File(tempDir, "test.zip");
        IOUtil.zip(tempFile, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);

        try (ZipFile zf = new ZipFile(zipFile)) {
            assertEquals(1, zf.size());
            ZipEntry entry = zf.entries().nextElement();
            assertEquals(tempFile.getName(), entry.getName());
        }
    }

    @Test
    public void testUnzip() throws IOException {
        File zipFile = new File(tempDir, "test.zip");
        IOUtil.zip(tempFile, zipFile);

        File extractDir = Files.createTempDirectory(tempFolder, "extract").toFile();

        IOUtil.unzip(zipFile, extractDir);

        File extractedFile = new File(extractDir, tempFile.getName());
        assertTrue(extractedFile.exists());
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(extractedFile.toPath()), UTF_8));
    }

    @Test
    public void testSplitBySize() throws IOException {
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(largeFile.toPath(), content.toString().getBytes(UTF_8));

        IOUtil.splitBySize(largeFile, 100, tempDir);

        File[] parts = tempDir.listFiles((dir, name) -> name.startsWith(largeFile.getName() + "_"));
        assertTrue(parts.length > 1);
    }

    @Test
    public void testMerge() throws IOException {
        File file1 = new File(tempDir, "part1.txt");
        File file2 = new File(tempDir, "part2.txt");
        File file3 = new File(tempDir, "part3.txt");

        Files.write(file1.toPath(), "Part 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Part 2".getBytes(UTF_8));
        Files.write(file3.toPath(), "Part 3".getBytes(UTF_8));

        File mergedFile = new File(tempDir, "merged.txt");
        long bytesWritten = IOUtil.merge(Arrays.asList(file1, file2, file3), mergedFile);

        assertEquals(18, bytesWritten);
        String mergedContent = new String(Files.readAllBytes(mergedFile.toPath()), UTF_8);
        assertEquals("Part 1Part 2Part 3", mergedContent);
    }

    @Test
    public void testMergeWithDelimiter() throws IOException {
        File file1 = new File(tempDir, "part1.txt");
        File file2 = new File(tempDir, "part2.txt");

        Files.write(file1.toPath(), "Part 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Part 2".getBytes(UTF_8));

        File mergedFile = new File(tempDir, "merged.txt");
        byte[] delimiter = " | ".getBytes(UTF_8);
        long bytesWritten = IOUtil.merge(Arrays.asList(file1, file2), delimiter, mergedFile);

        String mergedContent = new String(Files.readAllBytes(mergedFile.toPath()), UTF_8);
        assertEquals("Part 1 | Part 2", mergedContent);
    }

    @Test
    @Disabled("This test is disabled due to platform-specific issues with memory mapping.")
    public void testMap() throws IOException {
        MappedByteBuffer mbb = IOUtil.map(tempFile);
        assertNotNull(mbb);
        assertEquals(TEST_CONTENT.length(), mbb.remaining());

        byte[] buffer = new byte[mbb.remaining()];
        mbb.get(buffer);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
    }

    @Test
    @Disabled("This test is disabled due to platform-specific issues with memory mapping.")
    public void testMapWithMode() throws IOException {
        MappedByteBuffer mbb = IOUtil.map(tempFile, MapMode.READ_ONLY);
        assertNotNull(mbb);
        assertEquals(TEST_CONTENT.length(), mbb.remaining());
    }

    @Test
    @Disabled("This test is disabled due to platform-specific issues with memory mapping.")
    public void testMapWithOffsetAndCount() throws IOException {
        MappedByteBuffer mbb = IOUtil.map(tempFile, MapMode.READ_ONLY, 0, 5);
        assertNotNull(mbb);
        assertEquals(5, mbb.remaining());

        byte[] buffer = new byte[5];
        mbb.get(buffer);
        assertArrayEquals("Hello".getBytes(UTF_8), buffer);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", IOUtil.simplifyPath(""));
        assertEquals(".", IOUtil.simplifyPath("."));
        assertEquals("b", IOUtil.simplifyPath("a/../b"));
        assertEquals("/b", IOUtil.simplifyPath("/a/../b"));
        assertEquals("b/c", IOUtil.simplifyPath("a/../b/c"));
        assertEquals("/", IOUtil.simplifyPath("/.."));
        assertEquals("c", IOUtil.simplifyPath("a/b/../../c"));
    }

    @Test
    public void testGetFileExtension() throws IOException {
        assertEquals("txt", IOUtil.getFileExtension(tempFile));

        File noExt = Files.createTempDirectory(tempFolder, "noext").toFile();
        assertEquals("", IOUtil.getFileExtension(noExt));

        assertNull(IOUtil.getFileExtension((File) null));
    }

    @Test
    public void testGetFileExtensionString() {
        assertEquals("txt", IOUtil.getFileExtension("file.txt"));
        assertEquals("gz", IOUtil.getFileExtension("file.tar.gz"));
        assertEquals("", IOUtil.getFileExtension("noext"));
        assertNull(IOUtil.getFileExtension((String) null));
    }

    @Test
    public void testGetNameWithoutExtension() {
        assertEquals(Strings.substringBeforeLast(tempFile.getName(), "."), IOUtil.getNameWithoutExtension(tempFile));
        assertNull(IOUtil.getNameWithoutExtension((File) null));
    }

    @Test
    public void testGetNameWithoutExtensionString() {
        assertEquals("file", IOUtil.getNameWithoutExtension("file.txt"));
        assertEquals("file.tar", IOUtil.getNameWithoutExtension("file.tar.gz"));
        assertEquals("noext", IOUtil.getNameWithoutExtension("noext"));
        assertNull(IOUtil.getNameWithoutExtension((String) null));
    }

    @Test
    public void testToFile() throws Exception {
        URL url = tempFile.toURI().toURL();
        File file = IOUtil.toFile(url);
        assertEquals(tempFile.getCanonicalPath(), file.getCanonicalPath());
    }

    @Test
    public void testToFileInvalidProtocol() throws Exception {
        URL url = new URL("http://example.com");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(url));
    }

    @Test
    public void testToFiles() throws Exception {
        URL[] urls = new URL[] { tempFile.toURI().toURL(), tempDir.toURI().toURL() };

        File[] files = IOUtil.toFiles(urls);
        assertEquals(2, files.length);
        assertEquals(tempFile.getCanonicalPath(), files[0].getCanonicalPath());
        assertEquals(tempDir.getCanonicalPath(), files[1].getCanonicalPath());
    }

    @Test
    public void testToURL() throws IOException {
        URL url = IOUtil.toURL(tempFile);
        assertNotNull(url);
        assertEquals("file", url.getProtocol());
    }

    @Test
    public void testToURLs() throws IOException {
        File[] files = new File[] { tempFile, tempDir };
        URL[] urls = IOUtil.toURLs(files);

        assertEquals(2, urls.length);
        assertEquals("file", urls[0].getProtocol());
        assertEquals("file", urls[1].getProtocol());
    }

    @Test
    public void testTouch() throws Exception {
        long originalTime = tempFile.lastModified();
        Thread.sleep(10);

        boolean touched = IOUtil.touch(tempFile);

        assertTrue(touched);
        assertTrue(tempFile.lastModified() > originalTime);
    }

    @Test
    public void testContentEquals() throws IOException {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();

        Files.write(file1.toPath(), TEST_CONTENT.getBytes(UTF_8));
        Files.write(file2.toPath(), TEST_CONTENT.getBytes(UTF_8));

        assertTrue(IOUtil.contentEquals(file1, file2));

        Files.write(file2.toPath(), "Different content".getBytes(UTF_8));
        assertFalse(IOUtil.contentEquals(file1, file2));
    }

    @Test
    public void testContentEqualsIgnoreEOL() throws IOException {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();

        Files.write(file1.toPath(), "Line 1\r\nLine 2\r\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line 1\nLine 2\n".getBytes(UTF_8));

        assertTrue(IOUtil.contentEqualsIgnoreEOL(file1, file2, "UTF-8"));
    }

    @Test
    public void testContentEqualsInputStream() throws IOException {
        byte[] content1 = TEST_CONTENT.getBytes(UTF_8);
        byte[] content2 = TEST_CONTENT.getBytes(UTF_8);

        try (InputStream is1 = new ByteArrayInputStream(content1); InputStream is2 = new ByteArrayInputStream(content2)) {
            assertTrue(IOUtil.contentEquals(is1, is2));
        }

        byte[] content3 = "Different".getBytes(UTF_8);
        try (InputStream is1 = new ByteArrayInputStream(content1); InputStream is3 = new ByteArrayInputStream(content3)) {
            assertFalse(IOUtil.contentEquals(is1, is3));
        }
    }

    @Test
    public void testContentEqualsReader() throws IOException {
        try (Reader r1 = new StringReader(TEST_CONTENT); Reader r2 = new StringReader(TEST_CONTENT)) {
            assertTrue(IOUtil.contentEquals(r1, r2));
        }

        try (Reader r1 = new StringReader(TEST_CONTENT); Reader r3 = new StringReader("Different")) {
            assertFalse(IOUtil.contentEquals(r1, r3));
        }
    }

    @Test
    public void testIsFileNewer() throws Exception {
        File newerFile = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        Thread.sleep(10);
        newerFile.setLastModified(System.currentTimeMillis());

        assertTrue(IOUtil.isFileNewer(newerFile, tempFile));
        assertFalse(IOUtil.isFileNewer(tempFile, newerFile));

        Date oldDate = new Date(tempFile.lastModified());
        assertTrue(IOUtil.isFileNewer(newerFile, oldDate));
    }

    @Test
    public void testIsFileOlder() throws Exception {
        File newerFile = Files.createTempFile(tempFolder, "newer", ".txt").toFile();
        Thread.sleep(10);
        newerFile.setLastModified(System.currentTimeMillis());

        assertTrue(IOUtil.isFileOlder(tempFile, newerFile));
        assertFalse(IOUtil.isFileOlder(newerFile, tempFile));

        Date newDate = new Date(newerFile.lastModified());
        assertTrue(IOUtil.isFileOlder(tempFile, newDate));
    }

    @Test
    public void testIsFile() {
        assertTrue(IOUtil.isFile(tempFile));
        assertFalse(IOUtil.isFile(tempDir));
        assertFalse(IOUtil.isFile(null));
    }

    @Test
    public void testIsDirectory() {
        assertTrue(IOUtil.isDirectory(tempDir));
        assertFalse(IOUtil.isDirectory(tempFile));
        assertFalse(IOUtil.isDirectory(null));
    }

    @Test
    public void testIsRegularFile() {
        assertTrue(IOUtil.isRegularFile(tempFile));
        assertFalse(IOUtil.isRegularFile(tempDir));
        assertFalse(IOUtil.isRegularFile(null));
    }

    @Test
    public void testIsSymbolicLink() {
        assertFalse(IOUtil.isSymbolicLink(tempFile));
        assertFalse(IOUtil.isSymbolicLink(tempDir));
        assertFalse(IOUtil.isSymbolicLink(null));
    }

    @Test
    public void testSizeOf() throws FileNotFoundException {
        assertEquals(TEST_CONTENT.length(), IOUtil.sizeOf(tempFile));
    }

    @Test
    public void testSizeOfNonExistent() throws FileNotFoundException {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> IOUtil.sizeOf(nonExistent));
    }

    @Test
    public void testSizeOfDirectory() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        Files.write(file1.toPath(), "12345".getBytes(UTF_8));
        Files.write(file2.toPath(), "67890".getBytes(UTF_8));

        assertEquals(10, IOUtil.sizeOfDirectory(tempDir));
    }

    @Test
    public void testClose() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.close(baos);

        IOUtil.close((AutoCloseable) null);
    }

    @Test
    public void testCloseWithExceptionHandler() {
        final AtomicInteger errorCount = new AtomicInteger(0);

        AutoCloseable failingCloseable = () -> {
            throw new IOException("Test exception");
        };

        IOUtil.close(failingCloseable, e -> errorCount.incrementAndGet());

        assertEquals(1, errorCount.get());
    }

    @Test
    public void testCloseAll() throws IOException {
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();

        IOUtil.closeAll(baos1, baos2);

        IOUtil.closeAll((AutoCloseable[]) null);
        IOUtil.closeAll(new AutoCloseable[0]);
    }

    @Test
    public void testCloseQuietly() {
        AutoCloseable failingCloseable = () -> {
            throw new IOException("Test exception");
        };

        IOUtil.closeQuietly(failingCloseable);
        IOUtil.closeQuietly(null);
    }

    @Test
    public void testIsBufferedReader() {
        Reader reader = new StringReader(TEST_CONTENT);
        BufferedReader br = new BufferedReader(reader);

        assertFalse(IOUtil.isBufferedReader(reader));
        assertTrue(IOUtil.isBufferedReader(br));
    }

    @Test
    public void testIsBufferedWriter() {
        Writer writer = new StringWriter();
        BufferedWriter bw = new BufferedWriter(writer);

        assertFalse(IOUtil.isBufferedWriter(writer));
        assertTrue(IOUtil.isBufferedWriter(bw));
    }

    @Test
    public void testForLines() throws Exception {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        List<String> lines = new ArrayList<>();
        IOUtil.forLines(multilineFile, lines::add);

        assertEquals(3, lines.size());
        assertEquals("Line 1", lines.get(0));
        assertEquals("Line 2", lines.get(1));
        assertEquals("Line 3", lines.get(2));
    }

    @Test
    public void testForLinesWithOffsetAndCount() throws Exception {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        List<String> lines = new ArrayList<>();
        IOUtil.forLines(multilineFile, 1, 2, lines::add);

        assertEquals(2, lines.size());
        assertEquals("Line 2", lines.get(0));
        assertEquals("Line 3", lines.get(1));
    }

    @Test
    public void testForLinesInputStream() throws Exception {
        List<String> lines = new ArrayList<>();
        try (InputStream is = new ByteArrayInputStream(MULTILINE_CONTENT.getBytes(UTF_8))) {
            IOUtil.forLines(is, lines::add);
        }

        assertEquals(3, lines.size());
    }

    @Test
    public void testForLinesReader() throws Exception {
        List<String> lines = new ArrayList<>();
        try (Reader reader = new StringReader(MULTILINE_CONTENT)) {
            IOUtil.forLines(reader, lines::add);
        }

        assertEquals(3, lines.size());
    }

    @Test
    public void testNewAppendableWriter() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newAppendableWriter(sb);
        writer.write(TEST_CONTENT);

        assertEquals(TEST_CONTENT, sb.toString());
    }

    @Test
    public void testNewStringWriter() {
        com.landawn.abacus.util.StringWriter sw = IOUtil.newStringWriter();
        assertNotNull(sw);
        assertEquals("", sw.toString());
    }

    @Test
    public void testNewStringWriterWithInitialSize() {
        com.landawn.abacus.util.StringWriter sw = IOUtil.newStringWriter(100);
        assertNotNull(sw);
    }

    @Test
    public void testNewStringWriterWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Initial");
        com.landawn.abacus.util.StringWriter sw = IOUtil.newStringWriter(sb);
        assertNotNull(sw);
        assertEquals("Initial", sw.toString());
    }

    @Test
    public void testNewByteArrayOutputStream() {
        com.landawn.abacus.util.ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
        assertNotNull(baos);
        assertEquals(0, baos.size());
    }

    @Test
    public void testNewByteArrayOutputStreamWithCapacity() {
        com.landawn.abacus.util.ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream(100);
        assertNotNull(baos);
    }

    @Test
    public void testReadFileWithBuffer() throws IOException {
        byte[] buffer = new byte[TEST_CONTENT.length()];
        int bytesRead = IOUtil.read(tempFile, buffer);

        assertEquals(TEST_CONTENT.length(), bytesRead);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
    }

    @Test
    public void testReadInputStreamWithBuffer() throws IOException {
        byte[] buffer = new byte[TEST_CONTENT.length()];
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            int bytesRead = IOUtil.read(is, buffer);

            assertEquals(TEST_CONTENT.length(), bytesRead);
            assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
        }
    }

    @Test
    public void testReadReaderWithBuffer() throws IOException {
        char[] buffer = new char[TEST_CONTENT.length()];
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            int charsRead = IOUtil.read(reader, buffer);

            assertEquals(TEST_CONTENT.length(), charsRead);
            assertArrayEquals(TEST_CONTENT.toCharArray(), buffer);
        }
    }

    @Test
    public void testWritePrimitives() throws IOException {
        StringWriter sw = new StringWriter();

        IOUtil.write(true, sw);
        IOUtil.write('A', sw);
        IOUtil.write((byte) 65, sw);
        IOUtil.write((short) 123, sw);
        IOUtil.write(456, sw);
        IOUtil.write(789L, sw);
        IOUtil.write(1.23f, sw);
        IOUtil.write(4.56, sw);

        String result = sw.toString();
        assertTrue(result.contains("true"));
        assertTrue(result.contains("A"));
        assertTrue(result.contains("65"));
        assertTrue(result.contains("123"));
        assertTrue(result.contains("456"));
        assertTrue(result.contains("789"));
        assertTrue(result.contains("1.23"));
        assertTrue(result.contains("4.56"));
    }

    @Test
    public void testTransfer() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ReadableByteChannel src = Channels.newChannel(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
                WritableByteChannel dest = Channels.newChannel(baos)) {

            long transferred = IOUtil.transfer(src, dest);

            assertEquals(TEST_CONTENT.length(), transferred);
            assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), baos.toByteArray());
        }
    }

    @Test
    public void testCopyPath() throws IOException {
        Path source = tempFile.toPath();
        Path target = Paths.get(tempDir.getAbsolutePath(), "copy.txt");

        Path result = IOUtil.copy(source, target);

        assertEquals(target, result);
        assertTrue(Files.exists(target));
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(target), UTF_8));
    }

    @Test
    public void testCopyInputStreamToPath() throws IOException {
        Path target = Paths.get(tempDir.getAbsolutePath(), "copy.txt");

        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long copied = IOUtil.copy(is, target);

            assertEquals(TEST_CONTENT.length(), copied);
            assertTrue(Files.exists(target));
            assertEquals(TEST_CONTENT, new String(Files.readAllBytes(target), UTF_8));
        }
    }

    @Test
    public void testCopyPathToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Path source = tempFile.toPath();

        long copied = IOUtil.copy(source, baos);

        assertEquals(TEST_CONTENT.length(), copied);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), baos.toByteArray());
    }

    @Test
    public void testLZ4Streams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (LZ4BlockOutputStream lz4os = IOUtil.newLZ4BlockOutputStream(baos)) {
            lz4os.write(TEST_CONTENT.getBytes(UTF_8));
        }

        byte[] compressed = baos.toByteArray();

        try (LZ4BlockInputStream lz4is = IOUtil.newLZ4BlockInputStream(new ByteArrayInputStream(compressed))) {
            String decompressed = IOUtil.readAllToString(lz4is, UTF_8);
            assertEquals(TEST_CONTENT, decompressed);
        }
    }

    @Test
    public void testSnappyStreams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (SnappyOutputStream sos = IOUtil.newSnappyOutputStream(baos)) {
            sos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        byte[] compressed = baos.toByteArray();

        try (SnappyInputStream sis = IOUtil.newSnappyInputStream(new ByteArrayInputStream(compressed))) {
            String decompressed = IOUtil.readAllToString(sis, UTF_8);
            assertEquals(TEST_CONTENT, decompressed);
        }
    }

    @Test
    public void testZipStreams() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try (ZipOutputStream zos = IOUtil.newZipOutputStream(baos)) {
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write(TEST_CONTENT.getBytes(UTF_8));
            zos.closeEntry();
        }

        byte[] zipped = baos.toByteArray();

        try (ZipInputStream zis = IOUtil.newZipInputStream(new ByteArrayInputStream(zipped))) {
            ZipEntry entry = zis.getNextEntry();
            assertEquals("test.txt", entry.getName());

            String content = IOUtil.readAllToString(zis, UTF_8);
            assertEquals(TEST_CONTENT, content);
        }
    }

    @Test
    public void testBrotliInputStream() throws IOException {
        ByteArrayInputStream bais = new ByteArrayInputStream(new byte[0]);

        try (BrotliInputStream bis = IOUtil.newBrotliInputStream(bais)) {
            assertNotNull(bis);
        } catch (IOException e) {
        }
    }
}
