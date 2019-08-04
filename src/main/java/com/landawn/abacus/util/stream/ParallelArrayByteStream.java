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
import com.landawn.abacus.util.ByteSummaryStatistics;
import com.landawn.abacus.util.ContinuableFuture;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Holder;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.ByteTernaryOperator;
import com.landawn.abacus.util.function.ByteToIntFunction;
import com.landawn.abacus.util.function.ByteUnaryOperator;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.ObjByteConsumer;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToIntFunction;

/** 
 * 
 */
final class ParallelArrayByteStream extends ArrayByteStream {
    private final int maxThreadNum;
    private final Splitor splitor;
    private final AsyncExecutor asyncExecutor;
    private volatile ArrayByteStream sequential;
    private volatile Stream<Byte> boxed;

    ParallelArrayByteStream(final byte[] values, final int fromIndex, final int toIndex, final boolean sorted, final int maxThreadNum, final Splitor splitor,
            final AsyncExecutor asyncExector, final Collection<Runnable> closeHandlers) {
        super(values, fromIndex, toIndex, sorted, closeHandlers);

        this.maxThreadNum = checkMaxThreadNum(maxThreadNum);
        this.splitor = splitor == null ? DEFAULT_SPLITOR : splitor;
        this.asyncExecutor = asyncExector == null ? DEFAULT_ASYNC_EXECUTOR : asyncExector;
    }

    @Override
    public ByteStream filter(final BytePredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.filter(predicate);
        }

