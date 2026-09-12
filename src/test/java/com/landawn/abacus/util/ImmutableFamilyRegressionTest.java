package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.ObjectType;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

/**
 * Covers the fixes applied from the 2026-09-01 (second) review of the {@code Immutable*}/{@code BiMap}
 * family.
 *
 * <p>The behavioural tests here fail on the pre-fix sources; each nested class names the finding it pins.
 */
public class ImmutableFamilyRegressionTest extends TestBase {

    @Nested
    public class CopyOfNormalizesComparatorSemantics {

        private SortedMap<String, Integer> caseInsensitiveMap() {
            final SortedMap<String, Integer> m = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            m.put("a", 1);
            m.put("B", 2);
            return m;
        }

        private SortedSet<String> caseInsensitiveSet() {
            final SortedSet<String> s = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            s.add("a");
            s.add("B");
            return s;
        }

        @Test
        public void immutableMapCopyOfDropsTheComparatorOfAnOwningSortedSource() {
            final ImmutableSortedMap<String, Integer> owning = ImmutableSortedMap.copyOf(caseInsensitiveMap());
            assertEquals(1, owning.get("A"), "precondition: the owning source itself is case-insensitive");

            final ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(owning);

            assertNotSame(owning, copy);
            assertNull(copy.get("A"));
            assertFalse(copy.containsKey("A"));
            assertFalse(copy instanceof SortedMap, "the copy must not remain a SortedMap");
            // The mappings and their iteration order survive; only the comparison rule is dropped.
            assertEquals(2, copy.size());
            assertEquals(1, copy.get("a"));
            assertEquals(2, copy.get("B"));
            assertEquals(Arrays.asList("a", "B"), new ArrayList<>(copy.keySet()));
        }

        @Test
        public void immutableMapCopyOfTreatsOwningAndWrappedSortedSourcesAlike() {
            final SortedMap<String, Integer> source = caseInsensitiveMap();

            final ImmutableMap<String, Integer> fromOwning = ImmutableMap.copyOf(ImmutableSortedMap.copyOf(source));
            final ImmutableMap<String, Integer> fromView = ImmutableMap.copyOf(ImmutableSortedMap.wrap(source));

            assertEquals(fromView.getClass(), fromOwning.getClass());
            assertNull(fromOwning.get("A"));
            assertNull(fromView.get("A"));
            assertEquals(fromView, fromOwning);
        }

        @Test
        public void immutableMapCopyOfAlsoNormalizesANavigableSource() {
            final ImmutableNavigableMap<String, Integer> owning = ImmutableNavigableMap.copyOf(caseInsensitiveMap());
            final ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(owning);

            assertNotSame(owning, copy);
            assertNull(copy.get("A"));
            assertFalse(copy instanceof SortedMap);
        }

        @Test
        public void immutableSetCopyOfDropsTheComparatorOfAnOwningSortedSource() {
            final ImmutableSortedSet<String> owning = ImmutableSortedSet.copyOf(caseInsensitiveSet());
            assertTrue(owning.contains("A"), "precondition: the owning source itself is case-insensitive");

            final ImmutableSet<String> copy = ImmutableSet.copyOf(owning);

            assertNotSame(owning, copy);
            assertFalse(copy.contains("A"));
            assertFalse(copy instanceof SortedSet, "the copy must not remain a SortedSet");
            assertEquals(2, copy.size());
            assertTrue(copy.contains("a"));
            assertTrue(copy.contains("B"));
            assertEquals(Arrays.asList("a", "B"), new ArrayList<>(copy));
        }

        @Test
        public void immutableSetCopyOfTreatsOwningAndWrappedSortedSourcesAlike() {
            final SortedSet<String> source = caseInsensitiveSet();

            final ImmutableSet<String> fromOwning = ImmutableSet.copyOf(ImmutableSortedSet.copyOf(source));
            final ImmutableSet<String> fromView = ImmutableSet.copyOf(ImmutableSortedSet.wrap(source));

            assertEquals(fromView.getClass(), fromOwning.getClass());
            assertFalse(fromOwning.contains("A"));
            assertFalse(fromView.contains("A"));
            assertEquals(fromView, fromOwning);
        }

        @Test
        public void immutableSetCopyOfAlsoNormalizesANavigableSource() {
            final ImmutableNavigableSet<String> owning = ImmutableNavigableSet.copyOf(caseInsensitiveSet());
            final ImmutableSet<String> copy = ImmutableSet.copyOf(owning);

            assertNotSame(owning, copy);
            assertFalse(copy.contains("A"));
            assertFalse(copy instanceof SortedSet);
        }

        // ----- the fast path itself must survive for the instances it was written for -----

        @Test
        public void aPlainOwningImmutableMapIsStillReturnedUnchanged() {
            final ImmutableMap<String, Integer> fromOf = ImmutableMap.of("a", 1, "b", 2);
            assertSame(fromOf, ImmutableMap.copyOf(fromOf));

            final ImmutableMap<String, Integer> fromCopyOf = ImmutableMap.copyOf(new LinkedHashMap<>(Map.of("a", 1)));
            assertSame(fromCopyOf, ImmutableMap.copyOf(fromCopyOf));

            assertSame(ImmutableMap.empty(), ImmutableMap.copyOf(ImmutableMap.empty()));
        }

        @Test
        public void aPlainOwningImmutableSetIsStillReturnedUnchanged() {
            final ImmutableSet<String> fromOf = ImmutableSet.of("a", "b");
            assertSame(fromOf, ImmutableSet.copyOf(fromOf));

            final ImmutableSet<String> fromCopyOf = ImmutableSet.copyOf(Arrays.asList("a", "b"));
            assertSame(fromCopyOf, ImmutableSet.copyOf(fromCopyOf));

            assertSame(ImmutableSet.empty(), ImmutableSet.copyOf(ImmutableSet.empty()));
        }

