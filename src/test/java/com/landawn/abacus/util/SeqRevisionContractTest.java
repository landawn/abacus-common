package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.stream.Stream;

/** Executable contracts for the Seq Javadoc corrections in SVN 9489-9503. */
@Tag("unit")
class SeqRevisionContractTest {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void extremaCompareKeysAndRejectOnlyANullWinner(boolean maximum) {
        AtomicInteger calls = new AtomicInteger();
        Function<String, Integer> length = value -> { calls.incrementAndGet(); return value.length(); };
        assertTrue(extreme(Seq.<String, RuntimeException>empty(), length, maximum).isEmpty());
        assertEquals("\u96ea", extreme(Seq.<String, RuntimeException>of("\u96ea"), length, maximum).get());
        assertEquals(0, calls.get(), "empty/singleton sources do not extract keys");
        assertThrows(NullPointerException.class,
                () -> extreme(Seq.<String, RuntimeException>of((String) null), length, maximum));
        assertEquals(0, calls.get(), "singleton null fails Optional construction, not key extraction");
        assertThrows(NullPointerException.class,
                () -> extreme(Seq.<String, RuntimeException>of("x", null), length, maximum));
        assertEquals("first", extreme(Seq.<String, RuntimeException>of("first", "second"), value -> null, maximum).get());
        Function<String, Integer> nullWins = value -> value == null ? (maximum ? 10 : -10) : 0;
        for (List<String> values : List.of(Arrays.asList(null, "x"), Arrays.asList("x", null))) {
            assertThrows(NullPointerException.class,
                    () -> extreme(Seq.<String, RuntimeException>of(values), nullWins, maximum));
            assertEquals("x", extreme(Seq.<String, RuntimeException>of(values).skipNulls(), nullWins, maximum).get());
        }
        Function<String, Integer> nullKey = value -> value.equals("null-key") ? null : 1;
        assertEquals("value",
                extreme(Seq.<String, RuntimeException>of("value", "null-key"), nullKey, maximum).get());
    }

    private static u.Optional<String> extreme(Seq<String, RuntimeException> source, Function<String, Integer> mapper, boolean maximum) {
        return maximum ? source.maxBy(mapper) : source.minBy(mapper);
    }

    @Test
    void findLastRejectsNullOnlyWhenItIsTheFinalMatch() {
        assertEquals("\u96ea", Seq.<String, RuntimeException>of(null, "\u96ea").findLast(value -> true).get());
        assertThrows(NullPointerException.class, () -> Seq.<String, RuntimeException>of("\u96ea", null).findLast(value -> true));
        assertEquals("\u96ea", Seq.<String, RuntimeException>of("\u96ea", null).findLast(value -> value != null).get());
        assertTrue(Seq.<String, RuntimeException>of((String) null).findLast(value -> false).isEmpty());
        assertTrue(Seq.<String, RuntimeException>empty().findLast(value -> true).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 1, 2, 3, 4, 5 })
    void immediateTransformCallbacksCanTraverseOrBufferTheirInput(int route) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (boolean consumeInsideCallback : new boolean[] { false, true }) {
                AtomicInteger callbacks = new AtomicInteger();
                AtomicInteger reads = new AtomicInteger();
                AtomicInteger closes = new AtomicInteger();
                Seq<Integer, RuntimeException> input = Seq.<Integer, RuntimeException>of(1, 2, 3)
                        .map(value -> { reads.incrementAndGet(); return value; }).onClose(closes::incrementAndGet);
                Function<Stream<Integer>, Stream<Integer>> transfer = stream -> {
                    callbacks.incrementAndGet();
                    return consumeInsideCallback ? Stream.of(stream.toList()) : stream;
                };
                Seq<Integer, RuntimeException> result = switch (route) {
                    case 0 -> input.transform(seq -> {
                        callbacks.incrementAndGet();
                        return consumeInsideCallback ? Seq.<Integer, RuntimeException>of(seq.toList()) : seq;
                    });
                    case 1 -> input.transformViaStream(transfer);
                    case 2 -> input.transformViaStream(transfer, false);
                    case 3 -> input.sps(transfer);
                    case 4 -> input.sps(2, transfer);
                    default -> input.sps(2, executor, transfer);
                };
                try (result) {
                    assertEquals(1, callbacks.get());
                    assertEquals(consumeInsideCallback ? 3 : 0, reads.get());
                    List<Integer> values = result.toList();
                    values.sort(Integer::compareTo);
                    assertEquals(List.of(1, 2, 3), values);
                }
                assertEquals(1, closes.get());
            }
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void deferredTransformRunsOnceOnDemandAndNeverOnCloseAlone(boolean empty) {
        for (boolean consume : new boolean[] { false, true }) {
            AtomicInteger calls = new AtomicInteger();
            AtomicInteger closes = new AtomicInteger();
            Seq<Integer, RuntimeException> input = Seq.<Integer, RuntimeException>of(empty ? List.of() : List.of(1, 2))
                    .onClose(closes::incrementAndGet);
            try (Seq<Integer, RuntimeException> result = input.transformViaStream(stream -> {
                calls.incrementAndGet();
                return stream;
            }, true)) {
                assertEquals(0, calls.get());
                if (consume) assertEquals(empty ? List.of() : List.of(1, 2), result.toList());
            }
            assertEquals(consume ? 1 : 0, calls.get());
            assertEquals(1, closes.get());
        }
    }

    @Test
    void mapFactoryLambdaSelectsTheSupplierOverload() {
        // A constructor method reference is ambiguous with the merge-function overload; the documented
        // zero-argument lambda must compile and preserve the chosen concrete map and insertion order.
        record Person(String name, int age) { }
        LinkedHashMap<String, Integer> result = Seq.<Person, RuntimeException>of(new Person("\u96ea", 20), new Person("x", 30))
                .toMap(Person::name, Person::age, () -> new LinkedHashMap<>());
        assertEquals(List.of("\u96ea", "x"), List.copyOf(result.keySet()));
        assertEquals(List.of(20, 30), List.copyOf(result.values()));
        List<java.util.Map.Entry<String, List<Integer>>> groups = Seq.<Person, RuntimeException>of(
                new Person("\u96ea", 20), new Person("x", 30), new Person("\u96ea", 40))
                .groupBy(Person::name, Person::age, () -> new LinkedHashMap<>()).toList();
        assertEquals(List.of(java.util.Map.entry("\u96ea", List.of(20, 40)), java.util.Map.entry("x", List.of(30))), groups);
        assertTrue(Seq.<Person, RuntimeException>empty().groupBy(Person::name, Person::age, () -> new LinkedHashMap<>()).toList().isEmpty());
    }
}
