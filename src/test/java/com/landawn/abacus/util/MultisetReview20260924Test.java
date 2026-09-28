package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-24 Multiset review (ledger C-341..C-349).
 */
public class MultisetReview20260924Test extends TestBase {

    // ---------------------------------------------------------------- helpers

    private static Multiset<String> synchronizedIdentityMultiset() {
        final Supplier<Map<String, Object>> supplier = () -> Collections.synchronizedMap(new IdentityHashMap<>());
        return new Multiset<>(supplier);
    }

    private static long sumOf(final Map<?, Integer> map) {
        long sum = 0;
        for (final Integer v : map.values()) {
            sum += v;
        }
        return sum;
    }

    // ---------------------------------------------------------------- C-341

    @Test
    public void testC341_toMapThrowsWhenFallbackHashMapWouldCollapseCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);
        assertEquals(2, ms.countOfDistinctElements());
        assertEquals(3, ms.sumOfOccurrences());

        final IllegalStateException e = assertThrows(IllegalStateException.class, ms::toMap);
        assertTrue(e.getMessage().contains("toMap(IntFunction)"), e.getMessage());
        assertTrue(e.getMessage().contains("entrySet()"), e.getMessage());
    }

    @Test
    public void testC341_toImmutableMapThrowsWhenFallbackHashMapWouldCollapseCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        assertThrows(IllegalStateException.class, ms::toImmutableMap);
    }

    @Test
    public void testC341_unicodeElementsCollapseDetected() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("日本😀"), 4);
        ms.add(new String("日本😀"), 5);

        assertThrows(IllegalStateException.class, ms::toMap);
    }

    @Test
    public void testC341_recommendedAlternativesPreserveAllCounts() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final IdentityHashMap<String, Integer> identity = ms.toMap(size -> new IdentityHashMap<>());
        assertEquals(2, identity.size());
        assertEquals(3, sumOf(identity));

        long entrySum = 0;
        for (final Multiset.Entry<String> entry : ms.entrySet()) {
            entrySum += entry.count();
        }
        assertEquals(3, entrySum);
    }

    @Test
    public void testC341_nonCollidingFallbackStillCopies() {
        final Supplier<Map<String, Object>> supplier = () -> Collections.synchronizedMap(new HashMap<>());
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add("a", 2);
        ms.add(null, 1);
        ms.add("é", 3);

        final Map<String, Integer> map = ms.toMap();
        assertEquals(3, map.size());
        assertEquals(2, map.get("a"));
        assertEquals(1, map.get(null));
        assertEquals(3, map.get("é"));
        assertEquals(map, ms.toImmutableMap());
    }

    @Test
    public void testC341_emptyFallbackMultisetGivesEmptyMap() {
        final Multiset<String> ms = synchronizedIdentityMultiset();
        assertTrue(ms.toMap().isEmpty());
        assertTrue(ms.toImmutableMap().isEmpty());
    }

    @Test
    public void testC341_instantiableIdentityBackingKeepsItsType() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final Map<String, Integer> map = ms.toMap();
        assertTrue(map instanceof IdentityHashMap, map.getClass().getName());
        assertEquals(2, map.size());
        assertEquals(3, sumOf(map));
    }

    @Test
    public void testC341_sortedByOccurrencesMessageQualifiesToMapRecommendation() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add(new String("k"), 1);
        ms.add(new String("k"), 2);

        final IllegalStateException e = assertThrows(IllegalStateException.class, ms::toMapSortedByOccurrences);
        // toMap() is only lossless when the backing map's type can be instantiated; the message must say so
        // instead of promising that toMap() always preserves the backing map's equivalence.
        assertFalse(e.getMessage().contains("which preserve the backing map's own equivalence"), e.getMessage());
        assertTrue(e.getMessage().contains("instantiated"), e.getMessage());
        assertTrue(e.getMessage().contains("toMap(IntFunction)"), e.getMessage());
        assertTrue(e.getMessage().contains("entrySet()"), e.getMessage());
    }

    // ---------------------------------------------------------------- C-342

    @Test
    public void testC342_toMapSupplierReturningNullOnEmptyMultisetThrowsNpe() {
        final Multiset<String> ms = new Multiset<>();
        final NullPointerException e = assertThrows(NullPointerException.class, () -> ms.<Map<String, Integer>> toMap(n -> null));
        assertEquals("supplier returned null", e.getMessage());
    }

    @Test
    public void testC342_toMapSupplierReturningNullOnNonEmptyMultisetThrowsNpe() {
        final Multiset<String> ms = Multiset.of("a", "a", "b");
        final NullPointerException e = assertThrows(NullPointerException.class, () -> ms.<Map<String, Integer>> toMap(n -> null));
        assertEquals("supplier returned null", e.getMessage());
    }

    @Test
    public void testC342_toImmutableMapSupplierReturningNullThrowsNpe() {
        final Multiset<String> empty = new Multiset<>();
        NullPointerException e = assertThrows(NullPointerException.class, () -> empty.toImmutableMap(n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());

        final Multiset<String> nonEmpty = Multiset.of("中", "中");
        e = assertThrows(NullPointerException.class, () -> nonEmpty.toImmutableMap(n -> null));
        assertEquals("mapSupplier returned null", e.getMessage());
    }

    @Test
    public void testC342_nullSupplierStillRejected() {
        final Multiset<String> ms = Multiset.of("a");
        assertThrows(IllegalArgumentException.class, () -> ms.toMap((java.util.function.IntFunction<Map<String, Integer>>) null));
        assertThrows(IllegalArgumentException.class, () -> ms.toImmutableMap(null));
    }

    @Test
    public void testC342_supplierGetsDistinctCountAndResultIsReturned() {
        final Multiset<String> ms = Multiset.of("b", "a", "a", "c");
        final int[] hint = { -1 };
        final TreeMap<String, Integer> target = new TreeMap<>();
        final TreeMap<String, Integer> result = ms.toMap(n -> {
            hint[0] = n;
            return target;
        });
        assertSame(target, result);
        assertEquals(3, hint[0]);
        assertEquals("{a=2, b=1, c=1}", result.toString());

        final ImmutableMap<String, Integer> imm = ms.toImmutableMap(n -> new LinkedHashMap<>());
        assertEquals(2, imm.get("a"));
        assertEquals(3, imm.size());

        assertTrue(new Multiset<String>().toImmutableMap(n -> new HashMap<>()).isEmpty());
    }

    // ---------------------------------------------------------------- C-343

    @Test
    public void testC343_updateAllOccurrencesIsAllOrNothingWhenFunctionThrows() {
        final Supplier<Map<String, Object>> supplier = LinkedHashMap::new;
        final Multiset<String> ms = new Multiset<>(supplier);
        ms.add("a", 1);
        ms.add("b", 2);
        ms.add("c", 3);

        assertThrows(IllegalStateException.class, () -> ms.updateAllOccurrences((e, n) -> {
            if (e.equals("a")) {
                return 0;
            } else if (e.equals("b")) {
                return n * 10;
            }
            throw new IllegalStateException("boom");
        }));

        assertEquals(1, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(3, ms.getCount("c"));
        assertEquals(6, ms.size());
    }

    @Test
    public void testC343_updateAllOccurrencesAppliesUpdatesAndRemovals() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 1);
        ms.add("b", 2);
        ms.add(null, 3);
        ms.add("😀", 4);

        ms.updateAllOccurrences((e, n) -> e == null ? null : (n == 1 ? -5 : n * 10));

        assertEquals(0, ms.getCount("a"));
        assertEquals(20, ms.getCount("b"));
        assertEquals(0, ms.getCount(null));
        assertFalse(ms.contains(null));
        assertEquals(40, ms.getCount("😀"));
        assertEquals(2, ms.countOfDistinctElements());
    }

    @Test
    public void testC343_updateAllOccurrencesOnEmptyNeverCallsFunction() {
        final Multiset<String> ms = new Multiset<>();
        ms.updateAllOccurrences((e, n) -> {
            throw new AssertionError("must not be called");
        });
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testC343_updateAllOccurrencesRemovingEverything() {
        final Multiset<String> ms = Multiset.of("a", "b", "b");
        ms.updateAllOccurrences((e, n) -> 0);
        assertTrue(ms.isEmpty());
    }

    // ---------------------------------------------------------------- C-344

    @Test
    public void testC344_classConstructorRejectsBiMap() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(BiMap.class));
        assertTrue(e.getMessage().contains("valueMapType"), e.getMessage());
        assertTrue(e.getMessage().contains("BiMap"), e.getMessage());
    }

    @Test
    public void testC344_supplierConstructorRejectsBiMap() {
        final Supplier<Map<String, Object>> supplier = BiMap::new;
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<>(supplier));
        assertTrue(e.getMessage().contains("mapSupplier"), e.getMessage());
        assertTrue(e.getMessage().contains("BiMap"), e.getMessage());
    }

    @Test
    public void testC344_nullAndOrdinaryBackingTypesUnchanged() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>((Class<? extends Map>) null));

        final Multiset<String> linked = new Multiset<>(LinkedHashMap.class);
        linked.add("x");
        linked.add("y");
        linked.add("z");
        assertEquals(Arrays.asList("x", "y", "z"), linked.elementSet().stream().toList());

        // Documented remapping: EnumMap/ImmutableMap resolve to a mutable HashMap, so distinct elements with
        // equal counts (the case a BiMap cannot hold) work.
        final Multiset<String> fromEnumMap = new Multiset<>(EnumMap.class);
        fromEnumMap.add("a");
        fromEnumMap.add("b");
        assertEquals(2, fromEnumMap.countOfDistinctElements());

        final Multiset<String> fromImmutableMap = new Multiset<>(ImmutableMap.class);
        fromImmutableMap.add("a");
        fromImmutableMap.add("b");
        assertEquals(2, fromImmutableMap.size());
    }

    // ---------------------------------------------------------------- C-345

    @Test
    public void testC345_addAllMultisetAddsWholeCountsQuickly() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", 500_000_000);
        src.add("b", 500_000_000);

        final Multiset<String> dst = new Multiset<>();
        assertTimeout(Duration.ofMillis(1000), () -> assertTrue(dst.addAll(src)));
        assertEquals(500_000_000, dst.getCount("a"));
        assertEquals(500_000_000, dst.getCount("b"));
        assertEquals(1_000_000_000L, dst.sumOfOccurrences());
    }

    @Test
    public void testC345_addAllMultisetOverflowLeavesFailingElementUnchanged() {
        final Multiset<String> dst = new Multiset<>();
        dst.add("a", Integer.MAX_VALUE - 1);

        final Multiset<String> src = new Multiset<>();
        src.add("a", 5);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> dst.addAll(src));
        assertEquals(Integer.MAX_VALUE - 1, dst.getCount("a"));
        assertFalse(e.getMessage().contains("occurrencesToAdd=1"), e.getMessage());
    }

    @Test
    public void testC345_addAllMultisetWithMultiplierOverflowingLong() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", Integer.MAX_VALUE);

        final Multiset<String> dst = new Multiset<>();
        assertThrows(IllegalArgumentException.class, () -> dst.addAll(src, Integer.MAX_VALUE));
        assertEquals(0, dst.getCount("a"));
        assertTrue(dst.isEmpty());
    }

    @Test
    public void testC345_addAllMultisetWithMultiplier() {
        final Multiset<String> src = new Multiset<>();
        src.add("a", 2);
        src.add(null, 1);
        src.add("ß", 3);

        final Multiset<String> dst = Multiset.of("a");
        assertTrue(dst.addAll(src, 3));
        assertEquals(7, dst.getCount("a"));
        assertEquals(3, dst.getCount(null));
        assertEquals(9, dst.getCount("ß"));

        assertFalse(dst.addAll(src, 0));
        assertFalse(dst.addAll(new Multiset<String>()));
        assertFalse(dst.addAll((Multiset<String>) null));
        assertEquals(19, dst.size());
    }

    @Test
    public void testC345_addAllSelfDoubles() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", 2);
        ms.add("b", 3);

        assertTrue(ms.addAll(ms));
        assertEquals(4, ms.getCount("a"));
        assertEquals(6, ms.getCount("b"));

        assertTrue(ms.addAll(ms, 2));
        assertEquals(12, ms.getCount("a"));
        assertEquals(18, ms.getCount("b"));
    }

    @Test
    public void testC345_addAllFromFinerBackingMergesCounts() {
        final Supplier<Map<String, Object>> supplier = IdentityHashMap::new;
        final Multiset<String> src = new Multiset<>(supplier);
        src.add(new String("k"), 1);
        src.add(new String("k"), 2);

        final Multiset<String> dst = new Multiset<>();
        assertTrue(dst.addAll(src));
        assertEquals(3, dst.getCount("k"));
        assertEquals(1, dst.countOfDistinctElements());
    }

    // ---------------------------------------------------------------- C-348

    private static void assertToArrayThrowsIse(final Multiset<String> ms) {
        try {
            ms.toArray();
            fail("toArray() should throw IllegalStateException");
        } catch (final IllegalStateException expected) {
            // expected
        } catch (final OutOfMemoryError oom) {
            // Caught here: JUnit treats an escaping OutOfMemoryError as unrecoverable.
            fail("toArray() threw OutOfMemoryError instead of IllegalStateException: " + oom);
        }

        try {
            ms.toArray(new String[0]);
            fail("toArray(T[]) should throw IllegalStateException");
        } catch (final IllegalStateException expected) {
            // expected
        } catch (final OutOfMemoryError oom) {
            fail("toArray(T[]) threw OutOfMemoryError instead of IllegalStateException: " + oom);
        }
    }

    @Test
    public void testC348_toArrayAtIntegerMaxValueThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", Integer.MAX_VALUE);
        assertToArrayThrowsIse(ms);
    }

    @Test
    public void testC348_toArrayJustAboveVmLimitThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("é", Integer.MAX_VALUE - 7);
        assertToArrayThrowsIse(ms);
    }

    @Test
    public void testC348_toArrayAboveIntRangeThrowsIse() {
        final Multiset<String> ms = new Multiset<>();
        ms.add("a", Integer.MAX_VALUE);
        ms.add(null, Integer.MAX_VALUE);
        assertToArrayThrowsIse(ms);
    }

    @Test
    public void testC348_toArraySmallAndEmptyUnchanged() {
        final Multiset<String> ms = Multiset.of("a", "a", null);
        assertEquals(3, ms.toArray().length);
        assertEquals(0, new Multiset<String>().toArray().length);

        final String[] big = { "x", "x", "x", "x", "x" };
        final String[] out = ms.toArray(big);
        assertSame(big, out);
        assertEquals(null, out[3]);
    }

    // ---------------------------------------------------------------- C-349

    @Test
    public void testC349_negativeInitialCapacityNamesTheParameter() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(-1));
        assertTrue(e.getMessage().contains("initialCapacity"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(Integer.MIN_VALUE));
        assertTrue(e.getMessage().contains("initialCapacity"), e.getMessage());
    }

    @Test
    public void testC349_zeroAndPositiveInitialCapacityAccepted() {
        final Multiset<String> zero = new Multiset<>(0);
        zero.add("a");
        assertEquals(1, zero.size());

        final Multiset<String> hundred = new Multiset<>(100);
        assertTrue(hundred.isEmpty());
    }
}
