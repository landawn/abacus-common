package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class GeneratedIteratorContractReview20261011Test extends TestBase {
    private static final RuntimeException FAILURE = new IllegalStateException("producer failure");

    private static void consumeBi(final BiIterator<Integer, String> iterator, final int mode, final List<Pair<Integer, String>> values) {
        switch (mode) {
            case 0 -> {
                while (iterator.hasNext()) {
                    values.add(iterator.next());
                }
            }
            case 1 -> {
                while (iterator.hasNext()) {
                    iterator.next((left, right) -> values.add(Pair.of(left, right)));
                }
            }
            case 2 -> iterator.forEachRemaining((left, right) -> values.add(Pair.of(left, right)));
            case 3 -> iterator.foreachRemaining((left, right) -> values.add(Pair.of(left, right)));
            case 4 -> iterator.map((left, right) -> Pair.of(left, right)).forEachRemaining(values::add);
            default -> throw new AssertionError(mode);
        }
    }

    private static void consumeTri(final TriIterator<Integer, String, String> iterator, final int mode, final List<Triple<Integer, String, String>> values) {
        switch (mode) {
            case 0 -> {
                while (iterator.hasNext()) {
                    values.add(iterator.next());
                }
            }
            case 1 -> {
                while (iterator.hasNext()) {
                    iterator.next((left, middle, right) -> values.add(Triple.of(left, middle, right)));
                }
            }
            case 2 -> iterator.forEachRemaining((left, middle, right) -> values.add(Triple.of(left, middle, right)));
            case 3 -> iterator.foreachRemaining((left, middle, right) -> values.add(Triple.of(left, middle, right)));
            case 4 -> iterator.map((left, middle, right) -> Triple.of(left, middle, right)).forEachRemaining(values::add);
            default -> throw new AssertionError(mode);
        }
    }

    @Test
    void biGeneratorsClearBeforeEachInvocationAcrossConsumptionPaths() {
        for (final boolean indexed : new boolean[] { false, true }) {
            for (int mode = 0; mode < 5; mode++) {
                final AtomicInteger produced = new AtomicInteger();
                final Consumer<Pair<Integer, String>> output = value -> {
                    assertNull(value.left());
                    assertNull(value.right());
                    final int ordinal = produced.getAndIncrement();
                    if (ordinal == 0) {
                        value.set(7, "first");
                    } else {
                        value.setLeft(8);
                    }
                };
                final BiIterator<Integer, String> iterator = indexed ? BiIterator.generate(7, 9, (index, value) -> {
                    assertEquals(7 + produced.get(), index);
                    output.accept(value);
                }) : BiIterator.generate(() -> produced.get() < 2, output);
                final List<Pair<Integer, String>> values = new ArrayList<>();

                assertTrue(iterator.hasNext());
                assertEquals(0, produced.get());
                consumeBi(iterator, mode, values);

                // The first snapshot must survive reuse of the generator's output holder.
                assertEquals(List.of(Pair.of(7, "first"), Pair.of(8, null)), values);
                assertNotSame(values.get(0), values.get(1));
                assertEquals(2, produced.get());
                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    void biGeneratorsClearPartialOutputBeforeRecoveryAcrossConsumptionPaths() {
        for (final boolean indexed : new boolean[] { false, true }) {
            for (int mode = 0; mode < 5; mode++) {
                final int consumptionMode = mode;
                final AtomicInteger attempts = new AtomicInteger();
                final AtomicInteger availabilityProbes = new AtomicInteger();
                final List<Integer> indexes = new ArrayList<>();
                final Consumer<Pair<Integer, String>> output = value -> {
                    assertNull(value.left());
                    assertNull(value.right());
                    if (attempts.getAndIncrement() == 0) {
                        value.set(99, "partial");
                        throw FAILURE;
                    }
                    value.setLeft(7);
                };
                final BiIterator<Integer, String> iterator = indexed ? BiIterator.generate(7, 8, (index, value) -> {
                    indexes.add(index);
                    output.accept(value);
                }) : BiIterator.generate(() -> availabilityProbes.getAndIncrement() < 2, output);
                final List<Pair<Integer, String>> values = new ArrayList<>();

                assertSame(FAILURE, assertThrows(RuntimeException.class, () -> consumeBi(iterator, consumptionMode, values)));
                assertTrue(values.isEmpty());
                assertEquals(1, attempts.get());
                if (!indexed) {
                    assertEquals(1, availabilityProbes.get());
                }
                assertTrue(iterator.hasNext());
                consumeBi(iterator, mode, values);

                assertEquals(List.of(Pair.of(7, null)), values);
                assertEquals(2, attempts.get());
                assertFalse(iterator.hasNext());
                if (indexed) {
                    assertEquals(List.of(7, 7), indexes, "A producer failure must leave the index available for retry");
                } else {
                    assertEquals(3, availabilityProbes.get(), "A failed boolean producer consumes its cached availability");
                }
            }
        }
    }

    @Test
    void triGeneratorsClearBeforeEachInvocationAcrossConsumptionPaths() {
        for (final boolean indexed : new boolean[] { false, true }) {
            for (int mode = 0; mode < 5; mode++) {
                final AtomicInteger produced = new AtomicInteger();
                final Consumer<Triple<Integer, String, String>> output = value -> {
                    assertNull(value.left());
                    assertNull(value.middle());
                    assertNull(value.right());
                    final int ordinal = produced.getAndIncrement();
                    if (ordinal == 0) {
                        value.set(7, "middle", "right");
                    } else {
                        value.setLeft(8);
                    }
                };
                final TriIterator<Integer, String, String> iterator = indexed ? TriIterator.generate(7, 9, (index, value) -> {
                    assertEquals(7 + produced.get(), index);
                    output.accept(value);
                }) : TriIterator.generate(() -> produced.get() < 2, output);
                final List<Triple<Integer, String, String>> values = new ArrayList<>();

                assertTrue(iterator.hasNext());
                assertEquals(0, produced.get());
                consumeTri(iterator, mode, values);

                // The first snapshot must survive reuse of the generator's output holder.
                assertEquals(List.of(Triple.of(7, "middle", "right"), Triple.of(8, null, null)), values);
                assertNotSame(values.get(0), values.get(1));
                assertEquals(2, produced.get());
                assertFalse(iterator.hasNext());
            }
        }
    }

    @Test
    void triGeneratorsClearPartialOutputBeforeRecoveryAcrossConsumptionPaths() {
        for (final boolean indexed : new boolean[] { false, true }) {
            for (int mode = 0; mode < 5; mode++) {
                final int consumptionMode = mode;
                final AtomicInteger attempts = new AtomicInteger();
                final AtomicInteger availabilityProbes = new AtomicInteger();
                final List<Integer> indexes = new ArrayList<>();
                final Consumer<Triple<Integer, String, String>> output = value -> {
                    assertNull(value.left());
                    assertNull(value.middle());
                    assertNull(value.right());
                    if (attempts.getAndIncrement() == 0) {
                        value.set(99, "partial middle", "partial right");
                        throw FAILURE;
                    }
                    value.setLeft(7);
                };
                final TriIterator<Integer, String, String> iterator = indexed ? TriIterator.generate(7, 8, (index, value) -> {
                    indexes.add(index);
                    output.accept(value);
                }) : TriIterator.generate(() -> availabilityProbes.getAndIncrement() < 2, output);
                final List<Triple<Integer, String, String>> values = new ArrayList<>();

                assertSame(FAILURE, assertThrows(RuntimeException.class, () -> consumeTri(iterator, consumptionMode, values)));
                assertTrue(values.isEmpty());
                assertEquals(1, attempts.get());
                if (!indexed) {
                    assertEquals(1, availabilityProbes.get());
                }
                assertTrue(iterator.hasNext());
                consumeTri(iterator, mode, values);

                assertEquals(List.of(Triple.of(7, null, null)), values);
                assertEquals(2, attempts.get());
                assertFalse(iterator.hasNext());
                if (indexed) {
                    assertEquals(List.of(7, 7), indexes, "A producer failure must leave the index available for retry");
                } else {
                    assertEquals(3, availabilityProbes.get(), "A failed boolean producer consumes its cached availability");
                }
            }
        }
    }
}
