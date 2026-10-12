package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.SetMultimap;

@Tag("unit")
class MultimapDeclaredCollectionRegressionTest extends TestBase {

    @Test
    void concreteSortedSetsRetainTypeAndOrderingAfterNewPuts() {
        final Type<Multimap<String, Integer, TreeSet<Integer>>> type = TypeFactory.getType("Multimap<String, Integer, TreeSet<Integer>>");
        final Multimap<String, Integer, TreeSet<Integer>> parsed = type.valueOf("{\"z\": [3, 1, 2], \"a\": [7]}");

        assertEquals(TreeSet.class, parsed.get("z").getClass());
        parsed.put("z", 0);
        parsed.put("fresh", 9);
        parsed.put("fresh", 8);
        assertEquals(List.of(0, 1, 2, 3), new ArrayList<>(parsed.get("z")));
        assertEquals(List.of(8, 9), new ArrayList<>(parsed.get("fresh")));
        assertNotSame(parsed.get("z"), parsed.get("fresh"));
        assertEquals(List.of("z", "a", "fresh"), new ArrayList<>(parsed.keySet()));
    }

    @Test
    void twoParameterLinkedListDeclarationRetainsConcreteValues() {
        final Type<Multimap<String, Integer, LinkedList<Integer>>> type = TypeFactory.getType("Multimap<String, LinkedList<Integer>>");
        final Multimap<String, Integer, LinkedList<Integer>> parsed = type.valueOf("{\"k\": [2, 1, 2]}");

        assertEquals(LinkedList.class, parsed.get("k").getClass());
        assertEquals(2, parsed.get("k").removeFirst());
        parsed.put("fresh", 3);
        assertEquals(LinkedList.class, parsed.get("fresh").getClass());
        assertEquals(List.of(1, 2), parsed.get("k"));
    }

    @Test
    void dequeValuesRetainQueueOperationsAndFreshCollectionFactories() {
        final Type<Multimap<String, Integer, ArrayDeque<Integer>>> type = TypeFactory.getType("Multimap<String, Integer, ArrayDeque<Integer>>");
        final Multimap<String, Integer, ArrayDeque<Integer>> parsed = type.valueOf("{\"k\": [2, 1]}");

        assertEquals(ArrayDeque.class, parsed.get("k").getClass());
        assertEquals(2, parsed.get("k").removeFirst());
        parsed.put("fresh", 3);
        assertEquals(ArrayDeque.class, parsed.get("fresh").getClass());
        assertEquals(List.of(1), new ArrayList<>(parsed.get("k")));

        final Type<Multimap<String, Integer, Queue<Integer>>> interfaceType = TypeFactory.getType("Multimap<String, Integer, Queue<Integer>>");
        assertEquals(4, interfaceType.valueOf("{\"k\": [4]}").get("k").poll());
    }

    @Test
    void sortedSetInterfacesUseSortedCollections() {
        final Type<Multimap<String, Integer, SortedSet<Integer>>> type = TypeFactory.getType("Multimap<String, SortedSet<Integer>>");
        final Multimap<String, Integer, SortedSet<Integer>> parsed = type.valueOf("{\"k\": [3, 1, 3]}");

        assertEquals(1, parsed.get("k").first());
        parsed.put("k", 0);
        assertEquals(List.of(0, 1, 3), new ArrayList<>(parsed.get("k")));
    }

    @Test
    void ordinaryListAndSetDeclarationsKeepTheirSpecializedMultimaps() {
        final Type<Multimap<String, Integer, List<Integer>>> listType = TypeFactory.getType("Multimap<String, List<Integer>>");
        final Type<Multimap<String, Integer, Set<Integer>>> setType = TypeFactory.getType("Multimap<String, Integer, Set<Integer>>");

        assertInstanceOf(ListMultimap.class, listType.valueOf("{\"k\": [1, 1]}"));
        assertEquals(List.of(1, 1), listType.valueOf("{\"k\": [1, 1]}").get("k"));
        assertInstanceOf(SetMultimap.class, setType.valueOf("{\"k\": [1, 1]}"));
        assertEquals(Set.of(1), setType.valueOf("{\"k\": [1, 1]}").get("k"));
    }

    // ---- bug review 2026-10-02 final G128 begin ----

    private static Multimap<String, Integer, ? extends java.util.Collection<Integer>> parseG128(final String typeName, final String json) {
        final Type<Multimap<String, Integer, ? extends java.util.Collection<Integer>>> type = TypeFactory.getType(typeName);
        return type.valueOf(json);
    }

