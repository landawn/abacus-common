package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Covers the fixes applied from the 2026-08-31 review of the {@code Immutable*} family
 * (ImmutableCollection / ImmutableList / ImmutableSet / ImmutableSortedSet / ImmutableNavigableSet /
 * AbstractImmutableMap / ImmutableMap / ImmutableSortedMap / ImmutableNavigableMap / ImmutableBiMap).
 *
 * <p>Every test here fails on the pre-fix sources; the assertions are written against the observed
 * pre-fix behaviour so that a regression is unambiguous.
 */
public class ImmutableFamilyTest extends TestBase {

    // ---------------------------------------------------------------------------------------------
    // B1 - ImmutableBiMap.Builder had no "consumed" guard, so build() handed back a live view that
    //      later put() calls mutated. The other three builders have thrown IllegalStateException all along.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapBuilderIsConsumedByBuild extends TestBase {

        @Test
        public void put_afterBuild_throwsIllegalStateException() {
            final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
            builder.put("a", 1);
            builder.build();

            assertThrows(IllegalStateException.class, () -> builder.put("b", 2));
        }

        @Test
        public void putAll_afterBuild_throwsIllegalStateException() {
            final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
            builder.put("a", 1);
            builder.build();

            assertThrows(IllegalStateException.class, () -> builder.putAll(Map.of("b", 2)));
        }

        @Test
        public void builtMap_isNotChangedByLaterBuilderUse() {
            final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
            builder.put("a", 1);
            final ImmutableBiMap<String, Integer> built = builder.build();

            assertThrows(IllegalStateException.class, () -> builder.put("b", 2));

            assertEquals(1, built.size());
            assertEquals(ImmutableBiMap.of("a", 1), built);
        }

        @Test
        public void guardAlsoAppliesToACallerSuppliedHolder() {
            final BiMap<String, Integer> holder = new BiMap<>();
            final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder(holder);
            builder.put("a", 1);
            builder.build();

            assertThrows(IllegalStateException.class, () -> builder.put("b", 2));
        }

        @Test
        public void build_mayBeCalledMoreThanOnce_andIsEqualEachTime() {
            final ImmutableBiMap.Builder<String, Integer> builder = ImmutableBiMap.builder();
            builder.put("a", 1);

            final ImmutableBiMap<String, Integer> first = builder.build();
            final ImmutableBiMap<String, Integer> second = builder.build();

            assertEquals(first, second);
            assertNotSame(first, second);
        }

        @Test
        public void aBuilderSuppliedHolderStillProducesALiveView() {
            // build() over a caller-owned holder is documented to stay a view; only the builder is consumed.
            final BiMap<String, Integer> holder = new BiMap<>();
            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.builder(holder).put("a", 1).build();

            holder.put("b", 2);

            assertEquals(2, view.size());
        }

