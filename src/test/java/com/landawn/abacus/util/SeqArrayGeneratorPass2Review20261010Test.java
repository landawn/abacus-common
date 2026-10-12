package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class SeqArrayGeneratorPass2Review20261010Test extends TestBase {
    @Test
    void checkedTraversalFailureSkipsGeneratorAndRetainsCloseSuppression() {
        final IOException failure = new IOException("source");
        final IllegalStateException closeFailure = new IllegalStateException("close");
        final AtomicInteger generators = new AtomicInteger();
        final Seq<Integer, IOException> sequence = Seq.<Integer, IOException> of(1, 2).onEach(value -> {
            throw failure;
        }).onClose(() -> {
            throw closeFailure;
        });

        assertSame(failure, assertThrows(IOException.class, () -> sequence.toArray(size -> {
            generators.incrementAndGet();
            return new Integer[size];
        })));
        assertEquals(0, generators.get());
        assertEquals(List.of(closeFailure), Arrays.asList(failure.getSuppressed()));
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 3, 6 })
    void generatedArraySizingAndNullElementsRemainUnchanged(final int requestedSize) {
        final Integer[] supplied = new Integer[requestedSize];
        Arrays.fill(supplied, -1);
        final Integer[] result = Seq.<Integer, RuntimeException> of(1, null, 3).toArray(size -> {
            assertEquals(3, size);
            return supplied;
        });

        final Integer[] expected = requestedSize > 3 ? new Integer[] { 1, null, 3, null, -1, -1 } : new Integer[] { 1, null, 3 };
        assertArrayEquals(expected, result);
        if (requestedSize >= 3) {
            assertSame(supplied, result);
        }
    }
}
