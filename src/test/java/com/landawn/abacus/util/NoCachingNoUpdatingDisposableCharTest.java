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

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableCharArray;

public class NoCachingNoUpdatingDisposableCharTest extends NoCachingNoUpdatingTestSupport {
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
    public void testDisposableCharArray_overflowSum() {
        // 40000 * 0xFFFF = 2_621_400_000, past Integer.MAX_VALUE.
        final char[] data = new char[40000];
        Arrays.fill(data, Character.MAX_VALUE);
        final DisposableCharArray array = DisposableCharArray.wrap(data);
        assertThrows(ArithmeticException.class, () -> array.sum());
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
    public void testDisposableCharArray_emptyArray() {
        char[] arr = new char[0];
        DisposableCharArray array = DisposableCharArray.wrap(arr);
        assertEquals(0, array.length());

        assertEquals(0, array.sum());
        assertTrue(Numbers.fuzzyEquals(array.average(), 0, 0.0001));
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
}
