package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.*;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

public class StreamCrossTypeIteratorContractTest extends TestBase {
    @FunctionalInterface
    interface Factory {
        BaseStream<?, ?, ?, ?, ?, ?, ?, ?> create();
    }

    record Pipeline(String name, Factory factory, List<Integer> expected) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Pipeline> pipelines() {
        List<Pipeline> cases = new ArrayList<>();
        for (boolean iterator : new boolean[] { false, true }) {
            for (int parallelMode : new int[] { 0, 1, 2, 3 }) {
                for (int size : new int[] { 0, 1, 4 }) {
                    List<Integer> expected = new ArrayList<>();
                    List<Integer> filtered = new ArrayList<>();
                    List<Integer> doubled = new ArrayList<>();
                    for (int i = 1; i <= size; i++) {
                        expected.add(i);
                        if (i != 2)
                            filtered.add(i);
                        doubled.add(i);
                        doubled.add(i);
                    }
                    String suffix = "/iterator=" + iterator + "/parallel=" + parallelMode + "/size=" + size;
                    cases.add(new Pipeline("Byte/mapToInt" + suffix, () -> sourceByte(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Byte/flatMapToInt" + suffix,
                            () -> sourceByte(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Byte/asIntStream" + suffix, () -> sourceByte(iterator, parallelMode, size).asIntStream(), expected));
                    cases.add(new Pipeline("Byte/mapToObj" + suffix, () -> sourceByte(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Byte/flatMapToObj" + suffix,
                            () -> sourceByte(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Byte/boxed" + suffix, () -> sourceByte(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Char/mapToInt" + suffix, () -> sourceChar(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Char/flatMapToInt" + suffix,
                            () -> sourceChar(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Char/asIntStream" + suffix, () -> sourceChar(iterator, parallelMode, size).asIntStream(), expected));
                    cases.add(new Pipeline("Char/mapToObj" + suffix, () -> sourceChar(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Char/flatMapToObj" + suffix,
                            () -> sourceChar(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Char/boxed" + suffix, () -> sourceChar(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Short/mapToInt" + suffix, () -> sourceShort(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Short/flatMapToInt" + suffix,
                            () -> sourceShort(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Short/asIntStream" + suffix, () -> sourceShort(iterator, parallelMode, size).asIntStream(), expected));
                    cases.add(new Pipeline("Short/mapToObj" + suffix, () -> sourceShort(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Short/flatMapToObj" + suffix,
                            () -> sourceShort(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Short/boxed" + suffix, () -> sourceShort(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Int/mapToByte" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToByte(v -> (byte) v), expected));
                    cases.add(new Pipeline("Int/flatMapToByte" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToByte(v -> v == 2 ? null : ByteStream.of((byte) v)), filtered));
                    cases.add(new Pipeline("Int/mapToChar" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToChar(v -> (char) v), expected));
                    cases.add(new Pipeline("Int/flatMapToChar" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToChar(v -> v == 2 ? null : CharStream.of((char) v)), filtered));
                    cases.add(new Pipeline("Int/mapToShort" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToShort(v -> (short) v), expected));
                    cases.add(new Pipeline("Int/flatMapToShort" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToShort(v -> v == 2 ? null : ShortStream.of((short) v)), filtered));
                    cases.add(new Pipeline("Int/mapToLong" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToLong(v -> (long) v), expected));
                    cases.add(new Pipeline("Int/flatMapToLong" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToLong(v -> v == 2 ? null : LongStream.of((long) v)), filtered));
                    cases.add(new Pipeline("Int/asLongStream" + suffix, () -> sourceInt(iterator, parallelMode, size).asLongStream(), expected));
                    cases.add(new Pipeline("Int/mapToFloat" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToFloat(v -> (float) v), expected));
                    cases.add(new Pipeline("Int/flatMapToFloat" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToFloat(v -> v == 2 ? null : FloatStream.of((float) v)), filtered));
                    cases.add(new Pipeline("Int/asFloatStream" + suffix, () -> sourceInt(iterator, parallelMode, size).asFloatStream(), expected));
                    cases.add(new Pipeline("Int/mapToDouble" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToDouble(v -> (double) v), expected));
                    cases.add(new Pipeline("Int/flatMapToDouble" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToDouble(v -> v == 2 ? null : DoubleStream.of((double) v)), filtered));
                    cases.add(new Pipeline("Int/asDoubleStream" + suffix, () -> sourceInt(iterator, parallelMode, size).asDoubleStream(), expected));
                    cases.add(new Pipeline("Int/mapToObj" + suffix, () -> sourceInt(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Int/flatMapToObj" + suffix,
                            () -> sourceInt(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Int/boxed" + suffix, () -> sourceInt(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Int/mapMulti" + suffix, () -> sourceInt(iterator, parallelMode, size).mapMulti((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Long/mapToInt" + suffix, () -> sourceLong(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Long/flatMapToInt" + suffix,
                            () -> sourceLong(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Long/mapToFloat" + suffix, () -> sourceLong(iterator, parallelMode, size).mapToFloat(v -> (float) v), expected));
                    cases.add(new Pipeline("Long/flatMapToFloat" + suffix,
                            () -> sourceLong(iterator, parallelMode, size).flatMapToFloat(v -> v == 2 ? null : FloatStream.of((float) v)), filtered));
                    cases.add(new Pipeline("Long/asFloatStream" + suffix, () -> sourceLong(iterator, parallelMode, size).asFloatStream(), expected));
                    cases.add(new Pipeline("Long/mapToDouble" + suffix, () -> sourceLong(iterator, parallelMode, size).mapToDouble(v -> (double) v), expected));
                    cases.add(new Pipeline("Long/flatMapToDouble" + suffix,
                            () -> sourceLong(iterator, parallelMode, size).flatMapToDouble(v -> v == 2 ? null : DoubleStream.of((double) v)), filtered));
                    cases.add(new Pipeline("Long/asDoubleStream" + suffix, () -> sourceLong(iterator, parallelMode, size).asDoubleStream(), expected));
                    cases.add(new Pipeline("Long/mapToObj" + suffix, () -> sourceLong(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Long/flatMapToObj" + suffix,
                            () -> sourceLong(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Long/boxed" + suffix, () -> sourceLong(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Long/mapMulti" + suffix, () -> sourceLong(iterator, parallelMode, size).mapMulti((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Float/mapToInt" + suffix, () -> sourceFloat(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Float/flatMapToInt" + suffix,
                            () -> sourceFloat(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Float/mapToLong" + suffix, () -> sourceFloat(iterator, parallelMode, size).mapToLong(v -> (long) v), expected));
                    cases.add(new Pipeline("Float/flatMapToLong" + suffix,
                            () -> sourceFloat(iterator, parallelMode, size).flatMapToLong(v -> v == 2 ? null : LongStream.of((long) v)), filtered));
                    cases.add(
                            new Pipeline("Float/mapToDouble" + suffix, () -> sourceFloat(iterator, parallelMode, size).mapToDouble(v -> (double) v), expected));
                    cases.add(new Pipeline("Float/flatMapToDouble" + suffix,
                            () -> sourceFloat(iterator, parallelMode, size).flatMapToDouble(v -> v == 2 ? null : DoubleStream.of((double) v)), filtered));
                    cases.add(new Pipeline("Float/asDoubleStream" + suffix, () -> sourceFloat(iterator, parallelMode, size).asDoubleStream(), expected));
                    cases.add(new Pipeline("Float/mapToObj" + suffix, () -> sourceFloat(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Float/flatMapToObj" + suffix,
                            () -> sourceFloat(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Float/boxed" + suffix, () -> sourceFloat(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Double/mapToInt" + suffix, () -> sourceDouble(iterator, parallelMode, size).mapToInt(v -> (int) v), expected));
                    cases.add(new Pipeline("Double/flatMapToInt" + suffix,
                            () -> sourceDouble(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v)), filtered));
                    cases.add(new Pipeline("Double/mapToLong" + suffix, () -> sourceDouble(iterator, parallelMode, size).mapToLong(v -> (long) v), expected));
                    cases.add(new Pipeline("Double/flatMapToLong" + suffix,
                            () -> sourceDouble(iterator, parallelMode, size).flatMapToLong(v -> v == 2 ? null : LongStream.of((long) v)), filtered));
                    cases.add(
                            new Pipeline("Double/mapToFloat" + suffix, () -> sourceDouble(iterator, parallelMode, size).mapToFloat(v -> (float) v), expected));
                    cases.add(new Pipeline("Double/flatMapToFloat" + suffix,
                            () -> sourceDouble(iterator, parallelMode, size).flatMapToFloat(v -> v == 2 ? null : FloatStream.of((float) v)), filtered));
                    cases.add(new Pipeline("Double/mapToObj" + suffix, () -> sourceDouble(iterator, parallelMode, size).mapToObj(v -> (int) v), expected));
                    cases.add(new Pipeline("Double/flatMapToObj" + suffix,
                            () -> sourceDouble(iterator, parallelMode, size).flatMapToObj(v -> v == 2 ? null : Stream.of((int) v)), filtered));
                    cases.add(new Pipeline("Double/boxed" + suffix, () -> sourceDouble(iterator, parallelMode, size).boxed(), expected));
                    cases.add(new Pipeline("Double/mapMulti" + suffix, () -> sourceDouble(iterator, parallelMode, size).mapMulti((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Object/mapToByte" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToByte(v -> (byte) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToByte" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToByte(v -> v == 2 ? null : ByteStream.of((byte) v.intValue())), filtered));
                    cases.add(new Pipeline("Object/mapToChar" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToChar(v -> (char) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToChar" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToChar(v -> v == 2 ? null : CharStream.of((char) v.intValue())), filtered));
                    cases.add(new Pipeline("Object/mapToShort" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToShort(v -> (short) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToShort" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToShort(v -> v == 2 ? null : ShortStream.of((short) v.intValue())),
                            filtered));
                    cases.add(new Pipeline("Object/mapToInt" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToInt(v -> (int) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToInt" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToInt(v -> v == 2 ? null : IntStream.of((int) v.intValue())), filtered));
                    cases.add(new Pipeline("Object/mapToLong" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToLong(v -> (long) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToLong" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToLong(v -> v == 2 ? null : LongStream.of((long) v.intValue())), filtered));
                    cases.add(new Pipeline("Object/mapToFloat" + suffix, () -> sourceObject(iterator, parallelMode, size).mapToFloat(v -> (float) v.intValue()),
                            expected));
                    cases.add(new Pipeline("Object/flatMapToFloat" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToFloat(v -> v == 2 ? null : FloatStream.of((float) v.intValue())),
                            filtered));
                    cases.add(new Pipeline("Object/mapToDouble" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).mapToDouble(v -> (double) v.intValue()), expected));
                    cases.add(new Pipeline("Object/flatMapToDouble" + suffix,
                            () -> sourceObject(iterator, parallelMode, size).flatMapToDouble(v -> v == 2 ? null : DoubleStream.of((double) v.intValue())),
                            filtered));
                    cases.add(new Pipeline("Object/mapMulti" + suffix, () -> sourceObject(iterator, parallelMode, size).<Integer> mapMulti((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Object/mapMultiToInt" + suffix, () -> sourceObject(iterator, parallelMode, size).mapMultiToInt((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Object/mapMultiToLong" + suffix, () -> sourceObject(iterator, parallelMode, size).mapMultiToLong((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                    cases.add(new Pipeline("Object/mapMultiToDouble" + suffix, () -> sourceObject(iterator, parallelMode, size).mapMultiToDouble((v, out) -> {
                        out.accept(v);
                        out.accept(v);
                    }), doubled));
                }
            }
        }
        return cases;
    }

    @ParameterizedTest
    @MethodSource("pipelines")
    void mappedValuesAndExhaustionMatchTheSourceContract(Pipeline pipeline) {
        try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = pipeline.factory.create()) {
            Iterator<?> iterator = stream.iterator();
            List<Integer> actual = new ArrayList<>();
            for (int i = 0; i < pipeline.expected.size(); i++) {
                Object value = iterator.next();
                actual.add(value instanceof Character ch ? (int) ch : ((Number) value).intValue());
            }
            assertThrows(NoSuchElementException.class, iterator::next);
            assertFalse(iterator.hasNext());
            Collections.sort(actual);
            assertEquals(pipeline.expected, actual);
        }
    }

    @ParameterizedTest
    @MethodSource("pipelines")
    void skipAndCountConsumeMappedValues(Pipeline pipeline) {
        for (long skip : new long[] { 0, 1, Long.MAX_VALUE }) {
            try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = pipeline.factory.create()) {
                assertEquals(Math.max(0L, pipeline.expected.size() - skip), stream.skip(skip).count());
            }
        }
    }

    private static ByteStream sourceByte(boolean iterator, int mode, int size) {
        byte[] values = new byte[size];
        for (int i = 0; i < size; i++)
            values[i] = (byte) (i + 1);
        ByteStream stream = iterator ? ByteStream.of(ByteIterator.of(values)) : ByteStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static CharStream sourceChar(boolean iterator, int mode, int size) {
        char[] values = new char[size];
        for (int i = 0; i < size; i++)
            values[i] = (char) (i + 1);
        CharStream stream = iterator ? CharStream.of(CharIterator.of(values)) : CharStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static ShortStream sourceShort(boolean iterator, int mode, int size) {
        short[] values = new short[size];
        for (int i = 0; i < size; i++)
            values[i] = (short) (i + 1);
        ShortStream stream = iterator ? ShortStream.of(ShortIterator.of(values)) : ShortStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static IntStream sourceInt(boolean iterator, int mode, int size) {
        int[] values = new int[size];
        for (int i = 0; i < size; i++)
            values[i] = (int) (i + 1);
        IntStream stream = iterator ? IntStream.of(IntIterator.of(values)) : IntStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static LongStream sourceLong(boolean iterator, int mode, int size) {
        long[] values = new long[size];
        for (int i = 0; i < size; i++)
            values[i] = (long) (i + 1);
        LongStream stream = iterator ? LongStream.of(LongIterator.of(values)) : LongStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static FloatStream sourceFloat(boolean iterator, int mode, int size) {
        float[] values = new float[size];
        for (int i = 0; i < size; i++)
            values[i] = (float) (i + 1);
        FloatStream stream = iterator ? FloatStream.of(FloatIterator.of(values)) : FloatStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static DoubleStream sourceDouble(boolean iterator, int mode, int size) {
        double[] values = new double[size];
        for (int i = 0; i < size; i++)
            values[i] = (double) (i + 1);
        DoubleStream stream = iterator ? DoubleStream.of(DoubleIterator.of(values)) : DoubleStream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }

    private static Stream<Integer> sourceObject(boolean iterator, int mode, int size) {
        Integer[] values = new Integer[size];
        for (int i = 0; i < size; i++)
            values[i] = i + 1;
        Stream<Integer> stream = iterator ? Stream.of(java.util.Arrays.asList(values).iterator()) : Stream.of(values);
        return mode == 0 ? stream : stream.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
    }
}
