package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.MappedByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("new-test")
public class IOUtil200Test extends TestBase {
    private static final SecureRandom random = new SecureRandom();
    @TempDir
    Path tempDir;

    private File createTempFileWithContent(String prefix, String suffix, String content) throws IOException {
        return createTempFileWithContent(prefix, suffix, content, StandardCharsets.UTF_8);
    }

    private File createTempFileWithContent(String prefix, String suffix, String content, Charset charset) throws IOException {

        long n = random.nextLong();
        String s = tempDir.resolve(prefix).toString() + Long.toUnsignedString(n) + Strings.nullToEmpty(suffix);
        File tempFile = tempDir.getFileSystem().getPath(s).toFile();
        tempFile.createNewFile();

        if (content != null) {
            IOUtil.write(content.getBytes(charset), tempFile);
        }
        return tempFile;
    }

    private File createTempFileWithContent(String prefix, String suffix, byte[] content) throws IOException {
        long n = random.nextLong();
        String s = tempDir.resolve(prefix).toString() + Long.toUnsignedString(n) + Strings.nullToEmpty(suffix);
        File tempFile = tempDir.getFileSystem().getPath(s).toFile();
        tempFile.createNewFile();

        if (content != null) {
            IOUtil.write(content, tempFile);
        }
        return tempFile;
    }

    private File createTempFile(String prefix, String suffix) throws IOException {
        return Files.createTempFile(tempDir, prefix, suffix).toFile();
    }

    private File createTempDirectory(String prefix) throws IOException {
        return Files.createTempDirectory(tempDir, prefix).toFile();
    }

    @Test
    public void testConstants() {
        assertEquals(1024, IOUtil.ONE_KB);
        assertEquals(1024 * 1024, IOUtil.ONE_MB);
        assertEquals(1024 * 1024 * 1024, IOUtil.ONE_GB);
        assertTrue(IOUtil.MAX_MEMORY_IN_MB > 0);
        assertTrue(IOUtil.CPU_CORES > 0);

        assertNotNull(IOUtil.OS_NAME);
        assertNotNull(IOUtil.OS_ARCH);
        assertNotNull(IOUtil.OS_VERSION);
        assertNotNull(IOUtil.JAVA_HOME);
        assertNotNull(IOUtil.JAVA_VERSION);
        assertNotNull(IOUtil.USER_DIR);
        assertNotNull(IOUtil.USER_HOME);
        assertNotNull(IOUtil.USER_NAME);
        assertNotNull(IOUtil.PATH_SEPARATOR);
        assertNotNull(IOUtil.DIR_SEPARATOR);
        assertNotNull(IOUtil.LINE_SEPARATOR_UNIX);
        assertNotNull(IOUtil.CURRENT_DIR);

        assertEquals(-1, IOUtil.EOF);
    }

    @Test
    public void testGetHostName() {
        String hostName = IOUtil.getHostName();
        assertNotNull(hostName);
        assertFalse(hostName.isEmpty());
    }

    @Test
    @Disabled("Depends on FileSystemUtil and external environment, hard to unit test reliably")
    public void testFreeDiskSpaceKb() {
        assertTrue(IOUtil.freeDiskSpaceKB() >= 0);
        File currentDir = new File(".");
        assertTrue(IOUtil.freeDiskSpaceKB(currentDir.getAbsolutePath()) >= 0);
        assertTrue(IOUtil.freeDiskSpaceKB(currentDir.getAbsolutePath(), 5000) >= 0);
    }

    @Test
    public void testChars2BytesAndBytes2Chars() {
        char[] emptyChars = CommonUtil.EMPTY_CHAR_ARRAY;
        byte[] emptyBytes = IOUtil.charsToBytes(emptyChars);
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, emptyBytes);
        assertArrayEquals(emptyChars, IOUtil.bytesToChars(emptyBytes));

        char[] testChars = "Hello World".toCharArray();
        byte[] utf8Bytes = IOUtil.charsToBytes(testChars, StandardCharsets.UTF_8);
        char[] convertedCharsUtf8 = IOUtil.bytesToChars(utf8Bytes, StandardCharsets.UTF_8);
        assertArrayEquals(testChars, convertedCharsUtf8);

        byte[] defaultCharsetBytes = IOUtil.charsToBytes(testChars);
        char[] convertedCharsDefault = IOUtil.bytesToChars(defaultCharsetBytes);
        assertArrayEquals(testChars, convertedCharsDefault);

        char[] subChars = { 'W', 'o', 'r' };
        byte[] subBytesUtf8 = IOUtil.charsToBytes(testChars, 6, 3, StandardCharsets.UTF_8);
        assertArrayEquals(IOUtil.charsToBytes(subChars, StandardCharsets.UTF_8), subBytesUtf8);

        char[] convertedSubChars = IOUtil.bytesToChars(subBytesUtf8, 0, subBytesUtf8.length, StandardCharsets.UTF_8);
        assertArrayEquals(subChars, convertedSubChars);

