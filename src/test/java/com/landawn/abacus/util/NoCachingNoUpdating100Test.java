package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableBooleanArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableByteArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableCharArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDeque;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDoubleArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableFloatArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableIntArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableLongArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposablePair;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableShortArray;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableTriple;
import com.landawn.abacus.util.NoCachingNoUpdating.Timed;

@Tag("new-test")
public class NoCachingNoUpdating100Test extends TestBase {

    @Test
    public void testDisposableArray_create() {
        DisposableArray<String> array = DisposableArray.create(String.class, 5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableArray_wrap() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));
        assertEquals("c", array.get(2));
    }

    @Test
    public void testDisposableArray_get() {
        String[] arr = { "test1", "test2", "test3" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals("test1", array.get(0));
        assertEquals("test2", array.get(1));
        assertEquals("test3", array.get(2));
    }

    @Test
    public void testDisposableArray_toArray() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        String[] target = new String[3];
        String[] result = array.toArray(target);
        assertArrayEquals(arr, result);

        String[] smallTarget = new String[2];
        String[] smallResult = array.toArray(smallTarget);
        assertEquals(3, smallResult.length);
        assertArrayEquals(arr, smallResult);
    }

    @Test
    public void testDisposableArray_copy() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        String[] copy = array.copy();
        assertArrayEquals(arr, copy);
        assertNotSame(arr, copy);
    }

    @Test
    public void testDisposableArray_toList() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        List<String> list = array.toList();
        assertEquals(Arrays.asList("a", "b", "c"), list);
    }

    @Test
    public void testDisposableArray_toSet() {
        String[] arr = { "a", "b", "c", "a" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        Set<String> set = array.toSet();
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testDisposableArray_toCollection() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        ArrayList<String> collection = array.toCollection(ArrayList::new);
        assertEquals(Arrays.asList("a", "b", "c"), collection);
    }

    @Test
    public void testDisposableArray_forEach() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        List<String> result = new ArrayList<>();
        array.foreach(e -> result.add(e));
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testDisposableArray_apply() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        int length = array.apply(a -> a.length);
        assertEquals(3, length);
    }

    @Test
    public void testDisposableArray_accept() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        final boolean[] called = { false };
        array.accept(a -> called[0] = true);
        assertTrue(called[0]);
    }

    @Test
    public void testDisposableArray_join() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals("a,b,c", array.join(","));
        assertEquals("[a|b|c]", array.join("|", "[", "]"));
    }

    @Test
    public void testDisposableArray_iterator() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        Iterator<String> iter = array.iterator();
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
    }

    @Test
    public void testDisposableObjArray_create() {
        DisposableObjArray array = DisposableObjArray.create(10);
        assertNotNull(array);
        assertEquals(10, array.length());
    }

    @Test
    public void testDisposableObjArray_wrap() {
        Object[] arr = { "a", 1, true };
        DisposableObjArray array = DisposableObjArray.wrap(arr);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals("a", array.get(0));
        assertEquals(1, array.get(1));
        assertEquals(true, array.get(2));
    }

    @Test
    public void testDisposableBooleanArray_create() {
        DisposableBooleanArray array = DisposableBooleanArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableBooleanArray_wrap() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        assertEquals(3, array.length());
        assertTrue(array.get(0));
        assertFalse(array.get(1));
        assertTrue(array.get(2));
    }

    @Test
    public void testDisposableBooleanArray_copy() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        boolean[] copy = array.copy();
        assertArrayEquals(arr, copy);
        assertNotSame(arr, copy);
    }

    @Test
    public void testDisposableBooleanArray_box() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        Boolean[] boxed = array.box();
        assertEquals(3, boxed.length);
        assertEquals(Boolean.TRUE, boxed[0]);
        assertEquals(Boolean.FALSE, boxed[1]);
        assertEquals(Boolean.TRUE, boxed[2]);
    }

    @Test
    public void testDisposableBooleanArray_toList() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        BooleanList list = array.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableBooleanArray_forEach() throws Exception {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        List<Boolean> result = new ArrayList<>();
        array.foreach(result::add);
        assertEquals(Arrays.asList(true, false, true), result);
    }

    @Test
    public void testDisposableBooleanArray_join() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        assertEquals("true,false,true", array.join(","));
        assertEquals("[true|false|true]", array.join("|", "[", "]"));
    }

    @Test
    public void testDisposableCharArray_create() {
        DisposableCharArray array = DisposableCharArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableCharArray_wrap() {
        char[] arr = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals('a', array.get(0));
        assertEquals('b', array.get(1));
        assertEquals('c', array.get(2));
    }

    @Test
    public void testDisposableCharArray_sum() {
        char[] arr = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        int sum = array.sum();
        assertEquals('a' + 'b' + 'c', sum);
    }

    @Test
    public void testDisposableCharArray_average() {
        char[] arr = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        double avg = array.average();
        assertEquals(('a' + 'b' + 'c') / 3.0, avg, 0.001);
    }

    @Test
    public void testDisposableCharArray_minMax() {
        char[] arr = { 'c', 'a', 'b' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals('a', array.min());
        assertEquals('c', array.max());
    }

    @Test
    public void testDisposableByteArray_create() {
        DisposableByteArray array = DisposableByteArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableByteArray_wrap() {
        byte[] arr = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableByteArray_sum() {
        byte[] arr = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(arr);
        assertEquals(6, array.sum());
    }

    @Test
    public void testDisposableByteArray_average() {
        byte[] arr = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(arr);
        assertEquals(2.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableByteArray_minMax() {
        byte[] arr = { 3, 1, 2 };
        DisposableByteArray array = DisposableByteArray.wrap(arr);
        assertEquals(1, array.min());
        assertEquals(3, array.max());
    }

    @Test
    public void testDisposableShortArray_create() {
        DisposableShortArray array = DisposableShortArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableShortArray_wrap() {
        short[] arr = { 10, 20, 30 };
        DisposableShortArray array = DisposableShortArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(10, array.get(0));
        assertEquals(20, array.get(1));
        assertEquals(30, array.get(2));
    }

    @Test
    public void testDisposableShortArray_sum() {
        short[] arr = { 10, 20, 30 };
        DisposableShortArray array = DisposableShortArray.wrap(arr);
        assertEquals(60, array.sum());
    }

    @Test
    public void testDisposableShortArray_average() {
        short[] arr = { 10, 20, 30 };
        DisposableShortArray array = DisposableShortArray.wrap(arr);
        assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableShortArray_minMax() {
        short[] arr = { 30, 10, 20 };
        DisposableShortArray array = DisposableShortArray.wrap(arr);
        assertEquals(10, array.min());
        assertEquals(30, array.max());
    }

    @Test
    public void testDisposableIntArray_create() {
        DisposableIntArray array = DisposableIntArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableIntArray_wrap() {
        int[] arr = { 100, 200, 300 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(100, array.get(0));
        assertEquals(200, array.get(1));
        assertEquals(300, array.get(2));
    }

    @Test
    public void testDisposableIntArray_sum() {
        int[] arr = { 100, 200, 300 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertEquals(600, array.sum());
    }

    @Test
    public void testDisposableIntArray_average() {
        int[] arr = { 100, 200, 300 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertEquals(200.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableIntArray_minMax() {
        int[] arr = { 300, 100, 200 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertEquals(100, array.min());
        assertEquals(300, array.max());
    }

    @Test
    public void testDisposableLongArray_create() {
        DisposableLongArray array = DisposableLongArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableLongArray_wrap() {
        long[] arr = { 1000L, 2000L, 3000L };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(1000L, array.get(0));
        assertEquals(2000L, array.get(1));
        assertEquals(3000L, array.get(2));
    }

    @Test
    public void testDisposableLongArray_sum() {
        long[] arr = { 1000L, 2000L, 3000L };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(6000L, array.sum());
    }

    @Test
    public void testDisposableLongArray_average() {
        long[] arr = { 1000L, 2000L, 3000L };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(2000.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableLongArray_minMax() {
        long[] arr = { 3000L, 1000L, 2000L };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(1000L, array.min());
        assertEquals(3000L, array.max());
    }

    @Test
    public void testDisposableFloatArray_create() {
        DisposableFloatArray array = DisposableFloatArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableFloatArray_wrap() {
        float[] arr = { 1.5f, 2.5f, 3.5f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(1.5f, array.get(0), 0.001f);
        assertEquals(2.5f, array.get(1), 0.001f);
        assertEquals(3.5f, array.get(2), 0.001f);
    }

    @Test
    public void testDisposableFloatArray_sum() {
        float[] arr = { 1.5f, 2.5f, 3.5f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(7.5f, array.sum(), 0.001f);
    }

    @Test
    public void testDisposableFloatArray_average() {
        float[] arr = { 1.5f, 2.5f, 3.5f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(2.5, array.average(), 0.001);
    }

    @Test
    public void testDisposableFloatArray_minMax() {
        float[] arr = { 3.5f, 1.5f, 2.5f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(1.5f, array.min(), 0.001f);
        assertEquals(3.5f, array.max(), 0.001f);
    }

    @Test
    public void testDisposableDoubleArray_create() {
        DisposableDoubleArray array = DisposableDoubleArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableDoubleArray_wrap() {
        double[] arr = { 1.5, 2.5, 3.5 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals(1.5, array.get(0), 0.001);
        assertEquals(2.5, array.get(1), 0.001);
        assertEquals(3.5, array.get(2), 0.001);
    }

    @Test
    public void testDisposableDoubleArray_sum() {
        double[] arr = { 1.5, 2.5, 3.5 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);
        assertEquals(7.5, array.sum(), 0.001);
    }

    @Test
    public void testDisposableDoubleArray_average() {
        double[] arr = { 1.5, 2.5, 3.5 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);
        assertEquals(2.5, array.average(), 0.001);
    }

    @Test
    public void testDisposableDoubleArray_minMax() {
        double[] arr = { 3.5, 1.5, 2.5 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);
        assertEquals(1.5, array.min(), 0.001);
        assertEquals(3.5, array.max(), 0.001);
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
    public void testDisposableDeque_toArray() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("c");

        DisposableDeque<String> deque = DisposableDeque.wrap(original);
        String[] array = deque.toArray(new String[0]);
        assertEquals(3, array.length);
        assertEquals("a", array[0]);
        assertEquals("b", array[1]);
        assertEquals("c", array[2]);
    }

    @Test
    public void testDisposableDeque_toList() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");

        DisposableDeque<String> deque = DisposableDeque.wrap(original);
        List<String> list = deque.toList();
        assertEquals(Arrays.asList("a", "b"), list);
    }

    @Test
    public void testDisposableDeque_toSet() {
        Deque<String> original = new ArrayDeque<>();
        original.add("a");
        original.add("b");
        original.add("a");

        DisposableDeque<String> deque = DisposableDeque.wrap(original);
        Set<String> set = deque.toSet();
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
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
    public void testDisposableEntry_wrap() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        assertEquals("key", entry.getKey());
        assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testDisposableEntry_setValueThrows() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testDisposableEntry_copy() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);
        Map.Entry<String, Integer> copy = entry.copy();

        assertEquals("key", copy.getKey());
        assertEquals(Integer.valueOf(100), copy.getValue());
        assertNotSame(original, copy);
    }

    @Test
    public void testDisposableEntry_apply() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        String result1 = entry.apply(e -> e.getKey() + ":" + e.getValue());
        assertEquals("key:100", result1);

        String result2 = entry.apply((k, v) -> k + "=" + v);
        assertEquals("key=100", result2);
    }

    @Test
    public void testDisposableEntry_accept() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);

        final boolean[] called1 = { false };
        entry.accept(e -> called1[0] = true);
        assertTrue(called1[0]);

        final String[] result = { "" };
        entry.accept((k, v) -> result[0] = k + ":" + v);
        assertEquals("key:100", result[0]);
    }

    @Test
    public void testDisposableEntry_toString() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        DisposableEntry<String, Integer> entry = DisposableEntry.wrap(original);
        assertEquals("key=100", entry.toString());
    }

    @Test
    public void testDisposablePair_wrap() {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);

        assertEquals("left", pair.left());
        assertEquals(Integer.valueOf(100), pair.right());
    }

    @Test
    public void testDisposablePair_copy() {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);
        Pair<String, Integer> copy = pair.copy();

        assertEquals("left", copy.left());
        assertEquals(Integer.valueOf(100), copy.right());
    }

    @Test
    public void testDisposablePair_apply() throws Exception {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);

        String result = pair.apply((l, r) -> l + ":" + r);
        assertEquals("left:100", result);
    }

    @Test
    public void testDisposablePair_accept() throws Exception {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);

        final String[] result = { "" };
        pair.accept((l, r) -> result[0] = l + "=" + r);
        assertEquals("left=100", result[0]);
    }

    @Test
    public void testDisposablePair_toString() {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);
        assertEquals("[left, 100]", pair.toString());
    }

    @Test
    public void testDisposableTriple_wrap() {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(original);

        assertEquals("left", triple.left());
        assertEquals(Integer.valueOf(100), triple.middle());
        assertEquals(Boolean.TRUE, triple.right());
    }

    @Test
    public void testDisposableTriple_copy() {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(original);
        Triple<String, Integer, Boolean> copy = triple.copy();

        assertEquals("left", copy.left());
        assertEquals(Integer.valueOf(100), copy.middle());
        assertEquals(Boolean.TRUE, copy.right());
    }

    @Test
    public void testDisposableTriple_apply() throws Exception {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(original);

        String result = triple.apply((l, m, r) -> l + ":" + m + ":" + r);
        assertEquals("left:100:true", result);
    }

    @Test
    public void testDisposableTriple_accept() throws Exception {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(original);

        final String[] result = { "" };
        triple.accept((l, m, r) -> result[0] = l + "=" + m + "=" + r);
        assertEquals("left=100=true", result[0]);
    }

    @Test
    public void testDisposableTriple_toString() {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        DisposableTriple<String, Integer, Boolean> triple = DisposableTriple.wrap(original);
        assertEquals("[left, 100, true]", triple.toString());
    }

    @Test
    public void testTimed_of() {
        long timestamp = System.currentTimeMillis();
        Timed<String> timed = Timed.of("value", timestamp);

        assertEquals("value", timed.value());
        assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimed_hashCode() {
        long timestamp = 1000L;
        Timed<String> timed1 = Timed.of("value", timestamp);
        Timed<String> timed2 = Timed.of("value", timestamp);

        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimed_equals() {
        long timestamp = 1000L;
        Timed<String> timed1 = Timed.of("value", timestamp);
        Timed<String> timed2 = Timed.of("value", timestamp);
        Timed<String> timed3 = Timed.of("different", timestamp);
        Timed<String> timed4 = Timed.of("value", 2000L);

        assertTrue(timed1.equals(timed1));
        assertTrue(timed1.equals(timed2));
        assertFalse(timed1.equals(timed3));
        assertFalse(timed1.equals(timed4));
        assertFalse(timed1.equals(null));
        assertFalse(timed1.equals("not a timed"));
    }

    @Test
    public void testTimed_toString() {
        Timed<String> timed = Timed.of("value", 1000L);
        assertEquals("1000: value", timed.toString());
    }

    @Test
    public void testTimed_withNullValue() {
        Timed<String> timed = Timed.of(null, 1000L);
        assertNull(timed.value());
        assertEquals(1000L, timed.timestamp());

        Timed<String> timed2 = Timed.of(null, 1000L);
        assertTrue(timed.equals(timed2));
    }
}
