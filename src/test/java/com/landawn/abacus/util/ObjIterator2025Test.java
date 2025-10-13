package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class ObjIterator2025Test extends TestBase {

    @Test
    public void testEmpty() {
        ObjIterator<String> iter = ObjIterator.empty();
        assertNotNull(iter);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testEmpty_multipleCallsReturnSameInstance() {
        ObjIterator<String> iter1 = ObjIterator.empty();
        ObjIterator<String> iter2 = ObjIterator.empty();
        assertSame(iter1, iter2);
    }

    @Test
    public void testEmpty_skipNulls() {
        ObjIterator<String> iter = ObjIterator.empty();
        ObjIterator<String> result = iter.skipNulls();
        assertSame(iter, result);
    }

    @Test
    public void testJust_withValue() {
        ObjIterator<String> iter = ObjIterator.just("test");
        assertTrue(iter.hasNext());
        assertEquals("test", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testJust_withNull() {
        ObjIterator<String> iter = ObjIterator.just(null);
        assertTrue(iter.hasNext());
        assertNull(iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testJust_multipleHasNextCalls() {
        ObjIterator<Integer> iter = ObjIterator.just(42);
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertTrue(iter.hasNext());
        assertEquals(42, iter.next());
        assertFalse(iter.hasNext());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_varargs_multipleElements() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_varargs_singleElement() {
        ObjIterator<Integer> iter = ObjIterator.of(42);
        assertEquals(42, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_varargs_emptyArray() {
        ObjIterator<String> iter = ObjIterator.of();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_varargs_nullArray() {
        ObjIterator<String> iter = ObjIterator.of((String[]) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_varargs_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b", null);
        assertEquals("a", iter.next());
        assertNull(iter.next());
        assertEquals("b", iter.next());
        assertNull(iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_arrayRange_validRange() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 1, 4);

        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_arrayRange_fullArray() {
        String[] array = { "a", "b", "c" };
        ObjIterator<String> iter = ObjIterator.of(array, 0, 3);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_arrayRange_emptyRange() {
        Integer[] array = { 1, 2, 3 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 2, 2);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_arrayRange_invalidNegativeFromIndex() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, -1, 2));
    }

    @Test
    public void testOf_arrayRange_invalidToIndexTooLarge() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 0, 4));
    }

    @Test
    public void testOf_arrayRange_invalidFromGreaterThanTo() {
        Integer[] array = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> ObjIterator.of(array, 2, 1));
    }

    @Test
    public void testOf_arrayRange_nullArray() {
        ObjIterator<String> iter = ObjIterator.of(null, 0, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_arrayRange_toArray() {
        String[] array = { "a", "b", "c", "d", "e" };
        ObjIterator<String> iter = ObjIterator.of(array, 1, 4);

        String[] result = iter.toArray(new String[0]);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);
    }

    @Test
    public void testOf_arrayRange_toList() {
        Integer[] array = { 1, 2, 3, 4, 5 };
        ObjIterator<Integer> iter = ObjIterator.of(array, 2, 5);

        List<Integer> result = iter.toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testOf_iterator_standardIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        ObjIterator<String> iter = ObjIterator.of(list.iterator());

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterator_nullIterator() {
        ObjIterator<String> iter = ObjIterator.of((Iterator<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterator_emptyIterator() {
        List<String> list = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.of(list.iterator());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterator_alreadyObjIterator() {
        ObjIterator<String> original = ObjIterator.of("a", "b");
        ObjIterator<String> wrapped = ObjIterator.of((Iterator<String>) original);
        assertSame(original, wrapped);
    }

    @Test
    public void testOf_collection_list() {
        List<String> list = Arrays.asList("x", "y", "z");
        ObjIterator<String> iter = ObjIterator.of(list);

        assertEquals("x", iter.next());
        assertEquals("y", iter.next());
        assertEquals("z", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_collection_nullCollection() {
        ObjIterator<String> iter = ObjIterator.of((Collection<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_collection_emptyCollection() {
        List<Integer> list = new ArrayList<>();
        ObjIterator<Integer> iter = ObjIterator.of(list);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterable_standardIterable() {
        Iterable<Integer> iterable = Arrays.asList(1, 2, 3);
        ObjIterator<Integer> iter = ObjIterator.of(iterable);

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterable_nullIterable() {
        ObjIterator<String> iter = ObjIterator.of((Iterable<String>) null);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testOf_iterable_customIterable() {
        Iterable<String> iterable = createIterable("a", "b", "c");
        ObjIterator<String> iter = ObjIterator.of(iterable);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_lazyInitialization() {
        boolean[] supplierCalled = { false };
        Supplier<Iterator<String>> supplier = () -> {
            supplierCalled[0] = true;
            return Arrays.asList("a", "b").iterator();
        };

        ObjIterator<String> iter = ObjIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertTrue(iter.hasNext());
        assertTrue(supplierCalled[0]);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer_calledOnNext() {
        boolean[] supplierCalled = { false };
        Supplier<Iterator<Integer>> supplier = () -> {
            supplierCalled[0] = true;
            return Arrays.asList(1, 2, 3).iterator();
        };

        ObjIterator<Integer> iter = ObjIterator.defer(supplier);
        assertFalse(supplierCalled[0]);

        assertEquals(1, iter.next());
        assertTrue(supplierCalled[0]);
    }

    @Test
    public void testDefer_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.defer(null));
    }

    @Test
    public void testDefer_supplierReturnsEmpty() {
        Supplier<Iterator<String>> supplier = () -> new ArrayList<String>().iterator();
        ObjIterator<String> iter = ObjIterator.defer(supplier);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_infiniteSupplier() {
        AtomicInteger counter = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.generate(() -> counter.incrementAndGet());

        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_infiniteSupplier_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate((Supplier<String>) null));
    }

    @Test
    public void testGenerate_infiniteSupplier_constantValue() {
        ObjIterator<String> iter = ObjIterator.generate(() -> "constant");
        assertEquals("constant", iter.next());
        assertEquals("constant", iter.next());
        assertTrue(iter.hasNext());
    }

    @Test
    public void testGenerate_withHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.generate(() -> counter.get() < 3, () -> counter.incrementAndGet());

        assertTrue(iter.hasNext());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_withHasNext_emptySequence() {
        ObjIterator<String> iter = ObjIterator.generate(() -> false, () -> "never");
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerate_withHasNext_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(null, () -> "value"));
    }

    @Test
    public void testGenerate_withHasNext_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(() -> true, null));
    }

    @Test
    public void testGenerate_withHasNext_nextWithoutHasNext() {
        AtomicInteger counter = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.generate(() -> counter.get() < 2, () -> counter.incrementAndGet());

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerate_withState() {
        int[] state = { 0 };
        ObjIterator<Integer> iter = ObjIterator.generate(state, s -> s[0] < 3, s -> s[0]++);

        assertEquals(0, iter.next());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_withState_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, null, x -> x));
    }

    @Test
    public void testGenerate_withState_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, x -> true, null));
    }

    @Test
    public void testGenerate_withState_nullInit() {
        ObjIterator<String> iter = ObjIterator.generate(null, s -> s == null, s -> "value");
        assertTrue(iter.hasNext());
        assertEquals("value", iter.next());
    }

    @Test
    public void testGenerate_withState_immediatelyFalse() {
        ObjIterator<Integer> iter = ObjIterator.generate(5, s -> s < 5, s -> s);
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, () -> iter.next());
    }

    @Test
    public void testGenerate_withBiPredicate() {
        ObjIterator<Integer> iter = ObjIterator.generate(0, (state, prev) -> prev == null || prev < 3, (state, prev) -> prev == null ? 1 : prev + 1);

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testGenerate_withBiPredicate_nullHasNext() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, null, (s, p) -> p));
    }

    @Test
    public void testGenerate_withBiPredicate_nullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.generate(0, (s, p) -> true, null));
    }

    @Test
    public void testGenerate_withBiPredicate_fibonacci() {
        int[] state = { 0, 1 };
        ObjIterator<Integer> iter = ObjIterator.generate(state, (s, prev) -> prev == null || prev < 50, (s, prev) -> {
            if (prev == null)
                return s[0];
            int next = s[0] + s[1];
            s[0] = s[1];
            s[1] = next;
            return s[0];
        });

        assertEquals(0, iter.next());
        assertEquals(1, iter.next());
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(5, iter.next());
        assertEquals(8, iter.next());
        assertEquals(13, iter.next());
        assertEquals(21, iter.next());
        assertEquals(34, iter.next());
        assertEquals(55, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_normalCase() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5).skip(2);

        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_zero() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").skip(0);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
    }

    @Test
    public void testSkip_negative() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skip(-1));
    }

    @Test
    public void testSkip_moreThanAvailable() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).skip(10);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_all() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").skip(3);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.<Integer> empty().skip(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkip_lazyEvaluation() {
        AtomicInteger skipped = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5).map(x -> {
            skipped.incrementAndGet();
            return x;
        }).skip(2);

        assertEquals(0, skipped.get());
        assertTrue(iter.hasNext());
        assertEquals(2, skipped.get());
        assertEquals(3, iter.next());
    }

    @Test
    public void testLimit_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c", "d", "e").limit(3);

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_zero() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).limit(0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_negative() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).limit(-1));
    }

    @Test
    public void testLimit_moreThanAvailable() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).limit(10);
        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_one() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").limit(1);
        assertEquals("a", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLimit_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.<Integer> empty().limit(5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit_normalCase() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5, 6, 7).skipAndLimit(2, 3);

        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit_skipZeroLimitAll() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").skipAndLimit(0, 10);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit_skipAllLimit() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).skipAndLimit(3, 5);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit_bothZero() {
        ObjIterator<String> iter = ObjIterator.of("a", "b").skipAndLimit(0, 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipAndLimit_negativeOffset() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skipAndLimit(-1, 2));
    }

    @Test
    public void testSkipAndLimit_negativeCount() {
        assertThrows(IllegalArgumentException.class, () -> ObjIterator.of(1, 2, 3).skipAndLimit(0, -1));
    }

    @Test
    public void testFilter_normalCase() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5, 6).filter(n -> n % 2 == 0);

        assertEquals(2, iter.next());
        assertEquals(4, iter.next());
        assertEquals(6, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_noneMatch() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 3, 5, 7).filter(n -> n % 2 == 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_allMatch() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").filter(s -> s != null);
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.<Integer> empty().filter(n -> n > 0);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFilter_chain() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(n -> n % 2 == 0).filter(n -> n > 4);

        assertEquals(6, iter.next());
        assertEquals(8, iter.next());
        assertEquals(10, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap_normalCase() {
        ObjIterator<Integer> iter = ObjIterator.of("hello", "world").map(String::length);

        assertEquals(5, iter.next());
        assertEquals(5, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.<String> empty().map(String::length);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap_withNull() {
        ObjIterator<Integer> iter = ObjIterator.of("a", null, "b").map(s -> s == null ? 0 : s.length());

        assertEquals(1, iter.next());
        assertEquals(0, iter.next());
        assertEquals(1, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap_chain() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3).map(n -> n * 2).map(n -> n + 1);

        assertEquals(3, iter.next());
        assertEquals(5, iter.next());
        assertEquals(7, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testMap_typeConversion() {
        ObjIterator<String> iter = ObjIterator.of(1, 2, 3).map(Object::toString);

        assertEquals("1", iter.next());
        assertEquals("2", iter.next());
        assertEquals("3", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "a", "c", "b").distinct();

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct_allUnique() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4).distinct();

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertEquals(4, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct_allDuplicate() {
        ObjIterator<String> iter = ObjIterator.of("x", "x", "x", "x").distinct();

        assertEquals("x", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b", null, "a").distinct();

        assertEquals("a", iter.next());
        assertNull(iter.next());
        assertEquals("b", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinct_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.<Integer> empty().distinct();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctBy_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("apple", "apricot", "banana", "blueberry").distinctBy(s -> s.charAt(0));

        assertEquals("apple", iter.next());
        assertEquals("banana", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctBy_length() {
        ObjIterator<String> iter = ObjIterator.of("a", "bb", "c", "dd", "eee").distinctBy(String::length);

        assertEquals("a", iter.next());
        assertEquals("bb", iter.next());
        assertEquals("eee", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctBy_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.<String> empty().distinctBy(String::length);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDistinctBy_identityFunction() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 1, 3, 2).distinctBy(x -> x);

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertEquals(3, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFirst_withElements() {
        ObjIterator<String> iter = ObjIterator.of("first", "second");
        Nullable<String> first = iter.first();

        assertTrue(first.isPresent());
        assertEquals("first", first.get());

        assertTrue(iter.hasNext());
        assertEquals("second", iter.next());
    }

    @Test
    public void testFirst_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.empty();
        Nullable<String> first = iter.first();

        assertFalse(first.isPresent());
    }

    @Test
    public void testFirst_singleElement() {
        ObjIterator<Integer> iter = ObjIterator.just(42);
        Nullable<Integer> first = iter.first();

        assertTrue(first.isPresent());
        assertEquals(42, first.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testFirst_nullElement() {
        ObjIterator<String> iter = ObjIterator.of((String) null, "second");
        Nullable<String> first = iter.first();

        assertTrue(first.isPresent());
        assertNull(first.get());
    }

    @Test
    public void testFirstNonNull_firstIsNonNull() {
        ObjIterator<String> iter = ObjIterator.of("first", "second");
        Optional<String> first = iter.firstNonNull();

        assertTrue(first.isPresent());
        assertEquals("first", first.get());
    }

    @Test
    public void testFirstNonNull_skipNulls() {
        ObjIterator<String> iter = ObjIterator.of(null, null, "found", "next");
        Optional<String> first = iter.firstNonNull();

        assertTrue(first.isPresent());
        assertEquals("found", first.get());
    }

    @Test
    public void testFirstNonNull_allNull() {
        ObjIterator<String> iter = ObjIterator.of(null, null, null);
        Optional<String> first = iter.firstNonNull();

        assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.empty();
        Optional<String> first = iter.firstNonNull();

        assertFalse(first.isPresent());
    }

    @Test
    public void testFirstNonNull_consumesUntilFound() {
        ObjIterator<Integer> iter = ObjIterator.of(null, null, 3, 4, 5);
        Optional<Integer> first = iter.firstNonNull();

        assertEquals(3, first.get());
        assertEquals(4, iter.next());
        assertEquals(5, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLast_withElements() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4);
        Nullable<Integer> last = iter.last();

        assertTrue(last.isPresent());
        assertEquals(4, last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testLast_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.empty();
        Nullable<String> last = iter.last();

        assertFalse(last.isPresent());
    }

    @Test
    public void testLast_singleElement() {
        ObjIterator<String> iter = ObjIterator.just("only");
        Nullable<String> last = iter.last();

        assertTrue(last.isPresent());
        assertEquals("only", last.get());
    }

    @Test
    public void testLast_nullElement() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", null);
        Nullable<String> last = iter.last();

        assertTrue(last.isPresent());
        assertNull(last.get());
    }

    @Test
    public void testLast_consumesAll() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
        Nullable<Integer> last = iter.last();

        assertEquals(3, last.get());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b", null, "c").skipNulls();

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_allNull() {
        ObjIterator<String> iter = ObjIterator.<String> of(null, null, null).skipNulls();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_noNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c").skipNulls();

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.<String> empty().skipNulls();
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_leadingNulls() {
        ObjIterator<Integer> iter = ObjIterator.of(null, null, 1, 2).skipNulls();

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testSkipNulls_trailingNulls() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, null, null).skipNulls();

        assertEquals(1, iter.next());
        assertEquals(2, iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        Object[] array = iter.toArray();

        assertArrayEquals(new Object[] { "a", "b", "c" }, array);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToArray_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.empty();
        Object[] array = iter.toArray();

        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_singleElement() {
        ObjIterator<String> iter = ObjIterator.just("only");
        Object[] array = iter.toArray();

        assertArrayEquals(new Object[] { "only" }, array);
    }

    @Test
    public void testToArray_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b");
        Object[] array = iter.toArray();

        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertNull(array[1]);
        assertEquals("b", array[2]);
    }

    @Test
    public void testToArray_typedArray_exactSize() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        String[] array = iter.toArray(new String[3]);

        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testToArray_typedArray_tooSmall() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        String[] array = iter.toArray(new String[0]);

        assertArrayEquals(new String[] { "a", "b", "c" }, array);
    }

    @Test
    public void testToArray_typedArray_tooLarge() {
        ObjIterator<String> iter = ObjIterator.of("a", "b");
        String[] array = iter.toArray(new String[5]);

        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
    }

    @Test
    public void testToArray_typedArray_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.empty();
        Integer[] array = iter.toArray(new Integer[0]);

        assertEquals(0, array.length);
    }

    @Test
    public void testToArray_typedArray_afterPartialIteration() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c", "d");
        iter.next();

        String[] array = iter.toArray(new String[0]);
        assertArrayEquals(new String[] { "b", "c", "d" }, array);
    }

    @Test
    public void testToList_normalCase() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3);
        List<Integer> list = iter.toList();

        assertEquals(Arrays.asList(1, 2, 3), list);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testToList_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.empty();
        List<String> list = iter.toList();

        assertTrue(list.isEmpty());
    }

    @Test
    public void testToList_singleElement() {
        ObjIterator<String> iter = ObjIterator.just("test");
        List<String> list = iter.toList();

        assertEquals(Arrays.asList("test"), list);
    }

    @Test
    public void testToList_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b", null);
        List<String> list = iter.toList();

        assertEquals(4, list.size());
        assertEquals("a", list.get(0));
        assertNull(list.get(1));
        assertEquals("b", list.get(2));
        assertNull(list.get(3));
    }

    @Test
    public void testToList_mutable() {
        ObjIterator<String> iter = ObjIterator.of("a", "b");
        List<String> list = iter.toList();

        list.add("c");
        assertEquals(3, list.size());
        assertEquals("c", list.get(2));
    }

    @Test
    public void testToList_afterPartialIteration() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);
        iter.next();
        iter.next();

        List<Integer> list = iter.toList();
        assertEquals(Arrays.asList(3, 4, 5), list);
    }

    @Test
    public void testStream_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        Stream<String> stream = iter.stream();

        List<String> list = stream.toList();
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testStream_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.empty();
        Stream<Integer> stream = iter.stream();

        assertEquals(0, stream.count());
    }

    @Test
    public void testStream_operations() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);
        long count = iter.stream().filter(n -> n % 2 == 0).count();

        assertEquals(2, count);
    }

    @Test
    public void testStream_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b");
        List<String> list = iter.stream().toList();

        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertNull(list.get(1));
        assertEquals("b", list.get(2));
    }

    @Test
    public void testIndexed_normalCase() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        ObjIterator<Indexed<String>> indexed = iter.indexed();

        Indexed<String> first = indexed.next();
        assertEquals(0, first.index());
        assertEquals("a", first.value());

        Indexed<String> second = indexed.next();
        assertEquals(1, second.index());
        assertEquals("b", second.value());

        Indexed<String> third = indexed.next();
        assertEquals(2, third.index());
        assertEquals("c", third.value());

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_emptyIterator() {
        ObjIterator<String> iter = ObjIterator.empty();
        ObjIterator<Indexed<String>> indexed = iter.indexed();

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_singleElement() {
        ObjIterator<Integer> iter = ObjIterator.just(42);
        ObjIterator<Indexed<Integer>> indexed = iter.indexed();

        Indexed<Integer> first = indexed.next();
        assertEquals(0, first.index());
        assertEquals(42, first.value());
        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_withNulls() {
        ObjIterator<String> iter = ObjIterator.of("a", null, "b");
        ObjIterator<Indexed<String>> indexed = iter.indexed();

        indexed.next();
        Indexed<String> second = indexed.next();
        assertEquals(1, second.index());
        assertNull(second.value());
    }

    @Test
    public void testIndexed_withStartIndex() {
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");
        ObjIterator<Indexed<String>> indexed = iter.indexed(10);

        Indexed<String> first = indexed.next();
        assertEquals(10, first.index());
        assertEquals("a", first.value());

        Indexed<String> second = indexed.next();
        assertEquals(11, second.index());
        assertEquals("b", second.value());

        Indexed<String> third = indexed.next();
        assertEquals(12, third.index());
        assertEquals("c", third.value());
    }

    @Test
    public void testIndexed_withStartIndex_zero() {
        ObjIterator<Integer> iter = ObjIterator.of(1, 2);
        ObjIterator<Indexed<Integer>> indexed = iter.indexed(0);

        assertEquals(0, indexed.next().index());
        assertEquals(1, indexed.next().index());
    }

    @Test
    public void testIndexed_withStartIndex_negative() {
        ObjIterator<String> iter = ObjIterator.of("a", "b");
        assertThrows(IllegalArgumentException.class, () -> iter.indexed(-1));
    }

    @Test
    public void testIndexed_withStartIndex_emptyIterator() {
        ObjIterator<Integer> iter = ObjIterator.empty();
        ObjIterator<Indexed<Integer>> indexed = iter.indexed(100);

        assertFalse(indexed.hasNext());
    }

    @Test
    public void testIndexed_withStartIndex_largeValue() {
        ObjIterator<String> iter = ObjIterator.of("x");
        ObjIterator<Indexed<String>> indexed = iter.indexed(1000000);

        Indexed<String> first = indexed.next();
        assertEquals(1000000, first.index());
        assertEquals("x", first.value());
    }

    @Test
    public void testForeachRemaining_normalCase() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");

        iter.foreachRemaining(collected::add);

        assertEquals(Arrays.asList("a", "b", "c"), collected);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachRemaining_emptyIterator() {
        List<Integer> collected = new ArrayList<>();
        ObjIterator<Integer> iter = ObjIterator.empty();

        iter.foreachRemaining(collected::add);

        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForeachRemaining_nullAction() {
        ObjIterator<String> iter = ObjIterator.of("a");
        assertThrows(IllegalArgumentException.class, () -> iter.foreachRemaining(null));
    }

    @Test
    public void testForeachRemaining_withNulls() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.of("a", null, "b");

        iter.foreachRemaining(collected::add);

        assertEquals(3, collected.size());
        assertEquals("a", collected.get(0));
        assertNull(collected.get(1));
        assertEquals("b", collected.get(2));
    }

    @Test
    public void testForeachRemaining_afterPartialIteration() {
        List<Integer> collected = new ArrayList<>();
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);

        iter.next();
        iter.next();

        iter.foreachRemaining(collected::add);

        assertEquals(Arrays.asList(3, 4, 5), collected);
    }

    @Test
    public void testForeachRemaining_sideEffects() {
        AtomicInteger sum = new AtomicInteger(0);
        ObjIterator<Integer> iter = ObjIterator.of(1, 2, 3, 4, 5);

        iter.foreachRemaining(sum::addAndGet);

        assertEquals(15, sum.get());
    }

    @Test
    public void testForeachIndexed_normalCase() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.of("a", "b", "c");

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), collected);
        assertFalse(iter.hasNext());
    }

    @Test
    public void testForeachIndexed_emptyIterator() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.empty();

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        assertTrue(collected.isEmpty());
    }

    @Test
    public void testForeachIndexed_nullAction() {
        ObjIterator<String> iter = ObjIterator.of("a");
        assertThrows(IllegalArgumentException.class, () -> iter.foreachIndexed(null));
    }

    @Test
    public void testForeachIndexed_withNulls() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.of("a", null, "b");

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        assertEquals(Arrays.asList("0:a", "1:null", "2:b"), collected);
    }

    @Test
    public void testForeachIndexed_afterPartialIteration() {
        List<String> collected = new ArrayList<>();
        ObjIterator<Integer> iter = ObjIterator.of(10, 20, 30, 40, 50);

        iter.next();
        iter.next();

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        assertEquals(Arrays.asList("0:30", "1:40", "2:50"), collected);
    }

    @Test
    public void testForeachIndexed_singleElement() {
        List<String> collected = new ArrayList<>();
        ObjIterator<String> iter = ObjIterator.just("only");

        iter.foreachIndexed((index, value) -> collected.add(index + ":" + value));

        assertEquals(Arrays.asList("0:only"), collected);
    }

    @Test
    public void testComplexChain() {
        List<String> result = ObjIterator.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(n -> n % 2 == 0)
                .map(n -> n * 2)
                .skip(1)
                .limit(2)
                .map(Object::toString)
                .toList();

        assertEquals(Arrays.asList("8", "12"), result);
    }

    @Test
    public void testChainWithDistinct() {
        List<Integer> result = ObjIterator.of(1, 2, 2, 3, 1, 4, 3, 5).distinct().filter(n -> n > 2).toList();

        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testChainWithSkipNulls() {
        List<String> result = ObjIterator.of("a", null, "b", null, "c").skipNulls().map(String::toUpperCase).toList();

        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testArrayRangeWithToArray() {
        String[] source = { "a", "b", "c", "d", "e" };
        ObjIterator<String> iter = ObjIterator.of(source, 1, 4);

        String[] smaller = new String[2];
        String[] result = iter.toArray(smaller);

        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "b", "c", "d" }, result);
    }
}
