package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.*;
import com.landawn.abacus.util.stream.BaseStream.ParallelSettings;
import com.landawn.abacus.util.stream.BaseStream.SplitStrategy;

public class StreamTerminalSourceFailureTest extends TestBase {
    record Source(Class<?> api, Function<RuntimeException, BaseStream<?, ?, ?, ?, ?, ?, ?, ?>> create) {
    }

    record Terminal(Source source, Method method, int mode) {
        @Override
        public String toString() {
            return source.api.getSimpleName() + "/" + method.getName() + "/parallel=" + mode;
        }
    }

    static List<Terminal> terminals() {
        List<Source> sources = List.of(new Source(ByteStream.class, failure -> ByteStream.of(new ByteIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public byte nextByte() {
                throw failure;
            }
        })), new Source(CharStream.class, failure -> CharStream.of(new CharIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public char nextChar() {
                throw failure;
            }
        })), new Source(ShortStream.class, failure -> ShortStream.of(new ShortIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public short nextShort() {
                throw failure;
            }
        })), new Source(IntStream.class, failure -> IntStream.of(new IntIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public int nextInt() {
                throw failure;
            }
        })), new Source(LongStream.class, failure -> LongStream.of(new LongIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public long nextLong() {
                throw failure;
            }
        })), new Source(FloatStream.class, failure -> FloatStream.of(new FloatIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public float nextFloat() {
                throw failure;
            }
        })), new Source(DoubleStream.class, failure -> DoubleStream.of(new DoubleIterator() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public double nextDouble() {
                throw failure;
            }
        })), new Source(Stream.class, failure -> Stream.of(new java.util.Iterator<Integer>() {
            @Override
            public boolean hasNext() {
                throw failure;
            }

            @Override
            public Integer next() {
                throw failure;
            }
        })));
        Set<String> operations = Set.of("count", "sum", "average", "min", "max", "first", "last", "findFirst", "findAny", "onlyOne", "toArray", "toList",
                "toSet", "toByteList", "toCharList", "toShortList", "toIntList", "toLongList", "toFloatList", "toDoubleList", "summaryStatistics",
                "summaryStatisticsAndPercentiles", "toMultiset", "toLongMultiset", "toImmutableList", "toImmutableSet", "percentiles", "median");
        List<Terminal> cases = new ArrayList<>();
        for (Source source : sources) {
            Arrays.stream(source.api.getMethods())
                    .filter(m -> m.getParameterCount() == 0 && operations.contains(m.getName()) && !m.isBridge() && !m.isSynthetic())
                    .sorted(java.util.Comparator.comparing(Method::getName))
                    .forEach(method -> {
                        for (int mode = 0; mode < 4; mode++)
                            cases.add(new Terminal(source, method, mode));
                    });
        }
        return cases;
    }

    @ParameterizedTest
    @MethodSource("terminals")
    void sourceFailurePropagatesAndClosesEveryTerminal(Terminal terminal) {
        AtomicInteger closes = new AtomicInteger();
        IllegalArgumentException failure = new IllegalArgumentException("source failed");
        BaseStream<?, ?, ?, ?, ?, ?, ?, ?> original = terminal.source.create.apply(failure).onClose(closes::incrementAndGet);
        BaseStream<?, ?, ?, ?, ?, ?, ?, ?> stream = terminal.mode == 0 ? original
                : original.parallel(new ParallelSettings(terminal.mode == 1 ? 1 : 3, terminal.mode == 3 ? SplitStrategy.ITERATOR : SplitStrategy.ARRAY, null));
        try (stream) {
            Throwable actual = assertThrows(RuntimeException.class, () -> {
                try {
                    terminal.method.invoke(stream);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
            assertSame(failure, actual);
            assertEquals(1, closes.get());
            assertThrows(IllegalStateException.class, stream::count);
        }
        assertEquals(1, closes.get());
    }
}
