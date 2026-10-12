package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.exception.UncheckedIOException;

public class HttpLazyStreamTest extends HttpRequestTestSupport {
    private static InputStream responseStream(final boolean async, final HttpClient client, final InputStream raw) throws Exception {
        final HttpRequest request = newOwnedSyncRequest(client, raw, "gzip");
        return async ? request.asyncGet(InputStream.class).get(5, TimeUnit.SECONDS) : request.get(InputStream.class);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void noOpReadsAndSkipsDoNotInitializeOrClose(final boolean async) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger closes = new AtomicInteger();

        try (InputStream stream = responseStream(async, client, countingStream("not gzip".getBytes(StandardCharsets.UTF_8), closes))) {
            assertEquals(0, stream.read(new byte[0]));
            assertEquals(0, stream.read(new byte[1], 1, 0));
            assertEquals(0, stream.skip(0));
            assertEquals(0, stream.skip(-1));
            assertEquals(0, stream.available());
            assertEquals(0, closes.get());
            verify(client, never()).close();

            // A real read still reports the corrupt header and releases the raw stream and owned client.
            assertThrows(UncheckedIOException.class, stream::read);
            assertEquals(1, closes.get());
            verify(client, times(1)).close();
        }

        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void invalidReadArgumentsDoNotConsumeTheBody(final boolean async) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger closes = new AtomicInteger();

        try (InputStream stream = responseStream(async, client, countingStream("not gzip".getBytes(StandardCharsets.UTF_8), closes))) {
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[1], -1, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[1], 0, -1));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[1], 2, 0));
            assertThrows(IndexOutOfBoundsException.class, () -> stream.read(new byte[1], 1, Integer.MAX_VALUE));
            assertThrows(NullPointerException.class, () -> stream.read(null, 0, 1));
            assertEquals(0, closes.get());
            verify(client, never()).close();
            assertThrows(UncheckedIOException.class, stream::read);
        }

        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void availabilityDoesNotAttemptToReadAHeader(final boolean async) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger reads = new AtomicInteger();
        final AtomicInteger closes = new AtomicInteger();
        final InputStream raw = new InputStream() {
            @Override
            public int read() throws IOException {
                reads.incrementAndGet();
                throw new IOException("No response bytes are ready");
            }

            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };

        try (InputStream stream = responseStream(async, client, raw)) {
            assertEquals(0, stream.available());
            assertEquals(0, reads.get());
            assertEquals(0, closes.get());
            verify(client, never()).close();
            assertThrows(UncheckedIOException.class, stream::read);
            assertEquals(1, reads.get());
        }

        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void normalReadsAndPositiveSkipsStillDecode(final boolean async) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger closes = new AtomicInteger();

        try (InputStream stream = responseStream(async, client, countingStream(gzip("hello"), closes))) {
            assertEquals(0, stream.available());
            assertEquals(0, stream.read(new byte[0]));
            assertEquals(1, stream.skip(1));
            assertEquals('e', stream.read());
            assertTrue(stream.available() >= 0);
            assertEquals(0, stream.read(new byte[1], 0, 0));
            assertEquals(0, stream.skip(0));
            assertEquals(0, stream.skip(-1));
            assertEquals("llo", new String(stream.readAllBytes(), StandardCharsets.UTF_8));
            assertEquals(0, closes.get());
            verify(client, never()).close();
        }

        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void closedStreamRejectsOperationsBeforeDecoding(final boolean async) throws Exception {
        assertClosedStreamRejectsOperations(async, false);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void closedStreamRejectsOperationsAfterDecoding(final boolean async) throws Exception {
        assertClosedStreamRejectsOperations(async, true);
    }

    private static void assertClosedStreamRejectsOperations(final boolean async, final boolean initialize) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger closes = new AtomicInteger();
        final InputStream stream = responseStream(async, client, countingStream(gzip("hello"), closes));

        if (initialize) {
            assertEquals('h', stream.read());
        }

        stream.close();
        assertThrows(IOException.class, stream::available);
        assertThrows(IOException.class, stream::read);
        assertThrows(IOException.class, () -> stream.read(new byte[0]));
        assertThrows(IOException.class, () -> stream.skip(0));
        assertThrows(IOException.class, () -> stream.skip(1));
        stream.close();
        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }
}
