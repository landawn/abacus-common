package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ImmutableMapTest extends TestBase {

    private static final class MutableImmutableNamedMap<K, V> extends HashMap<K, V> {
        private static final long serialVersionUID = 1L;
    }

    @Test
    public void testEmpty() {
        ImmutableMap<String, Integer> emptyMap = ImmutableMap.empty();
        Assertions.assertTrue(emptyMap.isEmpty());
        Assertions.assertEquals(0, emptyMap.size());
        Assertions.assertNull(emptyMap.get("any"));
    }

    @Test
    public void testIsEmpty() {
        Assertions.assertTrue(ImmutableMap.empty().isEmpty());
        Assertions.assertFalse(ImmutableMap.of("a", 1).isEmpty());
    }

    @Test
    public void testSize() {
        Assertions.assertEquals(0, ImmutableMap.empty().size());
        Assertions.assertEquals(1, ImmutableMap.of("a", 1).size());
        Assertions.assertEquals(3, ImmutableMap.of("a", 1, "b", 2, "c", 3).size());
    }

    @Test
    public void testToString() {
        ImmutableMap<String, Integer> empty = ImmutableMap.empty();
        Assertions.assertEquals("{}", empty.toString());

        ImmutableMap<String, Integer> single = ImmutableMap.of("a", 1);
        Assertions.assertEquals("{a=1}", single.toString());
    }

    @Test
    public void testOf_TwoEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1, "two", 2);
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals(2, map.get("two"));
    }

    @Test
    public void testOf_ThreeEntries() {
        ImmutableMap<String, String> map = ImmutableMap.of("a", "A", "b", "B", "c", "C");
        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("C", map.get("c"));
    }

    @Test
    public void testOf_FourEntries() {
        ImmutableMap<Integer, String> map = ImmutableMap.of(1, "one", 2, "two", 3, "three", 4, "four");
        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals("four", map.get(4));
    }

    @Test
    public void testOf_FiveEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5);
        Assertions.assertEquals(5, map.size());
        Assertions.assertEquals(5, map.get("e"));
    }

    @Test
    public void testOf_SixEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6);
        Assertions.assertEquals(6, map.size());
        Assertions.assertEquals(6, map.get("f"));
    }

    @Test
    public void testOf_SevenEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7);
        Assertions.assertEquals(7, map.size());
        Assertions.assertEquals(7, map.get("g"));
    }

    @Test
    public void testOf_EightEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8);
        Assertions.assertEquals(8, map.size());
        Assertions.assertEquals(8, map.get("h"));
    }

    @Test
    public void testOf_NineEntries() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        Assertions.assertEquals(9, map.size());
        Assertions.assertEquals(9, map.get("i"));
    }

    @Test
    public void testEquals() {
        ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
        ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
        ImmutableMap<String, Integer> map3 = ImmutableMap.of("a", 1, "c", 3);

        Assertions.assertEquals(map1, map2);
        Assertions.assertNotEquals(map1, map3);
        Assertions.assertEquals(map1, map1);

        // Test equals with regular Map
        Map<String, Integer> regularMap = new HashMap<>();
        regularMap.put("a", 1);
        regularMap.put("b", 2);
        Assertions.assertEquals(map1, regularMap);
    }

    @Test
    public void testHashCode() {
        ImmutableMap<String, Integer> map1 = ImmutableMap.of("a", 1, "b", 2);
        ImmutableMap<String, Integer> map2 = ImmutableMap.of("a", 1, "b", 2);
        Assertions.assertEquals(map1.hashCode(), map2.hashCode());
    }

    @Test
    public void testForEach_Functional() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
        Map<String, Integer> collected = new HashMap<>();
        map.forEach(collected::put);
        Assertions.assertEquals(3, collected.size());
        Assertions.assertEquals(1, collected.get("a"));
        Assertions.assertEquals(2, collected.get("b"));
        Assertions.assertEquals(3, collected.get("c"));
    }

    @Test
    public void testOf_SingleEntry() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("one", 1);
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertNull(map.get("two"));
    }

    @Test
    public void testGetOrDefault() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertEquals(1, map.getOrDefault("a", 0));
        Assertions.assertEquals(2, map.getOrDefault("b", 0));
        Assertions.assertEquals(99, map.getOrDefault("c", 99));
        Assertions.assertEquals(100, map.getOrDefault(null, 100));
    }

    @Test
    public void testContainsKey() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertTrue(map.containsKey("a"));
        Assertions.assertTrue(map.containsKey("b"));
        Assertions.assertFalse(map.containsKey("c"));
        Assertions.assertFalse(map.containsKey(null));
    }

    @Test
    public void testContainsValue() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);

        Assertions.assertTrue(map.containsValue(1));
        Assertions.assertTrue(map.containsValue(2));
        Assertions.assertFalse(map.containsValue(3));
        Assertions.assertFalse(map.containsValue(null));
    }

    @Test
    public void testGet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);

        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(2, map.get("b"));
        Assertions.assertNull(map.get("c"));
        Assertions.assertNull(map.get(null));
    }

    @Test
    public void testPut_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("b", 2));
    }

    @Test
    public void testRemove_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("a"));
    }

    @Test
    public void testPutAll_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Map<String, Integer> other = new HashMap<>();
        other.put("b", 2);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.putAll(other));
    }

    @Test
    public void testPutIfAbsent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.putIfAbsent("b", 2));
    }

    @Test
    public void testRemove_KeyValue_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("a", 1));
    }

    @Test
    public void testReplace_KeyOldNew_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 1, 2));
    }

    @Test
    public void testReplace_KeyValue_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replace("a", 2));
    }

    @Test
    public void testComputeIfAbsent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.computeIfAbsent("b", k -> 2));
    }

    @Test
    public void testComputeIfPresent_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("a", (k, v) -> v + 1));
    }

    @Test
    public void testCompute_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.compute("a", (k, v) -> 2));
    }

    @Test
    public void testMerge_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.merge("a", 2, (v1, v2) -> v1 + v2));
    }

    @Test
    public void testClear_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.clear());
    }

    @Test
    public void testReplaceAll_EmptyMap_ThrowsUnsupported() {
        // Regression: the inherited Map.replaceAll default iterates entrySet(), so on an EMPTY immutable map it
        // would silently no-op instead of throwing — inconsistent with every other mutator (and with the existing
        // non-empty testReplaceAll_ThrowsUnsupported). AbstractImmutableMap.replaceAll now blocks it unconditionally.
        ImmutableMap<String, Integer> map = ImmutableMap.empty();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> v + 1));
    }

    @Test
    public void testKeySet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
        Set<String> keys = map.keySet();

        Assertions.assertEquals(3, keys.size());
        Assertions.assertTrue(keys.contains("a"));
        Assertions.assertTrue(keys.contains("b"));
        Assertions.assertTrue(keys.contains("c"));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.add("d"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.remove("a"));
    }

    @Test
    public void testValues() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 1);
        Collection<Integer> values = map.values();

        Assertions.assertEquals(3, values.size());
        Assertions.assertTrue(values.contains(1));
        Assertions.assertTrue(values.contains(2));

        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.add(3));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
    }

    @Test
    public void testEntrySet() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
        Set<Map.Entry<String, Integer>> entries = map.entrySet();

        Assertions.assertEquals(2, entries.size());

        boolean foundA = false, foundB = false;
        for (Map.Entry<String, Integer> entry : entries) {
            if ("a".equals(entry.getKey()) && 1 == entry.getValue()) {
                foundA = true;
            } else if ("b".equals(entry.getKey()) && 2 == entry.getValue()) {
                foundB = true;
            }
        }
        Assertions.assertTrue(foundA);
        Assertions.assertTrue(foundB);

        Map.Entry<String, Integer> newEntry = new AbstractMap.SimpleEntry<>("c", 3);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entries.add(newEntry));
    }

    @Test
    public void testReplaceAll_ThrowsUnsupported() {
        ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.replaceAll((k, v) -> v + 1));
    }

    @Test
    public void testCopyOf_PreservesOrder() {
        LinkedHashMap<String, Integer> linked = new LinkedHashMap<>();
        linked.put("first", 1);
        linked.put("second", 2);
        linked.put("third", 3);

        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(linked);
        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("first", keys.next());
        Assertions.assertEquals("second", keys.next());
        Assertions.assertEquals("third", keys.next());
    }

    @Test
    public void testCopyOf() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("one", 1);
        mutable.put("two", 2);

        ImmutableMap<String, Integer> immutable = ImmutableMap.copyOf(mutable);
        Assertions.assertEquals(2, immutable.size());

        mutable.put("three", 3);
        Assertions.assertEquals(2, immutable.size());
        Assertions.assertNull(immutable.get("three"));
    }

    @Test
    public void testCopyOf_AlreadyImmutable() {
        ImmutableMap<String, Integer> original = ImmutableMap.of("a", 1);
        ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(original);
        Assertions.assertSame(original, copy);
    }

    @Test
    public void testCopyOf_Empty() {
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(new HashMap<>());
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testCopyOf_Null() {
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(null);
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    public void testGetOrDefault_WithNullValue() {
        Map<String, Integer> mapWithNull = new HashMap<>();
        mapWithNull.put("key", null);
        ImmutableMap<String, Integer> map = ImmutableMap.copyOf(mapWithNull);

        Assertions.assertNull(map.getOrDefault("key", 42));
        Assertions.assertEquals(42, map.getOrDefault("missing", 42));
    }

    @Test
    public void testWithNullKeyValue() {
        Map<String, String> mapWithNulls = new HashMap<>();
        mapWithNulls.put(null, "nullKey");
        mapWithNulls.put("nullValue", null);
        mapWithNulls.put("normal", "value");

        ImmutableMap<String, String> map = ImmutableMap.copyOf(mapWithNulls);

        Assertions.assertEquals(3, map.size());
        Assertions.assertEquals("nullKey", map.get(null));
        Assertions.assertNull(map.get("nullValue"));
        Assertions.assertEquals("value", map.get("normal"));
        Assertions.assertTrue(map.containsKey(null));
        Assertions.assertTrue(map.containsValue(null));
    }

    @Test
    public void testWrap() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("initial", 1);

        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(mutable);
        Assertions.assertEquals(1, wrapped.size());

        mutable.put("added", 2);
        Assertions.assertEquals(2, wrapped.size());
        Assertions.assertEquals(2, wrapped.get("added"));
    }

    @Test
    public void testWrap_AlreadyImmutable() {
        ImmutableMap<String, Integer> original = ImmutableMap.of("a", 1);
        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(original);
        Assertions.assertSame(original, wrapped);
    }

    @Test
    public void testWrap_Null() {
        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(null);
        Assertions.assertTrue(wrapped.isEmpty());
    }

    @Test
    public void testBuilder() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder()
                .put("one", 1)
                .put("two", 2)
                .putAll(CommonUtil.asMap("three", 3, "four", 4))
                .build();

        Assertions.assertEquals(4, map.size());
        Assertions.assertEquals(1, map.get("one"));
        Assertions.assertEquals(4, map.get("four"));
    }

    @Test
    public void testBuilder_WithBackingMap() {
        Map<String, Integer> backing = new LinkedHashMap<>();
        ImmutableMap<String, Integer> map = ImmutableMap.builder(backing).put("a", 1).put("b", 2).build();

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals(2, backing.size());
    }

    @Test
    public void testBuild_duplicateKeyOverwrites() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("a", 1).put("a", 2).build();
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(2, map.get("a"));
    }

    @Test
    public void testBuild_preservesInsertionOrder() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("c", 3).put("a", 1).put("b", 2).build();

        Iterator<String> keys = map.keySet().iterator();
        Assertions.assertEquals("c", keys.next());
        Assertions.assertEquals("a", keys.next());
        Assertions.assertEquals("b", keys.next());
    }

    @Test
    public void testBuilder_EmptyPutAll() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("a", 1).putAll(null).putAll(new HashMap<>()).build();

        Assertions.assertEquals(1, map.size());
    }

    @Test
    public void testBuild_emptyBuilder() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().build();
        Assertions.assertTrue(map.isEmpty());
        Assertions.assertEquals(0, map.size());
    }

    @Test
    public void testBuild_withNullKeyAndValue() {
        ImmutableMap<String, String> map = ImmutableMap.<String, String> builder().put(null, "nullKey").put("nullVal", null).build();

        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("nullKey", map.get(null));
        Assertions.assertNull(map.get("nullVal"));
        Assertions.assertTrue(map.containsKey(null));
    }

    @Test
    public void testBuilder_NullBackingMap() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ImmutableMap.builder(null));
    }

    @Test
    public void testBuild_singleEntry() {
        ImmutableMap<String, Integer> map = ImmutableMap.<String, Integer> builder().put("x", 42).build();
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals(42, map.get("x"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.put("y", 99));
    }

    @Test
    public void testViews_areUnmodifiable_forOfAndWrapInstances() {
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);

        ImmutableMap<String, Integer> ofMap = ImmutableMap.of("a", 1, "b", 2);
        ImmutableMap<String, Integer> wrappedMap = ImmutableMap.wrap(backing);

        for (ImmutableMap<String, Integer> map : CommonUtil.asList(ofMap, wrappedMap)) {
            // entrySet(): entries must not support setValue, the set must not support removal.
            Map.Entry<String, Integer> entry = map.entrySet().iterator().next();
            Assertions.assertThrows(UnsupportedOperationException.class, () -> entry.setValue(99));
            Assertions.assertThrows(UnsupportedOperationException.class, () -> map.entrySet().clear());
            Assertions.assertThrows(UnsupportedOperationException.class, () -> map.entrySet().remove(entry));

            Iterator<Map.Entry<String, Integer>> entryIter = map.entrySet().iterator();
            entryIter.next();
            Assertions.assertThrows(UnsupportedOperationException.class, entryIter::remove);

            // keySet() view and its iterator.
            Set<String> keys = map.keySet();
            Assertions.assertThrows(UnsupportedOperationException.class, () -> keys.remove("a"));
            Iterator<String> keyIter = keys.iterator();
            keyIter.next();
            Assertions.assertThrows(UnsupportedOperationException.class, keyIter::remove);

            // values() view and its iterator.
            Collection<Integer> values = map.values();
            Assertions.assertThrows(UnsupportedOperationException.class, () -> values.remove(1));
            Iterator<Integer> valueIter = values.iterator();
            valueIter.next();
            Assertions.assertThrows(UnsupportedOperationException.class, valueIter::remove);
        }

        // Values are still readable through the views.
        Assertions.assertTrue(ofMap.keySet().contains("a"));
        Assertions.assertTrue(wrappedMap.values().contains(2));
    }

    @Test
    public void testWrapDoesNotTrustBackingMapClassNameForImmutability() {
        MutableImmutableNamedMap<String, Integer> backing = new MutableImmutableNamedMap<>();
        backing.put("a", 1);

        ImmutableMap<String, Integer> wrapped = ImmutableMap.wrap(backing);
        Map.Entry<String, Integer> entry = wrapped.entrySet().iterator().next();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> entry.setValue(2));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> wrapped.keySet().remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> wrapped.values().remove(1));
        Assertions.assertEquals(1, backing.get("a"));
    }

    @Test
    public void testCopyOf_copiesAWrappedView() {
        final Map<String, Integer> live = new LinkedHashMap<>();
        live.put("a", 1);

        final ImmutableMap<String, Integer> view = ImmutableMap.wrap(live);
        final ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(view);

        Assertions.assertNotSame(view, copy);

        live.put("b", 2);

        Assertions.assertEquals(2, view.size());
        Assertions.assertEquals(1, copy.size());
        Assertions.assertFalse(copy.containsKey("b"));
    }

    @Test
    public void testCopyOf_returnsSameInstanceForAnOwningMap() {
        final ImmutableMap<String, Integer> owned = ImmutableMap.of("a", 1);
        Assertions.assertSame(owned, ImmutableMap.copyOf(owned));
        Assertions.assertSame(ImmutableMap.empty(), ImmutableMap.copyOf(ImmutableMap.empty()));

        final ImmutableMap<String, Integer> copied = ImmutableMap.copyOf(new HashMap<>(Map.of("a", 1)));
        Assertions.assertSame(copied, ImmutableMap.copyOf(copied));
    }

    @Test
    public void testCopyOf_builderResults() {
        final ImmutableMap.Builder<String, Integer> privateBuilder = ImmutableMap.<String, Integer> builder().put("a", 1);
        final ImmutableMap<String, Integer> fromPrivateStorage = privateBuilder.build();
        Assertions.assertSame(fromPrivateStorage, ImmutableMap.copyOf(fromPrivateStorage));
        final ImmutableMap<String, Integer> repeatedBuild = privateBuilder.build();
        Assertions.assertSame(repeatedBuild, ImmutableMap.copyOf(repeatedBuild));
        Assertions.assertEquals(fromPrivateStorage, repeatedBuild);
        Assertions.assertThrows(IllegalStateException.class, () -> privateBuilder.put("b", 2));
        Assertions.assertEquals(Map.of("a", 1), fromPrivateStorage);
        Assertions.assertEquals(Map.of("a", 1), repeatedBuild);

        final Map<String, Integer> holder = new LinkedHashMap<>();
        final ImmutableMap<String, Integer> fromHolder = ImmutableMap.builder(holder).put("a", 1).build();
        final ImmutableMap<String, Integer> copy = ImmutableMap.copyOf(fromHolder);
        Assertions.assertNotSame(fromHolder, copy);

        holder.put("b", 2);
        holder.put("a", 3);
        Assertions.assertEquals(2, fromHolder.size());
        Assertions.assertEquals(1, copy.size());
        Assertions.assertEquals(3, fromHolder.get("a"));
        Assertions.assertEquals(1, copy.get("a"));
    }

    @Test
    public void testCopyOf_preservesEntryOrderThroughAWrapper() {
        final LinkedHashMap<String, Integer> ordered = new LinkedHashMap<>();
        final java.util.List<String> order = java.util.Arrays.asList("z", "y", "x", "w", "v", "u", "t", "s", "r", "q");

        for (int i = 0; i < order.size(); i++) {
            ordered.put(order.get(i), i);
        }

        Assertions.assertEquals(order, new java.util.ArrayList<>(ImmutableMap.copyOf(ordered).keySet()));
        Assertions.assertEquals(order, new java.util.ArrayList<>(ImmutableMap.copyOf(java.util.Collections.unmodifiableMap(ordered)).keySet()));
        Assertions.assertEquals(order, new java.util.ArrayList<>(ImmutableMap.copyOf(java.util.Collections.synchronizedMap(ordered)).keySet()));
    }

    @Test
    public void testBuilderIsConsumedByBuild() {
        final ImmutableMap.Builder<String, Integer> builder = ImmutableMap.builder();
        final ImmutableMap<String, Integer> built = builder.put("a", 1).build();

        Assertions.assertEquals(1, built.size());
        Assertions.assertThrows(IllegalStateException.class, () -> builder.put("b", 2));
        Assertions.assertThrows(IllegalStateException.class, () -> builder.putAll(Map.of("b", 2)));

        Assertions.assertEquals(1, built.size());
        Assertions.assertEquals(built, builder.build());
    }

    @Test
    public void testGetOrDefaultDistinguishesNullValueFromAbsence() {
        final Map<String, Integer> backing = new HashMap<>();
        backing.put("nullValued", null);
        backing.put("a", 1);

        final ImmutableMap<String, Integer> map = ImmutableMap.wrap(backing);

        Assertions.assertNull(map.getOrDefault("nullValued", 9));
        Assertions.assertEquals(9, map.getOrDefault("absent", 9));
        Assertions.assertEquals(1, map.getOrDefault("a", 9));
    }

    @Test
    public void testForEachIteratesEveryEntryInOrder() {
        final ImmutableMap<String, Integer> map = ImmutableMap.of("a", 1, "b", 2, "c", 3);
        final java.util.List<String> seen = new java.util.ArrayList<>();

        map.forEach((k, v) -> seen.add(k + "=" + v));

        Assertions.assertEquals(java.util.Arrays.asList("a=1", "b=2", "c=3"), seen);
        Assertions.assertThrows(NullPointerException.class, () -> map.forEach(null));
    }

    @Test
    public void testEmptyMapViewsReportTheirInterfaceCharacteristics() {
        // Collections.emptyMap()'s views report only SIZED|SUBSIZED, dropping the DISTINCT that
        // Set.spliterator() promises for keySet()/entrySet() and the ORDERED that the views of every
        // ImmutableMap built by of(...)/copyOf(...)/a no-argument builder() report - they all back onto a
        // LinkedHashMap. A wrap(aHashMap) view reports no ORDERED, so this is not a family-wide property.
        final ImmutableMap<String, Integer> empty = ImmutableMap.empty();
        final ImmutableMap<String, Integer> one = ImmutableMap.of("a", 1);

        Assertions.assertEquals(one.keySet().spliterator().characteristics(), empty.keySet().spliterator().characteristics());
        Assertions.assertEquals(one.values().spliterator().characteristics(), empty.values().spliterator().characteristics());
        Assertions.assertEquals(one.entrySet().spliterator().characteristics(), empty.entrySet().spliterator().characteristics());

        Assertions.assertTrue(empty.keySet().spliterator().hasCharacteristics(java.util.Spliterator.DISTINCT));
        Assertions.assertTrue(empty.keySet().spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));
        Assertions.assertTrue(empty.entrySet().spliterator().hasCharacteristics(java.util.Spliterator.DISTINCT));
        Assertions.assertTrue(empty.values().spliterator().hasCharacteristics(java.util.Spliterator.ORDERED));

        // Stream.concat intersects both sides' characteristics, so an unordered empty prefix used to strip
        // ORDERED from the whole stream and make a parallel findFirst() stop meaning "first".
        final java.util.List<Integer> big = new java.util.ArrayList<>();

        for (int i = 0; i < 1000; i++) {
            big.add(i);
        }

        Assertions.assertTrue(java.util.stream.Stream.concat(ImmutableMap.<Integer, Integer> empty().keySet().stream(), big.stream())
                .spliterator()
                .hasCharacteristics(java.util.Spliterator.ORDERED));

        for (int i = 0; i < 50; i++) {
            Assertions.assertEquals(java.util.Optional.of(0),
                    java.util.stream.Stream.concat(ImmutableMap.<Integer, Integer> empty().keySet().stream(), big.stream()).parallel().findFirst());
        }
    }

    @Test
    public void testEmptyMapStillAcceptsNullQueries() {
        // pins the other half of the fix above: Map.of() would report the right characteristics but its
        // get(null)/containsKey(null) throw NullPointerException, which this empty map must never do.
        Assertions.assertNull(ImmutableMap.empty().get(null));
        Assertions.assertFalse(ImmutableMap.empty().containsKey(null));
        Assertions.assertFalse(ImmutableMap.empty().containsValue(null));

        Assertions.assertEquals(java.util.Collections.emptyMap(), ImmutableMap.empty());
        Assertions.assertEquals(0, ImmutableMap.empty().hashCode());
        Assertions.assertEquals("{}", ImmutableMap.empty().toString());
        Assertions.assertSame(ImmutableMap.empty(), ImmutableMap.copyOf(new HashMap<String, Integer>()));
        Assertions.assertTrue(ImmutableMap.empty().keySet().isEmpty());
        Assertions.assertTrue(ImmutableMap.empty().entrySet().isEmpty());

        // ... and pins the third consequence of the same one-line change: backing the singleton with an
        // unmodifiable LinkedHashMap makes its views reject mutation, which is what
        // AbstractImmutableMap.keySet()/values()/entrySet() document and what every non-empty ImmutableMap
        // already did. Collections.emptyMap()'s views silently no-op'd (clear()) or answered false
        // (remove/removeAll/removeIf/retainAll) instead, so a revert to N.emptyMap() must red this.
        final ImmutableMap<String, Integer> empty = ImmutableMap.empty();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.keySet().remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.keySet().removeIf(k -> true));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.keySet().retainAll(java.util.Collections.emptyList()));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.keySet().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.values().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.entrySet().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> empty.entrySet().removeIf(e -> true));

        // identical to the answer a non-empty instance has always given
        final ImmutableMap<String, Integer> one = ImmutableMap.of("a", 1);

        Assertions.assertThrows(UnsupportedOperationException.class, () -> one.keySet().remove("a"));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> one.values().clear());
        Assertions.assertThrows(UnsupportedOperationException.class, () -> one.entrySet().clear());
    }
}
