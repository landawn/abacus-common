package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.join;
import static com.landawn.abacus.util.Strings.joinEntries;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class StringsJoinTest extends StringsTestSupport {

    @Test
    public void testJoin_PrimitiveArrays() {
        assertEquals("true, false, true", join(new boolean[] { true, false, true }));
        assertEquals("true:false:true", join(new boolean[] { true, false, true }, ":"));
        assertEquals("truefalsetrue", join(new boolean[] { true, false, true }, null));
        assertEquals("true,false,true", Strings.join(new boolean[] { true, false, true }, ","));
        assertEquals("", join(new boolean[] {}));
        assertEquals("", join((boolean[]) null));
        assertEquals("true", join(new boolean[] { true }));

        assertEquals("a, b, c", join(new char[] { 'a', 'b', 'c' }));
        assertEquals("a:b:c", join(new char[] { 'a', 'b', 'c' }, ":"));
        assertEquals("abc", join(new char[] { 'a', 'b', 'c' }, ""));
        assertEquals("a,b,c", Strings.join(new char[] { 'a', 'b', 'c' }, ","));
        assertEquals("", join((char[]) null));
        assertEquals("x", join(new char[] { 'x' }));

        assertEquals("1, 2, 3", join(new byte[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new byte[] { 1, 2, 3 }, ":"));
        assertEquals("123", join(new byte[] { 1, 2, 3 }, null));
        assertEquals("1,2,3", Strings.join(new byte[] { 1, 2, 3 }, ","));
        assertEquals("", join((byte[]) null));

        assertEquals("1, 2, 3", join(new short[] { 1, 2, 3 }));
        assertEquals("10,20,30", Strings.join(new short[] { 10, 20, 30 }, ","));
        assertEquals("", join((short[]) null));

        assertEquals("1, 2, 3", join(new int[] { 1, 2, 3 }));
        assertEquals("1:2:3", join(new int[] { 1, 2, 3 }, ":"));
        assertEquals("1|2|3", Strings.join(new int[] { 1, 2, 3 }, "|"));
        assertEquals("", Strings.join((int[]) null));

        assertEquals("1, 2, 3", join(new long[] { 1L, 2L, 3L }));
        assertEquals("100,200,300", Strings.join(new long[] { 100L, 200L, 300L }, ","));
        assertEquals("", join((long[]) null));

        assertEquals("1.0, 2.0, 3.0", join(new float[] { 1.0f, 2.0f, 3.0f }));
        assertEquals("1.5,2.5,3.5", Strings.join(new float[] { 1.5f, 2.5f, 3.5f }, ","));
        assertEquals("", join((float[]) null));

        assertEquals("1.0, 2.0, 3.0", join(new double[] { 1.0, 2.0, 3.0 }));
        assertEquals("1.1,2.2,3.3", Strings.join(new double[] { 1.1, 2.2, 3.3 }, ","));
        assertEquals("", join((double[]) null));
    }

    @Test
    public void testJoin_PrimitiveArrays_RangeAndPrefixSuffix() {
        assertEquals("false,true", join(new boolean[] { true, false, true }, 1, 3, ","));
        assertEquals("[true, false]", join(new boolean[] { true, false }, 0, 2, ", ", "[", "]"));
        assertEquals("[true, false]", join(new boolean[] { true, false }, ", ", "[", "]"));
        assertEquals("[true,false]", Strings.join(new boolean[] { true, false, true, false }, 0, 2, ",", "[", "]"));
        assertEquals("[]", join(new boolean[] {}, ", ", "[", "]"));
        assertEquals("[]", join((boolean[]) null, ", ", "[", "]"));
        assertEquals("prefix", join(new boolean[] {}, 0, 0, ", ", "prefix", null));
        assertEquals("suffix", join(new boolean[] {}, 0, 0, ", ", null, "suffix"));
        assertEquals("[truefalsetrue]", Strings.join(new boolean[] { true, false, true }, 0, 3, "", "[", "]"));
        assertEquals("[", Strings.join(new boolean[] { true, false, true }, 1, 1, ", ", "[", ""));
        assertEquals("]", Strings.join(new boolean[] { true, false, true }, 1, 1, ", ", "", "]"));

        assertEquals("b,c", join(new char[] { 'a', 'b', 'c' }, 1, 3, ","));
        assertEquals("[a, b]", join(new char[] { 'a', 'b' }, 0, 2, ", ", "[", "]"));
        assertEquals("[a, b, c]", join(new char[] { 'a', 'b', 'c' }, ", ", "[", "]"));
        assertEquals("[a,b,c]", Strings.join(new char[] { 'a', 'b', 'c', 'd' }, 0, 3, ",", "[", "]"));
        assertEquals("[abc]", Strings.join(new char[] { 'a', 'b', 'c' }, 0, 3, "", "[", "]"));
        assertEquals("[]", join((char[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new byte[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new byte[] { 1, 2, 3 }, ", ", "[", "]"));
        assertEquals("[2,3,4]", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, ",", "[", "]"));
        assertEquals("Bytes: 2 | 3 | 4", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 1, 4, " | ", "Bytes: ", ""));
        assertEquals("<12>", Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 0, 2, "", "<", ">"));
        assertEquals("[]", join((byte[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new short[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new short[] { 1, 2, 3 }, ", ", "[", "]"));
        assertEquals("[10,20]", Strings.join(new short[] { 10, 20, 30, 40 }, 0, 2, ",", "[", "]"));
        assertEquals("2-3-4", Strings.join(new short[] { 1, 2, 3, 4, 5 }, 1, 4, "-", "", ""));
        assertEquals("[]", join((short[]) null, ", ", "[", "]"));

        assertEquals("2,3", join(new int[] { 1, 2, 3 }, 1, 3, ","));
        assertEquals("[1, 2]", join(new int[] { 1, 2 }, 0, 2, ", ", "[", "]"));
        assertEquals("START:100:200:END", join(new int[] { 100, 200 }, ":", "START:", ":END"));
        assertEquals("{2+3+4}", Strings.join(new int[] { 1, 2, 3, 4, 5 }, 1, 4, "+", "{", "}"));
        assertEquals("]", Strings.join(new int[] { 1, 2, 3 }, 1, 1, ", ", "", "]"));
        assertEquals("", Strings.join((int[]) null, 0, 0, ", ", "", ""));

        assertEquals("2,3", join(new long[] { 1L, 2L, 3L }, 1, 3, ","));
        assertEquals("[1, 2, 3]", join(new long[] { 1L, 2L, 3L }, ", ", "[", "]"));
        assertEquals("<100-200>", Strings.join(new long[] { 100L, 200L, 300L, 400L }, 0, 2, "-", "<", ">"));
        assertEquals("200-300-400", Strings.join(new long[] { 100L, 200L, 300L, 400L, 500L }, 1, 4, "-", "", ""));
        assertEquals("[]", join((long[]) null, ", ", "[", "]"));

        assertEquals("2.0,3.0", join(new float[] { 1.0f, 2.0f, 3.0f }, 1, 3, ","));
        assertEquals("[1.0, 2.0, 3.0]", join(new float[] { 1.0f, 2.0f, 3.0f }, ", ", "[", "]"));
        assertEquals("[2.5,3.5]", Strings.join(new float[] { 1.5f, 2.5f, 3.5f }, 1, 3, ",", "[", "]"));
        assertEquals("]", Strings.join(new float[] { 1.1f, 2.2f, 3.3f }, 0, 0, ", ", "", "]"));
        assertEquals("[]", join((float[]) null, ", ", "[", "]"));

        assertEquals("2.0,3.0", join(new double[] { 1.0, 2.0, 3.0 }, 1, 3, ","));
        assertEquals("[1.0, 2.0, 3.0]", join(new double[] { 1.0, 2.0, 3.0 }, ", ", "[", "]"));
        assertEquals("[1.1,2.2]", Strings.join(new double[] { 1.1, 2.2, 3.3, 4.4 }, 0, 2, ",", "[", "]"));
        assertEquals("[", Strings.join((double[]) null, 0, 0, ", ", "[", ""));
        assertEquals("[]", join((double[]) null, ", ", "[", "]"));

        assertThrows(IndexOutOfBoundsException.class, () -> join(new boolean[] { true, false }, 0, 3, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new byte[] { 1, 2, 3, 4, 5 }, -1, 3, ", ", "", ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new byte[] { 1, 2, 3, 4, 5 }, 0, 10, ", ", "", ""));
        assertThrows(IndexOutOfBoundsException.class, () -> Strings.join(new short[] { 1, 2, 3, 4, 5 }, -1, 3, ", ", "", ""));
    }

    @Test
    public void testJoin_ObjectArray() {
        assertEquals("a, b, c", Strings.join(new Object[] { "a", "b", "c" }));
        assertEquals("a|b|c", Strings.join(new Object[] { "a", "b", "c" }, "|"));
        assertEquals("[a|b|c]", Strings.join(new Object[] { "a", "b", "c" }, "|", "[", "]"));
        assertEquals("a,b,c", Strings.join(new String[] { "a", "b", "c" }, ","));
        assertEquals("abc", Strings.join(new String[] { "a", "b", "c" }, ""));
        assertEquals("", Strings.join((String[]) null, ","));
        assertEquals("1, null, 3", join(new Object[] { 1, null, 3 }));
        assertEquals("a, null, b", Strings.join(new Object[] { "a", null, "b" }, ", "));
        assertEquals("", join(new Object[] {}));
        assertEquals("", join((Object[]) null));
        assertEquals("", Strings.join((Object[]) null, ", "));
        assertEquals("[]", Strings.join(new Object[0], ", ", "[", "]"));

        assertEquals("b,c", join(new Object[] { "a", "b", "c" }, 1, 3, ","));
        assertEquals("b, c", Strings.join(new Object[] { "a", "b", "c", "d" }, 1, 3, ", "));
        assertEquals("a, b, c", join(new Object[] { " a ", " b ", " c " }, ", ", "", "", true));
        assertEquals("b:c", join(new Object[] { " a ", " b ", " c " }, 1, 3, ":", true));
        assertEquals("[a, b]", join(new Object[] { " a ", " b " }, 0, 2, ", ", "[", "]", true));
        assertEquals("[anull3]", join(new Object[] { " a ", null, 3 }, 0, 3, null, "[", "]", true));
        assertEquals("[ a ,  b ,  c ]", Strings.join(new Object[] { " a ", " b ", " c " }, 0, 3, ", ", "[", "]", false));
        assertEquals("[a, b, c]", Strings.join(new Object[] { " a ", " b ", " c " }, 0, 3, ", ", "[", "]", true));
        assertEquals("[apple,banana,cherry]", Strings.join(new Object[] { " apple ", " banana ", " cherry " }, 0, 3, ",", "[", "]", true));
        assertEquals("{1-null-3}", join(new Object[] { 1, null, 3 }, 0, 3, "-", "{", "}"));
        assertEquals("[]", Strings.join((Object[]) null, 0, 0, ", ", "[", "]", false));
    }

    @Test
    public void testJoin_Iterable() {
        assertEquals("1, 2, 3", Strings.join(list(1, 2, 3)));
        assertEquals("1-2-3", Strings.join(list(1, 2, 3), "-"));
        assertEquals("a,b,c", Strings.join(Arrays.asList("a", "b", "c"), ","));
        assertEquals("", Strings.join(new ArrayList<>(), ","));
        assertEquals("", Strings.join((Iterable<String>) null, ","));
        assertEquals("a|b|c", Strings.join(Arrays.asList("a", "b", "c"), "|"));
        assertEquals("abc", Strings.join(Arrays.asList("a", "b", "c"), null));
        assertEquals("", join((Iterable<?>) null));
        assertEquals("[a, b]", join(Arrays.asList("a", "b"), ", ", "[", "]"));
        assertEquals("{1-2-3}", join(Arrays.asList(1, 2, 3), "-", "{", "}"));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), ", ", "", "", true));
        assertEquals("[a, b]", join(Arrays.asList(" a ", " b "), ", ", "[", "]", true));
        assertEquals("[x,y,z]", Strings.join(Arrays.asList("x", "y", "z"), ",", "[", "]"));
        assertEquals("[x,y]", Strings.join(Arrays.asList(" x ", " y "), ",", "[", "]", true));
    }

    @Test
    public void testJoin_Collection() {
        final List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("[a, b, c]", join(list, 0, 3, ", ", "[", "]", false));
        assertEquals("START:b:c:END", join(list, 1, 3, ":", "START:", ":END", false));
        assertEquals("b, c", Strings.join(list, 1, 3, ", "));
        assertEquals("b,c", join(list, 1, 3, ","));
        assertEquals("", join(list, 2, 2, ","));
        assertEquals("[b,c,d]", Strings.join(Arrays.asList("a", "b", "c", "d", "e"), 1, 4, ",", "[", "]"));
        assertEquals("[a,b,c]", Strings.join(Arrays.asList(" a ", " b ", " c "), 0, 3, ",", "[", "]", true));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c "), 0, 3, ", ", true));
        assertThrows(IndexOutOfBoundsException.class, () -> join(list, 0, 5, ","));

        final LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList(" a ", " b ", " c "));
        assertEquals("[ a ,  b ,  c ]", Strings.join(set, 0, 3, ", ", "[", "]", false));
        assertEquals("[a, b, c]", Strings.join(set, 0, 3, ", ", "[", "]", true));
        assertEquals("[a]", Strings.join(set, 0, 1, ", ", "[", "]", true));
        assertEquals("[]", Strings.join(set, 0, 0, ", ", "[", "]", false));
        assertEquals("[]", Strings.join((java.util.Collection<?>) null, 0, 0, ", ", "[", "]", false));
    }

    @Test
    public void testJoin_Iterator() {
        assertEquals("a, b, c", join(Arrays.asList("a", "b", "c").iterator()));
        assertEquals("a:b:c", join(Arrays.asList("a", "b", "c").iterator(), ":"));
        assertEquals("abc", join(Arrays.asList("a", "b", "c").iterator(), null));
        assertEquals("", join(Collections.emptyList().iterator()));
        assertEquals("", join((Iterator<?>) null));
        assertEquals("[a, b]", join(Arrays.asList("a", "b").iterator(), ", ", "[", "]"));
        assertEquals("[a, b, c]", Strings.join(Arrays.asList("a", "b", "c").iterator(), ", ", "[", "]"));
        assertEquals("a, b, c", join(Arrays.asList(" a ", " b ", " c ").iterator(), ", ", "", "", true));
        assertEquals("[a,b,c]", Strings.join(Arrays.asList(" a ", " b ", " c ").iterator(), ",", "[", "]", true));
        assertEquals("[a, b, c]", Strings.join(Arrays.asList("a", "b", "c").iterator(), ", ", "[", "]", false));
        assertEquals("[]", Strings.join((Iterator<?>) null, ", ", "[", "]", false));
        assertEquals("]", Strings.join((Iterator<?>) null, ", ", "", "]", false));
        assertEquals("[", Strings.join((Iterator<?>) null, ", ", "[", "", false));
        assertEquals("", Strings.join((Iterator<?>) null, ", ", "", "", false));
    }

    @Test
    public void testJoin_IteratorConvertsElementsAsConsumed() {
        final List<String> events = new ArrayList<>();
        final Iterator<Object> iter = new Iterator<>() {
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < 2;
            }

            @Override
            public Object next() {
                final int current = index++;
                events.add("next" + current);
                return new Object() {
                    @Override
                    public String toString() {
                        events.add("string" + current);
                        return Integer.toString(current);
                    }
                };
            }
        };

        assertEquals("[0,1]", Strings.join(iter, ",", "[", "]", false));
        assertEquals(List.of("next0", "string0", "next1", "string1"), events);
    }

    @Test
    public void testJoin_BufferCapacityHintsAreBoundedAndDelimiterAware() {
        assertEquals(16, Strings.calculateBufferSize(1, 16, Integer.MAX_VALUE, 0, 0));
        assertEquals(32, Strings.calculateBufferSize(2, 10, 7, 2, 3));
        assertEquals(1024 * 1024, Strings.calculateBufferSize(Integer.MAX_VALUE, 16, 0, 4, 0));
        assertEquals(1024 * 1024, Strings.calculateBufferSize(Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE));

        final String unusedLargeDelimiter = "x".repeat(1_000_000);
        assertEquals("value", Strings.join(List.of("value").iterator(), unusedLargeDelimiter));
        assertEquals("value", Strings.join(List.of("value"), 0, 1, unusedLargeDelimiter));
    }

    @Test
    public void testJoin_ListRangeStartsAtRequestedIndex() {
        final int[] getCount = { 0 };
        final List<Integer> list = new java.util.AbstractList<>() {
            @Override
            public Integer get(final int index) {
                getCount[0]++;
                return index;
            }

            @Override
            public int size() {
                return 1000;
            }
        };

        assertEquals("[998,999]", Strings.join(list, 998, 1000, ",", "[", "]", false));
        assertEquals(2, getCount[0]);
    }

    @Test
    public void testJoinEntries() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1, b=2", Strings.joinEntries(map));
        assertEquals("a:1;b:2", Strings.joinEntries(map, ";", ":"));
        assertEquals("{a:1;b:2}", Strings.joinEntries(map, ";", ":", "{", "}"));
        assertEquals("a=1;b=2", joinEntries(map, ";"));
        assertEquals("", joinEntries(Collections.emptyMap()));
        assertEquals("", joinEntries((Map<?, ?>) null));
        assertEquals("a->1, b->2", joinEntries(map, ", ", "->"));
        assertEquals("{a=1, b=2}", joinEntries(map, ", ", "=", "{", "}"));

        final Map<String, String> ranged = new LinkedHashMap<>();
        ranged.put("a", "1");
        ranged.put("b", "2");
        ranged.put("c", "3");
        assertEquals("a=1", Strings.joinEntries(ranged, 0, 1, ", ", "="));
        assertEquals("b=2", joinEntries(toIntMap(), 1, 2, ","));
        assertEquals("b:2", joinEntries(toIntMap(), 1, 2, ",", ":"));
        assertEquals("b=2, c=3", joinEntries(toIntMap(), 1, 3, ", "));
        assertEquals("[b:2|c:3]", joinEntries(toIntMap(), 1, 3, "|", ":", "[", "]", false));
        assertEquals("", joinEntries(toIntMap(), 1, 1, ","));
        assertThrows(IndexOutOfBoundsException.class, () -> joinEntries(toIntMap(), 0, 4, ","));

        final Map<String, String> spaced = new LinkedHashMap<>();
        spaced.put(" a ", " 1 ");
        spaced.put(" b ", " 2 ");
        assertEquals("a=1, b=2", joinEntries(spaced, ", ", "=", "", "", true));
        assertEquals("{a:1|b:2}", joinEntries(spaced, "|", ":", "{", "}", true));

        final LinkedHashMap<String, Integer> fromTo = new LinkedHashMap<>();
        fromTo.put("a", 1);
        fromTo.put("b", 2);
        fromTo.put("c", 3);
        fromTo.put("d", 4);
        final String sliced = Strings.joinEntries(fromTo, 1, 3, ",", "=", "[", "]", false);
        assertTrue(sliced.contains("b=2"));
        assertTrue(sliced.contains("c=3"));
        assertFalse(sliced.contains("a=1"));

        final LinkedHashMap<String, String> trimMap = new LinkedHashMap<>();
        trimMap.put(" id ", " 1 ");
        trimMap.put(" name ", " Alice ");
        trimMap.put(" role ", " Admin ");
        assertEquals("{id:1, name:Alice}", Strings.joinEntries(trimMap, 0, 2, ", ", ":", "{", "}", true));
        assertEquals("{ id : 1 ,  name : Alice }", Strings.joinEntries(trimMap, 0, 2, ", ", ":", "{", "}", false));
        assertEquals("{}", Strings.joinEntries(trimMap, 1, 1, ", ", ":", "{", "}", false));
        assertEquals("{}", Strings.joinEntries((Map<?, ?>) null, 0, 0, ", ", ":", "{", "}", false));
        assertEquals("{a=1, b=2}", Strings.joinEntries(ranged, 0, 2, ", ", "=", "{", "}"));

        final LinkedHashMap<Object, Object> nullEntry = new LinkedHashMap<>();
        nullEntry.put(null, null);
        assertEquals("null=null", Strings.joinEntries(nullEntry));
    }

    @Test
    public void testJoinEntries_Extractors() {
        final List<Map.Entry<String, Integer>> entries = Arrays.asList(new AbstractMap.SimpleEntry<>(" name ", 1), new AbstractMap.SimpleEntry<>(" age ", 25));
        assertEquals("[ name =1,  age =25]", Strings.joinEntries(entries, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[name=1, age=25]", Strings.joinEntries(entries, ", ", "=", "[", "]", true, Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[]",
                Strings.joinEntries(new ArrayList<Map.Entry<String, Integer>>(), ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        final List<Map.Entry<String, String>> withNulls = Arrays.asList(new AbstractMap.SimpleEntry<>("key1", null),
                new AbstractMap.SimpleEntry<>(null, "value2"), new AbstractMap.SimpleEntry<>("key3", "value3"));
        assertEquals("key1=null, null=value2, key3=value3", Strings.joinEntries(withNulls, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));

        final List<Map.Entry<String, String>> single = Arrays.asList(new AbstractMap.SimpleEntry<>("key", "value"));
        assertEquals("key=value", Strings.joinEntries(single, ", ", "=", Map.Entry::getKey, Map.Entry::getValue));
        assertEquals("[key=value]", Strings.joinEntries(single, ", ", "=", "[", "]", false, Map.Entry::getKey, Map.Entry::getValue));

        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("key1", 100);
        map.put("key2", 200);
        final Function<Map.Entry<String, Integer>, String> keyExtractor = e -> e.getKey().toUpperCase();
        final Function<Map.Entry<String, Integer>, String> valueExtractor = e -> String.valueOf(e.getValue() * 2);
        assertEquals("KEY1=200, KEY2=400", joinEntries(map.entrySet(), ", ", "=", "", "", false, keyExtractor, valueExtractor));

        final List<String[]> pairs = Arrays.asList(new String[] { "key1", "val1" }, new String[] { "key2", "val2" });
        assertEquals("[key1=val1, key2=val2]", Strings.<String[]> joinEntries(pairs, ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));
        assertEquals("[k1=v1]",
                Strings.<String[]> joinEntries(Collections.singletonList(new String[] { " k1 ", " v1 " }), ", ", "=", "[", "]", true, e -> e[0], e -> e[1]));
        assertEquals("[]", Strings.<String[]> joinEntries((List<String[]>) null, ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));
        assertEquals("[null=null]",
                Strings.<String[]> joinEntries(Collections.singletonList(new String[] { null, null }), ", ", "=", "[", "]", false, e -> e[0], e -> e[1]));

        final Iterable<String[]> nullIterable = null;
        final Function<String[], Object> extractor = entry -> entry[0];
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", null, extractor));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", extractor, null));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", "[", "]", false, null, extractor));
        assertThrows(IllegalArgumentException.class, () -> Strings.<String[]> joinEntries(nullIterable, ", ", "=", "[", "]", false, extractor, null));
    }

    @Test
    public void testJoinEntries_nullDelimitersTreatedAsEmpty() {
        final Map<String, Integer> m = new LinkedHashMap<>();
        m.put("a", 1);
        m.put("b", 2);
        assertEquals("a=1b=2", Strings.joinEntries(m, (String) null));
        assertEquals("a1, b2", Strings.joinEntries(m, ", ", (String) null));
        assertEquals("a=1, b=2", Strings.joinEntries(m, ", "));
    }

    private static Map<String, Integer> toIntMap() {
        final Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        return map;
    }
}
