package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableBooleanArray;

public class NoCachingNoUpdatingDisposableBooleanTest extends NoCachingNoUpdatingTestSupport {
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
            for (boolean b : arr) {
                if (b) {
                    c++;
                }
            }
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
}
