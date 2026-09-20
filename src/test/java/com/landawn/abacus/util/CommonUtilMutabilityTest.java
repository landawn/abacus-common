package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractCollection;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.WeakHashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Regression coverage for SN 1225: classification must not observe mutability by changing caller data. */
public class CommonUtilMutabilityTest extends TestBase {

    @Test
    public void recognizesUnmodifiableCollectionsAndImmutableMarker() {
        final List<Collection<?>> collections = List.of(Collections.emptyList(), Collections.emptySet(), Collections.singleton("a"),
                Collections.singletonList("a"), Collections.nCopies(0, "a"), Collections.nCopies(2, "a"), Collections.unmodifiableCollection(new ArrayList<>()),
                Collections.unmodifiableList(new ArrayList<>()), Collections.unmodifiableList(new LinkedList<>()), Collections.unmodifiableSet(new HashSet<>()),
                Collections.unmodifiableSortedSet(new TreeSet<>()), Collections.unmodifiableNavigableSet(new TreeSet<>()), Collections.emptySortedSet(),
                Collections.emptyNavigableSet(), Collections.unmodifiableSequencedCollection(new ArrayList<>()),
                Collections.unmodifiableSequencedSet(new LinkedHashSet<>()), List.of(), List.of("a"), List.of("a", "b"), List.of("a", "b", "c"),
                List.of("a", "b").subList(0, 1), List.of("a", "b", "c").subList(0, 2).subList(0, 1), Set.of(), Set.of("a"), Set.of("a", "b"),
                Set.of("a", "b", "c"), Arrays.asList("a", null).stream().toList(), ImmutableList.of("a"), ImmutableSet.of("a"));

        for (final Collection<?> c : collections) {
            assertEquals(Mutability.KNOWN_UNMODIFIABLE, N.mutabilityOf(c), c.getClass().getName());
            assertSame(c, N.unmodifiableCollection(c));
        }
    }

    @Test
    public void recognizesUnmodifiableMapsAndImmutableMarker() {
        final List<Map<?, ?>> maps = List.of(Collections.emptyMap(), Collections.singletonMap("k", "v"), Collections.unmodifiableMap(new HashMap<>()),
                Collections.unmodifiableSortedMap(new TreeMap<>()), Collections.unmodifiableNavigableMap(new TreeMap<>()),
                Collections.unmodifiableSequencedMap(new LinkedHashMap<>()), Collections.emptySortedMap(), Collections.emptyNavigableMap(), Map.of(),
                Map.of("k", "v"), Map.of("k", "v", "k2", "v2"), ImmutableMap.of("k", "v"), ImmutableBiMap.of("k", "v"), ImmutableSortedMap.of("k", "v"));

        for (final Map<?, ?> m : maps) {
            assertEquals(Mutability.KNOWN_UNMODIFIABLE, N.mutabilityOf(m), m.getClass().getName());
            assertSame(m, N.unmodifiableMap(m));
        }
    }

    @Test
    public void recognizesMutableImplementationsDespiteElementRestrictions() {
        final List<Collection<?>> collections = List.of(new ArrayList<>(), new LinkedList<>(), new HashSet<>(), new LinkedHashSet<>(), new TreeSet<>(),
                new ArrayDeque<>(), new IdentityHashSet<>());
        for (final Collection<?> c : collections) {
            assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(c), c.getClass().getName());
        }

