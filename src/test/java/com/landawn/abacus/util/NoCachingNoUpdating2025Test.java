package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

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

@Tag("2025")
public class NoCachingNoUpdating2025Test extends TestBase {

    @Test
    public void testDisposableArray_create_normal() {
        DisposableArray<String> array = DisposableArray.create(String.class, 5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableArray_create_zeroLength() {
        DisposableArray<String> array = DisposableArray.create(String.class, 0);
        assertNotNull(array);
        assertEquals(0, array.length());
    }

    @Test
    public void testDisposableArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableArray.create(String.class, -1);
        });
    }

    @Test
    public void testDisposableArray_wrap_normal() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));
        assertEquals("c", array.get(2));
    }

    @Test
    public void testDisposableArray_wrap_emptyArray() {
        String[] data = {};
        DisposableArray<String> array = DisposableArray.wrap(data);
        assertNotNull(array);
        assertEquals(0, array.length());
    }

    @Test
    public void testDisposableArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableArray.wrap(null);
        });
    }

    @Test
    public void testDisposableArray_get_validIndex() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        assertEquals("a", array.get(0));
        assertEquals("b", array.get(1));
        assertEquals("c", array.get(2));
    }

    @Test
    public void testDisposableArray_get_invalidIndex() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(3));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(-1));
    }

    @Test
    public void testDisposableArray_length() {
        assertEquals(0, DisposableArray.wrap(new String[0]).length());
        assertEquals(1, DisposableArray.wrap(new String[1]).length());
        assertEquals(10, DisposableArray.wrap(new String[10]).length());
    }

    @Test
    public void testDisposableArray_toArray_sufficientSize() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String[] target = new String[5];
        String[] result = array.toArray(target);
        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    public void testDisposableArray_toArray_insufficientSize() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String[] target = new String[1];
        String[] result = array.toArray(target);
        assertNotSame(target, result);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    public void testDisposableArray_toArray_nullTarget() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        assertThrows(IllegalArgumentException.class, () -> array.toArray(null));
    }

    @Test
    public void testDisposableArray_copy() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableArray_copy_emptyArray() {
        String[] data = {};
        DisposableArray<String> array = DisposableArray.wrap(data);
        String[] copy = array.copy();
        assertNotSame(data, copy);
        assertEquals(0, copy.length);
    }

    @Test
    public void testDisposableArray_toList() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        List<String> list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
        assertEquals("a", list.get(0));
        assertEquals("b", list.get(1));
        assertEquals("c", list.get(2));
    }

    @Test
    public void testDisposableArray_toList_empty() {
        String[] data = {};
        DisposableArray<String> array = DisposableArray.wrap(data);
        List<String> list = array.toList();
        assertNotNull(list);
        assertEquals(0, list.size());
    }

    @Test
    public void testDisposableArray_toSet() {
        String[] data = { "a", "b", "c", "a" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        Set<String> set = array.toSet();
        assertNotNull(set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));
    }

    @Test
    public void testDisposableArray_toSet_empty() {
        String[] data = {};
        DisposableArray<String> array = DisposableArray.wrap(data);
        Set<String> set = array.toSet();
        assertNotNull(set);
        assertEquals(0, set.size());
    }

    @Test
    public void testDisposableArray_toCollection() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        LinkedList<String> result = array.toCollection(len -> new LinkedList<>());
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testDisposableArray_foreach() throws Exception {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        List<String> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
        assertEquals("a", collected.get(0));
        assertEquals("b", collected.get(1));
        assertEquals("c", collected.get(2));
    }

    @Test
    public void testDisposableArray_foreach_empty() throws Exception {
        String[] data = {};
        DisposableArray<String> array = DisposableArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.foreach(e -> count.incrementAndGet());
        assertEquals(0, count.get());
    }

    @Test
    public void testDisposableArray_apply() throws Exception {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableArray_accept() throws Exception {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableArray_join_delimiter() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String result = array.join(", ");
        assertEquals("a, b, c", result);
    }

    @Test
    public void testDisposableArray_join_delimiterPrefixSuffix() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertEquals("[a, b, c]", result);
    }

    @Test
    public void testDisposableArray_iterator() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        List<String> collected = new ArrayList<>();
        for (String s : array) {
            collected.add(s);
        }
        assertEquals(3, collected.size());
        assertEquals("a", collected.get(0));
        assertEquals("b", collected.get(1));
        assertEquals("c", collected.get(2));
    }

    @Test
    public void testDisposableArray_toString() {
        String[] data = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
        assertTrue(result.contains("a"));
    }

    @Test
    public void testDisposableObjArray_create_normal() {
        DisposableObjArray array = DisposableObjArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableObjArray_create_zeroLength() {
        DisposableObjArray array = DisposableObjArray.create(0);
        assertNotNull(array);
        assertEquals(0, array.length());
    }

    @Test
    public void testDisposableObjArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableObjArray.create(-1);
        });
    }

    @Test
    public void testDisposableObjArray_create_withComponentType_throwsException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            DisposableObjArray.create(String.class, 5);
        });
    }

    @Test
    public void testDisposableObjArray_wrap_normal() {
        Object[] data = { 1, "hello", true };
        DisposableObjArray array = DisposableObjArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1, array.get(0));
        assertEquals("hello", array.get(1));
        assertEquals(true, array.get(2));
    }

    @Test
    public void testDisposableObjArray_wrap_emptyArray() {
        Object[] data = {};
        DisposableObjArray array = DisposableObjArray.wrap(data);
        assertNotNull(array);
        assertEquals(0, array.length());
    }

    @Test
    public void testDisposableBooleanArray_create_normal() {
        DisposableBooleanArray array = DisposableBooleanArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableBooleanArray_create_zeroLength() {
        DisposableBooleanArray array = DisposableBooleanArray.create(0);
        assertNotNull(array);
        assertEquals(0, array.length());
    }

    @Test
    public void testDisposableBooleanArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableBooleanArray.create(-1);
        });
    }

    @Test
    public void testDisposableBooleanArray_wrap_normal() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertTrue(array.get(0));
        assertFalse(array.get(1));
        assertTrue(array.get(2));
    }

    @Test
    public void testDisposableBooleanArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableBooleanArray.wrap(null);
        });
    }

    @Test
    public void testDisposableBooleanArray_get_validIndex() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        assertTrue(array.get(0));
        assertFalse(array.get(1));
        assertTrue(array.get(2));
    }

    @Test
    public void testDisposableBooleanArray_get_invalidIndex() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(3));
    }

    @Test
    public void testDisposableBooleanArray_length() {
        assertEquals(0, DisposableBooleanArray.wrap(new boolean[0]).length());
        assertEquals(5, DisposableBooleanArray.wrap(new boolean[5]).length());
    }

    @Test
    public void testDisposableBooleanArray_copy() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        boolean[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableBooleanArray_box() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        Boolean[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Boolean.TRUE, boxed[0]);
        assertEquals(Boolean.FALSE, boxed[1]);
        assertEquals(Boolean.TRUE, boxed[2]);
    }

    @Test
    public void testDisposableBooleanArray_toList() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        BooleanList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableBooleanArray_toCollection() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        ArrayList<Boolean> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableBooleanArray_foreach() throws Exception {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        List<Boolean> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableBooleanArray_apply() throws Exception {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableBooleanArray_accept() throws Exception {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableBooleanArray_join_delimiter() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableBooleanArray_join_delimiterPrefixSuffix() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableBooleanArray_toString() {
        boolean[] data = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableCharArray_create_normal() {
        DisposableCharArray array = DisposableCharArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableCharArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableCharArray.create(-1);
        });
    }

    @Test
    public void testDisposableCharArray_wrap_normal() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals('a', array.get(0));
        assertEquals('b', array.get(1));
        assertEquals('c', array.get(2));
    }

    @Test
    public void testDisposableCharArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableCharArray.wrap(null);
        });
    }

    @Test
    public void testDisposableCharArray_get_validIndex() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        assertEquals('a', array.get(0));
        assertEquals('b', array.get(1));
        assertEquals('c', array.get(2));
    }

    @Test
    public void testDisposableCharArray_length() {
        assertEquals(0, DisposableCharArray.wrap(new char[0]).length());
        assertEquals(5, DisposableCharArray.wrap(new char[5]).length());
    }

    @Test
    public void testDisposableCharArray_copy() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        char[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableCharArray_box() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        Character[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Character.valueOf('a'), boxed[0]);
        assertEquals(Character.valueOf('b'), boxed[1]);
        assertEquals(Character.valueOf('c'), boxed[2]);
    }

    @Test
    public void testDisposableCharArray_toList() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        CharList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableCharArray_toCollection() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        ArrayList<Character> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableCharArray_sum() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        int sum = array.sum();
        assertEquals(294, sum);
    }

    @Test
    public void testDisposableCharArray_sum_empty() {
        char[] data = {};
        DisposableCharArray array = DisposableCharArray.wrap(data);
        int sum = array.sum();
        assertEquals(0, sum);
    }

    @Test
    public void testDisposableCharArray_average() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        double avg = array.average();
        assertEquals(98.0, avg, 0.01);
    }

    @Test
    public void testDisposableCharArray_min() {
        char[] data = { 'c', 'a', 'b' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        char min = array.min();
        assertEquals('a', min);
    }

    @Test
    public void testDisposableCharArray_max() {
        char[] data = { 'a', 'c', 'b' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        char max = array.max();
        assertEquals('c', max);
    }

    @Test
    public void testDisposableCharArray_foreach() throws Exception {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        List<Character> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableCharArray_apply() throws Exception {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableCharArray_accept() throws Exception {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableCharArray_join_delimiter() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableCharArray_join_delimiterPrefixSuffix() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableCharArray_toString() {
        char[] data = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableByteArray_create_normal() {
        DisposableByteArray array = DisposableByteArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableByteArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableByteArray.create(-1);
        });
    }

    @Test
    public void testDisposableByteArray_wrap_normal() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableByteArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableByteArray.wrap(null);
        });
    }

    @Test
    public void testDisposableByteArray_get_validIndex() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableByteArray_length() {
        assertEquals(0, DisposableByteArray.wrap(new byte[0]).length());
        assertEquals(5, DisposableByteArray.wrap(new byte[5]).length());
    }

    @Test
    public void testDisposableByteArray_copy() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        byte[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableByteArray_box() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        Byte[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Byte.valueOf((byte) 1), boxed[0]);
        assertEquals(Byte.valueOf((byte) 2), boxed[1]);
        assertEquals(Byte.valueOf((byte) 3), boxed[2]);
    }

    @Test
    public void testDisposableByteArray_toList() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        ByteList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableByteArray_toCollection() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        ArrayList<Byte> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableByteArray_sum() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        int sum = array.sum();
        assertEquals(6, sum);
    }

    @Test
    public void testDisposableByteArray_average() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableByteArray_min() {
        byte[] data = { 3, 1, 2 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        byte min = array.min();
        assertEquals(1, min);
    }

    @Test
    public void testDisposableByteArray_max() {
        byte[] data = { 1, 3, 2 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        byte max = array.max();
        assertEquals(3, max);
    }

    @Test
    public void testDisposableByteArray_foreach() throws Exception {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        List<Byte> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableByteArray_apply() throws Exception {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableByteArray_accept() throws Exception {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableByteArray_join_delimiter() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableByteArray_join_delimiterPrefixSuffix() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableByteArray_toString() {
        byte[] data = { 1, 2, 3 };
        DisposableByteArray array = DisposableByteArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableShortArray_create_normal() {
        DisposableShortArray array = DisposableShortArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableShortArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableShortArray.create(-1);
        });
    }

    @Test
    public void testDisposableShortArray_wrap_normal() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableShortArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableShortArray.wrap(null);
        });
    }

    @Test
    public void testDisposableShortArray_get_validIndex() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableShortArray_length() {
        assertEquals(0, DisposableShortArray.wrap(new short[0]).length());
        assertEquals(5, DisposableShortArray.wrap(new short[5]).length());
    }

    @Test
    public void testDisposableShortArray_copy() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        short[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableShortArray_box() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        Short[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Short.valueOf((short) 1), boxed[0]);
        assertEquals(Short.valueOf((short) 2), boxed[1]);
        assertEquals(Short.valueOf((short) 3), boxed[2]);
    }

    @Test
    public void testDisposableShortArray_toList() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        ShortList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableShortArray_toCollection() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        ArrayList<Short> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableShortArray_sum() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        int sum = array.sum();
        assertEquals(6, sum);
    }

    @Test
    public void testDisposableShortArray_average() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableShortArray_min() {
        short[] data = { 3, 1, 2 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        short min = array.min();
        assertEquals(1, min);
    }

    @Test
    public void testDisposableShortArray_max() {
        short[] data = { 1, 3, 2 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        short max = array.max();
        assertEquals(3, max);
    }

    @Test
    public void testDisposableShortArray_foreach() throws Exception {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        List<Short> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableShortArray_apply() throws Exception {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableShortArray_accept() throws Exception {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableShortArray_join_delimiter() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableShortArray_join_delimiterPrefixSuffix() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableShortArray_toString() {
        short[] data = { 1, 2, 3 };
        DisposableShortArray array = DisposableShortArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableIntArray_create_normal() {
        DisposableIntArray array = DisposableIntArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableIntArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableIntArray.create(-1);
        });
    }

    @Test
    public void testDisposableIntArray_wrap_normal() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableIntArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableIntArray.wrap(null);
        });
    }

    @Test
    public void testDisposableIntArray_get_validIndex() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        assertEquals(1, array.get(0));
        assertEquals(2, array.get(1));
        assertEquals(3, array.get(2));
    }

    @Test
    public void testDisposableIntArray_length() {
        assertEquals(0, DisposableIntArray.wrap(new int[0]).length());
        assertEquals(5, DisposableIntArray.wrap(new int[5]).length());
    }

    @Test
    public void testDisposableIntArray_copy() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        int[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableIntArray_box() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        Integer[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Integer.valueOf(1), boxed[0]);
        assertEquals(Integer.valueOf(2), boxed[1]);
        assertEquals(Integer.valueOf(3), boxed[2]);
    }

    @Test
    public void testDisposableIntArray_toList() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        IntList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableIntArray_toCollection() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        ArrayList<Integer> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableIntArray_sum() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        int sum = array.sum();
        assertEquals(6, sum);
    }

    @Test
    public void testDisposableIntArray_average() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableIntArray_min() {
        int[] data = { 3, 1, 2 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        int min = array.min();
        assertEquals(1, min);
    }

    @Test
    public void testDisposableIntArray_max() {
        int[] data = { 1, 3, 2 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        int max = array.max();
        assertEquals(3, max);
    }

    @Test
    public void testDisposableIntArray_foreach() throws Exception {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        List<Integer> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableIntArray_apply() throws Exception {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableIntArray_accept() throws Exception {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableIntArray_join_delimiter() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableIntArray_join_delimiterPrefixSuffix() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableIntArray_toString() {
        int[] data = { 1, 2, 3 };
        DisposableIntArray array = DisposableIntArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableLongArray_create_normal() {
        DisposableLongArray array = DisposableLongArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableLongArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableLongArray.create(-1);
        });
    }

    @Test
    public void testDisposableLongArray_wrap_normal() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1L, array.get(0));
        assertEquals(2L, array.get(1));
        assertEquals(3L, array.get(2));
    }

    @Test
    public void testDisposableLongArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableLongArray.wrap(null);
        });
    }

    @Test
    public void testDisposableLongArray_get_validIndex() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        assertEquals(1L, array.get(0));
        assertEquals(2L, array.get(1));
        assertEquals(3L, array.get(2));
    }

    @Test
    public void testDisposableLongArray_length() {
        assertEquals(0, DisposableLongArray.wrap(new long[0]).length());
        assertEquals(5, DisposableLongArray.wrap(new long[5]).length());
    }

    @Test
    public void testDisposableLongArray_copy() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        long[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy);
    }

    @Test
    public void testDisposableLongArray_box() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        Long[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Long.valueOf(1L), boxed[0]);
        assertEquals(Long.valueOf(2L), boxed[1]);
        assertEquals(Long.valueOf(3L), boxed[2]);
    }

    @Test
    public void testDisposableLongArray_toList() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        LongList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableLongArray_toCollection() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        ArrayList<Long> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableLongArray_sum() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        long sum = array.sum();
        assertEquals(6L, sum);
    }

    @Test
    public void testDisposableLongArray_average() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableLongArray_min() {
        long[] data = { 3L, 1L, 2L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        long min = array.min();
        assertEquals(1L, min);
    }

    @Test
    public void testDisposableLongArray_max() {
        long[] data = { 1L, 3L, 2L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        long max = array.max();
        assertEquals(3L, max);
    }

    @Test
    public void testDisposableLongArray_foreach() throws Exception {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        List<Long> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableLongArray_apply() throws Exception {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableLongArray_accept() throws Exception {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableLongArray_join_delimiter() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableLongArray_join_delimiterPrefixSuffix() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableLongArray_toString() {
        long[] data = { 1L, 2L, 3L };
        DisposableLongArray array = DisposableLongArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableFloatArray_create_normal() {
        DisposableFloatArray array = DisposableFloatArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableFloatArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableFloatArray.create(-1);
        });
    }

    @Test
    public void testDisposableFloatArray_wrap_normal() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1.0f, array.get(0), 0.01f);
        assertEquals(2.0f, array.get(1), 0.01f);
        assertEquals(3.0f, array.get(2), 0.01f);
    }

    @Test
    public void testDisposableFloatArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableFloatArray.wrap(null);
        });
    }

    @Test
    public void testDisposableFloatArray_get_validIndex() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        assertEquals(1.0f, array.get(0), 0.01f);
        assertEquals(2.0f, array.get(1), 0.01f);
        assertEquals(3.0f, array.get(2), 0.01f);
    }

    @Test
    public void testDisposableFloatArray_length() {
        assertEquals(0, DisposableFloatArray.wrap(new float[0]).length());
        assertEquals(5, DisposableFloatArray.wrap(new float[5]).length());
    }

    @Test
    public void testDisposableFloatArray_copy() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        float[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy, 0.01f);
    }

    @Test
    public void testDisposableFloatArray_box() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        Float[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Float.valueOf(1.0f), boxed[0]);
        assertEquals(Float.valueOf(2.0f), boxed[1]);
        assertEquals(Float.valueOf(3.0f), boxed[2]);
    }

    @Test
    public void testDisposableFloatArray_toList() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        FloatList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableFloatArray_toCollection() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        ArrayList<Float> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableFloatArray_sum() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        float sum = array.sum();
        assertEquals(6.0f, sum, 0.01f);
    }

    @Test
    public void testDisposableFloatArray_average() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableFloatArray_min() {
        float[] data = { 3.0f, 1.0f, 2.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        float min = array.min();
        assertEquals(1.0f, min, 0.01f);
    }

    @Test
    public void testDisposableFloatArray_max() {
        float[] data = { 1.0f, 3.0f, 2.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        float max = array.max();
        assertEquals(3.0f, max, 0.01f);
    }

    @Test
    public void testDisposableFloatArray_foreach() throws Exception {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        List<Float> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableFloatArray_apply() throws Exception {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableFloatArray_accept() throws Exception {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableFloatArray_join_delimiter() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableFloatArray_join_delimiterPrefixSuffix() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableFloatArray_toString() {
        float[] data = { 1.0f, 2.0f, 3.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableDoubleArray_create_normal() {
        DisposableDoubleArray array = DisposableDoubleArray.create(5);
        assertNotNull(array);
        assertEquals(5, array.length());
    }

    @Test
    public void testDisposableDoubleArray_create_negativeLength() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableDoubleArray.create(-1);
        });
    }

    @Test
    public void testDisposableDoubleArray_wrap_normal() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        assertNotNull(array);
        assertEquals(3, array.length());
        assertEquals(1.0, array.get(0), 0.01);
        assertEquals(2.0, array.get(1), 0.01);
        assertEquals(3.0, array.get(2), 0.01);
    }

    @Test
    public void testDisposableDoubleArray_wrap_nullArray() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableDoubleArray.wrap(null);
        });
    }

    @Test
    public void testDisposableDoubleArray_get_validIndex() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        assertEquals(1.0, array.get(0), 0.01);
        assertEquals(2.0, array.get(1), 0.01);
        assertEquals(3.0, array.get(2), 0.01);
    }

    @Test
    public void testDisposableDoubleArray_length() {
        assertEquals(0, DisposableDoubleArray.wrap(new double[0]).length());
        assertEquals(5, DisposableDoubleArray.wrap(new double[5]).length());
    }

    @Test
    public void testDisposableDoubleArray_copy() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        double[] copy = array.copy();
        assertNotSame(data, copy);
        assertArrayEquals(data, copy, 0.01);
    }

    @Test
    public void testDisposableDoubleArray_box() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        Double[] boxed = array.box();
        assertNotNull(boxed);
        assertEquals(3, boxed.length);
        assertEquals(Double.valueOf(1.0), boxed[0]);
        assertEquals(Double.valueOf(2.0), boxed[1]);
        assertEquals(Double.valueOf(3.0), boxed[2]);
    }

    @Test
    public void testDisposableDoubleArray_toList() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        DoubleList list = array.toList();
        assertNotNull(list);
        assertEquals(3, list.size());
    }

    @Test
    public void testDisposableDoubleArray_toCollection() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        ArrayList<Double> result = array.toCollection(ArrayList::new);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void testDisposableDoubleArray_sum() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        double sum = array.sum();
        assertEquals(6.0, sum, 0.01);
    }

    @Test
    public void testDisposableDoubleArray_average() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        double avg = array.average();
        assertEquals(2.0, avg, 0.01);
    }

    @Test
    public void testDisposableDoubleArray_min() {
        double[] data = { 3.0, 1.0, 2.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        double min = array.min();
        assertEquals(1.0, min, 0.01);
    }

    @Test
    public void testDisposableDoubleArray_max() {
        double[] data = { 1.0, 3.0, 2.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        double max = array.max();
        assertEquals(3.0, max, 0.01);
    }

    @Test
    public void testDisposableDoubleArray_foreach() throws Exception {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        List<Double> collected = new ArrayList<>();
        array.foreach(collected::add);
        assertEquals(3, collected.size());
    }

    @Test
    public void testDisposableDoubleArray_apply() throws Exception {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        Integer result = array.apply(arr -> arr.length);
        assertEquals(3, result);
    }

    @Test
    public void testDisposableDoubleArray_accept() throws Exception {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        AtomicInteger count = new AtomicInteger(0);
        array.accept(arr -> count.set(arr.length));
        assertEquals(3, count.get());
    }

    @Test
    public void testDisposableDoubleArray_join_delimiter() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        String result = array.join(", ");
        assertNotNull(result);
    }

    @Test
    public void testDisposableDoubleArray_join_delimiterPrefixSuffix() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        String result = array.join(", ", "[", "]");
        assertNotNull(result);
    }

    @Test
    public void testDisposableDoubleArray_toString() {
        double[] data = { 1.0, 2.0, 3.0 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(data);
        String result = array.toString();
        assertNotNull(result);
    }

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
    public void testDisposableEntry_wrap_normal() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertNotNull(disposable);
        assertEquals("key", disposable.getKey());
        assertEquals(123, disposable.getValue());
    }

    @Test
    public void testDisposableEntry_wrap_nullEntry() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableEntry.wrap(null);
        });
    }

    @Test
    public void testDisposableEntry_getKey() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertEquals("key", disposable.getKey());
    }

    @Test
    public void testDisposableEntry_getValue() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertEquals(123, disposable.getValue());
    }

    @Test
    public void testDisposableEntry_setValue_throwsException() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        assertThrows(UnsupportedOperationException.class, () -> {
            disposable.setValue(456);
        });
    }

    @Test
    public void testDisposableEntry_apply_withEntry() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.apply(e -> e.getKey() + "=" + e.getValue());
        assertEquals("key=123", result);
    }

    @Test
    public void testDisposableEntry_apply_withBiFunction() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.apply((k, v) -> k + "=" + v);
        assertEquals("key=123", result);
    }

    @Test
    public void testDisposableEntry_accept_withEntry() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept(e -> ref.set(e.getKey() + "=" + e.getValue()));
        assertEquals("key=123", ref.get());
    }

    @Test
    public void testDisposableEntry_accept_withBiConsumer() throws Exception {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((k, v) -> ref.set(k + "=" + v));
        assertEquals("key=123", ref.get());
    }

    @Test
    public void testDisposableEntry_copy() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        Map.Entry<String, Integer> copy = disposable.copy();
        assertNotNull(copy);
        assertEquals("key", copy.getKey());
        assertEquals(123, copy.getValue());
        assertNotSame(entry, copy);
    }

    @Test
    public void testDisposableEntry_toString() {
        Map.Entry<String, Integer> entry = Map.entry("key", 123);
        DisposableEntry<String, Integer> disposable = DisposableEntry.wrap(entry);
        String result = disposable.toString();
        assertNotNull(result);
        assertTrue(result.contains("key"));
        assertTrue(result.contains("123"));
    }

    @Test
    public void testDisposablePair_wrap_normal() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        assertNotNull(disposable);
        assertEquals("left", disposable.left());
        assertEquals(123, disposable.right());
    }

    @Test
    public void testDisposablePair_wrap_nullPair() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposablePair.wrap(null);
        });
    }

    @Test
    public void testDisposablePair_left() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        assertEquals("left", disposable.left());
    }

    @Test
    public void testDisposablePair_right() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        assertEquals(123, disposable.right());
    }

    @Test
    public void testDisposablePair_copy() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        Pair<String, Integer> copy = disposable.copy();
        assertNotNull(copy);
        assertEquals("left", copy.left());
        assertEquals(123, copy.right());
    }

    @Test
    public void testDisposablePair_apply() throws Exception {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        String result = disposable.apply((l, r) -> l + "=" + r);
        assertEquals("left=123", result);
    }

    @Test
    public void testDisposablePair_accept() throws Exception {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((l, r) -> ref.set(l + "=" + r));
        assertEquals("left=123", ref.get());
    }

    @Test
    public void testDisposablePair_toString() {
        Pair<String, Integer> pair = Pair.of("left", 123);
        DisposablePair<String, Integer> disposable = DisposablePair.wrap(pair);
        String result = disposable.toString();
        assertNotNull(result);
    }

    @Test
    public void testDisposableTriple_wrap_normal() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        assertNotNull(disposable);
        assertEquals("left", disposable.left());
        assertEquals(123, disposable.middle());
        assertEquals(true, disposable.right());
    }

    @Test
    public void testDisposableTriple_wrap_nullTriple() {
        assertThrows(IllegalArgumentException.class, () -> {
            DisposableTriple.wrap(null);
        });
    }

    @Test
    public void testDisposableTriple_left() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        assertEquals("left", disposable.left());
    }

    @Test
    public void testDisposableTriple_middle() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        assertEquals(123, disposable.middle());
    }

    @Test
    public void testDisposableTriple_right() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        assertEquals(true, disposable.right());
    }

    @Test
    public void testDisposableTriple_copy() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        Triple<String, Integer, Boolean> copy = disposable.copy();
        assertNotNull(copy);
        assertEquals("left", copy.left());
        assertEquals(123, copy.middle());
        assertEquals(true, copy.right());
    }

    @Test
    public void testDisposableTriple_apply() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        String result = disposable.apply((l, m, r) -> l + "=" + m + "=" + r);
        assertEquals("left=123=true", result);
    }

    @Test
    public void testDisposableTriple_accept() throws Exception {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        AtomicReference<String> ref = new AtomicReference<>();
        disposable.accept((l, m, r) -> ref.set(l + "=" + m + "=" + r));
        assertEquals("left=123=true", ref.get());
    }

    @Test
    public void testDisposableTriple_toString() {
        Triple<String, Integer, Boolean> triple = Triple.of("left", 123, true);
        DisposableTriple<String, Integer, Boolean> disposable = DisposableTriple.wrap(triple);
        String result = disposable.toString();
        assertNotNull(result);
    }

    @Test
    public void testTimed_of_normal() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertNotNull(timed);
        assertEquals("value", timed.value());
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_of_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        assertNotNull(timed);
        assertEquals(null, timed.value());
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_value() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertEquals("value", timed.value());
    }

    @Test
    public void testTimed_timestamp() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertEquals(12345L, timed.timestamp());
    }

    @Test
    public void testTimed_hashCode_sameObject() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 12345L);
        assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimed_hashCode_differentValue() {
        Timed<String> timed1 = Timed.of("value1", 12345L);
        Timed<String> timed2 = Timed.of("value2", 12345L);
        assertNotNull(Integer.valueOf(timed1.hashCode()));
        assertNotNull(Integer.valueOf(timed2.hashCode()));
    }

    @Test
    public void testTimed_hashCode_differentTimestamp() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 67890L);
        assertNotNull(Integer.valueOf(timed1.hashCode()));
        assertNotNull(Integer.valueOf(timed2.hashCode()));
    }

    @Test
    public void testTimed_hashCode_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        assertNotNull(Integer.valueOf(timed.hashCode()));
    }

    @Test
    public void testTimed_equals_sameObject() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertTrue(timed.equals(timed));
    }

    @Test
    public void testTimed_equals_equalObjects() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 12345L);
        assertTrue(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_differentValue() {
        Timed<String> timed1 = Timed.of("value1", 12345L);
        Timed<String> timed2 = Timed.of("value2", 12345L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_differentTimestamp() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of("value", 67890L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_nullValue() {
        Timed<String> timed1 = Timed.of(null, 12345L);
        Timed<String> timed2 = Timed.of(null, 12345L);
        assertTrue(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_oneNullValue() {
        Timed<String> timed1 = Timed.of("value", 12345L);
        Timed<String> timed2 = Timed.of(null, 12345L);
        assertFalse(timed1.equals(timed2));
    }

    @Test
    public void testTimed_equals_nullObject() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertFalse(timed.equals(null));
    }

    @Test
    public void testTimed_equals_differentClass() {
        Timed<String> timed = Timed.of("value", 12345L);
        assertFalse(timed.equals("not a Timed object"));
    }

    @Test
    public void testTimed_toString() {
        Timed<String> timed = Timed.of("value", 12345L);
        String result = timed.toString();
        assertNotNull(result);
        assertTrue(result.contains("12345"));
        assertTrue(result.contains("value"));
    }

    @Test
    public void testTimed_toString_nullValue() {
        Timed<String> timed = Timed.of(null, 12345L);
        String result = timed.toString();
        assertNotNull(result);
        assertTrue(result.contains("12345"));
    }
}
