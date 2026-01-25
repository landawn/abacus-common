package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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

@Tag("new-test")
public class Joiner101Test extends TestBase {

    public static class TestBean {
        private String name;
        private int age;
        private String city;
        private Double salary;

        public TestBean(String name, int age, String city, Double salary) {
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
    public void testAppendEntryChar() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("char", 'A');
        assertEquals("char=A", joiner.toString());
    }

    @Test
    public void testAppendEntryFloat() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("pi", 3.14f);
        assertEquals("pi=3.14", joiner.toString());
    }

    @Test
    public void testAppendEntryDouble() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("euler", 2.71828);
        assertEquals("euler=2.71828", joiner.toString());
    }

    @Test
    public void testAppendEntryCharSequence() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs = new StringBuilder("value");
        joiner.appendEntry("key", cs);
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder() {
        Joiner joiner = Joiner.with(",");
        StringBuilder sb = new StringBuilder("builder");
        joiner.appendEntry("key", sb);
        assertEquals("key=builder", joiner.toString());
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
    public void testAppendEntryWithTrim() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.appendEntry("key", "  value  ");
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntryWithEmptyKeyValueSeparator() {
        Joiner joiner = Joiner.with(",", "");
        joiner.appendEntry("key", "value");
        assertEquals("keyvalue", joiner.toString());
    }

    @Test
    public void testAppendEntryMapEntryNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry(null);
        assertEquals("null", joiner.toString());
    }

    @Test
    public void testAppendEntriesBean() {
        Joiner joiner = Joiner.with(",");
        TestBean bean = new TestBean("John", 30, "NYC", 50000.0);
        joiner.appendBean(bean);
        assertTrue(joiner.toString().contains("name=John"));
        assertTrue(joiner.toString().contains("age=30"));
        assertTrue(joiner.toString().contains("city=NYC"));
        assertTrue(joiner.toString().contains("salary=50000.0"));
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
    public void testAppendEntriesBeanWithFilter() {
        Joiner joiner = Joiner.with(",");
        TestBean bean = new TestBean("John", 30, "NYC", 50000.0);
        BiPredicate<String, Object> filter = (propName, propValue) -> propName.equals("name") || propName.equals("age");
        joiner.appendBean(bean, filter);
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
    public void testMergeEmpty() {
        Joiner joiner1 = Joiner.with(",");
        joiner1.append("a");

        Joiner joiner2 = Joiner.with("-");

        joiner1.merge(joiner2);
        assertEquals("a", joiner1.toString());
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
    public void testAppendToStringWriter() throws IOException {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b").append("c");

        StringWriter writer = new StringWriter();
        writer.write("prefix:");
        joiner.appendTo(writer);
        assertEquals("prefix:a,b,c", writer.toString());
    }

    @Test
    public void testAppendToEmptyJoiner() throws IOException {
        Joiner joiner = Joiner.with(",");
        StringBuilder sb = new StringBuilder("result:");
        joiner.appendTo(sb);
        assertEquals("result:", sb.toString());
    }

    @Test
    public void testReuseCachedBufferMultipleOperations() {
        Joiner joiner = Joiner.with(",").reuseBuffer();

        joiner.append("a").append("b");
        assertEquals("a,b", joiner.toString());

        assertEquals("a,b,c", joiner.append("c").toString());
    }

    @Test
    public void testReuseCachedBufferAfterBufferCreated() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IllegalStateException.class, () -> joiner.reuseBuffer());
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
    public void testUseForNullWithNull() {
        Joiner joiner = Joiner.with(",").useForNull(null);
        joiner.append((String) null);
        assertEquals("null", joiner.toString());
    }

    @Test
    public void testUseForNullEmpty() {
        Joiner joiner = Joiner.with(",").useForNull("");
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,,b", joiner.toString());
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
    public void testAppendIfWithException() {
        Joiner joiner = Joiner.with(",");

        Supplier<?> exceptionSupplier = () -> {
            throw new RuntimeException("Should not be called");
        };

        joiner.appendIf(false, exceptionSupplier);
        joiner.append("safe");

        assertEquals("safe", joiner.toString());
    }

    @Test
    public void testAppendIfWithNullSupplier() {
        Joiner joiner = Joiner.with(",");

        Supplier<?> nullSupplier = () -> null;

        joiner.append("before");
        joiner.appendIf(true, nullSupplier);
        joiner.append("after");

        assertEquals("before,null,after", joiner.toString());
    }

    @Test
    public void testAppendAllIterableNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before");
        joiner.appendAll((Iterable<?>) null);
        joiner.append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorNull() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before");
        joiner.appendAll((Iterator<?>) null);
        joiner.append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendAllIterableNullFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendAll(list, (Predicate<String>) null));
    }

    @Test
    public void testAppendAllIteratorNullFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendAll(list.iterator(), (Predicate<String>) null));
    }

    @Test
    public void testAppendEntriesMapEmpty() {
        Joiner joiner = Joiner.with(",");
        joiner.append("before");
        joiner.appendEntries(new HashMap<>());
        joiner.append("after");
        assertEquals("before,after", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapNullPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendEntries(map, (Predicate<Map.Entry<String, String>>) null));
    }

    @Test
    public void testAppendEntriesMapNullBiPredicate() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new HashMap<>();
        map.put("a", "1");
        assertThrows(IllegalArgumentException.class, () -> joiner.appendBean(map, (BiPredicate<String, String>) null));
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
}