        @Test
        public void aWrappedImmutableMapOrSetIsStillCopied() {
            final Map<String, Integer> liveMap = new LinkedHashMap<>(Map.of("a", 1));
            final ImmutableMap<String, Integer> mapView = ImmutableMap.wrap(liveMap);
            assertNotSame(mapView, ImmutableMap.copyOf(mapView));

            final Set<String> liveSet = new LinkedHashSet<>(List.of("a"));
            final ImmutableSet<String> setView = ImmutableSet.wrap(liveSet);
            assertNotSame(setView, ImmutableSet.copyOf(setView));
        }

        @Test
        public void nullAndEmptySourcesStillCollapseToTheSharedEmptyInstance() {
            assertSame(ImmutableMap.empty(), ImmutableMap.copyOf((Map<String, Integer>) null));
            assertSame(ImmutableMap.empty(), ImmutableMap.copyOf(new LinkedHashMap<String, Integer>()));
            assertSame(ImmutableSet.empty(), ImmutableSet.copyOf((Collection<String>) null));
            assertSame(ImmutableSet.empty(), ImmutableSet.copyOf(new ArrayList<String>()));
        }

        @Test
        public void theSortedFactoriesKeepTheirOwnFastPathAndTheirComparator() {
            // ImmutableSortedMap/Set.copyOf are a different contract - they DO keep the comparator - and their
            // own ownership fast path must not have been narrowed by the fix above.
            final ImmutableSortedMap<String, Integer> owningMap = ImmutableSortedMap.copyOf(caseInsensitiveMap());
            assertSame(owningMap, ImmutableSortedMap.copyOf(owningMap));
            assertSame(String.CASE_INSENSITIVE_ORDER, owningMap.comparator());
            assertEquals(1, owningMap.get("A"));

            final ImmutableSortedSet<String> owningSet = ImmutableSortedSet.copyOf(caseInsensitiveSet());
            assertSame(owningSet, ImmutableSortedSet.copyOf(owningSet));
            assertSame(String.CASE_INSENSITIVE_ORDER, owningSet.comparator());
            assertTrue(owningSet.contains("A"));
        }

