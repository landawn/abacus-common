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

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Fn.Suppliers;

public class Splitter100Test extends TestBase {

    // Test defauLt() method
    @Test
    public void testDefault() {
        Splitter splitter = Splitter.defauLt();
        List<String> result = splitter.split("a, b, c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // Test forLines() method
    @Test
    public void testForLines() {
        Splitter splitter = Splitter.forLines();
        List<String> result = splitter.split("line1\nline2\rline3\r\nline4");
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), result);
    }

    // Test with(char) method
    @Test
    public void testWithChar() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with empty string
        result = splitter.split("");
        assertEquals(Arrays.asList(""), result);

        // Test with null
        result = splitter.split(null);
        assertTrue(result.isEmpty());

        // Test with single character
        result = splitter.split("a");
        assertEquals(Arrays.asList("a"), result);

        // Test with delimiter at start
        result = splitter.split(",a,b");
        assertEquals(Arrays.asList("", "a", "b"), result);

        // Test with delimiter at end
        result = splitter.split("a,b,");
        assertEquals(Arrays.asList("a", "b", ""), result);

        // Test with consecutive delimiters
        result = splitter.split("a,,b");
        assertEquals(Arrays.asList("a", "", "b"), result);
    }

    // Test with(CharSequence) method
    @Test
    public void testWithCharSequence() {
        Splitter splitter = Splitter.with("::");
        List<String> result = splitter.split("a::b::c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test single char delegation
        splitter = Splitter.with(":");
        result = splitter.split("a:b:c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with empty delimiter (should use whitespace pattern)
        splitter = Splitter.with(" ").trimResults().omitEmptyStrings();
        result = splitter.split("a b  c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test IllegalArgumentException for null
        assertThrows(IllegalArgumentException.class, () -> Splitter.with((CharSequence) null));
    }

    // Test with(Pattern) method
    @Test
    public void testWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        Splitter splitter = Splitter.with(pattern);
        List<String> result = splitter.split("a  b\tc\nd");
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        // Test IllegalArgumentException for null
        assertThrows(IllegalArgumentException.class, () -> Splitter.with((Pattern) null));

        // Test IllegalArgumentException for pattern matching empty string
        Pattern emptyPattern = Pattern.compile(".*");
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(emptyPattern));
    }

    // Test pattern(CharSequence) method
    @Test
    public void testPatternCharSequence() {
        Splitter splitter = Splitter.pattern("\\d+");
        List<String> result = splitter.split("a123b456c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test IllegalArgumentException for null
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(null));

        // Test IllegalArgumentException for empty
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
    }

    // Test omitEmptyStrings() method
    @Test
    public void testOmitEmptyStrings() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings();
        List<String> result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "b"), result);

        // Test with all empty strings
        result = splitter.split(",,,");
        assertTrue(result.isEmpty());
    }

    // Test deprecated omitEmptyStrings(boolean) method
    @Test
    public void testOmitEmptyStringsBoolean() {
        Splitter splitter = Splitter.with(',').omitEmptyStrings(true);
        List<String> result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "b"), result);

        splitter = Splitter.with(',').omitEmptyStrings(false);
        result = splitter.split("a,,b,");
        assertEquals(Arrays.asList("a", "", "b", ""), result);
    }

    // Test trimResults() method
    @Test
    public void testTrimResults() {
        Splitter splitter = Splitter.with(',').trimResults();
        List<String> result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with spaces only
        result = splitter.split("  ,  ,  ");
        assertEquals(Arrays.asList("", "", ""), result);
    }

    // Test deprecated trim(boolean) method
    @Test
    public void testTrimBoolean() {
        Splitter splitter = Splitter.with(',').trim(true);
        List<String> result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').trim(false);
        result = splitter.split("a , b , c ");
        assertEquals(Arrays.asList("a ", " b ", " c "), result);
    }

    // Test stripResults() method
    @Test
    public void testStripResults() {
        Splitter splitter = Splitter.with(',').stripResults();
        List<String> result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test with whitespace only
        result = splitter.split("\t\n,\r\n,\t ");
        assertEquals(Arrays.asList("", "", ""), result);
    }

    // Test deprecated strip(boolean) method
    @Test
    public void testStripBoolean() {
        Splitter splitter = Splitter.with(',').strip(true);
        List<String> result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        splitter = Splitter.with(',').strip(false);
        result = splitter.split("a\t,\nb\t,\tc\n");
        assertEquals(Arrays.asList("a\t", "\nb\t", "\tc\n"), result);
    }

    // Test limit() method
    @Test
    public void testLimit() {
        Splitter splitter = Splitter.with(',').limit(2);
        List<String> result = splitter.split("a,b,c,d");
        assertEquals(Arrays.asList("a", "b,c,d"), result);

        splitter = Splitter.with(',').limit(1);
        result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a,b,c"), result);

        // Test IllegalArgumentException for non-positive limit
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(-1));
    }

    // Test split(CharSequence) method
    @Test
    public void testSplit() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.split("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test null input
        result = splitter.split(null);
        assertTrue(result.isEmpty());
    }

    // Test split(CharSequence, Supplier) method
    @Test
    public void testSplitWithSupplier() {
        Splitter splitter = Splitter.with(',');
        LinkedList<String> result = splitter.split("a,b,c", Suppliers.ofLinkedList());
        assertEquals(Arrays.asList("a", "b", "c"), result);
        assertTrue(result instanceof LinkedList);
    }

    // Test split(CharSequence, Function) method
    @Test
    public void testSplitWithMapper() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Fn.f(e -> Integer.parseInt(e)));
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // Test split(CharSequence, Class) method
    @Test
    public void testSplitWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> result = splitter.split("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test IllegalArgumentException for null targetType
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Class<?>) null));
    }

    // Test split(CharSequence, Class, Supplier) method
    @Test
    public void testSplitWithTargetClassAndSupplier() {
        Splitter splitter = Splitter.with(',');
        Set<Integer> result = splitter.split("1,2,3", Integer.class, Suppliers.ofSet());
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result);
    }

    // Test split(CharSequence, Type) method
    @Test
    public void testSplitWithType() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = N.typeOf(Integer.class);
        List<Integer> result = splitter.split("1,2,3", intType);
        assertEquals(Arrays.asList(1, 2, 3), result);

        // Test IllegalArgumentException for null targetType
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Type<?>) null));
    }

    // Test split(CharSequence, Type, Supplier) method
    @Test
    public void testSplitWithTypeAndSupplier() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = N.typeOf(Integer.class);
        Vector<Integer> result = splitter.split("1,2,3", intType, () -> new Vector<>());
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(result instanceof Vector);
    }

    // Test split(CharSequence, Collection) method
    @Test
    public void testSplitIntoCollection() {
        Splitter splitter = Splitter.with(',');
        List<String> output = new ArrayList<>();
        splitter.split("a,b,c", output);
        assertEquals(Arrays.asList("a", "b", "c"), output);

        // Test IllegalArgumentException for null output
        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Collection<String>) null));
    }

    // Test split(CharSequence, Class, Collection) method
    @Test
    public void testSplitIntoCollectionWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", Integer.class, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        // Test IllegalArgumentException for null targetType
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Class) null, output));

        // Test IllegalArgumentException for null output
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", Integer.class, (Collection<Integer>) null));
    }

    // Test split(CharSequence, Type, Collection) method
    @Test
    public void testSplitIntoCollectionWithType() {
        Splitter splitter = Splitter.with(',');
        Type<Integer> intType = N.typeOf(Integer.class);
        List<Integer> output = new ArrayList<>();
        splitter.split("1,2,3", intType, output);
        assertEquals(Arrays.asList(1, 2, 3), output);

        // Test IllegalArgumentException for null targetType
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", (Type<Integer>) null, output));

        // Test IllegalArgumentException for null output
        assertThrows(IllegalArgumentException.class, () -> splitter.split("1,2,3", intType, (Collection<Integer>) null));
    }

    // Test splitToImmutableList(CharSequence) method
    @Test
    public void testSplitToImmutableList() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<String> result = splitter.splitToImmutableList("a,b,c");
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    // Test splitToImmutableList(CharSequence, Class) method
    @Test
    public void testSplitToImmutableListWithTargetClass() {
        Splitter splitter = Splitter.with(',');
        ImmutableList<Integer> result = splitter.splitToImmutableList("1,2,3", Integer.class);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    // Test splitToArray(CharSequence) method
    @Test
    public void testSplitToArray() {
        Splitter splitter = Splitter.with(',');
        String[] result = splitter.splitToArray("a,b,c");
        assertArrayEquals(new String[] { "a", "b", "c" }, result);

        // Test empty input
        result = splitter.splitToArray("");
        assertArrayEquals(new String[] { "" }, result);

        // Test null input
        result = splitter.splitToArray(null);
        assertArrayEquals(new String[] {}, result);
    }

    // Test splitToArray(CharSequence, Function) method
    @Test
    public void testSplitToArrayWithMapper() {
        Splitter splitter = Splitter.with(',');
        String[] result = splitter.splitToArray("a,b,c", String::toUpperCase);
        assertArrayEquals(new String[] { "A", "B", "C" }, result);
    }

    // Test splitToArray(CharSequence, Class) method
    @Test
    public void testSplitToArrayWithArrayType() {
        Splitter splitter = Splitter.with(',');

        // Test String array
        String[] stringResult = splitter.splitToArray("a,b,c", String[].class);
        assertArrayEquals(new String[] { "a", "b", "c" }, stringResult);

        // Test Integer array
        Integer[] intResult = splitter.splitToArray("1,2,3", Integer[].class);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, intResult);

        // Test primitive int array
        int[] primitiveResult = splitter.splitToArray("1,2,3", int[].class);
        assertArrayEquals(new int[] { 1, 2, 3 }, primitiveResult);

        // Test IllegalArgumentException for null arrayType
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (Class<?>) null));
    }

    // Test splitToArray(CharSequence, String[]) method
    @Test
    public void testSplitToArrayIntoExisting() {
        Splitter splitter = Splitter.with(',');
        String[] output = new String[5];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b", "c", null, null }, output);

        // Test with smaller array
        output = new String[2];
        splitter.splitToArray("a,b,c", output);
        assertArrayEquals(new String[] { "a", "b" }, output);

        // Test IllegalArgumentException for null output
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (String[]) null));

        // Test IllegalArgumentException for empty output
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", new String[0]));
    }

    // Test splitToStream(CharSequence) method
    @Test
    public void testSplitToStream() {
        Splitter splitter = Splitter.with(',');
        List<String> result = splitter.splitToStream("a,b,c").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        // Test stream operations
        long count = splitter.splitToStream("a,b,c,d,e").filter(s -> s.compareTo("c") > 0).count();
        assertEquals(2, count);
    }

    // Test splitThenApply(CharSequence, Function) method
    @Test
    public void testSplitThenApply() {
        Splitter splitter = Splitter.with(',');
        int result = splitter.splitThenApply("a,b,c", List::size);
        assertEquals(3, result);

        String joined = splitter.splitThenApply("a,b,c", list -> String.join("-", list));
        assertEquals("a-b-c", joined);
    }

    // Test splitThenAccept(CharSequence, Consumer) method
    @Test
    public void testSplitThenAccept() {
        Splitter splitter = Splitter.with(',');
        List<String> captured = new ArrayList<>();
        splitter.splitThenAccept("a,b,c", captured::addAll);
        assertEquals(Arrays.asList("a", "b", "c"), captured);
    }

    // Test splitAndForEach(CharSequence, Consumer) method
    @Test
    public void testSplitAndForEach() {
        Splitter splitter = Splitter.with(',');
        List<String> captured = new ArrayList<>();
        splitter.splitAndForEach("a,b,c", captured::add);
        assertEquals(Arrays.asList("a", "b", "c"), captured);
    }

    // Test combination of options
    @Test
    public void testCombinedOptions() {
        Splitter splitter = Splitter.with(',').trimResults().omitEmptyStrings().limit(3);

        List<String> result = splitter.split(" a , , b , c , d ");
        assertEquals(Arrays.asList("a", "b", "c , d"), result);
    }

    // Test MapSplitter.defauLt() method
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

    // Test MapSplitter.with(CharSequence, CharSequence) method
    @Test
    public void testMapSplitterWithCharSequence() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(";", ":");
        Map<String, String> result = mapSplitter.split("a:1;b:2;c:3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        // Test IllegalArgumentException for null or empty delimiters
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(null, ":"));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with("", ":"));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(";", null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(";", ""));
    }

    // Test MapSplitter.with(Pattern, Pattern) method
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

    // Test MapSplitter.pattern(CharSequence, CharSequence) method
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

    // Test MapSplitter.omitEmptyStrings() method
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

    // Test MapSplitter.trimResults() method
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

    // Test MapSplitter.stripResults() method
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

    // Test MapSplitter.limit() method
    @Test
    public void testMapSplitterLimit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=").limit(2);
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3,d=4");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2,c=3,d=4");
        assertEquals(expected, result);

        // Test IllegalArgumentException for non-positive limit
        assertThrows(IllegalArgumentException.class, () -> Splitter.MapSplitter.with(",", "=").limit(0));
    }

    // Test MapSplitter.split(CharSequence) method
    @Test
    public void testMapSplitterSplit() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, String> result = mapSplitter.split("a=1,b=2,c=3");
        Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, result);

        // Test null input
        result = mapSplitter.split(null);
        assertTrue(result.isEmpty());

        // Test invalid format
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1,b"));
        // assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1=2"));

        assertEquals(N.asMap("a", "1=2"), mapSplitter.split("a=1=2"));
    }

    // Test MapSplitter.split(CharSequence, Supplier) method
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

    // Test MapSplitter.split(CharSequence, Class, Class) method
    @Test
    public void testMapSplitterSplitWithKeyValueTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", String.class, Integer.class);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);

        // Test IllegalArgumentException for null types
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, Integer.class));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null));
    }

    // Test MapSplitter.split(CharSequence, Type, Type) method
    @Test
    public void testMapSplitterSplitWithTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = N.typeOf(String.class);
        Type<Integer> intType = N.typeOf(Integer.class);
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", stringType, intType);
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);

        // Test IllegalArgumentException for null types
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, intType));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, null));
    }

    // Test MapSplitter.split(CharSequence, Class, Class, Supplier) method
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

    // Test MapSplitter.split(CharSequence, Type, Type, Supplier) method
    @Test
    public void testMapSplitterSplitWithTypesAndSupplier() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = N.typeOf(String.class);
        Type<Integer> intType = N.typeOf(Integer.class);
        Map<String, Integer> result = mapSplitter.split("a=1,b=2,c=3", stringType, intType, Suppliers.ofLinkedHashMap());
        Map<String, Integer> expected = new LinkedHashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, result);
        assertTrue(result instanceof LinkedHashMap);
    }

    // Test MapSplitter.split(CharSequence, Map) method
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

        // Test IllegalArgumentException for null output
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Map<String, String>) null));
    }

    // Test MapSplitter.split(CharSequence, Class, Class, Map) method
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

        // Test IllegalArgumentException for null types or output
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, Integer.class, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, Integer.class, (Map<String, Integer>) null));
    }

    // Test MapSplitter.split(CharSequence, Type, Type, Map) method
    @Test
    public void testMapSplitterSplitIntoMapWithTypes() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        Type<String> stringType = N.typeOf(String.class);
        Type<Integer> intType = N.typeOf(Integer.class);
        Map<String, Integer> output = new HashMap<>();
        mapSplitter.split("a=1,b=2,c=3", stringType, intType, output);
        Map<String, Integer> expected = new HashMap<>();
        expected.put("a", 1);
        expected.put("b", 2);
        expected.put("c", 3);
        assertEquals(expected, output);

        // Test IllegalArgumentException for null types or output
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, intType, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, null, output));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", stringType, intType, (Map<String, Integer>) null));
    }

    // Test MapSplitter.splitToImmutableMap(CharSequence) method
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

    // Test MapSplitter.splitToImmutableMap(CharSequence, Class, Class) method
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

    // Test MapSplitter.splitToStream(CharSequence) method
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

        // Test invalid format in stream
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitToStream("a=1,b").toList());
    }

    // Test MapSplitter.splitToEntryStream(CharSequence) method
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

    // Test MapSplitter.splitThenApply(CharSequence, Function) method
    @Test
    public void testMapSplitterSplitThenApply() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");
        int size = mapSplitter.splitThenApply("a=1,b=2,c=3", Map::size);
        assertEquals(3, size);

        String firstKey = mapSplitter.splitThenApply("a=1,b=2", map -> map.keySet().iterator().next());
        assertEquals("a", firstKey);
    }

    // Test MapSplitter.splitThenAccept(CharSequence, Consumer) method
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

    // Test deprecated MapSplitter methods
    @Test
    public void testMapSplitterDeprecatedMethods() {
        Splitter.MapSplitter mapSplitter = Splitter.MapSplitter.with(",", "=");

        // Test deprecated omitEmptyStrings(boolean)
        Map<String, String> result = mapSplitter.omitEmptyStrings(true).split("a=1,,b=2");
        assertEquals(2, result.size());

        // Test deprecated trim(boolean)
        result = mapSplitter.trim(true).split(" a = 1 , b = 2 ");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));

        // Test deprecated strip(boolean)
        result = mapSplitter.strip(true).split("\ta\t=\t1\t,\tb\t=\t2\t");
        assertEquals("1", result.get("a"));
        assertEquals("2", result.get("b"));
    }

    // Test edge cases and error conditions
    @Test
    public void testEdgeCases() {
        // Test empty string with various configurations
        Splitter splitter = Splitter.with(',');
        assertEquals(Arrays.asList(""), splitter.split(""));

        splitter = Splitter.with(',').omitEmptyStrings();
        assertTrue(splitter.split("").isEmpty());

        // Test single delimiter
        splitter = Splitter.with(',');
        assertEquals(Arrays.asList("", ""), splitter.split(","));

        // Test whitespace pattern edge case
        splitter = Splitter.with(Splitter.WHITE_SPACE_PATTERN);
        assertEquals(Arrays.asList("a", "b", "c"), splitter.split("a  b\t\nc"));

        // Test limit edge cases
        splitter = Splitter.with(',').limit(Integer.MAX_VALUE);
        assertEquals(Arrays.asList("a", "b", "c"), splitter.split("a,b,c"));
    }

    // Test iterator behavior
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

        // Test NoSuchElementException
        assertThrows(NoSuchElementException.class, iter::next);
    }
}