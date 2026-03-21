package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class BufferedReaderTest extends AbstractTest {

    @Test
    public void testConstructorWithString() throws IOException {
        BufferedReader reader = new BufferedReader("Hello World");
        assertEquals('H', reader.read());
        assertEquals("ello World", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testConstructorWithInputStream() throws IOException {
        String content = "Hello from stream";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);

        assertEquals("Hello from stream", reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

    @Test
    public void testConstructorWithReader() throws IOException {
        StringReader sr = new StringReader("Reader content\nLine two");
        BufferedReader reader = new BufferedReader((java.io.Reader) sr);

        assertEquals("Reader content", reader.readLine());
        assertEquals("Line two", reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

    @Test
    public void testRead() throws Exception {
        String str = "abc123456789";
        BufferedReader reader = new BufferedReader(str);

        assertEquals('a', reader.read());
        reader.skip(1);
        assertEquals('c', reader.read());
        assertTrue(reader.ready());
        assertEquals("123456789", reader.readLine());
    }

    @Test
    public void testRead_2() throws Exception {
        File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        String str = "abc123456789\n\rabc";
        IOUtil.write(str, file);

        InputStream is = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(is);

        assertEquals('a', reader.read());
        reader.skip(1);
        assertEquals('c', reader.read());

        assertTrue(reader.ready());
        assertEquals("123456789", reader.readLine());
        reader.readLine();
        assertEquals("abc", reader.readLine());

        IOUtil.close(is);
    }

    @Test
    public void testReadCharArray() throws IOException {
        BufferedReader reader = new BufferedReader("Hello World");
        char[] buffer = new char[5];
        int count = reader.read(buffer, 0, 5);

        assertEquals(5, count);
        assertEquals("Hello", new String(buffer, 0, count));
    }

    @Test
    public void testReadCharArray_PartialRead() throws IOException {
        BufferedReader reader = new BufferedReader("Hi");
        char[] buffer = new char[10];
        int count = reader.read(buffer, 0, 10);

        assertEquals(2, count);
        assertEquals("Hi", new String(buffer, 0, count));
    }

    @Test
    public void testReadCharArray_InvalidArgs() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        char[] buffer = new char[5];

        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buffer, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(buffer, 0, 10));
    }

    @Test
    public void testReadCharArray_ZeroLength() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        char[] buffer = new char[5];
        int count = reader.read(buffer, 0, 0);

        assertEquals(0, count);
    }

    @Test
    public void testRead_EndOfStream() throws IOException {
        BufferedReader reader = new BufferedReader("");
        assertEquals(-1, reader.read());
    }

    @Test
    public void testReadCharArray_LargeBufferFromInputStream() throws IOException {
        char[] source = new char[Objectory.BUFFER_SIZE + 8];
        Arrays.fill(source, 'x');

        InputStream is = new ByteArrayInputStream(new String(source).getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);
        char[] buffer = new char[source.length];
        int count = reader.read(buffer, 0, buffer.length);

        assertEquals(buffer.length, count);
        assertEquals('x', buffer[0]);
        assertEquals('x', buffer[buffer.length - 1]);
    }

    @Test
    public void testReadCharArray_EndOfStream() throws IOException {
        BufferedReader reader = new BufferedReader("");
        char[] buffer = new char[5];
        assertEquals(-1, reader.read(buffer, 0, 5));
    }

    @Test
    public void testReadMultipleLines_FromStream() throws IOException {
        String content = "line1\nline2\nline3";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);

        assertEquals("line1", reader.readLine());
        assertEquals("line2", reader.readLine());
        assertEquals("line3", reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

    @Test
    public void testReadAfterClose_ThrowsIOException() throws IOException {
        String content = "test";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);
        reader.close();

        assertThrows(IOException.class, () -> reader.read());
        assertThrows(IOException.class, () -> reader.readLine());
        assertThrows(IOException.class, () -> reader.ready());
    }

    @Test
    public void testReadCharArray_FromString() throws IOException {
        BufferedReader reader = new BufferedReader("ABCDE");
        char[] buf = new char[3];
        int n = reader.read(buf, 0, 3);
        assertEquals(3, n);
        assertEquals("ABC", new String(buf, 0, n));

        n = reader.read(buf, 0, 3);
        assertEquals(2, n);
        assertEquals("DE", new String(buf, 0, n));

        n = reader.read(buf, 0, 3);
        assertEquals(-1, n);
    }

    @Test
    public void testReadLine() throws IOException {
        BufferedReader reader = new BufferedReader("Line 1\nLine 2\nLine 3");
        assertEquals("Line 1", reader.readLine());
        assertEquals("Line 2", reader.readLine());
        assertEquals("Line 3", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testReadLine_CarriageReturn() throws IOException {
        BufferedReader reader = new BufferedReader("Line 1\rLine 2\rLine 3");
        assertEquals("Line 1", reader.readLine());
        assertEquals("Line 2", reader.readLine());
        assertEquals("Line 3", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testReadLine_CRLF() throws IOException {
        BufferedReader reader = new BufferedReader("Line 1\r\nLine 2\r\nLine 3");
        assertEquals("Line 1", reader.readLine());
        assertEquals("Line 2", reader.readLine());
        assertEquals("Line 3", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testReadLine_EmptyLines() throws IOException {
        BufferedReader reader = new BufferedReader("\n\nline3");
        assertEquals("", reader.readLine());
        assertEquals("", reader.readLine());
        assertEquals("line3", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testSkip() throws IOException {
        BufferedReader reader = new BufferedReader("1234567890");
        long skipped = reader.skip(5);
        assertEquals(5, skipped);
        assertEquals('6', reader.read());
    }

    @Test
    public void testSkip_BeyondEnd() throws IOException {
        BufferedReader reader = new BufferedReader("Hi");
        long skipped = reader.skip(100);
        assertEquals(2, skipped);
        assertEquals(-1, reader.read());
    }

    @Test
    public void testSkip_Zero() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        long skipped = reader.skip(0);
        assertEquals(0, skipped);
        assertEquals('H', reader.read());
    }

    @Test
    public void testSkip_NegativeThrows() {
        BufferedReader reader = new BufferedReader("Hello");
        assertThrows(IllegalArgumentException.class, () -> reader.skip(-1));
    }

    @Test
    public void testSkip_FromStream() throws IOException {
        String content = "abcdefghij";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);

        long skipped = reader.skip(3);
        assertTrue(skipped > 0);
        reader.close();
    }

    @Test
    public void testReady() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        assertTrue(reader.ready());
    }

    @Test
    public void testReady_AfterReadLineSkipsLineFeed() throws IOException {
        InputStream is = new ByteArrayInputStream("line1\r\nline2".getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);

        assertEquals("line1", reader.readLine());
        assertTrue(reader.ready());
        assertEquals('l', reader.read());
    }

    @Test
    public void testReady_FromStream() throws IOException {
        String content = "test";
        InputStream is = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
        BufferedReader reader = new BufferedReader(is);

        assertTrue(reader.ready());
        reader.close();
    }

    @Test
    public void testClose() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        assertEquals('H', reader.read());
        reader.close();

        assertThrows(IOException.class, () -> reader.read());
    }

    @Test
    public void testClose_Idempotent() throws IOException {
        BufferedReader reader = new BufferedReader("Hello");
        reader.close();
        reader.close(); // should not throw
    }

    @Test
    public void testReinitWithString() throws IOException {
        BufferedReader reader = new BufferedReader("First");
        assertEquals("First", reader.readLine());
        assertNull(reader.readLine());

        reader.reinit("Second");
        assertEquals("Second", reader.readLine());
        assertNull(reader.readLine());
    }

    @Test
    public void testReinitWithInputStream() throws IOException {
        BufferedReader reader = new BufferedReader("Initial");
        assertEquals("Initial", reader.readLine());

        InputStream is = new ByteArrayInputStream("FromStream".getBytes(StandardCharsets.UTF_8));
        reader.reinit(is);
        assertEquals("FromStream", reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

    @Test
    public void testReinitWithReader() throws IOException {
        BufferedReader reader = new BufferedReader("Initial");
        assertEquals("Initial", reader.readLine());

        reader.reinit(new StringReader("FromReader\nLine2"));
        assertEquals("FromReader", reader.readLine());
        assertEquals("Line2", reader.readLine());
        assertNull(reader.readLine());
        reader.close();
    }

    // Exercise package-private pool/reset helpers directly.

    @Test
    public void testReadLine_IgnoreLeadingLf() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("\nSecond"));

        assertEquals("Second", reader.readLine(true));
        assertNull(reader.readLine());
    }

    @Test
    public void testResetAndReinitWithString() throws IOException {
        BufferedReader reader = new BufferedReader(new StringReader("Before"));
        assertEquals("Before", reader.readLine());

        reader._reset();
        reader.reinit("After");

        assertEquals("After", reader.readLine());
        assertNull(reader.readLine());
    }
}
