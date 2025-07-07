package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;

public class Seq101Test extends TestBase {

    private File tempFile;
    private Path tempPath;

    @BeforeEach
    public void setUp() throws IOException {
        // Create temp file for file reading tests
        tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();
        Files.write(tempFile.toPath(), Arrays.asList("line1", "line2", "line3"), StandardCharsets.UTF_8);
        
        tempPath = tempFile.toPath();
    }

    @AfterEach
    public void tearDown() {
        if (tempFile != null && tempFile.exists()) {
            tempFile.delete();
        }
    }

    @Test
    public void testEmpty() throws Exception {
        Seq<String, Exception> seq = Seq.empty();
        Assertions.assertFalse(seq.iteratorEx().hasNext());
        Assertions.assertEquals(0, seq.toList().size());
    }

    @Test
    public void testDefer() throws Exception {
        // Test with supplier that returns a sequence
        Throwables.Supplier<Seq<Integer, Exception>, Exception> supplier = () -> Seq.of(1, 2, 3);
        Seq<Integer, Exception> seq = Seq.defer(supplier);
        List<Integer> result = seq.toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), result);

        // Test with null supplier - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.defer(null);
        });
    }

    @Test
    public void testJust() throws Exception {
        // Test with single element
        Seq<String, Exception> seq = Seq.just("hello");
        List<String> result = seq.toList();
        Assertions.assertEquals(Arrays.asList("hello"), result);

        // Test with null element
        Seq<String, Exception> seqNull = Seq.just(null);
        List<String> resultNull = seqNull.toList();
        Assertions.assertEquals(Arrays.asList((String)null), resultNull);
    }

    @Test
    public void testJustWithExceptionType() throws Exception {
        Seq<String, IOException> seq = Seq.just("hello", IOException.class);
        List<String> result = seq.toList();
        Assertions.assertEquals(Arrays.asList("hello"), result);
    }

    @Test
    public void testOfNullable() throws Exception {
        // Test with non-null value
        Seq<String, Exception> seq = Seq.ofNullable("hello");
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        // Test with null value
        Seq<String, Exception> seqNull = Seq.ofNullable(null);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfNullableWithExceptionType() throws Exception {
        // Test with non-null value
        Seq<String, IOException> seq = Seq.ofNullable("hello", IOException.class);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        // Test with null value
        Seq<String, IOException> seqNull = Seq.ofNullable(null, IOException.class);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfVarargs() throws Exception {
        // Test with multiple elements
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), seq.toList());

        // Test with empty array
        Seq<Integer, Exception> seqEmpty = Seq.of();
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Test with null array
        Integer[] nullArray = null;
        Seq<Integer, Exception> seqNull = Seq.of(nullArray);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfBooleanArray() throws Exception {
        boolean[] arr = {true, false, true};
        Seq<Boolean, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(true, false, true), seq.toList());

        // Test with empty array
        boolean[] empty = {};
        Seq<Boolean, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Test with null array
        boolean[] nullArr = null;
        Seq<Boolean, Exception> seqNull = Seq.of(nullArr);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfCharArray() throws Exception {
        char[] arr = {'a', 'b', 'c'};
        Seq<Character, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList('a', 'b', 'c'), seq.toList());

        // Test with empty array
        char[] empty = {};
        Seq<Character, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfByteArray() throws Exception {
        byte[] arr = {1, 2, 3};
        Seq<Byte, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList((byte)1, (byte)2, (byte)3), seq.toList());
    }

    @Test
    public void testOfShortArray() throws Exception {
        short[] arr = {10, 20, 30};
        Seq<Short, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList((short)10, (short)20, (short)30), seq.toList());
    }

    @Test
    public void testOfIntArray() throws Exception {
        int[] arr = {100, 200, 300};
        Seq<Integer, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(100, 200, 300), seq.toList());
    }

    @Test
    public void testOfLongArray() throws Exception {
        long[] arr = {1000L, 2000L, 3000L};
        Seq<Long, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1000L, 2000L, 3000L), seq.toList());
    }

    @Test
    public void testOfFloatArray() throws Exception {
        float[] arr = {1.1f, 2.2f, 3.3f};
        Seq<Float, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1.1f, 2.2f, 3.3f), seq.toList());
    }

    @Test
    public void testOfDoubleArray() throws Exception {
        double[] arr = {1.11, 2.22, 3.33};
        Seq<Double, Exception> seq = Seq.of(arr);
        Assertions.assertEquals(Arrays.asList(1.11, 2.22, 3.33), seq.toList());
    }

    @Test
    public void testOfOptional() throws Exception {
        // Test with present Optional
        Optional<String> opt = Optional.of("hello");
        Seq<String, Exception> seq = Seq.of(opt);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        // Test with empty Optional
        Optional<String> empty = Optional.empty();
        Seq<String, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Test with null Optional
        Optional<String> nullOpt = null;
        Seq<String, Exception> seqNull = Seq.of(nullOpt);
        Assertions.assertTrue(seqNull.toList().isEmpty());
    }

    @Test
    public void testOfJavaUtilOptional() throws Exception {
        // Test with present Optional
        java.util.Optional<String> opt = java.util.Optional.of("hello");
        Seq<String, Exception> seq = Seq.of(opt);
        Assertions.assertEquals(Arrays.asList("hello"), seq.toList());

        // Test with empty Optional
        java.util.Optional<String> empty = java.util.Optional.empty();
        Seq<String, Exception> seqEmpty = Seq.of(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfIterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        Seq<String, Exception> seq = Seq.of(list);
        Assertions.assertEquals(list, seq.toList());

        // Test with null iterable
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

        // Test with null iterator
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

        // Test with null enumeration
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

        // Test with empty map
        Map<String, Integer> emptyMap = new HashMap<>();
        Seq<Map.Entry<String, Integer>, Exception> seqEmpty = Seq.of(emptyMap);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Test with null map
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
    public void testOfKeys() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        
        Seq<String, Exception> seq = Seq.ofKeys(map);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), seq.toList());

        // Test with empty map
        Map<String, Integer> emptyMap = new HashMap<>();
        Seq<String, Exception> seqEmpty = Seq.ofKeys(emptyMap);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
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

    @Test
    public void testOfValues() throws Exception {
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        
        Seq<Integer, Exception> seq = Seq.ofValues(map);
        Assertions.assertEquals(Arrays.asList(1, 2, 3), seq.toList());

        // Test with empty map
        Map<String, Integer> emptyMap = new HashMap<>();
        Seq<Integer, Exception> seqEmpty = Seq.ofValues(emptyMap);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
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
    public void testOfReversedArray() throws Exception {
        Integer[] arr = {1, 2, 3, 4, 5};
        Seq<Integer, Exception> seq = Seq.ofReversed(arr);
        Assertions.assertEquals(Arrays.asList(5, 4, 3, 2, 1), seq.toList());

        // Test with empty array
        Integer[] empty = {};
        Seq<Integer, Exception> seqEmpty = Seq.ofReversed(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testOfReversedList() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c", "d");
        Seq<String, Exception> seq = Seq.ofReversed(list);
        Assertions.assertEquals(Arrays.asList("d", "c", "b", "a"), seq.toList());

        // Test with empty list
        List<String> empty = new ArrayList<>();
        Seq<String, Exception> seqEmpty = Seq.ofReversed(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testRepeat() throws Exception {
        // Test repeat with positive count
        Seq<String, Exception> seq = Seq.repeat("hello", 3);
        Assertions.assertEquals(Arrays.asList("hello", "hello", "hello"), seq.toList());

        // Test repeat with zero count
        Seq<String, Exception> seqZero = Seq.repeat("hello", 0);
        Assertions.assertTrue(seqZero.toList().isEmpty());

        // Test repeat with negative count - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.repeat("hello", -1);
        });
    }

    @Test
    public void testRange() throws Exception {
        // Test basic range
        Seq<Integer, Exception> seq = Seq.range(0, 5);
        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3, 4), seq.toList());

        // Test empty range
        Seq<Integer, Exception> seqEmpty = Seq.range(5, 5);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Test reverse range
        Seq<Integer, Exception> seqReverse = Seq.range(5, 0);
        Assertions.assertTrue(seqReverse.toList().isEmpty());
    }

    @Test
    public void testRangeWithStep() throws Exception {
        // Test range with positive step
        Seq<Integer, Exception> seq = Seq.range(0, 10, 2);
        Assertions.assertEquals(Arrays.asList(0, 2, 4, 6, 8), seq.toList());

        // Test range with negative step
        Seq<Integer, Exception> seqNeg = Seq.range(10, 0, -2);
        Assertions.assertEquals(Arrays.asList(10, 8, 6, 4, 2), seqNeg.toList());
    }

    @Test
    public void testRangeClosed() throws Exception {
        // Test basic rangeClosed
        Seq<Integer, Exception> seq = Seq.rangeClosed(0, 5);
        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), seq.toList());

        // Test single element range
        Seq<Integer, Exception> seqSingle = Seq.rangeClosed(5, 5);
        Assertions.assertEquals(Arrays.asList(5), seqSingle.toList());
    }

    @Test
    public void testRangeClosedWithStep() throws Exception {
        // Test rangeClosed with positive step
        Seq<Integer, Exception> seq = Seq.rangeClosed(0, 10, 3);
        Assertions.assertEquals(Arrays.asList(0, 3, 6, 9), seq.toList());

        // Test rangeClosed with negative step
        Seq<Integer, Exception> seqNeg = Seq.rangeClosed(10, 0, -3);
        Assertions.assertEquals(Arrays.asList(10, 7, 4, 1), seqNeg.toList());
    }

    @Test
    public void testSplitByCharDelimiter() throws Exception {
        String str = "a,b,c,d";
        Seq<String, Exception> seq = Seq.split(str, ',');
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());

        // Test with empty string
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
    public void testSplitToLines() throws Exception {
        String str = "line1\nline2\rline3\r\nline4";
        Seq<String, Exception> seq = Seq.splitToLines(str);
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3", "line4"), seq.toList());

        // Test with empty string
        Seq<String, Exception> seqEmpty = Seq.splitToLines("");
        Assertions.assertEquals(Arrays.asList(""), seqEmpty.toList());
    }

    @Test
    public void testSplitToLinesWithOptions() throws Exception {
        String str = "  line1  \n\n  line2  \n  \n  line3  ";
        
        // Test trim only
        Seq<String, Exception> seqTrim = Seq.splitToLines(str, true, false);
        Assertions.assertEquals(Arrays.asList("line1", "", "line2", "", "line3"), seqTrim.toList());

        // Test omit empty lines only
        Seq<String, Exception> seqOmit = Seq.splitToLines(str, false, true);
        Assertions.assertEquals(Arrays.asList("  line1  ", "  line2  ", "  ", "  line3  "), seqOmit.toList());

        // Test trim and omit empty lines
        Seq<String, Exception> seqBoth = Seq.splitToLines(str, true, true);
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), seqBoth.toList());
    }

    @Test
    public void testSplitByChunkCount() throws Exception {
        // Test with simple mapper
        Seq<List<Integer>, Exception> seq = Seq.splitByChunkCount(10, 3, (from, to) -> {
            List<Integer> chunk = new ArrayList<>();
            for (int i = from; i < to; i++) {
                chunk.add(i);
            }
            return chunk;
        });
        
        List<List<Integer>> result = seq.toList();
        Assertions.assertEquals(3, result.size());
        Assertions.assertEquals(Arrays.asList(0, 1, 2, 3), result.get(0));
        Assertions.assertEquals(Arrays.asList(4, 5, 6), result.get(1));
        Assertions.assertEquals(Arrays.asList(7, 8, 9), result.get(2));

        // Test with zero size
        Seq<List<Integer>, Exception> seqZero = Seq.splitByChunkCount(0, 3, (from, to) -> new ArrayList<>());
        Assertions.assertTrue(seqZero.toList().isEmpty());

        // Test with negative size - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.splitByChunkCount(-1, 3, (from, to) -> new ArrayList<>());
        });

        // Test with zero chunk count - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.splitByChunkCount(10, 0, (from, to) -> new ArrayList<>());
        });
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() throws Exception {
        // Test with sizeSmallerFirst = true
        Seq<List<Integer>, Exception> seqSmaller = Seq.splitByChunkCount(7, 5, true, (from, to) -> {
            List<Integer> chunk = new ArrayList<>();
            for (int i = from; i < to; i++) {
                chunk.add(i);
            }
            return chunk;
        });
        
        List<List<Integer>> resultSmaller = seqSmaller.toList();
        Assertions.assertEquals(5, resultSmaller.size());
        Assertions.assertEquals(1, resultSmaller.get(0).size()); // smaller chunks first
        Assertions.assertEquals(2, resultSmaller.get(3).size()); // bigger chunks later

        // Test with sizeSmallerFirst = false
        Seq<List<Integer>, Exception> seqBigger = Seq.splitByChunkCount(7, 5, false, (from, to) -> {
            List<Integer> chunk = new ArrayList<>();
            for (int i = from; i < to; i++) {
                chunk.add(i);
            }
            return chunk;
        });
        
        List<List<Integer>> resultBigger = seqBigger.toList();
        Assertions.assertEquals(5, resultBigger.size());
        Assertions.assertEquals(2, resultBigger.get(0).size()); // bigger chunks first
        Assertions.assertEquals(1, resultBigger.get(3).size()); // smaller chunks later
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

        // Test with null file - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((File)null, StandardCharsets.UTF_8);
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

        // Test with null path - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((Path)null, StandardCharsets.UTF_8);
        });
    }

    @Test
    public void testOfLinesReader() throws IOException {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Seq<String, IOException> seq = Seq.ofLines(reader);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);

        // Test with null reader - should throw exception
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Seq.ofLines((Reader)null);
        });
    }

    @Test
    public void testOfLinesReaderWithCloseOption() throws IOException {
        StringReader reader = new StringReader("line1\nline2\nline3");
        Seq<String, IOException> seq = Seq.ofLines(reader, true);
        List<String> lines = seq.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2", "line3"), lines);
        seq.close();

        // Test with closeReader = false
        StringReader reader2 = new StringReader("line1\nline2");
        Seq<String, IOException> seq2 = Seq.ofLines(reader2, false);
        List<String> lines2 = seq2.toList();
        Assertions.assertEquals(Arrays.asList("line1", "line2"), lines2);
        seq2.close();
    }

    @Test
    public void testListFiles() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();
        
        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();
        file1.deleteOnExit();
        file2.deleteOnExit();

        Seq<File, IOException> seq = Seq.listFiles(tempDir);
        List<File> files = seq.toList();
        Assertions.assertEquals(2, files.size());

        // Test with non-existent directory
        File nonExistent = new File("non_existent_dir");
        Seq<File, IOException> seqEmpty = Seq.listFiles(nonExistent);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());

        // Cleanup
        file1.delete();
        file2.delete();
        tempDir.delete();
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

        // Test non-recursive
        Seq<File, IOException> seqNonRecursive = Seq.listFiles(tempDir, false);
        List<File> filesNonRecursive = seqNonRecursive.toList();
        Assertions.assertEquals(2, filesNonRecursive.size()); // file1 and subdir

        // Test recursive
        Seq<File, IOException> seqRecursive = Seq.listFiles(tempDir, true);
        List<File> filesRecursive = seqRecursive.toList();
        Assertions.assertEquals(3, filesRecursive.size()); // file1, subdir, and file2

        // Cleanup
        file2.delete();
        file1.delete();
        subDir.delete();
        tempDir.delete();
    }

    @Test
    public void testConcatArrays() throws Exception {
        Integer[] arr1 = {1, 2, 3};
        Integer[] arr2 = {4, 5};
        Integer[] arr3 = {6, 7, 8};
        
        Seq<Integer, Exception> seq = Seq.concat(arr1, arr2, arr3);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), seq.toList());

        // Test with null arrays
        Integer[] nullArr = null;
        Seq<Integer, Exception> seqWithNull = Seq.concat(arr1, nullArr, arr2);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), seqWithNull.toList());

        // Test with empty varargs
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

        // Test with null iterables
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

        // Test with empty varargs
        Seq<Integer, Exception> seqEmpty = Seq.concat(new Seq[] {});
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testConcatSeqCollection() throws Exception {
        List<Seq<Integer, Exception>> seqs = Arrays.asList(
            Seq.of(1, 2),
            Seq.of(3, 4),
            Seq.of(5, 6)
        );
        
        Seq<Integer, Exception> concat = Seq.concat(seqs);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), concat.toList());
        concat.close();

        // Test with empty collection
        List<Seq<Integer, Exception>> empty = new ArrayList<>();
        Seq<Integer, Exception> seqEmpty = Seq.concat(empty);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testZipArrays() throws Exception {
        Integer[] arr1 = {1, 2, 3};
        String[] arr2 = {"a", "b", "c", "d"};
        
        Seq<String, Exception> seq = Seq.zip(arr1, arr2, (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3c"), seq.toList());

        // Test with empty arrays
        Integer[] empty1 = {};
        String[] empty2 = {};
        Seq<String, Exception> seqEmpty = Seq.zip(empty1, empty2, (n, s) -> n + s);
        Assertions.assertTrue(seqEmpty.toList().isEmpty());
    }

    @Test
    public void testZipThreeArrays() throws Exception {
        Integer[] arr1 = {1, 2, 3};
        String[] arr2 = {"a", "b", "c"};
        Double[] arr3 = {1.1, 2.2, 3.3, 4.4};
        
        Seq<String, Exception> seq = Seq.zip(arr1, arr2, arr3, (n, s, d) -> n + s + d);
        Assertions.assertEquals(Arrays.asList("1a1.1", "2b2.2", "3c3.3"), seq.toList());
    }

    @Test
    public void testZipIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<String> set2 = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        
        Seq<String, Exception> seq = Seq.zip(list1, set2, (n, s) -> n + s);
        List<String> result = seq.toList();
        Assertions.assertEquals(3, result.size()); // Limited by shorter collection
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
        Integer[] arr1 = {1, 2};
        String[] arr2 = {"a", "b", "c"};
        
        Seq<String, Exception> seq = Seq.zip(arr1, arr2, 0, "X", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "0c"), seq.toList());

        // Test with first array longer
        Integer[] arr3 = {1, 2, 3, 4};
        String[] arr4 = {"a", "b"};
        Seq<String, Exception> seq2 = Seq.zip(arr3, arr4, 0, "Y", (n, s) -> n + s);
        Assertions.assertEquals(Arrays.asList("1a", "2b", "3Y", "4Y"), seq2.toList());
    }

    @Test
    public void testZipThreeArraysWithDefaults() throws Exception {
        Integer[] arr1 = {1, 2};
        String[] arr2 = {"a", "b", "c"};
        Double[] arr3 = {1.1};
        
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
    public void testMergeArrays() throws Exception {
        Integer[] arr1 = {1, 3, 5};
        Integer[] arr2 = {2, 4, 6};
        
        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, 
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), seq.toList());

        // Test with empty arrays
        Integer[] empty = {};
        Seq<Integer, Exception> seqEmpty1 = Seq.merge(arr1, empty, 
            (a, b) -> MergeResult.TAKE_FIRST);
        Assertions.assertEquals(Arrays.asList(1, 3, 5), seqEmpty1.toList());

        Seq<Integer, Exception> seqEmpty2 = Seq.merge(empty, arr2, 
            (a, b) -> MergeResult.TAKE_FIRST);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), seqEmpty2.toList());
    }

    @Test
    public void testMergeThreeArrays() throws Exception {
        Integer[] arr1 = {1, 4, 7};
        Integer[] arr2 = {2, 5, 8};
        Integer[] arr3 = {3, 6, 9};
        
        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, arr3,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), seq.toList());
    }

    @Test
    public void testMergeIterables() throws Exception {
        List<String> list1 = Arrays.asList("apple", "cherry", "grape");
        List<String> list2 = Arrays.asList("banana", "date", "fig");
        
        Seq<String, Exception> seq = Seq.merge(list1, list2,
            (a, b) -> a.compareTo(b) <= 0 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList("apple", "banana", "cherry", "date", "fig", "grape"), 
            seq.toList());
    }

    @Test
    public void testMergeThreeIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 4);
        List<Integer> list2 = Arrays.asList(2, 5);
        List<Integer> list3 = Arrays.asList(3, 6);
        
        Seq<Integer, Exception> seq = Seq.merge(list1, list2, list3,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), seq.toList());
    }

    @Test
    public void testMergeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6, 8).iterator();
        
        Seq<Integer, Exception> seq = Seq.merge(iter1, iter2,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), seq.toList());
    }

    @Test
    public void testMergeThreeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 4, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 5, 8).iterator();
        Iterator<Integer> iter3 = Arrays.asList(3, 6, 9).iterator();
        
        Seq<Integer, Exception> seq = Seq.merge(iter1, iter2, iter3,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), seq.toList());
    }

    @Test
    public void testMergeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6);
        
        Seq<Integer, Exception> merged = Seq.merge(seq1, seq2,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged.toList());
        merged.close();
    }

    @Test
    public void testMergeThreeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 4);
        Seq<Integer, Exception> seq2 = Seq.of(2, 5);
        Seq<Integer, Exception> seq3 = Seq.of(3, 6);
        
        Seq<Integer, Exception> merged = Seq.merge(seq1, seq2, seq3,
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), merged.toList());
        merged.close();
    }

    @Test
    public void testFilter() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 4, 5, 6);
        Seq<Integer, Exception> filtered = seq.filter(n -> n % 2 == 0);
        Assertions.assertEquals(Arrays.asList(2, 4, 6), filtered.toList());

        // Test with no matching elements
        Seq<Integer, Exception> seq2 = Seq.of(1, 3, 5, 7);
        Seq<Integer, Exception> filtered2 = seq2.filter(n -> n % 2 == 0);
        Assertions.assertTrue(filtered2.toList().isEmpty());

        // Test with all matching elements
        Seq<Integer, Exception> seq3 = Seq.of(2, 4, 6, 8);
        Seq<Integer, Exception> filtered3 = seq3.filter(n -> n % 2 == 0);
        Assertions.assertEquals(Arrays.asList(2, 4, 6, 8), filtered3.toList());
    }

    @Test
    public void testFilterWithAction() throws Exception {
        List<String> dropped = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("Alice", "Bob", "Charlie", "David");
        Seq<String, Exception> filtered = seq.filter(
            name -> name.length() <= 4,
            droppedName -> dropped.add(droppedName)
        );
        
        List<String> result = filtered.toList();
        Assertions.assertEquals(Arrays.asList("Bob"), result);
        Assertions.assertEquals(Arrays.asList("Alice", "Charlie", "David"), dropped);
    }

    @Test
    public void testTakeWhile() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5, 2, 7, 9);
        Seq<Integer, Exception> taken = seq.takeWhile(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(1, 3, 5), taken.toList());

        // Test with no elements matching
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6, 8);
        Seq<Integer, Exception> taken2 = seq2.takeWhile(n -> n % 2 == 1);
        Assertions.assertTrue(taken2.toList().isEmpty());

        // Test with all elements matching
        Seq<Integer, Exception> seq3 = Seq.of(1, 3, 5, 7);
        Seq<Integer, Exception> taken3 = seq3.takeWhile(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(1, 3, 5, 7), taken3.toList());
    }

    @Test
    public void testDropWhile() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 3, 5, 2, 7, 9, 4);
        Seq<Integer, Exception> dropped = seq.dropWhile(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(2, 7, 9, 4), dropped.toList());

        // Test with no elements matching
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6, 8);
        Seq<Integer, Exception> dropped2 = seq2.dropWhile(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(2, 4, 6, 8), dropped2.toList());

        // Test with all elements matching
        Seq<Integer, Exception> seq3 = Seq.of(1, 3, 5, 7);
        Seq<Integer, Exception> dropped3 = seq3.dropWhile(n -> n % 2 == 1);
        Assertions.assertTrue(dropped3.toList().isEmpty());
    }

    @Test
    public void testDropWhileWithAction() throws Exception {
        List<String> droppedItems = new ArrayList<>();
        Seq<String, Exception> seq = Seq.of("a", "an", "the", "quick", "brown");
        Seq<String, Exception> result = seq.dropWhile(
            word -> word.length() <= 3,
            droppedWord -> droppedItems.add(droppedWord)
        );
        
        Assertions.assertEquals(Arrays.asList("quick", "brown"), result.toList());
        Assertions.assertEquals(Arrays.asList("a", "an", "the"), droppedItems);
    }

    @Test
    public void testSkipUntil() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(2, 4, 6, 5, 7, 9);
        Seq<Integer, Exception> result = seq.skipUntil(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(5, 7, 9), result.toList());

        // Test with first element matching
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3, 4);
        Seq<Integer, Exception> result2 = seq2.skipUntil(n -> n % 2 == 1);
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result2.toList());
    }

    @Test
    public void testDistinct() throws Exception {
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3, 2, 4, 3, 5);
        Seq<Integer, Exception> distinct = seq.distinct();
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), distinct.toList());

        // Test with all distinct elements
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3, 4, 5);
        Seq<Integer, Exception> distinct2 = seq2.distinct();
        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4, 5), distinct2.toList());

        // Test with all duplicate elements
        Seq<Integer, Exception> seq3 = Seq.of(1, 1, 1, 1);
        Seq<Integer, Exception> distinct3 = seq3.distinct();
        Assertions.assertEquals(Arrays.asList(1), distinct3.toList());
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
    public void testDistinctBy() throws Exception {
        class Person {
            String name;
            int age;
            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
            String getName() { return name; }
            int getAge() { return age; }
        }
        
        Seq<Person, Exception> seq = Seq.of(
            new Person("Alice", 30),
            new Person("Bob", 25),
            new Person("Alice", 35)
        );
        
        Seq<Person, Exception> distinctByName = seq.distinctBy(Person::getName);
        List<Person> result = distinctByName.toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("Alice", result.get(0).getName());
        Assertions.assertEquals(30, result.get(0).getAge());
        Assertions.assertEquals("Bob", result.get(1).getName());
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
            String getProduct() { return product; }
            int getAmount() { return amount; }
        }
        
        Seq<Sale, Exception> seq = Seq.of(
            new Sale("Apple", 10),
            new Sale("Banana", 5),
            new Sale("Apple", 15)
        );
        
        Seq<Sale, Exception> merged = seq.distinctBy(
            Sale::getProduct,
            (s1, s2) -> new Sale(s1.getProduct(), s1.getAmount() + s2.getAmount())
        );
        
        List<Sale> result = merged.toList();
        Assertions.assertEquals(2, result.size());
        Sale appleSale = result.stream().filter(s -> s.getProduct().equals("Apple")).findFirst().orElse(null);
        Assertions.assertNotNull(appleSale);
        Assertions.assertEquals(25, appleSale.getAmount());
    }

    @Test
    public void testMap() throws Exception {
        Seq<String, Exception> seq = Seq.of("Alice", "Bob", "Charlie");
        Seq<Integer, Exception> lengths = seq.map(String::length);
        Assertions.assertEquals(Arrays.asList(5, 3, 7), lengths.toList());

        // Test with transformation to different type
        Seq<Integer, Exception> numbers = Seq.of(1, 2, 3, 4);
        Seq<String, Exception> strings = numbers.map(n -> "Number: " + n);
        Assertions.assertEquals(Arrays.asList("Number: 1", "Number: 2", "Number: 3", "Number: 4"), 
            strings.toList());

        // Test with empty sequence
        Seq<String, Exception> empty = Seq.empty();
        Seq<Integer, Exception> mapped = empty.map(String::length);
        Assertions.assertTrue(mapped.toList().isEmpty());
    }

    // Test for edge cases and error conditions
    @Test
    public void testEdgeCases() throws Exception {
        // Test advance method with primitive array sequences
        Seq<Integer, Exception> seq = Seq.of(new int[]{1, 2, 3, 4, 5});
        Throwables.Iterator<Integer, Exception> iter = seq.iteratorEx();
        iter.advance(2);
        Assertions.assertTrue(iter.hasNext());
        Assertions.assertEquals(3, iter.next());

        // Test count method
        Seq<Integer, Exception> seq2 = Seq.of(new int[]{1, 2, 3, 4, 5});
        Throwables.Iterator<Integer, Exception> iter2 = seq2.iteratorEx();
        Assertions.assertEquals(5, iter2.count());
        iter2.next();
        Assertions.assertEquals(4, iter2.count());
    }

    @Test
    public void testComplexScenarios() throws Exception {
        // Test chaining multiple operations
        Seq<Integer, Exception> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
            .filter(n -> n > 3)
            .map(n -> n * 2)
            .takeWhile(n -> n < 16)
            .distinct();
        
        Assertions.assertEquals(Arrays.asList(8, 10, 12, 14), result.toList());

        // Test with mixed operations including zip and merge
        Integer[] arr1 = {1, 3, 5};
        Integer[] arr2 = {2, 4, 6};
        Seq<Integer, Exception> merged = Seq.merge(arr1, arr2, 
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Seq<String, Exception> mapped = merged.map(n -> "Val: " + n);
        Assertions.assertEquals(Arrays.asList("Val: 1", "Val: 2", "Val: 3", "Val: 4", "Val: 5", "Val: 6"), 
            mapped.toList());
    }

    @Test
    public void testIteratorExhaustion() throws Exception {
        // Test NoSuchElementException when iterator is exhausted
        Seq<Integer, Exception> seq = Seq.of(1, 2);
        Throwables.Iterator<Integer, Exception> iter = seq.iteratorEx();
        iter.next();
        iter.next();
        Assertions.assertThrows(NoSuchElementException.class, () -> iter.next());

        // Test hasNext after exhaustion
        Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testNullHandling() throws Exception {
        // Test with null elements in sequences
        Seq<String, Exception> seq = Seq.of("a", null, "b", null, "c");
        Assertions.assertEquals(Arrays.asList("a", null, "b", null, "c"), seq.toList());

        // Test distinct with null elements
        Seq<String, Exception> distinct = Seq.of("a", null, "b", null, "c").distinct();
        Assertions.assertEquals(Arrays.asList("a", null, "b", "c"), distinct.toList());

        // Test filter with null handling
        Seq<String, Exception> filtered = Seq.of("a", null, "b", null, "c")
            .filter(s -> s != null);
        Assertions.assertEquals(Arrays.asList("a", "b", "c"), filtered.toList());
    }

    @Test
    public void testLargeSequences() throws Exception {
        // Test with large range
        Seq<Integer, Exception> largeSeq = Seq.range(0, 1000);
        Assertions.assertEquals(1000, largeSeq.count());

        // Test filter on large sequence
        Seq<Integer, Exception> filtered = Seq.range(0, 1000)
            .filter(n -> n % 100 == 0);
        Assertions.assertEquals(Arrays.asList(0, 100, 200, 300, 400, 500, 600, 700, 800, 900), 
            filtered.toList());
    }

    @Test
    public void testSequenceReuse() throws Exception {
        // Sequences should not be reusable after terminal operation
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3);
        List<Integer> first = seq.toList();
        Assertions.assertEquals(Arrays.asList(1, 2, 3), first);
        
        // Attempting to use the same sequence again should throw exception
        Assertions.assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testCloseHandlers() throws Exception {
        // Test that close handlers are properly propagated
        boolean[] closed = {false};
        Seq<Integer, Exception> seq = Seq.of(1, 2, 3)
            .onClose(() -> closed[0] = true);
        
        seq.close();
        Assertions.assertTrue(closed[0]);

        // Test with multiple sequences and concat
        boolean[] closed1 = {false};
        boolean[] closed2 = {false};
        Seq<Integer, Exception> seq1 = Seq.of(1, 2).onClose(() -> closed1[0] = true);
        Seq<Integer, Exception> seq2 = Seq.of(3, 4).onClose(() -> closed2[0] = true);
        
        Seq<Integer, Exception> concat = Seq.concat(seq1, seq2);
        concat.close();
        Assertions.assertTrue(closed1[0]);
        Assertions.assertTrue(closed2[0]);
    }

    @Test
    public void testSpecialCharactersInSplit() throws Exception {
        // Test split with special regex characters
        String str = "a.b.c.d";
        Seq<String, Exception> seq = Seq.split(str, Pattern.compile("\\."));
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq.toList());

        // Test split with multiple character delimiter
        String str2 = "a::b::c::d";
        Seq<String, Exception> seq2 = Seq.split(str2, "::");
        Assertions.assertEquals(Arrays.asList("a", "b", "c", "d"), seq2.toList());
    }

    @Test 
    public void testEmptySequenceOperations() throws Exception {
        // Test operations on empty sequences
        Seq<Integer, Exception> empty = Seq.empty();
        
        // Filter on empty
        Seq<Integer, Exception> filtered = empty.filter(n -> n > 0);
        Assertions.assertTrue(filtered.toList().isEmpty());

        // Map on empty
        Seq<String, Exception> mapped = empty.map(Object::toString);
        Assertions.assertTrue(mapped.toList().isEmpty());

        // Distinct on empty
        Seq<Integer, Exception> distinct = empty.distinct();
        Assertions.assertTrue(distinct.toList().isEmpty());

        // TakeWhile on empty
        Seq<Integer, Exception> taken = empty.takeWhile(n -> true);
        Assertions.assertTrue(taken.toList().isEmpty());

        // DropWhile on empty
        Seq<Integer, Exception> dropped = empty.dropWhile(n -> true);
        Assertions.assertTrue(dropped.toList().isEmpty());
    }

    @Test
    public void testFileOperationsWithNonExistentFiles() throws IOException {
        // Test with non-existent file
        File nonExistent = new File("non_existent_file.txt");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            Seq<String, IOException> seq = Seq.ofLines(nonExistent);
            seq.toList(); // This should trigger the IOException
        });

        // Test with non-existent path
        Path nonExistentPath = Paths.get("non_existent_path.txt");
        Assertions.assertThrows(UncheckedIOException.class, () -> {
            Seq<String, IOException> seq = Seq.ofLines(nonExistentPath);
            seq.toList(); // This should trigger the IOException
        });
    }

    @Test
    public void testZipWithNullElements() throws Exception {
        // Test zip with null elements in arrays
        String[] arr1 = {"a", null, "c"};
        String[] arr2 = {"1", "2", "3"};
        
        Seq<String, Exception> seq = Seq.zip(arr1, arr2, (s1, s2) -> (s1 == null ? "null" : s1) + s2);
        Assertions.assertEquals(Arrays.asList("a1", "null2", "c3"), seq.toList());
    }

    @Test
    public void testMergeWithDuplicates() throws Exception {
        // Test merge with duplicate elements
        Integer[] arr1 = {1, 1, 3, 3, 5};
        Integer[] arr2 = {2, 2, 4, 4, 6};
        
        Seq<Integer, Exception> seq = Seq.merge(arr1, arr2, 
            (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        Assertions.assertEquals(Arrays.asList(1, 1, 2, 2, 3, 3, 4, 4, 5, 6), seq.toList());
    }

    @Test
    public void testDistinctByWithComplexKeys() throws Exception {
        // Test distinctBy with complex key extraction
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
        
        Seq<Product, Exception> products = Seq.of(
            new Product("Electronics", "Phone", 999.99),
            new Product("Electronics", "Tablet", 599.99),
            new Product("Books", "Novel", 19.99),
            new Product("Books", "Textbook", 89.99)
        );
        
        // Distinct by category
        Seq<Product, Exception> distinctByCategory = products.distinctBy(p -> p.category);
        List<Product> result = distinctByCategory.toList();
        Assertions.assertEquals(2, result.size());
        Assertions.assertEquals("Electronics", result.get(0).category);
        Assertions.assertEquals("Books", result.get(1).category);
    }

    @Test
    public void testPerformancePatterns() throws Exception {
        // Test lazy evaluation - operations should not execute until terminal operation
        int[] counter = {0};
        Seq<Integer, Exception> seq = Seq.range(0, 1000000)
                .takeWhile(n -> {
                counter[0]++;
                return n < 10;
            })
            .map(n -> n * 2);
        
        // No operations should have been performed yet
        Assertions.assertEquals(0, counter[0]);
        
        // Now trigger evaluation
        List<Integer> result = seq.toList();
        
        // Should have evaluated only until we found all matching elements
        Assertions.assertTrue(counter[0] < 20); // Should be much less than 1000000
        Assertions.assertEquals(Arrays.asList(0, 2, 4, 6, 8, 10, 12, 14, 16, 18), result);
    }
}