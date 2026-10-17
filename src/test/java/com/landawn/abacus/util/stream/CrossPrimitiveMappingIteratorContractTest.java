package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

/** Mapping between stream families preserves values and the Iterator exhaustion contract. */
class CrossPrimitiveMappingIteratorContractTest extends TestBase {
    static java.util.stream.Stream<Arguments> mappings() {
        List<Arguments> cases = new ArrayList<>();
        for (boolean iterator : new boolean[] { false, true }) {
            for (int threads : new int[] { 0, 1, 3 }) {
                String prefix = "iterator=" + iterator + "/threads=" + threads + "/";
                add(cases, prefix + "Byte/mapToInt", () -> byteSource(iterator, threads).mapToInt(v -> (int) (v + 10)), List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Byte/flatMapToInt",
                        () -> byteSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Byte/mapToObj", () -> byteSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Byte/flatMapToObj", () -> byteSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Byte/flatmapToObj", () -> byteSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Byte/flatMapArrayToObj",
                        () -> byteSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Char/mapToInt", () -> charSource(iterator, threads).mapToInt(v -> (int) (v + 10)), List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Char/flatMapToInt",
                        () -> charSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Char/mapToObj", () -> charSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Char/flatMapToObj", () -> charSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Char/flatmapToObj", () -> charSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Char/flatMapArrayToObj",
                        () -> charSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Short/mapToInt", () -> shortSource(iterator, threads).mapToInt(v -> (int) (v + 10)),
                        List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Short/flatMapToInt",
                        () -> shortSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Short/mapToObj", () -> shortSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Short/flatMapToObj", () -> shortSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Short/flatmapToObj",
                        () -> shortSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)), List.of("value1", "value3"));
                add(cases, prefix + "Short/flatMapArrayToObj",
                        () -> shortSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Int/mapToByte", () -> intSource(iterator, threads).mapToByte(v -> (byte) (v + 10)),
                        List.of((byte) 11, (byte) 12, (byte) 13));
                add(cases, prefix + "Int/flatMapToByte",
                        () -> intSource(iterator, threads).flatMapToByte(v -> v == 2 ? null : ByteStream.of(new byte[] { (byte) (int) v, (byte) (v + 10) })),
                        List.of((byte) 1, (byte) 11, (byte) 3, (byte) 13));
                add(cases, prefix + "Int/mapToChar", () -> intSource(iterator, threads).mapToChar(v -> (char) (v + 10)),
                        List.of((char) 11, (char) 12, (char) 13));
                add(cases, prefix + "Int/flatMapToChar",
                        () -> intSource(iterator, threads).flatMapToChar(v -> v == 2 ? null : CharStream.of(new char[] { (char) (int) v, (char) (v + 10) })),
                        List.of((char) 1, (char) 11, (char) 3, (char) 13));
                add(cases, prefix + "Int/mapToShort", () -> intSource(iterator, threads).mapToShort(v -> (short) (v + 10)),
                        List.of((short) 11, (short) 12, (short) 13));
                add(cases, prefix + "Int/flatMapToShort",
                        () -> intSource(iterator, threads)
                                .flatMapToShort(v -> v == 2 ? null : ShortStream.of(new short[] { (short) (int) v, (short) (v + 10) })),
                        List.of((short) 1, (short) 11, (short) 3, (short) 13));
                add(cases, prefix + "Int/mapToLong", () -> intSource(iterator, threads).mapToLong(v -> (long) (v + 10)),
                        List.of((long) 11, (long) 12, (long) 13));
                add(cases, prefix + "Int/flatMapToLong",
                        () -> intSource(iterator, threads).flatMapToLong(v -> v == 2 ? null : LongStream.of(new long[] { (long) v, (long) (v + 10) })),
                        List.of((long) 1, (long) 11, (long) 3, (long) 13));
                add(cases, prefix + "Int/mapToFloat", () -> intSource(iterator, threads).mapToFloat(v -> (float) (v + 10)),
                        List.of((float) 11, (float) 12, (float) 13));
                add(cases, prefix + "Int/flatMapToFloat",
                        () -> intSource(iterator, threads).flatMapToFloat(v -> v == 2 ? null : FloatStream.of(new float[] { (float) v, (float) (v + 10) })),
                        List.of((float) 1, (float) 11, (float) 3, (float) 13));
                add(cases, prefix + "Int/mapToDouble", () -> intSource(iterator, threads).mapToDouble(v -> (double) (v + 10)),
                        List.of((double) 11, (double) 12, (double) 13));
                add(cases, prefix + "Int/flatMapToDouble",
                        () -> intSource(iterator, threads)
                                .flatMapToDouble(v -> v == 2 ? null : DoubleStream.of(new double[] { (double) v, (double) (v + 10) })),
                        List.of((double) 1, (double) 11, (double) 3, (double) 13));
                add(cases, prefix + "Int/mapToObj", () -> intSource(iterator, threads).mapToObj(v -> "value" + (int) v), List.of("value1", "value2", "value3"));
                add(cases, prefix + "Int/flatMapToObj", () -> intSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Int/flatmapToObj", () -> intSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Int/flatMapArrayToObj",
                        () -> intSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Long/mapToInt", () -> longSource(iterator, threads).mapToInt(v -> (int) (v + 10)), List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Long/flatMapToInt",
                        () -> longSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Long/mapToFloat", () -> longSource(iterator, threads).mapToFloat(v -> (float) (v + 10)),
                        List.of((float) 11, (float) 12, (float) 13));
                add(cases, prefix + "Long/flatMapToFloat",
                        () -> longSource(iterator, threads).flatMapToFloat(v -> v == 2 ? null : FloatStream.of(new float[] { (float) v, (float) (v + 10) })),
                        List.of((float) 1, (float) 11, (float) 3, (float) 13));
                add(cases, prefix + "Long/mapToDouble", () -> longSource(iterator, threads).mapToDouble(v -> (double) (v + 10)),
                        List.of((double) 11, (double) 12, (double) 13));
                add(cases, prefix + "Long/flatMapToDouble",
                        () -> longSource(iterator, threads)
                                .flatMapToDouble(v -> v == 2 ? null : DoubleStream.of(new double[] { (double) v, (double) (v + 10) })),
                        List.of((double) 1, (double) 11, (double) 3, (double) 13));
                add(cases, prefix + "Long/mapToObj", () -> longSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Long/flatMapToObj", () -> longSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Long/flatmapToObj", () -> longSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Long/flatMapArrayToObj",
                        () -> longSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Float/mapToInt", () -> floatSource(iterator, threads).mapToInt(v -> (int) (v + 10)),
                        List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Float/flatMapToInt",
                        () -> floatSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Float/mapToLong", () -> floatSource(iterator, threads).mapToLong(v -> (long) (v + 10)),
                        List.of((long) 11, (long) 12, (long) 13));
                add(cases, prefix + "Float/flatMapToLong",
                        () -> floatSource(iterator, threads).flatMapToLong(v -> v == 2 ? null : LongStream.of(new long[] { (long) v, (long) (v + 10) })),
                        List.of((long) 1, (long) 11, (long) 3, (long) 13));
                add(cases, prefix + "Float/mapToDouble", () -> floatSource(iterator, threads).mapToDouble(v -> (double) (v + 10)),
                        List.of((double) 11, (double) 12, (double) 13));
                add(cases, prefix + "Float/flatMapToDouble",
                        () -> floatSource(iterator, threads)
                                .flatMapToDouble(v -> v == 2 ? null : DoubleStream.of(new double[] { (double) v, (double) (v + 10) })),
                        List.of((double) 1, (double) 11, (double) 3, (double) 13));
                add(cases, prefix + "Float/mapToObj", () -> floatSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Float/flatMapToObj", () -> floatSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)),
                        List.of("value1", "value3"));
                add(cases, prefix + "Float/flatmapToObj",
                        () -> floatSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)), List.of("value1", "value3"));
                add(cases, prefix + "Float/flatMapArrayToObj",
                        () -> floatSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Double/mapToInt", () -> doubleSource(iterator, threads).mapToInt(v -> (int) (v + 10)),
                        List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Double/flatMapToInt",
                        () -> doubleSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Double/mapToLong", () -> doubleSource(iterator, threads).mapToLong(v -> (long) (v + 10)),
                        List.of((long) 11, (long) 12, (long) 13));
                add(cases, prefix + "Double/flatMapToLong",
                        () -> doubleSource(iterator, threads).flatMapToLong(v -> v == 2 ? null : LongStream.of(new long[] { (long) v, (long) (v + 10) })),
                        List.of((long) 1, (long) 11, (long) 3, (long) 13));
                add(cases, prefix + "Double/mapToFloat", () -> doubleSource(iterator, threads).mapToFloat(v -> (float) (v + 10)),
                        List.of((float) 11, (float) 12, (float) 13));
                add(cases, prefix + "Double/flatMapToFloat",
                        () -> doubleSource(iterator, threads).flatMapToFloat(v -> v == 2 ? null : FloatStream.of(new float[] { (float) v, (float) (v + 10) })),
                        List.of((float) 1, (float) 11, (float) 3, (float) 13));
                add(cases, prefix + "Double/mapToObj", () -> doubleSource(iterator, threads).mapToObj(v -> "value" + (int) v),
                        List.of("value1", "value2", "value3"));
                add(cases, prefix + "Double/flatMapToObj",
                        () -> doubleSource(iterator, threads).flatMapToObj(v -> v == 2 ? null : Stream.of("value" + (int) v)), List.of("value1", "value3"));
                add(cases, prefix + "Double/flatmapToObj",
                        () -> doubleSource(iterator, threads).flatmapToObj(v -> v == 2 ? List.of() : List.of("value" + (int) v)), List.of("value1", "value3"));
                add(cases, prefix + "Double/flatMapArrayToObj",
                        () -> doubleSource(iterator, threads).flatMapArrayToObj(v -> v == 2 ? null : new String[] { "value" + (int) v }),
                        List.of("value1", "value3"));
                add(cases, prefix + "Obj/mapToByte", () -> objSource(iterator, threads).mapToByte(v -> (byte) (v + 10)),
                        List.of((byte) 11, (byte) 12, (byte) 13));
                add(cases, prefix + "Obj/flatMapToByte",
                        () -> objSource(iterator, threads).flatMapToByte(v -> v == 2 ? null : ByteStream.of(new byte[] { (byte) (int) v, (byte) (v + 10) })),
                        List.of((byte) 1, (byte) 11, (byte) 3, (byte) 13));
                add(cases, prefix + "Obj/mapToChar", () -> objSource(iterator, threads).mapToChar(v -> (char) (v + 10)),
                        List.of((char) 11, (char) 12, (char) 13));
                add(cases, prefix + "Obj/flatMapToChar",
                        () -> objSource(iterator, threads).flatMapToChar(v -> v == 2 ? null : CharStream.of(new char[] { (char) (int) v, (char) (v + 10) })),
                        List.of((char) 1, (char) 11, (char) 3, (char) 13));
                add(cases, prefix + "Obj/mapToShort", () -> objSource(iterator, threads).mapToShort(v -> (short) (v + 10)),
                        List.of((short) 11, (short) 12, (short) 13));
                add(cases, prefix + "Obj/flatMapToShort",
                        () -> objSource(iterator, threads)
                                .flatMapToShort(v -> v == 2 ? null : ShortStream.of(new short[] { (short) (int) v, (short) (v + 10) })),
                        List.of((short) 1, (short) 11, (short) 3, (short) 13));
                add(cases, prefix + "Obj/mapToInt", () -> objSource(iterator, threads).mapToInt(v -> (int) (v + 10)), List.of((int) 11, (int) 12, (int) 13));
                add(cases, prefix + "Obj/flatMapToInt",
                        () -> objSource(iterator, threads).flatMapToInt(v -> v == 2 ? null : IntStream.of(new int[] { (int) v, (int) (v + 10) })),
                        List.of((int) 1, (int) 11, (int) 3, (int) 13));
                add(cases, prefix + "Obj/mapToLong", () -> objSource(iterator, threads).mapToLong(v -> (long) (v + 10)),
                        List.of((long) 11, (long) 12, (long) 13));
                add(cases, prefix + "Obj/flatMapToLong",
                        () -> objSource(iterator, threads).flatMapToLong(v -> v == 2 ? null : LongStream.of(new long[] { (long) v, (long) (v + 10) })),
                        List.of((long) 1, (long) 11, (long) 3, (long) 13));
                add(cases, prefix + "Obj/mapToFloat", () -> objSource(iterator, threads).mapToFloat(v -> (float) (v + 10)),
                        List.of((float) 11, (float) 12, (float) 13));
                add(cases, prefix + "Obj/flatMapToFloat",
                        () -> objSource(iterator, threads).flatMapToFloat(v -> v == 2 ? null : FloatStream.of(new float[] { (float) v, (float) (v + 10) })),
                        List.of((float) 1, (float) 11, (float) 3, (float) 13));
                add(cases, prefix + "Obj/mapToDouble", () -> objSource(iterator, threads).mapToDouble(v -> (double) (v + 10)),
                        List.of((double) 11, (double) 12, (double) 13));
                add(cases, prefix + "Obj/flatMapToDouble",
                        () -> objSource(iterator, threads)
                                .flatMapToDouble(v -> v == 2 ? null : DoubleStream.of(new double[] { (double) v, (double) (v + 10) })),
                        List.of((double) 1, (double) 11, (double) 3, (double) 13));
            }
        }
        return cases.stream();
    }

    private static void add(List<Arguments> cases, String name, Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory, List<?> expected) {
        cases.add(Arguments.of(name, factory, expected));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappings")
    void nextWithoutHasNextPreservesValuesAndRejectsExhaustion(String name, Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory, List<?> expected) {
        AtomicInteger closes = new AtomicInteger();
        try (var source = factory.get().onClose(closes::incrementAndGet)) {
            Iterator<?> iterator = source.iterator();
            List<Object> actual = new ArrayList<>();
            for (int i = 0; i < expected.size(); i++) {
                actual.add(iterator.next());
            }
            assertValues(name, expected, actual);
            assertThrows(NoSuchElementException.class, iterator::next);
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            assertEquals(0, closes.get(), "reading an iterator does not close its owning stream");
        }
        assertEquals(1, closes.get());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mappings")
    void forEachRemainingAfterOneNextConsumesTheRest(String name, Supplier<? extends BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> factory, List<?> expected) {
        try (var source = factory.get()) {
            Iterator<?> iterator = source.iterator();
            List<Object> actual = new ArrayList<>();
            actual.add(iterator.next());
            iterator.forEachRemaining(actual::add);
            assertValues(name, expected, actual);
            assertFalse(iterator.hasNext());
            assertThrows(NoSuchElementException.class, iterator::next);
            iterator.forEachRemaining(value -> {
                throw new AssertionError("iterator is already exhausted");
            });
        }
    }

    private static void assertValues(String name, List<?> expected, List<?> actual) {
        if (name.contains("threads=3")) {
            assertEquals(frequencies(expected), frequencies(actual), name);
        } else {
            assertEquals(expected, actual, name);
        }
    }

    private static Map<Object, Long> frequencies(List<?> values) {
        Map<Object, Long> counts = new HashMap<>();
        values.forEach(value -> counts.merge(value, 1L, Long::sum));
        return counts;
    }

    private static ByteStream byteSource(boolean iterator, int threads) {
        byte[] values = { 1, 2, 3 };
        ByteStream source = iterator ? ByteStream.of(com.landawn.abacus.util.ByteIterator.of(values)) : ByteStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static CharStream charSource(boolean iterator, int threads) {
        char[] values = { 1, 2, 3 };
        CharStream source = iterator ? CharStream.of(com.landawn.abacus.util.CharIterator.of(values)) : CharStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static ShortStream shortSource(boolean iterator, int threads) {
        short[] values = { 1, 2, 3 };
        ShortStream source = iterator ? ShortStream.of(com.landawn.abacus.util.ShortIterator.of(values)) : ShortStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static IntStream intSource(boolean iterator, int threads) {
        int[] values = { 1, 2, 3 };
        IntStream source = iterator ? IntStream.of(com.landawn.abacus.util.IntIterator.of(values)) : IntStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static LongStream longSource(boolean iterator, int threads) {
        long[] values = { 1, 2, 3 };
        LongStream source = iterator ? LongStream.of(com.landawn.abacus.util.LongIterator.of(values)) : LongStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static FloatStream floatSource(boolean iterator, int threads) {
        float[] values = { 1, 2, 3 };
        FloatStream source = iterator ? FloatStream.of(com.landawn.abacus.util.FloatIterator.of(values)) : FloatStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static DoubleStream doubleSource(boolean iterator, int threads) {
        double[] values = { 1, 2, 3 };
        DoubleStream source = iterator ? DoubleStream.of(com.landawn.abacus.util.DoubleIterator.of(values)) : DoubleStream.of(values);
        return threads == 0 ? source : source.parallel(threads);
    }

    private static Stream<Integer> objSource(boolean iterator, int threads) {
        Stream<Integer> source = iterator ? Stream.of(List.of(1, 2, 3).iterator()) : Stream.of(1, 2, 3);
        return threads == 0 ? source : source.parallel(threads);
    }

}
