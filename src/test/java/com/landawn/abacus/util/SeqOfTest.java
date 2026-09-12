package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Optional;

public class SeqOfTest extends SeqTestSupport {

    @Test
    public void testOfNullable() throws Exception {
        assertEquals(Collections.singletonList("hello"), Seq.ofNullable("hello").toList());
        assertTrue(Seq.ofNullable(null).toList().isEmpty());
        assertEquals(0, Seq.ofNullable(null).count());
    }

    @Test
    public void testOfNullable_ExceptionType() throws Exception {
        assertEquals(Collections.singletonList("hello"), Seq.ofNullable("hello", IOException.class).toList());
        assertTrue(Seq.ofNullable(null, IOException.class).toList().isEmpty());
        assertEquals(Collections.singletonList("world"), Seq.ofNullable("world", Exception.class).toList());
    }

    @Test
    public void testOf() throws Exception {
        assertEquals(Arrays.asList(1, 2, 3), Seq.of(1, 2, 3).toList());
        assertTrue(Seq.of().toList().isEmpty());
        assertEquals(Collections.singletonList(null), Seq.of((Integer) null).toList());
        assertTrue(Seq.of((Integer[]) null).toList().isEmpty());
        assertEquals(0, Seq.of(new String[] {}).count());
        assertEquals(3, Seq.of(new String[] { "a", "b", "c" }).count());
    }

    @Test
    public void testOf_booleanArray() throws Exception {
        assertEquals(Arrays.asList(true, false, true), Seq.of(new boolean[] { true, false, true }).toList());
        assertTrue(Seq.of(new boolean[] {}).toList().isEmpty());
        assertTrue(Seq.of((boolean[]) null).toList().isEmpty());
        assertEquals(Arrays.asList(false, true, false), Seq.of(new boolean[] { true, false, true, false }).skip(1).toList());
        assertTrue(Seq.of(new boolean[] { true, false }).skip(5).toList().isEmpty());
        assertEquals(3, Seq.of(new boolean[] { true, false, true }).skip(0).toList().size());
    }

    @Test
    public void testOf_charArray() throws Exception {
        assertEquals(Arrays.asList('a', 'b', 'c'), Seq.of(new char[] { 'a', 'b', 'c' }).toList());
        assertTrue(Seq.of(new char[] {}).toList().isEmpty());
    }

    @Test
    public void testOf_byteArray() throws Exception {
        assertEquals(Arrays.asList((byte) 1, (byte) 2, (byte) 3), Seq.of(new byte[] { 1, 2, 3 }).toList());
        assertTrue(Seq.of(new byte[] {}).toList().isEmpty());
        assertEquals(Arrays.asList((byte) 30, (byte) 40), Seq.of(new byte[] { 10, 20, 30, 40 }).skip(2).toList());
        assertTrue(Seq.of(new byte[] { 1, 2, 3 }).skip(10).toList().isEmpty());
    }

    @Test
    public void testOf_shortArray() throws Exception {
        assertEquals(Arrays.asList((short) 10, (short) 20, (short) 30), Seq.of(new short[] { 10, 20, 30 }).toList());
        assertTrue(Seq.of(new short[] {}).toList().isEmpty());
        assertEquals(Arrays.asList((short) 4, (short) 5), Seq.of(new short[] { 1, 2, 3, 4, 5 }).skip(3).toList());
    }

    @Test
    public void testOf_intArray() throws Exception {
        assertEquals(Arrays.asList(100, 200, 300), Seq.of(new int[] { 100, 200, 300 }).toList());
        assertTrue(Seq.of(new int[] {}).toList().isEmpty());
        assertTrue(Seq.of(new int[] { 1, 2, 3 }).skip(100).toList().isEmpty());
    }

    @Test
    public void testOf_longArray() throws Exception {
        assertEquals(Arrays.asList(1L, 2L, 3L), Seq.of(new long[] { 1L, 2L, 3L }).toList());
        assertTrue(Seq.of(new long[] {}).toList().isEmpty());
    }

    @Test
    public void testOf_floatArray() throws Exception {
        assertEquals(Arrays.asList(1.0f, 2.5f, 3.0f), Seq.of(new float[] { 1.0f, 2.5f, 3.0f }).toList());
        assertTrue(Seq.of(new float[] {}).toList().isEmpty());
    }

