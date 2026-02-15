package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.Vector;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
@Tag("new-test")
public class Seq200Test extends TestBase {

    @TempDir
    Path tempDir;

    private <T, E extends Exception> List<T> drain(Seq<T, E> seq) throws E {
        return seq.toList();
    }

    private <T> List<T> drainWithException(Seq<T, Exception> seq) throws Exception {
        return seq.toList();
    }

    @Test
    public void test_empty() throws Exception {
        Seq<Object, Exception> emptySeq = Seq.empty();
        assertTrue(emptySeq.toList().isEmpty(), "Empty sequence should have no elements.");
        assertDoesNotThrow(emptySeq::close);
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
    public void test_repeat() throws Exception {
        Seq<String, Exception> seq = Seq.repeat("r", 3);
        assertEquals(Arrays.asList("r", "r", "r"), drainWithException(seq));

        Seq<String, Exception> zeroSeq = Seq.repeat("r", 0);
        assertTrue(drainWithException(zeroSeq).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("r", -1));
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
    public void test_listFiles_nonRecursive() throws IOException {
        File subDir = tempDir.resolve("sub").toFile();
        subDir.mkdir();
        File file1 = tempDir.resolve("file1.txt").toFile();
        file1.createNewFile();
        File file2InSub = subDir.toPath().resolve("file2.txt").toFile();
        file2InSub.createNewFile();

        Seq<File, IOException> seq = Seq.listFiles(tempDir.toFile());
        List<File> files = drain(seq);

        assertTrue(files.contains(file1));
        assertTrue(files.contains(subDir));
        assertFalse(files.contains(file2InSub));
        assertEquals(2, files.size());
    }

    @Test
    public void test_listFiles_recursive() throws IOException {
        File subDir = tempDir.resolve("subRecursive").toFile();
        subDir.mkdir();
        File file1 = tempDir.resolve("file1Rec.txt").toFile();
        file1.createNewFile();
        File file2InSub = subDir.toPath().resolve("file2Rec.txt").toFile();
        file2InSub.createNewFile();

        Seq<File, IOException> seq = Seq.listFiles(tempDir.toFile(), true);
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
    public void test_takeWhile() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).takeWhile(x -> x < 3);
        assertEquals(Arrays.asList(1, 2), drainWithException(seq));
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
        assertEquals(CommonUtil.asList(1, 2), dropped,
                "Action should be called on the first element not satisfying the drop condition, based on current impl.");
    }

    @Test
    public void test_skipUntil() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 1).skipUntil(x -> x == 3);
        assertEquals(Arrays.asList(3, 2, 1), drainWithException(seq));
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
    public void test_map() throws Exception {
        Seq<Integer, Exception> seq = Seq.of("1", "2", "3").map(Integer::parseInt);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));
    }

    @Test
    public void test_mapIfNotNull() throws Exception {
        Seq<String, Exception> input = Seq.of("a", null, "b", null, "c");
        Seq<String, Exception> result = input.mapIfNotNull(s -> s + s);
        assertEquals(Arrays.asList("aa", "bb", "cc"), drainWithException(result));
    }

    @Test
    public void test_mapFirst() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapFirst(x -> x * 10);
        assertEquals(Arrays.asList(10, 2, 3), drainWithException(seq));
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty().mapFirst(x -> x * 10);
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_mapFirstOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapFirstOrElse(x -> x * 10, y -> y + 1);
        assertEquals(Arrays.asList(10, 3, 4), drainWithException(seq));
    }

    @Test
    public void test_mapLast() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapLast(x -> x * 10);
        assertEquals(Arrays.asList(1, 2, 30), drainWithException(seq));
        Seq<Integer, Exception> emptySeq = Seq.<Integer, Exception> empty().mapLast(x -> x * 10);
        assertTrue(drainWithException(emptySeq).isEmpty());
    }

    @Test
    public void test_mapLastOrElse() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).mapLastOrElse(x -> x * 10, y -> y + 1);
        assertEquals(Arrays.asList(2, 3, 30), drainWithException(seq));
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
    public void test_onClose() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).onClose(() -> closed.set(true));

        assertFalse(closed.get(), "Should not be closed before terminal operation");
        drainWithException(seq);
        assertTrue(closed.get(), "Should be closed after terminal operation");
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
    public void test_forEach_consumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void test_forEach_throwableConsumer() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach((Throwables.Consumer<Integer, Exception>) collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
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
    public void test_min_comparator() throws Exception {
        Optional<Integer> min = Seq.of(5, 1, 3, 2, 4).min(Comparator.naturalOrder());
        assertEquals(Optional.of(1), min);
        assertTrue(Seq.<Integer, Exception> empty().min(Comparator.naturalOrder()).isEmpty());
    }

    @Test
    public void test_minBy_keyMapper() throws Exception {
        Optional<String> min = Seq.of("apple", "banana", "kiwi").minBy(String::length);
        assertEquals(Optional.of("kiwi"), min);
    }

    @Test
    public void test_max_comparator() throws Exception {
        Optional<Integer> max = Seq.of(5, 1, 3, 2, 4).max(Comparator.naturalOrder());
        assertEquals(Optional.of(5), max);
    }

    @Test
    public void test_maxBy_keyMapper() throws Exception {
        Optional<String> max = Seq.of("apple", "banana", "kiwi").maxBy(String::length);
        assertEquals(Optional.of("banana"), max);
    }

    @Test
    public void test_anyMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3).anyMatch(x -> x == 2));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x == 4));
        assertFalse(Seq.<Integer, Exception> empty().anyMatch(x -> true));
    }

    @Test
    public void test_allMatch() throws Exception {
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).allMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().allMatch(x -> false));
    }

    @Test
    public void test_noneMatch() throws Exception {
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().noneMatch(x -> true));
    }

    @Test
    public void test_nMatch() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertTrue(seq.nMatch(3, 3, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertFalse(seq.nMatch(2, 2, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertTrue(seq.nMatch(1, 5, x -> x % 2 == 0));

        seq = Seq.of(1, 2, 3, 4, 5, 6);
        assertFalse(seq.nMatch(4, 5, x -> x % 2 == 0));

        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).nMatch(-1, 1, x -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).nMatch(1, -1, x -> true));
        assertThrows(IllegalArgumentException.class, () -> Seq.of(1).nMatch(2, 1, x -> true));
    }

    @Test
    public void test_findFirst_predicate() throws Exception {
        Optional<Integer> firstEven = Seq.of(1, 3, 2, 4, 5).findFirst(x -> x % 2 == 0);
        assertEquals(Optional.of(2), firstEven);
    }

    @Test
    public void test_findAny_predicate() throws Exception {
        Optional<Integer> anyEven = Seq.of(1, 3, 2, 4, 5).findAny(x -> x % 2 == 0);
        assertEquals(Optional.of(2), anyEven);
    }

    @Test
    public void test_findLast_predicate() throws Exception {
        Optional<Integer> lastEven = Seq.of(1, 3, 2, 4, 5).findLast(x -> x % 2 == 0);
        assertEquals(Optional.of(4), lastEven);
        assertTrue(Seq.of(1, 3, 5).findLast(x -> x % 2 == 0).isEmpty());
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
    public void test_hasDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 2, 3).hasDuplicates());
        assertFalse(Seq.of(1, 2, 3, 4).hasDuplicates());
        assertFalse(Seq.<Integer, Exception> empty().hasDuplicates());
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
    public void test_first() throws Exception {
        assertEquals(Optional.of(1), Seq.of(1, 2, 3).first());
        assertTrue(Seq.<Integer, Exception> empty().first().isEmpty());
    }

    @Test
    public void test_last() throws Exception {
        assertEquals(Optional.of(3), Seq.of(1, 2, 3).last());
        assertTrue(Seq.<Integer, Exception> empty().last().isEmpty());
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
    public void test_onlyOne() throws Exception {
        assertEquals(Optional.of(1), Seq.of(1).onlyOne());
        assertTrue(Seq.<Integer, Exception> empty().onlyOne().isEmpty());
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2).onlyOne());
    }

    @Test
    public void test_count() throws Exception {
        assertEquals(3, Seq.of(1, 2, 3).count());
        assertEquals(0, Seq.<Integer, Exception> empty().count());
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
    public void test_toList() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).toList());
    }

    @Test
    public void test_toSet() throws Exception {
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), Seq.of(1, 2, 3, 2).toSet());
    }

    @Test
    public void test_toCollection() throws Exception {
        LinkedList<Integer> result = Seq.of(1, 2, 3).toCollection(LinkedList::new);
        assertEquals(new LinkedList<>(Arrays.asList(1, 2, 3)), result);
    }

    @Test
    public void test_toImmutableList() throws Exception {
        ImmutableList<Integer> list = Seq.of(1, 2, 3).toImmutableList();
        assertEquals(Arrays.asList(1, 2, 3), list);
        assertThrows(UnsupportedOperationException.class, () -> list.add(4));
    }

    @Test
    public void test_toImmutableSet() throws Exception {
        ImmutableSet<Integer> set = Seq.of(1, 2, 3, 2).toImmutableSet();
        assertEquals(new HashSet<>(Arrays.asList(1, 2, 3)), set);
        assertThrows(UnsupportedOperationException.class, () -> set.add(4));
    }

    @Test
    public void test_toListThenApply() throws Exception {
        Integer sum = Seq.of(1, 2, 3).toListThenApply(list -> list.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum.intValue());
    }

    @Test
    public void test_toListThenAccept() throws Exception {
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toListThenAccept(target::addAll);
        assertEquals(Arrays.asList(1, 2, 3), target);
    }

    @Test
    public void test_toSetThenApply() throws Exception {
        Integer sum = Seq.of(1, 2, 3, 2).toSetThenApply(set -> set.stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, sum.intValue());
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
    public void test_toCollectionThenAccept() throws Exception {
        List<Integer> target = new ArrayList<>();
        Seq.of(1, 2, 3).toCollectionThenAccept(LinkedList::new, list -> target.addAll(list));
        assertEquals(Arrays.asList(1, 2, 3), target);
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
    public void test_sumInt() throws Exception {
        long sum = Seq.of("1", "2", "3").sumInt(Integer::parseInt);
        assertEquals(6, sum);
    }

    @Test
    public void test_sumLong() throws Exception {
        long sum = Seq.of("10", "20", "30").sumLong(Long::parseLong);
        assertEquals(60L, sum);
    }

    @Test
    public void test_sumDouble() throws Exception {
        double sum = Seq.of("1.1", "2.2", "3.3").sumDouble(Double::parseDouble);
        assertEquals(6.6, sum, 0.001);
    }

    @Test
    public void test_averageInt() throws Exception {
        OptionalDouble avg = Seq.of("1", "2", "3").averageInt(Integer::parseInt);
        assertTrue(avg.isPresent());
        assertEquals(2.0, avg.getAsDouble(), 0.001);
        assertTrue(Seq.<String, Exception> empty().averageInt(Integer::parseInt).isEmpty());
    }

    @Test
    public void test_averageLong() throws Exception {
        OptionalDouble avg = Seq.of("10", "20", "30").averageLong(Long::parseLong);
        assertTrue(avg.isPresent());
        assertEquals(20.0, avg.getAsDouble(), 0.001);
    }

    @Test
    public void test_averageDouble() throws Exception {
        OptionalDouble avg = Seq.of("1.0", "2.0", "3.0", "4.0").averageDouble(Double::parseDouble);
        assertTrue(avg.isPresent());
        assertEquals(2.5, avg.getAsDouble(), 0.001);
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
    public void test_collectThenApply() throws Exception {
        int size = Seq.of(1, 2, 3).collectThenApply(Collectors.toList(), List::size);
        assertEquals(3, size);
    }

    @Test
    public void test_collectThenAccept() throws Exception {
        List<Integer> holder = new ArrayList<>();
        Seq.of(1, 2, 3).collectThenAccept(Collectors.toList(), holder::addAll);
        assertEquals(Arrays.asList(1, 2, 3), holder);
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
    public void test_joinTo_joiner() throws Exception {
        Joiner joiner = Joiner.with("-", "<", ">");
        Seq.of(1, 2, 3).joinTo(joiner);
        assertEquals("<1-2-3>", joiner.toString());
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
    public void test_applyIfNotEmpty() throws Exception {
        u.Optional<Long> result = Seq.of(1, 2, 3).applyIfNotEmpty(s -> s.sumInt(x -> x));
        assertEquals(Optional.of(6L), result);

        u.Optional<Long> emptyResult = Seq.<Integer, Exception> empty().applyIfNotEmpty(s -> s.sumInt(x -> x));
        assertTrue(emptyResult.isEmpty());
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
    public void test_close() throws Exception {
        AtomicBoolean closed = new AtomicBoolean(false);
        Seq<Integer, Exception> seq = Seq.of(1).onClose(() -> closed.set(true));
        seq.close();
        assertTrue(closed.get());
        seq.close();
        assertTrue(closed.get());
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
    public void test_transform_seqToSeq() throws Exception {
        Seq<Integer, Exception> original = Seq.of(1, 2, 3);
        Seq<String, Exception> transformed = original.transform(s -> s.map(String::valueOf).append("end"));
        assertEquals(Arrays.asList("1", "2", "3", "end"), drainWithException(transformed));
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
    public void test_cast() throws Exception {
        Seq<Number, Exception> numSeq = Seq.of(1, 2L, 3.0f);
        Seq<Number, Exception> castedSeq = numSeq.cast();
        assertSame(numSeq, castedSeq);
        assertEquals(Arrays.asList(1, 2L, 3.0f), drain(castedSeq));

        Seq<String, IOException> ioSeq = Seq.ofLines(tempDir.resolve("dummy.txt").toFile());
        Seq<String, IOException> stringIoSeq = Seq.<String, IOException> of("a", "b").map(Fnn.identity());
        Seq<String, Exception> stringExSeq = stringIoSeq.cast();
        assertEquals(Arrays.asList("a", "b"), stringExSeq.toList());
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
    public void test_indexed() throws Exception {
        List<Indexed<String>> result = Seq.of("a", "b", "c").indexed().toList();
        assertEquals(3, result.size());
        assertEquals(Indexed.of("a", 0L), result.get(0));
        assertEquals(Indexed.of("b", 1L), result.get(1));
        assertEquals(Indexed.of("c", 2L), result.get(2));
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
    public void test_intersperse() throws Exception {
        Seq<String, Exception> seq = Seq.of("a", "b", "c").intersperse("-");
        assertEquals(Arrays.asList("a", "-", "b", "-", "c"), drainWithException(seq));

        Seq<String, Exception> single = Seq.of("a").intersperse("-");
        assertEquals(Collections.singletonList("a"), drainWithException(single));

        Seq<String, Exception> empty = Seq.<String, Exception> empty().intersperse("-");
        assertTrue(drainWithException(empty).isEmpty());
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
    public void test_partitionBy_withCollector() throws Exception {
        Map<Boolean, Long> partition = Seq.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, partition.get(true));
        assertEquals(3L, partition.get(false));
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
    public void test_skipLast() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5).skipLast(2);
        assertEquals(Arrays.asList(1, 2, 3), drainWithException(seq));
        Seq<Integer, Exception> skipZero = Seq.of(1, 2).skipLast(0);
        assertEquals(Arrays.asList(1, 2), drainWithException(skipZero));
        Seq<Integer, Exception> skipTooMany = Seq.of(1, 2).skipLast(3);
        assertTrue(drainWithException(skipTooMany).isEmpty());
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
    public void test_rateLimited() throws Exception {
        RateLimiter mockLimiter = mock(RateLimiter.class);
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3).rateLimited(mockLimiter);
        List<Integer> result = drainWithException(seq);
        assertEquals(Arrays.asList(1, 2, 3), result);
        verify(mockLimiter, times(3)).acquire();
    }

    @Test
    public void test_delay() throws Exception {
        long startTime = System.currentTimeMillis();
        Seq<Integer, Exception> seq = Seq.of(1, 2).delay(Duration.ofMillis(10));
        List<Integer> result = drainWithException(seq);
        long endTime = System.currentTimeMillis();
        assertEquals(Arrays.asList(1, 2), result);
        assertTrue(endTime - startTime < 15, "Should have some delay, approx 20ms for 2 elements. Actual: " + (endTime - startTime));
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
    public void test_throwIfEmpty_nonEmptySeq_customException() {
        Seq<Integer, Exception> seq = Seq.of(1).throwIfEmpty(RuntimeException::new);
        assertDoesNotThrow(() -> drainWithException(seq));
    }

    @Test
    public void test_ifEmpty_actionOnNonEmpty() throws Exception {
        AtomicBoolean actionCalled = new AtomicBoolean(false);
        Seq<Integer, Exception> nonEmptySeq = Seq.of(1).ifEmpty(() -> actionCalled.set(true));
        assertEquals(Collections.singletonList(1), drainWithException(nonEmptySeq));
        assertFalse(actionCalled.get());
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

    private <T, E extends Exception> Comparator<? super T> extractComparator(Seq<T, E> seq) {
        throw new UnsupportedOperationException("Cannot reliably extract comparator without reflection/internal access.");
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
    public void test_splitByChunkCount_totalSizeLessThanMaxChunkCount() throws Exception {
        Seq<Pair<Integer, Integer>, Exception> seq = Seq.splitByChunkCount(2, 5, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expected = Arrays.asList(Pair.of(0, 1), Pair.of(1, 2));
        assertEquals(expected, drainWithException(seq));

        Seq<Pair<Integer, Integer>, Exception> seqSingle = Seq.splitByChunkCount(1, 5, (from, to) -> Pair.of(from, to));
        List<Pair<Integer, Integer>> expectedSingle = Collections.singletonList(Pair.of(0, 1));
        assertEquals(expectedSingle, drainWithException(seqSingle));
    }

    @Test
    public void test_distinctBy_keyMapper_withNullKeys() throws Exception {
        List<String> data = Arrays.asList("apple", null, "banana", "apricot", null, "avocado");
        Seq<String, Exception> seq = Seq.of(data).distinctBy(s -> s == null ? null : s.charAt(0));
        List<String> expected = Arrays.asList("apple", null, "banana");
        assertEquals(expected, drainWithException(seq));
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
    public void test_skip_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.skip(1));
    }

    @Test
    public void test_limit_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.limit(1));
    }

    @Test
    public void test_map_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.map(x -> x * 2));
    }

    @Test
    public void test_forEach_onAlreadyClosedSeq_throwsIllegalStateException() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        drainWithException(seq);
        assertThrows(IllegalStateException.class, () -> seq.forEach(x -> {
        }));
    }

}
