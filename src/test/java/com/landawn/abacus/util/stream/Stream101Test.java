package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.ByteIterator;
import com.landawn.abacus.util.CharIterator;
import com.landawn.abacus.util.DoubleIterator;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.IntIterator;
import com.landawn.abacus.util.LongIterator;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.ShortIterator;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteNFunction;
import com.landawn.abacus.util.function.ByteTriFunction;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharNFunction;
import com.landawn.abacus.util.function.CharTriFunction;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleNFunction;
import com.landawn.abacus.util.function.DoubleTriFunction;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatNFunction;
import com.landawn.abacus.util.function.FloatTriFunction;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntNFunction;
import com.landawn.abacus.util.function.IntTriFunction;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongNFunction;
import com.landawn.abacus.util.function.LongTriFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortNFunction;
import com.landawn.abacus.util.function.ShortTriFunction;

@Tag("new-test")
public class Stream101Test extends TestBase {

    @Mock
    private Function<Object, Object> mockFunction;

    @Mock
    private BiFunction<Object, Object, Object> mockBiFunction;

    @Mock
    private Consumer<Object> mockConsumer;

    @Mock
    private Predicate<Object> mockPredicate;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }

    @AfterEach
    public void tearDown() {
    }

    protected Stream<Boolean> createStream(final boolean... data) {
        return Stream.of(data);
    }

    protected Stream<Boolean> createStream(final boolean[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Character> createStream(final char... data) {
        return Stream.of(data);
    }

    protected Stream<Character> createStream(final char[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Byte> createStream(final byte... data) {
        return Stream.of(data);
    }

    protected Stream<Byte> createStream(final byte[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Short> createStream(final short... data) {
        return Stream.of(data);
    }

    protected Stream<Short> createStream(final short[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Integer> createStream(final int... data) {
        return Stream.of(data);
    }

    protected Stream<Integer> createStream(final int[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Long> createStream(final long... data) {
        return Stream.of(data);
    }

    protected Stream<Long> createStream(final long[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Float> createStream(final float... data) {
        return Stream.of(data);
    }

    protected Stream<Float> createStream(final float[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected Stream<Double> createStream(final double... data) {
        return Stream.of(data);
    }

    protected Stream<Double> createStream(final double[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream01(final T... data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final T[] data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterable<? extends T> data) {
        return Stream.of(data);
    }

    protected <T> Stream<T> createStream(final Collection<? extends T> data, int fromIndex, int toIndex) {
        return Stream.of(data, fromIndex, toIndex);
    }

    protected <T> Stream<T> createStream(final Iterator<? extends T> data) {
        return Stream.of(data);
    }

    protected <K, V> Stream<Map.Entry<K, V>> createStream(final Map<? extends K, ? extends V> map) {
        return Stream.of(map);
    }

    protected <T> Stream<T> createStream(final Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final java.util.Optional<T> op) {
        return op == null || op.isEmpty() ? Stream.empty() : Stream.of(op.get());
    }

    protected <T> Stream<T> createStream(final Enumeration<? extends T> enumeration) {
        return Stream.of(enumeration);
    }

    @Test
    public void testZipCharArraysWithBiFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharArraysWithTriFunction() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithBiFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithTriFunction() {
        CharIterator a = CharIterator.of('a', 'b', 'c');
        CharIterator b = CharIterator.of('x', 'y', 'z');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithBiFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("cz", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithTriFunction() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithNFunction() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a', 'b', 'c'), CharStream.of('x', 'y', 'z'), CharStream.of('1', '2', '3'));

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("by2", result.get(1));
        assertEquals("cz3", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNone() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y', 'z' };

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharArraysWithValueForNoneTriFunction() {
        char[] a = { 'a' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2', '3' };

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNone() {
        CharIterator a = CharIterator.of('a', 'b');
        CharIterator b = CharIterator.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharIteratorsWithValueForNoneTriFunction() {
        CharIterator a = CharIterator.of('a');
        CharIterator b = CharIterator.of('x', 'y');
        CharIterator c = CharIterator.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNone() {
        CharStream a = CharStream.of('a', 'b');
        CharStream b = CharStream.of('x', 'y', 'z');

        CharBiFunction<String> zipFunction = (c1, c2) -> "" + c1 + c2;

        List<String> result = Stream.zip(a, b, '-', '+', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax", result.get(0));
        assertEquals("by", result.get(1));
        assertEquals("-z", result.get(2));
    }

    @Test
    public void testZipCharStreamsWithValueForNoneTriFunction() {
        CharStream a = CharStream.of('a');
        CharStream b = CharStream.of('x', 'y');
        CharStream c = CharStream.of('1', '2', '3');

        CharTriFunction<String> zipFunction = (c1, c2, c3) -> "" + c1 + c2 + c3;

        List<String> result = Stream.zip(a, b, c, '-', '+', '*', zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipCharStreamCollectionWithValuesForNone() {
        Collection<CharStream> streams = Arrays.asList(CharStream.of('a'), CharStream.of('x', 'y'), CharStream.of('1', '2', '3'));

        char[] valuesForNone = { '-', '+', '*' };

        CharNFunction<String> zipFunction = chars -> {
            StringBuilder sb = new StringBuilder();
            for (char c : chars) {
                sb.append(c);
            }
            return sb.toString();
        };

        List<String> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals("ax1", result.get(0));
        assertEquals("-y2", result.get(1));
        assertEquals("-+3", result.get(2));
    }

    @Test
    public void testZipByteArraysWithBiFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteArraysWithTriFunction() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 4, 5, 6 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithBiFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2, (byte) 3);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithBiFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithTriFunction() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithNFunction() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1, (byte) 2, (byte) 3), ByteStream.of((byte) 4, (byte) 5, (byte) 6),
                ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNone() {
        byte[] a = { 1, 2 };
        byte[] b = { 4, 5, 6 };

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteArraysWithValueForNoneTriFunction() {
        byte[] a = { 1 };
        byte[] b = { 4, 5 };
        byte[] c = { 7, 8, 9 };

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNone() {
        ByteIterator a = ByteIterator.of((byte) 1, (byte) 2);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteIteratorsWithValueForNoneTriFunction() {
        ByteIterator a = ByteIterator.of((byte) 1);
        ByteIterator b = ByteIterator.of((byte) 4, (byte) 5);
        ByteIterator c = ByteIterator.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNone() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5, (byte) 6);

        ByteBiFunction<Integer> zipFunction = (b1, b2) -> (int) b1 + b2;

        List<Integer> result = Stream.zip(a, b, (byte) 10, (byte) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipByteStreamsWithValueForNoneTriFunction() {
        ByteStream a = ByteStream.of((byte) 1);
        ByteStream b = ByteStream.of((byte) 4, (byte) 5);
        ByteStream c = ByteStream.of((byte) 7, (byte) 8, (byte) 9);

        ByteTriFunction<Integer> zipFunction = (b1, b2, b3) -> (int) b1 + b2 + b3;

        List<Integer> result = Stream.zip(a, b, c, (byte) 10, (byte) 20, (byte) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipByteStreamCollectionWithValuesForNone() {
        Collection<ByteStream> streams = Arrays.asList(ByteStream.of((byte) 1), ByteStream.of((byte) 4, (byte) 5), ByteStream.of((byte) 7, (byte) 8, (byte) 9));

        byte[] valuesForNone = { 10, 20, 30 };

        ByteNFunction<Integer> zipFunction = bytes -> {
            int sum = 0;
            for (byte b : bytes) {
                sum += b;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortArraysWithBiFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortArraysWithTriFunction() {
        short[] a = { 1, 2, 3 };
        short[] b = { 4, 5, 6 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithBiFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2, (short) 3);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithBiFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithTriFunction() {
        ShortStream a = ShortStream.of((short) 1, (short) 2, (short) 3);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithNFunction() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1, (short) 2, (short) 3), ShortStream.of((short) 4, (short) 5, (short) 6),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNone() {
        short[] a = { 1, 2 };
        short[] b = { 4, 5, 6 };

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortArraysWithValueForNoneTriFunction() {
        short[] a = { 1 };
        short[] b = { 4, 5 };
        short[] c = { 7, 8, 9 };

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNone() {
        ShortIterator a = ShortIterator.of((short) 1, (short) 2);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortIteratorsWithValueForNoneTriFunction() {
        ShortIterator a = ShortIterator.of((short) 1);
        ShortIterator b = ShortIterator.of((short) 4, (short) 5);
        ShortIterator c = ShortIterator.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNone() {
        ShortStream a = ShortStream.of((short) 1, (short) 2);
        ShortStream b = ShortStream.of((short) 4, (short) 5, (short) 6);

        ShortBiFunction<Integer> zipFunction = (s1, s2) -> (int) s1 + s2;

        List<Integer> result = Stream.zip(a, b, (short) 10, (short) 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipShortStreamsWithValueForNoneTriFunction() {
        ShortStream a = ShortStream.of((short) 1);
        ShortStream b = ShortStream.of((short) 4, (short) 5);
        ShortStream c = ShortStream.of((short) 7, (short) 8, (short) 9);

        ShortTriFunction<Integer> zipFunction = (s1, s2, s3) -> (int) s1 + s2 + s3;

        List<Integer> result = Stream.zip(a, b, c, (short) 10, (short) 20, (short) 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipShortStreamCollectionWithValuesForNone() {
        Collection<ShortStream> streams = Arrays.asList(ShortStream.of((short) 1), ShortStream.of((short) 4, (short) 5),
                ShortStream.of((short) 7, (short) 8, (short) 9));

        short[] valuesForNone = { 10, 20, 30 };

        ShortNFunction<Integer> zipFunction = shorts -> {
            int sum = 0;
            for (short s : shorts) {
                sum += s;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntArraysWithBiFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntArraysWithTriFunction() {
        int[] a = { 1, 2, 3 };
        int[] b = { 4, 5, 6 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithBiFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithTriFunction() {
        IntIterator a = IntIterator.of(1, 2, 3);
        IntIterator b = IntIterator.of(4, 5, 6);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithBiFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(9), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithTriFunction() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(4, 5, 6);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithNFunction() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(4, 5, 6), IntStream.of(7, 8, 9));

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(15), result.get(1));
        assertEquals(Integer.valueOf(18), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNone() {
        int[] a = { 1, 2 };
        int[] b = { 4, 5, 6 };

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntArraysWithValueForNoneTriFunction() {
        int[] a = { 1 };
        int[] b = { 4, 5 };
        int[] c = { 7, 8, 9 };

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNone() {
        IntIterator a = IntIterator.of(1, 2);
        IntIterator b = IntIterator.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntIteratorsWithValueForNoneTriFunction() {
        IntIterator a = IntIterator.of(1);
        IntIterator b = IntIterator.of(4, 5);
        IntIterator c = IntIterator.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2);
        IntStream b = IntStream.of(4, 5, 6);

        IntBiFunction<Integer> zipFunction = (i1, i2) -> i1 + i2;

        List<Integer> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(5), result.get(0));
        assertEquals(Integer.valueOf(7), result.get(1));
        assertEquals(Integer.valueOf(16), result.get(2));
    }

    @Test
    public void testZipIntStreamsWithValueForNoneTriFunction() {
        IntStream a = IntStream.of(1);
        IntStream b = IntStream.of(4, 5);
        IntStream c = IntStream.of(7, 8, 9);

        IntTriFunction<Integer> zipFunction = (i1, i2, i3) -> i1 + i2 + i3;

        List<Integer> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipIntStreamCollectionWithValuesForNone() {
        Collection<IntStream> streams = Arrays.asList(IntStream.of(1), IntStream.of(4, 5), IntStream.of(7, 8, 9));

        int[] valuesForNone = { 10, 20, 30 };

        IntNFunction<Integer> zipFunction = ints -> {
            int sum = 0;
            for (int i : ints) {
                sum += i;
            }
            return sum;
        };

        List<Integer> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Integer.valueOf(12), result.get(0));
        assertEquals(Integer.valueOf(23), result.get(1));
        assertEquals(Integer.valueOf(39), result.get(2));
    }

    @Test
    public void testZipLongArraysWithBiFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithTriFunction() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 4L, 5L, 6L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithBiFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithTriFunction() {
        LongIterator a = LongIterator.of(1L, 2L, 3L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithBiFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(9L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithTriFunction() {
        LongStream a = LongStream.of(1L, 2L, 3L);
        LongStream b = LongStream.of(4L, 5L, 6L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithNFunction() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L, 2L, 3L), LongStream.of(4L, 5L, 6L), LongStream.of(7L, 8L, 9L));

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(15L), result.get(1));
        assertEquals(Long.valueOf(18L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNone() {
        long[] a = { 1L, 2L };
        long[] b = { 4L, 5L, 6L };

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongArraysWithValueForNoneTriFunction() {
        long[] a = { 1L };
        long[] b = { 4L, 5L };
        long[] c = { 7L, 8L, 9L };

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNone() {
        LongIterator a = LongIterator.of(1L, 2L);
        LongIterator b = LongIterator.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongIteratorsWithValueForNoneTriFunction() {
        LongIterator a = LongIterator.of(1L);
        LongIterator b = LongIterator.of(4L, 5L);
        LongIterator c = LongIterator.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNone() {
        LongStream a = LongStream.of(1L, 2L);
        LongStream b = LongStream.of(4L, 5L, 6L);

        LongBiFunction<Long> zipFunction = (l1, l2) -> l1 + l2;

        List<Long> result = Stream.zip(a, b, 10L, 20L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(5L), result.get(0));
        assertEquals(Long.valueOf(7L), result.get(1));
        assertEquals(Long.valueOf(16L), result.get(2));
    }

    @Test
    public void testZipLongStreamsWithValueForNoneTriFunction() {
        LongStream a = LongStream.of(1L);
        LongStream b = LongStream.of(4L, 5L);
        LongStream c = LongStream.of(7L, 8L, 9L);

        LongTriFunction<Long> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Long> result = Stream.zip(a, b, c, 10L, 20L, 30L, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipLongStreamCollectionWithValuesForNone() {
        Collection<LongStream> streams = Arrays.asList(LongStream.of(1L), LongStream.of(4L, 5L), LongStream.of(7L, 8L, 9L));

        long[] valuesForNone = { 10L, 20L, 30L };

        LongNFunction<Long> zipFunction = longs -> {
            long sum = 0;
            for (long l : longs) {
                sum += l;
            }
            return sum;
        };

        List<Long> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Long.valueOf(12L), result.get(0));
        assertEquals(Long.valueOf(23L), result.get(1));
        assertEquals(Long.valueOf(39L), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithBiFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithTriFunction() {
        float[] a = { 1f, 2f, 3f };
        float[] b = { 4f, 5f, 6f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12f), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithBiFunction() {
        FloatIterator a = FloatIterator.of(1f, 2f, 3f);
        FloatIterator b = FloatIterator.of(4f, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithTriFunction() {
        FloatIterator a = FloatIterator.of(1, 2, 3);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithBiFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(9f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithTriFunction() {
        FloatStream a = FloatStream.of(1, 2, 3);
        FloatStream b = FloatStream.of(4, 5f, 6f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithNFunction() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1, 2, 3), FloatStream.of(4, 5f, 6f), FloatStream.of(7f, 8f, 9f));

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(15f), result.get(1));
        assertEquals(Float.valueOf(18f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNone() {
        float[] a = { 1, 2 };
        float[] b = { 4, 5f, 6f };

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatArraysWithValueForNoneTriFunction() {
        float[] a = { 1 };
        float[] b = { 4, 5f };
        float[] c = { 7f, 8f, 9f };

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNone() {
        FloatIterator a = FloatIterator.of(1, 2);
        FloatIterator b = FloatIterator.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatIteratorsWithValueForNoneTriFunction() {
        FloatIterator a = FloatIterator.of(1);
        FloatIterator b = FloatIterator.of(4, 5f);
        FloatIterator c = FloatIterator.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNone() {
        FloatStream a = FloatStream.of(1, 2);
        FloatStream b = FloatStream.of(4, 5f, 6f);

        FloatBiFunction<Float> zipFunction = (l1, l2) -> l1 + l2;

        List<Float> result = Stream.zip(a, b, 10, 20, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(5f), result.get(0));
        assertEquals(Float.valueOf(7f), result.get(1));
        assertEquals(Float.valueOf(16f), result.get(2));
    }

    @Test
    public void testZipFloatStreamsWithValueForNoneTriFunction() {
        FloatStream a = FloatStream.of(1);
        FloatStream b = FloatStream.of(4, 5f);
        FloatStream c = FloatStream.of(7f, 8f, 9f);

        FloatTriFunction<Float> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Float> result = Stream.zip(a, b, c, 10, 20, 30, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipFloatStreamCollectionWithValuesForNone() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1), FloatStream.of(4, 5f), FloatStream.of(7f, 8f, 9f));

        float[] valuesForNone = { 10, 20, 30 };

        FloatNFunction<Float> zipFunction = floats -> {
            float sum = 0;
            for (float l : floats) {
                sum += l;
            }
            return sum;
        };

        List<Float> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Float.valueOf(12), result.get(0));
        assertEquals(Float.valueOf(23), result.get(1));
        assertEquals(Float.valueOf(39f), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithBiFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithTriFunction() {
        double[] a = { 1d, 2d, 3d };
        double[] b = { 4d, 5d, 6d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithBiFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d, 2d, 3d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithBiFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(9d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithTriFunction() {
        DoubleStream a = DoubleStream.of(1d, 2d, 3d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithNFunction() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d, 2d, 3d), DoubleStream.of(4d, 5d, 6d), DoubleStream.of(7d, 8d, 9d));

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(15d), result.get(1));
        assertEquals(Double.valueOf(18d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNone() {
        double[] a = { 1d, 2d };
        double[] b = { 4d, 5d, 6d };

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleArraysWithValueForNoneTriFunction() {
        double[] a = { 1d };
        double[] b = { 4d, 5d };
        double[] c = { 7d, 8d, 9d };

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNone() {
        DoubleIterator a = DoubleIterator.of(1d, 2d);
        DoubleIterator b = DoubleIterator.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleIteratorsWithValueForNoneTriFunction() {
        DoubleIterator a = DoubleIterator.of(1d);
        DoubleIterator b = DoubleIterator.of(4d, 5d);
        DoubleIterator c = DoubleIterator.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNone() {
        DoubleStream a = DoubleStream.of(1d, 2d);
        DoubleStream b = DoubleStream.of(4d, 5d, 6d);

        DoubleBiFunction<Double> zipFunction = (l1, l2) -> l1 + l2;

        List<Double> result = Stream.zip(a, b, 10d, 20d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(5d), result.get(0));
        assertEquals(Double.valueOf(7d), result.get(1));
        assertEquals(Double.valueOf(16d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamsWithValueForNoneTriFunction() {
        DoubleStream a = DoubleStream.of(1d);
        DoubleStream b = DoubleStream.of(4d, 5d);
        DoubleStream c = DoubleStream.of(7d, 8d, 9d);

        DoubleTriFunction<Double> zipFunction = (l1, l2, l3) -> l1 + l2 + l3;

        List<Double> result = Stream.zip(a, b, c, 10d, 20d, 30d, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testZipDoubleStreamCollectionWithValuesForNone() {
        Collection<DoubleStream> streams = Arrays.asList(DoubleStream.of(1d), DoubleStream.of(4d, 5d), DoubleStream.of(7d, 8d, 9d));

        double[] valuesForNone = { 10d, 20d, 30d };

        DoubleNFunction<Double> zipFunction = doubles -> {
            double sum = 0;
            for (double l : doubles) {
                sum += l;
            }
            return sum;
        };

        List<Double> result = Stream.zip(streams, valuesForNone, zipFunction).toList();

        assertEquals(3, result.size());
        assertEquals(Double.valueOf(12d), result.get(0));
        assertEquals(Double.valueOf(23d), result.get(1));
        assertEquals(Double.valueOf(39d), result.get(2));
    }

    @Test
    public void testSplitWithCharDelimiter() {
        List<String> result = Stream.split("a,b,c", ',').toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello world", ' ').toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("", ',').toList();
        assertEquals(Arrays.asList(""), result);

        result = Stream.split("abc", ',').toList();
        assertEquals(Arrays.asList("abc"), result);
    }

    @Test
    public void testSplitWithCharSequenceDelimiter() {
        List<String> result = Stream.split("a::b::c", "::").toList();
        assertEquals(Arrays.asList("a", "b", "c"), result);

        result = Stream.split("hello<br>world", "<br>").toList();
        assertEquals(Arrays.asList("hello", "world"), result);

        result = Stream.split("test", "::").toList();
        assertEquals(Arrays.asList("test"), result);
    }

    @Test
    public void testSplitWithPattern() {
        Pattern pattern = Pattern.compile("\\s+");
        List<String> result = Stream.split("hello   world  test", pattern).toList();
        assertEquals(Arrays.asList("hello", "world", "test"), result);

        pattern = Pattern.compile("[,;]");
        result = Stream.split("a,b;c,d", pattern).toList();
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testSplitToLines() {
        String multiLine = "line1\nline2\rline3\r\nline4";
        List<String> result = Stream.splitToLines(multiLine).toList();
        assertEquals(4, result.size());
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains("line3"));
        assertTrue(result.contains("line4"));

        result = Stream.splitToLines("single line").toList();
        assertEquals(Arrays.asList("single line"), result);

        result = Stream.splitToLines("").toList();
        assertEquals(Arrays.asList(""), result);
    }

    @Test
    public void testSplitToLinesWithOptions() {
        String multiLine = "  line1  \n\n  line2  \n\n";

        List<String> result = Stream.splitToLines(multiLine, false, false).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, true, false).toList();
        assertTrue(result.contains("line1"));
        assertTrue(result.contains("line2"));
        assertTrue(result.contains(""));

        result = Stream.splitToLines(multiLine, false, true).toList();
        assertTrue(result.contains("  line1  "));
        assertTrue(result.contains("  line2  "));
        assertFalse(result.contains(""));

        result = Stream.splitToLines(multiLine, true, true).toList();
        assertEquals(Arrays.asList("line1", "line2"), result);
    }

    @Test
    public void testSplitByChunkCount() {
        List<String> result = Stream.splitByChunkCount(10, 3, (from, to) -> from + "-" + to).toList();
        assertEquals(3, result.size());
        assertEquals("0-4", result.get(0));
        assertEquals("4-7", result.get(1));
        assertEquals("7-10", result.get(2));

        result = Stream.splitByChunkCount(7, 7, (from, to) -> from + "-" + to).toList();
        assertEquals(7, result.size());

        result = Stream.splitByChunkCount(0, 3, (from, to) -> from + "-" + to).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testSplitByChunkCountWithSizeSmallerFirst() {
        List<String> result = Stream.splitByChunkCount(7, 5, true, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-1", result.get(0));
        assertEquals("1-2", result.get(1));
        assertEquals("2-3", result.get(2));
        assertEquals("3-5", result.get(3));
        assertEquals("5-7", result.get(4));

        result = Stream.splitByChunkCount(7, 5, false, (from, to) -> from + "-" + to).toList();
        assertEquals(5, result.size());
        assertEquals("0-2", result.get(0));
        assertEquals("2-4", result.get(1));
        assertEquals("4-5", result.get(2));
        assertEquals("5-6", result.get(3));
        assertEquals("6-7", result.get(4));
    }

    @Test
    public void testSplitByChunkCountNegativeTotalSize() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(-1, 3, (from, to) -> from + "-" + to));
    }

    @Test
    public void testSplitByChunkCountZeroMaxChunkCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.splitByChunkCount(10, 0, (from, to) -> from + "-" + to));
    }

    @Test
    public void testFlattenCollection() {
        List<List<Integer>> nested = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(4, 5), Arrays.asList(6, 7, 8, 9));

        List<Integer> result = Stream.flatten(nested).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        List<List<Integer>> empty = new ArrayList<>();
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());

        nested = Arrays.asList(new ArrayList<>(), Arrays.asList(1, 2), new ArrayList<>());
        result = Stream.flatten(nested).toList();
        assertEquals(Arrays.asList(1, 2), result);
    }

    @Test
    public void testFlatten2DArray() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5 }, { 6, 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        Integer[][] empty = new Integer[0][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());

        Integer[][] withNulls = { { 1, 2 }, null, { 3, 4 } };
        result = Stream.flatten(withNulls).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatten2DArrayVertically() {
        Integer[][] array = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };

        List<Integer> result = Stream.flatten(array, false).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9), result);

        result = Stream.flatten(array, true).toList();
        assertEquals(Arrays.asList(1, 4, 7, 2, 5, 8, 3, 6, 9), result);

        Integer[][] jagged = { { 1, 2, 3 }, { 4, 5 }, { 6 } };
        result = Stream.flatten(jagged, true).toList();
        assertEquals(Arrays.asList(1, 4, 6, 2, 5, 3), result);
    }

    @Test
    public void testFlatten2DArrayWithAlignment() {
        Integer[][] array = { { 1, 2 }, { 3, 4, 5 }, { 6 } };

        List<Integer> result = Stream.flatten(array, 0, false).toList();
        assertEquals(Arrays.asList(1, 2, 0, 3, 4, 5, 6, 0, 0), result);

        result = Stream.flatten(array, -1, true).toList();
        assertEquals(Arrays.asList(1, 3, 6, 2, 4, -1, -1, 5, -1), result);
    }

    @Test
    public void testFlatten3DArray() {
        Integer[][][] array = { { { 1, 2 }, { 3, 4 } }, { { 5, 6 }, { 7, 8 } } };

        List<Integer> result = Stream.flatten(array).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        Integer[][][] empty = new Integer[0][][];
        result = Stream.flatten(empty).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testRepeat() {
        List<String> result = Stream.repeat("test", 3).toList();
        assertEquals(Arrays.asList("test", "test", "test"), result);

        result = Stream.repeat("hello", 0).toList();
        assertTrue(result.isEmpty());

        result = Stream.repeat((String) null, 2).toList();
        assertEquals(Arrays.asList(null, null), result);
    }

    @Test
    public void testRepeatNegativeCount() {
        assertThrows(IllegalArgumentException.class, () -> Stream.repeat("test", -1));
    }

    @Test
    public void testIterateWithBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(() -> count[0] < 5, () -> count[0]++).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);
    }

    @Test
    public void testIterateWithInitAndBooleanSupplier() {
        final int[] count = { 0 };
        List<Integer> result = Stream.iterate(1, () -> count[0]++ < 4, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8), result);
    }

    @Test
    public void testIterateWithInitAndPredicate() {
        List<Integer> result = Stream.iterate(1, n -> n < 20, n -> n * 2).toList();

        assertEquals(Arrays.asList(1, 2, 4, 8, 16), result);
    }

    @Test
    public void testIterateInfinite() {
        List<Integer> result = Stream.iterate(1, n -> n + 1).limit(5).toList();

        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void testGenerate() {
        final int[] counter = { 0 };
        List<Integer> result = Stream.generate(() -> counter[0]++).limit(5).toList();

        assertEquals(Arrays.asList(0, 1, 2, 3, 4), result);

        result = Stream.generate(() -> 42).limit(3).toList();

        assertEquals(Arrays.asList(42, 42, 42), result);
    }

    @Test
    public void testOfLinesFromFile() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("line1\nline2\nline3");
        }

        List<String> result = Stream.ofLines(tempFile).toList();
        assertEquals(Arrays.asList("line1", "line2", "line3"), result);
    }

    @Test
    public void testOfLinesFromFileWithCharset() throws IOException {
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit();

        String content = "Hello\nWorld\n你好";
        Files.write(tempFile.toPath(), content.getBytes(StandardCharsets.UTF_8));

        List<String> result = Stream.ofLines(tempFile, StandardCharsets.UTF_8).toList();
        assertEquals(Arrays.asList("Hello", "World", "你好"), result);
    }

    @Test
    public void testOfLinesFromPath() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.deleteIfExists(tempPath);

        Files.write(tempPath, Arrays.asList("path1", "path2", "path3"));

        List<String> result = Stream.ofLines(tempPath).toList();
        assertEquals(Arrays.asList("path1", "path2", "path3"), result);

        Files.deleteIfExists(tempPath);
    }

    @Test
    public void testOfLinesFromPathWithCharset() throws IOException {
        Path tempPath = Files.createTempFile("test", ".txt");
        Files.deleteIfExists(tempPath);

        String content = "测试\n内容";
        Files.write(tempPath, content.getBytes(StandardCharsets.UTF_8));

        List<String> result = Stream.ofLines(tempPath, StandardCharsets.UTF_8).toList();
        assertEquals(Arrays.asList("测试", "内容"), result);

        Files.deleteIfExists(tempPath);
    }

    @Test
    public void testOfLinesFromReader() throws IOException {
        String content = "reader1\nreader2\nreader3";
        StringReader reader = new StringReader(content);

        List<String> result = Stream.ofLines(reader).toList();
        assertEquals(Arrays.asList("reader1", "reader2", "reader3"), result);
    }

    @Test
    public void testOfLinesFromReaderWithCloseOption() throws IOException {
        String content = "test1\ntest2";
        StringReader reader = new StringReader(content);

        try (Stream<String> stream = Stream.ofLines(reader, true)) {
            List<String> result = stream.toList();
            assertEquals(Arrays.asList("test1", "test2"), result);
        }

    }

    @Test
    public void testListFiles() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File file2 = new File(tempDir, "file2.txt");
        File subDir = new File(tempDir, "subdir");

        file1.createNewFile();
        file2.createNewFile();
        subDir.mkdir();

        file1.deleteOnExit();
        file2.deleteOnExit();
        subDir.deleteOnExit();

        List<File> files = Stream.listFiles(tempDir).toList();
        assertEquals(3, files.size());

        File nonExistent = new File("non_existent_dir");
        files = Stream.listFiles(nonExistent).toList();
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFilesRecursively() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File subDir = new File(tempDir, "subdir");
        File file2 = new File(subDir, "file2.txt");

        file1.createNewFile();
        subDir.mkdir();
        file2.createNewFile();

        file1.deleteOnExit();
        file2.deleteOnExit();
        subDir.deleteOnExit();

        List<File> files = Stream.listFiles(tempDir, false).toList();
        assertEquals(2, files.size());

        files = Stream.listFiles(tempDir, true).toList();
        assertEquals(3, files.size());
    }

    @Test
    public void testListFilesExcludeDirectory() throws IOException {
        File tempDir = Files.createTempDirectory("test").toFile();
        tempDir.deleteOnExit();

        File file1 = new File(tempDir, "file1.txt");
        File subDir = new File(tempDir, "subdir");
        File file2 = new File(subDir, "file2.txt");

        file1.createNewFile();
        subDir.mkdir();
        file2.createNewFile();

        file1.deleteOnExit();
        file2.deleteOnExit();
        subDir.deleteOnExit();

        List<File> files = Stream.listFiles(tempDir, true, true).toList();
        assertEquals(2, files.size());
        assertTrue(files.stream().allMatch(File::isFile));
    }

    @Test
    public void testIntervalWithSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        List<Integer> result = Stream.interval(10, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(counter[0] >= 3);
    }

    @Test
    public void testIntervalWithDelayAndSupplier() throws InterruptedException {
        final int[] counter = { 0 };
        long startTime = System.currentTimeMillis();

        List<Integer> result = Stream.interval(50, 10, () -> counter[0]++).limit(3).toList();

        long elapsedTime = System.currentTimeMillis() - startTime;

        assertEquals(Arrays.asList(0, 1, 2), result);
        assertTrue(elapsedTime >= 50);
    }

    @Test
    public void testIntervalWithTimeUnit() throws InterruptedException {
        final int[] counter = { 0 };

        List<Integer> result = Stream.interval(0, 10, TimeUnit.MILLISECONDS, () -> counter[0]++).limit(3).toList();

        assertEquals(Arrays.asList(0, 1, 2), result);
    }

    @Test
    public void testIntervalWithLongFunction() throws InterruptedException {
        List<String> result = Stream.interval(10, time -> "Time: " + time).limit(2).toList();

        assertEquals(2, result.size());
        assertTrue(result.get(0).startsWith("Time: "));
        assertTrue(result.get(1).startsWith("Time: "));
    }

    @Test
    public void testObserveWithDuration() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, Duration.ofMillis(100)).toList();
        assertTrue(result.size() >= 2);
        assertTrue(result.contains("item1"));
        assertTrue(result.contains("item2"));
    }

    @Test
    public void testObserveWithBooleanSupplier() throws InterruptedException {
        BlockingQueue<String> queue = new ArrayBlockingQueue<>(10);
        MutableBoolean hasMore = MutableBoolean.of(true);

        queue.offer("item1");
        queue.offer("item2");

        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(50);
                queue.offer("item3");
                Thread.sleep(50);
                hasMore.setFalse();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        List<String> result = Stream.observe(queue, hasMore::value, 20).toList();
        assertEquals(3, result.size());
        assertEquals(Arrays.asList("item1", "item2", "item3"), result);
    }

    @Test
    public void testConcatArrays() {
        Integer[] arr1 = { 1, 2, 3 };
        Integer[] arr2 = { 4, 5 };
        Integer[] arr3 = { 6, 7, 8 };

        List<Integer> result = Stream.concat(arr1, arr2, arr3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);

        result = Stream.concat(new Integer[0], arr1, new Integer[0]).toList();
        assertEquals(Arrays.asList(1, 2, 3), result);

        result = Stream.concat(new Integer[0][0]).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterables() {
        List<Integer> list1 = Arrays.asList(1, 2, 3);
        Set<Integer> set2 = new HashSet<>(Arrays.asList(4, 5));
        List<Integer> list3 = Arrays.asList(6, 7);

        List<Integer> result = Stream.concat(list1, set2, list3).toList();
        assertEquals(7, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6, 7)));
    }

    @Test
    public void testConcatIterators() {
        Iterator<Integer> iter1 = Arrays.asList(1, 2, 3).iterator();
        Iterator<Integer> iter2 = Arrays.asList(4, 5).iterator();
        Iterator<Integer> iter3 = Arrays.asList(6, 7, 8).iterator();

        List<Integer> result = Stream.concat(iter1, iter2, iter3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testConcatStreams() {
        Stream<Integer> stream1 = createStream(1, 2, 3);
        Stream<Integer> stream2 = createStream(4, 5);
        Stream<Integer> stream3 = createStream(6, 7, 8);

        List<Integer> result = Stream.concat(stream1, stream2, stream3).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8), result);
    }

    @Test
    public void testConcatStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(3, 4), createStream(5, 6));

        List<Integer> result = Stream.concat(streams).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);

        List<Stream<Integer>> emptyStreams = new ArrayList<>();
        result = Stream.concat(emptyStreams).toList();
        assertTrue(result.isEmpty());
    }

    @Test
    public void testConcatIterablesCollection() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), new HashSet<>(Arrays.asList(3, 4)), Arrays.asList(5, 6));

        List<Integer> result = Stream.concatIterables(iterables).toList();
        assertEquals(6, result.size());
        assertTrue(result.containsAll(Arrays.asList(1, 2, 3, 4, 5, 6)));
    }

    @Test
    public void testConcatIteratorsCollection() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(3, 4).iterator(), Arrays.asList(5, 6).iterator());

        List<Integer> result = Stream.concatIterators(iterators).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testZipCharArrays() {
        char[] a = { 'a', 'b', 'c' };
        char[] b = { 'x', 'y', 'z' };

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);

        char[] c = { '1', '2' };
        result = Stream.zip(a, c, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("a1", "b2"), result);
    }

    @Test
    public void testZipThreeCharArrays() {
        char[] a = { 'a', 'b' };
        char[] b = { 'x', 'y' };
        char[] c = { '1', '2' };

        List<String> result = Stream.zip(a, b, c, (c1, c2, c3) -> "" + c1 + c2 + c3).toList();
        assertEquals(Arrays.asList("ax1", "by2"), result);
    }

    @Test
    public void testZipByteArrays() {
        byte[] a = { 1, 2, 3 };
        byte[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipShortArrays() {
        short[] a = { 1, 2, 3 };
        short[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (s1, s2) -> (int) (s1 + s2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntArrays() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20, 30 };

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipLongArrays() {
        long[] a = { 1L, 2L, 3L };
        long[] b = { 10L, 20L, 30L };

        List<Long> result = Stream.zip(a, b, (l1, l2) -> l1 + l2).toList();
        assertEquals(Arrays.asList(11L, 22L, 33L), result);
    }

    @Test
    public void testZipFloatArrays() {
        float[] a = { 1.0f, 2.0f, 3.0f };
        float[] b = { 10.0f, 20.0f, 30.0f };

        List<Float> result = Stream.zip(a, b, (f1, f2) -> f1 + f2).toList();
        assertEquals(Arrays.asList(11.0f, 22.0f, 33.0f), result);
    }

    @Test
    public void testZipDoubleArrays() {
        double[] a = { 1.0, 2.0, 3.0 };
        double[] b = { 10.0, 20.0, 30.0 };

        List<Double> result = Stream.zip(a, b, (d1, d2) -> d1 + d2).toList();
        assertEquals(Arrays.asList(11.0, 22.0, 33.0), result);
    }

    @Test
    public void testZipObjectArrays() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2, 3 };

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeObjectArrays() {
        String[] a = { "a", "b" };
        Integer[] b = { 1, 2 };
        Double[] c = { 0.1, 0.2 };

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipIterables() {
        List<String> a = Arrays.asList("a", "b", "c");
        Set<Integer> b = new LinkedHashSet<>(Arrays.asList(1, 2, 3));

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testZipIterators() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> b = Arrays.asList(1, 2, 3).iterator();

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipStreams() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2, 3);

        List<String> result = Stream.zip(a, b, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipThreeStreams() {
        Stream<String> a = createStream01("a", "b");
        Stream<Integer> b = createStream(1, 2);
        Stream<Double> c = createStream(0.1, 0.2);

        List<String> result = Stream.zip(a, b, c, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b20.2"), result);
    }

    @Test
    public void testZipStreamCollection() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2, 3), createStream(10, 20, 30), createStream(100, 200, 300));

        List<Integer> result = Stream.zip(streams, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipIterablesWithFunction() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2, 3), Arrays.asList(10, 20, 30), Arrays.asList(100, 200, 300));

        List<Integer> result = Stream.zipIterables(iterables, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipIteratorsWithFunction() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2, 3).iterator(), Arrays.asList(10, 20, 30).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> result = Stream.zipIterators(iterators, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipArraysWithValueForNone() {
        String[] a = { "a", "b", "c" };
        Integer[] b = { 1, 2 };

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipThreeArraysWithValueForNone() {
        String[] a = { "a", "b" };
        Integer[] b = { 1 };
        Double[] c = { 0.1, 0.2, 0.3 };

        List<String> result = Stream.zip(a, b, c, "X", 99, 0.0, (s, i, d) -> s + i + d).toList();
        assertEquals(Arrays.asList("a10.1", "b990.2", "X990.3"), result);
    }

    @Test
    public void testZipIterablesWithValueForNone() {
        List<String> a = Arrays.asList("a", "b", "c");
        List<Integer> b = Arrays.asList(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipIteratorsWithValueForNone() {
        Iterator<String> a = Arrays.asList("a", "b", "c").iterator();
        Iterator<Integer> b = Arrays.asList(1, 2).iterator();

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipStreamsWithValueForNone() {
        Stream<String> a = createStream01("a", "b", "c");
        Stream<Integer> b = createStream(1, 2);

        List<String> result = Stream.zip(a, b, "X", 99, (s, i) -> s + i).toList();
        assertEquals(Arrays.asList("a1", "b2", "c99"), result);
    }

    @Test
    public void testZipCollectionWithValuesForNone() {
        List<Stream<Integer>> streams = Arrays.asList(createStream(1, 2), createStream(10), createStream(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zip(streams, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIterablesCollectionWithValuesForNone() {
        List<Iterable<Integer>> iterables = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(10), Arrays.asList(100, 200, 300));

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterables(iterables, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipIteratorsCollectionWithValuesForNone() {
        List<Iterator<Integer>> iterators = Arrays.asList(Arrays.asList(1, 2).iterator(), Arrays.asList(10).iterator(),
                Arrays.asList(100, 200, 300).iterator());

        List<Integer> valuesForNone = Arrays.asList(0, 0, 0);

        List<Integer> result = Stream.zipIterators(iterators, valuesForNone, list -> list.stream().mapToInt(Integer::intValue).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

    @Test
    public void testZipCharStreams() {
        CharStream a = CharStream.of('a', 'b', 'c');
        CharStream b = CharStream.of('x', 'y', 'z');

        List<String> result = Stream.zip(a, b, (c1, c2) -> "" + c1 + c2).toList();
        assertEquals(Arrays.asList("ax", "by", "cz"), result);
    }

    @Test
    public void testZipByteStreams() {
        ByteStream a = ByteStream.of((byte) 1, (byte) 2, (byte) 3);
        ByteStream b = ByteStream.of((byte) 10, (byte) 20, (byte) 30);

        List<Integer> result = Stream.zip(a, b, (b1, b2) -> (int) (b1 + b2)).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipIntStreams() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20, 30);

        List<Integer> result = Stream.zip(a, b, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 33), result);
    }

    @Test
    public void testZipPrimitiveStreamCollection() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2, 3), IntStream.of(10, 20, 30), IntStream.of(100, 200, 300));

        List<Integer> result = Stream.zip(streams, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 222, 333), result);
    }

    @Test
    public void testZipPrimitiveArraysWithValueForNone() {
        int[] a = { 1, 2, 3 };
        int[] b = { 10, 20 };

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipPrimitiveStreamsWithValueForNone() {
        IntStream a = IntStream.of(1, 2, 3);
        IntStream b = IntStream.of(10, 20);

        List<Integer> result = Stream.zip(a, b, 0, 99, (i1, i2) -> i1 + i2).toList();
        assertEquals(Arrays.asList(11, 22, 102), result);
    }

    @Test
    public void testZipPrimitiveStreamCollectionWithValuesForNone() {
        List<IntStream> streams = Arrays.asList(IntStream.of(1, 2), IntStream.of(10), IntStream.of(100, 200, 300));

        int[] valuesForNone = { 0, 0, 0 };

        List<Integer> result = Stream.zip(streams, valuesForNone, arr -> Arrays.stream(arr).sum()).toList();
        assertEquals(Arrays.asList(111, 202, 300), result);
    }

}
