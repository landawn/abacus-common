package com.landawn.abacus.util;

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;

@Tag("new-test")
public class NoCachingNoUpdating102Test extends TestBase {

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
    public void testDisposableObjArrayCreateUnsupported() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> NoCachingNoUpdating.DisposableObjArray.create(String.class, 5));
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
}
