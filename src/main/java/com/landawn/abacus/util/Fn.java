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
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
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
import com.landawn.abacus.util.function.ByteConsumer;
import com.landawn.abacus.util.function.ByteFunction;
import com.landawn.abacus.util.function.BytePredicate;
import com.landawn.abacus.util.function.CharBiFunction;
import com.landawn.abacus.util.function.CharBiPredicate;
import com.landawn.abacus.util.function.CharConsumer;
import com.landawn.abacus.util.function.CharFunction;
import com.landawn.abacus.util.function.CharPredicate;
import com.landawn.abacus.util.function.Consumer;
import com.landawn.abacus.util.function.DoubleBiFunction;
import com.landawn.abacus.util.function.DoubleBiPredicate;
import com.landawn.abacus.util.function.DoubleConsumer;
import com.landawn.abacus.util.function.DoubleFunction;
import com.landawn.abacus.util.function.DoublePredicate;
import com.landawn.abacus.util.function.FloatBiFunction;
import com.landawn.abacus.util.function.FloatBiPredicate;
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
import com.landawn.abacus.util.function.IntConsumer;
import com.landawn.abacus.util.function.IntFunction;
import com.landawn.abacus.util.function.IntPredicate;
import com.landawn.abacus.util.function.LongBiFunction;
import com.landawn.abacus.util.function.LongBiPredicate;
import com.landawn.abacus.util.function.LongConsumer;
import com.landawn.abacus.util.function.LongFunction;
import com.landawn.abacus.util.function.LongPredicate;
import com.landawn.abacus.util.function.LongSupplier;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.QuadFunction;
import com.landawn.abacus.util.function.ShortBiFunction;
import com.landawn.abacus.util.function.ShortBiPredicate;
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

// TODO: Auto-generated Javadoc
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
public abstract class Fn extends Comparators {

    /** The Constant NONE. */
    private static final Object NONE = new Object();

    /** The Constant timer. */
    private static final Timer timer = new Timer();

    /** The Constant FACTORY_OF_MAP. */
    @SuppressWarnings("rawtypes")
    public static final IntFunction<Map<String, Object>> FACTORY_OF_MAP = (IntFunction) Factory.MAP_FACTORY;

    /** The Constant FACTORY_OF_LINKED_HASH_MAP. */
    @SuppressWarnings("rawtypes")
    public static final IntFunction<LinkedHashMap<String, Object>> FACTORY_OF_LINKED_HASH_MAP = (IntFunction) Factory.LINKED_HASH_MAP_FACTORY;

    /** The Constant SUPPLIER_OF_MAP. */
    @SuppressWarnings("rawtypes")
    public static final Supplier<Map<String, Object>> SUPPLIER_OF_MAP = (Supplier) Suppliers.MAP;

    /** The Constant SUPPLIER_OF_LINKED_HASH_MAP. */
    @SuppressWarnings("rawtypes")
    public static final Supplier<LinkedHashMap<String, Object>> SUPPLIER_OF_LINKED_HASH_MAP = (Supplier) Suppliers.LINKED_HASH_MAP;

    /** The Constant EMPTY_ACTION. */
    private static final com.landawn.abacus.util.function.Runnable EMPTY_ACTION = new com.landawn.abacus.util.function.Runnable() {
        @Override
        public void run() {
        }
    };

    /** The Constant DO_NOTHING. */
    @SuppressWarnings("rawtypes")
    private static final Consumer DO_NOTHING = new Consumer() {
        @Override
        public void accept(Object value) {
            // do nothing.
        }
    };

