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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableDoubleArray;

public class NoCachingNoUpdatingDisposableDoubleTest extends NoCachingNoUpdatingTestSupport {
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
            for (double v : arr) {
                s += v;
            }
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
    public void testDisposableDoubleArray_precisionTests() {
        double[] arr = { 0.1, 0.2, 0.3 };
        DisposableDoubleArray array = DisposableDoubleArray.wrap(arr);

        double sum = array.sum();
        assertEquals(0.6, sum, 0.0000001);

        double avg = array.average();
        assertEquals(0.2, avg, 0.0000001);
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
    public void testDisposableDoubleArray_toString_rendersEachElementAsADoubleLiteral() {
        assertEquals("[1.0, 2.0, 3.0]", DisposableDoubleArray.wrap(new double[] { 1, 2, 3 }).toString());
        assertEquals("[99.0]", DisposableDoubleArray.wrap(new double[] { 99 }).toString());
        assertEquals("[]", DisposableDoubleArray.wrap(new double[0]).toString());
    }
}
