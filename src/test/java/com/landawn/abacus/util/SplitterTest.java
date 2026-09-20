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
import java.util.function.Function;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Splitter.MapSplitter;
import com.landawn.abacus.util.stream.Stream;

public class SplitterTest extends AbstractTest {

    @Test
    public void testWithDefault() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.withDefault().split("a, b, c"));
        assertTrue(Splitter.withDefault().split(null).isEmpty());

        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, MapSplitter.withDefault().split("a=1, b=2, c=3"));
    }

    @Test
    public void testForLines() {
        final Splitter splitter = Splitter.forLines();
        assertEquals(Arrays.asList("line1", "line2", "line3"), splitter.split("line1\nline2\nline3"));
        assertEquals(Arrays.asList("line1", "line2", "line3"), splitter.split("line1\r\nline2\r\nline3"));
        assertEquals(Arrays.asList("line1", "line2", "line3"), splitter.split("line1\rline2\rline3"));
        assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), splitter.split("line1\nline2\r\nline3\rline4"));
        assertTrue(splitter.split(null).isEmpty());
        assertEquals(Arrays.asList("line1", "", "line2"), splitter.split("line1\n\nline2"));
        assertEquals(Arrays.asList("", "", "", ""), splitter.split("\n\n\n"));
        assertEquals(Arrays.asList("line1", "line2"), splitter.omitEmptyStrings().split("line1\n\nline2\n"));
    }

    @Test
    public void testWith() {
        final Splitter byChar = Splitter.with(',');
        assertEquals(Arrays.asList("a", "b", "c"), byChar.split("a,b,c"));
        assertEquals(Arrays.asList(""), byChar.split(""));
        assertTrue(byChar.split(null).isEmpty());
        assertEquals(Arrays.asList("a"), byChar.split("a"));
        assertEquals(Arrays.asList("", "a", "b"), byChar.split(",a,b"));
        assertEquals(Arrays.asList("a", "b", ""), byChar.split("a,b,"));
        assertEquals(Arrays.asList("a", "", "b"), byChar.split("a,,b"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with('|').split("a|b|c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with('\t').split("a\tb\tc"));

        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with("::").split("a::b::c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(":").split("a:b:c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(" | ").split("a | b | c"));
        assertEquals(Arrays.asList("", "a", "b"), Splitter.with("::").split("::a::b"));
        assertEquals(Arrays.asList("a", "b", ""), Splitter.with("::").split("a::b::"));
        assertEquals(Arrays.asList("", ""), Splitter.with("::").split("::"));
        assertEquals(Arrays.asList("a:b", "c"), Splitter.with("::").split("a:b::c"));

        assertEquals(Arrays.asList("a", "b", "c", "d"), Splitter.with(Pattern.compile("\\s+")).split("a b  c   d"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.pattern("\\d+").split("a123b456c"));
        assertEquals(Arrays.asList("abc", "def", "ghi"), Splitter.pattern("(\\d+)").split("abc123def456ghi"));
    }

    @Test
    public void testWith_EdgeCase() {
        assertEquals(Arrays.asList("a", "", "", "b"), Splitter.with(',').split("a,,,b"));
        assertEquals(Arrays.asList("", "", "", ""), Splitter.with(',').split(",,,"));
        assertEquals(Arrays.asList("", "x", ""), Splitter.with(',').split(",x,"));
        assertEquals(Arrays.asList("a", "", "b"), Splitter.with("::").split("a::::b"));
        assertEquals(Arrays.asList("", "a", "b", ""), Splitter.with("::").split("::a::b::"));
        assertEquals(Arrays.asList("a:b", "c:d"), Splitter.with("::").split("a:b::c:d"));
        assertEquals(Arrays.asList(""), Splitter.with("::").split(""));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(Pattern.compile("\\s+")).split("a \t\n b"));
        assertEquals(Arrays.asList("", "a", "b", ""), Splitter.with(Pattern.compile("\\s+")).split("  a b  "));
        assertEquals(Arrays.asList("", ""), Splitter.with(Pattern.compile("\\s+")).split("   "));
        assertEquals(Arrays.asList("abcdef"), Splitter.with(Pattern.compile("\\d+")).split("abcdef"));
        assertTrue(Splitter.with("::").split(null).isEmpty());
        assertTrue(Splitter.with(Pattern.compile(",")).split(null).isEmpty());

        final StringBuilder delimiter = new StringBuilder("::");
        final Splitter snapshot = Splitter.with(delimiter);
        delimiter.setLength(1);
        assertEquals(Arrays.asList("a", "b", "c"), snapshot.split("a::b::c"));

        assertThrows(IllegalArgumentException.class, () -> Splitter.with((CharSequence) null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(""));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with((Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile("")));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile(".*")));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(Pattern.compile("a*")));
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(null));
        assertThrows(IllegalArgumentException.class, () -> Splitter.pattern(""));
    }

    @Test
    public void testSplit_GuavaCompat() {
        final String source = "aaaaa";
        assertEquals(Arrays.asList("", "", "a"), Splitter.with("aa").split(source));
        assertArrayEquals(Array.of("", "", "", "", "", ""), Splitter.with("a").splitToArray(source));
        assertEquals(CommonUtil.toList("", "", "", "", "", ""), com.google.common.base.Splitter.on("a").splitToList(source));
        assertArrayEquals(Array.of("", "", "a"), Splitter.with("aa").splitToArray(source));
        assertEquals(CommonUtil.toList("", "", "a"), com.google.common.base.Splitter.on("aa").splitToList(source));
        assertArrayEquals(Array.of("", "", "", "", "", ""), Splitter.pattern("a").splitToArray(source));
        assertEquals(CommonUtil.toList("", "", "", "", "", ""), com.google.common.base.Splitter.onPattern("a").splitToList(source));
        assertArrayEquals(Array.of("", "", "a"), Splitter.pattern("aa").splitToArray(source));
        assertEquals(CommonUtil.toList("", "", "a"), com.google.common.base.Splitter.onPattern("aa").splitToList(source));

        final String whitespace = "a   b \t \n c \n \t \r d " + '\u0009' + '\u000B' + '\u000C' + " \re";
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), Splitter.with(Splitter.WHITE_SPACE_PATTERN).split(whitespace));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(Splitter.WHITE_SPACE_PATTERN).split("a  b\t\nc"));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(Splitter.WHITE_SPACE_PATTERN).omitEmptyStrings().split("  a  b  "));
    }

    @Test
    public void testOmitEmptyStrings() {
        assertEquals(Arrays.asList("a", "b"), Splitter.with(',').omitEmptyStrings(true).split("a,,b,"));
        assertEquals(Arrays.asList("a", "", "b", ""), Splitter.with(',').omitEmptyStrings(false).split("a,,b,"));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(',').omitEmptyStrings().split("a,,b,"));
        assertTrue(Splitter.with(',').omitEmptyStrings().split(",,,").isEmpty());
        assertTrue(Splitter.with(',').omitEmptyStrings().split("").isEmpty());
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').omitEmptyStrings().split(",,,,a,,,,b,,,,c,,,,"));
        assertTrue(Splitter.with(',').omitEmptyStrings().split(",,,,,,,,").isEmpty());
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(Pattern.compile("\\s+")).omitEmptyStrings().split("  a  b  c  "));
        assertTrue(Splitter.with(Pattern.compile("\\s+")).omitEmptyStrings().split("     ").isEmpty());
        assertEquals(Arrays.asList("a", "b"), Splitter.with("::").trimResults().omitEmptyStrings().split(" a :: :: b :: "));
        assertEquals(Arrays.asList("hello", " ", "world"), Splitter.with(Pattern.compile("\\b")).omitEmptyStrings().split("hello world"));
    }

    @Test
    public void testTrimResults() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').trim(true).split("a , b , c"));
        assertEquals(Arrays.asList("a ", " b ", " c"), Splitter.with(',').trim(false).split("a , b , c"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').trimResults().split("  a  ,  b  ,  c  "));
        assertEquals(Arrays.asList("", ""), Splitter.with(',').trimResults().split("   ,   "));
        assertEquals(Arrays.asList("a b", "c d"), Splitter.with(',').trimResults().split(" a b , c d "));
        assertEquals("a", Splitter.with(',').trimResults().split(" a ,\tb\t").get(0));
        assertEquals("\tb\t", Splitter.with(',').trimResults().split(" a ,\tb\t").get(1));
    }

    @Test
    public void testStripResults() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').strip(true).split("a\t,\nb\t,\tc"));
        assertEquals(Arrays.asList("a\t", "\nb\t", "\tc"), Splitter.with(',').strip(false).split("a\t,\nb\t,\tc"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').stripResults().split(" a\t\n,  b\r\n  ,   c\t   "));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(',').stripResults().split("\t\na\r\n,\t\nb\t\n"));
        assertEquals(Arrays.asList("", ""), Splitter.with(',').stripResults().split("\t\n,\r\n"));
        assertEquals(Arrays.asList("a\u00A0", "b"), Splitter.with(',').stripResults().split(" a\u00A0,\u2003b "));
        assertEquals(Arrays.asList("a", "b"), Splitter.with(',').stripResults().omitEmptyStrings().split("\ta\t,\n,\rb\r"));
    }

    @Test
    public void testLimit() {
        assertEquals(Arrays.asList("a", "b,c,d"), Splitter.with(',').limit(2).split("a,b,c,d"));
        assertEquals(Arrays.asList("a,b,c,d"), Splitter.with(',').limit(1).split("a,b,c,d"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').limit(5).split("a,b,c"));
        assertEquals(Arrays.asList("a,b,c,d,e"), Splitter.with(',').limit(1).split("a,b,c,d,e"));
        assertEquals(Arrays.asList(""), Splitter.with(',').limit(1).split(""));
        assertEquals(Arrays.asList("", ",,"), Splitter.with(',').limit(2).split(",,,"));
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').limit(Integer.MAX_VALUE).split("a,b,c"));
        assertEquals(Arrays.asList("a", "b::c::d"), Splitter.with("::").limit(2).split("a::b::c::d"));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(0));
        assertThrows(IllegalArgumentException.class, () -> Splitter.with(',').limit(-1));
    }

    @Test
    public void testLimit_EdgeCase() {
        assertEquals(Arrays.asList("a", "b,,c"), Splitter.with(',').omitEmptyStrings().limit(2).split("a,b,,c"));
        assertEquals(Arrays.asList("a", "b , , c"), Splitter.with(',').trimResults().limit(2).split(" a , b , , c "));
        assertEquals(Arrays.asList("a", "b,\t,c"), Splitter.with(',').stripResults().omitEmptyStrings().limit(2).split("a,\t,b,\t,c\n"));
        assertEquals(Arrays.asList("a", "b , c"), Splitter.with(',').trimResults().omitEmptyStrings().limit(2).split(" a , , b , c "));
        assertEquals(Arrays.asList("a", "b", "c , d"), Splitter.with(',').omitEmptyStrings().trimResults().limit(3).split(" a , , b , c , d "));
        assertEquals(Arrays.asList("a", "b,,c"), Splitter.with(Pattern.compile(",")).omitEmptyStrings().limit(2).split(",,a,,b,,c"));
        assertEquals(Arrays.asList("mykey", "2>@C=b"), Splitter.with('=').limit(2).omitEmptyStrings(true).split("mykey==2>@C=b"));
    }

    @Test
    public void testSplit() {
        final Splitter splitter = Splitter.with(',');
        assertEquals(Arrays.asList("a", "b", "c"), splitter.split("a,b,c"));
        assertTrue(splitter.split(null).isEmpty());
        assertTrue(splitter.split("a,b,c") instanceof ArrayList);
        assertEquals(Arrays.asList(1, 2, 3), splitter.split("1,2,3", Fn.f(e -> Integer.parseInt(e))));
        assertEquals(Arrays.asList("A", "B", "C"), splitter.split("a,b,c", Fn.toUpperCase()));
        assertEquals(Arrays.asList("HELLO", "WORLD"), splitter.split("hello,world", (Function<String, String>) String::toUpperCase));
        assertEquals(Arrays.asList(1, 2, 3), splitter.split("a,bb,ccc", (Function<String, Integer>) String::length));
        assertEquals(Arrays.asList("a", null, "c"), splitter.split("a,b,c", Fn.f(s -> "b".equals(s) ? null : s)));
        assertTrue(splitter.split((CharSequence) null, Fn.toUpperCase()).isEmpty());
        assertEquals(Arrays.asList(""), splitter.split("", (Function<String, String>) String::trim));
        assertEquals(Arrays.asList(1, 2, 3), splitter.split("1,2,3", Integer.class));
        assertEquals(Arrays.asList(10L, 20L, 30L), splitter.split("10,20,30", Long.class));
        assertEquals(Arrays.asList(1.5, 2.5, 3.5), splitter.split("1.5,2.5,3.5", Double.class));
        assertEquals(Arrays.asList(true, false, true), splitter.split("true,false,true", Boolean.class));
        assertTrue(splitter.split(null, Integer.class).isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), splitter.split("1,2,3", CommonUtil.typeOf(Integer.class)));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.split("a,b,c", (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.split(null, (Function<String, String>) null));
    }

    @Test
    public void testSplitToCollection() {
        final Splitter splitter = Splitter.with(',');
        assertEquals(new HashSet<>(Arrays.asList("a", "b", "c")), splitter.splitToCollection("a,b,c", Suppliers.ofSet()));
        assertEquals(new LinkedList<>(Arrays.asList("a", "b", "c")), splitter.splitToCollection("a,b,c", Suppliers.ofLinkedList()));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), splitter.splitToCollection("1,2,3", Integer.class, Suppliers.ofSet()));
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), splitter.splitToCollection("1,2,3", CommonUtil.typeOf(Integer.class), Suppliers.ofSet()));

        final List<Integer> typed = splitter.splitToCollection("1,2,3", Type.of(Integer.class),
                (java.util.function.Supplier<LinkedList<Integer>>) LinkedList::new);
        assertEquals(Arrays.asList(1, 2, 3), typed);
        assertTrue(typed instanceof LinkedList);

        final java.util.TreeSet<String> tree = splitter.splitToCollection("c,a,b", Type.of(String.class), Suppliers.ofTreeSet());
        assertEquals("a", tree.first());
        assertEquals("c", tree.last());

        final List<String> output = new ArrayList<>();
        output.add("existing");
        splitter.splitInto("a,b,c", output);
        assertEquals(Arrays.asList("existing", "a", "b", "c"), output);

        final List<Integer> ints = new ArrayList<>();
        ints.add(0);
        splitter.splitInto("1,2,3", Integer.class, ints);
        assertEquals(Arrays.asList(0, 1, 2, 3), ints);
        splitter.splitInto("1,2,3", Type.of(Integer.class), new ArrayList<>());

        final Set<String> set = new HashSet<>();
        splitter.splitInto("a,b,a,c", set);
        assertEquals(3, set.size());

        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToCollection(null, (java.util.function.Supplier<List<String>>) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToCollection(null, (java.util.function.Supplier<List<String>>) () -> null));

        final boolean[] supplierCalled = { false };
        assertThrows(IllegalArgumentException.class,
                () -> splitter.splitToCollection("", (Class<Integer>) null, (java.util.function.Supplier<List<Integer>>) () -> {
                    supplierCalled[0] = true;
                    return new ArrayList<>();
                }));
        assertFalse(supplierCalled[0]);
        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", (Class) null, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", Integer.class, (Collection) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", (Type<?>) null, new ArrayList<>()));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", CommonUtil.typeOf(Integer.class), (Collection) null));
    }

    @Test
    public void testSplitToImmutableList() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').splitToImmutableList("a,b,c"));
        assertEquals(Arrays.asList(1, 2, 3), Splitter.with(',').splitToImmutableList("1,2,3", Integer.class));
        assertEquals(Arrays.asList(""), Splitter.with(',').splitToImmutableList(""));
        final ImmutableList<String> result = Splitter.with(',').splitToImmutableList("a,b,c");
        assertThrows(UnsupportedOperationException.class, () -> result.add("d"));
        assertThrows(UnsupportedOperationException.class, () -> result.remove(0));
        assertThrows(UnsupportedOperationException.class, () -> result.set(0, "x"));
        assertThrows(UnsupportedOperationException.class, () -> result.clear());
    }

    @Test
    public void testSplitToArray() {
        final Splitter splitter = Splitter.with(',');
        assertArrayEquals(new String[] { "a", "b", "c" }, splitter.splitToArray("a,b,c"));
        assertArrayEquals(new String[] {}, splitter.splitToArray(null));
        assertArrayEquals(new String[] { "" }, splitter.splitToArray(""));
        assertArrayEquals(new String[] { "solo" }, splitter.splitToArray("solo"));
        assertArrayEquals(new String[] { "a", "b" }, splitter.omitEmptyStrings().splitToArray("a,,b"));
        assertArrayEquals(new String[] { "A", "B", "C" }, splitter.splitToArray("a,b,c", String::toUpperCase));
        assertArrayEquals(new Integer[] { 1, 2, 3 }, splitter.splitToArray("1,2,3", Integer[].class));
        assertArrayEquals(new int[] { 1, 2, 3 }, splitter.splitToArray("1,2,3", int[].class));
        assertArrayEquals(new long[] { 1L, 2L, 3L }, splitter.splitToArray("1,2,3", long[].class));
        assertArrayEquals(new double[] { 1.1, 2.2, 3.3 }, splitter.splitToArray("1.1,2.2,3.3", double[].class), 0.001);
        assertArrayEquals(new boolean[] { true, false, true }, splitter.splitToArray("true,false,true", boolean[].class));
        assertArrayEquals(new Object[] { "a", "b", "c" }, splitter.splitToArray("a,b,c", Object[].class));

        final String[] exact = new String[3];
        splitter.splitInto("a,b,c", exact);
        assertArrayEquals(new String[] { "a", "b", "c" }, exact);

        final String[] larger = new String[5];
        larger[3] = "keep";
        larger[4] = "this";
        splitter.splitInto("a,b,c", larger);
        assertArrayEquals(new String[] { "a", "b", "c", "keep", "this" }, larger);

        final String[] smaller = new String[2];
        splitter.splitInto("a,b,c,d", smaller);
        assertArrayEquals(new String[] { "a", "b" }, smaller);

        final String[] empty = new String[0];
        splitter.splitInto("a,b,c", empty);
        assertEquals(0, empty.length);

        assertArrayEquals(new String[] { "3341", "Wed. Apr 10, 2019", "", "4", "", "16", "", "22", "", "31", "", "42", "", "4", "" },
                Splitter.with("    ")
                        .trim(true)
                        .splitToArray("3341     Wed. Apr 10, 2019          4          16          22          31          42          4     "));

        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b,c", (Class<?>) null));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitToArray("a,b", String.class));
        assertThrows(IllegalArgumentException.class, () -> splitter.splitInto("a,b,c", (String[]) null));
    }

    @Test
    public void testSplitToStream() {
        assertEquals(Arrays.asList("a", "b", "c"), Splitter.with(',').splitToStream("a,b,c").toList());
        assertTrue(Splitter.with(',').splitToStream(null).toList().isEmpty());
        assertEquals(5, Splitter.with(',').splitToStream("a,b,c,d,e").count());
        assertEquals(Arrays.asList("bb", "ccc", "dddd"), Splitter.with(',').splitToStream("a,bb,ccc,dddd").filter(s -> s.length() > 1).toList());
        assertEquals(Arrays.asList(1, 2, 3), Splitter.with(',').splitToStream("1,2,3").map(Integer::parseInt).toList());
    }

    @Test
    public void testSplitThenApply() {
        assertEquals(3, Splitter.with(',').splitThenApply("a,b,c", List::size).intValue());
        assertEquals("a-b-c", Splitter.with(',').splitThenApply("a,b,c", list -> String.join("-", list)));
        assertEquals(15, Splitter.with(',').splitThenApply("1,2,3,4,5", list -> list.stream().mapToInt(Integer::parseInt).sum()).intValue());
        assertTrue(Splitter.with(',').splitThenApply(null, list -> list).isEmpty());
    }

    @Test
    public void testSplitThenAccept() {
        final List<String> captured = new ArrayList<>();
        Splitter.with(',').splitThenAccept("a,b,c", captured::addAll);
        assertEquals(Arrays.asList("a", "b", "c"), captured);

        final List<String> fromNull = new ArrayList<>();
        Splitter.with(',').splitThenAccept(null, fromNull::addAll);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testSplitThenForEach() {
        final List<String> captured = new ArrayList<>();
        Splitter.with(',').splitThenForEach("a,b,c", captured::add);
        assertEquals(Arrays.asList("a", "b", "c"), captured);

        final List<Integer> lengths = new ArrayList<>();
        Splitter.with(',').splitThenForEach("a,bb,ccc", s -> lengths.add(s.length()));
        assertEquals(Arrays.asList(1, 2, 3), lengths);

        final List<String> fromNull = new ArrayList<>();
        Splitter.with(',').splitThenForEach(null, fromNull::add);
        assertTrue(fromNull.isEmpty());
        Splitter.with(',').omitEmptyStrings().splitThenForEach("", fromNull::add);
        assertTrue(fromNull.isEmpty());
    }

    @Test
    public void testIterate() {
        final var iter = Splitter.with(',').iterate("a,b,c");
        assertTrue(iter.hasNext());
        assertEquals("a", iter.next());
        assertEquals("b", iter.next());
        assertEquals("c", iter.next());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::next);
    }

    @Test
    public void testCombinedOptions() {
        final List<String> chained = Splitter.with(',').omitEmptyStrings().trimResults().stripResults().limit(5).split(" a , \t, b ,\n c , d , e , f ");
        assertEquals(5, chained.size());
        assertEquals("a", chained.get(0));
        assertEquals("b", chained.get(1));
        assertEquals(Arrays.asList("a", "b , c"), Splitter.with(',').trimResults().omitEmptyStrings().limit(2).split(" a , , b , c "));
        assertEquals(Arrays.asList("a", "b", "c , d"), Splitter.with(',').omitEmptyStrings().trimResults().limit(3).split(" a , , b , c , d "));
    }

    @Test
    public void testDefensiveCopyAndUnicode() {
        final Splitter splitter = Splitter.with(',');
        final List<String> result1 = splitter.split("a,b,c");
        final List<String> result2 = splitter.split("a,b,c");
        assertTrue(result1 != result2);
        assertEquals(result1, result2);
        result1.add("d");
        assertEquals(4, result1.size());
        assertEquals(3, result2.size());

        assertEquals(Arrays.asList("你好", "世界", "测试"), splitter.split("你好,世界,测试"));
        assertEquals(Arrays.asList("😀", "😁", "😂"), splitter.split("😀,😁,😂"));
        assertEquals(Arrays.asList("a", "你好", "b", "😀", "c"), splitter.split("a,你好,b,😀,c"));

        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append("item").append(i);
        }
        final List<String> large = splitter.split(sb.toString());
        assertEquals(1000, large.size());
        assertEquals("item0", large.get(0));
        assertEquals("item999", large.get(999));
    }

    @Test
    public void testMapSplitter() {
        final MapSplitter mapSplitter = MapSplitter.with(",", "=");
        final Map<String, String> expected = new LinkedHashMap<>();
        expected.put("a", "1");
        expected.put("b", "2");
        expected.put("c", "3");
        assertEquals(expected, mapSplitter.split("a=1,b=2,c=3"));
        assertTrue(mapSplitter.split("a=1,b=2") instanceof LinkedHashMap);

        assertEquals(expected, MapSplitter.with(";", ":").split("a:1;b:2;c:3"));
        assertEquals(expected, MapSplitter.with(Pattern.compile("\\s*,\\s*"), Pattern.compile("\\s*=\\s*")).split("a=1, b=2, c=3"));
        assertEquals(expected, MapSplitter.pattern("\\s*,\\s*", "\\s*=\\s*").split("a=1, b=2, c=3"));
        assertEquals(expected, MapSplitter.pattern("[,;]", "[=:]").split("a=1;b:2,c=3"));

        final Map<String, String> mixed = MapSplitter.with(Pattern.compile("[;,]"), Pattern.compile("[:=]")).split("a=1,b:2;c=3");
        assertEquals(expected, mixed);

        final Map<Integer, Long> typed = mapSplitter.split("1=100,2=200", Integer.class, Long.class);
        assertEquals(Long.valueOf(100), typed.get(1));
        assertEquals(Long.valueOf(200), typed.get(2));
        assertEquals(typed, mapSplitter.split("1=100,2=200", CommonUtil.typeOf(Integer.class), CommonUtil.typeOf(Long.class)));

        final TreeMap<String, String> tree = mapSplitter.splitToMap("a=1,b=2", Suppliers.ofTreeMap());
        assertTrue(tree instanceof TreeMap);
        assertEquals(2, tree.size());

        final TreeMap<Integer, Long> typedTree = mapSplitter.splitToMap("1=100,2=200", Integer.class, Long.class, Suppliers.ofTreeMap());
        assertEquals(2, typedTree.size());
        assertEquals(mapSplitter.splitToMap("1=100,2=200", CommonUtil.typeOf(Integer.class), CommonUtil.typeOf(Long.class), Suppliers.ofTreeMap()).size(), 2);

        final Map<String, String> output = new HashMap<>();
        mapSplitter.splitInto("a=1,b=2", output);
        assertEquals("1", output.get("a"));
        mapSplitter.splitInto("1=100,2=200", Integer.class, Long.class, new HashMap<>());
        mapSplitter.splitInto("1=100,2=200", CommonUtil.typeOf(Integer.class), CommonUtil.typeOf(Long.class), new HashMap<>());

        final ImmutableMap<String, String> immutable = mapSplitter.splitToImmutableMap("a=1,b=2");
        assertEquals("1", immutable.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> immutable.put("c", "3"));
        assertThrows(UnsupportedOperationException.class, immutable::clear);

        final ImmutableMap<Integer, Long> typedImmutable = mapSplitter.splitToImmutableMap("1=100,2=200", Integer.class, Long.class);
        assertEquals(Long.valueOf(100), typedImmutable.get(1));
        assertThrows(UnsupportedOperationException.class, () -> typedImmutable.put(3, 300L));

        assertEquals(3, mapSplitter.splitToStream("a=1,b=2,c=3").count());
        assertEquals("a", mapSplitter.splitToStream("a=1,b=2,c=3").toList().get(0).getKey());
        assertEquals(2, mapSplitter.splitToEntryStream("a=1,b=2").toMap().size());
        assertEquals(3, mapSplitter.splitThenApply("a=1,b=2,c=3", Map::size).intValue());
        assertTrue(mapSplitter.splitThenApply("a=1,b=2,c=3", Map::keySet).contains("a"));

        final Map<String, String> accepted = new HashMap<>();
        mapSplitter.splitThenAccept("a=1,b=2", accepted::putAll);
        assertEquals(2, accepted.size());

        final Map<String, Integer> converted = MapSplitter.with(",", ":").split("one:1,two:2", String.class, Integer.class);
        assertEquals(Integer.valueOf(1), converted.get("one"));

        final Map<String, String> limited = MapSplitter.with(",", "=").limit(2).split("a=1,b=2,c=3");
        assertEquals("1", limited.get("a"));
        assertEquals("2,c=3", limited.get("b"));
    }

    @Test
    public void testMapSplitter_EdgeCase() {
        final MapSplitter mapSplitter = MapSplitter.with(",", "=");
        assertTrue(mapSplitter.split(null).isEmpty());
        assertTrue(mapSplitter.split("").isEmpty());
        assertEquals(0, mapSplitter.splitToStream(null).count());
        assertEquals(0, mapSplitter.splitToEntryStream(null).count());
        assertEquals(0, mapSplitter.splitThenApply(null, Map::size).intValue());

        final List<Integer> sizes = new ArrayList<>();
        mapSplitter.splitThenAccept(null, map -> sizes.add(map.size()));
        assertEquals(Arrays.asList(0), sizes);

        final Map<String, String> omitted = mapSplitter.omitEmptyStrings().split("a=1,,b=2");
        assertEquals(Map.of("a", "1", "b", "2"), omitted);
        assertEquals(Map.of("a", "1", "b", "2"), mapSplitter.omitEmptyStrings(true).split("a=1,,b=2"));
        assertEquals("1", mapSplitter.trim(true).split(" a = 1 , b = 2 ").get("a"));
        assertEquals("1", mapSplitter.trimResults().split(" a = 1 , b = 2 ").get("a"));
        assertEquals("1", mapSplitter.strip(true).split("\ta\t=\t1\t,\tb\t=\t2\t").get("a"));
        assertEquals("1", mapSplitter.stripResults().split("a\t=\n1,b=2").get("a"));

        final Map<String, String> combined = MapSplitter.with(",", "=").trimResults().omitEmptyStrings().limit(2).split(" a = 1 , , b = 2 , c = 3 ");
        assertEquals(2, combined.size());
        assertEquals("1", combined.get("a"));
        assertEquals("2 , c = 3", combined.get("b"));

        assertEquals("", mapSplitter.split("a=").get("a"));
        assertEquals("1", mapSplitter.split("=1").get(""));
        assertEquals("b=c", mapSplitter.split("a=b=c").get("a"));
        assertEquals("张三", MapSplitter.with(",", "=").split("名字=张三,年龄=25").get("名字"));

        final Stream<Map.Entry<String, String>> filtered = mapSplitter.splitToStream("a=1,b=2,c=3,d=4").filter(e -> Integer.parseInt(e.getValue()) > 2);
        assertEquals("c", filtered.toList().get(0).getKey());
        assertEquals(2, mapSplitter.splitToEntryStream("a=1,b=2,c=3").filter(e -> !"b".equals(e.getKey())).toMap().size());

        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(null, "="));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", null));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with("", ":"));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(";", ""));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with((Pattern) null, Pattern.compile("=")));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(Pattern.compile(","), (Pattern) null));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.pattern(null, "="));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.pattern(",", null));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").limit(0));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").limit(-1));
        assertThrows(IllegalArgumentException.class, () -> MapSplitter.with(",", "=").split("a=1,invalid,b=2"));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitInto("a=1", (Map<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", null, String.class));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", String.class, null));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", (Type<?>) null, CommonUtil.typeOf(String.class)));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.split("a=1", CommonUtil.typeOf(String.class), (Type<?>) null));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitInto("a=1", String.class, String.class, (Map<String, String>) null));
        assertThrows(IllegalArgumentException.class,
                () -> mapSplitter.splitInto("a=1", Type.of(String.class), Type.of(String.class), (Map<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitToMap(null, (java.util.function.Supplier<Map<String, String>>) null));
        assertThrows(IllegalArgumentException.class, () -> mapSplitter.splitToMap(null, (java.util.function.Supplier<Map<String, String>>) () -> null));

        final boolean[] supplierCalled = { false };
        assertThrows(IllegalArgumentException.class,
                () -> mapSplitter.splitToMap("", (Class<String>) null, Integer.class, (java.util.function.Supplier<Map<String, Integer>>) () -> {
                    supplierCalled[0] = true;
                    return new HashMap<>();
                }));
        assertFalse(supplierCalled[0]);
    }
}
