package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharSupplier;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;

/** CharStream creation contracts. */
public class CharStreamCreationTest extends CharStreamTestSupport {

    @Test
    public void testOfCharSequenceRange_countExhaustsIterator() {
        // regression: the CharSequence-range iterator's count() returned the remaining count
        // without advancing the cursor, so the iterator was not exhausted afterwards as the
        // CharIteratorEx.count() contract requires (sole outlier of the count() overrides).
        final CharIteratorEx iter = ((IteratorCharStream) CharStream.of("Hello", 1, 4)).iteratorEx();
        org.junit.jupiter.api.Assertions.assertEquals(3, iter.count());
        org.junit.jupiter.api.Assertions.assertFalse(iter.hasNext());
    }

    @Test
    public void testDefer() {
        CharStream stream = CharStream.defer(() -> CharStream.of('a', 'b', 'c'));
        assertNotNull(stream);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testDeferRejectsNullSupplier() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.defer(null));
    }

    // empty

    @Test
    public void testEmpty() {
        CharStream stream = CharStream.empty();
        assertNotNull(stream);
        assertEquals(0, stream.count());
    }

    @Test
    public void testEmptyStreamAverageDedicated() {
        assertFalse(CharStream.empty().average().isPresent());
    }

    @Test
    public void testEmptyStreamMaxDedicated() {
        assertFalse(CharStream.empty().max().isPresent());
    }

    @Test
    public void testEmptyStreamMinDedicated() {
        assertFalse(CharStream.empty().min().isPresent());
    }

    @Test
    public void testEmptyStreamNextElement() {
        assertThrows(NoSuchElementException.class, () -> CharStream.empty().iterator().nextChar());
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
    public void testEmptyStreamReduceDedicated() {
        assertFalse(CharStream.empty().reduce((a, b) -> (char) (a + b)).isPresent());
    }

    @Test
    public void testEmptyStreamSumDedicated() {
        assertEquals(0, CharStream.empty().sum());
    }

    @Test
    public void testEmptyStreamSummaryStatisticsDedicated() {
        CharSummaryStatistics stats = CharStream.empty().summaryStatistics();
        assertNotNull(stats);
        assertEquals(0, stats.getCount());
    }

    // flatten

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
    public void testFlatten_2DArray() {
        char[][] arrays = { { 'a', 'b' }, { 'c', 'd' }, { 'e' } };
        CharStream stream = CharStream.flatten(arrays);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayHorizontally() {
        char[][] arrays = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        CharStream stream = CharStream.flatten(arrays, false);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testFlatten_2DArrayVertically() {
        char[][] arrays = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        CharStream stream = CharStream.flatten(arrays, true);
        assertArrayEquals(new char[] { 'a', 'd', 'b', 'e', 'c', 'f' }, stream.toArray());
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



    // G114-01: flatten(char[][], true) keeps the original walk for dense input and uses a compacted row walk when
    // rows * longest row > 4 * elements; pins order, count, skip and the iterator (incl. NoSuchElementException) on both walks.
    @Test
    public void testFlattenVertically_denseAndSparseWalksMatchColumnMajorReference() {
        final Random random = new Random(114);

        for (int round = 0; round < 400; round++) {
            // kind 0: dense jagged, 1: mostly null/empty rows plus a few long ones, 2: rectangular, 3: one long row among empty/null rows
            final int kind = round % 4;
            final int rows = 2 + random.nextInt(kind == 1 || kind == 3 ? 40 : 8);
            final int width = 1 + random.nextInt(6);
            final int longRow = random.nextInt(rows);
            final char[][] a = new char[rows][];

            for (int i = 0; i < rows; i++) {
                final int len;

                if (kind == 0) {
                    len = random.nextInt(7) - 1;
                } else if (kind == 1) {
                    len = random.nextInt(6) == 0 ? random.nextInt(40) : random.nextInt(3) - 1;
                } else if (kind == 2) {
                    len = width;
                } else {
                    len = i == longRow ? 1 + random.nextInt(60) : random.nextInt(2) - 1;
                }

                if (len >= 0) {
                    a[i] = new char[len];

                    for (int j = 0; j < len; j++) {
                        a[i][j] = (char) (i * 100 + j);
                    }
                }
            }

            assertFlattenVerticallyMatchesReferenceG114(a, random.nextInt(rows * 3 + 2));
        }

        // rows * longest row == 4 * elements keeps the original walk; one more empty row switches to the compacted walk
        final char[][] boundary = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {} };
        final char[][] boundaryPlusOne = { { 1, 2, 3, 4 }, null, {}, { 5, 6, 7, 8 }, null, {}, {}, {}, {} };
        assertArrayEquals(new char[] { 1, 5, 2, 6, 3, 7, 4, 8 }, CharStream.flatten(boundary, true).toArray());
        assertArrayEquals(new char[] { 1, 5, 2, 6, 3, 7, 4, 8 }, CharStream.flatten(boundaryPlusOne, true).toArray());

        for (int skip = 0; skip <= 9; skip++) {
            assertFlattenVerticallyMatchesReferenceG114(boundary, skip);
            assertFlattenVerticallyMatchesReferenceG114(boundaryPlusOne, skip);
        }

        // sparse: rows drop out of the compacted walk at different columns
        final char[][] sparse = { null, {}, { 1, 2, 3, 4, 5, 6, 7, 8, 9 }, {}, {}, { 10, 11 }, {}, null, {}, {}, { 12, 13, 14, 15 }, {} };
        assertArrayEquals(new char[] { 1, 10, 12, 2, 11, 13, 3, 14, 4, 15, 5, 6, 7, 8, 9 }, CharStream.flatten(sparse, true).toArray());
        assertFlattenVerticallyMatchesReferenceG114(sparse, 4);

        // one long row among 999 null rows (first and last position)
        final char[][] oneLongRow = new char[1000][];
        oneLongRow[999] = new char[5000];

        for (int j = 0; j < 5000; j++) {
            oneLongRow[999][j] = (char) j;
        }

        assertArrayEquals(oneLongRow[999], CharStream.flatten(oneLongRow, true).toArray());
        oneLongRow[0] = oneLongRow[999];
        oneLongRow[999] = null;
        assertFlattenVerticallyMatchesReferenceG114(oneLongRow, 4999);
        assertEquals(0, CharStream.flatten(new char[][] { null, {}, null, {} }, true).count());
    }



    // G091-01: iterator exhaustion and a strongly jagged input (one long row, many single-element and null rows)
    @Test
    public void testFlattenVertically_iteratorExhaustionAndLongRow() {
        final char[][] a = { null, { 'a' }, {}, { 'b', 'c', 'd' }, null, { 'e', 'f' } };
        final CharIterator iter = CharStream.flatten(a, true).iterator();
        final StringBuilder sb = new StringBuilder();

        while (iter.hasNext()) {
            sb.append(iter.nextChar());
        }

        assertEquals("abecfd", sb.toString());
        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextChar);
        assertFalse(iter.hasNext());

        final char[][] jagged = new char[201][];

        for (int i = 0; i < 200; i++) {
            jagged[i] = i % 3 == 0 ? null : new char[] { (char) ('A' + i % 26) };
        }

        jagged[200] = new char[5000];

        for (int i = 0; i < 5000; i++) {
            jagged[200][i] = (char) ('0' + i % 10);
        }

        final char[] result = CharStream.flatten(jagged, true).toArray();
        final CharList expected = new CharList();

        for (int i = 0; i < 200; i++) {
            if (jagged[i] != null) {
                expected.add(jagged[i][0]);
            }
        }

        expected.add(jagged[200][0]);

        for (int i = 1; i < 5000; i++) {
            expected.add(jagged[200][i]);
        }

        assertArrayEquals(expected.toArray(), result);
        assertArrayEquals(new char[0], CharStream.flatten(new char[][] { null, {}, null }, true).toArray());
    }



    // G091-01: pins the column-major order of flatten(char[][], true) for jagged input with null/empty/short rows
    @Test
    public void testFlattenVertically_jaggedNullEmptyRowsMatchesColumnMajorReference() {
        final Random random = new Random(20260926L);

        for (int round = 0; round < 300; round++) {
            final int rows = 2 + random.nextInt(8);
            final char[][] a = new char[rows][];

            for (int r = 0; r < rows; r++) {
                final int kind = random.nextInt(6);
                a[r] = kind == 0 ? null : new char[kind == 1 ? 0 : random.nextInt(7)];

                if (a[r] != null) {
                    for (int c = 0; c < a[r].length; c++) {
                        a[r][c] = (char) ('a' + random.nextInt(26));
                    }
                }
            }

            int maxLen = 0;

            for (final char[] row : a) {
                maxLen = Math.max(maxLen, row == null ? 0 : row.length);
            }

            final CharList expected = new CharList();

            for (int c = 0; c < maxLen; c++) {
                for (final char[] row : a) {
                    if (row != null && c < row.length) {
                        expected.add(row[c]);
                    }
                }
            }

            assertArrayEquals(expected.toArray(), CharStream.flatten(a, true).toArray());
            assertEquals(expected.size(), CharStream.flatten(a, true).count());

            if (expected.size() > 1) {
                assertArrayEquals(expected.copy(1, expected.size()).toArray(), CharStream.flatten(a, true).skip(1).toArray());
            }
        }
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
    public void testFlattmapToObj_WithArray() {
        CharStream stream = createCharStream('a', 'b');
        CharFunction<String[]> mapper = c -> new String[] { String.valueOf(c), String.valueOf(Character.toUpperCase(c)) };
        List<String> result = stream.flatMapArrayToObj(mapper).toList();
        assertEquals(Arrays.asList("a", "A", "b", "B"), result);
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
        assertThrows(IllegalArgumentException.class, () -> CharStream.generate(null).limit(1).count());
    }

    // iterate

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
    public void testIterate_Infinite() {
        CharStream stream = CharStream.iterate('a', c -> (char) (c + 1));
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.limit(5).toArray());
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
    public void testIterateBooleanSupplierCharSupplierNull() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.iterate(null, () -> 'a').count());
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
    public void testIterateWithHasNextAndNext() {
        AtomicInteger counter = new AtomicInteger(0);
        CharStream stream = CharStream.iterate(() -> counter.get() < 3, () -> (char) ('a' + counter.getAndIncrement()));
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testIterateWithImmediateFalseHasNextForCoverage() {
        CharStream stream = CharStream.iterate('a', () -> false, c -> (char) (c + 1));
        assertEquals(0, stream.count());
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
    public void testOf_CharIterator() {
        CharList list = CharList.of('a', 'b', 'c');
        CharStream stream = CharStream.of(list.iterator());
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_ArrayWithRange() {
        CharStream stream = CharStream.of(new char[] { 'a', 'b', 'c', 'd', 'e' }, 1, 4);
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
    public void testOf_CharBuffer() {
        CharBuffer buffer = CharBuffer.wrap(new char[] { 'a', 'b', 'c' });
        CharStream stream = CharStream.of(buffer);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_CharSequence() {
        CharStream stream = CharStream.of("abcde");
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, stream.toArray());
    }

    @Test
    public void testOf_CharSequenceIteratorRejectsAccessAfterExhaustion() {
        CharIteratorEx iter = CharStream.of("abc", 1, 2).iteratorEx();

        assertEquals('b', iter.nextChar());
        assertThrows(NoSuchElementException.class, iter::nextChar);
    }

    @Test
    public void testOf_CharSequenceWithRange() {
        CharStream stream = CharStream.of("abcde", 1, 4);
        assertArrayEquals(new char[] { 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testOf_Collection() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        CharStream stream = CharStream.of(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_EmptyArray() {
        CharStream stream = CharStream.of(new char[0]);
        assertEquals(0, stream.count());
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
    public void testOf_Reader() {
        Reader reader = new StringReader("abc");
        CharStream stream = CharStream.of(reader);
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOf_VarArgs() {
        CharStream stream = CharStream.of('a', 'b', 'c');
        assertArrayEquals(new char[] { 'a', 'b', 'c' }, stream.toArray());
    }

    @Test
    public void testOfChar() {
        CharStream stream = createCharStream('x');
        assertThat(stream.toArray()).containsExactly('x');
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
    public void testOfCharArray() {
        CharStream stream1 = createCharStream('a', 'b', 'c');
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());

        CharStream stream2 = createCharStream();
        assertTrue(stream2.boxed().toList().isEmpty());
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharStream stream = createCharStream(array, 1, 4);
        assertThat(stream.toArray()).containsExactly('b', 'c', 'd');
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
    public void testOfCharSequence() {
        CharStream stream1 = createCharStream("hello");
        assertEquals(Arrays.asList('h', 'e', 'l', 'l', 'o'), stream1.boxed().toList());

        CharStream stream2 = createCharStream("");
        assertTrue(stream2.boxed().toList().isEmpty());
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
    public void testOfNullable_Null() {
        CharStream stream = CharStream.ofNullable(null);
        assertEquals(0, stream.count());
    }

    @Test
    public void testOfNullable_Present() {
        CharStream stream = CharStream.ofNullable('a');
        assertArrayEquals(new char[] { 'a' }, stream.toArray());
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
    public void testOfNullReader() {
        CharStream stream = CharStream.of((Reader) null);
        assertEquals(0, stream.count());
    }

    // random

    @Test
    public void testRandom_NoArgs() {
        CharStream stream = CharStream.random();
        assertNotNull(stream);
        assertEquals(10, stream.limit(10).count());
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
    public void testRandom_WithRange() {
        CharStream stream = CharStream.random('a', 'z');
        char[] result = stream.limit(100).toArray();
        for (char c : result) {
            assertTrue(c >= 'a' && c < 'z');
        }
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
    public void testRandomRange() {
        char start = 'a';
        char end = 'z';
        assertEquals(10, CharStream.random(start, end).limit(10).count());
        CharStream.random(start, end).limit(10).forEach(c -> assertTrue(c >= start && c < end));
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
    public void testRandomWithRange() {
        CharStream stream = CharStream.random('a', 'd').limit(100);
        char[] result = stream.toArray();
        assertThat(result).hasSize(100);
        for (char c : result) {
            assertThat(c).isGreaterThanOrEqualTo('a').isLessThan('d');
        }
    }

    // range

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
    public void testRange_EmptyRange() {
        CharStream stream = CharStream.range('e', 'a');
        assertEquals(0, stream.count());
    }

    @Test
    public void testRange_NegativeStep() {
        assertArrayEquals(new char[] {}, CharStream.range('a', 'e', -1).toArray());
    }

    @Test
    public void testRange_ThreeParams() {
        CharStream stream = CharStream.range('a', 'g', 2);
        assertArrayEquals(new char[] { 'a', 'c', 'e' }, stream.toArray());
    }

    @Test
    public void testRange_TwoParams() {
        CharStream stream = CharStream.range('a', 'e');
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testRangeEmptyRange() {
        assertEquals(0, CharStream.range('a', 'a').count());
    }

    @Test
    public void testRangeWithInvalidStepDirectionForCoverage() {
        CharStream stream = CharStream.range('a', 'j', -1);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRangeWithStepReturnsEmptyForEqualBoundsOrMismatchedDirection() {
        assertArrayEquals(new char[0], CharStream.range('a', 'a', 1).toArray());
        assertArrayEquals(new char[0], CharStream.range('a', 'a', -1).toArray());
        assertArrayEquals(new char[0], CharStream.range('a', 'f', -1).toArray());
        assertArrayEquals(new char[0], CharStream.range('f', 'a', 1).toArray());
        assertArrayEquals(new char[] { 'z', 'x', 'v' }, CharStream.range('z', 'u', -2).toArray());
    }

    @Test
    public void testRangeWithZeroStep() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.range('a', 'c', 0));
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
    public void testOfReader() throws IOException {
        Reader reader = new java.io.StringReader("abc");
        CharStream stream1 = createCharStream(reader);
        assertEquals(Arrays.asList('a', 'b', 'c'), stream1.boxed().toList());
        reader.read();
    }

    // repeat

    @Test
    public void testRepeat() {
        CharStream stream = CharStream.repeat('x', 5);
        assertArrayEquals(new char[] { 'x', 'x', 'x', 'x', 'x' }, stream.toArray());
    }

    @Test
    public void testRepeat_NegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('x', -1));
    }

    @Test
    public void testRepeat_ZeroTimes() {
        CharStream stream = CharStream.repeat('x', 0);
        assertEquals(0, stream.count());
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> CharStream.repeat('A', -1));
    }



    // G112-02: of(Reader) count()/toArray()/toCharList() consume whole buffered chunks - same read() calls, same results
    @Test
    public void testOfReader_bulkToArrayCountToList_chunks_G112() {
        final char[] data = g112Chars(20000);

        for (final int chunk : new int[] { 1, 5, 15, 16, 17, 3000, 8192, 10000 }) {
            final int reads = (data.length + Math.min(chunk, 8192) - 1) / Math.min(chunk, 8192) + 1;

            G112ChunkedReader reader = new G112ChunkedReader(data, chunk, -1);
            assertArrayEquals(data, CharStream.of(reader).toArray(), "chunk " + chunk);
            assertEquals(reads, reader.readCalls, "chunk " + chunk);

            reader = new G112ChunkedReader(data, chunk, -1);
            assertEquals(20000, CharStream.of(reader).count(), "chunk " + chunk);
            assertEquals(reads, reader.readCalls, "chunk " + chunk);

            reader = new G112ChunkedReader(data, chunk, -1);
            final CharList list = CharStream.of(reader).toCharList();
            assertEquals(CharList.of(data), list, "chunk " + chunk);
            assertEquals(reads, reader.readCalls, "chunk " + chunk);
            list.add('!');
            assertEquals(20001, list.size());

            // start inside a buffered chunk
            for (final int skip : new int[] { 1, 2, 14, 15, 16, 2999, 4500, 8191, 8192, 8193, 19999, 20000, 20001 }) {
                final char[] expected = Arrays.copyOfRange(data, Math.min(skip, data.length), data.length);
                assertArrayEquals(expected, CharStream.of(new G112ChunkedReader(data, chunk, -1)).skip(skip).toArray(), "chunk " + chunk + " skip " + skip);
                assertEquals(expected.length, CharStream.of(new G112ChunkedReader(data, chunk, -1)).skip(skip).count(), "chunk " + chunk + " skip " + skip);
                assertEquals(CharList.of(expected), CharStream.of(new G112ChunkedReader(data, chunk, -1)).skip(skip).toCharList(),
                        "chunk " + chunk + " skip " + skip);
            }
        }

        final char[] big = g112Chars(3 * 8192 + 17);
        assertArrayEquals(big, CharStream.of(new StringReader(new String(big))).toArray());
        assertEquals(big.length, CharStream.of(new StringReader(new String(big))).count());
        assertArrayEquals(Arrays.copyOfRange(big, 1, big.length), CharStream.of(new StringReader(new String(big))).skip(1).toArray());
        assertEquals(CharList.of(Arrays.copyOfRange(big, 5, big.length)), CharStream.of(new StringReader(new String(big))).skip(5).toCharList());
    }



    // G112-02: empty input, read() == 0, a read failure in a later call, and close handling
    @Test
    public void testOfReader_bulkToArrayCountToList_edgeCases_G112() {
        assertEquals(0, CharStream.of(new StringReader("")).toArray().length);
        assertEquals(0, CharStream.of(new StringReader("")).count());
        final CharList empty = CharStream.of(new StringReader("")).toCharList();
        assertEquals(0, empty.size());
        empty.add('x');
        assertEquals(CharList.of('x'), empty);

        final Reader zeroRead = new Reader() {
            @Override
            public int read(final char[] cbuf, final int off, final int len) {
                return 0;
            }

            @Override
            public void close() {
            }
        };

        assertEquals(0, CharStream.of(zeroRead).toArray().length);
        assertEquals(0, CharStream.of(zeroRead).count());
        assertEquals(0, CharStream.of(zeroRead).toCharList().size());

        final char[] data = g112Chars(10000);

        final G112ChunkedReader r1 = new G112ChunkedReader(data, 3000, 2);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> CharStream.of(r1).toArray());
        assertEquals(3, r1.readCalls);

        final G112ChunkedReader r2 = new G112ChunkedReader(data, 3000, 2);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> CharStream.of(r2).count());
        assertEquals(3, r2.readCalls);

        final G112ChunkedReader r3 = new G112ChunkedReader(data, 3000, 0);
        assertThrows(com.landawn.abacus.exception.UncheckedIOException.class, () -> CharStream.of(r3).toCharList());
        assertEquals(1, r3.readCalls);

        final G112ChunkedReader r4 = new G112ChunkedReader(data, 3000, -1);
        assertArrayEquals(data, CharStream.of(r4, true).toArray());
        assertEquals(1, r4.closeCalls);

        final G112ChunkedReader r5 = new G112ChunkedReader(data, 3000, -1);
        assertEquals(10000, CharStream.of(r5, false).count());
        assertEquals(0, r5.closeCalls);

        // a short-circuiting operation still reads only what it needs
        final G112ChunkedReader r6 = new G112ChunkedReader(data, 3000, -1);
        assertArrayEquals(Arrays.copyOf(data, 10), CharStream.of(r6).limit(10).toArray());
        assertEquals(1, r6.readCalls);
    }

}
