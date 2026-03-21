package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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

public class NoCachingNoUpdatingTest extends TestBase {

    private NoCachingNoUpdating.DisposableArray<String> disposableArray;
    private String[] sourceArray;
    private NoCachingNoUpdating.DisposableDeque<Integer> disposableDeque;
    private Deque<Integer> sourceDeque;
    private NoCachingNoUpdating.DisposableEntry<String, Integer> disposableEntry;
    private Map.Entry<String, Integer> sourceEntry;
    private NoCachingNoUpdating.Timed<String> timed;
    private long timestamp;
    private String value;

    @BeforeEach
    public void setUp() {
        sourceArray = new String[] { "a", "b", "c" };
        disposableArray = NoCachingNoUpdating.DisposableArray.wrap(sourceArray);

        sourceDeque = new ArrayDeque<>(Arrays.asList(1, 2, 3));
        disposableDeque = NoCachingNoUpdating.DisposableDeque.wrap(sourceDeque);

        sourceEntry = new AbstractMap.SimpleEntry<>("key", 123);
        disposableEntry = NoCachingNoUpdating.DisposableEntry.wrap(sourceEntry);

        value = "test-value";
        timestamp = System.currentTimeMillis();
        timed = NoCachingNoUpdating.Timed.of(value, timestamp);
    }

    private static String[] disposableArrayFixtureSource() {
        return new String[] { "a", "b", "c" };
    }

    private static DisposableArray<String> disposableArrayFixture() {
        return DisposableArray.wrap(disposableArrayFixtureSource());
    }

    private static DisposableDeque<Integer> disposableDequeFixture() {
        return DisposableDeque.wrap(new ArrayDeque<>(Arrays.asList(1, 2, 3)));
    }

    private static DisposableEntry<String, Integer> disposableEntryFixture() {
        return DisposableEntry.wrap(new AbstractMap.SimpleEntry<>("key", 123));
    }

    private static String timedFixtureValue() {
        return "test-value";
    }

    private static long timedFixtureTimestamp() {
        return 12345L;
    }

    private static Timed<String> timedFixture() {
        return Timed.of(timedFixtureValue(), timedFixtureTimestamp());
    }

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

    @Test
    @DisplayName("Should create a DisposableArray of the specified type and length")
    public void testCreate() {
        NoCachingNoUpdating.DisposableArray<Integer> created = NoCachingNoUpdating.DisposableArray.create(Integer.class, 5);
        assertNotNull(created);
        assertEquals(5, created.length());
    }

