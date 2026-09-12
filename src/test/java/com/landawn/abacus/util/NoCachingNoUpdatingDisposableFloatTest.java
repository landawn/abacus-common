package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableFloatArray;

public class NoCachingNoUpdatingDisposableFloatTest extends NoCachingNoUpdatingTestSupport {
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
            for (float v : arr) {
                s += v;
            }
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
    public void testDisposableFloatArray_toString_rendersEachElementAsAFloatLiteral() {
        assertEquals("[1.0, 2.0, 3.0]", DisposableFloatArray.wrap(new float[] { 1, 2, 3 }).toString());
        assertEquals("[99.0]", DisposableFloatArray.wrap(new float[] { 99 }).toString());
        assertEquals("[]", DisposableFloatArray.wrap(new float[0]).toString());
    }
}
