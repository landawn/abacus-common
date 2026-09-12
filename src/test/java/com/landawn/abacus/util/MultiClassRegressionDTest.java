package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.rowset.serial.SerialClob;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the behaviour corrected in the 2026-08-28 review pass:
 *
 * <ul>
 *   <li>natural {@code sort} and natural {@code binarySearch} agree on where {@code null} sorts,</li>
 *   <li>{@code containsDuplicates} uses one equivalence regardless of input length or the {@code isSorted} flag,</li>
 *   <li>the {@code runAsync}/{@code callAsync} completion iterators block for completions instead of polling, and
 *       still surface a failing command.</li>
 * </ul>
 */
public class MultiClassRegressionDTest extends TestBase {

    // ------------------------------------------------------------------ sort/binarySearch null ordering

    @Test
    public void testSortThenBinarySearchRoundTripsWithNull_array() {
        final Integer[] a = { 5, null, 3, null, 1 };
        CommonUtil.sort(a);

        Assertions.assertArrayEquals(new Integer[] { null, null, 1, 3, 5 }, a);

        // every element that is present must be found where it actually is
        for (int i = 0; i < a.length; i++) {
            final int found = CommonUtil.binarySearch(a, a[i]);
            Assertions.assertTrue(found >= 0, "not found at index " + i);
            Assertions.assertEquals(a[found], a[i]);
        }

        Assertions.assertTrue(CommonUtil.binarySearch(a, (Object) null) >= 0);
    }

    @Test
    public void testSortThenBinarySearchRoundTripsWithNull_list() {
        final List<Integer> list = new ArrayList<>(Arrays.asList(5, null, 3, null, 1));
        CommonUtil.sort(list);

        Assertions.assertEquals(Arrays.asList(null, null, 1, 3, 5), list);

        for (int i = 0; i < list.size(); i++) {
            final int found = CommonUtil.binarySearch(list, list.get(i));
            Assertions.assertTrue(found >= 0, "not found at index " + i);
            Assertions.assertEquals(list.get(found), list.get(i));
        }
    }

    @Test
    public void testFullAndRangedBinarySearchAgree() {
        final Integer[] a = { null, null, 1, 3, 5 };
        final List<Integer> list = Arrays.asList(null, null, 1, 3, 5);

        for (final Integer v : new Integer[] { null, 1, 3, 5 }) {
            Assertions.assertEquals(CommonUtil.binarySearch(a, v), CommonUtil.binarySearch(a, 0, a.length, v), "array full vs ranged for " + v);
            Assertions.assertEquals(CommonUtil.binarySearch(list, v), CommonUtil.binarySearch(list, 0, list.size(), v), "list full vs ranged for " + v);
            Assertions.assertEquals(CommonUtil.binarySearch(a, v), CommonUtil.binarySearch(list, v), "array vs list for " + v);
        }
    }

    @Test
    public void testBinarySearchStillWorksWithoutNull() {
        final Integer[] a = { 1, 3, 5, 7, 9 };

        Assertions.assertEquals(2, CommonUtil.binarySearch(a, 5));
        Assertions.assertEquals(2, CommonUtil.binarySearch(a, 0, 5, 5));
        Assertions.assertTrue(CommonUtil.binarySearch(a, 4) < 0);

        final List<Integer> list = Arrays.asList(10, 20, 30, 40, 50);
        Assertions.assertEquals(2, CommonUtil.binarySearch(list, 30));
        Assertions.assertTrue(CommonUtil.binarySearch(list, 35) < 0);
        Assertions.assertEquals(-1, CommonUtil.binarySearch(CommonUtil.<Integer> emptyList(), 5));
    }

    // ------------------------------------------------------------------ containsDuplicates equivalence

