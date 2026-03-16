package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

@Tag("old-test")
public class JoinerTest extends AbstractTest {

    private static final class Person {
        private String name;
        private Integer age;
        private String city;
        private Double salary;

        Person() {
        }

        Person(String name, Integer age, String city, Double salary) {
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

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
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

    private static final class TestBean {
        private String name;
        private Integer value;
        private Integer age;
        private String city;
        private Double salary;
        private String nullField;
        private NestedBean nestedBean;

        TestBean() {
            this.name = "test";
            this.value = 123;
        }

        TestBean(String name, Integer age, String city, Double salary) {
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

        public Integer getValue() {
            return value;
        }

        public void setValue(Integer value) {
            this.value = value;
        }

        public Integer getAge() {
            return age;
        }

        public void setAge(Integer age) {
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

        public String getNullField() {
            return nullField;
        }

        public void setNullField(String nullField) {
            this.nullField = nullField;
        }

        public NestedBean getNestedBean() {
            return nestedBean;
        }

        public void setNestedBean(NestedBean nestedBean) {
            this.nestedBean = nestedBean;
        }
    }

    private static final class NestedBean {
        private String value;

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    public void test_001() throws IOException {
        String str = Joiner.withDefault()
                .useForNull("NULL")
                .append(false)
                .append(0)
                .append(3L)
                .append(5d)
                .append((Boolean) null)
                .append((Double) null)
                .toString();
        N.println(str);

        assertEquals("false, 0, 3, 5.0, NULL, NULL", str);

        Writer writer = IOUtil.newStringWriter();

        IOUtil.write(false, writer);
        IOUtil.write(0, writer);
        IOUtil.write(3L, writer);
        IOUtil.write(5d, writer);
        IOUtil.write((Boolean) null, writer);
        IOUtil.write((Double) null, writer);

        str = writer.toString();
        N.println(str);
        assertEquals("false035.0nullnull", str);

    }

    @Test
    public void testDefauLt() {
        Joiner joiner = Joiner.withDefault();
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
        Joiner joiner = Joiner.with(",").reuseBuffer();
        joiner.append("a").append("b");
        String result = joiner.toString();
        assertEquals("a,b", result);
    }

    @Test
    public void testReuseCachedBufferAfterBufferCreated() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IllegalStateException.class, () -> joiner.reuseBuffer());
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
        Joiner joiner = Joiner.with(",").reuseBuffer();
        joiner.append("a").append("b");
        joiner.close();

        assertThrows(IllegalStateException.class, () -> joiner.append("c"));
    }

    @Test
    public void testCloseMultipleTimes() {
        Joiner joiner = Joiner.with(",");
        joiner.close();
        joiner.close();
        assertNotNull(joiner);
    }

    @Test
    public void testCloseWithoutReuseCachedBuffer() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        joiner.close();
        assertNotNull(joiner);
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
    public void testWithAndToString() {
        assertEquals("a, b, c", Joiner.with(", ").append("a").append("b").append("c").toString());
        assertEquals("a#b#c", Joiner.with("#").append("a").append("b").append("c").toString());
        assertEquals("a, b, c", Joiner.with(", ").appendAll(Arrays.asList("a", "b", "c")).toString());
    }

    @Test
    public void testWithPrefixAndSuffix() {
        assertEquals("[a, b, c]", Joiner.with(", ", "[", "]").append("a").append("b").append("c").toString());
        assertEquals("START-a|b|c-END", Joiner.with("|", "START-", "-END").append("a").append("b").append("c").toString());
    }

    @Test
    public void testEmptyAndSetEmptyValue() {
        assertEquals("[]", Joiner.with(", ", "[", "]").toString());
        assertEquals("EMPTY", Joiner.with(", ").setEmptyValue("EMPTY").toString());
    }

    @Test
    public void testAppendPrimitives() {
        assertEquals("true, 1, 2, 3.0, 4.0", Joiner.with(", ").append(true).append('1').append(2L).append(3.0f).append(4.0d).toString());
    }

    @Test
    public void testAppendAllArrays() {
        assertEquals("1, 2, 3", Joiner.with(", ").appendAll(new int[] { 1, 2, 3 }).toString());
        assertEquals("2, 3", Joiner.with(", ").appendAll(new int[] { 1, 2, 3 }, 1, 3).toString());
        assertEquals("b, c", Joiner.with(", ").appendAll(new String[] { "a", "b", "c" }, 1, 3).toString());
        assertEquals("", Joiner.with(", ").appendAll(new int[0]).toString());
        assertEquals("a, c", Joiner.with(", ").skipNulls().appendAll(new String[] { "a", null, "c" }).toString());
    }

    @Test
    public void testAppendAllCollections() {
        assertEquals("1, 2, 3", Joiner.with(", ").appendAll(Arrays.asList(1, 2, 3)).toString());
        assertEquals("a, b", Joiner.with(", ").appendAll(Arrays.asList("a", "b", "c"), 0, 2).toString());
        assertEquals("a, c", Joiner.with(", ").skipNulls().appendAll(Arrays.asList("a", null, "c")).toString());
        assertEquals("", Joiner.with(", ").appendAll(Collections.emptyList()).toString());
    }

    @Test
    public void testAppendAllWithPredicate() {
        List<String> list = Arrays.asList("a", "bb", "ccc", "dd");
        Predicate<String> filter = s -> s.length() < 3;
        assertEquals("a, bb, dd", Joiner.with(", ").appendAll(list, filter).toString());
    }

    @Test
    public void testAppendEntry() {
        assertEquals("k=v", Joiner.with(", ").appendEntry("k", "v").toString());
        assertEquals("k:v", Joiner.with(", ", ":").appendEntry("k", "v").toString());
        assertEquals("k=123", Joiner.with(", ").appendEntry("k", 123).toString());
        assertEquals("key=null", Joiner.with(", ").appendEntry("key", (Object) null).toString());
    }

    @Test
    public void testAppendEntriesMapWithFilter() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", -1);
        map.put("c", 3);
        BiPredicate<String, Integer> filter = (k, v) -> v > 0;
        assertEquals("a=1, c=3", Joiner.with(", ").appendEntries(map, filter).toString());
    }

    @Test
    public void testAppendEntriesMapWithMappers() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Function<String, String> keyMapper = k -> k.toUpperCase();
        Function<Integer, String> valueMapper = v -> "v" + v;
        assertEquals("A=v1;B=v2", Joiner.with(";", "=").appendEntries(map, keyMapper, valueMapper).toString());
    }

