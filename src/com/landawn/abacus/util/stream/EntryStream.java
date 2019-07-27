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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.ParallelSupported;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.util.AsyncExecutor;
import com.landawn.abacus.util.BiIterator;
import com.landawn.abacus.util.Comparators;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ImmutableEntry;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.LongMultiset;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.NoCachingNoUpdating;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableEntry;
import com.landawn.abacus.util.Nth;
import com.landawn.abacus.util.ObjIterator;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Try;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.stream.BaseStream.Splitor;

/** 
 * The Stream will be automatically closed after execution(A terminal method is executed/triggered).
 * 
 */
public final class EntryStream<K, V> implements AutoCloseable {

    private static final Function<Map<Object, Object>, Stream<Map.Entry<Object, Object>>> mapper_func = new Function<Map<Object, Object>, Stream<Map.Entry<Object, Object>>>() {
        @Override
        public Stream<Map.Entry<Object, Object>> apply(Map<Object, Object> t) {
            return Stream.of(t);
        }
    };

    final Map<K, V> m;
    final Stream<Map.Entry<K, V>> s;

    EntryStream(final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        this(null, s);
    }

    EntryStream(final Map<K, V> m, final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        this.m = m;
        this.s = (Stream<Map.Entry<K, V>>) s;
    }

    public Stream<K> keys() {
        if (m != null) {
            return Stream.of(m.keySet());
        }

        final Function<Map.Entry<K, V>, K> func = Fn.key();

        return s.map(func);
    }

    public Stream<V> values() {
        if (m != null) {
            return Stream.of(m.values());
        }

        final Function<Map.Entry<K, V>, V> func = Fn.value();

        return s.map(func);
    }

    public Stream<Map.Entry<K, V>> entries() {
        return s;
    }

    @ParallelSupported
    public EntryStream<V, K> inversed() {
        final Function<Map.Entry<K, V>, Map.Entry<V, K>> mapper = Fn.inverse();

        return map(mapper);
    }

