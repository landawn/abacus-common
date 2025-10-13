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
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedSQLException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;

@Tag("new-test")
public class CommonUtil110Test extends TestBase {

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
        final boolean[] supplierCalled = { false };
        N.checkState(true, () -> {
            supplierCalled[0] = true;
            return "Should not be called";
        });
        Assertions.assertFalse(supplierCalled[0]);

        try {
            N.checkState(false, () -> {
                supplierCalled[0] = true;
                return "Should be called";
            });
        } catch (IllegalStateException e) {
            Assertions.assertTrue(supplierCalled[0]);
        }
    }

    @Test
    public void testCheckArgument_MessageFormatting() {
        try {
            N.checkArgument(false, "Value {} should be less than {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            N.checkArgument(false, "Value %s should be less than %s", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("5"));
        }

        try {
            N.checkArgument(false, "No placeholders", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("No placeholders"));
            Assertions.assertTrue(e.getMessage().contains("[10, 5]"));
        }

        try {
            N.checkArgument(false, "Only one {}", 10, 5);
            Assertions.fail("Should throw IllegalArgumentException");
        } catch (IllegalArgumentException e) {
            Assertions.assertTrue(e.getMessage().contains("10"));
            Assertions.assertTrue(e.getMessage().contains("[5]"));
        }
    }

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

    @Test
    public void testCompareBooleanArrays() {
        Assertions.assertEquals(0, N.compare(new boolean[] {}, new boolean[] {}));
        Assertions.assertEquals(-1, N.compare(new boolean[] {}, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] {}));

        Assertions.assertEquals(0, N.compare((boolean[]) null, (boolean[]) null));
        Assertions.assertEquals(-1, N.compare((boolean[]) null, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, (boolean[]) null));

        boolean[] arr1 = { true, false, true };
        boolean[] arr2 = { true, false, true };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new boolean[] { false }, new boolean[] { true }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true }, new boolean[] { false }));

        Assertions.assertEquals(-1, N.compare(new boolean[] { true }, new boolean[] { true, false }));
        Assertions.assertEquals(1, N.compare(new boolean[] { true, false }, new boolean[] { true }));

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

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 2, 2));

        Assertions.assertEquals(1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 1, arr2, 1, 1));

        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 3, arr2, 0, 2));

        boolean[] largeArr1 = new boolean[2000];
        boolean[] largeArr2 = new boolean[2000];
        Arrays.fill(largeArr1, true);
        Arrays.fill(largeArr2, true);
        largeArr2[1500] = false;
        Assertions.assertEquals(1, N.compare(largeArr1, 0, largeArr2, 0, 1600));
    }

    @Test
    public void testCompareCharArrays() {
        Assertions.assertEquals(0, N.compare(new char[] {}, new char[] {}));
        Assertions.assertEquals(-1, N.compare(new char[] {}, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, new char[] {}));

        Assertions.assertEquals(0, N.compare((char[]) null, (char[]) null));
        Assertions.assertEquals(-1, N.compare((char[]) null, new char[] { 'a' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a' }, (char[]) null));

        char[] arr1 = { 'a', 'b', 'c' };
        char[] arr2 = { 'a', 'b', 'c' };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'b' }, new char[] { 'a' }));

        Assertions.assertEquals(-1, N.compare(new char[] { 'a' }, new char[] { 'a', 'b' }));
        Assertions.assertEquals(1, N.compare(new char[] { 'a', 'b' }, new char[] { 'a' }));

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

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 3, 1));

        Assertions.assertEquals(0, N.compare(arr1, 0, arr2, 0, 0));

        Assertions.assertEquals(0, N.compare(arr1, 1, arr1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));

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
        Assertions.assertEquals(0, N.compare(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compare(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 1 }, new byte[] {}));

        byte[] arr1 = { 1, 2, 3 };
        byte[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new byte[] { 1 }, new byte[] { 2 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 2 }, new byte[] { 1 }));

        Assertions.assertEquals(-1, N.compare(new byte[] { -128 }, new byte[] { 127 }));
        Assertions.assertEquals(1, N.compare(new byte[] { 127 }, new byte[] { -128 }));
    }

    @Test
    public void testCompareByteArraysWithRange() {
        byte[] arr1 = { 1, 2, 3, 4 };
        byte[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
        Assertions.assertEquals(-1, N.compare(arr1, 3, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedByteArrays() {
        byte[] arr1 = { (byte) 255 };
        byte[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new byte[] {}, new byte[] {}));
        Assertions.assertEquals(-1, N.compareUnsigned(new byte[] {}, new byte[] { 1 }));
        Assertions.assertEquals(1, N.compareUnsigned(new byte[] { 1 }, new byte[] {}));
    }

    @Test
    public void testCompareUnsignedByteArraysWithRange() {
        byte[] arr1 = { 1, (byte) 255, 3 };
        byte[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compareUnsigned(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareShortArrays() {
        Assertions.assertEquals(0, N.compare(new short[] {}, new short[] {}));
        Assertions.assertEquals(-1, N.compare(new short[] {}, new short[] { 1 }));
        Assertions.assertEquals(1, N.compare(new short[] { 1 }, new short[] {}));

        short[] arr1 = { 1, 2, 3 };
        short[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new short[] { 1 }, new short[] { 2 }));
        Assertions.assertEquals(1, N.compare(new short[] { 2 }, new short[] { 1 }));
    }

    @Test
    public void testCompareShortArraysWithRange() {
        short[] arr1 = { 1, 2, 3, 4 };
        short[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedShortArrays() {
        short[] arr1 = { (short) 65535 };
        short[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new short[] {}, new short[] {}));
    }

    @Test
    public void testCompareUnsignedShortArraysWithRange() {
        short[] arr1 = { 1, (short) 65535, 3 };
        short[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareIntArrays() {
        Assertions.assertEquals(0, N.compare(new int[] {}, new int[] {}));
        Assertions.assertEquals(-1, N.compare(new int[] {}, new int[] { 1 }));
        Assertions.assertEquals(1, N.compare(new int[] { 1 }, new int[] {}));

        int[] arr1 = { 1, 2, 3 };
        int[] arr2 = { 1, 2, 3 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new int[] { 1 }, new int[] { 2 }));
        Assertions.assertEquals(1, N.compare(new int[] { 2 }, new int[] { 1 }));
    }

    @Test
    public void testCompareIntArraysWithRange() {
        int[] arr1 = { 1, 2, 3, 4 };
        int[] arr2 = { 5, 2, 3, 6 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedIntArrays() {
        int[] arr1 = { -1 };
        int[] arr2 = { 1 };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new int[] {}, new int[] {}));
    }

    @Test
    public void testCompareUnsignedIntArraysWithRange() {
        int[] arr1 = { 1, -1, 3 };
        int[] arr2 = { 5, 1, 3 };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareLongArrays() {
        Assertions.assertEquals(0, N.compare(new long[] {}, new long[] {}));
        Assertions.assertEquals(-1, N.compare(new long[] {}, new long[] { 1 }));
        Assertions.assertEquals(1, N.compare(new long[] { 1 }, new long[] {}));

        long[] arr1 = { 1L, 2L, 3L };
        long[] arr2 = { 1L, 2L, 3L };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new long[] { 1L }, new long[] { 2L }));
        Assertions.assertEquals(1, N.compare(new long[] { 2L }, new long[] { 1L }));
    }

    @Test
    public void testCompareLongArraysWithRange() {
        long[] arr1 = { 1L, 2L, 3L, 4L };
        long[] arr2 = { 5L, 2L, 3L, 6L };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareUnsignedLongArrays() {
        long[] arr1 = { -1L };
        long[] arr2 = { 1L };
        Assertions.assertTrue(N.compareUnsigned(arr1, arr2) > 0);

        Assertions.assertEquals(0, N.compareUnsigned(new long[] {}, new long[] {}));
    }

    @Test
    public void testCompareUnsignedLongArraysWithRange() {
        long[] arr1 = { 1L, -1L, 3L };
        long[] arr2 = { 5L, 1L, 3L };

        Assertions.assertTrue(N.compareUnsigned(arr1, 1, arr2, 1, 1) > 0);
    }

    @Test
    public void testCompareFloatArrays() {
        Assertions.assertEquals(0, N.compare(new float[] {}, new float[] {}));
        Assertions.assertEquals(-1, N.compare(new float[] {}, new float[] { 1.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 1.0f }, new float[] {}));

        float[] arr1 = { 1.0f, 2.0f, 3.0f };
        float[] arr2 = { 1.0f, 2.0f, 3.0f };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new float[] { 1.0f }, new float[] { 2.0f }));
        Assertions.assertEquals(1, N.compare(new float[] { 2.0f }, new float[] { 1.0f }));

        Assertions.assertTrue(N.compare(new float[] { Float.NaN }, new float[] { 1.0f }) > 0);
        Assertions.assertEquals(0, N.compare(new float[] { Float.NaN }, new float[] { Float.NaN }));
    }

    @Test
    public void testCompareFloatArraysWithRange() {
        float[] arr1 = { 1.0f, 2.0f, 3.0f, 4.0f };
        float[] arr2 = { 5.0f, 2.0f, 3.0f, 6.0f };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareDoubleArrays() {
        Assertions.assertEquals(0, N.compare(new double[] {}, new double[] {}));
        Assertions.assertEquals(-1, N.compare(new double[] {}, new double[] { 1.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 1.0 }, new double[] {}));

        double[] arr1 = { 1.0, 2.0, 3.0 };
        double[] arr2 = { 1.0, 2.0, 3.0 };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new double[] { 1.0 }, new double[] { 2.0 }));
        Assertions.assertEquals(1, N.compare(new double[] { 2.0 }, new double[] { 1.0 }));

        Assertions.assertTrue(N.compare(new double[] { Double.NaN }, new double[] { 1.0 }) > 0);
        Assertions.assertEquals(0, N.compare(new double[] { Double.NaN }, new double[] { Double.NaN }));
    }

    @Test
    public void testCompareDoubleArraysWithRange() {
        double[] arr1 = { 1.0, 2.0, 3.0, 4.0 };
        double[] arr2 = { 5.0, 2.0, 3.0, 6.0 };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertEquals(-1, N.compare(arr1, 0, arr2, 0, 1));
    }

    @Test
    public void testCompareObjectArrays() {
        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compare(new String[] {}, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] {}));

        String[] arr1 = { "a", "b", "c" };
        String[] arr2 = { "a", "b", "c" };
        Assertions.assertEquals(0, N.compare(arr1, arr2));

        Assertions.assertEquals(-1, N.compare(new String[] { "a" }, new String[] { "b" }));
        Assertions.assertEquals(1, N.compare(new String[] { "b" }, new String[] { "a" }));

        Assertions.assertEquals(-1, N.compare(new String[] { null }, new String[] { "a" }));
        Assertions.assertEquals(1, N.compare(new String[] { "a" }, new String[] { null }));
        Assertions.assertEquals(0, N.compare(new String[] { null }, new String[] { null }));
    }

    @Test
    public void testCompareObjectArraysWithRange() {
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2));

        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1) < 0);

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(arr1, 0, arr2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(arr1, 0, arr2, 0, 10));
    }

    @Test
    public void testCompareObjectArraysWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);

        String[] arr1 = { "a", "b" };
        String[] arr2 = { "a", "c" };
        Assertions.assertTrue(N.compare(arr1, arr2, reverseComparator) > 0);

        Assertions.assertEquals(0, N.compare(new String[] {}, new String[] {}, reverseComparator));

        Assertions.assertTrue(N.compare(arr1, arr2, (Comparator<String>) null) < 0);
    }

    @Test
    public void testCompareObjectArraysWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        String[] arr1 = { "a", "b", "c", "d" };
        String[] arr2 = { "x", "b", "c", "y" };

        Assertions.assertEquals(0, N.compare(arr1, 1, arr2, 1, 2, reverseComparator));

        Assertions.assertTrue(N.compare(arr1, 0, arr2, 0, 1, reverseComparator) > 0);
    }

    @Test
    public void testCompareCollectionsWithRange() {
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2));

        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1) < 0);

        Assertions.assertEquals(0, N.compare(list1, 1, list1, 1, 2));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.compare(list1, 0, list2, 0, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.compare(list1, 0, list2, 0, 10));
    }

    @Test
    public void testCompareIterables() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");
        Set<String> set1 = new LinkedHashSet<>(Arrays.asList("a", "b", "c"));

        Assertions.assertEquals(0, N.compare(list1, list2));
        Assertions.assertEquals(0, N.compare(list1, set1));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1, list3) < 0);

        Assertions.assertEquals(0, N.compare(Collections.<String> emptyList(), Collections.<String> emptyList()));
        Assertions.assertEquals(-1, N.compare(Collections.emptyList(), list1));
        Assertions.assertEquals(1, N.compare(list1, Collections.emptyList()));
    }

    @Test
    public void testCompareIterators() {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "c");

        Assertions.assertEquals(0, N.compare(list1.iterator(), list2.iterator()));

        List<String> list3 = Arrays.asList("a", "b", "d");
        Assertions.assertTrue(N.compare(list1.iterator(), list3.iterator()) < 0);

        List<String> list4 = Arrays.asList("a", "b");
        Assertions.assertTrue(N.compare(list1.iterator(), list4.iterator()) > 0);

        Assertions.assertEquals(0, N.compare((Iterator<String>) null, (Iterator<String>) null));
        Assertions.assertEquals(-1, N.compare((Iterator<String>) null, list1.iterator()));
        Assertions.assertEquals(1, N.compare(list1.iterator(), (Iterator<String>) null));
    }

    @Test
    public void testCompareCollectionsWithRangeAndComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c", "d");
        List<String> list2 = Arrays.asList("x", "b", "c", "y");

        Assertions.assertEquals(0, N.compare(list1, 1, list2, 1, 2, reverseComparator));

        Assertions.assertTrue(N.compare(list1, 0, list2, 0, 1, reverseComparator) > 0);

        Set<String> set1 = new LinkedHashSet<>(list1);
        Set<String> set2 = new LinkedHashSet<>(list2);
        Assertions.assertEquals(0, N.compare(set1, 1, set2, 1, 2, reverseComparator));
    }

    @Test
    public void testCompareIterablesWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(N.compare(list1, list2, (Comparator<String>) null) < 0);

        Assertions.assertTrue(N.compare(list1, list2, reverseComparator) > 0);

        Assertions.assertEquals(0, N.compare(Collections.emptyList(), Collections.emptyList(), reverseComparator));
    }

    @Test
    public void testCompareIteratorsWithComparator() {
        Comparator<String> reverseComparator = (a, b) -> b.compareTo(a);
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<String> list2 = Arrays.asList("a", "b", "d");

        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), (Comparator<String>) null) < 0);

        Assertions.assertTrue(N.compare(list1.iterator(), list2.iterator(), reverseComparator) > 0);
    }

    @Test
    public void testCompareIgnoreCase() {
        Assertions.assertEquals(0, N.compareIgnoreCase("Hello", "hello"));
        Assertions.assertEquals(0, N.compareIgnoreCase("HELLO", "hello"));
        Assertions.assertTrue(N.compareIgnoreCase("a", "B") < 0);
        Assertions.assertTrue(N.compareIgnoreCase("B", "a") > 0);

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

        Assertions.assertEquals(0, N.compareIgnoreCase(new String[] {}, new String[] {}));
        Assertions.assertEquals(-1, N.compareIgnoreCase(new String[] {}, arr1));
    }

    @Test
    public void testCompareByProps() {
    }

    @Test
    public void testLessThan() {
        Assertions.assertTrue(N.lessThan(1, 2));
        Assertions.assertFalse(N.lessThan(2, 1));
        Assertions.assertFalse(N.lessThan(1, 1));

        Assertions.assertTrue(N.lessThan("a", "b"));
        Assertions.assertFalse(N.lessThan("b", "a"));

        Assertions.assertTrue(N.lessThan(null, "a"));
        Assertions.assertFalse(N.lessThan("a", null));
        Assertions.assertFalse(N.lessThan((String) null, (String) null));
    }

    @Test
    public void testLessThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.lessThan(1, 2, null));

        Assertions.assertFalse(N.lessThan(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessThan(2, 1, reverseComparator));
    }

    @Test
    public void testLessEqual() {
        Assertions.assertTrue(N.lessEqual(1, 2));
        Assertions.assertFalse(N.lessEqual(2, 1));
        Assertions.assertTrue(N.lessEqual(1, 1));

        Assertions.assertTrue(N.lessEqual(null, "a"));
        Assertions.assertTrue(N.lessEqual((String) null, (String) null));
    }

    @Test
    public void testLessEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.lessEqual(1, 2, null));
        Assertions.assertTrue(N.lessEqual(1, 1, null));

        Assertions.assertFalse(N.lessEqual(1, 2, reverseComparator));
        Assertions.assertTrue(N.lessEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterThan() {
        Assertions.assertFalse(N.greaterThan(1, 2));
        Assertions.assertTrue(N.greaterThan(2, 1));
        Assertions.assertFalse(N.greaterThan(1, 1));

        Assertions.assertFalse(N.greaterThan("a", "b"));
        Assertions.assertTrue(N.greaterThan("b", "a"));

        Assertions.assertFalse(N.greaterThan(null, "a"));
        Assertions.assertTrue(N.greaterThan("a", null));
    }

    @Test
    public void testGreaterThanWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(N.greaterThan(1, 2, null));

        Assertions.assertTrue(N.greaterThan(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterThan(2, 1, reverseComparator));
    }

    @Test
    public void testGreaterEqual() {
        Assertions.assertFalse(N.greaterEqual(1, 2));
        Assertions.assertTrue(N.greaterEqual(2, 1));
        Assertions.assertTrue(N.greaterEqual(1, 1));

        Assertions.assertFalse(N.greaterEqual(null, "a"));
        Assertions.assertTrue(N.greaterEqual((String) null, (String) null));
    }

    @Test
    public void testGreaterEqualWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertFalse(N.greaterEqual(1, 2, null));
        Assertions.assertTrue(N.greaterEqual(1, 1, null));

        Assertions.assertTrue(N.greaterEqual(1, 2, reverseComparator));
        Assertions.assertFalse(N.greaterEqual(2, 1, reverseComparator));
    }

    @Test
    public void testGtAndLt() {
        Assertions.assertTrue(N.gtAndLt(5, 1, 10));
        Assertions.assertFalse(N.gtAndLt(1, 1, 10));
        Assertions.assertFalse(N.gtAndLt(10, 1, 10));
        Assertions.assertFalse(N.gtAndLt(0, 1, 10));
        Assertions.assertFalse(N.gtAndLt(11, 1, 10));

        Assertions.assertFalse(N.gtAndLt(5, 5, 5));
    }

    @Test
    public void testGtAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.gtAndLt(5, 1, 10, null));

        Assertions.assertTrue(N.gtAndLt(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLt(5, 1, 10, reverseComparator));
    }

    @Test
    public void testGeAndLt() {
        Assertions.assertTrue(N.geAndLt(5, 1, 10));
        Assertions.assertTrue(N.geAndLt(1, 1, 10));
        Assertions.assertFalse(N.geAndLt(10, 1, 10));
        Assertions.assertFalse(N.geAndLt(0, 1, 10));
    }

    @Test
    public void testGeAndLtWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.geAndLt(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLt(1, 1, 10, null));

        Assertions.assertTrue(N.geAndLt(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLt(10, 10, 1, reverseComparator));
    }

    @Test
    public void testGeAndLe() {
        Assertions.assertTrue(N.geAndLe(5, 1, 10));
        Assertions.assertTrue(N.geAndLe(1, 1, 10));
        Assertions.assertTrue(N.geAndLe(10, 1, 10));
        Assertions.assertFalse(N.geAndLe(0, 1, 10));
        Assertions.assertFalse(N.geAndLe(11, 1, 10));
    }

    @Test
    public void testGeAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.geAndLe(5, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.geAndLe(10, 1, 10, null));

        Assertions.assertTrue(N.geAndLe(5, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.geAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testGtAndLe() {
        Assertions.assertTrue(N.gtAndLe(5, 1, 10));
        Assertions.assertFalse(N.gtAndLe(1, 1, 10));
        Assertions.assertTrue(N.gtAndLe(10, 1, 10));
        Assertions.assertFalse(N.gtAndLe(0, 1, 10));
        Assertions.assertFalse(N.gtAndLe(11, 1, 10));
    }

    @Test
    public void testGtAndLeWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.gtAndLe(5, 1, 10, null));
        Assertions.assertFalse(N.gtAndLe(1, 1, 10, null));
        Assertions.assertTrue(N.gtAndLe(10, 1, 10, null));

        Assertions.assertTrue(N.gtAndLe(5, 10, 1, reverseComparator));
        Assertions.assertFalse(N.gtAndLe(10, 10, 1, reverseComparator));
        Assertions.assertTrue(N.gtAndLe(1, 10, 1, reverseComparator));
    }

    @Test
    public void testIsBetween() {
        Assertions.assertTrue(N.isBetween(5, 1, 10));
        Assertions.assertTrue(N.isBetween(1, 1, 10));
        Assertions.assertTrue(N.isBetween(10, 1, 10));
        Assertions.assertFalse(N.isBetween(0, 1, 10));
        Assertions.assertFalse(N.isBetween(11, 1, 10));
    }

    @Test
    public void testIsBetweenWithComparator() {
        Comparator<Integer> reverseComparator = (a, b) -> b.compareTo(a);

        Assertions.assertTrue(N.isBetween(5, 1, 10, null));
        Assertions.assertTrue(N.isBetween(5, 10, 1, reverseComparator));
    }

    @Test
    public void testGetElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", N.getElement(list, 0));
        Assertions.assertEquals("b", N.getElement(list, 1));
        Assertions.assertEquals("d", N.getElement(list, 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list, -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list, 4));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.getElement(set, 0));
        Assertions.assertEquals("b", N.getElement(set, 1));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterable<String>) null, 0));
    }

    @Test
    public void testGetElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d");

        Assertions.assertEquals("a", N.getElement(list.iterator(), 0));
        Assertions.assertEquals("b", N.getElement(list.iterator(), 1));
        Assertions.assertEquals("d", N.getElement(list.iterator(), 3));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement(list.iterator(), -1));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> N.getElement(list.iterator(), 4));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.getElement((Iterator<String>) null, 0));
    }

    @Test
    public void testGetOnlyElementFromIterable() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList).get());

        Assertions.assertFalse(N.getOnlyElement(Collections.emptyList()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList));

        Assertions.assertFalse(N.getOnlyElement((Iterable<String>) null).isPresent());

        Set<String> singleSet = Collections.singleton("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleSet).get());
    }

    @Test
    public void testGetOnlyElementFromIterator() {
        List<String> singleList = Arrays.asList("only");
        Assertions.assertEquals("only", N.getOnlyElement(singleList.iterator()).get());

        Assertions.assertFalse(N.getOnlyElement(Collections.emptyIterator()).isPresent());

        List<String> multiList = Arrays.asList("a", "b");
        Assertions.assertThrows(TooManyElementsException.class, () -> N.getOnlyElement(multiList.iterator()));

        Assertions.assertFalse(N.getOnlyElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list).get());

        Assertions.assertFalse(N.firstElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.firstElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("a", N.firstElement(set).get());
    }

    @Test
    public void testFirstElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstElement(list.iterator()).get());

        Assertions.assertFalse(N.firstElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.firstElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testLastElementFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list).get());

        Assertions.assertFalse(N.lastElement(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.lastElement((Iterable<String>) null).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastElement(arrayList).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("c", N.lastElement(set).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastElement(deque).get());
    }

    @Test
    public void testLastElementFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastElement(list.iterator()).get());

        Assertions.assertFalse(N.lastElement(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.lastElement((Iterator<String>) null).isPresent());
    }

    @Test
    public void testFirstElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(set, 3));
    }

    @Test
    public void testFirstElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), N.firstElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.firstElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.firstElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.firstElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.firstElements(list.iterator(), -1));
    }

    @Test
    public void testLastElementsFromIterable() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list, 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list, 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list, 0));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyList(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterable<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list, -1));

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(set, 3));
    }

    @Test
    public void testLastElementsFromIterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");

        Assertions.assertEquals(Arrays.asList("c", "d", "e"), N.lastElements(list.iterator(), 3));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 5));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d", "e"), N.lastElements(list.iterator(), 10));
        Assertions.assertEquals(Collections.emptyList(), N.lastElements(list.iterator(), 0));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements(Collections.emptyIterator(), 5));

        Assertions.assertEquals(Collections.emptyList(), N.lastElements((Iterator<String>) null, 5));

        Assertions.assertThrows(IllegalArgumentException.class, () -> N.lastElements(list.iterator(), -1));
    }

    @Test
    public void testFirstNonNullTwo() {
        Assertions.assertEquals("a", N.firstNonNull("a", "b").get());

        Assertions.assertEquals("b", N.firstNonNull(null, "b").get());

        Assertions.assertEquals("a", N.firstNonNull("a", null).get());

        Assertions.assertFalse(N.firstNonNull(null, null).isPresent());
    }

    @Test
    public void testFirstNonNullThree() {
        Assertions.assertEquals("a", N.firstNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonNull(null, "b", "c").get());

        Assertions.assertEquals("c", N.firstNonNull(null, null, "c").get());

        Assertions.assertFalse(N.firstNonNull(null, null, null).isPresent());
    }

    @Test
    public void testFirstNonNullVarargs() {
        Assertions.assertEquals("c", N.firstNonNull(null, null, "c", "d").get());

        Assertions.assertFalse(N.firstNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonNull((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testFirstNonNullIterable() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list).get());

        Assertions.assertFalse(N.firstNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls).isPresent());
    }

    @Test
    public void testFirstNonNullIterator() {
        List<String> list = Arrays.asList(null, null, "c", "d");
        Assertions.assertEquals("c", N.firstNonNull(list.iterator()).get());

        Assertions.assertFalse(N.firstNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.firstNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.firstNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testLastNonNullTwo() {
        Assertions.assertEquals("b", N.lastNonNull("a", "b").get());

        Assertions.assertEquals("b", N.lastNonNull(null, "b").get());

        Assertions.assertEquals("a", N.lastNonNull("a", null).get());

        Assertions.assertFalse(N.lastNonNull(null, null).isPresent());
    }

    @Test
    public void testLastNonNullThree() {
        Assertions.assertEquals("c", N.lastNonNull("a", "b", "c").get());

        Assertions.assertEquals("b", N.lastNonNull("a", "b", null).get());

        Assertions.assertEquals("a", N.lastNonNull("a", null, null).get());

        Assertions.assertFalse(N.lastNonNull(null, null, null).isPresent());
    }

    @Test
    public void testLastNonNullVarargs() {
        Assertions.assertEquals("d", N.lastNonNull("a", "b", null, "d", null).get());

        Assertions.assertFalse(N.lastNonNull(new String[] {}).isPresent());

        Assertions.assertFalse(N.lastNonNull((String[]) null).isPresent());

        Assertions.assertFalse(N.lastNonNull(new String[] { null, null, null }).isPresent());
    }

    @Test
    public void testLastNonNullIterable() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list).get());

        Assertions.assertFalse(N.lastNonNull(Collections.emptyList()).isPresent());

        Assertions.assertFalse(N.lastNonNull((Iterable<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("d", N.lastNonNull(arrayList).get());

    }

    @Test
    public void testLastNonNullIterator() {
        List<String> list = Arrays.asList("a", "b", null, "d", null);
        Assertions.assertEquals("d", N.lastNonNull(list.iterator()).get());

        Assertions.assertFalse(N.lastNonNull(Collections.emptyIterator()).isPresent());

        Assertions.assertFalse(N.lastNonNull((Iterator<String>) null).isPresent());

        List<String> allNulls = Arrays.asList(null, null, null);
        Assertions.assertFalse(N.lastNonNull(allNulls.iterator()).isPresent());
    }

    @Test
    public void testFirstNonEmptyArraysTwo() {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] empty = {};

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2).get());

        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2).get());

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

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

        Assertions.assertArrayEquals(arr1, N.firstNonEmpty(arr1, arr2, arr3).get());

        Assertions.assertArrayEquals(arr2, N.firstNonEmpty(empty, arr2, arr3).get());

        Assertions.assertArrayEquals(arr3, N.firstNonEmpty(empty, empty, arr3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCollectionsTwo() {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        List<String> empty = Collections.emptyList();

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2).get());

        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2).get());

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

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

        Assertions.assertEquals(list1, N.firstNonEmpty(list1, list2, list3).get());

        Assertions.assertEquals(list2, N.firstNonEmpty(empty, list2, list3).get());

        Assertions.assertEquals(list3, N.firstNonEmpty(empty, empty, list3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyMapsTwo() {
        Map<String, String> map1 = new HashMap<>();
        map1.put("a", "1");
        Map<String, String> map2 = new HashMap<>();
        map2.put("b", "2");
        Map<String, String> empty = Collections.emptyMap();

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2).get());

        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2).get());

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, empty).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty).isPresent());

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

        Assertions.assertEquals(map1, N.firstNonEmpty(map1, map2, map3).get());

        Assertions.assertEquals(map2, N.firstNonEmpty(empty, map2, map3).get());

        Assertions.assertEquals(map3, N.firstNonEmpty(empty, empty, map3).get());

        Assertions.assertFalse(N.firstNonEmpty(empty, empty, empty).isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesTwo() {
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "world").get());

        Assertions.assertEquals("world", N.firstNonEmpty("", "world").get());

        Assertions.assertEquals("hello", N.firstNonEmpty("hello", "").get());

        Assertions.assertFalse(N.firstNonEmpty("", "").isPresent());

        Assertions.assertEquals("hello", N.firstNonEmpty(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonEmpty("hello", null).get());
        Assertions.assertFalse(N.firstNonEmpty((String) null, (String) null).isPresent());

        StringBuilder sb = new StringBuilder("builder");
        StringBuffer buf = new StringBuffer("buffer");
        Assertions.assertEquals(sb, N.firstNonEmpty(sb, buf).get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesThree() {
        Assertions.assertEquals("a", N.firstNonEmpty("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonEmpty("", "b", "c").get());

        Assertions.assertEquals("c", N.firstNonEmpty("", "", "c").get());

        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());

        Assertions.assertEquals("c", N.firstNonEmpty(null, "", "c").get());
    }

    @Test
    public void testFirstNonEmptyCharSequencesVarargs() {
        Assertions.assertEquals("c", N.firstNonEmpty("", null, "c", "d").get());

        Assertions.assertFalse(N.firstNonEmpty(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonEmpty((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonEmpty("", "", "").isPresent());
    }

    @Test
    public void testFirstNonEmptyCharSequencesIterable() {
        List<String> list = Arrays.asList("", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonEmpty(list).get());

        Assertions.assertFalse(N.firstNonEmpty(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonEmpty((Iterable<String>) null).isPresent());

        List<String> allEmpty = Arrays.asList("", "", null);
        Assertions.assertFalse(N.firstNonEmpty(allEmpty).isPresent());
    }

    @Test
    public void testFirstNonBlankTwo() {
        Assertions.assertEquals("hello", N.firstNonBlank("hello", "world").get());

        Assertions.assertEquals("world", N.firstNonBlank("  ", "world").get());

        Assertions.assertEquals("hello", N.firstNonBlank("hello", "  ").get());

        Assertions.assertFalse(N.firstNonBlank("  ", "  ").isPresent());

        Assertions.assertEquals("hello", N.firstNonBlank(null, "hello").get());
        Assertions.assertEquals("hello", N.firstNonBlank("hello", null).get());
        Assertions.assertFalse(N.firstNonBlank((String) null, (String) null).isPresent());
    }

    @Test
    public void testFirstNonBlankThree() {
        Assertions.assertEquals("a", N.firstNonBlank("a", "b", "c").get());

        Assertions.assertEquals("b", N.firstNonBlank("  ", "b", "c").get());

        Assertions.assertEquals("c", N.firstNonBlank("  ", "  ", "c").get());

        Assertions.assertFalse(N.firstNonBlank("  ", "  ", "  ").isPresent());

        Assertions.assertEquals("c", N.firstNonBlank(null, "", "c").get());
        Assertions.assertEquals("c", N.firstNonBlank("  ", "\t", "c").get());
    }

    @Test
    public void testFirstNonBlankVarargs() {
        Assertions.assertEquals("c", N.firstNonBlank("  ", null, "c", "d").get());

        Assertions.assertFalse(N.firstNonBlank(new String[] {}).isPresent());

        Assertions.assertFalse(N.firstNonBlank((String[]) null).isPresent());

        Assertions.assertFalse(N.firstNonBlank("  ", "\t", "\n").isPresent());
    }

    @Test
    public void testFirstNonBlankIterable() {
        List<String> list = Arrays.asList("  ", null, "c", "d");
        Assertions.assertEquals("c", N.firstNonBlank(list).get());

        Assertions.assertFalse(N.firstNonBlank(Collections.<String> emptyList()).isPresent());

        Assertions.assertFalse(N.firstNonBlank((Iterable<String>) null).isPresent());

        List<String> allBlank = Arrays.asList("  ", "\t", null);
        Assertions.assertFalse(N.firstNonBlank(allBlank).isPresent());
    }

    @Test
    public void testFirstEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> first = N.firstEntry(map).get();
        Assertions.assertEquals("a", first.getKey());
        Assertions.assertEquals(1, first.getValue());

        Assertions.assertFalse(N.firstEntry(Collections.emptyMap()).isPresent());

        Assertions.assertFalse(N.firstEntry(null).isPresent());
    }

    @Test
    public void testLastEntry() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Map.Entry<String, Integer> last = N.lastEntry(map).get();
        Assertions.assertEquals("c", last.getKey());
        Assertions.assertEquals(3, last.getValue());

        Assertions.assertFalse(N.lastEntry(Collections.emptyMap()).isPresent());

        Assertions.assertFalse(N.lastEntry(null).isPresent());
    }

    @Test
    public void testFirstOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(arr));

        Assertions.assertNull(N.firstOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(N.firstOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list));

        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(N.firstOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testFirstOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(N.firstOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(N.firstOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testFirstOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(arrayList, "default"));
    }

    @Test
    public void testFirstOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("a", N.firstOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", N.firstOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testLastOrNullIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(arr));

        Assertions.assertNull(N.lastOrNullIfEmpty(new String[] {}));

        Assertions.assertNull(N.lastOrNullIfEmpty((String[]) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list));

        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyList()));

        Assertions.assertNull(N.lastOrNullIfEmpty((Iterable<String>) null));
    }

    @Test
    public void testLastOrNullIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrNullIfEmpty(list.iterator()));

        Assertions.assertNull(N.lastOrNullIfEmpty(Collections.emptyIterator()));

        Assertions.assertNull(N.lastOrNullIfEmpty((Iterator<String>) null));
    }

    @Test
    public void testLastOrDefaultIfEmptyArray() {
        String[] arr = { "a", "b", "c" };
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arr, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(new String[] {}, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((String[]) null, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterable() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list, "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyList(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterable<String>) null, "default"));

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(arrayList, "default"));

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(deque, "default"));
    }

    @Test
    public void testLastOrDefaultIfEmptyIterator() {
        List<String> list = Arrays.asList("a", "b", "c");
        Assertions.assertEquals("c", N.lastOrDefaultIfEmpty(list.iterator(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty(Collections.emptyIterator(), "default"));

        Assertions.assertEquals("default", N.lastOrDefaultIfEmpty((Iterator<String>) null, "default"));
    }

    @Test
    public void testFindFirstArray() {
        String[] arr = { "apple", "banana", "cherry", "date" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(arr, startsWithC).get());

        Assertions.assertFalse(N.findFirst(arr, startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(list, startsWithC).get());

        Assertions.assertFalse(N.findFirst(list, startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((Iterable<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindFirstIterator() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cherry", N.findFirst(list.iterator(), startsWithC).get());

        Assertions.assertFalse(N.findFirst(list.iterator(), startsWithX).isPresent());

        Assertions.assertFalse(N.findFirst(Collections.emptyIterator(), startsWithC).isPresent());

        Assertions.assertFalse(N.findFirst((Iterator<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastArray() {
        String[] arr = { "apple", "banana", "cherry", "date", "cucumber" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", N.findLast(arr, startsWithC).get());

        Assertions.assertFalse(N.findLast(arr, startsWithX).isPresent());

        Assertions.assertFalse(N.findLast(new String[] {}, startsWithC).isPresent());

        Assertions.assertFalse(N.findLast((String[]) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLastIterable() {
        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "cucumber");
        Predicate<String> startsWithC = s -> s.startsWith("c");
        Predicate<String> startsWithX = s -> s.startsWith("x");

        Assertions.assertEquals("cucumber", N.findLast(list, startsWithC).get());

        Assertions.assertFalse(N.findLast(list, startsWithX).isPresent());

        Assertions.assertFalse(N.findLast(Collections.emptyList(), startsWithC).isPresent());

        Assertions.assertFalse(N.findLast((Iterable<String>) null, startsWithC).isPresent());

        ArrayList<String> arrayList = new ArrayList<>(list);
        Assertions.assertEquals("cucumber", N.findLast(arrayList, startsWithC).get());

        Set<String> set = new LinkedHashSet<>(list);
        Assertions.assertEquals("cucumber", N.findLast(set, startsWithC).get());

        Deque<String> deque = new ArrayDeque<>(list);
        Assertions.assertEquals("cucumber", N.findLast(deque, startsWithC).get());
    }

    @Test
    public void testConvertNullToClass() {
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
        Integer intVal = 123;
        assertEquals(123L, N.convert(intVal, Long.class).longValue());
        assertEquals(123.0f, N.convert(intVal, Float.class).floatValue(), 0.000001f);
        assertEquals(123.0d, N.convert(intVal, Double.class).doubleValue(), 0.000001d);
        assertEquals((byte) 123, N.convert(intVal, Byte.class).byteValue());
        assertEquals((short) 123, N.convert(intVal, Short.class).shortValue());

        Long longVal = 456L;
        assertEquals(456, N.convert(longVal, Integer.class).intValue());
        assertEquals(456.0f, N.convert(longVal, Float.class).floatValue(), 0.000001f);
        assertEquals(456.0d, N.convert(longVal, Double.class).doubleValue(), 0.000001d);

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
        List<String> list = Arrays.asList("a", "b", "c", "b");
        Set<String> set = N.convert(list, Set.class);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains("c"));

        Set<Integer> intSet = new HashSet<>(Arrays.asList(1, 2, 3));
        List<Integer> intList = N.convert(intSet, List.class);
        assertEquals(3, intList.size());
        assertTrue(intList.containsAll(intSet));

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

        Map<String, String> emptyMap = new HashMap<>();
        Map<String, String> convertedEmpty = N.convert(emptyMap, Map.class);
        assertTrue(convertedEmpty.isEmpty());
    }

    @Test
    public void testConvertByteArrayFromBlob() throws SQLException {
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

    @Test
    public void testConvertNullToType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<List> listType = TypeFactory.getType(List.class);

        assertNull(N.convert(null, stringType));
        assertNull(N.convert(null, intType));
        assertNull(N.convert(null, listType));
    }

    @Test
    public void testConvertUsingType() {
        Type<String> stringType = TypeFactory.getType(String.class);
        Type<Integer> intType = TypeFactory.getType(Integer.class);
        Type<Boolean> boolType = TypeFactory.getType(Boolean.class);

        assertEquals(123, N.convert("123", intType).intValue());

        assertEquals("456", N.convert(456, stringType));

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
        Type<List> listType = TypeFactory.getType(List.class);
        Type<Map> mapType = TypeFactory.getType(Map.class);

        String[] array = { "x", "y", "z" };
        List<String> list = N.convert(array, listType);
        assertEquals(3, list.size());

        Map<String, Integer> srcMap = new HashMap<>();
        srcMap.put("one", 1);
        srcMap.put("two", 2);

        Map<String, Integer> destMap = N.convert(srcMap, mapType);
        assertEquals(2, destMap.size());
    }

    @Test
    public void testConvertAutoCloseableResources() {
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

        String bigNum = "9223372036854775807";
        assertEquals(Long.MAX_VALUE, N.convert(bigNum, Long.class).longValue());

        assertThrows(ArithmeticException.class, () -> N.convert(Long.MAX_VALUE, Integer.class).intValue());
        assertThrows(ArithmeticException.class, () -> N.convert(1000, Byte.class).byteValue());

        assertEquals((Float) Float.POSITIVE_INFINITY, N.convert("Infinity", Float.class));
        assertEquals((Double) Double.NEGATIVE_INFINITY, N.convert("-Infinity", Double.class));
        assertTrue(Double.isNaN(N.convert("NaN", Double.class).doubleValue()));
    }

    @Test
    public void testConvertUnsupportedConversions() {

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");

        assertThrows(RuntimeException.class, () -> N.convert(map, Integer.class));
        assertFalse(N.convert(map, Boolean.class));

        List<Map<String, Object>> complexList = new ArrayList<>();
        complexList.add(new HashMap<>());

        List<Map<String, Object>> convertedList = N.convert(complexList, List.class);
        assertNotNull(convertedList);
        assertEquals(1, convertedList.size());
    }
}
