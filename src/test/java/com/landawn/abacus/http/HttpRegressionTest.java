package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.HttpResponseException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IOUtil;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Regression coverage for the 2026-09-02 review of {@code HttpClient}, {@code HttpRequest},
 * {@code OkHttpRequest} and {@code WebUtil}.
 *
 * <p>Each test names the finding it locks down. The recording server captures the request line,
 * headers and body, which is what the existing suites were missing and why several of these defects
 * went unnoticed.</p>
 */
public class HttpRegressionTest extends TestBase {

    private HttpServer server;
    private String baseUrl;
    private final List<Recorded> recorded = Collections.synchronizedList(new ArrayList<>());
    private volatile int responseCode = 200;
    private volatile String responseBody = "{}";
    private volatile String responseContentType = "application/json";

    /** One recorded request: method, path (with query), headers and body bytes. */
    private static final class Recorded {
        private final String method;
        private final String path;
        private final Map<String, String> headers = new LinkedHashMap<>();
        private final byte[] body;

        private Recorded(final HttpExchange exchange, final byte[] body) {
            method = exchange.getRequestMethod();
            path = exchange.getRequestURI().toString();
            this.body = body;

            exchange.getRequestHeaders().forEach((name, values) -> headers.put(name.toLowerCase(Locale.ROOT), String.join(", ", values)));
        }

        private String header(final String name) {
            return headers.get(name.toLowerCase(Locale.ROOT));
        }

        private String bodyAsString() {
            return new String(body, StandardCharsets.UTF_8);
        }
    }

    @BeforeEach
    public void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            final byte[] body = exchange.getRequestBody().readAllBytes();
            recorded.add(new Recorded(exchange, body));

