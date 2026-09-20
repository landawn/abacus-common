package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class PooledBufferLifetimeTest extends TestBase {
    private static Object field(final Class<?> owner, final String name, final Object object) throws Exception {
        final Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    @Test
    void readerSuperclassAndResetLockCannotRetainAnySource() throws Exception {
        final var first = new StringReader("first");
        final var reader = new BufferedReader(first);
        assertSame(BufferedReader.DUMMY_READER, field(java.io.BufferedReader.class, "in", reader));
        assertEquals('f', reader.read());
        final String text = new String(new char[] { '\uD83D', '\uDE00' });
        reader.reinit(text);
        assertSame(text, field(Reader.class, "lock", reader));
        reader._reset();
        assertSame(BufferedReader.DUMMY_READER, field(Reader.class, "lock", reader));
        assertNull(field(BufferedReader.class, "str", reader));
        final var second = new StringReader("second\r\nnext");
        reader.reinit(second);
        assertEquals("second", reader.readLine());
        assertEquals("next", reader.readLine());
        reader._reset();
        assertNull(field(BufferedReader.class, "in", reader));
        assertSame(BufferedReader.DUMMY_READER, field(Reader.class, "lock", reader));
        assertSame(BufferedReader.DUMMY_READER, field(java.io.BufferedReader.class, "in", reader));
        assertEquals(-1, second.read()); // reset did not close the borrowed reader
        first.close();
        second.close();
    }

    @Test
    void writerSuperclassCannotRetainTheInitialOrReplacementDestination() throws Exception {
        final var first = new StringWriter();
        final var writer = new BufferedWriter(first);
        assertSame(BufferedWriter.DUMMY_WRITER, field(java.io.BufferedWriter.class, "out", writer));
        writer.write("\uD83D\uDE00");
        writer.flush();
        assertEquals("\uD83D\uDE00", first.toString());
        final var second = new StringWriter();
        writer.reinit(second);
        writer.write("second");
        writer.flush();
        assertEquals("second", second.toString());
        writer._reset();
        assertNull(field(BufferedWriter.class, "out", writer));
        assertNull(field(Writer.class, "lock", writer));
        assertSame(BufferedWriter.DUMMY_WRITER, field(java.io.BufferedWriter.class, "out", writer));
    }

    @Test
    void recyclingPreservesStagedWritesWithoutClosingTheBorrowedDestination() throws Exception {
        final int[] closed = { 0 };
        final var output = new StringWriter() {
            @Override
            public void close() {
                closed[0]++;
            }
        };
        final java.io.BufferedWriter writer = Objectory.createBufferedWriter(output);
        writer.write("staged");
        Objectory.recycle(writer);
        assertEquals("staged", output.toString());
        assertEquals(0, closed[0]);
        assertThrows(NullPointerException.class, () -> new BufferedReader((Reader) null));
        assertThrows(NullPointerException.class, () -> new BufferedWriter((Writer) null));
    }
}
