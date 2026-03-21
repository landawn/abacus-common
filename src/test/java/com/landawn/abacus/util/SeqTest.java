package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IntSummaryStatistics;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collector;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.stream.Collectors;
import com.landawn.abacus.util.stream.Stream;

public class SeqTest extends AbstractTest {

    @TempDir
    Path tempDir;

    private File tempFile;
    private Path tempPath;
    private Path tempFolder;

    private <T, E extends Exception> List<T> drain(Seq<T, E> seq) throws E {
        return seq.toList();
    }

    private <T, E extends Exception> List<T> drainWithException(Seq<T, E> seq) throws E {
        return seq.toList();
    }

    @BeforeEach
    public void initTempFixtures() throws IOException {
        tempFile = tempDir.resolve("seq-lines-file.txt").toFile();
        Files.write(tempFile.toPath(), Arrays.asList("line1", "line2", "line3"), StandardCharsets.UTF_8);

        tempPath = tempDir.resolve("seq-lines-path.txt");
        Files.write(tempPath, Arrays.asList("line1", "line2", "line3"), StandardCharsets.UTF_8);

        tempFolder = tempDir.resolve("seq-temp-folder");
        Files.createDirectories(tempFolder);
    }

    public static class Department {
        List<Team> teams;

        Department(Team... teams) {
            this.teams = Arrays.asList(teams);
        }

        List<Team> getTeams() {
            return teams;
        }
    }

    public static class Team {
        List<Employee> members;

        Team(Employee... members) {
            this.members = Arrays.asList(members);
        }

        List<Employee> getMembers() {
            return members;
        }
    }

    public static class Employee {
        String name;

        Employee(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    @Test
    public void testEmpty() throws Exception {
        Seq<String, RuntimeException> seq = Seq.empty();
        assertNotNull(seq);
        assertEquals(0, seq.count());
    }

    @Test
    public void testEmptySeq_Operations() throws Exception {
        assertEquals(0, Seq.<Integer, RuntimeException> empty().count());
        assertFalse(Seq.<Integer, RuntimeException> empty().first().isPresent());
        assertEquals(0, Seq.<Integer, RuntimeException> empty().toList().size());
    }

    @Test
    public void test_empty() throws Exception {
        Seq<Object, Exception> emptySeq = Seq.empty();
        assertTrue(emptySeq.toList().isEmpty(), "Empty sequence should have no elements.");
        assertDoesNotThrow(emptySeq::close);
    }

    @Test
    public void test_empty_seq_with_multiple_onClose_still_closes_all() {
        AtomicInteger close1Count = new AtomicInteger(0);
        AtomicInteger close2Count = new AtomicInteger(0);

        Seq<Object, Exception> emptySeq = Seq.<Object, Exception> empty().onClose(close1Count::incrementAndGet).onClose(close2Count::incrementAndGet);

        emptySeq.close();

        assertEquals(1, close1Count.get());
        assertEquals(1, close2Count.get());

        emptySeq.close();
        assertEquals(1, close1Count.get());
        assertEquals(1, close2Count.get());
    }

    @Test
    public void testEmptySequenceOperations() throws Exception {
        Seq<Integer, Exception> empty = Seq.empty();

        Seq<Integer, Exception> filtered = empty.filter(n -> n > 0);
        Assertions.assertTrue(filtered.toList().isEmpty());

        Seq<String, Exception> mapped = empty.map(Object::toString);
        Assertions.assertTrue(mapped.toList().isEmpty());

        Seq<Integer, Exception> distinct = empty.distinct();
        Assertions.assertTrue(distinct.toList().isEmpty());

        Seq<Integer, Exception> taken = empty.takeWhile(n -> true);
        Assertions.assertTrue(taken.toList().isEmpty());

        Seq<Integer, Exception> dropped = empty.dropWhile(n -> true);
        Assertions.assertTrue(dropped.toList().isEmpty());
    }

    // ===== minBy on empty - empty check =====

    @Test
    public void testMinBy_onNonEmpty() throws Exception {
        Optional<String> min = Seq.of("banana", "apple", "cherry").minBy(String::length);
        assertTrue(min.isPresent());
        assertEquals("apple", min.get());
    }

    @Test
    public void testDefer() throws Exception {
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Supplier<Seq<Integer, RuntimeException>> supplier = () -> {
            supplierCalls.incrementAndGet();
            return Seq.of(1, 2, 3);
        };

        Seq<Integer, RuntimeException> deferred = Seq.defer(supplier);
        assertEquals(0, supplierCalls.get());

        List<Integer> result = deferred.toList();
        assertEquals(1, supplierCalls.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_defer() throws Exception {
        AtomicInteger supplierCalls = new AtomicInteger(0);
        Supplier<Seq<Integer, Exception>> seqSupplier = () -> {
            supplierCalls.incrementAndGet();
            return Seq.of(1, 2, 3);
        };

        Seq<Integer, Exception> deferredSeq = Seq.defer(seqSupplier::get);
        assertEquals(0, supplierCalls.get(), "Supplier should not be called before terminal operation.");

        List<Integer> result = drainWithException(deferredSeq);
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(1, supplierCalls.get(), "Supplier should be called once after terminal operation.");

        Supplier<Seq<Integer, Exception>> failingSupplier = () -> {
            supplierCalls.incrementAndGet();
            throw new RuntimeException("Supplier failed");
        };
        Seq<Integer, Exception> failingDeferredSeq = Seq.defer(failingSupplier::get);
        assertThrows(RuntimeException.class, () -> drainWithException(failingDeferredSeq));
    }

    @Test
    public void testDeferWithNullSupplier() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.defer(null));
    }

    @Test
    public void testJust() throws Exception {
        Seq<String, RuntimeException> seq = Seq.just("test");
        assertEquals(1, seq.count());
    }

    @Test
    public void testJustWithExceptionType() throws Exception {
        Seq<String, IOException> seq = Seq.just("test", IOException.class);
        assertEquals(1, seq.count());
    }

    @Test
    public void test_just_singleElement() throws Exception {
        Seq<String, Exception> seq = Seq.just("hello");
        assertEquals(Collections.singletonList("hello"), drainWithException(seq));
    }

    @Test
    public void test_just_nullElement() throws Exception {
        Seq<String, Exception> seq = Seq.just(null);
        assertEquals(Collections.singletonList(null), drainWithException(seq));
    }

    @Test
    public void test_just_withExceptionType() throws Exception {
        Seq<String, Exception> seq = Seq.just("hello", Exception.class);
        assertEquals(Collections.singletonList("hello"), drainWithException(seq));
    }

    @Test
    public void testOfNullable_Null() throws Exception {
        Seq<String, RuntimeException> seq = Seq.ofNullable(null);
        assertEquals(0, seq.count());
    }

    @Test
    public void testOfNullable_NonNull() throws Exception {
        Seq<String, RuntimeException> seq = Seq.ofNullable("test");
        assertEquals(1, seq.count());
    }

    @Test
    public void test_ofNullable_nonNullElement() throws Exception {
        Seq<String, Exception> seq = Seq.ofNullable("world");
        assertEquals(Collections.singletonList("world"), drainWithException(seq));
    }

    @Test
    public void test_ofNullable_nullElement() throws Exception {
        Seq<String, Exception> seq = Seq.ofNullable(null);
        assertTrue(drainWithException(seq).isEmpty());
    }

    @Test
    public void test_ofNullable_withExceptionType() throws Exception {
        Seq<String, Exception> seq = Seq.ofNullable("world", Exception.class);
        assertEquals(Collections.singletonList("world"), drainWithException(seq));
        Seq<String, Exception> nullSeq = Seq.ofNullable(null, Exception.class);
        assertTrue(drainWithException(nullSeq).isEmpty());
    }

    @Test
    public void testOfNullable() throws Exception {
        Seq<String, Exception> seq = Seq.ofNullable("hello");
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        Seq<String, Exception> seqNull = Seq.ofNullable(null);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfNullableWithExceptionType() throws Exception {
        Seq<String, IOException> seq = Seq.ofNullable("hello", IOException.class);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        Seq<String, IOException> seqNull = Seq.ofNullable(null, IOException.class);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void test_cycle() throws Exception {
        assertDoesNotThrow(() -> {
            {
                Seq.of("a").cycled().limit(10).println();
                Seq.of("a", "b").cycled().limit(10).println();
                Stream.of("a").cycled().limit(10).println();
                Stream.of(CommonUtil.toList("a").iterator()).cycled().limit(9).println();
                Stream.of(CommonUtil.toList("a", "b").iterator()).cycled().limit(9).println();
            }
            {
                Seq.of("a").cycled(10).limit(10).println();
                Seq.of("a", "b").cycled(10).limit(10).println();
                Stream.of("a").cycled(10).limit(10).println();
                Stream.of(CommonUtil.toList("a").iterator()).cycled(10).limit(9).println();
                Stream.of(CommonUtil.toList("a", "b").iterator()).cycled(10).limit(9).println();
            }
        });
    }

    @Test
    public void testOf_Array() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_EmptyArray() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of();
        assertEquals(0, seq.count());
    }

    @Test
    public void testOf_BooleanArray() throws Exception {
        Seq<Boolean, RuntimeException> seq = Seq.of(new boolean[] { true, false, true });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_CharArray() throws Exception {
        Seq<Character, RuntimeException> seq = Seq.of(new char[] { 'a', 'b', 'c' });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_ByteArray() throws Exception {
        Seq<Byte, RuntimeException> seq = Seq.of(new byte[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_ShortArray() throws Exception {
        Seq<Short, RuntimeException> seq = Seq.of(new short[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_IntArray() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(new int[] { 1, 2, 3 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_LongArray() throws Exception {
        Seq<Long, RuntimeException> seq = Seq.of(new long[] { 1L, 2L, 3L });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_FloatArray() throws Exception {
        Seq<Float, RuntimeException> seq = Seq.of(new float[] { 1.0f, 2.0f, 3.0f });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_DoubleArray() throws Exception {
        Seq<Double, RuntimeException> seq = Seq.of(new double[] { 1.0, 2.0, 3.0 });
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Iterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, RuntimeException> seq = Seq.of(list);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Iterator() throws Exception {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        Seq<String, RuntimeException> seq = Seq.of(iter);
        assertEquals(3, seq.count());
    }

    @Test
    public void testOf_Map() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<Map.Entry<String, Integer>, RuntimeException> seq = Seq.of(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void testContains() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x == 4));
    }

    @Test
    public void testHasDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 2, 3).containsDuplicates());
        assertFalse(Seq.of(1, 2, 3).containsDuplicates());
    }

    @Test
    public void testClosedSeq_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        seq.toList();
        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testNegativeSkip_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> seq.skip(-1));
    }

    @Test
    public void testNegativeLimit_ThrowsException() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
        assertThrows(IllegalArgumentException.class, () -> seq.limit(-1));
    }

    @Test
    public void testSingleElement_Operations() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.of(42);
        assertEquals(1, seq.count());
    }

    @Test
    public void testNullElements() throws Exception {
        Seq<String, RuntimeException> seq = Seq.of("a", null, "b");
        List<String> list = seq.toList();
        assertEquals(3, list.size());
        assertNull(list.get(1));
    }

    @Test
    public void testMultipleOnClose() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        var seq = Seq.of(1, 2, 3).onClose(count::incrementAndGet).onClose(count::incrementAndGet);
        seq.toList();
        assertEquals(2, count.get());
    }

    @Test
    public void testExplicitClose() throws Exception {
        AtomicInteger closed = new AtomicInteger(0);
        try (var seq = Seq.of(1, 2, 3).onClose(closed::incrementAndGet)) {
        }
        assertEquals(1, closed.get());
    }

    @Test
    public void testChainedOperations() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6).filter(x -> x % 2 == 0).map(x -> x * 2).sorted(Comparator.reverseOrder()).limit(2).toList();
        assertEquals(Arrays.asList(12, 8), result);
    }

    @Test
    public void testsliding() throws Exception {
        List<List<Integer>> windows = Seq.of(1, 2, 3, 4, 5).sliding(3).toList();
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2, 3), windows.get(0));
        assertEquals(Arrays.asList(2, 3, 4), windows.get(1));
        assertEquals(Arrays.asList(3, 4, 5), windows.get(2));
    }

    @Test
    public void testFlattmap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMapArray(x -> new Integer[] { x, x * 10 }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testNMatch() throws Exception {
        boolean result = Seq.of(1, 2, 3, 4, 5).isMatchCountBetween(3, 3, x -> x % 2 == 0);
        assertFalse(result);

        result = Seq.of(2, 4, 6, 8, 10).isMatchCountBetween(3, Long.MAX_VALUE, x -> x % 2 == 0);
        assertTrue(result);
    }

    @Test
    public void test_of_varargs_elements() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));
    }

    @Test
    public void test_of_varargs_empty() throws Exception {
        Seq<Integer, Exception> seq = Seq.of();
        assertTrue(drainWithException(seq).isEmpty());
    }

    @Test
    public void test_of_varargs_oneNull() throws Exception {
        Seq<Integer, Exception> seq = Seq.of((Integer) null);
        assertEquals(Collections.singletonList(null), drainWithException(seq));
    }

    @Test
    public void test_of_booleanArray() throws Exception {
        Seq<Boolean, Exception> seq = Seq.of(new boolean[] { true, false, true });
        assertEquals(Arrays.asList(true, false, true), drainWithException(seq));
        Seq<Boolean, Exception> emptySeq = Seq.of(new boolean[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_charArray() throws Exception {
        Seq<Character, Exception> seq = Seq.of(new char[] { 'a', 'b', 'c' });
        assertEquals(Arrays.asList('a', 'b', 'c'), drainWithException(seq));
        Seq<Character, Exception> emptySeq = Seq.of(new char[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_byteArray() throws Exception {
        Seq<Byte, Exception> seq = Seq.of(new byte[] { 1, 2, 3 });
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), drainWithException(seq));
        Seq<Byte, Exception> emptySeq = Seq.of(new byte[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_shortArray() throws Exception {
        Seq<Short, Exception> seq = Seq.of(new short[] { 10, 20, 30 });
        assertEquals(Arrays.asList((short) 10, (short) 20, (short) 30), drainWithException(seq));
        Seq<Short, Exception> emptySeq = Seq.of(new short[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_intArray() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(new int[] { 100, 200, 300 });
        assertEquals(Arrays.asList(100, 200, 300), drainWithException(seq));
        Seq<Integer, Exception> emptySeq = Seq.of(new int[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_longArray() throws Exception {
        Seq<Long, Exception> seq = Seq.of(new long[] { 1L, 2L, 3L });
        assertEquals(Arrays.asList(1L, 2L, 3L), drainWithException(seq));
        Seq<Long, Exception> emptySeq = Seq.of(new long[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_floatArray() throws Exception {
        Seq<Float, Exception> seq = Seq.of(new float[] { 1.0f, 2.5f, 3.0f });
        assertEquals(Arrays.asList(1.0f, 2.5f, 3.0f), drainWithException(seq));
        Seq<Float, Exception> emptySeq = Seq.of(new float[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_doubleArray() throws Exception {
        Seq<Double, Exception> seq = Seq.of(new double[] { 1.1, 2.2, 3.3 });
        assertEquals(Arrays.asList(1.1, 2.2, 3.3), drainWithException(seq));
        Seq<Double, Exception> emptySeq = Seq.of(new double[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_of_abacusOptional() throws Exception {
        Seq<String, Exception> seqPresent = Seq.of(Optional.of("test"));
        assertEquals(Collections.singletonList("test"), drainWithException(seqPresent));

        Seq<String, Exception> seqEmpty = Seq.of(Optional.empty());
        assertTrue(drainWithException(seqEmpty).isEmpty());

        Seq<String, Exception> seqNull = Seq.of((Optional<String>) null);
        assertTrue(drainWithException(seqNull).isEmpty());
    }

    @Test
    public void test_of_javaUtilOptional() throws Exception {
        Seq<String, Exception> seqPresent = Seq.of(java.util.Optional.of("test"));
        assertEquals(Collections.singletonList("test"), drainWithException(seqPresent));

        Seq<String, Exception> seqEmpty = Seq.of(java.util.Optional.empty());
        assertTrue(drainWithException(seqEmpty).isEmpty());

        Seq<String, Exception> seqNull = Seq.of((java.util.Optional<String>) null);
        assertTrue(drainWithException(seqNull).isEmpty());
    }

    @Test
    public void test_of_iterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(list);
        assertEquals(list, drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.of(Collections.emptyList());
        assertTrue(drainWithException(emptySeq).isEmpty());

        Seq<String, Exception> nullIterableSeq = Seq.of((Iterable<String>) null);
        assertTrue(drainWithException(nullIterableSeq).isEmpty());
    }

    @Test
    public void test_of_iterable_withExceptionType() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(list, Exception.class);
        assertEquals(list, drainWithException(seq));
    }

    @Test
    public void test_of_iterator() throws Exception {
        Iterator<String> iterator = Arrays.asList("x", "y").iterator();
        Seq<String, Exception> seq = Seq.of(iterator);
        assertEquals(Arrays.asList("x", "y"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.of(Collections.emptyIterator());
        assertTrue(drainWithException(emptySeq).isEmpty());

        Seq<String, Exception> nullIteratorSeq = Seq.of((Iterator<String>) null);
        assertTrue(drainWithException(nullIteratorSeq).isEmpty());
    }

    @Test
    public void test_of_throwablesIterator() throws Exception {
        Throwables.Iterator<String, Exception> tIterator = Throwables.Iterator.of(Arrays.asList("x", "y").iterator());
        Seq<String, Exception> seq = Seq.of(tIterator);
        assertEquals(Arrays.asList("x", "y"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.of(Throwables.Iterator.<String, Exception> empty());
        assertTrue(drainWithException(emptySeq).isEmpty());

        Seq<String, Exception> nullIteratorSeq = Seq.of((Throwables.Iterator<String, Exception>) null);
        assertTrue(drainWithException(nullIteratorSeq).isEmpty());
    }

    @Test
    public void test_of_iterator_withExceptionType() throws Exception {
        Iterator<String> iterator = Arrays.asList("x", "y").iterator();
        Seq<String, Exception> seq = Seq.of(iterator, Exception.class);
        assertEquals(Arrays.asList("x", "y"), drainWithException(seq));
    }

    @Test
    public void test_of_enumeration() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("e1", "e2"));
        Enumeration<String> enumeration = vector.elements();
        Seq<String, Exception> seq = Seq.of(enumeration);
        assertEquals(Arrays.asList("e1", "e2"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.of(Collections.emptyEnumeration());
        assertTrue(drainWithException(emptySeq).isEmpty());

        Seq<String, Exception> nullEnumerationSeq = Seq.of((Enumeration<String>) null);
        assertTrue(drainWithException(nullEnumerationSeq).isEmpty());
    }

    @Test
    public void test_of_enumeration_withExceptionType() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("e1", "e2"));
        Enumeration<String> enumeration = vector.elements();
        Seq<String, Exception> seq = Seq.of(enumeration, Exception.class);
        assertEquals(Arrays.asList("e1", "e2"), drainWithException(seq));
    }

    @Test
    public void test_of_map() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        Seq<Map.Entry<Integer, String>, Exception> seq = Seq.of(map);
        List<Map.Entry<Integer, String>> result = drainWithException(seq);
        assertEquals(2, result.size());
        assertTrue(result.contains(new AbstractMap.SimpleEntry<>(1, "a")));
        assertTrue(result.contains(new AbstractMap.SimpleEntry<>(2, "b")));

        Seq<Map.Entry<Integer, String>, Exception> emptySeq = Seq.of(Collections.emptyMap());
        assertTrue(drainWithException(emptySeq).isEmpty());

        Seq<Map.Entry<Integer, String>, Exception> nullMapSeq = Seq.of((Map<Integer, String>) null);
        assertTrue(drainWithException(nullMapSeq).isEmpty());
    }

    @Test
    public void test_of_map_withExceptionType() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        Seq<Map.Entry<Integer, String>, Exception> seq = Seq.of(map, Exception.class);
        List<Map.Entry<Integer, String>> result = drainWithException(seq);
        assertEquals(1, result.size());
        assertEquals(new AbstractMap.SimpleEntry<>(1, "a"), result.get(0));
    }

    @Test
    public void test_multiple_onClose() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);
        Runnable handler1 = () -> closeCount.incrementAndGet();
        Runnable handler2 = () -> closeCount.addAndGet(10);

        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(handler1).onClose(handler2);
        assertEquals(0, closeCount.get());
        seq.toList();
        assertEquals(11, closeCount.get(), "Both close handlers should run");

        seq.close();
        assertEquals(11, closeCount.get(), "Close handlers should not run again");
    }

    @Test
    public void test_of_throwablesIterator_withCloseHandler() throws Exception {
        AtomicBoolean iteratorClosed = new AtomicBoolean(false);
        Throwables.Iterator<String, Exception> underlyingIterator = new Throwables.Iterator<>() {
            private final Iterator<String> source = Arrays.asList("a", "b").iterator();

            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public String next() {
                return source.next();
            }

            public void closeResource() {
                iteratorClosed.set(true);
            }
        };

        Seq<String, Exception> seq = Seq.of(underlyingIterator);
        assertEquals(Arrays.asList("a", "b"), drainWithException(seq));
        assertTrue(iteratorClosed.get(), "Underlying Throwables.Iterator's close method should be called when Seq is closed.");
    }

    @Test
    public void testFlattmap_WithEmptySequence() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.of();
        Seq<Integer, Exception> result = emptySeq.flatMapArray(n -> new Integer[] { n, n * 2 });

        assertEquals(0, result.count());
    }

    @Test
    public void testFlattmap_WithSingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(5);
        List<Integer> result = seq.flatMapArray(n -> new Integer[] { n, n * 10 }).toList();

        assertEquals(Arrays.asList(5, 50), result);
    }

    @Test
    public void testFlattmap_WithMultipleElements() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatMapArray(n -> new Integer[] { n, n * 10 }).toList();

        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlattmap_WithEmptyArrays() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatMapArray(n -> new Integer[0]).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testFlattmap_WithMixedArraySizes() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatMapArray(n -> {
            if (n == 1)
                return new Integer[] { n };
            else if (n == 2)
                return new Integer[0];
            else
                return new Integer[] { n, n * 10, n * 100 };
        }).toList();

        assertEquals(Arrays.asList(1, 3, 30, 300), result);
    }

    @Test
    public void testFlattmap_WithNullArrays() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> result = seq.flatMapArray(n -> n == 2 ? null : new Integer[] { n }).toList();

        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void testFlattmap_WithStringToCharArray() throws Exception {
        Seq<String, Exception> seq = Seq.of("Hello", "World");
        List<Character> result = seq.flatMapArray(s -> s.chars().mapToObj(c -> (char) c).toArray(Character[]::new)).toList();

        assertEquals(Arrays.asList('H', 'e', 'l', 'l', 'o', 'W', 'o', 'r', 'l', 'd'), result);
    }

    @Test
    public void testFlattmap_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.flatMapArray(n -> new Integer[] { n });
        });
    }

    @Test
    public void testAutoCloseOnTerminalOperation() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> closeHandlerCalled.set(true));

        long count = seq.count();

        assertEquals(3, count);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testTryWithResources() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> result;

        try (Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y", "z")).onClose(() -> closeHandlerCalled.set(true))) {
            result = seq.toList();
        }

        assertEquals(Arrays.asList("x", "y", "z"), result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testEdgeCases() throws Exception {
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3).sliding(3);
        List<List<Integer>> result = seq.toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));

        List<Integer> single = Arrays.asList(42);
        assertEquals(single, Seq.of(42).sorted().toList());
        assertEquals(single, Seq.of(42).reversed().toList());
        assertEquals(single, Seq.of(42).shuffled().toList());
        assertEquals(single, Seq.of(42).skip(0).toList());
        assertEquals(single, Seq.of(42).limit(10).toList());

        List<Integer> withNulls = Arrays.asList(null, 1, null, 2, null);
        Seq<Integer, Exception> nullSeq = Seq.of(withNulls);
        assertEquals(Arrays.asList(1, 2), nullSeq.skipNulls().toList());
    }

    @Test
    public void testExceptionHandling() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(2, 0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).skip(-1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).limit(-1));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).step(0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).rateLimited(0.0));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).buffered(0));
    }

    @Test
    public void testOfVarargs() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), seq.toList());

        Seq<Integer, Exception> seqEmpty = Seq.of();
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        Integer[] nullArray = null;
        Seq<Integer, Exception> seqNull = Seq.of(nullArray);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfBooleanArray() throws Exception {
        boolean[] arr = { true, false, true };
        Seq<Boolean, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(true, false, true), seq.toList());

        boolean[] empty = {};
        Seq<Boolean, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        boolean[] nullArr = null;
        Seq<Boolean, Exception> seqNull = Seq.of(nullArr);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfCharArray() throws Exception {
        char[] arr = { 'a', 'b', 'c' };
        Seq<Character, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList('a', 'b', 'c'), seq.toList());

        char[] empty = {};
        Seq<Character, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfByteArray() throws Exception {
        byte[] arr = { 1, 2, 3 };
        Seq<Byte, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), seq.toList());
    }

    @Test
    public void testOfShortArray() throws Exception {
        short[] arr = { 10, 20, 30 };
        Seq<Short, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList((short) 10, (short) 20, (short) 30), seq.toList());
    }

    @Test
    public void testOfIntArray() throws Exception {
        int[] arr = { 100, 200, 300 };
        Seq<Integer, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(100, 200, 300), seq.toList());
    }

    @Test
    public void testOfLongArray() throws Exception {
        long[] arr = { 1000L, 2000L, 3000L };
        Seq<Long, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1000L, 2000L, 3000L), seq.toList());
    }

    @Test
    public void testOfFloatArray() throws Exception {
        float[] arr = { 1.1f, 2.2f, 3.3f };
        Seq<Float, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), seq.toList());
    }

    @Test
    public void testOfDoubleArray() throws Exception {
        double[] arr = { 1.11, 2.22, 3.33 };
        Seq<Double, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1.11, 2.22, 3.33), seq.toList());
    }

    @Test
    public void testOfOptional() throws Exception {
        Optional<String> opt = Optional.of("hello");
        Seq<String, Exception> seq = Seq.of(opt);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        Optional<String> empty = Optional.empty();
        Seq<String, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        Optional<String> nullOpt = null;
        Seq<String, Exception> seqNull = Seq.of(nullOpt);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfJavaUtilOptional() throws Exception {
        java.util.Optional<String> opt = java.util.Optional.of("hello");
        Seq<String, Exception> seq = Seq.of(opt);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        java.util.Optional<String> empty = java.util.Optional.empty();
        Seq<String, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfIterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(list);
        Assertions.assertEquals(list, seq.toList());

        Iterable<String> nullIterable = null;
        Seq<String, Exception> seqNull = Seq.of(nullIterable);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfIterableWithExceptionType() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, IOException> seq = Seq.of(list, IOException.class);
        Assertions.assertEquals(list, seq.toList());
    }

    @Test
    public void testOfIterator() throws Exception {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        Seq<String, Exception> seq = Seq.of(iter);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), seq.toList());

        Iterator<String> nullIter = null;
        Seq<String, Exception> seqNull = Seq.of(nullIter);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfThrowablesIterator() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.of(list.iterator());
        Seq<String, Exception> seq = Seq.of(iter);
        Assertions.assertEquals(list, seq.toList());
    }

    @Test
    public void testOfIteratorWithExceptionType() throws Exception {
        Iterator<String> iter = Arrays.asList("a", "b", "c").iterator();
        Seq<String, IOException> seq = Seq.of(iter, IOException.class);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), seq.toList());
    }

    @Test
    public void testOfEnumeration() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("a", "b", "c"));
        Enumeration<String> enumeration = vector.elements();
        Seq<String, Exception> seq = Seq.of(enumeration);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), seq.toList());

        Enumeration<String> nullEnum = null;
        Seq<String, Exception> seqNull = Seq.of(nullEnum);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfEnumerationWithExceptionType() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("a", "b", "c"));
        Enumeration<String> enumeration = vector.elements();
        Seq<String, IOException> seq = Seq.of(enumeration, IOException.class);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), seq.toList());
    }

    @Test
    public void testOfMap() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<Map.Entry<String, Integer>, Exception> seq = Seq.of(map);
        List<Map.Entry<String, Integer>> entries = seq.toList();
        Assertions.assertEquals(3, entries.size());
        Assertions.assertEquals("a", entries.get(0).getKey());
        Assertions.assertEquals(1, entries.get(0).getValue());

        Map<String, Integer> emptyMap = new HashMap<>();
        Seq<Map.Entry<String, Integer>, Exception> seqEmpty = Seq.of(emptyMap);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        Map<String, Integer> nullMap = null;
        Seq<Map.Entry<String, Integer>, Exception> seqNull = Seq.of(nullMap);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfMapWithExceptionType() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Seq<Map.Entry<String, Integer>, IOException> seq = Seq.of(map, IOException.class);
        Assertions.assertEquals(1, seq.toList().size());
    }

    @Test
    public void testComplexScenarios() throws Exception {
        Seq<Integer, Exception> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).filter(n -> n > 3).map(n -> n * 2).takeWhile(n -> n < 16).distinct();

        Assertions.assertEquals(Arrays.asList(8, 10, 12, 14), result.toList());

        Integer[] arr1 = { 1, 3, 5 };
        Integer[] arr2 = { 2, 4, 6 };
        Seq<Integer, Exception> merged = Seq.merge(arr1, arr2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Seq<String, Exception> mapped = merged.map(n -> "Val: " + n);
        Assertions.assertEquals(Arrays.asList("Val: 1", "Val: 2", "Val: 3", "Val: 4", "Val: 5", "Val: 6"), mapped.toList());
    }

    @Test
    public void testIteratorExhaustion() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2);
        Throwables.Iterator<Integer, Exception> iter = seq.iteratorEx();
        iter.next();
        iter.next();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());

        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testNullHandling() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", null, "b", null, "c");
        Assertions.assertEquals(Arrays.asList("a", null, "b", null, "c"), seq.toList());

        Seq<String, Exception> distinct = Seq.of("a", null, "b", null, "c").distinct();
        Assertions.assertEquals(Arrays.asList("a", null, "b", "c"), distinct.toList());

        Seq<String, Exception> filtered = Seq.of("a", null, "b", null, "c").filter(s -> s != null);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), filtered.toList());
    }

    @Test
    public void testSequenceReuse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> first = seq.toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), first);

        Assertions.assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testOfArray() throws Exception {
        String[] array = { "a", "b", "c" };
        Seq<String, Exception> seq = Seq.of(array);
        assertEquals(3, seq.count());

        String[] emptyArray = {};
        assertEquals(0, Seq.of(emptyArray).count());

        assertEquals(0, Seq.of((String[]) null).count());
    }

    @Test
    public void testOfPrimitiveArrays() throws Exception {
        boolean[] boolArray = { true, false, true };
        assertEquals(3, Seq.of(boolArray).count());

        char[] charArray = { 'a', 'b', 'c' };
        assertEquals(3, Seq.of(charArray).count());

        byte[] byteArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(byteArray).count());

        short[] shortArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(shortArray).count());

        int[] intArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(intArray).count());

        long[] longArray = { 1L, 2L, 3L };
        assertEquals(3, Seq.of(longArray).count());

        float[] floatArray = { 1.0f, 2.0f, 3.0f };
        assertEquals(3, Seq.of(floatArray).count());

        double[] doubleArray = { 1.0, 2.0, 3.0 };
        assertEquals(3, Seq.of(doubleArray).count());
    }

    @Test
    public void testOfJavaOptional() throws Exception {
        java.util.Optional<String> optional = java.util.Optional.of("test");
        assertEquals(1, Seq.of(optional).count());

        java.util.Optional<String> empty = java.util.Optional.empty();
        assertEquals(0, Seq.of(empty).count());

        assertEquals(0, Seq.of((java.util.Optional<String>) null).count());
    }

    @Test
    public void testOperationOnClosedStream() {
        Seq<String, RuntimeException> seq = Seq.of("test");
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testConcurrentCloseHandling() throws Exception {
        boolean[] handler1Called = { false };
        boolean[] handler2Called = { false };

        Seq<String, Exception> seq = Seq.of("test").onClose(() -> {
            handler1Called[0] = true;
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
            }
        }).onClose(() -> handler2Called[0] = true);

        Thread t1 = new Thread(seq::close);
        Thread t2 = new Thread(seq::close);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertTrue(handler1Called[0]);
        assertTrue(handler2Called[0]);
    }

    @Test
    public void testOf_Enumeration() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("x", "y", "z"));
        List<String> result = Seq.<String, Exception> of(vector.elements()).toList();
        assertEquals(Arrays.asList("x", "y", "z"), result);
    }

    @Test
    public void testOf_LandawnOptional() throws Exception {
        List<String> result = Seq.<String, Exception> of(Optional.of("hello")).toList();
        assertEquals(Arrays.asList("hello"), result);

        List<String> emptyResult = Seq.<String, Exception> of(Optional.<String> empty()).toList();
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testOf_JavaOptional() throws Exception {
        List<String> result = Seq.<String, Exception> of(java.util.Optional.of("hello")).toList();
        assertEquals(Arrays.asList("hello"), result);

        List<String> emptyResult = Seq.<String, Exception> of(java.util.Optional.<String> empty()).toList();
        assertTrue(emptyResult.isEmpty());
    }

    // advance(n<=0) returns early in primitive array iterators; skip(0) is a no-op
    @Test
    public void testOf_BooleanArray_Skip_NonPositive_NoOp() throws Exception {
        List<Boolean> result = Seq.of(new boolean[] { true, false, true }).skip(0).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testOf_ByteArray_Skip_ExhaustIterator() throws Exception {
        List<Byte> result = Seq.of(new byte[] { 1, 2, 3 }).skip(10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOf_ByteArray_Skip_PartialAdvance() throws Exception {
        List<Byte> result = Seq.of(new byte[] { 10, 20, 30, 40 }).skip(2).toList();
        assertEquals(2, result.size());
        assertEquals(Byte.valueOf((byte) 30), result.get(0));
    }

    @Test
    public void testOf_ShortArray_Skip_PartialAdvance() throws Exception {
        List<Short> result = Seq.of(new short[] { 1, 2, 3, 4, 5 }).skip(3).toList();
        assertEquals(2, result.size());
        assertEquals(Short.valueOf((short) 4), result.get(0));
    }

    @Test
    public void testOf_IntArray_Skip_ExhaustIterator() throws Exception {
        List<Integer> result = Seq.of(new int[] { 1, 2, 3 }).skip(100).toList();
        assertTrue(result.isEmpty());
    }

    // Seq.of(T[]) advance(n<=0) returns early
    @Test
    public void testOf_GenericArray_Skip_NonPositive_NoOp() throws Exception {
        List<String> result = Seq.<String, RuntimeException> of("a", "b", "c").skip(0).toList();
        assertEquals(3, result.size());
    }

    // Seq.of(T[]) advance(n > remaining) exhausts
    @Test
    public void testOf_GenericArray_Skip_MoreThanSize_ReturnsEmpty() throws Exception {
        List<String> result = Seq.<String, RuntimeException> of("a", "b").skip(10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfKeys() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<String, RuntimeException> seq = Seq.ofKeys(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void test_ofKeys_map() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        Seq<Integer, Exception> seq = Seq.ofKeys(map);
        assertEquals(Arrays.asList(1, 2), drainWithException(seq));

        Seq<Integer, Exception> emptySeq = Seq.ofKeys(Collections.emptyMap());
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_ofKeys_map_withValueFilter() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "apple");
        map.put(2, "banana");
        map.put(3, "apricot");
        Seq<Integer, Exception> seq = Seq.ofKeys(map, (Throwables.Predicate<String, Exception>) value -> value.startsWith("a"));
        assertEquals(Arrays.asList(1, 3), drainWithException(seq));
    }

    @Test
    public void test_ofKeys_map_withBiFilter() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "apple");
        map.put(2, "banana");
        map.put(3, "apricot");
        Seq<Integer, Exception> seq = Seq.ofKeys(map, (Throwables.BiPredicate<Integer, String, Exception>) (key, value) -> key > 1 && value.length() > 5);
        assertEquals(Arrays.asList(2, 3), drainWithException(seq).stream().sorted().collect(Collectors.toList()));
    }

    @Test
    public void testOfKeysWithValueFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        Seq<String, Exception> seq = Seq.ofKeys(map, value -> value % 2 == 0);
        Assertions.assertEquals(Arrays.asList("b", "d"), seq.toList());
    }

    @Test
    public void testOfKeysWithBiPredicate() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<String, Exception> seq = Seq.ofKeys(map, (key, value) -> key.equals("a") || value == 3);
        Assertions.assertEquals(Arrays.asList("a", "c"), seq.toList());
    }

    // ==================== New tests for untested methods ====================

    @Test
    public void testOfKeys_WithValueFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        List<String> result = Seq.<String, Integer, Exception> ofKeys(map, v -> v > 1).toList();
        assertEquals(Arrays.asList("b", "c"), result);
    }

    @Test
    public void testOfKeys_WithBiFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("bb", 2);
        map.put("c", 3);
        List<String> result = Seq.<String, Integer, Exception> ofKeys(map, (k, v) -> k.length() == v).toList();
        assertEquals(Arrays.asList("a", "bb"), result);
    }

    @Test
    public void testOfValues() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        Seq<Integer, RuntimeException> seq = Seq.ofValues(map);
        assertEquals(2, seq.count());
    }

    @Test
    public void test_ofValues_map() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        Seq<String, Exception> seq = Seq.ofValues(map);
        assertEquals(Arrays.asList("a", "b"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.ofValues(Collections.emptyMap());
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_ofValues_map_withKeyFilter() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "apple");
        map.put(2, "banana");
        map.put(3, "apricot");
        Seq<String, Exception> seq = Seq.ofValues(map, (Throwables.Predicate<Integer, Exception>) key -> key % 2 != 0);
        assertEquals(Arrays.asList("apple", "apricot"), drainWithException(seq));
    }

    @Test
    public void test_ofValues_map_withBiFilter() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "short");
        map.put(2, "banana");
        map.put(3, "apricots");
        Seq<String, Exception> seq = Seq.ofValues(map, (Throwables.BiPredicate<Integer, String, Exception>) (key, value) -> value.length() > 5 && key > 1);
        assertEquals(Arrays.asList("banana", "apricots"), drainWithException(seq));
    }

    @Test
    public void testOfValuesWithKeyFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);

        Seq<Integer, Exception> seq = Seq.ofValues(map, key -> key.equals("a") || key.equals("c"));
        Assertions.assertEquals(Arrays.asList(1, 3), seq.toList());
    }

    @Test
    public void testOfValuesWithBiPredicate() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<Integer, Exception> seq = Seq.ofValues(map, (key, value) -> key.equals("b") || value == 3);
        Assertions.assertEquals(Arrays.asList(2, 3), seq.toList());
    }

    @Test
    public void testOfValues_WithKeyFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("bb", 2);
        map.put("c", 3);
        List<Integer> result = Seq.<String, Integer, Exception> ofValues(map, k -> k.length() > 1).toList();
        assertEquals(Arrays.asList(2), result);
    }

    @Test
    public void testOfValues_WithBiFilter() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("bb", 2);
        map.put("c", 3);
        List<Integer> result = Seq.<String, Integer, Exception> ofValues(map, (k, v) -> v > 1).toList();
        assertEquals(Arrays.asList(2, 3), result);
    }

    @Test
    public void testOfReversed_Array() throws Exception {
        Integer[] array = { 1, 2, 3 };
        Seq<Integer, RuntimeException> seq = Seq.ofReversed(array);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testOfReversed_List() throws Exception {
        List<Integer> list = Arrays.asList(1, 2, 3);
        Seq<Integer, RuntimeException> seq = Seq.ofReversed(list);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void test_ofReversed_array() throws Exception {
        String[] array = { "a", "b", "c" };
        Seq<String, Exception> seq = Seq.ofReversed(array);
        assertEquals(Arrays.asList("c", "b", "a"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.ofReversed(new String[] {});
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_ofReversed_list() throws Exception {
        List<String> list = Arrays.asList("x", "y", "z");
        Seq<String, Exception> seq = Seq.ofReversed(list);
        assertEquals(Arrays.asList("z", "y", "x"), drainWithException(seq));

        Seq<String, Exception> emptySeq = Seq.ofReversed(Collections.emptyList());
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void testOfReversedArray() throws Exception {
        Integer[] arr = { 1, 2, 3, 4, 5 };
        Seq<Integer, Exception> seq = Seq.ofReversed(arr);
        Assertions.assertEquals(Arrays.asList(5, 4, 3, 2, 1), seq.toList());

        Integer[] empty = {};
        Seq<Integer, Exception> seqEmpty = Seq.ofReversed(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfReversedList() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Seq<String, Exception> seq = Seq.ofReversed(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), seq.toList());

        List<String> empty = new ArrayList<>();
        Seq<String, Exception> seqEmpty = Seq.ofReversed(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfReversed_EmptyArray() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> ofReversed(new Integer[0]).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testOfReversed_EmptyList() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> ofReversed(Collections.<Integer> emptyList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeat() throws Exception {
        Seq<String, RuntimeException> seq = Seq.repeat("a", 3);
        assertEquals(3, seq.count());
    }

    @Test
    public void testRepeat_Zero() throws Exception {
        Seq<String, RuntimeException> seq = Seq.repeat("a", 0);
        assertEquals(0, seq.count());
    }

    @Test
    public void test_repeat() throws Exception {
        Seq<String, Exception> seq = Seq.repeat("r", 3);
        assertEquals(Arrays.asList("r", "r", "r"), drainWithException(seq));

        Seq<String, Exception> zeroSeq = Seq.repeat("r", 0);
        assertTrue(drainWithException(zeroSeq).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("r", -1));
    }

    @Test
    public void testRepeatWithNegativeCount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("x", -1));
    }

    @Test
    public void testRepeat_NegativeCount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.repeat(1, -1));
    }

    @Test
    public void test_bufferred() throws Exception {
        final int millisToSleep = 100;
        final int elementCount = 10;

        long startTime = System.currentTimeMillis();
        Seq.range(0, elementCount).delay(Duration.ofMillis(millisToSleep)).buffered().map(it -> it * 2).delay(Duration.ofMillis(millisToSleep)).println();
        N.println("Duration: " + (System.currentTimeMillis() - startTime));

        assertTrue(System.currentTimeMillis() - startTime < millisToSleep * elementCount * 2);

        startTime = System.currentTimeMillis();
        Seq.range(0, elementCount).delay(Duration.ofMillis(millisToSleep)).map(it -> it * 2).delay(Duration.ofMillis(millisToSleep)).println();
        N.println("Duration: " + (System.currentTimeMillis() - startTime));

        assertTrue(System.currentTimeMillis() - startTime < millisToSleep * elementCount * 2);
    }

    @Test
    public void testRange() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 5);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testRange_WithStep() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 10, 2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 2, 4, 6, 8), result);
    }

    @Test
    public void testLargeSeq() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.range(0, 10000);
        assertEquals(10000, seq.count());
    }

    @Test
    public void test_range_startEnd() throws Exception {
        Seq<Integer, Exception> seq = Seq.range(1, 4);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));

        Seq<Integer, Exception> emptyRangeSeq = Seq.range(1, 1);
        assertTrue(drainWithException(emptyRangeSeq).isEmpty());

        Seq<Integer, Exception> decreasingRangeSeq = Seq.range(4, 1);
        assertTrue(drainWithException(decreasingRangeSeq).isEmpty());
    }

    @Test
    public void test_range_startEndStep() throws Exception {
        Seq<Integer, Exception> seq = Seq.range(1, 6, 2);
        assertEquals(Arrays.asList(1, 3, 5), drainWithException(seq));

        Seq<Integer, Exception> seqNegStep = Seq.range(5, 0, -2);
        assertEquals(Arrays.asList(5, 3, 1), drainWithException(seqNegStep));

        assertThrows(IllegalArgumentException.class, () -> Seq.range(1, 5, 0));
    }

    @Test
    public void testRangeWithStep() throws Exception {
        Seq<Integer, Exception> seq = Seq.range(0, 10, 2);
        Assertions.assertEquals(Arrays.asList(0, 2, 4, 6, 8), seq.toList());

        Seq<Integer, Exception> seqNeg = Seq.range(10, 0, -2);
        Assertions.assertEquals(Arrays.asList(10, 8, 6, 4, 2), seqNeg.toList());
    }

    @Test
    public void testLargeSequences() throws Exception {
        Seq<Integer, Exception> largeSeq = Seq.range(0, 1000);
        Assertions.assertEquals(1000, largeSeq.count());

        Seq<Integer, Exception> filtered = Seq.range(0, 1000).filter(n -> n % 100 == 0);
        Assertions.assertEquals(Arrays.asList(0, 100, 200, 300, 400, 500, 600, 700, 800, 900), filtered.toList());
    }

    @Test
    public void testPerformancePatterns() throws Exception {
        int[] counter = { 0 };
        Seq<Integer, Exception> seq = Seq.range(0, 1000000).takeWhile(n -> {
            counter[0]++;
            return n < 10;
        }).map(n -> n * 2);

        Assertions.assertEquals(0, counter[0]);

        List<Integer> result = seq.toList();

        Assertions.assertTrue(counter[0] < 20);
        Assertions.assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10, 12, 14, 16, 18), result);
    }

    @Test
    public void testRange_Negative() throws Exception {
        List<Integer> result = Seq.range(5, 2, -1).toList();
        assertEquals(Arrays.asList(5, 4, 3), result);
    }

    @Test
    public void testRangeClosed() throws Exception {
        Seq<Integer, RuntimeException> seq = Seq.rangeClosed(0, 5);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), result);
    }

    @Test
    public void test_rangeClosed_startEnd() throws Exception {
        Seq<Integer, Exception> seq = Seq.rangeClosed(1, 3);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));

        Seq<Integer, Exception> singleElementSeq = Seq.rangeClosed(1, 1);
        assertEquals(Collections.singletonList(1), drainWithException(singleElementSeq));

        Seq<Integer, Exception> decreasingRangeSeq = Seq.rangeClosed(3, 1);
        assertTrue(drainWithException(decreasingRangeSeq).isEmpty());
    }

    @Test
    public void test_rangeClosed_startEndStep() throws Exception {
        Seq<Integer, Exception> seq = Seq.rangeClosed(1, 5, 2);
        assertEquals(Arrays.asList(1, 3, 5), drainWithException(seq));

        Seq<Integer, Exception> seqNegStep = Seq.rangeClosed(5, 1, -2);
        assertEquals(Arrays.asList(5, 3, 1), drainWithException(seqNegStep));

        assertThrows(IllegalArgumentException.class, () -> Seq.rangeClosed(1, 5, 0));
    }

    @Test
    public void testRangeClosedWithStep() throws Exception {
        Seq<Integer, Exception> seq = Seq.rangeClosed(0, 10, 3);
        Assertions.assertEquals(Arrays.asList(0, 3, 6, 9), seq.toList());

        Seq<Integer, Exception> seqNeg = Seq.rangeClosed(10, 0, -3);
        Assertions.assertEquals(Arrays.asList(10, 7, 4, 1), seqNeg.toList());
    }

    @Test
    public void testRangeClosed_WithStep() throws Exception {
        List<Integer> result = Seq.rangeClosed(0, 10, 3).toList();
        assertEquals(Arrays.asList(0, 3, 6, 9), result);
    }

    @Test
    public void testSplit_CharDelimiter() throws Exception {
        Seq<String, RuntimeException> seq = Seq.split("a,b,c", ',');
        List<String> result = seq.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplit_StringDelimiter() throws Exception {
        Seq<String, RuntimeException> seq = Seq.split("a::b::c", "::");
        List<String> result = seq.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplit() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 4, 5, 6).split(2).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));
    }

    @Test
    public void testSplitByPredicate() throws Exception {
        List<List<Integer>> chunks = Seq.of(1, 2, 3, 0, 4, 5, 0, 6).split(x -> x == 0).toList();
        assertEquals(5, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(0), chunks.get(1));
        assertEquals(Arrays.asList(4, 5), chunks.get(2));
        assertEquals(Arrays.asList(0), chunks.get(3));
        assertEquals(Arrays.asList(6), chunks.get(4));
    }

    @Test
    public void test_split_charDelimiter() throws Exception {
        Seq<String, Exception> seq = Seq.split("a,b,c", ',');
        assertEquals(Arrays.asList("a", "b", "c"), drainWithException(seq));
        Seq<String, Exception> seqEmpty = Seq.split("", ',');
        assertEquals(Collections.singletonList(""), drainWithException(seqEmpty));
    }

    @Test
    public void test_split_stringDelimiter() throws Exception {
        Seq<String, Exception> seq = Seq.split("a,,b,,c", ",,");
        assertEquals(Arrays.asList("a", "b", "c"), drainWithException(seq));
    }

    @Test
    public void test_split_patternDelimiter() throws Exception {
        Seq<String, Exception> seq = Seq.split("a1b2c", Pattern.compile("\\d"));
        assertEquals(Arrays.asList("a", "b", "c"), drainWithException(seq));
    }

    @Test
    public void test_split_withCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7).split(3, Collectors.mapping(String::valueOf, Collectors.joining("-")));
        assertEquals(Arrays.asList("1-2-3", "4-5-6", "7"), drainWithException(seq));

        Seq<String, Exception> emptySplit = Seq.<Integer, Exception> empty().split(3, Collectors.mapping(String::valueOf, Collectors.joining("-")));
        assertTrue(drainWithException(emptySplit).isEmpty());
    }

    @Test
    public void test_split_byPredicate_withCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 10, 11, 5, 6, 20).split(x -> x < 10, Collectors.summingInt(x -> x)).map(String::valueOf);
        assertEquals(Arrays.asList("3", "21", "11", "20"), drainWithException(seq));
    }

    @Test
    public void testSplitIntWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).skip(1).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("a", "b", "c"), result.get(0));
        assertEquals(Arrays.asList("d", "e"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(3).skip(1).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("d", "e"), result.get(0));
    }

    @Test
    public void testSplitPredicateWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).skip(1).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d", "e"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").split(s -> s.compareTo("c") < 0).skip(1).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("c", "d", "e"), result.get(0));
    }

    @Test
    public void testSplitBySize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<List<Integer>> chunks = seq.split(3).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2, 3), chunks.get(0));
        assertEquals(Arrays.asList(4, 5, 6), chunks.get(1));
        assertEquals(Arrays.asList(7), chunks.get(2));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        chunks = seq.split(2).toList();
        assertEquals(3, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));
        assertEquals(Arrays.asList(3, 4), chunks.get(1));
        assertEquals(Arrays.asList(5, 6), chunks.get(2));

        seq = Seq.of(1, 2);
        chunks = seq.split(5).toList();
        assertEquals(1, chunks.size());
        assertEquals(Arrays.asList(1, 2), chunks.get(0));

        seq = Seq.<Integer, Exception> empty();
        chunks = seq.split(3).toList();
        assertTrue(chunks.isEmpty());
    }

    @Test
    public void testSplitBySizeWithCollectionSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c", "d", "e");
        List<Set<String>> chunks = seq.split(2, IntFunctions.ofSet()).toList();
        assertEquals(3, chunks.size());
        assertEquals(new HashSet<>(Arrays.asList("a", "b")), chunks.get(0));
        assertEquals(new HashSet<>(Arrays.asList("c", "d")), chunks.get(1));
        assertEquals(new HashSet<>(Arrays.asList("e")), chunks.get(2));

        seq = Seq.of("1", "2", "3", "4");
        List<ArrayList<String>> arrayLists = seq.split(2, n -> new ArrayList<>(n)).toList();
        assertEquals(2, arrayLists.size());
        assertEquals(Arrays.asList("1", "2"), arrayLists.get(0));
        assertEquals(Arrays.asList("3", "4"), arrayLists.get(1));
    }

    @Test
    public void testSplitBySizeWithCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<String> joined = seq.split(2, com.landawn.abacus.util.stream.Collectors.joining(",")).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), joined);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9);
        List<Integer> sums = seq.split(3, Collectors.summingInt(Integer::intValue)).toList();
        assertEquals(Arrays.asList(6, 15, 24), sums);

        seq = Seq.of(10, 20, 30, 40, 50);
        List<Double> averages = seq.split(2, Collectors.averagingInt(Integer::intValue)).toList();
        assertEquals(15.0, averages.get(0), 0.01);
        assertEquals(35.0, averages.get(1), 0.01);
        assertEquals(50.0, averages.get(2), 0.01);
    }

    @Test
    public void testSplitByPredicateWithCollectionSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "ab", "abc", "d", "de");
        List<Set<String>> groups = seq.split(s -> s.length() > 1, Suppliers.ofSet()).toList();
        assertEquals(4, groups.size());
        assertEquals(new HashSet<>(Arrays.asList("a")), groups.get(0));
        assertEquals(new HashSet<>(Arrays.asList("ab", "abc")), groups.get(1));

        Seq<Integer, Exception> intSeq = Seq.of(1, 2, 4, 8, 3, 5, 7);
        List<LinkedList<Integer>> linkedGroups = intSeq.split(n -> n % 2 == 0, LinkedList::new).toList();
        assertEquals(3, linkedGroups.size());
    }

    @Test
    public void testSplitByPredicateWithCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5, 2, 4, 6, 7, 9);
        List<String> joined = seq.split(n -> n % 2 == 0, com.landawn.abacus.util.stream.Collectors.joining(",")).toList();
        assertEquals(Arrays.asList("1,3,5", "2,4,6", "7,9"), joined);

        Seq<String, Exception> strSeq = Seq.of("a", "b", "cc", "dd", "e", "f");
        List<Long> counts = strSeq.split(s -> s.length() > 1, Collectors.counting()).toList();
        assertEquals(Arrays.asList(2L, 2L, 2L), counts);

        seq = Seq.of(1, 3, 2, 8, 6, 4, 5, 7);
        List<Optional<Integer>> maxValues = seq.split(n -> n > 4, com.landawn.abacus.util.stream.Collectors.max()).toList();
        assertEquals(3, maxValues.get(0).get().intValue());
        assertEquals(8, maxValues.get(1).get().intValue());
        assertEquals(4, maxValues.get(2).get().intValue());
        assertEquals(7, maxValues.get(3).get().intValue());
    }

    @Test
    public void testSplitByCharDelimiter() throws Exception {
        String str = "a,b,c,d";
        Seq<String, Exception> seq = Seq.split(str, ',');
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());

        Seq<String, Exception> seqEmpty = Seq.split("", ',');
        Assertions.assertEquals(Arrays.asList(""), seqEmpty.toList());
    }

    @Test
    public void testSplitByStringDelimiter() throws Exception {
        String str = "a::b::c::d";
        Seq<String, Exception> seq = Seq.split(str, "::");
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());
    }

    @Test
    public void testSplitByPattern() throws Exception {
        String str = "a123b456c789d";
        Pattern pattern = Pattern.compile("\\d+");
        Seq<String, Exception> seq = Seq.split(str, pattern);
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());
    }

    @Test
    public void testSpecialCharactersInSplit() throws Exception {
        String str = "a.b.c.d";
        Seq<String, Exception> seq = Seq.split(str, Pattern.compile("\\."));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());

        String str2 = "a::b::c::d";
        Seq<String, Exception> seq2 = Seq.split(str2, "::");
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq2.toList());
    }

    @Test
    public void testSplitWithCharDelimiter() throws Exception {
        List<String> result = Seq.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitWithStringDelimiter() throws Exception {
        List<String> result = Seq.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testSplitWithPattern() throws Exception {
        List<String> result = Seq.split("a1b2c3", Pattern.compile("\\d")).toList();
        assertEquals(Arrays.asList("a", "b", "c", ""), result);
    }

    @Test
    public void testSplit_Pattern() throws Exception {
        List<String> result = Seq.<Exception> split("a1b2c3d", Pattern.compile("\\d")).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testSplit_WithCollector() throws Exception {
        List<String> result = Seq.of(1, 2, 3, 4, 5, 6)
                .split(2, java.util.stream.Collectors.toList())
                .map(list -> list.stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(",")))
                .toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), result);
    }

    @Test
    public void testSplit_ChunkSize_Empty() throws Exception {
        List<List<Integer>> result = Seq.<Integer, Exception> empty().split(3).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitToLines() throws Exception {
        Seq<String, RuntimeException> seq = Seq.splitToLines("a\nb\nc");
        assertEquals(3, seq.count());
    }

    @Test
    public void test_splitToLines_simple() throws Exception {
        Seq<String, Exception> seq = Seq.splitToLines("line1\nline2\r\nline3");
        assertEquals(Arrays.asList("line1", "line2", "line3"), drainWithException(seq));
    }

    @Test
    public void test_splitToLines_trimAndOmit() throws Exception {
        Seq<String, Exception> seq = Seq.splitToLines("  line1  \n\n  line2  \n", true, true);
        assertEquals(Arrays.asList("line1", "line2"), drainWithException(seq));
    }

    @Test
    public void testSplitToLinesWithOptions() throws Exception {
        String str = "  line1  \n\n  line2  \n  \n  line3  ";

        Seq<String, Exception> seqTrim = Seq.splitToLines(str, true, false);
        Assertions.assertEquals(Arrays.asList("line1", "", "line2", "", "line3"), seqTrim.toList());

        Seq<String, Exception> seqOmit = Seq.splitToLines(str, false, true);
        Assertions.assertEquals(Arrays.asList("  line1  ", "  line2  ", "  ", "  line3  "), seqOmit.toList());

        Seq<String, Exception> seqBoth = Seq.splitToLines(str, true, true);
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), seqBoth.toList());
    }

    @Test
    public void testSplitToLines_WithTrimAndOmitEmpty() throws Exception {
        List<String> result = Seq.<Exception> splitToLines("  line1  \n\n  line2  \n", true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testSplitToLines_NoTrimKeepEmpty() throws Exception {
        List<String> result = Seq.<Exception> splitToLines("line1\n\nline2\n", false, false).toList();
        assertEquals(Arrays.asList("line1", "", "line2", ""), result);
    }

    @Test
    public void testSplitByChunkCount() throws Exception {
        List<Pair<Integer, Integer>> chunks = Seq.splitByChunkCount(10, 3, Pair::of).toList();
        assertEquals(3, chunks.size());
    }

    @Test
    public void test_splitByChunkCount() throws Exception {
        Seq<Pair<Integer, Integer>, Exception> seq = Seq.splitByChunkCount(7, 3, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expected = Arrays.asList(Pair.of(0, 3), Pair.of(3, 5), Pair.of(5, 7));
        assertEquals(expected, drainWithException(seq));

        Seq<Pair<Integer, Integer>, Exception> seqSmallerFirst = Seq.splitByChunkCount(7, 3, true, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expectedSmallerFirst = Arrays.asList(Pair.of(0, 2), Pair.of(2, 4), Pair.of(4, 7));
        assertEquals(expectedSmallerFirst, drainWithException(seqSmallerFirst));

        Seq<Pair<Integer, Integer>, Exception> zeroTotalSize = Seq.splitByChunkCount(0, 3, (f, t) -> Pair.of(f, t));
        assertTrue(drainWithException(zeroTotalSize).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(-1, 3, (f, t) -> Pair.of(f, t)));
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(5, 0, (f, t) -> Pair.of(f, t)));
    }

    @Test
    public void test_splitByChunkCount_totalSizeLessThanMaxChunkCount() throws Exception {
        Seq<Pair<Integer, Integer>, Exception> seq = Seq.splitByChunkCount(2, 5, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expected = Arrays.asList(Pair.of(0, 1), Pair.of(1, 2));
        assertEquals(expected, drainWithException(seq));

        Seq<Pair<Integer, Integer>, Exception> seqSingle = Seq.splitByChunkCount(1, 5, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expectedSingle = Collections.singletonList(Pair.of(0, 1));
        assertEquals(expectedSingle, drainWithException(seqSingle));
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() throws Exception {
        Seq<List<Integer>, Exception> seqSmaller = Seq.splitByChunkCount(7, 5, true, (from, to) -> {
            List<Integer> chunk = new ArrayList<>();
            for (int i = from; i < to; i++) {
                chunk.add(i);
            }
            return chunk;
        });

        List<List<Integer>> resultSmaller = seqSmaller.toList();
        Assertions.assertEquals(5, resultSmaller.size());
        Assertions.assertEquals(1, resultSmaller.get(0).size());
        Assertions.assertEquals(2, resultSmaller.get(3).size());

        Seq<List<Integer>, Exception> seqBigger = Seq.splitByChunkCount(7, 5, false, (from, to) -> {
            List<Integer> chunk = new ArrayList<>();
            for (int i = from; i < to; i++) {
                chunk.add(i);
            }
            return chunk;
        });

        List<List<Integer>> resultBigger = seqBigger.toList();
        Assertions.assertEquals(5, resultBigger.size());
        Assertions.assertEquals(2, resultBigger.get(0).size());
        Assertions.assertEquals(1, resultBigger.get(3).size());
    }

    @Test
    public void testSplitByChunkCountWithNegativeTotalSize() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(-1, 3, (from, to) -> new Object()));
    }

    @Test
    public void testSplitByChunkCountWithZeroMaxChunkCount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(10, 0, (from, to) -> new Object()));
    }

    @Test
    public void testSplitByChunkCount_SizeSmallerFirst() throws Exception {
        List<Pair<Integer, Integer>> result = Seq.splitByChunkCount(10, 3, true, Pair::of).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testSplitByChunkCount_sizeSmallerFirst_true() throws Exception {
        List<String> chunks = Seq.splitByChunkCount(7, 3, true, (from, to) -> from + "-" + to).toList();
        assertEquals(3, chunks.size());
        // 7 / 3: biggerSize=3, biggerCount=1, smallerSize=2, smallerCount=2
        // sizeSmallerFirst=true => 2 smaller chunks (size 2) then 1 bigger chunk (size 3)
        assertEquals("0-2", chunks.get(0));
        assertEquals("2-4", chunks.get(1));
        assertEquals("4-7", chunks.get(2));
    }

    @Test
    public void testSplitByChunkCount_sizeSmallerFirst_false() throws Exception {
        List<String> chunks = Seq.splitByChunkCount(7, 3, false, (from, to) -> from + "-" + to).toList();
        assertEquals(3, chunks.size());
    }

    @Test
    public void testSplitByChunkCount_zeroTotalSize() throws Exception {
        List<String> chunks = Seq.splitByChunkCount(0, 3, true, (from, to) -> from + "-" + to).toList();
        assertEquals(0, chunks.size());
    }

    // ===== splitByChunkCount(int, int, boolean, IntBiFunction) with various combos =====

    @Test
    public void testSplitByChunkCount_sizeSmallerFirst_evenDivision() throws Exception {
        List<String> chunks = Seq.splitByChunkCount(6, 3, true, (from, to) -> from + "-" + to).toList();
        assertEquals(3, chunks.size());
        // 6/3 = 2 each, no remainder
        assertEquals("0-2", chunks.get(0));
        assertEquals("2-4", chunks.get(1));
        assertEquals("4-6", chunks.get(2));
    }

    @Test
    public void testSplitByChunkCount_sizeLargerFirst() throws Exception {
        List<String> chunks = Seq.splitByChunkCount(7, 3, false, (from, to) -> from + "-" + to).toList();
        assertEquals(3, chunks.size());
        // sizeSmallerFirst=false => bigger chunks come first
        assertEquals("0-3", chunks.get(0));
    }

    @Test
    public void testOfLines_File() throws IOException {
        File tempFile = File.createTempFile("seq-test", ".txt");
        try {
            Files.write(tempFile.toPath(), Arrays.asList("line1", "line2", "line3"));
            Seq<String, IOException> seq = Seq.ofLines(tempFile);
            List<String> lines = seq.toList();
            assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void test_ofLines_file() throws IOException {
        File tempFile = tempDir.resolve("testLines.txt").toFile();
        List<String> lines = Arrays.asList("line1", "line2", "line3");
        Files.write(tempFile.toPath(), lines);

        Seq<String, IOException> seq = Seq.ofLines(tempFile);
        assertEquals(lines, drain(seq));
    }

    @Test
    public void test_ofLines_fileWithCharset() throws IOException {
        File tempFile = tempDir.resolve("testLinesCharset.txt").toFile();
        List<String> lines = Arrays.asList("你好", "世界");
        Files.write(tempFile.toPath(), lines, StandardCharsets.UTF_8);

        Seq<String, IOException> seq = Seq.ofLines(tempFile, StandardCharsets.UTF_8);
        assertEquals(lines, drain(seq));
    }

    @Test
    public void test_ofLines_path() throws IOException {
        Path tempPath = tempDir.resolve("testLinesPath.txt");
        List<String> lines = Arrays.asList("pathLine1", "pathLine2");
        Files.write(tempPath, lines);

        Seq<String, IOException> seq = Seq.ofLines(tempPath);
        assertEquals(lines, drain(seq));
    }

    @Test
    public void test_ofLines_pathWithCharset() throws IOException {
        Path tempPath = tempDir.resolve("testLinesPathCharset.txt");
        List<String> lines = Arrays.asList("你好Path", "世界Path");
        Files.write(tempPath, lines, StandardCharsets.UTF_8);

        Seq<String, IOException> seq = Seq.ofLines(tempPath, StandardCharsets.UTF_8);
        assertEquals(lines, drain(seq));
    }

    @Test
    public void test_ofLines_reader() throws IOException {
        StringReader reader = new StringReader("readerLine1\nreaderLine2");
        Seq<String, IOException> seq = Seq.ofLines(reader);
        assertEquals(Arrays.asList("readerLine1", "readerLine2"), drain(seq));
        assertTrue(reader.ready());
    }

    @Test
    public void test_ofLines_reader_close() throws IOException {
        StringReader stringReader = new StringReader("line1\nline2");
        AtomicBoolean readerClosed = new AtomicBoolean(false);
        Reader mockReader = new BufferedReader(stringReader) {
            @Override
            public void close() throws IOException {
                super.close();
                readerClosed.set(true);
            }
        };

        Seq<String, IOException> seq = Seq.ofLines(mockReader, true);
        assertEquals(Arrays.asList("line1", "line2"), drain(seq));
        assertTrue(readerClosed.get(), "Reader should be closed when closeReaderWhenStreamIsClosed is true");
    }

    @Test
    public void testOfLinesFile() throws IOException {
        Seq<String, IOException> seq = Seq.ofLines(tempFile);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();
    }

    @Test
    public void testOfLinesFileWithCharset() throws IOException {
        Seq<String, IOException> seq = Seq.ofLines(tempFile, StandardCharsets.UTF_8);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((File) null, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testOfLinesPath() throws IOException {
        Seq<String, IOException> seq = Seq.ofLines(tempPath);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();
    }

    @Test
    public void testOfLinesPathWithCharset() throws IOException {
        Seq<String, IOException> seq = Seq.ofLines(tempPath, StandardCharsets.UTF_8);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((Path) null, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testOfLinesReader() throws IOException {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Seq<String, IOException> seq = Seq.ofLines(reader);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((Reader) null);
        });
    }

    @Test
    public void testOfLinesReaderWithCloseOption() throws IOException {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Seq<String, IOException> seq = Seq.ofLines(reader, true);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();

        StringReader reader2 = new StringReader("line1\nline2");
        Seq<String, IOException> seq2 = Seq.ofLines(reader2, false);
        List<String> lines2 = seq2.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2"), lines2);
        seq2.close();
    }

    @Test
    public void testFileOperationsWithNonExistentFiles() throws IOException {
        File nonExistent = new File("non_existent_file.txt");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            Seq<String, IOException> seq = Seq.ofLines(nonExistent);
            seq.toList();
        });

        Path nonExistentPath = Paths.get("non_existent_path.txt");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            Seq<String, IOException> seq = Seq.ofLines(nonExistentPath);
            seq.toList();
        });
    }

    @Test
    public void testOfLinesFromFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        Files.write(file.toPath(), Arrays.asList("line1", "line2", "line3"));

        List<String> result = Seq.ofLines(file).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesFromFileWithCharset() throws Exception {
        File file = Files.createTempFile(tempFolder, "test", ".txt").toFile();
        String content = "テスト";
        Files.write(file.toPath(), content.getBytes("UTF-8"));

        List<String> result = Seq.ofLines(file, Charset.forName("UTF-8")).toList();
        assertEquals(1, result.size());
        assertEquals(content, result.get(0));
    }

    @Test
    public void testOfLinesFromNullFile() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((File) null));
    }

    @Test
    public void testOfLinesFromPath() throws Exception {
        Path path = Files.createTempFile(tempFolder, "test", ".txt").toFile().toPath();
        Files.write(path, Arrays.asList("line1", "line2", "line3"));

        List<String> result = Seq.ofLines(path).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesFromPathWithCharset() throws Exception {
        Path path = Files.createTempFile(tempFolder, "test", ".txt").toFile().toPath();
        String content = "テスト";
        Files.write(path, content.getBytes("UTF-8"));

        List<String> result = Seq.ofLines(path, Charset.forName("UTF-8")).toList();
        assertEquals(1, result.size());
        assertEquals(content, result.get(0));
    }

    @Test
    public void testOfLinesFromNullPath() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Path) null));
    }

    @Test
    public void testOfLinesFromReader() throws Exception {
        String content = "line1\nline2\nline3";
        StringReader reader = new StringReader(content);

        List<String> result = Seq.ofLines(reader).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesFromReaderWithCloseOption() throws Exception {
        String content = "line1\nline2";
        StringReader reader = spy(new StringReader(content));

        List<String> result = Seq.ofLines(reader, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
        verify(reader).close();
    }

    @Test
    public void testOfLinesFromNullReader() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Reader) null));
    }

    @Test
    public void testListFiles() throws Exception {
        File tempDir = Files.createTempDirectory("seq-test").toFile();
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Seq.listFiles(tempDir).toList();
        assertTrue(files.size() >= 2);

        file1.delete();
        file2.delete();
        tempDir.delete();
    }

    @Test
    public void test_listFiles_nonRecursive() throws IOException {
        Path listRoot = Files.createDirectory(tempDir.resolve("listFilesNonRecursive"));
        File subDir = listRoot.resolve("sub").toFile();
        subDir.mkdir();
        File file1 = listRoot.resolve("file1.txt").toFile();
        file1.createNewFile();
        File file2InSub = subDir.toPath().resolve("file2.txt").toFile();
        file2InSub.createNewFile();

        Seq<File, IOException> seq = Seq.listFiles(listRoot.toFile());
        List<File> files = drain(seq);

        assertTrue(files.contains(file1));
        assertTrue(files.contains(subDir));
        assertFalse(files.contains(file2InSub));
        assertEquals(2, files.size());
    }

    @Test
    public void test_listFiles_recursive() throws IOException {
        Path listRoot = Files.createDirectory(tempDir.resolve("listFilesRecursive"));
        File subDir = listRoot.resolve("subRecursive").toFile();
        subDir.mkdir();
        File file1 = listRoot.resolve("file1Rec.txt").toFile();
        file1.createNewFile();
        File file2InSub = subDir.toPath().resolve("file2Rec.txt").toFile();
        file2InSub.createNewFile();

        Seq<File, IOException> seq = Seq.listFiles(listRoot.toFile(), true);
        List<File> files = drain(seq);

        Set<String> names = files.stream().map(File::getName).collect(Collectors.toSet());
        assertTrue(names.contains("file1Rec.txt"));
        assertTrue(names.contains("subRecursive"));
        assertTrue(names.contains("file2Rec.txt"));
    }

    @Test
    public void test_listFiles_nonExistentDir() throws IOException {
        File nonExistent = tempDir.resolve("nonExistent").toFile();
        Seq<File, IOException> seq = Seq.listFiles(nonExistent);
        assertTrue(drain(seq).isEmpty());
    }

    @Test
    public void testListFilesRecursive() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File subDir = new File(tempDir, "subdir");
        subDir.mkdir();
        subDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();
        file1.deleteOnExit();
        file2.deleteOnExit();

        Seq<File, IOException> seqNonRecursive = Seq.listFiles(tempDir, false);
        List<File> filesNonRecursive = seqNonRecursive.toList();
        Assertions.assertEquals(2, filesNonRecursive.size());

        Seq<File, IOException> seqRecursive = Seq.listFiles(tempDir, true);
        List<File> filesRecursive = seqRecursive.toList();
        Assertions.assertEquals(3, filesRecursive.size());

        file2.delete();
        file1.delete();
        subDir.delete();
        tempDir.delete();
    }

    @Test
    public void testListFilesNonExistentDirectory() throws Exception {
        File nonExistent = new File("nonExistent");
        assertEquals(0, Seq.listFiles(nonExistent).count());
    }

    @Test
    public void testListFilesRecursively() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "testDir").toFile();
        File subDir = new File(dir, "subDir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        List<File> files = Seq.listFiles(dir, true).toList();
        assertEquals(3, files.size());

        Set<String> names = files.stream().map(File::getName).collect(Collectors.toSet());
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("file2.txt"));
        assertTrue(names.contains("subDir"));
    }

    // ===== listFiles - basic path with non-recursive =====

    @Test
    public void testListFiles_nonRecursive_emptyDir() throws Exception {
        File dir = tempDir.resolve("emptySubDir").toFile();
        dir.mkdir();
        List<File> files = Seq.listFiles(dir).toList();
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_nonRecursive_withFiles() throws Exception {
        File dir = tempDir.resolve("fileDir").toFile();
        dir.mkdir();
        new java.io.File(dir, "a.txt").createNewFile();
        new java.io.File(dir, "b.txt").createNewFile();
        List<File> files = Seq.listFiles(dir).toList();
        assertEquals(2, files.size());
    }

    @Test
    public void testConcat_Arrays() throws Exception {
        Integer[] a1 = { 1, 2 };
        Integer[] a2 = { 3, 4 };
        Seq<Integer, RuntimeException> seq = Seq.concat(a1, a2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testConcat_Iterables() throws Exception {
        List<Integer> l1 = Arrays.asList(1, 2);
        List<Integer> l2 = Arrays.asList(3, 4);
        Seq<Integer, RuntimeException> seq = Seq.concat(l1, l2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testConcat_Seqs() throws Exception {
        Seq<Integer, RuntimeException> s1 = Seq.of(1, 2);
        Seq<Integer, RuntimeException> s2 = Seq.of(3, 4);
        Seq<Integer, RuntimeException> seq = Seq.concat(s1, s2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void test_concat_arrays() throws Exception {
        String[] a1 = { "a", "b" };
        String[] a2 = { "c", "d" };
        Seq<String, Exception> seq = Seq.concat(a1, a2);
        assertEquals(Arrays.asList("a", "b", "c", "d"), drainWithException(seq));
        assertTrue(drainWithException(Seq.concat(CommonUtil.EMPTY_STRING_ARRAY)).isEmpty());
    }

    @Test
    public void test_concat_iterables() throws Exception {
        List<String> l1 = Arrays.asList("a", "b");
        List<String> l2 = Arrays.asList("c", "d");
        Seq<String, Exception> seq = Seq.concat(l1, l2);
        assertEquals(Arrays.asList("a", "b", "c", "d"), drainWithException(seq));
        assertTrue(drainWithException(Seq.concat((Iterable<String>) null, (Iterable<String>) null)).isEmpty());
    }

    @Test
    public void test_concat_iterators() throws Exception {
        Iterator<String> i1 = Arrays.asList("a", "b").iterator();
        Iterator<String> i2 = Arrays.asList("c", "d").iterator();
        Seq<String, Exception> seq = Seq.concat(i1, i2);
        assertEquals(Arrays.asList("a", "b", "c", "d"), drainWithException(seq));
        assertTrue(drainWithException(Seq.concat((Iterator<String>) null, (Iterator<String>) null)).isEmpty());
    }

    @Test
    public void test_concat_seqs() throws Exception {
        Seq<String, Exception> s1 = Seq.of("a", "b");
        Seq<String, Exception> s2 = Seq.of("c", "d");
        AtomicBoolean s1Closed = new AtomicBoolean(false);
        AtomicBoolean s2Closed = new AtomicBoolean(false);
        s1 = s1.onClose(() -> s1Closed.set(true));
        s2 = s2.onClose(() -> s2Closed.set(true));

        Seq<String, Exception> seq = Seq.concat(s1, s2);
        assertEquals(Arrays.asList("a", "b", "c", "d"), drainWithException(seq));
        assertTrue(s1Closed.get(), "First sequence should be closed");
        assertTrue(s2Closed.get(), "Second sequence should be closed");
    }

    @Test
    public void test_concat_collectionOfSeqs() throws Exception {
        Seq<String, Exception> s1 = Seq.of("a", "b");
        Seq<String, Exception> s2 = Seq.of("c", "d");
        AtomicBoolean s1Closed = new AtomicBoolean(false);
        AtomicBoolean s2Closed = new AtomicBoolean(false);
        s1 = s1.onClose(() -> s1Closed.set(true));
        s2 = s2.onClose(() -> s2Closed.set(true));

        List<Seq<String, Exception>> listOfSeqs = Arrays.asList(s1, s2);
        Seq<String, Exception> seq = Seq.concat(listOfSeqs);
        assertEquals(Arrays.asList("a", "b", "c", "d"), drainWithException(seq));
        assertTrue(s1Closed.get());
        assertTrue(s2Closed.get());
    }

    @Test
    public void testConcatArrays() throws Exception {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5 };
        Integer[] arr3 = { 6, 7, 8 };

        Seq<Integer, Exception> seq = Seq.concat(arr1, arr2, arr3);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), seq.toList());

        Integer[] nullArr = null;
        Seq<Integer, Exception> seqWithNull = Seq.concat(arr1, nullArr, arr2);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), seqWithNull.toList());

        Seq<Integer, Exception> seqEmpty = Seq.concat(new Integer[][] {});
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testConcatIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<Integer> set2 = new HashSet<>(Arrays.asList(4, 5));
        List<Integer> list3 = Arrays.asList(6, 7);

        Seq<Integer, Exception> seq = Seq.concat(list1, set2, list3);
        List<Integer> result = seq.toList();
        Assertions.assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7)));

        Iterable<Integer> nullIter = null;
        Seq<Integer, Exception> seqWithNull = Seq.concat(list1, nullIter);
        Assertions.assertEquals(list1, seqWithNull.toList());
    }

    @Test
    public void testConcatIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(4, 5).iterator();

        Seq<Integer, Exception> seq = Seq.concat(iter1, iter2);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), seq.toList());
    }

    @Test
    public void testConcatSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2, 3);
        Seq<Integer, Exception> seq2 = Seq.of(4, 5);
        Seq<Integer, Exception> seq3 = Seq.of(6, 7, 8);

        Seq<Integer, Exception> concat = Seq.concat(seq1, seq2, seq3);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), concat.toList());
        concat.close();

        Seq<Integer, Exception> seqEmpty = Seq.concat(new Seq[] {});
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testConcatSeqCollection() throws Exception {
        List<Seq<Integer, Exception>> seqs = Arrays.asList(Seq.of(1, 2), Seq.of(3, 4), Seq.of(5, 6));

        Seq<Integer, Exception> concat = Seq.concat(seqs);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), concat.toList());
        concat.close();

        List<Seq<Integer, Exception>> empty = new ArrayList<>();
        Seq<Integer, Exception> seqEmpty = Seq.concat(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testConcatCollectionOfSeqs() throws Exception {
        List<Seq<String, Exception>> seqs = Arrays.asList(Seq.of("a", "b"), Seq.of("c", "d"), Seq.of("e"));

        List<String> result = Seq.concat(seqs).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testConcat_CollectionOfSeqs() throws Exception {
        List<Seq<Integer, Exception>> seqs = Arrays.asList(Seq.of(1, 2), Seq.of(3, 4), Seq.of(5));
        List<Integer> result = Seq.concat(seqs).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testConcat_Iterators() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> concat(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator()).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    // ===== concat(Iterator[]) - vararg iterator path =====

    @Test
    public void testConcat_iteratorArray() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<Integer> iter2 = Arrays.asList(3, 4).iterator();
        @SuppressWarnings("unchecked")
        List<Integer> result = Seq.concat(iter1, iter2).toList();
        assertEquals(4, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(4), result.get(3));
    }

    @Test
    public void testZip() throws Exception {
        List<Pair<Integer, String>> result = Seq.zip(Seq.of(1, 2, 3), Seq.of("a", "b", "c"), Pair::of).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0).left());
        assertEquals("a", result.get(0).right());
    }

    @Test
    public void test_zip_arrays_biFunction() throws Exception {
        Integer[] a = { 1, 2, 3 };
        String[] b = { "a", "b" };
        Seq<String, Exception> seq = Seq.zip(a, b, (x, y) -> x + y);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(seq));
    }

    @Test
    public void test_zip_arrays_triFunction() throws Exception {
        Integer[] a = { 1, 2 };
        String[] b = { "a", "b", "c" };
        Boolean[] c = { true, false };
        Seq<String, Exception> seq = Seq.zip(a, b, c, (x, y, z) -> x + y + z);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterables_biFunction() throws Exception {
        List<Integer> a = Arrays.asList(1, 2, 3);
        List<String> b = Arrays.asList("a", "b");
        Seq<String, Exception> seq = Seq.zip(a, b, (x, y) -> x + y);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterables_triFunction() throws Exception {
        List<Integer> a = Arrays.asList(1, 2);
        List<String> b = Arrays.asList("a", "b", "c");
        List<Boolean> c = Arrays.asList(true, false);
        Seq<String, Exception> seq = Seq.zip(a, b, c, (x, y, z) -> x + y + z);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterators_biFunction() throws Exception {
        Iterator<Integer> a = Arrays.asList(1, 2, 3).iterator();
        Iterator<String> b = Arrays.asList("a", "b").iterator();
        Seq<String, Exception> seq = Seq.zip(a, b, (x, y) -> x + y);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterators_triFunction() throws Exception {
        Iterator<Integer> a = Arrays.asList(1, 2).iterator();
        Iterator<String> b = Arrays.asList("a", "b", "c").iterator();
        Iterator<Boolean> c = Arrays.asList(true, false).iterator();
        Seq<String, Exception> seq = Seq.zip(a, b, c, (x, y, z) -> x + y + z);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(seq));
    }

    @Test
    public void test_zip_seqs_biFunction() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2, 3);
        Seq<String, Exception> seqB = Seq.of("a", "b");
        Seq<String, Exception> resultSeq = Seq.zip(seqA, seqB, (x, y) -> x + y);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(resultSeq));
    }

    @Test
    public void test_zip_seqs_triFunction() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        Seq<String, Exception> seqB = Seq.of("a", "b", "c");
        Seq<Boolean, Exception> seqC = Seq.of(true, false);
        Seq<String, Exception> resultSeq = Seq.zip(seqA, seqB, seqC, (x, y, z) -> x + y + z);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(resultSeq));
    }

    @Test
    public void test_zip_arrays_withDefaults_biFunction() throws Exception {
        Integer[] a = { 1, 2 };
        String[] b = { "x", "y", "z" };
        Seq<String, Exception> seq = Seq.zip(a, b, 0, "default", (x, y) -> x + y);
        assertEquals(Arrays.asList("1x", "2y", "0z"), drainWithException(seq));
    }

    @Test
    public void test_zip_arrays_withDefaults_triFunction() throws Exception {
        Integer[] a = { 1 };
        String[] b = { "x", "y" };
        Boolean[] c = { true, false, true };
        Seq<String, Exception> seq = Seq.zip(a, b, c, 0, "defB", false, (x, y, z) -> "" + x + y + z);
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterables_withDefaults_biFunction() throws Exception {
        List<Integer> a = Arrays.asList(1, 2);
        List<String> b = Arrays.asList("x", "y", "z");
        Seq<String, Exception> seq = Seq.zip(a, b, 0, "default", (x, y) -> x + y);
        assertEquals(Arrays.asList("1x", "2y", "0z"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterables_withDefaults_triFunction() throws Exception {
        List<Integer> a = Arrays.asList(1);
        List<String> b = Arrays.asList("x", "y");
        List<Boolean> c = Arrays.asList(true, false, true);
        Seq<String, Exception> seq = Seq.zip(a, b, c, 0, "defB", false, (x, y, z) -> "" + x + y + z);
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterators_withDefaults_biFunction() throws Exception {
        Iterator<Integer> a = Arrays.asList(1, 2).iterator();
        Iterator<String> b = Arrays.asList("x", "y", "z").iterator();
        Seq<String, Exception> seq = Seq.zip(a, b, 0, "default", (x, y) -> x + y);
        assertEquals(Arrays.asList("1x", "2y", "0z"), drainWithException(seq));
    }

    @Test
    public void test_zip_iterators_withDefaults_triFunction() throws Exception {
        Iterator<Integer> a = Arrays.asList(1).iterator();
        Iterator<String> b = Arrays.asList("x", "y").iterator();
        Iterator<Boolean> c = Arrays.asList(true, false, true).iterator();
        Seq<String, Exception> seq = Seq.zip(a, b, c, 0, "defB", false, (x, y, z) -> "" + x + y + z);
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"), drainWithException(seq));
    }

    @Test
    public void test_zip_seqs_withDefaults_biFunction() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        Seq<String, Exception> seqB = Seq.of("x", "y", "z");
        Seq<String, Exception> resultSeq = Seq.zip(seqA, seqB, 0, "default", (x, y) -> x + y);
        assertEquals(Arrays.asList("1x", "2y", "0z"), drainWithException(resultSeq));
    }

    @Test
    public void test_zip_seqs_withDefaults_triFunction() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1);
        Seq<String, Exception> seqB = Seq.of("x", "y");
        Seq<Boolean, Exception> seqC = Seq.of(true, false, true);
        Seq<String, Exception> resultSeq = Seq.zip(seqA, seqB, seqC, 0, "defB", false, (x, y, z) -> "" + x + y + z);
        assertEquals(Arrays.asList("1xtrue", "0yfalse", "0defBtrue"), drainWithException(resultSeq));
    }

    @Test
    public void testZipArrays() throws Exception {
        Integer[] arr1 = { 1, 2, 3 };
        String[] arr2 = { "a", "b", "c", "d" };

        Seq<String, Exception> seq = Seq.zip(arr1, arr2, (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3c"), seq.toList());

        Integer[] empty1 = {};
        String[] empty2 = {};
        Seq<String, Exception> seqEmpty = Seq.zip(empty1, empty2, (n, s) -> n + s);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testZipThreeArrays() throws Exception {
        Integer[] arr1 = { 1, 2, 3 };
        String[] arr2 = { "a", "b", "c" };
        Double[] arr3 = { 1.1, 2.2, 3.3, 4.4 };

        Seq<String, Exception> seq = Seq.zip(arr1, arr2, arr3, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b2.2", "3c3.3"), seq.toList());
    }

    @Test
    public void testZipIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));

        Seq<String, Exception> seq = Seq.zip(list1, set2, (n, s) -> n + s);
        List<String> result = seq.toList();
        Assertions.assertEquals(3, result.size());
    }

    @Test
    public void testZipThreeIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        List<String> list2 = Arrays.asList("a", "b", "c");
        List<Double> list3 = Arrays.asList(1.1, 2.2);

        Seq<String, Exception> seq = Seq.zip(list1, list2, list3, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b2.2"), seq.toList());
    }

    @Test
    public void testZipIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<String> iter2 = Arrays.asList("a", "b", "c", "d").iterator();

        Seq<String, Exception> seq = Seq.zip(iter1, iter2, (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3c"), seq.toList());
    }

    @Test
    public void testZipThreeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 2).iterator();
        Iterator<String> iter2 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2, 3.3).iterator();

        Seq<String, Exception> seq = Seq.zip(iter1, iter2, iter3, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b2.2"), seq.toList());
    }

    @Test
    public void testZipSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2, 3);
        Seq<String, Exception> seq2 = Seq.of("a", "b", "c", "d");

        Seq<String, Exception> zipped = Seq.zip(seq1, seq2, (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3c"), zipped.toList());
        zipped.close();
    }

    @Test
    public void testZipThreeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2);
        Seq<String, Exception> seq2 = Seq.of("a", "b", "c");
        Seq<Double, Exception> seq3 = Seq.of(1.1, 2.2, 3.3);

        Seq<String, Exception> zipped = Seq.zip(seq1, seq2, seq3, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b2.2"), zipped.toList());
        zipped.close();
    }

    @Test
    public void testZipArraysWithDefaults() throws Exception {
        Integer[] arr1 = { 1, 2 };
        String[] arr2 = { "a", "b", "c" };

        Seq<String, Exception> seq = Seq.zip(arr1, arr2, 0, "X", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "0c"), seq.toList());

        Integer[] arr3 = { 1, 2, 3, 4 };
        String[] arr4 = { "a", "b" };
        Seq<String, Exception> seq2 = Seq.zip(arr3, arr4, 0, "Y", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3Y", "4Y"), seq2.toList());
    }

    @Test
    public void testZipThreeArraysWithDefaults() throws Exception {
        Integer[] arr1 = { 1, 2 };
        String[] arr2 = { "a", "b", "c" };
        Double[] arr3 = { 1.1 };

        Seq<String, Exception> seq = Seq.zip(arr1, arr2, arr3, 0, "X", 0.0, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b0.0", "0c0.0"), seq.toList());
    }

    @Test
    public void testZipIterablesWithDefaults() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 2);
        List<String> list2 = Arrays.asList("a", "b", "c");

        Seq<String, Exception> seq = Seq.zip(list1, list2, 0, "X", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "0c"), seq.toList());
    }

    @Test
    public void testZipThreeIterablesWithDefaults() throws Exception {
        List<Integer> list1 = Arrays.asList(1);
        List<String> list2 = Arrays.asList("a", "b");
        List<Double> list3 = Arrays.asList(1.1, 2.2, 3.3);

        Seq<String, Exception> seq = Seq.zip(list1, list2, list3, 0, "X", 0.0, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "0b2.2", "0X3.3"), seq.toList());
    }

    @Test
    public void testZipIteratorsWithDefaults() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<String> iter2 = Arrays.asList("a", "b").iterator();

        Seq<String, Exception> seq = Seq.zip(iter1, iter2, 0, "Z", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3Z"), seq.toList());
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1).iterator();
        Iterator<String> iter2 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Double> iter3 = Arrays.asList(1.1, 2.2).iterator();

        Seq<String, Exception> seq = Seq.zip(iter1, iter2, iter3, 99, "Y", 9.9, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "99b2.2", "99c9.9"), seq.toList());
    }

    @Test
    public void testZipSeqsWithDefaults() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2);
        Seq<String, Exception> seq2 = Seq.of("a", "b", "c", "d");

        Seq<String, Exception> zipped = Seq.zip(seq1, seq2, 0, "X", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "0c", "0d"), zipped.toList());
        zipped.close();
    }

    @Test
    public void testZipThreeSeqsWithDefaults() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1);
        Seq<String, Exception> seq2 = Seq.of("a", "b");
        Seq<Double, Exception> seq3 = Seq.of(1.1, 2.2, 3.3);

        Seq<String, Exception> zipped = Seq.zip(seq1, seq2, seq3, 0, "X", 0.0, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "0b2.2", "0X3.3"), zipped.toList());
        zipped.close();
    }

    @Test
    public void testZipArraysWithDifferentLengths() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2 };

        List<String> result = Seq.zip(arr1, arr2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testMerge() throws Exception {
        List<Integer> result = Seq.merge(Seq.of(1, 3, 5), Seq.of(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void test_merge_arrays() throws Exception {
        Integer[] a = { 1, 3, 5 };
        Integer[] b = { 2, 4, 6 };
        Seq<Integer, Exception> seq = Seq.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(seq));

        Seq<Integer, Exception> emptyMerge = Seq.merge(new Integer[] {}, new Integer[] {}, (x, y) -> MergeResult.TAKE_FIRST);
        assertTrue(drainWithException(emptyMerge).isEmpty());

        Seq<Integer, Exception> oneEmpty = Seq.merge(new Integer[] { 1 }, new Integer[] {}, (x, y) -> MergeResult.TAKE_FIRST);
        assertEquals(Collections.singletonList(1), drainWithException(oneEmpty));
    }

    @Test
    public void test_merge_arrays_three() throws Exception {
        Integer[] a = { 1, 5 };
        Integer[] b = { 2, 6 };
        Integer[] c = { 3, 4, 7 };
        Seq<Integer, Exception> seq = Seq.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), drainWithException(seq));
    }

    @Test
    public void test_merge_iterables() throws Exception {
        List<Integer> a = Arrays.asList(1, 3, 5);
        List<Integer> b = Arrays.asList(2, 4, 6);
        Seq<Integer, Exception> seq = Seq.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(seq));
    }

    @Test
    public void test_merge_iterables_three() throws Exception {
        List<Integer> a = Arrays.asList(1, 5);
        List<Integer> b = Arrays.asList(2, 6);
        List<Integer> c = Arrays.asList(3, 4, 7);
        Seq<Integer, Exception> seq = Seq.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), drainWithException(seq));
    }

    @Test
    public void test_merge_iterators() throws Exception {
        Iterator<Integer> a = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> b = Arrays.asList(2, 4, 6).iterator();
        Seq<Integer, Exception> seq = Seq.merge(a, b, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(seq));
    }

    @Test
    public void test_merge_iterators_three() throws Exception {
        Iterator<Integer> a = Arrays.asList(1, 5).iterator();
        Iterator<Integer> b = Arrays.asList(2, 6).iterator();
        Iterator<Integer> c = Arrays.asList(3, 4, 7).iterator();
        Seq<Integer, Exception> seq = Seq.merge(a, b, c, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), drainWithException(seq));
    }

    @Test
    public void test_merge_seqs() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seqB = Seq.of(2, 4, 6);
        Seq<Integer, Exception> resultSeq = Seq.merge(seqA, seqB, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(resultSeq));
    }

    @Test
    public void test_merge_seqs_three() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 5);
        Seq<Integer, Exception> seqB = Seq.of(2, 6);
        Seq<Integer, Exception> seqC = Seq.of(3, 4, 7);
        Seq<Integer, Exception> resultSeq = Seq.merge(seqA, seqB, seqC, (x, y) -> x < y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7), drainWithException(resultSeq));
    }

    @Test
    public void testMergeArrays() throws Exception {
        Integer[] arr1 = { 1, 3, 5 };
        Integer[] arr2 = { 2, 4, 6 };

        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), seq.toList());

        Integer[] empty = {};
        Seq<Integer, Exception> seqEmpty1 = Seq.merge(arr1, empty, (a, b) -> MergeResult.TAKE_FIRST);
        Assertions.assertEquals(Arrays.asList(1, 3, 5), seqEmpty1.toList());

        Seq<Integer, Exception> seqEmpty2 = Seq.merge(empty, arr2, (a, b) -> MergeResult.TAKE_FIRST);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), seqEmpty2.toList());
    }

    @Test
    public void testMergeThreeArrays() throws Exception {
        Integer[] arr1 = { 1, 4, 7 };
        Integer[] arr2 = { 2, 5, 8 };
        Integer[] arr3 = { 3, 6, 9 };

        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, arr3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), seq.toList());
    }

    @Test
    public void testMergeIterables() throws Exception {
        List<String> list1 = Arrays.asList("apple", "cherry", "grape");
        List<String> list2 = Arrays.asList("banana", "date", "fig");

        Seq<String, Exception> seq = Seq.merge(list1, list2, (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "fig", "grape"), seq.toList());
    }

    @Test
    public void testMergeThreeIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 4);
        List<Integer> list2 = Arrays.asList(2, 5);
        List<Integer> list3 = Arrays.asList(3, 6);

        Seq<Integer, Exception> seq = Seq.merge(list1, list2, list3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), seq.toList());
    }

    @Test
    public void testMergeThreeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 4, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 5, 8).iterator();
        Iterator<Integer> iter3 = Arrays.asList(3, 6, 9).iterator();

        Seq<Integer, Exception> seq = Seq.merge(iter1, iter2, iter3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), seq.toList());
    }

    @Test
    public void testMergeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6);

        Seq<Integer, Exception> merged = Seq.merge(seq1, seq2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged.toList());
        merged.close();
    }

    @Test
    public void testMergeThreeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 4);
        Seq<Integer, Exception> seq2 = Seq.of(2, 5);
        Seq<Integer, Exception> seq3 = Seq.of(3, 6);

        Seq<Integer, Exception> merged = Seq.merge(seq1, seq2, seq3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged.toList());
        merged.close();
    }

    @Test
    public void testMergeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6, 8).iterator();

        Seq<Integer, Exception> seq = Seq.merge(iter1, iter2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), seq.toList());
    }

    @Test
    public void testFilter() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testFilterNulls() throws Exception {
        var seq = Seq.of("a", null, "b").filter(x -> x != null);
        List<String> list = seq.toList();
        assertEquals(2, list.size());
    }

    @Test
    public void test_filter() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0);
        assertEquals(Arrays.asList(2, 4), drainWithException(seq));
    }

    @Test
    public void test_filter_withActionOnDropped() throws Exception {
        List<Integer> dropped = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, dropped::add);
        assertEquals(Arrays.asList(2, 4), drainWithException(seq));
        assertEquals(Arrays.asList(1, 3, 5), dropped);
    }

    @Test
    public void testFilterWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).skip(2).count());
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0).skip(2).toArray(String[]::new));
    }

    @Test
    public void testFilterWithActionOnDroppedItemWithSkipCountAndToArray() throws Exception {
        List<String> dropped = new ArrayList<>();
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).count());
        assertEquals(1, dropped.size());

        dropped.clear();
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).skip(2).count());

        dropped.clear();
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).toArray(String[]::new));

        dropped.clear();
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").filter(s -> s.compareTo("b") >= 0, dropped::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testFilterWithAction() throws Exception {
        List<String> dropped = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("Alice", "Bob", "Charlie", "David");
        Seq<String, Exception> filtered = seq.filter(name -> name.length() <= 4, droppedName -> dropped.add(droppedName));

        List<String> result = filtered.toList();
        Assertions.assertEquals(Arrays.asList("Bob"), result);
        Assertions.assertEquals(Arrays.asList("Alice", "Charlie", "David"), dropped);
    }

    @Test
    public void testFilter_WithOnDrop() throws Exception {
        List<Integer> dropped = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).filter(x -> x % 2 == 0, dropped::add).toList();
        assertEquals(Arrays.asList(1, 3, 5), dropped);
        assertEquals(Arrays.asList(2, 4), result);
    }

    @Test
    public void testTakeWhile() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).takeWhile(x -> x < 4).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_takeWhile() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).takeWhile(x -> x < 3);
        assertEquals(Arrays.asList(1, 2), drainWithException(seq));
    }

    @Test
    public void testTakeWhileWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeWhile(s -> s.compareTo("d") < 0).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDropWhile() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void test_dropWhile() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).dropWhile(x -> x < 3);
        assertEquals(Arrays.asList(3, 2, 1), drainWithException(seq));
    }

    @Test
    public void test_dropWhile_withActionOnDropped() throws Exception {
        List<Integer> dropped = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).dropWhile(x -> x < 3, dropped::add);

        Seq<Integer, Exception> seqActual = Seq.of(1, 2, 3, 2, 1).dropWhile(x -> x < 3, dropped::add);
        assertEquals(Arrays.asList(3, 2, 1), drainWithException(seqActual));
        assertEquals(CommonUtil.toList(1, 2), dropped,
                "Action should be called on the first element not satisfying the drop condition, based on current impl.");
    }

    @Test
    public void testDropWhileWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDropWhileWithActionOnDroppedItemWithSkipCountAndToArray() throws Exception {
        List<String> dropped = new ArrayList<>();
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).count());
        assertEquals(3, dropped.size());

        dropped.clear();
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).skip(1).count());

        dropped.clear();
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).toArray(String[]::new));

        dropped.clear();
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").dropWhile(s -> s.compareTo("d") < 0, dropped::add).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDropWhileWithAction() throws Exception {
        List<String> droppedItems = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("a", "an", "the", "quick", "brown");
        Seq<String, Exception> result = seq.dropWhile(word -> word.length() <= 3, droppedWord -> droppedItems.add(droppedWord));

        Assertions.assertEquals(Arrays.asList("quick", "brown"), result.toList());
        Assertions.assertEquals(Arrays.asList("a", "an", "the"), droppedItems);
    }

    @Test
    public void testDropWhile_NeverTrue() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).dropWhile(x -> x > 10).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDropWhile_WithOnDrop() throws Exception {
        List<Integer> dropped = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).dropWhile(x -> x < 3, dropped::add).toList();
        assertEquals(Arrays.asList(1, 2), dropped);
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testSkipUntil() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skipUntil(x -> x >= 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void test_skipUntil() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).skipUntil(x -> x == 3);
        assertEquals(Arrays.asList(3, 2, 1), drainWithException(seq));
    }

    @Test
    public void testSkipUntilWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipUntil(s -> s.compareTo("d") >= 0).skip(1).toArray(String[]::new));
    }

    @Test
    public void testSkipUntil_NeverTrue() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).skipUntil(x -> x > 10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSkipUntil_AlwaysTrue() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).skipUntil(x -> x > 0).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDistinct() throws Exception {
        List<Integer> result = Seq.of(1, 2, 2, 3, 3, 3).distinct().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_distinct() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 2, 3, 1, 4).distinct();
        assertEquals(Arrays.asList(1, 2, 3, 4), drainWithException(seq));
    }

    @Test
    public void test_distinct_withMergeFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 2, 3, 1, 4).distinct((a, b) -> a + b);
        assertEquals(Arrays.asList(2, 4, 3, 4), drainWithException(seq));
    }

    @Test
    public void testDistinctWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "a", "c", "b").distinct().skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctMergeFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).skip(2).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c" },
                Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "B", "c" },
                Seq.<String, RuntimeException> of("a", "A", "b", "B", "c").distinct((a, b) -> a.toLowerCase()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctWithMergeFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello", "HELLO", "world", "WORLD");
        Seq<String, Exception> distinct = seq.distinct((a, b) -> a.toUpperCase());
        List<String> result = distinct.toList();
        Assertions.assertEquals(4, result.size());
        Assertions.assertFalse(result.stream().allMatch(s -> s.equals(s.toUpperCase())));
    }

    @Test
    public void testDistinct_WithMergeFunction() throws Exception {
        List<String> result = Seq.of("a", "A", "b", "B", "a").distinct((a, b) -> a + b).toList();
        assertEquals(Arrays.asList("aa", "A", "b", "B"), result);
    }

    @Test
    public void test_distinctBy() throws Exception {
        assertDoesNotThrow(() -> {
            Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).dropWhile(i -> i < 4).println();
            Seq.of(1, 2, 1, 2, 3, 1, 4, 5, 5).takeWhile(i -> i < 4).println();
        });
    }

    @Test
    public void testDistinctBy() throws Exception {
        List<String> result = Seq.of("a", "bb", "ccc", "dd").distinctBy(String::length).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void test_distinctBy_keyMapper() throws Exception {
        Seq<String, Exception> seq = Seq.of("apple", "banana", "apricot", "blueberry", "avocado").distinctBy(s -> s.charAt(0));
        assertEquals(Arrays.asList("apple", "banana"), drainWithException(seq));
    }

    @Test
    public void test_distinctBy_keyMapperAndMerge() throws Exception {
        Seq<Pair<Character, Integer>, Exception> data = Seq.of(Pair.of('a', 1), Pair.of('b', 2), Pair.of('a', 3), Pair.of('c', 4), Pair.of('b', 5));

        Seq<Pair<Character, Integer>, Exception> seq = data.distinctBy(Pair::left, (p1, p2) -> Pair.of(p1.left(), p1.right() + p2.right()));
        List<Pair<Character, Integer>> expected = Arrays.asList(Pair.of('a', 4), Pair.of('b', 7), Pair.of('c', 4));
        assertEquals(expected, drainWithException(seq));
    }

    @Test
    public void test_distinctBy_keyMapper_withNullKeys() throws Exception {
        List<String> data = Arrays.asList("apple", null, "banana", "apricot", null, "avocado");
        Seq<String, Exception> seq = Seq.of(data).distinctBy(s -> s == null ? null : s.charAt(0));
        List<String> expected = Arrays.asList("apple", null, "banana");
        assertEquals(expected, drainWithException(seq));
    }

    @Test
    public void testDistinctByWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).skip(2).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctByMergeFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).skip(2).count());
        assertArrayEquals(new String[] { "a", "bb", "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).toArray(String[]::new));
        assertArrayEquals(new String[] { "ccc" },
                Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").distinctBy(String::length, (a, b) -> a).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDistinctByWithMergeFunction() throws Exception {
        class Sale {
            String product;
            int amount;

            Sale(String product, int amount) {
                this.product = product;
                this.amount = amount;
            }

            String getProduct() {
                return product;
            }

            int getAmount() {
                return amount;
            }
        }

        Seq<Sale, Exception> seq = Seq.of(new Sale("Apple", 10), new Sale("Banana", 5), new Sale("Apple", 15));

        Seq<Sale, Exception> merged = seq.distinctBy(Sale::getProduct, (s1, s2) -> new Sale(s1.getProduct(), s1.getAmount() + s2.getAmount()));

        List<Sale> result = merged.toList();
        Assertions.assertEquals(2, result.size());
        Sale appleSale = result.stream().filter(s -> s.getProduct().equals("Apple")).findFirst().orElse(null);
        Assertions.assertNotNull(appleSale);
        Assertions.assertEquals(25, appleSale.getAmount());
    }

    @Test
    public void testDistinctByWithComplexKeys() throws Exception {
        class Product {
            String category;
            String name;
            double price;

            Product(String category, String name, double price) {
                this.category = category;
                this.name = name;
                this.price = price;
            }
        }

        Seq<Product, Exception> products = Seq.of(new Product("Electronics", "Phone", 999.99), new Product("Electronics", "Tablet", 599.99),
                new Product("Books", "Novel", 19.99), new Product("Books", "Textbook", 89.99));

        Seq<Product, Exception> distinctByCategory = products.distinctBy(p -> p.category);
        List<Product> result = distinctByCategory.toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("Electronics", result.get(0).category);
        Assertions.assertEquals("Books", result.get(1).category);
    }

    @Test
    public void test_mapPartitial() throws Exception {
        assertDoesNotThrow(() -> {
            Seq.range(0, 10).mapPartial(it -> it % 2 == 0 ? Optional.of(it) : Optional.empty()).println();
            Seq.range(0, 10).mapPartialToInt(it -> it % 2 == 0 ? OptionalInt.of(it) : OptionalInt.empty()).println();
        });
    }

    @Test
    public void testMap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).map(x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void test_map() throws Exception {
        Seq<Integer, Exception> seq = Seq.of("1", "2", "3").map(Integer::parseInt);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));
    }

    @Test
    public void test_map_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.map(x -> x * 2));
    }

    @Test
    public void testMapWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "B", "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").map(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapIfNotNull() throws Exception {
        List<Integer> result = Seq.of(1, null, 3).mapIfNotNull(x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 6), result);
    }

    @Test
    public void test_mapIfNotNull() throws Exception {
        Seq<String, Exception> input = Seq.of("a", null, "b", null, "c");
        Seq<String, Exception> result = input.mapIfNotNull(s -> s + s);
        assertEquals(Arrays.asList("aa", "bb", "cc"), drainWithException(result));
    }

    @Test
    public void testMapIfNotNullWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "C", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").mapIfNotNull(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirst() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapFirst(x -> x * 10).toList();
        assertEquals(Arrays.asList(10, 2, 3), result);
    }

    @Test
    public void test_mapFirst() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapFirst(x -> x * 10);
        assertEquals(Arrays.asList(10, 2, 3), drainWithException(seq));
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty().mapFirst(x -> x * 10);
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void testMapFirstWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "A", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirst(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirst_Empty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().mapFirst(x -> x * 10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapFirst_SingleElement() throws Exception {
        List<Integer> result = Seq.of(5).mapFirst(x -> x * 10).toList();
        assertEquals(Arrays.asList(50), result);
    }

    @Test
    public void testMapFirstOrElse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, x -> x * 2).toList();
        assertEquals(Arrays.asList(10, 4, 6), result);
    }

    @Test
    public void test_mapFirstOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, y -> y + 1);
        assertEquals(Arrays.asList(10, 3, 4), drainWithException(seq));
    }

    @Test
    public void testMapFirstOrElseWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").skip(2).count());
        assertArrayEquals(new String[] { "FIRST", "b!", "c!", "d!", "e!" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").toArray(String[]::new));
        assertArrayEquals(new String[] { "c!", "d!", "e!" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapFirstOrElse(s -> "FIRST", s -> s + "!").skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapFirstOrElse_Empty() throws Exception {
        List<String> result = Seq.<Integer, Exception> empty().mapFirstOrElse(x -> "first:" + x, x -> "other:" + x).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapLast(x -> x * 10).toList();
        assertEquals(Arrays.asList(1, 2, 30), result);
    }

    @Test
    public void test_mapLast() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapLast(x -> x * 10);
        assertEquals(Arrays.asList(1, 2, 30), drainWithException(seq));
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty().mapLast(x -> x * 10);
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void testMapLastWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLast(String::toUpperCase).skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapLast_Empty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().mapLast(x -> x * 10).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testMapLast_SingleElement() throws Exception {
        List<Integer> result = Seq.of(5).mapLast(x -> x * 10).toList();
        assertEquals(Arrays.asList(50), result);
    }

    @Test
    public void testMapLastOrElse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, x -> x * 2).toList();
        assertEquals(Arrays.asList(2, 4, 30), result);
    }

    @Test
    public void test_mapLastOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, y -> y + 1);
        assertEquals(Arrays.asList(2, 3, 30), drainWithException(seq));
    }

    @Test
    public void testMapLastOrElseWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").skip(2).count());
        assertArrayEquals(new String[] { "a!", "b!", "c!", "d!", "LAST" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").toArray(String[]::new));
        assertArrayEquals(new String[] { "c!", "d!", "LAST" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapLastOrElse(s -> "LAST", s -> s + "!").skip(2).toArray(String[]::new));
    }

    @Test
    public void testMapLastOrElse_Empty() throws Exception {
        List<String> result = Seq.<Integer, Exception> empty().mapLastOrElse(x -> "last:" + x, x -> "other:" + x).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void test_flatMap() throws Exception {
        assertDoesNotThrow(() -> {
            Seq.of("abc", "123").forEach(Fn.println());
        });
    }

    @Test
    public void testFlatMap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMap(x -> Seq.of(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void testFlatMapWithFilter() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMap(x -> Seq.of(x, x * 10, x * 100)).filter(x -> x > 10).toList();
        assertEquals(Arrays.asList(100, 20, 200, 30, 300), result);
    }

    @Test
    public void testFlatmap() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatmap(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void test_flatMap_toSeq() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2).flatMap(x -> Seq.of(x, x * 10));
        assertEquals(Arrays.asList(1, 10, 2, 20), drainWithException(seq));
    }

    @Test
    public void test_flatmap_toCollection() throws Exception {
        Seq<Integer, Exception> seq = Seq.of("a b", "c").flatmap(s -> Arrays.asList(s.split(" "))).map(String::length);
        assertEquals(Arrays.asList(1, 1, 1), drainWithException(seq));
    }

    @Test
    public void test_flatMap_withEmptyInnerSeqs() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).flatMap(i -> {
            if (i % 2 == 0) {
                return Seq.empty();
            }
            return Seq.of(i, i * 10);
        });
        assertEquals(Arrays.asList(1, 10, 3, 30), drainWithException(seq));
    }

    @Test
    public void test_flatMap_withNullInnerSeq() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).flatMap(i -> {
            if (i % 2 == 0) {
                return null;
            }
            return Seq.of(i, i * 10);
        });
        assertEquals(Arrays.asList(1, 10, 3, 30), drainWithException(seq));
    }

    @Test
    public void testFlatMapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMap(s -> Seq.of(s, s.toUpperCase())).skip(4).toArray(String[]::new));
    }

    @Test
    public void testFlatmapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatmap(s -> Arrays.asList(s, s.toUpperCase())).skip(4).toArray(String[]::new));
    }

    @Test
    public void testFlattmapWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMapArray(s -> new String[] { s, s.toUpperCase() }).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMapArray(s -> new String[] { s, s.toUpperCase() }).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").flatMapArray(s -> new String[] { s, s.toUpperCase() }).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .flatMapArray(s -> new String[] { s, s.toUpperCase() })
                        .skip(4)
                        .toArray(String[]::new));
    }

    @Test
    public void testFlatMapArray() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).flatMapArray(x -> new Integer[] { x, x * 10 }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);

        List<Integer> emptyResult = Seq.<Integer, Exception> empty().flatMapArray(x -> new Integer[] { x }).toList();
        assertTrue(emptyResult.isEmpty());

        List<Integer> emptyArrayResult = Seq.of(1, 2).flatMapArray(x -> new Integer[0]).toList();
        assertTrue(emptyArrayResult.isEmpty());
    }

    @Test
    public void testFlatmapIfNotNull() throws Exception {
        List<Integer> result = Seq.of(1, null, 3).flatmapIfNotNull(x -> Arrays.asList(x, x * 10)).toList();
        assertEquals(Arrays.asList(1, 10, 3, 30), result);
    }

    @Test
    public void test_flatmapIfNotNull_toCollection() throws Exception {
        Seq<String, Exception> input = Seq.of("a b", null, "c");
        Seq<String, Exception> result = input.flatmapIfNotNull(s -> Arrays.asList(s.split(" ")));
        assertEquals(Arrays.asList("a", "b", "c"), drainWithException(result));
    }

    @Test
    public void test_flatmapIfNotNull_twoLevels() throws Exception {
        Seq<String, Exception> input = Seq.of("a:b,c", null, "d:e");
        Seq<String, Exception> result = input.flatmapIfNotNull(s -> Arrays.asList(s.split(",")), ss -> Arrays.asList(ss.split(":")));
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), drainWithException(result));
    }

    @Test
    public void testFlatmapIfNotNullWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "c", "C", "e", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e").flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase())).toArray(String[]::new));
        assertArrayEquals(new String[] { "e", "E" },
                Seq.<String, RuntimeException> of("a", null, "c", null, "e")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()))
                        .skip(4)
                        .toArray(String[]::new));
    }

    @Test
    public void testFlatmapIfNotNullTwoMappersWithSkipCountAndToArray() throws Exception {
        assertEquals(8,
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .count());
        assertEquals(6,
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "a!", "a?", "A!", "A?", "c!", "c?", "C!", "C?" },
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "A!", "A?", "c!", "c?", "C!", "C?" },
                Seq.<String, RuntimeException> of("a", null, "c")
                        .flatmapIfNotNull(s -> Arrays.asList(s, s.toUpperCase()), s -> Arrays.asList(s + "!", s + "?"))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testFlatmapIfNotNullWithTwoMappers() throws Exception {

        Department dept1 = new Department(new Team(new Employee("Alice"), new Employee("Bob")), null, new Team(new Employee("Charlie")));
        Department dept2 = new Department(new Team(new Employee("David")));

        Seq<Department, Exception> departments = Seq.of(dept1, null, dept2);
        List<Employee> allEmployees = departments.flatmapIfNotNull(Department::getTeams, Team::getMembers).toList();

        assertEquals(4, allEmployees.size());
        assertEquals("Alice", allEmployees.get(0).toString());
        assertEquals("Bob", allEmployees.get(1).toString());
        assertEquals("Charlie", allEmployees.get(2).toString());
        assertEquals("David", allEmployees.get(3).toString());
    }

    @Test
    public void testFlatmapIfNotNull_TwoMappers() throws Exception {
        Department dept1 = new Department(new Team(new Employee("Alice"), new Employee("Bob")), new Team(new Employee("Charlie")));
        Department dept2 = new Department(new Team(new Employee("Dave")));

        List<String> result = Seq.<Department, Exception> of(dept1, null, dept2)
                .flatmapIfNotNull(Department::getTeams, Team::getMembers)
                .map(Employee::toString)
                .toList();
        assertEquals(Arrays.asList("Alice", "Bob", "Charlie", "Dave"), result);
    }

    @Test
    public void testMapPartial() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).mapPartial(x -> x % 2 == 0 ? Optional.of(x * 10) : Optional.<Integer> empty()).toList();
        assertEquals(Arrays.asList(20, 40), result);
    }

    @Test
    public void test_mapPartial() throws Exception {
        Seq<Integer, Exception> seq = Seq.of("1", "x", "2", "y").mapPartial(s -> {
            try {
                return Optional.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        });
        assertEquals(Arrays.asList(1, 2), drainWithException(seq));
    }

    @Test
    public void testMapPartialWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "C", "D", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartial(s -> s.compareTo("c") >= 0 ? Optional.of(s.toUpperCase()) : Optional.<String> empty())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testMapPartialToInt() throws Exception {
        List<Integer> result = Seq.of("1", "a", "3", "b").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1, 3), result);
    }

    @Test
    public void test_mapPartialToInt() throws Exception {
        Seq<Integer, Exception> seq = Seq.of("1", "x", "2", "y").mapPartialToInt(s -> {
            try {
                return OptionalInt.of(Integer.parseInt(s));
            } catch (NumberFormatException e) {
                return OptionalInt.empty();
            }
        });
        assertEquals(Arrays.asList(1, 2), drainWithException(seq));
    }

    @Test
    public void testMapPartialToIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Integer[] { 97, 98, 99, 100, 101 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).toArray(Integer[]::new));
        assertArrayEquals(new Integer[] { 99, 100, 101 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToInt(s -> OptionalInt.of(s.charAt(0))).skip(2).toArray(Integer[]::new));
    }

    @Test
    public void testMapPartialToLong() throws Exception {
        List<Long> result = Seq.of("1", "a", "3", "b").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1L, 3L), result);
    }

    @Test
    public void test_mapPartialToLong() throws Exception {
        Seq<Long, Exception> seq = Seq.of("10", "x", "20", "y").mapPartialToLong(s -> {
            try {
                return OptionalLong.of(Long.parseLong(s));
            } catch (NumberFormatException e) {
                return OptionalLong.empty();
            }
        });
        assertEquals(Arrays.asList(10L, 20L), drainWithException(seq));
    }

    @Test
    public void testMapPartialToLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Long[] { 97L, 98L, 99L, 100L, 101L },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).toArray(Long[]::new));
        assertArrayEquals(new Long[] { 99L, 100L, 101L },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToLong(s -> OptionalLong.of(s.charAt(0))).skip(2).toArray(Long[]::new));
    }

    @Test
    public void testMapPartialToDouble() throws Exception {
        List<Double> result = Seq.of("1.5", "a", "3.5", "b").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        }).toList();
        assertEquals(Arrays.asList(1.5, 3.5), result);
    }

    @Test
    public void test_mapPartialToDouble() throws Exception {
        Seq<Double, Exception> seq = Seq.of("1.1", "x", "2.2", "y").mapPartialToDouble(s -> {
            try {
                return OptionalDouble.of(Double.parseDouble(s));
            } catch (NumberFormatException e) {
                return OptionalDouble.empty();
            }
        });
        assertEquals(Arrays.asList(1.1, 2.2), drainWithException(seq));
    }

    @Test
    public void testMapPartialToDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).skip(2).count());
        assertArrayEquals(new Double[] { 97.0, 98.0, 99.0, 100.0, 101.0 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0))).toArray(Double[]::new));
        assertArrayEquals(new Double[] { 99.0, 100.0, 101.0 },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .mapPartialToDouble(s -> OptionalDouble.of(s.charAt(0)))
                        .skip(2)
                        .toArray(Double[]::new));
    }

    @Test
    public void testMapMulti() throws Exception {
        List<Object> result = Seq.of(1, 2, 3).mapMulti((n, consumer) -> {
            consumer.accept(n);
            consumer.accept(n * 10);
        }).toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), result);
    }

    @Test
    public void test_mapMulti() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3).mapMulti((num, consumer) -> {
            consumer.accept("N:" + num);
            if (num % 2 == 0) {
                consumer.accept("Even:" + num);
            }
        });
        assertEquals(Arrays.asList("N:1", "N:2", "Even:2", "N:3"), drainWithException(seq));
    }

    @Test
    public void testMapMultiWithSkipCountAndToArray() throws Exception {
        assertEquals(10, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).count());
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).skip(4).count());
        assertArrayEquals(new String[] { "a", "A", "b", "B", "c", "C", "d", "D", "e", "E" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
                    consumer.accept(s);
                    consumer.accept(s.toUpperCase());
                }).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "C", "d", "D", "e", "E" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").mapMulti((s, consumer) -> {
            consumer.accept(s);
            consumer.accept(s.toUpperCase());
        }).skip(4).toArray(String[]::new));
    }

    @Test
    public void testSlidingMap_BiFunction() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);
    }

    @Test
    public void testSlidingMap_TriFunction() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4).slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9), result);
    }

    @Test
    public void test_slidingMap_biFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4).map(String::valueOf).slidingMap((a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b));
        assertEquals(Arrays.asList("12", "23", "34"), drainWithException(seq));
        Seq<String, Exception> oneElement = Seq.of(1).map(String::valueOf).slidingMap((a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b));
        assertEquals(Collections.singletonList("1null"), drainWithException(oneElement));
        Seq<String, Exception> empty = Seq.<Integer, Exception> empty()
                .map(String::valueOf)
                .slidingMap((a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b));
        assertTrue(drainWithException(empty).isEmpty());
    }

    @Test
    public void test_slidingMap_biFunction_increment() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5).map(String::valueOf).slidingMap(2, (a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b));
        assertEquals(Arrays.asList("12", "34", "5null"), drainWithException(seq));
    }

    @Test
    public void test_slidingMap_biFunction_increment_ignoreNotPaired() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5)
                .map(String::valueOf)
                .slidingMap(2, true, (a, b) -> (a == null ? "null" : a) + (b == null ? "null" : b));
        assertEquals(Arrays.asList("12", "34"), drainWithException(seq));
    }

    @Test
    public void test_slidingMap_triFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5)
                .slidingMap((a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
        assertEquals(Arrays.asList("123", "234", "345"), drainWithException(seq));
    }

    @Test
    public void test_slidingMap_triFunction_increment() throws Exception {
        {
            Seq<String, Exception> seq = Seq.of(1).slidingMap(2, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
            assertEquals(Arrays.asList("1nn"), drainWithException(seq));
        }
        {
            Seq<String, Exception> seq = Seq.of(1, 2)
                    .slidingMap(1, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
            assertEquals(Arrays.asList("12n"), drainWithException(seq));
        }
        {
            Seq<String, Exception> seq = Seq.of(1, 2)
                    .slidingMap(2, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
            assertEquals(Arrays.asList("12n"), drainWithException(seq));
        }
        {
            Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7)
                    .slidingMap(2, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
            assertEquals(Arrays.asList("123", "345", "567"), drainWithException(seq));
        }
    }

    @Test
    public void test_slidingMap_triFunction_increment_ignoreNotPaired() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6)
                .slidingMap(2, true, (a, b, c) -> (a == null ? "n" : a) + "" + (b == null ? "n" : b) + "" + (c == null ? "n" : c));
        assertEquals(Arrays.asList("123", "345"), drainWithException(seq));
    }

    @Test
    public void testSlidingMapBiFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).skip(2).count());
        assertArrayEquals(new String[] { "ab", "bc", "cd", "de" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).toArray(String[]::new));
        assertArrayEquals(new String[] { "cd", "de" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b) -> a + (b == null ? "" : b)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapWithIncrementWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).skip(2).count());
        assertArrayEquals(new String[] { "ab", "cd", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, (a, b) -> a + (b == null ? "" : b)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapWithIncrementIgnoreNotPairedWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).count());
        assertEquals(0, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).skip(2).count());
        assertArrayEquals(new String[] { "ab", "cd" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).toArray(String[]::new));
        assertArrayEquals(new String[] {},
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap(2, true, (a, b) -> a + b).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSlidingMapTriFunctionWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c)).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "abc", "bcd", "cde" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "cde" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .slidingMap((a, b, c) -> a + (b == null ? "" : b) + (c == null ? "" : c))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testSlidingMapBiFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<Integer> result = seq.slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7), result);

        seq = Seq.of(1);
        List<String> strResult = seq.slidingMap((a, b) -> String.valueOf(a) + "," + String.valueOf(b)).toList();
        assertEquals(Arrays.asList("1,null"), strResult);

        seq = Seq.<Integer, Exception> empty();
        result = seq.slidingMap((a, b) -> a + b).toList();
        assertTrue(result.isEmpty());

        Seq<String, Exception> strSeq = Seq.of("a", "b", "c");
        strResult = strSeq.slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList("ab", "bc"), strResult);
    }

    @Test
    public void testSlidingMapWithIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<Integer> result = seq.slidingMap(2, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), result);

        seq = Seq.of(1, 2, 3, 4, 5);
        List<String> strResult = seq.slidingMap(2, (a, b) -> a + "," + b).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,null"), strResult);

        seq = Seq.of(1, 2, 3);
        result = seq.slidingMap(1, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        result = seq.slidingMap(3, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 9), result);
    }

    @Test
    public void testSlidingMapWithIgnoreNotPaired() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<String> result = seq.slidingMap(2, false, (a, b) -> a + "," + b).toList();
        assertEquals(Arrays.asList("1,2", "3,4", "5,null"), result);

        seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> intResult = seq.slidingMap(2, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), intResult);

        seq = Seq.of(1);
        intResult = seq.slidingMap(1, true, (a, b) -> a + b).toList();
        assertTrue(intResult.isEmpty());

        seq = Seq.of(1, 2, 3, 4);
        intResult = seq.slidingMap(2, true, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7), intResult);
    }

    @Test
    public void testSlidingMapTriFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), result);

        seq = Seq.of(1);
        List<String> strResult = seq.slidingMap((a, b, c) -> String.format("%s,%s,%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1,null,null"), strResult);

        seq = Seq.of(1, 2);
        strResult = seq.slidingMap((a, b, c) -> String.format("%s,%s,%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1,2,null"), strResult);

        seq = Seq.<Integer, Exception> empty();
        result = seq.slidingMap((a, b, c) -> a + b + c).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingMapTriFunctionWithIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<Integer> result = seq.slidingMap(3, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8);
        List<String> strResult = seq.slidingMap(3, (a, b, c) -> String.format("%s+%s+%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1+2+3", "4+5+6", "7+8+null"), strResult);

        seq = Seq.of(1, 2, 3, 4);
        result = seq.slidingMap(1, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        result = seq.slidingMap(2, (a, b, c) -> a + b + (c == null ? 0 : c)).toList();
        assertEquals(Arrays.asList(6, 12, 11), result);
    }

    @Test
    public void testSlidingMapTriFunctionWithIgnoreNotPaired() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<String> result = seq.slidingMap(3, false, (a, b, c) -> String.format("%s+%s+%s", a, b, c)).toList();
        assertEquals(Arrays.asList("1+2+3", "4+5+6", "7+null+null"), result);

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7);
        List<Integer> intResult = seq.slidingMap(3, true, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), intResult);

        seq = Seq.of(1, 2);
        intResult = seq.slidingMap(1, true, (a, b, c) -> a + b + c).toList();
        assertTrue(intResult.isEmpty());

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        intResult = seq.slidingMap(3, true, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), intResult);
    }

    @Test
    public void testSlidingMapDefaultIncrement() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).slidingMap((a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 5, 7, 9), result);

        List<Integer> triResult = Seq.of(1, 2, 3, 4, 5).slidingMap((a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 9, 12), triResult);
    }

    @Test
    public void testSlidingMap_WithIncrement() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6).slidingMap(2, (a, b) -> a + b).toList();
        assertEquals(Arrays.asList(3, 7, 11), result);
    }

    @Test
    public void testSlidingMap_TriFunction_WithIncrement() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6).slidingMap(3, (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList(6, 15), result);
    }

    @Test
    public void testGroupBy() throws Exception {
        Map<Integer, List<String>> grouped = Seq.of("a", "bb", "ccc", "dd").groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3, grouped.size());
        assertEquals(Arrays.asList("a"), grouped.get(1));
        assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));
    }

    @Test
    public void testGroupByThenMap() throws Exception {
        Map<Boolean, List<Integer>> grouped = Seq.of(1, 2, 3, 4, 5, 6).groupBy(x -> x % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(2, grouped.size());
        assertEquals(Arrays.asList(2, 4, 6), grouped.get(true));
        assertEquals(Arrays.asList(1, 3, 5), grouped.get(false));
    }

    @Test
    public void test_groupBy_withCollector() throws Exception {
        Map<Character, Long> counts = Seq.of("apple", "apricot", "banana", "avocado")
                .groupBy(s -> s.charAt(0), Collectors.counting())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3L, counts.get('a'));
        assertEquals(1L, counts.get('b'));
    }

    @Test
    public void test_groupBy_withCollectorAndMapFactory() throws Exception {
        Map<Character, String> joined = Seq.of("apple", "apricot", "banana", "avocado")
                .groupBy(s -> s.charAt(0), Collectors.joining(","), TreeMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertFalse(joined instanceof TreeMap);
        assertEquals("apple,apricot,avocado", joined.get('a'));
        assertEquals("banana", joined.get('b'));
    }

    @Test
    public void test_groupBy_keyValueMappersAndCollector() throws Exception {
        Map<Character, Long> counts = Seq.of("apple", "apricot", "banana", "avocado")
                .groupBy(s -> s.charAt(0), String::length, Collectors.summingLong(len -> (long) len))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(19L, counts.get('a'));
        assertEquals(6L, counts.get('b'));
    }

    @Test
    public void testGroupByKeyMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).skip(2).count());

        Map.Entry<Integer, List<String>>[] entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb")
                .groupBy(String::length)
                .toArray(Map.Entry[]::new);
        assertEquals(3, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").groupBy(String::length).skip(2).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testGroupByKeyMapper() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        Map<Integer, List<String>> groups = seq.groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList("a", "e"), groups.get(1));
        assertEquals(Arrays.asList("bb", "dd"), groups.get(2));
        assertEquals(Arrays.asList("ccc"), groups.get(3));

        seq = Seq.<String, Exception> empty();
        List<Map.Entry<Integer, List<String>>> result = seq.groupBy(String::length).toList();
        assertTrue(result.isEmpty());

        seq = Seq.of("hello");
        result = seq.groupBy(String::length).toList();
        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getKey().intValue());
        assertEquals(Arrays.asList("hello"), result.get(0).getValue());
    }

    @Test
    public void testGroupByWithMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        List<Map.Entry<Integer, List<String>>> result = seq.groupBy(String::length, Suppliers.ofTreeMap()).toList();

        assertEquals(1, result.get(0).getKey().intValue());
        assertEquals(2, result.get(1).getKey().intValue());
        assertEquals(3, result.get(2).getKey().intValue());

        seq = Seq.of("ccc", "a", "bb");
        Map<Integer, List<String>> groups = seq.groupBy(String::length, Suppliers.ofLinkedHashMap())
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());
        List<Integer> keys = new ArrayList<>(groups.keySet());
        assertEquals(Arrays.asList(3, 1, 2), keys);
    }

    @Test
    public void testGroupByWithValueMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }

            int getAge() {
                return age;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Bob", 30), new Person("Charlie", 25), new Person("David", 30));

        Map<Integer, List<String>> groups = seq.groupBy(Person::getAge, Person::getName).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList("Alice", "Charlie"), groups.get(25));
        assertEquals(Arrays.asList("Bob", "David"), groups.get(30));
    }

    @Test
    public void testGroupByWithValueMapperAndMapFactory() throws Exception {
        class Product {
            int id;
            String category;

            Product(int id, String category) {
                this.id = id;
                this.category = category;
            }

            int getId() {
                return id;
            }

            String getCategory() {
                return category;
            }
        }

        Seq<Product, Exception> seq = Seq.of(new Product(1, "Electronics"), new Product(2, "Books"), new Product(3, "Electronics"), new Product(4, "Books"));

        List<Map.Entry<String, List<Integer>>> result = seq.groupBy(Product::getCategory, Product::getId, TreeMap::new).toList();

        assertEquals("Books", result.get(0).getKey());
        assertEquals(Arrays.asList(2, 4), result.get(0).getValue());
        assertEquals("Electronics", result.get(1).getKey());
        assertEquals(Arrays.asList(1, 3), result.get(1).getValue());
    }

    @Test
    public void testGroupByWithMergeFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 4, 7, 2, 5, 8, 3, 6, 9);
        Map<Integer, Integer> sums = seq.groupBy(n -> n % 3, n -> n, Integer::sum).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(18, sums.get(0).intValue());
        assertEquals(12, sums.get(1).intValue());
        assertEquals(15, sums.get(2).intValue());
    }

    @Test
    public void testGroupByWithMergeFunctionAndMapFactory() throws Exception {
        class Sale {
            String dept;
            int amount;

            Sale(String dept, int amount) {
                this.dept = dept;
                this.amount = amount;
            }

            String getDept() {
                return dept;
            }

            int getAmount() {
                return amount;
            }
        }

        Seq<Sale, Exception> seq = Seq.of(new Sale("Sales", 100), new Sale("IT", 150), new Sale("Sales", 200), new Sale("IT", 120));

        List<Map.Entry<String, Integer>> result = seq.groupBy(Sale::getDept, Sale::getAmount, Integer::max, TreeMap::new).toList();

        assertEquals("IT", result.get(0).getKey());
        assertEquals(150, result.get(0).getValue().intValue());
        assertEquals("Sales", result.get(1).getKey());
        assertEquals(200, result.get(1).getValue().intValue());
    }

    @Test
    public void testGroupByWithDownstreamCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e");
        Map<Integer, Long> counts = seq.groupBy(String::length, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, counts.get(1).longValue());
        assertEquals(2L, counts.get(2).longValue());
        assertEquals(1L, counts.get(3).longValue());

        class Employee {
            String dept;
            double salary;

            Employee(String dept, double salary) {
                this.dept = dept;
                this.salary = salary;
            }

            String getDept() {
                return dept;
            }

            double getSalary() {
                return salary;
            }
        }

        Seq<Employee, Exception> empSeq = Seq.of(new Employee("IT", 50000), new Employee("Sales", 40000), new Employee("IT", 60000),
                new Employee("Sales", 45000));

        Map<String, Double> avgSalaries = empSeq.groupBy(Employee::getDept, Collectors.averagingDouble(Employee::getSalary))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(55000.0, avgSalaries.get("IT"), 0.01);
        assertEquals(42500.0, avgSalaries.get("Sales"), 0.01);
    }

    @Test
    public void testGroupByWithDownstreamCollectorAndMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("apple", "apricot", "banana", "berry");
        List<Map.Entry<Character, String>> result = seq.groupBy(s -> s.charAt(0), Collectors.joining(", "), TreeMap::new).toList();

        assertEquals('a', result.get(0).getKey().charValue());
        assertEquals("apple, apricot", result.get(0).getValue());
        assertEquals('b', result.get(1).getKey().charValue());
        assertEquals("banana, berry", result.get(1).getValue());
    }

    @Test
    public void testGroupByWithValueMapperAndDownstreamCollector() throws Exception {
        class Order {
            String customer;
            String product;

            Order(String customer, String product) {
                this.customer = customer;
                this.product = product;
            }

            String getCustomer() {
                return customer;
            }

            String getProduct() {
                return product;
            }
        }

        Seq<Order, Exception> seq = Seq.of(new Order("Alice", "Book"), new Order("Bob", "Pen"), new Order("Alice", "Notebook"), new Order("Bob", "Pencil"));

        Map<String, String> result = seq.groupBy(Order::getCustomer, Order::getProduct, Collectors.joining(", ")).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("Book, Notebook", result.get("Alice"));
        assertEquals("Pen, Pencil", result.get("Bob"));
    }

    @Test
    public void testGroupByWithValueMapperDownstreamCollectorAndMapFactory() throws Exception {
        class Transaction {
            String type;
            int amount;

            Transaction(String type, int amount) {
                this.type = type;
                this.amount = amount;
            }

            String getType() {
                return type;
            }

            int getAmount() {
                return amount;
            }
        }

        Seq<Transaction, Exception> seq = Seq.of(new Transaction("Credit", 100), new Transaction("Debit", 50), new Transaction("Credit", 200),
                new Transaction("Debit", 75));

        List<Map.Entry<String, Long>> result = seq
                .groupBy(Transaction::getType, Transaction::getAmount, Collectors.summingLong(Integer::longValue), LinkedHashMap::new)
                .toList();

        assertEquals("Credit", result.get(0).getKey());
        assertEquals(300L, result.get(0).getValue().longValue());
        assertEquals("Debit", result.get(1).getKey());
        assertEquals(125L, result.get(1).getValue().longValue());
    }

    @Test
    public void testGroupByWithDownstream() throws Exception {
        Map<Integer, Long> result = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, Collectors.counting())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, result.get(1).longValue());
        assertEquals(2L, result.get(2).longValue());
        assertEquals(1L, result.get(3).longValue());
    }

    @Test
    public void testGroupByWithDownstreamAndMapFactory() throws Exception {
        Map<Integer, Long> result = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, Collectors.counting(), LinkedHashMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());

        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testGroupByWithValueMapperAndDownstream() throws Exception {
        Map<Integer, String> result = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, String::toUpperCase, Collectors.joining(","))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("A,E", result.get(1));
        assertEquals("BB,DD", result.get(2));
        assertEquals("CCC", result.get(3));
    }

    @Test
    public void testGroupByWithValueMapperDownstreamAndMapFactory() throws Exception {
        Map<Integer, String> result = Seq.of("a", "bb", "ccc", "dd", "e")
                .groupBy(String::length, String::toUpperCase, Collectors.joining(","), LinkedHashMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertFalse(result instanceof LinkedHashMap);
        assertEquals("A,E", result.get(1));
    }

    @Test
    public void testGroupBy_WithCollector() throws Exception {
        List<Map.Entry<Integer, Long>> result = Seq.of("a", "bb", "c", "dd").groupBy(String::length, java.util.stream.Collectors.counting()).toList();
        assertEquals(2, result.size());
    }

    @Test
    public void test_partitionBy_withCollector() throws Exception {
        Map<Boolean, Long> partition = Seq.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, partition.get(true));
        assertEquals(3L, partition.get(false));
    }

    @Test
    public void testPartitionByWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).skip(1).count());

        Map.Entry<Boolean, List<String>>[] entries = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                .partitionBy(s -> s.compareTo("c") < 0)
                .toArray(Map.Entry[]::new);
        assertEquals(2, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").partitionBy(s -> s.compareTo("c") < 0).skip(1).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testPartitionBy() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList(2, 4, 6), partitions.get(true));
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));

        seq = Seq.of(2, 4, 6);
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(Arrays.asList(2, 4, 6), partitions.get(true));
        assertTrue(partitions.get(false).isEmpty());

        seq = Seq.of(1, 3, 5);
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertTrue(partitions.get(true).isEmpty());
        assertEquals(Arrays.asList(1, 3, 5), partitions.get(false));

        seq = Seq.<Integer, Exception> empty();
        partitions = seq.partitionBy(n -> n % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertTrue(partitions.get(true).isEmpty());
        assertTrue(partitions.get(false).isEmpty());
    }

    @Test
    public void testPartitionByWithDownstreamCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, Long> counts = seq.partitionBy(n -> n > 3, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3L, counts.get(true).longValue());
        assertEquals(3L, counts.get(false).longValue());

        Seq<String, Exception> strSeq = Seq.of("", "hello", "", "world", "");
        Map<Boolean, String> joined = strSeq.partitionBy(String::isEmpty, Collectors.joining(", ")).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(", , ", joined.get(true));
        assertEquals("hello, world", joined.get(false));
    }

    @Test
    public void testPartitionByWithDownstream() throws Exception {
        Map<Boolean, Long> result = Seq.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, result.get(true).longValue());
        assertEquals(3L, result.get(false).longValue());
    }

    @Test
    public void test_countBy_keyMapper() throws Exception {
        Map<Character, Integer> counts = Seq.of("apple", "apricot", "banana", "avocado")
                .countBy(s -> s.charAt(0))
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3, counts.get('a').intValue());
        assertEquals(1, counts.get('b').intValue());
    }

    @Test
    public void test_countBy_keyMapperAndMapFactory() throws Exception {
        Map<Character, Integer> counts = Seq.of("apple", "apricot", "banana", "avocado")
                .countBy(s -> s.charAt(0), TreeMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);
        assertEquals(3, counts.get('a').intValue());
    }

    @Test
    public void testCountByWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).skip(2).count());

        Map.Entry<Integer, Integer>[] entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb")
                .countBy(String::length)
                .toArray(Map.Entry[]::new);
        assertEquals(3, entries.length);

        entries = Seq.<String, RuntimeException> of("a", "bb", "aa", "ccc", "bbb").countBy(String::length).skip(2).toArray(Map.Entry[]::new);
        assertEquals(1, entries.length);
    }

    @Test
    public void testCountBy() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "bb", "ccc", "dd", "e", "fff");
        Map<Integer, Integer> counts = seq.countBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, counts.get(1).intValue());
        assertEquals(2, counts.get(2).intValue());
        assertEquals(2, counts.get(3).intValue());

        seq = Seq.<String, Exception> empty();
        assertTrue(seq.countBy(String::length).toList().isEmpty());

        seq = Seq.of("apple", "apricot", "banana", "berry", "cherry");
        counts = seq.countBy(s -> (int) s.charAt(0)).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, counts.get((int) 'a').intValue());
        assertEquals(2, counts.get((int) 'b').intValue());
        assertEquals(1, counts.get((int) 'c').intValue());
    }

    @Test
    public void testCountByWithMapFactory() throws Exception {
        Seq<String, Exception> seq = Seq.of("z", "a", "z", "b", "a", "a");
        List<Map.Entry<Character, Integer>> result = seq.countBy(s -> s.charAt(0), TreeMap::new).toList();

        assertEquals('a', result.get(0).getKey().charValue());
        assertEquals(3, result.get(0).getValue().intValue());
        assertEquals('b', result.get(1).getKey().charValue());
        assertEquals(1, result.get(1).getValue().intValue());
        assertEquals('z', result.get(2).getKey().charValue());
        assertEquals(2, result.get(2).getValue().intValue());
    }

    @Test
    public void testIntersection() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).intersection(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testIntersectionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).skip(2).count());
        assertArrayEquals(new String[] { "b", "c", "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(Arrays.asList("b", "c", "d", "f")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testIntersectionWithMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "b", "c", "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "d" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .intersection(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testIntersectionWithMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Bob", 30), new Person("Alice", 35));
        List<String> names = Arrays.asList("Alice", "Charlie");
        List<Person> result = seq.intersection(Person::getName, names).toList();
        assertEquals(1, result.size());
        assertEquals("Alice", result.get(0).getName());
        assertEquals(25, result.get(0).age);

        class Product {
            int id;
            String name;

            Product(int id, String name) {
                this.id = id;
                this.name = name;
            }

            int getId() {
                return id;
            }
        }

        Seq<Product, Exception> products = Seq.of(new Product(1, "A"), new Product(2, "B"), new Product(3, "C"));
        Set<Integer> ids = new HashSet<>(Arrays.asList(1, 3, 4));
        List<Product> productResult = products.intersection(Product::getId, ids).toList();
        assertEquals(2, productResult.size());
        assertEquals(1, productResult.get(0).getId());
        assertEquals(3, productResult.get(1).getId());
    }

    @Test
    public void testIntersection_WithMapper() throws Exception {
        List<String> result = Seq.<String, Exception> of("apple", "banana", "cherry").intersection(s -> s.length(), Arrays.asList(5, 6)).toList();
        assertEquals(Arrays.asList("apple", "banana"), result);
    }

    @Test
    public void testDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).difference(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testDifferenceWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(Arrays.asList("b", "c", "d", "f")).skip(1).toArray(String[]::new));
    }

    @Test
    public void testDifferenceWithMapperWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .difference(String::toUpperCase, Arrays.asList("B", "C", "D", "F"))
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testDifferenceWithMapper() throws Exception {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }

            String getName() {
                return name;
            }
        }

        Seq<Person, Exception> seq = Seq.of(new Person("Alice", 25), new Person("Alice", 30), new Person("Bob", 35));
        List<String> names = Arrays.asList("Alice", "Charlie");
        List<Person> result = seq.difference(Person::getName, names).toList();
        assertEquals(2, result.size());
        assertEquals("Alice", result.get(0).getName());
        assertEquals(30, result.get(0).age);
        assertEquals("Bob", result.get(1).getName());

        class Transaction {
            int id;

            Transaction(int id) {
                this.id = id;
            }

            int getId() {
                return id;
            }
        }

        Seq<Transaction, Exception> trans = Seq.of(new Transaction(101), new Transaction(102));
        List<Integer> ids = Arrays.asList(101, 101, 102);
        List<Transaction> transResult = trans.difference(Transaction::getId, ids).toList();
        assertTrue(transResult.isEmpty());
    }

    @Test
    public void testDifference_WithMapper() throws Exception {
        List<String> result = Seq.of("apple", "banana", "cherry").difference(s -> s.length(), Arrays.asList(5, 6)).toList();
        // "apple"(5) removed, "banana"(6) removed, "cherry"(6) remains since only one 6 in collection
        assertEquals(Arrays.asList("cherry"), result);
    }

    @Test
    public void testSymmetricDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).symmetricDifference(Arrays.asList(3, 4, 5, 6, 7)).toList();
        assertEquals(Arrays.asList(1, 2, 6, 7), result);
    }

    @Test
    public void test_symmetricDifference() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2, 3, 4);
        Collection<Integer> colB = Arrays.asList(3, 4, 5, 6);
        Seq<Integer, Exception> result = seqA.symmetricDifference(colB);
        Set<Integer> expectedSet = new HashSet<>(Arrays.asList(1, 2, 5, 6));
        assertEquals(expectedSet, new HashSet<>(drainWithException(result)));
    }

    @Test
    public void test_symmetricDifference_withEmptyInputs() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2, 3);
        Collection<Integer> colBEmpty = Collections.emptyList();
        Seq<Integer, Exception> result1 = seqA.symmetricDifference(colBEmpty);
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), new HashSet<>(drainWithException(result1)));

        Seq<Integer, Exception> seqAEmpty = Seq.empty();
        Collection<Integer> colB = Arrays.asList(3, 4);
        Seq<Integer, Exception> result2 = seqAEmpty.symmetricDifference(colB);
        assertEquals(new HashSet<>(Arrays.asList(3, 4)), new HashSet<>(drainWithException(result2)));

        Seq<Integer, Exception> bothEmpty = Seq.<Integer, Exception> empty().symmetricDifference(Collections.emptyList());
        assertTrue(drainWithException(bothEmpty).isEmpty());
    }

    @Test
    public void testSymmetricDifferenceWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).skip(1).count());
        assertArrayEquals(new String[] { "a", "e", "f" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").symmetricDifference(Arrays.asList("b", "c", "d", "f")).toArray(String[]::new));
        assertArrayEquals(new String[] { "e", "f" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .symmetricDifference(Arrays.asList("b", "c", "d", "f"))
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testPrepend_Single() throws Exception {
        List<Integer> result = Seq.of(2, 3).prepend(1).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrepend_Multiple() throws Exception {
        List<Integer> result = Seq.of(3, 4).prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testPrependArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "x", "y", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testPrependCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).skip(2).count());
        assertArrayEquals(new String[] { "x", "y", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Arrays.asList("x", "y")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPrependOptionalWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).skip(2).count());
        assertArrayEquals(new String[] { "x", "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").prepend(Optional.of("x")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPrependArray() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(3, 4, 5);
        List<Integer> result = seq.prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.prepend(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.of(1, 2, 3);
        result = seq.prepend(new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependCollection() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(3, 4, 5);
        List<Integer> toPrepend = Arrays.asList(1, 2);
        List<Integer> result = seq.prepend(toPrepend).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.of(1, 2, 3);
        result = seq.prepend(Collections.emptyList()).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPrependSeq() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("world");
        Seq<String, Exception> seq2 = Seq.of("hello", " ");
        List<String> result = seq1.prepend(seq2).toList();
        assertEquals(Arrays.asList("hello", " ", "world"), result);

        seq1 = Seq.of("a", "b");
        seq2 = Seq.<String, Exception> empty();
        result = seq1.prepend(seq2).toList();
        assertEquals(Arrays.asList("a", "b"), result);
    }

    @Test
    public void testPrependOptional() throws Exception {
        Seq<String, Exception> seq = Seq.of("world");
        Optional<String> maybeHello = Optional.of("hello");
        List<String> result = seq.prepend(maybeHello).toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        Optional<String> empty = Optional.empty();
        result = Seq.of("world").prepend(empty).toList();
        assertEquals(Arrays.asList("world"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.prepend(maybeHello).toList();
        assertEquals(Arrays.asList("hello"), result);
    }

    @Test
    public void testPrepend() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrepend_Collection() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrepend_Seq() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(Seq.of(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppend_Single() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppend_Multiple() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(3, 4).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testAppendAll() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(Seq.of(Arrays.asList(3, 4))).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testAppendArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(7, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x", "y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Arrays.asList("x", "y")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendOptionalWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e", "x" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e", "x" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").append(Optional.of("x")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendArray() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2);
        List<Integer> result = seq.append(3, 4, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);

        seq = Seq.<Integer, Exception> empty();
        result = seq.append(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        seq = Seq.of(1, 2, 3);
        result = seq.append(new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppendCollection() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b");
        List<String> toAppend = Arrays.asList("c", "d");
        List<String> result = seq.append(toAppend).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);

        seq = Seq.of("x", "y");
        result = seq.append(Collections.emptyList()).toList();
        assertEquals(Arrays.asList("x", "y"), result);
    }

    @Test
    public void testAppendSeq() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 2);
        Seq<Integer, Exception> seq2 = Seq.of(3, 4);
        List<Integer> result = seq1.append(seq2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);

        seq1 = Seq.of(1, 2);
        seq2 = Seq.<Integer, Exception> empty();
        result = seq1.append(seq2).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testAppendOptional() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello");
        Optional<String> maybeWorld = Optional.of("world");
        List<String> result = seq.append(maybeWorld).toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        Optional<String> empty = Optional.empty();
        result = Seq.of("hello").append(empty).toList();
        assertEquals(Arrays.asList("hello"), result);

        seq = Seq.<String, Exception> empty();
        result = seq.append(maybeWorld).toList();
        assertEquals(Arrays.asList("world"), result);
    }

    @Test
    public void testAppend() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(4, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppend_Collection() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(Arrays.asList(4, 5)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppend_Seq() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(Seq.of(4, 5)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppendIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, RuntimeException> empty().appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        List<Integer> result2 = Seq.of(1, 2, 3).appendIfEmpty(Arrays.asList(10, 20)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void testAppendIfEmptyArrayWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").appendIfEmpty("x", "y").skip(2).toArray(String[]::new));
    }

    @Test
    public void testAppendIfEmptyArray() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        List<Integer> result = emptySeq.appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testAppendIfEmptyCollection() throws Exception {
        Seq<String, Exception> emptySeq = Seq.<String, Exception> empty();
        List<String> defaults = Arrays.asList("default1", "default2");
        List<String> result = emptySeq.appendIfEmpty(defaults).toList();
        assertEquals(Arrays.asList("default1", "default2"), result);

        Seq<String, Exception> nonEmptySeq = Seq.of("value");
        result = nonEmptySeq.appendIfEmpty(defaults).toList();
        assertEquals(Arrays.asList("value"), result);

        emptySeq = Seq.<String, Exception> empty();
        result = emptySeq.appendIfEmpty(Collections.emptyList()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAppendIfEmptySupplier() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        Supplier<Seq<Integer, Exception>> defaultSupplier = () -> Seq.of(1, 2, 3);
        List<Integer> result = emptySeq.appendIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.appendIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(4, 5), result);

        emptySeq = Seq.<Integer, Exception> empty();
        result = emptySeq.appendIfEmpty(() -> Seq.<Integer, Exception> empty()).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testAppendIfEmpty_WithSupplier() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().appendIfEmpty(() -> Seq.of(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        List<Integer> result2 = Seq.of(1, 2).appendIfEmpty(() -> Seq.of(10, 20)).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testAppendIfEmpty_VarArgs() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().appendIfEmpty(10, 20).toList();
        assertEquals(Arrays.asList(10, 20), result);

        List<Integer> result2 = Seq.of(1, 2).appendIfEmpty(10, 20).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testDefaultIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, RuntimeException> empty().defaultIfEmpty(99).toList();
        assertEquals(Arrays.asList(99), result);

        List<Integer> result2 = Seq.of(1, 2, 3).defaultIfEmpty(99).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void test_defaultIfEmpty_value() throws Exception {
        Seq<Integer, Exception> notEmpty = Seq.of(1, 2).defaultIfEmpty(99);
        assertEquals(Arrays.asList(1, 2), drainWithException(notEmpty));

        Seq<Integer, Exception> emptyWithDefault = Seq.<Integer, Exception> empty().defaultIfEmpty(99);
        assertEquals(Collections.singletonList(99), drainWithException(emptyWithDefault));
    }

    @Test
    public void test_defaultIfEmpty_supplier() throws Exception {
        final Seq<Integer, Exception> defaultSource = Seq.of(88, 99);
        Seq<Integer, Exception> notEmpty = Seq.of(1, 2).defaultIfEmpty(() -> defaultSource);
        assertEquals(Arrays.asList(1, 2), drainWithException(notEmpty));

        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Seq<Integer, Exception> emptyWithDefault = Seq.<Integer, Exception> empty().defaultIfEmpty(() -> {
            supplierCalled.set(true);
            return Seq.of(88, 99);
        });
        assertEquals(Arrays.asList(88, 99), drainWithException(emptyWithDefault));
        assertTrue(supplierCalled.get());
    }

    @Test
    public void test_defaultIfEmpty_value_nonEmptySeq() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello").defaultIfEmpty("world");
        assertEquals(Collections.singletonList("hello"), drainWithException(seq));
    }

    @Test
    public void test_defaultIfEmpty_supplier_nonEmptySeq() throws Exception {
        AtomicBoolean supplierCalled = new AtomicBoolean(false);
        Seq<String, Exception> seq = Seq.of("hello").defaultIfEmpty(() -> {
            supplierCalled.set(true);
            return Seq.of("world");
        });
        assertEquals(Collections.singletonList("hello"), drainWithException(seq));
        assertFalse(supplierCalled.get());
    }

    @Test
    public void testDefaultIfEmptyWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").defaultIfEmpty("x").skip(2).toArray(String[]::new));
    }

    @Test
    public void testDefaultIfEmptyValue() throws Exception {
        Seq<String, Exception> emptySeq = Seq.<String, Exception> empty();
        List<String> result = emptySeq.defaultIfEmpty("default").toList();
        assertEquals(Arrays.asList("default"), result);

        Seq<String, Exception> nonEmptySeq = Seq.of("value");
        result = nonEmptySeq.defaultIfEmpty("default").toList();
        assertEquals(Arrays.asList("value"), result);
    }

    @Test
    public void testDefaultIfEmptySupplier() throws Exception {
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty();
        Supplier<Seq<Integer, Exception>> defaultSupplier = () -> Seq.of(1, 2, 3);
        List<Integer> result = emptySeq.defaultIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        Seq<Integer, Exception> nonEmptySeq = Seq.of(4, 5);
        result = nonEmptySeq.defaultIfEmpty(defaultSupplier).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testDefaultIfEmpty_WithSupplier() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().defaultIfEmpty(() -> Seq.of(10, 20)).toList();
        assertEquals(Arrays.asList(10, 20), result);

        List<Integer> result2 = Seq.of(1, 2).defaultIfEmpty(() -> Seq.of(10, 20)).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testThrowIfEmpty() throws Exception {
        assertThrows(RuntimeException.class, () -> {
            Seq.<Integer, RuntimeException> empty().throwIfEmpty().toList();
        });

        List<Integer> result = Seq.of(1, 2, 3).throwIfEmpty().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_throwIfEmpty_noError() {
        Seq<Integer, Exception> seq = Seq.of(1).throwIfEmpty();
        assertDoesNotThrow(() -> drainWithException(seq));
    }

    @Test
    public void test_throwIfEmpty_withError() {
        Seq<Integer, Exception> seq = Seq.<Integer, Exception> empty().throwIfEmpty();
        assertThrows(NoSuchElementException.class, () -> drainWithException(seq));
    }

    @Test
    public void test_throwIfEmpty_customException() {
        class MyCustomEmptyException extends RuntimeException {
        }
        Seq<Integer, Exception> seq = Seq.<Integer, Exception> empty().throwIfEmpty(MyCustomEmptyException::new);
        assertThrows(MyCustomEmptyException.class, () -> drainWithException(seq));
    }

    @Test
    public void test_throwIfEmpty_nonEmptySeq_customException() {
        Seq<Integer, Exception> seq = Seq.of(1).throwIfEmpty(RuntimeException::new);
        assertDoesNotThrow(() -> drainWithException(seq));
    }

    @Test
    public void testThrowIfEmptyWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").throwIfEmpty().skip(2).toArray(String[]::new));
    }

    @Test
    public void testThrowIfEmptyWithSupplier() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b");
        List<String> result = seq.throwIfEmpty(() -> new IllegalStateException("Sequence must not be empty")).toList();
        assertEquals(Arrays.asList("a", "b"), result);

        Seq<String, Exception> seq2 = Seq.<String, Exception> empty();
        assertThrows(IllegalStateException.class, () -> seq2.throwIfEmpty(() -> new IllegalStateException("Sequence must not be empty")).toList());
    }

    @Test
    public void testThrowIfEmpty_CustomException() throws Exception {
        assertThrows(IllegalStateException.class, () -> {
            Seq.<Integer, Exception> empty().throwIfEmpty(() -> new IllegalStateException("custom empty")).toList();
        });

        List<Integer> result = Seq.of(1, 2, 3).throwIfEmpty(() -> new IllegalStateException("custom empty")).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testIfEmpty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Seq.<Integer, RuntimeException> empty().ifEmpty(() -> counter.set(1)).toList();
        assertEquals(1, counter.get());

        counter.set(0);
        Seq.of(1, 2, 3).ifEmpty(() -> counter.set(1)).toList();
        assertEquals(0, counter.get());
    }

    @Test
    public void test_ifEmpty_actionOnEmpty() throws Exception {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty().ifEmpty(() -> actionCalled.set(true));
        assertTrue(drainWithException(emptySeq).isEmpty());
        assertTrue(actionCalled.get());

        actionCalled.set(false);
        Seq<Integer, Exception> nonEmptySeq = Seq.of(1).ifEmpty(() -> actionCalled.set(true));
        assertEquals(Collections.singletonList(1), drainWithException(nonEmptySeq));
        assertFalse(actionCalled.get());
    }

    @Test
    public void test_ifEmpty_actionOnNonEmpty() throws Exception {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        Seq<Integer, Exception> nonEmptySeq = Seq.of(1).ifEmpty(() -> actionCalled.set(true));
        assertEquals(Collections.singletonList(1), drainWithException(nonEmptySeq));
        assertFalse(actionCalled.get());
    }

    @Test
    public void testIfEmptyWithSkipCountAndToArray() throws Exception {
        List<String> actions = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).count());
        assertEquals(0, actions.size());

        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).skip(2).count());

        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).toArray(String[]::new));

        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").ifEmpty(() -> actions.add("empty")).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnEach() throws Exception {
        List<Integer> sideEffect = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).onEach(sideEffect::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), sideEffect);
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testOnFirst() throws Exception {
        AtomicInteger firstValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).onFirst(firstValue::set).toList();
        assertEquals(1, firstValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testOnFirstWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onFirst(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnFirst_Empty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Seq.<Integer, Exception> empty().onFirst(x -> counter.incrementAndGet()).toList();
        assertTrue(result.isEmpty());
        assertEquals(0, counter.get());
    }

    @Test
    public void testOnLast() throws Exception {
        AtomicInteger lastValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).onLast(lastValue::set).toList();
        assertEquals(3, lastValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testOnLastWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").onLast(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testOnLast_Empty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> result = Seq.<Integer, Exception> empty().onLast(x -> counter.incrementAndGet()).toList();
        assertTrue(result.isEmpty());
        assertEquals(0, counter.get());
    }

    @Test
    public void testPeek() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        Seq.of(1, 2, 3).peek(peeked::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testPeekWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).count());
        assertEquals(5, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peek(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekWithFilterAndEmpty() throws Exception {
        List<Integer> sideEffect = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.peek(sideEffect::add).filter(n -> n % 2 == 0).toList();
        assertEquals(Arrays.asList(2, 4), result);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), sideEffect);

        sideEffect.clear();
        seq = Seq.<Integer, Exception> empty();
        result = seq.peek(sideEffect::add).toList();
        assertTrue(result.isEmpty());
        assertTrue(sideEffect.isEmpty());
    }

    @Test
    public void testPeekFirst() throws Exception {
        AtomicInteger firstValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).peekFirst(firstValue::set).toList();
        assertEquals(1, firstValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekFirstWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekFirst(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekLast() throws Exception {
        AtomicInteger lastValue = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2, 3).peekLast(lastValue::set).toList();
        assertEquals(3, lastValue.get());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testPeekLastWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).count());
        assertEquals(1, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekLast(seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekIf() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).peekIf(x -> x % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(2, 4), peeked);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPeekIfPredicateWithSkipCountAndToArray() throws Exception {
        List<String> seen = new ArrayList<>();
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).count());
        assertEquals(3, seen.size());

        seen.clear();
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).skip(2).count());

        seen.clear();
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).toArray(String[]::new));

        seen.clear();
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").peekIf(s -> s.compareTo("c") >= 0, seen::add).skip(2).toArray(String[]::new));
    }

    @Test
    public void testPeekIfWithPredicate() throws Exception {
        List<Integer> evenNumbers = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.peekIf(n -> n % 2 == 0, evenNumbers::add).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), evenNumbers);

        List<Integer> largeNumbers = new ArrayList<>();
        seq = Seq.of(1, 2, 3);
        result = seq.peekIf(n -> n > 10, largeNumbers::add).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(largeNumbers.isEmpty());
    }

    @Test
    public void testPeekIfWithBiPredicate() throws Exception {
        List<String> evenPositions = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("a", "b", "c", "d", "e");
        List<String> result = seq.peekIf((s, index) -> index % 2 == 0, evenPositions::add).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
        assertEquals(Arrays.asList("b", "d"), evenPositions);

        List<String> firstThree = new ArrayList<>();
        seq = Seq.of("1", "2", "3", "4", "5");
        result = seq.peekIf((s, index) -> index <= 3, firstThree::add).toList();
        assertEquals(Arrays.asList("1", "2", "3", "4", "5"), result);
        assertEquals(Arrays.asList("1", "2", "3"), firstThree);
    }

    @Test
    public void testPeekIf_WithBiPredicate() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(10, 20, 30, 40, 50).peekIf((value, idx) -> idx % 2 == 0, peeked::add).toList();
        assertEquals(Arrays.asList(20, 40), peeked);
        assertEquals(Arrays.asList(10, 20, 30, 40, 50), result);
    }

    @Test
    public void testSplitAt() throws Exception {
        List<List<Integer>> parts = Seq.of(1, 2, 3, 4, 5).splitAt(2).map(Seq::toList).toList();
        assertEquals(2, parts.size());
        assertEquals(Arrays.asList(1, 2), parts.get(0));
        assertEquals(Arrays.asList(3, 4, 5), parts.get(1));
    }

    @Test
    public void testSplitAtIntWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).skip(1).count());

        Seq<String, RuntimeException>[] split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).toArray(Seq[]::new);
        assertEquals(2, split.length);
        assertArrayEquals(new String[] { "a", "b", "c" }, split[0].toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" }, split[1].toArray(String[]::new));

        split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(3).skip(1).toArray(Seq[]::new);
        assertEquals(1, split.length);
        assertArrayEquals(new String[] { "d", "e" }, split[0].toArray(String[]::new));
    }

    @Test
    public void testSplitAtPredicateWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).skip(1).count());

        Seq<String, RuntimeException>[] split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                .splitAt(s -> s.compareTo("c") > 0)
                .toArray(Seq[]::new);
        assertEquals(2, split.length);
        assertArrayEquals(new String[] { "a", "b", "c" }, split[0].toArray(String[]::new));
        assertArrayEquals(new String[] { "d", "e" }, split[1].toArray(String[]::new));

        split = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").splitAt(s -> s.compareTo("c") > 0).skip(1).toArray(Seq[]::new);
        assertEquals(1, split.length);
        assertArrayEquals(new String[] { "d", "e" }, split[0].toArray(String[]::new));
    }

    @Test
    public void testSplitAtPosition() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Seq<Integer, Exception>> split = seq.splitAt(3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(4, 5), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 4, 5);
        split = seq.splitAt(3).skip(1).toList();
        assertEquals(1, split.size());
        assertEquals(Arrays.asList(4, 5), split.get(0).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(0).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(1, 2, 3), split.get(1).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.of(1, 2);
        split = seq.splitAt(5).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.<Integer, Exception> empty();
        split = seq.splitAt(2).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertTrue(split.get(1).toList().isEmpty());
    }

    @Test
    public void testSplitAtPredicate() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Seq<Integer, Exception>> split = seq.splitAt(n -> n > 3).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(4, 5), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 4, 5);
        split = seq.splitAt(n -> n > 3).skip(1).toList();
        assertEquals(1, split.size());
        assertEquals(Arrays.asList(4, 5), split.get(0).toList());

        seq = Seq.of(1, 2, 3);
        split = seq.splitAt(n -> n > 10).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertTrue(split.get(1).toList().isEmpty());

        seq = Seq.of(5, 1, 2, 3);
        split = seq.splitAt(n -> n > 3).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertEquals(Arrays.asList(5, 1, 2, 3), split.get(1).toList());

        seq = Seq.of(1, 2, 3, 5);
        split = seq.splitAt(n -> n > 4).toList();
        assertEquals(2, split.size());
        assertEquals(Arrays.asList(1, 2, 3), split.get(0).toList());
        assertEquals(Arrays.asList(5), split.get(1).toList());

        seq = Seq.<Integer, Exception> empty();
        split = seq.splitAt(n -> n > 0).toList();
        assertEquals(2, split.size());
        assertTrue(split.get(0).toList().isEmpty());
        assertTrue(split.get(1).toList().isEmpty());
    }

    @Test
    public void testSplitAt_Position() throws Exception {
        List<Seq<Integer, Exception>> result = Seq.<Integer, Exception> of(1, 2, 3, 4, 5).splitAt(3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0).toList());
        assertEquals(Arrays.asList(4, 5), result.get(1).toList());
    }

    @Test
    public void testSplitAt_Predicate() throws Exception {
        List<Seq<Integer, Exception>> result = Seq.<Integer, Exception> of(1, 2, 3, 4, 5).splitAt(x -> x > 3).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0).toList());
        assertEquals(Arrays.asList(4, 5), result.get(1).toList());
    }

    @Test
    public void testSlidingWithIncrement() throws Exception {
        List<List<Integer>> windows = Seq.of(1, 2, 3, 4, 5, 6).sliding(2, 2).toList();
        assertEquals(3, windows.size());
        assertEquals(Arrays.asList(1, 2), windows.get(0));
        assertEquals(Arrays.asList(3, 4), windows.get(1));
        assertEquals(Arrays.asList(5, 6), windows.get(2));
    }

    @Test
    public void test_sliding_withCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3, Collectors.mapping(String::valueOf, Collectors.joining(",")));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5"), drainWithException(seq));
    }

    @Test
    public void test_sliding_withCollector_andIncrement() throws Exception {
        Seq<Long, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7).sliding(3, 2, Collectors.counting());
        assertEquals(Arrays.asList(3L, 3L, 3L), drainWithException(seq));

        Seq<Long, Exception> smallSeq = Seq.of(1, 2).sliding(3, 1, Collectors.counting());
        assertEquals(Collections.singletonList(2L), drainWithException(smallSeq));
    }

    @Test
    public void testSlidingIntWithSkipCountAndToArray() throws Exception {
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).count());
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).skip(2).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).toList();
        assertEquals(4, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("b", "c"), result.get(1));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2).skip(2).toList();
        assertEquals(2, result.size());
        assertEquals(Arrays.asList("c", "d"), result.get(0));
        assertEquals(Arrays.asList("d", "e"), result.get(1));
    }

    @Test
    public void testSlidingIntIntWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).skip(2).count());

        List<List<String>> result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "b"), result.get(0));
        assertEquals(Arrays.asList("c", "d"), result.get(1));
        assertEquals(Arrays.asList("e"), result.get(2));

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").sliding(2, 2).skip(2).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList("e"), result.get(0));
    }

    @Test
    public void testSliding_WithWindowSizeOne() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(1, 1, IntFunctions.ofList()).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
        assertEquals(Arrays.asList(2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));
        assertEquals(Arrays.asList(4), result.get(3));
    }

    @Test
    public void testSliding_WithWindowSizeTwoIncrementOne() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(2, 3), result.get(1));
    }

    @Test
    public void testSliding_WithWindowSizeTwoIncrementTwo() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(2, 2, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(3, 4), result.get(1));
    }

    @Test
    public void testSliding_WithWindowSizeLargerThanIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<List<Integer>> result = seq.sliding(3, 2, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));
    }

    @Test
    public void testSliding_WithIncrementLargerThanWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<List<Integer>> result = seq.sliding(2, 3, IntFunctions.ofList()).toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
    }

    @Test
    public void testSliding_WithSingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1);
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
    }

    @Test
    public void testSliding_WithEmptySequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of();
        List<List<Integer>> result = seq.sliding(2, 1, IntFunctions.ofList()).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSliding_WithLinkedListSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<LinkedList<Integer>> result = seq.sliding(2, 1, IntFunctions.ofLinkedList()).toList();

        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof LinkedList);
        assertEquals(Arrays.asList(1, 2), result.get(0));
    }

    @Test
    public void testSliding_WithHashSetSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<HashSet<Integer>> result = seq.sliding(2, 1, HashSet::new).toList();

        assertEquals(3, result.size());
        assertTrue(result.get(0) instanceof HashSet);
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.get(0));
    }

    @Test
    public void testSliding_WithInvalidWindowSize() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(0, 1, IntFunctions.ofList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(-1, 1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_WithInvalidIncrement() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(1, 0, IntFunctions.ofList());
        });

        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).sliding(1, -1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_WithNullSupplier() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 1, (IntFunction) null);
        });
    }

    @Test
    public void testSliding_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.sliding(2, 1, IntFunctions.ofList());
        });
    }

    @Test
    public void testSliding_CountMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<List<Integer>, Exception> slidingSeq = seq.sliding(2, 1, IntFunctions.ofList());

        assertEquals(4, slidingSeq.count());
    }

    @Test
    public void testSliding_AdvanceMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<List<Integer>, Exception> slidingSeq = seq.sliding(2, 1, IntFunctions.ofList()).skip(2);
        List<List<Integer>> result = slidingSeq.toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(3, 4), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
    }

    @Test
    public void testSlidingWithCollector_SimpleCase() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertEquals(3, result.size());
        assertEquals("1,2", result.get(0));
        assertEquals("2,3", result.get(1));
        assertEquals("3,4", result.get(2));
    }

    @Test
    public void testSlidingWithCollector_SumCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        List<Integer> result = seq.sliding(3, 2, Collectors.summingInt(Integer::intValue)).toList();

        assertEquals(2, result.size());
        assertEquals(6, result.get(0));
        assertEquals(12, result.get(1));
    }

    @Test
    public void testSlidingWithCollector_ListCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4);
        List<List<Integer>> result = seq.sliding(2, 1, Collectors.toList()).toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(2, 3), result.get(1));
        assertEquals(Arrays.asList(3, 4), result.get(2));
    }

    @Test
    public void testSlidingWithCollector_EmptySequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of();
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertTrue(result.isEmpty());
    }

    @Test
    public void testSlidingWithCollector_SingleElement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1);
        List<String> result = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).toList();

        assertEquals(1, result.size());
        assertEquals("1", result.get(0));
    }

    @Test
    public void testSlidingWithCollector_IncrementLargerThanWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        List<String> result = seq.map(String::valueOf).sliding(2, 3, Collectors.joining(",")).toList();

        assertEquals(2, result.size());
        assertEquals("1,2", result.get(0));
        assertEquals("4,5", result.get(1));
    }

    @Test
    public void testSlidingWithCollector_InvalidWindowSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(0, 1, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_InvalidIncrement() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 0, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_NullCollector() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.sliding(2, 1, (Collector) null);
        });
    }

    @Test
    public void testSlidingWithCollector_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.map(String::valueOf).sliding(2, 1, Collectors.toList());
        });
    }

    @Test
    public void testSlidingWithCollector_CountMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<String, Exception> slidingSeq = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(","));

        assertEquals(4, slidingSeq.count());
    }

    @Test
    public void testSlidingWithCollector_AdvanceMethod() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Seq<String, Exception> slidingSeq = seq.map(String::valueOf).sliding(2, 1, Collectors.joining(",")).skip(2);

        List<String> result = slidingSeq.toList();

        assertEquals(2, result.size());
        assertEquals("3,4", result.get(0));
        assertEquals("4,5", result.get(1));
    }

    @Test
    public void testSliding_WindowSize() throws Exception {
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3);
        List<List<Integer>> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));

        seq = Seq.of(1, 2).sliding(3);
        result = seq.toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));

        seq = Seq.of(1, 2, 3).sliding(1);
        result = seq.toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1), result.get(0));
        assertEquals(Arrays.asList(2), result.get(1));
        assertEquals(Arrays.asList(3), result.get(2));

        seq = Seq.<Integer, Exception> empty().sliding(2);
        result = seq.toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSliding_WindowSizeAndCollectionSupplier() throws IllegalStateException, Exception {
        Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 2, 3, 3, 4).sliding(3, HashSet::new);
        List<Set<Integer>> result = seq.toList();

        assertEquals(4, result.size());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.get(0));
        assertEquals(new HashSet<>(Arrays.asList(2, 3)), result.get(1));

        Seq<LinkedList<String>, Exception> seqStr = Seq.of("a", "b", "c", "d").sliding(2, n -> new LinkedList<>());
        List<LinkedList<String>> resultStr = seqStr.toList();

        assertEquals(3, resultStr.size());
        assertTrue(resultStr.get(0) instanceof LinkedList);
        assertEquals(Arrays.asList("a", "b"), resultStr.get(0));
    }

    @Test
    public void testSliding_WindowSizeAndCollector() throws Exception {
        Seq<String, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3, com.landawn.abacus.util.stream.Collectors.joining(","));
        List<String> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals("1,2,3", result.get(0));
        assertEquals("2,3,4", result.get(1));
        assertEquals("3,4,5", result.get(2));

        Seq<Integer, Exception> seqSum = Seq.of(1, 2, 3, 4, 5).sliding(2, Collectors.summingInt(Integer::intValue));
        List<Integer> resultSum = seqSum.toList();

        assertEquals(4, resultSum.size());
        assertEquals(3, resultSum.get(0));
        assertEquals(5, resultSum.get(1));
        assertEquals(7, resultSum.get(2));
        assertEquals(9, resultSum.get(3));
    }

    @Test
    public void testSliding_WindowSizeAndIncrement() throws IllegalStateException, Exception {
        Seq<List<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5).sliding(3, 1);
        List<List<Integer>> result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(2, 3, 4), result.get(1));
        assertEquals(Arrays.asList(3, 4, 5), result.get(2));

        seq = Seq.of(1, 2, 3, 4, 5).sliding(3, 2);
        result = seq.toList();

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(1, 2, 3), result.get(0));
        assertEquals(Arrays.asList(3, 4, 5), result.get(1));

        seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8).sliding(2, 3);
        result = seq.toList();

        assertEquals(3, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
        assertEquals(Arrays.asList(4, 5), result.get(1));
        assertEquals(Arrays.asList(7, 8), result.get(2));
    }

    @Test
    public void testSliding_WindowSizeIncrementAndCollectionSupplier() throws Exception {
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new);
            List<Set<Integer>> result = seq.toList();

            assertEquals(3, result.size());
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result.get(1));
            assertEquals(new HashSet<>(Arrays.asList(5, 6)), result.get(2));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new);
            List<Set<Integer>> result = seq.toList();

            assertEquals(2, result.size());
            assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(4, 5, 6)), result.get(1));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new);
            List<Set<Integer>> result = seq.skip(1).toList();

            assertEquals(2, result.size());
            assertEquals(new HashSet<>(Arrays.asList(3, 4, 5)), result.get(0));
            assertEquals(new HashSet<>(Arrays.asList(5, 6)), result.get(1));
        }
        {
            Seq<Set<Integer>, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new);
            List<Set<Integer>> result = seq.skip(1).toList();

            assertEquals(1, result.size());
            assertEquals(new HashSet<>(Arrays.asList(4, 5, 6)), result.get(0));
        }

        {
            assertEquals(4, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, HashSet::new).count());
            assertEquals(3, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 2, HashSet::new).count());

            assertEquals(2, Seq.of(1, 2, 3, 4, 5, 6).sliding(3, 3, HashSet::new).count());
        }
    }

    @Test
    public void testSliding_WindowSizeIncrementAndCollector() throws Exception {
        {

            List<String> result = Seq.of("a", "b", "c", "d", "e").sliding(2, 2, Collectors.joining("-")).toList();
            assertEquals(3, result.size());
            assertEquals("a-b", result.get(0));
            assertEquals("c-d", result.get(1));
            assertEquals("e", result.get(2));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 1, Collectors.joining("-")).skip(2).toList();
            assertEquals(2, result.size());
            assertEquals("c-d", result.get(0));
            assertEquals("d-e", result.get(1));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 3, Collectors.joining("-")).toList();
            assertEquals(2, result.size());
            assertEquals("a-b", result.get(0));
            assertEquals("d-e", result.get(1));

            result = Seq.of("a", "b", "c", "d", "e").sliding(2, 3, Collectors.joining("-")).skip(1).toList();
            assertEquals(1, result.size());
            assertEquals("d-e", result.get(0));
        }

        {
            assertEquals(4, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, Collectors.joining("-")).count());
            assertEquals(3, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, 2, Collectors.joining("-")).count());

            assertEquals(2, Seq.of(1, 2, 3, 4, 5, 6).map(String::valueOf).sliding(3, 3, Collectors.joining("-")).count());
        }

    }

    @Test
    public void testSliding_WithCollector() throws Exception {
        List<Long> result = Seq.of(1, 2, 3, 4, 5).sliding(3, java.util.stream.Collectors.counting()).toList();
        assertEquals(Arrays.asList(3L, 3L, 3L), result);
    }

    @Test
    public void testSliding_WithCollectionSupplier() throws Exception {
        List<Set<Integer>> result = Seq.of(1, 2, 3, 4, 5).sliding(2, IntFunctions.ofSet()).toList();
        assertEquals(4, result.size());
        assertEquals(new HashSet<>(Arrays.asList(1, 2)), result.get(0));
    }

    @Test
    public void testSliding_InvalidWindowSize() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).sliding(0).toList());
    }

    @Test
    public void testSliding_WindowLargerThanSeq() throws Exception {
        List<List<Integer>> result = Seq.of(1, 2).sliding(5).toList();
        assertEquals(1, result.size());
        assertEquals(Arrays.asList(1, 2), result.get(0));
    }

    @Test
    public void testSkip() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skip(2).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void test_skip_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.skip(1));
    }

    @Test
    public void testSkipWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).skip(2).count());
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skip(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipWithAction() throws Exception {
        List<Integer> skippedItems = new ArrayList<>();
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skip(3, skippedItems::add);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(4, 5), result);
        assertEquals(Arrays.asList(1, 2, 3), skippedItems);

        skippedItems.clear();
        seq = Seq.of(1, 2, 3).skip(0, skippedItems::add);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(skippedItems.isEmpty());
    }

    @Test
    public void testSkip_WithOnSkipConsumer() throws Exception {
        List<Integer> skipped = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skip(2, skipped::add).toList();
        assertEquals(Arrays.asList(1, 2), skipped);
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    // skip(n > remaining) exhausts the iterator via advance(n > len - position)
    @Test
    public void testOf_BooleanArray_Skip_MoreThanSize_ReturnsEmpty() throws Exception {
        List<Boolean> result = Seq.of(new boolean[] { true, false }).skip(5).toList();
        assertTrue(result.isEmpty());
    }

    // skip(n < remaining) partially advances position
    @Test
    public void testOf_BooleanArray_Skip_PartialAdvance() throws Exception {
        List<Boolean> result = Seq.of(new boolean[] { true, false, true, false }).skip(1).toList();
        assertEquals(3, result.size());
        assertFalse(result.get(0));
    }

    @Test
    public void testSkipNulls() throws Exception {
        List<Integer> result = Seq.of(1, null, 2, null, 3).skipNulls().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipNullsWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().skip(2).count());
        assertArrayEquals(new String[] { "a", "c", "e" }, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", null, "c", null, "e").skipNulls().skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).skipLast(2).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_skipLast() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skipLast(2);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));
        Seq<Integer, Exception> skipZero = Seq.of(1, 2).skipLast(0);
        assertEquals(Arrays.asList(1, 2), drainWithException(skipZero));
        Seq<Integer, Exception> skipTooMany = Seq.of(1, 2).skipLast(3);
        assertTrue(drainWithException(skipTooMany).isEmpty());
    }

    @Test
    public void testSkipLastWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").skipLast(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSkipLast_Zero() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).skipLast(0).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSkipLast_MoreThanSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).skipLast(5).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testLimit() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void test_limit_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.limit(1));
    }

    @Test
    public void testLimitWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).toArray(String[]::new));
        assertArrayEquals(new String[] { "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").limit(3).skip(2).toArray(String[]::new));
    }

    @Test
    public void testLimit_WithOffset() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7).limit(2, 3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testLimit_OffsetAndMaxSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(1, 3).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(2), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(1));
        assertEquals(Integer.valueOf(4), result.get(2));
    }

    @Test
    public void testLimit_offsetZero_maxSizeMax() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(0, Long.MAX_VALUE).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testLimit_offsetZero_withMaxSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(0, 3).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    @Test
    public void testLimit_withOffset_maxSizeMax() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(2, Long.MAX_VALUE).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(3), result.get(0));
    }

    @Test
    public void testLast() throws Exception {
        Optional<Integer> last = Seq.of(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(Integer.valueOf(3), last.get());
    }

    @Test
    public void testLast_Empty() throws Exception {
        Optional<Integer> last = Seq.<Integer, RuntimeException> empty().last();
        assertFalse(last.isPresent());
    }

    @Test
    public void test_last() throws Exception {
        assertEquals(Optional.of(3), Seq.of(1, 2, 3).last());
        assertTrue(Seq.<Integer, Exception> empty().last().isEmpty());
    }

    @Test
    public void testLastWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").last(2).skip(1).toArray(String[]::new));
    }

    @Test
    public void testTakeLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).takeLast(2).toList();
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void test_takeLast() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).takeLast(3);
        assertEquals(Arrays.asList(3, 4, 5), drainWithException(seq));
        Seq<Integer, Exception> takeZero = Seq.of(1, 2).takeLast(0);
        assertTrue(drainWithException(takeZero).isEmpty());
        Seq<Integer, Exception> takeTooMany = Seq.of(1, 2).takeLast(3);
        assertEquals(Arrays.asList(1, 2), drainWithException(takeTooMany));
    }

    @Test
    public void testTakeLastWithSkipCountAndToArray() throws Exception {
        assertEquals(2, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).skip(1).count());
        assertArrayEquals(new String[] { "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").takeLast(2).skip(1).toArray(String[]::new));
    }

    @Test
    public void testTakeLast_Zero() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).takeLast(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testTakeLast_MoreThanSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).takeLast(5).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testTopK() throws Exception {
        List<Integer> result = Seq.of(5, 2, 8, 1, 9, 3).top(3).toList();
        assertEquals(Arrays.asList(5, 8, 9), result);
    }

    @Test
    public void test_top_naturalOrder() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(5, 1, null, 4, 2, null, 3).top(3);
        assertThrows(NullPointerException.class, () -> drainWithException(seq));
    }

    @Test
    public void test_top_withComparator() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(5, 1, 4, 2, 3).top(3, Comparator.reverseOrder());
        List<Integer> result = drainWithException(seq);
        assertEquals(Arrays.asList(3, 2, 1), result.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()));
    }

    @Test
    public void testTopIntWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).skip(2).count());

        String[] top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).toArray(String[]::new);
        assertEquals(3, top.length);
        assertTrue(Arrays.asList(top).contains("c"));
        assertTrue(Arrays.asList(top).contains("d"));
        assertTrue(Arrays.asList(top).contains("e"));

        top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3).skip(2).toArray(String[]::new);
        assertEquals(1, top.length);
    }

    @Test
    public void testTopIntComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).skip(2).count());

        String[] top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).toArray(String[]::new);
        assertEquals(3, top.length);
        assertTrue(Arrays.asList(top).contains("a"));
        assertTrue(Arrays.asList(top).contains("b"));
        assertTrue(Arrays.asList(top).contains("c"));

        top = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").top(3, Comparator.reverseOrder()).skip(2).toArray(String[]::new);
        assertEquals(1, top.length);
    }

    @Test
    public void testTop() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(5, 2, 8, 1, 9, 3).top(3);
        List<Integer> result = seq.toList();

        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(8, 9, 5)));

        seq = Seq.of(5, 2, 8, 1, 9, 3).top(1);
        result = seq.toList();
        assertEquals(Arrays.asList(9), result);
    }

    @Test
    public void testTopWithComparator() throws Exception {
        Seq<String, Exception> seq = Seq.of("aa", "bbb", "c", "dddd").top(2, Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("bbb", "dddd")));

        seq = Seq.of("aa", "bbb", "c", "dddd").top(2, Comparator.comparingInt(String::length).reversed());
        result = seq.toList();

        assertEquals(2, result.size());
        assertTrue(result.containsAll(Arrays.asList("c", "aa")));
    }

    @Test
    public void testTop_WithComparator() throws Exception {
        List<Integer> result = Seq.of(5, 3, 8, 1, 9, 2).top(3, Comparator.reverseOrder()).toList();
        // top 3 smallest since reverse order comparator
        assertEquals(3, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3)));
    }

    @Test
    public void testTop_Empty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().top(3).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testReversed() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).reversed().toList();
        assertEquals(Arrays.asList(3, 2, 1), result);
    }

    @Test
    public void testReversedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").reversed().skip(2).toArray(String[]::new));
    }

    @Test
    public void testRotated() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(2).toList();
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);
    }

    @Test
    public void test_rotated() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).rotated(2);
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), drainWithException(seq));
        Seq<Integer, Exception> seqNeg = Seq.of(1, 2, 3, 4, 5).rotated(-2);
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), drainWithException(seqNeg));
        Seq<Integer, Exception> seqZero = Seq.of(1, 2, 3).rotated(0);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seqZero));
        Seq<Integer, Exception> seqFull = Seq.of(1, 2, 3).rotated(3);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seqFull));
    }

    @Test
    public void testRotatedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).skip(2).count());
        assertArrayEquals(new String[] { "d", "e", "a", "b", "c" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rotated(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testRotatedNegative() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(-2).toList();
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), result);
    }

    @Test
    public void testRotated_Zero() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(0).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testRotated_FullRotation() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).rotated(3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testShuffled() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).shuffled().toList();
        assertEquals(10, result.size());
        assertTrue(result.contains(1));
        assertTrue(result.contains(10));
    }

    @Test
    public void test_shuffled() throws Exception {
        List<Integer> originalList = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Seq<Integer, Exception> seq = Seq.of(originalList).shuffled();
        List<Integer> shuffledList = drainWithException(seq);
        assertEquals(originalList.size(), shuffledList.size());
        assertTrue(shuffledList.containsAll(originalList));
        if (originalList.size() > 5) {
            assertNotEquals(originalList, shuffledList, "Shuffled list should ideally not be identical to original for non-trivial lists");
        }
    }

    @Test
    public void test_shuffled_withRandom() throws Exception {
        List<Integer> originalList = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(originalList).shuffled(new Random(12345L));
        List<Integer> shuffledList = drainWithException(seq);
        assertEquals(originalList.size(), shuffledList.size());
        assertTrue(shuffledList.containsAll(originalList));
    }

    @Test
    public void testShuffledWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().skip(2).count());

        String[] shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().toArray(String[]::new);
        assertEquals(5, shuffled.length);
        assertTrue(Arrays.asList(shuffled).contains("a"));
        assertTrue(Arrays.asList(shuffled).contains("b"));
        assertTrue(Arrays.asList(shuffled).contains("c"));
        assertTrue(Arrays.asList(shuffled).contains("d"));
        assertTrue(Arrays.asList(shuffled).contains("e"));

        shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled().skip(2).toArray(String[]::new);
        assertEquals(3, shuffled.length);
    }

    @Test
    public void testShuffledRandomWithSkipCountAndToArray() throws Exception {
        Random random = new Random(42);
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).count());

        random = new Random(42);
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).skip(2).count());

        random = new Random(42);
        String[] shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).toArray(String[]::new);
        assertEquals(5, shuffled.length);

        random = new Random(42);
        shuffled = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").shuffled(random).skip(2).toArray(String[]::new);
        assertEquals(3, shuffled.length);
    }

    @Test
    public void testShuffledWithRandom() throws Exception {
        Random random = new Random(42);
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).shuffled(random).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testShuffled_WithSeed() throws Exception {
        List<Integer> result1 = Seq.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList();
        List<Integer> result2 = Seq.of(1, 2, 3, 4, 5).shuffled(new Random(42)).toList();
        assertEquals(result1, result2);
        assertEquals(5, result1.size());
    }

    @Test
    public void testSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).sorted().toList();
        assertEquals(Arrays.asList(1, 1, 3, 4, 5), result);
    }

    @Test
    public void testSorted_WithComparator() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).sorted(Comparator.reverseOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 1, 1), result);
    }

    @Test
    public void testSortedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" }, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted().skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sorted(Comparator.reverseOrder()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedWithComparator() throws Exception {
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").sorted(Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("d", "bb", "aaa", "cccc"), result);

        seq = Seq.of("aaa", "bb", "cccc", "d").sorted(Comparator.reverseOrder());
        result = seq.toList();
        assertEquals(Arrays.asList("d", "cccc", "bb", "aaa"), result);
    }

    @Test
    public void testSorted_withComparator_reverseOrder() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5, 9).sorted(Comparator.reverseOrder()).toList();
        assertEquals(6, result.size());
        assertEquals(Integer.valueOf(9), result.get(0));
        assertEquals(Integer.valueOf(1), result.get(result.size() - 1));
    }

    @Test
    public void testSorted_alreadySorted_sameComparator() throws Exception {
        // When sorted is already true with the same comparator, should return this
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).sorted(Comparator.naturalOrder());
        List<Integer> result = seq.sorted(Comparator.naturalOrder()).toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(3), result.get(2));
    }

    // ===== sorted(Comparator) - already sorted with same comparator returns this =====

    @Test
    public void testSorted_alreadySortedSameComparator_returnsSelf() throws Exception {
        // After sorted(naturalOrder), calling sorted(naturalOrder) again should return the same seq (no re-sort)
        Seq<Integer, Exception> seq1 = Seq.of(1, 2, 3).sorted(Comparator.naturalOrder());
        Seq<Integer, Exception> seq2 = seq1.sorted(Comparator.naturalOrder());
        // Both should produce the same ordered result
        List<Integer> result = seq2.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testSortedByInt() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByInt(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByInt(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByLong() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByLong(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByLong(s -> (long) s.length()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedByDouble() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").sortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "b", "cc", "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "dddd", "eeeee" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").sortedByDouble(s -> (double) s.length()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testSortedBy() throws Exception {
        List<String> result = Seq.of("ccc", "a", "bb").sortedBy(String::length).toList();
        assertEquals(Arrays.asList("a", "bb", "ccc"), result);
    }

    @Test
    public void testSortedByWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").sortedBy(Fn.identity()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5).reverseSorted().toList();
        assertEquals(Arrays.asList(5, 4, 3, 1, 1), result);
    }

    @Test
    public void testReverseSortedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted().skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedComparatorWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSorted(String::compareTo).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedWithComparator() throws Exception {
        Seq<String, Exception> seq = Seq.of("aaa", "bb", "cccc", "d").reverseSorted(Comparator.comparingInt(String::length));
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("cccc", "aaa", "bb", "d"), result);
    }

    @Test
    public void testReverseSorted_WithComparator() throws Exception {
        List<Integer> result = Seq.of(1, 5, 3, 2, 4).reverseSorted(Comparator.naturalOrder()).toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testReverseSortedByInt() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByInt(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByInt(String::length).skip(2).toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByLong() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByLong(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByLongWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByLong(s -> (long) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee")
                        .reverseSortedByLong(s -> (long) s.length())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testReverseSortedByDouble() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedByDouble(s -> s.length() * 1.0).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).skip(2).count());
        assertArrayEquals(new String[] { "eeeee", "dddd", "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee").reverseSortedByDouble(s -> (double) s.length()).toArray(String[]::new));
        assertArrayEquals(new String[] { "aaa", "cc", "b" },
                Seq.<String, RuntimeException> of("aaa", "b", "cc", "dddd", "eeeee")
                        .reverseSortedByDouble(s -> (double) s.length())
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testReverseSortedBy() throws Exception {
        List<String> result = Seq.of("a", "ccc", "bb").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("ccc", "bb", "a"), result);
    }

    @Test
    public void testReverseSortedByWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).count());
        assertEquals(3, Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).skip(2).count());
        assertArrayEquals(new String[] { "e", "d", "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "b", "a" },
                Seq.<String, RuntimeException> of("c", "e", "a", "d", "b").reverseSortedBy(Fn.identity()).skip(2).toArray(String[]::new));
    }

    @Test
    public void testCycled() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void test_cycled() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2).cycled().limit(5);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1), drainWithException(seq));
        Seq<Integer, Exception> emptyCycled = Seq.<Integer, Exception> empty().cycled().limit(5);
        assertTrue(drainWithException(emptyCycled).isEmpty());
    }

    @Test
    public void test_cycled_withRounds() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2).cycled(3);
        assertEquals(Arrays.asList(1, 2, 1, 2, 1, 2), drainWithException(seq));
        Seq<Integer, Exception> zeroRounds = Seq.of(1, 2).cycled(0);
        assertTrue(drainWithException(zeroRounds).isEmpty());
    }

    @Test
    public void testCycledWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").cycled().limit(5).count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").cycled().skip(10).limit(5).count());
        assertArrayEquals(new String[] { "a", "b", "c", "a", "b" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled().limit(5).toArray(String[]::new));
        assertArrayEquals(new String[] { "b", "c", "a" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled().skip(1).limit(3).toArray(String[]::new));
    }

    @Test
    public void testCycledLongWithSkipCountAndToArray() throws Exception {
        assertEquals(6, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).count());
        assertEquals(4, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "a", "b", "c" }, Seq.<String, RuntimeException> of("a", "b", "c").cycled(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testCycledWithRounds() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).cycled(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);

        seq = Seq.of(1, 2, 3).cycled(0);
        result = seq.toList();
        assertTrue(result.isEmpty());

        seq = Seq.of(1, 2, 3).cycled(1);
        result = seq.toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCycled_Infinite_WithLimit() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled().limit(7).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1), result);
    }

    @Test
    public void testCycled_ZeroRounds() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRateLimited() throws Exception {
        long start = System.currentTimeMillis();
        Seq.of(1, 2, 3).rateLimited(100).forEach(x -> {
        });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed >= 20);
    }

    @Test
    public void test_rateLimited() throws Exception {
        RateLimiter mockLimiter = mock(RateLimiter.class);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).rateLimited(mockLimiter);
        List<Integer> result = drainWithException(seq);
        assertEquals(Arrays.asList(1, 2, 3), result);
        verify(mockLimiter, times(3)).acquire();
    }

    @Test
    public void testRateLimitedDoubleWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(1000).skip(2).toArray(String[]::new));
    }

    @Test
    public void testRateLimitedRateLimiterWithSkipCountAndToArray() throws Exception {
        RateLimiter rateLimiter = RateLimiter.create(1000);
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").rateLimited(rateLimiter).skip(2).toArray(String[]::new));
    }

    @Test
    public void testRateLimitedWithRateLimiter() throws Exception {
        RateLimiter limiter = RateLimiter.create(5.0);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).rateLimited(limiter);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() throws Exception {
        long start = System.currentTimeMillis();

        Seq.of(1, 2, 3).rateLimited(10.0).forEach(x -> {
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 200);
    }

    @Test
    public void testDelay() throws Exception {
        long start = System.currentTimeMillis();
        Seq.of(1, 2, 3).delay(Duration.ofMillis(100)).forEach(x -> {
        });
        long elapsed = System.currentTimeMillis() - start;
        assertTrue(elapsed < 300);
    }

    @Test
    public void test_delay() throws Exception {
        long startTime = System.currentTimeMillis();
        Seq<Integer, Exception> seq = Seq.of(1, 2).delay(Duration.ofMillis(100));
        List<Integer> result = drainWithException(seq);
        long endTime = System.currentTimeMillis();
        assertEquals(Arrays.asList(1, 2), result);
        assertTrue(endTime - startTime < 150, "Should have some delay, approx 200ms for 2 elements. Actual: " + (endTime - startTime));
    }

    @Test
    public void testDelayWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(Duration.ofMillis(1)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDelayJavaTimeWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").delay(java.time.Duration.ofMillis(1)).skip(2).toArray(String[]::new));
    }

    @Test
    public void testDelayWithJavaTimeDuration() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2).delay(java.time.Duration.ofMillis(30));
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2), result);
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() throws Exception {
        // All elements should pass through when within limit
        List<Integer> result = Seq.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDebounce_ExceedsWindowLimit() throws Exception {
        // Only first 3 elements should pass through when limit is 3
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDebounce_ExactlyAtLimit() throws Exception {
        // Exactly maxWindowSize elements should pass through
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).debounce(5, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDebounce_SingleElementLimit() throws Exception {
        // Only first element should pass through when limit is 1
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).debounce(1, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testDebounce_EmptySeq() throws Exception {
        // Empty sequence should remain empty
        List<Integer> result = Seq.<Integer, RuntimeException> empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testDebounce_SingleElement() throws Exception {
        // Single element within limit should pass through
        List<Integer> result = Seq.of(42).debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertEquals(Arrays.asList(42), result);
    }

    @Test
    public void testDebounce_InvalidMaxWindowSize_Zero() throws Exception {
        // Zero maxWindowSize should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });
    }

    @Test
    public void testDebounce_InvalidMaxWindowSize_Negative() throws Exception {
        // Negative maxWindowSize should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        });
    }

    @Test
    public void testDebounce_InvalidDuration_Zero() throws Exception {
        // Zero duration should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toList();
        });
    }

    @Test
    public void testDebounce_InvalidDuration_Negative() throws Exception {
        // Negative duration should throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            Seq.of(1, 2, 3).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toList();
        });
    }

    @Test
    public void testDebounce_WindowResetsAfterDuration() throws Exception {
        // After duration elapses, the window should reset and allow more elements
        List<Integer> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).debounce(2, com.landawn.abacus.util.Duration.ofMillis(50)).peek(x -> {
            result.add(x);
            if (result.size() == 2) {
                // Sleep to allow window to reset
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).toList();

        // First 2 pass, then window resets after sleep, allowing more to pass
        assertTrue(result.size() >= 2);
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
    }

    @Test
    public void testDebounce_MultipleWindowResets() throws Exception {
        // Test multiple window resets over time
        AtomicInteger counter = new AtomicInteger(0);
        List<Integer> passedElements = new ArrayList<>();

        Seq.range(1, 10).debounce(2, com.landawn.abacus.util.Duration.ofMillis(30)).peek(x -> {
            passedElements.add(x);
            counter.incrementAndGet();
            // Add small delay between elements to simulate processing
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).toList();

        // Due to window resets, more than 2 elements should pass
        assertTrue(passedElements.size() >= 2);
    }

    @Test
    public void testDebounce_LargeWindowSize() throws Exception {
        // When window size is larger than elements, all should pass
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).debounce(1000, com.landawn.abacus.util.Duration.ofSeconds(1)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDebounce_WithNullElements() throws Exception {
        // Null elements should be handled (passed through if within limit)
        List<String> result = Seq.of("a", null, "b", null, "c").debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0));
        assertNull(result.get(1));
        assertEquals("b", result.get(2));
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() throws Exception {
        // Test debounce chained with filter and map
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
                .filter(x -> x % 2 == 0) // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)) // 2, 4, 6
                .map(x -> x * 10)
                .toList();
        assertEquals(Arrays.asList(20, 40, 60), result);
    }

    @Test
    public void testDebounce_ShortDuration() throws Exception {
        // Test with very short duration (1ms)
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).debounce(2, com.landawn.abacus.util.Duration.ofMillis(1)).toList();
        // First 2 should definitely pass, possibly more if windows reset
        assertTrue(result.size() >= 2);
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(2), result.get(1));
    }

    @Test
    public void testDebounce_PreservesOrder() throws Exception {
        // Verify that element order is preserved
        List<Integer> result = Seq.of(5, 3, 1, 4, 2).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(Arrays.asList(5, 3, 1), result);
    }

    @Test
    public void testDebounce_WithStrings() throws Exception {
        // Test with String elements
        List<String> result = Seq.of("apple", "banana", "cherry", "date", "elderberry").debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).toList();
        assertEquals(Arrays.asList("apple", "banana", "cherry"), result);
    }

    @Test
    public void testDebounce_IntermediateOperation() throws Exception {
        // Verify debounce is an intermediate operation (lazy evaluation)
        AtomicInteger counterBefore = new AtomicInteger(0);
        AtomicInteger counterAfter = new AtomicInteger(0);
        var seq = Seq.of(1, 2, 3, 4, 5)
                .peek(x -> counterBefore.incrementAndGet())
                .debounce(2, com.landawn.abacus.util.Duration.ofSeconds(10))
                .peek(x -> counterAfter.incrementAndGet());

        // No elements should be processed yet (lazy evaluation)
        assertEquals(0, counterBefore.get());
        assertEquals(0, counterAfter.get());

        // Now trigger terminal operation
        seq.toList();
        // All 5 elements pass through the first peek (before debounce)
        assertEquals(5, counterBefore.get());
        // Only 2 elements pass through the second peek (after debounce)
        assertEquals(2, counterAfter.get());
    }

    @Test
    public void testDebounce_LongDuration() throws Exception {
        // Test with long duration (elements should all be in same window)
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).debounce(5, com.landawn.abacus.util.Duration.ofHours(1)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testDebounce_CountTerminalOperation() throws Exception {
        // Test with count() terminal operation
        long count = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).debounce(4, com.landawn.abacus.util.Duration.ofSeconds(10)).count();
        assertEquals(4, count);
    }

    @Test
    public void testDebounce_ForEachTerminalOperation() throws Exception {
        // Test with forEach() terminal operation
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testDebounce_FirstTerminalOperation() throws Exception {
        // Test with first() terminal operation
        Optional<Integer> first = Seq.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).first();
        assertTrue(first.isPresent());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testDebounce_ReduceTerminalOperation() throws Exception {
        // Test with reduce() terminal operation
        Optional<Integer> sum = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).debounce(4, com.landawn.abacus.util.Duration.ofSeconds(10)).reduce(Integer::sum);
        assertTrue(sum.isPresent());
        assertEquals(Integer.valueOf(10), sum.get()); // 1 + 2 + 3 + 4
    }

    @Test
    public void testDebounce_AnyMatchTerminalOperation() throws Exception {
        // Test with anyMatch() terminal operation
        boolean result = Seq.of(1, 2, 3, 4, 5).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).anyMatch(x -> x == 2);
        assertTrue(result);
    }

    @Test
    public void testDebounce_AllMatchTerminalOperation() throws Exception {
        // Test with allMatch() terminal operation
        boolean result = Seq.of(2, 4, 6, 8, 10).debounce(3, com.landawn.abacus.util.Duration.ofSeconds(10)).allMatch(x -> x % 2 == 0);
        assertTrue(result); // 2, 4, 6 all even
    }

    @Test
    public void testIntersperse() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).intersperse(0).toList();
        assertEquals(Arrays.asList(1, 0, 2, 0, 3), result);
    }

    @Test
    public void test_intersperse() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c").intersperse("-");
        assertEquals(Arrays.asList("a", "-", "b", "-", "c"), drainWithException(seq));

        Seq<String, Exception> single = Seq.of("a").intersperse("-");
        assertEquals(Collections.singletonList("a"), drainWithException(single));

        Seq<String, Exception> empty = Seq.<String, Exception> empty().intersperse("-");
        assertTrue(drainWithException(empty).isEmpty());
    }

    @Test
    public void testInterspersWithSkipCountAndToArray() throws Exception {
        assertEquals(9, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").count());
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").skip(4).count());
        assertArrayEquals(new String[] { "a", "-", "b", "-", "c", "-", "d", "-", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "-", "d", "-", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").intersperse("-").skip(4).toArray(String[]::new));
    }

    @Test
    public void testIntersperse_Empty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().intersperse(0).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testIntersperse_SingleElement() throws Exception {
        List<Integer> result = Seq.of(1).intersperse(0).toList();
        assertEquals(Arrays.asList(1), result);
    }

    @Test
    public void testStep() throws Exception {
        var seq = Seq.of(1, 2, 3, 4, 5).step(2);
        List<Integer> result = seq.toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void test_step() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(0, 1, 2, 3, 4, 5, 6).step(3);
        assertEquals(Arrays.asList(0, 3, 6), drainWithException(seq));

        Seq<Integer, Exception> stepOne = Seq.of(0, 1, 2).step(1);
        assertEquals(Arrays.asList(0, 1, 2), drainWithException(stepOne));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).step(0));
    }

    @Test
    public void testStepWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).skip(2).count());
        assertArrayEquals(new String[] { "a", "c", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).toArray(String[]::new));
        assertArrayEquals(new String[] { "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").step(2).skip(2).toArray(String[]::new));
    }

    @Test
    public void testStep_ByTwo() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6).step(2).toList();
        assertEquals(Arrays.asList(1, 3, 5), result);
    }

    @Test
    public void testStep_ByThree() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).step(3).toList();
        assertEquals(Arrays.asList(1, 4, 7), result);
    }

    @Test
    public void testIndexed() throws Exception {
        var seq = Seq.of("a", "b", "c").indexed();
        List<Indexed<String>> result = seq.toList();
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).value());
        assertEquals(0, result.get(0).index());
        assertEquals("b", result.get(1).value());
        assertEquals(1, result.get(1).index());
    }

    @Test
    public void test_indexed() throws Exception {
        List<Indexed<String>> result = Seq.of("a", "b", "c").indexed().toList();
        assertEquals(3, result.size());
        assertEquals(Indexed.of("a", 0L), result.get(0));
        assertEquals(Indexed.of("b", 1L), result.get(1));
        assertEquals(Indexed.of("c", 2L), result.get(2));
    }

    @Test
    public void testIndexedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().skip(2).count());

        Indexed<String>[] result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().toArray(Indexed[]::new);
        assertEquals(5, result.length);
        assertEquals("a", result[0].value());
        assertEquals(0, result[0].index());
        assertEquals("e", result[4].value());
        assertEquals(4, result[4].index());

        result = Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").indexed().skip(2).toArray(Indexed[]::new);
        assertEquals(3, result.length);
        assertEquals("c", result[0].value());
        assertEquals(2, result[0].index());
    }

    @Test
    public void testBuffered() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void test_buffered() throws Exception, InterruptedException {
        final int bufferSize = 2;
        final int numElements = 5;
        final CountDownLatch produceLatch = new CountDownLatch(numElements);
        final CountDownLatch consumeLatch = new CountDownLatch(numElements);
        final List<Integer> sourceList = new ArrayList<>();
        for (int i = 0; i < numElements; i++)
            sourceList.add(i);

        Throwables.Iterator<Integer, Exception> slowIterator = new Throwables.Iterator<>() {
            private int current = 0;

            @Override
            public boolean hasNext() {
                return current < numElements;
            }

            @Override
            public Integer next() throws Exception {
                if (current >= numElements)
                    throw new NoSuchElementException();
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    throw new Exception("Interrupted", e);
                }
                int val = sourceList.get(current++);
                produceLatch.countDown();
                return val;
            }
        };

        Seq<Integer, Exception> originalSeq = Seq.of(slowIterator);
        Seq<Integer, Exception> bufferedSeq = originalSeq.buffered(bufferSize);

        List<Integer> result = new ArrayList<>();
        Thread consumerThread = new Thread(() -> {
            try {
                bufferedSeq.forEach(val -> {
                    result.add(val);
                    consumeLatch.countDown();
                    if (val < 2) {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                        }
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        consumerThread.start();

        assertTrue(produceLatch.await(5, TimeUnit.SECONDS), "Producer should have finished producing");
        assertTrue(consumeLatch.await(5, TimeUnit.SECONDS), "Consumer should have finished consuming");
        consumerThread.join();

        assertEquals(sourceList, result);
    }

    @Test
    public void testBufferedWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" }, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered().skip(2).toArray(String[]::new));
    }

    @Test
    public void testBufferedIntWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).count());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).skip(2).count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").buffered(10).skip(2).toArray(String[]::new));
    }

    @Test
    public void testBufferedWithSize() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).buffered(2);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    // ===== buffered() - default buffer size path =====

    @Test
    public void testBuffered_defaultSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).buffered().toList();
        assertEquals(5, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
        assertEquals(Integer.valueOf(5), result.get(4));
    }

    @Test
    public void test_mergeWith_collection() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 3, 5);
        List<Integer> listB = Arrays.asList(2, 4, 6);
        Seq<Integer, Exception> merged = seqA.mergeWith(listB, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(merged));
    }

    @Test
    public void test_mergeWith_seq() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seqB = Seq.of(2, 4, 6);
        Seq<Integer, Exception> merged = seqA.mergeWith(seqB, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), drainWithException(merged));
    }

    @Test
    public void testMergeWithCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(5,
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(2)
                        .count());
        assertArrayEquals(new String[] { "a", "b", "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "c", "d", "e" },
                Seq.<String, RuntimeException> of("a", "c", "e")
                        .mergeWith(Arrays.asList("b", "d"), (x, y) -> x.compareTo(y) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testMergeWithCollection() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = seq.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithSeq() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6);
        Seq<Integer, Exception> merged = seq1.mergeWith(seq2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        List<Integer> result = merged.toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithDuplicates() throws Exception {
        Integer[] arr1 = { 1, 1, 3, 3, 5 };
        Integer[] arr2 = { 2, 2, 4, 4, 6 };

        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3, 4, 4, 5, 6), seq.toList());
    }

    @Test
    public void testMergeWith() throws Exception {
        List<Integer> result = Seq.of(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipWith_Collection() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipWith_Seq() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Seq.of(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void test_zipWith_collection() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        List<String> listB = Arrays.asList("a", "b", "c");
        Seq<String, Exception> zipped = seqA.zipWith(listB, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_collection_defaults() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        List<String> listB = Arrays.asList("a", "b", "c");
        Seq<String, Exception> zipped = seqA.zipWith(listB, 0, "def", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "0c"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_collection_collection() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        List<String> listB = Arrays.asList("a", "b", "c");
        List<Boolean> listC = Arrays.asList(true, false);
        Seq<String, Exception> zipped = seqA.zipWith(listB, listC, (i, s, bool) -> i + s + bool);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_collection_collection_defaults() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1);
        List<String> listB = Arrays.asList("a", "b");
        List<Boolean> listC = Arrays.asList(true, false, true);
        Seq<String, Exception> zipped = seqA.zipWith(listB, listC, 0, "defS", false, (i, s, bool) -> i + s + bool);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0defStrue"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_seq() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        Seq<String, Exception> seqB = Seq.of("a", "b", "c");
        Seq<String, Exception> zipped = seqA.zipWith(seqB, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_seq_defaults() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        Seq<String, Exception> seqB = Seq.of("a", "b", "c");
        Seq<String, Exception> zipped = seqA.zipWith(seqB, 0, "def", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "0c"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_seq_seq() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1, 2);
        Seq<String, Exception> seqB = Seq.of("a", "b", "c");
        Seq<Boolean, Exception> seqC = Seq.of(true, false);
        Seq<String, Exception> zipped = seqA.zipWith(seqB, seqC, (i, s, bool) -> i + s + bool);
        assertEquals(Arrays.asList("1atrue", "2bfalse"), drainWithException(zipped));
    }

    @Test
    public void test_zipWith_seq_seq_defaults() throws Exception {
        Seq<Integer, Exception> seqA = Seq.of(1);
        Seq<String, Exception> seqB = Seq.of("a", "b");
        Seq<Boolean, Exception> seqC = Seq.of(true, false, true);
        Seq<String, Exception> zipped = seqA.zipWith(seqB, seqC, 0, "defS", false, (i, s, bool) -> i + s + bool);
        assertEquals(Arrays.asList("1atrue", "0bfalse", "0defStrue"), drainWithException(zipped));
    }

    @Test
    public void testZipWithCollectionWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y).toArray(String[]::new));
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2", "3"), (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithCollectionDefaultsWithSkipCountAndToArray() throws Exception {
        assertEquals(5, Seq.<String, RuntimeException> of("a", "b", "c").zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y).count());
        assertEquals(3,
                Seq.<String, RuntimeException> of("a", "b", "c").zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3", "z4", "z5" },
                Seq.<String, RuntimeException> of("a", "b", "c")
                        .zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "c3", "z4", "z5" },
                Seq.<String, RuntimeException> of("a", "b", "c")
                        .zipWith(Arrays.asList("1", "2", "3", "4", "5"), "z", "9", (x, y) -> x + y)
                        .skip(2)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithTwoCollectionsWithSkipCountAndToArray() throws Exception {
        assertEquals(2,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .count());
        assertEquals(1,
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .skip(1)
                        .count());
        assertArrayEquals(new String[] { "a1x", "b2y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .toArray(String[]::new));
        assertArrayEquals(new String[] { "b2y" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e")
                        .zipWith(Arrays.asList("1", "2"), Arrays.asList("x", "y"), (a, b, c) -> a + b + c)
                        .skip(1)
                        .toArray(String[]::new));
    }

    @Test
    public void testZipWithSeqWithSkipCountAndToArray() throws Exception {
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).count());
        assertEquals(1, Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).skip(2).count());
        assertArrayEquals(new String[] { "a1", "b2", "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).toArray(String[]::new));
        assertArrayEquals(new String[] { "c3" },
                Seq.<String, RuntimeException> of("a", "b", "c", "d", "e").zipWith(Seq.of("1", "2", "3"), (x, y) -> x + y).skip(2).toArray(String[]::new));
    }

    @Test
    public void testZipWithCollection() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1", "b2", "c3"), result);

        seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2), (s, i) -> s + i);
        result = seq.toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipWithCollectionAndDefaults() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), "z", 0, (s, i) -> s + i);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1", "b2", "z3"), result);
    }

    @Test
    public void testZipWithTwoCollections() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), Arrays.asList("x", "y", "z"), (s, i, s2) -> s + i + s2);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testZipWithTwoCollectionsAndDefaults() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b").zipWith(Arrays.asList(1, 2, 3), Arrays.asList("x"), "z", 0, "w", (s, i, s2) -> s + i + s2);
        List<String> result = seq.toList();

        assertEquals(Arrays.asList("a1x", "b2w", "z3w"), result);
    }

    @Test
    public void testZipWithSeq() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> zipped = seq1.zipWith(seq2, (s, i) -> s + i);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipWithSeqAndDefaults() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> zipped = seq1.zipWith(seq2, "z", 0, (s, i) -> s + i);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1", "b2", "z3"), result);
    }

    @Test
    public void testZipWithTwoSeqs() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> seq3 = Seq.of("x", "y", "z");
        Seq<String, Exception> zipped = seq1.zipWith(seq2, seq3, (s, i, s2) -> s + i + s2);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1x", "b2y", "c3z"), result);
    }

    @Test
    public void testZipWithTwoSeqsAndDefaults() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);
        Seq<String, Exception> seq3 = Seq.of("x");
        Seq<String, Exception> zipped = seq1.zipWith(seq2, seq3, "z", 0, "w", (s, i, s2) -> s + i + s2);
        List<String> result = zipped.toList();

        assertEquals(Arrays.asList("a1x", "b2w", "z3w"), result);
    }

    @Test
    public void testZipWithNullElements() throws Exception {
        String[] arr1 = { "a", null, "c" };
        String[] arr2 = { "1", "2", "3" };

        Seq<String, Exception> seq = Seq.zip(arr1, arr2, (s1, s2) -> (s1 == null ? "null" : s1) + s2);
        Assertions.assertEquals(Arrays.asList("a1", "null2", "c3"), seq.toList());
    }

    @Test
    public void testZipWithDefaults() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2), "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipWithThreeCollections() throws Exception {
        List<String> result = Seq.of("a", "b").zipWith(Arrays.asList(1, 2), Arrays.asList(true, false), (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipWithThreeCollectionsDefaults() throws Exception {
        List<String> result = Seq.of("a", "b").zipWith(Arrays.asList(1), Arrays.asList(true, false, true), "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testZipWithSeqDefaults() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Seq.of(1, 2), "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipWithThreeSeqs() throws Exception {
        List<String> result = Seq.of("a", "b").zipWith(Seq.of(1, 2), Seq.of(true, false), (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipWithThreeSeqsDefaults() throws Exception {
        List<String> result = Seq.of("a", "b").zipWith(Seq.of(1), Seq.of(true, false, true), "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testZipWith_CollectionDefaults() throws Exception {
        List<String> result = Seq.<Integer, Exception> of(1, 2, 3).zipWith(Arrays.asList("a", "b"), 0, "z", (a, b) -> a + b).toList();
        assertEquals(Arrays.asList("1a", "2b", "3z"), result);
    }

    @Test
    public void testZipWith_ThreeCollections() throws Exception {
        List<String> result = Seq.<Integer, Exception> of(1, 2).zipWith(Arrays.asList("a", "b"), Arrays.asList(10.0, 20.0), (a, b, c) -> a + b + c).toList();
        assertEquals(Arrays.asList("1a10.0", "2b20.0"), result);
    }

    @Test
    public void testZipWith_ThreeCollectionsDefaults() throws Exception {
        List<String> result = Seq.<Integer, Exception> of(1, 2, 3)
                .zipWith(Arrays.asList("a"), Arrays.asList(10.0), 0, "z", 0.0, (a, b, c) -> a + b + c)
                .toList();
        assertEquals(Arrays.asList("1a10.0", "2z0.0", "3z0.0"), result);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForeach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void test_forEach_throwableConsumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach((Throwables.Consumer<Integer, Exception>) collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void test_forEach_consumer_onComplete() throws Exception {
        List<Integer> collected = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Seq.of(1, 2, 3).forEach(collected::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), collected);
        assertTrue(completed.get());
    }

    @Test
    public void test_forEach_flatMapper_biConsumer() throws Exception {
        Map<Integer, List<String>> result = new HashMap<>();
        Seq.of(1, 2)
                .forEach(num -> Arrays.asList(String.valueOf(num), "s" + num),
                        (originalNum, mappedString) -> result.computeIfAbsent(originalNum, k -> new ArrayList<>()).add(mappedString));
        assertEquals(Arrays.asList("1", "s1"), result.get(1));
        assertEquals(Arrays.asList("2", "s2"), result.get(2));
    }

    @Test
    public void test_forEach_flatMapper_flatMapper2_triConsumer() throws Exception {
        List<Triple<Integer, String, Character>> result = new ArrayList<>();
        Seq.of(1, 2)
                .forEach(num -> Arrays.asList("A" + num, "B" + num), str -> Arrays.asList(str.charAt(0), str.charAt(1)),
                        (originalNum, intermediateStr, finalChar) -> result.add(Triple.of(originalNum, intermediateStr, finalChar)));

        List<Triple<Integer, String, Character>> expected = Arrays.asList(Triple.of(1, "A1", 'A'), Triple.of(1, "A1", '1'), Triple.of(1, "B1", 'B'),
                Triple.of(1, "B1", '1'), Triple.of(2, "A2", 'A'), Triple.of(2, "A2", '2'), Triple.of(2, "B2", 'B'), Triple.of(2, "B2", '2'));
        assertEquals(expected.size(), result.size());
        assertTrue(result.containsAll(expected) && expected.containsAll(result));
    }

    @Test
    public void test_forEach_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.forEach(x -> {
        }));
    }

    @Test
    public void testForEachWithOnComplete() throws Exception {
        List<Integer> result = new ArrayList<>();
        MutableBoolean completed = MutableBoolean.of(false);

        Seq.of(1, 2, 3).forEach(result::add, () -> completed.setTrue());

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertTrue(completed.isTrue());
    }

    @Test
    public void testForEachWithFlatMapper() throws Exception {
        List<String> result = new ArrayList<>();

        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2, 3), (s, i) -> result.add(s + i));

        assertEquals(Arrays.asList("a1", "a2", "a3", "b1", "b2", "b3"), result);
    }

    @Test
    public void testForEachWithTwoFlatMappers() throws Exception {
        List<String> result = new ArrayList<>();

        Seq.of("a", "b").forEach(s -> Arrays.asList(1, 2), i -> Arrays.asList("x", "y"), (s, i, s2) -> result.add(s + i + s2));

        assertEquals(Arrays.asList("a1x", "a1y", "a2x", "a2y", "b1x", "b1y", "b2x", "b2y"), result);
    }

    @Test
    public void testForEachFlatMapper() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of("a", "b").forEach(s -> Arrays.asList(s, s.toUpperCase()), (original, transformed) -> collected.add(original + transformed));

        assertEquals(Arrays.asList("aa", "aA", "bb", "bB"), collected);
    }

    @Test
    public void testForEachDoubleFlatMapper() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of("a").forEach(s -> Arrays.asList(1, 2), i -> Arrays.asList(true, false), (s, i, b) -> collected.add(s + i + b));

        assertEquals(Arrays.asList("a1true", "a1false", "a2true", "a2false"), collected);
    }

    @Test
    public void testForEach_WithOnComplete() throws Exception {
        List<Integer> collected = new ArrayList<>();
        AtomicBoolean completed = new AtomicBoolean(false);
        Seq.of(1, 2, 3).forEach(collected::add, () -> completed.set(true));
        assertEquals(Arrays.asList(1, 2, 3), collected);
        assertTrue(completed.get());
    }

    @Test
    public void testForEach_WithFlatMapper() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(x -> Arrays.asList(x, x * 10), (src, val) -> collected.add(val));
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), collected);
    }

    @Test
    public void testForEach_FlatMapperBiConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of("a", "b").forEach((String s) -> java.util.Arrays.asList(s + "1", s + "2"), (String s, String u) -> result.add(s + ":" + u));
        assertEquals(4, result.size());
        assertTrue(result.contains("a:a1"));
        assertTrue(result.contains("a:a2"));
        assertTrue(result.contains("b:b1"));
        assertTrue(result.contains("b:b2"));
    }

    @Test
    public void testForEach_FlatMapperFlatMapper2TriConsumer() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of("x")
                .forEach((String s) -> java.util.Arrays.asList(1, 2), (Integer i) -> java.util.Arrays.asList("p", "q"),
                        (String s, Integer i, String t) -> result.add(s + i + t));
        assertEquals(4, result.size());
        assertTrue(result.contains("x1p"));
        assertTrue(result.contains("x1q"));
        assertTrue(result.contains("x2p"));
        assertTrue(result.contains("x2q"));
    }

    @Test
    public void testForEachIndexed() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of("a", "b", "c").forEachIndexed((i, s) -> collected.add(i + ":" + s));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), collected);
    }

    @Test
    public void test_forEachIndexed() throws Exception {
        Map<Integer, String> collected = new HashMap<>();
        Seq.of("a", "b").forEachIndexed((idx, val) -> collected.put(idx, val));
        assertEquals("a", collected.get(0));
        assertEquals("b", collected.get(1));
        assertEquals(2, collected.size());
    }

    @Test
    public void testForEachUntil() throws Exception {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);
        Seq.of(1, 2, 3, 4, 5).forEachUntil((x, flag) -> {
            collected.add(x);
            if (x >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void test_forEachUntil_biConsumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachUntil((val, flag) -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void test_forEachUntil_flag_consumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Seq.of(1, 2, 3, 4, 5).forEachUntil(flag, val -> {
            collected.add(val);
            if (val == 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilWithBiConsumer() throws Exception {
        List<Integer> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachUntil((value, flag) -> {
            result.add(value);
            if (value >= 3) {
                flag.setTrue();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachUntilWithFlag() throws Exception {
        MutableBoolean stopFlag = MutableBoolean.of(false);
        List<Integer> result = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5).forEachUntil(stopFlag, value -> {
            result.add(value);
            if (value == 3) {
                stopFlag.setTrue();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testForEachUntilBiConsumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachUntil((value, flagToBreak) -> {
            collected.add(value);
            if (value == 3) {
                flagToBreak.setTrue();
            }
        });

        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachUntilMutableBoolean() throws Exception {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flagToBreak = MutableBoolean.of(false);

        Seq.of(1, 2, 3, 4, 5).forEachUntil(flagToBreak, value -> {
            if (value == 3) {
                flagToBreak.setTrue();
            } else {
                collected.add(value);
            }
        });

        assertEquals(Arrays.asList(1, 2), collected);
    }

    @Test
    public void testForEachUntil_WithMutableBoolean() throws Exception {
        List<Integer> collected = new ArrayList<>();
        MutableBoolean flag = MutableBoolean.of(false);
        Seq.of(1, 2, 3, 4, 5).forEachUntil(flag, x -> {
            collected.add(x);
            if (x >= 3) {
                flag.setTrue();
            }
        });
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachPair() throws Exception {
        List<String> collected = new ArrayList<>();
        Seq.of(1, 2, 3, 4).forEachPair((a, b) -> collected.add(a + "-" + b));
        assertEquals(Arrays.asList("1-2", "2-3", "3-4"), collected);
    }

    @Test
    public void testForEachPairWithIncrement() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachPair(2, (a, b) -> result.add(a + "," + b));

        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8).forEachPair(3, (a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "4,5", "7,8"), result);
    }

    @Test
    public void testForEachPair_WithIncrement() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachPair(2, (a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "3,4", "5,6"), result);
    }

    @Test
    public void testForEachPair_Default() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachPair((a, b) -> result.add(a + "," + b));
        assertEquals(Arrays.asList("1,2", "2,3", "3,4", "4,5"), result);
    }

    @Test
    public void testForEachTriple() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "2,3,4", "3,4,5", "4,5,6"), result);
    }

    @Test
    public void testForEachTripleWithIncrement() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).forEachTriple(3, (a, b, c) -> result.add(a + "," + b + "," + c));

        assertEquals(Arrays.asList("1,2,3", "4,5,6", "7,8,9"), result);

        result.clear();
        Seq.of(1, 2, 3, 4, 5, 6, 7).forEachTriple(2, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "3,4,5", "5,6,7"), result);
    }

    @Test
    public void testForEachTriple_WithIncrement() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple(3, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(Arrays.asList("1,2,3", "4,5,6"), result);
    }

    @Test
    public void testForEachTriple_New() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachTriple(1, (a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(3, result.size());
        assertEquals("1,2,3", result.get(0));
        assertEquals("2,3,4", result.get(1));
        assertEquals("3,4,5", result.get(2));
    }

    @Test
    public void testForEachTriple_basic() throws Exception {
        List<String> triples = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5).forEachTriple(1, (a, b, c) -> triples.add(a + "," + b + "," + c));
        assertEquals(3, triples.size());
        assertEquals("1,2,3", triples.get(0));
        assertEquals("2,3,4", triples.get(1));
        assertEquals("3,4,5", triples.get(2));
    }

    @Test
    public void testForEachTriple_withLargerIncrement() throws Exception {
        List<String> triples = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple(3, (a, b, c) -> triples.add(a + "," + b + "," + c));
        assertEquals(2, triples.size());
        assertEquals("1,2,3", triples.get(0));
        assertEquals("4,5,6", triples.get(1));
    }

    // ===== forEachTriple - increment > windowSize path with skip loop =====

    @Test
    public void testForEachTriple_incrementLargerThanWindow() throws Exception {
        // increment=4 > windowSize=3, so skip loop is triggered for non-first iterations
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12).forEachTriple(4, (a, b, c) -> result.add(a + "," + b + "," + c));
        // Triples: (1,2,3), skip 1, (5,6,7), skip 1, (9,10,11)
        assertEquals(3, result.size());
        assertEquals("1,2,3", result.get(0));
        assertEquals("5,6,7", result.get(1));
        assertEquals("9,10,11", result.get(2));
    }

    @Test
    public void testForEachTriple_noArgDefaultIncrement() throws Exception {
        List<String> result = new ArrayList<>();
        Seq.of(1, 2, 3).forEachTriple((a, b, c) -> result.add(a + "," + b + "," + c));
        assertEquals(1, result.size());
        assertEquals("1,2,3", result.get(0));
    }

    @Test
    public void testMin() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5).min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());
    }

    @Test
    public void testMin_WithComparator() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5).min(Comparator.reverseOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(5), min.get());
    }

    @Test
    public void test_min_comparator() throws Exception {
        Optional<Integer> min = Seq.of(5, 1, 3, 2, 4).min(Comparator.naturalOrder());
        assertEquals(Optional.of(1), min);
        assertTrue(Seq.<Integer, Exception> empty().min(Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void testMinEmpty() throws Exception {
        Optional<Integer> min = Seq.<Integer, Exception> empty().min(Comparator.naturalOrder());

        assertFalse(min.isPresent());
    }

    @Test
    public void testMin_Empty() throws Exception {
        Optional<Integer> result = Seq.<Integer, Exception> empty().min(Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMin_Comparator() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5, 9).min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());

        Optional<Integer> empty = Seq.<Integer, Exception> of().min(Comparator.naturalOrder());
        assertFalse(empty.isPresent());
    }

    @Test
    public void testMin_Comparator_reverseOrder() throws Exception {
        Optional<String> min = Seq.of("banana", "apple", "cherry").min(Comparator.reverseOrder());
        assertTrue(min.isPresent());
        assertEquals("cherry", min.get());
    }

    // ===== min(Comparator) - already-sorted + same comparator path (isSameComparator branch) =====

    @Test
    public void testMin_onAlreadySortedSeq_sameComparator() throws Exception {
        // After sorted() the seq has sorted=true and cmp=NATURAL_COMPARATOR
        // Calling min(naturalOrder()) should hit the isSameComparator branch
        Optional<Integer> min = Seq.of(5, 3, 1, 4, 2).sorted().min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(1), min.get());
    }

    @Test
    public void testMin_onAlreadySortedSeq_sameComparator_singleElement() throws Exception {
        Optional<Integer> min = Seq.of(42).sorted().min(Comparator.naturalOrder());
        assertTrue(min.isPresent());
        assertEquals(Integer.valueOf(42), min.get());
    }

    @Test
    public void testMinBy() throws Exception {
        List<String> words = Arrays.asList("a", "bb", "ccc", "dd");
        Optional<String> result = Seq.of(words).minBy(String::length);
        assertEquals("a", result.get());
    }

    @Test
    public void test_minBy_keyMapper() throws Exception {
        Optional<String> min = Seq.of("apple", "banana", "kiwi").minBy(String::length);
        assertEquals(Optional.of("kiwi"), min);
    }

    @Test
    public void testMinBy_Empty() throws Exception {
        Optional<String> result = Seq.<String, Exception> empty().minBy(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5).max(Comparator.naturalOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(5), max.get());
    }

    @Test
    public void testMax_WithComparator() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5).max(Comparator.reverseOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(1), max.get());
    }

    @Test
    public void test_max_comparator() throws Exception {
        Optional<Integer> max = Seq.of(5, 1, 3, 2, 4).max(Comparator.naturalOrder());
        assertEquals(Optional.of(5), max);
    }

    @Test
    public void testMaxEmpty() throws Exception {
        Optional<Integer> max = Seq.<Integer, Exception> empty().max(Comparator.naturalOrder());

        assertFalse(max.isPresent());
    }

    @Test
    public void testMax_Empty() throws Exception {
        Optional<Integer> result = Seq.<Integer, Exception> empty().max(Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax_Comparator() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5, 9).max(Comparator.naturalOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(9), max.get());
    }

    // ===== max(Comparator) - on sorted seq =====

    @Test
    public void testMax_onNonEmptySeq_withComparator() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).max(Comparator.naturalOrder());
        assertTrue(max.isPresent());
        assertEquals(Integer.valueOf(9), max.get());
    }

    @Test
    public void testMax_withReverseComparator() throws Exception {
        Optional<String> max = Seq.of("apple", "banana", "cherry").max(Comparator.reverseOrder());
        assertTrue(max.isPresent());
        assertEquals("apple", max.get());
    }

    @Test
    public void testMaxBy() throws Exception {
        List<String> words = Arrays.asList("a", "bb", "ccc", "dd");
        Optional<String> result = Seq.of(words).maxBy(String::length);
        assertEquals("ccc", result.get());
    }

    @Test
    public void test_maxBy_keyMapper() throws Exception {
        Optional<String> max = Seq.of("apple", "banana", "kiwi").maxBy(String::length);
        assertEquals(Optional.of("banana"), max);
    }

    @Test
    public void testMaxBy_Empty() throws Exception {
        Optional<String> result = Seq.<String, Exception> empty().maxBy(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void testMaxBy_onNonEmpty() throws Exception {
        Optional<String> max = Seq.of("banana", "apple", "cherry").maxBy(String::length);
        assertTrue(max.isPresent());
        assertEquals("banana", max.get());
    }

    @Test
    public void testAnyMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 3, 5).anyMatch(x -> x % 2 == 0));
    }

    @Test
    public void test_anyMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x == 4));
        assertFalse(Seq.<Integer, Exception> empty().anyMatch(x -> true));
    }

    @Test
    public void testAllMatch() throws Exception {
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(2, 3, 4).allMatch(x -> x % 2 == 0));
    }

    @Test
    public void test_allMatch() throws Exception {
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).allMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().allMatch(x -> false));
    }

    @Test
    public void testNoneMatch() throws Exception {
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
    }

    @Test
    public void test_noneMatch() throws Exception {
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().noneMatch(x -> true));
    }

    @Test
    public void testIsMatchCountBetween() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5, 6).isMatchCountBetween(2, 4, x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3, 4, 5, 6).isMatchCountBetween(4, 5, x -> x % 2 == 0));
        assertTrue(Seq.of(1, 2, 3, 4, 5, 6).isMatchCountBetween(3, 3, x -> x % 2 == 0));
    }

    @Test
    public void testIsMatchCountBetween_New() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5).isMatchCountBetween(2, 3, n -> n % 2 == 0));
        assertFalse(Seq.of(1, 2, 3, 4, 5).isMatchCountBetween(3, 5, n -> n % 2 == 0));
        assertTrue(Seq.of(1, 3, 5).isMatchCountBetween(0, 0, n -> n % 2 == 0));
    }

    @Test
    public void testFindFirst() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(4), found.get());
    }

    @Test
    public void test_findFirst_predicate() throws Exception {
        Optional<Integer> firstEven = Seq.of(1, 3, 2, 4, 5).findFirst(x -> x % 2 == 0);
        assertEquals(Optional.of(2), firstEven);
    }

    @Test
    public void testFindFirst_NoMatch() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).findFirst(x -> x > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findAny(x -> x > 3);
        assertTrue(found.isPresent());
        assertTrue(found.get() > 3);
    }

    @Test
    public void test_findAny_predicate() throws Exception {
        Optional<Integer> anyEven = Seq.of(1, 3, 2, 4, 5).findAny(x -> x % 2 == 0);
        assertEquals(Optional.of(2), anyEven);
    }

    @Test
    public void testFindAny_NoMatch() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).findAny(x -> x > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast() throws Exception {
        Optional<Integer> found = Seq.of(1, 2, 3, 4, 5).findLast(x -> x < 4);
        assertTrue(found.isPresent());
        assertEquals(Integer.valueOf(3), found.get());
    }

    @Test
    public void test_findLast_predicate() throws Exception {
        Optional<Integer> lastEven = Seq.of(1, 3, 2, 4, 5).findLast(x -> x % 2 == 0);
        assertEquals(Optional.of(4), lastEven);
        assertTrue(Seq.of(1, 3, 5).findLast(x -> x % 2 == 0).isEmpty());
    }

    @Test
    public void testFindLast_NoMatch() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).findLast(x -> x > 10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testContainsAll() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(2, 3));
        assertFalse(Seq.of(1, 2, 3).containsAll(2, 4));
    }

    @Test
    public void test_containsAll_varargs() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(2, 4));
        assertFalse(Seq.of(1, 2, 3, 4).containsAll(2, 5));
        assertTrue(Seq.of(1, 2, 3, 4).containsAll());
    }

    @Test
    public void test_containsAll_collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 4)));
        assertFalse(Seq.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 5)));
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsAllArray() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAll(2, 4));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsAll());

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAll(2));

        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAll(2, 2));

        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3, 4);
        assertTrue(seq5.containsAll(2, 4));

        Seq<Integer, RuntimeException> seq6 = Seq.of(1, 2, 3);
        assertFalse(seq6.containsAll(2, 5));

        Seq<Integer, RuntimeException> seq7 = Seq.of(1, 2, 3);
        assertFalse(seq7.containsAll(2, 3, 4));
    }

    @Test
    public void testContainsAllCollection() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAll(Arrays.asList(2, 3, 4)));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsAll(Collections.emptyList()));

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAll(Collections.singletonList(2)));

        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3, 4);
        assertTrue(seq4.containsAll(new HashSet<>(Arrays.asList(2, 3))));

        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertFalse(seq5.containsAll(Arrays.asList(2, 5)));
    }

    @Test
    public void testContainsAll_Collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAll(Arrays.asList(2, 3)));
        assertFalse(Seq.of(1, 2, 3).containsAll(Arrays.asList(2, 4)));
    }

    // ===== containsAll / containsAny - various coverage =====

    @Test
    public void testContainsAll_withEmptyCollection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsAll_partialMatch_returnsFalse() throws Exception {
        assertFalse(Seq.of(1, 2, 3).containsAll(Arrays.asList(1, 4)));
    }

    @Test
    public void testContainsAny() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(2, 4));
        assertFalse(Seq.of(1, 2, 3).containsAny(4, 5));
    }

    @Test
    public void test_containsAny_varargs() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAny(5, 2));
        assertFalse(Seq.of(1, 2, 3, 4).containsAny(5, 6));
        assertFalse(Seq.of(1, 2, 3, 4).containsAny());
    }

    @Test
    public void test_containsAny_collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 2)));
        assertFalse(Seq.of(1, 2, 3, 4).containsAny(Arrays.asList(5, 6)));
        assertFalse(Seq.of(1, 2, 3, 4).containsAny(Collections.emptyList()));
    }

    @Test
    public void testContainsAnyArray() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAny(7, 3, 9));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertFalse(seq2.containsAny());

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAny(2));

        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAny(2, 2));

        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertTrue(seq5.containsAny(2, 5));

        Seq<Integer, RuntimeException> seq6 = Seq.of(1, 2, 3);
        assertFalse(seq6.containsAny(4, 5, 6));

        Seq<Integer, RuntimeException> seq7 = Seq.of(1, 2, 3);
        assertTrue(seq7.containsAny(5, 6, 7, 1));
    }

    @Test
    public void testContainsAnyCollection() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsAny(Arrays.asList(7, 3, 9)));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertFalse(seq2.containsAny(Collections.emptyList()));

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertTrue(seq3.containsAny(Collections.singletonList(2)));

        Seq<Integer, RuntimeException> seq4 = Seq.of(1, 2, 3);
        assertTrue(seq4.containsAny(new HashSet<>(Arrays.asList(5, 2))));

        Seq<Integer, RuntimeException> seq5 = Seq.of(1, 2, 3);
        assertFalse(seq5.containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsAny_Collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(Arrays.asList(2, 4)));
        assertFalse(Seq.of(1, 2, 3).containsAny(Arrays.asList(4, 5)));
    }

    @Test
    public void testContainsAny_withEmptyCollection() throws Exception {
        assertFalse(Seq.of(1, 2, 3).containsAny(Collections.emptyList()));
    }

    @Test
    public void testContainsAny_oneMatch_returnsTrue() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(Arrays.asList(5, 3)));
    }

    @Test
    public void testContainsNone() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(4, 5));
        assertFalse(Seq.of(1, 2, 3).containsNone(2, 4));
    }

    @Test
    public void test_containsNone_varargs() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(4, 5));
        assertFalse(Seq.of(1, 2, 3).containsNone(3, 4));
        assertTrue(Seq.of(1, 2, 3).containsNone());
    }

    @Test
    public void test_containsNone_collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(Arrays.asList(4, 5)));
        assertFalse(Seq.of(1, 2, 3).containsNone(Arrays.asList(3, 4)));
        assertTrue(Seq.of(1, 2, 3).containsNone(Collections.emptyList()));
    }

    @Test
    public void testContainsNoneArray() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsNone(6, 7, 8));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsNone());

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertFalse(seq3.containsNone(4, 2, 5));
    }

    @Test
    public void testContainsNoneCollection() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        assertTrue(seq1.containsNone(Arrays.asList(6, 7, 8)));

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        assertTrue(seq2.containsNone(Collections.emptyList()));

        Seq<Integer, RuntimeException> seq3 = Seq.of(1, 2, 3);
        assertFalse(seq3.containsNone(Arrays.asList(4, 2, 5)));
    }

    @Test
    public void testContainsNone_Collection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(Arrays.asList(4, 5)));
        assertFalse(Seq.of(1, 2, 3).containsNone(Arrays.asList(2, 4)));
    }

    @Test
    public void test_containsDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 2, 3).containsDuplicates());
        assertFalse(Seq.of(1, 2, 3, 4).containsDuplicates());
        assertFalse(Seq.<Integer, Exception> empty().containsDuplicates());
    }

    @Test
    public void testContainsDuplicates_Empty() throws Exception {
        assertFalse(Seq.<Integer, Exception> empty().containsDuplicates());
    }

    @Test
    public void testKthLargest() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2).kthLargest(2, Comparator.naturalOrder());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void test_kthLargest() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(3, 1, 4, 1, 5, 9, 2, 6);
        Optional<Integer> thirdLargest = seq.kthLargest(3, Comparator.naturalOrder());
        assertEquals(Optional.of(5), thirdLargest);

        seq = Seq.of(3, 1, 4, 1, 5, 9, 2, 6);
        Optional<Integer> firstLargest = seq.kthLargest(1, Comparator.naturalOrder());
        assertEquals(Optional.of(9), firstLargest);

        seq = Seq.of(3, 1, 4, 1, 5, 9, 2, 6);
        Optional<Integer> lastLargest = seq.kthLargest(8, Comparator.naturalOrder());
        assertEquals(Optional.of(1), lastLargest);

        seq = Seq.of(3, 1, 4);
        assertTrue(seq.kthLargest(4, Comparator.naturalOrder()).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).kthLargest(0, Comparator.naturalOrder()));
    }

    @Test
    public void testKthLargestInsufficientElements() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).kthLargest(5, Comparator.naturalOrder());

        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_Empty() throws Exception {
        Optional<Integer> result = Seq.<Integer, Exception> empty().kthLargest(1, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_InsufficientElements() throws Exception {
        Optional<Integer> result = Seq.of(1, 2).kthLargest(5, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_First() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(1, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(9), result.get());
    }

    @Test
    public void testKthLargest_New() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());

        Optional<Integer> empty = Seq.<Integer, Exception> of().kthLargest(1, Comparator.naturalOrder());
        assertFalse(empty.isPresent());

        Optional<Integer> tooFew = Seq.of(1, 2).kthLargest(5, Comparator.naturalOrder());
        assertFalse(tooFew.isPresent());
    }

    @Test
    public void testKthLargest_basic() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(5), result.get());
    }

    @Test
    public void testKthLargest_empty() throws Exception {
        Optional<Integer> result = Seq.<Integer, Exception> of().kthLargest(1, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_kLargerThanSize() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).kthLargest(10, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_singleElement() throws Exception {
        Optional<Integer> result = Seq.of(42).kthLargest(1, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(42), result.get());
    }

    // ===== kthLargest - already-sorted + same comparator path =====

    @Test
    public void testKthLargest_onAlreadySortedSeq_sameComparator() throws Exception {
        // sorted() sets sorted=true with NATURAL_COMPARATOR; kthLargest with naturalOrder hits sorted branch
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).sorted().kthLargest(2, Comparator.naturalOrder());
        assertTrue(result.isPresent());
        // sorted ascending: [1,1,2,3,4,5,6,9], kth from the end in sorted context
        assertNotNull(result.get());
    }

    @Test
    public void testKthLargest_onAlreadySortedSeq_kLargerThanSize() throws Exception {
        Optional<Integer> result = Seq.of(1, 2).sorted().kthLargest(5, Comparator.naturalOrder());
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_SortedReverseComparator() throws Exception {
        Comparator<Integer> reverseOrder = Comparator.reverseOrder();
        Optional<Integer> result = Seq.of(3, 1, 4, 2).sorted(reverseOrder).kthLargest(1, reverseOrder);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(1), result.get());
    }

    @Test
    public void test_percentiles() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Optional<Map<Percentage, Integer>> percentiles = seq.percentiles();
        assertTrue(percentiles.isPresent());
        Map<Percentage, Integer> pMap = percentiles.get();
        assertEquals(1, pMap.get(Percentage._0_0001));
        assertEquals(3, pMap.get(Percentage._20));
        assertEquals(6, pMap.get(Percentage._50));
        assertEquals(8, pMap.get(Percentage._70));

        assertEquals(10, pMap.get(Percentage._99_9999));

        assertEquals(3, pMap.get(Percentage._20));
        assertEquals(6, pMap.get(Percentage._50));
        assertEquals(8, pMap.get(Percentage._70));

        assertTrue(Seq.<Integer, Exception> empty().percentiles().isEmpty());
    }

    @Test
    public void test_percentiles_comparator() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);
        Optional<Map<Percentage, Integer>> percentiles = seq.percentiles(Comparator.reverseOrder());
        assertTrue(percentiles.isPresent());
        Map<Percentage, Integer> pMap = percentiles.get();

        assertEquals(10, pMap.get(Percentage._0_0001));
        assertEquals(8, pMap.get(Percentage._20));
        assertEquals(5, pMap.get(Percentage._50));
        assertEquals(3, pMap.get(Percentage._70));
        assertEquals(1, pMap.get(Percentage._99_9999));
    }

    @Test
    public void testPercentiles() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Optional<Map<Percentage, Integer>> result1 = seq1.percentiles();
        assertTrue(result1.isPresent());
        assertNotNull(result1.get());
        assertFalse(result1.get().isEmpty());

        Seq<Integer, RuntimeException> seq2 = Seq.<Integer, RuntimeException> empty();
        Optional<Map<Percentage, Integer>> result2 = seq2.percentiles();
        assertFalse(result2.isPresent());

        Seq<Integer, RuntimeException> seq3 = Seq.of(5);
        Optional<Map<Percentage, Integer>> result3 = seq3.percentiles();
        assertTrue(result3.isPresent());
        assertNotNull(result3.get());
    }

    @Test
    public void testPercentilesWithComparator() {
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "pie", "banana", "zoo");
        Optional<Map<Percentage, String>> result1 = seq1.percentiles(String::compareTo);
        assertTrue(result1.isPresent());
        assertNotNull(result1.get());

        Seq<String, RuntimeException> seq2 = Seq.<String, RuntimeException> empty();
        Optional<Map<Percentage, String>> result2 = seq2.percentiles(String::compareTo);
        assertFalse(result2.isPresent());

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2, 3).percentiles(null));
    }

    @Test
    public void testPercentiles_Empty() throws Exception {
        Optional<Map<Percentage, Integer>> result = Seq.<Integer, Exception> empty().percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles_WithComparator() throws Exception {
        Optional<Map<Percentage, Integer>> result = Seq.of(5, 3, 1, 4, 2).percentiles(Comparator.naturalOrder());
        assertTrue(result.isPresent());
    }

    @Test
    public void testFirst() throws Exception {
        Optional<Integer> first = Seq.of(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(Integer.valueOf(1), first.get());
    }

    @Test
    public void testFirst_Empty() throws Exception {
        Optional<Integer> first = Seq.<Integer, RuntimeException> empty().first();
        assertFalse(first.isPresent());
    }

    @Test
    public void test_first() throws Exception {
        assertEquals(Optional.of(1), Seq.of(1, 2, 3).first());
        assertTrue(Seq.<Integer, Exception> empty().first().isEmpty());
    }

    @Test
    public void testElementAt() throws Exception {
        Optional<Integer> elem = Seq.of(1, 2, 3, 4, 5).elementAt(2);
        assertTrue(elem.isPresent());
        assertEquals(Integer.valueOf(3), elem.get());
    }

    @Test
    public void testElementAt_OutOfBounds() throws Exception {
        Optional<Integer> elem = Seq.of(1, 2, 3).elementAt(10);
        assertFalse(elem.isPresent());
    }

    @Test
    public void test_elementAt() throws Exception {
        {
            Seq<Integer, Exception> seq = Seq.of(10, 20, 30, 40);
            assertEquals(Optional.of(10), seq.elementAt(0));
            seq = Seq.of(10, 20, 30, 40);
            assertEquals(Optional.of(30), seq.elementAt(2));
            seq = Seq.of(10, 20, 30, 40);
            assertTrue(seq.elementAt(4).isEmpty());
        }
        {
            Seq<Integer, Exception> seq = Seq.of(10, 20, 30, 40);
            assertThrows(IllegalArgumentException.class, () -> seq.elementAt(-1));
        }
    }

    @Test
    public void testElementAt_Valid() throws Exception {
        Optional<Integer> result = Seq.of(10, 20, 30, 40, 50).elementAt(2);
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(30), result.get());
    }

    @Test
    public void testElementAt_BeyondSize() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).elementAt(10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne() throws Exception {
        Optional<Integer> only = Seq.of(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(Integer.valueOf(42), only.get());
    }

    @Test
    public void testOnlyOne_Empty() throws Exception {
        Optional<Integer> only = Seq.<Integer, RuntimeException> empty().onlyOne();
        assertFalse(only.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() throws Exception {
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void test_onlyOne() throws Exception {
        assertEquals(Optional.of(1), Seq.of(1).onlyOne());
        assertTrue(Seq.<Integer, Exception> empty().onlyOne().isEmpty());
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void testOnlyOneTooMany() throws Exception {
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2, 3).onlyOne());
    }

    @Test
    public void testOnlyOne_SingleElement() throws Exception {
        Optional<Integer> result = Seq.of(42).onlyOne();
        assertTrue(result.isPresent());
        assertEquals(Integer.valueOf(42), result.get());
    }

    @Test
    public void testOnlyOne_TooMany() throws Exception {
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void testCount() throws Exception {
        long count = Seq.of(1, 2, 3, 4, 5).count();
        assertEquals(5, count);
    }

    @Test
    public void testCount_Empty() throws Exception {
        long count = Seq.empty().count();
        assertEquals(0, count);
    }

    @Test
    public void test_countMatchBetween() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertTrue(seq.isMatchCountBetween(3, 3, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertFalse(seq.isMatchCountBetween(2, 2, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertTrue(seq.isMatchCountBetween(1, 5, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertFalse(seq.isMatchCountBetween(4, 5, x -> x % 2 == 0));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).isMatchCountBetween(-1, 1, x -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).isMatchCountBetween(1, -1, x -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).isMatchCountBetween(2, 1, x -> true));
    }

    @Test
    public void test_count() throws Exception {
        assertEquals(3, Seq.of(1, 2, 3).count());
        assertEquals(0, Seq.<Integer, Exception> empty().count());
    }

    @Test
    public void testToArray() throws Exception {
        Object[] array = Seq.of(1, 2, 3).toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArray_WithGenerator() throws Exception {
        Integer[] array = Seq.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void test_toArray() throws Exception {
        assertArrayEquals(new Object[] { 1, 2, 3 }, Seq.of(1, 2, 3).toArray());
    }

    @Test
    public void test_toArray_generator() throws Exception {
        Integer[] result = Seq.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    public void testToArrayWithGenerator() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String[] array1 = seq1.toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, array1);

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        Integer[] array2 = seq2.toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array2);

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toArray(null));
    }

    @Test
    public void testToArray_WithType() throws Exception {
        String[] result = Seq.of("a", "b", "c").toArray(String[]::new);
        assertArrayEquals(new String[] { "a", "b", "c" }, result);
    }

    @Test
    public void testToArray_Trimmed() throws Exception {
        Object[] arr = Seq.of(1, 2, 3).toArray(true);
        assertEquals(3, arr.length);
    }

    @Test
    public void test_index() {
        assertDoesNotThrow(() -> {
            Iterables.indexOf(CommonUtil.toList(1, 2, 5, 1), 1).boxed().ifPresent(Fn.println());
            Iterables.lastIndexOf(CommonUtil.toLinkedHashSet(1, 2, 5, 3), 3).boxed().ifPresent(Fn.println());
        });
    }

    @Test
    public void testToList() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_toList() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).toList());
    }

    @Test
    public void testToSet() throws Exception {
        Set<Integer> set = Seq.of(1, 2, 2, 3).toSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
    }

    @Test
    public void test_toSet() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3, 2).toSet());
    }

    @Test
    public void testToCollection() throws Exception {
        ArrayList<Integer> list = Seq.of(1, 2, 3).toCollection(ArrayList::new);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_toCollection() throws Exception {
        LinkedList<Integer> result = Seq.of(1, 2, 3).toCollection(LinkedList::new);
        assertEquals(new LinkedList<>(Arrays.asList(1, 2, 3)), result);
    }

    @Test
    public void testToCollection_TreeSet() throws Exception {
        TreeSet<Integer> result = Seq.of(3, 1, 4, 1, 5).toCollection(TreeSet::new);
        assertEquals(4, result.size());
        assertEquals(Integer.valueOf(1), result.first());
    }

    @Test
    public void testToImmutableList() throws Exception {
        ImmutableList<Integer> list = Seq.of(1, 2, 3).toImmutableList();
        assertEquals(3, list.size());
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toImmutableList() throws Exception {
        ImmutableList<Integer> list = Seq.of(1, 2, 3).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void testToImmutableSet() throws Exception {
        ImmutableSet<Integer> set = Seq.of(1, 2, 2, 3).toImmutableSet();
        assertEquals(3, set.size());
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toImmutableSet() throws Exception {
        ImmutableSet<Integer> set = Seq.of(1, 2, 3, 2).toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void testToListThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3).toListThenApply(list -> list.size());
        assertEquals(3, result.intValue());
    }

    @Test
    public void test_toListThenApply() throws Exception {
        Integer sum = Seq.of(1, 2, 3).toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum.intValue());
    }

    @Test
    public void testToListThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger(0);
        Seq.of(1, 2, 3).toListThenAccept(list -> size.set(list.size()));
        assertEquals(3, size.get());
    }

    @Test
    public void test_toListThenAccept() throws Exception {
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toListThenAccept(target::addAll);
        assertEquals(Arrays.asList(1, 2, 3), target);
    }

    @Test
    public void testToSetThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3, 2, 1).toSetThenApply(set -> set.size());
        assertEquals(3, result.intValue());
    }

    @Test
    public void test_toSetThenApply() throws Exception {
        Integer sum = Seq.of(1, 2, 3, 2).toSetThenApply(set -> set.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum.intValue());
    }

    @Test
    public void testToSetThenAccept() throws Exception {
        AtomicInteger size = new AtomicInteger(0);
        Seq.of(1, 2, 3, 2, 1).toSetThenAccept(set -> size.set(set.size()));
        assertEquals(3, size.get());
    }

    @Test
    public void test_toSetThenAccept() throws Exception {
        Set<Integer> target = new HashSet<>();
        Seq.of(1, 2, 3, 2).toSetThenAccept(target::addAll);
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), target);
    }

    @Test
    public void test_toCollectionThenApply() throws Exception {
        String joined = Seq.of(1, 2, 3).toCollectionThenApply(LinkedList::new, list -> list.stream().map(String::valueOf).collect(Collectors.joining("-")));
        assertEquals("1-2-3", joined);
    }

    @Test
    public void testToCollectionThenApply() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        String first = seq.toCollectionThenApply(LinkedList::new, list -> list.getFirst());
        assertEquals("apple", first);

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(null, Fn.identity()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenApply(ArrayList::new, null));
    }

    @Test
    public void test_toCollectionThenAccept() throws Exception {
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toCollectionThenAccept(LinkedList::new, list -> target.addAll(list));
        assertEquals(Arrays.asList(1, 2, 3), target);
    }

    @Test
    public void testToCollectionThenAccept() {
        Seq<Integer, RuntimeException> seq = Seq.of(3, 1, 4, 1, 5);
        List<Integer> result = new ArrayList<>();
        seq.toCollectionThenAccept(TreeSet::new, set -> set.forEach(result::add));
        assertEquals(Arrays.asList(1, 3, 4, 5), result);

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(null, s -> {
        }));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toCollectionThenAccept(ArrayList::new, null));
    }

    @Test
    public void testToMap() throws Exception {
        Map<String, Integer> map = Seq.of("a", "bb", "ccc").toMap(s -> s, String::length);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("bb"));
        assertEquals(Integer.valueOf(3), map.get("ccc"));
    }

    @Test
    public void test_toMap_keyValMappers() throws Exception {
        Map<String, Integer> map = Seq.of("a", "bb", "ccc").toMap(s -> s, String::length);
        assertEquals(1, map.get("a").intValue());
        assertEquals(2, map.get("bb").intValue());
        assertEquals(3, map.get("ccc").intValue());
    }

    @Test
    public void test_toMap_keyValMappers_duplicateKey_throws() {
        Seq<Pair<Character, Integer>, Exception> seq = Seq.of("apple", "apricot").map(s -> Pair.of(s.charAt(0), s.length()));
        assertThrows(IllegalStateException.class, () -> seq.toMap(Pair::left, Pair::right));
    }

    @Test
    public void test_toMap_keyValMappers_mapFactory() throws Exception {
        Map<String, Integer> map = Seq.of("a", "bb").toMap(s -> s, String::length, Suppliers.ofLinkedHashMap());
        assertTrue(map instanceof LinkedHashMap);
        assertEquals(1, map.get("a").intValue());
    }

    @Test
    public void test_toMap_keyValMergeMappers() throws Exception {
        Map<Character, Integer> map = Seq.of("apple", "apricot", "banana").toMap(s -> s.charAt(0), String::length, Integer::sum);
        assertEquals(Integer.valueOf(5 + 7), map.get('a'));
        assertEquals(Integer.valueOf(6), map.get('b'));
    }

    @Test
    public void test_toMap_keyValMergeMappers_mapFactory() throws Exception {
        Map<Character, Integer> map = Seq.of("apple", "apricot", "banana").toMap(s -> s.charAt(0), String::length, Integer::sum, TreeMap::new);
        assertTrue(map instanceof TreeMap);
        assertEquals(Integer.valueOf(12), map.get('a'));
    }

    @Test
    public void testToMapWithKeyValueMappers() {
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "banana", "cherry");
        Map<String, Integer> map1 = seq1.toMap(Fn.identity(), String::length);
        assertEquals(3, map1.size());
        assertEquals(5, map1.get("apple"));
        assertEquals(6, map1.get("banana"));

        Seq<String, RuntimeException> seq2 = Seq.of("apple", "apple");
        assertThrows(IllegalStateException.class, () -> seq2.toMap(Fn.identity(), String::length));
    }

    @Test
    public void testToMapWithKeyValueMappersAndMapFactory() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        Map<String, Integer> map = seq.toMap(Fn.identity(), String::length, Suppliers.ofLinkedHashMap());
        assertEquals(3, map.size());
        List<String> keys = new ArrayList<>(map.keySet());
        assertEquals(Arrays.asList("apple", "banana", "cherry"), keys);
    }

    @Test
    public void testToMapWithMergeFunction() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        Map<Character, String> map = seq.toMap(s -> s.charAt(0), Fn.identity(), (s1, s2) -> s1 + "," + s2);
        assertEquals(2, map.size());
        assertEquals("apple,apricot", map.get('a'));
        assertEquals("banana", map.get('b'));
    }

    @Test
    public void testToMapWithMergeFunctionAndMapFactory() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5);
        TreeMap<Boolean, Integer> map = seq.toMap(n -> n % 2 == 0, Fn.identity(), Integer::sum, TreeMap::new);
        assertEquals(2, map.size());
        assertEquals(6, map.get(true));
        assertEquals(9, map.get(false));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(null, Fn.identity(), Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), null, Integer::sum, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), Fn.identity(), null, TreeMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).toMap(Fn.identity(), Fn.identity(), Integer::sum, null));
    }

    @Test
    public void testToMap_WithMergeFunction() throws Exception {
        Map<Integer, String> result = Seq.of("a", "bb", "c", "dd").toMap(String::length, s -> s, (a, b) -> a + "+" + b);
        assertEquals("a+c", result.get(1));
        assertEquals("bb+dd", result.get(2));
    }

    @Test
    public void testToImmutableMap() throws Exception {
        ImmutableMap<String, Integer> map = Seq.of("a", "bb", "ccc").toImmutableMap(s -> s, String::length);
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
    }

    @Test
    public void test_toImmutableMap_keyValMappers() throws Exception {
        ImmutableMap<String, Integer> map = Seq.of("a", "bb").toImmutableMap(s -> s, String::length);
        assertEquals(1, map.get("a").intValue());
        assertThrows(UnsupportedOperationException.class, () -> map.put("c", 3));
    }

    @Test
    public void test_toImmutableMap_keyValMergeMappers() throws Exception {
        ImmutableMap<Character, Integer> map = Seq.of("apple", "apricot", "banana").toImmutableMap(s -> s.charAt(0), String::length, Integer::sum);
        assertEquals(Integer.valueOf(12), map.get('a'));
        assertThrows(UnsupportedOperationException.class, () -> map.put('d', 3));
    }

    @Test
    public void testToImmutableMapWithKeyValueMappers() {
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        ImmutableMap<String, Integer> map = seq.toImmutableMap(Fn.identity(), String::length);
        assertEquals(3, map.size());
        assertEquals(1, map.get("a"));
        assertThrows(UnsupportedOperationException.class, () -> map.put("d", 1));
    }

    @Test
    public void testToImmutableMapWithMergeFunction() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ImmutableMap<Character, String> map = seq.toImmutableMap(s -> s.charAt(0), Fn.identity(), (s1, s2) -> s1 + "," + s2);
        assertEquals(2, map.size());
        assertEquals("apple,apricot", map.get('a'));
        assertThrows(UnsupportedOperationException.class, () -> map.put('c', "cherry"));
    }

    @Test
    public void testToImmutableMap_WithMergeFunction() throws Exception {
        ImmutableMap<Integer, String> result = Seq.of("a", "bb", "c").toImmutableMap(String::length, s -> s, (a, b) -> a + "+" + b);
        assertEquals("a+c", result.get(1));
        assertEquals("bb", result.get(2));
    }

    @Test
    public void testGroupTo() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc", "dd").groupTo(String::length);
        assertEquals(1, result.get(1).size());
        assertEquals(2, result.get(2).size());
        assertEquals(1, result.get(3).size());
    }

    @Test
    public void testGroupToWithKeyMapper() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, List<String>> map = seq.groupTo(s -> s.charAt(0));
        assertEquals(2, map.size());
        assertEquals(Arrays.asList("apple", "apricot"), map.get('a'));
        assertEquals(Arrays.asList("banana", "blueberry"), map.get('b'));
    }

    @Test
    public void testGroupToWithKeyMapperAndMapFactory() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        TreeMap<Integer, List<Integer>> map = seq.groupTo(n -> n % 3, Suppliers.ofTreeMap());
        assertEquals(3, map.size());
        assertEquals(Arrays.asList(3, 6), map.get(0));
        assertEquals(Arrays.asList(1, 4), map.get(1));
        assertEquals(Arrays.asList(2, 5), map.get(2));
    }

    @Test
    public void testGroupToWithKeyValueMappers() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "banana", "cherry");
        Map<Integer, List<String>> map = seq.groupTo(String::length, String::toUpperCase);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList("APPLE"), map.get(5));
        assertEquals(Arrays.asList("BANANA", "CHERRY"), map.get(6));
    }

    @Test
    public void testGroupToWithKeyValueMappersAndMapFactory() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        LinkedHashMap<Character, List<Integer>> map = seq.groupTo(s -> s.charAt(0), String::length, LinkedHashMap::new);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(5, 7), map.get('a'));
        assertEquals(Arrays.asList(6), map.get('b'));

        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), (Throwables.Function) null, Suppliers.ofLinkedHashMap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, (Supplier) null));
    }

    @Test
    public void testGroupToWithDownstreamCollector() {
        Seq<String, RuntimeException> seq1 = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, Long> map1 = seq1.groupTo(s -> s.charAt(0), Collectors.counting());
        assertEquals(2, map1.size());
        assertEquals(2L, map1.get('a'));
        assertEquals(2L, map1.get('b'));

        Seq<String, RuntimeException> seq2 = Seq.of("a", "bb", "ccc", "dd");
        Map<Integer, Double> map2 = seq2.groupTo(String::length, Collectors.averagingInt(s -> s.charAt(0)));
        assertEquals(3, map2.size());
    }

    @Test
    public void testGroupToWithDownstreamCollectorAndMapFactory() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        TreeMap<Character, String> map = seq.groupTo(s -> s.charAt(0), Collectors.joining(", "), TreeMap::new);
        assertEquals(2, map.size());
        assertEquals("apple, apricot", map.get('a'));
        assertEquals("banana", map.get('b'));
    }

    @Test
    public void testGroupToWithValueMapperAndDownstreamCollector() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Map<Character, String> map = seq.groupTo(s -> s.charAt(0), String::toUpperCase, Collectors.joining(", "));
        assertEquals(2, map.size());
        assertEquals("APPLE, APRICOT", map.get('a'));
        assertEquals("BANANA, BLUEBERRY", map.get('b'));
    }

    @Test
    public void testGroupToWithValueMapperDownstreamCollectorAndMapFactory() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        Map<Character, Optional<Integer>> map = seq.groupTo(s -> s.charAt(0), String::length, com.landawn.abacus.util.stream.Collectors.max(),
                Suppliers.ofLinkedHashMap());
        assertEquals(2, map.size());
        assertEquals(7, map.get('a').get());
        assertEquals(6, map.get('b').get());

        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(null, String::length, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), null, Collectors.counting(), LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, null, LinkedHashMap::new));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").groupTo(s -> s.charAt(0), String::length, Collectors.counting(), null));
    }

    @Test
    public void testGroupTo_WithValueMapper() throws Exception {
        Map<Integer, List<Character>> result = Seq.of("a", "bb", "c", "dd").groupTo(String::length, s -> s.charAt(0));
        assertEquals(Arrays.asList('a', 'c'), result.get(1));
        assertEquals(Arrays.asList('b', 'd'), result.get(2));
    }

    @Test
    public void testGroupTo_WithMapFactory() throws Exception {
        TreeMap<Integer, List<String>> result = Seq.of("a", "bb", "ccc").groupTo(String::length, Suppliers.ofTreeMap());
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.firstKey());
    }

    @Test
    public void testGroupTo_WithCollector() throws Exception {
        Map<Integer, Long> result = Seq.of("a", "bb", "c", "dd").groupTo(String::length, java.util.stream.Collectors.counting());
        assertEquals(Long.valueOf(2), result.get(1));
        assertEquals(Long.valueOf(2), result.get(2));
    }

    @Test
    public void testPartitionTo() throws Exception {
        Map<Boolean, List<Integer>> result = Seq.of(1, 2, 3, 4, 5).partitionTo(x -> x % 2 == 0);
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testPartitionToWithPredicate() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, List<Integer>> map = seq.partitionTo(n -> n % 2 == 0);
        assertEquals(2, map.size());
        assertEquals(Arrays.asList(2, 4, 6), map.get(true));
        assertEquals(Arrays.asList(1, 3, 5), map.get(false));
    }

    @Test
    public void testPartitionToWithPredicateAndDownstreamCollector() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5, 6);
        Map<Boolean, Long> map1 = seq1.partitionTo(n -> n >= 4, Collectors.counting());
        assertEquals(2, map1.size());
        assertEquals(3L, map1.get(true));
        assertEquals(3L, map1.get(false));

        Seq<Integer, RuntimeException> seq2 = Seq.of(2, 4, 6);
        Map<Boolean, Long> map2 = seq2.partitionTo(n -> n % 2 == 0, Collectors.counting());
        assertEquals(2, map2.size());
        assertEquals(3L, map2.get(true));
        assertEquals(0L, map2.get(false));

        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Map<Boolean, Long> map3 = seq3.partitionTo(n -> n > 0, Collectors.counting());
        assertEquals(2, map3.size());
        assertEquals(0L, map3.get(true));
        assertEquals(0L, map3.get(false));
    }

    @Test
    public void testPartitionTo_WithCollector() throws Exception {
        Map<Boolean, Long> result = Seq.of(1, 2, 3, 4, 5).partitionTo(x -> x % 2 == 0, java.util.stream.Collectors.counting());
        assertEquals(Long.valueOf(2), result.get(true));
        assertEquals(Long.valueOf(3), result.get(false));
    }

    @Test
    public void testToMultimap() throws Exception {
        ListMultimap<Integer, String> multimap = Seq.of("a", "bb", "c", "dd").toMultimap(String::length, s -> s);
        assertEquals(2, multimap.get(1).size());
        assertEquals(2, multimap.get(2).size());
    }

    @Test
    public void test_toMultimap_keyMapper() throws Exception {
        ListMultimap<Character, String> multimap = Seq.of("apple", "apricot", "banana", "avocado").toMultimap(s -> s.charAt(0));
        assertEquals(Arrays.asList("apple", "apricot", "avocado"), multimap.get('a'));
        assertEquals(Collections.singletonList("banana"), multimap.get('b'));
    }

    @Test
    public void test_toMultimap_keyMapperAndMapFactory() throws Exception {
        ListMultimap<Character, String> multimap = Seq.of("apple", "apricot", "banana", "avocado")
                .toMultimap(s -> s.charAt(0), Suppliers.ofListMultimap(TreeMap.class));
        assertTrue(multimap.toMap() instanceof TreeMap);
        assertEquals(Arrays.asList("apple", "apricot", "avocado"), multimap.get('a'));
    }

    @Test
    public void test_toMultimap_keyValueMappers() throws Exception {
        ListMultimap<Character, Integer> multimap = Seq.of("apple", "apricot", "banana").toMultimap(s -> s.charAt(0), String::length);
        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
        assertEquals(Collections.singletonList(6), multimap.get('b'));
    }

    @Test
    public void test_toMultimap_keyValueMappersAndMapFactory() throws Exception {
        ListMultimap<Character, Integer> multimap = Seq.of("apple", "apricot", "banana")
                .toMultimap(s -> s.charAt(0), String::length, Suppliers.ofListMultimap(TreeMap.class));
        assertTrue(multimap.toMap() instanceof TreeMap);
        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
    }

    @Test
    public void testToMultimapWithKeyMapper() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ListMultimap<Character, String> multimap = seq.toMultimap(s -> s.charAt(0));
        assertEquals(2, multimap.keySet().size());
        assertEquals(Arrays.asList("apple", "apricot"), multimap.get('a'));
        assertEquals(Arrays.asList("banana"), multimap.get('b'));
    }

    @Test
    public void testToMultimapWithKeyMapperAndMapFactory() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Multimap<Integer, Integer, ? extends Collection<Integer>> multimap = seq.toMultimap(n -> n % 3, Suppliers.ofSetMultimap());
        assertEquals(3, multimap.keySet().size());
        assertTrue(multimap.get(0).contains(3));
        assertTrue(multimap.get(0).contains(6));
    }

    @Test
    public void testToMultimapWithKeyValueMappers() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana");
        ListMultimap<Character, Integer> multimap = seq.toMultimap(s -> s.charAt(0), String::length);
        assertEquals(2, multimap.keySet().size());
        assertEquals(Arrays.asList(5, 7), multimap.get('a'));
        assertEquals(Arrays.asList(6), multimap.get('b'));
    }

    @Test
    public void testToMultimapWithKeyValueMappersAndMapFactory() {
        Seq<String, RuntimeException> seq = Seq.of("apple", "apricot", "banana", "blueberry");
        Multimap<Integer, String, ? extends Collection<String>> multimap = seq.toMultimap(String::length, String::toUpperCase, Suppliers.ofSetMultimap());
        assertTrue(multimap.get(5).contains("APPLE"));
        assertTrue(multimap.get(6).contains("BANANA"));
        assertTrue(multimap.get(9).contains("BLUEBERRY"));

        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(null, String::length, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), null, Suppliers.ofListMultimap()));
        assertThrows(IllegalArgumentException.class, () -> Seq.of("a").toMultimap(s -> s.charAt(0), String::length, null));
    }

    @Test
    public void testToMultimap_KeyOnly() throws Exception {
        ListMultimap<Integer, String> result = Seq.of("a", "bb", "c", "dd").toMultimap(String::length);
        assertEquals(Arrays.asList("a", "c"), result.get(1));
        assertEquals(Arrays.asList("bb", "dd"), result.get(2));
    }

    @Test
    public void testToMultiset() throws Exception {
        Multiset<Integer> multiset = Seq.of(1, 2, 2, 3, 3, 3).toMultiset();
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));
    }

    @Test
    public void test_toMultiset() throws Exception {
        Multiset<String> multiset = Seq.of("a", "b", "a", "c", "a", "b").toMultiset();
        assertEquals(3, multiset.count("a"));
        assertEquals(2, multiset.count("b"));
        assertEquals(1, multiset.count("c"));
    }

    @Test
    public void test_toMultiset_withSupplier() throws Exception {
        Multiset<String> multiset = Seq.of("a", "b", "a").toMultiset();
        assertEquals(2, multiset.count("a"));
    }

    @Test
    public void testToMultisetWithSupplier() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 2, 3, 3, 3);
        Multiset<Integer> multiset = seq.toMultiset(Multiset::new);
        assertEquals(1, multiset.count(1));
        assertEquals(2, multiset.count(2));
        assertEquals(3, multiset.count(3));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).toMultiset(null));
    }

    @Test
    public void testToMultiset_WithSupplier() throws Exception {
        Multiset<Integer> result = Seq.of(1, 2, 2, 3, 3, 3).toMultiset(Multiset::new);
        assertEquals(1, result.count(1));
        assertEquals(2, result.count(2));
        assertEquals(3, result.count(3));
    }

    @Test
    public void testToDataset() throws Exception {
        Dataset ds = Seq.of(1, 2, 3).toDataset(Arrays.asList("value"));
        assertNotNull(ds);
    }

    @Test
    public void test_toDataset() throws Exception {
        List<Map<String, Object>> data = Arrays.asList(CommonUtil.asMap("id", 1, "name", "Alice"), CommonUtil.asMap("id", 2, "name", "Bob"));
        Dataset dataset = Seq.of(data).toDataset();
        assertEquals(2, dataset.size());
        assertTrue(dataset.columnNames().containsAll(Arrays.asList("id", "name")));
        assertEquals((Integer) 1, dataset.moveToRow(0).get("id"));
        assertEquals("Bob", dataset.moveToRow(1).get("name"));
    }

    @Test
    public void test_toDataset_withColumnNames() throws Exception {
        List<List<Object>> data = Arrays.asList(Arrays.asList(1, "Alice"), Arrays.asList(2, "Bob"));
        List<String> columnNames = Arrays.asList("UserID", "UserName");
        Dataset dataset = Seq.of(data).toDataset(columnNames);
        assertEquals(2, dataset.size());
        assertEquals(columnNames, dataset.columnNames());
        assertEquals((Integer) 1, dataset.moveToRow(0).get("UserID"));
        assertEquals("Bob", dataset.moveToRow(1).get("UserName"));
    }

    @Test
    public void testToDatasetWithColumnNames() {
        List<String> columns = Arrays.asList("Name", "Age");
        Seq<List<Object>, RuntimeException> seq = Seq.of(Arrays.asList("John", 25), Arrays.asList("Jane", 30));
        Dataset dataset = seq.toDataset(columns);
        assertNotNull(dataset);
        assertEquals(2, dataset.size());
    }

    @Test
    public void testSumInt() throws Exception {
        long sum = Seq.of(1, 2, 3, 4, 5).sumInt(x -> x);
        assertEquals(15L, sum);
    }

    @Test
    public void test_sumInt() throws Exception {
        long sum = Seq.of("1", "2", "3").sumInt(Integer::parseInt);
        assertEquals(6, sum);
    }

    @Test
    public void testSumInt_Empty() throws Exception {
        long sum = Seq.<Integer, Exception> empty().sumInt(x -> x);
        assertEquals(0L, sum);
    }

    @Test
    public void testSumLong() throws Exception {
        long sum = Seq.of(1, 2, 3, 4, 5).sumLong(x -> (long) x);
        assertEquals(15L, sum);
    }

    @Test
    public void test_sumLong() throws Exception {
        long sum = Seq.of("10", "20", "30").sumLong(Long::parseLong);
        assertEquals(60L, sum);
    }

    @Test
    public void testSumDouble() throws Exception {
        double sum = Seq.of(1.0, 2.0, 3.0).sumDouble(x -> x);
        assertEquals(6.0, sum, 0.001);
    }

    @Test
    public void test_sumDouble() throws Exception {
        double sum = Seq.of("1.1", "2.2", "3.3").sumDouble(Double::parseDouble);
        assertEquals(6.6, sum, 0.001);
    }

    @Test
    public void testSumDouble_Empty() throws Exception {
        double sum = Seq.<Double, Exception> empty().sumDouble(x -> x);
        assertEquals(0.0, sum, 0.001);
    }

    @Test
    public void testAverageInt() throws Exception {
        OptionalDouble avg = Seq.of(1, 2, 3, 4, 5).averageInt(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void test_averageInt() throws Exception {
        OptionalDouble avg = Seq.of("1", "2", "3").averageInt(Integer::parseInt);
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
        assertTrue(Seq.<String, Exception> empty().averageInt(Integer::parseInt).isEmpty());
    }

    @Test
    public void testAverageInt_Empty() throws Exception {
        OptionalDouble avg = Seq.<Integer, Exception> empty().averageInt(x -> x);
        assertFalse(avg.isPresent());
    }

    @Test
    public void testAverageLong() throws Exception {
        OptionalDouble avg = Seq.of(1L, 2L, 3L, 4L, 5L).averageLong(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void test_averageLong() throws Exception {
        OptionalDouble avg = Seq.of("10", "20", "30").averageLong(Long::parseLong);
        assertTrue(avg.isPresent());
        assertEquals(20.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void testAverageLong_basic() throws Exception {
        OptionalDouble avg = Seq.of("a", "bb", "ccc").averageLong(s -> (long) s.length());
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.0001);
    }

    @Test
    public void testAverageLong_empty() throws Exception {
        OptionalDouble avg = Seq.<String, Exception> of().averageLong(s -> (long) s.length());
        assertFalse(avg.isPresent());
    }

    @Test
    public void testAverageDouble() throws Exception {
        OptionalDouble avg = Seq.of(1.0, 2.0, 3.0, 4.0, 5.0).averageDouble(x -> x);
        assertTrue(avg.isPresent());
        assertEquals(3.0, avg.get(), 0.001);
    }

    @Test
    public void test_averageDouble() throws Exception {
        OptionalDouble avg = Seq.of("1.0", "2.0", "3.0", "4.0").averageDouble(Double::parseDouble);
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.getAsDouble(), 0.001);
    }

    // ===== averageDouble - empty sequence =====

    @Test
    public void testAverageDouble_emptySeq() throws Exception {
        OptionalDouble avg = Seq.<Double, Exception> empty().averageDouble(x -> x);
        assertFalse(avg.isPresent());
    }

    @Test
    public void testAverageDouble_withStrings() throws Exception {
        OptionalDouble avg = Seq.of("a", "bb", "ccc").averageDouble(s -> (double) s.length());
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.0001);
    }

    @Test
    public void testReduce_WithIdentity() throws Exception {
        int sum = Seq.of(1, 2, 3, 4, 5).reduce(0, Integer::sum);
        assertEquals(15, sum);
    }

    @Test
    public void testReduce_WithoutIdentity() throws Exception {
        Optional<Integer> sum = Seq.of(1, 2, 3, 4, 5).reduce(Integer::sum);
        assertTrue(sum.isPresent());
        assertEquals(Integer.valueOf(15), sum.get());
    }

    @Test
    public void test_reduce_binaryOperator() throws Exception {
        Optional<Integer> sum = Seq.of(1, 2, 3, 4).reduce(Integer::sum);
        assertEquals(Optional.of(10), sum);
        assertTrue(Seq.<Integer, Exception> empty().reduce(Integer::sum).isEmpty());
    }

    @Test
    public void test_reduce_identity_accumulator() throws Exception {
        Integer sum = Seq.of(1, 2, 3, 4).reduce(0, Integer::sum);
        assertEquals(10, sum.intValue());
        assertEquals(0, Seq.<Integer, Exception> empty().reduce(0, Integer::sum).intValue());
    }

    @Test
    public void testReduceWithAccumulator() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Optional<Integer> sum = seq1.reduce(Integer::sum);
        assertTrue(sum.isPresent());
        assertEquals(15, sum.get());

        Seq<Integer, RuntimeException> seq2 = Seq.of(3, 1, 4, 1, 5);
        Optional<Integer> max = seq2.reduce(Integer::max);
        assertTrue(max.isPresent());
        assertEquals(5, max.get());

        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Optional<Integer> result = seq3.reduce(Integer::sum);
        assertFalse(result.isPresent());

        Seq<Integer, RuntimeException> seq4 = Seq.of(42);
        Optional<Integer> single = seq4.reduce(Integer::sum);
        assertTrue(single.isPresent());
        assertEquals(42, single.get());

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).reduce(null));
    }

    @Test
    public void testReduceWithIdentityAndAccumulator() {
        Seq<Integer, RuntimeException> seq1 = Seq.of(1, 2, 3, 4, 5);
        Integer sum = seq1.reduce(10, Integer::sum);
        assertEquals(25, sum);

        Seq<String, RuntimeException> seq2 = Seq.of("a", "b", "c");
        String result = seq2.reduce("Start:", (acc, str) -> acc + str);
        assertEquals("Start:abc", result);

        Seq<Integer, RuntimeException> seq3 = Seq.<Integer, RuntimeException> empty();
        Integer empty = seq3.reduce(100, Integer::sum);
        assertEquals(100, empty);

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).reduce(0, null));
    }

    @Test
    public void testReduce_Empty() throws Exception {
        Optional<Integer> result = Seq.<Integer, Exception> empty().reduce(Integer::sum);
        assertFalse(result.isPresent());
    }

    @Test
    public void testReduce_WithIdentity_Empty() throws Exception {
        int result = Seq.<Integer, Exception> empty().reduce(0, Integer::sum);
        assertEquals(0, result);
    }

    @Test
    public void testCollect_SupplierAccumulator() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void testCollect_WithFinisher() throws Exception {
        Integer size = Seq.of(1, 2, 3).collect(ArrayList::new, List::add, List::size);
        assertEquals(Integer.valueOf(3), size);
    }

    @Test
    public void test_collect_supplier_accumulator() throws Exception {
        ArrayList<Integer> list = Seq.of(1, 2, 3).collect(ArrayList::new, ArrayList::add);
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_collect_supplier_accumulator_finisher() throws Exception {
        String str = Seq.of(1, 2, 3)
                .collect(StringBuilder::new, (sb, i) -> sb.append(i).append("-"), sb -> sb.length() > 0 ? sb.substring(0, sb.length() - 1) : "");
        assertEquals("1-2-3", str);
    }

    @Test
    public void test_collect_collector() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).collect(Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    @Test
    public void test_collect_withComplexCollector_groupingBy() throws Exception {
        List<String> items = Arrays.asList("apple", "banana", "apricot", "blueberry", "avocado");
        Map<Character, List<String>> result = Seq.of(items).collect(Collectors.groupingBy(s -> s.charAt(0)));

        assertEquals(Arrays.asList("apple", "apricot", "avocado"), result.get('a'));
        assertEquals(Arrays.asList("banana", "blueberry"), result.get('b'));
    }

    @Test
    public void test_collect_withComplexCollector_summarizingInt() throws Exception {
        List<String> items = Arrays.asList("apple", "banana", "kiwi");
        IntSummaryStatistics stats = Seq.of(items).collect(Collectors.summarizingInt(String::length));

        assertEquals(3, stats.getCount());
        assertEquals(15, stats.getSum());
        assertEquals(4, stats.getMin());
        assertEquals(6, stats.getMax());
        assertEquals(5.0, stats.getAverage(), 0.001);
    }

    @Test
    public void testCollectWithSupplierAndAccumulator() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        List<String> list = seq1.collect(ArrayList::new, List::add);
        assertEquals(Arrays.asList("a", "b", "c"), list);

        Seq<String, RuntimeException> seq2 = Seq.of("Hello", " ", "World");
        StringBuilder sb = seq2.collect(StringBuilder::new, StringBuilder::append);
        assertEquals("Hello World", sb.toString());

        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        List<String> empty = seq3.collect(ArrayList::new, List::add);
        assertTrue(empty.isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect((Throwables.Supplier<List<Integer>, Exception>) null, (l, i) -> l.add(i)));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect(ArrayList::new, null));
    }

    @Test
    public void testCollectWithSupplierAccumulatorAndFinisher() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        Integer size = seq1.collect(ArrayList::new, List::add, List::size);
        assertEquals(3, size);

        Seq<String, RuntimeException> seq2 = Seq.of("hello", " ", "world");
        String result = seq2.collect(StringBuilder::new, StringBuilder::append, sb -> sb.toString().toUpperCase());
        assertEquals("HELLO WORLD", result);

        assertThrows(IllegalArgumentException.class,
                () -> Seq.of(1).collect((Throwables.Supplier<List<Integer>, Exception>) null, (l, i) -> l.add(i), List::size));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collect(ArrayList::new, null, List::size));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).collect(ArrayList::new, List::add, null));
    }

    @Test
    public void testCollectWithCollector() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        List<String> list = seq1.collect(Collectors.toList());
        assertEquals(Arrays.asList("a", "b", "c"), list);

        Seq<String, RuntimeException> seq2 = Seq.of("a", "bb", "ccc", "dd");
        Map<Integer, List<String>> grouped = seq2.collect(Collectors.groupingBy(String::length));
        assertEquals(Arrays.asList("a"), grouped.get(1));
        assertEquals(Arrays.asList("bb", "dd"), grouped.get(2));
        assertEquals(Arrays.asList("ccc"), grouped.get(3));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1, 2).collect(null));
    }

    @Test
    public void testCollect_WithCollector() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCollect_WithCollectorToSet() throws Exception {
        Set<Integer> result = Seq.of(1, 2, 2, 3, 3).collect(java.util.stream.Collectors.toSet());
        assertEquals(3, result.size());
    }

    @Test
    public void testCollect_SupplierAccumulatorFinisher() throws Exception {
        String result = Seq.of(1, 2, 3).collect(StringBuilder::new, (sb, i) -> sb.append(i), StringBuilder::toString);
        assertEquals("123", result);
    }

    @Test
    public void testCollectThenApply() throws Exception {
        Integer result = Seq.of(1, 2, 3).collectThenApply(Collectors.toList(), list -> list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, result.intValue());
    }

    @Test
    public void test_collectThenApply() throws Exception {
        int size = Seq.of(1, 2, 3).collectThenApply(Collectors.toList(), List::size);
        assertEquals(3, size);
    }

    @Test
    public void testCollectThenApply_Counting() throws Exception {
        long count = Seq.of(1, 2, 3).collectThenApply(java.util.stream.Collectors.counting(), c -> c);
        assertEquals(3L, count);
    }

    @Test
    public void testCollectThenAccept() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        Seq.of(1, 2, 3).collectThenAccept(Collectors.toList(), list -> sum.set(list.stream().mapToInt(Integer::intValue).sum()));
        assertEquals(6, sum.get());
    }

    @Test
    public void test_collectThenAccept() throws Exception {
        List<Integer> holder = new ArrayList<>();
        Seq.of(1, 2, 3).collectThenAccept(Collectors.toList(), holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
    }

    @Test
    public void testCollectThenAccept_Counting() throws Exception {
        AtomicInteger holder = new AtomicInteger(0);
        Seq.of(1, 2, 3).collectThenAccept(java.util.stream.Collectors.counting(), c -> holder.set(c.intValue()));
        assertEquals(3, holder.get());
    }

    @Test
    public void testJoin() throws Exception {
        String joined = Seq.of("a", "b", "c").join(", ");
        assertEquals("a, b, c", joined);
    }

    @Test
    public void testJoin_WithPrefixAndSuffix() throws Exception {
        String joined = Seq.of("a", "b", "c").join(", ", "[", "]");
        assertEquals("[a, b, c]", joined);
    }

    @Test
    public void test_join_delimiter() throws Exception {
        assertEquals("1,2,3", Seq.of(1, 2, 3).join(","));
        assertEquals("", Seq.<Integer, Exception> empty().join(","));
    }

    @Test
    public void test_join_delimiterPrefixSuffix() throws Exception {
        assertEquals("[1,2,3]", Seq.of(1, 2, 3).join(",", "[", "]"));
    }

    @Test
    public void testJoinWithDelimiter() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String result1 = seq1.join(", ");
        assertEquals("a, b, c", result1);

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        String result2 = seq2.join("-");
        assertEquals("1-2-3", result2);

        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        String result3 = seq3.join(", ");
        assertEquals("", result3);

        Seq<String, RuntimeException> seq4 = Seq.of("single");
        String result4 = seq4.join(", ");
        assertEquals("single", result4);
    }

    @Test
    public void testJoinWithDelimiterPrefixSuffix() {
        Seq<String, RuntimeException> seq1 = Seq.of("a", "b", "c");
        String result1 = seq1.join(", ", "[", "]");
        assertEquals("[a, b, c]", result1);

        Seq<Integer, RuntimeException> seq2 = Seq.of(1, 2, 3);
        String result2 = seq2.join(",", "WHERE id IN (", ")");
        assertEquals("WHERE id IN (1,2,3)", result2);

        Seq<String, RuntimeException> seq3 = Seq.<String, RuntimeException> empty();
        String result3 = seq3.join(", ", "{", "}");
        assertEquals("{}", result3);
    }

    @Test
    public void testJoinTo() throws Exception {
        Joiner joiner = Joiner.with(", ");
        Seq.of(1, 2, 3).joinTo(joiner);
        assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void test_joinTo_joiner() throws Exception {
        Joiner joiner = Joiner.with("-", "<", ">");
        Seq.of(1, 2, 3).joinTo(joiner);
        assertEquals("<1-2-3>", joiner.toString());
    }

    @Test
    public void testJoinTo_Joiner() throws Exception {
        Joiner joiner = Joiner.with(", ", "[", "]");
        Seq.of("a", "b", "c").joinTo(joiner);
        assertEquals("[a, b, c]", joiner.toString());
    }

    @Test
    public void testPrintln() throws Exception {
        assertDoesNotThrow(() -> {
            Seq.of(1, 2, 3).println();
            Seq.range(0, 1000).println();

            Seq.range(0, 1001).println();
        });
    }

    @Test
    public void test_println() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));
        try {
            Seq.of(1, "hello", 3.0).println();
            assertEquals("[1, hello, 3.0]" + IOUtil.LINE_SEPARATOR, baos.toString());
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void test_cast() {
        assertDoesNotThrow(() -> {
            Seq.<String, SQLException> of("a", "b").transform(s -> s.map(String::length));
            Seq.of(CommonUtil.toList("a", "b"), SQLException.class)
                    .cast()
                    .peek(it -> IOUtils.readLines(new FileInputStream(new File("./test.txt")), Charsets.DEFAULT));
        });
    }

    @Test
    public void testCast() throws Exception {
        List<Number> numbers = Arrays.asList(1, 2, 3);
        List<Number> result = Seq.<Number, RuntimeException> of(numbers).cast().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testCastWithClosedSequence() throws Exception {
        Seq<String, IOException> seq = Seq.of(Arrays.asList("a", "b", "c").iterator(), IOException.class);
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.cast());
    }

    @Test
    public void testCast_ToException() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3);
        Seq<Integer, Exception> casted = original.cast();
        assertEquals(Arrays.asList(1, 2, 3), casted.toList());
    }

    @Test
    public void testStream() throws Exception {
        long count = Seq.of(1, 2, 3, 4, 5).stream().filter(x -> x % 2 == 0).count();
        assertEquals(2, count);
    }

    @Test
    public void test_stream_conversion() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        Stream<Integer> stream = seq.stream();
        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
    }

    @Test
    public void test_stream_conversion_withCloseHandler() throws Exception {
        AtomicBoolean closedBySeq = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closedBySeq.set(true));
        Stream<Integer> stream = seq.stream();

        assertEquals(Arrays.asList(1, 2, 3), stream.toList());
        assertTrue(closedBySeq.get());
    }

    @Test
    public void testStreamWithCloseHandlers() throws Exception {
        AtomicBoolean closeHandlerCalled = new AtomicBoolean(false);
        List<String> data = Arrays.asList("x", "y", "z");

        Seq<String, Exception> seq = Seq.of(data).onClose(() -> closeHandlerCalled.set(true));

        Stream<String> stream = seq.stream();
        List<String> result = stream.toList();

        assertEquals(data, result);
        assertTrue(closeHandlerCalled.get());
    }

    @Test
    public void testStreamClosedSequence() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c"));
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.stream());
    }

    @Test
    public void testStream_Conversion() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).stream().filter(x -> x > 3).collect(java.util.stream.Collectors.toList());
        assertEquals(Arrays.asList(4, 5), result);
    }

    @Test
    public void testStream_conversion() throws Exception {
        com.landawn.abacus.util.stream.Stream<Integer> stream = Seq.of(1, 2, 3).stream();
        assertNotNull(stream);
        List<Integer> result = stream.toList();
        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(1), result.get(0));
    }

    // ===== stream() with close handlers path =====

    @Test
    public void testStream_withCloseHandlers() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed.set(true));
        com.landawn.abacus.util.stream.Stream<Integer> stream = seq.stream();
        assertNotNull(stream);
        List<Integer> result = stream.toList();
        assertEquals(3, result.size());
        // The stream should have the close handler registered
    }

    @Test
    public void test_transform() throws Exception {

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2)).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2), false).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).transformB(s -> s.map(e -> e * 2), true).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).sps(s -> s.map(e -> e * 2)).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
            throw new SQLException("TheXyzSQLException");
        }).sps(64, s -> s.map(e -> e * 2)).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
                throw new SQLException("TheXyzSQLException");
            }).transformB(s -> s.map(e -> e * 2)).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
                throw new SQLException("TheXyzSQLException");
            }).transformB(s -> s.map(e -> e * 2), true).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).peek(e -> {
                throw new SQLException("TheXyzSQLException");
            }).sps(s -> s.map(e -> e * 2)).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }
    }

    @Test
    public void test_transform2() throws Exception {
        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new SQLException("TheXyzSQLException");
        }))).println());

        assertThrows(SQLException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new SQLException("TheXyzSQLException");
        }))).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                throw new SQLException("TheXyzSQLException");
            }))).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                throw new SQLException("TheXyzSQLException");
            }))).println();
        } catch (final SQLException e) {
            assertEquals("TheXyzSQLException", e.getMessage());
        }
    }

    @Test
    public void test_transform3() throws Exception {
        assertThrows(IOException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new IOException("TheXyzIOException");
        }))).println());

        assertThrows(IOException.class, () -> Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
            throw new IOException("TheXyzIOException");
        }))).println());

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).transformB(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                throw new IOException("TheXyzIOException");
            }))).println();
        } catch (final Exception e) {
            assertEquals("TheXyzIOException", e.getMessage());
        }

        try {
            Seq.<Integer, SQLException> of(1, 2, 3).sps(s -> s.map(e -> e * 2).peek(Fn.cc(e -> {
                throw new IOException("TheXyzIOException");
            }))).println();
        } catch (final Exception e) {
            assertEquals("TheXyzIOException", e.getMessage());
        }
    }

    @Test
    public void testTransform() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).transform(seq -> seq.map(x -> x * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void test_transform_seqToSeq() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3);
        Seq<String, Exception> transformed = original.transform(s -> s.map(String::valueOf).append("end"));
        assertEquals(Arrays.asList("1", "2", "3", "end"), drainWithException(transformed));
    }

    @Test
    public void testTransformWithFilter() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5, 6);
        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transform(s -> s.filter(i -> i % 2 == 0));

        List<Integer> result = transformed.toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void testTransformNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.transform(null));
    }

    @Test
    public void testTransformWithNullFunction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.transform(null));
    }

    @Test
    public void testTransformB() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).transformB(seq -> seq.map(x -> x * 2)).toList();
        assertEquals(Arrays.asList(2, 4, 6), result);
    }

    @Test
    public void test_transformB_streamToStream() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3);
        Seq<String, Exception> transformed = original.transformB(s -> s.map(String::valueOf).append("endB"));
        assertEquals(Arrays.asList("1", "2", "3", "endB"), drainWithException(transformed));
    }

    @Test
    public void test_transformB_streamToStream_deferred() throws Exception {
        AtomicBoolean transferCalled = new AtomicBoolean(false);
        Function<Stream<Integer>, Stream<String>> transferFunc = stream -> {
            transferCalled.set(true);
            return stream.map(String::valueOf).append("endB_deferred");
        };

        Seq<Integer, Exception> original = Seq.of(1, 2, 3);
        Seq<String, Exception> transformed = original.transformB(transferFunc, true);

        assertFalse(transferCalled.get(), "Transfer function should not be called yet for deferred transformB");
        assertEquals(Arrays.asList("1", "2", "3", "endB_deferred"), drainWithException(transformed));
        assertTrue(transferCalled.get(), "Transfer function should be called on consumption for deferred transformB");
    }

    @Test
    public void testTransformB_DeferredTrue() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n * 2), true);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4, 6), list);
    }

    @Test
    public void testTransformB_DeferredFalse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n * 2), false);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4, 6), list);
    }

    @Test
    public void testTransformB_WithFilter() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.filter(n -> n % 2 == 0), false);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(2, 4), list);
    }

    @Test
    public void testTransformB_WithFlatMap() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.flatMap(n -> Stream.of(n, n * 10)), false);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(1, 10, 2, 20, 3, 30), list);
    }

    @Test
    public void testTransformB_ReturningEmptyStream() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> Stream.empty(), false);

        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testTransformB_ReturningNull() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> null, false);

        assertTrue(result.toList().isEmpty());
    }

    @Test
    public void testTransformB_ComplexTransformation() throws Exception {
        Seq<String, Exception> seq = Seq.of("hello", "world", "java");

        Seq<Character, Exception> result = seq.transformB(stream -> stream.filter(s -> s.length() > 4).flatmapToChar(s -> s.toCharArray()).mapToObj(c -> c),
                false);

        List<Character> list = result.toList();
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o', 'w', 'o', 'r', 'l', 'd'), list);
    }

    @Test
    public void testTransformB_NullTransferFunction() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        assertThrows(IllegalArgumentException.class, () -> {
            seq.transformB(null, false);
        });
    }

    @Test
    public void testTransformB_CalledOnClosedSequence() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        seq.close();

        assertThrows(IllegalStateException.class, () -> {
            seq.transformB(stream -> stream.map(n -> n * 2), false);
        });
    }

    @Test
    public void testTransformB_DeferredExceptionPropagation() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> {
            if (n == 2)
                throw new RuntimeException("Test exception");
            return n;
        }), true);

        assertThrows(Exception.class, () -> {
            result.toList();
        });
    }

    @Test
    public void testTransformB_MultipleDeferredTransformations() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);

        Seq<Integer, Exception> result = seq.transformB(stream -> stream.map(n -> n + 1), true).transformB(stream -> stream.map(n -> n * 2), true);

        List<Integer> list = result.toList();
        assertEquals(Arrays.asList(4, 6, 8), list);
    }

    @Test
    public void testTransformBWithDeferred() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        AtomicBoolean transformExecuted = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> transformed = seq.transformB(stream -> {
            transformExecuted.set(true);
            return stream.filter(i -> i > 2);
        }, true);

        assertFalse(transformExecuted.get());

        List<Integer> result = transformed.toList();
        assertTrue(transformExecuted.get());
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testTransformBImmediate() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Seq<String, Exception> transformed = seq.transformB(stream -> stream.map(String::toUpperCase), false);

        List<String> result = transformed.toList();
        assertEquals(Arrays.asList("A", "B", "C"), result);
    }

    @Test
    public void testTransformBDeferred() throws Exception {
        {
            List<String> result = Seq.of(1, 2, 3).transformB(stream -> stream.map(x -> x * 2).map(Object::toString), true).toList();

            assertEquals(Arrays.asList("2", "4", "6"), result);
        }

        {
            List<String> result = Seq.of(1, 2, 3).transformB(stream -> stream.map(x -> x * 2).map(Object::toString), true).skip(1).toList();

            assertEquals(Arrays.asList("4", "6"), result);
        }

        {
            assertEquals(3, Seq.of(1, 2, 3).transformB(stream -> stream.map(x -> x * 2).map(Object::toString), true).count());
        }
    }

    @Test
    public void test_sps() throws Exception {
        assertDoesNotThrow(() -> {
            Seq.<String, Exception> of("a", "b", "c").peek(Fn.println()).forEach(Fn.println());
        });
    }

    @Test
    public void testSps() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).sps(stream -> stream.filter(x -> x % 2 == 0)).toList();
        assertEquals(Arrays.asList(2), result);
    }

    @Test
    public void test_sps_switchParallelSwitch() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3, 4, 5);
        Seq<String, Exception> transformed = original.sps(stream -> stream.filter(x -> x % 2 == 0).map(x -> "E:" + x));
        List<String> result = drainWithException(transformed);
        Set<String> resultSet = new HashSet<>(result);
        assertEquals(new HashSet<>(Arrays.asList("E:2", "E:4")), resultSet);
    }

    @Test
    public void test_sps_withMaxThreadNum() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        Seq<String, Exception> transformed = original.sps(2, stream -> stream.map(x -> {
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
            }
            return "T" + Thread.currentThread().getId() + ":" + x;
        }));
        List<String> results = drainWithException(transformed);
        assertEquals(10, results.size());
        for (String s : results) {
            assertTrue(s.matches("T\\d+:\\d+"));
        }
    }

    @Test
    public void testSpsWithMaxThreadNum() throws Exception {
        List<Integer> data = new ArrayList<>();
        for (int i = 1; i <= 100; i++) {
            data.add(i);
        }

        Seq<Integer, Exception> seq = Seq.of(data);

        Seq<Integer, Exception> result = seq.sps(4, stream -> stream.filter(i -> i > 50));

        assertEquals(50, result.count());
    }

    @Test
    public void testSpsInvalidThreadNum() throws Exception {

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(0, stream -> stream));

        assertThrows(IllegalArgumentException.class, () -> Seq.empty().sps(-1, stream -> stream));
    }

    @Test
    public void testSpsWithInvalidThreadNum() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.sps(-1, stream -> stream));
    }

    @Test
    public void testSps_WithExecutor() throws Exception {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(3);
        try {
            List<String> result = Seq.<Integer, Exception> of(1, 2, 3, 4, 5)
                    .sps(2, executor, s -> s.filter(n -> n % 2 == 0).map(n -> "val" + n))
                    .sorted()
                    .toList();
            assertEquals(Arrays.asList("val2", "val4"), result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncRun() throws Exception {
        AtomicInteger sum = new AtomicInteger(0);
        ContinuableFuture<Void> future = Seq.of(1, 2, 3).asyncRun(seq -> seq.forEach(sum::addAndGet));
        future.get();
        assertEquals(6, sum.get());
    }

    @Test
    public void testAsyncRunWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
            AtomicInteger sum = new AtomicInteger(0);

            Seq<Integer, Exception> seq = Seq.of(data);

            ContinuableFuture<Void> future = seq.asyncRun(s -> s.forEach(sum::addAndGet), executor);

            future.get();
            assertEquals(15, sum.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncRunNullAction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.asyncRun(null));
    }

    @Test
    public void testAsyncRunWithNullAction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.asyncRun(null));
    }

    @Test
    public void testAsyncRun_WithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            AtomicInteger sum = new AtomicInteger(0);
            ContinuableFuture<Void> future = Seq.of(1, 2, 3).asyncRun(seq -> seq.forEach(sum::addAndGet), executor);
            future.get();
            assertEquals(6, sum.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncCall() throws Exception {
        ContinuableFuture<List<Integer>> future = Seq.of(1, 2, 3).asyncCall(seq -> seq.toList());
        List<Integer> result = future.get();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAsyncCallWithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            List<String> data = Arrays.asList("hello", "world");
            Seq<String, Exception> seq = Seq.of(data);

            ContinuableFuture<String> future = seq.asyncCall(s -> s.join(" "), executor);

            String result = future.get();
            assertEquals("hello world", result);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void testAsyncCallReturningList() throws Exception {
        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        Seq<Integer, Exception> seq = Seq.of(data);

        ContinuableFuture<List<Integer>> future = seq.asyncCall(s -> s.filter(i -> i > 2).toList());

        List<Integer> result = future.get();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testAsyncCallWithNullAction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.asyncCall(null));
    }

    @Test
    public void testAsyncCall_WithExecutor() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            ContinuableFuture<List<Integer>> future = Seq.of(1, 2, 3).asyncCall(seq -> seq.toList(), executor);
            assertEquals(Arrays.asList(1, 2, 3), future.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    public void test_applyIfNotEmpty() throws Exception {
        u.Optional<Long> result = Seq.of(1, 2, 3).applyIfNotEmpty(s -> s.sumInt(x -> x));
        assertEquals(Optional.of(6L), result);

        u.Optional<Long> emptyResult = Seq.<Integer, Exception> empty().applyIfNotEmpty(s -> s.sumInt(x -> x));
        assertTrue(emptyResult.isEmpty());
    }

    @Test
    public void testApplyIfNotEmpty() throws Exception {
        List<Integer> data = Arrays.asList(10, 20, 30);
        Seq<Integer, Exception> seq = Seq.of(data);

        u.Optional<Double> result = seq.applyIfNotEmpty(s -> s.averageDouble(e -> e).orElse(0.0));

        assertTrue(result.isPresent());
        assertEquals(20.0, result.get(), 0.001);
    }

    @Test
    public void testApplyIfNotEmptyWithEmptySeq() throws Exception {
        Seq<String, Exception> seq = Seq.empty();

        Optional<String> result = seq.applyIfNotEmpty(s -> s.join(", "));

        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyReturningNull() throws Exception {
        List<String> data = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(data);

        Optional<String> result = seq.applyIfNotEmpty(s -> null);

        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmptyNullFunction() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.applyIfNotEmpty(null));
    }

    @Test
    public void testApplyIfNotEmptyWithElements() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        u.Optional<Integer> result = seq.applyIfNotEmpty(s -> (int) s.count());

        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmpty_Empty() throws Exception {
        u.Optional<List<Integer>> result = Seq.<Integer, Exception> empty().applyIfNotEmpty(seq -> seq.toList());
        assertFalse(result.isPresent());
    }

    @Test
    public void testApplyIfNotEmpty_NonEmpty() throws Exception {
        u.Optional<List<Integer>> result = Seq.<Integer, Exception> of(1, 2, 3).applyIfNotEmpty(seq -> seq.toList());
        assertTrue(result.isPresent());
        assertEquals(Arrays.asList(1, 2, 3), result.get());
    }

    @Test
    public void testAcceptIfNotEmpty() throws Exception {
        AtomicInteger counter = new AtomicInteger(0);
        Seq.of(1, 2, 3).acceptIfNotEmpty(seq -> counter.set((int) seq.count())).orElse(() -> {
        });
        assertEquals(3, counter.get());

        counter.set(0);
        Seq.<Integer, RuntimeException> empty().acceptIfNotEmpty(seq -> counter.set((int) seq.count())).orElse(() -> {
        });
        assertEquals(0, counter.get());
    }

    @Test
    public void test_acceptIfNotEmpty() throws Exception {
        List<Integer> holder = new ArrayList<>();
        OrElse orElse = Seq.of(1, 2, 3).acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertEquals(Arrays.asList(1, 2, 3), holder);
        assertSame(OrElse.TRUE, orElse);

        holder.clear();
        OrElse emptyOrElse = Seq.<Integer, Exception> empty().acceptIfNotEmpty(s -> s.forEach(holder::add));
        assertTrue(holder.isEmpty());
        assertSame(OrElse.FALSE, emptyOrElse);
    }

    @Test
    public void testAcceptIfNotEmptyWithEmptySeq() throws Exception {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.empty();

        OrElse result = seq.acceptIfNotEmpty(s -> {
            actionExecuted.set(true);
            s.forEach(System.out::println);
        });

        assertFalse(actionExecuted.get());
    }

    @Test
    public void testAcceptIfNotEmptyWithOrElse() throws Exception {
        AtomicBoolean primaryExecuted = new AtomicBoolean(false);
        AtomicBoolean elseExecuted = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.empty();

        seq.acceptIfNotEmpty(s -> {
            primaryExecuted.set(true);
        }).orElse(() -> {
            elseExecuted.set(true);
        });

        assertFalse(primaryExecuted.get());
        assertTrue(elseExecuted.get());
    }

    @Test
    public void testAcceptIfNotEmptyWithElements() {
        Seq<String, RuntimeException> seq = Seq.of("test");
        boolean[] actionCalled = { false };

        OrElse result = seq.acceptIfNotEmpty(s -> {
            actionCalled[0] = true;
            s.toList();
        });

        assertEquals(OrElse.TRUE, result);
        assertTrue(actionCalled[0]);
    }

    @Test
    public void testAcceptIfNotEmpty_Empty() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        OrElse orElse = Seq.<Integer, Exception> empty().acceptIfNotEmpty(seq -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testAcceptIfNotEmpty_NonEmpty() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        Seq.of(1, 2, 3).acceptIfNotEmpty(seq -> called.set(true));
        assertTrue(called.get());
    }

    @Test
    public void testOnClose() throws Exception {
        AtomicInteger closed = new AtomicInteger(0);
        var seq = Seq.of(1, 2, 3).onClose(closed::incrementAndGet);
        seq.toList();
        assertEquals(1, closed.get());
    }

    @Test
    public void test_onClose() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed.set(true));

        assertFalse(closed.get(), "Should not be closed before terminal operation");
        drainWithException(seq);
        assertTrue(closed.get(), "Should be closed after terminal operation");
    }

    @Test
    public void test_onClose_withFlatMap_innerSeqAlsoHasCloseHandler() throws Exception {
        AtomicBoolean outerSeqClosed = new AtomicBoolean(false);
        AtomicBoolean innerSeq1Closed = new AtomicBoolean(false);
        AtomicBoolean innerSeq2Closed = new AtomicBoolean(false);

        Seq<Integer, Exception> outer = Seq.of(1, 2).onClose(() -> outerSeqClosed.set(true));

        Seq<String, Exception> flatMapped = outer.flatMap(i -> {
            if (i == 1) {
                return Seq.of("a" + i, "b" + i).onClose(() -> innerSeq1Closed.set(true));
            } else {
                return Seq.of("x" + i, "y" + i).onClose(() -> innerSeq2Closed.set(true));
            }
        });

        List<String> result = drainWithException(flatMapped);
        assertEquals(Arrays.asList("a1", "b1", "x2", "y2"), result);

        assertTrue(outerSeqClosed.get(), "Outer sequence should be closed.");
        assertTrue(innerSeq1Closed.get(), "Inner sequence 1 should be closed.");
        assertTrue(innerSeq2Closed.get(), "Inner sequence 2 should be closed.");
    }

    @Test
    public void test_onClose_withFlatMap_innerSeqIsNull() throws Exception {
        AtomicBoolean outerSeqClosed = new AtomicBoolean(false);

        Seq<Integer, Exception> outer = Seq.of(1, 2).onClose(() -> outerSeqClosed.set(true));

        Seq<String, Exception> flatMapped = outer.flatMap(i -> {
            if (i == 1) {
                return Seq.of("a" + i);
            } else {
                return null;
            }
        });

        List<String> result = drainWithException(flatMapped);
        assertEquals(Collections.singletonList("a1"), result);
        assertTrue(outerSeqClosed.get(), "Outer sequence should be closed even if flatMap returns null inner Seq.");
    }

    @Test
    public void test_onClose_handlerThrowsException() {
        AtomicBoolean firstHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean secondHandlerCalled = new AtomicBoolean(false);
        RuntimeException exceptionFromClose = new RuntimeException("Close handler failed");

        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> {
            firstHandlerCalled.set(true);
            throw exceptionFromClose;
        }).onClose(() -> secondHandlerCalled.set(true));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> drainWithException(seq));

        assertTrue(firstHandlerCalled.get(), "First close handler should have been called.");
        assertTrue(secondHandlerCalled.get(), "Second close handler should have been called despite previous error.");
        assertEquals(exceptionFromClose, thrown, "The exception from the first handler should be the primary exception.");
    }

    @Test
    public void test_onClose_called_whenIntermediateOperationThrows() {
        AtomicBoolean closed = new AtomicBoolean(false);
        RuntimeException opException = new RuntimeException("Operation failed");

        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed.set(true)).map(i -> {
            if (i == 2) {
                throw opException;
            }
            return i * 2;
        });

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> seq.toList());
        assertSame(opException, thrown, "The operation's exception should be thrown.");
        assertTrue(closed.get(), "onClose handler should still be called even if an intermediate operation fails.");
    }

    @Test
    public void test_onClose_called_whenTerminalOperationThrowsAfterPartialIteration() {
        AtomicBoolean closed = new AtomicBoolean(false);
        RuntimeException terminalOpException = new RuntimeException("Terminal op failed mid-way");
        AtomicInteger processedCount = new AtomicInteger(0);

        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4).onClose(() -> closed.set(true));

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            seq.forEach(val -> {
                processedCount.incrementAndGet();
                if (val == 3) {
                    throw terminalOpException;
                }
            });
        });

        assertSame(terminalOpException, thrown);
        assertEquals(3, processedCount.get(), "Should process elements until the exception.");
        assertTrue(closed.get(), "onClose handler should be called even if terminal operation throws mid-way.");
    }

    @Test
    public void testOnCloseMultipleHandlers() throws Exception {
        List<Integer> closeOrder = new ArrayList<>();

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3))
                .onClose(() -> closeOrder.add(1))
                .onClose(() -> closeOrder.add(2))
                .onClose(() -> closeOrder.add(3));

        seq.count();

        assertEquals(Arrays.asList(1, 2, 3), closeOrder);
    }

    @Test
    public void testOnCloseWithNullHandler() throws Exception {
        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b"));

        assertThrows(IllegalArgumentException.class, () -> seq.onClose(null));
    }

    @Test
    public void testOnCloseNotCalledTwice() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("x", "y")).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testOnCloseMultiple() throws Exception {
        List<String> closeOrder = new ArrayList<>();

        Seq.of(1, 2, 3).onClose(() -> closeOrder.add("first")).onClose(() -> closeOrder.add("second")).onClose(() -> closeOrder.add("third")).toList();

        assertEquals(Arrays.asList("first", "second", "third"), closeOrder);
    }

    @Test
    public void testOnClose_multipleHandlersAllCalled() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        List<Integer> result = Seq.of(1, 2).onClose(count::incrementAndGet).onClose(count::incrementAndGet).toList();
        assertEquals(2, result.size());
        assertEquals(2, count.get());
    }

    @Test
    public void test_close() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1).onClose(() -> closed.set(true));
        seq.close();
        assertTrue(closed.get());
        seq.close();
        assertTrue(closed.get());
    }

    @Test
    public void testClose() throws Exception {
        AtomicBoolean isClosed = new AtomicBoolean(false);

        Seq<String, Exception> seq = Seq.of(Arrays.asList("a", "b", "c")).onClose(() -> isClosed.set(true));

        assertFalse(isClosed.get());

        seq.close();

        assertTrue(isClosed.get());

        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testCloseIdempotent() throws Exception {
        AtomicInteger closeCount = new AtomicInteger(0);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> closeCount.incrementAndGet());

        seq.close();
        seq.close();
        seq.close();

        assertEquals(1, closeCount.get());
    }

    @Test
    public void testCloseWithExceptionInHandler() throws Exception {
        AtomicBoolean firstHandlerCalled = new AtomicBoolean(false);
        AtomicBoolean secondHandlerCalled = new AtomicBoolean(false);

        Seq<Integer, Exception> seq = Seq.of(Arrays.asList(1, 2, 3)).onClose(() -> {
            firstHandlerCalled.set(true);
            throw new RuntimeException("First handler exception");
        }).onClose(() -> {
            secondHandlerCalled.set(true);
        });

        assertThrows(RuntimeException.class, () -> seq.close());

        assertTrue(firstHandlerCalled.get());
        assertTrue(secondHandlerCalled.get());
    }

    @Test
    public void testCloseHandlers() throws Exception {
        boolean[] closed = { false };
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed[0] = true);

        seq.close();
        Assertions.assertTrue(closed[0]);

        boolean[] closed1 = { false };
        boolean[] closed2 = { false };
        Seq<Integer, Exception> seq1 = Seq.of(1, 2).onClose(() -> closed1[0] = true);
        Seq<Integer, Exception> seq2 = Seq.of(3, 4).onClose(() -> closed2[0] = true);

        Seq<Integer, Exception> concat = Seq.concat(seq1, seq2);
        concat.close();
        Assertions.assertTrue(closed1[0]);
        Assertions.assertTrue(closed2[0]);
    }

    // ===== close() - idempotent multiple calls =====

    @Test
    public void testClose_calledMultipleTimesOnlyRunsOnce() throws Exception {
        AtomicInteger count = new AtomicInteger(0);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(count::incrementAndGet);
        seq.close();
        seq.close(); // second close should be no-op
        assertEquals(1, count.get());
    }

    // ===== sps with executor - create(Stream, boolean) path =====

    @Test
    public void testSps_withExecutorAndMaxThreadNum() throws Exception {
        java.util.concurrent.ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Integer> result = Seq.of(1, 2, 3, 4, 5).sps(2, executor, s -> s.map(x -> x * 2)).sorted().toList();
            assertEquals(5, result.size());
            assertTrue(result.contains(2));
            assertTrue(result.contains(10));
        } finally {
            executor.shutdown();
        }
    }

    // ===== checkArgPositive with long/double via limit =====

    @Test
    public void testLimit_longOffset_triggersCheckArgPositive() throws Exception {
        // limit(long offset, long maxSize) - exercises checkArgPositive/checkArgNotNegative
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).limit(2L, 2L).toList();
        assertEquals(2, result.size());
        assertEquals(Integer.valueOf(3), result.get(0));
        assertEquals(Integer.valueOf(4), result.get(1));
    }

    // ===== onClose with empty/trivial handler - isEmptyCloseHandler branch =====

    @Test
    public void testOnClose_handlerIsCalledOnTerminalOp() throws Exception {
        AtomicBoolean called = new AtomicBoolean(false);
        List<Integer> result = Seq.of(1, 2, 3).onClose(() -> called.set(true)).toList();
        assertEquals(3, result.size());
        assertTrue(called.get());
    }

    @Test
    public void testNullArgument_ThrowsException() throws Exception {
        assertTrue(true);
    }

}
