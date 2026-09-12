package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

public class IteratorsSkipTest extends IteratorsTestSupport {
    @Test
    public void testSkipAndLimitZeroCountDoesNotConsumeSource() {
        for (final long offset : new long[] { 0, 2, Long.MAX_VALUE }) {
            for (int factory = 0; factory < 3; factory++) {
                final int[] iteratorRequests = { 0 };
                final ObjIterator<Integer> source = new ObjIterator<>() {
                    @Override
                    public boolean hasNext() {
                        throw new AssertionError("a zero limit must not inspect the source");
                    }

                    @Override
                    public Integer next() {
                        throw new AssertionError("a zero limit must not consume the source");
                    }
                };
                final ObjIterator<Integer> result = switch (factory) {
                    case 0 -> Iterators.skipAndLimit(source, offset, 0);
                    case 1 -> source.skipAndLimit(offset, 0);
                    case 2 -> Iterators.skipAndLimit((Iterable<Integer>) () -> {
                        iteratorRequests[0]++;
                        return source;
                    }, offset, 0);
                    default -> throw new AssertionError();
                };

                assertEquals(factory == 2 ? 1 : 0, iteratorRequests[0]);
                assertFalse(result.hasNext());
                assertThrows(NoSuchElementException.class, result::next);
                assertTrue(result.toList().isEmpty());
            }
        }
    }

    @Test
    public void testSkipPreservesProgressWhenSupplierFailsBeforeProducingValue() {
        for (final boolean limited : new boolean[] { false, true }) {
            final int[] delivered = { 0 };
            final AtomicBoolean failed = new AtomicBoolean();
            final ObjIterator<Integer> source = ObjIterator.generate(() -> {
                if (delivered[0] == 1 && !failed.getAndSet(true)) {
                    throw new IllegalStateException("temporary source failure");
                }
                return delivered[0]++;
            });
            final ObjIterator<Integer> skipped = limited ? Iterators.skipAndLimit(source, 2, 1) : Iterators.skip(source, 2);

            assertThrows(IllegalStateException.class, skipped::hasNext);
            assertTrue(skipped.hasNext());
            assertEquals(2, skipped.next());
            assertEquals(3, delivered[0]);
            if (limited) {
                assertFalse(skipped.hasNext());
            }
        }
    }

