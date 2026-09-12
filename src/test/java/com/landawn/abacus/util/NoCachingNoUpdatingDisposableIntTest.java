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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableIntArray;

public class NoCachingNoUpdatingDisposableIntTest extends NoCachingNoUpdatingTestSupport {
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
            for (int v : arr) {
                s += v;
            }
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
    public void testDisposableIntArray_overflowSum() {
        int[] arr = { Integer.MAX_VALUE, 1 };
        DisposableIntArray array = DisposableIntArray.wrap(arr);
        assertThrows(ArithmeticException.class, () -> array.sum());
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
}
