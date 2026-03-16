package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BufferedWriterTest extends TestBase {

    // === Constructors ===

    @Test
    public void testDefaultConstructor() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("hello");
        assertEquals("hello", writer.toString());
    }

    @Test
    public void testConstructor_OutputStream() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter(baos);
        writer.write("test");
        writer.flush();
        writer.close();
        assertEquals("test", baos.toString());
    }

    @Test
    public void testConstructor_Writer() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write("abc");
        writer.flush();
        writer.close();
        assertEquals("abc", sw.toString());
    }

    // === write(boolean) ===

    @Test
    public void testWriteBoolean() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(true);
        assertEquals("true", writer.toString());
    }

    @Test
    public void testWriteBoolean_False() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(false);
        assertEquals("false", writer.toString());
    }

    // === write(byte) ===

    @Test
    public void testWriteByte() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((byte) 127);
        assertEquals("127", writer.toString());
    }

    @Test
    public void testWriteByte_Negative() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((byte) -1);
        assertEquals("-1", writer.toString());
    }

    // === write(short) ===

    @Test
    public void testWriteShort() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((short) 32767);
        assertEquals("32767", writer.toString());
    }

    // === write(int) (deprecated, writes char) ===

    @Test
    public void testWriteInt_AsChar() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((int) 'A');
        assertEquals("A", writer.toString());
    }

    // === writeInt(int) ===

    @Test
    public void testWriteInt() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.writeInt(12345);
        assertEquals("12345", writer.toString());
    }

    @Test
    public void testWriteInt_Negative() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.writeInt(-999);
        assertEquals("-999", writer.toString());
    }

    // === write(long) ===

    @Test
    public void testWriteLong() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(1234567890L);
        assertEquals("1234567890", writer.toString());
    }

    // === write(float) ===

    @Test
    public void testWriteFloat() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(3.14f);
        assertEquals(N.stringOf(3.14f), writer.toString());
    }

    // === write(double) ===

    @Test
    public void testWriteDouble() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(3.14159);
        assertEquals("3.14159", writer.toString());
    }

    // === write(Date) ===

    @Test
    public void testWriteDate() {
        BufferedWriter writer = new BufferedWriter();
        Date date = new Date(0);
        writer.write(date);
        String result = writer.toString();
        assertTrue(result.length() > 0);
    }

    // === write(Calendar) ===

    @Test
    public void testWriteCalendar() {
        BufferedWriter writer = new BufferedWriter();
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(0);
        writer.write(cal);
        String result = writer.toString();
        assertTrue(result.length() > 0);
    }

    // === write(char) ===

    @Test
    public void testWriteChar() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write('A');
        assertEquals("A", writer.toString());
    }

    @Test
    public void testWriteChar_WithExternalWriter() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write('Z');
        writer.flush();
        writer.close();
        assertEquals("Z", sw.toString());
    }

    // === write(String) ===

    @Test
    public void testWriteString() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("Hello World");
        assertEquals("Hello World", writer.toString());
    }

    @Test
    public void testWriteString_Null() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((String) null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteString_Empty() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("");
        assertEquals("", writer.toString());
    }

    // === write(String, int, int) ===

    @Test
    public void testWriteString_OffsetLen() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("Hello World", 6, 5);
        assertEquals("World", writer.toString());
    }

    @Test
    public void testWriteString_OffsetLen_Null() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write((String) null, 0, 4);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testWriteString_OffsetLen_InvalidBounds() {
        BufferedWriter writer = new BufferedWriter();
        assertThrows(IndexOutOfBoundsException.class, () -> writer.write("abc", -1, 1));
    }

    @Test
    public void testWriteString_OffsetLen_ZeroLen() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("abc", 0, 0);
        assertEquals("", writer.toString());
    }

    // === write(char[]) ===

    @Test
    public void testWriteCharArray() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(new char[] { 'H', 'e', 'l', 'l', 'o' });
        assertEquals("Hello", writer.toString());
    }

    @Test
    public void testWriteCharArray_WithExternalWriter() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write(new char[] { 'A', 'B', 'C' });
        writer.flush();
        writer.close();
        assertEquals("ABC", sw.toString());
    }

    // === write(char[], int, int) ===

    @Test
    public void testWriteCharArray_OffsetLen() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        char[] chars = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
        writer.write(chars, 6, 5);
        assertEquals("World", writer.toString());
    }

    @Test
    public void testWriteCharArray_OffsetLen_InvalidBounds() {
        BufferedWriter writer = new BufferedWriter();
        assertThrows(IndexOutOfBoundsException.class, () -> writer.write(new char[] { 'a' }, -1, 1));
    }

    @Test
    public void testWriteCharArray_OffsetLen_ZeroLen() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write(new char[] { 'a', 'b' }, 0, 0);
        assertEquals("", writer.toString());
    }

    // === newLine() ===

    @Test
    public void testNewLine() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("Line1");
        writer.newLine();
        writer.write("Line2");
        String result = writer.toString();
        assertTrue(result.contains("Line1"));
        assertTrue(result.contains("Line2"));
    }

    // === append(CharSequence) ===

    @Test
    public void testAppendCharSequence() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        Writer returned = writer.append("hello");
        assertEquals("hello", writer.toString());
        assertTrue(returned instanceof BufferedWriter);
    }

    @Test
    public void testAppendCharSequence_Null() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.append(null);
        assertEquals("null", writer.toString());
    }

    // === append(CharSequence, int, int) ===

    @Test
    public void testAppendCharSequence_OffsetLen() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.append("Hello World", 6, 11);
        assertEquals("World", writer.toString());
    }

    // === append(char) ===

    @Test
    public void testAppendChar() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        Writer returned = writer.append('X');
        assertEquals("X", writer.toString());
        assertTrue(returned instanceof BufferedWriter);
    }

    // === flush() ===

    @Test
    public void testFlush() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("data");
        writer.flush();
        assertEquals("data", writer.toString());
    }

    @Test
    public void testFlush_WithExternalWriter() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write("data");
        writer.flush();
        assertEquals("data", sw.toString());
    }

    // === close() ===

    @Test
    public void testClose() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("data");
        writer.close();
        assertThrows(IOException.class, () -> writer.write("more"));
    }

    @Test
    public void testClose_AlreadyClosed() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.close();
        writer.close(); // Should not throw
    }

    @Test
    public void testClose_WithExternalWriter() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write("data");
        writer.close();
        assertThrows(IOException.class, () -> writer.write("more"));
    }

    // === toString() ===

    @Test
    public void testToString() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("Hello ");
        writer.write("World");
        assertEquals("Hello World", writer.toString());
    }

    @Test
    public void testToString_Empty() {
        BufferedWriter writer = new BufferedWriter();
        assertEquals("", writer.toString());
    }

    @Test
    public void testToString_WithExternalWriter() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        writer.write("test");
        String result = writer.toString();
        assertEquals("test", result);
    }

    // === ensureOpen() ===

    @Test
    public void testEnsureOpen_Closed() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.close();
        assertThrows(IOException.class, () -> writer.write('a'));
    }

    // === Combined writes ===

    @Test
    public void testCombinedWrites() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        writer.write("Name: ");
        writer.write(true);
        writer.write(", Age: ");
        writer.writeInt(25);
        writer.write(", Score: ");
        writer.write(99.5);
        String result = writer.toString();
        assertEquals("Name: true, Age: 25, Score: 99.5", result);
    }

    // === Large content to test buffer expansion ===

    @Test
    public void testLargeContent() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            String s = "item" + i + ",";
            writer.write(s);
            expected.append(s);
        }
        assertEquals(expected.toString(), writer.toString());
    }

    @Test
    public void testLargeContent_CharArray() throws IOException {
        BufferedWriter writer = new BufferedWriter();
        char[] largeArray = new char[10000];
        java.util.Arrays.fill(largeArray, 'X');
        writer.write(largeArray);
        assertEquals(10000, writer.toString().length());
    }

    // === Write with external writer large content ===

    @Test
    public void testWriteString_ExternalWriter_LargeContent() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        StringBuilder expected = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            String s = "data" + i;
            writer.write(s);
            expected.append(s);
        }
        writer.flush();
        writer.close();
        assertEquals(expected.toString(), sw.toString());
    }

    @Test
    public void testWriteCharArray_ExternalWriter_LargeContent() throws IOException {
        StringWriter sw = new StringWriter();
        BufferedWriter writer = new BufferedWriter(sw);
        char[] largeArray = new char[10000];
        java.util.Arrays.fill(largeArray, 'Y');
        writer.write(largeArray);
        writer.flush();
        writer.close();
        assertEquals(10000, sw.toString().length());
    }

    // === DummyWriter ===

    @Test
    public void testDummyWriter_write() {
        BufferedWriter.DummyWriter dw = new BufferedWriter.DummyWriter();
        assertThrows(UnsupportedOperationException.class, () -> dw.write(new char[] { 'a' }, 0, 1));
    }

    @Test
    public void testDummyWriter_flush() {
        BufferedWriter.DummyWriter dw = new BufferedWriter.DummyWriter();
        assertThrows(UnsupportedOperationException.class, () -> dw.flush());
    }

    @Test
    public void testDummyWriter_close() {
        BufferedWriter.DummyWriter dw = new BufferedWriter.DummyWriter();
        assertThrows(UnsupportedOperationException.class, () -> dw.close());
    }

    // === Class structure ===

    @Test
    public void testClassStructure() {
        assertTrue(BufferedWriter.class.isSealed());
    }
}
