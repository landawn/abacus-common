package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

import org.junit.jupiter.api.Test;

@org.junit.jupiter.api.Tag("unit")
public class IndexedCallbackTest {
    private record Invocation(Object callback, Runnable invoke) {
    }

    private static List<Invocation> callbacks(final IntConsumer record) {
        final var consumer = Consumers.indexed((i, value) -> record.accept(i));
        final var function = Functions.indexed((i, value) -> {
            record.accept(i);
            return i;
        });
        final var predicate = Predicates.indexed((i, value) -> {
            record.accept(i);
            return true;
        });
        final var biConsumer = BiConsumers.indexed((i, first, second) -> record.accept(i));
        final var biFunction = BiFunctions.indexed((i, first, second) -> {
            record.accept(i);
            return i;
        });
        final var biPredicate = BiPredicates.indexed((i, first, second) -> {
            record.accept(i);
            return true;
        });
        final var delegated = Fn.indexed((i, value) -> {
            record.accept(i);
            return true;
        });
        return List.of(new Invocation(consumer, () -> consumer.accept(null)), new Invocation(function, () -> assertNotNull(function.apply("\uD83D\uDE00"))),
                new Invocation(predicate, () -> assertTrue(predicate.test(""))), new Invocation(biConsumer, () -> biConsumer.accept(null, "")),
                new Invocation(biFunction, () -> assertNotNull(biFunction.apply("", null))),
                new Invocation(biPredicate, () -> assertTrue(biPredicate.test(null, null))),
                new Invocation(delegated, () -> assertTrue(delegated.test("\uD83D\uDE00"))));
    }

    @Test
    void allFactoriesAllowTheLastIndexAndStayExhausted() throws Exception {
        final List<Integer> observed = new ArrayList<>();
        for (final Invocation invocation : callbacks(observed::add)) {
            observed.clear();
            invocation.invoke.run();
            invocation.invoke.run();
            assertEquals(List.of(0, 1), observed);
            // Seed only the boundary state: traversing billions of values would make this test impractical.
            final Field counter = invocation.callback.getClass().getDeclaredField("idx");
            counter.setAccessible(true);
            counter.setLong(invocation.callback, Integer.MAX_VALUE - 1L);
            invocation.invoke.run();
            invocation.invoke.run();
            assertEquals(List.of(0, 1, Integer.MAX_VALUE - 1, Integer.MAX_VALUE), observed);
            assertThrows(ArithmeticException.class, invocation.invoke::run);
            assertThrows(ArithmeticException.class, invocation.invoke::run);
            assertEquals(4, observed.size());
        }
    }

    @Test
    void userFailureStillConsumesItsIndex() {
        final List<Integer> observed = new ArrayList<>();
        final IllegalStateException failure = new IllegalStateException("callback");
        for (final Invocation invocation : callbacks(i -> {
            observed.add(i);
            throw failure;
        })) {
            observed.clear();
            assertSame(failure, assertThrows(IllegalStateException.class, invocation.invoke::run));
            assertSame(failure, assertThrows(IllegalStateException.class, invocation.invoke::run));
            assertEquals(List.of(0, 1), observed);
        }
    }

    @Test
    void nullCallbacksAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> Consumers.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> Functions.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> Predicates.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> BiConsumers.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> BiFunctions.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> BiPredicates.indexed(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.indexed(null));
    }
}
