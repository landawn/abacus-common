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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import com.landawn.abacus.DataSet;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedWriter;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.Duration;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Fn.BiFunctions;
import com.landawn.abacus.util.Fn.Factory;
import com.landawn.abacus.util.Fn.Suppliers;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.Iterables;
import com.landawn.abacus.util.Iterators;
import com.landawn.abacus.util.JdbcUtil;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.PermutationIterator;
import com.landawn.abacus.util.StringUtil.Strings;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.TriFunction;

/**
 *
 * @param <T> 
 */
abstract class AbstractStream<T> extends Stream<T> {

    AbstractStream(final boolean sorted, final Comparator<? super T> cmp, final Collection<Runnable> closeHandlers) {
        super(sorted, cmp, closeHandlers);
    }

    @Override
    public Stream<T> skip(final long n, final Consumer<? super T> action) {
        final Predicate<T> filter = isParallel() ? new Predicate<T>() {
            final AtomicLong cnt = new AtomicLong(n);

            @Override
            public boolean test(T value) {
                return cnt.getAndDecrement() > 0;
            }
        } : new Predicate<T>() {
            final MutableLong cnt = MutableLong.of(n);

            @Override
            public boolean test(T value) {
                return cnt.getAndDecrement() > 0;
            }
        };

        return dropWhile(filter, action);
    }

    @Override
    public Stream<T> peekFirst(final Consumer<? super T> action) {
        final Function<? super T, ? extends T> mapperForFirst = new Function<T, T>() {
            @Override
            public T apply(T t) {
                action.accept(t);
                return t;
            }
        };

        return mapFirst(mapperForFirst);
    }

    @Override
    public Stream<T> peekLast(final Consumer<? super T> action) {
        final Function<? super T, ? extends T> mapperForFirst = new Function<T, T>() {
            @Override
            public T apply(T t) {
                action.accept(t);
                return t;
            }
        };

        return mapLast(mapperForFirst);
    }

