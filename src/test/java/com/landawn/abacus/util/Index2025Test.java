package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("2025")
public class Index2025Test extends TestBase {

    @Test
    public void test_of_boolean_array() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.of(a, true);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.of(a, false);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        boolean[] b = { true, true };
        result = Index.of(b, false);
        assertFalse(result.isPresent());

        result = Index.of((boolean[]) null, true);
        assertFalse(result.isPresent());

        result = Index.of(new boolean[0], true);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_boolean_array_fromIndex() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.of(a, true, 1);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.of(a, true, 3);
        assertFalse(result.isPresent());

        result = Index.of(a, true, 10);
        assertFalse(result.isPresent());

        result = Index.of(a, true, -1);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.of((boolean[]) null, true, 0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_char_array() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.of(a, 'b');
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 'd');
        assertFalse(result.isPresent());

        result = Index.of((char[]) null, 'a');
        assertFalse(result.isPresent());

        result = Index.of(new char[0], 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_char_array_fromIndex() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.of(a, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 'b', -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 'b', 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_byte_array() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (byte) 5);
        assertFalse(result.isPresent());

        result = Index.of((byte[]) null, (byte) 1);
        assertFalse(result.isPresent());

        result = Index.of(new byte[0], (byte) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_byte_array_fromIndex() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (byte) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, (byte) 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (byte) 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_short_array() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (short) 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (short) 5);
        assertFalse(result.isPresent());

        result = Index.of((short[]) null, (short) 1);
        assertFalse(result.isPresent());

        result = Index.of(new short[0], (short) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_short_array_fromIndex() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, (short) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, (short) 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, (short) 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_int_array() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5);
        assertFalse(result.isPresent());

        result = Index.of((int[]) null, 1);
        assertFalse(result.isPresent());

        result = Index.of(new int[0], 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_int_array_fromIndex() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.of(a, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_long_array() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.of(a, 2L);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5L);
        assertFalse(result.isPresent());

        result = Index.of((long[]) null, 1L);
        assertFalse(result.isPresent());

        result = Index.of(new long[0], 1L);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_long_array_fromIndex() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.of(a, 2L, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2L, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2L, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_float_array() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.of(a, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5.0f);
        assertFalse(result.isPresent());

        result = Index.of((float[]) null, 1.0f);
        assertFalse(result.isPresent());

        result = Index.of(new float[0], 1.0f);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_float_array_fromIndex() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.of(a, 2.0f, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0f, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0f, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.of(a, 2.0);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 5.0);
        assertFalse(result.isPresent());

        result = Index.of((double[]) null, 1.0);
        assertFalse(result.isPresent());

        result = Index.of(new double[0], 1.0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_fromIndex() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.of(a, 2.0, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.of(a, 2.0, 0.2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 0.05);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((double[]) null, 2.0, 0.1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_double_array_tolerance_fromIndex() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.of(a, 2.0, 0.2, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, 2.0, 0.2, -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, 2.0, 0.05, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Object_array() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.of(a, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, "d");
        assertFalse(result.isPresent());

        String[] b = { "a", null, "c" };
        result = Index.of(b, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((Object[]) null, "a");
        assertFalse(result.isPresent());

        result = Index.of(new Object[0], "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Object_array_fromIndex() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.of(a, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(a, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(a, "b", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Collection() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.of(c, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(c, "d");
        assertFalse(result.isPresent());

        List<String> b = Arrays.asList("a", null, "c");
        result = Index.of(b, null);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of((Collection<?>) null, "a");
        assertFalse(result.isPresent());

        result = Index.of(new ArrayList<>(), "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Collection_fromIndex() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.of(c, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(c, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(c, "b", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Iterator<String> iter = list.iterator();
        OptionalInt result = Index.of(iter, "b");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        iter = list.iterator();
        result = Index.of(iter, "d");
        assertFalse(result.isPresent());

        result = Index.of((Iterator<?>) null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_Iterator_fromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Iterator<String> iter = list.iterator();
        OptionalInt result = Index.of(iter, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        iter = list.iterator();
        result = Index.of(iter, "b", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());
    }

    @Test
    public void test_of_String_char() {
        String str = "abcb";
        OptionalInt result = Index.of(str, 'b');
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, 'd');
        assertFalse(result.isPresent());

        result = Index.of((String) null, 'a');
        assertFalse(result.isPresent());

        result = Index.of("", 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_char_fromIndex() {
        String str = "abcb";
        OptionalInt result = Index.of(str, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.of(str, 'b', -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, 'b', 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_String() {
        String str = "abcabc";
        OptionalInt result = Index.of(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, "de");
        assertFalse(result.isPresent());

        result = Index.of((String) null, "a");
        assertFalse(result.isPresent());

        result = Index.of("", "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_of_String_String_fromIndex() {
        String str = "abcabc";
        OptionalInt result = Index.of(str, "bc", 2);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.of(str, "bc", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.of(str, "bc", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofIgnoreCase_String() {
        String str = "aBcAbC";
        OptionalInt result = Index.ofIgnoreCase(str, "BC");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "DE");
        assertFalse(result.isPresent());

        result = Index.ofIgnoreCase(null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofIgnoreCase_String_fromIndex() {
        String str = "aBcAbC";
        OptionalInt result = Index.ofIgnoreCase(str, "BC", 2);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc", -1);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.ofIgnoreCase(str, "bc", 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { true, true };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        boolean[] notFound = { false, false };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new boolean[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((boolean[]) null, sub);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, (boolean[]) null);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean_fromIndex() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 10, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_boolean_fromIndex_startIndex_size() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { false, true, true, false };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((boolean[]) null, 0, sub, 0, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_char() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        char[] notFound = { 'x', 'y' };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new char[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((char[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_char_fromIndex() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_char_fromIndex_startIndex_size() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'x', 'c', 'd', 'y' };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_byte() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        byte[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new byte[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((byte[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_byte_fromIndex() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_byte_fromIndex_startIndex_size() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_short() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        short[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new short[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((short[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_short_fromIndex() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_short_fromIndex_startIndex_size() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_int() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        int[] notFound = { 9, 8 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new int[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((int[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_int_fromIndex() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_int_fromIndex_startIndex_size() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_long() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        long[] notFound = { 9L, 8L };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new long[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((long[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_long_fromIndex() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_long_fromIndex_startIndex_size() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 0L, 3L, 4L, 0L };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_float() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        float[] notFound = { 9.0f, 8.0f };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new float[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((float[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_float_fromIndex() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_float_fromIndex_startIndex_size() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 0.0f, 3.0f, 4.0f, 0.0f };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_double() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        double[] notFound = { 9.0, 8.0 };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new double[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((double[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_double_fromIndex() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_double_fromIndex_startIndex_size() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 0.0, 3.0, 4.0, 0.0 };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_Object() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.ofSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        String[] notFound = { "x", "y" };
        result = Index.ofSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubArray(source, new String[0]);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubArray((Object[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubArray_Object_fromIndex() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.ofSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubArray(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubArray_Object_fromIndex_startIndex_size() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "x", "c", "d", "y" };
        OptionalInt result = Index.ofSubArray(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubArray(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_ofSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.ofSubList(source, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        List<String> notFound = Arrays.asList("x", "y");
        result = Index.ofSubList(source, notFound);
        assertFalse(result.isPresent());

        result = Index.ofSubList(source, new ArrayList<>());
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.ofSubList(null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_ofSubList_fromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.ofSubList(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.ofSubList(source, -1, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());
    }

    @Test
    public void test_ofSubList_fromIndex_startIndex_size() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("x", "c", "d", "y");
        OptionalInt result = Index.ofSubList(source, 0, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.ofSubList(source, 0, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());
    }

    @Test
    public void test_last_boolean_array() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.last(a, true);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.last(a, false);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        boolean[] b = { true, true };
        result = Index.last(b, false);
        assertFalse(result.isPresent());

        result = Index.last((boolean[]) null, true);
        assertFalse(result.isPresent());

        result = Index.last(new boolean[0], true);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_boolean_array_startIndexFromBack() {
        boolean[] a = { true, false, true, false };
        OptionalInt result = Index.last(a, true, 1);
        assertTrue(result.isPresent());
        assertEquals(0, result.getAsInt());

        result = Index.last(a, true, 10);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.last(a, true, -1);
        assertFalse(result.isPresent());

        result = Index.last((boolean[]) null, true, 2);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_char_array() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.last(a, 'b');
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 'd');
        assertFalse(result.isPresent());

        result = Index.last((char[]) null, 'a');
        assertFalse(result.isPresent());

        result = Index.last(new char[0], 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_char_array_startIndexFromBack() {
        char[] a = { 'a', 'b', 'c', 'b' };
        OptionalInt result = Index.last(a, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 'b', 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_byte_array() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (byte) 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, (byte) 5);
        assertFalse(result.isPresent());

        result = Index.last((byte[]) null, (byte) 1);
        assertFalse(result.isPresent());

        result = Index.last(new byte[0], (byte) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_byte_array_startIndexFromBack() {
        byte[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (byte) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, (byte) 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_short_array() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (short) 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, (short) 5);
        assertFalse(result.isPresent());

        result = Index.last((short[]) null, (short) 1);
        assertFalse(result.isPresent());

        result = Index.last(new short[0], (short) 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_short_array_startIndexFromBack() {
        short[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, (short) 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, (short) 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_int_array() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, 2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5);
        assertFalse(result.isPresent());

        result = Index.last((int[]) null, 1);
        assertFalse(result.isPresent());

        result = Index.last(new int[0], 1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_int_array_startIndexFromBack() {
        int[] a = { 1, 2, 3, 2 };
        OptionalInt result = Index.last(a, 2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_long_array() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.last(a, 2L);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5L);
        assertFalse(result.isPresent());

        result = Index.last((long[]) null, 1L);
        assertFalse(result.isPresent());

        result = Index.last(new long[0], 1L);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_long_array_startIndexFromBack() {
        long[] a = { 1L, 2L, 3L, 2L };
        OptionalInt result = Index.last(a, 2L, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2L, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_float_array() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.last(a, 2.0f);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5.0f);
        assertFalse(result.isPresent());

        result = Index.last((float[]) null, 1.0f);
        assertFalse(result.isPresent());

        result = Index.last(new float[0], 1.0f);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_float_array_startIndexFromBack() {
        float[] a = { 1.0f, 2.0f, 3.0f, 2.0f };
        OptionalInt result = Index.last(a, 2.0f, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0f, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_double_array() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.last(a, 2.0);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 5.0);
        assertFalse(result.isPresent());

        result = Index.last((double[]) null, 1.0);
        assertFalse(result.isPresent());

        result = Index.last(new double[0], 1.0);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_double_array_startIndexFromBack() {
        double[] a = { 1.0, 2.0, 3.0, 2.0 };
        OptionalInt result = Index.last(a, 2.0, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.last(a, 2.0, 0.2);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, 2.0, 0.05);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last((double[]) null, 2.0, 0.1);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_double_array_tolerance_startIndexFromBack() {
        double[] a = { 1.0, 2.0, 3.0, 2.1 };
        OptionalInt result = Index.last(a, 2.0, 0.2, 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, 2.0, 0.2, 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_Object_array() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.last(a, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(a, "d");
        assertFalse(result.isPresent());

        String[] b = { "a", null, "c", null };
        result = Index.last(b, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last((Object[]) null, "a");
        assertFalse(result.isPresent());

        result = Index.last(new Object[0], "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_Object_array_startIndexFromBack() {
        String[] a = { "a", "b", "c", "b" };
        OptionalInt result = Index.last(a, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(a, "b", 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_Collection() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.last(c, "b");
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(c, "d");
        assertFalse(result.isPresent());

        List<String> b = Arrays.asList("a", null, "c", null);
        result = Index.last(b, null);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last((Collection<?>) null, "a");
        assertFalse(result.isPresent());

        result = Index.last(new ArrayList<>(), "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_Collection_startIndexFromBack() {
        List<String> c = Arrays.asList("a", "b", "c", "b");
        OptionalInt result = Index.last(c, "b", 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(c, "b", 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_String_char() {
        String str = "abcb";
        OptionalInt result = Index.last(str, 'b');
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());

        result = Index.last(str, 'd');
        assertFalse(result.isPresent());

        result = Index.last((String) null, 'a');
        assertFalse(result.isPresent());

        result = Index.last("", 'a');
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_String_char_startIndexFromBack() {
        String str = "abcb";
        OptionalInt result = Index.last(str, 'b', 2);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(str, 'b', 10);
        assertTrue(result.isPresent());
        assertEquals(3, result.getAsInt());
    }

    @Test
    public void test_last_String_String() {
        String str = "abcabc";
        OptionalInt result = Index.last(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.last(str, "de");
        assertFalse(result.isPresent());

        result = Index.last((String) null, "a");
        assertFalse(result.isPresent());

        result = Index.last("", "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_last_String_String_startIndexFromBack() {
        String str = "abcabc";
        OptionalInt result = Index.last(str, "bc", 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.last(str, "bc", 10);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void test_lastOfIgnoreCase_String() {
        String str = "aBcAbC";
        OptionalInt result = Index.lastOfIgnoreCase(str, "BC");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "bc");
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "DE");
        assertFalse(result.isPresent());

        result = Index.lastOfIgnoreCase(null, "a");
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfIgnoreCase_String_startIndexFromBack() {
        String str = "aBcAbC";
        OptionalInt result = Index.lastOfIgnoreCase(str, "BC", 3);
        assertTrue(result.isPresent());
        assertEquals(1, result.getAsInt());

        result = Index.lastOfIgnoreCase(str, "bc", 10);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_boolean() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());

        boolean[] notFound = { false, false };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new boolean[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());

        result = Index.lastOfSubArray((boolean[]) null, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_boolean_startIndexFromBack() {
        boolean[] source = { true, false, true, true, false, true, true };
        boolean[] sub = { true, true };
        OptionalInt result = Index.lastOfSubArray(source, 4, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_boolean_startIndexFromBack_startIndex_size() {
        boolean[] source = { true, false, true, true, false };
        boolean[] sub = { false, true, true, false };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_char() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        char[] notFound = { 'x', 'y' };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new char[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_char_startIndexFromBack() {
        char[] source = { 'a', 'b', 'c', 'd', 'c', 'd' };
        char[] sub = { 'c', 'd' };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_char_startIndexFromBack_startIndex_size() {
        char[] source = { 'a', 'b', 'c', 'd', 'e' };
        char[] sub = { 'x', 'c', 'd', 'y' };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_byte() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        byte[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new byte[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_byte_startIndexFromBack() {
        byte[] source = { 1, 2, 3, 4, 3, 4 };
        byte[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_byte_startIndexFromBack_startIndex_size() {
        byte[] source = { 1, 2, 3, 4, 5 };
        byte[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_short() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        short[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new short[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_short_startIndexFromBack() {
        short[] source = { 1, 2, 3, 4, 3, 4 };
        short[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_short_startIndexFromBack_startIndex_size() {
        short[] source = { 1, 2, 3, 4, 5 };
        short[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_int() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        int[] notFound = { 9, 8 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new int[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_int_startIndexFromBack() {
        int[] source = { 1, 2, 3, 4, 3, 4 };
        int[] sub = { 3, 4 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_int_startIndexFromBack_startIndex_size() {
        int[] source = { 1, 2, 3, 4, 5 };
        int[] sub = { 0, 3, 4, 0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_long() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        long[] notFound = { 9L, 8L };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new long[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_long_startIndexFromBack() {
        long[] source = { 1L, 2L, 3L, 4L, 3L, 4L };
        long[] sub = { 3L, 4L };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_long_startIndexFromBack_startIndex_size() {
        long[] source = { 1L, 2L, 3L, 4L, 5L };
        long[] sub = { 0L, 3L, 4L, 0L };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_float() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        float[] notFound = { 9.0f, 8.0f };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new float[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_float_startIndexFromBack() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 3.0f, 4.0f };
        float[] sub = { 3.0f, 4.0f };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_float_startIndexFromBack_startIndex_size() {
        float[] source = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
        float[] sub = { 0.0f, 3.0f, 4.0f, 0.0f };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_double() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        double[] notFound = { 9.0, 8.0 };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new double[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_double_startIndexFromBack() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 3.0, 4.0 };
        double[] sub = { 3.0, 4.0 };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_double_startIndexFromBack_startIndex_size() {
        double[] source = { 1.0, 2.0, 3.0, 4.0, 5.0 };
        double[] sub = { 0.0, 3.0, 4.0, 0.0 };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_Object() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.lastOfSubArray(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        String[] notFound = { "x", "y" };
        result = Index.lastOfSubArray(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubArray(source, new String[0]);
        assertTrue(result.isPresent());
        assertEquals(source.length, result.getAsInt());
    }

    @Test
    public void test_lastOfSubArray_Object_startIndexFromBack() {
        String[] source = { "a", "b", "c", "d", "c", "d" };
        String[] sub = { "c", "d" };
        OptionalInt result = Index.lastOfSubArray(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubArray_Object_startIndexFromBack_startIndex_size() {
        String[] source = { "a", "b", "c", "d", "e" };
        String[] sub = { "x", "c", "d", "y" };
        OptionalInt result = Index.lastOfSubArray(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubArray(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_lastOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.lastOfSubList(source, sub);
        assertTrue(result.isPresent());
        assertEquals(4, result.getAsInt());

        List<String> notFound = Arrays.asList("x", "y");
        result = Index.lastOfSubList(source, notFound);
        assertFalse(result.isPresent());

        result = Index.lastOfSubList(source, new ArrayList<>());
        assertTrue(result.isPresent());
        assertEquals(source.size(), result.getAsInt());
    }

    @Test
    public void test_lastOfSubList_startIndexFromBack() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        OptionalInt result = Index.lastOfSubList(source, 3, sub);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubList(source, -1, sub);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_lastOfSubList_startIndexFromBack_startIndex_size() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("x", "c", "d", "y");
        OptionalInt result = Index.lastOfSubList(source, 5, sub, 1, 2);
        assertTrue(result.isPresent());
        assertEquals(2, result.getAsInt());

        result = Index.lastOfSubList(source, 5, sub, 0, 0);
        assertTrue(result.isPresent());
        assertEquals(5, result.getAsInt());
    }

    @Test
    public void test_allOf_boolean_array() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, false);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        boolean[] b = { true, true };
        result = Index.allOf(b, false);
        assertEquals(0, result.cardinality());

        result = Index.allOf((boolean[]) null, true);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_boolean_array_fromIndex() {
        boolean[] a = { true, false, true, false, true };
        BitSet result = Index.allOf(a, true, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, true, -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, true, 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((byte[]) null, (byte) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_byte_array_fromIndex() {
        byte[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (byte) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (byte) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_char_array() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a');
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'd');
        assertEquals(0, result.cardinality());

        result = Index.allOf((char[]) null, 'a');
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_char_array_fromIndex() {
        char[] a = { 'a', 'b', 'a', 'c', 'a' };
        BitSet result = Index.allOf(a, 'a', 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 'a', -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_short_array() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((short[]) null, (short) 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_short_array_fromIndex() {
        short[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, (short) 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, (short) 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_int_array() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5);
        assertEquals(0, result.cardinality());

        result = Index.allOf((int[]) null, 1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_int_array_fromIndex() {
        int[] a = { 1, 2, 1, 3, 1 };
        BitSet result = Index.allOf(a, 1, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_long_array() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5L);
        assertEquals(0, result.cardinality());

        result = Index.allOf((long[]) null, 1L);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_long_array_fromIndex() {
        long[] a = { 1L, 2L, 1L, 3L, 1L };
        BitSet result = Index.allOf(a, 1L, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1L, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_float_array() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0f);
        assertEquals(0, result.cardinality());

        result = Index.allOf((float[]) null, 1.0f);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_float_array_fromIndex() {
        float[] a = { 1.0f, 2.0f, 1.0f, 3.0f, 1.0f };
        BitSet result = Index.allOf(a, 1.0f, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0f, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 5.0);
        assertEquals(0, result.cardinality());

        result = Index.allOf((double[]) null, 1.0);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_fromIndex() {
        double[] a = { 1.0, 2.0, 1.0, 3.0, 1.0 };
        BitSet result = Index.allOf(a, 1.0, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 1.0, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 0.2);
        assertEquals(3, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, 0.01);
        assertEquals(1, result.cardinality());
        assertTrue(result.get(1));

        result = Index.allOf((double[]) null, 2.0, 0.1);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_double_array_tolerance_fromIndex() {
        double[] a = { 1.0, 2.0, 2.1, 3.0, 2.05 };
        BitSet result = Index.allOf(a, 2.0, 0.2, 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, 2.0, 0.2, -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "d");
        assertEquals(0, result.cardinality());

        String[] b = { "a", null, "c", null };
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Object[]) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_fromIndex() {
        String[] a = { "a", "b", "a", "c", "a" };
        BitSet result = Index.allOf(a, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Collection() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a");
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "d");
        assertEquals(0, result.cardinality());

        List<String> b = Arrays.asList("a", null, "c", null);
        result = Index.allOf(b, (String) null);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(1));
        assertTrue(result.get(3));

        result = Index.allOf((Collection<?>) null, "a");
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_fromIndex() {
        List<String> c = Arrays.asList("a", "b", "a", "c", "a");
        BitSet result = Index.allOf(c, "a", 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, "a", -1);
        assertEquals(3, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((String[]) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Object_array_Predicate_fromIndex() {
        String[] a = { "apple", "banana", "avocado", "cherry", "apricot" };
        BitSet result = Index.allOf(a, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(a, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(a, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"));
        assertEquals(3, result.cardinality());
        assertTrue(result.get(0));
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.length() > 6);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf((Collection<String>) null, s -> s.startsWith("a"));
        assertEquals(0, result.cardinality());
    }

    @Test
    public void test_allOf_Collection_Predicate_fromIndex() {
        List<String> c = Arrays.asList("apple", "banana", "avocado", "cherry", "apricot");
        BitSet result = Index.allOf(c, s -> s.startsWith("a"), 2);
        assertEquals(2, result.cardinality());
        assertTrue(result.get(2));
        assertTrue(result.get(4));

        result = Index.allOf(c, s -> s.startsWith("a"), -1);
        assertEquals(3, result.cardinality());

        result = Index.allOf(c, s -> s.startsWith("a"), 10);
        assertEquals(0, result.cardinality());
    }
}
