package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for cycle 2 of the sixth independent review pass over {@code IOUtil}
 * (run E, 2026-08-30).
 *
 * <ul>
 *   <li><b>D-001</b> - {@code writeLines(Iterator|Iterable, Writer, boolean)} reports a write failure as the
 *       {@code IOException} it declares. It used to report an unchecked
 *       {@code com.landawn.abacus.exception.UncheckedIOException} instead, and to discard the primary failure
 *       altogether: the pooled {@code BufferedWriter} taken for the operation is released in a
 *       {@code finally}, and releasing it flushes whatever it still holds into the destination that has just
 *       failed. Which of the two a caller saw depended on whether their {@code Writer} happened to be a
 *       {@code java.io.BufferedWriter} already, in which case no pooled writer is taken at all.</li>
 * </ul>
 */
public class IOUtilWriteLinesFailureTest extends TestBase {

    @TempDir
    Path tempFolder;

    /** A {@code Writer} whose {@code write}/{@code flush} fail once {@code failAfter} characters are in. */
    private static final class FailingWriter extends Writer {
        private final int failAfter;
        private int accepted;

        FailingWriter(final int failAfter) {
            this.failAfter = failAfter;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            if (accepted >= failAfter) {
                throw new IOException("PRIMARY");
            }

            accepted += len;
        }

        @Override
        public void flush() throws IOException {
            if (accepted >= failAfter) {
                throw new IOException("PRIMARY");
            }
        }

        @Override
        public void close() {
            // nothing to release
        }
    }

    private static List<String> manyLines() {
        final List<String> lines = new ArrayList<>();

        for (int i = 0; i < 4000; i++) {
            lines.add("line-" + i + "-0123456789012345678901234567890123456789");
        }

        return lines;
    }

    @Test
    public void testWriteLinesReportsAWriteFailureAsTheIOExceptionItDeclares() {
        final List<String> lines = manyLines();

        for (final int failAfter : new int[] { 0, 100, 50_000 }) {
            // UncheckedIOException is NOT an IOException, so these assertions fail outright on the old code.
            final IOException fromIterable = assertThrows(IOException.class, () -> IOUtil.writeLines(lines, new FailingWriter(failAfter), true));
            assertEquals("PRIMARY", fromIterable.getMessage(), "the primary failure must survive, not the cleanup's");

            final IOException fromIterator = assertThrows(IOException.class, () -> IOUtil.writeLines(lines.iterator(), new FailingWriter(failAfter), true));
            assertEquals("PRIMARY", fromIterator.getMessage());

            final IOException noFlush = assertThrows(IOException.class, () -> IOUtil.writeLines(lines, new FailingWriter(failAfter), false));
            assertEquals("PRIMARY", noFlush.getMessage());

            // Control: a caller-supplied java.io.BufferedWriter takes no pooled writer, and always reported
            // the failure correctly - the two paths must not disagree.
            final IOException buffered = assertThrows(IOException.class,
                    () -> IOUtil.writeLines(lines, new java.io.BufferedWriter(new FailingWriter(failAfter)), true));
            assertEquals("PRIMARY", buffered.getMessage());
        }
    }

    @Test
    public void testTheSiblingWriteTerminalsAlreadyReportedItCorrectly() {
        final List<String> lines = manyLines();

        final IOException fromWriteLine = assertThrows(IOException.class, () -> IOUtil.writeLine("x", new FailingWriter(0), true));
        assertEquals("PRIMARY", fromWriteLine.getMessage());

        final IOException fromWriteReader = assertThrows(IOException.class,
                () -> IOUtil.write(new StringReader(String.join("\n", lines)), new FailingWriter(0), true));
        assertEquals("PRIMARY", fromWriteReader.getMessage());
    }

    @Test
    public void testWriteLinesStillWorksAndTheWriterPoolIsUnharmed() throws Exception {
        // The pooled writer is returned to the pool on both paths, so an ordinary write right after a failed
        // one must still produce exactly the right bytes.
        final List<String> lines = manyLines();

        assertThrows(IOException.class, () -> IOUtil.writeLines(lines, new FailingWriter(10), true));

        final File out = new File(tempFolder.toFile(), "after-failure.txt");
        IOUtil.writeLines(Arrays.asList("a", "b", "c"), out);
        assertEquals("a\nb\nc\n", IOUtil.readAllToString(out));

        final com.landawn.abacus.util.StringWriter sw = IOUtil.newStringWriter();
        IOUtil.writeLines(Arrays.asList("x", "y"), sw, true);
        assertEquals("x\ny\n", sw.toString());

        // and the empty / null-line contracts are untouched
        final File empty = new File(tempFolder.toFile(), "empty.txt");
        IOUtil.write("OLD", empty);
        IOUtil.writeLines(new ArrayList<>(), empty);
        assertEquals(0, empty.length());

        final File withNull = new File(tempFolder.toFile(), "with-null.txt");
        IOUtil.writeLines(Arrays.asList("a", null, "b"), withNull);
        assertEquals("a\nnull\nb\n", IOUtil.readAllToString(withNull));

        final File utf16 = new File(tempFolder.toFile(), "u16.txt");
        IOUtil.writeLines(Arrays.asList("a", "b"), StandardCharsets.UTF_16, utf16);
        assertEquals(Arrays.asList("a", "b"), IOUtil.readAllLines(utf16, StandardCharsets.UTF_16));
        assertTrue(utf16.length() > 0);
    }
}
