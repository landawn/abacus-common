package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ReverseListTest extends TestBase {
    @Test
    void bulkTraversalsAvoidIndexedAccessOnSequentialLists() {
        CountingLinkedList<Integer> backing = new CountingLinkedList<>();
        for (int i = 0; i < 1000; i++) {
            backing.add(i);
        }
        List<Integer> expected = new ArrayList<>(backing);
        Collections.reverse(expected);
        List<Integer> reversed = ImmutableList.wrap(backing).reversed();
        assertEquals(expected, new ArrayList<>(reversed));
        assertEquals(expected, reversed);
        assertEquals(reversed, expected);
        assertEquals(expected.hashCode(), reversed.hashCode());
        assertEquals(expected.toString(), reversed.toString());
        assertArrayEquals(expected.toArray(), reversed.toArray());
        assertArrayEquals(expected.toArray(Integer[]::new), reversed.toArray(Integer[]::new));
        assertEquals(expected, reversed.stream().toList());
        List<Integer> visited = new ArrayList<>();
        reversed.forEach(visited::add);
        assertEquals(expected, visited);
        ListIterator<Integer> iter = reversed.listIterator(500);
        assertEquals(499, iter.next());
        assertEquals(499, iter.previous());
        assertEquals(expected.subList(10, 20), reversed.subList(10, 20));
        assertEquals(0, backing.getCalls, "Traversal must use backing iterators, not indexed node walks");
    }

    @Test
    void bidirectionalCursorAndFailureBehaviorMatchAListIterator() {
        List<String> values = Arrays.asList("a", null, "\u4e2d\ud83d\ude00", "a");
        List<String> expected = new ArrayList<>(values);
        Collections.reverse(expected);
        List<String> reversed = ImmutableList.wrap(new LinkedList<>(values)).reversed();
        for (int start = 0; start <= values.size(); start++) {
            ListIterator<String> actual = reversed.listIterator(start);
            ListIterator<String> oracle = expected.listIterator(start);
            while (oracle.hasNext()) {
                assertEquals(oracle.nextIndex(), actual.nextIndex());
                assertEquals(oracle.previousIndex(), actual.previousIndex());
                assertEquals(oracle.next(), actual.next());
            }
            assertFalse(actual.hasNext());
            assertThrows(NoSuchElementException.class, actual::next);
            assertEquals(values.size(), actual.nextIndex());
            while (oracle.hasPrevious()) {
                assertEquals(oracle.previous(), actual.previous());
            }
            assertFalse(actual.hasPrevious());
            assertThrows(NoSuchElementException.class, actual::previous);
            assertEquals(0, actual.nextIndex());
            assertEquals(-1, actual.previousIndex());
            assertThrows(UnsupportedOperationException.class, actual::remove);
            assertThrows(UnsupportedOperationException.class, () -> actual.add("x"));
            assertThrows(UnsupportedOperationException.class, () -> actual.set("x"));
        }
        assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> reversed.listIterator(5));
        assertThrows(NullPointerException.class, () -> reversed.forEach(null));
    }

    @Test
    void keepsBackingIteratorModificationPolicyAndLiveView() {
        LinkedList<Integer> backing = new LinkedList<>(List.of(1, 2, 3));
        List<Integer> reversed = ImmutableList.wrap(backing).reversed();
        Iterator<Integer> iter = reversed.iterator();
        assertEquals(3, iter.next());
        backing.add(4);
        assertThrows(ConcurrentModificationException.class, iter::next);
        assertEquals(List.of(4, 3, 2, 1), reversed);

        CopyOnWriteArrayList<Integer> snapshotSource = new CopyOnWriteArrayList<>(List.of(1, 2, 3));
        List<Integer> snapshotView = ImmutableList.wrap(snapshotSource).reversed();
        Iterator<Integer> snapshot = snapshotView.iterator();
        snapshotSource.add(4);
        List<Integer> visited = new ArrayList<>();
        snapshot.forEachRemaining(visited::add);
        assertEquals(List.of(3, 2, 1), visited);
        assertEquals(List.of(4, 3, 2, 1), snapshotView);
    }

    @Test
    void typedArraysPreserveAllocationReuseSentinelAndStoreChecks() {
        List<String> reversed = ImmutableList.wrap(new LinkedList<>(Arrays.asList("a", null, "\u4e2d"))).reversed();
        String[] oversized = { "old", "old", "old", "sentinel", "tail" };
        assertSame(oversized, reversed.toArray(oversized));
        assertArrayEquals(new String[] { "\u4e2d", null, "a", null, "tail" }, oversized);
        assertArrayEquals(new String[] { "\u4e2d", null, "a" }, reversed.toArray(new String[0]));
        assertThrows(ArrayStoreException.class, () -> reversed.toArray(new Integer[3]));
        assertThrows(NullPointerException.class, () -> reversed.toArray((String[]) null));
        List<String> empty = ImmutableList.wrap(new LinkedList<String>()).reversed();
        String[] reused = { "sentinel", "tail" };
        assertSame(reused, empty.toArray(reused));
        assertArrayEquals(new String[] { null, "tail" }, reused);
        assertEquals("[]", empty.toString());
        assertThrows(NoSuchElementException.class, empty.iterator()::next);
    }

    private static final class CountingLinkedList<E> extends LinkedList<E> {
        private int getCalls;

        @Override
        public E get(int index) {
            getCalls++;
            return super.get(index);
        }
    }
}
