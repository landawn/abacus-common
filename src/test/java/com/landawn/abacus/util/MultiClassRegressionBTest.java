package com.landawn.abacus.util;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.TreeSet;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the behaviour corrected in the 2026-08-30 line-by-line review of
 * {@code CommonUtil}/{@code N}:
 *
 * <ul>
 *   <li>{@code tryOrDefaultIfExceptionOccurred(..., R)} accepts a default that is not {@link Comparable},</li>
 *   <li>{@code copyOfRange(..., step)} rejects an out-of-range {@code fromIndex} for a descending range instead of
 *       silently clamping it and returning a shorter result,</li>
 *   <li>{@code probeUnmodifiable} answers from the statically registered classification instead of probe-mutating a
 *       container whose class is already known,</li>
 *   <li>{@code reverse}/{@code rotate}/{@code shuffle} on a non-{@code List} {@code Collection} restore the original
 *       content when re-populating fails, rather than leaving the collection emptied,</li>
 *   <li>{@code format(String, Object...)} returns its pooled buffer even when an argument's {@code toString()} throws,</li>
 *   <li>{@code getDescendingIteratorIfPossible} finds an inherited {@code descendingIterator()}.</li>
 * </ul>
 *
 * It also pins the {@code null}-vs-empty and numeric-narrowing contracts that the review documented.
 */
public class MultiClassRegressionBTest extends TestBase {

    // ------------------------------------------------------------------ tryOrDefaultIfExceptionOccurred

    /** A default value that deliberately does not implement {@link Comparable}. */
    private static final class NotComparable {
        private final String name;

        NotComparable(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred_callable_acceptsNonComparableDefault() {
        final NotComparable fallback = new NotComparable("fallback");
        final NotComparable value = new NotComparable("value");

        // Previously the <R extends Comparable<? super R>> bound made this call fail to compile.
        Assertions.assertSame(value, N.tryOrDefaultIfExceptionOccurred(() -> value, fallback));
        Assertions.assertSame(fallback, N.tryOrDefaultIfExceptionOccurred((Callable<NotComparable>) () -> {
            throw new RuntimeException("boom");
        }, fallback));
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred_function_acceptsNonComparableDefault() {
        final NotComparable fallback = new NotComparable("fallback");

        Assertions.assertEquals("HELLO", N.tryOrDefaultIfExceptionOccurred("hello", s -> new NotComparable(s.toUpperCase()), fallback).toString());
        Assertions.assertSame(fallback, N.tryOrDefaultIfExceptionOccurred("hello", s -> {
            throw new RuntimeException("boom");
        }, fallback));
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred_collectionDefaultStillCompiles() {
        final List<String> fallback = Arrays.asList("a", "b");

        Assertions.assertSame(fallback, N.tryOrDefaultIfExceptionOccurred((Callable<List<String>>) () -> {
            throw new IllegalStateException("boom");
        }, fallback));
    }

    @Test
    public void testTryOrDefaultIfExceptionOccurred_supplierOverloadStillSelected() {
        // The Supplier-taking overload must still win when a Supplier is passed.
        final java.util.function.Supplier<String> supplier = () -> "from-supplier";

        Assertions.assertEquals("from-supplier", N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> {
            throw new RuntimeException("boom");
        }, supplier));
        Assertions.assertEquals("ok", N.tryOrDefaultIfExceptionOccurred((Callable<String>) () -> "ok", supplier));
    }

    // ------------------------------------------------------------------ copyOfRange(..., step) descending bounds

    @Test
    public void testCopyOfRangeWithNegativeStep_rejectsOutOfRangeFromIndex() {
        final int[] a = { 0, 1, 2, 3, 4 };

        // fromIndex == a.length is not a valid element index for a descending range. It used to be clamped to
        // a.length - 1, which silently returned [4, 3] instead of reporting the bad index.
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, 5, 2, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, 5, -1, -1));
    }

