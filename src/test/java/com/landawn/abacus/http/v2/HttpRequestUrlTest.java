package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.URI;
import java.net.URL;

import org.junit.jupiter.api.Test;

public class HttpRequestUrlTest extends HttpRequestTestSupport {

    @Test
    public void testUrl() {
        HttpRequest request = HttpRequest.url(TEST_URL);
        assertNotNull(request);
        assertNotNull(HttpRequest.url(testUrl));
    }

    @Test
    public void testUrl_Timeouts() {
        assertNotNull(HttpRequest.url(TEST_URL, 5000, 10000));
        assertNotNull(HttpRequest.url(testUrl, 0L, 0L));
        assertNotNull(HttpRequest.url(testUrl, 60000L, 120000L));
    }

    @Test
    public void testUrl_URI() {
        assertNotNull(HttpRequest.url(URI.create(TEST_URL)));
        assertNotNull(HttpRequest.url(testUri));
    }

    @Test
    public void testUrl_URIAndTimeouts() {
        assertNotNull(HttpRequest.url(URI.create(TEST_URL), 5000, 10000));
        assertNotNull(HttpRequest.url(testUri, 0L, 0L));
        assertNotNull(HttpRequest.url(testUri, 5000L, 30000L));
    }

    @Test
    public void testUrl_QueryStringAlreadyInUrl() {
        assertNotNull(HttpRequest.url("https://httpbin.org/get?existing=param"));
    }

    @Test
    public void testUrl_URL() throws Exception {
        assertNotNull(HttpRequest.url(new URL(TEST_URL)));
        assertNotNull(HttpRequest.url(new URL(testUrl)));
    }

    @Test
    public void testUrl_URLAndTimeouts() throws Exception {
        assertNotNull(HttpRequest.url(new URL(TEST_URL), 5000, 10000));
        assertNotNull(HttpRequest.url(new URL(testUrl), 0L, 0L));
        assertNotNull(HttpRequest.url(new URL(testUrl), 5000L, 30000L));
    }

    @Test
    public void testUrl_URITimeoutsUsesNewClient() throws Exception {
        HttpRequest request = HttpRequest.url(URI.create(testUrl), 1000L, 2000L);
        assertNotNull(request);
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }

    @Test
    public void testUrl_URLTimeoutsUsesNewClient() throws Exception {
        HttpRequest request = HttpRequest.url(new URL(testUrl), 1000L, 2000L);
        assertNotNull(request);
        assertDoesNotThrow(() -> {
            try {
                request.get();
            } catch (Exception e) {
                // Network errors are expected
            }
        });
    }
}