    @Test
    public void testOf_doubleArray() throws Exception {
        assertEquals(Arrays.asList(1.1, 2.2, 3.3), Seq.of(new double[] { 1.1, 2.2, 3.3 }).toList());
        assertTrue(Seq.of(new double[] {}).toList().isEmpty());
    }

    @Test
    public void testOf_PrimitiveArray_throwsNoSuchElementWhenExhausted() {
        final Iterator<Integer> iter = Seq.<Exception> of(new int[] { 1 }).stream().iterator();
        assertEquals(Integer.valueOf(1), iter.next());
        assertThrows(NoSuchElementException.class, iter::next);

        final Iterator<Double> iter2 = Seq.<Exception> of(new double[] { 1.5d }).stream().iterator();
        assertEquals(Double.valueOf(1.5d), iter2.next());
        assertThrows(NoSuchElementException.class, iter2::next);
    }

    @Test
    public void testOf_Optional() throws Exception {
        assertEquals(Collections.singletonList("hello"), Seq.of(Optional.of("hello")).toList());
        assertTrue(Seq.of(Optional.<String> empty()).toList().isEmpty());
        assertTrue(Seq.of((Optional<String>) null).toList().isEmpty());
    }

    @Test
    public void testOf_JavaOptional() throws Exception {
        assertEquals(Collections.singletonList("hello"), Seq.of(java.util.Optional.of("hello")).toList());
        assertTrue(Seq.of(java.util.Optional.<String> empty()).toList().isEmpty());
        assertTrue(Seq.of((java.util.Optional<String>) null).toList().isEmpty());
    }

    @Test
    public void testOf_Iterable() throws Exception {
        List<String> list = Arrays.asList("a", "b", "c");
        assertEquals(list, Seq.of(list).toList());
        assertTrue(Seq.of(Collections.emptyList()).toList().isEmpty());
        assertTrue(Seq.of((Iterable<String>) null).toList().isEmpty());
        assertEquals(list, Seq.of(list, IOException.class).toList());
        assertEquals(3, Seq.<String, RuntimeException> of("a", "b", "c").skip(0).toList().size());
        assertTrue(Seq.<String, RuntimeException> of("a", "b").skip(10).toList().isEmpty());
    }

