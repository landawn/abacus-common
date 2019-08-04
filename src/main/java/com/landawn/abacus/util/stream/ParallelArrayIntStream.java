/*
 * Copyright (C) 2016, 2017, 2018, 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util.stream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.IntSummaryStatistics;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.IntTernaryOperator;
import com.landawn.abacus.util.function.IntToByteFunction;
import com.landawn.abacus.util.function.IntToCharFunction;
import com.landawn.abacus.util.function.IntToDoubleFunction;
import com.landawn.abacus.util.function.IntToFloatFunction;
import com.landawn.abacus.util.function.IntToLongFunction;
import com.landawn.abacus.util.function.IntToShortFunction;
import com.landawn.abacus.util.function.IntUnaryOperator;
import com.landawn.abacus.util.function.ObjIntConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;

/** 
 * 
 */
final class ParallelArrayIntStream extends ArrayIntStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private volatile ArrayIntStream sequential;
    private volatile Stream<Integer> boxed;

    ParallelArrayIntStream(final int[] values, final int fromIndex, final int toIndex, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExector, final Collection<Runnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = checkMaxThreadNum(maxThreadNum);
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExector == null ? DEFAULT_ASYNC_EXECUTOR : asyncExector;
    }

    @Override
    public IntStream filter(final IntPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.filter(predicate);
        }

        final Stream<Integer> stream = boxed().filter(new Predicate<Integer>() {
            @Override
            public boolean test(Integer value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream takeWhile(final IntPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.takeWhile(predicate);
        }

        final Stream<Integer> stream = boxed().takeWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream dropWhile(final IntPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.dropWhile(predicate);
        }

        final Stream<Integer> stream = boxed().dropWhile(new Predicate<Integer>() {
            @Override
            public boolean test(Integer value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream map(final IntUnaryOperator mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.map(mapper);
        }

        final IntStream stream = boxed().mapToInt(new ToIntFunction<Integer>() {
            @Override
            public int applyAsInt(Integer value) {
                return mapper.applyAsInt(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public CharStream mapToChar(final IntToCharFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToChar(mapper);
        }

        final CharStream stream = boxed().mapToChar(new ToCharFunction<Integer>() {
            @Override
            public char applyAsChar(Integer value) {
                return mapper.applyAsChar(value);
            }
        });

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream mapToByte(final IntToByteFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToByte(mapper);
        }

        final ByteStream stream = boxed().mapToByte(new ToByteFunction<Integer>() {
            @Override
            public byte applyAsByte(Integer value) {
                return mapper.applyAsByte(value);
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ShortStream mapToShort(final IntToShortFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToShort(mapper);
        }

        final ShortStream stream = boxed().mapToShort(new ToShortFunction<Integer>() {
            @Override
            public short applyAsShort(Integer value) {
                return mapper.applyAsShort(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public LongStream mapToLong(final IntToLongFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToLong(mapper);
        }

        final LongStream stream = boxed().mapToLong(new ToLongFunction<Integer>() {
            @Override
            public long applyAsLong(Integer value) {
                return mapper.applyAsLong(value);
            }
        });

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream mapToFloat(final IntToFloatFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToFloat(mapper);
        }

        final FloatStream stream = boxed().mapToFloat(new ToFloatFunction<Integer>() {
            @Override
            public float applyAsFloat(Integer value) {
                return mapper.applyAsFloat(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public DoubleStream mapToDouble(final IntToDoubleFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToDouble(mapper);
        }

        final DoubleStream stream = boxed().mapToDouble(new ToDoubleFunction<Integer>() {
            @Override
            public double applyAsDouble(Integer value) {
                return mapper.applyAsDouble(value);
            }
        });

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <U> Stream<U> mapToObj(final IntFunction<? extends U> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToObj(mapper);
        }

        return boxed().map(new Function<Integer, U>() {
            @Override
            public U apply(Integer value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public IntStream flatMap(final IntFunction<? extends IntStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorIntStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final IntStream stream = boxed().flatMapToInt(new Function<Integer, IntStream>() {
            @Override
            public IntStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public CharStream flatMapToChar(final IntFunction<? extends CharStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorCharStream(sequential().flatMapToChar(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final CharStream stream = boxed().flatMapToChar(new Function<Integer, CharStream>() {
            @Override
            public CharStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorCharStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public ByteStream flatMapToByte(final IntFunction<? extends ByteStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorByteStream(sequential().flatMapToByte(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final ByteStream stream = boxed().flatMapToByte(new Function<Integer, ByteStream>() {
            @Override
            public ByteStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public ShortStream flatMapToShort(final IntFunction<? extends ShortStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorShortStream(sequential().flatMapToShort(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final ShortStream stream = boxed().flatMapToShort(new Function<Integer, ShortStream>() {
            @Override
            public ShortStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorShortStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public LongStream flatMapToLong(final IntFunction<? extends LongStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final LongStream stream = boxed().flatMapToLong(new Function<Integer, LongStream>() {
            @Override
            public LongStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public FloatStream flatMapToFloat(final IntFunction<? extends FloatStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorFloatStream(sequential().flatMapToFloat(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final FloatStream stream = boxed().flatMapToFloat(new Function<Integer, FloatStream>() {
            @Override
            public FloatStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public DoubleStream flatMapToDouble(final IntFunction<? extends DoubleStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final DoubleStream stream = boxed().flatMapToDouble(new Function<Integer, DoubleStream>() {
            @Override
            public DoubleStream apply(Integer value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final IntFunction<? extends Stream<T>> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, null);
        }

        return boxed().flatMap(new Function<Integer, Stream<T>>() {
            @Override
            public Stream<T> apply(Integer value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public IntStream peek(final IntConsumer action) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.peek(action);
        }

        final IntStream stream = boxed().peek(new Consumer<Integer>() {
            @Override
            public void accept(Integer t) {
                action.accept(t);
            }
        }).sequential().mapToInt(ToIntFunction.UNBOX);

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <E extends Exception> void forEach(final Try.IntConsumer<E> action) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            super.forEach(action);
            return;
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                action.accept(elements[cursor++]);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {

                    @Override
                    public void run() {
                        int next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                action.accept(next);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(final IntFunction<? extends K> keyMapper, final IntFunction<? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Function<? super Integer, ? extends K> keyMapper2 = new Function<Integer, K>() {
            @Override
            public K apply(Integer value) {
                return keyMapper.apply(value);
            }
        };

        final Function<? super Integer, ? extends V> valueMapper2 = new Function<Integer, V>() {
            @Override
            public V apply(Integer value) {
                return valueMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final IntFunction<? extends K> keyMapper, final Collector<Integer, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, downstream, mapFactory);
        }

        final Function<? super Integer, ? extends K> keyMapper2 = new Function<Integer, K>() {
            @Override
            public K apply(Integer value) {
                return keyMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, downstream, mapFactory);
    }

    @Override
    public int reduce(final int identity, final IntBinaryOperator op) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(identity, op);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        int result = identity;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = op.applyAsInt(result, elements[cursor++]);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<Integer>() {

                    @Override
                    public Integer call() {
                        int result = identity;
                        int next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = op.applyAsInt(result, next);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            close();
            throw N.toRuntimeException(eHolder.value());
        }

        Integer result = null;

        try {
            for (ContinuableFuture<Integer> future : futureList) {
                if (result == null) {
                    result = future.get();
                } else {
                    result = op.applyAsInt(result, future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return result == null ? identity : result;
    }

    @Override
    public OptionalInt reduce(final IntBinaryOperator accumulator) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Integer>() {
                    @Override
                    public Integer call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        if (cursor >= to) {
                            return null;
                        }

                        int result = elements[cursor++];

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = accumulator.applyAsInt(result, elements[cursor++]);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<Integer>() {

                    @Override
                    public Integer call() {
                        int result = 0;

                        synchronized (elements) {
                            if (cursor.intValue() < toIndex) {
                                result = elements[cursor.getAndIncrement()];
                            } else {
                                return null;
                            }
                        }

                        int next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = accumulator.applyAsInt(result, next);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return result;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            close();
            throw N.toRuntimeException(eHolder.value());
        }

        Integer result = null;

        try {
            for (ContinuableFuture<Integer> future : futureList) {
                final Integer tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsInt(result, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return result == null ? OptionalInt.empty() : OptionalInt.of(result);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjIntConsumer<? super R> accumulator, final BiConsumer<R, R> combiner) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.collect(supplier, accumulator, combiner);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<R>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<R>() {
                    @Override
                    public R call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        final R container = supplier.get();

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                accumulator.accept(container, elements[cursor++]);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return container;
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Callable<R>() {

                    @Override
                    public R call() {
                        final R container = supplier.get();
                        int next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                accumulator.accept(container, next);
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }

                        return container;
                    }
                }));
            }
        }

        if (eHolder.value() != null) {
            close();
            throw N.toRuntimeException(eHolder.value());
        }

        R container = (R) NONE;

        try {
            for (ContinuableFuture<R> future : futureList) {
                if (container == NONE) {
                    container = future.get();
                } else {
                    combiner.accept(container, future.get());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return container == NONE ? supplier.get() : container;
    }

    @Override
    public OptionalInt min() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalInt.empty();
            } else if (sorted) {
                return OptionalInt.of(elements[fromIndex]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalInt.of(N.min(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Integer>() {
                @Override
                public Integer call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    return cursor >= to ? null : N.min(elements, cursor, to);
                }
            }));
        }

        Integer candidate = null;

        try {
            for (ContinuableFuture<Integer> future : futureList) {
                final Integer tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.intValue() < candidate.intValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalInt.empty() : OptionalInt.of(candidate);
    }

    @Override
    public OptionalInt max() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalInt.empty();
            } else if (sorted) {
                return OptionalInt.of(elements[toIndex - 1]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalInt.of(N.max(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Integer>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Integer>() {
                @Override
                public Integer call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    return cursor >= to ? null : N.max(elements, cursor, to);
                }
            }));
        }

        Integer candidate = null;

        try {
            for (ContinuableFuture<Integer> future : futureList) {
                final Integer tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.intValue() > candidate.intValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalInt.empty() : OptionalInt.of(candidate);
    }

    @Override
    public int sum() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return 0;
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return sum(elements, fromIndex, toIndex);
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Long>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Long>() {
                @Override
                public Long call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    if (cursor >= to) {
                        return null;
                    } else {
                        long sum = 0;

                        for (int i = cursor; i < to; i++) {
                            sum += elements[i];
                        }

                        return sum;
                    }
                }
            }));
        }

        long result = 0;

        try {
            for (ContinuableFuture<Long> future : futureList) {
                final Long tmp = future.get();

                if (tmp == null) {
                    continue;
                } else {
                    result += tmp.longValue();
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return N.toIntExact(result);
    }

    @Override
    public IntSummaryStatistics summarize() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return new IntSummaryStatistics();
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return super.summarize();
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<IntSummaryStatistics>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<IntSummaryStatistics>() {
                @Override
                public IntSummaryStatistics call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final IntSummaryStatistics result = new IntSummaryStatistics();

                    for (int i = cursor; i < to; i++) {
                        result.accept(elements[i]);
                    }

                    return result;
                }
            }));
        }

        IntSummaryStatistics result = null;

        try {
            for (ContinuableFuture<IntSummaryStatistics> future : futureList) {
                final IntSummaryStatistics tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result.combine(tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return result;
    }

    @Override
    public <E extends Exception> boolean anyMatch(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.anyMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(false);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isFalse() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++])) {
                                    result.setTrue();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {

                    @Override
                    public void run() {
                        int next = 0;

                        try {
                            while (result.isFalse() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    result.setTrue();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return result.value();
    }

    @Override
    public <E extends Exception> boolean allMatch(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.allMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isTrue() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++]) == false) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {

                    @Override
                    public void run() {
                        int next = 0;

                        try {
                            while (result.isTrue() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next) == false) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return result.value();
    }

    @Override
    public <E extends Exception> boolean noneMatch(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.noneMatch(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final MutableBoolean result = MutableBoolean.of(true);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        try {
                            while (cursor < to && result.isTrue() && eHolder.value() == null) {
                                if (predicate.test(elements[cursor++])) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {

                    @Override
                    public void run() {
                        int next = 0;

                        try {
                            while (result.isTrue() && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    result.setFalse();
                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return result.value();
    }

    @Override
    public <E extends Exception> OptionalInt findFirst(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Integer>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        final Pair<Integer, Integer> pair = new Pair<>();

                        try {
                            while (cursor < to && (resultHolder.value() == null || cursor < resultHolder.value().left) && eHolder.value() == null) {
                                pair.left = cursor;
                                pair.right = elements[cursor++];

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left < resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        final Pair<Integer, Integer> pair = new Pair<>();

                        try {
                            while (resultHolder.value() == null && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        pair.left = cursor.intValue();
                                        pair.right = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left < resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalInt findLast(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Integer>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        final int from = fromIndex + sliceIndex * sliceSize;
                        int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                        final Pair<Integer, Integer> pair = new Pair<>();

                        try {
                            while (cursor > from && (resultHolder.value() == null || cursor > resultHolder.value().left) && eHolder.value() == null) {
                                pair.left = cursor;
                                pair.right = elements[--cursor];

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left > resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(toIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        final Pair<Integer, Integer> pair = new Pair<>();

                        try {
                            while (resultHolder.value() == null && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() > fromIndex) {
                                        pair.left = cursor.intValue();
                                        pair.right = elements[cursor.decrementAndGet()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(pair.right)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == null || pair.left > resultHolder.value().left) {
                                            resultHolder.setValue(pair.copy());
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return resultHolder.value() == null ? OptionalInt.empty() : OptionalInt.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalInt findAny(final Try.IntPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findAny(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Object> resultHolder = Holder.of(NONE);

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        int next = 0;

                        try {
                            while (cursor < to && resultHolder.value() == NONE && eHolder.value() == null) {
                                next = elements[cursor++];

                                if (predicate.test(next)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == NONE) {
                                            resultHolder.setValue(next);
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        } else {
            final MutableInt cursor = MutableInt.of(fromIndex);

            for (int i = 0; i < threadNum; i++) {
                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int next = 0;

                        try {
                            while (resultHolder.value() == NONE && eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                if (predicate.test(next)) {
                                    synchronized (resultHolder) {
                                        if (resultHolder.value() == NONE) {
                                            resultHolder.setValue(next);
                                        }
                                    }

                                    break;
                                }
                            }
                        } catch (Exception e) {
                            setError(eHolder, e);
                        }
                    }
                }));
            }
        }

        try {
            complette(futureList, eHolder, (E) null);
        } finally {
            close();
        }

        return resultHolder.value() == NONE ? OptionalInt.empty() : OptionalInt.of((Integer) resultHolder.value());
    }

    @Override
    public Stream<Integer> boxed() {
        Stream<Integer> tmp = boxed;

        if (tmp == null) {
            tmp = new ParallelIteratorStream<Integer>(iteratorEx(), sorted, sorted ? INT_COMPARATOR : null, maxThreadNum, splitor, asyncExecutor,
                    closeHandlers);
            boxed = tmp;
        }

        return tmp;
    }

    @Override
    public IntStream append(IntStream stream) {
        return new ParallelIteratorIntStream(IntStream.concat(this, stream), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream prepend(IntStream stream) {
        return new ParallelIteratorIntStream(IntStream.concat(stream, this), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream merge(final IntStream b, final IntBiFunction<Nth> nextSelector) {
        return new ParallelIteratorIntStream(IntStream.merge(this, b, nextSelector), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream zipWith(IntStream b, IntBinaryOperator zipFunction) {
        return new ParallelIteratorIntStream(IntStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream zipWith(IntStream b, IntStream c, IntTernaryOperator zipFunction) {
        return new ParallelIteratorIntStream(IntStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream zipWith(IntStream b, int valueForNoneA, int valueForNoneB, IntBinaryOperator zipFunction) {
        return new ParallelIteratorIntStream(IntStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                closeHandlers);
    }

    @Override
    public IntStream zipWith(IntStream b, IntStream c, int valueForNoneA, int valueForNoneB, int valueForNoneC, IntTernaryOperator zipFunction) {
        return new ParallelIteratorIntStream(IntStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum, splitor,
                asyncExecutor, closeHandlers);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public IntStream sequential() {
        ArrayIntStream tmp = sequential;

        if (tmp == null) {
            tmp = new ArrayIntStream(elements, fromIndex, toIndex, sorted, closeHandlers);
            sequential = tmp;
        }

        return tmp;
    }

    @Override
    public IntStream parallel(int maxThreadNum, Splitor splitor) {
        checkMaxThreadNum(maxThreadNum);
        checkSplitor(splitor);

        return new ParallelArrayIntStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    protected int maxThreadNum() {
        return maxThreadNum;
    }

    @Override
    protected BaseStream.Splitor splitor() {
        return splitor;
    }

    @Override
    protected AsyncExecutor asyncExecutor() {
        return asyncExecutor;
    }

    @Override
    public IntStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ParallelArrayIntStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, newCloseHandlers);
    }
}