        final Stream<Byte> stream = boxed().filter(new Predicate<Byte>() {
            @Override
            public boolean test(Byte value) {
                return predicate.test(value.byteValue());
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream takeWhile(final BytePredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.takeWhile(predicate);
        }

        final Stream<Byte> stream = boxed().takeWhile(new Predicate<Byte>() {
            @Override
            public boolean test(Byte value) {
                return predicate.test(value.byteValue());
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream dropWhile(final BytePredicate predicate) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.dropWhile(predicate);
        }

        final Stream<Byte> stream = boxed().dropWhile(new Predicate<Byte>() {
            @Override
            public boolean test(Byte value) {
                return predicate.test(value.byteValue());
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream map(final ByteUnaryOperator mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.map(mapper);
        }

        final ByteStream stream = boxed().mapToByte(new ToByteFunction<Byte>() {
            @Override
            public byte applyAsByte(Byte value) {
                return mapper.applyAsByte(value.byteValue());
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public IntStream mapToInt(final ByteToIntFunction mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToInt(mapper);
        }

        final IntStream stream = boxed().mapToInt(new ToIntFunction<Byte>() {
            @Override
            public int applyAsInt(Byte value) {
                return mapper.applyAsInt(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <U> Stream<U> mapToObj(final ByteFunction<? extends U> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.mapToObj(mapper);
        }

        return boxed().map(new Function<Byte, U>() {
            @Override
            public U apply(Byte value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public ByteStream flatMap(final ByteFunction<? extends ByteStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorByteStream(sequential().flatMap(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final ByteStream stream = boxed().flatMapToByte(new Function<Byte, ByteStream>() {
            @Override
            public ByteStream apply(Byte value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public IntStream flatMapToInt(final ByteFunction<? extends IntStream> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorIntStream(sequential().flatMapToInt(mapper), false, maxThreadNum, splitor, asyncExecutor, null);
        }

        final IntStream stream = boxed().flatMapToInt(new Function<Byte, IntStream>() {
            @Override
            public IntStream apply(Byte value) {
                return mapper.apply(value);
            }
        });

        return new ParallelIteratorIntStream(stream, false, maxThreadNum, splitor, asyncExecutor, null);
    }

    @Override
    public <T> Stream<T> flatMapToObj(final ByteFunction<? extends Stream<T>> mapper) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return new ParallelIteratorStream<>(sequential().flatMapToObj(mapper), false, null, maxThreadNum, splitor, asyncExecutor, null);
        }

        return boxed().flatMap(new Function<Byte, Stream<T>>() {
            @Override
            public Stream<T> apply(Byte value) {
                return mapper.apply(value);
            }
        });
    }

    @Override
    public ByteStream peek(final ByteConsumer action) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.peek(action);
        }

        final ByteStream stream = boxed().peek(new Consumer<Byte>() {
            @Override
            public void accept(Byte t) {
                action.accept(t);
            }
        }).sequential().mapToByte(ToByteFunction.UNBOX);

        return new ParallelIteratorByteStream(stream, false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public <E extends Exception> void forEach(final Try.ByteConsumer<E> action) throws E {
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
                        byte next = 0;

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
    public <K, V, M extends Map<K, V>> M toMap(final ByteFunction<? extends K> keyMapper, final ByteFunction<? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
        }

        final Function<? super Byte, ? extends K> keyMapper2 = new Function<Byte, K>() {
            @Override
            public K apply(Byte value) {
                return keyMapper.apply(value);
            }
        };

        final Function<? super Byte, ? extends V> valueMapper2 = new Function<Byte, V>() {
            @Override
            public V apply(Byte value) {
                return valueMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, valueMapper2, mergeFunction, mapFactory);
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final ByteFunction<? extends K> keyMapper, final Collector<Byte, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.toMap(keyMapper, downstream, mapFactory);
        }

        final Function<? super Byte, ? extends K> keyMapper2 = new Function<Byte, K>() {
            @Override
            public K apply(Byte value) {
                return keyMapper.apply(value);
            }
        };

        return boxed().toMap(keyMapper2, downstream, mapFactory);
    }

    @Override
    public byte reduce(final byte identity, final ByteBinaryOperator op) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(identity, op);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                    @Override
                    public Byte call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        byte result = identity;

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = op.applyAsByte(result, elements[cursor++]);
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
                futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                    @Override
                    public Byte call() {
                        byte result = identity;
                        byte next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = op.applyAsByte(result, next);
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

        Byte result = null;

        try {
            for (ContinuableFuture<Byte> future : futureList) {
                if (result == null) {
                    result = future.get();
                } else {
                    result = op.applyAsByte(result, future.get());
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
    public OptionalByte reduce(final ByteBinaryOperator accumulator) {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.reduce(accumulator);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                    @Override
                    public Byte call() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                        if (cursor >= to) {
                            return null;
                        }

                        byte result = elements[cursor++];

                        try {
                            while (cursor < to && eHolder.value() == null) {
                                result = accumulator.applyAsByte(result, elements[cursor++]);
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
                futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                    @Override
                    public Byte call() {
                        byte result = 0;

                        synchronized (elements) {
                            if (cursor.intValue() < toIndex) {
                                result = elements[cursor.getAndIncrement()];
                            } else {
                                return null;
                            }
                        }

                        byte next = 0;

                        try {
                            while (eHolder.value() == null) {
                                synchronized (elements) {
                                    if (cursor.intValue() < toIndex) {
                                        next = elements[cursor.getAndIncrement()];
                                    } else {
                                        break;
                                    }
                                }

                                result = accumulator.applyAsByte(result, next);
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

        Byte result = null;

        try {
            for (ContinuableFuture<Byte> future : futureList) {
                final Byte tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (result == null) {
                    result = tmp;
                } else {
                    result = accumulator.applyAsByte(result, tmp);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return result == null ? OptionalByte.empty() : OptionalByte.of(result);
    }

    @Override
    public <R> R collect(final Supplier<R> supplier, final ObjByteConsumer<? super R> accumulator, final BiConsumer<R, R> combiner) {
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
                        byte next = 0;

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
    public OptionalByte min() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalByte.empty();
            } else if (sorted) {
                return OptionalByte.of(elements[fromIndex]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalByte.of(N.min(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                @Override
                public Byte call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;

                    return cursor >= to ? null : N.min(elements, cursor, to);
                }
            }));
        }

        Byte candidate = null;

        try {
            for (ContinuableFuture<Byte> future : futureList) {
                final Byte tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.byteValue() < candidate.byteValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalByte.empty() : OptionalByte.of(candidate);
    }

    @Override
    public OptionalByte max() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return OptionalByte.empty();
            } else if (sorted) {
                return OptionalByte.of(elements[toIndex - 1]);
            } else if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
                return OptionalByte.of(N.max(elements, fromIndex, toIndex));
            } else {
                isDone = false;
            }
        } finally {
            if (isDone) {
                close();
            }
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Byte>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<Byte>() {
                @Override
                public Byte call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    return cursor >= to ? null : N.max(elements, cursor, to);
                }
            }));
        }

        Byte candidate = null;

        try {
            for (ContinuableFuture<Byte> future : futureList) {
                final Byte tmp = future.get();

                if (tmp == null) {
                    continue;
                } else if (candidate == null || tmp.byteValue() > candidate.byteValue()) {
                    candidate = tmp;
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            throw N.toRuntimeException(e);
        } finally {
            close();
        }

        return candidate == null ? OptionalByte.empty() : OptionalByte.of(candidate);
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
    public ByteSummaryStatistics summarize() {
        boolean isDone = true;

        try {
            if (fromIndex == toIndex) {
                return new ByteSummaryStatistics();
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
        final List<ContinuableFuture<ByteSummaryStatistics>> futureList = new ArrayList<>(threadNum);
        final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

        for (int i = 0; i < threadNum; i++) {
            final int sliceIndex = i;

            futureList.add(asyncExecutor.execute(new Callable<ByteSummaryStatistics>() {
                @Override
                public ByteSummaryStatistics call() {
                    int cursor = fromIndex + sliceIndex * sliceSize;
                    final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                    final ByteSummaryStatistics result = new ByteSummaryStatistics();

                    for (int i = cursor; i < to; i++) {
                        result.accept(elements[i]);
                    }

                    return result;
                }
            }));
        }

        ByteSummaryStatistics result = null;

        try {
            for (ContinuableFuture<ByteSummaryStatistics> future : futureList) {
                final ByteSummaryStatistics tmp = future.get();

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
    public <E extends Exception> boolean anyMatch(final Try.BytePredicate<E> predicate) throws E {
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
                        byte next = 0;

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
    public <E extends Exception> boolean allMatch(final Try.BytePredicate<E> predicate) throws E {
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
                        byte next = 0;

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
    public <E extends Exception> boolean noneMatch(final Try.BytePredicate<E> predicate) throws E {
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
                        byte next = 0;

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
    public <E extends Exception> OptionalByte findFirst(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findFirst(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Byte>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        int cursor = fromIndex + sliceIndex * sliceSize;
                        final int to = toIndex - cursor > sliceSize ? cursor + sliceSize : toIndex;
                        final Pair<Integer, Byte> pair = new Pair<>();

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
                        final Pair<Integer, Byte> pair = new Pair<>();

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

        return resultHolder.value() == null ? OptionalByte.empty() : OptionalByte.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalByte findLast(final Try.BytePredicate<E> predicate) throws E {
        assertNotClosed();

        if (maxThreadNum <= 1 || toIndex - fromIndex <= 1) {
            return super.findLast(predicate);
        }

        final int threadNum = N.min(maxThreadNum, (toIndex - fromIndex));
        final List<ContinuableFuture<Void>> futureList = new ArrayList<>(threadNum);
        final Holder<Throwable> eHolder = new Holder<>();
        final Holder<Pair<Integer, Byte>> resultHolder = new Holder<>();

        if (splitor == Splitor.ARRAY) {
            final int sliceSize = (toIndex - fromIndex) / threadNum + ((toIndex - fromIndex) % threadNum == 0 ? 0 : 1);

            for (int i = 0; i < threadNum; i++) {
                final int sliceIndex = i;

                futureList.add(asyncExecutor.execute(new Try.Runnable<RuntimeException>() {
                    @Override
                    public void run() {
                        final int from = fromIndex + sliceIndex * sliceSize;
                        int cursor = toIndex - from > sliceSize ? from + sliceSize : toIndex;
                        final Pair<Integer, Byte> pair = new Pair<>();

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
                        final Pair<Integer, Byte> pair = new Pair<>();

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

        return resultHolder.value() == null ? OptionalByte.empty() : OptionalByte.of(resultHolder.value().right);
    }

    @Override
    public <E extends Exception> OptionalByte findAny(final Try.BytePredicate<E> predicate) throws E {
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
                        byte next = 0;

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
                        byte next = 0;

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

        return resultHolder.value() == NONE ? OptionalByte.empty() : OptionalByte.of((Byte) resultHolder.value());
    }

    @Override
    public Stream<Byte> boxed() {
        Stream<Byte> tmp = boxed;

        if (tmp == null) {
            tmp = new ParallelIteratorStream<Byte>(iteratorEx(), sorted, sorted ? BYTE_COMPARATOR : null, maxThreadNum, splitor, asyncExecutor, closeHandlers);
            boxed = tmp;
        }

        return tmp;
    }

    @Override
    public ByteStream append(ByteStream stream) {
        return new ParallelIteratorByteStream(ByteStream.concat(this, stream), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream prepend(ByteStream stream) {
        return new ParallelIteratorByteStream(ByteStream.concat(stream, this), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream merge(final ByteStream b, final ByteBiFunction<Nth> nextSelector) {
        return new ParallelIteratorByteStream(ByteStream.merge(this, b, nextSelector), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteBinaryOperator zipFunction) {
        return new ParallelIteratorByteStream(ByteStream.zip(this, b, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteStream c, ByteTernaryOperator zipFunction) {
        return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, zipFunction), false, maxThreadNum, splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public ByteStream zipWith(ByteStream b, byte valueForNoneA, byte valueForNoneB, ByteBinaryOperator zipFunction) {
        return new ParallelIteratorByteStream(ByteStream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction), false, maxThreadNum, splitor, asyncExecutor,
                closeHandlers);
    }

    @Override
    public ByteStream zipWith(ByteStream b, ByteStream c, byte valueForNoneA, byte valueForNoneB, byte valueForNoneC, ByteTernaryOperator zipFunction) {
        return new ParallelIteratorByteStream(ByteStream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction), false, maxThreadNum,
                splitor, asyncExecutor, closeHandlers);
    }

    @Override
    public boolean isParallel() {
        return true;
    }

    @Override
    public ByteStream sequential() {
        ArrayByteStream tmp = sequential;

        if (tmp == null) {
            tmp = new ArrayByteStream(elements, fromIndex, toIndex, sorted, closeHandlers);
            sequential = tmp;
        }

        return tmp;
    }

    @Override
    public ByteStream parallel(int maxThreadNum, Splitor splitor) {
        checkMaxThreadNum(maxThreadNum);
        checkSplitor(splitor);

        return new ParallelArrayByteStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, closeHandlers);
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
    public ByteStream onClose(Runnable closeHandler) {
        final Deque<Runnable> newCloseHandlers = new LocalArrayDeque<>(N.isNullOrEmpty(this.closeHandlers) ? 1 : this.closeHandlers.size() + 1);

        newCloseHandlers.add(wrapCloseHandlers(closeHandler));

        if (N.notNullOrEmpty(this.closeHandlers)) {
            newCloseHandlers.addAll(this.closeHandlers);
        }

        return new ParallelArrayByteStream(elements, fromIndex, toIndex, sorted, maxThreadNum, splitor, asyncExecutor, newCloseHandlers);
    }
}
