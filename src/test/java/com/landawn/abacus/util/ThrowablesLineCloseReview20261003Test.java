package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

@Tag("unit")
class ThrowablesLineCloseReview20261003Test extends TestBase {
    @ParameterizedTest
    @CsvSource({ "hasNext,false", "next,false", "count,false", "toList,false", "hasNext,true", "next,true", "count,true", "toList,true" })
    void readCallbackCannotPublishAfterClose(final String operation, final boolean eof) throws IOException {
        final AtomicReference<Throwables.Iterator<String, IOException>> ref = new AtomicReference<>();
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final BufferedReader reader = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() {
                reads.incrementAndGet();
                ref.get().closeResource();
                return eof ? null : "discard this line";
            }

            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                super.close();
            }
        };
        final Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(reader);
        ref.set(lines);
        switch (operation) {
            case "hasNext" -> assertFalse(lines.hasNext());
            case "next" -> assertThrows(NoSuchElementException.class, lines::next);
            case "count" -> assertEquals(0L, lines.count());
            case "toList" -> assertEquals(List.of(), lines.toList());
            default -> throw new AssertionError(operation);
        }
        assertFalse(lines.hasNext());
        assertThrows(NoSuchElementException.class, lines::next);
        lines.closeResource();
        assertEquals(1, reads.get());
        assertEquals(1, closes.get());
    }

    @ParameterizedTest
    @CsvSource({ "false,false", "true,false", "false,true", "true,true" })
    void closeCallbackSeesExhaustion(final boolean prefetch, final boolean closeFails) throws IOException {
        final AtomicReference<Throwables.Iterator<String, IOException>> ref = new AtomicReference<>();
        final AtomicInteger closes = new AtomicInteger();
        final IOException failure = new IOException("close failed");
        final BufferedReader reader = new BufferedReader(new StringReader("first\nsecond")) {
            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                assertFalse(ref.get().hasNext());
                assertThrows(NoSuchElementException.class, ref.get()::next);
                assertEquals(0L, ref.get().count());
                ref.get().closeResource();
                super.close();
                if (closeFails) {
                    throw failure;
                }
            }
        };
        final Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(reader);
        ref.set(lines);
        if (prefetch) {
            assertTrue(lines.hasNext());
        }
        if (closeFails) {
            assertSame(failure, assertThrows(UncheckedIOException.class, lines::closeResource).getCause());
        } else {
            lines.closeResource();
        }
        assertFalse(lines.hasNext());
        lines.closeResource();
        assertEquals(1, closes.get());
    }

    @Test
    void readFailureAfterClosingIsPreserved() throws IOException {
        final AtomicReference<Throwables.Iterator<String, IOException>> ref = new AtomicReference<>();
        final IOException failure = new IOException("read failed");
        final BufferedReader reader = new BufferedReader(new StringReader("")) {
            @Override
            public String readLine() throws IOException {
                ref.get().closeResource();
                throw failure;
            }
        };
        final Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(reader);
        ref.set(lines);
        assertSame(failure, assertThrows(IOException.class, lines::hasNext));
        assertFalse(lines.hasNext());
    }

    @Test
    void ordinaryIterationPreservesBlankLinesAndExplicitClose() throws IOException {
        final AtomicInteger closes = new AtomicInteger();
        final BufferedReader reader = new BufferedReader(new StringReader("first\r\n\nlast")) {
            @Override
            public void close() throws IOException {
                closes.incrementAndGet();
                super.close();
            }
        };
        final Throwables.Iterator<String, IOException> lines = Throwables.Iterator.ofLines(reader);
        assertTrue(lines.hasNext());
        assertTrue(lines.hasNext());
        assertEquals(List.of("first", "", "last"), lines.toList());
        assertEquals(0, closes.get());
        lines.closeResource();
        lines.closeResource();
        assertEquals(1, closes.get());
        assertFalse(lines.hasNext());
    }
}