    @Test
    @DisplayName("Should wrap an existing array")
    public void testWrap() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        assertNotNull(disposableArray);
        assertEquals(sourceArray.length, disposableArray.length());
    }

    @Test
    @DisplayName("Should get the element at the specified index")
    public void testGet() {
        DisposableArray<String> disposableArray = disposableArrayFixture();
        assertEquals("a", disposableArray.get(0));
        assertEquals("b", disposableArray.get(1));
        assertEquals("c", disposableArray.get(2));
    }

    @Test
    @DisplayName("Should return the correct length of the array")
    public void testLength() {
        DisposableArray<String> disposableArray = disposableArrayFixture();
        assertEquals(3, disposableArray.length());
    }

    @Test
    @DisplayName("Should copy the contents to a target array")
    public void testToArray() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        String[] target = new String[3];
        disposableArray.toArray(target);
        assertArrayEquals(sourceArray, target);
    }

    @Test
    @DisplayName("Should create a copy of the internal array")
    public void testCopy() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        String[] copy = disposableArray.copy();
        assertNotSame(sourceArray, copy);
        assertArrayEquals(sourceArray, copy);
    }

    @Test
    @DisplayName("Should convert the array to a List")
    public void testToList() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        List<String> list = disposableArray.toList();
        assertEquals(Arrays.asList(sourceArray), list);
    }

    @Test
    @DisplayName("Should convert the array to a Set")
    public void testToSet() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        Set<String> set = disposableArray.toSet();
        assertEquals(new HashSet<>(Arrays.asList(sourceArray)), set);
    }

    @Test
    @DisplayName("Should convert the array to a specified collection")
    public void testToCollection() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        ArrayList<String> collection = disposableArray.toCollection(ArrayList::new);
        assertEquals(Arrays.asList(sourceArray), collection);
    }

    @Test
    @DisplayName("Should iterate over each element")
    public void testForEach() throws Exception {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        List<String> result = new ArrayList<>();
        disposableArray.foreach(result::add);
        assertEquals(Arrays.asList(sourceArray), result);
    }

    @Test
    @DisplayName("Should apply a function to the array")
    public void testApply() throws Exception {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        Integer length = disposableArray.apply(arr -> arr.length);
        assertEquals(sourceArray.length, length);
    }

    @Test
    @DisplayName("Should accept a consumer for the array")
    public void testAccept() throws Exception {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        final List<String[]> captured = new ArrayList<>();
        disposableArray.accept(captured::add);
        assertEquals(1, captured.size());
        assertSame(sourceArray, captured.get(0));
    }

    @Test
    @DisplayName("Should join array elements with a delimiter")
    public void testJoinWithDelimiter() {
        DisposableArray<String> disposableArray = disposableArrayFixture();
        assertEquals("a,b,c", disposableArray.join(","));
    }

    @Test
    @DisplayName("Should join array elements with delimiter, prefix, and suffix")
    public void testJoinWithDelimiterPrefixSuffix() {
        DisposableArray<String> disposableArray = disposableArrayFixture();
        assertEquals("[a,b,c]", disposableArray.join(",", "[", "]"));
    }

    @Test
    @DisplayName("Should provide a working iterator")
    public void testIterator() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        List<String> result = new ArrayList<>();
        disposableArray.iterator().forEachRemaining(result::add);
        assertEquals(Arrays.asList(sourceArray), result);
    }

    @Test
    @DisplayName("Should return a string representation of the array")
    public void testToString() {
        String[] sourceArray = disposableArrayFixtureSource();
        DisposableArray<String> disposableArray = DisposableArray.wrap(sourceArray);
        assertEquals(Arrays.toString(sourceArray), disposableArray.toString());
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException for create with component type")
    public void testCreateWithComponentType() {
        assertThrows(UnsupportedOperationException.class, () -> NoCachingNoUpdating.DisposableObjArray.create(String.class, 5));
    }

    @Test
    @DisplayName("Should box the primitive array to a wrapper array")
    public void testBox() {
        DisposableBooleanArray disposableArray = NoCachingNoUpdating.DisposableBooleanArray.wrap(new boolean[] { true, false, true });
        Boolean[] boxed = disposableArray.box();
        assertArrayEquals(new Boolean[] { true, false, true }, boxed);
    }

    @Test
    @DisplayName("Should calculate the sum of char values")
    public void testSum() {
        DisposableCharArray disposableArray = NoCachingNoUpdating.DisposableCharArray.wrap(new char[] { 'a', 'b', 'c' });
        assertEquals(97 + 98 + 99, disposableArray.sum());
    }

    @Test
    @DisplayName("Should calculate the average of char values")
    public void testAverage() {
        DisposableCharArray disposableArray = NoCachingNoUpdating.DisposableCharArray.wrap(new char[] { 'a', 'b', 'c' });
        assertEquals((97 + 98 + 99) / 3.0, disposableArray.average());
    }

    @Test
    @DisplayName("Should find the minimum char value")
    public void testMin() {
        DisposableCharArray disposableArray = NoCachingNoUpdating.DisposableCharArray.wrap(new char[] { 'a', 'b', 'c' });
        assertEquals('a', disposableArray.min());
    }

    @Test
    @DisplayName("Should find the maximum char value")
    public void testMax() {
        DisposableCharArray disposableArray = NoCachingNoUpdating.DisposableCharArray.wrap(new char[] { 'a', 'b', 'c' });
        assertEquals('c', disposableArray.max());
    }

    @Test
    @DisplayName("Should return the first element")
    public void testGetFirst() {
        DisposableDeque<Integer> disposableDeque = disposableDequeFixture();
        assertEquals(1, disposableDeque.getFirst());
    }

    @Test
    @DisplayName("Should return the last element")
    public void testGetLast() {
        DisposableDeque<Integer> disposableDeque = disposableDequeFixture();
        assertEquals(3, disposableDeque.getLast());
    }

    @Test
    @DisplayName("Should throw exception when getting first from empty deque")
    public void testGetFirst_Empty() {
        NoCachingNoUpdating.DisposableDeque<Integer> emptyDeque = NoCachingNoUpdating.DisposableDeque.wrap(new ArrayDeque<>());
        assertThrows(NoSuchElementException.class, emptyDeque::getFirst);
    }

    @Test
    @DisplayName("Should throw UnsupportedOperationException on setValue")
    public void testSetValue() {
        DisposableEntry<String, Integer> disposableEntry = disposableEntryFixture();
        assertThrows(UnsupportedOperationException.class, () -> disposableEntry.setValue(456));
    }

    @Test
    @DisplayName("Should apply a BiFunction to key and value")
    public void testApplyBiFunction() throws Exception {
        DisposableEntry<String, Integer> disposableEntry = disposableEntryFixture();
        String result = disposableEntry.apply((k, v) -> k + ":" + v);
        assertEquals("key:123", result);
    }

    @Test
    @DisplayName("Should accept a BiConsumer for key and value")
    public void testAcceptBiConsumer() throws Exception {
        DisposableEntry<String, Integer> disposableEntry = disposableEntryFixture();
        List<String> result = new ArrayList<>();
        disposableEntry.accept((k, v) -> result.add(k + "=" + v));
        assertEquals(1, result.size());
        assertEquals("key=123", result.get(0));
    }

    @Test
    @DisplayName("Should create a Timed object with correct value and timestamp")
    public void testOf() {
        String value = timedFixtureValue();
        long timestamp = timedFixtureTimestamp();
        Timed<String> timed = timedFixture();
        assertEquals(value, timed.value());
        assertEquals(timestamp, timed.timestamp());
    }

    @Test
    @DisplayName("Should return the correct value")
    public void testValue() {
        Timed<String> timed = timedFixture();
        assertEquals("test-value", timed.value());
    }

    @Test
    @DisplayName("Should return the correct timestamp")
    public void testTimestamp() {
        long timestamp = timedFixtureTimestamp();
        Timed<String> timed = timedFixture();
        assertEquals(timestamp, timed.timestamp());
    }

    @Test
    @DisplayName("Should have consistent hash code")
    public void testHashCode() {
        String value = timedFixtureValue();
        long timestamp = timedFixtureTimestamp();
        Timed<String> timed = timedFixture();
        NoCachingNoUpdating.Timed<String> sameTimed = NoCachingNoUpdating.Timed.of(value, timestamp);
        assertEquals(timed.hashCode(), sameTimed.hashCode());
    }

    @Test
    @DisplayName("Should be equal to another Timed object with the same value and time")
    public void testEquals() {
        String value = timedFixtureValue();
        long timestamp = timedFixtureTimestamp();
        Timed<String> timed = timedFixture();
        NoCachingNoUpdating.Timed<String> sameTimed = NoCachingNoUpdating.Timed.of(value, timestamp);
        NoCachingNoUpdating.Timed<String> differentTime = NoCachingNoUpdating.Timed.of(value, timestamp + 1);
        NoCachingNoUpdating.Timed<String> differentValue = NoCachingNoUpdating.Timed.of("other", timestamp);

        assertTrue(timed.equals(sameTimed));
        assertFalse(timed.equals(differentTime));
        assertFalse(timed.equals(differentValue));
        assertFalse(timed.equals(null));
        assertFalse(timed.equals(new Object()));
    }

    @Test
    public void testDisposableArrayCreate() {
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.create(String.class, 5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableArrayWrap() {
        String[] original = { "a", "b", "c" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals("a", array.get(0));
        Assertions.assertEquals("b", array.get(1));
        Assertions.assertEquals("c", array.get(2));
    }

    @Test
    public void testDisposableArrayGet() {
        String[] original = { "hello", "world" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Assertions.assertEquals("hello", array.get(0));
        Assertions.assertEquals("world", array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableArrayLength() {
        DisposableArray<Integer> array = NoCachingNoUpdating.DisposableArray.create(Integer.class, 10);
        Assertions.assertEquals(10, array.length());
    }

    @Test
    public void testDisposableArrayToArray() {
        String[] original = { "a", "b", "c" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);

        String[] small = new String[1];
        String[] result1 = array.toArray(small);
        Assertions.assertEquals(3, result1.length);
        Assertions.assertArrayEquals(original, result1);

        String[] large = new String[5];
        String[] result2 = array.toArray(large);
        Assertions.assertSame(large, result2);
        Assertions.assertArrayEquals(original, Arrays.copyOf(result2, 3));
    }

    @Test
    public void testDisposableArrayCopy() {
        String[] original = { "x", "y", "z" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        String[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableArrayToList() {
        Integer[] original = { 1, 2, 3 };
        DisposableArray<Integer> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        List<Integer> list = array.toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testDisposableArrayToSet() {
        String[] original = { "a", "b", "a", "c" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Set<String> set = array.toSet();
        Assertions.assertEquals(3, set.size());
        Assertions.assertTrue(set.contains("a"));
        Assertions.assertTrue(set.contains("b"));
        Assertions.assertTrue(set.contains("c"));
    }

    @Test
    public void testDisposableArrayToCollection() {
        String[] original = { "one", "two", "three" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        LinkedList<String> linkedList = array.toCollection(IntFunctions.ofLinkedList());
        Assertions.assertEquals(3, linkedList.size());
        Assertions.assertEquals("one", linkedList.get(0));
        Assertions.assertEquals("two", linkedList.get(1));
        Assertions.assertEquals("three", linkedList.get(2));
    }

    @Test
    public void testDisposableArrayForeach() throws Exception {
        String[] original = { "a", "b", "c" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        List<String> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testDisposableArrayApply() throws Exception {
        Integer[] original = { 1, 2, 3 };
        DisposableArray<Integer> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        int sum = array.apply(arr -> Arrays.stream(arr).mapToInt(Integer::intValue).sum());
        Assertions.assertEquals(6, sum);
    }

    @Test
    public void testDisposableArrayAccept() throws Exception {
        String[] original = { "test" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableArrayJoin() {
        String[] original = { "a", "b", "c" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Assertions.assertEquals("a, b, c", array.join(", "));
    }

    @Test
    public void testDisposableArrayJoinWithPrefixSuffix() {
        String[] original = { "x", "y", "z" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Assertions.assertEquals("[x, y, z]", array.join(", ", "[", "]"));
    }

    @Test
    public void testDisposableArrayIterator() {
        String[] original = { "1", "2", "3" };
        DisposableArray<String> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        Iterator<String> iter = array.iterator();
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals("1", iter.next());
        Assertions.assertEquals("2", iter.next());
        Assertions.assertEquals("3", iter.next());
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDisposableArrayToString() {
        Integer[] original = { 10, 20, 30 };
        DisposableArray<Integer> array = NoCachingNoUpdating.DisposableArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("10"));
        Assertions.assertTrue(str.contains("20"));
        Assertions.assertTrue(str.contains("30"));
    }

    @Test
    public void testDisposableObjArrayCreate() {
        NoCachingNoUpdating.DisposableObjArray array = NoCachingNoUpdating.DisposableObjArray.create(10);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(10, array.length());
    }

    @Test
    public void testDisposableObjArrayWrap() {
        Object[] original = { 1, "hello", true };
        NoCachingNoUpdating.DisposableObjArray array = NoCachingNoUpdating.DisposableObjArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals(1, array.get(0));
        Assertions.assertEquals("hello", array.get(1));
        Assertions.assertEquals(true, array.get(2));
    }

    @Test
    public void testDisposableBooleanArrayCreate() {
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableBooleanArrayWrap() {
        boolean[] original = { true, false, true };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertTrue(array.get(0));
        Assertions.assertFalse(array.get(1));
        Assertions.assertTrue(array.get(2));
    }

    @Test
    public void testDisposableBooleanArrayGet() {
        boolean[] original = { true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        Assertions.assertTrue(array.get(0));
        Assertions.assertFalse(array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableBooleanArrayLength() {
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.create(7);
        Assertions.assertEquals(7, array.length());
    }

    @Test
    public void testDisposableBooleanArrayCopy() {
        boolean[] original = { true, true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        boolean[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableBooleanArrayBox() {
        boolean[] original = { true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        Boolean[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Boolean.TRUE, boxed[0]);
        Assertions.assertEquals(Boolean.FALSE, boxed[1]);
    }

    @Test
    public void testDisposableBooleanArrayToList() {
        boolean[] original = { false, true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        BooleanList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertFalse(list.get(0));
        Assertions.assertTrue(list.get(1));
        Assertions.assertFalse(list.get(2));
    }

    @Test
    public void testDisposableBooleanArrayToCollection() {
        boolean[] original = { true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        ArrayList<Boolean> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Boolean.TRUE, list.get(0));
        Assertions.assertEquals(Boolean.FALSE, list.get(1));
    }

    @Test
    public void testDisposableBooleanArrayForeach() throws Exception {
        boolean[] original = { true, false, true };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        List<Boolean> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList(true, false, true), collected);
    }

    @Test
    public void testDisposableBooleanArrayApply() throws Exception {
        boolean[] original = { true, true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        int count = array.apply(arr -> {
            int c = 0;
            for (boolean b : arr)
                if (b)
                    c++;
            return c;
        });
        Assertions.assertEquals(2, count);
    }

    @Test
    public void testDisposableBooleanArrayAccept() throws Exception {
        boolean[] original = { true };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableBooleanArrayJoin() {
        boolean[] original = { true, false, true };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        Assertions.assertEquals("true, false, true", array.join(", "));
    }

    @Test
    public void testDisposableBooleanArrayJoinWithPrefixSuffix() {
        boolean[] original = { false, true };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        Assertions.assertEquals("[false, true]", array.join(", ", "[", "]"));
    }

    @Test
    public void testDisposableBooleanArrayToString() {
        boolean[] original = { true, false };
        NoCachingNoUpdating.DisposableBooleanArray array = NoCachingNoUpdating.DisposableBooleanArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("true"));
        Assertions.assertTrue(str.contains("false"));
    }

    @Test
    public void testDisposableCharArrayCreate() {
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableCharArrayWrap() {
        char[] original = { 'a', 'b', 'c' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals('a', array.get(0));
        Assertions.assertEquals('b', array.get(1));
        Assertions.assertEquals('c', array.get(2));
    }

    @Test
    public void testDisposableCharArrayGet() {
        char[] original = { 'x', 'y' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals('x', array.get(0));
        Assertions.assertEquals('y', array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableCharArrayLength() {
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.create(8);
        Assertions.assertEquals(8, array.length());
    }

    @Test
    public void testDisposableCharArrayCopy() {
        char[] original = { '1', '2', '3' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        char[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableCharArrayBox() {
        char[] original = { 'a', 'b' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Character[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Character.valueOf('a'), boxed[0]);
        Assertions.assertEquals(Character.valueOf('b'), boxed[1]);
    }

    @Test
    public void testDisposableCharArrayToList() {
        char[] original = { 'x', 'y', 'z' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        CharList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals('x', list.get(0));
        Assertions.assertEquals('y', list.get(1));
        Assertions.assertEquals('z', list.get(2));
    }

    @Test
    public void testDisposableCharArrayToCollection() {
        char[] original = { 'a', 'b' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        ArrayList<Character> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Character.valueOf('a'), list.get(0));
        Assertions.assertEquals(Character.valueOf('b'), list.get(1));
    }

    @Test
    public void testDisposableCharArraySum() {
        char[] original = { 'a', 'b', 'c' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals(294, array.sum());
    }

    @Test
    public void testDisposableCharArrayAverage() {
        char[] original = { 'a', 'b', 'c' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals(98.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableCharArrayMin() {
        char[] original = { 'z', 'a', 'm' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals('a', array.min());
    }

    @Test
    public void testDisposableCharArrayMax() {
        char[] original = { 'z', 'a', 'm' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals('z', array.max());
    }

    @Test
    public void testDisposableCharArrayForeach() throws Exception {
        char[] original = { 'a', 'b', 'c' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        List<Character> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList('a', 'b', 'c'), collected);
    }

    @Test
    public void testDisposableCharArrayApply() throws Exception {
        char[] original = { 'h', 'i' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        String result = array.apply(arr -> new String(arr));
        Assertions.assertEquals("hi", result);
    }

    @Test
    public void testDisposableCharArrayAccept() throws Exception {
        char[] original = { 't', 'e', 's', 't' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableCharArrayJoin() {
        char[] original = { 'a', 'b', 'c' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals("a-b-c", array.join("-"));
    }

    @Test
    public void testDisposableCharArrayJoinWithPrefixSuffix() {
        char[] original = { 'x', 'y' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        Assertions.assertEquals("(x:y)", array.join(":", "(", ")"));
    }

    @Test
    public void testDisposableCharArrayToString() {
        char[] original = { 'a', 'b' };
        NoCachingNoUpdating.DisposableCharArray array = NoCachingNoUpdating.DisposableCharArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("a"));
        Assertions.assertTrue(str.contains("b"));
    }

    @Test
    public void testDisposableByteArrayCreate() {
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableByteArrayWrap() {
        byte[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals((byte) 1, array.get(0));
        Assertions.assertEquals((byte) 2, array.get(1));
        Assertions.assertEquals((byte) 3, array.get(2));
    }

    @Test
    public void testDisposableByteArrayGet() {
        byte[] original = { 10, 20 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals((byte) 10, array.get(0));
        Assertions.assertEquals((byte) 20, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableByteArrayLength() {
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.create(6);
        Assertions.assertEquals(6, array.length());
    }

    @Test
    public void testDisposableByteArrayCopy() {
        byte[] original = { 5, 6, 7 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        byte[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableByteArrayBox() {
        byte[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Byte[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Byte.valueOf((byte) 1), boxed[0]);
        Assertions.assertEquals(Byte.valueOf((byte) 2), boxed[1]);
    }

    @Test
    public void testDisposableByteArrayToList() {
        byte[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        ByteList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals((byte) 10, list.get(0));
        Assertions.assertEquals((byte) 20, list.get(1));
        Assertions.assertEquals((byte) 30, list.get(2));
    }

    @Test
    public void testDisposableByteArrayToCollection() {
        byte[] original = { 3, 4 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        ArrayList<Byte> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Byte.valueOf((byte) 3), list.get(0));
        Assertions.assertEquals(Byte.valueOf((byte) 4), list.get(1));
    }

    @Test
    public void testDisposableByteArraySum() {
        byte[] original = { 1, 2, 3, 4, 5 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals(15, array.sum());
    }

    @Test
    public void testDisposableByteArrayAverage() {
        byte[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableByteArrayMin() {
        byte[] original = { 5, 1, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals((byte) 1, array.min());
    }

    @Test
    public void testDisposableByteArrayMax() {
        byte[] original = { 5, 10, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals((byte) 10, array.max());
    }

    @Test
    public void testDisposableByteArrayForeach() throws Exception {
        byte[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        List<Byte> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), collected);
    }

    @Test
    public void testDisposableByteArrayApply() throws Exception {
        byte[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        int sum = array.apply(arr -> {
            int s = 0;
            for (byte b : arr)
                s += b;
            return s;
        });
        Assertions.assertEquals(6, sum);
    }

    @Test
    public void testDisposableByteArrayAccept() throws Exception {
        byte[] original = { 7, 8, 9 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableByteArrayJoin() {
        byte[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals("1, 2, 3", array.join(", "));
    }

    @Test
    public void testDisposableByteArrayJoinWithPrefixSuffix() {
        byte[] original = { 10, 20 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        Assertions.assertEquals("{10|20}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableByteArrayToString() {
        byte[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableByteArray array = NoCachingNoUpdating.DisposableByteArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1"));
        Assertions.assertTrue(str.contains("2"));
    }

    @Test
    public void testDisposableShortArrayCreate() {
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableShortArrayWrap() {
        short[] original = { 100, 200, 300 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals((short) 100, array.get(0));
        Assertions.assertEquals((short) 200, array.get(1));
        Assertions.assertEquals((short) 300, array.get(2));
    }

    @Test
    public void testDisposableShortArrayGet() {
        short[] original = { 10, 20 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals((short) 10, array.get(0));
        Assertions.assertEquals((short) 20, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableShortArrayLength() {
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.create(7);
        Assertions.assertEquals(7, array.length());
    }

    @Test
    public void testDisposableShortArrayCopy() {
        short[] original = { 5, 6, 7 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        short[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableShortArrayBox() {
        short[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Short[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Short.valueOf((short) 1), boxed[0]);
        Assertions.assertEquals(Short.valueOf((short) 2), boxed[1]);
    }

    @Test
    public void testDisposableShortArrayToList() {
        short[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        ShortList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals((short) 10, list.get(0));
        Assertions.assertEquals((short) 20, list.get(1));
        Assertions.assertEquals((short) 30, list.get(2));
    }

    @Test
    public void testDisposableShortArrayToCollection() {
        short[] original = { 3, 4 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        ArrayList<Short> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Short.valueOf((short) 3), list.get(0));
        Assertions.assertEquals(Short.valueOf((short) 4), list.get(1));
    }

    @Test
    public void testDisposableShortArraySum() {
        short[] original = { 100, 200, 300 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals(600, array.sum());
    }

    @Test
    public void testDisposableShortArrayAverage() {
        short[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableShortArrayMin() {
        short[] original = { 5, 1, 3 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals((short) 1, array.min());
    }

    @Test
    public void testDisposableShortArrayMax() {
        short[] original = { 5, 10, 3 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals((short) 10, array.max());
    }

    @Test
    public void testDisposableShortArrayForeach() throws Exception {
        short[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        List<Short> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList((short) 1, (short) 2, (short) 3), collected);
    }

    @Test
    public void testDisposableShortArrayApply() throws Exception {
        short[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        int sum = array.apply(arr -> {
            int s = 0;
            for (short v : arr)
                s += v;
            return s;
        });
        Assertions.assertEquals(60, sum);
    }

    @Test
    public void testDisposableShortArrayAccept() throws Exception {
        short[] original = { 7, 8, 9 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableShortArrayJoin() {
        short[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals("1, 2, 3", array.join(", "));
    }

    @Test
    public void testDisposableShortArrayJoinWithPrefixSuffix() {
        short[] original = { 10, 20 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        Assertions.assertEquals("{10|20}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableShortArrayToString() {
        short[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableShortArray array = NoCachingNoUpdating.DisposableShortArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1"));
        Assertions.assertTrue(str.contains("2"));
    }

    @Test
    public void testDisposableIntArrayCreate() {
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableIntArrayWrap() {
        int[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals(10, array.get(0));
        Assertions.assertEquals(20, array.get(1));
        Assertions.assertEquals(30, array.get(2));
    }

    @Test
    public void testDisposableIntArrayGet() {
        int[] original = { 100, 200 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals(100, array.get(0));
        Assertions.assertEquals(200, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableIntArrayLength() {
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.create(8);
        Assertions.assertEquals(8, array.length());
    }

    @Test
    public void testDisposableIntArrayCopy() {
        int[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        int[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableIntArrayBox() {
        int[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Integer[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Integer.valueOf(1), boxed[0]);
        Assertions.assertEquals(Integer.valueOf(2), boxed[1]);
    }

    @Test
    public void testDisposableIntArrayToList() {
        int[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        IntList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(10, list.get(0));
        Assertions.assertEquals(20, list.get(1));
        Assertions.assertEquals(30, list.get(2));
    }

    @Test
    public void testDisposableIntArrayToCollection() {
        int[] original = { 3, 4 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        ArrayList<Integer> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Integer.valueOf(3), list.get(0));
        Assertions.assertEquals(Integer.valueOf(4), list.get(1));
    }

    @Test
    public void testDisposableIntArraySum() {
        int[] original = { 10, 20, 30, 40, 50 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals(150, array.sum());
    }

    @Test
    public void testDisposableIntArrayAverage() {
        int[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableIntArrayMin() {
        int[] original = { 5, 1, 3 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals(1, array.min());
    }

    @Test
    public void testDisposableIntArrayMax() {
        int[] original = { 5, 10, 3 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals(10, array.max());
    }

    @Test
    public void testDisposableIntArrayForeach() throws Exception {
        int[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        List<Integer> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testDisposableIntArrayApply() throws Exception {
        int[] original = { 10, 20, 30 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        int sum = array.apply(arr -> {
            int s = 0;
            for (int v : arr)
                s += v;
            return s;
        });
        Assertions.assertEquals(60, sum);
    }

    @Test
    public void testDisposableIntArrayAccept() throws Exception {
        int[] original = { 7, 8, 9 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableIntArrayJoin() {
        int[] original = { 1, 2, 3 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals("1, 2, 3", array.join(", "));
    }

    @Test
    public void testDisposableIntArrayJoinWithPrefixSuffix() {
        int[] original = { 10, 20 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        Assertions.assertEquals("{10|20}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableIntArrayToString() {
        int[] original = { 1, 2 };
        NoCachingNoUpdating.DisposableIntArray array = NoCachingNoUpdating.DisposableIntArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1"));
        Assertions.assertTrue(str.contains("2"));
    }

    @Test
    public void testDisposableLongArrayCreate() {
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableLongArrayWrap() {
        long[] original = { 1000L, 2000L, 3000L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals(1000L, array.get(0));
        Assertions.assertEquals(2000L, array.get(1));
        Assertions.assertEquals(3000L, array.get(2));
    }

    @Test
    public void testDisposableLongArrayGet() {
        long[] original = { 100L, 200L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals(100L, array.get(0));
        Assertions.assertEquals(200L, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableLongArrayLength() {
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.create(9);
        Assertions.assertEquals(9, array.length());
    }

    @Test
    public void testDisposableLongArrayCopy() {
        long[] original = { 1L, 2L, 3L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        long[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableLongArrayBox() {
        long[] original = { 1L, 2L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Long[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Long.valueOf(1L), boxed[0]);
        Assertions.assertEquals(Long.valueOf(2L), boxed[1]);
    }

    @Test
    public void testDisposableLongArrayToList() {
        long[] original = { 10L, 20L, 30L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        LongList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(10L, list.get(0));
        Assertions.assertEquals(20L, list.get(1));
        Assertions.assertEquals(30L, list.get(2));
    }

    @Test
    public void testDisposableLongArrayToCollection() {
        long[] original = { 3L, 4L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        ArrayList<Long> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Long.valueOf(3L), list.get(0));
        Assertions.assertEquals(Long.valueOf(4L), list.get(1));
    }

    @Test
    public void testDisposableLongArraySum() {
        long[] original = { 1000L, 2000L, 3000L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals(6000L, array.sum());
    }

    @Test
    public void testDisposableLongArrayAverage() {
        long[] original = { 10L, 20L, 30L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableLongArrayMin() {
        long[] original = { 5L, 1L, 3L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals(1L, array.min());
    }

    @Test
    public void testDisposableLongArrayMax() {
        long[] original = { 5L, 10L, 3L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals(10L, array.max());
    }

    @Test
    public void testDisposableLongArrayForeach() throws Exception {
        long[] original = { 1L, 2L, 3L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        List<Long> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList(1L, 2L, 3L), collected);
    }

    @Test
    public void testDisposableLongArrayApply() throws Exception {
        long[] original = { 10L, 20L, 30L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        long sum = array.apply(arr -> {
            long s = 0;
            for (long v : arr)
                s += v;
            return s;
        });
        Assertions.assertEquals(60L, sum);
    }

    @Test
    public void testDisposableLongArrayAccept() throws Exception {
        long[] original = { 7L, 8L, 9L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableLongArrayJoin() {
        long[] original = { 1L, 2L, 3L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals("1, 2, 3", array.join(", "));
    }

    @Test
    public void testDisposableLongArrayJoinWithPrefixSuffix() {
        long[] original = { 10L, 20L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        Assertions.assertEquals("{10|20}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableLongArrayToString() {
        long[] original = { 1L, 2L };
        NoCachingNoUpdating.DisposableLongArray array = NoCachingNoUpdating.DisposableLongArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1"));
        Assertions.assertTrue(str.contains("2"));
    }

    @Test
    public void testDisposableFloatArrayCreate() {
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableFloatArrayWrap() {
        float[] original = { 1.5f, 2.5f, 3.5f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals(1.5f, array.get(0));
        Assertions.assertEquals(2.5f, array.get(1));
        Assertions.assertEquals(3.5f, array.get(2));
    }

    @Test
    public void testDisposableFloatArrayGet() {
        float[] original = { 1.1f, 2.2f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals(1.1f, array.get(0));
        Assertions.assertEquals(2.2f, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableFloatArrayLength() {
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.create(10);
        Assertions.assertEquals(10, array.length());
    }

    @Test
    public void testDisposableFloatArrayCopy() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        float[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableFloatArrayBox() {
        float[] original = { 1.0f, 2.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Float[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Float.valueOf(1.0f), boxed[0]);
        Assertions.assertEquals(Float.valueOf(2.0f), boxed[1]);
    }

    @Test
    public void testDisposableFloatArrayToList() {
        float[] original = { 1.1f, 2.2f, 3.3f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        FloatList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1.1f, list.get(0));
        Assertions.assertEquals(2.2f, list.get(1));
        Assertions.assertEquals(3.3f, list.get(2));
    }

    @Test
    public void testDisposableFloatArrayToCollection() {
        float[] original = { 3.0f, 4.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        ArrayList<Float> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Float.valueOf(3.0f), list.get(0));
        Assertions.assertEquals(Float.valueOf(4.0f), list.get(1));
    }

    @Test
    public void testDisposableFloatArraySum() {
        float[] original = { 1.5f, 2.5f, 3.5f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals(7.5f, array.sum(), 0.001f);
    }

    @Test
    public void testDisposableFloatArrayAverage() {
        float[] original = { 1.0f, 2.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals(2.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableFloatArrayMin() {
        float[] original = { 5.0f, 1.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals(1.0f, array.min());
    }

    @Test
    public void testDisposableFloatArrayMax() {
        float[] original = { 5.0f, 10.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals(10.0f, array.max());
    }

    @Test
    public void testDisposableFloatArrayForeach() throws Exception {
        float[] original = { 1.0f, 2.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        List<Float> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
    }

    @Test
    public void testDisposableFloatArrayApply() throws Exception {
        float[] original = { 1.0f, 2.0f, 3.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        float sum = array.apply(arr -> {
            float s = 0;
            for (float v : arr)
                s += v;
            return s;
        });
        Assertions.assertEquals(6.0f, sum, 0.001f);
    }

    @Test
    public void testDisposableFloatArrayAccept() throws Exception {
        float[] original = { 7.0f, 8.0f, 9.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableFloatArrayJoin() {
        float[] original = { 1.1f, 2.2f, 3.3f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals("1.1, 2.2, 3.3", array.join(", "));
    }

    @Test
    public void testDisposableFloatArrayJoinWithPrefixSuffix() {
        float[] original = { 1.0f, 2.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        Assertions.assertEquals("{1.0|2.0}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableFloatArrayToString() {
        float[] original = { 1.0f, 2.0f };
        NoCachingNoUpdating.DisposableFloatArray array = NoCachingNoUpdating.DisposableFloatArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1.0"));
        Assertions.assertTrue(str.contains("2.0"));
    }

    @Test
    public void testDisposableDoubleArrayCreate() {
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.create(5);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(5, array.length());
    }

    @Test
    public void testDisposableDoubleArrayWrap() {
        double[] original = { 1.1, 2.2, 3.3 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertNotNull(array);
        Assertions.assertEquals(3, array.length());
        Assertions.assertEquals(1.1, array.get(0));
        Assertions.assertEquals(2.2, array.get(1));
        Assertions.assertEquals(3.3, array.get(2));
    }

    @Test
    public void testDisposableDoubleArrayGet() {
        double[] original = { 10.5, 20.5 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals(10.5, array.get(0));
        Assertions.assertEquals(20.5, array.get(1));
        Assertions.assertThrows(ArrayIndexOutOfBoundsException.class, () -> array.get(2));
    }

    @Test
    public void testDisposableDoubleArrayLength() {
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.create(11);
        Assertions.assertEquals(11, array.length());
    }

    @Test
    public void testDisposableDoubleArrayCopy() {
        double[] original = { 1.0, 2.0, 3.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        double[] copy = array.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertArrayEquals(original, copy);
    }

    @Test
    public void testDisposableDoubleArrayBox() {
        double[] original = { 1.0, 2.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Double[] boxed = array.box();
        Assertions.assertEquals(2, boxed.length);
        Assertions.assertEquals(Double.valueOf(1.0), boxed[0]);
        Assertions.assertEquals(Double.valueOf(2.0), boxed[1]);
    }

    @Test
    public void testDisposableDoubleArrayToList() {
        double[] original = { 1.1, 2.2, 3.3 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        DoubleList list = array.toList();
        Assertions.assertEquals(3, list.size());
        Assertions.assertEquals(1.1, list.get(0));
        Assertions.assertEquals(2.2, list.get(1));
        Assertions.assertEquals(3.3, list.get(2));
    }

    @Test
    public void testDisposableDoubleArrayToCollection() {
        double[] original = { 3.0, 4.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        ArrayList<Double> list = array.toCollection(ArrayList::new);
        Assertions.assertEquals(2, list.size());
        Assertions.assertEquals(Double.valueOf(3.0), list.get(0));
        Assertions.assertEquals(Double.valueOf(4.0), list.get(1));
    }

    @Test
    public void testDisposableDoubleArraySum() {
        double[] original = { 1.1, 2.2, 3.3, 4.4 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals(11.0, array.sum(), 0.001);
    }

    @Test
    public void testDisposableDoubleArrayAverage() {
        double[] original = { 10.0, 20.0, 30.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals(20.0, array.average(), 0.001);
    }

    @Test
    public void testDisposableDoubleArrayMin() {
        double[] original = { 5.0, 1.0, 3.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals(1.0, array.min());
    }

    @Test
    public void testDisposableDoubleArrayMax() {
        double[] original = { 5.0, 10.0, 3.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals(10.0, array.max());
    }

    @Test
    public void testDisposableDoubleArrayForeach() throws Exception {
        double[] original = { 1.0, 2.0, 3.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        List<Double> collected = new ArrayList<>();
        array.foreach(collected::add);
        Assertions.assertEquals(Arrays.asList(1.0, 2.0, 3.0), collected);
    }

    @Test
    public void testDisposableDoubleArrayApply() throws Exception {
        double[] original = { 1.0, 2.0, 3.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        double sum = array.apply(arr -> {
            double s = 0;
            for (double v : arr)
                s += v;
            return s;
        });
        Assertions.assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void testDisposableDoubleArrayAccept() throws Exception {
        double[] original = { 7.0, 8.0, 9.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        boolean[] called = { false };
        array.accept(arr -> {
            called[0] = true;
            Assertions.assertArrayEquals(original, arr);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableDoubleArrayJoin() {
        double[] original = { 1.1, 2.2, 3.3 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals("1.1, 2.2, 3.3", array.join(", "));
    }

    @Test
    public void testDisposableDoubleArrayJoinWithPrefixSuffix() {
        double[] original = { 1.0, 2.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        Assertions.assertEquals("{1.0|2.0}", array.join("|", "{", "}"));
    }

    @Test
    public void testDisposableDoubleArrayToString() {
        double[] original = { 1.0, 2.0 };
        NoCachingNoUpdating.DisposableDoubleArray array = NoCachingNoUpdating.DisposableDoubleArray.wrap(original);
        String str = array.toString();
        Assertions.assertNotNull(str);
        Assertions.assertTrue(str.contains("1.0"));
        Assertions.assertTrue(str.contains("2.0"));
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
    public void testDisposableEntryWrap() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertNotNull(entry);
        Assertions.assertEquals("key", entry.getKey());
        Assertions.assertEquals(Integer.valueOf(100), entry.getValue());
    }

    @Test
    public void testDisposableEntrySetValueUnsupported() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> entry.setValue(200));
    }

    @Test
    public void testDisposableEntryCopy() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("test", 42);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Map.Entry<String, Integer> copy = entry.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals("test", copy.getKey());
        Assertions.assertEquals(Integer.valueOf(42), copy.getValue());
    }

    @Test
    public void testDisposableEntryApplyWithFunction() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("age", 25);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        String result = entry.apply(e -> e.getKey() + "=" + e.getValue());
        Assertions.assertEquals("age=25", result);
    }

    @Test
    public void testDisposableEntryApplyWithBiFunction() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("count", 10);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        String result = entry.apply((k, v) -> k + " is " + v);
        Assertions.assertEquals("count is 10", result);
    }

    @Test
    public void testDisposableEntryAcceptWithConsumer() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("value", 100);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        boolean[] called = { false };
        entry.accept(e -> {
            called[0] = true;
            Assertions.assertEquals("value", e.getKey());
            Assertions.assertEquals(Integer.valueOf(100), e.getValue());
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableEntryAcceptWithBiConsumer() throws Exception {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("test", 50);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        boolean[] called = { false };
        entry.accept((k, v) -> {
            called[0] = true;
            Assertions.assertEquals("test", k);
            Assertions.assertEquals(Integer.valueOf(50), v);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableEntryToString() {
        Map.Entry<String, Integer> original = new AbstractMap.SimpleEntry<>("key", 123);
        NoCachingNoUpdating.DisposableEntry<String, Integer> entry = NoCachingNoUpdating.DisposableEntry.wrap(original);
        Assertions.assertEquals("key=123", entry.toString());
    }

    @Test
    public void testDisposablePairWrap() {
        Pair<String, Integer> original = Pair.of("left", 100);
        NoCachingNoUpdating.DisposablePair<String, Integer> pair = NoCachingNoUpdating.DisposablePair.wrap(original);
        Assertions.assertNotNull(pair);
        Assertions.assertEquals("left", pair.left());
        Assertions.assertEquals(Integer.valueOf(100), pair.right());
    }

    @Test
    public void testDisposablePairCopy() {
        Pair<String, Boolean> original = Pair.of("test", true);
        NoCachingNoUpdating.DisposablePair<String, Boolean> pair = NoCachingNoUpdating.DisposablePair.wrap(original);
        Pair<String, Boolean> copy = pair.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals("test", copy.left());
        Assertions.assertEquals(Boolean.TRUE, copy.right());
    }

    @Test
    public void testDisposablePairApply() throws Exception {
        Pair<Integer, Integer> original = Pair.of(3, 4);
        NoCachingNoUpdating.DisposablePair<Integer, Integer> pair = NoCachingNoUpdating.DisposablePair.wrap(original);
        Integer sum = pair.apply((l, r) -> l + r);
        Assertions.assertEquals(Integer.valueOf(7), sum);
    }

    @Test
    public void testDisposablePairAccept() throws Exception {
        Pair<String, Double> original = Pair.of("pi", 3.14);
        NoCachingNoUpdating.DisposablePair<String, Double> pair = NoCachingNoUpdating.DisposablePair.wrap(original);
        boolean[] called = { false };
        pair.accept((l, r) -> {
            called[0] = true;
            Assertions.assertEquals("pi", l);
            Assertions.assertEquals(3.14, r, 0.001);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposablePairToString() {
        Pair<String, Integer> original = Pair.of("abc", 123);
        NoCachingNoUpdating.DisposablePair<String, Integer> pair = NoCachingNoUpdating.DisposablePair.wrap(original);
        String str = pair.toString();
        Assertions.assertEquals("[abc, 123]", str);
    }

    @Test
    public void testDisposableTripleWrap() {
        Triple<String, Integer, Boolean> original = Triple.of("left", 100, true);
        NoCachingNoUpdating.DisposableTriple<String, Integer, Boolean> triple = NoCachingNoUpdating.DisposableTriple.wrap(original);
        Assertions.assertNotNull(triple);
        Assertions.assertEquals("left", triple.left());
        Assertions.assertEquals(Integer.valueOf(100), triple.middle());
        Assertions.assertEquals(Boolean.TRUE, triple.right());
    }

    @Test
    public void testDisposableTripleCopy() {
        Triple<String, String, String> original = Triple.of("a", "b", "c");
        NoCachingNoUpdating.DisposableTriple<String, String, String> triple = NoCachingNoUpdating.DisposableTriple.wrap(original);
        Triple<String, String, String> copy = triple.copy();
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals("a", copy.left());
        Assertions.assertEquals("b", copy.middle());
        Assertions.assertEquals("c", copy.right());
    }

    @Test
    public void testDisposableTripleApply() throws Exception {
        Triple<Integer, Integer, Integer> original = Triple.of(1, 2, 3);
        NoCachingNoUpdating.DisposableTriple<Integer, Integer, Integer> triple = NoCachingNoUpdating.DisposableTriple.wrap(original);
        Integer sum = triple.apply((l, m, r) -> l + m + r);
        Assertions.assertEquals(Integer.valueOf(6), sum);
    }

    @Test
    public void testDisposableTripleAccept() throws Exception {
        Triple<String, String, String> original = Triple.of("x", "y", "z");
        NoCachingNoUpdating.DisposableTriple<String, String, String> triple = NoCachingNoUpdating.DisposableTriple.wrap(original);
        boolean[] called = { false };
        triple.accept((l, m, r) -> {
            called[0] = true;
            Assertions.assertEquals("x", l);
            Assertions.assertEquals("y", m);
            Assertions.assertEquals("z", r);
        });
        Assertions.assertTrue(called[0]);
    }

    @Test
    public void testDisposableTripleToString() {
        Triple<Integer, Integer, Integer> original = Triple.of(1, 2, 3);
        NoCachingNoUpdating.DisposableTriple<Integer, Integer, Integer> triple = NoCachingNoUpdating.DisposableTriple.wrap(original);
        String str = triple.toString();
        Assertions.assertEquals("[1, 2, 3]", str);
    }

    @Test
    public void testTimedOf() {
        long timestamp = System.currentTimeMillis();
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("Hello", timestamp);
        Assertions.assertNotNull(timed);
        Assertions.assertEquals("Hello", timed.value());
        Assertions.assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimedValue() {
        NoCachingNoUpdating.Timed<Integer> timed = NoCachingNoUpdating.Timed.of(42, 1000L);
        Assertions.assertEquals(Integer.valueOf(42), timed.value());
    }

    @Test
    public void testTimedTimestamp() {
        long timestamp = 123456789L;
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("test", timestamp);
        Assertions.assertEquals(timestamp, timed.timestamp());
    }

    @Test
    public void testTimedHashCode() {
        long timestamp = 1000L;
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of("test", timestamp);
        Assertions.assertEquals(timed1.hashCode(), timed2.hashCode());
    }

    @Test
    public void testTimedEquals() {
        long timestamp = 1000L;
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of("test", timestamp);
        NoCachingNoUpdating.Timed<String> timed3 = NoCachingNoUpdating.Timed.of("other", timestamp);
        NoCachingNoUpdating.Timed<String> timed4 = NoCachingNoUpdating.Timed.of("test", 2000L);

        Assertions.assertEquals(timed1, timed1);
        Assertions.assertEquals(timed1, timed2);
        Assertions.assertNotEquals(timed1, timed3);
        Assertions.assertNotEquals(timed1, timed4);
        Assertions.assertNotEquals(timed1, null);
        Assertions.assertNotEquals(timed1, "string");
    }

    @Test
    public void testTimedEqualsWithNull() {
        NoCachingNoUpdating.Timed<String> timed1 = NoCachingNoUpdating.Timed.of(null, 1000L);
        NoCachingNoUpdating.Timed<String> timed2 = NoCachingNoUpdating.Timed.of(null, 1000L);
        NoCachingNoUpdating.Timed<String> timed3 = NoCachingNoUpdating.Timed.of("test", 1000L);

        Assertions.assertEquals(timed1, timed2);
        Assertions.assertNotEquals(timed1, timed3);
    }

    @Test
    public void testTimedToString() {
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of("value", 12345L);
        String str = timed.toString();
        Assertions.assertEquals("12345: value", str);
    }

    @Test
    public void testTimedToStringWithNull() {
        NoCachingNoUpdating.Timed<String> timed = NoCachingNoUpdating.Timed.of(null, 12345L);
        String str = timed.toString();
        Assertions.assertEquals("12345: null", str);
    }

    @Test
    public void testDisposableArray_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableArray.wrap(null));
    }

    @Test
    public void testDisposableArray_emptyArray() {
        String[] arr = new String[0];
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals(0, array.length());

        List<String> list = array.toList();
        assertTrue(list.isEmpty());

        Set<String> set = array.toSet();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testDisposableArray_withNullElements() {
        String[] arr = { "a", null, "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals("a", array.get(0));
        assertNull(array.get(1));
        assertEquals("c", array.get(2));

        List<String> list = array.toList();
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test
    public void testDisposableArray_largeArray() {
        Integer[] arr = new Integer[1000];
        for (int i = 0; i < 1000; i++) {
            arr[i] = i;
        }
        DisposableArray<Integer> array = DisposableArray.wrap(arr);
        assertEquals(1000, array.length());
        assertEquals(Integer.valueOf(999), array.get(999));
    }

    @Test
    public void testDisposableArray_toArrayWithLargerTarget() {
        String[] arr = { "a", "b" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        String[] target = new String[5];
        Arrays.fill(target, "x");
        String[] result = array.toArray(target);

        assertSame(target, result);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("x", result[2]);
        assertEquals("x", result[3]);
        assertEquals("x", result[4]);
    }

    @Test
    public void testDisposableArray_functionalInterfaceExceptions() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        try {
            array.apply(a -> {
                throw new RuntimeException("Test exception");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        try {
            array.accept(a -> {
                throw new RuntimeException("Test exception");
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }

        try {
            array.forEach(e -> {
                if ("b".equals(e)) {
                    throw new RuntimeException("Test exception at b");
                }
            });
            fail("Should have thrown exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception at b", e.getMessage());
        }
    }

    @Test
    public void testDisposableBooleanArray_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableBooleanArray.wrap(null));
    }

    @Test
    public void testDisposableBooleanArray_emptyArray() {
        boolean[] arr = new boolean[0];
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        assertEquals(0, array.length());

        Boolean[] boxed = array.box();
        assertEquals(0, boxed.length);

        BooleanList list = array.toList();
        assertEquals(0, list.size());
    }

    @Test
    public void testDisposableCharArray_emptyArray() {
        char[] arr = new char[0];
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals(0, array.length());

        assertEquals(0, array.sum());
        assertTrue(Numbers.fuzzyEquals(array.average(), 0, 0.0001));
    }

    @Test
    public void testDisposableIntArray_overflowSum() {
        int[] arr = { Integer.MAX_VALUE, 1 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertThrows(ArithmeticException.class, () -> array.sum());
    }

    @Test
    public void testDisposableLongArray_largeValues() {
        long[] arr = { Long.MAX_VALUE, Long.MIN_VALUE, 0 };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(Long.MAX_VALUE, array.max());
        assertEquals(Long.MIN_VALUE, array.min());
        assertEquals(-1L, array.sum());
    }

    @Test
    public void testDisposableFloatArray_specialValues() {
        float[] arr = { Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f, -0.0f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(5, array.length());

        assertTrue(Float.isNaN(array.get(0)));
        assertTrue(Float.isInfinite(array.get(1)));
        assertTrue(Float.isInfinite(array.get(2)));

        float sum = array.sum();
        assertTrue(Float.isNaN(sum));
    }

    @Test
    public void testDisposableDoubleArray_precisionTests() {
        double[] arr = { 0.1, 0.2, 0.3 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);

        double sum = array.sum();
        assertEquals(0.6, sum, 0.0000001);

        double avg = array.average();
        assertEquals(0.2, avg, 0.0000001);
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
    public void testDisposableEntry_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableEntry.wrap(null));
    }

    @Test
    public void testDisposableEntry_withNullKeyValue() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>(null, 100);
        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        assertNull(disposable1.getKey());
        assertEquals(Integer.valueOf(100), disposable1.getValue());

        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", null);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);
        assertEquals("key", disposable2.getKey());
        assertNull(disposable2.getValue());

        Map.Entry<String, Integer> entry3 = new AbstractMap.SimpleEntry<>(null, null);
        DisposableEntry<String, Integer> disposable3 = DisposableEntry.wrap(entry3);
        assertNull(disposable3.getKey());
        assertNull(disposable3.getValue());
    }

    @Test
    public void testDisposableEntry_hashCodeAndEquals() {
        Map.Entry<String, Integer> entry1 = new AbstractMap.SimpleEntry<>("key", 100);
        Map.Entry<String, Integer> entry2 = new AbstractMap.SimpleEntry<>("key", 100);

        DisposableEntry<String, Integer> disposable1 = DisposableEntry.wrap(entry1);
        DisposableEntry<String, Integer> disposable2 = DisposableEntry.wrap(entry2);

        assertNotEquals(disposable1.hashCode(), disposable2.hashCode());
        assertNotEquals(disposable1, disposable2);
    }

    @Test
    public void testDisposablePair_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposablePair.wrap(null));
    }

    @Test
    public void testDisposablePair_withNullElements() {
        Pair<String, Integer> pair1 = Pair.of(null, 100);
        DisposablePair<String, Integer> disposable1 = DisposablePair.wrap(pair1);
        assertNull(disposable1.left());
        assertEquals(Integer.valueOf(100), disposable1.right());

        Pair<String, Integer> pair2 = Pair.of("left", null);
        DisposablePair<String, Integer> disposable2 = DisposablePair.wrap(pair2);
        assertEquals("left", disposable2.left());
        assertNull(disposable2.right());

        Pair<String, Integer> pair3 = Pair.of(null, null);
        DisposablePair<String, Integer> disposable3 = DisposablePair.wrap(pair3);
        assertNull(disposable3.left());
        assertNull(disposable3.right());
    }

    @Test
    public void testDisposableTriple_wrapNull() {
        assertThrows(IllegalArgumentException.class, () -> DisposableTriple.wrap(null));
    }

    @Test
    public void testDisposableTriple_withNullElements() {
        Triple<String, Integer, Boolean> triple1 = Triple.of(null, 100, true);
        DisposableTriple<String, Integer, Boolean> disposable1 = DisposableTriple.wrap(triple1);
        assertNull(disposable1.left());
        assertEquals(Integer.valueOf(100), disposable1.middle());
        assertEquals(Boolean.TRUE, disposable1.right());

        Triple<String, Integer, Boolean> triple2 = Triple.of("left", null, true);
        DisposableTriple<String, Integer, Boolean> disposable2 = DisposableTriple.wrap(triple2);
        assertEquals("left", disposable2.left());
        assertNull(disposable2.middle());
        assertEquals(Boolean.TRUE, disposable2.right());

        Triple<String, Integer, Boolean> triple3 = Triple.of("left", 100, null);
        DisposableTriple<String, Integer, Boolean> disposable3 = DisposableTriple.wrap(triple3);
        assertEquals("left", disposable3.left());
        assertEquals(Integer.valueOf(100), disposable3.middle());
        assertNull(disposable3.right());

        Triple<String, Integer, Boolean> triple4 = Triple.of(null, null, null);
        DisposableTriple<String, Integer, Boolean> disposable4 = DisposableTriple.wrap(triple4);
        assertNull(disposable4.left());
        assertNull(disposable4.middle());
        assertNull(disposable4.right());
    }

    @Test
    public void testTimed_hashCodeWithNullValue() {
        Timed<String> timed1 = Timed.of(null, 1000L);
        Timed<String> timed2 = Timed.of(null, 1000L);
        assertEquals(timed1.hashCode(), timed2.hashCode());

        Timed<String> timed3 = Timed.of("value", 1000L);
        assertNotEquals(timed1.hashCode(), timed3.hashCode());
    }

    @Test
    public void testTimed_equalsEdgeCases() {
        Timed<String> timed = Timed.of("value", 1000L);

        assertTrue(timed.equals(timed));

        assertFalse(timed.equals(null));

        assertFalse(timed.equals("not a timed"));
        assertFalse(timed.equals(new Object()));

        Timed<String> different1 = Timed.of("value", 2000L);
        assertFalse(timed.equals(different1));

        Timed<String> different2 = Timed.of("other", 1000L);
        assertFalse(timed.equals(different2));

        Timed<String> different3 = Timed.of("other", 2000L);
        assertFalse(timed.equals(different3));
    }

    @Test
    public void testDisposableArray_performanceWithLargeData() {
        int size = 100000;
        Integer[] arr = new Integer[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }

        DisposableArray<Integer> array = DisposableArray.wrap(arr);

        long sum = 0;
        for (int i = 0; i < size; i++) {
            sum += array.get(i);
        }

        long expectedSum = (long) size * (size - 1) / 2;
        assertEquals(expectedSum, sum);

        final long[] forEachSum = { 0 };
        array.forEach(i -> forEachSum[0] += i);
        assertEquals(expectedSum, forEachSum[0]);
    }

    @Test
    public void testPrimitiveArrays_boundaryValues() {
        byte[] byteArr = { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE };
        DisposableByteArray byteArray = DisposableByteArray.wrap(byteArr);
        assertEquals(Byte.MIN_VALUE, byteArray.min());
        assertEquals(Byte.MAX_VALUE, byteArray.max());

        short[] shortArr = { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE };
        DisposableShortArray shortArray = DisposableShortArray.wrap(shortArr);
        assertEquals(Short.MIN_VALUE, shortArray.min());
        assertEquals(Short.MAX_VALUE, shortArray.max());

        int[] intArr = { Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE };
        DisposableIntArray intArray = DisposableIntArray.wrap(intArr);
        assertEquals(Integer.MIN_VALUE, intArray.min());
        assertEquals(Integer.MAX_VALUE, intArray.max());

        long[] longArr = { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE };
        DisposableLongArray longArray = DisposableLongArray.wrap(longArr);
        assertEquals(Long.MIN_VALUE, longArray.min());
        assertEquals(Long.MAX_VALUE, longArray.max());
    }

    @Test
    public void testJoinOperations_specialCharacters() {
        String[] arr = { "a\"b", "c,d", "e\nf", "g\th" };
        DisposableArray<String> array = DisposableArray.wrap(arr);

        assertEquals("a\"b,c,d,e\nf,g\th", array.join(","));
        assertEquals("[a\"b|c,d|e\nf|g\th]", array.join("|", "[", "]"));

        assertEquals("a\"bc,de\nfg\th", array.join(""));

        String[] arrWithNull = { "a", null, "b" };
        DisposableArray<String> arrayWithNull = DisposableArray.wrap(arrWithNull);
        assertEquals("a,null,b", arrayWithNull.join(","));
    }

    @Test
    public void testCollectionConversions_customSuppliers() {
        Integer[] arr = { 1, 2, 3, 2, 1 };
        DisposableArray<Integer> array = DisposableArray.wrap(arr);

        LinkedHashSet<Integer> linkedSet = array.toCollection(LinkedHashSet::new);
        assertEquals(3, linkedSet.size());
        Iterator<Integer> iter = linkedSet.iterator();
        assertEquals(Integer.valueOf(1), iter.next());
        assertEquals(Integer.valueOf(2), iter.next());
        assertEquals(Integer.valueOf(3), iter.next());

        TreeSet<Integer> treeSet = array.toCollection(IntFunctions.ofTreeSet());
        assertEquals(3, treeSet.size());
        assertEquals(Integer.valueOf(1), treeSet.first());
        assertEquals(Integer.valueOf(3), treeSet.last());

        ArrayList<Integer> customList = array.toCollection(size -> new ArrayList<>(size * 2));
        assertEquals(5, customList.size());
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
    public void testDisposableArray_forEach() throws Exception {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        List<String> result = new ArrayList<>();
        array.foreach(e -> result.add(e));
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testDisposableArray_join() {
        String[] arr = { "a", "b", "c" };
        DisposableArray<String> array = DisposableArray.wrap(arr);
        assertEquals("a,b,c", array.join(","));
        assertEquals("[a|b|c]", array.join("|", "[", "]"));
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
    public void testDisposableBooleanArray_wrap() {
        boolean[] arr = { true, false, true };
        DisposableBooleanArray array = DisposableBooleanArray.wrap(arr);
        assertEquals(3, array.length());
        assertTrue(array.get(0));
        assertFalse(array.get(1));
        assertTrue(array.get(2));
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
    public void testDisposableCharArray_wrap() {
        char[] arr = { 'a', 'b', 'c' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals(3, array.length());
        assertEquals('a', array.get(0));
        assertEquals('b', array.get(1));
        assertEquals('c', array.get(2));
    }

    @Test
    public void testDisposableCharArray_minMax() {
        char[] arr = { 'c', 'a', 'b' };
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals('a', array.min());
        assertEquals('c', array.max());
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
    public void testDisposableByteArray_minMax() {
        byte[] arr = { 3, 1, 2 };
        DisposableByteArray array = DisposableByteArray.wrap(arr);
        assertEquals(1, array.min());
        assertEquals(3, array.max());
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
    public void testDisposableShortArray_minMax() {
        short[] arr = { 30, 10, 20 };
        DisposableShortArray array = DisposableShortArray.wrap(arr);
        assertEquals(10, array.min());
        assertEquals(30, array.max());
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
    public void testDisposableIntArray_minMax() {
        int[] arr = { 300, 100, 200 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertEquals(100, array.min());
        assertEquals(300, array.max());
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
    public void testDisposableLongArray_minMax() {
        long[] arr = { 3000L, 1000L, 2000L };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(1000L, array.min());
        assertEquals(3000L, array.max());
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
    public void testDisposableFloatArray_minMax() {
        float[] arr = { 3.5f, 1.5f, 2.5f };
        DisposableFloatArray array = DisposableFloatArray.wrap(arr);
        assertEquals(1.5f, array.min(), 0.001f);
        assertEquals(3.5f, array.max(), 0.001f);
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
    public void testDisposablePair_wrap() {
        Pair<String, Integer> original = Pair.of("left", 100);
        DisposablePair<String, Integer> pair = DisposablePair.wrap(original);

        assertEquals("left", pair.left());
        assertEquals(Integer.valueOf(100), pair.right());
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
    public void testTimed_withNullValue() {
        Timed<String> timed = Timed.of(null, 1000L);
        assertNull(timed.value());
        assertEquals(1000L, timed.timestamp());

        Timed<String> timed2 = Timed.of(null, 1000L);
        assertTrue(timed.equals(timed2));
    }

}