    /** The Constant CLOSE. */
    private static final Consumer<AutoCloseable> CLOSE = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable value) {
            IOUtil.close(value);
        }
    };

    /** The Constant CLOSE_QUIETLY. */
    private static final Consumer<AutoCloseable> CLOSE_QUIETLY = new Consumer<AutoCloseable>() {
        @Override
        public void accept(AutoCloseable value) {
            IOUtil.closeQuietly(value);
        }
    };

    /** The Constant PRINTLN_EQUAL. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EQUAL = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "=", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_HYPHEN. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_HYPHEN = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "-", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_UNDERSCORE. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_UNDERSCORE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), "_", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_COLON. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ":", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_COLON_SPACE. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COLON_SPACE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ": ", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_COMMA. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ",", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_COMMA_SPACE. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_COMMA_SPACE = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), ", ", N.toString(value)));
        }
    };

    /** The Constant PRINTLN_EMPTY. */
    @SuppressWarnings("rawtypes")
    private static final BiConsumer PRINTLN_EMPTY = new BiConsumer() {
        @Override
        public void accept(Object key, Object value) {
            N.println(StringUtil.concat(N.toString(key), N.toString(value)));
        }
    };

    /** The Constant PRINTLN. */
    @SuppressWarnings("rawtypes")
    private static final Consumer PRINTLN = new Consumer() {
        @Override
        public void accept(Object value) {
            N.println(value);
        }
    };

    /** The Constant TO_STRING. */
    @SuppressWarnings("rawtypes")
    private static final Function TO_STRING = new Function() {
        @Override
        public Object apply(Object t) {
            return N.toString(t);
        }
    };

    /** The Constant TO_CAMEL_CASE. */
    private static final Function<String, String> TO_CAMEL_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toCamelCase(t);
        }
    };

    /** The Constant TO_LOWER_CASE. */
    private static final Function<String, String> TO_LOWER_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toLowerCase(t);
        }
    };

    /** The Constant TO_LOWER_CASE_WITH_UNDERSCORE. */
    private static final Function<String, String> TO_LOWER_CASE_WITH_UNDERSCORE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toLowerCaseWithUnderscore(t);
        }
    };

    /** The Constant TO_UPPER_CASE. */
    private static final Function<String, String> TO_UPPER_CASE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toUpperCase(t);
        }
    };

    /** The Constant TO_UPPER_CASE_WITH_UNDERSCORE. */
    private static final Function<String, String> TO_UPPER_CASE_WITH_UNDERSCORE = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return StringUtil.toUpperCaseWithUnderscore(t);
        }
    };

    /** The Constant COMPARE. */
    @SuppressWarnings("rawtypes")
    private static final BiFunction COMPARE = new BiFunction<Comparable, Comparable, Integer>() {
        @Override
        public Integer apply(Comparable a, Comparable b) {
            return N.compare(a, b);
        }
    };

    /** The Constant IDENTITY. */
    @SuppressWarnings("rawtypes")
    private static final Function IDENTITY = new Function() {
        @Override
        public Object apply(Object t) {
            return t;
        }
    };

    /** The Constant TRIM. */
    private static final Function<String, String> TRIM = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? null : t.trim();
        }
    };

    /** The Constant TRIM_TO_EMPTY. */
    private static final Function<String, String> TRIM_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? "" : t.trim();
        }
    };

    /** The Constant TRIM_TO_NULL. */
    private static final Function<String, String> TRIM_TO_NULL = new Function<String, String>() {
        @Override
        public String apply(String t) {
            if (t == null || (t = t.trim()).length() == 0) {
                return null;
            }

            return t;
        }
    };

    /** The Constant NULL_TO_EMPTY. */
    private static final Function<String, String> NULL_TO_EMPTY = new Function<String, String>() {
        @Override
        public String apply(String t) {
            return t == null ? N.EMPTY_STRING : t;
        }
    };

    /** The Constant NULL_TO_EMPTY_L. */
    @SuppressWarnings("rawtypes")
    private static final Function<List, List> NULL_TO_EMPTY_L = new Function<List, List>() {
        @Override
        public List apply(List t) {
            return t == null ? N.emptyList() : t;
        }
    };

    /** The Constant NULL_TO_EMPTY_S. */
    @SuppressWarnings("rawtypes")
    private static final Function<Set, Set> NULL_TO_EMPTY_S = new Function<Set, Set>() {
        @Override
        public Set apply(Set t) {
            return t == null ? N.emptySet() : t;
        }
    };

    /** The Constant NULL_TO_EMPTY_M. */
    @SuppressWarnings("rawtypes")
    private static final Function<Map, Map> NULL_TO_EMPTY_M = new Function<Map, Map>() {
        @Override
        public Map apply(Map t) {
            return t == null ? N.emptyMap() : t;
        }
    };

    /** The Constant LENGTH. */
    private static final Function<CharSequence, Integer> LENGTH = new Function<CharSequence, Integer>() {
        @Override
        public Integer apply(CharSequence t) {
            return t == null ? 0 : t.length();
        }
    };

    /** The Constant LEN. */
    private static final Function<Object[], Integer> LEN = new Function<Object[], Integer>() {
        @Override
        public Integer apply(Object[] t) {
            return t == null ? 0 : t.length;
        }
    };

    /** The Constant SIZE. */
    @SuppressWarnings("rawtypes")
    private static final Function<Collection, Integer> SIZE = new Function<Collection, Integer>() {
        @Override
        public Integer apply(Collection t) {
            return t == null ? 0 : t.size();
        }
    };

    /** The Constant SIZE_M. */
    @SuppressWarnings("rawtypes")
    private static final Function<Map, Integer> SIZE_M = new Function<Map, Integer>() {
        @Override
        public Integer apply(Map t) {
            return t == null ? 0 : t.size();
        }
    };

    /** The Constant KEY. */
    private static final Function<Map.Entry<Object, Object>, Object> KEY = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getKey();
        }
    };

    /** The Constant VALUE. */
    private static final Function<Map.Entry<Object, Object>, Object> VALUE = new Function<Map.Entry<Object, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Object, Object> t) {
            return t.getValue();
        }
    };

    /** The Constant INVERSE. */
    private static final Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>> INVERSE = new Function<Map.Entry<Object, Object>, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Map.Entry<Object, Object> t) {
            return new SimpleImmutableEntry<>(t.getValue(), t.getKey());
        }
    };

    /** The Constant ENTRY. */
    private static final BiFunction<Object, Object, Map.Entry<Object, Object>> ENTRY = new BiFunction<Object, Object, Map.Entry<Object, Object>>() {
        @Override
        public Map.Entry<Object, Object> apply(Object key, Object value) {
            return new SimpleImmutableEntry<>(key, value);
        }
    };

    /** The Constant PAIR. */
    private static final BiFunction<Object, Object, Pair<Object, Object>> PAIR = new BiFunction<Object, Object, Pair<Object, Object>>() {
        @Override
        public Pair<Object, Object> apply(Object key, Object value) {
            return Pair.of(key, value);
        }
    };

    /** The Constant TRIPLE. */
    private static final TriFunction<Object, Object, Object, Triple<Object, Object, Object>> TRIPLE = new TriFunction<Object, Object, Object, Triple<Object, Object, Object>>() {
        @Override
        public Triple<Object, Object, Object> apply(Object a, Object b, Object c) {
            return Triple.of(a, b, c);
        }
    };

    /** The Constant TUPLE_1. */
    private static final Function<Object, Tuple1<Object>> TUPLE_1 = new Function<Object, Tuple1<Object>>() {
        @Override
        public Tuple1<Object> apply(Object t) {
            return Tuple.of(t);
        }
    };

    /** The Constant TUPLE_2. */
    private static final BiFunction<Object, Object, Tuple2<Object, Object>> TUPLE_2 = new BiFunction<Object, Object, Tuple2<Object, Object>>() {
        @Override
        public Tuple2<Object, Object> apply(Object t, Object u) {
            return Tuple.of(t, u);
        }
    };

    /** The Constant TUPLE_3. */
    private static final TriFunction<Object, Object, Object, Tuple3<Object, Object, Object>> TUPLE_3 = new TriFunction<Object, Object, Object, Tuple3<Object, Object, Object>>() {
        @Override
        public Tuple3<Object, Object, Object> apply(Object a, Object b, Object c) {
            return Tuple.of(a, b, c);
        }
    };

    /** The Constant TUPLE_4. */
    private static final QuadFunction<Object, Object, Object, Object, Tuple4<Object, Object, Object, Object>> TUPLE_4 = new QuadFunction<Object, Object, Object, Object, Tuple4<Object, Object, Object, Object>>() {
        @Override
        public Tuple4<Object, Object, Object, Object> apply(Object a, Object b, Object c, Object d) {
            return Tuple.of(a, b, c, d);
        }
    };

    /** The Constant ALWAYS_TRUE. */
    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_TRUE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return true;
        }
    };

    /** The Constant ALWAYS_FALSE. */
    @SuppressWarnings("rawtypes")
    private static final Predicate ALWAYS_FALSE = new Predicate() {
        @Override
        public boolean test(Object value) {
            return false;
        }
    };

    /** The Constant IS_NULL. */
    @SuppressWarnings("rawtypes")
    private static final Predicate IS_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value == null;
        }
    };

    /** The Constant IS_NULL_OR_EMPTY. */
    private static final Predicate<CharSequence> IS_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value == null || value.length() == 0;
        }
    };

    /** The Constant IS_NULL_OR_EMPTY_OR_BLANK. */
    private static final Predicate<CharSequence> IS_NULL_OR_EMPTY_OR_BLANK = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return N.isNullOrEmptyOrBlank(value);
        }
    };

    /** The Constant NOT_NULL. */
    @SuppressWarnings("rawtypes")
    private static final Predicate NOT_NULL = new Predicate() {
        @Override
        public boolean test(Object value) {
            return value != null;
        }
    };

    /** The Constant NOT_NULL_OR_EMPTY. */
    private static final Predicate<CharSequence> NOT_NULL_OR_EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return value != null && value.length() > 0;
        }
    };

    /** The Constant NOT_NULL_OR_EMPTY_OR_BLANK. */
    private static final Predicate<CharSequence> NOT_NULL_OR_EMPTY_OR_BLANK = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence value) {
            return N.notNullOrEmptyOrBlank(value);
        }
    };

    /** The Constant IS_FILE. */
    private static final Predicate<File> IS_FILE = new Predicate<File>() {
        @Override
        public boolean test(File value) {
            return value.isFile();
        }
    };

    /** The Constant IS_DIRECTORY. */
    private static final Predicate<File> IS_DIRECTORY = new Predicate<File>() {
        @Override
        public boolean test(File value) {
            return value.isDirectory();
        }
    };

    /**
     * Instantiates a new fn.
     */
    protected Fn() {
        super();
        // for extention.
    }

    /**
     * Gets the.
     *
     * @param <T> the generic type
     * @param supplier the supplier
     * @return the t
     */
    public static <T> T get(final Supplier<T> supplier) {
        return supplier.get();
    }

    /**
     * Returns a {@code Supplier} which returns a single instance created by calling the specified {@code supplier.get()}.
     *
     * @param <T> the generic type
     * @param supplier the supplier
     * @return the supplier
     */
    public static <T> Supplier<T> memoize(final Supplier<T> supplier) {
        return new Supplier<T>() {
            private volatile boolean initialized = false;
            private T instance = null;

            @Override
            public T get() {
                if (initialized == false) {
                    synchronized (this) {
                        if (initialized == false) {
                            instance = supplier.get();
                        }

                    }
                }

                return instance;
            }
        };
    }

    /**
     * Memoize.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param func the func
     * @return the function
     */
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
     * @param <T> the generic type
     * @param <C> the generic type
     * @param supplier the supplier
     * @return the supplier<? extends c>
     * @see {@code Stream.split/sliding};
     */
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
     * @param <T> the generic type
     * @param <C> the generic type
     * @param supplier the supplier
     * @return the int function<? extends c>
     * @see {@code Stream.split/sliding};
     */
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
     * Close.
     *
     * @param closeable the closeable
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable close(final AutoCloseable closeable) {
        return new com.landawn.abacus.util.function.Runnable() {
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
     * Close all.
     *
     * @param a the a
     * @return the com.landawn.abacus.util.function. runnable
     */
    @SafeVarargs
    public static com.landawn.abacus.util.function.Runnable closeAll(final AutoCloseable... a) {
        return new com.landawn.abacus.util.function.Runnable() {
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
     * Close all.
     *
     * @param c the c
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable closeAll(final Collection<? extends AutoCloseable> c) {
        return new com.landawn.abacus.util.function.Runnable() {
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
     * Close quietly.
     *
     * @param closeable the closeable
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable closeQuietly(final AutoCloseable closeable) {
        return new com.landawn.abacus.util.function.Runnable() {
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
     * @param a the a
     * @return the com.landawn.abacus.util.function. runnable
     */
    @SafeVarargs
    public static com.landawn.abacus.util.function.Runnable closeAllQuietly(final AutoCloseable... a) {
        return new com.landawn.abacus.util.function.Runnable() {
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
     * @param c the c
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable closeAllQuietly(final Collection<? extends AutoCloseable> c) {
        return new com.landawn.abacus.util.function.Runnable() {
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

    /**
     * Empty action.
     *
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable emptyAction() {
        return EMPTY_ACTION;
    }

    /**
     * Do nothing.
     *
     * @param <T> the generic type
     * @return the consumer
     */
    public static <T> Consumer<T> doNothing() {
        return DO_NOTHING;
    }

    /**
     * Close.
     *
     * @param <T> the generic type
     * @return the consumer
     */
    public static <T extends AutoCloseable> Consumer<T> close() {
        return (Consumer<T>) CLOSE;
    }

    /**
     * Close quietly.
     *
     * @param <T> the generic type
     * @return the consumer
     */
    public static <T extends AutoCloseable> Consumer<T> closeQuietly() {
        return (Consumer<T>) CLOSE_QUIETLY;
    }

    /**
     * Sleep.
     *
     * @param <T> the generic type
     * @param millis the millis
     * @return the consumer
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
     * Sleep uninterruptibly.
     *
     * @param <T> the generic type
     * @param millis the millis
     * @return the consumer
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
     * Println.
     *
     * @param <T> the generic type
     * @return the consumer
     */
    public static <T> Consumer<T> println() {
        return PRINTLN;
    }

    /**
     * Println.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param separator the separator
     * @return the bi consumer
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
     * To str.
     *
     * @param <T> the generic type
     * @return the function
     */
    public static <T> Function<T, String> toStr() {
        return TO_STRING;
    }

    /**
     * To camel case.
     *
     * @return the function
     */
    public static Function<String, String> toCamelCase() {
        return TO_CAMEL_CASE;
    }

    /**
     * To lower case.
     *
     * @return the function
     */
    public static Function<String, String> toLowerCase() {
        return TO_LOWER_CASE;
    }

    /**
     * To lower case with underscore.
     *
     * @return the function
     */
    public static Function<String, String> toLowerCaseWithUnderscore() {
        return TO_LOWER_CASE_WITH_UNDERSCORE;
    }

    /**
     * To upper case.
     *
     * @return the function
     */
    public static Function<String, String> toUpperCase() {
        return TO_UPPER_CASE;
    }

    /**
     * To upper case with underscore.
     *
     * @return the function
     */
    public static Function<String, String> toUpperCaseWithUnderscore() {
        return TO_UPPER_CASE_WITH_UNDERSCORE;
    }

    /**
     * Identity.
     *
     * @param <T> the generic type
     * @return the function
     */
    public static <T> Function<T, T> identity() {
        return IDENTITY;
    }

    /**
     * Keyed.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @param keyMapper the key mapper
     * @return the function
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

    /** The Constant VAL. */
    private static final Function<Keyed<?, Object>, Object> VAL = new Function<Keyed<?, Object>, Object>() {
        @Override
        public Object apply(Keyed<?, Object> t) {
            return t.val();
        }
    };

    /** The Constant KK. */
    private static final Function<Map.Entry<Keyed<Object, Object>, Object>, Object> KK = new Function<Map.Entry<Keyed<Object, Object>, Object>, Object>() {
        @Override
        public Object apply(Map.Entry<Keyed<Object, Object>, Object> t) {
            return t.getKey().val();
        }
    };

    /**
     * Val.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Keyed<K, T>, T> val() {
        return (Function) VAL;
    }

    /**
     * Kk.
     *
     * @param <T> the generic type
     * @param <K> the key type
     * @param <V> the value type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T, K, V> Function<Map.Entry<Keyed<K, T>, V>, T> kk() {
        return (Function) KK;
    }

    /** The Constant WRAP. */
    private static final Function<Object, Wrapper<Object>> WRAP = new Function<Object, Wrapper<Object>>() {
        @Override
        public Wrapper<Object> apply(Object t) {
            return Wrapper.of(t);
        }
    };

    /**
     * Wrap.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Wrapper<T>> wrap() {
        return (Function) WRAP;
    }

    /**
     * Wrap.
     *
     * @param <T> the generic type
     * @param hashFunction the hash function
     * @param equalsFunction the equals function
     * @return the function
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

    /** The Constant UNWRAP. */
    private static final Function<Wrapper<Object>, Object> UNWRAP = new Function<Wrapper<Object>, Object>() {
        @Override
        public Object apply(Wrapper<Object> t) {
            return t.value();
        }
    };

    /**
     * Unwrap.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, T> Function<Wrapper<T>, T> unwrap() {
        return (Function) UNWRAP;
    }

    /**
     * Key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, K> key() {
        return (Function) KEY;
    }

    /**
     * Value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, V> value() {
        return (Function) VALUE;
    }

    /**
     * Inverse.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Entry<K, V>, Entry<V, K>> inverse() {
        return (Function) INVERSE;
    }

    /**
     * Entry.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the bi function
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> BiFunction<K, V, Map.Entry<K, V>> entry() {
        return (BiFunction) ENTRY;
    }

    /**
     * Entry.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @param key the key
     * @return the function
     */
    public static <K, T> Function<T, Map.Entry<K, T>> entry(final K key) {
        return new Function<T, Map.Entry<K, T>>() {
            @Override
            public Entry<K, T> apply(T t) {
                return new SimpleImmutableEntry<>(key, t);
            }
        };
    }

    /**
     * Entry.
     *
     * @param <K> the key type
     * @param <T> the generic type
     * @param keyMapper the key mapper
     * @return the function
     */
    public static <K, T> Function<T, Map.Entry<K, T>> entry(final Function<? super T, K> keyMapper) {
        N.checkArgNotNull(keyMapper);

        return new Function<T, Map.Entry<K, T>>() {
            @Override
            public Entry<K, T> apply(T t) {
                return new SimpleImmutableEntry<>(keyMapper.apply(t), t);
            }
        };
    }

    /**
     * Pair.
     *
     * @param <L> the generic type
     * @param <R> the generic type
     * @return the bi function
     */
    @SuppressWarnings("rawtypes")
    public static <L, R> BiFunction<L, R, Pair<L, R>> pair() {
        return (BiFunction) PAIR;
    }

    /**
     * Triple.
     *
     * @param <L> the generic type
     * @param <M> the generic type
     * @param <R> the generic type
     * @return the tri function
     */
    @SuppressWarnings("rawtypes")
    public static <L, M, R> TriFunction<L, M, R, Triple<L, M, R>> triple() {
        return (TriFunction) TRIPLE;
    }

    /**
     * Tuple 1.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Tuple1<T>> tuple1() {
        return (Function) TUPLE_1;
    }

    /**
     * Tuple 2.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @return the bi function
     */
    @SuppressWarnings("rawtypes")
    public static <T, U> BiFunction<T, U, Tuple2<T, U>> tuple2() {
        return (BiFunction) TUPLE_2;
    }

    /**
     * Tuple 3.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @return the tri function
     */
    @SuppressWarnings("rawtypes")
    public static <A, B, C> TriFunction<A, B, C, Tuple3<A, B, C>> tuple3() {
        return (TriFunction) TUPLE_3;
    }

    /**
     * Tuple 4.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @return the quad function
     */
    @SuppressWarnings({ "rawtypes" })
    public static <A, B, C, D> QuadFunction<A, B, C, D, Tuple4<A, B, C, D>> tuple4() {
        return (QuadFunction) TUPLE_4;
    }

    /**
     * Trim.
     *
     * @return the function
     */
    public static Function<String, String> trim() {
        return TRIM;
    }

    /**
     * Trim to empty.
     *
     * @return the function
     */
    public static Function<String, String> trimToEmpty() {
        return TRIM_TO_EMPTY;
    }

    /**
     * Trim to null.
     *
     * @return the function
     */
    public static Function<String, String> trimToNull() {
        return TRIM_TO_NULL;
    }

    /**
     * Null to empty.
     *
     * @return the function
     */
    public static Function<String, String> nullToEmpty() {
        return NULL_TO_EMPTY;
    }

    /**
     * Null to empty L.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<List<T>, List<T>> nullToEmptyL() {
        return (Function) NULL_TO_EMPTY_L;
    }

    /**
     * Null to empty S.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<Set<T>, Set<T>> nullToEmptyS() {
        return (Function) NULL_TO_EMPTY_S;
    }

    /**
     * Null to empty M.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <K, V> Function<Map<K, V>, Map<K, V>> nullToEmptyM() {
        return (Function) NULL_TO_EMPTY_M;
    }

    /**
     * Length.
     *
     * @param <T> the generic type
     * @return the function
     */
    public static <T extends CharSequence> Function<T, Integer> length() {
        return (Function<T, Integer>) LENGTH;
    }

    /**
     * Len.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T[], Integer> len() {
        return (Function) LEN;
    }

    /**
     * Size.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Collection> Function<T, Integer> size() {
        return (Function<T, Integer>) SIZE;
    }

    /**
     * Size M.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Map> Function<T, Integer> sizeM() {
        return (Function<T, Integer>) SIZE_M;
    }

    /**
     * Cast.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param clazz the clazz
     * @return the function
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
     * Always true.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T> Predicate<T> alwaysTrue() {
        return ALWAYS_TRUE;
    }

    /**
     * Always false.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T> Predicate<T> alwaysFalse() {
        return ALWAYS_FALSE;
    }

    /**
     * Checks if is null.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T> Predicate<T> isNull() {
        return IS_NULL;
    }

    /**
     * Checks if is null or empty.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T extends CharSequence> Predicate<T> isNullOrEmpty() {
        return (Predicate<T>) IS_NULL_OR_EMPTY;
    }

    /**
     * Checks if is null or empty or blank.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T extends CharSequence> Predicate<T> isNullOrEmptyOrBlank() {
        return (Predicate<T>) IS_NULL_OR_EMPTY_OR_BLANK;
    }

    /**
     * Not null.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T> Predicate<T> notNull() {
        return NOT_NULL;
    }

    /**
     * Not null or empty.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T extends CharSequence> Predicate<T> notNullOrEmpty() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY;
    }

    /**
     * Not null or empty or blank.
     *
     * @param <T> the generic type
     * @return the predicate
     */
    public static <T extends CharSequence> Predicate<T> notNullOrEmptyOrBlank() {
        return (Predicate<T>) NOT_NULL_OR_EMPTY_OR_BLANK;
    }

    /**
     * Checks if is file.
     *
     * @return the predicate
     */
    public static Predicate<File> isFile() {
        return IS_FILE;
    }

    /**
     * Checks if is directory.
     *
     * @return the predicate
     */
    public static Predicate<File> isDirectory() {
        return IS_DIRECTORY;
    }

    /**
     * Equal.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
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
     * Not equal.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
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
     * Greater than.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> greaterThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) > 0;
            }
        };
    }

    /**
     * Greater equal.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> greaterEqual(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) >= 0;
            }
        };
    }

    /**
     * Less than.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> lessThan(final T target) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, target) < 0;
            }
        };
    }

    /**
     * Less equal.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> lessEqual(final T target) {
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
     * @param <T> the generic type
     * @param minValue the min value
     * @param maxValue the max value
     * @return the predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Predicate<T> between(final T minValue, final T maxValue) {
        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return N.compare(value, minValue) > 0 && N.compare(value, maxValue) < 0;
            }
        };
    }

    /**
     * In.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the predicate
     */
    public static <T> Predicate<T> in(final Collection<?> c) {
        N.checkArgNotNull(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return c != null && c.size() > 0 && c.contains(value);
            }
        };
    }

    /**
     * Not in.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the predicate
     */
    public static <T> Predicate<T> notIn(final Collection<?> c) {
        N.checkArgNotNull(c);

        return new Predicate<T>() {
            @Override
            public boolean test(T value) {
                return c == null || c.size() == 0 || !c.contains(value);
            }
        };
    }

    /**
     * Instance of.
     *
     * @param <T> the generic type
     * @param clazz the clazz
     * @return the predicate
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
     * Subtype of.
     *
     * @param clazz the clazz
     * @return the predicate
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
     * Starts with.
     *
     * @param prefix the prefix
     * @return the predicate
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
     * Ends with.
     *
     * @param suffix the suffix
     * @return the predicate
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
     * Contains.
     *
     * @param str the str
     * @return the predicate
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
     * @param prefix the prefix
     * @return the predicate
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
     * @param suffix the suffix
     * @return the predicate
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
     * Not contains.
     *
     * @param str the str
     * @return the predicate
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
     * Matches.
     *
     * @param pattern the pattern
     * @return the predicate
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
     * Equal.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @return the bi predicate
     */
    public static <T, U> BiPredicate<T, U> equal() {
        return BiPredicates.EQUAL;
    }

    /**
     * Not equal.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @return the bi predicate
     */
    public static <T, U> BiPredicate<T, U> notEqual() {
        return BiPredicates.NOT_EQUAL;
    }

    /**
     * Greater than.
     *
     * @param <T> the generic type
     * @return the bi predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> greaterThan() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_THAN;
    }

    /**
     * Greater equal.
     *
     * @param <T> the generic type
     * @return the bi predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> greaterEqual() {
        return (BiPredicate<T, T>) BiPredicates.GREATER_EQUAL;
    }

    /**
     * Less than.
     *
     * @param <T> the generic type
     * @return the bi predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> lessThan() {
        return (BiPredicate<T, T>) BiPredicates.LESS_THAN;
    }

    /**
     * Less equal.
     *
     * @param <T> the generic type
     * @return the bi predicate
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiPredicate<T, T> lessEqual() {
        return (BiPredicate<T, T>) BiPredicates.LESS_EQUAL;
    }

    /**
     * Not.
     *
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the predicate
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
     * Not.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param biPredicate the bi predicate
     * @return the bi predicate
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
     * Not.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param triPredicate the tri predicate
     * @return the tri predicate
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
     * And.
     *
     * @param first the first
     * @param second the second
     * @return the boolean supplier
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
     * And.
     *
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the boolean supplier
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
     * And.
     *
     * @param <T> the generic type
     * @param first the first
     * @param second the second
     * @return the predicate
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
     * And.
     *
     * @param <T> the generic type
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the predicate
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
     * And.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the predicate
     */
    public static <T> Predicate<T> and(final Collection<Predicate<? super T>> c) {
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
     * And.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param first the first
     * @param second the second
     * @return the bi predicate
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
     * And.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the bi predicate
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
     * And.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param c the c
     * @return the bi predicate
     */
    public static <T, U> BiPredicate<T, U> and(final List<BiPredicate<? super T, ? super U>> c) {
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
     * Or.
     *
     * @param first the first
     * @param second the second
     * @return the boolean supplier
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
     * Or.
     *
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the boolean supplier
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
     * Or.
     *
     * @param <T> the generic type
     * @param first the first
     * @param second the second
     * @return the predicate
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
     * Or.
     *
     * @param <T> the generic type
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the predicate
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
     * Or.
     *
     * @param <T> the generic type
     * @param c the c
     * @return the predicate
     */
    public static <T> Predicate<T> or(final Collection<Predicate<? super T>> c) {
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
     * Or.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param first the first
     * @param second the second
     * @return the bi predicate
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
     * Or.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param first the first
     * @param second the second
     * @param third the third
     * @return the bi predicate
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
     * Or.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param c the c
     * @return the bi predicate
     */
    public static <T, U> BiPredicate<T, U> or(final List<BiPredicate<? super T, ? super U>> c) {
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
     * @param predicate the predicate
     * @return the predicate
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
     * @param predicate the predicate
     * @return the predicate
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
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the predicate
     * @deprecated replaced by {@link Fn#p(Predicate)}.
     */
    @Deprecated
    static <T> Predicate<T> test(final Predicate<T> predicate) {
        return predicate;
    }

    /**
     * Returns the specified instance.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param predicate the predicate
     * @return the bi predicate
     * @deprecated replaced by {@link Fn#p(BiPredicate)}.
     */
    @Deprecated
    static <T, U> BiPredicate<T, U> test(final BiPredicate<T, U> predicate) {
        return predicate;
    }

    /**
     * Returns the specified instance.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param predicate the predicate
     * @return the tri predicate
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
     * @param consumer the consumer
     * @return the consumer
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
     * @param consumer the consumer
     * @return the consumer
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
     * @param <R> the generic type
     * @param func the func
     * @return the function
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
     * @param <R> the generic type
     * @param func the func
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param defaultValue the default value
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param mapperC the mapper C
     * @param defaultValue the default value
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param mapperC the mapper C
     * @param mapperD the mapper D
     * @param defaultValue the default value
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param supplier the supplier
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param mapperC the mapper C
     * @param supplier the supplier
     * @return the function
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <D> the generic type
     * @param <R> the generic type
     * @param mapperA the mapper A
     * @param mapperB the mapper B
     * @param mapperC the mapper C
     * @param mapperD the mapper D
     * @param supplier the supplier
     * @return the function
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
     * Map key.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <KK> the generic type
     * @param func the func
     * @return the function
     */
    public static <K, V, KK> Function<Map.Entry<K, V>, Map.Entry<KK, V>> mapKey(final Function<? super K, ? extends KK> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<KK, V>>() {
            @Override
            public Map.Entry<KK, V> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<KK, V>(func.apply(entry.getKey()), entry.getValue());
            }
        };
    }

    /**
     * Map value.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <VV> the generic type
     * @param func the func
     * @return the function
     */
    public static <K, V, VV> Function<Map.Entry<K, V>, Map.Entry<K, VV>> mapValue(final Function<? super V, ? extends VV> func) {
        N.checkArgNotNull(func);

        return new Function<Map.Entry<K, V>, Map.Entry<K, VV>>() {
            @Override
            public Map.Entry<K, VV> apply(Entry<K, V> entry) {
                return new SimpleImmutableEntry<K, VV>(entry.getKey(), func.apply(entry.getValue()));
            }
        };
    }

    /**
     * Test key val.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param predicate the predicate
     * @return the predicate
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
     * @param consumer the consumer
     * @return the consumer
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
     * @param <R> the generic type
     * @param func the func
     * @return the function
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

    /** The parse byte func. */
    private static Function<String, Byte> PARSE_BYTE_FUNC = new Function<String, Byte>() {
        @Override
        public Byte apply(String t) {
            return N.parseByte(t);
        }
    };

    /**
     * Parses the byte.
     *
     * @return the function
     */
    public static Function<String, Byte> parseByte() {
        return PARSE_BYTE_FUNC;
    }

    /** The parse short func. */
    private static Function<String, Short> PARSE_SHORT_FUNC = new Function<String, Short>() {
        @Override
        public Short apply(String t) {
            return N.parseShort(t);
        }
    };

    /**
     * Parses the short.
     *
     * @return the function
     */
    public static Function<String, Short> parseShort() {
        return PARSE_SHORT_FUNC;
    }

    /** The parse int func. */
    private static Function<String, Integer> PARSE_INT_FUNC = new Function<String, Integer>() {
        @Override
        public Integer apply(String t) {
            return N.parseInt(t);
        }
    };

    /**
     * Parses the int.
     *
     * @return the function
     */
    public static Function<String, Integer> parseInt() {
        return PARSE_INT_FUNC;
    }

    /** The parse long func. */
    private static Function<String, Long> PARSE_LONG_FUNC = new Function<String, Long>() {
        @Override
        public Long apply(String t) {
            return N.parseLong(t);
        }
    };

    /**
     * Parses the long.
     *
     * @return the function
     */
    public static Function<String, Long> parseLong() {
        return PARSE_LONG_FUNC;
    }

    /** The parse float func. */
    private static Function<String, Float> PARSE_FLOAT_FUNC = new Function<String, Float>() {
        @Override
        public Float apply(String t) {
            return N.parseFloat(t);
        }
    };

    /**
     * Parses the float.
     *
     * @return the function
     */
    public static Function<String, Float> parseFloat() {
        return PARSE_FLOAT_FUNC;
    }

    /** The parse double func. */
    private static Function<String, Double> PARSE_DOUBLE_FUNC = new Function<String, Double>() {
        @Override
        public Double apply(String t) {
            return N.parseDouble(t);
        }
    };

    /**
     * Parses the double.
     *
     * @return the function
     */
    public static Function<String, Double> parseDouble() {
        return PARSE_DOUBLE_FUNC;
    }

    /** The create number func. */
    private static Function<String, Number> CREATE_NUMBER_FUNC = new Function<String, Number>() {
        @Override
        public Number apply(final String t) {
            return N.isNullOrEmpty(t) ? null : StringUtil.createNumber(t).orElseThrow(new Supplier<NumberFormatException>() {
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
     * @return the function
     * @see StringUtil#createNumber(String)
     */
    public static Function<String, Number> createNumber() {
        return CREATE_NUMBER_FUNC;
    }

    /**
     * Num to int.
     *
     * @param <T> the generic type
     * @return the to int function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToIntFunction<T> numToInt() {
        return (ToIntFunction) ToIntFunction.FROM_NUM;
    }

    /**
     * Num to long.
     *
     * @param <T> the generic type
     * @return the to long function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToLongFunction<T> numToLong() {
        return (ToLongFunction) ToLongFunction.FROM_NUM;
    }

    /**
     * Num to double.
     *
     * @param <T> the generic type
     * @return the to double function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T extends Number> ToDoubleFunction<T> numToDouble() {
        return (ToDoubleFunction) ToDoubleFunction.FROM_NUM;
    }

    /**
     * Limit then filter.
     *
     * @param <T> the generic type
     * @param limit the limit
     * @param predicate the predicate
     * @return the predicate
     */
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
     * Limit then filter.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param limit the limit
     * @param predicate the predicate
     * @return the bi predicate
     */
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
     * Filter then limit.
     *
     * @param <T> the generic type
     * @param predicate the predicate
     * @param limit the limit
     * @return the predicate
     */
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
     * Filter then limit.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param predicate the predicate
     * @param limit the limit
     * @return the bi predicate
     */
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
     * Time limit.
     *
     * @param <T> the generic type
     * @param timeInMillis the time in millis
     * @return the predicate
     */
    public static <T> Predicate<T> timeLimit(final long timeInMillis) {
        N.checkArgNotNegative(timeInMillis, "timeInMillis");

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
     * Time limit.
     *
     * @param <T> the generic type
     * @param duration the duration
     * @return the predicate
     */
    public static <T> Predicate<T> timeLimit(final Duration duration) {
        N.checkArgNotNull(duration, "duration");

        return timeLimit(duration.toMillis());
    }

    /**
     * Returns a stateful <code>Function</code> which should not be used in parallel stream.
     *
     * @param <T> the generic type
     * @return the function
     */
    @SequentialOnly
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
     * Returns a stateful <code>Predicate</code> which should not be used in parallel stream.
     *
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the predicate
     */
    @SequentialOnly
    public static <T> Predicate<T> indexed(final IndexedPredicate<T> predicate) {
        return Predicates.indexed(predicate);
    }

    /**
     * Returns a stateful <code>BiPredicate</code> which should not be used in parallel stream.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the bi predicate
     * @deprecated replaced by {@code BiPredicates#indexed(IndexedBiPredicate)}.
     */
    @Deprecated
    static <U, T> BiPredicate<U, T> indexed(final IndexedBiPredicate<U, T> predicate) {
        return BiPredicates.indexed(predicate);
    }

    /**
     * Returns a stateful <code>Function</code> which should not be used in parallel stream.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param func the func
     * @return the function
     * @deprecated replaced by {@code Functions#indexed(IndexedFunction)}.
     */
    @Deprecated
    static <T, R> Function<T, R> indexedd(final IndexedFunction<T, R> func) {
        return Functions.indexed(func);
    }

    /**
     * Returns a stateful <code>BiFunction</code> which should not be used in parallel stream.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param func the func
     * @return the bi function
     * @deprecated replaced by {@code BiFunctions#indexed(IndexedBiFunction)}.
     */
    @Deprecated
    static <U, T, R> BiFunction<U, T, R> indexedd(final IndexedBiFunction<U, T, R> func) {
        return BiFunctions.indexed(func);
    }

    /**
     * Returns a stateful <code>Consumer</code> which should not be used in parallel stream.
     *
     * @param <T> the generic type
     * @param action the action
     * @return the consumer
     * @deprecated replaced by {@code Consumers#indexed(IndexedConsumer)}.
     */
    @Deprecated
    static <T> Consumer<T> indexeed(final IndexedConsumer<T> action) {
        return Consumers.indexed(action);
    }

    /**
     * Returns a stateful <code>BiConsumer</code> which should not be used in parallel stream.
     *
     * @param <U> the generic type
     * @param <T> the generic type
     * @param action the action
     * @return the bi consumer
     * @deprecated replaced by {@code BiConsumers#indexed(IndexedBiConsumer)}.
     */
    @Deprecated
    static <U, T> BiConsumer<U, T> indexeed(final IndexedBiConsumer<U, T> action) {
        return BiConsumers.indexed(action);
    }

    /**
     * Compare to.
     *
     * @param <T> the generic type
     * @param target the target
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> Function<T, Integer> compareTo(final T target) {
        return new Function<T, Integer>() {
            @Override
            public Integer apply(T t) {
                return N.compare(t, target);
            }
        };
    }

    /**
     * Compare to.
     *
     * @param <T> the generic type
     * @param target the target
     * @param cmp the cmp
     * @return the function
     */
    @SuppressWarnings("rawtypes")
    public static <T> Function<T, Integer> compareTo(final T target, final Comparator<? super T> cmp) {
        // N.checkArgNotNull(cmp);

        if (cmp == null || cmp == Comparators.naturalOrder()) {
            return (Function) compareTo((Comparable) target);
        }

        return new Function<T, Integer>() {
            @Override
            public Integer apply(T t) {
                return N.compare(t, target, cmp);
            }
        };
    }

    /**
     * Compare.
     *
     * @param <T> the generic type
     * @return the bi function
     */
    @SuppressWarnings("rawtypes")
    public static <T extends Comparable> BiFunction<T, T, Integer> compare() {
        return COMPARE;
    }

    /**
     * Compare.
     *
     * @param <T> the generic type
     * @param cmp the cmp
     * @return the bi function
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
     * P.
     *
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the predicate
     */
    @Beta
    public static <T> Predicate<T> p(final Predicate<T> predicate) {
        return predicate;
    }

    /**
     * P.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param a the a
     * @param biPredicate the bi predicate
     * @return the predicate
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
     * P.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param a the a
     * @param b the b
     * @param triPredicate the tri predicate
     * @return the predicate
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
     * P.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param biPredicate the bi predicate
     * @return the bi predicate
     */
    @Beta
    public static <T, U> BiPredicate<T, U> p(final BiPredicate<T, U> biPredicate) {
        return biPredicate;
    }

    /**
     * P.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param a the a
     * @param triPredicate the tri predicate
     * @return the bi predicate
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
     * P.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param triPredicate the tri predicate
     * @return the tri predicate
     */
    @Beta
    public static <A, B, C> TriPredicate<A, B, C> p(final TriPredicate<A, B, C> triPredicate) {
        return triPredicate;
    }

    /**
     * C.
     *
     * @param <T> the generic type
     * @param predicate the predicate
     * @return the consumer
     */
    @Beta
    public static <T> Consumer<T> c(final Consumer<T> predicate) {
        return predicate;
    }

    /**
     * C.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param a the a
     * @param biConsumer the bi consumer
     * @return the consumer
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
     * C.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param a the a
     * @param b the b
     * @param triConsumer the tri consumer
     * @return the consumer
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
     * C.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param biConsumer the bi consumer
     * @return the bi consumer
     */
    @Beta
    public static <T, U> BiConsumer<T, U> c(final BiConsumer<T, U> biConsumer) {
        return biConsumer;
    }

    /**
     * C.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param a the a
     * @param triConsumer the tri consumer
     * @return the bi consumer
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
     * C.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param triConsumer the tri consumer
     * @return the tri consumer
     */
    @Beta
    public static <A, B, C> TriConsumer<A, B, C> c(final TriConsumer<A, B, C> triConsumer) {
        return triConsumer;
    }

    /**
     * F.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param predicate the predicate
     * @return the function
     */
    @Beta
    public static <T, R> Function<T, R> f(final Function<T, R> predicate) {
        return predicate;
    }

    /**
     * F.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param a the a
     * @param biFunction the bi function
     * @return the function
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
     * F.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param a the a
     * @param b the b
     * @param triFunction the tri function
     * @return the function
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
     * F.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param biFunction the bi function
     * @return the bi function
     */
    @Beta
    public static <T, U, R> BiFunction<T, U, R> f(final BiFunction<T, U, R> biFunction) {
        return biFunction;
    }

    /**
     * F.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param a the a
     * @param triFunction the tri function
     * @return the bi function
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
     * F.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param triFunction the tri function
     * @return the tri function
     */
    @Beta
    public static <A, B, C, R> TriFunction<A, B, C, R> f(final TriFunction<A, B, C, R> triFunction) {
        return triFunction;
    }

    /**
     * Ep.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param predicate the predicate
     * @return the try. predicate
     */
    @Beta
    public static <T, E extends Exception> Try.Predicate<T, E> ep(final Try.Predicate<T, E> predicate) {
        return predicate;
    }

    /**
     * Ep.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biPredicate the bi predicate
     * @return the try. predicate
     */
    @Beta
    public static <A, T, E extends Exception> Try.Predicate<T, E> ep(final A a, final Try.BiPredicate<A, T, E> biPredicate) {
        N.checkArgNotNull(biPredicate);

        return new Try.Predicate<T, E>() {
            @Override
            public boolean test(T t) throws E {
                return biPredicate.test(a, t);
            }
        };
    }

    /**
     * Ep.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triPredicate the tri predicate
     * @return the try. predicate
     */
    @Beta
    public static <A, B, T, E extends Exception> Try.Predicate<T, E> ep(final A a, final B b, final Try.TriPredicate<A, B, T, E> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new Try.Predicate<T, E>() {
            @Override
            public boolean test(T t) throws E {
                return triPredicate.test(a, b, t);
            }
        };
    }

    /**
     * Ep.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param biPredicate the bi predicate
     * @return the try. bi predicate
     */
    @Beta
    public static <T, U, E extends Exception> Try.BiPredicate<T, U, E> ep(final Try.BiPredicate<T, U, E> biPredicate) {
        return biPredicate;
    }

    /**
     * Ep.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triPredicate the tri predicate
     * @return the try. bi predicate
     */
    @Beta
    public static <A, T, U, E extends Exception> Try.BiPredicate<T, U, E> ep(final A a, final Try.TriPredicate<A, T, U, E> triPredicate) {
        N.checkArgNotNull(triPredicate);

        return new Try.BiPredicate<T, U, E>() {
            @Override
            public boolean test(T t, U u) throws E {
                return triPredicate.test(a, t, u);
            }
        };
    }

    /**
     * Ep.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     * @param triPredicate the tri predicate
     * @return the try. tri predicate
     */
    @Beta
    public static <A, B, C, E extends Exception> Try.TriPredicate<A, B, C, E> ep(final Try.TriPredicate<A, B, C, E> triPredicate) {
        return triPredicate;
    }

    /**
     * Ec.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param predicate the predicate
     * @return the try. consumer
     */
    @Beta
    public static <T, E extends Exception> Try.Consumer<T, E> ec(final Try.Consumer<T, E> predicate) {
        return predicate;
    }

    /**
     * Ec.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biConsumer the bi consumer
     * @return the try. consumer
     */
    @Beta
    public static <A, T, E extends Exception> Try.Consumer<T, E> ec(final A a, final Try.BiConsumer<A, T, E> biConsumer) {
        N.checkArgNotNull(biConsumer);

        return new Try.Consumer<T, E>() {
            @Override
            public void accept(T t) throws E {
                biConsumer.accept(a, t);
            }
        };
    }

    /**
     * Ec.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triConsumer the tri consumer
     * @return the try. consumer
     */
    @Beta
    public static <A, B, T, E extends Exception> Try.Consumer<T, E> ec(final A a, final B b, final Try.TriConsumer<A, B, T, E> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new Try.Consumer<T, E>() {
            @Override
            public void accept(T t) throws E {
                triConsumer.accept(a, b, t);
            }
        };
    }

    /**
     * Ec.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param biConsumer the bi consumer
     * @return the try. bi consumer
     */
    @Beta
    public static <T, U, E extends Exception> Try.BiConsumer<T, U, E> ec(final Try.BiConsumer<T, U, E> biConsumer) {
        return biConsumer;
    }

    /**
     * Ec.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triConsumer the tri consumer
     * @return the try. bi consumer
     */
    @Beta
    public static <A, T, U, E extends Exception> Try.BiConsumer<T, U, E> ec(final A a, final Try.TriConsumer<A, T, U, E> triConsumer) {
        N.checkArgNotNull(triConsumer);

        return new Try.BiConsumer<T, U, E>() {
            @Override
            public void accept(T t, U u) throws E {
                triConsumer.accept(a, t, u);
            }
        };
    }

    /**
     * Ec.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     * @param triConsumer the tri consumer
     * @return the try. tri consumer
     */
    @Beta
    public static <A, B, C, E extends Exception> Try.TriConsumer<A, B, C, E> ec(final Try.TriConsumer<A, B, C, E> triConsumer) {
        return triConsumer;
    }

    /**
     * Ef.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param predicate the predicate
     * @return the try. function
     */
    @Beta
    public static <T, R, E extends Exception> Try.Function<T, R, E> ef(final Try.Function<T, R, E> predicate) {
        return predicate;
    }

    /**
     * Ef.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biFunction the bi function
     * @return the try. function
     */
    @Beta
    public static <A, T, R, E extends Exception> Try.Function<T, R, E> ef(final A a, final Try.BiFunction<A, T, R, E> biFunction) {
        N.checkArgNotNull(biFunction);

        return new Try.Function<T, R, E>() {
            @Override
            public R apply(T t) throws E {
                return biFunction.apply(a, t);
            }
        };
    }

    /**
     * Ef.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triFunction the tri function
     * @return the try. function
     */
    @Beta
    public static <A, B, T, R, E extends Exception> Try.Function<T, R, E> ef(final A a, final B b, final Try.TriFunction<A, B, T, R, E> triFunction) {
        N.checkArgNotNull(triFunction);

        return new Try.Function<T, R, E>() {
            @Override
            public R apply(T t) throws E {
                return triFunction.apply(a, b, t);
            }
        };
    }

    /**
     * Ef.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param biFunction the bi function
     * @return the try. bi function
     */
    @Beta
    public static <T, U, R, E extends Exception> Try.BiFunction<T, U, R, E> ef(final Try.BiFunction<T, U, R, E> biFunction) {
        return biFunction;
    }

    /**
     * Ef.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triFunction the tri function
     * @return the try. bi function
     */
    @Beta
    public static <A, T, U, R, E extends Exception> Try.BiFunction<T, U, R, E> ef(final A a, final Try.TriFunction<A, T, U, R, E> triFunction) {
        N.checkArgNotNull(triFunction);

        return new Try.BiFunction<T, U, R, E>() {
            @Override
            public R apply(T t, U u) throws E {
                return triFunction.apply(a, t, u);
            }
        };
    }

    /**
     * Ef.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param triFunction the tri function
     * @return the try. tri function
     */
    @Beta
    public static <A, B, C, R, E extends Exception> Try.TriFunction<A, B, C, R, E> ef(final Try.TriFunction<A, B, C, R, E> triFunction) {
        return triFunction;
    }

    /**
     * Pp.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param predicate the predicate
     * @return the predicate
     */
    @Beta
    public static <T, E extends Exception> Predicate<T> pp(final Try.Predicate<T, E> predicate) {
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
     * Pp.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biPredicate the bi predicate
     * @return the predicate
     */
    @Beta
    public static <A, T, E extends Exception> Predicate<T> pp(final A a, final Try.BiPredicate<A, T, E> biPredicate) {
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
     * Pp.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triPredicate the tri predicate
     * @return the predicate
     */
    @Beta
    public static <A, B, T, E extends Exception> Predicate<T> pp(final A a, final B b, final Try.TriPredicate<A, B, T, E> triPredicate) {
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
     * Pp.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param biPredicate the bi predicate
     * @return the bi predicate
     */
    @Beta
    public static <T, U, E extends Exception> BiPredicate<T, U> pp(final Try.BiPredicate<T, U, E> biPredicate) {
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
     * Pp.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triPredicate the tri predicate
     * @return the bi predicate
     */
    @Beta
    public static <A, T, U, E extends Exception> BiPredicate<T, U> pp(final A a, final Try.TriPredicate<A, T, U, E> triPredicate) {
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
     * Pp.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     * @param triPredicate the tri predicate
     * @return the tri predicate
     */
    @Beta
    public static <A, B, C, E extends Exception> TriPredicate<A, B, C> pp(final Try.TriPredicate<A, B, C, E> triPredicate) {
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
     * Cc.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param consumer the consumer
     * @return the consumer
     */
    @Beta
    public static <T, E extends Exception> Consumer<T> cc(final Try.Consumer<T, E> consumer) {
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
     * Cc.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biConsumer the bi consumer
     * @return the consumer
     */
    @Beta
    public static <A, T, E extends Exception> Consumer<T> cc(final A a, final Try.BiConsumer<A, T, E> biConsumer) {
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
     * Cc.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triConsumer the tri consumer
     * @return the consumer
     */
    @Beta
    public static <A, B, T, E extends Exception> Consumer<T> cc(final A a, final B b, final Try.TriConsumer<A, B, T, E> triConsumer) {
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
     * Cc.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param biConsumer the bi consumer
     * @return the bi consumer
     */
    @Beta
    public static <T, U, E extends Exception> BiConsumer<T, U> cc(final Try.BiConsumer<T, U, E> biConsumer) {
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
     * Cc.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triConsumer the tri consumer
     * @return the bi consumer
     */
    @Beta
    public static <A, T, U, E extends Exception> BiConsumer<T, U> cc(final A a, final Try.TriConsumer<A, T, U, E> triConsumer) {
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
     * Cc.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <E> the element type
     * @param triConsumer the tri consumer
     * @return the tri consumer
     */
    @Beta
    public static <A, B, C, E extends Exception> TriConsumer<A, B, C> cc(final Try.TriConsumer<A, B, C, E> triConsumer) {
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
     * Ff.
     *
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param function the function
     * @return the function
     */
    @Beta
    public static <T, R, E extends Exception> Function<T, R> ff(final Try.Function<T, R, E> function) {
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
     * Ff.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param biFunction the bi function
     * @return the function
     */
    @Beta
    public static <A, T, R, E extends Exception> Function<T, R> ff(final A a, final Try.BiFunction<A, T, R, E> biFunction) {
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
     * Ff.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param b the b
     * @param triFunction the tri function
     * @return the function
     */
    @Beta
    public static <A, B, T, R, E extends Exception> Function<T, R> ff(final A a, final B b, final Try.TriFunction<A, B, T, R, E> triFunction) {
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
     * Ff.
     *
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param biFunction the bi function
     * @return the bi function
     */
    @Beta
    public static <T, U, R, E extends Exception> BiFunction<T, U, R> ff(final Try.BiFunction<T, U, R, E> biFunction) {
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
     * Ff.
     *
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param a the a
     * @param triFunction the tri function
     * @return the bi function
     */
    @Beta
    public static <A, T, U, R, E extends Exception> BiFunction<T, U, R> ff(final A a, final Try.TriFunction<A, T, U, R, E> triFunction) {
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
     * Ff.
     *
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <C> the generic type
     * @param <R> the generic type
     * @param <E> the element type
     * @param triFunction the tri function
     * @return the tri function
     */
    @Beta
    public static <A, B, C, R, E extends Exception> TriFunction<A, B, C, R> ff(final Try.TriFunction<A, B, C, R, E> triFunction) {
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
     * Synchronized {@code Predicate}.
     *
     * @param <T> the generic type
     * @param mutex to synchronized on
     * @param predicate the predicate
     * @return the predicate
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
     * @param <A> the generic type
     * @param <T> the generic type
     * @param mutex to synchronized on
     * @param a the a
     * @param biPredicate the bi predicate
     * @return the predicate
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
     * @param <A> the generic type
     * @param <B> the generic type
     * @param <T> the generic type
     * @param mutex to synchronized on
     * @param a the a
     * @param b the b
     * @param triPredicate the tri predicate
     * @return the predicate
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
     * @param <T> the generic type
     * @param <U> the generic type
     * @param mutex to synchronized on
     * @param biPredicate the bi predicate
     * @return the bi predicate
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
     * @param <T> the generic type
     * @param mutex to synchronized on
     * @param consumer the consumer
     * @return the consumer
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
     * @param <A> the generic type
     * @param <T> the generic type
     * @param mutex to synchronized on
     * @param a the a
     * @param biConsumer the bi consumer
     * @return the consumer
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
     * @param <T> the generic type
     * @param <U> the generic type
     * @param mutex to synchronized on
     * @param biConsumer the bi consumer
     * @return the bi consumer
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
     * @param <T> the generic type
     * @param <R> the generic type
     * @param mutex to synchronized on
     * @param function the function
     * @return the function
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
     * @param <A> the generic type
     * @param <T> the generic type
     * @param <R> the generic type
     * @param mutex to synchronized on
     * @param a the a
     * @param biFunction the bi function
     * @return the function
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
     * @param <T> the generic type
     * @param <U> the generic type
     * @param <R> the generic type
     * @param mutex to synchronized on
     * @param biFunction the bi function
     * @return the bi function
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
     * @param <T> the generic type
     * @param duration the duration
     * @param startTime the start time
     * @return the function
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Stream<Timed<T>>>> window(final Duration duration, final LongSupplier startTime) {
        return window(duration, duration.toMillis(), startTime);
    }

    /**
     * Window to list.
     *
     * @param <T> the generic type
     * @param duration the duration
     * @param startTime the start time
     * @return the function
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> windowToList(final Duration duration, final LongSupplier startTime) {
        return window(duration, startTime, Suppliers.<Timed<T>> ofList());
    }

    /**
     * Window to set.
     *
     * @param <T> the generic type
     * @param duration the duration
     * @param startTime the start time
     * @return the function
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Set<Timed<T>>>> windowToSet(final Duration duration, final LongSupplier startTime) {
        return window(duration, startTime, Suppliers.<Timed<T>> ofSet());
    }

    /**
     * Window.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param duration the duration
     * @param startTime the start time
     * @param collectionSupplier the collection supplier
     * @return the function
     */
    public static <T, C extends Collection<Timed<T>>> Function<Stream<Timed<T>>, Stream<C>> window(final Duration duration, final LongSupplier startTime,
            final Supplier<? extends C> collectionSupplier) {
        return window(duration, duration.toMillis(), startTime, collectionSupplier);
    }

    /**
     * Window.
     *
     * @param <T> the generic type
     * @param <A> the generic type
     * @param <R> the generic type
     * @param duration the duration
     * @param startTime the start time
     * @param collector the collector
     * @return the function
     * @see #window(Duration, LongSupplier)
     */
    public static <T, A, R> Function<Stream<Timed<T>>, Stream<R>> window(final Duration duration, final LongSupplier startTime,
            final Collector<? super Timed<T>, A, R> collector) {
        return window(duration, duration.toMillis(), startTime, collector);
    }

    /**
     * Window.
     *
     * @param <T> the generic type
     * @param duration the duration
     * @param incrementInMillis the increment in millis
     * @param startTime the start time
     * @return the function
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
     * @param <T> the generic type
     * @param duration the duration
     * @param incrementInMillis the increment in millis
     * @param startTime the start time
     * @return the function
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<List<Timed<T>>>> windowToList(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime) {
        return window(duration, incrementInMillis, startTime, Suppliers.<Timed<T>> ofList());
    }

    /**
     * Window to set.
     *
     * @param <T> the generic type
     * @param duration the duration
     * @param incrementInMillis the increment in millis
     * @param startTime the start time
     * @return the function
     * @see #window(Duration, LongSupplier)
     */
    public static <T> Function<Stream<Timed<T>>, Stream<Set<Timed<T>>>> windowToSet(final Duration duration, final long incrementInMillis,
            final LongSupplier startTime) {
        return window(duration, incrementInMillis, startTime, Suppliers.<Timed<T>> ofSet());
    }

    /**
     * Window.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param duration the duration
     * @param incrementInMillis the increment in millis
     * @param startTime the start time
     * @param collectionSupplier the collection supplier
     * @return the function
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

                return Stream.of(iter).onClose(new Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     * Window.
     *
     * @param <T> the generic type
     * @param <A> the generic type
     * @param <R> the generic type
     * @param duration the duration
     * @param incrementInMillis the increment in millis
     * @param startTime the start time
     * @param collector the collector
     * @return the function
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

                return Stream.of(iter).onClose(new Runnable() {
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
     * @param <T> the generic type
     * @param maxWindowSize the max window size
     * @param maxDuration the max duration
     * @param startTime the start time
     * @return the function
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
     * @param <T> the generic type
     * @param <C> the generic type
     * @param maxWindowSize the max window size
     * @param maxDuration the max duration
     * @param startTime the start time
     * @param collectionSupplier the collection supplier
     * @return the function
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

                return Stream.of(iter).onClose(new Runnable() {
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
     * @param <T> the generic type
     * @param <A> the generic type
     * @param <R> the generic type
     * @param maxWindowSize the max window size
     * @param maxDuration the max duration
     * @param startTime the start time
     * @param collector the collector
     * @return the function
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

                return Stream.of(iter).onClose(new Runnable() {
                    @Override
                    public void run() {
                        s.close();
                    }
                });
            }
        };
    }

    /**
     * Callable.
     *
     * @param <R> the generic type
     * @param callable the callable
     * @return the com.landawn.abacus.util.function. callable
     */
    public static <R> com.landawn.abacus.util.function.Callable<R> callable(final com.landawn.abacus.util.function.Callable<R> callable) {
        N.checkArgNotNull(callable);

        return callable;
    }

    /**
     * Runnable.
     *
     * @param runnable the runnable
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static com.landawn.abacus.util.function.Runnable runnable(final com.landawn.abacus.util.function.Runnable runnable) {
        N.checkArgNotNull(runnable);

        return runnable;
    }

    /**
     * To callable.
     *
     * @param runnable the runnable
     * @return the com.landawn.abacus.util.function. callable
     */
    public static com.landawn.abacus.util.function.Callable<Void> toCallable(final com.landawn.abacus.util.function.Runnable runnable) {
        N.checkArgNotNull(runnable);

        return new com.landawn.abacus.util.function.Callable<Void>() {
            @Override
            public Void call() {
                runnable.run();
                return null;
            }
        };
    }

    /**
     * To runnable.
     *
     * @param <R> the generic type
     * @param callable the callable
     * @return the com.landawn.abacus.util.function. runnable
     */
    public static <R> com.landawn.abacus.util.function.Runnable toRunnable(final com.landawn.abacus.util.function.Callable<R> callable) {
        N.checkArgNotNull(callable);

        return new com.landawn.abacus.util.function.Runnable() {
            @Override
            public void run() {
                callable.call();
            }
        };
    }

    /**
     * Throwing merger.
     *
     * @param <T> the generic type
     * @return the binary operator
     */
    public static <T> BinaryOperator<T> throwingMerger() {
        return BinaryOperators.THROWING_MERGER;
    }

    /**
     * Ignoring merger.
     *
     * @param <T> the generic type
     * @return the binary operator
     */
    public static <T> BinaryOperator<T> ignoringMerger() {
        return BinaryOperators.IGNORING_MERGER;
    }

    /**
     * Replacing merger.
     *
     * @param <T> the generic type
     * @return the binary operator
     */
    public static <T> BinaryOperator<T> replacingMerger() {
        return BinaryOperators.REPLACING_MERGER;
    }

    /**
     * Adds the all.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @return the bi consumer
     * @deprecated replaced by {@code BiConsumers#ofAddAll()}
     */
    @Deprecated
    static <T, C extends Collection<T>> BiConsumer<C, C> addAll() {
        return BiConsumers.<T, C> ofAddAll();
    }

    /**
     * Put all.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the generic type
     * @return the bi consumer
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
                return new HashSet<>(N.initHashCapacity(len));
            }
        };

        /** The Constant LINKED_HASH_SET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedHashSet> LINKED_HASH_SET_FACTORY = new IntFunction<LinkedHashSet>() {
            @Override
            public LinkedHashSet apply(int len) {
                return new LinkedHashSet<>(N.initHashCapacity(len));
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
                return new HashMap<>(N.initHashCapacity(len));
            }
        };

        /** The Constant LINKED_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LinkedHashMap> LINKED_HASH_MAP_FACTORY = new IntFunction<LinkedHashMap>() {
            @Override
            public LinkedHashMap apply(int len) {
                return new LinkedHashMap<>(N.initHashCapacity(len));
            }
        };

        /** The Constant IDENTITY_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super IdentityHashMap> IDENTITY_HASH_MAP_FACTORY = new IntFunction<IdentityHashMap>() {
            @Override
            public IdentityHashMap apply(int len) {
                return new IdentityHashMap<>(N.initHashCapacity(len));
            }
        };

        /** The Constant TREE_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super TreeMap> TREE_MAP_FACTORY = new IntFunction<TreeMap>() {
            @Override
            public TreeMap apply(int len) {
                return new TreeMap<>();
            }
        };

        /** The Constant CONCURRENT_HASH_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ConcurrentHashMap> CONCURRENT_HASH_MAP_FACTORY = new IntFunction<ConcurrentHashMap>() {
            @Override
            public ConcurrentHashMap apply(int len) {
                return new ConcurrentHashMap(N.initHashCapacity(len));
            }
        };

        /** The Constant BI_MAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super BiMap> BI_MAP_FACTORY = new IntFunction<BiMap>() {
            @Override
            public BiMap apply(int len) {
                return new BiMap(N.initHashCapacity(len));
            }
        };

        /** The Constant MULTISET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super Multiset> MULTISET_FACTORY = new IntFunction<Multiset>() {
            @Override
            public Multiset apply(int len) {
                return new Multiset(N.initHashCapacity(len));
            }
        };

        /** The Constant LONG_MULTISET_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super LongMultiset> LONG_MULTISET_FACTORY = new IntFunction<LongMultiset>() {
            @Override
            public LongMultiset apply(int len) {
                return new LongMultiset(N.initHashCapacity(len));
            }
        };

        /** The Constant LIST_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super ListMultimap> LIST_MULTIMAP_FACTORY = new IntFunction<ListMultimap>() {
            @Override
            public ListMultimap apply(int len) {
                return new ListMultimap(N.initHashCapacity(len));
            }
        };

        /** The Constant SET_MULTIMAP_FACTORY. */
        @SuppressWarnings("rawtypes")
        private static final IntFunction<? super SetMultimap> SET_MULTIMAP_FACTORY = new IntFunction<SetMultimap>() {
            @Override
            public SetMultimap apply(int len) {
                return new SetMultimap(N.initHashCapacity(len));
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
         * @return the int function
         */
        public static IntFunction<boolean[]> ofBooleanArray() {
            return BOOLEAN_ARRAY;
        }

        /**
         * Of char array.
         *
         * @return the int function
         */
        public static IntFunction<char[]> ofCharArray() {
            return CHAR_ARRAY;
        }

        /**
         * Of byte array.
         *
         * @return the int function
         */
        public static IntFunction<byte[]> ofByteArray() {
            return BYTE_ARRAY;
        }

        /**
         * Of short array.
         *
         * @return the int function
         */
        public static IntFunction<short[]> ofShortArray() {
            return SHORT_ARRAY;
        }

        /**
         * Of int array.
         *
         * @return the int function
         */
        public static IntFunction<int[]> ofIntArray() {
            return INT_ARRAY;
        }

        /**
         * Of long array.
         *
         * @return the int function
         */
        public static IntFunction<long[]> ofLongArray() {
            return LONG_ARRAY;
        }

        /**
         * Of float array.
         *
         * @return the int function
         */
        public static IntFunction<float[]> ofFloatArray() {
            return FLOAT_ARRAY;
        }

        /**
         * Of double array.
         *
         * @return the int function
         */
        public static IntFunction<double[]> ofDoubleArray() {
            return DOUBLE_ARRAY;
        }

        /**
         * Of string array.
         *
         * @return the int function
         */
        public static IntFunction<String[]> ofStringArray() {
            return STRING_ARRAY;
        }

        /**
         * Of object array.
         *
         * @return the int function
         */
        public static IntFunction<Object[]> ofObjectArray() {
            return OBJECT_ARRAY;
        }

        /**
         * Of boolean list.
         *
         * @return the int function
         */
        public static IntFunction<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return the int function
         */
        public static IntFunction<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return the int function
         */
        public static IntFunction<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return the int function
         */
        public static IntFunction<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return the int function
         */
        public static IntFunction<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return the int function
         */
        public static IntFunction<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return the int function
         */
        public static IntFunction<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return the int function
         */
        public static IntFunction<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         * Of list.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<List<T>> ofList() {
            return (IntFunction) LIST_FACTORY;
        }

        /**
         * Of linked list.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedList<T>> ofLinkedList() {
            return (IntFunction) LINKED_LIST_FACTORY;
        }

        /**
         * Of set.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Set<T>> ofSet() {
            return (IntFunction) SET_FACTORY;
        }

        /**
         * Of linked hash set.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedHashSet<T>> ofLinkedHashSet() {
            return (IntFunction) LINKED_HASH_SET_FACTORY;
        }

        /**
         * Of sorted set.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<SortedSet<T>> ofSortedSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of navigable set.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<NavigableSet<T>> ofNavigableSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of tree set.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<TreeSet<T>> ofTreeSet() {
            return (IntFunction) TREE_SET_FACTORY;
        }

        /**
         * Of queue.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Queue<T>> ofQueue() {
            return (IntFunction) QUEUE_FACTORY;
        }

        /**
         * Of deque.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Deque<T>> ofDeque() {
            return (IntFunction) DEQUE_FACTORY;
        }

        /**
         * Of array deque.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ArrayDeque<T>> ofArrayDeque() {
            return (IntFunction) ARRAY_DEQUE_FACTORY;
        }

        /**
         * Of linked blocking queue.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (IntFunction) LINKED_BLOCKING_QUEUE_FACTORY;
        }

        /**
         * Of concurrent linked queue.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (IntFunction) CONCURRENT_LINKED_QUEUE_FACTORY;
        }

        /**
         * Of priority queue.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<PriorityQueue<T>> ofPriorityQueue() {
            return (IntFunction) PRIORITY_QUEUE_FACTORY;
        }

        /**
         * Of map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the int function
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
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<LinkedHashMap<K, V>> ofLinkedHashMap() {
            return (IntFunction) LINKED_HASH_MAP_FACTORY;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the int function
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
         * @return the int function
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
         * @return the int function
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
         * @return the int function
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
         * @return the int function
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
         * @return the int function
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
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> IntFunction<BiMap<K, V>> ofBiMap() {
            return (IntFunction) BI_MAP_FACTORY;
        }

        /**
         * Of multiset.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<Multiset<T>> ofMultiset() {
            return (IntFunction) MULTISET_FACTORY;
        }

        /**
         * Of long multiset.
         *
         * @param <T> the generic type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <T> IntFunction<LongMultiset<T>> ofLongMultiset() {
            return (IntFunction) LONG_MULTISET_FACTORY;
        }

        /**
         * Of list multimap.
         *
         * @param <K> the key type
         * @param <E> the element type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<ListMultimap<K, E>> ofListMultimap() {
            return (IntFunction) LIST_MULTIMAP_FACTORY;
        }

        /**
         * Of set multimap.
         *
         * @param <K> the key type
         * @param <E> the element type
         * @return the int function
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> IntFunction<SetMultimap<K, E>> ofSetMultimap() {
            return (IntFunction) SET_MULTIMAP_FACTORY;
        }

        /**
         * Won't work.
         *
         * @return the int function
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         * Won't work.
         *
         * @return the int function
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         * Won't work.
         *
         * @return the int function
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static IntFunction<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        /**
         * Single.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @param supplier the supplier
         * @return the int function<? extends c>
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
    public static abstract class IntFunctions extends Factory {

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
    public static abstract class Suppliers {

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
                return new HashSet();
            }
        };

        /** The Constant LINKED_HASH_SET. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedHashSet> LINKED_HASH_SET = new Supplier<LinkedHashSet>() {
            @Override
            public LinkedHashSet get() {
                return new LinkedHashSet();
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
                return new HashMap();
            }
        };

        /** The Constant LINKED_HASH_MAP. */
        @SuppressWarnings("rawtypes")
        private static final Supplier<? super LinkedHashMap> LINKED_HASH_MAP = new Supplier<LinkedHashMap>() {
            @Override
            public LinkedHashMap get() {
                return new LinkedHashMap();
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
         * @param <T> the generic type
         * @param instance the instance
         * @return the supplier
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
         * Of UUID.
         *
         * @return the supplier
         */
        public static Supplier<String> ofUUID() {
            return UUID;
        }

        /**
         * Of GUID.
         *
         * @return the supplier
         */
        public static Supplier<String> ofGUID() {
            return GUID;
        }

        /**
         * Of empty boolean array.
         *
         * @return the supplier
         */
        public static Supplier<boolean[]> ofEmptyBooleanArray() {
            return EMPTY_BOOLEAN_ARRAY;
        }

        /**
         * Of empty char array.
         *
         * @return the supplier
         */
        public static Supplier<char[]> ofEmptyCharArray() {
            return EMPTY_CHAR_ARRAY;
        }

        /**
         * Of empty byte array.
         *
         * @return the supplier
         */
        public static Supplier<byte[]> ofEmptyByteArray() {
            return EMPTY_BYTE_ARRAY;
        }

        /**
         * Of empty short array.
         *
         * @return the supplier
         */
        public static Supplier<short[]> ofEmptyShortArray() {
            return EMPTY_SHORT_ARRAY;
        }

        /**
         * Of empty int array.
         *
         * @return the supplier
         */
        public static Supplier<int[]> ofEmptyIntArray() {
            return EMPTY_INT_ARRAY;
        }

        /**
         * Of empty long array.
         *
         * @return the supplier
         */
        public static Supplier<long[]> ofEmptyLongArray() {
            return EMPTY_LONG_ARRAY;
        }

        /**
         * Of empty float array.
         *
         * @return the supplier
         */
        public static Supplier<float[]> ofEmptyFloatArray() {
            return EMPTY_FLOAT_ARRAY;
        }

        /**
         * Of empty double array.
         *
         * @return the supplier
         */
        public static Supplier<double[]> ofEmptyDoubleArray() {
            return EMPTY_DOUBLE_ARRAY;
        }

        /**
         * Of empty string array.
         *
         * @return the supplier
         */
        public static Supplier<String[]> ofEmptyStringArray() {
            return EMPTY_STRING_ARRAY;
        }

        /**
         * Of empty object array.
         *
         * @return the supplier
         */
        public static Supplier<Object[]> ofEmptyObjectArray() {
            return EMPTY_OBJECT_ARRAY;
        }

        /**
         * Of boolean list.
         *
         * @return the supplier
         */
        public static Supplier<BooleanList> ofBooleanList() {
            return BOOLEAN_LIST;
        }

        /**
         * Of char list.
         *
         * @return the supplier
         */
        public static Supplier<CharList> ofCharList() {
            return CHAR_LIST;
        }

        /**
         * Of byte list.
         *
         * @return the supplier
         */
        public static Supplier<ByteList> ofByteList() {
            return BYTE_LIST;
        }

        /**
         * Of short list.
         *
         * @return the supplier
         */
        public static Supplier<ShortList> ofShortList() {
            return SHORT_LIST;
        }

        /**
         * Of int list.
         *
         * @return the supplier
         */
        public static Supplier<IntList> ofIntList() {
            return INT_LIST;
        }

        /**
         * Of long list.
         *
         * @return the supplier
         */
        public static Supplier<LongList> ofLongList() {
            return LONG_LIST;
        }

        /**
         * Of float list.
         *
         * @return the supplier
         */
        public static Supplier<FloatList> ofFloatList() {
            return FLOAT_LIST;
        }

        /**
         * Of double list.
         *
         * @return the supplier
         */
        public static Supplier<DoubleList> ofDoubleList() {
            return DOUBLE_LIST;
        }

        /**
         * Of list.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<List<T>> ofList() {
            return (Supplier) LIST;
        }

        /**
         * Of linked list.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedList<T>> ofLinkedList() {
            return (Supplier) LINKED_LIST;
        }

        /**
         * Of set.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Set<T>> ofSet() {
            return (Supplier) SET;
        }

        /**
         * Of linked hash set.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedHashSet<T>> ofLinkedHashSet() {
            return (Supplier) LINKED_HASH_SET;
        }

        /**
         * Of sorted set.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<SortedSet<T>> ofSortedSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of navigable set.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<NavigableSet<T>> ofNavigableSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of tree set.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<TreeSet<T>> ofTreeSet() {
            return (Supplier) TREE_SET;
        }

        /**
         * Of queue.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Queue<T>> ofQueue() {
            return (Supplier) QUEUE;
        }

        /**
         * Of deque.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Deque<T>> ofDeque() {
            return (Supplier) DEQUE;
        }

        /**
         * Of array deque.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ArrayDeque<T>> ofArrayDeque() {
            return (Supplier) ARRAY_DEQUE;
        }

        /**
         * Of linked blocking queue.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LinkedBlockingQueue<T>> ofLinkedBlockingQueue() {
            return (Supplier) LINKED_BLOCKING_QUEUE;
        }

        /**
         * Of concurrent linked queue.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<ConcurrentLinkedQueue<T>> ofConcurrentLinkedQueue() {
            return (Supplier) CONCURRENT_LINKED_QUEUE;
        }

        /**
         * Of priority queue.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<PriorityQueue<T>> ofPriorityQueue() {
            return (Supplier) PRIORITY_QUEUE;
        }

        /**
         * Of map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the supplier
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
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<LinkedHashMap<K, V>> ofLinkedHashMap() {
            return (Supplier) LINKED_HASH_MAP;
        }

        /**
         * Of identity hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the supplier
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
         * @return the supplier
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
         * @return the supplier
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
         * @return the supplier
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
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentMap<K, V>> ofConcurrentMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         * Of concurrent hash map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<ConcurrentHashMap<K, V>> ofConcurrentHashMap() {
            return (Supplier) CONCURRENT_HASH_MAP;
        }

        /**
         * Of bi map.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, V> Supplier<BiMap<K, V>> ofBiMap() {
            return (Supplier) BI_MAP;
        }

        /**
         * Of multiset.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<Multiset<T>> ofMultiset() {
            return (Supplier) MULTISET;
        }

        /**
         * Of long multiset.
         *
         * @param <T> the generic type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <T> Supplier<LongMultiset<T>> ofLongMultiset() {
            return (Supplier) LONG_MULTISET;
        }

        /**
         * Of list multimap.
         *
         * @param <K> the key type
         * @param <E> the element type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<ListMultimap<K, E>> ofListMultimap() {
            return (Supplier) LIST_MULTIMAP;
        }

        /**
         * Of set multimap.
         *
         * @param <K> the key type
         * @param <E> the element type
         * @return the supplier
         */
        @SuppressWarnings("rawtypes")
        public static <K, E> Supplier<SetMultimap<K, E>> ofSetMultimap() {
            return (Supplier) SET_MULTIMAP;
        }

        /**
         * Of string builder.
         *
         * @return the supplier
         */
        public static Supplier<StringBuilder> ofStringBuilder() {
            return STRING_BUILDER;
        }

        /**
         * Won't work.
         *
         * @return the supplier
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableList<?>> ofImmutableList() {
            throw new UnsupportedOperationException();
        }

        /**
         * Won't work.
         *
         * @return the supplier
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableSet<?>> ofImmutableSet() {
            throw new UnsupportedOperationException();
        }

        /**
         * Won't work.
         *
         * @return the supplier
         * @throws UnsupportedOperationException the unsupported operation exception
         */
        @Deprecated
        public static Supplier<ImmutableMap<?, ?>> ofImmutableMap() {
            throw new UnsupportedOperationException();
        }

        /**
         * Single.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @param supplier the supplier
         * @return the supplier<? extends c>
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
    public static abstract class Predicates {

        /**
         * Instantiates a new predicates.
         */
        protected Predicates() {
            // for extention.
        }

        /**
         * Indexed.
         *
         * @param <T> the generic type
         * @param predicate the predicate
         * @return the predicate
         */
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
         * Distinct.
         *
         * @param <T> the generic type
         * @return the predicate
         */
        public static <T> Predicate<T> distinct() {
            return new Predicate<T>() {
                private final Set<Object> set = new HashSet<Object>();

                @Override
                public boolean test(T value) {
                    return set.add(value);
                }
            };
        }

        /**
         * Distinct by.
         *
         * @param <T> the generic type
         * @param mapper the mapper
         * @return the predicate
         */
        public static <T> Predicate<T> distinctBy(final Function<? super T, ?> mapper) {
            return new Predicate<T>() {
                private final Set<Object> set = new HashSet<Object>();

                @Override
                public boolean test(T value) {
                    return set.add(mapper.apply(value));
                }
            };
        }

        /**
         * Concurrent distinct.
         *
         * @param <T> the generic type
         * @return the predicate
         */
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
         * Concurrent distinct by.
         *
         * @param <T> the generic type
         * @param mapper the mapper
         * @return the predicate
         */
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
         * Remove the continuous repeat elements.
         * Returns a stateful predicate which should not be used in parallel stream.
         *
         * @param <T> the generic type
         * @return the predicate
         */
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
         * @param <T> the generic type
         * @param periodInMillis the period in millis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @return the predicate
         */
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
         * @param <T> the generic type
         * @param periodInMillis the period in millis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @param update called at the beginning of each duration.
         * @return the predicate
         */
        public static <T> Predicate<T> invertedByDuration(final long periodInMillis, final MutableBoolean cancellationFlag, final Runnable update) {
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
         * @param <T> the generic type
         * @param delayInMillis the delay in millis
         * @param periodInMillis the period in millis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @return the predicate
         */
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
         * @param <T> the generic type
         * @param delayInMillis the delay in millis
         * @param periodInMillis the period in millis
         * @param cancellationFlag the underline scheduled {@code Task} will be cancelled if {@code cancellationFlag} is set to true.
         * @param update called at the beginning of each duration.
         * @return the predicate
         */
        public static <T> Predicate<T> invertedByDuration(final long delayInMillis, final long periodInMillis, final MutableBoolean cancellationFlag,
                final Runnable update) {
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
    public static abstract class BiPredicates {

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
         * Always true.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @return the bi predicate
         */
        public static <T, U> BiPredicate<T, U> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         * Always false.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @return the bi predicate
         */
        public static <T, U> BiPredicate<T, U> alwaysFalse() {
            return ALWAYS_FALSE;
        }

        /**
         * Indexed.
         *
         * @param <U> the generic type
         * @param <T> the generic type
         * @param predicate the predicate
         * @return the bi predicate
         */
        public static <U, T> BiPredicate<U, T> indexed(final IndexedBiPredicate<U, T> predicate) {
            N.checkArgNotNull(predicate);

            return new BiPredicate<U, T>() {
                private final MutableInt idx = new MutableInt(0);

                @Override
                public boolean test(U u, T t) {
                    return predicate.test(u, idx.getAndIncrement(), t);
                }
            };
        }
    }

    /**
     * The Class TriPredicates.
     */
    public static abstract class TriPredicates {

        /** The Constant ALWAYS_TRUE. */
        @SuppressWarnings("rawtypes")
        private static final TriPredicate ALWAYS_TRUE = new TriPredicate() {
            @Override
            public boolean test(Object a, Object b, Object c) {
                return true;
            }
        };

        /** The Constant ALWAYS_FALSE. */
        @SuppressWarnings("rawtypes")
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
         * Always true.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @return the tri predicate
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysTrue() {
            return ALWAYS_TRUE;
        }

        /**
         * Always false.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @return the tri predicate
         */
        public static <A, B, C> TriPredicate<A, B, C> alwaysFalse() {
            return ALWAYS_FALSE;
        }

    }

    /**
     * The Class Consumers.
     */
    public static abstract class Consumers {

        /**
         * Instantiates a new consumers.
         */
        protected Consumers() {
            // for extention.
        }

        /**
         * Returns a <code>Consumer</code> which calls the specified <code>func</code>.
         *
         * @param <T> the generic type
         * @param func the func
         * @return the consumer
         */
        public static <T> Consumer<T> convert(final Function<? super T, ?> func) {
            N.checkArgNotNull(func);

            return new Consumer<T>() {
                @Override
                public void accept(T t) {
                    func.apply(t);
                }
            };
        }

        /**
         * Indexed.
         *
         * @param <T> the generic type
         * @param action the action
         * @return the consumer
         */
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
    public static abstract class BiConsumers {

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
         * Do nothing.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @return the bi consumer
         */
        public static <T, U> BiConsumer<T, U> doNothing() {
            return DO_NOTHING;
        }

        /**
         * Of add.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi consumer
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofAdd() {
            return (BiConsumer<C, T>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi consumer
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofAddAll() {
            return (BiConsumer<C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T> the generic type
         * @return the bi consumer
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofAddAlll() {
            return (BiConsumer<T, T>) ADD_ALL_2;
        }

        /**
         * Of remove.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi consumer
         */
        public static <T, C extends Collection<? super T>> BiConsumer<C, T> ofRemove() {
            return (BiConsumer<C, T>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi consumer
         */
        public static <T, C extends Collection<T>> BiConsumer<C, C> ofRemoveAll() {
            return (BiConsumer<C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T> the generic type
         * @return the bi consumer
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiConsumer<T, T> ofRemoveAlll() {
            return (BiConsumer<T, T>) REMOVE_ALL_2;
        }

        /**
         * Of put.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param <E> the element type
         * @return the bi consumer
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiConsumer<M, E> ofPut() {
            return (BiConsumer<M, E>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @return the bi consumer
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, M> ofPutAll() {
            return (BiConsumer<M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @return the bi consumer
         */
        public static <K, V, M extends Map<K, V>> BiConsumer<M, K> ofRemoveByKey() {
            return (BiConsumer<M, K>) REMOVE_BY_KEY;
        }

        /**
         * Of merge.
         *
         * @return the bi consumer
         */
        public static BiConsumer<Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         * Of append.
         *
         * @param <T> the generic type
         * @return the bi consumer
         */
        public static <T> BiConsumer<StringBuilder, T> ofAppend() {
            return (BiConsumer<StringBuilder, T>) APPEND;
        }

        /**
         * Returns a <code>BiConsumer</code> which calls the specified <code>func</code>.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param func the func
         * @return the bi consumer
         */
        public static <T, U> BiConsumer<T, U> convert(final BiFunction<? super T, ? super U, ?> func) {
            N.checkArgNotNull(func);

            return new BiConsumer<T, U>() {
                @Override
                public void accept(T t, U u) {
                    func.apply(t, u);
                }
            };
        }

        /**
         * Indexed.
         *
         * @param <U> the generic type
         * @param <T> the generic type
         * @param action the action
         * @return the bi consumer
         */
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
    public static abstract class TriConsumers {

        /**
         * Instantiates a new tri consumers.
         */
        protected TriConsumers() {
            // for extention.
        }

        /**
         * Returns a <code>TriConsumer</code> which calls the specified <code>func</code>.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param func the func
         * @return the tri consumer
         */
        public static <A, B, C> TriConsumer<A, B, C> convert(final TriFunction<? super A, ? super B, ? super C, ?> func) {
            N.checkArgNotNull(func);

            return new TriConsumer<A, B, C>() {
                @Override
                public void accept(A a, B b, C c) {
                    func.apply(a, b, c);
                }
            };
        }
    }

    /**
     * The Class Functions.
     */
    public static abstract class Functions {

        /**
         * Instantiates a new functions.
         */
        protected Functions() {
            // for extention.
        }

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param action the action
         * @return the function
         */
        public static <T> Function<T, Void> convert(final Consumer<? super T> action) {
            N.checkArgNotNull(action);

            return new Function<T, Void>() {
                @Override
                public Void apply(T t) {
                    action.accept(t);
                    return null;
                }
            };
        }

        /**
         * Indexed.
         *
         * @param <T> the generic type
         * @param <R> the generic type
         * @param func the func
         * @return the function
         */
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

        /**
         * Pair to list.
         *
         * @param <T> the generic type
         * @return the function
         * @deprecated replaced by PairFn#toList();
         */
        @Deprecated
        public static <T> Function<Pair<T, T>, List<T>> pairToList() {
            return Pairs.toList();
        }

        /**
         * Pair to set.
         *
         * @param <T> the generic type
         * @return the function
         * @deprecated replaced by PairFn#toSet();
         */
        @Deprecated
        public static <T> Function<Pair<T, T>, Set<T>> pairToSet() {
            return Pairs.toSet();
        }

        /**
         * Triple to list.
         *
         * @param <T> the generic type
         * @return the function
         * @deprecated replaced by TripleFn#toList();
         */
        @Deprecated
        public static <T> Function<Triple<T, T, T>, List<T>> tripleToList() {
            return Triples.toList();
        }

        /**
         * Triple to set.
         *
         * @param <T> the generic type
         * @return the function
         * @deprecated replaced by TripleFn#toSet();
         */
        @Deprecated
        public static <T> Function<Triple<T, T, T>, Set<T>> tripleToSet() {
            return Triples.toSet();
        }
    }

    /**
     * The Class BiFunctions.
     */
    public static abstract class BiFunctions {

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
         * Return first.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @return the bi function
         */
        public static <T, U> BiFunction<T, U, T> returnFirst() {
            return (BiFunction<T, U, T>) RETURN_FIRST;
        }

        /**
         * Return second.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @return the bi function
         */
        public static <T, U> BiFunction<T, U, U> returnSecond() {
            return (BiFunction<T, U, U>) RETURN_SECOND;
        }

        /**
         * Of add.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi function
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofAdd() {
            return (BiFunction<C, T, C>) ADD;
        }

        /**
         * Of add all.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi function
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofAddAll() {
            return (BiFunction<C, C, C>) ADD_ALL;
        }

        /**
         * Of add alll.
         *
         * @param <T> the generic type
         * @return the bi function
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofAddAlll() {
            return (BiFunction<T, T, T>) ADD_ALL_2;
        }

        /**
         * Of remove.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi function
         */
        public static <T, C extends Collection<? super T>> BiFunction<C, T, C> ofRemove() {
            return (BiFunction<C, T, C>) REMOVE;
        }

        /**
         * Of remove all.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the bi function
         */
        public static <T, C extends Collection<T>> BiFunction<C, C, C> ofRemoveAll() {
            return (BiFunction<C, C, C>) REMOVE_ALL;
        }

        /**
         * Of remove alll.
         *
         * @param <T> the generic type
         * @return the bi function
         */
        @SuppressWarnings("rawtypes")
        public static <T extends PrimitiveList> BiFunction<T, T, T> ofRemoveAlll() {
            return (BiFunction<T, T, T>) REMOVE_ALL_2;
        }

        /**
         * Of put.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param <E> the element type
         * @return the bi function
         */
        public static <K, V, M extends Map<K, V>, E extends Map.Entry<K, V>> BiFunction<M, E, M> ofPut() {
            return (BiFunction<M, E, M>) PUT;
        }

        /**
         * Of put all.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @return the bi function
         */
        public static <K, V, M extends Map<K, V>> BiFunction<M, M, M> ofPutAll() {
            return (BiFunction<M, M, M>) PUT_ALL;
        }

        /**
         * Of remove by key.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param <U> the generic type
         * @return the bi function
         */
        public static <K, V, M extends Map<K, V>, U> BiFunction<M, K, M> ofRemoveByKey() {
            return (BiFunction<M, K, M>) REMOVE_BY_KEY;
        }

        /**
         * Of merge.
         *
         * @return the bi function
         */
        public static BiFunction<Joiner, Joiner, Joiner> ofMerge() {
            return MERGE;
        }

        /**
         * Of append.
         *
         * @param <T> the generic type
         * @return the bi function
         */
        public static <T> BiFunction<StringBuilder, T, StringBuilder> ofAppend() {
            return (BiFunction<StringBuilder, T, StringBuilder>) APPEND;
        }

        /**
         * Convert.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param action the action
         * @return the bi function
         */
        public static <T, U> BiFunction<T, U, Void> convert(final BiConsumer<? super T, ? super U> action) {
            N.checkArgNotNull(action);

            return new BiFunction<T, U, Void>() {
                @Override
                public Void apply(T t, U u) {
                    action.accept(t, u);
                    return null;
                }
            };
        }

        /**
         * Indexed.
         *
         * @param <U> the generic type
         * @param <T> the generic type
         * @param <R> the generic type
         * @param func the func
         * @return the bi function
         */
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
    public static abstract class TriFunctions {

        /**
         * Instantiates a new tri functions.
         */
        protected TriFunctions() {
            // for extention.
        }

        /**
         * Convert.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param action the action
         * @return the tri function
         */
        public static <A, B, C> TriFunction<A, B, C, Void> convert(final TriConsumer<? super A, ? super B, ? super C> action) {
            N.checkArgNotNull(action);

            return new TriFunction<A, B, C, Void>() {
                @Override
                public Void apply(A a, B b, C c) {
                    action.accept(a, b, c);
                    return null;
                }
            };
        }
    }

    /**
     * The Class BinaryOperators.
     */
    public static abstract class BinaryOperators {

        /** The Constant THROWING_MERGER. */
        @SuppressWarnings("rawtypes")
        private static final BinaryOperator THROWING_MERGER = new BinaryOperator() {
            @Override
            public Object apply(Object t, Object u) {
                throw new IllegalStateException(String.format("Duplicate key (attempted merging values %s and %s)", t, u));
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
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the binary operator
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
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the binary operator
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToFirst() {
            return (BinaryOperator<C>) ADD_ALL_TO_FIRST;
        }

        /**
         * Of add all to bigger.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the binary operator
         */
        @SuppressWarnings("unchecked")
        public static <T, C extends Collection<T>> BinaryOperator<C> ofAddAllToBigger() {
            return (BinaryOperator<C>) ADD_ALL_TO_BIGGER;
        }

        /**
         * Of remove all.
         *
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the binary operator
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
         * @param <T> the generic type
         * @param <C> the generic type
         * @return the binary operator
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
         * @param <M> the generic type
         * @return the binary operator
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
         * @param <M> the generic type
         * @return the binary operator
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
         * @param <M> the generic type
         * @return the binary operator
         */
        @SuppressWarnings("unchecked")
        public static <K, V, M extends Map<K, V>> BinaryOperator<M> ofPutAllToBigger() {
            return (BinaryOperator<M>) PUT_ALL_TO_BIGGER;
        }

        /**
         * Of merge.
         *
         * @return the binary operator
         * @deprecated replaced by {@code #ofMergeToFirst}.
         */
        @Deprecated
        public static BinaryOperator<Joiner> ofMerge() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to first.
         *
         * @return the binary operator
         */
        public static BinaryOperator<Joiner> ofMergeToFirst() {
            return MERGE_TO_FIRST;
        }

        /**
         * Of merge to bigger.
         *
         * @return the binary operator
         */
        public static BinaryOperator<Joiner> ofMergeToBigger() {
            return MERGE_TO_BIGGER;
        }

        /**
         * Of append.
         *
         * @return the binary operator
         * @deprecated replaced by {@code #ofAppendToFirst()}
         */
        @Deprecated
        public static BinaryOperator<StringBuilder> ofAppend() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to first.
         *
         * @return the binary operator
         */
        public static BinaryOperator<StringBuilder> ofAppendToFirst() {
            return APPEND_TO_FIRST;
        }

        /**
         * Of append to bigger.
         *
         * @return the binary operator
         */
        public static BinaryOperator<StringBuilder> ofAppendToBigger() {
            return APPEND_TO_BIGGER;
        }

        /**
         * Of concat.
         *
         * @return the binary operator
         */
        public static BinaryOperator<String> ofConcat() {
            return CONCAT;
        }

        /**
         * Of add int.
         *
         * @return the binary operator
         */
        public static BinaryOperator<Integer> ofAddInt() {
            return ADD_INTEGER;
        }

        /**
         * Of add long.
         *
         * @return the binary operator
         */
        public static BinaryOperator<Long> ofAddLong() {
            return ADD_LONG;
        }

        /**
         * Of add double.
         *
         * @return the binary operator
         */
        public static BinaryOperator<Double> ofAddDouble() {
            return ADD_DOUBLE;
        }

        /**
         * Of add big integer.
         *
         * @return the binary operator
         */
        public static BinaryOperator<BigInteger> ofAddBigInteger() {
            return ADD_BIG_INTEGER;
        }

        /**
         * Of add big decimal.
         *
         * @return the binary operator
         */
        public static BinaryOperator<BigDecimal> ofAddBigDecimal() {
            return ADD_BIG_DECIMAL;
        }

        /**
         * Min.
         *
         * @param <T> the generic type
         * @return the binary operator
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> min() {
            return (BinaryOperator) MIN;
        }

        /**
         * Max.
         *
         * @param <T> the generic type
         * @return the binary operator
         */
        @SuppressWarnings({ "unchecked", "rawtypes" })
        public static <T extends Comparable<? super T>> BinaryOperator<T> max() {
            return (BinaryOperator) MAX;
        }

        /**
         * Min by.
         *
         * @param <T> the generic type
         * @param comparator the comparator
         * @return the binary operator
         */
        public static <T> BinaryOperator<T> minBy(final Comparator<? super T> comparator) {
            N.checkArgNotNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) <= 0 ? t : u;
                }
            };
        }

        /**
         * Max by.
         *
         * @param <T> the generic type
         * @param comparator the comparator
         * @return the binary operator
         */
        public static <T> BinaryOperator<T> maxBy(final Comparator<? super T> comparator) {
            N.checkArgNotNull(comparator);

            return new BinaryOperator<T>() {
                @Override
                public T apply(T t, T u) {
                    return comparator.compare(t, u) >= 0 ? t : u;
                }
            };
        }
    }

    /**
     * The Class UnaryOperators.
     */
    public static abstract class UnaryOperators {

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
         * Identity.
         *
         * @param <T> the generic type
         * @return the unary operator
         */
        public static <T> UnaryOperator<T> identity() {
            return IDENTITY;
        }
    }

    /**
     * The Class Entries.
     */
    public static abstract class Entries {

        /**
         * Instantiates a new entries.
         */
        protected Entries() {
            // for extention.
        }

        /**
         * F.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T> the generic type
         * @param f the f
         * @return the function
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
         * P.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param p the p
         * @return the predicate
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
         * C.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param c the c
         * @return the consumer
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
         * Ef.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T> the generic type
         * @param <E> the element type
         * @param f the f
         * @return the try. function
         */
        public static <K, V, T, E extends Exception> Try.Function<Map.Entry<K, V>, T, E> ef(final Try.BiFunction<? super K, ? super V, ? extends T, E> f) {
            N.checkArgNotNull(f, "BiFunction");

            return new Try.Function<Map.Entry<K, V>, T, E>() {
                @Override
                public T apply(Entry<K, V> e) throws E {
                    return f.apply(e.getKey(), e.getValue());
                }
            };
        }

        /**
         * Ep.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E> the element type
         * @param p the p
         * @return the try. predicate
         */
        public static <K, V, E extends Exception> Try.Predicate<Map.Entry<K, V>, E> ep(final Try.BiPredicate<? super K, ? super V, E> p) {
            N.checkArgNotNull(p, "BiPredicate");

            return new Try.Predicate<Map.Entry<K, V>, E>() {
                @Override
                public boolean test(Entry<K, V> e) throws E {
                    return p.test(e.getKey(), e.getValue());
                }

            };
        }

        /**
         * Ec.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E> the element type
         * @param c the c
         * @return the try. consumer
         */
        public static <K, V, E extends Exception> Try.Consumer<Map.Entry<K, V>, E> ec(final Try.BiConsumer<? super K, ? super V, E> c) {
            N.checkArgNotNull(c, "BiConsumer");

            return new Try.Consumer<Map.Entry<K, V>, E>() {
                @Override
                public void accept(Entry<K, V> e) throws E {
                    c.accept(e.getKey(), e.getValue());
                }
            };
        }

        /**
         * Ff.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <T> the generic type
         * @param <E> the element type
         * @param f the f
         * @return the function
         */
        public static <K, V, T, E extends Exception> Function<Map.Entry<K, V>, T> ff(final Try.BiFunction<? super K, ? super V, ? extends T, E> f) {
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
         * Pp.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E> the element type
         * @param p the p
         * @return the predicate
         */
        public static <K, V, E extends Exception> Predicate<Map.Entry<K, V>> pp(final Try.BiPredicate<? super K, ? super V, E> p) {
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
         * Cc.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <E> the element type
         * @param c the c
         * @return the consumer
         */
        public static <K, V, E extends Exception> Consumer<Map.Entry<K, V>> cc(final Try.BiConsumer<? super K, ? super V, E> c) {
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
    public static abstract class Pairs {

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
         * To list.
         *
         * @param <T> the generic type
         * @return the function
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, List<T>> toList() {
            return (Function) PAIR_TO_LIST;
        }

        /**
         * To set.
         *
         * @param <T> the generic type
         * @return the function
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Pair<T, T>, Set<T>> toSet() {
            return (Function) PAIR_TO_SET;
        }

    }

    /**
     * The Class Triples.
     */
    public static abstract class Triples {

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
         * To list.
         *
         * @param <T> the generic type
         * @return the function
         */
        @SuppressWarnings("rawtypes")
        public static <T> Function<Triple<T, T, T>, List<T>> toList() {
            return (Function) TRIPLE_TO_LIST;
        }

        /**
         * To set.
         *
         * @param <T> the generic type
         * @return the function
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
         * Clone array.
         *
         * @param <T> the generic type
         * @param <A> the generic type
         * @return the function
         */
        @SuppressWarnings("rawtypes")
        public static <T, A extends DisposableArray<T>> Function<A, T[]> cloneArray() {
            return (Function) CLONE;
        }

        /**
         * To str.
         *
         * @param <A> the generic type
         * @return the function
         */
        @SuppressWarnings("rawtypes")
        public static <A extends DisposableArray> Function<A, String> toStr() {
            return (Function) TO_STRING;
        }

        /**
         * Join.
         *
         * @param <A> the generic type
         * @param delimiter the delimiter
         * @return the function
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
    public static abstract class FnC {

        /** The Constant POSITIVE. */
        private static final CharPredicate POSITIVE = new CharPredicate() {
            @Override
            public boolean test(char t) {
                return t > 0;
            }
        };

        /** The Constant NOT_NEGATIVE. */
        private static final CharPredicate NOT_NEGATIVE = new CharPredicate() {
            @Override
            public boolean test(char t) {
                return t >= 0;
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

        /**
         * Instantiates a new fn C.
         */
        protected FnC() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the char predicate
         */
        public static CharPredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the char predicate
         */
        public static CharPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the char bi predicate
         */
        public static CharBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the char bi function
         */
        public static CharBiFunction<Nth> alternate() {
            return new CharBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(char t, char u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnB {

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

        /**
         * Instantiates a new fn B.
         */
        protected FnB() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the byte predicate
         */
        public static BytePredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the byte predicate
         */
        public static BytePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the byte bi predicate
         */
        public static ByteBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the byte bi function
         */
        public static ByteBiFunction<Nth> alternate() {
            return new ByteBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(byte t, byte u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnS {

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

        /**
         * Instantiates a new fn S.
         */
        protected FnS() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the short predicate
         */
        public static ShortPredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the short predicate
         */
        public static ShortPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the short bi predicate
         */
        public static ShortBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the short bi function
         */
        public static ShortBiFunction<Nth> alternate() {
            return new ShortBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(short t, short u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnI {

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

        /**
         * Instantiates a new fn I.
         */
        protected FnI() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the int predicate
         */
        public static IntPredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the int predicate
         */
        public static IntPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the int bi predicate
         */
        public static IntBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the int bi function
         */
        public static IntBiFunction<Nth> alternate() {
            return new IntBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(int t, int u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnL {

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

        /**
         * Instantiates a new fn L.
         */
        protected FnL() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the long predicate
         */
        public static LongPredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the long predicate
         */
        public static LongPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the long bi predicate
         */
        public static LongBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the long bi function
         */
        public static LongBiFunction<Nth> alternate() {
            return new LongBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(long t, long u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnF {

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

        /**
         * Instantiates a new fn F.
         */
        protected FnF() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the float predicate
         */
        public static FloatPredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the float predicate
         */
        public static FloatPredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the float bi predicate
         */
        public static FloatBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the float bi function
         */
        public static FloatBiFunction<Nth> alternate() {
            return new FloatBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(float t, float u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
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
    public static abstract class FnD {

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

        /**
         * Instantiates a new fn D.
         */
        protected FnD() {
            // for extention.
        }

        /**
         * Positve.
         *
         * @return the double predicate
         */
        public static DoublePredicate positve() {
            return POSITIVE;
        }

        /**
         * Not negative.
         *
         * @return the double predicate
         */
        public static DoublePredicate notNegative() {
            return NOT_NEGATIVE;
        }

        /**
         * Equal.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate equal() {
            return EQUAL;
        }

        /**
         * Not equal.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate notEqual() {
            return NOT_EQUAL;
        }

        /**
         * Greater than.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate greaterThan() {
            return GREATER_THAN;
        }

        /**
         * Greater equal.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate greaterEqual() {
            return GREATER_EQUAL;
        }

        /**
         * Less than.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate lessThan() {
            return LESS_THAN;
        }

        /**
         * Less equal.
         *
         * @return the double bi predicate
         */
        public static DoubleBiPredicate lessEqual() {
            return LESS_EQUAL;
        }

        /**
         * Alternate.
         *
         * @return the double bi function
         */
        public static DoubleBiFunction<Nth> alternate() {
            return new DoubleBiFunction<Nth>() {
                private final MutableBoolean flag = MutableBoolean.of(true);

                @Override
                public Nth apply(double t, double u) {
                    return flag.getAndInvert() ? Nth.FIRST : Nth.SECOND;
                }
            };
        }
    }

    /**
     * Utility class for primitive {@code Predicate/Function/Consumer}.
     * 
     * @author haiyangl
     *
     */
    public static abstract class Fnn {

        /**
         * Instantiates a new fnn.
         */
        protected Fnn() {
            // for extention
        }

        /**
         * Unbox B.
         *
         * @return the to byte function
         */
        public static ToByteFunction<Byte> unboxB() {
            return ToByteFunction.UNBOX;
        }

        /**
         * Unbox C.
         *
         * @return the to char function
         */
        public static ToCharFunction<Character> unboxC() {
            return ToCharFunction.UNBOX;
        }

        /**
         * Unbox S.
         *
         * @return the to short function
         */
        public static ToShortFunction<Short> unboxS() {
            return ToShortFunction.UNBOX;
        }

        /**
         * Unbox I.
         *
         * @return the to int function
         */
        public static ToIntFunction<Integer> unboxI() {
            return ToIntFunction.UNBOX;
        }

        /**
         * Unbox L.
         *
         * @return the to long function
         */
        public static ToLongFunction<Long> unboxL() {
            return ToLongFunction.UNBOX;
        }

        /**
         * Unbox F.
         *
         * @return the to float function
         */
        public static ToFloatFunction<Float> unboxF() {
            return ToFloatFunction.UNBOX;
        }

        /**
         * Unbox D.
         *
         * @return the to double function
         */
        public static ToDoubleFunction<Double> unboxD() {
            return ToDoubleFunction.UNBOX;
        }

        /**
         * Cp.
         *
         * @param p the p
         * @return the char predicate
         */
        public static CharPredicate cp(final CharPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Bp.
         *
         * @param p the p
         * @return the byte predicate
         */
        public static BytePredicate bp(final BytePredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Sp.
         *
         * @param p the p
         * @return the short predicate
         */
        public static ShortPredicate sp(final ShortPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Ip.
         *
         * @param p the p
         * @return the int predicate
         */
        public static IntPredicate ip(final IntPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Lp.
         *
         * @param p the p
         * @return the long predicate
         */
        public static LongPredicate lp(final LongPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Fp.
         *
         * @param p the p
         * @return the float predicate
         */
        public static FloatPredicate fp(final FloatPredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Dp.
         *
         * @param p the p
         * @return the double predicate
         */
        public static DoublePredicate dp(final DoublePredicate p) {
            N.checkArgNotNull(p);

            return p;
        }

        /**
         * Cf.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the char function
         */
        public static <R> CharFunction<R> cf(final CharFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Bf.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the byte function
         */
        public static <R> ByteFunction<R> bf(final ByteFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Sf.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the short function
         */
        public static <R> ShortFunction<R> sf(final ShortFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * I F.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the int function
         */
        public static <R> IntFunction<R> iF(final IntFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Lf.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the long function
         */
        public static <R> LongFunction<R> lf(final LongFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Ff.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the float function
         */
        public static <R> FloatFunction<R> ff(final FloatFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Df.
         *
         * @param <R> the generic type
         * @param f the f
         * @return the double function
         */
        public static <R> DoubleFunction<R> df(final DoubleFunction<R> f) {
            N.checkArgNotNull(f);

            return f;
        }

        /**
         * Cc.
         *
         * @param c the c
         * @return the char consumer
         */
        public static CharConsumer cc(final CharConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Bc.
         *
         * @param c the c
         * @return the byte consumer
         */
        public static ByteConsumer bc(final ByteConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Sc.
         *
         * @param c the c
         * @return the short consumer
         */
        public static ShortConsumer sc(final ShortConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Ic.
         *
         * @param c the c
         * @return the int consumer
         */
        public static IntConsumer ic(final IntConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Lc.
         *
         * @param c the c
         * @return the long consumer
         */
        public static LongConsumer lc(final LongConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Fc.
         *
         * @param c the c
         * @return the float consumer
         */
        public static FloatConsumer fc(final FloatConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }

        /**
         * Dc.
         *
         * @param c the c
         * @return the double consumer
         */
        public static DoubleConsumer dc(final DoubleConsumer c) {
            N.checkArgNotNull(c);

            return c;
        }
    }

    /**
     * Utility class for exceptional {@code Predicate/Function/Consumer}.
     * 
     * @author haiyangl
     *
     */
    public static abstract class FN {

        /**
         * Instantiates a new fn.
         */
        protected FN() {
            // for extention
        }

        /**
         * Identity.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. function
         */
        public static <T, E extends Exception> Try.Function<T, T, E> identity() {
            return Fn.IDENTITY;
        }

        /**
         * Empty action.
         *
         * @param <E> the element type
         * @return the try. runnable
         */
        public static <E extends Exception> Try.Runnable<E> emptyAction() {
            return (Try.Runnable<E>) Fn.EMPTY_ACTION;
        }

        /**
         * Do nothing.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. consumer
         */
        public static <T, E extends Exception> Try.Consumer<T, E> doNothing() {
            return Fn.DO_NOTHING;
        }

        /**
         * Sleep.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param millis the millis
         * @return the try. consumer
         */
        public static <T, E extends Exception> Try.Consumer<T, E> sleep(final long millis) {
            return new Try.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    N.sleep(millis);
                }
            };
        }

        /**
         * Sleep uninterruptibly.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param millis the millis
         * @return the try. consumer
         */
        public static <T, E extends Exception> Try.Consumer<T, E> sleepUninterruptibly(final long millis) {
            return new Try.Consumer<T, E>() {
                @Override
                public void accept(T t) throws E {
                    N.sleepUninterruptibly(millis);
                }
            };
        }

        //    public static <T extends AutoCloseable, E extends Exception> Try.Consumer<T, E> close() {
        //        return (Try.Consumer<T, E>) Fn.CLOSE;
        //    }

        /**
         * Close quietly.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. consumer
         */
        public static <T extends AutoCloseable, E extends Exception> Try.Consumer<T, E> closeQuietly() {
            return (Try.Consumer<T, E>) Fn.CLOSE_QUIETLY;
        }

        /**
         * Println.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. consumer
         */
        public static <T, E extends Exception> Try.Consumer<T, E> println() {
            return Fn.PRINTLN;
        }

        /**
         * Println.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param separator the separator
         * @return the try. bi consumer
         */
        public static <T, U, E extends Exception> Try.BiConsumer<T, U, E> println(final String separator) {
            return cc(Fn.<T, U> println(separator));
        }

        /**
         * Pp.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param predicate the predicate
         * @return the try. predicate
         */
        @Beta
        public static <T, E extends Exception> Try.Predicate<T, E> pp(final Predicate<T> predicate) {
            N.checkArgNotNull(predicate);

            return (Try.Predicate<T, E>) predicate;
        }

        /**
         * Pp.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <E> the element type
         * @param a the a
         * @param biPredicate the bi predicate
         * @return the try. predicate
         */
        @Beta
        public static <A, T, E extends Exception> Try.Predicate<T, E> pp(final A a, final BiPredicate<A, T> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return new Try.Predicate<T, E>() {
                @Override
                public boolean test(T t) {
                    return biPredicate.test(a, t);
                }
            };
        }

        /**
         * Pp.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <T> the generic type
         * @param <E> the element type
         * @param a the a
         * @param b the b
         * @param triPredicate the tri predicate
         * @return the try. predicate
         */
        @Beta
        public static <A, B, T, E extends Exception> Try.Predicate<T, E> pp(final A a, final B b, final TriPredicate<A, B, T> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Try.Predicate<T, E>() {
                @Override
                public boolean test(T t) {
                    return triPredicate.test(a, b, t);
                }
            };
        }

        /**
         * Pp.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param biPredicate the bi predicate
         * @return the try. bi predicate
         */
        @Beta
        public static <T, U, E extends Exception> Try.BiPredicate<T, U, E> pp(final BiPredicate<T, U> biPredicate) {
            N.checkArgNotNull(biPredicate);

            return (Try.BiPredicate<T, U, E>) biPredicate;
        }

        /**
         * Pp.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param a the a
         * @param triPredicate the tri predicate
         * @return the try. bi predicate
         */
        @Beta
        public static <A, T, U, E extends Exception> Try.BiPredicate<T, U, E> pp(final A a, final TriPredicate<A, T, U> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return new Try.BiPredicate<T, U, E>() {
                @Override
                public boolean test(T t, U u) {
                    return triPredicate.test(a, t, u);
                }
            };
        }

        /**
         * Pp.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <E> the element type
         * @param triPredicate the tri predicate
         * @return the try. tri predicate
         */
        @Beta
        public static <A, B, C, E extends Exception> Try.TriPredicate<A, B, C, E> pp(final TriPredicate<A, B, C> triPredicate) {
            N.checkArgNotNull(triPredicate);

            return (Try.TriPredicate<A, B, C, E>) triPredicate;
        }

        /**
         * Cc.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param consumer the consumer
         * @return the try. consumer
         */
        @Beta
        public static <T, E extends Exception> Try.Consumer<T, E> cc(final Consumer<T> consumer) {
            N.checkArgNotNull(consumer);

            return (Try.Consumer<T, E>) consumer;
        }

        /**
         * Cc.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <E> the element type
         * @param a the a
         * @param biConsumer the bi consumer
         * @return the try. consumer
         */
        @Beta
        public static <A, T, E extends Exception> Try.Consumer<T, E> cc(final A a, final BiConsumer<A, T> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return new Try.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    biConsumer.accept(a, t);
                }
            };
        }

        /**
         * Cc.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <T> the generic type
         * @param <E> the element type
         * @param a the a
         * @param b the b
         * @param triConsumer the tri consumer
         * @return the try. consumer
         */
        @Beta
        public static <A, B, T, E extends Exception> Try.Consumer<T, E> cc(final A a, final B b, final TriConsumer<A, B, T> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Try.Consumer<T, E>() {
                @Override
                public void accept(T t) {
                    triConsumer.accept(a, b, t);
                }
            };
        }

        /**
         * Cc.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param biConsumer the bi consumer
         * @return the try. bi consumer
         */
        @Beta
        public static <T, U, E extends Exception> Try.BiConsumer<T, U, E> cc(final BiConsumer<T, U> biConsumer) {
            N.checkArgNotNull(biConsumer);

            return (Try.BiConsumer<T, U, E>) biConsumer;
        }

        /**
         * Cc.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <E> the element type
         * @param a the a
         * @param triConsumer the tri consumer
         * @return the try. bi consumer
         */
        @Beta
        public static <A, T, U, E extends Exception> Try.BiConsumer<T, U, E> cc(final A a, final TriConsumer<A, T, U> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return new Try.BiConsumer<T, U, E>() {
                @Override
                public void accept(T t, U u) {
                    triConsumer.accept(a, t, u);
                }
            };
        }

        /**
         * Cc.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <E> the element type
         * @param triConsumer the tri consumer
         * @return the try. tri consumer
         */
        @Beta
        public static <A, B, C, E extends Exception> Try.TriConsumer<A, B, C, E> cc(final TriConsumer<A, B, C> triConsumer) {
            N.checkArgNotNull(triConsumer);

            return (Try.TriConsumer<A, B, C, E>) triConsumer;
        }

        /**
         * Ff.
         *
         * @param <T> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param function the function
         * @return the try. function
         */
        @Beta
        public static <T, R, E extends Exception> Try.Function<T, R, E> ff(final Function<T, R> function) {
            N.checkArgNotNull(function);

            return (Try.Function<T, R, E>) function;
        }

        /**
         * Ff.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param a the a
         * @param biFunction the bi function
         * @return the try. function
         */
        @Beta
        public static <A, T, R, E extends Exception> Try.Function<T, R, E> ff(final A a, final BiFunction<A, T, R> biFunction) {
            N.checkArgNotNull(biFunction);

            return new Try.Function<T, R, E>() {
                @Override
                public R apply(T t) {
                    return biFunction.apply(a, t);
                }
            };
        }

        /**
         * Ff.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <T> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param a the a
         * @param b the b
         * @param triFunction the tri function
         * @return the try. function
         */
        @Beta
        public static <A, B, T, R, E extends Exception> Try.Function<T, R, E> ff(final A a, final B b, final TriFunction<A, B, T, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Try.Function<T, R, E>() {
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
         * Ff.
         *
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param biFunction the bi function
         * @return the try. bi function
         */
        @Beta
        public static <T, U, R, E extends Exception> Try.BiFunction<T, U, R, E> ff(final BiFunction<T, U, R> biFunction) {
            N.checkArgNotNull(biFunction);

            return (Try.BiFunction<T, U, R, E>) biFunction;
        }

        /**
         * Ff.
         *
         * @param <A> the generic type
         * @param <T> the generic type
         * @param <U> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param a the a
         * @param triFunction the tri function
         * @return the try. bi function
         */
        @Beta
        public static <A, T, U, R, E extends Exception> Try.BiFunction<T, U, R, E> ff(final A a, final TriFunction<A, T, U, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return new Try.BiFunction<T, U, R, E>() {
                @Override
                public R apply(T t, U u) {
                    return triFunction.apply(a, t, u);
                }
            };
        }

        /**
         * Ff.
         *
         * @param <A> the generic type
         * @param <B> the generic type
         * @param <C> the generic type
         * @param <R> the generic type
         * @param <E> the element type
         * @param triFunction the tri function
         * @return the try. tri function
         */
        @Beta
        public static <A, B, C, R, E extends Exception> Try.TriFunction<A, B, C, R, E> ff(final TriFunction<A, B, C, R> triFunction) {
            N.checkArgNotNull(triFunction);

            return (Try.TriFunction<A, B, C, R, E>) triFunction;
        }

        /**
         * Throwing merger.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. binary operator
         */
        public static <T, E extends Exception> Try.BinaryOperator<T, E> throwingMerger() {
            return BinaryOperators.THROWING_MERGER;
        }

        /**
         * Ignoring merger.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. binary operator
         */
        public static <T, E extends Exception> Try.BinaryOperator<T, E> ignoringMerger() {
            return BinaryOperators.IGNORING_MERGER;
        }

        /**
         * Replacing merger.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @return the try. binary operator
         */
        public static <T, E extends Exception> Try.BinaryOperator<T, E> replacingMerger() {
            return BinaryOperators.REPLACING_MERGER;
        }

        /**
         * Callable.
         *
         * @param <R> the generic type
         * @param <E> the element type
         * @param callable the callable
         * @return the try. callable
         */
        public static <R, E extends Exception> Try.Callable<R, E> callable(final Try.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            return callable;
        }

        /**
         * Runnable.
         *
         * @param <E> the element type
         * @param runnable the runnable
         * @return the try. runnable
         */
        public static <E extends Exception> Try.Runnable<E> runnable(final Try.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            return runnable;
        }

        /**
         * To callable.
         *
         * @param <E> the element type
         * @param runnable the runnable
         * @return the try. callable
         */
        public static <E extends Exception> Try.Callable<Void, E> toCallable(final Try.Runnable<E> runnable) {
            N.checkArgNotNull(runnable);

            return new Try.Callable<Void, E>() {
                @Override
                public Void call() throws E {
                    runnable.run();
                    return null;
                }
            };
        }

        /**
         * To runnable.
         *
         * @param <R> the generic type
         * @param <E> the element type
         * @param callable the callable
         * @return the try. runnable
         */
        public static <R, E extends Exception> Try.Runnable<E> toRunnable(final Try.Callable<R, E> callable) {
            N.checkArgNotNull(callable);

            return new Try.Runnable<E>() {
                @Override
                public void run() throws E {
                    callable.call();
                }
            };
        }
    }
}
