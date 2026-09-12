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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableByteArray;

public class NoCachingNoUpdatingDisposableByteTest extends NoCachingNoUpdatingTestSupport {
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
    public void testDisposableByteArray_overflowSum() {
        // The smallest all-Byte.MAX_VALUE array whose sum is past Integer.MAX_VALUE.
        final byte[] data = new byte[Integer.MAX_VALUE / Byte.MAX_VALUE + 1];
        Arrays.fill(data, Byte.MAX_VALUE);
        final DisposableByteArray array = DisposableByteArray.wrap(data);
        assertThrows(ArithmeticException.class, () -> array.sum());
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
            for (byte b : arr) {
                s += b;
            }
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
}
