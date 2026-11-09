package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Objectory;

public class UtilsTest extends TestBase {

    @Test
    public void testOpenBinaryStreamPreservesUncheckedFreeFailure() throws Exception {
        final Blob blob = mock(Blob.class);
        final RuntimeException freeFailure = new IllegalStateException("free");
        when(blob.getBinaryStream()).thenReturn(new ByteArrayInputStream(new byte[0]));
        doThrow(freeFailure).when(blob).free();

        final InputStream stream = Utils.openBinaryStream(blob);

        assertSame(freeFailure, assertThrows(IllegalStateException.class, stream::close));
    }

    @Test
    public void testOpenCharacterStreamPreservesErrorFromFree() throws Exception {
        final Clob clob = mock(Clob.class);
        final AssertionError freeFailure = new AssertionError("free");
        when(clob.getCharacterStream()).thenReturn(new StringReader("value"));
        doThrow(freeFailure).when(clob).free();

        final Reader reader = Utils.openCharacterStream(clob);

        assertSame(freeFailure, assertThrows(AssertionError.class, reader::close));
    }

    static final class FailingWriter extends Writer {
        private final boolean checked;

        FailingWriter(final boolean checked) {
            this.checked = checked;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            if (checked) {
                throw new IOException("io");
            }
            throw new IllegalStateException("rt");
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }

    private static BufferedWriter dirtyWriter(final boolean checked) throws IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(new FailingWriter(checked));
        bw.write("abc");
        return bw;
    }

    @Test
    public void testRecycleCheckedFailureWithoutPrimary() throws Exception {
        final IOException e = assertThrows(IOException.class, () -> Utils.recycle(dirtyWriter(true), null));
        assertEquals("io", e.getMessage());
    }

    @Test
    public void testRecycleCheckedFailureSuppressedOnPrimary() throws Exception {
        final RuntimeException primary = new RuntimeException("primary");
        Utils.recycle(dirtyWriter(true), primary);
        assertEquals(1, primary.getSuppressed().length);
        assertEquals("io", primary.getSuppressed()[0].getMessage());
    }

    @Test
    public void testRecycleRuntimeFailure() throws Exception {
        final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Utils.recycle(dirtyWriter(false), null));
        assertEquals("rt", e.getMessage());

        final RuntimeException primary = new RuntimeException("primary");
        Utils.recycle(dirtyWriter(false), primary);
        assertEquals(1, primary.getSuppressed().length);
        assertEquals("rt", primary.getSuppressed()[0].getMessage());
    }

    @Test
    public void testNullLobStreamsFreeTheLob() throws Exception {
        final Clob clob = mock(Clob.class);
        when(clob.getAsciiStream()).thenReturn(null);
        assertNull(Utils.openAsciiStream(clob));
        verify(clob).free();

        final Blob blob = mock(Blob.class);
        final SQLException failure = new SQLException("x");
        when(blob.getBinaryStream()).thenThrow(failure);
        final SQLException thrown = assertThrows(SQLException.class, () -> Utils.openBinaryStream(blob));
        assertSame(failure, thrown);
        verify(blob).free();

        final Blob failingFree = mock(Blob.class);
        when(failingFree.getBinaryStream()).thenThrow(new SQLException("y"));
        doThrow(new SQLException("free")).when(failingFree).free();
        final SQLException thrown2 = assertThrows(SQLException.class, () -> Utils.openBinaryStream(failingFree));
        assertEquals("free", thrown2.getSuppressed()[0].getMessage());
    }
}
