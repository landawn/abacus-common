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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableLongArray;

public class NoCachingNoUpdatingDisposableLongTest extends NoCachingNoUpdatingTestSupport {
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
            for (long v : arr) {
                s += v;
            }
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
    public void testDisposableLongArray_largeValues() {
        long[] arr = { Long.MAX_VALUE, Long.MIN_VALUE, 0 };
        DisposableLongArray array = DisposableLongArray.wrap(arr);
        assertEquals(Long.MAX_VALUE, array.max());
        assertEquals(Long.MIN_VALUE, array.min());
        assertEquals(-1L, array.sum());
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
}
