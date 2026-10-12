package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTernaryOperator;
import com.landawn.abacus.util.u.OptionalChar;

/** CharStream combination contracts. */
public class CharStreamCombinationTest extends CharStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        CharStream stream = createCharStream('a', 'b').append('c');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
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
    public void testAppend_Stream() {
        CharStream stream = createCharStream('a', 'b', 'c');
        CharStream toAppend = createCharStream('d', 'e');
        char[] result = stream.append(toAppend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testAppend_VarArgs() {
        CharStream stream = createCharStream('a', 'b', 'c');
        char[] result = stream.append('d', 'e').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    // appendIfEmpty

    @Test
    public void testAppendIfEmpty() {
        assertThat(CharStream.empty().appendIfEmpty('x').toArray()).containsExactly('x');
        assertThat(createCharStream('a').appendIfEmpty('x').toArray()).containsExactly('a');
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
    public void testAppendIfEmptyWithSupplierOnEmpty() {
        char[] result = CharStream.empty().appendIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'z' }, result);
    }

    @Test
    public void testAppendIfEmptyWithSupplierOnNonEmpty() {
        char[] result = createCharStream('a').appendIfEmpty(() -> CharStream.of('z')).toArray();
        assertArrayEquals(new char[] { 'a' }, result);
    }

    // concat

    @Test
    public void testConcat_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('c', 'd'));
        CharStream stream = CharStream.concat(streams);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    @Test
    public void testConcat_ListOfArrays() {
        List<char[]> list = Arrays.asList(new char[] { 'a', 'b' }, new char[] { 'c', 'd' });
        CharStream stream = CharStream.concat(list);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
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
    public void testConcatArrays() {
        char[] a1 = { 'a', 'b' };
        char[] a2 = { 'c', 'd' };
        CharStream stream = CharStream.concat(a1, a2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
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
    public void testConcatCollection() {
        List<CharStream> streams = Arrays.asList(createCharStream('a', 'b'), createCharStream('c', 'd'));
        CharStream stream = CharStream.concat(streams);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testConcatCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'o', 'n', 'e' }), createCharStream(new char[] { 't', 'w', 'o' }));
        List<Character> result = CharStream.concat(streams).boxed().toList();
        assertEquals(Arrays.asList('o', 'n', 'e', 't', 'w', 'o'), result);
    }

    @Test
    public void testConcatCollectionSnapshotsSources() {
        List<CharStream> sources = new ArrayList<>(Arrays.asList(CharStream.of('a', 'b'), CharStream.of('c', 'd')));
        CharStream concatenated = CharStream.concat(sources);

        sources.clear();

        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, concatenated.toArray());
    }

    @Test
    public void testConcatEmptyArrays() {
        CharStream stream = CharStream.concat(new char[0], new char[0]);
        assertEquals(0, stream.count());
    }



    // G112-01: bulk count()/toCharList()/toArray() of concat(List<char[]>) / concat(char[]...) - null/empty arrays, skip into/at/after a
    // segment, short (element-wise) and long (bulk copy) segments, fresh results, lazy read of the list, same list traversal calls
    @Test
    public void testConcatListOfArrays_bulkOpsMatchElementwise_G112() {
        final char[] a = new char[20];
        final char[] b = new char[40];
        final char[] c = { (char) 1, (char) 2, (char) 3 };
        for (int i = 0; i < a.length; i++) {
            a[i] = (char) (10 + i);
        }
        for (int i = 0; i < b.length; i++) {
            b[i] = (char) (100 + i);
        }
        final List<char[]> list = Arrays.asList(null, a, null, new char[0], c, b, c, new char[0]);
        final char[] all = new char[66];
        System.arraycopy(a, 0, all, 0, 20);
        System.arraycopy(c, 0, all, 20, 3);
        System.arraycopy(b, 0, all, 23, 40);
        System.arraycopy(c, 0, all, 63, 3);

        for (final int n : new int[] { 0, 1, 5, 19, 20, 21, 22, 23, 24, 40, 62, 63, 64, 65, 66, 70 }) {
            final char[] expected = Arrays.copyOfRange(all, Math.min(n, all.length), all.length);
            assertArrayEquals(expected, CharStream.concat(list).skip(n).toArray(), "skip " + n);
            assertEquals(expected.length, CharStream.concat(list).skip(n).count(), "skip " + n);
            final CharList xl = CharStream.concat(list).skip(n).toCharList();
            assertArrayEquals(expected, xl.toArray(), "skip " + n);
            xl.add((char) 7);
            assertEquals(expected.length + 1, xl.size());
        }

        assertArrayEquals(all, CharStream.concat(a, c, b, c).toArray());
        assertEquals(66, CharStream.concat(a, null, c, b, c).count());

        final List<char[]> empties = Arrays.asList(null, new char[0]);
        assertEquals(0, CharStream.concat(empties).toArray().length);
        assertEquals(0, CharStream.concat(empties).count());
        final CharList emptyList = CharStream.concat(empties).toCharList();
        emptyList.add((char) 9);
        assertEquals(1, emptyList.size());

        final char[] result = CharStream.concat(Arrays.asList(b)).toArray();
        assertArrayEquals(b, result);
        assertFalse(result == b);
        result[0] = (char) 0;
        assertEquals((char) 100, b[0]);

        // the list is read when the terminal operation runs; close handlers still run
        final List<char[]> live = new ArrayList<>(Arrays.asList(new char[] { (char) 1 }, new char[] { (char) 2 }));
        final CharStream stream = CharStream.concat(live);
        live.set(1, b);
        final AtomicInteger closed = new AtomicInteger();
        assertEquals(41, stream.onClose(closed::incrementAndGet).count());
        assertEquals(1, closed.get());

        // partially consumed iterator, then drained in bulk
        final CharIterator iter = CharStream.concat(list).iterator();
        assertEquals((char) 10, iter.nextChar());
        assertArrayEquals(Arrays.copyOfRange(all, 1, all.length), iter.toArray());
        assertFalse(iter.hasNext());

        // the list iterator is advanced exactly as element-by-element iteration advances it
        final int[] calls = new int[2];
        final List<char[]> counting = new ArrayList<>(list) {
            private static final long serialVersionUID = 1L;

            @Override
            public java.util.Iterator<char[]> iterator() {
                final java.util.Iterator<char[]> it = super.iterator();

                return new java.util.Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        calls[0]++;
                        return it.hasNext();
                    }

                    @Override
                    public char[] next() {
                        calls[1]++;
                        return it.next();
                    }
                };
            }
        };

        assertArrayEquals(all, CharStream.concat(counting).toArray());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, CharStream.concat(counting).count());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
        calls[0] = calls[1] = 0;
        assertEquals(66, CharStream.concat(counting).toCharList().size());
        assertEquals(9, calls[0]);
        assertEquals(8, calls[1]);
    }

    @Test
    public void testConcatStreams() {
        CharStream s1 = createCharStream('a', 'b');
        CharStream s2 = createCharStream('c', 'd');
        CharStream stream = CharStream.concat(s1, s2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testConcatIterators() {
        Collection<CharIterator> iterators = Arrays.asList(CharIterator.of('a', 'b'), CharIterator.of('c', 'd'));
        CharStream stream = CharStream.concatIterators(iterators);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd' }, stream.toArray());
    }

    // join

    @Test
    public void testJoin() {
        String result = createCharStream('a', 'b', 'c').join(", ");
        assertThat(result).isEqualTo("a, b, c");
    }

    @Test
    public void testJoin_EmptyDelimiter() {
        CharStream stream = createCharStream('a', 'b', 'c');
        String result = stream.join("", "", "");
        assertEquals("abc", result);
    }

    @Test
    public void testJoin_WithDelimiterPrefixSuffix() {
        CharStream stream = createCharStream('a', 'b', 'c');
        String result = stream.join(", ", "[", "]");
        assertEquals("[a, b, c]", result);
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
    public void testJoinWithDelimiterOnly() {
        String result = createCharStream('a', 'b', 'c').join(", ");
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
        assertTrue(result.contains("c"));
    }

    @Test
    public void testJoinWithPrefixSuffix() {
        String result = createCharStream('a', 'b', 'c').join(", ", "[", "]");
        assertThat(result).isEqualTo("[a, b, c]");
    }

    @Test
    public void testJoinTo() {
        CharStream stream = createCharStream('a', 'b', 'c');
        Joiner joiner = Joiner.with(", ");
        Joiner result = stream.joinTo(joiner);
        assertEquals("a, b, c", result.toString());
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

    // merge

    @Test
    public void testMerge_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'd'), CharStream.of('b', 'e'), CharStream.of('c', 'f'));
        CharStream stream = CharStream.merge(streams, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
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
    public void testMerge_ThreeIterators() {
        CharIterator it1 = CharIterator.of('a', 'd');
        CharIterator it2 = CharIterator.of('b', 'e');
        CharIterator it3 = CharIterator.of('c', 'f');
        CharStream stream = CharStream.merge(it1, it2, it3, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
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
    public void testMerge_TwoArrays() {
        char[] a = { 'a', 'c', 'e' };
        char[] b = { 'b', 'd', 'f' };
        CharStream stream = CharStream.merge(a, b, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
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
    public void testMerge_TwoStreams() {
        CharStream s1 = CharStream.of('a', 'c', 'e');
        CharStream s2 = CharStream.of('b', 'd', 'f');
        CharStream stream = CharStream.merge(s1, s2, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, stream.toArray());
    }

    @Test
    public void testMergeArrays() {
        char[] a = { 'a', 'c', 'e' };
        char[] b = { 'b', 'd', 'f' };
        CharStream stream = CharStream.merge(a, b, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd', 'e', 'f');
    }

    @Test
    public void testMergeCollectionForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'e'), CharStream.of('b', 'f'), CharStream.of('c', 'g'));
        CharStream result = CharStream.merge(streams, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    public void testMergeCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { '1', '5' }), createCharStream(new char[] { '2', '6' }),
                createCharStream(new char[] { '3', '4' }));
        List<Character> result = CharStream.merge(streams, (c1, c2) -> c1 < c2 ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).boxed().toList();
        assertEquals(Arrays.asList('1', '2', '3', '4', '5', '6'), result);
    }

    @Test
    public void testMergeCollectionTreatsNullStreamAsEmpty() {
        assertEquals(0, CharStream.merge(Arrays.asList((CharStream) null), (a, b) -> MergeResult.TAKE_FIRST).count());
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
    public void testMergeWith() {
        CharStream streamA = createCharStream('a', 'c', 'e');
        CharStream streamB = createCharStream('b', 'd', 'f');
        CharBiFunction<MergeResult> selector = (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
        char[] result = streamA.mergeWith(streamB, selector).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    @Test
    public void testMergeWithDedicated() {
        CharStream a = createCharStream('a', 'c', 'e');
        CharStream b = CharStream.of('b', 'd', 'f');
        char[] result = a.mergeWith(b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e', 'f' }, result);
    }

    // prepend

    @Test
    public void testPrepend() {
        CharStream stream = createCharStream('b', 'c').prepend('a');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
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
    public void testPrepend_Stream() {
        CharStream stream = createCharStream('c', 'd', 'e');
        CharStream toPrePend = createCharStream('a', 'b');
        char[] result = stream.prepend(toPrePend).toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    @Test
    public void testPrepend_VarArgs() {
        CharStream stream = createCharStream('c', 'd', 'e');
        char[] result = stream.prepend('a', 'b').toArray();
        assertArrayEquals(new char[] { 'a', 'b', 'c', 'd', 'e' }, result);
    }

    // zip

    @Test
    public void testZip_CollectionOfStreams() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('x', 'y'));
        CharNFunction<Character> zipFunction = chars -> chars[0];
        CharStream stream = CharStream.zip(streams, zipFunction);
        assertArrayEquals(new char[] { 'a', 'b' }, stream.toArray());
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
    public void testZip_ThreeArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        char[] c = { '1', '2', '3' };
        CharStream stream = CharStream.zip(a, b, c, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
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
    public void testZip_ThreeIterators() {
        CharIterator it1 = CharIterator.of('a', 'b', 'c');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharIterator it3 = CharIterator.of('1', '2', '3');
        CharStream stream = CharStream.zip(it1, it2, it3, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
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
    public void testZip_ThreeStreams() {
        CharStream s1 = CharStream.of('a', 'b', 'c');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream s3 = CharStream.of('1', '2', '3');
        CharStream stream = CharStream.zip(s1, s2, s3, (c1, c2, c3) -> c3);
        assertArrayEquals(new char[] { '1', '2', '3' }, stream.toArray());
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
    public void testZip_TwoArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_TwoArraysWithDefaultValues() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZip_TwoIterators() {
        CharIterator it1 = CharIterator.of('a', 'b', 'c');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(it1, it2, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_TwoIteratorsWithDefaultValues() {
        CharIterator it1 = CharIterator.of('a', 'b');
        CharIterator it2 = CharIterator.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(it1, it2, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZip_TwoStreams() {
        CharStream s1 = CharStream.of('a', 'b', 'c');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, (c1, c2) -> (char) Math.max(c1, c2));
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, stream.toArray());
    }

    @Test
    public void testZip_TwoStreamsWithDefaultValues() {
        CharStream s1 = CharStream.of('a', 'b');
        CharStream s2 = CharStream.of('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, '0', '9', (c1, c2) -> c1);
        assertArrayEquals(new char[] { 'a', 'b', '0' }, stream.toArray());
    }

    @Test
    public void testZipArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, (c1, c2) -> (char) (c1 + c2 - 'd'));
        assertThat(stream.toArray()).containsExactly('u', 'w', 'y');
    }

    @Test
    public void testZipCollectionOfCharStreams() {
        Collection<CharStream> streams = Arrays.asList(createCharStream(new char[] { 'A', 'B' }), createCharStream(new char[] { '1', '2', '3' }));
        List<Character> result = CharStream.zip(streams, chars -> (char) (chars[0] + chars[1])).boxed().toList();
        assertEquals(Arrays.asList((char) ('A' + '1'), (char) ('B' + '2')), result);
    }

    @Test
    public void testZipCollectionWithDefaultsForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y'));
        char[] defaults = { '0', '9' };
        CharStream result = CharStream.zip(streams, defaults, chars -> chars[1]);
        assertEquals(3, result.count());
    }

    @Test
    public void testZipStreams() {
        CharStream s1 = createCharStream('a', 'b', 'c');
        CharStream s2 = createCharStream('x', 'y', 'z');
        CharStream stream = CharStream.zip(s1, s2, (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
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



    // Additional coverage improvement tests - Collection-based factory methods
    @Test
    public void testZipWithCollectionOfStreamsForCoverage() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b'), CharStream.of('x', 'y'), CharStream.of('1', '2'));
        CharStream result = CharStream.zip(streams, chars -> chars[0]);
        assertArrayEquals(new char[] { 'a', 'b' }, result.toArray());
    }

    @Test
    public void testZipWithDefaults() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };
        CharStream stream = CharStream.zip(a, b, '0', '1', (c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
    }

    // zipWith

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
    public void testZipWith_TwoStreams() {
        CharStream streamA = createCharStream('a', 'b', 'c');
        CharStream streamB = createCharStream('x', 'y', 'z');
        CharBinaryOperator zipFunction = (a, b) -> (char) Math.max(a, b);
        char[] result = streamA.zipWith(streamB, zipFunction).toArray();
        assertArrayEquals(new char[] { 'x', 'y', 'z' }, result);
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
    public void testZipWith_WithDefaults() {
        CharStream a = createCharStream('a', 'b', 'c');
        CharStream b = CharStream.of('x');
        char[] result = a.zipWith(b, '0', '0', (x, y) -> y).toArray();
        assertArrayEquals(new char[] { 'x', '0', '0' }, result);
    }

    @Test
    public void testZipWithThreeStreamsDedicated() {
        CharStream a = createCharStream('a', 'b');
        CharStream b = CharStream.of('x', 'y');
        CharStream c = CharStream.of('1');
        char[] result = a.zipWith(b, c, (x, y, z) -> (char) (x + y + z - 'a' - 'x')).toArray();
        assertEquals(1, result.length);
    }

}
