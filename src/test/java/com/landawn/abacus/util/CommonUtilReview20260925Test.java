package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.testfixture.CommonUtilReviewFixtures;

/**
 * Pins for the CommonUtil fixes of the 2026-09-25 review of the uncommitted r9620 tree (findings U04-01/U05-02, U04-03, U05-01,
 * U05-03, U05-05, U06-01, U06-02, U06-05, U06-06, U20-02). Every method name carries its finding ID.
 */
public class CommonUtilReview20260925Test extends TestBase {

    private static <T> com.landawn.abacus.type.Type<T> type(final TypeReference<T> ref) {
        return ref.type();
    }

    // ============================================================ U04-01 / U05-02: immutable sorted targets keep the comparator

    @Test
    public void testU0401_immutableSortedTargetsKeepSourceComparator() {
        final TreeSet<String> rev = new TreeSet<>(Comparator.reverseOrder());
        rev.addAll(Arrays.asList("a", "b", "c"));

        final ImmutableSortedSet<String> iss = CommonUtil.convert(rev, type(new TypeReference<ImmutableSortedSet<String>>() {
        }));
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(iss));
        assertSame(rev.comparator(), iss.comparator());

        final ImmutableNavigableSet<String> ins = CommonUtil.convert(rev, type(new TypeReference<ImmutableNavigableSet<String>>() {
        }));
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(ins));
        assertSame(rev.comparator(), ins.comparator());
        assertEquals("c", ins.first());

        // the mutable sibling behaved this way already (C-172); the Class-token overload takes the same path
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(CommonUtil.convert(rev, type(new TypeReference<TreeSet<String>>() {
        }))));
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(CommonUtil.convert(rev, ImmutableSortedSet.class)));

        final TreeMap<String, Object> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ci.put("b", "2");
        ci.put("A", "1");
        final ImmutableSortedMap<String, Integer> ism = CommonUtil.convert(ci, type(new TypeReference<ImmutableSortedMap<String, Integer>>() {
        }));
        assertSame(String.CASE_INSENSITIVE_ORDER, ism.comparator());
        assertEquals(Arrays.asList("A", "b"), new ArrayList<>(ism.keySet()));
        assertEquals(Integer.valueOf(1), ism.get("a")); // looked up through the kept comparator

        final ImmutableNavigableMap<String, Integer> inm = CommonUtil.convert(ci, type(new TypeReference<ImmutableNavigableMap<String, Integer>>() {
        }));
        assertSame(String.CASE_INSENSITIVE_ORDER, inm.comparator());
        assertEquals(Integer.valueOf(2), inm.get("B"));
        assertEquals("b", inm.lastKey());

        // a converted key/element type cannot be compared by the source comparator: natural order, as for the mutable targets
        final TreeMap<String, String> revMap = new TreeMap<>(Comparator.reverseOrder());
        revMap.put("1", "x");
        revMap.put("2", "y");
        final ImmutableSortedMap<Integer, String> natural = CommonUtil.convert(revMap, type(new TypeReference<ImmutableSortedMap<Integer, String>>() {
        }));
        assertNull(natural.comparator());
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(natural.keySet()));

        final TreeSet<String> revNums = new TreeSet<>(Comparator.reverseOrder());
        revNums.addAll(Arrays.asList("1", "2"));
        final ImmutableSortedSet<Integer> naturalSet = CommonUtil.convert(revNums, type(new TypeReference<ImmutableSortedSet<Integer>>() {
        }));
        assertNull(naturalSet.comparator());
        assertEquals(Arrays.asList(1, 2), new ArrayList<>(naturalSet));

        // an unsorted source still gets natural order; an empty sorted source keeps its comparator
        final ImmutableSortedSet<String> fromList = CommonUtil.convert(Arrays.asList("b", "a"), type(new TypeReference<ImmutableSortedSet<String>>() {
        }));
        assertNull(fromList.comparator());
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(fromList));

        final TreeSet<String> emptyRev = new TreeSet<>(Comparator.reverseOrder());
        final ImmutableSortedSet<String> emptyOut = CommonUtil.convert(emptyRev, type(new TypeReference<ImmutableSortedSet<String>>() {
        }));
        assertTrue(emptyOut.isEmpty());
        assertSame(emptyRev.comparator(), emptyOut.comparator());
    }

    // ============================================================ U04-03: delta goes through checkArgNotNegative

    @Test
    public void testU0403_deltaIsRejectedByTheHouseValidator() {
        assertEquals("'delta' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[] { 1 }, new float[] { 1 }, -1f)).getMessage());
        assertEquals("'delta' cannot be NaN",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[0], new float[0], Float.NaN)).getMessage());
        assertEquals("'delta' cannot be negative: -1.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[] { 1 }, new double[] { 1 }, -1d)).getMessage());
        assertEquals("'delta' cannot be NaN",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[0], new double[0], Double.NaN)).getMessage());
        assertEquals("'delta' cannot be negative: -0.5",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[] { 1 }, 0, new float[] { 1 }, 0, 1, -0.5f)).getMessage());
        assertEquals("'delta' cannot be NaN",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new float[] { 1 }, 0, new float[] { 1 }, 0, 1, Float.NaN)).getMessage());
        assertEquals("'delta' cannot be negative: -2.0",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[] { 1 }, 0, new double[] { 1 }, 0, 1, -2d)).getMessage());
        assertEquals("'delta' cannot be NaN",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(new double[] { 1 }, 0, new double[] { 1 }, 0, 1, Double.NaN)).getMessage());

        // delta is still checked before the arrays are looked at (null arrays, identical arrays)
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals((float[]) null, null, -1f));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals((double[]) null, null, Double.NaN));
        final double[] same = { 1d };
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.equals(same, same, -1d));
        // and the range overloads still validate the indices first
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(new double[] { 1 }, 0, new double[] { 1 }, 0, 2, -1d));
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.equals(new float[] { 1 }, 1, new float[] { 1 }, 0, 1, Float.NaN));

        // -0.0 is not negative (accepted before and after); ordinary deltas work
        assertTrue(CommonUtil.equals(new float[] { 1 }, new float[] { 1 }, -0f));
        assertTrue(CommonUtil.equals(new double[] { 1 }, new double[] { 1 }, -0d));
        assertTrue(CommonUtil.equals(new float[] { 1 }, new float[] { 1.01f }, 0.02f));
        assertTrue(CommonUtil.equals(new double[] { 1, 2 }, 1, new double[] { 2 }, 0, 1, 0d));
    }

    // ============================================================ U05-01: arraySupplier returning null is an NPE

    @Test
    public void testU0501_arraySupplierReturningNullIsNPE() {
        final String msg = "arraySupplier returned null";
        final IntFunction<String[]> nul = n -> null;

        // 2-arg overload: empty/null input (was a silent null result) and non-empty input (was a raw NPE)
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(Collections.<String> emptyList(), nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray((Collection<String>) null, nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(List.of("a"), nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b")), nul)).getMessage());

        // 4-arg overload: empty input, full range (Collection.toArray site), List sub-range (subList site), non-List sub-range
        // (manual-copy site) and an empty sub-range of a non-List (manual-copy site with size 0)
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(Collections.<String> emptyList(), 0, 0, nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray((Collection<String>) null, 0, 0, nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(List.of("a", "b"), 0, 2, nul)).getMessage());
        assertEquals(msg, assertThrows(NullPointerException.class, () -> CommonUtil.toArray(List.of("a", "b", "c"), 1, 3, nul)).getMessage());
        assertEquals(msg,
                assertThrows(NullPointerException.class, () -> CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 3, nul)).getMessage());
        assertEquals(msg,
                assertThrows(NullPointerException.class, () -> CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 1, nul)).getMessage());

        // a null supplier is still reported by its parameter name, and a bad range still comes first
        assertEquals("'arraySupplier' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.toArray(List.of("a"), (IntFunction<String[]>) null)).getMessage());
        assertThrows(IndexOutOfBoundsException.class, () -> CommonUtil.toArray(List.of("a"), 0, 2, nul));

        // happy paths unchanged, including the Collection.toArray(T[]) contract of the manual-copy branch
        assertArrayEquals(new String[0], CommonUtil.toArray(Collections.<String> emptyList(), String[]::new));
        assertArrayEquals(new String[] { "a", "b" }, CommonUtil.toArray(List.of("a", "b"), String[]::new));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(List.of("a", "b", "c"), 1, 3, String[]::new));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 3, String[]::new));
        assertArrayEquals(new String[] { "b", "c" }, CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 3, n -> new String[0]));
        assertArrayEquals(new String[] { "b", "c", null, null },
                CommonUtil.toArray(new LinkedHashSet<>(List.of("a", "b", "c")), 1, 3, n -> new String[4]));
    }

    // ============================================================ U05-03: integral values too large for a long parse as text (doc pin)

    @Test
    public void testU0503_integralValueTooLargeForLongParsesAsText() {
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(BigInteger.TWO.pow(70), Character.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(BigInteger.TWO.pow(70), char.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(new BigDecimal("1E+20"), Character.class));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(1e19, Character.class));
        // values that fit a long are range-checked as documented
        assertEquals(Character.valueOf('A'), CommonUtil.convert(BigInteger.valueOf(65), Character.class));
        assertEquals(Character.valueOf('A'), CommonUtil.convert(new BigDecimal("65"), Character.class));
        assertEquals(Character.valueOf('A'), CommonUtil.convert(65.0, Character.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(Long.MAX_VALUE, Character.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(65536, Character.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(BigInteger.valueOf(-1), Character.class));
    }

    // ============================================================ U05-05: add-ok / clear-UOE collections keep the probe element (doc pin)

    @Test
    public void testU0505_appendOnlyCollectionKeepsTheProbeElement() {
        final List<String> backing = new ArrayList<>(Arrays.asList("a", "b", "c"));
        final Collection<String> appendOnly = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return backing.iterator();
            }

            @Override
            public int size() {
                return backing.size();
            }

            @Override
            public boolean add(final String e) {
                return backing.add(e);
            }

            @Override
            public void clear() {
                throw new UnsupportedOperationException("clear");
            }
        };

        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.reverse(appendOnly));
        assertEquals(Arrays.asList("a", "b", "c", "a"), backing); // the documented shape: one extra copy of the first element
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.rotate(appendOnly, 1));
        assertEquals(Arrays.asList("a", "b", "c", "a", "a"), backing);
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.shuffle(appendOnly));
        assertEquals(6, backing.size());
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.shuffle(appendOnly, new Random(1)));
        assertEquals(7, backing.size());
        assertEquals("a", backing.get(6));
    }

    // ============================================================ U06-01: the two fill twins diverge as documented (doc pin)

    @Test
    public void testU0601_fillTwinsDivergeAsDocumented() {
        final List<String> fixed1 = Arrays.asList("a", "b");
        assertThrows(UnsupportedOperationException.class, () -> CommonUtil.fill(fixed1, 0, 3, "x"));
        assertEquals(Arrays.asList("a", "b"), fixed1); // N.fill: grows first, so a fixed-size list is left unchanged

        final List<String> fixed2 = Arrays.asList("a", "b");
        final List<String> supplied = new ArrayList<>();
        final Supplier<String> counter = () -> {
            supplied.add("s" + supplied.size());
            return supplied.get(supplied.size() - 1);
        };
        assertThrows(UnsupportedOperationException.class, () -> Iterables.fill(fixed2, 0, 3, counter));
        assertEquals(Arrays.asList("s0", "s1"), fixed2); // Iterables.fill: existing range first, supplier called in index order
        // the supplier was called in index order for every slot, including the one whose add() then failed
        assertEquals(Arrays.asList("s0", "s1", "s2"), supplied);
    }

    // ============================================================ U06-02: the IllegalAccessException negative cache, for real

    @Test
    public void testU0602_inaccessibleDescendingIteratorIsNegativeCachedForReal() throws Exception {
        final int callsBefore = CommonUtilReviewFixtures.descendingCalls();
        final Iterable<String> it = CommonUtilReviewFixtures.hiddenDescending(Arrays.asList("a", "b", "c"));

        // the public descendingIterator() of a package-private class in another package cannot be invoked reflectively:
        // the forward fallback still gives the right answer ...
        assertEquals("c", CommonUtil.lastElement(it).orElse(null));
        assertNull(CommonUtil.getDescendingIteratorIfPossible(it));

        // ... the method was found (public, returns an Iterator) but the failed invoke was recorded after the first call ...
        final Field poolField = CommonUtil.class.getDeclaredField("descendingIteratorMethodPool");
        poolField.setAccessible(true);
        final Object holder = ((ClassValue<?>) poolField.get(null)).get(CommonUtilReviewFixtures.hiddenDescendingClass());
        assertNotNull(holder);
        final Field methodField = holder.getClass().getDeclaredField("method");
        methodField.setAccessible(true);
        assertEquals("descendingIterator", ((Method) methodField.get(holder)).getName());
        final Field flag = holder.getClass().getDeclaredField("inaccessible");
        flag.setAccessible(true);
        assertTrue(flag.getBoolean(holder));

        // ... and it never actually ran, before or after the flag was set
        assertEquals("c", CommonUtil.lastElement(it).orElse(null));
        assertEquals("c", CommonUtil.lastElement(CommonUtilReviewFixtures.hiddenDescending(Arrays.asList("x", "c"))).orElse(null));
        assertEquals(callsBefore, CommonUtilReviewFixtures.descendingCalls());
    }

    // ============================================================ U06-05: a SortedSet never throws (doc pin)

    @Test
    public void testU0605_sortedSetsNeverThrow() {
        final SortedSet<Integer> ro = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1, 2, 3)));
        assertDoesNotThrow(() -> CommonUtil.reverse(ro));
        assertDoesNotThrow(() -> CommonUtil.rotate(ro, 1));
        assertDoesNotThrow(() -> CommonUtil.shuffle(ro));
        assertDoesNotThrow(() -> CommonUtil.shuffle(ro, new Random(3)));
        assertEquals(Arrays.asList(1, 2, 3), new ArrayList<>(ro));
    }

    // ============================================================ U06-06: indicesOfAll(Object[], Object) overload note (doc pin)

    @Test
    public void testU0606_indicesOfAllNullNeedsTheObjectCast() {
        final String[] a = { "a", null, "b", null };
        assertArrayEquals(new int[] { 1, 3 }, CommonUtil.indicesOfAll(a, (Object) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.indicesOfAll(a, null)); // binds to the Predicate overload
        final Predicate<String> p = "a"::equals;
        assertArrayEquals(new int[] { 0 }, CommonUtil.indicesOfAll(a, p)); // tested with the predicate
        assertArrayEquals(new int[0], CommonUtil.indicesOfAll(a, (Object) p)); // compared to it
    }

    // ============================================================ U20-02: defaultIfNull/Empty/Blank(T, Supplier) name the supplier

    @Test
    public void testU2002_supplierForDefaultIsNamedInTheMessage() {
        final Supplier<String> nullS = () -> null;
        final Supplier<String> emptyS = () -> "";
        final Supplier<String> blankS = () -> " \t";

        assertEquals("supplierForDefault returned null", assertThrows(NullPointerException.class, () -> CommonUtil.defaultIfNull((String) null, nullS)).getMessage());
        assertEquals("supplierForDefault returned null",
                assertThrows(NullPointerException.class, () -> CommonUtil.defaultIfEmpty("", nullS)).getMessage());
        assertEquals("supplierForDefault returned empty",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty((String) null, emptyS)).getMessage());
        assertEquals("supplierForDefault returned null",
                assertThrows(NullPointerException.class, () -> CommonUtil.defaultIfBlank(" ", nullS)).getMessage());
        assertEquals("supplierForDefault returned empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("", emptyS)).getMessage());
        assertEquals("supplierForDefault returned empty or blank",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank((String) null, blankS)).getMessage());

        // a null supplier is still reported by its parameter name; a value that needs no default never consults the supplier
        assertEquals("'supplierForDefault' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull((String) null, (Supplier<String>) null)).getMessage());
        assertEquals("'supplierForDefault' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (Supplier<String>) null)).getMessage());
        assertEquals("'supplierForDefault' cannot be null",
                assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank(" ", (Supplier<String>) null)).getMessage());
        assertEquals("v", CommonUtil.defaultIfNull("v", nullS));
        assertEquals("v", CommonUtil.defaultIfEmpty("v", nullS));
        assertEquals("v", CommonUtil.defaultIfBlank("v", nullS));

        // happy paths
        final Supplier<String> d = () -> "d";
        assertEquals("d", CommonUtil.defaultIfNull((String) null, d));
        assertEquals("d", CommonUtil.defaultIfEmpty("", d));
        assertEquals("d", CommonUtil.defaultIfEmpty((String) null, d));
        assertEquals("d", CommonUtil.defaultIfBlank("  ", d));
        assertEquals("d", CommonUtil.defaultIfBlank((String) null, d));

        // the Strings twins already followed the same style (L12/C-004)
        assertEquals("defaultValueSupplier returned null", assertThrows(NullPointerException.class, () -> Strings.defaultIfNull((String) null, nullS)).getMessage());
    }
}
