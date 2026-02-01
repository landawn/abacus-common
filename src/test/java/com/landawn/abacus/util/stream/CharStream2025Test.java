package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BooleanSupplier;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.TooManyElementsException;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
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
public class CharStream2025Test extends TestBase {

    private CharStream charStream;

    @BeforeEach
    public void setUp() {
        charStream = createCharStream('a', 'b', 'c', 'd', 'e');
    }

    protected CharStream createCharStream(char... elements) {
        return CharStream.of(elements);
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
        IntStream result = stream.flatMapToInt(c -> IntStream.of((int) c, (int) Character.toUpperCase(c)));
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
        List<String> result = stream.flattmapToObj(mapper).toList();
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
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e')
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals('a', result[0]);
        assertEquals('b', result[1]);
        assertEquals('c', result[2]);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e')
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_EmptyStream() {
        char[] result = CharStream.empty()
                .debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        char[] result = CharStream.of('z')
                .debounce(1, com.landawn.abacus.util.Duration.ofMillis(100))
                .toArray();

        assertEquals(1, result.length);
        assertEquals('z', result[0]);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e')
                .debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

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

        char[] result = CharStream.of(input)
                .debounce(26, com.landawn.abacus.util.Duration.ofSeconds(10))
                .toArray();

        assertEquals(26, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        char[] result = CharStream.of('x', 'y', 'z', 'a', 'b')
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals('x', result[0]);
        assertEquals('y', result[1]);
        assertEquals('z', result[2]);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        char[] result = CharStream.of('a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j')
                .filter(c -> c >= 'b' && c <= 'f')  // b, c, d, e, f
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))  // b, c, d
                .map(c -> Character.toUpperCase(c))  // B, C, D
                .toArray();

        assertEquals(3, result.length);
        assertEquals('B', result[0]);
        assertEquals('C', result[1]);
        assertEquals('D', result[2]);
    }

    @Test
    public void testDebounce_WithSpecialCharacters() {
        char[] result = CharStream.of('\n', '\t', ' ', 'a', 'b')
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertEquals('\n', result[0]);
        assertEquals('\t', result[1]);
        assertEquals(' ', result[2]);
    }

    @Test
    public void testDebounce_FromString() {
        char[] result = CharStream.of("Hello World!")
                .debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(5, result.length);
        assertEquals("Hello", new String(result));
    }
}
