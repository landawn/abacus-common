package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.TriFunction;

@Tag("new-test")
public class N106Test extends TestBase {

    @Test
    public void testComplexMergeWithMultipleIterables() {
        List<List<Integer>> iterables = new ArrayList<>();
        iterables.add(Arrays.asList(1, 5, 9));
        iterables.add(Arrays.asList(2, 6, 10));
        iterables.add(Arrays.asList(3, 7, 11));
        iterables.add(Arrays.asList(4, 8, 12));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;

        List<Integer> result = N.merge(iterables, selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12), result);
    }

    @Test
    public void testMergeWithNullElements() {
        List<List<Integer>> iterables = new ArrayList<>();
        iterables.add(null);
        iterables.add(Arrays.asList(1, 2, 3));
        iterables.add(null);
        iterables.add(Arrays.asList(4, 5, 6));

        BiFunction<Integer, Integer, MergeResult> selector = (a, b) -> MergeResult.TAKE_FIRST;

        List<Integer> result = N.merge(iterables, selector);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testGroupByWithComplexCollector() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 35, "Sales"),
                new Person("David", 25, "Sales"), new Person("Eve", 30, "Engineering"));

        Map<String, Double> avgAgeByDept = N.groupBy(people, Person::getDepartment, Collectors.averagingInt(Person::getAge));

        assertEquals(28.33, avgAgeByDept.get("Engineering"), 0.01);
        assertEquals(30.0, avgAgeByDept.get("Sales"), 0.01);
    }

    @Test
    public void testNestedGroupBy() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Engineering"), new Person("Charlie", 25, "Sales"),
                new Person("David", 30, "Sales"));

        Map<String, List<Person>> byDept = N.groupBy(people, Person::getDepartment);

        Map<String, Map<Integer, List<Person>>> result = new HashMap<>();
        for (Map.Entry<String, List<Person>> entry : byDept.entrySet()) {
            result.put(entry.getKey(), N.groupBy(entry.getValue(), Person::getAge));
        }

        assertEquals(2, result.get("Engineering").size());
        assertEquals(1, result.get("Engineering").get(25).size());
        assertEquals("Alice", result.get("Engineering").get(25).get(0).getName());
    }

    @Test
    public void testZipWithDifferentTypesAndTransformations() {
        String[] names = { "Alice", "Bob", "Charlie" };
        Integer[] ages = { 25, 30, 35 };
        String[] departments = { "Engineering", "Sales", "Marketing" };

        TriFunction<String, Integer, String, Person> zipper = (name, age, dept) -> new Person(name, age, dept);

        List<Person> people = N.zip(names, ages, departments, zipper);
        assertEquals(3, people.size());
        assertEquals("Alice", people.get(0).getName());
        assertEquals(30, people.get(1).getAge());
        assertEquals("Marketing", people.get(2).getDepartment());
    }

    @Test
    public void testUnzipComplexObjects() {
        List<Person> people = Arrays.asList(new Person("Alice", 25, "Engineering"), new Person("Bob", 30, "Sales"), new Person("Charlie", 35, "Marketing"));

        BiConsumer<Person, Pair<String, Integer>> unzipper = (person, pair) -> pair.set(person.getName(), person.getAge());

        Pair<List<String>, List<Integer>> result = N.unzip(people, unzipper);
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie"), result.left());
        assertEquals(Arrays.asList(25, 30, 35), result.right());
    }

    @Test
    public void testFilterPerformanceWithLargeData() {
        int size = 100000;
        Integer[] data = new Integer[size];
        for (int i = 0; i < size; i++) {
            data[i] = i;
        }

        long startTime = System.currentTimeMillis();
        List<Integer> result = N.filter(data, i -> i % 100 == 0);
        long endTime = System.currentTimeMillis();

        assertEquals(1000, result.size());
        assertTrue((endTime - startTime) < 100, "Filter operation took too long");
    }

    @Test
    public void testChainedOperations() {
        String[] data = { "apple", "banana", "apricot", "berry", "cherry", "date" };

        List<String> filtered = N.filter(data, s -> s.length() > 5);
        List<Character> mapped = N.map(filtered, s -> s.charAt(0));
        List<Character> distinct = N.distinct(mapped);

        assertEquals(3, distinct.size());
        assertTrue(distinct.contains('b'));
        assertTrue(distinct.contains('a'));
        assertTrue(distinct.contains('c'));
    }

    @Test
    public void testComplexPredicateCombinations() {
        Integer[] numbers = { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10 };

        Predicate<Integer> isEven = i -> i % 2 == 0;
        Predicate<Integer> isGreaterThan5 = i -> i > 5;
        Predicate<Integer> combined = i -> isEven.test(i) && isGreaterThan5.test(i);

        List<Integer> result = N.filter(numbers, combined);
        assertEquals(Arrays.asList(6, 8, 10), result);
    }

    @Nested
    public class NSpecialCasesTest {

        @Test
        public void testUnicodeAndSpecialCharacters() {
            String[] unicodeStrings = { "café", "naïve", "résumé", "🎉", "😀" };

            List<String> filtered = N.filter(unicodeStrings, s -> s.contains("é"));
            assertEquals(2, filtered.size());

            List<Integer> lengths = N.map(unicodeStrings, String::length);
            assertEquals(Arrays.asList(4, 5, 6, 2, 2), lengths);
        }

        @Test
        public void testConcurrentModificationScenarios() {
            List<Integer> list = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));

            assertThrows(ConcurrentModificationException.class, () -> N.filter(list, i -> {
                if (i == 3) {
                    list.add(6);
                }
                return i % 2 == 0;
            }));

        }

        @Test
        public void testMemoryEfficientOperations() {
            int[] largeArray = new int[1000000];
            for (int i = 0; i < largeArray.length; i++) {
                largeArray[i] = i;
            }

            long startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
            int count = N.count(largeArray, i -> i % 1000 == 0);
            long endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

            assertEquals(1000, count);
            assertTrue((endMemory - startMemory) < 1000000, "Memory usage increased significantly");
        }

        @Test
        public void testBoundaryValues() {
            int maxSize = 1000;

            boolean[] allTrue = new boolean[maxSize];
            Arrays.fill(allTrue, true);
            assertTrue(N.allTrue(allTrue));
            assertFalse(N.anyFalse(allTrue));

            int[] numbers = { 1, 2, 3, 4, 5 };
            assertArrayEquals(new int[0], N.filter(numbers, 3, 3, i -> true));
            assertEquals(0, N.count(numbers, 5, 5, i -> true));
        }

        @Test
        public void testCustomPredicatesAndFunctions() {
            String[] words = { "hello", "world", "java", "programming" };

            Predicate<String> complexPredicate = new Predicate<String>() {
                @Override
                public boolean test(String s) {
                    return s.length() > 4 && s.contains("o") && !s.startsWith("p");
                }
            };

            List<String> result = N.filter(words, complexPredicate);
            assertEquals(Arrays.asList("hello", "world"), result);

            Function<String, String> statefulMapper = new Function<String, String>() {
                private int counter = 0;

                @Override
                public String apply(String s) {
                    return s + "_" + (counter++);
                }
            };

            List<String> mapped = N.map(words, statefulMapper);
            assertEquals("hello_0", mapped.get(0));
            assertEquals("programming_3", mapped.get(3));
        }

        @Test
        public void testNullElementHandling() {
            String[] withNulls = { "a", null, "b", null, "c" };

            List<String> nonNulls = N.filter(withNulls, Objects::nonNull);
            assertEquals(Arrays.asList("a", "b", "c"), nonNulls);

            List<Integer> lengths = N.map(withNulls, s -> s == null ? -1 : s.length());
            assertEquals(Arrays.asList(1, -1, 1, -1, 1), lengths);

            String[] duplicatesWithNulls = { "a", null, "a", null, "b" };
            List<String> distinct = N.distinct(duplicatesWithNulls);
            assertEquals(3, distinct.size());
        }

        @Test
        public void testExtremeRanges() {
            int[] array = new int[100];
            for (int i = 0; i < 100; i++) {
                array[i] = i;
            }

            int[] filtered = N.filter(array, 0, 100, i -> i % 10 == 0);
            assertEquals(10, filtered.length);

            filtered = N.filter(array, 50, 51, i -> true);
            assertArrayEquals(new int[] { 50 }, filtered);

            filtered = N.filter(array, 99, 100, i -> true);
            assertArrayEquals(new int[] { 99 }, filtered);
        }
    }

    @Nested
    public class NCollectionsIntegrationTest {

        @Test
        public void testWithDifferentCollectionTypes() {
            LinkedList<String> linkedList = new LinkedList<>(Arrays.asList("a", "b", "c"));
            List<String> upperLinked = N.map(linkedList, 0, 3, String::toUpperCase);
            assertEquals(Arrays.asList("A", "B", "C"), upperLinked);

            TreeSet<Integer> treeSet = new TreeSet<>(Arrays.asList(3, 1, 4, 1, 5, 9));
            List<Integer> filtered = N.filter(treeSet, i -> i > 3);
            assertEquals(Arrays.asList(4, 5, 9), filtered);

            ArrayDeque<String> deque = new ArrayDeque<>(Arrays.asList("first", "second", "third"));
            List<Integer> lengths = N.map(deque, String::length);
            assertEquals(Arrays.asList(5, 6, 5), lengths);
        }

        @Test
        public void testWithCustomCollections() {
            CustomIterable<Integer> custom = new CustomIterable<>(Arrays.asList(1, 2, 3, 4, 5));

            List<Integer> filtered = N.filter(custom, i -> i % 2 == 0);
            assertEquals(Arrays.asList(2, 4), filtered);

            List<String> mapped = N.map(custom, i -> "num" + i);
            assertEquals(5, mapped.size());
        }

        @Test
        public void testCollectorIntegration() {
            List<String> words = Arrays.asList("apple", "banana", "apricot", "blueberry", "cherry");

            Map<Character, String> joined = N.groupBy(words, s -> s.charAt(0), Collectors.joining(", "));

            assertEquals("apple, apricot", joined.get('a'));
            assertEquals("banana, blueberry", joined.get('b'));
            assertEquals("cherry", joined.get('c'));

            Map<Integer, Long> countByLength = N.groupBy(words, String::length, Collectors.counting());

            assertEquals(1L, (long) countByLength.get(5));
            assertEquals(2L, (long) countByLength.get(6));
        }

        static class CustomIterable<T> implements Iterable<T> {
            private final List<T> data;

            public CustomIterable(List<T> data) {
                this.data = data;
            }

            @Override
            public Iterator<T> iterator() {
                return data.iterator();
            }
        }
    }

    @Nested
    public class NPrimitiveArrayTest {

        @Test
        public void testPrimitiveArrayConversions() {
            long[] longs = { 1L, 2L, 3L, 4L, 5L };
            int[] ints = N.mapToInt(longs, l -> (int) (l * 2));
            assertArrayEquals(new int[] { 2, 4, 6, 8, 10 }, ints);

            int[] intArray = { 1, 2, 3 };
            long[] longArray = N.mapToLong(intArray, i -> i * 1000000L);
            assertArrayEquals(new long[] { 1000000L, 2000000L, 3000000L }, longArray);

            int[] scores = { 85, 90, 78, 92, 88 };
            double[] percentages = N.mapToDouble(scores, score -> score / 100.0);
            assertArrayEquals(new double[] { 0.85, 0.90, 0.78, 0.92, 0.88 }, percentages, 0.001);
        }

        @Test
        public void testPrimitivePredicates() {
            char[] chars = { 'a', 'B', 'c', 'D', 'e' };
            char[] uppercase = N.filter(chars, Character::isUpperCase);
            assertArrayEquals(new char[] { 'B', 'D' }, uppercase);

            byte[] bytes = { -128, -1, 0, 1, 127 };
            byte[] positive = N.filter(bytes, b -> b > 0);
            assertArrayEquals(new byte[] { 1, 127 }, positive);

            float[] floats = { 1.5f, 2.0f, 2.5f, 3.0f, 3.5f };
            float[] integers = N.filter(floats, f -> f == (int) f);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, integers, 0.001f);
        }

        @Test
        public void testPrimitiveDistinct() {
            char[] chars = { 'a', 'b', 'a', 'c', 'b', 'c' };
            char[] distinctChars = N.distinct(chars);
            Arrays.sort(distinctChars);
            assertArrayEquals(new char[] { 'a', 'b', 'c' }, distinctChars);

            double[] doubles = { 1.1, 2.2, 1.1, 3.3, 2.2, 3.3 };
            double[] distinctDoubles = N.distinct(doubles);
            assertEquals(3, distinctDoubles.length);

            double[] special = { Double.NaN, 1.0, Double.POSITIVE_INFINITY, Double.NaN, 1.0 };
            double[] distinctSpecial = N.distinct(special);
            assertEquals(3, distinctSpecial.length);
        }
    }

    public static class Person {
        private String name;
        private int age;
        private String department;

        public Person(String name, int age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public String getDepartment() {
            return department;
        }
    }
}
