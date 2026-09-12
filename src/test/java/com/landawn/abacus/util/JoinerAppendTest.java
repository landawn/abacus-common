package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class JoinerAppendTest extends JoinerTestSupport {

    @Test
    public void testAppend() {
        assertEquals("true,false,true", Joiner.with(",").append(true).append(false).append(true).toString());
        assertEquals("a,b,c", Joiner.with(",").append('a').append('b').append('c').toString());
        assertEquals("1,2,3", Joiner.with(",").append(1).append(2).append(3).toString());
        assertEquals("1,2,3", Joiner.with(",").append(1L).append(2L).append(3L).toString());
        assertEquals("1.5,2.5,3.5", Joiner.with(",").append(1.5f).append(2.5f).append(3.5f).toString());
        assertEquals("1.5,2.5,3.5", Joiner.with(",").append(1.5).append(2.5).append(3.5).toString());
        assertEquals("hello,world,test", Joiner.with(",").append("hello").append("world").append("test").toString());
        assertEquals("hello,world", Joiner.with(",").append((CharSequence) new StringBuilder("hello")).append(new StringBuffer("world")).toString());
        assertEquals("hello,world", Joiner.with(",").append(new StringBuilder("hello")).append(new StringBuilder("world")).toString());
        assertEquals("hello,world", Joiner.with(",").append("hello world", 0, 5).append("hello world", 6, 11).toString());
        assertEquals("1,2.5,text", Joiner.with(",").append(Integer.valueOf(1)).append(Double.valueOf(2.5)).append("text").toString());
        assertEquals("[a, b],[c, d]", Joiner.with(",").append(new char[] { 'a', 'b' }).append(new char[] { 'c', 'd' }).toString());
        assertEquals("true, 1, 2, 3.0, 4.0", Joiner.with(", ").append(true).append('1').append(2L).append(3.0f).append(4.0d).toString());
        assertEquals("ell", Joiner.with("").append("hello", 1, 4).toString());
        assertEquals("trimmed", Joiner.with(",").trimBeforeAppend().append((CharSequence) "  trimmed  ").toString());
        assertEquals("hello world", Joiner.with(",").trimBeforeAppend().append("  hello world  ", 2, 13).toString());
        assertEquals("value", Joiner.with(",").trimBeforeAppend().append(new StringBuilder("  value  ")).toString());
        assertEquals("value", Joiner.with(",").stripBeforeAppend().append(new StringBuilder("  value  ")).toString());
    }

    @Test
    public void testAppend_EdgeCase() {
        assertEquals("\n,\t,\r", Joiner.with(",").append('\n').append('\t').append('\r').toString());
        assertEquals("-1,0,100", Joiner.with(",").append(-1).append(0).append(100).toString());
        assertEquals(Long.MAX_VALUE + "," + Long.MIN_VALUE, Joiner.with(",").append(Long.MAX_VALUE).append(Long.MIN_VALUE).toString());
        assertEquals("NaN,Infinity,-Infinity", Joiner.with(",").append(Float.NaN).append(Float.POSITIVE_INFINITY).append(Float.NEGATIVE_INFINITY).toString());
        assertEquals("NaN,Infinity", Joiner.with(",").append(Double.NaN).append(Double.POSITIVE_INFINITY).toString());
        assertEquals("a,,b", Joiner.with(",").append("a").append("").append("b").toString());
        assertEquals("a,null,b", Joiner.with(",").append("a").append((String) null).append("b").toString());
        assertEquals("a,null,b", Joiner.with(",").append("a").append((CharSequence) null).append("b").toString());
        assertEquals("a,null,b", Joiner.with(",").append("a").append((StringBuilder) null).append("b").toString());
        assertEquals("a,null,b", Joiner.with(",").append("a").append((Object) null).append("b").toString());
        assertEquals("a,b", Joiner.with(",").skipNulls().append("a").append((StringBuilder) null).append("b").toString());
        assertEquals("a, b", Joiner.with(", ").skipNulls().append("a").append((CharSequence) null).append("b").toString());
        assertEquals("a, NULL", Joiner.with(", ").useForNull("NULL").append("a").append((CharSequence) null).toString());
        assertEquals("a,null,b", Joiner.with(",").append("a").append((CharSequence) null, 0, 0).append("b").toString());
        assertEquals("a,b", Joiner.with(",").skipNulls().append("a").append((CharSequence) null, 0, 0).append("b").toString());
        assertEquals("a,NIL,b", Joiner.with(",").useForNull("NIL").append("a").append((CharSequence) null, 0, 0).append("b").toString());
        assertEquals("test,es", Joiner.with(",").append("test", 0, 4).append("test", 1, 3).toString());
        assertEquals("worl", Joiner.with("").trimBeforeAppend().append(" world ", 0, 5).toString());

        final Joiner joiner = Joiner.with(",");
        joiner.append("a");
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.append("bcd", 0, 99));
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.append("bcd", -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> joiner.append("bcd", 2, 1));
    }

    @Test
    public void testAppendIfNotNull() {
        assertEquals("a,b", Joiner.with(",").appendIfNotNull("a").appendIfNotNull(null).appendIfNotNull("b").appendIfNotNull(null).toString());
        assertEquals("", Joiner.with(",").appendIfNotNull(null).appendIfNotNull(null).toString());
    }

    @Test
    public void testAppendIf() {
        assertEquals("yes,maybe", Joiner.with(",").appendIf(true, () -> "yes").appendIf(false, () -> "no").appendIf(true, () -> "maybe").toString());
        assertEquals("a,b,c", Joiner.with(",").append("a").appendIf(true, () -> "b").append("c").toString());
        assertEquals("a,c", Joiner.with(",").append("a").appendIf(false, () -> "b").append("c").toString());
        assertEquals("before,null,after", Joiner.with(",").append("before").appendIf(true, () -> null).append("after").toString());

        final Supplier<?> exploding = () -> {
            throw new RuntimeException("Should not be called");
        };
        assertEquals("safe", Joiner.with(",").appendIf(false, exploding).append("safe").toString());
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendIf(true, null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendIf(false, null));
    }

    @Test
    public void testAppendAll() {
        assertEquals("true,false,true", Joiner.with(",").appendAll(new boolean[] { true, false, true }).toString());
        assertEquals("a,b,c", Joiner.with(",").appendAll(new char[] { 'a', 'b', 'c' }).toString());
        assertEquals("1,2,3", Joiner.with(",").appendAll(new byte[] { 1, 2, 3 }).toString());
        assertEquals("10,20,30", Joiner.with(",").appendAll(new short[] { 10, 20, 30 }).toString());
        assertEquals("1,2,3", Joiner.with(",").appendAll(new int[] { 1, 2, 3 }).toString());
        assertEquals("100,200,300", Joiner.with(",").appendAll(new long[] { 100L, 200L, 300L }).toString());
        assertEquals("1.1,2.2,3.3", Joiner.with(",").appendAll(new float[] { 1.1f, 2.2f, 3.3f }).toString());
        assertEquals("1.11,2.22,3.33", Joiner.with(",").appendAll(new double[] { 1.11, 2.22, 3.33 }).toString());
        assertEquals("a,b,c", Joiner.with(",").appendAll(new String[] { "a", "b", "c" }).toString());

        assertEquals("false,true", Joiner.with(",").appendAll(new boolean[] { true, false, true, false }, 1, 3).toString());
        assertEquals("b,c", Joiner.with(",").appendAll(new char[] { 'a', 'b', 'c', 'd' }, 1, 3).toString());
        assertEquals("2,3", Joiner.with(",").appendAll(new byte[] { 1, 2, 3, 4 }, 1, 3).toString());
        assertEquals("20,30", Joiner.with(",").appendAll(new short[] { 10, 20, 30, 40 }, 1, 3).toString());
        assertEquals("2,3", Joiner.with(",").appendAll(new int[] { 1, 2, 3, 4 }, 1, 3).toString());
        assertEquals("200,300", Joiner.with(",").appendAll(new long[] { 100L, 200L, 300L, 400L }, 1, 3).toString());
        assertEquals("2.2,3.3", Joiner.with(",").appendAll(new float[] { 1.1f, 2.2f, 3.3f, 4.4f }, 1, 3).toString());
        assertEquals("2.22,3.33", Joiner.with(",").appendAll(new double[] { 1.11, 2.22, 3.33, 4.44 }, 1, 3).toString());
        assertEquals("b,c", Joiner.with(",").appendAll(new String[] { "a", "b", "c", "d" }, 1, 3).toString());
        assertEquals("2+3+4", Joiner.with("+").appendAll(new int[] { 1, 2, 3, 4, 5 }, 1, 4).toString());

        assertEquals("true,false,true", Joiner.with(",").appendAll(BooleanList.of(true, false, true)).toString());
        assertEquals("a,b,c", Joiner.with(",").appendAll(CharList.of('a', 'b', 'c')).toString());
        assertEquals("1,2,3", Joiner.with(",").appendAll(ByteList.of((byte) 1, (byte) 2, (byte) 3)).toString());
        assertEquals("10,20,30", Joiner.with(",").appendAll(ShortList.of((short) 10, (short) 20, (short) 30)).toString());
        assertEquals("100,200,300", Joiner.with(",").appendAll(IntList.of(100, 200, 300)).toString());
        assertEquals("1000,2000,3000", Joiner.with(",").appendAll(LongList.of(1000L, 2000L, 3000L)).toString());
        assertEquals("1.1,2.2,3.3", Joiner.with(",").appendAll(FloatList.of(1.1f, 2.2f, 3.3f)).toString());
        assertEquals("1.11,2.22,3.33", Joiner.with(",").appendAll(DoubleList.of(1.11, 2.22, 3.33)).toString());

        assertEquals("false,true", Joiner.with(",").appendAll(BooleanList.of(true, false, true, false), 1, 3).toString());
        assertEquals("b,c", Joiner.with(",").appendAll(CharList.of('a', 'b', 'c', 'd'), 1, 3).toString());
        assertEquals("2,3", Joiner.with(",").appendAll(ByteList.of((byte) 1, (byte) 2, (byte) 3, (byte) 4), 1, 3).toString());
        assertEquals("20,30", Joiner.with(",").appendAll(ShortList.of((short) 10, (short) 20, (short) 30, (short) 40), 1, 3).toString());
        assertEquals("200,300", Joiner.with(",").appendAll(IntList.of(100, 200, 300, 400), 1, 3).toString());
        assertEquals("2000,3000", Joiner.with(",").appendAll(LongList.of(1000L, 2000L, 3000L, 4000L), 1, 3).toString());
        assertEquals("2.2,3.3", Joiner.with(",").appendAll(FloatList.of(1.1f, 2.2f, 3.3f, 4.4f), 1, 3).toString());
        assertEquals("2.22,3.33", Joiner.with(",").appendAll(DoubleList.of(1.11, 2.22, 3.33, 4.44), 1, 3).toString());

        final List<String> list = Arrays.asList("a", "b", "c", "d");
        assertEquals("a,b,c", Joiner.with(",").appendAll(list.subList(0, 3)).toString());
        assertEquals("b,c", Joiner.with(",").appendAll(list, 1, 3).toString());
        assertEquals("a,b,c", Joiner.with(",").appendAll((Iterable<?>) list.subList(0, 3)).toString());
        assertEquals("a,b,c", Joiner.with(",").appendAll(list.subList(0, 3).iterator()).toString());
        assertEquals("a,c", Joiner.with(",").appendAll(Arrays.asList("a", "bb", "c", "dd"), s -> s.length() == 1).toString());
        assertEquals("2,4", Joiner.with(",").appendAll(Arrays.asList(1, 2, 3, 4, 5).iterator(), i -> i % 2 == 0).toString());
        assertEquals("apple,apricot",
                Joiner.with(",").appendAll(Arrays.asList("apple", "banana", "apricot", "cherry"), (Predicate<String>) s -> s.startsWith("a")).toString());
        assertEquals("123", Joiner.with("").appendAll(new int[] { 1, 2, 3 }).toString());
        assertEquals("abc", Joiner.with("").appendAll(new String[] { "a", "b", "c" }).toString());
        assertEquals("xyz", Joiner.with("").appendAll(Arrays.asList("x", "y", "z")).toString());
        assertEquals("truefalse", Joiner.with("").appendAll(new boolean[] { true, false }).toString());
        assertEquals("ab", Joiner.with("").appendAll(new char[] { 'a', 'b' }).toString());
    }

    @Test
    public void testAppendAll_EdgeCase() {
        final Joiner empty = Joiner.with(",").append("start");
        empty.appendAll(new boolean[0])
                .appendAll(new char[0])
                .appendAll(new byte[0])
                .appendAll(new short[0])
                .appendAll(new int[0])
                .appendAll(new long[0])
                .appendAll(new float[0])
                .appendAll(new double[0])
                .appendAll(new Object[0])
                .appendAll(new BooleanList())
                .appendAll(new CharList())
                .appendAll(new ByteList())
                .appendAll(new ShortList())
                .appendAll(new IntList())
                .appendAll(new LongList())
                .appendAll(new FloatList())
                .appendAll(new DoubleList())
                .appendAll(new ArrayList<>())
                .appendAll(Collections.emptyList())
                .appendAll(Collections.<String> emptyList().iterator())
                .append("end");
        assertEquals("start,end", empty.toString());

        final Joiner nul = Joiner.with(",").append("start");
        nul.appendAll((boolean[]) null)
                .appendAll((char[]) null)
                .appendAll((byte[]) null)
                .appendAll((short[]) null)
                .appendAll((int[]) null)
                .appendAll((long[]) null)
                .appendAll((float[]) null)
                .appendAll((double[]) null)
                .appendAll((Object[]) null)
                .appendAll((BooleanList) null)
                .appendAll((CharList) null)
                .appendAll((ByteList) null)
                .appendAll((ShortList) null)
                .appendAll((IntList) null)
                .appendAll((LongList) null)
                .appendAll((FloatList) null)
                .appendAll((DoubleList) null)
                .appendAll((Collection<?>) null)
                .appendAll((Iterable<?>) null)
                .appendAll((Iterator<?>) null)
                .appendAll((Iterable<String>) null, s -> true)
                .appendAll((Iterator<String>) null, s -> true)
                .append("end");
        assertEquals("start,end", nul.toString());

        assertEquals("a,null,c", Joiner.with(",").appendAll(new String[] { "a", null, "c" }).toString());
        assertEquals("a,c", Joiner.with(",").skipNulls().appendAll(new String[] { "a", null, "c" }).toString());
        assertEquals("a,c", Joiner.with(",").skipNulls().appendAll(Arrays.asList("a", null, "c")).toString());
        assertEquals("", Joiner.with(",").appendAll(Arrays.asList("aa", "bb", "cc"), s -> s.length() == 1).toString());
        assertEquals("", Joiner.with(",").appendAll(Arrays.asList(1, 3, 5).iterator(), i -> i % 2 == 0).toString());

        final Joiner emptyRange = Joiner.with(",");
        assertSame(emptyRange, emptyRange.appendAll(new boolean[] { true, false }, 1, 1));
        emptyRange.appendAll(new char[] { 'a' }, 1, 1)
                .appendAll(new byte[] { 1 }, 0, 0)
                .appendAll(new short[] { 1 }, 1, 1)
                .appendAll(new int[] { 1 }, 1, 1)
                .appendAll(new long[] { 1L }, 1, 1)
                .appendAll(new float[] { 1f }, 0, 0)
                .appendAll(new double[] { 1d }, 1, 1)
                .appendAll(Arrays.asList("a", "b"), 1, 1);
        assertEquals("", emptyRange.toString());

        final int[] arr = { 1, 2, 3 };
        final List<String> list = Arrays.asList("a", "b", "c");
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendAll(arr, -1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendAll(arr, 0, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendAll(arr, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendAll(list, 2, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendAll(list, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendAll(list, (Predicate<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendAll(list.iterator(), (Predicate<String>) null));
    }

    @Test
    public void testAppendEntry() {
        assertEquals("flag=true,active=false", Joiner.with(",").appendEntry("flag", true).appendEntry("active", false).toString());
        assertEquals("letter=A,digit=9", Joiner.with(",").appendEntry("letter", 'A').appendEntry("digit", '9').toString());
        assertEquals("count=42,total=100", Joiner.with(",").appendEntry("count", 42).appendEntry("total", 100).toString());
        assertEquals("id=1234567890,timestamp=9876543210", Joiner.with(",").appendEntry("id", 1234567890L).appendEntry("timestamp", 9876543210L).toString());
        assertEquals("pi=3.14,e=2.71", Joiner.with(",").appendEntry("pi", 3.14f).appendEntry("e", 2.71f).toString());
        assertEquals("pi=3.14159,e=2.71828", Joiner.with(",").appendEntry("pi", 3.14159).appendEntry("e", 2.71828).toString());
        assertEquals("name=Alice,city=NYC", Joiner.with(",").appendEntry("name", "Alice").appendEntry("city", "NYC").toString());
        assertEquals("key=test", Joiner.with(",").appendEntry("key", (CharSequence) new StringBuilder("test")).toString());
        assertEquals("key=builder", Joiner.with(",").appendEntry("key", new StringBuilder("builder")).toString());
        assertEquals("int=123,double=4.56", Joiner.with(",").appendEntry("int", Integer.valueOf(123)).appendEntry("double", Double.valueOf(4.56)).toString());
        assertEquals("key=value", Joiner.with(",").appendEntry(new AbstractMap.SimpleEntry<>("key", "value")).toString());
        assertEquals("key->value", Joiner.with(",", "->").appendEntry("key", "value").toString());
        assertEquals("key=value", Joiner.with(",").trimBeforeAppend().appendEntry("  key  ", "  value  ").toString());
        assertEquals("key=value", Joiner.with(",").trimBeforeAppend().appendEntry("  key  ", new StringBuilder("  value  ")).toString());
        assertEquals("key=[t, e, s, t]", Joiner.with(",").appendEntry("key", new char[] { 't', 'e', 's', 't' }).toString());
        assertEquals("k=123", Joiner.with(", ").appendEntry("k", 123).toString());
        assertEquals("a: 1; b: 2", Joiner.with("; ", ": ").appendEntry("a", 1).appendEntry("b", 2).toString());
    }

    @Test
    public void testAppendEntry_EdgeCase() {
        assertEquals("key1=null,key2=value", Joiner.with(",").appendEntry("key1", (String) null).appendEntry("key2", "value").toString());
        assertEquals("key=null", Joiner.with(",").appendEntry("key", (CharSequence) null).toString());
        assertEquals("key=null", Joiner.with(",").appendEntry("key", (StringBuilder) null).toString());
        assertEquals("key=null", Joiner.with(",").appendEntry("key", (Object) null).toString());
        assertEquals("null", Joiner.with(",").appendEntry((Map.Entry<?, ?>) null).toString());
        assertEquals("null1=null,null2=null,null3=null,null4=null,null5=null",
                Joiner.with(",")
                        .appendEntry("null1", (String) null)
                        .appendEntry("null2", (CharSequence) null)
                        .appendEntry("null3", (StringBuilder) null)
                        .appendEntry("null4", (char[]) null)
                        .appendEntry("null5", (Object) null)
                        .toString());
        assertEquals("keyvalue", Joiner.with(",", "").appendEntry("key", "value").toString());
        assertEquals("keyvalue", Joiner.with(",", "").appendEntry("key", new StringBuilder("value")).toString());
        assertEquals("keyvalue", Joiner.with(",", "").trimBeforeAppend().appendEntry("  key  ", new StringBuilder("  value  ")).toString());
        assertEquals("keynull", Joiner.with(",", "").appendEntry("key", (StringBuilder) null).toString());
        assertEquals("flagtrue", Joiner.with(",", "").appendEntry("flag", true).toString());
        assertEquals("letterA", Joiner.with(",", "").appendEntry("letter", 'A').toString());
        assertEquals("count42", Joiner.with(",", "").appendEntry("count", 42).toString());
        assertEquals("id123", Joiner.with(",", "").appendEntry("id", 123L).toString());
        assertEquals("val1.5", Joiner.with(",", "").appendEntry("val", 1.5f).toString());
        assertEquals("pi3.14", Joiner.with(",", "").appendEntry("pi", 3.14).toString());
        assertEquals("nameAlice", Joiner.with(",", "").appendEntry("name", "Alice").toString());
        assertEquals("num99", Joiner.with(",", "").appendEntry("num", (Object) 99).toString());

        final String nullText = "  NIL  ";
        final Map.Entry<String, String> entry = new AbstractMap.SimpleImmutableEntry<>(null, null);
        assertEquals("  NIL  =  NIL  ", Joiner.with(",", "=").trimBeforeAppend().useForNull(nullText).appendEntry(entry).toString());
        assertEquals("  NIL  ", Joiner.with(",", "=").trimBeforeAppend().useForNull(nullText).appendEntry((Map.Entry<?, ?>) null).toString());
    }

    @Test
    public void testAppendEntries() {
        final Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        map.put("c", "3");
        map.put("d", "4");
        assertEquals("a=1,b=2,c=3,d=4", Joiner.with(",").appendEntries(map).toString());
        assertEquals("b=2,c=3", Joiner.with(",").appendEntries(map, 1, 3).toString());
        assertEquals("a: 1; b: 2; c: 3; d: 4", Joiner.with("; ", ": ").appendEntries(map).toString());

        final Map<String, Integer> ab = new LinkedHashMap<>();
        ab.put("a", 1);
        ab.put("b", 2);
        assertEquals("a=1b=2", Joiner.with("", "=").appendEntries(ab).toString());

        final Map<String, Integer> nums = new LinkedHashMap<>();
        nums.put("a", 1);
        nums.put("b", 2);
        nums.put("c", 3);
        assertEquals("b=2,c=3", Joiner.with(",").appendEntries(nums, e -> e.getValue() > 1).toString());
        assertEquals("b=2", Joiner.with(",").appendEntries(nums, (BiPredicate<String, Integer>) (k, v) -> v % 2 == 0).toString());

        final Map<String, Integer> keyed = new LinkedHashMap<>();
        keyed.put("key1", 100);
        keyed.put("key2", 200);
        assertEquals("KEY1=val:100,KEY2=val:200",
                Joiner.with(",").appendEntries(keyed, (Function<String, String>) String::toUpperCase, v -> "val:" + v).toString());

        final Map<String, Integer> hello = new LinkedHashMap<>();
        hello.put("hello", 5);
        hello.put("world", 3);
        assertEquals("HELLO->(5), WORLD->(3)",
                Joiner.with(", ", "->").appendEntries(hello, (Function<String, String>) String::toUpperCase, i -> "(" + i + ")").toString());
    }

    @Test
    public void testAppendEntries_EdgeCase() {
        assertEquals("before,after", Joiner.with(",").append("before").appendEntries(new HashMap<>()).append("after").toString());
        assertEquals("before,after", Joiner.with(",").append("before").appendEntries((Map<?, ?>) null).append("after").toString());
        assertEquals("", Joiner.with(",").appendEntries(Map.of("a", 1, "b", 2), e -> e.getValue() > 10).toString());
        assertEquals("", Joiner.with(",").appendEntries(Map.of("a", 1, "c", 3), (BiPredicate<String, Integer>) (k, v) -> v % 2 == 0).toString());
        assertEquals("", Joiner.with(",").appendEntries(new HashMap<String, Integer>(), e -> true).toString());
        assertEquals("", Joiner.with(",").appendEntries(new HashMap<String, Integer>(), (k, v) -> true).toString());
        assertEquals("", Joiner.with(",").appendEntries(new HashMap<String, Integer>(), String::toUpperCase, v -> v * 2).toString());
        assertEquals("", Joiner.with(", ", "=").appendEntries(new LinkedHashMap<>(Map.of("a", 1, "b", 2)), 1, 1).toString());

        final Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "1");
        map.put("b", "2");
        assertThrows(IndexOutOfBoundsException.class, () -> Joiner.with(",").appendEntries(map, 1, 3));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendEntries(map, (Predicate<Map.Entry<String, String>>) null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendEntries(map, (BiPredicate<String, String>) null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendEntries(map, null, Function.identity()));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendEntries(map, Function.identity(), null));
    }

    @Test
    public void testAppendBean() {
        final Person person = new Person("John", 30, "NYC", 50000.0);
        final String all = Joiner.with(",").appendBean(person).toString();
        assertTrue(all.contains("name=John"));
        assertTrue(all.contains("age=30"));
        assertTrue(all.contains("city=NYC"));
        assertTrue(all.contains("salary=50000.0"));

        final String selected = Joiner.with(",").appendBean(person, Arrays.asList("name", "age")).toString();
        assertTrue(selected.contains("name=John"));
        assertTrue(selected.contains("age=30"));
        assertFalse(selected.contains("city"));
        assertFalse(selected.contains("salary"));

        final String ignored = Joiner.with(",").appendBean(person, false, new HashSet<>(Arrays.asList("city", "salary"))).toString();
        assertTrue(ignored.contains("name=John"));
        assertTrue(ignored.contains("age=30"));
        assertFalse(ignored.contains("city"));
        assertFalse(ignored.contains("salary"));

        final String filtered = Joiner.with(",").appendBean(person, (propName, propValue) -> "name".equals(propName) || "age".equals(propName)).toString();
        assertTrue(filtered.contains("name=John"));
        assertTrue(filtered.contains("age=30"));
        assertFalse(filtered.contains("city"));

        assertEquals("name=test&value=123", Joiner.with("&").appendBean(new TestBean()).toString());
        assertEquals("value=123, name=test", Joiner.with(", ").appendBean(new TestBean(), Arrays.asList("value", "name")).toString());
    }

    @Test
    public void testAppendBean_EdgeCase() {
        assertEquals("before,after", Joiner.with(",").append("before").appendBean((Object) null).append("after").toString());
        assertEquals("", Joiner.with(",").appendBean(null, Arrays.asList("name")).toString());
        assertEquals("", Joiner.with(",").appendBean(null, false, new HashSet<>()).toString());
        assertEquals("", Joiner.with(",").appendBean(null, (k, v) -> true).toString());
        assertEquals("", Joiner.with(",").appendBean(new Person("John", 30, "NYC", 50000.0), Collections.emptyList()).toString());
        assertEquals("", Joiner.with(",").appendBean(new Person("John", 30, "NYC", 50000.0), (prop, val) -> false).toString());

        final String ignoreNull = Joiner.with(",").appendBean(new Person("John", 30, null, null), true, null).toString();
        assertTrue(ignoreNull.contains("name=John"));
        assertTrue(ignoreNull.contains("age=30"));
        assertFalse(ignoreNull.contains("city"));
        assertFalse(ignoreNull.contains("salary"));

        final String ignoreSet = Joiner.with(",").appendBean(new Person("Bob", null, "LA", 60000.0), true, new HashSet<>(Arrays.asList("salary"))).toString();
        assertTrue(ignoreSet.contains("name=Bob"));
        assertFalse(ignoreSet.contains("age="));
        assertFalse(ignoreSet.contains("salary="));

        assertEquals("name=test&value=123", Joiner.with("&").appendBean(new TestBean(), true, null).toString());
        assertThrows(IllegalArgumentException.class,
                () -> Joiner.with(",").appendBean(new Person("John", 30, "NYC", 50000.0), (BiPredicate<String, Object>) null));
        assertThrows(IllegalArgumentException.class, () -> Joiner.with(",").appendBean(Map.of("a", "1"), (BiPredicate<String, Object>) null));
    }

    @Test
    public void testAppendTo() throws IOException {
        final StringBuilder sb = new StringBuilder("prefix:");
        Joiner.with(",").append("a").append("b").append("c").appendTo(sb);
        assertEquals("prefix:a,b,c", sb.toString());

        final StringWriter writer = new StringWriter();
        writer.write("prefix:");
        Joiner.with(",").append("a").append("b").append("c").appendTo(writer);
        assertEquals("prefix:a,b,c", writer.toString());

        final StringBuilder withPrefix = new StringBuilder("data:");
        Joiner.with(",", "[", "]").append("a").append("b").appendTo(withPrefix);
        assertEquals("data:[a,b]", withPrefix.toString());
    }

    /**
     * R10 review of r9506: {@code append(Object)} was changed to call {@code assertNotClosed()} before rendering,
     * so that a closed Joiner never runs the element's {@code toString()}. Its one-element sibling
     * {@code appendIfNotNull(Object)} kept the hoisted rendering but not the guard, even though its comment says
     * "see append(Object)" - so the two disagreed about whether a refused write may still have side effects.
     */
    @Test
    public void testAppendIfNotNull_closedJoinerRefusesBeforeRenderingTheElement() {
        final int[] renders = { 0 };
        final Object counted = new Object() {
            @Override
            public String toString() {
                renders[0]++;

                return "x";
            }
        };

        final Joiner closed = Joiner.with(", ").append("a");
        closed.close();

        assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed.appendIfNotNull(counted)).getMessage());
        assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed.append(counted)).getMessage());
        assertEquals(0, renders[0], "a closed Joiner must not render the element it is going to refuse");

        // closed is checked before the null skip, so a null element is refused too
        assertEquals("Joiner has been closed", assertThrows(IllegalStateException.class, () -> closed.appendIfNotNull(null)).getMessage());

        // and an open Joiner is unaffected
        assertEquals("a, x", Joiner.with(", ").append("a").appendIfNotNull(counted).toString());
        assertEquals(1, renders[0]);
    }

    @Test
    public void testAppendTo_EdgeCase() throws IOException {
        final StringBuilder empty = new StringBuilder("result:");
        Joiner.with(",").appendTo(empty);
        assertEquals("result:", empty.toString());

        final StringBuilder emptyValue = new StringBuilder("prefix");
        Joiner.with(",").setEmptyValue("").appendTo(emptyValue);
        assertEquals("prefix", emptyValue.toString());

        final StringBuilder namedEmpty = new StringBuilder("prefix:");
        Joiner.with(",").setEmptyValue("EMPTY").appendTo(namedEmpty);
        assertEquals("prefix:EMPTY", namedEmpty.toString());
    }

    /**
     * Pins the {@code null}-key rendering that the {@code appendEntry} javadoc now states on the six
     * primitive-value overloads and on the {@code Object} overload: the key goes through the same
     * {@code format} step as everywhere else, so a {@code null} key becomes the configured {@code null} text and
     * is never skipped - {@link Joiner#skipNulls()} governs values, not keys.
     */
    @Test
    public void aNullEntryKeyIsRenderedWithTheConfiguredNullTextAndNeverSkipped() {
        assertEquals("null=true", Joiner.with(", ").appendEntry(null, true).toString());
        assertEquals("null=A", Joiner.with(", ").appendEntry(null, 'A').toString());
        assertEquals("null=42", Joiner.with(", ").appendEntry(null, 42).toString());
        assertEquals("null=42", Joiner.with(", ").appendEntry(null, 42L).toString());
        assertEquals("null=1.5", Joiner.with(", ").appendEntry(null, 1.5f).toString());
        assertEquals("null=2.5", Joiner.with(", ").appendEntry(null, 2.5d).toString());
        assertEquals("null=v", Joiner.with(", ").appendEntry((String) null, (Object) "v").toString());

        // useForNull() governs the key as well as the value.
        assertEquals("NIL=42", Joiner.with(", ").useForNull("NIL").appendEntry(null, 42).toString());
        assertEquals("NIL=true", Joiner.with(", ").useForNull("NIL").appendEntry(null, true).toString());
        assertEquals("NIL=NIL", Joiner.with(", ").useForNull("NIL").appendEntry((String) null, (Object) null).toString());

        // skipNulls() does NOT skip a null key.
        assertEquals("null=42", Joiner.with(", ").skipNulls().appendEntry(null, 42).toString());
        assertEquals("null=v", Joiner.with(", ").skipNulls().appendEntry((String) null, "v").toString());

        // Control: the trim/strip half of the same sentence still applies to the key.
        assertEquals("k=42", Joiner.with(", ").trimBeforeAppend().appendEntry("  k  ", 42).toString());
        assertEquals("k=42", Joiner.with(", ").stripBeforeAppend().appendEntry("  k  ", 42).toString());
    }
}
