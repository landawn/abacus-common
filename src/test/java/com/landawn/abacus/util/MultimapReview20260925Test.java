package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the Multimap / ListMultimap / SetMultimap findings of the 2026-09-25 cross review
 * (U22-01, U22-04, U22-06, U22-07, U16-02 - fixer F08).
 */
@Tag("unit")
public class MultimapReview20260925Test extends TestBase {

    private enum Color {
        RED, GREEN, BLUE
    }

    private static Set<String> caseInsensitiveSet() {
        return Collections.newSetFromMap(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
    }

    // ------------------------------------------------------------------------------------------------
    // U22-01: a set whose membership rule is unknown decides a replacement under its own rule (as r9620)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2201_replaceEntry_caseInsensitiveNewSetFromMap_replacesUnderTheSetsOwnRule() {
        final Multimap<String, String, Set<String>> mm = N.newMultimap(HashMap::new, MultimapReview20260925Test::caseInsensitiveSet);
        mm.putValues("k", List.of("a", "b"));

        // "A" is 'contained' only as "a" itself: the replacement r9620 performed, refused with ISE before the fix
        assertTrue(mm.replaceEntry("k", "a", "A"));
        assertEquals(List.of("A", "b"), new ArrayList<>(mm.get("k")));

        // replaceEntriesIf goes through the same helper
        assertTrue(mm.replaceEntriesIf(k -> true, "b", "B"));
        assertEquals(List.of("A", "B"), new ArrayList<>(mm.get("k")));

        // a genuine duplicate under the set's rule is still refused, on the remove-then-add-then-restore path
        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", "A", "b"));
        assertTrue(ex.getMessage().startsWith("Failed to add the new value: b for key: k"), ex.getMessage());
        assertEquals(List.of("A", "B"), new ArrayList<>(mm.get("k")));
        assertThrows(IllegalStateException.class, () -> mm.replaceEntriesIf(k -> true, "A", "b"));
        assertEquals(List.of("A", "B"), new ArrayList<>(mm.get("k")));

        // absent old value: false, untouched; same value: allowed
        assertFalse(mm.replaceEntry("k", "zzz", "A"));
        assertTrue(mm.replaceEntry("k", "a", "a"));
        assertEquals(List.of("a", "B"), new ArrayList<>(mm.get("k")));
    }

    @Test
    public void testU2201_knownEqualsOrIdentitySetsStillRefuseBeforeAnythingIsRemoved() {
        // LinkedHashSet: the C-353 case - refused, order untouched
        final SetMultimap<String, Integer> linked = N.newLinkedSetMultimap();
        linked.putValues("k", List.of(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> linked.replaceEntry("k", 1, 3));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(linked.get("k")));
        assertThrows(IllegalStateException.class, () -> linked.replaceEntriesIf(k -> true, 1, 2));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(linked.get("k")));
        assertTrue(linked.replaceEntry("k", 1, 9));
        assertEquals(List.of(2, 3, 9), new ArrayList<>(linked.get("k")));

        // HashSet
        final SetMultimap<String, Integer> hash = N.newSetMultimap();
        hash.putValues("k", List.of(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> hash.replaceEntry("k", 1, 3));
        assertEquals(Set.of(1, 2, 3), hash.get("k"));

        // EnumSet
        final Supplier<Set<Color>> enumSets = () -> EnumSet.noneOf(Color.class);
        final Multimap<String, Color, Set<Color>> enums = N.newMultimap(HashMap::new, enumSets);
        enums.putValues("k", List.of(Color.RED, Color.GREEN));
        assertThrows(IllegalStateException.class, () -> enums.replaceEntry("k", Color.RED, Color.GREEN));
        assertEquals(EnumSet.of(Color.RED, Color.GREEN), enums.get("k"));
        assertTrue(enums.replaceEntry("k", Color.RED, Color.BLUE));
        assertEquals(EnumSet.of(Color.GREEN, Color.BLUE), enums.get("k"));

        // CopyOnWriteArraySet
        final Supplier<Set<Integer>> cowSets = CopyOnWriteArraySet::new;
        final Multimap<String, Integer, Set<Integer>> cow = N.newMultimap(HashMap::new, cowSets);
        cow.putValues("k", List.of(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> cow.replaceEntry("k", 1, 3));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(cow.get("k")));
        assertTrue(cow.replaceEntry("k", 1, 9));
        assertEquals(List.of(2, 3, 9), new ArrayList<>(cow.get("k")));

        // IdentityHashSet: a member that is not equals to oldValue is a distinct member (refused); an
        // equal-but-distinct new value is handed to remove-then-add, which succeeds under identity
        final Supplier<Set<String>> identitySets = IdentityHashSet::new;
        final Multimap<String, String, Set<String>> identity = N.newMultimap(HashMap::new, identitySets);
        final String a = new String("a");
        final String b = new String("b");
        identity.putValues("k", List.of(a, b));
        assertThrows(IllegalStateException.class, () -> identity.replaceEntry("k", a, b));
        assertEquals(2, identity.get("k").size());
        assertTrue(identity.get("k").contains(a));
        assertTrue(identity.replaceEntry("k", a, new String("b")));
        assertEquals(2, identity.get("k").size());
        assertFalse(identity.get("k").contains(a));
    }

