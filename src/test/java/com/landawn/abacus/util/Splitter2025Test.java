package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Splitter.MapSplitter;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class Splitter2025Test extends TestBase {

    @Test
    public void testDefauLt() {
        Splitter splitter = Splitter.defauLt();
        List<String> result = splitter.split("a, b, c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testForLines() {
        Splitter splitter = Splitter.forLines();
        List<String> result = splitter.split("line1\nline2\rline3\r\nline4");
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), result);

        result = splitter.split(null);
        assertTrue(result.isEmpty());
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

        assertThrows(IllegalArgumentException.class, () -> Splitter.with((CharSequence) null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(""));
    }

    @Test
    public void testWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        Splitter splitter = Splitter.with(pattern);
        List<String> result = splitter.split("a b  c   d");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.with((Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile("")));
    }

    @Test
    public void testPattern() {
        Splitter splitter = Splitter.pattern("\\s+");
        List<String> result = splitter.split("a b  c   d");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
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
    public void testOmitEmptyStrings() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings();
        List<String> result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "b"), result);

        result = splitter.split(",,,");
        assertTrue(result.isEmpty());

        result = splitter.split("");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTrimBoolean() {
        Splitter splitter = Splitter.with(',').trim(true);
        List<String> result = splitter.split("a , b , c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').trim(false);
        result = splitter.split("a , b , c");
        assertEquals(Arrays.asList("a ", " b ", " c"), result);
    }

    @Test
    public void testTrimResults() {
        Splitter splitter = Splitter.with(',').trimResults();
        List<String> result = splitter.split("a , b , c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(" a ,  b  ,   c   ");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testStripBoolean() {
        Splitter splitter = Splitter.with(',').strip(true);
        List<String> result = splitter.split("a\t,\nb\t,\tc");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').strip(false);
        result = splitter.split("a\t,\nb\t,\tc");
        assertEquals(Arrays.asList("a\t", "\nb\t", "\tc"), result);
    }

    @Test
    public void testStripResults() {
        Splitter splitter = Splitter.with(',').stripResults();
        List<String> result = splitter.split("a\t,\nb\t,\tc");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(" a\t\n,  b\r\n  ,   c\t   ");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testLimit() {
        Splitter splitter = Splitter.with(',').limit(2);
        List<String> result = splitter.split("a,b,c,d");
        assertEquals(Arrays.asList("a", "b,c,d"), result);

        splitter = Splitter.with(',').limit(1);
        result = splitter.split("a,b,c,d");
        assertEquals(Arrays.asList("a,b,c,d"), result);

        splitter = Splitter.with(',').limit(5);
        result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(-1));
    }

    @Test
    public void testSplitCharSequence() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertNotNull(result);
        assertTrue(result instanceof ArrayList);
    }

    @Test
    public void testSplitWithSupplier() {
        Splitter splitter = Splitter.with(',');
        Set<String> result = splitter.split("a,b,c", Suppliers.ofSet());
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), result);

        LinkedList<String> linkedList = splitter.split("a,b,c", Suppliers.ofLinkedList());
        assertEquals(new LinkedList<>(Arrays.asList("a", "b", "c")), linkedList);
    }

    @Test
    public void testSplitWithMapper() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Fn.f(e -> Integer.parseInt(e)));
        assertEquals(Arrays.asList(1, 2, 3), result);

        List<String> upperResult = splitter.split("a,b,c", Fn.toUpperCase());
        assertEquals(Arrays.asList("A", "B", "C"), upperResult);
    }

    @Test
    public void testSplitWithClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);

        List<Long> longResult = splitter.split("10,20,30", Long.class);
        assertEquals(Arrays.asList(10L, 20L, 30L), longResult);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Class<?>) null));
    }

    @Test
    public void testSplitWithClassAndSupplier() {
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

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Type<?>) null));
    }

    @Test
    public void testSplitWithTypeAndSupplier() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Set<Integer> result = splitter.split("1,2,3", intType, Suppliers.ofSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result);
    }

    @Test
    public void testSplitToCollection() {
        Splitter splitter = Splitter.with(',');
        List<String> output = new ArrayList<>();
        splitter.split("a,b,c", output);
        assertEquals(Arrays.asList("a", "b", "c"), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Collection<String>) null));
    }

    @Test
    public void testSplitToCollectionWithClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", Integer.class, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Class) null, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", Integer.class, (Collection) null));
    }

    @Test
    public void testSplitToCollectionWithType() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", intType, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Type<?>) null, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", intType, (Collection) null));
    }

    @Test
    public void testSplitToImmutableList() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<String> result = splitter.splitToImmutableList("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
    }

    @Test
    public void testSplitToImmutableListWithClass() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<Integer> result = splitter.splitToImmutableList("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertThrows(UnsupportedOperationException.class, () -> result.add(4));
    }

    @Test
    public void testSplitToArray() {
        Splitter splitter = Splitter.with(',');
        String[] result = splitter.splitToArray("a,b,c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);

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
    public void testSplitToArrayWithClass() {
        Splitter splitter = Splitter.with(',');
        Integer[] result = splitter.splitToArray("1,2,3", Integer[].class);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);

        int[] primitiveResult = splitter.splitToArray("1,2,3", int[].class);
        assertArrayEquals(new int[] { 1, 2, 3 }, primitiveResult);

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (Class<?>) null));
    }

    @Test
    public void testSplitToArrayWithOutput() {
        Splitter splitter = Splitter.with(',');
        String[] output = new String[3];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b", "c" }, output);

        output = new String[5];
        output[3] = "x";
        output[4] = "y";
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b", "c", "x", "y" }, output);

        output = new String[2];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b" }, output);

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (String[]) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", new String[] {}));
    }

    @Test
    public void testSplitToStream() {
        Splitter splitter = Splitter.with(',');
        Stream<String> stream = splitter.splitToStream("a,b,c");
        List<String> result = stream.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        stream = splitter.splitToStream(null);
        result = stream.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitThenApply() {
        Splitter splitter = Splitter.with(',');
        int count = splitter.splitThenApply("a,b,c", List::size);
        assertEquals(3, count);

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
        splitter.splitAndForEach("a,b,c", captured::add);
        assertEquals(Arrays.asList("a", "b", "c"), captured);
    }

    @Test
    public void testCombinedOptions() {
        Splitter splitter = Splitter.with(',').trimResults().omitEmptyStrings().limit(2);

        List<String> result = splitter.split(" a , , b , c ");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals(", b , c", result.get(1));
    }

    @Test
    public void testMapSplitterDefauLt() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.defauLt();
        Map<String, String> result = mapSplitter.split("a=1, b=2, c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterWith() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(null, "="));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(",", null));
    }

    @Test
    public void testMapSplitterWithPattern() {
        Pattern entryPattern = Pattern.compile("\\s*,\\s*");
        Pattern kvPattern = Pattern.compile("\\s*=\\s*");
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(entryPattern, kvPattern);
        Map<String, String> result = mapSplitter.split("a=1, b=2, c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with((Pattern) null, kvPattern));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(entryPattern, (Pattern) null));
    }

    @Test
    public void testMapSplitterPattern() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.pattern("\\s*,\\s*", "\\s*=\\s*");
        Map<String, String> result = mapSplitter.split("a=1, b=2, c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.pattern(null, "="));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.pattern(",", null));
    }

    @Test
    public void testMapSplitterOmitEmptyStringsBoolean() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").omitEmptyStrings(true);
        Map<String, String> result = mapSplitter.split("a=1,,b=2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterOmitEmptyStrings() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").omitEmptyStrings();
        Map<String, String> result = mapSplitter.split("a=1,,b=2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterTrimBoolean() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").trim(true);
        Map<String, String> result = mapSplitter.split("a = 1 , b = 2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterTrimResults() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").trimResults();
        Map<String, String> result = mapSplitter.split("a = 1 , b = 2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterStripBoolean() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").strip(true);
        Map<String, String> result = mapSplitter.split("a\t=\n1,b=2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterStripResults() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").stripResults();
        Map<String, String> result = mapSplitter.split("a\t=\n1,b=2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterLimit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").limit(2);
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2,c=3");
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(",", "=").limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(",", "=").limit(-1));
    }

    @Test
    public void testMapSplitterSplit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> result = mapSplitter.split("a=1,b=2");
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2, result.size());
    }

    @Test
    public void testMapSplitterSplitWithSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        TreeMap<String, String> result = mapSplitter.split("a=1,b=2", Suppliers.ofTreeMap());
        assertTrue(result instanceof TreeMap);
        assertEquals(2, result.size());
    }

    @Test
    public void testMapSplitterSplitWithClass() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<Integer, Long> result = mapSplitter.split("1=100,2=200", Integer.class, Long.class);
        Map<Integer, Long> expected = new LinkedHashMap<>();
        expected.put(1, 100L);
        expected.put(2, 200L);
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, String.class));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null));
    }

    @Test
    public void testMapSplitterSplitWithType() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Type<Long> longType = CommonUtil.typeOf(Long.class);
        Map<Integer, Long> result = mapSplitter.split("1=100,2=200", intType, longType);
        Map<Integer, Long> expected = new LinkedHashMap<>();
        expected.put(1, 100L);
        expected.put(2, 200L);
        assertEquals(expected, result);

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Type<?>) null, longType));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", intType, (Type<?>) null));
    }

    @Test
    public void testMapSplitterSplitWithClassAndSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        TreeMap<Integer, Long> result = mapSplitter.split("1=100,2=200", Integer.class, Long.class, Suppliers.ofTreeMap());
        assertTrue(result instanceof TreeMap);
        assertEquals(2, result.size());
    }

    @Test
    public void testMapSplitterSplitWithTypeAndSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Type<Long> longType = CommonUtil.typeOf(Long.class);
        TreeMap<Integer, Long> result = mapSplitter.split("1=100,2=200", intType, longType, Suppliers.ofTreeMap());
        assertTrue(result instanceof TreeMap);
        assertEquals(2, result.size());
    }

    @Test
    public void testMapSplitterSplitToMap() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> output = new HashMap<>();
        mapSplitter.split("a=1,b=2", output);
        assertEquals(2, output.size());
        assertEquals("1", output.get("a"));
        assertEquals("2", output.get("b"));

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Map<String, String>) null));
    }

    @Test
    public void testMapSplitterSplitToMapWithClass() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<Integer, Long> output = new HashMap<>();
        mapSplitter.split("1=100,2=200", Integer.class, Long.class, output);
        assertEquals(2, output.size());
        assertEquals(100L, output.get(1));

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, String.class, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, String.class, (Map) null));
    }

    @Test
    public void testMapSplitterSplitToMapWithType() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<Integer> intType = CommonUtil.typeOf(Integer.class);
        Type<Long> longType = CommonUtil.typeOf(Long.class);
        Map<Integer, Long> output = new HashMap<>();
        mapSplitter.split("1=100,2=200", intType, longType, output);
        assertEquals(2, output.size());
        assertEquals(100L, output.get(1));

        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Type<?>) null, longType, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", intType, (Type<?>) null, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", intType, longType, (Map) null));
    }

    @Test
    public void testMapSplitterSplitToImmutableMap() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        ImmutableMap<String, String> result = mapSplitter.splitToImmutableMap("a=1,b=2");
        assertEquals(2, result.size());
        assertThrows(UnsupportedOperationException.class, () -> result.put("c", "3"));
    }

    @Test
    public void testMapSplitterSplitToImmutableMapWithClass() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        ImmutableMap<Integer, Long> result = mapSplitter.splitToImmutableMap("1=100,2=200", Integer.class, Long.class);
        assertEquals(2, result.size());
        assertEquals(100L, result.get(1));
        assertThrows(UnsupportedOperationException.class, () -> result.put(3, 300L));
    }

    @Test
    public void testMapSplitterSplitToStream() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Stream<Map.Entry<String, String>> stream = mapSplitter.splitToStream("a=1,b=2,c=3");
        List<Map.Entry<String, String>> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals("1", result.get(0).getValue());

    }

    @Test
    public void testMapSplitterSplitToEntryStream() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        var entryStream = mapSplitter.splitToEntryStream("a=1,b=2");
        assertNotNull(entryStream);
        Map<String, String> result = entryStream.toMap();
        assertEquals(2, result.size());
    }

    @Test
    public void testMapSplitterSplitThenApply() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        int size = mapSplitter.splitThenApply("a=1,b=2,c=3", Map::size);
        assertEquals(3, size);
    }

    @Test
    public void testMapSplitterSplitThenAccept() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> captured = new HashMap<>();
        mapSplitter.splitThenAccept("a=1,b=2", captured::putAll);
        assertEquals(2, captured.size());
    }

    @Test
    public void testWithCharEdgeCases() {
        Splitter splitter = Splitter.with(',');

        List<String> result = splitter.split("a,,,b");
        assertEquals(Arrays.asList("a", "", "", "b"), result);

        result = splitter.split(",,,");
        assertEquals(Arrays.asList("", "", "", ""), result);

        result = splitter.split("x");
        assertEquals(Arrays.asList("x"), result);

        result = splitter.split(",x,");
        assertEquals(Arrays.asList("", "x", ""), result);
    }

    @Test
    public void testWithCharSequenceEdgeCases() {
        Splitter splitter = Splitter.with("::");

        List<String> result = splitter.split("a::::b");
        assertEquals(Arrays.asList("a", "", "b"), result);

        result = splitter.split("::a::b::");
        assertEquals(Arrays.asList("", "a", "b", ""), result);

        result = splitter.split("a:b::c:d");
        assertEquals(Arrays.asList("a:b", "c:d"), result);

        result = splitter.split("");
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testWithPatternEdgeCases() {
        Pattern pattern = Pattern.compile("\\s+");
        Splitter splitter = Splitter.with(pattern);

        List<String> result = splitter.split("a \t\n b");
        assertEquals(Arrays.asList("a", "b"), result);

        result = splitter.split("  a b  ");
        assertEquals(Arrays.asList("", "a", "b", ""), result);

        result = splitter.split("   ");
        assertEquals(Arrays.asList("", ""), result);

        Pattern digits = Pattern.compile("\\d+");
        splitter = Splitter.with(digits);
        result = splitter.split("a123b456c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testOmitEmptyStringsWithPatterns() {
        Pattern pattern = Pattern.compile("\\s+");
        Splitter splitter = Splitter.with(pattern).omitEmptyStrings();

        List<String> result = splitter.split("  a  b  c  ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split("     ");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTrimResultsEdgeCases() {
        Splitter splitter = Splitter.with(',').trimResults();

        List<String> result = splitter.split("   a   ,   b   ,   c   ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split("   ,   ");
        assertEquals(Arrays.asList("", ""), result);

        result = splitter.split(" a b , c d ");
        assertEquals(Arrays.asList("a b", "c d"), result);
    }

    @Test
    public void testStripResultsEdgeCases() {
        Splitter splitter = Splitter.with(',').stripResults();

        List<String> result = splitter.split("\t\na\r\n,\t\nb\t\n");
        assertEquals(Arrays.asList("a", "b"), result);

        result = splitter.split("\t\n,\r\n");
        assertEquals(Arrays.asList("", ""), result);

        result = splitter.split(" a\u00A0,\u2003b ");
        assertEquals(Arrays.asList("a\u00A0", "b"), result);
    }

    @Test
    public void testLimitEdgeCases() {
        Splitter splitter = Splitter.with(',');

        List<String> result = splitter.limit(3).split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.limit(1).split("a,b,c,d,e");
        assertEquals(Arrays.asList("a,b,c,d,e"), result);

        result = splitter.limit(2).split(",,,");
        assertEquals(Arrays.asList("", ",,"), result);
    }

    @Test
    public void testSplitWithMapperEdgeCases() {
        Splitter splitter = Splitter.with(',');

        List<String> result = splitter.split((CharSequence) null, Fn.toUpperCase());
        assertTrue(result.isEmpty());

        result = splitter.split("", (Function<String, String>) String::trim);
        assertEquals(Arrays.asList(""), result);

        List<Integer> lengths = splitter.split("a,bb,ccc", (Function<String, Integer>) String::length);
        assertEquals(Arrays.asList(1, 2, 3), lengths);
    }

    @Test
    public void testSplitWithClassVariousTypes() {
        Splitter splitter = Splitter.with(',');

        List<Double> doubles = splitter.split("1.5,2.5,3.5", Double.class);
        assertEquals(Arrays.asList(1.5, 2.5, 3.5), doubles);

        List<Boolean> booleans = splitter.split("true,false,true", Boolean.class);
        assertEquals(Arrays.asList(true, false, true), booleans);

        List<Integer> empty = splitter.split(null, Integer.class);
        assertTrue(empty.isEmpty());
    }

    @Test
    public void testSplitToArrayEdgeCases() {
        Splitter splitter = Splitter.with(',');

        String[] result = splitter.splitToArray("");
        assertArrayEquals(new String[] { "" }, result);

        result = splitter.splitToArray("solo");
        assertArrayEquals(new String[] { "solo" }, result);

        result = splitter.omitEmptyStrings().splitToArray("a,,b");
        assertArrayEquals(new String[] { "a", "b" }, result);
    }

    @Test
    public void testSplitToArrayPrimitiveTypes() {
        Splitter splitter = Splitter.with(',');

        long[] longArray = splitter.splitToArray("1,2,3", long[].class);
        assertArrayEquals(new long[] { 1L, 2L, 3L }, longArray);

        double[] doubleArray = splitter.splitToArray("1.1,2.2,3.3", double[].class);
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, doubleArray, 0.001);

        boolean[] boolArray = splitter.splitToArray("true,false,true", boolean[].class);
        assertArrayEquals(new boolean[] { true, false, true }, boolArray);
    }

    @Test
    public void testSplitToStreamLazyEvaluation() {
        Splitter splitter = Splitter.with(',');

        Stream<String> stream = splitter.splitToStream("a,bb,ccc,dddd");
        List<String> filtered = stream.filter(s -> s.length() > 1).toList();
        assertEquals(Arrays.asList("bb", "ccc", "dddd"), filtered);

        stream = splitter.splitToStream("1,2,3");
        List<Integer> mapped = stream.map(Integer::parseInt).toList();
        assertEquals(Arrays.asList(1, 2, 3), mapped);
    }

    @Test
    public void testSplitAndForEachSideEffects() {
        Splitter splitter = Splitter.with(',');

        List<String> collected = new ArrayList<>();
        List<Integer> lengths = new ArrayList<>();

        splitter.splitAndForEach("a,bb,ccc", s -> {
            collected.add(s);
            lengths.add(s.length());
        });

        assertEquals(Arrays.asList("a", "bb", "ccc"), collected);
        assertEquals(Arrays.asList(1, 2, 3), lengths);
    }

    @Test
    public void testCombinedConfigurationsAdvanced() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings().trimResults().limit(3);

        List<String> result = splitter.split(" a , , b , c , d ");
        assertEquals(Arrays.asList("a", "b", "c , d"), result);

        splitter = Splitter.with(',').stripResults().omitEmptyStrings();

        result = splitter.split("\ta\t,\n,\rb\r");
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testMapSplitterNullAndEmptyInputs() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Map<String, String> result = mapSplitter.split(null);
        assertTrue(result.isEmpty());

        result = mapSplitter.split("");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapSplitterComplexPatterns() {
        Pattern entryPattern = Pattern.compile("[;,]");
        Pattern kvPattern = Pattern.compile("[:=]");
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(entryPattern, kvPattern);

        Map<String, String> result = mapSplitter.split("a=1,b:2;c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterTypeConversions() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", ":");

        Map<String, Integer> result = mapSplitter.split("one:1,two:2", String.class, Integer.class);
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(1), result.get("one"));
        assertEquals(Integer.valueOf(2), result.get("two"));

        Map<Integer, Double> numMap = mapSplitter.split("1:1.5,2:2.5", Integer.class, Double.class);
        assertEquals(Double.valueOf(1.5), numMap.get(1));
    }

    @Test
    public void testMapSplitterCombinedOptions() {
        Splitter.MapSplitter mapSplitter = MapSplitter.with(",", "=").trimResults().omitEmptyStrings().limit(2);

        Map<String, String> result = mapSplitter.split(" a = 1 , , b = 2 , c = 3 ");
        assertEquals(2, result.size());
        assertTrue(result.containsKey("a"));
        assertEquals("1", result.get("a"));
        assertTrue(result.containsKey(", b"));
        assertEquals("2 , c = 3", result.get(", b"));
    }

    @Test
    public void testMapSplitterSplitToStreamFiltering() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Stream<Map.Entry<String, String>> stream = mapSplitter.splitToStream("a=1,b=2,c=3,d=4");
        List<Map.Entry<String, String>> filtered = stream.filter(e -> Integer.parseInt(e.getValue()) > 2).toList();

        assertEquals(2, filtered.size());
        assertEquals("c", filtered.get(0).getKey());
        assertEquals("3", filtered.get(0).getValue());
    }

    @Test
    public void testMapSplitterSplitToEntryStreamOperations() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        var entryStream = mapSplitter.splitToEntryStream("a=1,b=2,c=3");
        Map<String, String> filtered = entryStream.filter(e -> !e.getKey().equals("b")).toMap();

        assertEquals(2, filtered.size());
        assertTrue(filtered.containsKey("a"));
        assertTrue(filtered.containsKey("c"));
    }

    @Test
    public void testForLinesVariousLineEndings() {
        Splitter splitter = Splitter.forLines();

        List<String> result = splitter.split("line1\nline2\nline3");
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);

        result = splitter.split("line1\r\nline2\r\nline3");
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);

        result = splitter.split("line1\rline2\rline3");
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);

        result = splitter.split("line1\nline2\r\nline3\rline4");
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), result);
    }

    @Test
    public void testForLinesEdgeCases() {
        Splitter splitter = Splitter.forLines();

        List<String> result = splitter.split("line1\n\nline2");
        assertEquals(Arrays.asList("line1", "", "line2"), result);

        result = splitter.split("\n\n\n");
        assertEquals(Arrays.asList("", "", "", ""), result);

        result = splitter.omitEmptyStrings().split("line1\n\nline2\n");
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testPatternWithCapturingGroups() {
        Splitter splitter = Splitter.pattern("(\\d+)");

        List<String> result = splitter.split("abc123def456ghi");
        assertEquals(Arrays.asList("abc", "def", "ghi"), result);
    }

    @Test
    public void testSplitWithVariousCollectionTypes() {
        Splitter splitter = Splitter.with(',');

        LinkedList<String> linkedList = new LinkedList<>();
        splitter.split("a,b,c", linkedList);
        assertEquals(Arrays.asList("a", "b", "c"), linkedList);

        Set<String> set = new HashSet<>();
        splitter.split("a,b,a,c", set);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
    }

    @Test
    public void testSplitToImmutableListImmutability() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<String> result = splitter.splitToImmutableList("a,b,c");

        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, "x"));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    public void testMapSplitterSplitToImmutableMapImmutability() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        ImmutableMap<String, String> result = mapSplitter.splitToImmutableMap("a=1,b=2");

        assertThrows(UnsupportedOperationException.class, () -> result.remove("a"));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    public void testSpecialCharactersInDelimiters() {
        Splitter splitter = Splitter.with('|');
        List<String> result = splitter.split("a|b|c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with('\t');
        result = splitter.split("a\tb\tc");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(" | ");
        result = splitter.split("a | b | c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testPatternWithWordBoundaries() {
        Pattern pattern = Pattern.compile("\\b");
        Splitter splitter = Splitter.with(pattern).omitEmptyStrings();

        List<String> result = splitter.split("hello world");
        assertTrue(result.size() >= 2);
    }

    @Test
    public void testWhiteSpacePattern() {
        Splitter splitter = Splitter.with(Splitter.WHITE_SPACE_PATTERN);

        List<String> result = splitter.split("a  b\tc\nd");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        result = splitter.omitEmptyStrings().split("  a  b  ");
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testLargeInput() {
        Splitter splitter = Splitter.with(',');

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            if (i > 0)
                sb.append(',');
            sb.append("item").append(i);
        }

        List<String> result = splitter.split(sb.toString());
        assertEquals(1000, result.size());
        assertEquals("item0", result.get(0));
        assertEquals("item999", result.get(999));
    }

    @Test
    public void testChainingMultipleConfigurations() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings().trimResults().stripResults().limit(5);

        List<String> result = splitter.split(" a , \t, b ,\n c , d , e , f ");
        assertEquals(5, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testMapSplitterErrorCases() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Map<String, String> result1 = mapSplitter.split("a=");
        assertEquals("", result1.get("a"));

        Map<String, String> result2 = mapSplitter.split("=1");
        assertEquals("1", result2.get(""));

        Map<String, String> result3 = mapSplitter.split("a=b=c");
        assertEquals("b=c", result3.get("a"));
    }

    @Test
    public void testSplitThenApplyComplexTransformations() {
        Splitter splitter = Splitter.with(',');

        Integer sum = splitter.splitThenApply("1,2,3,4,5", list -> list.stream().mapToInt(Integer::parseInt).sum());
        assertEquals(15, sum);

        Map<String, Integer> map = splitter.splitThenApply("a,bb,ccc", list -> {
            Map<String, Integer> m = new HashMap<>();
            for (String s : list) {
                m.put(s, s.length());
            }
            return m;
        });
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(3), map.get("ccc"));
    }

    @Test
    public void testMapSplitterSplitThenApplyTransformations() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Integer count = mapSplitter.splitThenApply("a=1,b=2,c=3", Map::size);
        assertEquals(3, count);

        Set<String> keys = mapSplitter.splitThenApply("a=1,b=2,c=3", Map::keySet);
        assertEquals(3, keys.size());
        assertTrue(keys.contains("a"));
    }

    @Test
    public void testDefensiveCopying() {
        Splitter splitter = Splitter.with(',');
        List<String> result1 = splitter.split("a,b,c");
        List<String> result2 = splitter.split("a,b,c");

        assertTrue(result1 != result2);
        assertEquals(result1, result2);

        result1.add("d");
        assertEquals(4, result1.size());
        assertEquals(3, result2.size());
    }

    @Test
    public void testUnicodeHandling() {
        Splitter splitter = Splitter.with(',');

        List<String> result = splitter.split("你好,世界,测试");
        assertEquals(Arrays.asList("你好", "世界", "测试"), result);

        result = splitter.split("😀,😁,😂");
        assertEquals(Arrays.asList("😀", "😁", "😂"), result);

        result = splitter.split("a,你好,b,😀,c");
        assertEquals(Arrays.asList("a", "你好", "b", "😀", "c"), result);
    }

    @Test
    public void testMapSplitterUnicodeHandling() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        Map<String, String> result = mapSplitter.split("名字=张三,年龄=25");
        assertEquals("张三", result.get("名字"));
        assertEquals("25", result.get("年龄"));
    }

    @Test
    public void testSplitToArrayOutputArraySizes() {
        Splitter splitter = Splitter.with(',');

        String[] output = new String[3];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b", "c" }, output);

        output = new String[5];
        output[3] = "keep";
        output[4] = "this";
        splitter.splitToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
        assertEquals("keep", output[3]);
        assertEquals("this", output[4]);

        output = new String[2];
        splitter.splitToArray("a,b,c,d", output);
        assertArrayEquals(new String[] { "a", "b" }, output);
    }

    @Test
    public void testSplitWithMapperProducingNulls() {
        Splitter splitter = Splitter.with(',');

        List<String> result = splitter.split("a,b,c", Fn.f(s -> s.equals("b") ? null : s));
        assertEquals(Arrays.asList("a", null, "c"), result);
    }

    @Test
    public void testSplitAndForEachWithNullInput() {
        Splitter splitter = Splitter.with(',');
        List<String> collected = new ArrayList<>();

        splitter.splitAndForEach(null, collected::add);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testSplitThenAcceptWithNullInput() {
        Splitter splitter = Splitter.with(',');
        List<String> collected = new ArrayList<>();

        splitter.splitThenAccept(null, collected::addAll);
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testPatternThatDoesntMatch() {
        Pattern pattern = Pattern.compile("\\d+");
        Splitter splitter = Splitter.with(pattern);

        List<String> result = splitter.split("abcdef");
        assertEquals(Arrays.asList("abcdef"), result);
    }

    @Test
    public void testLimitOneSpecialCase() {
        Splitter splitter = Splitter.with(',').limit(1);

        List<String> result = splitter.split("a,b,c,d,e,f,g");
        assertEquals(1, result.size());
        assertEquals("a,b,c,d,e,f,g", result.get(0));

        result = splitter.split("");
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testMultipleDelimitersWithOmitEmpty() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings();

        List<String> result = splitter.split(",,,,a,,,,b,,,,c,,,,");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(",,,,,,,,");
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapSplitterAllFeaturesCombined() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(" , ", " = ").trimResults().stripResults().omitEmptyStrings().limit(3);

        Map<String, String> result = mapSplitter.split("  a  =  1  ,  b  =  2  ,  c  =  3  ,  d  =  4  ");
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3  ,  d  =  4", result.get("c"));
    }
}