            final byte[] payload = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", responseContentType);

            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(responseCode, -1);
            } else {
                exchange.sendResponseHeaders(responseCode, payload.length);
                exchange.getResponseBody().write(payload);
            }

            exchange.close();
        });
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
    }

    @AfterEach
    public void tearDown() {
        server.stop(0);
        recorded.clear();
    }

    private Recorded last() {
        assertFalse(recorded.isEmpty(), "no request reached the server");
        return recorded.get(recorded.size() - 1);
    }

    // ------------------------------------------------------------------------------------------
    // B1: the Content-Type on the wire must describe the bytes actually written.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testSerializedObjectBodyIsLabelledJson() {
        HttpClient.create(baseUrl).post(new LinkedHashMap<>(Map.of("k", "v")), String.class);

        // Previously HttpURLConnection supplied its legacy application/x-www-form-urlencoded default
        // while the body was serialized as JSON by the default parser.
        assertEquals("application/json", last().header("Content-Type"));
        assertEquals("{\"k\": \"v\"}", last().bodyAsString());
    }

    @Test
    public void testRawStringBodyIsLabelledTextPlain() {
        HttpClient.create(baseUrl).post("raw-string", String.class);

        assertEquals("text/plain; charset=UTF-8", last().header("Content-Type"));
        assertEquals("raw-string", last().bodyAsString());
    }

    @Test
    public void testRawReaderBodyIsLabelledTextPlain() {
        HttpClient.create(baseUrl).post(new StringReader("reader-content"), String.class);

        assertEquals("text/plain; charset=UTF-8", last().header("Content-Type"));
        assertEquals("reader-content", last().bodyAsString());
    }

    @Test
    public void testRawByteArrayBodyIsLabelledOctetStream() {
        HttpClient.create(baseUrl).post(new byte[] { 1, 2, 3 }, String.class);

        assertEquals("application/octet-stream", last().header("Content-Type"));
        assertEquals(3, last().body.length);
    }

    @Test
    public void testRawInputStreamBodyIsLabelledOctetStream() {
        HttpClient.create(baseUrl).post(new ByteArrayInputStream(new byte[] { 9, 9 }), String.class);

        assertEquals("application/octet-stream", last().header("Content-Type"));
        assertEquals(2, last().body.length);
    }

    @Test
    public void testFileBodyIsLabelledOctetStream() throws IOException {
        final File file = File.createTempFile("http-review", ".bin");
        file.deleteOnExit();
        Files.write(file.toPath(), new byte[] { 4, 5, 6, 7 });

        HttpClient.create(baseUrl).post(file, String.class);

        assertEquals("application/octet-stream", last().header("Content-Type"));
        assertEquals(4, last().body.length);
    }

    @Test
    public void testExplicitContentTypeStillWins() {
        final HttpSettings settings = HttpSettings.create().setContentType("application/vnd.acme+json");

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post(new LinkedHashMap<>(Map.of("k", "v")), String.class);

        assertEquals("application/vnd.acme+json", last().header("Content-Type"));
    }

    @Test
    public void testExplicitContentFormatStillWins() {
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.FORM_URL_ENCODED);

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post(new LinkedHashMap<>(Map.of("k", "v")), String.class);

        assertEquals("application/x-www-form-urlencoded", last().header("Content-Type"));
        assertEquals("k=v", last().bodyAsString());
    }

    @Test
    public void testCompressionOnlyFormatStillLabelsTheBodyJson() throws IOException {
        // ContentFormat.GZIP declares no media type, but the body is still serialized by the
        // default JSON parser, so the header must say application/json - not octet-stream.
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.GZIP);

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post(new LinkedHashMap<>(Map.of("k", "v")), String.class);

        assertEquals("application/json", last().header("Content-Type"));
        assertEquals("gzip", last().header("Content-Encoding"));

        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(last().body))) {
            assertEquals("{\"k\": \"v\"}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testCompressionOnlyFormatKeepsARawTextBodyTextPlain() throws IOException {
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.GZIP);

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post("plain", String.class);

        assertEquals("text/plain; charset=UTF-8", last().header("Content-Type"));
        assertEquals("gzip", last().header("Content-Encoding"));

        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(last().body))) {
            assertEquals("plain", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testKryoFormatLabelsTheBodyApplicationKryo() {
        // A byte[] payload takes the raw-write path, so this exercises the media type without
        // requiring a Kryo parser on the classpath.
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.KRYO);

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post(new byte[] { 1, 2, 3 }, String.class);

        assertEquals("application/kryo", last().header("Content-Type"));
        assertEquals("kryo", last().header("Content-Encoding"));
        assertEquals(3, last().body.length);
    }

    @Test
    public void testHttpSettingsToStringRendersEffectiveFlagValues() {
        // The flags are stored as tri-state; toString must still print booleans, not "null".
        final String rendered = HttpSettings.create().toString();

        assertTrue(rendered.contains("useCaches=false"), rendered);
        assertTrue(rendered.contains("doInput=true"), rendered);
        assertTrue(rendered.contains("doOutput=true"), rendered);
        assertTrue(rendered.contains("isOneWayRequest=false"), rendered);
        assertTrue(HttpSettings.create().doOutput(false).toString().contains("doOutput=false"));
    }

    @Test
    public void testMalformedUrlIsReportedTheSameWayForStringAndUrlForms() throws Exception {
        final IllegalArgumentException fromString = assertThrows(IllegalArgumentException.class, () -> HttpClient.create("http://exa mple.com/x"));
        @SuppressWarnings("deprecation")
        final URL url = new URL(baseUrl + "a b");
        final IllegalArgumentException fromUrl = assertThrows(IllegalArgumentException.class, () -> HttpClient.create(url));

        assertTrue(fromString.getMessage().startsWith("Invalid URI syntax in url: "), fromString.getMessage());
        assertTrue(fromUrl.getMessage().startsWith("Invalid URI syntax in url: "), fromUrl.getMessage());
    }

    @Test
    public void testHttpRequestBodyIsLabelledJson() {
        HttpRequest.create(HttpClient.create(baseUrl)).body(new LinkedHashMap<>(Map.of("k", "v"))).post(String.class);

        assertEquals("application/json", last().header("Content-Type"));
    }

    @Test
    public void testOptionsBodyIsLabelledJson() {
        HttpClient.create(baseUrl).execute(HttpMethod.OPTIONS, new LinkedHashMap<>(Map.of("q", "1")), null, String.class);

        assertEquals("OPTIONS", last().method);
        assertEquals("application/json", last().header("Content-Type"));
        assertEquals("{\"q\": \"1\"}", last().bodyAsString());
    }

    // ------------------------------------------------------------------------------------------
    // B3: a Content-Encoding that contradicts the applied compression is rejected.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testContradictoryContentEncodingIsRejected() {
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON).header("Content-Encoding", "gzip");
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, settings);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> client.post("{\"a\":1}", String.class));

        assertTrue(e.getMessage().contains("Content-Encoding: gzip"));
        assertTrue(recorded.isEmpty(), "the request must be rejected before it reaches the network");
    }

    @Test
    public void testRejectionDoesNotConsumeAnInFlightSlot() {
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON).header("Content-Encoding", "gzip");
        final HttpClient client = HttpClient.create(baseUrl, 1, 5000, 10000, settings);

        for (int i = 0; i < 3; i++) {
            assertThrows(IllegalArgumentException.class, () -> client.post("{\"a\":1}", String.class));
        }

        // Same client, maxConnection 1: a leaked slot would make this fail with
        // RejectedExecutionException. A GET carries no body, so it skips the encoding check.
        assertNotNull(client.get());
    }

    @Test
    public void testMatchingContentEncodingIsAcceptedAndCompresses() throws IOException {
        final HttpSettings settings = HttpSettings.create().setContentFormat(ContentFormat.JSON_GZIP);

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post("{\"a\":1}", String.class);

        assertEquals("gzip", last().header("Content-Encoding"));
        assertEquals("application/json", last().header("Content-Type"));

        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(last().body))) {
            assertEquals("{\"a\":1}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testUnknownContentEncodingIsPassedThroughUntouched() {
        // "identity" names no compression this client implements, so it must not be rejected and the
        // body must be written verbatim.
        final HttpSettings settings = HttpSettings.create().setContentType("application/json").header("Content-Encoding", "identity");

        HttpClient.create(baseUrl, 16, 5000, 10000, settings).post("{\"a\":1}", String.class);

        assertEquals("identity", last().header("Content-Encoding"));
        assertEquals("{\"a\":1}", last().bodyAsString());
    }

    // ------------------------------------------------------------------------------------------
    // B2: connection flags layer request-over-client, and "unset" is distinguishable from "false".
    // ------------------------------------------------------------------------------------------

    @Test
    public void testOneWayRequestSurvivesAnEmptyPerRequestSettings() {
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().setOneWayRequest(true));

        assertNull(client.execute(HttpMethod.GET, null, null, String.class));
        assertNull(client.execute(HttpMethod.GET, null, HttpSettings.create(), String.class));
    }

    @Test
    public void testOneWayRequestSurvivesTheHttpRequestPath() {
        // HttpRequest always passes a settings object, so this used to reset the client's flag.
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().setOneWayRequest(true));

        assertNull(HttpRequest.create(client).get(String.class));
    }

    @Test
    public void testPerRequestSettingsCanStillOverrideTheClientFlag() {
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().setOneWayRequest(true));

        assertNotNull(client.execute(HttpMethod.GET, null, HttpSettings.create().setOneWayRequest(false), String.class));
    }

    @Test
    public void testClientDoOutputFalseIsHonouredOnBothSettingsPaths() {
        final HttpSettings clientSettings = HttpSettings.create().doOutput(false);
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, clientSettings);

        client.execute(HttpMethod.POST, "PAYLOAD", null, String.class);
        assertEquals(0, last().body.length, "settings == null must not ignore the client-level doOutput");

        client.execute(HttpMethod.POST, "PAYLOAD", HttpSettings.create(), String.class);
        assertEquals(0, last().body.length, "an empty per-request settings must not reset doOutput");
    }

    @Test
    public void testClientDoInputFalseIsHonouredWhenSettingsAreNull() {
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().doInput(false));

        assertThrows(UncheckedIOException.class, () -> client.execute(HttpMethod.GET, null, null, String.class));
    }

    @Test
    public void testHttpSettingsFlagsAreTriState() {
        final HttpSettings unset = HttpSettings.create();

        assertNull(unset.useCachesOrNull());
        assertNull(unset.doInputOrNull());
        assertNull(unset.doOutputOrNull());
        assertNull(unset.isOneWayRequestOrNull());

        // The public accessors still report the documented defaults.
        assertFalse(unset.useCaches());
        assertTrue(unset.doInput());
        assertTrue(unset.doOutput());
        assertFalse(unset.isOneWayRequest());

        final HttpSettings set = HttpSettings.create().useCaches(false).doInput(false).doOutput(false).setOneWayRequest(false);

        assertEquals(Boolean.FALSE, set.useCachesOrNull());
        assertEquals(Boolean.FALSE, set.doInputOrNull());
        assertEquals(Boolean.FALSE, set.doOutputOrNull());
        assertEquals(Boolean.FALSE, set.isOneWayRequestOrNull());
    }

    @Test
    public void testHttpSettingsCopyPreservesUnsetFlags() {
        final HttpSettings copy = HttpSettings.create().setContentType("application/json").copy();

        assertNull(copy.useCachesOrNull());
        assertNull(copy.doInputOrNull());
        assertNull(copy.doOutputOrNull());
        assertNull(copy.isOneWayRequestOrNull());

        final HttpSettings explicitCopy = HttpSettings.create().doOutput(false).copy();
        assertEquals(Boolean.FALSE, explicitCopy.doOutputOrNull());
    }

    @Test
    public void testSettingsPreservesUnsetFlags() {
        final HttpRequest request = HttpRequest.url(baseUrl).settings(HttpSettings.create().header("A", "1"));
        final HttpSettings settings = request.checkSettings();

        assertNull(settings.isOneWayRequestOrNull(), "an unset flag must stay unset so it can fall back to the client");
        assertEquals("1", HttpHeaders.valueOf(settings.headers().get("A")));
    }

    // ------------------------------------------------------------------------------------------
    // B5: payload routing by method - nothing is silently discarded.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testHeadCarriesQueryParameters() {
        HttpClient.create(baseUrl).execute(HttpMethod.HEAD, Map.of("zz", "9"), null, HttpResponse.class);

        assertEquals("HEAD", last().method);
        assertEquals("/?zz=9", last().path);
    }

    @Test
    public void testTraceCarriesQueryParameters() {
        HttpClient.create(baseUrl).execute(HttpMethod.TRACE, Map.of("q", "1"), null, String.class);

        assertEquals("TRACE", last().method);
        assertEquals("/?q=1", last().path);
        assertEquals(0, last().body.length);
    }

    @Test
    public void testGetStillCarriesQueryParameters() {
        HttpClient.create(baseUrl).get(Map.of("a", "1"), String.class);

        assertEquals("/?a=1", last().path);
    }

    @Test
    public void testNonAsciiQueryParametersAreUtf8Encoded() {
        HttpClient.create(baseUrl).get(Map.of("n", "é中"), String.class);

        assertEquals("/?n=%C3%A9%E4%B8%AD", last().path);
    }

    @Test
    public void testOpenConnectionAppliesQueryParametersForEveryMethod() {
        final HttpURLConnection connection = HttpClient.create(baseUrl).openConnection(HttpMethod.POST, Map.of("a", "1"), null, false);

        assertTrue(connection.getURL().toString().endsWith("/?a=1"), connection.getURL().toString());
    }

    @Test
    public void testHttpRequestQueryIsRejectedOnlyForBodyMethods() {
        assertThrows(IllegalStateException.class, () -> HttpRequest.url(baseUrl).query("a=b").post(String.class));
        assertThrows(IllegalStateException.class, () -> HttpRequest.url(baseUrl).query("a=b").put(String.class));
    }

    @Test
    public void testHttpRequestBodyIsRejectedForGetAndHead() {
        assertThrows(IllegalStateException.class, () -> HttpRequest.url(baseUrl).body("x").get(String.class));
        assertThrows(IllegalStateException.class, () -> HttpRequest.url(baseUrl).body("x").head(String.class));
    }

    @Test
    public void testExecuteRejectsANullHttpMethod() {
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(baseUrl).execute(null, null, null, String.class));
    }

    // ------------------------------------------------------------------------------------------
    // B6: non-2xx raises HttpResponseException carrying bounded, structured data.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testHttpErrorCarriesStatusHeadersAndBody() {
        responseCode = 404;
        responseBody = "no such user";

        final HttpResponseException e = assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl).get(String.class));

        assertEquals(404, e.statusCode());
        assertEquals("no such user", e.responseBody());
        assertEquals("application/json", e.header("content-type"));
        assertEquals(List.of("application/json"), e.headers("Content-Type"));
        assertTrue(e.headers("X-Absent").isEmpty());
        assertNull(e.header("X-Absent"));
        // HttpURLConnection.getHeaderFields() files the status line under a null key; the
        // case-insensitive lookup must skip it rather than NPE.
        assertTrue(e.headers().containsKey(null));
        assertNull(e.header(null));
        assertNotNull(e.requestUrl());
        assertTrue(e.getMessage().contains("404"));
    }

    @Test
    public void testHttpResponseExceptionIsAnUncheckedIOException() {
        responseCode = 500;

        // Existing catch blocks must keep working.
        assertThrows(UncheckedIOException.class, () -> HttpClient.create(baseUrl).get(String.class));
    }

    @Test
    public void testDeserializedHttpResponseExceptionPreservesHeaders() throws Exception {
        final HttpResponseException original = new HttpResponseException("https://example.test", 404, "Not Found",
                Map.of("Content-Type", List.of("text/plain")), "missing");
        final ByteArrayOutputStream bytes = new ByteArrayOutputStream();

        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(original);
        }

        final HttpResponseException restored;
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            restored = (HttpResponseException) in.readObject();
        }

        assertEquals(original.requestUrl(), restored.requestUrl());
        assertEquals(original.statusCode(), restored.statusCode());
        assertEquals(original.responseBody(), restored.responseBody());
        assertEquals(original.headers(), restored.headers());
        assertEquals("text/plain", restored.header("Content-Type"));
        assertEquals(List.of("text/plain"), restored.headers("Content-Type"));
        assertNull(restored.header(null));
    }

    @Test
    public void testHeadAndBodylessResponsesDoNotDecodeContentEncoding() {
        server.createContext("/bodyless", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
        });
        final HttpClient client = HttpClient.create(baseUrl + "bodyless");

        for (final int code : new int[] { 200, 404 }) {
            responseCode = code;
            final HttpResponse response = client.head();
            assertEquals(code, response.statusCode());
            assertEquals(0, response.body().length);
            assertEquals("gzip", HttpUtil.getContentEncoding(response.headers()));
        }

        for (final int code : new int[] { 204, 205, 304 }) {
            responseCode = code;
            final HttpResponse response = client.get(HttpResponse.class);
            assertEquals(code, response.statusCode());
            assertEquals(0, response.body().length);
        }

        responseCode = 404;
        final HttpResponseException error = assertThrows(HttpResponseException.class, () -> client.execute(HttpMethod.HEAD, null, String.class));
        assertEquals(404, error.statusCode());
        assertEquals("", error.responseBody());
    }

    @Test
    public void testOkHttpBodylessResponsesDoNotDecodeContentEncoding() {
        server.createContext("/bodyless", exchange -> {
            exchange.getResponseHeaders().set("Content-Encoding", "gzip");
            exchange.sendResponseHeaders(responseCode, -1);
            exchange.close();
        });

        assertEquals("", OkHttpRequest.url(baseUrl + "bodyless").execute(HttpMethod.HEAD, String.class));
        responseCode = 204;
        assertEquals("", OkHttpRequest.url(baseUrl + "bodyless").get(String.class));
        assertEquals(0, OkHttpRequest.url(baseUrl + "bodyless").get(byte[].class).length);

        responseCode = 205;
        assertEquals("", OkHttpRequest.url(baseUrl + "bodyless").get(String.class));
        assertEquals(0, OkHttpRequest.url(baseUrl + "bodyless").header("Accept-Encoding", "gzip").get(byte[].class).length);
    }

    @Test
    public void testResponseBodyRulesCoverStatusAndMethodBoundaries() {
        for (final int code : new int[] { 100, 101, 199, 204, 205, 304 }) {
            assertFalse(HttpUtil.hasResponseBody("GET", code));
        }
        for (final int code : new int[] { 200, 299, 300, 404, 500 }) {
            assertTrue(HttpUtil.hasResponseBody(null, code));
            assertFalse(HttpUtil.hasResponseBody("hEaD", code));
        }
        assertFalse(HttpUtil.hasResponseBody("CONNECT", 200));
        assertFalse(HttpUtil.hasResponseBody("connect", 299));
        assertTrue(HttpUtil.hasResponseBody("CONNECT", 300));
        assertTrue(HttpUtil.hasResponseBody("CONNECT", 407));
    }

    @Test
    public void testLargeErrorBodyIsBoundedInCaptureAndMessage() {
        responseCode = 500;
        responseBody = "E".repeat(200_000);

        final HttpResponseException e = assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl).get(String.class));

        assertTrue(e.responseBody().length() <= HttpUtil.MAX_ERROR_BODY_SIZE, "captured " + e.responseBody().length() + " chars");
        assertTrue(e.getMessage().length() < 2_000, "message was " + e.getMessage().length() + " chars");
        assertTrue(e.getMessage().endsWith("... (truncated)"));
    }

    @Test
    public void testHttpResponseResultTypeStillReturnsTheErrorResponse() {
        responseCode = 503;
        responseBody = "unavailable";

        final HttpResponse response = HttpClient.create(baseUrl).get(HttpResponse.class);

        assertEquals(503, response.statusCode());
        assertFalse(response.isSuccessful());
        assertEquals("unavailable", response.body(String.class));
    }

    // ------------------------------------------------------------------------------------------
    // B7: the in-flight limit rejects with a typed exception.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testExceedingTheInFlightLimitThrowsRejectedExecutionException() throws Exception {
        final CountDownLatch inHandler = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        server.createContext("/slow", exchange -> {
            inHandler.countDown();

            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final byte[] payload = "slow".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });

        final HttpClient client = HttpClient.create(baseUrl + "slow", 1, 5000, 10000);
        final Thread blocked = new Thread(client::get);
        blocked.start();

        try {
            assertTrue(inHandler.await(5, TimeUnit.SECONDS));

            final RejectedExecutionException e = assertThrows(RejectedExecutionException.class, client::get);
            assertTrue(e.getMessage().contains("in-flight"));
        } finally {
            release.countDown();
            blocked.join(5_000);
        }
    }

    @Test
    public void testTheInFlightSlotIsReleasedAfterEachRequest() {
        final HttpClient client = HttpClient.create(baseUrl, 1, 5000, 10000);

        for (int i = 0; i < 5; i++) {
            assertNotNull(client.get());
        }
    }

    @Test
    public void testASharedCounterIsEnforcedAcrossClients() throws Exception {
        final AtomicInteger shared = new AtomicInteger(0);
        final HttpClient first = HttpClient.create(baseUrl, 1, 5000, 10000, null, shared);
        final HttpClient second = HttpClient.create(baseUrl, 1, 5000, 10000, null, shared);

        shared.set(1); // simulate one request already in flight on `first`

        assertThrows(RejectedExecutionException.class, second::get);

        shared.set(0);
        assertNotNull(first.get());
    }

    // ------------------------------------------------------------------------------------------
    // B8 / B9 / D5: construction-time contracts.
    // ------------------------------------------------------------------------------------------

    @Test
    @SuppressWarnings("deprecation")
    public void testUrlThatIsNotValidUriSyntaxIsRejectedAtConstruction() throws Exception {
        final URL url = new URL(baseUrl + "a b");

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> HttpClient.create(url));

        assertTrue(e.getMessage().contains("Invalid URI syntax"));
    }

    @Test
    public void testMalformedUrlStringIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create("http://exa mple.com/x"));
    }

    @Test
    public void testLoadingHttpClientDoesNotMutateJvmNetworkingProperties() throws Exception {
        // A library must not overwrite the hosting application's networking policy. HttpClient is
        // already initialized by the time this test runs, so the class is re-initialized in a
        // throw-away child-first loader to observe its static initializer for real.
        final String[] properties = { "http.keepAlive", "http.maxConnections" };
        final String[] sentinels = { "review-sentinel-keepAlive", "review-sentinel-maxConnections" };
        final String[] previous = new String[properties.length];

        for (int i = 0; i < properties.length; i++) {
            previous[i] = System.getProperty(properties[i]);
            System.setProperty(properties[i], sentinels[i]);
        }

        try (ChildFirstClassLoader loader = new ChildFirstClassLoader(HttpClient.class)) {
            Class.forName(HttpClient.class.getName(), true, loader);

            for (int i = 0; i < properties.length; i++) {
                assertEquals(sentinels[i], System.getProperty(properties[i]), properties[i] + " must not be set by loading HttpClient");
            }
        } finally {
            for (int i = 0; i < properties.length; i++) {
                if (previous[i] == null) {
                    System.clearProperty(properties[i]);
                } else {
                    System.setProperty(properties[i], previous[i]);
                }
            }
        }
    }

    /** Loads exactly one class from its own bytes so that its static initializer runs again. */
    private static final class ChildFirstClassLoader extends ClassLoader implements AutoCloseable {
        private final String targetName;
        private final byte[] targetBytes;

        private ChildFirstClassLoader(final Class<?> target) throws IOException {
            super(target.getClassLoader());
            targetName = target.getName();

            try (InputStream is = target.getClassLoader().getResourceAsStream(targetName.replace('.', '/') + ".class")) {
                assertNotNull(is, "class bytes for " + targetName);
                targetBytes = is.readAllBytes();
            }
        }

        @Override
        protected Class<?> loadClass(final String name, final boolean resolve) throws ClassNotFoundException {
            if (targetName.equals(name)) {
                Class<?> loaded = findLoadedClass(name);

                if (loaded == null) {
                    loaded = defineClass(name, targetBytes, 0, targetBytes.length);
                }

                if (resolve) {
                    resolveClass(loaded);
                }

                return loaded;
            }

            return super.loadClass(name, resolve);
        }

        @Override
        public void close() {
            // Nothing to release; the loader becomes garbage once the test method returns.
        }
    }

    @Test
    public void testZeroSelectsTheDocumentedDefaults() {
        // 0 is a documented sentinel meaning "use the default", not the JDK's "no timeout".
        final HttpClient client = HttpClient.create(baseUrl, 0, 0, 0);

        assertNotNull(client.get());
    }

    // ------------------------------------------------------------------------------------------
    // B10 / D3: argument validation and the async HEAD result type.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testNullDurationTimeoutsAreRejectedAsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(baseUrl).connectTimeout((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(baseUrl).readTimeout((Duration) null));
    }

    @Test
    public void testNegativeDurationTimeoutsAreStillRejected() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(baseUrl).connectTimeout(Duration.ofSeconds(-1)));
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(baseUrl).readTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    public void testAsyncHeadYieldsTheHttpResponse() throws Exception {
        final ContinuableFuture<HttpResponse> future = HttpClient.create(baseUrl).asyncHead();
        final HttpResponse response = future.get(10, TimeUnit.SECONDS);

        assertNotNull(response, "asyncHead must expose the status and headers, which is all HEAD produces");
        assertEquals(200, response.statusCode());
        assertNotNull(response.headers());
    }

    // ------------------------------------------------------------------------------------------
    // D1: close() owns nothing and never invalidates the client.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testCloseIsANoOpAndTheClientStaysUsable() {
        final HttpClient client = HttpClient.create(baseUrl);

        client.close();
        client.close();

        assertNotNull(client.get());
    }

    @Test
    public void testAnHttpRequestFromUrlFactoryCanBeExecutedMoreThanOnce() {
        final HttpRequest request = HttpRequest.url(baseUrl);

        assertNotNull(request.get(String.class));
        assertNotNull(request.get(String.class));
    }

    // ------------------------------------------------------------------------------------------
    // Streaming output paths keep working with the new routing.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testExecuteToOutputStreamStillStreamsTheResponse() throws IOException {
        responseBody = "streamed";

        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        HttpClient.create(baseUrl).execute(HttpMethod.GET, Map.of("a", "1"), null, out);

        assertEquals("streamed", out.toString(StandardCharsets.UTF_8));
        assertEquals("/?a=1", last().path);
    }

    @Test
    public void testExecuteToOutputStreamRaisesHttpResponseExceptionOnError() {
        responseCode = 500;

        final java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, out));
    }

    // ------------------------------------------------------------------------------------------
    // WebUtil: O1-O5.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testGeneratedOkHttpBodyUsesTheCurrentArgumentOrder() {
        final String code = WebUtil.curlToOkHttpRequestCode("curl -X POST http://h/u -H 'Content-Type: application/json' -d '{\"n\":1}'");

        assertTrue(code.contains("RequestBody.create(\"{\\\"n\\\":1}\", MediaType.parse(\"application/json\"));"), code);
        assertFalse(code.contains("RequestBody.create(MediaType"), "the reversed order is deprecated in OkHttp 4");
    }

    @Test
    public void testGeneratedOkHttpBodyDefaultsToCurlsOwnMediaType() {
        final String code = WebUtil.curlToOkHttpRequestCode("curl -X POST http://h/u -d 'a=1'");

        assertTrue(code.contains("MediaType.parse(\"application/x-www-form-urlencoded\")"), code);
        assertFalse(code.contains("create(null,"));
    }

    @Test
    public void testUnsupportedCurlOptionsFailClosed() {
        for (final String option : new String[] { "--data-binary '@f.json'", "--data-urlencode 'a=b'", "-F 'f=@x'", "-u user:pass", "-b 'k=v'", "-T file.txt",
                "--json '{}'", "--form-string 'a=b'" }) {
            final String curl = "curl -X POST http://h/u " + option;

            assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode(curl), curl);
            assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToOkHttpRequestCode(curl), curl);
        }
    }

    @Test
    public void testSupportedCurlOptionsAreStillAccepted() {
        assertNotNull(WebUtil.curlToHttpRequestCode("curl -X POST http://h/u --data-raw 'a=1'"));
        assertNotNull(WebUtil.curlToHttpRequestCode("curl -X POST http://h/u --data 'a=1'"));
        // --data-ascii is a documented alias for -d, so it is reproducible and must be accepted.
        assertTrue(WebUtil.curlToHttpRequestCode("curl -X POST http://h/u --data-ascii 'a=1'").contains("String requestBody = \"a=1\";"));
        assertTrue(WebUtil.curlToHttpRequestCode("curl -X POST http://h/u --data-ascii=a=1").contains("String requestBody = \"a=1\";"));
        assertNotNull(WebUtil.curlToHttpRequestCode("curl -L --compressed http://h/u"));
    }

    @Test
    public void testBuildCurlEmitsMinusIForHead() {
        final String curl = WebUtil.buildCurl(HttpMethod.HEAD, "http://h/u", null, null, null, '\'');

        assertTrue(curl.contains("curl -I 'http://h/u'"), curl);
        assertFalse(curl.contains("-X HEAD"));
    }

    @Test
    public void testBuildCurlRoundTripsThroughTheConverter() {
        final String curl = WebUtil.buildCurl(HttpMethod.HEAD, "http://h/u", null, null, null, '\'');

        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains(".head();"));
    }

    @Test
    public void testBuildCurlRejectsAnUnusableQuoteChar() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.buildCurl(HttpMethod.GET, "http://h/u", null, null, null, '#'));
    }

    @Test
    public void testCurlInterceptorRejectsAnUnusableQuoteCharAtConstruction() {
        // Rejected while wiring the interceptor, not while intercepting: throwing from intercept()
        // would fail the HTTP call rather than just the logging.
        assertThrows(IllegalArgumentException.class, () -> new CurlInterceptor('#', curl -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> WebUtil.createCurlLoggingOkHttpRequest("http://h/u", '#', curl -> {
        }));

        assertNotNull(new CurlInterceptor('\'', curl -> {
        }));
        assertNotNull(new CurlInterceptor('"', curl -> {
        }));
    }

    @Test
    public void testCurlLoggingRequestSharesTheDefaultClientPool() throws Exception {
        final OkHttpRequest request = WebUtil.createCurlLoggingOkHttpRequest(baseUrl, curl -> {
        });
        final java.lang.reflect.Field field = OkHttpRequest.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        final okhttp3.OkHttpClient client = (okhttp3.OkHttpClient) field.get(request);
        final java.lang.reflect.Field defaultField = OkHttpRequest.class.getDeclaredField("DEFAULT_CLIENT");
        defaultField.setAccessible(true);
        final okhttp3.OkHttpClient defaultClient = (okhttp3.OkHttpClient) defaultField.get(null);

        // A standalone OkHttpClient per call leaked a dispatcher and connection pool with no close path.
        assertSame(defaultClient.connectionPool(), client.connectionPool());
        assertSame(defaultClient.dispatcher(), client.dispatcher());
    }

    @Test
    public void testGeneratedCodeHasNoUnusedRequestBodyLocal() {
        final String code = WebUtil.curlToHttpRequestCode("curl -X GET http://h/u -d 'a=1'");

        assertFalse(code.contains("String requestBody ="), code);
        assertTrue(code.contains("Request body omitted for GET"));
    }

    @Test
    public void testHeadIsGeneratedAsHeadCall() {
        assertTrue(WebUtil.curlToHttpRequestCode("curl -I http://h/u").contains(".head();"));
        assertTrue(WebUtil.curlToOkHttpRequestCode("curl -I http://h/u").contains(".head();"));
    }

    // ------------------------------------------------------------------------------------------
    // B13 / D7: OkHttpRequest argument validation and the lossless HttpHeaders bridge.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testFormBodyRejectsANullFieldName() {
        final Map<Object, Object> form = new LinkedHashMap<>();
        form.put(null, "value");

        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.url("http://h/u").formBody(form));
    }

    @Test
    public void testFormBodySkipsNullValues() {
        final Map<Object, Object> form = new LinkedHashMap<>();
        form.put("a", "1");
        form.put("b", null);

        assertNotNull(OkHttpRequest.url("http://h/u").formBody(form));
    }

    // ==========================================================================================
    // Cycle 2
    // ==========================================================================================

    // --- C-046: buildCurl must not emit a header literally named "null" ---

    @Test
    public void testBuildCurlRejectsANullHeaderName() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put(null, "v");

        assertThrows(IllegalArgumentException.class, () -> WebUtil.buildCurl(HttpMethod.GET, "http://h/u", headers, null, null, '\''));
    }

    @Test
    public void testBuildCurlStillAcceptsANullHeaderValue() {
        final Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-Null", null);

        // HttpHeaders.valueOf(null) is the empty string, matching how HttpClient sends header(n, null).
        // curl spells an empty-valued header "Name;": "Name: " would instead remove it.
        assertTrue(WebUtil.buildCurl(HttpMethod.GET, "http://h/u", headers, null, null, '\'').contains("-H 'X-Null;'"));
    }

    // --- C-048: setContentTypeByRequestBodyType follows the library's IAE convention ---

    @Test
    public void testSetContentTypeByRequestBodyTypeRejectsNullHeaders() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.setContentTypeByRequestBodyType("application/json", null));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeIgnoresAnEmptyTypeEvenWithNullHeaders() {
        // The headers argument is only required when there is a type to apply.
        assertDoesNotThrow(() -> WebUtil.setContentTypeByRequestBodyType(null, null));
        assertDoesNotThrow(() -> WebUtil.setContentTypeByRequestBodyType("", null));
    }

    @Test
    public void testSetContentTypeByRequestBodyTypeStillSetsAndPreserves() {
        final HttpHeaders empty = HttpHeaders.create();
        WebUtil.setContentTypeByRequestBodyType("application/json", empty);
        assertEquals("application/json", HttpHeaders.valueOf(empty.get(HttpHeaders.Names.CONTENT_TYPE)));

        final HttpHeaders existing = HttpHeaders.create().setContentType("text/xml");
        WebUtil.setContentTypeByRequestBodyType("application/json", existing);
        assertEquals("text/xml", HttpHeaders.valueOf(existing.get(HttpHeaders.Names.CONTENT_TYPE)));
    }

    // --- C-050: openConnection validates the method before opening a connection ---

    @Test
    public void testOpenConnectionRejectsANullHttpMethodEagerly() {
        final HttpClient client = HttpClient.create(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> client.openConnection(null, null, false));
        assertThrows(IllegalArgumentException.class, () -> client.openConnection(null, Map.of("a", "1"), null, false));
        // No request must have reached the server.
        assertTrue(recorded.isEmpty());
    }

    /** A shell line continuation: space, backslash, newline, then an indented next line. */
    private static String cont(final String indent) {
        return " " + ((char) 92) + ((char) 10) + indent;
    }

    /** A CRLF line continuation. */
    private static String contCrLf(final String indent) {
        return " " + ((char) 92) + ((char) 13) + ((char) 10) + indent;
    }

    @Test
    public void testIndentedContinuationKeepsHeaderAndBody() {
        final String curl = "curl -X POST http://h/u" + cont("  ") + "-H 'Content-Type: application/json'" + cont("  ") + "-d '{\"a\":1}'";

        final String code = WebUtil.curlToHttpRequestCode(curl);

        assertTrue(code.contains(".header(\"Content-Type\", \"application/json\")"), code);
        assertTrue(code.contains("String requestBody = \"{\\\"a\\\":1}\";"), code);
        assertTrue(code.contains(".post();"), code);
    }

    @Test
    public void testIndentedContinuationBetweenDataOptionAndValueKeepsTheBody() {
        final String curl = "curl -X POST http://h/u -d" + cont("  ") + "'{\"a\":1}'";

        // Used to emit String requestBody = ""; - the JSON body was silently dropped.
        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("String requestBody = \"{\\\"a\\\":1}\";"));
        assertTrue(WebUtil.curlToOkHttpRequestCode(curl).contains("RequestBody.create(\"{\\\"a\\\":1}\""));
    }

    @Test
    public void testIndentedContinuationBetweenHeaderOptionAndValueKeepsTheHeader() {
        final String curl = "curl http://h/u -H" + cont("  ") + "'X-A: 1'";

        // Used to produce no .header(..) call at all.
        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains(".header(\"X-A\", \"1\")"));
        assertTrue(WebUtil.curlToOkHttpRequestCode(curl).contains(".header(\"X-A\", \"1\")"));
    }

    @Test
    public void testIndentedContinuationBetweenMethodOptionAndValueKeepsTheMethod() {
        final String curl = "curl -X" + cont("  ") + "POST http://h/u -d 'x=1'";

        // Used to throw IllegalArgumentException: Unsupported HTTP method: (empty).
        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains(".post();"));
    }

    @Test
    public void testCrLfContinuationBehavesLikeLf() {
        final String curl = "curl -X POST http://h/u -d" + contCrLf("  ") + "'{\"a\":1}'";

        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("String requestBody = \"{\\\"a\\\":1}\";"));
    }

    @Test
    public void testTabIndentedContinuationAlsoWorks() {
        final String curl = "curl http://h/u -H" + cont("\t") + "'X-A: 1'";

        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains(".header(\"X-A\", \"1\")"));
    }

    @Test
    public void testNonIndentedContinuationStillWorks() {
        final String curl = "curl -X POST http://h/u" + cont("") + "-H 'Content-Type: application/json'" + cont("") + "-d '{\"a\":1}'";

        final String code = WebUtil.curlToHttpRequestCode(curl);

        assertTrue(code.contains(".header(\"Content-Type\", \"application/json\")"), code);
        assertTrue(code.contains("String requestBody = \"{\\\"a\\\":1}\";"), code);
    }

    @Test
    public void testContinuationInsideAWordStillJoinsIt() {
        // Shell semantics: abc\<newline>def is the single word abcdef. The fix must not break this.
        final String curl = "curl http://h/u/abc" + ((char) 92) + ((char) 10) + "def";

        assertTrue(WebUtil.curlToHttpRequestCode(curl).contains("HttpRequest.url(\"http://h/u/abcdef\")"));
    }

    @Test
    public void testEscapedSpaceInAWordIsStillLiteral() {
        final String curl = "curl http://h/u -H " + ((char) 92) + "'X-A:" + ((char) 92) + " 1" + ((char) 92) + "'";

        // A backslash outside quotes escapes the next character, which must still be appended.
        assertNotNull(WebUtil.curlToHttpRequestCode(curl));
    }

    @Test
    public void testTrailingEscapeIsStillRejected() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl http://h/u " + ((char) 92)));

        assertTrue(e.getMessage().contains("Trailing escape character"), e.getMessage());
    }

    @Test
    public void testContinuationOnlyCommandStillReportsTheMissingUrl() {
        assertThrows(IllegalArgumentException.class, () -> WebUtil.curlToHttpRequestCode("curl" + cont("  ") + "-H 'X-A: 1'"));
    }

    // --- C-049: a null OkHttpClient must be rejected at construction ---

    @Test
    public void testOkHttpRequestCreateRejectsANullClient() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create("http://h/u", null));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create(new URL("http://h/u"), null));
        assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create(okhttp3.HttpUrl.get("http://h/u"), null));
    }

    @Test
    public void testOkHttpRequestNullClientMessageMatchesItsSibling() {
        // HttpRequest.create(null) already failed fast; OkHttpRequest deferred it to a bare NPE from
        // clientBuilder()/execute(), outside its own documented @throws contract.
        final IllegalArgumentException fromOk = assertThrows(IllegalArgumentException.class, () -> OkHttpRequest.create("http://h/u", null));
        final IllegalArgumentException fromHttp = assertThrows(IllegalArgumentException.class, () -> HttpRequest.create(null));

        assertEquals(fromHttp.getMessage(), fromOk.getMessage());
    }

    @Test
    public void testOkHttpRequestFactoriesStillSupplyAClient() {
        assertNotNull(OkHttpRequest.url("http://h/u"));
        assertNotNull(OkHttpRequest.url("http://h/u", 1000L, 2000L));
        assertNotNull(OkHttpRequest.create("http://h/u", new okhttp3.OkHttpClient()));
    }

    // --- C-054: a failed request must not truncate the caller's existing file ---

    @Test
    public void testFailedRequestLeavesAnExistingOutputFileUntouched() throws IOException {
        final File file = File.createTempFile("http-review-c054", ".txt");
        file.deleteOnExit();
        Files.writeString(file.toPath(), "PRE-EXISTING CONTENT");

        responseCode = 500;
        responseBody = "server error";

        assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, file));

        // The file used to be opened (and therefore truncated) before the request was even sent.
        assertEquals("PRE-EXISTING CONTENT", Files.readString(file.toPath()));
    }

    @Test
    public void testFailedRequestDoesNotCreateAMissingOutputFile() throws IOException {
        final File file = File.createTempFile("http-review-c054b", ".txt");
        assertTrue(file.delete());

        responseCode = 404;

        assertThrows(HttpResponseException.class, () -> HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, file));

        assertFalse(file.exists(), "a failed request must not leave an empty file behind");
    }

    @Test
    public void testSuccessfulRequestStillWritesTheOutputFile() throws IOException {
        final File file = File.createTempFile("http-review-c054c", ".txt");
        file.deleteOnExit();
        Files.writeString(file.toPath(), "OLD CONTENT THAT MUST BE REPLACED");

        responseBody = "new body";
        HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, file);

        assertEquals("new body", Files.readString(file.toPath()));
    }

    @Test
    public void testSuccessfulEmptyResponseStillCreatesTheOutputFile() throws IOException {
        final File file = File.createTempFile("http-review-c054d", ".txt");
        assertTrue(file.delete());
        file.deleteOnExit();

        responseBody = "";
        HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, file);

        assertTrue(file.exists(), "a successful empty response must still create the file");
        assertEquals(0, file.length());
    }

    @Test
    public void testExecuteToFileRejectsANullFile() {
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(baseUrl).execute(HttpMethod.GET, null, null, (File) null));
    }

    @Test
    public void testHttpRequestToFileAlsoLeavesAnExistingFileUntouchedOnFailure() throws IOException {
        final File file = File.createTempFile("http-review-c054e", ".txt");
        file.deleteOnExit();
        Files.writeString(file.toPath(), "KEEP ME");

        responseCode = 503;

        assertThrows(HttpResponseException.class, () -> HttpRequest.url(baseUrl).body("payload").execute(HttpMethod.POST, file));

        assertEquals("KEEP ME", Files.readString(file.toPath()));
    }

    // --- C-052: builder null-handling is conventional and does not leak OkHttp's Kotlin intrinsics ---

    @Test
    public void testOkHttpRequestHeaderMethodsRejectANullNameConventionally() {
        final OkHttpRequest request = OkHttpRequest.url(baseUrl);

        for (final org.junit.jupiter.api.function.Executable call : new org.junit.jupiter.api.function.Executable[] { //
                () -> request.header(null, "v"), //
                () -> request.headers(null, "v", "B", "w"), //
                () -> request.headers("A", "v", null, "w"), //
                () -> request.headers("A", "v", "B", "w", null, "x"), //
                () -> request.headers(Collections.singletonMap(null, "v")), //
                () -> request.addHeader(null, "v"), //
                () -> request.removeHeader(null), //
                () -> request.setHeaders((okhttp3.Headers) null) }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertFalse(e.getMessage().contains("okhttp3"), "must not leak OkHttp's parameter name: " + e.getMessage());
        }
    }

    @Test
    public void testOkHttpRequestOtherArgumentsRejectNullConventionally() {
        final OkHttpRequest request = OkHttpRequest.url(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> request.cacheControl(null));
        assertThrows(IllegalArgumentException.class, () -> request.connectTimeout((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> request.readTimeout((Duration) null));
        assertThrows(IllegalArgumentException.class, () -> request.jsonBody((String) null));
        assertThrows(IllegalArgumentException.class, () -> request.xmlBody((String) null));
        assertThrows(IllegalArgumentException.class, () -> request.body((String) null, okhttp3.MediaType.parse("text/plain")));
        assertThrows(IllegalArgumentException.class, () -> request.body((byte[]) null, okhttp3.MediaType.parse("text/plain")));
        assertThrows(IllegalArgumentException.class, () -> request.body((File) null, okhttp3.MediaType.parse("text/plain")));
    }

    @Test
    public void testOkHttpRequestNullTolerantSettersStayTolerant() {
        // These accept null by design and must keep doing so.
        assertNotNull(OkHttpRequest.url(baseUrl).header("A", null));
        assertNotNull(OkHttpRequest.url(baseUrl).headers((Map<String, ?>) null));
        assertNotNull(OkHttpRequest.url(baseUrl).setHeaders((HttpHeaders) null));
        assertNotNull(OkHttpRequest.url(baseUrl).query((String) null));
        assertNotNull(OkHttpRequest.url(baseUrl).query((Map<String, ?>) null));
        assertNotNull(OkHttpRequest.url(baseUrl).formBody((Map<?, ?>) null));
        assertNotNull(OkHttpRequest.url(baseUrl).formBody((Object) null));
        assertNotNull(OkHttpRequest.url(baseUrl).body((okhttp3.RequestBody) null));
        assertNotNull(OkHttpRequest.url(baseUrl).tag(null));
    }

    @Test
    public void testOkHttpRequestByteRangeBodyIsRangeChecked() {
        final OkHttpRequest request = OkHttpRequest.url(baseUrl);

        assertThrows(IndexOutOfBoundsException.class, () -> request.body(new byte[] { 1, 2, 3 }, 0, 4, null));
        assertThrows(IndexOutOfBoundsException.class, () -> request.body(new byte[] { 1, 2, 3 }, -1, 2, null));
        assertNotNull(request.body(new byte[] { 1, 2, 3 }, 1, 2, null));
    }

    @Test
    public void testHttpRequestHeadersMapIsNullTolerantLikeItsSiblings() {
        // setHeaders(null), query(null) and settings(null) are all no-ops; headers(null) used
        // to be the one that threw.
        assertNotNull(HttpRequest.url(baseUrl).headers((Map<String, ?>) null));
        assertNotNull(HttpRequest.url(baseUrl).setHeaders(null));
        assertNotNull(HttpRequest.url(baseUrl).settings(null));
    }

    @Test
    public void testHttpRequestHeaderStillRejectsANullName() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(baseUrl).header(null, "v"));
    }

    @Test
    public void testUnicodeBodySurvivesTheBuildersAsUtf8() {
        // The request body is written in the resolved request charset (UTF-8 by default), so
        // non-ASCII text and astral-plane code points must survive byte-for-byte. Header VALUES are
        // deliberately not asserted here: RFC 7230 restricts them to ASCII, and the library makes no
        // UTF-8 promise for them.
        final String payload = "café-😀-é中";

        HttpRequest.create(HttpClient.create(baseUrl)).header("X-Ascii", "plain").body(payload).post(String.class);

        assertEquals("plain", last().header("X-Ascii"));
        assertEquals("text/plain; charset=UTF-8", last().header("Content-Type"));
        assertEquals(payload, last().bodyAsString());
        assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), last().body);
    }

    @Test
    public void testUnicodeJsonBodySurvivesAsUtf8() {
        final String payload = "{\"k\":\"café-😀\"}";

        HttpRequest.create(HttpClient.create(baseUrl)).jsonBody(payload).post(String.class);

        assertEquals("application/json", last().header("Content-Type"));
        assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), last().body);
    }

    @Test
    public void testIOUtilCloseOnAnAlreadyClosedClientIsHarmless() {
        // Guards the simplified OkHttpRequest lifecycle: nothing is shut down per request.
        assertNotNull(OkHttpRequest.url("http://h/u").readTimeout(1000L));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a03 F-1: a per-request content format is authoritative for the compression too.
    // A non-compressing request format must be able to opt out of a compressing client default.
    // ------------------------------------------------------------------------------------------

    private HttpClient gzipClient() {
        return HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().setContentFormat(ContentFormat.JSON_GZIP));
    }

    private static String gunzip(final byte[] bytes) throws IOException {
        try (InputStream is = new GZIPInputStream(new ByteArrayInputStream(bytes))) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    public void testRequestFormatOptsOutOfClientLevelCompression() {
        // (a) Previously the client's gzip (derived from JSON_GZIP) was pulled into a request that had
        // chosen plain JSON, and the conflict check then threw IllegalArgumentException.
        gzipClient().post(new LinkedHashMap<>(Map.of("k", "v")), HttpSettings.create().setContentFormat(ContentFormat.JSON), String.class);

        assertEquals("{\"k\": \"v\"}", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
        assertEquals("application/json", last().header("Content-Type"));
    }

    @Test
    public void testLiteralClientContentEncodingHeaderStillConflictsWithRequestFormat() {
        // (b) A literal client-level header is copied onto the connection by setHttpProperties, so it
        // would go on the wire over an uncompressed body; the loud rejection must stay.
        final HttpSettings clientSettings = HttpSettings.create().setContentType("application/json").header("Content-Encoding", "gzip");
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, clientSettings);
        final HttpSettings requestSettings = HttpSettings.create().setContentFormat(ContentFormat.JSON);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> client.post("{\"a\":1}", requestSettings, String.class));

        assertTrue(e.getMessage().contains("Content-Encoding: gzip"));
        assertTrue(recorded.isEmpty(), "the request must be rejected before it reaches the network");
    }

    @Test
    public void testRequestCompressingFormatOverridesClientCompression() throws IOException {
        // (c) The request layer picks its own compression: lz4 wins over the client's gzip.
        gzipClient().post("{\"a\":1}", HttpSettings.create().setContentFormat(ContentFormat.JSON_LZ4), String.class);

        assertEquals("lz4", last().header("Content-Encoding"));
        assertEquals("application/json", last().header("Content-Type"));

        try (InputStream is = IOUtil.newLZ4BlockInputStream(new ByteArrayInputStream(last().body))) {
            assertEquals("{\"a\":1}", new String(is.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test
    public void testHeaderOnlyRequestSettingsStillInheritClientCompression() throws IOException {
        // (d) Regression guard: request settings that do not touch the format keep the client's gzip.
        gzipClient().post("{\"a\":1}", HttpSettings.create().header("X-Trace-Id", "42"), String.class);

        assertEquals("gzip", last().header("Content-Encoding"));
        assertEquals("42", last().header("X-Trace-Id"));
        assertEquals("{\"a\":1}", gunzip(last().body));
    }

    @Test
    public void testNullRequestSettingsStillInheritClientCompression() throws IOException {
        gzipClient().post("{\"a\":1}", (HttpSettings) null, String.class);

        assertEquals("gzip", last().header("Content-Encoding"));
        assertEquals("{\"a\":1}", gunzip(last().body));
    }

    @Test
    public void testRequestFormatNoneStillInheritsClientCompression() throws IOException {
        // NONE means "not chosen" and keeps falling back to the client's format and encoding.
        gzipClient().post("{\"a\":1}", HttpSettings.create().setContentFormat(ContentFormat.NONE), String.class);

        assertEquals("gzip", last().header("Content-Encoding"));
        assertEquals("{\"a\":1}", gunzip(last().body));
    }

    @Test
    public void testConflictInsideTheRequestLayerIsStillRejected() {
        // (e) The request's own header contradicts the request's own format.
        final HttpClient client = gzipClient();
        final HttpSettings requestSettings = HttpSettings.create().setContentFormat(ContentFormat.JSON).header("Content-Encoding", "gzip");

        assertThrows(IllegalArgumentException.class, () -> client.post("{\"a\":1}", requestSettings, String.class));
        assertTrue(recorded.isEmpty());
    }

    @Test
    public void testRequestContentTypeHeaderAloneOptsOutOfClientCompression() {
        // (f) The request format is DERIVED from its Content-Type header; that still counts as the
        // request having chosen the format.
        gzipClient().post(new LinkedHashMap<>(Map.of("k", "v")), HttpSettings.create().setContentType("application/json"), String.class);

        assertEquals("{\"k\": \"v\"}", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
        assertEquals("application/json", last().header("Content-Type"));
    }

    @Test
    public void testStringBodyWithRequestFormatOptsOutOfClientCompression() {
        // (g) Raw text bodies hit the same check.
        gzipClient().post("{\"a\":1}", HttpSettings.create().setContentFormat(ContentFormat.JSON), String.class);

        assertEquals("{\"a\":1}", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
    }

    @Test
    public void testByteArrayAndReaderBodiesWithRequestFormatOptOutOfClientCompression() {
        final byte[] bytes = "{\"b\":2}".getBytes(StandardCharsets.UTF_8);

        gzipClient().post(bytes, HttpSettings.create().setContentFormat(ContentFormat.JSON), String.class);
        assertArrayEquals(bytes, last().body);
        assertNull(last().header("Content-Encoding"));

        gzipClient().post(new StringReader("{\"c\":3}"), HttpSettings.create().setContentFormat(ContentFormat.JSON), String.class);
        assertEquals("{\"c\":3}", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
    }

    @Test
    public void testUnicodeBodyWithRequestFormatIsSentAsUtf8() {
        // (h) Nothing in the opt-out path may re-encode the body.
        final String payload = "{\"k\":\"café-😀-中\"}";

        gzipClient().post(payload, HttpSettings.create().setContentFormat(ContentFormat.JSON), String.class);

        assertArrayEquals(payload.getBytes(StandardCharsets.UTF_8), last().body);
        assertEquals(payload, last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
    }

    @Test
    public void testFluentJsonBodyOverGzipClientIsSentPlain() {
        // The fluent path stores only a Content-Type header, so it used to fail the same way.
        HttpRequest.create(gzipClient()).jsonBody(new LinkedHashMap<>(Map.of("k", "v"))).post(String.class);

        assertEquals("{\"k\": \"v\"}", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
        assertEquals("application/json", last().header("Content-Type"));
    }

    @Test
    public void testFormBodyOverGzipClientIsNotMislabelledAsGzip() {
        // Form has no compressed variant, so previously the conflict check passed and the wire
        // carried "Content-Encoding: gzip" over an UNCOMPRESSED url-encoded body.
        HttpRequest.create(gzipClient()).formBody(new LinkedHashMap<>(Map.of("k", "v w"))).post(String.class);

        assertEquals("k=v+w", last().bodyAsString());
        assertNull(last().header("Content-Encoding"));
        assertEquals("application/x-www-form-urlencoded", last().header("Content-Type"));
    }

    @Test
    public void testClientLevelCompressionIsUntouchedForRequestsWithoutSettings() throws IOException {
        // Regression guard for the plain single-layer case.
        gzipClient().post("{\"a\":1}", String.class);

        assertEquals("gzip", last().header("Content-Encoding"));
        assertEquals("{\"a\":1}", gunzip(last().body));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a03 F-2: a null OutputStream/Writer sink is a programming error, not a one-way call.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testExecuteToNullOutputStreamIsRejectedBeforeTheNetwork() {
        final HttpClient client = HttpClient.create(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> client.execute(HttpMethod.GET, null, null, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> client.execute(HttpMethod.POST, "{\"a\":1}", HttpSettings.create(), (OutputStream) null));

        assertTrue(recorded.isEmpty(), "a null sink must not trigger a live request whose response is dropped");
    }

    @Test
    public void testExecuteToNullWriterIsRejectedBeforeTheNetwork() {
        final HttpClient client = HttpClient.create(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> client.execute(HttpMethod.GET, null, null, (Writer) null));
        assertThrows(IllegalArgumentException.class, () -> client.execute(HttpMethod.POST, "{\"a\":1}", HttpSettings.create(), (Writer) null));

        assertTrue(recorded.isEmpty());
    }

    @Test
    public void testHttpRequestExecuteToNullSinkIsRejectedBeforeTheNetwork() {
        // Both HttpRequest routes (query and body) delegate the check to HttpClient.
        final HttpRequest getRequest = HttpRequest.url(baseUrl);
        assertThrows(IllegalArgumentException.class, () -> getRequest.execute(HttpMethod.GET, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> getRequest.execute(HttpMethod.GET, (Writer) null));

        final HttpRequest postRequest = HttpRequest.url(baseUrl).jsonBody("{\"a\":1}");
        assertThrows(IllegalArgumentException.class, () -> postRequest.execute(HttpMethod.POST, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> postRequest.execute(HttpMethod.POST, (Writer) null));

        assertTrue(recorded.isEmpty());
    }

    @Test
    public void testAsyncExecuteToNullSinkDeliversTheRejectionThroughTheFuture() throws Exception {
        final HttpClient client = HttpClient.create(baseUrl);

        final ExecutionException streamFailure = assertThrows(ExecutionException.class,
                () -> client.asyncExecute(HttpMethod.GET, null, null, (OutputStream) null).get(5, TimeUnit.SECONDS));
        assertTrue(streamFailure.getCause() instanceof IllegalArgumentException, String.valueOf(streamFailure.getCause()));

        final ExecutionException writerFailure = assertThrows(ExecutionException.class,
                () -> client.asyncExecute(HttpMethod.GET, null, null, (Writer) null).get(5, TimeUnit.SECONDS));
        assertTrue(writerFailure.getCause() instanceof IllegalArgumentException, String.valueOf(writerFailure.getCause()));

        final ExecutionException requestFailure = assertThrows(ExecutionException.class,
                () -> HttpRequest.url(baseUrl).asyncExecute(HttpMethod.GET, (OutputStream) null).get(5, TimeUnit.SECONDS));
        assertTrue(requestFailure.getCause() instanceof IllegalArgumentException, String.valueOf(requestFailure.getCause()));

        assertTrue(recorded.isEmpty());
    }

    @Test
    public void testExecuteToNonNullSinksStillStreamsTheBody() {
        // Regression guard for the neighbouring behaviour.
        responseBody = "streamed";
        final HttpClient client = HttpClient.create(baseUrl);

        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        client.execute(HttpMethod.GET, null, null, out);
        assertEquals("streamed", out.toString(StandardCharsets.UTF_8));

        final StringWriter writer = new StringWriter();
        client.execute(HttpMethod.GET, null, null, writer);
        assertEquals("streamed", writer.toString());

        assertEquals(2, recorded.size());
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a03 F-3 (pinning): a one-way request returns null even for head()/HttpResponse.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testOneWayClientReturnsNullFromHeadAndHttpResponse() {
        final HttpClient client = HttpClient.create(baseUrl, 16, 5000, 10000, HttpSettings.create().setOneWayRequest(true));

        assertNull(client.head());
        assertNull(client.get(HttpResponse.class));
        assertEquals(2, recorded.size(), "the requests are still sent; only the response is not read");

        // A per-request opt-out restores the HttpResponse.
        assertNotNull(client.head(HttpSettings.create().setOneWayRequest(false)));
        assertNotNull(client.get(HttpSettings.create().setOneWayRequest(false), HttpResponse.class));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a03 F-4 (pinning): unencodable query parameters fail with IAE and release the slot.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testUnencodableQueryParametersThrowIllegalArgumentExceptionAndReleaseTheSlot() {
        final HttpClient client = HttpClient.create(baseUrl, 1, 5000, 10000);

        assertThrows(IllegalArgumentException.class, () -> client.get("a b=c"));

        final Map<String, Object> nullKey = new LinkedHashMap<>();
        nullKey.put(null, "v");
        assertThrows(IllegalArgumentException.class, () -> client.get(nullKey));

        assertTrue(recorded.isEmpty());

        // maxConnection 1: a leaked in-flight slot would surface here as RejectedExecutionException.
        assertNotNull(client.get());
        assertNotNull(client.get(Map.of("q", "1")));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a03 F-5 (pinning): the shared-counter factories reject a null counter.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testNullSharedCounterIsRejectedByEveryFactory() throws Exception {
        final URL url = new URL(baseUrl);

        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(baseUrl, 10, 5000, 10000, null, (AtomicInteger) null));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(url, 10, 5000, 10000, null, (AtomicInteger) null));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(baseUrl, 10, 5000, 10000, null, null, Executors.newSingleThreadExecutor()));
        assertThrows(IllegalArgumentException.class, () -> HttpClient.create(url, 10, 5000, 10000, null, null, Executors.newSingleThreadExecutor()));
    }

    // ------------------------------------------------------------------------------------------
    // 2026-09-06 a04 F-6 (pinning): a url()-built request admits one in-flight execution at a time.
    // ------------------------------------------------------------------------------------------

    @Test
    public void testUrlFactoryRequestRejectsAConcurrentSecondAsyncExecution() throws Exception {
        final CountDownLatch inHandler = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);

        server.createContext("/slow2", exchange -> {
            inHandler.countDown();

            try {
                release.await(5, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            final byte[] payload = "slow".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
        });

        final HttpRequest request = HttpRequest.url(baseUrl + "slow2");
        final ContinuableFuture<String> first = request.asyncGet(String.class);

        try {
            assertTrue(inHandler.await(5, TimeUnit.SECONDS));

            final ExecutionException e = assertThrows(ExecutionException.class, () -> request.asyncGet(String.class).get(5, TimeUnit.SECONDS));
            assertTrue(e.getCause() instanceof RejectedExecutionException, String.valueOf(e.getCause()));
            assertTrue(e.getCause().getMessage().contains("in-flight"));
        } finally {
            release.countDown();
        }

        assertEquals("slow", first.get(5, TimeUnit.SECONDS));

        // Sequential re-execution stays fine once the slot is free again.
        assertEquals("slow", request.get(String.class));
    }
}
