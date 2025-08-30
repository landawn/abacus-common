package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class Joiner100Test extends TestBase {

    @BeforeEach
    public void setUp() {
        // Clean up before each test
    }

    // ===== Factory Methods Tests =====

    @Test
    public void testDefauLt() {
        Joiner joiner = Joiner.defauLt();
        assertNotNull(joiner);
    }

    @Test
    public void testWithSeparator() {
        Joiner joiner = Joiner.with(",");
        assertNotNull(joiner);
        joiner.append("a").append("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testWithSeparatorAndKeyValueDelimiter() {
        Joiner joiner = Joiner.with(",", ":");
        assertNotNull(joiner);
        joiner.appendEntry("key", "value");
        assertEquals("key:value", joiner.toString());
    }

    @Test
    public void testWithSeparatorPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertNotNull(joiner);
        joiner.append("a").append("b");
        assertEquals("[a,b]", joiner.toString());
    }

    @Test
    public void testWithAllParameters() {
        Joiner joiner = Joiner.with(",", ":", "[", "]");
        assertNotNull(joiner);
        joiner.appendEntry("k1", "v1").appendEntry("k2", "v2");
        assertEquals("[k1:v1,k2:v2]", joiner.toString());
    }

    // ===== Configuration Methods Tests =====

    @Test
    public void testSetEmptyValue() {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        assertEquals("EMPTY", joiner.toString());
    }

    @Test
    public void testSetEmptyValueNull() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").setEmptyValue(null));
    }

    @Test
    public void testTrimBeforeAppend() {
        Joiner joiner = Joiner.with(",").trimBeforeAppend();
        joiner.append("  a  ").append("  b  ");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testSkipNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testUseForNull() {
        Joiner joiner = Joiner.with(",").useForNull("NULL");
        joiner.append("a").append((String) null).append("b");
        assertEquals("a,NULL,b", joiner.toString());
    }

    @Test
    public void testReuseCachedBuffer() {
        Joiner joiner = Joiner.with(",").reuseCachedBuffer();
        joiner.append("a").append("b");
        String result = joiner.toString();
        assertEquals("a,b", result);
        // After toString(), the buffer should be recycled
    }

    // ===== Append Primitive Types Tests =====

    @Test
    public void testAppendBoolean() {
        Joiner joiner = Joiner.with(",");
        joiner.append(true).append(false);
        assertEquals("true,false", joiner.toString());
    }

    @Test
    public void testAppendChar() {
        Joiner joiner = Joiner.with(",");
        joiner.append('a').append('b');
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testAppendInt() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1).append(2);
        assertEquals("1,2", joiner.toString());
    }

    @Test
    public void testAppendLong() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1L).append(2L);
        assertEquals("1,2", joiner.toString());
    }

    @Test
    public void testAppendFloat() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1.5f).append(2.5f);
        assertEquals("1.5,2.5", joiner.toString());
    }

    @Test
    public void testAppendDouble() {
        Joiner joiner = Joiner.with(",");
        joiner.append(1.5).append(2.5);
        assertEquals("1.5,2.5", joiner.toString());
    }

    // ===== Append String and CharSequence Tests =====

    @Test
    public void testAppendString() {
        Joiner joiner = Joiner.with(",");
        joiner.append("hello").append("world");
        assertEquals("hello,world", joiner.toString());
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
    //    @Test
    //    public void testAppendCharArrayWithOffsetAndLength() {
    //        Joiner joiner = Joiner.with(",");
    //        char[] arr = { 'a', 'b', 'c', 'd' };
    //        joiner.append(arr, 1, 2);
    //        assertEquals("bc", joiner.toString());
    //    }

    @Test
    public void testAppendCharSequence() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs1 = "hello";
        CharSequence cs2 = "world";
        joiner.append(cs1).append(cs2);
        assertEquals("hello,world", joiner.toString());
    }

    @Test
    public void testAppendCharSequenceWithStartEnd() {
        Joiner joiner = Joiner.with(",");
        CharSequence cs = "hello world";
        joiner.append(cs, 0, 5).append(cs, 6, 11);
        assertEquals("hello,world", joiner.toString());
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
    public void testAppendObject() {
        Joiner joiner = Joiner.with(",");
        joiner.append(new Integer(1)).append(new Double(2.5));
        assertEquals("1,2.5", joiner.toString());
    }

    @Test
    public void testAppendIfNotNull() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("b");
        assertEquals("a,b", joiner.toString());
    }

    @Test
    public void testAppendIf() {
        Joiner joiner = Joiner.with(",");
        joiner.appendIf(true, () -> "yes").appendIf(false, () -> "no");
        assertEquals("yes", joiner.toString());
    }

    // ===== AppendAll Array Tests =====

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
    public void testAppendAllCharArray() {
        Joiner joiner = Joiner.with(",");
        char[] arr = { 'a', 'b', 'c' };
        joiner.appendAll(arr);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllIntArray() {
        Joiner joiner = Joiner.with(",");
        int[] arr = { 1, 2, 3 };
        joiner.appendAll(arr);
        assertEquals("1,2,3", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArray() {
        Joiner joiner = Joiner.with(",");
        String[] arr = { "a", "b", "c" };
        joiner.appendAll(arr);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllObjectArrayWithNulls() {
        Joiner joiner = Joiner.with(",").skipNulls();
        String[] arr = { "a", null, "c" };
        joiner.appendAll(arr);
        assertEquals("a,c", joiner.toString());
    }

    // ===== AppendAll Collection Tests =====

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
    public void testAppendAllIterable() {
        Joiner joiner = Joiner.with(",");
        Iterable<String> iterable = Arrays.asList("a", "b", "c");
        joiner.appendAll(iterable);
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllIterableWithFilter() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "bb", "c", "dd");
        joiner.appendAll(list, s -> s.length() == 1);
        assertEquals("a,c", joiner.toString());
    }

    @Test
    public void testAppendAllIterator() {
        Joiner joiner = Joiner.with(",");
        List<String> list = Arrays.asList("a", "b", "c");
        joiner.appendAll(list.iterator());
        assertEquals("a,b,c", joiner.toString());
    }

    @Test
    public void testAppendAllIteratorWithFilter() {
        Joiner joiner = Joiner.with(",");
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5);
        joiner.appendAll(list.iterator(), i -> i % 2 == 0);
        assertEquals("2,4", joiner.toString());
    }

    // ===== AppendEntry Tests =====

    @Test
    public void testAppendEntryPrimitives() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("bool", true).appendEntry("int", 42);
        assertEquals("bool=true,int=42", joiner.toString());
    }

    @Test
    public void testAppendEntryString() {
        Joiner joiner = Joiner.with(",");
        joiner.appendEntry("key1", "value1").appendEntry("key2", "value2");
        assertEquals("key1=value1,key2=value2", joiner.toString());
    }

    @Test
    public void testAppendEntryMapEntry() {
        Joiner joiner = Joiner.with(",");
        Map.Entry<String, String> entry = new AbstractMap.SimpleEntry<>("key", "value");
        joiner.appendEntry(entry);
        assertEquals("key=value", joiner.toString());
    }

    @Test
    public void testAppendEntriesMap() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        joiner.appendEntries(map);
        assertEquals("a=1,b=2", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithRange() {
        Joiner joiner = Joiner.with(",");
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        joiner.appendEntries(map, 1, 3);
        assertEquals("b=2,c=3", joiner.toString());
    }

    @Test
    public void testAppendEntriesMapWithFilter() {
        Joiner joiner = Joiner.with(",");
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        joiner.appendEntries(map, entry -> entry.getValue() > 1);
        assertEquals("b=2,c=3", joiner.toString());
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

    // ===== Repeat Tests =====

    @Test
    public void testRepeatString() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat("x", 3);
        assertEquals("x,x,x", joiner.toString());
    }

    @Test
    public void testRepeatObject() {
        Joiner joiner = Joiner.with(",");
        joiner.repeat(42, 3);
        assertEquals("42,42,42", joiner.toString());
    }

    @Test
    public void testRepeatNegative() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").repeat("x", -1));
    }

    // ===== Merge Tests =====

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
    public void testMergeNull() {
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").merge(null));
    }

    // ===== Length Tests =====

    @Test
    public void testLength() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertEquals(2, joiner.length()); // "[" + "]"

        joiner.append("hello");
        assertEquals(7, joiner.length()); // "[hello]"
    }

    // ===== ToString Tests =====

    @Test
    public void testToStringEmpty() {
        Joiner joiner = Joiner.with(",");
        assertEquals("", joiner.toString());
    }

    @Test
    public void testToStringWithEmptyValue() {
        Joiner joiner = Joiner.with(",").setEmptyValue("EMPTY");
        assertEquals("EMPTY", joiner.toString());
    }

    @Test
    public void testToStringWithPrefixSuffix() {
        Joiner joiner = Joiner.with(",", "[", "]");
        assertEquals("[]", joiner.toString());

        joiner.append("a");
        assertEquals("[a]", joiner.toString());
    }

    // ===== AppendTo Tests =====

    @Test
    public void testAppendTo() throws IOException {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");

        StringBuilder sb = new StringBuilder("prefix:");
        joiner.appendTo(sb);
        assertEquals("prefix:a,b", sb.toString());
    }

    // ===== Map Tests =====

    @Test
    public void testMap() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");

        Integer length = joiner.map(String::length);
        assertEquals(Integer.valueOf(3), length);
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
    public void testMapToNonNullIfNotEmpty() {
        Joiner emptyJoiner = Joiner.with(",");
        assertFalse(emptyJoiner.mapToNonNullIfNotEmpty(String::length).isPresent());

        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");
        assertTrue(joiner.mapToNonNullIfNotEmpty(String::length).isPresent());
        assertEquals(Integer.valueOf(3), joiner.mapToNonNullIfNotEmpty(String::length).get());
    }

    // ===== Stream Tests =====

    @Test
    public void testStream() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a").append("b");

        List<String> result = joiner.stream().toList();
        assertEquals(1, result.size());
        assertEquals("a,b", result.get(0));
    }

    @Test
    public void testStreamIfNotEmpty() {
        Joiner emptyJoiner = Joiner.with(",");
        assertEquals(0, emptyJoiner.streamIfNotEmpty().count());

        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertEquals(1, joiner.streamIfNotEmpty().count());
    }

    // ===== Close Tests =====

    @Test
    public void testClose() {
        Joiner joiner = Joiner.with(",").reuseCachedBuffer();
        joiner.append("a").append("b");
        joiner.close();

        // After close, operations should throw IllegalStateException
        try {
            joiner.append("c");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException e) {
            // Expected
        }
    }

    @Test
    public void testMultipleClose() {
        Joiner joiner = Joiner.with(",");
        joiner.close();
        joiner.close(); // Should not throw
    }

    // ===== Edge Cases and Complex Scenarios =====

    @Test
    public void testEmptySeparator() {
        Joiner joiner = Joiner.with("");
        joiner.append("a").append("b").append("c");
        assertEquals("abc", joiner.toString());
    }

    @Test
    public void testComplexScenario() {
        Joiner joiner = Joiner.with(", ", " = ", "{", "}").skipNulls().trimBeforeAppend().useForNull("NULL");

        joiner.appendEntry("name", "  John  ").appendEntry("age", 30).appendEntry("city", (String) null);

        assertEquals("{name = John, age = 30, city = NULL}", joiner.toString());
    }

    @Test
    public void testAppendAllEmptyCollection() {
        Joiner joiner = Joiner.with(",");
        joiner.append("start");
        joiner.appendAll(new ArrayList<>());
        joiner.append("end");
        assertEquals("start,end", joiner.toString());
    }

    @Test
    public void testNullHandlingConsistency() {
        // Test without skipNulls
        Joiner j1 = Joiner.with(",");
        j1.append((String) null);
        assertEquals("null", j1.toString());

        // Test with skipNulls
        Joiner j2 = Joiner.with(",").skipNulls();
        j2.append((String) null);
        assertEquals("", j2.toString());

        // Test with custom null text
        Joiner j3 = Joiner.with(",").useForNull("NONE");
        j3.append((String) null);
        assertEquals("NONE", j3.toString());
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
}