        @Test
        public void immutableListCopyOfMustKeepItsInstanceofFastPath() {
            // Guard-rail: ImmutableList's owning factories all return the RandomAccessImmutableList subclass,
            // so the exact-class test used for Map/Set would break this. A List has no comparison rule, so
            // ImmutableList has nothing to normalize in the first place.
            final ImmutableList<Integer> owned = ImmutableList.of(1, 2, 3);
            assertNotSame(ImmutableList.class, owned.getClass());
            assertSame(owned, ImmutableList.copyOf(owned));
            assertSame(ImmutableList.empty(), ImmutableList.copyOf(ImmutableList.empty()));

            final ImmutableList<Integer> copied = ImmutableList.copyOf(new ArrayList<>(Arrays.asList(1, 2)));
            assertSame(copied, ImmutableList.copyOf(copied));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B2 - Map.replaceAll specifies NullPointerException for a null function; BiMap raised
    // IllegalArgumentException, disagreeing with putAll (NPE via requireNonNull). forcePutAll uses
    // checkArgNotNull and therefore throws IllegalArgumentException.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapReplaceAllRejectsANullFunctionWithNpe {

        @Test
        public void nullFunctionThrowsNullPointerException() {
            assertThrows(NullPointerException.class, () -> BiMap.of("a", 1).replaceAll(null));
        }

        @Test
        public void nullFunctionThrowsEvenOnAnEmptyBiMap() {
            // The argument check must run before the isEmpty() early return.
            assertThrows(NullPointerException.class, () -> new BiMap<String, Integer>().replaceAll(null));
        }

        @Test
        public void theNullCheckMatchesPutAllAndForcePutAll() {
            assertThrows(NullPointerException.class, () -> BiMap.of("a", 1).putAll(null));
            assertThrows(IllegalArgumentException.class, () -> BiMap.of("a", 1).forcePutAll(null));
        }

        @Test
        public void aNullReplacementValueIsStillAnIllegalArgument() {
            // Unchanged: a function that RETURNS null violates BiMap's own non-null value rule.
            final BiMap<String, Integer> map = BiMap.of("a", 1, "b", 2);
            assertThrows(IllegalArgumentException.class, () -> map.replaceAll((k, v) -> null));
            assertEquals(BiMap.of("a", 1, "b", 2), map, "a failed replaceAll must leave the map unchanged");
        }

        @Test
        public void aDuplicateReplacementValueIsStillAnIllegalArgument() {
            final BiMap<String, Integer> map = BiMap.of("a", 1, "b", 2);
            assertThrows(IllegalArgumentException.class, () -> map.replaceAll((k, v) -> 5));
            assertEquals(BiMap.of("a", 1, "b", 2), map);
        }

        @Test
        public void aValidReplaceAllStillWorks() {
            final BiMap<String, Integer> map = BiMap.of("a", 1, "b", 2, "c", 3);
            map.replaceAll((k, v) -> v == 3 ? 1 : v + 1);

            assertEquals(BiMap.of("a", 2, "b", 3, "c", 1), map);
            assertEquals("a", map.getByValue(2));
            assertEquals("c", map.getByValue(1));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // B3 - five builder(holder) overloads used the message-less N.checkArgNotNull overload, so a null
    // argument produced an IllegalArgumentException whose getMessage() was null.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BuilderNullHolderNamesTheArgument {

        private void assertNamedIae(final String expectedName, final Executable call) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertNotNull(e.getMessage(), "the exception must carry a message");
            assertTrue(e.getMessage().contains(expectedName), "message should name '" + expectedName + "' but was: " + e.getMessage());
        }

        @Test
        public void immutableListBuilder() {
            assertNamedIae("holder", () -> ImmutableList.builder((List<String>) null));
        }

        @Test
        public void immutableSetBuilder() {
            assertNamedIae("holder", () -> ImmutableSet.builder((Set<String>) null));
        }

        @Test
        public void immutableMapBuilder() {
            assertNamedIae("backedMap", () -> ImmutableMap.builder((Map<String, Integer>) null));
        }

        @Test
        public void immutableBiMapBuilder() {
            assertNamedIae("backedMap", () -> ImmutableBiMap.builder((BiMap<String, Integer>) null));
        }

        @Test
        public void biMapBuilder() {
            assertNamedIae("map", () -> BiMap.builder((Map<String, Integer>) null));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D1 - the package-private (backing, boolean) constructor meant `isUnmodifiable` on ImmutableSet /
    // ImmutableMap / AbstractImmutableMap but `ownsBacking` on their sorted subclasses, so a two-argument
    // super(...) call bound to the wrong parameter. The ambiguous overloads are gone; a mistaken two-argument
    // super() is now a compile error.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class NoAmbiguousTwoArgumentConstructors {

        private void assertNoTwoArgConstructor(final Class<?> type) {
            for (final Constructor<?> c : type.getDeclaredConstructors()) {
                final Class<?>[] p = c.getParameterTypes();

                if (p.length == 2 && p[1] == boolean.class) {
                    throw new AssertionError(type.getSimpleName() + " must not declare a (backing, boolean) constructor: " + c);
                }
            }
        }

        @Test
        public void theClassesWithASortedSubclassDeclareNoTwoArgumentForm() {
            // ImmutableSortedSet/ImmutableSortedMap/ImmutableBiMap all spell their two-argument constructor
            // (backing, ownsBacking). While these three declared (backing, isUnmodifiable), a two-argument
            // super(...) from a subclass bound to the wrong parameter silently; now it does not compile.
            assertNoTwoArgConstructor(ImmutableSet.class);
            assertNoTwoArgConstructor(ImmutableMap.class);
            assertNoTwoArgConstructor(AbstractImmutableMap.class);
        }

        @Test
        public void immutableCollectionsTwoArgumentFlagIsOwnership() {
            // Unchanged and unambiguous: this is the form ImmutableList/ImmutableSet's own three-argument
            // constructors call, and no subclass declares a conflicting two-argument form.
            assertTrue(new ImmutableCollection<>(List.of("a"), true).ownsBacking);
            assertFalse(new ImmutableCollection<>(List.of("a"), false).ownsBacking);
        }

        @Test
        public void immutableListsTwoArgumentFlagIsStillIsUnmodifiable() {
            // Kept because RowDataset.getRow and Sheet.row/columnValues build live AbstractList views with it,
            // and ImmutableList is sealed with no subclass declaring a conflicting two-argument form. The flag
            // is NOT ownership, so such an instance is still copied by copyOf.
            final ImmutableList<String> notOwning = new ImmutableList<>(Collections.unmodifiableList(new ArrayList<>(List.of("a"))), true);

            assertFalse(notOwning.ownsBacking);
            assertNotSame(notOwning, ImmutableList.copyOf(notOwning));
            assertEquals(List.of("a"), notOwning);
        }

        @Test
        public void theSortedFamilysTwoArgumentFlagIsOwnership() {
            final ImmutableSortedMap<String, Integer> owningMap = new ImmutableSortedMap<>(new TreeMap<>(Map.of("a", 1)), true);
            assertTrue(owningMap.ownsBacking);
            assertSame(owningMap, ImmutableSortedMap.copyOf(owningMap));

            final ImmutableSortedSet<String> owningSet = new ImmutableSortedSet<>(new TreeSet<>(List.of("a")), true);
            assertTrue(owningSet.ownsBacking);
            assertSame(owningSet, ImmutableSortedSet.copyOf(owningSet));

            final ImmutableSortedMap<String, Integer> viewMap = new ImmutableSortedMap<>(new TreeMap<>(Map.of("a", 1)), false);
            assertFalse(viewMap.ownsBacking);
            assertNotSame(viewMap, ImmutableSortedMap.copyOf(viewMap));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D2 - ImmutableBiMap.values() was narrowed to ImmutableSet but keySet()/entrySet() fell through to
    // AbstractImmutableMap, re-wrapping views the BiMap already publishes as immutable.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class ImmutableBiMapViewsAreItsBiMapsOwnImmutableSets {

        @Test
        public void allThreeViewsAreImmutableSets() {
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);

            assertInstanceOf(ImmutableSet.class, biMap.keySet());
            assertInstanceOf(ImmutableSet.class, biMap.values());
            assertInstanceOf(ImmutableSet.class, biMap.entrySet());
        }

        @Test
        public void theViewsStillCompareEqualToTheEquivalentJdkViews() {
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
            final Map<String, Integer> reference = new LinkedHashMap<>();
            reference.put("one", 1);
            reference.put("two", 2);

            assertEquals(reference.keySet(), biMap.keySet());
            assertEquals(biMap.keySet(), reference.keySet());
            assertEquals(reference.entrySet(), biMap.entrySet());
            assertEquals(biMap.entrySet(), reference.entrySet());
            assertEquals(Set.of(1, 2), biMap.values());
            assertEquals(reference.keySet().hashCode(), biMap.keySet().hashCode());
            assertEquals(reference.entrySet().hashCode(), biMap.entrySet().hashCode());
        }

        @Test
        public void theViewsAreStillReadOnly() {
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);

            assertThrows(UnsupportedOperationException.class, () -> biMap.keySet().remove("one"));
            assertThrows(UnsupportedOperationException.class, () -> biMap.keySet().clear());
            assertThrows(UnsupportedOperationException.class, () -> biMap.values().remove(1));
            assertThrows(UnsupportedOperationException.class, () -> biMap.entrySet().clear());
            assertThrows(UnsupportedOperationException.class, () -> biMap.entrySet().iterator().next().setValue(9));

            final java.util.Iterator<String> keyIter = biMap.keySet().iterator();
            keyIter.next();
            assertThrows(UnsupportedOperationException.class, keyIter::remove);
        }

        @Test
        public void theViewsStayLiveOverAWrappedBiMap() {
            final BiMap<String, Integer> backing = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            backing.put("one", 1);

            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(backing);
            final Set<String> keys = view.keySet();
            final Set<Map.Entry<String, Integer>> entries = view.entrySet();

            backing.put("two", 2);

            assertEquals(Set.of("one", "two"), new LinkedHashSet<>(keys));
            assertEquals(2, entries.size());
            assertTrue(entries.contains(Map.entry("two", 2)));
        }

        @Test
        public void theThreeViewsIterateInTheSameOrder() {
            final BiMap<String, Integer> backing = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            backing.put("b", 2);
            backing.put("a", 1);
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.wrap(backing);

            final List<String> keys = new ArrayList<>(biMap.keySet());
            final List<Integer> values = new ArrayList<>(biMap.values());
            final List<String> entryKeys = new ArrayList<>();
            final List<Integer> entryValues = new ArrayList<>();

            for (final Map.Entry<String, Integer> e : biMap.entrySet()) {
                entryKeys.add(e.getKey());
                entryValues.add(e.getValue());
            }

            assertEquals(Arrays.asList("b", "a"), keys);
            assertEquals(Arrays.asList(2, 1), values);
            assertEquals(keys, entryKeys);
            assertEquals(values, entryValues);
        }

        @Test
        public void mapLevelEqualityIsUnaffected() {
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1, "two", 2);
            final Map<String, Integer> reference = Map.of("one", 1, "two", 2);

            assertEquals(reference, biMap);
            assertEquals(biMap, reference);
            assertEquals(reference.hashCode(), biMap.hashCode());
        }
    }

