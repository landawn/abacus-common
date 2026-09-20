package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;

public class NoCachingNoUpdatingDisposableArrayTest extends NoCachingNoUpdatingTestSupport {
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
        assertNull(result[2]);
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
    public void testDisposableArrayCreateRejectsNullComponentType() {
        assertThrows(IllegalArgumentException.class, () -> DisposableArray.create(null, 0));
    }

    /**
     * A primitive component type must be rejected with a documented {@code IllegalArgumentException} rather than
     * failing with a {@code ClassCastException} from inside the constructor.
     *
     * <p>{@code DisposableArray} holds a reference array; {@code N.newArray(int.class, len)} produces an
     * {@code int[]}, which cannot be cast to {@code Object[]}. Both documented preconditions (non-null type,
     * non-negative length) were satisfied, so the failure gave the caller no usable diagnosis.</p>
     */
    @Test
    public void testCreate_PrimitiveComponentTypeIsRejected() {
        for (final Class<?> primitive : new Class<?>[] { int.class, long.class, double.class, char.class, boolean.class, byte.class, short.class,
                float.class }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> DisposableArray.create(primitive, 3));
            assertTrue(e.getMessage().contains("primitive"), "message should name the problem: " + e.getMessage());
        }

        // the length check still runs first, and a reference type still works
        assertThrows(IllegalArgumentException.class, () -> DisposableArray.create(int.class, -1));
        assertEquals(3, DisposableArray.create(String.class, 3).length());
    }
}
