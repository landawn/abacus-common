package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MultisetCreationTest extends MultisetTestSupport {

    @Test
    public void testC344_classConstructorRejectsBiMap() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(BiMap.class));
        assertTrue(e.getMessage().contains("valueMapType"), e.getMessage());
        assertTrue(e.getMessage().contains("BiMap"), e.getMessage());
    }

    @Test
    @DisplayName("Test constructor with initial capacity")
    public void testConstructorWithCapacity() {
        Multiset<String> ms = new Multiset<>(100);
        assertNotNull(ms);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> list = Arrays.asList("a", "b", "a", "c", "b", "a");
        Multiset<String> ms = new Multiset<>(list);
        assertEquals(6, ms.size());
        assertEquals(3, ms.getCount("a"));
        assertEquals(2, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));
    }

    @Test
    public void testConstructorWithCollection_Empty() {
        Multiset<String> ms = new Multiset<>(new ArrayList<>());
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithCollection_Null() {
        Multiset<String> ms = new Multiset<>((Collection<String>) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithEmptyCollection() {
        Collection<String> initialElements = Collections.emptyList();
        Multiset<String> multiset = new Multiset<>(initialElements);
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        Multiset<String> ms = new Multiset<>(100);
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());
    }

    @Test
    public void testConstructorWithInitialCapacity_Negative() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>(-1));
    }

    @Test
    public void testConstructorWithIterable_NonCollection() {
        Iterable<String> iterable = () -> Arrays.asList("a", "b", "a", "c").iterator();

        Multiset<String> result = new Multiset<>(iterable);

        assertEquals(2, result.getCount("a"));
        assertEquals(1, result.getCount("b"));
        assertEquals(1, result.getCount("c"));
    }

    @Test
    public void testConstructorWithMapClass() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithMapSupplier() {
        Multiset<String> ms = new Multiset<>(() -> new TreeMap<>());
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testConstructorWithMapSupplier_null() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Supplier<Map<String, ?>>) null));
    }

    @Test
    public void testConstructorWithMapSupplierReturningNonEmptyMap() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(() -> Map.of("a", 1)));
    }

    @Test
    public void testConstructorWithMapSupplierReturningNull() {
        assertThrows(NullPointerException.class, () -> new Multiset<String>((Supplier<Map<String, ?>>) () -> null));
    }

    @Test
    public void testConstructorWithMapType() {
        Multiset<String> ms = new Multiset<>(LinkedHashMap.class);
        assertTrue(ms.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Class<? extends Map>) null));
    }

    @Test
    public void testConstructorWithNullCollection() {
        Multiset<String> multiset = new Multiset<>((Collection<String>) null);
        assertTrue(multiset.isEmpty());
        assertEquals(0, multiset.size());
    }

    @Test
    public void testConstructorWithSetCollection() {
        Set<String> initialElements = CommonUtil.toSet("a", "b", "c");
        Multiset<String> multiset = new Multiset<>(initialElements);
        assertEquals(3, multiset.size());
        assertEquals(1, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testConstructorWithValueMapType() {
        Multiset<String> multiset = new Multiset<>(LinkedHashMap.class);
        multiset.add("c");
        multiset.add("a");
        multiset.add("b");
        List<String> elements = new ArrayList<>(multiset.elementSet());
        assertEquals(Arrays.asList("c", "a", "b"), elements);
        assertTrue(multiset.toString().startsWith("{c=1, a=1, b=1}"));
    }

    @Test
    public void testConstructorWithValueMapType_null() {
        assertThrows(IllegalArgumentException.class, () -> new Multiset<>((Class<? extends Map>) null));
    }

    // create

    @Test
    public void testCreate() {
        List<String> list = Arrays.asList("x", "y", "x", "z");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
    }

    @Test
    public void testCreateFromCollection() {
        List<String> list = Arrays.asList("x", "y", "x", "z");
        Multiset<String> ms = Multiset.create(list);
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("x"));
        assertEquals(1, ms.getCount("y"));
        assertEquals(1, ms.getCount("z"));
    }

    @Test
    public void testCreateFromIterator() {
        Iterator<String> iter = Arrays.asList("a", "b", "a").iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(3, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
    }

    @Test
    public void testCreateFromIterator_MaxValue() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            list.add("a");
            if (i > 100) {
                break;
            }
        }
        Iterator<String> iter = list.iterator();
        Multiset<String> ms = Multiset.create(iter);
        assertEquals(list.size(), ms.getCount("a"));
    }

    @Test
    public void testCreateFromIterator_Null() {
        Multiset<String> ms = Multiset.create((Iterator<String>) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testCreateWithCollection() {
        Collection<String> initialElements = Arrays.asList("x", "y", "x");
        Multiset<String> multiset = Multiset.create(initialElements);
        assertEquals(3, multiset.size());
        assertEquals(2, multiset.getCount("x"));
        assertEquals(1, multiset.getCount("y"));
    }

    @Test
    public void testCreateWithEmptyIterator() {
        Iterator<String> iterator = Collections.emptyIterator();
        Multiset<String> multiset = Multiset.create(iterator);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testCreateWithIterator() {
        Iterator<String> iterator = Arrays.asList("m", "n", "m", "o", "m").iterator();
        Multiset<String> multiset = Multiset.create(iterator);
        assertEquals(5, multiset.size());
        assertEquals(3, multiset.getCount("m"));
        assertEquals(1, multiset.getCount("n"));
        assertEquals(1, multiset.getCount("o"));
    }

    @Test
    public void testCreateWithNullCollection() {
        Multiset<String> multiset = Multiset.create((Collection<String>) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testCreateWithNullIterator() {
        Multiset<String> multiset = Multiset.create((Iterator<String>) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testDefaultConstructor() {
        Multiset<String> ms = new Multiset<>();
        assertTrue(ms.isEmpty());
        assertEquals(0, ms.size());
    }

    @Test
    public void testC349_negativeInitialCapacityNamesTheParameter() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(-1));
        assertTrue(e.getMessage().contains("initialCapacity"), e.getMessage());

        e = assertThrows(IllegalArgumentException.class, () -> new Multiset<String>(Integer.MIN_VALUE));
        assertTrue(e.getMessage().contains("initialCapacity"), e.getMessage());
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

    // of

    @Test
    public void testOf() {
        Multiset<String> ms = Multiset.of("a", "b", "a", "c");
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));
    }

    @Test
    public void testOf_Empty() {
        Multiset<String> ms = Multiset.of();
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testOf_Null() {
        Multiset<String> ms = Multiset.of((String[]) null);
        assertTrue(ms.isEmpty());
    }

    @Test
    public void testOfVarArgs() {
        Multiset<String> multiset = Multiset.of("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.getCount("c"));
    }

    @Test
    public void testOfVarargs() {
        Multiset<String> ms = Multiset.of("a", "b", "a", "c");
        assertEquals(4, ms.size());
        assertEquals(2, ms.getCount("a"));
        assertEquals(1, ms.getCount("b"));
        assertEquals(1, ms.getCount("c"));

        Multiset<String> empty = Multiset.of();
        assertTrue(empty.isEmpty());

        String[] nullArray = null;
        Multiset<String> nullMs = Multiset.of(nullArray);
        assertTrue(nullMs.isEmpty());
    }

    @Test
    public void testOfVarArgsEmpty() {
        Multiset<String> multiset = Multiset.of();
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testOfVarArgsNullArray() {
        Multiset<String> multiset = Multiset.of((String[]) null);
        assertTrue(multiset.isEmpty());
    }

    @Test
    public void testRemoveAllOccurrences_Object() {
        Multiset<String> multiset = Multiset.of("a", "a", "a", "b");
        assertEquals(3, multiset.removeAllOccurrencesOf("a"));
        assertEquals(0, multiset.getCount("a"));
        assertEquals(1, multiset.getCount("b"));
        assertEquals(1, multiset.size());

        assertEquals(0, multiset.removeAllOccurrencesOf("c"));
        assertEquals(1, multiset.getCount("b"));
    }

    @Test
    public void testRemoveAllOccurrencesSelfClearsWithoutConcurrentModification() {
        Multiset<String> set = Multiset.of("a", "a", "b");

        assertTrue(set.removeAllOccurrencesOf(set));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testC344_supplierConstructorRejectsBiMap() {
        final Supplier<Map<String, Object>> supplier = BiMap::new;
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new Multiset<>(supplier));
        assertTrue(e.getMessage().contains("mapSupplier"), e.getMessage());
        assertTrue(e.getMessage().contains("BiMap"), e.getMessage());
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
