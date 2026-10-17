package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.*;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

public class StreamDerivedFactoryContractTest extends TestBase {
    record Factory(String name, Function<Stream<Integer>, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> create) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Factory> factories() {
        return List.of(new Factory("Byte/array", s -> s.newStream(new byte[] { 1, 2, 3 })),
                new Factory("Byte/sortedArray", s -> s.newStream(new byte[] { 1, 2, 3 }, true)),
                new Factory("Byte/slice", s -> s.newStream(new byte[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Byte/sortedSlice", s -> s.newStream(new byte[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Byte/iterator", s -> s.newStream(ByteIterator.of(new byte[] { 1, 2, 3 }))),
                new Factory("Byte/sortedIterator", s -> s.newStream(ByteIterator.of(new byte[] { 1, 2, 3 }), true)),
                new Factory("Char/array", s -> s.newStream(new char[] { 1, 2, 3 })),
                new Factory("Char/sortedArray", s -> s.newStream(new char[] { 1, 2, 3 }, true)),
                new Factory("Char/slice", s -> s.newStream(new char[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Char/sortedSlice", s -> s.newStream(new char[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Char/iterator", s -> s.newStream(CharIterator.of(new char[] { 1, 2, 3 }))),
                new Factory("Char/sortedIterator", s -> s.newStream(CharIterator.of(new char[] { 1, 2, 3 }), true)),
                new Factory("Short/array", s -> s.newStream(new short[] { 1, 2, 3 })),
                new Factory("Short/sortedArray", s -> s.newStream(new short[] { 1, 2, 3 }, true)),
                new Factory("Short/slice", s -> s.newStream(new short[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Short/sortedSlice", s -> s.newStream(new short[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Short/iterator", s -> s.newStream(ShortIterator.of(new short[] { 1, 2, 3 }))),
                new Factory("Short/sortedIterator", s -> s.newStream(ShortIterator.of(new short[] { 1, 2, 3 }), true)),
                new Factory("Int/array", s -> s.newStream(new int[] { 1, 2, 3 })),
                new Factory("Int/sortedArray", s -> s.newStream(new int[] { 1, 2, 3 }, true)),
                new Factory("Int/slice", s -> s.newStream(new int[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Int/sortedSlice", s -> s.newStream(new int[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Int/iterator", s -> s.newStream(IntIterator.of(new int[] { 1, 2, 3 }))),
                new Factory("Int/sortedIterator", s -> s.newStream(IntIterator.of(new int[] { 1, 2, 3 }), true)),
                new Factory("Long/array", s -> s.newStream(new long[] { 1, 2, 3 })),
                new Factory("Long/sortedArray", s -> s.newStream(new long[] { 1, 2, 3 }, true)),
                new Factory("Long/slice", s -> s.newStream(new long[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Long/sortedSlice", s -> s.newStream(new long[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Long/iterator", s -> s.newStream(LongIterator.of(new long[] { 1, 2, 3 }))),
                new Factory("Long/sortedIterator", s -> s.newStream(LongIterator.of(new long[] { 1, 2, 3 }), true)),
                new Factory("Float/array", s -> s.newStream(new float[] { 1, 2, 3 })),
                new Factory("Float/sortedArray", s -> s.newStream(new float[] { 1, 2, 3 }, true)),
                new Factory("Float/slice", s -> s.newStream(new float[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Float/sortedSlice", s -> s.newStream(new float[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Float/iterator", s -> s.newStream(FloatIterator.of(new float[] { 1, 2, 3 }))),
                new Factory("Float/sortedIterator", s -> s.newStream(FloatIterator.of(new float[] { 1, 2, 3 }), true)),
                new Factory("Double/array", s -> s.newStream(new double[] { 1, 2, 3 })),
                new Factory("Double/sortedArray", s -> s.newStream(new double[] { 1, 2, 3 }, true)),
                new Factory("Double/slice", s -> s.newStream(new double[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Double/sortedSlice", s -> s.newStream(new double[] { 0, 1, 2, 3, 4 }, 1, 4, true)),
                new Factory("Double/iterator", s -> s.newStream(DoubleIterator.of(new double[] { 1, 2, 3 }))),
                new Factory("Double/sortedIterator", s -> s.newStream(DoubleIterator.of(new double[] { 1, 2, 3 }), true)),
                new Factory("Object/array", s -> s.newStream(new Integer[] { 1, 2, 3 })),
                new Factory("Object/slice", s -> s.newStream(new Integer[] { 0, 1, 2, 3, 4 }, 1, 4)),
                new Factory("Object/stream", s -> s.newStream(Stream.of(1, 2, 3))));
    }

    @ParameterizedTest
    @MethodSource("factories")
    void derivedStreamInheritsParallelModeAndOwnsSourceCleanup(Factory factory) {
        for (int mode = 0; mode < 4; mode++) {
            AtomicInteger closes = new AtomicInteger();
            Stream<Integer> source = Stream.of(99).onClose(closes::incrementAndGet);
            Stream<Integer> parent = mode == 0 ? source
                    : source.parallel(new ParallelSettings(mode == 1 ? 1 : 3, mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
            try (parent) {
                try (BaseStream<?, ?, ?, ?, ?, ?, ?, ?> child = factory.create.apply(parent)) {
                    assertEquals(parent.isParallel(), child.isParallel());
                    Iterator<?> iterator = child.iterator();
                    List<Integer> actual = new ArrayList<>();
                    while (iterator.hasNext()) {
                        Object value = iterator.next();
                        actual.add(value instanceof Character ch ? (int) ch : ((Number) value).intValue());
                    }
                    assertEquals(List.of(1, 2, 3), actual);
                    assertEquals(0, closes.get());
                }
                assertEquals(1, closes.get());
                assertThrows(IllegalStateException.class, parent::count);
            }
            assertEquals(1, closes.get());
        }
    }
}