    @Test
    public void testSkipMoreThanSize() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 10);

        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_ExactSize() {
        ObjIterator<Integer> result = Iterators.skip(Arrays.asList(1, 2, 3).iterator(), 3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipZero() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> skipped = Iterators.skip(iter, 0);

        List<Integer> result = new ArrayList<>();
        while (skipped.hasNext()) {
            result.add(skipped.next());
        }

        assertEquals(testList, result);
    }

    @Test
    public void testSkipNullIterator() {
        ObjIterator<Integer> skipped = Iterators.skip(null, 2);
        assertFalse(skipped.hasNext());
    }

    @Test
    public void testSkip_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.skip(Collections.<Integer> emptyIterator(), 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkip() {
        ObjIterator<Integer> result = Iterators.skip(Arrays.asList(1, 2, 3, 4, 5).iterator(), 2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skip(Arrays.asList(1, 2).iterator(), 5);
        assertFalse(result.hasNext());

        result = Iterators.skip((Iterator<Integer>) null, 2);
        assertFalse(result.hasNext());

        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testSkipNegativeThrowsException() {
        Iterator<Integer> iter = testList.iterator();
        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(iter, -1));
    }

    @Test
    public void testSkipAndLimit() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 1, 3);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(2, 3, 4), resultList);
    }

    @Test
    public void testSkipAndLimitIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5), 1, 3);
        assertEquals(Arrays.asList(2, 3, 4), iter.toList());
    }

    @Test
    public void testSkipAndLimit_OffsetBeyondSize() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3).iterator(), 10, 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimit_Iterator() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, 3);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(3, 4, 5), list);
    }

    @Test
    public void testSkipAndLimitIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5), 2, 2);
        assertEquals(Arrays.asList(3, 4), result.toList());

        result = Iterators.skipAndLimit((Iterable<Integer>) null, 1, 2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitMaxCount() {
        Iterator<Integer> iter = testList.iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, Long.MAX_VALUE);

        List<Integer> resultList = new ArrayList<>();
        while (result.hasNext()) {
            resultList.add(result.next());
        }

        assertEquals(Arrays.asList(3, 4, 5), resultList);
    }

    @Test
    public void testSkipAndLimit_ZeroCount() {
        ObjIterator<Integer> result = Iterators.skipAndLimit(Arrays.asList(1, 2, 3).iterator(), 0, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimitIterable_NullIterable() {
        ObjIterator<Integer> result = Iterators.skipAndLimit((Iterable<Integer>) null, 0, 5);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipAndLimit_ZeroOffset() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 0, 3);
        List<Integer> list = new ArrayList<>();
        result.forEachRemaining(list::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testSkipAndLimit_ZeroCountResult() {
        Iterator<Integer> iter = Arrays.asList(1, 2, 3, 4, 5).iterator();
        ObjIterator<Integer> result = Iterators.skipAndLimit(iter, 2, 0);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterable() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c"));
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls(Arrays.asList(null, null));
        assertFalse(result.hasNext());

        result = Iterators.skipNulls((Iterable<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsIterator() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c").iterator());
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());

        result = Iterators.skipNulls((Iterator<String>) null);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNullsAllNulls() {
        List<String> allNulls = Arrays.asList(null, null, null);
        ObjIterator<String> result = Iterators.skipNulls(allNulls);

        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNulls() {
        List<String> withNulls = Arrays.asList("a", null, "b", null, "c");
        ObjIterator<String> iter = Iterators.skipNulls(withNulls.iterator());

        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipNulls(withNulls);
        List<String> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSkipNullsIterable_Dedicated() {
        ObjIterator<String> iter = Iterators.skipNulls(Arrays.asList("a", null, "b", null, "c"));
        assertEquals(Arrays.asList("a", "b", "c"), iter.toList());
    }

    @Test
    public void testSkipNulls_NoNulls() {
        ObjIterator<String> result = Iterators.skipNulls(Arrays.asList("a", "b", "c"));
        assertEquals(Arrays.asList("a", "b", "c"), result.toList());
    }

    @Test
    public void testSkipNulls_EmptyIterable() {
        ObjIterator<String> result = Iterators.skipNulls(Collections.<String> emptyList());
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipNulls_EmptyIterator() {
        ObjIterator<String> result = Iterators.skipNulls(Collections.<String> emptyIterator());
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilNeverTrue() {
        Predicate<Integer> greaterThanTen = x -> x > 10;
        ObjIterator<Integer> result = Iterators.skipUntil(testList, greaterThanTen);

        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil() {
        Predicate<Integer> greaterThan2 = n -> n > 2;
        ObjIterator<Integer> iter = Iterators.skipUntil(intList.iterator(), greaterThan2);

        assertEquals(Integer.valueOf(3), iter.next());
        assertEquals(Integer.valueOf(4), iter.next());
        assertEquals(Integer.valueOf(5), iter.next());
        assertFalse(iter.hasNext());

        iter = Iterators.skipUntil(intList, greaterThan2);
        List<Integer> result = new ArrayList<>();
        while (iter.hasNext()) {
            result.add(iter.next());
        }
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testSkipUntilIterable_Dedicated() {
        ObjIterator<Integer> iter = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5), n -> n >= 4);
        assertEquals(Arrays.asList(4, 5), iter.toList());
    }

    @Test
    public void testSkipUntil_FirstMatch() {
        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(5, 6, 7).iterator(), n -> n >= 5);
        assertEquals(Arrays.asList(5, 6, 7), result.toList());
    }

    @Test
    public void testSkipUntilIterable() {
        Predicate<Integer> greaterThan2 = n -> n > 2;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5), greaterThan2);
        assertEquals(Arrays.asList(3, 4, 5), result.toList());

        result = Iterators.skipUntil(Arrays.asList(1, 2), greaterThan2);
        assertFalse(result.hasNext());

        result = Iterators.skipUntil((Iterable<Integer>) null, greaterThan2);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntilIterator() {
        Predicate<Integer> greaterThan3 = n -> n > 3;

        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3, 4, 5).iterator(), greaterThan3);
        assertEquals(Arrays.asList(4, 5), result.toList());

        result = Iterators.skipUntil((Iterator<Integer>) null, greaterThan3);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil_NoneMatch() {
        ObjIterator<Integer> result = Iterators.skipUntil(Arrays.asList(1, 2, 3).iterator(), n -> n > 10);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkipUntil_EmptyIterator() {
        ObjIterator<Integer> result = Iterators.skipUntil(Collections.<Integer> emptyIterator(), n -> true);
        assertFalse(result.hasNext());
    }

    @Test
    public void testSkip_NGreaterThanRemaining_ReturnsEmpty() {
        ObjIterator<Integer> r = Iterators.skip(Arrays.asList(1, 2, 3).iterator(), 10);
        assertFalse(r.hasNext());
    }

    @Test
    public void testSkip_Zero_ReturnsAll() {
        ObjIterator<Integer> r = Iterators.skip(Arrays.asList(1, 2, 3).iterator(), 0);
        assertEquals(Arrays.asList(1, 2, 3), r.toList());
    }

    @Test
    public void testSkip_NegativeThrows() {
        assertThrows(IllegalArgumentException.class, () -> Iterators.skip(Arrays.asList(1).iterator(), -1));
    }

    @Test
    public void testSkipAndLimit_Basic() {
        ObjIterator<Integer> r = Iterators.skipAndLimit(Arrays.asList(1, 2, 3, 4, 5).iterator(), 1, 3);
        assertEquals(Arrays.asList(2, 3, 4), r.toList());
    }

    @Test
    public void testSkipNulls_FiltersNulls() {
        Iterator<String> in = Arrays.asList("a", null, "b", null).iterator();
        ObjIterator<String> r = Iterators.skipNulls(in);
        assertEquals(Arrays.asList("a", "b"), r.toList());
    }
}
