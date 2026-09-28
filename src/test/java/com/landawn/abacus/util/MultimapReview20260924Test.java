package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the 2026-09-24 Multimap review (ledger C-351..C-360).
 */
@Tag("unit")
public class MultimapReview20260924Test extends TestBase {

    private record Box(int v) {
    }

    private static ListMultimap<String, Integer> listOf(final Object... keyValues) {
        final ListMultimap<String, Integer> mm = new ListMultimap<>(LinkedHashMap::new, ArrayList::new);

        for (int i = 0; i < keyValues.length; i += 2) {
            mm.put((String) keyValues[i], (Integer) keyValues[i + 1]);
        }

        return mm;
    }

    // ---------------------------------------------------------------- C-351

    @Test
    public void testC351_toMapIntFunction_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = N.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.toMap(n -> null));
        assertTrue(e1.getMessage().contains("returned null"), e1.getMessage());

        final ListMultimap<String, Integer> one = listOf("a", 1);
        assertThrows(NullPointerException.class, () -> one.toMap(n -> null));
        assertEquals(List.of(1), one.get("a"));
    }

    @Test
    public void testC351_flatValues_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = N.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.flatValues(n -> null));
        assertTrue(e1.getMessage().contains("returned null"), e1.getMessage());

        assertThrows(NullPointerException.class, () -> listOf("a", 1, "b", 2).flatValues(n -> null));
        assertThrows(IllegalArgumentException.class, () -> listOf("a", 1).flatValues(null));
        assertEquals(List.of(1, 2), listOf("a", 1, "b", 2).flatValues(ArrayList::new));
    }

    @Test
    public void testC351_invert_nullFactory_NPE_onEmptyAndNonEmpty() {
        final ListMultimap<String, Integer> empty = N.newListMultimap();
        final NullPointerException e1 = assertThrows(NullPointerException.class, () -> empty.<List<String>, ListMultimap<Integer, String>> invert(n -> null));
        assertTrue(e1.getMessage().contains("multimapSupplier returned null"), e1.getMessage());

        final ListMultimap<String, Integer> one = listOf("a", 1);
        assertThrows(NullPointerException.class, () -> one.<List<String>, ListMultimap<Integer, String>> invert(n -> null));
        assertEquals(List.of("a"), one.<List<String>, ListMultimap<Integer, String>> invert(n -> N.newListMultimap()).get(1));
    }

    // ---------------------------------------------------------------- C-352

    @Test
    public void testC352_toMap_keepsReverseTreeSetComparator() {
        final TreeSet<Integer> reversed = new TreeSet<>(Comparator.reverseOrder());
        reversed.addAll(List.of(1, 2, 3));
        final Map<String, Set<Integer>> backing = new HashMap<>();
        backing.put("k", reversed);
        final SetMultimap<String, Integer> mm = SetMultimap.wrap(backing);

        final Map<String, Set<Integer>> map = mm.toMap();
        assertEquals(List.of(3, 2, 1), new ArrayList<>(map.get("k")));
        assertSame(reversed.comparator(), ((SortedSet<Integer>) map.get("k")).comparator());
        assertNotSame(reversed, map.get("k"));

        final Map<String, Set<Integer>> map2 = mm.toMap(LinkedHashMap::new);
        assertEquals(List.of(3, 2, 1), new ArrayList<>(map2.get("k")));

        // independent copies
        map.get("k").clear();
        assertEquals(3, reversed.size());
    }

    @Test
    public void testC352_toMap_nonComparableElementsUnderComparator_noClassCastException() {
        final TreeSet<Box> boxes = new TreeSet<>(Comparator.comparingInt(Box::v));
        boxes.add(new Box(2));
        boxes.add(new Box(1));
        final Map<String, Set<Box>> backing = new HashMap<>();
        backing.put("k", boxes);
        final SetMultimap<String, Box> mm = SetMultimap.wrap(backing);

        assertEquals(List.of(new Box(1), new Box(2)), new ArrayList<>(mm.toMap().get("k")));
        assertEquals(List.of(new Box(1), new Box(2)), new ArrayList<>(mm.toMap(HashMap::new).get("k")));
        assertEquals(mm.copy().toMap(), mm.toMap());
    }

    @Test
    public void testC352_toMap_caseInsensitiveOrderKept_unicode() {
        final TreeSet<String> ci = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        ci.addAll(List.of("Ωmega", "B", "a"));
        final Map<String, Set<String>> backing = new LinkedHashMap<>();
        backing.put("k", ci);
        final SetMultimap<String, String> mm = SetMultimap.wrap(backing);

        final List<String> expected = List.of("a", "B", "Ωmega");
        assertEquals(expected, new ArrayList<>(mm.toMap().get("k")));
        assertEquals(expected, new ArrayList<>(mm.toMap(n -> new HashMap<>()).get("k")));
        assertTrue(mm.toMap().get("k").contains("A"), "case-insensitive membership survives the copy");
    }

    @Test
    public void testC352_toMap_emptyKeyAndEmptyMultimap() {
        final Map<String, Set<Integer>> backing = new LinkedHashMap<>();
        backing.put("k", new TreeSet<>(Set.of(1)));
        final SetMultimap<String, Integer> mm = SetMultimap.wrap(backing);
        backing.put("empty", new TreeSet<>(Comparator.reverseOrder()));

        final Map<String, Set<Integer>> map = mm.toMap();
        assertTrue(map.get("empty").isEmpty());
        assertEquals(2, map.size());

        assertTrue(N.<String, Integer> newSetMultimap().toMap().isEmpty());
        assertTrue(N.<String, Integer> newListMultimap().toMap(HashMap::new).isEmpty());
    }

    @Test
    public void testC352_toMap_listMultimapUnchanged() {
        final ListMultimap<String, Integer> mm = listOf("a", 1, "a", 1, "b", 2);
        final Map<String, List<Integer>> map = mm.toMap();

        assertEquals(Map.of("a", List.of(1, 1), "b", List.of(2)), map);
        assertNotSame(mm.get("a"), map.get("a"));
    }

    // ---------------------------------------------------------------- C-353

    @Test
    public void testC353_replaceEntry_linkedSet_duplicateNewValue_keepsOrder() {
        final SetMultimap<String, Integer> mm = N.newLinkedSetMultimap();
        mm.putValues("k", List.of(1, 2, 3));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", 1, 3));
        assertTrue(ex.getMessage().contains("Failed to add"), ex.getMessage());
        assertEquals(List.of(1, 2, 3), new ArrayList<>(mm.get("k")));

        final Multimap<String, Integer, Set<Integer>> base = N.newMultimap(HashMap::new, LinkedHashSet::new);
        base.putValues("k", List.of(1, 2, 3));
        assertThrows(IllegalStateException.class, () -> base.replaceEntry("k", 2, 1));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(base.get("k")));

        // replaceEntriesIf goes through the same helper
        assertThrows(IllegalStateException.class, () -> mm.replaceEntriesIf(k -> true, 1, 2));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(mm.get("k")));
    }

    @Test
    public void testC353_replaceEntry_linkedSet_nullValues() {
        final SetMultimap<String, Integer> mm = N.newLinkedSetMultimap();
        mm.putValues("k", Arrays.asList(null, 1, 2));

        assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", null, 2));
        assertEquals(Arrays.asList(null, 1, 2), new ArrayList<>(mm.get("k")));

        assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", 1, null));
        assertEquals(Arrays.asList(null, 1, 2), new ArrayList<>(mm.get("k")));

        assertTrue(mm.replaceEntry("k", null, 9));
        assertEquals(Arrays.asList(1, 2, 9), new ArrayList<>(mm.get("k")));
    }

    @Test
    public void testC353_replaceEntry_absentOldValue_presentNewValue_returnsFalse() {
        final SetMultimap<String, Integer> mm = N.newLinkedSetMultimap();
        mm.putValues("k", List.of(1, 2, 3));

        assertFalse(mm.replaceEntry("k", 7, 3));
        assertFalse(mm.replaceEntry("missing", 1, 2));
        assertEquals(List.of(1, 2, 3), new ArrayList<>(mm.get("k")));
    }

    @Test
    public void testC353_replaceEntry_sameValueAndComparatorEquivalentValue_stillSucceed() {
        final SetMultimap<String, Integer> mm = N.newLinkedSetMultimap();
        mm.putValues("k", List.of(1, 2));
        assertTrue(mm.replaceEntry("k", 1, 1));
        assertEquals(Set.of(1, 2), mm.get("k"));

        // "a" -> "A" under CASE_INSENSITIVE_ORDER: the new value is 'contained' only as the old value itself.
        final Multimap<String, String, TreeSet<String>> ci = N.newMultimap(HashMap::new, () -> new TreeSet<>(String.CASE_INSENSITIVE_ORDER));
        ci.putValues("k", List.of("a", "é"));
        assertTrue(ci.replaceEntry("k", "a", "A"));
        assertTrue(ci.replaceEntry("k", "é", "É"));
        assertEquals(List.of("A", "É"), new ArrayList<>(ci.get("k")));

        // and a genuine duplicate under the comparator is refused without change
        ci.put("k", "b");
        assertThrows(IllegalStateException.class, () -> ci.replaceEntry("k", "A", "B"));
        assertEquals(List.of("A", "b", "É"), new ArrayList<>(ci.get("k")));
    }

    @Test
    public void testC353_replaceEntry_treeSetNullNewValue_stillWrapsNpe() {
        final Multimap<String, Integer, TreeSet<Integer>> mm = N.newMultimap(HashMap::new, TreeSet::new);
        mm.putValues("k", List.of(1, 2));

        final IllegalStateException ex = assertThrows(IllegalStateException.class, () -> mm.replaceEntry("k", 1, null));
        assertTrue(ex.getCause() instanceof NullPointerException);
        assertEquals(List.of(1, 2), new ArrayList<>(mm.get("k")));
    }

    // ---------------------------------------------------------------- C-354

    @Test
    public void testC354_replaceAll_laterFailure_keepsEarlierRemoval_inPlaceEmptied() {
        final Multimap<String, Integer, TreeSet<Integer>> mm = N.newMultimap(LinkedHashMap::new, TreeSet::new);
        mm.putValues("a", List.of(1, 2));
        mm.put("b", 3);

        assertThrows(IllegalStateException.class, () -> mm.replaceAll((k, v) -> {
            if ("a".equals(k)) {
                v.clear();
                return v;
            }

            final TreeSet<Integer> bad = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
            bad.add(null);
            return bad;
        }));

        assertFalse(mm.containsKey("a"), "the key emptied in place must be removed even though a later key failed");
        assertNull(mm.get("a"));
        assertEquals(List.of(3), new ArrayList<>(mm.get("b")));
        assertEquals(1, mm.keyCount());
        assertEquals(1, mm.totalValueCount());
    }

    @Test
    public void testC354_replaceAll_laterFailure_keepsEarlierNullRemoval() {
        final Multimap<String, Integer, TreeSet<Integer>> mm = N.newMultimap(LinkedHashMap::new, TreeSet::new);
        mm.putValues("a", List.of(1, 2));
        mm.put("b", 3);

        assertThrows(IllegalStateException.class, () -> mm.replaceAll((k, v) -> {
            if ("a".equals(k)) {
                return null;
            }

            final TreeSet<Integer> bad = new TreeSet<>(Comparator.nullsFirst(Comparator.naturalOrder()));
            bad.add(null);
            return bad;
        }));

        assertFalse(mm.containsKey("a"));
        assertEquals(List.of(3), new ArrayList<>(mm.get("b")));
    }

    @Test
    public void testC354_replaceAll_normalPaths() {
        final ListMultimap<String, Integer> mm = listOf("a", 1, "b", 2, "c", 3);
        final List<Integer> bLive = mm.get("b");

        mm.replaceAll((k, v) -> "a".equals(k) ? Collections.emptyList() : "b".equals(k) ? List.of(20, 21) : v);

        assertFalse(mm.containsKey("a"));
        assertSame(bLive, mm.get("b"));
        assertEquals(List.of(20, 21), mm.get("b"));
        assertEquals(List.of(3), mm.get("c"));

        final ListMultimap<String, Integer> empty = N.newListMultimap();
        empty.replaceAll((k, v) -> null);
        assertTrue(empty.isEmpty());

        mm.replaceAll((k, v) -> null);
        assertTrue(mm.isEmpty());
    }

    // ---------------------------------------------------------------- C-355

    @Test
    public void testC355_listCopy_sharedSupplierResult_IAE() {
        final List<Integer> shared = new ArrayList<>();
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", new ArrayList<>(List.of(1)));
        backing.put("b", new ArrayList<>(List.of(2)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> shared);

        assertThrows(IllegalArgumentException.class, mm::copy);
        assertThrows(IllegalArgumentException.class, mm::toMap);
        assertThrows(IllegalArgumentException.class, () -> mm.toMap(HashMap::new));
        assertEquals(List.of(1), mm.get("a"));
        assertEquals(List.of(2), mm.get("b"));
        assertEquals(List.of(1), shared, "only the first copy was filled");
    }

    @Test
    public void testC355_baseCopy_nonEmptySupplierResult_IAE() {
        final AtomicInteger calls = new AtomicInteger();
        final Supplier<List<Integer>> supplier = () -> calls.incrementAndGet() == 1 ? new ArrayList<>() : new ArrayList<>(List.of(99));
        final Multimap<String, Integer, List<Integer>> mm = N.newMultimap(LinkedHashMap::new, supplier);
        mm.put("a", 1);

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, mm::copy);
        assertTrue(ex.getMessage().contains("non-empty"), ex.getMessage());
        assertEquals(List.of(1), mm.get("a"));
    }

    @Test
    public void testC355_copy_supplierReturnsLiveSourceCollection_IAE_sourceUntouched() {
        final List<Integer> live = new ArrayList<>(List.of(1));
        final Map<String, List<Integer>> backing = new LinkedHashMap<>();
        backing.put("a", live);
        backing.put("b", new ArrayList<>(List.of(2)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> live);

        assertThrows(IllegalArgumentException.class, mm::copy);
        assertEquals(List.of(1), live);
        assertEquals(List.of(2), mm.get("b"));
    }

    @Test
    public void testC355_copy_validSuppliers_emptyKey_equalsSource() {
        final ListMultimap<String, Integer> mm = listOf("a", 1, "b", 2);
        mm.get("b").clear();

        final ListMultimap<String, Integer> copy = mm.copy();
        assertEquals(mm, copy);
        assertTrue(copy.containsKey("b"));
        assertNotSame(mm.get("a"), copy.get("a"));

        assertTrue(N.<String, Integer> newListMultimap().copy().isEmpty());
    }

    // ---------------------------------------------------------------- C-356

    @Test
    public void testC356_listWrap_nullValueSupplier_NPE_everyCreationSite() {
        final Map<String, List<Integer>> backing = new HashMap<>();
        backing.put("x", new ArrayList<>(List.of(1)));
        final ListMultimap<String, Integer> mm = ListMultimap.wrap(backing, () -> null);

        final NullPointerException ex = assertThrows(NullPointerException.class, () -> mm.put("y", 2));
        assertEquals("valueSupplier returned null", ex.getMessage());
        assertThrows(NullPointerException.class, () -> mm.putValues("y", List.of(2)));
        assertThrows(NullPointerException.class, () -> mm.putIfKeyAbsent("y", 2));
        assertThrows(NullPointerException.class, () -> mm.putIfValueAbsent("y", 2));
        assertThrows(NullPointerException.class, () -> mm.putValuesIfKeyAbsent("y", List.of(2)));
        assertThrows(NullPointerException.class, () -> mm.putAll(Map.of("y", 2)));
        assertThrows(NullPointerException.class, () -> mm.putValues(Map.of("y", List.of(2))));
        assertThrows(NullPointerException.class, () -> mm.putValues(listOf("y", 2)));
        assertThrows(NullPointerException.class, () -> mm.compute("y", (k, v) -> new ArrayList<>(List.of(2))));
        assertThrows(NullPointerException.class, () -> mm.merge("y", 2, (v, e) -> v));
        assertThrows(NullPointerException.class, mm::copy);
        assertThrows(NullPointerException.class, mm::toMap);
        assertThrows(NullPointerException.class, () -> mm.toMap(HashMap::new));
        assertFalse(mm.containsKey("y"));

        // existing key: no supplier call, still works; empty input: nothing is created
        assertTrue(mm.put("x", 3));
        assertFalse(mm.putValues("y", List.of()));
        assertEquals(List.of(1, 3), mm.get("x"));
    }

    @Test
    public void testC356_baseAndSetMultimap_nullValueSupplier_NPE() {
        final Multimap<String, Integer, List<Integer>> base = N.newMultimap(HashMap::new, () -> null);
        assertThrows(NullPointerException.class, () -> base.put("a", 1));
        assertTrue(base.isEmpty());
        assertTrue(base.copy().isEmpty(), "nothing to copy: the supplier is never called");

        final Map<String, Set<String>> backing = new HashMap<>();
        backing.put("x", new HashSet<>(Set.of("ü")));
        final SetMultimap<String, String> set = SetMultimap.wrap(backing, () -> null);
        assertThrows(NullPointerException.class, () -> set.put("y", "ü"));
        // a known HashSet policy copies without the supplier
        assertEquals(Set.of("ü"), set.copy().get("x"));
        assertEquals(Set.of("ü"), set.toMap().get("x"));
    }

    // ---------------------------------------------------------------- C-357 (doc: documented disambiguations compile)

    @Test
    public void testC357_documentedMergeDisambiguations() {
        final ListMultimap<String, Object> mm = N.newListMultimap();
        mm.put("k", 1);
        final List<Integer> list = List.of(2, 3);

        final List<Object> single = mm.merge("k", (Object) list, (o, x) -> {
            final List<Object> r = new ArrayList<>(o);
            r.add(x);
            return r;
        });
        assertEquals(List.of(1, list), single);

        final BiFunction<List<Object>, Collection<Integer>, List<Object>> union = (o, x) -> {
            final List<Object> r = new ArrayList<>(o);
            r.addAll(x);
            return r;
        };
        assertEquals(List.of(1, list, 2, 3), mm.merge("k", list, union));
    }

    // ---------------------------------------------------------------- C-358

    @Test
    public void testC358_invert_hintIsKeyCount_notTotalValueCount() {
        final ListMultimap<String, Integer> mm = N.newListMultimap();
        mm.putValues("k", Collections.nCopies(100_000, 7));
        final AtomicInteger hint = new AtomicInteger(-1);

        final ListMultimap<Integer, String> inv = mm.invert(n -> {
            hint.set(n);
            return N.newListMultimap(n);
        });

        assertEquals(1, hint.get());
        assertEquals(1, inv.keyCount());
        assertEquals(100_000, inv.get(7).size());

        final ListMultimap<String, Integer> three = listOf("a", 1, "b", 1, "c", 2);
        three.invert(n -> {
            hint.set(n);
            return N.newListMultimap(n);
        });
        assertEquals(3, hint.get());

        N.<String, Integer> newListMultimap().invert(n -> {
            hint.set(n);
            return N.newListMultimap(n);
        });
        assertEquals(0, hint.get());
    }
}
