package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.commons.collections4.SetUtils;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Sets;
import com.landawn.abacus.AbstractParserTest;
import com.landawn.abacus.entity.PersonType;
import com.landawn.abacus.entity.PersonsType;
import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.entity.extendDirty.basic.AccountContact;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL;
import com.landawn.abacus.entity.extendDirty.basic.ExtendDirtyBasicPNL.AccountPNL;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.parser.XMLDeserializationConfig.XDC;
import com.landawn.abacus.parser.XMLSerializationConfig;
import com.landawn.abacus.parser.XMLSerializationConfig.XSC;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.types.JAXBean;
import com.landawn.abacus.types.WeekDay;
import com.landawn.abacus.util.Iterables.SetView;
import com.landawn.abacus.util.Strings.StrUtil;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BooleanPredicate;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.stream.Stream;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

public class NTest extends AbstractParserTest {

    @Test
    public void test_misMatch() {

        assertEquals(Double.valueOf(2), CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));
        assertEquals(2D, CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3)));

        double d = CommonUtil.defaultIfNull(Double.valueOf(2), Double.valueOf(3));
        N.println(d);

        final int len = 1;
        int[] a = Array.range(0, len);
        int[] b = Array.range(0, len);
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a[len - 1] = 0;
        b[len - 1] = 1;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        a = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(0, Arrays.mismatch(a, b));
        assertEquals(0, CommonUtil.mismatch(a, b));
        b = CommonUtil.EMPTY_INT_ARRAY;
        assertEquals(-1, Arrays.mismatch(a, b));
        assertEquals(-1, CommonUtil.mismatch(a, b));
        a = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
        b = null;
        assertEquals(-1, CommonUtil.mismatch(a, b));
    }

    @Test
    public void test_compare_perf() {
        final int len = 1000;
        final int[] a = Array.range(0, len);
        final int[] b = Array.range(0, len);
        a[len - 1] = 0;
        b[len - 1] = 1;

        assertEquals(-1, CommonUtil.compare(a, b));
        assertEquals(-1, Arrays.compare(a, b));

        Profiler.run(1, 1000, 3, "N.compare(...)", () -> assertEquals(-1, CommonUtil.compare(a, b))).printResult();
        Profiler.run(1, 1000, 3, "Arrays.compare(...)", () -> assertEquals(-1, Arrays.compare(a, b))).printResult();
    }

    @Test
    public void test_splitToCount() {

        final int[] a = Array.range(1, 8);

        {
            List<int[]> result = N.splitByChunkCount(7, 5, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3], [4, 5], [6, 7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(7, 5, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5], [6], [7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(3, 5, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(3, 5, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(6, 3, true, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5, 6]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(6, 3, false, (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex));
            assertEquals("[[1, 2], [3, 4], [5, 6]]", CommonUtil.toString(result));
        }

        {
            Collection<Integer> c = CommonUtil.asLinkedHashSet(1, 2, 3, 4, 5, 6, 7);

            List<List<Integer>> result = N.splitByChunkCount(c, 5, true);
            assertEquals("[[1], [2], [3], [4, 5], [6, 7]]", CommonUtil.toString(result));

            result = N.splitByChunkCount(c, 5, false);
            assertEquals("[[1, 2], [3, 4], [5], [6], [7]]", CommonUtil.toString(result));

            c = CommonUtil.asLinkedHashSet(1, 2, 3);

            result = N.splitByChunkCount(c, 5, true);
            assertEquals("[[1], [2], [3]]", CommonUtil.toString(result));

        }

    }

    @Test
    public void test_PermutationIterator() {
        PermutationIterator.of(CommonUtil.asList(1, 2, null, 3)).forEachRemaining(Fn.println());

        Iterables.powerSet(CommonUtil.asSet(1, 2, null, 3)).forEach(Fn.println());
    }

    @Test
    public void test_stream_persist_json() throws IOException {
        final List<Map<String, Object>> list = new ArrayList<>();
        list.add(CommonUtil.asMap("a", 1));

        final File file = new File("./a.json");

        Stream.of(list).persistToJSON(new File("./a.json"));

        final String json = IOUtil.readAllToString(file);

        N.println(json);

        file.delete();
    }

    @Test
    public void test_asynCall() {

        {

            final List<Callable<Integer>> commands = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                commands.add(() -> {
                    final int ret = Math.abs(CommonUtil.RAND.nextInt(10)) * 200;
                    N.sleepUninterruptibly(ret);

                    N.println(ret);

                    return ret;
                });
            }

            final List<Integer> result = N.asynCall(commands).toList();

            N.println(result);

            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i + 1) >= result.get(i));
            }
        }

        {

            final List<Callable<Integer>> commands = new ArrayList<>();

            for (int i = 0; i < 20; i++) {
                commands.add(() -> {
                    final int ret = Math.abs(CommonUtil.RAND.nextInt(10)) * 200;
                    N.sleepUninterruptibly(ret);

                    if (ret == 1000) {
                        System.out.println("ERROR: **************: " + ret);
                        throw new RuntimeException("ERROR: **************: " + ret);
                    }

                    N.println(ret);

                    return ret;
                });
            }

            final List<Integer> result = N.asynCall(commands).limit(2).toList();

            N.println(result);

            for (int i = 0; i < result.size() - 1; i++) {
                assertTrue(result.get(i + 1) >= result.get(i));
            }
        }

    }

    @Test
    public void test_indexOf() {
        {
            final double[] a = { Double.NEGATIVE_INFINITY, Double.NaN, Double.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Double.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Double.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Double.NaN));
        }

        {
            final float[] a = { Float.NEGATIVE_INFINITY, Float.NaN, Float.POSITIVE_INFINITY };
            assertTrue(N.contains(a, Float.POSITIVE_INFINITY));
            assertTrue(N.contains(a, Float.NEGATIVE_INFINITY));
            assertTrue(N.contains(a, Float.NaN));
        }
    }

    @Test
    public void test_occurrencesMap() {
        final Map<String, Integer> map = N.occurrencesMap(CommonUtil.asList("a", "b", "a", "c", "a", "D", "b"));

        N.println(map);
    }

    @Test
    public void test_applyToEach() {

        {
            final String[] a = CommonUtil.asArray("a ", "b", " c");
            N.replaceAll(a, Strings::trim);
            N.println(a);
        }

        {
            final List<String> c = CommonUtil.asList("a ", "b", " c");
            N.replaceAll(c, Strings::trim);
            N.println(c);
        }

        {
            final List<String> c = CommonUtil.asLinkedList("a ", "b", " c");
            N.replaceAll(c, Strings::trim);
            N.println(c);
        }
    }

    @Test
    public void test_isSorted() throws Exception {
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 3, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 3, 5, 5, 7, 9, 10)));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 3, 5)));
        assertFalse(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 7, 5)));
        assertFalse(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 2, 3, 7, 5)));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 2, 3, 7, 5), 1, 3));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 3, 7, 5), 0, 4));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 3, 7, 5), 2, 4));
        assertTrue(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 3, 7, 5), 3, 4));
        assertFalse(CommonUtil.isSorted(CommonUtil.asLinkedHashSet(1, 2, 3, 7, 5), 2, 5));

        assertFalse(CommonUtil.isSorted(new int[] { 1, 7, 5 }));
        assertFalse(CommonUtil.isSorted(new int[] { 1, 2, 2, 3, 7, 5 }));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 2, 3, 7, 5 }, 1, 3));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 0, 4));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 2, 4));
        assertTrue(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 3, 4));
        assertFalse(CommonUtil.isSorted(new int[] { 1, 2, 3, 7, 5 }, 2, 5));
    }

    @Test
    public void test_range() throws Exception {
        N.println(Array.range(0, 10));
        N.println(Array.rangeClosed(0, 10));
        N.println(Array.range(0, 10, -1));
        N.println(Array.range(10, 0, -1));
        N.println(Array.rangeClosed(10, 0, -1));
        N.println(Array.range(10, 0, 1));
    }

    @Test
    public void test_occurrencesOf() throws Exception {
        assertEquals(1, N.occurrencesOf("aaa", "aa"));
        assertEquals(1, N.occurrencesOf("ababaab", "aa"));
        assertEquals(4, N.occurrencesOf("ababaab", "a"));

    }

    @Test
    public void test_cartesianProduct() throws Exception {
        Sets.cartesianProduct(CommonUtil.asList(CommonUtil.asSet("a", "b"), CommonUtil.asSet("a", "b", "c"))).forEach(Fn.println());

        N.println(Strings.repeat('=', 80));

        Iterables.cartesianProduct(CommonUtil.asList(CommonUtil.asSet("a", "b"), CommonUtil.asSet("a", "b", "c"))).forEach(Fn.println());
    }

    @Test
    public void test_SetView() throws Exception {
        final Set<?> set1 = CommonUtil.asSet("a", "c", "d");
        final Set<?> set2 = CommonUtil.asSet("b", "a", "c");
        N.println(set1);
        N.println(set2);

        N.println(Strings.repeat('=', 80));

        SetView<?> setView = Iterables.union(set1, set2);
        N.println(setView);

        setView = Iterables.union(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.difference(set1, set2);
        N.println(setView);

        setView = Iterables.difference(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.intersection(set1, set2);
        N.println(setView);

        setView = Iterables.intersection(set2, set1);
        N.println(setView);

        N.println(Strings.repeat('=', 80));

        setView = Iterables.symmetricDifference(set1, set2);
        N.println(setView);

        setView = Iterables.symmetricDifference(set2, set1);
        N.println(setView);
    }

    @Test
    public void test_subSet() throws Exception {
        final Set<?> a = CommonUtil.asSet("a", "c");
        final Set<?> b = CommonUtil.asSet("b", "a", "c");

        N.println(N.difference(a, b));
        N.println(SetUtils.difference(a, b));

        N.println(N.difference(b, a));
        N.println(SetUtils.difference(b, a));

        assertTrue(Range.just("a").contains("a"));

        final NavigableSet<String> c = CommonUtil.asNavigableSet("a", "c", "d", "b");

        assertEquals(CommonUtil.asNavigableSet("a"), Iterables.subSet(c, Range.just("a")));

        assertEquals(CommonUtil.asNavigableSet(), Iterables.subSet(c, Range.open("a", "a")));
        assertEquals(CommonUtil.asNavigableSet("a"), Iterables.subSet(c, Range.closed("a", "a")));
        assertEquals(CommonUtil.asNavigableSet(), Iterables.subSet(c, Range.openClosed("a", "a")));
        assertEquals(CommonUtil.asNavigableSet(), Iterables.subSet(c, Range.closedOpen("a", "a")));
        assertEquals(CommonUtil.asNavigableSet("a"), Iterables.subSet(c, Range.closedOpen("a", "b")));
        assertEquals(CommonUtil.asNavigableSet("a", "b"), Iterables.subSet(c, Range.closed("a", "b")));
        assertEquals(CommonUtil.asNavigableSet("a", "b"), Iterables.subSet(c, Range.closedOpen("a", "c")));

        N.println(Strings.repeat("=", 80));

        N.println(N.commonSet(a, b));
        N.println(Strings.repeat("=", 80));

    }

    @Test
    public void test_isSubCollection() throws Exception {
        Collection<?> a = CommonUtil.asList("a", "b", "c");
        final Collection<?> b = CommonUtil.asSet("b", "a", "c");

        assertTrue(N.isEqualCollection(a, b));
        assertTrue(N.isSubCollection(a, b));
        assertTrue(N.isSubCollection(b, a));
        assertFalse(N.isProperSubCollection(b, a));

        a = CommonUtil.asList("a", "b", "c", "a");
        assertFalse(N.isProperSubCollection(a, b));
        assertTrue(N.isProperSubCollection(b, a));
    }

    @Test
    public void test_clone_01() {
        Beans.clone(u.Optional.of("a"));
        Beans.clone(u.Nullable.of("a"));
    }

    @Test
    public void test_removeDuplicates() {
        List<String> c = CommonUtil.asList("a", "a");
        N.removeDuplicates(c);
        assertEquals(1, c.size());

        c = CommonUtil.asLinkedList("a", "a");
        N.removeDuplicates(c);
        assertEquals(1, c.size());
    }

    @Test
    public void test_firstNonEmpty() {
        final Optional<List<String>> result = CommonUtil.firstNonEmpty(CommonUtil.asList(), CommonUtil.asList("a"), CommonUtil.asList());
        N.println(result);
    }

    @Test
    public void test_replaceRange() {
        {
            final int[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
            N.println(N.replaceRange(a, 1, 5, Array.of(3, 4, 5)));

        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            N.println(N.replaceRange(str, 7, 9, "00000"));

        }
    }

    @Test
    public void test_moveRanger() {
        {
            for (int i = 0; i < 7; i++) {
                final byte[] a = { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
                N.moveRange(a, 3, 6, i);
                N.println(a);
            }
        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            for (int i = 0; i < 7; i++) {
                N.println(i + ": " + N.moveRange(str, 3, 6, i));
            }
        }

        N.println(Strings.repeat("=", 80));

        {
            final String str = "123456789";
            for (int i = 0; i < 8; i++) {
                N.println(i + ": " + N.moveRange(str, 1, 3, i));
            }
        }
    }

    @Test
    public void test_repeat() {
        Array.repeat((String) null, 10);
    }

    @Test
    public void test_toString_2() {
        N.println(CommonUtil.toString((int[]) null));
        N.println(CommonUtil.toString((int[][]) null));
        N.println(CommonUtil.toString((int[][]) null));
        N.println(CommonUtil.toString(new int[0]));
        N.println(CommonUtil.toString(new int[0][]));
        N.println(CommonUtil.toString(new int[0][][]));
        N.println(CommonUtil.toString(new int[1]));
        N.println(CommonUtil.toString(new int[1][]));
        N.println(CommonUtil.toString(new int[1][][]));

    }

    @Test
    public void test_as() {
        List<String> list = CommonUtil.asList("a");
        N.println(list);

        list = CommonUtil.asList("a", "b");
        N.println(list);

        list = CommonUtil.asList(Array.of("a", "b", "c"));
        N.println(list);
    }

    @Test
    public void test_concat_01() {
        final String[] abc = N.concat(CommonUtil.asArray("a", "b"), CommonUtil.asArray("c"));
        N.println(abc);
        final List<String> ab123 = N.concat(CommonUtil.asList("a", "b"), CommonUtil.asList("1", "2", "3"));
        N.println(ab123);
        final List<String> ab = N.concat(CommonUtil.asList("a", "b"));
        N.println(ab);
        final List<String> abcd = N.concat(CommonUtil.asList(CommonUtil.asList("a", "b", "c", "d")));
        N.println(abcd);
    }

    @Test
    public void test_json_nullable() {
        final Map<String, Nullable<Integer>> map = CommonUtil.asMap("a", Nullable.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, Nullable<Integer>> map2 = N.fromJson(json, CommonUtil.<Map<String, Nullable<Integer>>> typeOf("Map<String, Nullable<Integer>>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_json_optional() {
        final Map<String, Optional<Integer>> map = CommonUtil.asMap("a", Optional.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, Optional<Integer>> map2 = N.fromJson(json, CommonUtil.<Map<String, Optional<Integer>>> typeOf("Map<String, Optional<Integer>>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_json_OptionalDouble() {
        final Map<String, OptionalDouble> map = CommonUtil.asMap("a", OptionalDouble.of(12));
        final String json = N.toJson(map);
        N.println(json);
        final Map<String, OptionalDouble> map2 = N.fromJson(json, CommonUtil.<Map<String, OptionalDouble>> typeOf("Map<String, OptionalDouble>"));
        N.println(map2);
        assertEquals(map, map2);
    }

    @Test
    public void test_delete() {
        final int[] a = { 1, 2, 3, 4, 5 };
        final int[] b = N.deleteAllByIndices(a, 0, 0, 1, 3);
        assertTrue(CommonUtil.equals(Array.of(3, 5), b));
    }

    @Test
    public void test_0003() {
        String str = "-a-";

        assertEquals("a", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("a", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));

        str = "--";

        assertEquals("", StrUtil.substringBetween(str, str.indexOf("-"), "-").orElse(null));
        assertEquals("", StrUtil.substringBetween(str, "-", str.lastIndexOf("-")).orElse(null));
    }

    @Test
    public void test_002() {
        try {
            Try.call((Callable<Object>) () -> {
                throw new Exception();
            });
        } catch (final RuntimeException e) {

        }

        try {
            Try.run((Throwables.Runnable) () -> {
                throw new Exception();
            });
        } catch (final RuntimeException e) {

        }
    }

    @Test
    public void test_001() {
        final Account account = Beans.fill(Account.class);
        N.println(account);
    }

    @Test
    public void test_difference() {
        {
            final int[] a = { 0, 1, 2, 2, 3 };
            final int[] b = { 2, 5, 1 };
            final int[] c = N.removeAll(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(0, 3)));
        }

        {
            final int[] a = { 0, 1, 2, 2, 3 };
            final int[] b = { 2, 5, 1 };
            int[] c = N.difference(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(0, 2, 3)));

            c = N.intersection(a, b);
            assertTrue(CommonUtil.equals(c, Array.of(1, 2)));
        }

        {
            IntList a = IntList.of(0, 1, 2, 2, 3);
            IntList b = IntList.of(2, 5, 1);
            a.removeAll(b);
            assertTrue(CommonUtil.equals(a, IntList.of(0, 3)));

            a = IntList.of(0, 1, 2, 2, 3);
            b = IntList.of(2, 5, 1);
            a.retainAll(b);
            assertTrue(CommonUtil.equals(a, IntList.of(1, 2, 2)));
        }

        {
            final IntList a = IntList.of(0, 1, 2, 2, 3);
            final IntList b = IntList.of(2, 5, 1);
            IntList c = a.difference(b);
            assertTrue(CommonUtil.equals(c, IntList.of(0, 2, 3)));

            c = a.intersection(b);
            assertTrue(CommonUtil.equals(c, IntList.of(1, 2)));
        }

        {
            final IntList a = IntList.of(0, 1, 2, 2, 3);
            final IntList b = IntList.of(2, 5, 1);
            final IntList c = a.symmetricDifference(b);
            assertTrue(CommonUtil.equals(c, IntList.of(0, 2, 3, 5)));
        }
    }

    @Test
    public void test_copyOfRange() {
        {
            final int[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final int[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1, 3, 5)));
        }

        {
            final long[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final long[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1L, 3, 5)));
        }

        {
            final float[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final float[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1F, 3, 5)));
        }

        {
            final double[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final double[] b = CommonUtil.copyOfRange(a, 1, 6, 2);
            assertTrue(CommonUtil.equals(b, Array.of(1D, 3, 5)));
        }
    }

    @Test
    public void test_split() {
        {
            final int[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<int[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1, 2], [3, 4], [5]]", CommonUtil.stringOf(list));
        }

        {
            final long[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<long[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1, 2], [3, 4], [5]]", CommonUtil.stringOf(list));
        }

        {
            final float[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<float[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1.0, 2.0], [3.0, 4.0], [5.0]]", CommonUtil.stringOf(list));
        }

        {
            final double[] a = { 0, 1, 2, 3, 4, 5, 6 };
            final List<double[]> list = N.split(a, 1, 6, 2);
            assertEquals("[[1.0, 2.0], [3.0, 4.0], [5.0]]", CommonUtil.stringOf(list));
        }
    }

    @Test
    public void test_pair() {
        final Pair<Integer, String> pair = Pair.of(1, "abc");
        N.println(pair);
        final Triple<Integer, Character, String> triple = Triple.of(1, 'c', "abc");
        N.println(triple);
    }

    //
    @Test
    public void test_copy_2() {
        final List<Integer> list1 = CommonUtil.asList(1, 2, 3);
        List<Integer> list2 = CommonUtil.asList(4, 5, 6);
        Iterables.copy(list1, list2);
        assertEquals(list1, list2);

        list2 = CommonUtil.asList(4, 5, 6);
        Iterables.copy(list1, 1, list2, 2, 1);
        assertEquals(CommonUtil.asList(4, 5, 2), list2);
    }

    @Test
    public void test_clone() {
        final int[][] a = { { 1, 2, 3 }, { 4, 5, 6 } };
        int[][] b = a.clone();
        CommonUtil.reverse(b[0]);
        assertTrue(CommonUtil.equals(a[0], b[0]));

        b = CommonUtil.clone(a);
        CommonUtil.reverse(b[0]);
        assertFalse(CommonUtil.equals(a[0], b[0]));

        final String[] c = null;
        N.println(CommonUtil.clone(c));

        final String[][] d = null;
        N.println(CommonUtil.clone(d));

        final String[][][] e = null;
        N.println(CommonUtil.clone(e));

    }

    @Test
    public void test_kthLargest() {
        {
            int[] a = { 1 };
            CommonUtil.shuffle(a);
            assertEquals(1, N.kthLargest(a, 1));

            a = Array.of(1, 2);
            CommonUtil.shuffle(a);
            assertEquals(2, N.kthLargest(a, 1));
            assertEquals(1, N.kthLargest(a, 2));

            a = Array.of(1, 2, 3);
            CommonUtil.shuffle(a);
            assertEquals(3, N.kthLargest(a, 1));
            assertEquals(2, N.kthLargest(a, 2));
            assertEquals(1, N.kthLargest(a, 3));
        }
    }

    @Test
    public void test_lambda() {
        int[] a = Array.repeat(3, 10);
        N.println(a);

        CommonUtil.fill(a, 0);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.reverse(a);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.rotate(a, 2);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        CommonUtil.shuffle(a);
        N.println(a);

        a = Array.of(1, 2, 3, 4, 5, 6);
        N.println(N.sum(a));
        N.println(N.average(a));
        N.println(N.min(a));
        N.println(N.max(a));
        N.println(N.median(a));

    }

    @Test
    public void test_nCopies() {
        N.println(Array.repeat(true, 10));
        N.println(Array.repeat(false, 10));
        N.println(Array.repeat('1', 10));
        N.println(Array.repeat((byte) 1, 10));
        N.println(Array.repeat((short) 1, 10));
        N.println(Array.repeat(1, 10));
        N.println(Array.repeat(1L, 10));
        N.println(Array.repeat(1f, 10));
        N.println(Array.repeat(1d, 10));
        N.println(Array.repeat("1", 10));
    }

    @Test
    public void test_isNumber() {
        N.println(1.2345354);
        N.println(Double.valueOf("2e10"));
        N.println(Numbers.toDouble("2e10"));
        N.println(Numbers.createDouble("2e10"));
        N.println(Numbers.toFloat("2e3"));
        N.println(Numbers.createFloat("2e3"));
        N.println(Numbers.createNumber("2e10"));
    }

    @Test
    public void test_distinct() {
        final Account[] a = { createAccount(Account.class), createAccount(Account.class), createAccount(Account.class) };
        final List<Account> c = CommonUtil.asList(a);

        final List<Account> m = N.distinctBy(a, (Function<Account, String>) Account::getFirstName);

        N.println(m);

        final List<Account> m2 = N.distinctBy(c, (Function<Account, String>) Account::getFirstName);

        N.println(m2);
    }

    @Test
    public void test_asyncExecute() throws InterruptedException, ExecutionException {
        {
            final List<Throwables.Runnable<RuntimeException>> runnableList = CommonUtil.asList((Throwables.Runnable<RuntimeException>) () -> N.println("Runnable"));

            N.asyncExecute(runnableList).get(0).get();
        }

        {
            final List<Throwables.Runnable<RuntimeException>> runnableList = CommonUtil.asList();
            runnableList.add(() -> N.println("Runnable"));

            N.asyncExecute(runnableList).get(0).get();
        }

        {
            final List<Callable<Void>> callableList = CommonUtil.asList((Callable<Void>) () -> {
                N.println("Callable");
                return null;
            });

            N.asyncExecute(callableList).get(0).get();
        }

        {
            final List<Callable<Void>> callableList = CommonUtil.asList();
            callableList.add(() -> {
                N.println("Callable");
                return null;
            });

            N.asyncExecute(callableList).get(0).get();
        }
    }

    @Test
    public void test_occurrences() {
        {
            final boolean[] a = { true, false, true };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final char[] a = { '1', '2', '1' };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final byte[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final short[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final int[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final long[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final float[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final double[] a = { 1, 2, 1 };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final String[] a = { "1", "2", "1" };
            assertEquals(2, N.occurrencesOf(a, a[0]));
            assertEquals(1, N.occurrencesOf(a, a[1]));
        }

        {
            final List<String> list = CommonUtil.asList("1", "2", "1");
            assertEquals(2, N.occurrencesOf(list, list.get(0)));
            assertEquals(1, N.occurrencesOf(list, list.get(1)));
        }
    }

    @Test
    public void test_rotate() {
        {
            final byte[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final byte[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final short[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final short[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final int[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final int[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final long[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final long[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final float[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final float[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final double[] a = { 1, 2, 3, 4, 5, 6 };
            CommonUtil.rotate(a, 2);
            final double[] expected = { 5, 6, 1, 2, 3, 4 };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final String[] a = { "1", "2", "3", "4", "5", "6" };
            CommonUtil.rotate(a, 2);
            final String[] expected = { "5", "6", "1", "2", "3", "4" };

            assertTrue(CommonUtil.equals(a, expected));
        }

        {
            final List<String> a = CommonUtil.asList("1", "2", "3", "4", "5", "6");
            CommonUtil.rotate(a, 2);
            final List<String> expected = CommonUtil.asList("5", "6", "1", "2", "3", "4");

            assertTrue(CommonUtil.equals(a, expected));
        }
    }

    @Test
    public void test_asImmutableMap() {
        final Map<String, String> m = ImmutableMap.of("123", "abc", "234", "ijk");
        N.println(m);

        for (final Map.Entry<String, String> entry : m.entrySet()) {
            N.println(entry.getKey() + ": " + entry.getValue());
        }

    }

    @Test
    public void test_asResultSet() {
        List<?> list = createAccountList(Account.class, 99);
        Dataset rs = CommonUtil.newDataset(list);
        rs.println();

        list = createAccountList(com.landawn.abacus.entity.pjo.basic.Account.class, 99);
        rs = CommonUtil.newDataset(list);
        rs.println();

        final List<?> list2 = createAccountPropsList(79);

        rs = CommonUtil.newDataset(list2);
        rs.println();

        list.addAll((List) list2);
        rs = CommonUtil.newDataset(list);
        rs.println();

        assertEquals(178, rs.size());
    }

    @Test
    public void test_asResultSet_2() {
        final List<Account> beanList = createAccountList(Account.class, 13);
        final Dataset rs1 = CommonUtil.newDataset(beanList);
        rs1.println();

        final List<Map<String, Object>> mapLsit = new ArrayList<>(beanList.size());
        for (final Account account : beanList) {
            mapLsit.add(Beans.bean2Map(account));
        }

        Dataset rs2 = CommonUtil.newDataset(rs1.columnNameList(), mapLsit);
        rs2.println();
        assertEquals(rs1, rs2);

        rs2 = CommonUtil.newDataset(rs1.toList(Map.class));
        rs2.println();

        final Dataset rs3 = CommonUtil.newDataset(rs1.columnNameList(), rs1.toList(Object[].class));
        rs3.println();

        final Dataset rs4 = CommonUtil.newDataset(rs1.columnNameList(), rs1.toList(List.class));
        rs4.println();

    }

    @Test
    public void test_fprintln() {
        Object[] array = null;
        N.println(array);

        array = new Object[0];
        N.println(array);

        array = CommonUtil.asArray("123", "abc", "234", "ijk");
        N.println(array);

        List<?> list = null;
        N.println(list);

        list = new ArrayList<>();
        N.println(list);

        list = CommonUtil.asList("123", "abc", "234", "ijk");
        N.println(list);

        Map<?, ?> map = null;
        N.println(map);

        map = new HashMap<>();
        N.println(map);

        map = CommonUtil.asMap("123", "abc", "234", "ijk");
        N.println(map);
    }

    @Test
    public void test_stringOf() {

        assertEquals(Strings.EMPTY, "abc".substring(1, 1));

        {
            final Multiset<String> multiSet = CommonUtil.asMultiset("1", "2", "3", "2", "3", "3");
            N.println(multiSet);

            final String str = CommonUtil.stringOf(multiSet);

            final Multiset<String> multiSet2 = CommonUtil.valueOf(str, Multiset.class);

            N.println(multiSet2);

            final Multiset<Integer> multiSet3 = (Multiset<Integer>) CommonUtil.typeOf("Multiset<Integer>").valueOf(str);
            N.println(multiSet3);

            final Multiset<Object> multiSet4 = (Multiset<Object>) CommonUtil.typeOf("Multiset<Object>").valueOf(str);
            N.println(multiSet4);
        }

    }

    @Test
    public void test_lastIndexOf() {
        final String str = "aaa";
        N.println(str.lastIndexOf("a"));
        N.println(str.lastIndexOf("a", str.length()));
        N.println(str.lastIndexOf("a", str.length() - 1));
        N.println(str.lastIndexOf("a", str.length() - 2));
    }

    @Test
    public void test_wrap() {
        N.println(Array.box(1, 2, 3));
    }

    @Test
    public void test_getEnumMap() {
        final List<Status> statusList = CommonUtil.enumListOf(Status.class);
        N.println(statusList);

        final Set<Status> statusSet = CommonUtil.enumSetOf(Status.class);
        N.println(statusSet);

        final Map<Status, String> statusMap = CommonUtil.enumMapOf(Status.class);
        N.println(statusMap);
    }

    @Test
    public void test_parallel_sort() {
    }

    @Test
    public void test_sort_big_int_array() {
    }

    @Test
    public void test_sort_big_int_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_long_array() {
    }

    @Test
    public void test_sort_big_long_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_double_array() {
    }

    @Test
    public void test_sort_big_double_array_by_parallel_sort() {
    }

    @Test
    public void test_sort_big_string_array() {
    }

    @Test
    public void test_sort_big_string_array_by_parallel_sort() {
    }

    @Test
    public void test_stringSplit() {
        final String str = "123, daskf32rfasdf, sf3qijfdlsafj23i9jqfl;sdjfawdfs, akfj3iq2pflsefj32ijrlqawdfjiq2, i3q2jfals;dfj32i9qjfsdk, askdaf";

        Profiler.run(1, 1000, 1, () -> Splitter.defauLt().splitToArray(str)).printResult();
    }

    @Test
    public void test_getPackage() {
        N.println(ClassUtil.getClassName(int.class));
        N.println(ClassUtil.getSimpleClassName(int.class));
        N.println(ClassUtil.getCanonicalClassName(int.class));

        N.println(ClassUtil.getPackage(int.class));
        N.println(ClassUtil.getPackage(Integer.class));

        N.println(ClassUtil.getPackageName(int.class));
        N.println(ClassUtil.getPackageName(Integer.class));
    }

    @Test
    public void test_filter() {
        {
            final boolean[] a = { true, false, true };
            final boolean[] b = N.filter(a, (BooleanPredicate) value -> value);

            assertTrue(CommonUtil.equals(Array.of(true, true), b));
        }

        {
            final char[] a = { '1', '2', '3' };
            final char[] b = N.filter(a, (CharPredicate) value -> value > '1');

            assertTrue(CommonUtil.equals(Array.of('2', '3'), b));
        }

        {
            final byte[] a = { 1, 2, 3 };
            final byte[] b = N.filter(a, (BytePredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((byte) 2, (byte) 3), b));
        }

        {
            final short[] a = { 1, 2, 3 };
            final short[] b = N.filter(a, (ShortPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((short) 2, (short) 3), b));
        }

        {
            final int[] a = { 1, 2, 3 };
            final int[] b = N.filter(a, (IntPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of(2, 3), b));
        }

        {
            final long[] a = { 1, 2, 3 };
            final long[] b = N.filter(a, (LongPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((long) 2, (long) 3), b));
        }

        {
            final float[] a = { 1, 2, 3 };
            final float[] b = N.filter(a, (FloatPredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((float) 2, (float) 3), b));
        }

        {
            final double[] a = { 1, 2, 3 };
            final double[] b = N.filter(a, (DoublePredicate) value -> value > 1);

            assertTrue(CommonUtil.equals(Array.of((double) 2, (double) 3), b));
        }

    }

    @Test
    public void test_count() {

        {
            assertEquals(N.count((String[]) null, Fn.isNull()), N.count((String[]) null, Fn.notNull()));
        }

        {
            final boolean[] a = { true, false, true };
            final int count = N.count(a, (BooleanPredicate) value -> value);

            assertEquals(2, count);
        }

        {
            final char[] a = { '1', '2', '3' };
            final int count = N.count(a, (CharPredicate) value -> value > '1');

            assertEquals(2, count);
        }

        {
            final byte[] a = { 1, 2, 3 };
            final int count = N.count(a, (BytePredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final short[] a = { 1, 2, 3 };
            final int count = N.count(a, (ShortPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final int[] a = { 1, 2, 3 };
            final int count = N.count(a, (IntPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final long[] a = { 1, 2, 3 };
            final int count = N.count(a, (LongPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final float[] a = { 1, 2, 3 };
            final int count = N.count(a, (FloatPredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final double[] a = { 1, 2, 3 };
            final int count = N.count(a, (DoublePredicate) value -> value > 1);

            assertEquals(2, count);
        }

        {
            final List<String> list = CommonUtil.asList("a", "b", "c");

            final int count = N.count(list, (Predicate<String>) value -> value.equals("a") || value.equals("b"));

            assertEquals(2, count);
        }

        {
            final String[] array = { "a", "b", "c" };

            final int count = N.count(array, (Predicate<String>) value -> value.equals("a") || value.equals("b"));

            assertEquals(2, count);
        }

        {
            final Map<String, Integer> m = CommonUtil.asMap("a", 1, "b", 2, "c", 3);

            final int count = N.count(m.entrySet(), (Predicate<Entry<String, Integer>>) entry -> entry.getKey().equals("a") || entry.getKey().equals("b"));

            assertEquals(2, count);
        }
    }

    @Test
    public void test_split_2() {

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.asList("a", "b", "c"), N.split(str, 1)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.asList("ab", "c"), N.split(str, 2)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.asList("abc"), N.split(str, 3)));
        }

        {
            final String str = "abc";
            assertTrue(CommonUtil.equals(CommonUtil.asList("abc"), N.split(str, 4)));
        }

        {
            final boolean[] a = { true, false, true };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of(true, false), Array.of(true)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of(true), Array.of(false), Array.of(true)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final char[] a = { '1', '2', '3' };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of('1', '2'), Array.of('3')).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of('1'), Array.of('2'), Array.of('3')).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final byte[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((byte) 1, (byte) 2), Array.of((byte) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((byte) 1), Array.of((byte) 2), Array.of((byte) 3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final short[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((short) 1, (short) 2), Array.of((short) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((short) 1), Array.of((short) 2), Array.of((short) 3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final int[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of(1, 2), Array.of(3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of(1), Array.of(2), Array.of(3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final long[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((long) 1, (long) 2), Array.of((long) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((long) 1), Array.of((long) 2), Array.of((long) 3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final float[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((float) 1, (float) 2), Array.of((float) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((float) 1), Array.of((float) 2), Array.of((float) 3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final double[] a = { 1, 2, 3 };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(Array.of((double) 1, (double) 2), Array.of((double) 3)).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(
                    CommonUtil.deepEquals(CommonUtil.asList(Array.of((double) 1), Array.of((double) 2), Array.of((double) 3)).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 1).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList((Object) CommonUtil.asArray("1", "2", "3")).toArray(new Object[0]), N.split(a, 3).toArray()));
        }

        {
            final String[] a = { "1", "2", "3" };
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1", "2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 2).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asArray("1"), CommonUtil.asArray("2"), CommonUtil.asArray("3")).toArray(new Object[0]), N.split(a, 1).toArray()));
            final Object tmp = CommonUtil.asArray("1", "2", "3");
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(tmp).toArray(), N.split(a, 3).toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(tmp).toArray(), N.split(a, 4).toArray()));
        }

        {
            final List<String> a = CommonUtil.asList("1", "2", "3");
            final List<List<String>> b = N.split(a, 2);
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asList("1", "2"), CommonUtil.asList("3")).toArray(new Object[0]), b.toArray()));
            assertTrue(CommonUtil.deepEquals(CommonUtil.asList(CommonUtil.asList("1"), CommonUtil.asList("2"), CommonUtil.asList("3")).toArray(new Object[0]), N.split(a, 1).toArray()));
        }

    }

    @Test
    public void test_isNumeric() {
        assertFalse(Strings.isAsciiDigitalNumber(null));
        assertFalse(Strings.isAsciiDigitalNumber(""));
        assertFalse(Strings.isAsciiDigitalNumber("  "));
        assertTrue(Strings.isAsciiDigitalNumber("123"));
        assertTrue(Strings.isAsciiDigitalNumber("-123"));
        assertTrue(Strings.isAsciiDigitalNumber("12.3"));
        assertTrue(Strings.isAsciiDigitalNumber("-12.3"));
        assertFalse(Strings.isAsciiDigitalNumber("12..3"));
        assertFalse(Strings.isAsciiDigitalNumber("12ae"));
        assertFalse(Strings.isAsciiDigitalNumber("123l"));
        assertFalse(Strings.isAsciiDigitalNumber("123f"));
    }

    @Test
    public void test_isBlank() {
        assertTrue(Strings.isBlank(null));
        assertTrue(Strings.isBlank(""));
        assertTrue(Strings.isBlank("  "));
        assertTrue(Strings.isBlank(" \n "));
        assertTrue(Strings.isBlank(" \r "));
        assertTrue(Strings.isBlank(" \r \n "));
        assertFalse(Strings.isBlank("12..3"));
        assertFalse(Strings.isBlank("12ae"));
        assertFalse(Strings.isBlank("123l"));
        assertFalse(Strings.isBlank("123f"));
    }

    @Test
    public void test_uuidPerformance() {
        final long startTime = System.currentTimeMillis();
        int k = 0;
        for (int i = 0; i < 1000000; i++) {
            Strings.uuid();
            k++;
        }

        N.println(k + " took: " + (System.currentTimeMillis() - startTime));
    }

    @Test
    public void test_propNameMethod() {
        final Account account = createAccount(Account.class);

        N.println(Beans.getPropValue(account, "firstName"));
        N.println(Beans.getPropValue(account, "firstname"));
        N.println(Beans.getPropValue(account, "FirstName"));
        N.println(Beans.getPropValue(account, "FIRSTNAME"));

        Beans.setPropValue(account, "lastName", "lastName1");
        N.println(Beans.getPropValue(account, "LASTNAME"));

        Beans.setPropValue(account, "lastname", "lastName2");
        N.println(Beans.getPropValue(account, "lastname"));

        Beans.setPropValue(account, "LastName", "lastName3");
        N.println(Beans.getPropValue(account, "LastName"));

        Beans.setPropValue(account, "LASTNAME", "lastName4");
        N.println(Beans.getPropValue(account, "LASTNAME"));
        N.println(Beans.getPropValue(account, "lastName"));
    }

    @Test
    public void test_checkNullOrEmpty() {
        List<String> list = CommonUtil.asList("a");
        list = CommonUtil.checkArgNotEmpty(list, "list");
        N.println(list);

        Set<String> set = CommonUtil.asSet("a");
        set = CommonUtil.checkArgNotEmpty(set, "set");
        N.println(set);

        Queue<String> queue = CommonUtil.asQueue("a");
        queue = CommonUtil.checkArgNotEmpty(queue, "queue");
        N.println(queue);
    }

    @Test
    public void test_array2List() {
        final boolean[] boa = { true, false, false, true, false };
        final List<Boolean> bol = CommonUtil.toList(boa);
        assertEquals(false, bol.get(2).booleanValue());

        final char[] ca = { '3', '2', '1', '4', '5' };
        final List<Character> cl = CommonUtil.toList(ca);
        assertEquals('1', cl.get(2).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        final List<Byte> bl = CommonUtil.toList(ba);
        assertEquals(1, bl.get(2).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        final List<Short> sl = CommonUtil.toList(sa);
        assertEquals(1, sl.get(2).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        final List<Integer> il = CommonUtil.toList(ia);
        assertEquals(1, il.get(2).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        final List<Long> ll = CommonUtil.toList(la);
        assertEquals(1, ll.get(2).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        final List<Float> fl = CommonUtil.toList(fa);
        assertEquals(1, fl.get(2).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        final List<Double> dl = CommonUtil.toList(da);
        assertEquals(1, dl.get(2).intValue());

        final String[] stra = { "3", "2", "1", "4", "5" };
        final List<String> strl = CommonUtil.toList(stra);
        assertEquals("1", strl.get(2));
    }

    @Test
    public void test_max() {
        final char[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('5', ((Character) N.max(ca)).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(ba)).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(sa)).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(ia)).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(la)).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(fa)).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        assertEquals(5, ((Number) N.max(da)).intValue());

        {
            final Iterable<Integer> c = CommonUtil.asList(3, 2, 1, 4, 5);
            assertEquals(5, N.max(c).intValue());
        }
        {
            final Iterable<Integer> c = CommonUtil.asList(3, 2, 1, 4, 5);
            assertEquals(1, N.min(c).intValue());
        }
    }

    @Test
    public void test_max_2() {
        final Character[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('5', N.max(ca).charValue());

        final Byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(ba).intValue());

        final Short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(sa).intValue());

        final Integer[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(5, N.max(ia).intValue());

        final Long[] la = { 3L, 2L, 1L, 4L, 5L };
        assertEquals(5, N.max(la).intValue());

        final Float[] fa = { 3f, 2f, 1f, 4f, 5f };
        assertEquals(5, N.max(fa).intValue());

        final Double[] da = { 3d, 2d, 1d, 4d, 5d };
        assertEquals(5, N.max(da).intValue());
    }

    @Test
    public void test_min() {
        final char[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('1', ((Character) N.min(ca)).charValue());

        final byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(ba)).intValue());

        final short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(sa)).intValue());

        final int[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(ia)).intValue());

        final long[] la = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(la)).intValue());

        final float[] fa = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(fa)).intValue());

        final double[] da = { 3, 2, 1, 4, 5 };
        assertEquals(1, ((Number) N.min(da)).intValue());
    }

    @Test
    public void test_min_2() {
        final Character[] ca = { '3', '2', '1', '4', '5' };
        assertEquals('1', N.min(ca).charValue());

        final Byte[] ba = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(ba).intValue());

        final Short[] sa = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(sa).intValue());

        final Integer[] ia = { 3, 2, 1, 4, 5 };
        assertEquals(1, N.min(ia).intValue());

        final Long[] la = { 3L, 2L, 1L, 4L, 5L };
        assertEquals(1, N.min(la).intValue());

        final Float[] fa = { 3f, 2f, 1f, 4f, 5f };
        assertEquals(1, N.min(fa).intValue());

        final Double[] da = { 3d, 2d, 1d, 4d, 5d };
        assertEquals(1, N.min(da).intValue());
    }

    @Test
    public void test_array_performance() {
    }

    double add_1() {
        final int[] a = new int[1000];

        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        double d = 0;

        for (final int element : a) {
            d += element;
        }

        return d;
    }

    double add_2() {
        final int[] a = new int[1000];

        for (int i = 0; i < a.length; i++) {
            a[i] = i;
        }

        return N.sum(a);
    }

    @Test
    public void test_arrayOf() {
        N.println(Array.of(false, true));
        N.println(Array.of('a', 'b'));
        N.println(Array.of((byte) 1, (byte) 2));
        N.println(Array.of((short) 1, (short) 2));
        N.println(Array.of(1, 2));
        N.println(Array.of(1L, 2L));
        N.println(Array.of(1f, 2f));
        N.println(Array.of(1d, 2d));
        N.println(CommonUtil.asArray(Dates.currentJUDate(), Dates.currentDate()));
        N.println(CommonUtil.asArray(Dates.currentCalendar(), Dates.currentCalendar()));

        final String a1 = "a";
        final String b1 = "b";
        final List<String> list = CommonUtil.asList(a1, b1);
        N.println(list);

        final List<Integer> list2 = CommonUtil.asList(1, 2, 3);
        N.println(list2);

        final int[] a = Array.of(1, 2, 3);
        N.println(a);

        final Class<?>[] classes = CommonUtil.asArray(String.class, Integer.class);
        N.println(classes);

        final Type<Object>[] types = CommonUtil.asArray(CommonUtil.typeOf(int.class), CommonUtil.typeOf(long.class));
        N.println(types);

        final Date[] dates = CommonUtil.asArray(Dates.currentDate(), Dates.currentDate());
        N.println(dates);

        final java.util.Date[] dateTimes = CommonUtil.asArray(Dates.currentDate(), Dates.currentTime());
        N.println(dateTimes);

        final Status[] status = CommonUtil.asArray(Status.ACTIVE, Status.CANCELED);
        N.println(status);

        N.println(ClassUtil.getCanonicalClassName(int.class));
    }

    @Test
    public void test_compare() {
        final int result = CommonUtil.compare("a", "bc", (Comparator<String>) String::compareTo);

        assertEquals(-1, result);
    }

    @Test
    public void test_requireNonNull() {
    }

    @Test
    public void test_Collections() {
        N.println(CommonUtil.asSingletonSet("abc"));
        N.println(CommonUtil.asSingletonList("abc"));
        N.println(CommonUtil.asSingletonMap("key", "value"));

        final List<String> list = CommonUtil.asList("a", "b", "c", "d");
        N.println(list);
        CommonUtil.reverse(list);
        N.println(list);
        N.replaceAll(list, "a", "newValue");
        N.println(list);
    }

    @Test
    public void test_format() {

        N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentJUDate(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentTime(), Dates.LOCAL_DATE_TIME_FORMAT));
        N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.format(Dates.currentTimestamp(), Dates.LOCAL_DATE_TIME_FORMAT));

        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate())));
        com.landawn.abacus.util.BufferedWriter writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        Dates.formatTo(Dates.currentDate(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);

        writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));

        Dates.formatTo(Dates.currentCalendar(), null, null, writer);
        Dates.formatTo(Dates.currentCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);

        writer = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar())));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT), Dates.LOCAL_DATE_FORMAT));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        N.println(Dates.parseTimestamp(Dates.format(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE), Dates.LOCAL_DATE_FORMAT,
                Dates.UTC_TIME_ZONE));
        Dates.formatTo(Dates.currentXMLGregorianCalendar(), null, null, writer);
        Dates.formatTo(Dates.currentXMLGregorianCalendar(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE, writer);
        N.println(Dates.parseTimestamp(writer.toString(), Dates.LOCAL_DATE_FORMAT, Dates.UTC_TIME_ZONE));
        Objectory.recycle(writer);
    }

    @Test
    public void test_isPrimaryType() {

        assertTrue(Strings.isAsciiDigitalNumber("123"));
        assertTrue(Strings.isAsciiDigitalNumber("-123"));
        assertTrue(Strings.isAsciiDigitalNumber("90239.0329"));
        assertFalse(Strings.isAsciiDigitalNumber("90239.0329f"));
    }

    @Test
    public void test_encode_decode_2() {
        final String str = "ůůůůů";
        N.println(Strings.base64UrlEncode(str.getBytes()));

        N.println(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(str.getBytes()));

        final String string = "This string encoded will be longer that 76 characters and cause MIME base64 line folding";

        System.out.println("commons-codec JDK8Base64.encodeBase64\n" + Strings.base64Encode(string.getBytes()));

        assertEquals(string, Strings.base64DecodeToString(Strings.base64Encode(string.getBytes())));
    }

    @Test
    public void test_toString() {
        assertEquals("true", CommonUtil.toString(true));
        assertEquals("1", CommonUtil.toString('1'));
        assertEquals("1", CommonUtil.toString((byte) 1));
        assertEquals("1", CommonUtil.toString((short) 1));
        assertEquals("1", CommonUtil.toString(1));
        assertEquals("1", CommonUtil.toString(1L));
        assertEquals("1.0", CommonUtil.toString(1f));
        assertEquals("1.0", CommonUtil.toString(1d));
        assertEquals("[a, b]", CommonUtil.toString(new String[] { "a", "b" }));
        assertEquals("[a, b]", CommonUtil.deepToString(new String[] { "a", "b" }));

        assertEquals("[[false, true], [a, b], [1, 2], [1, 2], [1, 2], [1, 2], [1.0, 2.0], [1.0, 2.0], [a, bc]]",
                CommonUtil.deepToString(new Object[] { new boolean[] { false, true }, new char[] { 'a', 'b' }, new byte[] { 1, 2 }, new short[] { 1, 2 },
                        new int[] { 1, 2 }, new long[] { 1, 2 }, new float[] { 1, 2 }, new double[] { 1, 2 }, new String[] { "a", "bc" } }));

        final Object obj = new String[] { "a", "b" };
        assertEquals("[a, b]", CommonUtil.toString(obj));
        assertEquals("[a, b]", CommonUtil.deepToString(obj));

        assertEquals(Strings.NULL, CommonUtil.deepToString((Object) null));

        N.println(CommonUtil.deepToString(Dates.currentDate()));
    }

    @Test
    public void test_convert() {
        {
            CommonUtil.convert(12L, byte.class);
            CommonUtil.convert(12L, Byte.class);
            CommonUtil.convert(12L, short.class);
            CommonUtil.convert(12L, Short.class);
            CommonUtil.convert(12L, int.class);
            CommonUtil.convert(12L, Integer.class);
            CommonUtil.convert(12L, long.class);
            CommonUtil.convert(12L, Long.class);
            CommonUtil.convert(12L, float.class);
            CommonUtil.convert(12L, Float.class);
            CommonUtil.convert(12L, double.class);
            CommonUtil.convert(12L, Double.class);
        }

        {
            CommonUtil.convert(12f, byte.class);
            CommonUtil.convert(12f, Byte.class);
            CommonUtil.convert(12f, short.class);
            CommonUtil.convert(12f, Short.class);
            CommonUtil.convert(12f, int.class);
            CommonUtil.convert(12f, Integer.class);
            CommonUtil.convert(12f, long.class);
            CommonUtil.convert(12f, Long.class);
            CommonUtil.convert(12f, float.class);
            CommonUtil.convert(12f, Float.class);
            CommonUtil.convert(12f, double.class);
            CommonUtil.convert(12f, Double.class);
        }

        {
            CommonUtil.convert(12d, byte.class);
            CommonUtil.convert(12d, Byte.class);
            CommonUtil.convert(12d, short.class);
            CommonUtil.convert(12d, Short.class);
            CommonUtil.convert(12d, int.class);
            CommonUtil.convert(12d, Integer.class);
            CommonUtil.convert(12d, long.class);
            CommonUtil.convert(12d, Long.class);
            CommonUtil.convert(12d, float.class);
            CommonUtil.convert(12d, Float.class);
            CommonUtil.convert(12d, double.class);
            CommonUtil.convert(12d, Double.class);
        }

        {
            CommonUtil.convert((short) 12, byte.class);
            CommonUtil.convert((short) 12, Byte.class);
            CommonUtil.convert((short) 12, short.class);
            CommonUtil.convert((short) 12, Short.class);
            CommonUtil.convert((short) 12, int.class);
            CommonUtil.convert((short) 12, Integer.class);
            CommonUtil.convert((short) 12, long.class);
            CommonUtil.convert((short) 12, Long.class);
            CommonUtil.convert((short) 12, float.class);
            CommonUtil.convert((short) 12, Float.class);
            CommonUtil.convert((short) 12, double.class);
            CommonUtil.convert((short) 12, Double.class);
        }

        {
            CommonUtil.convert((byte) 12, byte.class);
            CommonUtil.convert((byte) 12, Byte.class);
            CommonUtil.convert((byte) 12, short.class);
            CommonUtil.convert((byte) 12, Short.class);
            CommonUtil.convert((byte) 12, int.class);
            CommonUtil.convert((byte) 12, Integer.class);
            CommonUtil.convert((byte) 12, long.class);
            CommonUtil.convert((byte) 12, Long.class);
            CommonUtil.convert((byte) 12, float.class);
            CommonUtil.convert((byte) 12, Float.class);
            CommonUtil.convert((byte) 12, double.class);
            CommonUtil.convert((byte) 12, Double.class);
        }

        {
            try {
                CommonUtil.convert(Integer.MAX_VALUE, byte.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Integer.MAX_VALUE, short.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Long.MIN_VALUE, int.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Float.MAX_VALUE, long.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(-Float.MAX_VALUE, long.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(-Double.MAX_VALUE, float.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }

        {
            try {
                CommonUtil.convert(Double.MAX_VALUE, float.class);
                fail("Should throw ArithmeticException");
            } catch (final ArithmeticException e) {
            }
        }
    }

    @Test
    public void test_asAndNew() {
        final List<String> linkedList = new LinkedList<>();
        linkedList.add("abc");
        N.println(linkedList);

        N.println(CommonUtil.asProps("firstName", "1)1"));
        N.println(CommonUtil.asProps(CommonUtil.asMap("firstName", "1)1")));
        assertEquals(CommonUtil.asProps("firstName", "1)1"), CommonUtil.asProps(CommonUtil.asMap("firstName", "1)1")));

        N.println(new LinkedHashMap<>());
        N.println(new LinkedHashMap<>(10));
        N.println(new IdentityHashMap<>());
        N.println(new IdentityHashMap<>(10));

        N.println(new ArrayDeque<>());
        N.println(new ArrayDeque<>(10));
        N.println(new TreeMap<>());
        N.println(new Multiset<>());
        N.println(new TreeSet<>());
        N.println(new ConcurrentLinkedQueue<>());
        N.println(CommonUtil.newListMultimap());
        N.println(CommonUtil.newLinkedListMultimap());
        N.println(CommonUtil.newSetMultimap());
        N.println(CommonUtil.newLinkedSetMultimap());
        N.println(CommonUtil.newLinkedSetMultimap());
        N.println(CommonUtil.newLinkedSetMultimap());
        N.println(CommonUtil.newLinkedSetMultimap());
        N.println(CommonUtil.newLinkedSetMultimap());

        N.println(new MapEntity(AccountPNL.__, CommonUtil.asProps("firstName", "1)1")));

        N.println(new MapEntity(AccountPNL.__));

        N.println(Seid.of(AccountPNL.ID, 123));
        N.println(Seid.create(CommonUtil.asProps(AccountPNL.ID, 123)));

        N.println(new LinkedHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
        N.println(new ConcurrentHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
        N.println(new IdentityHashMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
        N.println(new TreeMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));
        N.println(new TreeMap<>(CommonUtil.asProps(AccountPNL.ID, 123)));

        N.println(CommonUtil.asLinkedList("ab", "c"));
        N.println(new LinkedList<>(CommonUtil.asList("ab", "c")));

        N.println(CommonUtil.asLinkedHashSet("ab", "c"));

        N.println(CommonUtil.asSortedSet());
        N.println(CommonUtil.asSortedSet("ab", "c"));
        N.println(new TreeSet<>(CommonUtil.asList("ab", "c")));
        N.println(new TreeSet<>(CommonUtil.asSortedSet("ab", "c")));

        N.println(CommonUtil.asQueue("ab", "c"));
        N.println(new ArrayDeque<>());
        N.println(new ArrayDeque<>(3));
        N.println(new ArrayDeque<>(CommonUtil.asList("ab", "c")));

        N.println(CommonUtil.asArrayBlockingQueue("ab", "c"));

        N.println(CommonUtil.asLinkedBlockingQueue("ab", "c"));

        N.println(CommonUtil.asPriorityQueue("ab", "c"));
        N.println(CommonUtil.asConcurrentLinkedQueue("ab", "c"));

        final Delayed d = new Delayed() {
            @Override
            public int compareTo(final Delayed o) {
                return 0;
            }

            @Override
            public long getDelay(final TimeUnit unit) {
                return 0;
            }
        };

        N.println(CommonUtil.asDelayQueue(d));

        N.println(CommonUtil.asDeque("ab", "c"));
        N.println(new ArrayDeque<>(CommonUtil.asList("ab", "c")));

        N.println(CommonUtil.asDeque("ab", "c"));
        N.println(new ArrayDeque<>(CommonUtil.asList("ab", "c")));

        N.println(CommonUtil.asLinkedBlockingDeque("ab", "c"));

        N.println(CommonUtil.asConcurrentLinkedDeque("ab", "c"));

        assertEquals(true, Strings.parseBoolean("True"));
        assertEquals(1, Numbers.toByte("1"));
        assertEquals(1, Numbers.toShort("1"));
        assertEquals(1, Numbers.toInt("1"));
        assertEquals(0, Numbers.toInt(""));
        assertEquals(1, Numbers.toLong("1"));
        assertEquals(0, Numbers.toLong(""));
        assertEquals(1f, Numbers.toFloat("1"));
        assertEquals(1f, Numbers.toFloat("1f"));
        assertEquals(1f, Numbers.toFloat("1F"));
        assertEquals(0f, Numbers.toFloat(""));
        assertEquals(1d, Numbers.toDouble("1"));
        assertEquals(0d, Numbers.toDouble(""));

        assertEquals(1, (int) CommonUtil.convert(1L, int.class));

        N.println(Dates.createDate(Dates.currentJUDate()));
        N.println(Dates.createTime(Dates.currentJUDate()));
        N.println(Dates.createTimestamp(Dates.currentJUDate()));

        N.println(Dates.createJUDate(Dates.currentCalendar()));
        N.println(Dates.createJUDate(Dates.currentJUDate()));
        N.println(Dates.createJUDate(System.currentTimeMillis()));

        N.println(Dates.createCalendar(Dates.currentCalendar()));
        N.println(Dates.createCalendar(Dates.currentJUDate()));
        N.println(Dates.createCalendar(System.currentTimeMillis()));

        N.println(Dates.createGregorianCalendar(Dates.currentCalendar()));
        N.println(Dates.createGregorianCalendar(Dates.currentJUDate()));
        N.println(Dates.createGregorianCalendar(System.currentTimeMillis()));

        N.println(Dates.createXMLGregorianCalendar(Dates.currentCalendar()));
        N.println(Dates.createXMLGregorianCalendar(Dates.currentJUDate()));
        N.println(Dates.createXMLGregorianCalendar(System.currentTimeMillis()));

        N.println(Dates.parseCalendar(Dates.format(Dates.currentJUDate())));
        N.println(Dates.parseGregorianCalendar(Dates.format(Dates.currentJUDate())));
        N.println(Dates.parseXMLGregorianCalendar(Dates.format(Dates.currentJUDate())));
    }

    @Test
    public void test_hashCode() {
        N.println(CommonUtil.hashCode(false));
        N.println(CommonUtil.hashCode(true));
        N.println(CommonUtil.hashCode('a'));
        N.println(CommonUtil.hashCode((byte) 1));
        N.println(CommonUtil.hashCode((short) 1));
        N.println(CommonUtil.hashCode(1));
        N.println(CommonUtil.hashCode(1L));
        N.println(CommonUtil.hashCode(1f));
        N.println(CommonUtil.hashCode(1d));

        N.println(CommonUtil.hashCode(new boolean[] { true, false }));

        N.println(CommonUtil.hashCode(new char[] { 'a', 'b' }));

        N.println(CommonUtil.hashCode(new byte[] { (byte) 1, (byte) 1 }));

        N.println(CommonUtil.hashCode(new short[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new int[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new long[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new float[] { 1, 1 }));

        N.println(CommonUtil.hashCode(new double[] { 1, 1 }));

        final String[][] a = { { "a", "b", "c" }, { "1", "2", "3" } };
        N.println(CommonUtil.hashCode(a));
        N.println(CommonUtil.deepHashCode(a));

        final Object b = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };

        N.println(CommonUtil.hashCode(b));
        N.println(CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode(a), CommonUtil.deepHashCode(b));

        assertEquals(CommonUtil.deepHashCode("abc"), CommonUtil.hashCode("abc"));
        assertEquals(0, CommonUtil.hashCode((Object) null));
        assertEquals(0, CommonUtil.deepHashCode((Object) null));
    }

    @Test
    public void test_equals() {
        assertFalse(CommonUtil.equals(true, false));

        assertFalse(CommonUtil.equals('a', 'b'));

        assertFalse(CommonUtil.equals((byte) 1, (byte) 2));

        assertFalse(CommonUtil.equals((short) 1, (short) 2));

        assertFalse(CommonUtil.equals(1, 2));

        assertFalse(CommonUtil.equals(1f, 2f));

        assertTrue(CommonUtil.equals(1f, 1));

        assertFalse(CommonUtil.equals(1d, 2d));

        assertTrue(CommonUtil.equals(1d, 1));

        assertFalse(CommonUtil.equals(new boolean[] { true, false }, new boolean[] { false, true }));

        assertFalse(CommonUtil.equals(new char[] { 'a', 'b' }, new char[] { 'b', 'b' }));

        assertFalse(CommonUtil.equals(new byte[] { (byte) 1, (byte) 1 }, new byte[] { (byte) 1, (byte) 2 }));

        assertFalse(CommonUtil.equals(new short[] { 1, 1 }, new short[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new int[] { 1, 1 }, new int[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new long[] { 1, 1 }, new long[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new float[] { 1, 1 }, new float[] { 1, 2 }));

        assertFalse(CommonUtil.equals(new double[] { 1, 1 }, new double[] { 1, 2 }));

        final String[][] a = { { "a", "b", "c" }, { "1", "2", "3" } };
        final String[][] b = { { "a", "b", "c" }, { "1", "2", "3" } };

        assertTrue(CommonUtil.equals(a, b));
        assertTrue(CommonUtil.deepEquals(a, b));

        final Object a1 = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };
        final Object b1 = new String[][] { { "a", "b", "c" }, { "1", "2", "3" } };

        assertTrue(CommonUtil.equals(a1, b1));
        assertTrue(CommonUtil.deepEquals(a1, b1));

        assertTrue(CommonUtil.deepEquals("a", "a"));

        assertFalse(CommonUtil.deepEquals("a", "b"));
    }

    @Test
    public void test_asMap() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        Map<String, Object> m = CommonUtil.asMap(account);
        assertEquals("firstName1", m.get("firstName"));

        final com.landawn.abacus.entity.pjo.basic.Account account2 = new com.landawn.abacus.entity.pjo.basic.Account();
        account2.setFirstName("firstName1");
        account2.setLastName("lastName1");
        m = CommonUtil.asMap(account2);
        assertEquals("firstName1", m.get("firstName"));

        final Map<String, Object> m2 = new HashMap<>(m);
        assertEquals("firstName1", m2.get("firstName"));
    }

    @Test
    public void test_xml2Json() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXMLParser.serialize(account);
        final String json = jsonParser.serialize(account);

        final String xml2 = N.json2Xml(json, Account.class);
        N.println(xml2);

        final String json2 = N.xml2Json(xml, Account.class);
        N.println(json2);
    }

    @Test
    public void test_xml2JSON_1() {
        final Account account = new Account();
        account.setFirstName("firstName1");
        account.setLastName("lastName1");

        final String xml = abacusXMLDOMParser.serialize(account);
        final String json = jsonParser.serialize(account);

        final String xml2 = N.json2Xml(json, Account.class);
        N.println(xml2);

        final String json2 = N.xml2Json(xml, Account.class);
        N.println(json2);

        final String xml3 = N.json2Xml(json);
        N.println(xml3);

        final String json3 = N.xml2Json(xml);
        N.println(json3);
    }

    @Test
    public void test_bean2Map_2() {
        final Map<String, Object> props = new HashMap<>();
        props.put("Account.firstName", "firstName1");
        props.put("Account.lastName", "lastName1");

        final Account account = Beans.map2Bean(props, Account.class);
        assertEquals("firstName1", account.getFirstName());
        assertEquals("lastName1", account.getLastName());
    }

    @Test
    public void test_copy() {
        final Account account1 = new Account();
        account1.setFirstName("firstName1");
        account1.setLastName("lastName1");

        final Account account2 = Beans.copy(account1);
        N.println(account2);
        assertEquals("firstName1", account2.getFirstName());

        final PersonType personType1 = new PersonType();
        personType1.setBirthday(Dates.currentDate());
        personType1.setFirstName("firstName");

        final PersonType personType2 = Beans.copy(personType1);
        N.println(personType2);
        assertEquals("firstName", personType2.getFirstName());

        final PersonsType personsType1 = new PersonsType();
        personsType1.getPerson().add(personType1);

        final PersonsType personsType2 = Beans.copy(personsType1);
        assertEquals("firstName", personsType2.getPerson().get(0).getFirstName());

        final JAXBean jaxBean1 = new JAXBean();
        jaxBean1.getCityList().add("a");
        jaxBean1.getCityList().add("b");

        final JAXBean jaxBean2 = Beans.copy(jaxBean1);
        assertEquals("b", jaxBean2.getCityList().get(1));
    }

    @Test
    public void test_erase() {
        Account account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");

        N.println(account);
        Beans.eraseAll(account);
        N.println(account);

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.erase(account, "firstName");
        N.println(account);
        assertNull(account.getFirstName());

        account = new Account();
        account.setFirstName("firstName");
        account.setLastName("lastName");
        Beans.erase(account, CommonUtil.asList("firstName", "lastName"));
        assertNull(account.getFirstName());
        assertNull(account.getLastName());

        final PersonType personType = new PersonType();
        personType.setBirthday(Dates.currentDate());
        Beans.eraseAll(personType);
        assertNull(personType.getBirthday());
    }

    @Test
    public void test_merge() {
        final Account account1 = new Account();
        account1.setFirstName("firstName1");
        account1.setLastName("lastName1");

        final Account account2 = new Account();
        account2.setId(2);
        account2.setFirstName("firstName2");

        Beans.merge(account2, account1);
        N.println(account1);
        assertEquals("firstName2", account1.getFirstName());

        final PersonType personType1 = new PersonType();
        personType1.setBirthday(Dates.currentDate());

        final PersonType personType2 = new PersonType();
        personType2.setId(2);
        personType2.setFirstName("firstName");

        Beans.merge(personType2, personType1);
        N.println(personType1);
        assertEquals("firstName", personType1.getFirstName());

        final PersonsType personsType1 = new PersonsType();
        personsType1.getPerson().add(personType1);

        final PersonsType personsType2 = new PersonsType();
        personsType2.getPerson().add(personType2);
        Beans.merge(personsType2, personsType1);
        N.println(personsType1);

        assertEquals(1, personsType1.getPerson().size());

        final JAXBean jaxBean1 = new JAXBean();
        jaxBean1.getCityList().add("a");

        final JAXBean jaxBean2 = new JAXBean();
        jaxBean2.getCityList().add("b");

        Beans.merge(jaxBean1, jaxBean2);

        assertEquals("a", jaxBean2.getCityList().get(0));
    }

    @Test
    public void test_getPropField() {
        Field field = Beans.getPropField(Account.class, "firstName");
        N.println(field);

        field = Beans.getPropField(Account.class, "first_Name");
        N.println(field);

        field = Beans.getPropField(Account.class, "getfirstName");
        N.println(field);
    }

    @Test
    public void test_registerPropGetSetMethod() {
        Beans.registerPropGetSetMethod("a", ClassUtil.getDeclaredMethod(Account.class, "getFirstName"));

        Method method = Beans.getPropGetMethod(Account.class, "a");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(Account.class, "getFirstName"), method);

        Beans.registerPropGetSetMethod("b", ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class));
        method = Beans.getPropSetMethod(Account.class, "b");
        N.println(method);
        assertEquals(ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class), method);
    }

    @Test
    public void test_invokeConstructor() {
        final Account account = ClassUtil.invokeConstructor(ClassUtil.getDeclaredConstructor(Account.class));
        N.println(account);
    }

    @Test
    public void test_getClassName() {
        N.println(ClassUtil.getSimpleClassName(long.class));
        N.println(ClassUtil.getClassName(long.class));
        N.println(ClassUtil.getCanonicalClassName(long.class));

        N.println(ClassUtil.getSimpleClassName(long[].class));
        N.println(ClassUtil.getClassName(long[].class));
        N.println(ClassUtil.getCanonicalClassName(long[].class));

        N.println(ClassUtil.getSimpleClassName(Long.class));
        N.println(ClassUtil.getClassName(Long.class));
        N.println(ClassUtil.getCanonicalClassName(Long.class));

        N.println(ClassUtil.getSimpleClassName(Long[].class));
        N.println(ClassUtil.getClassName(Long[].class));
        N.println(ClassUtil.getCanonicalClassName(Long[].class));
    }

    @Test
    public void test_min_max() {
        N.println("==================================================");

        final List<String> coll2 = CommonUtil.asList("1", "2", "7", "0", "-1");
        N.println(N.min(coll2));
        N.println(N.max(coll2));

        N.println("==================================================");

        final Set<String> coll3 = CommonUtil.asSet("1", "2", "7", "0", "-1");
        N.println(N.min(coll3));
        N.println(N.max(coll3));

        N.println("==================================================");

        final int[] array = { 1, 2, 7, 0, -1 };
        N.println(N.sum(array));
        N.println(N.average(array));
        N.println(N.min(array));
        N.println(N.max(array));

        N.println("==================================================");

        final int[] array1 = { 1, 2, 7, 0, -1 };
        N.println(N.sum(array1));
        N.println(N.average(array1));
        N.println(N.min(array1));
        N.println(N.max(array1));

        N.println("==================================================");

        final String[] array2 = { "1", "2", "7", "0", "-1" };
        N.println(N.min(array2));
        N.println(N.max(array2));
    }

    @Test
    public void test_dateFormat_performance() {
        Profiler.run(this, "test_dateFormat", 6, 10000, 1).printResult();
    }

    @Test
    public void test_dateFormat() {
        final Timestamp date = Dates.currentTimestamp();
        final String st = Dates.format(date);

        Dates.parseTimestamp(st);
    }

    @Test
    public void test_dateFormat_2() throws ParseException {
        final DateFormat df = new SimpleDateFormat(Dates.ISO_8601_TIMESTAMP_FORMAT);
        final Timestamp date = Dates.currentTimestamp();
        final String st = df.format(date);

        df.parse(st);
    }

    @Test
    public void test_asArray() {
        final String[] a = CommonUtil.asArray("a", "b");
        N.println(a);

        final Object[] b = CommonUtil.asArray("a", 'c');
        N.println(b);

        final char[] c = Array.of('a', 'c');
        N.println(c);
    }

    @Test
    public void test_Bean2Map() {
        final Account account = createAccountWithContact(Account.class);

        Map<String, Object> m = Beans.bean2FlatMap(account);
        N.println(CommonUtil.stringOf(m));

        final XBean xBean = createBigXBean(1);
        m = Beans.bean2Map(xBean);
        N.println(CommonUtil.stringOf(m));

        m = Beans.bean2FlatMap(xBean);
        N.println(CommonUtil.stringOf(m));
    }

    @Test
    public void test_Bean2Map_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);

        Map<String, Object> m = Beans.bean2Map(account);
        N.println(CommonUtil.stringOf(m));

        m = Beans.deepBean2Map(account);
        N.println(CommonUtil.stringOf(m));

        m = Beans.bean2FlatMap(account);
        N.println(CommonUtil.stringOf(m));
    }

    @Test
    public void testFormat_1() {
        N.println(Dates.parseDate("2014-01-01"));

        String st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT);
        N.println(st + " : " + Dates.format(Dates.parseDate(st)));

        st = Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("UTC"));
        N.println(st + " : " + Dates.format(Dates.parseDate(st), Dates.LOCAL_DATE_TIME_FORMAT, TimeZone.getTimeZone("PST")));
    }

    @Test
    public void testFormat_2() {
        final String st = Dates.format(Dates.currentDate());
        N.println(st + " : " + Dates.format(Dates.parseDate(st)));

        for (int i = 0; i < 1000000; i++) {
            Dates.parseDate(st);
        }
    }

    @Test
    public void testFormat_3() {
        final String st = Dates.format(Dates.currentDate(), Dates.ISO_8601_TIMESTAMP_FORMAT);
        N.println(st + " : " + Dates.format(Dates.parseDate(st), Dates.ISO_8601_TIMESTAMP_FORMAT));

        for (int i = 0; i < 1000000; i++) {
            Dates.parseDate(st);
        }
    }

    @Test
    public void test_jaxb_1() throws Exception {
        Beans.registerXMLBindingClass(JaxbBean.class);

        final JaxbBean jb = new JaxbBean();
        jb.setString("string1");
        jb.getList().add("list_e_1");
        jb.getMap().put("map_key_1", "map_value_1");
        N.println(abacusXMLParser.serialize(jb));
        N.println(CommonUtil.stringOf(abacusXMLParser.deserialize(abacusXMLParser.serialize(jb), JaxbBean.class)));
    }

    @Test
    public void test_jaxb_2() throws Exception {
        Beans.registerXMLBindingClass(JaxbBean.class);

        final JaxbBean jb = new JaxbBean();
        jb.setString("string1");
        jb.getList().add("list_e_1");
        jb.getMap().put("map_key_1", "map_value_1");
        N.println(abacusXMLDOMParser.serialize(jb));
        N.println(CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(jb), JaxbBean.class)));
    }

    @Test
    public void test_string2Array() {
        final byte[] array = Splitter.with(",").trim(true).splitToArray("1,2,3, 4", byte[].class);

        N.println(array);
    }

    @Test
    public void test_performance() {
        Profiler.run(this, "execute", 16, 100, 1).printResult();
    }

    void execute() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        abacusXMLParser.deserialize(abacusXMLParser.serialize(account), Account.class);

    }

    @Test
    public void test_valueOf_1() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.asList("abc", "123"));
        assertEquals(bean, abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.asList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.asList(1, 2, 3));
        assertFalse(bean.equals(abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXmlGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXMLParser.serialize(bean));

        assertFalse(bean.equals(abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void test_valueOf_2() throws Exception {
        final Bean_1 bean = new Bean_1();
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setStrList(CommonUtil.asList("abc", "123"));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setShortList(CommonUtil.asList((short) 1, (short) 2, (short) 3));
        assertEquals(bean, abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class));

        bean.setIntList(CommonUtil.asList(1, 2, 3));
        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));

        final GregorianCalendar c = new GregorianCalendar();
        bean.setXmlGregorianCalendar(DatatypeFactory.newInstance().newXMLGregorianCalendar(c));
        N.println(abacusXMLDOMParser.serialize(bean));

        assertFalse(bean.equals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean_1.class)));
    }

    @Test
    public void testArrayName() {
        N.println(byte[].class.getSimpleName());
        assertTrue("byte[]".equals(byte[].class.getSimpleName()));

        N.println(int[].class.getSimpleName());
        assertTrue("int[]".equals(int[].class.getSimpleName()));
    }

    protected String getDomainName() {
        return ExtendDirtyBasicPNL._DN;
    }

    @Test
    public void testString2Array() {
        N.println(Splitter.defauLt().splitToArray("a, b, c", char[].class));
        N.println(Splitter.defauLt().splitToArray("1, 2, 3", byte[].class));
        N.println(Splitter.defauLt().splitToArray("1, 2, 3", int[].class));
    }

    @Test
    public void testStringOf() {
        N.println(CommonUtil.stringOf(Splitter.defauLt().splitToArray(CommonUtil.stringOf(Dates.currentDate()), Date[].class)));
        N.println(CommonUtil.stringOf(Splitter.defauLt().splitToArray("a, b, c", char[].class)));
        N.println(CommonUtil.stringOf(Splitter.defauLt().splitToArray("1, 2, 3", byte[].class)));
        N.println(CommonUtil.stringOf(Splitter.defauLt().splitToArray("1, 2, 3", int[].class)));
    }

    @Test
    public void testValueOf() {
        N.println(abacusXMLParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
        N.println(abacusXMLParser.deserialize("<array>a, b, c</array>", char[].class));
        N.println(abacusXMLParser.deserialize("<array>1, 2, 3</array>", byte[].class));
        N.println(abacusXMLParser.deserialize("<array>1, 2, 3</array>", int[].class));
    }

    @Test
    public void testValueOf_1() {
        N.println(abacusXMLDOMParser.deserialize("<array><e>" + CommonUtil.stringOf(Dates.currentDate()) + "</e></array>", Date[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>a, b, c</array>", char[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", byte[].class));
        N.println(abacusXMLDOMParser.deserialize("<array>1, 2, 3</array>", int[].class));
    }

    @Test
    public void testForClass() throws Exception {
        N.println(int.class.getSimpleName());
        N.println(int.class.getCanonicalName());

        assertEquals(int.class, ClassUtil.forClass("int"));
        assertEquals(char.class, ClassUtil.forClass("char"));
        assertEquals(boolean.class, ClassUtil.forClass("boolean"));
        assertEquals(String.class, ClassUtil.forClass("String"));
        assertEquals(String.class, ClassUtil.forClass(String.class.getName()));
        assertEquals(Object.class, ClassUtil.forClass("Object"));
        assertEquals(java.lang.Math.class, ClassUtil.forClass("Math"));
        assertEquals(java.util.Date.class, ClassUtil.forClass("java.util.Date"));

        assertEquals(int[].class, ClassUtil.forClass("int[]"));
        assertEquals(char[].class, ClassUtil.forClass("char[]"));
        assertEquals(boolean[].class, ClassUtil.forClass("boolean[]"));
        assertEquals(String[].class, ClassUtil.forClass("String[]"));
        assertEquals(String[].class, ClassUtil.forClass(String[].class.getName()));
        assertEquals(Object[].class, ClassUtil.forClass("Object[]"));
        assertEquals(java.util.Date[].class, ClassUtil.forClass("java.util.Date[]"));

        assertEquals(int[][].class, ClassUtil.forClass("int[][]"));
        assertEquals(char[][].class, ClassUtil.forClass("char[][]"));
        assertEquals(boolean[][].class, ClassUtil.forClass("boolean[][]"));
        assertEquals(String[][].class, ClassUtil.forClass("String[][]"));
        assertEquals(String[][].class, ClassUtil.forClass(String[][].class.getName()));
        assertEquals(Object[][].class, ClassUtil.forClass("Object[][]"));
        assertEquals(java.util.Date[][].class, ClassUtil.forClass("java.util.Date[][]"));

        N.println(int[][][].class.getName());
        N.println(int.class.getName());

        assertEquals(int[][][].class, ClassUtil.forClass("int[][][]"));
        assertEquals(char[][][].class, ClassUtil.forClass("char[][][]"));
        assertEquals(boolean[][][].class, ClassUtil.forClass("boolean[][][]"));
        assertEquals(String[][][].class, ClassUtil.forClass("String[][][]"));
        assertEquals(Object[][][].class, ClassUtil.forClass("Object[][][]"));
        assertEquals(java.util.Date[][][].class, ClassUtil.forClass("java.util.Date[][][]"));

        try {
            ClassUtil.forClass("string");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }

        try {
            ClassUtil.forClass("object[]");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }

        try {
            ClassUtil.forClass("int[];[]");
            fail("should throw RuntimeException");
        } catch (final IllegalArgumentException e) {
        }
    }

    @Test
    public void testGetDefaultValueByType() {
        assertEquals(false, (boolean) CommonUtil.defaultValueOf(boolean.class));
        assertEquals(0, (char) CommonUtil.defaultValueOf(char.class));
        assertEquals(0, (byte) CommonUtil.defaultValueOf(byte.class));
        assertEquals(0, (short) CommonUtil.defaultValueOf(short.class));
        assertEquals(0, (int) CommonUtil.defaultValueOf(int.class));
        assertEquals(0L, (long) CommonUtil.defaultValueOf(long.class));
        assertEquals(0f, (float) CommonUtil.defaultValueOf(float.class));
        assertEquals(0d, (double) CommonUtil.defaultValueOf(double.class));
        assertEquals(null, CommonUtil.defaultValueOf(Boolean.class));
        assertEquals(null, CommonUtil.defaultValueOf(String.class));
        assertEquals(null, CommonUtil.defaultValueOf(Object.class));
    }

    @Test
    public void testIsNullorEmpty() {
        assertTrue(CommonUtil.isEmpty(new boolean[0]));
        assertTrue(CommonUtil.isEmpty(new char[0]));
        assertTrue(CommonUtil.isEmpty(new byte[0]));
        assertTrue(CommonUtil.isEmpty(new short[0]));
        assertTrue(CommonUtil.isEmpty(new int[0]));
        assertTrue(CommonUtil.isEmpty(new long[0]));
        assertTrue(CommonUtil.isEmpty(new float[0]));
        assertTrue(CommonUtil.isEmpty(new double[0]));

        assertTrue(CommonUtil.notEmpty(new boolean[1]));
        assertTrue(CommonUtil.notEmpty(new char[1]));
        assertTrue(CommonUtil.notEmpty(new byte[1]));
        assertTrue(CommonUtil.notEmpty(new short[1]));
        assertTrue(CommonUtil.notEmpty(new int[1]));
        assertTrue(CommonUtil.notEmpty(new long[1]));
        assertTrue(CommonUtil.notEmpty(new float[1]));
        assertTrue(CommonUtil.notEmpty(new double[1]));

        assertFalse(CommonUtil.isEmpty(new boolean[1]));
        assertFalse(CommonUtil.isEmpty(new char[1]));
        assertFalse(CommonUtil.isEmpty(new byte[1]));
        assertFalse(CommonUtil.isEmpty(new short[1]));
        assertFalse(CommonUtil.isEmpty(new int[1]));
        assertFalse(CommonUtil.isEmpty(new long[1]));
        assertFalse(CommonUtil.isEmpty(new float[1]));
        assertFalse(CommonUtil.isEmpty(new double[1]));

        assertTrue(CommonUtil.isEmpty(new ArrayList<>()));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));
        assertTrue(CommonUtil.isEmpty(new ArrayList<>(100)));
        assertTrue(Strings.isEmpty(""));

        final Map<String, Object> m = null;
        assertTrue(CommonUtil.isEmpty(m));
        assertTrue(CommonUtil.isEmpty(new HashMap<>()));

        final String st = null;
        assertTrue(Strings.isEmpty(st));

        final Account[] a = null;
        assertTrue(CommonUtil.isEmpty(a));
        assertTrue(CommonUtil.isEmpty(new String[] {}));

        assertFalse(Strings.isEmpty(" "));
        assertFalse(CommonUtil.isEmpty(new String[] { null }));
    }

    @Test
    public void testGetConstructor() {
        assertNotNull(ClassUtil.getDeclaredConstructor(Account.class));
        assertNull(ClassUtil.getDeclaredConstructor(Account.class, String.class));
        assertNotNull(ClassUtil.getDeclaredConstructor(TypeFactory.class));
    }

    @Test
    public void testGetMethod() {
        assertNotNull(ClassUtil.getDeclaredMethod(Account.class, "setFirstName", String.class));
        assertNotNull(ClassUtil.getDeclaredMethod(Account.class, "setFirstname", String.class));
        assertNull(ClassUtil.getDeclaredMethod(Account.class, "getFirstName", String.class));
        assertNull(ClassUtil.getDeclaredMethod(Account.class, "getFirstName1", String.class));

        assertNotNull(ClassUtil.getDeclaredMethod(CommonUtil.class, "typeOf", Class.class));
    }

    @Test
    public void testInvokeMethod() {
        final Account account = createAccount(Account.class);
        assertEquals(account.getFirstName(), ClassUtil.invokeMethod(account, ClassUtil.getDeclaredMethod(Account.class, "getFirstName")));
        ClassUtil.invokeMethod(account, ClassUtil.getDeclaredMethod(CommonUtil.class, "typeOf", Class.class), N.class);

        N.println(ClassUtil.invokeMethod(this, ClassUtil.getDeclaredMethod(NTest.class, "testGetMethod")));

        assertNull(ClassUtil.invokeMethod(this, ClassUtil.getDeclaredMethod(NTest.class, "testGetMethod")));
    }

    @Test
    public void testGetPropName() {
        assertEquals("gui", Beans.getPropNameByMethod(Beans.getPropGetMethod(Account.class, "GUI")));
        assertEquals("birthDate", Beans.getPropNameByMethod(Beans.getPropGetMethod(Account.class, "birthDate")));
    }

    @Test
    public void testGetGetterSetterMethodList() {
        assertEquals(Beans.getPropGetMethods(Account.class).size(), Beans.getPropSetMethods(Account.class).size());

        assertEquals(Beans.getPropGetMethods(XBean.class).size(), Beans.getPropSetMethods(XBean.class).size());
    }

    @Test
    public void testPropGetSetMethod() {
        final Account account = new Account();
        account.setFirstName("fn");
        account.setMiddleName("mn");
        account.setLastName("ln");

        println(Beans.getPropGetMethod(Account.class, "firstName"));
        println(Beans.getPropSetMethod(Account.class, "id"));
    }

    @Test
    public void testPropGetSetValue() {
        final Account account = new Account();

        Method getMethod = Beans.getPropGetMethod(Account.class, "firstName");
        Method setMethod = Beans.getPropSetMethod(Account.class, "firstName");
        Beans.setPropValue(account, setMethod, "fn");
        assertEquals("fn", Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetMethod(Account.class, AccountPNL.ID);
        setMethod = Beans.getPropSetMethod(Account.class, AccountPNL.ID);
        Beans.setPropValue(account, setMethod, -1);
        Beans.setPropValue(account, setMethod, Integer.valueOf(-2));
        assertEquals(Long.valueOf(-2), Beans.getPropValue(account, getMethod));
        println(account);

        getMethod = Beans.getPropGetMethod(Account.class, AccountPNL.BIRTH_DATE);
        setMethod = Beans.getPropSetMethod(Account.class, AccountPNL.BIRTH_DATE);

        final Date date = Dates.currentDate();
        Beans.setPropValue(account, setMethod, date);
        println(account);

        Beans.setPropValue(account, "firstName", "newfn");
        assertEquals("newfn", Beans.getPropValue(account, "firstName"));
        println(account);

        Beans.setPropValue(account, "id", null);

    }

    @Test
    public void testPropGetSetValue_2() {
        final Account account = new Account();
        String firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        Beans.setPropValue(account, "firstName", firstName);

        assertEquals(firstName, Beans.getPropValue(account, "firstName"));
        firstName = Beans.getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = Beans.getPropValue(account, "contact.id");
        N.println(contactId);

        String email = Beans.getPropValue(account, "contact.email");
        N.println(email);

        assertNull(Beans.getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = Beans.getPropValue(account, "contact.email");
        N.println(email);
    }

    @Test
    public void test_ParserUtil() {
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Account.class);
        final Set<BeanInfo> set = CommonUtil.asSet(beanInfo);
        assertTrue(set.contains(beanInfo));
        N.println(beanInfo);

        final PropInfo propInfo = beanInfo.getPropInfo("firstName");
        final Set<PropInfo> set2 = CommonUtil.asSet(propInfo);
        set2.contains(propInfo);

        N.println(propInfo);
    }

    @Test
    public void testPropGetSetValue_by_ParserUtil() {
        final Account account = new Account();
        String firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        firstName = "firstName";
        ParserUtil.getBeanInfo(Account.class).setPropValue(account, "firstName", firstName);

        assertEquals(firstName, ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName"));
        firstName = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "firstName");
        N.println(firstName);

        final long contactId = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.id");
        N.println(contactId);

        String email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);

        assertNull(ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact"));

        Beans.setPropValue(account, "contact.email", "myemail@email.com");
        email = ParserUtil.getBeanInfo(Account.class).getPropValue(account, "contact.email");
        N.println(email);
    }

    @Test
    public void testMerge() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(100001);

        final Account account2 = new Account();
        account2.setFirstName("fn2");
        account2.setMiddleName("mn2");
        account2.setLastName("ln2");
        account2.setBirthDate(Dates.currentTimestamp());

        Beans.merge(account, account2);
        println(account);
        println(account2);

        Beans.eraseAll(account);
        N.println(account);

    }

    @Test
    public void testMerge2() {
        final Account account = createAccount(Account.class);
        final Account account2 = createAccount(Account.class);

        Beans.merge(account, account2);
        println(account);
        println(account2);
    }

    @Test
    public void testCopyBean() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(1000);

        final Account copy = Beans.copy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCopyBean2() {
        final com.landawn.abacus.entity.extendDirty.basic.Account account = new com.landawn.abacus.entity.extendDirty.basic.Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(100001);
        account.setBirthDate(Dates.currentTimestamp());

        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCopyBean3() {
        final Account account = new Account();
        account.setId(1000);
        account.setBirthDate(Dates.currentTimestamp());

        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(account);
        println(copy);
        println(CommonUtil.stringOf(account));
        println(CommonUtil.stringOf(copy));
        assertEquals(account.toString(), copy.toString());
    }

    @Test
    public void testCloneBean() {
        final Account account = new Account();
        account.setFirstName("fn1");
        account.setMiddleName("mn1");
        account.setLastName("ln1");
        account.setId(1000);

        final Account copy = Beans.clone(account);
        println(copy);
        assertEquals(account, copy);
    }

    @Test
    public void testCloneBean_2() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeString(
                "‰β,『�?★业€ > \\n sfd \\r ds \\' f d // \\\\  \\\\\\\\ /// /////// \\\\\\\\\\\\\\\\  \\\\\\\\\\\\\\\\n \\\\\\\\\\\\\\\\r  \\t sd \\\" fe stri‰β,『�?★业€ ng黎< > </ <//、\\n");

        final Map<Integer, Object> typeMap = CommonUtil.asMap(1, "2", 1, "2");
        xBean.setTypeMap(typeMap);

        final XBean xBean2 = Beans.clone(xBean);
        assertEquals(xBean, xBean2);
    }

    @Test
    public void testCopy_1() {
        final Account account = createAccountWithContact(Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(copy);
    }

    @Test
    public void testCopy_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);
        println(copy);
    }

    @Test
    public void testMerge_1() {
        final Account account = createAccountWithContact(Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);

        Beans.merge(copy, account);
        println(account);
    }

    @Test
    public void testMerge_2() {
        final com.landawn.abacus.entity.pjo.basic.Account account = createAccountWithContact(com.landawn.abacus.entity.pjo.basic.Account.class);
        final com.landawn.abacus.entity.extendDirty.basic.Account copy = Beans.copy(account, com.landawn.abacus.entity.extendDirty.basic.Account.class);

        Beans.merge(copy, account);
        println(account);
    }

    @Test
    public void test_binarySearch_2() {
        final Random rand = new Random();

        for (int i = 0; i < 1000; i++) {
            final int len = i;
            final int[] a = new int[len];

            for (int j = 0; j < len; j++) {
                a[j] = rand.nextInt();
            }

            Arrays.sort(a);

            final List<Integer> listA = new ArrayList<>(CommonUtil.toList(a));
            final List<Integer> listB = new LinkedList<>(CommonUtil.toList(a));
            for (int k = 0; k < len; k++) {
                final int expected = CommonUtil.binarySearch(a, a[k]);
                assertEquals(expected, CommonUtil.binarySearch(listA, listA.get(k)));
                assertEquals(expected, CommonUtil.binarySearch(listB, listA.get(k)));
                assertEquals(a[k], listA.get(expected).intValue());
                assertEquals(a[k], listB.get(expected).intValue());
            }
        }
    }

    @Test
    public void test_binarySearch() {
        boolean[] copy0 = { false, false, true };
        assertEquals(0, CommonUtil.binarySearch(copy0, false));
        assertEquals(2, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, true, true, true, true };
        assertEquals(1, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, true };
        assertEquals(2, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, true };
        assertEquals(3, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, false, true };
        assertEquals(4, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false, false, false, true };
        assertEquals(5, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { false, false, false };
        assertEquals(0, CommonUtil.binarySearch(copy0, false));
        assertEquals(-4, CommonUtil.binarySearch(copy0, true));

        copy0 = new boolean[] { true, true, true };
        assertEquals(-1, CommonUtil.binarySearch(copy0, false));
        assertEquals(0, CommonUtil.binarySearch(copy0, true));

        final char[] copy1 = { '1', '2', '3' };
        assertEquals(1, CommonUtil.binarySearch(copy1, '2'));
        assertEquals(1, CommonUtil.binarySearch(copy1, 1, 2, '2'));

        final byte[] copy2 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy2, (byte) 2));
        assertEquals(1, CommonUtil.binarySearch(copy2, 1, 2, (byte) 2));

        final short[] copy3 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy3, (short) 2));
        assertEquals(1, CommonUtil.binarySearch(copy3, 1, 2, (short) 2));

        final int[] copy4 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy4, 2));
        assertEquals(1, CommonUtil.binarySearch(copy4, 1, 2, 2));

        final long[] copy5 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy5, 2));
        assertEquals(1, CommonUtil.binarySearch(copy5, 1, 2, 2));

        final float[] copy6 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy6, 2));
        assertEquals(1, CommonUtil.binarySearch(copy6, 1, 2, 2));

        final double[] copy7 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy7, 2));
        assertEquals(1, CommonUtil.binarySearch(copy7, 1, 2, 2));

        final Integer[] copy8 = { 1, 2, 3 };
        assertEquals(1, CommonUtil.binarySearch(copy8, 2));
        assertEquals(1, CommonUtil.binarySearch(copy8, 2, Comparators.naturalOrder()));

        final Integer[] copy9 = { 1, 2, 3 };
        assertEquals(-1, CommonUtil.binarySearch(copy9, 0));
        assertEquals(-1, CommonUtil.binarySearch(copy9, 0, Comparators.naturalOrder()));
        assertEquals(-4, CommonUtil.binarySearch(copy9, 5));
        assertEquals(-4, CommonUtil.binarySearch(copy9, 5, Comparators.naturalOrder()));

        final List<Integer> list = CommonUtil.asList(1, 2, 3);
        assertEquals(1, CommonUtil.binarySearch(list, 2));
        assertEquals(1, CommonUtil.binarySearch(list, 2, Comparators.naturalOrder()));

        {
            final int[] a = {};
            assertEquals(-1, CommonUtil.binarySearch(a, 1));
            assertEquals(-1, Arrays.binarySearch(a, 1));
            assertEquals(Arrays.binarySearch(a, 1), CommonUtil.binarySearch(a, 1));
            assertEquals(-1, CommonUtil.binarySearch((int[]) null, 1));
            assertEquals(-1, CommonUtil.binarySearch((int[]) null, 0));
        }
    }

    @Test
    public void testCopyArray() {
        boolean[] copy = CommonUtil.copyOfRange(new boolean[] { true, true, false }, 0, 2);
        println(CommonUtil.toString(copy));

        char[] copy1 = CommonUtil.copyOfRange(new char[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy1));

        byte[] copy2 = CommonUtil.copyOfRange(new byte[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy2));

        short[] copy3 = CommonUtil.copyOfRange(new short[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy3));

        int[] copy4 = CommonUtil.copyOfRange(new int[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy4));

        long[] copy5 = CommonUtil.copyOfRange(new long[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy5));

        float[] copy6 = CommonUtil.copyOfRange(new float[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy6));

        double[] copy7 = CommonUtil.copyOfRange(new double[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy7));

        Object[] copy8 = CommonUtil.copyOfRange(new Object[] { 1, 2, 3 }, 0, 2);
        println(CommonUtil.toString(copy8));

        copy8 = CommonUtil.copyOfRange(new Integer[] { 1, 2, 3 }, 0, 2, Object[].class);
        println(CommonUtil.toString(copy8));

        copy = CommonUtil.copyOf(new boolean[] { true, true, false }, 9);
        println(CommonUtil.toString(copy));

        copy1 = CommonUtil.copyOf(new char[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy1));

        copy2 = CommonUtil.copyOf(new byte[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy2));

        copy3 = CommonUtil.copyOf(new short[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy3));

        copy4 = CommonUtil.copyOf(new int[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy4));

        copy5 = CommonUtil.copyOf(new long[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy5));

        copy6 = CommonUtil.copyOf(new float[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy6));

        copy7 = CommonUtil.copyOf(new double[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy7));

        copy8 = CommonUtil.copyOf(new Object[] { 1, 2, 3 }, 9);
        println(CommonUtil.toString(copy8));

        copy8 = CommonUtil.copyOf(new String[] { "1", "2", "3" }, 9, Object[].class);
        println(CommonUtil.toString(copy8));
    }

    @Test
    public void testCombine() {
        final String[] a = { "a", "b", "c" };
        final String[] b = { "d", "e", "f" };
        final String[] c = { "a", "b", "c", "d", "e", "f" };

        println(CommonUtil.toString(N.concat(b, a)));
        assertTrue(CommonUtil.equals(c, N.concat(a, b)));

        final int[] i1 = { 1, 2, 3 };
        final int[] i2 = { 4, 5, 6 };

        N.concat(i2, i1);
    }

    @Test
    public void testBean2Map() {
        final Account account = createAccount(Account.class);
        println(Beans.bean2Map(account));
    }

    @Test
    public void testTimeUnit() {
        assertEquals(100, TimeUnit.MILLISECONDS.toMillis(100));
        assertEquals(100000, TimeUnit.SECONDS.toMillis(100));
        assertEquals(6000000, TimeUnit.MINUTES.toMillis(100));
        assertEquals(-100, TimeUnit.MILLISECONDS.toMillis(-100));
        assertEquals(-100000, TimeUnit.SECONDS.toMillis(-100));
        assertEquals(-6000000, TimeUnit.MINUTES.toMillis(-100));
    }

    @Test
    public void testAsDate() {
        final Calendar c = Calendar.getInstance();
        println(Dates.roll(Dates.createDate(c), 10, CalendarField.MONTH));

        println(Dates.roll(Dates.createDate(c), 10, TimeUnit.DAYS));
        println(Dates.roll(Dates.createDate(c), -10, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), -10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 10000, TimeUnit.DAYS));

        println(Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR));

        println(Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createDate(c), 1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createDate(c), 7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTime(c), -1000, CalendarField.WEEK_OF_YEAR)), Dates.roll(Dates.createTime(c), -7000, CalendarField.DAY_OF_MONTH));

        assertEquals((Dates.roll(Dates.createTimestamp(c), -1000, CalendarField.WEEK_OF_YEAR)),
                Dates.roll(Dates.createTimestamp(c), -7000, CalendarField.DAY_OF_MONTH));

        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, TimeUnit.DAYS)));
        println(Dates.createDate(Dates.roll(Dates.currentCalendar(), 10, CalendarField.DAY_OF_MONTH)));
    }

    @Test
    public void testAsDateWithString() {
        final Type<Date> type = TypeFactory.getType(Date.class.getCanonicalName());
        final String st = type.stringOf(Dates.currentDate());
        N.println(st);
    }

    @Test
    public void testToString() {
        final Object arraysOfDouble = new double[][] { { 1, 1 }, { 1.2111, 2.111 } };
        println(CommonUtil.toString(arraysOfDouble));
        println(CommonUtil.deepToString(arraysOfDouble));
    }

    @Test
    public void testUUID() {
        N.println(Strings.guid());
        N.println(Strings.guid().length());
        N.println(Strings.uuid());
        N.println(Strings.uuid().length());

        println(UUID.randomUUID().toString());

        final String uuid = Strings.uuid();
        println(uuid);
        println(Strings.base64Encode(uuid.getBytes()));
    }

    @Test
    public void testAsString() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(CommonUtil.stringOf(account));

        final Bean bean = new Bean();
        println(CommonUtil.stringOf(bean));

        bean.setStrings(CommonUtil.asArray("a", "b"));
        println(CommonUtil.stringOf(bean));

        bean.setBytes(new byte[] { 1, 2 });
        println(CommonUtil.stringOf(bean));
    }

    @Test
    public void testAsXml() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXMLParser.serialize(account));
        println(abacusXMLParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.asSet("lastUpdateTime"));
        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXMLParser.serialize(account, config));

        println(abacusXMLParser.deserialize(abacusXMLParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXMLParser.deserialize(abacusXMLParser.serialize(account), Account.class)));
    }

    @Test
    public void testAsXml_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        println(abacusXMLDOMParser.serialize(account));
        println(abacusXMLDOMParser.serialize(account));

        final Map<Class<?>, Set<String>> ignoredPropNames = CommonUtil.asMap(Account.class, CommonUtil.asSet("lastUpdateTime"));
        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.setIgnoredPropNames(ignoredPropNames);
        println(abacusXMLDOMParser.serialize(account, config));

        println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class));

        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(account), Account.class)));
    }

    @Test
    public void testValueOf_2() {
        valueOf();

        Profiler.run(this, "valueOf", 32, 100, 1).printResult();
    }

    void valueOf() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);
        abacusXMLParser.deserialize(abacusXMLParser.serialize(account), Account.class);

    }

    @Test
    public void testSerialize() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXMLParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXMLParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXMLParser.serialize(account));
        N.println(abacusXMLParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLParser.serialize(account), abacusXMLParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXMLParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXMLParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.tagByPropertyName(false);
        xml = abacusXMLParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.writeTypeInfo(true);
        xml = abacusXMLParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize_1() {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        String xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        Account xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        N.println(account);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(account));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(account));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.serialize(account), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));

        xml = abacusXMLDOMParser.serialize(account);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.tagByPropertyName(false);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);

        config.writeTypeInfo(true);
        xml = abacusXMLDOMParser.serialize(account, config);
        N.println(xml);

        xmlBean = abacusXMLDOMParser.deserialize(xml, Account.class);
        assertEquals(account, xmlBean);
    }

    @Test
    public void testSerialize1() {
        final Bean bean = new Bean();
        bean.setTypeList(CommonUtil.asList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        final String xml = abacusXMLParser.serialize(bean);
        println(xml);

        final Bean xmlBean = abacusXMLParser.deserialize(xml, Bean.class);
        N.println(abacusXMLParser.serialize(bean));
        N.println(abacusXMLParser.serialize(xmlBean));

        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), Bean.class));
        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize1_1() {
        final Bean bean = new Bean();
        bean.setTypeList(CommonUtil.asList(
                "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n", '★', '\n',
                '\r', '\t', '\"', '\'', ' ', new char[] { '\r', '\t', '\"', '\'', ' ' },
                new String[] {
                        "‰β,『�?★业€ > \n sfd \r ds \' f d // \\  \\\\ /// /////// \\\\\\\\  \\\\\\\\n \\\\\\\\r  \t sd \" fe stri‰β,『�?★业€ ng黎< > </ <//、\n",
                        "\r", "\t", "\"", "\'" }));

        bean.setBytes(new byte[] { 1, 2 });
        bean.setStrings(new String[] { "aa", "bb", "<>>" });
        bean.setChars(new char[] { '\r', '\t', '\"', '\'', ' ', ',', ' ', ',' });

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final Bean xmlBean = abacusXMLDOMParser.deserialize(xml, Bean.class);
        assertEquals(abacusXMLDOMParser.serialize(bean), abacusXMLDOMParser.serialize(xmlBean));
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), Bean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), Bean.class));
    }

    @Test
    public void testSerialize2() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        final List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        final String xml = abacusXMLParser.serialize(xBean, XSC.create().writeTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLParser.deserialize(xml, XBean.class);
        N.println(xmlBean.getTypeList().get(1).getClass());
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXMLParser.serialize(xBean));
        N.println(abacusXMLParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLParser.deserialize(abacusXMLParser.serialize(xBean), XBean.class),
                abacusXMLParser.deserialize(abacusXMLParser.serialize(xmlBean), XBean.class));

        assertEquals(abacusXMLParser.serialize(xBean), abacusXMLParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXMLParser.serialize(xBean));
        N.println(abacusXMLParser.serialize(xmlBean));

        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(xBean), XBean.class));
        N.println(abacusXMLParser.deserialize(abacusXMLParser.serialize(xmlBean), XBean.class));
    }

    @Test
    public void testSerialize2_1() {
        println(String.valueOf((char) 0));

        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('黎');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string黎< > </ <//、");

        final List typeList = new ArrayList();
        typeList.add(account.getFirstName());
        typeList.add(account);
        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        xBean.setWeekDay(WeekDay.THURSDAY);

        final String xml = abacusXMLDOMParser.serialize(xBean, XSC.create().writeTypeInfo(true));
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        N.println(xBean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(xBean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));

        assertEquals(abacusXMLDOMParser.serialize(xBean), abacusXMLDOMParser.serialize(xmlBean));
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));

        N.println(abacusXMLDOMParser.serialize(xBean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));

        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xBean), XBean.class));
        N.println(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));
    }

    @Test
    public void testSerialize3() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXMLParser.serialize(account);
        println(st);

        println(abacusXMLParser.deserialize(st, Account.class));

        st = abacusXMLParser.serialize(account);
        println(st);

        println(abacusXMLParser.deserialize(st, Account.class));
        assertEquals(account, abacusXMLParser.deserialize(st, Account.class));
    }

    @Test
    public void testSerialize3_1() {
        final Account account = createAccount(Account.class);
        account.setId(100);

        String st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));

        st = abacusXMLDOMParser.serialize(account);
        println(st);

        println(abacusXMLDOMParser.deserialize(st, Account.class));
        assertEquals(account, abacusXMLDOMParser.deserialize(st, Account.class));
    }

    @Test
    public void testGenericType() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.asList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.asSet(1L, 2L));

        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.writeTypeInfo(true);

        final String xml = abacusXMLParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXMLParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testGenericType_1() {
        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeGenericList(
                CommonUtil.asList(Dates.createDate(System.currentTimeMillis() / 1000 * 1000), Dates.createDate(System.currentTimeMillis() / 1000 * 1000)));
        xBean.setTypeGenericSet(CommonUtil.asSet(1L, 2L));

        final XMLSerializationConfig config = new XMLSerializationConfig();
        config.writeTypeInfo(true);

        final String xml = abacusXMLDOMParser.serialize(xBean, config);
        println(xml);

        assertEquals(xBean, abacusXMLDOMParser.deserialize(xml, XBean.class));
    }

    @Test
    public void testPerformanceForBigBean() throws Exception {
    }

    void executeBigBean() {
        abacusXMLParser.deserialize(abacusXMLParser.serialize(createBigXBean(10000)), XBean.class);
    }

    public static XBean createBigXBean(final int size) {
        final Account account = createAccount(Account.class);
        final AccountContact contact = createAccountContact(AccountContact.class);
        account.setContact(contact);

        final XBean xBean = new XBean();
        xBean.setTypeBoolean(true);
        xBean.setTypeBoolean2(Boolean.FALSE);
        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');
        xBean.setTypeByte((byte) 0);
        xBean.setTypeShort((short) 2);
        xBean.setTypeInt(3);
        xBean.setTypeLong(4);
        xBean.setTypeLong2((long) 5);
        xBean.setTypeFloat(1.01f);
        xBean.setTypeDouble(2.3134454d);

        xBean.setTypeString(">string< > </ <//");
        xBean.setWeekDay(WeekDay.FRIDAY);

        xBean.setTypeCalendar(Dates.currentCalendar());

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Set<Long> typeGenericSet = CommonUtil.asSortedSet();
        typeGenericSet.add(1332333L);
        typeGenericSet.add(Long.MAX_VALUE);
        typeGenericSet.add(Long.MIN_VALUE);
        xBean.setTypeGenericSet(typeGenericSet);

        final List typeList = new ArrayList();
        typeList.add(account.getFirstName());

        for (int i = 0; i < size; i++) {
            typeList.add(account);
        }

        typeList.add(account.getContact());
        typeList.add(account);
        typeList.add(null);
        typeList.add(new HashMap<>());
        typeList.add(new ArrayList<>());
        typeList.add(new HashSet<>());
        xBean.setTypeList(typeList);

        final Set typeSet = new HashSet<>();
        typeSet.add(new HashMap<>());
        typeSet.add(new ArrayList<>());
        typeSet.add(new HashMap<>());
        typeSet.add(new HashMap<>());
        typeSet.add(null);
        typeSet.add(null);
        typeSet.add(account);
        typeSet.add(account.getLastName());
        typeSet.add(account);
        typeSet.add(account.getContact());
        typeSet.add(account);
        typeSet.add(null);
        xBean.setTypeSet(typeSet);

        final Map<String, Account> typeGenericMap = new HashMap<>();
        typeGenericMap.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap.put(account.getLastName(), account);
        typeGenericMap.put(null, createAccount(Account.class));
        typeGenericMap.put("null", null);
        xBean.setTypeGenericMap(typeGenericMap);

        final Map<String, Object> typeGenericMap2 = new TreeMap<>();
        typeGenericMap2.put(account.getFirstName(), createAccount(Account.class));
        typeGenericMap2.put(account.getLastName(), createAccount(Account.class));
        typeGenericMap2.put("null", null);
        typeGenericMap2.put("bookList", CommonUtil.asList(createAccount(Account.class)));
        xBean.setTypeGenericMap2(typeGenericMap2);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put(createAccount(Account.class), createAccount(Account.class));
        typeGenericMap4.put("aaabbbccc", "");
        typeGenericMap4.put("bookList", CommonUtil.asList(createAccount(Account.class)));
        typeGenericMap4.put(" ", " ");
        typeGenericMap4.put(new HashMap<>(), " ");
        typeGenericMap4.put(new ArrayList<>(), new HashSet<>());
        typeGenericMap4.put(typeGenericMap2, typeGenericMap);
        xBean.setTypeGenericMap4(typeGenericMap4);

        final Map typeMap = new HashMap<>();

        for (int i = 0; i < size; i++) {
            typeMap.put(createAccount(Account.class), createAccount(Account.class));
        }

        typeMap.put("null", null);
        typeMap.put("bookList", CommonUtil.asList(createAccount(Account.class)));
        typeMap.put(" ", " ");
        typeMap.put(new HashMap<>(), " ");
        typeMap.put(new ArrayList<>(), new HashSet<>());
        typeMap.put(null, null);
        typeMap.put(new HashMap<>(), null);
        typeMap.put(new ArrayList<>(), new HashSet<>());
        xBean.setTypeMap(typeMap);

        return xBean;
    }

    @Test
    public void testSerialize4() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXMLParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize4_1() {
        final XBean xBean = new XBean();

        xBean.setTypeChar('<');
        xBean.setTypeChar2('>');

        xBean.setTypeString("");

        xBean.setTypeDate(new java.util.Date());
        xBean.setTypeSqlDate(Dates.currentDate());
        xBean.setTypeSqlTime(Dates.currentTime());
        xBean.setTypeSqlTimestamp(Dates.currentTimestamp());

        final List<Date> typeGenericList = new LinkedList<>();
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(Dates.currentDate());
        typeGenericList.add(null);
        xBean.setTypeGenericList(typeGenericList);

        final Map<Object, Object> typeGenericMap4 = new ConcurrentHashMap<>();
        typeGenericMap4.put("aaabbbccc", "");
        xBean.setTypeGenericMap4(typeGenericMap4);

        final String xml = abacusXMLDOMParser.serialize(xBean);
        println(xml);

        final String st = CommonUtil.stringOf(xBean);
        println(st);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        assertEquals(CommonUtil.stringOf(xBean), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize5() {
        final List<Account> accounts = createAccountWithContact(Account.class, 100);
        final String xml = abacusXMLParser.serialize(accounts);
        println(xml);

        final List<Account> xmlAccounts = abacusXMLParser.deserialize(xml, XDC.of(Account.class, true, null), List.class);

        N.println(CommonUtil.stringOf(accounts));
        N.println(CommonUtil.stringOf(xmlAccounts));

    }

    @Test
    public void testSerialize5_1() {
        final List<Account> accounts = createAccountWithContact(Account.class, 100);
        final String xml = abacusXMLDOMParser.serialize(accounts);
        println(xml);

        final List<Account> xmlAccounts = abacusXMLDOMParser.deserialize(xml, XDC.of(Account.class, true, null), List.class);

        N.println(CommonUtil.stringOf(accounts));
        N.println(CommonUtil.stringOf(xmlAccounts));

    }

    @Test
    public void testSerialize6() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXMLParser.serialize(account);
        println(xml);

        final com.landawn.abacus.entity.extendDirty.basic.Account xmlBean = abacusXMLParser.deserialize(xml,
                com.landawn.abacus.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize6_1() {
        final Account account = createAccountWithContact(Account.class);
        final String xml = abacusXMLDOMParser.serialize(account);
        println(xml);

        final com.landawn.abacus.entity.extendDirty.basic.Account xmlBean = abacusXMLDOMParser.deserialize(xml,
                com.landawn.abacus.entity.extendDirty.basic.Account.class);
        assertEquals(CommonUtil.stringOf(account), CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize7() throws Exception {
        final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
        final XBean xmlBean = abacusXMLParser.deserialize(is, XBean.class);
        is.close();

        N.println(CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testSerialize7_() throws Exception {
        final InputStream is = new FileInputStream("./src/test/resources/XBean.xml");
        final XBean xmlBean = abacusXMLDOMParser.deserialize(is, XBean.class);
        is.close();

        N.println(CommonUtil.stringOf(xmlBean));
    }

    @Test
    public void testXBean() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXMLParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXMLParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXMLParser.serialize(bean));
        N.println(abacusXMLParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXMLParser.deserialize(abacusXMLParser.serialize(bean), XBean.class),
                abacusXMLParser.deserialize(abacusXMLParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void testXBean_1() {
        final XBean bean = new XBean();
        final Set typeSet = new HashSet<>();
        typeSet.add(null);
        typeSet.add(new HashMap<>());
        bean.setTypeSet(typeSet);
        bean.setWeekDay(WeekDay.FRIDAY);
        bean.setTypeChar('0');

        final String xml = abacusXMLDOMParser.serialize(bean);
        println(xml);

        final XBean xmlBean = abacusXMLDOMParser.deserialize(xml, XBean.class);
        N.println(bean);
        N.println(xmlBean);
        N.println(abacusXMLDOMParser.serialize(bean));
        N.println(abacusXMLDOMParser.serialize(xmlBean));
        N.println(CommonUtil.stringOf(bean));
        N.println(CommonUtil.stringOf(xmlBean));
        assertEquals(bean, xmlBean);
        assertEquals(abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(bean), XBean.class),
                abacusXMLDOMParser.deserialize(abacusXMLDOMParser.serialize(xmlBean), XBean.class));

    }

    @Test
    public void testMarshaller() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + "kd ");
        customer.setAge(29);
        customer.setChar('c');

        final String xml = XmlUtil.marshal(customer);
        println(xml);

        final Customer newCustomer = XmlUtil.unmarshal(Customer.class, xml);

        assertEquals(customer, newCustomer);
    }

    @Test
    public void testXMLEncoder() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + (char) 0 + "kd ");
        customer.setAge(29);
        customer.setChar((char) 1);

        final String xml = XmlUtil.xmlEncode(customer);
        println(xml);

        final Customer newCustomer = XmlUtil.xmlDecode(xml);

        assertEquals(customer, newCustomer);
    }

    @Test
    public void testJAXB() throws JAXBException {
        final Customer customer = new Customer();
        customer.setId(100);
        customer.setName("mkyong" + ((char) 0) + "kd ");
        customer.setAge(29);
        customer.setChar('c');

        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(Customer.class);
            final Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            jaxbMarshaller.marshal(customer, System.out);
        } catch (final JAXBException e) {
            e.printStackTrace();
        }
    }

    public static class Bean_1 {
        private List<String> strList;
        private List intList;
        private List<Short> shortList;
        private XMLGregorianCalendar xmlGregorianCalendar;

        public List<String> getStrList() {
            return strList;
        }

        public void setStrList(final List<String> strList) {
            this.strList = strList;
        }

        public List getIntList() {
            return intList;
        }

        public void setIntList(final List intList) {
            this.intList = intList;
        }

        public List<Short> getShortList() {
            return shortList;
        }

        public void setShortList(final List<Short> shortList) {
            this.shortList = shortList;
        }

        public XMLGregorianCalendar getXmlGregorianCalendar() {
            return xmlGregorianCalendar;
        }

        public void setXmlGregorianCalendar(final XMLGregorianCalendar xmlGregorianCalendar) {
            this.xmlGregorianCalendar = xmlGregorianCalendar;
        }

        @Override
        public int hashCode() {
            return Objects.hash(intList, shortList, strList, xmlGregorianCalendar);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Bean_1 other = (Bean_1) obj;

            if (!Objects.equals(intList, other.intList) || !Objects.equals(shortList, other.shortList) || !Objects.equals(strList, other.strList)
                    || !Objects.equals(xmlGregorianCalendar, other.xmlGregorianCalendar)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Bean_1 [strList=" + strList + ", intList=" + intList + ", shortList=" + shortList + ", xmlGregorianCalendar=" + xmlGregorianCalendar + "]";
        }
    }

    @XmlRootElement
    public static class Customer {
        String name;
        char ch;
        int age;
        int id;

        public String getName() {
            return name;
        }

        @XmlElement
        public void setChar(final char ch) {
            this.ch = ch;
        }

        public char getChar() {
            return ch;
        }

        @XmlElement
        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        @XmlElement
        public void setAge(final int age) {
            this.age = age;
        }

        public int getId() {
            return id;
        }

        @XmlAttribute
        public void setId(final int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(age, ch, id, name);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Customer other = (Customer) obj;

            if ((age != other.age) || (ch != other.ch) || (id != other.id) || !Objects.equals(name, other.name)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Customer [name=" + name + ", ch=" + ch + ", age=" + age + ", id=" + id + "]";
        }
    }

    public static class Bean {
        private byte[] bytes;
        private char[] chars;
        private String[] strings;
        private List typeList;
        private Set typeSet;

        public byte[] getBytes() {
            return bytes;
        }

        public void setBytes(final byte[] bytes) {
            this.bytes = bytes;
        }

        public char[] getChars() {
            return chars;
        }

        public void setChars(final char[] chars) {
            this.chars = chars;
        }

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(final String[] strings) {
            this.strings = strings;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(final List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(final Set typeSet) {
            this.typeSet = typeSet;
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(bytes), Arrays.hashCode(chars), Arrays.hashCode(strings), typeList, typeSet);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final Bean other = (Bean) obj;

            if (!Arrays.equals(bytes, other.bytes) || !Arrays.equals(chars, other.chars) || !Arrays.equals(strings, other.strings)
                    || !Objects.equals(typeList, other.typeList)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Bean [bytes=" + Arrays.toString(bytes) + ", chars=" + Arrays.toString(chars) + ", strings=" + Arrays.toString(strings) + ", typeList="
                    + typeList + ", typeSet=" + typeSet + "]";
        }
    }

    public static class XBean {
        private boolean typeBoolean;
        private Boolean typeBoolean2;
        private char typeChar;
        private Character typeChar2;
        private byte typeByte;
        private short typeShort;
        private int typeInt;
        private long typeLong;
        private Long typeLong2;
        private float typeFloat;
        private double typeDouble;
        private String typeString;
        private Calendar typeCalendar;
        private java.util.Date typeDate;
        private Date typeSqlDate;
        private Time typeSqlTime;
        private Timestamp typeSqlTimestamp;
        private WeekDay weekDay;
        private List<Date> typeGenericList;
        private Set<Long> typeGenericSet;
        private List typeList;
        private Set typeSet;
        private Map<String, Account> typeGenericMap;
        private Map<String, Object> typeGenericMap2;
        private Map<Object, String> typeGenericMap3;
        private Map<Object, Object> typeGenericMap4;
        private Map typeMap;

        public boolean getTypeBoolean() {
            return typeBoolean;
        }

        public void setTypeBoolean(final boolean typeBoolean) {
            this.typeBoolean = typeBoolean;
        }

        public Boolean getTypeBoolean2() {
            return typeBoolean2;
        }

        public void setTypeBoolean2(final Boolean typeBoolean2) {
            this.typeBoolean2 = typeBoolean2;
        }

        public char getTypeChar() {
            return typeChar;
        }

        public void setTypeChar(final char typeChar) {
            this.typeChar = typeChar;
        }

        public Character getTypeChar2() {
            return typeChar2;
        }

        public void setTypeChar2(final Character typeChar2) {
            this.typeChar2 = typeChar2;
        }

        public byte getTypeByte() {
            return typeByte;
        }

        public void setTypeByte(final byte typeByte) {
            this.typeByte = typeByte;
        }

        public short getTypeShort() {
            return typeShort;
        }

        public void setTypeShort(final short typeShort) {
            this.typeShort = typeShort;
        }

        public int getTypeInt() {
            return typeInt;
        }

        public void setTypeInt(final int typeInt) {
            this.typeInt = typeInt;
        }

        public long getTypeLong() {
            return typeLong;
        }

        public void setTypeLong(final long typeLong) {
            this.typeLong = typeLong;
        }

        public Long getTypeLong2() {
            return typeLong2;
        }

        public void setTypeLong2(final Long typeLong2) {
            this.typeLong2 = typeLong2;
        }

        public float getTypeFloat() {
            return typeFloat;
        }

        public void setTypeFloat(final float typeFloat) {
            this.typeFloat = typeFloat;
        }

        public double getTypeDouble() {
            return typeDouble;
        }

        public void setTypeDouble(final double typeDouble) {
            this.typeDouble = typeDouble;
        }

        public String getTypeString() {
            return typeString;
        }

        public void setTypeString(final String typeString) {
            this.typeString = typeString;
        }

        public Calendar getTypeCalendar() {
            return typeCalendar;
        }

        public void setTypeCalendar(final Calendar typeCalendar) {
            this.typeCalendar = typeCalendar;
        }

        public java.util.Date getTypeDate() {
            return typeDate;
        }

        public void setTypeDate(final java.util.Date typeDate) {
            this.typeDate = typeDate;
        }

        public Date getTypeSqlDate() {
            return typeSqlDate;
        }

        public void setTypeSqlDate(final Date typeSqlDate) {
            this.typeSqlDate = typeSqlDate;
        }

        public Time getTypeSqlTime() {
            return typeSqlTime;
        }

        public void setTypeSqlTime(final Time typeSqlTime) {
            this.typeSqlTime = typeSqlTime;
        }

        public Timestamp getTypeSqlTimestamp() {
            return typeSqlTimestamp;
        }

        public void setTypeSqlTimestamp(final Timestamp typeSqlTimestamp) {
            this.typeSqlTimestamp = typeSqlTimestamp;
        }

        public WeekDay getWeekDay() {
            return weekDay;
        }

        public void setWeekDay(final WeekDay weekDay) {
            this.weekDay = weekDay;
        }

        public List<Date> getTypeGenericList() {
            return typeGenericList;
        }

        public void setTypeGenericList(final List<Date> typeGenericList) {
            this.typeGenericList = typeGenericList;
        }

        public Set<Long> getTypeGenericSet() {
            return typeGenericSet;
        }

        public void setTypeGenericSet(final Set<Long> typeGenericSet) {
            this.typeGenericSet = typeGenericSet;
        }

        public List getTypeList() {
            return typeList;
        }

        public void setTypeList(final List typeList) {
            this.typeList = typeList;
        }

        public Set getTypeSet() {
            return typeSet;
        }

        public void setTypeSet(final Set typeSet) {
            this.typeSet = typeSet;
        }

        public Map<String, Account> getTypeGenericMap() {
            return typeGenericMap;
        }

        public void setTypeGenericMap(final Map<String, Account> typeGenericMap) {
            this.typeGenericMap = typeGenericMap;
        }

        public Map getTypeMap() {
            return typeMap;
        }

        public void setTypeMap(final Map typeMap) {
            this.typeMap = typeMap;
        }

        public Map<String, Object> getTypeGenericMap2() {
            return typeGenericMap2;
        }

        public void setTypeGenericMap2(final Map<String, Object> typeGenericMap2) {
            this.typeGenericMap2 = typeGenericMap2;
        }

        public Map<Object, String> getTypeGenericMap3() {
            return typeGenericMap3;
        }

        public void setTypeGenericMap3(final Map<Object, String> typeGenericMap3) {
            this.typeGenericMap3 = typeGenericMap3;
        }

        public Map<Object, Object> getTypeGenericMap4() {
            return typeGenericMap4;
        }

        public void setTypeGenericMap4(final Map<Object, Object> typeGenericMap4) {
            this.typeGenericMap4 = typeGenericMap4;
        }

        @Override
        public int hashCode() {
            return Objects.hash(typeBoolean, typeBoolean2, typeByte, typeCalendar, typeChar, typeChar2, typeDate, typeDouble, typeFloat, typeGenericList,
                    typeGenericMap, typeGenericMap2, typeGenericMap3, typeGenericMap4, typeGenericSet, typeInt, typeList, typeLong, typeLong2, typeMap, typeSet,
                    typeShort, typeSqlDate, typeSqlTime, typeSqlTimestamp, typeString);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final XBean other = (XBean) obj;

            if ((typeBoolean != other.typeBoolean) || !Objects.equals(typeBoolean2, other.typeBoolean2) || (typeByte != other.typeByte)
                    || !Objects.equals(typeCalendar, other.typeCalendar)) {
                return false;
            }

            if ((typeChar != other.typeChar) || !Objects.equals(typeChar2, other.typeChar2) || !Objects.equals(typeDate, other.typeDate)
                    || (Double.doubleToLongBits(typeDouble) != Double.doubleToLongBits(other.typeDouble))) {
                return false;
            }

            if ((Float.floatToIntBits(typeFloat) != Float.floatToIntBits(other.typeFloat)) || !Objects.equals(typeGenericList, other.typeGenericList)
                    || !Objects.equals(typeGenericMap, other.typeGenericMap) || !Objects.equals(typeGenericMap2, other.typeGenericMap2)) {
                return false;
            }

            if (!Objects.equals(typeGenericMap3, other.typeGenericMap3) || !Objects.equals(typeGenericMap4, other.typeGenericMap4)
                    || !Objects.equals(typeGenericSet, other.typeGenericSet) || (typeInt != other.typeInt)) {
                return false;
            }

            if (!Objects.equals(typeList, other.typeList) || (typeLong != other.typeLong) || !Objects.equals(typeLong2, other.typeLong2)
                    || !Objects.equals(typeMap, other.typeMap)) {
                return false;
            }

            if (!Objects.equals(typeSet, other.typeSet) || (typeShort != other.typeShort) || !Objects.equals(typeSqlDate, other.typeSqlDate)
                    || !Objects.equals(typeSqlTime, other.typeSqlTime)) {
                return false;
            }

            if (!Objects.equals(typeSqlTimestamp, other.typeSqlTimestamp) || !Objects.equals(typeString, other.typeString)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "Bean [typeBoolean=" + typeBoolean + ", typeBoolean2=" + typeBoolean2 + ", typeChar=" + typeChar + ", typeChar2=" + typeChar2 + ", typeByte="
                    + typeByte + ", typeShort=" + typeShort + ", typeInt=" + typeInt + ", typeLong=" + typeLong + ", typeLong2=" + typeLong2 + ", typeFloat="
                    + typeFloat + ", typeDouble=" + typeDouble + ", typeString=" + typeString + ", typeCalendar=" + typeCalendar + ", typeDate=" + typeDate
                    + ", typeSqlDate=" + typeSqlDate + ", typeSqlTime=" + typeSqlTime + ", typeSqlTimestamp=" + typeSqlTimestamp + ", typeGenericList="
                    + typeGenericList + ", typeGenericSet=" + typeGenericSet + ", typeList=" + typeList + ", typeSet=" + typeSet + ", typeGenericMap="
                    + typeGenericMap + ", typeGenericMap2=" + typeGenericMap2 + ", typeGenericMap3=" + typeGenericMap3 + ", typeGenericMap4=" + typeGenericMap4
                    + ", typeMap=" + typeMap + "]";
        }
    }

    public static class JaxbBean {
        private String string;
        private List<String> list;
        private Map<String, String> map;

        public String getString() {
            return string;
        }

        public void setString(final String string) {
            this.string = string;
        }

        public List<String> getList() {
            if (list == null) {
                list = new ArrayList<>();
            }

            return list;
        }

        public Map<String, String> getMap() {
            if (map == null) {
                map = new HashMap<>();
            }

            return map;
        }

        @Override
        public int hashCode() {
            return Objects.hash(list, map, string);
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }

            if ((obj == null) || (getClass() != obj.getClass())) {
                return false;
            }

            final JaxbBean other = (JaxbBean) obj;

            if (!Objects.equals(list, other.list) || !Objects.equals(map, other.map) || !Objects.equals(string, other.string)) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "JaxBBean [string=" + string + ", list=" + list + ", map=" + map + "]";
        }
    }
}
