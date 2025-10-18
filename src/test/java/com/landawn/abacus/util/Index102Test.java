package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("new-test")
public class Index102Test extends TestBase {

    @Test
    public void testOf_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.of(array, true);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.of(array, false);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(null, true);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new boolean[0], true);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.of(array, true, 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, false, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, true, 5);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, true, -1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOf_CharArray() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.of(array, 'b');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 'e');
        Assertions.assertFalse(result.isPresent());

        result = Index.of((char[]) null, 'a');
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new char[0], 'a');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_CharArray_WithFromIndex() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.of(array, 'b', 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, 'a', 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, 'b', -1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testOf_ByteArray() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.of(array, (byte) 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, (byte) 5);
        Assertions.assertFalse(result.isPresent());

        result = Index.of((byte[]) null, (byte) 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ByteArray_WithFromIndex() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.of(array, (byte) 2, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, (byte) 1, 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ShortArray() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.of(array, (short) 20);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, (short) 50);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_ShortArray_WithFromIndex() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.of(array, (short) 20, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_IntArray() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.of(array, 200);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 500);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_IntArray_WithFromIndex() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.of(array, 200, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.of(array, 2000L);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5000L);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_LongArray_WithFromIndex() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.of(array, 2000L, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.of(array, 2.2f);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5.5f);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_FloatArray_WithFromIndex() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.of(array, 2.2f, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.of(array, 2.22);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 5.55);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_DoubleArray_WithFromIndex() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.of(array, 2.22, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.of(array, 2.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, 3.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, 2.5, 0.1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_DoubleArray_WithToleranceAndFromIndex() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.of(array, 3.0, 0.01, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.of(array, 3.0, 0.01, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.of(array, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(array, "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(array, null);
        Assertions.assertFalse(result.isPresent());

        String[] arrayWithNull = { "apple", null, "cherry" };
        result = Index.of(arrayWithNull, null);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testOf_ObjectArray_WithFromIndex() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.of(array, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(array, "apple", 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(list, "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((Collection<?>) null, "apple");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(new ArrayList<>(), "apple");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Collection_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(list, "apple", 1);
        Assertions.assertFalse(result.isPresent());

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.of(linkedList, "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOf_Iterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list.iterator(), "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.of(list.iterator(), "grape");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((Iterator<?>) null, "apple");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_Iterator_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.of(list.iterator(), "banana", 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.of(list.iterator(), "apple", 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_Char() {
        String str = "hello world";

        OptionalInt result = Index.of(str, 'o');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());

        result = Index.of(str, 'z');
        Assertions.assertFalse(result.isPresent());

        result = Index.of((String) null, 'a');
        Assertions.assertFalse(result.isPresent());

        result = Index.of("", 'a');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_Char_WithFromIndex() {
        String str = "hello world";

        OptionalInt result = Index.of(str, 'o', 5);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        result = Index.of(str, 'h', 1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_String() {
        String str = "hello world hello";

        OptionalInt result = Index.of(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.of(str, "world");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());

        result = Index.of(str, "goodbye");
        Assertions.assertFalse(result.isPresent());

        result = Index.of((String) null, "hello");
        Assertions.assertFalse(result.isPresent());

        result = Index.of(str, null);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOf_String_String_WithFromIndex() {
        String str = "hello world hello";

        OptionalInt result = Index.of(str, "hello", 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.of(str, "world", 10);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfIgnoreCase_String() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.ofIgnoreCase(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofIgnoreCase(str, "WORLD");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());

        result = Index.ofIgnoreCase(str, "goodbye");
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfIgnoreCase_String_WithFromIndex() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.ofIgnoreCase(str, "hello", 1);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.ofIgnoreCase(str, "world", 10);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubArray_BooleanArray() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        boolean[] subArray2 = { false, true };
        result = Index.ofSubArray(array, subArray2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        boolean[] notFound = { false, false, false };
        result = Index.ofSubArray(array, notFound);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(null, subArray);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, null);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, new boolean[0]);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOfSubArray_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.ofSubArray(array, 2, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.ofSubArray(array, 4, subArray);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubArray(array, -1, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOfSubArray_BooleanArray_WithRange() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { false, true, false, true };

        OptionalInt result = Index.ofSubArray(array, 0, subArray, 1, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 0, subArray, 0, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.ofSubArray(array, 0, subArray, 0, 0);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 10, subArray, 0, 0);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testOfSubArray_CharArray() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        char[] subArray = { 'c', 'd' };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ByteArray() {
        byte[] array = { 1, 2, 3, 4, 5 };
        byte[] subArray = { 3, 4 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ShortArray() {
        short[] array = { 10, 20, 30, 40, 50 };
        short[] subArray = { 30, 40 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_IntArray() {
        int[] array = { 100, 200, 300, 400, 500 };
        int[] subArray = { 300, 400 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 4000L, 5000L };
        long[] subArray = { 3000L, 4000L };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 4.4f, 5.5f };
        float[] subArray = { 3.3f, 4.4f };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 4.44, 5.55 };
        double[] subArray = { 3.33, 4.44 };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubArray_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "date", "elderberry" };
        String[] subArray = { "cherry", "date" };

        OptionalInt result = Index.ofSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        String[] notFound = { "banana", "date" };
        result = Index.ofSubArray(array, notFound);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubList() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        List<String> subList = Arrays.asList("cherry", "date");

        OptionalInt result = Index.ofSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        List<String> notFound = Arrays.asList("banana", "elderberry");
        result = Index.ofSubList(list, notFound);
        Assertions.assertFalse(result.isPresent());

        result = Index.ofSubList(list, new ArrayList<>());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());
    }

    @Test
    public void testOfSubList_WithFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "cherry");
        List<String> subList = Arrays.asList("apple", "banana");

        OptionalInt result = Index.ofSubList(list, 1, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.ofSubList(list, 3, subList);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testOfSubList_WithRange() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        List<String> subList = Arrays.asList("extra", "cherry", "date", "extra");

        OptionalInt result = Index.ofSubList(list, 0, subList, 1, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testOfSubList_NonRandomAccess() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("apple", "banana", "cherry", "date"));
        LinkedList<String> subList = new LinkedList<>(Arrays.asList("banana", "cherry"));

        OptionalInt result = Index.ofSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());
    }

    @Test
    public void testLast_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.last(array, true);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.get());

        result = Index.last(array, false);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.last(null, true);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_BooleanArray_WithStartIndex() {
        boolean[] array = { true, false, true, false, true };

        OptionalInt result = Index.last(array, true, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.last(array, false, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(1, result.get());

        result = Index.last(array, true, -1);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_CharArray() {
        char[] array = { 'a', 'b', 'c', 'b', 'd' };

        OptionalInt result = Index.last(array, 'b');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_ByteArray() {
        byte[] array = { 1, 2, 3, 2, 4 };

        OptionalInt result = Index.last(array, (byte) 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_ShortArray() {
        short[] array = { 10, 20, 30, 20, 40 };

        OptionalInt result = Index.last(array, (short) 20);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_IntArray() {
        int[] array = { 100, 200, 300, 200, 400 };

        OptionalInt result = Index.last(array, 200);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_LongArray() {
        long[] array = { 1000L, 2000L, 3000L, 2000L, 4000L };

        OptionalInt result = Index.last(array, 2000L);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_FloatArray() {
        float[] array = { 1.1f, 2.2f, 3.3f, 2.2f, 4.4f };

        OptionalInt result = Index.last(array, 2.2f);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray() {
        double[] array = { 1.11, 2.22, 3.33, 2.22, 4.44 };

        OptionalInt result = Index.last(array, 2.22);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.last(array, 3.0, 0.01);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_DoubleArray_WithToleranceAndStartIndex() {
        double[] array = { 1.0, 2.001, 3.0, 2.999, 4.0 };

        OptionalInt result = Index.last(array, 3.0, 0.01, 3);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        result = Index.last(array, 3.0, 0.01, 2);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLast_ObjectArray() {
        String[] array = { "apple", "banana", "cherry", "banana", "date" };

        OptionalInt result = Index.last(array, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "banana", "date");

        OptionalInt result = Index.last(list, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.last(linkedList, "banana");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLast_String_Char() {
        String str = "hello world";

        OptionalInt result = Index.last(str, 'o');
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        result = Index.last(str, 'z');
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLast_String_String() {
        String str = "hello world hello";

        OptionalInt result = Index.last(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());

        result = Index.last(str, "world");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(6, result.get());
    }

    @Test
    public void testLastOfIgnoreCase_String() {
        String str = "Hello World HELLO";

        OptionalInt result = Index.lastOfIgnoreCase(str, "hello");
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(12, result.get());
    }

    @Test
    public void testLastOfSubArray_BooleanArray() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.lastOfSubArray(array, new boolean[0]);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testLastOfSubArray_BooleanArray_WithStartIndex() {
        boolean[] array = { true, false, true, false, true };
        boolean[] subArray = { true, false };

        OptionalInt result = Index.lastOfSubArray(array, 1, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.lastOfSubArray(array, -1, subArray);
        Assertions.assertFalse(result.isPresent());
    }

    @Test
    public void testLastOfSubArray_CharArray() {
        char[] array = { 'a', 'b', 'c', 'a', 'b', 'c' };
        char[] subArray = { 'a', 'b' };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testLastOfSubArray_ObjectArray() {
        String[] array = { "apple", "banana", "apple", "banana", "cherry" };
        String[] subArray = { "apple", "banana" };

        OptionalInt result = Index.lastOfSubArray(array, subArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLastOfSubList() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "cherry");
        List<String> subList = Arrays.asList("apple", "banana");

        OptionalInt result = Index.lastOfSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());

        result = Index.lastOfSubList(list, new ArrayList<>());
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());
    }

    @Test
    public void testLastOfSubList_NonRandomAccess() {
        LinkedList<String> list = new LinkedList<>(Arrays.asList("apple", "banana", "apple", "banana"));
        LinkedList<String> subList = new LinkedList<>(Arrays.asList("apple", "banana"));

        OptionalInt result = Index.lastOfSubList(list, subList);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testAllOf_BooleanArray() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, false);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(null, true);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_BooleanArray_WithFromIndex() {
        boolean[] array = { true, false, true, false, true };

        BitSet result = Index.allOf(array, true, 2);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, true, 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_ByteArray() {
        byte[] array = { 1, 2, 1, 2, 1 };

        BitSet result = Index.allOf(array, (byte) 1);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_CharArray() {
        char[] array = { 'a', 'b', 'a', 'b', 'a' };

        BitSet result = Index.allOf(array, 'a');
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_ShortArray() {
        short[] array = { 10, 20, 10, 20, 10 };

        BitSet result = Index.allOf(array, (short) 10);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_IntArray() {
        int[] array = { 100, 200, 100, 200, 100 };

        BitSet result = Index.allOf(array, 100);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_LongArray() {
        long[] array = { 1000L, 2000L, 1000L, 2000L, 1000L };

        BitSet result = Index.allOf(array, 1000L);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_FloatArray() {
        float[] array = { 1.1f, 2.2f, 1.1f, 2.2f, 1.1f };

        BitSet result = Index.allOf(array, 1.1f);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray() {
        double[] array = { 1.11, 2.22, 1.11, 2.22, 1.11 };

        BitSet result = Index.allOf(array, 1.11);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_DoubleArray_WithTolerance() {
        double[] array = { 1.0, 2.001, 1.01, 2.999, 0.99 };

        BitSet result = Index.allOf(array, 1.0, 0.02);
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_ObjectArray() {
        String[] array = { "apple", "banana", "apple", "banana", "apple" };

        BitSet result = Index.allOf(array, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        result = Index.allOf(array, (String) null);
        Assertions.assertEquals(0, result.cardinality());

        String[] arrayWithNull = { "apple", null, "apple", null };
        result = Index.allOf(arrayWithNull, (String) null);
        Assertions.assertEquals(2, result.cardinality());
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection() {
        List<String> list = Arrays.asList("apple", "banana", "apple", "banana", "apple");

        BitSet result = Index.allOf(list, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, "apple");
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(2));
        Assertions.assertTrue(result.get(4));
    }

    @Test
    public void testAllOf_Array_WithPredicate() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        result = Index.allOf(array, s -> s.length() > 10);
        Assertions.assertEquals(0, result.cardinality());
    }

    @Test
    public void testAllOf_Array_WithPredicate_FromIndex() {
        String[] array = { "apple", "apricot", "banana", "avocado", "cherry" };

        BitSet result = Index.allOf(array, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection_WithPredicate() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));

        LinkedList<String> linkedList = new LinkedList<>(list);
        result = Index.allOf(linkedList, s -> s.startsWith("a"));
        Assertions.assertEquals(3, result.cardinality());
        Assertions.assertTrue(result.get(0));
        Assertions.assertTrue(result.get(1));
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testAllOf_Collection_WithPredicate_FromIndex() {
        List<String> list = Arrays.asList("apple", "apricot", "banana", "avocado", "cherry");

        BitSet result = Index.allOf(list, s -> s.startsWith("a"), 2);
        Assertions.assertEquals(1, result.cardinality());
        Assertions.assertTrue(result.get(3));
    }

    @Test
    public void testEdgeCases_EmptyArrays() {
        OptionalInt result = Index.of(new int[0], 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.last(new int[0], 1);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf(new int[0], 1);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_NullInputs() {
        OptionalInt result = Index.of((int[]) null, 1);
        Assertions.assertFalse(result.isPresent());

        result = Index.last((int[]) null, 1);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf((int[]) null, 1);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_LargeFromIndex() {
        int[] array = { 1, 2, 3, 4, 5 };

        OptionalInt result = Index.of(array, 3, 100);
        Assertions.assertFalse(result.isPresent());

        BitSet bitSet = Index.allOf(array, 3, 100);
        Assertions.assertEquals(0, bitSet.cardinality());
    }

    @Test
    public void testEdgeCases_NegativeFromIndex() {
        int[] array = { 1, 2, 3, 4, 5 };

        OptionalInt result = Index.of(array, 1, -10);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        BitSet bitSet = Index.allOf(array, 1, -10);
        Assertions.assertEquals(1, bitSet.cardinality());
        Assertions.assertTrue(bitSet.get(0));
    }

    @Test
    public void testOfSubArray_EmptySubArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] emptySubArray = {};

        OptionalInt result = Index.ofSubArray(array, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(0, result.get());

        result = Index.ofSubArray(array, 2, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.get());
    }

    @Test
    public void testLastOfSubArray_EmptySubArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] emptySubArray = {};

        OptionalInt result = Index.lastOfSubArray(array, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(5, result.get());

        result = Index.lastOfSubArray(array, 3, emptySubArray);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(3, result.get());
    }

    @Test
    public void testOfSubArray_IndexOutOfBounds() {
        int[] array = { 1, 2, 3, 4, 5 };
        int[] subArray = { 2, 3, 4 };

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, -1, 2);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, 0, 10);
        });

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> {
            Index.ofSubArray(array, 0, subArray, 2, 2);
        });
    }
}
