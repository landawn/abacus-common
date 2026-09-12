package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableShortArray;

public class NoCachingNoUpdatingDisposableShortTest extends NoCachingNoUpdatingTestSupport {
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
    public void testDisposableShortArray_overflowSum() {
        // 70000 * 32767 = 2_293_690_000, past Integer.MAX_VALUE.
        final short[] data = new short[70000];
        Arrays.fill(data, Short.MAX_VALUE);
        final DisposableShortArray array = DisposableShortArray.wrap(data);
        assertThrows(ArithmeticException.class, () -> array.sum());
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
            for (short v : arr) {
                s += v;
            }
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
}
