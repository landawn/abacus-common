package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilSkipTest extends IOUtilTestSupport {
    @Test
    public void testSkip_InputStream() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 5);

            assertEquals(5, skipped);
            assertEquals('5', is.read());
        }
    }

    @Test
    public void testSkip_InputStreamZeroBytes() throws IOException {
        byte[] data = "Test".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 0);

            assertEquals(0, skipped);
            assertEquals('T', is.read());
        }
    }

    @Test
    public void testSkip_InputStreamBeyondEnd() throws IOException {
        byte[] data = "Short".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            long skipped = IOUtil.skip(is, 100);

            assertEquals(5, skipped);
            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testSkip_InputStreamBreaksOnZeroProgress() throws IOException {
        assertEquals(0, IOUtil.skip(new ZeroThenEofInputStream(), 10));
    }

    @Test
    public void testSkip_InputStreamNegativeBytes() {
        try (InputStream is = new ByteArrayInputStream("Test".getBytes(UTF_8))) {
            assertThrows(IllegalArgumentException.class, () -> {
                IOUtil.skip(is, -5);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkip_Reader() throws IOException {
        try (Reader reader = new StringReader("0123456789ABCDEF")) {
            long skipped = IOUtil.skip(reader, 7);

            assertEquals(7, skipped);
            assertEquals('7', reader.read());
        }
    }

    @Test
    public void testSkip_ReaderZeroChars() throws IOException {
        try (Reader reader = new StringReader("Test")) {
            long skipped = IOUtil.skip(reader, 0);

            assertEquals(0, skipped);
            assertEquals('T', reader.read());
        }
    }

    @Test
    public void testSkip_ReaderBeyondEnd() throws IOException {
        try (Reader reader = new StringReader("Small")) {
            long skipped = IOUtil.skip(reader, 50);

            assertEquals(5, skipped);
            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSkip_ReaderBreaksOnZeroProgress() throws IOException {
        assertEquals(0, IOUtil.skip(new ZeroThenEofReader(), 10));
    }

    @Test
    public void testSkip_ReaderNegativeChars() {
        try (Reader reader = new StringReader("Test")) {
            assertThrows(IllegalArgumentException.class, () -> {
                IOUtil.skip(reader, -3);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkip_InputStreamLargeSkip() throws IOException {
        byte[] largeData = new byte[50000];
        try (InputStream is = new ByteArrayInputStream(largeData)) {
            long skipped = IOUtil.skip(is, 30000);

            assertEquals(30000, skipped);
        }
    }

    @Test
    public void testSkipFully_InputStream() throws IOException {
        byte[] data = "0123456789ABCDEF".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 8);

            assertEquals('8', is.read());
        }
    }

    @Test
    public void testSkipFully_InputStreamExactLength() throws IOException {
        byte[] data = "12345".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 5);

            assertEquals(-1, is.read());
        }
    }

    @Test
    public void testSkipFully_InputStreamBeyondEnd() {
        byte[] data = "Short".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            assertThrows(IOException.class, () -> {
                IOUtil.skipFully(is, 10);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkipFully_InputStreamZero() throws IOException {
        byte[] data = "Test".getBytes(UTF_8);
        try (InputStream is = new ByteArrayInputStream(data)) {
            IOUtil.skipFully(is, 0);

            assertEquals('T', is.read());
        }
    }

    @Test
    public void testSkipFully_Reader() throws IOException {
        try (Reader reader = new StringReader("0123456789")) {
            IOUtil.skipFully(reader, 6);

            assertEquals('6', reader.read());
        }
    }

    @Test
    public void testSkipFully_ReaderExactLength() throws IOException {
        try (Reader reader = new StringReader("ABC")) {
            IOUtil.skipFully(reader, 3);

            assertEquals(-1, reader.read());
        }
    }

    @Test
    public void testSkipFully_ReaderBeyondEnd() {
        try (Reader reader = new StringReader("Tiny")) {
            assertThrows(IOException.class, () -> {
                IOUtil.skipFully(reader, 20);
            });
        } catch (IOException e) {
        }
    }

    @Test
    public void testSkipFully_ReaderZero() throws IOException {
        try (Reader reader = new StringReader("Data")) {
            IOUtil.skipFully(reader, 0);

            assertEquals('D', reader.read());
        }
    }

    @Test
    public void testSkipStaysExactAcrossTheBulkAndReadingPaths() throws IOException {
        // skip() advances a FileInputStream by moving its channel position, and reads-and-discards for every
        // other source. It never calls the stream's own skip(..), which is allowed to fail on a source that
        // cannot seek. These are the cases where the shortcut could go wrong.
        final File file = new File(tempFolder.toFile(), "skip-target.bin");
        final byte[] data = new byte[64 * 1024];

        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i % 251);
        }

        Files.write(file.toPath(), data);

        // 1. A bulk skip on a file must land on exactly the right byte, not merely report the right count.
        try (InputStream is = new FileInputStream(file)) {
            assertEquals(40_000, IOUtil.skip(is, 40_000));
            assertEquals(40_000 % 251, is.read());
        }

        // 2. It must never report bytes past the end.
        try (InputStream is = new FileInputStream(file)) {
            assertEquals(data.length, IOUtil.skip(is, data.length * 4L));
            assertEquals(-1, is.read());
        }

        // 3. A BufferedInputStream holds data the underlying stream no longer knows about; the count must still
        //    be exact once some of it has been consumed.
        try (InputStream is = new java.io.BufferedInputStream(new FileInputStream(file))) {
            is.read();
            assertEquals(9_999, IOUtil.skip(is, 9_999));
            assertEquals(10_000 % 251, is.read());
        }

        // 4. A GZIPInputStream is not a FileInputStream, so it takes the buffered-read path on type alone -
        //    and must still be exact. (It also has no position to move: it reports available()==1 while merely
        //    "not at EOF", which is why an available()-driven shortcut was wrong for it in the first place.)
        final File gz = new File(tempFolder.toFile(), "skip-target.gz");

        try (OutputStream os = new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(gz))) {
            os.write(data);
        }

        try (InputStream is = new java.util.zip.GZIPInputStream(new FileInputStream(gz))) {
            assertEquals(40_000, IOUtil.skip(is, 40_000));
            assertEquals(40_000 % 251, is.read());
        }

        // 5. A stream whose available() over-reports must not let skip run past the real end. Nothing reads
        //    available() any more, so this is now exact by construction rather than by a bound - but it stays
        //    here to pin that no future shortcut reintroduces the dependency.
        final InputStream optimistic = new ByteArrayInputStream(new byte[100]) {
            @Override
            public int available() {
                return Integer.MAX_VALUE;
            }
        };

        assertEquals(100, IOUtil.skip(optimistic, 5_000));
    }

    @Test
    public void testSkipRejectsANullSourceRegardlessOfCount() throws IOException {
        // A null source used to be reported only when the count was positive: skip(null, 0) returned 0 while
        // skip(null, 1) threw NullPointerException, because the zero-count shortcut ran before anything touched
        // the stream. Whether a bad argument was reported must not depend on the count.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip((InputStream) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip((InputStream) null, 5));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip((Reader) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skip((Reader) null, 5));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skipFully((InputStream) null, 0));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.skipFully((Reader) null, 0));

        // A negative count is still rejected, and a valid call still works.
        try (InputStream is = new java.io.ByteArrayInputStream("0123456789".getBytes(UTF_8))) {
            assertThrows(IllegalArgumentException.class, () -> IOUtil.skip(is, -1));
            assertEquals(0, IOUtil.skip(is, 0));
            assertEquals(3, IOUtil.skip(is, 3));
            assertEquals('3', is.read());
        }
    }
}