    @Override
    public Stream<T> removeIf(final Predicate<? super T> predicate) {
        checkArgNotNull(predicate);

        return filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return predicate.test(value) == false;
            }
        });
    }

    @Override
    public Stream<T> removeIf(final Predicate<? super T> predicate, final Consumer<? super T> action) {
        checkArgNotNull(predicate);
        checkArgNotNull(action);

        return filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return false;
                }

                return true;
            }
        });
    }

    @Override
    public Stream<T> dropWhile(final Predicate<? super T> predicate, final Consumer<? super T> action) {
        checkArgNotNull(predicate);
        checkArgNotNull(action);

        return dropWhile(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                if (predicate.test(value)) {
                    action.accept(value);
                    return true;
                }

                return false;
            }
        });
    }

    @Override
    public Stream<T> step(final long step) {
        checkArgPositive(step, "step");

        final long skip = step - 1;
        final ObjIteratorEx<T> iter = iteratorEx();

        final Iterator<T> iterator = new ObjIteratorEx<T>() {
            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                final T next = iter.next();
                iter.skip(skip);
                return next;
            }
        };

        return newStream(iterator, sorted, cmp);
    }

    //    @Override
    //    public <R> Stream<R> biMap(BiFunction<? super T, ? super T, ? extends R> mapper) {
    //        return biMap(mapper, false);
    //    }
    //
    //    @Override
    //    public <R> Stream<R> triMap(TriFunction<? super T, ? super T, ? super T, ? extends R> mapper) {
    //        return triMap(mapper, false);
    //    }

    @Override
    public <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper) {
        return slidingMap(mapper, 1);
    }

    @Override
    public <R> Stream<R> slidingMap(BiFunction<? super T, ? super T, R> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

    @Override
    public <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper) {
        return slidingMap(mapper, 1);
    }

    @Override
    public <R> Stream<R> slidingMap(TriFunction<? super T, ? super T, ? super T, R> mapper, int increment) {
        return slidingMap(mapper, increment, false);
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends Map.Entry<? extends K, ? extends V>> mapper) {
        final Function<T, T> mapper2 = Fn.identity();

        if (mapper == mapper2) {
            return EntryStream.of((Stream<Map.Entry<K, V>>) this);
        }

        return EntryStream.of(map(mapper));
    }

    @Override
    public <K, V> EntryStream<K, V> mapToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper) {
        final Function<T, Map.Entry<K, V>> mapper = new Function<T, Map.Entry<K, V>>() {
            @Override
            public Entry<K, V> apply(T t) {
                return new SimpleImmutableEntry<K, V>(keyMapper.apply(t), valueMapper.apply(t));
            }
        };

        return mapToEntry(mapper);
    }

    //    private static final Predicate<Optional<?>> IS_PRESENT = new Predicate<Optional<?>>() {
    //        @Override
    //        public boolean test(Optional<?> t) {
    //            return t.isPresent();
    //        }
    //    };
    //
    //    private static final Function<Optional<Object>, Object> OPTIONAL_GET = new Function<Optional<Object>, Object>() {
    //        @Override
    //        public Object apply(Optional<Object> t) {
    //            return t.get();
    //        }
    //    };
    //
    //    @SuppressWarnings("rawtypes")
    //    @Override
    //    public <U> Stream<U> mapp(Function<? super T, ? extends Optional<? extends U>> mapper) {
    //        final Function<Optional<? extends U>, U> optionalGetter = (Function) OPTIONAL_GET;
    //        return map(mapper).filter(IS_PRESENT).map(optionalGetter);
    //    }

    @Override
    public <R> Stream<R> flattMap(final Function<? super T, ? extends Collection<? extends R>> mapper) {
        return flatMap(new Function<T, Stream<? extends R>>() {
            @Override
            public Stream<? extends R> apply(T t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <R> Stream<R> flatMapp(final Function<? super T, R[]> mapper) {
        return flatMap(new Function<T, Stream<? extends R>>() {
            @Override
            public Stream<? extends R> apply(T t) {
                return Stream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public CharStream flattMapToChar(final Function<? super T, char[]> mapper) {
        return flatMapToChar(new Function<T, CharStream>() {
            @Override
            public CharStream apply(T t) {
                return CharStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public ByteStream flattMapToByte(final Function<? super T, byte[]> mapper) {
        return flatMapToByte(new Function<T, ByteStream>() {
            @Override
            public ByteStream apply(T t) {
                return ByteStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public ShortStream flattMapToShort(final Function<? super T, short[]> mapper) {
        return flatMapToShort(new Function<T, ShortStream>() {
            @Override
            public ShortStream apply(T t) {
                return ShortStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public IntStream flattMapToInt(final Function<? super T, int[]> mapper) {
        return flatMapToInt(new Function<T, IntStream>() {
            @Override
            public IntStream apply(T t) {
                return IntStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public LongStream flattMapToLong(final Function<? super T, long[]> mapper) {
        return flatMapToLong(new Function<T, LongStream>() {
            @Override
            public LongStream apply(T t) {
                return LongStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public FloatStream flattMapToFloat(final Function<? super T, float[]> mapper) {
        return flatMapToFloat(new Function<T, FloatStream>() {
            @Override
            public FloatStream apply(T t) {
                return FloatStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public DoubleStream flattMapToDouble(final Function<? super T, double[]> mapper) {
        return flatMapToDouble(new Function<T, DoubleStream>() {
            @Override
            public DoubleStream apply(T t) {
                return DoubleStream.of(mapper.apply(t));
            }
        });
    }

    @Override
    public <K, V> EntryStream<K, V> flatMapToEntry(final Function<? super T, ? extends Stream<? extends Map.Entry<? extends K, ? extends V>>> mapper) {
        return EntryStream.of(flatMap(mapper));
    }

    @Override
    @ParallelSupported
    public <K, V> EntryStream<K, V> flattMapToEntry(final Function<? super T, ? extends Map<? extends K, ? extends V>> mapper) {
        final Function<? super T, ? extends Stream<Entry<K, V>>> mapper2 = new Function<T, Stream<Entry<K, V>>>() {
            @Override
            public Stream<Entry<K, V>> apply(T t) {
                return Stream.of((Map<K, V>) mapper.apply(t));
            }
        };

        return flatMapToEntry(mapper2);
    }

    @Override
    @ParallelSupported
    public <K, V> EntryStream<K, V> flatMappToEntry(final Function<? super T, ? extends EntryStream<? extends K, ? extends V>> mapper) {
        final Function<? super T, ? extends Stream<? extends Entry<? extends K, ? extends V>>> mapper2 = new Function<T, Stream<? extends Entry<? extends K, ? extends V>>>() {
            @Override
            public Stream<? extends Entry<? extends K, ? extends V>> apply(T t) {
                return mapper.apply(t).entries();
            }
        };

        return flatMapToEntry(mapper2);
    }

    //    @Override
    //    public <V> EntryStream<T, V> flattMapToEntry(final Function<? super T, ? extends Collection<? extends V>> flatValueMapper) {
    //        final Function<T, Stream<Map.Entry<T, V>>> flatEntryMapper = new Function<T, Stream<Map.Entry<T, V>>>() {
    //            @Override
    //            public Stream<Map.Entry<T, V>> apply(final T t) {
    //                final Function<V, Map.Entry<T, V>> entryMapper = new Function<V, Map.Entry<T, V>>() {
    //                    @Override
    //                    public Entry<T, V> apply(V v) {
    //                        return new SimpleImmutableEntry<>(t, v);
    //                    }
    //                };
    //
    //                return Stream.of(flatValueMapper.apply(t)).map(entryMapper);
    //            }
    //        };
    //
    //        return flatMapToEntry(flatEntryMapper);
    //    }

    @Override
    public <U> Stream<U> rangeMap(final BiPredicate<? super T, ? super T> sameRange, final BiFunction<? super T, ? super T, ? extends U> mapper) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<U>() {
            private T left = null, right = null, next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() {
                left = hasNext ? next : iter.next();
                right = left;

                while (hasNext = iter.hasNext()) {
                    next = iter.next();

                    if (sameRange.test(left, next)) {
                        right = next;
                    } else {
                        break;
                    }
                }

                return mapper.apply(left, right);
            }
        }, false, null);
    }

    @Override
    public Stream<Stream<T>> collapse(final BiPredicate<? super T, ? super T> collapsible) {
        return collapse(collapsible, Suppliers.<T> ofList()).map(listToStreamMapper());
    }

    @Override
    public <C extends Collection<T>> Stream<C> collapse(final BiPredicate<? super T, ? super T> collapsible, final Supplier<? extends C> supplier) {
        return newStream(new ObjIteratorEx<C>() {
            private final ObjIteratorEx<T> iter = iteratorEx();
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public C next() {
                if (hasNext == false) {
                    next = iter.next();
                }

                final C c = supplier.get();
                c.add(next);

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        c.add(next);
                    } else {
                        break;
                    }
                }

                return c;
            }
        }, false, null);
    }

    @Override
    public Stream<T> collapse(final BiPredicate<? super T, ? super T> collapsible, final BiFunction<? super T, ? super T, T> mergeFunction) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public T next() {
                T res = hasNext ? next : (next = iter.next());

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = mergeFunction.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> collapse(final BiPredicate<? super T, ? super T> collapsible, final U init, final BiFunction<U, ? super T, U> op) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<U>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public U next() {
                U res = op.apply(init, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        res = op.apply(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Supplier<R> supplier,
            final BiConsumer<? super R, ? super T> accumulator) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<R>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() {
                final R res = supplier.get();
                accumulator.accept(res, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        accumulator.accept(res, next);
                    } else {
                        break;
                    }
                }

                return res;
            }
        }, false, null);
    }

    @Override
    public <R, A> Stream<R> collapse(final BiPredicate<? super T, ? super T> collapsible, final Collector<? super T, A, R> collector) {
        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<R>() {
            private boolean hasNext = false;
            private T next = null;

            @Override
            public boolean hasNext() {
                return hasNext || iter.hasNext();
            }

            @Override
            public R next() {
                final A c = supplier.get();
                accumulator.accept(c, hasNext ? next : (next = iter.next()));

                while ((hasNext = iter.hasNext())) {
                    if (collapsible.test(next, (next = iter.next()))) {
                        accumulator.accept(c, next);
                    } else {
                        break;
                    }
                }

                return finisher.apply(c);
            }
        }, false, null);
    }

    @Override
    public Stream<T> scan(final BiFunction<? super T, ? super T, T> accumulator) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<T>() {
            private T res = null;
            private boolean isFirst = true;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                if (isFirst) {
                    isFirst = false;
                    return (res = iter.next());
                } else {
                    return (res = accumulator.apply(res, iter.next()));
                }
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final BiFunction<U, ? super T, U> accumulator) {
        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<U>() {
            private U res = init;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public U next() {
                return (res = accumulator.apply(res, iter.next()));
            }
        }, false, null);
    }

    @Override
    public <U> Stream<U> scan(final U init, final BiFunction<U, ? super T, U> accumulator, boolean initIncluded) {
        if (initIncluded == false) {
            return scan(init, accumulator);
        }

        final ObjIteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<U>() {
            private boolean isFirst = true;
            private U res = init;

            @Override
            public boolean hasNext() {
                return isFirst || iter.hasNext();
            }

            @Override
            public U next() {
                if (isFirst) {
                    isFirst = false;
                    return init;
                }

                return (res = accumulator.apply(res, iter.next()));
            }
        }, false, null);
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Stream<T> sortedBy(final Function<? super T, ? extends Comparable> keyMapper) {
        final Comparator<? super T> comparator = new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                return N.compare(keyMapper.apply(o1), keyMapper.apply(o2));
            }
        };

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByInt(final ToIntFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingInt(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByLong(final ToLongFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingLong(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<T> sortedByDouble(final ToDoubleFunction<? super T> keyMapper) {
        final Comparator<? super T> comparator = Comparators.comparingDouble(keyMapper);

        return sorted(comparator);
    }

    @Override
    public Stream<Stream<T>> split(final int chunkSize) {
        return splitToList(chunkSize).map(listToStreamMapper());
    }

    @Override
    public Stream<List<T>> splitToList(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofList());
    }

    @Override
    public Stream<Set<T>> splitToSet(final int chunkSize) {
        return split(chunkSize, Factory.<T> ofSet());
    }

    @Override
    public Stream<Stream<T>> split(final Predicate<? super T> predicate) {
        return splitToList(predicate).map(listToStreamMapper());
    }

    @Override
    public Stream<List<T>> splitToList(final Predicate<? super T> predicate) {
        return split(predicate, Suppliers.<T> ofList());
    }

    @Override
    public Stream<Set<T>> splitToSet(final Predicate<? super T> predicate) {
        return split(predicate, Suppliers.<T> ofSet());
    }

    @Override
    public Stream<Stream<T>> splitBy(final Predicate<? super T> where) {
        checkArgNotNull(where);

        final IteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<Stream<T>>() {
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public Stream<T> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                Stream<T> result = null;

                if (cursor == 0) {
                    final List<T> list = new ArrayList<>();

                    while (iter.hasNext()) {
                        next = iter.next();

                        if (where.test(next)) {
                            list.add(next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }

                    result = new ArrayStream<T>(Stream.toArray(list), 0, list.size(), sorted, cmp, null);
                } else {
                    IteratorEx<T> iterEx = iter;

                    if (hasNext) {
                        iterEx = new ObjIteratorEx<T>() {
                            private boolean isFirst = true;

                            @Override
                            public boolean hasNext() {
                                return isFirst || iter.hasNext();
                            }

                            @Override
                            public T next() {
                                if (hasNext() == false) {
                                    throw new NoSuchElementException();
                                }

                                if (isFirst) {
                                    isFirst = false;
                                    return next;
                                } else {
                                    return iter.next();
                                }
                            }
                        };
                    }

                    result = new IteratorStream<T>(iterEx, sorted, cmp, null);
                }

                cursor++;

                return result;
            }

            @Override
            public long count() {
                iter.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        while (iter.hasNext()) {
                            next = iter.next();

                            if (where.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else {
                        iter.skip(Long.MAX_VALUE);
                    }
                } else {
                    iter.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }

        }, false, null);
    }

    @Override
    public <A, R> Stream<R> splitBy(final Predicate<? super T> where, Collector<? super T, A, R> collector) {
        checkArgNotNull(where, "where");
        checkArgNotNull(collector, "collector");

        final Supplier<A> supplier = collector.supplier();
        final BiConsumer<? super A, ? super T> accumulator = collector.accumulator();
        final Function<A, R> finisher = collector.finisher();

        final IteratorEx<T> iter = iteratorEx();

        return newStream(new ObjIteratorEx<R>() {
            private int cursor = 0;
            private T next = null;
            private boolean hasNext = false;

            @Override
            public boolean hasNext() {
                return cursor < 2;
            }

            @Override
            public R next() {
                if (hasNext()) {
                    throw new NoSuchElementException();
                }

                final A container = supplier.get();

                if (cursor == 0) {
                    while (iter.hasNext()) {
                        next = iter.next();

                        if (where.test(next)) {
                            accumulator.accept(container, next);
                        } else {
                            hasNext = true;
                            break;
                        }
                    }
                } else {
                    if (hasNext) {
                        accumulator.accept(container, next);
                    }

                    while (iter.hasNext()) {
                        accumulator.accept(container, iter.next());
                    }
                }

                cursor++;

                return finisher.apply(container);
            }

            @Override
            public long count() {
                iter.count();

                return 2 - cursor;
            }

            @Override
            public void skip(long n) {
                if (n == 0) {
                    return;
                } else if (n == 1) {
                    if (cursor == 0) {
                        while (iter.hasNext()) {
                            next = iter.next();

                            if (where.test(next) == false) {
                                hasNext = true;
                                break;
                            }
                        }
                    } else {
                        iter.skip(Long.MAX_VALUE);
                    }
                } else {
                    iter.skip(Long.MAX_VALUE);
                }

                cursor = n >= 2 ? 2 : cursor + (int) n;
            }
        }, false, null);
    }

    @Override
    public Stream<Stream<T>> sliding(final int windowSize, final int increment) {
        return slidingToList(windowSize, increment).map(listToStreamMapper());
    }

    @Override
    public Stream<List<T>> slidingToList(final int windowSize, final int increment) {
        return sliding(windowSize, increment, Factory.<T> ofList());
    }

    @Override
    public <C extends Collection<T>> Stream<C> sliding(final int windowSize, IntFunction<? extends C> collectionSupplier) {
        return sliding(windowSize, 1, collectionSupplier);
    }

    @Override
    public <A, R> Stream<R> sliding(int windowSize, Collector<? super T, A, R> collector) {
        return sliding(windowSize, 1, collector);
    }

    private static final LongSupplier sysTimeGetter = new LongSupplier() {
        @Override
        public long getAsLong() {
            return System.currentTimeMillis();
        }
    };

    @Override
    public Stream<Stream<T>> window(final Duration duration) {
        return windowToList(duration).map(listToStreamMapper());
    }

    @Override
    public Stream<Stream<T>> window(final Duration duration, final LongSupplier startTime) {
        return window(duration, startTime, Suppliers.<T> ofList()).map(listToStreamMapper());
    }

    @Override
    public Stream<List<T>> windowToList(final Duration duration) {
        return window(duration, Suppliers.<T> ofList());
    }

    @Override
    public Stream<Set<T>> windowToSet(final Duration duration) {
        return window(duration, Suppliers.<T> ofSet());
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final Duration duration, final Supplier<? extends C> collectionSupplier) {
        return window(duration, sysTimeGetter, collectionSupplier);
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final Duration duration, final LongSupplier startTime, final Supplier<? extends C> collectionSupplier) {
        checkArgNotNull(duration, "duration");
        checkArgPositive(duration.toMillis(), "duration");

        return window(duration, duration.toMillis(), startTime, collectionSupplier);
    }

    @Override
    public <A, R> Stream<R> window(Duration duration, Collector<? super T, A, R> collector) {
        return window(duration, sysTimeGetter, collector);
    }

    @Override
    public <A, R> Stream<R> window(Duration duration, final LongSupplier startTime, Collector<? super T, A, R> collector) {
        checkArgNotNull(duration, "duration");
        checkArgPositive(duration.toMillis(), "duration");

        return window(duration, duration.toMillis(), startTime, collector);
    }

    @Override
    public Stream<Stream<T>> window(final Duration duration, final long incrementInMillis) {
        return windowToList(duration, incrementInMillis).map(listToStreamMapper());
    }

    @Override
    public Stream<Stream<T>> window(final Duration duration, final long incrementInMillis, final LongSupplier startTime) {
        return window(duration, incrementInMillis, startTime, Suppliers.<T> ofList()).map(listToStreamMapper());
    }

    @Override
    public Stream<List<T>> windowToList(final Duration duration, final long incrementInMillis) {
        return window(duration, incrementInMillis, Suppliers.<T> ofList());
    }

    @Override
    public Stream<Set<T>> windowToSet(final Duration duration, final long incrementInMillis) {
        return window(duration, incrementInMillis, Suppliers.<T> ofSet());
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final Duration duration, final long incrementInMillis, final Supplier<? extends C> collectionSupplier) {
        return window(duration, incrementInMillis, sysTimeGetter, collectionSupplier);
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final Duration duration, final long incrementInMillis, final LongSupplier startTime,
            final Supplier<? extends C> collectionSupplier) {
        checkArgNotNull(duration, "duration");
        checkArgPositive(duration.toMillis(), "duration");
        checkArgPositive(incrementInMillis, "incrementInMillis");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private final long durationInMillis = duration.toMillis();
            private final boolean useQueue = incrementInMillis < durationInMillis;

            private final Deque<Timed<T>> queue = useQueue ? new ArrayDeque<Timed<T>>() : null;
            private Iterator<Timed<T>> queueIter;

            private ObjIteratorEx<T> iter;
            private Timed<T> timedNext = null;
            private T next = null;

            private long fromTime;
            private long endTime;
            private long now;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                if (useQueue) {
                    if ((queue.size() > 0 && queue.getLast().timestamp() >= endTime)
                            || (timedNext != null && timedNext.timestamp() - fromTime >= incrementInMillis)) {
                        return true;
                    } else {
                        while (iter.hasNext()) {
                            next = iter.next();
                            now = System.currentTimeMillis();

                            if (now - fromTime >= incrementInMillis) {
                                timedNext = Timed.of(next, now);
                                queue.add(timedNext);
                                return true;
                            } else if (timedNext != null) {
                                timedNext = null;
                            }
                        }

                        return false;
                    }
                } else {
                    while ((timedNext == null || timedNext.timestamp() - fromTime < incrementInMillis) && iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now - fromTime >= incrementInMillis) {
                            timedNext = Timed.of(next, now);
                            break;
                        } else if (timedNext != null) {
                            timedNext = null;
                        }
                    }

                    return timedNext != null && timedNext.timestamp() - fromTime >= incrementInMillis;
                }
            }

            @Override
            public C next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                fromTime += incrementInMillis;
                endTime = fromTime + durationInMillis;

                final C result = collectionSupplier.get();

                if (useQueue) {
                    queueIter = queue.iterator();

                    while (queueIter.hasNext()) {
                        timedNext = queueIter.next();

                        if (timedNext.timestamp() < fromTime) {
                            queueIter.remove();
                        } else if (timedNext.timestamp() < endTime) {
                            result.add(timedNext.value());
                            timedNext = null;
                        } else {
                            return result;
                        }
                    }

                    while (iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now >= fromTime) {
                            timedNext = Timed.of(next, now);
                            queue.add(timedNext);

                            if (timedNext.timestamp() < endTime) {
                                result.add(timedNext.value());
                                timedNext = null;
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    if (timedNext != null) {
                        if (timedNext.timestamp() < fromTime) {
                            // ignore
                        } else if (timedNext.timestamp() < endTime) {
                            result.add(timedNext.value());
                        } else {
                            return result;
                        }
                    }

                    while (iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now < fromTime) {
                            // ignore
                        } else if (now < endTime) {
                            result.add(next);
                        } else {
                            timedNext = Timed.of(next, now);
                            break;
                        }
                    }
                }

                return result;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    iter = iteratorEx();

                    fromTime = startTime.getAsLong() - incrementInMillis;
                    endTime = fromTime + durationInMillis;
                }
            }
        }, false, null);
    }

    @Override
    public <A, R> Stream<R> window(final Duration duration, final long incrementInMillis, final Collector<? super T, A, R> collector) {
        return window(duration, incrementInMillis, sysTimeGetter, collector);
    }

    @Override
    public <A, R> Stream<R> window(final Duration duration, final long incrementInMillis, final LongSupplier startTime,
            final Collector<? super T, A, R> collector) {
        checkArgNotNull(duration, "duration");
        checkArgPositive(duration.toMillis(), "duration");
        checkArgPositive(incrementInMillis, "incrementInMillis");
        checkArgNotNull(collector, "collector");

        return newStream(new ObjIteratorEx<R>() {
            private final long durationInMillis = duration.toMillis();
            private final boolean useQueue = incrementInMillis < durationInMillis;

            private final Deque<Timed<T>> queue = useQueue ? new ArrayDeque<Timed<T>>() : null;
            private Iterator<Timed<T>> queueIter;

            private Supplier<A> supplier;
            private BiConsumer<A, ? super T> accumulator;
            private Function<A, R> finisher;

            private ObjIteratorEx<T> iter;
            private Timed<T> timedNext = null;
            private T next = null;

            private long fromTime;
            private long endTime;
            private long now;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                if (useQueue) {
                    if ((queue.size() > 0 && queue.getLast().timestamp() >= endTime)
                            || (timedNext != null && timedNext.timestamp() - fromTime >= incrementInMillis)) {
                        return true;
                    } else {
                        while (iter.hasNext()) {
                            next = iter.next();
                            now = System.currentTimeMillis();

                            if (now - fromTime >= incrementInMillis) {
                                timedNext = Timed.of(next, now);
                                queue.add(timedNext);
                                return true;
                            } else if (timedNext != null) {
                                timedNext = null;
                            }
                        }

                        return false;
                    }
                } else {
                    while ((timedNext == null || timedNext.timestamp() - fromTime < incrementInMillis) && iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now - fromTime >= incrementInMillis) {
                            timedNext = Timed.of(next, now);
                            break;
                        } else if (timedNext != null) {
                            timedNext = null;
                        }
                    }

                    return timedNext != null && timedNext.timestamp() - fromTime >= incrementInMillis;
                }
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                fromTime += incrementInMillis;
                endTime = fromTime + durationInMillis;

                final A container = supplier.get();

                if (useQueue) {
                    queueIter = queue.iterator();

                    while (queueIter.hasNext()) {
                        timedNext = queueIter.next();

                        if (timedNext.timestamp() < fromTime) {
                            queueIter.remove();
                        } else if (timedNext.timestamp() < endTime) {
                            accumulator.accept(container, timedNext.value());
                            timedNext = null;
                        } else {
                            return finisher.apply(container);
                        }
                    }

                    while (iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now >= fromTime) {
                            timedNext = Timed.of(next, now);
                            queue.add(timedNext);

                            if (timedNext.timestamp() < endTime) {
                                accumulator.accept(container, timedNext.value());
                                timedNext = null;
                            } else {
                                break;
                            }
                        }
                    }
                } else {
                    if (timedNext != null) {
                        if (timedNext.timestamp() < fromTime) {
                            // ignore
                        } else if (timedNext.timestamp() < endTime) {
                            accumulator.accept(container, timedNext.value());
                        } else {
                            return finisher.apply(container);
                        }
                    }

                    while (iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now < fromTime) {
                            // ignore
                        } else if (now < endTime) {
                            accumulator.accept(container, next);
                        } else {
                            timedNext = Timed.of(next, now);
                            break;
                        }
                    }
                }

                return finisher.apply(container);
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    supplier = collector.supplier();
                    accumulator = collector.accumulator();
                    finisher = collector.finisher();

                    iter = iteratorEx();

                    fromTime = startTime.getAsLong() - incrementInMillis;
                    endTime = fromTime + durationInMillis;
                }
            }
        }, false, null);
    }

    Function<List<T>, Stream<T>> listToStreamMapper() {
        return new Function<List<T>, Stream<T>>() {
            @Override
            public Stream<T> apply(List<T> t) {
                return new ArrayStream<>(Stream.toArray(t), 0, t.size(), sorted, cmp, null);
            }
        };
    }

    @Override
    public Stream<Stream<T>> window(final int maxWindowSize, final Duration maxDuration) {
        return window(maxWindowSize, maxDuration, Suppliers.<T> ofList()).map(listToStreamMapper());
    }

    @Override
    public Stream<Stream<T>> window(final int maxWindowSize, final Duration maxDuration, final LongSupplier startTime) {
        return window(maxWindowSize, maxDuration, startTime, Suppliers.<T> ofList()).map(listToStreamMapper());
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final int maxWindowSize, final Duration maxDuration, final Supplier<? extends C> collectionSupplier) {
        return window(maxWindowSize, maxDuration, sysTimeGetter, collectionSupplier);
    }

    @Override
    public <C extends Collection<T>> Stream<C> window(final int maxWindowSize, final Duration maxDuration, final LongSupplier startTime,
            final Supplier<? extends C> collectionSupplier) {
        checkArgPositive(maxWindowSize, "maxWindowSize");
        checkArgNotNull(maxDuration, "maxDuration");
        checkArgPositive(maxDuration.toMillis(), "maxDuration");
        checkArgNotNull(startTime, "startTime");
        checkArgNotNull(collectionSupplier, "collectionSupplier");

        return newStream(new ObjIteratorEx<C>() {
            private final long maxDurationInMillis = maxDuration.toMillis();
            private ObjIteratorEx<T> iter;
            private Timed<T> timedNext = null;
            private T next = null;

            private long fromTime;
            private long endTime;
            private long now;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                while (timedNext == null && iter.hasNext()) {
                    next = iter.next();
                    now = System.currentTimeMillis();

                    if (now >= endTime) {
                        timedNext = Timed.of(next, now);
                    }
                }

                return timedNext != null;
            }

            @Override
            public C next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                fromTime = endTime;
                endTime = fromTime + maxDurationInMillis;
                int cnt = 0;
                final C result = collectionSupplier.get();

                if (timedNext != null && timedNext.timestamp() < endTime) {
                    result.add(timedNext.value());
                    timedNext = null;
                    cnt++;
                }

                if (timedNext == null) {
                    while (cnt < maxWindowSize && iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now < endTime) {
                            result.add(next);
                            cnt++;
                        } else {
                            timedNext = Timed.of(next, now);
                            break;
                        }
                    }
                }

                endTime = N.min(endTime, timedNext == null ? System.currentTimeMillis() : timedNext.timestamp());

                return result;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    iter = iteratorEx();

                    fromTime = startTime.getAsLong() - maxDurationInMillis;
                    endTime = fromTime + maxDurationInMillis;
                }
            }
        }, false, null);
    }

    @Override
    public <A, R> Stream<R> window(final int maxWindowSize, final Duration maxDuration, Collector<? super T, A, R> collector) {
        return window(maxWindowSize, maxDuration, sysTimeGetter, collector);
    }

    @Override
    public <A, R> Stream<R> window(final int maxWindowSize, final Duration maxDuration, final LongSupplier startTime,
            final Collector<? super T, A, R> collector) {
        checkArgPositive(maxWindowSize, "maxWindowSize");
        checkArgNotNull(maxDuration, "maxDuration");
        checkArgPositive(maxDuration.toMillis(), "maxDuration");
        checkArgNotNull(startTime, "startTime");
        checkArgNotNull(collector, "collector");

        return newStream(new ObjIteratorEx<R>() {
            private final long maxDurationInMillis = maxDuration.toMillis();

            private Supplier<A> supplier;
            private BiConsumer<A, ? super T> accumulator;
            private Function<A, R> finisher;

            private ObjIteratorEx<T> iter;
            private Timed<T> timedNext = null;
            private T next = null;

            private long fromTime;
            private long endTime;
            private long now;

            private boolean initialized = false;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                while (timedNext == null && iter.hasNext()) {
                    next = iter.next();
                    now = System.currentTimeMillis();

                    if (now >= endTime) {
                        timedNext = Timed.of(next, now);
                    }
                }

                return timedNext != null;
            }

            @Override
            public R next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                fromTime = endTime;
                endTime = fromTime + maxDurationInMillis;
                int cnt = 0;
                final A container = supplier.get();

                if (timedNext != null && timedNext.timestamp() < endTime) {
                    accumulator.accept(container, timedNext.value());
                    timedNext = null;
                    cnt++;
                }

                if (timedNext == null) {
                    while (cnt < maxWindowSize && iter.hasNext()) {
                        next = iter.next();
                        now = System.currentTimeMillis();

                        if (now < endTime) {
                            accumulator.accept(container, next);
                            cnt++;
                        } else {
                            timedNext = Timed.of(next, now);
                            break;
                        }
                    }
                }

                endTime = N.min(endTime, timedNext == null ? System.currentTimeMillis() : timedNext.timestamp());

                return finisher.apply(container);
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    supplier = collector.supplier();
                    accumulator = collector.accumulator();
                    finisher = collector.finisher();

                    iter = iteratorEx();

                    fromTime = startTime.getAsLong() - maxDurationInMillis;
                    endTime = fromTime + maxDurationInMillis;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> intersperse(final T delimiter) {
        return newStream(new ObjIteratorEx<T>() {
            private final Iterator<T> iter = iteratorEx();
            private boolean toInsert = false;

            @Override
            public boolean hasNext() {
                return iter.hasNext();
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                if (toInsert) {
                    toInsert = false;
                    return delimiter;
                } else {
                    final T res = iter.next();
                    toInsert = true;
                    return res;
                }
            }
        }, false, null);
    }

    @Override
    public <E extends Exception> void forEachPair(final Try.BiConsumer<? super T, ? super T, E> action) throws E {
        forEachPair(action, 1);
    }

    @Override
    public <E extends Exception> void forEachTriple(final Try.TriConsumer<? super T, ? super T, ? super T, E> action) throws E {
        forEachTriple(action, 1);
    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper) {
        return groupBy(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    @Override
    public <K> Stream<Entry<K, List<T>>> groupBy(final Function<? super T, ? extends K> keyMapper, Supplier<? extends Map<K, List<T>>> mapFactory) {
        return groupBy(keyMapper, Fn.<T> identity(), mapFactory);
    }

    @Override
    public <K, V> Stream<Entry<K, List<V>>> groupBy(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return groupBy(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    @Override
    public <K, V> Stream<Map.Entry<K, List<V>>> groupBy(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends Map<K, List<V>>> mapFactory) {
        return groupBy(keyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    }

    @Override
    public <K, A, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, Collector<? super T, A, D> downstream) {
        return groupBy(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, A, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream,
            final Supplier<? extends Map<K, D>> mapFactory) {
        return groupBy(keyMapper, Fn.<T> identity(), downstream, mapFactory);
    }

    @Override
    public <K, V, A, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            Collector<? super V, A, D> downstream) {
        return groupBy(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, V, A, D> Stream<Entry<K, D>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final Collector<? super V, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) {
        return newStream(new ObjIteratorEx<Entry<K, D>>() {
            private boolean initialized = false;
            private Iterator<Entry<K, D>> iter = null;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return iter.hasNext();
            }

            @Override
            public Entry<K, D> next() {
                if (initialized == false) {
                    init();
                }

                return iter.next();
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;

                    iter = AbstractStream.this.toMap(keyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
                }
            }
        }, false, null);
    }

    @Override
    public <K, V> Stream<Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        return groupBy(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V> Stream<Entry<K, V>> groupBy(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) {
        return newStream(new ObjIteratorEx<Entry<K, V>>() {
            private Iterator<Entry<K, V>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<K, V> next() {
                init();
                return iter.next();
            }

            private void init() {
                if (iter == null) {
                    iter = AbstractStream.this.toMap(keyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
                }
            }
        }, false, null);
    }

    //    @Override
    //    public <K> Stream<Entry<K, List<T>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper) {
    //        return flatGroupBy(flatKeyMapper, Suppliers.<K, List<T>> ofMap());
    //    }
    //
    //    @Override
    //    public <K> Stream<Entry<K, List<T>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            Supplier<? extends Map<K, List<T>>> mapFactory) {
    //        return flatGroupBy(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, List<V>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flatGroupBy(flatKeyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, List<V>>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends Map<K, List<V>>> mapFactory) {
    //        return flatGroupBy(flatKeyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, A, D> Stream<Entry<K, D>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, Collector<? super T, A, D> downstream) {
    //        return flatGroupBy(flatKeyMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, A, D> Stream<Entry<K, D>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, Collector<? super T, A, D> downstream,
    //            Supplier<? extends Map<K, D>> mapFactory) {
    //        return flatGroupBy(flatKeyMapper, BiFunctions.<K, T> returnSecond(), downstream, mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Stream<Entry<K, D>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream) {
    //        return flatGroupBy(flatKeyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Stream<Entry<K, D>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends Map<K, D>> mapFactory) {
    //        return newStream(new ObjIteratorEx<Entry<K, D>>() {
    //            private Iterator<Entry<K, D>> iter = null;
    //
    //            @Override
    //            public boolean hasNext() {
    //                init();
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public Entry<K, D> next() {
    //                init();
    //                return iter.next();
    //            }
    //
    //            private void init() {
    //                if (iter == null) {
    //                    iter = AbstractStream.this.flatToMap(flatKeyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
    //                }
    //            }
    //        }, false, null);
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, V>> flatGroupBy(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
    //        return flatGroupBy(flatKeyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, V>> flatGroupBy(final Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction,
    //            final Supplier<? extends Map<K, V>> mapFactory) {
    //        return newStream(new ObjIteratorEx<Entry<K, V>>() {
    //            private Iterator<Entry<K, V>> iter = null;
    //
    //            @Override
    //            public boolean hasNext() {
    //                init();
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public Entry<K, V> next() {
    //                init();
    //                return iter.next();
    //            }
    //
    //            private void init() {
    //                if (iter == null) {
    //                    iter = AbstractStream.this.flatToMap(flatKeyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
    //                }
    //            }
    //        }, false, null);
    //    }
    //
    //    @Override
    //    public <K> Stream<Entry<K, List<T>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper) {
    //        return flattGroupBy(flatKeyMapper, Suppliers.<K, List<T>> ofMap());
    //    }
    //
    //    @Override
    //    public <K> Stream<Entry<K, List<T>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            Supplier<? extends Map<K, List<T>>> mapFactory) {
    //        return flattGroupBy(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, List<V>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flattGroupBy(flatKeyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, List<V>>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends Map<K, List<V>>> mapFactory) {
    //        return flattGroupBy(flatKeyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, A, D> Stream<Entry<K, D>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            Collector<? super T, A, D> downstream) {
    //        return flattGroupBy(flatKeyMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, A, D> Stream<Entry<K, D>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            Collector<? super T, A, D> downstream, Supplier<? extends Map<K, D>> mapFactory) {
    //        return flattGroupBy(flatKeyMapper, BiFunctions.<K, T> returnSecond(), downstream, mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Stream<Entry<K, D>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream) {
    //        return flattGroupBy(flatKeyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Stream<Entry<K, D>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends Map<K, D>> mapFactory) {
    //        return newStream(new ObjIteratorEx<Entry<K, D>>() {
    //            private Iterator<Entry<K, D>> iter = null;
    //
    //            @Override
    //            public boolean hasNext() {
    //                init();
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public Entry<K, D> next() {
    //                init();
    //                return iter.next();
    //            }
    //
    //            private void init() {
    //                if (iter == null) {
    //                    iter = AbstractStream.this.flattToMap(flatKeyMapper, valueMapper, downstream, mapFactory).entrySet().iterator();
    //                }
    //            }
    //        }, false, null);
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, V>> flattGroupBy(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
    //        return flattGroupBy(flatKeyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Stream<Entry<K, V>> flattGroupBy(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction,
    //            final Supplier<? extends Map<K, V>> mapFactory) {
    //        return newStream(new ObjIteratorEx<Entry<K, V>>() {
    //            private Iterator<Entry<K, V>> iter = null;
    //
    //            @Override
    //            public boolean hasNext() {
    //                init();
    //                return iter.hasNext();
    //            }
    //
    //            @Override
    //            public Entry<K, V> next() {
    //                init();
    //                return iter.next();
    //            }
    //
    //            private void init() {
    //                if (iter == null) {
    //                    iter = AbstractStream.this.flattToMap(flatKeyMapper, valueMapper, mergeFunction, mapFactory).entrySet().iterator();
    //                }
    //            }
    //        }, false, null);
    //    }

    @Override
    public Stream<Entry<Boolean, List<T>>> partitionBy(Predicate<? super T> predicate) {
        return partitionBy(predicate, Collectors.<T> toList());
    }

    @Override
    public <A, D> Stream<Entry<Boolean, D>> partitionBy(final Predicate<? super T> predicate, final Collector<? super T, A, D> downstream) {
        return newStream(new ObjIteratorEx<Entry<Boolean, D>>() {
            private Iterator<Entry<Boolean, D>> iter = null;

            @Override
            public boolean hasNext() {
                init();
                return iter.hasNext();
            }

            @Override
            public Entry<Boolean, D> next() {
                init();
                return iter.next();
            }

            private void init() {
                if (iter == null) {
                    iter = AbstractStream.this.partitionTo(predicate, downstream).entrySet().iterator();
                }
            }
        }, false, null);
    }

    @Override
    public EntryStream<Boolean, List<T>> partitionByToEntry(Predicate<? super T> predicate) {
        return partitionByToEntry(predicate, Collectors.<T> toList());
    }

    @Override
    public <A, D> EntryStream<Boolean, D> partitionByToEntry(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
        return partitionBy(predicate, downstream).mapToEntry(Fn.<Map.Entry<Boolean, D>> identity());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(Function<? super T, ? extends K> keyMapper) {
        return groupByToEntry(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    @Override
    public <K> EntryStream<K, List<T>> groupByToEntry(Function<? super T, ? extends K> keyMapper, Supplier<? extends Map<K, List<T>>> mapFactory) {
        return groupByToEntry(keyMapper, Fn.<T> identity(), mapFactory);
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return groupByToEntry(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    @Override
    public <K, V> EntryStream<K, List<V>> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends Map<K, List<V>>> mapFactory) {
        return groupBy(keyMapper, valueMapper, mapFactory).mapToEntry(Fn.<Map.Entry<K, List<V>>> identity());
    }

    @Override
    public <K, A, D> EntryStream<K, D> groupByToEntry(Function<? super T, ? extends K> keyMapper, Collector<? super T, A, D> downstream) {
        return groupByToEntry(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, A, D> EntryStream<K, D> groupByToEntry(Function<? super T, ? extends K> keyMapper, Collector<? super T, A, D> downstream,
            Supplier<? extends Map<K, D>> mapFactory) {
        return groupBy(keyMapper, downstream, mapFactory).mapToEntry(Fn.<Map.Entry<K, D>> identity());
    }

    @Override
    public <K, V, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            Collector<? super V, A, D> downstream) {
        return groupByToEntry(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, V, A, D> EntryStream<K, D> groupByToEntry(final Function<? super T, ? extends K> keyMapper, final Function<? super T, ? extends V> valueMapper,
            Collector<? super V, A, D> downstream, Supplier<? extends Map<K, D>> mapFactory) {
        return groupBy(keyMapper, valueMapper, downstream, mapFactory).mapToEntry(Fn.<Map.Entry<K, D>> identity());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction) {
        return groupByToEntry(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V> EntryStream<K, V> groupByToEntry(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            BinaryOperator<V> mergeFunction, Supplier<? extends Map<K, V>> mapFactory) {
        return groupBy(keyMapper, valueMapper, mergeFunction, mapFactory).mapToEntry(Fn.<Map.Entry<K, V>> identity());
    }

    @Override
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toMap(keyMapper, valueMapper, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, V>> M toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    }

    @Override
    public <K, V> Map<K, V> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
        return toMap(keyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    }

    @Override
    public <K, A, D> Map<K, D> toMap(Function<? super T, ? extends K> keyMapper, Collector<? super T, A, D> downstream) {
        return toMap(keyMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, V, A, D> Map<K, D> toMap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Collector<? super V, A, D> downstream) {
        return toMap(keyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    }

    @Override
    public <K, A, D, M extends Map<K, D>> M toMap(final Function<? super T, ? extends K> keyMapper, final Collector<? super T, A, D> downstream,
            final Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, Fn.<T> identity(), downstream, mapFactory);
    }

    //    @Override
    //    public <K, V> Map<K, V> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flatToMap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Map<K, V> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
    //        return flatToMap(flatKeyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, V>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
    //        return flatToMap(flatKeyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, A, D> Map<K, D> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, Collector<? super T, A, D> downstream) {
    //        return flatToMap(flatKeyMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, A, D, M extends Map<K, D>> M flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, Collector<? super T, A, D> downstream,
    //            Supplier<? extends M> mapFactory) {
    //        return flatToMap(flatKeyMapper, BiFunctions.<K, T> returnSecond(), downstream, mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Map<K, D> flatToMap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream) {
    //        return flatToMap(flatKeyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, A, D> Map<K, D> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper, Collector<? super T, A, D> downstream) {
    //        return flattToMap(flatKeyMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, A, D, M extends Map<K, D>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            Collector<? super T, A, D> downstream, Supplier<? extends M> mapFactory) {
    //        return flattToMap(flatKeyMapper, BiFunctions.<K, T> returnSecond(), downstream, mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V> Map<K, V> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flattToMap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V> Map<K, V> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, BinaryOperator<V> mergeFunction) {
    //        return flattToMap(flatKeyMapper, valueMapper, mergeFunction, Suppliers.<K, V> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, V>> M flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
    //        return flattToMap(flatKeyMapper, valueMapper, Fn.<V> throwingMerger(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, V>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
    //        return flatToMap(new Function<T, Stream<K>>() {
    //            @Override
    //            public Stream<K> apply(T t) {
    //                return Stream.of(flatKeyMapper.apply(t));
    //            }
    //        }, valueMapper, mergeFunction, mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V, A, D> Map<K, D> flattToMap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Collector<? super V, A, D> downstream) {
    //        return flattToMap(flatKeyMapper, valueMapper, downstream, Suppliers.<K, D> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, A, D, M extends Map<K, D>> M flattToMap(final Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            final BiFunction<? super K, ? super T, ? extends V> valueMapper, final Collector<? super V, A, D> downstream,
    //            final Supplier<? extends M> mapFactory) {
    //        return flatToMap(new Function<T, Stream<K>>() {
    //            @Override
    //            public Stream<K> apply(T t) {
    //                return Stream.of(flatKeyMapper.apply(t));
    //            }
    //        }, valueMapper, downstream, mapFactory);
    //    }

    @Override
    public <K> Map<K, List<T>> groupTo(Function<? super T, ? extends K> keyMapper) {
        return groupTo(keyMapper, Suppliers.<K, List<T>> ofMap());
    }

    @Override
    public <K, M extends Map<K, List<T>>> M groupTo(Function<? super T, ? extends K> keyMapper, Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, Collectors.<T> toList(), mapFactory);
    }

    @Override
    public <K, V> Map<K, List<V>> groupTo(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return groupTo(keyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    }

    @Override
    public <K, V, M extends Map<K, List<V>>> M groupTo(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper,
            Supplier<? extends M> mapFactory) {
        return toMap(keyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    }

    //    @Override
    //    public <K> Map<K, List<T>> flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper) {
    //        return flatGroupTo(flatKeyMapper, Suppliers.<K, List<T>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, M extends Map<K, List<T>>> M flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper, Supplier<? extends M> mapFactory) {
    //        return flatGroupTo(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V> Map<K, List<V>> flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flatGroupTo(flatKeyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, List<V>>> M flatGroupTo(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
    //        return flatToMap(flatKeyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K> Map<K, List<T>> flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper) {
    //        return flattGroupTo(flatKeyMapper, Suppliers.<K, List<T>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, M extends Map<K, List<T>>> M flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            Supplier<? extends M> mapFactory) {
    //        return flattGroupTo(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    //    }
    //
    //    @Override
    //    public <K, V> Map<K, List<V>> flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
    //        return flattGroupTo(flatKeyMapper, valueMapper, Suppliers.<K, List<V>> ofMap());
    //    }
    //
    //    @Override
    //    public <K, V, M extends Map<K, List<V>>> M flattGroupTo(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
    //            BiFunction<? super K, ? super T, ? extends V> valueMapper, Supplier<? extends M> mapFactory) {
    //        return flattToMap(flatKeyMapper, valueMapper, Collectors.<V> toList(), mapFactory);
    //    }

    @Override
    public Map<Boolean, List<T>> partitionTo(Predicate<? super T> predicate) {
        return partitionTo(predicate, Collectors.<T> toList());
    }

    @Override
    public <A, D> Map<Boolean, D> partitionTo(Predicate<? super T> predicate, Collector<? super T, A, D> downstream) {
        return collect(Collectors.partitioningBy(predicate, downstream));
    }

    @Override
    public <K> ListMultimap<K, T> toMultimap(Function<? super T, ? extends K> keyMapper) {
        return toMultimap(keyMapper, Suppliers.<K, T> ofListMultimap());
    }

    @Override
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>> M toMultimap(Function<? super T, ? extends K> keyMapper,
            Supplier<? extends M> mapFactory) {
        return toMultimap(keyMapper, Fn.<T> identity(), mapFactory);
    }

    @Override
    public <K, V> ListMultimap<K, V> toMultimap(Function<? super T, ? extends K> keyMapper, Function<? super T, ? extends V> valueMapper) {
        return toMultimap(keyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    @Override
    public <K> ListMultimap<K, T> flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper) {
        return flatToMultimap(flatKeyMapper, Suppliers.<K, T> ofListMultimap());
    }

    @Override
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>> M flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            Supplier<? extends M> mapFactory) {
        return flatToMultimap(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    }

    @Override
    public <K, V> ListMultimap<K, V> flatToMultimap(Function<? super T, ? extends Stream<? extends K>> flatKeyMapper,
            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
        return flatToMultimap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    @Override
    public <K> ListMultimap<K, T> flattToMultimap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper) {
        return flattToMultimap(flatKeyMapper, Suppliers.<K, T> ofListMultimap());
    }

    @Override
    public <K, V extends Collection<T>, M extends Multimap<K, T, V>> M flattToMultimap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
            Supplier<? extends M> mapFactory) {
        return flattToMultimap(flatKeyMapper, BiFunctions.<K, T> returnSecond(), mapFactory);
    }

    @Override
    public <K, V> ListMultimap<K, V> flattToMultimap(Function<? super T, ? extends Collection<? extends K>> flatKeyMapper,
            BiFunction<? super K, ? super T, ? extends V> valueMapper) {
        return flattToMultimap(flatKeyMapper, valueMapper, Suppliers.<K, V> ofListMultimap());
    }

    @Override
    public int sumInt(ToIntFunction<? super T> mapper) {
        return collect(Collectors.summingInt(mapper));
    }

    @Override
    public long sumLong(ToLongFunction<? super T> mapper) {
        return collect(Collectors.summingLong(mapper));
    }

    @Override
    public double sumDouble(ToDoubleFunction<? super T> mapper) {
        return collect(Collectors.summingDouble(mapper));
    }

    @Override
    public OptionalDouble averageInt(ToIntFunction<? super T> mapper) {
        return collect(Collectors.averagingIntt(mapper));
    }

    @Override
    public OptionalDouble averageLong(ToLongFunction<? super T> mapper) {
        return collect(Collectors.averagingLongg(mapper));
    }

    @Override
    public OptionalDouble averageDouble(ToDoubleFunction<? super T> mapper) {
        return collect(Collectors.averagingDoubble(mapper));
    }

    @Override
    public <U> Stream<Pair<T, U>> innerJoin(final Collection<U> b, final Function<? super T, ?> leftKeyMapper, final Function<? super U, ?> rightKeyMapper) {
        final ListMultimap<Object, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                return Stream.of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        return Pair.of(t, u);
                    }
                });
            }
        });
    }

    @Override
    public <U> Stream<Pair<T, U>> innerJoin(final Collection<U> b, final BiPredicate<? super T, ? super U> predicate) {
        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                return Stream.of(b).filter(new Predicate<U>() {
                    @Override
                    public boolean test(final U u) {
                        return predicate.test(t, u);
                    }
                }).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        return Pair.of(t, u);
                    }
                });
            }
        });
    }

    @Override
    public <U> Stream<Pair<T, U>> fullJoin(final Collection<U> b, final Function<? super T, ?> leftKeyMapper, final Function<? super U, ?> rightKeyMapper) {
        final ListMultimap<Object, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final boolean isParallelStream = this.isParallel();

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? Stream.of(Pair.of(t, (U) null)) : Stream.of(values).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        if (isParallelStream) {
                            synchronized (joinedRights) {
                                joinedRights.put(u, u);
                            }
                        } else {
                            joinedRights.put(u, u);
                        }

                        return Pair.of(t, u);
                    }
                });
            }
        }).append(Stream.of(b).filter(new Predicate<U>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Function<U, Pair<T, U>>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    @Override
    public <U> Stream<Pair<T, U>> fullJoin(final Collection<U> b, final BiPredicate<? super T, ? super U> predicate) {
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final boolean isParallelStream = this.isParallel();

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                final MutableBoolean joined = MutableBoolean.of(false);

                return Stream.of(b).filter(new Predicate<U>() {
                    @Override
                    public boolean test(final U u) {
                        return predicate.test(t, u);
                    }
                }).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        joined.setTrue();

                        if (isParallelStream) {
                            synchronized (joinedRights) {
                                joinedRights.put(u, u);
                            }
                        } else {
                            joinedRights.put(u, u);
                        }

                        return Pair.of(t, u);
                    }
                }).append(Stream.iterate(new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() {
                        return joined.isFalse();
                    }
                }, new Supplier<Pair<T, U>>() {
                    @Override
                    public Pair<T, U> get() {
                        joined.setTrue();
                        return Pair.of(t, (U) null);
                    }
                }));
            }
        }).append(Stream.of(b).filter(new Predicate<U>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Function<U, Pair<T, U>>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    @Override
    public <U> Stream<Pair<T, U>> leftJoin(final Collection<U> b, final Function<? super T, ?> leftKeyMapper, final Function<? super U, ?> rightKeyMapper) {
        final ListMultimap<Object, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                final List<U> values = rightKeyMap.get(leftKeyMapper.apply(t));

                return N.isNullOrEmpty(values) ? Stream.of(Pair.of(t, (U) null)) : Stream.of(values).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        return Pair.of(t, u);
                    }
                });
            }
        });
    }

    @Override
    public <U> Stream<Pair<T, U>> leftJoin(final Collection<U> b, final BiPredicate<? super T, ? super U> predicate) {
        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                final MutableBoolean joined = MutableBoolean.of(false);

                return Stream.of(b).filter(new Predicate<U>() {
                    @Override
                    public boolean test(final U u) {
                        return predicate.test(t, u);
                    }
                }).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        joined.setTrue();

                        return Pair.of(t, u);
                    }
                }).append(Stream.iterate(new BooleanSupplier() {
                    @Override
                    public boolean getAsBoolean() {
                        return joined.isFalse();
                    }
                }, new Supplier<Pair<T, U>>() {
                    @Override
                    public Pair<T, U> get() {
                        joined.setTrue();
                        return Pair.of(t, (U) null);
                    }
                }));
            }
        });
    }

    @Override
    public <U> Stream<Pair<T, U>> rightJoin(final Collection<U> b, final Function<? super T, ?> leftKeyMapper, final Function<? super U, ?> rightKeyMapper) {
        final ListMultimap<Object, U> rightKeyMap = ListMultimap.from(b, rightKeyMapper);
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final boolean isParallelStream = this.isParallel();

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                return Stream.of(rightKeyMap.get(leftKeyMapper.apply(t))).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {
                        if (isParallelStream) {
                            synchronized (joinedRights) {
                                joinedRights.put(u, u);
                            }
                        } else {
                            joinedRights.put(u, u);
                        }

                        return Pair.of(t, u);
                    }
                });
            }
        }).append(Stream.of(b).filter(new Predicate<U>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Function<U, Pair<T, U>>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    @Override
    public <U> Stream<Pair<T, U>> rightJoin(final Collection<U> b, final BiPredicate<? super T, ? super U> predicate) {
        final Map<U, U> joinedRights = new IdentityHashMap<>();
        final boolean isParallelStream = this.isParallel();

        return flatMap(new Function<T, Stream<Pair<T, U>>>() {
            @Override
            public Stream<Pair<T, U>> apply(final T t) {
                return Stream.of(b).filter(new Predicate<U>() {
                    @Override
                    public boolean test(final U u) {
                        return predicate.test(t, u);
                    }
                }).map(new Function<U, Pair<T, U>>() {
                    @Override
                    public Pair<T, U> apply(U u) {

                        if (isParallelStream) {
                            synchronized (joinedRights) {
                                joinedRights.put(u, u);
                            }
                        } else {
                            joinedRights.put(u, u);
                        }

                        return Pair.of(t, u);
                    }
                });
            }
        }).append(Stream.of(b).filter(new Predicate<U>() {
            @Override
            public boolean test(U u) {
                return joinedRights.containsKey(u) == false;
            }
        }).map(new Function<U, Pair<T, U>>() {
            @Override
            public Pair<T, U> apply(U u) {
                return Pair.of((T) null, u);
            }
        }));
    }

    @Override
    public <E extends Exception> Optional<T> findAny(final Try.Predicate<? super T, E> predicate) throws E {
        return findFirst(predicate);
    }

    @Override
    public <E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(final Try.Predicate<? super T, E> predicateForFirst,
            final Try.Predicate<? super T, E2> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final ObjIteratorEx<T> iter = iteratorEx();
            T last = (T) NONE;
            T next = null;

            while (iter.hasNext()) {
                next = iter.next();

                if (predicateForFirst.test(next)) {
                    return Optional.of(next);
                } else if (predicateForLast.test(next)) {
                    last = next;
                }
            }

            return last == NONE ? (Optional<T>) Optional.empty() : Optional.of(last);
        } finally {
            close();
        }
    }

    @Override
    public <U, E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(final U init,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final ObjIteratorEx<T> iter = iteratorEx();
            T last = (T) NONE;
            T next = null;

            while (iter.hasNext()) {
                next = iter.next();

                if (predicateForFirst.test(next, init)) {
                    return Optional.of(next);
                } else if (predicateForLast.test(next, init)) {
                    last = next;
                }
            }

            return last == NONE ? (Optional<T>) Optional.empty() : Optional.of(last);
        } finally {
            close();
        }
    }

    @Override
    public <U, E extends Exception, E2 extends Exception> Optional<T> findFirstOrLast(final Function<? super T, U> preFunc,
            final Try.BiPredicate<? super T, ? super U, E> predicateForFirst, final Try.BiPredicate<? super T, ? super U, E2> predicateForLast) throws E, E2 {
        assertNotClosed();

        try {
            final ObjIteratorEx<T> iter = iteratorEx();
            U init = null;
            T last = (T) NONE;
            T next = null;

            while (iter.hasNext()) {
                next = iter.next();
                init = preFunc.apply(next);

                if (predicateForFirst.test(next, init)) {
                    return Optional.of(next);
                } else if (predicateForLast.test(next, init)) {
                    last = next;
                }
            }

            return last == NONE ? (Optional<T>) Optional.empty() : Optional.of(last);
        } finally {
            close();
        }
    }

    @Override
    @SafeVarargs
    public final boolean containsAll(final T... a) {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(a)) {
                return true;
            } else if (a.length == 1) {
                return anyMatch(Fn.equal(a[0]));
            } else if (a.length == 2) {
                return filter(new Predicate<T>() {
                    private final T val1 = a[0];
                    private final T val2 = a[1];

                    @Override
                    public boolean test(T t) {
                        return N.equals(t, val1) || N.equals(t, val2);
                    }
                }).distinct().limit(2).count() == 2;
            } else {
                return containsAll(N.asSet(a));
            }
        } finally {
            close();
        }
    }

    @Override
    public boolean containsAll(final Collection<? extends T> c) {
        assertNotClosed();

        try {
            if (N.isNullOrEmpty(c)) {
                return true;
            } else if (c.size() == 1) {
                final T val = c instanceof List ? ((List<T>) c).get(0) : c.iterator().next();
                return anyMatch(Fn.equal(val));
            } else {
                final Set<T> set = c instanceof Set ? (Set<T>) c : N.newHashSet(c);
                return filter(new Predicate<T>() {
                    @Override
                    public boolean test(T t) {
                        return set.contains(t);
                    }
                }).distinct().limit(set.size()).count() == set.size();
            }
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> first() {
        assertNotClosed();

        try {
            final Iterator<T> iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return Optional.empty();
            }

            return Optional.of(iter.next());
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> last() {
        assertNotClosed();

        try {
            final Iterator<T> iter = this.iteratorEx();

            if (iter.hasNext() == false) {
                return Optional.empty();
            }

            T next = iter.next();

            while (iter.hasNext()) {
                next = iter.next();
            }

            return Optional.of(next);
        } finally {
            close();
        }
    }

    @Override
    public Optional<T> onlyOne() throws DuplicatedResultException {
        assertNotClosed();

        try {
            final Iterator<T> iter = this.iteratorEx();

            final Optional<T> result = iter.hasNext() ? Optional.of(iter.next()) : Optional.<T> empty();

            if (result.isPresent() && iter.hasNext()) {
                throw new DuplicatedResultException("There are at least two elements: " + Strings.concat(result.get(), ", ", iter.next()));
            }

            return result;
        } finally {
            close();
        }
    }

    @Override
    public Stream<T> skipNull() {
        return filter(Fn.notNull());
    }

    @Override
    public Stream<List<T>> slidingToList(int windowSize) {
        return slidingToList(windowSize, 1);
    }

    @Override
    public Stream<T> intersection(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(value) > 0;
            }
        }).iteratorEx(), sorted, cmp);
    }

    @Override
    public Stream<T> intersection(final Function<? super T, ?> mapper, final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(mapper.apply(value)) > 0;
            }
        }).iteratorEx(), sorted, cmp);
    }

    @Override
    public Stream<T> difference(final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).iteratorEx(), sorted, cmp);
    }

    @Override
    public Stream<T> difference(final Function<? super T, ?> mapper, final Collection<?> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(mapper.apply(value)) < 1;
            }
        }).iteratorEx(), sorted, cmp);
    }

    @Override
    public Stream<T> symmetricDifference(final Collection<T> c) {
        final Multiset<?> multiset = Multiset.from(c);

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(value) < 1;
            }
        }).append(Stream.of(c).filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return multiset.getAndRemove(value) > 0;
            }
        })).iteratorEx(), false, null);
    }

    @Override
    public Stream<T> reversed() {
        return newStream(new ObjIteratorEx<T>() {
            private boolean initialized = false;
            private T[] aar;
            private int cursor;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor > 0;
            }

            @Override
            public T next() {
                if (initialized == false) {
                    init();
                }

                if (cursor <= 0) {
                    throw new NoSuchElementException();
                }

                return aar[--cursor];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n < cursor ? cursor - (int) n : 0;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (initialized == false) {
                    init();
                }

                a = a.length >= cursor ? a : (A[]) N.newArray(a.getClass().getComponentType(), cursor);

                for (int i = 0; i < cursor; i++) {
                    a[i] = (A) aar[cursor - i - 1];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) AbstractStream.this.toArray();
                    cursor = aar.length;
                }
            }
        }, false, null);
    }

    @Override
    public Stream<T> shuffled(final Random rnd) {
        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                N.shuffle(a, rnd);
                return a;
            }
        }, false, null);
    }

    @Override
    public Stream<T> rotated(final int distance) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean initialized = false;
            private T[] aar;
            private int len;
            private int start;
            private int cnt = 0;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cnt < len;
            }

            @Override
            public T next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return aar[(start + cnt++) % len];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return len - cnt;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cnt = n < len - cnt ? cnt + (int) n : len;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (initialized == false) {
                    init();
                }

                a = a.length >= len - cnt ? a : (A[]) N.newArray(a.getClass().getComponentType(), len - cnt);

                for (int i = cnt; i < len; i++) {
                    a[i - cnt] = (A) aar[(start + i) % len];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) AbstractStream.this.toArray();
                    len = aar.length;

                    if (len > 0) {
                        start = distance % len;

                        if (start < 0) {
                            start += len;
                        }

                        start = len - start;
                    }
                }
            }
        }, distance == 0 && sorted, distance == 0 ? cmp : null);
    }

    @Override
    public Stream<T> sorted() {
        return sorted(NATURAL_COMPARATOR);
    }

    @Override
    public Stream<T> reverseSorted() {
        return sorted(REVERSED_COMPARATOR);
    }

    @Override
    public Stream<T> sorted(final Comparator<? super T> comparator) {
        final Comparator<? super T> cmp = comparator == null ? NATURAL_COMPARATOR : comparator;

        if (sorted && cmp == this.cmp) {
            return newStream(iteratorEx(), sorted, cmp);
        }

        return lazyLoad(new Function<Object[], Object[]>() {
            @Override
            public Object[] apply(final Object[] a) {
                if (isParallel()) {
                    N.parallelSort((T[]) a, cmp);
                } else {
                    N.sort((T[]) a, cmp);
                }

                return a;
            }
        }, true, cmp);
    }

    private Stream<T> lazyLoad(final Function<Object[], Object[]> op, final boolean sorted, final Comparator<? super T> cmp) {
        return newStream(new ObjIteratorEx<T>() {
            private boolean initialized = false;
            private T[] aar;
            private int cursor = 0;
            private int len;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < len;
            }

            @Override
            public T next() {
                if (initialized == false) {
                    init();
                }

                if (cursor >= len) {
                    throw new NoSuchElementException();
                }

                return aar[cursor++];
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return len - cursor;
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n > len - cursor ? len : cursor + (int) n;
            }

            @Override
            public <A> A[] toArray(A[] a) {
                if (initialized == false) {
                    init();
                }

                a = a.length >= (len - cursor) ? a : (A[]) N.newArray(a.getClass().getComponentType(), (len - cursor));

                for (int i = cursor; i < len; i++) {
                    a[i - cursor] = (A) aar[i];
                }

                return a;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    aar = (T[]) op.apply(AbstractStream.this.toArray());
                    len = aar.length;
                }
            }
        }, sorted, cmp);
    }

    @Override
    public Stream<T> distinct() {
        final Set<Object> set = new HashSet<>();

        return newStream(this.sequential().filter(new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return set.add(hashKey(value));
            }
        }).iteratorEx(), sorted, cmp);
    }

    @Override
    public Stream<T> distinctBy(final Function<? super T, ?> keyMapper) {
        //    final Set<Object> set = new HashSet<>();
        //
        //    final Predicate<T> predicate = isParallel() ? new Predicate<T>() {
        //        @Override
        //        public boolean test(T value) {
        //            final Object key = hashKey(keyMapper.apply(value));
        //
        //            synchronized (set) {
        //                return set.add(key);
        //            }
        //        }
        //    } : new Predicate<T>() {
        //        @Override
        //        public boolean test(T value) {
        //            return set.add(hashKey(keyMapper.apply(value)));
        //        }
        //    };

        final Predicate<T> predicate = isParallel() ? new Predicate<T>() {
            private final ConcurrentHashMap<Object, Object> map = new ConcurrentHashMap<>();

            @Override
            public boolean test(T value) {
                return map.put(hashKey(keyMapper.apply(value)), StreamBase.NONE) == null;
            }
        } : new Predicate<T>() {
            private final Set<Object> set = new HashSet<>();

            @Override
            public boolean test(T value) {
                return set.add(hashKey(keyMapper.apply(value)));
            }
        };

        return filter(predicate);
    }

    @Override
    public Stream<T> top(int n) {
        return top(n, NATURAL_COMPARATOR);
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles() {
        assertNotClosed();

        try {
            final Object[] a = sorted().toArray();

            if (N.isNullOrEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentiles(a));
        } finally {
            close();
        }
    }

    @Override
    public Optional<Map<Percentage, T>> percentiles(Comparator<? super T> comparator) {
        assertNotClosed();

        try {
            final Object[] a = sorted(comparator).toArray();

            if (N.isNullOrEmpty(a)) {
                return Optional.empty();
            }

            return Optional.of((Map<Percentage, T>) N.percentiles(a));
        } finally {
            close();
        }
    }

    //    @Override
    //    public Stream<Set<T>> powerSet() {
    //        final Set<T> set = toSet(new Supplier<Set<T>>() {
    //            @Override
    //            public Set<T> get() {
    //                return new LinkedHashSet<>();
    //            }
    //        });
    //
    //        return newStream(Seq.powerSet(set).iterator(), false, null);
    //    }

    @Override
    public Stream<List<T>> combinations() {
        if (this instanceof ArrayStream) {
            @SuppressWarnings("resource")
            final ArrayStream<T> s = ((ArrayStream<T>) this);
            final int count = s.toIndex - s.fromIndex;

            return newStream(IntStream.rangeClosed(0, count).flatMapToObj(new IntFunction<Stream<List<T>>>() {
                @Override
                public Stream<List<T>> apply(int value) {
                    return combinations(value);
                }
            }).iteratorEx(), false, null);
        } else {
            return newStream((T[]) toArray(), false, null).combinations();
        }
    }

    @Override
    public Stream<List<T>> combinations(final int len) {
        if (this instanceof ArrayStream) {
            @SuppressWarnings("resource")
            final ArrayStream<T> s = ((ArrayStream<T>) this);
            final int count = s.toIndex - s.fromIndex;
            checkFromIndexSize(0, len, count);

            if (len == 0) {
                return newStream(N.asArray(N.<T> emptyList()), false, null);
            } else if (len == 1) {
                return map(new Function<T, List<T>>() {
                    @Override
                    public List<T> apply(T t) {
                        return N.asList(t);
                    }
                });
            } else if (len == count) {
                return newStream(N.asArray(toList()), false, null);
            } else {
                final T[] a = s.elements;
                final int fromIndex = s.fromIndex;
                final int toIndex = s.toIndex;

                return newStream(new ObjIteratorEx<List<T>>() {
                    private final int[] indices = Array.range(fromIndex, fromIndex + len);

                    @Override
                    public boolean hasNext() {
                        return indices[0] <= toIndex - len;
                    }

                    @Override
                    public List<T> next() {
                        final List<T> result = new ArrayList<>(len);

                        for (int idx : indices) {
                            result.add(a[idx]);
                        }

                        if (++indices[len - 1] == toIndex) {
                            for (int i = len - 1; i > 0; i--) {
                                if (indices[i] > toIndex - (len - i)) {
                                    indices[i - 1]++;

                                    for (int j = i; j < len; j++) {
                                        indices[j] = indices[j - 1] + 1;
                                    }
                                }
                            }
                        }

                        return result;
                    }

                }, false, null);
            }
        } else {
            return newStream((T[]) toArray(), false, null).combinations(len);
        }
    }

    @Override
    public Stream<List<T>> combinations(final int len, final boolean repeat) {
        if (repeat == false) {
            return combinations(len);
        } else {
            return newStream(new ObjIteratorEx<List<T>>() {
                private boolean initialized = false;
                private List<List<T>> list = null;
                private int size = 0;
                private int cursor = 0;

                @Override
                public boolean hasNext() {
                    if (initialized == false) {
                        init();
                    }

                    return cursor < size;
                }

                @Override
                public List<T> next() {
                    if (hasNext() == false) {
                        throw new NoSuchElementException();
                    }

                    return list.get(cursor++);
                }

                @Override
                public void skip(long n) {
                    if (initialized == false) {
                        init();
                    }

                    cursor = n <= size - cursor ? cursor + (int) n : size;
                }

                @Override
                public long count() {
                    if (initialized == false) {
                        init();
                    }

                    return size - cursor;
                }

                private void init() {
                    if (initialized == false) {
                        initialized = true;
                        list = Iterables.cartesianProduct(N.repeat(AbstractStream.this.toList(), len));
                        size = list.size();
                    }
                }
            }, false, null);
        }
    }

    @Override
    public Stream<List<T>> permutations() {
        return newStream(PermutationIterator.of(toList()), false, null);
    }

    @Override
    public Stream<List<T>> orderedPermutations() {
        return orderedPermutations(NATURAL_COMPARATOR);
    }

    @Override
    public Stream<List<T>> orderedPermutations(Comparator<? super T> comparator) {
        final Iterator<List<T>> iter = PermutationIterator.ordered(toList(), comparator == null ? NATURAL_COMPARATOR : comparator);

        return newStream(iter, false, null);
    }

    @Override
    public Stream<List<T>> cartesianProduct(final Collection<? extends Collection<? extends T>> cs) {
        final List<Collection<? extends T>> cList = new ArrayList<>(cs.size() + 1);
        cList.add(this.toList());
        cList.addAll(cs);

        return newStream(new ObjIteratorEx<List<T>>() {
            private boolean initialized = false;
            private List<List<T>> list = null;
            private int size = 0;
            private int cursor = 0;

            @Override
            public boolean hasNext() {
                if (initialized == false) {
                    init();
                }

                return cursor < size;
            }

            @Override
            public List<T> next() {
                if (hasNext() == false) {
                    throw new NoSuchElementException();
                }

                return list.get(cursor++);
            }

            @Override
            public void skip(long n) {
                if (initialized == false) {
                    init();
                }

                cursor = n <= size - cursor ? cursor + (int) n : size;
            }

            @Override
            public long count() {
                if (initialized == false) {
                    init();
                }

                return size - cursor;
            }

            private void init() {
                if (initialized == false) {
                    initialized = true;
                    list = Iterables.cartesianProduct(cList);
                    size = list.size();
                }
            }

        }, false, null);
    }

    @Override
    public <A> A[] toArray(IntFunction<A[]> generator) {
        assertNotClosed();

        try {
            final Object[] src = toArray();
            final A[] res = generator.apply(src.length);
            System.arraycopy(src, 0, res, 0, src.length);
            return res;
        } finally {
            close();
        }
    }

    @Override
    public DataSet toDataSet() {
        return toDataSet(null);
    }

    @Override
    public DataSet toDataSet(boolean isFirstTitle) {
        assertNotClosed();

        try {
            if (isFirstTitle) {
                final ObjIterator<T> iter = this.iteratorEx();

                if (iter.hasNext() == false) {
                    return N.newDataSet(new ArrayList<String>(0), new ArrayList<List<Object>>(0));
                }

                final T header = iter.next();
                final Type<?> type = N.typeOf(header.getClass());
                List<String> columnNames = null;

                if (type.isArray()) {
                    final Object[] a = (Object[]) header;
                    columnNames = new ArrayList<>(a.length);

                    for (Object e : a) {
                        columnNames.add(N.stringOf(e));
                    }
                } else if (type.isCollection()) {
                    final Collection<?> c = (Collection<?>) header;
                    columnNames = new ArrayList<>(c.size());

                    for (Object e : c) {
                        columnNames.add(N.stringOf(e));
                    }
                } else {
                    throw new IllegalArgumentException("Unsupported header type: " + type.name());
                }

                return N.newDataSet(columnNames, Iterators.toList(iter));
            } else {
                return toDataSet(null);
            }
        } finally {
            close();
        }
    }

    @Override
    public DataSet toDataSet(List<String> columnNames) {
        return N.newDataSet(columnNames, toList());
    }

    @Override
    public String join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, prefix, suffix).reuseCachedBuffer(true);
            final IteratorEx<T> iter = this.iteratorEx();

            while (iter.hasNext()) {
                joiner.append(iter.next());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @Override
    public boolean hasDuplicates() {
        assertNotClosed();

        try {
            final Set<T> set = new HashSet<>();
            final Iterator<T> iter = iteratorEx();

            while (iter.hasNext()) {
                if (set.add(iter.next()) == false) {
                    return true;
                }
            }

            return false;
        } finally {
            close();
        }
    }

    @Override
    public <R> R collect(Supplier<R> supplier, BiConsumer<? super R, ? super T> accumulator) {
        final BiConsumer<R, R> combiner = collectingCombiner;

        return collect(supplier, accumulator, combiner);
    }

    @Override
    public <R, A> R collect(java.util.stream.Collector<? super T, A, R> collector) {
        return collect(Collector.from(collector));
    }

    @Override
    public <R, A, RR> RR collectAndThen(Collector<? super T, A, R> downstream, Function<? super R, RR> func) {
        return func.apply(collect(downstream));
    }

    @Override
    public <R, A, RR> RR collectAndThen(java.util.stream.Collector<? super T, A, R> downstream, java.util.function.Function<? super R, RR> func) {
        return func.apply(collect(downstream));
    }

    @Override
    public <R> R toListAndThen(final Function<? super List<T>, R> func) {
        return func.apply(toList());
    }

    @Override
    public <R> R toSetAndThen(final Function<? super Set<T>, R> func) {
        return func.apply(toSet());
    }

    @Override
    public Stream<Indexed<T>> indexed() {
        final MutableLong idx = MutableLong.of(0);

        return newStream(this.sequential().map(new Function<T, Indexed<T>>() {
            @Override
            public Indexed<T> apply(T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        }).iteratorEx(), true, INDEXED_COMPARATOR);
    }

    @Override
    public Stream<T> queued() {
        return queued(DEFAULT_QUEUE_SIZE_PER_ITERATOR);
    }

    @Override
    public Stream<T> append(Stream<T> stream) {
        return Stream.concat(this, stream);
    }

    @Override
    public Stream<T> append(Collection<? extends T> c) {
        return append(Stream.of(c));
    }

    @Override
    public Stream<T> appendAlll(Collection<? extends Collection<? extends T>> cs) {
        return append(Stream.of(cs).flattMap(Fn.<Collection<? extends T>> identity()));
    }

    @Override
    public Stream<T> prepend(Stream<T> stream) {
        return Stream.concat(stream, this);
    }

    @Override
    public Stream<T> prepend(Collection<? extends T> c) {
        return prepend(Stream.of(c));
    }

    @Override
    public Stream<T> prependAlll(Collection<? extends Collection<? extends T>> cs) {
        return prepend(Stream.of(cs).flattMap(Fn.<Collection<? extends T>> identity()));
    }

    @Override
    public Stream<T> merge(final Stream<? extends T> b, final BiFunction<? super T, ? super T, Nth> nextSelector) {
        return Stream.merge(this, b, nextSelector);
    }

    @Override
    public <T2, R> Stream<R> zipWith(Stream<T2> b, BiFunction<? super T, ? super T2, R> zipFunction) {
        return Stream.zip(this, b, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(Stream<T2> b, Stream<T3> c, TriFunction<? super T, ? super T2, ? super T3, R> zipFunction) {
        return Stream.zip(this, b, c, zipFunction);
    }

    @Override
    public <T2, R> Stream<R> zipWith(Stream<T2> b, T valueForNoneA, T2 valueForNoneB, BiFunction<? super T, ? super T2, R> zipFunction) {
        return Stream.zip(this, b, valueForNoneA, valueForNoneB, zipFunction);
    }

    @Override
    public <T2, T3, R> Stream<R> zipWith(Stream<T2> b, Stream<T3> c, T valueForNoneA, T2 valueForNoneB, T3 valueForNoneC,
            TriFunction<? super T, ? super T2, ? super T3, R> zipFunction) {
        return Stream.zip(this, b, c, valueForNoneA, valueForNoneB, valueForNoneC, zipFunction);
    }

    @Override
    public <E extends Exception> void forEach(Try.Consumer<? super T, E> action) throws E {
        forEach(action, Fn.emptyAction());
    }

    @Override
    public long persist(File file, Try.Function<? super T, String, IOException> toLine) throws IOException {
        final Writer writer = new FileWriter(file);

        try {
            return persist(writer, toLine);
        } finally {
            IOUtil.close(writer);
        }
    }

    @Override
    public long persist(OutputStream os, Try.Function<? super T, String, IOException> toLine) throws IOException {
        final BufferedWriter bw = Objectory.createBufferedWriter(os);

        try {
            return persist(bw, toLine);
        } finally {
            Objectory.recycle(bw);
        }
    }

    @Override
    public long persist(Writer writer, Try.Function<? super T, String, IOException> toLine) throws IOException {
        assertNotClosed();

        try {
            boolean isBufferedWriter = writer instanceof BufferedWriter || writer instanceof java.io.BufferedWriter;
            final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer);
            final Iterator<T> iter = iteratorEx();
            long cnt = 0;

            try {
                while (iter.hasNext()) {
                    bw.write(toLine.apply(iter.next()));
                    bw.write(IOUtil.LINE_SEPARATOR);
                    cnt++;
                }

                bw.flush();
            } finally {
                if (!isBufferedWriter) {
                    Objectory.recycle((BufferedWriter) bw);
                }
            }

            return cnt;
        } finally {
            close();
        }
    }

    @Override
    public long persist(final Connection conn, final String insertSQL, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws SQLException {
        PreparedStatement stmt = null;

        try {
            stmt = conn.prepareStatement(insertSQL);

            return persist(stmt, batchSize, batchInterval, stmtSetter);
        } finally {
            JdbcUtil.closeQuietly(stmt);
        }
    }

    @Override
    public long persist(final PreparedStatement stmt, final int batchSize, final int batchInterval,
            final JdbcUtil.BiParametersSetter<? super PreparedStatement, ? super T> stmtSetter) throws SQLException {
        checkArgument(batchSize > 0 && batchInterval >= 0, "'batchSize'=%s must be greater than 0 and 'batchInterval'=%s can't be negative", batchSize,
                batchInterval);
        assertNotClosed();

        try {
            final Iterator<T> iter = iteratorEx();

            long cnt = 0;
            while (iter.hasNext()) {
                stmtSetter.accept(stmt, iter.next());

                stmt.addBatch();

                if ((++cnt % batchSize) == 0) {
                    stmt.executeBatch();
                    stmt.clearBatch();

                    if (batchInterval > 0) {
                        N.sleep(batchInterval);
                    }
                }
            }

            if ((cnt % batchSize) > 0) {
                stmt.executeBatch();
                stmt.clearBatch();
            }

            return cnt;
        } finally {
            close();
        }
    }

    /**
     * @param keyMapper
     * @param valueMapper
     * @return
     * @deprecated
     */
    @Deprecated
    @Override
    public <K, V> Stream<DisposableEntry<K, V>> mapToDisposableEntry(final Function<? super T, ? extends K> keyMapper,
            final Function<? super T, ? extends V> valueMapper) {
        checkState(isParallel() == false, "mapToDisposableEntry can't be applied to parallel stream");

        final Function<T, DisposableEntry<K, V>> mapper = new Function<T, DisposableEntry<K, V>>() {
            private final EntryStream.ReusableEntry<K, V> entry = new EntryStream.ReusableEntry<>();

            @Override
            public DisposableEntry<K, V> apply(T t) {
                entry.set(keyMapper.apply(t), valueMapper.apply(t));

                return entry;
            }
        };

        return map(mapper);
    }
}
