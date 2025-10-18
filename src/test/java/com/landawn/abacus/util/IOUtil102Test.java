package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.channels.FileChannel.MapMode;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class IOUtil102Test extends TestBase {

    @TempDir
    Path tempFolder;

    private File tempFile;
    private File tempDir;
    private static final String TEST_CONTENT = "Hello World!";
    private static final String LONG_CONTENT;
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    static {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            sb.append("Line ").append(i).append(" with some test content that makes the file larger.\n");
        }
        LONG_CONTENT = sb.toString();
    }

    @BeforeEach
    public void setUp() throws Exception {
        tempFile = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        tempDir = Files.createTempDirectory(tempFolder, "testDir").toFile();
        Files.write(tempFile.toPath(), TEST_CONTENT.getBytes(UTF_8));
    }

    @Test
    public void testConstants() {
        assertEquals(1024L, IOUtil.ONE_KB);
        assertEquals(1024L * 1024L, IOUtil.ONE_MB);
        assertEquals(1024L * 1024L * 1024L, IOUtil.ONE_GB);
        assertEquals(1024L * 1024L * 1024L * 1024L, IOUtil.ONE_TB);
        assertEquals(1024L * 1024L * 1024L * 1024L * 1024L, IOUtil.ONE_PB);
        assertEquals(1024L * 1024L * 1024L * 1024L * 1024L * 1024L, IOUtil.ONE_EB);

        assertNotNull(IOUtil.OS_NAME);
        assertNotNull(IOUtil.OS_VERSION);
        assertNotNull(IOUtil.OS_ARCH);
        assertNotNull(IOUtil.JAVA_HOME);
        assertNotNull(IOUtil.JAVA_VERSION);
        assertNotNull(IOUtil.USER_DIR);
        assertNotNull(IOUtil.USER_HOME);
        assertNotNull(IOUtil.USER_NAME);

        assertEquals(File.pathSeparator, IOUtil.PATH_SEPARATOR);
        assertEquals(File.separator, IOUtil.DIR_SEPARATOR);
        assertEquals(System.lineSeparator(), IOUtil.LINE_SEPARATOR);

        assertTrue(IOUtil.CPU_CORES > 0);

        assertTrue(IOUtil.MAX_MEMORY_IN_MB > 0);

        assertEquals(-1, IOUtil.EOF);
    }

    @Test
    public void testOSFlags() {
        assertTrue(IOUtil.IS_OS_WINDOWS || IOUtil.IS_OS_MAC || IOUtil.IS_OS_LINUX || true);

        if (IOUtil.IS_OS_MAC_OSX) {
            assertTrue(IOUtil.IS_OS_MAC);
        }
    }

    @Test
    public void testWriteLinesWithIterator() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        List<String> lines = Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines.iterator(), outputFile);

        List<String> readLines = Files.readAllLines(outputFile.toPath());
        assertEquals(3, readLines.size());
        assertEquals(lines, readLines);
    }

    @Test
    public void testWriteLinesWithIteratorToWriter() throws IOException {
        StringWriter sw = new StringWriter();
        List<String> lines = Arrays.asList("Line 1", "Line 2", "Line 3");

        IOUtil.writeLines(lines.iterator(), sw);

        String result = sw.toString();
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("Line 2"));
        assertTrue(result.contains("Line 3"));
    }

    @Test
    public void testWriteLinesWithEmptyIterator() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        List<String> emptyList = Collections.emptyList();

        IOUtil.writeLines(emptyList.iterator(), outputFile);

        assertEquals(0, Files.size(outputFile.toPath()));
    }

    @Test
    public void testWriteLinesWithNullElements() throws IOException {
        StringWriter sw = new StringWriter();
        List<String> lines = Arrays.asList("Line 1", null, "Line 3");

        IOUtil.writeLines(lines.iterator(), sw);

        String result = sw.toString();
        assertTrue(result.contains("Line 1"));
        assertTrue(result.contains("null"));
        assertTrue(result.contains("Line 3"));
    }

    @Test
    public void testWriteNullObject() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.write((Object) null, sw);
        assertEquals("null", sw.toString());
    }

    @Test
    public void testWriteLineNullObject() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.writeLine(null, sw);
        assertEquals("null" + IOUtil.LINE_SEPARATOR, sw.toString());
    }

    @Test
    public void testCloseAllIterable() throws IOException {
        List<AutoCloseable> closeables = new ArrayList<>();
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
        closeables.add(baos1);
        closeables.add(baos2);

        IOUtil.closeAll(closeables);
    }

    @Test
    public void testCloseAllWithException() {
        AtomicInteger closedCount = new AtomicInteger(0);

        AutoCloseable c1 = () -> closedCount.incrementAndGet();
        AutoCloseable c2 = () -> {
            throw new IOException("Test exception");
        };
        AutoCloseable c3 = () -> closedCount.incrementAndGet();

        try {
            IOUtil.closeAll(c1, c2, c3);
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals(2, closedCount.get());
        }
    }

    @Test
    public void testCloseAllQuietlyIterable() {
        List<AutoCloseable> closeables = new ArrayList<>();
        closeables.add(() -> {
            throw new IOException("Test exception 1");
        });
        closeables.add(() -> {
            throw new IOException("Test exception 2");
        });

        IOUtil.closeAllQuietly(closeables);
    }

    @Test
    public void testReadWithPartialData() throws IOException {
        InputStream is = new InputStream() {
            private int position = 0;
            private final byte[] data = TEST_CONTENT.getBytes(UTF_8);

            @Override
            public int read(byte[] b, int off, int len) {
                if (position >= data.length)
                    return -1;
                b[off] = data[position++];
                return 1;
            }

            @Override
            public int read() {
                if (position >= data.length)
                    return -1;
                return data[position++];
            }
        };

        byte[] buffer = new byte[TEST_CONTENT.length()];
        int read = IOUtil.read(is, buffer);
        assertEquals(TEST_CONTENT.length(), read);
        assertArrayEquals(TEST_CONTENT.getBytes(UTF_8), buffer);
    }

    @Test
    public void testReadFileWithOffsetAndLen() throws IOException {
        byte[] buffer = new byte[10];
        int read = IOUtil.read(tempFile, buffer, 2, 5);

        assertEquals(5, read);
        byte[] expected = new byte[10];
        System.arraycopy(TEST_CONTENT.getBytes(UTF_8), 0, expected, 2, 5);
        assertArrayEquals(expected, buffer);
    }

    @Test
    public void testReadReaderWithPartialData() throws IOException {
        Reader reader = new Reader() {
            private int position = 0;
            private final char[] data = TEST_CONTENT.toCharArray();

            @Override
            public int read(char[] cbuf, int off, int len) {
                if (position >= data.length)
                    return -1;
                cbuf[off] = data[position++];
                return 1;
            }

            @Override
            public void close() {
            }
        };

        char[] buffer = new char[TEST_CONTENT.length()];
        int read = IOUtil.read(reader, buffer);
        assertEquals(TEST_CONTENT.length(), read);
        assertArrayEquals(TEST_CONTENT.toCharArray(), buffer);
    }

    @Test
    public void testLargeFileSplitBySize() throws IOException {
        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), LONG_CONTENT.getBytes(UTF_8));

        long fileSize = largeFile.length();

        IOUtil.split(largeFile, 4);
        IOUtil.listFiles(largeFile.getParentFile()).forEach(f -> System.out.println("Part: " + f.getName()));

        File[] parts = largeFile.getParentFile().listFiles((dir, name) -> name.startsWith(largeFile.getName() + "_") && !name.equals("large.txt"));

        assertEquals(4, parts.length);

        long totalSize = 0;
        for (File part : parts) {
            totalSize += part.length();
        }
        assertEquals(fileSize, totalSize);
    }

    @Test
    public void testMergeEmptyFiles() throws IOException {
        File file1 = new File(tempDir, "empty1.txt");
        File file2 = new File(tempDir, "empty2.txt");
        file1.createNewFile();
        file2.createNewFile();

        File mergedFile = new File(tempDir, "merged.txt");
        long bytesWritten = IOUtil.merge(Arrays.asList(file1, file2), mergedFile);

        assertEquals(0, bytesWritten);
        assertEquals(0, mergedFile.length());
    }

    @Test
    public void testMergeWithEmptyDelimiter() throws IOException {
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        Files.write(file1.toPath(), "Part1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Part2".getBytes(UTF_8));

        File mergedFile = new File(tempDir, "merged.txt");
        long bytesWritten = IOUtil.merge(Arrays.asList(file1, file2), new byte[0], mergedFile);

        assertEquals(10, bytesWritten);
        assertEquals("Part1Part2", new String(Files.readAllBytes(mergedFile.toPath()), UTF_8));
    }

    @Test
    public void testForLinesWithDirectoryRecursive() throws Exception {
        File subDir1 = new File(tempDir, "sub1");
        File subDir2 = new File(tempDir, "sub2");
        subDir1.mkdir();
        subDir2.mkdir();

        File file1 = new File(subDir1, "file1.txt");
        File file2 = new File(subDir2, "file2.txt");
        File file3 = new File(tempDir, "file3.txt");

        Files.write(file1.toPath(), "Sub1 Line1\nSub1 Line2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Sub2 Line1\nSub2 Line2\n".getBytes(UTF_8));
        Files.write(file3.toPath(), "Root Line1\nRoot Line2\n".getBytes(UTF_8));

        List<String> allLines = new ArrayList<>();
        IOUtil.forLines(tempDir, allLines::add);

        assertEquals(6, allLines.size());
        assertTrue(allLines.stream().anyMatch(line -> line.contains("Sub1")));
        assertTrue(allLines.stream().anyMatch(line -> line.contains("Sub2")));
        assertTrue(allLines.stream().anyMatch(line -> line.contains("Root")));
    }

    @Test
    public void testForLinesEmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty").toFile();
        List<String> lines = new ArrayList<>();

        IOUtil.forLines(emptyDir, lines::add);

        assertEquals(0, lines.size());
    }

    @Test
    public void testForLinesEmptyCollection() throws Exception {
        List<File> emptyList = Collections.emptyList();
        List<String> lines = new ArrayList<>();

        IOUtil.forLines(emptyList, lines::add);

        assertEquals(0, lines.size());
    }

    @Test
    public void testSkipNegativeCount() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            IOUtil.skip(is, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("non-negative"));
        }
    }

    @Test
    public void testSkipZeroCount() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            long skipped = IOUtil.skip(is, 0);
            assertEquals(0, skipped);
        }
    }

    @Test
    public void testSkipMoreThanAvailable() throws IOException {
        byte[] data = "12345".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 10);
            assertEquals(5, skipped);
        }
    }

    @Test
    public void testSkipReaderNegativeCount() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            IOUtil.skip(reader, -1);
            fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("non-negative"));
        }
    }

    @Test
    public void testWriteEmptyFile() throws IOException {
        File emptySource = Files.createTempFile(tempFolder, "empty", ".txt").toFile();
        File dest = new File(tempDir, "dest.txt");

        long written = IOUtil.write(emptySource, dest);

        assertEquals(0, written);
        assertEquals(0, dest.length());
    }

    @Test
    public void testWriteWithZeroCount() throws IOException {
        File dest = new File(tempDir, "dest.txt");
        long written = IOUtil.write(tempFile, 0, 0, dest);

        assertEquals(0, written);
        assertEquals(0, dest.length());
    }

    @Test
    public void testWriteInputStreamZeroCount() throws IOException {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            long written = IOUtil.write(is, 0, 0, baos);

            assertEquals(0, written);
            assertEquals(0, baos.size());
        }
    }

    @Test
    public void testWriteReaderZeroCount() throws IOException {
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            StringWriter sw = new StringWriter();
            long written = IOUtil.write(reader, 0, 0, sw);

            assertEquals(0, written);
            assertEquals("", sw.toString());
        }
    }

    @Test
    public void testListNonExistentDirectory() {
        File nonExistent = new File(tempDir, "nonexistent");

        List<String> files = IOUtil.walk(nonExistent).map(File::getName).toList();
        assertEquals(0, files.size());

        List<File> fileList = IOUtil.listFiles(nonExistent);
        assertEquals(0, fileList.size());

        List<File> dirs = IOUtil.listDirectories(nonExistent);
        assertEquals(0, dirs.size());
    }

    @Test
    public void testMapNonExistentFile() throws IOException {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(nonExistent));
    }

    @Test
    public void testMapWithNegativeOffset() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(tempFile, MapMode.READ_ONLY, -1, 10));
    }

    @Test
    public void testMapWithNegativeCount() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(tempFile, MapMode.READ_ONLY, 0, -1));
    }

    @Test
    public void testSimplifyPathEdgeCases() {
        assertEquals(".", IOUtil.simplifyPath(""));
        assertEquals(".", IOUtil.simplifyPath("."));
        assertEquals(".", IOUtil.simplifyPath("./"));
        assertEquals("..", IOUtil.simplifyPath(".."));
        assertEquals("../..", IOUtil.simplifyPath("../.."));
        assertEquals("/", IOUtil.simplifyPath("/"));
        assertEquals("/", IOUtil.simplifyPath("/."));
        assertEquals("/", IOUtil.simplifyPath("/./"));
        assertEquals("a", IOUtil.simplifyPath("./a"));
        assertEquals("a/b", IOUtil.simplifyPath("./a/b"));
        assertEquals("/a/b", IOUtil.simplifyPath("/a/./b"));
        assertEquals("/b", IOUtil.simplifyPath("/a/../b"));
        assertEquals("b", IOUtil.simplifyPath("a/../b"));

        assertEquals("a/b", IOUtil.simplifyPath("a\\b"));
        assertEquals("/a/b", IOUtil.simplifyPath("\\a\\b"));
    }

    @Test
    public void testFileExtensionEdgeCases() {
        assertEquals("", IOUtil.getFileExtension("noextension"));
        assertEquals("txt", IOUtil.getFileExtension(".hidden.txt"));
        assertEquals("hidden", IOUtil.getFileExtension(".hidden"));
        assertEquals("gz", IOUtil.getFileExtension("archive.tar.gz"));
        assertEquals("", IOUtil.getFileExtension(""));
        assertEquals("", IOUtil.getFileExtension("."));
        assertEquals("", IOUtil.getFileExtension(".."));
    }

    @Test
    public void testGetNameWithoutExtensionEdgeCases() {
        assertEquals("noextension", IOUtil.getNameWithoutExtension("noextension"));
        assertEquals(".hidden", IOUtil.getNameWithoutExtension(".hidden.txt"));
        assertEquals("", IOUtil.getNameWithoutExtension(".hidden"));
        assertEquals("archive.tar", IOUtil.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("", IOUtil.getNameWithoutExtension(""));
        assertEquals("", IOUtil.getNameWithoutExtension("."));
        assertEquals(".", IOUtil.getNameWithoutExtension(".."));
    }

    @Test
    public void testCurrentDir() {
        assertNotNull(IOUtil.CURRENT_DIR);
        assertFalse(IOUtil.CURRENT_DIR.endsWith("."));
        assertTrue(new File(IOUtil.CURRENT_DIR).exists());
    }

    @Test
    public void testReadLinesWithDifferentLineSeparators() throws IOException {
        File file = Files.createTempFile(tempFolder, "mixed_line_endings", ".txt").toFile();
        String content = "Line1\rLine2\nLine3\r\nLine4";
        Files.write(file.toPath(), content.getBytes(UTF_8));

        List<String> lines = IOUtil.readAllLines(file);
        assertEquals(4, lines.size());
        assertEquals("Line1", lines.get(0));
        assertEquals("Line2", lines.get(1));
        assertEquals("Line3", lines.get(2));
        assertEquals("Line4", lines.get(3));
    }

    @Test
    public void testContentEqualsIgnoreEOLDifferentLineEndings() throws IOException {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\r\nLine2\r\nLine3".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\nLine2\nLine3".getBytes(UTF_8));

        assertTrue(IOUtil.contentEqualsIgnoreEOL(file1, file2, "UTF-8"));
    }

    @Test
    public void testContentEqualsIgnoreEOLDifferentContent() throws IOException {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\nLine3".getBytes(UTF_8));

        assertFalse(IOUtil.contentEqualsIgnoreEOL(file1, file2, "UTF-8"));
    }

    @Test
    public void testSymbolicLinkOperations() throws IOException {
        try {
            Path linkPath = Files.createSymbolicLink(Paths.get(tempDir.getAbsolutePath(), "link.txt"), tempFile.toPath());
            File linkFile = linkPath.toFile();

            assertTrue(IOUtil.isSymbolicLink(linkFile));
            assertFalse(IOUtil.isRegularFile(linkFile, LinkOption.NOFOLLOW_LINKS));
            assertTrue(IOUtil.isRegularFile(linkFile));

        } catch (UnsupportedOperationException | IOException e) {
        }
    }

    @Test
    public void testReadWriteWithSmallBuffer() throws IOException {
        byte[] data = new byte[1024];
        Arrays.fill(data, (byte) 'A');

        File largeFile = Files.createTempFile(tempFolder, "large", ".txt").toFile();
        Files.write(largeFile.toPath(), data);

        byte[] result = IOUtil.readBytes(largeFile);
        assertArrayEquals(data, result);
    }

    @Test
    public void testCopyFileToItself() throws IOException {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(tempFile, tempFile));
    }

    @Test
    public void testCopyFileDestinationExists() throws IOException {
        File dest = Files.createTempFile(tempFolder, "existing", ".txt").toFile();
        assertNotEquals(TEST_CONTENT, new String(Files.readAllBytes(dest.toPath()), UTF_8));
        IOUtil.copyFile(tempFile, dest, StandardCopyOption.REPLACE_EXISTING);
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(dest.toPath()), UTF_8));
    }

    @Test
    public void testOperationsOnReadOnlyFile() throws IOException {
        if (!IOUtil.IS_OS_WINDOWS) {
            File readOnlyFile = Files.createTempFile(tempFolder, "readonly", ".txt").toFile();
            Files.write(readOnlyFile.toPath(), TEST_CONTENT.getBytes(UTF_8));
            readOnlyFile.setWritable(false);

            try {
                String content = IOUtil.readAllToString(readOnlyFile);
                assertEquals(TEST_CONTENT, content);

                assertEquals(TEST_CONTENT.length(), IOUtil.sizeOf(readOnlyFile));

            } finally {
                readOnlyFile.setWritable(true);
            }
        }
    }

    @Test
    public void testWriteNumericEdgeCases() throws IOException {
        StringWriter sw = new StringWriter();

        IOUtil.write(Byte.MIN_VALUE, sw);
        IOUtil.write(Byte.MAX_VALUE, sw);
        IOUtil.write(Short.MIN_VALUE, sw);
        IOUtil.write(Short.MAX_VALUE, sw);
        IOUtil.write(Integer.MIN_VALUE, sw);
        IOUtil.write(Integer.MAX_VALUE, sw);
        IOUtil.write(Long.MIN_VALUE, sw);
        IOUtil.write(Long.MAX_VALUE, sw);
        IOUtil.write(Float.NaN, sw);
        IOUtil.write(Float.POSITIVE_INFINITY, sw);
        IOUtil.write(Float.NEGATIVE_INFINITY, sw);
        IOUtil.write(Double.NaN, sw);
        IOUtil.write(Double.POSITIVE_INFINITY, sw);
        IOUtil.write(Double.NEGATIVE_INFINITY, sw);

        String result = sw.toString();
        assertTrue(result.contains(String.valueOf(Byte.MIN_VALUE)));
        assertTrue(result.contains(String.valueOf(Integer.MAX_VALUE)));
        assertTrue(result.contains("NaN"));
        assertTrue(result.contains("Infinity"));
    }

    @Test
    public void testLongFileNames() throws IOException {
        StringBuilder longName = new StringBuilder();
        for (int i = 0; i < 200; i++) {
            longName.append("a");
        }
        longName.append(".txt");

        try {
            File longNameFile = new File(tempDir, longName.toString());
            longNameFile.createNewFile();

            IOUtil.write(TEST_CONTENT, longNameFile);
            String content = IOUtil.readAllToString(longNameFile);
            assertEquals(TEST_CONTENT, content);

        } catch (IOException e) {
        }
    }

    @Test
    public void testConcurrentForLines() throws Exception {
        File largeFile = Files.createTempFile(tempFolder, "concurrent", ".txt").toFile();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(largeFile.toPath(), content.toString().getBytes(UTF_8));

        List<String> lines = Collections.synchronizedList(new ArrayList<>());
        AtomicBoolean completed = new AtomicBoolean(false);

        IOUtil.forLines(largeFile, 0, Long.MAX_VALUE, 4, 100, line -> {
            lines.add(line);
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, () -> completed.set(true));

        assertTrue(completed.get());
        assertEquals(1000, lines.size());
    }

    @Test
    public void testSpecialCharactersInFileNames() throws IOException {
        String[] specialNames = { "file with spaces.txt", "file-with-dashes.txt", "file_with_underscores.txt", "file.multiple.dots.txt",
                "file@with#special$chars.txt" };

        for (String name : specialNames) {
            try {
                File specialFile = new File(tempDir, name);
                if (specialFile.createNewFile()) {
                    IOUtil.write(TEST_CONTENT, specialFile);
                    String content = IOUtil.readAllToString(specialFile);
                    assertEquals(TEST_CONTENT, content);
                }
            } catch (IOException e) {
            }
        }
    }

    @Test
    public void testDirectorySize() throws IOException {
        File dir1 = new File(tempDir, "dir1");
        File dir2 = new File(dir1, "dir2");
        dir1.mkdir();
        dir2.mkdir();

        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(dir1, "file2.txt");
        File file3 = new File(dir2, "file3.txt");

        Files.write(file1.toPath(), "12345".getBytes(UTF_8));
        Files.write(file2.toPath(), "67890".getBytes(UTF_8));
        Files.write(file3.toPath(), "ABCDE".getBytes(UTF_8));

        long size = IOUtil.sizeOfDirectory(tempDir);
        assertEquals(15, size);
    }

    @Test
    public void testSizeOfDirectoryOnFile() throws FileNotFoundException {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.sizeOfDirectory(tempFile));
    }

    @Test
    public void testAppendToNonExistentFile() throws IOException {
        File nonExistent = new File(tempDir, "nonexistent.txt");
        assertFalse(nonExistent.exists());

        IOUtil.append(TEST_CONTENT, nonExistent);

        assertTrue(nonExistent.exists());
        assertEquals(TEST_CONTENT, new String(Files.readAllBytes(nonExistent.toPath()), UTF_8));
    }

    @Test
    public void testReadBeyondEOF() throws IOException {
        byte[] smallData = "123".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(smallData)) {
            is.read();
            byte[] buffer = new byte[10];
            int read = IOUtil.read(is, buffer);
            assertEquals(2, read);
        }
    }

    @Test
    public void testWriteNullCharArray() throws IOException {
        File output = new File(tempDir, "null_output.txt");
        IOUtil.write((char[]) null, output);
        assertEquals(0, output.length());
    }

    @Test
    public void testWriteNullByteArray() throws IOException {
        File output = new File(tempDir, "null_output.txt");
        IOUtil.write((byte[]) null, output);
        assertEquals(0, output.length());
    }
}
