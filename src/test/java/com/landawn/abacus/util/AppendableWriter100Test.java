package com.landawn.abacus.util;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AppendableWriter100Test extends TestBase {

    @Test
    public void testConstructorWithNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            new AppendableWriter(null);
        });
    }

    @Test
    public void testAppendChar() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.append('A');
        writer.append('B');
        Assertions.assertEquals("AB", sb.toString());
    }

    @Test
    public void testAppendCharSequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.append("Hello");
        writer.append(" World");
        Assertions.assertEquals("Hello World", sb.toString());
    }

    @Test
    public void testAppendCharSequenceNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.append(null);
        Assertions.assertEquals("null", sb.toString());
    }

    @Test
    public void testAppendSubsequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.append("Hello World", 0, 5);
        Assertions.assertEquals("Hello", sb.toString());
    }

    @Test
    public void testWriteInt() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write(65);
        Assertions.assertEquals("A", sb.toString());
    }

    @Test
    public void testWriteCharArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        char[] chars = { 'H', 'e', 'l', 'l', 'o' };
        writer.write(chars);
        Assertions.assertEquals("Hello", sb.toString());
    }

    @Test
    public void testWriteCharArrayPortion() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        char[] chars = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
        writer.write(chars, 6, 5);
        Assertions.assertEquals("World", sb.toString());
    }

    @Test
    public void testWriteString() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Hello");
        Assertions.assertEquals("Hello", sb.toString());
    }

    @Test
    public void testWriteStringPortion() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Hello World", 6, 5);
        Assertions.assertEquals("World", sb.toString());
    }

    @Test
    public void testFlushWithFlushable() throws IOException {
        StringWriter sw = new StringWriter();
        AppendableWriter writer = new AppendableWriter(sw);
        writer.write("Test");
        writer.flush();
        Assertions.assertEquals("Test", sw.toString());
    }

    @Test
    public void testFlushWithNonFlushable() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Test");
        writer.flush();
        Assertions.assertEquals("Test", sb.toString());
    }

    @Test
    public void testClose() throws IOException {
        StringWriter sw = new StringWriter();
        AppendableWriter writer = new AppendableWriter(sw);
        writer.write("Test");
        writer.close();
        Assertions.assertEquals("Test", sw.toString());
    }

    @Test
    public void testWriteAfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        Assertions.assertThrows(IOException.class, () -> {
            writer.write("Test");
        });
    }

    @Test
    public void testToString() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Hello World");
        Assertions.assertEquals("Hello World", writer.toString());
    }
}
