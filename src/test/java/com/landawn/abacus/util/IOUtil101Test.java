package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiPredicate;

@Tag("new-test")
public class IOUtil101Test extends TestBase {

    @TempDir
    Path tempFolder;

    private File tempFile;
    private File tempDir;
    private static final String TEST_CONTENT = "Hello World!";
    private static final String MULTILINE_CONTENT = "Line 1\nLine 2\nLine 3\nLine 4\nLine 5\n";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        tempDir = Files.createTempDirectory(tempFolder, "testDir").toFile();
        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes(UTF_8));
    }

    @Test
    public void testReadBytesFromInputStreamWithOffset() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            byte[] result = IOUtil.readBytes(is, 5, 5);
            assertArrayEquals("56789".getBytes(UTF_8), result);
        }
    }

    @Test
    public void testReadBytesWithZeroLength() throws IOException {
        byte[] result = IOUtil.readBytes(tempFile, 0, 0);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadBytesWithOffsetBeyondFile() throws IOException {
        byte[] result = IOUtil.readBytes(tempFile, 1000, 10);
        assertEquals(0, result.length);
    }

    @Test
    public void testReadCharsFromInputStreamWithOffset() throws IOException {
        String data = "0123456789ABCDEF";
        try (InputStream is = new ByteArrayInputStream(data.getBytes(UTF_8))) {
            char[] result = IOUtil.readChars(is, 5, 5);
            assertArrayEquals("56789".toCharArray(), result);
        }
    }

    @Test
    public void testReadCharsFromFileWithOffset() throws IOException {
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), "0123456789ABCDEF".getBytes(UTF_8));

        char[] result = IOUtil.readChars(largeFile, 5, 5);
        assertArrayEquals("56789".toCharArray(), result);
    }

    @Test
    public void testReadToString() throws IOException {
        String result = IOUtil.readToString(tempFile, 6, 5);
        assertEquals("World", result);
    }

    @Test
    public void testReadToStringFromInputStream() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            String result = IOUtil.readToString(is, 6, 5);
            assertEquals("World", result);
        }
    }

    @Test
    public void testReadToStringFromReader() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            String result = IOUtil.readToString(reader, 6, 5);
            assertEquals("World", result);
        }
    }

    @Test
    public void testWriteCharSequenceWithCharset() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        IOUtil.write("Test äöü", StandardCharsets.UTF_8, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("Test äöü", content);
    }

    @Test
    public void testWriteCharSequenceToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.write("Test Content", baos);

        assertEquals("Test Content", baos.toString());
    }

    @Test
    public void testWriteCharSequenceToOutputStreamWithFlush() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.write("Test Content", baos, true);

        assertEquals("Test Content", baos.toString());
    }

    @Test
    public void testWriteCharSequenceToWriter() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.write("Test Content", sw);

        assertEquals("Test Content", sw.toString());
    }

    @Test
    public void testWriteCharSequenceToWriterWithFlush() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.write("Test Content", sw, true);

        assertEquals("Test Content", sw.toString());
    }

    @Test
    public void testWriteCharArrayWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        char[] chars = "Hello World!".toCharArray();
        IOUtil.write(chars, 6, 5, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("World", content);
    }

    @Test
    public void testWriteCharArrayToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        char[] chars = TEST_CONTENT.toCharArray();
        IOUtil.write(chars, baos);

        assertEquals(TEST_CONTENT, baos.toString());
    }

    @Test
    public void testWriteByteArrayWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        IOUtil.write(bytes, 6, 5, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("World", content);
    }

    @Test
    public void testWriteByteArrayToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        IOUtil.write(bytes, baos);

        assertArrayEquals(bytes, baos.toByteArray());
    }

    @Test
    public void testWriteFileToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long written = IOUtil.write(tempFile, baos);

        assertEquals(TEST_CONTENT.length(), written);
        assertEquals(TEST_CONTENT, baos.toString());
    }

    @Test
    public void testWriteFileWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        long written = IOUtil.write(tempFile, 6, 5, outputFile);

        assertEquals(5, written);
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("World", content);
    }

    @Test
    public void testWriteInputStreamWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long written = IOUtil.write(is, 6, 5, outputFile);

            assertEquals(5, written);
            String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
            assertEquals("World", content);
        }
    }

    @Test
    public void testWriteReaderWithCharset() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader("Test äöü")) {
            long written = IOUtil.write(reader, StandardCharsets.UTF_8, outputFile);

            assertEquals(8, written);
            String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
            assertEquals("Test äöü", content);
        }
    }

    @Test
    public void testWriteReaderWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long written = IOUtil.write(reader, 6, 5, outputFile);

            assertEquals(5, written);
            String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
            assertEquals("World", content);
        }
    }

    @Test
    public void testWriteReaderToWriter() throws IOException {
        StringWriter sw = new StringWriter();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long written = IOUtil.write(reader, sw);

            assertEquals(TEST_CONTENT.length(), written);
            assertEquals(TEST_CONTENT, sw.toString());
        }
    }

    @Test
    public void testWriteReaderToWriterWithOffsetAndCount() throws IOException {
        StringWriter sw = new StringWriter();
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long written = IOUtil.write(reader, 6, 5, sw);

            assertEquals(5, written);
            assertEquals("World", sw.toString());
        }
    }

    @Test
    public void testAppendBytesWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Hello ".getBytes(UTF_8));

        byte[] bytes = "World!".getBytes(UTF_8);
        IOUtil.append(bytes, 0, bytes.length, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Hello World!", content);
    }

    @Test
    public void testAppendCharsWithCharset() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Hello ".getBytes(UTF_8));

        IOUtil.append("äöü".toCharArray(), StandardCharsets.UTF_8, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
        assertEquals("Hello äöü", content);
    }

    @Test
    public void testAppendCharsWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Hello ".getBytes(UTF_8));

        char[] chars = "World!".toCharArray();
        IOUtil.append(chars, 0, chars.length, outputFile);

        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Hello World!", content);
    }

    @Test
    public void testAppendFile() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial ".getBytes(UTF_8));

        long appended = IOUtil.append(tempFile, outputFile);

        assertEquals(TEST_CONTENT.length(), appended);
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Initial " + TEST_CONTENT, content);
    }

    @Test
    public void testAppendFileWithOffsetAndCount() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial ".getBytes(UTF_8));

        long appended = IOUtil.append(tempFile, 6, 5, outputFile);

        assertEquals(5, appended);
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("Initial World", content);
    }

    @Test
    public void testAppendInputStream() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial ".getBytes(UTF_8));

        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long appended = IOUtil.append(is, outputFile);

            assertEquals(TEST_CONTENT.length(), appended);
            String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
            assertEquals("Initial " + TEST_CONTENT, content);
        }
    }

    @Test
    public void testAppendReader() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial ".getBytes(UTF_8));

        try (Reader reader = new StringReader(TEST_CONTENT)) {
            long appended = IOUtil.append(reader, outputFile);

            assertEquals(TEST_CONTENT.length(), appended);
            String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
            assertEquals("Initial " + TEST_CONTENT, content);
        }
    }

    @Test
    public void testAppendReaderWithCharset() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "Initial ".getBytes(StandardCharsets.UTF_8));

        try (Reader reader = new StringReader("äöü")) {
            long appended = IOUtil.append(reader, StandardCharsets.UTF_8, outputFile);

            assertEquals(3, appended);
            String content = new String(Files.readAllBytes(outputFile.toPath()), StandardCharsets.UTF_8);
            assertEquals("Initial äöü", content);
        }
    }

    @Test
    public void testCloseURLConnection() throws IOException {
        URL url = new URL("http://example.com");
        URLConnection conn = url.openConnection();

        if (conn instanceof HttpURLConnection) {
            ((HttpURLConnection) conn).setRequestMethod("HEAD");
        }

        IOUtil.close(conn);
    }

    @Test
    public void testCopyToDirectoryRecursive() throws IOException {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File subDir = new File(srcDir, "subdir");
        subDir.mkdir();

        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "File 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "File 2".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        IOUtil.copyToDirectory(srcDir, destDir);

        File copiedDir = new File(destDir, srcDir.getName());
        assertTrue(copiedDir.exists());
        assertTrue(copiedDir.isDirectory());

        File copiedFile1 = new File(copiedDir, "file1.txt");
        assertTrue(copiedFile1.exists());
        assertEquals("File 1", new String(Files.readAllBytes(copiedFile1.toPath()), UTF_8));

        File copiedSubDir = new File(copiedDir, "subdir");
        assertTrue(copiedSubDir.exists());

        File copiedFile2 = new File(copiedSubDir, "file2.txt");
        assertTrue(copiedFile2.exists());
        assertEquals("File 2", new String(Files.readAllBytes(copiedFile2.toPath()), UTF_8));
    }

    @Test
    public void testCopyToDirectoryWithFilter() throws IOException {
        File srcDir = Files.createTempDirectory(tempFolder, "src").toFile();
        File file1 = new File(srcDir, "file1.txt");
        File file2 = new File(srcDir, "file2.log");
        Files.write(file1.toPath(), "File 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "File 2".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();
        BiPredicate<File, File> filter = (parent, file) -> file.getName().endsWith(".txt");
        IOUtil.copyToDirectory(srcDir, destDir, true, filter);

        File copiedDir = new File(destDir, srcDir.getName());
        File copiedFile1 = new File(copiedDir, "file1.txt");
        File copiedFile2 = new File(copiedDir, "file2.log");

        assertTrue(copiedFile1.exists());
        assertFalse(copiedFile2.exists());
    }

    @Test
    public void testCopyToDirectorySourceDoesNotExist() throws IOException {
        File nonExistent = new File(tempDir, "nonexistent");
        File destDir = Files.createTempDirectory(tempFolder, "dest").toFile();

        assertThrows(FileNotFoundException.class, () -> IOUtil.copyToDirectory(nonExistent, destDir));
    }

    @Test
    public void testCopyToDirectoryDestinationIsFile() throws IOException {
        File destFile = Files.createTempFile(tempFolder, "destfile", ".txt").toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(tempFile, destFile));
    }

    @Test
    public void testCopyFileWithCopyOptions() throws IOException {
        File destFile = new File(tempDir, "copy.txt");
        IOUtil.copyFile(tempFile, destFile, StandardCopyOption.COPY_ATTRIBUTES);

        assertTrue(destFile.exists());
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(destFile.toPath()), UTF_8));
    }

    @Test
    public void testCopyFileWithPreserveDateAndOptions() throws IOException {
        File destFile = new File(tempDir, "copy.txt");
        IOUtil.copyFile(tempFile, destFile, true, StandardCopyOption.REPLACE_EXISTING);

        assertTrue(destFile.exists());
        assertEquals(tempFile.lastModified(), destFile.lastModified());
    }

    @Test
    public void testCopyFileToOutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long copied = IOUtil.copyFile(tempFile, baos);

        assertEquals(TEST_CONTENT.length(), copied);
        assertEquals(TEST_CONTENT, baos.toString());
    }

    @Test
    public void testDeleteFilesFromDirectoryWithFilter() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.log");
        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        File subFile = new File(subDir, "subfile.txt");

        Files.write(file1.toPath(), "content".getBytes(UTF_8));
        Files.write(file2.toPath(), "content".getBytes(UTF_8));
        Files.write(subFile.toPath(), "content".getBytes(UTF_8));

        BiPredicate<File, File> filter = (parent, file) -> file.getName().endsWith(".txt");
        boolean deleted = IOUtil.deleteFilesFromDirectory(tempDir, filter);

        assertTrue(deleted);
        assertFalse(file1.exists());
        assertTrue(file2.exists());
        assertTrue(subDir.exists());
        assertFalse(subFile.exists());
    }

    @Test
    public void testIsDirectoryWithLinkOptions() {
        assertTrue(IOUtil.isDirectory(tempDir, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isDirectory(tempFile, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsRegularFileWithLinkOptions() {
        assertTrue(IOUtil.isRegularFile(tempFile, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isRegularFile(tempDir, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testSizeOfWithNonExistingFileAsEmpty() throws FileNotFoundException {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        long size = IOUtil.sizeOf(nonExistent, true);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOfDirectoryWithNonExistingAsEmpty() throws FileNotFoundException {
        File nonExistentDir = new File(tempDir, "nonexistentdir");
        long size = IOUtil.sizeOfDirectory(nonExistentDir, true);
        assertEquals(0, size);
    }

    @Test
    public void testSizeOfAsBigInteger() throws FileNotFoundException {
        BigInteger size = IOUtil.sizeOfAsBigInteger(tempFile);
        assertEquals(BigInteger.valueOf(TEST_CONTENT.length()), size);
    }

    @Test
    public void testSizeOfAsBigIntegerNonExistent() throws FileNotFoundException {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        assertThrows(FileNotFoundException.class, () -> IOUtil.sizeOfAsBigInteger(nonExistent));
    }

    @Test
    public void testSizeOfDirectoryAsBigInteger() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        Files.write(file1.toPath(), "12345".getBytes(UTF_8));
        Files.write(file2.toPath(), "67890".getBytes(UTF_8));

        BigInteger size = IOUtil.sizeOfDirectoryAsBigInteger(tempDir);
        assertEquals(BigInteger.valueOf(10), size);
    }

    @Test
    public void testZipMultipleFiles() throws IOException {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();
        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        File zipFile = new File(tempDir, "test.zip");
        IOUtil.zip(Arrays.asList(file1, file2), zipFile);

        assertTrue(zipFile.exists());
        try (ZipFile zf = new ZipFile(zipFile)) {
            assertEquals(2, zf.size());

            ZipEntry entry1 = zf.getEntry(file1.getName());
            assertNotNull(entry1);

            ZipEntry entry2 = zf.getEntry(file2.getName());
            assertNotNull(entry2);
        }
    }

    @Test
    public void testZipDirectory() throws IOException {
        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        File zipFile = new File(tempFolder.toFile(), "test.zip");
        IOUtil.zip(tempDir, zipFile);

        assertTrue(zipFile.exists());
        try (ZipFile zf = new ZipFile(zipFile)) {
            assertTrue(zf.size() >= 2);
        }
    }

    @Test
    public void testSplit() throws IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            content.append("Line ").append(i).append(" with some content\n");
        }
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), content.toString().getBytes(UTF_8));

        IOUtil.split(largeFile, 3);

        File[] parts = largeFile.getParentFile().listFiles((dir, name) -> name.startsWith(largeFile.getName() + "_"));
        assertEquals(3, parts.length);
    }

    @Test
    public void testSplitToSpecificDirectory() throws IOException {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10; i++) {
            content.append("Line ").append(i).append("\n");
        }
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), content.toString().getBytes(UTF_8));

        File splitDir = Files.createTempDirectory(tempFolder, "splits").toFile();
        IOUtil.split(largeFile, 2, splitDir);

        File[] parts = splitDir.listFiles();
        assertEquals(2, parts.length);
    }

    @Test
    public void testListWithFilter() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.log");
        file1.createNewFile();
        file2.createNewFile();

        List<String> files = IOUtil.walk(tempDir, false, false).filter(it -> it.getName().endsWith(".txt")).map(File::getName).toList();

        assertEquals(1, files.size());
        assertTrue(files.get(0).endsWith("file1.txt"));
    }

    @Test
    public void testListFilesRecursivelyWithFilter() throws IOException {
        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        File file3 = new File(subDir, "file3.log");
        file1.createNewFile();
        file2.createNewFile();
        file3.createNewFile();

        BiPredicate<File, File> filter = (parent, file) -> file.getName().endsWith(".txt");
        List<File> files = IOUtil.listFiles(tempDir, true, filter);

        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(f -> f.getName().endsWith(".txt")));
    }

    @Test
    public void testForLinesWithMultipleFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();
        Files.write(file1.toPath(), "File1 Line1\nFile1 Line2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "File2 Line1\nFile2 Line2\n".getBytes(UTF_8));

        List<String> allLines = new ArrayList<>();
        IOUtil.forLines(Arrays.asList(file1, file2), allLines::add);

        assertEquals(4, allLines.size());
        assertTrue(allLines.contains("File1 Line1"));
        assertTrue(allLines.contains("File2 Line2"));
    }

    @Test
    public void testForLinesWithOffsetAndCountMultipleFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line4\nLine5\nLine6\n".getBytes(UTF_8));

        List<String> lines = new ArrayList<>();
        IOUtil.forLines(Arrays.asList(file1, file2), 2, 3, lines::add);

        assertEquals(3, lines.size());
        assertEquals("Line3", lines.get(0));
        assertEquals("Line4", lines.get(1));
        assertEquals("Line5", lines.get(2));
    }

    @Test
    public void testForLinesWithOnComplete() throws Exception {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        List<String> lines = new ArrayList<>();
        AtomicInteger completedCount = new AtomicInteger(0);

        IOUtil.forLines(multilineFile, lines::add, completedCount::incrementAndGet);

        assertEquals(5, lines.size());
        assertEquals(1, completedCount.get());
    }

    @Test
    public void testForLinesWithProcessThreads() throws Exception {
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("Line ").append(i).append("\n");
        }
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), content.toString().getBytes(UTF_8));

        List<String> lines = Collections.synchronizedList(new ArrayList<>());

        IOUtil.forLines(largeFile, 0, Long.MAX_VALUE, 2, 10, lines::add);

        assertEquals(100, lines.size());
    }

    @Test
    public void testForLinesWithReadAndProcessThreads() throws Exception {
        List<File> files = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            File file = Files.createTempFile(tempFolder, "file" + i, ".txt").toFile();
            StringBuilder content = new StringBuilder();
            for (int j = 0; j < 20; j++) {
                content.append("File").append(i).append(" Line").append(j).append("\n");
            }
            Files.write(file.toPath(), content.toString().getBytes(UTF_8));
            files.add(file);
        }

        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch completeLatch = new CountDownLatch(1);

        IOUtil.forLines(files, 0, Long.MAX_VALUE, 2, 3, 10, lines::add, completeLatch::countDown);

        completeLatch.await();
        assertEquals(100, lines.size());
    }

    @Test
    public void testNewFileReaderWithCharset() throws IOException {
        File file = Files.createTempFile(tempFolder, "utf8", ".txt").toFile();
        Files.write(file.toPath(), "Test äöü".getBytes(StandardCharsets.UTF_8));

        try (FileReader reader = IOUtil.newFileReader(file, StandardCharsets.UTF_8)) {
            char[] buffer = new char[8];
            int read = reader.read(buffer);
            assertEquals(8, read);
            assertEquals("Test äöü", new String(buffer));
        }
    }

    @Test
    public void testNewFileWriterWithAppend() throws IOException {
        File file = Files.createTempFile(tempFolder, "append", ".txt").toFile();
        Files.write(file.toPath(), "Initial".getBytes(UTF_8));

        try (FileWriter writer = IOUtil.newFileWriter(file, UTF_8, true)) {
            writer.write(" Appended");
        }

        String content = new String(Files.readAllBytes(file.toPath()), UTF_8);
        assertEquals("Initial Appended", content);
    }

    @Test
    public void testNewBufferedInputStreamWithSize() throws IOException {
        try (BufferedInputStream bis = IOUtil.newBufferedInputStream(tempFile, 1024)) {
            byte[] buffer = new byte[TEST_CONTENT.length()];
            int read = bis.read(buffer);
            assertEquals(TEST_CONTENT.length(), read);
            assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
        }
    }

    @Test
    public void testNewBufferedOutputStreamWithSize() throws IOException {
        File outputFile = new File(tempDir, "output.txt");
        try (BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile, 1024)) {
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(outputFile.toPath()), UTF_8));
    }

    @Test
    public void testNewBufferedReaderFromPath() throws IOException {
        Path path = tempFile.toPath();
        try (BufferedReader br = IOUtil.newBufferedReader(path)) {
            assertEquals(TEST_CONTENT, br.readLine());
        }
    }

    @Test
    public void testNewBufferedReaderFromPathWithCharset() throws IOException {
        File file = Files.createTempFile(tempFolder, "utf8", ".txt").toFile();
        Files.write(file.toPath(), "Test äöü".getBytes(StandardCharsets.UTF_8));

        Path path = file.toPath();
        try (BufferedReader br = IOUtil.newBufferedReader(path, StandardCharsets.UTF_8)) {
            assertEquals("Test äöü", br.readLine());
        }
    }

    @Test
    public void testNewLZ4BlockOutputStreamWithBlockSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4os = IOUtil.newLZ4BlockOutputStream(baos, 65536)) {
            lz4os.write(TEST_CONTENT.getBytes(UTF_8));
        }

        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewSnappyOutputStreamWithBufferSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (SnappyOutputStream sos = IOUtil.newSnappyOutputStream(baos, 1024)) {
            sos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewGZIPInputStreamWithBufferSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = new GZIPOutputStream(baos)) {
            gzos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (GZIPInputStream gzis = IOUtil.newGZIPInputStream(new ByteArrayInputStream(baos.toByteArray()), 512)) {
            String decompressed = IOUtil.readAllToString(gzis, UTF_8);
            assertEquals(TEST_CONTENT, decompressed);
        }
    }

    @Test
    public void testNewGZIPOutputStreamWithBufferSize() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gzos = IOUtil.newGZIPOutputStream(baos, 512)) {
            gzos.write(TEST_CONTENT.getBytes(UTF_8));
        }

        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewZipInputStreamWithCharset() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write("Test äöü".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        try (ZipInputStream zis = IOUtil.newZipInputStream(new ByteArrayInputStream(baos.toByteArray()), StandardCharsets.UTF_8)) {
            ZipEntry entry = zis.getNextEntry();
            assertEquals("test.txt", entry.getName());

            String content = IOUtil.readAllToString(zis, StandardCharsets.UTF_8);
            assertEquals("Test äöü", content);
        }
    }

    @Test
    public void testNewZipOutputStreamWithCharset() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = IOUtil.newZipOutputStream(baos, StandardCharsets.UTF_8)) {
            zos.putNextEntry(new ZipEntry("test.txt"));
            zos.write("Test äöü".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        assertTrue(baos.size() > 0);
    }

    @Test
    public void testEmptyFileOperations() throws IOException {
        File emptyFile = Files.createTempFile(tempFolder, "empty", ".txt").toFile();

        assertEquals(0, IOUtil.readAllBytes(emptyFile).length);
        assertEquals(0, IOUtil.readAllChars(emptyFile).length);
        assertEquals("", IOUtil.readAllToString(emptyFile));
        assertEquals(0, IOUtil.readAllLines(emptyFile).size());
        assertNull(IOUtil.readFirstLine(emptyFile));
        assertNull(IOUtil.readLastLine(emptyFile));

        assertEquals(0, IOUtil.sizeOf(emptyFile));
        assertEquals(BigInteger.ZERO, IOUtil.sizeOfAsBigInteger(emptyFile));
    }

    @Test
    public void testNullAndEmptyInputs() throws IOException {
        assertEquals(0, IOUtil.charsToBytes(new char[0]).length);

        assertEquals(0, IOUtil.bytesToChars(new byte[0]).length);

        File outputFile = Files.createTempFile(tempFolder, "empty_output", ".txt").toFile();
        IOUtil.write(new byte[0], outputFile);
        assertEquals(0, Files.size(outputFile.toPath()));

        IOUtil.write(new char[0], outputFile);
        assertEquals(0, Files.size(outputFile.toPath()));

        IOUtil.append(new byte[0], outputFile);
        IOUtil.append(new char[0], outputFile);
        assertEquals(0, Files.size(outputFile.toPath()));
    }

    @Test
    public void testCopyURLToFileWithTimeout() throws IOException {
        File destFile = Files.createTempFile(tempFolder, "downloaded", ".txt").toFile();
        URL url = tempFile.toURI().toURL();

        IOUtil.copyURLToFile(url, destFile, 5000, 5000);

        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(destFile.toPath()), UTF_8));
    }

    @Test
    public void testMovePath() throws IOException {
        Path source = tempFile.toPath();
        Path target = Paths.get(tempDir.getAbsolutePath(), "moved.txt");

        Path result = IOUtil.move(source, target);

        assertEquals(target, result);
        assertFalse(Files.exists(source));
        assertTrue(Files.exists(target));
    }

    @Test
    public void testSizeOfDirectoryWithSymbolicLinks() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        Files.write(file1.toPath(), "12345".getBytes(UTF_8));

        long size = IOUtil.sizeOfDirectory(tempDir);
        assertEquals(5, size);
    }

    @Test
    public void testContentEqualsWithNulls() throws IOException {
        assertTrue(IOUtil.contentEquals((File) null, (File) null));
        assertFalse(IOUtil.contentEquals(tempFile, null));
        assertFalse(IOUtil.contentEquals(null, tempFile));

        assertTrue(IOUtil.contentEquals((InputStream) null, (InputStream) null));
        assertTrue(IOUtil.contentEquals((Reader) null, (Reader) null));
    }

    @Test
    public void testContentEqualsWithSameFile() throws IOException {
        assertTrue(IOUtil.contentEquals(tempFile, tempFile));
    }

    @Test
    public void testContentEqualsNonExistentFiles() throws IOException {
        File nonExistent1 = new File(tempDir, "nonexistent1.txt");
        File nonExistent2 = new File(tempDir, "nonexistent2.txt");

        assertTrue(IOUtil.contentEquals(nonExistent1, nonExistent2));
    }

    @Test
    public void testToFilesCollection() throws IOException {
        List<URL> urls = Arrays.asList(tempFile.toURI().toURL(), tempDir.toURI().toURL());

        List<File> files = IOUtil.toFiles(urls);
        assertEquals(2, files.size());
    }

    @Test
    public void testToURLsCollection() throws IOException {
        List<File> files = Arrays.asList(tempFile, tempDir);
        List<URL> urls = IOUtil.toURLs(files);

        assertEquals(2, urls.size());
        assertEquals("file", urls.get(0).getProtocol());
        assertEquals("file", urls.get(1).getProtocol());
    }

    @Test
    public void testCheckFileExistsThrowsForDirectory() throws IOException {
        try {
            IOUtil.contentEqualsIgnoreEOL(tempDir, tempFile, "UTF-8");
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("is not a file"));
        }
    }
}
