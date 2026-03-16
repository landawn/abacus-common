package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.base.Strings;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.RateLimiter;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.function.CharTriPredicate;

@Tag("2025")
public class CharStreamTest extends TestBase {

    private CharStream charStream;
    private CharStream stream;
    private CharStream stream2;
    private CharStream stream3;

    @BeforeEach
    public void setUp() {
        charStream = createCharStream('a', 'b', 'c', 'd', 'e');
        stream = createCharStream("hello");
        stream2 = createCharStream("world");
        stream3 = createCharStream("abc");
    }

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements);
    }

    protected CharStream createCharStream(char[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex);
    }

    protected CharStream createCharStream(Character[] array) {
        return CharStream.of(array);
    }

    protected CharStream createCharStream(Character[] array, int fromIndex, int toIndex) {
        return CharStream.of(array, fromIndex, toIndex);
    }

    protected CharStream createCharStream(Collection<Character> coll) {
        return CharStream.of(coll.toArray(new Character[coll.size()]));
    }

    protected CharStream createCharStream(Collection<Character> coll, int fromIndex, int toIndex) {
        return CharStream.of(coll.toArray(new Character[coll.size()]), fromIndex, toIndex);
    }

    protected CharStream createCharStream(CharIterator iter) {
        return iter == null ? CharStream.empty() : CharStream.of(iter.toArray());
    }

    protected CharStream createCharStream(CharBuffer buff) {
        return buff == null ? CharStream.empty() : CharStream.of(buff);
    }

    protected CharStream createCharStream(File file) {
        return CharStream.of(IOUtil.readAllChars(file));
    }

    protected CharStream createCharStream(Reader reader) {
        return CharStream.of(IOUtil.readAllChars(reader));
    }

    protected CharStream createCharStream(Reader reader, boolean closeReader) {
        return CharStream.of(IOUtil.readAllChars(reader)).onClose(() -> {
            if (closeReader) {
                IOUtil.closeQuietly(reader);
            }
        });
    }

    protected CharStream createCharStream(CharSequence str) {
        return CharStream.of(Strings.nullToEmpty(str == null ? "" : str.toString()).toCharArray());
    }

    protected CharStream createCharStream(CharSequence str, int fromIndex, int toIndex) {
        return CharStream.of(Strings.nullToEmpty(str == null ? "" : str.toString()).toCharArray(), fromIndex, toIndex);
    }

    private Map.Entry<Character, Integer> entry(char key, int value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }

    @Test
    public void testEmpty() {
        CharStream stream = CharStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testDefer() {
        CharStream stream = CharStream.defer(() -> CharStream.of('a', 'b', 'c'));
        assertNotNull(stream);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testDefer_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.defer(null));
    }

    @Test
    public void testOfNullable_Present() {
        CharStream stream = CharStream.ofNullable('a');
        assertArrayEquals(new char[] { 'a' }, stream.toArray());
    }

    @Test
    public void testOfNullable_Null() {
        CharStream stream = CharStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_VarArgs() {
        CharStream stream = CharStream.of('a', 'b', 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_EmptyArray() {
        CharStream stream = CharStream.of(new char[0]);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOf_ArrayWithRange() {
        CharStream stream = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testOf_CharSequence() {
        CharStream stream = CharStream.of("abcde");
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testOf_CharSequenceWithRange() {
        CharStream stream = CharStream.of("abcde", 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testOf_CharacterArray() {
        CharStream stream = CharStream.of(new Character[] { 'a', 'b', 'c' });
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_CharacterArrayWithRange() {
        CharStream stream = CharStream.of(new Character[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testOf_Collection() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        CharStream stream = CharStream.of(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_CharIterator() {
        CharList list = CharList.of('a', 'b', 'c');
        CharStream stream = CharStream.of(list.iterator());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_CharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c' });
        CharStream stream = CharStream.of(buffer);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_Reader() {
        Reader reader = new StringReader("abc");
        CharStream stream = CharStream.of(reader);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_File() throws Exception {
        File tempFile = File.createTempFile("charstream_test", ".txt");
        tempFile.deleteOnExit();

        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            writer.write("test");
        }

        CharStream stream = CharStream.of(tempFile);
        assertNotNull(stream);
        assertArrayEquals(new char[] { 't', 'e', 's', 't' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArray() {
        char[][] arrays = { { 'a', 'b' }, { 'c', 'd' }, { 'e' } };
        CharStream stream = CharStream.flatten(arrays);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        char[][] arrays = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        CharStream stream = CharStream.flatten(arrays, true);
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayHorizontally() {
        char[][] arrays = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        CharStream stream = CharStream.flatten(arrays, false);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayWithAlignment() {
        char[][] arrays = { { 'a', 'b' }, { 'c' } };
        CharStream stream = CharStream.flatten(arrays, '0', true);
        assertArrayEquals(new char[] { 'a', 'c', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testFlatten_3DArray() {
        char[][][] arrays = { { { 'a', 'b' }, { 'c' } }, { { 'd', 'e' } } };
        CharStream stream = CharStream.flatten(arrays);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testRange_TwoParams() {
        CharStream stream = CharStream.range('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testRange_EmptyRange() {
        CharStream stream = CharStream.range('e', 'a');
        assertEquals(0, stream.count());
    }

    @Test
    public void testRange_ThreeParams() {
        CharStream stream = CharStream.range('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, stream.toArray());
    }

    @Test
    public void testRange_NegativeStep() {
        assertArrayEquals(new char[] {}, CharStream.range('a', 'e', -1).toArray());
    }

    @Test
    public void testRangeClosed_TwoParams() {
        CharStream stream = CharStream.rangeClosed('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testRangeClosed_SingleElement() {
        CharStream stream = CharStream.rangeClosed('c', 'c');
        assertArrayEquals(new char[] { 'c' }, stream.toArray());
    }

    @Test
    public void testRangeClosed_ThreeParams() {
        CharStream stream = CharStream.rangeClosed('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e', 'g' }, stream.toArray());
    }

    @Test
    public void testRepeat() {
        CharStream stream = CharStream.repeat('x', 5);
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x', 'x' }, stream.toArray());
    }

    @Test
    public void testRepeat_ZeroTimes() {
        CharStream stream = CharStream.repeat('x', 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeat_NegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('x', -1));
    }

    @Test
    public void testRandom_NoArgs() {
        CharStream stream = CharStream.random();
        assertNotNull(stream);
        assertEquals(10, stream.limit(10).count());
    }

    @Test
    public void testRandom_WithRange() {
        CharStream stream = CharStream.random('a', 'z');
        char[] result = stream.limit(100).toArray();
        for (char c : result) {
            assertTrue(c >= 'a' && c < 'z');
        }
    }

    @Test
    public void testRandom_WithCandidates() {
        char[] candidates = { 'a', 'b', 'c' };
        CharStream stream = CharStream.random(candidates);
        char[] result = stream.limit(100).toArray();
        for (char c : result) {
            assertTrue(c == 'a' || c == 'b' || c == 'c');
        }
    }

    @Test
    public void testIterate_HasNextAndNext() {
        BooleanSupplier hasNext = new BooleanSupplier() {
            int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 3;
            }
        };
        CharSupplier next = new CharSupplier() {
            char ch = 'a';

            @Override
            public char getAsChar() {
                return ch++;
            }
        };
        CharStream stream = CharStream.iterate(hasNext, next);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testIterate_InitHasNextFunction() {
        CharStream stream = CharStream.iterate('a', new BooleanSupplier() {
            int count = 0;

            @Override
            public boolean getAsBoolean() {
                return count++ < 5;
            }
        }, c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testIterate_InitPredicateFunction() {
        CharStream stream = CharStream.iterate('a', c -> c < 'e', c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testIterate_Infinite() {
        CharStream stream = CharStream.iterate('a', c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.limit(5).toArray());
    }

    @Test
    public void testGenerate() {
        CharSupplier supplier = new CharSupplier() {
            char ch = 'a';

            @Override
            public char getAsChar() {
                return ch++;
            }
        };
        CharStream stream = CharStream.generate(supplier);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.limit(5).toArray());
    }

    @Test
    public void testGenerate_NullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.generate(null));
    }

    @Test
    public void testConcat_VarArgsArrays() {
        char[] a1 = { 'a', 'b' };
        char[] a2 = { 'c', 'd' };
        CharStream stream = CharStream.concat(a1, a2);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcat_VarArgsIterators() {
        CharIterator it1 = CharIterator.of('a', 'b');
        CharIterator it2 = CharIterator.of('c', 'd');
        CharStream stream = CharStream.concat(it1, it2);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcat_VarArgsStreams() {
        CharStream s1 = CharStream.of('a', 'b');
        CharStream s2 = CharStream.of('c', 'd');
        CharStream stream = CharStream.concat(s1, s2);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcat_ListOfArrays() {
        List<char[]> list = Arrays.asList(new char[] { 'a', 'b' }, new char[] { 'c', 'd' });
        CharStream stream = CharStream.concat(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcat_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('c', 'd'));
        CharStream stream = CharStream.concat(streams);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcatIterators() {
        Collection<CharIterator> iterators = Arrays.asList(CharIterator.of('a', 'b'), CharIterator.of('c', 'd'));
        CharStream stream = CharStream.concatIterators(iterators);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testZip_TwoArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        char[] c = { '1', '2', '3' };
        CharStream stream = CharStream.zip(a, b, c, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_TwoIterators() {
        CharIterator it1 = CharIterator.of('a', 'b', 'c');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(it1, it2, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeIterators() {
        CharIterator it1 = CharIterator.of('a', 'b', 'c');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharIterator it3 = CharIterator.of('1', '2', '3');
        CharStream stream = CharStream.zip(it1, it2, it3, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_TwoStreams() {
        CharStream s1 = CharStream.of('a', 'b', 'c');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeStreams() {
        CharStream s1 = CharStream.of('a', 'b', 'c');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream s3 = CharStream.of('1', '2', '3');
        CharStream stream = CharStream.zip(s1, s2, s3, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('x', 'y'));
        CharNFunction<Character> zipFunction = chars -> chars[0];
        CharStream stream = CharStream.zip(streams, zipFunction);
        assertArrayEquals(new char[] { 'a', 'b' }, stream.toArray());
    }

    @Test
    public void testZip_TwoArraysWithDefaultValues() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeArraysWithDefaultValues() {
        char[] a = { 'a' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2', '3' };
        CharStream stream = CharStream.zip(a, b, c, '0', '9', '#', (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_TwoIteratorsWithDefaultValues() {
        CharIterator it1 = CharIterator.of('a', 'b');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(it1, it2, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeIteratorsWithDefaultValues() {
        CharIterator it1 = CharIterator.of('a');
        CharIterator it2 = CharIterator.of('x', 'y');
        CharIterator it3 = CharIterator.of('1', '2', '3');
        CharStream stream = CharStream.zip(it1, it2, it3, '0', '9', '#', (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_TwoStreamsWithDefaultValues() {
        CharStream s1 = CharStream.of('a', 'b');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZip_ThreeStreamsWithDefaultValues() {
        CharStream s1 = CharStream.of('a');
        CharStream s2 = CharStream.of('x', 'y');
        CharStream s3 = CharStream.of('1', '2', '3');
        CharStream stream = CharStream.zip(s1, s2, s3, '0', '9', '#', (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
    }

    @Test
    public void testZip_CollectionOfStreamsWithDefaultValues() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a'), CharStream.of('x', 'y'));
        char[] defaultValues = { '0', '9' };
        CharNFunction<Character> zipFunction = chars -> chars[0];
        CharStream stream = CharStream.zip(streams, defaultValues, zipFunction);
        assertArrayEquals(new char[] { 'a', '0' }, stream.toArray());
    }

    @Test
    public void testMerge_TwoArrays() {
        char[] a = { 'a', 'c', 'e' };
        char[] b = { 'b', 'd', 'f' };
        CharStream stream = CharStream.merge(a, b, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_ThreeArrays() {
        char[] a = { 'a', 'd' };
        char[] b = { 'b', 'e' };
        char[] c = { 'c', 'f' };
        CharStream stream = CharStream.merge(a, b, c, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_TwoIterators() {
        CharIterator it1 = CharIterator.of('a', 'c', 'e');
        CharIterator it2 = CharIterator.of('b', 'd', 'f');
        CharStream stream = CharStream.merge(it1, it2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_ThreeIterators() {
        CharIterator it1 = CharIterator.of('a', 'd');
        CharIterator it2 = CharIterator.of('b', 'e');
        CharIterator it3 = CharIterator.of('c', 'f');
        CharStream stream = CharStream.merge(it1, it2, it3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_TwoStreams() {
        CharStream s1 = CharStream.of('a', 'c', 'e');
        CharStream s2 = CharStream.of('b', 'd', 'f');
        CharStream stream = CharStream.merge(s1, s2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_ThreeStreams() {
        CharStream s1 = CharStream.of('a', 'd');
        CharStream s2 = CharStream.of('b', 'e');
        CharStream s3 = CharStream.of('c', 'f');
        CharStream stream = CharStream.merge(s1, s2, s3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMerge_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'd'), CharStream.of('b', 'e'), CharStream.of('c', 'f'));
        CharStream stream = CharStream.merge(streams, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testFilter_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.filter(c -> c > 'b').toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testFilter_NoMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.filter(c -> c > 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testFilter_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> dropped = new ArrayList<>();
        CharConsumer action = dropped::add;
        char[] result = stream.filter(c -> c > 'b', action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    @Test
    public void testTakeWhile_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.takeWhile(c -> c < 'd').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testTakeWhile_NoneMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.takeWhile(c -> c > 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testDropWhile_BasicPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.dropWhile(c -> c < 'c').toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testDropWhile_AllMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.dropWhile(c -> c < 'z').toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testDropWhile_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> dropped = new ArrayList<>();
        CharConsumer action = dropped::add;
        char[] result = stream.dropWhile(c -> c < 'c', action).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
        assertEquals(Arrays.asList('a', 'b'), dropped);
    }

    @Test
    public void testMap() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.map(c -> Character.toUpperCase(c)).toArray();
        assertArrayEquals(new char[] { 'A', 'B', 'C' }, result);
    }

    @Test
    public void testMapToInt() {
        CharStream stream = createCharStream('a', 'b', 'c');
        int[] result = stream.mapToInt(c -> (int) c).toArray();
        assertArrayEquals(new int[] { 97, 98, 99 }, result);
    }

    @Test
    public void testMapToObj() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> result = stream.mapToObj(c -> String.valueOf(c)).toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);
    }

    @Test
    public void testFlatMap() {
        CharStream stream = createCharStream('a', 'b');
        CharStream result = stream.flatMap(c -> CharStream.of(c, Character.toUpperCase(c)));
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, result.toArray());
    }

    @Test
    public void testFlatmap_WithCharArray() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<char[]> mapper = c -> new char[] { c, Character.toUpperCase(c) };
        char[] result = stream.flatmap(mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'A', 'b', 'B' }, result);
    }

    @Test
    public void testFlatMapToInt() {
        CharStream stream = createCharStream('a', 'b');
        IntStream result = stream.flatMapToInt(c -> IntStream.of(c, Character.toUpperCase(c)));
        assertArrayEquals(new int[] { 97, 65, 98, 66 }, result.toArray());
    }

    @Test
    public void testFlatMapToObj() {
        CharStream stream = createCharStream('a', 'b');
        Stream<String> result = stream.flatMapToObj(c -> Stream.of(String.valueOf(c), String.valueOf(Character.toUpperCase(c))));
        assertEquals(Arrays.asList("a", "A", "b", "B"), result.toList());
    }

    @Test
    public void testFlatmapToObj_WithCollection() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<List<String>> mapper = c -> Arrays.asList(String.valueOf(c), String.valueOf(Character.toUpperCase(c)));
        List<String> result = stream.flatmapToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testFlattmapToObj_WithArray() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<String[]> mapper = c -> new String[] { String.valueOf(c), String.valueOf(Character.toUpperCase(c)) };
        List<String> result = stream.flatMapArrayToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
    }

    @Test
    public void testMapPartial() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharFunction<OptionalChar> mapper = c -> c == 'b' ? OptionalChar.empty() : OptionalChar.of(Character.toUpperCase(c));
        char[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new char[] { 'A', 'C' }, result);
    }

    @Test
    public void testMapPartial_AllEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharFunction<OptionalChar> mapper = c -> OptionalChar.empty();
        char[] result = stream.mapPartial(mapper).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testRangeMap() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate sameRange = (a, b) -> a == b;
        CharBinaryOperator mapper = (first, last) -> last;
        char[] result = stream.rangeMap(sameRange, mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRangeMap_AllDifferent() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        CharBiPredicate sameRange = (a, b) -> false;
        CharBinaryOperator mapper = (first, last) -> first;
        char[] result = stream.rangeMap(sameRange, mapper).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testRangeMapToObj() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate sameRange = (a, b) -> a == b;
        CharBiFunction<String> mapper = (first, last) -> String.valueOf(first) + String.valueOf(last);
        List<String> result = stream.rangeMapToObj(sameRange, mapper).toList();
        assertEquals(Arrays.asList("aa", "bb", "cc"), result);
    }

    @Test
    public void testCollapse_BiPredicate() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate collapsible = (a, b) -> a == b;
        List<CharList> result = stream.collapse(collapsible).toList();
        assertEquals(3, result.size());
        assertEquals(CharList.of('a', 'a'), result.get(0));
        assertEquals(CharList.of('b', 'b'), result.get(1));
        assertEquals(CharList.of('c'), result.get(2));
    }

    @Test
    public void testCollapse_WithMergeFunction() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c');
        CharBiPredicate collapsible = (a, b) -> a == b;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testCollapse_WithTriPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        CharTriPredicate collapsible = (first, prev, curr) -> Math.abs(curr - prev) == 1;
        CharBinaryOperator mergeFunction = (a, b) -> b;
        char[] result = stream.collapse(collapsible, mergeFunction).toArray();
        assertEquals(1, result.length);
        assertEquals('e', result[0]);
    }

    @Test
    public void testScan() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(accumulator).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testScan_WithInit() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);
        char[] result = stream.scan(init, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testScan_WithInitIncluded() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char init = 'z';
        CharBinaryOperator accumulator = (a, b) -> (char) Math.max(a, b);

        char[] result = stream.scan(init, true, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z', 'z' }, result);

        stream = createCharStream('a', 'b', 'c');
        result = stream.scan(init, false, accumulator).toArray();
        assertArrayEquals(new char[] { 'z', 'z', 'z' }, result);
    }

    @Test
    public void testPrepend_VarArgs() {
        CharStream stream = createCharStream('c', 'd', 'e');
        char[] result = stream.prepend('a', 'b').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testPrepend_Stream() {
        CharStream stream = createCharStream('c', 'd', 'e');
        CharStream toPrePend = createCharStream('a', 'b');
        char[] result = stream.prepend(toPrePend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testPrepend_Optional() {
        CharStream stream = createCharStream('b', 'c');

        char[] result = stream.prepend(OptionalChar.of('a')).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        stream = createCharStream('b', 'c');
        result = stream.prepend(OptionalChar.empty()).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testAppend_VarArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.append('d', 'e').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testAppend_Stream() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream toAppend = createCharStream('d', 'e');
        char[] result = stream.append(toAppend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testAppend_Optional() {
        CharStream stream = createCharStream('a', 'b');

        char[] result = stream.append(OptionalChar.of('c')).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);

        stream = createCharStream('a', 'b');
        result = stream.append(OptionalChar.empty()).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testAppendIfEmpty_EmptyStream() {
        CharStream stream = createCharStream();
        char[] result = stream.appendIfEmpty('a', 'b').toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testAppendIfEmpty_NonEmptyStream() {
        CharStream stream = createCharStream('x', 'y');
        char[] result = stream.appendIfEmpty('a', 'b').toArray();
        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testToCharList() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharList result = stream.toCharList();
        assertEquals(CharList.of('a', 'b', 'c'), result);
    }

    @Test
    public void testToArray() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testToMap_KeyAndValueMappers() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c));
        assertEquals(3, result.size());
        assertEquals("a", result.get('a'));
        assertEquals("b", result.get('b'));
        assertEquals("c", result.get('c'));
    }

    @Test
    public void testToMap_WithMapFactory() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testToMap_WithMergeFunction() {
        CharStream stream = createCharStream('a', 'b', 'a');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals("aa", result.get('a'));
        assertEquals("b", result.get('b'));
    }

    @Test
    public void testToMap_WithMapFactoryAndMergeFunction() {
        CharStream stream = createCharStream('a', 'b', 'a');
        Map<Character, String> result = stream.toMap(c -> c, c -> String.valueOf(c), (v1, v2) -> v1 + v2, Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(2, result.size());
        assertEquals("aa", result.get('a'));
    }

    @Test
    public void testGroupTo() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b');
        Map<Character, Long> result = stream.groupTo(c -> c, Collectors.counting());
        assertEquals(3, result.size());
        assertEquals(2L, result.get('a'));
        assertEquals(2L, result.get('b'));
        assertEquals(1L, result.get('c'));
    }

    @Test
    public void testGroupTo_WithMapFactory() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b');
        Map<Character, Long> result = stream.groupTo(c -> c, Collectors.counting(), Suppliers.ofLinkedHashMap());
        assertTrue(result instanceof LinkedHashMap);
        assertEquals(3, result.size());
    }

    @Test
    public void testReduce_WithIdentity() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char result = stream.reduce('0', (a, b) -> (char) Math.max(a, b));
        assertEquals('c', result);
    }

    @Test
    public void testReduce_WithoutIdentity() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.reduce((a, b) -> (char) Math.max(a, b));
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testReduce_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.reduce((a, b) -> (char) Math.max(a, b));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollect_ThreeArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        StringBuilder result = stream.collect(StringBuilder::new, (sb, c) -> sb.append(c), (sb1, sb2) -> sb1.append(sb2));
        assertEquals("abc", result.toString());
    }

    @Test
    public void testCollect_TwoArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        StringBuilder result = stream.collect(StringBuilder::new, (sb, c) -> sb.append(c));
        assertEquals("abc", result.toString());
    }

    @Test
    public void testForEach() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> result = new ArrayList<>();
        stream.forEach(result::add);
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testForEachIndexed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> result = new ArrayList<>();
        stream.forEachIndexed((index, value) -> result.add(index + ":" + value));
        assertEquals(Arrays.asList("0:a", "1:b", "2:c"), result);
    }

    @Test
    public void testAnyMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.anyMatch(c -> c == 'b'));
    }

    @Test
    public void testAnyMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.anyMatch(c -> c == 'z'));
    }

    @Test
    public void testAllMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.allMatch(c -> c < 'z'));
    }

    @Test
    public void testAllMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.allMatch(c -> c == 'a'));
    }

    @Test
    public void testNoneMatch_True() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertTrue(stream.noneMatch(c -> c > 'z'));
    }

    @Test
    public void testNoneMatch_False() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.noneMatch(c -> c == 'b'));
    }

    @Test
    public void testFindFirst_NoCondition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findFirst();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testFindFirst_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findFirst(c -> c > 'b');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testFindFirst_NotFound() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findFirst(c -> c > 'z');
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoCondition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findAny(c -> c > 'b');
        assertTrue(result.isPresent());
        assertTrue(result.get() >= 'c');
    }

    @Test
    public void testFindLast_WithCondition() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        OptionalChar result = stream.findLast(c -> c < 'd');
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testMin() {
        CharStream stream = createCharStream('c', 'a', 'e', 'b', 'd');
        OptionalChar result = stream.min();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testMin_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.min();
        assertFalse(result.isPresent());
    }

    @Test
    public void testMax() {
        CharStream stream = createCharStream('c', 'a', 'e', 'b', 'd');
        OptionalChar result = stream.max();
        assertTrue(result.isPresent());
        assertEquals('e', result.get());
    }

    @Test
    public void testMax_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.max();
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        OptionalChar result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());
    }

    @Test
    public void testKthLargest_K1() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        OptionalChar result = stream.kthLargest(1);
        assertTrue(result.isPresent());
        assertEquals('e', result.get());
    }

    @Test
    public void testKthLargest_TooLarge() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.kthLargest(10);
        assertFalse(result.isPresent());
    }

    @Test
    public void testSum() {
        CharStream stream = createCharStream('a', 'b', 'c');
        int result = stream.sum();
        assertEquals(294, result);
    }

    @Test
    public void testSum_EmptyStream() {
        CharStream stream = createCharStream();
        int result = stream.sum();
        assertEquals(0, result);
    }

    @Test
    public void testAverage() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalDouble result = stream.average();
        assertTrue(result.isPresent());
        assertEquals(98.0, result.getAsDouble(), 0.001);
    }

    @Test
    public void testAverage_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalDouble result = stream.average();
        assertFalse(result.isPresent());
    }

    @Test
    public void testsummaryStatistics() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());
        assertEquals(0 + 'a' + 'e' + 'c', +'b' + 'd', stats.getSum());
    }

    @Test
    public void testSummarize_EmptyStream() {
        CharStream stream = createCharStream();
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();

        CharSummaryStatistics stats = result.left();
        assertEquals(5, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('e', stats.getMax());

        Optional<Map<Percentage, Character>> percentiles = result.right();
        assertTrue(percentiles.isPresent());
    }

    @Test
    public void testsummaryStatisticsAndPercentiles_EmptyStream() {
        CharStream stream = createCharStream();
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();
        CharSummaryStatistics stats = result.left();
        assertEquals(0, stats.getCount());
        assertFalse(result.right().isPresent());
    }

    @Test
    public void testMergeWith() {
        CharStream streamA = createCharStream('a', 'c', 'e');
        CharStream streamB = createCharStream('b', 'd', 'f');
        CharBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        char[] result = streamA.mergeWith(streamB, selector).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testZipWith_TwoStreams() {
        CharStream streamA = createCharStream('a', 'b', 'c');
        CharStream streamB = createCharStream('x', 'y', 'z');
        CharBinaryOperator zipFunction = (a, b) -> (char) Math.max(a, b);
        char[] result = streamA.zipWith(streamB, zipFunction).toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    public void testZipWith_ThreeStreams() {
        CharStream streamA = createCharStream('a', 'b', 'c');
        CharStream streamB = createCharStream('x', 'y', 'z');
        CharStream streamC = createCharStream('1', '2', '3');
        CharTernaryOperator zipFunction = (a, b, c) -> c;
        char[] result = streamA.zipWith(streamB, streamC, zipFunction).toArray();
        assertArrayEquals(new char[] { '1', '2', '3' }, result);
    }

    @Test
    public void testZipWith_TwoStreamsWithDefaultValues() {
        CharStream streamA = createCharStream('a', 'b');
        CharStream streamB = createCharStream('x', 'y', 'z');
        char valueForNoneA = '0';
        char valueForNoneB = '9';
        CharBinaryOperator zipFunction = (a, b) -> (char) Math.max(a, b);
        char[] result = streamA.zipWith(streamB, valueForNoneA, valueForNoneB, zipFunction).toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
    }

    @Test
    public void testZipWith_ThreeStreamsWithDefaultValues() {
        CharStream streamA = createCharStream('a');
        CharStream streamB = createCharStream('x', 'y');
        CharStream streamC = createCharStream('1', '2', '3');
        char valueForNoneA = '0';
        char valueForNoneB = '9';
        char valueForNoneC = '#';
        CharTernaryOperator zipFunction = (a, b, c) -> c;
        char[] result = streamA.zipWith(streamB, streamC, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction).toArray();
        assertArrayEquals(new char[] { '1', '2', '3' }, result);
    }

    @Test
    public void testAsIntStream() {
        CharStream stream = createCharStream('a', 'b', 'c');
        IntStream result = stream.asIntStream();
        assertArrayEquals(new int[] { 97, 98, 99 }, result.toArray());
    }

    @Test
    public void testBoxed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> result = stream.boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);
    }

    @Test
    public void testDistinct() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b', 'd');
        char[] result = stream.distinct().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, result);
    }

    @Test
    public void testDistinct_AlreadyDistinct() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.distinct().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testSorted() {
        CharStream stream = createCharStream('e', 'b', 'd', 'a', 'c');
        char[] result = stream.sorted().toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testReverseSorted() {
        CharStream stream = createCharStream('b', 'e', 'a', 'd', 'c');
        char[] result = stream.reverseSorted().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    @Test
    public void testReversed() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.reversed().toArray();
        assertArrayEquals(new char[] { 'e', 'd', 'c', 'b', 'a' }, result);
    }

    @Test
    public void testLimit() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.limit(3).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testLimit_ZeroElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.limit(0).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testSkip() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.skip(2).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
    }

    @Test
    public void testSkip_AllElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.skip(10).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testSkip_WithAction() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        List<Character> skipped = new ArrayList<>();
        CharConsumer action = skipped::add;
        char[] result = stream.skip(2, action).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, result);
        assertEquals(Arrays.asList('a', 'b'), skipped);
    }

    @Test
    public void testSkipUntil() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        CharPredicate predicate = c -> c == 'c';
        char[] result = stream.skipUntil(predicate).toArray();
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testPeek() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> peeked = new ArrayList<>();
        char[] result = stream.peek(peeked::add).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertEquals(Arrays.asList('a', 'b', 'c'), peeked);
    }

    @Test
    public void testDelay() {
        Duration delay = Duration.ofMillis(10);
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        CharStream result = stream.delay(delay);
        assertNotNull(result);
    }

    @Test
    public void testDelay_NullDuration() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        assertThrows(IllegalArgumentException.class, () -> stream.delay((Duration) null));
    }

    @Test
    public void testRateLimited() {
        RateLimiter rateLimiter = RateLimiter.create(10);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.rateLimited(rateLimiter);
        assertNotNull(result);
    }

    @Test
    public void testRateLimited_NullRateLimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> stream.rateLimited(null));
    }

    @Test
    public void testStep() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e', 'f');
        char[] result = stream.step(2).toArray();
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, result);
    }

    @Test
    public void testStep_One() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.step(1).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testStep_InvalidStep() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').step(0);
        });
    }

    @Test
    public void testRotated_Positive() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.rotated(2).toArray();
        assertArrayEquals(new char[] { 'd', 'e', 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRotated_Negative() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.rotated(-2).toArray();
        assertArrayEquals(new char[] { 'c', 'd', 'e', 'a', 'b' }, result);
    }

    @Test
    public void testRotated_Zero() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.rotated(0).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testShuffled() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        Random rnd = new Random(42);
        char[] result = stream.shuffled(rnd).toArray();
        assertNotNull(result);
        assertEquals(5, result.length);
    }

    @Test
    public void testShuffled_NullRandom() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').shuffled(null);
        });
    }

    @Test
    public void testShuffled_NoArg() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.shuffled().toArray();
        assertNotNull(result);
        assertEquals(5, result.length);
    }

    @Test
    public void testCycled_Infinite() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled().limit(10).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c', 'a', 'b', 'c', 'a' }, result);
    }

    @Test
    public void testCycled_WithRounds() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled(2).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'a', 'b', 'c' }, result);
    }

    @Test
    public void testCycled_ZeroRounds() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.cycled(0).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testCycled_NegativeRounds() {
        assertThrows(IllegalArgumentException.class, () -> {
            createCharStream('a').cycled(-1);
        });
    }

    @Test
    public void testIndexed() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<IndexedChar> result = stream.indexed().toList();
        assertEquals(3, result.size());
        assertEquals(IndexedChar.of('a', 0), result.get(0));
        assertEquals(IndexedChar.of('b', 1), result.get(1));
        assertEquals(IndexedChar.of('c', 2), result.get(2));
    }

    @Test
    public void testIntersection() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.intersection(c).toArray();
        assertArrayEquals(new char[] { 'b', 'c' }, result);
    }

    @Test
    public void testIntersection_NoCommonElements() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> c = Arrays.asList('x', 'y', 'z');
        char[] result = stream.intersection(c).toArray();
        assertArrayEquals(new char[] {}, result);
    }

    @Test
    public void testDifference() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.difference(c).toArray();
        assertArrayEquals(new char[] { 'a', 'd' }, result);
    }

    @Test
    public void testSymmetricDifference() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        List<Character> c = Arrays.asList('b', 'c', 'e');
        char[] result = stream.symmetricDifference(c).toArray();
        char[] expected = { 'a', 'd', 'e' };
        Arrays.sort(result);
        Arrays.sort(expected);
        assertArrayEquals(expected, result);
    }

    @Test
    public void testFirst() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.first();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testFirst_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.first();
        assertFalse(result.isPresent());
    }

    @Test
    public void testLast() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.last();
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testLast_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.last();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_SingleElement() {
        CharStream stream = createCharStream('a');
        OptionalChar result = stream.onlyOne();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testOnlyOne_EmptyStream() {
        CharStream stream = createCharStream();
        OptionalChar result = stream.onlyOne();
        assertFalse(result.isPresent());
    }

    @Test
    public void testOnlyOne_MultipleElements() {
        CharStream stream = createCharStream('a', 'b');
        assertThrows(TooManyElementsException.class, () -> stream.onlyOne());
    }

    @Test
    public void testCount() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        assertEquals(5, stream.count());
    }

    @Test
    public void testCount_EmptyStream() {
        CharStream stream = createCharStream();
        assertEquals(0, stream.count());
    }

    @Test
    public void testPercentiles() {
        CharStream stream = createCharStream('a', 'e', 'c', 'b', 'd');
        Optional<Map<Percentage, Character>> result = stream.percentiles();
        assertTrue(result.isPresent());
        Map<Percentage, Character> percentiles = result.get();
        assertNotNull(percentiles);
        assertTrue(percentiles.containsKey(Percentage._0_0001));
        assertTrue(percentiles.containsKey(Percentage._50));
        assertTrue(percentiles.containsKey(Percentage._99_9999));
    }

    @Test
    public void testPercentiles_EmptyStream() {
        CharStream stream = createCharStream();
        Optional<Map<Percentage, Character>> result = stream.percentiles();
        assertFalse(result.isPresent());
    }

    @Test
    public void testJoin_WithDelimiterPrefixSuffix() {
        CharStream stream = createCharStream('a', 'b', 'c');
        String result = stream.join(", ", "[", "]");
        assertEquals("[a, b, c]", result);
    }

    @Test
    public void testJoin_EmptyDelimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        String result = stream.join("", "", "");
        assertEquals("abc", result);
    }

    @Test
    public void testJoinTo() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Joiner joiner = Joiner.with(", ");
        Joiner result = stream.joinTo(joiner);
        assertEquals("a, b, c", result.toString());
    }

    @Test
    public void testIterator() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharIterator iterator = stream.iterator();
        assertTrue(iterator.hasNext());
        assertEquals('a', iterator.nextChar());
        assertEquals('b', iterator.nextChar());
        assertEquals('c', iterator.nextChar());
        assertFalse(iterator.hasNext());
    }

    @Test
    public void testOnClose() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<String> closeActions = new ArrayList<>();
        stream.onClose(() -> closeActions.add("closed"));
        stream.close();
        assertEquals(Arrays.asList("closed"), closeActions);
    }

    @Test
    public void testClose() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.close();
        assertNotNull(stream);
    }

    @Test
    public void testIsParallel() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertFalse(stream.isParallel());
    }

    @Test
    public void testSequential() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.sequential();
        assertNotNull(result);
        assertFalse(result.isParallel());
    }

    @Test
    public void testParallel() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel();
        assertNotNull(result);
    }

    // Additional coverage improvement tests - Collection-based factory methods

    @Test
    public void testZipWithCollectionOfStreamsForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('x', 'y'), CharStream.of('1', '2'));
        CharStream result = CharStream.zip(streams, chars -> chars[0]);
        assertArrayEquals(new char[] { 'a', 'b' }, result.toArray());
    }

    @Test
    public void testZipCollectionWithDefaultsForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y'));
        char[] defaults = { '0', '9' };
        CharStream result = CharStream.zip(streams, defaults, chars -> chars[1]);
        assertEquals(3, result.count());
    }

    @Test
    public void testMergeCollectionForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'e'), CharStream.of('b', 'f'), CharStream.of('c', 'g'));
        CharStream result = CharStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeThreeArraysForCoverage() {
        char[] a1 = { 'a', 'g' };
        char[] a2 = { 'c', 'h' };
        char[] a3 = { 'e', 'i' };
        CharStream stream = CharStream.merge(a1, a2, a3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    public void testMergeThreeStreamsForCoverage() {
        CharStream s1 = CharStream.of('a', 'j');
        CharStream s2 = CharStream.of('e', 'k');
        CharStream s3 = CharStream.of('h', 'l');
        CharStream stream = CharStream.merge(s1, s2, s3, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    // Edge cases

    @Test
    public void testFlattenWithAlignmentHorizontallyForCoverage() {
        char[][] array = { { 'a', 'b', 'c' }, { 'd', 'e' }, { 'f' } };
        CharStream stream = CharStream.flatten(array, '0', true);
        char[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals('a', result[0]);
        assertEquals('d', result[1]);
        assertEquals('f', result[2]);
    }

    @Test
    public void testRangeClosedWithNegativeStepForCoverage() {
        CharStream stream = CharStream.rangeClosed('j', 'a', -2);
        assertArrayEquals(new char[] { 'j', 'h', 'f', 'd', 'b' }, stream.toArray());
    }

    @Test
    public void testRangeWithInvalidStepDirectionForCoverage() {
        CharStream stream = CharStream.range('a', 'j', -1);
        assertEquals(0, stream.count());
    }

    // Parallel operations

    @Test
    public void testParallelStreamOperationsForCoverage() {
        CharStream stream = CharStream.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h').parallel();
        char[] result = stream.filter(c -> c > 'c').map(c -> (char) (c + 1)).toArray();
        assertEquals(5, result.length);
    }

    @Test
    public void testIterateWithImmediateFalseHasNextForCoverage() {
        CharStream stream = CharStream.iterate('a', () -> false, c -> (char) (c + 1));
        assertEquals(0, stream.count());
    }

    @Test
    public void testCollapseWithNoCollapsibleElementsForCoverage() {
        CharStream stream = CharStream.of('a', 'p', 'z', 'k');
        char[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 2, (a, b) -> b).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    public void testScanOnEmptyStreamForCoverage() {
        CharStream emptyStream = CharStream.empty();
        char[] result = emptyStream.scan((a, b) -> (char) Math.max(a, b)).toArray();
        assertEquals(0, result.length);
    }

    @Test
    public void testZipThreeArraysWithDefaultsForCoverage() {
        char[] a1 = { 'a', 'b' };
        char[] a2 = { 'x' };
        char[] a3 = { '1', '2', '3' };
        CharStream stream = CharStream.zip(a1, a2, a3, '0', '9', '#', (a, b, c) -> c);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeIteratorsWithDefaultsForCoverage() {
        CharIterator iter1 = CharIterator.of('a', 'b');
        CharIterator iter2 = CharIterator.of('x');
        CharIterator iter3 = CharIterator.of('1', '2', '3');
        CharStream stream = CharStream.zip(iter1, iter2, iter3, '0', '9', '#', (a, b, c) -> c);
        assertEquals(3, stream.count());
    }

    @Test
    public void testZipThreeStreamsWithDefaultsForCoverage() {
        CharStream s1 = CharStream.of('a', 'b');
        CharStream s2 = CharStream.of('x');
        CharStream s3 = CharStream.of('1', '2', '3');
        CharStream stream = CharStream.zip(s1, s2, s3, '0', '9', '#', (a, b, c) -> c);
        assertEquals(3, stream.count());
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e').debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals('a', result[0]);
        assertEquals('b', result[1]);
        assertEquals('c', result[2]);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e').debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_EmptyStream() {
        char[] result = CharStream.empty().debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        char[] result = CharStream.of('z').debounce(1, com.landawn.abacus.util.Duration.ofMillis(100)).toArray();

        assertEquals(1, result.length);
        assertEquals('z', result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e').debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(1, result.length);
        assertEquals('a', result[0]);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            CharStream.of('a', 'b', 'c').debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CharStream.of('a', 'b', 'c').debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            CharStream.of('a', 'b', 'c').debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            CharStream.of('a', 'b', 'c').debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        char[] input = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

        char[] result = CharStream.of(input).debounce(26, com.landawn.abacus.util.Duration.ofSeconds(10)).toArray();

        assertEquals(26, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        char[] result = CharStream.of('x', 'y', 'z', 'a', 'b').debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals('x', result[0]);
        assertEquals('y', result[1]);
        assertEquals('z', result[2]);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')
                .filter(c -> c >= 'b' && c <= 'f') // b, c, d, e, f
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)) // b, c, d
                .map(c -> Character.toUpperCase(c)) // B, C, D
                .toArray();

        assertEquals(3, result.length);
        assertEquals('B', result[0]);
        assertEquals('C', result[1]);
        assertEquals('D', result[2]);
    }

    @Test
    public void testDebounce_WithSpecialCharacters() {
        char[] result = CharStream.of('\n', '\t', ' ', 'a', 'b').debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(3, result.length);
        assertEquals('\n', result[0]);
        assertEquals('\t', result[1]);
        assertEquals(' ', result[2]);
    }

    @Test
    public void testDebounce_FromString() {
        char[] result = CharStream.of("Hello World!").debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();

        assertEquals(5, result.length);
        assertEquals("Hello", new String(result));
    }

    @Test
    public void testOfCharArray() {
        CharStream stream1 = createCharStream('a', 'b', 'c');
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());

        CharStream stream2 = createCharStream();
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharSequence() {
        CharStream stream1 = createCharStream("hello");
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o'), stream1.boxed().toList());

        CharStream stream2 = createCharStream("");
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharacterArray() {
        Character[] data = { 'X', 'Y', 'Z' };
        CharStream stream1 = createCharStream(data);
        assertEquals(Arrays.asList('X', 'Y', 'Z'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(new Character[] {});
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharacterCollection() {
        List<Character> list = Arrays.asList('1', '2', '3');
        CharStream stream1 = createCharStream(list);
        assertEquals(Arrays.asList('1', '2', '3'), stream1.boxed().toList());

        CharStream stream2 = createCharStream(Arrays.asList());
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 't', 'e', 's', 't' });
        buffer.position(1).limit(3);
        CharStream stream1 = createCharStream(buffer);
        assertEquals(Arrays.asList('e', 's'), stream1.boxed().toList());

        CharStream stream2 = createCharStream((CharBuffer) null);
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfReader() throws IOException {
        Reader reader = new java.io.StringReader("abc");
        CharStream stream1 = createCharStream(reader);
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());
        reader.read();
    }

    @Test
    public void testFlatten2DArray() {
        char[][] data = { { 'a', 'b' }, { 'c', 'd', 'e' } };
        List<Character> result = CharStream.flatten(data).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e'), result);

        result = CharStream.flatten(new char[][] {}).boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.flatten(new char[][] { { 'a' }, {} }).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        char[][] data = { { 'a', 'b' }, { 'c', 'd', 'e' } };
        char padding = '-';
        List<Character> result = CharStream.flatten(data, padding, false).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', '-', 'c', 'd', 'e'), result);

        result = CharStream.flatten(data, padding, true).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'b', 'd', '-', 'e'), result);

        result = CharStream.flatten(new char[][] { { 'x' } }, padding, false).boxed().toList();
        assertEquals(Arrays.asList('x'), result);
    }

    @Test
    public void testRange() {
        List<Character> result = CharStream.range('a', 'd').boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c'), result);

        result = CharStream.range('d', 'a').boxed().toList();
        assertTrue(result.isEmpty());

        result = CharStream.range('a', 'a').boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.range('a', 'c', 0));
    }

    @Test
    public void testRangeClosedWithStep() {
        List<Character> result = CharStream.rangeClosed('a', 'e', 2).boxed().toList();
        assertEquals(Arrays.asList('a', 'c', 'e'), result);

        result = CharStream.rangeClosed('e', 'a', -2).boxed().toList();
        assertEquals(Arrays.asList('e', 'c', 'a'), result);

        result = CharStream.rangeClosed('a', 'b', 5).boxed().toList();
        assertEquals(Arrays.asList('a'), result);
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('A', -1));
    }

    @Test
    public void testRandomRange() {
        char start = 'a';
        char end = 'z';
        assertEquals(10, CharStream.random(start, end).limit(10).count());
        CharStream.random(start, end).limit(10).forEach(c -> assertTrue(c >= start && c < end));
    }

    @Test
    public void testRandomCandidates() {
        char[] candidates = { 'x', 'y', 'z' };
        assertEquals(10, CharStream.random(candidates).limit(10).count());
        CharStream.random(candidates).limit(10).forEach(c -> assertTrue(c == 'x' || c == 'y' || c == 'z'));

        CharStream emptyRandom = CharStream.random(new char[] {});
        assertTrue(emptyRandom.boxed().toList().isEmpty());
    }

    @Test
    public void testIterateBooleanSupplierCharSupplierNull() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.iterate(null, () -> 'a'));
    }

    @Test
    public void testIterateInitCharPredicateCharUnaryOperator() {
        CharStream iteratedStream = CharStream.iterate('A', c -> c <= 'C', c -> (char) (c + 1));
        List<Character> result = iteratedStream.boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C'), result);

        result = CharStream.iterate('Z', c -> false, c -> c).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatCharArrays() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { '3', '4', '5' };
        List<Character> result = CharStream.concat(arr1, arr2).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5'), result);

        result = CharStream.concat(new char[0][0]).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'x', 'y' });
        CharStream stream2 = createCharStream(new char[] { 'z' });
        List<Character> result = CharStream.concat(stream1, stream2).boxed().toList();
        assertEquals(Arrays.asList('x', 'y', 'z'), result);
    }

    @Test
    public void testConcatCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'o', 'n', 'e' }), createCharStream(new char[] { 't', 'w', 'o' }));
        List<Character> result = CharStream.concat(streams).boxed().toList();
        assertEquals(Arrays.asList('o', 'n', 'e', 't', 'w', 'o'), result);
    }

    @Test
    public void testZipTwoCharArrays() {
        char[] arr1 = { '1', '2', '3' };
        char[] arr2 = { 'a', 'b' };
        List<Character> result = CharStream.zip(arr1, arr2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a'), (char) ('2' + 'b')), result);

        result = CharStream.zip(new char[] {}, new char[] { 'a' }, (c1, c2) -> c1).boxed().toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testZipTwoCharIterators() {
        CharIterator iter1 = CharList.of('A', 'B', 'C').iterator();
        CharIterator iter2 = CharList.of('D', 'E').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + 'D'), (char) ('B' + 'E')), result);
    }

    @Test
    public void testZipTwoCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'P', 'Q' });
        CharStream stream2 = createCharStream(new char[] { 'R', 'S', 'T' });
        List<Character> result = CharStream.zip(stream1, stream2, (c1, c2) -> (char) (c1 + c2)).boxed().toList();
        assertEquals(Arrays.asList((char) ('P' + 'R'), (char) ('Q' + 'S')), result);
    }

    @Test
    public void testZipCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'A', 'B' }), createCharStream(new char[] { '1', '2', '3' }));
        List<Character> result = CharStream.zip(streams, chars -> (char) (chars[0] + chars[1])).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + '1'), (char) ('B' + '2')), result);
    }

    @Test
    public void testZipThreeCharArraysWithNoneValues() {
        char[] arr1 = { '1', '2' };
        char[] arr2 = { 'a', 'b', 'c' };
        char[] arr3 = { 'x' };
        List<Character> result = CharStream.zip(arr1, arr2, arr3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('2' + 'b' + 'Z'), (char) ('X' + 'c' + 'Z')), result);
    }

    @Test
    public void testZipThreeCharIteratorsWithNoneValues() {
        CharIterator iter1 = CharList.of('1').iterator();
        CharIterator iter2 = CharList.of('a', 'b').iterator();
        CharIterator iter3 = CharList.of('x', 'y', 'z').iterator();
        List<Character> result = CharStream.zip(iter1, iter2, iter3, 'X', 'Y', 'Z', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('1' + 'a' + 'x'), (char) ('X' + 'b' + 'y'), (char) ('X' + 'Y' + 'z')), result);
    }

    @Test
    public void testZipThreeCharStreamsWithNoneValues() {
        CharStream stream1 = createCharStream(new char[] { 'X' });
        CharStream stream2 = createCharStream(new char[] { 'Y', 'Z' });
        CharStream stream3 = createCharStream(new char[] { 'a', 'b', 'c' });
        List<Character> result = CharStream.zip(stream1, stream2, stream3, 'D', 'E', 'F', (c1, c2, c3) -> (char) (c1 + c2 + c3)).boxed().toList();
        assertEquals(Arrays.asList((char) ('X' + 'Y' + 'a'), (char) ('D' + 'Z' + 'b'), (char) ('D' + 'E' + 'c')), result);
    }

    @Test
    public void testMergeTwoCharArrays() {
        char[] arr1 = { 'a', 'c', 'e' };
        char[] arr2 = { 'b', 'd', 'f' };
        List<Character> result = CharStream.merge(arr1, arr2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('a', 'b', 'c', 'd', 'e', 'f'), result);

        result = CharStream.merge(new char[] { 'x' }, new char[] {}, (c1, c2) -> MergeResult.TAKE_FIRST).boxed().toList();
        assertEquals(Arrays.asList('x'), result);

        result = CharStream.merge(new char[] {}, new char[] { 'y' }, (c1, c2) -> MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('y'), result);
    }

    @Test
    public void testMergeTwoCharIterators() {
        CharIterator iter1 = CharList.of('A', 'C').iterator();
        CharIterator iter2 = CharList.of('B', 'D').iterator();
        List<Character> result = CharStream.merge(iter1, iter2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('A', 'B', 'C', 'D'), result);
    }

    @Test
    public void testMergeTwoCharStreams() {
        CharStream stream1 = createCharStream(new char[] { 'X', 'Z' });
        CharStream stream2 = createCharStream(new char[] { 'Y' });
        List<Character> result = CharStream.merge(stream1, stream2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('X', 'Y', 'Z'), result);
    }

    @Test
    public void testMergeCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { '1', '5' }), createCharStream(new char[] { '2', '6' }),
                createCharStream(new char[] { '3', '4' }));
        List<Character> result = CharStream.merge(streams, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5', '6'), result);
    }

    @Test
    public void testToMapKeyAndValueMapperDuplicateKeys() {
        assertThrows(IllegalStateException.class, () -> createCharStream(new char[] { 'a', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c));
    }

    @Test
    public void testToMapKeyAndValueMapperAndMergeFunction() {
        Map<String, Integer> result = createCharStream(new char[] { 'a', 'b', 'a' }).toMap(c -> String.valueOf(c), c -> (int) c, (v1, v2) -> v1 + v2);
        assertEquals(2, result.size());
        assertEquals(97 + 97, (int) result.get("a"));
        assertEquals(98, (int) result.get("b"));
    }

    @Test
    public void testGroupToWithMapFactory() {
        Map<Boolean, List<Character>> result = createCharStream(new char[] { 'a', 'b', 'c', 'd' }).groupTo(c -> c % 2 == 0, Collectors.toList(),
                Suppliers.ofTreeMap());
        assertEquals(2, result.size());
    }

    @Test
    public void testReduceIdentity() {
        char sum = createCharStream(new char[] { '1', '2', '3' }).reduce('0', (c1, c2) -> (char) (c1 + c2 - '0'));
        assertEquals((char) ('0' + '1' + '2' + '3' - '0' - '0' - '0'), sum);

        sum = createCharStream(new char[] {}).reduce('Z', (c1, c2) -> c1);
        assertEquals('Z', sum);
    }

    @Test
    public void testCollectSupplierAccumulatorCombiner() {
        StringBuilder sb = createCharStream(new char[] { 'a', 'b', 'c' }).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("abc", sb.toString());

        sb = createCharStream(new char[] {}).collect(StringBuilder::new, StringBuilder::append, StringBuilder::append);
        assertEquals("", sb.toString());
    }

    @Test
    public void testCollectSupplierAccumulator() {
        CharList list = createCharStream(new char[] { 'd', 'e', 'f' }).collect(CharList::new, CharList::add);
        assertEquals(CharList.of('d', 'e', 'f'), list);

        list = createCharStream(new char[] {}).collect(CharList::new, CharList::add);
        assertEquals(0, list.size());
    }

    @Test
    public void testAllMatch() {
        assertTrue(stream.allMatch(c -> c >= 'a' && c <= 'z'));
        assertFalse(stream2.allMatch(c -> c == 'l'));
        assertTrue(createCharStream(new char[] {}).allMatch(c -> true));
    }

    @Test
    public void testNoneMatch() {
        assertTrue(stream.noneMatch(c -> c == 'x'));
        assertFalse(stream2.noneMatch(c -> c == 'l'));
        assertTrue(createCharStream(new char[] {}).noneMatch(c -> true));
    }

    @Test
    public void testFindFirst() {
        OptionalChar first = stream.findFirst(c -> c == 'l');
        assertTrue(first.isPresent());
        assertEquals('l', first.get());

        first = stream2.findFirst(c -> c == 'x');
        assertFalse(first.isPresent());

        first = createCharStream(new char[] {}).findFirst(c -> true);
        assertFalse(first.isPresent());
    }

    @Test
    public void testFindAny() {
        OptionalChar any = stream.findAny(c -> c == 'l');
        assertTrue(any.isPresent());
        assertEquals('l', any.get());

        any = stream2.findAny(c -> c == 'x');
        assertFalse(any.isPresent());
    }

    @Test
    public void testFindLast() {
        OptionalChar last = stream.findLast(c -> c == 'l');
        assertTrue(last.isPresent());
        assertEquals('l', last.get());

        last = stream2.findLast(c -> c == 'o');
        assertTrue(last.isPresent());
        assertEquals('o', last.get());

        last = stream3.findLast(c -> c == 'x');
        assertFalse(last.isPresent());

        last = createCharStream(new char[] {}).findLast(c -> true);
        assertFalse(last.isPresent());
    }

    @Test
    public void testJoinDelimiterPrefixSuffix() {
        String result = stream.join("-", "[", "]");
        assertEquals("[h-e-l-l-o]", result);

        result = createCharStream(new char[] { 'a' }).join("-", "[", "]");
        assertEquals("[a]", result);

        result = createCharStream(new char[] {}).join("-", "[", "]");
        assertEquals("[]", result);
    }

    @Test
    public void testJoinToJoiner() {
        Joiner joiner = Joiner.with(" | ", "<", ">");
        Joiner resultJoiner = stream.joinTo(joiner);
        assertEquals("<h | e | l | l | o>", resultJoiner.toString());

        joiner = Joiner.with(" | ", "<", ">");
        resultJoiner = createCharStream(new char[] {}).joinTo(joiner);
        assertEquals("<>", resultJoiner.toString());
    }

    @Test
    public void testStreamCreatedAfterFilter() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFilterWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).filter(i -> i > 2, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).filter(i -> i > 2, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterTakeWhile() {
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).takeWhile(i -> i < 3).skip(1).toList());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).count());
        assertEquals(1, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 2 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 2), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).takeWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhile() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDropWhileWithAction() {
        List<Character> dropped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
        dropped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).count());
        dropped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).count());
        dropped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toArray());
        dropped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toArray());
        dropped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).toList());
        dropped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).dropWhile(i -> i < 3, dropped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipUntil() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skipUntil(i -> i >= 3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skipUntil(i -> i >= 3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDistinct() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).distinct().skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 2, (char) 3, (char) 3).map(e -> e).distinct().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).sorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).sorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReverseSorted() {
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).reverseSorted().skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().count());
        assertEquals(4, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 },
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 3, (char) 1, (char) 4, (char) 5, (char) 2).map(e -> e).reverseSorted().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterReversed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).reversed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).count());
        assertArrayEquals(new char[] { 5, 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toArray());
        assertArrayEquals(new char[] { 4, 3, 2, 1 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().toList());
        assertEquals(N.toList((char) 4, (char) 3, (char) 2, (char) 1),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).reversed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterRotated() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).toList());
        assertEquals(N.toList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).rotated(2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).count());
        assertArrayEquals(new char[] { 4, 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toArray());
        assertArrayEquals(new char[] { 5, 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).toList());
        assertEquals(N.toList((char) 5, (char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).rotated(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterShuffled() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).count());
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).count());
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toArray();
        assertEquals(5, shuffled.length);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toArray();
        assertEquals(4, shuffled.length);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().toList();
        assertEquals(5, shuffledList.size());
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled().skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterShuffledWithRandom() {
        Random rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        char[] shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        List<Character> shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
        rnd = new Random(42);
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).count());
        rnd = new Random(42);
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).count());
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toArray();
        assertEquals(5, shuffled.length);
        rnd = new Random(42);
        shuffled = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toArray();
        assertEquals(4, shuffled.length);
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).toList();
        assertEquals(5, shuffledList.size());
        rnd = new Random(42);
        shuffledList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).shuffled(rnd).skip(1).toList();
        assertEquals(4, shuffledList.size());
    }

    @Test
    public void testStreamCreatedAfterCycled() {
        assertEquals(10, CharStream.of((char) 1, (char) 2).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).cycled().limit(10).skip(2).toList());
        assertEquals(10, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).count());
        assertEquals(8, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2),
                CharStream.of((char) 1, (char) 2).map(e -> e).cycled().limit(10).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterCycledWithRounds() {
        assertEquals(6, CharStream.of((char) 1, (char) 2).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).cycled(3).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).count());
        assertArrayEquals(new char[] { 1, 2, 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toArray());
        assertArrayEquals(new char[] { 1, 2, 1, 2 }, CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).toList());
        assertEquals(N.toList((char) 1, (char) 2, (char) 1, (char) 2), CharStream.of((char) 1, (char) 2).map(e -> e).cycled(3).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterIndexed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).count());
        Object[] indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toArray();
        assertEquals(5, indexed.length);
        indexed = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().skip(1).toArray();
        assertEquals(4, indexed.length);
        List<IndexedChar> indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().toList();
        assertEquals(5, indexedList.size());
        assertEquals(0, indexedList.get(0).index());
        assertEquals((char) 1, indexedList.get(0).value());
        indexedList = CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).indexed().skip(1).toList();
        assertEquals(4, indexedList.size());
        assertEquals(1, indexedList.get(0).index());
        assertEquals((char) 2, indexedList.get(0).value());
    }

    @Test
    public void testStreamCreatedAfterSkip() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterSkipWithAction() {
        List<Character> skipped = new ArrayList<>();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(2, skipped::add).skip(1).toList());
        skipped.clear();
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).count());
        skipped.clear();
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).count());
        skipped.clear();
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toArray());
        skipped.clear();
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toArray());
        skipped.clear();
        assertEquals(N.toList((char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).toList());
        skipped.clear();
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(2, skipped::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterLimit() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).limit(3).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).limit(3).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterStep() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).toList());
        assertEquals(N.toList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).step(2).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toArray());
        assertArrayEquals(new char[] { 3, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).toList());
        assertEquals(N.toList((char) 3, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).step(2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterOnEach() {
        List<Character> collected = new ArrayList<>();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).peek(collected::add).skip(1).toList());
        collected.clear();
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).count());
        collected.clear();
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).count());
        collected.clear();
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toArray());
        collected.clear();
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toArray());
        collected.clear();
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).toList());
        collected.clear();
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).peek(collected::add).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependStream() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend(CharStream.of((char) 1, (char) 2)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependOptional() {
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).prepend(OptionalChar.of((char) 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).count());
        assertEquals(4, CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).prepend(OptionalChar.of((char) 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append(CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendOptional() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).append(OptionalChar.of((char) 5)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4).map(e -> e).append(OptionalChar.of((char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().appendIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDefaultIfEmpty() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3 },
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3),
                CharStream.of((char) 1, (char) 2, (char) 3).defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());

        assertEquals(2, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).count());
        assertEquals(1, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().defaultIfEmpty(() -> CharStream.of((char) 4, (char) 5)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmpty() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty().skip(1).toList());

        try {
            CharStream.empty().throwIfEmpty().count();
            fail("Should throw NoSuchElementException");
        } catch (NoSuchElementException e) {
        }
    }

    @Test
    public void testStreamCreatedAfterThrowIfEmptyWithSupplier() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).throwIfEmpty(() -> new IllegalStateException("Empty")).skip(1).toList());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).count());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).throwIfEmpty(() -> new IllegalStateException("Empty")).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .throwIfEmpty(() -> new IllegalStateException("Empty"))
                        .skip(1)
                        .toList());

        try {
            CharStream.empty().throwIfEmpty(() -> new IllegalStateException("Empty")).count();
            fail("Should throw IllegalStateException");
        } catch (IllegalStateException e) {
            assertEquals("Empty", e.getMessage());
        }
    }

    @Test
    public void testStreamCreatedAfterIfEmpty() {
        AtomicBoolean actionExecuted = new AtomicBoolean(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).count());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toArray());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).ifEmpty(() -> actionExecuted.set(true)).skip(1).toList());
        assertFalse(actionExecuted.get());
        actionExecuted.set(false);

        assertEquals(0, CharStream.empty().ifEmpty(() -> actionExecuted.set(true)).count());
        assertTrue(actionExecuted.get());
    }

    @Test
    public void testStreamCreatedAfterOnClose() {
        AtomicBoolean closed = new AtomicBoolean(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).count());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).count());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toArray());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).toList());
        assertTrue(closed.get());
        closed.set(false);

        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).onClose(() -> closed.set(true)).skip(1).toList());
        assertTrue(closed.get());
    }

    @Test
    public void testStreamCreatedAfterIntersection() {
        List<Character> collection = N.toList((char) 2, (char) 3, (char) 4, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).toList());
        assertEquals(N.toList((char) 3, (char) 4), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).intersection(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toArray());
        assertArrayEquals(new char[] { 3, 4 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).toList());
        assertEquals(N.toList((char) 3, (char) 4),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).intersection(collection).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterDifference() {
        List<Character> collection = N.toList((char) 2, (char) 3, (char) 6);
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).difference(collection).skip(1).toList());
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toArray());
        assertArrayEquals(new char[] { 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).toList());
        assertEquals(N.toList((char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).difference(collection).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMap() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 3, 4, 5, 6 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5, (char) 6),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).map(e -> (char) (e + 1)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToInt() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.toList(10, 20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.toList(20, 30, 40, 50), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).mapToInt(e -> e * 10).skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).count());
        assertArrayEquals(new int[] { 10, 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).toArray());
        assertArrayEquals(new int[] { 20, 30, 40, 50 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).toArray());
        assertEquals(N.toList(10, 20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).boxed().toList());
        assertEquals(N.toList(20, 30, 40, 50),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).mapToInt(e -> e * 10).skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMap() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMap(e -> CharStream.of(e, (char) (e + 10))).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapArray() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).count());
        assertArrayEquals(new char[] { 1, 11, 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).toArray());
        assertArrayEquals(new char[] { 2, 12, 3, 13 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toArray());
        assertEquals(N.toList((char) 1, (char) 11, (char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).toList());
        assertEquals(N.toList((char) 2, (char) 12, (char) 3, (char) 13),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatmap(e -> new char[] { e, (char) (e + 10) }).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToInt() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 }, CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.toList(2, 20, 3, 30), CharStream.of((char) 1, (char) 2, (char) 3).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).count());
        assertArrayEquals(new int[] { 1, 10, 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).toArray());
        assertArrayEquals(new int[] { 2, 20, 3, 30 },
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).toArray());
        assertEquals(N.toList(1, 10, 2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).boxed().toList());
        assertEquals(N.toList(2, 20, 3, 30),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).flatMapToInt(e -> IntStream.of(e, e * 10)).skip(2).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterFlatMapToObj() {
        assertEquals(6, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapToObj(e -> Stream.of("A" + e, "B" + e)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapPartial() {
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.toList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .count());
        assertEquals(1,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 20, 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toArray());
        assertArrayEquals(new char[] { 40 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 20, (char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .toList());
        assertEquals(N.toList((char) 40),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)
                        .map(e -> e)
                        .mapPartial(e -> e % 2 == 0 ? OptionalChar.of((char) (e * 10)) : OptionalChar.empty())
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapse() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).count());
        List<CharList> collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).count());
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).toList();
        assertEquals(3, collapsed.size());
        assertEquals(CharList.of((char) 1, (char) 2), collapsed.get(0));
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(1));
        assertEquals(CharList.of((char) 7), collapsed.get(2));
        collapsed = CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1).skip(1).toList();
        assertEquals(2, collapsed.size());
        assertEquals(CharList.of((char) 4, (char) 5), collapsed.get(0));
        assertEquals(CharList.of((char) 7), collapsed.get(1));
    }

    @Test
    public void testStreamCreatedAfterCollapseWithMerge() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7).map(e -> e).collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((a, b) -> b - a <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterCollapseTriWithMerge() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 3, 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 9, 7 },
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 3, (char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 9, (char) 7),
                CharStream.of((char) 1, (char) 2, (char) 4, (char) 5, (char) 7)
                        .map(e -> e)
                        .collapse((first, last, next) -> next - last <= 1, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterScan() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 1, 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 3, 6, 10, 15 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 3, (char) 6, (char) 10, (char) 15),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInit() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterScanWithInitIncluded() {
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(6, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).count());
        assertEquals(5,
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 10, 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 11, 13, 16, 20, 25 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 10, (char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 11, (char) 13, (char) 16, (char) 20, (char) 25),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).scan((char) 10, true, (a, b) -> (char) (a + b)).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterPrependArray() {
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).prepend((char) 1, (char) 2).skip(1).toList());
        assertEquals(5, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).count());
        assertEquals(4, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 3, (char) 4, (char) 5).map(e -> e).prepend((char) 1, (char) 2).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendArray() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3).append((char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).append((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterAppendIfEmptyArray() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(2, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3 }, CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3), CharStream.of((char) 1, (char) 2, (char) 3).appendIfEmpty((char) 4, (char) 5).skip(1).toList());

        assertEquals(2, CharStream.empty().appendIfEmpty((char) 4, (char) 5).count());
        assertEquals(1, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).toArray());
        assertArrayEquals(new char[] { 5 }, CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 5), CharStream.empty().appendIfEmpty((char) 4, (char) 5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterMergeWith() {
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
        assertEquals(8,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .count());
        assertEquals(7,
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5, 6, 7, 8 },
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5, (char) 6, (char) 7, (char) 8),
                CharStream.of((char) 1, (char) 3, (char) 5, (char) 7)
                        .map(e -> e)
                        .mergeWith(CharStream.of((char) 2, (char) 4, (char) 6, (char) 8), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND)
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWith() {
        assertEquals(3, CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).toList());
        assertEquals(N.toList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).skip(1).toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3).map(e -> e).zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b)).count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 9 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 9),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThree() {
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(2,
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 18 },
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 18),
                CharStream.of((char) 1, (char) 2, (char) 3)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9), (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 5, 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toArray());
        assertArrayEquals(new char[] { 7, 6, 7 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 5, (char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .toList());
        assertEquals(N.toList((char) 7, (char) 6, (char) 7),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6, (char) 7), (char) 0, (char) 0, (a, b) -> (char) (a + b))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterZipWithThreeDefaults() {
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
        assertEquals(4,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .count());
        assertEquals(3,
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .count());
        assertArrayEquals(new char[] { 12, 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toArray());
        assertArrayEquals(new char[] { 15, 15, 10 },
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toArray());
        assertEquals(N.toList((char) 12, (char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .toList());
        assertEquals(N.toList((char) 15, (char) 15, (char) 10),
                CharStream.of((char) 1, (char) 2)
                        .map(e -> e)
                        .zipWith(CharStream.of((char) 4, (char) 5, (char) 6), CharStream.of((char) 7, (char) 8, (char) 9, (char) 10), (char) 0, (char) 0,
                                (char) 0, (a, b, c) -> (char) (a + b + c))
                        .skip(1)
                        .toList());
    }

    @Test
    public void testStreamCreatedAfterAsIntStream() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().boxed().toList());
        assertEquals(N.toList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).asIntStream().skip(1).boxed().toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).count());
        assertArrayEquals(new int[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().toArray());
        assertArrayEquals(new int[] { 2, 3, 4, 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).toArray());
        assertEquals(N.toList(1, 2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().boxed().toList());
        assertEquals(N.toList(2, 3, 4, 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).asIntStream().skip(1).boxed().toList());
    }

    @Test
    public void testStreamCreatedAfterBoxed() {
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).boxed().skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).count());
        assertArrayEquals(new Character[] { (char) 1, (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toArray(Character[]::new));
        assertArrayEquals(new Character[] { (char) 2, (char) 3, (char) 4, (char) 5 },
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toArray(Character[]::new));
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).boxed().skip(1).toList());
    }

    @Test
    public void testStreamCreatedByEmpty() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.toList(), CharStream.empty().toList());
        assertEquals(N.toList(), CharStream.empty().skip(1).toList());
        assertEquals(0, CharStream.empty().map(e -> e).count());
        assertEquals(0, CharStream.empty().map(e -> e).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().map(e -> e).skip(1).toArray());
        assertEquals(N.toList(), CharStream.empty().map(e -> e).toList());
        assertEquals(N.toList(), CharStream.empty().map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByDefer() {
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).skip(1).toList());
        assertEquals(5, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).count());
        assertEquals(4, CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 },
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.defer(() -> CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfNullable() {
        assertEquals(1, CharStream.ofNullable('a').count());
        assertEquals(0, CharStream.ofNullable('a').skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable('a').toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable('a').skip(1).toArray());
        assertEquals(N.toList('a'), CharStream.ofNullable('a').toList());
        assertEquals(N.toList(), CharStream.ofNullable('a').skip(1).toList());
        assertEquals(1, CharStream.ofNullable('a').map(e -> e).count());
        assertEquals(0, CharStream.ofNullable('a').map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a' }, CharStream.ofNullable('a').map(e -> e).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable('a').map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a'), CharStream.ofNullable('a').map(e -> e).toList());
        assertEquals(N.toList(), CharStream.ofNullable('a').map(e -> e).skip(1).toList());

        assertEquals(0, CharStream.ofNullable((Character) null).count());
        assertEquals(0, CharStream.ofNullable((Character) null).skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).toArray());
        assertArrayEquals(new char[] {}, CharStream.ofNullable((Character) null).skip(1).toArray());
        assertEquals(N.toList(), CharStream.ofNullable((Character) null).toList());
        assertEquals(N.toList(), CharStream.ofNullable((Character) null).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArray() {
        assertEquals(0, CharStream.empty().count());
        assertEquals(0, CharStream.empty().skip(1).count());
        assertArrayEquals(new char[] {}, CharStream.empty().toArray());
        assertArrayEquals(new char[] {}, CharStream.empty().skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.of((char) 1, (char) 2, (char) 3, (char) 4, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfArrayWithRange() {
        char[] array = new char[] { 1, 2, 3, 4, 5, 6, 7 };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 3, 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 5 }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 3, (char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 5), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequence() {
        assertEquals(5, CharStream.of("abcde").count());
        assertEquals(4, CharStream.of("abcde").skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of("abcde").skip(1).toList());
        assertEquals(5, CharStream.of("abcde").map(e -> e).count());
        assertEquals(4, CharStream.of("abcde").map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of("abcde").map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of("abcde").map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharSequenceWithRange() {
        assertEquals(3, CharStream.of("abcdefg", 2, 5).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of("abcdefg", 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of("abcdefg", 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of("abcdefg", 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArray() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e' };
        assertEquals(5, CharStream.of(array).count());
        assertEquals(4, CharStream.of(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(array).skip(1).toList());
        assertEquals(5, CharStream.of(array).map(e -> e).count());
        assertEquals(4, CharStream.of(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCharacterArrayWithRange() {
        Character[] array = new Character[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g' };
        assertEquals(3, CharStream.of(array, 2, 5).count());
        assertEquals(2, CharStream.of(array, 2, 5).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of(array, 2, 5).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of(array, 2, 5).skip(1).toList());
        assertEquals(3, CharStream.of(array, 2, 5).map(e -> e).count());
        assertEquals(2, CharStream.of(array, 2, 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'c', 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'e' }, CharStream.of(array, 2, 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('c', 'd', 'e'), CharStream.of(array, 2, 5).map(e -> e).toList());
        assertEquals(N.toList('d', 'e'), CharStream.of(array, 2, 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfCollection() {
        List<Character> collection = N.toList('a', 'b', 'c', 'd', 'e');
        assertEquals(5, CharStream.of(collection).count());
        assertEquals(4, CharStream.of(collection).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(collection).skip(1).toList());
        assertEquals(5, CharStream.of(collection).map(e -> e).count());
        assertEquals(4, CharStream.of(collection).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(collection).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(collection).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByOfIterator() {
        CharIterator iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(5, CharStream.of(iterator).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertEquals(4, CharStream.of(iterator).skip(1).count());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(iterator).toArray());

        iterator = new CharIterator() {
            private char[] arr = new char[] { 'a', 'b', 'c', 'd', 'e' };
            private int index = 0;

            @Override
            public boolean hasNext() {
                return index < arr.length;
            }

            @Override
            public char nextChar() {
                return arr[index++];
            }
        };
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(iterator).skip(1).toArray());
    }

    @Test
    public void testStreamCreatedByOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(5, CharStream.of(buffer).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(4, CharStream.of(buffer).skip(1).count());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.of(buffer).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.of(buffer).skip(1).toArray());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.of(buffer).toList());

        buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c', 'd', 'e' });
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.of(buffer).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlatten2D() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenVertically() {
        char[][] array = new char[][] { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        assertEquals(6, CharStream.flatten(array, true).count());
        assertEquals(5, CharStream.flatten(array, true).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).skip(1).toArray());
        assertEquals(N.toList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).toList());
        assertEquals(N.toList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, true).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, true).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'd', 'b', 'e', 'c', 'f' }, CharStream.flatten(array, true).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'd', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).toList());
        assertEquals(N.toList('d', 'b', 'e', 'c', 'f'), CharStream.flatten(array, true).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlattenWithAlignment() {
        char[][] array = new char[][] { { 'a', 'b' }, { 'c', 'd', 'e' } };
        assertEquals(6, CharStream.flatten(array, '*', false).count());
        assertEquals(5, CharStream.flatten(array, '*', false).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).skip(1).toArray());
        assertEquals(N.toList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).toList());
        assertEquals(N.toList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).skip(1).toList());
        assertEquals(6, CharStream.flatten(array, '*', false).map(e -> e).count());
        assertEquals(5, CharStream.flatten(array, '*', false).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', '*', 'c', 'd', 'e' }, CharStream.flatten(array, '*', false).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).toList());
        assertEquals(N.toList('b', '*', 'c', 'd', 'e'), CharStream.flatten(array, '*', false).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByFlatten3D() {
        char[][][] array = new char[][][] { { { 'a', 'b' }, { 'c' } }, { { 'd', 'e' } } };
        assertEquals(5, CharStream.flatten(array).count());
        assertEquals(4, CharStream.flatten(array).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).skip(1).toList());
        assertEquals(5, CharStream.flatten(array).map(e -> e).count());
        assertEquals(4, CharStream.flatten(array).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.flatten(array).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.flatten(array).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRange() {
        assertEquals(5, CharStream.range((char) 1, (char) 6).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).skip(1).toList());
        assertEquals(5, CharStream.range((char) 1, (char) 6).map(e -> e).count());
        assertEquals(4, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.range((char) 1, (char) 6).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeWithStep() {
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).toList());
        assertEquals(N.toList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(3, CharStream.range((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(2, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7 }, CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 7), CharStream.range((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeClosed() {
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).skip(1).toList());
        assertEquals(5, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).count());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.rangeClosed((char) 1, (char) 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRangeClosedWithStep() {
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).toList());
        assertEquals(N.toList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).skip(1).toList());
        assertEquals(4, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).count());
        assertEquals(3, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toArray());
        assertArrayEquals(new char[] { 4, 7, 10 }, CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).toList());
        assertEquals(N.toList((char) 4, (char) 7, (char) 10), CharStream.rangeClosed((char) 1, (char) 10, 3).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByRepeat() {
        assertEquals(5, CharStream.repeat('a', 5).count());
        assertEquals(4, CharStream.repeat('a', 5).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).skip(1).toArray());
        assertEquals(N.toList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).toList());
        assertEquals(N.toList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).skip(1).toList());
        assertEquals(5, CharStream.repeat('a', 5).map(e -> e).count());
        assertEquals(4, CharStream.repeat('a', 5).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'a', 'a', 'a', 'a' }, CharStream.repeat('a', 5).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).toList());
        assertEquals(N.toList('a', 'a', 'a', 'a'), CharStream.repeat('a', 5).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).count());
        count[0] = 0;
        assertEquals(4, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate(() -> count[0] < 5, () -> (char) ++count[0]).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateWithInitAndPredicate() {
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> e <= 5, e -> (char) (e + 1)).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByIterateInfinite() {
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).limit(5).skip(1).toList());
        assertEquals(5, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).count());
        assertEquals(4, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).count());
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toArray());
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toArray());
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5),
                CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).toList());
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.iterate((char) 1, e -> (char) (e + 1)).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByGenerate() {
        final int[] count = { 0 };
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).limit(5).skip(1).toList());
        count[0] = 0;
        assertEquals(5, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).count());
        count[0] = 0;
        assertEquals(4, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).count());
        count[0] = 0;
        assertArrayEquals(new char[] { 1, 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toArray());
        count[0] = 0;
        assertArrayEquals(new char[] { 2, 3, 4, 5 }, CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toArray());
        count[0] = 0;
        assertEquals(N.toList((char) 1, (char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).toList());
        count[0] = 0;
        assertEquals(N.toList((char) 2, (char) 3, (char) 4, (char) 5), CharStream.generate(() -> (char) ++count[0]).map(e -> e).limit(5).skip(1).toList());
    }

    @Test
    public void testStreamCreatedByConcatArrays() {
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' }, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).skip(1).toList());
        assertEquals(5, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).count());
        assertEquals(4, CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).count());
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toArray());
        assertArrayEquals(new char[] { 'b', 'c', 'd', 'e' },
                CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toArray());
        assertEquals(N.toList('a', 'b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).toList());
        assertEquals(N.toList('b', 'c', 'd', 'e'), CharStream.concat(new char[] { 'a', 'b' }, new char[] { 'c', 'd', 'e' }).map(e -> e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlatmapToObjCollection() {
        assertEquals(6, CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').map(e -> e).flatmapToObj(e -> N.toList("A" + e, "B" + e)).skip(2).toList());
    }

    @Test
    public void testStreamCreatedAfterMapToObj() {
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" }, CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.toList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).toList());
        assertEquals(N.toList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').mapToObj(e -> "C" + e).skip(1).toList());
        assertEquals(5, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).count());
        assertEquals(4, CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).count());
        assertArrayEquals(new String[] { "C1", "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toArray(String[]::new));
        assertArrayEquals(new String[] { "C2", "C3", "C4", "C5" },
                CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toArray(String[]::new));
        assertEquals(N.toList("C1", "C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).toList());
        assertEquals(N.toList("C2", "C3", "C4", "C5"), CharStream.of('1', '2', '3', '4', '5').map(e -> e).mapToObj(e -> "C" + e).skip(1).toList());
    }

    @Test
    public void testStreamCreatedAfterFlattmapToObjArray() {
        assertEquals(6, CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"), CharStream.of('1', '2', '3').flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
        assertEquals(6, CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).count());
        assertEquals(4, CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).count());
        assertArrayEquals(new String[] { "A1", "B1", "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toArray(String[]::new));
        assertArrayEquals(new String[] { "A2", "B2", "A3", "B3" },
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toArray(String[]::new));
        assertEquals(N.toList("A1", "B1", "A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).toList());
        assertEquals(N.toList("A2", "B2", "A3", "B3"),
                CharStream.of('1', '2', '3').map(e -> e).flatMapArrayToObj(e -> new String[] { "A" + e, "B" + e }).skip(2).toList());
    }

    @Test
    public void testOfChar() {
        CharStream stream = createCharStream('x');
        assertThat(stream.toArray()).containsExactly('x');
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharStream stream = createCharStream(array, 1, 4);
        assertThat(stream.toArray()).containsExactly('b', 'c', 'd');
    }

    @Test
    public void testOfCharSequenceWithRange() {
        CharStream stream = createCharStream("hello world", 2, 7);
        assertThat(stream.toArray()).containsExactly('l', 'l', 'o', ' ', 'w');
    }

    @Test
    public void testOfCollection() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        CharStream stream = createCharStream(list);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testRandomWithRange() {
        CharStream stream = CharStream.random('a', 'd').limit(100);
        char[] result = stream.toArray();
        assertThat(result).hasSize(100);
        for (char c : result) {
            assertThat(c).isGreaterThanOrEqualTo('a').isLessThan('d');
        }
    }

    @Test
    public void testRandomWithCandidates() {
        char[] candidates = { 'x', 'y', 'z' };
        CharStream stream = CharStream.random(candidates).limit(50);
        char[] result = stream.toArray();
        assertThat(result).hasSize(50);
        for (char c : result) {
            assertThat(candidates).contains(c);
        }
    }

    @Test
    public void testIterateWithHasNextAndNext() {
        AtomicInteger counter = new AtomicInteger(0);
        CharStream stream = CharStream.iterate(() -> counter.get() < 3, () -> (char) ('a' + counter.getAndIncrement()));
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testIterateWithInitAndFunction() {
        CharStream stream = CharStream.iterate('a', c -> (char) (c + 1)).limit(3);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testIterateWithInitPredicateAndFunction() {
        CharStream stream = CharStream.iterate('a', c -> c < 'd', c -> (char) (c + 1));
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testConcatArrays() {
        char[] a1 = { 'a', 'b' };
        char[] a2 = { 'c', 'd' };
        CharStream stream = CharStream.concat(a1, a2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testConcatStreams() {
        CharStream s1 = createCharStream('a', 'b');
        CharStream s2 = createCharStream('c', 'd');
        CharStream stream = CharStream.concat(s1, s2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testConcatCollection() {
        List<CharStream> streams = Arrays.asList(createCharStream('a', 'b'), createCharStream('c', 'd'));
        CharStream stream = CharStream.concat(streams);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testZipArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, (c1, c2) -> (char) (c1 + c2 - 'd'));
        assertThat(stream.toArray()).containsExactly('u', 'w', 'y');
    }

    @Test
    public void testZipStreams() {
        CharStream s1 = createCharStream('a', 'b', 'c');
        CharStream s2 = createCharStream('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
    }

    @Test
    public void testZipWithDefaults() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, '0', '1', (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
    }

    @Test
    public void testMergeArrays() {
        char[] a = { 'a', 'c', 'e' };
        char[] b = { 'b', 'd', 'f' };
        CharStream stream = CharStream.merge(a, b, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd', 'e', 'f');
    }

    @Test
    public void testFilter() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').filter(c -> c % 2 == 0);
        assertThat(stream.toArray()).containsExactly('b', 'd');
    }

    @Test
    public void testToList() {
        List<Character> list = createCharStream('a', 'b', 'c').toList();
        assertThat(list).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testToSet() {
        Set<Character> set = createCharStream('a', 'b', 'a', 'c').toSet();
        assertThat(set).containsExactlyInAnyOrder('a', 'b', 'c');
    }

    @Test
    public void testReduceWithoutIdentity() {
        OptionalChar result = createCharStream('a', 'b', 'c').reduce((c1, c2) -> c2);
        assertThat(result).isEqualTo(OptionalChar.of('c'));
    }

    @Test
    public void testCollect() {
        StringBuilder sb = createCharStream('a', 'b', 'c').collect(StringBuilder::new, (builder, c) -> builder.append(c), (b1, b2) -> b1.append(b2));
        assertThat(sb.toString()).isEqualTo("abc");
    }

    @Test
    public void testJoin() {
        String result = createCharStream('a', 'b', 'c').join(", ");
        assertThat(result).isEqualTo("a, b, c");
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = createCharStream('a', 'b', 'c').join(", ", "[", "]");
        assertThat(result).isEqualTo("[a, b, c]");
    }

    @Test
    public void testTakeWhile() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').takeWhile(c -> c < 'c');
        assertThat(stream.toArray()).containsExactly('a', 'b');
    }

    @Test
    public void testDropWhile() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').dropWhile(c -> c < 'c');
        assertThat(stream.toArray()).containsExactly('c', 'd');
    }

    @Test
    public void testScanWithSeed() {
        CharStream stream = createCharStream('a', 'b', 'c').scan('x', (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testCollapse() {
        Stream<CharList> result = createCharStream('a', 'a', 'b', 'b', 'c').collapse((c1, c2) -> c1 == c2);
        List<CharList> lists = result.toList();
        assertThat(lists).hasSize(3);
        assertThat(lists.get(0).toArray()).containsExactly('a', 'a');
        assertThat(lists.get(1).toArray()).containsExactly('b', 'b');
        assertThat(lists.get(2).toArray()).containsExactly('c');
    }

    @Test
    public void testRotated() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').rotated(2);
        assertThat(stream.toArray()).containsExactly('c', 'd', 'a', 'b');
    }

    @Test
    public void testCycled() {
        CharStream stream = createCharStream('a', 'b').cycled().limit(5);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'a', 'b', 'a');
    }

    @Test
    public void testCycledWithRounds() {
        CharStream stream = createCharStream('a', 'b').cycled(2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'a', 'b');
    }

    @Test
    public void testPrepend() {
        CharStream stream = createCharStream('b', 'c').prepend('a');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testAppend() {
        CharStream stream = createCharStream('a', 'b').append('c');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testAppendIfEmpty() {
        assertThat(CharStream.empty().appendIfEmpty('x').toArray()).containsExactly('x');
        assertThat(createCharStream('a').appendIfEmpty('x').toArray()).containsExactly('a');
    }

    @Test
    public void testInvalidRange() {
        assertDoesNotThrow(() -> {
            CharStream.range('z', 'a').toList().isEmpty();
        });
    }

    @Test
    public void testNegativeRepeat() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('a', -1));
    }

    @Test
    public void testEmptyStreamNextElement() {
        assertThrows(NoSuchElementException.class, () -> CharStream.empty().iterator().nextChar());
    }

    @Test
    public void testAutoClose() {
        AtomicInteger closeCount = new AtomicInteger(0);
        try (CharStream stream = createCharStream('a', 'b', 'c').onClose(closeCount::incrementAndGet)) {
            stream.count();
        }
        assertThat(closeCount.get()).isEqualTo(1);
    }

    @Test
    public void testEmptyStreamOperations() {
        assertThat(CharStream.empty().min()).isEqualTo(OptionalChar.empty());
        assertThat(CharStream.empty().max()).isEqualTo(OptionalChar.empty());
        assertThat(CharStream.empty().sum()).isEqualTo(0);
        assertThat(CharStream.empty().average()).isEqualTo(OptionalDouble.empty());
        assertThat(CharStream.empty().count()).isEqualTo(0);
    }

    @Test
    public void testStreamReuseException() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.count();

        assertThrows(IllegalStateException.class, () -> stream.count());
    }

    @Test
    public void testToMap() {
        Map<Character, Integer> map = createCharStream('a', 'b', 'c').toMap(c -> c, c -> c - 'a');
        assertThat(map).containsExactly(entry('a', 0), entry('b', 1), entry('c', 2));
    }

    // --- Missing dedicated test methods ---

    @Test
    public void testOfReaderWithCloseFlag() {
        Reader reader = new StringReader("abc");
        CharStream stream = CharStream.of(reader, true);
        assertEquals(3, stream.count());
        stream.close();
    }

    @Test
    public void testOfReaderWithCloseFlagFalse() {
        Reader reader = new StringReader("xyz");
        CharStream stream = CharStream.of(reader, false);
        char[] result = stream.toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
        stream.close();
    }

    @Test
    public void testOfNullReader() {
        CharStream stream = CharStream.of((Reader) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullCharBuffer() {
        CharStream stream = CharStream.of((CharBuffer) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullCharIterator() {
        CharStream stream = CharStream.of((CharIterator) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullCharSequence() {
        CharStream stream = CharStream.of((CharSequence) null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testFlatMapArrayToObjDedicated() {
        CharStream stream = createCharStream('a', 'b');
        List<String> result = stream.flatMapArrayToObj(c -> new String[] { "x" + c, "y" + c }).toList();
        assertEquals(4, result.size());
        assertEquals("xa", result.get(0));
        assertEquals("ya", result.get(1));
    }

    @Test
    public void testFindFirstNoPredicateDedicated() {
        OptionalChar result = createCharStream('x', 'y', 'z').findFirst();
        assertTrue(result.isPresent());
        assertEquals('x', result.get());
    }

    @Test
    public void testFindFirstNoPredicate_EmptyStream() {
        OptionalChar result = CharStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicateDedicated() {
        OptionalChar result = createCharStream('a', 'b').findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAnyNoPredicate_EmptyStream() {
        OptionalChar result = CharStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testEmptyStreamMinDedicated() {
        assertFalse(CharStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamMaxDedicated() {
        assertFalse(CharStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamAverageDedicated() {
        assertFalse(CharStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamSumDedicated() {
        assertEquals(0, CharStream.empty().sum());
    }

    @Test
    public void testEmptyStreamReduceDedicated() {
        assertFalse(CharStream.empty().reduce((a, b) -> (char) (a + b)).isPresent());
    }

    @Test
    public void testReduceWithIdentityOnEmpty() {
        char result = CharStream.empty().reduce('z', (a, b) -> (char) (a + b));
        assertEquals('z', result);
    }

    @Test
    public void testEmptyStreamSummaryStatisticsDedicated() {
        CharSummaryStatistics stats = CharStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    @Test
    public void testLimitWithOffset() {
        char[] result = createCharStream('a', 'b', 'c', 'd', 'e').limit(1, 3).toArray();
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, result);
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, CharStream.range('a', 'a').count());
    }

    @Test
    public void testRangeClosedSingleElement() {
        assertArrayEquals(new char[] { 'a' }, CharStream.rangeClosed('a', 'a').toArray());
    }

    @Test
    public void testDefaultIfEmptyOnNonEmpty() {
        char[] result = createCharStream('a', 'b').defaultIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDefaultIfEmptyOnEmpty() {
        char[] result = CharStream.empty().defaultIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'z' }, result);
    }

    @Test
    public void testPeekIsAliasForOnEach() {
        List<Character> peeked = new ArrayList<>();
        char[] result = createCharStream('a', 'b', 'c').peek(c -> peeked.add(c)).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
        assertEquals(3, peeked.size());
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        char[] result = CharStream.empty().appendIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'z' }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        char[] result = createCharStream('a').appendIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'a' }, result);
    }

    @Test
    public void testRateLimitedWithPermitsPerSecondDouble() {
        char[] result = createCharStream('a', 'b', 'c').rateLimited(1000.0).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testJoinWithDelimiterOnly() {
        String result = createCharStream('a', 'b', 'c').join(", ");
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testDeferNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.defer(null));
    }

    @Test
    public void testGenerateNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.generate(null));
    }

    @Test
    public void testDelayWithJavaDuration() {
        CharStream stream = createCharStream('a', 'b').delay(java.time.Duration.ofMillis(1));
        assertArrayEquals(new char[] { 'a', 'b' }, stream.toArray());
    }

    @Test
    public void testZipWithThreeStreamsDedicated() {
        CharStream a = createCharStream('a', 'b');
        CharStream b = CharStream.of('x', 'y');
        CharStream c = CharStream.of('1');
        char[] result = a.zipWith(b, c, (x, y, z) -> (char) (x + y + z - 'a' - 'x')).toArray();
        assertEquals(1, result.length);
    }

    @Test
    public void testMergeWithDedicated() {
        CharStream a = createCharStream('a', 'c', 'e');
        CharStream b = CharStream.of('b', 'd', 'f');
        char[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testConcatEmptyArrays() {
        CharStream stream = CharStream.concat(new char[0], new char[0]);
        assertEquals(0, stream.count());
    }

    // ========== New tests for previously untested methods ==========

    @Test
    public void testToImmutableList() {
        CharStream stream = createCharStream('a', 'b', 'c');
        com.landawn.abacus.util.ImmutableList<Character> result = stream.toImmutableList();
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
        assertEquals(Character.valueOf('b'), result.get(1));
        assertEquals(Character.valueOf('c'), result.get(2));
    }

    @Test
    public void testToImmutableList_EmptyStream() {
        com.landawn.abacus.util.ImmutableList<Character> result = CharStream.empty().toImmutableList();
        assertEquals(0, result.size());
    }

    @Test
    public void testToImmutableSet() {
        CharStream stream = createCharStream('a', 'b', 'c', 'a');
        com.landawn.abacus.util.ImmutableSet<Character> result = stream.toImmutableSet();
        assertEquals(3, result.size());
        assertTrue(result.contains('a'));
        assertTrue(result.contains('b'));
        assertTrue(result.contains('c'));
    }

    @Test
    public void testToImmutableSet_EmptyStream() {
        com.landawn.abacus.util.ImmutableSet<Character> result = CharStream.empty().toImmutableSet();
        assertEquals(0, result.size());
    }

    @Test
    public void testElementAt() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        OptionalChar result = stream.elementAt(2);
        assertTrue(result.isPresent());
        assertEquals('c', result.get());
    }

    @Test
    public void testElementAt_FirstElement() {
        CharStream stream = createCharStream('x', 'y', 'z');
        OptionalChar result = stream.elementAt(0);
        assertTrue(result.isPresent());
        assertEquals('x', result.get());
    }

    @Test
    public void testElementAt_OutOfBounds() {
        CharStream stream = createCharStream('a', 'b');
        OptionalChar result = stream.elementAt(5);
        assertFalse(result.isPresent());
    }

    @Test
    public void testElementAt_NegativePosition() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertThrows(IllegalArgumentException.class, () -> stream.elementAt(-1));
    }

    @Test
    public void testApplyIfNotEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Optional<char[]> result = stream.applyIfNotEmpty(s -> s.toArray());
        assertTrue(result.isPresent());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result.get());
    }

    @Test
    public void testApplyIfNotEmpty_EmptyStream() {
        CharStream stream = CharStream.empty();
        Optional<char[]> result = stream.applyIfNotEmpty(s -> s.toArray());
        assertFalse(result.isPresent());
    }

    @Test
    public void testAcceptIfNotEmpty() {
        CharStream stream = createCharStream('a', 'b', 'c');
        List<Character> collected = new ArrayList<>();
        stream.acceptIfNotEmpty(s -> s.forEach(c -> collected.add(c)));
        assertEquals(3, collected.size());
    }

    @Test
    public void testAcceptIfNotEmpty_EmptyStream() {
        CharStream stream = CharStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        stream.acceptIfNotEmpty(s -> called.set(true));
        assertFalse(called.get());
    }

    @Test
    public void testTransform() {
        CharStream stream = createCharStream('a', 'b', 'c');
        IntStream result = stream.transform(s -> s.mapToInt(c -> (int) c));
        assertNotNull(result);
        int[] arr = result.toArray();
        assertEquals(3, arr.length);
        assertEquals(97, arr[0]);
    }

    @Test
    public void testToCollection() {
        CharStream stream = createCharStream('a', 'b', 'c');
        java.util.LinkedList<Character> result = stream.toCollection(java.util.LinkedList::new);
        assertEquals(3, result.size());
        assertEquals(Character.valueOf('a'), result.get(0));
    }

    @Test
    public void testToMultiset() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> result = stream.toMultiset();
        assertEquals(3, result.get('a'));
        assertEquals(2, result.get('b'));
        assertEquals(1, result.get('c'));
    }

    @Test
    public void testToMultiset_WithSupplier() {
        CharStream stream = createCharStream('a', 'b', 'a');
        com.landawn.abacus.util.Multiset<Character> result = stream.toMultiset(com.landawn.abacus.util.Multiset::new);
        assertEquals(2, result.get('a'));
        assertEquals(1, result.get('b'));
    }

    @Test
    public void testIfEmpty() {
        CharStream stream = CharStream.empty();
        AtomicBoolean called = new AtomicBoolean(false);
        CharStream result = stream.ifEmpty(() -> called.set(true));
        result.count(); // terminal op to trigger
        assertTrue(called.get());
    }

    @Test
    public void testIfEmpty_NonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        AtomicBoolean called = new AtomicBoolean(false);
        char[] result = stream.ifEmpty(() -> called.set(true)).toArray();
        assertFalse(called.get());
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testPrintln() {
        CharStream stream = createCharStream('a', 'b', 'c');
        assertDoesNotThrow(() -> stream.println());
    }

    @Test
    public void testPrintln_EmptyStream() {
        assertDoesNotThrow(() -> CharStream.empty().println());
    }

    @Test
    public void testThrowIfEmpty_NonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.throwIfEmpty().toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testThrowIfEmpty_Empty() {
        CharStream stream = CharStream.empty();
        assertThrows(NoSuchElementException.class, () -> stream.throwIfEmpty().toArray());
    }

    @Test
    public void testThrowIfEmpty_WithCustomException() {
        CharStream stream = CharStream.empty();
        assertThrows(IllegalStateException.class, () -> stream.throwIfEmpty(() -> new IllegalStateException("custom")).toArray());
    }

    @Test
    public void testSps_WithMaxThreadNum() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        char[] result = stream.sps(2, s -> s.filter(c -> c > 'b')).toArray();
        assertHaveSameElements(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testPsp() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd');
        char[] result = stream.psp(s -> s.filter(c -> c > 'b')).toArray();
        Arrays.sort(result);
        assertArrayEquals(new char[] { 'c', 'd' }, result);
    }

    @Test
    public void testParallel_WithMaxThreadNum() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(2);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithExecutor() {
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testParallel_WithMaxThreadNumAndExecutor() {
        java.util.concurrent.Executor executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream result = stream.parallel(2, executor);
        assertNotNull(result);
        assertTrue(result.isParallel());
    }

    @Test
    public void testLimitWithOffset_ZeroOffset() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.limit(0, 2).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testSortedBy() {
        CharStream stream = createCharStream('c', 'a', 'b');
        List<Character> result = stream.boxed().sorted((a, b) -> Character.compare(b, a)).toList();
        assertEquals(Character.valueOf('c'), result.get(0));
        assertEquals(Character.valueOf('b'), result.get(1));
        assertEquals(Character.valueOf('a'), result.get(2));
    }

    @Test
    public void testShuffled_WithRandom() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        char[] result = stream.shuffled(new Random(42)).toArray();
        assertEquals(5, result.length);
        // Verify all elements are present
        Arrays.sort(result);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testStep_StepOne() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.step(1).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testRateLimited_WithRateLimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        RateLimiter rateLimiter = RateLimiter.create(1000);
        char[] result = stream.rateLimited(rateLimiter).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, result);
    }

    @Test
    public void testDelay_WithDuration() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.delay(Duration.ofMillis(1)).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnEmpty() {
        CharStream stream = CharStream.empty();
        char[] result = stream.defaultIfEmpty(() -> CharStream.of('x')).toArray();
        assertArrayEquals(new char[] { 'x' }, result);
    }

    @Test
    public void testDefaultIfEmpty_OnNonEmpty() {
        CharStream stream = createCharStream('a', 'b');
        char[] result = stream.defaultIfEmpty(() -> CharStream.of('x')).toArray();
        assertArrayEquals(new char[] { 'a', 'b' }, result);
    }

    @Test
    public void testAppendIfEmpty_WithSupplier() {
        CharStream stream = CharStream.empty();
        char[] result = stream.appendIfEmpty(() -> CharStream.of('x', 'y')).toArray();
        assertArrayEquals(new char[] { 'x', 'y' }, result);
    }

    @Test
    public void testAppendIfEmpty_WithSupplier_NonEmpty() {
        CharStream stream = createCharStream('a');
        char[] result = stream.appendIfEmpty(() -> CharStream.of('x', 'y')).toArray();
        assertArrayEquals(new char[] { 'a' }, result);
    }

    @Test
    public void testFindFirst_NoPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findFirst();
        assertTrue(result.isPresent());
        assertEquals('a', result.get());
    }

    @Test
    public void testFindFirst_NoPredicate_Empty() {
        OptionalChar result = CharStream.empty().findFirst();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindAny_NoPredicate() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findAny();
        assertTrue(result.isPresent());
    }

    @Test
    public void testFindAny_NoPredicate_Empty() {
        OptionalChar result = CharStream.empty().findAny();
        assertFalse(result.isPresent());
    }

    @Test
    public void testFindLast_NoMatch() {
        CharStream stream = createCharStream('a', 'b', 'c');
        OptionalChar result = stream.findLast(c -> c > 'z');
        assertFalse(result.isPresent());
    }

    @Test
    public void testKthLargest_SecondLargest() {
        CharStream stream = createCharStream('a', 'c', 'e', 'b', 'd');
        OptionalChar result = stream.kthLargest(2);
        assertTrue(result.isPresent());
        assertEquals('d', result.get());
    }

    @Test
    public void testSummaryStatistics() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharSummaryStatistics stats = stream.summaryStatistics();
        assertEquals(3, stats.getCount());
        assertEquals('a', stats.getMin());
        assertEquals('c', stats.getMax());
    }

    @Test
    public void testSummaryStatisticsAndPercentiles() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e');
        Pair<CharSummaryStatistics, Optional<Map<Percentage, Character>>> result = stream.summaryStatisticsAndPercentiles();
        assertNotNull(result);
        assertEquals(5, result.left().getCount());
    }

    @Test
    public void testZipWith_WithDefaults() {
        CharStream a = createCharStream('a', 'b', 'c');
        CharStream b = CharStream.of('x');
        char[] result = a.zipWith(b, '0', '0', (x, y) -> y).toArray();
        assertArrayEquals(new char[] { 'x', '0', '0' }, result);
    }

    @Test
    public void testOnClose_MultipleHandlers() {
        CharStream stream = createCharStream('a', 'b');
        List<String> actions = new ArrayList<>();
        stream.onClose(() -> actions.add("first")).onClose(() -> actions.add("second"));
        stream.close();
        assertEquals(2, actions.size());
        assertEquals("first", actions.get(0));
        assertEquals("second", actions.get(1));
    }

    @Test
    public void testStreamReuseThrowsException() {
        CharStream stream = createCharStream('a', 'b', 'c');
        stream.toArray(); // consume
        assertThrows(IllegalStateException.class, () -> stream.toArray());
    }

}
