package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class Joiner2025Test extends TestBase {

    public static class Person {
        private String name;
        private int age;
        private String city;
        private Double salary;

        public Person(String name, int age, String city, Double salary) {
            this.name = name;
            this.age = age;
            this.city = city;
            this.salary = salary;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public Double getSalary() {
            return salary;
        }

        public void setSalary(Double salary) {
            this.salary = salary;
        }
    }

    @Test
    public void testDefauLt() {
        Joiner joiner = Joiner.defauLt();
        assertNotNull(joiner);
        joiner.append("a").append("b").append("c");
        assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testWithSeparator() {
        Joiner joiner = Joiner.with("|");
        assertNotNull(joiner);
        joiner.append("x").append("y").append("z");
        assertEquals("x|y|z", joiner.toString());
    }

    @Test
    public void testWithSeparatorAndKeyValueDelimiter() {
        Joiner joiner = Joiner.with(";", "->");
        assertNotNull(joiner);
        joiner.appendEntry("key1", "val1").appendEntry("key2", "val2");
        assertEquals("key1->val1;key2->val2", joiner.toString());
    }

    @Test
    public void testWithSeparatorPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "{", "}");
        assertNotNull(joiner);
        joiner.append("a").append("b").append("c");
        assertEquals("{a,b,c}", joiner.toString());
    }

    @Test
    public void testWithAllParameters() {
        Joiner joiner = Joiner.with(";", ":", "<", ">");
        assertNotNull(joiner);
        joiner.appendEntry("name", "Alice").appendEntry("age", "30");
        assertEquals("<name:Alice;age:30>", joiner.toString());
    }

    @Test
    public void testWithEmptyParameters() {
        Joiner joiner = Joiner.with("", "", "", "");
        joiner.append("a").append("b");
        assertEquals("ab", joiner.toString());
    }

    @Test
    public void testSetEmptyValue() {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        assertEquals("EMPTY", joiner.toString());
    }

    @Test
    public void testSetEmptyValueWithContent() {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        joiner.append("test");
        assertEquals("test", joiner.toString());
    }

    @Test
    public void testSetEmptyValueNull() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").setEmptyValue(null));
    }

    @Test
    public void testTrimBeforeAppend() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.append("  a  ").append("  b  ").append("  c  ");
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testTrimBeforeAppendWithCharSequence() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        CharSequence cs = new StringBuilder("  text  ");
        joiner.append(cs);
        assertEquals("text", joiner.toString());
    }

    @Test
    public void testSkipNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        joiner.append("a").append((String) null).append("b").append((Object) null).append("c");
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testUseForNull() {
        Joiner joiner = Joiner.with(",").useForNull("NULL");
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,NULL,b", joiner.toString());
    }

    @Test
    public void testUseForNullEmpty() {
        Joiner joiner = Joiner.with(",").useForNull("");
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,,b", joiner.toString());
    }

    @Test
    public void testUseForNullWithNull() {
        Joiner joiner = Joiner.with(",").useForNull(null);
        joiner.append((String) null);
        assertEquals("null", joiner.toString());
    }

    @Test
    public void testReuseCachedBuffer() {
        Joiner joiner = Joiner.with(",").reuseCachedBuffer();
        joiner.append("a").append("b");
        String result = joiner.toString();
        assertEquals("a,b", result);
    }

    @Test
    public void testReuseCachedBufferAfterBufferCreated() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IllegalStateException.class, () -> joiner.reuseCachedBuffer());
    }

    @Test
    public void testAppendBoolean() {
        Joiner joiner = Joiner.with(",");
        joiner.append(true).append(false).append(true);
        assertEquals("true,false,true", joiner.toString());
    }

    @Test
    public void testAppendChar() {
        Joiner joiner = Joiner.with(",");
        joiner.append('a').append('b').append('c');
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendCharSpecial() {
        Joiner joiner = Joiner.with(",");
        joiner.append('\n').append('\t').append('\r');
        assertEquals("\n,\t,\r", joiner.toString());
    }

    @Test
    public void testAppendInt() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1).append(2).append(3);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendIntNegative() {
        Joiner joiner = Joiner.with(",");
        joiner.append(-1).append(0).append(100);
        assertEquals("-1,0,100", joiner.toString());
    }

    @Test
    public void testAppendLong() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1L).append(2L).append(3L);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendLongMaxMin() {
        Joiner joiner = Joiner.with(",");
        joiner.append(Long.MAX_VALUE).append(Long.MIN_VALUE);
        assertEquals(Long.MAX_VALUE + "," + Long.MIN_VALUE, joiner.toString());
    }

    @Test
    public void testAppendFloat() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1.5f).append(2.5f).append(3.5f);
        assertEquals("1.5,2.5,3.5", joiner.toString());
    }

    @Test
    public void testAppendFloatSpecial() {
        Joiner joiner = Joiner.with(",");
        joiner.append(Float.NaN).append(Float.POSITIVE_INFINITY).append(Float.NEGATIVE_INFINITY);
        assertEquals("NaN,Infinity,-Infinity", joiner.toString());
    }

    @Test
    public void testAppendDouble() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1.5).append(2.5).append(3.5);
        assertEquals("1.5,2.5,3.5", joiner.toString());
    }

    @Test
    public void testAppendDoubleSpecial() {
        Joiner joiner = Joiner.with(",");
        joiner.append(Double.NaN).append(Double.POSITIVE_INFINITY);
        assertEquals("NaN,Infinity", joiner.toString());
    }

    @Test
    public void testAppendString() {
        Joiner joiner = Joiner.with(",");
        joiner.append("hello").append("world").append("test");
        assertEquals("hello,world,test", joiner.toString());
    }

    @Test
    public void testAppendStringNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,null,b", joiner.toString());
    }

    @Test
    public void testAppendStringEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("").append("b");
        assertEquals("a,,b", joiner.toString());
    }

    @Test
    public void testAppendCharSequence() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs1 = new StringBuilder("hello");
        CharSequence cs2 = new StringBuffer("world");
        joiner.append(cs1).append(cs2);
        assertEquals("hello,world", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append((CharSequence) null).append("b");
        assertEquals("a,null,b", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceWithStartEnd() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs = "hello world";
        joiner.append(cs, 0, 5).append(cs, 6, 11);
        assertEquals("hello,world", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceWithStartEndBoundary() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs = "test";
        joiner.append(cs, 0, 4).append(cs, 1, 3);
        assertEquals("test,es", joiner.toString());
    }

    @Test
    public void testAppendStringBuilder() {
        Joiner joiner = Joiner.with(",");
        StringBuilder sb1 = new StringBuilder("hello");
        StringBuilder sb2 = new StringBuilder("world");
        joiner.append(sb1).append(sb2);
        assertEquals("hello,world", joiner.toString());
    }

    @Test
    public void testAppendStringBuilderNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append((StringBuilder) null).append("b");
        assertEquals("a,null,b", joiner.toString());
    }

    @Test
    public void testAppendObject() {
        Joiner joiner = Joiner.with(",");
        joiner.append(new Integer(1)).append(new Double(2.5)).append("text");
        assertEquals("1,2.5,text", joiner.toString());
    }

    @Test
    public void testAppendObjectNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append((Object) null).append("b");
        assertEquals("a,null,b", joiner.toString());
    }

    @Test
    public void testAppendIfNotNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("b").appendIfNotNull(null);
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testAppendIfNotNullAllNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIfNotNull(null).appendIfNotNull(null);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendIf() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIf(true, () -> "yes").appendIf(false, () -> "no").appendIf(true, () -> "maybe");
        assertEquals("yes,maybe", joiner.toString());
    }

    @Test
    public void testAppendIfWithNullSupplier() {
        Joiner joiner = Joiner.with(",");
        Supplier<?> nullSupplier = () -> null;
        joiner.append("before").appendIf(true, nullSupplier).append("after");
        assertEquals("before,null,after", joiner.toString());
    }

    @Test
    public void testAppendIfSupplierNotCalled() {
        Joiner joiner = Joiner.with(",");
        Supplier<?> exceptionSupplier = () -> {
            throw new RuntimeException("Should not be called");
        };
        joiner.appendIf(false, exceptionSupplier);
        joiner.append("safe");
        assertEquals("safe", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArray() {
        Joiner joiner = Joiner.with(",");
        boolean[] arr = { true, false, true };
        joiner.appendAll(arr);
        assertEquals("true,false,true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        boolean[] arr = { true, false, true, false };
        joiner.appendAll(arr, 1, 3);
        assertEquals("false,true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new boolean[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((boolean[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCharArray() {
        Joiner joiner = Joiner.with(",");
        char[] arr = { 'a', 'b', 'c' };
        joiner.appendAll(arr);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCharArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        char[] arr = { 'a', 'b', 'c', 'd' };
        joiner.appendAll(arr, 1, 3);
        assertEquals("b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCharArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new char[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCharArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((char[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllByteArray() {
        Joiner joiner = Joiner.with(",");
        byte[] arr = { 1, 2, 3 };
        joiner.appendAll(arr);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendAllByteArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        byte[] arr = { 1, 2, 3, 4 };
        joiner.appendAll(arr, 1, 3);
        assertEquals("2,3", joiner.toString());
    }

    @Test
    public void testAppendAllByteArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new byte[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllByteArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((byte[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllShortArray() {
        Joiner joiner = Joiner.with(",");
        short[] arr = { 10, 20, 30 };
        joiner.appendAll(arr);
        assertEquals("10,20,30", joiner.toString());
    }

    @Test
    public void testAppendAllShortArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        short[] arr = { 10, 20, 30, 40 };
        joiner.appendAll(arr, 1, 3);
        assertEquals("20,30", joiner.toString());
    }

    @Test
    public void testAppendAllShortArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new short[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllShortArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((short[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIntArray() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3 };
        joiner.appendAll(arr);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendAllIntArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3, 4 };
        joiner.appendAll(arr, 1, 3);
        assertEquals("2,3", joiner.toString());
    }

    @Test
    public void testAppendAllIntArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new int[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIntArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((int[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllLongArray() {
        Joiner joiner = Joiner.with(",");
        long[] arr = { 100L, 200L, 300L };
        joiner.appendAll(arr);
        assertEquals("100,200,300", joiner.toString());
    }

    @Test
    public void testAppendAllLongArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        long[] arr = { 100L, 200L, 300L, 400L };
        joiner.appendAll(arr, 1, 3);
        assertEquals("200,300", joiner.toString());
    }

    @Test
    public void testAppendAllLongArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new long[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllLongArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((long[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArray() {
        Joiner joiner = Joiner.with(",");
        float[] arr = { 1.1f, 2.2f, 3.3f };
        joiner.appendAll(arr);
        assertEquals("1.1,2.2,3.3", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        float[] arr = { 1.1f, 2.2f, 3.3f, 4.4f };
        joiner.appendAll(arr, 1, 3);
        assertEquals("2.2,3.3", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new float[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((float[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArray() {
        Joiner joiner = Joiner.with(",");
        double[] arr = { 1.11, 2.22, 3.33 };
        joiner.appendAll(arr);
        assertEquals("1.11,2.22,3.33", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        double[] arr = { 1.11, 2.22, 3.33, 4.44 };
        joiner.appendAll(arr, 1, 3);
        assertEquals("2.22,3.33", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new double[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((double[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArray() {
        Joiner joiner = Joiner.with(",");
        String[] arr = { "a", "b", "c" };
        joiner.appendAll(arr);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayWithRange() {
        Joiner joiner = Joiner.with(",");
        String[] arr = { "a", "b", "c", "d" };
        joiner.appendAll(arr, 1, 3);
        assertEquals("b,c", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayWithNulls() {
        Joiner joiner = Joiner.with(",");
        String[] arr = { "a", null, "c" };
        joiner.appendAll(arr);
        assertEquals("a,null,c", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayWithNullsSkipNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        String[] arr = { "a", null, "c" };
        joiner.appendAll(arr);
        assertEquals("a,c", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new Object[0]).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((Object[]) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanList() {
        Joiner joiner = Joiner.with(",");
        BooleanList list = BooleanList.of(true, false, true);
        joiner.appendAll(list);
        assertEquals("true,false,true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanListWithRange() {
        Joiner joiner = Joiner.with(",");
        BooleanList list = BooleanList.of(true, false, true, false);
        joiner.appendAll(list, 1, 3);
        assertEquals("false,true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new BooleanList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((BooleanList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCharList() {
        Joiner joiner = Joiner.with(",");
        CharList list = CharList.of('a', 'b', 'c');
        joiner.appendAll(list);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCharListWithRange() {
        Joiner joiner = Joiner.with(",");
        CharList list = CharList.of('a', 'b', 'c', 'd');
        joiner.appendAll(list, 1, 3);
        assertEquals("b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCharListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new CharList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCharListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((CharList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllByteList() {
        Joiner joiner = Joiner.with(",");
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        joiner.appendAll(list);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendAllByteListWithRange() {
        Joiner joiner = Joiner.with(",");
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        joiner.appendAll(list, 1, 3);
        assertEquals("2,3", joiner.toString());
    }

    @Test
    public void testAppendAllByteListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new ByteList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllByteListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((ByteList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllShortList() {
        Joiner joiner = Joiner.with(",");
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);
        joiner.appendAll(list);
        assertEquals("10,20,30", joiner.toString());
    }

    @Test
    public void testAppendAllShortListWithRange() {
        Joiner joiner = Joiner.with(",");
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 40);
        joiner.appendAll(list, 1, 3);
        assertEquals("20,30", joiner.toString());
    }

    @Test
    public void testAppendAllShortListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new ShortList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllShortListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((ShortList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIntList() {
        Joiner joiner = Joiner.with(",");
        IntList list = IntList.of(100, 200, 300);
        joiner.appendAll(list);
        assertEquals("100,200,300", joiner.toString());
    }

    @Test
    public void testAppendAllIntListWithRange() {
        Joiner joiner = Joiner.with(",");
        IntList list = IntList.of(100, 200, 300, 400);
        joiner.appendAll(list, 1, 3);
        assertEquals("200,300", joiner.toString());
    }

    @Test
    public void testAppendAllIntListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new IntList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIntListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((IntList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllLongList() {
        Joiner joiner = Joiner.with(",");
        LongList list = LongList.of(1000L, 2000L, 3000L);
        joiner.appendAll(list);
        assertEquals("1000,2000,3000", joiner.toString());
    }

    @Test
    public void testAppendAllLongListWithRange() {
        Joiner joiner = Joiner.with(",");
        LongList list = LongList.of(1000L, 2000L, 3000L, 4000L);
        joiner.appendAll(list, 1, 3);
        assertEquals("2000,3000", joiner.toString());
    }

    @Test
    public void testAppendAllLongListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new LongList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllLongListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((LongList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllFloatList() {
        Joiner joiner = Joiner.with(",");
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f);
        joiner.appendAll(list);
        assertEquals("1.1,2.2,3.3", joiner.toString());
    }

    @Test
    public void testAppendAllFloatListWithRange() {
        Joiner joiner = Joiner.with(",");
        FloatList list = FloatList.of(1.1f, 2.2f, 3.3f, 4.4f);
        joiner.appendAll(list, 1, 3);
        assertEquals("2.2,3.3", joiner.toString());
    }

    @Test
    public void testAppendAllFloatListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new FloatList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllFloatListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((FloatList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleList() {
        Joiner joiner = Joiner.with(",");
        DoubleList list = DoubleList.of(1.11, 2.22, 3.33);
        joiner.appendAll(list);
        assertEquals("1.11,2.22,3.33", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleListWithRange() {
        Joiner joiner = Joiner.with(",");
        DoubleList list = DoubleList.of(1.11, 2.22, 3.33, 4.44);
        joiner.appendAll(list, 1, 3);
        assertEquals("2.22,3.33", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleListEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new DoubleList()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleListNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((DoubleList) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCollection() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c");
        joiner.appendAll(list);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCollectionWithRange() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c", "d");
        joiner.appendAll(list, 1, 3);
        assertEquals("b,c", joiner.toString());
    }

    @Test
    public void testAppendAllCollectionEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll(new ArrayList<>()).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllCollectionNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((Collection<?>) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIterable() {
        Joiner joiner = Joiner.with(",");
        Iterable<String> iterable = Arrays.asList("a", "b", "c");
        joiner.appendAll(iterable);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllIterableNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((Iterable<?>) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIterableWithFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "bb", "c", "dd");
        joiner.appendAll(list, s -> s.length() == 1);
        assertEquals("a,c", joiner.toString());
    }

    @Test
    public void testAppendAllIterableWithFilterAllFiltered() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("aa", "bb", "cc");
        joiner.appendAll(list, s -> s.length() == 1);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendAllIterableNullFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendAll(list, (Predicate<String>) null));
    }

    @Test
    public void testAppendAllIterator() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c");
        joiner.appendAll(list.iterator());
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").appendAll((Iterator<?>) null).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorWithFilter() {
        Joiner joiner = Joiner.with(",");
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        joiner.appendAll(list.iterator(), i -> i % 2 == 0);
        assertEquals("2,4", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorWithFilterNone() {
        Joiner joiner = Joiner.with(",");
        List<Integer> list = Arrays.asList(1, 3, 5);
        joiner.appendAll(list.iterator(), i -> i % 2 == 0);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorNullFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendAll(list.iterator(), (Predicate<String>) null));
    }

    @Test
    public void testAppendEntryBoolean() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("flag", true).appendEntry("active", false);
        assertEquals("flag=true,active=false", joiner.toString());
    }

    @Test
    public void testAppendEntryChar() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("letter", 'A').appendEntry("digit", '9');
        assertEquals("letter=A,digit=9", joiner.toString());
    }

    @Test
    public void testAppendEntryInt() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("count", 42).appendEntry("total", 100);
        assertEquals("count=42,total=100", joiner.toString());
    }

    @Test
    public void testAppendEntryLong() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("id", 1234567890L).appendEntry("timestamp", 9876543210L);
        assertEquals("id=1234567890,timestamp=9876543210", joiner.toString());
    }

    @Test
    public void testAppendEntryFloat() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("pi", 3.14f).appendEntry("e", 2.71f);
        assertEquals("pi=3.14,e=2.71", joiner.toString());
    }

    @Test
    public void testAppendEntryDouble() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("pi", 3.14159).appendEntry("e", 2.71828);
        assertEquals("pi=3.14159,e=2.71828", joiner.toString());
    }

    @Test
    public void testAppendEntryString() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("name", "Alice").appendEntry("city", "NYC");
        assertEquals("name=Alice,city=NYC", joiner.toString());
    }

    @Test
    public void testAppendEntryStringNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("key1", (String) null).appendEntry("key2", "value");
        assertEquals("key1=null,key2=value", joiner.toString());
    }

    @Test
    public void testAppendEntryCharSequence() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs = new StringBuilder("test");
        joiner.appendEntry("key", cs);
        assertEquals("key=test", joiner.toString());
    }

    @Test
    public void testAppendEntryCharSequenceNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("key", (CharSequence) null);
        assertEquals("key=null", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder() {
        Joiner joiner = Joiner.with(",");
        StringBuilder sb = new StringBuilder("builder");
        joiner.appendEntry("key", sb);
        assertEquals("key=builder", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilderNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("key", (StringBuilder) null);
        assertEquals("key=null", joiner.toString());
    }

    @Test
    public void testAppendEntryObject() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("int", new Integer(123)).appendEntry("double", new Double(4.56));
        assertEquals("int=123,double=4.56", joiner.toString());
    }

    @Test
    public void testAppendEntryObjectNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("key", (Object) null);
        assertEquals("key=null", joiner.toString());
    }

    @Test
    public void testAppendEntryMapEntry() {
        Joiner joiner = Joiner.with(",");
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "value");
        joiner.appendEntry(entry);
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntryMapEntryNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry(null);
        assertEquals("null", joiner.toString());
    }

    @Test
    public void testAppendEntryWithCustomDelimiter() {
        Joiner joiner = Joiner.with(",", "->");
        joiner.appendEntry("key", "value");
        assertEquals("key->value", joiner.toString());
    }

    @Test
    public void testAppendEntryWithTrim() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.appendEntry("  key  ", "  value  ");
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntriesMap() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        joiner.appendEntries(map);
        assertEquals("a=1,b=2,c=3", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before").appendEntries(new HashMap<>()).append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before").appendEntries((Map<?, ?>) null).append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithRange() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        joiner.appendEntries(map, 1, 3);
        assertEquals("b=2,c=3", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithRangeInvalid() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendEntries(map, 1, 3));
    }

    @Test
    public void testAppendEntriesMapWithPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        joiner.appendEntries(map, entry -> entry.getValue() > 1);
        assertEquals("b=2,c=3", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithPredicateNone() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        joiner.appendEntries(map, entry -> entry.getValue() > 10);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapNullPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendEntries(map, (Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testAppendEntriesMapWithBiPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        BiPredicate<String, Integer> filter = (k, v) -> v % 2 == 0;
        joiner.appendEntries(map, filter);
        assertEquals("b=2", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithBiPredicateNone() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("c", 3);
        BiPredicate<String, Integer> filter = (k, v) -> v % 2 == 0;
        joiner.appendEntries(map, filter);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithExtractors() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);
        Function<String, String> keyExtractor = k -> k.toUpperCase();
        Function<Integer, String> valueExtractor = v -> "val:" + v;
        joiner.appendEntries(map, keyExtractor, valueExtractor);
        assertEquals("KEY1=val:100,KEY2=val:200", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapNullKeyExtractor() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendEntries(map, null, Function.identity()));
    }

    @Test
    public void testAppendEntriesMapNullValueExtractor() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendEntries(map, Function.identity(), null));
    }

    @Test
    public void testAppendBean() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        joiner.appendBean(person);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertTrue(result.contains("city=NYC"));
        assertTrue(result.contains("salary=50000.0"));
    }

    @Test
    public void testAppendBeanNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before").appendBean((Object) null).append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendBeanWithSelectPropNames() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        List<String> selectProps = Arrays.asList("name", "age");
        joiner.appendBean(person, selectProps);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendBeanWithSelectPropNamesEmpty() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        joiner.appendBean(person, Collections.emptyList());
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendBeanWithIgnoredPropNames() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        Set<String> ignoredProps = new HashSet<>(Arrays.asList("city", "salary"));
        joiner.appendBean(person, false, ignoredProps);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendBeanIgnoreNullProperty() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, null, null);
        joiner.appendBean(person, true, null);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendBeanWithFilter() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        BiPredicate<String, Object> filter = (propName, propValue) -> propName.equals("name") || propName.equals("age");
        joiner.appendBean(person, filter);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendBeanWithFilterNone() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        BiPredicate<String, Object> filter = (propName, propValue) -> false;
        joiner.appendBean(person, filter);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testRepeatString() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat("x", 3);
        assertEquals("x,x,x", joiner.toString());
    }

    @Test
    public void testRepeatStringZero() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").repeat("x", 0).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testRepeatStringNegative() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").repeat("x", -1));
    }

    @Test
    public void testRepeatStringLarge() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat("a", 10);
        String result = joiner.toString();
        assertEquals("a,a,a,a,a,a,a,a,a,a", result);
    }

    @Test
    public void testRepeatObject() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat(42, 3);
        assertEquals("42,42,42", joiner.toString());
    }

    @Test
    public void testRepeatObjectZero() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start").repeat(42, 0).append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testRepeatObjectNegative() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").repeat(new Object(), -1));
    }

    @Test
    public void testMerge() {
        Joiner joiner1 = Joiner.with(",", "[", "]");
        joiner1.append("a").append("b");

        Joiner joiner2 = Joiner.with("-");
        joiner2.append("c").append("d");

        joiner1.merge(joiner2);
        assertEquals("[a,b,c-d]", joiner1.toString());
    }

    @Test
    public void testMergeEmpty() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a");

        Joiner joiner2 = Joiner.with("-");

        joiner1.merge(joiner2);
        assertEquals("a", joiner1.toString());
    }

    @Test
    public void testMergeBothEmpty() {
        Joiner joiner1 = Joiner.with(",");
        Joiner joiner2 = Joiner.with("-");

        joiner1.merge(joiner2);
        assertEquals("", joiner1.toString());
    }

    @Test
    public void testMergeNull() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").merge(null));
    }

    @Test
    public void testMergeWithDifferentConfigurations() {
        Joiner joiner1 = Joiner.with(",", "[", "]").skipNulls();
        joiner1.append("a").append((String) null).append("b");

        Joiner joiner2 = Joiner.with("-", "{", "}").useForNull("EMPTY");
        joiner2.append("c").append((String) null).append("d");

        joiner1.merge(joiner2);
        assertEquals("[a,b,c-EMPTY-d]", joiner1.toString());
    }

    @Test
    public void testLength() {
        Joiner joiner = Joiner.with(",");
        assertEquals(0, joiner.length());

        joiner.append("hello");
        assertEquals(5, joiner.length());

        joiner.append("world");
        assertEquals(11, joiner.length());
    }

    @Test
    public void testLengthWithPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertEquals(2, joiner.length());

        joiner.append("a");
        assertEquals(3, joiner.length());

        joiner.append("b");
        assertEquals(5, joiner.length());
    }

    @Test
    public void testLengthAfterClear() {
        Joiner joiner = Joiner.with(",");
        joiner.append("test");
        joiner.toString();
        assertTrue(joiner.length() >= 0);
    }

    @Test
    public void testToStringEmpty() {
        Joiner joiner = Joiner.with(",");
        assertEquals("", joiner.toString());
    }

    @Test
    public void testToStringEmptyWithPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertEquals("[]", joiner.toString());
    }

    @Test
    public void testToStringWithEmptyValue() {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        assertEquals("EMPTY", joiner.toString());
    }

    @Test
    public void testToStringWithContent() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b").append("c");
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testToStringWithPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        joiner.append("a").append("b");
        assertEquals("[a,b]", joiner.toString());
    }

    @Test
    public void testToStringMultipleCalls() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        String first = joiner.toString();
        joiner.append("c");
        String second = joiner.toString();
        assertEquals("a,b", first);
        assertEquals("a,b,c", second);
    }

    @Test
    public void testAppendTo() throws IOException {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b").append("c");

        StringBuilder sb = new StringBuilder("prefix:");
        joiner.appendTo(sb);
        assertEquals("prefix:a,b,c", sb.toString());
    }

    @Test
    public void testAppendToEmpty() throws IOException {
        Joiner joiner = Joiner.with(",");
        StringBuilder sb = new StringBuilder("result:");
        joiner.appendTo(sb);
        assertEquals("result:", sb.toString());
    }

    @Test
    public void testAppendToWithPrefixSuffix() throws IOException {
        Joiner joiner = Joiner.with(",", "[", "]");
        joiner.append("a").append("b");

        StringBuilder sb = new StringBuilder("data:");
        joiner.appendTo(sb);
        assertEquals("data:[a,b]", sb.toString());
    }

    @Test
    public void testAppendToStringWriter() throws IOException {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b").append("c");

        StringWriter writer = new StringWriter();
        writer.write("prefix:");
        joiner.appendTo(writer);
        assertEquals("prefix:a,b,c", writer.toString());
    }

    @Test
    public void testMap() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b").append("c");

        Integer length = joiner.map(String::length);
        assertEquals(Integer.valueOf(5), length);
    }

    @Test
    public void testMapWithTransformation() {
        Joiner joiner = Joiner.with(",");
        joiner.append("hello").append("world");

        String upper = joiner.map(String::toUpperCase);
        assertEquals("HELLO,WORLD", upper);
    }

    @Test
    public void testMapEmpty() {
        Joiner joiner = Joiner.with(",");
        Integer length = joiner.map(String::length);
        assertEquals(Integer.valueOf(0), length);
    }

    @Test
    public void testMapIfNotEmpty() {
        Joiner emptyJoiner = Joiner.with(",");
        assertFalse(emptyJoiner.mapIfNotEmpty(String::length).isPresent());

        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        assertTrue(joiner.mapIfNotEmpty(String::length).isPresent());
        assertEquals(Integer.valueOf(3), joiner.mapIfNotEmpty(String::length).get());
    }

    @Test
    public void testMapIfNotEmptyWithPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertFalse(joiner.mapIfNotEmpty(String::length).isPresent());
    }

    @Test
    public void testClose() {
        Joiner joiner = Joiner.with(",").reuseCachedBuffer();
        joiner.append("a").append("b");
        joiner.close();

        assertThrows(IllegalStateException.class, () -> joiner.append("c"));
    }

    @Test
    public void testCloseMultipleTimes() {
        Joiner joiner = Joiner.with(",");
        joiner.close();
        joiner.close();
    }

    @Test
    public void testCloseWithoutReuseCachedBuffer() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        joiner.close();
    }

    @Test
    public void testEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.append("a").append("b").append("c");
        assertEquals("abc", joiner.toString());
    }

    @Test
    public void testLongSeparator() {
        Joiner joiner = Joiner.with(" --- ");
        joiner.append("a").append("b").append("c");
        assertEquals("a --- b --- c", joiner.toString());
    }

    @Test
    public void testSpecialCharacters() {
        Joiner joiner = Joiner.with("\t");
        joiner.append("a\nb").append("c\rd").append("e\tf");
        assertEquals("a\nb\tc\rd\te\tf", joiner.toString());
    }

    @Test
    public void testUnicodeCharacters() {
        Joiner joiner = Joiner.with(",");
        joiner.append("Hello").append("世界").append("🌍");
        assertEquals("Hello,世界,🌍", joiner.toString());
    }

    @Test
    public void testLargeDataset() {
        Joiner joiner = Joiner.with(",");
        for (int i = 0; i < 1000; i++) {
            joiner.append(i);
        }
        String result = joiner.toString();
        assertTrue(result.startsWith("0,1,2"));
        assertTrue(result.endsWith("997,998,999"));
    }

    @Test
    public void testMethodChaining() {
        String result = Joiner.with(",").skipNulls().trimBeforeAppend().append("  a  ").append((String) null).append("  b  ").toString();
        assertEquals("a,b", result);
    }

    @Test
    public void testComplexScenario() {
        Joiner joiner = Joiner.with(", ", " = ", "{", "}").skipNulls().trimBeforeAppend();

        joiner.appendEntry("  name  ", "  Alice  ").appendEntry("age", 30).appendEntry("city", (String) null);

        String result = joiner.toString();
        assertTrue(result.contains("name = Alice"));
        assertTrue(result.contains("age = 30"));
        assertTrue(result.contains("city"));
    }

    @Test
    public void testNullHandlingConsistency() {
        Joiner j1 = Joiner.with(",");
        j1.append((String) null);
        assertEquals("null", j1.toString());

        Joiner j2 = Joiner.with(",").skipNulls();
        j2.append((String) null);
        assertEquals("", j2.toString());

        Joiner j3 = Joiner.with(",").useForNull("NONE");
        j3.append((String) null);
        assertEquals("NONE", j3.toString());
    }

    @Test
    public void testIndexOutOfBoundsExceptions() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3 };

        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(arr, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(arr, 2, 1));
    }

    @Test
    public void testCollectionRangeInvalid() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(list, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(list, 0, 4));
    }

    @Test
    public void testAppendAllEmptyAndNullCollections() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.appendAll(new ArrayList<>());
        joiner.appendAll((Collection<?>) null);
        joiner.appendAll((Iterable<?>) null);
        joiner.appendAll((Iterator<?>) null);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testPrefixSuffixOnly() {
        Joiner joiner = Joiner.with(",", "<", ">");
        assertEquals("<>", joiner.toString());
    }

    @Test
    public void testConfigurationAfterAppend() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");

        assertThrows(IllegalStateException.class, () -> joiner.reuseCachedBuffer());
    }
}
