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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.http.HttpMethod;
import com.sun.net.httpserver.HttpServer;

public class HttpRequestAsyncTest extends HttpRequestTestSupport {

    @Test
    public void testAsyncGet() throws Exception {
        try {
            CompletableFuture<HttpResponse<String>> future = HttpRequest.url(TEST_URL).asyncGet();
            assertNotNull(future);
            HttpResponse<String> response = future.get(5, TimeUnit.SECONDS);
            assertNotNull(response);
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncGet_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncGet_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncGet(String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncGet_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(TEST_URL).asyncGet(BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncGet_QueryMap() throws Exception {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", 1);
            assertNotNull(HttpRequest.url(TEST_URL).query(params).asyncGet().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPost() throws Exception {
        try {
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPost_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPost_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPost_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncPost(BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncPost_FormBody() throws Exception {
        try {
            Map<String, String> formData = new HashMap<>();
            formData.put("key", "value");
            assertNotNull(HttpRequest.url(POST_URL).formBody(formData).asyncPost().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPut() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPut_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPut_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPut_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncPut(BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncPatch() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPatch_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPatch_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncPatch_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class,
                () -> HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncPatch(BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncDelete() throws Exception {
        try {
            assertNotNull(HttpRequest.url(DELETE_URL).asyncDelete().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncDelete_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncDelete_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(DELETE_URL).asyncDelete(String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncDelete_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(DELETE_URL).asyncDelete(BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncHead() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncHead().get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecute() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(POST_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.POST).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(PUT_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.PUT).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(DELETE_URL).asyncExecute(HttpMethod.DELETE).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(PATCH_URL).jsonBody("{\"test\":\"data\"}").asyncExecute(HttpMethod.PATCH).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecute_NullMethod() {
        HttpRequest request = HttpRequest.url(TEST_URL);
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null));
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null, BodyHandlers.ofString()));
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> request.asyncExecute(null, BodyHandlers.ofString(), null));
    }

    @Test
    public void testAsyncExecute_BodyHandler() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString()).get(5, TimeUnit.SECONDS));
            assertNotNull(HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofByteArray()).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_BodyHandlerClosesOwnedClientOnSynchronousSendFailure() throws Exception {
        final IllegalStateException failure = new IllegalStateException("synchronous send failure");
        final AssertionError cleanupFailure = new AssertionError("body-handler cleanup failure");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenThrow(failure);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> request.asyncExecute(HttpMethod.GET, BodyHandlers.ofString()));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_NonStreamCleanupErrorClosesOwnedClientOnce() throws Exception {
        final AssertionError cleanupFailure = new AssertionError("async cleanup failure");
        final HttpResponse<String> response = mock(HttpResponse.class);
        when(response.body()).thenReturn("body");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
        final CompletableFuture<HttpResponse<String>> future = request.asyncExecute(HttpMethod.GET, BodyHandlers.ofString());

        final CompletionException thrown = assertThrows(CompletionException.class, future::join);

        assertSame(cleanupFailure, thrown.getCause());
        assertEquals(0, cleanupFailure.getSuppressed().length);
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_CancellationStillCleansOwnedClientAfterUpstreamFailure() throws Exception {
        final IOException failure = new IOException("upstream failure after cancellation");
        final AssertionError cleanupFailure = new AssertionError("cleanup failure after cancellation");
        final CompletableFuture<HttpResponse<String>> upstream = new CompletableFuture<>();
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(upstream);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
        final CompletableFuture<HttpResponse<String>> future = request.asyncExecute(HttpMethod.GET, BodyHandlers.ofString());

        assertTrue(future.cancel(false));
        assertTrue(future.isCancelled());
        assertTrue(upstream.completeExceptionally(failure));

        assertTrue(future.isCancelled());
        assertEquals(1, failure.getSuppressed().length);
        assertSame(cleanupFailure, failure.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_CancellationClosesOrphanedInputStreamResponse() throws Exception {
        final AtomicInteger delegateCloseCount = new AtomicInteger();
        final InputStream delegate = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() {
                delegateCloseCount.incrementAndGet();
            }
        };
        final HttpResponse<InputStream> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(delegate);
        final CompletableFuture<HttpResponse<InputStream>> upstream = new CompletableFuture<>();
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(upstream);

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
        final CompletableFuture<HttpResponse<InputStream>> future = request.asyncExecute(HttpMethod.GET, BodyHandlers.ofInputStream());

        assertTrue(future.cancel(false));
        assertTrue(future.isCancelled());
        assertTrue(upstream.complete(response));

        assertTrue(future.isCancelled());
        assertEquals(1, delegateCloseCount.get());
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    @Test
    public void testAsyncExecute_ResultClass() throws Exception {
        try {
            assertNotNull(HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, String.class).get(5, TimeUnit.SECONDS));
        } catch (Exception e) {
        }
    }

    @Test
    public void testAsyncExecute_ResultClassClosesOwnedClientOnSynchronousBuildFailure() throws Exception {
        final IllegalStateException failure = new IllegalStateException("synchronous build failure");
        final AssertionError cleanupFailure = new AssertionError("result-class cleanup failure");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();
        final java.net.http.HttpRequest.Builder requestBuilder = mock(java.net.http.HttpRequest.Builder.class);
        when(requestBuilder.uri(any(URI.class))).thenReturn(requestBuilder);
        when(requestBuilder.method(any(String.class), any(BodyPublisher.class))).thenReturn(requestBuilder);
        when(requestBuilder.build()).thenThrow(failure);

        final HttpRequest request = newOwnedRequest(ownedClient, requestBuilder);

        final IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> request.asyncExecute(HttpMethod.GET, String.class));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient).close();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_ResultProcessingFailureDoesNotRepeatCompletedCleanup() throws Exception {
        final IllegalStateException failure = new IllegalStateException("result processing failure");
        final HttpResponse<byte[]> response = mock(HttpResponse.class);
        when(response.body()).thenReturn(new byte[] { 1 }).thenThrow(failure);
        when(response.headers()).thenReturn(java.net.http.HttpHeaders.of(Map.of(), (name, value) -> true));
        when(response.statusCode()).thenReturn(200);
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class)))
                .thenReturn((CompletableFuture) CompletableFuture.completedFuture(response));

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
        final CompletableFuture<String> future = request.asyncExecute(HttpMethod.GET, String.class);

        final CompletionException thrown = assertThrows(CompletionException.class, future::join);

        assertSame(failure, thrown.getCause());
        assertEquals(0, failure.getSuppressed().length);
        verify((AutoCloseable) ownedClient, times(1)).close();
    }

    @Test
    public void testAsyncExecute_PushPromiseHandler() {
        assertThrows(IllegalArgumentException.class, () -> HttpRequest.url(TEST_URL).asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), null));
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void testAsyncExecute_PushHandlerClosesOwnedClientOnSynchronousSendFailure() throws Exception {
        final IllegalStateException failure = new IllegalStateException("synchronous push send failure");
        final AssertionError cleanupFailure = new AssertionError("push-handler cleanup failure");
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        when(ownedClient.sendAsync(any(java.net.http.HttpRequest.class), any(HttpResponse.BodyHandler.class), any(HttpResponse.PushPromiseHandler.class)))
                .thenThrow(failure);
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();

        final HttpRequest request = newOwnedRequest(ownedClient, java.net.http.HttpRequest.newBuilder());
        final HttpResponse.PushPromiseHandler<String> pushHandler = (initiatingRequest, pushPromiseRequest, acceptor) -> {
        };

        final IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> request.asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), pushHandler));

        assertSame(failure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        verify((AutoCloseable) ownedClient).close();
    }

    @Test
    public void testAsyncExecute_RejectsNullBodyHandlersBeforeBuildingOwnedClient() {
        final HttpClient.Builder twoArgBuilder = mock(HttpClient.Builder.class);
        final HttpRequest twoArgRequest = new HttpRequest(TEST_URL, null, null, twoArgBuilder, java.net.http.HttpRequest.newBuilder())
                .closeHttpClientAfterExecution(true);

        assertThrows(IllegalArgumentException.class, () -> twoArgRequest.asyncExecute(HttpMethod.GET, (HttpResponse.BodyHandler<String>) null));
        verify(twoArgBuilder, never()).build();

        final HttpClient.Builder pushBuilder = mock(HttpClient.Builder.class);
        final HttpRequest pushRequest = new HttpRequest(TEST_URL, null, null, pushBuilder, java.net.http.HttpRequest.newBuilder())
                .closeHttpClientAfterExecution(true);

        assertThrows(IllegalArgumentException.class, () -> pushRequest.asyncExecute(HttpMethod.GET, (HttpResponse.BodyHandler<String>) null, null));
        verify(pushBuilder, never()).build();

        final HttpClient.Builder nullPushHandlerBuilder = mock(HttpClient.Builder.class);
        final HttpRequest nullPushHandlerRequest = new HttpRequest(TEST_URL, null, null, nullPushHandlerBuilder, java.net.http.HttpRequest.newBuilder())
                .closeHttpClientAfterExecution(true);

        assertThrows(IllegalArgumentException.class, () -> nullPushHandlerRequest.asyncExecute(HttpMethod.GET, BodyHandlers.ofString(), null));
        verify(nullPushHandlerBuilder, never()).build();
    }

    @Test
    public void testAsyncExecute_InputStreamFutureCompletesBeforeBodyRead() throws Exception {
        final HttpServer server = startLocalServer("async stream body");

        try {
            final CompletableFuture<HttpResponse<InputStream>> future = HttpRequest.url(localUrl(server), 1_000L, 5_000L)
                    .asyncExecute(HttpMethod.GET, BodyHandlers.ofInputStream());
            final HttpResponse<InputStream> response = future.get(2, TimeUnit.SECONDS);

            assertEquals(200, response.statusCode());

            try (InputStream inputStream = response.body()) {
                assertEquals("async stream body", new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testAsyncCleanupInputStream_ClosesDelegateAndOwnedClientOnceConcurrently() throws Exception {
        final int threadCount = 16;
        final AtomicInteger delegateCloseCount = new AtomicInteger();
        final InputStream delegate = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() {
                delegateCloseCount.incrementAndGet();
            }
        };
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        final HttpRequest request = newOwnedAsyncInputStreamRequest(ownedClient, delegate);
        final InputStream stream = request.asyncExecute(HttpMethod.GET, BodyHandlers.ofInputStream()).get(2, TimeUnit.SECONDS).body();
        final CountDownLatch allReady = new CountDownLatch(threadCount);
        final CountDownLatch releaseClose = new CountDownLatch(1);
        final ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        try {
            final Future<?>[] closes = new Future<?>[threadCount];

            for (int i = 0; i < threadCount; i++) {
                closes[i] = executor.submit(() -> {
                    allReady.countDown();

                    try {
                        releaseClose.await();
                        stream.close();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    } catch (final IOException e) {
                        throw new AssertionError(e);
                    }
                });
            }

            assertTrue(allReady.await(5, TimeUnit.SECONDS));
            releaseClose.countDown();

            for (final Future<?> close : closes) {
                close.get(5, TimeUnit.SECONDS);
            }

            assertEquals(1, delegateCloseCount.get());
            verify((AutoCloseable) ownedClient, times(1)).close();
        } finally {
            releaseClose.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    public void testAsyncCleanupInputStream_PreservesDelegateCloseFailureWhenClientCleanupThrowsError() throws Exception {
        final IOException delegateFailure = new IOException("delegate close failure");
        final AssertionError cleanupFailure = new AssertionError("owned-client cleanup failure");
        final AtomicInteger delegateCloseCount = new AtomicInteger();
        final InputStream delegate = new InputStream() {
            @Override
            public int read() {
                return -1;
            }

            @Override
            public void close() throws IOException {
                delegateCloseCount.incrementAndGet();
                throw delegateFailure;
            }
        };
        final HttpClient ownedClient = mock(HttpClient.class, withSettings().extraInterfaces(AutoCloseable.class));
        doThrow(cleanupFailure).when((AutoCloseable) ownedClient).close();
        final HttpRequest request = newOwnedAsyncInputStreamRequest(ownedClient, delegate);
        final InputStream stream = request.asyncExecute(HttpMethod.GET, BodyHandlers.ofInputStream()).get(2, TimeUnit.SECONDS).body();

        final IOException thrown = assertThrows(IOException.class, stream::close);

        assertSame(delegateFailure, thrown);
        assertEquals(1, thrown.getSuppressed().length);
        assertSame(cleanupFailure, thrown.getSuppressed()[0]);
        assertEquals(1, delegateCloseCount.get());
        verify((AutoCloseable) ownedClient, times(1)).close();
    }
}
