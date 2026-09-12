package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDeque;

public class NoCachingNoUpdatingDisposableDequeTest extends NoCachingNoUpdatingTestSupport {
    @Test
    public void testDisposableDeque_create_normal() {
        DisposableDeque<String> deque = DisposableDeque.create(5);
        assertNotNull(deque);
        assertEquals(0, deque.size());
    }

    @Test
    public void testDisposableDeque_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableDeque.create(-1);
        });
    }

    @Test
    public void testDisposableDeque_wrap_normal() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        data.add("c");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertNotNull(deque);
        assertEquals(3, deque.size());
    }

    @Test
    public void testDisposableDeque_wrap_nullDeque() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableDeque.wrap(null);
        });
    }

    @Test
    public void testDisposableDeque_size() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertEquals(2, deque.size());
    }

    @Test
    public void testDisposableDeque_getFirst() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertEquals("a", deque.getFirst());
    }

    @Test
    public void testDisposableDeque_getFirst_empty() {
        Deque<String> data = new ArrayDeque<>();
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertThrows(NoSuchElementException.class, deque::getFirst);
    }

    @Test
    public void testDisposableDeque_getLast() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertEquals("b", deque.getLast());
    }

    @Test
    public void testDisposableDeque_getLast_empty() {
        Deque<String> data = new ArrayDeque<>();
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        assertThrows(NoSuchElementException.class, deque::getLast);
    }

    @Test
    public void testDisposableDeque_toArray() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        String[] result = deque.toArray(new String[0]);
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    public void testDisposableDeque_toList() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        List<String> list = deque.toList();
        assertNotNull(list);
        assertEquals(2, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
    }

    @Test
    public void testDisposableDeque_toSet() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        data.add("a");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        Set<String> set = deque.toSet();
        assertNotNull(set);
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
    }

    @Test
    public void testDisposableDeque_toCollection() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        LinkedList<String> result = deque.toCollection(len -> new LinkedList<>());
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void testDisposableDeque_foreach() throws Exception {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        List<String> collected = new ArrayList<>();
        deque.foreach(collected::add);
        assertEquals(2, collected.size());
    }

    @Test
    public void testDisposableDeque_apply() throws Exception {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        Integer result = deque.apply(Deque::size);
        assertEquals(2, result);
    }

    @Test
    public void testDisposableDeque_accept() throws Exception {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        deque.accept(d -> count.set(d.size()));
        assertEquals(2, count.get());
    }

    @Test
    public void testDisposableDeque_join_delimiter() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        String result = deque.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableDeque_join_delimiterPrefixSuffix() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        String result = deque.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableDeque_toString() {
        Deque<String> data = new ArrayDeque<>();
        data.add("a");
        data.add("b");
        DisposableDeque<String> deque = DisposableDeque.wrap(data);
        String result = deque.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableDequeCreate() {
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.create(10);
        Assertions.assertNotNull(deque);
        Assertions.assertEquals(0, deque.size());
    }

    @Test
    public void testDisposableDequeWrap() {
        Deque<String> original = new ArrayDeque<>();
        original.add("first");
        original.add("second");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertNotNull(deque);
        Assertions.assertEquals(2, deque.size());
    }

    @Test
    public void testDisposableDequeSize() {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(1);
        original.add(2);
        original.add(3);
        NoCachingNoUpdating.DisposableDeque<Integer> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertEquals(3, deque.size());
    }

    @Test
    public void testDisposableDequeGetFirst() {
        Deque<String> original = new ArrayDeque<>();
        original.add("first");
        original.add("second");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertEquals("first", deque.getFirst());
    }

    @Test
    public void testDisposableDequeGetLast() {
        Deque<String> original = new ArrayDeque<>();
        original.add("first");
        original.add("second");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertEquals("second", deque.getLast());
    }

    @Test
    public void testDisposableDequeGetFirstEmpty() {
        Deque<String> original = new ArrayDeque<>();
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertThrows(NoSuchElementException.class, () -> deque.getFirst());
    }

    @Test
    public void testDisposableDequeGetLastEmpty() {
        Deque<String> original = new ArrayDeque<>();
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertThrows(NoSuchElementException.class, () -> deque.getLast());
    }

    @Test
    public void testDisposableDequeToArray() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("c");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);

        String[] small = new String[1];
        String[] result1 = deque.toArray(small);
        Assertions.assertEquals(3, result1.length);

        String[] large = new String[5];
        String[] result2 = deque.toArray(large);
        Assertions.assertSame(large, result2);
    }

    @Test
    public void testDisposableDequeToList() {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(1);
        original.add(2);
        original.add(3);
        NoCachingNoUpdating.DisposableDeque<Integer> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        List<Integer> list = deque.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(Integer.valueOf(1), list.get(0));
        Assertions.assertEquals(Integer.valueOf(2), list.get(1));
        Assertions.assertEquals(Integer.valueOf(3), list.get(2));
    }

    @Test
    public void testDisposableDequeToSet() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("a");
        original.add("c");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Set<String> set = deque.toSet();
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
        Assertions.assertTrue(set.contains("c"));
    }

    @Test
    public void testDisposableDequeToCollection() {
        Deque<String> original = new ArrayDeque<>();
        original.add("one");
        original.add("two");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        LinkedList<String> linkedList = deque.toCollection(IntFunctions.ofLinkedList());
        Assertions.assertEquals(2, linkedList.size());
        Assertions.assertEquals("one", linkedList.get(0));
        Assertions.assertEquals("two", linkedList.get(1));
    }

    @Test
    public void testDisposableDequeForeach() throws Exception {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("c");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        List<String> collected = new ArrayList<>();
        deque.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testDisposableDequeApply() throws Exception {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(1);
        original.add(2);
        original.add(3);
        NoCachingNoUpdating.DisposableDeque<Integer> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        int size = deque.apply(Deque::size);
        Assertions.assertEquals(3, size);
    }

    @Test
    public void testDisposableDequeAccept() throws Exception {
        Deque<String> original = new ArrayDeque<>();
        original.add("test");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        boolean[] called = { false };
        deque.accept(d -> {
            called[0] = true;
            Assertions.assertEquals(1, d.size());
            Assertions.assertEquals("test", d.getFirst());
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableDequeJoin() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("c");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertEquals("a, b, c", deque.join(", "));
    }

    @Test
    public void testDisposableDequeJoinWithPrefixSuffix() {
        Deque<String> original = new ArrayDeque<>();
        original.add("x");
        original.add("y");
        NoCachingNoUpdating.DisposableDeque<String> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        Assertions.assertEquals("[x, y]", deque.join(", ", "[", "]"));
    }

    @Test
    public void testDisposableDequeToString() {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(10);
        original.add(20);
        NoCachingNoUpdating.DisposableDeque<Integer> deque = NoCachingNoUpdating.DisposableDeque.wrap(original);
        String str = deque.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("10"));
        Assertions.assertTrue(str.contains("20"));
    }

    @Test
    public void testDisposableDeque_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableDeque.wrap(null));
    }

    @Test
    public void testDisposableDeque_getFirstOnEmpty() {
        DisposableDeque<String> deque = DisposableDeque.create(10);
        assertThrows(NoSuchElementException.class, () -> deque.getFirst());
    }

    @Test
    public void testDisposableDeque_getLastOnEmpty() {
        DisposableDeque<String> deque = DisposableDeque.create(10);
        assertThrows(NoSuchElementException.class, () -> deque.getLast());
    }

    @Test
    public void testDisposableDeque_functionalOperations() throws Exception {
        Deque<Integer> original = new ArrayDeque<>();
        original.add(1);
        original.add(2);
        original.add(3);

        DisposableDeque<Integer> deque = DisposableDeque.wrap(original);

        int sum = deque.apply(d -> {
            int total = 0;
            for (int n : d) {
                total += n;
            }
            return total;
        });
        assertEquals(6, sum);

        final int[] count = { 0 };
        deque.accept(d -> count[0] = d.size());
        assertEquals(3, count[0]);

        List<Integer> collected = new ArrayList<>();
        deque.foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testDisposableDeque_create() {
        DisposableDeque<String> deque = DisposableDeque.create(10);
        assertNotNull(deque);
        assertEquals(0, deque.size());
    }

    @Test
    public void testDisposableDeque_wrap() {
        Deque<String> original = new ArrayDeque<>();
        original.add("first");
        original.add("second");
        original.add("third");

        DisposableDeque<String> deque = DisposableDeque.wrap(original);
        assertEquals(3, deque.size());
        assertEquals("first", deque.getFirst());
        assertEquals("third", deque.getLast());
    }

    @Test
    public void testDisposableDeque_join() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("c");

        DisposableDeque<String> deque = DisposableDeque.wrap(original);
        assertEquals("a,b,c", deque.join(","));
        assertEquals("[a|b|c]", deque.join("|", "[", "]"));
    }

    @Test
    public void testDisposableDequeToArray_overlongPreFilledTargetKeepsTheTailAfterTheSentinel() {
        final Deque<String> data = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        final DisposableDeque<String> deque = DisposableDeque.wrap(data);

        final String[] preFilled = { "P", "Q", "R", "S", "T" };
        final String[] result = deque.toArray(preFilled);
        assertSame(preFilled, result);
        assertEquals(5, result.length);
        assertArrayEquals(new String[] { "a", "b", "c", null, "T" }, result);

        assertArrayEquals(new String[] { "a", "b", "c", null, null }, deque.toArray(new String[5]));
        assertArrayEquals(new String[] { "a", "b", "c" }, deque.toArray(new String[0]));
    }
}
