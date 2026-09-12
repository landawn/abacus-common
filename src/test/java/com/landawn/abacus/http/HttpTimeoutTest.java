package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.HttpURLConnection;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class HttpTimeoutTest extends TestBase {
    private static final String URL = "http://127.0.0.1:1/";

    @Test
    public void testClientTimeoutsSurviveUnspecifiedRequestSettings() {
        final HttpSettings defaults = HttpSettings.create().setConnectTimeout(123).setReadTimeout(456);
        final HttpClient client = HttpClient.create(URL, 1, 8000, 16000, defaults);
        assertTimeouts(client, null, 123, 456);
        assertTimeouts(client, HttpSettings.create(), 123, 456);
        assertTimeouts(client, HttpSettings.create().header("X-Trace", "request"), 123, 456);
        assertTimeouts(client, HttpRequest.create(client).header("X-Trace", "request").checkSettings(), 123, 456);
    }

    @Test
    public void testEachRequestTimeoutOverridesItsOwnLayer() {
        final HttpClient client = HttpClient.create(URL, 1, 8000, 16000, HttpSettings.create().setConnectTimeout(123).setReadTimeout(456));
        assertTimeouts(client, HttpSettings.create().setConnectTimeout(1), 1, 456);
        assertTimeouts(client, HttpSettings.create().setReadTimeout(2), 123, 2);
        assertTimeouts(client, HttpSettings.create().setConnectTimeout(3).setReadTimeout(4), 3, 4);
        assertTimeouts(client, HttpSettings.create().setConnectTimeout(0).setReadTimeout(0), 123, 456);
    }

    @Test
    public void testConstructorAndLibraryDefaultsAreLastFallbacks() {
        assertTimeouts(HttpClient.create(URL, 1, 8000, 16000, HttpSettings.create()), null, 8000, 16000);
        assertTimeouts(HttpClient.create(URL, 1, 8000, 16000, HttpSettings.create().setReadTimeout(456)), HttpSettings.create(), 8000, 456);
        assertTimeouts(HttpClient.create(URL, 1, 0, 0), null, HttpClient.DEFAULT_CONNECTION_TIMEOUT, HttpClient.DEFAULT_READ_TIMEOUT);
    }

    @Test
    public void testTimeoutSaturationAtEveryLayer() {
        for (final long value : new long[] { Integer.MAX_VALUE - 1L, Integer.MAX_VALUE, Integer.MAX_VALUE + 1L, Long.MAX_VALUE }) {
            final int expected = (int) Math.min(value, Integer.MAX_VALUE);
            assertTimeouts(HttpClient.create(URL, 1, value, value), null, expected, expected);
            final HttpClient client = HttpClient.create(URL, 1, 8000, 16000, HttpSettings.create().setConnectTimeout(value).setReadTimeout(value));
            assertTimeouts(client, HttpSettings.create(), expected, expected);
            assertTimeouts(client, HttpSettings.create().setConnectTimeout(1).setReadTimeout(2), 1, 2);
            assertTimeouts(HttpClient.create(URL), HttpSettings.create().setConnectTimeout(value).setReadTimeout(value), expected, expected);
        }
    }

    private static void assertTimeouts(final HttpClient client, final HttpSettings settings, final int connect, final int read) {
        final HttpURLConnection connection = client.openConnection(HttpMethod.GET, settings, false);
        try {
            assertEquals(connect, connection.getConnectTimeout());
            assertEquals(read, connection.getReadTimeout());
        } finally {
            connection.disconnect();
        }
    }
}
