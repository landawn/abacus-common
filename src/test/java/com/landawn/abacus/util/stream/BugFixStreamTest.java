package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.u.Optional;

/**
 * Tests verifying stream-related bug fixes found during deep code review.
 */
@Tag("2025")
public class BugFixStreamTest extends TestBase {

    // ============================================================
    // Fix: groupTo with downstream supplier (null container handling)
    // ============================================================

    @Test
    public void testGroupTo_withDownstream() {
        // groupTo should correctly group elements even when downstream supplier returns empty container
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6);

        Map<Boolean, Long> result = Stream.of(list).groupTo(i -> i % 2 == 0, Collectors.counting());

        assertEquals(3L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void testGroupTo_withDownstream_multipleKeys() {
        List<String> words = Arrays.asList("hello", "world", "hi", "hey", "wow");

        Map<Character, List<String>> result = Stream.of(words).groupTo(s -> s.charAt(0), Collectors.toList());

        assertEquals(Arrays.asList("hello", "hi", "hey"), result.get('h'));
        assertEquals(Arrays.asList("world", "wow"), result.get('w'));
    }

    @Test
    public void testGroupTo_withDownstream_emptyStream() {
        List<Integer> empty = new ArrayList<>();

        Map<Boolean, Long> result = Stream.of(empty).groupTo(i -> i % 2 == 0, Collectors.counting());

        assertTrue(result.isEmpty());
    }

    @Test
    public void testGroupTo_withValueMapper() {
        List<String> words = Arrays.asList("apple", "banana", "avocado", "blueberry");

        Map<Character, List<Integer>> result = Stream.of(words).groupTo(s -> s.charAt(0), String::length, Collectors.toList());

        assertEquals(Arrays.asList(5, 7), result.get('a'));
        assertEquals(Arrays.asList(6, 9), result.get('b'));
    }

    // ============================================================
    // Fix: IntStream/LongStream/etc. groupTo (same bug in primitive streams)
    // ============================================================

    @Test
    public void testIntStream_groupTo_withDownstream() {
        Map<Boolean, Long> result = IntStream.of(1, 2, 3, 4, 5, 6).boxed().groupTo(i -> i % 2 == 0, Collectors.counting());

        assertEquals(3L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void testLongStream_groupTo_withDownstream() {
        Map<Boolean, Long> result = LongStream.of(1L, 2L, 3L, 4L, 5L, 6L).boxed().groupTo(i -> i % 2 == 0, Collectors.counting());

        assertEquals(3L, result.get(true));
        assertEquals(3L, result.get(false));
    }

    @Test
    public void testDoubleStream_groupTo_withDownstream() {
        Map<Boolean, Long> result = DoubleStream.of(1.0, 2.0, 3.0, 4.0).boxed().groupTo(d -> d > 2.0, Collectors.counting());

        assertEquals(2L, result.get(true));
        assertEquals(2L, result.get(false));
    }

    // ============================================================
    // Fix: Collectors.onlyOne() NPE on null elements
    // ============================================================

    @Test
    public void testCollectors_onlyOne_withNonNullElement() {
        Optional<String> result = Stream.of("hello").collect(Collectors.onlyOne());
        assertTrue(result.isPresent());
        assertEquals("hello", result.get());
    }

    @Test
    public void testCollectors_onlyOne_throwsForMultipleElements() {
        try {
            Stream.of("a", "b").collect(Collectors.onlyOne());
            assertTrue(false, "Should have thrown TooManyElementsException");
        } catch (TooManyElementsException e) {
            // expected
        }
    }

    // ============================================================
    // Fix: flatGroupTo with downstream supplier
    // ============================================================

    @Test
    public void testFlatGroupTo_withDownstream() {
        List<List<String>> data = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("b", "c"), Arrays.asList("a", "c"));

        Map<String, Long> result = Stream.of(data).flatGroupTo(l -> l, Collectors.counting());

        assertEquals(2L, result.get("a"));
        assertEquals(2L, result.get("b"));
        assertEquals(2L, result.get("c"));
    }
}
