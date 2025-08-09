package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.Supplier;

public class Suppliers100Test extends TestBase {

    @Test
    public void testOf() {
        // Test identity method
        Supplier<String> original = () -> "test";
        Supplier<String> result = Suppliers.of(original);
        Assertions.assertSame(original, result);
        Assertions.assertEquals("test", result.get());
    }

    @Test
    public void testOfWithFunction() {
        // Test supplier creation with function
        String input = "Hello";
        Supplier<String> supplier = Suppliers.of(input, s -> s.toUpperCase());
        Assertions.assertEquals("HELLO", supplier.get());
        Assertions.assertEquals("HELLO", supplier.get()); // Test multiple calls

        // Test with different types
        Supplier<Integer> lengthSupplier = Suppliers.of("test", String::length);
        Assertions.assertEquals(4, lengthSupplier.get());
    }

    @Test
    public void testOfInstance() {
        // Test with various instances
        String str = "constant";
        Supplier<String> strSupplier = Suppliers.ofInstance(str);
        Assertions.assertEquals("constant", strSupplier.get());
        Assertions.assertSame(str, strSupplier.get()); // Should return same instance

        // Test with null
        Supplier<Object> nullSupplier = Suppliers.ofInstance(null);
        Assertions.assertNull(nullSupplier.get());
    }

    @Test
    public void testOfUUID() {
        Supplier<String> uuidSupplier = Suppliers.ofUUID();
        String uuid1 = uuidSupplier.get();
        String uuid2 = uuidSupplier.get();

        Assertions.assertNotNull(uuid1);
        Assertions.assertNotNull(uuid2);
        Assertions.assertNotEquals(uuid1, uuid2); // Should generate different UUIDs
        Assertions.assertEquals(36, uuid1.length()); // Standard UUID length with dashes
    }

    @Test
    public void testOfGUID() {
        Supplier<String> guidSupplier = Suppliers.ofGUID();
        String guid1 = guidSupplier.get();
        String guid2 = guidSupplier.get();

        Assertions.assertNotNull(guid1);
        Assertions.assertNotNull(guid2);
        Assertions.assertNotEquals(guid1, guid2); // Should generate different GUIDs
    }

    @Test
    public void testOfEmptyArrays() {
        // Test all empty array suppliers
        Assertions.assertArrayEquals(new boolean[0], Suppliers.ofEmptyBooleanArray().get());
        Assertions.assertArrayEquals(new char[0], Suppliers.ofEmptyCharArray().get());
        Assertions.assertArrayEquals(new byte[0], Suppliers.ofEmptyByteArray().get());
        Assertions.assertArrayEquals(new short[0], Suppliers.ofEmptyShortArray().get());
        Assertions.assertArrayEquals(new int[0], Suppliers.ofEmptyIntArray().get());
        Assertions.assertArrayEquals(new long[0], Suppliers.ofEmptyLongArray().get());
        Assertions.assertArrayEquals(new float[0], Suppliers.ofEmptyFloatArray().get(), 0.0f);
        Assertions.assertArrayEquals(new double[0], Suppliers.ofEmptyDoubleArray().get(), 0.0);
        Assertions.assertArrayEquals(new String[0], Suppliers.ofEmptyStringArray().get());
        Assertions.assertArrayEquals(new Object[0], Suppliers.ofEmptyObjectArray().get());

        // Test that same instance is returned
        Assertions.assertSame(Suppliers.ofEmptyIntArray().get(), Suppliers.ofEmptyIntArray().get());
    }

    @Test
    public void testOfEmptyString() {
        Supplier<String> emptyStringSupplier = Suppliers.ofEmptyString();
        Assertions.assertEquals("", emptyStringSupplier.get());
        Assertions.assertSame(emptyStringSupplier.get(), emptyStringSupplier.get());
    }

