package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class BiIterator101Test extends TestBase {

    @Nested
    @DisplayName("Additional Empty Iterator Tests")
    public class EmptyIteratorTests {

        @Test
        @DisplayName("Test empty iterator map() returns empty ObjIterator")
        public void testEmptyMap() {
            BiIterator<String, Integer> empty = BiIterator.empty();
            ObjIterator<String> mapped = empty.map((a, b) -> a + b);

            assertFalse(mapped.hasNext());
            assertThrows(NoSuchElementException.class, () -> mapped.next());
        }

        @Test
        @DisplayName("Test empty iterator operations chaining")
        public void testEmptyChaining() {
            BiIterator<String, Integer> result = BiIterator.<String, Integer> empty().skip(10).limit(5).filter((a, b) -> true);

            assertFalse(result.hasNext());
            assertEquals(0, result.toList().size());
        }

        @Test
        @DisplayName("Test empty iterator foreachRemaining with exception")
        public void testEmptyForeachRemainingException() {
            BiIterator<String, Integer> empty = BiIterator.empty();

            assertDoesNotThrow(() -> {
                empty.foreachRemaining((Throwables.BiConsumer<String, Integer, Exception>) (a, b) -> {
                    throw new Exception("Should not be called");
                });
            });
        }
    }

    @Nested
    @DisplayName("Deprecated Method Tests")
    public class DeprecatedMethodTests {

        @Test
        @DisplayName("Test deprecated forEachRemaining(Consumer<Pair>)")
        @SuppressWarnings("deprecation")
        public void testDeprecatedForEachRemaining() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] arr2 = { 1, 2, 3 };

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

            List<Pair<String, Integer>> pairs = new ArrayList<>();
            iter.forEachRemaining((Consumer<Pair<String, Integer>>) pairs::add);

            assertEquals(3, pairs.size());
            assertEquals("a", pairs.get(0).left());
            assertEquals(1, pairs.get(0).right());
        }
    }

    @Nested
    @DisplayName("Complex Generate Scenarios")
    public class GenerateComplexTests {

        @Test
        @DisplayName("Test generate with stateful output consumer")
        public void testGenerateStateful() {
            MutableInt counter = MutableInt.of(10);
            BiIterator<Integer, Integer> fib = BiIterator.generate(() -> counter.getAndDecrement() > 0, pair -> {
                if (pair.left() == null || pair.right() == null) {
                    pair.set(0, 1);
                } else {
                    int next = pair.left() + pair.right();
                    pair.set(pair.right(), next);
                }
            });

            List<Pair<Integer, Integer>> fibPairs = fib.toList();
            assertEquals(10, fibPairs.size());
            assertEquals(Pair.of(0, 1), fibPairs.get(0));
            assertEquals(Pair.of(1, 1), fibPairs.get(1));
            assertEquals(Pair.of(1, 2), fibPairs.get(2));
            assertEquals(Pair.of(2, 3), fibPairs.get(3));
        }

        @Test
        @DisplayName("Test generate with exception in output consumer")
        public void testGenerateWithException() {
            BiIterator<String, String> iter = BiIterator.generate(() -> true, pair -> {
                throw new RuntimeException("Test exception");
            });

            assertThrows(RuntimeException.class, () -> iter.next());
        }

        @Test
        @DisplayName("Test generate with index at boundaries")
        public void testGenerateIndexBoundaries() {
            BiIterator<Long, Long> iter = BiIterator.generate(Integer.MAX_VALUE - 5, Integer.MAX_VALUE - 1,
                    (index, pair) -> pair.set((long) index, (long) index));

            List<Pair<Long, Long>> results = iter.toList();
            assertEquals(4, results.size());
            assertEquals(Integer.MAX_VALUE - 5, results.get(0).left());
        }
    }

    @Nested
    @DisplayName("Zip Edge Cases")
    public class ZipEdgeCaseTests {

        @Test
        @DisplayName("Test zip with empty arrays")
        public void testZipEmptyArrays() {
            String[] empty1 = new String[0];
            Integer[] empty2 = new Integer[0];

            BiIterator<String, Integer> iter = BiIterator.zip(empty1, empty2);
            assertFalse(iter.hasNext());
        }

        @Test
        @DisplayName("Test zip with one empty array and defaults")
        public void testZipOneEmptyWithDefaults() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] empty = new Integer[0];

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, empty, "default", 99);

            List<Pair<String, Integer>> results = iter.toList();
            assertEquals(3, results.size());
            assertTrue(results.stream().allMatch(p -> p.right().equals(99)));
        }

        @Test
        @DisplayName("Test zip with null elements in arrays")
        public void testZipWithNullElements() {
            String[] arr1 = { "a", null, "c" };
            Integer[] arr2 = { 1, 2, null };

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

            assertEquals(Pair.of("a", 1), iter.next());
            assertEquals(Pair.of(null, 2), iter.next());
            assertEquals(Pair.of("c", null), iter.next());
        }
    }

    @Nested
    @DisplayName("Filter Advanced Tests")
    public class FilterAdvancedTests {

        @Test
        @DisplayName("Test filter with no matches")
        public void testFilterNoMatches() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] arr2 = { 1, 2, 3 };

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2).filter((s, i) -> i > 10);

            assertFalse(iter.hasNext());
            assertEquals(0, iter.toList().size());
        }

        @Test
        @DisplayName("Test filter with all matches")
        public void testFilterAllMatch() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] arr2 = { 1, 2, 3 };

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2).filter((s, i) -> i > 0);

            assertEquals(3, iter.toList().size());
        }

        @Test
        @DisplayName("Test multiple filters chained")
        public void testMultipleFilters() {
            Map<String, Integer> map = new LinkedHashMap<>();
            for (int i = 0; i < 20; i++) {
                map.put("key" + i, i);
            }

            BiIterator<String, Integer> iter = BiIterator.of(map).filter((k, v) -> v % 2 == 0).filter((k, v) -> v % 3 == 0);

            List<Pair<String, Integer>> results = iter.toList();
            assertTrue(results.stream().allMatch(p -> p.right() % 6 == 0));
        }
    }

    @Nested
    @DisplayName("Map Transformation Tests")
    public class MapTransformationTests {

        @Test
        @DisplayName("Test map with null transformation result")
        public void testMapToNull() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] arr2 = { 1, 2, 3 };

            ObjIterator<String> iter = BiIterator.zip(arr1, arr2).map((s, i) -> i == 2 ? null : s + i);

            assertEquals("a1", iter.next());
            assertNull(iter.next());
            assertEquals("c3", iter.next());
        }

        @Test
        @DisplayName("Test map after filter")
        public void testMapAfterFilter() {
            Map<String, Integer> map = new LinkedHashMap<>();
            map.put("one", 1);
            map.put("two", 2);
            map.put("three", 3);
            map.put("four", 4);

            List<Integer> doubled = BiIterator.of(map).filter((k, v) -> k.length() == 3).map((k, v) -> v * 2).toList();

            assertEquals(2, doubled.size());
            assertTrue(doubled.contains(2));
            assertTrue(doubled.contains(4));
        }

        @Test
        @DisplayName("Test map with exception in mapper")
        public void testMapWithException() {
            String[] arr1 = { "a", "b" };
            Integer[] arr2 = { 1, 0 };

            ObjIterator<Integer> iter = BiIterator.zip(arr1, arr2).map((s, i) -> 10 / i);

            assertEquals(10, iter.next());
            assertThrows(ArithmeticException.class, () -> iter.next());
        }
    }

    @Nested
    @DisplayName("Stream Integration Tests")
    public class StreamIntegrationTests {

        @Test
        @DisplayName("Test stream operations after BiIterator")
        public void testStreamOperations() {
            String[] arr1 = { "apple", "banana", "cherry" };
            Integer[] arr2 = { 5, 6, 6 };

            long count = BiIterator.zip(arr1, arr2).stream().filter(e -> e.getValue() == 6).count();

            assertEquals(2, count);
        }

        @Test
        @DisplayName("Test stream with mapper complex operations")
        public void testStreamMapperComplex() {
            Map<String, Integer> scores = new LinkedHashMap<>();
            scores.put("Alice", 85);
            scores.put("Bob", 92);
            scores.put("Charlie", 78);
            scores.put("David", 95);

            List<String> highScorers = BiIterator.of(scores).stream((name, score) -> score >= 90 ? name : null).filter(Objects::nonNull).sorted().toList();

            assertEquals(Arrays.asList("Bob", "David"), highScorers);
        }
    }

    @Nested
    @DisplayName("Unzip Complex Tests")
    public class UnzipComplexTests {

        @Test
        @DisplayName("Test unzip with complex objects")
        public void testUnzipComplexObjects() {
            class Person {
                String name;
                int age;

                Person(String name, int age) {
                    this.name = name;
                    this.age = age;
                }
            }

            List<Person> people = Arrays.asList(new Person("Alice", 25), new Person("Bob", 30), new Person("Charlie", 35));

            BiIterator<String, Integer> iter = BiIterator.unzip(people, (person, pair) -> pair.set(person.name, person.age));

            Map<String, Integer> nameAgeMap = new HashMap<>();
            iter.forEachRemaining(nameAgeMap::put);

            assertEquals(3, nameAgeMap.size());
            assertEquals(25, nameAgeMap.get("Alice"));
            assertEquals(30, nameAgeMap.get("Bob"));
            assertEquals(35, nameAgeMap.get("Charlie"));
        }

        @Test
        @DisplayName("Test unzip with null iterator")
        public void testUnzipNullIterator() {
            BiIterator<String, Integer> iter = BiIterator.unzip((Iterator<String>) null, (s, pair) -> pair.set(s, s.length()));

            assertFalse(iter.hasNext());
        }
    }

    @Nested
    @DisplayName("Skip and Limit Combination Tests")
    public class SkipLimitCombinationTests {

        @Test
        @DisplayName("Test skip and limit with exact boundaries")
        public void testSkipLimitExactBoundaries() {
            List<String> list = new ArrayList<>();
            for (int i = 0; i < 10; i++) {
                list.add("item" + i);
            }

            BiIterator<String, Integer> iter = BiIterator.unzip(list, (s, pair) -> pair.set(s, Integer.parseInt(s.substring(4))));

            List<Pair<String, Integer>> result = iter.skip(3).limit(7).toList();

            assertEquals(7, result.size());
            assertEquals("item3", result.get(0).left());
            assertEquals("item9", result.get(6).left());
        }

        @Test
        @DisplayName("Test limit then skip")
        public void testLimitThenSkip() {
            Map<Integer, String> map = new LinkedHashMap<>();
            for (int i = 0; i < 20; i++) {
                map.put(i, "value" + i);
            }

            List<Pair<Integer, String>> result = BiIterator.of(map).limit(10).skip(5).toList();

            assertEquals(5, result.size());
            assertEquals(5, result.get(0).left());
            assertEquals(9, result.get(4).left());
        }
    }

    @Nested
    @DisplayName("ToMultiList and ToMultiSet Advanced Tests")
    public class ToMultiCollectionTests {

        @Test
        @DisplayName("Test toMultiList with custom list implementations")
        public void testToMultiListCustomImpl() {
            String[] arr1 = { "a", "b", "c" };
            Integer[] arr2 = { 1, 2, 3 };

            Pair<List<String>, List<Integer>> result = BiIterator.zip(arr1, arr2).toMultiList(LinkedList::new);

            assertTrue(result.left() instanceof LinkedList);
            assertTrue(result.right() instanceof LinkedList);
            assertEquals(3, result.left().size());
            assertEquals(3, result.right().size());
        }

        @Test
        @DisplayName("Test toMultiSet with duplicates")
        public void testToMultiSetDuplicates() {
            String[] arr1 = { "a", "b", "a", "c", "b" };
            Integer[] arr2 = { 1, 2, 1, 3, 2 };

            Pair<Set<String>, Set<Integer>> result = BiIterator.zip(arr1, arr2).toMultiSet(TreeSet::new);

            assertTrue(result.left() instanceof TreeSet);
            assertTrue(result.right() instanceof TreeSet);
            assertEquals(3, result.left().size());
            assertEquals(3, result.right().size());

            assertEquals("a", result.left().iterator().next());
            assertEquals(1, result.right().iterator().next());
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    public class ErrorHandlingTests {

        @Test
        @DisplayName("Test null arguments validation")
        public void testNullArgumentsValidation() {
            BiIterator<String, Integer> iter = BiIterator.zip(new String[] { "a" }, new Integer[] { 1 });

            assertThrows(IllegalArgumentException.class, () -> iter.filter(null));
        }

        @Test
        @DisplayName("Test operations after iterator exhaustion")
        public void testOperationsAfterExhaustion() {
            String[] arr1 = { "a" };
            Integer[] arr2 = { 1 };

            BiIterator<String, Integer> iter = BiIterator.zip(arr1, arr2);

            iter.next();

            assertFalse(iter.hasNext());
            assertFalse(iter.first().isPresent());
            assertFalse(iter.last().isPresent());
            assertEquals(0, iter.toList().size());

            AtomicInteger count = new AtomicInteger(0);
            iter.forEachRemaining((a, b) -> count.incrementAndGet());
            assertEquals(0, count.get());
        }

        @Test
        @DisplayName("Test concurrent modification scenarios")
        public void testConcurrentModification() {
            Map<String, Integer> map = new HashMap<>();
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3);

            BiIterator<String, Integer> iter = BiIterator.of(map);

            iter.next();

            map.put("d", 4);

            try {
                iter.forEachRemaining((k, v) -> {
                });
            } catch (ConcurrentModificationException e) {
            }
        }
    }

    @Nested
    @DisplayName("Performance and Large Data Tests")
    public class PerformanceTests {

        @Test
        @DisplayName("Test with large dataset")
        public void testLargeDataset() {
            final int size = 10000;
            Map<Integer, String> largeMap = new LinkedHashMap<>();
            for (int i = 0; i < size; i++) {
                largeMap.put(i, "value" + i);
            }

            long count = BiIterator.of(largeMap).filter((k, v) -> k % 100 == 0).map((k, v) -> k).count();

            assertEquals(100, count);
        }

        @Test
        @DisplayName("Test deep chaining performance")
        public void testDeepChaining() {
            BiIterator<Integer, Integer> iter = BiIterator.generate(0, 1000, (i, pair) -> pair.set(i, i * 2));

            List<String> result = iter.skip(100)
                    .limit(800)
                    .filter((a, b) -> a % 2 == 0)
                    .filter((a, b) -> b % 4 == 0)
                    .skip(50)
                    .limit(100)
                    .map((a, b) -> a + ":" + b)
                    .toList();

            assertTrue(result.size() <= 100);
            assertTrue(result.stream().allMatch(s -> {
                String[] parts = s.split(":");
                int first = Integer.parseInt(parts[0]);
                int second = Integer.parseInt(parts[1]);
                return first % 2 == 0 && second % 4 == 0;
            }));
        }
    }
}
