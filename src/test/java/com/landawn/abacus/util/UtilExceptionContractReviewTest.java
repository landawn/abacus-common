package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.junit.jupiter.api.Test;

class UtilExceptionContractReviewTest {
    @Test
    void futureSubmissionRejectionPropagatesImmediately() {
        final var rejection = new RejectedExecutionException("executor stopped");
        final Executor rejecting = command -> { throw rejection; };
        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> ContinuableFuture.run(() -> { }, rejecting)));
        assertSame(rejection, assertThrows(RejectedExecutionException.class, () -> ContinuableFuture.call(() -> 1, rejecting)));
        assertSame(rejection, assertThrows(RejectedExecutionException.class,
                () -> ContinuableFuture.completed(1).thenUse(rejecting).thenCallAsync(value -> value + 1)));
    }

    @Test
    void prefixBuilderRejectsNullValueAsAnArgumentBeforeAddingNodes() {
        final var builder = PrefixSearchTable.<String, Integer>builder();
        assertThrows(IllegalArgumentException.class, () -> builder.add(List.of("key"), null));
        assertTrue(builder.build().get(List.of("key")).isEmpty());
        assertThrows(NullPointerException.class, () -> builder.add(Arrays.asList("key", null), 1));
        assertEquals(1, builder.add(List.of("key"), 1).build().get(List.of("key")).get());
    }

    @Test
    void prefixBulkAddPreservesNullAndEmptyNoOpsButRejectsNullValues() {
        final var builder = PrefixSearchTable.<String, Integer>builder();
        assertSame(builder, builder.addAll(null));
        assertSame(builder, builder.addAll(Map.of()));
        final Map<List<String>, Integer> entries = new HashMap<>();
        entries.put(List.of("key"), null);
        assertThrows(IllegalArgumentException.class, () -> builder.addAll(entries));
    }

    @Test
    @SuppressWarnings("deprecation")
    void tripleArrayConversionRetainsJdkNullAndArrayStoreExceptions() {
        assertThrows(NullPointerException.class, () -> TriIterator.zip(new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }).toArray(null));
        assertThrows(ArrayStoreException.class, () -> TriIterator.zip(new Integer[] { 1 }, new Integer[] { 2 }, new Integer[] { 3 }).toArray(new String[0]));
    }

    @Test
    void appendableWriterChecksClosedStateBeforeArrayValidation() throws IOException {
        final var writer = new AppendableWriter(new StringBuilder());
        assertThrows(NullPointerException.class, () -> writer.write((char[]) null));
        writer.close();
        assertThrows(IOException.class, () -> writer.write((char[]) null));
        assertThrows(IOException.class, () -> writer.write(new char[1], -1, 1));
    }

    @Test
    void bufferedReaderChecksClosedStateThenBoundsBeforeNullBuffer() throws IOException {
        final var reader = new BufferedReader("text");
        assertThrows(IndexOutOfBoundsException.class, () -> reader.read(null, -1, 1));
        assertThrows(NullPointerException.class, () -> reader.read(null, 0, 1));
        reader.close();
        assertThrows(IOException.class, () -> reader.read(null, -1, 1));
    }
}
