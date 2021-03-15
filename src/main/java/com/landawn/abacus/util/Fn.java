/*
 * Copyright (c) 2017, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.exception.DuplicatedResultException;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.Tuple.Tuple1;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.Tuple.Tuple4;
import com.landawn.abacus.util.function.BiConsumer;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.BinaryOperator;
import com.landawn.abacus.util.function.BooleanSupplier;
import com.landawn.abacus.util.function.ByteBiFunction;
import com.landawn.abacus.util.function.ByteBiPredicate;
import com.landawn.abacus.util.function.ByteBinaryOperator;
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.Callable;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharBinaryOperator;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleBinaryOperator;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
import com.landawn.abacus.util.function.FloatBinaryOperator;
import com.landawn.abacus.util.function.FloatConsumer;
import com.landawn.abacus.util.function.FloatFunction;
import com.landawn.abacus.util.function.FloatPredicate;
import com.landawn.abacus.util.function.Function;
import com.landawn.abacus.util.function.IndexedBiConsumer;
import com.landawn.abacus.util.function.IndexedBiFunction;
import com.landawn.abacus.util.function.IndexedBiPredicate;
import com.landawn.abacus.util.function.IndexedConsumer;
import com.landawn.abacus.util.function.IndexedFunction;
import com.landawn.abacus.util.function.IndexedPredicate;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.function.IntBiPredicate;
import com.landawn.abacus.util.function.IntBinaryOperator;
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongBinaryOperator;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.Runnable;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
import com.landawn.abacus.util.function.ShortBinaryOperator;
import com.landawn.abacus.util.function.ShortConsumer;
import com.landawn.abacus.util.function.ShortFunction;
import com.landawn.abacus.util.function.ShortPredicate;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.function.ToByteFunction;
import com.landawn.abacus.util.function.ToCharFunction;
import com.landawn.abacus.util.function.ToDoubleFunction;
import com.landawn.abacus.util.function.ToFloatFunction;
import com.landawn.abacus.util.function.ToIntFunction;
import com.landawn.abacus.util.function.ToLongFunction;
import com.landawn.abacus.util.function.ToShortFunction;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.function.TriPredicate;
import com.landawn.abacus.util.function.UnaryOperator;
import com.landawn.abacus.util.stream.Collector;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 * Factory utility class for functional interfaces.
 *
 * <br>
 * Note: Don't save and reuse any Function/Predicat/Consumer/... created by calling the methods in this class.
 * The method should be called every time.
 * </br>
 *
 * <pre>
 * <code>
 *
 * Map<String, Integer> map = N.asMap("a", 1, "b", 2, "c", 3);
 * // Instead of
 * Stream.of(map).filter(e -> e.getKey().equals("a") || e.getKey().equals("b")).toMap(e -> e.getKey(), e -> e.getValue());
 * // Using Fn
 * Stream.of(map).filter(Fn.testByKey(k -> k.equals("a") || k.equals("b"))).collect(Collectors.toMap());
 *
 * </code>
 * </pre>
 *
 *
 *
 * @author haiyang li
 *
 */
public final class Fn extends Comparators {

    private static final Object NONE = new Object();

    private static final Timer timer = new Timer();

    @SuppressWarnings("rawtypes")
    public static final IntFunction<Map<String, Object>> FACTORY_OF_MAP = (IntFunction) Factory.MAP_FACTORY;

    @SuppressWarnings("rawtypes")
    public static final IntFunction<LinkedHashMap<String, Object>> FACTORY_OF_LINKED_HASH_MAP = (IntFunction) Factory.LINKED_HASH_MAP_FACTORY;

    @SuppressWarnings("rawtypes")
    public static final Supplier<Map<String, Object>> SUPPLIER_OF_MAP = (Supplier) Suppliers.MAP;

    @SuppressWarnings("rawtypes")
    public static final Supplier<LinkedHashMap<String, Object>> SUPPLIER_OF_LINKED_HASH_MAP = (Supplier) Suppliers.LINKED_HASH_MAP;

    private static final Runnable EMPTY_ACTION = new Runnable() {
        @Override
        public void run() {
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Consumer DO_NOTHING = new Consumer() {
        @Override
        public void accept(Object value) {
            // do nothing.
        }
    };

    private static final Consumer<AutoCloseable> CLOSE = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable closeable) {
            IOUtil.close(closeable);
        }
    };