    /**
     * Returns a stream consisting of the elements of this stream which keys are
     * instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <KK> a type of keys to select.
     * @param clazz a class to filter the keys.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    @SequentialOnly
    public <KK> EntryStream<KK, V> selectByKey(Class<KK> clazz) {
        if (isParallel()) {
            return (EntryStream<KK, V>) sequential().filterByKey(Fn.instanceOf(clazz)).parallel(maxThreadNum(), splitor());
        } else {
            return (EntryStream<KK, V>) filterByKey(Fn.instanceOf(clazz));
        }
    }

    /**
     * Returns a stream consisting of the elements of this stream which values
     * are instances of given class.
     *
     * <p>
     * This is an <a href="package-summary.html#StreamOps">intermediate</a>
     * operation.
     *
     * @param <VV> a type of values to select.
     * @param clazz a class to filter the values.
     * @return the new stream
     */
    @SuppressWarnings({ "unchecked" })
    @SequentialOnly
    public <VV> EntryStream<K, VV> selectByValue(Class<VV> clazz) {
        if (isParallel()) {
            return (EntryStream<K, VV>) sequential().filterByValue(Fn.instanceOf(clazz)).parallel(maxThreadNum(), splitor());
        } else {
            return (EntryStream<K, VV>) filterByValue(Fn.instanceOf(clazz));
        }
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> filter(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(s.filter(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> filter(final BiPredicate<? super K, ? super V> predicate) {

        return of(s.filter(Fn.Entries.p(predicate)));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> filterByKey(final Predicate<? super K> keyPredicate) {
        final Predicate<Map.Entry<K, V>> predicate = Fn.testByKey(keyPredicate);

        return of(s.filter(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> filterByValue(final Predicate<? super V> valuePredicate) {
        final Predicate<Map.Entry<K, V>> predicate = Fn.testByValue(valuePredicate);

        return of(s.filter(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> removeIf(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(s.removeIf(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> removeIf(final BiPredicate<? super K, ? super V> predicate) {
        return of(s.removeIf(Fn.Entries.p(predicate)));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> takeWhile(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(s.takeWhile(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> takeWhile(final BiPredicate<? super K, ? super V> predicate) {
        return of(s.takeWhile(Fn.Entries.p(predicate)));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> dropWhile(final Predicate<? super Map.Entry<K, V>> predicate) {
        return of(s.dropWhile(predicate));
    }

    @ParallelSupported
    public <KK> EntryStream<K, V> dropWhile(final BiPredicate<? super K, ? super V> predicate) {
        return of(s.dropWhile(Fn.Entries.p(predicate)));
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> map(final Function<? super Map.Entry<K, V>, ? extends Map.Entry<? extends KK, ? extends VV>> mapper) {
        return s.mapToEntry(mapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> map(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return s.mapToEntry(keyMapper, valueMapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> map(final BiFunction<? super K, ? super V, ? extends Map.Entry<? extends KK, ? extends VV>> mapper) {
        return map(Fn.Entries.f(mapper));
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> map(final BiFunction<? super K, ? super V, ? extends KK> keyMapper,
            final BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, VV>> mapper = new Function<Map.Entry<K, V>, Map.Entry<KK, VV>>() {
            @Override
            public Entry<KK, VV> apply(Entry<K, V> t) {
                return new SimpleImmutableEntry<KK, VV>(keyMapper.apply(t.getKey(), t.getValue()), valueMapper.apply(t.getKey(), t.getValue()));
            }
        };

        return map(mapper);
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> mapKey(final Function<? super K, ? extends KK> keyMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = Fn.mapKey(keyMapper);

        return map(mapper);
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> mapKey(final BiFunction<? super K, ? super V, ? extends KK> keyMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapper = new Function<Map.Entry<K, V>, Map.Entry<KK, V>>() {
            @Override
            public Entry<KK, V> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<KK, V>(keyMapper.apply(entry.getKey(), entry.getValue()), entry.getValue());
            }
        };

        return map(mapper);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> mapValue(final Function<? super V, ? extends VV> valueMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = Fn.mapValue(valueMapper);

        return map(mapper);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> mapValue(final BiFunction<? super K, ? super V, ? extends VV> keyMapper) {
        final Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapper = new Function<Map.Entry<K, V>, Map.Entry<K, VV>>() {
            @Override
            public Entry<K, VV> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<K, VV>(entry.getKey(), keyMapper.apply(entry.getKey(), entry.getValue()));
            }
        };

        return map(mapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flatMap(final Function<? super Map.Entry<K, V>, ? extends EntryStream<? extends KK, ? extends VV>> mapper) {
        return s.flatMappToEntry(mapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flatMap(final BiFunction<? super K, ? super V, ? extends EntryStream<? extends KK, ? extends VV>> mapper) {
        return flatMap(Fn.Entries.f(mapper));
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flattMap(
            final Function<? super Map.Entry<K, V>, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return s.flatMapToEntry(mapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flattMap(
            final BiFunction<? super K, ? super V, ? extends Stream<? extends Map.Entry<? extends KK, ? extends VV>>> mapper) {
        return flattMap(Fn.Entries.f(mapper));
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flatMapp(final Function<? super Map.Entry<K, V>, ? extends Map<? extends KK, ? extends VV>> mapper) {
        return s.flattMapToEntry(mapper);
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> flatMapp(final BiFunction<? super K, ? super V, ? extends Map<? extends KK, ? extends VV>> mapper) {
        return flatMapp(Fn.Entries.f(mapper));
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> flatMapKey(final Function<? super K, ? extends Stream<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return keyMapper.apply(e.getKey()).map(new Function<KK, Map.Entry<KK, V>>() {
                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        return new SimpleImmutableEntry<>(kk, e.getValue());
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> flatMapKey(final BiFunction<? super K, ? super V, ? extends Stream<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return keyMapper.apply(e.getKey(), e.getValue()).map(new Function<KK, Map.Entry<KK, V>>() {
                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        return new SimpleImmutableEntry<>(kk, e.getValue());
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> flattMapKey(final Function<? super K, ? extends Collection<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return Stream.of(keyMapper.apply(e.getKey())).map(new Function<KK, Map.Entry<KK, V>>() {
                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        return new SimpleImmutableEntry<>(kk, e.getValue());
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <KK> EntryStream<KK, V> flattMapKey(final BiFunction<? super K, ? super V, ? extends Collection<? extends KK>> keyMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return Stream.of(keyMapper.apply(e.getKey(), e.getValue())).map(new Function<KK, Map.Entry<KK, V>>() {
                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        return new SimpleImmutableEntry<>(kk, e.getValue());
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> flatMapValue(final Function<? super V, ? extends Stream<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return valueMapper.apply(e.getValue()).map(new Function<VV, Map.Entry<K, VV>>() {
                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        return new SimpleImmutableEntry<>(e.getKey(), vv);
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> flatMapValue(final BiFunction<? super K, ? super V, ? extends Stream<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return valueMapper.apply(e.getKey(), e.getValue()).map(new Function<VV, Map.Entry<K, VV>>() {
                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        return new SimpleImmutableEntry<>(e.getKey(), vv);
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> flattMapValue(final Function<? super V, ? extends Collection<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return Stream.of(valueMapper.apply(e.getValue())).map(new Function<VV, Map.Entry<K, VV>>() {
                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        return new SimpleImmutableEntry<>(e.getKey(), vv);
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @ParallelSupported
    public <VV> EntryStream<K, VV> flattMapValue(final BiFunction<? super K, ? super V, ? extends Collection<? extends VV>> valueMapper) {
        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return Stream.of(valueMapper.apply(e.getKey(), e.getValue())).map(new Function<VV, Map.Entry<K, VV>>() {
                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        return new SimpleImmutableEntry<>(e.getKey(), vv);
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#groupingBy(Function)
     */
    @SequentialOnly
    public EntryStream<K, List<V>> groupBy() {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(s.sequential().groupBy(keyMapper, valueMapper).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.groupBy(keyMapper, valueMapper));
        }
    }

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @SequentialOnly
    public EntryStream<K, List<V>> groupBy(final Supplier<? extends Map<K, List<V>>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(s.sequential().groupBy(keyMapper, valueMapper, mapFactory).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.groupBy(keyMapper, valueMapper, mapFactory));
        }
    }

    @ParallelSupported
    public <KK, VV> EntryStream<KK, List<VV>> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {

        return of(s.groupBy(keyMapper, valueMapper));
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public <KK, VV> EntryStream<KK, List<VV>> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<? extends Map<KK, List<VV>>> mapFactory) {

        return of(s.groupBy(keyMapper, valueMapper, mapFactory));
    }

    /**
     * 
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <A, D> EntryStream<K, D> groupBy(final Collector<? super Map.Entry<K, V>, A, D> downstream) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();

        return of(s.groupBy(keyMapper, downstream));
    }

    /**
     * 
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <A, D> EntryStream<K, D> groupBy(final Collector<? super Map.Entry<K, V>, A, D> downstream, final Supplier<? extends Map<K, D>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();

        return of(s.groupBy(keyMapper, downstream, mapFactory));
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    public <KK, A, D> EntryStream<KK, D> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, A, D> downstream) {

        return of(s.groupBy(keyMapper, downstream));
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <KK, A, D> EntryStream<KK, D> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, A, D> downstream, final Supplier<? extends Map<KK, D>> mapFactory) {

        return of(s.groupBy(keyMapper, downstream, mapFactory));
    }

    /**
     * 
     * @param mergeFunction
     * @return
     */
    @SequentialOnly
    public EntryStream<K, V> groupBy(final BinaryOperator<V> mergeFunction) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(s.sequential().groupBy(keyMapper, valueMapper, mergeFunction).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.groupBy(keyMapper, valueMapper, mergeFunction));
        }
    }

    /**
     * 
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    @SequentialOnly
    public EntryStream<K, V> groupBy(final BinaryOperator<V> mergeFunction, final Supplier<? extends Map<K, V>> mapFactory) {
        final Function<? super Map.Entry<K, V>, K> keyMapper = Fn.key();
        final Function<? super Map.Entry<K, V>, V> valueMapper = Fn.value();

        if (isParallel()) {
            return of(s.sequential().groupBy(keyMapper, valueMapper, mergeFunction, mapFactory).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.groupBy(keyMapper, valueMapper, mergeFunction, mapFactory));
        }
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#groupBy(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction) {

        return of(s.groupBy(keyMapper, valueMapper, mergeFunction));
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#groupBy(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    public <KK, VV> EntryStream<KK, VV> groupBy(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction,
            final Supplier<? extends Map<KK, VV>> mapFactory) {

        return of(s.groupBy(keyMapper, valueMapper, mergeFunction, mapFactory));
    }

    /**
     * 
     * @param collapsible
     * @return
     * @see Stream#collapse(BiPredicate, Collector)
     */
    @SequentialOnly
    public Stream<List<V>> collapseByKey(final BiPredicate<? super K, ? super K> collapsible) {
        return collapseByKey(collapsible, Collectors.<V> toList());
    }

    /**
     * Merge series of adjacent elements which satisfy the given predicate using
     * the merger function and return a new stream.
     * 
     * <br />
     * This method only run sequentially, even in parallel stream.
     * 
     * @param collapsible
     * @param collector
     * @return
     */
    @SequentialOnly
    public <R, A> Stream<R> collapseByKey(final BiPredicate<? super K, ? super K> collapsible, final Collector<? super V, A, R> collector) {
        final BiPredicate<? super Entry<K, V>, ? super Entry<K, V>> collapsible2 = new BiPredicate<Entry<K, V>, Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> t, Entry<K, V> u) {
                return collapsible.test(t.getKey(), u.getKey());
            }
        };

        final Function<Entry<K, V>, V> mapper = Fn.value();

        return s.collapse(collapsible2, Collectors.mapping(mapper, collector));
    }

    @ParallelSupported
    public EntryStream<K, V> sorted(final Comparator<? super Map.Entry<K, V>> comparator) {
        return of(s.sorted(comparator));
    }

    @ParallelSupported
    public EntryStream<K, V> sortedByKey(final Comparator<? super K> keyComparator) {
        final Comparator<Map.Entry<K, V>> comparator = Comparators.comparingByKey(keyComparator);

        return of(s.sorted(comparator));
    }

    @ParallelSupported
    public EntryStream<K, V> sortedByValue(final Comparator<? super V> valueComparator) {
        final Comparator<Map.Entry<K, V>> comparator = Comparators.comparingByValue(valueComparator);

        return of(s.sorted(comparator));
    }

    @SuppressWarnings("rawtypes")
    @ParallelSupported
    public EntryStream<K, V> sortedBy(Function<? super Map.Entry<K, V>, ? extends Comparable> keyMapper) {
        return of(s.sortedBy(keyMapper));
    }

    /**
     * 
     * @param keyMapper
     * @return 
     */
    @ParallelSupported
    public EntryStream<K, V> sortedByInt(final ToIntFunction<? super Map.Entry<K, V>> keyMapper) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingInt(keyMapper);

        return sorted(comparator);
    }

    /**
     * 
     * @param keyMapper
     * @return 
     */
    @ParallelSupported
    public EntryStream<K, V> sortedByLong(final ToLongFunction<? super Map.Entry<K, V>> keyMapper) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingLong(keyMapper);

        return sorted(comparator);
    }

    /**
     * 
     * @param keyMapper
     * @return 
     */
    @ParallelSupported
    public EntryStream<K, V> sortedByDouble(final ToDoubleFunction<? super Map.Entry<K, V>> keyMapper) {
        final Comparator<? super Map.Entry<K, V>> comparator = Comparators.comparingDouble(keyMapper);

        return sorted(comparator);
    }

    @SequentialOnly
    public EntryStream<K, V> distinct() {
        return of(s.distinct());
    }

    @SequentialOnly
    public EntryStream<K, V> distinctByKey() {
        final Function<? super Entry<K, V>, K> keyMapper = Fn.key();

        if (isParallel()) {
            return of(s.sequential().distinctBy(keyMapper).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.distinctBy(keyMapper));
        }
    }

    @SequentialOnly
    public EntryStream<K, V> distinctByValue() {
        final Function<? super Entry<K, V>, V> keyMapper = Fn.value();

        if (isParallel()) {
            return of(s.sequential().distinctBy(keyMapper).parallel(s.maxThreadNum(), s.splitor()));
        } else {
            return of(s.distinctBy(keyMapper));
        }
    }

    @ParallelSupported
    public EntryStream<K, V> distinctBy(final Function<? super Map.Entry<K, V>, ?> keyMapper) {
        return of(s.distinctBy(keyMapper));
    }

    @ParallelSupported
    public EntryStream<K, V> distinct(final Predicate<? super Long> occurrencesFilter) {
        return of(s.distinct(occurrencesFilter));
    }

    @ParallelSupported
    public EntryStream<K, V> distinctBy(final Function<? super Entry<K, V>, ?> keyMapper, final Predicate<? super Long> occurrencesFilter) {
        return of(s.distinctBy(keyMapper, occurrencesFilter));
    }

    @SequentialOnly
    public EntryStream<K, V> reversed() {
        return of(s.reversed());
    }

    @SequentialOnly
    public EntryStream<K, V> shuffled() {
        return of(s.shuffled());
    }

    @SequentialOnly
    public EntryStream<K, V> shuffled(final Random rnd) {
        return of(s.shuffled(rnd));
    }

    @SequentialOnly
    public EntryStream<K, V> rotated(final int distance) {
        return of(s.rotated(distance));
    }

    @SuppressWarnings("rawtypes")
    @SequentialOnly
    public <M extends Map<? extends K, ? extends V>> EntryStream<K, V> prepend(M map) {
        if (N.isNullOrEmpty(map)) {
            return of(s);
        }

        final Set<Map.Entry<K, V>> set = (Set) map.entrySet();

        return of(s.prepend(set));
    }

    @SuppressWarnings("rawtypes")
    @SequentialOnly
    public <M extends Map<? extends K, ? extends V>> EntryStream<K, V> append(M map) {
        if (N.isNullOrEmpty(map)) {
            return of(s);
        }

        final Set<Map.Entry<K, V>> set = (Set) map.entrySet();

        return of(s.append(set));
    }

    public EntryStream<K, V> appendIfEmpty(final Supplier<? extends EntryStream<K, V>> supplier) {
        return EntryStream.of(s.appendIfEmpty(new Supplier<Stream<Map.Entry<K, V>>>() {
            @Override
            public Stream<Map.Entry<K, V>> get() {
                return supplier.get().s;
            }
        }));
    }

    @SequentialOnly
    public EntryStream<K, V> skip(long n) {
        return of(s.skip(n));
    }

    @SequentialOnly
    public EntryStream<K, V> limit(final long maxSize) {
        return of(s.limit(maxSize));
    }

    @SequentialOnly
    public EntryStream<K, V> slice(final long from, final long to) {
        return of(s.slice(from, to));
    }

    @ParallelSupported
    public EntryStream<K, V> peek(final Consumer<? super Map.Entry<K, V>> action) {
        return of(s.peek(action));
    }

    @ParallelSupported
    public EntryStream<K, V> peek(final BiConsumer<? super K, ? super V> action) {
        return of(s.peek(Fn.Entries.c(action)));
    }

    @ParallelSupported
    public <E extends Exception> void forEach(final Try.Consumer<? super Map.Entry<K, V>, E> action) throws E {
        s.forEach(action);
    }

    @ParallelSupported
    public <E extends Exception> void forEach(final Try.BiConsumer<? super K, ? super V, E> action) throws E {
        s.forEach(Fn.Entries.ec(action));
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> min(Comparator<? super Map.Entry<K, V>> comparator) {
        return s.min(comparator);
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> minByKey(Comparator<? super K> keyComparator) {
        return s.min(Comparators.comparingBy(Fn.<K, V> key(), keyComparator));
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> minByValue(Comparator<? super V> valueComparator) {
        return s.min(Comparators.comparingBy(Fn.<K, V> value(), valueComparator));
    }

    @ParallelSupported
    @SuppressWarnings("rawtypes")
    public Optional<Map.Entry<K, V>> minBy(final Function<? super Map.Entry<K, V>, ? extends Comparable> keyMapper) {
        return s.minBy(keyMapper);
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> max(Comparator<? super Map.Entry<K, V>> comparator) {
        return s.max(comparator);
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> maxByKey(Comparator<? super K> keyComparator) {
        return s.max(Comparators.comparingBy(Fn.<K, V> key(), keyComparator));
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> maxByValue(Comparator<? super V> valueComparator) {
        return s.max(Comparators.comparingBy(Fn.<K, V> value(), valueComparator));
    }

    @ParallelSupported
    @SuppressWarnings("rawtypes")
    public Optional<Map.Entry<K, V>> maxBy(final Function<? super Map.Entry<K, V>, ? extends Comparable> keyMapper) {
        return s.maxBy(keyMapper);
    }

    @ParallelSupported
    public <E extends Exception> boolean anyMatch(final Try.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return s.anyMatch(predicate);
    }

    @ParallelSupported
    public <E extends Exception> boolean anyMatch(final Try.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return s.anyMatch(Fn.Entries.ep(predicate));
    }

    @ParallelSupported
    public <E extends Exception> boolean allMatch(final Try.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return s.allMatch(predicate);
    }

    @ParallelSupported
    public <E extends Exception> boolean allMatch(final Try.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return s.allMatch(Fn.Entries.ep(predicate));
    }

    @ParallelSupported
    public <E extends Exception> boolean noneMatch(final Try.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return s.noneMatch(predicate);
    }

    @ParallelSupported
    public <E extends Exception> boolean noneMatch(final Try.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return s.noneMatch(Fn.Entries.ep(predicate));
    }

    @ParallelSupported
    public <E extends Exception> Optional<Map.Entry<K, V>> findFirst(final Try.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return s.findFirst(predicate);
    }

    @ParallelSupported
    public <E extends Exception> Optional<Map.Entry<K, V>> findFirst(final Try.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return s.findFirst(Fn.Entries.ep(predicate));
    }

    @ParallelSupported
    public <E extends Exception> Optional<Map.Entry<K, V>> findAny(final Try.Predicate<? super Map.Entry<K, V>, E> predicate) throws E {
        return s.findAny(predicate);
    }

    @ParallelSupported
    public <E extends Exception> Optional<Map.Entry<K, V>> findAny(final Try.BiPredicate<? super K, ? super V, E> predicate) throws E {
        return s.findAny(Fn.Entries.ep(predicate));
    }

    @SequentialOnly
    public Optional<Map.Entry<K, V>> first() {
        return s.first();
    }

    @SequentialOnly
    public Optional<Map.Entry<K, V>> last() {
        return s.last();
    }

    @SequentialOnly
    public long count() {
        return s.count();
    }

    /**
     * Remember to close this Stream after the iteration is done, if required.
     * 
     * @return
     */
    @SequentialOnly
    public BiIterator<K, V> iterator() {
        final ObjIterator<Entry<K, V>> iter = s.iteratorEx();

        final BooleanSupplier hasNext = new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return iter.hasNext();
            }
        };

        final Consumer<Pair<K, V>> output = new Consumer<Pair<K, V>>() {
            private Entry<K, V> entry = null;

            @Override
            public void accept(Pair<K, V> t) {
                entry = iter.next();
                t.set(entry.getKey(), entry.getValue());
            }
        };

        return BiIterator.generate(hasNext, output);
    }

    public <T> List<T> toList(final Function<? super Map.Entry<K, V>, ? extends T> mapper) {
        return s.<T> map(mapper).toList();
    }

    public <T> List<T> toList(final BiFunction<? super K, ? super V, ? extends T> mapper) {
        return s.<T> map(Fn.Entries.f(mapper)).toList();
    }

    public <T> Set<T> toSet(final Function<? super Map.Entry<K, V>, ? extends T> mapper) {
        return s.<T> map(mapper).toSet();
    }

    public <T> Set<T> toSet(final BiFunction<? super K, ? super V, ? extends T> mapper) {
        return s.<T> map(Fn.Entries.f(mapper)).toSet();
    }

    public <T, CC extends Collection<T>> CC toCollection(final Function<? super Map.Entry<K, V>, ? extends T> mapper, final Supplier<? extends CC> supplier) {
        return s.<T> map(mapper).toCollection(supplier);
    }

    public <T, CC extends Collection<T>> CC toCollection(final BiFunction<? super K, ? super V, ? extends T> mapper, final Supplier<? extends CC> supplier) {
        return s.<T> map(Fn.Entries.f(mapper)).toCollection(supplier);
    }

    /**
     * 
     * @return
     */
    @ParallelSupported
    public ImmutableMap<K, V> toImmutableMap() {
        return ImmutableMap.of(toMap());
    }

    /**
     * 
     * @return
     */
    @ParallelSupported
    public ImmutableMap<K, V> toImmutableMap(final BinaryOperator<V> mergeFunction) {
        return ImmutableMap.of(toMap(mergeFunction));
    }

    /**
     * 
     * @return
     */
    @SequentialOnly
    public Map<K, V> toMap() {
        if (isParallel()) {
            return s.sequential().toMap(Fn.<K, V> key(), Fn.<K, V> value());
        } else {
            return s.toMap(Fn.<K, V> key(), Fn.<K, V> value());
        }
    }

    /**
     * 
     * @param mergeFunction
     * @return
     */
    @SequentialOnly
    public Map<K, V> toMap(final BinaryOperator<V> mergeFunction) {
        if (isParallel()) {
            return s.sequential().toMap(Fn.<K, V> key(), Fn.<K, V> value(), mergeFunction);
        } else {
            return s.toMap(Fn.<K, V> key(), Fn.<K, V> value(), mergeFunction);
        }
    }

    /**
     * 
     * @param mapFactory
     * @return
     */
    @SequentialOnly
    public <M extends Map<K, V>> M toMap(final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return s.sequential().toMap(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        } else {
            return s.toMap(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        }
    }

    /**
     * 
     * @param mergeFunction
     * @param mapFactory
     * @return
     */
    @SequentialOnly
    public <M extends Map<K, V>> M toMap(final BinaryOperator<V> mergeFunction, final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return s.sequential().toMap(Fn.<K, V> key(), Fn.<K, V> value(), mergeFunction, mapFactory);
        } else {
            return s.toMap(Fn.<K, V> key(), Fn.<K, V> value(), mergeFunction, mapFactory);
        }
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMap(Function, Function)
     */
    @ParallelSupported
    public <KK, VV> Map<KK, VV> toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return s.toMap(keyMapper, valueMapper);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, Supplier)
     */
    @ParallelSupported
    public <KK, VV, M extends Map<KK, VV>> M toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<? extends M> mapFactory) {
        return s.toMap(keyMapper, valueMapper, mapFactory);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator)
     */
    @ParallelSupported
    public <KK, VV> Map<KK, VV> toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction) {
        return s.toMap(keyMapper, valueMapper, mergeFunction);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mergeFunction
     * @param mapFactory
     * @return
     * @see Collectors#toMap(Function, Function, BinaryOperator, Supplier)
     */
    @ParallelSupported
    public <KK, VV, M extends Map<KK, VV>> M toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final BinaryOperator<VV> mergeFunction, final Supplier<? extends M> mapFactory) {
        return s.toMap(keyMapper, valueMapper, mergeFunction, mapFactory);
    }

    /**
     * 
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <A, D> Map<K, D> toMap(final Collector<? super Map.Entry<K, V>, A, D> downstream) {
        final Function<Map.Entry<K, V>, K> keyMapper = Fn.key();
        return s.toMap(keyMapper, downstream);
    }

    /**
     * 
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <A, D, M extends Map<K, D>> M toMap(final Collector<? super Map.Entry<K, V>, A, D> downstream, final Supplier<? extends M> mapFactory) {
        return s.toMap(Fn.<K, V> key(), downstream, mapFactory);
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Collector, Supplier)
     */
    @ParallelSupported
    public <KK, A, D> Map<KK, D> toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, A, D> downstream) {
        return s.toMap(keyMapper, downstream);
    }

    /**
     * 
     * @param keyMapper
     * @param downstream
     * @return
     * @see Collectors#groupingBy(Function, Collector)
     */
    @ParallelSupported
    public <KK, A, D, M extends Map<KK, D>> M toMap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Collector<? super Map.Entry<K, V>, A, D> downstream, final Supplier<? extends M> mapFactory) {
        return s.toMap(keyMapper, downstream, mapFactory);
    }

    @SequentialOnly
    public <R> R toMapAndThen(Function<? super Map<K, V>, R> func) {
        return func.apply(toMap());
    }

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#groupingBy(Function)
     */
    @SequentialOnly
    public Map<K, List<V>> groupTo() {
        if (isParallel()) {
            return s.sequential().groupTo(Fn.<K, V> key(), Fn.<K, V> value());
        } else {
            return s.groupTo(Fn.<K, V> key(), Fn.<K, V> value());
        }
    }

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#groupingBy(Function, Supplier)
     */
    @SequentialOnly
    public <M extends Map<K, List<V>>> M groupTo(final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return s.sequential().groupTo(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        } else {
            return s.groupTo(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        }
    }

    @ParallelSupported
    public <KK, VV> Map<KK, List<VV>> groupTo(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return s.groupTo(keyMapper, valueMapper);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public <KK, VV, M extends Map<KK, List<VV>>> M groupTo(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<? extends M> mapFactory) {
        return s.groupTo(keyMapper, valueMapper, mapFactory);
    }

    @SequentialOnly
    public <R> R groupToAndThen(Function<? super Map<K, List<V>>, R> func) {
        return func.apply(groupTo());
    }

    /**
     * 
     * @param keyMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @SequentialOnly
    public ListMultimap<K, V> toMultimap() {
        if (isParallel()) {
            return s.sequential().toMultimap(Fn.<K, V> key(), Fn.<K, V> value());
        } else {
            return s.toMultimap(Fn.<K, V> key(), Fn.<K, V> value());
        }
    }

    /**
     * 
     * @param keyMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @SequentialOnly
    public <C extends Collection<V>, M extends Multimap<K, V, C>> M toMultimap(final Supplier<? extends M> mapFactory) {
        if (isParallel()) {
            return s.sequential().toMultimap(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        } else {
            return s.toMultimap(Fn.<K, V> key(), Fn.<K, V> value(), mapFactory);
        }
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see Collectors#toMultimap(Function, Function)
     */
    @ParallelSupported
    public <KK, VV> ListMultimap<KK, VV> toMultimap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        return s.toMultimap(keyMapper, valueMapper);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @param mapFactory
     * @return
     * @see Collectors#toMultimap(Function, Function, Supplier)
     */
    @ParallelSupported
    public <KK, VV, C extends Collection<VV>, M extends Multimap<KK, VV, C>> M toMultimap(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper, final Supplier<? extends M> mapFactory) {
        return s.toMultimap(keyMapper, valueMapper, mapFactory);
    }

    @ParallelSupported
    public Map.Entry<K, V> reduce(final ImmutableEntry<K, V> identity, final BinaryOperator<Map.Entry<K, V>> accumulator) {
        return s.reduce(identity, accumulator);
    }

    @ParallelSupported
    public Optional<Map.Entry<K, V>> reduce(final BinaryOperator<Map.Entry<K, V>> accumulator) {
        return s.reduce(accumulator);
    }

    @ParallelSupported
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super Map.Entry<K, V>> accumulator, final BiConsumer<R, R> combiner) {
        return s.collect(supplier, accumulator, combiner);
    }

    @ParallelSupported
    public <R> R collect(final Supplier<R> supplier, final BiConsumer<? super R, ? super Map.Entry<K, V>> accumulator) {
        return s.collect(supplier, accumulator);
    }

    @ParallelSupported
    public <R, A> R collect(final Collector<? super Map.Entry<K, V>, A, R> collector) {
        return s.collect(collector);
    }

    @ParallelSupported
    public <R, A> R collect(final java.util.stream.Collector<? super Map.Entry<K, V>, A, R> collector) {
        return s.collect(collector);
    }

    @ParallelSupported
    public <R, A, RR> RR collectAndThen(final Collector<? super Map.Entry<K, V>, A, R> downstream, final Function<? super R, RR> finisher) {
        return s.collectAndThen(downstream, finisher);
    }

    @ParallelSupported
    public <R, A, RR> RR collectAndThen(final java.util.stream.Collector<? super Map.Entry<K, V>, A, R> downstream,
            final java.util.function.Function<? super R, RR> finisher) {
        return s.collectAndThen(downstream, finisher);
    }

    @SequentialOnly
    public String join(CharSequence delimiter) {
        return join(delimiter, "", "");
    }

    @SequentialOnly
    public String join(CharSequence delimiter, CharSequence prefix, CharSequence suffix) {
        return join(delimiter, "=", prefix, suffix);
    }

    @SequentialOnly
    public String join(CharSequence delimiter, CharSequence keyValueDelimiter) {
        return join(delimiter, keyValueDelimiter, "", "");
    }

    @SequentialOnly
    public String join(CharSequence delimiter, CharSequence keyValueDelimiter, CharSequence prefix, CharSequence suffix) {
        s.assertNotClosed();

        try {
            final Joiner joiner = Joiner.with(delimiter, keyValueDelimiter, prefix, suffix).reuseCachedBuffer(true);
            final Iterator<Entry<K, V>> iter = s.iteratorEx();

            while (iter.hasNext()) {
                joiner.appendEntry(iter.next());
            }

            return joiner.toString();
        } finally {
            close();
        }
    }

    @SequentialOnly
    public <KK, VV> EntryStream<KK, VV> chain(Function<? super Stream<Map.Entry<K, V>>, ? extends Stream<Map.Entry<KK, VV>>> transfer) {
        return of(transfer.apply(s));
    }

    @SequentialOnly
    public <R> R __(Function<? super EntryStream<K, V>, R> transfer) {
        return transfer.apply(this);
    }

    public EntryStream<K, V> sequential() {
        return s.isParallel() ? of(s.sequential()) : this;
    }

    public EntryStream<K, V> parallel() {
        return of(s.parallel());
    }

    public EntryStream<K, V> parallel(final int threadNum) {
        return of(s.parallel(threadNum));
    }

    public EntryStream<K, V> parallel(final int maxThreadNum, final Splitor splitor) {
        return of(s.parallel(maxThreadNum, splitor));
    }

    public EntryStream<K, V> parallel(final int maxThreadNum, final Executor executor) {
        return of(s.parallel(maxThreadNum, executor));
    }

    public EntryStream<K, V> parallel(final Executor executor) {
        return of(s.parallel(executor));
    }

    protected int maxThreadNum() {
        return s.maxThreadNum();
    }

    protected Splitor splitor() {
        return s.splitor();
    }

    protected AsyncExecutor asyncExecutor() {
        return s.asyncExecutor();
    }

    public boolean isParallel() {
        return s.isParallel();
    }

    /**
     * @return
     * @see Stream#mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public EntryStream<V, K> inversedToDisposableEntry() {
        checkState(s.isParallel() == false, "inversedER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, DisposableEntry<V, K>> mapper = new Function<Map.Entry<K, V>, DisposableEntry<V, K>>() {
            private final ReusableEntry<V, K> entry = new ReusableEntry<>();

            @Override
            public DisposableEntry<V, K> apply(Entry<K, V> e) {
                entry.set(e.getValue(), e.getKey());

                return entry;
            }
        };

        return map(mapper);
    }

    /**
     * To reduce the memory footprint, Only one instance of <code>DisposableEntry</code> is created, 
     * and the same entry instance is returned and set with different keys/values during iteration of the returned stream.
     * The elements only can be retrieved one by one, can't be modified or saved.
     * The returned Stream doesn't support the operations which require two or more elements at the same time: (e.g. sort/distinct/pairMap/slidingMap/sliding/split/toList/toSet/...).
     * , and can't be parallel stream.
     * Operations: filter/map/toMap/groupBy/groupTo/... are supported. 
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * 
     * @see DisposableEntry
     * @see NoCachingNoUpdating
     * 
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <KK, VV> EntryStream<KK, VV> mapToDisposableEntry(final Function<? super Map.Entry<K, V>, ? extends KK> keyMapper,
            final Function<? super Map.Entry<K, V>, ? extends VV> valueMapper) {
        checkState(s.isParallel() == false, "mapER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Map.Entry<KK, VV>> mapper = new Function<Map.Entry<K, V>, Map.Entry<KK, VV>>() {
            private final ReusableEntry<KK, VV> entry = new ReusableEntry<>();

            @Override
            public Entry<KK, VV> apply(Entry<K, V> t) {
                entry.set(keyMapper.apply(t), valueMapper.apply(t));

                return entry;
            }
        };

        return map(mapper);
    }

    /**
     * 
     * @param keyMapper
     * @param valueMapper
     * @return
     * @see #mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <KK, VV> EntryStream<KK, VV> mapToDisposableEntry(final BiFunction<? super K, ? super V, ? extends KK> keyMapper,
            final BiFunction<? super K, ? super V, ? extends VV> valueMapper) {
        checkState(s.isParallel() == false, "mapER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Map.Entry<KK, VV>> mapper = new Function<Map.Entry<K, V>, Map.Entry<KK, VV>>() {
            private final ReusableEntry<KK, VV> entry = new ReusableEntry<>();

            @Override
            public Entry<KK, VV> apply(Entry<K, V> t) {
                entry.set(keyMapper.apply(t.getKey(), t.getValue()), valueMapper.apply(t.getKey(), t.getValue()));

                return entry;
            }
        };

        return map(mapper);

    }

    /**
     * 
     * @param keyMapper
     * @return
     * @see #mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <KK> EntryStream<KK, V> flatMapKeyToDisposableEntry(final Function<? super K, ? extends Stream<? extends KK>> keyMapper) {
        checkState(s.isParallel() == false, "flatMapKeyER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return keyMapper.apply(e.getKey()).map(new Function<KK, Map.Entry<KK, V>>() {
                    private final ReusableEntry<KK, V> entry = new ReusableEntry<>();

                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        entry.set(kk, e.getValue());
                        return entry;
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    /**
     * 
     * @param keyMapper
     * @return
     * @see #mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <KK> EntryStream<KK, V> flattMapKeyToDisposableEntry(final Function<? super K, ? extends Collection<? extends KK>> keyMapper) {
        checkState(s.isParallel() == false, "flatMapKeyER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<KK, V>>>() {
            @Override
            public Stream<Entry<KK, V>> apply(final Map.Entry<K, V> e) {
                return Stream.of(keyMapper.apply(e.getKey())).map(new Function<KK, Map.Entry<KK, V>>() {
                    private final ReusableEntry<KK, V> entry = new ReusableEntry<>();

                    @Override
                    public Map.Entry<KK, V> apply(KK kk) {
                        entry.set(kk, e.getValue());
                        return entry;
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    /**
     * 
     * @param valueMapper
     * @return
     * @see #mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <VV> EntryStream<K, VV> flatMapValueToDisposableEntry(final Function<? super V, ? extends Stream<? extends VV>> valueMapper) {
        checkState(s.isParallel() == false, "flatMapValueER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return valueMapper.apply(e.getValue()).map(new Function<VV, Map.Entry<K, VV>>() {
                    private final ReusableEntry<K, VV> entry = new ReusableEntry<>();

                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        entry.set(e.getKey(), vv);
                        return entry;
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    /**
     * 
     * @param valueMapper
     * @return
     * @see #mapToDisposableEntry(Function, Function)
     * @deprecated
     */
    @Deprecated
    @Beta
    @SequentialOnly
    public <VV> EntryStream<K, VV> flattMapValueToDisposableEntry(final Function<? super V, ? extends Collection<? extends VV>> valueMapper) {
        checkState(s.isParallel() == false, "flatMapValueER can't be applied to parallel stream");

        final Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>> mapper2 = new Function<Map.Entry<K, V>, Stream<Map.Entry<K, VV>>>() {
            @Override
            public Stream<Entry<K, VV>> apply(final Entry<K, V> e) {
                return Stream.of(valueMapper.apply(e.getValue())).map(new Function<VV, Map.Entry<K, VV>>() {
                    private final ReusableEntry<K, VV> entry = new ReusableEntry<>();

                    @Override
                    public Map.Entry<K, VV> apply(VV vv) {
                        entry.set(e.getKey(), vv);
                        return entry;
                    }
                });
            }
        };

        return flattMap(mapper2);
    }

    @SequentialOnly
    public EntryStream<K, V> onClose(Runnable closeHandler) {
        return of(s.onClose(closeHandler));
    }

    @Override
    @SequentialOnly
    public void close() {
        s.close();
    }

    void checkState(boolean b, String errorMessage) {
        if (!b) {
            try {
                N.checkState(b, errorMessage);
            } finally {
                close();
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    static <K, V> Function<Map<K, V>, Stream<Map.Entry<K, V>>> mapFunc() {
        return (Function) mapper_func;
    }

    public static <K, V> EntryStream<K, V> empty() {
        return new EntryStream<>(Stream.<Map.Entry<K, V>> empty());
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5), new SimpleImmutableEntry<>(k6, v6)));
    }

    public static <K, V> EntryStream<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {
        return of(Stream.of(new SimpleImmutableEntry<>(k1, v1), new SimpleImmutableEntry<>(k2, v2), new SimpleImmutableEntry<>(k3, v3),
                new SimpleImmutableEntry<>(k4, v4), new SimpleImmutableEntry<>(k5, v5), new SimpleImmutableEntry<>(k6, v6),
                new SimpleImmutableEntry<>(k7, v7)));
    }

    static <K, V> EntryStream<K, V> of(final Stream<? extends Map.Entry<? extends K, ? extends V>> s) {
        return new EntryStream<K, V>(s);
    }

    public static <K, V> EntryStream<K, V> of(final Map<K, V> map) {
        return new EntryStream<K, V>(map, Stream.of(map));
    }

    public static <K, V> EntryStream<K, V> of(final Iterator<? extends Map.Entry<? extends K, ? extends V>> iterator) {
        return new EntryStream<K, V>(Stream.of(iterator));
    }

    public static <K, V> EntryStream<K, V> of(final Collection<? extends Map.Entry<? extends K, ? extends V>> entries) {
        return new EntryStream<K, V>(Stream.of(entries));
    }

    //    @SafeVarargs
    //    public static <K, V> EntryStream<K, V> of(final Map.Entry<K, V>... entries) {
    //        return new EntryStream<K, V>(Stream.of(entries));
    //    }

    public static <E> EntryStream<E, Integer> of(final Multiset<E> multiset) {
        return multiset == null ? EntryStream.<E, Integer> empty() : multiset.entryStream();
    }

    public static <E> EntryStream<E, Long> of(final LongMultiset<E> multiset) {
        return multiset == null ? EntryStream.<E, Long> empty() : multiset.entryStream();
    }

    public static <K, E, V extends Collection<E>> EntryStream<K, V> of(final Multimap<K, E, V> mulitmap) {
        return mulitmap == null ? EntryStream.<K, V> empty() : mulitmap.entryStream();
    }

    public static <K, T> EntryStream<K, T> of(final T[] a, final Function<? super T, K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(a).mapToEntry(keyMapper, valueMapper);
    }

    public static <K, T> EntryStream<K, T> of(final Collection<? extends T> c, final Function<? super T, K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(c).mapToEntry(keyMapper, valueMapper);
    }

    public static <K, T> EntryStream<K, T> of(final Iterator<? extends T> iter, final Function<? super T, K> keyMapper) {
        final Function<T, T> valueMapper = Fn.identity();

        return Stream.of(iter).mapToEntry(keyMapper, valueMapper);
    }

    @SafeVarargs
    public static <K, V> EntryStream<K, V> concat(final Map<K, V>... maps) {
        if (N.isNullOrEmpty(maps)) {
            return EntryStream.empty();
        }

        final Function<Map<K, V>, Stream<Map.Entry<K, V>>> mapper = mapFunc();

        return Stream.of(maps).flatMapToEntry(mapper);
    }

    public static <K, V> EntryStream<K, V> concat(final Collection<? extends Map<K, V>> maps) {
        if (N.isNullOrEmpty(maps)) {
            return EntryStream.empty();
        }

        final Function<Map<K, V>, Stream<Map.Entry<K, V>>> mapper = mapFunc();

        return Stream.of(maps).flatMapToEntry(mapper);
    }

    public static <K, V> EntryStream<K, V> merge(final Map<K, V> a, final Map<K, V> b,
            final BiFunction<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(a)) {
            return of(b);
        } else if (N.isNullOrEmpty(b)) {
            return of(a);
        } else {
            return Stream.merge(a.entrySet(), b.entrySet(), nextSelector).mapToEntry(Fn.<Map.Entry<K, V>> identity());
        }
    }

    public static <K, V> EntryStream<K, V> merge(final Map<K, V> a, final Map<K, V> b, final Map<K, V> c,
            final BiFunction<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(a)) {
            return merge(b, c, nextSelector);
        } else if (N.isNullOrEmpty(b)) {
            return merge(a, c, nextSelector);
        } else if (N.isNullOrEmpty(c)) {
            return merge(a, b, nextSelector);
        } else {
            return Stream.merge(a.entrySet(), b.entrySet(), c.entrySet(), nextSelector).mapToEntry(Fn.<Map.Entry<K, V>> identity());
        }
    }

    public static <K, V> EntryStream<K, V> merge(final Collection<? extends Map<K, V>> maps,
            final BiFunction<? super Map.Entry<K, V>, ? super Map.Entry<K, V>, Nth> nextSelector) {
        N.checkArgNotNull(nextSelector);

        if (N.isNullOrEmpty(maps)) {
            return EntryStream.empty();
        }

        final List<Set<Map.Entry<K, V>>> entryIteratorList = new ArrayList<>(maps.size());

        for (Map<K, V> map : maps) {
            if (N.notNullOrEmpty(map)) {
                entryIteratorList.add(map.entrySet());
            }
        }

        return Stream.merge(entryIteratorList, nextSelector).mapToEntry(Fn.<Map.Entry<K, V>> identity());
    }

    public static <K, V> EntryStream<K, V> zip(final K[] keys, final V[] values) {
        if (N.isNullOrEmpty(keys) || N.isNullOrEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        return Stream.zip(keys, values, zipFunction).mapToEntry(mapper);
    }

    public static <K, V> EntryStream<K, V> zip(final K[] keys, final V[] values, K valueForNonKey, V valueForNonValue) {
        if (N.isNullOrEmpty(keys) && N.isNullOrEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        return Stream.zip(keys, values, valueForNonKey, valueForNonValue, zipFunction).mapToEntry(mapper);
    }

    public static <K, V> EntryStream<K, V> zip(final Collection<? extends K> keys, final Collection<? extends V> values) {
        if (N.isNullOrEmpty(keys) || N.isNullOrEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        return Stream.zip(keys, values, zipFunction).mapToEntry(mapper);
    }

    public static <K, V> EntryStream<K, V> zip(final Collection<? extends K> keys, final Collection<? extends V> values, K valueForNonKey, V valueForNonValue) {
        if (N.isNullOrEmpty(keys) && N.isNullOrEmpty(values)) {
            return EntryStream.empty();
        }

        final BiFunction<K, V, Map.Entry<K, V>> zipFunction = Fn.entry();
        final Function<Map.Entry<K, V>, Map.Entry<K, V>> mapper = Fn.identity();

        return Stream.zip(keys, values, valueForNonKey, valueForNonValue, zipFunction).mapToEntry(mapper);
    }

    static class ReusableEntry<K, V> extends DisposableEntry<K, V> {
        private K key = null;
        private V value = null;
        private boolean flag = false; //check if it's used/read.

        @Override
        public K getKey() {
            flag = false;
            return key;
        }

        @Override
        public V getValue() {
            flag = false;
            return value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }

        public void set(K key, V value) {
            if (flag) {
                throw new IllegalStateException();
            }

            this.key = key;
            this.value = value;
            this.flag = true;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            if (obj instanceof ReusableEntry) {
                final ReusableEntry<K, V> other = (ReusableEntry<K, V>) obj;

                return N.equals(key, other.key) && N.equals(value, other.value);
            }

            return false;
        }

        @Override
        public int hashCode() {
            return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
        }

        @Override
        public String toString() {
            flag = false;
            return key + "=" + value;
        }
    }
}
