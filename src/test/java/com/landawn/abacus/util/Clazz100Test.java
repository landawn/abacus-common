package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Clazz100Test extends TestBase {

    @Test
    public void testConstants() {
        Assertions.assertEquals(LinkedHashMap.class, Clazz.PROPS_MAP);
        Assertions.assertEquals(Map.class, Clazz.MAP);
        Assertions.assertEquals(LinkedHashMap.class, Clazz.LINKED_HASH_MAP);
        Assertions.assertEquals(List.class, Clazz.STRING_LIST);
        Assertions.assertEquals(List.class, Clazz.INTEGER_LIST);
        Assertions.assertEquals(List.class, Clazz.LONG_LIST);
        Assertions.assertEquals(List.class, Clazz.DOUBLE_LIST);
        Assertions.assertEquals(List.class, Clazz.OBJECT_LIST);
        Assertions.assertEquals(Set.class, Clazz.STRING_SET);
        Assertions.assertEquals(Set.class, Clazz.INTEGER_SET);
        Assertions.assertEquals(Set.class, Clazz.LONG_SET);
        Assertions.assertEquals(Set.class, Clazz.DOUBLE_SET);
        Assertions.assertEquals(Set.class, Clazz.OBJECT_SET);
    }

    @Test
    public void testOf() {
        Class<ArrayList<String>> arrayListClass = Clazz.of(ArrayList.class);
        Assertions.assertEquals(ArrayList.class, arrayListClass);

        Class<HashMap<String, Integer>> hashMapClass = Clazz.of(HashMap.class);
        Assertions.assertEquals(HashMap.class, hashMapClass);
    }

    @Test
    public void testOfList() {
        Class<List<String>> listClass = Clazz.ofList();
        Assertions.assertEquals(List.class, listClass);

        Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);
        Assertions.assertEquals(List.class, intListClass);
    }

    @Test
    public void testOfLinkedList() {
        Class<List<String>> linkedListClass = Clazz.ofLinkedList();
        Assertions.assertEquals(LinkedList.class, linkedListClass);

        Class<List<Double>> doubleLinkedListClass = Clazz.ofLinkedList(Double.class);
        Assertions.assertEquals(LinkedList.class, doubleLinkedListClass);
    }

    @Test
    public void testOfListOfMap() {
        Class<List<Map<String, Object>>> listOfMapsClass = Clazz.ofListOfMap(String.class, Object.class);
        Assertions.assertEquals(List.class, listOfMapsClass);
    }

    @Test
    public void testOfSetOfMap() {
        Class<Set<Map<String, Integer>>> setOfMapsClass = Clazz.ofSetOfMap(String.class, Integer.class);
        Assertions.assertEquals(Set.class, setOfMapsClass);
    }

    @Test
    public void testOfSet() {
        Class<Set<String>> setClass = Clazz.ofSet();
        Assertions.assertEquals(Set.class, setClass);

        Class<Set<Long>> longSetClass = Clazz.ofSet(Long.class);
        Assertions.assertEquals(Set.class, longSetClass);
    }

    @Test
    public void testOfLinkedHashSet() {
        Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet();
        Assertions.assertEquals(LinkedHashSet.class, linkedHashSetClass);

        Class<Set<Integer>> intLinkedHashSetClass = Clazz.ofLinkedHashSet(Integer.class);
        Assertions.assertEquals(LinkedHashSet.class, intLinkedHashSetClass);
    }

    @Test
    public void testOfSortedSet() {
        Class<SortedSet<String>> sortedSetClass = Clazz.ofSortedSet();
        Assertions.assertEquals(SortedSet.class, sortedSetClass);

        Class<SortedSet<Integer>> intSortedSetClass = Clazz.ofSortedSet(Integer.class);
        Assertions.assertEquals(SortedSet.class, intSortedSetClass);
    }

    @Test
    public void testOfNavigableSet() {
        Class<NavigableSet<String>> navigableSetClass = Clazz.ofNavigableSet();
        Assertions.assertEquals(NavigableSet.class, navigableSetClass);

        Class<NavigableSet<Double>> doubleNavigableSetClass = Clazz.ofNavigableSet(Double.class);
        Assertions.assertEquals(NavigableSet.class, doubleNavigableSetClass);
    }

    @Test
    public void testOfTreeSet() {
        Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet();
        Assertions.assertEquals(TreeSet.class, treeSetClass);

        Class<NavigableSet<Integer>> intTreeSetClass = Clazz.ofTreeSet(Integer.class);
        Assertions.assertEquals(TreeSet.class, intTreeSetClass);
    }

    @Test
    public void testOfQueue() {
        Class<Queue<String>> queueClass = Clazz.ofQueue();
        Assertions.assertEquals(Queue.class, queueClass);

        Class<Queue<Object>> objectQueueClass = Clazz.ofQueue(Object.class);
        Assertions.assertEquals(Queue.class, objectQueueClass);
    }

    @Test
    public void testOfDeque() {
        Class<Deque<String>> dequeClass = Clazz.ofDeque();
        Assertions.assertEquals(Deque.class, dequeClass);

        Class<Deque<Integer>> intDequeClass = Clazz.ofDeque(Integer.class);
        Assertions.assertEquals(Deque.class, intDequeClass);
    }

    @Test
    public void testOfArrayDeque() {
        Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque();
        Assertions.assertEquals(ArrayDeque.class, arrayDequeClass);

        Class<Deque<Long>> longArrayDequeClass = Clazz.ofArrayDeque(Long.class);
        Assertions.assertEquals(ArrayDeque.class, longArrayDequeClass);
    }

    @Test
    public void testOfConcurrentLinkedQueue() {
        Class<Queue<String>> concurrentQueueClass = Clazz.ofConcurrentLinkedQueue();
        Assertions.assertEquals(ConcurrentLinkedQueue.class, concurrentQueueClass);

        Class<Queue<Integer>> intConcurrentQueueClass = Clazz.ofConcurrentLinkedQueue(Integer.class);
        Assertions.assertEquals(ConcurrentLinkedQueue.class, intConcurrentQueueClass);
    }

    @Test
    public void testOfPriorityQueue() {
        Class<Queue<String>> priorityQueueClass = Clazz.ofPriorityQueue();
        Assertions.assertEquals(PriorityQueue.class, priorityQueueClass);

        Class<Queue<Integer>> intPriorityQueueClass = Clazz.ofPriorityQueue(Integer.class);
        Assertions.assertEquals(PriorityQueue.class, intPriorityQueueClass);
    }

    @Test
    public void testOfLinkedBlockingQueue() {
        Class<BlockingQueue<String>> blockingQueueClass = Clazz.ofLinkedBlockingQueue();
        Assertions.assertEquals(LinkedBlockingQueue.class, blockingQueueClass);

        Class<BlockingQueue<Object>> objectBlockingQueueClass = Clazz.ofLinkedBlockingQueue(Object.class);
        Assertions.assertEquals(LinkedBlockingQueue.class, objectBlockingQueueClass);
    }

    @Test
    public void testOfCollection() {
        Class<Collection<String>> collectionClass = Clazz.ofCollection();
        Assertions.assertEquals(Collection.class, collectionClass);

        Class<Collection<Integer>> intCollectionClass = Clazz.ofCollection(Integer.class);
        Assertions.assertEquals(Collection.class, intCollectionClass);
    }

    @Test
    public void testOfMap() {
        Class<Map<String, Object>> mapClass = Clazz.ofMap();
        Assertions.assertEquals(Map.class, mapClass);

        Class<Map<String, Integer>> stringIntMapClass = Clazz.ofMap(String.class, Integer.class);
        Assertions.assertEquals(Map.class, stringIntMapClass);

        Class<Map<Long, String>> longStringMapClass = Clazz.ofMap(Long.class, String.class);
        Assertions.assertEquals(Map.class, longStringMapClass);
    }

    @Test
    public void testOfLinkedHashMap() {
        Class<Map<String, Object>> linkedHashMapClass = Clazz.ofLinkedHashMap();
        Assertions.assertEquals(LinkedHashMap.class, linkedHashMapClass);

        Class<Map<Integer, String>> intStringLinkedHashMapClass = Clazz.ofLinkedHashMap(Integer.class, String.class);
        Assertions.assertEquals(LinkedHashMap.class, intStringLinkedHashMapClass);
    }

    @Test
    public void testOfSortedMap() {
        Class<SortedMap<String, Object>> sortedMapClass = Clazz.ofSortedMap();
        Assertions.assertEquals(SortedMap.class, sortedMapClass);

        Class<SortedMap<Integer, String>> intStringSortedMapClass = Clazz.ofSortedMap(Integer.class, String.class);
        Assertions.assertEquals(SortedMap.class, intStringSortedMapClass);
    }

    @Test
    public void testOfNavigableMap() {
        Class<NavigableMap<String, Object>> navigableMapClass = Clazz.ofNavigableMap();
        Assertions.assertEquals(NavigableMap.class, navigableMapClass);

        Class<NavigableMap<Long, Double>> longDoubleNavigableMapClass = Clazz.ofNavigableMap(Long.class, Double.class);
        Assertions.assertEquals(NavigableMap.class, longDoubleNavigableMapClass);
    }

    @Test
    public void testOfTreeMap() {
        Class<NavigableMap<String, Object>> treeMapClass = Clazz.ofTreeMap();
        Assertions.assertEquals(TreeMap.class, treeMapClass);

        Class<NavigableMap<Integer, String>> intStringTreeMapClass = Clazz.ofTreeMap(Integer.class, String.class);
        Assertions.assertEquals(TreeMap.class, intStringTreeMapClass);
    }

    @Test
    public void testOfConcurrentMap() {
        Class<ConcurrentMap<String, Object>> concurrentMapClass = Clazz.ofConcurrentMap();
        Assertions.assertEquals(ConcurrentMap.class, concurrentMapClass);

        Class<ConcurrentMap<Long, String>> longStringConcurrentMapClass = Clazz.ofConcurrentMap(Long.class, String.class);
        Assertions.assertEquals(ConcurrentMap.class, longStringConcurrentMapClass);
    }

    @Test
    public void testOfConcurrentHashMap() {
        Class<ConcurrentMap<String, Object>> concurrentHashMapClass = Clazz.ofConcurrentHashMap();
        Assertions.assertEquals(ConcurrentHashMap.class, concurrentHashMapClass);

        Class<ConcurrentMap<Integer, Double>> intDoubleConcurrentHashMapClass = Clazz.ofConcurrentHashMap(Integer.class, Double.class);
        Assertions.assertEquals(ConcurrentHashMap.class, intDoubleConcurrentHashMapClass);
    }

    @Test
    public void testOfBiMap() {
        Class<BiMap<String, Integer>> biMapClass = Clazz.ofBiMap();
        Assertions.assertEquals(BiMap.class, biMapClass);

        Class<BiMap<Long, String>> longStringBiMapClass = Clazz.ofBiMap(Long.class, String.class);
        Assertions.assertEquals(BiMap.class, longStringBiMapClass);
    }

    @Test
    public void testOfMultiset() {
        Class<Multiset<String>> multisetClass = Clazz.ofMultiset();
        Assertions.assertEquals(Multiset.class, multisetClass);

        Class<Multiset<Integer>> intMultisetClass = Clazz.ofMultiset(Integer.class);
        Assertions.assertEquals(Multiset.class, intMultisetClass);
    }

    @Test
    public void testOfListMultimap() {
        Class<ListMultimap<String, Integer>> listMultimapClass = Clazz.ofListMultimap();
        Assertions.assertEquals(ListMultimap.class, listMultimapClass);

        Class<ListMultimap<Long, String>> longStringListMultimapClass = Clazz.ofListMultimap(Long.class, String.class);
        Assertions.assertEquals(ListMultimap.class, longStringListMultimapClass);
    }

    @Test
    public void testOfSetMultimap() {
        Class<SetMultimap<String, Integer>> setMultimapClass = Clazz.ofSetMultimap();
        Assertions.assertEquals(SetMultimap.class, setMultimapClass);

        Class<SetMultimap<Integer, String>> intStringSetMultimapClass = Clazz.ofSetMultimap(Integer.class, String.class);
        Assertions.assertEquals(SetMultimap.class, intStringSetMultimapClass);
    }

    @Test
    public void testTypeErasure() {
        Class<List<String>> stringListClass = Clazz.ofList(String.class);
        Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);

        Assertions.assertEquals(stringListClass, intListClass);
        Assertions.assertEquals(List.class, stringListClass);
        Assertions.assertEquals(List.class, intListClass);
    }

    @Test
    public void testUsageWithInstanceOf() {
        Object obj = new ArrayList<String>();
        Assertions.assertTrue(Clazz.ofList().isInstance(obj));

        obj = new LinkedHashMap<String, Object>();
        Assertions.assertTrue(Clazz.PROPS_MAP.isInstance(obj));

        obj = new TreeSet<Integer>();
        Assertions.assertTrue(Clazz.ofTreeSet().isInstance(obj));
    }

    @Test
    public void testPrivateConstructor() {
        try {
            java.lang.reflect.Constructor<Clazz> constructor = Clazz.class.getDeclaredConstructor();
            Assertions.assertTrue(java.lang.reflect.Modifier.isPrivate(constructor.getModifiers()));
        } catch (NoSuchMethodException e) {
            Assertions.fail("Should have a private constructor");
        }
    }
}
