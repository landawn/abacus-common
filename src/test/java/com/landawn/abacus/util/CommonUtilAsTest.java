package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;

public class CommonUtilAsTest extends CommonUtilTestSupport {

    @Test
    public void testAsListSetMapArray() {
        List<String> list = CommonUtil.asList("a", "b", "c");
        assertTrue(list instanceof ImmutableList);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertNull(CommonUtil.asList("a", null, "c").get(1));
        assertTrue(CommonUtil.asList().isEmpty());
        assertTrue(CommonUtil.asList((String[]) null).isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> list.add("d"));

        Set<String> set = CommonUtil.asSet("a", "b", "c", "a");
        assertTrue(set instanceof ImmutableSet);
        assertEquals(3, set.size());
        assertTrue(CommonUtil.asSet("a", null, "a").contains(null));
        assertTrue(CommonUtil.asSet().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> set.add("d"));

        Map<String, Integer> map = CommonUtil.asMap("a", 1, "b", 2, "c", 3, "d", 4, "e", 5, "f", 6, "g", 7, "h", 8, "i", 9);
        assertTrue(map instanceof ImmutableMap);
        assertEquals(9, map.size());
        assertEquals(Integer.valueOf(9), map.get("i"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("j", 10));

        String[] arr = CommonUtil.asArray("a", "b", "c");
        assertEquals(3, arr.length);
        assertEquals("a", arr[0]);
        assertEquals("c", arr[2]);
    }

    @Test
    public void testAsSingleton() {
        List<String> list = CommonUtil.asSingletonList("test");
        assertEquals(1, list.size());
        assertEquals("test", list.get(0));
        assertNull(CommonUtil.asSingletonList(null).get(0));
        assertEquals(42, CommonUtil.asSingletonList(42).get(0));

        Set<String> set = CommonUtil.asSingletonSet("test");
        assertEquals(1, set.size());
        assertTrue(set.contains("test"));
        assertTrue(CommonUtil.asSingletonSet(null).contains(null));
        assertTrue(CommonUtil.asSingletonSet(100).contains(100));

        Map<String, Integer> map = CommonUtil.asSingletonMap("key", 100);
        assertEquals(Integer.valueOf(100), map.get("key"));
        assertEquals("value", CommonUtil.asSingletonMap(null, "value").get(null));
        assertNull(CommonUtil.asSingletonMap("key", null).get("key"));
        assertTrue(CommonUtil.asSingletonMap(null, null).containsKey(null));
    }

    @Test
    public void testToCollectionsAndQueues() {
        LinkedList<String> linked = CommonUtil.toLinkedList("a", "b", "c");
        assertEquals(3, linked.size());
        assertEquals("a", linked.getFirst());
        assertTrue(CommonUtil.toLinkedList().isEmpty());
        assertEquals(7, CommonUtil.toLinkedList("a", "b", "c", "d", "e", "f", "g").size());

        Set<String> linkedSet = CommonUtil.toLinkedHashSet("a", "b", "c", "d", "e", "f", "g");
        assertTrue(linkedSet instanceof LinkedHashSet);
        assertEquals(7, linkedSet.size());
        Iterator<String> iter = linkedSet.iterator();
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());

        SortedSet<Integer> sorted = CommonUtil.toSortedSet(3, 1, 2);
        assertEquals(Integer.valueOf(1), sorted.first());
        assertEquals(Integer.valueOf(3), sorted.last());

        NavigableSet<Integer> navigable = CommonUtil.toNavigableSet(3, 1, 2);
        assertEquals(Integer.valueOf(1), navigable.first());

        Queue<String> queue = CommonUtil.toQueue("a", "b", "c");
        assertEquals("a", queue.peek());

        LinkedBlockingQueue<String> lbq = CommonUtil.toLinkedBlockingQueue("a", "b");
        assertTrue(lbq.offer("c"));
        assertEquals(3, lbq.size());

        ConcurrentLinkedQueue<String> clq = CommonUtil.toConcurrentLinkedQueue("a", "b");
        assertEquals("a", clq.peek());

        DelayQueue<Delayed> delay = CommonUtil.toDelayQueue();
        assertNotNull(delay);
        assertTrue(delay.isEmpty());

        PriorityQueue<Integer> pq = CommonUtil.toPriorityQueue(3, 1, 2);
        assertEquals(Integer.valueOf(1), pq.poll());
        assertEquals(Integer.valueOf(2), pq.poll());
        assertEquals(Integer.valueOf(3), pq.poll());

        Deque<String> deque = CommonUtil.toDeque("a", "b", "c");
        assertEquals("a", deque.getFirst());
        assertEquals("c", deque.getLast());

        LinkedBlockingDeque<String> lbd = CommonUtil.toLinkedBlockingDeque("a", "b");
        assertTrue(lbd.offerLast("c"));
        assertEquals(3, lbd.size());

        ConcurrentLinkedDeque<String> cld = CommonUtil.toConcurrentLinkedDeque("a", "b");
        assertEquals("a", cld.getFirst());

        ArrayDeque<String> ad = CommonUtil.toArrayDeque("a", "b", "c");
        assertEquals("a", ad.getFirst());
        assertEquals("c", ad.getLast());

        ArrayBlockingQueue<String> abq = CommonUtil.toArrayBlockingQueue("a", "b");
        assertEquals(0, abq.remainingCapacity());
        assertFalse(abq.offer("c"));

        Multiset<String> multiset = CommonUtil.toMultiset("a", "b", "a", "c");
        assertEquals(4, multiset.size());
        assertEquals(2, multiset.count("a"));
    }
    private static <T> T[] pairViaAsArray(final T a, final T b) {
        return CommonUtil.asArray(a, b);
    }

    private static <T> Object pairRuntimeArray(final T a, final T b) {
        return CommonUtil.asArray(a, b);
    }

    private static <T extends Number> Object boundedRuntimeArray(final T a, final T b) {
        return CommonUtil.asArray(a, b);
    }

    @Test
    public void testAsArray_returnsTheCallSiteVarargsArray() {
        assertEquals(String[].class, CommonUtil.asArray("x", "y").getClass());

        final Object raw = pairRuntimeArray("x", "y");
        assertEquals(Object[].class, raw.getClass());
        assertEquals(Number[].class, boundedRuntimeArray(Integer.valueOf(1), Integer.valueOf(2)).getClass());

        assertThrows(ClassCastException.class, () -> {
            final String[] s = pairViaAsArray("x", "y");
            assertNotNull(s);
        });

        final Object copied = CommonUtil.copyOf((Object[]) raw, 2);
        assertEquals(Object[].class, copied.getClass());

        final String[] repaired = CommonUtil.copyOf((Object[]) raw, 2, String[].class);
        assertEquals(String[].class, repaired.getClass());
        assertEquals("x", repaired[0]);
    }

}
