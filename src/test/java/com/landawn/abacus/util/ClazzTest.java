package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ClazzTest extends TestBase {

    @Test
    public void testOf() {
        assertEquals(ArrayList.class, Clazz.of(ArrayList.class));
        assertEquals(HashMap.class, Clazz.of(HashMap.class));
        assertEquals(TreeSet.class, Clazz.of(TreeSet.class));
        assertEquals(LinkedList.class, Clazz.of(LinkedList.class));

        String json = "[1, 2, 3]";
        List<String> list = N.fromJson(json, Clazz.of(List.class));
        assertEquals(3, list.size());
        ListMultimap multimap = N.fromJson("{\"a\":[1,2,3]}", Clazz.of(ListMultimap.class));
        assertEquals(3, ((List<?>) multimap.get("a")).size());
    }

    @Test
    public void testFactoryMethods() {
        assertEquals(List.class, Clazz.ofList());
        assertEquals(List.class, Clazz.ofList(String.class));
        assertEquals(LinkedList.class, Clazz.ofLinkedList());
        assertEquals(LinkedList.class, Clazz.ofLinkedList(Integer.class));
        assertEquals(List.class, Clazz.ofListOfMap(String.class, Object.class));
        assertEquals(Set.class, Clazz.ofSetOfMap(String.class, Integer.class));
        assertEquals(Set.class, Clazz.ofSet());
        assertEquals(Set.class, Clazz.ofSet(Long.class));
        assertEquals(LinkedHashSet.class, Clazz.ofLinkedHashSet());
        assertEquals(LinkedHashSet.class, Clazz.ofLinkedHashSet(Integer.class));
        assertEquals(SortedSet.class, Clazz.ofSortedSet());
        assertEquals(SortedSet.class, Clazz.ofSortedSet(String.class));
        assertEquals(NavigableSet.class, Clazz.ofNavigableSet());
        assertEquals(NavigableSet.class, Clazz.ofNavigableSet(Double.class));
        assertEquals(TreeSet.class, Clazz.ofTreeSet());
        assertEquals(TreeSet.class, Clazz.ofTreeSet(Integer.class));
        assertEquals(Queue.class, Clazz.ofQueue());
        assertEquals(Queue.class, Clazz.ofQueue(Object.class));
        assertEquals(Deque.class, Clazz.ofDeque());
        assertEquals(Deque.class, Clazz.ofDeque(Integer.class));
        assertEquals(ArrayDeque.class, Clazz.ofArrayDeque());
        assertEquals(ArrayDeque.class, Clazz.ofArrayDeque(Long.class));
        assertEquals(ConcurrentLinkedQueue.class, Clazz.ofConcurrentLinkedQueue());
        assertEquals(ConcurrentLinkedQueue.class, Clazz.ofConcurrentLinkedQueue(Integer.class));
        assertEquals(PriorityQueue.class, Clazz.ofPriorityQueue());
        assertEquals(PriorityQueue.class, Clazz.ofPriorityQueue(Integer.class));
        assertEquals(LinkedBlockingQueue.class, Clazz.ofLinkedBlockingQueue());
        assertEquals(LinkedBlockingQueue.class, Clazz.ofLinkedBlockingQueue(Object.class));
        assertEquals(Collection.class, Clazz.ofCollection());
        assertEquals(Collection.class, Clazz.ofCollection(Integer.class));
        assertEquals(Map.class, Clazz.ofMap());
        assertEquals(Map.class, Clazz.ofMap(String.class, Integer.class));
        assertEquals(LinkedHashMap.class, Clazz.ofLinkedHashMap());
        assertEquals(LinkedHashMap.class, Clazz.ofLinkedHashMap(Integer.class, String.class));
        assertEquals(SortedMap.class, Clazz.ofSortedMap());
        assertEquals(SortedMap.class, Clazz.ofSortedMap(Integer.class, String.class));
        assertEquals(NavigableMap.class, Clazz.ofNavigableMap());
        assertEquals(NavigableMap.class, Clazz.ofNavigableMap(Long.class, Double.class));
        assertEquals(TreeMap.class, Clazz.ofTreeMap());
        assertEquals(TreeMap.class, Clazz.ofTreeMap(Integer.class, String.class));
        assertEquals(ConcurrentMap.class, Clazz.ofConcurrentMap());
        assertEquals(ConcurrentMap.class, Clazz.ofConcurrentMap(Long.class, String.class));
        assertEquals(ConcurrentHashMap.class, Clazz.ofConcurrentHashMap());
        assertEquals(ConcurrentHashMap.class, Clazz.ofConcurrentHashMap(Integer.class, Double.class));
        Class<ConcurrentHashMap<String, AtomicInteger>> counterMapClass = Clazz.ofConcurrentHashMap(String.class, AtomicInteger.class);
        assertEquals(ConcurrentHashMap.class, counterMapClass);
        assertEquals(BiMap.class, Clazz.ofBiMap());
        assertEquals(BiMap.class, Clazz.ofBiMap(Long.class, String.class));
        assertEquals(Multiset.class, Clazz.ofMultiset());
        assertEquals(Multiset.class, Clazz.ofMultiset(Integer.class));
        assertEquals(ListMultimap.class, Clazz.ofListMultimap());
        assertEquals(ListMultimap.class, Clazz.ofListMultimap(Long.class, String.class));
        assertEquals(SetMultimap.class, Clazz.ofSetMultimap());
        assertEquals(SetMultimap.class, Clazz.ofSetMultimap(Integer.class, String.class));
    }

    @Test
    public void testTypeErasureAndInstanceOf() {
        assertEquals(Clazz.ofList(String.class), Clazz.ofList(Integer.class));
        assertEquals(List.class, Clazz.ofList(String.class));
        assertEquals(Clazz.ofMap(String.class, Integer.class), Clazz.ofMap(Long.class, Double.class));
        assertEquals(Map.class, Clazz.ofMap(String.class, Integer.class));

        assertTrue(Clazz.ofList().isInstance(new ArrayList<>()));
        assertTrue(Clazz.ofCollection().isInstance(new ArrayList<>()));
        assertTrue(Clazz.PROPS_MAP.isInstance(new LinkedHashMap<>()));
        assertTrue(Clazz.ofLinkedHashMap().isInstance(new LinkedHashMap<>()));
        assertTrue(Clazz.ofTreeSet().isInstance(new TreeSet<>()));
        assertTrue(Clazz.ofNavigableSet().isInstance(new TreeSet<>()));
        assertTrue(Clazz.ofLinkedList().isInstance(new LinkedList<>()));
        assertTrue(Clazz.ofConcurrentHashMap().isInstance(new ConcurrentHashMap<>()));
        assertTrue(Clazz.ofConcurrentMap().isInstance(new ConcurrentHashMap<>()));

        assertTrue(Clazz.ofList().isInterface());
        assertTrue(Clazz.ofSet().isInterface());
        assertTrue(Clazz.ofMap().isInterface());
        assertTrue(Clazz.ofQueue().isInterface());
        assertTrue(Clazz.ofDeque().isInterface());
        assertTrue(Clazz.ofCollection().isInterface());
        assertTrue(Clazz.ofSortedSet().isInterface());
        assertTrue(Clazz.ofNavigableSet().isInterface());
        assertTrue(Clazz.ofSortedMap().isInterface());
        assertTrue(Clazz.ofNavigableMap().isInterface());
        assertTrue(Clazz.ofConcurrentMap().isInterface());
        assertFalse(Clazz.ofLinkedList().isInterface());
        assertFalse(Clazz.ofLinkedHashSet().isInterface());
        assertFalse(Clazz.ofTreeSet().isInterface());
        assertFalse(Clazz.ofLinkedHashMap().isInterface());
        assertFalse(Clazz.ofTreeMap().isInterface());
        assertFalse(Clazz.ofArrayDeque().isInterface());
        assertFalse(Clazz.ofConcurrentLinkedQueue().isInterface());
        assertFalse(Clazz.ofPriorityQueue().isInterface());
        assertFalse(Clazz.ofLinkedBlockingQueue().isInterface());
        assertFalse(Clazz.ofConcurrentHashMap().isInterface());
    }

    @Test
    public void testConstants() {
        assertEquals(LinkedHashMap.class, Clazz.PROPS_MAP);
        assertEquals(Map.class, Clazz.MAP);
        assertEquals(LinkedHashMap.class, Clazz.LINKED_HASH_MAP);
        assertEquals(List.class, Clazz.STRING_LIST);
        assertEquals(List.class, Clazz.INTEGER_LIST);
        assertEquals(List.class, Clazz.LONG_LIST);
        assertEquals(List.class, Clazz.DOUBLE_LIST);
        assertEquals(List.class, Clazz.OBJECT_LIST);
        assertEquals(Set.class, Clazz.STRING_SET);
        assertEquals(Set.class, Clazz.INTEGER_SET);
        assertEquals(Set.class, Clazz.LONG_SET);
        assertEquals(Set.class, Clazz.DOUBLE_SET);
        assertEquals(Set.class, Clazz.OBJECT_SET);
        assertNotNull(Clazz.PROPS_MAP);
    }

    @Test
    public void testPrivateConstructor() {
        try {
            Constructor<Clazz> constructor = Clazz.class.getDeclaredConstructor();
            assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        } catch (NoSuchMethodException e) {
            fail("Should have a private constructor");
        }
    }
}
