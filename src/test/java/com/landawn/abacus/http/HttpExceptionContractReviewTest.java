package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

class HttpExceptionContractReviewTest extends TestBase {
    @Test
    void unknownUrlProtocolPreservesItsTranslatedCause() {
        final String url = "abacus-review-unknown://example.invalid";
        assertInstanceOf(MalformedURLException.class, assertThrows(UncheckedIOException.class, () -> HttpClient.create(url)).getCause());
        assertInstanceOf(MalformedURLException.class, assertThrows(UncheckedIOException.class, () -> HttpRequest.url(url)).getCause());
    }

    @Test
    void stringFactoryValidatesEarlierArgumentsBeforeExecutor() {
        final Executor executor = null;
        final IllegalArgumentException urlFailure = assertThrows(IllegalArgumentException.class,
                () -> HttpClient.create("http://bad host", -1, -1, -1, executor));
        assertTrue(urlFailure.getMessage().contains("Invalid URI syntax"));
        final IllegalArgumentException numericFailure = assertThrows(IllegalArgumentException.class,
                () -> HttpClient.create("http://example.invalid", -1, 0, 0, executor));
        assertTrue(numericFailure.getMessage().contains("maxConnection"));
        final IllegalArgumentException counterFailure = assertThrows(IllegalArgumentException.class,
                () -> HttpClient.create("http://example.invalid", 1, 0, 0, null, null, executor));
        assertTrue(counterFailure.getMessage().contains("sharedActiveConnectionCounter"));
        final IllegalArgumentException executorFailure = assertThrows(IllegalArgumentException.class,
                () -> HttpClient.create("http://example.invalid", 1, 0, 0, null, new AtomicInteger(), executor));
        assertTrue(executorFailure.getMessage().contains("executor"));
    }

    @Test
    void urlFactoryValidatesNumericArgumentsBeforeExecutor() throws Exception {
        final URL url = URI.create("http://example.invalid").toURL();
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> HttpClient.create(url, -1, 0, 0, (Executor) null));
        assertTrue(failure.getMessage().contains("maxConnection"));
    }
}
