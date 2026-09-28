package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.StringJoiner;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentSkipListMap;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the Maps / BiMap / ImmutableBiMap findings of the 2026-09-25 cross review (U16-01,
 * U16-03, U16-04, U16-05, U16-06 - fixer F08).
 */
public class MapsReview20260925Test extends TestBase {

    private record Box(int v) {
    }

    private static LinkedHashMap<String, Integer> ordered(final String... keys) {
        final LinkedHashMap<String, Integer> m = new LinkedHashMap<>();

        for (int i = 0; i < keys.length; i++) {
            m.put(keys[i], i);
        }

        return m;
    }

    // ------------------------------------------------------------------------------------------------
    // U16-01: BiMap.copyOf / ImmutableBiMap.copyOf of an unconstructible ordered source keep its order
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1601_copyOfUnconstructibleOrderedSourceKeepsOrder() {
        final List<String> keys = Arrays.asList("zeta", "alpha", "mu", "beta", "omega");
        final Map<String, Integer> unmodifiable = Collections.unmodifiableMap(ordered(keys.toArray(new String[0])));

        final BiMap<String, Integer> copy = BiMap.copyOf(unmodifiable);
        assertEquals(keys, new ArrayList<>(copy.keySet()));
        assertEquals(LinkedHashMap.class, copy.keyMap.getClass());
        assertEquals(LinkedHashMap.class, copy.valueMap.getClass());
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), new ArrayList<>(copy.inverse().keySet()));
        // the copy's own copies keep the order too (the suppliers are derived from the LinkedHashMap)
        assertEquals(keys, new ArrayList<>(copy.copy().keySet()));

        final ImmutableBiMap<String, Integer> immutable = ImmutableBiMap.copyOf(unmodifiable);
        assertEquals(keys, new ArrayList<>(immutable.keySet()));

        assertEquals(keys, new ArrayList<>(BiMap.copyOf(Collections.synchronizedMap(ordered(keys.toArray(new String[0])))).keySet()));
        assertEquals(keys, new ArrayList<>(ImmutableBiMap.copyOf(ImmutableMap.copyOf(ordered(keys.toArray(new String[0])))).keySet()));
    }

    @Test
    public void testU1601_valueToKeyMapIsHashMapOnlyForAHashMapSource() {
        final BiMap<String, Integer> fromHash = BiMap.copyOf(new HashMap<>(ordered("a", "b")));
        assertEquals(HashMap.class, fromHash.keyMap.getClass());
        assertEquals(HashMap.class, fromHash.valueMap.getClass());

        final Hashtable<String, Integer> table = new Hashtable<>();
        table.put("a", 1);
        final BiMap<String, Integer> fromTable = BiMap.copyOf(table);
        assertEquals(Hashtable.class, fromTable.keyMap.getClass());
        assertEquals(LinkedHashMap.class, fromTable.valueMap.getClass());

        final BiMap<String, Integer> fromLinked = BiMap.copyOf(ordered("b", "a"));
        assertEquals(LinkedHashMap.class, fromLinked.keyMap.getClass());
        assertEquals(LinkedHashMap.class, fromLinked.valueMap.getClass());
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(fromLinked.keySet()));
    }

    // ------------------------------------------------------------------------------------------------
    // U16-06: BiMap.copyOf(ConcurrentSkipListMap) mirrors the concurrent sorted map on both sides
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1606_copyOfConcurrentSkipListMapMirrorsOnBothSides() {
        final Comparator<String> reverse = Comparator.reverseOrder();
        final ConcurrentSkipListMap<String, Integer> source = new ConcurrentSkipListMap<>(reverse);
        source.put("a", 1);
        source.put("c", 3);
        source.put("b", 2);

        final BiMap<String, Integer> copy = BiMap.copyOf(source);
        assertEquals(ConcurrentSkipListMap.class, copy.keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) copy.keyMap).comparator());
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(copy.keySet()));
        assertEquals(LinkedHashMap.class, copy.valueMap.getClass());

        // copy() rebuilds the key side through the supplier: same kind, same comparator (was a TreeMap)
        final BiMap<String, Integer> copyOfCopy = copy.copy();
        assertEquals(ConcurrentSkipListMap.class, copyOfCopy.keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) copyOfCopy.keyMap).comparator());
        assertEquals(Arrays.asList("c", "b", "a"), new ArrayList<>(copyOfCopy.keySet()));

        // the inverse view shares the live maps, and its copy swaps the suppliers
        final BiMap<Integer, String> inverse = copy.inverse();
        assertSame(copy.keyMap, inverse.valueMap);
        assertEquals(ConcurrentSkipListMap.class, inverse.copy().valueMap.getClass());
        assertEquals(LinkedHashMap.class, inverse.copy().keyMap.getClass());

        // independence
        copyOfCopy.put("d", 4);
        assertFalse(copy.containsKey("d"));
        assertFalse(source.containsKey("d"));
    }

    @Test
    public void testU1606_copyOfConcurrentSkipListMapWithNonComparableKeysCopiesWithoutCCE() {
        final Comparator<Box> byV = Comparator.comparingInt(Box::v);
        final ConcurrentSkipListMap<Box, String> source = new ConcurrentSkipListMap<>(byV);
        source.put(new Box(2), "two");
        source.put(new Box(1), "one");

        final BiMap<Box, String> copy = BiMap.copyOf(source);
        final BiMap<Box, String> copyOfCopy = copy.copy();
        assertEquals(Arrays.asList(new Box(1), new Box(2)), new ArrayList<>(copyOfCopy.keySet()));
        assertEquals(ConcurrentSkipListMap.class, copyOfCopy.keyMap.getClass());
        assertEquals(Arrays.asList("one", "two"), new ArrayList<>(copy.inverse().copy().keySet()));
    }

    @Test
    public void testU1606_copyOfTreeMapStillYieldsTreeMapOnBothSides() {
        final Comparator<String> reverse = Comparator.reverseOrder();
        final TreeMap<String, Integer> tree = new TreeMap<>(reverse);
        tree.put("a", 1);
        tree.put("b", 2);

        final BiMap<String, Integer> fromTree = BiMap.copyOf(tree);
        assertEquals(TreeMap.class, fromTree.keyMap.getClass());
        assertEquals(TreeMap.class, fromTree.copy().keyMap.getClass());
        assertSame(reverse, ((SortedMap<String, Integer>) fromTree.copy().keyMap).comparator());
        assertEquals(Arrays.asList("b", "a"), new ArrayList<>(fromTree.copy().keySet()));
    }

    // ------------------------------------------------------------------------------------------------
    // U16-04: an unclearable map reports the UOE unless the duplicate is certain under every equivalence
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1604_unmodifiableIdentityMapReportsUnsupportedWithTheProbableDuplicateSuppressed() {
        final IdentityHashMap<String, Integer> identity = new IdentityHashMap<>();
        identity.put("p", 1);
        identity.put("q", 2);
        final Map<String, Integer> map = Collections.unmodifiableMap(identity);

        // two distinct String instances: a duplicate under equals, not under the map's identity rule
        final UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> new String("K")));
        assertEquals(1, ex.getSuppressed().length);
        assertTrue(ex.getSuppressed()[0] instanceof IllegalStateException, ex.getSuppressed()[0].toString());
        final String probable = ex.getSuppressed()[0].getMessage();
        assertTrue(probable.startsWith("Duplicate new key: K - produced by both '"), probable);
        assertTrue(probable.contains("equals-keyed copy"), probable);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("p"));
        assertEquals(Integer.valueOf(2), map.get("q"));

        // control: the same converter on a modifiable identity map keeps both keys apart
        final Map<String, Integer> modifiable = Collections.synchronizedMap(new IdentityHashMap<>(identity));
        Maps.replaceKeys(modifiable, k -> new String("K"));
        assertEquals(2, modifiable.size());
    }

    @Test
    public void testU1604_sameReferenceCollisionOnAnUnclearableMapIsStillIllegalState() {
        final Map<String, Integer> map = Collections.unmodifiableMap(ordered("a", "b", "c"));

        // the same literal for every key: a duplicate under every key equivalence -> ISE, cause UOE
        final IllegalStateException same = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "same"));
        assertEquals("Duplicate new key: same - produced by both 'a' and 'b'", same.getMessage());
        assertTrue(same.getCause() instanceof UnsupportedOperationException);

        // the first collision decides, and it names the earliest producer
        final IllegalStateException first = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "b".equals(k) ? "y" : "x"));
        assertEquals("Duplicate new key: x - produced by both 'a' and 'c'", first.getMessage());

        // multiple nulls are a certain duplicate too
        final IllegalStateException nulls = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> null));
        assertEquals("Duplicate new key: null - produced by both 'a' and 'b'", nulls.getMessage());
        assertTrue(nulls.getCause() instanceof UnsupportedOperationException);

        // equals-only collision (distinct instances): UOE with the probable duplicate suppressed
        final UnsupportedOperationException equalsOnly = assertThrows(UnsupportedOperationException.class,
                () -> Maps.replaceKeys(map, k -> new String("same")));
        assertEquals(1, equalsOnly.getSuppressed().length);
        assertTrue(equalsOnly.getSuppressed()[0].getMessage().startsWith("Duplicate new key: same - produced by both 'a' and 'b'"),
                equalsOnly.getSuppressed()[0].getMessage());

        // no collision at all: the plain UOE, nothing suppressed
        final UnsupportedOperationException plain = assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> k + "!"));
        assertEquals(0, plain.getSuppressed().length);

        // an ImmutableMap is unclearable in the same way
        final Map<String, Integer> immutable = ImmutableMap.copyOf(ordered("a", "b"));
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(immutable, k -> "same"));
        assertEquals(0, assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(immutable, k -> k + "!")).getSuppressed().length);

        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
    }

    @Test
    public void testU1604_faithfulScratchStillReportsDuplicatesBeforeTouchingTheMap() {
        // an unmodifiable SortedMap gets a faithful TreeMap scratch: the duplicate is decided there, before
        // clear() is ever attempted, so there is no UOE cause and equals-only instances collide as the
        // comparator says they do
        final TreeMap<String, Integer> tree = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        tree.put("a", 1);
        tree.put("b", 2);
        final SortedMap<String, Integer> map = Collections.unmodifiableSortedMap(tree);

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(map, k -> "b".equals(k) ? "A" : k));
        assertEquals("Duplicate new key: A - produced by both 'a' and 'b'", ex.getMessage());
        assertEquals(null, ex.getCause());
        assertThrows(UnsupportedOperationException.class, () -> Maps.replaceKeys(map, k -> k + "!"));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(map.keySet()));
    }

    // ------------------------------------------------------------------------------------------------
    // U16-05: the in-place path (no scratch fill) still decides duplicates on the real map
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1605_inPlacePathDecidesDuplicatesOnTheRealMap() {
        final Map<String, Integer> map = Collections.synchronizedMap(ordered("a", "b", "c"));

        final IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> Maps.replaceKeys(map, k -> "c".equals(k) ? "A" : k.toUpperCase(Locale.ROOT)));
        assertEquals("Duplicate new key: A - produced by both 'a' and 'c'", ex.getMessage());
        assertEquals(Arrays.asList("a", "b", "c"), new ArrayList<>(map.keySet()));
        assertEquals(Arrays.asList(0, 1, 2), new ArrayList<>(map.values()));

        // a synchronized case-insensitive TreeMap decides under its own rule: "a" and "A" collide there
        final TreeMap<String, Integer> ci = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        ci.put("a", 1);
        ci.put("b", 2);
        final Map<String, Integer> sync = Collections.synchronizedMap(ci);
        assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(sync, k -> "b".equals(k) ? "A" : k));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(sync.keySet()));

        // and a converter failure leaves the map untouched (every key is converted before clear())
        assertThrows(ArithmeticException.class, () -> Maps.replaceKeys(sync, k -> {
            if ("b".equals(k)) {
                throw new ArithmeticException("boom");
            }
            return k + "!";
        }));
        assertEquals(Arrays.asList("a", "b"), new ArrayList<>(sync.keySet()));

        Maps.replaceKeys(sync, k -> k + "!");
        assertEquals(Arrays.asList("a!", "b!"), new ArrayList<>(sync.keySet()));
        assertEquals(Integer.valueOf(1), sync.get("A!"));
    }

    // ------------------------------------------------------------------------------------------------
    // U16-03: the documented empty-text rule of getByPathAsInt and the getAs* accessors
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1603_getByPathAsIntEmptyTextIsAbsentAndOtherTypesAreReadFromTheirStringForm() {
        final Map<String, Object> root = new HashMap<>();
        root.put("user", N.asMap("joiner", new StringJoiner(","), "flag", Boolean.TRUE, "age", "25", "score", "", "blank", " "));

        assertFalse(Maps.getByPathAsInt(root, "user.joiner").isPresent());
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.joiner", -1));
        assertFalse(Maps.getByPathAsInt(root, "user.score").isPresent());
        assertEquals(-1, Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.score", -1));
        assertEquals(25, Maps.getByPathAsInt(root, "user.age").getAsInt());
        assertEquals(25, Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.age", -1));

        assertThrows(NumberFormatException.class, () -> Maps.getByPathAsInt(root, "user.flag"));
        assertThrows(NumberFormatException.class, () -> Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.flag", -1));
        assertThrows(NumberFormatException.class, () -> Maps.getByPathAsInt(root, "user.blank"));
        assertThrows(NumberFormatException.class, () -> Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.blank", -1));

        final Map<String, Object> m = N.asMap("joiner", new StringJoiner(","), "flag", Boolean.TRUE);
        assertFalse(Maps.getAsByte(m, "joiner").isPresent());
        assertFalse(Maps.getAsShort(m, "joiner").isPresent());
        assertFalse(Maps.getAsInt(m, "joiner").isPresent());
        assertFalse(Maps.getAsLong(m, "joiner").isPresent());
        assertFalse(Maps.getAsFloat(m, "joiner").isPresent());
        assertFalse(Maps.getAsDouble(m, "joiner").isPresent());
        assertEquals(7, Maps.getAsIntOrDefaultIfAbsent(m, "joiner", 7));
        assertEquals(7L, Maps.getAsLongOrDefaultIfAbsent(m, "joiner", 7L));
        assertThrows(NumberFormatException.class, () -> Maps.getAsInt(m, "flag"));
        assertThrows(NumberFormatException.class, () -> Maps.getAsDoubleOrDefaultIfAbsent(m, "flag", 1d));
    }
}
