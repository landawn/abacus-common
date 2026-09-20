package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class XmlCharacterValidationOrderTest extends TestBase {
    @Test
    void sourceAndRangeAreValidatedBeforeDestination() {
        assertThrows(NullPointerException.class, () -> XmlUtil.writeCharacters((char[]) null, -1, -1, (StringBuilder) null));
        assertThrows(NullPointerException.class, () -> XmlUtil.writeCharacters((char[]) null, -1, -1, (OutputStream) null));
        assertThrows(NullPointerException.class, () -> XmlUtil.writeCharacters((char[]) null, -1, -1, (Writer) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", -1, 1, (StringBuilder) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", -1, 1, (OutputStream) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", -1, 1, (Writer) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 0, -1, (StringBuilder) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(new char[1], 0, -1, (OutputStream) null));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(new char[1], 0, -1, (Writer) null));
    }

    @Test
    void closedBufferedXmlWriterIsCheckedBeforeSourceAndRange() throws IOException {
        BufferedXmlWriter writer = Objectory.createBufferedXmlWriter(new StringWriter());
        writer.close();
        assertThrows(IOException.class, () -> XmlUtil.writeCharacters((char[]) null, -1, -1, writer));
        assertThrows(IOException.class, () -> XmlUtil.writeCharacters((char[]) null, writer));
        assertThrows(IOException.class, () -> XmlUtil.writeCharacters("abc", -1, -1, writer));
    }

    @Test
    void nullStringSlicesAndEscapingArePreserved() {
        StringBuilder output = new StringBuilder("prefix:");
        XmlUtil.writeCharacters((String) null, 1, 3, output);
        XmlUtil.writeCharacters("<&>", 0, 3, output);
        assertEquals("prefix:ull&lt;&amp;&gt;", output.toString());
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("abc", 1, Integer.MAX_VALUE, output));
        assertEquals("prefix:ull&lt;&amp;&gt;", output.toString());
    }

    @Test
    void invalidSlicesLeaveTheStreamUntouchedAndUsable() throws IOException {
        class TrackingOutput extends ByteArrayOutputStream {
            int flushes;
            boolean closed;

            @Override
            public void flush() {
                flushes++;
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackingOutput output = new TrackingOutput();
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters("<&>", 1, Integer.MAX_VALUE, output));
        assertThrows(IndexOutOfBoundsException.class, () -> XmlUtil.writeCharacters(new char[] { '<' }, 0, 2, output));
        assertEquals(0, output.size());
        assertEquals(0, output.flushes);

        XmlUtil.writeCharacters(new char[] { '<', '&', '>' }, 0, 3, output);
        assertEquals("&lt;&amp;&gt;", output.toString(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(1, output.flushes);
        assertFalse(output.closed);
    }

    @Test
    void failedFlushPropagatesWithoutRetryingOrClosingTheCallerWriter() {
        IOException failure = new IOException("flush failed");
        class FailingWriter extends StringWriter {
            int flushes;
            boolean closed;

            @Override
            public void flush() {
                flushes++;
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        FailingWriter output = new FailingWriter();
        Writer failing = new Writer() {
            @Override
            public void write(char[] buffer, int offset, int length) {
                output.write(buffer, offset, length);
            }

            @Override
            public void flush() throws IOException {
                output.flush();
                throw failure;
            }

            @Override
            public void close() {
                output.close();
            }
        };
        assertSame(failure, assertThrows(IOException.class, () -> XmlUtil.writeCharacters("<&>", 0, 3, failing)));
        assertEquals("&lt;&amp;&gt;", output.toString());
        assertEquals(1, output.flushes);
        assertFalse(output.closed);
    }
}