    @Test
    public void testRepeat() {
        assertEquals("a, a, a", Joiner.with(", ").repeat("a", 3).toString());
        assertEquals("ha", Joiner.with("").repeat("ha", 1).toString());
        assertEquals("", Joiner.with(", ").repeat("a", 0).toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ").repeat("a", -1));
    }

    @Test
    public void testReuseCachedBufferThrowsException() {
        Joiner joiner = Joiner.with(", ").append("a");
        assertThrows(IllegalStateException.class, joiner::reuseBuffer);
    }

    @Test
    public void testDefaultJoiner() {
        assertEquals("a, b", Joiner.withDefault().append("a").append("b").toString());
        assertEquals("k=v", Joiner.withDefault().appendEntry("k", "v").toString());
    }

    @Test
    public void testWithKeyValueDelimiter() {
        assertEquals("k:v,a:1", Joiner.with(",", ":").appendEntry("k", "v").appendEntry("a", 1).toString());
    }

    @Test
    public void testAppendCharSequenceWithRange() {
        assertEquals("ell", Joiner.with("").append("hello", 1, 4).toString());
        assertEquals("worl", Joiner.with("").trimBeforeAppend().append(" world ", 0, 5).toString());
        assertEquals("a, wo, b", Joiner.with(", ").append("a").append("world", 0, 2).append("b").toString());
    }

    @Test
    public void testAppendAllPrimitiveArrays() {
        assertEquals("true, false", Joiner.with(", ").appendAll(new boolean[] { true, false }).toString());
        assertEquals("a, b, c", Joiner.with(", ").appendAll(new char[] { 'a', 'b', 'c' }).toString());
        assertEquals("1, 2", Joiner.with(", ").appendAll(new byte[] { 1, 2 }).toString());
        assertEquals("10, 20", Joiner.with(", ").appendAll(new short[] { 10, 20 }).toString());
        assertEquals("1.1, 2.2", Joiner.with(", ").appendAll(new float[] { 1.1f, 2.2f }).toString());
        assertEquals("3.3, 4.4", Joiner.with(", ").appendAll(new double[] { 3.3, 4.4 }).toString());
    }

    @Test
    public void testAppendAllIteratorWithPredicate() {
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        Predicate<Integer> isEven = x -> x % 2 == 0;
        assertEquals("2, 4", Joiner.with(", ").appendAll(list.iterator(), isEven).toString());
    }

    @Test
    public void testAppendEntriesBean() {
        TestBean bean = new TestBean();
        assertEquals("name=test&value=123", Joiner.with("&").appendBean(bean).toString());
    }

    @Test
    public void testAppendEntriesBeanWithSelectedProperties() {
        TestBean bean = new TestBean();
        List<String> selected = Arrays.asList("value", "name");
        assertEquals("value=123, name=test", Joiner.with(", ").appendBean(bean, selected).toString());
    }

    @Test
    public void testAppendEntriesBeanWithIgnoredProperties() {
        TestBean bean = new TestBean();
        Set<String> ignored = Set.of("active", "nullField", "age", "city", "salary", "nestedBean");
        assertEquals("name=test, value=123", Joiner.with(", ").appendBean(bean, false, ignored).toString());
    }

    @Test
    public void testAppendEntriesBeanIgnoreNull() {
        TestBean bean = new TestBean();
        assertEquals("name=test&value=123", Joiner.with("&").appendBean(bean, true, null).toString());
    }

    @Test
    public void testAppendEntriesBeanWithFilter() {
        TestBean bean = new TestBean();
        BiPredicate<String, Object> filter = (prop, val) -> prop.equals("name") || (val instanceof Integer && (Integer) val > 100);
        assertEquals("name=test, value=123", Joiner.with(", ").appendBean(bean, filter).toString());
    }

    @Test
    public void testMergeIntoEmptyJoiner() {
        Joiner j1 = Joiner.with(", ");
        Joiner j2 = Joiner.with("|").append("c").append("d");
        assertEquals("c|d", j1.merge(j2).toString());
    }

    @Test
    public void testMapIfNotEmptyOnEmptyJoiner() {
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Function<String, String> mapper = s -> {
            wasCalled.set(true);
            return s;
        };
        u.Optional<String> result = Joiner.with(",").mapIfNotEmpty(mapper);
        assertFalse(wasCalled.get());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMapIfNotEmptyOnNonEmptyJoiner() {
        AtomicBoolean wasCalled = new AtomicBoolean(false);
        Function<String, String> mapper = s -> {
            wasCalled.set(true);
            return "mapped:" + s;
        };
        u.Optional<String> result = Joiner.with(",").append("a").mapIfNotEmpty(mapper);
        assertTrue(wasCalled.get());
        assertTrue(result.isPresent());
        assertEquals("mapped:a", result.get());
    }

    @Test
    public void testWith() {
        Joiner joiner = Joiner.with(", ");
        joiner.append("a").append("b");
        Assertions.assertEquals("a, b", joiner.toString());
    }

    @Test
    public void testWithAll() {
        Joiner joiner = Joiner.with(", ", "=", "{", "}");
        joiner.appendEntry("a", 1).appendEntry("b", 2);
        Assertions.assertEquals("{a=1, b=2}", joiner.toString());
    }

    @Test
    public void testSetEmptyValue() {
        Joiner joiner = Joiner.with(", ").setEmptyValue("NONE");
        Assertions.assertEquals("NONE", joiner.toString());

        joiner.append("a");
        Assertions.assertEquals("a", joiner.toString());
    }

    @Test
    public void testAppendEntries() {
        Joiner joiner = Joiner.with(", ");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        joiner.appendEntries(map);
        Assertions.assertEquals("a=1, b=2", joiner.toString());
    }

    @Test
    public void testAppendEntriesWithRange() {
        Joiner joiner = Joiner.with(", ");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        joiner.appendEntries(map, 1, 3);
        Assertions.assertEquals("b=2, c=3", joiner.toString());
    }

    @Test
    public void testAppendEntriesWithFilter() {
        Joiner joiner = Joiner.with(", ");
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        joiner.appendEntries(map, e -> e.getValue() > 1);
        String result = joiner.toString();
        Assertions.assertTrue(result.contains("=2"));
        Assertions.assertTrue(result.contains("=3"));
        Assertions.assertFalse(result.contains("a=1"));
    }

    @Test
    public void testAppendEntriesWithBiPredicate() {
        Joiner joiner = Joiner.with(", ");
        Map<String, Integer> map = new HashMap<>();
        map.put("apple", 5);
        map.put("banana", 3);
        map.put("cherry", 8);
        joiner.appendEntries(map, (k, v) -> k.length() > 5 && v > 4);
        Assertions.assertEquals("cherry=8", joiner.toString());
    }

    @Test
    public void testAppendEntriesWithExtractors() {
        Joiner joiner = Joiner.with(", ");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("item1", 100);
        map.put("item2", 200);
        joiner.appendEntries(map, k -> k.toUpperCase(), v -> "$" + v);
        Assertions.assertEquals("ITEM1=$100, ITEM2=$200", joiner.toString());
    }

    @Test
    public void testAppendBeanWithSelectProps() {
        class Person {
            private String name = "John";
            private int age = 30;
            private String city = "NYC";

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
        }

        Joiner joiner = Joiner.with(", ");
        Person p = new Person();
        joiner.appendBean(p, Arrays.asList("name", "city"));
        Assertions.assertEquals("name=John, city=NYC", joiner.toString());
    }

    @Test
    public void testAppendBeanWithIgnoreNull() {
        class User {
            private String id = "123";
            private String name = "Alice";
            private String email = null;
            private String password = "secret";

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public String getEmail() {
                return email;
            }

            public void setEmail(String email) {
                this.email = email;
            }

            public String getPassword() {
                return password;
            }

            public void setPassword(String password) {
                this.password = password;
            }
        }

        Joiner joiner = Joiner.with(", ");
        User u = new User();
        Set<String> ignored = new HashSet<>(Arrays.asList("password"));
        joiner.appendBean(u, true, ignored);
        String result = joiner.toString();
        Assertions.assertTrue(result.contains("id=123"));
        Assertions.assertTrue(result.contains("name=Alice"));
        Assertions.assertFalse(result.contains("email"));
        Assertions.assertFalse(result.contains("password"));
    }

    @Test
    public void testToString() {
        Joiner j = Joiner.with(", ", "[", "]");
        j.append("a").append("b").append("c");
        Assertions.assertEquals("[a, b, c]", j.toString());
    }

    @Test
    public void testNullHandling() {
        Joiner j1 = Joiner.with(", ");
        j1.append((String) null);
        Assertions.assertEquals("null", j1.toString());

        Joiner j2 = Joiner.with(", ").skipNulls();
        j2.append("a").append((Object) null).append("b");
        Assertions.assertEquals("a, b", j2.toString());

        Joiner j3 = Joiner.with(", ").useForNull("N/A");
        j3.append("a").append((List) null).append("b");
        Assertions.assertEquals("a, N/A, b", j3.toString());
    }

    @Test
    public void testEdgeCases() {
        Assertions.assertEquals("", Joiner.with(", ").toString());

        Assertions.assertEquals("a", Joiner.with(", ").append("a").toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll(new int[0]).toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll((int[]) null).toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll(Collections.emptyList()).toString());
    }

    @Test
    public void testAppendAllEmptyArrays() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");

        joiner.appendAll(new boolean[0]);
        joiner.appendAll(new char[0]);
        joiner.appendAll(new byte[0]);
        joiner.appendAll(new short[0]);
        joiner.appendAll(new int[0]);
        joiner.appendAll(new long[0]);
        joiner.appendAll(new float[0]);
        joiner.appendAll(new double[0]);
        joiner.appendAll(new Object[0]);

        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllNullArrays() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");

        joiner.appendAll((boolean[]) null);
        joiner.appendAll((char[]) null);
        joiner.appendAll((byte[]) null);
        joiner.appendAll((short[]) null);
        joiner.appendAll((int[]) null);
        joiner.appendAll((long[]) null);
        joiner.appendAll((float[]) null);
        joiner.appendAll((double[]) null);
        joiner.appendAll((Object[]) null);

        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllEmptyLists() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");

        joiner.appendAll(new BooleanList());
        joiner.appendAll(new CharList());
        joiner.appendAll(new ByteList());
        joiner.appendAll(new ShortList());
        joiner.appendAll(new IntList());
        joiner.appendAll(new LongList());
        joiner.appendAll(new FloatList());
        joiner.appendAll(new DoubleList());

        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllNullLists() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");

        joiner.appendAll((BooleanList) null);
        joiner.appendAll((CharList) null);
        joiner.appendAll((ByteList) null);
        joiner.appendAll((ShortList) null);
        joiner.appendAll((IntList) null);
        joiner.appendAll((LongList) null);
        joiner.appendAll((FloatList) null);
        joiner.appendAll((DoubleList) null);

        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendEntryCharArray() {
        Joiner joiner = Joiner.with(",");
        char[] arr = { 't', 'e', 's', 't' };
        joiner.appendEntry("key", arr);
        assertEquals("key=[t, e, s, t]", joiner.toString());
    }

    @Test
    public void testAppendEntryNullValues() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("null1", (String) null);
        joiner.appendEntry("null2", (CharSequence) null);
        joiner.appendEntry("null3", (StringBuilder) null);
        joiner.appendEntry("null4", (char[]) null);
        joiner.appendEntry("null5", (Object) null);
        assertEquals("null1=null,null2=null,null3=null,null4=null,null5=null", joiner.toString());
    }

    @Test
    public void testAppendEntryWithEmptyKeyValueSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("key", "value");
        assertEquals("keyvalue", joiner.toString());
    }

