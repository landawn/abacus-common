package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

public class CommonUtilProbeTest extends CommonUtilTestSupport {

    @Test
    public void testProbeUnmodifiable_collections() {
        assertTrue(CommonUtil.probeUnmodifiable((Collection) null));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.emptyList()));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.emptySet()));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.singleton("value")));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.singletonList("value")));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableList(new ArrayList<>(Arrays.asList("a", "b")))));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableSet(new HashSet<>(Arrays.asList("a", "b")))));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList("a", "b")))));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableNavigableSet(new TreeSet<>(Arrays.asList("a", "b")))));
        assertTrue(CommonUtil.probeUnmodifiable(List.of("a", "b")));
        assertTrue(CommonUtil.probeUnmodifiable(Set.of("a", "b")));
        assertTrue(CommonUtil.probeUnmodifiable(ImmutableList.of("a", "b")));
        assertTrue(CommonUtil.probeUnmodifiable(ImmutableSet.of("a", "b")));
        assertTrue(CommonUtil.probeUnmodifiable(List.of("a", "b").stream().toList()));
        assertFalse(CommonUtil.probeUnmodifiable(List.of("a", "b").stream().collect(Collectors.toList())));

        assertFalse(CommonUtil.probeUnmodifiable(new ArrayList<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new HashSet<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new LinkedList<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new TreeSet<>()));
        assertFalse(CommonUtil.probeUnmodifiable(ConcurrentHashMap.newKeySet()));

        List<String> list = new ArrayList<>();
        list.add("original");
        assertFalse(CommonUtil.probeUnmodifiable(list));
        assertEquals(1, list.size());
        assertEquals("original", list.get(0));
        assertFalse(CommonUtil.probeUnmodifiable(new ArrayList<>()));
    }

    @Test
    public void testProbeUnmodifiable_maps() {
        assertTrue(CommonUtil.probeUnmodifiable((Map) null));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.emptyMap()));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.singletonMap("key", "value")));
        Map<String, String> modifiable = new HashMap<>();
        modifiable.put("key", "value");
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableMap(modifiable)));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableSortedMap(new TreeMap<>(Map.of("key", "value")))));
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableNavigableMap(new TreeMap<>(Map.of("key", "value")))));
        assertTrue(CommonUtil.probeUnmodifiable(Map.of("k1", "v1", "k2", "v2")));
        assertTrue(CommonUtil.probeUnmodifiable(Map.ofEntries(Map.entry("k1", "v1"), Map.entry("k2", "v2"))));
        assertTrue(CommonUtil.probeUnmodifiable(ImmutableMap.of("k1", "v1")));
        assertTrue(CommonUtil.probeUnmodifiable(ImmutableBiMap.of("k1", "v1")));
        assertTrue(CommonUtil.probeUnmodifiable(ImmutableSortedMap.of("k1", "v1")));

        assertFalse(CommonUtil.probeUnmodifiable(new HashMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new TreeMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new LinkedHashMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new ConcurrentHashMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new IdentityHashMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new WeakHashMap<>()));
        assertFalse(CommonUtil.probeUnmodifiable(new EnumMap<>(DayOfWeek.class)));
        Map<DayOfWeek, String> enumMap = new EnumMap<>(DayOfWeek.class);
        enumMap.put(DayOfWeek.MONDAY, "First day");
        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableMap(enumMap)));

        Map<String, String> map = new HashMap<>();
        map.put("original", "value");
        assertFalse(CommonUtil.probeUnmodifiable(map));
        assertEquals(1, map.size());
        assertEquals("value", map.get("original"));
        Map<String, String> withNull = new HashMap<>();
        withNull.put(null, "value");
        assertFalse(CommonUtil.probeUnmodifiable(withNull));
    }

    @Test
    public void testProbeUnmodifiable_customImplementations() {
        Collection<String> unmodifiable = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String s) {
                throw new UnsupportedOperationException();
            }
        };
        assertTrue(CommonUtil.probeUnmodifiable(unmodifiable));

        Collection<String> otherException = new AbstractCollection<>() {
            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String s) {
                throw new IllegalStateException("Custom exception");
            }
        };
        assertFalse(CommonUtil.probeUnmodifiable(otherException));

        Map<String, String> unmodifiableMap = new AbstractMap<>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public String put(String key, String value) {
                throw new UnsupportedOperationException();
            }
        };
        assertTrue(CommonUtil.probeUnmodifiable(unmodifiableMap));

        Map<String, String> otherMap = new AbstractMap<>() {
            @Override
            public Set<Entry<String, String>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public String put(String key, String value) {
                throw new IllegalStateException("Custom exception");
            }
        };
        assertFalse(CommonUtil.probeUnmodifiable(otherMap));
    }
    @Test
    public void testProbeUnmodifiable_mapClassesAnsweredFromStaticClassification() {
        final Map<String, String> hash = new HashMap<>();
        final Map<String, String> linked = new LinkedHashMap<>();
        final Map<String, String> tree = new TreeMap<>();
        final Map<String, String> concurrent = new ConcurrentHashMap<>();

        assertFalse(CommonUtil.probeUnmodifiable(hash));
        assertFalse(CommonUtil.probeUnmodifiable(linked));
        assertFalse(CommonUtil.probeUnmodifiable(tree));
        assertFalse(CommonUtil.probeUnmodifiable(concurrent));

        assertEquals(0, hash.size());
        assertEquals(0, linked.size());
        assertEquals(0, tree.size());
        assertEquals(0, concurrent.size());

        assertTrue(CommonUtil.probeUnmodifiable(Collections.unmodifiableMap(new HashMap<String, String>())));
        assertTrue(CommonUtil.probeUnmodifiable(Map.of("k", "v")));
        assertTrue(CommonUtil.probeUnmodifiable((Map<?, ?>) null));
    }

    /** A map whose {@code put} succeeds and which counts how many times it was called. */
    private static class ProbeableMap extends AbstractMap<Object, Object> {
        private final Map<Object, Object> backing = new HashMap<>();

        int puts;

        @Override
        public Object put(final Object key, final Object value) {
            puts++;
            return backing.put(key, value);
        }

        @Override
        public Object remove(final Object key) {
            return backing.remove(key);
        }

        @Override
        public Set<Entry<Object, Object>> entrySet() {
            return backing.entrySet();
        }
    }

    /** The same map, marked {@link Immutable} - the marker is the only difference. */
    private static final class ImmutableMarkedMap extends ProbeableMap implements Immutable {
    }

    /**
     * Pins the "No probe for known classes" bullet: a map marked {@link Immutable} is answered from the static
     * classification and is never probe-mutated, even though its {@code put} would succeed. Without the marker the
     * very same map is probed - the sentinel {@code put} lands, the map is reported modifiable, and the probe is
     * rolled back.
     */
    @Test
    public void testProbeUnmodifiable_mapMarkedImmutableIsNeverProbed() {
        final ImmutableMarkedMap marked = new ImmutableMarkedMap();

        assertTrue(CommonUtil.probeUnmodifiable(marked));
        assertEquals(0, marked.puts, "a map marked Immutable must not be probe-mutated");
        assertEquals(0, marked.size());

        final ProbeableMap probeable = new ProbeableMap();

        assertFalse(CommonUtil.probeUnmodifiable(probeable));
        assertEquals(1, probeable.puts);
        assertEquals(0, probeable.size());
    }

}