        for (final Map<?, ?> m : List.of(new HashMap<>(), new LinkedHashMap<>(), new TreeMap<>(), new ConcurrentHashMap<>())) {
            assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(m), m.getClass().getName());
        }

        final TreeSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.add("a");
        assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(sorted));
        assertThrows(NullPointerException.class, () -> sorted.add(null));
        assertTrue(sorted.add("b"));
    }

    @Test
    public void recognizesTheRemainingMutableJdkImplementations() {
        final List<Collection<?>> collections = List.of(new Vector<>(), new Stack<>(), new PriorityQueue<>(), new CopyOnWriteArrayList<>(),
                new CopyOnWriteArraySet<>(), new ConcurrentLinkedQueue<>(), new ConcurrentLinkedDeque<>(), new LinkedBlockingQueue<>(),
                new LinkedBlockingDeque<>(), new ConcurrentSkipListSet<>(), new ArrayBlockingQueue<>(1), new PriorityBlockingQueue<>(),
                new LinkedTransferQueue<>(), EnumSet.noneOf(Thread.State.class), EnumSet.allOf(Thread.State.class),
                EnumSet.noneOf(Character.UnicodeScript.class), EnumSet.allOf(Character.UnicodeScript.class));

        for (final Collection<?> c : collections) {
            assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(c), c.getClass().getName());
        }

        final List<Map<?, ?>> maps = List.of(new IdentityHashMap<>(), new WeakHashMap<>(), new EnumMap<>(Thread.State.class), new Hashtable<>(),
                new ConcurrentSkipListMap<>(), new Properties());

        for (final Map<?, ?> m : maps) {
            assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(m), m.getClass().getName());
        }
    }

    @Test
    public void subclassesOfKnownMutableClassesStayUnknown() {
        // Registering Properties itself must not also register subclasses that can restrict mutation.
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(new Properties() {
            private static final long serialVersionUID = 1L;
        }));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(new ArrayList<>() {
            private static final long serialVersionUID = 1L;
        }));
    }

    @Test
    public void synchronousQueuesStayUnknownAndBoundedQueuesRemainMutable() {
        // SynchronousQueue.add(..) fails unless a consumer is already waiting.
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(new SynchronousQueue<>()));
        assertThrows(IllegalStateException.class, () -> new SynchronousQueue<>().add("a"));

        // Capacity restrictions do not make a bounded queue unmodifiable. Classification must leave it untouched.
        final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<>(1);
        queue.add("full");
        assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(queue));
        assertEquals(List.of("full"), new ArrayList<>(queue));
        assertThrows(IllegalStateException.class, () -> queue.add("overflow"));
        assertEquals("full", queue.remove());
        assertTrue(queue.add("replacement"));
    }

    @Test
    public void nullHasNoKnownMutability() {
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf((Collection<?>) null));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf((Map<?, ?>) null));
    }

    @Test
    public void partiallyMutableListsAndViewsRemainUnknownAndAreStillWrapped() {
        final List<String> fixed = Arrays.asList("a", "b");
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(fixed));
        assertThrows(UnsupportedOperationException.class, () -> fixed.add("c"));
        assertEquals("a", fixed.set(0, "changed"));
        assertThrows(UnsupportedOperationException.class, () -> N.unmodifiableList(fixed).set(0, "blocked"));

        // Registering List.of sublists must not classify mutable ArrayList views as unmodifiable.
        final List<String> backingList = new ArrayList<>(List.of("a", "b"));
        final List<String> subList = backingList.subList(0, 1);
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(subList));
        assertThrows(UnsupportedOperationException.class, () -> N.unmodifiableList(subList).set(0, "blocked"));
        assertEquals("a", subList.set(0, "changed"));
        assertEquals(List.of("changed", "b"), backingList);

        final Map<String, Integer> backing = new HashMap<>(Map.of("k", 1));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(backing.keySet()));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(backing.values()));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(backing.entrySet()));
        assertThrows(UnsupportedOperationException.class, () -> N.unmodifiableCollection(backing.keySet()).remove("k"));
        assertEquals(Map.of("k", 1), backing);
        assertTrue(backing.keySet().remove("k"));

        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(new TreeMap<String, Integer>().descendingMap()));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(Collections.checkedList(new ArrayList<>(), String.class)));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(Collections.synchronizedMap(new HashMap<>())));
    }

    @Test
    public void sameJdkViewClassCanHaveDifferentInsertionCapabilities() {
        final Set<String> addable = ConcurrentHashMap.newKeySet();
        final Set<String> nonAddable = new ConcurrentHashMap<String, Boolean>().keySet();
        assertSame(addable.getClass(), nonAddable.getClass());

        for (int i = 0; i < 2; i++) {
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(addable));
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(nonAddable));
        }
        assertTrue(addable.add("a"));
        assertThrows(UnsupportedOperationException.class, () -> nonAddable.add("a"));
    }

    @Test
    public void customCollectionMethodsAreNeverInvoked() {
        final Collection<Object> c = new AbstractCollection<>() {
            @Override
            public Iterator<Object> iterator() {
                throw new AssertionError("iterator must not be called");
            }

            @Override
            public int size() {
                throw new AssertionError("size must not be called");
            }

            @Override
            public boolean add(final Object e) {
                throw new AssertionError("add must not be called");
            }
        };
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(c));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(c));
    }

    @Test
    public void customMapMethodsAreNeverInvoked() {
        final Map<Object, Object> m = new AbstractMap<>() {
            @Override
            public Set<Entry<Object, Object>> entrySet() {
                throw new AssertionError("entrySet must not be called");
            }

            @Override
            public Object put(final Object key, final Object value) {
                throw new AssertionError("put must not be called");
            }

            @Override
            public boolean containsKey(final Object key) {
                throw new AssertionError("containsKey must not be called");
            }

            @Override
            public int size() {
                throw new AssertionError("size must not be called");
            }
        };
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(m));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(m));
    }

    private static final class ConfigurableList extends AbstractList<String> {
        private final List<String> backing = new ArrayList<>(List.of("original"));
        private boolean writable;
        private int addCalls;

        @Override
        public String get(final int index) {
            return backing.get(index);
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean add(final String value) {
            addCalls++;
            if (!writable) {
                throw new UnsupportedOperationException();
            }
            return backing.add(value);
        }
    }

    private static final class ConfigurableMap extends AbstractMap<String, String> {
        private final Map<String, String> backing;
        private int putCalls;

        ConfigurableMap(final Map<String, String> backing) {
            this.backing = backing;
        }

        @Override
        public Set<Entry<String, String>> entrySet() {
            return backing.entrySet();
        }

        @Override
        public String put(final String key, final String value) {
            putCalls++;
            return backing.put(key, value);
        }
    }

    @Test
    public void customInstanceStateDoesNotEstablishClassWideMutability() {
        final ConfigurableList readOnly = new ConfigurableList();
        final ConfigurableList writable = new ConfigurableList();
        writable.writable = true;
        for (int i = 0; i < 2; i++) {
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(readOnly));
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(writable));
        }
        readOnly.writable = true;
        writable.writable = false;
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(writable));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(readOnly));
        assertEquals(0, readOnly.addCalls);
        assertEquals(0, writable.addCalls);
        assertEquals(List.of("original"), readOnly);
        assertEquals(List.of("original"), writable);

        final ConfigurableMap mutableMap = new ConfigurableMap(new HashMap<>(Map.of("k", "v")));
        final ConfigurableMap immutableMap = new ConfigurableMap(Map.of("k", "v"));
        for (int i = 0; i < 2; i++) {
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(mutableMap));
            assertEquals(Mutability.UNKNOWN, N.mutabilityOf(immutableMap));
        }
        assertEquals(0, mutableMap.putCalls);
        assertEquals(0, immutableMap.putCalls);
        assertEquals(Map.of("k", "v"), mutableMap);
        assertEquals(Map.of("k", "v"), immutableMap);
    }

    private static final class EvictingMap extends LinkedHashMap<Object, Object> {
        private static final long serialVersionUID = 1L;
        private int evictions;

        @Override
        protected boolean removeEldestEntry(final Map.Entry<Object, Object> eldest) {
            if (size() > 2) {
                evictions++;
                return true;
            }
            return false;
        }
    }

    @Test
    public void classificationNeverEvictsRealMapEntriesOrInvalidatesIterators() {
        final EvictingMap map = new EvictingMap();
        map.put("first", "value");
        map.put(null, "existing null-key value");
        final Map<Object, Object> before = new LinkedHashMap<>(map);
        final Iterator<Object> keys = map.keySet().iterator();

        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(map));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(map));
        assertEquals(0, map.evictions);
        assertEquals(before, map);
        assertEquals("first", keys.next());
        assertEquals(null, keys.next());
        assertFalse(keys.hasNext());
    }

    private static final class EvictingCollection extends ArrayDeque<Object> {
        private static final long serialVersionUID = 1L;
        private int addCalls;

        @Override
        public boolean add(final Object value) {
            addCalls++;
            if (size() == 2) {
                removeFirst();
            }
            return super.add(value);
        }
    }

    @Test
    public void classificationNeverEvictsRealCollectionElementsOrNotifiesMutationObservers() {
        final EvictingCollection c = new EvictingCollection();
        c.add("first");
        c.add("second");
        c.addCalls = 0;
        final Iterator<Object> elements = c.iterator();

        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(c));
        assertEquals(Mutability.UNKNOWN, N.mutabilityOf(c));
        assertEquals(0, c.addCalls);
        assertEquals(List.of("first", "second"), new ArrayList<>(c));
        assertEquals("first", elements.next());
        assertEquals("second", elements.next());
        assertFalse(elements.hasNext());
    }

    @Test
    public void classificationPreservesAccessOrderAndLiveIterators() {
        final LinkedHashMap<String, String> map = new LinkedHashMap<>(4, 0.75f, true);
        map.put("a", "A");
        map.put("b", "B");
        map.put("c", "C");
        map.get("a");
        final Iterator<String> keys = map.keySet().iterator();

        assertEquals(Mutability.KNOWN_MUTABLE, N.mutabilityOf(map));
        assertEquals(List.of("b", "c", "a"), new ArrayList<>(map.keySet()));
        assertEquals("b", keys.next());
        assertEquals("c", keys.next());
        assertEquals("a", keys.next());
        assertFalse(keys.hasNext());
    }
}