    private static final Consumer<AutoCloseable> CLOSE_QUIETLY = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable closeable) {
            IOUtil.closeQuietly(closeable);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EQUAL = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "=", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_HYPHEN = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "-", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_UNDERSCORE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "_", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ":", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON_SPACE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ": ", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ",", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA_SPACE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ", ", N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EMPTY = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), N.toString(value)));
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Consumer PRINTLN = new Consumer() {
        @Override
        public void accept(Object value) {
            N.println(value);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function TO_STRING = new Function() {
        @Override
        public Object apply(Object t) {
            return N.toString(t);
        }
    };

    private static final Function<String, String> TO_CAMEL_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toCamelCase(t);
        }
    };

    private static final Function<String, String> TO_LOWER_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toLowerCase(t);
        }
    };

    private static final Function<String, String> TO_LOWER_CASE_WITH_UNDERSCORE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toLowerCaseWithUnderscore(t);
        }
    };

    private static final Function<String, String> TO_UPPER_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toUpperCase(t);
        }
    };

    private static final Function<String, String> TO_UPPER_CASE_WITH_UNDERSCORE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toUpperCaseWithUnderscore(t);
        }
    };

    private static final Function<Throwable, RuntimeException> TO_RUNTIME_EXCEPTION = new Function<Throwable, RuntimeException>() {
        @Override
        public RuntimeException apply(Throwable e) {
            return N.toRuntimeException(e);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final BiFunction COMPARE = new BiFunction<Comparable, Comparable, Integer>() {
        @Override
        public Integer apply(Comparable a, Comparable b) {
            return N.compare(a, b);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function IDENTITY = new Function() {
        @Override
        public Object apply(Object t) {
            return t;
        }
    };

    private static final Function<String, String> TRIM = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? null : t.trim();
        }
    };

    private static final Function<String, String> TRIM_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? "" : t.trim();
        }
    };

    private static final Function<String, String> TRIM_TO_NULL = new Function<String, String>() {
        @Override
        public String apply(String t) {
            if (t == null || (t = t.trim()).length() == 0) {
                return null;
            }

            return t;
        }
    };

    private static final Function<String, String> STRIP = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.strip(t);
        }
    };

    private static final Function<String, String> STRIP_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.stripToEmpty(t);
        }
    };

    private static final Function<String, String> STRIP_TO_NULL = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.stripToNull(t);
        }
    };

    private static final Function<String, String> NULL_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? N.EMPTY_STRING : t;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<List, List> NULL_TO_EMPTY_LIST = new Function<List, List>() {
        @Override
        public List apply(List t) {
            return t == null ? N.emptyList() : t;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<Set, Set> NULL_TO_EMPTY_SET = new Function<Set, Set>() {
        @Override
        public Set apply(Set t) {
            return t == null ? N.emptySet() : t;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<Map, Map> NULL_TO_EMPTY_MAP = new Function<Map, Map>() {
        @Override
        public Map apply(Map t) {
            return t == null ? N.emptyMap() : t;
        }
    };

    private static final Function<CharSequence, Integer> LENGTH = new Function<CharSequence, Integer>() {
        @Override
        public Integer apply(CharSequence t) {
            return t == null ? 0 : t.length();
        }
    };

    private static final Function<Object[], Integer> LEN = new Function<Object[], Integer>() {
        @Override
        public Integer apply(Object[] t) {
            return t == null ? 0 : t.length;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<Collection, Integer> SIZE = new Function<Collection, Integer>() {
        @Override
        public Integer apply(Collection t) {
            return t == null ? 0 : t.size();
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Function<Map, Integer> SIZE_MAP = new Function<Map, Integer>() {
        @Override
        public Integer apply(Map t) {
            return t == null ? 0 : t.size();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Object> KEY = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getKey();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Object> VALUE = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getValue();
        }
    };

    private static final Function<Pair<Object, Object>, Object> LEFT = new Function<Pair<Object, Object>, Object>() {
        @Override
        public Object apply(Pair<Object, Object> t) {
            return t.getLeft();
        }
    };

    private static final Function<Pair<Object, Object>, Object> RIGHT = new Function<Pair<Object, Object>, Object>() {
        @Override
        public Object apply(Pair<Object, Object> t) {
            return t.getRight();
        }
    };

    private static final Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>> INVERSE = new Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Map.Entry<Object, Object> t) {
            return new ImmutableEntry<>(t.getValue(), t.getKey());
        }
    };

    private static final BiFunction<Object, Object, Map.Entry<Object, Object>> ENTRY = new BiFunction<Object, Object, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Object key, Object value) {
            return new ImmutableEntry<>(key, value);
        }
    };

    private static final BiFunction<Object, Object, Pair<Object, Object>> PAIR = new BiFunction<Object, Object, Pair<Object, Object>>() {
        @Override
        public Pair<Object, Object> apply(Object key, Object value) {
            return Pair.of(key, value);
        }
    };

    private static final TriFunction<Object, Object, Object, Triple<Object, Object, Object>> TRIPLE = new TriFunction<Object, Object, Object, Triple<Object, Object, Object>>() {
        @Override
        public Triple<Object, Object, Object> apply(Object a, Object b, Object c) {
            return Triple.of(a, b, c);
        }
    };

    private static final Function<Object, Tuple1<Object>> TUPLE_1 = new Function<Object, Tuple1<Object>>() {
        @Override
        public Tuple1<Object> apply(Object t) {
            return Tuple.of(t);
        }
    };

    private static final BiFunction<Object, Object, Tuple2<Object, Object>> TUPLE_2 = new BiFunction<Object, Object, Tuple2<Object, Object>>() {
        @Override
        public Tuple2<Object, Object> apply(Object t, Object u) {
            return Tuple.of(t, u);
        }
    };

    private static final TriFunction<Object, Object, Object, Tuple3<Object, Object, Object>> TUPLE_3 = new TriFunction<Object, Object, Object, Tuple3<Object, Object, Object>>() {
        @Override
        public Tuple3<Object, Object, Object> apply(Object a, Object b, Object c) {
            return Tuple.of(a, b, c);
        }
    };

    private static final QuadFunction<Object, Object, Object, Object, Tuple4<Object, Object, Object, Object>> TUPLE_4 = new QuadFunction<Object, Object, Object, Object, Tuple4<Object, Object, Object, Object>>() {
        @Override
        public Tuple4<Object, Object, Object, Object> apply(Object a, Object b, Object c, Object d) {
            return Tuple.of(a, b, c, d);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return true;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_FALSE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return false;
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate IS_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value == null;
        }
    };

    private static final Predicate<CharSequence> IS_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value == null || value.length() == 0;
        }
    };

    private static final Predicate<CharSequence> IS_NULL_OR_EMPTY_OR_BLANK = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return N.isNullOrEmptyOrBlank(value);
        }
    };

    @SuppressWarnings("rawtypes")
    private static final Predicate NOT_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value != null;
        }
    };

    private static final Predicate<CharSequence> NOT_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value != null && value.length() > 0;
        }
    };

    private static final Predicate<CharSequence> NOT_NULL_OR_EMPTY_OR_BLANK = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return N.notNullOrEmptyOrBlank(value);
        }
    };

    private static final Predicate<File> IS_FILE = new Predicate<File>() {
        @Override
        public boolean test(File file) {
            return file != null && file.isFile();
        }
    };

    private static final Predicate<File> IS_DIRECTORY = new Predicate<File>() {
        @Override
        public boolean test(File file) {
            return file != null && file.isDirectory();
        }
    };

    protected Fn() {
        super();
        // for extension.
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     */
    public static <T> T get(final Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Returns a {@code Supplier} which returns a single instance created by calling the specified {@code supplier.get()}.
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @Beta
    @Stateful
    public static <T> Supplier<T> memoize(final Supplier<T> supplier) {
        return LazyInitializer.of(supplier);
    }

    /**
     * 
     * @param <T>
     * @param <R>
     * @param func
     * @return
     */
    @Beta
    @Stateful
    public static <T, R> Function<T, R> memoize(final Function<? super T, ? extends R> func) {
        return new Function<T, R>() {
            private volatile R resultForNull = (R) NONE;
            private volatile Map<T, R> resultMap = null;

            @Override
            public R apply(T t) {
                R result = null;

                if (t == null) {
                    result = resultForNull;

                    if (result == NONE) {
                        synchronized (this) {
                            if (resultForNull == NONE) {
                                resultForNull = func.apply(t);
                            }

                            result = resultForNull;
                        }
                    }
                } else {
                    synchronized (this) {
                        if (resultMap == null) {
                            resultMap = new HashMap<>();
                        }

                        result = resultMap.get(t);

                        if (result == null && resultMap.containsKey(t) == false) {
                            result = func.apply(t);
                            resultMap.put(t, result);
                        }
                    }
                }

                return result;
            }
        };
    }

    /**
     * Only for temporary use in sequential stream/single thread, not for parallel stream/multiple threads.
     * The returned Collection will clean up before it's returned every time when {@code get} is called.
     * Don't save the returned Collection object or use it to save objects.
     *
     * @param <T>
     * @param <C>
     * @param supplier
     * @return
     * @see {@code Stream.split/sliding};
     * @deprecated
     */
    @Stateful
    @Deprecated
    @Beta
    @SequentialOnly
    public static <T, C extends Collection<T>> Supplier<? extends C> reuse(final Supplier<? extends C> supplier) {
        return new Supplier<C>() {
            private C c;

            @Override
            public C get() {
                if (c == null) {
                    c = supplier.get();
                } else if (c.size() > 0) {
                    c.clear();
                }

                return c;
            }
        };
    }

    /**
     * Only for temporary use in sequential stream/single thread, not for parallel stream/multiple threads.
     * The returned Collection will clean up before it's returned every time when {@code get} is called.
     * Don't save the returned Collection object or use it to save objects.
     *
     * @param <T>
     * @param <C>
     * @param supplier
     * @return
     * @see {@code Stream.split/sliding};
     * @deprecated
     */
    @Stateful
    @Deprecated
    @Beta
    @SequentialOnly
    public static <T, C extends Collection<T>> IntFunction<? extends C> reuse(final IntFunction<? extends C> supplier) {
        return new IntFunction<C>() {
            private C c;

            @Override
            public C apply(int size) {
                if (c == null) {
                    c = supplier.apply(size);
                } else if (c.size() > 0) {
                    c.clear();
                }

                return c;
            }
        };
    }

    /**
     *
     * @param closeable
     * @return
     */
    public static Runnable close(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.close(closeable);
            }
        };
    }

    /**
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static Runnable closeAll(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAll(a);
            }
        };
    }

    /**
     *
     * @param c
     * @return
     */
    public static Runnable closeAll(final Collection<? extends AutoCloseable> c) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAll(c);
            }
        };
    }

    /**
     *
     * @param closeable
     * @return
     */
    public static Runnable closeQuietly(final AutoCloseable closeable) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeQuietly(closeable);
            }
        };
    }

    /**
     * Close all quietly.
     *
     * @param a
     * @return
     */
    @SafeVarargs
    public static Runnable closeAllQuietly(final AutoCloseable... a) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAllQuietly(a);
            }
        };
    }

    /**
     * Close all quietly.
     *
     * @param c
     * @return
     */
    public static Runnable closeAllQuietly(final Collection<? extends AutoCloseable> c) {
        return new Runnable() {
            private volatile boolean isClosed = false;

            @Override
            public void run() {
                if (isClosed) {
                    return;
                }

                isClosed = true;
                IOUtil.closeAllQuietly(c);
            }
        };
    }

    public static Runnable emptyAction() {
        return EMPTY_ACTION;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Consumer<T> doNothing() {
        return DO_NOTHING;
    }

    /**
     *
     * @param <T>
     * @param errorMessage
     * @return
     */
    public static <T> Consumer<T> throwRuntimeException(final String errorMessage) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                throw new RuntimeException(errorMessage);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param excpetionSupplier
     * @return
     */
    public static <T> Consumer<T> throwException(final Supplier<? extends RuntimeException> excpetionSupplier) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                throw excpetionSupplier.get();
            }
        };
    }

    public static Function<Throwable, RuntimeException> toRuntimeException() {
        return TO_RUNTIME_EXCEPTION;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return (Consumer<T>) CLOSE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends AutoCloseable> Consumer<T> closeQuietly() {
        return (Consumer<T>) CLOSE_QUIETLY;
    }

    /**
     *
     * @param <T>
     * @param millis
     * @return
     */
    public static <T> Consumer<T> sleep(final long millis) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                N.sleep(millis);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param millis
     * @return
     */
    public static <T> Consumer<T> sleepUninterruptibly(final long millis) {
        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                N.sleepUninterruptibly(millis);
            }
        };
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Consumer<T> println() {
        return PRINTLN;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param separator
     * @return
     */
    public static <T, U> BiConsumer<T, U> println(final String separator) {
        N.checkArgNotNull(separator);

        switch (separator) {
            case "=":
                return PRINTLN_EQUAL;

            case ":":
                return PRINTLN_COLON;

            case ": ":
                return PRINTLN_COLON_SPACE;

            case "-":
                return PRINTLN_HYPHEN;

            case "_":
                return PRINTLN_UNDERSCORE;

            case ",":
                return PRINTLN_COMMA;

            case ", ":
                return PRINTLN_COMMA_SPACE;

            case "":
                return PRINTLN_EMPTY;

            default:
                return new BiConsumer<T, U>() {
                    @Override
                    public void accept(T t, U u) {
                        N.println(t + separator + u);
                    }
                };
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, String> toStr() {
        return TO_STRING;
    }

    /**
     * To camel case.
     *
     * @return
     */
    public static Function<String, String> toCamelCase() {
        return TO_CAMEL_CASE;
    }

    /**
     * To lower case.
     *
     * @return
     */
    public static Function<String, String> toLowerCase() {
        return TO_LOWER_CASE;
    }

    /**
     * To lower case with underscore.
     *
     * @return
     */
    public static Function<String, String> toLowerCaseWithUnderscore() {
        return TO_LOWER_CASE_WITH_UNDERSCORE;
    }

    /**
     * To upper case.
     *
     * @return
     */
    public static Function<String, String> toUpperCase() {
        return TO_UPPER_CASE;
    }

    /**
     * To upper case with underscore.
     *
     * @return
     */
    public static Function<String, String> toUpperCaseWithUnderscore() {
        return TO_UPPER_CASE_WITH_UNDERSCORE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Function<T, T> identity() {
        return IDENTITY;
    }

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @param keyMapper
     * @return
     */
    public static <K, T> Function<T, Keyed<K, T>> keyed(final Function<? super T, K> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Function<T, Keyed<K, T>>() {
            @Override
            public Keyed<K, T> apply(T t) {
                return Keyed.of(keyMapper.apply(t), t);
            }
        };
    }

    private static final Function<Keyed<?, Object>, Object> VAL = new Function<Keyed<?, Object>, Object>() {
        @Override
        public Object apply(Keyed<?, Object> t) {
            return t.val();
        }
    };

    private static final Function<Map.Entry<Keyed<Object, Object>, Object>, Object> KK = new Function<Map.Entry<Keyed<Object, Object>, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Keyed<Object, Object>, Object> t) {
            return t.getKey().val();
        }
    };

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Keyed<K, T>, T> val() {
        return (Function) VAL;
    }

    /**
     *
     * @param <T>
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, K, V> Function<Map.Entry<Keyed<K, T>, V>, T> kk() {
        return (Function) KK;
    }

    private static final Function<Object, Wrapper<Object>> WRAP = new Function<Object, Wrapper<Object>>() {
        @Override
        public Wrapper<Object> apply(Object t) {
            return Wrapper.of(t);
        }
    };

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Wrapper<T>> wrap() {
        return (Function) WRAP;
    }

    /**
     *
     * @param <T>
     * @param hashFunction
     * @param equalsFunction
     * @return
     */
    public static <T> Function<T, Wrapper<T>> wrap(final ToIntFunction<? super T> hashFunction, final BiPredicate<? super T, ? super T> equalsFunction) {
        N.checkArgNotNull(hashFunction);
        N.checkArgNotNull(equalsFunction);

        return new Function<T, Wrapper<T>>() {
            @Override
            public Wrapper<T> apply(T t) {
                return Wrapper.of(t, hashFunction, equalsFunction);
            }
        };
    }

    private static final Function<Wrapper<Object>, Object> UNWRAP = new Function<Wrapper<Object>, Object>() {
        @Override
        public Object apply(Wrapper<Object> t) {
            return t.value();
        }
    };

    /**
     *
     * @param <K> the key type
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Wrapper<T>, T> unwrap() {
        return (Function) UNWRAP;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, K> key() {
        return (Function) KEY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, V> value() {
        return (Function) VALUE;
    }

    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, L> left() {
        return (Function) LEFT;
    }

    @SuppressWarnings("rawtypes")
    public static <L, R> Function<Pair<L, R>, R> right() {
        return (Function) RIGHT;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, Entry<V, K>> inverse() {
        return (Function) INVERSE;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> BiFunction<K, V, Map.Entry<K, V>> entry() {
        return (BiFunction) ENTRY;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @return
     * @deprecated replaced by {@code Fn#entryWithKey(Object)}
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final K key) {
        return entryWithKey(key);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @return
     * @deprecated replaced by {@code Fn#entryByKeyMapper(Function)}
     */
    @Deprecated
    public static <K, V> Function<V, Map.Entry<K, V>> entry(final Function<? super V, K> keyMapper) {
        return entryByKeyMapper(keyMapper);
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param key
     * @return
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryWithKey(final K key) {
        return new Function<V, Map.Entry<K, V>>() {
            @Override
            public Entry<K, V> apply(V v) {
                return new ImmutableEntry<>(key, v);
            }
        };
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param keyMapper
     * @return
     */
    public static <K, V> Function<V, Map.Entry<K, V>> entryByKeyMapper(final Function<? super V, K> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Function<V, Map.Entry<K, V>>() {
            @Override
            public Entry<K, V> apply(V v) {
                return new ImmutableEntry<>(keyMapper.apply(v), v);
            }
        };
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param value
     * @return
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryWithValue(final V value) {
        return new Function<K, Map.Entry<K, V>>() {
            @Override
            public Entry<K, V> apply(K k) {
                return new ImmutableEntry<>(k, value);
            }
        };
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param valueMapper
     * @return
     */
    public static <K, V> Function<K, Map.Entry<K, V>> entryByValueMapper(final Function<? super K, V> valueMapper) {
        N.checkArgNotNull(valueMapper);

        return new Function<K, Map.Entry<K, V>>() {
            @Override
            public Entry<K, V> apply(K k) {
                return new ImmutableEntry<>(k, valueMapper.apply(k));
            }
        };
    }

    /**
     *
     * @param <L>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> BiFunction<L, R, Pair<L, R>> pair() {
        return (BiFunction) PAIR;
    }

    /**
     *
     * @param <L>
     * @param <M>
     * @param <R>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <L, M, R> TriFunction<L, M, R, Triple<L, M, R>> triple() {
        return (TriFunction) TRIPLE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Tuple1<T>> tuple1() {
        return (Function) TUPLE_1;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T, U> BiFunction<T, U, Tuple2<T, U>> tuple2() {
        return (BiFunction) TUPLE_2;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <A, B, C> TriFunction<A, B, C, Tuple3<A, B, C>> tuple3() {
        return (TriFunction) TUPLE_3;
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @return
     */
    @SuppressWarnings({ "rawtypes" })
    public static <A, B, C, D> QuadFunction<A, B, C, D, Tuple4<A, B, C, D>> tuple4() {
        return (QuadFunction) TUPLE_4;
    }

    public static Function<String, String> trim() {
        return TRIM;
    }

    public static Function<String, String> trimToEmpty() {
        return TRIM_TO_EMPTY;
    }

    public static Function<String, String> trimToNull() {
        return TRIM_TO_NULL;
    }

    public static Function<String, String> strip() {
        return STRIP;
    }

    public static Function<String, String> stripToEmpty() {
        return STRIP_TO_EMPTY;
    }

    public static Function<String, String> stripToNull() {
        return STRIP_TO_NULL;
    }

    public static Function<String, String> nullToEmpty() {
        return NULL_TO_EMPTY;
    }

    /**
     *
     * @param <T>
     * @return
     * @deprecated replaced by {@code nullToEmptyList}
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static <T> Function<List<T>, List<T>> nullToEmptyL() {
        return (Function) NULL_TO_EMPTY_LIST;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<List<T>, List<T>> nullToEmptyList() {
        return (Function) NULL_TO_EMPTY_LIST;
    }

    /**
     *
     * @param <T>
     * @return
     * @deprecated replaced by {@code nullToEmptySet}
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static <T> Function<Set<T>, Set<T>> nullToEmptyS() {
        return (Function) NULL_TO_EMPTY_SET;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Set<T>, Set<T>> nullToEmptySet() {
        return (Function) NULL_TO_EMPTY_SET;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     * @deprecated replaced by {@code nullToEmptyMap}
     */
    @Deprecated
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Map<K, V>, Map<K, V>> nullToEmptyM() {
        return (Function) NULL_TO_EMPTY_MAP;
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Map<K, V>, Map<K, V>> nullToEmptyMap() {
        return (Function) NULL_TO_EMPTY_MAP;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Function<T, Integer> length() {
        return (Function<T, Integer>) LENGTH;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T[], Integer> len() {
        return (Function) LEN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Function<T, Integer> size() {
        return (Function<T, Integer>) SIZE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Function<T, Integer> sizeM() {
        return (Function<T, Integer>) SIZE_MAP;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param clazz
     * @return
     */
    public static <T, U> Function<T, U> cast(final Class<U> clazz) {
        N.checkArgNotNull(clazz);

        return new Function<T, U>() {
            @Override
            public U apply(T t) {
                return (U) t;
            }
        };
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Checks if is null.
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> isNull() {
        return IS_NULL;
    }

    /**
     * Checks if is null.
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isNull(final Function<T, ?> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return valueExtractor.apply(t) == null;
            }
        };
    }

    /**
     * Checks if is null or empty.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> isNullOrEmpty() {
        return (Predicate<T>) IS_NULL_OR_EMPTY;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isNullOrEmpty(final Function<T, ? extends CharSequence> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return N.isNullOrEmpty(valueExtractor.apply(t));
            }
        };
    }

    /**
     * Checks if is null or empty or blank.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> isNullOrEmptyOrBlank() {
        return (Predicate<T>) IS_NULL_OR_EMPTY_OR_BLANK;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> isNullOrEmptyOrBlank(final Function<T, ? extends CharSequence> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return N.isNullOrEmptyOrBlank(valueExtractor.apply(t));
            }
        };
    }

    private static final Predicate<Object[]> IS_NULL_OR_EMPTY_A = new Predicate<Object[]>() {
        @Override
        public boolean test(Object[] value) {
            return value == null || value.length == 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> isNullOrEmptyA() {
        return (Predicate) IS_NULL_OR_EMPTY_A;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> IS_NULL_OR_EMPTY_C = new Predicate<Collection>() {
        @Override
        public boolean test(Collection value) {
            return value == null || value.size() == 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> isNullOrEmptyC() {
        return (Predicate<T>) IS_NULL_OR_EMPTY_C;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> IS_NULL_OR_EMPTY_M = new Predicate<Map>() {
        @Override
        public boolean test(Map value) {
            return value == null || value.size() == 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> isNullOrEmptyM() {
        return (Predicate<T>) IS_NULL_OR_EMPTY_M;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> Predicate<T> notNull() {
        return NOT_NULL;
    }

    /**
     * Not null
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notNull(final Function<T, ?> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return valueExtractor.apply(t) != null;
            }
        };
    }

    /**
     * Not null or empty.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> notNullOrEmpty() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notNullOrEmpty(final Function<T, ? extends CharSequence> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return N.notNullOrEmpty(valueExtractor.apply(t));
            }
        };
    }

    /**
     * Not null or empty or blank.
     *
     * @param <T>
     * @return
     */
    public static <T extends CharSequence> Predicate<T> notNullOrEmptyOrBlank() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY_OR_BLANK;
    }

    /**
     *
     * @param <T>
     * @param valueExtractor
     * @return
     */
    public static <T> Predicate<T> notNullOrEmptyOrBlank(final Function<T, ? extends CharSequence> valueExtractor) {
        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return N.notNullOrEmptyOrBlank(valueExtractor.apply(t));
            }
        };
    }

    private static final Predicate<Object[]> NOT_NULL_OR_EMPTY_A = new Predicate<Object[]>() {
        @Override
        public boolean test(Object[] value) {
            return value != null && value.length > 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T> Predicate<T[]> notNullOrEmptyA() {
        return (Predicate) NOT_NULL_OR_EMPTY_A;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Collection> NOT_NULL_OR_EMPTY_C = new Predicate<Collection>() {
        @Override
        public boolean test(Collection value) {
            return value != null && value.size() > 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Predicate<T> notNullOrEmptyC() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY_C;
    }

    @SuppressWarnings("rawtypes")
    private static final Predicate<Map> NOT_NULL_OR_EMPTY_M = new Predicate<Map>() {
        @Override
        public boolean test(Map value) {
            return value != null && value.size() > 0;
        }
    };

    @Beta
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Predicate<T> notNullOrEmptyM() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY_M;
    }

    /**
     * Checks if is file.
     *
     * @return
     */
    public static Predicate<File> isFile() {
        return IS_FILE;
    }

    /**
     * Checks if is directory.
     *
     * @return
     */
    public static Predicate<File> isDirectory() {
        return IS_DIRECTORY;
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T> Predicate<T> equal(final Object target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.equals(value, target);
            }
        };
    }

    /**
     * 
     * @param <T>
     * @param targetValue1
     * @param targetValue2
     * @return
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.equals(value, targetValue1) || N.equals(value, targetValue2);
            }
        };
    }

    /**
     * 
     * @param <T>
     * @param targetValue1
     * @param targetValue2
     * @param targetValue3
     * @return
     */
    public static <T> Predicate<T> eqOr(final Object targetValue1, final Object targetValue2, final Object targetValue3) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.equals(value, targetValue1) || N.equals(value, targetValue2) || N.equals(value, targetValue3);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T> Predicate<T> notEqual(final Object target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return !N.equals(value, target);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) > 0;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> greaterEqual(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) >= 0;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) < 0;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> lessEqual(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) <= 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue < e < maxValue}.
     * 
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLt(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue <= e < maxValue}.
     * 
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLt(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) >= 0 && N.compare(value, maxValue) < 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue <= e <= maxValue}.
     * 
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> geAndLe(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) >= 0 && N.compare(value, maxValue) <= 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue < e <= maxValue}.
     * 
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     */
    public static <T extends Comparable<? super T>> Predicate<T> gtAndLe(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) > 0 && N.compare(value, maxValue) <= 0;
            }
        };
    }

    /**
     * Checks if the value/element: {@code minValue < e < maxValue}.
     *
     * @param <T>
     * @param minValue
     * @param maxValue
     * @return
     * @deprecated replaced by {@code gtAndLt}.
     */
    @Deprecated
    public static <T extends Comparable<? super T>> Predicate<T> between(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Predicate<T> in(final Collection<?> c) {
        N.checkArgNotNull(c);

        final boolean isNotEmpty = N.notNullOrEmpty(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return isNotEmpty && c.contains(value);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     */
    public static <T> Predicate<T> notIn(final Collection<?> c) {
        N.checkArgNotNull(c);

        final boolean isEmpty = N.isNullOrEmpty(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return isEmpty || !c.contains(value);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param clazz
     * @return
     */
    public static <T> Predicate<T> instanceOf(final Class<?> clazz) {
        N.checkArgNotNull(clazz);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return value != null && clazz.isInstance(value);
            }
        };
    }

    /**
     *
     * @param clazz
     * @return
     */
    @SuppressWarnings("rawtypes")
    public static Predicate<Class> subtypeOf(final Class<?> clazz) {
        N.checkArgNotNull(clazz);

        return new Predicate<Class>() {
            @Override
            public boolean test(Class value) {
                return clazz.isAssignableFrom(value);
            }
        };
    }

    /**
     *
     * @param prefix
     * @return
     */
    public static Predicate<String> startsWith(final String prefix) {
        N.checkArgNotNull(prefix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value != null && value.startsWith(prefix);
            }
        };
    }

    /**
     *
     * @param suffix
     * @return
     */
    public static Predicate<String> endsWith(final String suffix) {
        N.checkArgNotNull(suffix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value != null && value.endsWith(suffix);
            }
        };
    }

    /**
     *
     * @param str
     * @return
     */
    public static Predicate<String> contains(final String str) {
        N.checkArgNotNull(str);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value != null && value.contains(str);
            }
        };
    }

    /**
     * Not starts with.
     *
     * @param prefix
     * @return
     */
    public static Predicate<String> notStartsWith(final String prefix) {
        N.checkArgNotNull(prefix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value == null || !value.startsWith(prefix);
            }
        };
    }

    /**
     * Not ends with.
     *
     * @param suffix
     * @return
     */
    public static Predicate<String> notEndsWith(final String suffix) {
        N.checkArgNotNull(suffix);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value == null || !value.endsWith(suffix);
            }
        };
    }

    /**
     *
     * @param str
     * @return
     */
    public static Predicate<String> notContains(final String str) {
        N.checkArgNotNull(str);

        return new Predicate<String>() {
            @Override
            public boolean test(String value) {
                return value == null || !value.contains(str);
            }
        };
    }

    /**
     *
     * @param pattern
     * @return
     */
    public static Predicate<CharSequence> matches(final Pattern pattern) {
        N.checkArgNotNull(pattern);

        return new Predicate<CharSequence>() {
            @Override
            public boolean test(CharSequence value) {
                return pattern.matcher(value).find();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    public static <T, U> BiPredicate<T, U> equal() {
        return BiPredicates.EQUAL;
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @return
     */
    public static <T, U> BiPredicate<T, U> notEqual() {
        return BiPredicates.NOT_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterThan() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_THAN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> greaterEqual() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_EQUAL;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessThan() {
        return (BiPredicate<T, T>) BiPredicates.LESS_THAN;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiPredicate<T, T> lessEqual() {
        return (BiPredicate<T, T>) BiPredicates.LESS_EQUAL;
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     */
    public static <T> Predicate<T> not(final Predicate<T> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return !predicate.test(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param biPredicate
     * @return
     */
    public static <T, U> BiPredicate<T, U> not(final BiPredicate<T, U> biPredicate) {
        N.checkArgNotNull(biPredicate);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return !biPredicate.test(t, u);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param triPredicate
     * @return
     */
    public static <A, B, C> TriPredicate<A, B, C> not(final TriPredicate<A, B, C> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new TriPredicate<A, B, C>() {
            @Override
            public boolean test(A a, B b, C c) {
                return !triPredicate.test(a, b, c);
            }
        };
    }

    /**
     *
     * @param first
     * @param second
     * @return
     */
    public static BooleanSupplier and(final BooleanSupplier first, final BooleanSupplier second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() && second.getAsBoolean();
            }
        };
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static BooleanSupplier and(final BooleanSupplier first, final BooleanSupplier second, final BooleanSupplier third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() && second.getAsBoolean() && third.getAsBoolean();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @return
     */
    public static <T> Predicate<T> and(final Predicate<? super T> first, final Predicate<? super T> second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) && second.test(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static <T> Predicate<T> and(final Predicate<? super T> first, final Predicate<? super T> second, final Predicate<? super T> third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) && second.test(t) && third.test(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is null or empty.
     */
    public static <T> Predicate<T> and(final Collection<Predicate<? super T>> c) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(c, "c");

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                for (Predicate<? super T> p : c) {
                    if (p.test(t) == false) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @return
     */
    public static <T, U> BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> first, final BiPredicate<? super T, ? super U> second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) && second.test(t, u);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static <T, U> BiPredicate<T, U> and(final BiPredicate<? super T, ? super U> first, final BiPredicate<? super T, ? super U> second,
            final BiPredicate<? super T, ? super U> third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) && second.test(t, u) && third.test(t, u);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is null or empty.
     */
    public static <T, U> BiPredicate<T, U> and(final List<BiPredicate<? super T, ? super U>> c) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(c, "c");

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                for (BiPredicate<? super T, ? super U> p : c) {
                    if (p.test(t, u) == false) {
                        return false;
                    }
                }

                return true;
            }
        };
    }

    /**
     *
     * @param first
     * @param second
     * @return
     */
    public static BooleanSupplier or(final BooleanSupplier first, final BooleanSupplier second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() || second.getAsBoolean();
            }
        };
    }

    /**
     *
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static BooleanSupplier or(final BooleanSupplier first, final BooleanSupplier second, final BooleanSupplier third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new BooleanSupplier() {
            @Override
            public boolean getAsBoolean() {
                return first.getAsBoolean() || second.getAsBoolean() || third.getAsBoolean();
            }
        };
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @return
     */
    public static <T> Predicate<T> or(final Predicate<? super T> first, final Predicate<? super T> second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) || second.test(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static <T> Predicate<T> or(final Predicate<? super T> first, final Predicate<? super T> second, final Predicate<? super T> third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return first.test(t) || second.test(t) || third.test(t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is null or empty.
     */
    public static <T> Predicate<T> or(final Collection<Predicate<? super T>> c) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(c, "c");

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                for (Predicate<? super T> p : c) {
                    if (p.test(t)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @return
     */
    public static <T, U> BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> first, final BiPredicate<? super T, ? super U> second) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) || second.test(t, u);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param first
     * @param second
     * @param third
     * @return
     */
    public static <T, U> BiPredicate<T, U> or(final BiPredicate<? super T, ? super U> first, final BiPredicate<? super T, ? super U> second,
            final BiPredicate<? super T, ? super U> third) {
        N.checkArgNotNull(first);
        N.checkArgNotNull(second);
        N.checkArgNotNull(third);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return first.test(t, u) || second.test(t, u) || third.test(t, u);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param c
     * @return
     * @throws IllegalArgumentException if the specified {@code c} is null or empty.
     */
    public static <T, U> BiPredicate<T, U> or(final List<BiPredicate<? super T, ? super U>> c) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(c, "c");

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                for (BiPredicate<? super T, ? super U> p : c) {
                    if (p.test(t, u)) {
                        return true;
                    }
                }

                return false;
            }
        };
    }

    /**
     * Test by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByKey(final Predicate<? super K> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getKey());
            }
        };
    }

    /**
     * Test by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testByValue(final Predicate<? super V> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getValue());
            }
        };
    }

    /**
     * Returns the specified instance.
     *
     * @param <T>
     * @param predicate
     * @return
     * @deprecated replaced by {@link Fn#p(Predicate)}.
     */
    @Deprecated
    static <T> Predicate<T> test(final Predicate<T> predicate) {
        return predicate;
    }

    /**
     * Returns the specified instance.
     *
     * @param <T>
     * @param <U>
     * @param predicate
     * @return
     * @deprecated replaced by {@link Fn#p(BiPredicate)}.
     */
    @Deprecated
    static <T, U> BiPredicate<T, U> test(final BiPredicate<T, U> predicate) {
        return predicate;
    }

    /**
     * Returns the specified instance.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param predicate
     * @return
     * @deprecated
     */
    @Deprecated
    static <A, B, C> TriPredicate<A, B, C> test(final TriPredicate<A, B, C> predicate) {
        return predicate;
    }

    /**
     * Accept by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByKey(final Consumer<? super K> consumer) {
        N.checkArgNotNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getKey());
            }
        };
    }

    /**
     * Accept by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptByValue(final Consumer<? super V> consumer) {
        N.checkArgNotNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getValue());
            }
        };
    }

    /**
     * Apply by key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByKey(final Function<? super K, R> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getKey());
            }
        };
    }

    /**
     * Apply by value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyByValue(final Function<? super V, R> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getValue());
            }
        };
    }

    /**
     * Apply if not null or default.
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param defaultValue
     * @return
     */
    public static <A, B, R> Function<A, R> applyIfNotNullOrDefault(final Function<A, B> mapperA, final Function<B, R> mapperB, final R defaultValue) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return defaultValue;
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return defaultValue;
                } else {
                    return mapperB.apply(b);
                }
            }
        };
    }

    /**
     * Apply if not null or default.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param defaultValue
     * @return
     */
    public static <A, B, C, R> Function<A, R> applyIfNotNullOrDefault(final Function<A, B> mapperA, final Function<B, C> mapperB, final Function<C, R> mapperC,
            final R defaultValue) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return defaultValue;
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return defaultValue;
                }

                final C c = mapperB.apply(b);

                if (c == null) {
                    return defaultValue;
                } else {
                    return mapperC.apply(c);
                }
            }
        };
    }

    /**
     * Apply if not null or default.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param mapperD
     * @param defaultValue
     * @return
     */
    public static <A, B, C, D, R> Function<A, R> applyIfNotNullOrDefault(final Function<A, B> mapperA, final Function<B, C> mapperB,
            final Function<C, D> mapperC, final Function<D, R> mapperD, final R defaultValue) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);
        N.checkArgNotNull(mapperD);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return defaultValue;
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return defaultValue;
                }

                final C c = mapperB.apply(b);

                if (c == null) {
                    return defaultValue;
                }

                final D d = mapperC.apply(c);

                if (d == null) {
                    return defaultValue;
                } else {
                    return mapperD.apply(d);
                }
            }
        };
    }

    /**
     * Apply if not null or get.
     *
     * @param <A>
     * @param <B>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param supplier
     * @return
     */
    public static <A, B, R> Function<A, R> applyIfNotNullOrGet(final Function<A, B> mapperA, final Function<B, R> mapperB, final Supplier<R> supplier) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return supplier.get();
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return supplier.get();
                } else {
                    return mapperB.apply(b);
                }
            }
        };
    }

    /**
     * Apply if not null or get.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param supplier
     * @return
     */
    public static <A, B, C, R> Function<A, R> applyIfNotNullOrGet(final Function<A, B> mapperA, final Function<B, C> mapperB, final Function<C, R> mapperC,
            final Supplier<R> supplier) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return supplier.get();
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return supplier.get();
                }

                final C c = mapperB.apply(b);

                if (c == null) {
                    return supplier.get();
                } else {
                    return mapperC.apply(c);
                }
            }
        };
    }

    /**
     * Apply if not null or get.
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <D>
     * @param <R>
     * @param mapperA
     * @param mapperB
     * @param mapperC
     * @param mapperD
     * @param supplier
     * @return
     */
    public static <A, B, C, D, R> Function<A, R> applyIfNotNullOrGet(final Function<A, B> mapperA, final Function<B, C> mapperB, final Function<C, D> mapperC,
            final Function<D, R> mapperD, final Supplier<R> supplier) {
        N.checkArgNotNull(mapperA);
        N.checkArgNotNull(mapperB);
        N.checkArgNotNull(mapperC);
        N.checkArgNotNull(mapperD);

        return new Function<A, R>() {
            @Override
            public R apply(A a) {
                if (a == null) {
                    return supplier.get();
                }

                final B b = mapperA.apply(a);

                if (b == null) {
                    return supplier.get();
                }

                final C c = mapperB.apply(b);

                if (c == null) {
                    return supplier.get();
                }

                final D d = mapperC.apply(c);

                if (d == null) {
                    return supplier.get();
                } else {
                    return mapperD.apply(d);
                }
            }
        };
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <KK>
     * @param func
     * @return
     */
    public static <K, V, KK> Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapKey(final Function<? super K, ? extends KK> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<KK, V>>() {
            @Override
            public Map.Entry<KK, V> apply(Entry<K, V> entry) {
                return new ImmutableEntry<>(func.apply(entry.getKey()), entry.getValue());
            }
        };
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <VV>
     * @param func
     * @return
     */
    public static <K, V, VV> Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapValue(final Function<? super V, ? extends VV> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<K, VV>>() {
            @Override
            public Map.Entry<K, VV> apply(Entry<K, V> entry) {
                return new ImmutableEntry<>(entry.getKey(), func.apply(entry.getValue()));
            }
        };
    }

    /**
     * Test key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate
     * @return
     */
    public static <K, V> Predicate<Map.Entry<K, V>> testKeyVal(final BiPredicate<? super K, ? super V> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<Map.Entry<K, V>>() {
            @Override
            public boolean test(Entry<K, V> entry) {
                return predicate.test(entry.getKey(), entry.getValue());
            }
        };
    }

    /**
     * Accept key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param consumer
     * @return
     */
    public static <K, V> Consumer<Map.Entry<K, V>> acceptKeyVal(final BiConsumer<? super K, ? super V> consumer) {
        N.checkArgNotNull(consumer);

        return new Consumer<Map.Entry<K, V>>() {
            @Override
            public void accept(Entry<K, V> entry) {
                consumer.accept(entry.getKey(), entry.getValue());
            }
        };
    }

    /**
     * Apply key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <R>
     * @param func
     * @return
     */
    public static <K, V, R> Function<Map.Entry<K, V>, R> applyKeyVal(final BiFunction<? super K, ? super V, R> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, R>() {
            @Override
            public R apply(Entry<K, V> entry) {
                return func.apply(entry.getKey(), entry.getValue());
            }
        };
    }

    private static Function<Map<Object, Collection<Object>>, List<Map<Object, Object>>> FLAT_TO_MAP_FUNC = new Function<Map<Object, Collection<Object>>, List<Map<Object, Object>>>() {
        @Override
        public List<Map<Object, Object>> apply(Map<Object, Collection<Object>> map) {
            return Maps.flatToMap(map);
        }
    };

    /** 
     * {a=[1, 2, 3], b=[4, 5, 6], c=[7, 8]} -> [{a=1, b=4, c=7}, {a=2, b=5, c=8}, {a=3, b=6}]
     *
     * @param <K> the key type
     * @param <V> the value type 
     * @return
     * @see Maps#flatToMap(Map)
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<? super Map<K, ? extends Collection<? extends V>>, List<Map<K, V>>> flatToMap() {
        return (Function) FLAT_TO_MAP_FUNC;
    }

    private static Function<String, Byte> PARSE_BYTE_FUNC = new Function<String, Byte>() {
        @Override
        public Byte apply(String t) {
            return Numbers.toByte(t);
        }
    };

    /**
     * Parses the byte.
     *
     * @return
     */
    public static Function<String, Byte> parseByte() {
        return PARSE_BYTE_FUNC;
    }

    private static Function<String, Short> PARSE_SHORT_FUNC = new Function<String, Short>() {
        @Override
        public Short apply(String t) {
            return Numbers.toShort(t);
        }
    };

    /**
     * Parses the short.
     *
     * @return
     */
    public static Function<String, Short> parseShort() {
        return PARSE_SHORT_FUNC;
    }

    private static Function<String, Integer> PARSE_INT_FUNC = new Function<String, Integer>() {
        @Override
        public Integer apply(String t) {
            return Numbers.toInt(t);
        }
    };

    /**
     * Parses the int.
     *
     * @return
     */
    public static Function<String, Integer> parseInt() {
        return PARSE_INT_FUNC;
    }

    private static Function<String, Long> PARSE_LONG_FUNC = new Function<String, Long>() {
        @Override
        public Long apply(String t) {
            return Numbers.toLong(t);
        }
    };

    /**
     * Parses the long.
     *
     * @return
     */
    public static Function<String, Long> parseLong() {
        return PARSE_LONG_FUNC;
    }

    private static Function<String, Float> PARSE_FLOAT_FUNC = new Function<String, Float>() {
        @Override
        public Float apply(String t) {
            return Numbers.toFloat(t);
        }
    };

    /**
     * Parses the float.
     *
     * @return
     */
    public static Function<String, Float> parseFloat() {
        return PARSE_FLOAT_FUNC;
    }

    private static Function<String, Double> PARSE_DOUBLE_FUNC = new Function<String, Double>() {
        @Override
        public Double apply(String t) {
            return Numbers.toDouble(t);
        }
    };

    /**
     * Parses the double.
     *
     * @return
     */
    public static Function<String, Double> parseDouble() {
        return PARSE_DOUBLE_FUNC;
    }

    private static Function<String, Number> CREATE_NUMBER_FUNC = new Function<String, Number>() {
        @Override
        public Number apply(final String t) {
            return N.isNullOrEmpty(t) ? null : Numbers.createNumber(t).orElseThrow(new Supplier<NumberFormatException>() {
                @Override
                public NumberFormatException get() {
                    return new NumberFormatException("Invalid number: " + t);
                }
            });
        }
    };

    /**
     * Creates the number.
     *
     * @return
     * @see Numbers#createNumber(String)
     */
    public static Function<String, Number> createNumber() {
        return CREATE_NUMBER_FUNC;
    }

    /**
     * Num to int.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToIntFunction<T> numToInt() {
        return (ToIntFunction) ToIntFunction.FROM_NUM;
    }

    /**
     * Num to long.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToLongFunction<T> numToLong() {
        return (ToLongFunction) ToLongFunction.FROM_NUM;
    }

    /**
     * Num to double.
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToDoubleFunction<T> numToDouble() {
        return (ToDoubleFunction) ToDoubleFunction.FROM_NUM;
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param limit
     * @param predicate
     * @return
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> limitThenFilter(final int limit, final Predicate<T> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<T>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t) {
                return counter.getAndDecrement() > 0 && predicate.test(t);
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param <U>
     * @param limit
     * @param predicate
     * @return
     */
    @Beta
    @Stateful
    public static <T, U> BiPredicate<T, U> limitThenFilter(final int limit, final BiPredicate<T, U> predicate) {
        N.checkArgNotNull(predicate);

        return new BiPredicate<T, U>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t, U u) {
                return counter.getAndDecrement() > 0 && predicate.test(t, u);
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param predicate
     * @param limit
     * @return
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> filterThenLimit(final Predicate<T> predicate, final int limit) {
        N.checkArgNotNull(predicate);

        return new Predicate<T>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t) {
                return predicate.test(t) && counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param <U>
     * @param predicate
     * @param limit
     * @return
     */
    @Beta
    @Stateful
    public static <T, U> BiPredicate<T, U> filterThenLimit(final BiPredicate<T, U> predicate, final int limit) {
        N.checkArgNotNull(predicate);

        return new BiPredicate<T, U>() {
            private final AtomicInteger counter = new AtomicInteger(limit);

            @Override
            public boolean test(T t, U u) {
                return predicate.test(t, u) && counter.getAndDecrement() > 0;
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param timeInMillis
     * @return
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> timeLimit(final long timeInMillis) {
        N.checkArgNotNegative(timeInMillis, "timeInMillis");

        if (timeInMillis == 0) {
            return Fn.alwaysFalse();
        }

        final MutableBoolean ongoing = MutableBoolean.of(true);

        final TimerTask task = new TimerTask() {
            @Override
            public void run() {
                ongoing.setFalse();
            }
        };

        timer.schedule(task, timeInMillis);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return ongoing.value();
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param duration
     * @return
     */
    @Beta
    @Stateful
    public static <T> Predicate<T> timeLimit(final Duration duration) {
        N.checkArgNotNull(duration, "duration");

        return timeLimit(duration.toMillis());
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @return
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Function<T, Indexed<T>> indexed() {
        return new Function<T, Indexed<T>>() {
            private final MutableLong idx = new MutableLong(0);

            @Override
            public Indexed<T> apply(T t) {
                return Indexed.of(t, idx.getAndIncrement());
            }
        };
    }

    /**
     * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @param predicate
     * @return
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> Predicate<T> indexed(final IndexedPredicate<T> predicate) {
        return Predicates.indexed(predicate);
    }

    /**
     *
     * @param <T>
     * @param target
     * @return
     */
    public static <T extends Comparable<? super T>> Function<T, Integer> compareTo(final T target) {
        return new Function<T, Integer>() {
            @Override
            public Integer apply(T t) {
                return N.compare(t, target);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param target
     * @param cmp
     * @return
     */
    public static <T> Function<T, Integer> compareTo(final T target, final Comparator<? super T> cmp) {
        // N.checkArgNotNull(cmp);

        if (cmp == null) {
            return new Function<T, Integer>() {
                @Override
                public Integer apply(T t) {
                    return NATURAL_ORDER.compare(t, target);
                }
            };
        } else {
            return new Function<T, Integer>() {
                @Override
                public Integer apply(T t) {
                    return N.compare(t, target, cmp);
                }
            };
        }
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T extends Comparable<? super T>> BiFunction<T, T, Integer> compare() {
        return COMPARE;
    }

    /**
     *
     * @param <T>
     * @param cmp
     * @return
     */
    public static <T> BiFunction<T, T, Integer> compare(final Comparator<? super T> cmp) {
        // N.checkArgNotNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) {
            return COMPARE;
        }

        return new BiFunction<T, T, Integer>() {
            @Override
            public Integer apply(T a, T b) {
                return N.compare(a, b, cmp);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param defaultValue
     * @return
     */
    @Beta
    public static <T> Function<Future<T>, T> futureGetOrDefaultOnError(final T defaultValue) {
        return new Function<Future<T>, T>() {
            @Override
            public T apply(Future<T> f) {
                try {
                    return f.get();
                } catch (InterruptedException | ExecutionException e) {
                    // throw N.toRuntimeException(e);
                    return defaultValue;
                }
            }
        };
    }

    private static final Function<Future<Object>, Object> FUTURE_GETTER = new Function<Future<Object>, Object>() {
        @Override
        public Object apply(Future<Object> f) {
            try {
                return f.get();
            } catch (InterruptedException | ExecutionException e) {
                return N.toRuntimeException(e);
            }
        }
    };

    /**
     *
     * @param <T>
     * @return
     */
    @SuppressWarnings("rawtypes")
    @Beta
    public static <T> Function<Future<T>, T> futureGet() {
        return (Function) FUTURE_GETTER;
    }

    /**
     *
     * @param <T>
     * @param supplier
     * @return
     */
    @Beta
    public static <T> Supplier<T> from(final java.util.function.Supplier<T> supplier) {
        return () -> supplier.get();
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     */
    @Beta
    public static <T> Predicate<T> from(final java.util.function.Predicate<T> predicate) {
        return t -> predicate.test(t);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param predicate
     * @return
     */
    @Beta
    public static <T, U> BiPredicate<T, U> from(final java.util.function.BiPredicate<T, U> predicate) {
        return (t, u) -> predicate.test(t, u);
    }

    /**
     *
     * @param <T>
     * @param consumer
     * @return
     */
    @Beta
    public static <T> Consumer<T> from(final java.util.function.Consumer<T> consumer) {
        return t -> consumer.accept(t);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param consumer
     * @return
     */
    @Beta
    public static <T, U> BiConsumer<T, U> from(final java.util.function.BiConsumer<T, U> consumer) {
        return (t, u) -> consumer.accept(t, u);
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param function
     * @return
     */
    @Beta
    public static <T, R> Function<T, R> from(final java.util.function.Function<T, R> function) {
        return t -> function.apply(t);
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param function
     * @return
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> from(final java.util.function.BiFunction<T, U, R> function) {
        return (t, u) -> function.apply(t, u);
    }

    /**
     *
     * @param <T>
     * @param op
     * @return
     */
    @Beta
    public static <T> UnaryOperator<T> from(final java.util.function.UnaryOperator<T> op) {
        return t -> op.apply(t);
    }

    /**
     *
     * @param <T>
     * @param op
     * @return
     */
    @Beta
    public static <T> BinaryOperator<T> from(final java.util.function.BinaryOperator<T> op) {
        return (t1, t2) -> op.apply(t1, t2);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param collector
     * @return
     */
    @Beta
    public static <T, A, R> Collector<T, A, R> from(final java.util.stream.Collector<T, A, R> collector) {
        return Collector.from(collector);
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     */
    @Beta
    public static <T> Predicate<T> p(final Predicate<T> predicate) {
        return predicate;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param a
     * @param biPredicate
     * @return
     */
    @Beta
    public static <A, T> Predicate<T> p(final A a, final BiPredicate<A, T> biPredicate) {
        N.checkArgNotNull(biPredicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return biPredicate.test(a, t);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param a
     * @param b
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, B, T> Predicate<T> p(final A a, final B b, final TriPredicate<A, B, T> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                return triPredicate.test(a, b, t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param biPredicate
     * @return
     */
    @Beta
    public static <T, U> BiPredicate<T, U> p(final BiPredicate<T, U> biPredicate) {
        return biPredicate;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param a
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, T, U> BiPredicate<T, U> p(final A a, final TriPredicate<A, T, U> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                return triPredicate.test(a, t, u);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, B, C> TriPredicate<A, B, C> p(final TriPredicate<A, B, C> triPredicate) {
        return triPredicate;
    }

    /**
     *
     * @param <T>
     * @param predicate
     * @return
     */
    @Beta
    public static <T> Consumer<T> c(final Consumer<T> predicate) {
        return predicate;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param a
     * @param biConsumer
     * @return
     */
    @Beta
    public static <A, T> Consumer<T> c(final A a, final BiConsumer<A, T> biConsumer) {
        N.checkArgNotNull(biConsumer);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                biConsumer.accept(a, t);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param a
     * @param b
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, B, T> Consumer<T> c(final A a, final B b, final TriConsumer<A, B, T> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                triConsumer.accept(a, b, t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param biConsumer
     * @return
     */
    @Beta
    public static <T, U> BiConsumer<T, U> c(final BiConsumer<T, U> biConsumer) {
        return biConsumer;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param a
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, T, U> BiConsumer<T, U> c(final A a, final TriConsumer<A, T, U> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new BiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                triConsumer.accept(a, t, u);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> c(final TriConsumer<A, B, C> triConsumer) {
        return triConsumer;
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param predicate
     * @return
     */
    @Beta
    public static <T, R> Function<T, R> f(final Function<T, R> predicate) {
        return predicate;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <R>
     * @param a
     * @param biFunction
     * @return
     */
    @Beta
    public static <A, T, R> Function<T, R> f(final A a, final BiFunction<A, T, R> biFunction) {
        N.checkArgNotNull(biFunction);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                return biFunction.apply(a, t);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <R>
     * @param a
     * @param b
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, B, T, R> Function<T, R> f(final A a, final B b, final TriFunction<A, B, T, R> triFunction) {
        N.checkArgNotNull(triFunction);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                return triFunction.apply(a, b, t);
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param biFunction
     * @return
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> f(final BiFunction<T, U, R> biFunction) {
        return biFunction;
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param <R>
     * @param a
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, T, U, R> BiFunction<T, U, R> f(final A a, final TriFunction<A, T, U, R> triFunction) {
        N.checkArgNotNull(triFunction);

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                return triFunction.apply(a, t, u);
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> f(final TriFunction<A, B, C, R> triFunction) {
        return triFunction;
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param predicate
     * @return
     */
    @Beta
    public static <T, E extends Exception> Predicate<T> pp(final Throwables.Predicate<T, E> predicate) {
        N.checkArgNotNull(predicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                try {
                    return predicate.test(value);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <E>
     * @param a
     * @param biPredicate
     * @return
     */
    @Beta
    public static <A, T, E extends Exception> Predicate<T> pp(final A a, final Throwables.BiPredicate<A, T, E> biPredicate) {
        N.checkArgNotNull(biPredicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                try {
                    return biPredicate.test(a, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, B, T, E extends Exception> Predicate<T> pp(final A a, final B b, final Throwables.TriPredicate<A, B, T, E> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                try {
                    return triPredicate.test(a, b, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param biPredicate
     * @return
     */
    @Beta
    public static <T, U, E extends Exception> BiPredicate<T, U> pp(final Throwables.BiPredicate<T, U, E> biPredicate) {
        N.checkArgNotNull(biPredicate);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                try {
                    return biPredicate.test(t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param <E>
     * @param a
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, T, U, E extends Exception> BiPredicate<T, U> pp(final A a, final Throwables.TriPredicate<A, T, U, E> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                try {
                    return triPredicate.test(a, t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, B, C, E extends Exception> TriPredicate<A, B, C> pp(final Throwables.TriPredicate<A, B, C, E> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new TriPredicate<A, B, C>() {
            @Override
            public boolean test(A a, B b, C c) {
                try {
                    return triPredicate.test(a, b, c);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <E>
     * @param consumer
     * @return
     */
    @Beta
    public static <T, E extends Exception> Consumer<T> cc(final Throwables.Consumer<T, E> consumer) {
        N.checkArgNotNull(consumer);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                try {
                    consumer.accept(t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <E>
     * @param a
     * @param biConsumer
     * @return
     */
    @Beta
    public static <A, T, E extends Exception> Consumer<T> cc(final A a, final Throwables.BiConsumer<A, T, E> biConsumer) {
        N.checkArgNotNull(biConsumer);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                try {
                    biConsumer.accept(a, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <E>
     * @param a
     * @param b
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, B, T, E extends Exception> Consumer<T> cc(final A a, final B b, final Throwables.TriConsumer<A, B, T, E> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                try {
                    triConsumer.accept(a, b, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <E>
     * @param biConsumer
     * @return
     */
    @Beta
    public static <T, U, E extends Exception> BiConsumer<T, U> cc(final Throwables.BiConsumer<T, U, E> biConsumer) {
        N.checkArgNotNull(biConsumer);

        return new BiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                try {
                    biConsumer.accept(t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param <E>
     * @param a
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, T, U, E extends Exception> BiConsumer<T, U> cc(final A a, final Throwables.TriConsumer<A, T, U, E> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new BiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                try {
                    triConsumer.accept(a, t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <E>
     * @param triConsumer
     * @return
     */
    @Beta
    public static <A, B, C, E extends Exception> TriConsumer<A, B, C> cc(final Throwables.TriConsumer<A, B, C, E> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new TriConsumer<A, B, C>() {
            @Override
            public void accept(A a, B b, C c) {
                try {
                    triConsumer.accept(a, b, c);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param function
     * @return
     */
    @Beta
    public static <T, R, E extends Exception> Function<T, R> ff(final Throwables.Function<T, R, E> function) {
        N.checkArgNotNull(function);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                try {
                    return function.apply(t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <R>
     * @param <E>
     * @param function
     * @param defaultOnError
     * @return
     */
    @Beta
    public static <T, R, E extends Exception> Function<T, R> ff(final Throwables.Function<T, R, E> function, final R defaultOnError) {
        N.checkArgNotNull(function);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                try {
                    return function.apply(t);
                } catch (Exception e) {
                    return defaultOnError;
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param biFunction
     * @return
     */
    @Beta
    public static <A, T, R, E extends Exception> Function<T, R> ff(final A a, final Throwables.BiFunction<A, T, R, E> biFunction) {
        N.checkArgNotNull(biFunction);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                try {
                    return biFunction.apply(a, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param <R>
     * @param <E>
     * @param a
     * @param b
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, B, T, R, E extends Exception> Function<T, R> ff(final A a, final B b, final Throwables.TriFunction<A, B, T, R, E> triFunction) {
        N.checkArgNotNull(triFunction);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                try {
                    return triFunction.apply(a, b, t);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param biFunction
     * @return
     */
    @Beta
    public static <T, U, R, E extends Exception> BiFunction<T, U, R> ff(final Throwables.BiFunction<T, U, R, E> biFunction) {
        N.checkArgNotNull(biFunction);

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                try {
                    return biFunction.apply(t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param biFunction
     * @param defaultOnError
     * @return
     */
    @Beta
    public static <T, U, R, E extends Exception> BiFunction<T, U, R> ff(final Throwables.BiFunction<T, U, R, E> biFunction, final R defaultOnError) {
        N.checkArgNotNull(biFunction);

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                try {
                    return biFunction.apply(t, u);
                } catch (Exception e) {
                    return defaultOnError;
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <T>
     * @param <U>
     * @param <R>
     * @param <E>
     * @param a
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, T, U, R, E extends Exception> BiFunction<T, U, R> ff(final A a, final Throwables.TriFunction<A, T, U, R, E> triFunction) {
        N.checkArgNotNull(triFunction);

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                try {
                    return triFunction.apply(a, t, u);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param triFunction
     * @return
     */
    @Beta
    public static <A, B, C, R, E extends Exception> TriFunction<A, B, C, R> ff(final Throwables.TriFunction<A, B, C, R, E> triFunction) {
        N.checkArgNotNull(triFunction);

        return new TriFunction<A, B, C, R>() {
            @Override
            public R apply(A a, B b, C c) {
                try {
                    return triFunction.apply(a, b, c);
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param <A>
     * @param <B>
     * @param <C>
     * @param <R>
     * @param <E>
     * @param triFunction
     * @param defaultOnError
     * @return
     */
    @Beta
    public static <A, B, C, R, E extends Exception> TriFunction<A, B, C, R> ff(final Throwables.TriFunction<A, B, C, R, E> triFunction,
            final R defaultOnError) {
        N.checkArgNotNull(triFunction);

        return new TriFunction<A, B, C, R>() {
            @Override
            public R apply(A a, B b, C c) {
                try {
                    return triFunction.apply(a, b, c);
                } catch (Exception e) {
                    return defaultOnError;
                }
            }
        };
    }

    /**
     * Synchronized {@code Predicate}.
     *
     * @param <T>
     * @param mutex to synchronized on
     * @param predicate
     * @return
     */
    @Beta
    public static <T> Predicate<T> sp(final Object mutex, final Predicate<T> predicate) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(predicate, "predicate");

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                synchronized (mutex) {
                    return predicate.test(t);
                }
            }
        };
    }

    /**
     * Synchronized {@code Predicate}.
     *
     * @param <A>
     * @param <T>
     * @param mutex to synchronized on
     * @param a
     * @param biPredicate
     * @return
     */
    @Beta
    public static <A, T> Predicate<T> sp(final Object mutex, final A a, final BiPredicate<A, T> biPredicate) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biPredicate, "biPredicate");

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                synchronized (mutex) {
                    return biPredicate.test(a, t);
                }
            }
        };
    }

    /**
     * Synchronized {@code Predicate}.
     *
     * @param <A>
     * @param <B>
     * @param <T>
     * @param mutex to synchronized on
     * @param a
     * @param b
     * @param triPredicate
     * @return
     */
    @Beta
    public static <A, B, T> Predicate<T> sp(final Object mutex, final A a, final B b, final TriPredicate<A, B, T> triPredicate) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(triPredicate, "triPredicate");

        return new Predicate<T>() {
            @Override
            public boolean test(T t) {
                synchronized (mutex) {
                    return triPredicate.test(a, b, t);
                }
            }
        };
    }

    /**
     * Synchronized {@code BiPredicate}.
     *
     * @param <T>
     * @param <U>
     * @param mutex to synchronized on
     * @param biPredicate
     * @return
     */
    @Beta
    public static <T, U> BiPredicate<T, U> sp(final Object mutex, final BiPredicate<T, U> biPredicate) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biPredicate, "biPredicate");

        return new BiPredicate<T, U>() {
            @Override
            public boolean test(T t, U u) {
                synchronized (mutex) {
                    return biPredicate.test(t, u);
                }
            }
        };
    }

    /**
     * Synchronized {@code Consumer}.
     *
     * @param <T>
     * @param mutex to synchronized on
     * @param consumer
     * @return
     */
    @Beta
    public static <T> Consumer<T> sc(final Object mutex, final Consumer<T> consumer) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(consumer, "consumer");

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                synchronized (mutex) {
                    consumer.accept(t);
                }
            }
        };
    }

    /**
     * Synchronized {@code Consumer}.
     *
     * @param <A>
     * @param <T>
     * @param mutex to synchronized on
     * @param a
     * @param biConsumer
     * @return
     */
    @Beta
    public static <A, T> Consumer<T> sc(final Object mutex, final A a, final BiConsumer<A, T> biConsumer) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biConsumer, "biConsumer");

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                synchronized (mutex) {
                    biConsumer.accept(a, t);
                }
            }
        };
    }

    /**
     * Synchronized {@code BiConsumer}.
     *
     * @param <T>
     * @param <U>
     * @param mutex to synchronized on
     * @param biConsumer
     * @return
     */
    @Beta
    public static <T, U> BiConsumer<T, U> sc(final Object mutex, final BiConsumer<T, U> biConsumer) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biConsumer, "biConsumer");

        return new BiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                synchronized (mutex) {
                    biConsumer.accept(t, u);
                }
            }
        };
    }

    /**
     * Synchronized {@code Function}.
     *
     * @param <T>
     * @param <R>
     * @param mutex to synchronized on
     * @param function
     * @return
     */
    @Beta
    public static <T, R> Function<T, R> sf(final Object mutex, final Function<T, R> function) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(function, "function");

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                synchronized (mutex) {
                    return function.apply(t);
                }
            }
        };
    }

    /**
     * Synchronized {@code Function}.
     *
     * @param <A>
     * @param <T>
     * @param <R>
     * @param mutex to synchronized on
     * @param a
     * @param biFunction
     * @return
     */
    @Beta
    public static <A, T, R> Function<T, R> sf(final Object mutex, final A a, final BiFunction<A, T, R> biFunction) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biFunction, "biFunction");

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                synchronized (mutex) {
                    return biFunction.apply(a, t);
                }
            }
        };
    }

    /**
     * Synchronized {@code BiFunction}.
     *
     * @param <T>
     * @param <U>
     * @param <R>
     * @param mutex to synchronized on
     * @param biFunction
     * @return
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> sf(final Object mutex, final BiFunction<T, U, R> biFunction) {
        N.checkArgNotNull(mutex, "mutex");
        N.checkArgNotNull(biFunction, "biFunction");

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                synchronized (mutex) {
                    return biFunction.apply(t, u);
                }
            }
        };
    }

    public static <T> Function<T, Void> c2f(final Consumer<? super T> action) {
        N.checkArgNotNull(action);

        return new Function<T, Void>() {
            @Override
            public Void apply(T t) {
                action.accept(t);
                return null;
            }
        };
    }

    public static <T, R> Function<T, R> c2f(final Consumer<? super T> action, final R valueToReturn) {
        N.checkArgNotNull(action);

        return new Function<T, R>() {
            @Override
            public R apply(T t) {
                action.accept(t);
                return valueToReturn;
            }
        };
    }

    public static <T, U> BiFunction<T, U, Void> c2f(final BiConsumer<? super T, ? super U> action) {
        N.checkArgNotNull(action);

        return new BiFunction<T, U, Void>() {
            @Override
            public Void apply(T t, U u) {
                action.accept(t, u);
                return null;
            }
        };
    }

    public static <T, U, R> BiFunction<T, U, R> c2f(final BiConsumer<? super T, ? super U> action, final R valueToReturn) {
        N.checkArgNotNull(action);

        return new BiFunction<T, U, R>() {
            @Override
            public R apply(T t, U u) {
                action.accept(t, u);
                return valueToReturn;
            }
        };
    }

    public static <A, B, C> TriFunction<A, B, C, Void> c2f(final TriConsumer<? super A, ? super B, ? super C> action) {
        N.checkArgNotNull(action);

        return new TriFunction<A, B, C, Void>() {
            @Override
            public Void apply(A a, B b, C c) {
                action.accept(a, b, c);
                return null;
            }
        };
    }

    public static <A, B, C, R> TriFunction<A, B, C, R> c2f(final TriConsumer<? super A, ? super B, ? super C> action, final R valueToReturn) {
        N.checkArgNotNull(action);

        return new TriFunction<A, B, C, R>() {
            @Override
            public R apply(A a, B b, C c) {
                action.accept(a, b, c);
                return valueToReturn;
            }
        };
    }

    /**
     * Returns a <code>Consumer</code> which calls the specified <code>func</code>.
     *
     * @param <T>
     * @param func
     * @return
     */
    public static <T> Consumer<T> f2c(final Function<? super T, ?> func) {
        N.checkArgNotNull(func);

        return new Consumer<T>() {
            @Override
            public void accept(T t) {
                func.apply(t);
            }
        };
    }

    public static <T, U> BiConsumer<T, U> f2c(final BiFunction<? super T, ? super U, ?> func) {
        N.checkArgNotNull(func);

        return new BiConsumer<T, U>() {
            @Override
            public void accept(T t, U u) {
                func.apply(t, u);
            }
        };
    }

    public static <A, B, C> TriConsumer<A, B, C> f2c(final TriFunction<? super A, ? super B, ? super C, ?> func) {
        N.checkArgNotNull(func);

        return new TriConsumer<A, B, C>() {
            @Override
            public void accept(A a, B b, C c) {
                func.apply(a, b, c);
            }
        };
    }

    public static <E extends Exception> Runnable rr(final Throwables.Runnable<E> runnbale) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    runnbale.run();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    public static <R, E extends Exception> Callable<R> cc(final Throwables.Callable<R, E> callable) {
        return new Callable<R>() {
            @Override
            public R call() {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
     *
     * @param runnable
     * @return
     */
    public static Callable<Void> r2c(final Runnable runnable) {
        N.checkArgNotNull(runnable);

        return new Callable<Void>() {
            @Override
            public Void call() {
                runnable.run();
                return null;
            }
        };
    }

    /**
     *
     * @param runnable
     * @param valueToReturn
     * @return
     */
    public static <R> Callable<R> r2c(final Runnable runnable, final R valueToReturn) {
        N.checkArgNotNull(runnable);

        return new Callable<R>() {
            @Override
            public R call() {
                runnable.run();
                return valueToReturn;
            }
        };
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     */
    public static <R> Runnable c2r(final Callable<R> callable) {
        N.checkArgNotNull(callable);

        return new Runnable() {
            @Override
            public void run() {
                callable.call();
            }
        };
    }

    /**
     *
     * @param runnable
     * @return
     */
    public static Runnable r(final Runnable runnable) {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
    *
    * @param <R>
    * @param callable
    * @return
    */
    public static <R> Callable<R> c(final Callable<R> callable) {
        N.checkArgNotNull(callable);

        return callable;
    }

    public static Runnable jr2r(final java.lang.Runnable runnable) {
        N.checkArgNotNull(runnable);

        if (runnable instanceof Runnable) {
            return (Runnable) runnable;
        }

        return new Runnable() {
            @Override
            public void run() {
                runnable.run();
            }
        };
    }

    public static Callable<Void> jr2c(final java.lang.Runnable runnable) {
        N.checkArgNotNull(runnable);

        return new Callable<Void>() {
            @Override
            public Void call() {
                runnable.run();
                return null;
            }
        };
    }

    public static <R> Callable<R> jc2c(final java.util.concurrent.Callable<R> callable) {
        N.checkArgNotNull(callable);

        if (callable instanceof Callable) {
            return (Callable<R>) callable;
        }

        return new Callable<R>() {
            @Override
            public R call() {
                try {
                    return callable.call();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    public static Runnable jc2r(final java.util.concurrent.Callable<?> callable) {
        N.checkArgNotNull(callable);

        return new Runnable() {
            @Override
            public void run() {
                try {
                    callable.call();
                } catch (Exception e) {
                    throw N.toRuntimeException(e);
                }
            }
        };
    }

    /**
    *
    * @param runnable
    * @return 
    * @deprecated replaced by {@link #r(Runnable)}
    */
    @Deprecated
    public static Runnable runnable(final Runnable runnable) {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     *
     * @param <R>
     * @param callable
     * @return
     * @deprecated replaced by {@link #c(Callable)}
     */
    @Deprecated
    public static <R> Callable<R> callable(final Callable<R> callable) {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     * Split this stream by the specified duration.
     *
     * <pre>
     * <code>
     * Stream<Timed<MyObject>> s = ...;
     * s.__(Fn.window(Duration.ofMinutes(3), () - System.currentTimeMillis()))...// Do your stuffs with Stream<Stream<Timed<T>>>;
     * </code>
     * </pre>
     *
     * @param <T>
     * @param duration
     * @param startTime
     * @return
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>> window(final Duration duration, final LongSupplier startTime) {
        return window(duration, duration.toMillis(), startTime);
    }

    /**
     * Window to list.
     *
     * @param <T>
     * @param duration
     * @param startTime
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> windowToList(final Duration duration, final LongSupplier startTime) {
        return window(duration, startTime, Suppliers.<Timed<T>> ofList());
    }

    /**
     * Window to set.
     *
     * @param <T>
     * @param duration
     * @param startTime
     * @return
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Set<Timed<T>>>> windowToSet(final Duration duration, final LongSupplier startTime) {
        return window(duration, startTime, Suppliers.<Timed<T>> ofSet());
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param duration
     * @param startTime
     * @param collectionSupplier
     * @return
     */
    public static <T, C extends Collection<Timed<T>>> Function<Stream<Timed<T>>, Stream<C>> window(final Duration duration, final LongSupplier startTime,
            final Supplier<? extends C> collectionSupplier) {
        return window(duration, duration.toMillis(), startTime, collectionSupplier);
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param duration
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T, A, R> Function<Stream<Timed<T>>, Stream<R>> window(final Duration duration, final LongSupplier startTime,
            final Collector<? super Timed<T>, A, R> collector) {
        return window(duration, duration.toMillis(), startTime, collector);
    }

    /**
     *
     * @param <T>
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>> window(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime) {
        final Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> mapper = windowToList(duration, incrementInMillis, startTime);

        return new Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>>() {
            @Override
            public Stream<Stream<Timed<T>>> apply(Stream<Timed<T>> t) {
                return mapper.apply(t).map(new Function<List<Timed<T>>, Stream<Timed<T>>>() {
                    @Override
                    public Stream<Timed<T>> apply(List<Timed<T>> t) {
                        return Stream.of(t);
                    }
                });
            }
        };
    }

    /**
     * Window to list.
     *
     * @param <T>
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> windowToList(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime) {
        return window(duration, incrementInMillis, startTime, Suppliers.<Timed<T>> ofList());
    }

    /**
     * Window to set.
     *
     * @param <T>
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Set<Timed<T>>>> windowToSet(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime) {
        return window(duration, incrementInMillis, startTime, Suppliers.<Timed<T>> ofSet());
    }

    /**
     *
     * @param <T>
     * @param <C>
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @param collectionSupplier
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T, C extends Collection<Timed<T>>> Function<Stream<Timed<T>>, Stream<C>> window(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime, final Supplier<? extends C> collectionSupplier) {
        return new Function<Stream<Timed<T>>, Stream<C>>() {
            @Override
            public Stream<C> apply(final Stream<Timed<T>> s) {
                final ObjIterator<C> iter = new ObjIteratorEx<C>() {
                    private long durationInMillis;
                    private boolean useQueue;

                    private Deque<Timed<T>> queue;
                    private Iterator<Timed<T>> queueIter;

                    private ObjIterator<Timed<T>> iter;
                    private Timed<T> next = null;

                    private long fromTime;
                    private long endTime;

                    private boolean initialized = false;

                    @Override
                    public boolean hasNext() {
                        if (initialized == false) {
                            init();
                        }

                        if (useQueue) {
                            if ((queue.size() > 0 && queue.getLast().timestamp() >= endTime)
                                    || (next != null && next.timestamp() - fromTime >= incrementInMillis)) {
                                return true;
                            } else {
                                while (iter.hasNext()) {
                                    next = iter.next();

                                    if (next.timestamp() - fromTime >= incrementInMillis) {
                                        queue.add(next);
                                        return true;
                                    }
                                }

                                return false;
                            }
                        } else {
                            while ((next == null || next.timestamp() - fromTime < incrementInMillis) && iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() - fromTime >= incrementInMillis) {
                                    break;
                                }
                            }

                            return next != null && next.timestamp() - fromTime >= incrementInMillis;
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
                                next = queueIter.next();

                                if (next.timestamp() < fromTime) {
                                    queueIter.remove();
                                } else if (next.timestamp() < endTime) {
                                    result.add(next);
                                    next = null;
                                } else {
                                    return result;
                                }
                            }

                            while (iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() >= fromTime) {
                                    queue.add(next);

                                    if (next.timestamp() < endTime) {
                                        result.add(next);
                                        next = null;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (next != null) {
                                if (next.timestamp() < fromTime) {
                                    // ignore
                                } else if (next.timestamp() < endTime) {
                                    result.add(next);
                                } else {
                                    return result;
                                }
                            }

                            while (iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() < fromTime) {
                                    // ignore
                                } else if (next.timestamp() < endTime) {
                                    result.add(next);
                                } else {
                                    break;
                                }
                            }
                        }

                        return result;
                    }

                    private void init() {
                        if (initialized == false) {
                            initialized = true;

                            N.checkArgNotNull(duration, "duration");
                            N.checkArgPositive(duration.toMillis(), "duration");
                            N.checkArgPositive(incrementInMillis, "incrementInMillis");
                            N.checkArgNotNull(startTime, "startTime");
                            N.checkArgNotNull(collectionSupplier, "collectionSupplier");

                            iter = s.iterator();

                            durationInMillis = duration.toMillis();
                            useQueue = incrementInMillis < durationInMillis;

                            if (useQueue) {
                                queue = new ArrayDeque<>();
                            }

                            fromTime = startTime.getAsLong() - incrementInMillis;
                            endTime = fromTime + durationInMillis;
                        }
                    }
                };

                return Stream.of(iter).onClose(new java.lang.Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param duration
     * @param incrementInMillis
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration, LongSupplier)
     */
    public static <T, A, R> Function<Stream<Timed<T>>, Stream<R>> window(final Duration duration, final long incrementInMillis, final LongSupplier startTime,
            final Collector<? super Timed<T>, A, R> collector) {
        return new Function<Stream<Timed<T>>, Stream<R>>() {
            @Override
            public Stream<R> apply(final Stream<Timed<T>> s) {
                final ObjIterator<R> iter = new ObjIteratorEx<R>() {
                    private long durationInMillis;
                    private boolean useQueue;

                    private Deque<Timed<T>> queue;
                    private Iterator<Timed<T>> queueIter;

                    private Supplier<A> supplier;
                    private BiConsumer<A, ? super Timed<T>> accumulator;
                    private Function<A, R> finisher;

                    private ObjIterator<Timed<T>> iter;
                    private Timed<T> next = null;

                    private long fromTime;
                    private long endTime;

                    private boolean initialized = false;

                    @Override
                    public boolean hasNext() {
                        if (initialized == false) {
                            init();
                        }

                        if (useQueue) {
                            if ((queue.size() > 0 && queue.getLast().timestamp() >= endTime)
                                    || (next != null && next.timestamp() - fromTime >= incrementInMillis)) {
                                return true;
                            } else {
                                while (iter.hasNext()) {
                                    next = iter.next();

                                    if (next.timestamp() - fromTime >= incrementInMillis) {
                                        queue.add(next);
                                        return true;
                                    }
                                }

                                return false;
                            }
                        } else {
                            while ((next == null || next.timestamp() - fromTime < incrementInMillis) && iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() - fromTime >= incrementInMillis) {
                                    break;
                                }
                            }

                            return next != null && next.timestamp() - fromTime >= incrementInMillis;
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
                                next = queueIter.next();

                                if (next.timestamp() < fromTime) {
                                    queueIter.remove();
                                } else if (next.timestamp() < endTime) {
                                    accumulator.accept(container, next);
                                    next = null;
                                } else {
                                    return finisher.apply(container);
                                }
                            }

                            while (iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() >= fromTime) {
                                    queue.add(next);

                                    if (next.timestamp() < endTime) {
                                        accumulator.accept(container, next);
                                        next = null;
                                    } else {
                                        break;
                                    }
                                }
                            }
                        } else {
                            if (next != null) {
                                if (next.timestamp() < fromTime) {
                                    // ignore
                                } else if (next.timestamp() < endTime) {
                                    accumulator.accept(container, next);
                                } else {
                                    return finisher.apply(container);
                                }
                            }

                            while (iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() < fromTime) {
                                    // ignore
                                } else if (next.timestamp() < endTime) {
                                    accumulator.accept(container, next);
                                } else {
                                    break;
                                }
                            }
                        }

                        return finisher.apply(container);
                    }

                    private void init() {
                        if (initialized == false) {
                            initialized = true;

                            N.checkArgNotNull(duration, "duration");
                            N.checkArgPositive(duration.toMillis(), "duration");
                            N.checkArgPositive(incrementInMillis, "incrementInMillis");
                            N.checkArgNotNull(startTime, "startTime");
                            N.checkArgNotNull(collector, "collector");

                            durationInMillis = duration.toMillis();
                            useQueue = incrementInMillis < durationInMillis;

                            if (useQueue) {
                                queue = new ArrayDeque<>();
                            }

                            supplier = collector.supplier();
                            accumulator = collector.accumulator();
                            finisher = collector.finisher();

                            iter = s.iterator();

                            fromTime = startTime.getAsLong() - incrementInMillis;
                            endTime = fromTime + durationInMillis;
                        }
                    }
                };

                return Stream.of(iter).onClose(new java.lang.Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     * Split this stream by the specified duration.
     *
     * <pre>
     * <code>
     * Stream<Timed<MyObject>> s = ...;
     * s.__(Fn.window(Duration.ofMinutes(3), () - System.currentTimeMillis()))...// Do your stuffs with Stream<Stream<Timed<T>>>;
     * </code>
     * </pre>
     *
     * @param <T>
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @return
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>> window(final int maxWindowSize, final Duration maxDuration,
            final LongSupplier startTime) {
        final Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> mapper = window(maxWindowSize, maxDuration, startTime, Suppliers.<Timed<T>> ofList());

        return new Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>>() {
            @Override
            public Stream<Stream<Timed<T>>> apply(Stream<Timed<T>> t) {
                return mapper.apply(t).map(new Function<List<Timed<T>>, Stream<Timed<T>>>() {
                    @Override
                    public Stream<Timed<T>> apply(List<Timed<T>> t) {
                        return Stream.of(t);
                    }
                });
            }
        };
    }

    /**
     * Split this stream at where {@code maxWindowSize} or {@code maxDuration} reaches first.
     *
     * @param <T>
     * @param <C>
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @param collectionSupplier
     * @return
     * @see #window(Duration, long, LongSupplier, Supplier)
     */
    public static <T, C extends Collection<Timed<T>>> Function<Stream<Timed<T>>, Stream<C>> window(final int maxWindowSize, final Duration maxDuration,
            final LongSupplier startTime, final Supplier<? extends C> collectionSupplier) {
        return new Function<Stream<Timed<T>>, Stream<C>>() {
            @Override
            public Stream<C> apply(final Stream<Timed<T>> s) {
                final ObjIterator<C> iter = new ObjIteratorEx<C>() {
                    private long maxDurationInMillis;

                    private ObjIterator<Timed<T>> iter;
                    private Timed<T> next = null;

                    private long fromTime;
                    private long endTime;

                    private boolean initialized = false;

                    @Override
                    public boolean hasNext() {
                        if (initialized == false) {
                            init();
                        }

                        while ((next == null || next.timestamp() < endTime) && iter.hasNext()) {
                            next = iter.next();
                        }

                        return next != null && next.timestamp() >= endTime;
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

                        if (next != null && next.timestamp() < endTime) {
                            result.add(next);
                            next = null;
                            cnt++;
                        }

                        if (next == null) {
                            while (cnt < maxWindowSize && iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() < endTime) {
                                    result.add(next);
                                    next = null;
                                    cnt++;
                                } else {
                                    break;
                                }
                            }
                        }

                        endTime = N.min(endTime, next == null ? System.currentTimeMillis() : next.timestamp());

                        return result;
                    }

                    private void init() {
                        if (initialized == false) {
                            initialized = true;

                            N.checkArgNotNull(maxDuration, "maxDuration");
                            N.checkArgPositive(maxDuration.toMillis(), "maxDuration");
                            N.checkArgPositive(maxWindowSize, "maxWindowSize");
                            N.checkArgNotNull(startTime, "startTime");
                            N.checkArgNotNull(collectionSupplier, "collectionSupplier");

                            iter = s.iterator();

                            maxDurationInMillis = maxDuration.toMillis();

                            fromTime = startTime.getAsLong() - maxDurationInMillis;
                            endTime = fromTime + maxDurationInMillis;
                        }
                    }
                };

                return Stream.of(iter).onClose(new java.lang.Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     * Split this stream at where {@code maxWindowSize} or {@code maxDuration} reaches first.
     *
     * @param <T>
     * @param <A>
     * @param <R>
     * @param maxWindowSize
     * @param maxDuration
     * @param startTime
     * @param collector
     * @return
     * @see #window(Duration, long, LongSupplier, Collector)
     */
    public static <T, A, R> Function<Stream<Timed<T>>, Stream<R>> window(final int maxWindowSize, final Duration maxDuration, final LongSupplier startTime,
            final Collector<? super Timed<T>, A, R> collector) {
        return new Function<Stream<Timed<T>>, Stream<R>>() {
            @Override
            public Stream<R> apply(final Stream<Timed<T>> s) {
                final ObjIterator<R> iter = new ObjIteratorEx<R>() {
                    private long maxDurationInMillis;

                    private Supplier<A> supplier;
                    private BiConsumer<A, ? super Timed<T>> accumulator;
                    private Function<A, R> finisher;

                    private ObjIterator<Timed<T>> iter;
                    private Timed<T> next = null;

                    private long fromTime;
                    private long endTime;

                    private boolean initialized = false;

                    @Override
                    public boolean hasNext() {
                        if (initialized == false) {
                            init();
                        }

                        while ((next == null || next.timestamp() < endTime) && iter.hasNext()) {
                            next = iter.next();
                        }

                        return next != null && next.timestamp() >= endTime;
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

                        if (next != null && next.timestamp() < endTime) {
                            accumulator.accept(container, next);
                            next = null;
                            cnt++;
                        }

                        if (next == null) {
                            while (cnt < maxWindowSize && iter.hasNext()) {
                                next = iter.next();

                                if (next.timestamp() < endTime) {
                                    accumulator.accept(container, next);
                                    next = null;
                                    cnt++;
                                } else {
                                    break;
                                }
                            }
                        }

                        endTime = N.min(endTime, next == null ? System.currentTimeMillis() : next.timestamp());

                        return finisher.apply(container);
                    }

                    private void init() {
                        if (initialized == false) {
                            initialized = true;

                            N.checkArgNotNull(maxDuration, "maxDuration");
                            N.checkArgPositive(maxDuration.toMillis(), "maxDuration");
                            N.checkArgPositive(maxWindowSize, "maxWindowSize");
                            N.checkArgNotNull(startTime, "startTime");
                            N.checkArgNotNull(collector, "collector");

                            supplier = collector.supplier();
                            accumulator = collector.accumulator();
                            finisher = collector.finisher();

                            iter = s.iterator();

                            maxDurationInMillis = maxDuration.toMillis();

                            fromTime = startTime.getAsLong() - maxDurationInMillis;
                            endTime = fromTime + maxDurationInMillis;
                        }
                    }
                };

                return Stream.of(iter).onClose(new java.lang.Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> throwingMerger() {
        return BinaryOperators.THROWING_MERGER;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> ignoringMerger() {
        return BinaryOperators.IGNORING_MERGER;
    }

    /**
     *
     * @param <T>
     * @return
     */
    public static <T> BinaryOperator<T> replacingMerger() {
        return BinaryOperators.REPLACING_MERGER;
    }

    /**
     * Returns a stateful {@code BiFunction}. Don't save or cache for reuse or use it in parallel stream.
     *
     * @param <T>
     * @return
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static <T> BiFunction<T, T, MergeResult> alternated() {
        return new BiFunction<T, T, MergeResult>() {
            private final MutableBoolean flag = MutableBoolean.of(true);

            @Override
            public MergeResult apply(T t, T u) {
                return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
            }
        };
    }

    /**
     * Adds the all.
     *
     * @param <T>
     * @param <C>
     * @return
     * @deprecated replaced by {@code BiConsumers#ofAddAll()}
     */
    @Deprecated
    static <T, C extends Collection<T>> BiConsumer<C, C> addAll() {
        return BiConsumers.<T, C> ofAddAll();
    }

    /**
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M>
     * @return
     * @deprecated replaced by {@code BiConsumers#ofPutAll()}
     */
    @Deprecated
    static <K, V, M extends Map<K, V>> BiConsumer<M, M> putAll() {
        return BiConsumers.<K, V, M> ofPutAll();
    }

    /**
     * The Class Factory.
     */
    public static abstract class Factory {

        /** The Constant BOOLEAN_ARRAY. */
        private static final IntFunction<boolean[]> BOOLEAN_ARRAY = new IntFunction<boolean[]>() {
            @Override
            public boolean[] apply(int len) {
                return new boolean[len];
            }
        };

        /** The Constant CHAR_ARRAY. */
        private static final IntFunction<char[]> CHAR_ARRAY = new IntFunction<char[]>() {
            @Override
            public char[] apply(int len) {
                return new char[len];
            }
        };

        /** The Constant BYTE_ARRAY. */
        private static final IntFunction<byte[]> BYTE_ARRAY = new IntFunction<byte[]>() {
            @Override
            public byte[] apply(int len) {
                return new byte[len];
            }
        };

        /** The Constant SHORT_ARRAY. */
        private static final IntFunction<short[]> SHORT_ARRAY = new IntFunction<short[]>() {
            @Override
            public short[] apply(int len) {
                return new short[len];
            }
        };

        /** The Constant INT_ARRAY. */
        private static final IntFunction<int[]> INT_ARRAY = new IntFunction<int[]>() {
            @Override
            public int[] apply(int len) {
                return new int[len];
            }
        };

        /** The Constant LONG_ARRAY. */
        private static final IntFunction<long[]> LONG_ARRAY = new IntFunction<long[]>() {
            @Override
            public long[] apply(int len) {
                return new long[len];
            }
        };

        /** The Constant FLOAT_ARRAY. */
        private static final IntFunction<float[]> FLOAT_ARRAY = new IntFunction<float[]>() {
            @Override
            public float[] apply(int len) {
                return new float[len];
            }
        };

        /** The Constant DOUBLE_ARRAY. */
        private static final IntFunction<double[]> DOUBLE_ARRAY = new IntFunction<double[]>() {
            @Override
            public double[] apply(int len) {
                return new double[len];
            }
        };

        /** The Constant STRING_ARRAY. */
        private static final IntFunction<String[]> STRING_ARRAY = new IntFunction<String[]>() {
            @Override
            public String[] apply(int len) {
                return new String[len];
            }
        };

        /** The Constant OBJECT_ARRAY. */
        private static final IntFunction<Object[]> OBJECT_ARRAY = new IntFunction<Object[]>() {
            @Override
            public Object[] apply(int len) {
                return new Object[len];
            }
        };

        /** The Constant BOOLEAN_LIST. */
        private static final IntFunction<BooleanList> BOOLEAN_LIST = new IntFunction<BooleanList>() {
            @Override
            public BooleanList apply(int len) {
                return new BooleanList(len);
            }
        };

        /** The Constant CHAR_LIST. */
        private static final IntFunction<CharList> CHAR_LIST = new IntFunction<CharList>() {
            @Override
            public CharList apply(int len) {
                return new CharList(len);
            }
        };

        /** The Constant BYTE_LIST. */
        private static final IntFunction<ByteList> BYTE_LIST = new IntFunction<ByteList>() {
            @Override
            public ByteList apply(int len) {
                return new ByteList(len);
            }
        };

        /** The Constant SHORT_LIST. */
        private static final IntFunction<ShortList> SHORT_LIST = new IntFunction<ShortList>() {
            @Override
            public ShortList apply(int len) {
                return new ShortList(len);
            }
        };

        /** The Constant INT_LIST. */
        private static final IntFunction<IntList> INT_LIST = new IntFunction<IntList>() {
            @Override
            public IntList apply(int len) {
                return new IntList(len);
            }
        };

        /** The Constant LONG_LIST. */
        private static final IntFunction<LongList> LONG_LIST = new IntFunction<LongList>() {
            @Override
            public LongList apply(int len) {
                return new LongList(len);
            }
        };

        /** The Constant FLOAT_LIST. */
        private static final IntFunction<FloatList> FLOAT_LIST = new IntFunction<FloatList>() {
            @Override
            public FloatList apply(int len) {
                return new FloatList(len);
            }
        };

        /** The Constant DOUBLE_LIST. */
        private static final IntFunction<DoubleList> DOUBLE_LIST = new IntFunction<DoubleList>() {
            @Override
            public DoubleList apply(int len) {
                return new DoubleList(len);
            }
        };

        /** The Constant LIST_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super List> LIST_FACTORY = new IntFunction<List>() {
            @Override
            public List apply(int len) {
                return new ArrayList<>(len);
            }
        };

        /** The Constant LINKED_LIST_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedList> LINKED_LIST_FACTORY = new IntFunction<LinkedList>() {
            @Override
            public LinkedList apply(int len) {
                return new LinkedList<>();
            }
        };

        /** The Constant SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Set> SET_FACTORY = new IntFunction<Set>() {
            @Override
            public Set apply(int len) {
                return N.newHashSet(len);
            }
        };

        /** The Constant LINKED_HASH_SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Set> LINKED_HASH_SET_FACTORY = new IntFunction<Set>() {
            @Override
            public Set apply(int len) {
                return N.newLinkedHashSet(len);
            }
        };

        /** The Constant TREE_SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeSet> TREE_SET_FACTORY = new IntFunction<TreeSet>() {
            @Override
            public TreeSet apply(int len) {
                return new TreeSet<>();
            }
        };

        /** The Constant QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Queue> QUEUE_FACTORY = new IntFunction<Queue>() {
            @Override
            public Queue apply(int len) {
                return new LinkedList();
            }
        };

        /** The Constant DEQUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Deque> DEQUE_FACTORY = new IntFunction<Deque>() {
            @Override
            public Deque apply(int len) {
                return new LinkedList();
            }
        };

        /** The Constant ARRAY_DEQUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ArrayDeque> ARRAY_DEQUE_FACTORY = new IntFunction<ArrayDeque>() {
            @Override
            public ArrayDeque apply(int len) {
                return new ArrayDeque(len);
            }
        };

        /** The Constant LINKED_BLOCKING_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE_FACTORY = new IntFunction<LinkedBlockingQueue>() {
            @Override
            public LinkedBlockingQueue apply(int len) {
                return new LinkedBlockingQueue(len);
            }
        };

        /** The Constant CONCURRENT_LINKED_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE_FACTORY = new IntFunction<ConcurrentLinkedQueue>() {
            @Override
            public ConcurrentLinkedQueue apply(int len) {
                return new ConcurrentLinkedQueue();
            }
        };

        /** The Constant PRIORITY_QUEUE_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super PriorityQueue> PRIORITY_QUEUE_FACTORY = new IntFunction<PriorityQueue>() {
            @Override
            public PriorityQueue apply(int len) {
                return new PriorityQueue(len);
            }
        };

        /** The Constant MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Map> MAP_FACTORY = new IntFunction<Map>() {
            @Override
            public Map apply(int len) {
                return N.newHashMap(len);
            }
        };

        /** The Constant LINKED_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Map> LINKED_HASH_MAP_FACTORY = new IntFunction<Map>() {
            @Override
            public Map apply(int len) {
                return N.newLinkedHashMap(len);
            }
        };

        /** The Constant IDENTITY_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super IdentityHashMap> IDENTITY_HASH_MAP_FACTORY = new IntFunction<IdentityHashMap>() {
            @Override
            public IdentityHashMap apply(int len) {
                return N.newIdentityHashMap(len);
            }
        };

        /** The Constant TREE_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeMap> TREE_MAP_FACTORY = new IntFunction<TreeMap>() {
            @Override
            public TreeMap apply(int len) {
                return N.newTreeMap();
            }
        };

        /** The Constant CONCURRENT_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentHashMap> CONCURRENT_HASH_MAP_FACTORY = new IntFunction<ConcurrentHashMap>() {
            @Override
            public ConcurrentHashMap apply(int len) {
                return N.newConcurrentHashMap(len);
            }
        };

        /** The Constant BI_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super BiMap> BI_MAP_FACTORY = new IntFunction<BiMap>() {
            @Override
            public BiMap apply(int len) {
                return N.newBiMap(len);
            }
        };

        /** The Constant MULTISET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Multiset> MULTISET_FACTORY = new IntFunction<Multiset>() {
            @Override
            public Multiset apply(int len) {
                return N.newMultiset(len);
            }
        };

        /** The Constant LONG_MULTISET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LongMultiset> LONG_MULTISET_FACTORY = new IntFunction<LongMultiset>() {
            @Override
            public LongMultiset apply(int len) {
                return N.newLongMultiset(len);
            }
        };

        /** The Constant LIST_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ListMultimap> LIST_MULTIMAP_FACTORY = new IntFunction<ListMultimap>() {
            @Override
            public ListMultimap apply(int len) {
                return N.newLinkedListMultimap(len);
            }
        };

        /** The Constant SET_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super SetMultimap> SET_MULTIMAP_FACTORY = new IntFunction<SetMultimap>() {
            @Override
            public SetMultimap apply(int len) {
                return N.newSetMultimap(len);
            }
        };

        /**
         * Instantiates a new factory.
         */
        protected Factory() {
            // for extention
        }

        /**
         * Of boolean array.
         *
         * @return
         */
        public static IntFunction<boolean[]> ofBooleanArray() {
            return BOOLEAN_ARRAY;
        }

        /**
         * Of char array.
         *
         * @return
         */
        public static IntFunction<char[]> ofCharArray() {
            return CHAR_ARRAY;
        }

        /**
         * Of byte array.
         *
         * @return
         */
        public static IntFunction<byte[]> ofByteArray() {
            return BYTE_ARRAY;
        }

        /**
         * Of short array.
         *
         * @return
         */
        public static IntFunction<short[]> ofShortArray() {
            return SHORT_ARRAY;
        }

        /**
         * Of int array.
         *
         * @return
         */
        public static IntFunction<int[]> ofIntArray() {
            return INT_ARRAY;
        }

        /**
         * Of long array.
         *
         * @return
         */
        public static IntFunction<long[]> ofLongArray() {
            return LONG_ARRAY;
        }

        /**
         * Of float array.
         *
         * @return
         */
        public static IntFunction<float[]> ofFloatArray() {
            return FLOAT_ARRAY;
        }

        /**
         * Of double array.
         *
         * @return
         */
        public static IntFunction<double[]> ofDoubleArray() {
            return DOUBLE_ARRAY;
        }

        /**
         * Of string array.
         *
         * @return
         */
        public static IntFunction<String[]> ofStringArray() {
            return STRING_ARRAY;
        }

        /**
         * Of object array.
         *
         * @return
         */
        public static IntFunction<Object[]> ofObjectArray() {
            return OBJECT_ARRAY;
        }

        /**
         * Of boolean list.
         *
         * @return
         */
        public static IntFunction<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return
         */
        public static IntFunction<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return
         */
        public static IntFunction<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return
         */
        public static IntFunction<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return
         */
        public static IntFunction<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return
         */
        public static IntFunction<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return
         */
        public static IntFunction<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return
         */
        public static IntFunction<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<List<T>> ofList() {
            return (IntFunction) LIST_FACTORY;
        }

        /**
         * Of linked list.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
            return (IntFunction) LINKED_LIST_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofSet() {
            return (IntFunction) SET_FACTORY;
        }

        /**
         * Of linked hash set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofLinkedHashSet() {
            return (IntFunction) LINKED_HASH_SET_FACTORY;
        }

        /**
         * Of sorted set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of navigable set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of tree set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Queue<T>> ofQueue() {
            return (IntFunction) QUEUE_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Deque<T>> ofDeque() {
            return (IntFunction) DEQUE_FACTORY;
        }

        /**
         * Of array deque.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
            return (IntFunction) ARRAY_DEQUE_FACTORY;
        }

        /**
         * Of linked blocking queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
        }

        /**
         * Of concurrent linked queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
        }

        /**
         * Of priority queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
            return (IntFunction) PRIORITY_QUEUE_FACTORY;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofMap() {
            return (IntFunction) MAP_FACTORY;
        }

        /**
         * Of linked hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<Map<K, V>> ofLinkedHashMap() {
            return (IntFunction) LINKED_HASH_MAP_FACTORY;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (IntFunction) IDENTITY_HASH_MAP_FACTORY;
        }

        /**
         * Of sorted map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<SortedMap<K, V>> ofSortedMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of navigable map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<NavigableMap<K, V>> ofNavigableMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of tree map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<TreeMap<K, V>> ofTreeMap() {
            return (IntFunction) TREE_MAP_FACTORY;
        }

        /**
         * Of concurrent map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Of concurrent hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (IntFunction) CONCURRENT_HASH_MAP_FACTORY;
        }

        /**
         * Of bi map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<BiMap<K, V>> ofBiMap() {
            return (IntFunction) BI_MAP_FACTORY;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Multiset<T>> ofMultiset() {
            return (IntFunction) MULTISET_FACTORY;
        }

        /**
         * Of long multiset.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LongMultiset<T>> ofLongMultiset() {
            return (IntFunction) LONG_MULTISET_FACTORY;
        }

        /**
         * Of list multimap.
         *
         * @param <K> the key type
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<ListMultimap<K, E>> ofListMultimap() {
            return (IntFunction) LIST_MULTIMAP_FACTORY;
        }

        /**
         * Of set multimap.
         *
         * @param <K> the key type
         * @param <E>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<SetMultimap<K, E>> ofSetMultimap() {
            return (IntFunction) SET_MULTIMAP_FACTORY;
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @param supplier
         * @return
         */
        @Deprecated
        public static <T, C extends Collection<T>> IntFunction<? extends C> single(final IntFunction<? extends C> supplier) {
            return new IntFunction<C>() {
                private C c = null;

                @Override
                public C apply(int t) {
                    if (c == null) {
                        c = supplier.apply(t);
                    } else {
                        c.clear();
                    }

                    return c;
                }
            };
        }
    }

    /**
     * The Class IntFunctions.
     */
    public static final class IntFunctions extends Factory {

        /**
         * Instantiates a new int functions.
         */
        protected IntFunctions() {
            // for extention.
        }
    }

    /**
     * The Class Suppliers.
     */
    public static final class Suppliers {

        /** The Constant UUID. */
        private static final Supplier<String> UUID = new Supplier<String>() {
            @Override
            public String get() {
                return N.uuid();
            }
        };

        /** The Constant GUID. */
        private static final Supplier<String> GUID = new Supplier<String>() {
            @Override
            public String get() {
                return N.guid();
            }
        };

        /** The Constant EMPTY_BOOLEAN_ARRAY. */
        private static final Supplier<boolean[]> EMPTY_BOOLEAN_ARRAY = new Supplier<boolean[]>() {
            @Override
            public boolean[] get() {
                return N.EMPTY_BOOLEAN_ARRAY;
            }
        };

        /** The Constant EMPTY_CHAR_ARRAY. */
        private static final Supplier<char[]> EMPTY_CHAR_ARRAY = new Supplier<char[]>() {
            @Override
            public char[] get() {
                return N.EMPTY_CHAR_ARRAY;
            }
        };

        /** The Constant EMPTY_BYTE_ARRAY. */
        private static final Supplier<byte[]> EMPTY_BYTE_ARRAY = new Supplier<byte[]>() {
            @Override
            public byte[] get() {
                return N.EMPTY_BYTE_ARRAY;
            }
        };

        /** The Constant EMPTY_SHORT_ARRAY. */
        private static final Supplier<short[]> EMPTY_SHORT_ARRAY = new Supplier<short[]>() {
            @Override
            public short[] get() {
                return N.EMPTY_SHORT_ARRAY;
            }
        };

        /** The Constant EMPTY_INT_ARRAY. */
        private static final Supplier<int[]> EMPTY_INT_ARRAY = new Supplier<int[]>() {
            @Override
            public int[] get() {
                return N.EMPTY_INT_ARRAY;
            }
        };

        /** The Constant EMPTY_LONG_ARRAY. */
        private static final Supplier<long[]> EMPTY_LONG_ARRAY = new Supplier<long[]>() {
            @Override
            public long[] get() {
                return N.EMPTY_LONG_ARRAY;
            }
        };

        /** The Constant EMPTY_FLOAT_ARRAY. */
        private static final Supplier<float[]> EMPTY_FLOAT_ARRAY = new Supplier<float[]>() {
            @Override
            public float[] get() {
                return N.EMPTY_FLOAT_ARRAY;
            }
        };

        /** The Constant EMPTY_DOUBLE_ARRAY. */
        private static final Supplier<double[]> EMPTY_DOUBLE_ARRAY = new Supplier<double[]>() {
            @Override
            public double[] get() {
                return N.EMPTY_DOUBLE_ARRAY;
            }
        };

        /** The Constant EMPTY_STRING_ARRAY. */
        private static final Supplier<String[]> EMPTY_STRING_ARRAY = new Supplier<String[]>() {
            @Override
            public String[] get() {
                return N.EMPTY_STRING_ARRAY;
            }
        };

        /** The Constant EMPTY_OBJECT_ARRAY. */
        private static final Supplier<Object[]> EMPTY_OBJECT_ARRAY = new Supplier<Object[]>() {
            @Override
            public Object[] get() {
                return N.EMPTY_OBJECT_ARRAY;
            }
        };

        /** The Constant BOOLEAN_LIST. */
        private static final Supplier<BooleanList> BOOLEAN_LIST = new Supplier<BooleanList>() {
            @Override
            public BooleanList get() {
                return new BooleanList();
            }
        };

        /** The Constant CHAR_LIST. */
        private static final Supplier<CharList> CHAR_LIST = new Supplier<CharList>() {
            @Override
            public CharList get() {
                return new CharList();
            }
        };

        /** The Constant BYTE_LIST. */
        private static final Supplier<ByteList> BYTE_LIST = new Supplier<ByteList>() {
            @Override
            public ByteList get() {
                return new ByteList();
            }
        };

        /** The Constant SHORT_LIST. */
        private static final Supplier<ShortList> SHORT_LIST = new Supplier<ShortList>() {
            @Override
            public ShortList get() {
                return new ShortList();
            }
        };

        /** The Constant INT_LIST. */
        private static final Supplier<IntList> INT_LIST = new Supplier<IntList>() {
            @Override
            public IntList get() {
                return new IntList();
            }
        };

        /** The Constant LONG_LIST. */
        private static final Supplier<LongList> LONG_LIST = new Supplier<LongList>() {
            @Override
            public LongList get() {
                return new LongList();
            }
        };

        /** The Constant FLOAT_LIST. */
        private static final Supplier<FloatList> FLOAT_LIST = new Supplier<FloatList>() {
            @Override
            public FloatList get() {
                return new FloatList();
            }
        };

        /** The Constant DOUBLE_LIST. */
        private static final Supplier<DoubleList> DOUBLE_LIST = new Supplier<DoubleList>() {
            @Override
            public DoubleList get() {
                return new DoubleList();
            }
        };

        /** The Constant LIST. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super List> LIST = new Supplier<List>() {
            @Override
            public List get() {
                return new ArrayList();
            }
        };

        /** The Constant LINKED_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedList> LINKED_LIST = new Supplier<LinkedList>() {
            @Override
            public LinkedList get() {
                return new LinkedList();
            }
        };

        /** The Constant SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> SET = new Supplier<Set>() {
            @Override
            public Set get() {
                return N.newHashSet();
            }
        };

        /** The Constant LINKED_HASH_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Set> LINKED_HASH_SET = new Supplier<Set>() {
            @Override
            public Set get() {
                return N.newLinkedHashSet();
            }
        };

        /** The Constant TREE_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeSet> TREE_SET = new Supplier<TreeSet>() {
            @Override
            public TreeSet get() {
                return new TreeSet();
            }
        };

        /** The Constant QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Queue> QUEUE = new Supplier<Queue>() {
            @Override
            public Queue get() {
                return new LinkedList();
            }
        };

        /** The Constant DEQUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Deque> DEQUE = new Supplier<Deque>() {
            @Override
            public Deque get() {
                return new LinkedList();
            }
        };

        /** The Constant ARRAY_DEQUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ArrayDeque> ARRAY_DEQUE = new Supplier<ArrayDeque>() {
            @Override
            public ArrayDeque get() {
                return new ArrayDeque();
            }
        };

        /** The Constant LINKED_BLOCKING_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedBlockingQueue> LINKED_BLOCKING_QUEUE = new Supplier<LinkedBlockingQueue>() {
            @Override
            public LinkedBlockingQueue get() {
                return new LinkedBlockingQueue();
            }
        };

        /** The Constant CONCURRENT_LINKED_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentLinkedQueue> CONCURRENT_LINKED_QUEUE = new Supplier<ConcurrentLinkedQueue>() {
            @Override
            public ConcurrentLinkedQueue get() {
                return new ConcurrentLinkedQueue();
            }
        };

        /** The Constant PRIORITY_QUEUE. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super PriorityQueue> PRIORITY_QUEUE = new Supplier<PriorityQueue>() {
            @Override
            public PriorityQueue get() {
                return new PriorityQueue();
            }
        };

        /** The Constant MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Map> MAP = new Supplier<Map>() {
            @Override
            public Map get() {
                return N.newHashMap();
            }
        };

        /** The Constant LINKED_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Map> LINKED_HASH_MAP = new Supplier<Map>() {
            @Override
            public Map get() {
                return N.newLinkedHashMap();
            }
        };

        /** The Constant IDENTITY_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super IdentityHashMap> IDENTITY_HASH_MAP = new Supplier<IdentityHashMap>() {
            @Override
            public IdentityHashMap get() {
                return new IdentityHashMap();
            }
        };

        /** The Constant TREE_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super TreeMap> TREE_MAP = new Supplier<TreeMap>() {
            @Override
            public TreeMap get() {
                return new TreeMap();
            }
        };

        /** The Constant CONCURRENT_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ConcurrentHashMap> CONCURRENT_HASH_MAP = new Supplier<ConcurrentHashMap>() {
            @Override
            public ConcurrentHashMap get() {
                return new ConcurrentHashMap();
            }
        };

        /** The Constant BI_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super BiMap> BI_MAP = new Supplier<BiMap>() {
            @Override
            public BiMap get() {
                return new BiMap();
            }
        };

        /** The Constant MULTISET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super Multiset> MULTISET = new Supplier<Multiset>() {
            @Override
            public Multiset get() {
                return new Multiset();
            }
        };

        /** The Constant LONG_MULTISET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LongMultiset> LONG_MULTISET = new Supplier<LongMultiset>() {
            @Override
            public LongMultiset get() {
                return new LongMultiset();
            }
        };

        /** The Constant LIST_MULTIMAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super ListMultimap> LIST_MULTIMAP = new Supplier<ListMultimap>() {
            @Override
            public ListMultimap get() {
                return N.newListMultimap();
            }
        };

        /** The Constant SET_MULTIMAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super SetMultimap> SET_MULTIMAP = new Supplier<SetMultimap>() {
            @Override
            public SetMultimap get() {
                return N.newSetMultimap();
            }
        };

        /** The Constant STRING_BUILDER. */
        private static final Supplier<StringBuilder> STRING_BUILDER = new Supplier<StringBuilder>() {
            @Override
            public StringBuilder get() {
                return new StringBuilder();
            }
        };

        /**
         * Instantiates a new suppliers.
         */
        protected Suppliers() {
            // for extention.
        }

        /**
         * Returns a supplier that always supplies {@code instance}.
         *
         * @param <T>
         * @param instance
         * @return
         */
        public static <T> Supplier<T> ofInstance(final T instance) {
            return new Supplier<T>() {
                @Override
                public T get() {
                    return instance;
                }
            };
        }

        /**
         *
         * @return
         */
        public static Supplier<String> ofUUID() {
            return UUID;
        }

        /**
         *
         * @return
         */
        public static Supplier<String> ofGUID() {
            return GUID;
        }

        /**
         * Of empty boolean array.
         *
         * @return
         */
        public static Supplier<boolean[]> ofEmptyBooleanArray() {
            return EMPTY_BOOLEAN_ARRAY;
        }

        /**
         * Of empty char array.
         *
         * @return
         */
        public static Supplier<char[]> ofEmptyCharArray() {
            return EMPTY_CHAR_ARRAY;
        }

        /**
         * Of empty byte array.
         *
         * @return
         */
        public static Supplier<byte[]> ofEmptyByteArray() {
            return EMPTY_BYTE_ARRAY;
        }

        /**
         * Of empty short array.
         *
         * @return
         */
        public static Supplier<short[]> ofEmptyShortArray() {
            return EMPTY_SHORT_ARRAY;
        }

        /**
         * Of empty int array.
         *
         * @return
         */
        public static Supplier<int[]> ofEmptyIntArray() {
            return EMPTY_INT_ARRAY;
        }

        /**
         * Of empty long array.
         *
         * @return
         */
        public static Supplier<long[]> ofEmptyLongArray() {
            return EMPTY_LONG_ARRAY;
        }

        /**
         * Of empty float array.
         *
         * @return
         */
        public static Supplier<float[]> ofEmptyFloatArray() {
            return EMPTY_FLOAT_ARRAY;
        }

        /**
         * Of empty double array.
         *
         * @return
         */
        public static Supplier<double[]> ofEmptyDoubleArray() {
            return EMPTY_DOUBLE_ARRAY;
        }

        /**
         * Of empty string array.
         *
         * @return
         */
        public static Supplier<String[]> ofEmptyStringArray() {
            return EMPTY_STRING_ARRAY;
        }

        /**
         * Of empty object array.
         *
         * @return
         */
        public static Supplier<Object[]> ofEmptyObjectArray() {
            return EMPTY_OBJECT_ARRAY;
        }

        /**
         * Of boolean list.
         *
         * @return
         */
        public static Supplier<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return
         */
        public static Supplier<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return
         */
        public static Supplier<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return
         */
        public static Supplier<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return
         */
        public static Supplier<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return
         */
        public static Supplier<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return
         */
        public static Supplier<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return
         */
        public static Supplier<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<List<T>> ofList() {
            return (Supplier) LIST;
        }

        /**
         * Of linked list.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedList<T>> ofLinkedList() {
            return (Supplier) LINKED_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofSet() {
            return (Supplier) SET;
        }

        /**
         * Of linked hash set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofLinkedHashSet() {
            return (Supplier) LINKED_HASH_SET;
        }

        /**
         * Of sorted set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<SortedSet<T>> ofSortedSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of navigable set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of tree set.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<TreeSet<T>> ofTreeSet() {
            return (Supplier) TREE_SET;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Queue<T>> ofQueue() {
            return (Supplier) QUEUE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Deque<T>> ofDeque() {
            return (Supplier) DEQUE;
        }

        /**
         * Of array deque.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
            return (Supplier) ARRAY_DEQUE;
        }

        /**
         * Of linked blocking queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (Supplier) LINKED_BLOCKING_QUEUE;
        }

        /**
         * Of concurrent linked queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (Supplier) CONCURRENT_LINKED_QUEUE;
        }

        /**
         * Of priority queue.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
            return (Supplier) PRIORITY_QUEUE;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofMap() {
            return (Supplier) MAP;
        }

        /**
         * Of linked hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<Map<K, V>> ofLinkedHashMap() {
            return (Supplier) LINKED_HASH_MAP;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<IdentityHashMap<K, V>> ofIdentityHashMap() {
            return (Supplier) IDENTITY_HASH_MAP;
        }

        /**
         * Of sorted map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<SortedMap<K, V>> ofSortedMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of navigable map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<NavigableMap<K, V>> ofNavigableMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of tree map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<TreeMap<K, V>> ofTreeMap() {
            return (Supplier) TREE_MAP;
        }

        /**
         * Of concurrent map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<BiMap<K, V>> ofBiMap() {
            return (Supplier) BI_MAP;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset() {
            return (Supplier) MULTISET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset(final Class<? extends Map> valueMapType) {
            return new Supplier<Multiset<T>>() {
                @Override
                public Multiset<T> get() {
                    return N.newMultiset(valueMapType);
                }
            };
        }

        public static <T> Supplier<Multiset<T>> ofMultiset(final Supplier<? extends Map<T, ?>> mapSupplier) {
            return new Supplier<Multiset<T>>() {
                @Override
                public Multiset<T> get() {
                    return N.newMultiset(mapSupplier);
                }
            };
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LongMultiset<T>> ofLongMultiset() {
            return (Supplier) LONG_MULTISET;
        }

        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LongMultiset<T>> ofLongMultiset(final Class<? extends Map> valueMapType) {
            return new Supplier<LongMultiset<T>>() {
                @Override
                public LongMultiset<T> get() {
                    return new LongMultiset<>(valueMapType);
                }
            };
        }

        public static <T> Supplier<LongMultiset<T>> ofLongMultiset(final Supplier<? extends Map<T, ?>> mapSupplier) {
            return new Supplier<LongMultiset<T>>() {
                @Override
                public LongMultiset<T> get() {
                    return new LongMultiset<>(mapSupplier);
                }
            };
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap() {
            return (Supplier) LIST_MULTIMAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType) {
            return new Supplier<ListMultimap<K, E>>() {
                @Override
                public ListMultimap<K, E> get() {
                    return N.newListMultimap(mapType);
                }
            };
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Class<? extends Map> mapType, final Class<? extends List> valueType) {
            return new Supplier<ListMultimap<K, E>>() {
                @Override
                public ListMultimap<K, E> get() {
                    return N.newListMultimap(mapType, valueType);
                }
            };
        }

        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap(final Supplier<? extends Map<K, List<E>>> mapSupplier,
                final Supplier<? extends List<E>> valueSupplier) {
            return new Supplier<ListMultimap<K, E>>() {
                @Override
                public ListMultimap<K, E> get() {
                    return N.newListMultimap(mapSupplier, valueSupplier);
                }
            };
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap() {
            return (Supplier) SET_MULTIMAP;
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType) {
            return new Supplier<SetMultimap<K, E>>() {
                @Override
                public SetMultimap<K, E> get() {
                    return N.newSetMultimap(mapType);
                }
            };
        }

        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Class<? extends Map> mapType, final Class<? extends Set> valueType) {
            return new Supplier<SetMultimap<K, E>>() {
                @Override
                public SetMultimap<K, E> get() {
                    return N.newSetMultimap(mapType, valueType);
                }
            };
        }

        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap(final Supplier<? extends Map<K, Set<E>>> mapSupplier,
                final Supplier<? extends Set<E>> valueSupplier) {
            return new Supplier<SetMultimap<K, E>>() {
                @Override
                public SetMultimap<K, E> get() {
                    return N.newSetMultimap(mapSupplier, valueSupplier);
                }
            };
        }

        public static <K, E, V extends Collection<E>> Supplier<Multimap<K, E, V>> ofMultimap(final Supplier<? extends Map<K, V>> mapSupplier,
                final Supplier<? extends V> valueSupplier) {
            return new Supplier<Multimap<K, E, V>>() {
                @Override
                public Multimap<K, E, V> get() {
                    return N.newMultimap(mapSupplier, valueSupplier);
                }
            };
        }

        /**
         * Of string builder.
         *
         * @return
         */
        public static Supplier<StringBuilder> ofStringBuilder() {
            return STRING_BUILDER;
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @param supplier
         * @return
         */
        @Deprecated
        public static <T, C extends Collection<T>> Supplier<? extends C> single(final Supplier<? extends C> supplier) {
            return new Supplier<C>() {
                private C c = null;

                @Override
                public C get() {
                    if (c == null) {
                        c = supplier.get();
                    } else {
                        c.clear();
                    }

                    return c;
                }
            };
        }
    }

    /**
     * The Class Predicates.
     */
    public static final class Predicates {

        /**
         * Instantiates a new predicates.
         */
        protected Predicates() {
            // for extention.
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param predicate
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> indexed(final IndexedPredicate<T> predicate) {
            N.checkArgNotNull(predicate);

            return new Predicate<T>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public boolean test(T t) {
                    return predicate.test(idx.getAndIncrement(), t);
                }
            };
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> distinct() {
            return new Predicate<T>() {
                private final Set<Object> set = N.newHashSet();

                @Override
                public boolean test(T value) {
                    return set.add(value);
                }
            };
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param mapper
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> distinctBy(final Function<? super T, ?> mapper) {
            return new Predicate<T>() {
                private final Set<Object> set = N.newHashSet();

                @Override
                public boolean test(T value) {
                    return set.add(mapper.apply(value));
                }
            };
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @return
         */
        @Beta
        @Stateful
        public static <T> Predicate<T> concurrentDistinct() {
            return new Predicate<T>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(T value) {
                    return map.put(value, NONE) == null;
                }
            };
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param mapper
         * @return
         */
        @Beta
        @Stateful
        public static <T> Predicate<T> concurrentDistinctBy(final Function<? super T, ?> mapper) {
            return new Predicate<T>() {
                private final Map<Object, Object> map = new ConcurrentHashMap<>();

                @Override
                public boolean test(T value) {
                    return map.put(mapper.apply(value), NONE) == null;
                }
            };
        }

        /**
         * Returns a stateful <code>Predicate</code>. Don't save or cache for reuse or use it in parallel stream.
         * Remove the continuous repeat elements.
         *
         * @param <T>
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Predicate<T> skipRepeats() {
            return new Predicate<T>() {
                private T pre = (T) NONE;

                @Override
                public boolean test(T value) {
                    boolean res = pre == NONE || N.equals(value, pre) == false;
                    pre = value;
                    return res;
                }
            };
        }

        /**
         * {@code true/false} are repeatedly returned after each specified duration.
         *
         * @param <T>
         * @param periodInMillis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        public static <T> Predicate<T> invertedByDuration(final long periodInMillis, final MutableBoolean cancellationFlag) {
            final MutableBoolean switcher = MutableBoolean.of(true);

            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (cancellationFlag.isTrue()) {
                        this.cancel();
                    }

                    switcher.invert();
                }
            };

            timer.schedule(task, periodInMillis, periodInMillis);

            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return switcher.value();
                }
            };
        }

        /**
         * {@code true/false} are repeatedly returned after each specified duration.
         *
         * @param <T>
         * @param periodInMillis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @param update called at the beginning of each duration.
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        public static <T> Predicate<T> invertedByDuration(final long periodInMillis, final MutableBoolean cancellationFlag, final java.lang.Runnable update) {
            final MutableBoolean switcher = MutableBoolean.of(true);

            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (cancellationFlag.isTrue()) {
                        this.cancel();
                    }

                    switcher.invert();
                    update.run();
                }
            };

            timer.schedule(task, periodInMillis, periodInMillis);

            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return switcher.value();
                }
            };
        }

        /**
         * {@code true/false} are repeatedly returned after each specified duration.
         *
         * @param <T>
         * @param delayInMillis
         * @param periodInMillis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        public static <T> Predicate<T> invertedByDuration(final long delayInMillis, final long periodInMillis, final MutableBoolean cancellationFlag) {
            final MutableBoolean switcher = MutableBoolean.of(true);

            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (cancellationFlag.isTrue()) {
                        this.cancel();
                    }

                    switcher.invert();
                }
            };

            timer.schedule(task, delayInMillis, periodInMillis);

            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return switcher.value();
                }
            };
        }

        /**
         * {@code true/false} are repeatedly returned after each specified duration.
         *
         * @param <T>
         * @param delayInMillis
         * @param periodInMillis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @param update called at the beginning of each duration.
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        public static <T> Predicate<T> invertedByDuration(final long delayInMillis, final long periodInMillis, final MutableBoolean cancellationFlag,
                final java.lang.Runnable update) {
            final MutableBoolean switcher = MutableBoolean.of(true);

            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    if (cancellationFlag.isTrue()) {
                        this.cancel();
                    }

                    switcher.invert();
                    update.run();
                }
            };

            timer.schedule(task, delayInMillis, periodInMillis);

            return new Predicate<T>() {
                @Override
                public boolean test(T t) {
                    return switcher.value();
                }
            };
        }
    }

    /**
     * The Class BiPredicates.
     */
    public static final class BiPredicates {

        /** The Constant ALWAYS_TRUE. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_TRUE = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return true;
            }
        };

        /** The Constant ALWAYS_FALSE. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate ALWAYS_FALSE = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return false;
            }
        };

        /** The Constant EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate EQUAL = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return N.equals(t, u);
            }
        };

        /** The Constant NOT_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate NOT_EQUAL = new BiPredicate() {
            @Override
            public boolean test(Object t, Object u) {
                return !N.equals(t, u);
            }
        };

        /** The Constant GREATER_THAN. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_THAN = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return N.compare(t, u) > 0;
            }
        };

        /** The Constant GREATER_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> GREATER_EQUAL = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return N.compare(t, u) >= 0;
            }
        };

        /** The Constant LESS_THAN. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_THAN = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return N.compare(t, u) < 0;
            }
        };

        /** The Constant LESS_EQUAL. */
        @SuppressWarnings("rawtypes")
        private static final BiPredicate<? extends Comparable, ? extends Comparable> LESS_EQUAL = new BiPredicate<Comparable, Comparable>() {
            @Override
            public boolean test(Comparable t, Comparable u) {
                return N.compare(t, u) <= 0;
            }
        };

        /**
         * Instantiates a new bi predicates.
         */
        protected BiPredicates() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiPredicate<T, U> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiPredicate<T, U> alwaysFalse() {
            return ALWAYS_FALSE;
        }

        /**
         * Returns a stateful <code>BiPredicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         *
         * @param <T>
         * @param <U>
         * @param predicate
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, U> BiPredicate<T, U> indexed(final IndexedBiPredicate<T, U> predicate) {
            N.checkArgNotNull(predicate);

            return new BiPredicate<T, U>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public boolean test(T t, U u) {
                    return predicate.test(t, idx.getAndIncrement(), u);
                }
            };
        }
    }

    /**
     * The Class TriPredicates.
     */
    public static final class TriPredicates {

        /** The Constant ALWAYS_TRUE. */
        @SuppressWarnings("rawtypes")
        private static final TriPredicate ALWAYS_TRUE = new TriPredicate() {
            @Override
            public boolean test(Object a, Object b, Object c) {
                return true;
            }
        };

        /** The Constant ALWAYS_FALSE. */
        @SuppressWarnings({ "rawtypes", "hiding" })
        private static final TriPredicate ALWAYS_FALSE = new TriPredicate() {
            @Override
            public boolean test(Object a, Object b, Object c) {
                return false;
            }
        };

        /**
         * Instantiates a new tri predicates.
         */
        protected TriPredicates() {
            // for extention.
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @return
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         *
         * @param <A>
         * @param <B>
         * @param <C>
         * @return
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysFalse() {
            return ALWAYS_FALSE;
        }

    }

    /**
     * The Class Consumers.
     */
    public static final class Consumers {

        /**
         * Instantiates a new consumers.
         */
        protected Consumers() {
            // for extention.
        }

        /**
         * Returns a stateful <code>BiPredicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <T>
         * @param action
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T> Consumer<T> indexed(final IndexedConsumer<T> action) {
            N.checkArgNotNull(action);

            return new Consumer<T>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public void accept(T t) {
                    action.accept(idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class BiConsumers.
     */
    public static final class BiConsumers {

        /** The Constant DO_NOTHING. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer DO_NOTHING = new BiConsumer() {
            @Override
            public void accept(Object t, Object u) {
                // do nothing.
            }
        };

        /** The Constant ADD. */
        private static final BiConsumer<Collection<Object>, Object> ADD = new BiConsumer<Collection<Object>, Object>() {
            @Override
            public void accept(Collection<Object> t, Object u) {
                t.add(u);
            }
        };

        /** The Constant ADD_ALL. */
        private static final BiConsumer<Collection<Object>, Collection<Object>> ADD_ALL = new BiConsumer<Collection<Object>, Collection<Object>>() {
            @Override
            public void accept(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
            }
        };

        /** The Constant ADD_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer<PrimitiveList, PrimitiveList> ADD_ALL_2 = new BiConsumer<PrimitiveList, PrimitiveList>() {
            @Override
            public void accept(PrimitiveList t, PrimitiveList u) {
                t.addAll(u);
            }
        };

        /** The Constant REMOVE. */
        private static final BiConsumer<Collection<Object>, Object> REMOVE = new BiConsumer<Collection<Object>, Object>() {
            @Override
            public void accept(Collection<Object> t, Object u) {
                t.remove(u);
            }
        };

        /** The Constant REMOVE_ALL. */
        private static final BiConsumer<Collection<Object>, Collection<Object>> REMOVE_ALL = new BiConsumer<Collection<Object>, Collection<Object>>() {
            @Override
            public void accept(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
            }
        };

        /** The Constant REMOVE_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiConsumer<PrimitiveList, PrimitiveList> REMOVE_ALL_2 = new BiConsumer<PrimitiveList, PrimitiveList>() {
            @Override
            public void accept(PrimitiveList t, PrimitiveList u) {
                t.removeAll(u);
            }
        };

        /** The Constant PUT. */
        private static final BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>> PUT = new BiConsumer<Map<Object, Object>, Map.Entry<Object, Object>>() {
            @Override
            public void accept(Map<Object, Object> t, Map.Entry<Object, Object> u) {
                t.put(u.getKey(), u.getValue());
            }
        };

        /** The Constant PUT_ALL. */
        private static final BiConsumer<Map<Object, Object>, Map<Object, Object>> PUT_ALL = new BiConsumer<Map<Object, Object>, Map<Object, Object>>() {
            @Override
            public void accept(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
            }
        };

        /** The Constant REMOVE_BY_KEY. */
        private static final BiConsumer<Map<Object, Object>, Object> REMOVE_BY_KEY = new BiConsumer<Map<Object, Object>, Object>() {
            @Override
            public void accept(Map<Object, Object> t, Object u) {
                t.remove(u);
            }
        };

        /** The Constant MERGE. */
        private static final BiConsumer<Joiner, Joiner> MERGE = new BiConsumer<Joiner, Joiner>() {
            @Override
            public void accept(Joiner t, Joiner u) {
                t.merge(u);
            }
        };

        /** The Constant APPEND. */
        private static final BiConsumer<StringBuilder, Object> APPEND = new BiConsumer<StringBuilder, Object>() {
            @Override
            public void accept(StringBuilder t, Object u) {
                t.append(u);
            }
        };

        /**
         * Instantiates a new bi consumers.
         */
        protected BiConsumers() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiConsumer<T, U> doNothing() {
            return DO_NOTHING;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
            return (BiConsumer<C, T>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
            return (BiConsumer<C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofAddAlll() {
            return (BiConsumer<T, T>) ADD_ALL_2;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
            return (BiConsumer<C, T>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
            return (BiConsumer<C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofRemoveAlll() {
            return (BiConsumer<T, T>) REMOVE_ALL_2;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param <E>
         * @return
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
            return (BiConsumer<M, E>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
            return (BiConsumer<M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
            return (BiConsumer<M, K>) REMOVE_BY_KEY;
        }

        /**
         *
         * @return
         */
        public static BiConsumer<Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> BiConsumer<StringBuilder, T> ofAppend() {
            return (BiConsumer<StringBuilder, T>) APPEND;
        }

        /**
         * Returns a stateful <code>BiPredicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         *
         * @param <U>
         * @param <T>
         * @param action
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <U, T> BiConsumer<U, T> indexed(final IndexedBiConsumer<U, T> action) {
            N.checkArgNotNull(action);

            return new BiConsumer<U, T>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public void accept(U u, T t) {
                    action.accept(u, idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class TriConsumers.
     */
    public static final class TriConsumers {

        /**
         * Instantiates a new tri consumers.
         */
        protected TriConsumers() {
            // for extention.
        }
    }

    /**
     * The Class Functions.
     */
    public static final class Functions {

        /**
         * Instantiates a new functions.
         */
        protected Functions() {
            // for extention.
        }

        /**
         * Returns a stateful <code>Function</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         *
         * @param <T>
         * @param <R>
         * @param func
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <T, R> Function<T, R> indexed(final IndexedFunction<T, R> func) {
            N.checkArgNotNull(func);

            return new Function<T, R>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public R apply(T t) {
                    return func.apply(idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class BiFunctions.
     */
    public static final class BiFunctions {

        /** The Constant RETURN_FIRST. */
        private static final BiFunction<Object, Object, Object> RETURN_FIRST = new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object t, Object u) {
                return t;
            }
        };

        /** The Constant RETURN_SECOND. */
        private static final BiFunction<Object, Object, Object> RETURN_SECOND = new BiFunction<Object, Object, Object>() {
            @Override
            public Object apply(Object t, Object u) {
                return u;
            }
        };

        /** The Constant ADD. */
        private static final BiFunction<Collection<Object>, Object, Collection<Object>> ADD = new BiFunction<Collection<Object>, Object, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Object u) {
                t.add(u);
                return t;
            }
        };

        /** The Constant ADD_ALL. */
        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> ADD_ALL = new BiFunction<Collection<Object>, Collection<Object>, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
                return t;
            }
        };

        /** The Constant ADD_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> ADD_ALL_2 = new BiFunction<PrimitiveList, PrimitiveList, PrimitiveList>() {
            @Override
            public PrimitiveList apply(PrimitiveList t, PrimitiveList u) {
                t.addAll(u);
                return t;
            }
        };

        /** The Constant REMOVE. */
        private static final BiFunction<Collection<Object>, Object, Collection<Object>> REMOVE = new BiFunction<Collection<Object>, Object, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Object u) {
                t.remove(u);
                return t;
            }
        };

        /** The Constant REMOVE_ALL. */
        private static final BiFunction<Collection<Object>, Collection<Object>, Collection<Object>> REMOVE_ALL = new BiFunction<Collection<Object>, Collection<Object>, Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
                return t;
            }
        };

        /** The Constant REMOVE_ALL_2. */
        @SuppressWarnings("rawtypes")
        private static final BiFunction<PrimitiveList, PrimitiveList, PrimitiveList> REMOVE_ALL_2 = new BiFunction<PrimitiveList, PrimitiveList, PrimitiveList>() {
            @Override
            public PrimitiveList apply(PrimitiveList t, PrimitiveList u) {
                t.removeAll(u);
                return t;
            }
        };

        /** The Constant PUT. */
        private static final BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>> PUT = new BiFunction<Map<Object, Object>, Map.Entry<Object, Object>, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map.Entry<Object, Object> u) {
                t.put(u.getKey(), u.getValue());
                return t;
            }
        };

        /** The Constant PUT_ALL. */
        private static final BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>> PUT_ALL = new BiFunction<Map<Object, Object>, Map<Object, Object>, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
                return t;
            }
        };

        /** The Constant REMOVE_BY_KEY. */
        private static final BiFunction<Map<Object, Object>, Object, Map<Object, Object>> REMOVE_BY_KEY = new BiFunction<Map<Object, Object>, Object, Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Object u) {
                t.remove(u);
                return t;
            }
        };

        /** The Constant MERGE. */
        private static final BiFunction<Joiner, Joiner, Joiner> MERGE = new BiFunction<Joiner, Joiner, Joiner>() {
            @Override
            public Joiner apply(Joiner t, Joiner u) {
                return t.merge(u);
            }
        };

        /** The Constant APPEND. */
        private static final BiFunction<StringBuilder, Object, StringBuilder> APPEND = new BiFunction<StringBuilder, Object, StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, Object u) {
                return t.append(u);
            }
        };

        /**
         * Instantiates a new bi functions.
         */
        protected BiFunctions() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiFunction<T, U, T> returnFirst() {
            return (BiFunction<T, U, T>) RETURN_FIRST;
        }

        /**
         *
         * @param <T>
         * @param <U>
         * @return
         */
        public static <T, U> BiFunction<T, U, U> returnSecond() {
            return (BiFunction<T, U, U>) RETURN_SECOND;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
            return (BiFunction<C, T, C>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
            return (BiFunction<C, C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofAddAlll() {
            return (BiFunction<T, T, T>) ADD_ALL_2;
        }

        /**
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
            return (BiFunction<C, T, C>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
            return (BiFunction<C, C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofRemoveAlll() {
            return (BiFunction<T, T, T>) REMOVE_ALL_2;
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param <E>
         * @return
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
            return (BiFunction<M, E, M>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
            return (BiFunction<M, M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param <U>
         * @return
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, K, M> ofRemoveByKey() {
            return (BiFunction<M, K, M>) REMOVE_BY_KEY;
        }

        /**
         *
         * @return
         */
        public static BiFunction<Joiner, Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
            return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
        }

        /**
         * Returns a stateful <code>BiPredicate</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @param <U>
         * @param <T>
         * @param <R>
         * @param func
         * @return
         */
        @Beta
        @SequentialOnly
        @Stateful
        public static <U, T, R> BiFunction<U, T, R> indexed(final IndexedBiFunction<U, T, R> func) {
            N.checkArgNotNull(func);

            return new BiFunction<U, T, R>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public R apply(U u, T t) {
                    return func.apply(u, idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class TriFunctions.
     */
    public static final class TriFunctions {

        /**
         * Instantiates a new tri functions.
         */
        protected TriFunctions() {
            // for extention.
        }
    }

    /**
     * The Class BinaryOperators.
     */
    public static final class BinaryOperators {

        /** The Constant THROWING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator THROWING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                throw new DuplicatedResultException(String.format("Duplicate key (attempted merging values %s and %s)", t, u));
            }
        };

        /** The Constant IGNORING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator IGNORING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                return t;
            }
        };

        /** The Constant REPLACING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator REPLACING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                return u;
            }
        };

        /** The Constant ADD_ALL_TO_FIRST. */
        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_FIRST = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.addAll(u);
                return t;
            }
        };

        /** The Constant ADD_ALL_TO_BIGGER. */
        private static final BinaryOperator<Collection<Object>> ADD_ALL_TO_BIGGER = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                if (t.size() >= u.size()) {
                    t.addAll(u);
                    return t;
                } else {
                    u.addAll(t);
                    return u;
                }
            }
        };

        /** The Constant REMOVE_ALL_FROM_FIRST. */
        private static final BinaryOperator<Collection<Object>> REMOVE_ALL_FROM_FIRST = new BinaryOperator<Collection<Object>>() {
            @Override
            public Collection<Object> apply(Collection<Object> t, Collection<Object> u) {
                t.removeAll(u);
                return t;
            }
        };

        /** The Constant PUT_ALL_TO_FIRST. */
        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_FIRST = new BinaryOperator<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                t.putAll(u);
                return t;
            }
        };

        /** The Constant PUT_ALL_TO_BIGGER. */
        private static final BinaryOperator<Map<Object, Object>> PUT_ALL_TO_BIGGER = new BinaryOperator<Map<Object, Object>>() {
            @Override
            public Map<Object, Object> apply(Map<Object, Object> t, Map<Object, Object> u) {
                if (t.size() >= u.size()) {
                    t.putAll(u);
                    return t;
                } else {
                    u.putAll(t);
                    return u;
                }
            }
        };

        /** The Constant MERGE_TO_FIRST. */
        private static final BinaryOperator<Joiner> MERGE_TO_FIRST = new BinaryOperator<Joiner>() {
            @Override
            public Joiner apply(Joiner t, Joiner u) {
                return t.merge(u);
            }
        };

        /** The Constant MERGE_TO_BIGGER. */
        private static final BinaryOperator<Joiner> MERGE_TO_BIGGER = new BinaryOperator<Joiner>() {
            @Override
            public Joiner apply(Joiner t, Joiner u) {
                if (t.length() >= u.length()) {
                    return t.merge(u);
                } else {
                    return u.merge(t);
                }
            }
        };

        /** The Constant APPEND_TO_FIRST. */
        private static final BinaryOperator<StringBuilder> APPEND_TO_FIRST = new BinaryOperator<StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, StringBuilder u) {
                return t.append(u);
            }
        };

        /** The Constant APPEND_TO_BIGGER. */
        private static final BinaryOperator<StringBuilder> APPEND_TO_BIGGER = new BinaryOperator<StringBuilder>() {
            @Override
            public StringBuilder apply(StringBuilder t, StringBuilder u) {
                if (t.length() >= u.length()) {
                    return t.append(u);
                } else {
                    return u.append(t);
                }
            }
        };

        /** The Constant CONCAT. */
        private static final BinaryOperator<String> CONCAT = new BinaryOperator<String>() {
            @Override
            public String apply(String t, String u) {
                return t + u;
            }
        };

        /** The Constant ADD_INTEGER. */
        private static final BinaryOperator<Integer> ADD_INTEGER = new BinaryOperator<Integer>() {
            @Override
            public Integer apply(Integer t, Integer u) {
                return t.intValue() + u.intValue();
            }
        };

        /** The Constant ADD_LONG. */
        private static final BinaryOperator<Long> ADD_LONG = new BinaryOperator<Long>() {
            @Override
            public Long apply(Long t, Long u) {
                return t.longValue() + u.longValue();
            }
        };

        /** The Constant ADD_DOUBLE. */
        private static final BinaryOperator<Double> ADD_DOUBLE = new BinaryOperator<Double>() {
            @Override
            public Double apply(Double t, Double u) {
                return t.doubleValue() + u.doubleValue();
            }
        };

        /** The Constant ADD_BIG_INTEGER. */
        private static final BinaryOperator<BigInteger> ADD_BIG_INTEGER = new BinaryOperator<BigInteger>() {
            @Override
            public BigInteger apply(BigInteger t, BigInteger u) {
                return t.add(u);
            }
        };

        /** The Constant ADD_BIG_DECIMAL. */
        private static final BinaryOperator<BigDecimal> ADD_BIG_DECIMAL = new BinaryOperator<BigDecimal>() {
            @Override
            public BigDecimal apply(BigDecimal t, BigDecimal u) {
                return t.add(u);
            }
        };

        /** The Constant MIN. */
        @SuppressWarnings({ "rawtypes" })
        private static final BinaryOperator<Comparable> MIN = new BinaryOperator<Comparable>() {
            @Override
            public Comparable apply(Comparable t, Comparable u) {
                return N.compare(t, u) <= 0 ? t : u;
            }
        };

        /** The Constant MAX. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Comparable> MAX = new BinaryOperator<Comparable>() {
            @Override
            public Comparable apply(Comparable t, Comparable u) {
                return N.compare(t, u) >= 0 ? t : u;
            }
        };

        /**
         * Instantiates a new binary operators.
         */
        protected BinaryOperators() {
            // for extention.
        }

        /**
         * Of add all.
         *
         * @param <T>
         * @param <C>
         * @return
         * @deprecated replaced by {@code #ofAddAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAll() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Of add all to first.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Of add all to bigger.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
            return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
        }

        /**
         * Of remove all.
         *
         * @param <T>
         * @param <C>
         * @return
         * @deprecated replaced by {@code #ofRemoveAllFromFirst()}.
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAll() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Of remove all from first.
         *
         * @param <T>
         * @param <C>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofRemoveAllFromFirst() {
            return (BinaryOperator<C>) REMOVE_ALL_FROM_FIRST;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         * @deprecated replaced by {@code #ofPutAllToFirst()}
         */
        @Deprecated
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAll() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Of put all to first.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToFirst() {
            return (BinaryOperator<M>) PUT_ALL_TO_FIRST;
        }

        /**
         * Of put all to bigger.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @return
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
            return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
        }

        /**
         *
         * @return
         * @deprecated replaced by {@code #ofMergeToFirst}.
         */
        @Deprecated
        public static BinaryOperator<Joiner> ofMerge() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to first.
         *
         * @return
         */
        public static BinaryOperator<Joiner> ofMergeToFirst() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to bigger.
         *
         * @return
         */
        public static BinaryOperator<Joiner> ofMergeToBigger() {
            return MERGE_TO_BIGGER;
        }

        /**
         *
         * @return
         * @deprecated replaced by {@code #ofAppendToFirst()}
         */
        @Deprecated
        public static BinaryOperator<StringBuilder> ofAppend() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to first.
         *
         * @return
         */
        public static BinaryOperator<StringBuilder> ofAppendToFirst() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to bigger.
         *
         * @return
         */
        public static BinaryOperator<StringBuilder> ofAppendToBigger() {
            return APPEND_TO_BIGGER;
        }

        /**
         *
         * @return
         */
        public static BinaryOperator<String> ofConcat() {
            return CONCAT;
        }

        /**
         * Of add int.
         *
         * @return
         */
        public static BinaryOperator<Integer> ofAddInt() {
            return ADD_INTEGER;
        }

        /**
         * Of add long.
         *
         * @return
         */
        public static BinaryOperator<Long> ofAddLong() {
            return ADD_LONG;
        }

        /**
         * Of add double.
         *
         * @return
         */
        public static BinaryOperator<Double> ofAddDouble() {
            return ADD_DOUBLE;
        }

        /**
         * Of add big integer.
         *
         * @return
         */
        public static BinaryOperator<BigInteger> ofAddBigInteger() {
            return ADD_BIG_INTEGER;
        }

        /**
         * Of add big decimal.
         *
         * @return
         */
        public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
            return ADD_BIG_DECIMAL;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
            return (BinaryOperator) MIN;
        }

        /**
         *
         * @param <T>
         * @param comparator
         * @return
         */
        public static <T> BinaryOperator<T> min(final Comparator<? super T> comparator) {
            N.checkArgNotNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) <= 0 ? t : u;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param comparator
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> BinaryOperator<T> minBy(final Function<? super T, ? extends Comparable> keyMapper) {
            N.checkArgNotNull(keyMapper);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return N.compare(keyMapper.apply(t), keyMapper.apply(u)) <= 0 ? t : u;
                }
            };
        }

        /** The Constant MIN_BY_KEY. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Map.Entry<Comparable, Object>> MIN_BY_KEY = new BinaryOperator<Map.Entry<Comparable, Object>>() {
            @Override
            public Map.Entry<Comparable, Object> apply(Map.Entry<Comparable, Object> t, Map.Entry<Comparable, Object> u) {
                return N.compare(t.getKey(), u.getKey()) <= 0 ? t : u;
            }
        };

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> minByKey() {
            return (BinaryOperator) MIN_BY_KEY;
        }

        /** The Constant MIN_BY_VALUE. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Map.Entry<Object, Comparable>> MIN_BY_VALUE = new BinaryOperator<Map.Entry<Object, Comparable>>() {
            @Override
            public Map.Entry<Object, Comparable> apply(Map.Entry<Object, Comparable> t, Map.Entry<Object, Comparable> u) {
                return N.compare(t.getValue(), u.getValue()) <= 0 ? t : u;
            }
        };

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> minByValue() {
            return (BinaryOperator) MIN_BY_VALUE;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
            return (BinaryOperator) MAX;
        }

        /**
         *
         * @param <T>
         * @param comparator
         * @return
         */
        public static <T> BinaryOperator<T> max(final Comparator<? super T> comparator) {
            N.checkArgNotNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) >= 0 ? t : u;
                }
            };
        }

        /**
         *
         * @param <T>
         * @param comparator
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> BinaryOperator<T> maxBy(final Function<? super T, ? extends Comparable> keyMapper) {
            N.checkArgNotNull(keyMapper);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return N.compare(keyMapper.apply(t), keyMapper.apply(u)) >= 0 ? t : u;
                }
            };
        }

        /** The Constant MAX_BY_KEY. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Map.Entry<Comparable, Object>> MAX_BY_KEY = new BinaryOperator<Map.Entry<Comparable, Object>>() {
            @Override
            public Map.Entry<Comparable, Object> apply(Map.Entry<Comparable, Object> t, Map.Entry<Comparable, Object> u) {
                return N.compare(t.getKey(), u.getKey()) >= 0 ? t : u;
            }
        };

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K extends Comparable<? super K>, V> BinaryOperator<Map.Entry<K, V>> maxByKey() {
            return (BinaryOperator) MAX_BY_KEY;
        }

        /** The Constant MAX_BY_VALUE. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator<Map.Entry<Object, Comparable>> MAX_BY_VALUE = new BinaryOperator<Map.Entry<Object, Comparable>>() {
            @Override
            public Map.Entry<Object, Comparable> apply(Map.Entry<Object, Comparable> t, Map.Entry<Object, Comparable> u) {
                return N.compare(t.getValue(), u.getValue()) >= 0 ? t : u;
            }
        };

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <K, V extends Comparable<? super V>> BinaryOperator<Map.Entry<K, V>> maxByValue() {
            return (BinaryOperator) MAX_BY_VALUE;
        }
    }

    /**
     * The Class UnaryOperators.
     */
    public static final class UnaryOperators {

        /** The Constant IDENTITY. */
        @SuppressWarnings("rawtypes")
        private static final UnaryOperator IDENTITY = new UnaryOperator() {
            @Override
            public Object apply(Object t) {
                return t;
            }
        };

        /**
         * Instantiates a new unary operators.
         */
        protected UnaryOperators() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @return
         */
        public static <T> UnaryOperator<T> identity() {
            return IDENTITY;
        }
    }

    /**
     * The Class Entries.
     */
    public static final class Entries {

        /**
         * Instantiates a new entries.
         */
        protected Entries() {
            // for extention.
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param f
         * @return
         */
        public static <K, V, T> Function<Map.Entry<K, V>, T> f(final BiFunction<? super K, ? super V, ? extends T> f) {
            N.checkArgNotNull(f, "BiFunction");

            return new Function<Map.Entry<K, V>, T>() {
                @Override
                public T apply(Entry<K, V> e) {
                    return f.apply(e.getKey(), e.getValue());
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param p
         * @return
         */
        public static <K, V> Predicate<Map.Entry<K, V>> p(final BiPredicate<? super K, ? super V> p) {
            N.checkArgNotNull(p, "BiPredicate");

            return new Predicate<Map.Entry<K, V>>() {
                @Override
                public boolean test(Entry<K, V> e) {
                    return p.test(e.getKey(), e.getValue());
                }

            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param c
         * @return
         */
        public static <K, V> Consumer<Map.Entry<K, V>> c(final BiConsumer<? super K, ? super V> c) {
            N.checkArgNotNull(c, "BiConsumer");

            return new Consumer<Map.Entry<K, V>>() {
                @Override
                public void accept(Entry<K, V> e) {
                    c.accept(e.getKey(), e.getValue());
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param <E>
         * @param f
         * @return
         */
        @Beta
        public static <K, V, T, E extends Exception> Throwables.Function<Map.Entry<K, V>, T, E> ef(
                final Throwables.BiFunction<? super K, ? super V, ? extends T, E> f) {
            N.checkArgNotNull(f, "BiFunction");

            return new Throwables.Function<Map.Entry<K, V>, T, E>() {
                @Override
                public T apply(Entry<K, V> e) throws E {
                    return f.apply(e.getKey(), e.getValue());
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param p
         * @return
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Predicate<Map.Entry<K, V>, E> ep(final Throwables.BiPredicate<? super K, ? super V, E> p) {
            N.checkArgNotNull(p, "BiPredicate");

            return new Throwables.Predicate<Map.Entry<K, V>, E>() {
                @Override
                public boolean test(Entry<K, V> e) throws E {
                    return p.test(e.getKey(), e.getValue());
                }

            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param c
         * @return
         */
        @Beta
        public static <K, V, E extends Exception> Throwables.Consumer<Map.Entry<K, V>, E> ec(final Throwables.BiConsumer<? super K, ? super V, E> c) {
            N.checkArgNotNull(c, "BiConsumer");

            return new Throwables.Consumer<Map.Entry<K, V>, E>() {
                @Override
                public void accept(Entry<K, V> e) throws E {
                    c.accept(e.getKey(), e.getValue());
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T>
         * @param <E>
         * @param f
         * @return
         */
        public static <K, V, T, E extends Exception> Function<Map.Entry<K, V>, T> ff(final Throwables.BiFunction<? super K, ? super V, ? extends T, E> f) {
            N.checkArgNotNull(f, "BiFunction");

            return new Function<Map.Entry<K, V>, T>() {
                @Override
                public T apply(Entry<K, V> e) {
                    try {
                        return f.apply(e.getKey(), e.getValue());
                    } catch (Exception ex) {
                        throw N.toRuntimeException(ex);
                    }
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param p
         * @return
         */
        public static <K, V, E extends Exception> Predicate<Map.Entry<K, V>> pp(final Throwables.BiPredicate<? super K, ? super V, E> p) {
            N.checkArgNotNull(p, "BiPredicate");

            return new Predicate<Map.Entry<K, V>>() {
                @Override
                public boolean test(Entry<K, V> e) {
                    try {
                        return p.test(e.getKey(), e.getValue());
                    } catch (Exception ex) {
                        throw N.toRuntimeException(ex);
                    }
                }
            };
        }

        /**
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E>
         * @param c
         * @return
         */
        public static <K, V, E extends Exception> Consumer<Map.Entry<K, V>> cc(final Throwables.BiConsumer<? super K, ? super V, E> c) {
            N.checkArgNotNull(c, "BiConsumer");

            return new Consumer<Map.Entry<K, V>>() {
                @Override
                public void accept(Entry<K, V> e) {
                    try {
                        c.accept(e.getKey(), e.getValue());
                    } catch (Exception ex) {
                        throw N.toRuntimeException(ex);
                    }
                }
            };
        }
    }

    /**
     * The Class Pairs.
     */
    public static final class Pairs {

        /** The Constant PAIR_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, List> PAIR_TO_LIST = new Function<Pair, List>() {
            @Override
            public List apply(Pair t) {
                return N.asList(t.left, t.right);
            }
        };

        /** The Constant PAIR_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Pair, Set> PAIR_TO_SET = new Function<Pair, Set>() {
            @Override
            public Set apply(Pair t) {
                return N.asSet(t.left, t.right);
            }
        };

        /**
         * Instantiates a new pairs.
         */
        protected Pairs() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, List<T>> toList() {
            return (Function) PAIR_TO_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, Set<T>> toSet() {
            return (Function) PAIR_TO_SET;
        }

    }

    /**
     * The Class Triples.
     */
    public static final class Triples {

        /** The Constant TRIPLE_TO_LIST. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, List> TRIPLE_TO_LIST = new Function<Triple, List>() {
            @Override
            public List apply(Triple t) {
                return N.asList(t.left, t.middle, t.right);
            }
        };

        /** The Constant TRIPLE_TO_SET. */
        @SuppressWarnings("rawtypes")
        private static final Function<Triple, Set> TRIPLE_TO_SET = new Function<Triple, Set>() {
            @Override
            public Set apply(Triple t) {
                return N.asSet(t.left, t.middle, t.right);
            }
        };

        /**
         * Instantiates a new triples.
         */
        protected Triples() {
            // for extention.
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, List<T>> toList() {
            return (Function) TRIPLE_TO_LIST;
        }

        /**
         *
         * @param <T>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, Set<T>> toSet() {
            return (Function) TRIPLE_TO_SET;
        }
    }

    /**
     * The Class Disposables.
     */
    public static final class Disposables {

        /** The Constant CLONE. */
        @SuppressWarnings("rawtypes")
        private static final Function<DisposableArray, Object[]> CLONE = new Function<DisposableArray, Object[]>() {
            @Override
            public Object[] apply(DisposableArray t) {
                return t.clone();
            }
        };

        /** The Constant TO_STRING. */
        @SuppressWarnings("rawtypes")
        private static final Function<DisposableArray, String> TO_STRING = new Function<DisposableArray, String>() {
            @Override
            public String apply(DisposableArray t) {
                return t.toString();
            }
        };

        /**
         * Instantiates a new disposables.
         */
        private Disposables() {
            // singleton.
        }

        /**
         *
         * @param <T>
         * @param <A>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <T, A extends DisposableArray<T>> Function<A, T[]> cloneArray() {
            return (Function) CLONE;
        }

        /**
         *
         * @param <A>
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> toStr() {
            return (Function) TO_STRING;
        }

        /**
         *
         * @param <A>
         * @param delimiter
         * @return
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> join(final String delimiter) {
            return new Function<A, String>() {
                @Override
                public String apply(A t) {
                    return t.join(delimiter);
                }
            };
        }
    }

    /**
     * Utility class for {@code CharPredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnC {

        /** The Constant IS_ZERO. */
        private static final CharPredicate IS_ZERO = new CharPredicate() {
            @Override
            public boolean test(char t) {
                return t == 0;
            }
        };

        /** The Constant IS_WHITE_SPACE. */
        private static final CharPredicate IS_WHITESPACE = new CharPredicate() {
            @Override
            public boolean test(char t) {
                return Character.isWhitespace(t);
            }
        };

        /** The Constant EQUAL. */
        private static final CharBiPredicate EQUAL = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final CharBiPredicate NOT_EQUAL = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final CharBiPredicate GREATER_THAN = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final CharBiPredicate GREATER_EQUAL = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final CharBiPredicate LESS_THAN = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final CharBiPredicate LESS_EQUAL = new CharBiPredicate() {
            @Override
            public boolean test(char t, char u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<char[], Integer> LEN = new Function<char[], Integer>() {
            @Override
            public Integer apply(char[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn C.
         */
        protected FnC() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static CharPredicate isZero() {
            return IS_ZERO;
        }

        /**
         *
         * @return
         */
        public static CharPredicate isWhitespace() {
            return IS_WHITESPACE;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static CharBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToCharFunction<Character> unbox() {
            return ToCharFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static CharPredicate p(final CharPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> CharFunction<R> f(final CharFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static CharConsumer c(final CharConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<char[], Integer> len() {
            return LEN;
        }

        /**
         * Returns a stateful <code>CharBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static CharBiFunction<MergeResult> alternated() {
            return new CharBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(char t, char u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class CharBinaryOperators {
            private CharBinaryOperators() {
                // Singleton for utility class.
            }

            public static final CharBinaryOperator MIN = new CharBinaryOperator() {
                @Override
                public char applyAsChar(char left, char right) {
                    return left <= right ? left : right;
                }
            };

            public static final CharBinaryOperator MAX = new CharBinaryOperator() {
                @Override
                public char applyAsChar(char left, char right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code BytePredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnB {

        /** The Constant POSITIVE. */
        private static final BytePredicate POSITIVE = new BytePredicate() {
            @Override
            public boolean test(byte t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final BytePredicate NOT_NEGATIVE = new BytePredicate() {
            @Override
            public boolean test(byte t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final ByteBiPredicate EQUAL = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final ByteBiPredicate NOT_EQUAL = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final ByteBiPredicate GREATER_THAN = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final ByteBiPredicate GREATER_EQUAL = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final ByteBiPredicate LESS_THAN = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final ByteBiPredicate LESS_EQUAL = new ByteBiPredicate() {
            @Override
            public boolean test(byte t, byte u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<byte[], Integer> LEN = new Function<byte[], Integer>() {
            @Override
            public Integer apply(byte[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn B.
         */
        protected FnB() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static BytePredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static BytePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static ByteBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToByteFunction<Byte> unbox() {
            return ToByteFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static BytePredicate p(final BytePredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> ByteFunction<R> f(final ByteFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static ByteConsumer c(final ByteConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<byte[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<byte[], Integer> SUM = new Function<byte[], Integer>() {
            @Override
            public Integer apply(byte[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<byte[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<byte[], Double> AVERAGE = new Function<byte[], Double>() {
            @Override
            public Double apply(byte[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<byte[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>ByteBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static ByteBiFunction<MergeResult> alternated() {
            return new ByteBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(byte t, byte u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class ByteBinaryOperators {
            private ByteBinaryOperators() {
                // Singleton for utility class.
            }

            public static final ByteBinaryOperator MIN = new ByteBinaryOperator() {
                @Override
                public byte applyAsByte(byte left, byte right) {
                    return left <= right ? left : right;
                }
            };

            public static final ByteBinaryOperator MAX = new ByteBinaryOperator() {
                @Override
                public byte applyAsByte(byte left, byte right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code ShortPredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnS {

        /** The Constant POSITIVE. */
        private static final ShortPredicate POSITIVE = new ShortPredicate() {
            @Override
            public boolean test(short t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final ShortPredicate NOT_NEGATIVE = new ShortPredicate() {
            @Override
            public boolean test(short t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final ShortBiPredicate EQUAL = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final ShortBiPredicate NOT_EQUAL = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final ShortBiPredicate GREATER_THAN = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final ShortBiPredicate GREATER_EQUAL = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final ShortBiPredicate LESS_THAN = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final ShortBiPredicate LESS_EQUAL = new ShortBiPredicate() {
            @Override
            public boolean test(short t, short u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<short[], Integer> LEN = new Function<short[], Integer>() {
            @Override
            public Integer apply(short[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn S.
         */
        protected FnS() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static ShortPredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static ShortPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static ShortBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToShortFunction<Short> unbox() {
            return ToShortFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static ShortPredicate p(final ShortPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> ShortFunction<R> f(final ShortFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static ShortConsumer c(final ShortConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<short[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<short[], Integer> SUM = new Function<short[], Integer>() {
            @Override
            public Integer apply(short[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<short[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<short[], Double> AVERAGE = new Function<short[], Double>() {
            @Override
            public Double apply(short[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<short[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>ShortBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static ShortBiFunction<MergeResult> alternated() {
            return new ShortBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(short t, short u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class ShortBinaryOperators {
            private ShortBinaryOperators() {
                // Singleton for utility class.
            }

            public static final ShortBinaryOperator MIN = new ShortBinaryOperator() {
                @Override
                public short applyAsShort(short left, short right) {
                    return left <= right ? left : right;
                }
            };

            public static final ShortBinaryOperator MAX = new ShortBinaryOperator() {
                @Override
                public short applyAsShort(short left, short right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code IntPredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnI {

        /** The Constant POSITIVE. */
        private static final IntPredicate POSITIVE = new IntPredicate() {
            @Override
            public boolean test(int t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final IntPredicate NOT_NEGATIVE = new IntPredicate() {
            @Override
            public boolean test(int t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final IntBiPredicate EQUAL = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final IntBiPredicate NOT_EQUAL = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final IntBiPredicate GREATER_THAN = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final IntBiPredicate GREATER_EQUAL = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final IntBiPredicate LESS_THAN = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final IntBiPredicate LESS_EQUAL = new IntBiPredicate() {
            @Override
            public boolean test(int t, int u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<int[], Integer> LEN = new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn I.
         */
        protected FnI() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static IntPredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static IntPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static IntBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToIntFunction<Integer> unbox() {
            return ToIntFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static IntPredicate p(final IntPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> IntFunction<R> f(final IntFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static IntConsumer c(final IntConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<int[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<int[], Integer> SUM = new Function<int[], Integer>() {
            @Override
            public Integer apply(int[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<int[], Integer> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<int[], Double> AVERAGE = new Function<int[], Double>() {
            @Override
            public Double apply(int[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<int[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>IntBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static IntBiFunction<MergeResult> alternated() {
            return new IntBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(int t, int u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class IntBinaryOperators {
            private IntBinaryOperators() {
                // Singleton for utility class.
            }

            public static final IntBinaryOperator MIN = new IntBinaryOperator() {
                @Override
                public int applyAsInt(int left, int right) {
                    return left <= right ? left : right;
                }
            };

            public static final IntBinaryOperator MAX = new IntBinaryOperator() {
                @Override
                public int applyAsInt(int left, int right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code LongPredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnL {

        /** The Constant POSITIVE. */
        private static final LongPredicate POSITIVE = new LongPredicate() {
            @Override
            public boolean test(long t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final LongPredicate NOT_NEGATIVE = new LongPredicate() {
            @Override
            public boolean test(long t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final LongBiPredicate EQUAL = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final LongBiPredicate NOT_EQUAL = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final LongBiPredicate GREATER_THAN = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final LongBiPredicate GREATER_EQUAL = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final LongBiPredicate LESS_THAN = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final LongBiPredicate LESS_EQUAL = new LongBiPredicate() {
            @Override
            public boolean test(long t, long u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<long[], Integer> LEN = new Function<long[], Integer>() {
            @Override
            public Integer apply(long[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn L.
         */
        protected FnL() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static LongPredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static LongPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static LongBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToLongFunction<Long> unbox() {
            return ToLongFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static LongPredicate p(final LongPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> LongFunction<R> f(final LongFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static LongConsumer c(final LongConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<long[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<long[], Long> SUM = new Function<long[], Long>() {
            @Override
            public Long apply(long[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<long[], Long> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<long[], Double> AVERAGE = new Function<long[], Double>() {
            @Override
            public Double apply(long[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<long[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>LongBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static LongBiFunction<MergeResult> alternated() {
            return new LongBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(long t, long u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class LongBinaryOperators {
            private LongBinaryOperators() {
                // Singleton for utility class.
            }

            public static final LongBinaryOperator MIN = new LongBinaryOperator() {
                @Override
                public long applyAsLong(long left, long right) {
                    return left <= right ? left : right;
                }
            };

            public static final LongBinaryOperator MAX = new LongBinaryOperator() {
                @Override
                public long applyAsLong(long left, long right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code FloatPredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnF {

        /** The Constant POSITIVE. */
        private static final FloatPredicate POSITIVE = new FloatPredicate() {
            @Override
            public boolean test(float t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final FloatPredicate NOT_NEGATIVE = new FloatPredicate() {
            @Override
            public boolean test(float t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final FloatBiPredicate EQUAL = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t == u;
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final FloatBiPredicate NOT_EQUAL = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t != u;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final FloatBiPredicate GREATER_THAN = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t > u;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final FloatBiPredicate GREATER_EQUAL = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t >= u;
            }
        };

        /** The Constant LESS_THAN. */
        private static final FloatBiPredicate LESS_THAN = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t < u;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final FloatBiPredicate LESS_EQUAL = new FloatBiPredicate() {
            @Override
            public boolean test(float t, float u) {
                return t <= u;
            }
        };

        /** The Constant LEN. */
        private static final Function<float[], Integer> LEN = new Function<float[], Integer>() {
            @Override
            public Integer apply(float[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn F.
         */
        protected FnF() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static FloatPredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static FloatPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static FloatBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToFloatFunction<Float> unbox() {
            return ToFloatFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static FloatPredicate p(final FloatPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> FloatFunction<R> f(final FloatFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static FloatConsumer c(final FloatConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<float[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<float[], Float> SUM = new Function<float[], Float>() {
            @Override
            public Float apply(float[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<float[], Float> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<float[], Double> AVERAGE = new Function<float[], Double>() {
            @Override
            public Double apply(float[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<float[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>FloatBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static FloatBiFunction<MergeResult> alternated() {
            return new FloatBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(float t, float u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class FloatBinaryOperators {
            private FloatBinaryOperators() {
                // Singleton for utility class.
            }

            public static final FloatBinaryOperator MIN = new FloatBinaryOperator() {
                @Override
                public float applyAsFloat(float left, float right) {
                    return left <= right ? left : right;
                }
            };

            public static final FloatBinaryOperator MAX = new FloatBinaryOperator() {
                @Override
                public float applyAsFloat(float left, float right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for {@code DoublePredicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class FnD {

        /** The Constant POSITIVE. */
        private static final DoublePredicate POSITIVE = new DoublePredicate() {
            @Override
            public boolean test(double t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final DoublePredicate NOT_NEGATIVE = new DoublePredicate() {
            @Override
            public boolean test(double t) {
                return t >= 0;
            }
        };

        /** The Constant EQUAL. */
        private static final DoubleBiPredicate EQUAL = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.equals(t, u);
            }
        };

        /** The Constant NOT_EQUAL. */
        private static final DoubleBiPredicate NOT_EQUAL = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.compare(t, u) != 0;
            }
        };

        /** The Constant GREATER_THAN. */
        private static final DoubleBiPredicate GREATER_THAN = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.compare(t, u) > 0;
            }
        };

        /** The Constant GREATER_EQUAL. */
        private static final DoubleBiPredicate GREATER_EQUAL = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.compare(t, u) >= 0;
            }
        };

        /** The Constant LESS_THAN. */
        private static final DoubleBiPredicate LESS_THAN = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.compare(t, u) < 0;
            }
        };

        /** The Constant LESS_EQUAL. */
        private static final DoubleBiPredicate LESS_EQUAL = new DoubleBiPredicate() {
            @Override
            public boolean test(double t, double u) {
                return N.compare(t, u) <= 0;
            }
        };

        /** The Constant LEN. */
        private static final Function<double[], Integer> LEN = new Function<double[], Integer>() {
            @Override
            public Integer apply(double[] t) {
                return t == null ? 0 : t.length;
            }
        };

        /**
         * Instantiates a new fn D.
         */
        protected FnD() {
            // for extention.
        }

        /**
         *
         * @return
         */
        public static DoublePredicate positve() {
            return POSITIVE;
        }

        /**
         *
         * @return
         */
        public static DoublePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate equal() {
            return EQUAL;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         *
         * @return
         */
        public static DoubleBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         *
         * @return
         */
        public static ToDoubleFunction<Double> unbox() {
            return ToDoubleFunction.UNBOX;
        }

        /**
         *
         * @param p
         * @return
         */
        public static DoublePredicate p(final DoublePredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         *
         * @param <R>
         * @param f
         * @return
         */
        public static <R> DoubleFunction<R> f(final DoubleFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         *
         * @param c
         * @return
         */
        public static DoubleConsumer c(final DoubleConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         *
         * @return
         */
        public static Function<double[], Integer> len() {
            return LEN;
        }

        /** The Constant SUM. */
        private static final Function<double[], Double> SUM = new Function<double[], Double>() {
            @Override
            public Double apply(double[] t) {
                return N.sum(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<double[], Double> sum() {
            return SUM;
        }

        /** The Constant AVERAGE. */
        private static final Function<double[], Double> AVERAGE = new Function<double[], Double>() {
            @Override
            public Double apply(double[] t) {
                return N.average(t);
            }
        };

        /**
         *
         * @return
         */
        public static Function<double[], Double> average() {
            return AVERAGE;
        }

        /**
         * Returns a stateful <code>DoubleBiFunction</code>. Don't save or cache for reuse or use it in parallel stream.
         *
         * @return
         * @deprecated
         */
        @Deprecated
        @Beta
        @SequentialOnly
        @Stateful
        public static DoubleBiFunction<MergeResult> alternated() {
            return new DoubleBiFunction<MergeResult>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public MergeResult apply(double t, double u) {
                    return flag.getAndInvert() ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND;
                }
            };
        }

        public static final class DoubleBinaryOperators {
            private DoubleBinaryOperators() {
                // Singleton for utility class.
            }

            public static final DoubleBinaryOperator MIN = new DoubleBinaryOperator() {
                @Override
                public double applyAsDouble(double left, double right) {
                    return left <= right ? left : right;
                }
            };

            public static final DoubleBinaryOperator MAX = new DoubleBinaryOperator() {
                @Override
                public double applyAsDouble(double left, double right) {
                    return left >= right ? left : right;
                }
            };
        }
    }

    /**
     * Utility class for exceptional {@code Predicate/Function/Consumer}.
     *
     * @author haiyangl
     *
     */
    public static final class Fnn {
        private Fnn() {
            // Singleton for utility class
        }

        /**
         * Returns a {@code Supplier} which returns a single instance created by calling the specified {@code supplier.get()}.
         *
         * @param <T>
         * @param supplier
         * @return
         */
        public static <T, E extends Throwable> Throwables.LazyInitializer<T, E> memoize(final Throwables.Supplier<T, E> supplier) {
            return Throwables.LazyInitializer.of(supplier);
        }

        public static <T, E extends Exception> Throwables.Function<T, T, E> identity() {
            return Fn.IDENTITY;
        }

        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysTrue() {
            return Fn.ALWAYS_TRUE;
        }

        public static <T, E extends Exception> Throwables.Predicate<T, E> alwaysFalse() {
            return Fn.ALWAYS_FALSE;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, K, E> key() {
            return (Throwables.Function) Fn.KEY;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Map.Entry<K, V>, V, E> value() {
            return (Throwables.Function) Fn.VALUE;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.Function<Entry<K, V>, Entry<V, K>, E> inverse() {
            return (Throwables.Function) Fn.INVERSE;
        }

        @SuppressWarnings("rawtypes")
        public static <K, V, E extends Exception> Throwables.BiFunction<K, V, Map.Entry<K, V>, E> entry() {
            return (Throwables.BiFunction) Fn.ENTRY;
        }

        @SuppressWarnings("rawtypes")
        public static <L, R, E extends Exception> Throwables.BiFunction<L, R, Pair<L, R>, E> pair() {
            return (Throwables.BiFunction) Fn.PAIR;
        }

        @SuppressWarnings("rawtypes")
        public static <L, M, R, E extends Exception> Throwables.TriFunction<L, M, R, Triple<L, M, R>, E> triple() {
            return (Throwables.TriFunction) Fn.TRIPLE;
        }

        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Function<T, Tuple1<T>, E> tuple1() {
            return (Throwables.Function) Fn.TUPLE_1;
        }

        @SuppressWarnings("rawtypes")
        public static <T, U, E extends Exception> Throwables.BiFunction<T, U, Tuple2<T, U>, E> tuple2() {
            return (Throwables.BiFunction) Fn.TUPLE_2;
        }

        @SuppressWarnings("rawtypes")
        public static <A, B, C, E extends Exception> Throwables.TriFunction<A, B, C, Tuple3<A, B, C>, E> tuple3() {
            return (Throwables.TriFunction) Fn.TUPLE_3;
        }

        public static <E extends Exception> Throwables.Runnable<E> emptyAction() {
            return (Throwables.Runnable<E>) Fn.EMPTY_ACTION;
        }

        public static <T, E extends Exception> Throwables.Consumer<T, E> doNothing() {
            return Fn.DO_NOTHING;
        }

        public static <T> Throwables.Consumer<T, RuntimeException> throwRuntimeException(final String errorMessage) {
            return new Throwables.Consumer<T, RuntimeException>() {
                @Override
                public void accept(T t) throws RuntimeException {
                    throw new RuntimeException(errorMessage);
                }
            };
        }

        public static <T> Throwables.Consumer<T, IOException> throwIOException(final String errorMessage) {
            return new Throwables.Consumer<T, IOException>() {
                @Override
                public void accept(T t) throws IOException {
                    throw new IOException(errorMessage);
                }
            };
        }

        public static <T> Throwables.Consumer<T, Exception> throwException(final String errorMessage) {
            return new Throwables.Consumer<T, Exception>() {
                @Override
                public void accept(T t) throws Exception {
                    throw new Exception(errorMessage);
                }
            };
        }

        public static <T, E extends Exception> Throwables.Consumer<T, E> throwException(final Supplier<? extends E> excpetionSupplier) {
            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    throw excpetionSupplier.get();
                }
            };
        }

        public static <T, E extends Exception> Throwables.Consumer<T, E> sleep(final long millis) {
            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    N.sleep(millis);
                }
            };
        }

        public static <T, E extends Exception> Throwables.Consumer<T, E> sleepUninterruptibly(final long millis) {
            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    N.sleepUninterruptibly(millis);
                }
            };
        }

        private static final Throwables.Consumer<AutoCloseable, Exception> CLOSE = new Throwables.Consumer<AutoCloseable, Exception>() {
            @Override
            public void accept(final AutoCloseable closeable) throws Exception {
                if (closeable != null) {
                    closeable.close();
                }
            }
        };

        //    public static <E extends Exception> Throwables.Runnable<E> close(final AutoCloseable closeable) {
        //        return new Throwables.Runnable<E>() {
        //            private volatile boolean isClosed = false;
        //
        //            @Override
        //            public void run() throws E {
        //                if (isClosed) {
        //                    return;
        //                }
        //
        //                isClosed = true;
        //                IOUtil.close(closeable);
        //            }
        //        };
        //    }
        //
        //    public static <E extends Exception> Throwables.Runnable<E> closeQuietly(final AutoCloseable closeable) {
        //        return new Throwables.Runnable<E>() {
        //            private volatile boolean isClosed = false;
        //
        //            @Override
        //            public void run() {
        //                if (isClosed) {
        //                    return;
        //                }
        //
        //                isClosed = true;
        //                IOUtil.closeQuietly(closeable);
        //            }
        //        };
        //    }

        public static <T extends AutoCloseable> Throwables.Consumer<T, Exception> close() {
            return (Throwables.Consumer<T, Exception>) CLOSE;
        }

        public static <T extends AutoCloseable, E extends Exception> Throwables.Consumer<T, E> closeQuietly() {
            return (Throwables.Consumer<T, E>) Fn.CLOSE_QUIETLY;
        }

        public static <T, E extends Exception> Throwables.Consumer<T, E> println() {
            return Fn.PRINTLN;
        }

        public static <T, U, E extends Exception> Throwables.BiConsumer<T, U, E> println(final String separator) {
            return cc(Fn.<T, U> println(separator));
        }

        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> notNull() {
            return Fn.NOT_NULL;
        }

        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> notNullOrEmpty() {
            return (Throwables.Predicate<T, E>) Fn.NOT_NULL_OR_EMPTY;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> notNullOrEmptyA() {
            return (Throwables.Predicate) Fn.NOT_NULL_OR_EMPTY_A;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> notNullOrEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.NOT_NULL_OR_EMPTY_C;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> notNullOrEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.NOT_NULL_OR_EMPTY_M;
        }

        @Beta
        public static <T, E extends Exception> Throwables.Predicate<T, E> isNull() {
            return Fn.IS_NULL;
        }

        public static <T extends CharSequence, E extends Exception> Throwables.Predicate<T, E> isNullOrEmpty() {
            return (Throwables.Predicate<T, E>) Fn.IS_NULL_OR_EMPTY;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T, E extends Exception> Throwables.Predicate<T[], E> isNullOrEmptyA() {
            return (Throwables.Predicate) Fn.IS_NULL_OR_EMPTY_A;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Collection, E extends Exception> Throwables.Predicate<T, E> isNullOrEmptyC() {
            return (Throwables.Predicate<T, E>) Fn.IS_NULL_OR_EMPTY_C;
        }

        @Beta
        @SuppressWarnings("rawtypes")
        public static <T extends Map, E extends Exception> Throwables.Predicate<T, E> isNullOrEmptyM() {
            return (Throwables.Predicate<T, E>) Fn.IS_NULL_OR_EMPTY_M;
        }

        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> throwingMerger() {
            return BinaryOperators.THROWING_MERGER;
        }

        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> ignoringMerger() {
            return BinaryOperators.IGNORING_MERGER;
        }

        public static <T, E extends Exception> Throwables.BinaryOperator<T, E> replacingMerger() {
            return BinaryOperators.REPLACING_MERGER;
        }

        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByKey(final Throwables.Predicate<? super K, E> predicate) {
            N.checkArgNotNull(predicate);

            return new Throwables.Predicate<Map.Entry<K, V>, E>() {
                @Override
                public boolean test(Entry<K, V> entry) throws E {
                    return predicate.test(entry.getKey());
                }
            };
        }

        public static <K, V, E extends Throwable> Throwables.Predicate<Map.Entry<K, V>, E> testByValue(final Throwables.Predicate<? super V, E> predicate) {
            N.checkArgNotNull(predicate);

            return new Throwables.Predicate<Map.Entry<K, V>, E>() {
                @Override
                public boolean test(Entry<K, V> entry) throws E {
                    return predicate.test(entry.getValue());
                }
            };
        }

        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByKey(final Throwables.Consumer<? super K, E> consumer) {
            N.checkArgNotNull(consumer);

            return new Throwables.Consumer<Map.Entry<K, V>, E>() {
                @Override
                public void accept(Entry<K, V> entry) throws E {
                    consumer.accept(entry.getKey());
                }
            };
        }

        public static <K, V, E extends Throwable> Throwables.Consumer<Map.Entry<K, V>, E> acceptByValue(final Throwables.Consumer<? super V, E> consumer) {
            N.checkArgNotNull(consumer);

            return new Throwables.Consumer<Map.Entry<K, V>, E>() {
                @Override
                public void accept(Entry<K, V> entry) throws E {
                    consumer.accept(entry.getValue());
                }
            };
        }

        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByKey(final Throwables.Function<? super K, R, E> func) {
            N.checkArgNotNull(func);

            return new Throwables.Function<Map.Entry<K, V>, R, E>() {
                @Override
                public R apply(Entry<K, V> entry) throws E {
                    return func.apply(entry.getKey());
                }
            };
        }

        public static <K, V, R, E extends Throwable> Throwables.Function<Map.Entry<K, V>, R, E> applyByValue(final Throwables.Function<? super V, R, E> func) {
            N.checkArgNotNull(func);

            return new Throwables.Function<Map.Entry<K, V>, R, E>() {
                @Override
                public R apply(Entry<K, V> entry) throws E {
                    return func.apply(entry.getValue());
                }
            };
        }

        public static <T, E extends Throwable> Throwables.Predicate<T, E> not(final Throwables.Predicate<T, E> predicate) {
            N.checkArgNotNull(predicate);

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    return !predicate.test(t);
                }
            };
        }

        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> not(final Throwables.BiPredicate<T, U, E> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return new Throwables.BiPredicate<T, U, E>() {
                @Override
                public boolean test(T t, U u) throws E {
                    return !biPredicate.test(t, u);
                }
            };
        }

        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> not(final Throwables.TriPredicate<A, B, C, E> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Throwables.TriPredicate<A, B, C, E>() {
                @Override
                public boolean test(A a, B b, C c) throws E {
                    return !triPredicate.test(a, b, c);
                }
            };
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Supplier<T, E> from(final java.util.function.Supplier<T> supplier) {
            return () -> supplier.get();
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> from(final java.util.function.Predicate<T> predicate) {
            return t -> predicate.test(t);
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> from(final java.util.function.BiPredicate<T, U> predicate) {
            return (t, u) -> predicate.test(t, u);
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> from(final java.util.function.Consumer<T> consumer) {
            return t -> consumer.accept(t);
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> from(final java.util.function.BiConsumer<T, U> consumer) {
            return (t, u) -> consumer.accept(t, u);
        }

        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> from(final java.util.function.Function<T, R> function) {
            return t -> function.apply(t);
        }

        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> from(final java.util.function.BiFunction<T, U, R> function) {
            return (t, u) -> function.apply(t, u);
        }

        @Beta
        public static <T, E extends Throwable> Throwables.UnaryOperator<T, E> from(final java.util.function.UnaryOperator<T> op) {
            return t -> op.apply(t);
        }

        @Beta
        public static <T, E extends Throwable> Throwables.BinaryOperator<T, E> from(final java.util.function.BinaryOperator<T> op) {
            return (t1, t2) -> op.apply(t1, t2);
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> p(final Throwables.Predicate<T, E> predicate) {
            return predicate;
        }

        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final Throwables.BiPredicate<A, T, E> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    return biPredicate.test(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> p(final A a, final B b,
                final Throwables.TriPredicate<A, B, T, E> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    return triPredicate.test(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final Throwables.BiPredicate<T, U, E> biPredicate) {
            return biPredicate;
        }

        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> p(final A a, final Throwables.TriPredicate<A, T, U, E> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Throwables.BiPredicate<T, U, E>() {
                @Override
                public boolean test(T t, U u) throws E {
                    return triPredicate.test(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> p(final Throwables.TriPredicate<A, B, C, E> triPredicate) {
            return triPredicate;
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> c(final Throwables.Consumer<T, E> predicate) {
            return predicate;
        }

        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final Throwables.BiConsumer<A, T, E> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    biConsumer.accept(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> c(final A a, final B b, final Throwables.TriConsumer<A, B, T, E> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    triConsumer.accept(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final Throwables.BiConsumer<T, U, E> biConsumer) {
            return biConsumer;
        }

        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> c(final A a, final Throwables.TriConsumer<A, T, U, E> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) throws E {
                    triConsumer.accept(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> c(final Throwables.TriConsumer<A, B, C, E> triConsumer) {
            return triConsumer;
        }

        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> f(final Throwables.Function<T, R, E> predicate) {
            return predicate;
        }

        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final Throwables.BiFunction<A, T, R, E> biFunction) {
            N.checkArgNotNull(biFunction);

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) throws E {
                    return biFunction.apply(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> f(final A a, final B b,
                final Throwables.TriFunction<A, B, T, R, E> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) throws E {
                    return triFunction.apply(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final Throwables.BiFunction<T, U, R, E> biFunction) {
            return biFunction;
        }

        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> f(final A a,
                final Throwables.TriFunction<A, T, U, R, E> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Throwables.BiFunction<T, U, R, E>() {
                @Override
                public R apply(T t, U u) throws E {
                    return triFunction.apply(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> f(final Throwables.TriFunction<A, B, C, R, E> triFunction) {
            return triFunction;
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> pp(final Predicate<T> predicate) {
            N.checkArgNotNull(predicate);

            return (Throwables.Predicate<T, E>) predicate;
        }

        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final BiPredicate<A, T> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) {
                    return biPredicate.test(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> pp(final A a, final B b, final TriPredicate<A, B, T> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) {
                    return triPredicate.test(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final BiPredicate<T, U> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return (Throwables.BiPredicate<T, U, E>) biPredicate;
        }

        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> pp(final A a, final TriPredicate<A, T, U> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Throwables.BiPredicate<T, U, E>() {
                @Override
                public boolean test(T t, U u) {
                    return triPredicate.test(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriPredicate<A, B, C, E> pp(final TriPredicate<A, B, C> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return (Throwables.TriPredicate<A, B, C, E>) triPredicate;
        }

        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> cc(final Consumer<T> consumer) {
            N.checkArgNotNull(consumer);

            return (Throwables.Consumer<T, E>) consumer;
        }

        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final BiConsumer<A, T> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    biConsumer.accept(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Consumer<T, E> cc(final A a, final B b, final TriConsumer<A, B, T> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    triConsumer.accept(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final BiConsumer<T, U> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return (Throwables.BiConsumer<T, U, E>) biConsumer;
        }

        @Beta
        public static <A, T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> cc(final A a, final TriConsumer<A, T, U> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) {
                    triConsumer.accept(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, E extends Throwable> Throwables.TriConsumer<A, B, C, E> cc(final TriConsumer<A, B, C> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return (Throwables.TriConsumer<A, B, C, E>) triConsumer;
        }

        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final Function<T, R> function) {
            N.checkArgNotNull(function);

            return (Throwables.Function<T, R, E>) function;
        }

        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final BiFunction<A, T, R> biFunction) {
            N.checkArgNotNull(biFunction);

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) {
                    return biFunction.apply(a, t);
                }
            };
        }

        @Beta
        public static <A, B, T, R, E extends Throwable> Throwables.Function<T, R, E> ff(final A a, final B b, final TriFunction<A, B, T, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) {
                    return triFunction.apply(a, b, t);
                }
            };
        }

        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final BiFunction<T, U, R> biFunction) {
            N.checkArgNotNull(biFunction);

            return (Throwables.BiFunction<T, U, R, E>) biFunction;
        }

        @Beta
        public static <A, T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> ff(final A a, final TriFunction<A, T, U, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Throwables.BiFunction<T, U, R, E>() {
                @Override
                public R apply(T t, U u) {
                    return triFunction.apply(a, t, u);
                }
            };
        }

        @Beta
        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> ff(final TriFunction<A, B, C, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return (Throwables.TriFunction<A, B, C, R, E>) triFunction;
        }

        /**
         * Synchronized {@code Predicate}.
         *
         * @param <T>
         * @param <E>
         * @param mutex to synchronized on
         * @param predicate
         * @return
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final Throwables.Predicate<T, E> predicate) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(predicate, "predicate");

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    synchronized (mutex) {
                        return predicate.test(t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Predicate}.
         *
         * @param <A>
         * @param <T>
         * @param mutex to synchronized on
         * @param a
         * @param biPredicate
         * @return
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final A a,
                final Throwables.BiPredicate<A, T, E> biPredicate) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biPredicate, "biPredicate");

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    synchronized (mutex) {
                        return biPredicate.test(a, t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Predicate}.
         *
         * @param <A>
         * @param <B>
         * @param <T>
         * @param <E>
         * @param mutex to synchronized on
         * @param a
         * @param b
         * @param triPredicate
         * @return
         */
        @Beta
        public static <A, B, T, E extends Throwable> Throwables.Predicate<T, E> sp(final Object mutex, final A a, final B b,
                final Throwables.TriPredicate<A, B, T, E> triPredicate) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(triPredicate, "triPredicate");

            return new Throwables.Predicate<T, E>() {
                @Override
                public boolean test(T t) throws E {
                    synchronized (mutex) {
                        return triPredicate.test(a, b, t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code BiPredicate}.
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param mutex to synchronized on
         * @param biPredicate
         * @return
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiPredicate<T, U, E> sp(final Object mutex, final Throwables.BiPredicate<T, U, E> biPredicate) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biPredicate, "biPredicate");

            return new Throwables.BiPredicate<T, U, E>() {
                @Override
                public boolean test(T t, U u) throws E {
                    synchronized (mutex) {
                        return biPredicate.test(t, u);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Consumer}.
         *
         * @param <T>
         * @param <E>
         * @param mutex to synchronized on
         * @param consumer
         * @return
         */
        @Beta
        public static <T, E extends Throwable> Throwables.Consumer<T, E> sc(final Object mutex, final Throwables.Consumer<T, E> consumer) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(consumer, "consumer");

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    synchronized (mutex) {
                        consumer.accept(t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Consumer}.
         *
         * @param <A>
         * @param <T>
         * @param <E>
         * @param mutex to synchronized on
         * @param a
         * @param biConsumer
         * @return
         */
        @Beta
        public static <A, T, E extends Throwable> Throwables.Consumer<T, E> sc(final Object mutex, final A a, final Throwables.BiConsumer<A, T, E> biConsumer) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biConsumer, "biConsumer");

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    synchronized (mutex) {
                        biConsumer.accept(a, t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code BiConsumer}.
         *
         * @param <T>
         * @param <U>
         * @param <E>
         * @param mutex to synchronized on
         * @param biConsumer
         * @return
         */
        @Beta
        public static <T, U, E extends Throwable> Throwables.BiConsumer<T, U, E> sc(final Object mutex, final Throwables.BiConsumer<T, U, E> biConsumer) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biConsumer, "biConsumer");

            return new Throwables.BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) throws E {
                    synchronized (mutex) {
                        biConsumer.accept(t, u);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Function}.
         *
         * @param <T>
         * @param <R>
         * @param <E>
         * @param mutex to synchronized on
         * @param function
         * @return
         */
        @Beta
        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> sf(final Object mutex, final Throwables.Function<T, R, E> function) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(function, "function");

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) throws E {
                    synchronized (mutex) {
                        return function.apply(t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code Function}.
         *
         * @param <A>
         * @param <T>
         * @param <R>
         * @param <E>
         * @param mutex to synchronized on
         * @param a
         * @param biFunction
         * @return
         */
        @Beta
        public static <A, T, R, E extends Throwable> Throwables.Function<T, R, E> sf(final Object mutex, final A a,
                final Throwables.BiFunction<A, T, R, E> biFunction) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biFunction, "biFunction");

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) throws E {
                    synchronized (mutex) {
                        return biFunction.apply(a, t);
                    }
                }
            };
        }

        /**
         * Synchronized {@code BiFunction}.
         *
         * @param <T>
         * @param <U>
         * @param <R>
         * @param <E>
         * @param mutex to synchronized on
         * @param biFunction
         * @return
         */
        @Beta
        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> sf(final Object mutex,
                final Throwables.BiFunction<T, U, R, E> biFunction) {
            N.checkArgNotNull(mutex, "mutex");
            N.checkArgNotNull(biFunction, "biFunction");

            return new Throwables.BiFunction<T, U, R, E>() {
                @Override
                public R apply(T t, U u) throws E {
                    synchronized (mutex) {
                        return biFunction.apply(t, u);
                    }
                }
            };
        }

        public static <T, E extends Throwable> Throwables.Function<T, Void, E> c2f(final Throwables.Consumer<T, E> consumer) {
            N.checkArgNotNull(consumer);

            return new Throwables.Function<T, Void, E>() {
                @Override
                public Void apply(T t) throws E {
                    consumer.accept(t);

                    return null;
                }
            };
        }

        public static <T, R, E extends Throwable> Throwables.Function<T, R, E> c2f(final Throwables.Consumer<T, E> consumer, final R valueToReturn) {
            N.checkArgNotNull(consumer);

            return new Throwables.Function<T, R, E>() {
                @Override
                public R apply(T t) throws E {
                    consumer.accept(t);

                    return valueToReturn;
                }
            };
        }

        public static <T, U, E extends Throwable> Throwables.BiFunction<T, U, Void, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return new Throwables.BiFunction<T, U, Void, E>() {
                @Override
                public Void apply(T t, U u) throws E {
                    biConsumer.accept(t, u);

                    return null;
                }
            };
        }

        public static <T, U, R, E extends Throwable> Throwables.BiFunction<T, U, R, E> c2f(final Throwables.BiConsumer<T, U, E> biConsumer,
                final R valueToReturn) {
            N.checkArgNotNull(biConsumer);

            return new Throwables.BiFunction<T, U, R, E>() {
                @Override
                public R apply(T t, U u) throws E {
                    biConsumer.accept(t, u);

                    return valueToReturn;
                }
            };
        }

        public static <A, B, C, E extends Throwable> Throwables.TriFunction<A, B, C, Void, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.TriFunction<A, B, C, Void, E>() {
                @Override
                public Void apply(A a, B b, C c) throws E {
                    triConsumer.accept(a, b, c);

                    return null;
                }
            };
        }

        public static <A, B, C, R, E extends Throwable> Throwables.TriFunction<A, B, C, R, E> c2f(final Throwables.TriConsumer<A, B, C, E> triConsumer,
                final R valueToReturn) {
            N.checkArgNotNull(triConsumer);

            return new Throwables.TriFunction<A, B, C, R, E>() {
                @Override
                public R apply(A a, B b, C c) throws E {
                    triConsumer.accept(a, b, c);

                    return valueToReturn;
                }
            };
        }

        public static <T, R, E extends Throwable> Throwables.Consumer<T, E> f2c(final Throwables.Function<T, R, E> func) {
            N.checkArgNotNull(func);

            return new Throwables.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    func.apply(t);
                }
            };
        }

        public static <T, U, R, E extends Throwable> Throwables.BiConsumer<T, U, E> f2c(final Throwables.BiFunction<T, U, R, E> func) {
            N.checkArgNotNull(func);

            return new Throwables.BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) throws E {
                    func.apply(t, u);
                }
            };
        }

        public static <A, B, C, R, E extends Throwable> Throwables.TriConsumer<A, B, C, E> f2c(final Throwables.TriFunction<A, B, C, R, E> func) {
            N.checkArgNotNull(func);

            return new Throwables.TriConsumer<A, B, C, E>() {
                @Override
                public void accept(A a, B b, C c) throws E {
                    func.apply(a, b, c);
                }
            };
        }

        public static <E extends Throwable> Throwables.Callable<Void, E> r2c(final Throwables.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            return new Throwables.Callable<Void, E>() {
                @Override
                public Void call() throws E {
                    runnable.run();
                    return null;
                }
            };
        }

        public static <R, E extends Throwable> Throwables.Callable<R, E> r2c(final Throwables.Runnable<E> runnable, final R valueToReturn) {
            N.checkArgNotNull(runnable);

            return new Throwables.Callable<R, E>() {
                @Override
                public R call() throws E {
                    runnable.run();
                    return valueToReturn;
                }
            };
        }

        public static <R, E extends Throwable> Throwables.Runnable<E> c2r(final Throwables.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            return new Throwables.Runnable<E>() {
                @Override
                public void run() throws E {
                    callable.call();
                }
            };
        }

        public static <E extends Throwable> Throwables.Runnable<E> rr(final Runnable runnable) {
            return (Throwables.Runnable<E>) runnable;
        }

        public static <R, E extends Throwable> Throwables.Callable<R, E> cc(final Callable<R> callable) {
            return (Throwables.Callable<R, E>) callable;
        }

        public static <E extends Throwable> Throwables.Runnable<E> r(final Throwables.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            return runnable;
        }

        public static <R, E extends Throwable> Throwables.Callable<R, E> c(final Throwables.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            return callable;
        }

        public static <E extends Throwable> Throwables.Runnable<E> jr2r(final java.lang.Runnable runnable) {
            N.checkArgNotNull(runnable);

            if (runnable instanceof Throwables.Runnable) {
                return (Throwables.Runnable<E>) runnable;
            }

            return new Throwables.Runnable<E>() {
                @Override
                public void run() {
                    runnable.run();
                }
            };
        }

        public static <E extends Throwable> java.lang.Runnable r2jr(final Throwables.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            if (runnable instanceof java.lang.Runnable) {
                return (java.lang.Runnable) runnable;
            }

            return new java.lang.Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } catch (Throwable e) {
                        throw N.toRuntimeException(e);
                    }
                }
            };
        }

        public static <R> Throwables.Callable<R, Exception> jc2c(final java.util.concurrent.Callable<R> callable) {
            N.checkArgNotNull(callable);

            if (callable instanceof Throwables.Callable) {
                return (Throwables.Callable<R, Exception>) callable;
            }

            return new Throwables.Callable<R, Exception>() {
                @Override
                public R call() throws Exception {
                    return callable.call();
                }
            };
        }

        public static <R, E extends Exception> java.util.concurrent.Callable<R> c2jc(final Throwables.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            if (callable instanceof java.util.concurrent.Callable) {
                return (java.util.concurrent.Callable<R>) callable;
            }

            return new java.util.concurrent.Callable<R>() {
                @Override
                public R call() throws Exception {
                    return callable.call();
                }
            };
        }

        /**
         * 
         * @param <E>
         * @param runnable
         * @return
         * @deprecated replaced by {@link #r(com.landawn.abacus.util.Throwables.Runnable)}
         */
        @Deprecated
        public static <E extends Throwable> Throwables.Runnable<E> runnable(final Throwables.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            return runnable;
        }

        /**
         * 
         * @param <R>
         * @param <E>
         * @param callable
         * @return
         * @deprecated replaced by {@link #c(com.landawn.abacus.util.Throwables.Callable)}
         */
        @Deprecated
        public static <R, E extends Throwable> Throwables.Callable<R, E> callable(final Throwables.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            return callable;
        }
    }
}
