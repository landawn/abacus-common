package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class OutputConnectionArgumentValidationTest extends TestBase {
    @Test
    void requiredConnectionIsValidatedForEveryHeaderPath() {
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getOutputStream(null, null, null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getOutputStream(null, ContentFormat.JSON, null, null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getOutputStream(null, null, "text/plain", null));
        assertThrowsExactly(IllegalArgumentException.class, () -> HttpUtil.getOutputStream(null, null, null, "identity"));
    }

    @Test
    void optionalFormatAndHeadersPreserveTheConnectionOutput() throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
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
            public OutputStream getOutputStream() {
                return output;
            }
        };

        assertSame(output, HttpUtil.getOutputStream(connection, null, null, null));
        assertSame(output, HttpUtil.getOutputStream(connection, ContentFormat.JSON, null, null));
        assertEquals("application/json", connection.getRequestProperty(HttpHeaders.Names.CONTENT_TYPE));
    }
}
