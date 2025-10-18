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

@Tag("2025")
public class Clazz2025Test extends TestBase {

    @Test
    public void testConstants() {
        Assertions.assertNotNull(Clazz.PROPS_MAP);
        Assertions.assertNotNull(Clazz.MAP);
        Assertions.assertNotNull(Clazz.LINKED_HASH_MAP);
        Assertions.assertNotNull(Clazz.STRING_LIST);
        Assertions.assertNotNull(Clazz.INTEGER_LIST);
        Assertions.assertNotNull(Clazz.LONG_LIST);
        Assertions.assertNotNull(Clazz.DOUBLE_LIST);
        Assertions.assertNotNull(Clazz.OBJECT_LIST);
        Assertions.assertNotNull(Clazz.STRING_SET);
        Assertions.assertNotNull(Clazz.INTEGER_SET);
        Assertions.assertNotNull(Clazz.LONG_SET);
        Assertions.assertNotNull(Clazz.DOUBLE_SET);
        Assertions.assertNotNull(Clazz.OBJECT_SET);

        Assertions.assertEquals(LinkedHashMap.class, Clazz.PROPS_MAP);
        Assertions.assertEquals(Map.class, Clazz.MAP);
        Assertions.assertEquals(LinkedHashMap.class, Clazz.LINKED_HASH_MAP);
    }

    @Test
    public void testConstantsAreCorrectTypes() {
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

        Class<TreeSet<Long>> treeSetClass = Clazz.of(TreeSet.class);
        Assertions.assertEquals(TreeSet.class, treeSetClass);

        Class<LinkedList<Double>> linkedListClass = Clazz.of(LinkedList.class);
        Assertions.assertEquals(LinkedList.class, linkedListClass);
    }

    @Test
    public void testOfList() {
        Class<List<String>> listClass = Clazz.ofList();
        Assertions.assertEquals(List.class, listClass);
        Assertions.assertNotNull(listClass);

        Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);
        Assertions.assertEquals(List.class, intListClass);

        Class<List<String>> stringListClass = Clazz.ofList(String.class);
        Assertions.assertEquals(List.class, stringListClass);

        Class<List<Double>> doubleListClass = Clazz.ofList(Double.class);
        Assertions.assertEquals(List.class, doubleListClass);

        Class<List<Long>> longListClass = Clazz.ofList(Long.class);
        Assertions.assertEquals(List.class, longListClass);