    @Test
    public void testContainsDuplicatesDoesNotDependOnLength() {
        // Two distinct int[] instances with equal contents are duplicates of each other. That verdict must not
        // change when unrelated elements make the input longer.
        final Object x = new int[] { 1 };
        final Object y = new int[] { 1 };

        Assertions.assertTrue(N.containsDuplicates(new Object[] { x, y }));
        Assertions.assertTrue(N.containsDuplicates(new Object[] { x, y, "a" }));
        Assertions.assertTrue(N.containsDuplicates(new Object[] { x, y, "a", "b" }));
        Assertions.assertTrue(N.containsDuplicates(new Object[] { x, y, "a", "b", "c" }));

        Assertions.assertTrue(N.containsDuplicates(Arrays.asList(x, y)));
        Assertions.assertTrue(N.containsDuplicates(Arrays.asList(x, y, "a", "b", "c")));
    }

    @Test
    public void testContainsDuplicatesDoesNotDependOnIsSortedFlag() {
        final Object x = new int[] { 1 };
        final Object y = new int[] { 1 };

        for (final int extra : new int[] { 0, 1, 2, 3 }) {
            final List<Object> elements = new ArrayList<>();
            elements.add(x);
            elements.add(y);

            for (int i = 0; i < extra; i++) {
                elements.add("filler" + i);
            }

            final Object[] a = elements.toArray();

            Assertions.assertEquals(N.containsDuplicates(a, false), N.containsDuplicates(a, true), "array, extra=" + extra);
            Assertions.assertEquals(N.containsDuplicates(elements, false), N.containsDuplicates(elements, true), "collection, extra=" + extra);
        }
    }

    @Test
    public void testContainsDuplicatesUnchangedForNonArrayElements() {
        Assertions.assertFalse(N.containsDuplicates(new String[] { "a", "b" }));
        Assertions.assertTrue(N.containsDuplicates(new String[] { "a", "a" }));
        Assertions.assertFalse(N.containsDuplicates(new String[] { "a", "b", "c" }));
        Assertions.assertTrue(N.containsDuplicates(new String[] { "a", "b", "a" }));
        Assertions.assertFalse(N.containsDuplicates(new String[] { "a", "b", "c", "d" }));
        Assertions.assertTrue(N.containsDuplicates(new String[] { "a", "b", "c", "a" }));

        // nulls count as equal to each other, at every length
        Assertions.assertTrue(N.containsDuplicates(new String[] { null, null }));
        Assertions.assertTrue(N.containsDuplicates(new String[] { null, "a", null }));
        Assertions.assertTrue(N.containsDuplicates(new String[] { null, "a", "b", null }));
        Assertions.assertFalse(N.containsDuplicates(new String[] { null, "a" }));
    }

    // ------------------------------------------------------------------ async completion iterators