    @Test
    public void testAppendEntriesBeanWithSelectPropNames() {
        Joiner joiner = Joiner.with(",");
        TestBean bean = new TestBean("John", 30, "NYC", 50000.0);
        List<String> selectProps = Arrays.asList("name", "age");
        joiner.appendBean(bean, selectProps);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendEntriesBeanWithIgnoredPropNames() {
        Joiner joiner = Joiner.with(",");
        TestBean bean = new TestBean("John", 30, "NYC", 50000.0);
        Set<String> ignoredProps = new HashSet<>(Arrays.asList("city", "salary"));
        joiner.appendBean(bean, false, ignoredProps);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendEntriesBeanIgnoreNullProperty() {
        Joiner joiner = Joiner.with(",");
        TestBean bean = new TestBean("John", 30, null, null);
        joiner.appendBean(bean, true, null);
        String result = joiner.toString();
        assertTrue(result.contains("name=John"));
        assertTrue(result.contains("age=30"));
        assertFalse(result.contains("city"));
        assertFalse(result.contains("salary"));
    }

    @Test
    public void testAppendEntriesBeanNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before");
        joiner.appendBean((Object) null);
        joiner.append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testRepeatLargeNumber() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat("x", 20);
        String result = joiner.toString();
        assertEquals(39, result.length());
        assertTrue(result.startsWith("x,x,x"));
        assertTrue(result.endsWith("x,x,x"));
    }

    @Test
    public void testRepeatZero() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.repeat("x", 0);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testReuseCachedBufferMultipleOperations() {
        Joiner joiner = Joiner.with(",").reuseBuffer();

        joiner.append("a").append("b");
        assertEquals("a,b", joiner.toString());

        assertEquals("a,b,c", joiner.append("c").toString());
    }

    @Test
    public void testSequentialUse() {
        Joiner joiner = Joiner.with(",");

        for (int i = 0; i < 100; i++) {
            joiner.append(i);
        }

        String result = joiner.toString();
        assertTrue(result.startsWith("0,1,2,3"));
        assertTrue(result.contains(",97,98,99"));
    }

    @Test
    public void testLargeStringBuilder() {
        Joiner joiner = Joiner.with(",");
        StringBuilder largeBuilder = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            largeBuilder.append("x");
        }

        joiner.append(largeBuilder);
        joiner.append("end");

        String result = joiner.toString();
        assertTrue(result.startsWith("xxxx"));
        assertTrue(result.endsWith("x,end"));
    }