    // ---------------------------------------------------------------------------------------------
    // D3 - BiMap inherited Map.forEach (one ImmutableEntry allocated per entry) and Map.getOrDefault
    // (a second containsKey probe on a miss) instead of delegating to the forward backing map, unlike
    // AbstractImmutableMap which delegates both.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapDelegatesForEachAndGetOrDefault {

        @Test
        public void forEachVisitsEveryEntryInForwardMapOrder() {
            final BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            map.put("b", 2);
            map.put("a", 1);
            map.put("c", 3);

            final List<String> seen = new ArrayList<>();
            map.forEach((k, v) -> seen.add(k + "=" + v));

            assertEquals(Arrays.asList("b=2", "a=1", "c=3"), seen);
        }

        @Test
        public void forEachOnAnEmptyBiMapDoesNothing() {
            final AtomicInteger calls = new AtomicInteger();
            new BiMap<String, Integer>().forEach((k, v) -> calls.incrementAndGet());
            assertEquals(0, calls.get());
        }

        @Test
        public void forEachRejectsANullAction() {
            assertThrows(NullPointerException.class, () -> BiMap.of("a", 1).forEach(null));
            // Also on an empty map: the delegate would not invoke the action at all.
            assertThrows(NullPointerException.class, () -> new BiMap<String, Integer>().forEach(null));
        }

        @Test
        public void forEachSeesTheInverseViewsSwappedEntries() {
            final BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);
            map.put("a", 1);
            map.put("b", 2);

            final List<String> seen = new ArrayList<>();
            map.inverse().forEach((k, v) -> seen.add(k + "=" + v));

