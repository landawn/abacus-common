package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;

public class Seq103Test extends TestBase {

    @Test
    public void testSliding_WindowSize() throws Exception {
        // Test basic sliding with window size
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));

        // Test window size larger than sequence
        seq = Seq.of(1, 2).sliding(3);
        result = seq.toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));

        // Test window size 1
        seq = Seq.of(1, 2, 3).sliding(1);
        result = seq.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
        assertEquals(Arrays.asList(2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));

        // Test empty sequence
        seq = Seq.<Integer, Exception> empty().sliding(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSliding_WindowSizeAndCollectionSupplier() throws IllegalStateException, Exception {
        // Test sliding with HashSet supplier
        Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 2, 3, 3, 4).sliding(3, HashSet::new);
        List<Set<Integer>> result = seq.toList();

        assertEquals(4, result.size());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.get(0));
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), result.get(1));

        // Test with LinkedList supplier
        Seq<LinkedList<String>, Exception> seqStr = Seq.of("a", "b", "c", "d").sliding(2, n -> new LinkedList<>());
        List<LinkedList<String>> resultStr = seqStr.toList();

        assertEquals(3, resultStr.size());
        assertTrue(resultStr.get(0) instanceof LinkedList);
        assertEquals(Arrays.asList("a", "b"), resultStr.get(0));
    }

    @Test
    public void testSliding_WindowSizeAndCollector() throws Exception {
        // Test sliding with joining collector
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3, MoreCollectors.joining(","));
        List<String> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals("1,2,3", result.get(0));
        assertEquals("2,3,4", result.get(1));
        assertEquals("3,4,5", result.get(2));

        // Test with summing collector
        Seq<Integer, Exception> seqSum = Seq.of(1, 2, 3, 4, 5).sliding(2, Collectors.summingInt(Integer::intValue));
        List<Integer> resultSum = seqSum.toList();

        assertEquals(4, resultSum.size());
        assertEquals(3, resultSum.get(0)); // 1+2
        assertEquals(5, resultSum.get(1)); // 2+3
        assertEquals(7, resultSum.get(2)); // 3+4
        assertEquals(9, resultSum.get(3)); // 4+5
    }

    @Test
    public void testSliding_WindowSizeAndIncrement() throws IllegalStateException, Exception {
        // Test sliding with increment 1 (default)
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3, 1);
        List<List<Integer>> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));

        // Test sliding with increment 2
        seq = Seq.of(1, 2, 3, 4, 5).sliding(3, 2);
        result = seq.toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));

        // Test increment larger than window size
        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(2, 3);
        result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
        assertEquals(Arrays.asList(7, 8), result.get(2));
    }

    @Test
    public void testSliding_WindowSizeIncrementAndCollectionSupplier() throws Exception {
        // Test with custom collection and increment
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new);
            List<Set<Integer>> result = seq.toList();

            assertEquals(3, result.size());
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result.get(1));
            assertEquals(new HashSet<>(Arrays.asList(5, 6)), result.get(2));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new);
            List<Set<Integer>> result = seq.toList();

            assertEquals(2, result.size());
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(4, 5, 6)), result.get(1));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new);
            List<Set<Integer>> result = seq.skip(1).toList();

            assertEquals(2, result.size());
            assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(5, 6)), result.get(1));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new);
            List<Set<Integer>> result = seq.skip(1).toList();

            assertEquals(1, result.size());
            assertEquals(new HashSet<>(Arrays.asList(4, 5, 6)), result.get(0));
        }

        {
            assertEquals(4, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, HashSet::new).count());
            assertEquals(3, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new).count());

            assertEquals(2, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new).count());
        }
    }

    @Test
    public void testSliding_WindowSizeIncrementAndCollector() throws Exception {
        {

            // Test with collector and increment 
            List<String> result = Seq.of("a", "b", "c", "d", "e").sliding(2, 2, Collectors.joining("-")).toList();
            assertEquals(3, result.size());
            assertEquals("a-b", result.get(0));
            assertEquals("c-d", result.get(1));
            assertEquals("e", result.get(2));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 1, Collectors.joining("-")).skip(2).toList();
            assertEquals(2, result.size());
            assertEquals("c-d", result.get(0));
            assertEquals("d-e", result.get(1));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 3, Collectors.joining("-")).toList();
            assertEquals(2, result.size());
            assertEquals("a-b", result.get(0));
            assertEquals("d-e", result.get(1));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 3, Collectors.joining("-")).skip(1).toList();
            assertEquals(1, result.size());
            assertEquals("d-e", result.get(0));
        }

        {
            assertEquals(4, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, Collectors.joining("-")).count());
            assertEquals(3, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, 2, Collectors.joining("-")).count());

            assertEquals(2, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, 3, Collectors.joining("-")).count());
        }

    }

    @Test
    public void testSkip() throws Exception {
        // Test skip positive number
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skip(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(3, 4, 5), result);

        // Test skip 0
        seq = Seq.of(1, 2, 3).skip(0);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test skip more than available
        seq = Seq.of(1, 2, 3).skip(5);
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test skip on empty sequence
        seq = Seq.<Integer, Exception> empty().skip(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipWithAction() throws Exception {
        // Test skip with action on skipped items
        List<Integer> skippedItems = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skip(3, skippedItems::add);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(4, 5), result);
        assertEquals(Arrays.asList(1, 2, 3), skippedItems);

        // Test skip 0 with action
        skippedItems.clear();
        seq = Seq.of(1, 2, 3).skip(0, skippedItems::add);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(skippedItems.isEmpty());
    }

    @Test
    public void testSkipNulls() throws Exception {
        // Test skip nulls
        Seq<String, Exception> seq = Seq.of("a", null, "b", null, "c").skipNulls();
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test all nulls
        seq = Seq.of((String) null, (String) null, (String) null).skipNulls();
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test no nulls
        seq = Seq.of("a", "b", "c").skipNulls();
        result = seq.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSkipLast() throws Exception {
        // Test skip last elements
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skipLast(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test skip last 0
        seq = Seq.of(1, 2, 3).skipLast(0);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test skip more than available
        seq = Seq.of(1, 2, 3).skipLast(5);
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test skip last on empty sequence
        seq = Seq.<Integer, Exception> empty().skipLast(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLimit() throws Exception {
        // Test limit
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).limit(3);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test limit 0
        seq = Seq.of(1, 2, 3).limit(0);
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test limit more than available
        seq = Seq.of(1, 2, 3).limit(5);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test limit on empty sequence
        seq = Seq.<Integer, Exception> empty().limit(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLast() throws Exception {
        // Test last/takeLast
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).last(3);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(3, 4, 5), result);

        // Test last 0
        seq = Seq.of(1, 2, 3).last(0);
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test last more than available
        seq = Seq.of(1, 2, 3).last(5);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTakeLast() throws Exception {
        // Test takeLast (same as last)
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).takeLast(3);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(3, 4, 5), result);

        // Test takeLast on empty sequence
        seq = Seq.<Integer, Exception> empty().takeLast(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTop() throws Exception {
        // Test top without comparator
        Seq<Integer, Exception> seq = Seq.of(5, 2, 8, 1, 9, 3).top(3);
        List<Integer> result = seq.toList();

        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(8, 9, 5)));

        // Test top 1
        seq = Seq.of(5, 2, 8, 1, 9, 3).top(1);
        result = seq.toList();
        assertEquals(Arrays.asList(9), result);
    }

    @Test
    public void testTopWithComparator() throws Exception {
        // Test top with custom comparator
        Seq<String, Exception> seq = Seq.of("aa", "bbb", "c", "dddd").top(2, Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("bbb", "dddd")));

        // Test with reverse comparator
        seq = Seq.of("aa", "bbb", "c", "dddd").top(2, Comparator.comparingInt(String::length).reversed());
        result = seq.toList();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("c", "aa")));
    }

    @Test
    public void testReversed() throws Exception {
        // Test reversed
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).reversed();
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);

        // Test reversed on empty sequence
        seq = Seq.<Integer, Exception> empty().reversed();
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test reversed on single element
        seq = Seq.of(42).reversed();
        result = seq.toList();
        assertEquals(Arrays.asList(42), result);
    }

    @Test
    public void testRotated() throws Exception {
        // Test positive rotation
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).rotated(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);

        // Test negative rotation
        seq = Seq.of(1, 2, 3, 4, 5).rotated(-1);
        result = seq.toList();
        assertEquals(Arrays.asList(2, 3, 4, 5, 1), result);

        // Test rotation by 0
        seq = Seq.of(1, 2, 3, 4, 5).rotated(0);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        // Test rotation more than size
        seq = Seq.of(1, 2, 3).rotated(7);
        result = seq.toList();
        assertEquals(Arrays.asList(3, 1, 2), result); // 7 % 3 = 1
    }

    @Test
    public void testShuffled() throws Exception {
        // Test shuffled - just verify size and elements remain the same
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(original).shuffled();
        List<Integer> result = seq.toList();

        assertEquals(original.size(), result.size());
        assertTrue(result.containsAll(original));

        // Test shuffled with specific Random
        Random rnd = new Random(42);
        seq = Seq.of(1, 2, 3, 4, 5).shuffled(rnd);
        result = seq.toList();

        assertEquals(5, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5)));
    }

    @Test
    public void testSorted() throws Exception {
        // Test natural order sorting
        Seq<Integer, Exception> seq = Seq.of(5, 2, 8, 1, 9, 3).sorted();
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 5, 8, 9), result);

        // Test with nulls
        Seq<Integer, Exception> seqWithNulls = Seq.of(3, null, 1, null, 2).sorted();
        List<Integer> resultWithNulls = seqWithNulls.toList();
        assertEquals(Arrays.asList(null, null, 1, 2, 3), resultWithNulls);
    }

    @Test
    public void testSortedWithComparator() throws Exception {
        // Test with custom comparator
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").sorted(Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("d", "bb", "aaa", "cccc"), result);

        // Test with reverse comparator
        seq = Seq.of("aaa", "bb", "cccc", "d").sorted(Comparator.reverseOrder());
        result = seq.toList();
        assertEquals(Arrays.asList("d", "cccc", "bb", "aaa"), result);
    }

    @Test
    public void testSortedByInt() throws Exception {
        // Test sortedByInt
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").sortedByInt(String::length);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("d", "bb", "aaa", "cccc"), result);
    }

    @Test
    public void testSortedByLong() throws Exception {
        // Test sortedByLong
        class Item {
            long value;
            String name;

            Item(long value, String name) {
                this.value = value;
                this.name = name;
            }
        }

        Item item1 = new Item(100L, "item1");
        Item item2 = new Item(50L, "item2");
        Item item3 = new Item(200L, "item3");

        Seq<Item, Exception> seq = Seq.of(item1, item2, item3).sortedByLong(item -> item.value);
        List<Item> result = seq.toList();

        assertEquals(item2, result.get(0));
        assertEquals(item1, result.get(1));
        assertEquals(item3, result.get(2));
    }

    @Test
    public void testSortedByDouble() throws Exception {
        // Test sortedByDouble
        class Product {
            double price;
            String name;

            Product(double price, String name) {
                this.price = price;
                this.name = name;
            }
        }

        Product p1 = new Product(10.5, "p1");
        Product p2 = new Product(5.0, "p2");
        Product p3 = new Product(15.75, "p3");

        Seq<Product, Exception> seq = Seq.of(p1, p2, p3).sortedByDouble(p -> p.price);
        List<Product> result = seq.toList();

        assertEquals(p2, result.get(0));
        assertEquals(p1, result.get(1));
        assertEquals(p3, result.get(2));
    }

    @Test
    public void testSortedBy() throws Exception {
        // Test sortedBy with comparable
        Seq<String, Exception> seq = Seq.of("banana", "apple", "cherry").sortedBy(Function.identity());
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void testReverseSorted() throws Exception {
        // Test reverseSorted
        Seq<Integer, Exception> seq = Seq.of(5, 2, 8, 1, 9, 3).reverseSorted();
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(9, 8, 5, 3, 2, 1), result);
    }

    @Test
    public void testReverseSortedWithComparator() throws Exception {
        // Test reverseSorted with comparator
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").reverseSorted(Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("cccc", "aaa", "bb", "d"), result);
    }

    @Test
    public void testReverseSortedByInt() throws Exception {
        // Test reverseSortedByInt
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").reverseSortedByInt(String::length);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("cccc", "aaa", "bb", "d"), result);
    }

    @Test
    public void testReverseSortedByLong() throws Exception {
        // Test reverseSortedByLong
        class Item {
            long value;

            Item(long value) {
                this.value = value;
            }
        }

        Item item1 = new Item(100L);
        Item item2 = new Item(50L);
        Item item3 = new Item(200L);

        Seq<Item, Exception> seq = Seq.of(item1, item2, item3).reverseSortedByLong(item -> item.value);
        List<Item> result = seq.toList();

        assertEquals(item3, result.get(0));
        assertEquals(item1, result.get(1));
        assertEquals(item2, result.get(2));
    }

    @Test
    public void testReverseSortedByDouble() throws Exception {
        // Test reverseSortedByDouble
        class Product {
            double price;

            Product(double price) {
                this.price = price;
            }
        }

        Product p1 = new Product(10.5);
        Product p2 = new Product(5.0);
        Product p3 = new Product(15.75);

        Seq<Product, Exception> seq = Seq.of(p1, p2, p3).reverseSortedByDouble(p -> p.price);
        List<Product> result = seq.toList();

        assertEquals(p3, result.get(0));
        assertEquals(p1, result.get(1));
        assertEquals(p2, result.get(2));
    }

    @Test
    public void testReverseSortedBy() throws Exception {
        // Test reverseSortedBy
        Seq<String, Exception> seq = Seq.of("banana", "apple", "cherry").reverseSortedBy(Function.identity());
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("cherry", "banana", "apple"), result);
    }

    @Test
    public void testCycled() throws Exception {
        // Test cycled infinitely
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).cycled().limit(10);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2, 3, 1), result);

        // Test cycled with empty sequence
        seq = Seq.<Integer, Exception> empty().cycled().limit(5);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testCycledWithRounds() throws Exception {
        // Test cycled with specific rounds
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).cycled(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);

        // Test cycled with 0 rounds
        seq = Seq.of(1, 2, 3).cycled(0);
        result = seq.toList();
        assertTrue(result.isEmpty());

        // Test cycled with 1 round
        seq = Seq.of(1, 2, 3).cycled(1);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testRateLimited() throws Exception {
        // Test rate limited with permits per second
        long startTime = System.currentTimeMillis();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).rateLimited(10.0); // 10 permits per second
        List<Integer> result = seq.toList();
        long endTime = System.currentTimeMillis();

        assertEquals(Arrays.asList(1, 2, 3), result);
        // Should take at least ~200ms for 3 elements at 10/second
        assertTrue(endTime - startTime >= 100);
    }

    @Test
    public void testRateLimitedWithRateLimiter() throws Exception {
        // Test with custom RateLimiter
        RateLimiter limiter = RateLimiter.create(5.0);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).rateLimited(limiter);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDelay() throws Exception {
        // Test delay with Duration
        long startTime = System.currentTimeMillis();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).delay(Duration.ofMillis(50));
        List<Integer> result = seq.toList();
        long endTime = System.currentTimeMillis();

        assertEquals(Arrays.asList(1, 2, 3), result);
        // Should take at least 100ms (50ms x 2 delays between elements)
        assertTrue(endTime - startTime >= 50);
    }

    @Test
    public void testDelayWithJavaTimeDuration() throws Exception {
        // Test delay with java.time.Duration
        Seq<Integer, Exception> seq = Seq.of(1, 2).delay(java.time.Duration.ofMillis(30));
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testIntersperse() throws Exception {
        // Test intersperse
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).intersperse(0);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);

        // Test intersperse with single element
        seq = Seq.of(42).intersperse(0);
        result = seq.toList();
        assertEquals(Arrays.asList(42), result);

        // Test intersperse with empty sequence
        seq = Seq.<Integer, Exception> empty().intersperse(0);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testStep() throws Exception {
        // Test step
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).step(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 3, 5), result);

        // Test step 3
        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).step(3);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 4, 7), result);

        // Test step 1
        seq = Seq.of(1, 2, 3).step(1);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIndexed() throws Exception {
        // Test indexed
        Seq<Indexed<String>, Exception> seq = Seq.of("a", "b", "c").indexed();
        List<Indexed<String>> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals("a", result.get(0).value());
        assertEquals(0L, result.get(0).index());
        assertEquals("b", result.get(1).value());
        assertEquals(1L, result.get(1).index());
        assertEquals("c", result.get(2).value());
        assertEquals(2L, result.get(2).index());
    }

    @Test
    public void testBuffered() throws Exception {
        // Test buffered with default size
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).buffered();
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testBufferedWithSize() throws Exception {
        // Test buffered with specific size
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).buffered(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testMergeWithCollection() throws Exception {
        // Test merge with collection
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithSeq() throws Exception {
        // Test merge with another Seq
        Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6);
        Seq<Integer, Exception> merged = seq1.mergeWith(seq2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = merged.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipWithCollection() throws Exception {
        // Test zip with collection
        Seq<String, Exception> seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1", "b2", "c3"), result);

        // Test zip with different sizes
        seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2), (s, i) -> s + i);
        result = seq.toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipWithCollectionAndDefaults() throws Exception {
        // Test zip with defaults
        Seq<String, Exception> seq = Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), "z", 0, (s, i) -> s + i);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1", "b2", "z3"), result);
    }

    @Test
    public void testZipWithTwoCollections() throws Exception {
        // Test zip with two collections
        Seq<String, Exception> seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), Arrays.asList("x", "y", "z"), (s, i, s2) -> s + i + s2);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testZipWithTwoCollectionsAndDefaults() throws Exception {
        // Test zip with two collections and defaults
        Seq<String, Exception> seq = Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), Arrays.asList("x"), "z", 0, "w", (s, i, s2) -> s + i + s2);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1x", "b2w", "z3w"), result);
    }

    @Test
    public void testZipWithSeq() throws Exception {
        // Test zip with Seq
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> zipped = seq1.zipWith(seq2, (s, i) -> s + i);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipWithSeqAndDefaults() throws Exception {
        // Test zip with Seq and defaults
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> zipped = seq1.zipWith(seq2, "z", 0, (s, i) -> s + i);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1", "b2", "z3"), result);
    }

    @Test
    public void testZipWithTwoSeqs() throws Exception {
        // Test zip with two Seqs
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> seq3 = Seq.of("x", "y", "z");
        Seq<String, Exception> zipped = seq1.zipWith(seq2, seq3, (s, i, s2) -> s + i + s2);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testZipWithTwoSeqsAndDefaults() throws Exception {
        // Test zip with two Seqs and defaults
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> seq3 = Seq.of("x");
        Seq<String, Exception> zipped = seq1.zipWith(seq2, seq3, "z", 0, "w", (s, i, s2) -> s + i + s2);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1x", "b2w", "z3w"), result);
    }

    @Test
    public void testForeach() throws Exception {
        // Test foreach (alias for forEach with Consumer)
        List<Integer> result = new ArrayList<>();
        Seq.of(1, 2, 3).foreach(result::add);

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEach() throws Exception {
        // Test forEach
        List<Integer> result = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(result::add);

        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test forEach on empty sequence
        result.clear();
        Seq.<Integer, Exception> empty().forEach(result::add);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForEachIndexed() throws Exception {
        // Test forEachIndexed
        List<String> result = new ArrayList<>();
        Seq.of("a", "b", "c").forEachIndexed((index, value) -> result.add(index + ":" + value));

        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testForEachUntilWithBiConsumer() throws Exception {
        // Test forEachUntil with BiConsumer
        List<Integer> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachUntil((value, flag) -> {
            result.add(value);
            if (value >= 3) {
                flag.setTrue();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachUntilWithFlag() throws Exception {
        // Test forEachUntil with external flag
        MutableBoolean stopFlag = MutableBoolean.of(false);
        List<Integer> result = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5).forEachUntil(stopFlag, value -> {
            result.add(value);
            if (value == 3) {
                stopFlag.setTrue();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), result); // Flag is checked before processing
    }

    @Test
    public void testForEachWithOnComplete() throws Exception {
        // Test forEach with onComplete
        List<Integer> result = new ArrayList<>();
        MutableBoolean completed = MutableBoolean.of(false);

        Seq.of(1, 2, 3).forEach(result::add, () -> completed.setTrue());

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(completed.isTrue());
    }

    @Test
    public void testForEachWithFlatMapper() throws Exception {
        // Test forEach with flatMapper
        List<String> result = new ArrayList<>();

        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2, 3), (s, i) -> result.add(s + i));

        assertEquals(Arrays.asList("a1", "a2", "a3", "b1", "b2", "b3"), result);
    }

    @Test
    public void testForEachWithTwoFlatMappers() throws Exception {
        // Test forEach with two flatMappers
        List<String> result = new ArrayList<>();

        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2), i -> Arrays.asList("x", "y"), (s, i, s2) -> result.add(s + i + s2));

        assertEquals(Arrays.asList("a1x", "a1y", "a2x", "a2y", "b1x", "b1y", "b2x", "b2y"), result);
    }

    @Test
    public void testForEachPair() throws Exception {
        // Test forEachPair
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4).forEachPair((a, b) -> result.add(a + "," + b));

        assertEquals(Arrays.asList("1,2", "2,3", "3,4"), result);

        // Test with single element
        result.clear();
        Seq.of(1).forEachPair((a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,null"), result);
    }

    @Test
    public void testForEachPairWithIncrement() throws Exception {
        // Test forEachPair with increment
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachPair(2, (a, b) -> result.add(a + "," + b));

        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), result);

        // Test with increment 3
        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8).forEachPair(3, (a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "4,5", "7,8"), result);
    }

    @Test
    public void testForEachTriple() throws Exception {
        // Test forEachTriple
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));

        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), result);

        // Test with less than 3 elements
        result.clear();
        Seq.of(1, 2).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,null"), result);
    }

    @Test
    public void testForEachTripleWithIncrement() throws Exception {
        // Test forEachTriple with increment
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).forEachTriple(3, (a, b, c) -> result.add(a + "," + b + "," + c));

        assertEquals(Arrays.asList("1,2,3", "4,5,6", "7,8,9"), result);

        // Test with increment 2
        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7).forEachTriple(2, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "3,4,5", "5,6,7"), result);
    }

    @Test
    public void testMin() throws Exception {
        // Test min with comparator
        Optional<Integer> min = Seq.of(5, 2, 8, 1, 9, 3).min(Comparator.naturalOrder());

        assertTrue(min.isPresent());
        assertEquals(1, min.get());

        // Test min on empty sequence
        min = Seq.<Integer, Exception> empty().min(Comparator.naturalOrder());
        assertFalse(min.isPresent());

        // Test min with reverse comparator
        min = Seq.of(5, 2, 8, 1, 9, 3).min(Comparator.reverseOrder());
        assertTrue(min.isPresent());
        assertEquals(9, min.get());
    }

    @Test
    public void testMinBy() throws Exception {
        // Test minBy
        Optional<String> shortest = Seq.of("apple", "pie", "banana").minBy(String::length);

        assertTrue(shortest.isPresent());
        assertEquals("pie", shortest.get());

        // Test minBy on empty sequence
        shortest = Seq.<String, Exception> empty().minBy(String::length);
        assertFalse(shortest.isPresent());
    }

    @Test
    public void testMax() throws Exception {
        // Test max with comparator
        Optional<Integer> max = Seq.of(5, 2, 8, 1, 9, 3).max(Comparator.naturalOrder());

        assertTrue(max.isPresent());
        assertEquals(9, max.get());

        // Test max on empty sequence
        max = Seq.<Integer, Exception> empty().max(Comparator.naturalOrder());
        assertFalse(max.isPresent());
    }

    @Test
    public void testMaxBy() throws Exception {
        // Test maxBy
        Optional<String> longest = Seq.of("apple", "pie", "banana").maxBy(String::length);

        assertTrue(longest.isPresent());
        assertEquals("banana", longest.get());

        // Test maxBy on empty sequence
        longest = Seq.<String, Exception> empty().maxBy(String::length);
        assertFalse(longest.isPresent());
    }

    @Test
    public void testAnyMatch() throws Exception {
        // Test anyMatch
        boolean hasEven = Seq.of(1, 2, 3, 4, 5).anyMatch(n -> n % 2 == 0);
        assertTrue(hasEven);

        boolean hasNegative = Seq.of(1, 2, 3, 4, 5).anyMatch(n -> n < 0);
        assertFalse(hasNegative);

        // Test anyMatch on empty sequence
        boolean anyMatch = Seq.<Integer, Exception> empty().anyMatch(n -> true);
        assertFalse(anyMatch);
    }

    @Test
    public void testEdgeCases() throws Exception {
        // Test sliding with window size equal to sequence size
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3).sliding(3);
        List<List<Integer>> result = seq.toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));

        // Test various operations on single element sequence
        List<Integer> single = Arrays.asList(42);
        assertEquals(single, Seq.of(42).sorted().toList());
        assertEquals(single, Seq.of(42).reversed().toList());
        assertEquals(single, Seq.of(42).shuffled().toList());
        assertEquals(single, Seq.of(42).skip(0).toList());
        assertEquals(single, Seq.of(42).limit(10).toList());

        // Test operations that handle nulls
        List<Integer> withNulls = Arrays.asList(null, 1, null, 2, null);
        Seq<Integer, Exception> nullSeq = Seq.of(withNulls);
        assertEquals(Arrays.asList(1, 2), nullSeq.skipNulls().toList());
    }

    @Test
    public void testExceptionHandling() throws Exception {
        // Test that operations throw appropriate exceptions for invalid arguments
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(2, 0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).skip(-1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).limit(-1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).rateLimited(0.0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).buffered(0));
    }
}