    @Test
    public void testAppendAllArrayInvalidFromIndex() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(arr, -1, 2));
    }

    @Test
    public void testAppendAllArrayInvalidToIndex() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(arr, 0, 4));
    }

    @Test
    public void testAppendAllCollectionInvalidRange() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendAll(list, 2, 1));
    }

    @Test
    public void testAppendEntriesMapInvalidRange() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.appendEntries(map, 1, 3));
    }

    @Test
    public void testAllConfigurationsCombined() {
        Joiner joiner = Joiner.with("|", "->", "<<", ">>").setEmptyValue("NOTHING").trimBeforeAppend().skipNulls().useForNull("NIL").reuseBuffer();

        assertEquals("NOTHING", joiner.toString());

        joiner = Joiner.with("|", "->", "<<", ">>").trimBeforeAppend().skipNulls();

        joiner.appendEntry("  key1  ", "  value1  ").appendEntry("key2", (String) null).appendEntry("key3", "value3");

        assertEquals("<<key1->value1|key2->null|key3->value3>>", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapNullBiPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(map, (BiPredicate<String, String>) null));
    }

    @Test
    public void testAppendStringWithNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("hello").append((String) null).append("world");
        assertEquals("hello,null,world", joiner.toString());
    }

    @Test
    public void testAppendCharArray() {
        Joiner joiner = Joiner.with(",");
        char[] arr1 = { 'a', 'b' };
        char[] arr2 = { 'c', 'd' };
        joiner.append(arr1).append(arr2);
        assertEquals("[a, b],[c, d]", joiner.toString());
    }

    @Test
    public void testAppendEntryPrimitives() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("bool", true).appendEntry("int", 42);
        assertEquals("bool=true,int=42", joiner.toString());
    }

    @Test
    public void testAppendAllEmptyCollection() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.appendAll(new ArrayList<>());
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    // --- Additional gap-filling tests ---

    @Test
    public void testWithDefault_KeyValueSeparator() {
        // Tests that withDefault uses DEFAULT_KEY_VALUE_DELIMITER ("=")
        Joiner joiner = Joiner.withDefault();
        joiner.appendEntry("k", "v");
        assertEquals("k=v", joiner.toString());
    }

    @Test
    public void testSetEmptyValue_ReturnsSameInstance() {
        Joiner joiner = Joiner.with(",");
        Joiner same = joiner.setEmptyValue("EMPTY");
        assertSame(joiner, same);
        assertEquals("EMPTY", joiner.toString());
    }

    @Test
    public void testTrimBeforeAppend_WithObject() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.append((Object) "  padded  ");
        assertEquals("padded", joiner.toString());
    }

    @Test
    public void testSkipNulls_WithObject() {
        Joiner joiner = Joiner.with(",").skipNulls();
        joiner.append("a").append((Object) null).append("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testUseForNull_ReturnsSameInstance() {
        Joiner joiner = Joiner.with(",");
        Joiner same = joiner.useForNull("N/A");
        assertSame(joiner, same);
        joiner.append((String) null);
        assertEquals("N/A", joiner.toString());
    }

    @Test
    public void testAppendCharSequence_WithTrim() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        CharSequence cs = "  trimmed  ";
        joiner.append(cs);
        assertEquals("trimmed", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceRange_WithTrim() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.append("  hello world  ", 2, 13);
        assertEquals("hello world", joiner.toString());
    }

    @Test
    public void testAppendStringBuilder_WithSkipNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        joiner.append("a").append((StringBuilder) null).append("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testAppendIfNotNull_WithMixed() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("c").appendIfNotNull(null);
        assertEquals("a,c", joiner.toString());
    }

    @Test
    public void testAppendIf_True() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").appendIf(true, () -> "b").append("c");
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendIf_False() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").appendIf(false, () -> "b").append("c");
        assertEquals("a,c", joiner.toString());
    }

    @Test
    public void testAppendAllIterableEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        Iterable<String> empty = Collections.emptyList();
        joiner.appendAll(empty);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        Iterator<String> emptyIter = Collections.<String> emptyList().iterator();
        joiner.appendAll(emptyIter);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendEntryObject_WithTrim() {
        Joiner joiner = Joiner.with(",", "->").trimBeforeAppend();
        joiner.appendEntry("  key  ", "  value  ");
        assertEquals("key->value", joiner.toString());
    }

    @Test
    public void testAppendEntryMapEntry_WithValues() {
        Joiner joiner = Joiner.with(",", "->");
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("age", 30);
        joiner.appendEntry(entry);
        assertEquals("age->30", joiner.toString());
    }

    @Test
    public void testAppendEntries_MultipleEntries() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Joiner joiner = Joiner.with("; ", ": ");
        joiner.appendEntries(map);
        assertEquals("a: 1; b: 2; c: 3", joiner.toString());
    }

    @Test
    public void testAppendBeanNull_WithSelectPropNames() {
        Joiner joiner = Joiner.with(",");
        joiner.appendBean(null, Arrays.asList("name"));
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendBeanNull_WithIgnoredPropNames() {
        Joiner joiner = Joiner.with(",");
        joiner.appendBean(null, false, new HashSet<>());
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendBeanNull_WithFilter() {
        Joiner joiner = Joiner.with(",");
        joiner.appendBean(null, (k, v) -> true);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testRepeatString_WithSeparator() {
        Joiner joiner = Joiner.with("-");
        joiner.repeat("x", 3);
        assertEquals("x-x-x", joiner.toString());
    }

    @Test
    public void testRepeatObject_WithSeparator() {
        Joiner joiner = Joiner.with("-");
        joiner.repeat((Object) 5, 3);
        assertEquals("5-5-5", joiner.toString());
    }

    @Test
    public void testMerge_WithPrefixSuffix() {
        Joiner joiner1 = Joiner.with(",", "[", "]");
        joiner1.append("a").append("b");

        Joiner joiner2 = Joiner.with(",", "[", "]");
        joiner2.append("c").append("d");

        joiner1.merge(joiner2);
        assertEquals("[a,b,c,d]", joiner1.toString());
    }

    @Test
    public void testLength_Empty() {
        Joiner joiner = Joiner.with(",");
        assertEquals(0, joiner.length());
    }

    @Test
    public void testAppendTo_StringBuilder() throws IOException {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        StringBuilder sb = new StringBuilder("prefix:");
        joiner.appendTo(sb);
        assertEquals("prefix:a,b", sb.toString());
    }

    @Test
    public void testMapIfNotEmpty_Empty() {
        Joiner joiner = Joiner.with(",");
        assertTrue(joiner.mapIfNotEmpty(s -> s.length()).isEmpty());
    }

    @Test
    public void testClose_ThenToString() {
        Joiner joiner = Joiner.with(",").reuseBuffer();
        joiner.append("a");
        String result = joiner.toString();
        assertEquals("a", result);
        joiner.close();
        // After close, toString returns emptyValue since buffer is recycled
        assertNotNull(joiner.toString());
    }

    // --- Additional gap-filling tests for uncovered paths ---

    @Test
    public void testAppendToEmpty_WithEmptyEmptyValue() throws IOException {
        Joiner joiner = Joiner.with(",").setEmptyValue("");
        StringBuilder sb = new StringBuilder("prefix");
        joiner.appendTo(sb);
        // emptyValue is empty string, so nothing should be appended
        assertEquals("prefix", sb.toString());
    }

    @Test
    public void testAppendToEmpty_WithNonEmptyEmptyValue() throws IOException {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        StringBuilder sb = new StringBuilder("prefix:");
        joiner.appendTo(sb);
        assertEquals("prefix:EMPTY", sb.toString());
    }

    @Test
    public void testAppendEntryStringBuilder_WithTrim() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        StringBuilder sb = new StringBuilder("  value  ");
        joiner.appendEntry("  key  ", sb);
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder_WithEmptyKeyValueSeparator() {
        Joiner joiner = Joiner.with(",", "");
        StringBuilder sb = new StringBuilder("value");
        joiner.appendEntry("key", sb);
        assertEquals("keyvalue", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder_WithTrimAndEmptyKeyValueSeparator() {
        Joiner joiner = Joiner.with(",", "").trimBeforeAppend();
        StringBuilder sb = new StringBuilder("  value  ");
        joiner.appendEntry("  key  ", sb);
        assertEquals("keyvalue", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder_NullWithEmptyKeyValueSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("key", (StringBuilder) null);
        assertEquals("keynull", joiner.toString());
    }

    @Test
    public void testMapIfNotEmpty_NullMapper() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IllegalArgumentException.class, () -> joiner.mapIfNotEmpty(null));
    }

    @Test
    public void testAppendCharSequenceRange_Null_SkipNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        joiner.append("a").append((CharSequence) null, 0, 0).append("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceRange_Null_NoSkipNulls() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append((CharSequence) null, 0, 0).append("b");
        assertEquals("a,null,b", joiner.toString());
    }

    @Test
    public void testAppendAllWithEmptySeparator_IntArray() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new int[] { 1, 2, 3 });
        assertEquals("123", joiner.toString());
    }

    @Test
    public void testAppendAllWithEmptySeparator_ObjectArray() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new String[] { "a", "b", "c" });
        assertEquals("abc", joiner.toString());
    }

    @Test
    public void testAppendAllWithEmptySeparator_Collection() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(Arrays.asList("x", "y", "z"));
        assertEquals("xyz", joiner.toString());
    }

    @Test
    public void testAppendAllWithEmptySeparator_Iterable() {
        Joiner joiner = Joiner.with("");
        Iterable<String> iterable = Arrays.asList("a", "b");
        joiner.appendAll(iterable);
        assertEquals("ab", joiner.toString());
    }

    @Test
    public void testAppendAllWithEmptySeparator_Iterator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(Arrays.asList("x", "y").iterator());
        assertEquals("xy", joiner.toString());
    }

    @Test
    public void testAppendEntriesWithEmptySeparator_Map() {
        Joiner joiner = Joiner.with("", "=");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        joiner.appendEntries(map);
        assertEquals("a=1b=2", joiner.toString());
    }

    @Test
    public void testAppendAllIterableWithFilter_NullIterable() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.appendAll((Iterable<String>) null, s -> true);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorWithFilter_NullIterator() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.appendAll((Iterator<String>) null, s -> true);
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testAppendEntryBooleanWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("flag", true);
        assertEquals("flagtrue", joiner.toString());
    }

    @Test
    public void testAppendEntryCharWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("letter", 'A');
        assertEquals("letterA", joiner.toString());
    }

    @Test
    public void testAppendEntryIntWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("count", 42);
        assertEquals("count42", joiner.toString());
    }

    @Test
    public void testAppendEntryLongWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("id", 123L);
        assertEquals("id123", joiner.toString());
    }

    @Test
    public void testAppendEntryFloatWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("val", 1.5f);
        assertEquals("val1.5", joiner.toString());
    }

    @Test
    public void testAppendEntryDoubleWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("pi", 3.14);
        assertEquals("pi3.14", joiner.toString());
    }

    @Test
    public void testAppendEntryStringWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("name", "Alice");
        assertEquals("nameAlice", joiner.toString());
    }

    @Test
    public void testAppendEntryCharSequenceWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("key", (CharSequence) "value");
        assertEquals("keyvalue", joiner.toString());
    }

    @Test
    public void testAppendEntryObjectWithEmptyKvSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("num", (Object) 99);
        assertEquals("num99", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithFilter_EmptyMap() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new HashMap<>();
        joiner.appendEntries(map, entry -> true);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithBiPredicate_EmptyMap() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new HashMap<>();
        joiner.appendEntries(map, (k, v) -> true);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithBiPredicate_NullFilter() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        assertThrows(IllegalArgumentException.class, () -> joiner.appendEntries(map, (java.util.function.BiPredicate<String, Integer>) null));
    }

    @Test
    public void testAppendEntriesMapWithExtractors_EmptyMap() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new HashMap<>();
        joiner.appendEntries(map, String::toUpperCase, v -> v * 2);
        assertEquals("", joiner.toString());
    }

    @Test
    public void testRepeatNull() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat((String) null, 3);
        assertEquals("null,null,null", joiner.toString());
    }

    @Test
    public void testRepeatStringLargeN() {
        // Tests the branch where n >= 10, which uses Strings.repeat
        Joiner joiner = Joiner.with(",");
        joiner.repeat("x", 15);
        String result = joiner.toString();
        String[] parts = result.split(",");
        assertEquals(15, parts.length);
        for (String part : parts) {
            assertEquals("x", part);
        }
    }

    @Test
    public void testTrimBeforeAppend_WithCharSequenceRange() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.append("  hello world  ", 0, 15);
        assertEquals("hello world", joiner.toString());
    }

    @Test
    public void testAppendBeanFilter_NullFilter() {
        Joiner joiner = Joiner.with(",");
        Person person = new Person("John", 30, "NYC", 50000.0);
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(person, (BiPredicate<String, Object>) null));
    }

    @Test
    public void testReuseBuffer_TryWithResources() {
        String result;
        try (Joiner joiner = Joiner.with(",").reuseBuffer()) {
            joiner.append("a").append("b").append("c");
            result = joiner.toString();
        }
        assertEquals("a,b,c", result);
    }

    @Test
    public void testAppendAllBooleanArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new boolean[] { true, false });
        assertEquals("truefalse", joiner.toString());
    }

    @Test
    public void testAppendAllCharArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new char[] { 'a', 'b' });
        assertEquals("ab", joiner.toString());
    }

    @Test
    public void testAppendAllByteArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new byte[] { 1, 2 });
        assertEquals("12", joiner.toString());
    }

    @Test
    public void testAppendAllShortArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new short[] { 10, 20 });
        assertEquals("1020", joiner.toString());
    }

    @Test
    public void testAppendAllLongArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new long[] { 100L, 200L });
        assertEquals("100200", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new float[] { 1.1f, 2.2f });
        assertEquals("1.12.2", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArrayWithEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.appendAll(new double[] { 1.1, 2.2 });
        assertEquals("1.12.2", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceRange_NullValueUsesConfiguredText() {
        Joiner joiner = Joiner.with(",").useForNull("NIL");
        joiner.append("a").append((CharSequence) null, 0, 0).append("b");

        assertEquals("a,NIL,b", joiner.toString());
    }

    @Test
    public void testToString_AfterReuseBufferWithSuffix() {
        Joiner joiner = Joiner.with(", ", "=", "{", "}").reuseBuffer();

        assertEquals("{a=1}", joiner.appendEntry("a", 1).toString());
        assertEquals("{a=1, b=2}", joiner.appendEntry("b", 2).toString());
    }

    @Test
    public void testWithFullConfiguration_NullArguments() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(null, "=", "{", "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", null, "{", "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", "=", null, "}"));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",", "=", "{", null));
    }

}
