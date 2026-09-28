package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

/**
 * Pins the shared-client reuse of the v2 {@link HttpRequest}: a connect timeout of 0 runs on the shared
 * default client, and each positive connect timeout runs on one shared client, for up to
 * {@link HttpRequest#MAX_TIMEOUT_CLIENTS} distinct timeouts. Before this, every timeout request built and
 * closed its own client, so it never reused a connection.
 */
public class HttpRequestSharedClientTest extends HttpRequestTestSupport {

    private static Object staticField(final String fieldName) throws Exception {
        final Field field = HttpRequest.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(null);
    }

    @SuppressWarnings("unchecked")
    private static Map<Duration, HttpClient> timeoutClients() throws Exception {
        return (Map<Duration, HttpClient>) staticField("TIMEOUT_CLIENTS");
    }

    private static void assertShared(final HttpRequest request) throws Exception {
        assertNull(field(request, "clientBuilder"));
        assertFalse(booleanField(request, "requireNewClient"));
        assertFalse(booleanField(request, "closeHttpClientAfterExecution"));
    }

    @Test
    public void testZeroConnectTimeoutUsesTheSharedDefaultClient() throws Exception {
        final Object defaultClient = staticField("DEFAULT_HTTP_CLIENT");

        for (final HttpRequest request : new HttpRequest[] { HttpRequest.url(testUrl, 0L, 0L), HttpRequest.url(testUrl, 0L, 5_000L),
                HttpRequest.url(new URL(testUrl), 0L, 5_000L), HttpRequest.url(testUri, 0L, 5_000L) }) {
            assertSame(defaultClient, field(request, "httpClient"));
            assertShared(request);
        }

        assertSame(field(HttpRequest.url(testUrl), "httpClient"), field(HttpRequest.url(testUrl, 0L, 5_000L), "httpClient"));
        assertEquals("GET response", HttpRequest.url(testUrl, 0L, 5_000L).get(String.class));
    }

    @Test
    public void testSameConnectTimeoutSharesOneClient() throws Exception {
        final HttpRequest first = HttpRequest.url(testUrl, 1_234L, 0L);
        final HttpClient client = (HttpClient) field(first, "httpClient");

        if (client == staticField("DEFAULT_HTTP_CLIENT")) {
            return; // the pool was already full in this JVM; the fallback is covered by testFullPoolFallsBackToAnOwnedClient
        }

        assertShared(first);
        assertEquals(Optional.of(Duration.ofMillis(1_234)), client.connectTimeout());
        assertEquals(HttpClient.Redirect.NORMAL, client.followRedirects());

        // Every factory, and the fluent setter on any request still on a shared client, pick the same client.
        final HttpRequest[] sameTimeout = { HttpRequest.url(testUrl, 1_234L, 5_000L), HttpRequest.url(new URL(testUrl), 1_234L, 9L),
                HttpRequest.url(testUri, 1_234L, 0L), HttpRequest.url(testUrl).connectTimeout(1_234L),
                HttpRequest.url(testUrl).connectTimeout(Duration.ofMillis(1_234)), HttpRequest.create(testUrl, null).connectTimeout(Duration.ofMillis(1_234)),
                HttpRequest.url(testUrl, 5_000L, 0L).connectTimeout(Duration.ofMillis(1_234)) };

        for (final HttpRequest request : sameTimeout) {
            assertSame(client, field(request, "httpClient"));
            assertShared(request);
        }

        // A different timeout moves the request to that timeout's client.
        final HttpRequest moved = HttpRequest.url(testUrl, 1_234L, 0L).connectTimeout(Duration.ofMillis(1_235));
        assertNotSame(client, field(moved, "httpClient"));
        assertEquals(Optional.of(Duration.ofMillis(1_235)), connectTimeoutOf(moved));

        // Executing does not close a shared client, so it serves the next request too.
        assertEquals("GET response", HttpRequest.url(testUrl, 1_234L, 5_000L).get(String.class));
        assertEquals("GET response", HttpRequest.url(testUrl, 1_234L, 5_000L).get(String.class));
        assertEquals("GET response", HttpRequest.url(testUrl, 1_234L, 5_000L).asyncGet(String.class).get(5, TimeUnit.SECONDS));
        assertFalse(client.isTerminated());
    }

    @Test
    public void testSharedClientReusesItsConnection() throws Exception {
        final Set<Integer> clientPorts = ConcurrentHashMap.newKeySet();
        final HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            clientPorts.add(exchange.getRemoteAddress().getPort());
            final byte[] bytes = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);

            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();

