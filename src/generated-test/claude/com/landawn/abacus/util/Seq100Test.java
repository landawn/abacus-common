package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.If.OrElse;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.Function;

public class Seq100Test extends TestBase {

    @Test
    public void testEmpty() throws Exception {
        Seq<String, Exception> seq = Seq.empty();
        assertEquals(0, seq.count());
    }

    @Test
    public void testDefer() throws Exception {
        int[] counter = { 0 };
        Seq<Integer, Exception> seq = Seq.defer(() -> {
            counter[0]++;
            return Seq.of(1, 2, 3);
        });

        assertEquals(0, counter[0]); // Supplier not called yet
        assertEquals(3, seq.count());
        assertEquals(1, counter[0]); // Supplier called once
    }

    @Test
    public void testDeferWithNullSupplier() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.defer(null));
    }

    @Test
    public void testJust() throws Exception {
        Seq<String, Exception> seq = Seq.just("test");
        assertEquals("test", seq.first().orElse(null));
    }

    @Test
    public void testJustWithExceptionType() throws Exception {
        Seq<String, IOException> seq = Seq.just("test", IOException.class);
        // assertEquals(1, seq.count());
        assertEquals("test", seq.first().orElse(null));
    }

    @Test
    public void testOfNullable() throws Exception {
        assertEquals(0, Seq.ofNullable(null).count());
        assertEquals(1, Seq.ofNullable("test").count());
    }

    @Test
    public void testOfNullableWithExceptionType() throws Exception {
        assertEquals(0, Seq.ofNullable(null, IOException.class).count());
        assertEquals(1, Seq.ofNullable("test", IOException.class).count());
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
        // boolean array
        boolean[] boolArray = { true, false, true };
        assertEquals(3, Seq.of(boolArray).count());

        // char array
        char[] charArray = { 'a', 'b', 'c' };
        assertEquals(3, Seq.of(charArray).count());

        // byte array
        byte[] byteArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(byteArray).count());

        // short array
        short[] shortArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(shortArray).count());

        // int array
        int[] intArray = { 1, 2, 3 };
        assertEquals(3, Seq.of(intArray).count());

        // long array
        long[] longArray = { 1L, 2L, 3L };
        assertEquals(3, Seq.of(longArray).count());

        // float array
        float[] floatArray = { 1.0f, 2.0f, 3.0f };
        assertEquals(3, Seq.of(floatArray).count());

        // double array
        double[] doubleArray = { 1.0, 2.0, 3.0 };
        assertEquals(3, Seq.of(doubleArray).count());
    }

    @Test
    public void testOfOptional() throws Exception {
        Optional<String> optional = Optional.of("test");
        assertEquals(1, Seq.of(optional).count());

        Optional<String> empty = Optional.empty();
        assertEquals(0, Seq.of(empty).count());

        assertEquals(0, Seq.of((Optional<String>) null).count());
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
    public void testOfIterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Seq.of(list).count());

        assertEquals(0, Seq.of((Iterable<String>) null).count());
    }

    @Test
    public void testOfIterableWithExceptionType() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Seq.of(list, IOException.class).count());
    }

    @Test
    public void testOfIterator() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Seq.of(list.iterator()).count());

        assertEquals(0, Seq.of((Iterator<String>) null).count());
    }

    @Test
    public void testOfThrowablesIterator() throws Exception {
        Throwables.Iterator<String, IOException> iter = new Throwables.Iterator<String, IOException>() {
            private int index = 0;
            private String[] values = { "a", "b", "c" };

            @Override
            public boolean hasNext() throws IOException {
                return index < values.length;
            }

            @Override
            public String next() throws IOException {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return values[index++];
            }
        };

        assertEquals(3, Seq.of(iter).count());
        assertEquals(0, Seq.of((Throwables.Iterator<String, IOException>) null).count());
    }

    @Test
    public void testOfIteratorWithExceptionType() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(3, Seq.of(list.iterator(), IOException.class).count());
    }

    @Test
    public void testOfEnumeration() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("a", "b", "c"));
        assertEquals(3, Seq.of(vector.elements()).count());

        assertEquals(0, Seq.of((Enumeration<String>) null).count());
    }

    @Test
    public void testOfEnumerationWithExceptionType() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("a", "b", "c"));
        assertEquals(3, Seq.of(vector.elements(), IOException.class).count());
    }

    @Test
    public void testOfMap() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Seq<Map.Entry<String, Integer>, Exception> seq = Seq.of(map);
        assertEquals(2, seq.count());

        assertEquals(0, Seq.of((Map<String, Integer>) null).count());
    }

    @Test
    public void testOfMapWithExceptionType() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        assertEquals(2, Seq.of(map, IOException.class).count());
    }

    @Test
    public void testOfKeys() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Set<String> set = Seq.ofKeys(map).toSet();
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "b")));

        assertEquals(0, Seq.ofKeys((Map<String, Integer>) null).count());
    }

    @Test
    public void testOfKeysWithValueFilter() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<String, Exception> seq = Seq.ofKeys(map, v -> v > 1);
        assertTrue(seq.toSet().containsAll(Arrays.asList("b", "c")));
    }

    @Test
    public void testOfKeysWithBiPredicate() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<String, Exception> seq = Seq.ofKeys(map, (k, v) -> k.equals("a") || v > 2);
        // assertEquals(2, seq.count());
        assertTrue(seq.toSet().containsAll(Arrays.asList("a", "c")));
    }

    @Test
    public void testOfValues() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);

        Seq<Integer, Exception> seq = Seq.ofValues(map);
        assertTrue(seq.toSet().containsAll(Arrays.asList(1, 2)));

        assertEquals(0, Seq.ofValues((Map<String, Integer>) null).count());
    }

    @Test
    public void testOfValuesWithKeyFilter() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Seq<Integer, Exception> seq = Seq.ofValues(map, k -> !k.equals("b"));
        assertTrue(seq.toSet().containsAll(Arrays.asList(1, 3)));
    }

    @Test
    public void testOfValuesWithBiPredicate() throws Exception {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);

        Set<Integer> set = Seq.ofValues(map, (k, v) -> k.equals("a") || v > 2).toSet();
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(1, 3)));
    }

    @Test
    public void testOfReversedArray() throws Exception {
        String[] array = { "a", "b", "c" };
        List<String> result = Seq.ofReversed(array).toList();
        assertEquals(Arrays.asList("c", "b", "a"), result);
    }

    @Test
    public void testOfReversedList() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        List<String> result = Seq.ofReversed(list).toList();
        assertEquals(Arrays.asList("c", "b", "a"), result);
    }

    @Test
    public void testRepeat() throws Exception {
        assertEquals(5, Seq.repeat("x", 5).count());
        assertEquals(Arrays.asList("x", "x", "x"), Seq.repeat("x", 3).toList());
        assertEquals(0, Seq.repeat("x", 0).count());
    }

    @Test
    public void testRepeatWithNegativeCount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.repeat("x", -1));
    }

    @Test
    public void testRange() throws Exception {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4), Seq.range(0, 5).toList());
        assertEquals(Arrays.asList(5, 6, 7), Seq.range(5, 8).toList());
        assertEquals(0, Seq.range(5, 5).count());
        assertEquals(0, Seq.range(5, 3).count());
    }

    @Test
    public void testRangeWithStep() throws Exception {
        assertEquals(Arrays.asList(0, 2, 4), Seq.range(0, 6, 2).toList());
        assertEquals(Arrays.asList(10, 8, 6), Seq.range(10, 4, -2).toList());
    }

    @Test
    public void testRangeClosed() throws Exception {
        assertEquals(Arrays.asList(0, 1, 2, 3, 4, 5), Seq.rangeClosed(0, 5).toList());
        assertEquals(Arrays.asList(5, 4, 3), Seq.rangeClosed(5, 3, -1).toList());
    }

    @Test
    public void testRangeClosedWithStep() throws Exception {
        assertEquals(Arrays.asList(0, 2, 4, 6), Seq.rangeClosed(0, 6, 2).toList());
        assertEquals(Arrays.asList(10, 8, 6, 4), Seq.rangeClosed(10, 4, -2).toList());
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
    public void testSplitToLines() throws Exception {
        String text = "line1\nline2\nline3\r\nline4";
        List<String> result = Seq.splitToLines(text).toList();
        assertEquals(4, result.size());
        assertEquals("line1", result.get(0));
        assertEquals("line2", result.get(1));
        assertEquals("line3", result.get(2));
        assertEquals("line4", result.get(3));
    }

    @Test
    public void testSplitToLinesWithOptions() throws Exception {
        String text = "  line1  \n\n  line2  \n";

        // Test with trim = true, omitEmptyLines = true
        List<String> result1 = Seq.splitToLines(text, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result1);

        // Test with trim = true, omitEmptyLines = false
        List<String> result2 = Seq.splitToLines(text, true, false).toList();
        assertEquals(Arrays.asList("line1", "", "line2", ""), result2);

        // Test with trim = false, omitEmptyLines = true
        List<String> result3 = Seq.splitToLines(text, false, true).toList();
        assertEquals(Arrays.asList("  line1  ", "  line2  "), result3);

        // Test with trim = false, omitEmptyLines = false
        List<String> result4 = Seq.splitToLines(text, false, false).toList();
        assertEquals(Arrays.asList("  line1  ", "", "  line2  ", ""), result4);
    }

    @Test
    public void testSplitByChunkCount() throws Exception {
        int[] array = { 1, 2, 3, 4, 5, 6, 7 };

        List<int[]> result1 = Seq.splitByChunkCount(7, 3, (from, to) -> Arrays.copyOfRange(array, from, to)).toList();

        assertEquals(3, result1.size());
        assertArrayEquals(new int[] { 1, 2, 3 }, result1.get(0));
        assertArrayEquals(new int[] { 4, 5 }, result1.get(1));
        assertArrayEquals(new int[] { 6, 7 }, result1.get(2));
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() throws Exception {
        int[] array = { 1, 2, 3, 4, 5, 6, 7 };

        List<int[]> result = Seq.splitByChunkCount(7, 5, true, (from, to) -> Arrays.copyOfRange(array, from, to)).toList();

        assertEquals(5, result.size());
        assertArrayEquals(new int[] { 1 }, result.get(0));
        assertArrayEquals(new int[] { 2 }, result.get(1));
        assertArrayEquals(new int[] { 3 }, result.get(2));
        assertArrayEquals(new int[] { 4, 5 }, result.get(3));
        assertArrayEquals(new int[] { 6, 7 }, result.get(4));
    }

    @Test
    public void testSplitByChunkCountWithNegativeTotalSize() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(-1, 3, (from, to) -> new Object()));
    }

    @Test
    public void testSplitByChunkCountWithZeroMaxChunkCount() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> Seq.splitByChunkCount(10, 0, (from, to) -> new Object()));
    }

    @TempDir
    Path tempFolder;

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
        File dir = Files.createTempDirectory(tempFolder, "testDir").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        file1.createNewFile();
        file2.createNewFile();

        Set<String> fileNames = Seq.listFiles(dir).map(File::getName).toSet();

        assertEquals(2, fileNames.size());
        assertTrue(fileNames.contains("file1.txt"));
        assertTrue(fileNames.contains("file2.txt"));
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
        assertEquals(3, files.size()); // subDir, file1.txt, file2.txt

        Set<String> names = files.stream().map(File::getName).collect(Collectors.toSet());
        assertTrue(names.contains("file1.txt"));
        assertTrue(names.contains("file2.txt"));
        assertTrue(names.contains("subDir"));
    }

    @Test
    public void testConcatArrays() throws Exception {
        String[] arr1 = { "a", "b" };
        String[] arr2 = { "c", "d" };
        String[] arr3 = { "e" };

        List<String> result = Seq.concat(arr1, arr2, arr3).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);

        assertEquals(0, Seq.concat(N.EMPTY_STRING_ARRAY).count());
    }

    @Test
    public void testConcatIterables() throws Exception {
        List<String> list1 = Arrays.asList("a", "b");
        List<String> list2 = Arrays.asList("c", "d");
        Set<String> set = new HashSet<>(Arrays.asList("e"));

        List<String> result = Seq.concat(list1, list2, set).toList();
        assertEquals(5, result.size());
        assertTrue(result.containsAll(Arrays.asList("a", "b", "c", "d", "e")));
    }

    @Test
    public void testConcatIterators() throws Exception {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<String> iter2 = Arrays.asList("c", "d").iterator();

        List<String> result = Seq.concat(iter1, iter2).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testConcatSeqs() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<String, Exception> seq2 = Seq.of("c", "d");
        Seq<String, Exception> seq3 = Seq.of("e");

        List<String> result = Seq.concat(seq1, seq2, seq3).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testConcatCollectionOfSeqs() throws Exception {
        List<Seq<String, Exception>> seqs = Arrays.asList(Seq.of("a", "b"), Seq.of("c", "d"), Seq.of("e"));

        List<String> result = Seq.concat(seqs).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d", "e"), result);
    }

    @Test
    public void testZipArrays() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2, 3 };

        List<String> result = Seq.zip(arr1, arr2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipArraysWithDifferentLengths() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2 };

        List<String> result = Seq.zip(arr1, arr2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipThreeArrays() throws Exception {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1, 2 };
        Boolean[] arr3 = { true, false };

        List<String> result = Seq.zip(arr1, arr2, arr3, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipIterables() throws Exception {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2, 3);

        List<String> result = Seq.zip(list1, list2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeIterables() throws Exception {
        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = Arrays.asList(1, 2);
        List<Boolean> list3 = Arrays.asList(true, false);

        List<String> result = Seq.zip(list1, list2, list3, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipIterators() throws Exception {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2, 3).iterator();

        List<String> result = Seq.zip(iter1, iter2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeIterators() throws Exception {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false).iterator();

        List<String> result = Seq.zip(iter1, iter2, iter3, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipSeqs() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2, 3);

        List<String> result = Seq.zip(seq1, seq2, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeSeqs() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2);
        Seq<Boolean, Exception> seq3 = Seq.of(true, false);

        List<String> result = Seq.zip(seq1, seq2, seq3, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b2false"), result);
    }

    @Test
    public void testZipArraysWithDefaults() throws Exception {
        String[] arr1 = { "a", "b", "c" };
        Integer[] arr2 = { 1, 2 };

        List<String> result = Seq.zip(arr1, arr2, "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeArraysWithDefaults() throws Exception {
        String[] arr1 = { "a", "b" };
        Integer[] arr2 = { 1 };
        Boolean[] arr3 = { true, false, true };

        List<String> result = Seq.zip(arr1, arr2, arr3, "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testZipIterablesWithDefaults() throws Exception {
        List<String> list1 = Arrays.asList("a", "b", "c");
        List<Integer> list2 = Arrays.asList(1, 2);

        List<String> result = Seq.zip(list1, list2, "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeIterablesWithDefaults() throws Exception {
        List<String> list1 = Arrays.asList("a", "b");
        List<Integer> list2 = Arrays.asList(1);
        List<Boolean> list3 = Arrays.asList(true, false, true);

        List<String> result = Seq.zip(list1, list2, list3, "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testZipIteratorsWithDefaults() throws Exception {
        Iterator<String> iter1 = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1, 2).iterator();

        List<String> result = Seq.zip(iter1, iter2, "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeIteratorsWithDefaults() throws Exception {
        Iterator<String> iter1 = Arrays.asList("a", "b").iterator();
        Iterator<Integer> iter2 = Arrays.asList(1).iterator();
        Iterator<Boolean> iter3 = Arrays.asList(true, false, true).iterator();

        List<String> result = Seq.zip(iter1, iter2, iter3, "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testZipSeqsWithDefaults() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b", "c");
        Seq<Integer, Exception> seq2 = Seq.of(1, 2);

        List<String> result = Seq.zip(seq1, seq2, "x", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeSeqsWithDefaults() throws Exception {
        Seq<String, Exception> seq1 = Seq.of("a", "b");
        Seq<Integer, Exception> seq2 = Seq.of(1);
        Seq<Boolean, Exception> seq3 = Seq.of(true, false, true);

        List<String> result = Seq.zip(seq1, seq2, seq3, "x", 99, false, (s, i, b) -> s + i + b).toList();
        assertEquals(Arrays.asList("a1true", "b99false", "x99true"), result);
    }

    @Test
    public void testMergeArrays() throws Exception {
        Integer[] arr1 = { 1, 3, 5 };
        Integer[] arr2 = { 2, 4, 6 };

        List<Integer> result = Seq.merge(arr1, arr2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeThreeArrays() throws Exception {
        Integer[] arr1 = { 1, 4, 7 };
        Integer[] arr2 = { 2, 5, 8 };
        Integer[] arr3 = { 3, 6, 9 };

        List<Integer> result = Seq.merge(arr1, arr2, arr3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testMergeIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 3, 5);
        List<Integer> list2 = Arrays.asList(2, 4, 6);

        List<Integer> result = Seq.merge(list1, list2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeThreeIterables() throws Exception {
        List<Integer> list1 = Arrays.asList(1, 4, 7);
        List<Integer> list2 = Arrays.asList(2, 5, 8);
        List<Integer> list3 = Arrays.asList(3, 6, 9);

        List<Integer> result = Seq.merge(list1, list2, list3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testMergeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 3, 5).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 4, 6).iterator();

        List<Integer> result = Seq.merge(iter1, iter2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeThreeIterators() throws Exception {
        Iterator<Integer> iter1 = Arrays.asList(1, 4, 7).iterator();
        Iterator<Integer> iter2 = Arrays.asList(2, 5, 8).iterator();
        Iterator<Integer> iter3 = Arrays.asList(3, 6, 9).iterator();

        List<Integer> result = Seq.merge(iter1, iter2, iter3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testMergeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 3, 5);
        Seq<Integer, Exception> seq2 = Seq.of(2, 4, 6);

        List<Integer> result = Seq.merge(seq1, seq2, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeThreeSeqs() throws Exception {
        Seq<Integer, Exception> seq1 = Seq.of(1, 4, 7);
        Seq<Integer, Exception> seq2 = Seq.of(2, 5, 8);
        Seq<Integer, Exception> seq3 = Seq.of(3, 6, 9);

        List<Integer> result = Seq.merge(seq1, seq2, seq3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);
    }

    @Test
    public void testTakeLast() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).takeLast(3).toList();
        assertEquals(Arrays.asList(3, 4, 5), result);
    }

    @Test
    public void testTop() throws Exception {
        List<Integer> result = Seq.of(5, 2, 8, 1, 9, 3).top(3).toList();
        assertTrue(result.containsAll(Arrays.asList(8, 9, 5)));
    }

    @Test
    public void testTopWithComparator() throws Exception {
        List<String> result = Seq.of("aa", "b", "ccc", "dd", "e").top(3, Comparator.comparingInt(String::length)).toList();
        assertTrue(result.containsAll(Arrays.asList("ccc", "aa", "dd")));
    }

    @Test
    public void testReversed() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).reversed().toList();
        assertEquals(Arrays.asList(5, 4, 3, 2, 1), result);
    }

    @Test
    public void testRotated() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(2).toList();
        assertEquals(Arrays.asList(4, 5, 1, 2, 3), result);
    }

    @Test
    public void testRotatedNegative() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).rotated(-2).toList();
        assertEquals(Arrays.asList(3, 4, 5, 1, 2), result);
    }

    @Test
    public void testShuffled() throws Exception {
        List<Integer> original = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).shuffled().toList();
        assertEquals(original.size(), result.size());
        assertTrue(result.containsAll(original));
    }

    @Test
    public void testShuffledWithRandom() throws Exception {
        Random random = new Random(42);
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).shuffled(random).toList();
        assertEquals(5, result.size());
    }

    @Test
    public void testSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5, 9).sorted().toList();
        assertEquals(Arrays.asList(1, 1, 3, 4, 5, 9), result);
    }

    @Test
    public void testSortedWithComparator() throws Exception {
        List<String> result = Seq.of("bb", "aaa", "c", "dddd").sorted(Comparator.comparingInt(String::length)).toList();
        assertEquals(Arrays.asList("c", "bb", "aaa", "dddd"), result);
    }

    @Test
    public void testSortedByInt() throws Exception {
        List<String> result = Seq.of("333", "1", "22").sortedByInt(s -> Integer.parseInt(s)).toList();
        assertEquals(Arrays.asList("1", "22", "333"), result);
    }

    @Test
    public void testSortedByLong() throws Exception {
        List<String> result = Seq.of("333", "1", "22").sortedByLong(s -> Long.parseLong(s)).toList();
        assertEquals(Arrays.asList("1", "22", "333"), result);
    }

    @Test
    public void testSortedByDouble() throws Exception {
        List<String> result = Seq.of("3.3", "1.1", "2.2").sortedByDouble(s -> Double.parseDouble(s)).toList();
        assertEquals(Arrays.asList("1.1", "2.2", "3.3"), result);
    }

    @Test
    public void testSortedBy() throws Exception {
        List<String> result = Seq.of("bb", "aaa", "c").sortedBy(String::length).toList();
        assertEquals(Arrays.asList("c", "bb", "aaa"), result);
    }

    @Test
    public void testReverseSorted() throws Exception {
        List<Integer> result = Seq.of(3, 1, 4, 1, 5, 9).reverseSorted().toList();
        assertEquals(Arrays.asList(9, 5, 4, 3, 1, 1), result);
    }

    @Test
    public void testReverseSortedWithComparator() throws Exception {
        List<String> result = Seq.of("bb", "aaa", "c", "dddd").reverseSorted(Comparator.comparingInt(String::length)).toList();
        assertEquals(Arrays.asList("dddd", "aaa", "bb", "c"), result);
    }

    @Test
    public void testReverseSortedByInt() throws Exception {
        List<String> result = Seq.of("333", "1", "22").reverseSortedByInt(s -> Integer.parseInt(s)).toList();
        assertEquals(Arrays.asList("333", "22", "1"), result);
    }

    @Test
    public void testReverseSortedByLong() throws Exception {
        List<String> result = Seq.of("333", "1", "22").reverseSortedByLong(s -> Long.parseLong(s)).toList();
        assertEquals(Arrays.asList("333", "22", "1"), result);
    }

    @Test
    public void testReverseSortedByDouble() throws Exception {
        List<String> result = Seq.of("3.3", "1.1", "2.2").reverseSortedByDouble(s -> Double.parseDouble(s)).toList();
        assertEquals(Arrays.asList("3.3", "2.2", "1.1"), result);
    }

    @Test
    public void testReverseSortedBy() throws Exception {
        List<String> result = Seq.of("bb", "aaa", "c").reverseSortedBy(String::length).toList();
        assertEquals(Arrays.asList("aaa", "bb", "c"), result);
    }

    @Test
    public void testCycled() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled().limit(8).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3, 1, 2), result);
    }

    @Test
    public void testCycledWithRounds() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).cycled(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 1, 2, 3), result);
    }

    @Test
    public void testRateLimited() throws Exception {
        RateLimiter rateLimiter = RateLimiter.create(10.0); // 10 permits per second
        long start = System.currentTimeMillis();

        Seq.of(1, 2, 3).rateLimited(rateLimiter).forEach(x -> {
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 200); // Should take at least 200ms for 3 items at 10/sec
    }

    @Test
    public void testRateLimitedWithPermitsPerSecond() throws Exception {
        long start = System.currentTimeMillis();

        Seq.of(1, 2, 3).rateLimited(10.0).forEach(x -> {
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 200); // Should take at least 200ms for 3 items at 10/sec
    }

    @Test
    public void testDelay() throws Exception {
        long start = System.currentTimeMillis();

        Seq.of(1, 2, 3).delay(Duration.ofMillis(50)).forEach(x -> {
        });

        long duration = System.currentTimeMillis() - start;
        assertTrue(duration >= 100); // Should take at least 100ms for 2 delays
    }

    @Test
    public void testIntersperse() throws Exception {
        List<String> result = Seq.of("a", "b", "c").intersperse("-").toList();
        assertEquals(Arrays.asList("a", "-", "b", "-", "c"), result);
    }

    @Test
    public void testStep() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9).step(3).toList();
        assertEquals(Arrays.asList(1, 4, 7), result);
    }

    @Test
    public void testIndexed() throws Exception {
        List<Indexed<String>> result = Seq.of("a", "b", "c").indexed().toList();
        assertEquals(3, result.size());
        assertEquals(0, result.get(0).index());
        assertEquals("a", result.get(0).value());
        assertEquals(1, result.get(1).index());
        assertEquals("b", result.get(1).value());
        assertEquals(2, result.get(2).index());
        assertEquals("c", result.get(2).value());
    }

    @Test
    public void testBuffered() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).buffered().toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testBufferedWithSize() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).buffered(2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testGroupBy() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc", "dd", "e").groupBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(3, result.size());
        assertEquals(Arrays.asList("a", "e"), result.get(1));
        assertEquals(Arrays.asList("bb", "dd"), result.get(2));
        assertEquals(Arrays.asList("ccc"), result.get(3));
    }

    @Test
    public void testGroupByWithMapFactory() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc")
                .groupBy(String::length, Suppliers.ofLinkedHashMap())
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertFalse(result instanceof LinkedHashMap);
    }

    @Test
    public void testGroupByWithValueMapper() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc").groupBy(String::length, String::toUpperCase).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(Arrays.asList("A"), result.get(1));
        assertEquals(Arrays.asList("BB"), result.get(2));
        assertEquals(Arrays.asList("CCC"), result.get(3));
    }

    @Test
    public void testGroupByWithValueMapperAndMapFactory() throws Exception {
        Map<Integer, List<String>> result = Seq.of("a", "bb", "ccc")
                .groupBy(String::length, String::toUpperCase, Suppliers.ofLinkedHashMap())
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());

        assertTrue(result instanceof LinkedHashMap);
        assertEquals(Arrays.asList("A"), result.get(1));
    }

    @Test
    public void testGroupByWithMergeFunction() throws Exception {
        Map<Integer, String> result = Seq.of("a", "b", "cc", "dd", "eee")
                .groupBy(String::length, Function.identity(), (s1, s2) -> s1 + s2)
                .toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals("ab", result.get(1));
        assertEquals("ccdd", result.get(2));
        assertEquals("eee", result.get(3));
    }

    @Test
    public void testGroupByWithMergeFunctionAndMapFactory() throws Exception {
        Map<Integer, String> result = Seq.of("a", "b", "cc", "dd")
                .groupBy(String::length, Function.identity(), (s1, s2) -> s1 + s2, LinkedHashMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());

        assertTrue(result instanceof LinkedHashMap);
        assertEquals("ab", result.get(1));
        assertEquals("ccdd", result.get(2));
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
    public void testPartitionBy() throws Exception {
        Map<Boolean, List<Integer>> result = Seq.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, result.size());
        assertEquals(Arrays.asList(2, 4), result.get(true));
        assertEquals(Arrays.asList(1, 3, 5), result.get(false));
    }

    @Test
    public void testPartitionByWithDownstream() throws Exception {
        Map<Boolean, Long> result = Seq.of(1, 2, 3, 4, 5).partitionBy(x -> x % 2 == 0, Collectors.counting()).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2L, result.get(true).longValue());
        assertEquals(3L, result.get(false).longValue());
    }

    @Test
    public void testCountBy() throws Exception {
        Map<Integer, Integer> result = Seq.of("a", "bb", "ccc", "dd", "e").countBy(String::length).toMap(Map.Entry::getKey, Map.Entry::getValue);

        assertEquals(2, result.get(1).intValue());
        assertEquals(2, result.get(2).intValue());
        assertEquals(1, result.get(3).intValue());
    }

    @Test
    public void testCountByWithMapFactory() throws Exception {
        Map<Integer, Integer> result = Seq.of("a", "bb", "ccc", "dd", "e")
                .countBy(String::length, LinkedHashMap::new)
                .toMap(Map.Entry::getKey, Map.Entry::getValue, Suppliers.ofLinkedHashMap());

        assertTrue(result instanceof LinkedHashMap);
    }

    @Test
    public void testIntersection() throws Exception {
        List<Integer> result = Seq.of(1, 2, 2, 3, 4, 5).intersection(Arrays.asList(2, 2, 3, 6)).toList();
        assertEquals(Arrays.asList(2, 2, 3), result);
    }

    @Test
    public void testIntersectionWithMapper() throws Exception {
        List<String> result = Seq.of("a", "B", "c", "D").intersection(String::toLowerCase, Arrays.asList("b", "d", "e")).toList();
        assertEquals(Arrays.asList("B", "D"), result);
    }

    @Test
    public void testDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 2, 3, 4, 5).difference(Arrays.asList(2, 3, 6)).toList();
        assertEquals(Arrays.asList(1, 2, 4, 5), result);
    }

    @Test
    public void testDifferenceWithMapper() throws Exception {
        List<String> result = Seq.of("a", "B", "c", "D").difference(String::toLowerCase, Arrays.asList("b", "d")).toList();
        assertEquals(Arrays.asList("a", "c"), result);
    }

    @Test
    public void testSymmetricDifference() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).symmetricDifference(Arrays.asList(2, 3, 4)).toList();
        assertEquals(Arrays.asList(1, 4), result);
    }

    @Test
    public void testPrepend() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(1, 2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrependCollection() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(Arrays.asList(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrependSeq() throws Exception {
        List<Integer> result = Seq.of(3, 4, 5).prepend(Seq.of(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testPrependOptional() throws Exception {
        List<Integer> result = Seq.of(2, 3).prepend(Optional.of(1)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        List<Integer> result2 = Seq.of(2, 3).prepend(Optional.<Integer> empty()).toList();
        assertEquals(Arrays.asList(2, 3), result2);
    }

    @Test
    public void testAppend() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(4, 5).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppendCollection() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(Arrays.asList(4, 5)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppendSeq() throws Exception {
        List<Integer> result = Seq.of(1, 2, 3).append(Seq.of(4, 5)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testAppendOptional() throws Exception {
        List<Integer> result = Seq.of(1, 2).append(Optional.of(3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        List<Integer> result2 = Seq.of(1, 2).append(Optional.<Integer> empty()).toList();
        assertEquals(Arrays.asList(1, 2), result2);
    }

    @Test
    public void testAppendIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        List<Integer> result2 = Seq.of(4, 5).appendIfEmpty(1, 2, 3).toList();
        assertEquals(Arrays.asList(4, 5), result2);
    }

    @Test
    public void testAppendIfEmptyCollection() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().appendIfEmpty(Arrays.asList(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testAppendIfEmptySupplier() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().appendIfEmpty(() -> Seq.of(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testDefaultIfEmpty() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().defaultIfEmpty(42).toList();
        assertEquals(Arrays.asList(42), result);

        List<Integer> result2 = Seq.of(1, 2, 3).defaultIfEmpty(42).toList();
        assertEquals(Arrays.asList(1, 2, 3), result2);
    }

    @Test
    public void testDefaultIfEmptySupplier() throws Exception {
        List<Integer> result = Seq.<Integer, Exception> empty().defaultIfEmpty(() -> Seq.of(1, 2, 3)).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testThrowIfEmpty() throws Exception {
        try {
            Seq.<Integer, Exception> empty().throwIfEmpty().toList();
            fail("Should have thrown NoSuchElementException");
        } catch (NoSuchElementException e) {
            // Expected
        }

        // Should not throw for non-empty
        List<Integer> result = Seq.of(1, 2, 3).throwIfEmpty().toList();
        assertEquals(Arrays.asList(1, 2, 3), result);
    }

    @Test
    public void testThrowIfEmptyWithSupplier() throws Exception {
        try {
            Seq.<Integer, Exception> empty().throwIfEmpty(() -> new IllegalStateException("Empty!")).toList();
            fail("Should have thrown IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Empty!", e.getMessage());
        }
    }

    @Test
    public void testIfEmpty() throws Exception {
        boolean[] executed = { false };

        Seq.<Integer, Exception> empty().ifEmpty(() -> executed[0] = true).toList();

        assertTrue(executed[0]);

        executed[0] = false;
        Seq.of(1, 2, 3).ifEmpty(() -> executed[0] = true).toList();

        assertFalse(executed[0]);
    }

    @Test
    public void testOnEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).onEach(collected::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testOnFirst() throws Exception {
        List<Integer> collected = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).onFirst(collected::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(1), collected);
    }

    @Test
    public void testOnLast() throws Exception {
        List<Integer> collected = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).onLast(collected::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(3), collected);
    }

    @Test
    public void testPeek() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).peek(peeked::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(1, 2, 3), peeked);
    }

    @Test
    public void testPeekFirst() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).peekFirst(peeked::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(1), peeked);
    }

    @Test
    public void testPeekLast() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3).peekLast(peeked::add).toList();

        assertEquals(Arrays.asList(1, 2, 3), result);
        assertEquals(Arrays.asList(3), peeked);
    }

    @Test
    public void testPeekIf() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).peekIf(x -> x % 2 == 0, peeked::add).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testPeekIfWithBiPredicate() throws Exception {
        List<Integer> peeked = new ArrayList<>();
        List<Integer> result = Seq.of(1, 2, 3, 4, 5).peekIf((value, index) -> index % 2 == 0, peeked::add).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
        assertEquals(Arrays.asList(2, 4), peeked);
    }

    @Test
    public void testMergeWith() throws Exception {
        List<Integer> result = Seq.of(1, 3, 5).mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWithSeq() throws Exception {
        List<Integer> result = Seq.of(1, 3, 5).mergeWith(Seq.of(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipWith() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Arrays.asList(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
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
    public void testZipWithSeq() throws Exception {
        List<String> result = Seq.of("a", "b", "c").zipWith(Seq.of(1, 2, 3), (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
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
    public void testSaveEachToFile() throws Exception {
        File output = Files.createTempFile(tempFolder, "test", ".txt").toFile();

        List<String> data = Arrays.asList("line1", "line2", "line3");
        List<String> result = Seq.of(data).saveEach(output).toList();

        assertEquals(data, result);
        assertEquals(Arrays.asList("line1", "line2", "line3"), Files.readAllLines(output.toPath()));
    }

    @Test
    public void testSaveEachToFileWithMapper() throws Exception {
        File output = Files.createTempFile(tempFolder, "test", ".txt").toFile();

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach(n -> "Number: " + n, output).toList();

        assertEquals(data, result);
        assertEquals(Arrays.asList("Number: 1", "Number: 2", "Number: 3"), Files.readAllLines(output.toPath()));
    }

    @Test
    public void testSaveEachToOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        List<String> data = Arrays.asList("line1", "line2");
        List<String> result = Seq.of(data).saveEach(s -> s, baos).toList();

        assertEquals(data, result);
        String content = baos.toString();
        assertTrue(content.contains("line1"));
        assertTrue(content.contains("line2"));
    }

    @Test
    public void testSaveEachToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        List<String> data = Arrays.asList("line1", "line2");
        List<String> result = Seq.of(data).saveEach(s -> s, writer).toList();

        assertEquals(data, result);
        String content = writer.toString();
        assertTrue(content.contains("line1"));
        assertTrue(content.contains("line2"));
    }

    @Test
    public void testSaveEachWithBiConsumerToFile() throws Exception {
        File output = Files.createTempFile(tempFolder, "test", ".txt").toFile();

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach((n, w) -> w.write("Num: " + n), output).toList();

        assertEquals(data, result);
        List<String> lines = Files.readAllLines(output.toPath());
        assertEquals(Arrays.asList("Num: 1", "Num: 2", "Num: 3"), lines);
    }

    @Test
    public void testSaveEachWithBiConsumerToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach((n, w) -> w.write("Num: " + n), writer).toList();

        assertEquals(data, result);
        String content = writer.toString();
        assertTrue(content.contains("Num: 1"));
        assertTrue(content.contains("Num: 2"));
        assertTrue(content.contains("Num: 3"));
    }

    @Test
    public void testSaveEachToPreparedStatement() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach(stmt, (n, ps) -> {
            ps.setInt(1, n);
            ps.setString(2, "Value" + n);
        }).toList();

        assertEquals(data, result);
        verify(stmt, times(3)).setInt(anyInt(), anyInt());
        verify(stmt, times(3)).setString(anyInt(), anyString());
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testSaveEachToPreparedStatementBatch() throws Exception {
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(stmt.executeBatch()).thenReturn(new int[] { 1, 1 });

        List<Integer> data = Arrays.asList(1, 2, 3, 4, 5);
        List<Integer> result = Seq.of(data).saveEach(stmt, 2, 0, (n, ps) -> {
            ps.setInt(1, n);
        }).toList();

        assertEquals(data, result);
        verify(stmt, times(5)).setInt(anyInt(), anyInt());
        verify(stmt, times(5)).addBatch();
        verify(stmt, times(3)).executeBatch(); // 2 + 2 + 1
    }

    //    @Test
    //    public void testSaveEachToConnection() throws Exception {
    //        Connection conn = mock(Connection.class);
    //        PreparedStatement stmt = mock(PreparedStatement.class);
    //        when(conn.prepareStatement(anyString())).thenReturn(stmt);
    //
    //        List<Integer> data = Arrays.asList(1, 2, 3);
    //        List<Integer> result = Seq.of(data).saveEach(conn, "INSERT INTO test VALUES (?)", (n, ps) -> {
    //            ps.setInt(1, n);
    //        }).toList();
    //
    //        assertEquals(data, result);
    //        verify(conn).prepareStatement("INSERT INTO test VALUES (?)");
    //        verify(stmt, times(3)).setInt(1, anyInt());
    //        verify(stmt, times(3)).execute();
    //    }

    @Test
    public void testSaveEachToConnectionBatch() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[] { 1, 1 });

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach(conn, "INSERT INTO test VALUES (?)", 2, 0, (n, ps) -> {
            ps.setInt(1, n);
        }).toList();

        assertEquals(data, result);
        verify(stmt, times(3)).addBatch();
        verify(stmt, times(2)).executeBatch(); // 2 + 1
    }

    @Test
    public void testSaveEachToDataSource() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach(ds, "INSERT INTO test VALUES (?)", (n, ps) -> {
            ps.setInt(1, n);
        }).toList();

        assertEquals(data, result);
        verify(ds).getConnection();
        verify(stmt, times(3)).execute();
    }

    @Test
    public void testSaveEachToDataSourceBatch() throws Exception {
        DataSource ds = mock(DataSource.class);
        Connection conn = mock(Connection.class);
        PreparedStatement stmt = mock(PreparedStatement.class);
        when(ds.getConnection()).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(stmt);
        when(stmt.executeBatch()).thenReturn(new int[] { 1, 1 });

        List<Integer> data = Arrays.asList(1, 2, 3);
        List<Integer> result = Seq.of(data).saveEach(ds, "INSERT INTO test VALUES (?)", 2, 0, (n, ps) -> {
            ps.setInt(1, n);
        }).toList();

        assertEquals(data, result);
        verify(stmt, times(3)).addBatch();
        verify(stmt, times(2)).executeBatch();
    }

    @Test
    public void testTransform() throws Exception {
        List<String> result = Seq.of(1, 2, 3).transform(seq -> seq.map(x -> x * 2).map(Object::toString)).toList();

        assertEquals(Arrays.asList("2", "4", "6"), result);
    }

    @Test
    public void testTransformBDeferred() throws Exception {
        List<String> result = Seq.of(1, 2, 3).transformB(stream -> stream.map(x -> x * 2).map(Object::toString), true).toList();

        assertEquals(Arrays.asList("2", "4", "6"), result);
    }

    @Test
    public void testOnCloseMultiple() throws Exception {
        List<String> closeOrder = new ArrayList<>();

        Seq.of(1, 2, 3).onClose(() -> closeOrder.add("first")).onClose(() -> closeOrder.add("second")).onClose(() -> closeOrder.add("third")).toList();

        assertEquals(Arrays.asList("first", "second", "third"), closeOrder);
    }

    @Test
    public void testForeach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).foreach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEach() throws Exception {
        List<Integer> collected = new ArrayList<>();
        Seq.of(1, 2, 3).forEach(collected::add);
        assertEquals(Arrays.asList(1, 2, 3), collected);
    }

    @Test
    public void testForEachIndexed() throws Exception {
        Map<Integer, String> collected = new HashMap<>();
        Seq.of("a", "b", "c").forEachIndexed((index, value) -> collected.put(index, value));

        assertEquals("a", collected.get(0));
        assertEquals("b", collected.get(1));
        assertEquals("c", collected.get(2));
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
    public void testForEachWithOnComplete() throws Exception {
        List<Integer> collected = new ArrayList<>();
        boolean[] completed = { false };

        Seq.of(1, 2, 3).forEach(collected::add, () -> completed[0] = true);

        assertEquals(Arrays.asList(1, 2, 3), collected);
        assertTrue(completed[0]);
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
    public void testForEachPair() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of(1, 2, 3, 4).forEachPair((a, b) -> collected.add(a + "+" + b));

        assertEquals(Arrays.asList("1+2", "2+3", "3+4"), collected);
    }

    @Test
    public void testForEachPairWithIncrement() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5).forEachPair(2, (a, b) -> collected.add(a + "+" + b));

        assertEquals(Arrays.asList("1+2", "3+4", "5+null"), collected);
    }

    @Test
    public void testForEachTriple() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple((a, b, c) -> collected.add(a + "+" + b + "+" + c));

        assertEquals(Arrays.asList("1+2+3", "2+3+4", "3+4+5", "4+5+6"), collected);
    }

    @Test
    public void testForEachTripleWithIncrement() throws Exception {
        List<String> collected = new ArrayList<>();

        Seq.of(1, 2, 3, 4, 5, 6).forEachTriple(3, (a, b, c) -> collected.add(a + "+" + b + "+" + c));

        assertEquals(Arrays.asList("1+2+3", "4+5+6"), collected);
    }

    @Test
    public void testMin() throws Exception {
        Optional<Integer> min = Seq.of(3, 1, 4, 1, 5, 9).min(Comparator.naturalOrder());

        assertTrue(min.isPresent());
        assertEquals(1, min.get().intValue());
    }

    @Test
    public void testMinEmpty() throws Exception {
        Optional<Integer> min = Seq.<Integer, Exception> empty().min(Comparator.naturalOrder());

        assertFalse(min.isPresent());
    }

    @Test
    public void testMinBy() throws Exception {
        Optional<String> min = Seq.of("aaa", "b", "cc").minBy(String::length);

        assertTrue(min.isPresent());
        assertEquals("b", min.get());
    }

    @Test
    public void testMax() throws Exception {
        Optional<Integer> max = Seq.of(3, 1, 4, 1, 5, 9).max(Comparator.naturalOrder());

        assertTrue(max.isPresent());
        assertEquals(9, max.get().intValue());
    }

    @Test
    public void testMaxEmpty() throws Exception {
        Optional<Integer> max = Seq.<Integer, Exception> empty().max(Comparator.naturalOrder());

        assertFalse(max.isPresent());
    }

    @Test
    public void testMaxBy() throws Exception {
        Optional<String> max = Seq.of("aaa", "b", "cc").maxBy(String::length);

        assertTrue(max.isPresent());
        assertEquals("aaa", max.get());
    }

    @Test
    public void testAnyMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5).anyMatch(x -> x > 3));
        assertFalse(Seq.of(1, 2, 3).anyMatch(x -> x > 5));
    }

    @Test
    public void testAllMatch() throws Exception {
        assertTrue(Seq.of(2, 4, 6).allMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(2, 3, 4).allMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().allMatch(x -> false));
    }

    @Test
    public void testNoneMatch() throws Exception {
        assertTrue(Seq.of(1, 3, 5).noneMatch(x -> x % 2 == 0));
        assertFalse(Seq.of(1, 2, 3).noneMatch(x -> x % 2 == 0));
        assertTrue(Seq.<Integer, Exception> empty().noneMatch(x -> true));
    }

    @Test
    public void testNMatch() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5).nMatch(2, 3, x -> x > 2));
        assertFalse(Seq.of(1, 2, 3).nMatch(2, 3, x -> x > 2));
        assertTrue(Seq.of(1, 2, 3, 4, 5).nMatch(0, 2, x -> x > 10));
    }

    @Test
    public void testFindFirst() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3, 4, 5).findFirst(x -> x > 3);

        assertTrue(result.isPresent());
        assertEquals(4, result.get().intValue());
    }

    @Test
    public void testFindAny() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3, 4, 5).findAny(x -> x > 3);

        assertTrue(result.isPresent());
        assertEquals(4, result.get().intValue());
    }

    @Test
    public void testFindLast() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3, 4, 5, 3, 2, 1).findLast(x -> x > 3);

        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());
    }

    @Test
    public void testContainsAll() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5).containsAll(1, 3, 5));
        assertFalse(Seq.of(1, 2, 3).containsAll(1, 3, 5));
        assertTrue(Seq.of(1, 2, 3).containsAll());
    }

    @Test
    public void testContainsAllCollection() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 4, 5).containsAll(Arrays.asList(1, 3, 5)));
        assertFalse(Seq.of(1, 2, 3).containsAll(Arrays.asList(1, 3, 5)));
    }

    @Test
    public void testContainsAny() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(3, 4, 5));
        assertFalse(Seq.of(1, 2, 3).containsAny(4, 5, 6));
        assertFalse(Seq.of(1, 2, 3).containsAny());
    }

    @Test
    public void testContainsAnyCollection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsAny(Arrays.asList(3, 4, 5)));
        assertFalse(Seq.of(1, 2, 3).containsAny(Arrays.asList(4, 5, 6)));
    }

    @Test
    public void testContainsNone() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(4, 5, 6));
        assertFalse(Seq.of(1, 2, 3).containsNone(3, 4, 5));
        assertTrue(Seq.of(1, 2, 3).containsNone());
    }

    @Test
    public void testContainsNoneCollection() throws Exception {
        assertTrue(Seq.of(1, 2, 3).containsNone(Arrays.asList(4, 5, 6)));
        assertFalse(Seq.of(1, 2, 3).containsNone(Arrays.asList(3, 4, 5)));
    }

    @Test
    public void testHasDuplicates() throws Exception {
        assertTrue(Seq.of(1, 2, 3, 2, 4).hasDuplicates());
        assertFalse(Seq.of(1, 2, 3, 4, 5).hasDuplicates());
        assertFalse(Seq.<Integer, Exception> empty().hasDuplicates());
    }

    @Test
    public void testKthLargest() throws Exception {
        Optional<Integer> result = Seq.of(3, 1, 4, 1, 5, 9, 2, 6).kthLargest(3, Comparator.naturalOrder());

        assertTrue(result.isPresent());
        assertEquals(5, result.get().intValue());
    }

    @Test
    public void testKthLargestInsufficientElements() throws Exception {
        Optional<Integer> result = Seq.of(1, 2, 3).kthLargest(5, Comparator.naturalOrder());

        assertFalse(result.isPresent());
    }

    @Test
    public void testPercentiles() throws Exception {
        Optional<Map<Percentage, Integer>> result = Seq.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).percentiles();

        assertTrue(result.isPresent());
        Map<Percentage, Integer> percentiles = result.get();
        assertNotNull(percentiles.get(Percentage._50));
        assertNotNull(percentiles.get(Percentage._95));
    }

    @Test
    public void testPercentilesWithComparator() throws Exception {
        Optional<Map<Percentage, String>> result = Seq.of("a", "bb", "ccc", "dddd").percentiles(Comparator.comparingInt(String::length));

        assertTrue(result.isPresent());
    }

    @Test
    public void testFirst() throws Exception {
        Optional<Integer> first = Seq.of(1, 2, 3).first();
        assertTrue(first.isPresent());
        assertEquals(1, first.get().intValue());

        assertFalse(Seq.<Integer, Exception> empty().first().isPresent());
    }

    @Test
    public void testLast() throws Exception {
        Optional<Integer> last = Seq.of(1, 2, 3).last();
        assertTrue(last.isPresent());
        assertEquals(3, last.get().intValue());

        assertFalse(Seq.<Integer, Exception> empty().last().isPresent());
    }

    @Test
    public void testElementAt() throws Exception {
        Optional<Integer> element = Seq.of(1, 2, 3, 4, 5).elementAt(2);
        assertTrue(element.isPresent());
        assertEquals(3, element.get().intValue());

        assertFalse(Seq.of(1, 2).elementAt(5).isPresent());
    }

    @Test
    public void testOnlyOne() throws Exception {
        Optional<Integer> only = Seq.of(42).onlyOne();
        assertTrue(only.isPresent());
        assertEquals(42, only.get().intValue());

        assertFalse(Seq.<Integer, Exception> empty().onlyOne().isPresent());
    }

    @Test
    public void testOnlyOneTooMany() throws Exception {
        assertThrows(TooManyElementsException.class, () -> Seq.of(1, 2, 3).onlyOne());
    }

    @Test
    public void testCount() throws Exception {
        assertEquals(5, Seq.of(1, 2, 3, 4, 5).count());
        assertEquals(0, Seq.empty().count());
    }

    @Test
    public void testToArray() throws Exception {
        Object[] array = Seq.of(1, 2, 3).toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToArrayWithGenerator() throws Exception {
        Integer[] array = Seq.of(1, 2, 3).toArray(Integer[]::new);
        assertArrayEquals(new Integer[] { 1, 2, 3 }, array);
    }

    @Test
    public void testToList() throws Exception {
        List<Integer> list = Seq.of(1, 2, 3).toList();
        assertEquals(Arrays.asList(1, 2, 3), list);
    }

    private Seq<String, RuntimeException> testSeq;
    private List<String> testData;

    @BeforeEach
    public void setUp() {
        testData = Arrays.asList("apple", "banana", "cherry", "date", "elderberry");
        testSeq = Seq.of(testData);
    }

    @AfterEach
    public void tearDown() {
        if (testSeq != null) {
            testSeq.close();
        }
    }

    // Tests for saveEach methods

    @Test
    public void testSaveEachToFile2() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");

        try {
            Seq<String, RuntimeException> seq = Seq.of("line1", "line2", "line3");
            Seq<String, RuntimeException> result = seq.saveEach(tempFile.toFile());

            // Verify the sequence is returned for chaining
            assertNotNull(result);

            // Consume the sequence to trigger the save operation
            List<String> consumed = result.toList();
            assertEquals(Arrays.asList("line1", "line2", "line3"), consumed);

            // Verify file content
            List<String> fileContent = Files.readAllLines(tempFile);
            assertEquals(Arrays.asList("line1", "line2", "line3"), fileContent);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSaveEachToFileWithMapper2() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");

        try {
            Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
            Seq<Integer, RuntimeException> result = seq.saveEach(i -> "Number: " + i, tempFile.toFile());

            assertNotNull(result);
            List<Integer> consumed = result.toList();
            assertEquals(Arrays.asList(1, 2, 3), consumed);

            List<String> fileContent = Files.readAllLines(tempFile);
            assertEquals(Arrays.asList("Number: 1", "Number: 2", "Number: 3"), fileContent);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSaveEachToOutputStream2() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Seq<String, RuntimeException> seq = Seq.of("test1", "test2");
        Seq<String, RuntimeException> result = seq.saveEach(s -> s.toUpperCase(), baos);

        assertNotNull(result);
        List<String> consumed = result.toList();
        assertEquals(Arrays.asList("test1", "test2"), consumed);

        String output = baos.toString();
        assertTrue(output.contains("TEST1"));
        assertTrue(output.contains("TEST2"));
    }

    @Test
    public void testSaveEachToWriter2() throws IOException {
        StringWriter writer = new StringWriter();

        Seq<String, RuntimeException> seq = Seq.of("hello", "world");
        Seq<String, RuntimeException> result = seq.saveEach(s -> s + "!", writer);

        assertNotNull(result);
        List<String> consumed = result.toList();
        assertEquals(Arrays.asList("hello", "world"), consumed);

        String output = writer.toString();
        assertTrue(output.contains("hello!"));
        assertTrue(output.contains("world!"));
    }

    @Test
    public void testSaveEachWithBiConsumerToFile2() throws IOException {
        Path tempFile = Files.createTempFile("test", ".txt");

        try {
            Seq<String, RuntimeException> seq = Seq.of("data1", "data2");
            Throwables.BiConsumer<String, Writer, IOException> writeFunc = (item, w) -> w.write("[" + item + "]");

            Seq<String, RuntimeException> result = seq.saveEach(writeFunc, tempFile.toFile());

            assertNotNull(result);
            result.toList(); // Consume to trigger save

            String content = Files.readString(tempFile);
            assertTrue(content.contains("[data1]"));
            assertTrue(content.contains("[data2]"));

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSaveEachWithBiConsumerToWriter2() throws IOException {
        StringWriter writer = new StringWriter();

        Seq<Integer, RuntimeException> seq = Seq.of(10, 20, 30);
        Throwables.BiConsumer<Integer, Writer, IOException> writeFunc = (item, w) -> w.write("Value=" + item);

        Seq<Integer, RuntimeException> result = seq.saveEach(writeFunc, writer);

        assertNotNull(result);
        result.toList(); // Consume to trigger save

        String output = writer.toString();
        assertTrue(output.contains("Value=10"));
        assertTrue(output.contains("Value=20"));
        assertTrue(output.contains("Value=30"));
    }

    @Test
    public void testSaveEachToPreparedStatement2() throws SQLException {
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        Seq<String, RuntimeException> seq = Seq.of("value1", "value2");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        Seq<String, RuntimeException> result = seq.saveEach(mockStmt, stmtSetter);

        assertNotNull(result);
        result.toList(); // Consume to trigger save

        verify(mockStmt, times(2)).execute();
        verify(mockStmt).setString(1, "value1");
        verify(mockStmt).setString(1, "value2");
    }

    @Test
    public void testSaveEachToPreparedStatementWithBatch() throws SQLException {
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        Seq<String, RuntimeException> seq = Seq.of("v1", "v2", "v3");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        Seq<String, RuntimeException> result = seq.saveEach(mockStmt, 2, 0, stmtSetter);

        assertNotNull(result);
        result.toList(); // Consume to trigger save

        verify(mockStmt, times(3)).addBatch();
        verify(mockStmt, times(2)).executeBatch(); // 2 items per batch, so 2 batch executions
    }

    @Test
    public void testSaveEachToConnection2() throws SQLException {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        Seq<String, RuntimeException> seq = Seq.of("test");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        Seq<String, RuntimeException> result = seq.saveEach(mockConn, "INSERT INTO test VALUES (?)", stmtSetter);

        assertNotNull(result);
        result.toList(); // Consume to trigger save

        verify(mockConn).prepareStatement("INSERT INTO test VALUES (?)");
        verify(mockStmt).execute();
    }

    @Test
    public void testSaveEachToDataSource2() throws SQLException {
        DataSource mockDs = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        Seq<String, RuntimeException> seq = Seq.of("test");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        Seq<String, RuntimeException> result = seq.saveEach(mockDs, "INSERT INTO test VALUES (?)", stmtSetter);

        assertNotNull(result);
        result.toList(); // Consume to trigger save

        verify(mockDs).getConnection();
        verify(mockConn).prepareStatement("INSERT INTO test VALUES (?)");
        verify(mockStmt).execute();
    }

    // Tests for persist methods

    @Test
    public void testPersistToFile() throws Exception {
        Path tempFile = Files.createTempFile("persist", ".txt");

        try {
            Seq<String, RuntimeException> seq = Seq.of("line1", "line2", "line3");
            long count = seq.persist(tempFile.toFile());

            assertEquals(3, count);

            List<String> content = Files.readAllLines(tempFile);
            assertEquals(Arrays.asList("line1", "line2", "line3"), content);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistToFileWithHeaderAndTail() throws Exception {
        Path tempFile = Files.createTempFile("persist", ".txt");

        try {
            Seq<String, RuntimeException> seq = Seq.of("data1", "data2");
            long count = seq.persist("HEADER", "TAIL", tempFile.toFile());

            assertEquals(2, count);

            List<String> content = Files.readAllLines(tempFile);
            assertEquals(Arrays.asList("HEADER", "data1", "data2", "TAIL"), content);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistWithMapper() throws Exception {
        Path tempFile = Files.createTempFile("persist", ".txt");

        try {
            Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);
            long count = seq.persist(i -> "NUM:" + i, tempFile.toFile());

            assertEquals(3, count);

            List<String> content = Files.readAllLines(tempFile);
            assertEquals(Arrays.asList("NUM:1", "NUM:2", "NUM:3"), content);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistToOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Seq<String, RuntimeException> seq = Seq.of("test1", "test2");
        long count = seq.persist(s -> s.toUpperCase(), baos);

        assertEquals(2, count);

        String output = baos.toString();
        assertTrue(output.contains("TEST1"));
        assertTrue(output.contains("TEST2"));
    }

    @Test
    public void testPersistToWriter() throws Exception {
        StringWriter writer = new StringWriter();

        Seq<String, RuntimeException> seq = Seq.of("hello", "world");
        long count = seq.persist(s -> s + "!", writer);

        assertEquals(2, count);

        String output = writer.toString();
        assertTrue(output.contains("hello!"));
        assertTrue(output.contains("world!"));
    }

    @Test
    public void testPersistWithBiConsumer() throws Exception {
        StringWriter writer = new StringWriter();

        Seq<String, RuntimeException> seq = Seq.of("data1", "data2");
        Throwables.BiConsumer<String, Writer, IOException> writeFunc = (item, w) -> w.write("[" + item + "]");

        long count = seq.persist(writeFunc, writer);

        assertEquals(2, count);

        String output = writer.toString();
        assertTrue(output.contains("[data1]"));
        assertTrue(output.contains("[data2]"));
    }

    @Test
    public void testPersistToPreparedStatement() throws Exception {
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        Seq<String, RuntimeException> seq = Seq.of("value1", "value2");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        long count = seq.persist(mockStmt, 1, 0, stmtSetter);

        assertEquals(2, count);
        verify(mockStmt, times(2)).execute();
    }

    @Test
    public void testPersistToConnection() throws Exception {
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        Seq<String, RuntimeException> seq = Seq.of("test");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        long count = seq.persist(mockConn, "INSERT INTO test VALUES (?)", 1, 0, stmtSetter);

        assertEquals(1, count);
        verify(mockConn).prepareStatement("INSERT INTO test VALUES (?)");
        verify(mockStmt).execute();
    }

    @Test
    public void testPersistToDataSource() throws Exception {
        DataSource mockDs = mock(DataSource.class);
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockStmt = mock(PreparedStatement.class);

        when(mockDs.getConnection()).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockStmt);

        Seq<String, RuntimeException> seq = Seq.of("test");
        Throwables.BiConsumer<String, PreparedStatement, SQLException> stmtSetter = (item, stmt) -> stmt.setString(1, item);

        long count = seq.persist(mockDs, "INSERT INTO test VALUES (?)", 1, 0, stmtSetter);

        assertEquals(1, count);
        verify(mockDs).getConnection();
        verify(mockStmt).execute();
    }

    // Tests for persistToCSV methods

    @Test
    public void testPersistToCSVFile() throws Exception {
        Path tempFile = Files.createTempFile("test", ".csv");

        try {
            Seq<Map<String, Object>, RuntimeException> seq = Seq.of(Map.of("name", "John", "age", 30), Map.of("name", "Jane", "age", 25));

            long count = seq.persistToCSV(tempFile.toFile());

            assertEquals(2, count);

            List<String> content = Files.readAllLines(tempFile);
            assertTrue(content.size() >= 2); // Header + data rows

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistToCSVWithHeaders() throws Exception {
        Path tempFile = Files.createTempFile("test", ".csv");

        try {
            List<String> headers = Arrays.asList("name", "age");
            Seq<Map<String, Object>, RuntimeException> seq = Seq.of(Map.of("name", "John", "age", 30), Map.of("name", "Jane", "age", 25));

            long count = seq.persistToCSV(headers, tempFile.toFile());

            assertEquals(2, count);

            List<String> content = Files.readAllLines(tempFile);
            assertTrue(content.size() >= 3); // Header + data rows
            assertTrue(content.get(0).contains("name"));
            assertTrue(content.get(0).contains("age"));

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistToCSVOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Seq<Map<String, Object>, Exception> seq = Seq.just(Map.of("id", 1, "value", "test"));

        long count = seq.persistToCSV(baos);

        assertEquals(1, count);

        String output = baos.toString();
        assertTrue(output.length() > 0);
    }

    @Test
    public void testPersistToCSVWriter() throws Exception {
        StringWriter writer = new StringWriter();

        List<String> headers = Arrays.asList("col1", "col2");
        Seq<List<String>, RuntimeException> seq = Seq.of(Arrays.asList("val1", "val2"), Arrays.asList("val3", "val4"));

        long count = seq.persistToCSV(headers, writer);

        assertEquals(2, count);

        String output = writer.toString();
        assertTrue(output.contains("col1"));
        assertTrue(output.contains("val1"));
    }

    // Tests for persistToJSON methods

    @Test
    public void testPersistToJSONFile() throws Exception {
        Path tempFile = Files.createTempFile("test", ".json");

        try {
            Seq<Map<String, Object>, RuntimeException> seq = Seq.of(Map.of("name", "John", "age", 30), Map.of("name", "Jane", "age", 25));

            long count = seq.persistToJSON(tempFile.toFile());

            assertEquals(2, count);

            String content = Files.readString(tempFile);
            assertTrue(content.startsWith("["));
            assertTrue(content.endsWith("]"));

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testPersistToJSONOutputStream() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Seq<String, RuntimeException> seq = Seq.of("test1", "test2");

        long count = seq.persistToJSON(baos);

        assertEquals(2, count);

        String output = baos.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
    }

    @Test
    public void testPersistToJSONWriter() throws Exception {
        StringWriter writer = new StringWriter();

        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);

        long count = seq.persistToJSON(writer);

        assertEquals(3, count);

        String output = writer.toString();
        assertTrue(output.contains("["));
        assertTrue(output.contains("]"));
        assertTrue(output.contains("1"));
        assertTrue(output.contains("2"));
        assertTrue(output.contains("3"));
    }

    // Tests for println method

    @Test
    public void testPrintln() {
        // Capture System.out
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
            seq.println();

            String output = baos.toString();
            assertTrue(output.contains("[a, b, c]"));

        } finally {
            System.setOut(originalOut);
        }
    }

    // Tests for cast method

    @Test
    public void testCast() throws NoSuchElementException, IllegalStateException, Exception {
        Seq<String, RuntimeException> seq = Seq.of("test");
        Seq<String, Exception> casted = seq.cast();

        assertNotNull(casted);
        assertEquals("test", casted.first().get());
    }

    // Tests for stream method

    @Test
    public void testStream() {
        Seq<String, RuntimeException> seq = Seq.of("a", "b", "c");
        com.landawn.abacus.util.stream.Stream<String> stream = seq.stream();

        assertNotNull(stream);
        List<String> result = stream.toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testTransformB() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);

        Seq<String, RuntimeException> transformed = seq.transformB(stream -> stream.map(i -> "val:" + i));

        assertNotNull(transformed);
        List<String> result = transformed.toList();
        assertEquals(Arrays.asList("val:1", "val:2", "val:3"), result);
    }

    @Test
    public void testTransformBWithDeferred() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3);

        Seq<String, RuntimeException> transformed = seq.transformB(stream -> stream.map(i -> "deferred:" + i), true);

        assertNotNull(transformed);
        List<String> result = transformed.toList();
        assertEquals(Arrays.asList("deferred:1", "deferred:2", "deferred:3"), result);
    }

    // Tests for sps methods

    @Test
    public void testSps() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4);

        Seq<Integer, RuntimeException> result = seq.sps(stream -> stream.filter(i -> i % 2 == 0));

        assertNotNull(result);
        List<Integer> filtered = result.toList();
        assertTrue(Arrays.asList(2, 4).containsAll(filtered));
    }

    @Test
    public void testSpsWithMaxThreadNum() {
        Seq<Integer, RuntimeException> seq = Seq.of(1, 2, 3, 4, 5);

        Seq<Integer, RuntimeException> result = seq.sps(2, stream -> stream.map(i -> i * 2));

        assertNotNull(result);
        List<Integer> doubled = result.toList();
        assertTrue(Arrays.asList(2, 4, 6, 8, 10).containsAll(doubled));
    }

    // Tests for async methods

    @Test
    public void testAsyncRun() throws Exception {
        Seq<String, RuntimeException> seq = Seq.of("test");

        ContinuableFuture<Void> future = seq.asyncRun(s -> {
            // Consume the sequence
            s.toList();
        });

        assertNotNull(future);
        future.get(1, TimeUnit.SECONDS); // Wait for completion
    }

    @Test
    public void testAsyncRunWithExecutor() throws Exception {
        Executor executor = Executors.newSingleThreadExecutor();
        Seq<String, RuntimeException> seq = Seq.of("test");

        ContinuableFuture<Void> future = seq.asyncRun(s -> {
            s.toList();
        }, executor);

        assertNotNull(future);
        future.get(1, TimeUnit.SECONDS);
    }

    @Test
    public void testAsyncCallWithExecutor() throws Exception {
        Executor executor = Executors.newSingleThreadExecutor();
        Seq<String, RuntimeException> seq = Seq.of("a", "b");

        ContinuableFuture<String> future = seq.asyncCall(s -> s.join(","), executor);

        assertNotNull(future);
        String result = future.get(1, TimeUnit.SECONDS);
        assertEquals("a,b", result);
    }

    // Tests for applyIfNotEmpty method

    @Test
    public void testApplyIfNotEmptyWithElements() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        u.Optional<Integer> result = seq.applyIfNotEmpty(s -> (int) s.count());

        assertTrue(result.isPresent());
        assertEquals(1, result.get().intValue());
    }

    @Test
    public void testApplyIfNotEmptyWithEmptySeq() {
        Seq<String, RuntimeException> seq = Seq.empty();

        u.Optional<Integer> result = seq.applyIfNotEmpty(s -> (int) s.count());

        assertFalse(result.isPresent());
    }

    // Tests for acceptIfNotEmpty method

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
    public void testAcceptIfNotEmptyWithEmptySeq() {
        Seq<String, RuntimeException> seq = Seq.empty();
        boolean[] actionCalled = { false };

        OrElse result = seq.acceptIfNotEmpty(s -> {
            actionCalled[0] = true;
            s.toList();
        });

        assertEquals(OrElse.FALSE, result);
        assertFalse(actionCalled[0]);
    }

    // Tests for onClose method

    @Test
    public void testOnClose() {
        boolean[] handlerCalled = { false };

        Seq<String, Exception> seq = Seq.of("test").onClose(() -> handlerCalled[0] = true);

        assertNotNull(seq);
        seq.close();

        assertTrue(handlerCalled[0]);
    }

    @Test
    public void testOnCloseMultipleHandlers() {
        boolean[] handler1Called = { false };
        boolean[] handler2Called = { false };

        Seq<String, Exception> seq = Seq.of("test").onClose(() -> handler1Called[0] = true).onClose(() -> handler2Called[0] = true);

        assertNotNull(seq);
        seq.close();

        assertTrue(handler1Called[0]);
        assertTrue(handler2Called[0]);
    }

    @Test
    public void testSaveEachWithNullFile() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.saveEach((File) null).count());
    }

    @Test
    public void testPersistWithNullFile() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.persist((File) null));
    }

    @Test
    public void testTransformWithNullFunction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.transform(null));
    }

    @Test
    public void testOnCloseWithNullHandler() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.onClose(null));
    }

    @Test
    public void testOperationOnClosedStream() {
        Seq<String, RuntimeException> seq = Seq.of("test");
        seq.close();

        assertThrows(IllegalStateException.class, () -> seq.toList());
    }

    @Test
    public void testAsyncRunWithNullAction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.asyncRun(null));
    }

    @Test
    public void testAsyncCallWithNullAction() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.asyncCall(null));
    }

    @Test
    public void testSpsWithInvalidThreadNum() {
        Seq<String, RuntimeException> seq = Seq.of("test");

        assertThrows(IllegalArgumentException.class, () -> seq.sps(-1, stream -> stream));
    }

    // Performance and edge case tests

    @Test
    public void testPersistLargeDataset() throws Exception {
        Path tempFile = Files.createTempFile("large", ".txt");

        try {
            // Create a large sequence
            Seq<Integer, RuntimeException> seq = Seq.range(0, 10000);

            long count = seq.persist(Object::toString, tempFile.toFile());

            assertEquals(10000, count);

            long fileLineCount = Files.lines(tempFile).count();
            assertEquals(10000, fileLineCount);

        } finally {
            Files.deleteIfExists(tempFile);
        }
    }

    @Test
    public void testSaveEachWithFlushingBehavior() throws Exception {
        Path tempFile = Files.createTempFile("flush", ".txt");

        try {
            // Create sequence larger than flush batch size
            List<String> largeData = new ArrayList<>();
            for (int i = 0; i < 500; i++) {
                largeData.add("line" + i);
            }

            Seq<String, RuntimeException> seq = Seq.of(largeData);
            Seq<String, RuntimeException> result = seq.saveEach(tempFile.toFile());

            List<String> consumed = result.toList();
            assertEquals(500, consumed.size());

            List<String> fileContent = Files.readAllLines(tempFile);
            assertEquals(500, fileContent.size());

        } finally {
            Files.deleteIfExists(tempFile);
        }
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

        // Close from multiple threads
        Thread t1 = new Thread(seq::close);
        Thread t2 = new Thread(seq::close);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        assertTrue(handler1Called[0]);
        assertTrue(handler2Called[0]);
    }
}
