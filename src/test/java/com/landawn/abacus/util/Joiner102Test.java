package com.landawn.abacus.util;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Joiner102Test extends TestBase {

    @Test
    public void testDefauLt() {
        Joiner joiner = Joiner.defauLt();
        joiner.append("a").append("b").append("c");
        Assertions.assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testWith() {
        Joiner joiner = Joiner.with(", ");
        joiner.append("a").append("b");
        Assertions.assertEquals("a, b", joiner.toString());
    }

    @Test
    public void testWithSeparatorAndKeyValueDelimiter() {
        Joiner joiner = Joiner.with(", ", ": ");
        joiner.appendEntry("key", "value");
        Assertions.assertEquals("key: value", joiner.toString());
    }

    @Test
    public void testWithSeparatorPrefixSuffix() {
        Joiner joiner = Joiner.with(", ", "[", "]");
        joiner.append("a").append("b");
        Assertions.assertEquals("[a, b]", joiner.toString());
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
    public void testTrimBeforeAppend() {
        Joiner joiner = Joiner.with(", ").trimBeforeAppend();
        joiner.append("  a  ").append(" b ").append("c  ");
        Assertions.assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testSkipNulls() {
        Joiner joiner = Joiner.with(", ").skipNulls();
        joiner.append("a").append((String) null).append("b");
        Assertions.assertEquals("a, b", joiner.toString());
    }

    @Test
    public void testUseForNull() {
        Joiner joiner = Joiner.with(", ").useForNull("N/A");
        joiner.append("a").append((String) null).append("b");
        Assertions.assertEquals("a, N/A, b", joiner.toString());
    }

    @Test
    public void testReuseCachedBuffer() {
        Joiner joiner = Joiner.with(", ").reuseBuffer();
        joiner.append("a").append("b");
        String result = joiner.toString();
        Assertions.assertEquals("a, b", result);
        joiner.close();
    }

    @Test
    public void testAppendBoolean() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(true).append(false);
        Assertions.assertEquals("true, false", joiner.toString());
    }

    @Test
    public void testAppendChar() {
        Joiner joiner = Joiner.with(", ");
        joiner.append('a').append('b').append('c');
        Assertions.assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testAppendInt() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(1).append(2).append(3);
        Assertions.assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testAppendLong() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(100L).append(200L);
        Assertions.assertEquals("100, 200", joiner.toString());
    }

    @Test
    public void testAppendFloat() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(1.5f).append(2.5f);
        Assertions.assertEquals("1.5, 2.5", joiner.toString());
    }

    @Test
    public void testAppendDouble() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(1.5).append(2.5);
        Assertions.assertEquals("1.5, 2.5", joiner.toString());
    }

    @Test
    public void testAppendString() {
        Joiner joiner = Joiner.with(", ");
        joiner.append("hello").append("world");
        Assertions.assertEquals("hello, world", joiner.toString());
    }

    @Test
    public void testAppendCharSequence() {
        Joiner joiner = Joiner.with(", ");
        StringBuilder sb = new StringBuilder("test");
        joiner.append("hello").append(sb);
        Assertions.assertEquals("hello, test", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceWithRange() {
        Joiner joiner = Joiner.with(", ");
        joiner.append("hello", 0, 2).append("world", 1, 4);
        Assertions.assertEquals("he, orl", joiner.toString());
    }

    @Test
    public void testAppendStringBuilder() {
        Joiner joiner = Joiner.with(", ");
        StringBuilder sb = new StringBuilder("world");
        joiner.append("hello").append(sb);
        Assertions.assertEquals("hello, world", joiner.toString());
    }

    @Test
    public void testAppendObject() {
        Joiner joiner = Joiner.with(", ");
        joiner.append(123).append("text").append(new Date(0));
        String result = joiner.toString();
        Assertions.assertTrue(result.startsWith("123, text, "));
    }

    @Test
    public void testAppendIfNotNull() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("b");
        Assertions.assertEquals("a, b", joiner.toString());
    }

    @Test
    public void testAppendIf() {
        Joiner joiner = Joiner.with(", ");
        boolean includeDetails = true;
        joiner.append("basic").appendIf(includeDetails, () -> "detailed info");
        Assertions.assertEquals("basic, detailed info", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArray() {
        Joiner joiner = Joiner.with(", ");
        boolean[] arr = { true, false, true };
        joiner.appendAll(arr);
        Assertions.assertEquals("true, false, true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanArrayWithRange() {
        Joiner joiner = Joiner.with(", ");
        boolean[] arr = { true, false, true, false };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("false, true", joiner.toString());
    }

    @Test
    public void testAppendAllCharArray() {
        Joiner joiner = Joiner.with(", ");
        char[] arr = { 'a', 'b', 'c' };
        joiner.appendAll(arr);
        Assertions.assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testAppendAllCharArrayWithRange() {
        Joiner joiner = Joiner.with("-");
        char[] arr = { 'a', 'b', 'c', 'd' };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("b-c", joiner.toString());
    }

    @Test
    public void testAppendAllByteArray() {
        Joiner joiner = Joiner.with(", ");
        byte[] arr = { 1, 2, 3 };
        joiner.appendAll(arr);
        Assertions.assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testAppendAllByteArrayWithRange() {
        Joiner joiner = Joiner.with("-");
        byte[] arr = { 1, 2, 3, 4 };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("2-3", joiner.toString());
    }

    @Test
    public void testAppendAllShortArray() {
        Joiner joiner = Joiner.with(", ");
        short[] arr = { 10, 20, 30 };
        joiner.appendAll(arr);
        Assertions.assertEquals("10, 20, 30", joiner.toString());
    }

    @Test
    public void testAppendAllShortArrayWithRange() {
        Joiner joiner = Joiner.with(" | ");
        short[] arr = { 10, 20, 30, 40 };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("20 | 30", joiner.toString());
    }

    @Test
    public void testAppendAllIntArray() {
        Joiner joiner = Joiner.with(", ");
        int[] arr = { 1, 2, 3 };
        joiner.appendAll(arr);
        Assertions.assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testAppendAllIntArrayWithRange() {
        Joiner joiner = Joiner.with("-");
        int[] arr = { 1, 2, 3, 4, 5 };
        joiner.appendAll(arr, 1, 4);
        Assertions.assertEquals("2-3-4", joiner.toString());
    }

    @Test
    public void testAppendAllLongArray() {
        Joiner joiner = Joiner.with(", ");
        long[] arr = { 100L, 200L, 300L };
        joiner.appendAll(arr);
        Assertions.assertEquals("100, 200, 300", joiner.toString());
    }

    @Test
    public void testAppendAllLongArrayWithRange() {
        Joiner joiner = Joiner.with(" - ");
        long[] arr = { 100L, 200L, 300L, 400L };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("200 - 300", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArray() {
        Joiner joiner = Joiner.with(", ");
        float[] arr = { 1.5f, 2.5f, 3.5f };
        joiner.appendAll(arr);
        Assertions.assertEquals("1.5, 2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllFloatArrayWithRange() {
        Joiner joiner = Joiner.with("; ");
        float[] arr = { 1.1f, 2.2f, 3.3f, 4.4f };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("2.2; 3.3", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArray() {
        Joiner joiner = Joiner.with(", ");
        double[] arr = { 1.5, 2.5, 3.5 };
        joiner.appendAll(arr);
        Assertions.assertEquals("1.5, 2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleArrayWithRange() {
        Joiner joiner = Joiner.with(" | ");
        double[] arr = { 1.1, 2.2, 3.3, 4.4 };
        joiner.appendAll(arr, 0, 2);
        Assertions.assertEquals("1.1 | 2.2", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArray() {
        Joiner joiner = Joiner.with(", ").skipNulls();
        Object[] arr = { "a", 1, null, "b" };
        joiner.appendAll(arr);
        Assertions.assertEquals("a, 1, b", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayWithRange() {
        Joiner joiner = Joiner.with("-");
        String[] arr = { "a", "b", "c", "d" };
        joiner.appendAll(arr, 1, 3);
        Assertions.assertEquals("b-c", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanList() {
        Joiner joiner = Joiner.with(", ");
        BooleanList list = BooleanList.of(true, false, true);
        joiner.appendAll(list);
        Assertions.assertEquals("true, false, true", joiner.toString());
    }

    @Test
    public void testAppendAllBooleanListWithRange() {
        Joiner joiner = Joiner.with("-");
        BooleanList list = BooleanList.of(true, false, true, false);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("false-true", joiner.toString());
    }

    @Test
    public void testAppendAllCharList() {
        Joiner joiner = Joiner.with(", ");
        CharList list = CharList.of('a', 'b', 'c');
        joiner.appendAll(list);
        Assertions.assertEquals("a, b, c", joiner.toString());
    }

    @Test
    public void testAppendAllCharListWithRange() {
        Joiner joiner = Joiner.with("-");
        CharList list = CharList.of('a', 'b', 'c', 'd');
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("b-c", joiner.toString());
    }

    @Test
    public void testAppendAllByteList() {
        Joiner joiner = Joiner.with(", ");
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3);
        joiner.appendAll(list);
        Assertions.assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testAppendAllByteListWithRange() {
        Joiner joiner = Joiner.with("-");
        ByteList list = ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("2-3", joiner.toString());
    }

    @Test
    public void testAppendAllShortList() {
        Joiner joiner = Joiner.with(", ");
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30);
        joiner.appendAll(list);
        Assertions.assertEquals("10, 20, 30", joiner.toString());
    }

    @Test
    public void testAppendAllShortListWithRange() {
        Joiner joiner = Joiner.with("-");
        ShortList list = ShortList.of((short) 10, (short) 20, (short) 30, (short) 40);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("20-30", joiner.toString());
    }

    @Test
    public void testAppendAllIntList() {
        Joiner joiner = Joiner.with(", ");
        IntList list = IntList.of(1, 2, 3);
        joiner.appendAll(list);
        Assertions.assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testAppendAllIntListWithRange() {
        Joiner joiner = Joiner.with("-");
        IntList list = IntList.of(1, 2, 3, 4);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("2-3", joiner.toString());
    }

    @Test
    public void testAppendAllLongList() {
        Joiner joiner = Joiner.with(", ");
        LongList list = LongList.of(100L, 200L, 300L);
        joiner.appendAll(list);
        Assertions.assertEquals("100, 200, 300", joiner.toString());
    }

    @Test
    public void testAppendAllLongListWithRange() {
        Joiner joiner = Joiner.with("-");
        LongList list = LongList.of(100L, 200L, 300L, 400L);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("200-300", joiner.toString());
    }

    @Test
    public void testAppendAllFloatList() {
        Joiner joiner = Joiner.with(", ");
        FloatList list = FloatList.of(1.5f, 2.5f, 3.5f);
        joiner.appendAll(list);
        Assertions.assertEquals("1.5, 2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllFloatListWithRange() {
        Joiner joiner = Joiner.with(", ");
        FloatList list = FloatList.of(1.5f, 2.5f, 3.5f, 4.5f);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleList() {
        Joiner joiner = Joiner.with(", ");
        DoubleList list = DoubleList.of(1.5, 2.5, 3.5);
        joiner.appendAll(list);
        Assertions.assertEquals("1.5, 2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllDoubleListWithRange() {
        Joiner joiner = Joiner.with(", ");
        DoubleList list = DoubleList.of(1.5, 2.5, 3.5, 4.5);
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("2.5, 3.5", joiner.toString());
    }

    @Test
    public void testAppendAllCollection() {
        Joiner joiner = Joiner.with(", ");
        List<String> list = Arrays.asList("apple", "banana", "cherry");
        joiner.appendAll(list);
        Assertions.assertEquals("apple, banana, cherry", joiner.toString());
    }

    @Test
    public void testAppendAllCollectionWithRange() {
        Joiner joiner = Joiner.with("-");
        List<String> list = Arrays.asList("a", "b", "c", "d");
        joiner.appendAll(list, 1, 3);
        Assertions.assertEquals("b-c", joiner.toString());
    }

    @Test
    public void testAppendAllIterable() {
        Joiner joiner = Joiner.with(" | ");
        Iterable<String> iterable = Arrays.asList("one", "two", "three");
        joiner.appendAll(iterable);
        Assertions.assertEquals("one | two | three", joiner.toString());
    }

    @Test
    public void testAppendAllIterableWithFilter() {
        Joiner joiner = Joiner.with(", ");
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        joiner.appendAll(numbers, n -> n % 2 == 0);
        Assertions.assertEquals("2, 4", joiner.toString());
    }

    @Test
    public void testAppendAllIterator() {
        Joiner joiner = Joiner.with("->");
        Iterator<String> iter = Arrays.asList("x", "y", "z").iterator();
        joiner.appendAll(iter);
        Assertions.assertEquals("x->y->z", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorWithFilter() {
        Joiner joiner = Joiner.with(", ");
        Iterator<String> iter = Arrays.asList("cat", "dog", "bird", "fish").iterator();
        joiner.appendAll(iter, s -> s.length() > 3);
        Assertions.assertEquals("bird, fish", joiner.toString());
    }

    @Test
    public void testAppendEntryBoolean() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("enabled", true);
        Assertions.assertEquals("enabled=true", joiner.toString());
    }

    @Test
    public void testAppendEntryChar() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("grade", 'A');
        Assertions.assertEquals("grade=A", joiner.toString());
    }

    @Test
    public void testAppendEntryInt() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("count", 42);
        Assertions.assertEquals("count=42", joiner.toString());
    }

    @Test
    public void testAppendEntryLong() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("timestamp", 1234567890L);
        Assertions.assertEquals("timestamp=1234567890", joiner.toString());
    }

    @Test
    public void testAppendEntryFloat() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("price", 19.99f);
        Assertions.assertEquals("price=19.99", joiner.toString());
    }

    @Test
    public void testAppendEntryDouble() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("temperature", 98.6);
        Assertions.assertEquals("temperature=98.6", joiner.toString());
    }

    @Test
    public void testAppendEntryString() {
        Joiner joiner = Joiner.with(", ");
        joiner.appendEntry("name", "John");
        Assertions.assertEquals("name=John", joiner.toString());
    }

    @Test
    public void testAppendEntryCharSequence() {
        Joiner joiner = Joiner.with(", ");
        StringBuilder sb = new StringBuilder("value");
        joiner.appendEntry("key", sb);
        Assertions.assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntryStringBuilder() {
        Joiner joiner = Joiner.with(", ");
        StringBuilder sb = new StringBuilder("dynamic content");
        joiner.appendEntry("data", sb);
        Assertions.assertEquals("data=dynamic content", joiner.toString());
    }

    @Test
    public void testAppendEntryObject() {
        Joiner joiner = Joiner.with(", ");
        Date date = new Date(0);
        joiner.appendEntry("created", date);
        String result = joiner.toString();
        Assertions.assertTrue(result.startsWith("created="));
    }

    @Test
    public void testAppendEntryMapEntry() {
        Joiner joiner = Joiner.with(", ");
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("score", 100);
        joiner.appendEntry(entry);
        Assertions.assertEquals("score=100", joiner.toString());
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
    public void testAppendBean() {
        class Person {
            private String name = "John";
            private int age = 30;

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
        }

        Joiner joiner = Joiner.with(", ");
        Person p = new Person();
        joiner.appendBean(p);
        String result = joiner.toString();
        Assertions.assertTrue(result.contains("name=John"));
        Assertions.assertTrue(result.contains("age=30"));
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
    public void testAppendBeanWithFilter() {
        class Product {
            private String name = "Laptop";
            private double price = 999.99;
            private int stock = 0;

            public String getName() {
                return name;
            }

            public void setName(String name) {
                this.name = name;
            }

            public double getPrice() {
                return price;
            }

            public void setPrice(double price) {
                this.price = price;
            }

            public int getStock() {
                return stock;
            }

            public void setStock(int stock) {
                this.stock = stock;
            }
        }

        Joiner joiner = Joiner.with(", ");
        Product p = new Product();
        joiner.appendBean(p, (prop, val) -> !prop.equals("stock") || (val != null && (Integer) val > 0));
        String result = joiner.toString();
        Assertions.assertTrue(result.contains("name=Laptop"));
        Assertions.assertTrue(result.contains("price=999.99"));
        Assertions.assertFalse(result.contains("stock"));
    }

    @Test
    public void testRepeatString() {
        Joiner joiner = Joiner.with(", ");
        joiner.repeat("Hello", 3);
        Assertions.assertEquals("Hello, Hello, Hello", joiner.toString());
    }

    @Test
    public void testRepeatObject() {
        Joiner joiner = Joiner.with("-");
        Integer num = 42;
        joiner.repeat(num, 3);
        Assertions.assertEquals("42-42-42", joiner.toString());
    }

    @Test
    public void testMerge() {
        Joiner j1 = Joiner.with(", ").append("a").append("b");
        Joiner j2 = Joiner.with(", ").append("c").append("d");
        j1.merge(j2);
        Assertions.assertEquals("a, b, c, d", j1.toString());
    }

    @Test
    public void testLength() {
        Joiner j = Joiner.with(", ", "[", "]");
        j.append("a").append("b");
        Assertions.assertEquals(6, j.length());
    }

    @Test
    public void testToString() {
        Joiner j = Joiner.with(", ", "[", "]");
        j.append("a").append("b").append("c");
        Assertions.assertEquals("[a, b, c]", j.toString());
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder("Result: ");
        Joiner.with(", ").append("a").append("b").appendTo(sb);
        Assertions.assertEquals("Result: a, b", sb.toString());
    }

    @Test
    public void testMap() {
        int length = Joiner.with(", ").append("a").append("b").map(String::length);
        Assertions.assertEquals(4, length);
    }

    @Test
    public void testMapIfNotEmpty() {
        u.Optional<Integer> result1 = Joiner.with(", ").mapIfNotEmpty(String::length);
        Assertions.assertFalse(result1.isPresent());

        u.Optional<Integer> result2 = Joiner.with(", ").append("hello").mapIfNotEmpty(String::length);
        Assertions.assertTrue(result2.isPresent());
        Assertions.assertEquals(5, result2.get());
    }

    @Test
    public void testClose() {
        Joiner j = Joiner.with(", ").reuseBuffer();
        j.append("a").append("b");
        j.close();
        j.close();
    }

    @Test
    public void testEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.append("a").append("b").append("c");
        Assertions.assertEquals("abc", joiner.toString());
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
    public void testComplexScenario() {
        Joiner joiner = Joiner.with(", ", "=", "{", "}").trimBeforeAppend().skipNulls();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("name", "  John  ");
        data.put("age", 30);
        data.put("city", null);
        data.put("active", true);

        joiner.appendEntries(data);
        String result = joiner.toString();
        Assertions.assertEquals("{name=John, age=30, city=null, active=true}", result);
    }

    @Test
    public void testEdgeCases() {
        Assertions.assertEquals("", Joiner.with(", ").toString());

        Assertions.assertEquals("a", Joiner.with(", ").append("a").toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll(new int[0]).toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll((int[]) null).toString());

        Assertions.assertEquals("", Joiner.with(", ").appendAll(Collections.emptyList()).toString());
    }
}
