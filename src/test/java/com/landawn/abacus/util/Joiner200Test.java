package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

import lombok.Data;

@Tag("new-test")
public class Joiner200Test extends TestBase {

    @Data
    public static class TestBean {
        private String name = "test";
        private int value = 123;
        private String nullField = null;
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
    public void testSkipNulls() {
        assertEquals("a, c", Joiner.with(", ").skipNulls().append("a").append((String) null).append("c").toString());
        assertEquals("a, c", Joiner.with(", ").skipNulls().appendAll(Arrays.asList("a", (String) null, "c")).toString());
        assertEquals("a, null, c", Joiner.with(", ").append("a").append((String) null).append("c").toString());
    }

    @Test
    public void testUseForNull() {
        assertEquals("a, NA, c", Joiner.with(", ").useForNull("NA").append("a").append((String) null).append("c").toString());
        assertEquals("a, null, c", Joiner.with(", ").append("a").append((String) null).append("c").toString());
    }

    @Test
    public void testTrimBeforeAppend() {
        assertEquals("a, b, c", Joiner.with(", ").trimBeforeAppend().append(" a ").append("b").append(" c").toString());
        assertEquals("a,   b ,  c ", Joiner.with(", ").append("a").append("  b ").append(" c ").toString());
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
    public void testAppendEntriesMap() {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertEquals("a=1&b=2", Joiner.with("&").appendEntries(map).toString());
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
    public void testAppendIf() {
        assertEquals("hello", Joiner.with("").appendIf(true, () -> "hello").toString());
        assertEquals("", Joiner.with("").appendIf(false, () -> "hello").toString());
        assertEquals("a, c", Joiner.with(", ").append("a").appendIf(false, () -> "b").append("c").toString());
    }

    @Test
    public void testRepeat() {
        assertEquals("a, a, a", Joiner.with(", ").repeat("a", 3).toString());
        assertEquals("ha", Joiner.with("").repeat("ha", 1).toString());
        assertEquals("", Joiner.with(", ").repeat("a", 0).toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(", ").repeat("a", -1));
    }

    @Test
    public void testMerge() {
        Joiner j1 = Joiner.with(", ").append("a").append("b");
        Joiner j2 = Joiner.with("|").append("c").append("d");
        assertEquals("a, b, c|d", j1.merge(j2).toString());

        Joiner j3 = Joiner.with(", ", "[", "]").append("a");
        Joiner j4 = Joiner.with(":", "{", "}").append("b");
        assertEquals("[a, b]", j3.merge(j4).toString());
    }

    @Test
    public void testLength() {
        assertEquals(2, Joiner.with(", ", "[", "]").length());
        assertEquals(4, Joiner.with(", ").append("a").append("b").length());
        assertEquals(6, Joiner.with(", ", "[", "]").append("a").append("b").length());
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();
        Joiner.with(", ").append("a").append("b").appendTo(writer);
        assertEquals("a, b", writer.toString());

        StringWriter writer2 = new StringWriter();
        Joiner.with(", ", "[", "]").append("x").appendTo(writer2);
        assertEquals("[x]", writer2.toString());
    }

    @Test
    public void testReuseCachedBuffer() {
        Joiner joiner = Joiner.with(", ").reuseBuffer();
        joiner.append("a").append("b");
        assertEquals("a, b", joiner.toString());

        joiner.append("c").append("d");
        assertEquals("a, b, c, d", joiner.toString());

        Joiner joiner2 = Joiner.with(", ").reuseBuffer();
        joiner2.append("x");
        joiner2.close();
        assertThrows(IllegalStateException.class, () -> joiner2.append("y"));
    }

    @Test
    public void testReuseCachedBufferThrowsException() {
        Joiner joiner = Joiner.with(", ").append("a");
        assertThrows(IllegalStateException.class, joiner::reuseBuffer);
    }

    @Test
    public void testMap() {
        String result = Joiner.with("-").append("a").append("b").map(s -> "Result: " + s);
        assertEquals("Result: a-b", result);

        Integer length = Joiner.with("").append("123").map(String::length);
        assertEquals(3, length.intValue());
    }

    @Test
    public void testClose() {
        Joiner joiner = Joiner.with(",");
        joiner.append("a");
        joiner.close();
        assertThrows(IllegalStateException.class, () -> joiner.append("b"));
    }

    @Test
    public void testDefaultJoiner() {
        assertEquals("a, b", Joiner.defauLt().append("a").append("b").toString());
        assertEquals("k=v", Joiner.defauLt().appendEntry("k", "v").toString());
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
    public void testAppendAllIterator() {
        List<String> list = Arrays.asList("a", "b", null, "c");
        assertEquals("a-b-null-c", Joiner.with("-").appendAll(list.iterator()).toString());
        assertEquals("a-b-c", Joiner.with("-").skipNulls().appendAll(list.iterator()).toString());
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
        Set<String> ignored = Set.of("active", "nullField");
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
}