    @Test
    public void testOfPrimitiveLists() {
        // Test all primitive list suppliers
        BooleanList boolList = Suppliers.ofBooleanList().get();
        Assertions.assertNotNull(boolList);
        Assertions.assertTrue(boolList.isEmpty());

        CharList charList = Suppliers.ofCharList().get();
        Assertions.assertNotNull(charList);
        Assertions.assertTrue(charList.isEmpty());

        ByteList byteList = Suppliers.ofByteList().get();
        Assertions.assertNotNull(byteList);
        Assertions.assertTrue(byteList.isEmpty());

        ShortList shortList = Suppliers.ofShortList().get();
        Assertions.assertNotNull(shortList);
        Assertions.assertTrue(shortList.isEmpty());

        IntList intList = Suppliers.ofIntList().get();
        Assertions.assertNotNull(intList);
        Assertions.assertTrue(intList.isEmpty());

        LongList longList = Suppliers.ofLongList().get();
        Assertions.assertNotNull(longList);
        Assertions.assertTrue(longList.isEmpty());

        FloatList floatList = Suppliers.ofFloatList().get();
        Assertions.assertNotNull(floatList);
        Assertions.assertTrue(floatList.isEmpty());

        DoubleList doubleList = Suppliers.ofDoubleList().get();
        Assertions.assertNotNull(doubleList);
        Assertions.assertTrue(doubleList.isEmpty());

        // Test that different instances are created
        Assertions.assertNotSame(Suppliers.ofIntList().get(), Suppliers.ofIntList().get());
    }

    @Test
    public void testOfCollections() {
        // Test List
        List<String> list = Suppliers.<String> ofList().get();
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list instanceof ArrayList);
        Assertions.assertTrue(list.isEmpty());

        // Test LinkedList
        LinkedList<Integer> linkedList = Suppliers.<Integer> ofLinkedList().get();
        Assertions.assertNotNull(linkedList);
        Assertions.assertTrue(linkedList.isEmpty());

