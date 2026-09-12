package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.RetryExhaustedException;

@org.junit.jupiter.api.Tag("unit")
public class RetryAttemptCountTest {
    @Test
    void maximumPolicyMetadataSurvivesSerializationWithoutExecutingBillionsOfAttempts() throws Exception {
        final long attempts = Integer.MAX_VALUE + 1L;
        final var failure = new RetryExhaustedException("exhausted", attempts, Integer.MAX_VALUE);
        failure.addSuppressed(new IOException("previous attempt"));
        assertEquals(2147483648L, failure.attempts());
        assertEquals(Integer.MAX_VALUE, failure.retries());
        final var bytes = new ByteArrayOutputStream();
        try (final var out = new ObjectOutputStream(bytes)) {
            out.writeObject(failure);
        }
        try (final var in = new ObjectInputStream(new ByteArrayInputStream(bytes.toByteArray()))) {
            final var copy = (RetryExhaustedException) in.readObject();
            assertEquals(attempts, copy.attempts());
            assertEquals(Integer.MAX_VALUE, copy.retries());
            assertEquals("previous attempt", copy.getSuppressed()[0].getMessage());
        }
    }

    @Test
    void ordinaryPoliciesCountInitialAndRetriedCalls() {
        for (final int retries : new int[] { 0, 1, 3 }) {
            final AtomicInteger calls = new AtomicInteger();
            final var failure = assertThrows(RetryExhaustedException.class,
                    () -> Retry.<String> withFixedDelay(retries, 0, (result, error) -> true).call(() -> {
                        calls.incrementAndGet();
                        return "";
                    }));
            assertEquals(retries + 1L, failure.attempts());
            assertEquals(retries + 1, calls.get());
            assertTrue(failure.getMessage().contains((retries + 1L) + " attempts"));
        }
    }
}
