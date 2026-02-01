package com.landawn.abacus.util.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.CharBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.google.common.base.Strings;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.CharList;
import com.landawn.abacus.util.CharSummaryStatistics;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.IndexedChar;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class CharStream100Test extends TestBase {

    private CharStream emptyStream;
    private CharStream singleElementStream;
    private CharStream multiElementStream;

    protected CharStream createCharStream(char... array) {
        return CharStream.of(array);
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

    @BeforeEach
    public void setUp() {
        emptyStream = CharStream.empty();
        singleElementStream = createCharStream('a');
        multiElementStream = createCharStream('a', 'b', 'c', 'd', 'e');
    }

    @AfterEach
    public void tearDown() {
        if (emptyStream != null)
            emptyStream.close();
        if (singleElementStream != null)
            singleElementStream.close();
        if (multiElementStream != null)
            multiElementStream.close();
    }

    @Test
    public void testEmpty() {
        CharStream stream = CharStream.empty();
        assertThat(stream.toArray()).isEmpty();
    }

    @Test
    public void testOfChar() {
        CharStream stream = createCharStream('x');
        assertThat(stream.toArray()).containsExactly('x');
    }

    @Test
    public void testOfCharArray() {
        char[] array = { 'a', 'b', 'c' };
        CharStream stream = createCharStream(array);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testOfCharArrayWithRange() {
        char[] array = { 'a', 'b', 'c', 'd', 'e' };
        CharStream stream = createCharStream(array, 1, 4);
        assertThat(stream.toArray()).containsExactly('b', 'c', 'd');
    }

    @Test
    public void testOfCharSequence() {
        CharStream stream = createCharStream("hello");
        assertThat(stream.toArray()).containsExactly('h', 'e', 'l', 'l', 'o');
    }

    @Test
    public void testOfCharSequenceWithRange() {
        CharStream stream = createCharStream("hello world", 2, 7);
        assertThat(stream.toArray()).containsExactly('l', 'l', 'o', ' ', 'w');
    }

    @Test
    public void testOfCharacterArray() {
        Character[] array = { 'x', 'y', 'z' };
        CharStream stream = createCharStream(array);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
    }

    @Test
    public void testOfCollection() {
        List<Character> list = Arrays.asList('a', 'b', 'c');
        CharStream stream = createCharStream(list);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testOfCharIterator() {
        CharIterator iter = CharIterator.of('a', 'b', 'c');
        CharStream stream = createCharStream(iter);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testOfCharBuffer() {
        CharBuffer buffer = CharBuffer.wrap("test");
        CharStream stream = createCharStream(buffer);
        assertThat(stream.toArray()).containsExactly('t', 'e', 's', 't');
    }

    @Test
    public void testOfFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("abc");
        }

        CharStream stream = createCharStream(tempFile);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testOfReader() {
        StringReader reader = new StringReader("xyz");
        CharStream stream = createCharStream(reader);
        assertThat(stream.toArray()).containsExactly('x', 'y', 'z');
    }

    @Test
    public void testOfNullable() {
        assertThat(CharStream.ofNullable(null).toArray()).isEmpty();
        assertThat(CharStream.ofNullable('a').toArray()).containsExactly('a');
    }

    @Test
    public void testDefer() {
        Supplier<CharStream> supplier = () -> createCharStream('a', 'b', 'c');
        CharStream stream = CharStream.defer(supplier);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testRange() {
        CharStream stream = CharStream.range('a', 'e');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testRangeWithStep() {
        CharStream stream = CharStream.range('a', 'g', 2);
        assertThat(stream.toArray()).containsExactly('a', 'c', 'e');
    }

    @Test
    public void testRangeClosed() {
        CharStream stream = CharStream.rangeClosed('a', 'd');
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testRangeClosedWithStep() {
        CharStream stream = CharStream.rangeClosed('a', 'g', 3);
        assertThat(stream.toArray()).containsExactly('a', 'd', 'g');
    }

    @Test
    public void testRepeat() {
        CharStream stream = CharStream.repeat('x', 3);
        assertThat(stream.toArray()).containsExactly('x', 'x', 'x');
    }

    @Test
    public void testRandom() {
        CharStream stream = CharStream.random().limit(10);
        char[] result = stream.toArray();
        assertThat(result).hasSize(10);
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
    public void testGenerate() {
        AtomicInteger counter = new AtomicInteger(0);
        CharStream stream = CharStream.generate(() -> (char) ('a' + counter.getAndIncrement())).limit(3);
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
    public void testConcatIterators() {
        CharIterator iter1 = CharIterator.of('a', 'b');
        CharIterator iter2 = CharIterator.of('c', 'd');
        CharStream stream = CharStream.concat(iter1, iter2);
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
    public void testFlatten2DArray() {
        char[][] array = { { 'a', 'b' }, { 'c', 'd' } };
        CharStream stream = CharStream.flatten(array);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c', 'd');
    }

    @Test
    public void testFlatten2DArrayVertically() {
        char[][] array = { { 'a', 'b', 'c' }, { 'd', 'e', 'f' } };
        CharStream stream = CharStream.flatten(array, true);
        assertThat(stream.toArray()).containsExactly('a', 'd', 'b', 'e', 'c', 'f');
    }

    @Test
    public void testFlatten3DArray() {
        char[][][] array = { { { 'a', 'b' } }, { { 'c', 'd' } } };
        CharStream stream = CharStream.flatten(array);
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
    public void testMap() {
        CharStream stream = createCharStream('a', 'b', 'c').map(c -> Character.toUpperCase(c));
        assertThat(stream.toArray()).containsExactly('A', 'B', 'C');
    }

    @Test
    public void testMapToInt() {
        IntStream stream = createCharStream('a', 'b', 'c').mapToInt(c -> c - 'a');
        assertThat(stream.toArray()).containsExactly(0, 1, 2);
    }

    @Test
    public void testMapToObj() {
        Stream<String> stream = createCharStream('a', 'b', 'c').mapToObj(String::valueOf);
        assertThat(stream.toList()).containsExactly("a", "b", "c");
    }

    @Test
    public void testFlatMap() {
        CharStream stream = createCharStream('a', 'b').flatMap(c -> createCharStream(c, Character.toUpperCase(c)));
        assertThat(stream.toArray()).containsExactly('a', 'A', 'b', 'B');
    }

    @Test
    public void testFilter() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').filter(c -> c % 2 == 0);
        assertThat(stream.toArray()).containsExactly('b', 'd');
    }

    @Test
    public void testDistinct() {
        CharStream stream = createCharStream('a', 'b', 'a', 'c', 'b').distinct();
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testSorted() {
        CharStream stream = createCharStream('c', 'a', 'b').sorted();
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testReverseSorted() {
        CharStream stream = createCharStream('a', 'c', 'b').reverseSorted();
        assertThat(stream.toArray()).containsExactly('c', 'b', 'a');
    }

    @Test
    public void testSkip() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').skip(2);
        assertThat(stream.toArray()).containsExactly('c', 'd');
    }

    @Test
    public void testLimit() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').limit(2);
        assertThat(stream.toArray()).containsExactly('a', 'b');
    }

    @Test
    public void testPeek() {
        List<Character> peeked = new ArrayList<>();
        CharStream stream = createCharStream('a', 'b', 'c').peek(peeked::add);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
        assertThat(peeked).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testToArray() {
        char[] array = createCharStream('a', 'b', 'c').toArray();
        assertThat(array).containsExactly('a', 'b', 'c');
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
    public void testToCharList() {
        CharList list = createCharStream('a', 'b', 'c').toCharList();
        assertThat(list.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testCount() {
        assertThat(createCharStream('a', 'b', 'c').count()).isEqualTo(3);
        assertThat(CharStream.empty().count()).isEqualTo(0);
    }

    @Test
    public void testMin() {
        assertThat(createCharStream('c', 'a', 'b').min()).isEqualTo(OptionalChar.of('a'));
        assertThat(CharStream.empty().min()).isEqualTo(OptionalChar.empty());
    }

    @Test
    public void testMax() {
        assertThat(createCharStream('a', 'c', 'b').max()).isEqualTo(OptionalChar.of('c'));
        assertThat(CharStream.empty().max()).isEqualTo(OptionalChar.empty());
    }

    @Test
    public void testSum() {
        int sum = createCharStream('a', 'b', 'c').sum();
        assertThat(sum).isEqualTo('a' + 'b' + 'c');
    }

    @Test
    public void testAverage() {
        OptionalDouble avg = createCharStream('a', 'b', 'c').average();
        assertThat(avg.isPresent()).isTrue();
        assertThat(avg.getAsDouble()).isEqualTo(('a' + 'b' + 'c') / 3.0);
    }

    @Test
    public void testFindFirst() {
        assertThat(createCharStream('a', 'b', 'c').first()).isEqualTo(OptionalChar.of('a'));
        assertThat(CharStream.empty().first()).isEqualTo(OptionalChar.empty());
    }

    @Test
    public void testFindLast() {
        assertThat(createCharStream('a', 'b', 'c').last()).isEqualTo(OptionalChar.of('c'));
        assertThat(CharStream.empty().last()).isEqualTo(OptionalChar.empty());
    }

    @Test
    public void testFindAny() {
        OptionalChar result = createCharStream('a', 'b', 'c').first();
        assertThat(result.isPresent()).isTrue();
        assertThat(result.get()).isIn('a', 'b', 'c');
    }

    @Test
    public void testAnyMatch() {
        assertThat(createCharStream('a', 'b', 'c').anyMatch(c -> c == 'b')).isTrue();
        assertThat(createCharStream('a', 'b', 'c').anyMatch(c -> c == 'd')).isFalse();
    }

    @Test
    public void testAllMatch() {
        assertThat(createCharStream('a', 'b', 'c').allMatch(c -> c >= 'a')).isTrue();
        assertThat(createCharStream('a', 'b', 'c').allMatch(c -> c == 'a')).isFalse();
    }

    @Test
    public void testNoneMatch() {
        assertThat(createCharStream('a', 'b', 'c').noneMatch(c -> c == 'd')).isTrue();
        assertThat(createCharStream('a', 'b', 'c').noneMatch(c -> c == 'a')).isFalse();
    }

    @Test
    public void testReduce() {
        char result = createCharStream('a', 'b', 'c').reduce('x', (c1, c2) -> c2);
        assertThat(result).isEqualTo('c');
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
    public void testForEach() {
        List<Character> result = new ArrayList<>();
        createCharStream('a', 'b', 'c').forEach(result::add);
        assertThat(result).containsExactly('a', 'b', 'c');
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
    public void testScan() {
        CharStream stream = createCharStream('a', 'b', 'c').scan((c1, c2) -> c2);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
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
    public void testReversed() {
        CharStream stream = createCharStream('a', 'b', 'c').reversed();
        assertThat(stream.toArray()).containsExactly('c', 'b', 'a');
    }

    @Test
    public void testRotated() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').rotated(2);
        assertThat(stream.toArray()).containsExactly('c', 'd', 'a', 'b');
    }

    @Test
    public void testShuffled() {
        char[] original = { 'a', 'b', 'c', 'd', 'e' };
        CharStream stream = createCharStream(original).shuffled();
        char[] shuffled = stream.toArray();
        assertThat(shuffled).hasSize(5);
        assertThat(shuffled).containsExactlyInAnyOrder('a', 'b', 'c', 'd', 'e');
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
    public void testIndexed() {
        Stream<IndexedChar> stream = createCharStream('a', 'b', 'c').indexed();
        List<IndexedChar> list = stream.toList();
        assertThat(list).hasSize(3);
        assertThat(list.get(0).value()).isEqualTo('a');
        assertThat(list.get(0).index()).isEqualTo(0);
        assertThat(list.get(2).value()).isEqualTo('c');
        assertThat(list.get(2).index()).isEqualTo(2);
    }

    @Test
    public void testIntersection() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').intersection(Arrays.asList('b', 'c', 'e'));
        assertThat(stream.toArray()).containsExactly('b', 'c');
    }

    @Test
    public void testDifference() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').difference(Arrays.asList('b', 'c', 'e'));
        assertThat(stream.toArray()).containsExactly('a', 'd');
    }

    @Test
    public void testSymmetricDifference() {
        CharStream stream = createCharStream('a', 'b', 'c').symmetricDifference(Arrays.asList('b', 'c', 'd'));
        assertThat(stream.toArray()).containsExactlyInAnyOrder('a', 'd');
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
    public void testParallel() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd', 'e').parallel();
        assertThat(stream.isParallel()).isTrue();
        assertThat(stream.toArray()).containsExactlyInAnyOrder('a', 'b', 'c', 'd', 'e');
    }

    @Test
    public void testSequential() {
        CharStream stream = createCharStream('a', 'b', 'c').parallel().sequential();
        assertThat(stream.isParallel()).isFalse();
    }

    @Test
    public void testInvalidRange() {
        CharStream.range('z', 'a').toList().isEmpty();
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
    public void testClose() {
        AtomicInteger closeCount = new AtomicInteger(0);
        CharStream stream = createCharStream('a', 'b', 'c').onClose(closeCount::incrementAndGet);
        stream.close();
        assertThat(closeCount.get()).isEqualTo(1);
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
    public void testKthLargest() {
        CharStream stream = createCharStream('c', 'a', 'e', 'b', 'd');
        assertThat(stream.kthLargest(2)).isEqualTo(OptionalChar.of('d'));
    }

    @Test
    public void testMapPartial() {
        CharStream stream = createCharStream('a', 'b', 'c', 'd').mapPartial(c -> c % 2 == 0 ? OptionalChar.of(c) : OptionalChar.empty());
        assertThat(stream.toArray()).containsExactly('b', 'd');
    }

    @Test
    public void testRangeMap() {
        CharStream stream = createCharStream('a', 'a', 'b', 'b', 'c').rangeMap((c1, c2) -> c1 == c2, (first, last) -> last);
        assertThat(stream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testsummaryStatistics() {
        CharSummaryStatistics stats = createCharStream('a', 'b', 'c').summaryStatistics();
        assertThat(stats.getCount()).isEqualTo(3);
        assertThat(stats.getMin()).isEqualTo('a');
        assertThat(stats.getMax()).isEqualTo('c');
        assertThat(stats.getSum()).isEqualTo('a' + 'b' + 'c');
    }

    @Test
    public void testPercentiles() {
        Optional<Map<Percentage, Character>> percentiles = createCharStream('a', 'b', 'c', 'd', 'e').percentiles();
        assertThat(percentiles.isPresent()).isTrue();
        Map<Percentage, Character> map = percentiles.get();
        assertThat(map.get(Percentage._50)).isEqualTo('c');
    }

    @Test
    public void testAsIntStream() {
        IntStream intStream = createCharStream('a', 'b', 'c').asIntStream();
        assertThat(intStream.toArray()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testBoxed() {
        Stream<Character> boxed = createCharStream('a', 'b', 'c').boxed();
        assertThat(boxed.toList()).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testToMap() {
        Map<Character, Integer> map = createCharStream('a', 'b', 'c').toMap(c -> c, c -> c - 'a');
        assertThat(map).containsExactly(entry('a', 0), entry('b', 1), entry('c', 2));
    }

    @Test
    public void testGroupTo() {
        Map<Integer, List<Character>> grouped = createCharStream('a', 'b', 'c', 'd', 'e').groupTo(c -> c % 2, Collectors.toList());
        assertThat(grouped.get(0)).containsExactlyInAnyOrder('b', 'd');
        assertThat(grouped.get(1)).containsExactlyInAnyOrder('a', 'c', 'e');
    }

    private Map.Entry<Character, Integer> entry(char key, int value) {
        return new AbstractMap.SimpleEntry<>(key, value);
    }
}