    @Test
    public void testCopyOfRangeWithNegativeStep_validDescendingRangesUnchanged() {
        final int[] a = { 0, 1, 2, 3, 4 };

        Assertions.assertArrayEquals(new int[] { 4, 3, 2 }, CommonUtil.copyOfRange(a, 4, 1, -1));
        Assertions.assertArrayEquals(new int[] { 4, 3, 2, 1, 0 }, CommonUtil.copyOfRange(a, 4, -1, -1));
        Assertions.assertArrayEquals(new int[] { 4, 2, 0 }, CommonUtil.copyOfRange(a, 4, -1, -2));
        Assertions.assertArrayEquals(new int[] {}, CommonUtil.copyOfRange(a, 2, 2, -1));
        // ascending is untouched
        Assertions.assertArrayEquals(new int[] { 0, 2, 4 }, CommonUtil.copyOfRange(a, 0, 5, 2));
    }

    @Test
    public void testCopyOfRangeWithNegativeStep_allPrimitiveOverloadsAgree() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new boolean[] { true, false }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new char[] { 'a', 'b' }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new byte[] { 1, 2 }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new short[] { 1, 2 }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new int[] { 1, 2 }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new long[] { 1L, 2L }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new float[] { 1f, 2f }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new double[] { 1d, 2d }, 2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(new String[] { "a", "b" }, 2, 0, -1));
    }

    @Test
    public void testCopyOfRangeWithNegativeStep_listAndStringOverloads() {
        final List<Integer> randomAccess = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
        final List<Integer> sequential = new java.util.LinkedList<>(randomAccess);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(randomAccess, 5, 2, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(sequential, 5, 2, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange("abcde", 5, 2, -1));

        Assertions.assertEquals(Arrays.asList(4, 2, 0), CommonUtil.copyOfRange(randomAccess, 4, -1, -2));
        Assertions.assertEquals(Arrays.asList(4, 2, 0), CommonUtil.copyOfRange(sequential, 4, -1, -2));
        Assertions.assertEquals("edc", CommonUtil.copyOfRange("abcde", 4, 1, -1));
    }

    // ------------------------------------------------------------------ probeUnmodifiable

    @Test
    public void testProbeUnmodifiable_knownModifiableClassIsNotMutated() {
        final List<String> list = new ArrayList<>(Arrays.asList("a", "b"));
        // A live iterator fails fast on any structural change, so it detects the probe's add/remove pair.
        final Iterator<String> iter = list.iterator();

        Assertions.assertFalse(CommonUtil.probeUnmodifiable(list));

        Assertions.assertEquals("a", iter.next());
        Assertions.assertEquals("b", iter.next());
        Assertions.assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testProbeUnmodifiable_knownModifiableMapIsNotMutated() {
        final java.util.Map<String, String> map = new java.util.HashMap<>();
        map.put("k", "v");
        final Iterator<java.util.Map.Entry<String, String>> iter = map.entrySet().iterator();

        Assertions.assertFalse(CommonUtil.probeUnmodifiable(map));

        Assertions.assertEquals("k", iter.next().getKey());
        Assertions.assertEquals(1, map.size());
    }

    @Test
    public void testProbeUnmodifiable_classificationUnchanged() {
        // The no-mutation shortcut must not change any answer. In particular Arrays.asList(...) is deliberately
        // NOT treated as known-mutable: it rejects add(), so the documented add-only heuristic still reports it
        // as unmodifiable (see CommonUtilTest#testUnmodifiableNotPoisonedByProbeUnmodifiableProbe).
        Assertions.assertTrue(CommonUtil.probeUnmodifiable(Arrays.asList("a", "b")));

        Assertions.assertTrue(CommonUtil.probeUnmodifiable(List.of("a", "b")));
        Assertions.assertTrue(CommonUtil.probeUnmodifiable((Collection<?>) null));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new ArrayList<>()));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new TreeSet<>()));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new java.util.LinkedList<>()));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new LinkedHashSet<>()));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new java.util.HashMap<>()));
        Assertions.assertFalse(CommonUtil.probeUnmodifiable(new java.util.TreeMap<>()));
    }

    // ------------------------------------------------------------------ reverse/rotate/shuffle rollback

    /** A non-{@code List} collection whose {@code add} fails once, {@code failAfter} successful adds in. */
    private static final class FailOnceSet<T> extends LinkedHashSet<T> {
        private static final long serialVersionUID = 1L;

        private int failAfter = -1;

        FailOnceSet(final Collection<T> initial) {
            // NOT super(initial): the superclass constructor would call the overridden add(...) before this
            // class's field initializers have run, so failAfter would still be 0 and would fail immediately.
            addAll(initial);
        }

        public void armAfter(final int successfulAdds) {
            failAfter = successfulAdds;
        }

        @Override
        public boolean add(final T e) {
            if (failAfter == 0) {
                failAfter = -1; // fail exactly once so the rollback itself can succeed
                throw new IllegalStateException("rejected: " + e);
            }

            if (failAfter > 0) {
                failAfter--;
            }

            return super.add(e);
        }
    }

    @Test
    public void testReverseCollection_restoresContentWhenRepopulationFails() {
        final FailOnceSet<String> set = new FailOnceSet<>(Arrays.asList("a", "b", "c"));
        set.armAfter(2);

        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.reverse(set));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));
    }

    @Test
    public void testRotateCollection_restoresContentWhenRepopulationFails() {
        final FailOnceSet<String> set = new FailOnceSet<>(Arrays.asList("a", "b", "c"));
        set.armAfter(2);

        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.rotate(set, 1));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));
    }

    @Test
    public void testShuffleCollection_restoresContentWhenRepopulationFails() {
        final FailOnceSet<String> set = new FailOnceSet<>(Arrays.asList("a", "b", "c"));
        set.armAfter(2);

        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.shuffle(set));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set));

        final FailOnceSet<String> set2 = new FailOnceSet<>(Arrays.asList("a", "b", "c"));
        set2.armAfter(2);

        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.shuffle(set2, new Random(1)));
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(set2));
    }

    @Test
    public void testReverseCollection_restoreFailureIsSuppressedOntoTheOriginal() {
        // When the rollback fails too, the original failure still propagates and carries the restore failure.
        final LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c")) {
            private static final long serialVersionUID = 1L;

            private boolean poisoned = false;

            @Override
            public boolean add(final String e) {
                if (poisoned) {
                    throw new IllegalStateException("always rejected: " + e);
                }

                return super.add(e);
            }

            /** The first clear() is the one replaceElements(...) performs, so arm from there. */
            @Override
            public void clear() {
                super.clear();
                poisoned = true;
            }
        };

        final IllegalStateException ex = Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.reverse(set));
        Assertions.assertTrue(ex.getMessage().startsWith("always rejected"));
        Assertions.assertEquals(1, ex.getSuppressed().length);
        Assertions.assertInstanceOf(IllegalStateException.class, ex.getSuppressed()[0]);
    }

    @Test
    public void testCopyOfRangeWithNegativeStep_objectArrayWithExplicitNewType() {
        final Object[] a = { "a", "b", "c" };

        Assertions.assertArrayEquals(new String[] { "c", "a" }, CommonUtil.copyOfRange(a, 2, -1, -2, String[].class));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.copyOfRange(a, 3, 0, -1, String[].class));
    }

    @Test
    public void testCheckArgument_throwingToStringInTheSurplusArgumentPosition() {
        // exercises the second toString(...) loop, which also runs while the pooled buffer is checked out
        Assertions.assertThrows(IllegalStateException.class, () -> CommonUtil.checkArgument(false, "{}", new Object[] { "only", new ExplodingToString() }));

        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkArgument(false, "{}", new Object[] { "only", "extra" }));
        Assertions.assertEquals("only [extra]", ex.getMessage());
    }

    @Test
    public void testReverseAndRotateCollection_stillWorkOnTheHappyPath() {
        final Collection<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));
        CommonUtil.reverse(set);
        Assertions.assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(set));

        CommonUtil.rotate(set, 1);
        Assertions.assertEquals(Arrays.asList("a", "c", "b"), new ArrayList<>(set));

        final Collection<String> toShuffle = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        CommonUtil.shuffle(toShuffle, new Random(42));
        Assertions.assertEquals(4, toShuffle.size());
        Assertions.assertTrue(toShuffle.containsAll(Arrays.asList("a", "b", "c", "d")));
    }

    // ------------------------------------------------------------------ format(...) buffer recycling

    /** An argument whose {@code toString()} blows up while the pooled buffer is checked out. */
    private static final class ExplodingToString {
        @Override
        public String toString() {
            throw new IllegalStateException("toString blew up");
        }
    }

    @Test
    public void testCheckArgument_throwingArgumentToStringDoesNotCorruptTheBufferPool() {
        Assertions.assertThrows(IllegalStateException.class,
                () -> CommonUtil.checkArgument(false, "{} and {}", new Object[] { "first", new ExplodingToString() }));

        // The pooled StringBuilder must have been returned; a following format must be clean, not a
        // continuation of the half-built message above.
        final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.checkArgument(false, "{} and {}", new Object[] { "x", "y" }));
        Assertions.assertEquals("x and y", ex.getMessage());
    }

    // ------------------------------------------------------------------ descendingIterator lookup

    /** A {@code TreeSet} subclass: {@code descendingIterator()} is inherited, not declared. */
    private static final class InheritingTreeSet<T> extends TreeSet<T> {
        private static final long serialVersionUID = 1L;

        InheritingTreeSet(final Collection<T> initial) {
            super(initial);
        }
    }

    @Test
    public void testLastElement_findsInheritedDescendingIterator() {
        final InheritingTreeSet<String> set = new InheritingTreeSet<>(Arrays.asList("b", "a", "c"));

        // A declared-methods-only lookup missed the inherited TreeSet.descendingIterator().
        Assertions.assertEquals("c", CommonUtil.lastElement(set).orElse(null));
        Assertions.assertEquals("c", CommonUtil.lastOrNullIfEmpty(set));
        // repeated calls must keep working once the lookup is cached
        Assertions.assertEquals("c", CommonUtil.lastElement(set).orElse(null));
        Assertions.assertEquals(2, CommonUtil.lastIndexOf(set, "c"));
    }

    @Test
    public void testLastElement_collectionWithoutDescendingIterator() {
        final Collection<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        Assertions.assertEquals("c", CommonUtil.lastElement(set).orElse(null));
        Assertions.assertEquals("c", CommonUtil.lastElement(set).orElse(null));
        Assertions.assertFalse(CommonUtil.lastElement(new LinkedHashSet<String>()).isPresent());
    }

    // ------------------------------------------------------------------ firstElements/lastElements validation

    @Test
    public void testFirstAndLastElements_rejectNegativeCount() {
        final List<String> list = Arrays.asList("a", "b");

        for (final org.junit.jupiter.api.function.Executable e : new org.junit.jupiter.api.function.Executable[] { //
                () -> CommonUtil.firstElements(new String[] { "a" }, -1), //
                () -> CommonUtil.firstElements(list, -1), //
                () -> CommonUtil.firstElements(list.iterator(), -1), //
                () -> CommonUtil.lastElements(new String[] { "a" }, -1), //
                () -> CommonUtil.lastElements(list, -1), //
                () -> CommonUtil.lastElements(list.iterator(), -1) }) {
            final IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, e);
            Assertions.assertEquals("'n' cannot be negative: -1", ex.getMessage());
        }

        Assertions.assertEquals(Arrays.asList("a"), CommonUtil.firstElements(list, 1));
        Assertions.assertEquals(Arrays.asList("b"), CommonUtil.lastElements(list, 1));
    }

    // ------------------------------------------------------------------ insert(String, int, String)

    @Test
    public void testInsertIntoString() {
        Assertions.assertEquals("xabc", N.insert("abc", 0, "x"));
        Assertions.assertEquals("abcx", N.insert("abc", 3, "x"));
        Assertions.assertEquals("axbc", N.insert("abc", 1, "x"));
        Assertions.assertEquals("abc", N.insert("abc", 1, ""));
        Assertions.assertEquals("abc", N.insert("abc", 1, null));
        Assertions.assertEquals("x", N.insert((String) null, 0, "x"));
        Assertions.assertEquals("", N.insert((String) null, 0, (String) null));
    }

    // ------------------------------------------------------------------ documented null-vs-empty split

    @Test
    public void testArrayNullVsEmpty_equalsDiffersFromCompareAndMismatch() {
        // equals follows Arrays.equals: null is not an empty array.
        Assertions.assertFalse(CommonUtil.equals((Object[]) null, new Object[0]));
        Assertions.assertFalse(CommonUtil.equals((int[]) null, new int[0]));
        Assertions.assertTrue(CommonUtil.equals((Object[]) null, (Object[]) null));

        // compare/mismatch/containsSameElements treat null and empty as interchangeable.
        Assertions.assertEquals(0, CommonUtil.compare((String[]) null, new String[0]));
        Assertions.assertEquals(0, CommonUtil.compare((int[]) null, new int[0]));
        Assertions.assertEquals(-1, CommonUtil.mismatch((int[]) null, new int[0]));
        Assertions.assertTrue(CommonUtil.containsSameElements((int[]) null, new int[0]));
    }

    // ------------------------------------------------------------------ documented numeric narrowing

    @Test
    public void testToIntArray_outOfRangeNumbersWrapRatherThanThrow() {
        // Number.intValue() keeps only the low-order bits, so an out-of-range BigDecimal/BigInteger
        // produces an arbitrary in-range value instead of throwing.
        Assertions.assertArrayEquals(new int[] { 0 }, CommonUtil.toIntArray(Arrays.asList(new BigDecimal("1E+400"))));
        Assertions.assertArrayEquals(new int[] { 0 }, CommonUtil.toIntArray(Arrays.asList(BigInteger.ONE.shiftLeft(70))));
        Assertions.assertArrayEquals(new long[] { 0L }, CommonUtil.toLongArray(Arrays.asList(new BigDecimal("1E+400"))));

        // convert(...) is the strict counterpart named by the javadoc.
        Assertions.assertThrows(ArithmeticException.class, () -> CommonUtil.convert(BigInteger.ONE.shiftLeft(70), int.class));
    }

    @Test
    public void testToFloatAndDoubleArray_outOfRangeNumbersSaturate() {
        Assertions.assertArrayEquals(new float[] { Float.POSITIVE_INFINITY }, CommonUtil.toFloatArray(Arrays.asList(new BigDecimal("1E+400"))));
        Assertions.assertArrayEquals(new double[] { Double.POSITIVE_INFINITY }, CommonUtil.toDoubleArray(Arrays.asList(new BigDecimal("1E+400"))));
    }

    // ------------------------------------------------------------------ unmodifiableCollection (now documented)

    @Test
    public void testUnmodifiableCollection() {
        Assertions.assertTrue(CommonUtil.unmodifiableCollection(null).isEmpty());

        final Collection<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        final Collection<String> view = CommonUtil.unmodifiableCollection(source);

        Assertions.assertEquals(Arrays.asList("a", "b"), new ArrayList<>(view));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> view.add("c"));

        // an already-unmodifiable instance is handed back as-is rather than double-wrapped
        final Collection<String> immutable = ImmutableList.of("a");
        Assertions.assertSame(immutable, CommonUtil.unmodifiableCollection(immutable));
    }

    // ------------------------------------------------------------------ helper equality guard

    @Test
    public void testFailOnceSetHelperBehavesAsExpected() {
        final FailOnceSet<String> set = new FailOnceSet<>(Arrays.asList("a"));
        set.armAfter(1);

        Assertions.assertTrue(set.add("b"));
        Assertions.assertThrows(IllegalStateException.class, () -> set.add("c"));
        Assertions.assertTrue(set.add("c"));
        Assertions.assertTrue(Objects.equals(Arrays.asList("a", "b", "c"), new ArrayList<>(set)));
    }
}