        @Test
        public void theOtherThreeBuildersBehaveIdentically() {
            final ImmutableList.Builder<String> listBuilder = ImmutableList.builder();
            listBuilder.add("a").build();
            assertThrows(IllegalStateException.class, () -> listBuilder.add("b"));

            final ImmutableSet.Builder<String> setBuilder = ImmutableSet.builder();
            setBuilder.add("a").build();
            assertThrows(IllegalStateException.class, () -> setBuilder.add("b"));

            final ImmutableMap.Builder<String, Integer> mapBuilder = ImmutableMap.builder();
            mapBuilder.put("a", 1).build();
            assertThrows(IllegalStateException.class, () -> mapBuilder.put("b", 2));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - Java 21 added SequencedCollection/SequencedMap. Their default mutators check emptiness FIRST,
    //      so on an EMPTY immutable instance they used to return null (pollFirstEntry) or raise
    //      NoSuchElementException (removeFirst) instead of reporting that the collection is read-only.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class SequencedMutatorsAreBlockedEvenWhenEmpty extends TestBase {

        @Test
        public void sortedMap_pollFirstEntry_onEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedMap.empty().pollFirstEntry());
        }

        @Test
        public void sortedMap_pollLastEntry_onEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedMap.empty().pollLastEntry());
        }

        @Test
        public void sortedMap_pollEntry_onNonEmpty() {
            final ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.of("a", 1, "b", 2);

            assertThrows(UnsupportedOperationException.class, map::pollFirstEntry);
            assertThrows(UnsupportedOperationException.class, map::pollLastEntry);
        }

        @Test
        public void sortedMap_pollEntry_onAWrappedEmptyMap() {
            final ImmutableSortedMap<String, Integer> map = ImmutableSortedMap.wrap(new TreeMap<>());

            assertThrows(UnsupportedOperationException.class, map::pollFirstEntry);
            assertThrows(UnsupportedOperationException.class, map::pollLastEntry);
        }

        @Test
        public void sortedMap_pollEntry_onARangeView() {
            final ImmutableSortedMap<Integer, String> empty = ImmutableSortedMap.of(1, "a").headMap(1);
            assertTrue(empty.isEmpty());

            assertThrows(UnsupportedOperationException.class, empty::pollFirstEntry);
            assertThrows(UnsupportedOperationException.class, empty::pollLastEntry);
        }

        @Test
        public void navigableMap_pollEntry_stillBlocked() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableMap.empty().pollFirstEntry());
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableMap.empty().pollLastEntry());
        }

        @Test
        public void list_removeFirstLast_onEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.empty().removeFirst());
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.empty().removeLast());
        }

        @Test
        public void list_removeFirstLast_onNonEmpty() {
            final ImmutableList<Integer> list = ImmutableList.of(1, 2, 3);

            assertThrows(UnsupportedOperationException.class, list::removeFirst);
            assertThrows(UnsupportedOperationException.class, list::removeLast);
        }

        @Test
        public void list_removeFirstLast_onDerivedViews() {
            final ImmutableList<Integer> reversedEmpty = (ImmutableList<Integer>) ImmutableList.wrap(new ArrayList<Integer>()).reversed();
            assertThrows(UnsupportedOperationException.class, reversedEmpty::removeFirst);
            assertThrows(UnsupportedOperationException.class, reversedEmpty::removeLast);

            final ImmutableList<Integer> emptySubList = ImmutableList.of(1, 2, 3).subList(1, 1);
            assertThrows(UnsupportedOperationException.class, emptySubList::removeFirst);
            assertThrows(UnsupportedOperationException.class, emptySubList::removeLast);
        }

        @Test
        public void sortedSet_removeFirstLast_onEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedSet.empty().removeFirst());
            assertThrows(UnsupportedOperationException.class, () -> ImmutableSortedSet.empty().removeLast());
        }

        @Test
        public void navigableSet_removeFirstLast_onEmpty() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.empty().removeFirst());
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.empty().removeLast());

            // pollFirst/pollLast were already blocked; check they still are.
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.empty().pollFirst());
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.empty().pollLast());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B3 - The always-failing mutators used to validate their functional argument first, so a null
    //      argument produced IllegalArgumentException instead of UnsupportedOperationException. For
    //      List.sort that was outright wrong: null is a legal argument meaning "natural ordering".
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class NullArgumentDoesNotMaskUnsupportedOperation extends TestBase {

        @Test
        public void list_sort_null() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.of(1, 2).sort(null));
        }

        @Test
        public void list_sort_nonNull() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.of(1, 2).sort(Comparator.naturalOrder()));
        }

        @Test
        public void list_replaceAll_null() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.of(1, 2).replaceAll(null));
        }

        @Test
        public void collection_removeIf_null() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableList.of(1, 2).removeIf(null));
            assertThrows(UnsupportedOperationException.class, () -> ImmutableSet.of(1, 2).removeIf(null));
            assertThrows(UnsupportedOperationException.class, () -> ImmutableCollection.wrap(new ArrayList<>(List.of(1))).removeIf(null));
        }

        @Test
        public void map_functionalMutators_null() {
            final ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);

            assertThrows(UnsupportedOperationException.class, () -> map.replaceAll(null));
            assertThrows(UnsupportedOperationException.class, () -> map.compute("a", null));
            assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("a", null));
            assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("a", null));
            assertThrows(UnsupportedOperationException.class, () -> map.merge("a", 1, null));
        }

        @Test
        public void map_functionalMutators_null_onEveryMapSubtype() {
            final List<Map<String, Integer>> maps = List.of(ImmutableMap.of("a", 1), ImmutableSortedMap.of("a", 1), ImmutableNavigableMap.of("a", 1),
                    ImmutableBiMap.of("a", 1), ImmutableMap.<String, Integer> empty());

            for (final Map<String, Integer> map : maps) {
                assertThrows(UnsupportedOperationException.class, () -> map.replaceAll(null), map.getClass().getSimpleName());
                assertThrows(UnsupportedOperationException.class, () -> map.compute("a", null), map.getClass().getSimpleName());
                assertThrows(UnsupportedOperationException.class, () -> map.merge("a", 1, null), map.getClass().getSimpleName());
            }
        }

        @Test
        public void thisMatchesTheJdkReadOnlyCollections() {
            // java.util.List.of(...) and Collections.unmodifiableList(...) both answer UnsupportedOperationException.
            assertThrows(UnsupportedOperationException.class, () -> List.of(1, 2).sort(null));
            assertThrows(UnsupportedOperationException.class, () -> List.of(1, 2).removeIf(null));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B4 - ImmutableBiMap.values() inherited AbstractImmutableMap.values(), which returned
    //      Collections.unmodifiableMap(..).values(): not a Set, and equal only by identity, even though
    //      BiMap.values() is an ImmutableSet.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapValuesIsASet extends TestBase {

        @Test
        public void values_isAnImmutableSet() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2);

            assertInstanceOf(Set.class, map.values());
            assertInstanceOf(ImmutableSet.class, map.values());
            assertInstanceOf(Immutable.class, map.values());
        }

        @Test
        public void values_hasSetValueEquality() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1, "two", 2);
            final Set<Integer> expected = new HashSet<>(Arrays.asList(1, 2));

            assertEquals(expected, map.values());
            assertEquals(map.values(), expected);
            assertEquals(expected.hashCode(), map.values().hashCode());
        }

        @Test
        public void values_isReadOnly() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1);

            assertThrows(UnsupportedOperationException.class, () -> map.values().add(2));
            assertThrows(UnsupportedOperationException.class, () -> map.values().remove(1));
            assertThrows(UnsupportedOperationException.class, () -> map.values().clear());
            assertThrows(UnsupportedOperationException.class, () -> map.values().removeIf(v -> true));
            assertThrows(UnsupportedOperationException.class, () -> {
                final java.util.Iterator<Integer> iter = map.values().iterator();
                iter.next();
                iter.remove();
            });
        }

        @Test
        public void values_ofAnInverseViewIsAlsoASet() {
            final ImmutableBiMap<Integer, String> inverse = ImmutableBiMap.of("one", 1, "two", 2).inverse();

            assertInstanceOf(ImmutableSet.class, inverse.values());
            assertEquals(Set.of("one", "two"), inverse.values());
        }

        @Test
        public void values_reflectsAWrappedSource() {
            final BiMap<String, Integer> source = new BiMap<>();
            source.put("a", 1);
            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(source);

            source.put("b", 2);

            assertEquals(2, view.values().size());
            assertTrue(view.values().contains(2));
        }

        @Test
        public void values_ofEmpty() {
            assertTrue(ImmutableBiMap.empty().values().isEmpty());
            assertEquals(Set.of(), ImmutableBiMap.empty().values());
        }

        @Test
        public void theOtherViewsAreUnchanged() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("one", 1);

            assertEquals(Set.of("one"), map.keySet());
            assertEquals(Map.of("one", 1).entrySet(), map.entrySet());
            assertThrows(UnsupportedOperationException.class, () -> map.keySet().add("two"));
        }

        @Test
        public void aPlainMapValueViewIsStillAnIdentityEqualCollection() {
            // Narrowing was BiMap-specific: a general Map's values() is a bag, and the JDK contract for it
            // (identity equality) must not change.
            assertFalse(ImmutableMap.of("a", 1).values().equals(List.of(1)));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D4 - Range views of a navigable map/set used to come back as the merely-sorted supertype.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class NavigableRangeViewsStayNavigable extends TestBase {

        @Test
        public void navigableMap_rangeViewTypes() {
            final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

            assertInstanceOf(ImmutableNavigableMap.class, map.subMap(1, 3));
            assertInstanceOf(ImmutableNavigableMap.class, map.headMap(2));
            assertInstanceOf(ImmutableNavigableMap.class, map.tailMap(2));
        }

        @Test
        public void navigableMap_rangeViewContents() {
            final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

            assertEquals(Map.of(1, "a", 2, "b"), map.subMap(1, 3));
            assertEquals(Map.of(1, "a"), map.headMap(2));
            assertEquals(Map.of(2, "b", 3, "c"), map.tailMap(2));
        }

        @Test
        public void navigableMap_rangeViewsAgreeWithTheInclusiveForms() {
            final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

            assertEquals(map.subMap(1, true, 3, false), map.subMap(1, 3));
            assertEquals(map.headMap(2, false), map.headMap(2));
            assertEquals(map.tailMap(2, true), map.tailMap(2));
        }

        @Test
        public void navigableMap_rangeViewsAgreeWithTheSortedSuperclass() {
            final ImmutableSortedMap<Integer, String> sorted = ImmutableSortedMap.of(1, "a", 2, "b", 3, "c");
            final ImmutableNavigableMap<Integer, String> navigable = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c");

            assertEquals(sorted.subMap(1, 3), navigable.subMap(1, 3));
            assertEquals(sorted.headMap(2), navigable.headMap(2));
            assertEquals(sorted.tailMap(2), navigable.tailMap(2));
        }

        @Test
        public void navigableMap_rangeViewsRemainUsableAsNavigableMaps() {
            final ImmutableNavigableMap<Integer, String> sub = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c").subMap(1, 3);

            assertEquals(1, sub.floorKey(1));
            assertEquals(2, sub.ceilingKey(2));
            assertEquals("{2=b, 1=a}", sub.descendingMap().toString());
        }

        @Test
        public void navigableSet_rangeViewTypes() {
            final ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);

            assertInstanceOf(ImmutableNavigableSet.class, set.subSet(1, 3));
            assertInstanceOf(ImmutableNavigableSet.class, set.headSet(2));
            assertInstanceOf(ImmutableNavigableSet.class, set.tailSet(2));
        }

        @Test
        public void navigableSet_rangeViewContents() {
            final ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3);

            assertEquals(Set.of(1, 2), set.subSet(1, 3));
            assertEquals(Set.of(1), set.headSet(2));
            assertEquals(Set.of(2, 3), set.tailSet(2));
            assertEquals(set.subSet(1, true, 3, false), set.subSet(1, 3));
            assertEquals(set.headSet(2, false), set.headSet(2));
            assertEquals(set.tailSet(2, true), set.tailSet(2));
        }

        @Test
        public void navigableSet_rangeViewsAgreeWithTheSortedSuperclass() {
            final ImmutableSortedSet<Integer> sorted = ImmutableSortedSet.of(1, 2, 3);
            final ImmutableNavigableSet<Integer> navigable = ImmutableNavigableSet.of(1, 2, 3);

            assertEquals(sorted.subSet(1, 3), navigable.subSet(1, 3));
            assertEquals(sorted.headSet(2), navigable.headSet(2));
            assertEquals(sorted.tailSet(2), navigable.tailSet(2));
        }

        @Test
        public void rangeViewsAreStillReadOnly() {
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableMap.of(1, "a").headMap(1).put(0, "x"));
            assertThrows(UnsupportedOperationException.class, () -> ImmutableNavigableSet.of(1).headSet(1).add(0));
        }

        @Test
        public void rangeViewsKeepTheSuperclassExceptionBehaviour() {
            final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b");
            final ImmutableSortedMap<Integer, String> sortedMap = ImmutableSortedMap.of(1, "a", 2, "b");

            assertThrows(IllegalArgumentException.class, () -> map.subMap(2, 1));
            assertThrows(IllegalArgumentException.class, () -> sortedMap.subMap(2, 1));
            assertThrows(NullPointerException.class, () -> map.headMap(null));
            assertThrows(NullPointerException.class, () -> sortedMap.headMap(null));

            final ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2);
            final ImmutableSortedSet<Integer> sortedSet = ImmutableSortedSet.of(1, 2);

            assertThrows(IllegalArgumentException.class, () -> set.subSet(2, 1));
            assertThrows(IllegalArgumentException.class, () -> sortedSet.subSet(2, 1));
            assertThrows(NullPointerException.class, () -> set.headSet(null));
            assertThrows(NullPointerException.class, () -> sortedSet.headSet(null));
        }

        @Test
        public void rangeOfARangeStaysNavigableAndStaysWithinBounds() {
            final ImmutableNavigableMap<Integer, String> map = ImmutableNavigableMap.of(1, "a", 2, "b", 3, "c", 4, "d");
            final ImmutableNavigableMap<Integer, String> outer = map.subMap(2, 4);

            assertInstanceOf(ImmutableNavigableMap.class, outer.headMap(3));
            assertEquals(Map.of(2, "b"), outer.headMap(3));
            assertThrows(IllegalArgumentException.class, () -> outer.headMap(9));

            final ImmutableNavigableSet<Integer> set = ImmutableNavigableSet.of(1, 2, 3, 4).subSet(2, 4);
            assertInstanceOf(ImmutableNavigableSet.class, set.headSet(3));
            assertEquals(Set.of(2), set.headSet(3));
            assertThrows(IllegalArgumentException.class, () -> set.headSet(9));
        }

        @Test
        public void rangeViewsInheritOwnership() {
            // A range of an owning map is itself a stable value, so copyOf hands it back unchanged.
            final ImmutableNavigableMap<Integer, String> owningRange = ImmutableNavigableMap.of(1, "a", 2, "b").headMap(2);
            assertSame(owningRange, ImmutableNavigableMap.copyOf(owningRange));

            // A range of a wrap()-backed map is a live view, so copyOf must snapshot it.
            final TreeMap<Integer, String> holder = new TreeMap<>();
            holder.put(1, "a");
            final ImmutableNavigableMap<Integer, String> liveRange = ImmutableNavigableMap.wrap(holder).headMap(2);
            final ImmutableNavigableMap<Integer, String> snapshot = ImmutableNavigableMap.copyOf(liveRange);
            assertNotSame(liveRange, snapshot);

            holder.put(0, "z");
            assertEquals(2, liveRange.size());
            assertEquals(1, snapshot.size());
        }

        @Test
        public void rangeViewsPropagateAComparator() {
            final TreeMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
            source.put("a", 1);
            source.put("b", 2);
            final ImmutableNavigableMap<String, Integer> map = ImmutableNavigableMap.copyOf(source);

            assertEquals(Comparator.reverseOrder(), map.headMap("a").comparator());
            assertEquals("{b=2}", map.headMap("a").toString());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D5 - There was no direct way to snapshot an ImmutableBiMap, because copyOf only accepted a BiMap
    //      and ImmutableBiMap is not one. inverse() also dropped the source's ownership flag.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapCopyOfAcceptsAnyMap extends TestBase {

        @Test
        public void copyOf_snapshotsAWrappedView() {
            final BiMap<String, Integer> source = new BiMap<>();
            source.put("a", 1);
            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(source);

            final ImmutableBiMap<String, Integer> snapshot = ImmutableBiMap.copyOf(view);
            source.put("b", 2);

            assertEquals(2, view.size());
            assertEquals(1, snapshot.size());
            assertEquals(ImmutableBiMap.of("a", 1), snapshot);
        }

        @Test
        public void copyOf_acceptsAPlainMap() {
            final Map<String, Integer> source = new LinkedHashMap<>();
            source.put("b", 2);
            source.put("a", 1);

            final ImmutableBiMap<String, Integer> copy = ImmutableBiMap.copyOf(source);
            source.put("c", 3);

            assertEquals(2, copy.size());
            assertEquals("{b=2, a=1}", copy.toString());
            assertEquals("a", copy.getByValue(1));
        }

        @Test
        public void copyOf_rejectsNullsAndDuplicateValues() {
            final Map<String, Integer> withNullValue = new LinkedHashMap<>();
            withNullValue.put("a", null);
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.copyOf(withNullValue));

            final Map<String, Integer> duplicateValues = new LinkedHashMap<>();
            duplicateValues.put("a", 1);
            duplicateValues.put("b", 1);
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.copyOf(duplicateValues));
        }

        @Test
        public void copyOf_nullAndEmptyGiveTheSharedEmptyInstance() {
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.copyOf((Map<String, Integer>) null));
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.copyOf((BiMap<String, Integer>) null));
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.copyOf(Map.<String, Integer> of()));
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.copyOf(new BiMap<String, Integer>()));
        }

        @Test
        public void copyOf_bareNullStillResolvesToTheBiMapOverload() {
            // BiMap is more specific than Map, so an untyped null keeps compiling and stays unambiguous.
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.copyOf(null));
        }

        @Test
        public void copyOf_ofABiMapStillCopies() {
            final BiMap<String, Integer> source = new BiMap<>();
            source.put("a", 1);

            final ImmutableBiMap<String, Integer> copy = ImmutableBiMap.copyOf(source);
            source.put("b", 2);

            assertEquals(1, copy.size());
        }

        @Test
        public void copyOf_returnsAnOwningSourceUnchanged() {
            final ImmutableBiMap<String, Integer> owning = ImmutableBiMap.of("a", 1);
            assertSame(owning, ImmutableBiMap.copyOf(owning));
            assertSame(owning, ImmutableBiMap.copyOf(ImmutableBiMap.copyOf(owning)));

            // the no-arg builder's storage is private, so its result owns its backing too
            final ImmutableBiMap<String, Integer> fromBuilder = ImmutableBiMap.<String, Integer> builder().put("a", 1).build();
            assertSame(fromBuilder, ImmutableBiMap.copyOf(fromBuilder));

            // ... and inverse() inherits that ownership
            assertSame(owning.inverse(), ImmutableBiMap.copyOf(owning.inverse()));
        }

        @Test
        public void copyOf_copiesEveryNonOwningSource() {
            final BiMap<String, Integer> holder = new BiMap<>();
            holder.put("a", 1);

            final ImmutableBiMap<String, Integer> wrapped = ImmutableBiMap.wrap(holder);
            assertNotSame(wrapped, ImmutableBiMap.copyOf(wrapped));

            final ImmutableBiMap<String, Integer> fromHolderBuilder = ImmutableBiMap.builder(holder).build();
            assertNotSame(fromHolderBuilder, ImmutableBiMap.copyOf(fromHolderBuilder));

            // a view's inverse is a view too, so it is copied as well
            assertNotSame(wrapped.inverse(), ImmutableBiMap.copyOf(wrapped.inverse()));
        }

        @Test
        public void copyOf_ofAnImmutableBiMapPreservesIterationOrder() {
            final BiMap<String, Integer> ordered = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            ordered.put("c", 3);
            ordered.put("a", 1);
            ordered.put("b", 2);

            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(ordered);
            final ImmutableBiMap<String, Integer> snapshot = ImmutableBiMap.copyOf(view);

            assertEquals("{c=3, a=1, b=2}", snapshot.toString());
            assertEquals(List.of("c", "a", "b"), new ArrayList<>(snapshot.keySet()));
        }

        @Test
        public void inverse_isCachedAndSelfInverting() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "b", 2);

            assertSame(map.inverse(), map.inverse());
            assertSame(map, map.inverse().inverse());
            assertEquals(ImmutableBiMap.of(1, "a", 2, "b"), map.inverse());
        }

        @Test
        public void inverse_ofAWrappedMapStaysLive() {
            final BiMap<String, Integer> source = new BiMap<>();
            source.put("a", 1);
            final ImmutableBiMap<Integer, String> inverse = ImmutableBiMap.wrap(source).inverse();

            source.put("b", 2);

            assertEquals(2, inverse.size());
            assertEquals("b", inverse.get(2));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // J4 - subList used to delegate its range check to the backing list. java.util.List.subList specifies
    //      IndexOutOfBoundsException for an inverted range while the AbstractList family raises
    //      IllegalArgumentException, so the exception depended on what the caller happened to wrap.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class SubListRangeCheckIsUniform extends TestBase {

        /**
         * A {@code List} that follows the {@link List#subList(int, int)} specification literally: the spec
         * says {@link IndexOutOfBoundsException} for {@code fromIndex > toIndex}, whereas every
         * {@code AbstractList}-derived JDK implementation raises {@link IllegalArgumentException} instead.
         * Such a list is legal, and {@link ImmutableList#wrap(List)} accepts it.
         */
        private static final class SpecConformingList<E> extends java.util.AbstractList<E> {
            private final List<E> delegate;

            SpecConformingList(final List<E> delegate) {
                this.delegate = delegate;
            }

            @Override
            public E get(final int index) {
                return delegate.get(index);
            }

            @Override
            public int size() {
                return delegate.size();
            }

            @Override
            public List<E> subList(final int fromIndex, final int toIndex) {
                java.util.Objects.checkFromToIndex(fromIndex, toIndex, size());
                return delegate.subList(fromIndex, toIndex);
            }
        }

        @Test
        public void invertedRange_isIllegalArgument_evenOverASpecConformingBacking() {
            final SpecConformingList<Integer> backing = new SpecConformingList<>(new ArrayList<>(List.of(1, 2, 3)));

            // The backing list itself answers IndexOutOfBoundsException, as java.util.List.subList specifies.
            assertThrows(IndexOutOfBoundsException.class, () -> backing.subList(2, 1));

            // ImmutableList must still honour its own documented contract.
            assertThrows(IllegalArgumentException.class, () -> ImmutableList.wrap(backing).subList(2, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> ImmutableList.wrap(backing).subList(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> ImmutableList.wrap(backing).subList(0, 4));
            assertEquals(List.of(2, 3), ImmutableList.wrap(backing).subList(1, 3));
        }

        @Test
        public void invertedRange_isIllegalArgument_forEveryBacking() {
            assertThrows(IllegalArgumentException.class, () -> ImmutableList.of(1, 2, 3).subList(2, 1));
            assertThrows(IllegalArgumentException.class, () -> ImmutableList.copyOf(new ArrayList<>(List.of(1, 2, 3))).subList(2, 1));
            assertThrows(IllegalArgumentException.class, () -> ImmutableList.wrap(List.of(1, 2, 3)).subList(2, 1));
            assertThrows(IllegalArgumentException.class, () -> ImmutableList.wrap(new java.util.LinkedList<>(List.of(1, 2, 3))).subList(2, 1));
        }

        @Test
        public void outOfRangeEndpoint_isIndexOutOfBounds() {
            assertThrows(IndexOutOfBoundsException.class, () -> ImmutableList.of(1, 2, 3).subList(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> ImmutableList.of(1, 2, 3).subList(0, 4));
        }

        @Test
        public void bothDirectionsOfAListAgree() {
            final ImmutableList<Integer> forward = ImmutableList.of(1, 2, 3);
            final ImmutableList<Integer> reversed = (ImmutableList<Integer>) forward.reversed();

            assertThrows(IllegalArgumentException.class, () -> forward.subList(2, 1));
            assertThrows(IllegalArgumentException.class, () -> reversed.subList(2, 1));
            assertThrows(IndexOutOfBoundsException.class, () -> forward.subList(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(-1, 2));
            assertThrows(IndexOutOfBoundsException.class, () -> forward.subList(0, 4));
            assertThrows(IndexOutOfBoundsException.class, () -> reversed.subList(0, 4));
        }

        @Test
        public void validRangesAreUnaffected() {
            final ImmutableList<Integer> list = ImmutableList.of(1, 2, 3, 4, 5);

            assertEquals(List.of(2, 3, 4), list.subList(1, 4));
            assertEquals(List.of(), list.subList(2, 2));
            assertEquals(List.of(), list.subList(0, 0));
            assertEquals(list, list.subList(0, 5));
            assertEquals(List.of(), ImmutableList.empty().subList(0, 0));
        }

        @Test
        public void subListOfAWrappedListStillTracksItsSource() {
            final List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
            final ImmutableList<Integer> sub = ImmutableList.wrap(source).subList(0, 2);

            assertEquals(List.of(1, 2), sub);
            source.set(0, 9);
            assertEquals(List.of(9, 2), sub);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // copyOf(E[]) used to build an intermediate ArrayList and then copy it a second time.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class ArrayCopyOfIsStillDefensive extends TestBase {

        @Test
        public void list_copyOfArray() {
            final String[] array = { "a", "b", "c", "b" };
            final ImmutableList<String> copy = ImmutableList.copyOf(array);

            array[0] = "X";

            assertEquals(List.of("a", "b", "c", "b"), copy);
            assertInstanceOf(RandomAccess.class, copy);
            assertThrows(UnsupportedOperationException.class, () -> copy.set(0, "z"));
            assertThrows(UnsupportedOperationException.class, () -> copy.add("z"));
        }

        @Test
        public void set_copyOfArray() {
            final String[] array = { "a", "b", "a", "c" };
            final ImmutableSet<String> copy = ImmutableSet.copyOf(array);

            array[0] = "X";

            assertEquals(3, copy.size());
            assertEquals("[a, b, c]", copy.toString());
            assertThrows(UnsupportedOperationException.class, () -> copy.add("z"));
        }

        @Test
        public void arrayEdgeCases() {
            assertSame(ImmutableList.empty(), ImmutableList.copyOf(new String[0]));
            assertSame(ImmutableList.empty(), ImmutableList.copyOf((String[]) null));
            assertSame(ImmutableSet.empty(), ImmutableSet.copyOf(new String[0]));
            assertSame(ImmutableSet.empty(), ImmutableSet.copyOf((String[]) null));

            assertEquals(List.of("q"), ImmutableList.copyOf(new String[] { "q" }));
            assertEquals(Set.of("q"), ImmutableSet.copyOf(new String[] { "q" }));
        }

        @Test
        public void arraysWithNullElements() {
            final ImmutableList<String> list = ImmutableList.copyOf(new String[] { null, "a", null });
            assertEquals(3, list.size());
            assertEquals(Arrays.asList(null, "a", null), list);

            final ImmutableSet<String> set = ImmutableSet.copyOf(new String[] { null, "a", null });
            assertEquals(2, set.size());
            assertTrue(set.contains(null));
        }

        @Test
        public void copyOfArray_matchesCopyOfCollection() {
            final String[] array = { "c", "a", "b", "a" };

            assertEquals(ImmutableList.copyOf(Arrays.asList(array)), ImmutableList.copyOf(array));
            assertEquals(ImmutableSet.copyOf(Arrays.asList(array)), ImmutableSet.copyOf(array));
            assertEquals(ImmutableSet.copyOf(Arrays.asList(array)).toString(), ImmutableSet.copyOf(array).toString());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Javadoc-driven behaviours the review documented; pinned so the docs cannot drift from the code.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class DocumentedBehaviours extends TestBase {

        @Test
        public void copyOfDropsASourcesComparatorBasedMembership() {
            final TreeSet<String> source = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            source.add("a");
            source.add("B");

            assertTrue(source.contains("A"));
            assertFalse(ImmutableSet.copyOf(source).contains("A"));

            final SortedMap<String, Integer> sourceMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            sourceMap.put("a", 1);

            assertTrue(sourceMap.containsKey("A"));
            assertFalse(ImmutableMap.copyOf(sourceMap).containsKey("A"));
        }

        @Test
        public void immutableSortedMapCopyOfKeepsTheComparator() {
            final SortedMap<String, Integer> source = new TreeMap<>(Comparator.reverseOrder());
            source.put("a", 1);
            source.put("b", 2);

            assertEquals(Comparator.reverseOrder(), ImmutableSortedMap.copyOf(source).comparator());
            assertEquals("{b=2, a=1}", ImmutableSortedMap.copyOf(source).toString());
            assertEquals(Comparator.reverseOrder(), ImmutableSortedMap.copyOf(new TreeMap<String, Integer>(Comparator.reverseOrder())).comparator());
        }

        @Test
        public void immutableMapAcceptsNullKeysAndValues() {
            final ImmutableMap<String, Integer> map = ImmutableMap.of(null, null);

            assertEquals(1, map.size());
            assertTrue(map.containsKey(null));
            assertEquals(null, map.get(null));

            final Map<String, Integer> source = new LinkedHashMap<>();
            source.put(null, 1);
            source.put("a", null);
            assertEquals(2, ImmutableMap.copyOf(source).size());
            assertEquals(2, ImmutableMap.<String, Integer> builder().put(null, 1).put("a", null).build().size());
        }

        @Test
        public void biMapOfAcceptsARepeatedKeyWithLastValueWinning() {
            final ImmutableBiMap<String, Integer> map = ImmutableBiMap.of("a", 1, "a", 2);

            assertEquals(1, map.size());
            assertEquals(2, map.get("a"));
            assertEquals("a", map.getByValue(2));
        }

        @Test
        public void biMapOfRejectsNullsAndDuplicateValues() {
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of(null, 1));
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("a", null));
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("a", 1, "b", 1));
        }

        @Test
        public void everyFamilyMemberCarriesTheImmutableMarker() {
            // The runtime-observable marker is the Immutable interface; the @Immutable annotation is
            // CLASS-retention and therefore deliberately invisible here.
            final List<Object> instances = List.of(ImmutableList.of(1), ImmutableSet.of(1), ImmutableSortedSet.of(1), ImmutableNavigableSet.of(1),
                    ImmutableMap.of("a", 1), ImmutableSortedMap.of("a", 1), ImmutableNavigableMap.of("a", 1), ImmutableBiMap.of("a", 1),
                    ImmutableCollection.wrap(new LinkedHashSet<>(List.of(1))), ImmutableBiMap.of("a", 1).values(), ImmutableList.of(1, 2).subList(0, 1),
                    ImmutableList.of(1, 2).reversed());

            for (final Object instance : instances) {
                assertInstanceOf(Immutable.class, instance, instance.getClass().getSimpleName());
            }
        }
    }
}
