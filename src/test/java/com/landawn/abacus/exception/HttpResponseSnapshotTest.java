package com.landawn.abacus.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.http.HttpUtil;

public class HttpResponseSnapshotTest extends TestBase {
    @Test
    public void capturedHeadersDoNotFollowInputMutations() {
        final List<String> values = new ArrayList<>(List.of("original"));
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Value", values);
        final HttpResponseException error = error(headers);
        values.set(0, "changed");
        headers.clear();
        assertEquals("original", error.header("x-value"));
        assertEquals(List.of("original"), error.headers("X-VALUE"));
    }

    @Test
    public void everyHeaderAccessorProtectsCapturedValues() {
        final HttpResponseException error = error(Map.of("X-Value", new ArrayList<>(List.of("a", "b"))));
        assertThrows(UnsupportedOperationException.class, () -> error.headers().clear());
        assertThrows(UnsupportedOperationException.class, () -> error.headers("x-value").add("c"));
        assertThrows(UnsupportedOperationException.class, () -> error.headers().get("X-Value").set(0, "c"));
        assertThrows(UnsupportedOperationException.class, () -> error.headers().entrySet().iterator().next().setValue(List.of("c")));
    }

    @Test
    public void serializedExceptionRetainsHeadersAndOtherContext() throws Exception {
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put(null, List.of("HTTP/1.1 503 Unavailable"));
        headers.put("X-Value", new ArrayList<>(Arrays.asList("\u03bb\ud83d\ude00", null, "")));
        headers.put("X-Null", null);
        headers.put("X-Empty", List.of());
        final HttpResponseException original = error(headers);
        final HttpResponseException restored = roundTrip(original);
        assertEquals(original.headers(), restored.headers());
        assertEquals(original.requestUrl(), restored.requestUrl());
        assertEquals(original.statusCode(), restored.statusCode());
        assertEquals(original.responseMessage(), restored.responseMessage());
        assertEquals(original.responseBody(), restored.responseBody());
        assertEquals(original.getMessage(), restored.getMessage());
        assertEquals(original.getCause().getMessage(), restored.getCause().getMessage());
        assertThrows(UnsupportedOperationException.class, () -> restored.headers("x-value").clear());
        assertNull(restored.header(null));
        assertNull(restored.header("X-Null"));
        assertTrue(restored.headers("X-Null").isEmpty());
        assertTrue(restored.headers("X-Empty").isEmpty());
        assertNull(restored.rawResponseBody());
        assertNull(restored.responseBodyDecodingFailure());
    }

    @Test
    public void serializedDecodingFailureRetainsRawPrefixAndCauseIdentity() throws Exception {
        final byte[] bytes = new byte[HttpUtil.MAX_ERROR_BODY_SIZE + 7];
        Arrays.fill(bytes, (byte) 42);
        final byte[] prefix = Arrays.copyOf(bytes, HttpUtil.MAX_ERROR_BODY_SIZE);
        final EOFException failure = new EOFException("truncated gzip trailer");
        final HttpResponseException original = new HttpResponseException("https://example.test/error", 502, null, Map.of("Content-Encoding", List.of("gzip")),
                "best effort", bytes, failure);
        bytes[0] = 1;

        final HttpResponseException restored = roundTrip(original);
        assertArrayEquals(prefix, original.rawResponseBody());
        assertArrayEquals(prefix, restored.rawResponseBody());
        assertEquals(original.requestUrl(), restored.requestUrl());
        assertEquals(original.statusCode(), restored.statusCode());
        assertEquals(original.headers(), restored.headers());
        assertEquals(original.responseBody(), restored.responseBody());
        assertEquals(original.getMessage(), restored.getMessage());
        assertEquals(EOFException.class, restored.responseBodyDecodingFailure().getClass());
        assertEquals(failure.getMessage(), restored.responseBodyDecodingFailure().getMessage());
        assertNotSame(failure, restored.responseBodyDecodingFailure());
        // Serialization must preserve the shared reference between diagnostics and the cause chain.
        assertSame(restored.responseBodyDecodingFailure(), restored.getCause().getCause());
        final byte[] exposed = restored.rawResponseBody();
        exposed[0] = 2;
        assertArrayEquals(prefix, restored.rawResponseBody());
    }

    @Test
    public void serializedRawDiagnosticsPreserveAbsentAndEmptyValues() throws Exception {
        for (final byte[] raw : new byte[][] { null, new byte[0], { 1, 2, 3 } }) {
            final HttpResponseException restored = roundTrip(new HttpResponseException(null, 500, null, null, null, raw, null));
            assertArrayEquals(raw, restored.rawResponseBody());
            assertNull(restored.responseBodyDecodingFailure());
            assertNull(restored.getCause().getCause());
        }
    }

