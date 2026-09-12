package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.TriFunction;

public class IteratorsZipTest extends IteratorsTestSupport {
    @Test
    public void testZipTwoIterables() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());
    }

    @Test
    public void testZipTwoIteratorsWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a").iterator(), 9, "z", zipFn);
        assertEquals(Arrays.asList("1a", "2z", "3z"), result.toList());
    }

    @Test
    public void testZipTwoIterablesWithDefaultValues() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b", "c"), 0, "x", zipFn);
        assertEquals(Arrays.asList("1a", "2b", "0c"), result.toList());
    }

    @Test
    public void testZipThreeIterators() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false).iterator(),
                zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());

        result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true).iterator(), zipFn);
        assertEquals(Arrays.asList("1atrue"), result.toList());
    }

    @Test
    public void testZipThreeIterables() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(true, false), zipFn);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), result.toList());
    }

    @Test
    public void testZipThreeIteratorsWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a", "b").iterator(), Arrays.asList(true, false, true).iterator(),
                0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0xtrue"), result.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaultValues() {
        TriFunction<Integer, String, Boolean, String> zipFn = (a, b, c) -> a + b + c;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(true), 0, "x", false, zipFn);
        assertEquals(Arrays.asList("1atrue", "0bfalse"), result.toList());
    }

    @Test
    public void testZipIterators() {
        Iterator<String> s = list("a", "b").iterator();
        Iterator<Integer> i = list(1, 2, 3).iterator();
        ObjIterator<String> zipped = Iterators.zip(s, i, (str, num) -> str + num);

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaults() {
        Iterable<String> s = list("a", "b");
        Iterable<Integer> i = list(1, 2, 3, 4);
        ObjIterator<String> zipped = Iterators.zip(s, i, "defaultS", 0, (str, num) -> str + num);

        assertEquals("a1", zipped.next());
        assertEquals("b2", zipped.next());
        assertEquals("defaultS3", zipped.next());
        assertEquals("defaultS4", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() {
        Iterable<String> s = list("a");
        Iterable<Integer> i = list(1, 2);
        Iterable<Boolean> bl = list(true, false, true);

        ObjIterator<String> zipped = Iterators.zip(s, i, bl, "defS", 0, false, (str, num, boolVal) -> str + num + boolVal);
        assertEquals("a1true", zipped.next());
        assertEquals("defS2false", zipped.next());
        assertEquals("defS0true", zipped.next());
        assertFalse(zipped.hasNext());
    }

    @Test
    public void testZipTwoIteratorsWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());

        iter1 = Arrays.asList("a").iterator();
        iter2 = Arrays.asList(1, 2, 3).iterator();

        iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("X2", iter.next());
        assertEquals("X3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipTwoIterablesWithDefaults() {
        BiFunction<String, Integer, String> zipFunc = (a, b) -> a + b;

        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1);

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, "X", 0, zipFunc);
        assertEquals("a1", iter.next());
        assertEquals("b0", iter.next());
        assertEquals("c0", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() {
        TriFunction<String, Integer, Boolean, String> zipFunc = (a, b, c) -> a + b + c;

        Iterator<String> iter1 = Arrays.asList("a").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false, true).iterator();

        ObjIterator<String> iter = Iterators.zip(iter1, iter2, iter3, "X", 0, false, zipFunc);
        assertEquals("a1true", iter.next());
        assertEquals("X2false", iter.next());
        assertEquals("X0true", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testZip() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipIterables() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2, 3);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c3", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipIterablesWithDefaultValues() {
        Iterable<String> iter1 = Arrays.asList("a", "b", "c");
        Iterable<Integer> iter2 = Arrays.asList(1, 2);

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "default", -1, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c-1", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeWithDefaultValues() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + b;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, "default", -1, false, zipFunction);

        assertEquals("a1true", result.next());
        assertEquals("b2false", result.next());
        assertEquals("c-1false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThree() {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        TriFunction<String, Integer, Boolean, String> zipFunction = (s, i, b) -> s + i + ":" + b;

        ObjIterator<String> result = Iterators.zip(iter1, iter2, iter3, zipFunction);

        assertEquals("a1:true", result.next());
        assertEquals("b2:false", result.next());
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipWithDefaults() {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        BiFunction<String, Integer, String> zipFunction = (s, i) -> s + i;
        ObjIterator<String> result = Iterators.zip(iter1, iter2, "X", 0, zipFunction);

        assertEquals("a1", result.next());
        assertEquals("b2", result.next());
        assertEquals("c0", result.next());
        assertFalse(result.hasNext());
    }

    // ===================== zip with Iterable overloads =====================

    @Test
    public void testZipIterables_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b", "c"), (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b", "3c"), iter.toList());
    }

    @Test
    public void testZipThreeIterables_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2), Arrays.asList("a", "b"), Arrays.asList(true, false), (a, b, c) -> a + b + c);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), iter.toList());
    }

    @Test
    public void testZipIterablesWithDefaults_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1, 2, 3), Arrays.asList("a", "b"), 0, "z", (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b", "3z"), iter.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaults_Dedicated() {
        ObjIterator<String> iter = Iterators.zip(Arrays.asList(1), Arrays.asList("a", "b"), Arrays.asList(true, false, true), 0, "z", false,
                (a, b, c) -> a + b + c);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0ztrue"), iter.toList());
    }

    @Test
    public void testZipTwoIterators_UnequalLengths() {
        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2, 3, 4).iterator(), Arrays.asList("a", "b").iterator(), (a, b) -> a + b);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());
    }

    // ===================== zip Additional Edge Cases =====================

    @Test
    public void testZipTwoIterators_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(), (a, b) -> a + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIterators_ExhaustedNextDoesNotConsumeLongerInput() {
        final Iterator<Integer> longer = Arrays.asList(1, 2).iterator();
        final Iterator<String> shorter = Arrays.asList("a").iterator();
        final ObjIterator<String> result = Iterators.zip(longer, shorter, (a, b) -> a + b);

        assertEquals("1a", result.next());
        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, result::next);
        assertEquals(2, longer.next());
    }

    @Test
    public void testZipThreeIterators_ExhaustedNextDoesNotConsumeOtherInputs() {
        final Iterator<Integer> first = Arrays.asList(1).iterator();
        final Iterator<String> second = Arrays.asList("a").iterator();
        final Iterator<Boolean> exhausted = Collections.emptyIterator();
        final ObjIterator<String> result = Iterators.zip(first, second, exhausted, (a, b, c) -> a + b + c);

        assertFalse(result.hasNext());
        assertThrows(NoSuchElementException.class, result::next);
        assertEquals(1, first.next());
        assertEquals("a", second.next());
    }

    @Test
    public void testZipTwoIterators_NullIterators() {
        ObjIterator<String> result = Iterators.zip((Iterator<Integer>) null, (Iterator<String>) null, (a, b) -> a + "" + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipThreeIterators_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(),
                Collections.<Boolean> emptyIterator(), (a, b, c) -> a + "" + b + c);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIteratorsWithDefaults_BothEmpty() {
        ObjIterator<String> result = Iterators.zip(Collections.<Integer> emptyIterator(), Collections.<String> emptyIterator(), 0, "x", (a, b) -> a + b);
        assertFalse(result.hasNext());
    }

    @Test
    public void testZipTwoIterators() {
        BiFunction<Integer, String, String> zipFn = (a, b) -> a + b;

        ObjIterator<String> result = Iterators.zip(Arrays.asList(1, 2, 3).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());

        result = Iterators.zip(Arrays.asList(1, 2).iterator(), Arrays.asList("a", "b", "c").iterator(), zipFn);
        assertEquals(Arrays.asList("1a", "2b"), result.toList());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> Iterators.zip(Arrays.asList(1).iterator(), Arrays.asList("a").iterator(), null));
    }

    // ===================== zip: shortest of inputs =====================

    @Test
    public void testZip_StopsAtShortest() {
        Iterator<Integer> a = Arrays.asList(1, 2, 3).iterator();
        Iterator<String> b = Arrays.asList("x", "y").iterator();
        ObjIterator<String> r = Iterators.zip(a, b, (i, s) -> i + s);
        assertEquals(Arrays.asList("1x", "2y"), r.toList());
    }

    @Test
    public void testZip_NullIteratorTreatedAsEmpty() {
        ObjIterator<String> r = Iterators.zip((Iterator<Integer>) null, Arrays.asList("a").iterator(), (i, s) -> i + s);
        assertFalse(r.hasNext());
    }
}
