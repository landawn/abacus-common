package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.Objectory;

class ResourceTypeOutputExceptionContractTest {
    @Test
    void inputStreamReadsUseCheckedIoForWriterCopiesAndWrappedIoForOtherDestinations() {
        final IOException failure = new IOException("source failed");
        final InputStream source = new InputStream() {
            @Override public int read() throws IOException { throw failure; }
        };
        final InputStreamType type = new InputStreamType();
        assertSame(failure, assertThrowsExactly(IOException.class, () -> type.appendTo(new StringWriter(), source)));
        assertSame(failure, assertThrowsExactly(UncheckedIOException.class, () -> type.appendTo(new StringBuilder(), source)).getCause());
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();
        try {
            assertSame(failure, assertThrowsExactly(UncheckedIOException.class, () -> type.serializeTo(writer, source, null)).getCause());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    void readerCopiesPreserveTheDestinationDependentIoContract() {
        final IOException failure = new IOException("source failed");
        final Reader source = new Reader() {
            @Override public int read(final char[] buffer, final int offset, final int length) throws IOException { throw failure; }
            @Override public void close() { }
        };
        final ReaderType type = new ReaderType();
        assertSame(failure, assertThrowsExactly(IOException.class, () -> type.appendTo(new StringWriter(), source)));
        assertSame(failure, assertThrowsExactly(UncheckedIOException.class, () -> type.appendTo(new StringBuilder(), source)).getCause());
    }
}
