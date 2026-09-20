package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.RetryExhaustedException;

@org.junit.jupiter.api.Tag("unit")
public class RetryInitialResultTest extends TestBase {
    @Test
    void zeroRetriesStillChecksAndRejectsTheInitialResult() {
        final AtomicInteger operations = new AtomicInteger();
        final AtomicInteger predicates = new AtomicInteger();
        final var retry = Retry.<String> withFixedDelay(0, 1000, (result, error) -> {
            predicates.incrementAndGet();
            assertNull(error);
            return result == null;
        });
        final var failure = assertThrows(RetryExhaustedException.class, () -> retry.call(() -> {
            operations.incrementAndGet();
            return null;
        }));
        assertEquals(1, failure.attempts());
        assertEquals(0, failure.retries());
        assertEquals(1, operations.get());
        assertEquals(1, predicates.get());
        assertNull(failure.getCause());
        assertEquals(0, failure.getSuppressed().length);
    }

    @Test
    void acceptedValuesPredicateFailuresAndOperationFailuresKeepTheirIdentity() throws Exception {
        final var retry = Retry.<String> withFixedDelay(0, 0, (result, error) -> result == null);
        assertEquals("", retry.call(() -> ""));
        assertEquals("\uD83D\uDE00", retry.call(() -> "\uD83D\uDE00"));
        final var predicateFailure = new IllegalStateException("predicate");
        final var throwing = Retry.<String> withFixedDelay(0, 0, (result, error) -> {
            throw predicateFailure;
        });
        assertSame(predicateFailure, assertThrows(IllegalStateException.class, () -> throwing.call(() -> "")));
        final var operationFailure = new IOException("operation");
        assertSame(operationFailure, assertThrows(IOException.class, () -> throwing.call(() -> {
            throw operationFailure;
        })));
        assertThrows(IllegalArgumentException.class, () -> retry.call(null));
        assertNull(Retry.<String> withFixedDelay(0, 0, error -> {
            throw predicateFailure;
        }).call(() -> null));
    }
}
