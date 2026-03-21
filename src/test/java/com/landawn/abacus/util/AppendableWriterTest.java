package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AppendableWriterTest extends TestBase {

    private static final class FailingCloseAppendable implements Appendable, AutoCloseable {
        private final IOException failure;

        private FailingCloseAppendable(IOException failure) {
            this.failure = failure;
        }

        @Override
        public Appendable append(CharSequence csq) {
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) {
            return this;
        }

        @Override
        public Appendable append(char c) {
            return this;
        }

        @Override
        public void close() throws IOException {
            throw failure;
        }
    }

    @Test
    public void testConstructorWithValidAppendable() {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        assertEquals("", writer.toString());
    }

    @Test
    public void testConstructorWithNullAppendable() {
        assertThrows(IllegalArgumentException.class, () -> {
            new AppendableWriter(null);
        });
    }

    @Test
    public void testChainedOperations() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("Hello").append(' ').append("World");
            assertEquals("Hello World", sb.toString());
        }
    }

    @Test
    public void testMixedWrites() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Hello");
            writer.write(' ');
            writer.append("World");
            writer.write('!');
            assertEquals("Hello World!", sb.toString());
        }
    }

    @Test
    public void testWithStringBuffer() throws IOException {
        StringBuffer sb = new StringBuffer();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Hello");
            writer.append(" World");
            assertEquals("Hello World", sb.toString());
        }
    }

    @Test
    public void testAppendChar() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append('A');
            writer.append('B');
            writer.append('C');
            assertEquals("ABC", sb.toString());
        }
    }

    @Test
    public void testAppendChar_ReturnsSelf() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            Writer result = writer.append('A');
            assertSame(writer, result);
        }
    }

    @Test
    public void testAppendChar_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.append('A'));
    }

    @Test
    public void testAppendCharSequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("Hello");
            writer.append(" ");
            writer.append("World");
            assertEquals("Hello World", sb.toString());
        }
    }

    @Test
    public void testAppendCharSequenceNull() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append((CharSequence) null);
            assertEquals("null", sb.toString());
        }
    }

    @Test
    public void testAppendCharSequence_ReturnsSelf() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            Writer result = writer.append("Hello");
            assertSame(writer, result);
        }
    }

    @Test
    public void testAppendCharSequence_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.append("test"));
    }

    @Test
    public void testAppendCharSequenceSubsequence() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("Hello World", 0, 5);
            assertEquals("Hello", sb.toString());
        }
    }

    @Test
    public void testAppendCharSequenceSubsequenceMiddle() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("Hello World", 6, 11);
            assertEquals("World", sb.toString());
        }
    }

    @Test
    public void testAppendCharSequenceSubsequenceEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.append("Hello", 0, 0);
            assertEquals("", sb.toString());
        }
    }

    @Test
    public void testAppendCharSequenceSubsequence_ReturnsSelf() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            Writer result = writer.append("Hello", 0, 5);
            assertSame(writer, result);
        }
    }

    @Test
    public void testAppendCharSequenceSubsequence_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.append("Hello", 0, 3));
    }

    @Test
    public void testAppendAfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();

        assertThrows(IOException.class, () -> {
            writer.append('A');
        });
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
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write(65);
            writer.write(66);
            writer.write(67);
            assertEquals("ABC", sb.toString());
        }
    }

    @Test
    public void testWriteInt_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(65));
    }

    @Test
    public void testWriteCharArray() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            char[] chars = { 'H', 'e', 'l', 'l', 'o' };
            writer.write(chars);
            assertEquals("Hello", sb.toString());
        }
    }

    @Test
    public void testWriteCharArrayEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            char[] chars = {};
            writer.write(chars);
            assertEquals("", sb.toString());
        }
    }

    @Test
    public void testWriteCharArray_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(new char[] { 'A' }));
    }

    @Test
    public void testWriteCharArrayWithOffsetAndLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            char[] chars = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };
            writer.write(chars, 6, 5);
            assertEquals("World", sb.toString());
        }
    }

    @Test
    public void testWriteCharArrayFullRange() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            char[] chars = { 'T', 'e', 's', 't' };
            writer.write(chars, 0, 4);
            assertEquals("Test", sb.toString());
        }
    }

    @Test
    public void testWriteCharArrayZeroLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            char[] chars = { 'T', 'e', 's', 't' };
            writer.write(chars, 0, 0);
            assertEquals("", sb.toString());
        }
    }

    @Test
    public void testWriteCharArrayWithOffsetAndLength_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.write(new char[] { 'A', 'B' }, 0, 1));
    }

    @Test
    public void testWriteString() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Hello, World!");
            assertEquals("Hello, World!", sb.toString());
        }
    }

    @Test
    public void testWriteStringEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("");
            assertEquals("", sb.toString());
        }
    }

    @Test
    public void testWriteString_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.write("Test"));
    }

    @Test
    public void testWriteStringWithOffsetAndLength() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Hello, World!", 7, 5);
            assertEquals("World", sb.toString());
        }
    }

    @Test
    public void testWriteStringSubstring() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Testing", 0, 4);
            assertEquals("Test", sb.toString());
        }
    }

    @Test
    public void testWriteStringWithOffsetAndLength_AfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();
        assertThrows(IOException.class, () -> writer.write("Hello", 0, 3));
    }

    @Test
    public void testWriteAfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();

        assertThrows(IOException.class, () -> {
            writer.write("Test");
        });
    }

    @Test
    public void testWriteUnicodeCharacters() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Hello \u4E16\u754C");
            writer.write('!');
            assertTrue(sb.toString().contains("\u4E16\u754C"));
        }
    }

    @Test
    public void testWriteSpecialCharacters() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("\n\t\r");
            assertEquals("\n\t\r", sb.toString());
        }
    }

    @Test
    public void testWriteLargeData() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            StringBuilder largeData = new StringBuilder();
            for (int i = 0; i < 1000; i++) {
                largeData.append("Line ").append(i).append("\n");
            }
            writer.write(largeData.toString());
            assertEquals(largeData.toString(), sb.toString());
        }
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
    public void testWriteStringPortion() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Hello World", 6, 5);
        Assertions.assertEquals("World", sb.toString());
    }

    @Test
    public void testFlush() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write("Test");
            writer.flush();
            assertEquals("Test", sb.toString());
        }
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
    public void testFlushAfterClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.close();

        assertThrows(IOException.class, () -> {
            writer.flush();
        });
    }

    @Test
    public void testClose() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Test");
        writer.close();
        assertEquals("Test", sb.toString());
    }

    @Test
    public void testCloseIdempotent() throws IOException {
        StringBuilder sb = new StringBuilder();
        AppendableWriter writer = new AppendableWriter(sb);
        writer.write("Test");
        writer.close();
        writer.close();
        assertEquals("Test", sb.toString());
    }

    @Test
    public void testClosePropagatesIOExceptionFromUnderlyingAppendable() {
        IOException failure = new IOException("close failed");
        AppendableWriter writer = new AppendableWriter(new FailingCloseAppendable(failure));

        IOException thrown = assertThrows(IOException.class, writer::close);
        assertTrue(thrown == failure);
    }

    @Test
    public void testToString() throws IOException {
        StringBuilder sb = new StringBuilder("Initial");
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            writer.write(" Data");
            assertEquals("Initial Data", writer.toString());
        }
    }

    @Test
    public void testToStringEmpty() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (AppendableWriter writer = new AppendableWriter(sb)) {
            assertEquals("", writer.toString());
        }
    }
}