    @Test
    public void nullEmptyAndMissingHeadersRemainSafe() throws Exception {
        for (HttpResponseException error : List.of(error(null), error(Map.of()), roundTrip(error(null)))) {
            assertTrue(error.headers().isEmpty());
            assertNull(error.header(null));
            assertNull(error.header(""));
            assertNull(error.header("missing"));
            assertTrue(error.headers(null).isEmpty());
            assertTrue(error.headers("").isEmpty());
        }
        final HttpResponseException empty = new HttpResponseException(null, 500, null, null, null);
        assertEquals("", empty.responseBody());
    }

    private static HttpResponseException error(final Map<String, List<String>> headers) {
        return new HttpResponseException("https://example.test/\u03bb", 503, "Unavailable", headers, "\u03bb\ud83d\ude00");
    }

    private static HttpResponseException roundTrip(final HttpResponseException original) throws Exception {
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            return (HttpResponseException) in.readObject();
        }
    }

    // a13 F-4 / a06 F-7: a status line without a reason phrase must not render as "503: null" / "503: ".
    @Test
    public void messageOmitsNullOrEmptyStatusMessage() {
        assertEquals("503", new HttpResponseException("u", 503, null, null, null).getMessage());
        assertEquals("503", new HttpResponseException("u", 503, "", null, "").getMessage());
        assertEquals("302", new HttpResponseException(null, 302, null, Map.of(), null).getMessage());
        assertEquals("503. body", new HttpResponseException("u", 503, null, null, "body").getMessage());
        assertEquals("503. λ😀", new HttpResponseException("u", 503, "", null, "λ😀").getMessage());
        assertFalse(new HttpResponseException("u", 503, null, null, null).getMessage().contains("null"));
        // The cause carries the same rendered message and the structured accessor still reports the raw value.
        final HttpResponseException e = new HttpResponseException("u", 503, null, null, "body");
        assertEquals(e.getMessage(), e.getCause().getMessage());
        assertNull(e.responseMessage());
        assertEquals("", new HttpResponseException("u", 503, "", null, null).responseMessage());
    }

    @Test
    public void messageKeepsNonEmptyStatusMessageAndBody() {
        assertEquals("503: Unavailable", new HttpResponseException("u", 503, "Unavailable", null, null).getMessage());
        assertEquals("503: Unavailable", new HttpResponseException("u", 503, "Unavailable", null, "").getMessage());
        assertEquals("503: Unavailable. λ😀", error(null).getMessage());
        // A whitespace-only phrase is not empty and is kept as-is.
        assertEquals("503:  ", new HttpResponseException("u", 503, " ", null, null).getMessage());
    }

    @Test
    public void messageTruncatesBodyOnlyBeyondTheLimit() {
        final int limit = com.landawn.abacus.http.HttpUtil.MAX_ERROR_BODY_IN_MESSAGE;
        final String exact = "x".repeat(limit);
        final String over = exact + "y";
        assertEquals("500. " + exact, new HttpResponseException("u", 500, null, null, exact).getMessage());
        assertEquals("500: Err. " + exact, new HttpResponseException("u", 500, "Err", null, exact).getMessage());
        assertEquals("500. " + exact + "... (truncated)", new HttpResponseException("u", 500, null, null, over).getMessage());
        assertEquals("500: Err. " + exact + "... (truncated)", new HttpResponseException("u", 500, "Err", null, over).getMessage());
        // responseBody() is what was captured, untouched by the message rendering.
        assertEquals(over, new HttpResponseException("u", 500, "Err", null, over).responseBody());
    }

    @Test
    public void headerLookupPrefersFirstCaseVariantInEncounterOrder() {
        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("X-Value", List.of("first", "first-2"));
        headers.put("x-value", List.of("second"));
        headers.put("X-VALUE", List.of("third"));
        final HttpResponseException error = error(headers);
        for (final String name : List.of("X-Value", "x-value", "X-VALUE", "x-VaLuE")) {
            assertEquals("first", error.header(name));
            assertEquals(List.of("first", "first-2"), error.headers(name));
        }
        assertNull(error.header(null));
        assertTrue(error.headers(null).isEmpty());
        // The snapshot itself keeps every variant.
        assertEquals(3, error.headers().size());
        assertEquals(List.of("third"), error.headers().get("X-VALUE"));
    }
}