    // Unchanged: a declared set type that LinkedHashSet already is (Set, HashSet, AbstractSet, LinkedHashSet, SetMultimap<K, E>)
    // keeps linked set values, so later puts keep their insertion order.
    @Test
    void setDeclarationsSatisfiedByLinkedHashSetKeepLinkedValues() {
        for (final String typeName : new String[] { "Multimap<String, Integer, Set<Integer>>", "Multimap<String, HashSet<Integer>>",
                "Multimap<String, Integer, java.util.AbstractSet<Integer>>", "Multimap<String, LinkedHashSet<Integer>>", "SetMultimap<String, Integer>" }) {
            final Multimap<String, Integer, ? extends java.util.Collection<Integer>> parsed = parseG128(typeName, "{\"k\": [1]}");

            assertInstanceOf(SetMultimap.class, parsed, typeName);
            assertEquals(java.util.LinkedHashSet.class, parsed.get("k").getClass(), typeName);
            parsed.put("fresh", 30);
            parsed.put("fresh", 10);
            parsed.put("fresh", 20);
            assertEquals(java.util.LinkedHashSet.class, parsed.get("fresh").getClass(), typeName);
            assertEquals(List.of(30, 10, 20), new ArrayList<>(parsed.get("fresh")), typeName);
        }
    }

    // Unchanged: a declared type that ArrayList already is (Collection, AbstractCollection, List, ArrayList, ListMultimap<K, E>)
    // keeps ArrayList values in a ListMultimap.
    @Test
    void declarationsSatisfiedByArrayListKeepListMultimap() {
        for (final String typeName : new String[] { "Multimap<String, Integer, java.util.Collection<Integer>>",
                "Multimap<String, java.util.AbstractCollection<Integer>>", "Multimap<String, List<Integer>>", "Multimap<String, Integer, ArrayList<Integer>>",
                "ListMultimap<String, Integer>" }) {
            final Multimap<String, Integer, ? extends java.util.Collection<Integer>> parsed = parseG128(typeName, "{\"k\": [2, 1, 2]}");

            assertInstanceOf(ListMultimap.class, parsed, typeName);
            assertEquals(ArrayList.class, parsed.get("k").getClass(), typeName);
            assertEquals(List.of(2, 1, 2), parsed.get("k"), typeName);
            parsed.put("fresh", 3);
            assertEquals(ArrayList.class, parsed.get("fresh").getClass(), typeName);
        }
    }

    // Unchanged: a declared collection without a factory (ImmutableCollection; the reader builds it through a converter)
    // still parses, into the default ArrayList values, instead of failing.
    @Test
    void declaredCollectionWithoutFactoryKeepsDefaultValues() {
        final Multimap<String, Integer, ? extends java.util.Collection<Integer>> parsed = parseG128(
                "Multimap<String, Integer, com.landawn.abacus.util.ImmutableCollection<Integer>>", "{\"k\": [2, 1]}");

        assertInstanceOf(ListMultimap.class, parsed);
        assertEquals(List.of(2, 1), new ArrayList<>(parsed.get("k")));
    }

    // Other declared classes get their own factory for every key, including concrete JDK and nested classes
    @Test
    void otherDeclaredCollectionsUseTheirOwnFactoryForEveryKey() {
        final Multimap<String, Integer, ? extends java.util.Collection<Integer>> priority = parseG128(
                "Multimap<String, Integer, java.util.PriorityQueue<Integer>>", "{\"k\": [3, 1, 2]}");
        assertInstanceOf(Multimap.class, priority);
        org.junit.jupiter.api.Assertions.assertFalse(priority instanceof ListMultimap);
        assertEquals(java.util.PriorityQueue.class, priority.get("k").getClass());
        assertEquals(1, ((Queue<Integer>) priority.get("k")).peek());
        priority.put("fresh", 5);
        assertEquals(java.util.PriorityQueue.class, priority.get("fresh").getClass());

        final Multimap<String, Integer, ? extends java.util.Collection<Integer>> custom = parseG128(
                "Multimap<String, " + ValuesG128.class.getCanonicalName() + "<Integer>>", "{\"k\": [2, 1]}");
        assertInstanceOf(ListMultimap.class, custom);
        assertEquals(ValuesG128.class, custom.get("k").getClass());
        custom.put("fresh", 3);
        assertEquals(ValuesG128.class, custom.get("fresh").getClass());
        assertEquals(List.of(2, 1), custom.get("k"));
    }

    public static class ValuesG128<E> extends ArrayList<E> {
        private static final long serialVersionUID = 1L;
    }

    // ---- bug review 2026-10-02 final G128 end ----
}
