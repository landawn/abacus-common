package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class StringWriter2025Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        StringWriter writer = new StringWriter();
        assertNotNull(writer);
        assertEquals("", writer.toString());
    }

    @Test
    public void testConstructorWithInitialSize() {
        StringWriter writer = new StringWriter(100);
        assertNotNull(writer);
        assertEquals("", writer.toString());
    }

    @Test
    public void testConstructorWithStringBuilder() {
        StringBuilder sb = new StringBuilder("Existing content");
        StringWriter writer = new StringWriter(sb);
        assertNotNull(writer);
        assertEquals("Existing content", writer.toString());
        assertSame(sb, writer.stringBuilder());
    }

    @Test
    public void testWrite_int() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write(65);   // 'A'
        writer.write(66);   // 'B'
        assertEquals("AB", writer.toString());
    }

    @Test
    public void testWrite_charArray() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write(new char[] { 'H', 'e', 'l', 'l', 'o' });
        assertEquals("Hello", writer.toString());
    }

    @Test
    public void testWrite_charArraySubset() throws Exception {
        StringWriter writer = new StringWriter();
        char[] chars = "Hello World".toCharArray();
        writer.write(chars, 6, 5);
        assertEquals("World", writer.toString());
    }

    @Test
    public void testWrite_string() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("Hello");
        writer.write(" ");
        writer.write("World");
        assertEquals("Hello World", writer.toString());
    }

    @Test
    public void testWrite_stringSubset() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("Hello World", 6, 5);
        assertEquals("World", writer.toString());
    }

    @Test
    public void testAppend_char() throws Exception {
        StringWriter writer = new StringWriter();
        writer.append('H').append('i').append('!');
        assertEquals("Hi!", writer.toString());
    }

    @Test
    public void testAppend_charSequence() throws Exception {
        StringWriter writer = new StringWriter();
        writer.append("Hello").append(" ").append("World");
        assertEquals("Hello World", writer.toString());
    }

    @Test
    public void testAppend_nullCharSequence() throws Exception {
        StringWriter writer = new StringWriter();
        writer.append((CharSequence) null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppend_charSequenceSubset() throws Exception {
        StringWriter writer = new StringWriter();
        writer.append("Hello World", 0, 5);
        assertEquals("Hello", writer.toString());
    }

    @Test
    public void testStringBuilder() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("Hello");
        StringBuilder sb = writer.stringBuilder();
        assertNotNull(sb);
        assertEquals("Hello", sb.toString());

        // Modifying StringBuilder affects writer
        sb.append(" World");
        assertEquals("Hello World", writer.toString());
    }

    @Test
    public void testFlush() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("Test");
        writer.flush();   // Should not throw exception
        assertEquals("Test", writer.toString());
    }

    @Test
    public void testClose() throws Exception {
        StringWriter writer = new StringWriter();
        writer.write("Test");
        writer.close();   // Should not throw exception

        // Can still be used after close
        writer.write(" More");
        assertEquals("Test More", writer.toString());
    }

    @Test
    public void testMethodChaining() throws Exception {
        StringWriter writer = new StringWriter();
        StringWriter result = writer.append("Hello").append(' ').append("World").append("!", 0, 1);
        assertSame(writer, result);
        assertEquals("Hello World!", writer.toString());
    }

    @Test
    public void testToString() {
        StringWriter writer = new StringWriter();
        writer.write("Test Content");
        assertEquals("Test Content", writer.toString());
    }

    @Test
    public void testEmptyWriter() {
        StringWriter writer = new StringWriter();
        assertEquals("", writer.toString());
        assertEquals(0, writer.toString().length());
    }
}