        byte[] bytesNullCharset = IOUtil.charsToBytes(testChars, null);
        assertArrayEquals(defaultCharsetBytes, bytesNullCharset);
        char[] charsNullCharset = IOUtil.bytesToChars(bytesNullCharset, null);
        assertArrayEquals(testChars, charsNullCharset);
    }

    @Test
    public void testStringConversions() throws IOException {
        String testStr = "Hello Gemini";

        InputStream isDefault = IOUtil.stringToInputStream(testStr);
        assertNotNull(isDefault);
        assertEquals(testStr, assertDoesNotThrow(() -> IOUtil.readAllToString(isDefault)));

        InputStream isUtf8 = IOUtil.stringToInputStream(testStr, StandardCharsets.UTF_8);
        assertNotNull(isUtf8);
        assertEquals(testStr, assertDoesNotThrow(() -> IOUtil.readAllToString(isUtf8, StandardCharsets.UTF_8)));

        InputStream is = IOUtil.stringToInputStream(null);

        assertEquals(0, IOUtil.readBytes(is).length);

        Reader reader = IOUtil.stringToReader(testStr);
        assertNotNull(reader);
        assertEquals(testStr, assertDoesNotThrow(() -> IOUtil.readAllToString(reader)));

        Reader readerNull = IOUtil.stringToReader(null);
        assertEquals("", assertDoesNotThrow(() -> IOUtil.readAllToString(readerNull)));

        StringBuilder sb = new StringBuilder("Test SB");
        Writer writer = IOUtil.stringBuilderToWriter(sb);
        assertNotNull(writer);
        assertDoesNotThrow(() -> writer.write(" Appended"));
        assertEquals("Test SB Appended", sb.toString());

        assertThrows(IllegalArgumentException.class, () -> IOUtil.stringBuilderToWriter(null));
    }

    @Test
    public void testReadAllBytesFromFile() throws IOException {
        byte[] content = "Test content for bytes.".getBytes(StandardCharsets.UTF_8);
        File testFile = createTempFileWithContent("readBytes", ".txt", content);

        byte[] readContent = IOUtil.readAllBytes(testFile);
        assertArrayEquals(content, readContent);

        File emptyFile = createTempFileWithContent("emptyBytes", ".txt", new byte[0]);
        byte[] readEmptyContent = IOUtil.readAllBytes(emptyFile);
        assertArrayEquals(new byte[0], readEmptyContent);
    }

    @Test
    public void testReadAllBytesFromInputStream() throws IOException {
        byte[] content = "Stream content.".getBytes(StandardCharsets.UTF_8);
        InputStream is = new ByteArrayInputStream(content);
        byte[] readContent = IOUtil.readAllBytes(is);
        assertArrayEquals(content, readContent);

        InputStream emptyIs = new ByteArrayInputStream(new byte[0]);
        byte[] readEmptyContent = IOUtil.readAllBytes(emptyIs);
        assertArrayEquals(new byte[0], readEmptyContent);
    }

    @Test
    public void testReadBytesFromFile() throws IOException {
        byte[] fullContent = "This is a test file for reading bytes.".getBytes(StandardCharsets.UTF_8);
        File testFile = createTempFileWithContent("readBytesOffset", ".txt", fullContent);

        byte[] readAll = IOUtil.readBytes(testFile);
        assertArrayEquals(fullContent, readAll);

        byte[] expectedPart = "is a test".getBytes(StandardCharsets.UTF_8);
        byte[] readPart = IOUtil.readBytes(testFile, 5, 9);
        assertArrayEquals(expectedPart, readPart);

        byte[] readBeyond = IOUtil.readBytes(testFile, 0, fullContent.length + 10);
        assertArrayEquals(fullContent, readBeyond);

        byte[] readFromFarOffset = IOUtil.readBytes(testFile, fullContent.length + 5, 10);
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, readFromFarOffset);

        byte[] readZero = IOUtil.readBytes(testFile, 5, 0);
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, readZero);
    }

    @Test
    public void testReadBytesFromInputStream() throws IOException {
        byte[] fullContent = "Input stream for byte reading test.".getBytes(StandardCharsets.UTF_8);

        InputStream isReadAll = new ByteArrayInputStream(fullContent);
        byte[] readAll = IOUtil.readBytes(isReadAll);
        assertArrayEquals(fullContent, readAll);

        InputStream isReadPart = new ByteArrayInputStream(fullContent);
        byte[] expectedPart = "stream for".getBytes(StandardCharsets.UTF_8);
        byte[] readPart = IOUtil.readBytes(isReadPart, 6, 10);
        assertArrayEquals(expectedPart, readPart);

        InputStream isReadBeyond = new ByteArrayInputStream(fullContent);
        byte[] readBeyond = IOUtil.readBytes(isReadBeyond, 0, fullContent.length + 10);
        assertArrayEquals(fullContent, readBeyond);

        InputStream isReadFromFarOffset = new ByteArrayInputStream(fullContent);
        byte[] readFromFarOffset = IOUtil.readBytes(isReadFromFarOffset, fullContent.length + 5, 10);
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, readFromFarOffset);

        InputStream isReadZero = new ByteArrayInputStream(fullContent);
        byte[] readZero = IOUtil.readBytes(isReadZero, 5, 0);
        assertArrayEquals(CommonUtil.EMPTY_BYTE_ARRAY, readZero);
    }

    @Test
    public void testReadAllCharsFromFile() throws IOException {
        String contentStr = "Test content for chars. With Ümlauts.";
        File testFileDefault = createTempFileWithContent("readChars", ".txt", contentStr, Charsets.DEFAULT);
        char[] readContentDefault = IOUtil.readAllChars(testFileDefault);
        assertArrayEquals(contentStr.toCharArray(), readContentDefault);

        File testFileUtf8 = createTempFileWithContent("readCharsUtf8", ".txt", contentStr, StandardCharsets.UTF_8);
        char[] readContentUtf8 = IOUtil.readAllChars(testFileUtf8, StandardCharsets.UTF_8);
        assertArrayEquals(contentStr.toCharArray(), readContentUtf8);

        File emptyFile = createTempFileWithContent("emptyChars", ".txt", "");
        char[] readEmpty = IOUtil.readAllChars(emptyFile);
        assertArrayEquals(new char[0], readEmpty);
    }

    @Test
    public void testReadAllCharsFromInputStream() throws IOException {
        String contentStr = "Stream content for chars. With € symbol.";

        InputStream isDefault = new ByteArrayInputStream(contentStr.getBytes(Charsets.DEFAULT));
        char[] readContentDefault = IOUtil.readAllChars(isDefault);
        assertArrayEquals(contentStr.toCharArray(), readContentDefault);

        InputStream isUtf8 = new ByteArrayInputStream(contentStr.getBytes(StandardCharsets.UTF_8));
        char[] readContentUtf8 = IOUtil.readAllChars(isUtf8, StandardCharsets.UTF_8);
        assertArrayEquals(contentStr.toCharArray(), readContentUtf8);

        InputStream emptyIs = new ByteArrayInputStream(new byte[0]);
        char[] readEmpty = IOUtil.readAllChars(emptyIs, StandardCharsets.UTF_8);
        assertArrayEquals(new char[0], readEmpty);
    }

    @Test
    public void testReadAllCharsFromReader() throws IOException {
        String contentStr = "Reader content for chars. 123.";
        Reader reader = new StringReader(contentStr);
        char[] readContent = IOUtil.readAllChars(reader);
        assertArrayEquals(contentStr.toCharArray(), readContent);

        Reader emptyReader = new StringReader("");
        char[] readEmpty = IOUtil.readAllChars(emptyReader);
        assertArrayEquals(new char[0], readEmpty);
    }

    @Test
    public void testReadCharsFromFile() throws IOException {
        String fullContentStr = "This is a test file for reading characters.";
        File testFile = createTempFileWithContent("readCharsOffset", ".txt", fullContentStr, StandardCharsets.UTF_8);

        char[] readAll = IOUtil.readChars(testFile, StandardCharsets.UTF_8);
        assertArrayEquals(fullContentStr.toCharArray(), readAll);

        String expectedPartStr = "is a test";
        char[] readPart = IOUtil.readChars(testFile, StandardCharsets.UTF_8, 5, 9);
        assertArrayEquals(expectedPartStr.toCharArray(), readPart);

        char[] readBeyond = IOUtil.readChars(testFile, StandardCharsets.UTF_8, 0, fullContentStr.length() + 10);
        assertArrayEquals(fullContentStr.toCharArray(), readBeyond);

        char[] readFromFarOffset = IOUtil.readChars(testFile, StandardCharsets.UTF_8, fullContentStr.length() + 5, 10);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readFromFarOffset);

        char[] readZero = IOUtil.readChars(testFile, StandardCharsets.UTF_8, 5, 0);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readZero);
    }

    @Test
    public void testReadCharsFromInputStream() throws IOException {
        String fullContentStr = "Input stream for character reading test.";

        InputStream isReadAll = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        char[] readAll = IOUtil.readChars(isReadAll, StandardCharsets.UTF_8);
        assertArrayEquals(fullContentStr.toCharArray(), readAll);

        InputStream isReadPart = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        String expectedPartStr = "stream for";
        char[] readPart = IOUtil.readChars(isReadPart, StandardCharsets.UTF_8, 6, 10);
        assertArrayEquals(expectedPartStr.toCharArray(), readPart);

        InputStream isReadBeyond = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        char[] readBeyond = IOUtil.readChars(isReadBeyond, StandardCharsets.UTF_8, 0, fullContentStr.length() + 10);
        assertArrayEquals(fullContentStr.toCharArray(), readBeyond);

        InputStream isReadFromFarOffset = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        char[] readFromFarOffset = IOUtil.readChars(isReadFromFarOffset, StandardCharsets.UTF_8, fullContentStr.length() + 5, 10);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readFromFarOffset);

        InputStream isReadZero = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        char[] readZero = IOUtil.readChars(isReadZero, StandardCharsets.UTF_8, 5, 0);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readZero);
    }

    @Test
    public void testReadCharsFromReader() throws IOException {
        String fullContentStr = "Reader for character reading test.";

        Reader rReadAll = new StringReader(fullContentStr);
        char[] readAll = IOUtil.readChars(rReadAll);
        assertArrayEquals(fullContentStr.toCharArray(), readAll);

        Reader rReadPart = new StringReader(fullContentStr);
        String expectedPartStr = "character";
        char[] readPart = IOUtil.readChars(rReadPart, 11, 9);
        assertArrayEquals(expectedPartStr.toCharArray(), readPart);

        Reader rReadBeyond = new StringReader(fullContentStr);
        char[] readBeyond = IOUtil.readChars(rReadBeyond, 0, fullContentStr.length() + 10);
        assertArrayEquals(fullContentStr.toCharArray(), readBeyond);

        Reader rReadFromFarOffset = new StringReader(fullContentStr);
        char[] readFromFarOffset = IOUtil.readChars(rReadFromFarOffset, fullContentStr.length() + 5, 10);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readFromFarOffset);

        Reader rReadZero = new StringReader(fullContentStr);
        char[] readZero = IOUtil.readChars(rReadZero, 5, 0);
        assertArrayEquals(CommonUtil.EMPTY_CHAR_ARRAY, readZero);
    }

    @Test
    public void testReadAllToStringFromFile() throws IOException {
        String contentStr = "File to string. Test with Ümlauts and €.";
        File testFileDefault = createTempFileWithContent("toStringFile", ".txt", contentStr, Charsets.DEFAULT);
        assertEquals(contentStr, IOUtil.readAllToString(testFileDefault));

        File testFileUtf8 = createTempFileWithContent("toStringFileUtf8", ".txt", contentStr, StandardCharsets.UTF_8);
        assertEquals(contentStr, IOUtil.readAllToString(testFileUtf8, StandardCharsets.UTF_8));

        File emptyFile = createTempFileWithContent("emptyStringFile", ".txt", "");
        assertEquals("", IOUtil.readAllToString(emptyFile));
    }

    @Test
    public void testReadAllToStringFromInputStream() throws IOException {
        String contentStr = "InputStream to string. Special chars: ñ, ç, ê.";
        InputStream isDefault = new ByteArrayInputStream(contentStr.getBytes(Charsets.DEFAULT));
        assertEquals(contentStr, IOUtil.readAllToString(isDefault));

        InputStream isUtf8 = new ByteArrayInputStream(contentStr.getBytes(StandardCharsets.UTF_8));
        assertEquals(contentStr, IOUtil.readAllToString(isUtf8, StandardCharsets.UTF_8));

        InputStream emptyIs = new ByteArrayInputStream(new byte[0]);
        assertEquals("", IOUtil.readAllToString(emptyIs));
    }

    @Test
    public void testReadAllToStringFromReader() throws IOException {
        String contentStr = "Reader to string. Just simple text.";
        Reader reader = new StringReader(contentStr);
        assertEquals(contentStr, IOUtil.readAllToString(reader));

        Reader emptyReader = new StringReader("");
        assertEquals("", IOUtil.readAllToString(emptyReader));
    }

    @Test
    public void testReadToStringFromFile() throws IOException {
        String fullContentStr = "This is a file for limited string reading.";
        File testFile = createTempFileWithContent("readStringOffset", ".txt", fullContentStr, StandardCharsets.UTF_8);

        String expectedPartStr = "is a file";
        assertEquals(expectedPartStr, IOUtil.readToString(testFile, StandardCharsets.UTF_8, 5, 9));

        assertEquals(fullContentStr, IOUtil.readToString(testFile, StandardCharsets.UTF_8, 0, fullContentStr.length() + 5));

        assertEquals("", IOUtil.readToString(testFile, StandardCharsets.UTF_8, fullContentStr.length() + 2, 5));

        assertEquals("", IOUtil.readToString(testFile, StandardCharsets.UTF_8, 5, 0));
    }

    @Test
    public void testReadToStringFromInputStream() throws IOException {
        String fullContentStr = "This is an input stream for limited string reading.";

        String expectedPartStr = "an input";
        InputStream isReadPart = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedPartStr, IOUtil.readToString(isReadPart, StandardCharsets.UTF_8, 8, 8));

        InputStream isReadAll = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        assertEquals(fullContentStr, IOUtil.readToString(isReadAll, StandardCharsets.UTF_8, 0, fullContentStr.length() + 5));

        InputStream isReadFromFarOffset = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        assertEquals("", IOUtil.readToString(isReadFromFarOffset, StandardCharsets.UTF_8, fullContentStr.length() + 2, 5));

        InputStream isReadZero = new ByteArrayInputStream(fullContentStr.getBytes(StandardCharsets.UTF_8));
        assertEquals("", IOUtil.readToString(isReadZero, StandardCharsets.UTF_8, 5, 0));
    }

    @Test
    public void testReadToStringFromReader() throws IOException {
        String fullContentStr = "This is a reader for limited string reading.";

        String expectedPartStr = "a reader";
        Reader rReadPart = new StringReader(fullContentStr);
        assertEquals(expectedPartStr, IOUtil.readToString(rReadPart, 8, 8));

        Reader rReadAll = new StringReader(fullContentStr);
        assertEquals(fullContentStr, IOUtil.readToString(rReadAll, 0, fullContentStr.length() + 5));

        Reader rReadFromFarOffset = new StringReader(fullContentStr);
        assertEquals("", IOUtil.readToString(rReadFromFarOffset, fullContentStr.length() + 2, 5));

        Reader rReadZero = new StringReader(fullContentStr);
        assertEquals("", IOUtil.readToString(rReadZero, 5, 0));
    }

    @Test
    public void testReadAllLinesFromFile() throws IOException {
        String line1 = "First line.";
        String line2 = "Second line with Ümlauts.";
        String line3 = "Third line €.";
        String content = line1 + IOUtil.LINE_SEPARATOR_UNIX + line2 + IOUtil.LINE_SEPARATOR_UNIX + line3;
        List<String> expectedLines = Arrays.asList(line1, line2, line3);

        File testFileDefault = createTempFileWithContent("allLinesFile", ".txt", content, Charsets.DEFAULT);
        assertEquals(expectedLines, IOUtil.readAllLines(testFileDefault));

        File testFileUtf8 = createTempFileWithContent("allLinesFileUtf8", ".txt", content, StandardCharsets.UTF_8);
        assertEquals(expectedLines, IOUtil.readAllLines(testFileUtf8, StandardCharsets.UTF_8));

        File emptyFile = createTempFileWithContent("emptyLinesFile", ".txt", "");
        assertEquals(Collections.emptyList(), IOUtil.readAllLines(emptyFile));

        File fileWithEmptyLine = createTempFileWithContent("fileWithEmptyLine", ".txt",
                "line1" + IOUtil.LINE_SEPARATOR_UNIX + "" + IOUtil.LINE_SEPARATOR_UNIX + "line3");
        assertEquals(Arrays.asList("line1", "", "line3"), IOUtil.readAllLines(fileWithEmptyLine));
    }

    @Test
    public void testReadAllLinesFromInputStream() throws IOException {
        String line1 = "Stream line 1.";
        String line2 = "Stream line 2 with ñ.";
        String content = line1 + IOUtil.LINE_SEPARATOR_UNIX + line2;
        List<String> expectedLines = Arrays.asList(line1, line2);

        InputStream isDefault = new ByteArrayInputStream(content.getBytes(Charsets.DEFAULT));
        assertEquals(expectedLines, IOUtil.readAllLines(isDefault));

        InputStream isUtf8 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(expectedLines, IOUtil.readAllLines(isUtf8, StandardCharsets.UTF_8));

        InputStream emptyIs = new ByteArrayInputStream(new byte[0]);
        assertEquals(Collections.emptyList(), IOUtil.readAllLines(emptyIs));
    }

    @Test
    public void testReadAllLinesFromReader() throws IOException {
        String line1 = "Reader line one.";
        String line2 = "Reader line two.";
        String content = line1 + IOUtil.LINE_SEPARATOR_UNIX + line2;
        List<String> expectedLines = Arrays.asList(line1, line2);

        Reader reader = new StringReader(content);
        assertEquals(expectedLines, IOUtil.readAllLines(reader));

        Reader emptyReader = new StringReader("");
        assertEquals(Collections.emptyList(), IOUtil.readAllLines(emptyReader));
    }

    @Test
    public void testReadLinesFromFile() throws IOException {
        String[] lines = { "Line 0", "Line 1", "Line 2", "Line 3", "Line 4" };
        String content = String.join(IOUtil.LINE_SEPARATOR_UNIX, lines);
        File testFile = createTempFileWithContent("readLinesFile", ".txt", content, StandardCharsets.UTF_8);

        List<String> expected1 = Arrays.asList("Line 1", "Line 2");
        assertEquals(expected1, IOUtil.readLines(testFile, StandardCharsets.UTF_8, 1, 2));

        List<String> expected2 = Arrays.asList("Line 3", "Line 4");
        assertEquals(expected2, IOUtil.readLines(testFile, StandardCharsets.UTF_8, 3, 5));

        List<String> expected3 = Collections.emptyList();
        assertEquals(expected3, IOUtil.readLines(testFile, StandardCharsets.UTF_8, 5, 2));

        assertEquals(expected3, IOUtil.readLines(testFile, StandardCharsets.UTF_8, 1, 0));
    }

    @Test
    public void testReadLinesFromInputStream() throws IOException {
        String[] lines = { "IS Line 0", "IS Line 1", "IS Line 2", "IS Line 3", "IS Line 4" };
        String content = String.join(IOUtil.LINE_SEPARATOR_UNIX, lines);

        InputStream is1 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        List<String> expected1 = Arrays.asList("IS Line 1", "IS Line 2");
        assertEquals(expected1, IOUtil.readLines(is1, StandardCharsets.UTF_8, 1, 2));

        InputStream is2 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        List<String> expected2 = Arrays.asList("IS Line 3", "IS Line 4");
        assertEquals(expected2, IOUtil.readLines(is2, StandardCharsets.UTF_8, 3, 5));

        InputStream is3 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        List<String> expected3 = Collections.emptyList();
        assertEquals(expected3, IOUtil.readLines(is3, StandardCharsets.UTF_8, 5, 2));

        InputStream is4 = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        assertEquals(expected3, IOUtil.readLines(is4, StandardCharsets.UTF_8, 1, 0));
    }

    @Test
    public void testReadLinesFromReader() throws IOException {
        String[] lines = { "Reader Line 0", "Reader Line 1", "Reader Line 2", "Reader Line 3" };
        String content = String.join(IOUtil.LINE_SEPARATOR_UNIX, lines);

        Reader r1 = new StringReader(content);
        List<String> expected1 = Arrays.asList("Reader Line 1", "Reader Line 2");
        assertEquals(expected1, IOUtil.readLines(r1, 1, 2));

        Reader r2 = new StringReader(content);
        List<String> expected2 = Arrays.asList("Reader Line 2", "Reader Line 3");
        assertEquals(expected2, IOUtil.readLines(r2, 2, 5));

        Reader r3 = new StringReader(content);
        List<String> expected3 = Collections.emptyList();
        assertEquals(expected3, IOUtil.readLines(r3, 4, 2));

        Reader r4 = new StringReader(content);
        assertEquals(expected3, IOUtil.readLines(r4, 1, 0));
    }

    @Test
    public void testReadIntoBuffer() throws IOException {
        byte[] contentBytes = "Buffer read test.".getBytes(StandardCharsets.UTF_8);
        File testFileBytes = createTempFileWithContent("readBufferBytes", ".txt", contentBytes);
        byte[] bufferBytes = new byte[10];

        int bytesRead = IOUtil.read(testFileBytes, bufferBytes);
        assertEquals(10, bytesRead);
        assertArrayEquals(Arrays.copyOfRange(contentBytes, 0, 10), bufferBytes);

        bytesRead = IOUtil.read(testFileBytes, bufferBytes, 2, 5);
        InputStream isForBytes = IOUtil.newFileInputStream(testFileBytes);
        byte[] seqBufferBytes = new byte[contentBytes.length];
        bytesRead = IOUtil.read(isForBytes, seqBufferBytes);
        assertEquals(contentBytes.length, bytesRead);
        assertArrayEquals(contentBytes, seqBufferBytes);
        isForBytes.close();

        String contentCharsStr = "Buffer char read test.";
        char[] contentChars = contentCharsStr.toCharArray();
        File testFileChars = createTempFileWithContent("readBufferChars", ".txt", contentCharsStr, StandardCharsets.UTF_8);
        char[] bufferChars = new char[10];

        int charsRead = IOUtil.read(testFileChars, StandardCharsets.UTF_8, bufferChars);
        assertEquals(10, charsRead);
        assertArrayEquals(Arrays.copyOfRange(contentChars, 0, 10), bufferChars);

        Reader readerForChars = IOUtil.newFileReader(testFileChars, StandardCharsets.UTF_8);
        char[] seqBufferChars = new char[contentChars.length];
        charsRead = IOUtil.read(readerForChars, seqBufferChars);
        assertEquals(contentChars.length, charsRead);
        assertArrayEquals(contentChars, seqBufferChars);
        readerForChars.close();
    }

    @Test
    public void testWriteLinesIterator() throws IOException {
        List<String> lines = Arrays.asList("Iter Line 1", "Iter Line 2", null, "Iter Line 4");
        Iterator<?> iterator = lines.iterator();
        File testFile = createTempFile("writeLinesIterator", ".txt");

        IOUtil.writeLines(iterator, testFile);

        StringBuilder expectedSb = new StringBuilder();
        expectedSb.append("Iter Line 1").append(IOUtil.LINE_SEPARATOR_UNIX);
        expectedSb.append("Iter Line 2").append(IOUtil.LINE_SEPARATOR_UNIX);
        expectedSb.append(Strings.NULL.toCharArray()).append(IOUtil.LINE_SEPARATOR_UNIX);
        expectedSb.append("Iter Line 4").append(IOUtil.LINE_SEPARATOR_UNIX);
        assertEquals(expectedSb.toString(), IOUtil.readAllToString(testFile));
    }

    @Test
    public void testWritePrimitivesAndObject() throws IOException {
        StringWriter sw = new StringWriter();
        IOUtil.write(true, sw);
        assertEquals("true", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write('A', sw);
        assertEquals("A", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write((byte) 10, sw);
        assertEquals("10", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write((short) 100, sw);
        assertEquals("100", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write(1000, sw);
        assertEquals("1000", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write(10000L, sw);
        assertEquals("10000", sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write(1.5f, sw);
        assertEquals(String.valueOf(1.5f), sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write(2.5d, sw);
        assertEquals(String.valueOf(2.5d), sw.toString());
        sw.getBuffer().setLength(0);

        Object obj = new Date(0);
        IOUtil.write(obj, sw);
        assertEquals(CommonUtil.toString(obj), sw.toString());
        sw.getBuffer().setLength(0);

        IOUtil.write((Object) null, sw);
        assertArrayEquals(Strings.NULL.toCharArray(), sw.toString().toCharArray());
    }

    @Test
    public void testWriteCharSequence() throws IOException {
        CharSequence cs = "CharSequence test with Ümlaut €";
        File testFile = createTempFile("writeCS", ".txt");

        IOUtil.write(cs, testFile);
        assertEquals(cs.toString(), IOUtil.readAllToString(testFile));

        File testFileUtf8 = createTempFile("writeCSUtf8", ".txt");
        IOUtil.write(cs, StandardCharsets.UTF_8, testFileUtf8);
        assertEquals(cs.toString(), IOUtil.readAllToString(testFileUtf8, StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.write(cs, baos, true);
        assertEquals(cs.toString(), baos.toString(Charsets.DEFAULT.name()));

        baos.reset();
        IOUtil.write(cs, StandardCharsets.UTF_8, baos, true);
        assertEquals(cs.toString(), baos.toString(StandardCharsets.UTF_8.name()));

        StringWriter sw = new StringWriter();
        IOUtil.write(cs, sw, true);
        assertEquals(cs.toString(), sw.toString());
    }

    @Test
    public void testWriteCharArray() throws IOException {
        char[] chars = "char array test with Ümlaut €".toCharArray();
        File testFile = createTempFile("writeCharsArr", ".txt");

        IOUtil.write(chars, testFile);
        assertArrayEquals(chars, IOUtil.readAllChars(testFile));

        File testFileUtf8 = createTempFile("writeCharsArrUtf8", ".txt");
        IOUtil.write(chars, StandardCharsets.UTF_8, testFileUtf8);
        assertArrayEquals(chars, IOUtil.readAllChars(testFileUtf8, StandardCharsets.UTF_8));

        File testFileOffset = createTempFile("writeCharsArrOffset", ".txt");
        char[] subChars = Arrays.copyOfRange(chars, 5, 10);
        IOUtil.write(chars, 5, 5, StandardCharsets.UTF_8, testFileOffset);
        assertArrayEquals(subChars, IOUtil.readAllChars(testFileOffset, StandardCharsets.UTF_8));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.write(chars, baos, true);
        assertArrayEquals(chars, IOUtil.bytesToChars(baos.toByteArray(), Charsets.DEFAULT));

        baos.reset();
        IOUtil.write(chars, 5, 5, StandardCharsets.UTF_8, baos, true);
        assertArrayEquals(subChars, IOUtil.bytesToChars(baos.toByteArray(), StandardCharsets.UTF_8));

        StringWriter sw = new StringWriter();
        IOUtil.write(chars, sw, true);
        assertArrayEquals(chars, sw.toString().toCharArray());

        sw = new StringWriter();
        IOUtil.write(chars, 5, 5, sw, true);
        assertArrayEquals(subChars, sw.toString().toCharArray());
    }

    @Test
    public void testWriteByteArray() throws IOException {
        byte[] bytes = "byte array test with some data".getBytes(StandardCharsets.UTF_8);
        File testFile = createTempFile("writeBytesArr", ".bin");

        IOUtil.write(bytes, testFile);
        assertArrayEquals(bytes, IOUtil.readAllBytes(testFile));

        File testFileOffset = createTempFile("writeBytesArrOffset", ".bin");
        byte[] subBytes = Arrays.copyOfRange(bytes, 5, 10);
        IOUtil.write(bytes, 5, 5, testFileOffset);
        assertArrayEquals(subBytes, IOUtil.readAllBytes(testFileOffset));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.write(bytes, baos, true);
        assertArrayEquals(bytes, baos.toByteArray());

        baos.reset();
        IOUtil.write(bytes, 5, 5, baos, true);
        assertArrayEquals(subBytes, baos.toByteArray());
    }

    @Test
    public void testWriteFromFileToFile() throws IOException {
        String content = "Source file content for writing from file to file.";
        File sourceFile = createTempFileWithContent("sourceWrite", ".txt", content);
        File destFile = createTempFile("destWrite", ".txt");
        destFile.delete();

        long bytesWritten = IOUtil.write(sourceFile, destFile);
        assertTrue(bytesWritten > 0);
        assertEquals(content, IOUtil.readAllToString(destFile));

        File destFileOffset = createTempFile("destWriteOffset", ".txt");
        destFileOffset.delete();
        String expectedPart = content.substring(7, 17);
        bytesWritten = IOUtil.write(sourceFile, 7, 10, destFileOffset);
        assertEquals(10, bytesWritten);
        assertEquals(expectedPart, IOUtil.readAllToString(destFileOffset));
    }

    @Test
    public void testWriteFromFileToOutputStream() throws IOException {
        String content = "Source file content for writing to output stream.";
        File sourceFile = createTempFileWithContent("sourceWriteOS", ".txt", content);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        long bytesWritten = IOUtil.write(sourceFile, baos, true);
        assertTrue(bytesWritten > 0);
        assertEquals(content, baos.toString(Charsets.DEFAULT.name()));

        baos.reset();
        String expectedPart = content.substring(7, 17);
        bytesWritten = IOUtil.write(sourceFile, 7, 10, baos, true);
        assertEquals(10, bytesWritten);
        assertEquals(expectedPart, baos.toString(Charsets.DEFAULT.name()));
    }

    @Test
    public void testWriteFromInputStreamToFile() throws IOException {
        String content = "Input stream content for writing to file.";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        File destFile = createTempFile("destWriteIS", ".txt");
        destFile.delete();

        long bytesWritten = IOUtil.write(is, destFile);
        assertTrue(bytesWritten > 0);
        assertEquals(content, IOUtil.readAllToString(destFile, StandardCharsets.UTF_8));
        is.close();

        is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        File destFileOffset = createTempFile("destWriteISOffset", ".txt");
        destFileOffset.delete();
        String expectedPart = content.substring(6, 16);
        bytesWritten = IOUtil.write(is, 6, 10, destFileOffset);
        assertEquals(10, bytesWritten);
        assertEquals(expectedPart, IOUtil.readAllToString(destFileOffset, StandardCharsets.UTF_8));
        is.close();
    }

    @Test
    public void testWriteFromInputStreamToOutputStream() throws IOException {
        String content = "Input stream content for writing to output stream.";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        long bytesWritten = IOUtil.write(is, baos, true);
        assertTrue(bytesWritten > 0);
        assertEquals(content, baos.toString(StandardCharsets.UTF_8.name()));
        is.close();

        is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        baos.reset();
        String expectedPart = content.substring(6, 16);
        bytesWritten = IOUtil.write(is, 6, 10, baos, true);
        assertEquals(10, bytesWritten);
        assertEquals(expectedPart, baos.toString(StandardCharsets.UTF_8.name()));
        is.close();
    }

    @Test
    public void testWriteFromReaderToFile() throws IOException {
        String content = "Reader content for writing to file. With Ümlauts €.";
        Reader reader = new StringReader(content);
        File destFile = createTempFile("destWriteReader", ".txt");
        destFile.delete();

        long charsWritten = IOUtil.write(reader, StandardCharsets.UTF_8, destFile);
        assertTrue(charsWritten > 0);
        assertEquals(content, IOUtil.readAllToString(destFile, StandardCharsets.UTF_8));
        reader.close();

        reader = new StringReader(content);
        File destFileOffset = createTempFile("destWriteReaderOffset", ".txt");
        destFileOffset.delete();
        String expectedPart = content.substring(7, 17);
        charsWritten = IOUtil.write(reader, 7, 10, StandardCharsets.UTF_8, destFileOffset);
        assertEquals(10, charsWritten);
        assertEquals(expectedPart, IOUtil.readAllToString(destFileOffset, StandardCharsets.UTF_8));
        reader.close();
    }

    @Test
    public void testWriteFromReaderToWriter() throws IOException {
        String content = "Reader content for writing to writer. With Ümlauts €.";
        Reader reader = new StringReader(content);
        StringWriter sw = new StringWriter();

        long charsWritten = IOUtil.write(reader, sw, true);
        assertTrue(charsWritten > 0);
        assertEquals(content, sw.toString());
        reader.close();

        reader = new StringReader(content);
        sw = new StringWriter();
        String expectedPart = content.substring(7, 17);
        charsWritten = IOUtil.write(reader, 7, 10, sw, true);
        assertEquals(10, charsWritten);
        assertEquals(expectedPart, sw.toString());
        reader.close();
    }

    @Test
    public void testAppendBytes() throws IOException {
        File testFile = createTempFile("appendBytes", ".bin");
        byte[] initialBytes = "Initial.".getBytes(StandardCharsets.UTF_8);
        IOUtil.write(initialBytes, testFile);

        byte[] toAppend = " Appended.".getBytes(StandardCharsets.UTF_8);
        IOUtil.append(toAppend, testFile);

        byte[] expected = (new String(initialBytes, StandardCharsets.UTF_8) + new String(toAppend, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, IOUtil.readAllBytes(testFile));

        File testFileOffset = createTempFile("appendBytesOffset", ".bin");
        IOUtil.write(initialBytes, testFileOffset);
        byte[] fullAppend = "Full Appended String".getBytes(StandardCharsets.UTF_8);
        byte[] partToAppend = Arrays.copyOfRange(fullAppend, 5, 13);
        IOUtil.append(fullAppend, 5, 8, testFileOffset);

        expected = (new String(initialBytes, StandardCharsets.UTF_8) + new String(partToAppend, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
        assertArrayEquals(expected, IOUtil.readAllBytes(testFileOffset));
    }

    @Test
    public void testAppendChars() throws IOException {
        File testFile = createTempFile("appendChars", ".txt");
        char[] initialChars = "Initial text.".toCharArray();
        IOUtil.write(initialChars, StandardCharsets.UTF_8, testFile);

        char[] toAppend = " Appended text with Ümlaut €.".toCharArray();
        IOUtil.append(toAppend, StandardCharsets.UTF_8, testFile);

        char[] expected = (new String(initialChars) + new String(toAppend)).toCharArray();
        assertArrayEquals(expected, IOUtil.readAllChars(testFile, StandardCharsets.UTF_8));

        File testFileOffset = createTempFile("appendCharsOffset", ".txt");
        IOUtil.write(initialChars, StandardCharsets.UTF_8, testFileOffset);
        char[] fullAppend = "Full Appended String Chars".toCharArray();
        char[] partToAppend = Arrays.copyOfRange(fullAppend, 5, 13);
        IOUtil.append(fullAppend, 5, 8, StandardCharsets.UTF_8, testFileOffset);

        expected = (new String(initialChars) + new String(partToAppend)).toCharArray();
        assertArrayEquals(expected, IOUtil.readAllChars(testFileOffset, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendCharSequence() throws IOException {
        File testFile = createTempFile("appendCS", ".txt");
        CharSequence initialCS = "Initial CS.";
        IOUtil.write(initialCS.toString().getBytes(StandardCharsets.UTF_8), testFile);

        CharSequence toAppend = " Appended CS with Ümlaut €.";
        IOUtil.append(toAppend, StandardCharsets.UTF_8, testFile);

        String expected = initialCS.toString() + toAppend.toString();
        assertEquals(expected, IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendFromFile() throws IOException {
        File targetFile = createTempFile("targetAppend", ".txt");
        String initialTargetContent = "Target initial. ";
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFile);

        File sourceFile = createTempFileWithContent("sourceAppend", ".txt", "Source content to append.");

        long bytesAppended = IOUtil.append(sourceFile, targetFile);
        assertTrue(bytesAppended > 0);

        String expected = initialTargetContent + "Source content to append.";
        assertEquals(expected, IOUtil.readAllToString(targetFile, StandardCharsets.UTF_8));

        File targetFileOffset = createTempFile("targetAppendOffset", ".txt");
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFileOffset);
        File sourceFileOffset = createTempFileWithContent("sourceAppendOffset", ".txt", "This is the full source for offset append.");
        long bytesAppendedOffset = IOUtil.append(sourceFileOffset, 5, 7, targetFileOffset);
        assertEquals(7, bytesAppendedOffset);
        expected = initialTargetContent + "is the ";
        assertEquals(initialTargetContent + "is the ", IOUtil.readAllToString(targetFileOffset, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendFromInputStream() throws IOException {
        File targetFile = createTempFile("targetAppendIS", ".txt");
        String initialTargetContent = "Target IS initial. ";
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFile);

        String sourceContent = "Source IS content to append. Ümlaut €";
        InputStream sourceIS = new ByteArrayInputStream(sourceContent.getBytes(StandardCharsets.UTF_8));

        long bytesAppended = IOUtil.append(sourceIS, targetFile);
        assertTrue(bytesAppended > 0);
        sourceIS.close();

        String expected = initialTargetContent + sourceContent;
        assertEquals(expected, IOUtil.readAllToString(targetFile, StandardCharsets.UTF_8));

        File targetFileOffset = createTempFile("targetAppendISOffset", ".txt");
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFileOffset);
        String fullSourceContent = "This is the full IS source for offset append.";
        InputStream sourceISOffset = new ByteArrayInputStream(fullSourceContent.getBytes(StandardCharsets.UTF_8));
        long bytesAppendedOffset = IOUtil.append(sourceISOffset, 5, 7, targetFileOffset);
        assertEquals(7, bytesAppendedOffset);
        sourceISOffset.close();

        expected = initialTargetContent + "is the ";
        assertEquals(expected, IOUtil.readAllToString(targetFileOffset, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendFromReader() throws IOException {
        File targetFile = createTempFile("targetAppendReader", ".txt");
        String initialTargetContent = "Target Reader initial. ";
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFile);

        String sourceContent = "Source Reader content to append. Ümlaut €";
        Reader sourceReader = new StringReader(sourceContent);

        long charsAppended = IOUtil.append(sourceReader, StandardCharsets.UTF_8, targetFile);
        assertTrue(charsAppended > 0);
        sourceReader.close();

        String expected = initialTargetContent + sourceContent;
        assertEquals(expected, IOUtil.readAllToString(targetFile, StandardCharsets.UTF_8));

        File targetFileOffset = createTempFile("targetAppendReaderOffset", ".txt");
        IOUtil.write(initialTargetContent, StandardCharsets.UTF_8, targetFileOffset);
        String fullSourceContent = "This is the full Reader source for offset append.";
        Reader sourceReaderOffset = new StringReader(fullSourceContent);
        long charsAppendedOffset = IOUtil.append(sourceReaderOffset, 5, 7, StandardCharsets.UTF_8, targetFileOffset);
        assertEquals(7, charsAppendedOffset);
        sourceReaderOffset.close();

        expected = initialTargetContent + "is the ";
        assertEquals(expected, IOUtil.readAllToString(targetFileOffset, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendLine() throws IOException {
        File testFile = createTempFile("appendLine", ".txt");
        String initialContent = "Initial line." + IOUtil.LINE_SEPARATOR_UNIX;
        IOUtil.write(initialContent, StandardCharsets.UTF_8, testFile);

        Object objToAppend = "Appended line with Ümlaut €";
        IOUtil.appendLine(objToAppend, StandardCharsets.UTF_8, testFile);

        String expected = initialContent + CommonUtil.toString(objToAppend) + IOUtil.LINE_SEPARATOR_UNIX;
        assertEquals(expected, IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testAppendLines() throws IOException {
        File testFile = createTempFile("appendLines", ".txt");
        String initialContent = "First existing line." + IOUtil.LINE_SEPARATOR_UNIX;
        IOUtil.write(initialContent, StandardCharsets.UTF_8, testFile);

        List<String> linesToAppend = Arrays.asList("Append Line 1", "Append Line 2 with Ümlaut €");
        IOUtil.appendLines(linesToAppend, StandardCharsets.UTF_8, testFile);

        StringBuilder expectedSb = new StringBuilder(initialContent);
        for (String line : linesToAppend) {
            expectedSb.append(line).append(IOUtil.LINE_SEPARATOR_UNIX);
        }
        assertEquals(expectedSb.toString(), IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testTransfer() throws IOException {
        byte[] sourceData = "Data for channel transfer.".getBytes(StandardCharsets.UTF_8);
        ReadableByteChannel srcChannel = Channels.newChannel(new ByteArrayInputStream(sourceData));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WritableByteChannel destChannel = Channels.newChannel(baos);

        long bytesTransferred = IOUtil.transfer(srcChannel, destChannel);
        assertEquals(sourceData.length, bytesTransferred);
        assertArrayEquals(sourceData, baos.toByteArray());

        srcChannel.close();
        destChannel.close();
    }

    @Test
    public void testSkip() throws IOException {
        byte[] data = new byte[100];
        for (int i = 0; i < 100; i++)
            data[i] = (byte) i;

        InputStream is = new ByteArrayInputStream(data);
        long skipped = IOUtil.skip(is, 10);
        assertEquals(10, skipped);
        assertEquals(10, is.read());

        skipped = IOUtil.skip(is, 200);
        assertEquals(100 - 11, skipped);
        assertEquals(-1, is.read());
        is.close();

        String strData = "0123456789abcdef";
        Reader reader = new StringReader(strData);
        skipped = IOUtil.skip(reader, 5);
        assertEquals(5, skipped);
        assertEquals('5', reader.read());
        reader.close();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip(new ByteArrayInputStream(data), -1));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip(new StringReader(strData), -1));
    }

    @Test
    public void testSkipFully() throws IOException {
        byte[] data = new byte[50];
        InputStream is = new ByteArrayInputStream(data);
        IOUtil.skipFully(is, 20);
        assertEquals(30, is.available());
        is.close();

        is = new ByteArrayInputStream(data);
        final InputStream finalIs = is;
        assertThrows(IOException.class, () -> IOUtil.skipFully(finalIs, 60));
        is.close();

        String strData = "TestSkipFullyReader";
        Reader reader = new StringReader(strData);
        IOUtil.skipFully(reader, 4);
        char[] remaining = new char[strData.length() - 4];
        reader.read(remaining);
        assertArrayEquals("SkipFullyReader".toCharArray(), remaining);
        reader.close();

        reader = new StringReader(strData);
        final Reader finalReader = reader;
        assertThrows(IOException.class, () -> IOUtil.skipFully(finalReader, strData.length() + 1));
        reader.close();
    }

    @Test
    public void testMapFile() throws IOException {
        String content = "Content to be memory mapped. 12345";
        File testFile = createTempFileWithContent("mapFile", ".txt", content);

        MappedByteBuffer bufferRO = IOUtil.map(testFile);
        assertEquals(content.length(), bufferRO.limit());
        byte[] readBytes = new byte[content.length()];
        bufferRO.get(readBytes);
        assertArrayEquals(content.getBytes(Charsets.DEFAULT), readBytes);

        MappedByteBuffer bufferRW = IOUtil.map(testFile, FileChannel.MapMode.READ_WRITE, 0, testFile.length());
        assertEquals(content.length(), bufferRW.limit());
        bufferRW.put(0, (byte) 'X');
        bufferRW.force();

        File nonExistent = new File(tempDir.toFile(), "nonExistentMap.txt");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(nonExistent));

        unmap(bufferRO);
        unmap(bufferRW);
    }

    @Test
    public void testSimplifyPath() {
        assertEquals(".", IOUtil.simplifyPath(""));
        assertEquals(".", IOUtil.simplifyPath("."));
        assertEquals("/", IOUtil.simplifyPath("/"));
        assertEquals("a/b", IOUtil.simplifyPath("a/b"));
        assertEquals("a/b", IOUtil.simplifyPath("a//b"));
        assertEquals("a/b", IOUtil.simplifyPath("a/./b"));
        assertEquals("b", IOUtil.simplifyPath("a/../b"));
        assertEquals("../b", IOUtil.simplifyPath("../b"));
        assertEquals("/b", IOUtil.simplifyPath("/../b"));
        assertEquals("a", IOUtil.simplifyPath("a/b/.."));
        assertEquals("/", IOUtil.simplifyPath("/a/../.."));
        assertEquals("../..", IOUtil.simplifyPath("../../"));
        assertEquals("c/d", IOUtil.simplifyPath("./a/../b/../c/./d"));
        assertEquals("a", IOUtil.simplifyPath("a/"));
        assertEquals("a/b", IOUtil.simplifyPath("a/b/"));
    }

    @Test
    public void testGetFileExtension() {
        assertEquals("txt", IOUtil.getFileExtension("file.txt"));
        assertEquals("gz", IOUtil.getFileExtension("archive.tar.gz"));
        assertEquals("", IOUtil.getFileExtension("file"));
        assertEquals("", IOUtil.getFileExtension("file."));
        assertNull(IOUtil.getFileExtension((String) null));

        File fTxt = new File("test.txt");
        assertEquals("txt", IOUtil.getFileExtension(fTxt));
        File fNoExt = new File("test");
        assertEquals("", IOUtil.getFileExtension(fNoExt));
        assertNull(IOUtil.getFileExtension((File) null));
        File nonExistentFile = new File(tempDir.toFile(), "non.existent");
        assertEquals("existent", IOUtil.getFileExtension(nonExistentFile));
        assertEquals("existent", com.landawn.abacus.guava.Files.getFileExtension(nonExistentFile.toPath()));
    }

    @Test
    public void testGetNameWithoutExtension() {
        assertEquals("file", IOUtil.getNameWithoutExtension("file.txt"));
        assertEquals("archive.tar", IOUtil.getNameWithoutExtension("archive.tar.gz"));
        assertEquals("file", IOUtil.getNameWithoutExtension("file"));
        assertEquals("file", IOUtil.getNameWithoutExtension("file."));
        assertNull(IOUtil.getNameWithoutExtension((String) null));

        File fTxt = new File("test.txt");
        assertEquals("test", IOUtil.getNameWithoutExtension(fTxt));
        File fNoExt = new File("test");
        assertEquals("test", IOUtil.getNameWithoutExtension(fNoExt));
        assertNull(IOUtil.getNameWithoutExtension((File) null));
        File nonExistentFile = new File(tempDir.toFile(), "non.existent");
        assertEquals("non", IOUtil.getNameWithoutExtension(nonExistentFile));
    }

    @Test
    public void testNewStreamsAndReadersWriters() throws IOException {

        File testFile = createTempFileWithContent("newStreams", ".txt", "data");

        InputStream is = new ByteArrayInputStream(new byte[0]);
        assertTrue(IOUtil.newInputStreamReader(is) instanceof InputStreamReader);
        assertTrue(IOUtil.newInputStreamReader(is, StandardCharsets.UTF_8) instanceof InputStreamReader);
        is.close();

        OutputStream os = new ByteArrayOutputStream();
        assertTrue(IOUtil.newOutputStreamWriter(os) instanceof OutputStreamWriter);
        assertTrue(IOUtil.newOutputStreamWriter(os, StandardCharsets.UTF_8) instanceof OutputStreamWriter);
        os.close();

        is = new ByteArrayInputStream(new byte[] { 31, -117, 8, 0, 0, 0, 0, 0, 0, 0 });
        assertTrue(IOUtil.newGZIPInputStream(is) instanceof GZIPInputStream);
        is.close();

        os = new ByteArrayOutputStream();
        assertTrue(IOUtil.newGZIPOutputStream(os) instanceof GZIPOutputStream);
        os.close();

        is = new ByteArrayInputStream(new byte[0]);
        is.close();

        os = new ByteArrayOutputStream();
        assertTrue(IOUtil.newZipOutputStream(os) instanceof ZipOutputStream);
        os.close();

    }

    @Test
    public void testCloseOperations() {
        MockCloseable mc = new MockCloseable();
        IOUtil.close(mc);
        assertTrue(mc.isClosed());

        final MockCloseable mcThrows = new MockCloseable(true);
        assertThrows(RuntimeException.class, () -> IOUtil.close(mcThrows));
        assertTrue(mcThrows.isClosed());

        mc = new MockCloseable();
        AtomicInteger exceptionCount = new AtomicInteger(0);
        IOUtil.close(mc, e -> exceptionCount.incrementAndGet());
        assertTrue(mc.isClosed());
        assertEquals(0, exceptionCount.get());

        final MockCloseable mcThrows2 = new MockCloseable(true);
        IOUtil.close(mcThrows2, e -> exceptionCount.incrementAndGet());
        assertTrue(mcThrows2.isClosed());
        assertEquals(1, exceptionCount.get());

        mc = new MockCloseable();
        IOUtil.closeQuietly(mc);
        assertTrue(mc.isClosed());

        final MockCloseable mcThrows3 = new MockCloseable(true);
        assertDoesNotThrow(() -> IOUtil.closeQuietly(mcThrows3));
        assertTrue(mcThrows3.isClosed());

        MockCloseable mc1 = new MockCloseable();
        MockCloseable mc2 = new MockCloseable();
        IOUtil.closeAll(mc1, mc2, null);
        assertTrue(mc1.isClosed());
        assertTrue(mc2.isClosed());

        MockCloseable mc3Throws = new MockCloseable(true);
        MockCloseable mc4 = new MockCloseable();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(mc3Throws, mc4));
        assertTrue(mc3Throws.isClosed());
        assertTrue(mc4.isClosed());
        assertEquals(1, ex.getSuppressed().length + (ex.getCause() == null ? 0 : 1));

        MockCloseable mc11 = new MockCloseable();
        MockCloseable mc22 = new MockCloseable(true);
        assertDoesNotThrow(() -> IOUtil.closeAllQuietly(mc11, mc22, null));
        assertTrue(mc11.isClosed());
        assertTrue(mc22.isClosed());

        URLConnection mockConn = new URLConnection(null) {
            @Override
            public void connect() throws IOException {
            }
        };
        IOUtil.close(mockConn);

    }

    private static class MockCloseable implements AutoCloseable {
        private boolean closed = false;
        private boolean throwOnClose = false;
        private String name;

        MockCloseable() {
            this(false);
        }

        MockCloseable(boolean throwOnClose) {
            this.throwOnClose = throwOnClose;
        }

        public MockCloseable(boolean b, String name) {
            this.throwOnClose = b;
            this.name = name;
        }

        @Override
        public void close() throws Exception {
            closed = true;
            if (throwOnClose) {
                throw new IOException("Mock close exception: " + (name != null ? name : "Unnamed"));
            }
        }

        public boolean isClosed() {
            return closed;
        }
    }

    @Test
    public void testCopyToDirectory() throws IOException, Exception {
        File srcFile = createTempFileWithContent("srcCopy", ".txt", "Source content.");
        File destDir = createTempDirectory("destCopyDir");

        IOUtil.copyToDirectory(srcFile, destDir);
        File copiedFile = new File(destDir, srcFile.getName());
        assertTrue(copiedFile.exists());
        assertEquals("Source content.", IOUtil.readAllToString(copiedFile));

        File srcFile2 = createTempFileWithContent("srcCopyDate", ".txt", "Date test.");
        File destDir2 = createTempDirectory("destCopyDirDate");
        IOUtil.copyToDirectory(srcFile2, destDir2, true);
        assertTrue(new File(destDir2, srcFile2.getName()).exists());

        File srcDir = createTempDirectory("srcDirToCopy");
        File tempFile1 = createTempFileWithContent(srcDir.toPath().resolve("file1.txt").toString(), null, "File 1 in dir");
        File tempFile2 = createTempFileWithContent(srcDir.toPath().resolve("file2.txt").toString(), null, "File 2 in dir");
        File subDir = new File(srcDir, "subdir");
        subDir.mkdirs();
        File tempFile3 = createTempFileWithContent(subDir.toPath().resolve("file3.txt").toString(), null, "File 3 in subdir");

        File destDir3 = createTempDirectory("destDirForDirCopy");
        IOUtil.copyToDirectory(srcDir, destDir3);

        assertTrue(new File(destDir3, srcDir.getName() + "/" + tempFile1.getName()).exists());
        assertTrue(new File(destDir3, srcDir.getName() + "/" + tempFile2.getName()).exists());
        assertTrue(new File(destDir3, srcDir.getName() + "/subdir/" + tempFile3.getName()).exists());

        File srcDirFiltered = createTempDirectory("srcDirFiltered");
        File fileA = new File(srcDirFiltered, "copyA.txt");
        IOUtil.write("AAA", fileA);
        File fileB = new File(srcDirFiltered, "skipB.txt");
        IOUtil.write("BBB", fileB);
        File destDirFiltered = createTempDirectory("destDirFiltered");

        Throwables.BiPredicate<File, File, IOException> filter = (parent, file) -> file.getName().startsWith("copy");
        IOUtil.copyToDirectory(srcDirFiltered, destDirFiltered, true, filter);

        File copiedA = new File(new File(destDirFiltered, srcDirFiltered.getName()), "copyA.txt");
        File skippedB = new File(new File(destDirFiltered, srcDirFiltered.getName()), "skipB.txt");
        assertTrue(copiedA.exists());
        assertFalse(skippedB.exists());
    }

    @Test
    public void testCopyFile() throws IOException {
        File srcFile = createTempFileWithContent("srcCopyFile", ".txt", "Content for copyFile.");
        File destFile = new File(tempDir.toFile(), "destCopyFile.txt");

        IOUtil.copyFile(srcFile, destFile);
        assertTrue(destFile.exists());
        assertEquals("Content for copyFile.", IOUtil.readAllToString(destFile));

        File srcFileDate = createTempFileWithContent("srcCopyFileDate", ".txt", "Date content.");
        File destFileDate = new File(tempDir.toFile(), "destCopyFileDate.txt");
        IOUtil.copyFile(srcFileDate, destFileDate, true);
        assertTrue(destFileDate.exists());

        File destFileOverwrite = new File(tempDir.toFile(), "destCopyFileOverwrite.txt");
        IOUtil.write("Old content", destFileOverwrite);
        IOUtil.copyFile(srcFile, destFileOverwrite, StandardCopyOption.REPLACE_EXISTING);
        assertEquals("Content for copyFile.", IOUtil.readAllToString(destFileOverwrite));
    }

    @Test
    public void testCopyFileToOutputStream() throws IOException {
        File srcFile = createTempFileWithContent("srcCopyFileOS", ".txt", "Copy to OS content.");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        long bytesCopied = IOUtil.copyFile(srcFile, baos);
        assertEquals(srcFile.length(), bytesCopied);
        assertEquals("Copy to OS content.", baos.toString(Charsets.DEFAULT.name()));
    }

    @Test
    @Disabled("Requires network or local HTTP server setup")
    public void testCopyURLToFile() throws IOException {
        File tempSource = createTempFileWithContent("urlSource", ".txt", "URL source content.");
        URL fileUrl = tempSource.toURI().toURL();
        File destFile = new File(tempDir.toFile(), "destFromUrl.txt");

        IOUtil.copyURLToFile(fileUrl, destFile);
        assertTrue(destFile.exists());
        assertEquals("URL source content.", IOUtil.readAllToString(destFile));

    }

    @Test
    public void testNioCopyOperations() throws IOException {
        Path srcPath = createTempFileWithContent("nioSrc", ".txt", "NIO copy source.").toPath();
        Path destPath = tempDir.resolve("nioDest.txt");

        IOUtil.copy(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(destPath));
        assertEquals("NIO copy source.", Files.readString(destPath));

        Path destFromISPath = tempDir.resolve("nioDestFromIS.txt");
        InputStream is = new ByteArrayInputStream("NIO IS copy.".getBytes(StandardCharsets.UTF_8));
        IOUtil.copy(is, destFromISPath, StandardCopyOption.REPLACE_EXISTING);
        assertTrue(Files.exists(destFromISPath));
        assertEquals("NIO IS copy.", Files.readString(destFromISPath));
        is.close();

        Path srcForOSPath = createTempFileWithContent("nioSrcForOS", ".txt", "NIO copy to OS.").toPath();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        IOUtil.copy(srcForOSPath, baos);
        assertEquals("NIO copy to OS.", baos.toString(Charsets.DEFAULT.name()));
    }

    @Test
    public void testMoveFile() throws IOException {
        File srcFile = createTempFileWithContent("srcMove", ".txt", "File to move.");
        File destDir = createTempDirectory("destMoveDir");

        IOUtil.move(srcFile, destDir);

        File movedFile = new File(destDir, srcFile.getName());
        assertFalse(srcFile.exists());
        assertTrue(movedFile.exists());
        assertEquals("File to move.", IOUtil.readAllToString(movedFile));
    }

    @Test
    public void testNioMove() throws IOException {
        Path srcPath = createTempFileWithContent("nioSrcMove", ".txt", "NIO file to move.").toPath();
        Path destPath = tempDir.resolve("nioDestMove.txt");

        IOUtil.move(srcPath, destPath, StandardCopyOption.REPLACE_EXISTING);
        assertFalse(Files.exists(srcPath));
        assertTrue(Files.exists(destPath));
        assertEquals("NIO file to move.", Files.readString(destPath));
    }

    @Test
    public void testRenameTo() throws IOException {
        File srcFile = createTempFileWithContent("srcRename", ".txt", "File to rename.");
        String newName = "renamedFile.txt";

        boolean success = IOUtil.renameTo(srcFile, newName);
        assertTrue(success);

        File renamedFile = new File(srcFile.getParentFile(), newName);
        assertFalse(srcFile.exists());
        assertTrue(renamedFile.exists());
        assertEquals("File to rename.", IOUtil.readAllToString(renamedFile));
    }

    @Test
    public void testDeleteOperations() throws IOException {
        File toDelete = createTempFileWithContent("toDelete", ".txt", "content");
        assertTrue(IOUtil.deleteIfExists(toDelete));
        assertFalse(toDelete.exists());
        assertFalse(IOUtil.deleteIfExists(toDelete));

        File toDeleteQuietly = createTempFileWithContent("toDeleteQuietly", ".txt", "content");
        assertTrue(IOUtil.deleteQuietly(toDeleteQuietly));
        assertFalse(toDeleteQuietly.exists());
        File nonExistent = new File(tempDir.toFile(), "nonexist.del");
        assertFalse(IOUtil.deleteQuietly(nonExistent));

        File toDeleteAllFile = createTempFileWithContent("delAllFile", ".txt", "data");
        assertTrue(IOUtil.deleteRecursivelyIfExists(toDeleteAllFile));
        assertFalse(toDeleteAllFile.exists());

        File dirToDelete = createTempDirectory("dirDelAll");
        createTempFileWithContent(dirToDelete.toPath().resolve("f1.txt").toString(), null, "d1");
        File subDir = new File(dirToDelete, "subDel");
        subDir.mkdir();
        createTempFileWithContent(subDir.toPath().resolve("f2.txt").toString(), null, "d2");

        assertTrue(IOUtil.deleteRecursivelyIfExists(dirToDelete));
        assertFalse(dirToDelete.exists());

        File dirToClean = createTempDirectory("dirClean");
        File fInClean1 = new File(dirToClean, "clean1.txt");
        IOUtil.write("c1", fInClean1);
        File fInClean2 = new File(dirToClean, "clean2.txt");
        IOUtil.write("c2", fInClean2);
        File subDirInClean = new File(dirToClean, "subClean");
        subDirInClean.mkdir();
        File fInSubClean = new File(subDirInClean, "subf.txt");
        IOUtil.write("sc", fInSubClean);

        assertTrue(IOUtil.cleanDirectory(dirToClean));
        assertTrue(dirToClean.exists());
        assertEquals(0, dirToClean.listFiles().length);

        File dirToCleanFiltered = createTempDirectory("dirCleanFilter");
        File copyF = new File(dirToCleanFiltered, "copy.txt");
        IOUtil.write("copy", copyF);
        File keepF = new File(dirToCleanFiltered, "keep.txt");
        IOUtil.write("keep", keepF);
        Throwables.BiPredicate<File, File, IOException> filter = (p, f) -> f.getName().startsWith("copy");
        IOUtil.deleteFilesFromDirectory(dirToCleanFiltered, filter);
        assertFalse(copyF.exists());
        assertTrue(keepF.exists());
    }

    @Test
    public void testCreateIfNotExists() throws IOException {
        File newFile = new File(tempDir.toFile(), "createTest.txt");
        assertFalse(newFile.exists());
        assertTrue(IOUtil.createFileIfNotExists(newFile));
        assertTrue(newFile.exists());
        assertFalse(IOUtil.createFileIfNotExists(newFile));
    }

    @Test
    public void testMkdirIfNotExists() throws IOException {
        File newDir = new File(tempDir.toFile(), "mkdirTest");
        assertFalse(newDir.exists());
        assertTrue(IOUtil.mkdirIfNotExists(newDir));
        assertTrue(newDir.exists() && newDir.isDirectory());
        assertFalse(IOUtil.mkdirIfNotExists(newDir));
    }

    @Test
    public void testMkdirsIfNotExists() throws IOException {
        File deepDir = new File(tempDir.toFile(), "mkdirsTest/level2/level3");
        assertFalse(deepDir.exists());
        assertTrue(IOUtil.mkdirsIfNotExists(deepDir));
        assertTrue(deepDir.exists() && deepDir.isDirectory());
        assertFalse(IOUtil.mkdirsIfNotExists(deepDir));
    }

    @Test
    public void testIsBufferedChecks() {
        assertTrue(IOUtil.isBufferedReader(new BufferedReader(new StringReader(""))));
        assertFalse(IOUtil.isBufferedReader(new StringReader("")));
        assertTrue(IOUtil.isBufferedWriter(new BufferedWriter(new StringWriter())));
        assertFalse(IOUtil.isBufferedWriter(new StringWriter()));
    }

    @Test
    public void testIsFileNewerOlder() throws IOException, InterruptedException {
        File file1 = createTempFileWithContent("fileDate1", ".txt", "f1");
        Thread.sleep(10);
        File file2 = createTempFileWithContent("fileDate2", ".txt", "f2");
        Thread.sleep(10);
        Date dateInBetween = new Date(file1.lastModified() + (file2.lastModified() - file1.lastModified()) / 2);
        Date dateAfter = new Date(System.currentTimeMillis() + 1000);

        assertTrue(IOUtil.isFileNewer(file2, file1));
        assertFalse(IOUtil.isFileNewer(file1, file2));
        assertTrue(IOUtil.isFileNewer(file2, dateInBetween));
        assertFalse(IOUtil.isFileNewer(file1, dateInBetween));

        assertTrue(IOUtil.isFileOlder(file1, file2));
        assertFalse(IOUtil.isFileOlder(file2, file1));
        assertTrue(IOUtil.isFileOlder(file1, dateInBetween));
        assertFalse(IOUtil.isFileOlder(file2, dateInBetween));
        assertTrue(IOUtil.isFileOlder(file2, dateAfter));
    }

    @Test
    public void testIsFileIsDirectory() throws IOException {
        File testFile = createTempFileWithContent("isFileTest", ".txt", "data");
        File testDir = createTempDirectory("isDirTest");

        assertTrue(IOUtil.isFile(testFile));
        assertFalse(IOUtil.isFile(testDir));
        assertFalse(IOUtil.isFile(null));

        assertTrue(IOUtil.isDirectory(testDir));
        assertFalse(IOUtil.isDirectory(testFile));
        assertFalse(IOUtil.isDirectory(null));

        assertTrue(IOUtil.isDirectory(testDir, LinkOption.NOFOLLOW_LINKS));
        assertTrue(IOUtil.isRegularFile(testFile, LinkOption.NOFOLLOW_LINKS));
        assertFalse(IOUtil.isSymbolicLink(testFile));
    }

    @Test
    public void testSizeOf() throws IOException {
        File emptyFile = createTempFileWithContent("sizeEmpty", ".txt", "");
        assertEquals(0, IOUtil.sizeOf(emptyFile));

        byte[] content = new byte[123];
        File sizedFile = createTempFileWithContent("sizeData", ".bin", content);
        assertEquals(123, IOUtil.sizeOf(sizedFile));

        File dir = createTempDirectory("sizeDir");
        assertEquals(0, IOUtil.sizeOfDirectory(dir));

        File fileInDir1 = new File(dir, "f1.bin");
        IOUtil.write(new byte[50], fileInDir1);
        File fileInDir2 = new File(dir, "f2.bin");
        IOUtil.write(new byte[70], fileInDir2);
        assertEquals(120, IOUtil.sizeOfDirectory(dir));
        assertEquals(120, IOUtil.sizeOf(dir));

        File subDir = new File(dir, "sub");
        subDir.mkdir();
        File fileInSubDir = new File(subDir, "f3.bin");
        IOUtil.write(new byte[30], fileInSubDir);
        assertEquals(150, IOUtil.sizeOfDirectory(dir));

        File nonExistent = new File(tempDir.toFile(), "nonExistentSize.txt");
        assertEquals(0, IOUtil.sizeOf(nonExistent, true));
        assertThrows(FileNotFoundException.class, () -> IOUtil.sizeOf(nonExistent, false));

        File nonExistentDir = new File(tempDir.toFile(), "nonExistentDirSize");
        assertEquals(0, IOUtil.sizeOfDirectory(nonExistentDir, true));
        assertThrows(FileNotFoundException.class, () -> IOUtil.sizeOfDirectory(nonExistentDir, false));
    }

    @Test
    public void testSizeOfAsBigInteger() throws IOException {
        File emptyFile = createTempFileWithContent("sizeBigEmpty", ".txt", "");
        assertEquals(0, IOUtil.sizeOfAsBigInteger(emptyFile).longValue());

        byte[] content = new byte[200];
        File sizedFile = createTempFileWithContent("sizeBigData", ".bin", content);
        assertEquals(200, IOUtil.sizeOfAsBigInteger(sizedFile).longValue());

        File dir = createTempDirectory("sizeBigDir");
        File fileInDir1 = new File(dir, "bf1.bin");
        IOUtil.write(new byte[100], fileInDir1);
        File fileInDir2 = new File(dir, "bf2.bin");
        IOUtil.write(new byte[150], fileInDir2);
        File subDir = new File(dir, "bsub");
        subDir.mkdir();
        File fileInSubDir = new File(subDir, "bf3.bin");
        IOUtil.write(new byte[80], fileInSubDir);
        assertEquals(330, IOUtil.sizeOfDirectoryAsBigInteger(dir).longValue());
        assertEquals(330, IOUtil.sizeOfAsBigInteger(dir).longValue());
    }

    @Test
    public void testToFileToURL() throws IOException {
        File testFile = createTempFileWithContent("toUrlTest", ".txt", "data");
        URL url = IOUtil.toURL(testFile);
        assertNotNull(url);
        assertEquals("file", url.getProtocol());

        File convertedFile = IOUtil.toFile(url);
        assertEquals(testFile.getCanonicalPath(), convertedFile.getCanonicalPath());

        URL[] urls = IOUtil.toURLs(new File[] { testFile });
        assertEquals(1, urls.length);
        assertEquals(url.toString(), urls[0].toString());

        File[] files = IOUtil.toFiles(new URL[] { url });
        assertEquals(1, files.length);
        assertEquals(testFile.getCanonicalPath(), files[0].getCanonicalPath());

        File spaceFile = new File(tempDir.toFile(), "file with spaces.txt");
        IOUtil.write("space test", spaceFile);
        URL spaceUrl = spaceFile.toURI().toURL();
        File decodedFile = IOUtil.toFile(spaceUrl);
        assertEquals(spaceFile.getCanonicalPath(), decodedFile.getCanonicalPath());
    }

    @Test
    public void testTouch() throws IOException, InterruptedException {
        File testFile = createTempFileWithContent("touchTest", ".txt", "content");
        long originalTime = testFile.lastModified();
        Thread.sleep(10);
        assertTrue(IOUtil.touch(testFile));
        assertTrue(testFile.lastModified() > originalTime);

        File nonExistent = new File(tempDir.toFile(), "touchNonExistent.txt");
        assertFalse(IOUtil.touch(nonExistent));
    }

    @Test
    public void testContentEqualsFile() throws IOException {
        File file1a = createTempFileWithContent("ce1a", ".txt", "Same content");
        File file1b = createTempFileWithContent("ce1b", ".txt", "Same content");
        File file2 = createTempFileWithContent("ce2", ".txt", "Different content");
        File empty1 = createTempFileWithContent("ceEmpty1", ".txt", "");
        File empty2 = createTempFileWithContent("ceEmpty2", ".txt", "");

        assertTrue(IOUtil.contentEquals(file1a, file1b));
        assertFalse(IOUtil.contentEquals(file1a, file2));
        assertTrue(IOUtil.contentEquals(empty1, empty2));
        assertFalse(IOUtil.contentEquals(file1a, empty1));

        File nonExistent1 = new File(tempDir.toFile(), "nonEx1.txt");
        File nonExistent2 = new File(tempDir.toFile(), "nonEx2.txt");
        assertTrue(IOUtil.contentEquals(nonExistent1, nonExistent2));
        assertFalse(IOUtil.contentEquals(file1a, nonExistent1));
    }

    @Test
    public void testContentEqualsIgnoreEOLFile() throws IOException {
        String contentUnix = "Line1\nLine2\n";
        String contentWindows = "Line1\r\nLine2\r\n";
        String contentMac = "Line1\rLine2\r";
        String contentMixed = "Line1\nLine2\r\n";
        String differentContent = "Line1\nOtherLine\n";

        File fileUnix = createTempFileWithContent("ceEOLUnix", ".txt", contentUnix);
        File fileWindows = createTempFileWithContent("ceEOLWin", ".txt", contentWindows);
        File fileMac = createTempFileWithContent("ceEOLMac", ".txt", contentMac);
        File fileMixed = createTempFileWithContent("ceEOLMix", ".txt", contentMixed);
        File fileDiff = createTempFileWithContent("ceEOLDiff", ".txt", differentContent);

        assertTrue(IOUtil.contentEqualsIgnoreEOL(fileUnix, fileWindows, null));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(fileUnix, fileMac, StandardCharsets.UTF_8.name()));
        assertTrue(IOUtil.contentEqualsIgnoreEOL(fileWindows, fileMixed, StandardCharsets.ISO_8859_1.name()));
        assertFalse(IOUtil.contentEqualsIgnoreEOL(fileUnix, fileDiff, null));
    }

    @Test
    public void testContentEqualsStream() throws IOException {
        byte[] data1 = "Stream content".getBytes();
        byte[] data2 = "Stream content".getBytes();
        byte[] data3 = "Different stream".getBytes();

        assertTrue(IOUtil.contentEquals(new ByteArrayInputStream(data1), new ByteArrayInputStream(data2)));
        assertFalse(IOUtil.contentEquals(new ByteArrayInputStream(data1), new ByteArrayInputStream(data3)));
        assertTrue(IOUtil.contentEquals(new ByteArrayInputStream(new byte[0]), new ByteArrayInputStream(new byte[0])));
        assertFalse(IOUtil.contentEquals(new ByteArrayInputStream(data1), new ByteArrayInputStream(new byte[0])));
    }

    @Test
    public void testContentEqualsReader() throws IOException {
        String str1 = "Reader content";
        String str2 = "Reader content";
        String str3 = "Different reader";

        assertTrue(IOUtil.contentEquals(new StringReader(str1), new StringReader(str2)));
        assertFalse(IOUtil.contentEquals(new StringReader(str1), new StringReader(str3)));
        assertTrue(IOUtil.contentEquals(new StringReader(""), new StringReader("")));
        assertFalse(IOUtil.contentEquals(new StringReader(str1), new StringReader("")));
    }

    @Test
    public void testContentEqualsIgnoreEOLReader() throws IOException {
        String r1 = "LineA\nLineB";
        String r2 = "LineA\r\nLineB";
        String r3 = "LineA\nOther";

        assertTrue(IOUtil.contentEqualsIgnoreEOL(new StringReader(r1), new StringReader(r2)));
        assertFalse(IOUtil.contentEqualsIgnoreEOL(new StringReader(r1), new StringReader(r3)));
    }

    @Test
    public void testZipAndUnzip() throws IOException {
        File dirToZip = createTempDirectory("zipTestDir");
        File file1 = new File(dirToZip, "file1.txt");
        IOUtil.write("Content of file1 for zip Ümlaut €", StandardCharsets.UTF_8, file1);
        File subDir = new File(dirToZip, "subfolder");
        subDir.mkdirs();
        File file2 = new File(subDir, "file2.dat");
        IOUtil.write(new byte[] { 1, 2, 3, 4, 5 }, file2);

        File zipFile = new File(tempDir.toFile(), "testArchive.zip");
        IOUtil.zip(dirToZip, zipFile);
        assertTrue(zipFile.exists() && zipFile.length() > 0);

        File unzipDir = createTempDirectory("unzipTestDir");
        IOUtil.unzip(zipFile, unzipDir);

        File unzippedDir = new File(unzipDir, dirToZip.getName());
        assertTrue(unzippedDir.exists() && unzippedDir.isDirectory());

        File unzippedFile1 = new File(unzippedDir, "file1.txt");
        assertTrue(unzippedFile1.exists());
        assertEquals("Content of file1 for zip Ümlaut €", IOUtil.readAllToString(unzippedFile1, StandardCharsets.UTF_8));

        File unzippedSubDir = new File(unzippedDir, "subfolder");
        assertTrue(unzippedSubDir.exists() && unzippedSubDir.isDirectory());
        File unzippedFile2 = new File(unzippedSubDir, "file2.dat");
        assertTrue(unzippedFile2.exists());
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, IOUtil.readAllBytes(unzippedFile2));

        File zipFileCollection = new File(tempDir.toFile(), "testArchiveCol.zip");
        IOUtil.zip(Arrays.asList(file1, file2), zipFileCollection);
        assertTrue(zipFileCollection.exists() && zipFileCollection.length() > 0);

        File unzipDirCol = createTempDirectory("unzipTestDirCol");
        IOUtil.unzip(zipFileCollection, unzipDirCol);
        assertTrue(new File(unzipDirCol, "file1.txt").exists());
        assertTrue(new File(unzipDirCol, "file2.dat").exists());
    }

    @Test
    public void testSplitBySize() throws IOException {
        byte[] content = new byte[100];
        for (int i = 0; i < 100; i++)
            content[i] = (byte) i;
        File sourceFile = createTempFileWithContent("splitSource", ".dat", content);
        File destDir = createTempDirectory("splitDest");

        IOUtil.splitBySize(sourceFile, 30, destDir);

        File part1 = new File(destDir, sourceFile.getName() + "_0001");
        File part2 = new File(destDir, sourceFile.getName() + "_0002");
        File part3 = new File(destDir, sourceFile.getName() + "_0003");
        File part4 = new File(destDir, sourceFile.getName() + "_0004");

        assertTrue(part1.exists() && part1.length() == 30);
        assertTrue(part2.exists() && part2.length() == 30);
        assertTrue(part3.exists() && part3.length() == 30);
        assertTrue(part4.exists() && part4.length() == 10);

        assertArrayEquals(Arrays.copyOfRange(content, 0, 30), IOUtil.readAllBytes(part1));
        assertArrayEquals(Arrays.copyOfRange(content, 30, 60), IOUtil.readAllBytes(part2));
        assertArrayEquals(Arrays.copyOfRange(content, 60, 90), IOUtil.readAllBytes(part3));
        assertArrayEquals(Arrays.copyOfRange(content, 90, 100), IOUtil.readAllBytes(part4));
    }

    @Test
    public void testSplitByCount() throws IOException {
        byte[] content = new byte[105];
        for (int i = 0; i < 105; i++)
            content[i] = (byte) i;
        File sourceFile = createTempFileWithContent("splitCountSource", ".dat", content);
        File destDir = createTempDirectory("splitCountDest");

        IOUtil.split(sourceFile, 4, destDir);

        File part1 = new File(destDir, sourceFile.getName() + "_0001");
        File part2 = new File(destDir, sourceFile.getName() + "_0002");
        File part3 = new File(destDir, sourceFile.getName() + "_0003");
        File part4 = new File(destDir, sourceFile.getName() + "_0004");

        assertTrue(part1.exists() && part1.length() == 27);
        assertTrue(part2.exists() && part2.length() == 27);
        assertTrue(part3.exists() && part3.length() == 27);
        assertTrue(part4.exists() && part4.length() == 24);
    }

    @Test
    public void testMergeFiles() throws IOException {
        File file1 = createTempFileWithContent("merge1", ".txt", "Content1");
        File file2 = createTempFileWithContent("merge2", ".txt", "Content2Ü");
        File file3 = createTempFileWithContent("merge3", ".txt", "Content3€");
        File destFile = new File(tempDir.toFile(), "mergedFile.txt");
        byte[] delimiter = "--DELIMITER--".getBytes(StandardCharsets.UTF_8);

        List<File> sourceFiles = Arrays.asList(file1, file2, file3);
        long totalBytes = IOUtil.merge(sourceFiles, delimiter, destFile);
        N.println(IOUtil.readAllToString(destFile, StandardCharsets.UTF_8));

        String expectedContent = "Content1" + new String(delimiter, StandardCharsets.UTF_8) + "Content2Ü" + new String(delimiter, StandardCharsets.UTF_8)
                + "Content3€";
        assertEquals(expectedContent.getBytes(StandardCharsets.UTF_8).length, totalBytes);
        assertEquals(expectedContent, IOUtil.readAllToString(destFile, StandardCharsets.UTF_8));

        File destFileNoDelim = new File(tempDir.toFile(), "mergedFileNoDelim.txt");
        IOUtil.merge(sourceFiles, destFileNoDelim);
        String expectedNoDelim = "Content1Content2ÜContent3€";
        assertEquals(expectedNoDelim, IOUtil.readAllToString(destFileNoDelim, StandardCharsets.UTF_8));
    }

    @Test
    public void testListFilesAndDirectories() throws IOException {
        File baseDir = createTempDirectory("listTestBase");
        File f1 = new File(baseDir, "file1.txt");
        IOUtil.write("f1", f1);
        File d1 = new File(baseDir, "dir1");
        d1.mkdir();
        File f2InD1 = new File(d1, "file2.txt");
        IOUtil.write("f2", f2InD1);
        File d2InD1 = new File(d1, "dir2");
        d2InD1.mkdir();
        File f3InD2 = new File(d2InD1, "file3.txt");
        IOUtil.write("f3", f3InD2);

        List<String> namesNonRecursive = IOUtil.walk(baseDir).map(File::getAbsolutePath).toList();
        assertTrue(namesNonRecursive.contains(f1.getAbsolutePath()));
        assertTrue(namesNonRecursive.contains(d1.getAbsolutePath()));
        assertEquals(2, namesNonRecursive.size());

        List<String> namesRecursive = IOUtil.walk(baseDir, true, false).map(File::getAbsolutePath).toList();
        assertTrue(namesRecursive.contains(f1.getAbsolutePath()));
        assertTrue(namesRecursive.contains(d1.getAbsolutePath()));
        assertTrue(namesRecursive.contains(f2InD1.getAbsolutePath()));
        assertTrue(namesRecursive.contains(d2InD1.getAbsolutePath()));
        assertTrue(namesRecursive.contains(f3InD2.getAbsolutePath()));
        assertEquals(5, namesRecursive.size());

        List<String> namesRecursiveExcludeDirs = IOUtil.walk(baseDir, true, true).map(File::getAbsolutePath).toList();
        assertTrue(namesRecursiveExcludeDirs.contains(f1.getAbsolutePath()));
        assertFalse(namesRecursiveExcludeDirs.contains(d1.getAbsolutePath()));
        assertTrue(namesRecursiveExcludeDirs.contains(f2InD1.getAbsolutePath()));
        assertFalse(namesRecursiveExcludeDirs.contains(d2InD1.getAbsolutePath()));
        assertTrue(namesRecursiveExcludeDirs.contains(f3InD2.getAbsolutePath()));
        assertEquals(3, namesRecursiveExcludeDirs.size());

        List<File> filesNonRecursive = IOUtil.listFiles(baseDir);
        assertTrue(filesNonRecursive.stream().anyMatch(f -> f.equals(f1)));
        assertTrue(filesNonRecursive.stream().anyMatch(f -> f.equals(d1)));
        assertEquals(2, filesNonRecursive.size());

        List<File> dirsOnly = IOUtil.listDirectories(baseDir);
        assertEquals(1, dirsOnly.size());
        assertEquals(d1, dirsOnly.get(0));

        List<File> dirsRecursive = IOUtil.listDirectories(baseDir, true);
        assertTrue(dirsRecursive.contains(d1));
        assertTrue(dirsRecursive.contains(d2InD1));
        assertEquals(2, dirsRecursive.size());
    }

    @Test
    public void testForLinesFile() throws Exception {
        File testFile = createTempFileWithContent("forLinesTest", ".txt", "Line1\nLine2\nLine3");
        AtomicInteger lineCount = new AtomicInteger(0);
        List<String> collectedLines = new ArrayList<>();

        IOUtil.forLines(testFile, line -> {
            collectedLines.add(line);
            lineCount.incrementAndGet();
        });
        assertEquals(3, lineCount.get());
        assertEquals(Arrays.asList("Line1", "Line2", "Line3"), collectedLines);

        lineCount.set(0);
        collectedLines.clear();
        IOUtil.forLines(testFile, 1, 1, line -> {
            collectedLines.add(line);
            lineCount.incrementAndGet();
        });
        assertEquals(1, lineCount.get());
        assertEquals(Collections.singletonList("Line2"), collectedLines);
    }

    @Test
    public void testForLinesInputStream() throws Exception {
        String content = "IS_LineA\nIS_LineB\nIS_LineC";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        AtomicLong sumLength = new AtomicLong(0);

        IOUtil.forLines(is, line -> sumLength.addAndGet(line.length()));
        assertEquals("IS_LineA".length() + "IS_LineB".length() + "IS_LineC".length(), sumLength.get());
        is.close();
    }

    @Test
    public void testForLinesReader() throws Exception {
        String content = "ReaderX\nReaderY";
        Reader reader = new StringReader(content);
        List<String> linesUpper = new ArrayList<>();

        IOUtil.forLines(reader, 0, Long.MAX_VALUE, 0, 0, line -> linesUpper.add(line.toUpperCase()), () -> System.out.println("Reader forLines complete"));
        assertEquals(Arrays.asList("READERX", "READERY"), linesUpper);
        reader.close();
    }

    @Test
    public void testCopyToDirectory_destinationIsFile() throws IOException {
        File srcFile = createTempFileWithContent("srcCopyToFile", ".txt", "Source content.");
        File destFileAsDir = createTempFileWithContent("destFileAsDir", ".txt", "This is a file, not a dir.");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(srcFile, destFileAsDir));
    }

    @Test
    public void testCopyToDirectory_sourceDoesNotExist() throws IOException {
        File nonExistentSrc = new File(tempDir.toFile(), "nonExistentSrc.txt");
        File destDir = createTempDirectory("destCopyDirFail");
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyToDirectory(nonExistentSrc, destDir));
    }

    @Test
    public void testCopyToDirectory_cannotWrite() throws IOException {
        File srcFile = createTempFileWithContent("srcCannotWrite", ".txt", "content");
        File destDirParent = createTempDirectory("destParentNonWritable");
        File destDir = new File(destDirParent, "actualDest");
        destDir.mkdirs();

        if (destDir.setWritable(false)) {
            if (!destDir.canWrite()) {
                assertThrows(IOException.class, () -> IOUtil.copyToDirectory(srcFile, destDir));
            } else {
                System.err.println("Warning: Could not make test directory non-writable for testCopyToDirectory_cannotWrite: " + destDir.getAbsolutePath());
                IOUtil.copyToDirectory(srcFile, destDir);
                assertTrue(new File(destDir, srcFile.getName()).exists());
            }
            destDir.setWritable(true);
        } else {
            System.err.println("Warning: setWritable(false) returned false for testCopyToDirectory_cannotWrite: " + destDir.getAbsolutePath());
        }
    }

    @Test
    public void testCopyFile_sourceDoesNotExist() throws IOException {
        File nonExistentSrc = new File(tempDir.toFile(), "nonExistentCopySrc.txt");
        File destFile = new File(tempDir.toFile(), "destCopyFail.txt");
        assertThrows(FileNotFoundException.class, () -> IOUtil.copyFile(nonExistentSrc, destFile));
    }

    @Test
    public void testCopyFile_destinationIsDirectory() throws IOException {
        File srcFile = createTempFileWithContent("srcCopyDestIsDir", ".txt", "content");
        File destDir = createTempDirectory("destIsDir");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(srcFile, destDir));
    }

    @Test
    public void testCopyFile_sameFile() throws IOException {
        File srcFile = createTempFileWithContent("srcCopySame", ".txt", "content");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.copyFile(srcFile, srcFile));
    }

    @Test
    public void testMove_sourceDoesNotExist() throws IOException {
        File nonExistentSrc = new File(tempDir.toFile(), "nonExistentMoveSrc.txt");
        File destDir = createTempDirectory("destMoveDirFail");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.move(nonExistentSrc, destDir));
    }

    @Test
    public void testMove_destinationIsFile() throws IOException {
        File srcFile = createTempFileWithContent("srcMoveToFileDest", ".txt", "content");
        File destFileAsDir = createTempFileWithContent("destMoveFileAsDir", ".txt", "This is a file.");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.move(srcFile, destFileAsDir));
    }

    @Test
    public void testDeleteAllIfExists_symlink() throws IOException {
        File targetFile = createTempFileWithContent("symlinkTarget", ".txt", "target");
        Path linkPath = tempDir.resolve("symlink.txt");
        try {
            Files.createSymbolicLink(linkPath, targetFile.toPath());
            File linkFile = linkPath.toFile();
            assertTrue(linkFile.exists());
            assertTrue(IOUtil.isSymbolicLink(linkFile));

            assertTrue(IOUtil.deleteRecursivelyIfExists(linkFile));
            assertFalse(linkFile.exists());
            assertTrue(targetFile.exists());

        } catch (UnsupportedOperationException | FileSystemException | SecurityException e) {
            System.err.println("Skipping deleteRecursivelyIfExists_symlink test: Symlink creation not supported or permission denied. " + e.getMessage());
        }
    }

    @Test
    public void testCleanDirectory_nonExistentDir() {
        File nonExistentDir = new File(tempDir.toFile(), "nonExistentCleanDir");
        assertFalse(IOUtil.cleanDirectory(nonExistentDir));
    }

    @Test
    public void testCleanDirectory_fileInsteadOfDir() throws IOException {
        File fileAsDir = createTempFileWithContent("fileAsCleanDir", ".txt", "content");
        assertFalse(IOUtil.cleanDirectory(fileAsDir));
    }

    @Test
    public void testSizeOf_symlinkInDirectory() throws IOException {
        File dir = createTempDirectory("sizeDirWithSymlink");
        File realFile = createTempFileWithContent(dir.toPath().resolve("real.txt").toString(), null, new byte[100]);
        Path linkPath = dir.toPath().resolve("link.txt");

        try {
            Files.createSymbolicLink(linkPath, realFile.toPath());
            File linkFile = linkPath.toFile();
            assertTrue(IOUtil.isSymbolicLink(linkFile));

            assertEquals(100, IOUtil.sizeOfDirectory(dir));
            assertEquals(100, IOUtil.sizeOfDirectoryAsBigInteger(dir).longValue());

        } catch (UnsupportedOperationException | FileSystemException | SecurityException e) {
            System.err.println("Skipping testSizeOf_symlinkInDirectory test: Symlink creation not supported or permission denied. " + e.getMessage());
            assertEquals(100, IOUtil.sizeOfDirectory(dir));
        }
    }

    @Test
    public void testZip_emptyDirectory() throws IOException {
        File emptyDirToZip = createTempDirectory("emptyZipDir");
        File zipFile = new File(tempDir.toFile(), "emptyDirArchive.zip");
        IOUtil.zip(emptyDirToZip, zipFile);
        assertTrue(zipFile.exists());

        File unzipDir = createTempDirectory("unzipEmptyDir");
        IOUtil.unzip(zipFile, unzipDir);
        File unzippedOriginalDir = new File(unzipDir, emptyDirToZip.getName());
        assertTrue(unzippedOriginalDir.exists() && unzippedOriginalDir.isDirectory());
        assertEquals(0, unzippedOriginalDir.listFiles().length);
    }

    @Test
    public void testZip_collectionWithDirectory() throws IOException {
        File dirToZip = createTempDirectory("collZipDir");
        File file1 = createTempFileWithContent(dirToZip.toPath().resolve("file1.txt").toString(), null, "content1");
        File subDir = new File(dirToZip, "sub");
        subDir.mkdir();
        File file2 = createTempFileWithContent(subDir.toPath().resolve("file2.txt").toString(), null, "content2");

        File zipFile = new File(tempDir.toFile(), "collectionWithDir.zip");
        IOUtil.zip(Arrays.asList(dirToZip), zipFile);

        File unzipDir = createTempDirectory("unzipCollDir");
        IOUtil.unzip(zipFile, unzipDir);

        assertTrue(new File(unzipDir, dirToZip.getName() + "/" + file1.getName()).exists());
        assertTrue(new File(unzipDir, dirToZip.getName() + "/sub/" + file2.getName()).exists());
    }

    @Test
    public void testZip_collectionWithDirectory_2() throws IOException {
        File dirToZip = createTempDirectory("collZipDir");
        File file1 = createTempFileWithContent(dirToZip.toPath().resolve("file1.txt").toString(), null, "content1");
        File subDir = new File(dirToZip, "sub");
        subDir.mkdir();
        File file2 = createTempFileWithContent(subDir.toPath().resolve("file2.txt").toString(), null, "content2");

        File zipFile = new File(tempDir.toFile(), "collectionWithDir.zip");
        IOUtil.zip(Arrays.asList(file1, subDir), zipFile);

        File unzipDir = createTempDirectory("unzipCollDir");
        IOUtil.unzip(zipFile, unzipDir);

        assertTrue(new File(unzipDir, file1.getName()).exists());
        assertTrue(new File(unzipDir, "/sub/" + file2.getName()).exists());
    }

    @Test
    public void testUnzip_zipFileDoesNotExist() throws IOException {
        File nonExistentZip = new File(tempDir.toFile(), "noSuch.zip");
        File unzipDir = createTempDirectory("unzipFailDir");
        assertThrows(FileNotFoundException.class, () -> IOUtil.unzip(nonExistentZip, unzipDir));
    }

    @Test
    public void testMerge_emptySourceFiles() throws IOException {
        File destFile = new File(tempDir.toFile(), "mergedEmpty.txt");
        long bytesMerged = IOUtil.merge(Collections.emptyList(), destFile);
        assertEquals(0, bytesMerged);
        assertTrue(destFile.exists());
        assertEquals(0, destFile.length());
    }

    @Test
    public void testMerge_singleFile() throws IOException {
        File file1 = createTempFileWithContent("mergeSingle", ".txt", "Single file content.");
        File destFile = new File(tempDir.toFile(), "mergedSingle.txt");
        byte[] delimiter = "::".getBytes();

        long bytesMerged = IOUtil.merge(Collections.singletonList(file1), delimiter, destFile);
        assertEquals(file1.length(), bytesMerged);
        assertEquals("Single file content.", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testToFile_invalidURLProtocol() throws MalformedURLException {
        URL httpUrl = new URL("http://example.com");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFile(httpUrl));
    }

    @Test
    public void testToURL_fileDoesNotExist() {
        File nonExistentFile = new File(tempDir.toFile(), "nonExistentForUrl.txt");
        assertDoesNotThrow(() -> IOUtil.toURL(nonExistentFile));
        URL url = IOUtil.toURL(nonExistentFile);
        assertTrue(url.toString().startsWith("file:/"));
        assertTrue(url.toString().endsWith("nonExistentForUrl.txt"));
    }

    @Test
    public void testForLines_emptyFile() throws Exception {
        File emptyFile = createTempFileWithContent("forLinesEmpty", ".txt", "");
        AtomicInteger count = new AtomicInteger(0);
        IOUtil.forLines(emptyFile, line -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testForLines_withThreading() throws Exception {
        StringBuilder content = new StringBuilder();
        List<String> expectedLines = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            String line = "Threaded Line " + i;
            content.append(line).append("\n");
            expectedLines.add(line);
        }
        File testFile = createTempFileWithContent("forLinesThreaded", ".txt", content.toString());

        List<String> collectedLines = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger actionCount = new AtomicInteger(0);
        AtomicInteger onCompleteCount = new AtomicInteger(0);

        IOUtil.forLines(testFile, 0, Long.MAX_VALUE, 1, 2, 10, line -> {
            collectedLines.add(line);
            actionCount.incrementAndGet();
        }, () -> {
            onCompleteCount.incrementAndGet();
        });

        assertEquals(100, actionCount.get());
        assertEquals(1, onCompleteCount.get());
        assertEquals(expectedLines.size(), collectedLines.size());
        assertTrue(collectedLines.containsAll(expectedLines));
        assertTrue(expectedLines.containsAll(collectedLines));
    }

    @Test
    public void testListFiles_nonExistentParent() {
        File nonExistentParent = new File(tempDir.toFile(), "noSuchDirForList");
        assertTrue(IOUtil.listFiles(nonExistentParent).isEmpty());
        assertTrue(IOUtil.walk(nonExistentParent).count() == 0);
    }

    @Test
    public void testListFiles_emptyParent() throws IOException {
        File emptyParent = createTempDirectory("emptyDirForList");
        assertTrue(IOUtil.listFiles(emptyParent).isEmpty());
        assertTrue(IOUtil.walk(emptyParent).count() == 0);
    }

    @Test
    public void testIsRegularFile_directory() throws IOException {
        File dir = createTempDirectory("isRegularTestDir");
        assertFalse(IOUtil.isRegularFile(dir));
        assertFalse(IOUtil.isRegularFile(dir, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testIsDirectory_file() throws IOException {
        File file = createTempFileWithContent("isDirTestFile", ".txt", "data");
        assertFalse(IOUtil.isDirectory(file));
        assertFalse(IOUtil.isDirectory(file, LinkOption.NOFOLLOW_LINKS));
    }

    @Test
    public void testToFiles_emptyAndNullURLs() {
        assertEquals(0, IOUtil.toFiles(new URL[0]).length);
        List<URL> urlList = new ArrayList<>();
        assertTrue(IOUtil.toFiles(urlList).isEmpty());

        urlList.add(null);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles(urlList));

        URL[] urlArrayWithNull = new URL[] { null };
        assertThrows(IllegalArgumentException.class, () -> IOUtil.toFiles(urlArrayWithNull));
    }

    @Test
    public void testToURLs_emptyAndNullFiles() {
        assertEquals(0, IOUtil.toURLs(new File[0]).length);
        List<File> fileList = new ArrayList<>();
        assertTrue(IOUtil.toURLs(fileList).isEmpty());

        fileList.add(null);
        assertThrows(NullPointerException.class, () -> IOUtil.toURLs(fileList));

        File[] fileArrayWithNull = new File[] { null };
        assertThrows(NullPointerException.class, () -> IOUtil.toURLs(fileArrayWithNull));
    }

    @Test
    public void testGzipStreams() throws IOException {
        String originalText = "This is a string to be GZIPped xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx and then unGZIPped. It contains Ümlauts and € symbols.";
        File originalFile = createTempFileWithContent("gzipOrig", ".txt", originalText, StandardCharsets.UTF_8);
        File gzippedFile = new File(tempDir.toFile(), "gzipped.txt.gz");
        File unGzippedFile = new File(tempDir.toFile(), "unGzippedFromGz.txt");

        try (InputStream in = IOUtil.newBufferedInputStream(originalFile);
                OutputStream out = IOUtil.newGZIPOutputStream(IOUtil.newBufferedOutputStream(gzippedFile))) {
            IOUtil.write(in, out);
        }
        assertTrue(gzippedFile.exists());
        assertTrue(gzippedFile.length() < originalFile.length());

        try (InputStream in = IOUtil.newGZIPInputStream(IOUtil.newBufferedInputStream(gzippedFile));
                OutputStream out = IOUtil.newBufferedOutputStream(unGzippedFile)) {
            IOUtil.write(in, out);
        }
        assertTrue(unGzippedFile.exists());
        assertEquals(originalText, IOUtil.readAllToString(unGzippedFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testReadAllBytesFromFile_GZ() throws IOException {
        String originalContent = "Test GZIPped content for bytes. €Üñ";
        File tempTxtFile = createTempFile("tempTxtForGz", ".txt");
        IOUtil.write(originalContent, StandardCharsets.UTF_8, tempTxtFile);

        File gzippedFile = new File(tempDir.toFile(), "testReadAllBytes.txt.gz");
        try (FileOutputStream fos = new FileOutputStream(gzippedFile);
                GZIPOutputStream gzos = new GZIPOutputStream(fos);
                FileInputStream fis = new FileInputStream(tempTxtFile)) {
            IOUtil.write(fis, gzos);
        }
        assertTrue(gzippedFile.exists());

        byte[] readBytes = IOUtil.readAllBytes(gzippedFile);
        assertEquals(originalContent, new String(readBytes, StandardCharsets.UTF_8));
    }

    @Test
    public void testReadAllBytesFromFile_ZIP() throws IOException {
        String originalContent = "Test ZIPped content for bytes. €Üñ";
        String entryName = "entry.txt";
        File zippedFile = new File(tempDir.toFile(), "testReadAllBytes.zip");

        try (FileOutputStream fos = new FileOutputStream(zippedFile); ZipOutputStream zos = new ZipOutputStream(fos)) {
            ZipEntry entry = new ZipEntry(entryName);
            zos.putNextEntry(entry);
            zos.write(originalContent.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }
        assertTrue(zippedFile.exists());

        byte[] readBytes = IOUtil.readAllBytes(zippedFile);
        assertEquals(originalContent, new String(readBytes, StandardCharsets.UTF_8));
    }

    static Charset checkCharset(final Charset charset) {
        return charset == null ? Charsets.DEFAULT : charset;
    }

    @Test
    public void testCheckCharset_nullInput() {
        Charset defaultCharset = Charset.defaultCharset();
        Charset result = checkCharset(null);
        assertNotNull(result);
        String test = "test";
        assertArrayEquals(test.getBytes(result), test.getBytes(checkCharset(null)));

        Charset utf8 = StandardCharsets.UTF_8;
        assertEquals(utf8, checkCharset(utf8));
    }

    @Test
    public void testNewFileOutputStream_appendMode() throws IOException {
        File testFile = createTempFile("fosAppend", ".txt");
        IOUtil.write("Initial", StandardCharsets.UTF_8, testFile);

        try (FileOutputStream fosAppend = IOUtil.newFileOutputStream(testFile, true);
                OutputStreamWriter osw = new OutputStreamWriter(fosAppend, StandardCharsets.UTF_8)) {
            osw.write("-Appended");
        }
        assertEquals("Initial-Appended", IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));

        try (FileOutputStream fosOverwrite = IOUtil.newFileOutputStream(testFile);
                OutputStreamWriter osw = new OutputStreamWriter(fosOverwrite, StandardCharsets.UTF_8)) {
            osw.write("Overwritten");
        }
        assertEquals("Overwritten", IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewFileWriter_appendMode() throws IOException {
        File testFile = createTempFile("fwAppend", ".txt");
        IOUtil.write("FirstPart", StandardCharsets.UTF_8, testFile);

        try (FileWriter fwAppend = IOUtil.newFileWriter(testFile, StandardCharsets.UTF_8, true)) {
            fwAppend.write("-SecondPart");
        }
        assertEquals("FirstPart-SecondPart", IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));

        try (FileWriter fwOverwrite = IOUtil.newFileWriter(testFile, StandardCharsets.UTF_8, false)) {
            fwOverwrite.write("NewContent");
        }
        assertEquals("NewContent", IOUtil.readAllToString(testFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewBufferedStreams_withSize() throws IOException {
        File testFile = createTempFileWithContent("bufferedSize", ".txt", "some data");
        int customBufferSize = 16;

        try (BufferedInputStream bis = IOUtil.newBufferedInputStream(testFile, customBufferSize)) {
            assertNotNull(bis);
            byte[] readData = IOUtil.readBytes(bis, 0, 4);
            assertArrayEquals("some".getBytes(StandardCharsets.UTF_8), readData);
        }

        File outFile = createTempFile("bufferedOutSize", ".txt");
        try (BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outFile, customBufferSize)) {
            assertNotNull(bos);
            bos.write("test buffer".getBytes(StandardCharsets.UTF_8));
        }
        assertEquals("test buffer", IOUtil.readAllToString(outFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testNewGzipStreams_withBufferSize() throws IOException {
        String originalText = "GZIP with custom buffer size.";
        File gzippedFile = new File(tempDir.toFile(), "customBuffer.gz");
        int bufferSize = 256;

        try (ByteArrayInputStream bais = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
                GZIPOutputStream gzos = IOUtil.newGZIPOutputStream(new FileOutputStream(gzippedFile), bufferSize)) {
            IOUtil.write(bais, gzos);
        }
        assertTrue(gzippedFile.exists());

        String uncompressedText;
        try (GZIPInputStream gzis = IOUtil.newGZIPInputStream(new FileInputStream(gzippedFile), bufferSize)) {
            uncompressedText = IOUtil.readAllToString(gzis, StandardCharsets.UTF_8);
        }
        assertEquals(originalText, uncompressedText);
    }

    @Test
    public void testCloseAll_multipleExceptions() {
        MockCloseable mc1 = new MockCloseable(true, "E1");
        MockCloseable mc2 = new MockCloseable(false, "OK2");
        MockCloseable mc3 = new MockCloseable(true, "E3");
        MockCloseable mc4 = new MockCloseable(false, "OK4");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> IOUtil.closeAll(mc1, mc2, mc3, mc4));
        assertTrue(mc1.isClosed());
        assertTrue(mc2.isClosed());
        assertTrue(mc3.isClosed());
        assertTrue(mc4.isClosed());

        assertEquals("java.io.IOException: Mock close exception: E1", ex.getMessage());
        Throwable[] suppressed = ex.getSuppressed();
        assertEquals(1, suppressed.length);
        assertTrue(suppressed[0] instanceof IOException);
        assertTrue(suppressed[0].getMessage().contains("E3"));
    }

    @Test
    public void testCopyToDirectory_filterThrowsException() throws IOException {
        File srcFile = createTempFileWithContent("srcFilterEx", ".txt", "content");
        File destDir = createTempDirectory("destFilterEx");

        class MyCustomException extends Exception {
            MyCustomException(String msg) {
                super(msg);
            }
        }

        Throwables.BiPredicate<File, File, MyCustomException> filter = (parent, file) -> {
            if (file.getName().equals(srcFile.getName())) {
                throw new MyCustomException("Filter failed for " + file.getName());
            }
            return true;
        };

        File srcDir = createTempDirectory("srcDirForFilterEx");
        File child1 = new File(srcDir, "child1.txt");
        IOUtil.write("c1", child1);
        File child2ThatThrows = new File(srcDir, "child2throws.txt");
        IOUtil.write("c2", child2ThatThrows);

        Throwables.BiPredicate<File, File, MyCustomException> dirFilter = (parent, file) -> {
            if (file.getName().equals("child2throws.txt")) {
                throw new MyCustomException("Filter failed on child2throws");
            }
            return true;
        };

        assertThrows(MyCustomException.class, () -> IOUtil.copyToDirectory(srcDir, destDir, true, dirFilter));
        assertTrue(new File(new File(destDir, srcDir.getName()), "child1.txt").exists());
        assertFalse(new File(new File(destDir, srcDir.getName()), "child2throws.txt").exists());
    }

    @Test
    public void testCopyFile_withNoFollowLinks() throws IOException {
        File targetFile = createTempFileWithContent("symlinkTargetForCopy", ".txt", "target data");
        Path linkPath = tempDir.resolve("symlinkForCopy.txt");
        File destFile = new File(tempDir.toFile(), "destCopyNoFollow.txt");

        try {
            Files.createSymbolicLink(linkPath, targetFile.toPath());
            File linkFile = linkPath.toFile();

            IOUtil.copyFile(linkFile, destFile, true, LinkOption.NOFOLLOW_LINKS, StandardCopyOption.REPLACE_EXISTING);
            assertTrue(destFile.exists());
            assertFalse(IOUtil.isSymbolicLink(destFile));
            assertEquals("target data", IOUtil.readAllToString(destFile));
            assertTrue(IOUtil.isSymbolicLink(linkFile));

        } catch (UnsupportedOperationException | FileSystemException | SecurityException e) {
            System.err.println("Skipping testCopyFile_withNoFollowLinks: Symlink creation not supported. " + e.getMessage());
        }
    }

    @Test
    public void testIsFileNewerOlder_referenceFileNonExistent() throws IOException {
        File file1 = createTempFileWithContent("fileDateRef1", ".txt", "f1");
        File nonExistentRef = new File(tempDir.toFile(), "nonExistentRef.txt");

        long nonExistentTime = nonExistentRef.lastModified();
        assertEquals(0L, nonExistentTime);

        if (file1.lastModified() > 0) {
            assertTrue(IOUtil.isFileNewer(file1, nonExistentRef));
            assertFalse(IOUtil.isFileOlder(file1, nonExistentRef));
        } else {
            assertFalse(IOUtil.isFileNewer(file1, nonExistentRef));
            assertFalse(IOUtil.isFileOlder(file1, nonExistentRef));
        }
    }

    @Test
    public void testZip_singleSourceFile() throws IOException {
        File srcFile = createTempFileWithContent("singleFileZip", ".txt", "Zipping a single file. Ümlaut €");
        File zipFile = new File(tempDir.toFile(), "singleFileArchive.zip");

        IOUtil.zip(srcFile, zipFile);
        assertTrue(zipFile.exists());

        File unzipDir = createTempDirectory("unzipSingleFile");
        IOUtil.unzip(zipFile, unzipDir);

        File unzippedFile = new File(unzipDir, srcFile.getName());
        assertTrue(unzippedFile.exists());
        assertEquals("Zipping a single file. Ümlaut €", IOUtil.readAllToString(unzippedFile, StandardCharsets.UTF_8));
    }

    @Test
    public void testZip_emptyCollection() throws IOException {
        File zipFile = new File(tempDir.toFile(), "emptyCollectionArchive.zip");
        IOUtil.zip(Collections.emptyList(), zipFile);
        assertTrue(zipFile.exists());

        File unzipDir = createTempDirectory("unzipEmptyCollection");
        IOUtil.unzip(zipFile, unzipDir);
        assertEquals(0, unzipDir.listFiles().length);
    }

    @Test
    public void testUnzip_corruptedZip() throws IOException {
        File corruptedZip = createTempFileWithContent("corrupted", ".zip", "This is not a valid zip file content".getBytes());
        File unzipDir = createTempDirectory("unzipCorrupted");

        assertThrows(UncheckedIOException.class, () -> IOUtil.unzip(corruptedZip, unzipDir));
    }

    @Test
    public void testSplit_destDirCreation() throws IOException {
        File sourceFile = createTempFileWithContent("splitDestCreate", ".dat", new byte[100]);
        File parentDestDir = createTempDirectory("splitParentDest");
        File destDir = new File(parentDestDir, "actualSplitDest");

        IOUtil.split(sourceFile, 3, destDir);
        assertTrue(destDir.exists() && destDir.isDirectory());
        assertTrue(new File(destDir, sourceFile.getName() + "_0001").exists());
    }

    @Test
    public void testMerge_arrayVersion() throws IOException {
        File file1 = createTempFileWithContent("mergeArr1", ".txt", "Array1");
        File file2 = createTempFileWithContent("mergeArr2", ".txt", "Array2");
        File destFile = new File(tempDir.toFile(), "mergedArray.txt");

        IOUtil.merge(new File[] { file1, file2 }, destFile);
        assertEquals("Array1Array2", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testMerge_destFileAlreadyExists() throws IOException {
        File file1 = createTempFileWithContent("mergeOverwrite1", ".txt", "New1");
        File destFile = createTempFileWithContent("mergeDestExists", ".txt", "Old Content");

        IOUtil.merge(Collections.singletonList(file1), destFile);
        assertEquals("New1", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testMapFile_readWriteNonExistent() throws IOException {
        File nonExistentFile = new File(tempDir.toFile(), "mapRWNonExistent.dat");
        long sizeToCreate = 1024;

        MappedByteBuffer buffer = null;
        try {
            buffer = IOUtil.map(nonExistentFile, FileChannel.MapMode.READ_WRITE, 0, sizeToCreate);
            assertTrue(nonExistentFile.exists());
            assertEquals(sizeToCreate, nonExistentFile.length());
            assertEquals(sizeToCreate, buffer.capacity());

            buffer.put(0, (byte) 'X');
            buffer.put((int) (sizeToCreate - 1), (byte) 'Y');
            buffer.force();
        } finally {
            if (buffer != null) {
            }
        }
        try (RandomAccessFile raf = new RandomAccessFile(nonExistentFile, "r")) {
            assertEquals('X', raf.readByte());
            raf.seek(sizeToCreate - 1);
            assertEquals('Y', raf.readByte());
        }

        unmap(buffer);
    }

    @Test
    public void testReadIntoBuffer_zeroLength() throws IOException {
        File testFile = createTempFileWithContent("readZeroLen", ".txt", "data");
        byte[] buffer = new byte[10];

        int bytesRead = IOUtil.read(testFile, buffer, 0, 0);
        assertEquals(0, bytesRead);

        try (InputStream is = IOUtil.newFileInputStream(testFile)) {
            bytesRead = IOUtil.read(is, buffer, 0, 0);
            assertEquals(0, bytesRead);
        }

        char[] charBuffer = new char[10];
        int charsRead = IOUtil.read(testFile, StandardCharsets.UTF_8, charBuffer, 0, 0);
        assertEquals(0, charsRead);

        try (Reader reader = IOUtil.newFileReader(testFile, StandardCharsets.UTF_8)) {
            charsRead = IOUtil.read(reader, charBuffer, 0, 0);
            assertEquals(0, charsRead);
        }
    }

    @Test
    public void testWriteFileOffsetCountToFile() throws IOException {
        File srcFile = createTempFileWithContent("writeSrcOffCnt", ".txt", "This is source data for offset write.");
        File destFile = createTempFile("writeDestOffCnt", ".txt");
        if (destFile.exists())
            destFile.delete();

        long bytesWritten = IOUtil.write(srcFile, 8, 11, destFile);
        assertEquals(11, bytesWritten);
        assertEquals("source data", IOUtil.readAllToString(destFile, StandardCharsets.UTF_8));
    }

}