    @Test
    public void testOf_Iterator() throws Exception {
        assertEquals(Arrays.asList("x", "y"), Seq.of(Arrays.asList("x", "y").iterator()).toList());
        assertTrue(Seq.of(Collections.emptyIterator()).toList().isEmpty());
        assertTrue(Seq.of((Iterator<String>) null).toList().isEmpty());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.of(Arrays.asList("a", "b", "c").iterator(), IOException.class).toList());
    }

    @Test
    public void testOf_ThrowablesIterator() throws Exception {
        assertEquals(Arrays.asList("x", "y"), Seq.of(Throwables.Iterator.of(Arrays.asList("x", "y").iterator())).toList());
        assertTrue(Seq.of(Throwables.Iterator.<String, Exception> empty()).toList().isEmpty());
        assertTrue(Seq.of((Throwables.Iterator<String, Exception>) null).toList().isEmpty());

        AtomicBoolean iteratorClosed = new AtomicBoolean(false);
        Throwables.Iterator<String, Exception> underlying = new Throwables.Iterator<>() {
            private final Iterator<String> source = Arrays.asList("a", "b").iterator();

            @Override
            public boolean hasNext() {
                return source.hasNext();
            }

            @Override
            public String next() {
                return source.next();
            }

            public void closeResourceInternal() {
                iteratorClosed.set(true);
            }
        };
        assertEquals(Arrays.asList("a", "b"), Seq.of(underlying).toList());
        assertTrue(iteratorClosed.get());
    }

    @Test
    public void testOf_Enumeration() throws Exception {
        Vector<String> vector = new Vector<>(Arrays.asList("e1", "e2"));
        assertEquals(Arrays.asList("e1", "e2"), Seq.of(vector.elements()).toList());
        assertTrue(Seq.of(Collections.emptyEnumeration()).toList().isEmpty());
        assertTrue(Seq.of((Enumeration<String>) null).toList().isEmpty());
        assertEquals(Arrays.asList("a", "b", "c"), Seq.of(new Vector<>(Arrays.asList("a", "b", "c")).elements(), IOException.class).toList());
    }

    @Test
    public void testOf_Map() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "a");
        map.put(2, "b");
        List<Map.Entry<Integer, String>> result = Seq.of(map).toList();
        assertEquals(2, result.size());
        assertEquals(new AbstractMap.SimpleEntry<>(1, "a"), result.get(0));
        assertEquals(new AbstractMap.SimpleEntry<>(2, "b"), result.get(1));
        assertTrue(Seq.of(Collections.emptyMap()).toList().isEmpty());
        assertTrue(Seq.of((Map<Integer, String>) null).toList().isEmpty());
        assertEquals(2, Seq.of(map, IOException.class).toList().size());
    }

    @Test
    public void testOfKeys() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "apple");
        map.put(2, "banana");
        map.put(3, "apricot");
        assertEquals(Arrays.asList(1, 2, 3), Seq.ofKeys(map).toList());
        assertTrue(Seq.ofKeys(Collections.emptyMap()).toList().isEmpty());
        assertEquals(Arrays.asList(1, 3), Seq.ofKeys(map, (Throwables.Predicate<String, Exception>) value -> value.startsWith("a")).toList());
        assertEquals(Arrays.asList(2, 3),
                Seq.ofKeys(map, (Throwables.BiPredicate<Integer, String, Exception>) (key, value) -> key > 1 && value.length() > 5).toList());
    }

    @Test
    public void testOfValues() throws Exception {
        Map<Integer, String> map = new LinkedHashMap<>();
        map.put(1, "apple");
        map.put(2, "banana");
        map.put(3, "apricot");
        assertEquals(Arrays.asList("apple", "banana", "apricot"), Seq.ofValues(map).toList());
        assertTrue(Seq.ofValues(Collections.emptyMap()).toList().isEmpty());
        assertEquals(Arrays.asList("apple", "apricot"), Seq.ofValues(map, (Throwables.Predicate<Integer, Exception>) key -> key % 2 != 0).toList());
        assertEquals(Arrays.asList("banana", "apricot"),
                Seq.ofValues(map, (Throwables.BiPredicate<Integer, String, Exception>) (key, value) -> value.length() > 5 && key > 1).toList());
    }

    @Test
    public void testOfReversed_Array() throws Exception {
        assertEquals(Arrays.asList(3, 2, 1), Seq.ofReversed(new Integer[] { 1, 2, 3 }).toList());
        assertTrue(Seq.ofReversed(new Integer[0]).toList().isEmpty());
        assertEquals(Arrays.asList("c", "b", "a"), Seq.ofReversed(new String[] { "a", "b", "c" }).toList());
    }

    @Test
    public void testOfReversed_List() throws Exception {
        assertEquals(Arrays.asList("d", "c", "b", "a"), Seq.ofReversed(Arrays.asList("a", "b", "c", "d")).toList());
        assertTrue(Seq.ofReversed(Collections.emptyList()).toList().isEmpty());
        assertEquals(CommonUtil.asList("c", "b", "a"), Seq.<String, Exception> ofReversed(new ArrayList<>(CommonUtil.asList("a", "b", "c"))).toList());
        assertEquals(CommonUtil.asList("c", "b", "a"), Seq.<String, Exception> ofReversed(new LinkedList<>(CommonUtil.asList("a", "b", "c"))).toList());
        assertEquals(Arrays.asList(null, "b", "a"), Seq.<String, Exception> ofReversed(new LinkedList<>(Arrays.asList("a", "b", null))).toList());
        assertEquals(CommonUtil.emptyList(), Seq.<String, Exception> ofReversed((List<String>) null).toList());
        assertEquals(CommonUtil.asList("b"), Seq.<String, Exception> ofReversed(new LinkedList<>(CommonUtil.asList("a", "b", "c"))).skip(1).limit(1).toList());
    }

    @Test
    public void testOfReversed_anchorsOnTheFactoryTimeSize() throws Exception {
        final LinkedList<String> linked = new LinkedList<>(CommonUtil.asList("a", "b", "c"));
        final Seq<String, Exception> fromLinked = Seq.ofReversed(linked);
        linked.add("d");
        assertEquals(CommonUtil.asList("c", "b", "a"), fromLinked.toList());

        final ArrayList<String> indexed = new ArrayList<>(CommonUtil.asList("a", "b", "c"));
        final Seq<String, Exception> fromIndexed = Seq.ofReversed(indexed);
        indexed.add("d");
        assertEquals(CommonUtil.asList("c", "b", "a"), fromIndexed.toList());
    }

    @Test
    public void testOfReversed_linkedListIsLinearNotQuadratic() throws Exception {
        final LinkedList<Integer> big = new LinkedList<>();
        for (int i = 0; i < 200_000; i++) {
            big.add(i);
        }
        assertEquals(200_000, Seq.<Integer, Exception> ofReversed(big).count());
        assertEquals(Optional.of(199_999), Seq.<Integer, Exception> ofReversed(big).first());
    }

    @Test
    public void testOfLines_File() throws Exception {
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.ofLines(tempFile).toList());
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.ofLines(tempFile, StandardCharsets.UTF_8).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((File) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((File) null, StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines(tempDir.toFile()).toList());

        File utf = Files.createTempFile(tempFolder, "utf", ".txt").toFile();
        Files.write(utf.toPath(), "テスト".getBytes(StandardCharsets.UTF_8));
        assertEquals(Collections.singletonList("テスト"), Seq.ofLines(utf, StandardCharsets.UTF_8).toList());
    }

    @Test
    public void testOfLines_Path() throws Exception {
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.ofLines(tempPath).toList());
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.ofLines(tempPath, StandardCharsets.UTF_8).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Path) null));
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Path) null, StandardCharsets.UTF_8));
    }

    @Test
    public void testOfLines_missingSource() {
        final Seq<String, IOException> missingFile = Seq.ofLines(new File("no_such_seq_file.txt"));
        final IOException fileEx = assertThrows(IOException.class, missingFile::toList);
        assertTrue(fileEx instanceof java.io.FileNotFoundException);

        final Seq<String, IOException> missingPath = Seq.ofLines(Paths.get("no_such_seq_path.txt"));
        final IOException pathEx = assertThrows(IOException.class, missingPath::toList);
        assertTrue(pathEx instanceof java.io.FileNotFoundException);

        assertDoesNotThrow(() -> Seq.ofLines(new File("no_such_seq_file.txt")).close());
        for (int i = 0; i < 2; i++) {
            assertThrows(java.io.FileNotFoundException.class, () -> Seq.ofLines(new File("no_such_retry_file.txt")).toList());
        }
    }

    @Test
    public void testOfLines_Reader() throws Exception {
        assertEquals(Arrays.asList("line1", "line2", "line3"), Seq.ofLines(new StringReader("line1\nline2\nline3")).toList());
        assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Reader) null));

        StringReader open = new StringReader("readerLine1\nreaderLine2");
        assertEquals(Arrays.asList("readerLine1", "readerLine2"), Seq.ofLines(open).toList());
        assertTrue(open.ready());

        final MutableBoolean closed = MutableBoolean.of(false);
        final Reader r1 = new StringReader("a\nb") {
            @Override
            public void close() {
                closed.setTrue();
                super.close();
            }
        };
        assertEquals(CommonUtil.asList("a", "b"), Seq.ofLines(r1, true).toList());
        assertTrue(closed.value());

        final MutableBoolean leftOpen = MutableBoolean.of(false);
        final Reader r2 = new StringReader("a\nb") {
            @Override
            public void close() {
                leftOpen.setTrue();
                super.close();
            }
        };
        assertEquals(CommonUtil.asList("a", "b"), Seq.ofLines(r2, false).toList());
        assertFalse(leftOpen.value());

        Reader spyReader = spy(new StringReader("line1\nline2"));
        assertEquals(Arrays.asList("line1", "line2"), Seq.ofLines(spyReader, true).toList());
        verify(spyReader).close();

        final IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> Seq.ofLines((Reader) null, true));
        assertTrue(ex.getMessage().contains("reader"), ex.getMessage());
    }
}