        try {
            final HttpRequest probe = HttpRequest.url(localUrl(server), 4_321L, 5_000L);

            if (field(probe, "clientBuilder") != null) {
                return; // the pool was already full in this JVM: every request builds its own client
            }

            for (int i = 0; i < 5; i++) {
                assertEquals("ok", HttpRequest.url(localUrl(server), 4_321L, 5_000L).get(String.class));
            }

            // One keep-alive connection served all five requests. An owned client per request used five.
            assertEquals(1, clientPorts.size(), clientPorts.toString());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testSharedClientIsNeverClosedEvenWhenTheRequestIsMarkedOwning() throws Exception {
        // closeHttpClientAfterExecution(true) is package-private; the close helpers must still spare a shared client.
        final HttpRequest request = HttpRequest.create(testUrl, null).closeHttpClientAfterExecution(true).connectTimeout(Duration.ofMillis(1_236));
        final HttpClient client = (HttpClient) field(request, "httpClient");

        if (client == null || field(request, "clientBuilder") != null) {
            return; // the pool was already full in this JVM, so the request owns a client of its own
        }

        assertEquals("GET response", request.get(String.class));

        final HttpRequest streaming = HttpRequest.create(testUrl, null).closeHttpClientAfterExecution(true).connectTimeout(Duration.ofMillis(1_236));
        assertSame(client, field(streaming, "httpClient"));

        try (java.util.stream.Stream<String> lines = streaming.get(java.net.http.HttpResponse.BodyHandlers.ofLines()).body()) {
            assertEquals("GET response", String.join("\n", lines.toList()));
        }

        assertFalse(client.awaitTermination(Duration.ofMillis(50)));
        assertEquals("GET response", HttpRequest.url(testUrl, 1_236L, 0L).get(String.class));
    }

    @Test
    public void testOtherClientSettingsStillBuildAnOwnedClient() throws Exception {
        final Authenticator authenticator = new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication("u", "p".toCharArray());
            }
        };

        final HttpRequest shared = HttpRequest.url(testUrl, 1_234L, 0L);
        final HttpClient sharedClient = (HttpClient) field(shared, "httpClient");
        final HttpRequest request = HttpRequest.url(testUrl, 1_234L, 0L).authenticator(authenticator);

        assertTrue(booleanField(request, "requireNewClient"));
        assertTrue(booleanField(request, "closeHttpClientAfterExecution"));

        final HttpClient built = ((HttpClient.Builder) field(request, "clientBuilder")).build();
        assertEquals(Optional.of(Duration.ofMillis(1_234)), built.connectTimeout());
        assertEquals(Optional.of(authenticator), built.authenticator());
        assertEquals(HttpClient.Redirect.NORMAL, built.followRedirects());

        assertEquals("GET response", request.get(String.class));
        assertFalse(sharedClient.isTerminated());

        // Once a request owns a builder, a later connectTimeout goes to that builder, not to a shared client.
        final HttpRequest ownedThenTimeout = HttpRequest.url(testUrl).authenticator(authenticator).connectTimeout(Duration.ofMillis(1_234));
        assertTrue(booleanField(ownedThenTimeout, "closeHttpClientAfterExecution"));
        assertEquals(Optional.of(Duration.ofMillis(1_234)), connectTimeoutOf(ownedThenTimeout));

        // A caller-supplied client is never swapped for a shared one.
        final HttpRequest caller = HttpRequest.create(testUrl, HttpClient.newHttpClient()).connectTimeout(Duration.ofMillis(1_234));
        assertTrue(booleanField(caller, "requireNewClient"));
        assertTrue(booleanField(caller, "closeHttpClientAfterExecution"));
    }

    @Test
    public void testFullPoolFallsBackToAnOwnedClient() throws Exception {
        final Map<Duration, HttpClient> pool = timeoutClients();
        final Set<Duration> before = new HashSet<>(pool.keySet());
        final int max = HttpRequest.MAX_TIMEOUT_CLIENTS;

        final ExecutorService executor = Executors.newFixedThreadPool(8);

        try {
            // Concurrent misses on 8 x 10 distinct timeouts must not push the pool past its bound.
            final List<Future<?>> futures = new ArrayList<>();

            for (int t = 0; t < 8; t++) {
                final int thread = t;

                futures.add(executor.submit(() -> {
                    for (int i = 0; i < 10; i++) {
                        HttpRequest.url(testUrl, 777_000L + thread * 10 + i, 0L);
                    }

                    return null;
                }));
            }

            for (final Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }

            assertEquals(max, pool.size());

            // A timeout without a shared client builds an owned client with exactly that timeout.
            final HttpRequest fallback = HttpRequest.url(testUrl, 778_000L, 0L);
            assertNull(pool.get(Duration.ofMillis(778_000)));
            assertTrue(booleanField(fallback, "requireNewClient"));
            assertTrue(booleanField(fallback, "closeHttpClientAfterExecution"));
            assertEquals(Optional.of(Duration.ofMillis(778_000)), connectTimeoutOf(fallback));
            assertEquals("GET response", fallback.get(String.class));
            assertEquals(max, pool.size());

            final HttpRequest fluentFallback = HttpRequest.url(testUrl).connectTimeout(Duration.ofMillis(778_000));
            assertTrue(booleanField(fluentFallback, "closeHttpClientAfterExecution"));
            assertEquals(Optional.of(Duration.ofMillis(778_000)), connectTimeoutOf(fluentFallback));

            // Timeouts already in the pool are still shared.
            final Duration pooled = pool.keySet().iterator().next();
            final HttpRequest stillShared = HttpRequest.url(testUrl, pooled.toMillis(), 0L);
            assertSame(pool.get(pooled), field(stillShared, "httpClient"));
            assertShared(stillShared);
        } finally {
            executor.shutdownNow();

            // Give the added slots back so later tests in this JVM still get shared clients.
            for (final Duration key : new ArrayList<>(pool.keySet())) {
                if (!before.contains(key)) {
                    pool.remove(key).close();
                }
            }
        }
    }
}
