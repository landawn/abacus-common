package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class NullValidationRestartHttpTest extends TestBase {
    @Test
    public void requiredHarEntriesUseArgumentValidation() {
        assertThrowsExactly(IllegalArgumentException.class, () -> HARUtil.getRequestUrl(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HARUtil.getHttpMethodByRequestEntry(null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HARUtil.getHeadersByRequestEntry(null));
        assertNull(HARUtil.getRequestUrl(Map.of()));
        assertEquals(HttpMethod.GET, HARUtil.getHttpMethodByRequestEntry(Map.of("method", "get")));
        assertNull(HARUtil.getBodyAndMimeTypeByRequestEntry(null)._1);
        assertNull(HARUtil.getBodyAndMimeTypeByRequestEntry(null)._2);
    }

    @Test
    public void requiredConnectionsAndFlushOutputUseArgumentValidation() {
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getContentType((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getContentEncoding((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getAccept((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getAcceptEncoding((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getAcceptCharset((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getContentFormat((HttpURLConnection) null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getInputStream(null, ContentFormat.NONE));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.flush(null));
    }

    @Test
    public void optionalHeadersAndFormatsRetainTheirNullBehavior() throws Exception {
        assertNull(HttpUtil.getContentType((Map<String, ?>) null));
        assertNull(HttpUtil.getContentType((HttpHeaders) null));
        assertNull(HttpUtil.getContentType((HttpSettings) null));
        final InputStream body = new ByteArrayInputStream(new byte[] { 42 });
        final HttpURLConnection connection = new HttpURLConnection(URI.create("http://localhost/").toURL()) {
            @Override
            public void connect() {
            }

            @Override
            public void disconnect() {
            }

            @Override
            public boolean usingProxy() {
                return false;
            }

            @Override
            public Map<String, List<String>> getHeaderFields() {
                return Map.of("Content-Type", List.of("application/json"));
            }

            @Override
            public InputStream getInputStream() {
                return body;
            }
        };
        assertEquals("application/json", HttpUtil.getContentType(connection));
        assertEquals(ContentFormat.JSON, HttpUtil.getContentFormat(connection));
        assertSame(body, HttpUtil.getInputStream(connection, null));
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(42);
        HttpUtil.flush(output);
        assertEquals(1, output.size());
    }
}