        Class<List<Object>> objectListClass = Clazz.ofList(Object.class);
        Assertions.assertEquals(List.class, objectListClass);
    }

    @Test
    public void testOfLinkedList() {
        Class<List<String>> linkedListClass = Clazz.ofLinkedList();
        Assertions.assertEquals(LinkedList.class, linkedListClass);

        Class<List<Double>> doubleLinkedListClass = Clazz.ofLinkedList(Double.class);
        Assertions.assertEquals(LinkedList.class, doubleLinkedListClass);

        Class<List<Integer>> intLinkedListClass = Clazz.ofLinkedList(Integer.class);
        Assertions.assertEquals(LinkedList.class, intLinkedListClass);

        Class<List<String>> stringLinkedListClass = Clazz.ofLinkedList(String.class);
        Assertions.assertEquals(LinkedList.class, stringLinkedListClass);
    }

    @Test
    public void testOfListOfMap() {
        Class<List<Map<String, Object>>> listOfMapsClass = Clazz.ofListOfMap(String.class, Object.class);
        Assertions.assertEquals(List.class, listOfMapsClass);
        Assertions.assertNotNull(listOfMapsClass);

        Class<List<Map<Integer, String>>> intStringListOfMapsClass = Clazz.ofListOfMap(Integer.class, String.class);
        Assertions.assertEquals(List.class, intStringListOfMapsClass);

        Class<List<Map<Long, Double>>> longDoubleListOfMapsClass = Clazz.ofListOfMap(Long.class, Double.class);
        Assertions.assertEquals(List.class, longDoubleListOfMapsClass);
    }

    @Test
    public void testOfSetOfMap() {
        Class<Set<Map<String, Integer>>> setOfMapsClass = Clazz.ofSetOfMap(String.class, Integer.class);
        Assertions.assertEquals(Set.class, setOfMapsClass);
        Assertions.assertNotNull(setOfMapsClass);

        Class<Set<Map<Long, String>>> longStringSetOfMapsClass = Clazz.ofSetOfMap(Long.class, String.class);
        Assertions.assertEquals(Set.class, longStringSetOfMapsClass);

        Class<Set<Map<Integer, Double>>> intDoubleSetOfMapsClass = Clazz.ofSetOfMap(Integer.class, Double.class);
        Assertions.assertEquals(Set.class, intDoubleSetOfMapsClass);
    }

    @Test
    public void testOfSet() {
        Class<Set<String>> setClass = Clazz.ofSet();
        Assertions.assertEquals(Set.class, setClass);

        Class<Set<Long>> longSetClass = Clazz.ofSet(Long.class);
        Assertions.assertEquals(Set.class, longSetClass);

        Class<Set<Integer>> intSetClass = Clazz.ofSet(Integer.class);
        Assertions.assertEquals(Set.class, intSetClass);

        Class<Set<String>> stringSetClass = Clazz.ofSet(String.class);
        Assertions.assertEquals(Set.class, stringSetClass);

        Class<Set<Double>> doubleSetClass = Clazz.ofSet(Double.class);
        Assertions.assertEquals(Set.class, doubleSetClass);
    }

    @Test
    public void testOfLinkedHashSet() {
        Class<Set<String>> linkedHashSetClass = Clazz.ofLinkedHashSet();
        Assertions.assertEquals(LinkedHashSet.class, linkedHashSetClass);

        Class<Set<Integer>> intLinkedHashSetClass = Clazz.ofLinkedHashSet(Integer.class);
        Assertions.assertEquals(LinkedHashSet.class, intLinkedHashSetClass);

        Class<Set<Long>> longLinkedHashSetClass = Clazz.ofLinkedHashSet(Long.class);
        Assertions.assertEquals(LinkedHashSet.class, longLinkedHashSetClass);

        Class<Set<Double>> doubleLinkedHashSetClass = Clazz.ofLinkedHashSet(Double.class);
        Assertions.assertEquals(LinkedHashSet.class, doubleLinkedHashSetClass);
    }

    @Test
    public void testOfSortedSet() {
        Class<SortedSet<String>> sortedSetClass = Clazz.ofSortedSet();
        Assertions.assertEquals(SortedSet.class, sortedSetClass);

        Class<SortedSet<Integer>> intSortedSetClass = Clazz.ofSortedSet(Integer.class);
        Assertions.assertEquals(SortedSet.class, intSortedSetClass);

        Class<SortedSet<Long>> longSortedSetClass = Clazz.ofSortedSet(Long.class);
        Assertions.assertEquals(SortedSet.class, longSortedSetClass);

        Class<SortedSet<String>> stringSortedSetClass = Clazz.ofSortedSet(String.class);
        Assertions.assertEquals(SortedSet.class, stringSortedSetClass);
    }

    @Test
    public void testOfNavigableSet() {
        Class<NavigableSet<String>> navigableSetClass = Clazz.ofNavigableSet();
        Assertions.assertEquals(NavigableSet.class, navigableSetClass);

        Class<NavigableSet<Double>> doubleNavigableSetClass = Clazz.ofNavigableSet(Double.class);
        Assertions.assertEquals(NavigableSet.class, doubleNavigableSetClass);

        Class<NavigableSet<Integer>> intNavigableSetClass = Clazz.ofNavigableSet(Integer.class);
        Assertions.assertEquals(NavigableSet.class, intNavigableSetClass);

        Class<NavigableSet<Long>> longNavigableSetClass = Clazz.ofNavigableSet(Long.class);
        Assertions.assertEquals(NavigableSet.class, longNavigableSetClass);
    }

    @Test
    public void testOfTreeSet() {
        Class<NavigableSet<String>> treeSetClass = Clazz.ofTreeSet();
        Assertions.assertEquals(TreeSet.class, treeSetClass);

        Class<NavigableSet<Integer>> intTreeSetClass = Clazz.ofTreeSet(Integer.class);
        Assertions.assertEquals(TreeSet.class, intTreeSetClass);

        Class<NavigableSet<Long>> longTreeSetClass = Clazz.ofTreeSet(Long.class);
        Assertions.assertEquals(TreeSet.class, longTreeSetClass);

        Class<NavigableSet<String>> stringTreeSetClass = Clazz.ofTreeSet(String.class);
        Assertions.assertEquals(TreeSet.class, stringTreeSetClass);
    }

    @Test
    public void testOfQueue() {
        Class<Queue<String>> queueClass = Clazz.ofQueue();
        Assertions.assertEquals(Queue.class, queueClass);

        Class<Queue<Object>> objectQueueClass = Clazz.ofQueue(Object.class);
        Assertions.assertEquals(Queue.class, objectQueueClass);

        Class<Queue<Integer>> intQueueClass = Clazz.ofQueue(Integer.class);
        Assertions.assertEquals(Queue.class, intQueueClass);

        Class<Queue<String>> stringQueueClass = Clazz.ofQueue(String.class);
        Assertions.assertEquals(Queue.class, stringQueueClass);
    }

    @Test
    public void testOfDeque() {
        Class<Deque<String>> dequeClass = Clazz.ofDeque();
        Assertions.assertEquals(Deque.class, dequeClass);

        Class<Deque<Integer>> intDequeClass = Clazz.ofDeque(Integer.class);
        Assertions.assertEquals(Deque.class, intDequeClass);

        Class<Deque<Long>> longDequeClass = Clazz.ofDeque(Long.class);
        Assertions.assertEquals(Deque.class, longDequeClass);

        Class<Deque<String>> stringDequeClass = Clazz.ofDeque(String.class);
        Assertions.assertEquals(Deque.class, stringDequeClass);
    }

    @Test
    public void testOfArrayDeque() {
        Class<Deque<String>> arrayDequeClass = Clazz.ofArrayDeque();
        Assertions.assertEquals(ArrayDeque.class, arrayDequeClass);

        Class<Deque<Long>> longArrayDequeClass = Clazz.ofArrayDeque(Long.class);
        Assertions.assertEquals(ArrayDeque.class, longArrayDequeClass);

        Class<Deque<Integer>> intArrayDequeClass = Clazz.ofArrayDeque(Integer.class);
        Assertions.assertEquals(ArrayDeque.class, intArrayDequeClass);

        Class<Deque<String>> stringArrayDequeClass = Clazz.ofArrayDeque(String.class);
        Assertions.assertEquals(ArrayDeque.class, stringArrayDequeClass);
    }

    @Test
    public void testOfConcurrentLinkedQueue() {
        Class<Queue<String>> concurrentQueueClass = Clazz.ofConcurrentLinkedQueue();
        Assertions.assertEquals(ConcurrentLinkedQueue.class, concurrentQueueClass);

        Class<Queue<Integer>> intConcurrentQueueClass = Clazz.ofConcurrentLinkedQueue(Integer.class);
        Assertions.assertEquals(ConcurrentLinkedQueue.class, intConcurrentQueueClass);

        Class<Queue<Long>> longConcurrentQueueClass = Clazz.ofConcurrentLinkedQueue(Long.class);
        Assertions.assertEquals(ConcurrentLinkedQueue.class, longConcurrentQueueClass);

        Class<Queue<String>> stringConcurrentQueueClass = Clazz.ofConcurrentLinkedQueue(String.class);
        Assertions.assertEquals(ConcurrentLinkedQueue.class, stringConcurrentQueueClass);
    }

    @Test
    public void testOfPriorityQueue() {
        Class<Queue<String>> priorityQueueClass = Clazz.ofPriorityQueue();
        Assertions.assertEquals(PriorityQueue.class, priorityQueueClass);

        Class<Queue<Integer>> intPriorityQueueClass = Clazz.ofPriorityQueue(Integer.class);
        Assertions.assertEquals(PriorityQueue.class, intPriorityQueueClass);

        Class<Queue<Long>> longPriorityQueueClass = Clazz.ofPriorityQueue(Long.class);
        Assertions.assertEquals(PriorityQueue.class, longPriorityQueueClass);

        Class<Queue<String>> stringPriorityQueueClass = Clazz.ofPriorityQueue(String.class);
        Assertions.assertEquals(PriorityQueue.class, stringPriorityQueueClass);
    }

    @Test
    public void testOfLinkedBlockingQueue() {
        Class<BlockingQueue<String>> blockingQueueClass = Clazz.ofLinkedBlockingQueue();
        Assertions.assertEquals(LinkedBlockingQueue.class, blockingQueueClass);

        Class<BlockingQueue<Object>> objectBlockingQueueClass = Clazz.ofLinkedBlockingQueue(Object.class);
        Assertions.assertEquals(LinkedBlockingQueue.class, objectBlockingQueueClass);

        Class<BlockingQueue<Integer>> intBlockingQueueClass = Clazz.ofLinkedBlockingQueue(Integer.class);
        Assertions.assertEquals(LinkedBlockingQueue.class, intBlockingQueueClass);

        Class<BlockingQueue<String>> stringBlockingQueueClass = Clazz.ofLinkedBlockingQueue(String.class);
        Assertions.assertEquals(LinkedBlockingQueue.class, stringBlockingQueueClass);
    }

    @Test
    public void testOfCollection() {
        Class<Collection<String>> collectionClass = Clazz.ofCollection();
        Assertions.assertEquals(Collection.class, collectionClass);

        Class<Collection<Integer>> intCollectionClass = Clazz.ofCollection(Integer.class);
        Assertions.assertEquals(Collection.class, intCollectionClass);

        Class<Collection<Long>> longCollectionClass = Clazz.ofCollection(Long.class);
        Assertions.assertEquals(Collection.class, longCollectionClass);

        Class<Collection<String>> stringCollectionClass = Clazz.ofCollection(String.class);
        Assertions.assertEquals(Collection.class, stringCollectionClass);
    }

    @Test
    public void testOfMap() {
        Class<Map<String, Object>> mapClass = Clazz.ofMap();
        Assertions.assertEquals(Map.class, mapClass);

        Class<Map<String, Integer>> stringIntMapClass = Clazz.ofMap(String.class, Integer.class);
        Assertions.assertEquals(Map.class, stringIntMapClass);

        Class<Map<Long, String>> longStringMapClass = Clazz.ofMap(Long.class, String.class);
        Assertions.assertEquals(Map.class, longStringMapClass);

        Class<Map<Integer, Double>> intDoubleMapClass = Clazz.ofMap(Integer.class, Double.class);
        Assertions.assertEquals(Map.class, intDoubleMapClass);

        Class<Map<String, Object>> stringObjectMapClass = Clazz.ofMap(String.class, Object.class);
        Assertions.assertEquals(Map.class, stringObjectMapClass);
    }

    @Test
    public void testOfLinkedHashMap() {
        Class<Map<String, Object>> linkedHashMapClass = Clazz.ofLinkedHashMap();
        Assertions.assertEquals(LinkedHashMap.class, linkedHashMapClass);

        Class<Map<Integer, String>> intStringLinkedHashMapClass = Clazz.ofLinkedHashMap(Integer.class, String.class);
        Assertions.assertEquals(LinkedHashMap.class, intStringLinkedHashMapClass);

        Class<Map<Long, Double>> longDoubleLinkedHashMapClass = Clazz.ofLinkedHashMap(Long.class, Double.class);
        Assertions.assertEquals(LinkedHashMap.class, longDoubleLinkedHashMapClass);

        Class<Map<String, Object>> stringObjectLinkedHashMapClass = Clazz.ofLinkedHashMap(String.class, Object.class);
        Assertions.assertEquals(LinkedHashMap.class, stringObjectLinkedHashMapClass);
    }

    @Test
    public void testOfSortedMap() {
        Class<SortedMap<String, Object>> sortedMapClass = Clazz.ofSortedMap();
        Assertions.assertEquals(SortedMap.class, sortedMapClass);

        Class<SortedMap<Integer, String>> intStringSortedMapClass = Clazz.ofSortedMap(Integer.class, String.class);
        Assertions.assertEquals(SortedMap.class, intStringSortedMapClass);

        Class<SortedMap<Long, Double>> longDoubleSortedMapClass = Clazz.ofSortedMap(Long.class, Double.class);
        Assertions.assertEquals(SortedMap.class, longDoubleSortedMapClass);

        Class<SortedMap<String, Integer>> stringIntSortedMapClass = Clazz.ofSortedMap(String.class, Integer.class);
        Assertions.assertEquals(SortedMap.class, stringIntSortedMapClass);
    }

    @Test
    public void testOfNavigableMap() {
        Class<NavigableMap<String, Object>> navigableMapClass = Clazz.ofNavigableMap();
        Assertions.assertEquals(NavigableMap.class, navigableMapClass);

        Class<NavigableMap<Long, Double>> longDoubleNavigableMapClass = Clazz.ofNavigableMap(Long.class, Double.class);
        Assertions.assertEquals(NavigableMap.class, longDoubleNavigableMapClass);

        Class<NavigableMap<Integer, String>> intStringNavigableMapClass = Clazz.ofNavigableMap(Integer.class, String.class);
        Assertions.assertEquals(NavigableMap.class, intStringNavigableMapClass);

        Class<NavigableMap<String, Object>> stringObjectNavigableMapClass = Clazz.ofNavigableMap(String.class, Object.class);
        Assertions.assertEquals(NavigableMap.class, stringObjectNavigableMapClass);
    }

    @Test
    public void testOfTreeMap() {
        Class<NavigableMap<String, Object>> treeMapClass = Clazz.ofTreeMap();
        Assertions.assertEquals(TreeMap.class, treeMapClass);

        Class<NavigableMap<Integer, String>> intStringTreeMapClass = Clazz.ofTreeMap(Integer.class, String.class);
        Assertions.assertEquals(TreeMap.class, intStringTreeMapClass);

        Class<NavigableMap<Long, Double>> longDoubleTreeMapClass = Clazz.ofTreeMap(Long.class, Double.class);
        Assertions.assertEquals(TreeMap.class, longDoubleTreeMapClass);

        Class<NavigableMap<String, Object>> stringObjectTreeMapClass = Clazz.ofTreeMap(String.class, Object.class);
        Assertions.assertEquals(TreeMap.class, stringObjectTreeMapClass);
    }

    @Test
    public void testOfConcurrentMap() {
        Class<ConcurrentMap<String, Object>> concurrentMapClass = Clazz.ofConcurrentMap();
        Assertions.assertEquals(ConcurrentMap.class, concurrentMapClass);

        Class<ConcurrentMap<Long, String>> longStringConcurrentMapClass = Clazz.ofConcurrentMap(Long.class, String.class);
        Assertions.assertEquals(ConcurrentMap.class, longStringConcurrentMapClass);

        Class<ConcurrentMap<Integer, Double>> intDoubleConcurrentMapClass = Clazz.ofConcurrentMap(Integer.class, Double.class);
        Assertions.assertEquals(ConcurrentMap.class, intDoubleConcurrentMapClass);

        Class<ConcurrentMap<String, Object>> stringObjectConcurrentMapClass = Clazz.ofConcurrentMap(String.class, Object.class);
        Assertions.assertEquals(ConcurrentMap.class, stringObjectConcurrentMapClass);
    }

    @Test
    public void testOfConcurrentHashMap() {
        Class<ConcurrentMap<String, Object>> concurrentHashMapClass = Clazz.ofConcurrentHashMap();
        Assertions.assertEquals(ConcurrentHashMap.class, concurrentHashMapClass);

        Class<ConcurrentMap<Integer, Double>> intDoubleConcurrentHashMapClass = Clazz.ofConcurrentHashMap(Integer.class, Double.class);
        Assertions.assertEquals(ConcurrentHashMap.class, intDoubleConcurrentHashMapClass);

        Class<ConcurrentMap<Long, String>> longStringConcurrentHashMapClass = Clazz.ofConcurrentHashMap(Long.class, String.class);
        Assertions.assertEquals(ConcurrentHashMap.class, longStringConcurrentHashMapClass);

        Class<ConcurrentMap<String, Object>> stringObjectConcurrentHashMapClass = Clazz.ofConcurrentHashMap(String.class, Object.class);
        Assertions.assertEquals(ConcurrentHashMap.class, stringObjectConcurrentHashMapClass);
    }

    @Test
    public void testOfBiMap() {
        Class<BiMap<String, Integer>> biMapClass = Clazz.ofBiMap();
        Assertions.assertEquals(BiMap.class, biMapClass);

        Class<BiMap<Long, String>> longStringBiMapClass = Clazz.ofBiMap(Long.class, String.class);
        Assertions.assertEquals(BiMap.class, longStringBiMapClass);

        Class<BiMap<Integer, Double>> intDoubleBiMapClass = Clazz.ofBiMap(Integer.class, Double.class);
        Assertions.assertEquals(BiMap.class, intDoubleBiMapClass);

        Class<BiMap<String, Object>> stringObjectBiMapClass = Clazz.ofBiMap(String.class, Object.class);
        Assertions.assertEquals(BiMap.class, stringObjectBiMapClass);
    }

    @Test
    public void testOfMultiset() {
        Class<Multiset<String>> multisetClass = Clazz.ofMultiset();
        Assertions.assertEquals(Multiset.class, multisetClass);

        Class<Multiset<Integer>> intMultisetClass = Clazz.ofMultiset(Integer.class);
        Assertions.assertEquals(Multiset.class, intMultisetClass);

        Class<Multiset<Long>> longMultisetClass = Clazz.ofMultiset(Long.class);
        Assertions.assertEquals(Multiset.class, longMultisetClass);

        Class<Multiset<String>> stringMultisetClass = Clazz.ofMultiset(String.class);
        Assertions.assertEquals(Multiset.class, stringMultisetClass);
    }

    @Test
    public void testOfListMultimap() {
        Class<ListMultimap<String, Integer>> listMultimapClass = Clazz.ofListMultimap();
        Assertions.assertEquals(ListMultimap.class, listMultimapClass);

        Class<ListMultimap<Long, String>> longStringListMultimapClass = Clazz.ofListMultimap(Long.class, String.class);
        Assertions.assertEquals(ListMultimap.class, longStringListMultimapClass);

        Class<ListMultimap<Integer, Double>> intDoubleListMultimapClass = Clazz.ofListMultimap(Integer.class, Double.class);
        Assertions.assertEquals(ListMultimap.class, intDoubleListMultimapClass);

        Class<ListMultimap<String, Object>> stringObjectListMultimapClass = Clazz.ofListMultimap(String.class, Object.class);
        Assertions.assertEquals(ListMultimap.class, stringObjectListMultimapClass);
    }

    @Test
    public void testOfSetMultimap() {
        Class<SetMultimap<String, Integer>> setMultimapClass = Clazz.ofSetMultimap();
        Assertions.assertEquals(SetMultimap.class, setMultimapClass);

        Class<SetMultimap<Integer, String>> intStringSetMultimapClass = Clazz.ofSetMultimap(Integer.class, String.class);
        Assertions.assertEquals(SetMultimap.class, intStringSetMultimapClass);

        Class<SetMultimap<Long, Double>> longDoubleSetMultimapClass = Clazz.ofSetMultimap(Long.class, Double.class);
        Assertions.assertEquals(SetMultimap.class, longDoubleSetMultimapClass);

        Class<SetMultimap<String, Object>> stringObjectSetMultimapClass = Clazz.ofSetMultimap(String.class, Object.class);
        Assertions.assertEquals(SetMultimap.class, stringObjectSetMultimapClass);
    }

    @Test
    public void testTypeErasure() {
        Class<List<String>> stringListClass = Clazz.ofList(String.class);
        Class<List<Integer>> intListClass = Clazz.ofList(Integer.class);
        Class<List<Double>> doubleListClass = Clazz.ofList(Double.class);

        Assertions.assertEquals(stringListClass, intListClass);
        Assertions.assertEquals(stringListClass, doubleListClass);
        Assertions.assertEquals(List.class, stringListClass);
        Assertions.assertEquals(List.class, intListClass);
        Assertions.assertEquals(List.class, doubleListClass);

        Class<Map<String, Integer>> stringIntMapClass = Clazz.ofMap(String.class, Integer.class);
        Class<Map<Long, Double>> longDoubleMapClass = Clazz.ofMap(Long.class, Double.class);

        Assertions.assertEquals(stringIntMapClass, longDoubleMapClass);
        Assertions.assertEquals(Map.class, stringIntMapClass);
        Assertions.assertEquals(Map.class, longDoubleMapClass);
    }

    @Test
    public void testUsageWithInstanceOf() {
        Object obj = new ArrayList<String>();
        Assertions.assertTrue(Clazz.ofList().isInstance(obj));
        Assertions.assertTrue(Clazz.ofList(String.class).isInstance(obj));
        Assertions.assertTrue(Clazz.ofCollection().isInstance(obj));

        obj = new LinkedHashMap<String, Object>();
        Assertions.assertTrue(Clazz.PROPS_MAP.isInstance(obj));
        Assertions.assertTrue(Clazz.ofLinkedHashMap().isInstance(obj));
        Assertions.assertTrue(Clazz.ofMap().isInstance(obj));

        obj = new TreeSet<Integer>();
        Assertions.assertTrue(Clazz.ofTreeSet().isInstance(obj));
        Assertions.assertTrue(Clazz.ofNavigableSet().isInstance(obj));
        Assertions.assertTrue(Clazz.ofSortedSet().isInstance(obj));
        Assertions.assertTrue(Clazz.ofSet().isInstance(obj));
        Assertions.assertTrue(Clazz.ofCollection().isInstance(obj));

        obj = new LinkedList<String>();
        Assertions.assertTrue(Clazz.ofLinkedList().isInstance(obj));
        Assertions.assertTrue(Clazz.ofList().isInstance(obj));
        Assertions.assertTrue(Clazz.ofCollection().isInstance(obj));

        obj = new ConcurrentHashMap<String, Object>();
        Assertions.assertTrue(Clazz.ofConcurrentHashMap().isInstance(obj));
        Assertions.assertTrue(Clazz.ofConcurrentMap().isInstance(obj));
        Assertions.assertTrue(Clazz.ofMap().isInstance(obj));
    }

    @Test
    public void testAllMethodsReturnNonNull() {
        Assertions.assertNotNull(Clazz.of(ArrayList.class));
        Assertions.assertNotNull(Clazz.ofList());
        Assertions.assertNotNull(Clazz.ofList(String.class));
        Assertions.assertNotNull(Clazz.ofLinkedList());
        Assertions.assertNotNull(Clazz.ofLinkedList(String.class));
        Assertions.assertNotNull(Clazz.ofListOfMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofSetOfMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofSet());
        Assertions.assertNotNull(Clazz.ofSet(String.class));
        Assertions.assertNotNull(Clazz.ofLinkedHashSet());
        Assertions.assertNotNull(Clazz.ofLinkedHashSet(String.class));
        Assertions.assertNotNull(Clazz.ofSortedSet());
        Assertions.assertNotNull(Clazz.ofSortedSet(String.class));
        Assertions.assertNotNull(Clazz.ofNavigableSet());
        Assertions.assertNotNull(Clazz.ofNavigableSet(String.class));
        Assertions.assertNotNull(Clazz.ofTreeSet());
        Assertions.assertNotNull(Clazz.ofTreeSet(String.class));
        Assertions.assertNotNull(Clazz.ofQueue());
        Assertions.assertNotNull(Clazz.ofQueue(String.class));
        Assertions.assertNotNull(Clazz.ofDeque());
        Assertions.assertNotNull(Clazz.ofDeque(String.class));
        Assertions.assertNotNull(Clazz.ofArrayDeque());
        Assertions.assertNotNull(Clazz.ofArrayDeque(String.class));
        Assertions.assertNotNull(Clazz.ofConcurrentLinkedQueue());
        Assertions.assertNotNull(Clazz.ofConcurrentLinkedQueue(String.class));
        Assertions.assertNotNull(Clazz.ofPriorityQueue());
        Assertions.assertNotNull(Clazz.ofPriorityQueue(String.class));
        Assertions.assertNotNull(Clazz.ofLinkedBlockingQueue());
        Assertions.assertNotNull(Clazz.ofLinkedBlockingQueue(String.class));
        Assertions.assertNotNull(Clazz.ofCollection());
        Assertions.assertNotNull(Clazz.ofCollection(String.class));
        Assertions.assertNotNull(Clazz.ofMap());
        Assertions.assertNotNull(Clazz.ofMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofLinkedHashMap());
        Assertions.assertNotNull(Clazz.ofLinkedHashMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofSortedMap());
        Assertions.assertNotNull(Clazz.ofSortedMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofNavigableMap());
        Assertions.assertNotNull(Clazz.ofNavigableMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofTreeMap());
        Assertions.assertNotNull(Clazz.ofTreeMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofConcurrentMap());
        Assertions.assertNotNull(Clazz.ofConcurrentMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofConcurrentHashMap());
        Assertions.assertNotNull(Clazz.ofConcurrentHashMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofBiMap());
        Assertions.assertNotNull(Clazz.ofBiMap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofMultiset());
        Assertions.assertNotNull(Clazz.ofMultiset(String.class));
        Assertions.assertNotNull(Clazz.ofListMultimap());
        Assertions.assertNotNull(Clazz.ofListMultimap(String.class, Object.class));
        Assertions.assertNotNull(Clazz.ofSetMultimap());
        Assertions.assertNotNull(Clazz.ofSetMultimap(String.class, Object.class));
    }

    @Test
    public void testInterfaceVsImplementationClasses() {
        Assertions.assertTrue(Clazz.ofList().isInterface());
        Assertions.assertTrue(Clazz.ofSet().isInterface());
        Assertions.assertTrue(Clazz.ofMap().isInterface());
        Assertions.assertTrue(Clazz.ofQueue().isInterface());
        Assertions.assertTrue(Clazz.ofDeque().isInterface());
        Assertions.assertTrue(Clazz.ofCollection().isInterface());
        Assertions.assertTrue(Clazz.ofSortedSet().isInterface());
        Assertions.assertTrue(Clazz.ofNavigableSet().isInterface());
        Assertions.assertTrue(Clazz.ofSortedMap().isInterface());
        Assertions.assertTrue(Clazz.ofNavigableMap().isInterface());
        Assertions.assertTrue(Clazz.ofConcurrentMap().isInterface());

        Assertions.assertFalse(Clazz.ofLinkedList().isInterface());
        Assertions.assertFalse(Clazz.ofLinkedHashSet().isInterface());
        Assertions.assertFalse(Clazz.ofTreeSet().isInterface());
        Assertions.assertFalse(Clazz.ofLinkedHashMap().isInterface());
        Assertions.assertFalse(Clazz.ofTreeMap().isInterface());
        Assertions.assertFalse(Clazz.ofArrayDeque().isInterface());
        Assertions.assertFalse(Clazz.ofConcurrentLinkedQueue().isInterface());
        Assertions.assertFalse(Clazz.ofPriorityQueue().isInterface());
        Assertions.assertFalse(Clazz.ofLinkedBlockingQueue().isInterface());
        Assertions.assertFalse(Clazz.ofConcurrentHashMap().isInterface());
    }
}
