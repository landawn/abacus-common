package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

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
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Splitter.MapSplitter;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class SplitterTest extends AbstractTest {

    @Test
    public void test_62026064() {
        final String key = "mykey";
        final String value = "=2>@C=b";
        final String combined = key + "=" + value;

        N.println(Splitter.with('=').limit(2).omitEmptyStrings(true).split(combined));
        assertNotNull(combined);
    }

    @Test
    public void test_splitToCount() {
        final int[] a = Array.rangeClosed(1, 7);

        final IntBiFunction<int[]> func = (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex);

        N.splitByChunkCount(7, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 5, false, func).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(7, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 5, false, func).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(1).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(1).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(2).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(2).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(5).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(5).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(6).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(6).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(7, 8, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 8, false, func).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(0, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(0, 5, false, func).forEach(Fn.println());
        assertNotNull(func);
    }

    @Test
    public void test_03() {
        final String str = "3341     Wed. Apr 10, 2019          4          16          22          31          42          4     ";
        final String[] strs = Splitter.with("    ").trim(true).splitToArray(str);
        N.println(strs);
        assertNotNull(strs);
    }

    @Test
    public void test_01() {
        String source = "aaaaa";
        N.println(Splitter.with("aa").split(source));
        assertTrue(CommonUtil.equals(Array.of("", "", "", "", "", ""), Splitter.with("a").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.toList("", "", "", "", "", ""), com.google.common.base.Splitter.on("a").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "a"), Splitter.with("aa").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.toList("", "", "a"), com.google.common.base.Splitter.on("aa").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "", "", "", ""), Splitter.pattern("a").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.toList("", "", "", "", "", ""), com.google.common.base.Splitter.onPattern("a").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "a"), Splitter.pattern("aa").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.toList("", "", "a"), com.google.common.base.Splitter.onPattern("aa").splitToList(source)));

        source = "a   b \t \n c \n \t \r d " + '\u0009' + '\u000B' + '\u000C' + " \re";
        N.println(source);
        N.println(Splitter.with(Splitter.WHITE_SPACE_PATTERN).split(source));
    }

    @Test
    public void test_02() {
        try {
            N.println(Splitter.with(Pattern.compile("a*")).split("aaa"));
            fail("IllegalArgumentException should be threw");
        } catch (final IllegalArgumentException e) {

        }

    }

    @Test
    public void testDefauLt() {
        Splitter splitter = Splitter.withDefault();
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
        splitter.splitThenForEach("a,b,c", captured::add);
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
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.withDefault();
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
    public void testMapSplitterightTrimBoolean() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").trim(true);
        Map<String, String> result = mapSplitter.split("a = 1 , b = 2");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        assertEquals(expected, result);
    }

    @Test
    public void testMapSplitterightTrimResults() {
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

        splitter.splitThenForEach("a,bb,ccc", s -> {
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

        splitter.splitThenForEach(null, collected::add);
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

    @Test
    public void testDefault() {
        Splitter splitter = Splitter.withDefault();
        List<String> result = splitter.split("a, b, c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
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
    public void testSplit() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = splitter.split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);

        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Class<?>) null));
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
    public void testSplitToImmutableListWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<Integer> result = splitter.splitToImmutableList("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);
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

    // --- Additional gap-filling tests ---

    @Test
    public void testWithDefault_BasicSplit() {
        List<String> result = Splitter.withDefault().split("a, b, c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testForLines_BasicSplit() {
        List<String> result = Splitter.forLines().split("line1\nline2\nline3");
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testWithChar_NullInput() {
        List<String> result = Splitter.with(',').split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWithCharSequence_NullInput() {
        List<String> result = Splitter.with("::").split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testWithPattern_NullInput() {
        List<String> result = Splitter.with(Pattern.compile(",")).split(null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOmitEmptyStrings_WithConsecutiveDelimiters() {
        List<String> result = Splitter.with(',').omitEmptyStrings().split("a,,b,,,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testTrimResults_WithWhitespace() {
        List<String> result = Splitter.with(',').trimResults().split("  a  ,  b  ,  c  ");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testStripResults_WithWhitespace() {
        List<String> result = Splitter.with(',').stripResults().split("  a  ,  b  ,  c  ");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testLimit_Basic() {
        List<String> result = Splitter.with(',').limit(2).split("a,b,c,d");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b,c,d", result.get(1));
    }

    @Test
    public void testSplitToCollection_WithOutput() {
        List<String> output = new ArrayList<>();
        output.add("existing");
        Splitter.with(',').split("a,b,c", output);
        assertEquals(Arrays.asList("existing", "a", "b", "c"), output);
    }

    @Test
    public void testSplitToCollectionWithClass_Output() {
        List<Integer> output = new ArrayList<>();
        output.add(0);
        Splitter.with(',').split("1,2,3", Integer.class, output);
        assertEquals(Arrays.asList(0, 1, 2, 3), output);
    }

    @Test
    public void testSplitToCollectionWithType_Output() {
        List<Integer> output = new ArrayList<>();
        Splitter.with(',').split("1,2,3", Type.of(Integer.class), output);
        assertEquals(Arrays.asList(1, 2, 3), output);
    }

    @Test
    public void testSplitWithMapper_ToUpperCase() {
        List<String> result = Splitter.with(',').split("hello,world", (Function<String, String>) String::toUpperCase);
        assertEquals(Arrays.asList("HELLO", "WORLD"), result);
    }

    @Test
    public void testSplitWithTypeAndSupplier_LinkedList() {
        List<Integer> result = Splitter.with(',').split("1,2,3", Type.of(Integer.class), (java.util.function.Supplier<LinkedList<Integer>>) LinkedList::new);
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(result instanceof LinkedList);
    }

    @Test
    public void testSplitToImmutableList_Empty() {
        ImmutableList<String> result = Splitter.with(',').splitToImmutableList("");
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    public void testSplitToImmutableListWithClass_Integers() {
        ImmutableList<Integer> result = Splitter.with(',').splitToImmutableList("1,2,3", Integer.class);
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testSplitToArrayWithMapper_ToUpperCase() {
        String[] result = Splitter.with(',').splitToArray("a,b,c", String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C" }, result);
    }

    @Test
    public void testSplitToStream_Count() {
        long count = Splitter.with(',').splitToStream("a,b,c,d,e").count();
        assertEquals(5, count);
    }

    @Test
    public void testSplitThenApply_Sum() {
        int sum = Splitter.with(',').splitThenApply("1,2,3", parts -> parts.stream().mapToInt(Integer::parseInt).sum());
        assertEquals(6, sum);
    }

    @Test
    public void testSplitThenAccept_SideEffect() {
        List<String> collected = new ArrayList<>();
        Splitter.with(',').splitThenAccept("a,b,c", collected::addAll);
        assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testSplitThenForEach_SideEffect() {
        List<String> collected = new ArrayList<>();
        Splitter.with(',').splitThenForEach("a,b,c", collected::add);
        assertEquals(Arrays.asList("a", "b", "c"), collected);
    }

    @Test
    public void testMapSplitterSplitToStream_Count() {
        long count = MapSplitter.with(",", "=").splitToStream("a=1,b=2,c=3").count();
        assertEquals(3, count);
    }

    @Test
    public void testMapSplitterSplitToEntryStream_MapCollect() {
        Map<String, String> result = MapSplitter.with(",", "=").splitToEntryStream("a=1,b=2").toMap();
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    @Test
    public void testMapSplitterSplitThenApply_Size() {
        int size = MapSplitter.with(",", "=").splitThenApply("a=1,b=2,c=3", Map::size);
        assertEquals(3, size);
    }

    @Test
    public void testMapSplitterSplitThenAccept_SideEffect() {
        Map<String, String> collected = new HashMap<>();
        MapSplitter.with(",", "=").splitThenAccept("a=1,b=2", collected::putAll);
        assertEquals("1", collected.get("a"));
        assertEquals("2", collected.get("b"));
    }

    @Test
    public void testSplitWithClassAndSupplier_HashSet() {
        Set<Integer> result = Splitter.with(',').split("1,2,1,3", Integer.class, Suppliers.ofSet());
        assertEquals(3, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(2));
        assertTrue(result.contains(3));
    }

    @Test
    public void testSplitToArrayWithOutput_SmallerArray() {
        String[] output = new String[2];
        Splitter.with(',').splitToArray("a,b,c,d", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
    }

    @Test
    public void testSplitToArrayWithClass_IntArray() {
        int[] result = Splitter.with(',').splitToArray("1,2,3", int[].class);
        assertArrayEquals(new int[] { 1, 2, 3 }, result);
    }

    @Test
    public void testMapSplitterSplitToImmutableMap_Basic() {
        ImmutableMap<String, String> result = MapSplitter.with(",", "=").splitToImmutableMap("a=1,b=2");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    @Test
    public void testMapSplitterSplitToImmutableMapWithClass_Typed() {
        ImmutableMap<String, Integer> result = MapSplitter.with(",", "=").splitToImmutableMap("a=1,b=2", String.class, Integer.class);
        assertEquals(Integer.valueOf(1), result.get("a"));
        assertEquals(Integer.valueOf(2), result.get("b"));
    }

    // --- Additional edge case and error path tests ---

    @Test
    public void testLimit_InvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(-1));
    }

    @Test
    public void testWithCharSequence_EmptyDelimiter() {
        // Empty string delimiter should throw
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(""));
    }

    @Test
    public void testWithPattern_NullDelimiter() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with((Pattern) null));
    }

    @Test
    public void testWithPattern_EmptyMatchingPattern() {
        // Pattern that can match empty string should throw
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile(".*")));
    }

    @Test
    public void testPattern_EmptyRegex() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
    }

    @Test
    public void testSplitToArray_NullArrayType() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("a,b", (Class<?>) null));
    }

    @Test
    public void testSplitToArray_NonArrayType() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("a,b", String.class));
    }

    @Test
    public void testSplitToArray_NullOutputArray() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("a,b", (String[]) null));
    }

    @Test
    public void testSplitToArray_EmptyOutputArray() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').splitToArray("a,b", new String[0]));
    }

    @Test
    public void testSplitToCollection_NullOutput() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').split("a,b", (List<String>) null));
    }

    @Test
    public void testSplitToCollectionWithClass_NullTargetType() {
        List<Object> output = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').split("a,b", (Class<Object>) null, output));
    }

    @Test
    public void testSplitToCollectionWithType_NullTargetType() {
        List<Object> output = new ArrayList<>();
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').split("a,b", (Type<Object>) null, output));
    }

    @Test
    public void testSplitWithClass_NullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').split("a,b", (Class<?>) null));
    }

    @Test
    public void testSplitWithType_NullTargetType() {
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').split("a,b", (Type<?>) null));
    }

    @Test
    public void testSplitToArray_DoubleArrayType() {
        double[] result = Splitter.with(',').splitToArray("1.5,2.5,3.5", double[].class);
        assertEquals(3, result.length);
        assertEquals(1.5, result[0], 0.001);
        assertEquals(2.5, result[1], 0.001);
        assertEquals(3.5, result[2], 0.001);
    }

    @Test
    public void testSplitToArray_LongArrayType() {
        long[] result = Splitter.with(',').splitToArray("100,200,300", long[].class);
        assertEquals(3, result.length);
        assertEquals(100L, result[0]);
        assertEquals(200L, result[1]);
        assertEquals(300L, result[2]);
    }

    @Test
    public void testSplitToArray_ObjectArrayType() {
        Object[] result = Splitter.with(',').splitToArray("a,b,c", Object[].class);
        assertEquals(3, result.length);
        assertEquals("a", result[0]);
        assertEquals("b", result[1]);
        assertEquals("c", result[2]);
    }

    @Test
    public void testSplitToArray_OutputArrayLargerThanResult() {
        String[] output = new String[5];
        output[3] = "unchanged";
        output[4] = "unchanged";
        Splitter.with(',').splitToArray("a,b,c", output);
        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
        assertEquals("unchanged", output[3]);
        assertEquals("unchanged", output[4]);
    }

    @Test
    public void testWithMultiCharDelimiter_AtBoundaries() {
        // delimiter at start
        List<String> result = Splitter.with("::").split("::a::b");
        assertEquals(3, result.size());
        assertEquals("", result.get(0));
        assertEquals("a", result.get(1));
        assertEquals("b", result.get(2));

        // delimiter at end
        result = Splitter.with("::").split("a::b::");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("", result.get(2));

        // only delimiter
        result = Splitter.with("::").split("::");
        assertEquals(2, result.size());
        assertEquals("", result.get(0));
        assertEquals("", result.get(1));
    }

    @Test
    public void testWithMultiCharDelimiter_PartialMatch() {
        // partial match of delimiter should not split
        List<String> result = Splitter.with("::").split("a:b::c");
        assertEquals(2, result.size());
        assertEquals("a:b", result.get(0));
        assertEquals("c", result.get(1));
    }

    @Test
    public void testWithMultiCharDelimiter_WithLimit() {
        List<String> result = Splitter.with("::").limit(2).split("a::b::c::d");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b::c::d", result.get(1));
    }

    @Test
    public void testWithMultiCharDelimiter_TrimAndOmitEmpty() {
        List<String> result = Splitter.with("::").trimResults().omitEmptyStrings().split(" a :: :: b :: ");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
    }

    @Test
    public void testMapSplitter_InvalidEntry_NoKeyValueDelimiter() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1,invalid,b=2"));
    }

    @Test
    public void testMapSplitter_NullInput() {
        Map<String, String> result = MapSplitter.with(",", "=").split((CharSequence) null);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapSplitter_LimitInvalid() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").limit(0));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").limit(-1));
    }

    @Test
    public void testMapSplitter_SplitToMap_NullOutput() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1", (Map<String, String>) null));
    }

    @Test
    public void testMapSplitter_SplitToMapWithTypes_NullOutput() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1", String.class, String.class, (Map<String, String>) null));
    }

    @Test
    public void testMapSplitter_SplitToMapWithTypeInstances_NullOutput() {
        assertThrows(IllegalArgumentException.class,
                () -> MapSplitter.with(",", "=").split("a=1", Type.of(String.class), Type.of(String.class), (Map<String, String>) null));
    }

    @Test
    public void testMapSplitter_SplitToMapWithTypes_NullKeyType() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1", (Class<String>) null, String.class));
    }

    @Test
    public void testMapSplitter_SplitToMapWithTypes_NullValueType() {
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1", String.class, (Class<String>) null));
    }

    @Test
    public void testSplitToStream_NullInput() {
        long count = Splitter.with(',').splitToStream(null).count();
        assertEquals(0, count);
    }

    @Test
    public void testSplitThenApply_NullInput() {
        List<String> result = Splitter.with(',').splitThenApply(null, list -> list);
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitThenForEach_EmptyInput() {
        List<String> collected = new ArrayList<>();
        Splitter.with(',').omitEmptyStrings().splitThenForEach("", s -> collected.add(s));
        assertTrue(collected.isEmpty());
    }

    @Test
    public void testMapSplitter_SplitToStream_NullInput() {
        long count = MapSplitter.with(",", "=").splitToStream(null).count();
        assertEquals(0, count);
    }

    @Test
    public void testMapSplitter_SplitToEntryStream_NullInput() {
        long count = MapSplitter.with(",", "=").splitToEntryStream(null).count();
        assertEquals(0, count);
    }

    @Test
    public void testMapSplitter_SplitThenApply_NullInput() {
        int size = MapSplitter.with(",", "=").splitThenApply(null, Map::size);
        assertEquals(0, size);
    }

    @Test
    public void testMapSplitter_SplitThenAccept_NullInput() {
        List<Integer> sizes = new ArrayList<>();
        MapSplitter.with(",", "=").splitThenAccept(null, map -> sizes.add(map.size()));
        assertEquals(1, sizes.size());
        assertEquals(0, sizes.get(0).intValue());
    }

    @Test
    public void testWithChar_EmptyInput() {
        List<String> result = Splitter.with(',').split("");
        assertEquals(1, result.size());
        assertEquals("", result.get(0));
    }

    @Test
    public void testWithChar_SingleElement() {
        List<String> result = Splitter.with(',').split("abc");
        assertEquals(1, result.size());
        assertEquals("abc", result.get(0));
    }

    @Test
    public void testWithPattern_LimitWithOmitEmpty() {
        List<String> result = Splitter.with(Pattern.compile(",")).omitEmptyStrings().limit(2).split(",,a,,b,,c");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals(",b,,c", result.get(1));
    }

    @Test
    public void testStripResults_TabsAndNewlines() {
        List<String> result = Splitter.with(',').stripResults().split("\ta\t,\nb\n,\r\nc\r\n");
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertEquals("b", result.get(1));
        assertEquals("c", result.get(2));
    }

    @Test
    public void testTrimResults_OnlyTrimsSpaces() {
        // trimResults only trims space chars, not tabs
        List<String> result = Splitter.with(',').trimResults().split(" a ,\tb\t");
        assertEquals(2, result.size());
        assertEquals("a", result.get(0));
        assertEquals("\tb\t", result.get(1));
    }

    @Test
    public void testMapSplitter_WithPatternDelimiters() {
        Map<String, String> result = MapSplitter.with(Pattern.compile("[,;]"), Pattern.compile("[=:]")).split("a=1,b:2;c=3");
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
    }

    @Test
    public void testMapSplitter_PatternFactory() {
        Map<String, String> result = MapSplitter.pattern("[,;]", "[=:]").split("a=1;b:2,c=3");
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
    }

    @Test
    public void testMapSplitter_WithDefault() {
        Map<String, String> result = MapSplitter.withDefault().split("a=1, b=2, c=3");
        assertEquals(3, result.size());
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
        assertEquals("3", result.get("c"));
    }

    @Test
    public void testSplitWithTypeSupplier_TreeSet() {
        java.util.TreeSet<String> result = Splitter.with(',').split("c,a,b", Type.of(String.class), Suppliers.ofTreeSet());
        assertEquals(3, result.size());
        assertEquals("a", result.first());
        assertEquals("c", result.last());
    }

    @Test
    public void testMapSplitter_SplitWithTypeSupplier() {
        TreeMap<String, String> result = MapSplitter.with(",", "=").split("z=3,a=1", Type.of(String.class), Type.of(String.class), Suppliers.ofTreeMap());
        assertEquals(2, result.size());
        assertEquals("a", result.firstKey());
    }

}