    @Test
    public void testU2201_subclassedSetDecidesByRemoveThenAdd() {
        // an anonymous HashSet subclass is not exactly a HashSet: the base path still refuses a duplicate
        final Supplier<Set<Integer>> subclassed = () -> new HashSet<Integer>() {
            private static final long serialVersionUID = 1L;
        };
        final Multimap<String, Integer, Set<Integer>> sub = N.newMultimap(HashMap::new, subclassed);
        sub.putValues("k", List.of(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> sub.replaceEntry("k", 1, 3));
        assertEquals(Set.of(1, 2, 3), sub.get("k"));
        assertTrue(sub.replaceEntry("k", 1, 9));
        assertEquals(Set.of(2, 3, 9), sub.get("k"));

        // a sorted set is decided by its comparator, as before
        final Multimap<String, String, TreeSet<String>> ci = N.newMultimap(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        ci.putValues("k", List.of("a", "b"));
        assertTrue(ci.replaceEntry("k", "a", "A"));
        assertThrows(IllegalStateException.class, () -> ci.replaceEntry("k", "A", "B"));
        assertEquals(List.of("A", "b"), new ArrayList<>(ci.get("k")));
    }

    // ------------------------------------------------------------------------------------------------
    // U16-02: invert() backing map is a HashMap only for an exact HashMap backing, else a LinkedHashMap
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU1602_listMultimapInvertBackingMap() {
        final ListMultimap<String, Integer> concurrent = new ListMultimap<>(ConcurrentHashMap.class, ArrayList.class);
        concurrent.put("a", 1);
        concurrent.put("b", 1);
        assertEquals(LinkedHashMap.class, concurrent.invert().backingMap.getClass());
        assertEquals(ConcurrentHashMap.class, concurrent.copy().backingMap.getClass());
        assertEquals(Set.of("a", "b"), new HashSet<>(concurrent.invert().get(1)));

        final ListMultimap<String, Integer> hash = new ListMultimap<>(HashMap.class, ArrayList.class);
        hash.put("a", 1);
        assertEquals(HashMap.class, hash.invert().backingMap.getClass());

        final ListMultimap<String, Integer> sorted = new ListMultimap<>(TreeMap.class, ArrayList.class);
        sorted.put("b", 1);
        sorted.put("a", 2);
        final ListMultimap<Integer, String> inverted = sorted.invert();
        assertEquals(LinkedHashMap.class, inverted.backingMap.getClass());
        assertEquals(List.of(2, 1), new ArrayList<>(inverted.keySet()));

        final ListMultimap<String, Integer> table = new ListMultimap<>(Hashtable.class, ArrayList.class);
        table.put("a", 1);
        assertEquals(LinkedHashMap.class, table.invert().backingMap.getClass());
        assertEquals(Hashtable.class, table.copy().backingMap.getClass());
    }

    @Test
    public void testU1602_setMultimapInvertBackingMap() {
        final SetMultimap<String, Integer> concurrent = new SetMultimap<>(ConcurrentHashMap.class, HashSet.class);
        concurrent.put("a", 1);
        concurrent.put("b", 1);
        assertEquals(LinkedHashMap.class, concurrent.invert().backingMap.getClass());
        assertEquals(ConcurrentHashMap.class, concurrent.copy().backingMap.getClass());
        assertEquals(Set.of("a", "b"), concurrent.invert().get(1));

        final SetMultimap<String, Integer> hash = new SetMultimap<>(HashMap.class, HashSet.class);
        hash.put("a", 1);
        assertEquals(HashMap.class, hash.invert().backingMap.getClass());

        final SetMultimap<String, Integer> sorted = new SetMultimap<>(TreeMap.class, HashSet.class);
        sorted.put("b", 1);
        sorted.put("a", 2);
        final SetMultimap<Integer, String> inverted = sorted.invert();
        assertEquals(LinkedHashMap.class, inverted.backingMap.getClass());
        assertEquals(List.of(2, 1), new ArrayList<>(inverted.keySet()));
    }

    // ------------------------------------------------------------------------------------------------
    // U22-04: computeIfAbsent / compute / merge x2 report a value supplier that returns null
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2204_computeAndMergeOnAnAbsentKeyReportTheNullValueSupplier() {
        final Supplier<List<Integer>> nullSupplier = () -> null;
        final Multimap<String, Integer, List<Integer>> mm = N.newMultimap(HashMap::new, nullSupplier);

        NullPointerException e = assertThrows(NullPointerException.class, () -> mm.computeIfAbsent("k", k -> List.of(1)));
        assertEquals("valueSupplier returned null", e.getMessage());

        e = assertThrows(NullPointerException.class, () -> mm.compute("k", (k, v) -> List.of(1)));
        assertEquals("valueSupplier returned null", e.getMessage());

        final BiFunction<List<Integer>, Collection<Integer>, List<Integer>> keepOld = (v, c) -> v;
        e = assertThrows(NullPointerException.class, () -> mm.merge("k", List.of(1), keepOld));
        assertEquals("valueSupplier returned null", e.getMessage());

        e = assertThrows(NullPointerException.class, () -> mm.merge("k", 1, (v, x) -> v));
        assertEquals("valueSupplier returned null", e.getMessage());

        assertTrue(mm.isEmpty());
    }

    // ------------------------------------------------------------------------------------------------
    // U22-06: toMap() / toMap(IntFunction) reject a lossy set copy (the SetMultimap copy policy)
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2206_toMapRejectsALossySetCopy() {
        final Set<String> values = new HashSet<String>() {
            private static final long serialVersionUID = 1L;
        };
        values.add("a");
        values.add("A");
        final Map<String, Set<String>> source = new HashMap<>();
        source.put("k", values);
        final SetMultimap<String, String> mm = SetMultimap.wrap(source, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, mm::toMap);
        assertEquals("Set copy loses distinct source elements", e.getMessage());
        e = assertThrows(IllegalArgumentException.class, () -> mm.toMap(HashMap::new));
        assertEquals("Set copy loses distinct source elements", e.getMessage());
        assertEquals(Set.of("a", "A"), mm.get("k"));

        // a faithful copy is fine
        final SetMultimap<String, String> ok = N.newLinkedSetMultimap();
        ok.putValues("k", List.of("a", "A"));
        assertEquals(Set.of("a", "A"), ok.toMap().get("k"));
    }

    // ------------------------------------------------------------------------------------------------
    // U22-07: toMultiset() with a BiMap map supplier
    // ------------------------------------------------------------------------------------------------

    @Test
    public void testU2207_toMultisetRejectsABiMapMapSupplier() {
        final Multimap<String, Integer, List<Integer>> mm = N.newMultimap(BiMap::new, ArrayList::new);
        mm.put("a", 1);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, mm::toMultiset);
        assertTrue(e.getMessage().contains("cannot supply a BiMap"), e.getMessage());

        // control: an ordinary map supplier
        final ListMultimap<String, Integer> plain = N.newListMultimap();
        plain.putValues("a", Arrays.asList(1, 2));
        plain.put("b", 3);
        final Multiset<String> counts = plain.toMultiset();
        assertEquals(2, counts.getCount("a"));
        assertEquals(1, counts.getCount("b"));
    }
}
