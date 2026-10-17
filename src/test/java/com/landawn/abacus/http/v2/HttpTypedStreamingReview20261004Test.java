package com.landawn.abacus.http.v2;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodySubscriber;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.http.ContentFormat;
import com.landawn.abacus.http.HttpMethod;
import com.landawn.abacus.http.HttpUtil;
import com.landawn.abacus.util.N;
import com.sun.net.httpserver.HttpServer;

public class HttpTypedStreamingReview20261004Test extends HttpRequestTestSupport {

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void typedAsyncReadIsOffloadedAndCancellationStillClosesBodyAndClient(final boolean cancel) throws Exception {
        final CountDownLatch reading = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final CountDownLatch closed = new CountDownLatch(1);
        final CountDownLatch clientClosed = new CountDownLatch(1);
        final AtomicInteger closeCount = new AtomicInteger();
        final InputStream input = new ByteArrayInputStream("{\"name\":\"parsed\",\"value\":7}".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public synchronized int read(final byte[] bytes, final int offset, final int length) {
                reading.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("read was not released");
                    }
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return super.read(bytes, offset, length);
            }

            @Override
            public void close() {
                closeCount.incrementAndGet();
                closed.countDown();
            }
        };
        final HttpClient client = mock(HttpClient.class);
        final HttpResponse<InputStream> response = response(input, "application/json", null);
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
        doAnswer(invocation -> {
            clientClosed.countDown();
            return null;
        }).when(client).close();
        final HttpRequest request = HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
        try {
            final CompletableFuture<TestBean> result = request.asyncGet(TestBean.class);
            assertTrue(reading.await(2, TimeUnit.SECONDS));
            assertFalse(result.isDone());
            if (cancel) {
                assertTrue(result.cancel(true));
            }
            release.countDown();
            if (cancel) {
                assertTrue(result.isCancelled());
            } else {
                assertEquals("parsed", result.get(5, TimeUnit.SECONDS).getName());
            }
            assertTrue(closed.await(5, TimeUnit.SECONDS));
            assertTrue(clientClosed.await(5, TimeUnit.SECONDS));
            assertEquals(1, closeCount.get());
            verify(client, times(1)).close();
        } finally {
            release.countDown();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void emptyAndInvalidDecodedStreamsCloseTheRawBodyExactlyOnce(final boolean async) throws Exception {
        for (final int variant : new int[] { 0, 1, 2 }) {
            final byte[] bytes = variant == 0 ? new byte[0] : variant == 1 ? gzip(new byte[0]) : new byte[] { 1, 2, 3 };
            final AtomicInteger bodyCloses = new AtomicInteger();
            final InputStream input = new ByteArrayInputStream(bytes) {
                @Override
                public void close() {
                    bodyCloses.incrementAndGet();
                }
            };
            final HttpResponse<InputStream> response = response(input, "application/xml", variant == 0 ? null : "gzip");
            final HttpClient client = mock(HttpClient.class);
            when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
            when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                    .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
            final HttpRequest request = HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
            if (variant < 2) {
                assertNull(execute(request, HttpMethod.GET, Map.class, async));
            } else {
                assertThrows(RuntimeException.class, () -> execute(request, HttpMethod.GET, Map.class, async));
            }
            assertEquals(1, bodyCloses.get());
            verify(client, times(1)).close();
        }
    }

    @Test
    void malformedStructuredBodyClosesItsStreamAndOwnedClient() throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final AtomicInteger closes = new AtomicInteger();
        final InputStream input = new ByteArrayInputStream("{\"name\": [".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() {
                closes.incrementAndGet();
            }
        };
        @SuppressWarnings("rawtypes")
        final HttpResponse response = response(input, "application/json", null);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
        final HttpRequest request = HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
        assertThrows(RuntimeException.class, () -> request.get(TestBean.class));
        assertEquals(1, closes.get());
        verify(client, times(1)).close();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void emptyXmlUsesStringDefaultsAfterDecodingAndClosesOwnedClient(final boolean async) throws Exception {
        for (final boolean compressed : new boolean[] { false, true }) {
            final byte[] bytes = compressed ? gzip(new byte[0]) : new byte[0];
            for (final Class<?> type : new Class<?>[] { Map.class, List.class, TestBean.class, Integer.class, int.class, Object[].class, String.class,
                    byte[].class }) {
                final AtomicInteger closes = new AtomicInteger();
                final HttpRequest request = responseRequest(bytes, "application/xml", compressed ? "gzip" : null, 200, closes);
                final Object actual = execute(request, HttpMethod.GET, type, async);
                if (type == String.class) {
                    assertEquals("", actual);
                } else if (type == byte[].class) {
                    assertArrayEquals(new byte[0], (byte[]) actual);
                } else {
                    assertEquals(HttpUtil.getParser(ContentFormat.XML).deserialize("", type), actual, type.getName());
                }
                assertEquals(1, closes.get(), "owned client close: " + type);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void headAndNoContentStatusesKeepDirectConversionAndIgnoreCompressionHeaders(final boolean async) throws Exception {
        for (final HttpMethod method : new HttpMethod[] { HttpMethod.GET, HttpMethod.HEAD }) {
            for (final int status : new int[] { 200, 204, 205 }) {
                if (method == HttpMethod.GET && status == 200) {
                    continue;
                }
                for (final String contentType : new String[] { "application/xml", "application/json" }) {
                    for (final Class<?> type : new Class<?>[] { Map.class, LinkedHashMap.class, List.class, Set.class, Integer.class, int.class, int[].class,
                            Object[].class, String.class, byte[].class }) {
                        final AtomicInteger closes = new AtomicInteger();
                        // An absent body with a gzip header must not initialize a gzip decoder.
                        final HttpRequest request = responseRequest(new byte[0], contentType, "gzip", status, closes);
                        final Object actual = execute(request, method, type, async);
                        if (type == byte[].class) {
                            assertArrayEquals(new byte[0], (byte[]) actual);
                        } else if (type == Object[].class) {
                            assertArrayEquals(N.convert("", Object[].class), (Object[]) actual);
                        } else if (type == int[].class) {
                            assertArrayEquals(N.convert("", int[].class), (int[]) actual);
                        } else {
                            final Object expected = N.convert("", type);
                            assertEquals(expected, actual, method + " " + status + " " + contentType + " " + type);
                            if (expected != null) {
                                assertEquals(expected.getClass(), actual.getClass(), "empty conversion must retain its concrete result type");
                            }
                        }
                        assertEquals(1, closes.get());
                    }
                }
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void internalStreamsRetainBestEffortClientCleanup(final boolean async) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        final byte[] json = "{\"name\":\"parsed\",\"value\":7}".getBytes(StandardCharsets.UTF_8);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenAnswer(invocation -> delivered(invocation.getArgument(1), json));
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                .thenAnswer(invocation -> CompletableFuture.completedFuture(delivered(invocation.getArgument(1), json)));
        doThrow(new IllegalStateException("best-effort client close")).when(client).close();
        final HttpRequest request = HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
        final TestBean bean = async ? request.asyncGet(TestBean.class).get(5, TimeUnit.SECONDS) : request.get(TestBean.class);
        assertEquals("parsed", bean.getName());
        assertEquals(7, bean.getValue());
        verify(client, times(1)).close();
    }

    @Test
    void largeJsonAndXmlUseDeclaredCharsetWithAndWithoutCompression() throws Exception {
        final TestBean bean = new TestBean();
        bean.setName("caf\u00e9".repeat(40_000));
        bean.setValue(123);
        for (final boolean xml : new boolean[] { false, true }) {
            final Charset charset = xml ? StandardCharsets.UTF_16LE : StandardCharsets.ISO_8859_1;
            final byte[] plain = (xml ? N.toXml(bean) : N.toJson(bean)).getBytes(charset);
            for (final boolean compressed : new boolean[] { false, true }) {
                final HttpServer server = startResponseServer(compressed ? gzip(plain) : plain,
                        (xml ? "application/xml" : "application/json") + "; charset=" + charset.name(), compressed ? "gzip" : null);
                try {
                    final String url = localUrl(server);
                    final TestBean sync = HttpRequest.url(url).get(TestBean.class);
                    assertEquals(bean.getName(), sync.getName());
                    assertEquals(bean.getValue(), sync.getValue());
                    final TestBean async = HttpRequest.url(url).asyncGet(TestBean.class).get(10, TimeUnit.SECONDS);
                    assertEquals(bean.getName(), async.getName());
                    assertEquals(bean.getValue(), async.getValue());
                } finally {
                    server.stop(0);
                }
            }
        }
    }

    @Test
    void structuredSubscriberDeliversStreamBeforeReceivingThePayload() throws Exception {
        final Method method = HttpRequest.class.getDeclaredMethod("createResponseBodyHandler", Class.class);
        method.setAccessible(true);
        for (final Class<?> type : new Class<?>[] { TestBean.class, Map.class, String.class, byte[].class }) {
            @SuppressWarnings("unchecked")
            final BodyHandler<Object> handler = (BodyHandler<Object>) method.invoke(HttpRequest.url(TEST_URL), type);
            final BodySubscriber<Object> subscriber = handler.apply(info("application/json", null));
            final AtomicInteger cancelled = new AtomicInteger();
            subscriber.onSubscribe(subscription(cancelled));
            final var body = subscriber.getBody().toCompletableFuture();
            if (type == String.class || type == byte[].class) {
                assertFalse(body.isDone());
                subscriber.onComplete();
                assertInstanceOf(byte[].class, body.get(2, TimeUnit.SECONDS));
            } else {
                assertTrue(body.isDone(), "structured responses must not aggregate bytes before delivery");
                assertInstanceOf(InputStream.class, body.get(2, TimeUnit.SECONDS)).close();
                assertEquals(1, cancelled.get());
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void whitespaceXmlAndMalformedCompressionRemainFailuresAndCloseResources(final boolean async) throws Exception {
        final byte[] whitespace = " \t\r\n".getBytes(StandardCharsets.UTF_8);
        for (final boolean compressed : new boolean[] { false, true }) {
            final AtomicInteger closes = new AtomicInteger();
            final HttpRequest request = responseRequest(compressed ? gzip(whitespace) : whitespace, "application/xml", compressed ? "gzip" : null, 200, closes);
            assertThrows(RuntimeException.class, () -> execute(request, HttpMethod.GET, Map.class, async));
            assertEquals(1, closes.get());
        }
        final byte[] corruptTrailer = gzip(new byte[0]);
        corruptTrailer[corruptTrailer.length - 8] ^= 1;
        for (final byte[] bytes : new byte[][] { new byte[0], "not gzip".getBytes(StandardCharsets.UTF_8), corruptTrailer }) {
            final AtomicInteger closes = new AtomicInteger();
            final HttpRequest request = responseRequest(bytes, "application/xml", "gzip", 200, closes);
            assertThrows(RuntimeException.class, () -> execute(request, HttpMethod.GET, Map.class, async));
            assertEquals(1, closes.get());
        }
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static HttpRequest responseRequest(final byte[] bytes, final String contentType, final String encoding, final int status,
            final AtomicInteger closes) throws Exception {
        final HttpClient client = mock(HttpClient.class);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                .thenAnswer(invocation -> delivered(invocation.getArgument(1), bytes, contentType, encoding, status, invocation.getArgument(0)));
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenAnswer(invocation -> CompletableFuture
                .completedFuture(delivered(invocation.getArgument(1), bytes, contentType, encoding, status, invocation.getArgument(0))));
        doAnswer(invocation -> {
            closes.incrementAndGet();
            return null;
        }).when(client).close();
        return HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
    }

    private static Object execute(final HttpRequest request, final HttpMethod method, final Class<?> type, final boolean async) {
        return async ? request.asyncExecute(method, type).join() : request.execute(method, type);
    }

    private static byte[] gzip(final byte[] body) throws IOException {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(body);
        }
        return out.toByteArray();
    }

    private static Flow.Subscription subscription(final AtomicInteger cancelled) {
        return new Flow.Subscription() {
            @Override
            public void request(final long n) {
            }

            @Override
            public void cancel() {
                cancelled.incrementAndGet();
            }
        };
    }

    private static HttpResponse.ResponseInfo info(final String contentType, final String encoding) {
        final HttpResponse.ResponseInfo info = mock(HttpResponse.ResponseInfo.class);
        when(info.statusCode()).thenReturn(200);
        when(info.headers()).thenReturn(headers(contentType, encoding));
        return info;
    }

    private static HttpHeaders headers(final String contentType, final String encoding) {
        return HttpHeaders.of(encoding == null ? Map.of("Content-Type", List.of(contentType))
                : Map.of("Content-Type", List.of(contentType), "Content-Encoding", List.of(encoding)), (name, value) -> true);
    }

    @SuppressWarnings("unchecked")
    private static <T> HttpResponse<T> response(final T body, final String contentType, final String encoding) {
        final HttpResponse<T> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(body);
        when(response.statusCode()).thenReturn(200);
        when(response.headers()).thenReturn(headers(contentType, encoding));
        when(response.uri()).thenReturn(URI.create(TEST_URL));
        return response;
    }

    private static <T> HttpResponse<T> delivered(final BodyHandler<T> handler, final byte[] bytes) throws Exception {
        return delivered(handler, bytes, "application/json", null, 200, null);
    }

    private static <T> HttpResponse<T> delivered(final BodyHandler<T> handler, final byte[] bytes, final String contentType, final String encoding,
            final int status, final java.net.http.HttpRequest request) throws Exception {
        final HttpResponse.ResponseInfo responseInfo = info(contentType, encoding);
        when(responseInfo.statusCode()).thenReturn(status);
        final BodySubscriber<T> subscriber = handler.apply(responseInfo);
        subscriber.onSubscribe(subscription(new AtomicInteger()));
        if (bytes.length > 0) {
            subscriber.onNext(List.of(ByteBuffer.wrap(bytes)));
        }
        subscriber.onComplete();
        final HttpResponse<T> response = response(subscriber.getBody().toCompletableFuture().get(2, TimeUnit.SECONDS), contentType, encoding);
        when(response.statusCode()).thenReturn(status);
        when(response.request()).thenReturn(request);
        return response;
    }

    // ---- deep review 2026-10-16 G127 begin ----

    private static Throwable g127Failure(final org.junit.jupiter.api.function.Executable executable) {
        final Throwable thrown = assertThrows(Throwable.class, executable);
        return thrown instanceof java.util.concurrent.CompletionException || thrown instanceof java.util.concurrent.ExecutionException ? thrown.getCause()
                : thrown;
    }

    // Pins the current (owner-accepted) behaviour: a typed XML result is parsed from the arriving stream (lower memory), whose
    // Reader parse stops at the end of the root element, so a second root element or trailing text is not rejected - the
    // first root is read - for get/asyncGet, with and without Content-Encoding. (The String parser would reject such a body.)
    @Test
    public void testGet_xmlTrailingContentAfterRootIsReadAsFirstRoot() throws Exception {
        for (final String body : new String[] { "<map><a>1</a></map><map><b>2</b></map>", "<map><a>1</a></map>junk" }) {
            final byte[] plain = body.getBytes(StandardCharsets.UTF_8);
            for (final boolean compressed : new boolean[] { false, true }) {
                final HttpServer server = startResponseServer(compressed ? gzip(plain) : plain, "application/xml; charset=UTF-8", compressed ? "gzip" : null);
                try {
                    final String url = localUrl(server);
                    assertEquals(Map.of("a", "1"), HttpRequest.url(url).get(Map.class), body + " compressed=" + compressed);
                    assertEquals(Map.of("a", "1"), HttpRequest.url(url).asyncGet(Map.class).get(10, TimeUnit.SECONDS), body + " compressed=" + compressed);
                } finally {
                    server.stop(0);
                }
            }
        }
    }

    // The same when a client hands the body over as an InputStream (bypassing the result body handler): the first root is
    // read, and the body and the owned client are closed exactly once.
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public void testExecute_xmlTrailingContentInDeliveredStreamIsReadAsFirstRoot(final boolean async) throws Exception {
        final AtomicInteger bodyCloses = new AtomicInteger();
        final InputStream input = new ByteArrayInputStream("<map><a>1</a></map>junk".getBytes(StandardCharsets.UTF_8)) {
            @Override
            public void close() {
                bodyCloses.incrementAndGet();
            }
        };
        final HttpResponse<InputStream> response = response(input, "application/xml", null);
        final HttpClient client = mock(HttpClient.class);
        when(client.send(any(java.net.http.HttpRequest.class), any(BodyHandler.class))).thenReturn(response);
        when(client.sendAsync(any(java.net.http.HttpRequest.class), any(BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
        final HttpRequest request = HttpRequest.create(TEST_URL, client).closeHttpClientAfterExecution(true);
        assertEquals(Map.of("a", "1"), execute(request, HttpMethod.GET, Map.class, async));
        assertEquals(1, bodyCloses.get());
        verify(client, times(1)).close();
    }

    // Neighbours: legal XML epilogs (whitespace, comment, processing instruction) and a declared
    // charset still parse; JSON trailing content is still rejected; String results still return the raw text.
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testExecute_xmlLegalEpilogAndJsonTrailingContentUnchanged(final boolean async) throws Exception {
        for (final String body : new String[] { "<map><a>1</a></map>", " <map><a>1</a></map> \r\n", "<map><a>1</a></map><!-- c -->",
                "<map><a>1</a></map><?pi x?>" }) {
            for (final boolean compressed : new boolean[] { false, true }) {
                for (final Charset charset : new Charset[] { StandardCharsets.UTF_8, StandardCharsets.UTF_16LE }) {
                    final byte[] plain = body.getBytes(charset);
                    final AtomicInteger closes = new AtomicInteger();
                    final HttpRequest request = responseRequest(compressed ? gzip(plain) : plain, "application/xml; charset=" + charset.name(),
                            compressed ? "gzip" : null, 200, closes);
                    assertEquals(Map.of("a", "1"), execute(request, HttpMethod.GET, Map.class, async), body);
                    assertEquals(1, closes.get());
                }
            }
        }

        final AtomicInteger closes = new AtomicInteger();
        final HttpRequest json = responseRequest("{\"a\":1}{\"b\":2}".getBytes(StandardCharsets.UTF_8), "application/json", null, 200, closes);
        assertInstanceOf(com.landawn.abacus.exception.ParsingException.class, g127Failure(() -> execute(json, HttpMethod.GET, Map.class, async)));

        final HttpRequest text = responseRequest("<map><a>1</a></map>junk".getBytes(StandardCharsets.UTF_8), "application/xml", null, 200, closes);
        assertEquals("<map><a>1</a></map>junk", execute(text, HttpMethod.GET, String.class, async));
    }

    // ---- deep review 2026-10-16 G127 end ----

    // ---- deep review 2026-10-16 verify G132 begin ----

    // Every XML content type (text/xml, +xml suffixes, x-xml) and every XML content encoding (lz4, snappy, x-gzip) is parsed
    // from the arriving stream: the first root element is read whether or not content follows it, and the owned client is
    // closed once either way.
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testExecute_xmlTrailingContentReadAsFirstRootForAllXmlContentTypesAndEncodings(final boolean async) throws Exception {
        final String[][] contentTypesAndEncodings = { { "text/xml", "lz4" }, { "application/atom+xml; charset=UTF-8", "snappy" },
                { "application/vnd.test+xml", null }, { "application/x-xml", "x-gzip" } };
        for (final String[] contentTypeAndEncoding : contentTypesAndEncodings) {
            final String contentType = contentTypeAndEncoding[0];
            final String encoding = contentTypeAndEncoding[1];
            final ContentFormat compression = encoding == null ? null
                    : "lz4".equals(encoding) ? ContentFormat.LZ4 : "snappy".equals(encoding) ? ContentFormat.SNAPPY : ContentFormat.GZIP;
            for (final String body : new String[] { "<map><a>1</a></map>junk", "<map><a>1</a></map><map><b>2</b></map>", "<map><a>1</a></map>" }) {
                final byte[] plain = body.getBytes(StandardCharsets.UTF_8);
                byte[] wire = plain;
                if (compression != null) {
                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try (java.io.OutputStream compressed = HttpUtil.wrapOutputStream(out, compression)) {
                        compressed.write(plain);
                    }
                    wire = out.toByteArray();
                }
                final AtomicInteger closes = new AtomicInteger();
                final HttpRequest request = responseRequest(wire, contentType, encoding, 200, closes);
                assertEquals(Map.of("a", "1"), execute(request, HttpMethod.GET, Map.class, async), contentType + " " + encoding + " " + body);
                assertEquals(1, closes.get());
            }
        }
    }

    // The parser choice follows the Content-Type only - an XML-looking body served as application/json is still given to
    // the JSON parser (which rejects it), and its String result is the raw text.
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void testExecute_xmlLookingJsonBodyStaysOnJsonParser(final boolean async) throws Exception {
        final byte[] body = "<map><a>1</a></map>".getBytes(StandardCharsets.UTF_8);
        final AtomicInteger closes = new AtomicInteger();
        final HttpRequest request = responseRequest(body, "application/json", null, 200, closes);
        final Throwable failure = g127Failure(() -> execute(request, HttpMethod.GET, Map.class, async));
        assertInstanceOf(com.landawn.abacus.exception.ParsingException.class, failure);
        assertTrue(String.valueOf(failure.getMessage()).contains("Can't parse"), String.valueOf(failure.getMessage()));
        assertEquals("<map><a>1</a></map>", execute(responseRequest(body, "application/json", null, 200, closes), HttpMethod.GET, String.class, async));
    }

    // ---- deep review 2026-10-16 verify G132 end ----
}