        // Test Set
        Set<String> set = Suppliers.<String> ofSet().get();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set instanceof HashSet);
        Assertions.assertTrue(set.isEmpty());

        // Test LinkedHashSet
        Set<String> linkedHashSet = Suppliers.<String> ofLinkedHashSet().get();
        Assertions.assertNotNull(linkedHashSet);
        Assertions.assertTrue(linkedHashSet instanceof LinkedHashSet);

        // Test TreeSet
        TreeSet<String> treeSet = Suppliers.<String> ofTreeSet().get();
        Assertions.assertNotNull(treeSet);
        Assertions.assertTrue(treeSet.isEmpty());

        // Test SortedSet
        SortedSet<String> sortedSet = Suppliers.<String> ofSortedSet().get();
        Assertions.assertNotNull(sortedSet);
        Assertions.assertTrue(sortedSet instanceof TreeSet);

        // Test NavigableSet
        NavigableSet<String> navigableSet = Suppliers.<String> ofNavigableSet().get();
        Assertions.assertNotNull(navigableSet);
        Assertions.assertTrue(navigableSet instanceof TreeSet);
    }

    @Test
    public void testOfQueuesAndDeques() {
        // Test Queue
        Queue<String> queue = Suppliers.<String> ofQueue().get();
        Assertions.assertNotNull(queue);
        Assertions.assertTrue(queue instanceof LinkedList);
        Assertions.assertTrue(queue.isEmpty());

        // Test Deque
        Deque<String> deque = Suppliers.<String> ofDeque().get();
        Assertions.assertNotNull(deque);
        Assertions.assertTrue(deque instanceof LinkedList);

        // Test ArrayDeque
        ArrayDeque<String> arrayDeque = Suppliers.<String> ofArrayDeque().get();
        Assertions.assertNotNull(arrayDeque);
        Assertions.assertTrue(arrayDeque.isEmpty());

        // Test LinkedBlockingQueue
        LinkedBlockingQueue<String> lbq = Suppliers.<String> ofLinkedBlockingQueue().get();
        Assertions.assertNotNull(lbq);
        Assertions.assertTrue(lbq.isEmpty());

        // Test LinkedBlockingDeque
        LinkedBlockingDeque<String> lbd = Suppliers.<String> ofLinkedBlockingDeque().get();
        Assertions.assertNotNull(lbd);
        Assertions.assertTrue(lbd.isEmpty());

        // Test ConcurrentLinkedQueue
        ConcurrentLinkedQueue<String> clq = Suppliers.<String> ofConcurrentLinkedQueue().get();
        Assertions.assertNotNull(clq);
        Assertions.assertTrue(clq.isEmpty());

        // Test PriorityQueue
        PriorityQueue<String> pq = Suppliers.<String> ofPriorityQueue().get();
        Assertions.assertNotNull(pq);
        Assertions.assertTrue(pq.isEmpty());
    }

    @Test
    public void testOfMaps() {
        // Test Map
        Map<String, Object> map = Suppliers.<String, Object> ofMap().get();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof HashMap);
        Assertions.assertTrue(map.isEmpty());

        // Test LinkedHashMap
        Map<String, Object> linkedHashMap = Suppliers.<String, Object> ofLinkedHashMap().get();
        Assertions.assertNotNull(linkedHashMap);
        Assertions.assertTrue(linkedHashMap instanceof LinkedHashMap);

        // Test IdentityHashMap
        IdentityHashMap<String, Object> identityHashMap = Suppliers.<String, Object> ofIdentityHashMap().get();
        Assertions.assertNotNull(identityHashMap);
        Assertions.assertTrue(identityHashMap.isEmpty());

        // Test TreeMap
        TreeMap<String, Object> treeMap = Suppliers.<String, Object> ofTreeMap().get();
        Assertions.assertNotNull(treeMap);
        Assertions.assertTrue(treeMap.isEmpty());

        // Test SortedMap
        SortedMap<String, Object> sortedMap = Suppliers.<String, Object> ofSortedMap().get();
        Assertions.assertNotNull(sortedMap);
        Assertions.assertTrue(sortedMap instanceof TreeMap);

        // Test NavigableMap
        NavigableMap<String, Object> navigableMap = Suppliers.<String, Object> ofNavigableMap().get();
        Assertions.assertNotNull(navigableMap);
        Assertions.assertTrue(navigableMap instanceof TreeMap);

        // Test ConcurrentMap
        ConcurrentMap<String, Object> concurrentMap = Suppliers.<String, Object> ofConcurrentMap().get();
        Assertions.assertNotNull(concurrentMap);
        Assertions.assertTrue(concurrentMap instanceof ConcurrentHashMap);

        // Test ConcurrentHashMap
        ConcurrentHashMap<String, Object> concurrentHashMap = Suppliers.<String, Object> ofConcurrentHashMap().get();
        Assertions.assertNotNull(concurrentHashMap);
        Assertions.assertTrue(concurrentHashMap.isEmpty());
    }

    @Test
    public void testOfConcurrentHashSet() {
        Set<String> concurrentSet = Suppliers.<String> ofConcurrentHashSet().get();
        Assertions.assertNotNull(concurrentSet);
        Assertions.assertTrue(concurrentSet.isEmpty());

        // Test thread safety
        concurrentSet.add("test");
        Assertions.assertTrue(concurrentSet.contains("test"));
    }

    @Test
    public void testOfSpecialCollections() {
        // Test BiMap
        BiMap<String, Integer> biMap = Suppliers.<String, Integer> ofBiMap().get();
        Assertions.assertNotNull(biMap);
        Assertions.assertTrue(biMap.isEmpty());

        // Test Multiset
        Multiset<String> multiset = Suppliers.<String> ofMultiset().get();
        Assertions.assertNotNull(multiset);
        Assertions.assertTrue(multiset.isEmpty());

        // Test Multiset with map type
        Multiset<String> linkedMultiset = Suppliers.<String> ofMultiset(LinkedHashMap.class).get();
        Assertions.assertNotNull(linkedMultiset);
        Assertions.assertTrue(linkedMultiset.isEmpty());

        // Test Multiset with map supplier
        Multiset<String> customMultiset = Suppliers.<String> ofMultiset(TreeMap::new).get();
        Assertions.assertNotNull(customMultiset);
        Assertions.assertTrue(customMultiset.isEmpty());
    }

    @Test
    public void testOfMultimaps() {
        // Test ListMultimap
        ListMultimap<String, Integer> listMultimap = Suppliers.<String, Integer>ofListMultimap().get();
        Assertions.assertNotNull(listMultimap);
        Assertions.assertTrue(listMultimap.isEmpty());

        // Test SetMultimap
        SetMultimap<String, Integer> setMultimap = Suppliers.<String, Integer>ofSetMultimap().get();
        Assertions.assertNotNull(setMultimap);
        Assertions.assertTrue(setMultimap.isEmpty());

        // Test with custom types
        ListMultimap<String, Integer> customListMultimap = Suppliers.<String, Integer> ofListMultimap(TreeMap.class).get();
        Assertions.assertNotNull(customListMultimap);

        SetMultimap<String, Integer> customSetMultimap = Suppliers.<String, Integer>ofSetMultimap(LinkedHashMap.class).get();
        Assertions.assertNotNull(customSetMultimap);

        // Test with map and value types
        ListMultimap<String, Integer> typedListMultimap = Suppliers.<String, Integer>ofListMultimap(HashMap.class, ArrayList.class).get();
        Assertions.assertNotNull(typedListMultimap);

        SetMultimap<String, Integer> typedSetMultimap = Suppliers.<String, Integer>ofSetMultimap(TreeMap.class, TreeSet.class).get();
        Assertions.assertNotNull(typedSetMultimap);

        // Test with suppliers
        ListMultimap<String, Integer> supplierListMultimap = Suppliers.<String, Integer> ofListMultimap(HashMap::new, ArrayList::new).get();
        Assertions.assertNotNull(supplierListMultimap);

        SetMultimap<String, Integer> supplierSetMultimap = Suppliers.<String, Integer> ofSetMultimap(LinkedHashMap::new, HashSet::new).get();
        Assertions.assertNotNull(supplierSetMultimap);

        // Test general multimap
        Multimap<String, Integer, List<Integer>> generalMultimap = Suppliers.<String, Integer, List<Integer>> ofMultimap(TreeMap::new, ArrayList::new).get();
        Assertions.assertNotNull(generalMultimap);
    }

    @Test
    public void testOfStringBuilder() {
        StringBuilder sb = Suppliers.ofStringBuilder().get();
        Assertions.assertNotNull(sb);
        Assertions.assertEquals(0, sb.length());

        // Test that different instances are created
        StringBuilder sb2 = Suppliers.ofStringBuilder().get();
        Assertions.assertNotSame(sb, sb2);
    }

    @Test
    public void testOfCollection() {
        // Test with standard collection types
        Supplier<? extends Collection<String>> listSupplier = Suppliers.ofCollection(ArrayList.class);
        Collection<String> list = listSupplier.get();
        Assertions.assertTrue(list instanceof ArrayList);

        Supplier<? extends Collection<String>> setSupplier = Suppliers.ofCollection(HashSet.class);
        Collection<String> set = setSupplier.get();
        Assertions.assertTrue(set instanceof HashSet);

        // Test with LinkedList
        Supplier<? extends Collection<String>> linkedListSupplier = Suppliers.ofCollection(LinkedList.class);
        Collection<String> linkedList = linkedListSupplier.get();
        Assertions.assertTrue(linkedList instanceof LinkedList);

        // Test with TreeSet
        Supplier<? extends Collection<String>> treeSetSupplier = Suppliers.ofCollection(TreeSet.class);
        Collection<String> treeSet = treeSetSupplier.get();
        Assertions.assertTrue(treeSet instanceof TreeSet);

        // Test with interface types
        Supplier<? extends Collection<String>> collectionSupplier = Suppliers.ofCollection(Collection.class);
        Collection<String> collection = collectionSupplier.get();
        Assertions.assertTrue(collection instanceof ArrayList); // Default to ArrayList

        Supplier<? extends Collection<String>> listInterfaceSupplier = Suppliers.ofCollection(List.class);
        Collection<String> listInterface = listInterfaceSupplier.get();
        Assertions.assertTrue(listInterface instanceof ArrayList);

        Supplier<? extends Collection<String>> setInterfaceSupplier = Suppliers.ofCollection(Set.class);
        Collection<String> setInterface = setInterfaceSupplier.get();
        Assertions.assertTrue(setInterface instanceof HashSet);
    }

    @Test
    public void testOfMap() {
        // Test with standard map types
        Supplier<? extends Map<String, Object>> mapSupplier = Suppliers.ofMap(HashMap.class);
        Map<String, Object> map = mapSupplier.get();
        Assertions.assertTrue(map instanceof HashMap);

        Supplier<? extends Map<String, Object>> treeMapSupplier = Suppliers.ofMap(TreeMap.class);
        Map<String, Object> treeMap = treeMapSupplier.get();
        Assertions.assertTrue(treeMap instanceof TreeMap);

        // Test with LinkedHashMap
        Supplier<? extends Map<String, Object>> linkedMapSupplier = Suppliers.ofMap(LinkedHashMap.class);
        Map<String, Object> linkedMap = linkedMapSupplier.get();
        Assertions.assertTrue(linkedMap instanceof LinkedHashMap);

        // Test with ConcurrentHashMap
        Supplier<? extends Map<String, Object>> concurrentMapSupplier = Suppliers.ofMap(ConcurrentHashMap.class);
        Map<String, Object> concurrentMap = concurrentMapSupplier.get();
        Assertions.assertTrue(concurrentMap instanceof ConcurrentHashMap);

        // Test with interface types
        Supplier<? extends Map<String, Object>> mapInterfaceSupplier = Suppliers.ofMap(Map.class);
        Map<String, Object> mapInterface = mapInterfaceSupplier.get();
        Assertions.assertTrue(mapInterface instanceof HashMap); // Default to HashMap

        Supplier<? extends Map<String, Object>> sortedMapSupplier = Suppliers.ofMap(SortedMap.class);
        Map<String, Object> sortedMap = sortedMapSupplier.get();
        Assertions.assertTrue(sortedMap instanceof TreeMap);
    }

    //    @Test
    //    public void testRegisterForCollection() {
    //        // Create custom collection class
    //        class CustomCollection<E> extends ArrayList<E> {
    //            public boolean isCustom() {
    //                return true;
    //            }
    //        }
    //
    //        // Test registration
    //        boolean registered = Suppliers.registerForCollection(CustomCollection.class, () -> new CustomCollection<>());
    //        Assertions.assertTrue(registered);
    //
    //        // Test that registration works
    //        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(CustomCollection.class);
    //        Collection<String> collection = supplier.get();
    //        Assertions.assertTrue(collection instanceof CustomCollection);
    //        Assertions.assertTrue(((CustomCollection<String>) collection).isCustom());
    //
    //        // Test duplicate registration fails
    //        boolean duplicateRegistered = Suppliers.registerForCollection(CustomCollection.class, () -> new CustomCollection<>());
    //        Assertions.assertFalse(duplicateRegistered);
    //
    //        // Test registration with built-in class fails
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Suppliers.registerForCollection(ArrayList.class, ArrayList::new);
    //        });
    //    }

    //    @Test
    //    public void testRegisterForMap() {
    //        // Create custom map class
    //        class CustomMap<K, V> extends HashMap<K, V> {
    //            public boolean isCustom() {
    //                return true;
    //            }
    //        }
    //
    //        // Test registration
    //        boolean registered = Suppliers.registerForMap(CustomMap.class, () -> new CustomMap<>());
    //        Assertions.assertTrue(registered);
    //
    //        // Test that registration works
    //        Supplier<? extends Map<String, Object>> supplier = Suppliers.ofMap(CustomMap.class);
    //        Map<String, Object> map = supplier.get();
    //        Assertions.assertTrue(map instanceof CustomMap);
    //        Assertions.assertTrue(((CustomMap<String, Object>) map).isCustom());
    //
    //        // Test duplicate registration fails
    //        boolean duplicateRegistered = Suppliers.registerForMap(CustomMap.class, () -> new CustomMap<>());
    //        Assertions.assertFalse(duplicateRegistered);
    //
    //        // Test registration with built-in class fails
    //        Assertions.assertThrows(IllegalArgumentException.class, () -> {
    //            Suppliers.registerForMap(HashMap.class, HashMap::new);
    //        });
    //    }

    @Test
    public void testUnsupportedImmutableOperations() {
        // Test that immutable operations throw UnsupportedOperationException
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableList();
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableSet();
        });

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableMap();
        });
    }

    @Test
    public void testNewException() {
        Supplier<Exception> exceptionSupplier = Suppliers.newException();
        Exception ex1 = exceptionSupplier.get();
        Exception ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2); // Different instances
    }

    @Test
    public void testNewRuntimeException() {
        Supplier<RuntimeException> exceptionSupplier = Suppliers.newRuntimeException();
        RuntimeException ex1 = exceptionSupplier.get();
        RuntimeException ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2); // Different instances
    }

    @Test
    public void testNewNoSuchElementException() {
        Supplier<NoSuchElementException> exceptionSupplier = Suppliers.newNoSuchElementException();
        NoSuchElementException ex1 = exceptionSupplier.get();
        NoSuchElementException ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2); // Different instances
    }

    @Test
    public void testInvalidCollectionType() {
        //    // Test with abstract class
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> {
        //        Suppliers.ofCollection(AbstractList.class);
        //    });

        // Test with non-collection class
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofCollection((Class) String.class);
        });
    }

    @Test
    public void testInvalidMapType() {
        //    // Test with abstract class
        //    Assertions.assertThrows(IllegalArgumentException.class, () -> {
        //        Suppliers.ofMap(AbstractMap.class);
        //    });

        // Test with non-map class
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofMap((Class) String.class);
        });
    }
}