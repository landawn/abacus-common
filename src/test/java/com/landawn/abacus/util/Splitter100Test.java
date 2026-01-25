package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("new-test")
public class Splitter100Test extends TestBase {

    @Test
    public void testDefault() {
        Splitter splitter = Splitter.defauLt();
        List<String> result = splitter.split("a, b, c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testForLines() {
        Splitter splitter = Splitter.forLines();
        List<String> result = splitter.split("line1\nline2\rline3\r\nline4");
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), result);
    }

    @Test
    public void testWithChar() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split("");
        assertEquals(Arrays.asList(""), result);

        result = splitter.split(null);
        assertTrue(result.isEmpty());

        result = splitter.split("a");
        assertEquals(Arrays.asList("a"), result);

        result = splitter.split(",a,b");
        assertEquals(Arrays.asList("", "a", "b"), result);

        result = splitter.split("a,b,");
        assertEquals(Arrays.asList("a", "b", ""), result);

        result = splitter.split("a,,b");
        assertEquals(Arrays.asList("a", "", "b"), result);
    }

    @Test
    public void testWithCharSequence() {
        Splitter splitter = Splitter.with("::");
        List<String> result = splitter.split("a::b::c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(":");
        result = splitter.split("a:b:c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(" ").trimResults().omitEmptyStrings();
        result = splitter.split("a b  c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.with((CharSequence) null));
    }

    @Test
    public void testWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        Splitter splitter = Splitter.with(pattern);
        List<String> result = splitter.split("a  b\tc\nd");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.with((Pattern) null));

        Pattern emptyPattern = Pattern.compile(".*");
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(emptyPattern));
    }

    @Test
    public void testPatternCharSequence() {
        Splitter splitter = Splitter.pattern("\\d+");
        List<String> result = splitter.split("a123b456c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(null));

        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
    }

    @Test
    public void testOmitEmptyStrings() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings();
        List<String> result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "b"), result);

        result = splitter.split(",,,");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOmitEmptyStringsBoolean() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings(true);
        List<String> result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "b"), result);

        splitter = Splitter.with(',').omitEmptyStrings(false);
        result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "", "b", ""), result);
    }

    @Test
    public void testTrimResults() {
        Splitter splitter = Splitter.with(',').trimResults();
        List<String> result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split("  ,  ,  ");
        assertEquals(Arrays.asList("", "", ""), result);
    }

    @Test
    public void testTrimBoolean() {
        Splitter splitter = Splitter.with(',').trim(true);
        List<String> result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').trim(false);
        result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a ", " b ", " c "), result);
    }

    @Test
    public void testStripResults() {
        Splitter splitter = Splitter.with(',').stripResults();
        List<String> result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split("\t\n,\r\n,\t ");
        assertEquals(Arrays.asList("", "", ""), result);
    }

    @Test
    public void testStripBoolean() {
        Splitter splitter = Splitter.with(',').strip(true);
        List<String> result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').strip(false);
        result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a\t", "\nb\t", "\tc\n"), result);
    }

    @Test
    public void testLimit() {
        Splitter splitter = Splitter.with(',').limit(2);
        List<String> result = splitter.split("a,b,c,d");
        assertEquals(Arrays.asList("a", "b,c,d"), result);

        splitter = Splitter.with(',').limit(1);
        result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a,b,c"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(-1));
    }

    @Test
    public void testSplit() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitWithSupplier() {
        Splitter splitter = Splitter.with(',');
        LinkedList<String> result = splitter.split("a,b,c", Suppliers.ofLinkedList());
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testSplitWithMapper() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Fn.f(e -> Integer.parseInt(e)));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSplitWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Class<?>) null));
    }

    @Test
    public void testSplitWithTargetClassAndSupplier() {
        Splitter splitter = Splitter.with(',');
        Set<Integer> result = splitter.split("1,2,3", Integer.class, Suppliers.ofSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result);
    }

    @Test
    public void testSplitWithType() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        List<Integer> result = splitter.split("1,2,3", intType);
        assertEquals(Arrays.asList(1, 2, 3), result);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Type<?>) null));
    }

    @Test
    public void testSplitWithTypeAndSupplier() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Vector<Integer> result = splitter.split("1,2,3", intType, Fn.s(() -> new Vector<>()));
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(result instanceof Vector);
    }

    @Test
    public void testSplitIntoCollection() {
        Splitter splitter = Splitter.with(',');
        List<String> output = new ArrayList<>();
        splitter.split("a,b,c", output);
        assertEquals(Arrays.asList("a", "b", "c"), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Collection<String>) null));
    }

    @Test
    public void testSplitIntoCollectionWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", Integer.class, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Class) null, output));

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", Integer.class, (Collection<Integer>) null));
    }

    @Test
    public void testSplitIntoCollectionWithType() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", intType, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Type<Integer>) null, output));

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", intType, (Collection<Integer>) null));
    }

    @Test
    public void testSplitToImmutableList() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<String> result = splitter.splitToImmutableList("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitToImmutableListWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<Integer> result = splitter.splitToImmutableList("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSplitToArray() {
        Splitter splitter = Splitter.with(',');
        String[] result = splitter.splitToArray("a,b,c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);

        result = splitter.splitToArray("");
        assertArrayEquals(new String[] { "" }, result);

        result = splitter.splitToArray(null);
        assertArrayEquals(new String[] {}, result);
    }

    @Test
    public void testSplitToArrayWithMapper() {
        Splitter splitter = Splitter.with(',');
        String[] result = splitter.splitToArray("a,b,c", String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C" }, result);
    }

    @Test
    public void testSplitToArrayWithArrayType() {
        Splitter splitter = Splitter.with(',');

        String[] stringResult = splitter.splitToArray("a,b,c", String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, stringResult);

        Integer[] intResult = splitter.splitToArray("1,2,3", Integer[].class);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, intResult);

        int[] primitiveResult = splitter.splitToArray("1,2,3", int[].class);
        assertArrayEquals(new int[] { 1, 2, 3 }, primitiveResult);

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (Class<?>) null));
    }

    @Test
    public void testSplitToArrayIntoExisting() {
        Splitter splitter = Splitter.with(',');
        String[] output = new String[5];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b", "c", null, null }, output);

        output = new String[2];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b" }, output);

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (String[]) null));

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", new String[0]));
    }

    @Test
    public void testSplitToStream() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.splitToStream("a,b,c").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        long count = splitter.splitToStream("a,b,c,d,e").filter(s -> s.compareTo("c") > 0).count();
        assertEquals(2, count);
    }

    @Test
    public void testSplitThenApply() {
        Splitter splitter = Splitter.with(',');
        int result = splitter.splitThenApply("a,b,c", List::size);
        assertEquals(3, result);

        String joined = splitter.splitThenApply("a,b,c", list -> String.join("-", list));
        assertEquals("a-b-c", joined);
    }

    @Test
    public void testSplitThenAccept() {
        Splitter splitter = Splitter.with(',');
        List<String> captured = new ArrayList<>();
        splitter.splitThenAccept("a,b,c", captured::addAll);
        assertEquals(Arrays.asList("a", "b", "c"), captured);
    }

    @Test
    public void testSplitAndForEach() {
        Splitter splitter = Splitter.with(',');
        List<String> captured = new ArrayList<>();
        splitter.splitThenForEach("a,b,c", captured::add);
        assertEquals(Arrays.asList("a", "b", "c"), captured);
    }

    @Test
    public void testCombinedOptions() {
        Splitter splitter = Splitter.with(',').trimResults().omitEmptyStrings().limit(3);

        List<String> result = splitter.split(" a , , b , c , d ");
        assertEquals(Arrays.asList("a", "b", "c , d"), result);
    }

    @Test
    public void testMapSplitterDefault() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.defauLt();
        Map<String, String> result = mapSplitter.split("a=1, b=2, c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterWithCharSequence() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(";", ":");
        Map<String, String> result = mapSplitter.split("a:1;b:2;c:3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(null, ":"));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with("", ":"));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(";", null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(";", ""));
    }

    @Test
    public void testMapSplitterWithPattern() {
        Pattern entryPattern = Pattern.compile("\\s*;\\s*");
        Pattern kvPattern = Pattern.compile("\\s*:\\s*");
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(entryPattern, kvPattern);
        Map<String, String> result = mapSplitter.split("a : 1 ; b : 2 ; c : 3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterPattern() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.pattern("\\s*,\\s*", "\\s*=\\s*");
        Map<String, String> result = mapSplitter.split("a = 1 , b = 2 , c = 3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterOmitEmptyStrings() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").omitEmptyStrings();
        Map<String, String> result = mapSplitter.split("a=1,,b=2,=3,c=");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("", "3");
        expected.put("c", "");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterTrimResults() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").trimResults();
        Map<String, String> result = mapSplitter.split(" a = 1 , b = 2 , c = 3 ");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterStripResults() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").stripResults();
        Map<String, String> result = mapSplitter.split("\ta\t=\t1\t,\nb\n=\n2\n,\rc\r=\r3\r");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterLimit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").limit(2);
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3,d=4");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2,c=3,d=4");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(",", "=").limit(0));
    }

    @Test
    public void testMapSplitterSplit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        result = mapSplitter.split(null);
        assertTrue(result.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1,b"));

        assertEquals(CommonUtil.asMap("a", "1=2"), mapSplitter.split("a=1=2"));
    }

    @Test
    public void testMapSplitterSplitWithSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        TreeMap<String, String> result = mapSplitter.split("b=2,a=1,c=3", Suppliers.ofTreeMap());
        TreeMap<String, String> expected = new TreeMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testMapSplitterSplitWithKeyValueTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", String.class, Integer.class);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null));
    }

    @Test
    public void testMapSplitterSplitWithTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = CommonUtil.typeOf(String.class);
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", stringType, intType);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, intType));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, null));
    }

    @Test
    public void testMapSplitterSplitWithKeyValueTypesAndSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        TreeMap<String, Integer> result = mapSplitter.split("b=2,a=1,c=3", String.class, Integer.class, Suppliers.ofTreeMap());
        TreeMap<String, Integer> expected = new TreeMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);
        assertTrue(result instanceof TreeMap);
    }

    @Test
    public void testMapSplitterSplitWithTypesAndSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = CommonUtil.typeOf(String.class);
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", stringType, intType, Suppliers.ofLinkedHashMap());
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);
        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testMapSplitterSplitIntoMap() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> output = new HashMap<>();
        mapSplitter.split("a=1,b=2,c=3", output);
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, output);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Map<String, String>) null));
    }

    @Test
    public void testMapSplitterSplitIntoMapWithKeyValueTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, Integer> output = new HashMap<>();
        mapSplitter.split("a=1,b=2,c=3", String.class, Integer.class, output);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, output);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, Integer.class, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, Integer.class, (Map<String, Integer>) null));
    }

    @Test
    public void testMapSplitterSplitIntoMapWithTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = CommonUtil.typeOf(String.class);
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Map<String, Integer> output = new HashMap<>();
        mapSplitter.split("a=1,b=2,c=3", stringType, intType, output);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, output);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, intType, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, null, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, intType, (Map<String, Integer>) null));
    }

    @Test
    public void testMapSplitterSplitToImmutableMap() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        ImmutableMap<String, String> result = mapSplitter.splitToImmutableMap("a=1,b=2,c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterSplitToImmutableMapWithKeyValueTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        ImmutableMap<String, Integer> result = mapSplitter.splitToImmutableMap("a=1,b=2,c=3", String.class, Integer.class);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterSplitToStream() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        List<Map.Entry<String, String>> entries = mapSplitter.splitToStream("a=1,b=2,c=3").toList();
        assertEquals(3, entries.size());
        assertEquals("a", entries.get(0).getKey());
        assertEquals("1", entries.get(0).getValue());
        assertEquals("b", entries.get(1).getKey());
        assertEquals("2", entries.get(1).getValue());
        assertEquals("c", entries.get(2).getKey());
        assertEquals("3", entries.get(2).getValue());

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitToStream("a=1,b").toList());
    }

    @Test
    public void testMapSplitterSplitToEntryStream() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> result = mapSplitter.splitToEntryStream("a=1,b=2,c=3").toMap();
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterSplitThenApply() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        int size = mapSplitter.splitThenApply("a=1,b=2,c=3", Map::size);
        assertEquals(3, size);

        String firstKey = mapSplitter.splitThenApply("a=1,b=2", map -> map.keySet().iterator().next());
        assertEquals("a", firstKey);
    }

    @Test
    public void testMapSplitterSplitThenAccept() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> captured = new HashMap<>();
        mapSplitter.splitThenAccept("a=1,b=2,c=3", captured::putAll);
        Map<String, String> expected = new HashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, captured);
    }

    @Test
    public void testMapSplitterDeprecatedMethods() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Map<String, String> result = mapSplitter.omitEmptyStrings(true).split("a=1,,b=2");
        assertEquals(2, result.size());

        result = mapSplitter.trim(true).split(" a = 1 , b = 2 ");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));

        result = mapSplitter.strip(true).split("\ta\t=\t1\t,\tb\t=\t2\t");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    @Test
    public void testEdgeCases() {
        Splitter splitter = Splitter.with(',');
        assertEquals(Arrays.asList(""), splitter.split(""));

        splitter = Splitter.with(',').omitEmptyStrings();
        assertTrue(splitter.split("").isEmpty());

        splitter = Splitter.with(',');
        assertEquals(Arrays.asList("", ""), splitter.split(","));

        splitter = Splitter.with(Splitter.WHITE_SPACE_PATTERN);
        assertEquals(Arrays.asList("a", "b", "c"), splitter.split("a  b\t\nc"));

        splitter = Splitter.with(',').limit(Integer.MAX_VALUE);
        assertEquals(Arrays.asList("a", "b", "c"), splitter.split("a,b,c"));
    }

    @Test
    public void testIteratorBehavior() {
        Splitter splitter = Splitter.with(',');
        var iter = splitter.iterate("a,b,c");

        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("b", iter.next());
        assertTrue(iter.hasNext());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());

        assertThrows(NoSuchElementException.class, iter::next);
    }
}
