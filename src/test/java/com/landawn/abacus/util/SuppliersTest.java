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
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class SuppliersTest extends TestBase {

    // --- of(Supplier) ---

    @Test
    public void testOfSupplier() {
        Supplier<String> original = () -> "hello";
        Supplier<String> result = Suppliers.of(original);
        Assertions.assertSame(original, result);
        Assertions.assertEquals("hello", result.get());
    }

    // --- of(A, Function) ---

    @Test
    public void testOfWithFunction() {
        String input = "Hello";
        Supplier<String> supplier = Suppliers.of(input, s -> s.toUpperCase());
        Assertions.assertEquals("HELLO", supplier.get());
        Assertions.assertEquals("HELLO", supplier.get());

        Supplier<Integer> lengthSupplier = Suppliers.of("test", String::length);
        Assertions.assertEquals(4, lengthSupplier.get());
    }

    @Test
    public void testOfWithFunction_NullInput() {
        Supplier<String> supplier = Suppliers.of(null, s -> "result");
        Assertions.assertEquals("result", supplier.get());
    }

    // --- ofInstance ---

    @Test
    public void testOfInstance() {
        String str = "constant";
        Supplier<String> strSupplier = Suppliers.ofInstance(str);
        Assertions.assertEquals("constant", strSupplier.get());
        Assertions.assertSame(str, strSupplier.get());

        Supplier<Object> nullSupplier = Suppliers.ofInstance(null);
        Assertions.assertNull(nullSupplier.get());
    }

    @Test
    public void testOfInstance_ReturnsSameInstance() {
        Object obj = new Object();
        Supplier<Object> supplier = Suppliers.ofInstance(obj);
        Assertions.assertSame(obj, supplier.get());
        Assertions.assertSame(supplier.get(), supplier.get());
    }

    // --- ofUuid ---

    @Test
    public void testOfUUID() {
        Supplier<String> uuidSupplier = Suppliers.ofUuid();
        String uuid1 = uuidSupplier.get();
        String uuid2 = uuidSupplier.get();

        Assertions.assertNotNull(uuid1);
        Assertions.assertNotNull(uuid2);
        Assertions.assertNotEquals(uuid1, uuid2);
        Assertions.assertEquals(36, uuid1.length());
    }

    // --- ofUuidWithoutHyphens ---

    @Test
    public void testOfGUID() {
        Supplier<String> guidSupplier = Suppliers.ofUuidWithoutHyphens();
        String guid1 = guidSupplier.get();
        String guid2 = guidSupplier.get();

        Assertions.assertNotNull(guid1);
        Assertions.assertNotNull(guid2);
        Assertions.assertNotEquals(guid1, guid2);
    }

    @Test
    public void testOfUuidWithoutHyphens_NoHyphens() {
        Supplier<String> guidSupplier = Suppliers.ofUuidWithoutHyphens();
        String guid = guidSupplier.get();
        Assertions.assertFalse(guid.contains("-"));
        Assertions.assertEquals(32, guid.length());
    }

    // --- ofEmptyBooleanArray through ofEmptyObjectArray ---

    @Test
    public void testOfEmptyBooleanArray() {
        Assertions.assertArrayEquals(new boolean[0], Suppliers.ofEmptyBooleanArray().get());
        Assertions.assertSame(Suppliers.ofEmptyBooleanArray().get(), Suppliers.ofEmptyBooleanArray().get());
    }

    @Test
    public void testOfEmptyCharArray() {
        Assertions.assertArrayEquals(new char[0], Suppliers.ofEmptyCharArray().get());
        Assertions.assertSame(Suppliers.ofEmptyCharArray().get(), Suppliers.ofEmptyCharArray().get());
    }

    @Test
    public void testOfEmptyByteArray() {
        Assertions.assertArrayEquals(new byte[0], Suppliers.ofEmptyByteArray().get());
        Assertions.assertSame(Suppliers.ofEmptyByteArray().get(), Suppliers.ofEmptyByteArray().get());
    }

    @Test
    public void testOfEmptyShortArray() {
        Assertions.assertArrayEquals(new short[0], Suppliers.ofEmptyShortArray().get());
        Assertions.assertSame(Suppliers.ofEmptyShortArray().get(), Suppliers.ofEmptyShortArray().get());
    }

    @Test
    public void testOfEmptyIntArray() {
        Assertions.assertArrayEquals(new int[0], Suppliers.ofEmptyIntArray().get());
        Assertions.assertSame(Suppliers.ofEmptyIntArray().get(), Suppliers.ofEmptyIntArray().get());
    }

    @Test
    public void testOfEmptyLongArray() {
        Assertions.assertArrayEquals(new long[0], Suppliers.ofEmptyLongArray().get());
        Assertions.assertSame(Suppliers.ofEmptyLongArray().get(), Suppliers.ofEmptyLongArray().get());
    }

    @Test
    public void testOfEmptyFloatArray() {
        Assertions.assertArrayEquals(new float[0], Suppliers.ofEmptyFloatArray().get(), 0.0f);
        Assertions.assertSame(Suppliers.ofEmptyFloatArray().get(), Suppliers.ofEmptyFloatArray().get());
    }

    @Test
    public void testOfEmptyDoubleArray() {
        Assertions.assertArrayEquals(new double[0], Suppliers.ofEmptyDoubleArray().get(), 0.0);
        Assertions.assertSame(Suppliers.ofEmptyDoubleArray().get(), Suppliers.ofEmptyDoubleArray().get());
    }

    @Test
    public void testOfEmptyStringArray() {
        Assertions.assertArrayEquals(new String[0], Suppliers.ofEmptyStringArray().get());
        Assertions.assertSame(Suppliers.ofEmptyStringArray().get(), Suppliers.ofEmptyStringArray().get());
    }

    @Test
    public void testOfEmptyObjectArray() {
        Assertions.assertArrayEquals(new Object[0], Suppliers.ofEmptyObjectArray().get());
        Assertions.assertSame(Suppliers.ofEmptyObjectArray().get(), Suppliers.ofEmptyObjectArray().get());
    }

    @Test
    public void testOfEmptyArrays() {
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

        Assertions.assertSame(Suppliers.ofEmptyIntArray().get(), Suppliers.ofEmptyIntArray().get());
    }

    // --- ofEmptyString ---

    @Test
    public void testOfEmptyString() {
        Supplier<String> emptyStringSupplier = Suppliers.ofEmptyString();
        Assertions.assertEquals("", emptyStringSupplier.get());
        Assertions.assertSame(emptyStringSupplier.get(), emptyStringSupplier.get());
    }

    // --- ofBooleanList through ofDoubleList ---

    @Test
    public void testOfBooleanList() {
        BooleanList boolList = Suppliers.ofBooleanList().get();
        Assertions.assertNotNull(boolList);
        Assertions.assertTrue(boolList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofBooleanList().get(), Suppliers.ofBooleanList().get());
    }

    @Test
    public void testOfCharList() {
        CharList charList = Suppliers.ofCharList().get();
        Assertions.assertNotNull(charList);
        Assertions.assertTrue(charList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofCharList().get(), Suppliers.ofCharList().get());
    }

    @Test
    public void testOfByteList() {
        ByteList byteList = Suppliers.ofByteList().get();
        Assertions.assertNotNull(byteList);
        Assertions.assertTrue(byteList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofByteList().get(), Suppliers.ofByteList().get());
    }

    @Test
    public void testOfShortList() {
        ShortList shortList = Suppliers.ofShortList().get();
        Assertions.assertNotNull(shortList);
        Assertions.assertTrue(shortList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofShortList().get(), Suppliers.ofShortList().get());
    }

    @Test
    public void testOfIntList() {
        IntList intList = Suppliers.ofIntList().get();
        Assertions.assertNotNull(intList);
        Assertions.assertTrue(intList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofIntList().get(), Suppliers.ofIntList().get());
    }

    @Test
    public void testOfLongList() {
        LongList longList = Suppliers.ofLongList().get();
        Assertions.assertNotNull(longList);
        Assertions.assertTrue(longList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofLongList().get(), Suppliers.ofLongList().get());
    }

    @Test
    public void testOfFloatList() {
        FloatList floatList = Suppliers.ofFloatList().get();
        Assertions.assertNotNull(floatList);
        Assertions.assertTrue(floatList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofFloatList().get(), Suppliers.ofFloatList().get());
    }

    @Test
    public void testOfDoubleList() {
        DoubleList doubleList = Suppliers.ofDoubleList().get();
        Assertions.assertNotNull(doubleList);
        Assertions.assertTrue(doubleList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofDoubleList().get(), Suppliers.ofDoubleList().get());
    }

    @Test
    public void testOfPrimitiveLists() {
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

        Assertions.assertNotSame(Suppliers.ofIntList().get(), Suppliers.ofIntList().get());
    }

    // --- ofList ---

    @Test
    public void testOfList() {
        List<String> list = Suppliers.<String> ofList().get();
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list instanceof ArrayList);
        Assertions.assertTrue(list.isEmpty());
        Assertions.assertNotSame(Suppliers.ofList().get(), Suppliers.ofList().get());
    }

    // --- ofLinkedList ---

    @Test
    public void testOfLinkedList() {
        LinkedList<Integer> linkedList = Suppliers.<Integer> ofLinkedList().get();
        Assertions.assertNotNull(linkedList);
        Assertions.assertTrue(linkedList.isEmpty());
        Assertions.assertNotSame(Suppliers.ofLinkedList().get(), Suppliers.ofLinkedList().get());
    }

    // --- ofSet ---

    @Test
    public void testOfSet() {
        Set<String> set = Suppliers.<String> ofSet().get();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set instanceof HashSet);
        Assertions.assertTrue(set.isEmpty());
        Assertions.assertNotSame(Suppliers.ofSet().get(), Suppliers.ofSet().get());
    }

    // --- ofLinkedHashSet ---

    @Test
    public void testOfLinkedHashSet() {
        Set<String> linkedHashSet = Suppliers.<String> ofLinkedHashSet().get();
        Assertions.assertNotNull(linkedHashSet);
        Assertions.assertTrue(linkedHashSet instanceof LinkedHashSet);
        Assertions.assertTrue(linkedHashSet.isEmpty());
    }

    // --- ofSortedSet ---

    @Test
    public void testOfSortedSet() {
        SortedSet<String> sortedSet = Suppliers.<String> ofSortedSet().get();
        Assertions.assertNotNull(sortedSet);
        Assertions.assertTrue(sortedSet instanceof TreeSet);
        Assertions.assertTrue(sortedSet.isEmpty());
    }

    // --- ofNavigableSet ---

    @Test
    public void testOfNavigableSet() {
        NavigableSet<String> navigableSet = Suppliers.<String> ofNavigableSet().get();
        Assertions.assertNotNull(navigableSet);
        Assertions.assertTrue(navigableSet instanceof TreeSet);
        Assertions.assertTrue(navigableSet.isEmpty());
    }

    // --- ofTreeSet ---

    @Test
    public void testOfTreeSet() {
        TreeSet<String> treeSet = Suppliers.<String> ofTreeSet().get();
        Assertions.assertNotNull(treeSet);
        Assertions.assertTrue(treeSet.isEmpty());
    }

    // --- ofQueue ---

    @Test
    public void testOfQueue() {
        Queue<String> queue = Suppliers.<String> ofQueue().get();
        Assertions.assertNotNull(queue);
        Assertions.assertTrue(queue instanceof LinkedList);
        Assertions.assertTrue(queue.isEmpty());
    }

    // --- ofDeque ---

    @Test
    public void testOfDeque() {
        Deque<String> deque = Suppliers.<String> ofDeque().get();
        Assertions.assertNotNull(deque);
        Assertions.assertTrue(deque instanceof LinkedList);
        Assertions.assertTrue(deque.isEmpty());
    }

    // --- ofArrayDeque ---

    @Test
    public void testOfArrayDeque() {
        ArrayDeque<String> arrayDeque = Suppliers.<String> ofArrayDeque().get();
        Assertions.assertNotNull(arrayDeque);
        Assertions.assertTrue(arrayDeque.isEmpty());
    }

    // --- ofLinkedBlockingQueue ---

    @Test
    public void testOfLinkedBlockingQueue() {
        LinkedBlockingQueue<String> lbq = Suppliers.<String> ofLinkedBlockingQueue().get();
        Assertions.assertNotNull(lbq);
        Assertions.assertTrue(lbq.isEmpty());
    }

    // --- ofLinkedBlockingDeque ---

    @Test
    public void testOfLinkedBlockingDeque() {
        LinkedBlockingDeque<String> lbd = Suppliers.<String> ofLinkedBlockingDeque().get();
        Assertions.assertNotNull(lbd);
        Assertions.assertTrue(lbd.isEmpty());
    }

    // --- ofConcurrentLinkedQueue ---

    @Test
    public void testOfConcurrentLinkedQueue() {
        ConcurrentLinkedQueue<String> clq = Suppliers.<String> ofConcurrentLinkedQueue().get();
        Assertions.assertNotNull(clq);
        Assertions.assertTrue(clq.isEmpty());
    }

    // --- ofPriorityQueue ---

    @Test
    public void testOfPriorityQueue() {
        PriorityQueue<String> pq = Suppliers.<String> ofPriorityQueue().get();
        Assertions.assertNotNull(pq);
        Assertions.assertTrue(pq.isEmpty());
    }

    @Test
    public void testOfCollections() {
        List<String> list = Suppliers.<String> ofList().get();
        Assertions.assertNotNull(list);
        Assertions.assertTrue(list instanceof ArrayList);
        Assertions.assertTrue(list.isEmpty());

        LinkedList<Integer> linkedList = Suppliers.<Integer> ofLinkedList().get();
        Assertions.assertNotNull(linkedList);
        Assertions.assertTrue(linkedList.isEmpty());

        Set<String> set = Suppliers.<String> ofSet().get();
        Assertions.assertNotNull(set);
        Assertions.assertTrue(set instanceof HashSet);
        Assertions.assertTrue(set.isEmpty());

        Set<String> linkedHashSet = Suppliers.<String> ofLinkedHashSet().get();
        Assertions.assertNotNull(linkedHashSet);
        Assertions.assertTrue(linkedHashSet instanceof LinkedHashSet);

        TreeSet<String> treeSet = Suppliers.<String> ofTreeSet().get();
        Assertions.assertNotNull(treeSet);
        Assertions.assertTrue(treeSet.isEmpty());

        SortedSet<String> sortedSet = Suppliers.<String> ofSortedSet().get();
        Assertions.assertNotNull(sortedSet);
        Assertions.assertTrue(sortedSet instanceof TreeSet);

        NavigableSet<String> navigableSet = Suppliers.<String> ofNavigableSet().get();
        Assertions.assertNotNull(navigableSet);
        Assertions.assertTrue(navigableSet instanceof TreeSet);
    }

    @Test
    public void testOfQueuesAndDeques() {
        Queue<String> queue = Suppliers.<String> ofQueue().get();
        Assertions.assertNotNull(queue);
        Assertions.assertTrue(queue instanceof LinkedList);
        Assertions.assertTrue(queue.isEmpty());

        Deque<String> deque = Suppliers.<String> ofDeque().get();
        Assertions.assertNotNull(deque);
        Assertions.assertTrue(deque instanceof LinkedList);

        ArrayDeque<String> arrayDeque = Suppliers.<String> ofArrayDeque().get();
        Assertions.assertNotNull(arrayDeque);
        Assertions.assertTrue(arrayDeque.isEmpty());

        LinkedBlockingQueue<String> lbq = Suppliers.<String> ofLinkedBlockingQueue().get();
        Assertions.assertNotNull(lbq);
        Assertions.assertTrue(lbq.isEmpty());

        LinkedBlockingDeque<String> lbd = Suppliers.<String> ofLinkedBlockingDeque().get();
        Assertions.assertNotNull(lbd);
        Assertions.assertTrue(lbd.isEmpty());

        ConcurrentLinkedQueue<String> clq = Suppliers.<String> ofConcurrentLinkedQueue().get();
        Assertions.assertNotNull(clq);
        Assertions.assertTrue(clq.isEmpty());

        PriorityQueue<String> pq = Suppliers.<String> ofPriorityQueue().get();
        Assertions.assertNotNull(pq);
        Assertions.assertTrue(pq.isEmpty());
    }

    // --- ofMap ---

    @Test
    public void testOfMap_Default() {
        Map<String, Object> map = Suppliers.<String, Object> ofMap().get();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof HashMap);
        Assertions.assertTrue(map.isEmpty());
    }

    // --- ofLinkedHashMap ---

    @Test
    public void testOfLinkedHashMap() {
        Map<String, Object> linkedHashMap = Suppliers.<String, Object> ofLinkedHashMap().get();
        Assertions.assertNotNull(linkedHashMap);
        Assertions.assertTrue(linkedHashMap instanceof LinkedHashMap);
        Assertions.assertTrue(linkedHashMap.isEmpty());
    }

    // --- ofIdentityHashMap ---

    @Test
    public void testOfIdentityHashMap() {
        IdentityHashMap<String, Object> identityHashMap = Suppliers.<String, Object> ofIdentityHashMap().get();
        Assertions.assertNotNull(identityHashMap);
        Assertions.assertTrue(identityHashMap.isEmpty());
    }

    // --- ofSortedMap ---

    @Test
    public void testOfSortedMap() {
        SortedMap<String, Object> sortedMap = Suppliers.<String, Object> ofSortedMap().get();
        Assertions.assertNotNull(sortedMap);
        Assertions.assertTrue(sortedMap instanceof TreeMap);
        Assertions.assertTrue(sortedMap.isEmpty());
    }

    // --- ofNavigableMap ---

    @Test
    public void testOfNavigableMap() {
        NavigableMap<String, Object> navigableMap = Suppliers.<String, Object> ofNavigableMap().get();
        Assertions.assertNotNull(navigableMap);
        Assertions.assertTrue(navigableMap instanceof TreeMap);
        Assertions.assertTrue(navigableMap.isEmpty());
    }

    // --- ofTreeMap ---

    @Test
    public void testOfTreeMap() {
        TreeMap<String, Object> treeMap = Suppliers.<String, Object> ofTreeMap().get();
        Assertions.assertNotNull(treeMap);
        Assertions.assertTrue(treeMap.isEmpty());
    }

    // --- ofConcurrentMap ---

    @Test
    public void testOfConcurrentMap() {
        ConcurrentMap<String, Object> concurrentMap = Suppliers.<String, Object> ofConcurrentMap().get();
        Assertions.assertNotNull(concurrentMap);
        Assertions.assertTrue(concurrentMap instanceof ConcurrentHashMap);
        Assertions.assertTrue(concurrentMap.isEmpty());
    }

    // --- ofConcurrentHashMap ---

    @Test
    public void testOfConcurrentHashMap() {
        ConcurrentHashMap<String, Object> concurrentHashMap = Suppliers.<String, Object> ofConcurrentHashMap().get();
        Assertions.assertNotNull(concurrentHashMap);
        Assertions.assertTrue(concurrentHashMap.isEmpty());
    }

    // --- ofConcurrentNavigableMap ---

    @Test
    public void testOfConcurrentNavigableMap() {
        ConcurrentNavigableMap<String, Object> concurrentNavigableMap = Suppliers.<String, Object> ofConcurrentNavigableMap().get();
        Assertions.assertNotNull(concurrentNavigableMap);
        Assertions.assertTrue(concurrentNavigableMap instanceof ConcurrentSkipListMap);
        Assertions.assertTrue(concurrentNavigableMap.isEmpty());
    }

    // --- ofConcurrentHashSet ---

    @Test
    public void testOfConcurrentHashSet() {
        Set<String> concurrentSet = Suppliers.<String> ofConcurrentHashSet().get();
        Assertions.assertNotNull(concurrentSet);
        Assertions.assertTrue(concurrentSet.isEmpty());

        concurrentSet.add("test");
        Assertions.assertTrue(concurrentSet.contains("test"));
    }

    // --- ofBiMap ---

    @Test
    public void testOfBiMap() {
        BiMap<String, Integer> biMap = Suppliers.<String, Integer> ofBiMap().get();
        Assertions.assertNotNull(biMap);
        Assertions.assertTrue(biMap.isEmpty());
    }

    // --- ofMultiset ---

    @Test
    public void testOfMultiset() {
        Multiset<String> multiset = Suppliers.<String> ofMultiset().get();
        Assertions.assertNotNull(multiset);
        Assertions.assertTrue(multiset.isEmpty());
    }

    @Test
    public void testOfMultiset_WithMapType() {
        Multiset<String> linkedMultiset = Suppliers.<String> ofMultiset(LinkedHashMap.class).get();
        Assertions.assertNotNull(linkedMultiset);
        Assertions.assertTrue(linkedMultiset.isEmpty());
    }

    @Test
    public void testOfMultiset_WithMapSupplier() {
        Multiset<String> customMultiset = Suppliers.<String> ofMultiset(TreeMap::new).get();
        Assertions.assertNotNull(customMultiset);
        Assertions.assertTrue(customMultiset.isEmpty());
    }

    // --- ofListMultimap ---

    @Test
    public void testOfListMultimap() {
        ListMultimap<String, Integer> listMultimap = Suppliers.<String, Integer> ofListMultimap().get();
        Assertions.assertNotNull(listMultimap);
        Assertions.assertTrue(listMultimap.isEmpty());
    }

    @Test
    public void testOfListMultimap_WithMapType() {
        ListMultimap<String, Integer> customListMultimap = Suppliers.<String, Integer> ofListMultimap(TreeMap.class).get();
        Assertions.assertNotNull(customListMultimap);
        Assertions.assertTrue(customListMultimap.isEmpty());
    }

    @Test
    public void testOfListMultimap_WithMapAndValueType() {
        ListMultimap<String, Integer> typedListMultimap = Suppliers.<String, Integer> ofListMultimap(HashMap.class, ArrayList.class).get();
        Assertions.assertNotNull(typedListMultimap);
        Assertions.assertTrue(typedListMultimap.isEmpty());
    }

    @Test
    public void testOfListMultimap_WithSuppliers() {
        ListMultimap<String, Integer> supplierListMultimap = Suppliers.<String, Integer> ofListMultimap(HashMap::new, ArrayList::new).get();
        Assertions.assertNotNull(supplierListMultimap);
        Assertions.assertTrue(supplierListMultimap.isEmpty());
    }

    // --- ofSetMultimap ---

    @Test
    public void testOfSetMultimap() {
        SetMultimap<String, Integer> setMultimap = Suppliers.<String, Integer> ofSetMultimap().get();
        Assertions.assertNotNull(setMultimap);
        Assertions.assertTrue(setMultimap.isEmpty());
    }

    @Test
    public void testOfSetMultimap_WithMapType() {
        SetMultimap<String, Integer> customSetMultimap = Suppliers.<String, Integer> ofSetMultimap(LinkedHashMap.class).get();
        Assertions.assertNotNull(customSetMultimap);
        Assertions.assertTrue(customSetMultimap.isEmpty());
    }

    @Test
    public void testOfSetMultimap_WithMapAndValueType() {
        SetMultimap<String, Integer> typedSetMultimap = Suppliers.<String, Integer> ofSetMultimap(TreeMap.class, TreeSet.class).get();
        Assertions.assertNotNull(typedSetMultimap);
        Assertions.assertTrue(typedSetMultimap.isEmpty());
    }

    @Test
    public void testOfSetMultimap_WithSuppliers() {
        SetMultimap<String, Integer> supplierSetMultimap = Suppliers.<String, Integer> ofSetMultimap(LinkedHashMap::new, HashSet::new).get();
        Assertions.assertNotNull(supplierSetMultimap);
        Assertions.assertTrue(supplierSetMultimap.isEmpty());
    }

    // --- ofMultimap ---

    @Test
    public void testOfMultimap() {
        Multimap<String, Integer, List<Integer>> generalMultimap = Suppliers.<String, Integer, List<Integer>> ofMultimap(TreeMap::new, ArrayList::new).get();
        Assertions.assertNotNull(generalMultimap);
        Assertions.assertTrue(generalMultimap.isEmpty());
    }

    @Test
    public void testOfSpecialCollections() {
        BiMap<String, Integer> biMap = Suppliers.<String, Integer> ofBiMap().get();
        Assertions.assertNotNull(biMap);
        Assertions.assertTrue(biMap.isEmpty());

        Multiset<String> multiset = Suppliers.<String> ofMultiset().get();
        Assertions.assertNotNull(multiset);
        Assertions.assertTrue(multiset.isEmpty());

        Multiset<String> linkedMultiset = Suppliers.<String> ofMultiset(LinkedHashMap.class).get();
        Assertions.assertNotNull(linkedMultiset);
        Assertions.assertTrue(linkedMultiset.isEmpty());

        Multiset<String> customMultiset = Suppliers.<String> ofMultiset(TreeMap::new).get();
        Assertions.assertNotNull(customMultiset);
        Assertions.assertTrue(customMultiset.isEmpty());
    }

    @Test
    public void testOfMultimaps() {
        ListMultimap<String, Integer> listMultimap = Suppliers.<String, Integer> ofListMultimap().get();
        Assertions.assertNotNull(listMultimap);
        Assertions.assertTrue(listMultimap.isEmpty());

        SetMultimap<String, Integer> setMultimap = Suppliers.<String, Integer> ofSetMultimap().get();
        Assertions.assertNotNull(setMultimap);
        Assertions.assertTrue(setMultimap.isEmpty());

        ListMultimap<String, Integer> customListMultimap = Suppliers.<String, Integer> ofListMultimap(TreeMap.class).get();
        Assertions.assertNotNull(customListMultimap);

        SetMultimap<String, Integer> customSetMultimap = Suppliers.<String, Integer> ofSetMultimap(LinkedHashMap.class).get();
        Assertions.assertNotNull(customSetMultimap);

        ListMultimap<String, Integer> typedListMultimap = Suppliers.<String, Integer> ofListMultimap(HashMap.class, ArrayList.class).get();
        Assertions.assertNotNull(typedListMultimap);

        SetMultimap<String, Integer> typedSetMultimap = Suppliers.<String, Integer> ofSetMultimap(TreeMap.class, TreeSet.class).get();
        Assertions.assertNotNull(typedSetMultimap);

        ListMultimap<String, Integer> supplierListMultimap = Suppliers.<String, Integer> ofListMultimap(HashMap::new, ArrayList::new).get();
        Assertions.assertNotNull(supplierListMultimap);

        SetMultimap<String, Integer> supplierSetMultimap = Suppliers.<String, Integer> ofSetMultimap(LinkedHashMap::new, HashSet::new).get();
        Assertions.assertNotNull(supplierSetMultimap);

        Multimap<String, Integer, List<Integer>> generalMultimap = Suppliers.<String, Integer, List<Integer>> ofMultimap(TreeMap::new, ArrayList::new).get();
        Assertions.assertNotNull(generalMultimap);
    }

    // --- ofStringBuilder ---

    @Test
    public void testOfStringBuilder() {
        StringBuilder sb = Suppliers.ofStringBuilder().get();
        Assertions.assertNotNull(sb);
        Assertions.assertEquals(0, sb.length());

        StringBuilder sb2 = Suppliers.ofStringBuilder().get();
        Assertions.assertNotSame(sb, sb2);
    }

    // --- ofCollection(Class) ---

    @Test
    public void testOfCollection() {
        Supplier<? extends Collection<String>> listSupplier = Suppliers.ofCollection(ArrayList.class);
        Collection<String> list = listSupplier.get();
        Assertions.assertTrue(list instanceof ArrayList);

        Supplier<? extends Collection<String>> setSupplier = Suppliers.ofCollection(HashSet.class);
        Collection<String> set = setSupplier.get();
        Assertions.assertTrue(set instanceof HashSet);

        Supplier<? extends Collection<String>> linkedListSupplier = Suppliers.ofCollection(LinkedList.class);
        Collection<String> linkedList = linkedListSupplier.get();
        Assertions.assertTrue(linkedList instanceof LinkedList);

        Supplier<? extends Collection<String>> treeSetSupplier = Suppliers.ofCollection(TreeSet.class);
        Collection<String> treeSet = treeSetSupplier.get();
        Assertions.assertTrue(treeSet instanceof TreeSet);

        Supplier<? extends Collection<String>> collectionSupplier = Suppliers.ofCollection(Collection.class);
        Collection<String> collection = collectionSupplier.get();
        Assertions.assertTrue(collection instanceof ArrayList);

        Supplier<? extends Collection<String>> listInterfaceSupplier = Suppliers.ofCollection(List.class);
        Collection<String> listInterface = listInterfaceSupplier.get();
        Assertions.assertTrue(listInterface instanceof ArrayList);

        Supplier<? extends Collection<String>> setInterfaceSupplier = Suppliers.ofCollection(Set.class);
        Collection<String> setInterface = setInterfaceSupplier.get();
        Assertions.assertTrue(setInterface instanceof HashSet);
    }

    @Test
    public void testOfCollection_LinkedHashSet() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(LinkedHashSet.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof LinkedHashSet);
    }

    @Test
    public void testOfCollection_SortedSet() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(SortedSet.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof TreeSet);
    }

    @Test
    public void testOfCollection_Queue() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(Queue.class);
        Collection<String> result = supplier.get();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testOfCollection_Deque() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(Deque.class);
        Collection<String> result = supplier.get();
        Assertions.assertNotNull(result);
    }

    @Test
    public void testOfCollection_PriorityQueue() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(PriorityQueue.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof PriorityQueue);
    }

    @Test
    public void testOfCollection_ConcurrentLinkedQueue() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(ConcurrentLinkedQueue.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof ConcurrentLinkedQueue);
    }

    // --- ofMap(Class) ---

    @Test
    public void testOfMap() {
        Supplier<? extends Map<String, Object>> mapSupplier = Suppliers.ofMap(HashMap.class);
        Map<String, Object> map = mapSupplier.get();
        Assertions.assertTrue(map instanceof HashMap);

        Supplier<? extends Map<String, Object>> treeMapSupplier = Suppliers.ofMap(TreeMap.class);
        Map<String, Object> treeMap = treeMapSupplier.get();
        Assertions.assertTrue(treeMap instanceof TreeMap);

        Supplier<? extends Map<String, Object>> linkedMapSupplier = Suppliers.ofMap(LinkedHashMap.class);
        Map<String, Object> linkedMap = linkedMapSupplier.get();
        Assertions.assertTrue(linkedMap instanceof LinkedHashMap);

        Supplier<? extends Map<String, Object>> concurrentMapSupplier = Suppliers.ofMap(ConcurrentHashMap.class);
        Map<String, Object> concurrentMap = concurrentMapSupplier.get();
        Assertions.assertTrue(concurrentMap instanceof ConcurrentHashMap);

        Supplier<? extends Map<String, Object>> mapInterfaceSupplier = Suppliers.ofMap(Map.class);
        Map<String, Object> mapInterface = mapInterfaceSupplier.get();
        Assertions.assertTrue(mapInterface instanceof HashMap);

        Supplier<? extends Map<String, Object>> sortedMapSupplier = Suppliers.ofMap(SortedMap.class);
        Map<String, Object> sortedMap = sortedMapSupplier.get();
        Assertions.assertTrue(sortedMap instanceof TreeMap);

        Supplier<? extends Map<String, Object>> concurrentNavigableMapSupplier = Suppliers.ofMap(ConcurrentNavigableMap.class);
        Map<String, Object> concurrentNavigableMap = concurrentNavigableMapSupplier.get();
        Assertions.assertTrue(concurrentNavigableMap instanceof ConcurrentSkipListMap);

        Supplier<? extends Map<String, Object>> concurrentSkipListMapSupplier = Suppliers.ofMap(ConcurrentSkipListMap.class);
        Map<String, Object> concurrentSkipListMap = concurrentSkipListMapSupplier.get();
        Assertions.assertTrue(concurrentSkipListMap instanceof ConcurrentSkipListMap);
    }

    @Test
    public void testOfMap_ConcurrentMapInterface() {
        Supplier<? extends Map<String, Object>> supplier = Suppliers.ofMap(ConcurrentMap.class);
        Map<String, Object> result = supplier.get();
        Assertions.assertTrue(result instanceof ConcurrentHashMap);
    }

    @Test
    public void testOfMap_NavigableMap() {
        Supplier<? extends Map<String, Object>> supplier = Suppliers.ofMap(NavigableMap.class);
        Map<String, Object> result = supplier.get();
        Assertions.assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testOfMaps() {
        Map<String, Object> map = Suppliers.<String, Object> ofMap().get();
        Assertions.assertNotNull(map);
        Assertions.assertTrue(map instanceof HashMap);
        Assertions.assertTrue(map.isEmpty());

        Map<String, Object> linkedHashMap = Suppliers.<String, Object> ofLinkedHashMap().get();
        Assertions.assertNotNull(linkedHashMap);
        Assertions.assertTrue(linkedHashMap instanceof LinkedHashMap);

        IdentityHashMap<String, Object> identityHashMap = Suppliers.<String, Object> ofIdentityHashMap().get();
        Assertions.assertNotNull(identityHashMap);
        Assertions.assertTrue(identityHashMap.isEmpty());

        TreeMap<String, Object> treeMap = Suppliers.<String, Object> ofTreeMap().get();
        Assertions.assertNotNull(treeMap);
        Assertions.assertTrue(treeMap.isEmpty());

        SortedMap<String, Object> sortedMap = Suppliers.<String, Object> ofSortedMap().get();
        Assertions.assertNotNull(sortedMap);
        Assertions.assertTrue(sortedMap instanceof TreeMap);

        NavigableMap<String, Object> navigableMap = Suppliers.<String, Object> ofNavigableMap().get();
        Assertions.assertNotNull(navigableMap);
        Assertions.assertTrue(navigableMap instanceof TreeMap);

        ConcurrentMap<String, Object> concurrentMap = Suppliers.<String, Object> ofConcurrentMap().get();
        Assertions.assertNotNull(concurrentMap);
        Assertions.assertTrue(concurrentMap instanceof ConcurrentHashMap);

        ConcurrentHashMap<String, Object> concurrentHashMap = Suppliers.<String, Object> ofConcurrentHashMap().get();
        Assertions.assertNotNull(concurrentHashMap);
        Assertions.assertTrue(concurrentHashMap.isEmpty());

        ConcurrentNavigableMap<String, Object> concurrentNavigableMap = Suppliers.<String, Object> ofConcurrentNavigableMap().get();
        Assertions.assertNotNull(concurrentNavigableMap);
        Assertions.assertTrue(concurrentNavigableMap instanceof ConcurrentSkipListMap);

        Map<String, Object> concurrentMapFromType = Suppliers.<String, Object> ofMap(ConcurrentMap.class).get();
        Assertions.assertTrue(concurrentMapFromType instanceof ConcurrentHashMap);

        Map<String, Object> concurrentNavigableMapFromType = Suppliers.<String, Object> ofMap(ConcurrentNavigableMap.class).get();
        Assertions.assertTrue(concurrentNavigableMapFromType instanceof ConcurrentSkipListMap);

        Map<String, Object> concurrentSkipListMapFromType = Suppliers.<String, Object> ofMap(ConcurrentSkipListMap.class).get();
        Assertions.assertTrue(concurrentSkipListMapFromType instanceof ConcurrentSkipListMap);
    }

    // --- registerForCollection ---

    @Test
    public void testRegisterForCollection_BuiltinClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForCollection(ArrayList.class, ArrayList::new);
        });
    }

    @Test
    public void testRegisterForCollection_NullArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForCollection(null, ArrayList::new);
        });
    }

    // --- registerForMap ---

    @Test
    public void testRegisterForMap_BuiltinClass() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForMap(HashMap.class, HashMap::new);
        });
    }

    @Test
    public void testRegisterForMap_NullArgs() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.registerForMap(null, HashMap::new);
        });
    }

    // --- ofImmutableList, ofImmutableSet, ofImmutableMap ---

    @Test
    public void testOfImmutableList() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableList();
        });
    }

    @Test
    public void testOfImmutableSet() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableSet();
        });
    }

    @Test
    public void testOfImmutableMap() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            Suppliers.ofImmutableMap();
        });
    }

    @Test
    public void testUnsupportedImmutableOperations() {
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

    // --- newException ---

    @Test
    public void testNewException() {
        Supplier<Exception> exceptionSupplier = Suppliers.newException();
        Exception ex1 = exceptionSupplier.get();
        Exception ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);
    }

    // --- newRuntimeException ---

    @Test
    public void testNewRuntimeException() {
        Supplier<RuntimeException> exceptionSupplier = Suppliers.newRuntimeException();
        RuntimeException ex1 = exceptionSupplier.get();
        RuntimeException ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);
    }

    // --- newNoSuchElementException ---

    @Test
    public void testNewNoSuchElementException() {
        Supplier<NoSuchElementException> exceptionSupplier = Suppliers.newNoSuchElementException();
        NoSuchElementException ex1 = exceptionSupplier.get();
        NoSuchElementException ex2 = exceptionSupplier.get();

        Assertions.assertNotNull(ex1);
        Assertions.assertNotNull(ex2);
        Assertions.assertNotSame(ex1, ex2);
    }

    // --- error cases ---

    @Test
    public void testInvalidCollectionType() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofCollection((Class) String.class);
        });
    }

    @Test
    public void testInvalidMapType() {

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Suppliers.ofMap((Class) String.class);
        });
    }

    @Test
    public void testOfSupplier_ReturnsSameInstance() {
        Supplier<String> original = () -> "hello";
        Supplier<String> result = Suppliers.of(original);
        Assertions.assertSame(original, result);
    }

    @Test
    public void testOfInstance_Null() {
        Supplier<Object> supplier = Suppliers.ofInstance(null);
        Assertions.assertNull(supplier.get());
        Assertions.assertNull(supplier.get());
    }

    @Test
    public void testOfUuid_UniqueEachTime() {
        Supplier<String> uuidSupplier = Suppliers.ofUuid();
        java.util.Set<String> uuids = new java.util.HashSet<>();
        for (int i = 0; i < 100; i++) {
            uuids.add(uuidSupplier.get());
        }
        Assertions.assertEquals(100, uuids.size());
    }

    @Test
    public void testOfUuidWithoutHyphens_Format() {
        Supplier<String> guidSupplier = Suppliers.ofUuidWithoutHyphens();
        String guid = guidSupplier.get();
        Assertions.assertNotNull(guid);
        Assertions.assertEquals(32, guid.length());
        Assertions.assertFalse(guid.contains("-"));
        // All characters should be hex digits
        Assertions.assertTrue(guid.matches("[0-9a-f]+"));
    }

    @Test
    public void testOfStringBuilder_NewInstanceEachCall() {
        Supplier<StringBuilder> supplier = Suppliers.ofStringBuilder();
        StringBuilder sb1 = supplier.get();
        StringBuilder sb2 = supplier.get();
        Assertions.assertNotSame(sb1, sb2);
        Assertions.assertEquals(0, sb1.length());
        Assertions.assertEquals(0, sb2.length());
    }

    @Test
    public void testOfEmptyString_ReturnsEmptyString() {
        Supplier<String> supplier = Suppliers.ofEmptyString();
        Assertions.assertEquals("", supplier.get());
        // Should return same instance
        Assertions.assertSame(supplier.get(), supplier.get());
    }

    @Test
    public void testOfCollection_ArrayDeque() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(ArrayDeque.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof ArrayDeque);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testOfCollection_NavigableSet() {
        Supplier<? extends Collection<String>> supplier = Suppliers.ofCollection(NavigableSet.class);
        Collection<String> result = supplier.get();
        Assertions.assertTrue(result instanceof TreeSet);
    }

    @Test
    public void testOfMap_IdentityHashMap() {
        Supplier<? extends Map<String, Object>> supplier = Suppliers.ofMap(IdentityHashMap.class);
        Map<String, Object> result = supplier.get();
        Assertions.assertTrue(result instanceof IdentityHashMap);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void testNewException_ReturnsNewInstances() {
        Supplier<Exception> supplier = Suppliers.newException();
        Exception e1 = supplier.get();
        Exception e2 = supplier.get();
        Assertions.assertNotSame(e1, e2);
        Assertions.assertTrue(e1 instanceof Exception);
    }

    @Test
    public void testNewRuntimeException_ReturnsNewInstances() {
        Supplier<RuntimeException> supplier = Suppliers.newRuntimeException();
        RuntimeException e1 = supplier.get();
        RuntimeException e2 = supplier.get();
        Assertions.assertNotSame(e1, e2);
        Assertions.assertTrue(e1 instanceof RuntimeException);
    }

    @Test
    public void testNewNoSuchElementException_ReturnsNewInstances() {
        Supplier<NoSuchElementException> supplier = Suppliers.newNoSuchElementException();
        NoSuchElementException e1 = supplier.get();
        NoSuchElementException e2 = supplier.get();
        Assertions.assertNotSame(e1, e2);
        Assertions.assertTrue(e1 instanceof NoSuchElementException);
    }
}
