package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Array} / {@code Iterables} / {@code Iterators} issues corrected in the
 * fourth 2026-08-31 review pass.
 *
 * <ul>
 *   <li>{@code Array.repeat(element, n, elementClass)} and {@code Array.repeat(a, n, elementClass)} now reject a
 *       {@code null} {@code elementClass} up front, naming the parameter the caller actually passed instead of
 *       leaking the internal {@code componentType} name from {@code Array.newInstance},</li>
 *   <li>{@code Array.random(start, end, len)} uses the fast {@code nextInt(bound)} path for a span of exactly
 *       {@code Integer.MAX_VALUE} - the bound is legal, so only a span that genuinely overflows an {@code int}
 *       needs {@code nextLong},</li>
 *   <li>the 16 {@code box}/{@code unbox} range overloads document that a {@code null} array returns {@code null}
 *       <i>without</i> range validation; these tests pin that behaviour,</li>
 *   <li>{@code Iterables.subSet} documents exactly which sets it rejects - a descending set with a non-degenerate
 *       range - and these tests pin the accepted cases it was previously described as rejecting,</li>
 *   <li>{@code powerSet}/{@code permutations}/{@code orderedPermutations} render their input from
 *       {@code toString()}, now documented,</li>
 *   <li>{@code Iterators.repeat(e, int)} delegates to the {@code long} overload; these tests pin that the
 *       observable behaviour, including the exception messages, is unchanged.</li>
 * </ul>
 */
public class ArrayIterablesIteratorsTest extends TestBase {

    // ==================================================== Array.repeat(..., Class): elementClass validation

    @Test
    public void testRepeatWithElementClass_nullElementClassNamesTheCallersParameter() {
        final IllegalArgumentException scalar = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", 3, (Class<String>) null));
        Assertions.assertTrue(scalar.getMessage().contains("elementClass"), "message should name the caller's parameter, was: " + scalar.getMessage());
        Assertions.assertFalse(scalar.getMessage().contains("componentType"),
                "message must not leak Array.newInstance's parameter name, was: " + scalar.getMessage());
    }

    @Test
    public void testRepeatArrayWithElementClass_nullElementClassRejectedOnBothPaths() {
        // Non-empty input: used to fail later, inside N.newArray.
        final IllegalArgumentException nonEmpty = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Array.repeat(new String[] { "a" }, 3, (Class<String>) null));
        Assertions.assertTrue(nonEmpty.getMessage().contains("elementClass"), nonEmpty.getMessage());
        Assertions.assertFalse(nonEmpty.getMessage().contains("componentType"), nonEmpty.getMessage());

