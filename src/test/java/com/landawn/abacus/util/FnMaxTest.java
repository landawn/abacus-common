package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;

public class FnMaxTest extends FnTestSupport {

    @Test
    public void testSelectFirstAndSecond() {
        assertEquals("first", Fn.selectFirst().apply("first", "second"));
        assertNull(Fn.selectFirst().apply(null, "second"));
        assertEquals("second", Fn.selectSecond().apply("first", "second"));
        assertNull(Fn.selectSecond().apply("first", null));
    }

    @Test
    public void testMin() {
        assertEquals(1, Fn.<Integer> min().apply(1, 2));
        assertEquals("apple", Fn.min(Comparator.<String> naturalOrder()).apply("apple", "banana"));
        assertEquals("ab", Fn.minBy(String::length).apply("ab", "abcd"));
        final Map.Entry<Integer, String> ten = CommonUtil.newImmutableEntry(10, "ten");
        final Map.Entry<Integer, String> five = CommonUtil.newImmutableEntry(5, "five");
        assertEquals(five, Fn.<Integer, String> minByKey().apply(ten, five));
        assertEquals(CommonUtil.newImmutableEntry("five", 5),
                Fn.<String, Integer> minByValue().apply(CommonUtil.newImmutableEntry("ten", 10), CommonUtil.newImmutableEntry("five", 5)));
        assertThrows(IllegalArgumentException.class, () -> Fn.min(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.minBy(null));
    }

    @Test
    public void testMax() {
        assertEquals(2, Fn.<Integer> max().apply(1, 2));
        assertEquals("banana", Fn.max(Comparator.<String> naturalOrder()).apply("apple", "banana"));
        assertEquals("abcd", Fn.maxBy(String::length).apply("ab", "abcd"));
        final Map.Entry<Integer, String> ten = CommonUtil.newImmutableEntry(10, "ten");
        final Map.Entry<Integer, String> five = CommonUtil.newImmutableEntry(5, "five");
        assertEquals(ten, Fn.<Integer, String> maxByKey().apply(ten, five));
        assertEquals(CommonUtil.newImmutableEntry("ten", 10),
                Fn.<String, Integer> maxByValue().apply(CommonUtil.newImmutableEntry("ten", 10), CommonUtil.newImmutableEntry("five", 5)));
        assertThrows(IllegalArgumentException.class, () -> Fn.max(null));
        assertThrows(IllegalArgumentException.class, () -> Fn.maxBy(null));
    }

    @Test
    public void testMinMax_NullHandling() {
        final BinaryOperator<String> min = Fn.min();
        final BinaryOperator<String> max = Fn.max();
        assertEquals("test", min.apply("test", null));
        assertEquals("test", min.apply(null, "test"));
        assertNull(min.apply(null, null));
        assertEquals("test", max.apply("test", null));
        assertEquals("test", max.apply(null, "test"));
        assertNull(max.apply(null, null));
    }

    @Test
    public void testCompare() {
        assertTrue(Fn.compareTo(5).apply(3) < 0);
        assertEquals(0, Fn.compareTo(5).apply(5));
        assertTrue(Fn.<String> compare().apply("apple", "banana") < 0);
        assertEquals(0, Fn.<Integer> compare().apply(5, 5));
        assertEquals(-1, Fn.<String> compare(Comparator.naturalOrder()).apply("a", "b"));
    }

    @Test
    public void testAtMost() {
        final Predicate<Integer> atMost = Fn.atMost(3);
        assertTrue(atMost.test(1));
        assertTrue(atMost.test(2));
        assertTrue(atMost.test(3));
        assertFalse(atMost.test(4));
        assertFalse(atMost.test(5));
        assertThrows(IllegalArgumentException.class, () -> Fn.atMost(-1));
        assertFalse(Fn.atMost(0).test(1));
    }

    @Test
    public void testLimitThenFilter() {
        final Predicate<Integer> pred = Fn.limitThenFilter(5, n -> n % 2 == 0);
        assertEquals(List.of(2, 4), List.of(1, 2, 3, 4, 5, 6, 7, 8).stream().filter(pred).toList());
        final BiPredicate<Integer, Integer> bi = Fn.limitThenFilter(2, (a, b) -> a < b);
        assertTrue(bi.test(1, 2));
        assertFalse(bi.test(3, 1));
        assertFalse(bi.test(1, 4));
        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(1, (Predicate<Integer>) null));
        assertThrows(IllegalArgumentException.class, () -> Fn.limitThenFilter(-1, (Predicate<Integer>) n -> true));
    }

    @Test
    public void testFilterThenLimit() {
        final Predicate<Integer> pred = Fn.filterThenLimit(n -> n % 2 == 0, 2);
        assertEquals(List.of(2, 4), List.of(1, 2, 3, 4, 5, 6).stream().filter(pred).toList());
        final BiPredicate<Integer, Integer> bi = Fn.filterThenLimit((a, b) -> a < b, 1);
        assertTrue(bi.test(1, 2));
        assertFalse(bi.test(1, 4));
        assertThrows(IllegalArgumentException.class, () -> Fn.filterThenLimit((Predicate<Integer>) null, 1));
    }

    @Test
    public void testTimeLimit() throws InterruptedException {
        final Predicate<Object> millis = Fn.timeLimit(50);
        assertTrue(millis.test("a"));
        Thread.sleep(80);
        assertFalse(millis.test("b"));
        final Predicate<Object> duration = Fn.timeLimit(com.landawn.abacus.util.Duration.ofMillis(50));
        assertTrue(duration.test("a"));
        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit(-1));
        assertThrows(IllegalArgumentException.class, () -> Fn.timeLimit((com.landawn.abacus.util.Duration) null));
    }

    @Test
    public void testIndexed() {
        final Function<String, Indexed<String>> indexed = Fn.indexed();
        assertEquals(0, indexed.apply("a").index());
        assertEquals(1, indexed.apply("b").index());
        assertEquals("c", indexed.apply("c").value());

        final Predicate<String> firstOnly = Fn.indexed((idx, s) -> idx == 0);
        assertTrue(firstOnly.test("x"));
        assertFalse(firstOnly.test("y"));
        assertThrows(IllegalArgumentException.class, () -> Fn.indexed((com.landawn.abacus.util.function.IntObjPredicate<String>) null));
    }

    @Test
    public void testMergers() {
        assertThrows(IllegalStateException.class, () -> Fn.throwingMerger().apply("a", "b"));
        assertEquals("a", Fn.ignoringMerger().apply("a", "b"));
        assertEquals("b", Fn.replacingMerger().apply("a", "b"));
    }

    @Test
    public void testAlternate() {
        final BiFunction<String, String, MergeResult> alt = Fn.alternate();
        assertEquals(MergeResult.TAKE_FIRST, alt.apply("a", "b"));
        assertEquals(MergeResult.TAKE_SECOND, alt.apply("a", "b"));
        assertEquals(MergeResult.TAKE_FIRST, alt.apply("a", "b"));
    }
}
