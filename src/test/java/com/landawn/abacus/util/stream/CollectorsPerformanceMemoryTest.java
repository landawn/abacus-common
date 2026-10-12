package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Stream performance and memory regression contracts. */
@Tag("slow-test")
public class CollectorsPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G092 end ----
    // ---- perf review 2026-10-08 G093 begin ----
    @Nested
    class G093 {
        private final java.util.stream.Collector.Characteristics concurrent = java.util.stream.Collector.Characteristics.CONCURRENT;
        private final java.util.stream.Collector.Characteristics unordered = java.util.stream.Collector.Characteristics.UNORDERED;
        private final java.util.stream.Collector.Characteristics identityFinish = java.util.stream.Collector.Characteristics.IDENTITY_FINISH;

        private final java.util.function.Supplier<List<String>> listSupplier = ArrayList::new;
        private final java.util.function.BiConsumer<List<String>, String> listAccumulator = List::add;
        private final java.util.function.BinaryOperator<List<String>> listCombiner = (a, b) -> {
            a.addAll(b);
            return a;
        };
        private final java.util.function.Function<List<String>, String> listFinisher = String::valueOf;

        private java.util.stream.Collector.Characteristics[] characteristicsWithNullAt(final int length, final int nullIndex) {
            final java.util.stream.Collector.Characteristics[] values = java.util.stream.Collector.Characteristics.values();
            final java.util.stream.Collector.Characteristics[] result = new java.util.stream.Collector.Characteristics[length];

            for (int i = 0; i < length; i++) {
                result[i] = i == nullIndex ? null : values[i % values.length];
            }

            return result;
        }

        private String createMessage(final org.junit.jupiter.api.function.Executable executable) {
            return assertThrows(IllegalArgumentException.class, executable).getMessage();
        }

        // G093-01: checkCharacteristics builds the indexed message only for a null element; pins the exact message, the first null index, all four create overloads and the check order.
        @Test
        public void testCreate_nullCharacteristicMessages() {
            final int[][] lengthAndNullIndex = { { 1, 0 }, { 2, 0 }, { 2, 1 }, { 3, 2 }, { 11, 10 }, { 12, 10 }, { 13, 0 } };

            for (final int[] c : lengthAndNullIndex) {
                final java.util.stream.Collector.Characteristics[] array = characteristicsWithNullAt(c[0], c[1]);
                final List<java.util.stream.Collector.Characteristics> list = Arrays.asList(array);
                final String expected = "'characteristics[" + c[1] + "]' cannot be null";

                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, array)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, list)));
                assertEquals(expected, createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, list)));
            }

            final java.util.stream.Collector.Characteristics[] twoNulls = { unordered, null, concurrent, null };

            assertEquals("'characteristics[1]' cannot be null", createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, twoNulls)));
            assertEquals("'characteristics[1]' cannot be null",
                    createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Arrays.asList(twoNulls))));
            assertEquals("'characteristics' cannot be null",
                    createMessage(() -> Collectors.create(listSupplier, listAccumulator, listCombiner, (java.util.stream.Collector.Characteristics[]) null)));
            assertEquals("'characteristics' cannot be null", createMessage(
                    () -> Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, (java.util.stream.Collector.Characteristics[]) null)));

            // the functional arguments are still checked first
            assertEquals("'supplier' cannot be null",
                    createMessage(() -> Collectors.create((java.util.function.Supplier<List<String>>) null, listAccumulator, listCombiner, twoNulls)));
            assertEquals("'finisher' cannot be null", createMessage(
                    () -> Collectors.create(listSupplier, listAccumulator, listCombiner, (java.util.function.Function<List<String>, String>) null, twoNulls)));
        }

        // G093-01: valid characteristics pass through unchanged (same sets as Collector.of builds) and the collectors still work.
        @Test
        public void testCreate_validCharacteristicsUnchanged() {
            final java.util.stream.Collector.Characteristics[][] arrays = { {}, java.util.stream.Collector.Characteristics.values(),
                    characteristicsWithNullAt(12, -1), { unordered }, { identityFinish, identityFinish }, { concurrent, unordered } };

            for (final java.util.stream.Collector.Characteristics[] array : arrays) {
                final Set<java.util.stream.Collector.Characteristics> withoutFinisher = java.util.stream.Collector
                        .of(listSupplier, listAccumulator, listCombiner, array)
                        .characteristics();
                final Set<java.util.stream.Collector.Characteristics> withFinisher = java.util.stream.Collector
                        .of(listSupplier, listAccumulator, listCombiner, listFinisher, array)
                        .characteristics();

                assertEquals(withoutFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, array).characteristics());
                assertEquals(withFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, array).characteristics());
                assertEquals(withoutFinisher, Collectors.create(listSupplier, listAccumulator, listCombiner, Arrays.asList(array)).characteristics());
                assertEquals(withFinisher,
                        Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Arrays.asList(array)).characteristics());

                assertEquals(Arrays.asList("a", "b", "c"),
                        java.util.stream.Stream.of("a", "b", "c").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
                assertEquals(Arrays.asList("a", "b", "c"),
                        Stream.of("a", "b", "c").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, array)));
            }

            assertEquals("[a, b]", Stream.of("a", "b").collect(Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher)));
            assertEquals("[a, b]", Stream.of("a", "b")
                    .collect(Collectors.create(listSupplier, listAccumulator, listCombiner, listFinisher, Collections.singleton(unordered))));

            // factories in the reviewed range keep their characteristics
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.toList().characteristics());
            assertEquals(java.util.EnumSet.of(unordered, identityFinish), Collectors.toSet().characteristics());
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.toLinkedHashSet().characteristics());
            assertEquals(java.util.EnumSet.noneOf(java.util.stream.Collector.Characteristics.class), Collectors.joining().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.counting().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.countingToInt().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.summingInt((Integer x) -> x).characteristics());
            assertEquals(java.util.EnumSet.of(unordered, identityFinish), Collectors.mapping(String::valueOf, Collectors.toSet()).characteristics());
            assertEquals(java.util.EnumSet.of(identityFinish), Collectors.filtering((Integer x) -> x > 0, Collectors.toList()).characteristics());
            assertEquals(java.util.EnumSet.noneOf(java.util.stream.Collector.Characteristics.class), Collectors.toImmutableList().characteristics());
            assertEquals(java.util.EnumSet.of(unordered), Collectors.collectingOrEmpty(Collectors.toSet()).characteristics());
        }

        // G093-02: counting()/countingToInt() increment the summingLong/summingInt containers directly; pins results, result types, nulls, sizes, sequential/parallel (combiner) paths.
        @Test
        public void testCounting_resultsTypesAndParallel() {
            for (final int n : new int[] { 0, 1, 2, 3, 17, 1000, 5000 }) {
                final List<String> input = new ArrayList<>();

                for (int i = 0; i < n; i++) {
                    input.add(i % 3 == 0 ? null : "e" + i);
                }

                final Long expectedLong = (long) n;
                final Integer expectedInt = n;

                assertEquals(expectedLong, Stream.of(input).collect(Collectors.counting()));
                assertEquals(expectedInt, Stream.of(input).collect(Collectors.countingToInt()));
                assertEquals(expectedLong, input.stream().collect(Collectors.counting()));
                assertEquals(expectedInt, input.stream().collect(Collectors.countingToInt()));
                assertEquals(expectedLong, input.parallelStream().collect(Collectors.counting()));
                assertEquals(expectedInt, input.parallelStream().collect(Collectors.countingToInt()));
                assertEquals(expectedLong, Stream.of(input).parallel(4).collect(Collectors.counting()));
                assertEquals(expectedInt, Stream.of(input).parallel(4).collect(Collectors.countingToInt()));
                assertEquals(expectedLong, Stream.of(input).collect(Collectors.collectingOrDefaultIfEmpty(Collectors.counting(), 0L)));
            }

            assertNotSame(Collectors.counting(), Collectors.counting());
            assertNotSame(Collectors.countingToInt(), Collectors.countingToInt());
        }

        // G093-02: the accumulation containers, combiners, finishers and overflow wrap-around are unchanged.
        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Test
        public void testCounting_containersCombinerAndOverflow() {
            final java.util.stream.Collector longCounting = Collectors.counting();
            final Object first = longCounting.supplier().get();
            final Object second = longCounting.supplier().get();

            assertTrue(first instanceof long[]);
            assertNotSame(first, second);
            assertEquals(1, ((long[]) first).length);

            for (int i = 0; i < 3; i++) {
                longCounting.accumulator().accept(first, i == 1 ? null : "x");
            }

            for (int i = 0; i < 4; i++) {
                longCounting.accumulator().accept(second, i);
            }

            assertSame(first, longCounting.combiner().apply(first, second));
            assertEquals(Long.valueOf(7), longCounting.finisher().apply(first));
            assertEquals(Long.valueOf(4), longCounting.finisher().apply(second));

            ((long[]) first)[0] = Long.MAX_VALUE;
            longCounting.accumulator().accept(first, "overflow");
            assertEquals(Long.valueOf(Long.MIN_VALUE), longCounting.finisher().apply(first));

            final java.util.stream.Collector intCounting = Collectors.countingToInt();
            final Object intFirst = intCounting.supplier().get();
            final Object intSecond = intCounting.supplier().get();

            assertTrue(intFirst instanceof int[]);
            assertNotSame(intFirst, intSecond);
            assertEquals(1, ((int[]) intFirst).length);

            intCounting.accumulator().accept(intFirst, null);
            intCounting.accumulator().accept(intSecond, "a");
            intCounting.accumulator().accept(intSecond, "b");

            assertSame(intFirst, intCounting.combiner().apply(intFirst, intSecond));
            assertEquals(Integer.valueOf(3), intCounting.finisher().apply(intFirst));

            ((int[]) intFirst)[0] = Integer.MAX_VALUE;
            intCounting.accumulator().accept(intFirst, "overflow");
            assertEquals(Integer.valueOf(Integer.MIN_VALUE), intCounting.finisher().apply(intFirst));
        }

        // G093-02: grouped counting through the library paths that use counting()/countingToInt() as their downstream.
        @Test
        public void testCounting_groupedCallers() {
            final List<String> words = Arrays.asList("apple", "avocado", "banana", null, "blueberry", "cherry", "apple", null);
            final java.util.function.Function<String, String> firstLetter = s -> s == null ? "-" : s.substring(0, 1);

            final Map<String, Long> expectedLong = new LinkedHashMap<>();
            final Map<String, Integer> expectedInt = new LinkedHashMap<>();

            for (final String w : words) {
                expectedLong.merge(firstLetter.apply(w), 1L, Long::sum);
                expectedInt.merge(firstLetter.apply(w), 1, Integer::sum);
            }

            assertEquals(expectedLong, Stream.of(words).collect(Collectors.groupingBy(firstLetter, Collectors.counting())));
            assertEquals(expectedInt, Stream.of(words).collect(Collectors.groupingBy(firstLetter, Collectors.countingToInt())));
            assertEquals(expectedLong, Stream.of(words).collect(Collectors.countingBy(firstLetter)));
            assertEquals(expectedInt, Stream.of(words).collect(Collectors.countingToIntBy(firstLetter)));
            assertEquals(expectedLong, Stream.of(words).collect(Collectors.countingBy(firstLetter, TreeMap::new)));
            assertEquals(expectedInt, Stream.of(words).countBy(firstLetter).toMap(Map.Entry::getKey, Map.Entry::getValue));
            assertEquals(new ArrayList<>(new TreeMap<>(expectedInt).entrySet()), new ArrayList<>(Stream.of(words).countBy(firstLetter, TreeMap::new).toList()));
            assertEquals(expectedInt, words.parallelStream().collect(java.util.stream.Collectors.groupingBy(firstLetter, Collectors.countingToInt())));
        }
    }
}
