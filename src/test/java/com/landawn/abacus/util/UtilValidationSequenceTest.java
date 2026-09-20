package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings.DelimiterMatchMode;

class UtilValidationSequenceTest extends TestBase {
    @Test
    void closedJoinerRejectsBeforeCallbacksAndRanges() {
        final Joiner joiner = Joiner.with(",").append("saved");
        joiner.close();
        final AtomicBoolean called = new AtomicBoolean();
        assertThrows(IllegalStateException.class, () -> joiner.appendIf(true, () -> {
            called.set(true);
            return "unexpected";
        }));
        assertThrows(IllegalStateException.class, () -> joiner.appendAll(new int[0], -1, 1));
        assertFalse(called.get());
        assertEquals("saved", joiner.toString());
    }

    @Test
    void noOpInputsStillValidateRequiredCollaborators() {
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.shuffle("\ud83d\ude00", null));
        final Properties<String, Object> properties = new Properties<>();
        assertThrows(IllegalArgumentException.class, () -> properties.getOrDefault("missing", "fallback", null));
        assertEquals("fallback", properties.getOrDefault("missing", "fallback", String.class));
    }

    @Test
    void sheetValidatesKeysAndRangesBeforeMappers() {
        final Sheet<String, String, Integer> sheet = new Sheet<>(List.of("row"), List.of("col"));
        final IllegalArgumentException invalidKey = assertThrows(IllegalArgumentException.class, () -> sheet.updateRow("missing", null));
        assertTrue(invalidKey.getMessage().contains("missing"));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.rows(-1, 0, null));
        assertThrows(IndexOutOfBoundsException.class, () -> sheet.columns(-1, 0, null));
    }

    @Test
    void staleObserverChecksStateBeforeCallbacks() {
        final Observer<Integer> source = Observer.of(List.of(1));
        final Observer<String> current = source.map(Object::toString);
        assertThrows(IllegalStateException.class, () -> source.map(null));
        assertThrows(IllegalStateException.class, () -> source.observe(null));
        assertThrows(IllegalStateException.class, () -> source.observe(null, null, null));
        // Invalid subscription callbacks do not consume the current stage.
        assertThrows(IllegalArgumentException.class, () -> current.observe(null, null, null));
        current.map(String::length);
    }

    @Test
    void joinerSuppliersStayConditionalAndCannotAppendAfterClosingTheReceiver() {
        final Joiner joiner = Joiner.with(",").reuseBuffer().append("saved");
        assertSame(joiner, joiner.appendIf(false, () -> {
            throw new AssertionError("unused supplier");
        }));
        assertThrows(IllegalArgumentException.class, () -> joiner.appendIf(false, null));
        final RuntimeException supplierFailure = new RuntimeException("supplier failure");
        assertSame(supplierFailure, assertThrows(RuntimeException.class, () -> joiner.appendIf(true, () -> {
            throw supplierFailure;
        })));
        assertEquals("saved", joiner.toString());
        assertThrows(IllegalStateException.class, () -> joiner.appendIf(true, () -> {
            joiner.close();
            return "unexpected";
        }));
        assertEquals("saved", joiner.toString());

        final Joiner rendered = Joiner.with(",").reuseBuffer().append("saved");
        assertThrows(IllegalStateException.class, () -> rendered.append(new Object() {
            @Override
            public String toString() {
                rendered.close();
                return "unexpected";
            }
        }));
        assertEquals("saved", rendered.toString());
    }

    @Test
    void validNoOpShuffleDoesNotConsumeRandomnessAndPropertiesStillConvertValues() {
        final Random unused = new Random(0) {
            @Override
            public int nextInt(final int bound) {
                throw new AssertionError("a no-op shuffle must not consume randomness");
            }
        };
        for (String input : new String[] { null, "", "a", "\ud83d\ude00" }) {
            assertSame(input, Strings.shuffle(input, unused));
            assertThrows(IllegalArgumentException.class, () -> Strings.shuffle(input, null));
        }
        final Properties<String, Object> properties = new Properties<>();
        properties.put("number", "42");
        assertEquals(42, properties.getOrDefault("number", 0, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> properties.getOrDefault("number", 0, null));
        properties.put("number", "invalid");
        assertThrows(RuntimeException.class, () -> properties.getOrDefault("number", 0, Integer.class));
    }

    @Test
    void stringDelimitersAndMatchModeValidateBeforeNoOpReturns() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.split(null, "", 0)).getMessage().contains("delimiter"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.splitPreserveAllTokens("", "", 0)).getMessage().contains("delimiter"));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.substringIndicesBetween(null, -1, 0, "[", "]", null, -1));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(null, 0, 0, "", "", null, -1)).getMessage()
                .contains("delimiterMatchMode"));
        assertThrows(IllegalArgumentException.class, () -> Strings.substringIndicesBetween(null, 0, 0, "", "", DelimiterMatchMode.SEQUENTIAL, -1));
        assertEquals(List.of(), Strings.substringIndicesBetween(null, 0, 0, "", "", DelimiterMatchMode.SEQUENTIAL, 0));
        assertEquals(List.of("a", "b"), Strings.substringsBetween("[a][b]", 0, 6, "[", "]", DelimiterMatchMode.SEQUENTIAL, 2));
    }

    @Test
    void sheetValidatesUninitializedAndEmptySortsBeforeReturning() {
        final Sheet<String, String, Integer> sheet = new Sheet<>(List.of("row"), List.of("col"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> sheet.updateColumn("missing", null)).getMessage().contains("missing"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues("missing", null)).getMessage().contains("missing"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues(List.of("missing"), null)).getMessage().contains("missing"));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortRowsByColumnValues("col", null));
        assertThrows(IllegalArgumentException.class, () -> sheet.sortColumnsByRowValues(List.of("row"), null));
        sheet.sortRowsByColumnValues("col", (a, b) -> {
            throw new AssertionError("uninitialized sort");
        });
        sheet.sortColumnsByRowValues(List.of("row"), (a, b) -> {
            throw new AssertionError("uninitialized sort");
        });
        final Sheet<String, String, Integer> empty = new Sheet<>(List.of(), List.of());
        assertThrows(IllegalArgumentException.class, () -> empty.sortRowsByColumnValues(List.of(), null));
        assertThrows(IllegalArgumentException.class, () -> empty.sortColumnsByRowValues(List.of(), null));
        empty.sortRowsByColumnValues(List.of(), (a, b) -> {
            throw new AssertionError("empty sort");
        });
        empty.sortColumnsByRowValues(List.of(), (a, b) -> {
            throw new AssertionError("empty sort");
        });
        sheet.freeze();
        assertThrows(IllegalStateException.class, () -> sheet.updateRow("missing", null));
        assertThrows(IllegalStateException.class, () -> sheet.sortColumnsByRowValues("missing", null));
    }

    @Test
    void invalidObserverCallbacksDoNotStartOrConsumeAnySourceKind() throws InterruptedException {
        final List<Supplier<Observer<Long>>> sources = List.of(() -> Observer.of(List.of(7L)), () -> Observer.of(new LinkedBlockingQueue<>(List.of(7L))),
                () -> Observer.timer(0), () -> Observer.interval(0, 1));
        for (Supplier<Observer<Long>> factory : sources) {
            final Observer<Long> source = factory.get();
            final AtomicInteger values = new AtomicInteger();
            assertTrue(assertThrows(IllegalArgumentException.class, () -> source.observe(null, null, null)).getMessage().contains("action"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> source.observe(v -> values.incrementAndGet(), null, null)).getMessage()
                    .contains("onError"));
            assertTrue(assertThrows(IllegalArgumentException.class, () -> source.observe(v -> values.incrementAndGet(), e -> {
            }, null)).getMessage().contains("onComplete"));
            assertEquals(0, values.get());
            final Observer<Long> current = source.limit(1);
            final CountDownLatch complete = new CountDownLatch(1);
            final AtomicReference<Exception> error = new AtomicReference<>();
            current.observe(v -> values.incrementAndGet(), e -> {
                error.set(e);
                complete.countDown();
            }, complete::countDown);
            assertTrue(complete.await(5, TimeUnit.SECONDS));
            assertNull(error.get());
            assertEquals(1, values.get());
            assertThrows(IllegalStateException.class, () -> current.observe(null, null, null));
            assertThrows(IllegalStateException.class, () -> source.observe(null));
        }
    }

    @Test
    void invalidObserverBufferArgumentsLeaveTheStageUsable() {
        final Observer<Integer> source = Observer.of(List.of(1));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> source.buffer(0, 0, null, 0)).getMessage().contains("timespan"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> source.buffer(1, 0, null, 0)).getMessage().contains("timeskip"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> source.buffer(1, 1, null, 0)).getMessage().contains("Time unit"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> source.buffer(1, 1, TimeUnit.MILLISECONDS, 0)).getMessage().contains("count"));
        source.map(Object::toString);
        assertThrows(IllegalStateException.class, () -> source.buffer(0, 0, null, 0));
    }
}