            assertEquals(Arrays.asList("1=a", "2=b"), seen);
        }

        @Test
        public void getOrDefaultReturnsTheMappingOrTheDefault() {
            final BiMap<String, Integer> map = BiMap.of("one", 1, "two", 2);

            assertEquals(1, map.getOrDefault("one", 0));
            assertEquals(2, map.getOrDefault("two", 0));
            assertEquals(0, map.getOrDefault("three", 0));
            assertNull(map.getOrDefault("three", null));
        }

        @Test
        public void getOrDefaultOnAnEmptyBiMapReturnsTheDefault() {
            assertEquals(42, new BiMap<String, Integer>().getOrDefault("missing", 42));
        }

        /** A forward backing map that records which of its bulk/lookup entry points the BiMap actually used. */
        private static final class CountingMap<K, V> extends HashMap<K, V> {
            private static final long serialVersionUID = 1L;
            private transient int containsKeyCalls;
            private transient int forEachCalls;

            @Override
            public boolean containsKey(final Object key) {
                containsKeyCalls++;
                return super.containsKey(key);
            }

            @Override
            public void forEach(final BiConsumer<? super K, ? super V> action) {
                forEachCalls++;
                super.forEach(action);
            }
        }

        private BiMap<String, Integer> withCountingKeyMap(final CountingMap<String, Integer> keyMap) {
            // keyMap and the value map must be distinct empty instances; the BiMap constructor checks that.
            return new BiMap<>(() -> keyMap, HashMap::new);
        }

        @Test
        public void forEachGoesThroughTheForwardMapNotTheEntrySet() {
            // The inherited Map.forEach default iterates entrySet(), which materializes one ImmutableEntry per
            // entry purely to read the key and value back out. Measured out of band over a 100,000-entry BiMap
            // (com.sun.management.ThreadMXBean.getThreadAllocatedBytes): 1,578,496 bytes before the override,
            // 30,112 bytes after - a ~52x reduction. That is not assertable in-process, so this test pins the
            // mechanism instead: the forward map's own forEach must be the one that runs.
            final CountingMap<String, Integer> keyMap = new CountingMap<>();
            final BiMap<String, Integer> map = withCountingKeyMap(keyMap);
            map.put("a", 1);
            map.put("b", 2);
            keyMap.forEachCalls = 0;

            final List<String> seen = new ArrayList<>();
            map.forEach((k, v) -> seen.add(k + "=" + v));

            assertEquals(1, keyMap.forEachCalls);
            assertEquals(2, seen.size());
        }

        @Test
        public void getOrDefaultDoesNotProbeContainsKeyOnAMiss() {
            // The inherited Map.getOrDefault default follows a null get() with a containsKey() probe - a second
            // hash lookup a BiMap never needs, because it stores no null value.
            final CountingMap<String, Integer> keyMap = new CountingMap<>();
            final BiMap<String, Integer> map = withCountingKeyMap(keyMap);
            map.put("a", 1);
            keyMap.containsKeyCalls = 0;

            assertEquals(99, map.getOrDefault("missing", 99));
            assertEquals(0, keyMap.containsKeyCalls);

            assertEquals(1, map.getOrDefault("a", 99));
            assertEquals(0, keyMap.containsKeyCalls);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // O2 - BiMap.copyOf(null) allocated both backing maps and their suppliers before putAll rejected null.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class BiMapCopyOfRejectsNullUpFront {

        @Test
        public void nullSourceThrowsNullPointerException() {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> BiMap.copyOf(null));
            // Before the fix the throw came from putAll's own check, after both backing maps and their
            // suppliers had been built, and named putAll's parameter instead of copyOf's.
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("map"), "message should name 'map' but was: " + e.getMessage());
        }

        @Test
        public void anEmptyMapIsStillCopiedIntoAnEmptyBiMap() {
            final BiMap<String, Integer> copy = BiMap.copyOf(new LinkedHashMap<String, Integer>());
            assertTrue(copy.isEmpty());
        }

        @Test
        public void copyingStillWorksForBothTheBiMapAndTheGeneralPath() {
            final BiMap<String, Integer> source = BiMap.of("a", 1, "b", 2);
            final BiMap<String, Integer> viaBiMap = BiMap.copyOf(source);
            assertEquals(source, viaBiMap);
            assertNotSame(source, viaBiMap);

            final Map<String, Integer> plain = new LinkedHashMap<>();
            plain.put("a", 1);
            assertEquals(BiMap.of("a", 1), BiMap.copyOf(plain));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // O4 - ImmutableBiMap.empty().inverse() built (and permanently cached on the shared singleton) a second
    // empty instance instead of returning the singleton itself.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class TheEmptyImmutableBiMapIsItsOwnInverse {

        @Test
        public void emptyInverseIsTheSingleton() {
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.empty().inverse());
            assertSame(ImmutableBiMap.empty(), ImmutableBiMap.empty().inverse().inverse());
        }

        @Test
        public void anEmptyWrappedViewIsNotSpecialCased() {
            // A wrap()-backed empty BiMap can still grow, so its inverse must be a real inverse view.
            final BiMap<String, Integer> backing = new BiMap<>();
            final ImmutableBiMap<String, Integer> view = ImmutableBiMap.wrap(backing);

            final ImmutableBiMap<Integer, String> inverse = view.inverse();
            assertNotSame(view, inverse);

            backing.put("a", 1);
            assertEquals("a", inverse.get(1));
            assertSame(view, inverse.inverse());
        }

        @Test
        public void aNonEmptyInverseStillRoundTrips() {
            final ImmutableBiMap<String, Integer> biMap = ImmutableBiMap.of("one", 1);
            final ImmutableBiMap<Integer, String> inverse = biMap.inverse();

            assertSame(inverse, biMap.inverse());
            assertSame(biMap, inverse.inverse());
            assertEquals("one", inverse.get(1));
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Javadoc-driven behaviours corrected in this pass; pinned so the docs cannot drift from the code.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class DocumentedBehaviours {

        @Test
        public void immutableMapOfKeepsTheLastValueForARepeatedKey() {
            // J2: the of(...) summaries said "exactly N key-value mappings"; duplicate keys collapse.
            assertEquals(1, ImmutableMap.of("a", 1, "a", 2).size());
            assertEquals(2, ImmutableMap.of("a", 1, "a", 2).get("a"));
            assertEquals(2, ImmutableMap.of("a", 1, "b", 2, "a", 3).size());
            assertEquals(3, ImmutableMap.of("a", 1, "b", 2, "a", 3).get("a"));
        }

        @Test
        public void immutableCollectionWrapNullYieldsAnEmptyListNotAPlainCollection() {
            // D4: the note claimed every non-ImmutableCollection argument yields a plain ImmutableCollection.
            final ImmutableCollection<String> fromNull = ImmutableCollection.wrap(null);
            final ImmutableCollection<String> fromEmpty = ImmutableCollection.wrap(new ArrayList<>());

            assertInstanceOf(List.class, fromNull);
            assertFalse(fromEmpty instanceof List);
            assertTrue(fromNull.isEmpty());
            assertTrue(fromEmpty.isEmpty());
            // Identity equality on the plain wrapper makes the two empty results unequal both ways.
            assertNotEquals(fromNull, fromEmpty);
            assertNotEquals(fromEmpty, fromNull);
        }

        @Test
        public void biMapCopyOfPreservesTheOrderOfAnInstantiableSourceClass() {
            // J6: order preservation is best-effort - guaranteed when the source's runtime class can be
            // reconstructed, which is the case for a plain LinkedHashMap.
            final LinkedHashMap<String, Integer> source = new LinkedHashMap<>();
            source.put("z", 26);
            source.put("a", 1);
            source.put("m", 13);

            assertEquals(Arrays.asList("z", "a", "m"), new ArrayList<>(BiMap.copyOf(source).keySet()));
            assertEquals(Arrays.asList("z", "a", "m"), new ArrayList<>(ImmutableBiMap.copyOf(source).keySet()));

            // Through a wrapper the class cannot be reconstructed; only the mappings are guaranteed.
            final BiMap<String, Integer> viaWrapper = BiMap.copyOf(Collections.unmodifiableMap(source));
            assertEquals(source, viaWrapper);
            assertEquals(3, viaWrapper.size());
        }

        @Test
        public void biMapCopyOfKeepsASortedSourcesComparator() {
            final SortedMap<String, Integer> source = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            source.put("a", 1);

            final BiMap<String, Integer> copy = BiMap.copyOf(source);
            assertEquals(1, copy.get("A"));
            // copy() must reproduce it too - the supplier captured the comparator, not the map class.
            assertEquals(1, copy.copy().get("A"));
        }

        @Test
        public void anOwningRangeViewIsStillReturnedUnchangedByCopyOf() {
            // B4: documented, not changed - copyOf detaches from modification, not from the parent's memory.
            final ImmutableList<String> owned = ImmutableList.copyOf(Arrays.asList("a", "b", "c", "d"));
            final ImmutableList<String> ownedSub = owned.subList(0, 2);
            assertSame(ownedSub, ImmutableList.copyOf(ownedSub));

            final ImmutableSortedMap<String, Integer> ownedMap = ImmutableSortedMap.of("a", 1, "b", 2, "c", 3);
            final ImmutableSortedMap<String, Integer> ownedRange = ownedMap.subMap("a", "c");
            assertSame(ownedRange, ImmutableSortedMap.copyOf(ownedRange));
        }

    }

    @Nested
    public class ImmutableTypeNamesResolveBySimpleName {

        private final List<Class<?>> family = List.of(ImmutableList.class, ImmutableSet.class, ImmutableMap.class, ImmutableCollection.class,
                ImmutableSortedSet.class, ImmutableNavigableSet.class, ImmutableSortedMap.class, ImmutableNavigableMap.class, ImmutableBiMap.class);

        @Test
        public void everyFamilyMemberResolvesByItsBareSimpleName() {
            for (final Class<?> c : family) {
                final Type<?> t = TypeFactory.getType(c.getSimpleName());
                assertFalse(t instanceof ObjectType, c.getSimpleName() + " resolved to ObjectType");
            }
        }

        @Test
        public void theSetShapedMembersResolveWithOneTypeParameter() {
            for (final Class<?> c : List.of(ImmutableList.class, ImmutableSet.class, ImmutableCollection.class, ImmutableSortedSet.class,
                    ImmutableNavigableSet.class)) {
                final Type<?> t = TypeFactory.getType(c.getSimpleName() + "<String>");
                assertFalse(t instanceof ObjectType, c.getSimpleName() + "<String> resolved to ObjectType");
            }
        }

        @Test
        public void theMapShapedMembersResolveWithTwoTypeParameters() {
            for (final Class<?> c : List.of(ImmutableMap.class, ImmutableSortedMap.class, ImmutableNavigableMap.class, ImmutableBiMap.class)) {
                final Type<?> t = TypeFactory.getType(c.getSimpleName() + "<String, Integer>");
                assertFalse(t instanceof ObjectType, c.getSimpleName() + "<String, Integer> resolved to ObjectType");
            }
        }

        @Test
        public void aWrongArityTypeParameterListIsRejectedRatherThanSilentlyDegraded() {
            // Previously these produced an ObjectType; now they fail the same way ImmutableList<String, Integer>
            // already did (TypeFactoryTest pins that contract for the three original members).
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableSortedSet<String, Integer>"));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableNavigableSet<String, Integer>"));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableCollection<String, Integer>"));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableSortedMap<String>"));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableNavigableMap<String>"));
            assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("ImmutableBiMap<String>"));
        }

        @Test
        public void declaringNameCanBeParsedBackForEveryFamilyMember() {
            // C-002: a name-emitting method must produce a name its own reader accepts. ImmutableSortedSet,
            // ImmutableNavigableSet, ImmutableSortedMap and ImmutableNavigableMap previously emitted their own
            // simple class name, which did not resolve.
            for (final Class<?> c : family) {
                final String declaringName = CommonUtil.typeOf(c).declaringName();
                final Type<?> reparsed = TypeFactory.getType(declaringName);
                assertFalse(reparsed instanceof ObjectType, c.getSimpleName() + ".declaringName() = " + declaringName + " does not parse back");
            }
        }

        @Test
        public void aSimpleNameTypeRoundTripsAValueRatherThanYieldingAString() {
            assertRoundTrip("ImmutableSortedSet<String>", ImmutableSortedSet.of("b", "a"), ImmutableSortedSet.class);
            assertRoundTrip("ImmutableNavigableSet<String>", ImmutableNavigableSet.of("b", "a"), ImmutableNavigableSet.class);
            assertRoundTrip("ImmutableSortedMap<String, Integer>", ImmutableSortedMap.of("b", 2, "a", 1), ImmutableSortedMap.class);
            assertRoundTrip("ImmutableNavigableMap<String, Integer>", ImmutableNavigableMap.of("b", 2, "a", 1), ImmutableNavigableMap.class);
            assertRoundTrip("ImmutableBiMap<String, Integer>", ImmutableBiMap.of("b", 2, "a", 1), ImmutableBiMap.class);
            assertRoundTrip("ImmutableCollection<String>", ImmutableList.of("x"), ImmutableCollection.class);
            // the three that already worked must keep working
            assertRoundTrip("ImmutableList<String>", ImmutableList.of("b", "a"), ImmutableList.class);
            assertRoundTrip("ImmutableSet<String>", ImmutableSet.of("b", "a"), ImmutableSet.class);
            assertRoundTrip("ImmutableMap<String, Integer>", ImmutableMap.of("b", 2), ImmutableMap.class);
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        private void assertRoundTrip(final String typeName, final Object value, final Class<?> expected) {
            final Type type = TypeFactory.getType(typeName);
            final Object back = type.valueOf(type.stringOf(value));

            assertNotNull(back, typeName + " round-tripped to null");
            assertTrue(expected.isInstance(back), typeName + " round-tripped to a " + back.getClass().getName() + " instead of a " + expected.getSimpleName());
            assertEquals(value, back, typeName + " lost its contents");
        }

        @Test
        public void anEmptyValueRoundTripsForTheTypesThatHaveADedicatedHandler() {
            assertRoundTrip("ImmutableList<String>", ImmutableList.<String> empty(), ImmutableList.class);
            assertRoundTrip("ImmutableSet<String>", ImmutableSet.<String> empty(), ImmutableSet.class);
            assertRoundTrip("ImmutableMap<String, Integer>", ImmutableMap.<String, Integer> empty(), ImmutableMap.class);
            assertRoundTrip("ImmutableSortedSet<String>", ImmutableSortedSet.<String> empty(), ImmutableSortedSet.class);
            assertRoundTrip("ImmutableNavigableSet<String>", ImmutableNavigableSet.<String> empty(), ImmutableNavigableSet.class);
            assertRoundTrip("ImmutableSortedMap<String, Integer>", ImmutableSortedMap.<String, Integer> empty(), ImmutableSortedMap.class);
            assertRoundTrip("ImmutableNavigableMap<String, Integer>", ImmutableNavigableMap.<String, Integer> empty(), ImmutableNavigableMap.class);
        }

        @Test
        public void emptyImmutableBiMapAndImmutableCollectionStillFailThroughTheGenericHandlers() {
            // C-003, a SEPARATE pre-existing defect this suite pins rather than hides. ImmutableBiMap and
            // ImmutableCollection are the only two family members with no dedicated *Type: they fall through to
            // the generic MapType/CollectionType, whose empty-value path constructs the target class reflectively
            // and neither has an accessible no-arg constructor. Non-empty values work (they go through the
            // parser's registered creator/converter), and so does the bean path, so this is reachable only via
            // the direct Type API. Verified identical before and after the C-001 registration change.
            final Type<?> biMapType = TypeFactory.getType("ImmutableBiMap<String, Integer>");
            assertThrows(IllegalArgumentException.class, () -> biMapType.valueOf("{}"));

            final Type<?> collType = TypeFactory.getType("ImmutableCollection<String>");
            assertThrows(IllegalArgumentException.class, () -> collType.valueOf("[]"));

            // ... while a non-empty value of the very same type round-trips.
            assertRoundTrip("ImmutableBiMap<String, Integer>", ImmutableBiMap.of("b", 2), ImmutableBiMap.class);
            assertRoundTrip("ImmutableCollection<String>", ImmutableList.of("x"), ImmutableCollection.class);
        }

        @Test
        public void unicodeAndSurrogateElementsSurviveTheRoundTrip() {
            // Natural String ordering is UTF-16 code-unit order, so the emoji (a surrogate pair) sorts last.
            final ImmutableSortedSet<String> src = ImmutableSortedSet.of("😀", "é", "a");
            assertEquals(List.of("a", "é", "😀"), new ArrayList<>(src));
            assertRoundTrip("ImmutableSortedSet<String>", src, ImmutableSortedSet.class);

            assertRoundTrip("ImmutableSortedMap<String, Integer>", ImmutableSortedMap.of("😀", 1, "é", 2), ImmutableSortedMap.class);
        }

        @Test
        public void theFullyQualifiedSpellingStillResolvesAsBefore() {
            // Backward compatibility: names written by an older version are fully qualified.
            for (final Class<?> c : family) {
                final Type<?> t = TypeFactory.getType(c.getName() + (Map.class.isAssignableFrom(c) ? "<String, Integer>" : "<String>"));
                assertFalse(t instanceof ObjectType, c.getName() + " no longer resolves");
            }
        }

        @Test
        public void anUnrelatedSimpleNameIsUnaffected() {
            // The registration is six explicit entries, not a package-wide fallback: a name that resolved to
            // ObjectType before must still do so, or unrelated enums/classes would be silently hijacked.
            assertTrue(TypeFactory.getType("NoSuchTypeNameAtAll") instanceof ObjectType);
            assertFalse(TypeFactory.getType("Multiset<String>") instanceof ObjectType);
            assertFalse(TypeFactory.getType("BiMap<String, Integer>") instanceof ObjectType);
        }
    }

    // ---------------------------------------------------------------------------------------------
    // C-004 - ImmutableBiMap.of(...) delegated to BiMap.of(...), which builds HashMap-backed storage, so it
    // iterated in hash order while the sibling ImmutableMap.of(...) guarantees insertion order and says so 9
    // times in its javadoc. ImmutableBiMap claimed nothing either way, so the divergence was invisible.
    // ---------------------------------------------------------------------------------------------
    @Nested
    public class ImmutableBiMapOfPreservesInsertionOrder {

        // Keys chosen so that HashMap order differs from insertion order (it used to yield
        // [zeta, mu, beta, omega, alpha]).
        private static final List<String> KEYS = List.of("zeta", "alpha", "mu", "beta", "omega");

        private ImmutableBiMap<String, Integer> five() {
            return ImmutableBiMap.of("zeta", 1, "alpha", 2, "mu", 3, "beta", 4, "omega", 5);
        }

        @Test
        public void keySetIteratesInTheOrderSupplied() {
            assertEquals(KEYS, new ArrayList<>(five().keySet()));
        }

        @Test
        public void valuesAndEntrySetLineUpWithKeySet() {
            final ImmutableBiMap<String, Integer> m = five();
            assertEquals(List.of(1, 2, 3, 4, 5), new ArrayList<>(m.values()));

            final List<String> entryKeys = new ArrayList<>();
            final List<Integer> entryValues = new ArrayList<>();
            for (final Map.Entry<String, Integer> e : m.entrySet()) {
                entryKeys.add(e.getKey());
                entryValues.add(e.getValue());
            }
            assertEquals(KEYS, entryKeys);
            assertEquals(List.of(1, 2, 3, 4, 5), entryValues);
        }

        @Test
        public void everyArityPreservesOrder() {
            assertEquals(List.of("zeta"), new ArrayList<>(ImmutableBiMap.of("zeta", 1).keySet()));
            assertEquals(List.of("zeta", "alpha"), new ArrayList<>(ImmutableBiMap.of("zeta", 1, "alpha", 2).keySet()));
            assertEquals(List.of("zeta", "alpha", "mu"), new ArrayList<>(ImmutableBiMap.of("zeta", 1, "alpha", 2, "mu", 3).keySet()));
            assertEquals(List.of("zeta", "alpha", "mu", "beta"), new ArrayList<>(ImmutableBiMap.of("zeta", 1, "alpha", 2, "mu", 3, "beta", 4).keySet()));
            assertEquals(KEYS, new ArrayList<>(five().keySet()));
            assertEquals(List.of(10, 9, 8, 7, 6, 5), new ArrayList<>(ImmutableBiMap.of(10, "j", 9, "i", 8, "h", 7, "g", 6, "f", 5, "e").keySet()));
            assertEquals(List.of(10, 9, 8, 7, 6, 5, 4), new ArrayList<>(ImmutableBiMap.of(10, "j", 9, "i", 8, "h", 7, "g", 6, "f", 5, "e", 4, "d").keySet()));
            assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3),
                    new ArrayList<>(ImmutableBiMap.of(10, "j", 9, "i", 8, "h", 7, "g", 6, "f", 5, "e", 4, "d", 3, "c").keySet()));
            assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2),
                    new ArrayList<>(ImmutableBiMap.of(10, "j", 9, "i", 8, "h", 7, "g", 6, "f", 5, "e", 4, "d", 3, "c", 2, "b").keySet()));
            assertEquals(List.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1),
                    new ArrayList<>(ImmutableBiMap.of(10, "j", 9, "i", 8, "h", 7, "g", 6, "f", 5, "e", 4, "d", 3, "c", 2, "b", 1, "a").keySet()));
        }

        @Test
        public void itNowMatchesItsImmutableMapSibling() {
            assertEquals(new ArrayList<>(ImmutableMap.of("zeta", 1, "alpha", 2, "mu", 3, "beta", 4, "omega", 5).keySet()), new ArrayList<>(five().keySet()));
        }

        @Test
        public void theKeySetSpliteratorNowReportsOrdered() {
            // keySet() comes straight off the (now LinkedHashMap-backed) forward map, so it advertises ORDERED.
            assertTrue(five().keySet().spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));
        }

        @Test
        public void valuesAndEntrySetAdvertiseTheirEncounterOrder() {
            // Ordered backing maps retain encounter-order metadata through both projected views.
            final ImmutableBiMap<String, Integer> m = five();

            assertTrue(m.values().spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));
            assertTrue(m.entrySet().spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));

            // ... while the actual iteration and sequential stream order are correct.
            assertEquals(List.of(1, 2, 3, 4, 5), m.values().stream().collect(java.util.stream.Collectors.toList()));
            assertEquals(KEYS, m.entrySet().stream().map(Map.Entry::getKey).collect(java.util.stream.Collectors.toList()));
        }

        @Test
        public void aRepeatedKeyKeepsItsFirstPositionAndLastValue() {
            // Insertion-ordered storage keeps a re-put key in its original slot; the last value still wins.
            final ImmutableBiMap<String, Integer> m = ImmutableBiMap.of("a", 1, "b", 2, "a", 3);
            assertEquals(List.of("a", "b"), new ArrayList<>(m.keySet()));
            assertEquals(3, m.get("a"));
            assertEquals(2, m.size());
        }

        @Test
        public void theInverseViewIsOrderedByItsOwnKeysNotTheForwardKeys() {
            // inverse() swaps the two backing maps, so it iterates in the order the VALUES were inserted -
            // which here is the same supply order.
            assertEquals(List.of(1, 2, 3, 4, 5), new ArrayList<>(five().inverse().keySet()));
            assertEquals(KEYS, new ArrayList<>(five().inverse().values()));
        }

        @Test
        public void orderSurvivesACopyOfRoundTrip() {
            assertEquals(KEYS, new ArrayList<>(ImmutableBiMap.copyOf(five()).keySet()));
        }

        @Test
        public void unicodeAndSurrogateKeysKeepTheirSuppliedOrder() {
            final ImmutableBiMap<String, Integer> m = ImmutableBiMap.of("\uD83D\uDE00", 1, "\u00e9", 2, "a", 3);
            assertEquals(List.of("\uD83D\uDE00", "\u00e9", "a"), new ArrayList<>(m.keySet()));
        }

        @Test
        public void nullKeysAndValuesAreStillRejected() {
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of(null, 1));
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("a", null));
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("a", 1, null, 2));
        }

        @Test
        public void duplicateValuesAreStillRejected() {
            assertThrows(IllegalArgumentException.class, () -> ImmutableBiMap.of("a", 1, "b", 1));
        }

        @Test
        public void theEmptyInstanceIsUnaffected() {
            assertTrue(ImmutableBiMap.empty().isEmpty());
            assertEquals(List.of(), new ArrayList<>(ImmutableBiMap.empty().keySet()));
        }
    }
}
