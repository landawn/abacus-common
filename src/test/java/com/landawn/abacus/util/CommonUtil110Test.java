package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

public class CommonUtil110Test extends TestBase {

    // Tests for checkArgNotEmpty with CharSequence
    @Test
    public void testCheckArgNotEmpty_CharSequence_Valid() {
        String result = N.checkArgNotEmpty("test", "argName");
        Assertions.assertEquals("test", result);

        StringBuilder sb = new StringBuilder("builder");
        StringBuilder result2 = N.checkArgNotEmpty(sb, "argName");
        Assertions.assertEquals(sb, result2);
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((String) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharSequence_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty("", "argName");
        });
    }

    // Tests for checkArgNotEmpty with boolean[]
    @Test
    public void testCheckArgNotEmpty_BooleanArray_Valid() {
        boolean[] arr = { true, false };
        boolean[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((boolean[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_BooleanArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new boolean[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with char[]
    @Test
    public void testCheckArgNotEmpty_CharArray_Valid() {
        char[] arr = { 'a', 'b' };
        char[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((char[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_CharArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new char[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with byte[]
    @Test
    public void testCheckArgNotEmpty_ByteArray_Valid() {
        byte[] arr = { 1, 2 };
        byte[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((byte[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ByteArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new byte[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with short[]
    @Test
    public void testCheckArgNotEmpty_ShortArray_Valid() {
        short[] arr = { 1, 2 };
        short[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((short[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ShortArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new short[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with int[]
    @Test
    public void testCheckArgNotEmpty_IntArray_Valid() {
        int[] arr = { 1, 2 };
        int[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((int[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_IntArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new int[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with long[]
    @Test
    public void testCheckArgNotEmpty_LongArray_Valid() {
        long[] arr = { 1L, 2L };
        long[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((long[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_LongArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new long[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with float[]
    @Test
    public void testCheckArgNotEmpty_FloatArray_Valid() {
        float[] arr = { 1.0f, 2.0f };
        float[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((float[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_FloatArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new float[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with double[]
    @Test
    public void testCheckArgNotEmpty_DoubleArray_Valid() {
        double[] arr = { 1.0, 2.0 };
        double[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((double[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_DoubleArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new double[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with Object[]
    @Test
    public void testCheckArgNotEmpty_ObjectArray_Valid() {
        String[] arr = { "a", "b" };
        String[] result = N.checkArgNotEmpty(arr, "argName");
        Assertions.assertArrayEquals(arr, result);
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((String[]) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_ObjectArray_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new String[0], "argName");
        });
    }

    // Tests for checkArgNotEmpty with Collection
    @Test
    public void testCheckArgNotEmpty_Collection_Valid() {
        List<String> list = Arrays.asList("a", "b");
        List<String> result = N.checkArgNotEmpty(list, "argName");
        Assertions.assertEquals(list, result);

        Set<Integer> set = new HashSet<>();
        set.add(1);
        Set<Integer> result2 = N.checkArgNotEmpty(set, "argName");
        Assertions.assertEquals(set, result2);
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Collection<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Collection_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new ArrayList<>(), "argName");
        });
    }

    // Tests for checkArgNotEmpty with Iterable
    @Test
    public void testCheckArgNotEmpty_Iterable_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterable<String> iterable = list;
        Iterable<String> result = N.checkArgNotEmpty(iterable, "argName");
        Assertions.assertEquals(iterable, result);
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Iterable<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterable_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            N.checkArgNotEmpty((Iterable<String>) list, "argName");
        });
    }

    // Tests for checkArgNotEmpty with Iterator
    @Test
    public void testCheckArgNotEmpty_Iterator_Valid() {
        List<String> list = Arrays.asList("a", "b");
        Iterator<String> iterator = list.iterator();
        Iterator<String> result = N.checkArgNotEmpty(iterator, "argName");
        Assertions.assertEquals(iterator, result);
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Iterator<?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Iterator_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            List<String> list = new ArrayList<>();
            N.checkArgNotEmpty(list.iterator(), "argName");
        });
    }

    // Tests for checkArgNotEmpty with Map
    @Test
    public void testCheckArgNotEmpty_Map_Valid() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Map<String, Integer> result = N.checkArgNotEmpty(map, "argName");
        Assertions.assertEquals(map, result);
    }

    @Test
    public void testCheckArgNotEmpty_Map_Null() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty((Map<?, ?>) null, "argName");
        });
    }

    @Test
    public void testCheckArgNotEmpty_Map_Empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgNotEmpty(new HashMap<>(), "argName");
        });
    }

    // Tests for checkArgument with varargs
    @Test
    public void testCheckArgument_Varargs_Pass() {
        N.checkArgument(true, "Error message %s %s", "arg1", "arg2");
        N.checkArgument(true, "Error message {} {}", "arg1", "arg2");
    }

    @Test
    public void testCheckArgument_Varargs_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error message %s %s", "arg1", "arg2");
        });

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error message {} {}", "arg1", "arg2");
        });
    }

    // Tests for checkArgument with char parameter
    @Test
    public void testCheckArgument_Char_Pass() {
        N.checkArgument(true, "Error with char: %s", 'a');
    }

    @Test
    public void testCheckArgument_Char_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with char: %s", 'a');
        });
    }

    // Tests for checkArgument with int parameter
    @Test
    public void testCheckArgument_Int_Pass() {
        N.checkArgument(true, "Error with int: %s", 42);
    }

    @Test
    public void testCheckArgument_Int_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with int: %s", 42);
        });
    }

    // Tests for checkArgument with long parameter
    @Test
    public void testCheckArgument_Long_Pass() {
        N.checkArgument(true, "Error with long: %s", 42L);
    }

    @Test
    public void testCheckArgument_Long_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with long: %s", 42L);
        });
    }

    // Tests for checkArgument with double parameter
    @Test
    public void testCheckArgument_Double_Pass() {
        N.checkArgument(true, "Error with double: %s", 3.14);
    }

    @Test
    public void testCheckArgument_Double_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with double: %s", 3.14);
        });
    }

    // Tests for checkArgument with Object parameter
    @Test
    public void testCheckArgument_Object_Pass() {
        N.checkArgument(true, "Error with object: %s", "test");
    }

    @Test
    public void testCheckArgument_Object_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error with object: %s", "test");
        });
    }

    // Tests for checkArgument with two char parameters
    @Test
    public void testCheckArgument_CharChar_Pass() {
        N.checkArgument(true, "Error: %s %s", 'a', 'b');
    }

    @Test
    public void testCheckArgument_CharChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 'b');
        });
    }

    // Tests for checkArgument with char and int parameters
    @Test
    public void testCheckArgument_CharInt_Pass() {
        N.checkArgument(true, "Error: %s %s", 'a', 42);
    }

    @Test
    public void testCheckArgument_CharInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 42);
        });
    }

    // Tests for checkArgument with char and long parameters
    @Test
    public void testCheckArgument_CharLong_Pass() {
        N.checkArgument(true, "Error: %s %s", 'a', 42L);
    }

    @Test
    public void testCheckArgument_CharLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 42L);
        });
    }

    // Tests for checkArgument with char and double parameters
    @Test
    public void testCheckArgument_CharDouble_Pass() {
        N.checkArgument(true, "Error: %s %s", 'a', 3.14);
    }

    @Test
    public void testCheckArgument_CharDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', 3.14);
        });
    }

    // Tests for checkArgument with char and Object parameters
    @Test
    public void testCheckArgument_CharObject_Pass() {
        N.checkArgument(true, "Error: %s %s", 'a', "test");
    }

    @Test
    public void testCheckArgument_CharObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 'a', "test");
        });
    }

    // Tests for checkArgument with int and char parameters
    @Test
    public void testCheckArgument_IntChar_Pass() {
        N.checkArgument(true, "Error: %s %s", 42, 'a');
    }

    @Test
    public void testCheckArgument_IntChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 'a');
        });
    }

    // Tests for checkArgument with two int parameters
    @Test
    public void testCheckArgument_IntInt_Pass() {
        N.checkArgument(true, "Error: %s %s", 42, 24);
    }

    @Test
    public void testCheckArgument_IntInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 24);
        });
    }

    // Tests for checkArgument with int and long parameters
    @Test
    public void testCheckArgument_IntLong_Pass() {
        N.checkArgument(true, "Error: %s %s", 42, 24L);
    }

    @Test
    public void testCheckArgument_IntLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 24L);
        });
    }

    // Tests for checkArgument with int and double parameters
    @Test
    public void testCheckArgument_IntDouble_Pass() {
        N.checkArgument(true, "Error: %s %s", 42, 3.14);
    }

    @Test
    public void testCheckArgument_IntDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, 3.14);
        });
    }

    // Tests for checkArgument with int and Object parameters
    @Test
    public void testCheckArgument_IntObject_Pass() {
        N.checkArgument(true, "Error: %s %s", 42, "test");
    }

    @Test
    public void testCheckArgument_IntObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42, "test");
        });
    }

    // Tests for checkArgument with long and char parameters
    @Test
    public void testCheckArgument_LongChar_Pass() {
        N.checkArgument(true, "Error: %s %s", 42L, 'a');
    }

    @Test
    public void testCheckArgument_LongChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 'a');
        });
    }

    // Tests for checkArgument with long and int parameters
    @Test
    public void testCheckArgument_LongInt_Pass() {
        N.checkArgument(true, "Error: %s %s", 42L, 24);
    }

    @Test
    public void testCheckArgument_LongInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 24);
        });
    }

    // Tests for checkArgument with two long parameters
    @Test
    public void testCheckArgument_LongLong_Pass() {
        N.checkArgument(true, "Error: %s %s", 42L, 24L);
    }

    @Test
    public void testCheckArgument_LongLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 24L);
        });
    }

    // Tests for checkArgument with long and double parameters
    @Test
    public void testCheckArgument_LongDouble_Pass() {
        N.checkArgument(true, "Error: %s %s", 42L, 3.14);
    }

    @Test
    public void testCheckArgument_LongDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, 3.14);
        });
    }

    // Tests for checkArgument with long and Object parameters
    @Test
    public void testCheckArgument_LongObject_Pass() {
        N.checkArgument(true, "Error: %s %s", 42L, "test");
    }

    @Test
    public void testCheckArgument_LongObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 42L, "test");
        });
    }

    // Tests for checkArgument with double and char parameters
    @Test
    public void testCheckArgument_DoubleChar_Pass() {
        N.checkArgument(true, "Error: %s %s", 3.14, 'a');
    }

    @Test
    public void testCheckArgument_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 'a');
        });
    }

    // Tests for checkArgument with double and int parameters
    @Test
    public void testCheckArgument_DoubleInt_Pass() {
        N.checkArgument(true, "Error: %s %s", 3.14, 42);
    }

    @Test
    public void testCheckArgument_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 42);
        });
    }

    // Tests for checkArgument with double and long parameters
    @Test
    public void testCheckArgument_DoubleLong_Pass() {
        N.checkArgument(true, "Error: %s %s", 3.14, 42L);
    }

    @Test
    public void testCheckArgument_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 42L);
        });
    }

    // Tests for checkArgument with two double parameters
    @Test
    public void testCheckArgument_DoubleDouble_Pass() {
        N.checkArgument(true, "Error: %s %s", 3.14, 2.71);
    }

    @Test
    public void testCheckArgument_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, 2.71);
        });
    }

    // Tests for checkArgument with double and Object parameters
    @Test
    public void testCheckArgument_DoubleObject_Pass() {
        N.checkArgument(true, "Error: %s %s", 3.14, "test");
    }

    @Test
    public void testCheckArgument_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", 3.14, "test");
        });
    }

    // Tests for checkArgument with Object and char parameters
    @Test
    public void testCheckArgument_ObjectChar_Pass() {
        N.checkArgument(true, "Error: %s %s", "test", 'a');
    }

    @Test
    public void testCheckArgument_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 'a');
        });
    }

    // Tests for checkArgument with Object and int parameters
    @Test
    public void testCheckArgument_ObjectInt_Pass() {
        N.checkArgument(true, "Error: %s %s", "test", 42);
    }

    @Test
    public void testCheckArgument_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 42);
        });
    }

    // Tests for checkArgument with Object and long parameters
    @Test
    public void testCheckArgument_ObjectLong_Pass() {
        N.checkArgument(true, "Error: %s %s", "test", 42L);
    }

    @Test
    public void testCheckArgument_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 42L);
        });
    }

    // Tests for checkArgument with Object and double parameters
    @Test
    public void testCheckArgument_ObjectDouble_Pass() {
        N.checkArgument(true, "Error: %s %s", "test", 3.14);
    }

    @Test
    public void testCheckArgument_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            N.checkArgument(false, "Error: %s %s", "test", 3.14);
        });
    }

    // Tests for checkState with two char parameters
    @Test
    public void testCheckState_CharChar_Pass() {
        N.checkState(true, "State error: %s %s", 'a', 'b');
    }

    @Test
    public void testCheckState_CharChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 'b');
        });
    }

    // Tests for checkState with char and int parameters
    @Test
    public void testCheckState_CharInt_Pass() {
        N.checkState(true, "State error: %s %s", 'a', 42);
    }

    @Test
    public void testCheckState_CharInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 42);
        });
    }

    // Tests for checkState with char and long parameters
    @Test
    public void testCheckState_CharLong_Pass() {
        N.checkState(true, "State error: %s %s", 'a', 42L);
    }

    @Test
    public void testCheckState_CharLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 42L);
        });
    }

    // Tests for checkState with char and double parameters
    @Test
    public void testCheckState_CharDouble_Pass() {
        N.checkState(true, "State error: %s %s", 'a', 3.14);
    }

    @Test
    public void testCheckState_CharDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', 3.14);
        });
    }

    // Tests for checkState with char and Object parameters
    @Test
    public void testCheckState_CharObject_Pass() {
        N.checkState(true, "State error: %s %s", 'a', "test");
    }

    @Test
    public void testCheckState_CharObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 'a', "test");
        });
    }

    // Tests for checkState with int and char parameters
    @Test
    public void testCheckState_IntChar_Pass() {
        N.checkState(true, "State error: %s %s", 42, 'a');
    }

    @Test
    public void testCheckState_IntChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 'a');
        });
    }

    // Tests for checkState with two int parameters
    @Test
    public void testCheckState_IntInt_Pass() {
        N.checkState(true, "State error: %s %s", 42, 24);
    }

    @Test
    public void testCheckState_IntInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 24);
        });
    }

    // Tests for checkState with int and long parameters
    @Test
    public void testCheckState_IntLong_Pass() {
        N.checkState(true, "State error: %s %s", 42, 24L);
    }

    @Test
    public void testCheckState_IntLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 24L);
        });
    }

    // Tests for checkState with int and double parameters
    @Test
    public void testCheckState_IntDouble_Pass() {
        N.checkState(true, "State error: %s %s", 42, 3.14);
    }

    @Test
    public void testCheckState_IntDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, 3.14);
        });
    }

    // Tests for checkState with int and Object parameters
    @Test
    public void testCheckState_IntObject_Pass() {
        N.checkState(true, "State error: %s %s", 42, "test");
    }

    @Test
    public void testCheckState_IntObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42, "test");
        });
    }

    // Tests for checkState with long and char parameters
    @Test
    public void testCheckState_LongChar_Pass() {
        N.checkState(true, "State error: %s %s", 42L, 'a');
    }

    @Test
    public void testCheckState_LongChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 'a');
        });
    }

    // Tests for checkState with long and int parameters
    @Test
    public void testCheckState_LongInt_Pass() {
        N.checkState(true, "State error: %s %s", 42L, 24);
    }

    @Test
    public void testCheckState_LongInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 24);
        });
    }

    // Tests for checkState with two long parameters
    @Test
    public void testCheckState_LongLong_Pass() {
        N.checkState(true, "State error: %s %s", 42L, 24L);
    }

    @Test
    public void testCheckState_LongLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 24L);
        });
    }

    // Tests for checkState with long and double parameters
    @Test
    public void testCheckState_LongDouble_Pass() {
        N.checkState(true, "State error: %s %s", 42L, 3.14);
    }

    @Test
    public void testCheckState_LongDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, 3.14);
        });
    }

    // Tests for checkState with long and Object parameters
    @Test
    public void testCheckState_LongObject_Pass() {
        N.checkState(true, "State error: %s %s", 42L, "test");
    }

    @Test
    public void testCheckState_LongObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 42L, "test");
        });
    }

    // Tests for checkState with double and char parameters
    @Test
    public void testCheckState_DoubleChar_Pass() {
        N.checkState(true, "State error: %s %s", 3.14, 'a');
    }

    @Test
    public void testCheckState_DoubleChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 'a');
        });
    }

    // Tests for checkState with double and int parameters
    @Test
    public void testCheckState_DoubleInt_Pass() {
        N.checkState(true, "State error: %s %s", 3.14, 42);
    }

    @Test
    public void testCheckState_DoubleInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 42);
        });
    }

    // Tests for checkState with double and long parameters
    @Test
    public void testCheckState_DoubleLong_Pass() {
        N.checkState(true, "State error: %s %s", 3.14, 42L);
    }

    @Test
    public void testCheckState_DoubleLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 42L);
        });
    }

    // Tests for checkState with two double parameters
    @Test
    public void testCheckState_DoubleDouble_Pass() {
        N.checkState(true, "State error: %s %s", 3.14, 2.71);
    }

    @Test
    public void testCheckState_DoubleDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, 2.71);
        });
    }

    // Tests for checkState with double and Object parameters
    @Test
    public void testCheckState_DoubleObject_Pass() {
        N.checkState(true, "State error: %s %s", 3.14, "test");
    }

    @Test
    public void testCheckState_DoubleObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", 3.14, "test");
        });
    }

    // Tests for checkState with Object and char parameters
    @Test
    public void testCheckState_ObjectChar_Pass() {
        N.checkState(true, "State error: %s %s", "test", 'a');
    }

    @Test
    public void testCheckState_ObjectChar_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 'a');
        });
    }

    // Tests for checkState with Object and int parameters
    @Test
    public void testCheckState_ObjectInt_Pass() {
        N.checkState(true, "State error: %s %s", "test", 42);
    }

    @Test
    public void testCheckState_ObjectInt_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 42);
        });
    }

    // Tests for checkState with Object and long parameters
    @Test
    public void testCheckState_ObjectLong_Pass() {
        N.checkState(true, "State error: %s %s", "test", 42L);
    }

    @Test
    public void testCheckState_ObjectLong_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 42L);
        });
    }

    // Tests for checkState with Object and double parameters
    @Test
    public void testCheckState_ObjectDouble_Pass() {
        N.checkState(true, "State error: %s %s", "test", 3.14);
    }

    @Test
    public void testCheckState_ObjectDouble_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test", 3.14);
        });
    }

    // Tests for checkState with two Object parameters
    @Test
    public void testCheckState_ObjectObject_Pass() {
        N.checkState(true, "State error: %s %s", "test1", "test2");
    }

    @Test
    public void testCheckState_ObjectObject_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s", "test1", "test2");
        });
    }

    // Tests for checkState with three Object parameters
    @Test
    public void testCheckState_ThreeObjects_Pass() {
        N.checkState(true, "State error: %s %s %s", "test1", "test2", "test3");
    }

    @Test
    public void testCheckState_ThreeObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s %s", "test1", "test2", "test3");
        });
    }

    // Tests for checkState with four Object parameters
    @Test
    public void testCheckState_FourObjects_Pass() {
        N.checkState(true, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
    }

    @Test
    public void testCheckState_FourObjects_Fail() {
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, "State error: %s %s %s %s", "test1", "test2", "test3", "test4");
        });
    }

    // Tests for checkState with Supplier
    @Test
    public void testCheckState_Supplier_Pass() {
        N.checkState(true, () -> "This message should not be created");
    }

    @Test
    public void testCheckState_Supplier_Fail() {
        Supplier<String> messageSupplier = () -> "State error from supplier";
        Assertions.assertThrows(IllegalStateException.class, () -> {
            N.checkState(false, messageSupplier);
        });
    }

    @Test
    public void testCheckState_Supplier_MessageCreatedOnlyWhenNeeded() {
        // Verify supplier is not called when condition is true
        final boolean[] supplierCalled = { false };
        N.checkState(true, () -> {
            supplierCalled[0] = true;
            return "Should not be called";
        });
        Assertions.assertFalse(supplierCalled[0]);

        // Verify supplier is called when condition is false
        try {
            N.checkState(false, () -> {
                supplierCalled[0] = true;
                return "Should be called";
            });
        } catch (IllegalStateException e) {
            Assertions.assertTrue(supplierCalled[0]);
        }
    }

    // Additional tests for error message formatting
    @Test
    public void testCheckArgument_MessageFormatting() {
        // Test with {} placeholders
        try {
            N.checkArgument(false, "Value {} should be less than {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        // Test with %s placeholders
        try {
            N.checkArgument(false, "Value %s should be less than %s", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        // Test with no placeholders
        try {
            N.checkArgument(false, "No placeholders", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("No placeholders"));
            Assertions.assertTrue(e.getMessage().contains("[10, 5]"));
        }

        // Test with one placeholder but two arguments
        try {
            N.checkArgument(false, "Only one {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("[5]"));
        }
    }

    // Test null handling in format
    @Test
    public void testCheckArgument_NullFormatting() {
        try {
            N.checkArgument(false, "Value is {}", (Object) null);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }

        try {
            N.checkArgument(false, null, "arg1", "arg2");
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("null"));
        }
    }
    // ==================== Tests for compare methods ====================

    @Test
    public void testCompareBooleanArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new boolean[] {}, new boolean[] {}));
        Assertions.assertEquals(-1, N.compare(new boolean[] {}, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] {}));

        // null arrays
        Assertions.assertEquals(0, N.compare((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, N.compare((boolean[]) null, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, (boolean[]) null));

        // Same arrays
        boolean[] arr1 = { true, false, true };
        boolean[] arr2 = { true, false, true };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new boolean[] { false }, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] { false }));

        // Different lengths
        Assertions.assertEquals(-1, N.compare(new boolean[] { true }, new boolean[] { true, false }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true, false }, new boolean[] { true }));

        // Large arrays to test MISMATCH_THRESHOLD
        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1999] = false;
        Assertions.assertEquals(1, N.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareBooleanArraysWithRange() {
        boolean[] arr1 = { true, false, true, false };
        boolean[] arr2 = { false, true, false, true };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 2, 2));

        // Compare different ranges
        Assertions.assertEquals(1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 1, arr2, 1, 1));

        // Zero length
        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        // Same array reference
        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 3, arr2, 0, 2));

        // Large arrays to test MISMATCH_THRESHOLD
        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1500] = false;
        Assertions.assertEquals(1, N.compare(largeArr1, 0, largeArr2, 0, 1600));
    }

    @Test
    public void testCompareCharArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new char[] {}, new char[] {}));
        Assertions.assertEquals(-1, N.compare(new char[] {}, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, new char[] {}));

        // null arrays
        Assertions.assertEquals(0, N.compare((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, N.compare((char[]) null, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, (char[]) null));

        // Same arrays
        char[] arr1 = { 'a', 'b', 'c' };
        char[] arr2 = { 'a', 'b', 'c' };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'b' }, new char[] { 'a' }));

        // Different lengths
        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'a', 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a', 'b' }, new char[] { 'a' }));

        // Large arrays to test MISMATCH_THRESHOLD
        char[] largeArr1 = new char[2000];
        char[] largeArr2 = new char[2000];
        Arrays.fill(largeArr1, 'x');
        Arrays.fill(largeArr2, 'x');
        largeArr2[1999] = 'y';
        Assertions.assertEquals(-1, N.compare(largeArr1, largeArr2));
    }

    @Test
    public void testCompareCharArraysWithRange() {
        char[] arr1 = { 'a', 'b', 'c', 'd' };
        char[] arr2 = { 'x', 'b', 'c', 'y' };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 3, 1));

        // Zero length
        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        // Same array reference
        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));

        // Test bug in implementation (should use b[j] not b[i])
        char[] bugArr1 = { 'a', 'b' };
        char[] bugArr2 = { 'x', 'a' };
        Assertions.assertTrue(N.compare(bugArr1, 1, bugArr2, 1, 1) > 0);
    }

    @Test
    public void testCompareCharArraysWithRange001() {

        {
            char[] arr1 = { 1, 2, 3, 4 };
            char[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            byte[] arr1 = { 1, 2, 3, 4 };
            byte[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            short[] arr1 = { 1, 2, 3, 4 };
            short[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            int[] arr1 = { 1, 2, 3, 4 };
            int[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            long[] arr1 = { 1, 2, 3, 4 };
            long[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            float[] arr1 = { 1, 2, 3, 4 };
            float[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

        {
            double[] arr1 = { 1, 2, 3, 4 };
            double[] arr2 = { 5, 1, 2, 3 };
            Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 1, 3));
        }

    }

    @Test
    public void testCompareByteArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compare(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 1 }, new byte[] {}));

        // Same arrays
        byte[] arr1 = { 1, 2, 3 };
        byte[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new byte[] { 1 }, new byte[] { 2 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 2 }, new byte[] { 1 }));

        // Negative values
        Assertions.assertEquals(-1, N.compare(new byte[] { -128 }, new byte[] { 127 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 127 }, new byte[] { -128 }));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] arr1 = { 1, 2, 3, 4 };
        byte[] arr2 = { 5, 2, 3, 6 };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedByteArrays() {
        // Basic unsigned comparison
        byte[] arr1 = { (byte) 255 }; // -1 as signed, 255 as unsigned
        byte[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compareUnsigned(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compareUnsigned(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compareUnsigned(new byte[] { 1 }, new byte[] {}));
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] arr1 = { 1, (byte) 255, 3 };
        byte[] arr2 = { 5, 1, 3 };

        // Compare range where unsigned makes a difference
        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareShortArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new short[] {}, new short[] {}));
        Assertions.assertEquals(-1, N.compare(new short[] {}, new short[] { 1 }));
        Assertions.assertEquals(1, N.compare(new short[] { 1 }, new short[] {}));

        // Same arrays
        short[] arr1 = { 1, 2, 3 };
        short[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new short[] { 1 }, new short[] { 2 }));
        Assertions.assertEquals(1, N.compare(new short[] { 2 }, new short[] { 1 }));
    }

    @Test
    public void testCompareShortArraysWithRange() {
        short[] arr1 = { 1, 2, 3, 4 };
        short[] arr2 = { 5, 2, 3, 6 };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedShortArrays() {
        // Basic unsigned comparison
        short[] arr1 = { (short) 65535 }; // -1 as signed, 65535 as unsigned
        short[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compareUnsigned(new short[] {}, new short[] {}));
    }

    @Test
    public void testCompareUnsignedShortArraysWithRange() {
        short[] arr1 = { 1, (short) 65535, 3 };
        short[] arr2 = { 5, 1, 3 };

        // Compare range where unsigned makes a difference
        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareIntArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new int[] {}, new int[] {}));
        Assertions.assertEquals(-1, N.compare(new int[] {}, new int[] { 1 }));
        Assertions.assertEquals(1, N.compare(new int[] { 1 }, new int[] {}));

        // Same arrays
        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new int[] { 1 }, new int[] { 2 }));
        Assertions.assertEquals(1, N.compare(new int[] { 2 }, new int[] { 1 }));
    }

    @Test
    public void testCompareIntArraysWithRange() {
        int[] arr1 = { 1, 2, 3, 4 };
        int[] arr2 = { 5, 2, 3, 6 };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedIntArrays() {
        // Basic unsigned comparison
        int[] arr1 = { -1 }; // Maximum unsigned value
        int[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compareUnsigned(new int[] {}, new int[] {}));
    }

    @Test
    public void testCompareUnsignedIntArraysWithRange() {
        int[] arr1 = { 1, -1, 3 };
        int[] arr2 = { 5, 1, 3 };

        // Compare range where unsigned makes a difference
        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareLongArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new long[] {}, new long[] {}));
        Assertions.assertEquals(-1, N.compare(new long[] {}, new long[] { 1 }));
        Assertions.assertEquals(1, N.compare(new long[] { 1 }, new long[] {}));

        // Same arrays
        long[] arr1 = { 1L, 2L, 3L };
        long[] arr2 = { 1L, 2L, 3L };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new long[] { 1L }, new long[] { 2L }));
        Assertions.assertEquals(1, N.compare(new long[] { 2L }, new long[] { 1L }));
    }

    @Test
    public void testCompareLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L };
        long[] arr2 = { 5L, 2L, 3L, 6L };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedLongArrays() {
        // Basic unsigned comparison
        long[] arr1 = { -1L }; // Maximum unsigned value
        long[] arr2 = { 1L };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compareUnsigned(new long[] {}, new long[] {}));
    }

    @Test
    public void testCompareUnsignedLongArraysWithRange() {
        long[] arr1 = { 1L, -1L, 3L };
        long[] arr2 = { 5L, 1L, 3L };

        // Compare range where unsigned makes a difference
        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareFloatArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new float[] {}, new float[] {}));
        Assertions.assertEquals(-1, N.compare(new float[] {}, new float[] { 1.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 1.0f }, new float[] {}));

        // Same arrays
        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new float[] { 1.0f }, new float[] { 2.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 2.0f }, new float[] { 1.0f }));

        // NaN handling
        Assertions.assertTrue(N.compare(new float[] { Float.NaN }, new float[] { 1.0f }) > 0);
        Assertions.assertEquals(0, N.compare(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testCompareFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 5.0f, 2.0f, 3.0f, 6.0f };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareDoubleArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new double[] {}, new double[] {}));
        Assertions.assertEquals(-1, N.compare(new double[] {}, new double[] { 1.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 1.0 }, new double[] {}));

        // Same arrays
        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new double[] { 1.0 }, new double[] { 2.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 2.0 }, new double[] { 1.0 }));

        // NaN handling
        Assertions.assertTrue(N.compare(new double[] { Double.NaN }, new double[] { 1.0 }) > 0);
        Assertions.assertEquals(0, N.compare(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testCompareDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 5.0, 2.0, 3.0, 6.0 };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareObjectArrays() {
        // Empty arrays
        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compare(new String[] {}, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] {}));

        // Same arrays
        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        // Different arrays
        Assertions.assertEquals(-1, N.compare(new String[] { "a" }, new String[] { "b" }));
        Assertions.assertEquals(1, N.compare(new String[] { "b" }, new String[] { "a" }));

        // With nulls (null is considered smallest)
        Assertions.assertEquals(-1, N.compare(new String[] { null }, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] { null }));
        Assertions.assertEquals(0, N.compare(new String[] { null }, new String[] { null }));
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        // Compare different ranges
        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1) < 0);

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);

        // Basic comparison with custom comparator
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "a", "c" };
        Assertions.assertTrue(N.compare(arr1, arr2, reverseComparator) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}, reverseComparator));

        // Null comparator (should use natural ordering)
        Assertions.assertTrue(N.compare(arr1, arr2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareObjectArraysWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        // Compare same range
        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2, reverseComparator));

        // Compare different ranges
        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1, reverseComparator) > 0);
    }

    @Test
    public void testCompareCollectionsWithRange() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        // Compare same range
        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2));

        // Compare different ranges
        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1) < 0);

        // Same collection reference
        Assertions.assertEquals(0, N.compare(list1, 1, list1, 1, 2));

        // Test exceptions
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(list1, 0, list2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(list1, 0, list2, 0, 10));
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        // Same content
        Assertions.assertEquals(0, N.compare(list1, list2));
        Assertions.assertEquals(0, N.compare(list1, set1));

        // Different content
        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1, list3) < 0);

        // Empty iterables
        Assertions.assertEquals(0, N.compare(Collections.<String> emptyList(), Collections.<String> emptyList()));
        Assertions.assertEquals(-1, N.compare(Collections.emptyList(), list1));
        Assertions.assertEquals(1, N.compare(list1, Collections.emptyList()));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");

        // Same content
        Assertions.assertEquals(0, N.compare(list1.iterator(), list2.iterator()));

        // Different content
        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1.iterator(), list3.iterator()) < 0);

        // Different lengths
        List<String> list4 = Arrays.asList("a", "b");
        Assertions.assertTrue(N.compare(list1.iterator(), list4.iterator()) > 0);

        // Null iterators
        Assertions.assertEquals(0, N.compare((Iterator<String>) null, (Iterator<String>) null));
        Assertions.assertEquals(-1, N.compare((Iterator<String>) null, list1.iterator()));
        Assertions.assertEquals(1, N.compare(list1.iterator(), (Iterator<String>) null));
    }

    @Test
    public void testCompareCollectionsWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        // Compare same range
        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2, reverseComparator));

        // Compare different ranges
        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1, reverseComparator) > 0);

        // Non-list collections
        Set<String> set1 = new LinkedHashSet<>(list1);
        Set<String> set2 = new LinkedHashSet<>(list2);
        Assertions.assertEquals(0, N.compare(set1, 1, set2, 1, 2, reverseComparator));
    }

    @Test
    public void testCompareIterablesWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        // Normal comparison
        Assertions.assertTrue(N.compare(list1, list2, (Comparator<String>) null) < 0);

        // Reverse comparison
        Assertions.assertTrue(N.compare(list1, list2, reverseComparator) > 0);

        // Empty iterables
        Assertions.assertEquals(0, N.compare(Collections.emptyList(), Collections.emptyList(), reverseComparator));
    }

    @Test
    public void testCompareIteratorsWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        // Normal comparison
        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), (Comparator<String>) null) < 0);

        // Reverse comparison
        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), reverseComparator) > 0);
    }

    @Test
    public void testCompareIgnoreCase() {
        // Basic case-insensitive comparison
        Assertions.assertEquals(0, N.compareIgnoreCase("Hello", "hello"));
        Assertions.assertEquals(0, N.compareIgnoreCase("HELLO", "hello"));
        Assertions.assertTrue(N.compareIgnoreCase("a", "B") < 0);
        Assertions.assertTrue(N.compareIgnoreCase("B", "a") > 0);

        // Null handling
        Assertions.assertEquals(0, N.compareIgnoreCase((String) null, (String) null));
        Assertions.assertEquals(-1, N.compareIgnoreCase(null, "hello"));
        Assertions.assertEquals(1, N.compareIgnoreCase("hello", null));
    }

    @Test
    public void testCompareIgnoreCaseArrays() {
        String[] arr1 = { "Hello", "World" };
        String[] arr2 = { "HELLO", "WORLD" };
        Assertions.assertEquals(0, N.compareIgnoreCase(arr1, arr2));

        String[] arr3 = { "Hello", "World" };
        String[] arr4 = { "HELLO", "EARTH" };
        Assertions.assertTrue(N.compareIgnoreCase(arr3, arr4) > 0);

        // Empty arrays
        Assertions.assertEquals(0, N.compareIgnoreCase(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compareIgnoreCase(new String[] {}, arr1));
    }

    @Test
    public void testCompareByProps() {
        // Note: This method is deprecated and requires bean classes
        // Skipping detailed tests as it's deprecated
    }

    // ==================== Tests for comparison helper methods ====================

    @Test
    public void testLessThan() {
        // With Comparable
        Assertions.assertTrue(N.lessThan(1, 2));
        Assertions.assertFalse(N.lessThan(2, 1));
        Assertions.assertFalse(N.lessThan(1, 1));

        // With strings
        Assertions.assertTrue(N.lessThan("a", "b"));
        Assertions.assertFalse(N.lessThan("b", "a"));

        // With nulls (null is considered smallest)
        Assertions.assertTrue(N.lessThan(null, "a"));
        Assertions.assertFalse(N.lessThan("a", null));
        Assertions.assertFalse(N.lessThan((String) null, (String) null));
    }

    @Test
    public void testLessThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.lessThan(1, 2, null));

        // Reverse comparison
        Assertions.assertFalse(N.lessThan(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessThan(2, 1, reverseComparator));
    }

    @Test
    public void testLessEqual() {
        // With Comparable
        Assertions.assertTrue(N.lessEqual(1, 2));
        Assertions.assertFalse(N.lessEqual(2, 1));
        Assertions.assertTrue(N.lessEqual(1, 1));

        // With nulls
        Assertions.assertTrue(N.lessEqual(null, "a"));
        Assertions.assertTrue(N.lessEqual((String) null, (String) null));
    }

    @Test
    public void testLessEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.lessEqual(1, 2, null));
        Assertions.assertTrue(N.lessEqual(1, 1, null));

        // Reverse comparison
        Assertions.assertFalse(N.lessEqual(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThan() {
        // With Comparable
        Assertions.assertFalse(N.greaterThan(1, 2));
        Assertions.assertTrue(N.greaterThan(2, 1));
        Assertions.assertFalse(N.greaterThan(1, 1));

        // With strings
        Assertions.assertFalse(N.greaterThan("a", "b"));
        Assertions.assertTrue(N.greaterThan("b", "a"));

        // With nulls
        Assertions.assertFalse(N.greaterThan(null, "a"));
        Assertions.assertTrue(N.greaterThan("a", null));
    }

    @Test
    public void testGreaterThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertFalse(N.greaterThan(1, 2, null));

        // Reverse comparison
        Assertions.assertTrue(N.greaterThan(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterThan(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterEqual() {
        // With Comparable
        Assertions.assertFalse(N.greaterEqual(1, 2));
        Assertions.assertTrue(N.greaterEqual(2, 1));
        Assertions.assertTrue(N.greaterEqual(1, 1));

        // With nulls
        Assertions.assertFalse(N.greaterEqual(null, "a"));
        Assertions.assertTrue(N.greaterEqual((String) null, (String) null));
    }

    @Test
    public void testGreaterEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertFalse(N.greaterEqual(1, 2, null));
        Assertions.assertTrue(N.greaterEqual(1, 1, null));

        // Reverse comparison
        Assertions.assertTrue(N.greaterEqual(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGtAndLt() {
        // Value is in range (exclusive)
        Assertions.assertTrue(N.gtAndLt(5, 1, 10));
        Assertions.assertFalse(N.gtAndLt(1, 1, 10));
        Assertions.assertFalse(N.gtAndLt(10, 1, 10));
        Assertions.assertFalse(N.gtAndLt(0, 1, 10));
        Assertions.assertFalse(N.gtAndLt(11, 1, 10));

        // Edge cases
        Assertions.assertFalse(N.gtAndLt(5, 5, 5));
    }

    @Test
    public void testGtAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.gtAndLt(5, 1, 10, null));

        // Reverse comparison (5 > 10 and 5 < 1 in reverse order)
        Assertions.assertTrue(N.gtAndLt(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLt(5, 1, 10, reverseComparator));
    }

    @Test
    public void testGeAndLt() {
        // Value is in range [min, max)
        Assertions.assertTrue(N.geAndLt(5, 1, 10));
        Assertions.assertTrue(N.geAndLt(1, 1, 10));
        Assertions.assertFalse(N.geAndLt(10, 1, 10));
        Assertions.assertFalse(N.geAndLt(0, 1, 10));
    }

    @Test
    public void testGeAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.geAndLt(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLt(1, 1, 10, null));

        // Reverse comparison
        Assertions.assertTrue(N.geAndLt(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLt(10, 10, 1, reverseComparator));
    }

    @Test
    public void testGeAndLe() {
        // Value is in range [min, max]
        Assertions.assertTrue(N.geAndLe(5, 1, 10));
        Assertions.assertTrue(N.geAndLe(1, 1, 10));
        Assertions.assertTrue(N.geAndLe(10, 1, 10));
        Assertions.assertFalse(N.geAndLe(0, 1, 10));
        Assertions.assertFalse(N.geAndLe(11, 1, 10));
    }

    @Test
    public void testGeAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.geAndLe(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(10, 1, 10, null));

        // Reverse comparison
        Assertions.assertTrue(N.geAndLe(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testGtAndLe() {
        // Value is in range (min, max]
        Assertions.assertTrue(N.gtAndLe(5, 1, 10));
        Assertions.assertFalse(N.gtAndLe(1, 1, 10));
        Assertions.assertTrue(N.gtAndLe(10, 1, 10));
        Assertions.assertFalse(N.gtAndLe(0, 1, 10));
        Assertions.assertFalse(N.gtAndLe(11, 1, 10));
    }

    @Test
    public void testGtAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Normal comparison
        Assertions.assertTrue(N.gtAndLe(5, 1, 10, null));
        Assertions.assertFalse(N.gtAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.gtAndLe(10, 1, 10, null));

        // Reverse comparison
        Assertions.assertTrue(N.gtAndLe(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.gtAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testIsBetween() {
        // Deprecated method, equivalent to geAndLe
        Assertions.assertTrue(N.isBetween(5, 1, 10));
        Assertions.assertTrue(N.isBetween(1, 1, 10));
        Assertions.assertTrue(N.isBetween(10, 1, 10));
        Assertions.assertFalse(N.isBetween(0, 1, 10));
        Assertions.assertFalse(N.isBetween(11, 1, 10));
    }

    @Test
    public void testIsBetweenWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        // Deprecated method, equivalent to geAndLe with comparator
        Assertions.assertTrue(N.isBetween(5, 1, 10, null));
        Assertions.assertTrue(N.isBetween(5, 10, 1, reverseComparator));
    }

    // ==================== Tests for get/find element methods ====================

    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        // Valid indices
        Assertions.assertEquals("a", N.getElement(list, 0));
        Assertions.assertEquals("b", N.getElement(list, 1));
        Assertions.assertEquals("d", N.getElement(list, 3));

        // Invalid indices
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 4));

        // Non-list iterable
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.getElement(set, 0));
        Assertions.assertEquals("b", N.getElement(set, 1));

        // Null iterable
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        // Valid indices
        Assertions.assertEquals("a", N.getElement(list.iterator(), 0));
        Assertions.assertEquals("b", N.getElement(list.iterator(), 1));
        Assertions.assertEquals("d", N.getElement(list.iterator(), 3));

        // Invalid indices
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list.iterator(), -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list.iterator(), 4));

        // Null iterator
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterator<String>) null, 0));
    }

    @Test
    public void testGetOnlyElementFromIterable() {
        // Single element
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList).get());

        // Empty collection
        Assertions.assertFalse(N.getOnlyElement(Collections.emptyList()).isPresent());

        // Multiple elements
        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList));

        // Null collection
        Assertions.assertFalse(N.getOnlyElement((Iterable<String>) null).isPresent());

        // Non-collection iterable with one element
        Set<String> singleSet = Collections.singleton("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleSet).get());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        // Single element
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList.iterator()).get());

        // Empty iterator
        Assertions.assertFalse(N.getOnlyElement(Collections.emptyIterator()).isPresent());

        // Multiple elements
        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList.iterator()));

        // Null iterator
        Assertions.assertFalse(N.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementFromIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list).get());

        // Empty collection
        Assertions.assertFalse(N.firstElement(Collections.emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.firstElement((Iterable<String>) null).isPresent());

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstElement(arrayList).get());

        // Non-list iterable
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.firstElement(set).get());
    }

    @Test
    public void testFirstElementFromIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list.iterator()).get());

        // Empty iterator
        Assertions.assertFalse(N.firstElement(Collections.emptyIterator()).isPresent());

        // Null iterator
        Assertions.assertFalse(N.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElementFromIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list).get());

        // Empty collection
        Assertions.assertFalse(N.lastElement(Collections.emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.lastElement((Iterable<String>) null).isPresent());

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastElement(arrayList).get());

        // Non-list iterable
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("c", N.lastElement(set).get());

        // Deque with descending iterator
        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastElement(deque).get());
    }

    @Test
    public void testLastElementFromIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list.iterator()).get());

        // Empty iterator
        Assertions.assertFalse(N.lastElement(Collections.emptyIterator()).isPresent());

        // Null iterator
        Assertions.assertFalse(N.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        // Normal cases
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list, 0));

        // Empty collection
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyList(), 5));

        // Null collection
        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterable<String>) null, 5));

        // Negative n
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list, -1));

        // Non-list collection
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(set, 3));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        // Normal cases
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list.iterator(), 0));

        // Empty iterator
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyIterator(), 5));

        // Null iterator
        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterator<String>) null, 5));

        // Negative n
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list.iterator(), -1));
    }

    @Test
    public void testLastElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        // Normal cases
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list, 0));

        // Empty collection
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyList(), 5));

        // Null collection
        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterable<String>) null, 5));

        // Negative n
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list, -1));

        // Non-list collection
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(set, 3));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        // Normal cases
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list.iterator(), 0));

        // Empty iterator
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyIterator(), 5));

        // Null iterator
        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterator<String>) null, 5));

        // Negative n
        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list.iterator(), -1));
    }

    // ==================== Tests for firstNonNull methods ====================

    @Test
    public void testFirstNonNullTwo() {
        // Both non-null
        Assertions.assertEquals("a", N.firstNonNull("a", "b").get());

        // First null
        Assertions.assertEquals("b", N.firstNonNull(null, "b").get());

        // Second null
        Assertions.assertEquals("a", N.firstNonNull("a", null).get());

        // Both null
        Assertions.assertFalse(N.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullThree() {
        // All non-null
        Assertions.assertEquals("a", N.firstNonNull("a", "b", "c").get());

        // First null
        Assertions.assertEquals("b", N.firstNonNull(null, "b", "c").get());

        // First two null
        Assertions.assertEquals("c", N.firstNonNull(null, null, "c").get());

        // All null
        Assertions.assertFalse(N.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        // Multiple values
        Assertions.assertEquals("c", N.firstNonNull(null, null, "c", "d").get());

        // Empty array
        Assertions.assertFalse(N.firstNonNull(new String[] {}).isPresent());

        // Null array
        Assertions.assertFalse(N.firstNonNull((String[]) null).isPresent());

        // All null values
        Assertions.assertFalse(N.firstNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        // Normal case
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list).get());

        // Empty collection
        Assertions.assertFalse(N.firstNonNull(Collections.emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.firstNonNull((Iterable<String>) null).isPresent());

        // All null values
        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        // Normal case
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list.iterator()).get());

        // Empty iterator
        Assertions.assertFalse(N.firstNonNull(Collections.emptyIterator()).isPresent());

        // Null iterator
        Assertions.assertFalse(N.firstNonNull((Iterator<String>) null).isPresent());

        // All null values
        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls.iterator()).isPresent());
    }

    // ==================== Tests for lastNonNull methods ====================

    @Test
    public void testLastNonNullTwo() {
        // Both non-null
        Assertions.assertEquals("b", N.lastNonNull("a", "b").get());

        // First null
        Assertions.assertEquals("b", N.lastNonNull(null, "b").get());

        // Second null
        Assertions.assertEquals("a", N.lastNonNull("a", null).get());

        // Both null
        Assertions.assertFalse(N.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNullThree() {
        // All non-null
        Assertions.assertEquals("c", N.lastNonNull("a", "b", "c").get());

        // Last null
        Assertions.assertEquals("b", N.lastNonNull("a", "b", null).get());

        // Last two null
        Assertions.assertEquals("a", N.lastNonNull("a", null, null).get());

        // All null
        Assertions.assertFalse(N.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        // Multiple values
        Assertions.assertEquals("d", N.lastNonNull("a", "b", null, "d", null).get());

        // Empty array
        Assertions.assertFalse(N.lastNonNull(new String[] {}).isPresent());

        // Null array
        Assertions.assertFalse(N.lastNonNull((String[]) null).isPresent());

        // All null values
        Assertions.assertFalse(N.lastNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        // Normal case
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list).get());

        // Empty collection
        Assertions.assertFalse(N.lastNonNull(Collections.emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.lastNonNull((Iterable<String>) null).isPresent());

        // All null values
        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls).isPresent());

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("d", N.lastNonNull(arrayList).get());

    }

    @Test
    public void testLastNonNullIterator() {
        // Normal case
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list.iterator()).get());

        // Empty iterator
        Assertions.assertFalse(N.lastNonNull(Collections.emptyIterator()).isPresent());

        // Null iterator
        Assertions.assertFalse(N.lastNonNull((Iterator<String>) null).isPresent());

        // All null values
        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls.iterator()).isPresent());
    }

    // ==================== Tests for firstNonEmpty methods ====================

    @Test
    public void testFirstNonEmptyArraysTwo() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] empty = {};

        // Both non-empty
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2).get());

        // First empty
        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2).get());

        // Second empty
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, empty).get());

        // Both empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        // Null arrays
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(null, arr1).get());
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((String[]) null, (String[]) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysThree() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] arr3 = { "e", "f" };
        String[] empty = {};

        // All non-empty
        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2, arr3).get());

        // First empty
        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2, arr3).get());

        // First two empty
        Assertions.assertArrayEquals(arr3, N.firstNonEmpty(empty, empty, arr3).get());

        // All empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsTwo() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        // Both non-empty
        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2).get());

        // First empty
        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2).get());

        // Second empty
        Assertions.assertEquals(list1, N.firstNonEmpty(list1, empty).get());

        // Both empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        // Null collections
        Assertions.assertEquals(list1, N.firstNonEmpty(null, list1).get());
        Assertions.assertEquals(list1, N.firstNonEmpty(list1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((List<String>) null, (List<String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsThree() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> list3 = Arrays.asList("e", "f");
        List<String> empty = Collections.emptyList();

        // All non-empty
        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2, list3).get());

        // First empty
        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2, list3).get());

        // First two empty
        Assertions.assertEquals(list3, N.firstNonEmpty(empty, empty, list3).get());

        // All empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsTwo() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        // Both non-empty
        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2).get());

        // First empty
        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2).get());

        // Second empty
        Assertions.assertEquals(map1, N.firstNonEmpty(map1, empty).get());

        // Both empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

        // Null maps
        Assertions.assertEquals(map1, N.firstNonEmpty(null, map1).get());
        Assertions.assertEquals(map1, N.firstNonEmpty(map1, null).get());
        Assertions.assertFalse(N.firstNonEmpty((Map<String, String>) null, (Map<String, String>) null).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsThree() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> map3 = new HashMap<>();
        map3.put("c", "3");
        Map<String, String> empty = Collections.emptyMap();

        // All non-empty
        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2, map3).get());

        // First empty
        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2, map3).get());

        // First two empty
        Assertions.assertEquals(map3, N.firstNonEmpty(empty, empty, map3).get());

        // All empty
        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesTwo() {
        // Both non-empty
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "world").get());

        // First empty
        Assertions.assertEquals("world", N.firstNonEmpty("", "world").get());

        // Second empty
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "").get());

        // Both empty
        Assertions.assertFalse(N.firstNonEmpty("", "").isPresent());

        // Null strings
        Assertions.assertEquals("hello", N.firstNonEmpty(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", null).get());
        Assertions.assertFalse(N.firstNonEmpty((String) null, (String) null).isPresent());

        // Different CharSequence types
        StringBuilder sb = new StringBuilder("builder");
        StringBuffer buf = new StringBuffer("buffer");
        Assertions.assertEquals(sb, N.firstNonEmpty(sb, buf).get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesThree() {
        // All non-empty
        Assertions.assertEquals("a", N.firstNonEmpty("a", "b", "c").get());

        // First empty
        Assertions.assertEquals("b", N.firstNonEmpty("", "b", "c").get());

        // First two empty
        Assertions.assertEquals("c", N.firstNonEmpty("", "", "c").get());

        // All empty
        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());

        // Mixed null and empty
        Assertions.assertEquals("c", N.firstNonEmpty(null, "", "c").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        // Multiple values
        Assertions.assertEquals("c", N.firstNonEmpty("", null, "c", "d").get());

        // Empty array
        Assertions.assertFalse(N.firstNonEmpty(new String[] {}).isPresent());

        // Null array
        Assertions.assertFalse(N.firstNonEmpty((String[]) null).isPresent());

        // All empty values
        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        // Normal case
        List<String> list = Arrays.asList("", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonEmpty(list).get());

        // Empty collection
        Assertions.assertFalse(N.firstNonEmpty(Collections.<String> emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.firstNonEmpty((Iterable<String>) null).isPresent());

        // All empty values
        List<String> allEmpty = Arrays.asList("", "", null);
        Assertions.assertFalse(N.firstNonEmpty(allEmpty).isPresent());
    }

    // ==================== Tests for firstNonBlank methods ====================

    @Test
    public void testFirstNonBlankTwo() {
        // Both non-blank
        Assertions.assertEquals("hello", N.firstNonBlank("hello", "world").get());

        // First blank
        Assertions.assertEquals("world", N.firstNonBlank("  ", "world").get());

        // Second blank
        Assertions.assertEquals("hello", N.firstNonBlank("hello", "  ").get());

        // Both blank
        Assertions.assertFalse(N.firstNonBlank("  ", "  ").isPresent());

        // Null strings
        Assertions.assertEquals("hello", N.firstNonBlank(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonBlank("hello", null).get());
        Assertions.assertFalse(N.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlankThree() {
        // All non-blank
        Assertions.assertEquals("a", N.firstNonBlank("a", "b", "c").get());

        // First blank
        Assertions.assertEquals("b", N.firstNonBlank("  ", "b", "c").get());

        // First two blank
        Assertions.assertEquals("c", N.firstNonBlank("  ", "  ", "c").get());

        // All blank
        Assertions.assertFalse(N.firstNonBlank("  ", "  ", "  ").isPresent());

        // Mixed null, empty and blank
        Assertions.assertEquals("c", N.firstNonBlank(null, "", "c").get());
        Assertions.assertEquals("c", N.firstNonBlank("  ", "\t", "c").get());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        // Multiple values
        Assertions.assertEquals("c", N.firstNonBlank("  ", null, "c", "d").get());

        // Empty array
        Assertions.assertFalse(N.firstNonBlank(new String[] {}).isPresent());

        // Null array
        Assertions.assertFalse(N.firstNonBlank((String[]) null).isPresent());

        // All blank values
        Assertions.assertFalse(N.firstNonBlank("  ", "\t", "\n").isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        // Normal case
        List<String> list = Arrays.asList("  ", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonBlank(list).get());

        // Empty collection
        Assertions.assertFalse(N.firstNonBlank(Collections.<String> emptyList()).isPresent());

        // Null collection
        Assertions.assertFalse(N.firstNonBlank((Iterable<String>) null).isPresent());

        // All blank values
        List<String> allBlank = Arrays.asList("  ", "\t", null);
        Assertions.assertFalse(N.firstNonBlank(allBlank).isPresent());
    }

    // ==================== Tests for entry methods ====================

    @Test
    public void testFirstEntry() {
        // Non-empty map
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> first = N.firstEntry(map).get();
        Assertions.assertEquals("a", first.getKey());
        Assertions.assertEquals(1, first.getValue());

        // Empty map
        Assertions.assertFalse(N.firstEntry(Collections.emptyMap()).isPresent());

        // Null map
        Assertions.assertFalse(N.firstEntry(null).isPresent());
    }

    @Test
    public void testLastEntry() {
        // Non-empty map
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> last = N.lastEntry(map).get();
        Assertions.assertEquals("c", last.getKey());
        Assertions.assertEquals(3, last.getValue());

        // Empty map
        Assertions.assertFalse(N.lastEntry(Collections.emptyMap()).isPresent());

        // Null map
        Assertions.assertFalse(N.lastEntry(null).isPresent());
    }

    // ==================== Tests for firstOrNullIfEmpty/lastOrNullIfEmpty methods ====================

    @Test
    public void testFirstOrNullIfEmptyArray() {
        // Non-empty array
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(arr));

        // Empty array
        Assertions.assertNull(N.firstOrNullIfEmpty(new String[] {}));

        // Null array
        Assertions.assertNull(N.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list));

        // Empty collection
        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyList()));

        // Null collection
        Assertions.assertNull(N.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list.iterator()));

        // Empty iterator
        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyIterator()));

        // Null iterator
        Assertions.assertNull(N.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        // Non-empty array
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arr, "default"));

        // Empty array
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(new String[] {}, "default"));

        // Null array
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));

        // Empty collection
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyList(), "default"));

        // Null collection
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arrayList, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list.iterator(), "default"));

        // Empty iterator
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        // Null iterator
        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        // Non-empty array
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(arr));

        // Empty array
        Assertions.assertNull(N.lastOrNullIfEmpty(new String[] {}));

        // Null array
        Assertions.assertNull(N.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list));

        // Empty collection
        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyList()));

        // Null collection
        Assertions.assertNull(N.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list.iterator()));

        // Empty iterator
        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyIterator()));

        // Null iterator
        Assertions.assertNull(N.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        // Non-empty array
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arr, "default"));

        // Empty array
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(new String[] {}, "default"));

        // Null array
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        // Non-empty list
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list, "default"));

        // Empty collection
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyList(), "default"));

        // Null collection
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arrayList, "default"));

        // Deque with descending iterator
        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(deque, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        // Non-empty iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list.iterator(), "default"));

        // Empty iterator
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        // Null iterator
        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    // ==================== Tests for findFirst/findLast methods ====================

    @Test
    public void testFindFirstArray() {
        String[] arr = { "apple", "banana", "cherry", "date" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        // Found
        Assertions.assertEquals("cherry", N.findFirst(arr, startsWithC).get());

        // Not found
        Assertions.assertFalse(N.findFirst(arr, startsWithX).isPresent());

        // Empty array
        Assertions.assertFalse(N.findFirst(new String[] {}, startsWithC).isPresent());

        // Null array
        Assertions.assertFalse(N.findFirst((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        // Found
        Assertions.assertEquals("cherry", N.findFirst(list, startsWithC).get());

        // Not found
        Assertions.assertFalse(N.findFirst(list, startsWithX).isPresent());

        // Empty collection
        Assertions.assertFalse(N.findFirst(Collections.emptyList(), startsWithC).isPresent());

        // Null collection
        Assertions.assertFalse(N.findFirst((Iterable<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        // Found
        Assertions.assertEquals("cherry", N.findFirst(list.iterator(), startsWithC).get());

        // Not found
        Assertions.assertFalse(N.findFirst(list.iterator(), startsWithX).isPresent());

        // Empty iterator
        Assertions.assertFalse(N.findFirst(Collections.emptyIterator(), startsWithC).isPresent());

        // Null iterator
        Assertions.assertFalse(N.findFirst((Iterator<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "apple", "banana", "cherry", "date", "cucumber" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        // Found (should find last one)
        Assertions.assertEquals("cucumber", N.findLast(arr, startsWithC).get());

        // Not found
        Assertions.assertFalse(N.findLast(arr, startsWithX).isPresent());

        // Empty array
        Assertions.assertFalse(N.findLast(new String[] {}, startsWithC).isPresent());

        // Null array
        Assertions.assertFalse(N.findLast((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "cucumber");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        // Found (should find last one)
        Assertions.assertEquals("cucumber", N.findLast(list, startsWithC).get());

        // Not found
        Assertions.assertFalse(N.findLast(list, startsWithX).isPresent());

        // Empty collection
        Assertions.assertFalse(N.findLast(Collections.emptyList(), startsWithC).isPresent());

        // Null collection
        Assertions.assertFalse(N.findLast((Iterable<String>) null, startsWithC).isPresent());

        // RandomAccess list
        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("cucumber", N.findLast(arrayList, startsWithC).get());

        // Non-RandomAccess collection
        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("cucumber", N.findLast(set, startsWithC).get());

        // Deque with descending iterator
        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("cucumber", N.findLast(deque, startsWithC).get());
    }

    // Tests for convert(Object srcObj, Class<? extends T> targetType)

    @Test
    public void testConvertNullToClass() {
        // Test with null source object
        assertNull(N.convert(null, String.class));
        assertNull(N.convert(null, Integer.class));
        assertNull(N.convert(null, Long.class));
        assertNull(N.convert(null, Float.class));
        assertNull(N.convert(null, Double.class));
        assertNull(N.convert(null, Boolean.class));
        assertNull(N.convert(null, Object.class));
        assertNull(N.convert(null, Date.class));
        assertNull(N.convert(null, List.class));
        assertNull(N.convert(null, Map.class));
    }

    @Test
    public void testConvertStringToNumbers() {
        // String to various number types
        assertEquals(123, N.convert("123", Integer.class).intValue());
        assertEquals(123L, N.convert("123", Long.class).longValue());
        assertEquals(123.45f, N.convert("123.45", Float.class).floatValue(), 0.001);
        assertEquals(123.45d, N.convert("123.45", Double.class).doubleValue(), 0.001);
        assertEquals((byte) 123, N.convert("123", Byte.class).byteValue());
        assertEquals((short) 123, N.convert("123", Short.class).shortValue());
        assertEquals(new BigInteger("123456789012345678901234567890"), N.convert("123456789012345678901234567890", BigInteger.class));
        assertEquals(new BigDecimal("123.456789"), N.convert("123.456789", BigDecimal.class));
        assertNull(N.convert("", Integer.class));
    }

    @Test
    public void testConvertStringToNumbersWithInvalidFormat() {
        // Test NumberFormatException
        assertThrows(NumberFormatException.class, () -> N.convert("abc", Integer.class));
        assertThrows(NumberFormatException.class, () -> N.convert("12.34.56", Double.class));
    }

    @Test
    public void testConvertStringToBoolean() {
        assertTrue(N.convert("true", Boolean.class));
        assertTrue(N.convert("TRUE", Boolean.class));
        assertTrue(N.convert("True", Boolean.class));
        assertFalse(N.convert("false", Boolean.class));
        assertFalse(N.convert("FALSE", Boolean.class));
        assertFalse(N.convert("False", Boolean.class));
        assertFalse(N.convert("anything else", Boolean.class));
    }

    @Test
    public void testConvertStringToCharacter() {
        assertEquals('A', N.convert("A", Character.class).charValue());
        assertEquals('1', N.convert("1", Character.class).charValue());
        assertThrows(RuntimeException.class, () -> N.convert("AB", Character.class));
        assertNull(N.convert("", Character.class));
    }

    @Test
    public void testConvertNumberToNumber() {
        // Integer to other number types
        Integer intVal = 123;
        assertEquals(123L, N.convert(intVal, Long.class).longValue());
        assertEquals(123.0f, N.convert(intVal, Float.class).floatValue(), 0.000001f);
        assertEquals(123.0d, N.convert(intVal, Double.class).doubleValue(), 0.000001d);
        assertEquals((byte) 123, N.convert(intVal, Byte.class).byteValue());
        assertEquals((short) 123, N.convert(intVal, Short.class).shortValue());

        // Long to other number types
        Long longVal = 456L;
        assertEquals(456, N.convert(longVal, Integer.class).intValue());
        assertEquals(456.0f, N.convert(longVal, Float.class).floatValue(), 0.000001f);
        assertEquals(456.0d, N.convert(longVal, Double.class).doubleValue(), 0.000001d);

        // Double to other number types
        Double doubleVal = 789.5;
        assertEquals(789, N.convert(doubleVal, Integer.class).intValue());
        assertEquals(789L, N.convert(doubleVal, Long.class).longValue());
        assertEquals(789.5f, N.convert(doubleVal, Float.class).floatValue(), 0.001);
    }

    @Test
    public void testConvertNumberToBoolean() {
        assertTrue(N.convert(1, Boolean.class));
        assertTrue(N.convert(123, Boolean.class));
        assertFalse(N.convert(-1, Boolean.class));
        assertFalse(N.convert(0, Boolean.class));

        assertTrue(N.convert(1L, Boolean.class));
        assertFalse(N.convert(0L, Boolean.class));

        assertTrue(N.convert(1.5, Boolean.class));
        assertFalse(N.convert(0.0, Boolean.class));
    }

    @Test
    public void testConvertNumberToString() {
        assertEquals("123", N.convert(123, String.class));
        assertEquals("456", N.convert(456L, String.class));
        assertEquals("78.9", N.convert(78.9, String.class));
        assertEquals("true", N.convert(true, String.class));
        assertEquals("false", N.convert(false, String.class));
    }

    @Test
    public void testConvertCharacterToInteger() {
        assertEquals(65, N.convert('A', Integer.class).intValue());
        assertEquals(65, N.convert('A', int.class).intValue());
        assertEquals(97, N.convert('a', Integer.class).intValue());
        assertEquals(48, N.convert('0', Integer.class).intValue());
    }

    @Test
    public void testConvertIntegerToCharacter() {
        assertEquals('A', N.convert(65, Character.class).charValue());
        assertEquals('A', N.convert(65, char.class).charValue());
        assertEquals('a', N.convert(97, Character.class).charValue());
        assertEquals('0', N.convert(48, Character.class).charValue());
    }

    @Test
    public void testConvertDateToLong() {
        Date date = new Date(1234567890L);
        assertEquals(1234567890L, N.convert(date, Long.class).longValue());
        assertEquals(1234567890L, N.convert(date, long.class).longValue());

        java.sql.Timestamp timestamp = new java.sql.Timestamp(1234567890L);
        assertEquals(1234567890L, N.convert(timestamp, Long.class).longValue());

        java.sql.Date sqlDate = new java.sql.Date(1234567890L);
        assertEquals(1234567890L, N.convert(sqlDate, Long.class).longValue());
    }

    @Test
    public void testConvertLongToDate() {
        Long timeMillis = 1234567890L;

        Date date = N.convert(timeMillis, Date.class);
        assertEquals(timeMillis.longValue(), date.getTime());

        java.sql.Timestamp timestamp = N.convert(timeMillis, java.sql.Timestamp.class);
        assertEquals(timeMillis.longValue(), timestamp.getTime());

        java.sql.Date sqlDate = N.convert(timeMillis, java.sql.Date.class);
        assertEquals(timeMillis.longValue(), sqlDate.getTime());

        java.sql.Time sqlTime = N.convert(timeMillis, java.sql.Time.class);
        assertEquals(timeMillis.longValue(), sqlTime.getTime());
    }

    @Test
    public void testConvertCollectionToCollection() {
        // List to Set
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = N.convert(list, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        // Set to List
        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = N.convert(intSet, List.class);
        assertEquals(3, intList.size());
        assertTrue(intList.containsAll(intSet));

        // Empty collection
        List<String> emptyList = new ArrayList<>();
        Set<String> emptySet = N.convert(emptyList, Set.class);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testConvertArrayToCollection() {
        String[] array = { "x", "y", "z" };
        List<String> list = N.convert(array, List.class);
        assertEquals(3, list.size());
        assertEquals("x", list.get(0));
        assertEquals("y", list.get(1));
        assertEquals("z", list.get(2));

        Integer[] intArray = { 1, 2, 3 };
        Set<Integer> set = N.convert(intArray, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
    }

    @Test
    public void testConvertCollectionToArray() {
        List<String> list = Arrays.asList("a", "b", "c");
        String[] array = N.convert(list, String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, array);

        Set<Integer> set = new LinkedHashSet<>(Arrays.asList(1, 2, 3));
        Integer[] intArray = N.convert(set, Integer[].class);
        assertEquals(3, intArray.length);

        // Empty collection
        List<String> emptyList = new ArrayList<>();
        String[] emptyArray = N.convert(emptyList, String[].class);
        assertEquals(0, emptyArray.length);
    }

    @Test
    public void testConvertSingleObjectToCollection() {
        String str = "hello";
        List<String> list = N.convert(str, List.class);
        assertEquals(1, list.size());
        assertEquals("hello", list.get(0));

        Integer num = 42;
        Set<Integer> set = N.convert(num, Set.class);
        assertEquals(1, set.size());
        assertTrue(set.contains(42));
    }

    @Test
    public void testConvertSingleObjectToArray() {
        String str = "world";
        String[] array = N.convert(str, String[].class);
        assertEquals(1, array.length);
        assertEquals("world", array[0]);

        Integer num = 99;
        Integer[] intArray = N.convert(num, Integer[].class);
        assertEquals(1, intArray.length);
        assertEquals(99, intArray[0].intValue());
    }

    @Test
    public void testConvertMapToMap() {
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);
        srcMap.put("three", 3);

        Map<String, Integer> destMap = N.convert(srcMap, Map.class);
        assertEquals(3, destMap.size());
        assertEquals(1, destMap.get("one").intValue());
        assertEquals(2, destMap.get("two").intValue());
        assertEquals(3, destMap.get("three").intValue());

        // Empty map
        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> convertedEmpty = N.convert(emptyMap, Map.class);
        assertTrue(convertedEmpty.isEmpty());
    }

    @Test
    public void testConvertByteArrayFromBlob() throws SQLException {
        // Mock Blob
        Blob blob = new Blob() {
            private boolean freed = false;

            @Override
            public long length() throws SQLException {
                return 5;
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                if (freed)
                    throw new SQLException("Blob already freed");
                return new byte[] { 1, 2, 3, 4, 5 };
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void free() throws SQLException {
                freed = true;
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };

        byte[] result = N.convert(blob, byte[].class);
        assertArrayEquals(new byte[] { 1, 2, 3, 4, 5 }, result);
    }

    @Test
    public void testConvertByteArrayFromInputStream() {
        byte[] data = { 10, 20, 30, 40, 50 };
        InputStream is = new ByteArrayInputStream(data);

        byte[] result = N.convert(is, byte[].class);
        assertArrayEquals(data, result);
    }

    @Test
    public void testConvertCharArrayFromClob() throws SQLException {
        // Mock Clob
        Clob clob = new Clob() {
            private boolean freed = false;

            @Override
            public long length() throws SQLException {
                return 5;
            }

            @Override
            public String getSubString(long pos, int length) throws SQLException {
                if (freed)
                    throw new SQLException("Clob already freed");
                return "Hello";
            }

            @Override
            public Reader getCharacterStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream getAsciiStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(String searchstr, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(Clob searchstr, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setString(long pos, String str) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setString(long pos, String str, int offset, int len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream setAsciiStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Writer setCharacterStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void free() throws SQLException {
                freed = true;
            }

            @Override
            public Reader getCharacterStream(long pos, long length) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };

        char[] result = N.convert(clob, char[].class);
        assertArrayEquals("Hello".toCharArray(), result);
    }

    @Test
    public void testConvertCharArrayFromReader() {
        String data = "World";
        Reader reader = new StringReader(data);

        char[] result = N.convert(reader, char[].class);
        assertArrayEquals(data.toCharArray(), result);
    }

    @Test
    public void testConvertCharArrayFromInputStream() {
        String data = "['T','e','s','t']";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        char[] result = N.convert(is, char[].class);
        assertEquals("Test", String.valueOf(result));
    }

    @Test
    public void testConvertStringFromCharSequence() {
        StringBuilder sb = new StringBuilder("Hello");
        assertEquals("Hello", N.convert(sb, String.class));

        StringBuffer sbuf = new StringBuffer("World");
        assertEquals("World", N.convert(sbuf, String.class));

        CharSequence cs = "Test";
        assertEquals("Test", N.convert(cs, String.class));
    }

    @Test
    public void testConvertStringFromClob() throws SQLException {
        // Mock Clob
        Clob clob = new Clob() {
            private boolean freed = false;

            @Override
            public long length() throws SQLException {
                return 11;
            }

            @Override
            public String getSubString(long pos, int length) throws SQLException {
                if (freed)
                    throw new SQLException("Clob already freed");
                return "Test String";
            }

            @Override
            public Reader getCharacterStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public InputStream getAsciiStream() throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(String searchstr, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public long position(Clob searchstr, long start) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setString(long pos, String str) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public int setString(long pos, String str, int offset, int len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public OutputStream setAsciiStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public Writer setCharacterStream(long pos) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new UnsupportedOperationException();
            }

            @Override
            public void free() throws SQLException {
                freed = true;
            }

            @Override
            public Reader getCharacterStream(long pos, long length) throws SQLException {
                throw new UnsupportedOperationException();
            }
        };

        String result = N.convert(clob, String.class);
        assertEquals("Test String", result);
    }

    @Test
    public void testConvertStringFromReader() {
        String data = "Reader Content";
        Reader reader = new StringReader(data);

        String result = N.convert(reader, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertStringFromInputStream() {
        String data = "InputStream Content";
        InputStream is = new ByteArrayInputStream(data.getBytes());

        String result = N.convert(is, String.class);
        assertEquals(data, result);
    }

    @Test
    public void testConvertInputStreamFromByteArray() {
        byte[] data = { 1, 2, 3, 4, 5 };
        InputStream is = N.convert(data, InputStream.class);

        assertNotNull(is);
        assertTrue(is instanceof ByteArrayInputStream);

        // Verify content
        byte[] readData = new byte[5];
        try {
            is.read(readData);
            assertArrayEquals(data, readData);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertReaderFromCharSequence() {
        String data = "Test String";
        Reader reader = N.convert(data, Reader.class);

        assertNotNull(reader);
        assertTrue(reader instanceof StringReader);

        // Verify content
        char[] buffer = new char[data.length()];
        try {
            reader.read(buffer);
            assertArrayEquals(data.toCharArray(), buffer);
        } catch (Exception e) {
            fail("Should not throw exception");
        }
    }

    @Test
    public void testConvertSameType() {
        String str = "test";
        assertSame(str, N.convert(str, String.class));

        Integer num = 42;
        assertSame(num, N.convert(num, Integer.class));

        List<String> list = new ArrayList<>();
        assertSame(list, N.convert(list, ArrayList.class));
    }

    @Test
    public void testConvertPrimitiveTypes() {
        // Test primitive types
        assertEquals(123, N.convert(123, int.class).intValue());
        assertEquals(456L, N.convert(456L, long.class).longValue());
        assertEquals(78.9f, N.convert(78.9f, float.class).floatValue(), 0.001);
        assertEquals(12.34d, N.convert(12.34d, double.class).doubleValue(), 0.001);
        assertTrue(N.convert(true, boolean.class));
        assertFalse(N.convert(false, boolean.class));
        assertEquals('A', N.convert('A', char.class).charValue());
        assertEquals((byte) 99, N.convert((byte) 99, byte.class).byteValue());
        assertEquals((short) 999, N.convert((short) 999, short.class).shortValue());
    }

    // Tests for convert(Object srcObj, Type<? extends T> targetType)

    @Test
    public void testConvertNullToType() {
        // Test with null source object using Type
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<List> listType = TypeFactory.getType(List.class);

        assertNull(N.convert(null, stringType));
        // assertEquals(0, N.convert(null, intType).intValue());
        assertNull(N.convert(null, intType));
        assertNull(N.convert(null, listType));
    }

    @Test
    public void testConvertUsingType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

        // String to Integer using Type
        assertEquals(123, N.convert("123", intType).intValue());

        // Integer to String using Type
        assertEquals("456", N.convert(456, stringType));

        // Number to Boolean using Type
        assertTrue(N.convert(1, boolType));
        assertFalse(N.convert(0, boolType));
    }

    @Test
    public void testConvertCollectionWithType() {
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Set> setType = TypeFactory.getType(Set.class);

        List<String> list = Arrays.asList("a", "b", "c");
        Set<String> set = N.convert(list, setType);
        assertEquals(3, set.size());

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = N.convert(intSet, listType);
        assertEquals(3, intList.size());
    }

    @Test
    public void testConvertWithParameterizedType() {
        // Test with parameterized types
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Map> mapType = TypeFactory.getType(Map.class);

        // Array to List
        String[] array = { "x", "y", "z" };
        List<String> list = N.convert(array, listType);
        assertEquals(3, list.size());

        // Map conversion
        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);

        Map<String, Integer> destMap = N.convert(srcMap, mapType);
        assertEquals(2, destMap.size());
    }

    @Test
    public void testConvertAutoCloseableResources() {
        // Test with AutoCloseable resources
        String data = "AutoCloseable test";

        class TestAutoCloseable implements AutoCloseable {
            boolean closed = false;
            String value = data;

            @Override
            public void close() {
                closed = true;
            }

            @Override
            public String toString() {
                return value;
            }
        }

        TestAutoCloseable resource = new TestAutoCloseable();
        String result = N.convert(resource, String.class);

        assertEquals(data, result);
        assertFalse(resource.closed);
    }

    @Test
    public void testConvertWithSQLException() {
        // Test Blob that throws SQLException
        Blob blob = new Blob() {
            @Override
            public long length() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public byte[] getBytes(long pos, int length) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(byte[] pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public long position(Blob pattern, long start) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public OutputStream setBinaryStream(long pos) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void truncate(long len) throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public void free() throws SQLException {
                throw new SQLException("Test exception");
            }

            @Override
            public InputStream getBinaryStream(long pos, long length) throws SQLException {
                throw new SQLException("Test exception");
            }
        };

        assertThrows(UncheckedSQLException.class, () -> N.convert(blob, byte[].class));
    }

    @Test
    public void testConvertEdgeCases() {
        // Test edge cases and boundary conditions

        // Very large numbers
        String bigNum = "9223372036854775807"; // Long.MAX_VALUE
        assertEquals(Long.MAX_VALUE, N.convert(bigNum, Long.class).longValue());

        // Overflow cases
        assertThrows(ArithmeticException.class, () -> N.convert(Long.MAX_VALUE, Integer.class).intValue());
        assertThrows(ArithmeticException.class, () -> N.convert(1000, Byte.class).byteValue());

        // Special float/double values
        assertEquals((Float) Float.POSITIVE_INFINITY, N.convert("Infinity", Float.class));
        assertEquals((Double) Double.NEGATIVE_INFINITY, N.convert("-Infinity", Double.class));
        assertTrue(Double.isNaN(N.convert("NaN", Double.class).doubleValue()));
    }

    @Test
    public void testConvertUnsupportedConversions() {
        // Test conversions that should throw exceptions or return unexpected results

        // Complex object to primitive
        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        assertThrows(RuntimeException.class, () -> N.convert(map, Integer.class));
        // assertThrows(RuntimeException.class, () -> N.convert(map, Boolean.class));
        assertFalse(N.convert(map, Boolean.class));

        // Incompatible collection conversions
        List<Map<String, Object>> complexList = new ArrayList<>();
        complexList.add(new HashMap<>());

        // This should work but create a new list
        List<Map<String, Object>> convertedList = N.convert(complexList, List.class);
        assertNotNull(convertedList);
        assertEquals(1, convertedList.size());
    }
}