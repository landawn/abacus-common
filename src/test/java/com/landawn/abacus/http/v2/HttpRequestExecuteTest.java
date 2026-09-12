package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.http.HttpMethod;
import com.sun.net.httpserver.HttpServer;

public class HttpRequestExecuteTest extends HttpRequestTestSupport {

    @Test
    public void testExecute() {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).execute(HttpMethod.GET));
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.POST));
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.PUT));
            assertNotNull(HttpRequest.url(DELETE_URL).execute(HttpMethod.DELETE));
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").execute(HttpMethod.PATCH));
        } catch (Exception e) {
        }
    }

    @Test
    public void testExecute_NullMethod() {
        HttpRequest request = HttpRequest.url(TEST_URL);
        assertThrows(IllegalArgumentException.class, () -> request.execute(null));
        assertThrows(IllegalArgumentException.class, () -> request.execute(null, BodyHandlers.ofString()));
        assertThrows(IllegalArgumentException.class, () -> request.execute(null, String.class));
    }

    @Test
    public void testExecute_Head() throws Exception {
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.getResponseHeaders().set("X-Request-Method", exchange.getRequestMethod());
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        server.start();

        try {
            HttpResponse<String> response = HttpRequest.url(localUrl(server), 1_000L, 5_000L).execute(HttpMethod.HEAD);
            assertNotNull(response);
            assertEquals(200, response.statusCode());
            assertEquals("HEAD", response.headers().firstValue("X-Request-Method").orElse(null));
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testExecute_BodyHandler() {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).execute(HttpMethod.GET, BodyHandlers.ofString()));
            assertNotNull(HttpRequest.url(TEST_URL).execute(HttpMethod.GET, BodyHandlers.ofByteArray()));
        } catch (Exception e) {
        }
    }

    @Test
    public void testExecute_ResultClass() {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).execute(HttpMethod.GET, String.class));
            assertNotNull(HttpRequest.url(TEST_URL).execute(HttpMethod.GET, byte[].class));
        } catch (Exception e) {
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testExecute_RestoresInterruptStatusWhenSendIsInterrupted() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        when(client.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("interrupted"));

        Thread.interrupted();

        try {
            assertThrows(RuntimeException.class, () -> HttpRequest.create("http://localhost", client).execute(HttpMethod.GET, BodyHandlers.ofString()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    public void testExecute_RejectsNullBodyHandlerBeforeBuildingOwnedClient() {
        final HttpClient.Builder clientBuilder = mock(HttpClient.Builder.class);
        final HttpRequest request = new HttpRequest(TEST_URL, null, null, clientBuilder, java.net.http.HttpRequest.newBuilder())
                .closeHttpClientAfterExecution(true);

        assertThrows(IllegalArgumentException.class, () -> request.execute(HttpMethod.GET, (HttpResponse.BodyHandler<String>) null));
        verify(clientBuilder, never()).build();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testExecute_PreservesIOExceptionWhenOwnedClientCloseThrowsError() throws Exception {
        final IOException failure = new IOException("send failure");
        final AssertionError cleanupFailure = new AssertionError("cleanup failure");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(failure);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());

        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> request.execute(HttpMethod.GET, BodyHandlers.ofString()));

        assertSame(failure, thrown.getCause());
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testExecute_PreservesRuntimeFailureWhenOwnedClientCloseThrowsError() throws Exception {
        final IllegalStateException failure = new IllegalStateException("send failure");
        final AssertionError cleanupFailure = new AssertionError("cleanup failure");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(failure);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> request.execute(HttpMethod.GET, BodyHandlers.ofString()));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testExecute_PreservesResponseBodyFailureAndClosesOwnedClientOnce() throws Exception {
        final IllegalStateException failure = new IllegalStateException("response body failure");
        final AssertionError cleanupFailure = new AssertionError("cleanup failure");
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenThrow(failure);
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.send(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> request.execute(HttpMethod.GET, BodyHandlers.ofString()));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    @Test
    public void testExecute_InputStreamBodyCanBeReadBeforeClientCleanup() throws Exception {
        final HttpServer server = startLocalServer("stream body");

        try {
            final HttpResponse<InputStream> response = HttpRequest.url(localUrl(server), 1_000L, 5_000L).execute(HttpMethod.GET, BodyHandlers.ofInputStream());

            assertEquals(200, response.statusCode());

            try (InputStream inputStream = response.body()) {
                assertEquals("stream body", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testExecute_InputStreamPreservesPreviousResponseAfterRedirect() throws Exception {
        final HttpServer server = startRedirectServer("redirect body");

        try {
            final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.ALWAYS).build();
            final String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/redirect";
            final HttpResponse<InputStream> response = HttpRequest.create(URI.create(url), client).execute(HttpMethod.GET, BodyHandlers.ofInputStream());

            assertEquals(200, response.statusCode());
            assertEquals(true, response.previousResponse().isPresent());
            assertEquals(302, response.previousResponse().get().statusCode());

            try (InputStream inputStream = response.body()) {
                assertEquals("redirect body", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            server.stop(0);
        }
    }
}