        // Empty input: used to fail in the Array.newInstance(elementClass, 0) short-circuit.
        final IllegalArgumentException empty = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Array.repeat(new String[0], 3, (Class<String>) null));
        Assertions.assertTrue(empty.getMessage().contains("elementClass"), empty.getMessage());
        Assertions.assertFalse(empty.getMessage().contains("componentType"), empty.getMessage());

        // ... and a null input array, which also takes the isEmpty short-circuit.
        final IllegalArgumentException nullArray = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Array.repeat((String[]) null, 3, (Class<String>) null));
        Assertions.assertTrue(nullArray.getMessage().contains("elementClass"), nullArray.getMessage());
    }

    @Test
    public void testRepeatWithElementClass_elementClassCheckedBeforeNegativeCount() {
        // Both are invalid; elementClass is validated first, so that is the reported failure.
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", -1, (Class<String>) null));
        Assertions.assertTrue(e.getMessage().contains("elementClass"), e.getMessage());
    }

    @Test
    public void testRepeatWithElementClass_validArgumentsStillBehaveAsBefore() {
        Assertions.assertArrayEquals(new String[] { "hello", "hello", "hello" }, Array.repeat("hello", 3, String.class));
        Assertions.assertArrayEquals(new String[] { null, null }, Array.repeat((String) null, 2, String.class));
        Assertions.assertArrayEquals(new String[0], Array.repeat("hello", 0, String.class));

        Assertions.assertArrayEquals(new Integer[] { 1, 2, 1, 2 }, Array.repeat(new Integer[] { 1, 2 }, 2, Integer.class));
        Assertions.assertArrayEquals(new Integer[0], Array.repeat(new Integer[0], 5, Integer.class));
        Assertions.assertArrayEquals(new Integer[0], Array.repeat((Integer[]) null, 5, Integer.class));

        // The negative-count contract is unchanged for a valid elementClass.
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.repeat("x", -1, String.class));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.repeat(new String[] { "x" }, -1, String.class));

        // The result's component type comes from elementClass, so a sibling subtype can be stored.
        final Number[] widened = Array.repeat(Integer.valueOf(1), 2, Number.class);
        Assertions.assertEquals(Number[].class, widened.getClass());
        widened[0] = Double.valueOf(1.5);
        Assertions.assertEquals(Double.valueOf(1.5), widened[0]);
    }

    // ==================================================== Array.random: span of exactly Integer.MAX_VALUE

    @Test
    public void testRandomRange_spanOfExactlyIntegerMaxValueStaysInRange() {
        // mod == Integer.MAX_VALUE: this is the boundary that used to fall through to the nextLong path.
        final int[] a = Array.random(0, Integer.MAX_VALUE, 2000);
        Assertions.assertEquals(2000, a.length);

        for (final int v : a) {
            Assertions.assertTrue(v >= 0 && v < Integer.MAX_VALUE, "out of range: " + v);
        }
    }

    @Test
    public void testRandomRange_spanOfExactlyIntegerMaxValueAtTheNegativeEnd() {
        // start + nextInt(bound) must not overflow: MIN_VALUE + [0, MAX_VALUE-1] == [MIN_VALUE, -2].
        final int[] a = Array.random(Integer.MIN_VALUE, -1, 2000);
        Assertions.assertEquals(2000, a.length);

        for (final int v : a) {
            Assertions.assertTrue(v >= Integer.MIN_VALUE && v < -1, "out of range: " + v);
        }
    }

    @Test
    public void testRandomRange_spanWiderThanAnIntStillUsesTheLongPath() {
        // mod == 2^32-2, genuinely wider than an int: the nextLong branch must still be correct.
        final int[] a = Array.random(Integer.MIN_VALUE, Integer.MAX_VALUE, 2000);
        Assertions.assertEquals(2000, a.length);

        for (final int v : a) {
            Assertions.assertTrue(v < Integer.MAX_VALUE, "out of range: " + v);
        }
    }

    @Test
    public void testRandomRange_ordinarySpansAndValidationUnchanged() {
        final int[] a = Array.random(1, 100, 500);
        Assertions.assertEquals(500, a.length);

        for (final int v : a) {
            Assertions.assertTrue(v >= 1 && v < 100, "out of range: " + v);
        }

        // A single-value range is degenerate but legal.
        Assertions.assertArrayEquals(new int[] { 7, 7, 7 }, Array.random(7, 8, 3));

        Assertions.assertEquals(0, Array.random(1, 100, 0).length);
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.random(1, 100, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.random(100, 100, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.random(100, 1, 5));
        Assertions.assertThrows(IllegalArgumentException.class, () -> Array.random(-1));
    }

    // ==================================================== box/unbox: null array skips range validation

    @Test
    public void testBoxRange_nullArrayReturnsNullWithoutRangeValidation() {
        // Documented carve-out: the null check runs before N.checkFromToIndex, so no index is ever rejected.
        Assertions.assertNull(Array.box((boolean[]) null, 5, 10));
        Assertions.assertNull(Array.box((char[]) null, 5, 10));
        Assertions.assertNull(Array.box((byte[]) null, 5, 10));
        Assertions.assertNull(Array.box((short[]) null, 5, 10));
        Assertions.assertNull(Array.box((int[]) null, 5, 10));
        Assertions.assertNull(Array.box((long[]) null, 5, 10));
        Assertions.assertNull(Array.box((float[]) null, 5, 10));
        Assertions.assertNull(Array.box((double[]) null, 5, 10));

        // Even a reversed range, which is invalid for any non-null array.
        Assertions.assertNull(Array.box((int[]) null, 10, 5));
        Assertions.assertNull(Array.box((int[]) null, -3, -1));
    }

    @Test
    public void testUnboxRange_nullArrayReturnsNullWithoutRangeValidation() {
        Assertions.assertNull(Array.unbox((Boolean[]) null, 5, 10, false));
        Assertions.assertNull(Array.unbox((Character[]) null, 5, 10, ' '));
        Assertions.assertNull(Array.unbox((Byte[]) null, 5, 10, (byte) 0));
        Assertions.assertNull(Array.unbox((Short[]) null, 5, 10, (short) 0));
        Assertions.assertNull(Array.unbox((Integer[]) null, 5, 10, 0));
        Assertions.assertNull(Array.unbox((Long[]) null, 5, 10, 0L));
        Assertions.assertNull(Array.unbox((Float[]) null, 5, 10, 0f));
        Assertions.assertNull(Array.unbox((Double[]) null, 5, 10, 0d));

        Assertions.assertNull(Array.unbox((Integer[]) null, 10, 5, 0));
    }

    @Test
    public void testBoxUnboxRange_nonNullArrayStillValidatesTheRange() {
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new int[] { 1, 2, 3 }, 0, 5));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new int[] { 1, 2, 3 }, -1, 2));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new int[] { 1, 2, 3 }, 2, 1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(new Integer[] { 1, 2, 3 }, 0, 5, 0));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.unbox(new Integer[] { 1, 2, 3 }, 2, 1, 0));

        // ... and an empty array is still a real array, so any non-empty range on it is rejected.
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> Array.box(new int[0], 0, 1));

        Assertions.assertArrayEquals(new Integer[] { 2, 3 }, Array.box(new int[] { 1, 2, 3 }, 1, 3));
        Assertions.assertArrayEquals(new int[] { 2, 0 }, Array.unbox(new Integer[] { 1, 2, null }, 1, 3, 0));
    }

    // ==================================================== Iterables.subSet: which orderings are accepted

    @Test
    public void testSubSet_descendingSetAcceptsADegenerateRange() {
        final NavigableSet<Integer> desc = new TreeSet<>(Comparator.reverseOrder());
        desc.addAll(Arrays.asList(1, 2, 3, 4, 5));

        // compare(3, 3) == 0, so the endpoints are not "ordered oppositely" and the call succeeds.
        Assertions.assertEquals(Arrays.asList(3), new ArrayList<>(Iterables.subSet(desc, Range.closed(3, 3))));
    }

    @Test
    public void testSubSet_descendingSetRejectsANonDegenerateRange() {
        final NavigableSet<Integer> desc = new TreeSet<>(Comparator.reverseOrder());
        desc.addAll(Arrays.asList(1, 2, 3, 4, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.subSet(desc, Range.closed(2, 4)));

        // The documented workaround: call the NavigableSet method directly with the endpoints swapped.
        Assertions.assertEquals(Arrays.asList(4, 3, 2), new ArrayList<>(desc.subSet(4, true, 2, true)));
    }

    @Test
    public void testSubSet_descendingViewOfANaturalSetBehavesTheSameWay() {
        final NavigableSet<Integer> natural = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));
        final NavigableSet<Integer> desc = natural.descendingSet();

        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.subSet(desc, Range.closed(2, 4)));
        Assertions.assertEquals(Arrays.asList(3), new ArrayList<>(Iterables.subSet(desc, Range.closed(3, 3))));
    }

    @Test
    public void testSubSet_nonNaturalButConsistentComparatorIsAccepted() {
        // Ordered by length, which is not the natural order of String, but which agrees with the
        // range's endpoints - so this is accepted, contrary to "any non-natural set is rejected".
        final NavigableSet<String> byLength = new TreeSet<>(Comparator.comparingInt(String::length).thenComparing(Comparator.naturalOrder()));
        byLength.addAll(Arrays.asList("a", "bb", "ccc", "dddd"));

        Assertions.assertEquals(Arrays.asList("a", "bb", "ccc"), new ArrayList<>(Iterables.subSet(byLength, Range.closed("a", "ccc"))));
    }

    @Test
    public void testSubSet_naturalOrderingCasesUnchanged() {
        final NavigableSet<Integer> set = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5));

        Assertions.assertEquals(Arrays.asList(2, 3), new ArrayList<>(Iterables.subSet(set, Range.closedOpen(2, 4))));
        Assertions.assertEquals(Arrays.asList(2, 3, 4), new ArrayList<>(Iterables.subSet(set, Range.closed(2, 4))));
        Assertions.assertEquals(Arrays.asList(3), new ArrayList<>(Iterables.subSet(set, Range.open(2, 4))));
        Assertions.assertEquals(Arrays.asList(3, 4), new ArrayList<>(Iterables.subSet(set, Range.openClosed(2, 4))));
        Assertions.assertTrue(Iterables.subSet((NavigableSet<Integer>) null, Range.closed(2, 4)).isEmpty());
        Assertions.assertThrows(IllegalArgumentException.class, () -> Iterables.subSet(set, (Range<Integer>) null));
    }

    // ==================================================== lazy-view toString() of the combinatorial results

    @Test
    public void testPowerSetToStringRendersTheInputNotTheSubsets() {
        final Set<Integer> input = new LinkedHashSet<>(Arrays.asList(1, 2));
        final Set<Set<Integer>> ps = Iterables.powerSet(input);

        Assertions.assertEquals("powerSet([1, 2])", ps.toString());
        Assertions.assertEquals(4, ps.size());
        // Materialising is what actually renders the members.
        Assertions.assertEquals(4, new ArrayList<>(ps).size());
    }

    @Test
    public void testPermutationsToStringRendersTheInput() {
        Assertions.assertEquals("permutations([1, 2, 3])", Iterables.permutations(Arrays.asList(1, 2, 3)).toString());
        Assertions.assertEquals(6, new ArrayList<>(Iterables.permutations(Arrays.asList(1, 2, 3))).size());
    }

    @Test
    public void testOrderedPermutationsToStringRendersTheSortedInput() {
        // Both overloads: the input is sorted first, so toString shows the sorted form.
        Assertions.assertEquals("orderedPermutations([1, 2, 3])", Iterables.orderedPermutations(Arrays.asList(3, 1, 2)).toString());
        Assertions.assertEquals("orderedPermutations([1, 2, 3])",
                Iterables.orderedPermutations(Arrays.asList(3, 1, 2), Comparator.<Integer> naturalOrder()).toString());
        Assertions.assertEquals(6, new ArrayList<>(Iterables.orderedPermutations(Arrays.asList(3, 1, 2))).size());
    }

    @Test
    public void testCartesianProductToStringStillRendersTheAxes() {
        // The already-documented sibling, pinned so the four now read alike.
        Assertions.assertEquals("cartesianProduct([[1, 2], [A, B]])", Iterables.cartesianProduct(Arrays.asList(1, 2), Arrays.asList("A", "B")).toString());
    }

    // ==================================================== Iterators.repeat(e, int) delegation

    @Test
    public void testRepeatIntOverloadIsUnchangedByTheDelegation() {
        final List<String> out = new ArrayList<>();
        Iterators.repeat("Hello", 3).forEachRemaining(out::add);
        Assertions.assertEquals(Arrays.asList("Hello", "Hello", "Hello"), out);

        Assertions.assertFalse(Iterators.repeat("x", 0).hasNext());
        Assertions.assertFalse(Iterators.repeat((String) null, 0).hasNext());

        // null elements are allowed and repeated.
        final List<String> nulls = new ArrayList<>();
        Iterators.repeat((String) null, 2).forEachRemaining(nulls::add);
        Assertions.assertEquals(Arrays.asList(null, null), nulls);
    }

    @Test
    public void testRepeatIntAndLongOverloadsAgreeExactly() {
        for (final int n : new int[] { 0, 1, 2, 5 }) {
            final List<Integer> viaInt = new ArrayList<>();
            Iterators.repeat(7, n).forEachRemaining(viaInt::add);

            final List<Integer> viaLong = new ArrayList<>();
            Iterators.repeat(7, (long) n).forEachRemaining(viaLong::add);

            Assertions.assertEquals(viaLong, viaInt, "mismatch at n=" + n);
            Assertions.assertEquals(n, viaInt.size());
        }
    }

    @Test
    public void testRepeatIntOverloadExhaustionAndValidationUnchanged() {
        final Iterator<String> iter = Iterators.repeat("a", 1);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("a", iter.next());
        Assertions.assertFalse(iter.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, iter::next);

        // The int and long overloads must still report a negative count identically.
        final IllegalArgumentException viaInt = Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -5));
        final IllegalArgumentException viaLong = Assertions.assertThrows(IllegalArgumentException.class, () -> Iterators.repeat("a", -5L));
        Assertions.assertEquals(viaLong.getMessage(), viaInt.getMessage());
        Assertions.assertTrue(viaInt.getMessage().contains("'n'"), viaInt.getMessage());
    }

    // ==================================================== concat2D/concat3D null-row contract (documented)

    @Test
    public void testGenericConcatPropagatesNullRowsWhilePrimitiveConcatNormalisesThem() {
        // The documented, deliberate divergence the review flagged: pinned here so it cannot drift silently.
        Assertions.assertArrayEquals(new int[][] { { 1, 2 }, {} }, Array.concat(new int[][] { { 1 }, null }, new int[][] { { 2 } }));

        final String[][] generic = Array.concat2D(new String[][] { { "1" }, null }, new String[][] { { "2" } });
        Assertions.assertArrayEquals(new String[] { "1", "2" }, generic[0]);
        Assertions.assertNull(generic[1]);

        Assertions.assertNull(Array.concat2D((String[][]) null, (String[][]) null));
        Assertions.assertNull(Array.concat3D((String[][][]) null, (String[][][]) null));
    }

    @Test
    public void testGenericConcat2DAnd3DResolveIndependently() {
        // The javadoc previously claimed concat(T[][],T[][]) and concat(T[][][],T[][][]) would be ambiguous.
        // They are not; these two calls pick different, correctly-shaped overloads.
        final String[][] two = Array.concat2D(new String[][] { { "a" } }, new String[][] { { "b" } });
        final String[][][] three = Array.concat3D(new String[][][] { { { "a" } } }, new String[][][] { { { "b" } } });

        Assertions.assertArrayEquals(new String[] { "a", "b" }, two[0]);
        Assertions.assertArrayEquals(new String[] { "a", "b" }, three[0][0]);
    }
}
