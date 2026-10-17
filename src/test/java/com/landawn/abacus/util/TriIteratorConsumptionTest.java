package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class TriIteratorConsumptionTest extends TestBase {
    record Source(String name, Supplier<TriIterator<Integer, Integer, Integer>> factory, List<String> expected) {
        @Override
        public String toString() {
            return name;
        }
    }

    private static TriIterator<Integer, Integer, Integer> padded() {
        return TriIterator.zip(List.of(1, 2, 3).iterator(), List.of(10).iterator(), List.of(20, 21).iterator(), -1, -2, -3);
    }

    static List<Source> sources() {
        return List.of(new Source("paddedZip", TriIteratorConsumptionTest::padded, List.of("1/10/20", "2/-2/21", "3/-2/-3")),
                new Source("shortestZip", () -> TriIterator.zip(List.of(1, 2, 3).iterator(), List.of(10, 11).iterator(), List.of(20, 21).iterator()),
                        List.of("1/10/20", "2/11/21")),
                new Source("generated", () -> TriIterator.generate(1, 4, (i, out) -> out.set(i, i + 10, i + 20)), List.of("1/11/21", "2/12/22", "3/13/23")),
                new Source("unzip", () -> TriIterator.unzip(List.of(1, 2, 3), (i, out) -> out.set(i, i + 10, i + 20)),
                        List.of("1/11/21", "2/12/22", "3/13/23")),
                new Source("skip", () -> padded().skip(1), List.of("2/-2/21", "3/-2/-3")),
                new Source("limit", () -> padded().limit(2), List.of("1/10/20", "2/-2/21")),
                new Source("filter", () -> padded().filter((a, b, c) -> a != 2), List.of("1/10/20", "3/-2/-3")),
                new Source("empty", TriIterator::empty, List.of()));
    }

    @ParameterizedTest
    @MethodSource("sources")
    void eachConsumptionFormProducesTheSameTriples(Source source) throws Exception {
        TriIterator<Integer, Integer, Integer> callbacks = source.factory.get();
        List<String> actual = new ArrayList<>();
        for (int i = 0; i < source.expected.size(); i++)
            callbacks.next((a, b, c) -> actual.add(text(a, b, c)));
        assertEquals(source.expected, actual);
        assertThrows(NoSuchElementException.class, () -> callbacks.next((a, b, c) -> fail("exhausted callback")));

        actual.clear();
        TriIterator<Integer, Integer, Integer> ordinary = source.factory.get();
        ordinary.forEachRemaining((a, b, c) -> actual.add(text(a, b, c)));
        assertEquals(source.expected, actual);
        assertFalse(ordinary.hasNext());
        assertThrows(NoSuchElementException.class, ordinary::next);

        actual.clear();
        TriIterator<Integer, Integer, Integer> checked = source.factory.get();
        checked.foreachRemaining((a, b, c) -> actual.add(text(a, b, c)));
        assertEquals(source.expected, actual);
        assertFalse(checked.hasNext());

        actual.clear();
        ObjIterator<String> mapped = source.factory.get().map(TriIteratorConsumptionTest::text);
        for (String expected : source.expected)
            assertEquals(expected, mapped.next());
        assertThrows(NoSuchElementException.class, mapped::next);
        assertFalse(mapped.hasNext());
    }

    @ParameterizedTest
    @MethodSource("sources")
    void failedConsumerDoesNotReplayTheConsumedTriple(Source source) throws Exception {
        TriIterator<Integer, Integer, Integer> iterator = source.factory.get();
        if (source.expected.isEmpty()) {
            iterator.foreachRemaining((a, b, c) -> fail("empty source"));
            return;
        }
        IOException failure = new IOException("consumer failed");
        assertSame(failure, assertThrows(IOException.class, () -> iterator.foreachRemaining((a, b, c) -> {
            throw failure;
        })));
        List<String> remaining = new ArrayList<>();
        iterator.forEachRemaining((a, b, c) -> remaining.add(text(a, b, c)));
        assertEquals(source.expected.subList(1, source.expected.size()), remaining);
    }

    private static String text(Integer a, Integer b, Integer c) {
        return a + "/" + b + "/" + c;
    }
}
