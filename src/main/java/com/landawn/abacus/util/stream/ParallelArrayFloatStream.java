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
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.KahanSummation;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.FloatTernaryOperator;
import com.landawn.abacus.util.function.FloatToDoubleFunction;
import com.landawn.abacus.util.function.FloatToIntFunction;
import com.landawn.abacus.util.function.FloatToLongFunction;
import com.landawn.abacus.util.function.FloatUnaryOperator;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjFloatConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;

/**
 * 
 */
final class ParallelArrayFloatStream extends ArrayFloatStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private volatile ArrayFloatStream sequential;
    private volatile Stream<Float> boxed;

    ParallelArrayFloatStream(final float[] values, final int fromIndex, final int toIndex, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExector, final Collection<Runnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = checkMaxThreadNum(maxThreadNum);
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExector == null ? DEFAULT_ASYNC_EXECUTOR : asyncExector;
    }

    @Override
    public FloatStream filter(final FloatPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.filter(predicate);
        }

        final Stream<Float> stream = boxed().filter(new Predicate<Float>() {
            @Override
            public boolean test(Float value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream takeWhile(final FloatPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.takeWhile(predicate);
        }

        final Stream<Float> stream = boxed().takeWhile(new Predicate<Float>() {
            @Override
            public boolean test(Float value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream dropWhile(final FloatPredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.dropWhile(predicate);
        }

        final Stream<Float> stream = boxed().dropWhile(new Predicate<Float>() {
            @Override
            public boolean test(Float value) {
                return predicate.test(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream map(final FloatUnaryOperator mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.map(mapper);
        }

        final FloatStream stream = boxed().mapToFloat(new ToFloatFunction<Float>() {
            @Override
            public float applyAsFloat(Float value) {
                return mapper.applyAsFloat(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream mapToInt(final FloatToIntFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToInt(mapper);
        }

        final IntStream stream = boxed().mapToInt(new ToIntFunction<Float>() {
            @Override
            public int applyAsInt(Float value) {
                return mapper.applyAsInt(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public LongStream mapToLong(final FloatToLongFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToLong(mapper);
        }

        final LongStream stream = boxed().mapToLong(new ToLongFunction<Float>() {
            @Override
            public long applyAsLong(Float value) {
                return mapper.applyAsLong(value);
            }
        });

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public DoubleStream mapToDouble(final FloatToDoubleFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToDouble(mapper);
        }

        final DoubleStream stream = boxed().mapToDouble(new ToDoubleFunction<Float>() {
            @Override
            public double applyAsDouble(Float value) {
                return mapper.applyAsDouble(value);
            }
        });

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <U> Stream<U> mapToObj(final FloatFunction<? extends U> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToObj(mapper);
        }

        return boxed().map(new Function<Float, U>() {
            @Override
            public U apply(Float value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public FloatStream flatMap(final FloatFunction<? extends FloatStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorFloatStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final FloatStream stream = boxed().flatMapToFloat(new Function<Float, FloatStream>() {
            @Override
            public FloatStream apply(Float value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public IntStream flatMapToInt(final FloatFunction<? extends IntStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final IntStream stream = boxed().flatMapToInt(new Function<Float, IntStream>() {
            @Override
            public IntStream apply(Float value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public LongStream flatMapToLong(final FloatFunction<? extends LongStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorLongStream(sequential().flatMapToLong(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final LongStream stream = boxed().flatMapToLong(new Function<Float, LongStream>() {
            @Override
            public LongStream apply(Float value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorLongStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public DoubleStream flatMapToDouble(final FloatFunction<? extends DoubleStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorDoubleStream(sequential().flatMapToDouble(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final DoubleStream stream = boxed().flatMapToDouble(new Function<Float, DoubleStream>() {
            @Override
            public DoubleStream apply(Float value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorDoubleStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final FloatFunction<? extends Stream<T>> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, null);
        }

        return boxed().flatMap(new Function<Float, Stream<T>>() {
            @Override
            public Stream<T> apply(Float value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public FloatStream peek(final FloatConsumer action) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.peek(action);
        }

        final FloatStream stream = boxed().peek(new Consumer<Float>() {
            @Override
            public void accept(Float t) {
                action.accept(t);
            }
        }).sequential().mapToFloat(ToFloatFunction.UNBOX);

        return new ParallelIteratorFloatStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <E extends Exception> void forEach(final Try.FloatConsumer<E> action) throws E {
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
                        float next = 0;

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
    public <K, V, M extends Map<K, V>> M toMap(final FloatFunction<? extends K> keyMapper, final FloatFunction<? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Function<? super Float, ? extends K> keyMapper2 = new Function<Float, K>() {
            @Override
            public K apply(Float value) {
                return keyMapper.apply(value);
            }
        };

        final Function<? super Float, ? extends V> valueMapper2 = new Function<Float, V>() {
            @Override
            public V apply(Float value) {
                return valueMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final FloatFunction<? extends K> keyMapper, final Collector<Float, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, downstream, mapFactory);
        }

        final Function<? super Float, ? extends K> keyMapper2 = new Function<Float, K>() {
            @Override
            public K apply(Float value) {
                return keyMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, downstream, mapFactory);
    }

    @Override
    public float reduce(final float identity, final FloatBinaryOperator op) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(identity, op);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Float>() {
                    @Override
                    public Float call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        float result = identity;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = op.applyAsFloat(result, elements[cursor++]);
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
                futureList.add(asyncExecutor.execute(new Callable<Float>() {
                    @Override
                    public Float call() {
                        float result = identity;
                        float next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = op.applyAsFloat(result, next);
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

        Float result = null;

        try {
            for (ContinuableFuture<Float> future : futureList) {
                if (result == null) {
                    result = future.get();
                } else {
                    result = op.applyAsFloat(result, future.get());
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
    public OptionalFloat reduce(final FloatBinaryOperator accumulator) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Float>() {
                    @Override
                    public Float call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        if (cursor >= to) {
                            return null;
                        }

                        float result = elements[cursor++];

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = accumulator.applyAsFloat(result, elements[cursor++]);
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
                futureList.add(asyncExecutor.execute(new Callable<Float>() {
                    @Override
                    public Float call() {
                        float result = 0;

                        synchronized (elements) {
                            if (cursor.intValue() < toIndex) {
                                result = elements[cursor.getAndIncrement()];
                            } else {
                                return null;
                            }
                        }

                        float next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = accumulator.applyAsFloat(result, next);
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

        Float result = null;

        try {
            for (ContinuableFuture<Float> future : futureList) {
                final Float tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsFloat(result, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return result == null ? OptionalFloat.empty() : OptionalFloat.of(result);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjFloatConsumer<? super R> accumulator, final BiConsumer<R, R> combiner) {
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
                        float next = 0;

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
    public OptionalFloat min() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalFloat.empty();
            } else if (sorted) {
                return OptionalFloat.of(elements[fromIndex]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalFloat.of(N.min(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Float>() {
                @Override
                public Float call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    return cursor >= to ? null : N.min(elements, cursor, to);
                }
            }));
        }

        Float candidate = null;

        try {
            for (ContinuableFuture<Float> future : futureList) {
                final Float tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || N.compare(tmp.floatValue(), candidate.floatValue()) < 0) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalFloat.empty() : OptionalFloat.of(candidate);
    }

    @Override
    public OptionalFloat max() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalFloat.empty();
            } else if (sorted) {
                return OptionalFloat.of(elements[toIndex - 1]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalFloat.of(N.max(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Float>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Float>() {
                @Override
                public Float call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    return cursor >= to ? null : N.max(elements, cursor, to);
                }
            }));
        }

        Float candidate = null;

        try {
            for (ContinuableFuture<Float> future : futureList) {
                final Float tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || N.compare(tmp.floatValue(), candidate.floatValue()) > 0) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalFloat.empty() : OptionalFloat.of(candidate);
    }

    @Override
    public double sum() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return 0d;
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return super.sum();
            }

            return summation().sum();
        } finally {
            close();
        }
    }

    @Override
    public OptionalDouble average() {
        assertNotClosed();

        try {
            if (fromIndex == toIndex) {
                return OptionalDouble.empty();
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return super.average();
            }

            return summation().average();
        } finally {
            close();
        }
    }

    private KahanSummation summation() {
        final Supplier<KahanSummation> supplier = new Supplier<KahanSummation>() {
            @Override
            public KahanSummation get() {
                return new KahanSummation();
            }
        };

        final ObjFloatConsumer<KahanSummation> accumulator = new ObjFloatConsumer<KahanSummation>() {
            @Override
            public void accept(KahanSummation a, float value) {
                a.add(value);
            }
        };

        final BiConsumer<KahanSummation, KahanSummation> combiner = new BiConsumer<KahanSummation, KahanSummation>() {
            @Override
            public void accept(KahanSummation a, KahanSummation b) {
                a.combine(b);
            }
        };

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<KahanSummation>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<KahanSummation>() {
                @Override
                public KahanSummation call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    KahanSummation container = supplier.get();

                    while (cursor < to) {
                        accumulator.accept(container, elements[cursor++]);
                    }

                    return container;
                }
            }));
        }

        KahanSummation summation = null;

        try {
            for (ContinuableFuture<KahanSummation> future : futureList) {
                final KahanSummation tmp = future.get();

                if (summation == null) {
                    summation = tmp;
                } else {
                    combiner.accept(summation, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }
        return summation;
    }

    @Override
    public FloatSummaryStatistics summarize() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return new FloatSummaryStatistics();
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
        final List<ContinuableFuture<FloatSummaryStatistics>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<FloatSummaryStatistics>() {
                @Override
                public FloatSummaryStatistics call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final FloatSummaryStatistics result = new FloatSummaryStatistics();

                    for (int i = cursor; i < to; i++) {
                        result.accept(elements[i]);
                    }

                    return result;
                }
            }));
        }

        FloatSummaryStatistics result = null;

        try {
            for (ContinuableFuture<FloatSummaryStatistics> future : futureList) {
                final FloatSummaryStatistics tmp = future.get();

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
    public <E extends Exception> boolean anyMatch(final Try.FloatPredicate<E> predicate) throws E {
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
                        float next = 0;

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
    public <E extends Exception> boolean allMatch(final Try.FloatPredicate<E> predicate) throws E {
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
                        float next = 0;

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
    public <E extends Exception> boolean noneMatch(final Try.FloatPredicate<E> predicate) throws E {
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
                        float next = 0;

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
    public <E extends Exception> OptionalFloat findFirst(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Float>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        final Pair<Integer, Float> pair = new Pair<>();

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
                        final Pair<Integer, Float> pair = new Pair<>();

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

        return resultHolder.value() == null ? OptionalFloat.empty() : OptionalFloat.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalFloat findLast(final Try.FloatPredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Float>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        final int from = fromIndex + sliceIndex * sliceSize;
                        int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                        final Pair<Integer, Float> pair = new Pair<>();

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
                        final Pair<Integer, Float> pair = new Pair<>();

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

        return resultHolder.value() == null ? OptionalFloat.empty() : OptionalFloat.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalFloat findAny(final Try.FloatPredicate<E> predicate) throws E {
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
                        float next = 0;

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
                        float next = 0;

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

        return resultHolder.value() == NONE ? OptionalFloat.empty() : OptionalFloat.of((Float) resultHolder.value());
    }

    @Override
    public Stream<Float> boxed() {
        Stream<Float> tmp = boxed;

        if (tmp == null) {
            tmp = new ParallelIteratorStream<Float>(iteratorEx(), sorted, sorted ? FLOAT_COMPARATOR : null, maxThreadNum, splitor, asyncExecutor,
                    closeHandlers);
            boxed = tmp;
        }

        return tmp;
    }

    @Override
    public FloatStream append(FloatStream stream) {
        return new ParallelIteratorFloatStream(FloatStream.concat(this, stream), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream prepend(FloatStream stream) {
        return new ParallelIteratorFloatStream(FloatStream.concat(stream, this), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream merge(final FloatStream b, final FloatBiFunction<Nth> nextSelector) {
        return new ParallelIteratorFloatStream(FloatStream.merge(this, b, nextSelector), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatBinaryOperator zipFunction) {
        return new ParallelIteratorFloatStream(FloatStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatStream c, FloatTernaryOperator zipFunction) {
        return new ParallelIteratorFloatStream(FloatStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public FloatStream zipWith(FloatStream b, float valueForNoneA, float valueForNoneB, FloatBinaryOperator zipFunction) {
        return new ParallelIteratorFloatStream(FloatStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                closeHandlers);
    }

    @Override
    public FloatStream zipWith(FloatStream b, FloatStream c, float valueForNoneA, float valueForNoneB, float valueForNoneC, FloatTernaryOperator zipFunction) {
        return new ParallelIteratorFloatStream(FloatStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public FloatStream sequential() {
        ArrayFloatStream tmp = sequential;

        if (tmp == null) {
            tmp = new ArrayFloatStream(elements, fromIndex, toIndex, sorted, closeHandlers);
            sequential = tmp;
        }

        return tmp;
    }

    @Override
    public FloatStream parallel(int maxThreadNum, Splitor splitor) {
        checkMaxThreadNum(maxThreadNum);
        checkSplitor(splitor);

        return new ParallelArrayFloatStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, closeHandlers);
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
    public FloatStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ParallelArrayFloatStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, newCloseHandlers);
    }
}