    @Test
    public void testRunAsyncYieldsOneElementPerCommandAndBlocks() throws Exception {
        final AtomicInteger ran = new AtomicInteger();
        final CountDownLatch release = new CountDownLatch(1);

        final Collection<Throwables.Runnable<? extends Exception>> commands = Arrays.asList( //
                () -> {
                    release.await(10, TimeUnit.SECONDS);
                    ran.incrementAndGet();
                }, //
                () -> {
                    release.await(10, TimeUnit.SECONDS);
                    ran.incrementAndGet();
                }, //
                () -> {
                    release.await(10, TimeUnit.SECONDS);
                    ran.incrementAndGet();
                });

        final ObjIterator<Void> iter = N.runAsync(commands);

        // nothing has completed yet; releasing here proves hasNext() waits for the completion rather than
        // deciding there is none.
        release.countDown();

        int count = 0;

        while (iter.hasNext()) {
            iter.next();
            count++;
        }

        Assertions.assertEquals(3, count);
        Assertions.assertEquals(3, ran.get());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testCallAsyncYieldsEveryResultIncludingNull() {
        final Collection<Callable<String>> commands = Arrays.asList(() -> "a", () -> null, () -> "c");

        final List<String> results = new ArrayList<>();
        final ObjIterator<String> iter = N.callAsync(commands);

        while (iter.hasNext()) {
            results.add(iter.next());
        }

        Assertions.assertEquals(3, results.size());
        Assertions.assertTrue(results.contains("a"));
        Assertions.assertTrue(results.contains("c"));
        Assertions.assertTrue(results.contains(null));
    }

    @Test
    public void testRunAsyncSurfacesCommandFailure() {
        final Collection<Throwables.Runnable<? extends Exception>> commands = Arrays.asList( //
                () -> {
                    throw new IllegalStateException("boom");
                });

        final ObjIterator<Void> iter = N.runAsync(commands);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            while (iter.hasNext()) {
                iter.next();
            }
        });
    }

    @Test
    public void testCallAsyncSurfacesCommandFailureAmongSuccesses() {
        final Collection<Callable<String>> commands = Arrays.asList( //
                () -> "ok", //
                () -> {
                    throw new IllegalStateException("boom");
                }, //
                () -> "ok2");

        final ObjIterator<String> iter = N.callAsync(commands);

        Assertions.assertThrows(IllegalStateException.class, () -> {
            while (iter.hasNext()) {
                iter.next();
            }
        });
    }

    @Test
    public void testCallAsyncEmptyCommands() {
        Assertions.assertFalse(N.callAsync(CommonUtil.<Callable<String>> emptyList()).hasNext());
        Assertions.assertFalse(N.runAsync(CommonUtil.<Throwables.Runnable<? extends Exception>> emptyList()).hasNext());
    }

    // ------------------------------------------------------------------ convert(...) resource ownership

    /**
     * {@code java.sql.Clob} does not implement {@link AutoCloseable}, so a Clob that reaches the generic
     * fall-through must not be handed to the close path. Converting a Clob to {@code byte[]} is such a case:
     * the {@code byte[]} branch only handles Blob and InputStream, so the Clob falls through.
     */
    @Test
    public void testClobReachingFallThroughIsNotTreatedAsCloseable() throws Exception {
        final SerialClob clob = new SerialClob("abc".toCharArray());

        Assertions.assertFalse(clob instanceof AutoCloseable, "precondition: java.sql.Clob is not AutoCloseable");

        // Whatever the conversion does, it must not fail trying to cast the Clob to AutoCloseable.
        try {
            CommonUtil.convert(clob, byte[].class);
        } catch (final ClassCastException e) {
            Assertions.fail("convert() must not cast a Clob to AutoCloseable: " + e);
        } catch (final RuntimeException e) {
            // a normal conversion failure is fine
            Assertions.assertFalse(e instanceof ClassCastException);
        }
    }

    /** A consumed InputStream source is still closed by the conversion, as the contract states. */
    @Test
    public void testConvertClosesConsumedInputStream() {
        final AtomicInteger closed = new AtomicInteger();
        final InputStream is = new ByteArrayInputStream("hello".getBytes()) {
            @Override
            public void close() throws java.io.IOException {
                closed.incrementAndGet();
                super.close();
            }
        };

        Assertions.assertEquals("hello", CommonUtil.convert(is, String.class));
        Assertions.assertEquals(1, closed.get());
    }

    /** An AutoCloseable that is not one of the documented resource types stays open - it is the caller's. */
    @Test
    public void testConvertDoesNotCloseForeignAutoCloseable() {
        final AtomicInteger closed = new AtomicInteger();

        final class Foreign implements AutoCloseable {
            @Override
            public void close() {
                closed.incrementAndGet();
            }

            @Override
            public String toString() {
                return "foreign";
            }
        }

        final Foreign foreign = new Foreign();

        try {
            // java.util.Date is not a String/number/bean/map/collection target, so this reaches the
            // generic fall-through where the old code closed any AutoCloseable.
            CommonUtil.convert(foreign, java.util.Date.class);
        } catch (final RuntimeException e) {
            // conversion may well fail; what matters is that the caller's resource was left alone
        }

        Assertions.assertEquals(0, closed.get(), "convert() must not close a caller-owned AutoCloseable");
    }
}
